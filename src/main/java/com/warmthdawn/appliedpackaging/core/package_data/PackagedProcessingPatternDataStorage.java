package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.registry.APItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class PackagedProcessingPatternDataStorage {
    public static final String PATTERN_TAG = "appliedpackaging.packaged_processing_pattern";

    private static final String VERSION = "version";
    private static final String COLOR = "color";
    private static final String PACKAGES = "packages";
    private static final String OUTPUTS = "outputs";
    private static final int CURRENT_VERSION = 2;

    private PackagedProcessingPatternDataStorage() {
    }

    public static boolean canStore(ItemStack stack) {
        return stack.is(APItems.PACKAGED_PROCESSING_PATTERN.get()) || PackagePatternDataStorage.isAe2BlankPattern(stack);
    }

    public static Optional<EncodedPackagedProcessingPattern> read(ItemStack stack) {
        if (!canStore(stack)) {
            return Optional.empty();
        }

        Optional<EncodedPackagedProcessingPattern> encoded = readCurrentFormat(stack);
        if (encoded.isPresent()) {
            return encoded;
        }

        if (stack.is(APItems.PACKAGED_PROCESSING_PATTERN.get())) {
            return PackagePatternDataStorage.read(stack)
                    .map(pattern -> new EncodedPackagedProcessingPattern(pattern.color(), List.of(pattern.data()), List.of()));
        }
        return Optional.empty();
    }

    public static void write(ItemStack stack, PackageColor color, List<PackageData> packages) {
        write(stack, color, packages, List.of());
    }

    public static void write(
            ItemStack stack,
            PackageColor color,
            List<PackageData> packages,
            List<GenericStack> outputs) {
        if (!canStore(stack)) {
            throw new IllegalArgumentException("Packaged processing pattern data can only be written to packaged processing pattern carriers");
        }
        EncodedPackagedProcessingPattern encoded = new EncodedPackagedProcessingPattern(color, packages, outputs);
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION, CURRENT_VERSION);
        tag.putString(COLOR, encoded.color().id());

        ListTag packageList = new ListTag();
        for (PackageData data : encoded.packages()) {
            packageList.add(PackageDataStorage.writeTag(data));
        }
        tag.put(PACKAGES, packageList);
        if (!encoded.outputs().isEmpty()) {
            ListTag outputList = new ListTag();
            for (GenericStack output : encoded.outputs()) {
                outputList.add(GenericStack.writeTag(output));
            }
            tag.put(OUTPUTS, outputList);
        }
        stack.getOrCreateTag().put(PATTERN_TAG, tag);
    }

    private static Optional<EncodedPackagedProcessingPattern> readCurrentFormat(ItemStack stack) {
        if (!stack.hasTag()) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTagElement(PATTERN_TAG);
        int version = tag == null ? 0 : tag.getInt(VERSION);
        if (tag == null || version < 1 || version > CURRENT_VERSION || !tag.contains(PACKAGES, Tag.TAG_LIST)) {
            return Optional.empty();
        }
        Optional<PackageColor> color = PackageColor.byId(tag.getString(COLOR));
        if (color.isEmpty()) {
            return Optional.empty();
        }

        List<PackageData> packages = new ArrayList<>();
        for (Tag element : tag.getList(PACKAGES, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag packageTag)) {
                return Optional.empty();
            }
            Optional<PackageData> data = PackageDataStorage.readTag(packageTag, color.get());
            if (data.isEmpty()) {
                return Optional.empty();
            }
            packages.add(data.get());
        }
        if (packages.isEmpty()) {
            return Optional.empty();
        }

        List<GenericStack> outputs = new ArrayList<>();
        if (version >= 2 && tag.contains(OUTPUTS, Tag.TAG_LIST)) {
            for (Tag element : tag.getList(OUTPUTS, Tag.TAG_COMPOUND)) {
                if (!(element instanceof CompoundTag outputTag)) {
                    return Optional.empty();
                }
                GenericStack output = GenericStack.readTag(outputTag);
                if (output == null || output.amount() <= 0) {
                    return Optional.empty();
                }
                outputs.add(output);
            }
        }
        return Optional.of(new EncodedPackagedProcessingPattern(color.get(), packages, outputs));
    }

    public record EncodedPackagedProcessingPattern(
            PackageColor color,
            List<PackageData> packages,
            List<GenericStack> outputs) {
        public EncodedPackagedProcessingPattern {
            if (color == null) {
                throw new IllegalArgumentException("Packaged processing pattern color cannot be null");
            }
            packages = List.copyOf(packages);
            outputs = List.copyOf(outputs);
            if (packages.isEmpty()) {
                throw new IllegalArgumentException("Packaged processing patterns must contain at least one package");
            }
            for (PackageData data : packages) {
                if (data == null) {
                    throw new IllegalArgumentException("Packaged processing pattern package data cannot be null");
                }
            }
            for (GenericStack output : outputs) {
                if (output == null || output.amount() <= 0) {
                    throw new IllegalArgumentException("Packaged processing pattern outputs must have positive amounts");
                }
            }
        }
    }
}

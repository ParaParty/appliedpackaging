package com.warmthdawn.appliedpackaging.core.package_data;

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
    private static final int CURRENT_VERSION = 1;

    private PackagedProcessingPatternDataStorage() {
    }

    public static boolean canStore(ItemStack stack) {
        return stack.is(APItems.PACKAGED_PROCESSING_PATTERN.get());
    }

    public static Optional<EncodedPackagedProcessingPattern> read(ItemStack stack) {
        if (!canStore(stack)) {
            return Optional.empty();
        }

        Optional<EncodedPackagedProcessingPattern> encoded = readCurrentFormat(stack);
        if (encoded.isPresent()) {
            return encoded;
        }

        return PackagePatternDataStorage.read(stack)
                .map(pattern -> new EncodedPackagedProcessingPattern(pattern.color(), List.of(pattern.data())));
    }

    public static void write(ItemStack stack, PackageColor color, List<PackageData> packages) {
        if (!canStore(stack)) {
            throw new IllegalArgumentException("Packaged processing pattern data can only be written to packaged processing pattern items");
        }
        EncodedPackagedProcessingPattern encoded = new EncodedPackagedProcessingPattern(color, packages);
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION, CURRENT_VERSION);
        tag.putString(COLOR, encoded.color().id());

        ListTag packageList = new ListTag();
        for (PackageData data : encoded.packages()) {
            packageList.add(PackageDataStorage.writeTag(data));
        }
        tag.put(PACKAGES, packageList);
        stack.getOrCreateTag().put(PATTERN_TAG, tag);
    }

    private static Optional<EncodedPackagedProcessingPattern> readCurrentFormat(ItemStack stack) {
        if (!stack.hasTag()) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTagElement(PATTERN_TAG);
        if (tag == null || tag.getInt(VERSION) != CURRENT_VERSION || !tag.contains(PACKAGES, Tag.TAG_LIST)) {
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
        return Optional.of(new EncodedPackagedProcessingPattern(color.get(), packages));
    }

    public record EncodedPackagedProcessingPattern(PackageColor color, List<PackageData> packages) {
        public EncodedPackagedProcessingPattern {
            if (color == null) {
                throw new IllegalArgumentException("Packaged processing pattern color cannot be null");
            }
            packages = List.copyOf(packages);
            if (packages.isEmpty()) {
                throw new IllegalArgumentException("Packaged processing patterns must contain at least one package");
            }
            for (PackageData data : packages) {
                if (data == null) {
                    throw new IllegalArgumentException("Packaged processing pattern package data cannot be null");
                }
            }
        }
    }
}

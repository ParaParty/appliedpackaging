package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.registry.APItems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class PackageCraftingPatternDataStorage {
    public static final String PATTERN_TAG = "appliedpackaging.package_crafting_pattern";

    public static final int INPUT_SLOT_COUNT = 9 * 9;
    private static final String VERSION = "version";
    private static final String COLOR = "color";
    private static final String INPUTS = "inputs";
    private static final String SLOT = "slot";
    private static final String STACK = "stack";
    private static final String PACKAGE = "package";
    private static final int CURRENT_VERSION = 1;

    private PackageCraftingPatternDataStorage() {
    }

    public static boolean canStore(ItemStack stack) {
        return stack.is(APItems.PACKAGE_PATTERN.get());
    }

    public static boolean hasData(ItemStack stack) {
        return canStore(stack) && stack.hasTag() && stack.getTagElement(PATTERN_TAG) != null;
    }

    public static Optional<EncodedPackageCraftingPattern> read(ItemStack stack) {
        if (!hasData(stack)) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTagElement(PATTERN_TAG);
        if (tag == null
                || tag.getInt(VERSION) != CURRENT_VERSION
                || !tag.contains(INPUTS, Tag.TAG_LIST)
                || !tag.contains(PACKAGE, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        Optional<PackageColor> color = PackageColor.byId(tag.getString(COLOR));
        if (color.isEmpty()) {
            return Optional.empty();
        }
        Optional<PackageData> data = PackageDataStorage.readTag(tag.getCompound(PACKAGE), color.get());
        if (data.isEmpty()) {
            return Optional.empty();
        }

        GenericStack[] inputs = new GenericStack[INPUT_SLOT_COUNT];
        for (Tag element : tag.getList(INPUTS, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag inputTag)
                    || !inputTag.contains(STACK, Tag.TAG_COMPOUND)) {
                return Optional.empty();
            }
            int slot = inputTag.getInt(SLOT);
            if (slot < 0 || slot >= INPUT_SLOT_COUNT) {
                return Optional.empty();
            }
            GenericStack input = GenericStack.readTag(inputTag.getCompound(STACK));
            if (input == null || input.amount() <= 0) {
                return Optional.empty();
            }
            inputs[slot] = input;
        }
        boolean hasInput = Arrays.stream(inputs).anyMatch(input -> input != null && input.amount() > 0);
        if (!hasInput) {
            return Optional.empty();
        }
        return Optional.of(new EncodedPackageCraftingPattern(color.get(), inputs, data.get()));
    }

    public static ItemStack encode(EncodedPackageCraftingPattern pattern) {
        ItemStack stack = new ItemStack(APItems.PACKAGE_PATTERN.get());
        write(stack, pattern);
        return stack;
    }

    public static void write(ItemStack stack, EncodedPackageCraftingPattern pattern) {
        if (!canStore(stack)) {
            throw new IllegalArgumentException("Package crafting pattern data can only be written to package pattern carriers");
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION, CURRENT_VERSION);
        tag.putString(COLOR, pattern.color().id());
        ListTag inputList = new ListTag();
        GenericStack[] sparseInputs = pattern.sparseInputs();
        for (int slot = 0; slot < sparseInputs.length; slot++) {
            GenericStack input = sparseInputs[slot];
            if (input == null || input.amount() <= 0) {
                continue;
            }
            CompoundTag inputTag = new CompoundTag();
            inputTag.putInt(SLOT, slot);
            inputTag.put(STACK, GenericStack.writeTag(input));
            inputList.add(inputTag);
        }
        tag.put(INPUTS, inputList);
        tag.put(PACKAGE, PackageDataStorage.writeTag(pattern.data()));
        stack.getOrCreateTag().put(PATTERN_TAG, tag);
    }

    public static Optional<EncodedPackageCraftingPattern> create(
            PackageColor color,
            GenericStack[] sparseInputs,
            Optional<MarkerSpec> marker) {
        if (sparseInputs == null || sparseInputs.length < INPUT_SLOT_COUNT) {
            return Optional.empty();
        }
        List<GenericStack> contents = new ArrayList<>();
        GenericStack[] normalizedInputs = new GenericStack[INPUT_SLOT_COUNT];
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            GenericStack input = sparseInputs[slot];
            if (input == null || input.amount() <= 0) {
                continue;
            }
            normalizedInputs[slot] = input;
            contents.add(input);
        }
        if (contents.isEmpty()) {
            return Optional.empty();
        }
        PackagePlanResult result = PackagePlanBuilder.build(
                color == null ? PackageColor.FLUIX : color,
                contents,
                List.of(),
                marker != null && marker.isPresent() ? MarkerMergeMode.OVERRIDE : MarkerMergeMode.CLEAR,
                marker == null ? Optional.empty() : marker,
                PackageCapacityProfile.STORAGE_256K,
                0);
        return result.data().map(data -> new EncodedPackageCraftingPattern(
                color == null ? PackageColor.FLUIX : color,
                normalizedInputs,
                data));
    }

    public static ItemStack toPackageStack(EncodedPackageCraftingPattern pattern) {
        ItemStack stack = new ItemStack(APItems.packageItems().get(pattern.color()).get());
        PackageDataStorage.write(stack, pattern.data());
        return stack;
    }

    public record EncodedPackageCraftingPattern(
            PackageColor color,
            GenericStack[] sparseInputs,
            PackageData data) {
        public EncodedPackageCraftingPattern {
            if (color == null) {
                throw new IllegalArgumentException("Package crafting pattern color cannot be null");
            }
            if (sparseInputs == null || sparseInputs.length != INPUT_SLOT_COUNT) {
                throw new IllegalArgumentException("Package crafting pattern must have 81 sparse input slots");
            }
            sparseInputs = Arrays.copyOf(sparseInputs, INPUT_SLOT_COUNT);
            if (data == null) {
                throw new IllegalArgumentException("Package crafting pattern data cannot be null");
            }
        }

        public List<GenericStack> denseInputs() {
            List<GenericStack> inputs = new ArrayList<>();
            for (GenericStack input : sparseInputs) {
                if (input != null && input.amount() > 0) {
                    inputs.add(input);
                }
            }
            return List.copyOf(inputs);
        }
    }
}

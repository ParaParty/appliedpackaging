package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class ColoredProcessingPatternDataStorage {
    public static final String PATTERN_TAG = "appliedpackaging.colored_processing_pattern";

    private static final String VERSION = "version";
    private static final String INPUTS = "inputs";
    private static final String SLOT = "slot";
    private static final String COLOR = "color";
    private static final String AE2_PROCESSING_INPUTS = "in";
    private static final String AE2_PROCESSING_OUTPUTS = "out";
    private static final int CURRENT_VERSION = 1;

    private ColoredProcessingPatternDataStorage() {
    }

    public static boolean canStore(ItemStack stack) {
        return !stack.isEmpty()
                && stack.hasTag()
                && stack.getTag().contains(AE2_PROCESSING_INPUTS, Tag.TAG_LIST);
    }

    public static boolean hasData(ItemStack stack) {
        return stack.hasTag() && stack.getTagElement(PATTERN_TAG) != null;
    }

    public static Optional<EncodedColoredProcessingPattern> read(ItemStack stack) {
        if (!hasData(stack)) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTagElement(PATTERN_TAG);
        if (tag == null || tag.getInt(VERSION) != CURRENT_VERSION || !tag.contains(INPUTS, Tag.TAG_LIST)) {
            return Optional.empty();
        }

        Map<Integer, PackageColor> slotColors = new LinkedHashMap<>();
        for (Tag element : tag.getList(INPUTS, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag entry)) {
                return Optional.empty();
            }
            int slot = entry.getInt(SLOT);
            if (slot < 0) {
                return Optional.empty();
            }
            Optional<PackageColor> color = PackageColor.byId(entry.getString(COLOR));
            if (color.isEmpty()) {
                return Optional.empty();
            }
            slotColors.put(slot, color.get());
        }

        if (slotColors.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EncodedColoredProcessingPattern(slotColors));
    }

    public static void write(ItemStack stack, Map<Integer, PackageColor> slotColors) {
        if (!canStore(stack)) {
            throw new IllegalArgumentException("Colored processing data can only be written to encoded processing patterns");
        }
        if (slotColors == null || slotColors.isEmpty()) {
            stack.getOrCreateTag().remove(PATTERN_TAG);
            return;
        }

        EncodedColoredProcessingPattern encoded = new EncodedColoredProcessingPattern(slotColors);
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION, CURRENT_VERSION);
        ListTag inputs = new ListTag();
        for (var entry : encoded.slotColors().entrySet()) {
            CompoundTag input = new CompoundTag();
            input.putInt(SLOT, entry.getKey());
            input.putString(COLOR, entry.getValue().id());
            inputs.add(input);
        }
        tag.put(INPUTS, inputs);
        stack.getOrCreateTag().put(PATTERN_TAG, tag);
    }

    public static List<GenericStack> readSparseInputs(ItemStack stack) {
        return readSparseStacks(stack, AE2_PROCESSING_INPUTS);
    }

    public static List<GenericStack> readSparseOutputs(ItemStack stack) {
        return readSparseStacks(stack, AE2_PROCESSING_OUTPUTS);
    }

    private static List<GenericStack> readSparseStacks(ItemStack stack, String key) {
        if (!stack.hasTag() || !stack.getTag().contains(key, Tag.TAG_LIST)) {
            return List.of();
        }

        List<GenericStack> inputs = new ArrayList<>();
        for (Tag element : stack.getTag().getList(key, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag entry) || entry.isEmpty()) {
                inputs.add(null);
                continue;
            }
            GenericStack stackEntry = GenericStack.readTag(entry);
            inputs.add(stackEntry != null && stackEntry.amount() > 0 ? stackEntry : null);
        }
        return Collections.unmodifiableList(inputs);
    }

    public record EncodedColoredProcessingPattern(Map<Integer, PackageColor> slotColors) {
        public EncodedColoredProcessingPattern {
            Map<Integer, PackageColor> copy = new LinkedHashMap<>();
            for (var entry : slotColors.entrySet()) {
                if (entry.getKey() == null || entry.getKey() < 0 || entry.getValue() == null) {
                    throw new IllegalArgumentException("Colored processing pattern slots must have a non-negative slot and color");
                }
                copy.put(entry.getKey(), entry.getValue());
            }
            if (copy.isEmpty()) {
                throw new IllegalArgumentException("Colored processing pattern must contain at least one colored slot");
            }
            slotColors = Collections.unmodifiableMap(copy);
        }

        public PackageColor colorForSlot(int slot) {
            return slotColors.getOrDefault(slot, PackageColor.FLUIX);
        }
    }
}

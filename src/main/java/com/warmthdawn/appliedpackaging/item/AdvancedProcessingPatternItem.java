package com.warmthdawn.appliedpackaging.item;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.EncodedPatternItem;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDetails;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Encoded processing pattern whose input columns describe ordered packages. */
public class AdvancedProcessingPatternItem extends EncodedPatternItem {
    public AdvancedProcessingPatternItem(Properties properties) {
        super(properties);
    }

    @Override
    public AdvancedProcessingPatternDetails decode(ItemStack stack, Level level, boolean tryRecovery) {
        return decode(AEItemKey.of(stack), level);
    }

    @Override
    public AdvancedProcessingPatternDetails decode(AEItemKey what, Level level) {
        if (what == null || !what.hasTag()) {
            return null;
        }
        try {
            return new AdvancedProcessingPatternDetails(what);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public ItemStack encode(GenericStack[] sparseInputs, GenericStack[] sparseOutputs) {
        if (sparseInputs.length > com.warmthdawn.appliedpackaging.core.package_data
                .AdvancedProcessingPatternDataStorage.MAX_INPUT_SLOTS) {
            throw new IllegalArgumentException("Advanced pattern has too many inputs");
        }
        if (sparseOutputs.length > com.warmthdawn.appliedpackaging.core.package_data
                .AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS) {
            throw new IllegalArgumentException("Advanced pattern has too many outputs");
        }
        if (Arrays.stream(sparseInputs).noneMatch(Objects::nonNull)) {
            throw new IllegalArgumentException("At least one input must be non-null");
        }
        if (sparseOutputs.length == 0 || sparseOutputs[0] == null) {
            throw new IllegalArgumentException("The primary output must be non-null");
        }

        ItemStack stack = new ItemStack(this);
        stack.getOrCreateTag().put("in", encodeStacks(sparseInputs));
        stack.getOrCreateTag().put("out", encodeStacks(sparseOutputs));
        return stack;
    }

    private static ListTag encodeStacks(GenericStack[] stacks) {
        ListTag result = new ListTag();
        for (GenericStack stack : stacks) {
            result.add(GenericStack.writeTag(stack));
        }
        while (!result.isEmpty() && result.getCompound(result.size() - 1).isEmpty()) {
            result.remove(result.size() - 1);
        }
        return result;
    }
}

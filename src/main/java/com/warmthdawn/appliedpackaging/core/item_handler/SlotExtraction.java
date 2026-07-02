package com.warmthdawn.appliedpackaging.core.item_handler;

import net.minecraft.world.item.ItemStack;

public record SlotExtraction(int slot, ItemStack stack) {
    public SlotExtraction {
        if (slot < 0) {
            throw new IllegalArgumentException("Slot must be non-negative");
        }
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("Extraction stack cannot be empty");
        }
        stack = stack.copy();
    }
}

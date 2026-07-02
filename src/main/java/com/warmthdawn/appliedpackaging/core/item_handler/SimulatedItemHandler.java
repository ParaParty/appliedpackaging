package com.warmthdawn.appliedpackaging.core.item_handler;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

final class SimulatedItemHandler extends ItemStackHandler {
    private SimulatedItemHandler(int size) {
        super(size);
    }

    static SimulatedItemHandler copyOf(IItemHandler source) {
        SimulatedItemHandler copy = new SimulatedItemHandler(source.getSlots());
        for (int slot = 0; slot < source.getSlots(); slot++) {
            copy.setStackInSlot(slot, source.getStackInSlot(slot).copy());
        }
        return copy;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        validateSlotIndex(slot);
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int extractedAmount = Math.min(amount, existing.getCount());
        ItemStack extracted = existing.copy();
        extracted.setCount(extractedAmount);
        if (!simulate) {
            existing.shrink(extractedAmount);
            if (existing.isEmpty()) {
                stacks.set(slot, ItemStack.EMPTY);
            }
            onContentsChanged(slot);
        }
        return extracted;
    }
}

package com.warmthdawn.appliedpackaging.core.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class PackageItemStorage implements MEStorage {
    private final IItemHandler itemHandler;
    private final Component description;

    public PackageItemStorage(IItemHandler itemHandler, Component description) {
        this.itemHandler = itemHandler;
        this.description = description;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!isPackageKey(what) || amount <= 0) {
            return 0;
        }

        AEItemKey key = (AEItemKey) what;
        long inserted = 0;
        long remaining = amount;
        while (remaining > 0) {
            int batch = (int) Math.min(remaining, key.getMaxStackSize());
            ItemStack stack = key.toStack(batch);
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(itemHandler, stack, mode.isSimulate());
            int accepted = batch - remainder.getCount();
            inserted += accepted;
            remaining -= accepted;
            if (accepted < batch) {
                break;
            }
        }
        return inserted;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!isPackageKey(what) || amount <= 0) {
            return 0;
        }

        AEItemKey key = (AEItemKey) what;
        long extracted = 0;
        long remaining = amount;
        for (int slot = 0; slot < itemHandler.getSlots() && remaining > 0; slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (!isLegalPackageStack(stack) || !key.matches(stack)) {
                continue;
            }

            int requested = (int) Math.min(remaining, stack.getCount());
            ItemStack result = itemHandler.extractItem(slot, requested, mode.isSimulate());
            if (!result.isEmpty() && key.matches(result)) {
                extracted += result.getCount();
                remaining -= result.getCount();
            }
        }
        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (isLegalPackageStack(stack)) {
                out.add(AEItemKey.of(stack), stack.getCount());
            }
        }
    }

    @Override
    public Component getDescription() {
        return description;
    }

    public static boolean isPackageKey(AEKey key) {
        if (!AEItemKey.is(key)) {
            return false;
        }
        return isLegalPackageStack(((AEItemKey) key).toStack());
    }

    public static boolean isLegalPackageStack(ItemStack stack) {
        return !stack.isEmpty() && PackageDataStorage.read(stack).isPresent();
    }
}

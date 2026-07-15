package com.warmthdawn.appliedpackaging.core.item_handler;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public final class PackageContentsInserter {
    private PackageContentsInserter() {
    }

    /**
     * Checks the complete package against one cumulative snapshot of the target.
     * The real target is also queried in simulation mode so custom slot behavior is respected.
     */
    public static boolean canInsert(PackageData data, IItemHandler target) {
        SimulatedItemHandler simulated = SimulatedItemHandler.copyOf(target);
        return visitOrderedInputPushes(data, stack ->
                ItemHandlerHelper.insertItemStacked(target, stack.copy(), true).isEmpty()
                        && ItemHandlerHelper.insertItemStacked(simulated, stack, false).isEmpty());
    }

    /**
     * Uses the same check-then-push contract as an AE2 Pattern Provider: simulate the complete
     * input first, then push every stored package entry in order without aggregating repeated keys.
     * One oversized entry may still require multiple physical ItemStack pushes.
     */
    public static boolean insert(PackageData data, IItemHandler target) {
        if (!canInsert(data, target)) {
            return false;
        }
        return visitOrderedInputPushes(data,
                stack -> ItemHandlerHelper.insertItemStacked(target, stack, false).isEmpty());
    }

    private static boolean visitOrderedInputPushes(PackageData data, Predicate<ItemStack> visitor) {
        for (GenericStack entry : data.contents()) {
            if (!AEItemKey.is(entry.what()) || entry.amount() <= 0 || entry.amount() > Integer.MAX_VALUE) {
                return false;
            }
            AEItemKey key = (AEItemKey) entry.what();
            long remaining = entry.amount();
            while (remaining > 0) {
                int amount = (int) Math.min(remaining, key.getMaxStackSize());
                if (!visitor.test(key.toStack(amount))) {
                    return false;
                }
                remaining -= amount;
            }
        }
        return true;
    }
}

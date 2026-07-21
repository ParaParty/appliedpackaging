package com.warmthdawn.appliedpackaging.core.ae2;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.core.item_handler.PackageContentsInserter;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public final class PackageUnpackingOperations {
    private PackageUnpackingOperations() {
    }

    public static boolean canUnpack(ItemStack packageStack, IItemHandler target) {
        return canUnpack(packageStack, target, false);
    }

    public static boolean canUnpack(ItemStack packageStack, IItemHandler target, boolean blockingMode) {
        return PackageItemStorage.isLegalPackageStack(packageStack)
                && PackageDataStorage.read(packageStack)
                        .map(data -> (!blockingMode || isEmpty(target))
                                && PackageContentsInserter.canInsert(data, target))
                        .orElse(false);
    }

    public static boolean canUnpack(
            ItemStack packageStack,
            PackageUnpackingTarget target,
            boolean blockingMode) {
        return PackageItemStorage.isLegalPackageStack(packageStack)
                && PackageDataStorage.read(packageStack)
                        .map(data -> (!blockingMode || target.isEmpty())
                                && canInsertAll(data.contents(), target))
                        .orElse(false);
    }

    /** Pushes a locally-held package into its destination without consuming the held stack itself. */
    public static boolean unpack(ItemStack packageStack, IItemHandler target) {
        return unpack(packageStack, target, false);
    }

    public static boolean unpack(ItemStack packageStack, IItemHandler target, boolean blockingMode) {
        return PackageItemStorage.isLegalPackageStack(packageStack)
                && PackageDataStorage.read(packageStack)
                        .map(data -> (!blockingMode || isEmpty(target))
                                && PackageContentsInserter.insert(data, target))
                        .orElse(false);
    }

    public static boolean unpack(
            ItemStack packageStack,
            PackageUnpackingTarget target,
            boolean blockingMode) {
        return PackageItemStorage.isLegalPackageStack(packageStack)
                && PackageDataStorage.read(packageStack)
                        .map(data -> (!blockingMode || target.isEmpty())
                                && insertAll(data.contents(), target))
                        .orElse(false);
    }

    private static boolean canInsertAll(List<GenericStack> contents, PackageUnpackingTarget target) {
        Map<AEKey, Long> totals = new LinkedHashMap<>();
        for (GenericStack entry : contents) {
            try {
                totals.merge(entry.what(), entry.amount(), Math::addExact);
            } catch (ArithmeticException ignored) {
                return false;
            }
        }
        for (var entry : totals.entrySet()) {
            if (target.insert(entry.getKey(), entry.getValue(), Actionable.SIMULATE) != entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertAll(List<GenericStack> contents, PackageUnpackingTarget target) {
        if (!canInsertAll(contents, target)) {
            return false;
        }
        for (GenericStack entry : contents) {
            if (target.insert(entry.what(), entry.amount(), Actionable.MODULATE) != entry.amount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEmpty(IItemHandler target) {
        for (int slot = 0; slot < target.getSlots(); slot++) {
            if (!target.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}

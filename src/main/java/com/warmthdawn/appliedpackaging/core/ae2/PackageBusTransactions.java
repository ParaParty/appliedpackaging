package com.warmthdawn.appliedpackaging.core.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackageTransactions;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public final class PackageBusTransactions {
    private PackageBusTransactions() {
    }

    public static boolean exportOnePackage(
            MEStorage source,
            IItemHandler target,
            PackageFilter filter,
            IActionSource actionSource) {
        for (var entry : source.getAvailableStacks()) {
            AEKey key = entry.getKey();
            if (!PackageItemStorage.isPackageKey(key) || entry.getLongValue() <= 0) {
                continue;
            }
            ItemStack packageStack = key.wrapForDisplayOrFilter();
            packageStack.setCount(1);
            if (!matches(filter, packageStack)) {
                continue;
            }
            if (!ItemHandlerHelper.insertItemStacked(target, packageStack.copy(), true).isEmpty()
                    || source.extract(key, 1, Actionable.SIMULATE, actionSource) != 1) {
                continue;
            }

            if (source.extract(key, 1, Actionable.MODULATE, actionSource) != 1) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, packageStack, false);
            if (remainder.isEmpty()) {
                return true;
            }
            restorePackage(source, key, actionSource, "export");
            return false;
        }
        return false;
    }

    public static boolean unpackOnePackage(
            MEStorage source,
            IItemHandler target,
            PackageFilter filter,
            IActionSource actionSource) {
        for (var entry : source.getAvailableStacks()) {
            AEKey key = entry.getKey();
            if (!PackageItemStorage.isPackageKey(key) || entry.getLongValue() <= 0) {
                continue;
            }
            ItemStack packageStack = key.wrapForDisplayOrFilter();
            packageStack.setCount(1);
            if (!matches(filter, packageStack)) {
                continue;
            }
            var data = PackageDataStorage.read(packageStack);
            if (data.isEmpty()
                    || !ItemPackageTransactions.canInsertPackageContents(data.get(), target)
                    || source.extract(key, 1, Actionable.SIMULATE, actionSource) != 1) {
                continue;
            }

            if (source.extract(key, 1, Actionable.MODULATE, actionSource) != 1) {
                continue;
            }
            if (ItemPackageTransactions.insertPackageContents(data.get(), target, false)) {
                return true;
            }
            restorePackage(source, key, actionSource, "unpack");
            return false;
        }
        return false;
    }

    private static boolean matches(PackageFilter filter, ItemStack stack) {
        if (!(stack.getItem() instanceof PackageItem packageItem)) {
            return false;
        }
        return PackageDataStorage.read(stack)
                .map(data -> filter == null || filter.isAny() || filter.matches(packageItem.color(), data))
                .orElse(false);
    }

    private static void restorePackage(
            MEStorage source,
            AEKey key,
            IActionSource actionSource,
            String operation) {
        long restored = source.insert(key, 1, Actionable.MODULATE, actionSource);
        if (restored != 1) {
            AppliedPackaging.LOGGER.error(
                    "Package bus {} rollback failed for {}: restored {} of 1",
                    operation,
                    key,
                    restored);
        }
    }
}

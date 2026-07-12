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
import java.util.function.Predicate;

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

    public static boolean unpackOnePackage(
            MEStorage source,
            IItemHandler target,
            Predicate<ItemStack> filter,
            IActionSource actionSource) {
        for (var entry : source.getAvailableStacks()) {
            AEKey key = entry.getKey();
            if (!PackageItemStorage.isPackageKey(key) || entry.getLongValue() <= 0) {
                continue;
            }
            ItemStack packageStack = key.wrapForDisplayOrFilter();
            packageStack.setCount(1);
            if (filter != null && !filter.test(packageStack)) {
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

    /**
     * Removes one complete package from network storage and returns it to the caller as a locally-held work item.
     * The destination is simulated before extraction so a bus does not reserve packages that are already blocked.
     * The caller owns the returned stack and must either unpack it later or return/drop it.
     */
    public static ItemStack reserveOnePackageForUnpacking(
            MEStorage source,
            IItemHandler target,
            Predicate<ItemStack> filter,
            IActionSource actionSource) {
        for (var entry : source.getAvailableStacks()) {
            AEKey key = entry.getKey();
            if (!PackageItemStorage.isPackageKey(key) || entry.getLongValue() <= 0) {
                continue;
            }
            ItemStack packageStack = key.wrapForDisplayOrFilter();
            packageStack.setCount(1);
            if (filter != null && !filter.test(packageStack)) {
                continue;
            }
            if (!canUnpackHeldPackage(packageStack, target)
                    || source.extract(key, 1, Actionable.SIMULATE, actionSource) != 1) {
                continue;
            }
            if (source.extract(key, 1, Actionable.MODULATE, actionSource) == 1) {
                return packageStack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean canUnpackHeldPackage(ItemStack packageStack, IItemHandler target) {
        return PackageItemStorage.isLegalPackageStack(packageStack)
                && PackageDataStorage.read(packageStack)
                        .map(data -> ItemPackageTransactions.canInsertPackageContents(data, target))
                        .orElse(false);
    }

    /** Commits a previously-reserved package into its destination without consuming the held stack itself. */
    public static boolean unpackHeldPackage(ItemStack packageStack, IItemHandler target) {
        return PackageItemStorage.isLegalPackageStack(packageStack)
                && PackageDataStorage.read(packageStack)
                        .map(data -> ItemPackageTransactions.canInsertPackageContents(data, target)
                                && ItemPackageTransactions.insertPackageContents(data, target, false))
                        .orElse(false);
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

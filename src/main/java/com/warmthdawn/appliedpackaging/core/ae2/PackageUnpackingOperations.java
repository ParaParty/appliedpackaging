package com.warmthdawn.appliedpackaging.core.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import com.warmthdawn.appliedpackaging.core.item_handler.PackageContentsInserter;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public final class PackageUnpackingOperations {
    private PackageUnpackingOperations() {
    }

    /**
     * Removes one complete package from network storage and returns it to the caller as a locally-held work item.
     * The destination is simulated before extraction so a bus does not reserve packages that are already blocked.
     * The caller owns the returned stack and must either unpack it later or return/drop it.
     */
    public static ItemStack reserveOnePackage(
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
            if (!canUnpack(packageStack, target)
                    || source.extract(key, 1, Actionable.SIMULATE, actionSource) != 1) {
                continue;
            }
            if (source.extract(key, 1, Actionable.MODULATE, actionSource) == 1) {
                return packageStack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean canUnpack(ItemStack packageStack, IItemHandler target) {
        return PackageItemStorage.isLegalPackageStack(packageStack)
                && PackageDataStorage.read(packageStack)
                        .map(data -> PackageContentsInserter.canInsert(data, target))
                        .orElse(false);
    }

    /** Pushes a previously-reserved package into its destination without consuming the held stack itself. */
    public static boolean unpack(ItemStack packageStack, IItemHandler target) {
        return PackageItemStorage.isLegalPackageStack(packageStack)
                && PackageDataStorage.read(packageStack)
                        .map(data -> PackageContentsInserter.insert(data, target))
                        .orElse(false);
    }
}

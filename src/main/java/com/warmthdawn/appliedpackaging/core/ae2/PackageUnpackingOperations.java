package com.warmthdawn.appliedpackaging.core.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.core.item_handler.PackageContentsInserter;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                        .map(data -> (!blockingMode || !containsPackageInput(data.contents(), target))
                                && PackageContentsInserter.canInsert(data, target))
                        .orElse(false);
    }

    /** Pushes a locally-held package into its destination without consuming the held stack itself. */
    public static boolean unpack(ItemStack packageStack, IItemHandler target) {
        return unpack(packageStack, target, false);
    }

    public static boolean unpack(ItemStack packageStack, IItemHandler target, boolean blockingMode) {
        return PackageItemStorage.isLegalPackageStack(packageStack)
                && PackageDataStorage.read(packageStack)
                        .map(data -> (!blockingMode || !containsPackageInput(data.contents(), target))
                                && PackageContentsInserter.insert(data, target))
                        .orElse(false);
    }

    /** Mirrors Pattern Provider blocking: any target stack matching a package input type blocks the push. */
    private static boolean containsPackageInput(List<GenericStack> contents, IItemHandler target) {
        Set<AEKey> inputs = new HashSet<>();
        for (var content : contents) {
            if (content.what() instanceof AEItemKey itemKey) {
                inputs.add(itemKey.dropSecondary());
            }
        }
        if (inputs.isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < target.getSlots(); slot++) {
            ItemStack targetStack = target.getStackInSlot(slot);
            if (!targetStack.isEmpty() && inputs.contains(AEItemKey.of(targetStack).dropSecondary())) {
                return true;
            }
        }
        return false;
    }
}

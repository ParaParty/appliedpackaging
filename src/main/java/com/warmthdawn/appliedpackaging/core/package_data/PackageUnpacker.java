package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;

public final class PackageUnpacker {
    private PackageUnpacker() {
    }

    public static boolean canUnpackAsItems(ItemStack packageStack) {
        return PackageDataStorage.read(packageStack)
                .flatMap(data -> itemDrops(data, 1))
                .isPresent();
    }

    public static boolean unpackStackToPlayer(Player player, ItemStack packageStack) {
        int packageCount = packageStack.getCount();
        Optional<List<ItemStack>> drops = PackageDataStorage.read(packageStack)
                .flatMap(data -> itemDrops(data, packageCount));
        if (drops.isEmpty()) {
            return false;
        }

        if (!player.level().isClientSide) {
            packageStack.shrink(packageCount);
            for (ItemStack drop : drops.get()) {
                ItemHandlerHelper.giveItemToPlayer(player, drop.copy());
            }
        }
        return true;
    }

    public static boolean unpackStackToWorld(Level level, Vec3 position, ItemStack packageStack) {
        Optional<List<ItemStack>> drops = PackageDataStorage.read(packageStack)
                .flatMap(data -> itemDrops(data, packageStack.getCount()));
        if (drops.isEmpty()) {
            return false;
        }

        if (!level.isClientSide) {
            for (ItemStack drop : drops.get()) {
                Containers.dropItemStack(level, position.x, position.y, position.z, drop.copy());
            }
        }
        return true;
    }

    private static Optional<List<ItemStack>> itemDrops(PackageData data, int packageCount) {
        if (packageCount <= 0) {
            return Optional.empty();
        }

        List<ItemStack> drops = new ArrayList<>();
        for (GenericStack entry : data.contents()) {
            if (!AEItemKey.is(entry.what())) {
                return Optional.empty();
            }
            AEItemKey key = (AEItemKey) entry.what();
            long remaining = entry.amount() * (long) packageCount;
            while (remaining > 0) {
                int amount = (int) Math.min(remaining, key.getMaxStackSize());
                drops.add(key.toStack(amount));
                remaining -= amount;
            }
        }
        return drops.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(drops));
    }
}

package com.warmthdawn.appliedpackaging.core.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.MEStorage;
import appeng.capabilities.Capabilities;
import appeng.me.storage.CompositeStorage;
import appeng.parts.automation.StackWorldBehaviors;
import com.google.common.util.concurrent.Runnables;
import java.util.IdentityHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Unpacking destination resolved through the same capability chain as AE2's Pattern Provider.
 */
public final class PackageUnpackingTarget {
    private final MEStorage storage;
    private final IActionSource source;

    private PackageUnpackingTarget(MEStorage storage, IActionSource source) {
        this.storage = storage;
        this.source = source;
    }

    public static PackageUnpackingTarget get(
            ServerLevel level,
            BlockPos pos,
            BlockEntity blockEntity,
            Direction side,
            IActionSource source) {
        if (blockEntity == null) {
            return null;
        }

        MEStorage storage = blockEntity.getCapability(Capabilities.STORAGE, side).orElse(null);
        if (storage != null) {
            return new PackageUnpackingTarget(storage, source);
        }

        var strategies = StackWorldBehaviors.createExternalStorageStrategies(level, pos, side);
        var externalStorages = new IdentityHashMap<AEKeyType, MEStorage>(2);
        for (var entry : strategies.entrySet()) {
            MEStorage wrapper = entry.getValue().createWrapper(false, Runnables.doNothing());
            if (wrapper != null) {
                externalStorages.put(entry.getKey(), wrapper);
            }
        }
        if (externalStorages.isEmpty()) {
            return null;
        }
        return new PackageUnpackingTarget(new CompositeStorage(externalStorages), source);
    }

    public static PackageUnpackingTarget of(MEStorage storage, IActionSource source) {
        return new PackageUnpackingTarget(storage, source);
    }

    public long insert(AEKey what, long amount, Actionable mode) {
        return storage.insert(what, amount, mode, source);
    }

    public boolean isEmpty() {
        return storage.getAvailableStacks().isEmpty();
    }
}

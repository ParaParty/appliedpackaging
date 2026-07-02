package com.warmthdawn.appliedpackaging.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface InventoryDroppingBlockEntity {
    void dropContents(Level level, BlockPos pos);
}

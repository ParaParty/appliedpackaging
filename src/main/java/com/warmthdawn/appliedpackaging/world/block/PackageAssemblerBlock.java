package com.warmthdawn.appliedpackaging.world.block;

import com.warmthdawn.appliedpackaging.world.block.entity.PackageAssemblerBlockEntity;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PackageAssemblerBlock extends AbstractHorizontalMachineBlock {
    public PackageAssemblerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PackageAssemblerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, APBlockEntities.PACKAGE_ASSEMBLER.get(), PackageAssemblerBlockEntity::serverTick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            dropInventory(level, pos, level.getBlockEntity(pos));
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}

package com.warmthdawn.appliedpackaging.world.block;

import com.warmthdawn.appliedpackaging.world.block.entity.PackageAssemblerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
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
}

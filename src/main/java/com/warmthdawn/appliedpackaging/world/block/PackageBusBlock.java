package com.warmthdawn.appliedpackaging.world.block;

import com.warmthdawn.appliedpackaging.world.block.entity.bus.AbstractPackageBusBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.PackageExportBusBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.PackageStorageBusBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.PackageUnpackingBusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PackageBusBlock extends AbstractHorizontalMachineBlock {
    private final BusKind kind;

    public PackageBusBlock(BlockBehaviour.Properties properties, BusKind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case STORAGE -> new PackageStorageBusBlockEntity(pos, state);
            case EXPORT -> new PackageExportBusBlockEntity(pos, state);
            case UNPACKING -> new PackageUnpackingBusBlockEntity(pos, state);
        };
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof AbstractPackageBusBlockEntity bus) {
                bus.serverTick();
            }
        };
    }

    public enum BusKind {
        STORAGE,
        EXPORT,
        UNPACKING
    }
}

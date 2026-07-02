package com.warmthdawn.appliedpackaging.world.block.entity.bus;

import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

public class PackageExportBusBlockEntity extends AbstractPackageBusBlockEntity {
    public PackageExportBusBlockEntity(BlockPos pos, BlockState blockState) {
        super(APBlockEntities.PACKAGE_EXPORT_BUS.get(), pos, blockState);
    }

    @Override
    protected void tickNetwork() {
        exportOnePackageToTarget(Component.translatable("block.appliedpackaging.package_export_bus"));
    }
}

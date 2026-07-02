package com.warmthdawn.appliedpackaging.world.block.entity.bus;

import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import com.warmthdawn.appliedpackaging.core.ae2.PackageItemStorage;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;

public class PackageStorageBusBlockEntity extends AbstractPackageBusBlockEntity implements IStorageProvider {
    public PackageStorageBusBlockEntity(BlockPos pos, BlockState blockState) {
        super(APBlockEntities.PACKAGE_STORAGE_BUS.get(), pos, blockState);
        getMainNode().addService(IStorageProvider.class, this);
    }

    @Override
    protected void tickNetwork() {
        IStorageProvider.requestUpdate(getMainNode());
    }

    @Override
    public void mountInventories(IStorageMounts storageMounts) {
        Optional<IItemHandler> target = findTargetItemHandler();
        target.ifPresent(handler -> storageMounts.mount(
                new PackageItemStorage(handler, Component.translatable("block.appliedpackaging.package_storage_bus"))));
    }
}

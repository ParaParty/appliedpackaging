package com.warmthdawn.appliedpackaging.world.block.entity.bus;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import com.warmthdawn.appliedpackaging.core.ae2.PackageItemStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackageTransactions;
import com.warmthdawn.appliedpackaging.world.block.AbstractHorizontalMachineBlock;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public abstract class AbstractPackageBusBlockEntity extends AENetworkBlockEntity {
    private int tickCounter;

    protected AbstractPackageBusBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        getMainNode().setIdlePowerUsage(1.0D);
    }

    public void serverTick() {
        tickCounter++;
        if (tickCounter % 10 == 0) {
            tickNetwork();
        }
    }

    @Override
    public AECableType getCableConnectionType(Direction side) {
        return AECableType.SMART;
    }

    protected abstract void tickNetwork();

    protected Optional<IItemHandler> findTargetItemHandler() {
        if (level == null) {
            return Optional.empty();
        }
        Direction facing = getBlockState().getValue(AbstractHorizontalMachineBlock.FACING);
        Direction targetDirection = facing.getOpposite();
        BlockEntity targetBlockEntity = level.getBlockEntity(worldPosition.relative(targetDirection));
        if (targetBlockEntity == null) {
            return Optional.empty();
        }
        LazyOptional<IItemHandler> capability = targetBlockEntity.getCapability(
                ForgeCapabilities.ITEM_HANDLER,
                targetDirection.getOpposite());
        return capability.resolve();
    }

    protected Optional<IStorageService> storageService() {
        if (!getMainNode().isOnline()) {
            return Optional.empty();
        }
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return Optional.empty();
        }
        return Optional.of(grid.getStorageService());
    }

    protected IActionSource actionSource() {
        return IActionSource.ofMachine(this);
    }

    protected boolean exportOnePackageToTarget(Component description) {
        Optional<IItemHandler> target = findTargetItemHandler();
        Optional<IStorageService> storage = storageService();
        if (target.isEmpty() || storage.isEmpty()) {
            return false;
        }

        for (var entry : storage.get().getCachedInventory()) {
            AEKey key = entry.getKey();
            if (!PackageItemStorage.isPackageKey(key) || entry.getLongValue() <= 0) {
                continue;
            }
            ItemStack packageStack = key.wrapForDisplayOrFilter();
            packageStack.setCount(1);
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target.get(), packageStack, true);
            if (!remainder.isEmpty()) {
                continue;
            }
            long extracted = storage.get().getInventory().extract(key, 1, Actionable.MODULATE, actionSource());
            if (extracted == 1) {
                ItemHandlerHelper.insertItemStacked(target.get(), packageStack, false);
                return true;
            }
        }
        return false;
    }

    protected boolean unpackOnePackageToTarget(Component description) {
        Optional<IItemHandler> target = findTargetItemHandler();
        Optional<IStorageService> storage = storageService();
        if (target.isEmpty() || storage.isEmpty()) {
            return false;
        }

        for (var entry : storage.get().getCachedInventory()) {
            AEKey key = entry.getKey();
            if (!PackageItemStorage.isPackageKey(key) || entry.getLongValue() <= 0) {
                continue;
            }
            ItemStack packageStack = key.wrapForDisplayOrFilter();
            packageStack.setCount(1);
            Optional<PackageData> data = PackageDataStorage.read(packageStack);
            if (data.isEmpty() || !ItemPackageTransactions.canInsertPackageContents(data.get(), target.get())) {
                continue;
            }
            long extracted = storage.get().getInventory().extract(key, 1, Actionable.MODULATE, actionSource());
            if (extracted == 1 && ItemPackageTransactions.insertPackageContents(data.get(), target.get(), false)) {
                return true;
            }
        }
        return false;
    }
}

package com.warmthdawn.appliedpackaging.world.block;

import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.AbstractPackageBusBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.PackageExportBusBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.PackageStorageBusBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.PackageUnpackingBusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

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
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        boolean shouldClear = player.isShiftKeyDown() && held.isEmpty();
        boolean shouldSet = !held.isEmpty() && PackageFilter.fromTemplate(held).isPresent();
        if (level.isClientSide) {
            return shouldClear || shouldSet || held.isEmpty() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AbstractPackageBusBlockEntity bus)) {
            return InteractionResult.PASS;
        }
        if (shouldClear) {
            bus.clearFilterTemplate();
            player.displayClientMessage(Component.translatable("message.appliedpackaging.package_bus.filter_cleared"), true);
            return InteractionResult.CONSUME;
        }
        if (shouldSet) {
            bus.setFilterTemplate(held);
            player.displayClientMessage(Component.translatable("message.appliedpackaging.package_bus.filter_set"), true);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, bus, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
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

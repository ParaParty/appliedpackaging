package com.warmthdawn.appliedpackaging.world.block;

import com.warmthdawn.appliedpackaging.world.block.entity.terminal.PackagePatternTerminalBlockEntity;
import com.warmthdawn.appliedpackaging.world.menu.PackagePatternTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public class PackagePatternTerminalBlock extends AbstractHorizontalMachineBlock {
    private static final VoxelShape NORTH_SHAPE = box(1, 1, 0, 15, 15, 7);
    private static final VoxelShape SOUTH_SHAPE = box(1, 1, 9, 15, 15, 16);
    private static final VoxelShape WEST_SHAPE = box(0, 1, 1, 7, 15, 15);
    private static final VoxelShape EAST_SHAPE = box(9, 1, 1, 16, 15, 15);

    public PackagePatternTerminalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PackagePatternTerminalBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PackagePatternTerminalBlockEntity terminal && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, terminal, buffer -> PackagePatternTerminalMenu.writeBlockHost(buffer, pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            dropInventory(level, pos, level.getBlockEntity(pos));
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}

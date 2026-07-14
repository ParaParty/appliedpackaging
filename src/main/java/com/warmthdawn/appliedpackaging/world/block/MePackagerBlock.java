package com.warmthdawn.appliedpackaging.world.block;

import appeng.api.orientation.IOrientableBlock;
import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public class MePackagerBlock extends AbstractHorizontalMachineBlock implements IOrientableBlock {
    private static final double BELT_MIN_X = 1.0 / 16.0;
    private static final double BELT_MAX_X = 1.0;
    private static final double BELT_MIN_Z = 2.0 / 16.0;
    private static final double BELT_MAX_Z = 14.0 / 16.0;
    private static final double BELT_TOP_Y = 2.0 / 16.0;
    private static final double HIT_EPSILON = 1.0E-4;
    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(0, 0, 0, 16, 1, 16),
            Block.box(0, 1, 0, 4, 16, 16),
            Block.box(4, 1, 0, 16, 3, 2),
            Block.box(4, 1, 14, 16, 3, 16),
            Block.box(1, 1, 2, 16, 2, 14)).optimize();
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(0, 0, 0, 16, 1, 16),
            Block.box(0, 1, 0, 16, 16, 4),
            Block.box(0, 1, 4, 2, 3, 16),
            Block.box(14, 1, 4, 16, 3, 16),
            Block.box(2, 1, 1, 14, 2, 16)).optimize();
    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(0, 0, 0, 16, 1, 16),
            Block.box(12, 1, 0, 16, 16, 16),
            Block.box(0, 1, 0, 12, 3, 2),
            Block.box(0, 1, 14, 12, 3, 16),
            Block.box(0, 1, 2, 15, 2, 14)).optimize();
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(0, 0, 0, 16, 1, 16),
            Block.box(0, 1, 12, 16, 16, 16),
            Block.box(0, 1, 0, 2, 3, 12),
            Block.box(14, 1, 0, 16, 3, 12),
            Block.box(2, 1, 0, 14, 2, 15)).optimize();
    public MePackagerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.horizontalFacing();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForFacing(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForFacing(state.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MePackagerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, APBlockEntities.ME_PACKAGER.get(), (tickLevel, pos, tickState, packager) ->
                packager.tick());
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
        if (!(blockEntity instanceof MePackagerBlockEntity packager)) {
            return InteractionResult.PASS;
        }

        if (isBeltSurfaceHit(state, pos, hit)) {
            ItemStack held = player.getItemInHand(hand);
            MePackagerBlockEntity.ActionResult result = packager.interact(player, held);
            if (result.messageKey() != null) {
                player.displayClientMessage(Component.translatable(result.messageKey()), true);
            }
            return result.consumed() ? InteractionResult.CONSUME : InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            NetworkHooks.openScreen(serverPlayer, packager, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MePackagerBlockEntity packager) {
            packager.updatePowered(level.hasNeighborSignal(pos));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            dropInventory(level, pos, level.getBlockEntity(pos));
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    private static VoxelShape shapeForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case NORTH -> SHAPE_NORTH;
            default -> SHAPE_EAST;
        };
    }

    static boolean isBeltSurfaceHit(BlockState state, BlockPos pos, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP) {
            return false;
        }

        double worldX = hit.getLocation().x - pos.getX();
        double worldY = hit.getLocation().y - pos.getY();
        double worldZ = hit.getLocation().z - pos.getZ();
        if (Math.abs(worldY - BELT_TOP_Y) > HIT_EPSILON) {
            return false;
        }

        double modelX;
        double modelZ;
        switch (state.getValue(FACING)) {
            case SOUTH -> {
                modelX = worldZ;
                modelZ = 1.0 - worldX;
            }
            case WEST -> {
                modelX = 1.0 - worldX;
                modelZ = 1.0 - worldZ;
            }
            case NORTH -> {
                modelX = 1.0 - worldZ;
                modelZ = worldX;
            }
            default -> {
                modelX = worldX;
                modelZ = worldZ;
            }
        }

        return modelX >= BELT_MIN_X - HIT_EPSILON
                && modelX <= BELT_MAX_X + HIT_EPSILON
                && modelZ >= BELT_MIN_Z - HIT_EPSILON
                && modelZ <= BELT_MAX_Z + HIT_EPSILON;
    }
}

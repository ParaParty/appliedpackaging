package com.warmthdawn.appliedpackaging.world.block;

import appeng.util.InteractionUtil;
import com.warmthdawn.appliedpackaging.core.sequence_buffer.SequenceBufferTopology;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.world.block.entity.SequenceBufferBlockEntity;
import com.warmthdawn.appliedpackaging.world.menu.SequenceBufferMainMenu;
import com.warmthdawn.appliedpackaging.world.menu.SequenceBufferSideMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class SequenceBufferBlock extends BaseEntityBlock {
    public static final EnumProperty<SequenceBufferVisualState> STATE =
            EnumProperty.create("state", SequenceBufferVisualState.class);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty DIRECTIONAL = BooleanProperty.create("directional");
    public static final DirectionProperty SEQUENCE_DIRECTION = DirectionProperty.create("sequence_direction");
    public static final EnumProperty<Direction.Axis> AXIS =
            EnumProperty.create("axis", Direction.Axis.class);
    public static final BooleanProperty TAIL = BooleanProperty.create("tail");

    public SequenceBufferBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(STATE, SequenceBufferVisualState.UNFORMED)
                .setValue(FACING, Direction.NORTH)
                .setValue(DIRECTIONAL, false)
                .setValue(SEQUENCE_DIRECTION, Direction.NORTH)
                .setValue(AXIS, Direction.Axis.X)
                .setValue(TAIL, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SequenceBufferBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, APBlockEntities.SEQUENCE_BUFFER.get(), SequenceBufferBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (InteractionUtil.canWrenchRotate(player.getItemInHand(hand))) {
            Direction clickedSide = hit.getDirection();
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            return handleWrench((ServerLevel) level, pos, state, clickedSide)
                    ? InteractionResult.CONSUME
                    : InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.connection == null
                || !(level.getBlockEntity(pos) instanceof SequenceBufferBlockEntity viewed)) {
            return InteractionResult.PASS;
        }
        SequenceBufferBlockEntity authority = SequenceBufferTopology.resolveEndpoint(viewed).orElse(viewed);
        if (viewed.isEndpoint()) {
            var provider = new SimpleMenuProvider(
                    (containerId, inventory, ignored) ->
                            new SequenceBufferMainMenu(containerId, inventory, viewed),
                    net.minecraft.network.chat.Component.translatable(
                            "gui.appliedpackaging.sequence_buffer.main"));
            NetworkHooks.openScreen(serverPlayer, provider, buffer -> buffer.writeBlockPos(viewed.getBlockPos()));
        } else {
            var provider = new SimpleMenuProvider(
                    (containerId, inventory, ignored) ->
                            new SequenceBufferSideMenu(containerId, inventory, authority, viewed),
                    net.minecraft.network.chat.Component.translatable(
                            "gui.appliedpackaging.sequence_buffer.side"));
            NetworkHooks.openScreen(serverPlayer, provider, buffer -> {
                buffer.writeBlockPos(authority.getBlockPos());
                buffer.writeBlockPos(viewed.getBlockPos());
            });
        }
        return InteractionResult.CONSUME;
    }

    private static boolean handleWrench(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            Direction clickedSide) {
        SequenceBufferVisualState visual = state.getValue(STATE);
        if (visual == SequenceBufferVisualState.UNFORMED
                || visual == SequenceBufferVisualState.UNFORMED_DIRECTED) {
            BlockState updated = cycleDirection(
                    state,
                    hasOwnDirection(state),
                    SequenceBufferVisualState.UNFORMED,
                    SequenceBufferVisualState.UNFORMED_DIRECTED,
                    clickedSide);
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
            SequenceBufferTopology.scheduleAround(level, pos);
            return true;
        }
        if (visual == SequenceBufferVisualState.ENDPOINT) {
            BlockState desired = cycleDirection(
                    state,
                    hasOwnDirection(state),
                    SequenceBufferVisualState.UNFORMED,
                    SequenceBufferVisualState.UNFORMED_DIRECTED,
                    clickedSide);
            SequenceBufferTopology.disassembleEndpoint(level, pos);
            BlockState detached = level.getBlockState(pos);
            level.setBlock(
                    pos,
                    detached.setValue(STATE, desired.getValue(STATE))
                            .setValue(FACING, desired.getValue(FACING))
                            .setValue(DIRECTIONAL, desired.getValue(DIRECTIONAL)),
                    Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
            SequenceBufferTopology.scheduleAround(level, pos);
            return true;
        }
        Direction.Axis structureAxis = state.getValue(AXIS);
        if (clickedSide.getAxis() == structureAxis) {
            return false;
        }
        level.setBlock(
                pos,
                cycleDirection(
                        state,
                        hasOwnDirection(state),
                        SequenceBufferVisualState.MEMBER,
                        SequenceBufferVisualState.MEMBER_DIRECTED,
                        clickedSide),
                Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
        return true;
    }

    private static BlockState cycleDirection(
            BlockState state,
            boolean hasDirection,
            SequenceBufferVisualState neutralState,
            SequenceBufferVisualState directedState,
            Direction clickedSide) {
        Direction oppositeSide = clickedSide.getOpposite();
        if (!hasDirection || (state.getValue(FACING) != oppositeSide
                && state.getValue(FACING) != clickedSide)) {
            return state.setValue(STATE, directedState)
                    .setValue(FACING, oppositeSide)
                    .setValue(DIRECTIONAL, true);
        }
        if (state.getValue(FACING) == oppositeSide) {
            return state.setValue(STATE, directedState)
                    .setValue(FACING, clickedSide)
                    .setValue(DIRECTIONAL, true);
        }
        return state.setValue(STATE, neutralState).setValue(DIRECTIONAL, false);
    }

    public static boolean hasOwnDirection(BlockState state) {
        return state.getValue(DIRECTIONAL) || state.getValue(STATE).isDirected();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            SequenceBufferTopology.scheduleAround(serverLevel, pos);
        }
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            SequenceBufferTopology.schedule(serverLevel, pos);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        SequenceBufferTopology.reconcile(level, pos);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        if (level instanceof ServerLevel serverLevel) {
            SequenceBufferTopology.schedule(serverLevel, pos);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            SequenceBufferTopology.onBlockRemoved(serverLevel, pos, state);
            if (level.getBlockEntity(pos) instanceof SequenceBufferBlockEntity sequenceBuffer) {
                sequenceBuffer.dropStoredContents();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATE, FACING, DIRECTIONAL, SEQUENCE_DIRECTION, AXIS, TAIL);
    }
}

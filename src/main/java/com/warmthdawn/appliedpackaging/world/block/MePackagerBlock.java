package com.warmthdawn.appliedpackaging.world.block;

import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ToolAction;

public class MePackagerBlock extends AbstractHorizontalMachineBlock {
    public static final DirectionProperty NETWORK_SIDE = DirectionProperty.create("network_side");
    private static final TagKey<Item> WRENCHES =
            TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/wrench"));
    private static final ToolAction WRENCH_ACTION = ToolAction.get("wrench");
    private static final ToolAction WRENCH_ROTATE_ACTION = ToolAction.get("wrench_rotate");

    public MePackagerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(NETWORK_SIDE, Direction.SOUTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(NETWORK_SIDE, context.getClickedFace().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NETWORK_SIDE);
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

        ItemStack held = player.getItemInHand(hand);
        if (isWrench(held)) {
            Direction side = hit.getDirection();
            level.setBlock(pos, state.setValue(NETWORK_SIDE, side), Block.UPDATE_ALL);
            packager.onNetworkSideChanged();
            packager.setChanged();
            player.displayClientMessage(
                    Component.translatable(
                            "message.appliedpackaging.me_packager.network_side_set",
                            Component.translatable("direction.appliedpackaging." + side.getName())),
                    true);
            return InteractionResult.CONSUME;
        }

        MePackagerBlockEntity.ActionResult result = packager.interact(player, held);
        if (result.messageKey() != null) {
            player.displayClientMessage(Component.translatable(result.messageKey()), true);
        }
        return result.consumed() ? InteractionResult.CONSUME : InteractionResult.PASS;
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && !state.is(oldState.getBlock())
                && level.getBlockEntity(pos) instanceof MePackagerBlockEntity packager) {
            packager.onNetworkSideChanged();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            dropInventory(level, pos, level.getBlockEntity(pos));
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    public static Direction networkSide(BlockState state) {
        if (state.hasProperty(NETWORK_SIDE)) {
            return state.getValue(NETWORK_SIDE);
        }
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
        return facing.getOpposite();
    }

    private static boolean isWrench(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(WRENCHES)
                || stack.canPerformAction(WRENCH_ACTION)
                || stack.canPerformAction(WRENCH_ROTATE_ACTION)) {
            return true;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "create".equals(id.getNamespace()) && "wrench".equals(id.getPath())
                || "ae2".equals(id.getNamespace()) && id.getPath().endsWith("_wrench");
    }
}

package com.warmthdawn.appliedpackaging.world.block;

import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MePackagerBlock extends AbstractHorizontalMachineBlock {
    public MePackagerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MePackagerBlockEntity(pos, state);
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
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            dropInventory(level, pos, level.getBlockEntity(pos));
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}

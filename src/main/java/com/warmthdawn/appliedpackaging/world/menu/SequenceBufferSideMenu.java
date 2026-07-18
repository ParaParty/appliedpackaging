package com.warmthdawn.appliedpackaging.world.menu;

import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.world.block.entity.SequenceBufferBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SequenceBufferSideMenu extends AbstractSequenceBufferMenu {
    public SequenceBufferSideMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, readContext(inventory, buffer));
    }

    private SequenceBufferSideMenu(int containerId, Inventory inventory, OpenContext context) {
        this(containerId, inventory, context.authority(), context.viewed());
    }

    public SequenceBufferSideMenu(
            int containerId,
            Inventory inventory,
            SequenceBufferBlockEntity authority,
            SequenceBufferBlockEntity viewed) {
        super(APMenus.SEQUENCE_BUFFER_SIDE.get(), containerId, inventory, authority, viewed);
    }

    @Override
    protected void setupInventorySlots() {
        addStorageDisplaySlots(1);
    }

    @Override
    protected int currentMemberCount() {
        return 1;
    }

    @Override
    protected int memberIndexForVisibleSlot(int visibleIndex) {
        return visibleIndex;
    }

    @Override
    protected SequenceBufferBlockEntity memberForVisibleSlot(int visibleIndex) {
        return visibleIndex == 0 ? viewedBlock() : null;
    }

    @Override
    protected int insertShiftClickedItem(ItemStack stack) {
        return viewedBlock().insertMenuItem(stack, stack.getCount(), false);
    }

    private static OpenContext readContext(Inventory inventory, FriendlyByteBuf buffer) {
        SequenceBufferBlockEntity authority = getBlockEntity(inventory, buffer.readBlockPos());
        SequenceBufferBlockEntity viewed = getBlockEntity(inventory, buffer.readBlockPos());
        return new OpenContext(authority, viewed);
    }

    private static SequenceBufferBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof SequenceBufferBlockEntity sequenceBuffer) {
            return sequenceBuffer;
        }
        throw new IllegalStateException("Expected Sequence Buffer block entity at " + pos);
    }

    private record OpenContext(
            SequenceBufferBlockEntity authority,
            SequenceBufferBlockEntity viewed) {
    }
}

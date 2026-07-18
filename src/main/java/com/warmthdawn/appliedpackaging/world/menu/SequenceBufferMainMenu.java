package com.warmthdawn.appliedpackaging.world.menu;

import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.world.block.entity.SequenceBufferBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SequenceBufferMainMenu extends AbstractSequenceBufferMenu {
    public static final int COLUMNS = 9;
    public static final int VISIBLE_ROWS = 3;
    public static final int VISIBLE_SLOT_COUNT = COLUMNS * VISIBLE_ROWS;
    public static final int BUTTON_SCROLL_BASE = 100;

    public SequenceBufferMainMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, getBlockEntity(inventory, buffer.readBlockPos()));
    }

    public SequenceBufferMainMenu(
            int containerId,
            Inventory inventory,
            SequenceBufferBlockEntity authority) {
        super(APMenus.SEQUENCE_BUFFER_MAIN.get(), containerId, inventory, authority, authority, true);
    }

    @Override
    protected void setupInventorySlots() {
        addStorageDisplaySlots(VISIBLE_SLOT_COUNT);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_SCROLL_BASE && id <= BUTTON_SCROLL_BASE + maxScrollOffset()) {
            setScrollOffset(id - BUTTON_SCROLL_BASE);
            return true;
        }
        return false;
    }

    @Override
    public int maxScrollOffset() {
        return maxScrollOffsetForMemberCount(memberCount());
    }

    public static int maxScrollOffsetForMemberCount(int memberCount) {
        int rows = (Math.max(0, memberCount) + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - VISIBLE_ROWS);
    }

    @Override
    protected int currentMemberCount() {
        return getHost().storageMemberCount();
    }

    @Override
    protected int memberIndexForVisibleSlot(int visibleIndex) {
        return scrollOffset() * COLUMNS + visibleIndex;
    }

    @Override
    protected SequenceBufferBlockEntity memberForVisibleSlot(int visibleIndex) {
        return getHost().storageMemberAt(memberIndexForVisibleSlot(visibleIndex));
    }

    @Override
    protected int insertShiftClickedItem(ItemStack stack) {
        for (int slot = 0; slot < getHost().storageMemberCount(); slot++) {
            SequenceBufferBlockEntity member = getHost().storageMemberAt(slot);
            if (member == null || !member.isEmpty()) {
                continue;
            }
            return member.insertMenuItem(stack, stack.getCount(), false);
        }
        return 0;
    }

    private static SequenceBufferBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof SequenceBufferBlockEntity sequenceBuffer && sequenceBuffer.isEndpoint()) {
            return sequenceBuffer;
        }
        throw new IllegalStateException("Expected Sequence Buffer endpoint at " + pos);
    }
}

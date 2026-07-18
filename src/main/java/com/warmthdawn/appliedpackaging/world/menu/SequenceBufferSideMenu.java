package com.warmthdawn.appliedpackaging.world.menu;

import com.warmthdawn.appliedpackaging.core.sequence_buffer.SequenceBufferTopology;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.world.block.entity.SequenceBufferBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkHooks;

public final class SequenceBufferSideMenu extends AbstractSequenceBufferMenu {
    private static final String ACTION_OPEN_MAIN = "openMain";

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
        super(APMenus.SEQUENCE_BUFFER_SIDE.get(), containerId, inventory, authority, viewed, false);
        registerClientAction(ACTION_OPEN_MAIN, this::openMain);
    }

    public boolean canOpenMain() {
        return getHost().isEndpoint()
                && !getHost().getBlockPos().equals(viewedBlock().getBlockPos());
    }

    public void openMain() {
        if (!canOpenMain()) {
            return;
        }
        if (isClientSide()) {
            sendClientAction(ACTION_OPEN_MAIN);
            return;
        }
        if (!(getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        SequenceBufferBlockEntity endpoint = getHost();
        if (SequenceBufferTopology.resolveEndpoint(viewedBlock())
                .filter(resolved -> resolved.getBlockPos().equals(endpoint.getBlockPos()))
                .isEmpty()) {
            return;
        }
        var provider = new SimpleMenuProvider(
                (containerId, inventory, ignored) ->
                        new SequenceBufferMainMenu(containerId, inventory, endpoint),
                Component.translatable("gui.appliedpackaging.sequence_buffer.main"));
        NetworkHooks.openScreen(
                serverPlayer,
                provider,
                buffer -> buffer.writeBlockPos(endpoint.getBlockPos()));
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

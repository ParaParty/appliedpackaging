package com.warmthdawn.appliedpackaging.world.menu;

import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.AbstractPackageBusBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PackageBusMenu extends AbstractContainerMenu {
    public static final int BUTTON_SET_FROM_CURSOR = 0;
    public static final int BUTTON_CLEAR_FILTER = 1;
    public static final int BUTTON_COLOR_BASE = 10;
    public static final int BUTTON_CONTENT_AMOUNT_INCREASE_BASE = 40;
    public static final int BUTTON_CONTENT_AMOUNT_DECREASE_BASE =
            BUTTON_CONTENT_AMOUNT_INCREASE_BASE + AbstractPackageBusBlockEntity.REQUIRED_CONTENT_SLOT_COUNT;
    public static final int FILTER_DISPLAY_SLOT = 0;
    public static final int MARKER_FILTER_SLOT = 1;
    public static final int CONTENT_FILTER_START = 2;
    public static final int CONTENT_FILTER_END =
            CONTENT_FILTER_START + AbstractPackageBusBlockEntity.REQUIRED_CONTENT_SLOT_COUNT;
    public static final int PLAYER_INVENTORY_START = CONTENT_FILTER_END;
    public static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    public static final int HOTBAR_START = PLAYER_INVENTORY_END;
    public static final int HOTBAR_END = HOTBAR_START + 9;

    private final AbstractPackageBusBlockEntity blockEntity;
    private final SimpleContainer filterDisplay = new SimpleContainer(1);
    private final SimpleContainer markerDisplay = new SimpleContainer(1);
    private final SimpleContainer contentDisplay =
            new SimpleContainer(AbstractPackageBusBlockEntity.REQUIRED_CONTENT_SLOT_COUNT);
    private final boolean clientSide;
    private int syncedSelectedColor;
    private final int[] syncedContentAmounts =
            new int[AbstractPackageBusBlockEntity.REQUIRED_CONTENT_SLOT_COUNT];
    private final DataSlot selectedColorSlot;
    private final DataSlot[] contentAmountSlots =
            new DataSlot[AbstractPackageBusBlockEntity.REQUIRED_CONTENT_SLOT_COUNT];

    public PackageBusMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buffer.readBlockPos()));
    }

    public PackageBusMenu(int containerId, Inventory playerInventory, AbstractPackageBusBlockEntity blockEntity) {
        super(APMenus.PACKAGE_BUS.get(), containerId);
        this.blockEntity = blockEntity;
        this.clientSide = playerInventory.player.level().isClientSide;
        this.syncedSelectedColor = blockEntity.filterColor().ordinal();
        this.selectedColorSlot = new DataSlot() {
            @Override
            public int get() {
                return clientSide ? syncedSelectedColor : blockEntity.filterColor().ordinal();
            }

            @Override
            public void set(int value) {
                PackageColor[] values = PackageColor.values();
                if (value >= 0 && value < values.length) {
                    syncedSelectedColor = value;
                }
            }
        };
        updateFilterDisplays();
        addSlot(new GhostDisplaySlot(filterDisplay, 0, 80, 25));
        addSlot(new GhostDisplaySlot(markerDisplay, 0, 35, 58));
        for (int slot = 0; slot < AbstractPackageBusBlockEntity.REQUIRED_CONTENT_SLOT_COUNT; slot++) {
            addSlot(new GhostDisplaySlot(contentDisplay, slot, 72 + slot * 18, 58));
        }
        addPlayerInventory(playerInventory);
        addDataSlot(selectedColorSlot);
        addContentAmountSlots();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_COLOR_BASE && id < BUTTON_COLOR_BASE + PackageColor.values().length) {
            if (!player.level().isClientSide) {
                blockEntity.setManualFilterColor(PackageColor.values()[id - BUTTON_COLOR_BASE]);
                updateFilterDisplays();
                broadcastChanges();
            }
            return true;
        }
        if (id == BUTTON_SET_FROM_CURSOR) {
            if (!player.level().isClientSide) {
                ItemStack carried = getCarried();
                if (blockEntity.setFilterTemplate(carried)) {
                    updateFilterDisplays();
                    broadcastChanges();
                    player.displayClientMessage(Component.translatable("message.appliedpackaging.package_bus.filter_set"), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.appliedpackaging.package_bus.filter_invalid"), true);
                }
            }
            return true;
        }
        if (id == BUTTON_CLEAR_FILTER) {
            if (!player.level().isClientSide) {
                blockEntity.clearFilterTemplate();
                updateFilterDisplays();
                broadcastChanges();
                player.displayClientMessage(Component.translatable("message.appliedpackaging.package_bus.filter_cleared"), true);
            }
            return true;
        }
        if (id >= BUTTON_CONTENT_AMOUNT_INCREASE_BASE
                && id < BUTTON_CONTENT_AMOUNT_INCREASE_BASE + AbstractPackageBusBlockEntity.REQUIRED_CONTENT_SLOT_COUNT) {
            if (!player.level().isClientSide) {
                blockEntity.adjustManualFilterContentAmount(id - BUTTON_CONTENT_AMOUNT_INCREASE_BASE, true);
                updateFilterDisplays();
                broadcastChanges();
            }
            return true;
        }
        if (id >= BUTTON_CONTENT_AMOUNT_DECREASE_BASE
                && id < BUTTON_CONTENT_AMOUNT_DECREASE_BASE + AbstractPackageBusBlockEntity.REQUIRED_CONTENT_SLOT_COUNT) {
            if (!player.level().isClientSide) {
                blockEntity.adjustManualFilterContentAmount(id - BUTTON_CONTENT_AMOUNT_DECREASE_BASE, false);
                updateFilterDisplays();
                broadcastChanges();
            }
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == MARKER_FILTER_SLOT) {
            clickMarkerFilter(clickType, player);
            return;
        }
        if (slotId >= CONTENT_FILTER_START && slotId < CONTENT_FILTER_END) {
            clickContentFilter(slotId - CONTENT_FILTER_START, button, clickType, player);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < PLAYER_INVENTORY_START || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        if (PackageFilter.fromTemplate(source).isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!player.level().isClientSide && blockEntity.setFilterTemplate(source)) {
            updateFilterDisplays();
            broadcastChanges();
            player.displayClientMessage(Component.translatable("message.appliedpackaging.package_bus.filter_set"), true);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity.getLevel() == null || blockEntity.isRemoved()) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void broadcastChanges() {
        if (!clientSide) {
            updateFilterDisplays();
        }
        super.broadcastChanges();
    }

    public ItemStack filterTemplate() {
        return filterDisplay.getItem(0).copy();
    }

    public PackageColor selectedColor() {
        PackageColor[] values = PackageColor.values();
        int index = selectedColorSlot.get();
        if (index < 0 || index >= values.length) {
            return PackageColor.FLUIX;
        }
        return values[index];
    }

    public ItemStack markerFilter() {
        return markerDisplay.getItem(0).copy();
    }

    public ItemStack contentFilter(int slot) {
        if (slot < 0 || slot >= contentDisplay.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return contentDisplay.getItem(slot).copy();
    }

    public int contentFilterAmount(int slot) {
        if (slot < 0 || slot >= contentAmountSlots.length) {
            return 0;
        }
        return contentAmountSlots[slot].get();
    }

    private void clickMarkerFilter(ClickType clickType, Player player) {
        if (clickType != ClickType.PICKUP) {
            return;
        }
        if (!player.level().isClientSide) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                blockEntity.clearManualFilterMarker();
            } else {
                blockEntity.setManualFilterMarker(carried);
            }
            updateFilterDisplays();
            broadcastChanges();
        }
    }

    private void clickContentFilter(int contentSlot, int button, ClickType clickType, Player player) {
        if (clickType != ClickType.PICKUP) {
            return;
        }
        if (!player.level().isClientSide) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                blockEntity.clearManualFilterContent(contentSlot);
            } else {
                blockEntity.setManualFilterContentFromGhostStack(contentSlot, carried, button == 1);
            }
            updateFilterDisplays();
            broadcastChanges();
        }
    }

    private void updateFilterDisplays() {
        filterDisplay.setItem(0, blockEntity.getFilterTemplate());
        markerDisplay.setItem(0, blockEntity.filterMarker()
                .map(marker -> displayStack(marker.stack()))
                .orElse(ItemStack.EMPTY));

        List<GenericStack> requiredContents = blockEntity.filterRequiredContents();
        for (int slot = 0; slot < contentDisplay.getContainerSize(); slot++) {
            contentDisplay.setItem(slot, slot < requiredContents.size()
                    ? displayStack(requiredContents.get(slot))
                    : ItemStack.EMPTY);
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 125 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 183));
        }
    }

    private void addContentAmountSlots() {
        for (int slot = 0; slot < contentAmountSlots.length; slot++) {
            final int contentSlot = slot;
            contentAmountSlots[contentSlot] = new DataSlot() {
                @Override
                public int get() {
                    return clientSide
                            ? syncedContentAmounts[contentSlot]
                            : blockEntity.filterRequiredContentAmountForDisplay(contentSlot);
                }

                @Override
                public void set(int value) {
                    syncedContentAmounts[contentSlot] = value;
                }
            };
            addDataSlot(contentAmountSlots[contentSlot]);
        }
    }

    private static ItemStack displayStack(GenericStack stack) {
        ItemStack display = stack.what().wrapForDisplayOrFilter();
        if (display.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int count = (int) Math.max(1L, Math.min(stack.amount(), display.getMaxStackSize()));
        display.setCount(count);
        return display;
    }

    private static AbstractPackageBusBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof AbstractPackageBusBlockEntity bus) {
            return bus;
        }
        throw new IllegalStateException("Expected Package Bus block entity at " + pos);
    }

    private static final class GhostDisplaySlot extends Slot {
        private GhostDisplaySlot(SimpleContainer container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}

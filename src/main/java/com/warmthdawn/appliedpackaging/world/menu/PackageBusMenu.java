package com.warmthdawn.appliedpackaging.world.menu;

import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.AbstractPackageBusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PackageBusMenu extends AbstractContainerMenu {
    public static final int BUTTON_SET_FROM_CURSOR = 0;
    public static final int BUTTON_CLEAR_FILTER = 1;
    public static final int FILTER_DISPLAY_SLOT = 0;
    public static final int PLAYER_INVENTORY_START = 1;
    public static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    public static final int HOTBAR_START = PLAYER_INVENTORY_END;
    public static final int HOTBAR_END = HOTBAR_START + 9;

    private final AbstractPackageBusBlockEntity blockEntity;
    private final SimpleContainer filterDisplay = new SimpleContainer(1);

    public PackageBusMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buffer.readBlockPos()));
    }

    public PackageBusMenu(int containerId, Inventory playerInventory, AbstractPackageBusBlockEntity blockEntity) {
        super(APMenus.PACKAGE_BUS.get(), containerId);
        this.blockEntity = blockEntity;
        updateFilterDisplay();
        addSlot(new FilterDisplaySlot(filterDisplay, 0, 80, 35));
        addPlayerInventory(playerInventory);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_SET_FROM_CURSOR) {
            if (!player.level().isClientSide) {
                ItemStack carried = getCarried();
                if (blockEntity.setFilterTemplate(carried)) {
                    updateFilterDisplay();
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
                updateFilterDisplay();
                broadcastChanges();
                player.displayClientMessage(Component.translatable("message.appliedpackaging.package_bus.filter_cleared"), true);
            }
            return true;
        }
        return false;
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
            updateFilterDisplay();
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

    public ItemStack filterTemplate() {
        return filterDisplay.getItem(0).copy();
    }

    private void updateFilterDisplay() {
        filterDisplay.setItem(0, blockEntity.getFilterTemplate());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 107 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 165));
        }
    }

    private static AbstractPackageBusBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof AbstractPackageBusBlockEntity bus) {
            return bus;
        }
        throw new IllegalStateException("Expected Package Bus block entity at " + pos);
    }

    private static final class FilterDisplaySlot extends Slot {
        private FilterDisplaySlot(SimpleContainer container, int slot, int x, int y) {
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

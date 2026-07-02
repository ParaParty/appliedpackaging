package com.warmthdawn.appliedpackaging.world.menu;

import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class MePackagerMenu extends AbstractContainerMenu {
    public static final int BUTTON_PACK_ONCE = 0;

    private static final int MACHINE_SLOT_COUNT = 2;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final MePackagerBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public MePackagerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buffer.readBlockPos()));
    }

    public MePackagerMenu(int containerId, Inventory playerInventory, MePackagerBlockEntity blockEntity) {
        super(APMenus.ME_PACKAGER.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        addSlot(new SlotItemHandler(blockEntity.getItems(), MePackagerBlockEntity.SLOT_INPUT, 53, 34));
        addSlot(new OutputPackageSlot(blockEntity, 116, 34));
        addPlayerInventory(playerInventory);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_PACK_ONCE) {
            return false;
        }
        if (!player.level().isClientSide) {
            MePackagerBlockEntity.MachineResult result = blockEntity.runOnce();
            player.displayClientMessage(Component.translatable(result.messageKey()), true);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.getItems().isItemValid(MePackagerBlockEntity.SLOT_INPUT, source)) {
            if (!moveItemStackTo(source, MePackagerBlockEntity.SLOT_INPUT, MePackagerBlockEntity.SLOT_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_END, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, APBlocks.ME_PACKAGER.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    private static MePackagerBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MePackagerBlockEntity packager) {
            return packager;
        }
        throw new IllegalStateException("Expected ME Packager block entity at " + pos);
    }

    private static final class OutputPackageSlot extends SlotItemHandler {
        private OutputPackageSlot(MePackagerBlockEntity blockEntity, int x, int y) {
            super(blockEntity.getItems(), MePackagerBlockEntity.SLOT_OUTPUT, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}

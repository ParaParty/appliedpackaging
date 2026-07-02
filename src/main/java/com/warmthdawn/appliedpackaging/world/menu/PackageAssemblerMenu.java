package com.warmthdawn.appliedpackaging.world.menu;

import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.world.block.entity.PackageAssemblerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class PackageAssemblerMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = 11;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final PackageAssemblerBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public PackageAssemblerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buffer.readBlockPos()));
    }

    public PackageAssemblerMenu(int containerId, Inventory playerInventory, PackageAssemblerBlockEntity blockEntity) {
        super(APMenus.PACKAGE_ASSEMBLER.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        addInputSlots();
        addSlot(new SlotItemHandler(blockEntity.getItems(), PackageAssemblerBlockEntity.SLOT_PATTERN, 116, 24));
        addSlot(new OutputSlot(blockEntity, 144, 24));
        addPlayerInventory(playerInventory);
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
        } else if (source.is(APItems.PACKAGE_PATTERN.get()) || source.is(APItems.PACKAGED_PROCESSING_PATTERN.get())) {
            if (!moveItemStackTo(
                    source,
                    PackageAssemblerBlockEntity.SLOT_PATTERN,
                    PackageAssemblerBlockEntity.SLOT_PATTERN + 1,
                    false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, 0, PackageAssemblerBlockEntity.INPUT_SLOT_COUNT, false)) {
            if (index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(source, PLAYER_INVENTORY_END, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(source, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
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
        return stillValid(access, player, APBlocks.PACKAGE_ASSEMBLER.get());
    }

    private void addInputSlots() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new SlotItemHandler(blockEntity.getItems(), column + row * 3, 26 + column * 18, 18 + row * 18));
            }
        }
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

    private static PackageAssemblerBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof PackageAssemblerBlockEntity assembler) {
            return assembler;
        }
        throw new IllegalStateException("Expected Package Assembler block entity at " + pos);
    }

    private static final class OutputSlot extends SlotItemHandler {
        private OutputSlot(PackageAssemblerBlockEntity blockEntity, int x, int y) {
            super(blockEntity.getItems(), PackageAssemblerBlockEntity.SLOT_OUTPUT, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}

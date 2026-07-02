package com.warmthdawn.appliedpackaging.world.menu;

import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.item.PackageColor;
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
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class MePackagerMenu extends AbstractContainerMenu {
    public static final int BUTTON_PACK_ONCE = 0;
    public static final int BUTTON_MARKER_MODE = 1;
    public static final int BUTTON_REDSTONE_MODE = 2;
    public static final int BUTTON_COLOR_BASE = 10;

    private static final int MACHINE_SLOT_COUNT = 5;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final MePackagerBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final DataSlot selectedColorSlot;
    private final DataSlot markerModeSlot;
    private final DataSlot redstoneModeSlot;

    public MePackagerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buffer.readBlockPos()));
    }

    public MePackagerMenu(int containerId, Inventory playerInventory, MePackagerBlockEntity blockEntity) {
        super(APMenus.ME_PACKAGER.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.selectedColorSlot = new DataSlot() {
            @Override
            public int get() {
                return blockEntity.selectedColor().ordinal();
            }

            @Override
            public void set(int value) {
                PackageColor[] values = PackageColor.values();
                if (value >= 0 && value < values.length) {
                    blockEntity.setSelectedColor(values[value]);
                }
            }
        };
        this.markerModeSlot = new DataSlot() {
            @Override
            public int get() {
                return blockEntity.markerMode().ordinal();
            }

            @Override
            public void set(int value) {
                MarkerMergeMode[] values = MarkerMergeMode.values();
                if (value >= 0 && value < values.length) {
                    blockEntity.setMarkerMode(values[value]);
                }
            }
        };
        this.redstoneModeSlot = new DataSlot() {
            @Override
            public int get() {
                return blockEntity.redstoneMode().ordinal();
            }

            @Override
            public void set(int value) {
                MePackagerBlockEntity.RedstoneMode[] values = MePackagerBlockEntity.RedstoneMode.values();
                if (value >= 0 && value < values.length) {
                    blockEntity.setRedstoneMode(values[value]);
                }
            }
        };

        addSlot(new SlotItemHandler(blockEntity.getItems(), MePackagerBlockEntity.SLOT_INPUT, 35, 34));
        addSlot(new OutputPackageSlot(blockEntity, 123, 34));
        addSlot(new SlotItemHandler(blockEntity.getItems(), MePackagerBlockEntity.SLOT_CAPACITY, 35, 60));
        addSlot(new SlotItemHandler(blockEntity.getItems(), MePackagerBlockEntity.SLOT_FILTER, 61, 60));
        addSlot(new SlotItemHandler(blockEntity.getItems(), MePackagerBlockEntity.SLOT_MARKER, 87, 60));
        addPlayerInventory(playerInventory);
        addDataSlot(selectedColorSlot);
        addDataSlot(markerModeSlot);
        addDataSlot(redstoneModeSlot);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_COLOR_BASE && id < BUTTON_COLOR_BASE + PackageColor.values().length) {
            if (!player.level().isClientSide) {
                blockEntity.setSelectedColor(PackageColor.values()[id - BUTTON_COLOR_BASE]);
            }
            return true;
        }
        if (id == BUTTON_MARKER_MODE) {
            if (!player.level().isClientSide) {
                blockEntity.cycleMarkerMode();
            }
            return true;
        }
        if (id == BUTTON_REDSTONE_MODE) {
            if (!player.level().isClientSide) {
                blockEntity.cycleRedstoneMode();
            }
            return true;
        }
        if (id != BUTTON_PACK_ONCE) {
            return false;
        }
        if (!player.level().isClientSide) {
            MePackagerBlockEntity.MachineResult result = blockEntity.runOnce();
            player.displayClientMessage(Component.translatable(result.messageKey()), true);
        }
        return true;
    }

    public PackageColor selectedColor() {
        PackageColor[] values = PackageColor.values();
        int index = selectedColorSlot.get();
        if (index < 0 || index >= values.length) {
            return PackageColor.FLUIX;
        }
        return values[index];
    }

    public MarkerMergeMode markerMode() {
        MarkerMergeMode[] values = MarkerMergeMode.values();
        int index = markerModeSlot.get();
        if (index < 0 || index >= values.length) {
            return MarkerMergeMode.RETAIN;
        }
        return values[index];
    }

    public MePackagerBlockEntity.RedstoneMode redstoneMode() {
        MePackagerBlockEntity.RedstoneMode[] values = MePackagerBlockEntity.RedstoneMode.values();
        int index = redstoneModeSlot.get();
        if (index < 0 || index >= values.length) {
            return MePackagerBlockEntity.RedstoneMode.PULSE;
        }
        return values[index];
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
        } else if (blockEntity.getItems().isItemValid(MePackagerBlockEntity.SLOT_CAPACITY, source)) {
            if (!moveItemStackTo(
                    source,
                    MePackagerBlockEntity.SLOT_CAPACITY,
                    MePackagerBlockEntity.SLOT_CAPACITY + 1,
                    false)) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.getItems().isItemValid(MePackagerBlockEntity.SLOT_FILTER, source)) {
            if (!moveItemStackTo(
                    source,
                    MePackagerBlockEntity.SLOT_FILTER,
                    MePackagerBlockEntity.SLOT_FILTER + 1,
                    false)) {
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
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 107 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 165));
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

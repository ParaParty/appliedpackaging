package com.warmthdawn.appliedpackaging.world.menu;

import com.warmthdawn.appliedpackaging.core.package_data.ColoredProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.terminal.PackagePatternTerminalBlockEntity;
import java.util.Optional;
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

public class PackagePatternTerminalMenu extends AbstractContainerMenu {
    public static final int BUTTON_ENCODE = 0;
    public static final int BUTTON_COLOR_BASE = 10;
    public static final int BUTTON_INPUT_COLOR_BASE = 40;

    private static final int MACHINE_SLOT_COUNT = 13;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final PackagePatternTerminalBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final DataSlot selectedColorSlot;
    private final DataSlot[] inputColorSlots = new DataSlot[PackagePatternTerminalBlockEntity.INPUT_SLOT_COUNT];

    public PackagePatternTerminalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buffer.readBlockPos()));
    }

    public PackagePatternTerminalMenu(int containerId, Inventory playerInventory, PackagePatternTerminalBlockEntity blockEntity) {
        super(APMenus.PACKAGE_PATTERN_TERMINAL.get(), containerId);
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

        addInputSlots();
        addSlot(new SlotItemHandler(blockEntity.getItems(), PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN, 116, 24));
        addSlot(new OutputSlot(blockEntity, 144, 24));
        addSlot(new SlotItemHandler(blockEntity.getItems(), PackagePatternTerminalBlockEntity.SLOT_CAPACITY, 116, 52));
        addSlot(new SlotItemHandler(blockEntity.getItems(), PackagePatternTerminalBlockEntity.SLOT_MARKER, 144, 52));
        addPlayerInventory(playerInventory);
        addDataSlot(selectedColorSlot);
        addInputColorSlots();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_COLOR_BASE && id < BUTTON_COLOR_BASE + PackageColor.values().length) {
            if (!player.level().isClientSide) {
                blockEntity.setSelectedColor(PackageColor.values()[id - BUTTON_COLOR_BASE]);
            }
            return true;
        }
        if (id >= BUTTON_INPUT_COLOR_BASE
                && id < BUTTON_INPUT_COLOR_BASE + PackagePatternTerminalBlockEntity.INPUT_SLOT_COUNT) {
            if (!player.level().isClientSide) {
                blockEntity.setInputSlotColor(id - BUTTON_INPUT_COLOR_BASE, blockEntity.selectedColor());
            }
            return true;
        }
        if (id != BUTTON_ENCODE) {
            return false;
        }
        if (!player.level().isClientSide) {
            PackagePatternTerminalBlockEntity.EncodeResult result = blockEntity.encodeOnce();
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

    public Optional<PackageColor> inputSlotColor(int slot) {
        if (slot < 0 || slot >= inputColorSlots.length) {
            return Optional.empty();
        }
        int index = inputColorSlots[slot].get();
        PackageColor[] values = PackageColor.values();
        if (index < 0 || index >= values.length) {
            return Optional.empty();
        }
        return Optional.of(values[index]);
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
        } else if (isPatternInput(source)) {
            if (!moveItemStackTo(
                    source,
                    PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                    PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN + 1,
                    false)) {
                return ItemStack.EMPTY;
            }
        } else if (MePackagerBlockEntity.capacityProfileFromItem(source).isPresent()) {
            if (!moveItemStackTo(
                    source,
                    PackagePatternTerminalBlockEntity.SLOT_CAPACITY,
                    PackagePatternTerminalBlockEntity.SLOT_CAPACITY + 1,
                    false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, 0, PackagePatternTerminalBlockEntity.INPUT_SLOT_COUNT, false)) {
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
        return stillValid(access, player, APBlocks.PACKAGE_PATTERN_TERMINAL.get());
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
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 107 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 165));
        }
    }

    private void addInputColorSlots() {
        for (int slot = 0; slot < inputColorSlots.length; slot++) {
            final int inputSlot = slot;
            inputColorSlots[inputSlot] = new DataSlot() {
                @Override
                public int get() {
                    return blockEntity.inputSlotColorOrdinal(inputSlot);
                }

                @Override
                public void set(int value) {
                    blockEntity.setInputSlotColorOrdinal(inputSlot, value);
                }
            };
            addDataSlot(inputColorSlots[inputSlot]);
        }
    }

    private static boolean isPatternInput(ItemStack stack) {
        return ColoredProcessingPatternDataStorage.canStore(stack) || PackagePatternDataStorage.canStore(stack);
    }

    private static PackagePatternTerminalBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof PackagePatternTerminalBlockEntity terminal) {
            return terminal;
        }
        throw new IllegalStateException("Expected Package Pattern Terminal block entity at " + pos);
    }

    private static final class OutputSlot extends SlotItemHandler {
        private OutputSlot(PackagePatternTerminalBlockEntity blockEntity, int x, int y) {
            super(blockEntity.getItems(), PackagePatternTerminalBlockEntity.SLOT_OUTPUT, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}

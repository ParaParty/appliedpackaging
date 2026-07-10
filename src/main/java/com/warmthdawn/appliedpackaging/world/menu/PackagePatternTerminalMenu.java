package com.warmthdawn.appliedpackaging.world.menu;

import appeng.api.parts.IPart;
import appeng.api.parts.PartHelper;
import com.warmthdawn.appliedpackaging.core.package_data.ColoredProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.terminal.PackagePatternTerminalBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.terminal.PackagePatternTerminalHost;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.items.SlotItemHandler;

public class PackagePatternTerminalMenu extends AbstractContainerMenu {
    private static final int HOST_BLOCK = 0;
    private static final int HOST_PART = 1;

    public static final int BUTTON_ENCODE = 0;
    public static final int BUTTON_SPLIT = 1;
    public static final int BUTTON_COLOR_BASE = 10;
    public static final int BUTTON_INPUT_COLOR_BASE = 40;
    public static final int BUTTON_INPUT_COLOR_CLEAR_BASE = 60;
    public static final int BUTTON_OUTPUT_AMOUNT_INCREASE_BASE = 80;
    public static final int BUTTON_OUTPUT_AMOUNT_DECREASE_BASE =
            BUTTON_OUTPUT_AMOUNT_INCREASE_BASE + PackagePatternTerminalBlockEntity.PROCESSING_OUTPUT_SLOT_COUNT;

    public static final int ITEM_HANDLER_SLOT_COUNT = 13;
    public static final int PROCESSING_OUTPUT_START = ITEM_HANDLER_SLOT_COUNT;
    public static final int PROCESSING_OUTPUT_END =
            PROCESSING_OUTPUT_START + PackagePatternTerminalBlockEntity.PROCESSING_OUTPUT_SLOT_COUNT;
    public static final int MACHINE_SLOT_COUNT = PROCESSING_OUTPUT_END;
    public static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    public static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    public static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final PackagePatternTerminalHost terminal;
    private final boolean clientSide;
    private int syncedSelectedColor;
    private final int[] syncedInputColors = new int[PackagePatternTerminalBlockEntity.INPUT_SLOT_COUNT];
    private final DataSlot selectedColorSlot;
    private final DataSlot[] inputColorSlots = new DataSlot[PackagePatternTerminalBlockEntity.INPUT_SLOT_COUNT];
    private final SplitIntDataSlots[] processingOutputAmountSlots =
            new SplitIntDataSlots[PackagePatternTerminalBlockEntity.PROCESSING_OUTPUT_SLOT_COUNT];
    private final SimpleContainer processingOutputDisplay =
            new SimpleContainer(PackagePatternTerminalBlockEntity.PROCESSING_OUTPUT_SLOT_COUNT);

    public PackagePatternTerminalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, getHost(playerInventory, buffer));
    }

    public PackagePatternTerminalMenu(int containerId, Inventory playerInventory, PackagePatternTerminalHost terminal) {
        super(APMenus.PACKAGE_PATTERN_TERMINAL.get(), containerId);
        this.terminal = terminal;
        this.clientSide = playerInventory.player.level().isClientSide;
        this.syncedSelectedColor = terminal.selectedColor().ordinal();
        for (int slot = 0; slot < syncedInputColors.length; slot++) {
            syncedInputColors[slot] = terminal.inputSlotColorOrdinal(slot);
        }
        this.selectedColorSlot = new DataSlot() {
            @Override
            public int get() {
                return clientSide ? syncedSelectedColor : terminal.selectedColor().ordinal();
            }

            @Override
            public void set(int value) {
                PackageColor[] values = PackageColor.values();
                if (value >= 0 && value < values.length) {
                    syncedSelectedColor = value;
                }
            }
        };

        addInputSlots();
        addSlot(new SlotItemHandler(terminal.getItems(), PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN, 116, 24));
        addSlot(new OutputSlot(terminal, 144, 24));
        addSlot(new SlotItemHandler(terminal.getItems(), PackagePatternTerminalBlockEntity.SLOT_CAPACITY, 116, 52));
        addSlot(new SlotItemHandler(terminal.getItems(), PackagePatternTerminalBlockEntity.SLOT_MARKER, 144, 52));
        updateProcessingOutputDisplay();
        addProcessingOutputSlots();
        addPlayerInventory(playerInventory);
        addDataSlot(selectedColorSlot);
        addInputColorSlots();
        addProcessingOutputAmountSlots();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_COLOR_BASE && id < BUTTON_COLOR_BASE + PackageColor.values().length) {
            if (!player.level().isClientSide) {
                terminal.setSelectedColor(PackageColor.values()[id - BUTTON_COLOR_BASE]);
            }
            return true;
        }
        if (id >= BUTTON_INPUT_COLOR_BASE
                && id < BUTTON_INPUT_COLOR_BASE + PackagePatternTerminalBlockEntity.INPUT_SLOT_COUNT) {
            if (!player.level().isClientSide) {
                terminal.setInputSlotColor(id - BUTTON_INPUT_COLOR_BASE, terminal.selectedColor());
            }
            return true;
        }
        if (id >= BUTTON_INPUT_COLOR_CLEAR_BASE
                && id < BUTTON_INPUT_COLOR_CLEAR_BASE + PackagePatternTerminalBlockEntity.INPUT_SLOT_COUNT) {
            if (!player.level().isClientSide) {
                terminal.clearInputSlotColor(id - BUTTON_INPUT_COLOR_CLEAR_BASE);
            }
            return true;
        }
        if (id >= BUTTON_OUTPUT_AMOUNT_INCREASE_BASE
                && id < BUTTON_OUTPUT_AMOUNT_INCREASE_BASE + PackagePatternTerminalBlockEntity.PROCESSING_OUTPUT_SLOT_COUNT) {
            if (!player.level().isClientSide) {
                terminal.adjustProcessingOutputAmount(id - BUTTON_OUTPUT_AMOUNT_INCREASE_BASE, true);
                updateProcessingOutputDisplay();
                broadcastChanges();
            }
            return true;
        }
        if (id >= BUTTON_OUTPUT_AMOUNT_DECREASE_BASE
                && id < BUTTON_OUTPUT_AMOUNT_DECREASE_BASE + PackagePatternTerminalBlockEntity.PROCESSING_OUTPUT_SLOT_COUNT) {
            if (!player.level().isClientSide) {
                terminal.adjustProcessingOutputAmount(id - BUTTON_OUTPUT_AMOUNT_DECREASE_BASE, false);
                updateProcessingOutputDisplay();
                broadcastChanges();
            }
            return true;
        }
        if (id == BUTTON_SPLIT) {
            if (!player.level().isClientSide) {
                PackagePatternTerminalBlockEntity.SplitResult result = terminal.splitOnce();
                player.displayClientMessage(Component.translatable(result.messageKey()), true);
            }
            return true;
        }
        if (id != BUTTON_ENCODE) {
            return false;
        }
        if (!player.level().isClientSide) {
            PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
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

    public ItemStack processingOutput(int slot) {
        if (slot < 0 || slot >= processingOutputDisplay.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return processingOutputDisplay.getItem(slot).copy();
    }

    public int processingOutputAmount(int slot) {
        if (slot < 0 || slot >= processingOutputAmountSlots.length) {
            return 0;
        }
        return processingOutputAmountSlots[slot].get();
    }

    public static void writeBlockHost(FriendlyByteBuf buffer, BlockPos pos) {
        buffer.writeByte(HOST_BLOCK);
        buffer.writeBlockPos(pos);
    }

    public static void writePartHost(FriendlyByteBuf buffer, BlockPos pos, Direction side) {
        buffer.writeByte(HOST_PART);
        buffer.writeBlockPos(pos);
        buffer.writeByte(side.ordinal());
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= PROCESSING_OUTPUT_START && slotId < PROCESSING_OUTPUT_END) {
            clickProcessingOutput(slotId - PROCESSING_OUTPUT_START, button, clickType, player);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= PROCESSING_OUTPUT_START && index < PROCESSING_OUTPUT_END) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (index < ITEM_HANDLER_SLOT_COUNT) {
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
        return terminal.isTerminalMenuValid(player);
    }

    @Override
    public void broadcastChanges() {
        if (!clientSide) {
            updateProcessingOutputDisplay();
        }
        super.broadcastChanges();
    }

    private void clickProcessingOutput(int outputSlot, int button, ClickType clickType, Player player) {
        if (clickType != ClickType.PICKUP) {
            return;
        }
        if (!player.level().isClientSide) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                terminal.clearProcessingOutput(outputSlot);
            } else {
                terminal.setProcessingOutputFromGhostStack(outputSlot, carried, button == 1);
            }
            updateProcessingOutputDisplay();
            broadcastChanges();
        }
    }

    private void addInputSlots() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new SlotItemHandler(terminal.getItems(), column + row * 3, 26 + column * 18, 18 + row * 18));
            }
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

    private void addProcessingOutputSlots() {
        for (int slot = 0; slot < PackagePatternTerminalBlockEntity.PROCESSING_OUTPUT_SLOT_COUNT; slot++) {
            addSlot(new GhostOutputSlot(processingOutputDisplay, slot, 82 + slot * 18, 75));
        }
    }

    private void updateProcessingOutputDisplay() {
        for (int slot = 0; slot < PackagePatternTerminalBlockEntity.PROCESSING_OUTPUT_SLOT_COUNT; slot++) {
            processingOutputDisplay.setItem(slot, terminal.processingOutput(slot));
        }
    }

    private void addInputColorSlots() {
        for (int slot = 0; slot < inputColorSlots.length; slot++) {
            final int inputSlot = slot;
            inputColorSlots[inputSlot] = new DataSlot() {
                @Override
                public int get() {
                    return clientSide
                            ? syncedInputColors[inputSlot]
                            : terminal.inputSlotColorOrdinal(inputSlot);
                }

                @Override
                public void set(int value) {
                    syncedInputColors[inputSlot] = value;
                }
            };
            addDataSlot(inputColorSlots[inputSlot]);
        }
    }

    private void addProcessingOutputAmountSlots() {
        for (int slot = 0; slot < processingOutputAmountSlots.length; slot++) {
            final int outputSlot = slot;
            SplitIntDataSlots amountSlots = new SplitIntDataSlots(
                    clientSide,
                    () -> terminal.processingOutputAmountForDisplay(outputSlot));
            processingOutputAmountSlots[outputSlot] = amountSlots;
            addDataSlot(amountSlots.lowWordSlot());
            addDataSlot(amountSlots.highWordSlot());
        }
    }

    private static boolean isPatternInput(ItemStack stack) {
        return ColoredProcessingPatternDataStorage.canStore(stack) || PackagePatternDataStorage.canStore(stack);
    }

    private static PackagePatternTerminalHost getHost(Inventory inventory, FriendlyByteBuf buffer) {
        int hostType = buffer.readUnsignedByte();
        BlockPos pos = buffer.readBlockPos();
        if (hostType == HOST_PART) {
            Direction side = Direction.values()[buffer.readUnsignedByte()];
            IPart part = PartHelper.getPart(inventory.player.level(), pos, side);
            if (part instanceof PackagePatternTerminalHost terminal) {
                return terminal;
            }
            throw new IllegalStateException("Expected Package Pattern Terminal part at " + pos + " side " + side);
        }
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof PackagePatternTerminalBlockEntity terminal) {
            return terminal;
        }
        throw new IllegalStateException("Expected Package Pattern Terminal block entity at " + pos);
    }

    private static final class OutputSlot extends SlotItemHandler {
        private OutputSlot(PackagePatternTerminalHost terminal, int x, int y) {
            super(terminal.getItems(), PackagePatternTerminalBlockEntity.SLOT_OUTPUT, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private static final class GhostOutputSlot extends Slot {
        private GhostOutputSlot(SimpleContainer container, int slot, int x, int y) {
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

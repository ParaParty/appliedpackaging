package com.warmthdawn.appliedpackaging.world.menu;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.IOptionalSlot;
import appeng.menu.slot.FakeSlot;
import appeng.client.Point;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.world.block.entity.PackageAssemblerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import java.util.Optional;

public class PackageAssemblerMenu extends UpgradeableMenu<PackageAssemblerBlockEntity> implements IProgressProvider {
    public static final int BUTTON_OUTPUT_MODE = 0;
    public static final int BUTTON_AUTO_EXPORT = BUTTON_OUTPUT_MODE;
    public static final int BUTTON_BLOCKING_MODE = 1;
    public static final int BUTTON_SCROLL_BASE = 100;
    private static final String ACTION_CYCLE_OUTPUT_MODE = "cycleOutputMode";
    private static final String ACTION_TOGGLE_BLOCKING_MODE = "toggleBlockingMode";
    private static final String ACTION_SET_COLOR = "setColor";

    public static final int VISIBLE_ROWS = 4;
    public static final int VISIBLE_INPUT_COLUMNS = PackageAssemblerBlockEntity.MENU_INPUT_COLUMNS;
    public static final int VISIBLE_INPUT_COUNT = VISIBLE_ROWS * VISIBLE_INPUT_COLUMNS;
    public static final int MENU_INPUT_START = 0;
    public static final int MENU_INPUT_END = MENU_INPUT_START + VISIBLE_INPUT_COUNT;
    public static final int PATTERN_SLOT = MENU_INPUT_END;
    public static final int CAPACITY_SLOT = PATTERN_SLOT + 1;
    public static final int OUTPUT_START = CAPACITY_SLOT + 1;
    public static final int OUTPUT_END = OUTPUT_START + 2;
    public static final int HOTBAR_START = OUTPUT_END;
    public static final int HOTBAR_END = HOTBAR_START + Inventory.getSelectionSize();
    public static final int PLAYER_INVENTORY_START = HOTBAR_END;
    public static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;

    public static final int SCROLLED_SLOT_Y = 67;
    public static final int SLOT_STEP = 18;

    private static final SlotSemantic[] MENU_INPUT_ROW_SEMANTICS = {
            SlotSemantics.PROCESSING_INPUTS,
            SlotSemantics.MACHINE_CRAFTING_GRID,
            SlotSemantics.CRAFTING_GRID,
            SlotSemantics.CONFIG
    };

    private final ContainerLevelAccess access;
    private final DataSlot outputModeSlot;
    private final DataSlot blockingModeSlot;
    @GuiSync(12)
    public int craftProgress = 0;
    @GuiSync(13)
    public int queuedOutputCount = 0;
    @GuiSync(14)
    public int scrollableInputRows = 0;
    @GuiSync(15)
    public int syncedMenuInputSlotCount = 0;
    @GuiSync(16)
    public boolean syncedPatternCapacityValid = true;
    @GuiSync(17)
    public PackageColor selectedColor = PackageColor.FLUIX;
    @GuiSync(18)
    public int effectiveColorOrdinal = PackageColor.FLUIX.ordinal();
    @GuiSync(19)
    public boolean syncedWorking;
    @GuiSync(20)
    public int capacityUnitLimit = (int) PackageCapacityProfile.DEFAULT.unitLimit();
    @GuiSync(21)
    public int capacityTypeLimit = PackageCapacityProfile.DEFAULT.typeLimit();
    @GuiSync(22)
    public GenericStack effectiveMarker;
    @GuiSync(23)
    public boolean syncedColorConfigurable = true;
    private SimpleContainer previewOutput;
    private int[] menuInputSlotIndexes;
    private int mainOutputSlotIndex;
    private int previewOutputSlotIndex;
    private int markerSlotIndex;
    private int scrollOffset;

    public PackageAssemblerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buffer.readBlockPos()));
    }

    public PackageAssemblerMenu(int containerId, Inventory playerInventory, PackageAssemblerBlockEntity blockEntity) {
        super(APMenus.PACKAGE_ASSEMBLER.get(), containerId, playerInventory, blockEntity);
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.outputModeSlot = new DataSlot() {
            @Override
            public int get() {
                return blockEntity.outputMode().ordinal();
            }

            @Override
            public void set(int value) {
                PackageAssemblerBlockEntity.OutputMode[] modes = PackageAssemblerBlockEntity.OutputMode.values();
                blockEntity.setOutputMode(modes[Math.max(0, Math.min(value, modes.length - 1))]);
            }
        };
        this.blockingModeSlot = new DataSlot() {
            @Override
            public int get() {
                return blockEntity.blockingMode() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                blockEntity.setBlockingMode(value != 0);
            }
        };

        registerClientAction(ACTION_CYCLE_OUTPUT_MODE, this::cycleOutputMode);
        registerClientAction(ACTION_TOGGLE_BLOCKING_MODE, this::toggleBlockingMode);
        registerClientAction(ACTION_SET_COLOR, PackageColor.class, this::setSelectedColor);
        addDataSlot(outputModeSlot);
        addDataSlot(blockingModeSlot);
        refreshMenuInputDimensions();
    }

    @Override
    protected void setupInventorySlots() {
        previewOutput = new SimpleContainer(1);
        menuInputSlotIndexes = new int[VISIBLE_INPUT_COUNT];

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int column = 0; column < VISIBLE_INPUT_COLUMNS; column++) {
                int visibleIndex = column + row * VISIBLE_INPUT_COLUMNS;
                Slot slot = addSlot(new MenuInputDisplaySlot(visibleIndex), MENU_INPUT_ROW_SEMANTICS[row]);
                menuInputSlotIndexes[visibleIndex] = slot.index;
            }
        }

        addSlot(
                new SlotItemHandler(getHost().getItems(), PackageAssemblerBlockEntity.SLOT_PATTERN, 0, 0),
                SlotSemantics.ENCODED_PATTERN);
        addSlot(
                new SlotItemHandler(getHost().getItems(), PackageAssemblerBlockEntity.SLOT_CAPACITY, 0, 0),
                SlotSemantics.STORAGE_CELL);
        markerSlotIndex = addSlot(
                new EffectiveMarkerSlot(getHost().getMarkerFilter().createMenuWrapper(), 0),
                SlotSemantics.BLANK_PATTERN).index;
        mainOutputSlotIndex = addSlot(
                new OrderedOutputSlot(getHost()),
                SlotSemantics.MACHINE_OUTPUT).index;
        previewOutputSlotIndex = addSlot(
                new PreviewOutputSlot(previewOutput),
                SlotSemantics.PROCESSING_OUTPUTS).index;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_SCROLL_BASE && id <= BUTTON_SCROLL_BASE + maxScrollOffset()) {
            setScrollOffset(id - BUTTON_SCROLL_BASE);
            return true;
        }
        if (id == BUTTON_OUTPUT_MODE) {
            if (!player.level().isClientSide) {
                cycleOutputMode();
            }
            return true;
        }
        if (id == BUTTON_BLOCKING_MODE) {
            if (!player.level().isClientSide) {
                toggleBlockingMode();
            }
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == markerSlotIndex && isCrafting()) {
            return;
        }
        int visibleIndex = visibleIndexForMenuSlot(slotId);
        if (visibleIndex >= 0) {
            int inputSlot = inputSlotForVisibleIndex(visibleIndex);
            if (inputSlot < menuInputSlotCount()) {
                clickMenuInput(inputSlot, button, clickType, player);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        int visibleIndex = visibleIndexForMenuSlot(index);
        if (visibleIndex >= 0) {
            int inputSlot = inputSlotForVisibleIndex(visibleIndex);
            return inputSlot < menuInputSlotCount()
                    ? quickMoveMenuInput(inputSlot)
                    : ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (!isPlayerInventorySlot(slot)) {
            if (!moveItemStackToPlayerInventory(source)) {
                return ItemStack.EMPTY;
            }
        } else if (PackageAssemblerBlockEntity.isPatternSlotItem(source)) {
            if (!moveItemStackTo(source, PATTERN_SLOT, PATTERN_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (PackageCapacityProfile.fromStorageComponent(source).isPresent()) {
            if (!moveItemStackTo(source, CAPACITY_SLOT, CAPACITY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            int inserted = getHost().insertMenuInput(-1, source, source.getCount(), false);
            if (inserted <= 0) {
                return ItemStack.EMPTY;
            }
            source.shrink(inserted);
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

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            craftProgress = getHost().craftingProgress();
            queuedOutputCount = getHost().queuedOutputCount();
            selectedColor = getHost().selectedColor();
            effectiveColorOrdinal = getHost().effectiveColor().map(Enum::ordinal).orElse(-1);
            effectiveMarker = getHost().effectiveMarker().map(marker -> marker.stack()).orElse(null);
            syncedWorking = getHost().isCrafting();
            syncedColorConfigurable = getHost().isSelectedColorConfigurable();
            PackageCapacityProfile capacity = getHost().configuredCapacityProfile();
            capacityUnitLimit = (int) capacity.unitLimit();
            capacityTypeLimit = capacity.typeLimit();
            refreshMenuInputDimensions();
            setScrollOffset(scrollOffset);
            previewOutput.setItem(0, getHost().nextOutputPreview());
        }
        super.broadcastChanges();
    }

    public boolean autoExport() {
        return outputMode() != PackageAssemblerBlockEntity.OutputMode.NONE;
    }

    public void toggleAutoExport() {
        cycleOutputMode();
    }

    public PackageAssemblerBlockEntity.OutputMode outputMode() {
        PackageAssemblerBlockEntity.OutputMode[] modes = PackageAssemblerBlockEntity.OutputMode.values();
        int ordinal = Math.max(0, Math.min(outputModeSlot.get(), modes.length - 1));
        return modes[ordinal];
    }

    public void cycleOutputMode() {
        if (isClientSide()) {
            sendClientAction(ACTION_CYCLE_OUTPUT_MODE);
            return;
        }

        if (getHost().isCrafting()) {
            return;
        }
        getHost().toggleAutoExport();
        broadcastChanges();
    }

    public boolean blockingMode() {
        return blockingModeSlot.get() != 0;
    }

    public void toggleBlockingMode() {
        if (isClientSide()) {
            sendClientAction(ACTION_TOGGLE_BLOCKING_MODE);
            return;
        }

        if (getHost().isCrafting()) {
            return;
        }
        getHost().toggleBlockingMode();
        broadcastChanges();
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public int maxScrollOffset() {
        return Math.max(0, scrollableInputRows - VISIBLE_ROWS);
    }

    public int menuInputSlotCount() {
        return Math.max(0, syncedMenuInputSlotCount);
    }

    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset()));
    }

    public int inputSlotForVisibleIndex(int visibleIndex) {
        return scrollOffset * VISIBLE_INPUT_COLUMNS + visibleIndex;
    }

    public int menuInputMenuSlotIndex(int visibleIndex) {
        if (visibleIndex < 0 || visibleIndex >= VISIBLE_INPUT_COUNT) {
            throw new IndexOutOfBoundsException(visibleIndex);
        }
        return menuInputSlotIndexes[visibleIndex];
    }

    public int outputMenuSlotIndex(int visibleRow) {
        return switch (visibleRow) {
            case 0 -> mainOutputSlotIndex;
            case 1 -> previewOutputSlotIndex;
            default -> throw new IndexOutOfBoundsException(visibleRow);
        };
    }

    public int queuedOutputCount() {
        return Math.max(0, queuedOutputCount);
    }

    public boolean isCrafting() {
        return syncedWorking;
    }

    public boolean canEditColor() {
        return syncedColorConfigurable && !syncedWorking;
    }

    public PackageColor selectedColor() {
        return selectedColor == null ? PackageColor.FLUIX : selectedColor;
    }

    public Optional<PackageColor> effectiveColor() {
        PackageColor[] colors = PackageColor.values();
        return effectiveColorOrdinal >= 0 && effectiveColorOrdinal < colors.length
                ? Optional.of(colors[effectiveColorOrdinal])
                : Optional.empty();
    }

    public ItemStack effectiveMarkerDisplayStack() {
        return effectiveMarker != null && effectiveMarker.what() instanceof AEItemKey itemKey
                ? itemKey.toStack(1)
                : ItemStack.EMPTY;
    }

    public void setSelectedColor(PackageColor color) {
        PackageColor value = color == null ? PackageColor.FLUIX : color;
        if (isClientSide()) {
            if (!canEditColor()) {
                return;
            }
            sendClientAction(ACTION_SET_COLOR, value);
            return;
        }
        if (!getHost().isSelectedColorConfigurable()) {
            return;
        }
        getHost().setSelectedColor(value);
        selectedColor = getHost().selectedColor();
        effectiveColorOrdinal = getHost().effectiveColor().map(Enum::ordinal).orElse(-1);
        broadcastChanges();
    }

    public int capacityUnitLimit() {
        return Math.max(0, capacityUnitLimit);
    }

    public int capacityTypeLimit() {
        return Math.max(0, capacityTypeLimit);
    }

    public boolean isPatternCapacityValid() {
        return syncedPatternCapacityValid;
    }

    public int hotbarMenuSlotIndex(int hotbarSlot) {
        return getSlots(SlotSemantics.PLAYER_HOTBAR).get(hotbarSlot).index;
    }

    public ItemStack inputFilterForSlot(int slot) {
        return getHost().menuInputFilterStack(slot);
    }

    public boolean isInputSlotEnabled(int slot) {
        return slot >= 0
                && slot < menuInputSlotCount()
                && getHost().isMenuInputSlotEnabled(slot);
    }

    public boolean isInputSlotValid(int slot) {
        int visibleIndex = slot - scrollOffset * VISIBLE_INPUT_COLUMNS;
        if (visibleIndex < 0 || visibleIndex >= menuInputSlotIndexes.length) {
            return getHost().isMenuInputSlotValid(slot);
        }
        ItemStack displayed = getSlot(menuInputSlotIndexes[visibleIndex]).getItem();
        if (displayed.isEmpty()) {
            return true;
        }
        ItemStack filter = inputFilterForSlot(slot);
        return !filter.isEmpty()
                && ItemStack.isSameItemSameTags(displayed, filter)
                && displayed.getCount() <= filter.getCount();
    }

    public int inputSlotForMenuSlotIndex(int slotIndex) {
        int visibleIndex = visibleIndexForMenuSlot(slotIndex);
        return visibleIndex < 0 ? -1 : inputSlotForVisibleIndex(visibleIndex);
    }

    private void refreshMenuInputDimensions() {
        syncedMenuInputSlotCount = getHost().menuInputSlotCount();
        scrollableInputRows = (syncedMenuInputSlotCount + VISIBLE_INPUT_COLUMNS - 1) / VISIBLE_INPUT_COLUMNS;
        syncedPatternCapacityValid = getHost().isPatternCapacityValid();
    }

    @Override
    public int getCurrentProgress() {
        return craftProgress;
    }

    @Override
    public int getMaxProgress() {
        return PackageAssemblerBlockEntity.MAX_CRAFT_PROGRESS;
    }

    private void clickMenuInput(int inputSlot, int button, ClickType clickType, Player player) {
        if (player.level().isClientSide) {
            return;
        }
        if (clickType == ClickType.QUICK_MOVE) {
            quickMoveMenuInput(inputSlot);
            broadcastChanges();
            return;
        }
        if (clickType != ClickType.PICKUP) {
            return;
        }

        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            int amount = button == 1 ? 1 : carriedStackLimit(inputSlot);
            setCarried(getHost().extractMenuInput(inputSlot, amount, false));
        } else {
            int requested = button == 1 ? 1 : carried.getCount();
            int inserted = getHost().insertMenuInput(inputSlot, carried, requested, false);
            if (inserted > 0) {
                carried.shrink(inserted);
                setCarried(carried);
            }
        }
        broadcastChanges();
    }

    private int carriedStackLimit(int inputSlot) {
        ItemStack stack = getHost().menuInputDisplay(inputSlot);
        return stack.isEmpty() ? 64 : stack.getMaxStackSize();
    }

    private ItemStack quickMoveMenuInput(int inputSlot) {
        ItemStack extracted = getHost().extractMenuInput(inputSlot, carriedStackLimit(inputSlot), true);
        if (extracted.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = extracted.copy();
        if (!moveItemStackToPlayerInventory(remaining)) {
            return ItemStack.EMPTY;
        }
        int moved = extracted.getCount() - remaining.getCount();
        if (moved <= 0) {
            return ItemStack.EMPTY;
        }
        getHost().extractMenuInput(inputSlot, moved, false);
        return extracted;
    }

    private int visibleIndexForMenuSlot(int slotId) {
        for (int index = 0; index < menuInputSlotIndexes.length; index++) {
            if (menuInputSlotIndexes[index] == slotId) {
                return index;
            }
        }
        return -1;
    }

    private boolean moveItemStackToPlayerInventory(ItemStack source) {
        int start = Integer.MAX_VALUE;
        int end = -1;
        for (Slot slot : getSlots(SlotSemantics.PLAYER_HOTBAR)) {
            start = Math.min(start, slot.index);
            end = Math.max(end, slot.index + 1);
        }
        for (Slot slot : getSlots(SlotSemantics.PLAYER_INVENTORY)) {
            start = Math.min(start, slot.index);
            end = Math.max(end, slot.index + 1);
        }
        return start <= end && moveItemStackTo(source, start, end, true);
    }

    private boolean isPlayerInventorySlot(Slot slot) {
        return getSlotSemantic(slot) == SlotSemantics.PLAYER_INVENTORY
                || getSlotSemantic(slot) == SlotSemantics.PLAYER_HOTBAR;
    }

    private static PackageAssemblerBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof PackageAssemblerBlockEntity assembler) {
            return assembler;
        }
        throw new IllegalStateException("Expected Package Assembler block entity at " + pos);
    }

    private final class EffectiveMarkerSlot extends FakeSlot {
        private EffectiveMarkerSlot(InternalInventory inventory, int slot) {
            super(inventory, slot);
        }

        @Override
        public ItemStack getDisplayStack() {
            return effectiveMarkerDisplayStack();
        }

        @Override
        public boolean hasItem() {
            return !getDisplayStack().isEmpty();
        }
    }

    private final class MenuInputDisplaySlot extends Slot implements IOptionalSlot {
        private final int visibleIndex;
        private ItemStack clientDisplay = ItemStack.EMPTY;

        private MenuInputDisplaySlot(int visibleIndex) {
            super(new SimpleContainer(1), 0, 0, 0);
            this.visibleIndex = visibleIndex;
        }

        @Override
        public ItemStack getItem() {
            if (PackageAssemblerMenu.this.isClientSide()) {
                return clientDisplay;
            }
            int inputSlot = inputSlotForVisibleIndex(visibleIndex);
            return getHost().menuInputDisplay(inputSlot);
        }

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }

        @Override
        public ItemStack remove(int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public void set(ItemStack stack) {
            if (PackageAssemblerMenu.this.isClientSide()) {
                clientDisplay = stack.copy();
            }
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean isSlotEnabled() {
            int inputSlot = inputSlotForVisibleIndex(visibleIndex);
            return inputSlot < menuInputSlotCount()
                    && (!getItem().isEmpty()
                            || getHost().hasMenuInput(inputSlot)
                            || getHost().isMenuInputSlotEnabled(inputSlot));
        }

        @Override
        public boolean isRenderDisabled() {
            // The screen owns the current-AE2 slot sprite; suppress AE2 15's legacy states.png fallback.
            return false;
        }

        @Override
        public Point getBackgroundPos() {
            return new Point(x - 1, y - 1);
        }
    }

    private static final class OrderedOutputSlot extends SlotItemHandler {
        private OrderedOutputSlot(PackageAssemblerBlockEntity blockEntity) {
            super(blockEntity.getOrderedOutputItems(), 0, 0, 0);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private static final class PreviewOutputSlot extends Slot {
        private PreviewOutputSlot(SimpleContainer preview) {
            super(preview, 0, 0, 0);
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

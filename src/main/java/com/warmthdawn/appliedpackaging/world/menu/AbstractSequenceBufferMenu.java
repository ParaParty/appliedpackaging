package com.warmthdawn.appliedpackaging.world.menu;

import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.core.definitions.AEItems;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.IOptionalSlot;
import com.warmthdawn.appliedpackaging.core.sequence_buffer.SequenceBufferConfiguration;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.world.block.entity.SequenceBufferBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public abstract class AbstractSequenceBufferMenu extends UpgradeableMenu<SequenceBufferBlockEntity> {
    public static final SlotSemantic BUFFER_CONTENTS =
            SlotSemantics.register("APPLIEDPACKAGING_SEQUENCE_BUFFER_CONTENTS", false);

    private static final Container DISPLAY_CONTAINER = new SimpleContainer(1);
    private static final String ACTION_TOGGLE_AUTO_OUTPUT = "toggleAutoOutput";
    private static final String ACTION_TOGGLE_BLOCKING_MODE = "toggleBlockingMode";
    private static final String ACTION_TOGGLE_SYNCHRONIZED_OUTPUT = "toggleSynchronizedOutput";
    private static final String ACTION_TOGGLE_PATTERN_MODE = "togglePatternMode";
    private static final String ACTION_CYCLE_INPUT_DELAY = "cycleInputDelay";
    private static final int[] INPUT_DELAY_PRESETS = { 0, 1, 5, 10, 20, 40, 100 };

    private final SequenceBufferBlockEntity viewedBlock;
    private final ContainerLevelAccess access;
    private int scrollOffset;

    @GuiSync(30)
    private int memberCount;

    @GuiSync(31)
    private boolean autoOutput = true;

    @GuiSync(32)
    private boolean blockingMode;

    @GuiSync(33)
    private boolean synchronizedOutput;

    @GuiSync(34)
    private boolean patternMode;

    @GuiSync(35)
    private int inputDelayTicks = SequenceBufferConfiguration.DEFAULT_INPUT_DELAY_TICKS;

    protected AbstractSequenceBufferMenu(
            MenuType<?> type,
            int containerId,
            Inventory playerInventory,
            SequenceBufferBlockEntity authority,
            SequenceBufferBlockEntity viewedBlock) {
        super(type, containerId, playerInventory, authority);
        this.viewedBlock = viewedBlock;
        BlockPos viewedPos = viewedBlock.getBlockPos();
        this.access = ContainerLevelAccess.create(viewedBlock.getLevel(), viewedPos);
        this.memberCount = currentMemberCount();

        registerClientAction(ACTION_TOGGLE_AUTO_OUTPUT, this::toggleAutoOutput);
        registerClientAction(ACTION_TOGGLE_BLOCKING_MODE, this::toggleBlockingMode);
        registerClientAction(ACTION_TOGGLE_SYNCHRONIZED_OUTPUT, this::toggleSynchronizedOutput);
        registerClientAction(ACTION_TOGGLE_PATTERN_MODE, this::togglePatternMode);
        registerClientAction(ACTION_CYCLE_INPUT_DELAY, Integer.class, this::cycleInputDelay);
    }

    protected final void addStorageDisplaySlots(int count) {
        for (int visibleIndex = 0; visibleIndex < count; visibleIndex++) {
            addSlot(new StorageDisplaySlot(visibleIndex), BUFFER_CONTENTS);
        }
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            memberCount = currentMemberCount();
            setScrollOffset(scrollOffset);
            synchronizeConfigurationFields();
        }
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof StorageDisplaySlot displaySlot) {
            clickStorageSlot(displaySlot.visibleIndex, button, clickType, player);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (isClientSide() || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (slot instanceof StorageDisplaySlot displaySlot) {
            return quickMoveStorage(displaySlot.visibleIndex);
        }
        if (!isPlayerInventorySlot(slot)) {
            return super.quickMoveStack(player, index);
        }

        ItemStack source = slot.getItem();
        if (AEItems.REDSTONE_CARD.isSameAs(source)) {
            return super.quickMoveStack(player, index);
        }

        ItemStack original = source.copy();
        int inserted = insertShiftClickedItem(source);
        if (inserted <= 0) {
            return ItemStack.EMPTY;
        }
        source.shrink(inserted);
        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        broadcastChanges();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, APBlocks.SEQUENCE_BUFFER.get());
    }

    public final int memberCount() {
        return Math.max(0, memberCount);
    }

    public final boolean autoOutput() {
        return autoOutput;
    }

    public final boolean blockingMode() {
        return blockingMode;
    }

    public final boolean synchronizedOutput() {
        return synchronizedOutput;
    }

    public final boolean patternMode() {
        return patternMode;
    }

    public final int inputDelayTicks() {
        return Math.max(0, inputDelayTicks);
    }

    public final void toggleAutoOutput() {
        if (isClientSide()) {
            sendClientAction(ACTION_TOGGLE_AUTO_OUTPUT);
            return;
        }
        updateConfiguration(configuration -> configuration.setAutoOutput(!configuration.autoOutput()));
    }

    public final void toggleBlockingMode() {
        if (isClientSide()) {
            sendClientAction(ACTION_TOGGLE_BLOCKING_MODE);
            return;
        }
        updateConfiguration(configuration -> configuration.setBlockingMode(!configuration.blockingMode()));
    }

    public final void toggleSynchronizedOutput() {
        if (isClientSide()) {
            sendClientAction(ACTION_TOGGLE_SYNCHRONIZED_OUTPUT);
            return;
        }
        updateConfiguration(configuration ->
                configuration.setSynchronizedOutput(!configuration.synchronizedOutput()));
    }

    public final void togglePatternMode() {
        if (isClientSide()) {
            sendClientAction(ACTION_TOGGLE_PATTERN_MODE);
            return;
        }
        updateConfiguration(configuration -> configuration.setPatternMode(!configuration.patternMode()));
    }

    public final void cycleInputDelay(boolean backwards) {
        if (isClientSide()) {
            sendClientAction(ACTION_CYCLE_INPUT_DELAY, backwards ? -1 : 1);
            return;
        }
        cycleInputDelay(backwards ? -1 : 1);
    }

    public int maxScrollOffset() {
        return 0;
    }

    public final int scrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset()));
    }

    public final boolean isStorageSlotEnabled(int visibleIndex) {
        int memberIndex = memberIndexForVisibleSlot(visibleIndex);
        return memberIndex >= 0 && memberIndex < memberCount();
    }

    protected final SequenceBufferBlockEntity viewedBlock() {
        return viewedBlock;
    }

    protected abstract int currentMemberCount();

    protected abstract int memberIndexForVisibleSlot(int visibleIndex);

    protected abstract SequenceBufferBlockEntity memberForVisibleSlot(int visibleIndex);

    protected abstract int insertShiftClickedItem(ItemStack stack);

    private void cycleInputDelay(Integer direction) {
        int step = direction != null && direction < 0 ? -1 : 1;
        updateConfiguration(configuration -> configuration.setInputDelayTicks(
                nextInputDelayPreset(configuration.inputDelayTicks(), step)));
    }

    private void updateConfiguration(java.util.function.Consumer<SequenceBufferConfiguration> change) {
        SequenceBufferConfiguration configuration = getHost().configurationCopy();
        change.accept(configuration);
        getHost().updateConfiguration(configuration);
        synchronizeConfigurationFields();
        broadcastChanges();
    }

    private void synchronizeConfigurationFields() {
        SequenceBufferConfiguration configuration = getHost().configurationCopy();
        autoOutput = configuration.autoOutput();
        blockingMode = configuration.blockingMode();
        synchronizedOutput = configuration.synchronizedOutput();
        patternMode = configuration.patternMode();
        inputDelayTicks = configuration.inputDelayTicks();
    }

    static int nextInputDelayPreset(int current, int direction) {
        if (direction < 0) {
            for (int index = INPUT_DELAY_PRESETS.length - 1; index >= 0; index--) {
                if (INPUT_DELAY_PRESETS[index] < current) {
                    return INPUT_DELAY_PRESETS[index];
                }
            }
            return INPUT_DELAY_PRESETS[INPUT_DELAY_PRESETS.length - 1];
        }
        for (int preset : INPUT_DELAY_PRESETS) {
            if (preset > current) {
                return preset;
            }
        }
        return INPUT_DELAY_PRESETS[0];
    }

    private void clickStorageSlot(int visibleIndex, int button, ClickType clickType, Player player) {
        if (player.level().isClientSide || !isStorageSlotEnabled(visibleIndex)) {
            return;
        }
        if (clickType == ClickType.QUICK_MOVE) {
            quickMoveStorage(visibleIndex);
            broadcastChanges();
            return;
        }
        if (clickType != ClickType.PICKUP) {
            return;
        }

        SequenceBufferBlockEntity member = memberForVisibleSlot(visibleIndex);
        if (member == null) {
            return;
        }
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            int requested = button == 1 ? 1 : 64;
            ItemStack extracted = member.extractMenuItem(requested, false);
            if (!extracted.isEmpty()) {
                setCarried(extracted);
            }
        } else if (!GenericStack.isWrapped(carried)) {
            int requested = button == 1 ? 1 : carried.getCount();
            int inserted = member.insertMenuItem(carried, requested, false);
            if (inserted > 0) {
                carried.shrink(inserted);
                setCarried(carried);
            }
        }
        broadcastChanges();
    }

    private ItemStack quickMoveStorage(int visibleIndex) {
        SequenceBufferBlockEntity member = memberForVisibleSlot(visibleIndex);
        if (member == null) {
            return ItemStack.EMPTY;
        }
        ItemStack simulated = member.extractMenuItem(64, true);
        if (simulated.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = simulated.copy();
        if (!moveItemStackToPlayerInventory(remaining)) {
            return ItemStack.EMPTY;
        }
        int moved = simulated.getCount() - remaining.getCount();
        if (moved <= 0) {
            return ItemStack.EMPTY;
        }
        member.extractMenuItem(moved, false);
        return simulated;
    }

    private boolean moveItemStackToPlayerInventory(ItemStack stack) {
        int start = slots.size();
        int end = -1;
        for (int menuIndex = 0; menuIndex < slots.size(); menuIndex++) {
            if (!isPlayerInventorySlot(slots.get(menuIndex))) {
                continue;
            }
            start = Math.min(start, menuIndex);
            end = Math.max(end, menuIndex + 1);
        }
        return start <= end && moveItemStackTo(stack, start, end, true);
    }

    private boolean isPlayerInventorySlot(Slot slot) {
        return getSlotSemantic(slot) == SlotSemantics.PLAYER_INVENTORY
                || getSlotSemantic(slot) == SlotSemantics.PLAYER_HOTBAR;
    }

    private final class StorageDisplaySlot extends Slot implements IOptionalSlot {
        private final int visibleIndex;
        private ItemStack clientDisplay = ItemStack.EMPTY;

        private StorageDisplaySlot(int visibleIndex) {
            super(DISPLAY_CONTAINER, 0, 0, 0);
            this.visibleIndex = visibleIndex;
        }

        @Override
        public ItemStack getItem() {
            if (AbstractSequenceBufferMenu.this.isClientSide()) {
                return clientDisplay;
            }
            SequenceBufferBlockEntity member = memberForVisibleSlot(visibleIndex);
            return member == null ? ItemStack.EMPTY : member.menuDisplayStack();
        }

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }

        @Override
        public void set(ItemStack stack) {
            if (AbstractSequenceBufferMenu.this.isClientSide()) {
                clientDisplay = stack.copy();
            }
        }

        @Override
        public ItemStack remove(int amount) {
            return ItemStack.EMPTY;
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
        public boolean isActive() {
            return isSlotEnabled();
        }

        @Override
        public boolean isSlotEnabled() {
            return isStorageSlotEnabled(visibleIndex);
        }

        @Override
        public boolean isRenderDisabled() {
            return false;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public Point getBackgroundPos() {
            return new Point(x - 1, y - 1);
        }

        @Override
        public void setChanged() {
        }
    }
}

package com.warmthdawn.appliedpackaging.world.menu;

import appeng.core.definitions.AEItems;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.OptionalFakeSlot;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class MePackagerMenu extends UpgradeableMenu<MePackagerBlockEntity> {
    public static final int BUTTON_PACK_ONCE = 0;
    public static final int BUTTON_MARKER_MODE = 1;
    public static final int BUTTON_REDSTONE_MODE = 2;
    public static final int BUTTON_COLOR_BASE = 10;

    private static final String ACTION_PACK_ONCE = "packOnce";
    private static final String ACTION_CLEAR = "clear";
    private static final String ACTION_PARTITION = "partition";
    private static final String ACTION_SET_COLOR = "setColor";
    private static final String ACTION_CYCLE_MARKER = "cycleMarker";
    private static final String ACTION_CYCLE_FILTER = "cycleFilter";
    private static final String ACTION_CYCLE_ACTIVATION = "cycleActivation";
    private static final String ACTION_CYCLE_BLOCKING = "cycleBlocking";
    private static final String ACTION_SET_NAME = "setName";

    @GuiSync(10)
    public PackageColor selectedColor = PackageColor.FLUIX;

    @GuiSync(11)
    public MarkerMergeMode markerMode = MarkerMergeMode.RETAIN;

    @GuiSync(12)
    public MePackagerBlockEntity.RedstoneMode activationMode = MePackagerBlockEntity.RedstoneMode.HIGH_SIGNAL;

    @GuiSync(13)
    public MePackagerBlockEntity.FilterApplicationMode filterMode =
            MePackagerBlockEntity.FilterApplicationMode.BOTH;

    @GuiSync(14)
    public MePackagerBlockEntity.BlockingMode blockingMode =
            MePackagerBlockEntity.BlockingMode.IGNORE_NETWORK_CONTENTS;

    @GuiSync(15)
    public String packageName = "";

    @GuiSync(16)
    public int workTicksRemaining = 0;

    @GuiSync(17)
    public int workingOperation = MePackagerBlockEntity.WorkingOperation.NONE.ordinal();

    public MePackagerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buffer.readBlockPos()));
    }

    public MePackagerMenu(int containerId, Inventory playerInventory, MePackagerBlockEntity blockEntity) {
        super(APMenus.ME_PACKAGER.get(), containerId, playerInventory, blockEntity);

        registerClientAction(ACTION_PACK_ONCE, this::packOnce);
        registerClientAction(ACTION_CLEAR, this::clear);
        registerClientAction(ACTION_PARTITION, this::partition);
        registerClientAction(ACTION_SET_COLOR, PackageColor.class, this::setSelectedColor);
        registerClientAction(ACTION_CYCLE_MARKER, this::cycleMarkerMode);
        registerClientAction(ACTION_CYCLE_FILTER, this::cycleFilterMode);
        registerClientAction(ACTION_CYCLE_ACTIVATION, this::cycleActivationMode);
        registerClientAction(ACTION_CYCLE_BLOCKING, this::cycleBlockingMode);
        registerClientAction(ACTION_SET_NAME, String.class, this::setPackageName);
    }

    @Override
    protected void setupInventorySlots() {
        addSlot(
                new MenuInputPackageSlot(getHost()),
                SlotSemantics.MACHINE_INPUT);
        addSlot(new OutputPackageSlot(getHost()), SlotSemantics.MACHINE_OUTPUT);
        addSlot(
                new SlotItemHandler(getHost().getItems(), MePackagerBlockEntity.SLOT_CAPACITY, 0, 0),
                SlotSemantics.STORAGE_CELL);
        addSlot(
                new SlotItemHandler(getHost().getItems(), MePackagerBlockEntity.SLOT_MARKER, 0, 0),
                SlotSemantics.BLANK_PATTERN);
    }

    @Override
    protected void setupConfig() {
        addExpandableConfigSlots(
                getHost().getContentFilter(),
                MePackagerBlockEntity.BASE_FILTER_ROWS,
                MePackagerBlockEntity.FILTER_COLUMNS,
                MePackagerBlockEntity.MAX_FILTER_CAPACITY_CARDS);
        for (var slot : getSlots(SlotSemantics.CONFIG)) {
            if (slot instanceof OptionalFakeSlot optionalSlot) {
                optionalSlot.setRenderDisabled(false);
            }
        }
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        return getUpgrades().getInstalledUpgrades(AEItems.CAPACITY_CARD) > idx;
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            selectedColor = getHost().selectedColor();
            markerMode = getHost().markerMode();
            activationMode = getHost().redstoneMode();
            filterMode = getHost().filterApplicationMode();
            blockingMode = getHost().blockingMode();
            packageName = getHost().packageName();
            workTicksRemaining = getHost().animationTicks();
            workingOperation = getHost().workingOperation().ordinal();
        }
        super.broadcastChanges();
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

        ItemStack source = slot.getItem();
        if (isPlayerInventorySlot(slot) && PackageDataStorage.read(source).isPresent()) {
            ItemStack singlePackage = source.copy();
            singlePackage.setCount(1);
            ItemStack remainder = getHost().insertPackageFromMenu(singlePackage, false);
            if (!remainder.isEmpty()) {
                return ItemStack.EMPTY;
            }
            source.shrink(1);
            if (source.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return singlePackage;
        }

        return super.quickMoveStack(player, index);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_COLOR_BASE && id < BUTTON_COLOR_BASE + PackageColor.values().length) {
            if (!player.level().isClientSide) {
                setSelectedColor(PackageColor.values()[id - BUTTON_COLOR_BASE]);
            }
            return true;
        }
        if (id == BUTTON_MARKER_MODE) {
            if (!player.level().isClientSide) {
                cycleMarkerMode();
            }
            return true;
        }
        if (id == BUTTON_REDSTONE_MODE) {
            if (!player.level().isClientSide) {
                cycleActivationMode();
            }
            return true;
        }
        if (id == BUTTON_PACK_ONCE) {
            if (!player.level().isClientSide) {
                packOnce();
            }
            return true;
        }
        return false;
    }

    public void packOnce() {
        if (isClientSide()) {
            sendClientAction(ACTION_PACK_ONCE);
            return;
        }

        MePackagerBlockEntity.MachineResult result = getHost().runOnce();
        getPlayer().displayClientMessage(Component.translatable(result.messageKey()), true);
    }

    public void clear() {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR);
            return;
        }

        getHost().clearConfiguration();
        broadcastChanges();
    }

    public void partition() {
        if (isClientSide()) {
            sendClientAction(ACTION_PARTITION);
            return;
        }

        getHost().partitionFilterFromNetwork();
        broadcastChanges();
    }

    public void setSelectedColor(PackageColor color) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_COLOR, color);
            return;
        }

        getHost().setSelectedColor(color);
        selectedColor = getHost().selectedColor();
        broadcastChanges();
    }

    public void cycleMarkerMode() {
        if (isClientSide()) {
            sendClientAction(ACTION_CYCLE_MARKER);
            return;
        }

        getHost().cycleMarkerMode();
        markerMode = getHost().markerMode();
        broadcastChanges();
    }

    public void cycleFilterMode() {
        if (isClientSide()) {
            sendClientAction(ACTION_CYCLE_FILTER);
            return;
        }

        getHost().cycleFilterApplicationMode();
        filterMode = getHost().filterApplicationMode();
        broadcastChanges();
    }

    public void cycleActivationMode() {
        if (isClientSide()) {
            sendClientAction(ACTION_CYCLE_ACTIVATION);
            return;
        }

        getHost().cycleRedstoneMode();
        activationMode = getHost().redstoneMode();
        broadcastChanges();
    }

    public void cycleBlockingMode() {
        if (isClientSide()) {
            sendClientAction(ACTION_CYCLE_BLOCKING);
            return;
        }

        getHost().cycleBlockingMode();
        blockingMode = getHost().blockingMode();
        broadcastChanges();
    }

    public void setPackageName(String name) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_NAME, name);
            return;
        }

        getHost().setPackageName(name);
        packageName = getHost().packageName();
        broadcastChanges();
    }

    public PackageColor selectedColor() {
        return selectedColor == null ? PackageColor.FLUIX : selectedColor;
    }

    public MarkerMergeMode markerMode() {
        return markerMode == null ? MarkerMergeMode.RETAIN : markerMode;
    }

    public MePackagerBlockEntity.RedstoneMode activationMode() {
        return activationMode == null
                ? MePackagerBlockEntity.RedstoneMode.HIGH_SIGNAL
                : activationMode;
    }

    public MePackagerBlockEntity.FilterApplicationMode filterMode() {
        return filterMode == null
                ? MePackagerBlockEntity.FilterApplicationMode.BOTH
                : filterMode;
    }

    public MePackagerBlockEntity.BlockingMode blockingMode() {
        return blockingMode == null
                ? MePackagerBlockEntity.BlockingMode.IGNORE_NETWORK_CONTENTS
                : blockingMode;
    }

    public String packageName() {
        return packageName == null ? "" : packageName;
    }

    public MePackagerBlockEntity.WorkingOperation workingOperation() {
        MePackagerBlockEntity.WorkingOperation[] values = MePackagerBlockEntity.WorkingOperation.values();
        if (workingOperation < 0 || workingOperation >= values.length) {
            return MePackagerBlockEntity.WorkingOperation.NONE;
        }
        return values[workingOperation];
    }

    public boolean isWorking() {
        return workingOperation() != MePackagerBlockEntity.WorkingOperation.NONE;
    }

    public float workProgress(float partialTicks) {
        if (!isWorking() && workTicksRemaining <= 0) {
            return 0.0F;
        }
        float remaining = Math.max(0.0F, workTicksRemaining - partialTicks);
        return Mth.clamp(
                1.0F - remaining / MePackagerBlockEntity.ANIMATION_CYCLE_TICKS,
                0.0F,
                1.0F);
    }

    private static MePackagerBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MePackagerBlockEntity packager) {
            return packager;
        }
        throw new IllegalStateException("Expected ME Packager block entity at " + pos);
    }

    private boolean isPlayerInventorySlot(Slot slot) {
        return getSlotSemantic(slot) == SlotSemantics.PLAYER_INVENTORY
                || getSlotSemantic(slot) == SlotSemantics.PLAYER_HOTBAR;
    }

    private static final class MenuInputPackageSlot extends SlotItemHandler {
        private MenuInputPackageSlot(MePackagerBlockEntity blockEntity) {
            super(blockEntity.getItems(), MePackagerBlockEntity.SLOT_INPUT, -9999, -9999);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player playerIn) {
            return false;
        }
    }

    private static final class OutputPackageSlot extends SlotItemHandler {
        private OutputPackageSlot(MePackagerBlockEntity blockEntity) {
            super(blockEntity.getItems(), MePackagerBlockEntity.SLOT_OUTPUT, 0, 0);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}

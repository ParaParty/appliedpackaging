package com.warmthdawn.appliedpackaging.world.block.entity;

import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageService;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.util.ConfigInventory;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackagePlan;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackageTransactions;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.world.menu.MePackagerMenu;
import com.warmthdawn.appliedpackaging.world.block.InventoryDroppingBlockEntity;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class MePackagerBlockEntity extends AENetworkBlockEntity
        implements InventoryDroppingBlockEntity, MenuProvider, IUpgradeableObject {
    public static final PackageCapacityProfile BASE_CAPACITY_PROFILE = PackageCapacityProfile.DEFAULT;
    public static final int SLOT_HELD_BOX = 0;
    public static final int SLOT_INPUT = SLOT_HELD_BOX;
    public static final int SLOT_OUTPUT = SLOT_HELD_BOX;
    public static final int SLOT_CAPACITY = 1;
    public static final int FILTER_COLUMNS = 9;
    public static final int FILTER_ROWS = 5;
    public static final int BASE_FILTER_ROWS = 2;
    public static final int MAX_FILTER_CAPACITY_CARDS = 3;
    public static final int FILTER_SLOT_COUNT = FILTER_COLUMNS * FILTER_ROWS;
    public static final int UPGRADE_SLOT_COUNT = 6;
    public static final int CYCLIC_REDSTONE_INTERVAL_TICKS = 20;
    public static final int PACKING_BASE_WORK_TICKS = 40;
    public static final int UNPACKING_BASE_WORK_TICKS = 20;
    public static final int MIN_WORK_TICKS = 4;
    public static final int ANIMATION_CYCLE_TICKS = 20;
    public static final int BELT_SCROLL_PERIOD_PIXELS = 16;
    public static final float BELT_PACKAGE_TRAVEL_PIXELS = 9.0F;
    private static final float CURTAIN_SPRING_STRENGTH = 0.35F;
    private static final float CURTAIN_DAMPING = 0.65F;
    private static final float CURTAIN_MIN_DRIVE = 0.65F;
    private static final float CURTAIN_SPEED_WEIGHT = 0.7F;
    private static final float CURTAIN_REST_EPSILON = 0.001F;
    private static final int[] PACKING_WORK_TICKS_BY_SPEED = {40, 30, 20, 15, 10, 6, 4};
    private static final int[] UNPACKING_WORK_TICKS_BY_SPEED = {20, 15, 10, 6, 4};
    private static final int SLOT_COUNT = 2;
    private static final String ITEMS_TAG = "items";
    private static final String FILTER_TAG = "content_filter";
    private static final String MARKER_FILTER_TAG = "marker_filter";
    private static final String UPGRADES_TAG = "upgrades";
    private static final String POWERED_TAG = "powered";
    private static final String SELECTED_COLOR_TAG = "selected_color";
    private static final String MARKER_MODE_TAG = "marker_mode";
    private static final String REDSTONE_MODE_TAG = "redstone_mode";
    private static final String FILTER_APPLICATION_MODE_TAG = "filter_application_mode";
    private static final String BLOCKING_MODE_TAG = "blocking_mode";
    private static final String ANTI_CLOG_MODE_TAG = "anti_clog_mode";
    private static final String ANIMATION_TICKS_TAG = "animation_ticks";
    private static final String ANIMATION_DURATION_TICKS_TAG = "animation_duration_ticks";
    private static final String ANIMATION_INWARD_TAG = "animation_inward";
    private static final String CURTAIN_DEFLECTION_TAG = "curtain_deflection";
    private static final String CURTAIN_VELOCITY_TAG = "curtain_velocity";
    private static final String BELT_SCROLL_PIXELS_TAG = "belt_scroll_pixels";
    private static final String RENDERED_BOX_TAG = "rendered_box";
    private static final String WORKING_OPERATION_TAG = "working_operation";
    private static final String WORKING_STACK_TAG = "working_stack";
    private static final String PENDING_PACK_TRIGGER_TAG = "pending_pack_trigger";
    private static final String HELD_BOX_STATE_TAG = "held_box_state";
    private static final String UNPACK_BLOCKED_TAG = "unpack_blocked";

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_INPUT) {
                return PackageDataStorage.read(stack).isPresent();
            }
            if (slot == SLOT_CAPACITY) {
                return PackageCapacityProfile.fromStorageComponent(stack).isPresent();
            }
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == SLOT_INPUT || slot == SLOT_CAPACITY) {
                return 1;
            }
            return super.getSlotLimit(slot);
        }

        @Override
        protected void onContentsChanged(int slot) {
            onInventorySlotChanged(slot);
            setChanged();
        }
    };
    private final IItemHandlerModifiable heldBoxItems = new IItemHandlerModifiable() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? heldBox() : ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot != 0) {
                return;
            }

            ItemStack value = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
            if (level != null && level.isClientSide) {
                items.setStackInSlot(SLOT_INPUT, value);
                return;
            }

            if (value.isEmpty()) {
                if (!isWorking()) {
                    items.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
                    heldBoxState = HeldBoxState.EMPTY;
                    unpackBlocked = false;
                    renderedBox = ItemStack.EMPTY;
                    syncVisualState();
                }
                return;
            }

            if (heldBox().isEmpty() && heldBoxState == HeldBoxState.EMPTY) {
                insertPackageFromExternal(value, false);
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? insertPackageFromExternal(stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? extractHeldBox(amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 1 : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && PackageDataStorage.read(stack).isPresent();
        }
    };
    private final ConfigInventory contentFilter = ConfigInventory.configStacks(
            null,
            FILTER_SLOT_COUNT,
            this::onFilterChanged,
            true);
    private final ConfigInventory markerFilter = ConfigInventory.configTypes(
            key -> key instanceof AEItemKey itemKey && !(itemKey.getItem() instanceof PackageItem),
            1,
            this::onFilterChanged);
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            APBlocks.ME_PACKAGER.get(),
            UPGRADE_SLOT_COUNT,
            this::onUpgradesChanged);
    private final IItemHandler externalItems = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? heldBox() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? insertPackageFromExternal(stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? extractHeldBox(amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 1 : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && PackageDataStorage.read(stack).isPresent();
        }
    };
    private LazyOptional<IItemHandler> internalItemHandler = createInternalItemHandlerCapability();
    private LazyOptional<IItemHandler> externalItemHandler = createExternalItemHandlerCapability();
    private boolean powered;
    private int redstoneCooldown;
    private int animationTicks;
    private int animationDurationTicks = PACKING_BASE_WORK_TICKS;
    private boolean animationInward = true;
    private float curtainDeflection;
    private float previousCurtainDeflection;
    private float curtainVelocity;
    private float beltScrollPixels;
    private ItemStack renderedBox = ItemStack.EMPTY;
    private ItemStack workingStack = ItemStack.EMPTY;
    private WorkingOperation workingOperation = WorkingOperation.NONE;
    private HeldBoxState heldBoxState = HeldBoxState.EMPTY;
    private boolean unpackBlocked;
    private PackageColor selectedColor = PackageColor.FLUIX;
    private MarkerMergeMode markerMode = MarkerMergeMode.RETAIN;
    private RedstoneMode redstoneMode = RedstoneMode.HIGH_SIGNAL;
    private FilterApplicationMode filterApplicationMode = FilterApplicationMode.BOTH;
    private BlockingMode blockingMode = BlockingMode.IGNORE_NETWORK_CONTENTS;
    private boolean antiClogMode = true;
    private boolean pendingPackTrigger;

    public MePackagerBlockEntity(BlockPos pos, BlockState blockState) {
        super(APBlockEntities.ME_PACKAGER.get(), pos, blockState);
        getMainNode().setIdlePowerUsage(1.0D);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public IItemHandlerModifiable getHeldBoxItems() {
        return heldBoxItems;
    }

    public ConfigInventory getContentFilter() {
        return contentFilter;
    }

    public ConfigInventory getMarkerFilter() {
        return markerFilter;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    public PackageColor selectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(PackageColor selectedColor) {
        this.selectedColor = selectedColor == null ? PackageColor.FLUIX : selectedColor;
        setChanged();
    }

    public MarkerMergeMode markerMode() {
        return markerMode;
    }

    public void setMarkerMode(MarkerMergeMode markerMode) {
        this.markerMode = markerMode == null ? MarkerMergeMode.RETAIN : markerMode;
        setChanged();
    }

    public void cycleMarkerMode() {
        MarkerMergeMode[] values = MarkerMergeMode.values();
        setMarkerMode(values[(markerMode.ordinal() + 1) % values.length]);
    }

    public RedstoneMode redstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode redstoneMode) {
        this.redstoneMode = redstoneMode == null ? RedstoneMode.HIGH_SIGNAL : redstoneMode;
        this.redstoneCooldown = 0;
        setChanged();
    }

    public void cycleRedstoneMode() {
        RedstoneMode[] values = RedstoneMode.uiValues();
        RedstoneMode current = redstoneMode;
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                index = i;
                break;
            }
        }
        setRedstoneMode(values[(index + 1) % values.length]);
    }

    public RedstoneMode effectiveRedstoneMode() {
        return redstoneMode;
    }

    public FilterApplicationMode filterApplicationMode() {
        return filterApplicationMode;
    }

    public void setFilterApplicationMode(FilterApplicationMode filterApplicationMode) {
        this.filterApplicationMode = filterApplicationMode == null
                ? FilterApplicationMode.BOTH
                : filterApplicationMode;
        setChanged();
    }

    public void cycleFilterApplicationMode() {
        FilterApplicationMode[] values = FilterApplicationMode.values();
        setFilterApplicationMode(values[(filterApplicationMode.ordinal() + 1) % values.length]);
    }

    public BlockingMode blockingMode() {
        return blockingMode;
    }

    public void setBlockingMode(BlockingMode blockingMode) {
        this.blockingMode = blockingMode == null ? BlockingMode.IGNORE_NETWORK_CONTENTS : blockingMode;
        setChanged();
    }

    public void cycleBlockingMode() {
        BlockingMode[] values = BlockingMode.values();
        setBlockingMode(values[(blockingMode.ordinal() + 1) % values.length]);
    }

    public boolean antiClogMode() {
        return antiClogMode;
    }

    public void setAntiClogMode(boolean antiClogMode) {
        this.antiClogMode = antiClogMode;
        setChanged();
    }

    public void toggleAntiClogMode() {
        setAntiClogMode(!antiClogMode);
    }

    public int unlockedFilterRows() {
        return BASE_FILTER_ROWS + Math.min(MAX_FILTER_CAPACITY_CARDS, installedCapacityCards());
    }

    public int installedCapacityCards() {
        return getUpgrades().getInstalledUpgrades(AEItems.CAPACITY_CARD);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.appliedpackaging.me_packager");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MePackagerMenu(containerId, playerInventory, this);
    }

    public ActionResult interact(Player player, ItemStack held) {
        if (!held.isEmpty() && PackageDataStorage.read(held).isPresent()) {
            ItemStack one = held.copy();
            one.setCount(1);
            ItemStack remainder = insertPackageFromExternal(one, false);
            if (remainder.isEmpty()) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                return ActionResult.consumed("message.appliedpackaging.me_packager.inserted_package");
            }
            return ActionResult.consumed("message.appliedpackaging.me_packager.input_blocked");
        }

        HeldBoxState previousState = heldBoxState;
        ItemStack extractedBox = extractHeldBox(1, false);
        if (!extractedBox.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(player, extractedBox);
            syncVisualState();
            return ActionResult.consumed(previousState == HeldBoxState.PACK_OUTPUT
                    ? "message.appliedpackaging.me_packager.output_taken"
                    : "message.appliedpackaging.me_packager.input_taken");
        }

        return ActionResult.consumed(null);
    }

    public void updatePowered(boolean nowPowered) {
        boolean wasPowered = powered;
        powered = nowPowered;
        if (!nowPowered) {
            redstoneCooldown = 0;
        }
        if (effectiveRedstoneMode() == RedstoneMode.PULSE && nowPowered && !wasPowered
                && heldBoxState == HeldBoxState.EMPTY) {
            if (isWorking()) {
                pendingPackTrigger = true;
            } else {
                runPackOnce();
            }
        }
        setChanged();
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            tickAnimation();
            return;
        }
        boolean wasWorking = isWorking();
        tickAnimation();
        boolean nowPowered = level.hasNeighborSignal(worldPosition);
        if (nowPowered != powered) {
            updatePowered(nowPowered);
        }
        if (wasWorking || isWorking()) {
            return;
        }
        if (pendingPackTrigger) {
            pendingPackTrigger = false;
            runPackOnce();
            return;
        }
        if (heldBoxState == HeldBoxState.UNPACK_INPUT && !heldBox().isEmpty()) {
            tickAutomaticUnpack();
            return;
        }
        if (heldBoxState == HeldBoxState.PACK_OUTPUT) {
            return;
        }
        tickAutomaticPack();
    }

    private void tickAutomaticUnpack() {
        if (redstoneCooldown > 0) {
            redstoneCooldown--;
            return;
        }
        MachineResult result = runUnpackOnce();
        redstoneCooldown = result == MachineResult.UNPACKED ? 0 : unpackingRetryIntervalTicks();
    }

    private void tickAutomaticPack() {
        RedstoneMode effectiveMode = effectiveRedstoneMode();
        if (effectiveMode == RedstoneMode.PULSE || effectiveMode == RedstoneMode.NEVER) {
            return;
        }
        if (!effectiveMode.isActive(powered)) {
            redstoneCooldown = 0;
            return;
        }
        if (redstoneCooldown > 0) {
            redstoneCooldown--;
            return;
        }
        MachineResult result = runPackOnce();
        redstoneCooldown = result == MachineResult.PACKED ? 0 : packingRetryIntervalTicks();
    }

    public MachineResult runOnce() {
        if (level == null || level.isClientSide) {
            return MachineResult.NO_TARGET;
        }
        if (isWorking()) {
            return MachineResult.WORKING;
        }
        Optional<MEStorage> meStorage = findTargetMEStorage();
        if (meStorage.isEmpty()) {
            return MachineResult.NO_TARGET;
        }

        if (heldBoxState == HeldBoxState.UNPACK_INPUT && !heldBox().isEmpty()) {
            return unpackOne(meStorage.get(), heldBox());
        }
        if (heldBoxState == HeldBoxState.PACK_OUTPUT) {
            return MachineResult.OUTPUT_BLOCKED;
        }
        return packOne(meStorage.get());
    }

    public MachineResult runPackOnce() {
        if (level == null || level.isClientSide) {
            return MachineResult.NO_TARGET;
        }
        if (isWorking()) {
            pendingPackTrigger = true;
            return MachineResult.WORKING;
        }
        Optional<MEStorage> meStorage = findTargetMEStorage();
        if (meStorage.isEmpty()) {
            return MachineResult.NO_TARGET;
        }
        return packOne(meStorage.get());
    }

    private MachineResult runUnpackOnce() {
        if (level == null || level.isClientSide) {
            return MachineResult.NO_TARGET;
        }
        if (isWorking()) {
            return MachineResult.WORKING;
        }
        Optional<MEStorage> meStorage = findTargetMEStorage();
        if (meStorage.isEmpty()) {
            return MachineResult.NO_TARGET;
        }
        ItemStack input = heldBox();
        if (heldBoxState != HeldBoxState.UNPACK_INPUT || input.isEmpty()) {
            return MachineResult.NO_CONTENTS;
        }
        return unpackOne(meStorage.get(), input);
    }

    private MachineResult packOne(MEStorage source) {
        if (heldBoxState != HeldBoxState.EMPTY || !heldBox().isEmpty()) {
            return MachineResult.OUTPUT_BLOCKED;
        }
        PackageFilter configuredFilter = configuredFilter();
        PackageFilter filter = filterApplicationMode.appliesToPack() ? configuredFilter : PackageFilter.any();
        PackageColor color = filter.color().orElse(selectedColor);
        PackageFilter packingFilter = configuredPackingFilter(filter);
        Optional<MEStoragePackagePlan> plan = MEStoragePackageTransactions.planPack(
                source,
                color,
                configuredCapacityProfile(),
                packingFilter,
                markerMode,
                configuredOverrideMarker(filter),
                contentFilterInverted());
        if (plan.isEmpty()) {
            return MachineResult.NO_CONTENTS;
        }
        if (!MEStoragePackageTransactions.canExtract(source, plan.get())) {
            return MachineResult.SOURCE_CHANGED;
        }

        ItemStack packageStack = new ItemStack(APItems.packageItems().get(color).get());
        PackageDataStorage.write(packageStack, plan.get().data());
        if (!MEStoragePackageTransactions.commitExtract(source, plan.get())) {
            return MachineResult.SOURCE_CHANGED;
        }
        startWorkingOperation(WorkingOperation.PACKING, packageStack);
        setChanged();
        return MachineResult.PACKED;
    }

    private MachineResult unpackOne(MEStorage target, ItemStack input) {
        MachineResult validation = validateUnpackInput(target, input);
        if (validation != MachineResult.UNPACKED) {
            return validation;
        }
        unpackBlocked = false;
        startWorkingOperation(WorkingOperation.UNPACKING, input);
        setChanged();
        return MachineResult.UNPACKED;
    }

    private ItemStack insertPackageFromExternal(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (isWorking() || heldBoxState != HeldBoxState.EMPTY || !heldBox().isEmpty()) {
            return stack;
        }
        ItemStack one = stack.copy();
        one.setCount(1);
        MachineResult inputValidation = validateUnpackInputIdentity(one, false);
        if (inputValidation != MachineResult.UNPACKED) {
            return stack;
        }
        Optional<MEStorage> meStorage = findTargetMEStorage();
        MachineResult outputReadiness = meStorage
                .map(storage -> validateUnpackOutput(storage, one))
                .orElse(MachineResult.NO_TARGET);
        if (antiClogMode && outputReadiness != MachineResult.UNPACKED) {
            return stack;
        }

        ItemStack remainder = stack.copy();
        remainder.shrink(1);
        if (!simulate) {
            heldBoxState = HeldBoxState.UNPACK_INPUT;
            items.setStackInSlot(SLOT_INPUT, one);
            if (outputReadiness == MachineResult.UNPACKED) {
                unpackBlocked = false;
                startWorkingOperation(WorkingOperation.UNPACKING, one);
            } else {
                unpackBlocked = true;
                redstoneCooldown = 0;
                renderedBox = displayStack(one);
                syncVisualState();
            }
            setChanged();
        }
        return remainder.isEmpty() ? ItemStack.EMPTY : remainder;
    }

    private ItemStack heldBox() {
        return items.getStackInSlot(SLOT_INPUT);
    }

    private ItemStack extractHeldBox(int amount, boolean simulate) {
        if (amount <= 0 || isWorking() || heldBoxState == HeldBoxState.EMPTY) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = items.extractItem(SLOT_INPUT, 1, simulate);
        if (!simulate && !extracted.isEmpty()) {
            heldBoxState = HeldBoxState.EMPTY;
            unpackBlocked = false;
            renderedBox = ItemStack.EMPTY;
            syncVisualState();
        }
        return extracted;
    }

    private MachineResult validateUnpackInput(MEStorage target, ItemStack input) {
        return validateUnpackInput(target, input, false);
    }

    private MachineResult validateUnpackInput(MEStorage target, ItemStack input, boolean allowCurrentWork) {
        MachineResult inputValidation = validateUnpackInputIdentity(input, allowCurrentWork);
        if (inputValidation != MachineResult.UNPACKED) {
            return inputValidation;
        }
        return validateUnpackOutput(target, input);
    }

    private MachineResult validateUnpackInputIdentity(ItemStack input, boolean allowCurrentWork) {
        if (isWorking() && !allowCurrentWork) {
            return MachineResult.WORKING;
        }
        if (heldBoxState == HeldBoxState.PACK_OUTPUT) {
            return MachineResult.OUTPUT_BLOCKED;
        }
        if (filterApplicationMode == FilterApplicationMode.PACK_ONLY) {
            return MachineResult.FILTER_REJECTED;
        }
        if (!(input.getItem() instanceof PackageItem packageItem)) {
            return MachineResult.INVALID_INPUT;
        }
        Optional<PackageData> data = PackageDataStorage.read(input);
        if (data.isEmpty()) {
            return MachineResult.INVALID_INPUT;
        }
        if (!matchesCurrentPackageIdentity(packageItem.color(), data.get())) {
            return MachineResult.FILTER_REJECTED;
        }
        if (!configuredFilter().matchesContents(data.get(), contentFilterInverted())) {
            return MachineResult.FILTER_REJECTED;
        }
        return MachineResult.UNPACKED;
    }

    private MachineResult validateUnpackOutput(MEStorage target, ItemStack input) {
        Optional<PackageData> data = PackageDataStorage.read(input);
        if (data.isEmpty()) {
            return MachineResult.INVALID_INPUT;
        }
        if (blockingMode == BlockingMode.BLOCK_UNPACK_WHEN_NETWORK_HAS_ITEMS && targetHasContents(target)) {
            return MachineResult.TARGET_BLOCKED;
        }
        if (!MEStoragePackageTransactions.canInsertPackageContents(data.get(), target)) {
            return MachineResult.TARGET_BLOCKED;
        }
        return MachineResult.UNPACKED;
    }

    private boolean matchesCurrentPackageIdentity(PackageColor packageColor, PackageData data) {
        if (selectedColor != PackageColor.FLUIX && selectedColor != packageColor) {
            return false;
        }
        Optional<MarkerSpec> marker = configuredMarkerItem();
        return marker.isEmpty() || data.marker().map(actual -> actual.sameAs(marker.get())).orElse(false);
    }

    private Optional<MarkerSpec> configuredMarkerItem() {
        GenericStack markerStack = markerFilter.getStack(0);
        if (markerStack == null
                || !(markerStack.what() instanceof AEItemKey itemKey)
                || itemKey.getItem() instanceof PackageItem) {
            return Optional.empty();
        }
        return Optional.of(new MarkerSpec(new GenericStack(markerStack.what(), 1)));
    }

    private boolean contentFilterInverted() {
        return getUpgrades().isInstalled(AEItems.INVERTER_CARD);
    }

    private PackageCapacityProfile configuredCapacityProfile() {
        return PackageCapacityProfile.fromStorageComponent(items.getStackInSlot(SLOT_CAPACITY))
                .orElse(BASE_CAPACITY_PROFILE);
    }

    private PackageFilter configuredFilter() {
        List<GenericStack> requiredContents = contentFilterStacks();
        if (requiredContents.isEmpty()) {
            return PackageFilter.any();
        }
        return new PackageFilter(Optional.empty(), Optional.empty(), requiredContents);
    }

    private PackageFilter configuredPackingFilter(PackageFilter filter) {
        return new PackageFilter(filter.color(), Optional.empty(), filter.requiredContents());
    }

    private Optional<MarkerSpec> configuredOverrideMarker(PackageFilter filter) {
        Optional<MarkerSpec> markerItem = configuredMarkerItem();
        if (markerItem.isPresent()) {
            return markerItem;
        }
        if (markerMode != MarkerMergeMode.OVERRIDE) {
            return Optional.empty();
        }
        return filter.marker();
    }

    public void clearConfiguration() {
        contentFilter.clear();
        markerFilter.clear();
        setSelectedColor(PackageColor.FLUIX);
        setFilterApplicationMode(FilterApplicationMode.BOTH);
        setBlockingMode(BlockingMode.IGNORE_NETWORK_CONTENTS);
        setAntiClogMode(true);
        setChanged();
    }

    public void partitionFilterFromNetwork() {
        Optional<MEStorage> storage = findTargetMEStorage();
        if (storage.isEmpty()) {
            contentFilter.clear();
            return;
        }
        KeyCounter available = new KeyCounter();
        storage.get().getAvailableStacks(available);
        contentFilter.beginBatch();
        try {
            for (int slot = 0; slot < contentFilter.size(); slot++) {
                contentFilter.setStack(slot, null);
            }
            int slot = 0;
            int enabledSlots = unlockedFilterRows() * FILTER_COLUMNS;
            for (var entry : available) {
                if (slot >= enabledSlots) {
                    break;
                }
                if (entry.getLongValue() > 0) {
                    contentFilter.setStack(slot++, new GenericStack(entry.getKey(), 1));
                }
            }
        } finally {
            contentFilter.endBatch();
        }
        setChanged();
    }

    private Optional<MEStorage> findTargetMEStorage() {
        if (!getMainNode().isOnline() || !getMainNode().hasGridBooted()) {
            return Optional.empty();
        }
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return Optional.empty();
        }
        IStorageService storageService = grid.getStorageService();
        if (storageService == null) {
            return Optional.empty();
        }
        return Optional.of(storageService.getInventory());
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.of(Direction.DOWN, orientation.getSide(RelativeSide.BACK));
    }

    @Override
    public AECableType getCableConnectionType(Direction side) {
        return getGridConnectableSides(getOrientation()).contains(side)
                ? AECableType.SMART
                : AECableType.NONE;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) {
                return internalItemHandler.cast();
            }
            if (!getGridConnectableSides(getOrientation()).contains(side)) {
                return externalItemHandler.cast();
            }
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        internalItemHandler.invalidate();
        externalItemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        internalItemHandler = createInternalItemHandlerCapability();
        externalItemHandler = createExternalItemHandlerCapability();
    }

    private LazyOptional<IItemHandler> createInternalItemHandlerCapability() {
        return LazyOptional.of(() -> heldBoxItems);
    }

    private LazyOptional<IItemHandler> createExternalItemHandlerCapability() {
        return LazyOptional.of(() -> externalItems);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ITEMS_TAG, items.serializeNBT());
        contentFilter.writeToChildTag(tag, FILTER_TAG);
        markerFilter.writeToChildTag(tag, MARKER_FILTER_TAG);
        upgrades.writeToNBT(tag, UPGRADES_TAG);
        tag.putBoolean(POWERED_TAG, powered);
        tag.putString(SELECTED_COLOR_TAG, selectedColor.id());
        tag.putString(MARKER_MODE_TAG, markerMode.name());
        tag.putString(REDSTONE_MODE_TAG, redstoneMode.name());
        tag.putString(FILTER_APPLICATION_MODE_TAG, filterApplicationMode.name());
        tag.putString(BLOCKING_MODE_TAG, blockingMode.name());
        tag.putBoolean(ANTI_CLOG_MODE_TAG, antiClogMode);
        tag.putInt(ANIMATION_TICKS_TAG, animationTicks);
        tag.putInt(ANIMATION_DURATION_TICKS_TAG, animationDurationTicks);
        tag.putBoolean(ANIMATION_INWARD_TAG, animationInward);
        tag.putFloat(CURTAIN_DEFLECTION_TAG, curtainDeflection);
        tag.putFloat(CURTAIN_VELOCITY_TAG, curtainVelocity);
        tag.putFloat(BELT_SCROLL_PIXELS_TAG, beltScrollPixels);
        if (!renderedBox.isEmpty()) {
            tag.put(RENDERED_BOX_TAG, renderedBox.save(new CompoundTag()));
        }
        tag.putString(WORKING_OPERATION_TAG, workingOperation.name());
        tag.putString(HELD_BOX_STATE_TAG, heldBoxState.name());
        tag.putBoolean(UNPACK_BLOCKED_TAG, unpackBlocked);
        if (!workingStack.isEmpty()) {
            tag.put(WORKING_STACK_TAG, workingStack.save(new CompoundTag()));
        }
        tag.putBoolean(PENDING_PACK_TRIGGER_TAG, pendingPackTrigger);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        if (tag.contains(ITEMS_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            items.deserializeNBT(tag.getCompound(ITEMS_TAG));
        }
        contentFilter.readFromChildTag(tag, FILTER_TAG);
        markerFilter.readFromChildTag(tag, MARKER_FILTER_TAG);
        upgrades.readFromNBT(tag, UPGRADES_TAG);
        powered = tag.getBoolean(POWERED_TAG);
        selectedColor = PackageColor.byId(tag.getString(SELECTED_COLOR_TAG)).orElse(PackageColor.FLUIX);
        markerMode = markerModeByName(tag.getString(MARKER_MODE_TAG));
        redstoneMode = RedstoneMode.byName(tag.getString(REDSTONE_MODE_TAG));
        filterApplicationMode = FilterApplicationMode.byName(tag.getString(FILTER_APPLICATION_MODE_TAG));
        blockingMode = BlockingMode.byName(tag.getString(BLOCKING_MODE_TAG));
        antiClogMode = !tag.contains(ANTI_CLOG_MODE_TAG, Tag.TAG_BYTE) || tag.getBoolean(ANTI_CLOG_MODE_TAG);
        animationDurationTicks = tag.contains(ANIMATION_DURATION_TICKS_TAG, Tag.TAG_INT)
                ? Math.max(MIN_WORK_TICKS, Math.min(PACKING_BASE_WORK_TICKS, tag.getInt(ANIMATION_DURATION_TICKS_TAG)))
                : PACKING_BASE_WORK_TICKS;
        animationTicks = Math.max(0, Math.min(animationDurationTicks, tag.getInt(ANIMATION_TICKS_TAG)));
        animationInward = !tag.contains(ANIMATION_INWARD_TAG) || tag.getBoolean(ANIMATION_INWARD_TAG);
        curtainDeflection = net.minecraft.util.Mth.clamp(tag.getFloat(CURTAIN_DEFLECTION_TAG), -1.0F, 1.0F);
        previousCurtainDeflection = curtainDeflection;
        curtainVelocity = net.minecraft.util.Mth.clamp(tag.getFloat(CURTAIN_VELOCITY_TAG), -1.0F, 1.0F);
        beltScrollPixels = net.minecraft.util.Mth.positiveModulo(
                tag.getFloat(BELT_SCROLL_PIXELS_TAG), BELT_SCROLL_PERIOD_PIXELS);
        renderedBox = tag.contains(RENDERED_BOX_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound(RENDERED_BOX_TAG))
                : currentInventoryBox();
        workingOperation = WorkingOperation.byName(tag.getString(WORKING_OPERATION_TAG));
        heldBoxState = HeldBoxState.byName(tag.getString(HELD_BOX_STATE_TAG));
        unpackBlocked = tag.getBoolean(UNPACK_BLOCKED_TAG);
        workingStack = tag.contains(WORKING_STACK_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound(WORKING_STACK_TAG))
                : ItemStack.EMPTY;
        if (workingOperation == WorkingOperation.NONE) {
            workingStack = ItemStack.EMPTY;
        }
        pendingPackTrigger = tag.getBoolean(PENDING_PACK_TRIGGER_TAG);
    }

    @Override
    protected boolean readFromStream(FriendlyByteBuf data) {
        animationTicks = data.readVarInt();
        animationDurationTicks = Math.max(MIN_WORK_TICKS, Math.min(PACKING_BASE_WORK_TICKS, data.readVarInt()));
        animationInward = data.readBoolean();
        curtainDeflection = net.minecraft.util.Mth.clamp(data.readFloat(), -1.0F, 1.0F);
        previousCurtainDeflection = curtainDeflection;
        curtainVelocity = net.minecraft.util.Mth.clamp(data.readFloat(), -1.0F, 1.0F);
        beltScrollPixels = net.minecraft.util.Mth.positiveModulo(
                data.readFloat(), BELT_SCROLL_PERIOD_PIXELS);
        renderedBox = data.readItem();
        return true;
    }

    @Override
    protected void writeToStream(FriendlyByteBuf data) {
        data.writeVarInt(animationTicks);
        data.writeVarInt(animationDurationTicks);
        data.writeBoolean(animationInward);
        data.writeFloat(curtainDeflection);
        data.writeFloat(curtainVelocity);
        data.writeFloat(beltScrollPixels);
        data.writeItem(renderedBox);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        for (ItemStack stack : upgrades) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        if (workingOperation == WorkingOperation.PACKING && !workingStack.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), workingStack);
        }
    }

    public enum MachineResult {
        PACKED("message.appliedpackaging.me_packager.packed"),
        UNPACKED("message.appliedpackaging.me_packager.unpacked"),
        NO_TARGET("message.appliedpackaging.me_packager.no_target"),
        NO_CONTENTS("message.appliedpackaging.me_packager.no_contents"),
        OUTPUT_BLOCKED("message.appliedpackaging.me_packager.output_blocked"),
        INPUT_BLOCKED("message.appliedpackaging.me_packager.input_blocked"),
        TARGET_BLOCKED("message.appliedpackaging.me_packager.target_blocked"),
        INVALID_INPUT("message.appliedpackaging.me_packager.invalid_input"),
        FILTER_REJECTED("message.appliedpackaging.me_packager.filter_rejected"),
        SOURCE_CHANGED("message.appliedpackaging.me_packager.source_changed"),
        WORKING("message.appliedpackaging.me_packager.working");

        private final String messageKey;

        MachineResult(String messageKey) {
            this.messageKey = messageKey;
        }

        public String messageKey() {
            return messageKey;
        }
    }

    public enum WorkingOperation {
        NONE,
        PACKING,
        UNPACKING;

        private static WorkingOperation byName(String name) {
            if (name == null || name.isBlank()) {
                return NONE;
            }
            try {
                return WorkingOperation.valueOf(name);
            } catch (IllegalArgumentException e) {
                return NONE;
            }
        }
    }

    public enum HeldBoxState {
        EMPTY,
        UNPACK_INPUT,
        PACK_OUTPUT;

        private static HeldBoxState byName(String name) {
            if (name == null || name.isBlank()) {
                return EMPTY;
            }
            try {
                return HeldBoxState.valueOf(name);
            } catch (IllegalArgumentException e) {
                return EMPTY;
            }
        }
    }

    public record ActionResult(boolean consumed, String messageKey) {
        public static ActionResult consumed(String messageKey) {
            return new ActionResult(true, messageKey);
        }
    }

    public int animationTicks() {
        return animationTicks;
    }

    public int animationDurationTicks() {
        return animationDurationTicks;
    }

    public boolean isWorking() {
        return workingOperation != WorkingOperation.NONE;
    }

    public WorkingOperation workingOperation() {
        return workingOperation;
    }

    public HeldBoxState heldBoxState() {
        return heldBoxState;
    }

    public boolean unpackBlocked() {
        return unpackBlocked;
    }

    public ItemStack insertPackageFromMenu(ItemStack stack, boolean simulate) {
        return insertPackageFromExternal(stack, simulate);
    }

    public boolean canInsertPackageFromMenu(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack remainder = insertPackageFromExternal(stack, true);
        return remainder.getCount() < stack.getCount();
    }

    public boolean animationInward() {
        return animationInward;
    }

    public float beltScrollPixels() {
        return beltScrollPixels;
    }

    public float getTransportAnimationProgress(float partialTicks) {
        if (animationTicks <= 0) {
            return 1.0F;
        }
        int animationWindowTicks = Math.min(ANIMATION_CYCLE_TICKS, animationDurationTicks);
        return net.minecraft.util.Mth.clamp(
                (animationWindowTicks - animationTicks + partialTicks) / animationWindowTicks,
                0.0F,
                1.0F);
    }

    public float getPackageTravelProgress(float partialTicks) {
        return getTransportAnimationProgress(partialTicks);
    }

    public float getCurtainDeflection(float partialTicks) {
        return net.minecraft.util.Mth.lerp(
                net.minecraft.util.Mth.clamp(partialTicks, 0.0F, 1.0F),
                previousCurtainDeflection,
                curtainDeflection);
    }

    public ItemStack getRenderedBox() {
        if (animationTicks <= 0) {
            return renderedBox;
        }
        float transportProgress = getTransportAnimationProgress(0.0F);
        if (animationInward) {
            return transportProgress >= 1.0F ? ItemStack.EMPTY : renderedBox;
        }
        return transportProgress <= 0.0F ? ItemStack.EMPTY : renderedBox;
    }

    public enum RedstoneMode {
        HIGH_SIGNAL,
        LOW_SIGNAL,
        ALWAYS,
        PULSE,
        NEVER;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public boolean isActive(boolean powered) {
            return switch (this) {
                case HIGH_SIGNAL -> powered;
                case LOW_SIGNAL -> !powered;
                case ALWAYS -> true;
                case PULSE, NEVER -> false;
            };
        }

        public static RedstoneMode[] uiValues() {
            return new RedstoneMode[] { HIGH_SIGNAL, LOW_SIGNAL, ALWAYS, PULSE, NEVER };
        }

        private static RedstoneMode byName(String name) {
            if (name == null || name.isBlank()) {
                return HIGH_SIGNAL;
            }
            try {
                return RedstoneMode.valueOf(name);
            } catch (IllegalArgumentException e) {
                return HIGH_SIGNAL;
            }
        }
    }

    public enum FilterApplicationMode {
        BOTH(true, true),
        PACK_ONLY(true, false),
        UNPACK_ONLY(false, true);

        private final boolean appliesToPack;
        private final boolean appliesToUnpack;

        FilterApplicationMode(boolean appliesToPack, boolean appliesToUnpack) {
            this.appliesToPack = appliesToPack;
            this.appliesToUnpack = appliesToUnpack;
        }

        public boolean appliesToPack() {
            return appliesToPack;
        }

        public boolean appliesToUnpack() {
            return appliesToUnpack;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        private static FilterApplicationMode byName(String name) {
            if (name == null || name.isBlank()) {
                return BOTH;
            }
            try {
                return FilterApplicationMode.valueOf(name);
            } catch (IllegalArgumentException e) {
                return BOTH;
            }
        }
    }

    public enum BlockingMode {
        IGNORE_NETWORK_CONTENTS,
        BLOCK_UNPACK_WHEN_NETWORK_HAS_ITEMS;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        private static BlockingMode byName(String name) {
            if (name == null || name.isBlank()) {
                return IGNORE_NETWORK_CONTENTS;
            }
            try {
                return BlockingMode.valueOf(name);
            } catch (IllegalArgumentException e) {
                return IGNORE_NETWORK_CONTENTS;
            }
        }
    }

    private static MarkerMergeMode markerModeByName(String name) {
        if (name == null || name.isBlank()) {
            return MarkerMergeMode.RETAIN;
        }
        try {
            return MarkerMergeMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return MarkerMergeMode.RETAIN;
        }
    }

    private void onInventorySlotChanged(int slot) {
        if (slot == SLOT_INPUT) {
            redstoneCooldown = 0;
            if (heldBox().isEmpty() && workingOperation != WorkingOperation.PACKING) {
                heldBoxState = HeldBoxState.EMPTY;
                unpackBlocked = false;
            } else if (!heldBox().isEmpty() && heldBoxState == HeldBoxState.EMPTY) {
                heldBoxState = HeldBoxState.UNPACK_INPUT;
            }
        }
        if (slot == SLOT_HELD_BOX && animationTicks == 0) {
            renderedBox = currentInventoryBox();
            syncVisualState();
        }
    }

    private void onFilterChanged() {
        setChanged();
    }

    private void onUpgradesChanged() {
        setChanged();
    }

    private List<GenericStack> contentFilterStacks() {
        List<GenericStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < contentFilter.size(); slot++) {
            GenericStack stack = contentFilter.getStack(slot);
            if (stack != null && stack.amount() > 0) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private int packingRetryIntervalTicks() {
        int speedCards = getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD);
        return packingWorkTicks(speedCards);
    }

    private int unpackingRetryIntervalTicks() {
        int speedCards = getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD);
        return unpackingWorkTicks(speedCards);
    }

    public static int packingWorkTicks(int speedCards) {
        return PACKING_WORK_TICKS_BY_SPEED[
                Math.min(Math.max(0, speedCards), PACKING_WORK_TICKS_BY_SPEED.length - 1)];
    }

    public static int unpackingWorkTicks(int speedCards) {
        return UNPACKING_WORK_TICKS_BY_SPEED[
                Math.min(Math.max(0, speedCards), UNPACKING_WORK_TICKS_BY_SPEED.length - 1)];
    }

    private static boolean targetHasContents(MEStorage target) {
        KeyCounter available = new KeyCounter();
        target.getAvailableStacks(available);
        available.removeZeros();
        return !available.isEmpty();
    }

    private void tickAnimation() {
        if (animationTicks <= 0) {
            tickCurtainAnimation(0.0F, 0.0F);
            if (isWorking() && level != null && !level.isClientSide) {
                finishWorkingOperation();
                return;
            }
            if (!isWorking()) {
                ItemStack currentBox = currentInventoryBox();
                if (!currentBox.isEmpty() && !ItemStack.matches(renderedBox, currentBox)) {
                    renderedBox = currentBox;
                }
            }
            return;
        }
        float travelBefore = getPackageTravelProgress(0.0F);
        animationTicks--;
        float travelAfter = getPackageTravelProgress(0.0F);
        float direction = animationInward ? 1.0F : -1.0F;
        beltScrollPixels = net.minecraft.util.Mth.positiveModulo(
                beltScrollPixels + direction * (travelAfter - travelBefore) * BELT_PACKAGE_TRAVEL_PIXELS,
                BELT_SCROLL_PERIOD_PIXELS);
        tickCurtainAnimation(travelBefore, travelAfter);
        if (animationTicks > 0) {
            return;
        }
        if (level != null && level.isClientSide) {
            if (animationInward) {
                renderedBox = ItemStack.EMPTY;
            }
            return;
        }
        finishWorkingOperation();
    }

    private void tickCurtainAnimation(float travelBefore, float travelAfter) {
        previousCurtainDeflection = curtainDeflection;
        float packageMovement = Math.abs(travelAfter - travelBefore);
        float targetDeflection = 0.0F;
        if (packageMovement > 0.0F) {
            float passage = Math.max(0.0F, net.minecraft.util.Mth.sin((float) Math.PI * travelAfter));
            float speedWeight = net.minecraft.util.Mth.clamp(
                    CURTAIN_MIN_DRIVE + packageMovement * CURTAIN_SPEED_WEIGHT,
                    0.0F,
                    1.0F);
            targetDeflection = passage * speedWeight * (animationInward ? -1.0F : 1.0F);
        }

        curtainVelocity = (curtainVelocity
                        + (targetDeflection - curtainDeflection) * CURTAIN_SPRING_STRENGTH)
                * CURTAIN_DAMPING;
        curtainDeflection = net.minecraft.util.Mth.clamp(
                curtainDeflection + curtainVelocity,
                -1.0F,
                1.0F);
        if (packageMovement == 0.0F
                && Math.abs(curtainDeflection) < CURTAIN_REST_EPSILON
                && Math.abs(curtainVelocity) < CURTAIN_REST_EPSILON) {
            curtainDeflection = 0.0F;
            curtainVelocity = 0.0F;
        }
    }

    private void finishWorkingOperation() {
        if (workingOperation == WorkingOperation.PACKING && !workingStack.isEmpty()) {
            if (!heldBox().isEmpty()) {
                animationTicks = 0;
                return;
            }
            heldBoxState = HeldBoxState.PACK_OUTPUT;
            ItemStack remainder = items.insertItem(SLOT_INPUT, workingStack.copy(), false);
            if (!remainder.isEmpty()) {
                heldBoxState = HeldBoxState.EMPTY;
                animationTicks = 0;
                return;
            }
        } else if (workingOperation == WorkingOperation.UNPACKING) {
            ItemStack input = heldBox();
            if (!input.isEmpty()) {
                Optional<MEStorage> target = findTargetMEStorage();
                MachineResult validation = target
                        .map(storage -> validateUnpackInput(storage, input, true))
                        .orElse(MachineResult.NO_TARGET);
                if (validation != MachineResult.UNPACKED
                        || !MEStoragePackageTransactions.insertPackageContents(
                                PackageDataStorage.read(input).orElseThrow(),
                                target.orElseThrow())) {
                    workingOperation = WorkingOperation.NONE;
                    workingStack = ItemStack.EMPTY;
                    animationTicks = 0;
                    unpackBlocked = true;
                    heldBoxState = HeldBoxState.UNPACK_INPUT;
                    renderedBox = displayStack(input);
                    syncVisualState();
                    return;
                }
                items.extractItem(SLOT_INPUT, 1, false);
            }
            heldBoxState = HeldBoxState.EMPTY;
            unpackBlocked = false;
            renderedBox = ItemStack.EMPTY;
        }
        workingOperation = WorkingOperation.NONE;
        workingStack = ItemStack.EMPTY;
        renderedBox = currentInventoryBox();
        syncVisualState();
    }

    private void startWorkingOperation(WorkingOperation operation, ItemStack stack) {
        workingOperation = operation;
        workingStack = displayStack(stack);
        renderedBox = displayStack(stack);
        animationInward = operation == WorkingOperation.UNPACKING;
        int speedCards = getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD);
        animationDurationTicks = operation == WorkingOperation.PACKING
                ? packingWorkTicks(speedCards)
                : unpackingWorkTicks(speedCards);
        animationTicks = animationDurationTicks;
        syncVisualState();
    }

    private void syncVisualState() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static ItemStack displayStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private ItemStack currentInventoryBox() {
        return displayStack(heldBox());
    }
}

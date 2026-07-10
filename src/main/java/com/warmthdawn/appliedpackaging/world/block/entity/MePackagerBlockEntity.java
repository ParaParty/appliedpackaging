package com.warmthdawn.appliedpackaging.world.block.entity;

import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageService;
import appeng.api.orientation.BlockOrientation;
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
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.world.menu.MePackagerMenu;
import com.warmthdawn.appliedpackaging.world.block.InventoryDroppingBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.MePackagerBlock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class MePackagerBlockEntity extends AENetworkBlockEntity
        implements InventoryDroppingBlockEntity, MenuProvider, IUpgradeableObject {
    public static final PackageCapacityProfile BASE_CAPACITY_PROFILE = PackageCapacityProfile.STORAGE_1K;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_CAPACITY = 2;
    public static final int SLOT_FILTER = 3;
    public static final int SLOT_MARKER = 4;
    public static final int FILTER_COLUMNS = 9;
    public static final int FILTER_ROWS = 5;
    public static final int BASE_FILTER_ROWS = 2;
    public static final int MAX_FILTER_CAPACITY_CARDS = 3;
    public static final int FILTER_SLOT_COUNT = FILTER_COLUMNS * FILTER_ROWS;
    public static final int UPGRADE_SLOT_COUNT = 6;
    public static final int CYCLIC_REDSTONE_INTERVAL_TICKS = 20;
    public static final int ANIMATION_CYCLE_TICKS = 20;
    private static final int SLOT_COUNT = 5;
    private static final String ITEMS_TAG = "items";
    private static final String FILTER_TAG = "content_filter";
    private static final String UPGRADES_TAG = "upgrades";
    private static final String POWERED_TAG = "powered";
    private static final String SELECTED_COLOR_TAG = "selected_color";
    private static final String MARKER_MODE_TAG = "marker_mode";
    private static final String REDSTONE_MODE_TAG = "redstone_mode";
    private static final String FILTER_APPLICATION_MODE_TAG = "filter_application_mode";
    private static final String BLOCKING_MODE_TAG = "blocking_mode";
    private static final String PACKAGE_NAME_TAG = "package_name";
    private static final String ANIMATION_TICKS_TAG = "animation_ticks";
    private static final String ANIMATION_INWARD_TAG = "animation_inward";
    private static final String RENDERED_BOX_TAG = "rendered_box";
    private static final String WORKING_OPERATION_TAG = "working_operation";
    private static final String WORKING_STACK_TAG = "working_stack";
    private static final String PENDING_PACK_TRIGGER_TAG = "pending_pack_trigger";

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_INPUT) {
                return PackageDataStorage.read(stack).isPresent();
            }
            if (slot == SLOT_OUTPUT) {
                return stack.getItem() instanceof PackageItem;
            }
            if (slot == SLOT_CAPACITY) {
                return componentCapacityProfileFromItem(stack).isPresent();
            }
            if (slot == SLOT_FILTER) {
                return PackageFilter.fromTemplate(stack).isPresent();
            }
            if (slot == SLOT_MARKER) {
                return isMarkerItem(stack);
            }
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == SLOT_CAPACITY || slot == SLOT_FILTER || slot == SLOT_MARKER) {
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
    private final ConfigInventory contentFilter = ConfigInventory.configStacks(
            null,
            FILTER_SLOT_COUNT,
            this::onFilterChanged,
            true);
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            APBlocks.ME_PACKAGER.get(),
            UPGRADE_SLOT_COUNT,
            this::onUpgradesChanged);
    private final IItemHandler externalItems = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return switch (slot) {
                case 0 -> items.getStackInSlot(SLOT_INPUT);
                case 1 -> items.getStackInSlot(SLOT_OUTPUT);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0) {
                return stack;
            }
            return insertPackageFromExternal(stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 1) {
                return ItemStack.EMPTY;
            }
            return items.extractItem(SLOT_OUTPUT, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && items.isItemValid(SLOT_INPUT, stack);
        }
    };
    private LazyOptional<IItemHandler> internalItemHandler = createInternalItemHandlerCapability();
    private LazyOptional<IItemHandler> externalItemHandler = createExternalItemHandlerCapability();
    private boolean powered;
    private int redstoneCooldown;
    private int animationTicks;
    private boolean animationInward = true;
    private ItemStack renderedBox = ItemStack.EMPTY;
    private ItemStack workingStack = ItemStack.EMPTY;
    private WorkingOperation workingOperation = WorkingOperation.NONE;
    private PackageColor selectedColor = PackageColor.FLUIX;
    private MarkerMergeMode markerMode = MarkerMergeMode.RETAIN;
    private RedstoneMode redstoneMode = RedstoneMode.HIGH_SIGNAL;
    private FilterApplicationMode filterApplicationMode = FilterApplicationMode.BOTH;
    private BlockingMode blockingMode = BlockingMode.IGNORE_NETWORK_CONTENTS;
    private String packageName = "";
    private boolean pendingPackTrigger;

    public MePackagerBlockEntity(BlockPos pos, BlockState blockState) {
        super(APBlockEntities.ME_PACKAGER.get(), pos, blockState);
        getMainNode().setIdlePowerUsage(1.0D);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public ConfigInventory getContentFilter() {
        return contentFilter;
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
        this.redstoneMode = normalizeRedstoneMode(redstoneMode == null ? RedstoneMode.HIGH_SIGNAL : redstoneMode);
        this.redstoneCooldown = 0;
        setChanged();
    }

    public void cycleRedstoneMode() {
        RedstoneMode[] values = RedstoneMode.uiValues();
        RedstoneMode current = normalizeRedstoneMode(redstoneMode);
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
        return getUpgrades().isInstalled(AEItems.REDSTONE_CARD)
                ? normalizeRedstoneMode(redstoneMode)
                : RedstoneMode.HIGH_SIGNAL;
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

    public String packageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        String value = packageName == null ? "" : packageName.strip();
        if (value.length() > 50) {
            value = value.substring(0, 50);
        }
        if (!this.packageName.equals(value)) {
            this.packageName = value;
            setChanged();
        }
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

        ItemStack output = items.extractItem(SLOT_OUTPUT, 64, false);
        if (!output.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(player, output);
            syncVisualState();
            return ActionResult.consumed("message.appliedpackaging.me_packager.output_taken");
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
                && items.getStackInSlot(SLOT_INPUT).isEmpty()) {
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
        if (!items.getStackInSlot(SLOT_INPUT).isEmpty()) {
            tickAutomaticUnpack();
            return;
        }
        tickAutomaticPack();
    }

    private void tickAutomaticUnpack() {
        if (redstoneCooldown > 0) {
            redstoneCooldown--;
            return;
        }
        runUnpackOnce();
        redstoneCooldown = redstoneIntervalTicks();
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
        runPackOnce();
        redstoneCooldown = redstoneIntervalTicks();
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

        ItemStack input = items.getStackInSlot(SLOT_INPUT);
        if (!input.isEmpty()) {
            return unpackOne(meStorage.get(), input);
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
        ItemStack input = items.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) {
            return MachineResult.NO_CONTENTS;
        }
        return unpackOne(meStorage.get(), input);
    }

    private MachineResult packOne(MEStorage source) {
        if (!items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
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
        if (!packageName.isBlank()) {
            packageStack.setHoverName(Component.literal(packageName));
        }
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, packageStack, true);
        if (!remainder.isEmpty()) {
            return MachineResult.OUTPUT_BLOCKED;
        }

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
        PackageData data = PackageDataStorage.read(input).orElseThrow();
        if (!MEStoragePackageTransactions.insertPackageContents(data, target)) {
            return MachineResult.TARGET_BLOCKED;
        }
        ItemStack renderedInput = input.copy();
        renderedInput.setCount(1);
        items.extractItem(SLOT_INPUT, 1, false);
        startWorkingOperation(WorkingOperation.UNPACKING, renderedInput);
        setChanged();
        return MachineResult.UNPACKED;
    }

    private ItemStack insertPackageFromExternal(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (isWorking()
                || !items.getStackInSlot(SLOT_INPUT).isEmpty()
                || !items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            return stack;
        }
        Optional<MEStorage> meStorage = findTargetMEStorage();
        if (meStorage.isEmpty()) {
            return stack;
        }

        ItemStack one = stack.copy();
        one.setCount(1);
        MachineResult validation = validateUnpackInput(meStorage.get(), one);
        if (validation != MachineResult.UNPACKED) {
            return stack;
        }

        ItemStack remainder = stack.copy();
        remainder.shrink(1);
        if (!simulate) {
            PackageData data = PackageDataStorage.read(one).orElseThrow();
            if (!MEStoragePackageTransactions.insertPackageContents(data, meStorage.get())) {
                return stack;
            }
            startWorkingOperation(WorkingOperation.UNPACKING, one);
            setChanged();
        }
        return remainder.isEmpty() ? ItemStack.EMPTY : remainder;
    }

    private MachineResult validateUnpackInput(MEStorage target, ItemStack input) {
        if (isWorking()) {
            return MachineResult.WORKING;
        }
        if (!items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
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
        ItemStack markerStack = items.getStackInSlot(SLOT_MARKER);
        if (!isMarkerItem(markerStack)) {
            return Optional.empty();
        }
        ItemStack keyStack = markerStack.copy();
        keyStack.setCount(1);
        return Optional.of(new MarkerSpec(new GenericStack(AEItemKey.of(keyStack), 1)));
    }

    private boolean contentFilterInverted() {
        return getUpgrades().isInstalled(AEItems.INVERTER_CARD);
    }

    private PackageCapacityProfile configuredCapacityProfile() {
        return componentCapacityProfileFromItem(items.getStackInSlot(SLOT_CAPACITY))
                .orElse(BASE_CAPACITY_PROFILE);
    }

    private PackageFilter configuredFilter() {
        PackageFilter legacyFilter = PackageFilter.fromTemplate(items.getStackInSlot(SLOT_FILTER))
                .orElse(PackageFilter.any());
        List<GenericStack> requiredContents = new ArrayList<>(legacyFilter.requiredContents());
        requiredContents.addAll(contentFilterStacks());
        if (requiredContents.isEmpty() && legacyFilter.color().isEmpty() && legacyFilter.marker().isEmpty()) {
            return PackageFilter.any();
        }
        return new PackageFilter(legacyFilter.color(), legacyFilter.marker(), requiredContents);
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

    public static Optional<PackageCapacityProfile> capacityProfileFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"ae2".equals(id.getNamespace())) {
            return Optional.empty();
        }
        return switch (id.getPath()) {
            case "cell_component_16k",
                    "item_storage_cell_16k",
                    "fluid_storage_cell_16k",
                    "portable_item_cell_16k",
                    "portable_fluid_cell_16k",
                    "storage_cell_16k" -> Optional.of(PackageCapacityProfile.STORAGE_16K);
            case "cell_component_64k",
                    "item_storage_cell_64k",
                    "fluid_storage_cell_64k",
                    "portable_item_cell_64k",
                    "portable_fluid_cell_64k",
                    "storage_cell_64k" -> Optional.of(PackageCapacityProfile.STORAGE_64K);
            case "cell_component_256k",
                    "item_storage_cell_256k",
                    "fluid_storage_cell_256k",
                    "portable_item_cell_256k",
                    "portable_fluid_cell_256k",
                    "storage_cell_256k" -> Optional.of(PackageCapacityProfile.STORAGE_256K);
            default -> Optional.empty();
        };
    }

    public static Optional<PackageCapacityProfile> componentCapacityProfileFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"ae2".equals(id.getNamespace())) {
            return Optional.empty();
        }
        return switch (id.getPath()) {
            case "cell_component_16k" -> Optional.of(PackageCapacityProfile.STORAGE_16K);
            case "cell_component_64k" -> Optional.of(PackageCapacityProfile.STORAGE_64K);
            case "cell_component_256k" -> Optional.of(PackageCapacityProfile.STORAGE_256K);
            default -> Optional.empty();
        };
    }

    public void clearConfiguration() {
        contentFilter.clear();
        items.setStackInSlot(SLOT_FILTER, ItemStack.EMPTY);
        items.setStackInSlot(SLOT_MARKER, ItemStack.EMPTY);
        setPackageName("");
        setSelectedColor(PackageColor.FLUIX);
        setFilterApplicationMode(FilterApplicationMode.BOTH);
        setBlockingMode(BlockingMode.IGNORE_NETWORK_CONTENTS);
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
        return EnumSet.of(networkSide());
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return dir == networkSide() ? AECableType.SMART : AECableType.NONE;
    }

    public void onNetworkSideChanged() {
        onGridConnectableSidesChanged();
        setChanged();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) {
                return internalItemHandler.cast();
            }
            if (side == networkSide()) {
                return LazyOptional.empty();
            }
            return externalItemHandler.cast();
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
        return LazyOptional.of(() -> items);
    }

    private LazyOptional<IItemHandler> createExternalItemHandlerCapability() {
        return LazyOptional.of(() -> externalItems);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ITEMS_TAG, items.serializeNBT());
        contentFilter.writeToChildTag(tag, FILTER_TAG);
        upgrades.writeToNBT(tag, UPGRADES_TAG);
        tag.putBoolean(POWERED_TAG, powered);
        tag.putString(SELECTED_COLOR_TAG, selectedColor.id());
        tag.putString(MARKER_MODE_TAG, markerMode.name());
        tag.putString(REDSTONE_MODE_TAG, redstoneMode.name());
        tag.putString(FILTER_APPLICATION_MODE_TAG, filterApplicationMode.name());
        tag.putString(BLOCKING_MODE_TAG, blockingMode.name());
        if (!packageName.isBlank()) {
            tag.putString(PACKAGE_NAME_TAG, packageName);
        }
        tag.putInt(ANIMATION_TICKS_TAG, animationTicks);
        tag.putBoolean(ANIMATION_INWARD_TAG, animationInward);
        if (!renderedBox.isEmpty()) {
            tag.put(RENDERED_BOX_TAG, renderedBox.save(new CompoundTag()));
        }
        tag.putString(WORKING_OPERATION_TAG, workingOperation.name());
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
        upgrades.readFromNBT(tag, UPGRADES_TAG);
        powered = tag.getBoolean(POWERED_TAG);
        selectedColor = PackageColor.byId(tag.getString(SELECTED_COLOR_TAG)).orElse(PackageColor.FLUIX);
        markerMode = markerModeByName(tag.getString(MARKER_MODE_TAG));
        redstoneMode = RedstoneMode.byName(tag.getString(REDSTONE_MODE_TAG));
        filterApplicationMode = FilterApplicationMode.byName(tag.getString(FILTER_APPLICATION_MODE_TAG));
        blockingMode = BlockingMode.byName(tag.getString(BLOCKING_MODE_TAG));
        packageName = tag.getString(PACKAGE_NAME_TAG);
        animationTicks = tag.getInt(ANIMATION_TICKS_TAG);
        animationInward = !tag.contains(ANIMATION_INWARD_TAG) || tag.getBoolean(ANIMATION_INWARD_TAG);
        renderedBox = tag.contains(RENDERED_BOX_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound(RENDERED_BOX_TAG))
                : currentInventoryBox();
        workingOperation = WorkingOperation.byName(tag.getString(WORKING_OPERATION_TAG));
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
        animationInward = data.readBoolean();
        renderedBox = data.readItem();
        return true;
    }

    @Override
    protected void writeToStream(FriendlyByteBuf data) {
        data.writeVarInt(animationTicks);
        data.writeBoolean(animationInward);
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

    public record ActionResult(boolean consumed, String messageKey) {
        public static ActionResult consumed(String messageKey) {
            return new ActionResult(true, messageKey);
        }
    }

    public Direction networkSide() {
        return MePackagerBlock.networkSide(getBlockState());
    }

    public int animationTicks() {
        return animationTicks;
    }

    public boolean isWorking() {
        return workingOperation != WorkingOperation.NONE;
    }

    public WorkingOperation workingOperation() {
        return workingOperation;
    }

    public ItemStack insertPackageFromMenu(ItemStack stack, boolean simulate) {
        return insertPackageFromExternal(stack, simulate);
    }

    public boolean animationInward() {
        return animationInward;
    }

    public float getTrayOffset(float partialTicks) {
        float tickCycle = animationInward ? animationTicks - partialTicks : animationTicks - 5 - partialTicks;
        float progress = net.minecraft.util.Mth.clamp(tickCycle / (ANIMATION_CYCLE_TICKS - 5.0F) * 2.0F - 1.0F, -1.0F, 1.0F);
        progress = 1.0F - progress * progress;
        return progress * progress;
    }

    public ItemStack getRenderedBox() {
        if (animationTicks <= 0) {
            return idleRenderedBox();
        }
        if (animationInward) {
            return animationTicks <= ANIMATION_CYCLE_TICKS / 2 ? ItemStack.EMPTY : renderedBox;
        }
        return animationTicks >= ANIMATION_CYCLE_TICKS / 2 ? ItemStack.EMPTY : renderedBox;
    }

    public boolean isHatchOpen() {
        return animationTicks > (animationInward ? 1 : 5)
                && animationTicks < ANIMATION_CYCLE_TICKS - (animationInward ? 5 : 1);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        Direction previousSide = networkSide();
        super.setBlockState(state);
        if (previousSide != networkSide()) {
            onNetworkSideChanged();
        }
    }

    public enum RedstoneMode {
        HIGH_SIGNAL,
        LOW_SIGNAL,
        ALWAYS,
        PULSE,
        NEVER,
        DISABLED,
        CYCLIC;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public boolean isActive(boolean powered) {
            return switch (normalizeRedstoneMode(this)) {
                case HIGH_SIGNAL -> powered;
                case LOW_SIGNAL -> !powered;
                case ALWAYS -> true;
                case PULSE, NEVER -> false;
                case DISABLED, CYCLIC -> false;
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
                return normalizeRedstoneMode(RedstoneMode.valueOf(name));
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

    private static boolean isMarkerItem(ItemStack stack) {
        return !stack.isEmpty()
                && !(stack.getItem() instanceof PackageItem)
                && !PackagePatternDataStorage.canStore(stack);
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
        }
        if ((slot == SLOT_INPUT || slot == SLOT_OUTPUT) && animationTicks == 0) {
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

    private int redstoneIntervalTicks() {
        int speedCards = getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD);
        return Math.max(2, CYCLIC_REDSTONE_INTERVAL_TICKS - speedCards * 3);
    }

    private static RedstoneMode normalizeRedstoneMode(RedstoneMode mode) {
        if (mode == RedstoneMode.DISABLED) {
            return RedstoneMode.NEVER;
        }
        if (mode == RedstoneMode.CYCLIC) {
            return RedstoneMode.HIGH_SIGNAL;
        }
        return mode == null ? RedstoneMode.HIGH_SIGNAL : mode;
    }

    private static boolean targetHasContents(MEStorage target) {
        KeyCounter available = new KeyCounter();
        target.getAvailableStacks(available);
        available.removeZeros();
        return !available.isEmpty();
    }

    private void tickAnimation() {
        if (animationTicks <= 0) {
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
        animationTicks--;
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

    private void finishWorkingOperation() {
        if (workingOperation == WorkingOperation.PACKING && !workingStack.isEmpty()) {
            if (!items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
                animationTicks = 1;
                return;
            }
            ItemStack remainder = items.insertItem(SLOT_OUTPUT, workingStack.copy(), false);
            if (!remainder.isEmpty()) {
                animationTicks = 1;
                return;
            }
        } else if (workingOperation == WorkingOperation.UNPACKING) {
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
        animationTicks = ANIMATION_CYCLE_TICKS;
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

    private ItemStack idleRenderedBox() {
        ItemStack currentBox = currentInventoryBox();
        return currentBox.isEmpty() ? renderedBox : currentBox;
    }

    private ItemStack currentInventoryBox() {
        ItemStack input = displayStack(items.getStackInSlot(SLOT_INPUT));
        if (!input.isEmpty()) {
            return input;
        }
        return displayStack(items.getStackInSlot(SLOT_OUTPUT));
    }
}

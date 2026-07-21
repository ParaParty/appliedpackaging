package com.warmthdawn.appliedpackaging.world.block.entity;

import appeng.api.crafting.IPatternDetails;
import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.capabilities.Capabilities;
import appeng.core.definitions.AEItems;
import appeng.util.ConfigInventory;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageLayout;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanBuilder;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanResult;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.world.block.AbstractHorizontalMachineBlock;
import com.warmthdawn.appliedpackaging.world.block.InventoryDroppingBlockEntity;
import com.warmthdawn.appliedpackaging.world.menu.PackageAssemblerMenu;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.TickPriority;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PackageAssemblerBlockEntity extends AENetworkBlockEntity
        implements InventoryDroppingBlockEntity, MenuProvider, ICraftingMachine, IUpgradeableObject {
    public static final int OUTPUT_SLOT_COUNT = 17;
    public static final int MENU_INPUT_COLUMNS = 4;
    public static final int MAX_MENU_INPUT_SLOT_COUNT = AdvancedProcessingPatternDataStorage.MAX_INPUT_SLOTS;
    public static final int SLOT_PATTERN = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_CAPACITY = 2;
    public static final int EXTRA_OUTPUT_SLOT_START = 3;
    public static final int UPGRADE_SLOT_COUNT = 5;
    public static final int MAX_CRAFT_PROGRESS = 100;
    private static final int COMPARATOR_WORKING_SIGNAL = 1;
    private static final int COMPARATOR_COMPLETED_SIGNAL = 2;
    private static final int SLOT_COUNT = EXTRA_OUTPUT_SLOT_START + OUTPUT_SLOT_COUNT - 1;
    private static final String ITEMS_TAG = "items";
    private static final String UPGRADES_TAG = "upgrades";
    private static final String MENU_INPUTS_TAG = "menu_inputs";
    private static final String MENU_INPUT_SLOT_TAG = "slot";
    private static final String MENU_INPUT_STACK_TAG = "stack";
    private static final String MENU_INPUT_AMOUNT_TAG = "amount";
    private static final String PENDING_PACKAGES_TAG = "pending_packages";
    private static final String PENDING_COLOR_TAG = "color";
    private static final String PENDING_DATA_TAG = "data";
    private static final String OUTPUT_MODE_TAG = "output_mode";
    private static final String BLOCKING_MODE_TAG = "blocking_mode";
    private static final String AUTO_EXPORT_BATCH_ACTIVE_TAG = "auto_export_batch_active";
    private static final String AUTO_EXPORT_BATCH_MODE_TAG = "auto_export_batch_mode";
    private static final String AUTO_EXPORT_BATCH_DIRECTION_TAG = "auto_export_batch_direction";
    private static final String CRAFT_PROGRESS_TAG = "craft_progress";
    private static final String ACTIVE_PACKAGES_TAG = "active_packages";
    private static final String ACTIVE_MENU_PATTERN_TAG = "active_menu_pattern";
    private static final String SELECTED_COLOR_TAG = "selected_color";
    private static final String MARKER_FILTER_TAG = "marker_filter";

    private MenuInputEntry[] menuInputs = new MenuInputEntry[0];
    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (isOutputSlot(slot)) {
                return stack.getItem() instanceof PackageItem && PackageDataStorage.read(stack).isPresent();
            }
            if (slot == SLOT_PATTERN) {
                return isPatternSlotItem(stack);
            }
            if (slot == SLOT_CAPACITY) {
                return PackageCapacityProfile.fromStorageComponent(stack).isPresent();
            }
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (isOutputSlot(slot) || slot == SLOT_CAPACITY || slot == SLOT_PATTERN) {
                return 1;
            }
            return super.getSlotLimit(slot);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == SLOT_PATTERN && activePackages.isEmpty()) {
                craftingProgress = 0;
            }
            if (slot == SLOT_PATTERN) {
                refreshGenericInventoryCapability();
            }
            if (isOutputSlot(slot)) {
                notifyComparatorOutputChanged();
            }
            setChanged();
        }
    };
    private final IItemHandlerModifiable orderedOutputItems = new IItemHandlerModifiable() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? items.getStackInSlot(SLOT_OUTPUT) : ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot != 0) {
                throw new IndexOutOfBoundsException(slot);
            }
            items.setStackInSlot(SLOT_OUTPUT, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? extractPrimaryOutput(amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 1 : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            APBlocks.PACKAGE_ASSEMBLER.get(),
            UPGRADE_SLOT_COUNT,
            this::onUpgradesChanged);
    private final ConfigInventory markerFilter = ConfigInventory.configTypes(
            key -> key instanceof AEItemKey itemKey && !(itemKey.getItem() instanceof PackageItem),
            1,
            this::setChanged);
    private ExternalGenericInventory externalGenericInventory;
    private final MEStorage externalStorage = new ExternalStorage();
    private LazyOptional<GenericInternalInventory> genericInventory = LazyOptional.empty();
    private LazyOptional<MEStorage> storage = createStorageCapability();
    private LazyOptional<ICraftingMachine> craftingMachine = createCraftingMachineCapability();
    private final List<QueuedPackage> activePackages = new ArrayList<>();
    private final List<QueuedPackage> pendingPackages = new ArrayList<>();
    private ItemStack activeMenuPattern = ItemStack.EMPTY;
    private ItemStack cachedMenuFilterPattern = ItemStack.EMPTY;
    private Level cachedMenuFilterLevel;
    private List<GenericStack> cachedMenuInputFilters = List.of();
    private List<AdvancedMenuInputFilter> cachedAdvancedMenuInputFilters = List.of();
    private ItemStack cachedCapacityPattern = ItemStack.EMPTY;
    private ItemStack cachedCapacityComponent = ItemStack.EMPTY;
    private boolean cachedPatternCapacityValid = true;
    private OutputMode outputMode = OutputMode.ME_NETWORK;
    private boolean blockingMode;
    private boolean autoExportBatchActive;
    private OutputMode autoExportBatchMode = OutputMode.NONE;
    private Direction autoExportBatchDirection;
    private int craftingProgress;
    private PackageColor selectedColor = PackageColor.FLUIX;

    public PackageAssemblerBlockEntity(BlockPos pos, BlockState blockState) {
        super(APBlockEntities.PACKAGE_ASSEMBLER.get(), pos, blockState);
        getMainNode().setIdlePowerUsage(0.0);
        refreshGenericInventoryCapability();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PackageAssemblerBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        finishAutoExportBatchIfEmpty();
        int completedBeforeExport = completedOutputCount();
        boolean exported = exportOutputIfEnabled();
        boolean completedOutputCleared = exported
                && completedBeforeExport > 0
                && completedOutputCount() == 0;
        if (completedOutputCleared) {
            scheduleComparatorSamplingNextTick();
        }
        if (!pendingPackages.isEmpty()) {
            outputPendingPackage();
            return;
        }
        if (completedOutputCount() > 0 || completedOutputCleared) {
            return;
        }
        if (!activePackages.isEmpty()) {
            advanceCraftingProgress();
            return;
        }
        tryStartAssembly();
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public IItemHandlerModifiable getOrderedOutputItems() {
        return orderedOutputItems;
    }

    public ItemStack nextOutputPreview() {
        return pendingPackages.isEmpty()
                ? ItemStack.EMPTY
                : packageStack(pendingPackages.get(0).color(), pendingPackages.get(0).data());
    }

    public int queuedOutputCount() {
        return pendingPackages.size();
    }

    public ConfigInventory getMarkerFilter() {
        return markerFilter;
    }

    public PackageColor selectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(PackageColor selectedColor) {
        PackageColor value = selectedColor == null ? PackageColor.FLUIX : selectedColor;
        if (this.selectedColor != value) {
            this.selectedColor = value;
            setChanged();
        }
    }

    public boolean isSelectedColorConfigurable() {
        if (isCrafting() || !activePackages.isEmpty()) {
            return false;
        }
        ItemStack patternStack = items.getStackInSlot(SLOT_PATTERN);
        return PackageCraftingPatternDataStorage.read(patternStack).isEmpty()
                && AdvancedProcessingPatternDataStorage.read(patternStack).isEmpty();
    }

    public Optional<PackageColor> effectiveColor() {
        if (!activePackages.isEmpty()) {
            return singleQueuedColor(activePackages);
        }

        ItemStack patternStack = items.getStackInSlot(SLOT_PATTERN);
        Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> packagePattern =
                PackageCraftingPatternDataStorage.read(patternStack);
        if (packagePattern.isPresent()) {
            return Optional.of(packagePattern.orElseThrow().color());
        }

        Optional<AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern> advancedPattern =
                AdvancedProcessingPatternDataStorage.read(patternStack);
        if (advancedPattern.isPresent()) {
            PackageColor color = null;
            for (var column : advancedPattern.orElseThrow().columns()) {
                if (color == null) {
                    color = column.color();
                } else if (color != column.color()) {
                    return Optional.empty();
                }
            }
            return color == null ? Optional.of(selectedColor) : Optional.of(color);
        }
        return Optional.of(selectedColor);
    }

    public Optional<MarkerSpec> effectiveMarker() {
        if (!activePackages.isEmpty()) {
            return singleQueuedMarker(activePackages);
        }

        ItemStack patternStack = items.getStackInSlot(SLOT_PATTERN);
        Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> packagePattern =
                PackageCraftingPatternDataStorage.read(patternStack);
        if (packagePattern.isPresent()) {
            return packagePattern.orElseThrow().data().marker();
        }

        Optional<AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern> advancedPattern =
                AdvancedProcessingPatternDataStorage.read(patternStack);
        if (advancedPattern.isPresent()) {
            return singleAdvancedMarker(advancedPattern.orElseThrow());
        }

        if (PatternDetailsHelper.isEncodedPattern(patternStack)) {
            return ordinaryPatternMarkerWithOverride(patternStack);
        }
        return configuredMarkerItem();
    }

    private static Optional<PackageColor> singleQueuedColor(List<QueuedPackage> packages) {
        PackageColor color = packages.get(0).color();
        for (int index = 1; index < packages.size(); index++) {
            if (packages.get(index).color() != color) {
                return Optional.empty();
            }
        }
        return Optional.of(color);
    }

    private static Optional<MarkerSpec> singleQueuedMarker(List<QueuedPackage> packages) {
        Optional<MarkerSpec> marker = packages.get(0).data().marker();
        for (int index = 1; index < packages.size(); index++) {
            if (!sameMarker(marker, packages.get(index).data().marker())) {
                return Optional.empty();
            }
        }
        return marker;
    }

    private static Optional<MarkerSpec> singleAdvancedMarker(
            AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern pattern) {
        Optional<MarkerSpec> marker = null;
        for (var column : pattern.columns()) {
            if (marker == null) {
                marker = column.marker();
            } else if (!sameMarker(marker, column.marker())) {
                return Optional.empty();
            }
        }
        return marker == null ? Optional.empty() : marker;
    }

    private static boolean sameMarker(Optional<MarkerSpec> left, Optional<MarkerSpec> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return left.isEmpty() && right.isEmpty();
        }
        return left.orElseThrow().sameAs(right.orElseThrow());
    }

    public int completedOutputCount() {
        long count = pendingPackages.size();
        for (int outputIndex = 0; outputIndex < OUTPUT_SLOT_COUNT; outputIndex++) {
            count += items.getStackInSlot(outputHandlerSlot(outputIndex)).getCount();
        }
        return count >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    public int comparatorOutputSignal() {
        if (completedOutputCount() > 0) {
            return COMPARATOR_COMPLETED_SIGNAL;
        }
        return activePackages.isEmpty() ? 0 : COMPARATOR_WORKING_SIGNAL;
    }

    private void notifyComparatorOutputChanged() {
        if (level != null && !level.isClientSide) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    private void scheduleComparatorSamplingNextTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacentPos = worldPosition.relative(direction);
            if (!serverLevel.hasChunkAt(adjacentPos)) {
                continue;
            }
            BlockState adjacentState = serverLevel.getBlockState(adjacentPos);
            if (scheduleComparatorSample(serverLevel, adjacentPos, adjacentState, direction)) {
                continue;
            }
            if (!adjacentState.isRedstoneConductor(serverLevel, adjacentPos)) {
                continue;
            }
            BlockPos comparatorPos = adjacentPos.relative(direction);
            if (serverLevel.hasChunkAt(comparatorPos)) {
                scheduleComparatorSample(
                        serverLevel,
                        comparatorPos,
                        serverLevel.getBlockState(comparatorPos),
                        direction);
            }
        }
    }

    private static boolean scheduleComparatorSample(
            ServerLevel level,
            BlockPos comparatorPos,
            BlockState comparatorState,
            Direction sourceDirection) {
        if (!comparatorState.is(Blocks.COMPARATOR)
                || comparatorState.getValue(HorizontalDirectionalBlock.FACING) != sourceDirection.getOpposite()) {
            return false;
        }
        if (level.getBlockTicks().hasScheduledTick(comparatorPos, Blocks.COMPARATOR)) {
            level.getBlockTicks().clearArea(new BoundingBox(comparatorPos));
        }
        level.scheduleTick(comparatorPos, Blocks.COMPARATOR, 0, TickPriority.HIGH);
        return true;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    public static boolean isPatternSlotItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (PackageCraftingPatternDataStorage.read(stack).isPresent()
                || AdvancedProcessingPatternDataStorage.hasData(stack)
                || PatternDetailsHelper.isEncodedPattern(stack)) {
            return true;
        }
        return false;
    }

    public boolean isPatternCapacityValid() {
        ItemStack patternStack = items.getStackInSlot(SLOT_PATTERN);
        if (patternStack.isEmpty()) {
            return true;
        }
        ItemStack capacityStack = items.getStackInSlot(SLOT_CAPACITY);
        if (sameStackAndCount(cachedCapacityPattern, patternStack)
                && sameStackAndCount(cachedCapacityComponent, capacityStack)) {
            return cachedPatternCapacityValid;
        }
        cachedCapacityPattern = patternStack.copy();
        cachedCapacityComponent = capacityStack.copy();
        cachedPatternCapacityValid = patternFitsCapacity(patternStack, configuredCapacityProfile());
        return cachedPatternCapacityValid;
    }

    private static boolean sameStackAndCount(ItemStack first, ItemStack second) {
        return first.getCount() == second.getCount() && ItemStack.isSameItemSameTags(first, second);
    }

    private static boolean isAe2ProcessingPattern(ItemStack stack) {
        return !stack.isEmpty() && AEItems.PROCESSING_PATTERN.isSameAs(stack);
    }

    private static boolean isAe2CraftingPattern(ItemStack stack) {
        return !stack.isEmpty() && AEItems.CRAFTING_PATTERN.isSameAs(stack);
    }

    public static int outputHandlerSlot(int outputSlot) {
        if (outputSlot < 0 || outputSlot >= OUTPUT_SLOT_COUNT) {
            throw new IndexOutOfBoundsException(outputSlot);
        }
        return outputSlot == 0 ? SLOT_OUTPUT : EXTRA_OUTPUT_SLOT_START + outputSlot - 1;
    }

    public static boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT
                || (slot >= EXTRA_OUTPUT_SLOT_START && slot < EXTRA_OUTPUT_SLOT_START + OUTPUT_SLOT_COUNT - 1);
    }

    public ItemStack menuInputDisplay(int slot) {
        MenuInputEntry entry = menuInput(slot);
        return entry == null ? ItemStack.EMPTY : entry.displayStack();
    }

    public int menuInputAmountForDisplay(int slot) {
        MenuInputEntry entry = menuInput(slot);
        if (entry == null) {
            return 0;
        }
        return entry.amount() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) entry.amount();
    }

    public ItemStack menuInputFilterStack(int slot) {
        List<GenericStack> filters = menuInputFilters();
        if (slot < 0 || slot >= filters.size() || filters.get(slot) == null) {
            return ItemStack.EMPTY;
        }
        return displayStack(filters.get(slot));
    }

    public int menuInputDisplaySlotCount() {
        int count = Math.min(menuInputFilters().size(), MAX_MENU_INPUT_SLOT_COUNT);
        for (int slot = menuInputs.length - 1; slot >= count; slot--) {
            if (menuInputs[slot] != null) {
                return slot + 1;
            }
        }
        return count;
    }

    public int menuInputSlotCount() {
        return menuInputDisplaySlotCount();
    }

    public int externalOutputSlot() {
        return menuInputSlotCount();
    }

    public int externalSlotCount() {
        return externalOutputSlot() + 1;
    }

    public boolean hasMenuInput(int slot) {
        return menuInput(slot) != null;
    }

    public int insertMenuInput(int preferredSlot, ItemStack stack, int requestedAmount, boolean simulate) {
        if (stack.isEmpty() || requestedAmount <= 0 || !isPatternCapacityValid()) {
            return 0;
        }
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return 0;
        }
        int amount = Math.min(requestedAmount, stack.getCount());
        long inserted = insertMenuInput(preferredSlot, key, amount, Actionable.ofSimulate(simulate));
        return (int) inserted;
    }

    private long insertMenuInput(int preferredSlot, AEKey key, long requestedAmount, Actionable mode) {
        if (key == null || requestedAmount <= 0 || !isPatternCapacityValid()) {
            return 0;
        }
        long inserted = 0;

        int inputSlotCount = Math.min(menuInputFilters().size(), MAX_MENU_INPUT_SLOT_COUNT);
        if (preferredSlot >= 0 && preferredSlot < inputSlotCount) {
            inserted = insertMenuInputIntoSlot(preferredSlot, key, requestedAmount, mode);
        } else {
            for (int slot = 0; slot < inputSlotCount && inserted < requestedAmount; slot++) {
                MenuInputEntry entry = menuInput(slot);
                if (entry != null && entry.key().equals(key)) {
                    inserted += insertMenuInputIntoSlot(slot, key, requestedAmount - inserted, mode);
                }
            }
            for (int slot = 0; slot < inputSlotCount && inserted < requestedAmount; slot++) {
                if (menuInput(slot) == null) {
                    inserted += insertMenuInputIntoSlot(slot, key, requestedAmount - inserted, mode);
                }
            }
        }

        if (inserted > 0 && mode == Actionable.MODULATE) {
            setChanged();
        }
        return inserted;
    }

    public ItemStack extractMenuInput(int slot, int requestedAmount, boolean simulate) {
        MenuInputEntry entry = menuInput(slot);
        if (entry == null || requestedAmount <= 0) {
            return ItemStack.EMPTY;
        }
        if (!(entry.key() instanceof AEItemKey itemKey)) {
            return ItemStack.EMPTY;
        }
        int amount = (int) Math.min(Math.min(entry.amount(), requestedAmount), itemKey.getMaxStackSize());
        ItemStack extracted = itemKey.toStack(amount);
        if (!simulate) {
            long remaining = entry.amount() - amount;
            menuInputs[slot] = remaining <= 0 ? null : new MenuInputEntry(entry.key(), remaining);
            trimMenuInputStorage();
            setChanged();
        }
        return extracted;
    }

    private long insertMenuInputIntoSlot(int slot, AEKey key, long requestedAmount, Actionable mode) {
        if (requestedAmount <= 0) {
            return 0;
        }
        MenuInputEntry current = menuInput(slot);
        if (current != null && !current.key().equals(key)) {
            return 0;
        }
        List<GenericStack> filters = menuInputFilters();
        if (!isPatternCapacityValid() || slot < 0 || slot >= filters.size()) {
            return 0;
        }
        GenericStack filter = filters.get(slot);
        if (filter == null || !filter.what().equals(key)) {
            return 0;
        }
        long currentAmount = current == null ? 0 : current.amount();
        long inserted = Math.min(requestedAmount, Math.max(0, filter.amount() - currentAmount));
        if (inserted > 0 && mode == Actionable.MODULATE) {
            ensureMenuInputStorageCapacity(slot + 1);
            menuInputs[slot] = new MenuInputEntry(key, currentAmount + inserted);
        }
        return inserted;
    }

    private MenuInputEntry menuInput(int slot) {
        if (slot < 0 || slot >= menuInputs.length) {
            return null;
        }
        return menuInputs[slot];
    }

    private boolean hasMenuInputs() {
        for (MenuInputEntry entry : menuInputs) {
            if (entry != null && entry.amount() > 0) {
                return true;
            }
        }
        return false;
    }

    private void clearMenuInputs() {
        menuInputs = new MenuInputEntry[0];
        refreshGenericInventoryCapability();
        setChanged();
    }

    private void ensureMenuInputStorageCapacity(int requiredLength) {
        if (requiredLength < 0 || requiredLength > MAX_MENU_INPUT_SLOT_COUNT) {
            throw new IndexOutOfBoundsException(requiredLength);
        }
        if (requiredLength > menuInputs.length) {
            menuInputs = Arrays.copyOf(menuInputs, requiredLength);
        }
    }

    private void trimMenuInputStorage() {
        int length = menuInputs.length;
        while (length > 0 && menuInputs[length - 1] == null) {
            length--;
        }
        if (length != menuInputs.length) {
            menuInputs = Arrays.copyOf(menuInputs, length);
            refreshGenericInventoryCapability();
        }
    }

    private List<GenericStack> menuInputFilters() {
        ItemStack patternStack = items.getStackInSlot(SLOT_PATTERN);
        if (cachedMenuFilterLevel == level
                && ItemStack.isSameItemSameTags(cachedMenuFilterPattern, patternStack)
                && cachedMenuFilterPattern.getCount() == patternStack.getCount()) {
            return cachedMenuInputFilters;
        }
        cachedMenuFilterLevel = level;
        cachedMenuFilterPattern = patternStack.copy();
        cachedAdvancedMenuInputFilters = List.of();
        if (!isPatternSlotItem(patternStack)) {
            cachedMenuInputFilters = List.of();
            return cachedMenuInputFilters;
        }
        Optional<AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern> advancedPattern =
                AdvancedProcessingPatternDataStorage.read(patternStack);
        if (advancedPattern.isPresent()) {
            cachedAdvancedMenuInputFilters = buildAdvancedMenuInputFilters(patternStack, advancedPattern.get());
            cachedMenuInputFilters = cachedAdvancedMenuInputFilters.stream()
                    .map(AdvancedMenuInputFilter::stack)
                    .toList();
            return cachedMenuInputFilters;
        }
        Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> packageCraftingPattern =
                PackageCraftingPatternDataStorage.read(patternStack);
        if (packageCraftingPattern.isPresent()) {
            cachedMenuInputFilters = packageCraftingPattern.get().denseInputs();
            return cachedMenuInputFilters;
        }
        if (PatternDetailsHelper.isEncodedPattern(patternStack)) {
            cachedMenuInputFilters = ordinaryPatternInputs(patternStack);
            return cachedMenuInputFilters;
        }
        cachedMenuInputFilters = List.of();
        return cachedMenuInputFilters;
    }

    private List<AdvancedMenuInputFilter> advancedMenuInputFilters(ItemStack patternStack) {
        menuInputFilters();
        if (!ItemStack.isSameItemSameTags(cachedMenuFilterPattern, patternStack)
                || cachedMenuFilterPattern.getCount() != patternStack.getCount()) {
            return List.of();
        }
        return cachedAdvancedMenuInputFilters;
    }

    private static List<AdvancedMenuInputFilter> buildAdvancedMenuInputFilters(
            ItemStack patternStack,
            AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern encoded) {
        List<AdvancedMenuInputFilter> filters = new ArrayList<>();
        for (var column : encoded.columns()) {
            for (GenericStack stack : column.inputs()) {
                if (stack != null && stack.amount() > 0) {
                    filters.add(new AdvancedMenuInputFilter(column.index(), stack));
                }
            }
        }
        return List.copyOf(filters);
    }

    public boolean isMenuInputSlotEnabled(int slot) {
        if (slot < 0 || slot >= menuInputSlotCount()) {
            return false;
        }
        List<GenericStack> filters = menuInputFilters();
        return menuInput(slot) != null
                || (isPatternCapacityValid() && slot < filters.size() && filters.get(slot) != null);
    }

    public boolean isMenuInputSlotValid(int slot) {
        MenuInputEntry entry = menuInput(slot);
        if (entry == null) {
            return true;
        }
        if (!isPatternCapacityValid()) {
            return false;
        }
        List<GenericStack> filters = menuInputFilters();
        if (slot < 0 || slot >= filters.size()) {
            return false;
        }
        GenericStack filter = filters.get(slot);
        return filter != null
                && filter.what().equals(entry.key())
                && entry.amount() <= filter.amount();
    }

    private List<GenericStack> ordinaryPatternInputs(ItemStack patternStack) {
        if (isAe2ProcessingPattern(patternStack)) {
            return densePositiveStacks(AdvancedProcessingPatternDataStorage.readSparseInputs(patternStack));
        }
        if (isAe2CraftingPattern(patternStack)) {
            return readCraftingPatternInputs(patternStack);
        }
        if (level == null || !PatternDetailsHelper.isEncodedPattern(patternStack)) {
            return List.of();
        }
        return decodedPatternInputs(PatternDetailsHelper.decodePattern(patternStack, level, false));
    }

    private static List<GenericStack> ordinaryPatternInputs(
            ItemStack patternStack,
            IPatternDetails details) {
        if (isAe2ProcessingPattern(patternStack)) {
            return densePositiveStacks(AdvancedProcessingPatternDataStorage.readSparseInputs(patternStack));
        }
        if (isAe2CraftingPattern(patternStack)) {
            return readCraftingPatternInputs(patternStack);
        }
        return decodedPatternInputs(details);
    }

    private static List<GenericStack> decodedPatternInputs(IPatternDetails details) {
        if (details == null) {
            return List.of();
        }
        List<GenericStack> filters = new ArrayList<>();
        for (var input : details.getInputs()) {
            GenericStack[] possibleInputs = input.getPossibleInputs();
            if (possibleInputs.length == 0 || possibleInputs[0] == null || possibleInputs[0].amount() <= 0) {
                continue;
            }
            GenericStack primary = possibleInputs[0];
            filters.add(new GenericStack(primary.what(), primary.amount() * input.getMultiplier()));
        }
        return List.copyOf(filters);
    }

    private static List<GenericStack> densePositiveStacks(List<GenericStack> sparseStacks) {
        List<GenericStack> stacks = new ArrayList<>();
        for (GenericStack stack : sparseStacks) {
            if (stack != null && stack.amount() > 0) {
                stacks.add(new GenericStack(stack.what(), stack.amount()));
            }
        }
        return List.copyOf(stacks);
    }

    private static List<GenericStack> readCraftingPatternInputs(ItemStack patternStack) {
        if (!patternStack.hasTag() || !patternStack.getTag().contains("in", Tag.TAG_LIST)) {
            return List.of();
        }
        List<GenericStack> inputs = new ArrayList<>();
        for (Tag element : patternStack.getTag().getList("in", Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag stackTag) || stackTag.isEmpty()) {
                continue;
            }
            GenericStack input = GenericStack.fromItemStack(ItemStack.of(stackTag));
            if (input != null && input.amount() > 0) {
                inputs.add(input);
            }
        }
        return List.copyOf(inputs);
    }

    private static Optional<PackageLayout> ordinaryPatternLayout(
            ItemStack patternStack,
            IPatternDetails details) {
        List<GenericStack> sparse;
        if (details instanceof appeng.crafting.pattern.AECraftingPattern craftingPattern) {
            sparse = Arrays.asList(craftingPattern.getSparseInputs());
        } else if (details instanceof appeng.crafting.pattern.AEProcessingPattern processingPattern) {
            sparse = Arrays.asList(processingPattern.getSparseInputs());
        } else if (isAe2ProcessingPattern(patternStack)) {
            sparse = AdvancedProcessingPatternDataStorage.readSparseInputs(patternStack);
        } else if (isAe2CraftingPattern(patternStack)
                && patternStack.hasTag()
                && patternStack.getTag().contains("in", Tag.TAG_LIST)) {
            List<GenericStack> craftingInputs = new ArrayList<>();
            for (Tag element : patternStack.getTag().getList("in", Tag.TAG_COMPOUND)) {
                GenericStack input = element instanceof CompoundTag stackTag && !stackTag.isEmpty()
                        ? GenericStack.fromItemStack(ItemStack.of(stackTag))
                        : null;
                craftingInputs.add(input != null && input.amount() > 0 ? input : null);
            }
            sparse = craftingInputs;
        } else {
            return Optional.empty();
        }
        return sparseInputLayout(sparse);
    }

    private static Optional<PackageLayout> sparseInputLayout(List<GenericStack> sparse) {
        List<Integer> contentSlots = new ArrayList<>();
        for (int slot = 0; slot < sparse.size(); slot++) {
            GenericStack input = sparse.get(slot);
            if (input != null && input.amount() > 0) {
                contentSlots.add(slot);
            }
        }
        return contentSlots.isEmpty()
                ? Optional.empty()
                : Optional.of(new PackageLayout(sparse.size(), contentSlots));
    }

    private Optional<MarkerSpec> ordinaryPatternMarker(ItemStack patternStack) {
        if (isAe2ProcessingPattern(patternStack)) {
            return firstStackAsMarker(AdvancedProcessingPatternDataStorage.readSparseOutputs(patternStack));
        }
        if (isAe2CraftingPattern(patternStack)) {
            if (!patternStack.hasTag() || !patternStack.getTag().contains("out", Tag.TAG_COMPOUND)) {
                return Optional.empty();
            }
            ItemStack output = ItemStack.of(patternStack.getTag().getCompound("out"));
            AEItemKey key = AEItemKey.of(output);
            return key == null
                    ? Optional.empty()
                    : Optional.of(new MarkerSpec(new GenericStack(key, 1)));
        }
        if (level == null) {
            return Optional.empty();
        }
        return patternDetailsMarker(PatternDetailsHelper.decodePattern(patternStack, level, false));
    }

    private static Optional<MarkerSpec> ordinaryPatternMarker(
            ItemStack patternStack,
            IPatternDetails details) {
        if (isAe2ProcessingPattern(patternStack)) {
            return firstStackAsMarker(AdvancedProcessingPatternDataStorage.readSparseOutputs(patternStack));
        }
        return patternDetailsMarker(details);
    }

    private static Optional<MarkerSpec> firstStackAsMarker(List<GenericStack> stacks) {
        for (GenericStack stack : stacks) {
            if (stack != null && stack.amount() > 0) {
                return Optional.of(new MarkerSpec(new GenericStack(stack.what(), 1)));
            }
        }
        return Optional.empty();
    }

    private static Optional<MarkerSpec> patternDetailsMarker(IPatternDetails details) {
        if (details == null) {
            return Optional.empty();
        }
        GenericStack output = details.getPrimaryOutput();
        return output == null || output.amount() <= 0
                ? Optional.empty()
                : Optional.of(new MarkerSpec(new GenericStack(output.what(), 1)));
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

    private Optional<MarkerSpec> ordinaryPatternMarkerWithOverride(ItemStack patternStack) {
        return configuredMarkerItem().or(() -> ordinaryPatternMarker(patternStack));
    }

    private Optional<MarkerSpec> ordinaryPatternMarkerWithOverride(
            ItemStack patternStack,
            IPatternDetails details) {
        return configuredMarkerItem().or(() -> ordinaryPatternMarker(patternStack, details));
    }

    private static List<GenericStack> genericStacksFromMap(Map<AEKey, Long> amounts) {
        List<GenericStack> stacks = new ArrayList<>();
        for (var entry : amounts.entrySet()) {
            if (entry.getValue() > 0) {
                stacks.add(new GenericStack(entry.getKey(), entry.getValue()));
            }
        }
        return List.copyOf(stacks);
    }

    private static boolean menuInputsMatchFilters(MenuInputEntry[] inputs, List<GenericStack> filters) {
        for (int slot = 0; slot < inputs.length; slot++) {
            MenuInputEntry entry = inputs[slot];
            if (entry == null) {
                continue;
            }
            if (slot >= filters.size()) {
                return false;
            }
            GenericStack filter = filters.get(slot);
            if (filter == null
                    || !filter.what().equals(entry.key())
                    || entry.amount() > filter.amount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean menuInputsExactlyMatchFilters(MenuInputEntry[] inputs, List<GenericStack> filters) {
        if (filters.isEmpty()) {
            return false;
        }
        int slotCount = Math.max(inputs.length, filters.size());
        for (int slot = 0; slot < slotCount; slot++) {
            MenuInputEntry entry = slot < inputs.length ? inputs[slot] : null;
            if (slot >= filters.size()) {
                if (entry != null) {
                    return false;
                }
                continue;
            }
            GenericStack filter = filters.get(slot);
            if (filter == null) {
                if (entry != null) {
                    return false;
                }
                continue;
            }
            if (entry == null) {
                return false;
            }
            if (!filter.what().equals(entry.key()) || entry.amount() != filter.amount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean packageContentsExactlyMatch(PackageData data, List<GenericStack> filters) {
        List<GenericStack> expected = filters.stream()
                .filter(stack -> stack != null && stack.amount() > 0)
                .toList();
        List<GenericStack> actual = data.contents();
        if (actual.size() != expected.size()) {
            return false;
        }
        for (int slot = 0; slot < expected.size(); slot++) {
            GenericStack expectedStack = expected.get(slot);
            GenericStack actualStack = actual.get(slot);
            if (!actualStack.what().equals(expectedStack.what())
                    || actualStack.amount() != expectedStack.amount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean menuInputsFitCapacity(MenuInputEntry[] inputs, PackageCapacityProfile capacityProfile) {
        MenuInputContents contents = menuInputContents(inputs);
        if (contents.isEmpty()) {
            return true;
        }
        return PackagePlanBuilder.buildOrdered(
                        PackageColor.FLUIX,
                        contents.orderedContents(),
                        contents.sourcePackages(),
                        MarkerMergeMode.RETAIN,
                        Optional.empty(),
                        capacityProfile,
                        0)
                .success();
    }

    public boolean autoExport() {
        return outputMode != OutputMode.NONE;
    }

    public void setAutoExport(boolean autoExport) {
        setOutputMode(autoExport ? OutputMode.ME_NETWORK : OutputMode.NONE);
    }

    public OutputMode outputMode() {
        return outputMode;
    }

    public void setOutputMode(OutputMode outputMode) {
        OutputMode next = outputMode == null ? OutputMode.ME_NETWORK : outputMode;
        if (this.outputMode != next) {
            if (autoExportBatchActive
                    && next != OutputMode.NONE
                    && autoExportBatchMode != next) {
                resetAutoExportBatch();
            }
            this.outputMode = next;
            setChanged();
        }
    }

    public void toggleAutoExport() {
        setOutputMode(outputMode.next());
    }

    public boolean blockingMode() {
        return blockingMode;
    }

    public void setBlockingMode(boolean blockingMode) {
        if (this.blockingMode != blockingMode) {
            this.blockingMode = blockingMode;
            setChanged();
        }
    }

    public void toggleBlockingMode() {
        setBlockingMode(!blockingMode);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.appliedpackaging.package_assembler");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PackageAssemblerMenu(containerId, playerInventory, this);
    }

    public AssemblyResult tryAssemble() {
        if (level != null && !level.isClientSide) {
            return tryStartAssembly();
        }
        if (!outputSlotsEmpty()) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        if (!pendingPackages.isEmpty()) {
            return outputPendingPackage();
        }
        if (!isPatternCapacityValid()) {
            return AssemblyResult.PATTERN_MISMATCH;
        }
        if (hasMenuInputs()) {
            if (AdvancedProcessingPatternDataStorage.hasData(items.getStackInSlot(SLOT_PATTERN))) {
                Optional<MenuBatchPlan> batch = planMenuAdvancedPattern(items.getStackInSlot(SLOT_PATTERN));
                if (batch.isEmpty()) {
                    return AssemblyResult.PATTERN_MISMATCH;
                }
                if (!commitMenuExtractions(batch.get().menuExtractions())) {
                    return AssemblyResult.PATTERN_MISMATCH;
                }
                return commitProviderPackages(batch.get().packages())
                        ? AssemblyResult.ASSEMBLED
                        : AssemblyResult.OUTPUT_BLOCKED;
            }
            AssemblyAttempt attempt = planMenuAssembly();
            if (attempt.plan().isEmpty()) {
                return attempt.failure();
            }
            return commitMenuAssemblyPlan(attempt.plan().orElseThrow());
        }
        return AssemblyResult.NO_CONTENTS;
    }

    private AssemblyResult tryStartAssembly() {
        if (!outputSlotsEmpty() || !pendingPackages.isEmpty() || !activePackages.isEmpty()) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        if (!isPatternCapacityValid()) {
            return AssemblyResult.PATTERN_MISMATCH;
        }
        if (hasMenuInputs()) {
            if (AdvancedProcessingPatternDataStorage.hasData(items.getStackInSlot(SLOT_PATTERN))) {
                Optional<MenuBatchPlan> batch = planMenuAdvancedPattern(items.getStackInSlot(SLOT_PATTERN));
                if (batch.isEmpty()) {
                    return AssemblyResult.PATTERN_MISMATCH;
                }
                beginMenuCrafting(new MenuCraftPlan(
                        batch.get().packages(),
                        batch.get().menuExtractions()));
                return AssemblyResult.ASSEMBLED;
            }
            AssemblyAttempt attempt = planMenuAssembly();
            if (attempt.plan().isEmpty()) {
                return attempt.failure();
            }
            return beginMenuAssemblyPlan(attempt.plan().orElseThrow());
        }
        return AssemblyResult.NO_CONTENTS;
    }

    private AssemblyResult beginMenuAssemblyPlan(AssemblyPlan plan) {
        if (!outputSlotsEmpty()) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        beginMenuCrafting(new MenuCraftPlan(
                List.of(new QueuedPackage(plan.color(), plan.data())),
                plan.menuExtractions()));
        return AssemblyResult.ASSEMBLED;
    }

    private void beginMenuCrafting(MenuCraftPlan plan) {
        beginCrafting(plan.packages());
        activeMenuPattern = items.getStackInSlot(SLOT_PATTERN).copy();
        setChanged();
    }

    private void beginCrafting(List<QueuedPackage> packages) {
        if (packages.isEmpty()) {
            return;
        }
        activePackages.clear();
        activePackages.addAll(packages);
        activeMenuPattern = ItemStack.EMPTY;
        craftingProgress = 0;
        scheduleComparatorSamplingNextTick();
        notifyComparatorOutputChanged();
        setChanged();
        syncClientVisualState();
    }

    private void advanceCraftingProgress() {
        if (activePackages.isEmpty()) {
            craftingProgress = 0;
            return;
        }
        if (!outputSlotsEmpty()) {
            return;
        }
        Optional<MenuCraftPlan> activeMenuPlan = Optional.empty();
        if (!activeMenuPattern.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(activeMenuPattern, items.getStackInSlot(SLOT_PATTERN))) {
                cancelActiveMenuCraft();
                return;
            }
            activeMenuPlan = currentMenuCraftPlan();
            if (activeMenuPlan.isEmpty() || !activeMenuPlan.get().packages().equals(activePackages)) {
                return;
            }
        }
        int step = craftingProgressStep();
        if (step <= 0) {
            return;
        }
        craftingProgress += step;
        if (craftingProgress < MAX_CRAFT_PROGRESS) {
            setChanged();
            return;
        }
        List<QueuedPackage> packages = List.copyOf(activePackages);
        if (activeMenuPlan.isPresent()
                && !commitMenuExtractions(activeMenuPlan.get().menuExtractions())) {
            craftingProgress = MAX_CRAFT_PROGRESS - 1;
            setChanged();
            return;
        }
        activePackages.clear();
        activeMenuPattern = ItemStack.EMPTY;
        craftingProgress = 0;
        commitProviderPackages(packages);
        setChanged();
        syncClientVisualState();
    }

    private Optional<MenuCraftPlan> currentMenuCraftPlan() {
        if (!isPatternCapacityValid()) {
            return Optional.empty();
        }
        ItemStack patternStack = items.getStackInSlot(SLOT_PATTERN);
        if (AdvancedProcessingPatternDataStorage.hasData(patternStack)) {
            return planMenuAdvancedPattern(patternStack)
                    .map(plan -> new MenuCraftPlan(plan.packages(), plan.menuExtractions()));
        }
        AssemblyAttempt attempt = planMenuAssembly();
        return attempt.plan().map(plan -> new MenuCraftPlan(
                List.of(new QueuedPackage(plan.color(), plan.data())),
                plan.menuExtractions()));
    }

    private void cancelActiveMenuCraft() {
        activePackages.clear();
        activeMenuPattern = ItemStack.EMPTY;
        craftingProgress = 0;
        scheduleComparatorSamplingNextTick();
        notifyComparatorOutputChanged();
        setChanged();
        syncClientVisualState();
    }

    private int craftingProgressStep() {
        return switch (upgrades.getInstalledUpgrades(AEItems.SPEED_CARD)) {
            case 0 -> useCraftingPower(1, 10, 1.0);
            case 1 -> useCraftingPower(1, 13, 1.3);
            case 2 -> useCraftingPower(1, 17, 1.7);
            case 3 -> useCraftingPower(1, 20, 2.0);
            case 4 -> useCraftingPower(1, 25, 2.5);
            default -> useCraftingPower(1, 50, 5.0);
        };
    }

    private int useCraftingPower(int ticksPassed, int progressValue, double acceleratorTax) {
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return 0;
        }
        return (int) (grid.getEnergyService().extractAEPower(
                ticksPassed * progressValue * acceleratorTax,
                Actionable.MODULATE,
                PowerMultiplier.CONFIG) / acceleratorTax);
    }

    public int craftingProgress() {
        return craftingProgress;
    }

    public boolean isCrafting() {
        return !activePackages.isEmpty();
    }

    public ItemStack activePackageDisplayStack() {
        if (activePackages.isEmpty()) {
            return ItemStack.EMPTY;
        }
        QueuedPackage active = activePackages.get(0);
        return packageStack(active.color(), active.data());
    }

    private void syncClientVisualState() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public boolean exportOutputOnce() {
        if (level == null || level.isClientSide) {
            return false;
        }
        if (outputMode == OutputMode.NONE) {
            return false;
        }

        return switch (outputMode) {
            case ME_NETWORK -> findOutputMEStorage()
                    .filter(target -> ensureAutoExportBatchStarted(OutputMode.ME_NETWORK, target))
                    .map(this::exportOutputOnceToMEStorage)
                    .orElse(false);
            case ADJACENT_BLOCK -> findAutoExportItemTarget()
                    .map(target -> exportOutputOnceToItemHandler(target.handler()))
                    .orElse(false);
            case NONE -> false;
        };
    }

    private boolean exportOutputIfEnabled() {
        if (level == null
                || level.isClientSide
                || outputMode == OutputMode.NONE) {
            return false;
        }
        return switch (outputMode) {
            case ME_NETWORK -> findOutputMEStorage()
                    .filter(target -> ensureAutoExportBatchStarted(OutputMode.ME_NETWORK, target))
                    .map(this::exportAllOutputsToMEStorage)
                    .orElse(false);
            case ADJACENT_BLOCK -> findAutoExportItemTarget()
                    .map(target -> exportAllOutputsToItemHandler(target.handler()))
                    .orElse(false);
            case NONE -> false;
        };
    }

    private boolean exportAllOutputsToMEStorage(MEStorage target) {
        boolean exported = false;
        while (exportOutputOnceToMEStorage(target)) {
            exported = true;
        }
        finishAutoExportBatchIfEmpty();
        return exported;
    }

    private boolean exportAllOutputsToItemHandler(IItemHandler target) {
        boolean exported = false;
        while (exportOutputOnceToItemHandler(target)) {
            exported = true;
        }
        finishAutoExportBatchIfEmpty();
        return exported;
    }

    private boolean exportOutputOnceToMEStorage(MEStorage target) {
        promoteNextOutput();
        ItemStack output = items.getStackInSlot(SLOT_OUTPUT);
        return !output.isEmpty() && exportOutputToMEStorage(SLOT_OUTPUT, target, output);
    }

    private boolean exportOutputOnceToItemHandler(IItemHandler target) {
        promoteNextOutput();
        ItemStack output = items.getStackInSlot(SLOT_OUTPUT);
        return !output.isEmpty() && exportOutputToItemHandler(SLOT_OUTPUT, target, output);
    }

    private boolean ensureAutoExportBatchStarted(OutputMode mode, MEStorage target) {
        promoteNextOutput();
        List<ItemStack> outputs = completedOutputStacks();
        if (outputs.isEmpty()) {
            finishAutoExportBatchIfEmpty();
            return false;
        }
        if (autoExportBatchActive) {
            return autoExportBatchMode == mode;
        }
        Set<AEKey> outputTypes = outputTypes(outputs);
        if (blockingMode && containsAnyOutputType(target, outputTypes)) {
            return false;
        }
        if (!acceptsOutput(target, outputs.get(0))) {
            return false;
        }
        startAutoExportBatch(mode, null);
        return true;
    }

    private boolean ensureAutoExportBatchStarted(Direction direction, IItemHandler target) {
        promoteNextOutput();
        List<ItemStack> outputs = completedOutputStacks();
        if (outputs.isEmpty()) {
            finishAutoExportBatchIfEmpty();
            return false;
        }
        if (autoExportBatchActive) {
            return autoExportBatchMode == OutputMode.ADJACENT_BLOCK
                    && autoExportBatchDirection == direction;
        }
        Set<AEKey> outputTypes = outputTypes(outputs);
        if (blockingMode && containsAnyOutputType(target, outputTypes)) {
            return false;
        }
        if (!acceptsOutput(target, outputs.get(0))) {
            return false;
        }
        startAutoExportBatch(OutputMode.ADJACENT_BLOCK, direction);
        return true;
    }

    private Optional<AdjacentItemTarget> findAutoExportItemTarget() {
        if (autoExportBatchActive) {
            if (autoExportBatchMode != OutputMode.ADJACENT_BLOCK || autoExportBatchDirection == null) {
                return Optional.empty();
            }
            return findOutputItemHandler(autoExportBatchDirection)
                    .filter(target -> ensureAutoExportBatchStarted(autoExportBatchDirection, target))
                    .map(target -> new AdjacentItemTarget(autoExportBatchDirection, target));
        }
        for (Direction direction : Direction.values()) {
            Optional<IItemHandler> target = findOutputItemHandler(direction);
            if (target.isPresent() && ensureAutoExportBatchStarted(direction, target.get())) {
                return Optional.of(new AdjacentItemTarget(direction, target.get()));
            }
        }
        return Optional.empty();
    }

    private List<ItemStack> completedOutputStacks() {
        List<ItemStack> outputs = new ArrayList<>();
        for (int outputIndex = 0; outputIndex < OUTPUT_SLOT_COUNT; outputIndex++) {
            ItemStack output = items.getStackInSlot(outputHandlerSlot(outputIndex));
            if (!output.isEmpty()) {
                outputs.add(output.copy());
            }
        }
        for (QueuedPackage pendingPackage : pendingPackages) {
            outputs.add(packageStack(pendingPackage.color(), pendingPackage.data()));
        }
        return outputs;
    }
    private static Set<AEKey> outputTypes(List<ItemStack> outputs) {
        Set<AEKey> outputTypes = new HashSet<>();
        for (ItemStack output : outputs) {
            outputTypes.add(AEItemKey.of(output).dropSecondary());
        }
        return outputTypes;
    }

    private static boolean containsAnyOutputType(MEStorage target, Set<AEKey> outputTypes) {
        for (var entry : target.getAvailableStacks()) {
            if (entry.getLongValue() > 0 && outputTypes.contains(entry.getKey().dropSecondary())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyOutputType(IItemHandler target, Set<AEKey> outputTypes) {
        for (int slot = 0; slot < target.getSlots(); slot++) {
            ItemStack stack = target.getStackInSlot(slot);
            if (!stack.isEmpty() && outputTypes.contains(AEItemKey.of(stack).dropSecondary())) {
                return true;
            }
        }
        return false;
    }

    private static boolean acceptsOutput(MEStorage target, ItemStack output) {
        long accepted = target.insert(
                AEItemKey.of(output),
                output.getCount(),
                Actionable.SIMULATE,
                IActionSource.empty());
        return accepted >= output.getCount();
    }

    private static boolean acceptsOutput(IItemHandler target, ItemStack output) {
        return ItemHandlerHelper.insertItemStacked(target, output.copy(), true).isEmpty();
    }

    private void startAutoExportBatch(OutputMode mode, Direction direction) {
        autoExportBatchActive = true;
        autoExportBatchMode = mode;
        autoExportBatchDirection = direction;
        setChanged();
    }

    private void finishAutoExportBatchIfEmpty() {
        if (autoExportBatchActive && completedOutputCount() == 0) {
            resetAutoExportBatch();
        }
    }

    private void resetAutoExportBatch() {
        if (!autoExportBatchActive
                && autoExportBatchMode == OutputMode.NONE
                && autoExportBatchDirection == null) {
            return;
        }
        autoExportBatchActive = false;
        autoExportBatchMode = OutputMode.NONE;
        autoExportBatchDirection = null;
        setChanged();
    }

    private boolean exportOutputToMEStorage(int itemSlot, MEStorage target, ItemStack output) {
        AEItemKey key = AEItemKey.of(output);
        long simulated = target.insert(key, 1, Actionable.SIMULATE, IActionSource.empty());
        if (simulated <= 0) {
            return false;
        }
        long committed = target.insert(key, Math.min(1, simulated), Actionable.MODULATE, IActionSource.empty());
        if (committed <= 0) {
            return false;
        }
        items.extractItem(itemSlot, (int) committed, false);
        promoteNextOutput();
        setChanged();
        return true;
    }

    private boolean exportOutputToItemHandler(int itemSlot, IItemHandler target, ItemStack output) {
        ItemStack single = output.copy();
        single.setCount(1);
        ItemStack simulatedRemainder = ItemHandlerHelper.insertItemStacked(target, single.copy(), true);
        int transferable = single.getCount() - simulatedRemainder.getCount();
        if (transferable <= 0) {
            return false;
        }

        ItemStack extracted = items.extractItem(itemSlot, transferable, true);
        if (extracted.isEmpty()) {
            return false;
        }
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, extracted.copy(), false);
        int inserted = extracted.getCount() - remainder.getCount();
        if (inserted <= 0) {
            return false;
        }
        items.extractItem(itemSlot, inserted, false);
        promoteNextOutput();
        setChanged();
        return true;
    }

    private AssemblyResult commitMenuAssemblyPlan(AssemblyPlan plan) {
        ItemStack packageStack = packageStack(plan.color(), plan.data());
        if (!canInsertOutputPackage(packageStack)) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        if (plan.menuExtractions() != null) {
            if (!commitMenuExtractions(plan.menuExtractions())) {
                return AssemblyResult.PATTERN_MISMATCH;
            }
        } else {
            clearMenuInputs();
        }
        insertOutputPackage(packageStack);
        setChanged();
        return AssemblyResult.ASSEMBLED;
    }

    private AssemblyResult outputPendingPackage() {
        if (pendingPackages.isEmpty()) {
            return AssemblyResult.NO_CONTENTS;
        }
        if (!outputSlotsEmpty()) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        AssemblyResult result = outputPackage(pendingPackages.get(0));
        if (result == AssemblyResult.ASSEMBLED) {
            pendingPackages.remove(0);
            notifyComparatorOutputChanged();
            setChanged();
        }
        return result;
    }

    private AssemblyResult outputPackage(QueuedPackage queuedPackage) {
        ItemStack packageStack = packageStack(queuedPackage.color(), queuedPackage.data());
        if (!canInsertOutputPackage(packageStack)) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        insertOutputPackage(packageStack);
        setChanged();
        return AssemblyResult.ASSEMBLED;
    }

    private boolean hasOutputRoom() {
        return outputSlotsEmpty();
    }

    private boolean outputSlotsEmpty() {
        return items.getStackInSlot(SLOT_OUTPUT).isEmpty();
    }

    private boolean canInsertOutputPackage(ItemStack packageStack) {
        return findOutputSlotFor(packageStack).isPresent();
    }

    private ItemStack insertOutputPackage(ItemStack packageStack) {
        Optional<Integer> slot = findOutputSlotFor(packageStack);
        if (slot.isEmpty()) {
            return packageStack;
        }
        return items.insertItem(slot.get(), packageStack, false);
    }

    private Optional<Integer> findOutputSlotFor(ItemStack packageStack) {
        return items.insertItem(SLOT_OUTPUT, packageStack.copy(), true).isEmpty()
                ? Optional.of(SLOT_OUTPUT)
                : Optional.empty();
    }

    private AssemblyAttempt planMenuAssembly() {
        ItemStack patternStack = items.getStackInSlot(SLOT_PATTERN);
        Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> packageCraftingPattern =
                PackageCraftingPatternDataStorage.read(patternStack);
        if (packageCraftingPattern.isPresent()) {
            return planMenuExactPackage(
                            packageCraftingPattern.get().color(),
                            packageCraftingPattern.get().data())
                    .map(AssemblyAttempt::planned)
                    .orElseGet(() -> AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH));
        }

        if (PatternDetailsHelper.isEncodedPattern(patternStack)) {
            return planMenuDefaultEncodedPattern(patternStack);
        }

        return AssemblyAttempt.failed(AssemblyResult.NO_CONTENTS);
    }

    private Optional<AssemblyPlan> planMenuExactPackage(PackageColor color, PackageData target) {
        if (!menuInputsExactlyMatchFilters(menuInputs, target.contents())) {
            return Optional.empty();
        }
        MenuExactExtraction extraction = extractMenuInputsForTarget(target);
        if (!extraction.matched()) {
            return Optional.empty();
        }
        MarkerMergeMode markerMode = target.marker().isPresent()
                ? MarkerMergeMode.OVERRIDE
                : MarkerMergeMode.CLEAR;
        PackageCapacityProfile capacityProfile = configuredCapacityProfile();
        PackagePlanResult result = PackagePlanBuilder.buildOrdered(
                color,
                target.contents(),
                target.layout(),
                extraction.sourcePackages(),
                markerMode,
                target.marker(),
                capacityProfile,
                target.flags());
        if (result.data().isEmpty()
                || !result.data().orElseThrow().canonicalHash().equals(target.canonicalHash())) {
            return Optional.empty();
        }
        return Optional.of(new AssemblyPlan(color, result.data().orElseThrow(), extraction.extractions()));
    }

    private Optional<MenuBatchPlan> planMenuAdvancedPattern(ItemStack patternStack) {
        Optional<AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern> encoded =
                AdvancedProcessingPatternDataStorage.read(patternStack);
        if (encoded.isEmpty()) {
            return Optional.empty();
        }
        List<AdvancedMenuInputFilter> advancedFilters = advancedMenuInputFilters(patternStack);
        List<GenericStack> filters = advancedFilters.stream()
                .map(AdvancedMenuInputFilter::stack)
                .toList();
        if (!menuInputsExactlyMatchFilters(menuInputs, filters)) {
            return Optional.empty();
        }

        List<QueuedPackage> packages = new ArrayList<>();
        List<MenuInputExtraction> extractions = new ArrayList<>();
        List<List<GenericStack>> columnInputs = new ArrayList<>(encoded.get().activeColumnCount());
        for (int column = 0; column < encoded.get().activeColumnCount(); column++) {
            columnInputs.add(new ArrayList<>());
        }
        for (int denseSlot = 0; denseSlot < advancedFilters.size(); denseSlot++) {
            MenuInputEntry entry = menuInput(denseSlot);
            if (entry == null || entry.amount() <= 0) {
                return Optional.empty();
            }
            AdvancedMenuInputFilter filter = advancedFilters.get(denseSlot);
            columnInputs.get(filter.column()).add(new GenericStack(entry.key(), entry.amount()));
            extractions.add(new MenuInputExtraction(denseSlot, entry.amount()));
        }
        for (var column : encoded.get().columns()) {
            List<GenericStack> inputs = columnInputs.get(column.index());
            if (inputs.isEmpty()) {
                continue;
            }
            Optional<QueuedPackage> queued = buildPatternPackage(
                    column.color(),
                    column.marker(),
                    inputs,
                    sparseInputLayout(column.inputs()),
                    configuredCapacityProfile());
            if (queued.isEmpty()) {
                return Optional.empty();
            }
            packages.add(queued.get());
        }
        if (packages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MenuBatchPlan(packages, extractions));
    }

    private MenuExactExtraction extractMenuInputsForTarget(PackageData target) {
        Map<AEKey, Long> remaining = new LinkedHashMap<>();
        for (GenericStack stack : target.contents()) {
            remaining.merge(stack.what(), stack.amount(), Long::sum);
        }

        List<GenericStack> looseContents = new ArrayList<>();
        List<PackageData> sourcePackages = new ArrayList<>();
        List<MenuInputExtraction> extractions = new ArrayList<>();
        for (int slot = 0; slot < menuInputs.length; slot++) {
            MenuInputEntry entry = menuInputs[slot];
            if (entry == null || entry.amount() <= 0 || remainingValuesEmpty(remaining)) {
                continue;
            }

            Optional<PackageData> packageData = packageData(entry);
            if (packageData.isPresent()) {
                long packagesToUse = 0;
                for (long count = 0; count < entry.amount() && packageFitsRemaining(packageData.get(), remaining); count++) {
                    subtractPackageContents(packageData.get(), remaining);
                    sourcePackages.add(packageData.get());
                    packagesToUse++;
                }
                if (packagesToUse > 0) {
                    extractions.add(new MenuInputExtraction(slot, packagesToUse));
                }
                continue;
            }

            AEKey key = entry.key();
            long required = remaining.getOrDefault(key, 0L);
            if (required <= 0) {
                continue;
            }
            long used = Math.min(required, entry.amount());
            remaining.put(key, required - used);
            looseContents.add(new GenericStack(key, used));
            extractions.add(new MenuInputExtraction(slot, used));
        }

        return new MenuExactExtraction(
                remainingValuesEmpty(remaining),
                List.copyOf(looseContents),
                List.copyOf(sourcePackages),
                List.copyOf(extractions));
    }

    private static boolean remainingValuesEmpty(Map<AEKey, Long> remaining) {
        for (long amount : remaining.values()) {
            if (amount > 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean packageFitsRemaining(PackageData data, Map<AEKey, Long> remaining) {
        for (GenericStack stack : data.contents()) {
            if (remaining.getOrDefault(stack.what(), 0L) < stack.amount()) {
                return false;
            }
        }
        return true;
    }

    private static void subtractPackageContents(PackageData data, Map<AEKey, Long> remaining) {
        for (GenericStack stack : data.contents()) {
            remaining.put(stack.what(), remaining.getOrDefault(stack.what(), 0L) - stack.amount());
        }
    }

    private AssemblyAttempt planMenuDefaultEncodedPattern(ItemStack patternStack) {
        List<GenericStack> filters = ordinaryPatternInputs(patternStack);
        if (!menuInputsExactlyMatchFilters(menuInputs, filters)) {
            return AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH);
        }
        MenuInputContents contents = menuInputContents(menuInputs);
        Optional<MarkerSpec> marker = ordinaryPatternMarkerWithOverride(patternStack);
        PackagePlanResult result = PackagePlanBuilder.buildOrdered(
                selectedColor,
                contents.orderedContents(),
                ordinaryPatternLayout(patternStack, null),
                contents.sourcePackages(),
                marker.isPresent() ? MarkerMergeMode.OVERRIDE : MarkerMergeMode.CLEAR,
                marker,
                configuredCapacityProfile(),
                0);
        if (result.data().isEmpty() || !packageContentsExactlyMatch(result.data().orElseThrow(), filters)) {
            return AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH);
        }
        List<MenuInputExtraction> extractions = new ArrayList<>();
        for (int slot = 0; slot < filters.size(); slot++) {
            MenuInputEntry entry = menuInput(slot);
            if (entry != null && entry.amount() > 0) {
                extractions.add(new MenuInputExtraction(slot, entry.amount()));
            }
        }
        return AssemblyAttempt.planned(new AssemblyPlan(
                selectedColor,
                result.data().orElseThrow(),
                List.copyOf(extractions)));
    }

    private boolean patternFitsCapacity(ItemStack patternStack, PackageCapacityProfile capacity) {
        Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> packagePattern =
                PackageCraftingPatternDataStorage.read(patternStack);
        if (packagePattern.isPresent()) {
            PackageData data = packagePattern.get().data();
            return capacity.fits(data.usedUnits(), data.usedTypes());
        }

        Optional<AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern> advancedPattern =
                AdvancedProcessingPatternDataStorage.read(patternStack);
        if (advancedPattern.isPresent()) {
            List<AdvancedMenuInputFilter> filters = buildAdvancedMenuInputFilters(patternStack, advancedPattern.get());
            List<List<GenericStack>> columnInputs = new ArrayList<>(advancedPattern.get().activeColumnCount());
            for (int column = 0; column < advancedPattern.get().activeColumnCount(); column++) {
                columnInputs.add(new ArrayList<>());
            }
            for (AdvancedMenuInputFilter filter : filters) {
                columnInputs.get(filter.column()).add(filter.stack());
            }
            boolean hasPackage = false;
            for (var column : advancedPattern.get().columns()) {
                List<GenericStack> inputs = columnInputs.get(column.index());
                if (inputs.isEmpty()) {
                    continue;
                }
                hasPackage = true;
                if (buildPatternPackage(
                                column.color(),
                                column.marker(),
                                inputs,
                                sparseInputLayout(column.inputs()),
                                capacity)
                        .isEmpty()) {
                    return false;
                }
            }
            return hasPackage;
        }

        if (!PatternDetailsHelper.isEncodedPattern(patternStack)) {
            return false;
        }
        List<GenericStack> inputs = ordinaryPatternInputs(patternStack);
        Optional<MarkerSpec> marker = ordinaryPatternMarkerWithOverride(patternStack);
        return !inputs.isEmpty()
                && buildPatternPackage(
                                selectedColor,
                                marker,
                                inputs,
                                ordinaryPatternLayout(patternStack, null),
                                capacity)
                        .isPresent();
    }

    private boolean patternFitsCapacity(
            ItemStack patternStack,
            IPatternDetails patternDetails,
            PackageCapacityProfile capacity) {
        if (PackageCraftingPatternDataStorage.read(patternStack).isPresent()
                || AdvancedProcessingPatternDataStorage.hasData(patternStack)) {
            return patternFitsCapacity(patternStack, capacity);
        }
        if (!PatternDetailsHelper.isEncodedPattern(patternStack)) {
            return false;
        }
        List<GenericStack> inputs = ordinaryPatternInputs(patternStack, patternDetails);
        Optional<MarkerSpec> marker = ordinaryPatternMarkerWithOverride(patternStack, patternDetails);
        return !inputs.isEmpty()
                && buildPatternPackage(
                                selectedColor,
                                marker,
                                inputs,
                                ordinaryPatternLayout(patternStack, patternDetails),
                                capacity)
                        .isPresent();
    }

    private Optional<QueuedPackage> buildPatternPackage(
            PackageColor color,
            Optional<MarkerSpec> marker,
            List<GenericStack> inputs) {
        return buildPatternPackage(color, marker, inputs, configuredCapacityProfile());
    }

    private Optional<QueuedPackage> buildPatternPackage(
            PackageColor color,
            Optional<MarkerSpec> marker,
            List<GenericStack> inputs,
            PackageCapacityProfile capacity) {
        return buildPatternPackage(color, marker, inputs, Optional.empty(), capacity);
    }

    private Optional<QueuedPackage> buildPatternPackage(
            PackageColor color,
            Optional<MarkerSpec> marker,
            List<GenericStack> inputs,
            Optional<PackageLayout> layout,
            PackageCapacityProfile capacity) {
        List<GenericStack> orderedContents = new ArrayList<>();
        List<PackageData> sourcePackages = new ArrayList<>();
        for (GenericStack input : inputs) {
            if (input == null || input.amount() <= 0) {
                continue;
            }
            Optional<PackageData> sourcePackage = Optional.empty();
            if (input.what() instanceof AEItemKey itemKey) {
                sourcePackage = PackageDataStorage.read(itemKey.toStack());
            }
            if (sourcePackage.isPresent()) {
                if (input.amount() > capacity.unitLimit()) {
                    return Optional.empty();
                }
                for (long count = 0; count < input.amount(); count++) {
                    sourcePackages.add(sourcePackage.get());
                    orderedContents.addAll(sourcePackage.get().contents());
                }
            } else {
                orderedContents.add(input);
            }
        }
        PackagePlanResult result = PackagePlanBuilder.buildOrdered(
                color,
                orderedContents,
                layout,
                sourcePackages,
                marker.isPresent() ? MarkerMergeMode.OVERRIDE : MarkerMergeMode.CLEAR,
                marker,
                capacity,
                0);
        return result.data().map(data -> new QueuedPackage(color, data));
    }

    private Optional<ProviderPlan> planAdvancedProcessingPush(
            ItemStack definitionStack,
            KeyCounter[] inputHolder) {
        Optional<AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern> encoded =
                AdvancedProcessingPatternDataStorage.read(definitionStack);
        if (encoded.isEmpty()) {
            return Optional.empty();
        }
        Map<AEKey, Long> remainingInputs = new HashMap<>(aggregateInputs(inputHolder).orElse(Map.of()));
        List<List<GenericStack>> columnInputs = new ArrayList<>();
        for (int column = 0; column < encoded.get().activeColumnCount(); column++) {
            columnInputs.add(new ArrayList<>());
        }
        List<GenericStack> consumedInputs = new ArrayList<>();
        for (var column : encoded.get().columns()) {
            for (GenericStack input : column.inputs()) {
                if (input == null || input.amount() <= 0) {
                    continue;
                }
                long available = remainingInputs.getOrDefault(input.what(), 0L);
                if (available < input.amount()) {
                    return Optional.empty();
                }
                long remaining = available - input.amount();
                if (remaining == 0) {
                    remainingInputs.remove(input.what());
                } else {
                    remainingInputs.put(input.what(), remaining);
                }
                GenericStack consumed = new GenericStack(input.what(), input.amount());
                columnInputs.get(column.index()).add(consumed);
                consumedInputs.add(consumed);
            }
        }
        if (hasRemainingInputs(remainingInputs)) {
            return Optional.empty();
        }

        List<QueuedPackage> packages = new ArrayList<>();
        for (var column : encoded.get().columns()) {
            List<GenericStack> inputs = columnInputs.get(column.index());
            if (inputs.isEmpty()) {
                continue;
            }
            Optional<QueuedPackage> queued = buildPatternPackage(
                    column.color(),
                    column.marker(),
                    inputs,
                    sparseInputLayout(column.inputs()),
                    configuredCapacityProfile());
            if (queued.isEmpty()) {
                return Optional.empty();
            }
            packages.add(queued.get());
        }
        if (packages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ProviderPlan(packages, consumedInputs));
    }

    private Optional<ProviderPlan> planOrdinaryPatternPush(
            IPatternDetails patternDetails,
            KeyCounter[] inputHolder) {
        ItemStack definitionStack = patternDetails.getDefinition().toStack();
        if (!PatternDetailsHelper.isEncodedPattern(definitionStack)
                || AdvancedProcessingPatternDataStorage.hasData(definitionStack)
                || PackageCraftingPatternDataStorage.read(definitionStack).isPresent()) {
            return Optional.empty();
        }
        List<GenericStack> inputs = ordinaryPatternInputs(definitionStack, patternDetails);
        Map<AEKey, Long> expected = new LinkedHashMap<>();
        for (GenericStack input : inputs) {
            if (input == null || input.amount() <= 0) {
                continue;
            }
            expected.merge(input.what(), input.amount(), Long::sum);
        }
        if (inputs.isEmpty() || !expected.equals(aggregateInputs(inputHolder).orElse(Map.of()))) {
            return Optional.empty();
        }
        Optional<QueuedPackage> queued = buildPatternPackage(
                selectedColor,
                ordinaryPatternMarkerWithOverride(definitionStack, patternDetails),
                inputs,
                ordinaryPatternLayout(definitionStack, patternDetails),
                configuredCapacityProfile());
        return queued.map(value -> new ProviderPlan(List.of(value), inputs));
    }

    private boolean commitProviderPackages(List<QueuedPackage> packages) {
        if (packages.isEmpty()) {
            return false;
        }
        if (!pendingPackages.isEmpty()) {
            return false;
        }
        if (!hasOutputRoom()) {
            return false;
        }
        resetAutoExportBatch();
        QueuedPackage first = packages.get(0);
        scheduleComparatorSamplingNextTick();
        if (!insertOutputPackage(packageStack(first.color(), first.data())).isEmpty()) {
            return false;
        }
        if (packages.size() > 1) {
            pendingPackages.addAll(packages.subList(1, packages.size()));
        }
        setChanged();
        return true;
    }

    private static Optional<Map<AEKey, Long>> aggregateInputs(KeyCounter[] inputHolder) {
        Map<AEKey, Long> inputs = new HashMap<>();
        for (KeyCounter counter : inputHolder) {
            if (counter == null) {
                continue;
            }
            for (var entry : counter) {
                if (entry.getLongValue() <= 0) {
                    continue;
                }
                inputs.merge(entry.getKey(), entry.getLongValue(), Long::sum);
            }
        }
        return Optional.of(inputs);
    }

    private static boolean hasRemainingInputs(Map<AEKey, Long> inputs) {
        for (long amount : inputs.values()) {
            if (amount > 0) {
                return true;
            }
        }
        return false;
    }

    private static void consumePatternInputs(KeyCounter[] inputHolder, List<GenericStack> inputs) {
        for (GenericStack input : inputs) {
            long remaining = input.amount();
            for (KeyCounter counter : inputHolder) {
                if (counter == null || remaining <= 0) {
                    continue;
                }
                long available = counter.get(input.what());
                long extracted = Math.min(available, remaining);
                if (extracted > 0) {
                    counter.remove(input.what(), extracted);
                    remaining -= extracted;
                }
            }
        }
        for (KeyCounter counter : inputHolder) {
            if (counter != null) {
                counter.removeZeros();
            }
        }
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return new PatternContainerGroup(
                AEItemKey.of(new ItemStack(APBlocks.PACKAGE_ASSEMBLER.get())),
                getDisplayName(),
                List.of());
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, Direction ejectionDirection) {
        if (!acceptsPlans() || patternDetails == null || inputHolder == null) {
            return false;
        }

        ItemStack definitionStack = patternDetails.getDefinition().toStack();
        if (!patternFitsCapacity(definitionStack, patternDetails, configuredCapacityProfile())) {
            return false;
        }
        Optional<ProviderPlan> packageCraftingPlan = planPackageCraftingPush(definitionStack, inputHolder);
        if (packageCraftingPlan.isPresent()) {
            if (!beginProviderCraft(
                    packageCraftingPlan.get().packages(),
                    inputHolder,
                    packageCraftingPlan.get().consumedInputs())) {
                return false;
            }
            return true;
        }

        if (AdvancedProcessingPatternDataStorage.hasData(definitionStack)) {
            Optional<ProviderPlan> advancedPlan = planAdvancedProcessingPush(definitionStack, inputHolder);
            if (advancedPlan.isEmpty()
                    || !beginProviderCraft(
                            advancedPlan.get().packages(),
                            inputHolder,
                            advancedPlan.get().consumedInputs())) {
                return false;
            }
            return true;
        }

        if (PatternDetailsHelper.isEncodedPattern(definitionStack)) {
            Optional<ProviderPlan> ordinaryPlan = planOrdinaryPatternPush(patternDetails, inputHolder);
            if (ordinaryPlan.isEmpty()
                    || !beginProviderCraft(
                            ordinaryPlan.get().packages(),
                            inputHolder,
                            ordinaryPlan.get().consumedInputs())) {
                return false;
            }
            return true;
        }

        return false;
    }

    private boolean beginProviderCraft(
            List<QueuedPackage> packages,
            KeyCounter[] inputHolder,
            List<GenericStack> consumedInputs) {
        if (packages.isEmpty()) {
            return false;
        }
        if (!pendingPackages.isEmpty()) {
            return false;
        }
        if (!activePackages.isEmpty()) {
            return false;
        }
        if (!outputSlotsEmpty()) {
            return false;
        }
        consumePatternInputs(inputHolder, consumedInputs);
        if (level == null) {
            return commitProviderPackages(packages);
        }
        beginCrafting(packages);
        return true;
    }

    private Optional<ProviderPlan> planPackageCraftingPush(
            ItemStack definitionStack,
            KeyCounter[] inputHolder) {
        Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> encoded =
                PackageCraftingPatternDataStorage.read(definitionStack);
        if (encoded.isEmpty()) {
            return Optional.empty();
        }
        PackageData encodedData = encoded.get().data();
        if (!configuredCapacityProfile().fits(encodedData.usedUnits(), encodedData.usedTypes())) {
            return Optional.empty();
        }
        Optional<Map<AEKey, Long>> availableInputs = aggregateInputs(inputHolder);
        if (availableInputs.isEmpty()) {
            return Optional.empty();
        }

        Map<AEKey, Long> remainingInputs = new HashMap<>(availableInputs.get());
        Map<AEKey, Long> consumedByKey = new LinkedHashMap<>();
        for (GenericStack stack : encoded.get().denseInputs()) {
            long available = remainingInputs.getOrDefault(stack.what(), 0L);
            if (available < stack.amount()) {
                return Optional.empty();
            }
            long remaining = available - stack.amount();
            if (remaining == 0) {
                remainingInputs.remove(stack.what());
            } else {
                remainingInputs.put(stack.what(), remaining);
            }
            consumedByKey.merge(stack.what(), stack.amount(), Long::sum);
        }
        if (hasRemainingInputs(remainingInputs)) {
            return Optional.empty();
        }

        List<GenericStack> consumedInputs = consumedByKey.entrySet().stream()
                .map(entry -> new GenericStack(entry.getKey(), entry.getValue()))
                .toList();
        return Optional.of(new ProviderPlan(
                List.of(new QueuedPackage(encoded.get().color(), encoded.get().data())),
                consumedInputs));
    }

    @Override
    public boolean acceptsPlans() {
        return outputSlotsEmpty()
                && !hasMenuInputs()
                && pendingPackages.isEmpty()
                && activePackages.isEmpty()
                && items.getStackInSlot(SLOT_PATTERN).isEmpty();
    }

    private Optional<MEStorage> findOutputMEStorage() {
        if (!getMainNode().isActive() || !getMainNode().hasGridBooted()) {
            return Optional.empty();
        }
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return Optional.empty();
        }
        return Optional.of(grid.getStorageService().getInventory());
    }

    private Optional<IItemHandler> findOutputItemHandler(Direction targetDirection) {
        BlockEntity targetBlockEntity = level.getBlockEntity(worldPosition.relative(targetDirection));
        if (targetBlockEntity == null) {
            return Optional.empty();
        }
        LazyOptional<IItemHandler> capability = targetBlockEntity.getCapability(
                ForgeCapabilities.ITEM_HANDLER,
                targetDirection.getOpposite());
        return resolveOptionalCapability(capability);
    }

    private static <T> Optional<T> resolveOptionalCapability(LazyOptional<T> capability) {
        try {
            return capability.resolve();
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, net.minecraft.core.Direction side) {
        if (capability == Capabilities.CRAFTING_MACHINE) {
            return Capabilities.CRAFTING_MACHINE.orEmpty(capability, craftingMachine);
        }
        if (capability == Capabilities.GENERIC_INTERNAL_INV) {
            return genericInventory.cast();
        }
        if (capability == Capabilities.STORAGE) {
            return storage.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        genericInventory.invalidate();
        storage.invalidate();
        craftingMachine.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        genericInventory = createGenericInventoryCapability();
        storage = createStorageCapability();
        craftingMachine = createCraftingMachineCapability();
    }

    private LazyOptional<GenericInternalInventory> createGenericInventoryCapability() {
        return LazyOptional.of(() -> externalGenericInventory);
    }

    private void refreshGenericInventoryCapability() {
        int slotCount = externalSlotCount();
        if (externalGenericInventory != null && externalGenericInventory.size() == slotCount) {
            return;
        }
        genericInventory.invalidate();
        externalGenericInventory = new ExternalGenericInventory(slotCount);
        genericInventory = createGenericInventoryCapability();
    }

    private LazyOptional<MEStorage> createStorageCapability() {
        return LazyOptional.of(() -> externalStorage);
    }

    private LazyOptional<ICraftingMachine> createCraftingMachineCapability() {
        return LazyOptional.of(() -> this);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ITEMS_TAG, items.serializeNBT());
        upgrades.writeToNBT(tag, UPGRADES_TAG);
        tag.put(MENU_INPUTS_TAG, saveMenuInputs());
        tag.put(PENDING_PACKAGES_TAG, savePendingPackages());
        tag.put(ACTIVE_PACKAGES_TAG, saveQueuedPackages(activePackages));
        markerFilter.writeToChildTag(tag, MARKER_FILTER_TAG);
        tag.putString(SELECTED_COLOR_TAG, selectedColor.id());
        if (!activeMenuPattern.isEmpty()) {
            tag.put(ACTIVE_MENU_PATTERN_TAG, activeMenuPattern.save(new CompoundTag()));
        }
        tag.putInt(CRAFT_PROGRESS_TAG, craftingProgress);
        tag.putString(OUTPUT_MODE_TAG, outputMode.id());
        tag.putBoolean(BLOCKING_MODE_TAG, blockingMode);
        tag.putBoolean(AUTO_EXPORT_BATCH_ACTIVE_TAG, autoExportBatchActive);
        if (autoExportBatchActive) {
            tag.putString(AUTO_EXPORT_BATCH_MODE_TAG, autoExportBatchMode.id());
            if (autoExportBatchDirection != null) {
                tag.putInt(AUTO_EXPORT_BATCH_DIRECTION_TAG, autoExportBatchDirection.get3DDataValue());
            }
        }
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        if (tag.contains(ITEMS_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            items.deserializeNBT(tag.getCompound(ITEMS_TAG));
        }
        upgrades.readFromNBT(tag, UPGRADES_TAG);
        markerFilter.readFromChildTag(tag, MARKER_FILTER_TAG);
        selectedColor = PackageColor.byId(tag.getString(SELECTED_COLOR_TAG)).orElse(PackageColor.FLUIX);
        loadMenuInputs(tag);
        outputMode = OutputMode.byId(tag.getString(OUTPUT_MODE_TAG)).orElse(OutputMode.ME_NETWORK);
        blockingMode = tag.getBoolean(BLOCKING_MODE_TAG);
        craftingProgress = Math.max(0, Math.min(MAX_CRAFT_PROGRESS, tag.getInt(CRAFT_PROGRESS_TAG)));
        loadQueuedPackages(tag, ACTIVE_PACKAGES_TAG, activePackages);
        activeMenuPattern = tag.contains(ACTIVE_MENU_PATTERN_TAG, Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound(ACTIVE_MENU_PATTERN_TAG))
                : ItemStack.EMPTY;
        if (activePackages.isEmpty()) {
            activeMenuPattern = ItemStack.EMPTY;
        }
        loadPendingPackages(tag);
        promoteNextOutput();
        autoExportBatchActive = tag.getBoolean(AUTO_EXPORT_BATCH_ACTIVE_TAG);
        autoExportBatchMode = OutputMode.byId(tag.getString(AUTO_EXPORT_BATCH_MODE_TAG))
                .filter(mode -> mode != OutputMode.NONE)
                .orElse(OutputMode.NONE);
        autoExportBatchDirection = tag.contains(AUTO_EXPORT_BATCH_DIRECTION_TAG, Tag.TAG_INT)
                ? Direction.from3DDataValue(tag.getInt(AUTO_EXPORT_BATCH_DIRECTION_TAG))
                : null;
        if (!autoExportBatchActive
                || autoExportBatchMode == OutputMode.NONE
                || completedOutputCount() == 0
                || (autoExportBatchMode == OutputMode.ADJACENT_BLOCK && autoExportBatchDirection == null)
                || (outputMode != OutputMode.NONE && outputMode != autoExportBatchMode)) {
            autoExportBatchActive = false;
            autoExportBatchMode = OutputMode.NONE;
            autoExportBatchDirection = null;
        }
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        for (MenuInputEntry entry : menuInputs) {
            if (entry != null) {
                dropMenuInput(level, pos, entry);
            }
        }
        for (ItemStack stack : upgrades) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        for (QueuedPackage queuedPackage : pendingPackages) {
            Containers.dropItemStack(
                    level,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    packageStack(queuedPackage.color(), queuedPackage.data()));
        }
        if (activeMenuPattern.isEmpty()) {
            for (QueuedPackage queuedPackage : activePackages) {
                Containers.dropItemStack(
                        level,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        packageStack(queuedPackage.color(), queuedPackage.data()));
            }
        }
    }

    private ListTag saveMenuInputs() {
        ListTag list = new ListTag();
        for (int slot = 0; slot < menuInputs.length; slot++) {
            MenuInputEntry entry = menuInputs[slot];
            if (entry == null || entry.amount() <= 0) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putInt(MENU_INPUT_SLOT_TAG, slot);
            tag.put(MENU_INPUT_STACK_TAG, GenericStack.writeTag(new GenericStack(entry.key(), entry.amount())));
            tag.putLong(MENU_INPUT_AMOUNT_TAG, entry.amount());
            list.add(tag);
        }
        return list;
    }

    private void loadMenuInputs(CompoundTag tag) {
        menuInputs = new MenuInputEntry[0];
        if (!tag.contains(MENU_INPUTS_TAG, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList(MENU_INPUTS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entryTag = list.getCompound(index);
            int slot = entryTag.getInt(MENU_INPUT_SLOT_TAG);
            if (slot < 0 || slot >= MAX_MENU_INPUT_SLOT_COUNT
                    || !entryTag.contains(MENU_INPUT_STACK_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            GenericStack stack = GenericStack.readTag(entryTag.getCompound(MENU_INPUT_STACK_TAG));
            long amount = stack == null ? 0 : stack.amount();
            if (stack != null && amount > 0) {
                ensureMenuInputStorageCapacity(slot + 1);
                menuInputs[slot] = new MenuInputEntry(stack.what(), amount);
            }
        }
        trimMenuInputStorage();
        refreshGenericInventoryCapability();
    }

    private static void dropMenuInput(Level level, BlockPos pos, MenuInputEntry entry) {
        List<ItemStack> drops = new ArrayList<>();
        entry.key().addDrops(entry.amount(), drops, level, pos);
        for (ItemStack drop : drops) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
        }
    }

    public enum AssemblyResult {
        ASSEMBLED,
        NO_CONTENTS,
        OUTPUT_BLOCKED,
        PATTERN_MISMATCH,
        SOURCE_CHANGED
    }

    public enum OutputMode {
        ME_NETWORK("me_network"),
        ADJACENT_BLOCK("adjacent_block"),
        NONE("none");

        private final String id;

        OutputMode(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public OutputMode next() {
            OutputMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static Optional<OutputMode> byId(String id) {
            for (OutputMode mode : values()) {
                if (mode.id.equals(id)) {
                    return Optional.of(mode);
                }
            }
            return Optional.empty();
        }
    }

    private boolean commitMenuExtractions(List<MenuInputExtraction> extractions) {
        MenuInputEntry[] updated = Arrays.copyOf(menuInputs, menuInputs.length);
        List<GenericStack> filters = menuInputFilters();
        for (MenuInputExtraction extraction : extractions) {
            if (extraction.slot() < 0
                    || extraction.slot() >= updated.length
                    || extraction.slot() >= filters.size()
                    || extraction.amount() <= 0) {
                return false;
            }
            MenuInputEntry entry = updated[extraction.slot()];
            GenericStack filter = filters.get(extraction.slot());
            if (entry == null
                    || filter == null
                    || !filter.what().equals(entry.key())
                    || entry.amount() < extraction.amount()) {
                return false;
            }
            long remaining = entry.amount() - extraction.amount();
            updated[extraction.slot()] = remaining <= 0
                    ? null
                    : new MenuInputEntry(entry.key(), remaining);
        }
        menuInputs = updated;
        trimMenuInputStorage();
        setChanged();
        return true;
    }

    private static MenuInputContents menuInputContents(MenuInputEntry[] inputs) {
        List<GenericStack> orderedContents = new ArrayList<>();
        List<PackageData> sourcePackages = new ArrayList<>();
        for (MenuInputEntry entry : inputs) {
            if (entry == null || entry.amount() <= 0) {
                continue;
            }
            Optional<PackageData> packageData = packageData(entry);
            if (packageData.isPresent()) {
                for (long count = 0; count < entry.amount(); count++) {
                    sourcePackages.add(packageData.get());
                    orderedContents.addAll(packageData.get().contents());
                }
                continue;
            }
            orderedContents.add(new GenericStack(entry.key(), entry.amount()));
        }
        return new MenuInputContents(List.copyOf(orderedContents), List.copyOf(sourcePackages));
    }

    private static Optional<PackageData> packageData(MenuInputEntry entry) {
        return entry.key() instanceof AEItemKey itemKey
                ? PackageDataStorage.read(itemKey.toStack(1))
                : Optional.empty();
    }

    private static ItemStack displayStack(GenericStack stack) {
        ItemStack display = stack.what().wrapForDisplayOrFilter();
        if (display.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int count = (int) Math.max(1L, Math.min(stack.amount(), Integer.MAX_VALUE));
        display.setCount(count);
        return display;
    }

    private ListTag savePendingPackages() {
        return saveQueuedPackages(pendingPackages);
    }

    private ListTag saveQueuedPackages(List<QueuedPackage> packages) {
        ListTag list = new ListTag();
        for (QueuedPackage queuedPackage : packages) {
            CompoundTag tag = new CompoundTag();
            tag.putString(PENDING_COLOR_TAG, queuedPackage.color().id());
            tag.put(PENDING_DATA_TAG, PackageDataStorage.writeTag(queuedPackage.data()));
            list.add(tag);
        }
        return list;
    }

    private void loadPendingPackages(CompoundTag tag) {
        loadQueuedPackages(tag, PENDING_PACKAGES_TAG, pendingPackages);
    }

    private void loadQueuedPackages(CompoundTag tag, String key, List<QueuedPackage> target) {
        target.clear();
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return;
        }
        for (Tag element : tag.getList(key, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag pendingTag)) {
                continue;
            }
            Optional<PackageColor> color = PackageColor.byId(pendingTag.getString(PENDING_COLOR_TAG));
            if (color.isEmpty() || !pendingTag.contains(PENDING_DATA_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            Optional<PackageData> data = PackageDataStorage.readTag(
                    pendingTag.getCompound(PENDING_DATA_TAG),
                    color.get());
            data.ifPresent(packageData -> target.add(new QueuedPackage(color.get(), packageData)));
        }
    }

    private ItemStack packageStack(PackageColor color, PackageData data) {
        ItemStack stack = new ItemStack(APItems.packageItems().get(color).get());
        PackageDataStorage.write(stack, data);
        return stack;
    }

    public PackageCapacityProfile configuredCapacityProfile() {
        return PackageCapacityProfile.fromStorageComponent(items.getStackInSlot(SLOT_CAPACITY))
                .orElse(PackageCapacityProfile.DEFAULT);
    }

    private void onUpgradesChanged() {
        setChanged();
    }

    private ItemStack extractPrimaryOutput(int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = items.extractItem(SLOT_OUTPUT, 1, simulate);
        if (!simulate && !extracted.isEmpty()) {
            promoteNextOutput();
            finishAutoExportBatchIfEmpty();
            setChanged();
        }
        return extracted;
    }

    private void promoteNextOutput() {
        if (!items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            return;
        }
        for (int outputIndex = 1; outputIndex < OUTPUT_SLOT_COUNT; outputIndex++) {
            int slot = outputHandlerSlot(outputIndex);
            ItemStack next = items.extractItem(slot, 1, false);
            if (!next.isEmpty()) {
                items.setStackInSlot(SLOT_OUTPUT, next);
                return;
            }
        }
        if (pendingPackages.isEmpty()) {
            return;
        }
        QueuedPackage next = pendingPackages.remove(0);
        ItemStack nextStack = packageStack(next.color(), next.data());
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, nextStack, false);
        if (!remainder.isEmpty()) {
            pendingPackages.add(0, next);
            return;
        }
    }

    private GenericStack externalOutputStack() {
        ItemStack output = items.getStackInSlot(SLOT_OUTPUT);
        AEItemKey key = AEItemKey.of(output);
        return key == null ? null : new GenericStack(key, output.getCount());
    }

    private long extractExternalOutput(AEKey key, long amount, Actionable mode) {
        if (key == null || amount <= 0) {
            return 0;
        }
        GenericStack output = externalOutputStack();
        if (output == null || !output.what().equals(key)) {
            return 0;
        }
        long extracted = Math.min(1, Math.min(amount, output.amount()));
        if (extracted > 0 && mode == Actionable.MODULATE) {
            extractPrimaryOutput((int) extracted, false);
        }
        return extracted;
    }

    private final class ExternalStorage implements MEStorage {
        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            MEStorage.checkPreconditions(what, amount, mode, source);
            return insertMenuInput(-1, what, amount, mode);
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            MEStorage.checkPreconditions(what, amount, mode, source);
            return extractExternalOutput(what, amount, mode);
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            GenericStack output = externalOutputStack();
            if (output != null) {
                out.add(output.what(), output.amount());
            }
        }

        @Override
        public Component getDescription() {
            return Component.translatable("block.appliedpackaging.package_assembler");
        }
    }

    private final class ExternalGenericInventory implements GenericInternalInventory {
        private final int slotCount;
        private final int outputSlot;
        private boolean batching;
        private boolean changedWhileBatching;

        private ExternalGenericInventory(int slotCount) {
            this.slotCount = slotCount;
            this.outputSlot = slotCount - 1;
        }

        @Override
        public int size() {
            return slotCount;
        }

        @Override
        public GenericStack getStack(int slot) {
            if (slot >= 0 && slot < outputSlot) {
                MenuInputEntry entry = menuInput(slot);
                return entry == null ? null : new GenericStack(entry.key(), entry.amount());
            }
            return slot == outputSlot ? externalOutputStack() : null;
        }

        @Override
        public AEKey getKey(int slot) {
            GenericStack stack = getStack(slot);
            return stack == null ? null : stack.what();
        }

        @Override
        public long getAmount(int slot) {
            GenericStack stack = getStack(slot);
            return stack == null ? 0 : stack.amount();
        }

        @Override
        public long getMaxAmount(AEKey key) {
            long maximum = 0;
            for (GenericStack filter : menuInputFilters()) {
                if (filter != null && filter.what().equals(key)) {
                    maximum = Math.max(maximum, filter.amount());
                }
            }
            return Math.max(1, maximum);
        }

        @Override
        public long getCapacity(AEKeyType keyType) {
            long maximum = 0;
            for (GenericStack filter : menuInputFilters()) {
                if (filter != null && filter.what().getType() == keyType) {
                    maximum = Math.max(maximum, filter.amount());
                }
            }
            return Math.max(1, maximum);
        }

        @Override
        public boolean canInsert() {
            return true;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public void setStack(int slot, GenericStack newStack) {
            // External automation must use insert/extract so pattern limits and ordered-output semantics cannot be bypassed.
        }

        @Override
        public boolean isAllowed(AEKey what) {
            if (what == null || !isPatternCapacityValid()) {
                return false;
            }
            for (GenericStack filter : menuInputFilters()) {
                if (filter != null && filter.what().equals(what)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public long insert(int slot, AEKey what, long amount, Actionable mode) {
            long inserted = insertMenuInputIntoSlot(slot, what, amount, mode);
            if (inserted > 0 && mode == Actionable.MODULATE) {
                onChange();
            }
            return inserted;
        }

        @Override
        public long extract(int slot, AEKey what, long amount, Actionable mode) {
            if (slot != outputSlot) {
                return 0;
            }
            long extracted = extractExternalOutput(what, amount, mode);
            if (extracted > 0 && mode == Actionable.MODULATE) {
                onChange();
            }
            return extracted;
        }

        @Override
        public void beginBatch() {
            if (batching) {
                throw new IllegalStateException("Generic assembler inventory batch already active");
            }
            batching = true;
        }

        @Override
        public void endBatch() {
            if (!batching) {
                throw new IllegalStateException("Generic assembler inventory batch not active");
            }
            batching = false;
            if (changedWhileBatching) {
                changedWhileBatching = false;
                setChanged();
            }
        }

        @Override
        public void endBatchSuppressed() {
            if (!batching) {
                throw new IllegalStateException("Generic assembler inventory batch not active");
            }
            batching = false;
            changedWhileBatching = false;
        }

        @Override
        public void onChange() {
            if (batching) {
                changedWhileBatching = true;
            } else {
                setChanged();
            }
        }
    }

    private record AssemblyPlan(
            PackageColor color,
            PackageData data,
            List<MenuInputExtraction> menuExtractions) {
    }

    private record QueuedPackage(PackageColor color, PackageData data) {
    }

    private record AdjacentItemTarget(Direction direction, IItemHandler handler) {
    }

    private record ProviderPlan(List<QueuedPackage> packages, List<GenericStack> consumedInputs) {
        private ProviderPlan {
            packages = List.copyOf(packages);
            consumedInputs = List.copyOf(consumedInputs);
        }
    }

    private record MenuBatchPlan(List<QueuedPackage> packages, List<MenuInputExtraction> menuExtractions) {
        private MenuBatchPlan {
            packages = List.copyOf(packages);
            menuExtractions = List.copyOf(menuExtractions);
        }
    }

    private record MenuCraftPlan(List<QueuedPackage> packages, List<MenuInputExtraction> menuExtractions) {
        private MenuCraftPlan {
            packages = List.copyOf(packages);
            menuExtractions = List.copyOf(menuExtractions);
        }
    }

    private record AssemblyAttempt(Optional<AssemblyPlan> plan, AssemblyResult failure) {
        private static AssemblyAttempt planned(AssemblyPlan plan) {
            return new AssemblyAttempt(Optional.of(plan), AssemblyResult.ASSEMBLED);
        }

        private static AssemblyAttempt failed(AssemblyResult failure) {
            return new AssemblyAttempt(Optional.empty(), failure);
        }
    }

    private record ConsumedPatternInput(KeyCounter source, AEKey key, long amount) {
    }

    private record PatternProviderInput(List<ItemStack> stacks, List<ConsumedPatternInput> consumedInputs) {
        private static Optional<PatternProviderInput> create(KeyCounter[] inputHolder) {
            List<ItemStack> stacks = new ArrayList<>();
            List<ConsumedPatternInput> consumedInputs = new ArrayList<>();
            for (KeyCounter counter : inputHolder) {
                if (counter == null) {
                    continue;
                }
                for (var entry : counter) {
                    if (entry.getLongValue() <= 0) {
                        continue;
                    }
                    if (!AEItemKey.is(entry.getKey())) {
                        return Optional.empty();
                    }
                    AEItemKey key = (AEItemKey) entry.getKey();
                    long remaining = entry.getLongValue();
                    consumedInputs.add(new ConsumedPatternInput(counter, key, remaining));
                    while (remaining > 0) {
                        int amount = (int) Math.min(remaining, key.getMaxStackSize());
                        stacks.add(key.toStack(amount));
                        remaining -= amount;
                    }
                }
            }
            if (stacks.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new PatternProviderInput(List.copyOf(stacks), List.copyOf(consumedInputs)));
        }

        private boolean insertInto(IItemHandler target, boolean simulate) {
            for (ItemStack stack : stacks) {
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, stack.copy(), simulate);
                if (!remainder.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        private void consume() {
            for (ConsumedPatternInput consumed : consumedInputs) {
                consumed.source().remove(consumed.key(), consumed.amount());
                consumed.source().removeZeros();
            }
        }
    }

    private record MenuInputEntry(AEKey key, long amount) {
        private MenuInputEntry {
            if (key == null || amount <= 0) {
                throw new IllegalArgumentException("Menu input entries require a key and positive amount");
            }
        }

        private ItemStack displayStack() {
            ItemStack display = key.wrapForDisplayOrFilter();
            if (display.isEmpty()) {
                return ItemStack.EMPTY;
            }
            display.setCount((int) Math.max(1L, Math.min(amount, Integer.MAX_VALUE)));
            return display;
        }
    }

    private record MenuInputExtraction(int slot, long amount) {
    }

    private record AdvancedMenuInputFilter(int column, GenericStack stack) {
        private AdvancedMenuInputFilter {
            if (column < 0 || stack == null || stack.amount() <= 0) {
                throw new IllegalArgumentException("Invalid advanced menu input filter");
            }
        }
    }

    private record MenuInputContents(List<GenericStack> orderedContents, List<PackageData> sourcePackages) {
        private MenuInputContents {
            orderedContents = List.copyOf(orderedContents);
            sourcePackages = List.copyOf(sourcePackages);
        }

        private boolean isEmpty() {
            return orderedContents.isEmpty();
        }
    }

    private record MenuExactExtraction(
            boolean matched,
            List<GenericStack> looseContents,
            List<PackageData> sourcePackages,
            List<MenuInputExtraction> extractions) {
        private MenuExactExtraction {
            looseContents = List.copyOf(looseContents);
            sourcePackages = List.copyOf(sourcePackages);
            extractions = List.copyOf(extractions);
        }
    }
}

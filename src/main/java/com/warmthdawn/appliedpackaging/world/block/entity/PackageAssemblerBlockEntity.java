package com.warmthdawn.appliedpackaging.world.block.entity;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.capabilities.Capabilities;
import appeng.core.definitions.AEItems;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackagePlan;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackageTransactions;
import com.warmthdawn.appliedpackaging.core.package_data.ColoredProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackagedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanBuilder;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanResult;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PackageAssemblerBlockEntity extends AENetworkBlockEntity
        implements InventoryDroppingBlockEntity, MenuProvider, ICraftingMachine, IUpgradeableObject {
    public static final int LEGACY_INPUT_SLOT_COUNT = 9;
    public static final int INPUT_SLOT_COUNT = LEGACY_INPUT_SLOT_COUNT;
    public static final int OUTPUT_SLOT_COUNT = 17;
    public static final int MENU_INPUT_COLUMNS = 4;
    public static final int MENU_INPUT_SLOT_COUNT = OUTPUT_SLOT_COUNT * MENU_INPUT_COLUMNS;
    public static final int SLOT_PATTERN = 9;
    public static final int SLOT_OUTPUT = 10;
    public static final int SLOT_CAPACITY = 11;
    public static final int EXTRA_OUTPUT_SLOT_START = 12;
    public static final int SLOT_MARKER = EXTRA_OUTPUT_SLOT_START + OUTPUT_SLOT_COUNT - 1;
    public static final int UPGRADE_SLOT_COUNT = 5;
    public static final int MAX_CRAFT_PROGRESS = 100;
    private static final int SLOT_COUNT = SLOT_MARKER + 1;
    private static final String ITEMS_TAG = "items";
    private static final String UPGRADES_TAG = "upgrades";
    private static final String MENU_INPUTS_TAG = "menu_inputs";
    private static final String MENU_INPUT_SLOT_TAG = "slot";
    private static final String MENU_INPUT_STACK_TAG = "stack";
    private static final String MENU_INPUT_AMOUNT_TAG = "amount";
    private static final String PENDING_PACKAGES_TAG = "pending_packages";
    private static final String PENDING_COLOR_TAG = "color";
    private static final String PENDING_DATA_TAG = "data";
    private static final String AUTO_EXPORT_TAG = "auto_export";
    private static final String OUTPUT_MODE_TAG = "output_mode";
    private static final String SELECTED_COLOR_TAG = "selected_color";
    private static final String CRAFT_PROGRESS_TAG = "craft_progress";
    private static final String ACTIVE_PACKAGES_TAG = "active_packages";

    private final MenuInputEntry[] menuInputs = new MenuInputEntry[MENU_INPUT_SLOT_COUNT];
    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (isOutputSlot(slot)) {
                return stack.getItem() instanceof PackageItem;
            }
            if (slot == SLOT_PATTERN) {
                return isPatternSlotItem(stack);
            }
            if (slot == SLOT_CAPACITY) {
                return MePackagerBlockEntity.capacityProfileFromItem(stack).isPresent();
            }
            if (slot == SLOT_MARKER) {
                return isMarkerSlotItem(stack);
            }
            if (slot >= 0 && slot < LEGACY_INPUT_SLOT_COUNT) {
                return isLegacyInputSlotValid(slot, stack);
            }
            if (stack.getItem() instanceof PackageItem) {
                return PackageDataStorage.read(stack).isPresent();
            }
            return true;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (isOutputSlot(slot) || slot == SLOT_CAPACITY || slot == SLOT_PATTERN || slot == SLOT_MARKER) {
                return 1;
            }
            return super.getSlotLimit(slot);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == SLOT_PATTERN && activePackages.isEmpty()) {
                craftingProgress = 0;
            }
            setChanged();
        }
    };
    private final IItemHandler orderedOutputItems = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? items.getStackInSlot(SLOT_OUTPUT) : ItemStack.EMPTY;
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
    private final RangedWrapper inputView = new RangedWrapper(items, 0, LEGACY_INPUT_SLOT_COUNT);
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            APBlocks.PACKAGE_ASSEMBLER.get(),
            UPGRADE_SLOT_COUNT,
            this::onUpgradesChanged);
    private final ExternalItemHandler externalItemHandler = new ExternalItemHandler();
    private LazyOptional<IItemHandler> itemHandler = createItemHandlerCapability();
    private LazyOptional<ICraftingMachine> craftingMachine = createCraftingMachineCapability();
    private final List<QueuedPackage> activePackages = new ArrayList<>();
    private final List<QueuedPackage> pendingPackages = new ArrayList<>();
    private PackageColor selectedColor = PackageColor.FLUIX;
    private OutputMode outputMode = OutputMode.ME_NETWORK;
    private int craftingProgress;

    public PackageAssemblerBlockEntity(BlockPos pos, BlockState blockState) {
        super(APBlockEntities.PACKAGE_ASSEMBLER.get(), pos, blockState);
        getMainNode().setIdlePowerUsage(0.0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PackageAssemblerBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        exportOutputIfEnabled();
        if (!pendingPackages.isEmpty()) {
            outputPendingPackage();
            exportOutputIfEnabled();
            return;
        }
        if (!activePackages.isEmpty()) {
            advanceCraftingProgress();
            exportOutputIfEnabled();
            return;
        }
        if (tryStartAssembly() == AssemblyResult.ASSEMBLED) {
            exportOutputIfEnabled();
        }
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public IItemHandler getOrderedOutputItems() {
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

    public static boolean isPatternSlotItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (PackagePatternDataStorage.read(stack).isPresent()
                || PackagedProcessingPatternDataStorage.read(stack).isPresent()
                || PackageCraftingPatternDataStorage.read(stack).isPresent()
                || ColoredProcessingPatternDataStorage.hasData(stack)
                || AdvancedProcessingPatternDataStorage.hasData(stack)
                || isAe2ProcessingPattern(stack)) {
            return true;
        }
        return false;
    }

    private static boolean isAe2ProcessingPattern(ItemStack stack) {
        return !stack.isEmpty() && AEItems.PROCESSING_PATTERN.isSameAs(stack);
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

    public ItemStack menuInputFilterDisplay(int slot) {
        List<GenericStack> filters = menuInputFilters();
        if (slot < 0 || slot >= filters.size() || filters.get(slot) == null) {
            return ItemStack.EMPTY;
        }
        return displayStack(filters.get(slot));
    }

    public int menuInputFilterAmountForDisplay(int slot) {
        List<GenericStack> filters = menuInputFilters();
        if (slot < 0 || slot >= filters.size() || filters.get(slot) == null) {
            return 0;
        }
        long amount = filters.get(slot).amount();
        return amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
    }

    public boolean hasMenuInput(int slot) {
        return menuInput(slot) != null;
    }

    public int insertMenuInput(int preferredSlot, ItemStack stack, int requestedAmount, boolean simulate) {
        if (stack.isEmpty() || requestedAmount <= 0) {
            return 0;
        }
        ItemStack keyStack = stack.copy();
        keyStack.setCount(1);
        int amount = Math.min(requestedAmount, stack.getCount());
        int inserted = 0;

        if (preferredSlot >= 0 && preferredSlot < MENU_INPUT_SLOT_COUNT) {
            inserted = insertMenuInputIntoSlot(preferredSlot, keyStack, amount, simulate);
        } else {
            for (int slot = 0; slot < MENU_INPUT_SLOT_COUNT && inserted < amount; slot++) {
                MenuInputEntry entry = menuInputs[slot];
                if (entry != null && ItemStack.isSameItemSameTags(entry.stack(), keyStack)) {
                    inserted += insertMenuInputIntoSlot(slot, keyStack, amount - inserted, simulate);
                }
            }
            for (int slot = 0; slot < MENU_INPUT_SLOT_COUNT && inserted < amount; slot++) {
                if (menuInputs[slot] == null) {
                    inserted += insertMenuInputIntoSlot(slot, keyStack, amount - inserted, simulate);
                }
            }
        }

        if (inserted > 0 && !simulate) {
            setChanged();
        }
        return inserted;
    }

    public ItemStack extractMenuInput(int slot, int requestedAmount, boolean simulate) {
        MenuInputEntry entry = menuInput(slot);
        if (entry == null || requestedAmount <= 0) {
            return ItemStack.EMPTY;
        }
        int amount = (int) Math.min(Math.min(entry.amount(), requestedAmount), entry.stack().getMaxStackSize());
        ItemStack extracted = entry.stack().copy();
        extracted.setCount(amount);
        if (!simulate) {
            long remaining = entry.amount() - amount;
            menuInputs[slot] = remaining <= 0 ? null : new MenuInputEntry(entry.stack(), remaining);
            setChanged();
        }
        return extracted;
    }

    private int insertMenuInputIntoSlot(int slot, ItemStack keyStack, int requestedAmount, boolean simulate) {
        if (requestedAmount <= 0) {
            return 0;
        }
        MenuInputEntry current = menuInputs[slot];
        if (current != null && !ItemStack.isSameItemSameTags(current.stack(), keyStack)) {
            return 0;
        }
        int low = 0;
        int high = requestedAmount;
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (canInsertMenuInputIntoSlot(slot, keyStack, mid)) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        if (low > 0 && !simulate) {
            long amount = (current == null ? 0 : current.amount()) + low;
            menuInputs[slot] = new MenuInputEntry(keyStack, amount);
        }
        return low;
    }

    private boolean canInsertMenuInputIntoSlot(int slot, ItemStack keyStack, int amount) {
        List<GenericStack> filters = menuInputFilters();
        if (slot < 0 || slot >= filters.size()) {
            return false;
        }
        MenuInputEntry current = menuInputs[slot];
        long newAmount = (current == null ? 0 : current.amount()) + amount;
        if (newAmount <= 0) {
            return false;
        }
        MenuInputEntry[] trial = copyMenuInputs();
        trial[slot] = new MenuInputEntry(keyStack, newAmount);
        return menuInputsMatchFilters(trial, filters);
    }

    private MenuInputEntry[] copyMenuInputs() {
        return Arrays.copyOf(menuInputs, menuInputs.length);
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
        Arrays.fill(menuInputs, null);
        setChanged();
    }

    private List<GenericStack> menuInputFilters() {
        ItemStack patternStack = items.getStackInSlot(SLOT_PATTERN);
        if (!isPatternSlotItem(patternStack)) {
            return List.of();
        }
        Optional<AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern> advancedPattern =
                AdvancedProcessingPatternDataStorage.read(patternStack);
        if (advancedPattern.isPresent()) {
            List<GenericStack> sparseInputs = ColoredProcessingPatternDataStorage.readSparseInputs(patternStack);
            int size = advancedPattern.get().activeColumnCount()
                    * AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
            List<GenericStack> filters = new ArrayList<>(size);
            for (int slot = 0; slot < size; slot++) {
                filters.add(slot < sparseInputs.size() ? sparseInputs.get(slot) : null);
            }
            return filters;
        }
        Optional<PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern> processingPattern =
                PackagedProcessingPatternDataStorage.read(patternStack);
        if (processingPattern.isPresent()) {
            Map<AEKey, Long> merged = new LinkedHashMap<>();
            for (PackageData data : processingPattern.get().packages()) {
                for (GenericStack stack : data.contents()) {
                    merged.merge(stack.what(), stack.amount(), Long::sum);
                }
            }
            return genericStacksFromMap(merged);
        }
        Optional<PackagePatternDataStorage.EncodedPackagePattern> packagePattern =
                PackagePatternDataStorage.read(patternStack);
        if (packagePattern.isPresent()) {
            return packagePattern.get().data().contents();
        }
        Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> packageCraftingPattern =
                PackageCraftingPatternDataStorage.read(patternStack);
        if (packageCraftingPattern.isPresent()) {
            return packageCraftingPattern.get().denseInputs();
        }
        if (ColoredProcessingPatternDataStorage.hasData(patternStack) || isAe2ProcessingPattern(patternStack)) {
            return decodedPatternInputs(patternStack);
        }
        return List.of();
    }

    public boolean isMenuInputSlotEnabled(int slot) {
        if (slot < 0 || slot >= MENU_INPUT_SLOT_COUNT) {
            return false;
        }
        List<GenericStack> filters = menuInputFilters();
        return menuInput(slot) != null
                || (slot < filters.size() && filters.get(slot) != null);
    }

    public boolean isMenuInputSlotValid(int slot) {
        MenuInputEntry entry = menuInput(slot);
        if (entry == null) {
            return true;
        }
        List<GenericStack> filters = menuInputFilters();
        if (slot < 0 || slot >= filters.size()) {
            return false;
        }
        GenericStack filter = filters.get(slot);
        return filter != null
                && filter.what().equals(AEItemKey.of(entry.stack()))
                && entry.amount() <= filter.amount();
    }

    private List<GenericStack> decodedPatternInputs(ItemStack patternStack) {
        if (level == null || !PatternDetailsHelper.isEncodedPattern(patternStack)) {
            return List.of();
        }
        var details = PatternDetailsHelper.decodePattern(patternStack, level, false);
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
                    || !filter.what().equals(AEItemKey.of(entry.stack()))
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
        for (int slot = 0; slot < inputs.length; slot++) {
            MenuInputEntry entry = inputs[slot];
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
            if (!filter.what().equals(AEItemKey.of(entry.stack())) || entry.amount() != filter.amount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean packageContentsExactlyMatch(PackageData data, List<GenericStack> filters) {
        Map<AEKey, Long> expected = new LinkedHashMap<>();
        for (GenericStack filter : filters) {
            if (filter == null) {
                continue;
            }
            expected.merge(filter.what(), filter.amount(), Long::sum);
        }
        Map<AEKey, Long> actual = new LinkedHashMap<>();
        for (GenericStack stack : data.contents()) {
            actual.merge(stack.what(), stack.amount(), Long::sum);
        }
        return actual.equals(expected);
    }

    private boolean isLegacyInputSlotValid(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0 || slot >= LEGACY_INPUT_SLOT_COUNT) {
            return false;
        }
        List<GenericStack> filters = menuInputFilters();
        if (slot >= filters.size()) {
            return false;
        }
        GenericStack filter = filters.get(slot);
        return filter.what().equals(AEItemKey.of(stack)) && stack.getCount() <= filter.amount();
    }

    private static boolean menuInputsFitCapacity(MenuInputEntry[] inputs, PackageCapacityProfile capacityProfile) {
        MenuInputContents contents = menuInputContents(inputs);
        if (contents.isEmpty()) {
            return true;
        }
        return PackagePlanBuilder.build(
                        PackageColor.FLUIX,
                        contents.looseContents(),
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
            this.outputMode = next;
            setChanged();
        }
    }

    public void toggleAutoExport() {
        setOutputMode(outputMode.next());
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
        if (hasMenuInputs()) {
            if (AdvancedProcessingPatternDataStorage.hasData(items.getStackInSlot(SLOT_PATTERN))) {
                Optional<MenuBatchPlan> batch = planMenuAdvancedPattern(items.getStackInSlot(SLOT_PATTERN));
                if (batch.isEmpty()) {
                    return AssemblyResult.PATTERN_MISMATCH;
                }
                commitMenuExtractions(batch.get().menuExtractions());
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
        AssemblyAttempt attempt = planAssembly(inputView);
        if (attempt.plan().isEmpty()) {
            return attempt.failure();
        }

        return commitAssemblyPlan(inputView, attempt.plan().orElseThrow());
    }

    private AssemblyResult tryStartAssembly() {
        if (!outputSlotsEmpty() || !pendingPackages.isEmpty() || !activePackages.isEmpty()) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        if (hasMenuInputs()) {
            if (AdvancedProcessingPatternDataStorage.hasData(items.getStackInSlot(SLOT_PATTERN))) {
                Optional<MenuBatchPlan> batch = planMenuAdvancedPattern(items.getStackInSlot(SLOT_PATTERN));
                if (batch.isEmpty()) {
                    return AssemblyResult.PATTERN_MISMATCH;
                }
                commitMenuExtractions(batch.get().menuExtractions());
                beginCrafting(batch.get().packages());
                return AssemblyResult.ASSEMBLED;
            }
            AssemblyAttempt attempt = planMenuAssembly();
            if (attempt.plan().isEmpty()) {
                return attempt.failure();
            }
            return beginMenuAssemblyPlan(attempt.plan().orElseThrow());
        }
        AssemblyAttempt attempt = planAssembly(inputView);
        if (attempt.plan().isEmpty()) {
            return attempt.failure();
        }
        return beginAssemblyPlan(inputView, attempt.plan().orElseThrow());
    }

    private AssemblyResult beginAssemblyPlan(IItemHandler input, AssemblyPlan plan) {
        if (!outputSlotsEmpty()) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        if (!ItemPackageTransactions.canExtract(input, plan.itemPlan())) {
            return AssemblyResult.SOURCE_CHANGED;
        }
        if (!ItemPackageTransactions.commitExtract(input, plan.itemPlan())) {
            return AssemblyResult.SOURCE_CHANGED;
        }
        beginCrafting(List.of(new QueuedPackage(plan.color(), plan.data())));
        return AssemblyResult.ASSEMBLED;
    }

    private AssemblyResult beginMenuAssemblyPlan(AssemblyPlan plan) {
        if (!outputSlotsEmpty()) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        if (plan.menuExtractions() != null) {
            commitMenuExtractions(plan.menuExtractions());
        } else {
            clearMenuInputs();
        }
        beginCrafting(List.of(new QueuedPackage(plan.color(), plan.data())));
        return AssemblyResult.ASSEMBLED;
    }

    private void beginCrafting(List<QueuedPackage> packages) {
        if (packages.isEmpty()) {
            return;
        }
        activePackages.clear();
        activePackages.addAll(packages);
        craftingProgress = 0;
        setChanged();
    }

    private void advanceCraftingProgress() {
        if (activePackages.isEmpty()) {
            craftingProgress = 0;
            return;
        }
        if (!outputSlotsEmpty()) {
            return;
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
        activePackages.clear();
        craftingProgress = 0;
        commitProviderPackages(packages);
        setChanged();
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

    public boolean exportOutputOnce() {
        if (level == null || level.isClientSide) {
            return false;
        }
        if (outputMode == OutputMode.NONE) {
            return false;
        }

        promoteNextOutput();
        ItemStack output = items.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return false;
        }
        Optional<MEStorage> meStorage = outputMode == OutputMode.ME_NETWORK ? findOutputMEStorage() : Optional.empty();
        Optional<IItemHandler> itemTarget = outputMode == OutputMode.ADJACENT_BLOCK
                ? findOutputItemHandler()
                : Optional.empty();
        if (meStorage.isPresent() && exportOutputToMEStorage(SLOT_OUTPUT, meStorage.get(), output)) {
            return true;
        }
        if (itemTarget.isPresent() && exportOutputToItemHandler(SLOT_OUTPUT, itemTarget.get(), output)) {
            return true;
        }
        return false;
    }

    private boolean exportOutputIfEnabled() {
        boolean exported = false;
        while (outputMode != OutputMode.NONE && exportOutputOnce()) {
            exported = true;
        }
        return exported;
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

    private AssemblyResult commitAssemblyPlan(IItemHandler input, AssemblyPlan plan) {
        ItemStack packageStack = packageStack(plan.color(), plan.data());
        if (!canInsertOutputPackage(packageStack)) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        if (!ItemPackageTransactions.canExtract(input, plan.itemPlan())) {
            return AssemblyResult.SOURCE_CHANGED;
        }

        if (!ItemPackageTransactions.commitExtract(input, plan.itemPlan())) {
            return AssemblyResult.SOURCE_CHANGED;
        }
        insertOutputPackage(packageStack);
        setChanged();
        return AssemblyResult.ASSEMBLED;
    }

    private AssemblyResult commitMenuAssemblyPlan(AssemblyPlan plan) {
        ItemStack packageStack = packageStack(plan.color(), plan.data());
        if (!canInsertOutputPackage(packageStack)) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        if (plan.menuExtractions() != null) {
            commitMenuExtractions(plan.menuExtractions());
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

    private AssemblyAttempt planAssembly(IItemHandler input) {
        ItemStack patternStack = items.getStackInSlot(SLOT_PATTERN);
        Optional<PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern> processingPattern =
                PackagedProcessingPatternDataStorage.read(patternStack);
        if (processingPattern.isPresent()) {
            return planProcessingPattern(input, processingPattern.get());
        }

        Optional<PackagePatternDataStorage.EncodedPackagePattern> pattern =
                PackagePatternDataStorage.read(patternStack);
        if (pattern.isPresent()) {
            Optional<ItemPackagePlan> exactPlan = ItemPackageTransactions.planExactPackage(
                    input,
                    pattern.get().color(),
                    packageDataWithConfiguredFallbacks(pattern.get().color(), pattern.get().data()));
            if (exactPlan.isEmpty()) {
                return AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH);
            }
            if (!ItemPackageTransactions.canExtract(input, exactPlan.get())) {
                return AssemblyAttempt.failed(AssemblyResult.SOURCE_CHANGED);
            }
            return AssemblyAttempt.planned(new AssemblyPlan(pattern.get().color(), exactPlan.get()));
        }

        Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> packageCraftingPattern =
                PackageCraftingPatternDataStorage.read(patternStack);
        if (packageCraftingPattern.isPresent()) {
            Optional<ItemPackagePlan> exactPlan = ItemPackageTransactions.planExactPackage(
                    input,
                    packageCraftingPattern.get().color(),
                    packageCraftingPattern.get().data());
            if (exactPlan.isEmpty() || !ItemPackageTransactions.canExtract(input, exactPlan.get())) {
                return AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH);
            }
            return AssemblyAttempt.planned(new AssemblyPlan(packageCraftingPattern.get().color(), exactPlan.get()));
        }

        if (ColoredProcessingPatternDataStorage.hasData(patternStack)) {
            return planDefaultEncodedPattern(input, patternStack);
        }

        if (isAe2ProcessingPattern(patternStack)) {
            return planDefaultEncodedPattern(input, patternStack);
        }

        return AssemblyAttempt.failed(AssemblyResult.NO_CONTENTS);
    }

    private AssemblyAttempt planMenuAssembly() {
        ItemStack patternStack = items.getStackInSlot(SLOT_PATTERN);
        Optional<PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern> processingPattern =
                PackagedProcessingPatternDataStorage.read(patternStack);
        if (processingPattern.isPresent()) {
            for (PackageData target : processingPattern.get().packages()) {
                Optional<AssemblyPlan> plan = planMenuExactPackage(processingPattern.get().color(), target);
                if (plan.isPresent()) {
                    return AssemblyAttempt.planned(plan.get());
                }
            }
            return AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH);
        }

        Optional<PackagePatternDataStorage.EncodedPackagePattern> pattern =
                PackagePatternDataStorage.read(patternStack);
        if (pattern.isPresent()) {
            return planMenuExactPackage(
                            pattern.get().color(),
                            packageDataWithConfiguredFallbacks(pattern.get().color(), pattern.get().data()))
                    .map(AssemblyAttempt::planned)
                    .orElseGet(() -> AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH));
        }

        Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> packageCraftingPattern =
                PackageCraftingPatternDataStorage.read(patternStack);
        if (packageCraftingPattern.isPresent()) {
            return planMenuExactPackage(
                            packageCraftingPattern.get().color(),
                            packageCraftingPattern.get().data())
                    .map(AssemblyAttempt::planned)
                    .orElseGet(() -> AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH));
        }

        if (ColoredProcessingPatternDataStorage.hasData(patternStack)) {
            return planMenuDefaultEncodedPattern(patternStack);
        }

        if (isAe2ProcessingPattern(patternStack)) {
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
        PackageCapacityProfile capacityProfile = capacityProfileFor(target).orElse(configuredCapacityProfile());
        PackagePlanResult result = PackagePlanBuilder.build(
                color,
                extraction.looseContents(),
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
        List<GenericStack> filters = menuInputFilters();
        if (!menuInputsExactlyMatchFilters(menuInputs, filters)) {
            return Optional.empty();
        }

        List<QueuedPackage> packages = new ArrayList<>();
        List<MenuInputExtraction> extractions = new ArrayList<>();
        for (var column : encoded.get().columns()) {
            List<GenericStack> inputs = new ArrayList<>();
            int start = column.index() * AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
            for (int row = 0; row < AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE; row++) {
                int slot = start + row;
                MenuInputEntry entry = menuInputs[slot];
                if (entry == null || entry.amount() <= 0) {
                    continue;
                }
                inputs.add(new GenericStack(AEItemKey.of(entry.stack()), entry.amount()));
                extractions.add(new MenuInputExtraction(slot, entry.amount()));
            }
            if (inputs.isEmpty()) {
                continue;
            }
            Optional<QueuedPackage> queued = buildPatternPackage(
                    column.color(),
                    column.marker(),
                    inputs);
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

            Optional<PackageData> packageData = PackageDataStorage.read(entry.stack());
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

            AEItemKey key = AEItemKey.of(entry.stack());
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

    private AssemblyAttempt planProcessingPattern(
            IItemHandler input,
            PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern pattern) {
        for (var target : pattern.packages()) {
            PackageData effectiveTarget = packageDataWithConfiguredFallbacks(pattern.color(), target);
            Optional<ItemPackagePlan> plan = ItemPackageTransactions.planExactPackage(input, pattern.color(), effectiveTarget);
            if (plan.isEmpty() || !ItemPackageTransactions.canExtract(input, plan.get())) {
                continue;
            }
            return AssemblyAttempt.planned(new AssemblyPlan(pattern.color(), plan.get()));
        }
        return AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH);
    }

    private AssemblyAttempt planDefaultEncodedPattern(IItemHandler input, ItemStack patternStack) {
        List<GenericStack> filters = decodedPatternInputs(patternStack);
        if (filters.isEmpty()) {
            return AssemblyAttempt.failed(AssemblyResult.NO_CONTENTS);
        }
        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                input,
                PackageColor.FLUIX,
                configuredCapacityProfile(),
                new com.warmthdawn.appliedpackaging.core.package_data.PackageFilter(
                        Optional.of(PackageColor.FLUIX),
                        Optional.empty(),
                        filters),
                MarkerMergeMode.CLEAR,
                Optional.empty());
        if (plan.isEmpty()) {
            return AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH);
        }
        if (!packageContentsExactlyMatch(plan.get().data(), filters)
                || !ItemPackageTransactions.canExtract(input, plan.get())) {
            return AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH);
        }
        return AssemblyAttempt.planned(new AssemblyPlan(PackageColor.FLUIX, plan.get()));
    }

    private AssemblyAttempt planMenuDefaultEncodedPattern(ItemStack patternStack) {
        List<GenericStack> filters = decodedPatternInputs(patternStack);
        if (!menuInputsExactlyMatchFilters(menuInputs, filters)) {
            return AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH);
        }
        MenuInputContents contents = menuInputContents(menuInputs);
        PackagePlanResult result = PackagePlanBuilder.build(
                PackageColor.FLUIX,
                contents.looseContents(),
                contents.sourcePackages(),
                MarkerMergeMode.CLEAR,
                Optional.empty(),
                configuredCapacityProfile(),
                0);
        if (result.data().isEmpty() || !packageContentsExactlyMatch(result.data().orElseThrow(), filters)) {
            return AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH);
        }
        return AssemblyAttempt.planned(new AssemblyPlan(
                PackageColor.FLUIX,
                result.data().orElseThrow(),
                List.of()));
    }

    private Optional<QueuedPackage> buildPatternPackage(
            PackageColor color,
            Optional<MarkerSpec> marker,
            List<GenericStack> inputs) {
        List<GenericStack> looseContents = new ArrayList<>();
        List<PackageData> sourcePackages = new ArrayList<>();
        PackageCapacityProfile capacity = configuredCapacityProfile();
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
                }
            } else {
                looseContents.add(input);
            }
        }
        PackagePlanResult result = PackagePlanBuilder.build(
                color,
                looseContents,
                sourcePackages,
                marker.isPresent() ? MarkerMergeMode.OVERRIDE : MarkerMergeMode.CLEAR,
                marker,
                capacity,
                0);
        return result.data().map(data -> new QueuedPackage(color, data));
    }

    private Optional<ColoredProviderPlan> planAdvancedProcessingPush(
            ItemStack definitionStack,
            KeyCounter[] inputHolder) {
        Optional<AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern> encoded =
                AdvancedProcessingPatternDataStorage.read(definitionStack);
        if (encoded.isEmpty()) {
            return Optional.empty();
        }
        List<GenericStack> sparseInputs = ColoredProcessingPatternDataStorage.readSparseInputs(definitionStack);
        int encodedSlots = encoded.get().activeColumnCount()
                * AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
        for (int slot = encodedSlots; slot < sparseInputs.size(); slot++) {
            if (sparseInputs.get(slot) != null) {
                return Optional.empty();
            }
        }

        Map<AEKey, Long> remainingInputs = new HashMap<>(aggregateInputs(inputHolder).orElse(Map.of()));
        List<List<GenericStack>> columnInputs = new ArrayList<>();
        for (int column = 0; column < encoded.get().activeColumnCount(); column++) {
            columnInputs.add(new ArrayList<>());
        }
        List<GenericStack> consumedInputs = new ArrayList<>();
        for (int slot = 0; slot < encodedSlots; slot++) {
            GenericStack input = slot < sparseInputs.size() ? sparseInputs.get(slot) : null;
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
            columnInputs.get(slot / AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE).add(consumed);
            consumedInputs.add(consumed);
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
                    inputs);
            if (queued.isEmpty()) {
                return Optional.empty();
            }
            packages.add(queued.get());
        }
        if (packages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ColoredProviderPlan(packages, consumedInputs));
    }

    private Optional<ColoredProviderPlan> planOrdinaryProcessingPush(
            ItemStack definitionStack,
            KeyCounter[] inputHolder) {
        if (!isAe2ProcessingPattern(definitionStack)
                || AdvancedProcessingPatternDataStorage.hasData(definitionStack)
                || ColoredProcessingPatternDataStorage.hasData(definitionStack)) {
            return Optional.empty();
        }
        List<GenericStack> sparseInputs = ColoredProcessingPatternDataStorage.readSparseInputs(definitionStack);
        List<GenericStack> inputs = new ArrayList<>();
        Map<AEKey, Long> expected = new LinkedHashMap<>();
        for (GenericStack input : sparseInputs) {
            if (input == null || input.amount() <= 0) {
                continue;
            }
            inputs.add(new GenericStack(input.what(), input.amount()));
            expected.merge(input.what(), input.amount(), Long::sum);
        }
        if (inputs.isEmpty() || !expected.equals(aggregateInputs(inputHolder).orElse(Map.of()))) {
            return Optional.empty();
        }
        Optional<QueuedPackage> queued = buildPatternPackage(
                PackageColor.FLUIX,
                Optional.empty(),
                inputs);
        return queued.map(value -> new ColoredProviderPlan(List.of(value), inputs));
    }

    private Optional<ColoredProviderPlan> planColoredProcessingPush(ItemStack definitionStack, KeyCounter[] inputHolder) {
        Optional<ColoredProcessingPatternDataStorage.EncodedColoredProcessingPattern> encoded =
                ColoredProcessingPatternDataStorage.read(definitionStack);
        if (encoded.isEmpty()) {
            return Optional.empty();
        }

        List<GenericStack> sparseInputs = ColoredProcessingPatternDataStorage.readSparseInputs(definitionStack);
        if (sparseInputs.isEmpty()) {
            return Optional.empty();
        }
        for (int coloredSlot : encoded.get().slotColors().keySet()) {
            if (coloredSlot >= sparseInputs.size() || sparseInputs.get(coloredSlot) == null) {
                return Optional.empty();
            }
        }

        Optional<Map<AEKey, Long>> availableInputs = aggregateInputs(inputHolder);
        if (availableInputs.isEmpty()) {
            return Optional.empty();
        }
        Map<AEKey, Long> remainingInputs = new HashMap<>(availableInputs.get());
        Map<PackageColor, List<GenericStack>> groupedInputs = new LinkedHashMap<>();
        List<GenericStack> consumedInputs = new ArrayList<>();

        for (int slot = 0; slot < sparseInputs.size(); slot++) {
            GenericStack sparseInput = sparseInputs.get(slot);
            if (sparseInput == null || sparseInput.amount() <= 0) {
                continue;
            }
            long available = remainingInputs.getOrDefault(sparseInput.what(), 0L);
            if (available < sparseInput.amount()) {
                return Optional.empty();
            }
            long remaining = available - sparseInput.amount();
            if (remaining == 0) {
                remainingInputs.remove(sparseInput.what());
            } else {
                remainingInputs.put(sparseInput.what(), remaining);
            }

            PackageColor color = encoded.get().colorForSlot(slot);
            GenericStack consumed = new GenericStack(sparseInput.what(), sparseInput.amount());
            groupedInputs.computeIfAbsent(color, ignored -> new ArrayList<>()).add(consumed);
            consumedInputs.add(consumed);
        }

        if (groupedInputs.isEmpty() || hasRemainingInputs(remainingInputs)) {
            return Optional.empty();
        }

        List<QueuedPackage> packages = new ArrayList<>();
        for (var entry : groupedInputs.entrySet()) {
            var result = PackagePlanBuilder.build(
                    entry.getKey(),
                    entry.getValue(),
                    List.of(),
                    configuredMarkerMode(),
                    configuredMarkerItem(),
                    configuredCapacityProfile(),
                    0);
            if (result.data().isEmpty()) {
                return Optional.empty();
            }
            packages.add(new QueuedPackage(entry.getKey(), result.data().orElseThrow()));
        }

        return Optional.of(new ColoredProviderPlan(packages, consumedInputs));
    }

    private boolean commitProviderPackages(List<QueuedPackage> packages) {
        if (packages.isEmpty() || !pendingPackages.isEmpty() || !hasOutputRoom()) {
            return false;
        }
        QueuedPackage first = packages.get(0);
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
        Optional<PackagedProviderPlan> packageCraftingPlan = planPackageCraftingPush(definitionStack, inputHolder);
        if (packageCraftingPlan.isPresent()) {
            if (!beginProviderCraft(
                    packageCraftingPlan.get().packages(),
                    inputHolder,
                    packageCraftingPlan.get().consumedInputs())) {
                return false;
            }
            return true;
        }

        Optional<PackagedProviderPlan> packagedPlan = planPackagedProcessingPush(definitionStack, inputHolder);
        if (packagedPlan.isPresent()) {
            if (!beginProviderCraft(packagedPlan.get().packages(), inputHolder, packagedPlan.get().consumedInputs())) {
                return false;
            }
            return true;
        }

        if (AdvancedProcessingPatternDataStorage.hasData(definitionStack)) {
            Optional<ColoredProviderPlan> advancedPlan = planAdvancedProcessingPush(definitionStack, inputHolder);
            if (advancedPlan.isEmpty()
                    || !beginProviderCraft(
                            advancedPlan.get().packages(),
                            inputHolder,
                            advancedPlan.get().consumedInputs())) {
                return false;
            }
            return true;
        }

        if (ColoredProcessingPatternDataStorage.hasData(definitionStack)) {
            Optional<ColoredProviderPlan> coloredPlan = planColoredProcessingPush(definitionStack, inputHolder);
            if (coloredPlan.isEmpty()
                    || !beginProviderCraft(coloredPlan.get().packages(), inputHolder, coloredPlan.get().consumedInputs())) {
                return false;
            }
            return true;
        }

        if (isAe2ProcessingPattern(definitionStack)) {
            Optional<ColoredProviderPlan> ordinaryPlan = planOrdinaryProcessingPush(definitionStack, inputHolder);
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
        if (packages.isEmpty() || !pendingPackages.isEmpty() || !activePackages.isEmpty() || !outputSlotsEmpty()) {
            return false;
        }
        consumePatternInputs(inputHolder, consumedInputs);
        if (level == null) {
            return commitProviderPackages(packages);
        }
        beginCrafting(packages);
        return true;
    }

    private Optional<PackagedProviderPlan> planPackagedProcessingPush(
            ItemStack definitionStack,
            KeyCounter[] inputHolder) {
        Optional<PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern> encoded =
                PackagedProcessingPatternDataStorage.read(definitionStack);
        if (encoded.isEmpty()) {
            return Optional.empty();
        }
        Optional<Map<AEKey, Long>> availableInputs = aggregateInputs(inputHolder);
        if (availableInputs.isEmpty()) {
            return Optional.empty();
        }

        Map<AEKey, Long> remainingInputs = new HashMap<>(availableInputs.get());
        Map<AEKey, Long> consumedByKey = new LinkedHashMap<>();
        List<QueuedPackage> packages = new ArrayList<>();
        for (PackageData data : encoded.get().packages()) {
            for (GenericStack stack : data.contents()) {
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
            packages.add(new QueuedPackage(
                    encoded.get().color(),
                    packageDataWithConfiguredFallbacks(encoded.get().color(), data)));
        }
        if (packages.isEmpty() || hasRemainingInputs(remainingInputs)) {
            return Optional.empty();
        }

        List<GenericStack> consumedInputs = consumedByKey.entrySet().stream()
                .map(entry -> new GenericStack(entry.getKey(), entry.getValue()))
                .toList();
        return Optional.of(new PackagedProviderPlan(packages, consumedInputs));
    }

    private Optional<PackagedProviderPlan> planPackageCraftingPush(
            ItemStack definitionStack,
            KeyCounter[] inputHolder) {
        Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> encoded =
                PackageCraftingPatternDataStorage.read(definitionStack);
        if (encoded.isEmpty()) {
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
        return Optional.of(new PackagedProviderPlan(
                List.of(new QueuedPackage(encoded.get().color(), encoded.get().data())),
                consumedInputs));
    }

    @Override
    public boolean acceptsPlans() {
        return outputSlotsEmpty()
                && inputBufferIsEmpty()
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

    private Optional<IItemHandler> findOutputItemHandler() {
        Direction targetDirection = outputDirection();
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

    private Direction outputDirection() {
        Direction facing = getBlockState().getValue(AbstractHorizontalMachineBlock.FACING);
        return facing.getOpposite();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, net.minecraft.core.Direction side) {
        if (capability == Capabilities.CRAFTING_MACHINE) {
            return Capabilities.CRAFTING_MACHINE.orEmpty(capability, craftingMachine);
        }
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
        craftingMachine.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandler = createItemHandlerCapability();
        craftingMachine = createCraftingMachineCapability();
    }

    private LazyOptional<IItemHandler> createItemHandlerCapability() {
        return LazyOptional.of(() -> externalItemHandler);
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
        tag.putInt(CRAFT_PROGRESS_TAG, craftingProgress);
        tag.putString(OUTPUT_MODE_TAG, outputMode.id());
        tag.putString(SELECTED_COLOR_TAG, selectedColor.id());
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        List<QueuedPackage> legacyOutputs = List.of();
        if (tag.contains(ITEMS_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            legacyOutputs = loadItems(tag.getCompound(ITEMS_TAG));
        }
        upgrades.readFromNBT(tag, UPGRADES_TAG);
        loadMenuInputs(tag);
        if (tag.contains(OUTPUT_MODE_TAG, Tag.TAG_STRING)) {
            outputMode = OutputMode.byId(tag.getString(OUTPUT_MODE_TAG)).orElse(OutputMode.ME_NETWORK);
        } else {
            outputMode = !tag.contains(AUTO_EXPORT_TAG, Tag.TAG_BYTE) || tag.getBoolean(AUTO_EXPORT_TAG)
                    ? OutputMode.ME_NETWORK
                    : OutputMode.NONE;
        }
        selectedColor = PackageColor.byId(tag.getString(SELECTED_COLOR_TAG)).orElse(PackageColor.FLUIX);
        craftingProgress = Math.max(0, Math.min(MAX_CRAFT_PROGRESS, tag.getInt(CRAFT_PROGRESS_TAG)));
        loadQueuedPackages(tag, ACTIVE_PACKAGES_TAG, activePackages);
        loadPendingPackages(tag);
        if (!legacyOutputs.isEmpty()) {
            pendingPackages.addAll(0, legacyOutputs);
        }
        promoteNextOutput();
    }

    private List<QueuedPackage> loadItems(CompoundTag itemsTag) {
        ItemStackHandler loadedItems = new ItemStackHandler(SLOT_COUNT);
        loadedItems.deserializeNBT(itemsTag);
        List<QueuedPackage> legacyOutputs = new ArrayList<>();
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = slot < loadedItems.getSlots() ? loadedItems.getStackInSlot(slot) : ItemStack.EMPTY;
            if (isOutputSlot(slot)) {
                if (slot == SLOT_OUTPUT && !stack.isEmpty()) {
                    ItemStack primary = stack.copyWithCount(1);
                    items.setStackInSlot(SLOT_OUTPUT, primary);
                    for (int count = 1; count < stack.getCount(); count++) {
                        queuedPackage(stack).ifPresent(legacyOutputs::add);
                    }
                } else if (slot != SLOT_OUTPUT && !stack.isEmpty()) {
                    for (int count = 0; count < stack.getCount(); count++) {
                        queuedPackage(stack).ifPresent(legacyOutputs::add);
                    }
                }
            } else {
                items.setStackInSlot(slot, stack);
            }
        }
        return legacyOutputs;
    }

    private Optional<QueuedPackage> queuedPackage(ItemStack stack) {
        if (!(stack.getItem() instanceof PackageItem packageItem)) {
            return Optional.empty();
        }
        return PackageDataStorage.read(stack).map(data -> new QueuedPackage(packageItem.color(), data));
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
        for (QueuedPackage queuedPackage : activePackages) {
            Containers.dropItemStack(
                    level,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    packageStack(queuedPackage.color(), queuedPackage.data()));
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
            tag.put(MENU_INPUT_STACK_TAG, entry.stack().save(new CompoundTag()));
            tag.putLong(MENU_INPUT_AMOUNT_TAG, entry.amount());
            list.add(tag);
        }
        return list;
    }

    private void loadMenuInputs(CompoundTag tag) {
        Arrays.fill(menuInputs, null);
        if (!tag.contains(MENU_INPUTS_TAG, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList(MENU_INPUTS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entryTag = list.getCompound(index);
            int slot = entryTag.getInt(MENU_INPUT_SLOT_TAG);
            if (slot < 0 || slot >= menuInputs.length
                    || !entryTag.contains(MENU_INPUT_STACK_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            ItemStack stack = ItemStack.of(entryTag.getCompound(MENU_INPUT_STACK_TAG));
            long amount = entryTag.getLong(MENU_INPUT_AMOUNT_TAG);
            if (!stack.isEmpty() && amount > 0) {
                stack.setCount(1);
                menuInputs[slot] = new MenuInputEntry(stack, amount);
            }
        }
    }

    private static void dropMenuInput(Level level, BlockPos pos, MenuInputEntry entry) {
        long remaining = entry.amount();
        int maxStack = Math.max(1, entry.stack().getMaxStackSize());
        while (remaining > 0) {
            int amount = (int) Math.min(remaining, maxStack);
            ItemStack drop = entry.stack().copy();
            drop.setCount(amount);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
            remaining -= amount;
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

    private boolean inputBufferIsEmpty() {
        for (int slot = 0; slot < LEGACY_INPUT_SLOT_COUNT; slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void commitMenuExtractions(List<MenuInputExtraction> extractions) {
        for (MenuInputExtraction extraction : extractions) {
            MenuInputEntry entry = menuInput(extraction.slot());
            if (entry == null) {
                continue;
            }
            long remaining = entry.amount() - extraction.amount();
            menuInputs[extraction.slot()] = remaining <= 0
                    ? null
                    : new MenuInputEntry(entry.stack(), remaining);
        }
    }

    private static MenuInputContents menuInputContents(MenuInputEntry[] inputs) {
        List<GenericStack> looseContents = new ArrayList<>();
        List<PackageData> sourcePackages = new ArrayList<>();
        for (MenuInputEntry entry : inputs) {
            if (entry == null || entry.amount() <= 0) {
                continue;
            }
            Optional<PackageData> packageData = PackageDataStorage.read(entry.stack());
            if (packageData.isPresent()) {
                for (long count = 0; count < entry.amount(); count++) {
                    sourcePackages.add(packageData.get());
                }
                continue;
            }
            looseContents.add(new GenericStack(AEItemKey.of(entry.stack()), entry.amount()));
        }
        return new MenuInputContents(List.copyOf(looseContents), List.copyOf(sourcePackages));
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

    private static Optional<PackageCapacityProfile> capacityProfileFor(PackageData data) {
        for (PackageCapacityProfile profile : PackageCapacityProfile.values()) {
            if (profile.fits(data.usedUnits(), data.usedTypes())) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    private PackageData packageDataWithConfiguredFallbacks(PackageColor color, PackageData target) {
        Optional<MarkerSpec> marker = target.marker().isPresent() ? target.marker() : configuredMarkerItem();
        if (marker.equals(target.marker())) {
            return target;
        }
        return PackageData.create(color, target.contents(), marker, target.flags());
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

    private PackageCapacityProfile configuredCapacityProfile() {
        return MePackagerBlockEntity.capacityProfileFromItem(items.getStackInSlot(SLOT_CAPACITY))
                .orElse(PackageCapacityProfile.DEFAULT);
    }

    private MarkerMergeMode configuredMarkerMode() {
        return configuredMarkerItem().isPresent() ? MarkerMergeMode.OVERRIDE : MarkerMergeMode.RETAIN;
    }

    private Optional<MarkerSpec> configuredMarkerItem() {
        ItemStack markerStack = items.getStackInSlot(SLOT_MARKER);
        if (!isMarkerSlotItem(markerStack)) {
            return Optional.empty();
        }
        ItemStack keyStack = markerStack.copy();
        keyStack.setCount(1);
        return Optional.of(new MarkerSpec(new GenericStack(AEItemKey.of(keyStack), 1)));
    }

    public static boolean isMarkerSlotItem(ItemStack stack) {
        return !stack.isEmpty()
                && !(stack.getItem() instanceof PackageItem)
                && !isPatternSlotItem(stack);
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
            setChanged();
        }
        return extracted;
    }

    private void promoteNextOutput() {
        if (!items.getStackInSlot(SLOT_OUTPUT).isEmpty() || pendingPackages.isEmpty()) {
            return;
        }
        QueuedPackage next = pendingPackages.remove(0);
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, packageStack(next.color(), next.data()), false);
        if (!remainder.isEmpty()) {
            pendingPackages.add(0, next);
        }
    }

    private final class ExternalItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return isOutputSlot(slot) && slot != SLOT_OUTPUT ? ItemStack.EMPTY : items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || isOutputSlot(slot) || isCrafting()) {
                return stack;
            }
            if (!items.isItemValid(slot, stack)) {
                return stack;
            }
            return items.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || slot != SLOT_OUTPUT) {
                return ItemStack.EMPTY;
            }
            return extractPrimaryOutput(amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !isOutputSlot(slot) && !isCrafting() && items.isItemValid(slot, stack);
        }
    }

    private record AssemblyPlan(
            PackageColor color,
            PackageData data,
            ItemPackagePlan itemPlan,
            List<MenuInputExtraction> menuExtractions) {
        private AssemblyPlan(PackageColor color, ItemPackagePlan plan) {
            this(color, plan.data(), plan, List.of());
        }

        private AssemblyPlan(PackageColor color, PackageData data, List<MenuInputExtraction> menuExtractions) {
            this(color, data, null, menuExtractions);
        }
    }

    private record QueuedPackage(PackageColor color, PackageData data) {
    }

    private record ColoredProviderPlan(List<QueuedPackage> packages, List<GenericStack> consumedInputs) {
        private ColoredProviderPlan {
            packages = List.copyOf(packages);
            consumedInputs = List.copyOf(consumedInputs);
        }
    }

    private record PackagedProviderPlan(List<QueuedPackage> packages, List<GenericStack> consumedInputs) {
        private PackagedProviderPlan {
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

    private record MenuInputEntry(ItemStack stack, long amount) {
        private MenuInputEntry {
            stack = stack.copy();
            stack.setCount(1);
        }

        private ItemStack displayStack() {
            ItemStack display = stack.copy();
            display.setCount((int) Math.max(1L, Math.min(amount, Integer.MAX_VALUE)));
            return display;
        }
    }

    private record MenuInputExtraction(int slot, long amount) {
    }

    private record MenuInputContents(List<GenericStack> looseContents, List<PackageData> sourcePackages) {
        private MenuInputContents {
            looseContents = List.copyOf(looseContents);
            sourcePackages = List.copyOf(sourcePackages);
        }

        private boolean isEmpty() {
            return looseContents.isEmpty() && sourcePackages.isEmpty();
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

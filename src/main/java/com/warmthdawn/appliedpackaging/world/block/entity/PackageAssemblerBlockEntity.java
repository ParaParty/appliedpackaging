package com.warmthdawn.appliedpackaging.world.block.entity;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.capabilities.Capabilities;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackagePlan;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackageTransactions;
import com.warmthdawn.appliedpackaging.core.package_data.ColoredProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.PackagedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanBuilder;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.world.block.InventoryDroppingBlockEntity;
import com.warmthdawn.appliedpackaging.world.menu.PackageAssemblerMenu;
import java.util.ArrayList;
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

public class PackageAssemblerBlockEntity extends BlockEntity implements InventoryDroppingBlockEntity, MenuProvider, ICraftingMachine {
    public static final int INPUT_SLOT_COUNT = 9;
    public static final int SLOT_PATTERN = 9;
    public static final int SLOT_OUTPUT = 10;
    public static final int SLOT_CAPACITY = 11;
    private static final int SLOT_COUNT = 12;
    private static final String ITEMS_TAG = "items";
    private static final String PENDING_PACKAGES_TAG = "pending_packages";
    private static final String PENDING_COLOR_TAG = "color";
    private static final String PENDING_DATA_TAG = "data";

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_OUTPUT) {
                return stack.getItem() instanceof PackageItem;
            }
            if (slot == SLOT_PATTERN) {
                return isPatternSlotItem(stack);
            }
            if (slot == SLOT_CAPACITY) {
                return MePackagerBlockEntity.capacityProfileFromItem(stack).isPresent();
            }
            if (stack.getItem() instanceof PackageItem) {
                return PackageDataStorage.read(stack).isPresent();
            }
            return true;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == SLOT_CAPACITY) {
                return 1;
            }
            return super.getSlotLimit(slot);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final RangedWrapper inputView = new RangedWrapper(items, 0, INPUT_SLOT_COUNT);
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> items);
    private final LazyOptional<ICraftingMachine> craftingMachine = LazyOptional.of(() -> this);
    private final List<QueuedPackage> pendingPackages = new ArrayList<>();

    public PackageAssemblerBlockEntity(BlockPos pos, BlockState blockState) {
        super(APBlockEntities.PACKAGE_ASSEMBLER.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PackageAssemblerBlockEntity blockEntity) {
        blockEntity.tryAssemble();
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public static boolean isPatternSlotItem(ItemStack stack) {
        return PackagePatternDataStorage.canStore(stack) || PackagedProcessingPatternDataStorage.canStore(stack);
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
        if (!items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        if (!pendingPackages.isEmpty()) {
            return outputPendingPackage();
        }
        AssemblyAttempt attempt = planAssembly(inputView);
        if (attempt.plan().isEmpty()) {
            return attempt.failure();
        }

        return commitAssemblyPlan(inputView, attempt.plan().orElseThrow());
    }

    private AssemblyResult commitAssemblyPlan(IItemHandler input, AssemblyPlan plan) {
        ItemStack packageStack = packageStack(plan.color(), plan.plan().data());
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, packageStack.copy(), true);
        if (!remainder.isEmpty()) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        if (!ItemPackageTransactions.canExtract(input, plan.plan())) {
            return AssemblyResult.SOURCE_CHANGED;
        }

        ItemPackageTransactions.commitExtract(input, plan.plan());
        items.insertItem(SLOT_OUTPUT, packageStack, false);
        setChanged();
        return AssemblyResult.ASSEMBLED;
    }

    private AssemblyResult outputPendingPackage() {
        if (pendingPackages.isEmpty()) {
            return AssemblyResult.NO_CONTENTS;
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
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, packageStack.copy(), true);
        if (!remainder.isEmpty()) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        remainder = items.insertItem(SLOT_OUTPUT, packageStack, false);
        if (!remainder.isEmpty()) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }
        setChanged();
        return AssemblyResult.ASSEMBLED;
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
                    pattern.get().data());
            if (exactPlan.isEmpty()) {
                return AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH);
            }
            if (!ItemPackageTransactions.canExtract(input, exactPlan.get())) {
                return AssemblyAttempt.failed(AssemblyResult.SOURCE_CHANGED);
            }
            return AssemblyAttempt.planned(new AssemblyPlan(pattern.get().color(), exactPlan.get()));
        }

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                input,
                PackageColor.FLUIX,
                configuredCapacityProfile());
        if (plan.isEmpty()) {
            return AssemblyAttempt.failed(AssemblyResult.NO_CONTENTS);
        }
        if (!ItemPackageTransactions.canExtract(input, plan.get())) {
            return AssemblyAttempt.failed(AssemblyResult.SOURCE_CHANGED);
        }
        return AssemblyAttempt.planned(new AssemblyPlan(PackageColor.FLUIX, plan.get()));
    }

    private AssemblyAttempt planProcessingPattern(
            IItemHandler input,
            PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern pattern) {
        for (var target : pattern.packages()) {
            Optional<ItemPackagePlan> plan = ItemPackageTransactions.planExactPackage(input, pattern.color(), target);
            if (plan.isEmpty() || !ItemPackageTransactions.canExtract(input, plan.get())) {
                continue;
            }
            return AssemblyAttempt.planned(new AssemblyPlan(pattern.color(), plan.get()));
        }
        return AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH);
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

        Optional<Map<AEKey, Long>> availableInputs = aggregateItemInputs(inputHolder);
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
            if (!AEItemKey.is(sparseInput.what())) {
                return Optional.empty();
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
                    MarkerMergeMode.CLEAR,
                    Optional.empty(),
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
        if (packages.isEmpty()
                || !pendingPackages.isEmpty()
                || !items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            return false;
        }

        QueuedPackage firstPackage = packages.get(0);
        ItemStack firstStack = packageStack(firstPackage.color(), firstPackage.data());
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, firstStack.copy(), true);
        if (!remainder.isEmpty()) {
            return false;
        }
        remainder = items.insertItem(SLOT_OUTPUT, firstStack, false);
        if (!remainder.isEmpty()) {
            return false;
        }

        if (packages.size() > 1) {
            pendingPackages.addAll(packages.subList(1, packages.size()));
        }
        setChanged();
        return true;
    }

    private Optional<ColoredProviderPlan> planDefaultProviderPush(KeyCounter[] inputHolder) {
        Optional<List<GenericStack>> contents = providerContents(inputHolder);
        if (contents.isEmpty()) {
            return Optional.empty();
        }
        var result = PackagePlanBuilder.build(
                PackageColor.FLUIX,
                contents.get(),
                List.of(),
                MarkerMergeMode.CLEAR,
                Optional.empty(),
                configuredCapacityProfile(),
                0);
        return result.data()
                .map(data -> new ColoredProviderPlan(List.of(new QueuedPackage(PackageColor.FLUIX, data)), contents.get()));
    }

    private static Optional<List<GenericStack>> providerContents(KeyCounter[] inputHolder) {
        List<GenericStack> contents = new ArrayList<>();
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
                contents.add(new GenericStack(entry.getKey(), entry.getLongValue()));
            }
        }
        if (contents.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(List.copyOf(contents));
    }

    private static Optional<Map<AEKey, Long>> aggregateItemInputs(KeyCounter[] inputHolder) {
        Map<AEKey, Long> inputs = new HashMap<>();
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
        Optional<PackagedProviderPlan> packagedPlan = planPackagedProcessingPush(definitionStack, inputHolder);
        if (packagedPlan.isPresent()) {
            if (!commitProviderPackages(packagedPlan.get().packages())) {
                return false;
            }
            consumePatternInputs(inputHolder, packagedPlan.get().consumedInputs());
            return true;
        }

        if (ColoredProcessingPatternDataStorage.hasData(definitionStack)) {
            Optional<ColoredProviderPlan> coloredPlan = planColoredProcessingPush(definitionStack, inputHolder);
            if (coloredPlan.isEmpty() || !commitProviderPackages(coloredPlan.get().packages())) {
                return false;
            }
            consumePatternInputs(inputHolder, coloredPlan.get().consumedInputs());
            return true;
        }

        if (items.getStackInSlot(SLOT_PATTERN).isEmpty()) {
            Optional<ColoredProviderPlan> defaultPlan = planDefaultProviderPush(inputHolder);
            if (defaultPlan.isEmpty() || !commitProviderPackages(defaultPlan.get().packages())) {
                return false;
            }
            consumePatternInputs(inputHolder, defaultPlan.get().consumedInputs());
            return true;
        }

        Optional<PatternProviderInput> providerInput = PatternProviderInput.create(inputHolder);
        if (providerInput.isEmpty()) {
            return false;
        }
        ItemStackHandler simulatedInput = new ItemStackHandler(INPUT_SLOT_COUNT);
        if (!providerInput.get().insertInto(simulatedInput, false)) {
            return false;
        }
        AssemblyAttempt attempt = planAssembly(simulatedInput);
        if (attempt.plan().isEmpty()) {
            return false;
        }

        if (!providerInput.get().insertInto(inputView, false)) {
            return false;
        }
        AssemblyResult result = commitAssemblyPlan(inputView, attempt.plan().orElseThrow());
        if (result != AssemblyResult.ASSEMBLED) {
            return false;
        }
        providerInput.get().consume();
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
        Optional<Map<AEKey, Long>> availableInputs = aggregateItemInputs(inputHolder);
        if (availableInputs.isEmpty()) {
            return Optional.empty();
        }

        Map<AEKey, Long> remainingInputs = new HashMap<>(availableInputs.get());
        Map<AEKey, Long> consumedByKey = new LinkedHashMap<>();
        List<QueuedPackage> packages = new ArrayList<>();
        for (PackageData data : encoded.get().packages()) {
            for (GenericStack stack : data.contents()) {
                if (!AEItemKey.is(stack.what())) {
                    return Optional.empty();
                }
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
            packages.add(new QueuedPackage(encoded.get().color(), data));
        }
        if (packages.isEmpty() || hasRemainingInputs(remainingInputs)) {
            return Optional.empty();
        }

        List<GenericStack> consumedInputs = consumedByKey.entrySet().stream()
                .map(entry -> new GenericStack(entry.getKey(), entry.getValue()))
                .toList();
        return Optional.of(new PackagedProviderPlan(packages, consumedInputs));
    }

    @Override
    public boolean acceptsPlans() {
        return items.getStackInSlot(SLOT_OUTPUT).isEmpty() && inputBufferIsEmpty() && pendingPackages.isEmpty();
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
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ITEMS_TAG, items.serializeNBT());
        tag.put(PENDING_PACKAGES_TAG, savePendingPackages());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ITEMS_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            loadItems(tag.getCompound(ITEMS_TAG));
        }
        loadPendingPackages(tag);
    }

    private void loadItems(CompoundTag itemsTag) {
        ItemStackHandler loadedItems = new ItemStackHandler(SLOT_COUNT);
        loadedItems.deserializeNBT(itemsTag);
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = slot < loadedItems.getSlots() ? loadedItems.getStackInSlot(slot) : ItemStack.EMPTY;
            items.setStackInSlot(slot, stack);
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
        for (QueuedPackage queuedPackage : pendingPackages) {
            Containers.dropItemStack(
                    level,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    packageStack(queuedPackage.color(), queuedPackage.data()));
        }
    }

    public enum AssemblyResult {
        ASSEMBLED,
        NO_CONTENTS,
        OUTPUT_BLOCKED,
        PATTERN_MISMATCH,
        SOURCE_CHANGED
    }

    private boolean inputBufferIsEmpty() {
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private ListTag savePendingPackages() {
        ListTag list = new ListTag();
        for (QueuedPackage queuedPackage : pendingPackages) {
            CompoundTag tag = new CompoundTag();
            tag.putString(PENDING_COLOR_TAG, queuedPackage.color().id());
            tag.put(PENDING_DATA_TAG, PackageDataStorage.writeTag(queuedPackage.data()));
            list.add(tag);
        }
        return list;
    }

    private void loadPendingPackages(CompoundTag tag) {
        pendingPackages.clear();
        if (!tag.contains(PENDING_PACKAGES_TAG, Tag.TAG_LIST)) {
            return;
        }
        for (Tag element : tag.getList(PENDING_PACKAGES_TAG, Tag.TAG_COMPOUND)) {
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
            data.ifPresent(packageData -> pendingPackages.add(new QueuedPackage(color.get(), packageData)));
        }
    }

    private static ItemStack packageStack(PackageColor color, PackageData data) {
        ItemStack stack = new ItemStack(APItems.packageItems().get(color).get());
        PackageDataStorage.write(stack, data);
        return stack;
    }

    private PackageCapacityProfile configuredCapacityProfile() {
        return MePackagerBlockEntity.capacityProfileFromItem(items.getStackInSlot(SLOT_CAPACITY))
                .orElse(PackageCapacityProfile.DEFAULT);
    }

    private record AssemblyPlan(PackageColor color, ItemPackagePlan plan) {
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
}

package com.warmthdawn.appliedpackaging.world.block.entity.terminal;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackagePlan;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackageTransactions;
import com.warmthdawn.appliedpackaging.core.package_data.ColoredProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackagedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.InventoryDroppingBlockEntity;
import com.warmthdawn.appliedpackaging.world.menu.PackagePatternTerminalMenu;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;

public class PackagePatternTerminalBlockEntity extends BlockEntity
        implements MenuProvider, InventoryDroppingBlockEntity, PackagePatternTerminalHost {
    public static final int INPUT_SLOT_COUNT = 9;
    public static final int SLOT_BLANK_PATTERN = 9;
    public static final int SLOT_OUTPUT = 10;
    public static final int SLOT_CAPACITY = 11;
    public static final int SLOT_MARKER = 12;
    public static final int PROCESSING_OUTPUT_SLOT_COUNT = 3;
    private static final int SLOT_COUNT = 13;
    private static final String ITEMS_TAG = "items";
    private static final String SELECTED_COLOR_TAG = "selected_color";
    private static final String INPUT_SLOT_COLORS_TAG = "input_slot_colors";
    private static final String PROCESSING_OUTPUTS_TAG = "processing_outputs";
    private static final String PROCESSING_OUTPUT_SLOT_TAG = "slot";
    private static final String PROCESSING_OUTPUT_STACK_TAG = "stack";
    private static final String PROCESSING_OUTPUT_KEY_TAG = "key";
    private static final String PENDING_SPLIT_PATTERNS_TAG = "pending_split_patterns";
    private static final int UNCOLORED_SLOT = -1;

    private PackageColor selectedColor = PackageColor.FLUIX;
    private final int[] inputSlotColors = new int[INPUT_SLOT_COUNT];
    private final ItemStack[] processingOutputs = new ItemStack[PROCESSING_OUTPUT_SLOT_COUNT];
    private final GenericStack[] processingOutputKeys = new GenericStack[PROCESSING_OUTPUT_SLOT_COUNT];
    private final List<ItemStack> pendingSplitPatterns = new ArrayList<>();
    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_BLANK_PATTERN) {
                return isPatternInput(stack);
            }
            if (slot == SLOT_OUTPUT) {
                return isPatternOutput(stack);
            }
            if (slot == SLOT_CAPACITY) {
                return MePackagerBlockEntity.capacityProfileFromItem(stack).isPresent();
            }
            if (slot == SLOT_MARKER) {
                return isMarkerItem(stack);
            }
            if (stack.getItem() instanceof PackageItem) {
                return PackageDataStorage.read(stack).isPresent();
            }
            return true;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == SLOT_CAPACITY || slot == SLOT_MARKER) {
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

    public PackagePatternTerminalBlockEntity(BlockPos pos, BlockState blockState) {
        super(APBlockEntities.PACKAGE_PATTERN_TERMINAL.get(), pos, blockState);
        Arrays.fill(inputSlotColors, UNCOLORED_SLOT);
        Arrays.fill(processingOutputs, ItemStack.EMPTY);
        Arrays.fill(processingOutputKeys, null);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    @Override
    public Level getTerminalLevel() {
        return getLevel();
    }

    @Override
    public BlockPos getTerminalPos() {
        return getBlockPos();
    }

    @Override
    public boolean isTerminalMenuValid(Player player) {
        if (getLevel() == null || isRemoved()) {
            return false;
        }
        BlockPos pos = getBlockPos();
        return player.distanceToSqr(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D) <= 64.0D;
    }

    public PackageColor selectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(PackageColor selectedColor) {
        this.selectedColor = selectedColor;
        setChanged();
    }

    public Optional<PackageColor> inputSlotColor(int slot) {
        if (slot < 0 || slot >= INPUT_SLOT_COUNT) {
            return Optional.empty();
        }
        int color = inputSlotColors[slot];
        PackageColor[] values = PackageColor.values();
        if (color < 0 || color >= values.length) {
            return Optional.empty();
        }
        return Optional.of(values[color]);
    }

    public int inputSlotColorOrdinal(int slot) {
        return inputSlotColor(slot).map(Enum::ordinal).orElse(UNCOLORED_SLOT);
    }

    public void setInputSlotColor(int slot, PackageColor color) {
        if (slot < 0 || slot >= INPUT_SLOT_COUNT || color == null) {
            return;
        }
        inputSlotColors[slot] = color.ordinal();
        setChanged();
    }

    public void setInputSlotColorOrdinal(int slot, int color) {
        if (slot < 0 || slot >= INPUT_SLOT_COUNT) {
            return;
        }
        if (color < 0 || color >= PackageColor.values().length) {
            inputSlotColors[slot] = UNCOLORED_SLOT;
        } else {
            inputSlotColors[slot] = color;
        }
        setChanged();
    }

    public void clearInputSlotColor(int slot) {
        if (slot < 0 || slot >= INPUT_SLOT_COUNT) {
            return;
        }
        inputSlotColors[slot] = UNCOLORED_SLOT;
        setChanged();
    }

    public ItemStack processingOutput(int slot) {
        if (slot < 0 || slot >= processingOutputs.length) {
            return ItemStack.EMPTY;
        }
        return processingOutputs[slot].copy();
    }

    public void setProcessingOutput(int slot, ItemStack stack) {
        if (slot < 0 || slot >= processingOutputs.length) {
            return;
        }
        if (stack.isEmpty()) {
            clearProcessingOutput(slot);
            return;
        }
        ItemStack copy = stack.copy();
        copy.setCount(Math.min(copy.getCount(), copy.getMaxStackSize()));
        setProcessingOutput(slot, copy, itemOutputStack(copy));
    }

    public void setProcessingOutputFromGhostStack(int slot, ItemStack stack, boolean singleContainerOrItem) {
        if (slot < 0 || slot >= processingOutputs.length) {
            return;
        }
        if (stack.isEmpty()) {
            clearProcessingOutput(slot);
            return;
        }

        ItemStack display = stack.copy();
        display.setCount(singleContainerOrItem ? 1 : Math.min(display.getCount(), display.getMaxStackSize()));
        Optional<FluidStack> fluid = FluidUtil.getFluidContained(stack);
        if (fluid.isPresent() && !fluid.get().isEmpty()) {
            FluidStack fluidStack = fluid.get();
            long amount = fluidStack.getAmount();
            if (!singleContainerOrItem) {
                amount *= Math.max(1, stack.getCount());
            }
            display.setCount(1);
            setProcessingOutput(slot, display, new GenericStack(AEFluidKey.of(fluidStack), amount));
            return;
        }

        setProcessingOutput(slot, display);
    }

    public GenericStack processingOutputKey(int slot) {
        if (slot < 0 || slot >= processingOutputKeys.length) {
            return null;
        }
        return processingOutputKeys[slot];
    }

    public long processingOutputAmount(int slot) {
        GenericStack stack = processingOutputKey(slot);
        return stack == null ? 0L : stack.amount();
    }

    public int processingOutputAmountForDisplay(int slot) {
        long amount = processingOutputAmount(slot);
        return amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
    }

    public void adjustProcessingOutputAmount(int slot, boolean increase) {
        if (slot < 0 || slot >= processingOutputKeys.length) {
            return;
        }
        GenericStack current = processingOutputKeys[slot];
        ItemStack display = processingOutputs[slot];
        if (current == null || display.isEmpty()) {
            return;
        }
        long step = amountStep(current);
        long amount = increase
                ? saturatingAdd(current.amount(), step)
                : Math.max(step, current.amount() - step);
        setProcessingOutput(slot, display, new GenericStack(current.what(), amount));
    }

    public void setProcessingOutput(int slot, ItemStack displayStack, GenericStack outputStack) {
        if (slot < 0 || slot >= processingOutputs.length) {
            return;
        }
        if (displayStack.isEmpty() || outputStack == null || outputStack.amount() <= 0) {
            clearProcessingOutput(slot);
            return;
        }
        ItemStack copy = displayStack.copy();
        copy.setCount(Math.max(1, Math.min(copy.getCount(), copy.getMaxStackSize())));
        processingOutputs[slot] = copy;
        processingOutputKeys[slot] = outputStack;
        setChanged();
    }

    public void clearProcessingOutput(int slot) {
        if (slot < 0 || slot >= processingOutputs.length) {
            return;
        }
        processingOutputs[slot] = ItemStack.EMPTY;
        processingOutputKeys[slot] = null;
        setChanged();
    }

    public EncodeResult encodeOnce() {
        if (!items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            return EncodeResult.OUTPUT_BLOCKED;
        }
        ItemStack blankPattern = items.getStackInSlot(SLOT_BLANK_PATTERN);
        if (blankPattern.isEmpty() || isEncodedPackagePattern(blankPattern)) {
            return EncodeResult.NO_PATTERN;
        }
        if (ColoredProcessingPatternDataStorage.canStore(blankPattern)) {
            return encodeColoredProcessingPattern(blankPattern);
        }
        if (!PackagePatternDataStorage.canStore(blankPattern)) {
            return EncodeResult.NO_PATTERN;
        }

        Optional<MarkerSpec> marker = configuredMarker();
        PackageCapacityProfile capacityProfile = configuredCapacityProfile();
        List<ItemPackagePlan> plans = ItemPackageTransactions.planAllPackages(
                inputView,
                selectedColor,
                capacityProfile,
                PackageFilter.any(),
                marker.isPresent() ? MarkerMergeMode.OVERRIDE : MarkerMergeMode.RETAIN,
                marker);
        if (plans.isEmpty()) {
            return EncodeResult.NO_CONTENTS;
        }

        List<GenericStack> processingOutputs = processingOutputStacks();
        ItemStack encoded = createEncodedPatternStack(blankPattern, plans, processingOutputs);
        if (shouldEncodePackagedProcessingPattern(blankPattern, plans, processingOutputs)) {
            PackagedProcessingPatternDataStorage.write(
                    encoded,
                    selectedColor,
                    plans.stream().map(ItemPackagePlan::data).toList(),
                    processingOutputs);
        } else {
            PackagePatternDataStorage.write(encoded, selectedColor, plans.get(0).data());
        }
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, encoded, true);
        if (!remainder.isEmpty()) {
            return EncodeResult.OUTPUT_BLOCKED;
        }

        items.extractItem(SLOT_BLANK_PATTERN, 1, false);
        items.insertItem(SLOT_OUTPUT, encoded, false);
        setChanged();
        return EncodeResult.ENCODED;
    }

    private static ItemStack createEncodedPatternStack(
            ItemStack blankPattern,
            List<ItemPackagePlan> plans,
            List<GenericStack> processingOutputs) {
        if (PackagePatternDataStorage.isAe2BlankPattern(blankPattern) && !processingOutputs.isEmpty()) {
            return PatternDetailsHelper.encodeProcessingPattern(
                    mergedPatternInputs(plans).toArray(GenericStack[]::new),
                    processingOutputs.toArray(GenericStack[]::new));
        }
        return new ItemStack(blankPattern.getItem());
    }

    private static List<GenericStack> mergedPatternInputs(List<ItemPackagePlan> plans) {
        Map<AEKey, Long> merged = new LinkedHashMap<>();
        for (ItemPackagePlan plan : plans) {
            for (GenericStack stack : plan.data().contents()) {
                merged.merge(stack.what(), stack.amount(), Long::sum);
            }
        }
        List<GenericStack> inputs = new ArrayList<>();
        for (var entry : merged.entrySet()) {
            inputs.add(new GenericStack(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(inputs);
    }

    private static boolean shouldEncodePackagedProcessingPattern(
            ItemStack blankPattern,
            List<ItemPackagePlan> plans,
            List<GenericStack> processingOutputs) {
        return blankPattern.is(APItems.PACKAGED_PROCESSING_PATTERN.get())
                || (PackagePatternDataStorage.isAe2BlankPattern(blankPattern)
                        && (!processingOutputs.isEmpty() || plans.size() > 1));
    }

    public SplitResult splitOnce() {
        if (!items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            return SplitResult.OUTPUT_BLOCKED;
        }
        if (emitPendingSplitPattern()) {
            return SplitResult.SPLIT;
        }

        ItemStack source = items.getStackInSlot(SLOT_BLANK_PATTERN);
        Optional<PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern> pattern =
                PackagedProcessingPatternDataStorage.read(source);
        if (pattern.isEmpty()) {
            return SplitResult.NO_PATTERN;
        }

        List<ItemStack> splitPatterns = packagePatternStacks(pattern.get());
        if (splitPatterns.isEmpty()) {
            return SplitResult.NO_PATTERN;
        }

        items.extractItem(SLOT_BLANK_PATTERN, 1, false);
        pendingSplitPatterns.addAll(splitPatterns.subList(1, splitPatterns.size()));
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, splitPatterns.get(0), false);
        if (!remainder.isEmpty()) {
            pendingSplitPatterns.add(0, remainder);
            return SplitResult.OUTPUT_BLOCKED;
        }
        setChanged();
        return SplitResult.SPLIT;
    }

    private boolean emitPendingSplitPattern() {
        if (pendingSplitPatterns.isEmpty()) {
            return false;
        }
        ItemStack next = pendingSplitPatterns.remove(0);
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, next, false);
        if (!remainder.isEmpty()) {
            pendingSplitPatterns.add(0, remainder);
            return false;
        }
        setChanged();
        return true;
    }

    private static List<ItemStack> packagePatternStacks(
            PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern pattern) {
        List<ItemStack> splitPatterns = new ArrayList<>();
        for (PackageData data : pattern.packages()) {
            ItemStack stack = new ItemStack(APItems.PACKAGE_PATTERN.get());
            PackagePatternDataStorage.write(stack, pattern.color(), data);
            splitPatterns.add(stack);
        }
        return splitPatterns;
    }

    private EncodeResult encodeColoredProcessingPattern(ItemStack patternStack) {
        List<GenericStack> sparseInputs = ColoredProcessingPatternDataStorage.readSparseInputs(patternStack);
        Map<Integer, PackageColor> colors = configuredProcessingInputColors(sparseInputs);
        if (colors.isEmpty()) {
            return EncodeResult.NO_CONTENTS;
        }

        ItemStack encoded = patternStack.copy();
        encoded.setCount(1);
        ColoredProcessingPatternDataStorage.write(encoded, colors);
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, encoded.copy(), true);
        if (!remainder.isEmpty()) {
            return EncodeResult.OUTPUT_BLOCKED;
        }

        items.extractItem(SLOT_BLANK_PATTERN, 1, false);
        items.insertItem(SLOT_OUTPUT, encoded, false);
        setChanged();
        return EncodeResult.ENCODED;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.appliedpackaging.package_pattern_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PackagePatternTerminalMenu(containerId, playerInventory, this);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ITEMS_TAG, items.serializeNBT());
        tag.putString(SELECTED_COLOR_TAG, selectedColor.id());
        tag.putIntArray(INPUT_SLOT_COLORS_TAG, inputSlotColors);
        tag.put(PROCESSING_OUTPUTS_TAG, saveProcessingOutputs());
        ListTag pendingTag = new ListTag();
        for (ItemStack pendingPattern : pendingSplitPatterns) {
            pendingTag.add(pendingPattern.save(new CompoundTag()));
        }
        tag.put(PENDING_SPLIT_PATTERNS_TAG, pendingTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ITEMS_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            items.deserializeNBT(tag.getCompound(ITEMS_TAG));
        }
        PackageColor.byId(tag.getString(SELECTED_COLOR_TAG)).ifPresent(color -> selectedColor = color);
        Arrays.fill(inputSlotColors, UNCOLORED_SLOT);
        int[] colors = tag.getIntArray(INPUT_SLOT_COLORS_TAG);
        for (int slot = 0; slot < Math.min(colors.length, inputSlotColors.length); slot++) {
            setInputSlotColorOrdinal(slot, colors[slot]);
        }
        loadProcessingOutputs(tag);
        pendingSplitPatterns.clear();
        ListTag pendingTag = tag.getList(PENDING_SPLIT_PATTERNS_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int index = 0; index < pendingTag.size(); index++) {
            ItemStack stack = ItemStack.of(pendingTag.getCompound(index));
            if (!stack.isEmpty()) {
                pendingSplitPatterns.add(stack);
            }
        }
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        List<ItemStack> drops = new ArrayList<>();
        addContentDrops(drops);
        for (ItemStack stack : drops) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    public void addContentDrops(List<ItemStack> drops) {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        for (ItemStack pendingPattern : pendingSplitPatterns) {
            if (!pendingPattern.isEmpty()) {
                drops.add(pendingPattern.copy());
            }
        }
    }

    public void clearTerminalContent() {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            items.setStackInSlot(slot, ItemStack.EMPTY);
        }
        pendingSplitPatterns.clear();
        setChanged();
    }

    public enum EncodeResult {
        ENCODED("message.appliedpackaging.package_pattern_terminal.encoded"),
        NO_PATTERN("message.appliedpackaging.package_pattern_terminal.no_pattern"),
        NO_CONTENTS("message.appliedpackaging.package_pattern_terminal.no_contents"),
        OUTPUT_BLOCKED("message.appliedpackaging.package_pattern_terminal.output_blocked");

        private final String messageKey;

        EncodeResult(String messageKey) {
            this.messageKey = messageKey;
        }

        public String messageKey() {
            return messageKey;
        }
    }

    public enum SplitResult {
        SPLIT("message.appliedpackaging.package_pattern_terminal.split"),
        NO_PATTERN("message.appliedpackaging.package_pattern_terminal.no_split_pattern"),
        OUTPUT_BLOCKED("message.appliedpackaging.package_pattern_terminal.output_blocked");

        private final String messageKey;

        SplitResult(String messageKey) {
            this.messageKey = messageKey;
        }

        public String messageKey() {
            return messageKey;
        }
    }

    private PackageCapacityProfile configuredCapacityProfile() {
        return MePackagerBlockEntity.capacityProfileFromItem(items.getStackInSlot(SLOT_CAPACITY))
                .orElse(PackageCapacityProfile.DEFAULT);
    }

    private Optional<MarkerSpec> configuredMarker() {
        ItemStack markerStack = items.getStackInSlot(SLOT_MARKER);
        if (!isMarkerItem(markerStack)) {
            return Optional.empty();
        }
        ItemStack keyStack = markerStack.copy();
        keyStack.setCount(1);
        return Optional.of(new MarkerSpec(new GenericStack(AEItemKey.of(keyStack), 1)));
    }

    private Map<Integer, PackageColor> configuredProcessingInputColors(List<GenericStack> sparseInputs) {
        if (sparseInputs.isEmpty()) {
            return Map.of();
        }

        Map<Integer, PackageColor> colors = new LinkedHashMap<>();
        boolean hasConfiguredSlot = false;
        for (int slot = 0; slot < inputSlotColors.length; slot++) {
            if (inputSlotColor(slot).isPresent()) {
                hasConfiguredSlot = true;
                break;
            }
        }

        if (!hasConfiguredSlot) {
            for (int slot = 0; slot < sparseInputs.size(); slot++) {
                GenericStack input = sparseInputs.get(slot);
                if (input != null && input.amount() > 0) {
                    colors.put(slot, selectedColor);
                }
            }
            return colors;
        }

        for (int slot = 0; slot < Math.min(inputSlotColors.length, sparseInputs.size()); slot++) {
            GenericStack input = sparseInputs.get(slot);
            Optional<PackageColor> color = inputSlotColor(slot);
            if (input != null && input.amount() > 0 && color.isPresent()) {
                colors.put(slot, color.get());
            }
        }
        return colors;
    }

    private List<GenericStack> processingOutputStacks() {
        List<GenericStack> outputs = new ArrayList<>();
        for (int slot = 0; slot < processingOutputs.length; slot++) {
            ItemStack output = processingOutputs[slot];
            GenericStack outputKey = processingOutputKeys[slot];
            if (!output.isEmpty() && outputKey != null) {
                outputs.add(outputKey);
            }
        }
        return List.copyOf(outputs);
    }

    private ListTag saveProcessingOutputs() {
        ListTag outputs = new ListTag();
        for (int slot = 0; slot < processingOutputs.length; slot++) {
            ItemStack output = processingOutputs[slot];
            if (output.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt(PROCESSING_OUTPUT_SLOT_TAG, slot);
            entry.put(PROCESSING_OUTPUT_STACK_TAG, output.save(new CompoundTag()));
            GenericStack outputKey = processingOutputKeys[slot];
            if (outputKey != null) {
                entry.put(PROCESSING_OUTPUT_KEY_TAG, GenericStack.writeTag(outputKey));
            }
            outputs.add(entry);
        }
        return outputs;
    }

    private void loadProcessingOutputs(CompoundTag tag) {
        Arrays.fill(processingOutputs, ItemStack.EMPTY);
        Arrays.fill(processingOutputKeys, null);
        if (!tag.contains(PROCESSING_OUTPUTS_TAG, net.minecraft.nbt.Tag.TAG_LIST)) {
            return;
        }
        ListTag outputs = tag.getList(PROCESSING_OUTPUTS_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int index = 0; index < outputs.size(); index++) {
            CompoundTag entry = outputs.getCompound(index);
            int slot = entry.getInt(PROCESSING_OUTPUT_SLOT_TAG);
            if (slot < 0 || slot >= processingOutputs.length
                    || !entry.contains(PROCESSING_OUTPUT_STACK_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                continue;
            }
            ItemStack stack = ItemStack.of(entry.getCompound(PROCESSING_OUTPUT_STACK_TAG));
            if (!stack.isEmpty()) {
                stack.setCount(Math.min(stack.getCount(), stack.getMaxStackSize()));
                GenericStack outputKey = readProcessingOutputKey(entry).orElseGet(() -> itemOutputStack(stack));
                processingOutputs[slot] = stack;
                processingOutputKeys[slot] = outputKey;
            }
        }
    }

    private static Optional<GenericStack> readProcessingOutputKey(CompoundTag entry) {
        if (!entry.contains(PROCESSING_OUTPUT_KEY_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        GenericStack stack = GenericStack.readTag(entry.getCompound(PROCESSING_OUTPUT_KEY_TAG));
        if (stack == null || stack.amount() <= 0) {
            return Optional.empty();
        }
        return Optional.of(stack);
    }

    private static GenericStack itemOutputStack(ItemStack output) {
        return new GenericStack(AEItemKey.of(output), output.getCount());
    }

    private static long amountStep(GenericStack stack) {
        return stack.what() instanceof AEFluidKey ? 1000L : 1L;
    }

    private static long saturatingAdd(long amount, long step) {
        if (Long.MAX_VALUE - amount < step) {
            return Long.MAX_VALUE;
        }
        return amount + step;
    }

    private static boolean isMarkerItem(ItemStack stack) {
        return !stack.isEmpty()
                && !(stack.getItem() instanceof PackageItem)
                && !PackagePatternDataStorage.canStore(stack)
                && !ColoredProcessingPatternDataStorage.canStore(stack);
    }

    private static boolean isPatternInput(ItemStack stack) {
        if (PackagedProcessingPatternDataStorage.read(stack).isPresent()) {
            return true;
        }
        if (ColoredProcessingPatternDataStorage.canStore(stack)) {
            return true;
        }
        return PackagePatternDataStorage.canStore(stack) && !isEncodedPackagePattern(stack);
    }

    private static boolean isPatternOutput(ItemStack stack) {
        return PackagePatternDataStorage.canStore(stack) || ColoredProcessingPatternDataStorage.canStore(stack);
    }

    private static boolean isEncodedPackagePattern(ItemStack stack) {
        return PackagePatternDataStorage.read(stack).isPresent()
                || PackagedProcessingPatternDataStorage.read(stack).isPresent();
    }
}

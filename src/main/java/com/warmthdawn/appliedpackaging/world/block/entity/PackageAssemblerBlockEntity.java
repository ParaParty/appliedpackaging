package com.warmthdawn.appliedpackaging.world.block.entity;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.capabilities.Capabilities;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackagePlan;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackageTransactions;
import com.warmthdawn.appliedpackaging.core.package_data.PackagedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.world.block.InventoryDroppingBlockEntity;
import com.warmthdawn.appliedpackaging.world.menu.PackageAssemblerMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
    private static final int SLOT_COUNT = 11;
    private static final String ITEMS_TAG = "items";

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_OUTPUT) {
                return stack.getItem() instanceof PackageItem;
            }
            if (slot == SLOT_PATTERN) {
                return stack.is(APItems.PACKAGE_PATTERN.get()) || stack.is(APItems.PACKAGED_PROCESSING_PATTERN.get());
            }
            if (stack.getItem() instanceof PackageItem) {
                return PackageDataStorage.read(stack).isPresent();
            }
            return true;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final RangedWrapper inputView = new RangedWrapper(items, 0, INPUT_SLOT_COUNT);
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> items);
    private final LazyOptional<ICraftingMachine> craftingMachine = LazyOptional.of(() -> this);

    public PackageAssemblerBlockEntity(BlockPos pos, BlockState blockState) {
        super(APBlockEntities.PACKAGE_ASSEMBLER.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PackageAssemblerBlockEntity blockEntity) {
        blockEntity.tryAssemble();
    }

    public ItemStackHandler getItems() {
        return items;
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
        AssemblyAttempt attempt = planAssembly(inputView);
        if (attempt.plan().isEmpty()) {
            return attempt.failure();
        }

        return commitAssemblyPlan(inputView, attempt.plan().orElseThrow());
    }

    private AssemblyResult commitAssemblyPlan(IItemHandler input, AssemblyPlan plan) {
        ItemStack packageStack = new ItemStack(APItems.packageItems().get(plan.color()).get());
        PackageDataStorage.write(packageStack, plan.plan().data());
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, packageStack, true);
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

    private AssemblyAttempt planAssembly(IItemHandler input) {
        ItemStack patternStack = items.getStackInSlot(SLOT_PATTERN);
        Optional<PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern> processingPattern =
                PackagedProcessingPatternDataStorage.read(patternStack);
        if (processingPattern.isPresent()) {
            return planProcessingPattern(input, processingPattern.get());
        }

        Optional<PackagePatternDataStorage.EncodedPackagePattern> pattern =
                PackagePatternDataStorage.read(patternStack);
        PackageColor color = pattern.map(PackagePatternDataStorage.EncodedPackagePattern::color)
                .orElse(PackageColor.FLUIX);

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                input,
                color,
                PackageCapacityProfile.DEFAULT);
        if (plan.isEmpty()) {
            return AssemblyAttempt.failed(AssemblyResult.NO_CONTENTS);
        }
        if (pattern.isPresent()
                && !plan.get().data().canonicalHash().equals(pattern.get().data().canonicalHash())) {
            return AssemblyAttempt.failed(AssemblyResult.PATTERN_MISMATCH);
        }
        if (!ItemPackageTransactions.canExtract(input, plan.get())) {
            return AssemblyAttempt.failed(AssemblyResult.SOURCE_CHANGED);
        }
        return AssemblyAttempt.planned(new AssemblyPlan(color, plan.get()));
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

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return new PatternContainerGroup(
                AEItemKey.of(new ItemStack(APBlocks.PACKAGE_ASSEMBLER.get())),
                getDisplayName(),
                List.of());
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, Direction ejectionDirection) {
        if (!acceptsPlans() || inputHolder == null) {
            return false;
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

    @Override
    public boolean acceptsPlans() {
        return items.getStackInSlot(SLOT_OUTPUT).isEmpty() && inputBufferIsEmpty();
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
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ITEMS_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            items.deserializeNBT(tag.getCompound(ITEMS_TAG));
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

    private record AssemblyPlan(PackageColor color, ItemPackagePlan plan) {
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

package com.warmthdawn.appliedpackaging.world.block.entity;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.capabilities.Capabilities;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.config.APServerConfig;
import com.warmthdawn.appliedpackaging.core.item_handler.SimulatedItemHandler;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDetails;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDetails;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.sequence_buffer.SequenceBufferConfiguration;
import com.warmthdawn.appliedpackaging.core.sequence_buffer.SequenceBufferTopology;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.world.block.SequenceBufferBlock;
import com.warmthdawn.appliedpackaging.world.block.SequenceBufferVisualState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class SequenceBufferBlockEntity extends BlockEntity implements ICraftingMachine {
    private static final String STORED_STACK = "stored_stack";
    private static final String RELEASE_AT = "release_at";
    private static final String CONTROLLER_POS = "controller_pos";
    private static final String SEQUENCE_DIRECTION = "sequence_direction";
    private static final String SEQUENCE_INDEX = "sequence_index";
    private static final String CONFIGURATION = "configuration";
    private static final Direction[] OUTPUT_ORDER = {
        Direction.DOWN,
        Direction.UP,
        Direction.NORTH,
        Direction.SOUTH,
        Direction.WEST,
        Direction.EAST
    };

    private final SequenceBufferConfiguration configuration = new SequenceBufferConfiguration();
    private AEKey storedKey;
    private long storedAmount;
    private long releaseAtGameTime;
    private BlockPos controllerPos;
    private Direction sequenceDirection = Direction.NORTH;
    private int sequenceIndex = -1;

    private final MEStorage storageView = new StorageView();
    private final IItemHandler itemView = new ItemView();
    private final IFluidHandler fluidView = new FluidView();
    private LazyOptional<MEStorage> storageCapability = LazyOptional.of(() -> storageView);
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> itemView);
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fluidView);
    private LazyOptional<ICraftingMachine> craftingCapability = LazyOptional.of(() -> this);

    public SequenceBufferBlockEntity(BlockPos pos, BlockState state) {
        super(APBlockEntities.SEQUENCE_BUFFER.get(), pos, state);
        controllerPos = pos.immutable();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            SequenceBufferBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    private void serverTick() {
        if (level == null || level.isClientSide || !configuration.autoOutput()) {
            return;
        }
        if (isEndpoint()) {
            if (configuration.synchronizedOutput()) {
                runSynchronizedOutput();
            }
            return;
        }
        if (!configuration.synchronizedOutput()) {
            tryAutomaticOutput(false);
        }
    }

    public BlockPos controllerPos() {
        return controllerPos;
    }

    public Direction sequenceDirection() {
        return sequenceDirection;
    }

    public int sequenceIndex() {
        return sequenceIndex;
    }

    public AEKey storedKey() {
        return storedKey;
    }

    public long storedAmount() {
        return storedAmount;
    }

    public long releaseAtGameTime() {
        return releaseAtGameTime;
    }

    public boolean isEmpty() {
        return storedKey == null || storedAmount <= 0;
    }

    public boolean canBecomeEndpoint() {
        return isEmpty();
    }

    public boolean isEndpoint() {
        return getBlockState().hasProperty(SequenceBufferBlock.STATE)
                && getBlockState().getValue(SequenceBufferBlock.STATE) == SequenceBufferVisualState.ENDPOINT;
    }

    public SequenceBufferConfiguration configurationCopy() {
        return configuration.copy();
    }

    public void updateConfiguration(SequenceBufferConfiguration updated) {
        if (updated == null) {
            return;
        }
        SequenceBufferBlockEntity authority = SequenceBufferTopology.resolveEndpoint(this).orElse(this);
        authority.configuration.copyFrom(updated);
        authority.setChanged();
        for (SequenceBufferBlockEntity member : SequenceBufferTopology.members(authority)) {
            member.applyControllerConfiguration(updated);
        }
    }

    public void applyControllerConfiguration(SequenceBufferConfiguration updated) {
        if (updated != null && !configuration.equals(updated)) {
            configuration.copyFrom(updated);
            setChanged();
        }
    }

    public void assignTopology(BlockPos controller, Direction direction, int index) {
        controllerPos = controller.immutable();
        sequenceDirection = direction;
        sequenceIndex = index;
        setChanged();
    }

    public void clearTopology() {
        controllerPos = worldPosition.immutable();
        sequenceIndex = -1;
        setChanged();
    }

    public boolean acceptPackage(PackageData data, boolean simulate) {
        return acceptPackage(data, false, simulate);
    }

    public boolean acceptPackage(PackageData data, boolean blocking, boolean simulate) {
        if (data == null || !isEndpoint()) {
            return false;
        }
        if (blocking) {
            for (SequenceBufferBlockEntity member : SequenceBufferTopology.members(this)) {
                if (!member.isEmpty()
                        && data.contents().stream().anyMatch(stack -> stack.what().equals(member.storedKey))) {
                    return false;
                }
            }
        }
        List<PlannedInput> plan = new ArrayList<>(data.contents().size());
        List<Integer> slots = data.layout()
                .map(layout -> layout.contentSlots())
                .orElseGet(() -> {
                    List<Integer> dense = new ArrayList<>(data.contents().size());
                    for (int slot = 0; slot < data.contents().size(); slot++) {
                        dense.add(slot);
                    }
                    return dense;
                });
        for (int i = 0; i < data.contents().size(); i++) {
            plan.add(new PlannedInput(slots.get(i), data.contents().get(i)));
        }
        return applyInputPlan(plan, simulate);
    }

    public void dropStoredContents() {
        if (level == null || level.isClientSide || isEmpty()) {
            return;
        }
        List<ItemStack> drops = new ArrayList<>();
        storedKey.addDrops(storedAmount, drops, level, worldPosition);
        for (ItemStack drop : drops) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), drop);
        }
        clearStored();
    }

    private List<SequenceBufferBlockEntity> storageMembers() {
        return isEndpoint() ? SequenceBufferTopology.members(this) : List.of(this);
    }

    private long insertSingle(AEKey key, long amount, Actionable mode) {
        if (key == null || amount <= 0 || !isEmpty() || isEndpoint() || !configuration.accepts(key)) {
            return 0;
        }
        long accepted = Math.min(amount, APServerConfig.sequenceBufferCapacity());
        if (accepted <= 0) {
            return 0;
        }
        if (!mode.isSimulate()) {
            storedKey = key;
            storedAmount = accepted;
            markInputDelay();
            setChanged();
        }
        return accepted;
    }

    private long insertAggregated(AEKey key, long amount, Actionable mode) {
        if (key == null || amount <= 0 || !configuration.accepts(key)) {
            return 0;
        }
        for (SequenceBufferBlockEntity member : storageMembers()) {
            if (!member.isEmpty()) {
                continue;
            }
            return member.insertSingle(key, amount, mode);
        }
        return 0;
    }

    private long extractSingle(AEKey key, long amount, Actionable mode) {
        if (key == null
                || amount <= 0
                || isEmpty()
                || !storedKey.equals(key)
                || extractionBlocked()) {
            return 0;
        }
        long extracted = Math.min(amount, storedAmount);
        if (!mode.isSimulate()) {
            storedAmount -= extracted;
            if (storedAmount == 0) {
                storedKey = null;
            }
            setChanged();
        }
        return extracted;
    }

    private long extractAggregated(AEKey key, long amount, Actionable mode) {
        if (amount <= 0 || extractionBlocked()) {
            return 0;
        }
        long remaining = amount;
        long extracted = 0;
        for (SequenceBufferBlockEntity member : storageMembers()) {
            long current = member.extractSingle(key, remaining, mode);
            extracted += current;
            remaining -= current;
            if (remaining == 0) {
                break;
            }
        }
        return extracted;
    }

    private boolean extractionBlocked() {
        if (level == null) {
            return true;
        }
        SequenceBufferBlockEntity authority = SequenceBufferTopology.resolveEndpoint(this).orElse(this);
        return level.getGameTime() < authority.releaseAtGameTime;
    }

    private void markInputDelay() {
        if (level == null) {
            return;
        }
        long releaseAt = level.getGameTime() + Math.max(1, configuration.inputDelayTicks());
        releaseAtGameTime = Math.max(releaseAtGameTime, releaseAt);
        SequenceBufferBlockEntity authority = SequenceBufferTopology.resolveEndpoint(this).orElse(this);
        authority.releaseAtGameTime = Math.max(authority.releaseAtGameTime, releaseAt);
        authority.setChanged();
    }

    private void clearStored() {
        storedKey = null;
        storedAmount = 0;
        setChanged();
    }

    private void tryAutomaticOutput(boolean requireFullAmount) {
        if (isEmpty() || extractionBlocked()) {
            return;
        }
        TransferTarget target = findTransferTarget(storedKey, storedAmount, requireFullAmount).orElse(null);
        if (target == null) {
            return;
        }
        long inserted = target.insert(storedKey, storedAmount, false);
        if (inserted > 0) {
            storedAmount -= inserted;
            if (storedAmount == 0) {
                storedKey = null;
            }
            setChanged();
        }
    }

    private void runSynchronizedOutput() {
        if (extractionBlocked()) {
            return;
        }
        List<PlannedOutput> plan = new ArrayList<>();
        for (SequenceBufferBlockEntity member : SequenceBufferTopology.members(this)) {
            if (member.isEmpty()) {
                continue;
            }
            TransferTarget target = member.findTransferTarget(member.storedKey, member.storedAmount, true).orElse(null);
            if (target == null) {
                return;
            }
            plan.add(new PlannedOutput(member, target, member.storedKey, member.storedAmount));
        }
        if (plan.isEmpty()) {
            return;
        }
        for (PlannedOutput output : plan) {
            long inserted = output.target().insert(output.key(), output.amount(), false);
            if (inserted > 0) {
                output.member().storedAmount -= inserted;
                if (output.member().storedAmount == 0) {
                    output.member().storedKey = null;
                }
                output.member().setChanged();
            }
        }
    }

    private Optional<TransferTarget> findTransferTarget(AEKey key, long amount, boolean requireFullAmount) {
        if (level == null || key == null || amount <= 0) {
            return Optional.empty();
        }
        for (Direction direction : outputDirections()) {
            BlockEntity targetEntity = level.getBlockEntity(worldPosition.relative(direction));
            if (targetEntity == null || targetEntity instanceof SequenceBufferBlockEntity) {
                continue;
            }
            Direction targetSide = direction.getOpposite();
            List<TransferTarget> candidates = new ArrayList<>(2);
            targetEntity.getCapability(Capabilities.STORAGE, targetSide)
                    .resolve()
                    .ifPresent(storage -> candidates.add(new METransferTarget(storage)));
            if (key instanceof AEItemKey itemKey) {
                targetEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, targetSide)
                        .resolve()
                        .ifPresent(handler -> candidates.add(new ItemTransferTarget(handler, itemKey)));
            } else if (key instanceof AEFluidKey fluidKey) {
                targetEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, targetSide)
                        .resolve()
                        .ifPresent(handler -> candidates.add(new FluidTransferTarget(handler, fluidKey)));
            }
            for (TransferTarget candidate : candidates) {
                if (configuration.blockingMode() && !candidate.isEmpty()) {
                    continue;
                }
                long accepted = candidate.insert(key, amount, true);
                if (accepted > 0 && (!requireFullAmount || accepted == amount)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    private List<Direction> outputDirections() {
        BlockState state = getBlockState();
        if (state.hasProperty(SequenceBufferBlock.STATE)
                && SequenceBufferBlock.hasOwnDirection(state)) {
            return List.of(state.getValue(SequenceBufferBlock.FACING));
        }
        return Arrays.asList(OUTPUT_ORDER);
    }

    private boolean applyInputPlan(List<PlannedInput> plan, boolean simulate) {
        if (!isEndpoint() || plan.isEmpty()) {
            return false;
        }
        List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(this);
        for (PlannedInput input : plan) {
            if (input.memberSlot() < 0 || input.memberSlot() >= members.size()) {
                return false;
            }
            GenericStack stack = input.stack();
            SequenceBufferBlockEntity member = members.get(input.memberSlot());
            if (stack == null
                    || stack.what() == null
                    || stack.amount() <= 0
                    || stack.amount() > APServerConfig.sequenceBufferCapacity()
                    || !member.isEmpty()
                    || !configuration.accepts(stack.what())) {
                return false;
            }
        }
        if (simulate) {
            return true;
        }
        for (PlannedInput input : plan) {
            SequenceBufferBlockEntity member = members.get(input.memberSlot());
            member.storedKey = input.stack().what();
            member.storedAmount = input.stack().amount();
            member.setChanged();
        }
        markInputDelay();
        return true;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return PatternContainerGroup.nothing();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, Direction side) {
        if (!acceptsPlans() || patternDetails == null || inputHolder == null) {
            return false;
        }
        List<PlannedInput> plan = patternPlan(patternDetails).orElse(null);
        if (plan == null || !inputsExactlyAvailable(plan, inputHolder) || !applyInputPlan(plan, true)) {
            return false;
        }
        if (!applyInputPlan(plan, false)) {
            return false;
        }
        consumeInputs(plan, inputHolder);
        return true;
    }

    @Override
    public boolean acceptsPlans() {
        return isEndpoint() && !SequenceBufferTopology.members(this).isEmpty();
    }

    private Optional<List<PlannedInput>> patternPlan(IPatternDetails details) {
        if (details instanceof AdvancedProcessingPatternDetails advanced) {
            List<GenericStack> dense = Arrays.stream(advanced.getSparseInputs())
                    .filter(stack -> stack != null && stack.amount() > 0)
                    .toList();
            return Optional.of(densePlan(dense));
        }
        if (details instanceof PackageCraftingPatternDetails packagePattern) {
            return Optional.of(sparsePlan(packagePattern.sparseInputs()));
        }
        if (details instanceof AECraftingPattern craftingPattern) {
            return Optional.of(sparsePlan(craftingPattern.getSparseInputs()));
        }
        if (details instanceof AEProcessingPattern processingPattern) {
            return Optional.of(sparsePlan(processingPattern.getSparseInputs()));
        }
        if (configuration.patternMode()) {
            return Optional.empty();
        }
        List<GenericStack> dense = new ArrayList<>();
        for (IPatternDetails.IInput input : details.getInputs()) {
            GenericStack[] possible = input.getPossibleInputs();
            if (possible.length == 0 || possible[0] == null) {
                continue;
            }
            GenericStack primary = possible[0];
            dense.add(new GenericStack(primary.what(), Math.multiplyExact(primary.amount(), input.getMultiplier())));
        }
        return Optional.of(densePlan(dense));
    }

    private static List<PlannedInput> sparsePlan(GenericStack[] sparse) {
        List<PlannedInput> plan = new ArrayList<>();
        for (int slot = 0; slot < sparse.length; slot++) {
            GenericStack stack = sparse[slot];
            if (stack != null && stack.amount() > 0) {
                plan.add(new PlannedInput(slot, new GenericStack(stack.what(), stack.amount())));
            }
        }
        return List.copyOf(plan);
    }

    private static List<PlannedInput> densePlan(List<GenericStack> dense) {
        List<PlannedInput> plan = new ArrayList<>();
        for (GenericStack stack : dense) {
            if (stack != null && stack.amount() > 0) {
                plan.add(new PlannedInput(plan.size(), new GenericStack(stack.what(), stack.amount())));
            }
        }
        return List.copyOf(plan);
    }

    private static boolean inputsExactlyAvailable(List<PlannedInput> plan, KeyCounter[] holders) {
        Map<AEKey, Long> expected = new HashMap<>();
        for (PlannedInput input : plan) {
            expected.merge(input.stack().what(), input.stack().amount(), Math::addExact);
        }
        Map<AEKey, Long> actual = new HashMap<>();
        for (KeyCounter holder : holders) {
            if (holder == null) {
                continue;
            }
            for (var entry : holder) {
                if (entry.getLongValue() > 0) {
                    actual.merge(entry.getKey(), entry.getLongValue(), Math::addExact);
                }
            }
        }
        return expected.equals(actual);
    }

    private static void consumeInputs(List<PlannedInput> plan, KeyCounter[] holders) {
        for (PlannedInput input : plan) {
            long remaining = input.stack().amount();
            for (KeyCounter holder : holders) {
                if (holder == null || remaining == 0) {
                    continue;
                }
                long available = holder.get(input.stack().what());
                long removed = Math.min(available, remaining);
                if (removed > 0) {
                    holder.remove(input.stack().what(), removed);
                    remaining -= removed;
                }
            }
        }
        for (KeyCounter holder : holders) {
            if (holder != null) {
                holder.removeZeros();
                holder.removeEmptySubmaps();
            }
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == Capabilities.STORAGE) {
            return storageCapability.cast();
        }
        if (capability == Capabilities.CRAFTING_MACHINE) {
            return Capabilities.CRAFTING_MACHINE.orEmpty(capability, craftingCapability);
        }
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        storageCapability.invalidate();
        itemCapability.invalidate();
        fluidCapability.invalidate();
        craftingCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        storageCapability = LazyOptional.of(() -> storageView);
        itemCapability = LazyOptional.of(() -> itemView);
        fluidCapability = LazyOptional.of(() -> fluidView);
        craftingCapability = LazyOptional.of(() -> this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!isEmpty()) {
            tag.put(STORED_STACK, GenericStack.writeTag(new GenericStack(storedKey, storedAmount)));
        }
        tag.putLong(RELEASE_AT, releaseAtGameTime);
        tag.putLong(CONTROLLER_POS, controllerPos.asLong());
        tag.putInt(SEQUENCE_DIRECTION, sequenceDirection.get3DDataValue());
        tag.putInt(SEQUENCE_INDEX, sequenceIndex);
        tag.put(CONFIGURATION, configuration.writeTag());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        configuration.readTag(tag.getCompound(CONFIGURATION));
        GenericStack stored = tag.contains(STORED_STACK, Tag.TAG_COMPOUND)
                ? GenericStack.readTag(tag.getCompound(STORED_STACK))
                : null;
        if (stored != null && stored.what() != null && stored.amount() > 0) {
            storedKey = stored.what();
            storedAmount = stored.amount();
            if (storedAmount > APServerConfig.sequenceBufferCapacity()) {
                AppliedPackaging.LOGGER.warn(
                        "Sequence Buffer at {} loaded {} units of {}, above the current configured capacity {}; preserving the stored resource until it is extracted",
                        worldPosition,
                        storedAmount,
                        storedKey,
                        APServerConfig.sequenceBufferCapacity());
            }
        } else {
            storedKey = null;
            storedAmount = 0;
        }
        releaseAtGameTime = Math.max(0, tag.getLong(RELEASE_AT));
        controllerPos = tag.contains(CONTROLLER_POS, Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong(CONTROLLER_POS))
                : worldPosition.immutable();
        Direction loadedDirection = Direction.from3DDataValue(tag.getInt(SEQUENCE_DIRECTION));
        sequenceDirection = loadedDirection;
        sequenceIndex = tag.contains(SEQUENCE_INDEX, Tag.TAG_INT) ? tag.getInt(SEQUENCE_INDEX) : -1;
    }

    private final class StorageView implements MEStorage {
        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            MEStorage.checkPreconditions(what, amount, mode, source);
            return isEndpoint()
                    ? insertAggregated(what, amount, mode)
                    : insertSingle(what, amount, mode);
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            MEStorage.checkPreconditions(what, amount, mode, source);
            return isEndpoint()
                    ? extractAggregated(what, amount, mode)
                    : extractSingle(what, amount, mode);
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            for (SequenceBufferBlockEntity member : storageMembers()) {
                if (!member.isEmpty()) {
                    out.add(member.storedKey, member.storedAmount);
                }
            }
        }

        @Override
        public Component getDescription() {
            return Component.translatable("block.appliedpackaging.sequence_buffer");
        }
    }

    private final class ItemView implements IItemHandler {
        @Override
        public int getSlots() {
            return storageMembers().size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            SequenceBufferBlockEntity member = memberAt(slot);
            if (member == null || !(member.storedKey instanceof AEItemKey itemKey) || member.storedAmount <= 0) {
                return ItemStack.EMPTY;
            }
            return itemKey.toStack((int) Math.min(Integer.MAX_VALUE, member.storedAmount));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || slot < 0 || slot >= getSlots()) {
                return stack;
            }
            AEItemKey key = AEItemKey.of(stack);
            long accepted = isEndpoint()
                    ? insertAggregated(key, stack.getCount(), simulate ? Actionable.SIMULATE : Actionable.MODULATE)
                    : insertSingle(key, stack.getCount(), simulate ? Actionable.SIMULATE : Actionable.MODULATE);
            if (accepted <= 0) {
                return stack;
            }
            ItemStack remainder = stack.copy();
            remainder.shrink((int) accepted);
            return remainder;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            SequenceBufferBlockEntity member = memberAt(slot);
            if (member == null || amount <= 0 || !(member.storedKey instanceof AEItemKey itemKey)) {
                return ItemStack.EMPTY;
            }
            long extracted = member.extractSingle(
                    itemKey,
                    amount,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE);
            return extracted <= 0 ? ItemStack.EMPTY : itemKey.toStack((int) extracted);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot >= 0 && slot < getSlots()
                    ? (int) Math.min(Integer.MAX_VALUE, APServerConfig.sequenceBufferCapacity())
                    : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 0
                    && slot < getSlots()
                    && !stack.isEmpty()
                    && configuration.accepts(AEItemKey.of(stack));
        }

        private SequenceBufferBlockEntity memberAt(int slot) {
            List<SequenceBufferBlockEntity> members = storageMembers();
            return slot >= 0 && slot < members.size() ? members.get(slot) : null;
        }
    }

    private final class FluidView implements IFluidHandler {
        @Override
        public int getTanks() {
            return storageMembers().size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            SequenceBufferBlockEntity member = memberAt(tank);
            if (member == null || !(member.storedKey instanceof AEFluidKey fluidKey) || member.storedAmount <= 0) {
                return FluidStack.EMPTY;
            }
            return fluidKey.toStack((int) Math.min(Integer.MAX_VALUE, member.storedAmount));
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank >= 0 && tank < getTanks()
                    ? (int) Math.min(Integer.MAX_VALUE, APServerConfig.sequenceBufferCapacity())
                    : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank >= 0
                    && tank < getTanks()
                    && !stack.isEmpty()
                    && configuration.accepts(AEFluidKey.of(stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            AEFluidKey key = AEFluidKey.of(resource);
            long accepted = isEndpoint()
                    ? insertAggregated(
                            key,
                            resource.getAmount(),
                            action.simulate() ? Actionable.SIMULATE : Actionable.MODULATE)
                    : insertSingle(
                            key,
                            resource.getAmount(),
                            action.simulate() ? Actionable.SIMULATE : Actionable.MODULATE);
            return (int) Math.min(Integer.MAX_VALUE, accepted);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            AEFluidKey key = AEFluidKey.of(resource);
            long extracted = isEndpoint()
                    ? extractAggregated(
                            key,
                            resource.getAmount(),
                            action.simulate() ? Actionable.SIMULATE : Actionable.MODULATE)
                    : extractSingle(
                            key,
                            resource.getAmount(),
                            action.simulate() ? Actionable.SIMULATE : Actionable.MODULATE);
            return extracted <= 0 ? FluidStack.EMPTY : key.toStack((int) extracted);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            for (SequenceBufferBlockEntity member : storageMembers()) {
                if (member.storedKey instanceof AEFluidKey fluidKey && member.storedAmount > 0) {
                    return drain(fluidKey.toStack(maxDrain), action);
                }
            }
            return FluidStack.EMPTY;
        }

        private SequenceBufferBlockEntity memberAt(int tank) {
            List<SequenceBufferBlockEntity> members = storageMembers();
            return tank >= 0 && tank < members.size() ? members.get(tank) : null;
        }
    }

    private interface TransferTarget {
        long insert(AEKey key, long amount, boolean simulate);

        boolean isEmpty();
    }

    private record METransferTarget(MEStorage storage) implements TransferTarget {
        @Override
        public long insert(AEKey key, long amount, boolean simulate) {
            return storage.insert(
                    key,
                    amount,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE,
                    IActionSource.empty());
        }

        @Override
        public boolean isEmpty() {
            return storage.getAvailableStacks().isEmpty();
        }
    }

    private record ItemTransferTarget(IItemHandler handler, AEItemKey expectedKey) implements TransferTarget {
        @Override
        public long insert(AEKey key, long amount, boolean simulate) {
            if (!expectedKey.equals(key)) {
                return 0;
            }
            IItemHandler target = simulate ? SimulatedItemHandler.copyOf(handler) : handler;
            long remaining = amount;
            while (remaining > 0) {
                int batch = (int) Math.min(remaining, expectedKey.getMaxStackSize());
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, expectedKey.toStack(batch), false);
                int accepted = batch - remainder.getCount();
                remaining -= accepted;
                if (accepted < batch) {
                    break;
                }
            }
            return amount - remaining;
        }

        @Override
        public boolean isEmpty() {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (!handler.getStackInSlot(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    private record FluidTransferTarget(IFluidHandler handler, AEFluidKey expectedKey) implements TransferTarget {
        @Override
        public long insert(AEKey key, long amount, boolean simulate) {
            if (!expectedKey.equals(key)) {
                return 0;
            }
            return handler.fill(
                    expectedKey.toStack((int) Math.min(Integer.MAX_VALUE, amount)),
                    simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        }

        @Override
        public boolean isEmpty() {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                if (!handler.getFluidInTank(tank).isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    private record PlannedInput(int memberSlot, GenericStack stack) {
    }

    private record PlannedOutput(
            SequenceBufferBlockEntity member,
            TransferTarget target,
            AEKey key,
            long amount) {
    }
}

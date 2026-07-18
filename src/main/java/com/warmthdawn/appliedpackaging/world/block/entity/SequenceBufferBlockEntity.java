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
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.capabilities.Capabilities;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.helpers.externalstorage.GenericStackInv;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.config.APServerConfig;
import com.warmthdawn.appliedpackaging.core.item_handler.SimulatedItemHandler;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDetails;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDetails;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageLayout;
import com.warmthdawn.appliedpackaging.core.sequence_buffer.SequenceBufferConfiguration;
import com.warmthdawn.appliedpackaging.core.sequence_buffer.SequenceBufferTopology;
import com.warmthdawn.appliedpackaging.diagnostic.RoutingTrace;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.world.block.SequenceBufferBlock;
import com.warmthdawn.appliedpackaging.world.block.SequenceBufferVisualState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
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

public class SequenceBufferBlockEntity extends BlockEntity implements ICraftingMachine, IUpgradeableObject {
    private static final String STORED_STACK = "stored_stack";
    private static final String RELEASE_AT = "release_at";
    private static final String ADMISSION_OPEN_AT = "admission_open_at";
    private static final String CONTROLLER_POS = "controller_pos";
    private static final String SEQUENCE_DIRECTION = "sequence_direction";
    private static final String SEQUENCE_INDEX = "sequence_index";
    private static final String CONFIGURATION = "configuration";
    private static final String UPGRADES = "upgrades";
    public static final int FILTER_SLOT_COUNT = 9;
    public static final int UPGRADE_SLOT_COUNT = 1;
    private static final int REPEATED_TRACE_INTERVAL_TICKS = 20;
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
    private long admissionOpenAtGameTime = Long.MIN_VALUE;
    private BlockPos controllerPos;
    private Direction sequenceDirection = Direction.NORTH;
    private int sequenceIndex = -1;
    private boolean synchronizingInputFilter;
    private final Map<String, Long> repeatedTraceTicks = new HashMap<>();

    private final GenericStackInv inputFilter = new GenericStackInv(
            this::onInputFilterChanged,
            GenericStackInv.Mode.CONFIG_TYPES,
            FILTER_SLOT_COUNT);
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            APBlocks.SEQUENCE_BUFFER.get(),
            UPGRADE_SLOT_COUNT,
            this::onUpgradesChanged);

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
        if (level == null || level.isClientSide) {
            return;
        }
        if (getBlockState().hasProperty(SequenceBufferBlock.STATE)
                && getBlockState().getValue(SequenceBufferBlock.STATE).isMember()) {
            return;
        }

        List<SequenceBufferBlockEntity> tickMembers = isEndpoint()
                ? SequenceBufferTopology.members(this)
                : List.of(this);
        SequenceBufferConfiguration runtimeConfiguration = effectiveConfiguration();

        if (!runtimeConfiguration.autoOutput() || !redstoneAllowsAutomaticOutput()) {
            return;
        }
        if (isEndpoint()) {
            if (runtimeConfiguration.synchronizedOutput()) {
                runSynchronizedOutput();
            } else {
                for (SequenceBufferBlockEntity member : tickMembers) {
                    member.tryAutomaticOutput(false);
                }
            }
            return;
        }
        tryAutomaticOutput(false);
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

    public GenericStackInv inputFilter() {
        return inputFilter;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    public boolean hasRedstoneCard() {
        return upgrades.isInstalled(AEItems.REDSTONE_CARD);
    }

    public void absorbUpgradesFrom(SequenceBufferBlockEntity source) {
        if (source == null || source == this || level == null || level.isClientSide) {
            return;
        }
        for (int slot = 0; slot < source.upgrades.size(); slot++) {
            ItemStack extracted = source.upgrades.extractItem(slot, Integer.MAX_VALUE, false);
            if (extracted.isEmpty()) {
                continue;
            }
            ItemStack remainder = upgrades.addItems(extracted);
            if (!remainder.isEmpty()) {
                Containers.dropItemStack(
                        level,
                        source.worldPosition.getX(),
                        source.worldPosition.getY(),
                        source.worldPosition.getZ(),
                        remainder);
            }
        }
        source.setChanged();
        setChanged();
    }

    public long releaseAtGameTime() {
        return releaseAtGameTime;
    }

    public long admissionOpenAtGameTime() {
        return admissionOpenAtGameTime;
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
        return effectiveConfiguration().copy();
    }

    private SequenceBufferConfiguration effectiveConfiguration() {
        return SequenceBufferTopology.resolveEndpoint(this)
                .map(endpoint -> endpoint.configuration)
                .orElse(configuration);
    }

    private int effectiveInputDelayTicks() {
        return SequenceBufferTopology.resolveEndpoint(this)
                .map(endpoint -> Math.max(0, endpoint.configuration.inputDelayTicks()))
                .orElse(0);
    }

    public void updateConfiguration(SequenceBufferConfiguration updated) {
        if (updated == null) {
            return;
        }
        SequenceBufferBlockEntity authority = SequenceBufferTopology.resolveEndpoint(this).orElse(this);
        authority.configuration.copyFrom(updated);
        authority.synchronizeInputFilterFromConfiguration();
        authority.setChanged();
    }

    public int storageMemberCount() {
        return storageMembers().size();
    }

    public SequenceBufferBlockEntity storageMemberAt(int slot) {
        List<SequenceBufferBlockEntity> members = storageMembers();
        return slot >= 0 && slot < members.size() ? members.get(slot) : null;
    }

    public ItemStack menuDisplayStack() {
        return isEmpty()
                ? ItemStack.EMPTY
                : GenericStack.wrapInItemStack(new GenericStack(storedKey, storedAmount));
    }

    public int insertMenuItem(ItemStack stack, int amount, boolean simulate) {
        if (stack.isEmpty() || amount <= 0) {
            return 0;
        }
        long inserted = insertSingle(
                AEItemKey.of(stack),
                Math.min(amount, stack.getCount()),
                simulate ? Actionable.SIMULATE : Actionable.MODULATE);
        return (int) Math.min(Integer.MAX_VALUE, inserted);
    }

    public ItemStack extractMenuItem(int amount, boolean simulate) {
        if (amount <= 0 || !(storedKey instanceof AEItemKey itemKey)) {
            return ItemStack.EMPTY;
        }
        long extracted = extractSingle(
                itemKey,
                amount,
                simulate ? Actionable.SIMULATE : Actionable.MODULATE,
                true);
        return extracted <= 0 ? ItemStack.EMPTY : itemKey.toStack((int) extracted);
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
        SequenceBufferConfiguration runtimeConfiguration = effectiveConfiguration();
        trace(
                "package_accept_attempt",
                "action=" + (simulate ? "SIMULATE" : "MODULATE")
                        + " busBlocking=" + blocking
                        + " patternMode=" + runtimeConfiguration.patternMode()
                        + " data=" + RoutingTrace.packageData(data)
                        + " members=" + traceMembers());
        if (data == null) {
            trace(
                    "package_accept_rejected",
                    "reason=null_data action=" + (simulate ? "SIMULATE" : "MODULATE"));
            return false;
        }
        if (!isEndpoint()) {
            trace(
                    "package_accept_rejected",
                    "reason=not_endpoint action=" + (simulate ? "SIMULATE" : "MODULATE"));
            return false;
        }
        if (blocking) {
            List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(this);
            for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
                SequenceBufferBlockEntity member = members.get(memberIndex);
                if (!member.isEmpty()
                        && data.contents().stream().anyMatch(stack -> stack.what().equals(member.storedKey))) {
                    trace(
                            "package_accept_rejected",
                            "reason=bus_blocking_matching_member"
                                    + " action=" + (simulate ? "SIMULATE" : "MODULATE")
                                    + " memberSlot=" + memberIndex
                                    + " memberPos=" + member.worldPosition.toShortString()
                                    + " memberKey=" + RoutingTrace.key(member.storedKey)
                                    + " memberAmount=" + member.storedAmount);
                    return false;
                }
            }
        }
        List<PlannedInput> plan = new ArrayList<>(data.contents().size());
        Optional<PackageLayout> activeLayout = runtimeConfiguration.patternMode()
                ? data.layout()
                : Optional.empty();
        List<Integer> slots = activeLayout
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
        trace(
                "package_input_plan",
                "action=" + (simulate ? "SIMULATE" : "MODULATE")
                        + " layout=" + activeLayout.map(value -> value.slotCount() + ":" + value.contentSlots())
                                .orElse("dense")
                        + " plan=" + tracePlan(plan));
        boolean accepted = applyInputPlan(plan, simulate);
        trace(
                accepted ? "package_accept_succeeded" : "package_accept_rejected",
                "reason=" + (accepted ? "input_plan_applied" : "input_plan_rejected")
                        + " action=" + (simulate ? "SIMULATE" : "MODULATE")
                        + " plan=" + tracePlan(plan)
                        + " members=" + traceMembers());
        return accepted;
    }

    public void dropStoredContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!isEmpty()) {
            List<ItemStack> drops = new ArrayList<>();
            storedKey.addDrops(storedAmount, drops, level, worldPosition);
            for (ItemStack drop : drops) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), drop);
            }
            clearStored();
        }
        for (ItemStack upgrade : upgrades) {
            if (!upgrade.isEmpty()) {
                Containers.dropItemStack(
                        level,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        upgrade.copy());
            }
        }
    }

    private List<SequenceBufferBlockEntity> storageMembers() {
        return isEndpoint() ? SequenceBufferTopology.members(this) : List.of(this);
    }

    private long insertSingle(AEKey key, long amount, Actionable mode) {
        SequenceBufferConfiguration runtimeConfiguration = effectiveConfiguration();
        if (key == null
                || amount <= 0
                || !isEmpty()
                || inputAdmissionBlocked()
                || isEndpoint()
                || !runtimeConfiguration.accepts(key)) {
            return 0;
        }
        long accepted = Math.min(amount, APServerConfig.sequenceBufferCapacity());
        if (accepted <= 0) {
            return 0;
        }
        if (runtimeConfiguration.antiClogMode() && findTransferTarget(key, accepted, true).isEmpty()) {
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
        if (key == null || amount <= 0 || !effectiveConfiguration().accepts(key)) {
            return 0;
        }
        for (SequenceBufferBlockEntity member : storageMembers()) {
            if (!member.isEmpty() || member.inputAdmissionBlocked()) {
                continue;
            }
            long inserted = member.insertSingle(key, amount, mode);
            if (inserted > 0) {
                return inserted;
            }
        }
        return 0;
    }

    private long extractSingle(AEKey key, long amount, Actionable mode) {
        return extractSingle(key, amount, mode, false);
    }

    private long extractSingle(AEKey key, long amount, Actionable mode, boolean ignoreOutputDelay) {
        if (key == null
                || amount <= 0
                || isEmpty()
                || !storedKey.equals(key)
                || (!ignoreOutputDelay && extractionBlocked())) {
            return 0;
        }
        long extracted = Math.min(amount, storedAmount);
        if (!mode.isSimulate()) {
            removeStoredAmount(extracted);
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

    private boolean redstoneAllowsAutomaticOutput() {
        SequenceBufferBlockEntity authority = SequenceBufferTopology.resolveEndpoint(this).orElse(this);
        return !authority.hasRedstoneCard()
                || authority.level == null
                || authority.level.hasNeighborSignal(authority.worldPosition);
    }

    private void onInputFilterChanged() {
        if (synchronizingInputFilter) {
            return;
        }
        List<AEKey> allowed = new ArrayList<>(FILTER_SLOT_COUNT);
        for (int slot = 0; slot < inputFilter.size(); slot++) {
            AEKey key = inputFilter.getKey(slot);
            if (key != null) {
                allowed.add(key);
            }
        }
        SequenceBufferConfiguration updated = effectiveConfiguration().copy();
        updated.setAllowedInputs(allowed);
        updateConfiguration(updated);
    }

    private void synchronizeInputFilterFromConfiguration() {
        synchronizingInputFilter = true;
        inputFilter.beginBatch();
        try {
            int slot = 0;
            for (AEKey key : configuration.allowedInputs()) {
                if (slot >= inputFilter.size()) {
                    break;
                }
                inputFilter.setStack(slot++, new GenericStack(key, 0));
            }
            while (slot < inputFilter.size()) {
                inputFilter.setStack(slot++, null);
            }
        } finally {
            inputFilter.endBatchSuppressed();
            synchronizingInputFilter = false;
        }
    }

    private void onUpgradesChanged() {
        setChanged();
    }

    private void markInputDelay() {
        if (level == null) {
            return;
        }
        long releaseAt = level.getGameTime() + effectiveInputDelayTicks();
        releaseAtGameTime = Math.max(releaseAtGameTime, releaseAt);
        SequenceBufferBlockEntity authority = SequenceBufferTopology.resolveEndpoint(this).orElse(this);
        authority.releaseAtGameTime = Math.max(authority.releaseAtGameTime, releaseAt);
        authority.setChanged();
        trace(
                "input_delay_marked",
                "releaseAt=" + releaseAt
                        + " authority=" + authority.worldPosition.toShortString()
                        + " authorityReleaseAt=" + authority.releaseAtGameTime);
    }

    private boolean inputAdmissionBlocked() {
        return level == null || level.getGameTime() < admissionOpenAtGameTime;
    }

    private void markEmptyUntilNextGameTick() {
        if (level == null) {
            return;
        }
        long gameTime = level.getGameTime();
        long openAt = gameTime == Long.MAX_VALUE ? Long.MAX_VALUE : gameTime + 1;
        admissionOpenAtGameTime = Math.max(admissionOpenAtGameTime, openAt);
    }

    private void removeStoredAmount(long amount) {
        if (amount <= 0 || isEmpty()) {
            return;
        }
        storedAmount = Math.max(0, storedAmount - amount);
        if (storedAmount == 0) {
            storedKey = null;
            markEmptyUntilNextGameTick();
        }
        setChanged();
    }

    private void clearStored() {
        storedKey = null;
        storedAmount = 0;
        setChanged();
    }

    private void tryAutomaticOutput(boolean requireFullAmount) {
        if (isEmpty()) {
            return;
        }
        if (extractionBlocked()) {
            traceRepeated(
                    "auto_output_blocked",
                    "reason=input_delay mode=individual key=" + RoutingTrace.key(storedKey)
                            + " amount=" + storedAmount
                            + " releaseAt=" + releaseAtGameTime);
            return;
        }
        TransferTarget target = findTransferTarget(storedKey, storedAmount, requireFullAmount).orElse(null);
        if (target == null) {
            traceRepeated(
                    "auto_output_blocked",
                    "reason=no_transfer_target mode=individual key=" + RoutingTrace.key(storedKey)
                            + " amount=" + storedAmount);
            return;
        }
        AEKey outputKey = storedKey;
        long requested = storedAmount;
        long inserted = target.insert(storedKey, storedAmount, false);
        trace(
                "auto_output_commit",
                "mode=individual target=" + targetName(target)
                        + " key=" + RoutingTrace.key(outputKey)
                        + " requested=" + requested
                        + " inserted=" + inserted);
        if (inserted > 0) {
            removeStoredAmount(inserted);
        }
    }

    private void runSynchronizedOutput() {
        List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(this);
        boolean hasContents = members.stream().anyMatch(member -> !member.isEmpty());
        if (!hasContents) {
            return;
        }
        if (extractionBlocked()) {
            traceRepeated(
                    "auto_output_blocked",
                    "reason=input_delay mode=synchronized releaseAt=" + releaseAtGameTime
                            + " members=" + traceMembers(members));
            return;
        }
        List<PlannedOutput> plan = new ArrayList<>();
        for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
            SequenceBufferBlockEntity member = members.get(memberIndex);
            if (member.isEmpty()) {
                continue;
            }
            TransferTarget target = member.findTransferTarget(member.storedKey, member.storedAmount, true).orElse(null);
            if (target == null) {
                traceRepeated(
                        "auto_output_blocked",
                        "reason=member_no_transfer_target mode=synchronized"
                                + " memberSlot=" + memberIndex
                                + " memberPos=" + member.worldPosition.toShortString()
                                + " key=" + RoutingTrace.key(member.storedKey)
                                + " amount=" + member.storedAmount);
                return;
            }
            plan.add(new PlannedOutput(member, target, member.storedKey, member.storedAmount));
        }
        if (plan.isEmpty()) {
            return;
        }
        trace("auto_output_plan", "mode=synchronized plan=" + traceOutputPlan(plan));
        for (PlannedOutput output : plan) {
            long inserted = output.target().insert(output.key(), output.amount(), false);
            trace(
                    "auto_output_commit",
                    "mode=synchronized memberPos=" + output.member().worldPosition.toShortString()
                            + " target=" + targetName(output.target())
                            + " key=" + RoutingTrace.key(output.key())
                            + " requested=" + output.amount()
                            + " inserted=" + inserted);
            if (inserted > 0) {
                output.member().removeStoredAmount(inserted);
            }
        }
    }

    private Optional<TransferTarget> findTransferTarget(AEKey key, long amount, boolean requireFullAmount) {
        if (level == null || key == null || amount <= 0) {
            traceRepeated(
                    "transfer_target_rejected",
                    "reason=invalid_request key=" + RoutingTrace.key(key)
                            + " amount=" + amount
                            + " requireFull=" + requireFullAmount);
            return Optional.empty();
        }
        for (Direction direction : outputDirections()) {
            BlockPos targetPos = worldPosition.relative(direction);
            BlockEntity targetEntity = level.getBlockEntity(targetPos);
            if (targetEntity == null) {
                continue;
            }
            if (targetEntity instanceof SequenceBufferBlockEntity) {
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
                if (effectiveConfiguration().blockingMode()) {
                    boolean targetEmpty = candidate.isEmpty();
                    traceRepeated(
                            "transfer_target_blocking_check",
                            "direction=" + direction
                                    + " targetPos=" + targetPos.toShortString()
                                    + " target=" + targetName(candidate)
                                    + " empty=" + targetEmpty
                                    + " key=" + RoutingTrace.key(key)
                                    + " amount=" + amount);
                    if (!targetEmpty) {
                        traceRepeated(
                                "transfer_target_rejected",
                                "reason=blocking_target_not_empty direction=" + direction
                                        + " targetPos=" + targetPos.toShortString()
                                        + " target=" + targetName(candidate));
                        continue;
                    }
                }
                long accepted = candidate.insert(key, amount, true);
                traceRepeated(
                        "transfer_target_preflight",
                        "direction=" + direction
                                + " targetPos=" + targetPos.toShortString()
                                + " target=" + targetName(candidate)
                                + " key=" + RoutingTrace.key(key)
                                + " requested=" + amount
                                + " accepted=" + accepted
                                + " requireFull=" + requireFullAmount);
                if (accepted > 0 && (!requireFullAmount || accepted == amount)) {
                    trace(
                            "transfer_target_selected",
                            "direction=" + direction
                                    + " targetPos=" + targetPos.toShortString()
                                    + " target=" + targetName(candidate)
                                    + " key=" + RoutingTrace.key(key)
                                    + " amount=" + amount);
                    return Optional.of(candidate);
                }
                if (accepted <= 0) {
                    traceRepeated(
                            "transfer_target_rejected",
                            "reason=simulation_zero direction=" + direction
                                    + " targetPos=" + targetPos.toShortString()
                                    + " target=" + targetName(candidate));
                } else if (requireFullAmount) {
                    traceRepeated(
                            "transfer_target_rejected",
                            "reason=partial_when_full_required direction=" + direction
                                    + " targetPos=" + targetPos.toShortString()
                                    + " target=" + targetName(candidate)
                                    + " accepted=" + accepted
                                    + " requested=" + amount);
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
        SequenceBufferConfiguration runtimeConfiguration = effectiveConfiguration();
        trace(
                "input_plan_check",
                "action=" + (simulate ? "SIMULATE" : "MODULATE")
                        + " plan=" + tracePlan(plan)
                        + " members=" + traceMembers());
        if (!isEndpoint()) {
            trace(
                    "input_plan_rejected",
                    "reason=not_endpoint action=" + (simulate ? "SIMULATE" : "MODULATE"));
            return false;
        }
        if (plan.isEmpty()) {
            trace(
                    "input_plan_rejected",
                    "reason=empty_plan action=" + (simulate ? "SIMULATE" : "MODULATE"));
            return false;
        }
        List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(this);
        for (int inputIndex = 0; inputIndex < plan.size(); inputIndex++) {
            PlannedInput input = plan.get(inputIndex);
            if (input.memberSlot() < 0 || input.memberSlot() >= members.size()) {
                trace(
                        "input_plan_rejected",
                        "reason=member_slot_out_of_range"
                                + " action=" + (simulate ? "SIMULATE" : "MODULATE")
                                + " inputIndex=" + inputIndex
                                + " memberSlot=" + input.memberSlot()
                                + " memberCount=" + members.size());
                return false;
            }
            GenericStack stack = input.stack();
            SequenceBufferBlockEntity member = members.get(input.memberSlot());
            if (stack == null) {
                trace(
                        "input_plan_rejected",
                        "reason=null_stack action=" + (simulate ? "SIMULATE" : "MODULATE")
                                + " inputIndex=" + inputIndex
                                + " memberSlot=" + input.memberSlot());
                return false;
            }
            if (stack.what() == null) {
                trace(
                        "input_plan_rejected",
                        "reason=null_key action=" + (simulate ? "SIMULATE" : "MODULATE")
                                + " inputIndex=" + inputIndex
                                + " memberSlot=" + input.memberSlot());
                return false;
            }
            if (stack.amount() <= 0) {
                trace(
                        "input_plan_rejected",
                        "reason=non_positive_amount action=" + (simulate ? "SIMULATE" : "MODULATE")
                                + " inputIndex=" + inputIndex
                                + " memberSlot=" + input.memberSlot()
                                + " amount=" + stack.amount());
                return false;
            }
            if (stack.amount() > APServerConfig.sequenceBufferCapacity()) {
                trace(
                        "input_plan_rejected",
                        "reason=over_capacity action=" + (simulate ? "SIMULATE" : "MODULATE")
                                + " inputIndex=" + inputIndex
                                + " memberSlot=" + input.memberSlot()
                                + " amount=" + stack.amount()
                                + " capacity=" + APServerConfig.sequenceBufferCapacity());
                return false;
            }
            if (!member.isEmpty()) {
                trace(
                        "input_plan_rejected",
                        "reason=member_not_empty action=" + (simulate ? "SIMULATE" : "MODULATE")
                                + " inputIndex=" + inputIndex
                                + " memberSlot=" + input.memberSlot()
                                + " memberPos=" + member.worldPosition.toShortString()
                                + " memberKey=" + RoutingTrace.key(member.storedKey)
                                + " memberAmount=" + member.storedAmount);
                return false;
            }
            if (member.inputAdmissionBlocked()) {
                trace(
                        "input_plan_rejected",
                        "reason=member_input_admission_cooldown action=" + (simulate ? "SIMULATE" : "MODULATE")
                                + " inputIndex=" + inputIndex
                                + " memberSlot=" + input.memberSlot()
                                + " memberPos=" + member.worldPosition.toShortString()
                                + " openAt=" + member.admissionOpenAtGameTime);
                return false;
            }
            if (!runtimeConfiguration.accepts(stack.what())) {
                trace(
                        "input_plan_rejected",
                        "reason=input_filter_rejected action=" + (simulate ? "SIMULATE" : "MODULATE")
                                + " inputIndex=" + inputIndex
                                + " memberSlot=" + input.memberSlot()
                                + " key=" + RoutingTrace.key(stack.what()));
                return false;
            }
            if (runtimeConfiguration.antiClogMode()
                    && member.findTransferTarget(stack.what(), stack.amount(), true).isEmpty()) {
                trace(
                        "input_plan_rejected",
                        "reason=anti_clog_output_unavailable action=" + (simulate ? "SIMULATE" : "MODULATE")
                                + " inputIndex=" + inputIndex
                                + " memberSlot=" + input.memberSlot()
                                + " key=" + RoutingTrace.key(stack.what())
                                + " amount=" + stack.amount());
                return false;
            }
        }
        if (simulate) {
            trace(
                    "input_plan_simulated",
                    "accepted=true plan=" + tracePlan(plan) + " members=" + traceMembers(members));
            return true;
        }
        for (PlannedInput input : plan) {
            SequenceBufferBlockEntity member = members.get(input.memberSlot());
            member.storedKey = input.stack().what();
            member.storedAmount = input.stack().amount();
            member.setChanged();
            trace(
                    "input_member_committed",
                    "memberSlot=" + input.memberSlot()
                            + " memberPos=" + member.worldPosition.toShortString()
                            + " key=" + RoutingTrace.key(input.stack().what())
                            + " amount=" + input.stack().amount());
        }
        markInputDelay();
        trace("input_plan_committed", "plan=" + tracePlan(plan) + " members=" + traceMembers(members));
        return true;
    }

    private void trace(String event, String details) {
        SequenceBufferConfiguration runtimeConfiguration = effectiveConfiguration();
        RoutingTrace.log(
                level,
                worldPosition,
                "sequence_buffer",
                event,
                "endpoint=" + isEndpoint()
                        + " controller=" + controllerPos.toShortString()
                        + " index=" + sequenceIndex
                        + " sequenceDirection=" + sequenceDirection
                        + " bufferBlocking=" + runtimeConfiguration.blockingMode()
                        + " synchronized=" + runtimeConfiguration.synchronizedOutput()
                        + " autoOutput=" + runtimeConfiguration.autoOutput()
                        + " " + details);
    }

    private void traceRepeated(String event, String details) {
        if (level == null) {
            trace(event, details);
            return;
        }
        long tick = level.getGameTime();
        String signature = event + '|' + details;
        long previousTick = repeatedTraceTicks.getOrDefault(signature, Long.MIN_VALUE);
        if (previousTick != Long.MIN_VALUE && tick - previousTick < REPEATED_TRACE_INTERVAL_TICKS) {
            return;
        }
        repeatedTraceTicks.put(signature, tick);
        trace(event, details);
    }

    private String traceMembers() {
        return traceMembers(isEndpoint() ? SequenceBufferTopology.members(this) : List.of(this));
    }

    private static String traceMembers(List<SequenceBufferBlockEntity> members) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (int memberSlot = 0; memberSlot < members.size(); memberSlot++) {
            SequenceBufferBlockEntity member = members.get(memberSlot);
            joiner.add(memberSlot
                    + "@" + member.worldPosition.toShortString()
                    + "=" + RoutingTrace.key(member.storedKey)
                    + "x" + member.storedAmount
                    + ":inputOpenAt=" + member.admissionOpenAtGameTime
                    + ":inputBlocked=" + member.inputAdmissionBlocked());
        }
        return joiner.toString();
    }

    private static String tracePlan(List<PlannedInput> plan) {
        if (plan == null) {
            return "null";
        }
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (int inputIndex = 0; inputIndex < plan.size(); inputIndex++) {
            PlannedInput input = plan.get(inputIndex);
            joiner.add(inputIndex + "->" + input.memberSlot() + "=" + RoutingTrace.genericStack(input.stack()));
        }
        return joiner.toString();
    }

    private static String traceOutputPlan(List<PlannedOutput> plan) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (int outputIndex = 0; outputIndex < plan.size(); outputIndex++) {
            PlannedOutput output = plan.get(outputIndex);
            joiner.add(outputIndex
                    + "@" + output.member().worldPosition.toShortString()
                    + "=" + RoutingTrace.key(output.key())
                    + "x" + output.amount()
                    + "->" + targetName(output.target()));
        }
        return joiner.toString();
    }

    private static String targetName(TransferTarget target) {
        return target == null ? "null" : target.getClass().getSimpleName();
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
        if (effectiveConfiguration().patternMode()) {
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
        tag.putLong(ADMISSION_OPEN_AT, admissionOpenAtGameTime);
        tag.putLong(CONTROLLER_POS, controllerPos.asLong());
        tag.putInt(SEQUENCE_DIRECTION, sequenceDirection.get3DDataValue());
        tag.putInt(SEQUENCE_INDEX, sequenceIndex);
        tag.put(CONFIGURATION, configuration.writeTag());
        upgrades.writeToNBT(tag, UPGRADES);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        configuration.readTag(tag.getCompound(CONFIGURATION));
        synchronizeInputFilterFromConfiguration();
        upgrades.readFromNBT(tag, UPGRADES);
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
        admissionOpenAtGameTime = tag.contains(ADMISSION_OPEN_AT, Tag.TAG_LONG)
                ? tag.getLong(ADMISSION_OPEN_AT)
                : Long.MIN_VALUE;
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
                    && effectiveConfiguration().accepts(AEItemKey.of(stack));
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
                    && effectiveConfiguration().accepts(AEFluidKey.of(stack));
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

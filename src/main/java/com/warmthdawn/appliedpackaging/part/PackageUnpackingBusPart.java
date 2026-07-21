package com.warmthdawn.appliedpackaging.part;

import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.parts.PartModels;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.MEStorage;
import appeng.core.definitions.AEItems;
import appeng.parts.PartModel;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.ae2.PackageItemStorage;
import com.warmthdawn.appliedpackaging.core.ae2.PackageUnpackingOperations;
import com.warmthdawn.appliedpackaging.core.ae2.PackageUnpackingTarget;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.sequence_buffer.SequenceBufferTopology;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.SequenceBufferBlockEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public class PackageUnpackingBusPart extends AbstractPackageBusPart implements IStorageProvider {
    public static final int ANIMATION_CYCLE_TICKS = MePackagerBlockEntity.UNPACKING_BASE_WORK_TICKS;
    public static final int DEFAULT_PRIORITY = 0;

    private static final String HELD_PACKAGE_TAG = "heldPackage";
    private static final String WORKING_TAG = "unpackWorking";
    private static final String OPERATION_TICKS_TAG = "unpackOperationTicks";
    private static final String OPERATION_DURATION_TICKS_TAG = "unpackOperationDurationTicks";
    private static final String RETRY_COOLDOWN_TAG = "unpackRetryCooldown";
    private static final String UNPACK_BLOCKED_TAG = "unpackBlocked";
    private static final String ANTI_CLOG_MODE_TAG = "antiClogMode";

    private static final IPartModel MODELS_OFF = new PartModel(
            AppliedPackaging.id("part/package_unpacking_bus_base"),
            AppliedPackaging.id("part/package_bus_status_off"));
    private static final IPartModel MODELS_ON = new PartModel(
            AppliedPackaging.id("part/package_unpacking_bus_base"),
            AppliedPackaging.id("part/package_bus_status_on"));
    private static final IPartModel MODELS_HAS_CHANNEL = new PartModel(
            AppliedPackaging.id("part/package_unpacking_bus_base"),
            AppliedPackaging.id("part/package_bus_status_has_channel"));

    /**
     * Formation-Plane-style network input backed by the one real held work package. Anti-clog mode requires the bus to
     * be able to start unpacking immediately; when disabled, one package may wait here until the real output rules pass.
     * The package already being processed or waiting remains visible and extractable.
     */
    private final MEStorage packageInput = new MEStorage() {
        @Override
        public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
            return PackageItemStorage.isPackageKey(what);
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            return insertPackageFromNetwork(what, amount, mode);
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            return extractHeldPackageFromNetwork(what, amount, mode);
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            if (PackageItemStorage.isLegalPackageStack(heldPackage)) {
                out.add(AEItemKey.of(heldPackage), 1);
            }
        }

        @Override
        public Component getDescription() {
            return Component.translatable("item.appliedpackaging.package_unpacking_bus");
        }
    };

    private final IItemHandlerModifiable heldPackageItems = new IItemHandlerModifiable() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            validateHeldSlot(slot);
            return heldPackage.copy();
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            validateHeldSlot(slot);
            setHeldPackage(stack);
            if (heldPackage.isEmpty()) {
                working = false;
                unpackBlocked = false;
                operationTicks = 0;
                operationDurationTicks = ANIMATION_CYCLE_TICKS;
                retryCooldown = 0;
            }
            if (!isClientSide()) {
                configurationChanged();
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            validateHeldSlot(slot);
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            validateHeldSlot(slot);
            if (amount <= 0 || heldPackage.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack extracted = heldPackage.copy();
            extracted.setCount(1);
            if (!simulate) {
                clearHeldPackageState();
                configurationChanged();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            validateHeldSlot(slot);
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            validateHeldSlot(slot);
            return false;
        }
    };

    private ItemStack heldPackage = ItemStack.EMPTY;
    private boolean working;
    private boolean unpackBlocked;
    private int operationTicks;
    private int operationDurationTicks = ANIMATION_CYCLE_TICKS;
    private int retryCooldown;
    private boolean antiClogMode = true;

    public PackageUnpackingBusPart(IPartItem<?> partItem) {
        super(partItem, DEFAULT_PRIORITY);
        getConfigManager().registerSetting(Settings.BLOCKING_MODE, YesNo.NO);
        getMainNode().addService(IStorageProvider.class, this);
    }

    public static void registerModels() {
        PartModels.registerModels(
                AppliedPackaging.id("part/package_unpacking_bus_base"),
                AppliedPackaging.id("part/package_bus_status_off"),
                AppliedPackaging.id("part/package_bus_status_on"),
                AppliedPackaging.id("part/package_bus_status_has_channel"));
    }

    @Override
    protected int getUpgradeSlots() {
        return UPGRADE_SLOT_COUNT;
    }

    @Override
    protected void addCollisionBoxes(IPartCollisionHelper helper) {
        helper.addBox(5, 5, 12, 11, 11, 13);
        helper.addBox(3, 3, 13, 13, 13, 14);
        helper.addBox(2, 2, 14, 14, 14, 16);
    }

    @Override
    protected void tickBus() {
        if (working) {
            tickWorkingOperation();
            return;
        }

        if (retryCooldown > 0) {
            retryCooldown--;
            return;
        }

        if (!heldPackage.isEmpty()) {
            retryHeldPackage();
            retryCooldown = retryIntervalTicks();
        }
    }

    private long insertPackageFromNetwork(AEKey what, long amount, Actionable mode) {
        if (amount <= 0) {
            return 0;
        }
        if (!getMainNode().isOnline()) {
            return 0;
        }
        if (!heldPackage.isEmpty()) {
            return 0;
        }
        if (working) {
            return 0;
        }
        if (!PackageItemStorage.isPackageKey(what)) {
            return 0;
        }

        ItemStack packageStack = ((AEItemKey) what).toStack();
        packageStack.setCount(1);
        boolean filterMatches = filterSet().matches(packageStack);
        if (!filterMatches) {
            return 0;
        }
        boolean targetAccepts = canUnpackIntoTarget(packageStack);
        if (antiClogMode && !targetAccepts) {
            return 0;
        }

        if (!mode.isSimulate()) {
            setHeldPackage(packageStack);
            retryCooldown = 0;
            if (targetAccepts) {
                unpackBlocked = false;
                startWorkingOperation();
            } else {
                unpackBlocked = true;
            }
            configurationChanged();
        }
        return 1;
    }

    private long extractHeldPackageFromNetwork(AEKey what, long amount, Actionable mode) {
        if (amount <= 0) {
            return 0;
        }
        if (heldPackage.isEmpty()) {
            return 0;
        }
        if (!(what instanceof AEItemKey itemKey)) {
            return 0;
        }
        if (!itemKey.matches(heldPackage)) {
            return 0;
        }

        ItemStack extracted = heldPackage.copy();
        if (!mode.isSimulate()) {
            clearHeldPackageState();
            configurationChanged();
        }
        return 1;
    }

    private void retryHeldPackage() {
        boolean filterMatches = filterSet().matches(heldPackage);
        boolean targetAccepts = filterMatches && canUnpackIntoTarget(heldPackage);
        if (!filterMatches || !targetAccepts) {
            if (!unpackBlocked) {
                unpackBlocked = true;
                configurationChanged();
            }
            return;
        }

        unpackBlocked = false;
        startWorkingOperation();
        configurationChanged();
    }

    private void startWorkingOperation() {
        working = true;
        operationDurationTicks = MePackagerBlockEntity.unpackingWorkTicks(
                getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD));
        operationTicks = operationDurationTicks;
        setDisplayedPackage(heldPackage);
    }

    private void tickWorkingOperation() {
        if (operationTicks > 0) {
            operationTicks--;
        }
        if (operationTicks > 0) {
            return;
        }

        boolean filterMatches = filterSet().matches(heldPackage);
        boolean committed = filterMatches && unpackIntoTarget(heldPackage);
        if (!committed) {
            working = false;
            unpackBlocked = true;
            operationTicks = 0;
            retryCooldown = retryIntervalTicks();
            setDisplayedPackage(heldPackage);
            configurationChanged();
            return;
        }

        clearHeldPackageState();
        configurationChanged();
    }

    private int retryIntervalTicks() {
        int speedCards = getUpgrades().getInstalledUpgrades(AEItems.SPEED_CARD);
        return MePackagerBlockEntity.unpackingWorkTicks(speedCards);
    }

    private boolean canUnpackIntoTarget(ItemStack packageStack) {
        Optional<SequenceBufferBlockEntity> sequenceBuffer = findTargetSequenceBuffer();
        if (sequenceBuffer.isPresent()) {
            boolean accepted = PackageDataStorage.read(packageStack)
                    .map(data -> sequenceBuffer.get().acceptPackage(data, isBlockingMode(), true))
                    .orElse(false);
            return accepted;
        }
        boolean accepted = findTargetUnpackingTarget()
                .map(target -> PackageUnpackingOperations.canUnpack(packageStack, target, isBlockingMode()))
                .orElse(false);
        return accepted;
    }

    private boolean unpackIntoTarget(ItemStack packageStack) {
        Optional<SequenceBufferBlockEntity> sequenceBuffer = findTargetSequenceBuffer();
        if (sequenceBuffer.isPresent()) {
            boolean committed = PackageDataStorage.read(packageStack)
                    .map(data -> sequenceBuffer.get().acceptPackage(data, isBlockingMode(), false))
                    .orElse(false);
            return committed;
        }
        boolean committed = findTargetUnpackingTarget()
                .map(target -> PackageUnpackingOperations.unpack(packageStack, target, isBlockingMode()))
                .orElse(false);
        return committed;
    }

    private Optional<SequenceBufferBlockEntity> findTargetSequenceBuffer() {
        if (getLevel() == null || getSide() == null) {
            return Optional.empty();
        }
        BlockPos targetPos = getBlockEntity().getBlockPos().relative(getSide());
        if (!(getLevel().getBlockEntity(targetPos) instanceof SequenceBufferBlockEntity sequenceBuffer)) {
            return Optional.empty();
        }
        return SequenceBufferTopology.resolveEndpoint(sequenceBuffer)
                .filter(SequenceBufferBlockEntity::isEndpoint);
    }

    private Optional<PackageUnpackingTarget> findTargetUnpackingTarget() {
        if (!(getLevel() instanceof ServerLevel serverLevel) || getSide() == null) {
            return Optional.empty();
        }
        BlockPos targetPos = getBlockEntity().getBlockPos().relative(getSide());
        return Optional.ofNullable(PackageUnpackingTarget.get(
                serverLevel,
                targetPos,
                serverLevel.getBlockEntity(targetPos),
                getSide().getOpposite(),
                IActionSource.ofMachine(this)));
    }

    @Override
    public Component getConnectedToDescription() {
        if (!(getLevel() instanceof ServerLevel) || getSide() == null) {
            return null;
        }
        BlockPos targetPos = getBlockEntity().getBlockPos().relative(getSide());
        if (findTargetSequenceBuffer().isEmpty() && findTargetUnpackingTarget().isEmpty()) {
            return null;
        }
        return getLevel().getBlockState(targetPos).getBlock().getName();
    }

    public boolean isBlockingMode() {
        return getConfigManager().getSetting(Settings.BLOCKING_MODE) == YesNo.YES;
    }

    public boolean antiClogMode() {
        return antiClogMode;
    }

    public void setAntiClogMode(boolean antiClogMode) {
        if (this.antiClogMode != antiClogMode) {
            this.antiClogMode = antiClogMode;
            configurationChanged();
        }
    }

    public void toggleAntiClogMode() {
        setAntiClogMode(!antiClogMode);
    }

    @Override
    protected void configurationChanged() {
        super.configurationChanged();
        IStorageProvider.requestUpdate(getMainNode());
    }

    @Override
    public void mountInventories(IStorageMounts mounts) {
        if (getMainNode().isOnline()) {
            mounts.mount(packageInput, getPriority());
        }
    }

    @Override
    public int progress() {
        if (!working) {
            return 0;
        }
        int elapsed = operationDurationTicks - Math.max(0, operationTicks);
        return Math.min(15, (elapsed * 15) / operationDurationTicks);
    }

    public ItemStack heldPackage() {
        return heldPackage.copy();
    }

    public IItemHandlerModifiable getHeldPackageItems() {
        return heldPackageItems;
    }

    public boolean isWorking() {
        return working;
    }

    public boolean unpackBlocked() {
        return unpackBlocked;
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        ItemStack savedPackage = tag.contains(HELD_PACKAGE_TAG, Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound(HELD_PACKAGE_TAG))
                : ItemStack.EMPTY;
        setHeldPackage(savedPackage);
        working = !heldPackage.isEmpty() && tag.getBoolean(WORKING_TAG);
        operationDurationTicks = tag.contains(OPERATION_DURATION_TICKS_TAG, Tag.TAG_INT)
                ? Math.max(
                        MePackagerBlockEntity.MIN_WORK_TICKS,
                        Math.min(ANIMATION_CYCLE_TICKS, tag.getInt(OPERATION_DURATION_TICKS_TAG)))
                : ANIMATION_CYCLE_TICKS;
        operationTicks = Math.max(0, Math.min(operationDurationTicks, tag.getInt(OPERATION_TICKS_TAG)));
        retryCooldown = Math.max(0, tag.getInt(RETRY_COOLDOWN_TAG));
        unpackBlocked = !heldPackage.isEmpty() && !working && tag.getBoolean(UNPACK_BLOCKED_TAG);
        antiClogMode = !tag.contains(ANTI_CLOG_MODE_TAG, Tag.TAG_BYTE) || tag.getBoolean(ANTI_CLOG_MODE_TAG);
        if (heldPackage.isEmpty()) {
            working = false;
            unpackBlocked = false;
            operationTicks = 0;
            operationDurationTicks = ANIMATION_CYCLE_TICKS;
            retryCooldown = 0;
        }
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        if (!heldPackage.isEmpty()) {
            tag.put(HELD_PACKAGE_TAG, heldPackage.save(new CompoundTag()));
        }
        tag.putBoolean(WORKING_TAG, working);
        tag.putInt(OPERATION_TICKS_TAG, operationTicks);
        tag.putInt(OPERATION_DURATION_TICKS_TAG, operationDurationTicks);
        tag.putInt(RETRY_COOLDOWN_TAG, retryCooldown);
        tag.putBoolean(UNPACK_BLOCKED_TAG, unpackBlocked);
        tag.putBoolean(ANTI_CLOG_MODE_TAG, antiClogMode);
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        if (!heldPackage.isEmpty()) {
            drops.add(heldPackage.copy());
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        clearHeldPackageState();
    }

    private void setHeldPackage(ItemStack stack) {
        heldPackage = stack == null ? ItemStack.EMPTY : stack.copy();
        heldPackage.setCount(heldPackage.isEmpty() ? 0 : 1);
        setDisplayedPackage(heldPackage);
    }

    private void clearHeldPackageState() {
        heldPackage = ItemStack.EMPTY;
        working = false;
        unpackBlocked = false;
        operationTicks = 0;
        operationDurationTicks = ANIMATION_CYCLE_TICKS;
        retryCooldown = 0;
        setDisplayedPackage(ItemStack.EMPTY);
    }

    private static void validateHeldSlot(int slot) {
        if (slot != 0) {
            throw new IndexOutOfBoundsException(slot);
        }
    }

    @Override
    public boolean showsWorkingArea() {
        return true;
    }

    @Override
    public IPartModel getStaticModels() {
        if (isActive() && isPowered()) {
            return MODELS_HAS_CHANNEL;
        }
        return isPowered() ? MODELS_ON : MODELS_OFF;
    }
}

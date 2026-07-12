package com.warmthdawn.appliedpackaging.part;

import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.parts.PartModels;
import appeng.core.definitions.AEItems;
import appeng.parts.PartModel;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.ae2.PackageUnpackingOperations;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public class PackageUnpackingBusPart extends AbstractPackageBusPart {
    public static final int ANIMATION_CYCLE_TICKS = MePackagerBlockEntity.ANIMATION_CYCLE_TICKS;

    private static final String HELD_PACKAGE_TAG = "heldPackage";
    private static final String WORKING_TAG = "unpackWorking";
    private static final String OPERATION_TICKS_TAG = "unpackOperationTicks";
    private static final String RETRY_COOLDOWN_TAG = "unpackRetryCooldown";
    private static final String UNPACK_BLOCKED_TAG = "unpackBlocked";

    private static final IPartModel MODELS_OFF = new PartModel(
            AppliedPackaging.id("part/package_unpacking_bus_base"),
            AppliedPackaging.id("part/package_bus_status_off"));
    private static final IPartModel MODELS_ON = new PartModel(
            AppliedPackaging.id("part/package_unpacking_bus_base"),
            AppliedPackaging.id("part/package_bus_status_on"));
    private static final IPartModel MODELS_HAS_CHANNEL = new PartModel(
            AppliedPackaging.id("part/package_unpacking_bus_base"),
            AppliedPackaging.id("part/package_bus_status_has_channel"));

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
            if (amount <= 0 || working || heldPackage.isEmpty()) {
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
    private int retryCooldown;

    public PackageUnpackingBusPart(IPartItem<?> partItem) {
        super(partItem);
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

        if (heldPackage.isEmpty()) {
            reserveAndStartUnpacking();
        } else {
            retryHeldPackage();
        }
        retryCooldown = retryIntervalTicks();
    }

    private void reserveAndStartUnpacking() {
        var storage = storageService();
        var target = findTargetItemHandler();
        if (storage.isEmpty() || target.isEmpty()) {
            clearHeldPackageState();
            return;
        }

        ItemStack reserved = PackageUnpackingOperations.reserveOnePackage(
                storage.get().getInventory(),
                target.get(),
                filterSet()::matches,
                actionSource());
        if (reserved.isEmpty()) {
            return;
        }

        setHeldPackage(reserved);
        unpackBlocked = false;
        startWorkingOperation();
        configurationChanged();
    }

    private void retryHeldPackage() {
        var target = findTargetItemHandler();
        if (target.isEmpty()
                || !filterSet().matches(heldPackage)
                || !PackageUnpackingOperations.canUnpack(heldPackage, target.get())) {
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
        operationTicks = ANIMATION_CYCLE_TICKS;
        setDisplayedPackage(heldPackage);
    }

    private void tickWorkingOperation() {
        if (operationTicks > 0) {
            operationTicks--;
        }
        if (operationTicks > 0) {
            return;
        }

        var target = findTargetItemHandler();
        boolean committed = target.isPresent()
                && filterSet().matches(heldPackage)
                && PackageUnpackingOperations.unpack(heldPackage, target.get());
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
        return Math.max(2, MePackagerBlockEntity.CYCLIC_REDSTONE_INTERVAL_TICKS - speedCards * 3);
    }

    @Override
    public int progress() {
        if (!working) {
            return 0;
        }
        int elapsed = ANIMATION_CYCLE_TICKS - Math.max(0, operationTicks);
        return Math.min(15, (elapsed * 15) / ANIMATION_CYCLE_TICKS);
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
        operationTicks = Math.max(0, Math.min(ANIMATION_CYCLE_TICKS, tag.getInt(OPERATION_TICKS_TAG)));
        retryCooldown = Math.max(0, tag.getInt(RETRY_COOLDOWN_TAG));
        unpackBlocked = !heldPackage.isEmpty() && !working && tag.getBoolean(UNPACK_BLOCKED_TAG);
        if (heldPackage.isEmpty()) {
            working = false;
            unpackBlocked = false;
            operationTicks = 0;
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
        tag.putInt(RETRY_COOLDOWN_TAG, retryCooldown);
        tag.putBoolean(UNPACK_BLOCKED_TAG, unpackBlocked);
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

package com.warmthdawn.appliedpackaging.world.block.entity;

import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackagePlan;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackageTransactions;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.world.block.AbstractHorizontalMachineBlock;
import com.warmthdawn.appliedpackaging.world.block.InventoryDroppingBlockEntity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class MePackagerBlockEntity extends BlockEntity implements InventoryDroppingBlockEntity {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    private static final String ITEMS_TAG = "items";
    private static final String POWERED_TAG = "powered";

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_INPUT) {
                return PackageDataStorage.read(stack).isPresent();
            }
            if (slot == SLOT_OUTPUT) {
                return stack.getItem() instanceof PackageItem;
            }
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> items);
    private boolean powered;

    public MePackagerBlockEntity(BlockPos pos, BlockState blockState) {
        super(APBlockEntities.ME_PACKAGER.get(), pos, blockState);
    }

    public ActionResult interact(Player player, ItemStack held) {
        if (!held.isEmpty() && PackageDataStorage.read(held).isPresent()) {
            ItemStack one = held.copy();
            one.setCount(1);
            ItemStack remainder = items.insertItem(SLOT_INPUT, one, false);
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
            return ActionResult.consumed("message.appliedpackaging.me_packager.output_taken");
        }

        MachineResult result = runOnce();
        return ActionResult.consumed(result.messageKey());
    }

    public void updatePowered(boolean nowPowered) {
        if (nowPowered && !powered) {
            runOnce();
        }
        powered = nowPowered;
        setChanged();
    }

    public MachineResult runOnce() {
        if (level == null || level.isClientSide) {
            return MachineResult.NO_TARGET;
        }
        Optional<IItemHandler> target = findTargetItemHandler();
        if (target.isEmpty()) {
            return MachineResult.NO_TARGET;
        }

        ItemStack input = items.getStackInSlot(SLOT_INPUT);
        if (!input.isEmpty()) {
            return unpackOne(target.get(), input);
        }
        return packOne(target.get());
    }

    private MachineResult packOne(IItemHandler source) {
        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                source,
                PackageColor.FLUIX,
                PackageCapacityProfile.DEFAULT);
        if (plan.isEmpty()) {
            return MachineResult.NO_CONTENTS;
        }
        if (!ItemPackageTransactions.canExtract(source, plan.get())) {
            return MachineResult.SOURCE_CHANGED;
        }

        ItemStack packageStack = new ItemStack(APItems.packageItems().get(PackageColor.FLUIX).get());
        PackageDataStorage.write(packageStack, plan.get().data());
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, packageStack, true);
        if (!remainder.isEmpty()) {
            return MachineResult.OUTPUT_BLOCKED;
        }

        ItemPackageTransactions.commitExtract(source, plan.get());
        items.insertItem(SLOT_OUTPUT, packageStack, false);
        setChanged();
        return MachineResult.PACKED;
    }

    private MachineResult unpackOne(IItemHandler target, ItemStack input) {
        Optional<PackageData> data = PackageDataStorage.read(input);
        if (data.isEmpty()) {
            return MachineResult.INVALID_INPUT;
        }
        if (!ItemPackageTransactions.canInsertPackageContents(data.get(), target)) {
            return MachineResult.TARGET_BLOCKED;
        }
        if (!ItemPackageTransactions.insertPackageContents(data.get(), target, false)) {
            return MachineResult.TARGET_BLOCKED;
        }
        items.extractItem(SLOT_INPUT, 1, false);
        setChanged();
        return MachineResult.UNPACKED;
    }

    private Optional<IItemHandler> findTargetItemHandler() {
        Direction facing = getBlockState().getValue(AbstractHorizontalMachineBlock.FACING);
        Direction targetDirection = facing.getOpposite();
        BlockEntity targetBlockEntity = level.getBlockEntity(worldPosition.relative(targetDirection));
        if (targetBlockEntity == null) {
            return Optional.empty();
        }
        LazyOptional<IItemHandler> capability = targetBlockEntity.getCapability(
                ForgeCapabilities.ITEM_HANDLER,
                targetDirection.getOpposite());
        return capability.resolve();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
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
        tag.putBoolean(POWERED_TAG, powered);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ITEMS_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            items.deserializeNBT(tag.getCompound(ITEMS_TAG));
        }
        powered = tag.getBoolean(POWERED_TAG);
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

    public enum MachineResult {
        PACKED("message.appliedpackaging.me_packager.packed"),
        UNPACKED("message.appliedpackaging.me_packager.unpacked"),
        NO_TARGET("message.appliedpackaging.me_packager.no_target"),
        NO_CONTENTS("message.appliedpackaging.me_packager.no_contents"),
        OUTPUT_BLOCKED("message.appliedpackaging.me_packager.output_blocked"),
        INPUT_BLOCKED("message.appliedpackaging.me_packager.input_blocked"),
        TARGET_BLOCKED("message.appliedpackaging.me_packager.target_blocked"),
        INVALID_INPUT("message.appliedpackaging.me_packager.invalid_input"),
        SOURCE_CHANGED("message.appliedpackaging.me_packager.source_changed");

        private final String messageKey;

        MachineResult(String messageKey) {
            this.messageKey = messageKey;
        }

        public String messageKey() {
            return messageKey;
        }
    }

    public record ActionResult(boolean consumed, String messageKey) {
        public static ActionResult consumed(String messageKey) {
            return new ActionResult(true, messageKey);
        }
    }
}

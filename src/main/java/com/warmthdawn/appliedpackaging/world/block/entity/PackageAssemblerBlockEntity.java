package com.warmthdawn.appliedpackaging.world.block.entity;

import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackagePlan;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackageTransactions;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.world.block.InventoryDroppingBlockEntity;
import com.warmthdawn.appliedpackaging.world.menu.PackageAssemblerMenu;
import java.util.Optional;
import net.minecraft.core.BlockPos;
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
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PackageAssemblerBlockEntity extends BlockEntity implements InventoryDroppingBlockEntity, MenuProvider {
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

        Optional<PackagePatternDataStorage.EncodedPackagePattern> pattern =
                PackagePatternDataStorage.read(items.getStackInSlot(SLOT_PATTERN));
        PackageColor color = pattern.map(PackagePatternDataStorage.EncodedPackagePattern::color)
                .orElse(PackageColor.FLUIX);

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                inputView,
                color,
                PackageCapacityProfile.DEFAULT);
        if (plan.isEmpty()) {
            return AssemblyResult.NO_CONTENTS;
        }
        if (pattern.isPresent()
                && !plan.get().data().canonicalHash().equals(pattern.get().data().canonicalHash())) {
            return AssemblyResult.PATTERN_MISMATCH;
        }
        if (!ItemPackageTransactions.canExtract(inputView, plan.get())) {
            return AssemblyResult.SOURCE_CHANGED;
        }

        ItemStack packageStack = new ItemStack(APItems.packageItems().get(color).get());
        PackageDataStorage.write(packageStack, plan.get().data());
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, packageStack, true);
        if (!remainder.isEmpty()) {
            return AssemblyResult.OUTPUT_BLOCKED;
        }

        ItemPackageTransactions.commitExtract(inputView, plan.get());
        items.insertItem(SLOT_OUTPUT, packageStack, false);
        setChanged();
        return AssemblyResult.ASSEMBLED;
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
}

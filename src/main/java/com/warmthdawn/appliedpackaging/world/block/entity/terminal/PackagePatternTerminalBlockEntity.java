package com.warmthdawn.appliedpackaging.world.block.entity.terminal;

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
import com.warmthdawn.appliedpackaging.world.menu.PackagePatternTerminalMenu;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;

public class PackagePatternTerminalBlockEntity extends BlockEntity implements MenuProvider, InventoryDroppingBlockEntity {
    public static final int INPUT_SLOT_COUNT = 9;
    public static final int SLOT_BLANK_PATTERN = 9;
    public static final int SLOT_OUTPUT = 10;
    private static final int SLOT_COUNT = 11;
    private static final String ITEMS_TAG = "items";

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_BLANK_PATTERN) {
                return stack.is(APItems.PACKAGE_PATTERN.get()) && PackagePatternDataStorage.read(stack).isEmpty();
            }
            if (slot == SLOT_OUTPUT) {
                return stack.is(APItems.PACKAGE_PATTERN.get());
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

    public PackagePatternTerminalBlockEntity(BlockPos pos, BlockState blockState) {
        super(APBlockEntities.PACKAGE_PATTERN_TERMINAL.get(), pos, blockState);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public EncodeResult encodeOnce() {
        if (!items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            return EncodeResult.OUTPUT_BLOCKED;
        }
        ItemStack blankPattern = items.getStackInSlot(SLOT_BLANK_PATTERN);
        if (blankPattern.isEmpty()
                || !blankPattern.is(APItems.PACKAGE_PATTERN.get())
                || PackagePatternDataStorage.read(blankPattern).isPresent()) {
            return EncodeResult.NO_PATTERN;
        }

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                inputView,
                PackageColor.FLUIX,
                PackageCapacityProfile.DEFAULT);
        if (plan.isEmpty()) {
            return EncodeResult.NO_CONTENTS;
        }

        ItemStack encoded = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(encoded, PackageColor.FLUIX, plan.get().data());
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, encoded, true);
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
}

package com.warmthdawn.appliedpackaging.world.block.entity;

import appeng.api.storage.MEStorage;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.capabilities.Capabilities;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackagePlan;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackageTransactions;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackagePlan;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackageTransactions;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.world.menu.MePackagerMenu;
import com.warmthdawn.appliedpackaging.world.block.AbstractHorizontalMachineBlock;
import com.warmthdawn.appliedpackaging.world.block.InventoryDroppingBlockEntity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

public class MePackagerBlockEntity extends BlockEntity implements InventoryDroppingBlockEntity, MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_CAPACITY = 2;
    public static final int SLOT_FILTER = 3;
    public static final int SLOT_MARKER = 4;
    private static final int SLOT_COUNT = 5;
    private static final String ITEMS_TAG = "items";
    private static final String POWERED_TAG = "powered";
    private static final String SELECTED_COLOR_TAG = "selected_color";
    private static final String MARKER_MODE_TAG = "marker_mode";

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_INPUT) {
                return PackageDataStorage.read(stack).isPresent();
            }
            if (slot == SLOT_OUTPUT) {
                return stack.getItem() instanceof PackageItem;
            }
            if (slot == SLOT_CAPACITY) {
                return capacityProfileFromItem(stack).isPresent();
            }
            if (slot == SLOT_FILTER) {
                return PackageFilter.fromTemplate(stack).isPresent();
            }
            if (slot == SLOT_MARKER) {
                return isMarkerItem(stack);
            }
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == SLOT_CAPACITY || slot == SLOT_FILTER || slot == SLOT_MARKER) {
                return 1;
            }
            return super.getSlotLimit(slot);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> items);
    private boolean powered;
    private PackageColor selectedColor = PackageColor.FLUIX;
    private MarkerMergeMode markerMode = MarkerMergeMode.RETAIN;

    public MePackagerBlockEntity(BlockPos pos, BlockState blockState) {
        super(APBlockEntities.ME_PACKAGER.get(), pos, blockState);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public PackageColor selectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(PackageColor selectedColor) {
        this.selectedColor = selectedColor == null ? PackageColor.FLUIX : selectedColor;
        setChanged();
    }

    public MarkerMergeMode markerMode() {
        return markerMode;
    }

    public void setMarkerMode(MarkerMergeMode markerMode) {
        this.markerMode = markerMode == null ? MarkerMergeMode.RETAIN : markerMode;
        setChanged();
    }

    public void cycleMarkerMode() {
        MarkerMergeMode[] values = MarkerMergeMode.values();
        setMarkerMode(values[(markerMode.ordinal() + 1) % values.length]);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.appliedpackaging.me_packager");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MePackagerMenu(containerId, playerInventory, this);
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
        Optional<MEStorage> meStorage = findTargetMEStorage();
        if (meStorage.isPresent()) {
            ItemStack input = items.getStackInSlot(SLOT_INPUT);
            if (!input.isEmpty()) {
                return unpackOne(meStorage.get(), input);
            }
            return packOne(meStorage.get());
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

    private MachineResult packOne(MEStorage source) {
        PackageFilter filter = configuredFilter();
        PackageColor color = filter.color().orElse(selectedColor);
        PackageFilter packingFilter = configuredPackingFilter(filter);
        Optional<MEStoragePackagePlan> plan = MEStoragePackageTransactions.planPack(
                source,
                color,
                configuredCapacityProfile(),
                packingFilter,
                markerMode,
                configuredOverrideMarker(filter));
        if (plan.isEmpty()) {
            return MachineResult.NO_CONTENTS;
        }
        if (!MEStoragePackageTransactions.canExtract(source, plan.get())) {
            return MachineResult.SOURCE_CHANGED;
        }

        ItemStack packageStack = new ItemStack(APItems.packageItems().get(color).get());
        PackageDataStorage.write(packageStack, plan.get().data());
        ItemStack remainder = items.insertItem(SLOT_OUTPUT, packageStack, true);
        if (!remainder.isEmpty()) {
            return MachineResult.OUTPUT_BLOCKED;
        }

        MEStoragePackageTransactions.commitExtract(source, plan.get());
        items.insertItem(SLOT_OUTPUT, packageStack, false);
        setChanged();
        return MachineResult.PACKED;
    }

    private MachineResult packOne(IItemHandler source) {
        PackageFilter filter = configuredFilter();
        PackageColor color = filter.color().orElse(selectedColor);
        PackageFilter packingFilter = configuredPackingFilter(filter);
        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                source,
                color,
                configuredCapacityProfile(),
                packingFilter,
                markerMode,
                configuredOverrideMarker(filter));
        if (plan.isEmpty()) {
            return MachineResult.NO_CONTENTS;
        }
        if (!ItemPackageTransactions.canExtract(source, plan.get())) {
            return MachineResult.SOURCE_CHANGED;
        }

        ItemStack packageStack = new ItemStack(APItems.packageItems().get(color).get());
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
        if (!(input.getItem() instanceof PackageItem packageItem)) {
            return MachineResult.INVALID_INPUT;
        }
        Optional<PackageData> data = PackageDataStorage.read(input);
        if (data.isEmpty()) {
            return MachineResult.INVALID_INPUT;
        }
        if (!configuredFilter().matches(packageItem.color(), data.get())) {
            return MachineResult.FILTER_REJECTED;
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

    private MachineResult unpackOne(MEStorage target, ItemStack input) {
        if (!(input.getItem() instanceof PackageItem packageItem)) {
            return MachineResult.INVALID_INPUT;
        }
        Optional<PackageData> data = PackageDataStorage.read(input);
        if (data.isEmpty()) {
            return MachineResult.INVALID_INPUT;
        }
        if (!configuredFilter().matches(packageItem.color(), data.get())) {
            return MachineResult.FILTER_REJECTED;
        }
        if (!MEStoragePackageTransactions.canInsertPackageContents(data.get(), target)) {
            return MachineResult.TARGET_BLOCKED;
        }
        if (!MEStoragePackageTransactions.insertPackageContents(data.get(), target)) {
            return MachineResult.TARGET_BLOCKED;
        }
        items.extractItem(SLOT_INPUT, 1, false);
        setChanged();
        return MachineResult.UNPACKED;
    }

    private PackageCapacityProfile configuredCapacityProfile() {
        return capacityProfileFromItem(items.getStackInSlot(SLOT_CAPACITY))
                .orElse(PackageCapacityProfile.DEFAULT);
    }

    private PackageFilter configuredFilter() {
        return PackageFilter.fromTemplate(items.getStackInSlot(SLOT_FILTER))
                .orElse(PackageFilter.any());
    }

    private PackageFilter configuredPackingFilter(PackageFilter filter) {
        return new PackageFilter(filter.color(), Optional.empty(), filter.requiredContents());
    }

    private Optional<MarkerSpec> configuredOverrideMarker(PackageFilter filter) {
        if (markerMode != MarkerMergeMode.OVERRIDE) {
            return Optional.empty();
        }
        ItemStack markerStack = items.getStackInSlot(SLOT_MARKER);
        if (isMarkerItem(markerStack)) {
            ItemStack keyStack = markerStack.copy();
            keyStack.setCount(1);
            return Optional.of(new MarkerSpec(new GenericStack(AEItemKey.of(keyStack), 1)));
        }
        return filter.marker();
    }

    public static Optional<PackageCapacityProfile> capacityProfileFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"ae2".equals(id.getNamespace())) {
            return Optional.empty();
        }
        return switch (id.getPath()) {
            case "cell_component_16k",
                    "item_storage_cell_16k",
                    "fluid_storage_cell_16k",
                    "portable_item_cell_16k",
                    "portable_fluid_cell_16k",
                    "storage_cell_16k" -> Optional.of(PackageCapacityProfile.STORAGE_16K);
            case "cell_component_64k",
                    "item_storage_cell_64k",
                    "fluid_storage_cell_64k",
                    "portable_item_cell_64k",
                    "portable_fluid_cell_64k",
                    "storage_cell_64k" -> Optional.of(PackageCapacityProfile.STORAGE_64K);
            case "cell_component_256k",
                    "item_storage_cell_256k",
                    "fluid_storage_cell_256k",
                    "portable_item_cell_256k",
                    "portable_fluid_cell_256k",
                    "storage_cell_256k" -> Optional.of(PackageCapacityProfile.STORAGE_256K);
            default -> Optional.empty();
        };
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

    private Optional<MEStorage> findTargetMEStorage() {
        Direction facing = getBlockState().getValue(AbstractHorizontalMachineBlock.FACING);
        Direction targetDirection = facing.getOpposite();
        BlockEntity targetBlockEntity = level.getBlockEntity(worldPosition.relative(targetDirection));
        if (targetBlockEntity == null) {
            return Optional.empty();
        }
        LazyOptional<MEStorage> capability = targetBlockEntity.getCapability(
                Capabilities.STORAGE,
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
        tag.putString(SELECTED_COLOR_TAG, selectedColor.id());
        tag.putString(MARKER_MODE_TAG, markerMode.name());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ITEMS_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            items.deserializeNBT(tag.getCompound(ITEMS_TAG));
        }
        powered = tag.getBoolean(POWERED_TAG);
        selectedColor = PackageColor.byId(tag.getString(SELECTED_COLOR_TAG)).orElse(PackageColor.FLUIX);
        markerMode = markerModeByName(tag.getString(MARKER_MODE_TAG));
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
        FILTER_REJECTED("message.appliedpackaging.me_packager.filter_rejected"),
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

    private static boolean isMarkerItem(ItemStack stack) {
        return !stack.isEmpty()
                && !(stack.getItem() instanceof PackageItem)
                && !PackagePatternDataStorage.canStore(stack);
    }

    private static MarkerMergeMode markerModeByName(String name) {
        if (name == null || name.isBlank()) {
            return MarkerMergeMode.RETAIN;
        }
        try {
            return MarkerMergeMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return MarkerMergeMode.RETAIN;
        }
    }
}

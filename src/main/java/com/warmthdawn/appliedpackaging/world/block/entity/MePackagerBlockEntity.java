package com.warmthdawn.appliedpackaging.world.block.entity;

import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageService;
import appeng.api.orientation.BlockOrientation;
import appeng.api.storage.MEStorage;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackagePlan;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackageTransactions;
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
import com.warmthdawn.appliedpackaging.world.block.InventoryDroppingBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.MePackagerBlock;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class MePackagerBlockEntity extends AENetworkBlockEntity implements InventoryDroppingBlockEntity, MenuProvider {
    public static final PackageCapacityProfile BASE_CAPACITY_PROFILE = PackageCapacityProfile.STORAGE_1K;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_CAPACITY = 2;
    public static final int SLOT_FILTER = 3;
    public static final int SLOT_MARKER = 4;
    public static final int CYCLIC_REDSTONE_INTERVAL_TICKS = 20;
    public static final int ANIMATION_CYCLE_TICKS = 20;
    private static final int SLOT_COUNT = 5;
    private static final String ITEMS_TAG = "items";
    private static final String POWERED_TAG = "powered";
    private static final String SELECTED_COLOR_TAG = "selected_color";
    private static final String MARKER_MODE_TAG = "marker_mode";
    private static final String REDSTONE_MODE_TAG = "redstone_mode";
    private static final String ANIMATION_TICKS_TAG = "animation_ticks";
    private static final String ANIMATION_INWARD_TAG = "animation_inward";
    private static final String RENDERED_BOX_TAG = "rendered_box";

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
            onInventorySlotChanged(slot);
            setChanged();
        }
    };
    private final IItemHandler externalItems = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return switch (slot) {
                case 0 -> items.getStackInSlot(SLOT_INPUT);
                case 1 -> items.getStackInSlot(SLOT_OUTPUT);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0) {
                return stack;
            }
            return items.insertItem(SLOT_INPUT, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 1) {
                return ItemStack.EMPTY;
            }
            return items.extractItem(SLOT_OUTPUT, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && items.isItemValid(SLOT_INPUT, stack);
        }
    };
    private final LazyOptional<IItemHandler> internalItemHandler = LazyOptional.of(() -> items);
    private final LazyOptional<IItemHandler> externalItemHandler = LazyOptional.of(() -> externalItems);
    private boolean powered;
    private int redstoneCooldown;
    private int animationTicks;
    private boolean animationInward = true;
    private ItemStack renderedBox = ItemStack.EMPTY;
    private PackageColor selectedColor = PackageColor.FLUIX;
    private MarkerMergeMode markerMode = MarkerMergeMode.RETAIN;
    private RedstoneMode redstoneMode = RedstoneMode.PULSE;

    public MePackagerBlockEntity(BlockPos pos, BlockState blockState) {
        super(APBlockEntities.ME_PACKAGER.get(), pos, blockState);
        getMainNode().setIdlePowerUsage(1.0D);
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

    public RedstoneMode redstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode redstoneMode) {
        this.redstoneMode = redstoneMode == null ? RedstoneMode.PULSE : redstoneMode;
        this.redstoneCooldown = 0;
        setChanged();
    }

    public void cycleRedstoneMode() {
        RedstoneMode[] values = RedstoneMode.values();
        setRedstoneMode(values[(redstoneMode.ordinal() + 1) % values.length]);
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
            syncVisualState();
            return ActionResult.consumed("message.appliedpackaging.me_packager.output_taken");
        }

        return ActionResult.consumed(null);
    }

    public void updatePowered(boolean nowPowered) {
        boolean wasPowered = powered;
        powered = nowPowered;
        if (!nowPowered) {
            redstoneCooldown = 0;
        }
        if (redstoneMode == RedstoneMode.PULSE && nowPowered && !wasPowered) {
            runOnce();
        }
        setChanged();
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            tickAnimation();
            return;
        }
        tickAnimation();
        boolean nowPowered = level.hasNeighborSignal(worldPosition);
        if (nowPowered != powered) {
            updatePowered(nowPowered);
        }
        if (!items.getStackInSlot(SLOT_INPUT).isEmpty()) {
            runOnce();
            return;
        }
        if (redstoneMode != RedstoneMode.CYCLIC || !powered) {
            return;
        }
        if (redstoneCooldown > 0) {
            redstoneCooldown--;
            return;
        }
        runOnce();
        redstoneCooldown = CYCLIC_REDSTONE_INTERVAL_TICKS;
    }

    public MachineResult runOnce() {
        if (level == null || level.isClientSide) {
            return MachineResult.NO_TARGET;
        }
        Optional<MEStorage> meStorage = findTargetMEStorage();
        if (meStorage.isEmpty()) {
            return MachineResult.NO_TARGET;
        }

        ItemStack input = items.getStackInSlot(SLOT_INPUT);
        if (!input.isEmpty()) {
            return unpackOne(meStorage.get(), input);
        }
        return packOne(meStorage.get());
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
        startAnimation(packageStack, false);
        setChanged();
        return MachineResult.PACKED;
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
        ItemStack renderedInput = input.copy();
        renderedInput.setCount(1);
        items.extractItem(SLOT_INPUT, 1, false);
        startAnimation(renderedInput, true);
        setChanged();
        return MachineResult.UNPACKED;
    }

    private PackageCapacityProfile configuredCapacityProfile() {
        return BASE_CAPACITY_PROFILE;
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

    private Optional<MEStorage> findTargetMEStorage() {
        if (!getMainNode().isOnline() || !getMainNode().hasGridBooted()) {
            return Optional.empty();
        }
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return Optional.empty();
        }
        IStorageService storageService = grid.getStorageService();
        if (storageService == null) {
            return Optional.empty();
        }
        return Optional.of(storageService.getInventory());
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.of(networkSide());
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return dir == networkSide() ? AECableType.SMART : AECableType.NONE;
    }

    public void onNetworkSideChanged() {
        onGridConnectableSidesChanged();
        setChanged();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) {
                return internalItemHandler.cast();
            }
            if (side == networkSide()) {
                return LazyOptional.empty();
            }
            return externalItemHandler.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        internalItemHandler.invalidate();
        externalItemHandler.invalidate();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ITEMS_TAG, items.serializeNBT());
        tag.putBoolean(POWERED_TAG, powered);
        tag.putString(SELECTED_COLOR_TAG, selectedColor.id());
        tag.putString(MARKER_MODE_TAG, markerMode.name());
        tag.putString(REDSTONE_MODE_TAG, redstoneMode.name());
        tag.putInt(ANIMATION_TICKS_TAG, animationTicks);
        tag.putBoolean(ANIMATION_INWARD_TAG, animationInward);
        if (!renderedBox.isEmpty()) {
            tag.put(RENDERED_BOX_TAG, renderedBox.save(new CompoundTag()));
        }
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        if (tag.contains(ITEMS_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            items.deserializeNBT(tag.getCompound(ITEMS_TAG));
        }
        powered = tag.getBoolean(POWERED_TAG);
        selectedColor = PackageColor.byId(tag.getString(SELECTED_COLOR_TAG)).orElse(PackageColor.FLUIX);
        markerMode = markerModeByName(tag.getString(MARKER_MODE_TAG));
        redstoneMode = RedstoneMode.byName(tag.getString(REDSTONE_MODE_TAG));
        animationTicks = tag.getInt(ANIMATION_TICKS_TAG);
        animationInward = !tag.contains(ANIMATION_INWARD_TAG) || tag.getBoolean(ANIMATION_INWARD_TAG);
        renderedBox = tag.contains(RENDERED_BOX_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound(RENDERED_BOX_TAG))
                : displayStack(items.getStackInSlot(SLOT_OUTPUT));
    }

    @Override
    protected boolean readFromStream(FriendlyByteBuf data) {
        animationTicks = data.readVarInt();
        animationInward = data.readBoolean();
        renderedBox = data.readItem();
        return true;
    }

    @Override
    protected void writeToStream(FriendlyByteBuf data) {
        data.writeVarInt(animationTicks);
        data.writeBoolean(animationInward);
        data.writeItem(renderedBox);
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

    public Direction networkSide() {
        return MePackagerBlock.networkSide(getBlockState());
    }

    public int animationTicks() {
        return animationTicks;
    }

    public boolean animationInward() {
        return animationInward;
    }

    public float getTrayOffset(float partialTicks) {
        float tickCycle = animationInward ? animationTicks - partialTicks : animationTicks - 5 - partialTicks;
        float progress = net.minecraft.util.Mth.clamp(tickCycle / (ANIMATION_CYCLE_TICKS - 5.0F) * 2.0F - 1.0F, -1.0F, 1.0F);
        progress = 1.0F - progress * progress;
        return progress * progress;
    }

    public ItemStack getRenderedBox() {
        if (animationInward) {
            return animationTicks <= ANIMATION_CYCLE_TICKS / 2 ? ItemStack.EMPTY : renderedBox;
        }
        return animationTicks >= ANIMATION_CYCLE_TICKS / 2 ? ItemStack.EMPTY : renderedBox;
    }

    public boolean isHatchOpen() {
        return animationTicks > (animationInward ? 1 : 5)
                && animationTicks < ANIMATION_CYCLE_TICKS - (animationInward ? 5 : 1);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        Direction previousSide = networkSide();
        super.setBlockState(state);
        if (previousSide != networkSide()) {
            onNetworkSideChanged();
        }
    }

    public enum RedstoneMode {
        DISABLED,
        PULSE,
        CYCLIC;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        private static RedstoneMode byName(String name) {
            if (name == null || name.isBlank()) {
                return PULSE;
            }
            try {
                return RedstoneMode.valueOf(name);
            } catch (IllegalArgumentException e) {
                return PULSE;
            }
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

    private void onInventorySlotChanged(int slot) {
        if (slot == SLOT_OUTPUT && animationTicks == 0) {
            renderedBox = displayStack(items.getStackInSlot(SLOT_OUTPUT));
            syncVisualState();
        }
    }

    private void tickAnimation() {
        if (animationTicks <= 0) {
            if (!items.getStackInSlot(SLOT_OUTPUT).isEmpty() && renderedBox.isEmpty()) {
                renderedBox = displayStack(items.getStackInSlot(SLOT_OUTPUT));
            }
            return;
        }
        animationTicks--;
        if (animationTicks == 0 && animationInward) {
            renderedBox = displayStack(items.getStackInSlot(SLOT_OUTPUT));
        }
        if (animationTicks == 0 && level != null && !level.isClientSide) {
            syncVisualState();
        }
    }

    private void startAnimation(ItemStack stack, boolean inward) {
        renderedBox = displayStack(stack);
        animationInward = inward;
        animationTicks = ANIMATION_CYCLE_TICKS;
        syncVisualState();
    }

    private void syncVisualState() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static ItemStack displayStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }
}

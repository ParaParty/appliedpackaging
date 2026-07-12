package com.warmthdawn.appliedpackaging.world.block.entity.bus;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.security.IActionSource;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.IStorageProvider;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import com.warmthdawn.appliedpackaging.core.ae2.PackageBusTransactions;
import com.warmthdawn.appliedpackaging.core.ae2.PackageItemStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.world.block.AbstractHorizontalMachineBlock;
import com.warmthdawn.appliedpackaging.world.menu.PackageBusMenu;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.IItemHandler;

public abstract class AbstractPackageBusBlockEntity extends AENetworkBlockEntity implements MenuProvider {
    public static final int REQUIRED_CONTENT_SLOT_COUNT = 3;
    private static final String FILTER_TAG = "filter";
    private static final String FILTER_TEMPLATE_TAG = "filter_template";

    private int tickCounter;
    private PackageFilter filter = PackageFilter.any();
    private ItemStack filterTemplate = ItemStack.EMPTY;

    protected AbstractPackageBusBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(1.0D);
    }

    public void serverTick() {
        tickCounter++;
        if (tickCounter % 10 == 0) {
            tickNetwork();
        }
    }

    @Override
    public AECableType getCableConnectionType(Direction side) {
        return side == targetDirection() ? AECableType.NONE : AECableType.SMART;
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        EnumSet<Direction> sides = EnumSet.allOf(Direction.class);
        sides.remove(targetDirection());
        return sides;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        Direction previousTarget = targetDirection();
        super.setBlockState(state);
        if (previousTarget != targetDirection()) {
            onGridConnectableSidesChanged();
        }
    }

    protected abstract void tickNetwork();

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }

    protected Optional<IItemHandler> findTargetItemHandler() {
        if (level == null) {
            return Optional.empty();
        }
        Direction targetDirection = targetDirection();
        BlockEntity targetBlockEntity = level.getBlockEntity(worldPosition.relative(targetDirection));
        if (targetBlockEntity == null) {
            return Optional.empty();
        }
        LazyOptional<IItemHandler> capability = targetBlockEntity.getCapability(
                ForgeCapabilities.ITEM_HANDLER,
                targetDirection.getOpposite());
        return capability.resolve();
    }

    protected Optional<IStorageService> storageService() {
        if (!getMainNode().isOnline()) {
            return Optional.empty();
        }
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return Optional.empty();
        }
        return Optional.of(grid.getStorageService());
    }

    protected IActionSource actionSource() {
        return IActionSource.ofMachine(this);
    }

    private Direction targetDirection() {
        return getBlockState().getValue(AbstractHorizontalMachineBlock.FACING).getOpposite();
    }

    public boolean setFilterTemplate(ItemStack stack) {
        if (stack.isEmpty()) {
            return clearFilterTemplate();
        }
        Optional<PackageFilter> templateFilter = PackageFilter.fromTemplate(stack);
        if (templateFilter.isEmpty()) {
            return false;
        }
        filter = templateFilter.get();
        filterTemplate = stack.copy();
        filterTemplate.setCount(1);
        onFilterChanged();
        return true;
    }

    public boolean clearFilterTemplate() {
        if (filter.isAny() && filterTemplate.isEmpty()) {
            return false;
        }
        filter = PackageFilter.any();
        filterTemplate = ItemStack.EMPTY;
        onFilterChanged();
        return true;
    }

    public ItemStack getFilterTemplate() {
        return filterTemplate.copy();
    }

    public PackageFilter getConfiguredFilter() {
        return filter;
    }

    public PackageColor filterColor() {
        return filter.color().orElse(PackageColor.FLUIX);
    }

    public Optional<MarkerSpec> filterMarker() {
        return filter.marker();
    }

    public List<GenericStack> filterRequiredContents() {
        return filter.requiredContents();
    }

    public long filterRequiredContentAmount(int slot) {
        List<GenericStack> requiredContents = filter.requiredContents();
        if (slot < 0 || slot >= requiredContents.size()) {
            return 0L;
        }
        return requiredContents.get(slot).amount();
    }

    public int filterRequiredContentAmountForDisplay(int slot) {
        long amount = filterRequiredContentAmount(slot);
        return amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
    }

    public void setManualFilterColor(PackageColor color) {
        setManualFilter(new PackageFilter(Optional.of(color), filter.marker(), filter.requiredContents()));
    }

    public void setManualFilterMarker(ItemStack stack) {
        if (stack.isEmpty()) {
            clearManualFilterMarker();
            return;
        }
        setManualFilter(new PackageFilter(
                filter.color(),
                Optional.of(new MarkerSpec(new GenericStack(AEItemKey.of(stack), 1))),
                filter.requiredContents()));
    }

    public void clearManualFilterMarker() {
        setManualFilter(new PackageFilter(filter.color(), Optional.empty(), filter.requiredContents()));
    }

    public void setManualFilterContent(int slot, ItemStack stack, long amount) {
        if (stack.isEmpty() || amount <= 0) {
            clearManualFilterContent(slot);
            return;
        }
        setManualFilterContent(slot, new GenericStack(AEItemKey.of(stack), amount));
    }

    public void setManualFilterContentFromGhostStack(int slot, ItemStack stack, boolean singleContainerOrItem) {
        if (slot < 0 || slot >= REQUIRED_CONTENT_SLOT_COUNT) {
            return;
        }
        if (stack.isEmpty()) {
            clearManualFilterContent(slot);
            return;
        }

        Optional<FluidStack> fluid = FluidUtil.getFluidContained(stack);
        if (fluid.isPresent() && !fluid.get().isEmpty()) {
            FluidStack fluidStack = fluid.get();
            long amount = fluidStack.getAmount();
            if (!singleContainerOrItem) {
                amount *= Math.max(1, stack.getCount());
            }
            setManualFilterContent(slot, new GenericStack(AEFluidKey.of(fluidStack), amount));
            return;
        }

        setManualFilterContent(slot, stack, singleContainerOrItem ? 1 : stack.getCount());
    }

    public void setManualFilterContent(int slot, GenericStack required) {
        if (slot < 0 || slot >= REQUIRED_CONTENT_SLOT_COUNT) {
            return;
        }
        if (required == null || required.amount() <= 0) {
            clearManualFilterContent(slot);
            return;
        }

        List<GenericStack> requiredContents = new ArrayList<>(filter.requiredContents());
        if (slot < requiredContents.size()) {
            requiredContents.set(slot, required);
        } else {
            requiredContents.add(required);
        }
        while (requiredContents.size() > REQUIRED_CONTENT_SLOT_COUNT) {
            requiredContents.remove(requiredContents.size() - 1);
        }
        setManualFilter(new PackageFilter(filter.color(), filter.marker(), requiredContents));
    }

    public void adjustManualFilterContentAmount(int slot, boolean increase) {
        if (slot < 0 || slot >= REQUIRED_CONTENT_SLOT_COUNT) {
            return;
        }
        List<GenericStack> requiredContents = new ArrayList<>(filter.requiredContents());
        if (slot >= requiredContents.size()) {
            return;
        }
        GenericStack current = requiredContents.get(slot);
        long step = amountStep(current);
        long amount = increase
                ? saturatingAdd(current.amount(), step)
                : Math.max(step, current.amount() - step);
        requiredContents.set(slot, new GenericStack(current.what(), amount));
        setManualFilter(new PackageFilter(filter.color(), filter.marker(), requiredContents));
    }

    public void clearManualFilterContent(int slot) {
        if (slot < 0 || slot >= REQUIRED_CONTENT_SLOT_COUNT) {
            return;
        }
        List<GenericStack> requiredContents = new ArrayList<>(filter.requiredContents());
        if (slot < requiredContents.size()) {
            requiredContents.remove(slot);
            setManualFilter(new PackageFilter(filter.color(), filter.marker(), requiredContents));
        }
    }

    protected PackageFilter configuredFilter() {
        return filter;
    }

    protected boolean exportOnePackageToTarget() {
        Optional<IItemHandler> target = findTargetItemHandler();
        Optional<IStorageService> storage = storageService();
        if (target.isEmpty() || storage.isEmpty()) {
            return false;
        }
        return PackageBusTransactions.exportOnePackage(
                storage.get().getInventory(),
                target.get(),
                configuredFilter(),
                actionSource());
    }

    protected boolean unpackOnePackageToTarget() {
        Optional<IItemHandler> target = findTargetItemHandler();
        Optional<IStorageService> storage = storageService();
        if (target.isEmpty() || storage.isEmpty()) {
            return false;
        }
        return PackageBusTransactions.unpackOnePackage(
                storage.get().getInventory(),
                target.get(),
                configuredFilter(),
                actionSource());
    }

    private void onFilterChanged() {
        setChanged();
        if (this instanceof IStorageProvider) {
            IStorageProvider.requestUpdate(getMainNode());
        }
    }

    private void setManualFilter(PackageFilter manualFilter) {
        filter = manualFilter;
        filterTemplate = ItemStack.EMPTY;
        onFilterChanged();
    }

    private static long amountStep(GenericStack stack) {
        return stack.what() instanceof AEFluidKey ? 1000L : 1L;
    }

    private static long saturatingAdd(long amount, long step) {
        if (Long.MAX_VALUE - amount < step) {
            return Long.MAX_VALUE;
        }
        return amount + step;
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        if (tag.contains(FILTER_TEMPLATE_TAG, Tag.TAG_COMPOUND)) {
            filterTemplate = ItemStack.of(tag.getCompound(FILTER_TEMPLATE_TAG));
        } else {
            filterTemplate = ItemStack.EMPTY;
        }
        filter = PackageFilter.fromTemplate(filterTemplate).orElse(PackageFilter.any());
        if (tag.contains(FILTER_TAG, Tag.TAG_COMPOUND)) {
            filter = PackageFilter.readTag(tag.getCompound(FILTER_TAG)).orElse(PackageFilter.any());
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!filter.isAny()) {
            tag.put(FILTER_TAG, filter.writeTag());
        }
        if (!filterTemplate.isEmpty()) {
            tag.put(FILTER_TEMPLATE_TAG, filterTemplate.save(new CompoundTag()));
        }
    }
}

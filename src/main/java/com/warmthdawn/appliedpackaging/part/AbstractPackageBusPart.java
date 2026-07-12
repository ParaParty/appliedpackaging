package com.warmthdawn.appliedpackaging.part;

import appeng.api.config.AccessRestriction;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Setting;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.api.config.YesNo;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.helpers.IConfigInvHost;
import appeng.helpers.IPriorityHost;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.automation.UpgradeablePart;
import appeng.util.ConfigInventory;
import appeng.util.SettingsFrom;
import com.warmthdawn.appliedpackaging.core.package_data.PackageBusFilterSet;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.ae2.PackageItemStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public abstract class AbstractPackageBusPart extends UpgradeablePart
        implements IGridTickable, IPriorityHost, IConfigInvHost {
    public static final int FILTER_ROWS = 7;
    public static final int BASE_FILTER_ROWS = 2;
    public static final int CONTENTS_PER_ROW = 6;
    public static final int UPGRADE_SLOT_COUNT = 5;
    public static final int MAX_CAPACITY_CARDS = 5;

    private static final String MARKERS_TAG = "markers";
    private static final String CONTENTS_TAG = "contents";
    private static final String COLORS_TAG = "colors";
    private static final String COLOR_ENABLED_TAG = "colorEnabled";
    private static final String FUZZY_ROWS_TAG = "fuzzyRows";
    private static final String INVERTED_ROWS_TAG = "invertedRows";

    private final ConfigInventory markerFilters = ConfigInventory.configTypes(
            key -> key instanceof AEItemKey,
            FILTER_ROWS,
            this::configurationChanged);
    private final ConfigInventory contentFilters = ConfigInventory.configTypes(
            key -> key instanceof AEItemKey,
            FILTER_ROWS * CONTENTS_PER_ROW,
            this::configurationChanged);
    private final PackageColor[] colors = new PackageColor[FILTER_ROWS];
    private final boolean[] colorEnabled = new boolean[FILTER_ROWS];
    private final boolean[] fuzzyRows = new boolean[FILTER_ROWS];
    private final boolean[] invertedRows = new boolean[FILTER_ROWS];
    private int priority;
    private int workTicks;
    private ItemStack displayedPackage = ItemStack.EMPTY;

    protected AbstractPackageBusPart(IPartItem<?> partItem) {
        super(partItem);
        for (int row = 0; row < FILTER_ROWS; row++) {
            colors[row] = PackageColor.FLUIX;
        }
        getConfigManager().registerSetting(Settings.ACCESS, AccessRestriction.READ_WRITE);
        getConfigManager().registerSetting(Settings.STORAGE_FILTER, StorageFilter.EXTRACTABLE_ONLY);
        getConfigManager().registerSetting(Settings.FILTER_ON_EXTRACT, YesNo.YES);
        getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        getMainNode().setIdlePowerUsage(1.0D).addService(IGridTickable.class, this);
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!player.level().isClientSide) {
            MenuOpener.open(APMenus.PACKAGE_BUS.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(APMenus.PACKAGE_BUS.get(), player, MenuLocators.forPart(this));
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(getPartItem());
    }

    @Override
    public void getBoxes(IPartCollisionHelper helper) {
        addCollisionBoxes(helper);
    }

    protected abstract void addCollisionBoxes(IPartCollisionHelper helper);

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        tickBus();
        return TickRateModulation.SAME;
    }

    protected abstract void tickBus();

    protected int workTicks() {
        return workTicks;
    }

    protected int advanceWorkTicks() {
        return ++workTicks;
    }

    protected void resetWorkTicks() {
        workTicks = 0;
    }

    protected Optional<IItemHandler> findTargetItemHandler() {
        if (getLevel() == null || getSide() == null) {
            return Optional.empty();
        }
        BlockPos targetPos = getBlockEntity().getBlockPos().relative(getSide());
        BlockEntity target = getLevel().getBlockEntity(targetPos);
        if (target == null) {
            return Optional.empty();
        }
        return target.getCapability(ForgeCapabilities.ITEM_HANDLER, getSide().getOpposite()).resolve();
    }

    public Component getConnectedToDescription() {
        if (getLevel() == null || getSide() == null) {
            return null;
        }
        BlockPos targetPos = getBlockEntity().getBlockPos().relative(getSide());
        BlockEntity target = getLevel().getBlockEntity(targetPos);
        if (target == null
                || !target.getCapability(ForgeCapabilities.ITEM_HANDLER, getSide().getOpposite()).isPresent()) {
            return null;
        }
        return getLevel().getBlockState(targetPos).getBlock().getName();
    }

    protected Optional<IStorageService> storageService() {
        if (!getMainNode().isOnline()) {
            return Optional.empty();
        }
        IGrid grid = getMainNode().getGrid();
        return grid == null ? Optional.empty() : Optional.of(grid.getStorageService());
    }

    protected IActionSource actionSource() {
        return IActionSource.ofMachine(this);
    }

    public ConfigInventory markerFilters() {
        return markerFilters;
    }

    public ConfigInventory contentFilters() {
        return contentFilters;
    }

    @Override
    public ConfigInventory getConfig() {
        return contentFilters;
    }

    public int enabledRows() {
        return Math.min(FILTER_ROWS,
                BASE_FILTER_ROWS + getUpgrades().getInstalledUpgrades(AEItems.CAPACITY_CARD));
    }

    public PackageColor rowColor(int row) {
        return validRow(row) ? colors[row] : PackageColor.FLUIX;
    }

    public boolean isRowColorEnabled(int row) {
        return validRow(row) && colorEnabled[row];
    }

    public void setRowColor(int row, PackageColor color) {
        if (!validEnabledRow(row)) {
            return;
        }
        colors[row] = color == null ? PackageColor.FLUIX : color;
        colorEnabled[row] = true;
        configurationChanged();
    }

    public void clearRowColor(int row) {
        if (validEnabledRow(row) && colorEnabled[row]) {
            colorEnabled[row] = false;
            configurationChanged();
        }
    }

    public boolean isRowFuzzy(int row) {
        return validRow(row) && fuzzyRows[row];
    }

    public boolean isRowInverted(int row) {
        return validRow(row) && invertedRows[row];
    }

    public void toggleRowFuzzy(int row) {
        if (validEnabledRow(row) && hasFuzzyCard()) {
            fuzzyRows[row] = !fuzzyRows[row];
            configurationChanged();
        }
    }

    public void toggleRowInverted(int row) {
        if (validEnabledRow(row) && hasInverterCard()) {
            invertedRows[row] = !invertedRows[row];
            configurationChanged();
        }
    }

    public boolean hasFuzzyCard() {
        return getUpgrades().isInstalled(AEItems.FUZZY_CARD);
    }

    public boolean hasInverterCard() {
        return getUpgrades().isInstalled(AEItems.INVERTER_CARD);
    }

    public PackageBusFilterSet filterSet() {
        List<PackageBusFilterSet.Rule> rules = new ArrayList<>();
        int rows = enabledRows();
        for (int row = 0; row < rows; row++) {
            List<AEKey> contents = new ArrayList<>();
            for (int column = 0; column < CONTENTS_PER_ROW; column++) {
                AEKey key = contentFilters.getKey(row * CONTENTS_PER_ROW + column);
                if (key != null) {
                    contents.add(key);
                }
            }
            GenericStack marker = markerFilters.getStack(row);
            rules.add(new PackageBusFilterSet.Rule(
                    colorEnabled[row] ? Optional.of(colors[row]) : Optional.empty(),
                    marker,
                    contents,
                    hasFuzzyCard() && fuzzyRows[row],
                    hasInverterCard() && invertedRows[row]));
        }
        return new PackageBusFilterSet(rules, getConfigManager().getSetting(Settings.FUZZY_MODE));
    }

    public void clearFilters() {
        markerFilters.clear();
        contentFilters.clear();
        for (int row = 0; row < FILTER_ROWS; row++) {
            colorEnabled[row] = false;
            fuzzyRows[row] = false;
            invertedRows[row] = false;
        }
        configurationChanged();
    }

    public void partitionFromTarget() {
        var target = findTargetItemHandler();
        if (target.isEmpty()) {
            return;
        }
        for (int slot = 0; slot < target.get().getSlots(); slot++) {
            ItemStack stack = target.get().getStackInSlot(slot);
            if (!PackageItemStorage.isLegalPackageStack(stack)
                    || !(stack.getItem() instanceof com.warmthdawn.appliedpackaging.item.PackageItem packageItem)) {
                continue;
            }
            var data = PackageDataStorage.read(stack);
            if (data.isEmpty()) {
                continue;
            }
            clearFilters();
            setRowColor(0, packageItem.color());
            data.get().marker().ifPresent(marker -> markerFilters.setStack(0, marker.stack()));
            for (int index = 0; index < Math.min(CONTENTS_PER_ROW, data.get().contents().size()); index++) {
                GenericStack content = data.get().contents().get(index);
                if (content.what() instanceof AEItemKey) {
                    contentFilters.setStack(index, new GenericStack(content.what(), 1));
                }
            }
            configurationChanged();
            return;
        }
    }

    public ItemStack displayedPackage() {
        return displayedPackage.copy();
    }

    protected void setDisplayedPackage(ItemStack stack) {
        displayedPackage = stack == null ? ItemStack.EMPTY : stack.copy();
        displayedPackage.setCount(displayedPackage.isEmpty() ? 0 : 1);
    }

    public int progress() {
        return 0;
    }

    public boolean showsWorkingArea() {
        return false;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int newValue) {
        priority = newValue;
        configurationChanged();
    }

    @Override
    protected void onSettingChanged(appeng.api.util.IConfigManager manager, Setting<?> setting) {
        configurationChanged();
    }

    @Override
    public void upgradesChanged() {
        super.upgradesChanged();
        clearDisabledRows();
        configurationChanged();
    }

    @Override
    protected void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        configurationChanged();
    }

    protected void configurationChanged() {
        if (getHost() != null) {
            getHost().markForSave();
        }
    }

    private void clearDisabledRows() {
        for (int row = enabledRows(); row < FILTER_ROWS; row++) {
            markerFilters.setStack(row, null);
            for (int column = 0; column < CONTENTS_PER_ROW; column++) {
                contentFilters.setStack(row * CONTENTS_PER_ROW + column, null);
            }
            colorEnabled[row] = false;
            fuzzyRows[row] = false;
            invertedRows[row] = false;
        }
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        priority = tag.getInt("priority");
        markerFilters.readFromChildTag(tag, MARKERS_TAG);
        contentFilters.readFromChildTag(tag, CONTENTS_TAG);
        int[] colorOrdinals = tag.getIntArray(COLORS_TAG);
        for (int row = 0; row < Math.min(FILTER_ROWS, colorOrdinals.length); row++) {
            if (colorOrdinals[row] >= 0 && colorOrdinals[row] < PackageColor.values().length) {
                colors[row] = PackageColor.values()[colorOrdinals[row]];
            }
        }
        readBooleans(tag, COLOR_ENABLED_TAG, colorEnabled);
        readBooleans(tag, FUZZY_ROWS_TAG, fuzzyRows);
        readBooleans(tag, INVERTED_ROWS_TAG, invertedRows);
        clearDisabledRows();
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.putInt("priority", priority);
        markerFilters.writeToChildTag(tag, MARKERS_TAG);
        contentFilters.writeToChildTag(tag, CONTENTS_TAG);
        int[] colorOrdinals = new int[FILTER_ROWS];
        for (int row = 0; row < FILTER_ROWS; row++) {
            colorOrdinals[row] = colors[row].ordinal();
        }
        tag.putIntArray(COLORS_TAG, colorOrdinals);
        writeBooleans(tag, COLOR_ENABLED_TAG, colorEnabled);
        writeBooleans(tag, FUZZY_ROWS_TAG, fuzzyRows);
        writeBooleans(tag, INVERTED_ROWS_TAG, invertedRows);
    }

    @Override
    public InternalInventory getSubInventory(net.minecraft.resources.ResourceLocation id) {
        return super.getSubInventory(id);
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, Player player) {
        super.importSettings(mode, input, player);
        markerFilters.readFromChildTag(input, MARKERS_TAG);
        contentFilters.readFromChildTag(input, CONTENTS_TAG);
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        super.exportSettings(mode, output);
        if (mode == SettingsFrom.MEMORY_CARD) {
            markerFilters.writeToChildTag(output, MARKERS_TAG);
            contentFilters.writeToChildTag(output, CONTENTS_TAG);
        }
    }

    private boolean validRow(int row) {
        return row >= 0 && row < FILTER_ROWS;
    }

    private boolean validEnabledRow(int row) {
        return validRow(row) && row < enabledRows();
    }

    private static void writeBooleans(CompoundTag tag, String key, boolean[] values) {
        byte[] packed = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            packed[i] = (byte) (values[i] ? 1 : 0);
        }
        tag.putByteArray(key, packed);
    }

    private static void readBooleans(CompoundTag tag, String key, boolean[] output) {
        byte[] packed = tag.getByteArray(key);
        for (int i = 0; i < Math.min(output.length, packed.length); i++) {
            output[i] = packed[i] != 0;
        }
    }
}

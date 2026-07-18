package com.warmthdawn.appliedpackaging.world.menu;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.api.config.YesNo;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.OptionalFakeSlot;
import appeng.util.ConfigInventory;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.part.AbstractPackageBusPart;
import com.warmthdawn.appliedpackaging.part.PackageStorageBusPart;
import com.warmthdawn.appliedpackaging.part.PackageUnpackingBusPart;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkHooks;

public class PackageBusMenu extends UpgradeableMenu<AbstractPackageBusPart> {
    public static final SlotSemantic PACKAGE_MARKERS =
            SlotSemantics.register("APPLIEDPACKAGING_PACKAGE_MARKERS", false);
    public static final SlotSemantic PACKAGE_CONTENTS =
            SlotSemantics.register("APPLIEDPACKAGING_PACKAGE_CONTENTS", false);
    public static final SlotSemantic WORKING_PACKAGE =
            SlotSemantics.register("APPLIEDPACKAGING_WORKING_PACKAGE", false);

    private static final String ACTION_CLEAR = "apPackageBusClear";
    private static final String ACTION_PARTITION = "apPackageBusPartition";
    private static final String ACTION_TOGGLE_FUZZY = "apPackageBusToggleFuzzy";
    private static final String ACTION_TOGGLE_INVERTED = "apPackageBusToggleInverted";
    private static final String ACTION_SET_COLOR = "apPackageBusSetColor";
    private static final String ACTION_CLEAR_COLOR = "apPackageBusClearColor";
    private static final String ACTION_TOGGLE_ANTI_CLOG = "apPackageBusToggleAntiClog";

    private final int[] rowStates = new int[AbstractPackageBusPart.FILTER_ROWS];
    private final IItemHandlerModifiable workingPackage;
    private int syncedProgress;
    private int syncedWorkState;

    @GuiSync(40)
    public AccessRestriction rwMode = AccessRestriction.READ_WRITE;
    @GuiSync(41)
    public StorageFilter storageFilter = StorageFilter.EXTRACTABLE_ONLY;
    @GuiSync(42)
    public YesNo filterOnExtract = YesNo.YES;
    @GuiSync(43)
    public Component connectedTo;
    @GuiSync(44)
    public YesNo blockingMode = YesNo.NO;
    @GuiSync(45)
    public boolean antiClogMode = true;

    public PackageBusMenu(int id, Inventory inventory, AbstractPackageBusPart host) {
        super(APMenus.PACKAGE_BUS.get(), id, inventory, host);
        registerClientAction(ACTION_CLEAR, this::applyClear);
        registerClientAction(ACTION_PARTITION, this::applyPartition);
        registerClientAction(ACTION_TOGGLE_FUZZY, Integer.class, this::applyToggleFuzzy);
        registerClientAction(ACTION_TOGGLE_INVERTED, Integer.class, this::applyToggleInverted);
        registerClientAction(ACTION_SET_COLOR, RowColorAction.class, this::applySetColor);
        registerClientAction(ACTION_CLEAR_COLOR, Integer.class, this::applyClearColor);
        registerClientAction(ACTION_TOGGLE_ANTI_CLOG, this::applyToggleAntiClog);
        PackageUnpackingBusPart unpackingBus = host instanceof PackageUnpackingBusPart part ? part : null;
        workingPackage = unpackingBus == null ? new ItemStackHandler(1) : unpackingBus.getHeldPackageItems();
        addSlot(new HeldPackageSlot(workingPackage, unpackingBus), WORKING_PACKAGE);
        connectedTo = host.getConnectedToDescription();

        for (int row = 0; row < rowStates.length; row++) {
            final int rowIndex = row;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    if (isClientSide()) {
                        return rowStates[rowIndex];
                    }
                    int flags = host.isRowColorEnabled(rowIndex) ? 1 : 0;
                    flags |= host.isRowFuzzy(rowIndex) ? 2 : 0;
                    flags |= host.isRowInverted(rowIndex) ? 4 : 0;
                    flags |= host.rowColor(rowIndex).ordinal() << 3;
                    return flags;
                }

                @Override
                public void set(int value) {
                    rowStates[rowIndex] = value;
                }
            });
        }
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return isClientSide() ? syncedProgress : host.progress();
            }

            @Override
            public void set(int value) {
                syncedProgress = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (isClientSide()) {
                    return syncedWorkState;
                }
                if (!(host instanceof PackageUnpackingBusPart unpackingBus)) {
                    return 0;
                }
                int state = unpackingBus.isWorking() ? 1 : 0;
                return state | (unpackingBus.unpackBlocked() ? 2 : 0);
            }

            @Override
            public void set(int value) {
                syncedWorkState = value;
            }
        });
    }

    public static PackageBusMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        MenuLocator locator = MenuLocators.readFromPacket(buffer);
        AbstractPackageBusPart host = locator.locate(inventory.player, AbstractPackageBusPart.class);
        if (host == null) {
            throw new IllegalStateException("Could not locate package bus at " + locator);
        }
        PackageBusMenu menu = new PackageBusMenu(id, inventory, host);
        menu.setReturnedFromSubScreen(buffer.readBoolean());
        return menu;
    }

    public static void registerOpener(MenuType<PackageBusMenu> menuType) {
        MenuOpener.addOpener(menuType, PackageBusMenu::open);
    }

    private static boolean open(Player player, MenuLocator locator, boolean fromSubMenu) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        AbstractPackageBusPart host = locator.locate(player, AbstractPackageBusPart.class);
        if (host == null) {
            return false;
        }
        Component title = Component.translatable(host.showsWorkingArea()
                ? "item.appliedpackaging.package_unpacking_bus"
                : "item.appliedpackaging.package_storage_bus");
        var provider = new SimpleMenuProvider((id, inventory, ignored) -> {
            PackageBusMenu menu = new PackageBusMenu(id, inventory, host);
            menu.setLocator(locator);
            return menu;
        }, title);
        NetworkHooks.openScreen(serverPlayer, provider, buffer -> {
            MenuLocators.writeToPacket(buffer, locator);
            buffer.writeBoolean(fromSubMenu);
        });
        return true;
    }

    @Override
    protected void setupConfig() {
        addFilterSlots(getHost().markerFilters(), true);
        addFilterSlots(getHost().contentFilters(), false);
    }

    private void addFilterSlots(ConfigInventory inventory, boolean markers) {
        var wrapper = inventory.createMenuWrapper();
        int columns = markers ? 1 : AbstractPackageBusPart.CONTENTS_PER_ROW;
        for (int row = 0; row < AbstractPackageBusPart.FILTER_ROWS; row++) {
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                if (row < AbstractPackageBusPart.BASE_FILTER_ROWS) {
                    addSlot(new FakeSlot(wrapper, index), markers ? PACKAGE_MARKERS : PACKAGE_CONTENTS);
                } else {
                    var slot = new OptionalFakeSlot(
                            wrapper,
                            this,
                            index,
                            row - AbstractPackageBusPart.BASE_FILTER_ROWS);
                    // PackageBusScreen draws the current AE2 0.2-alpha slot
                    // overlay from the user-supplied sprite. Suppress AE2 15's
                    // automatic 0.4-alpha background from its own states.png.
                    slot.setRenderDisabled(false);
                    addSlot(slot, markers ? PACKAGE_MARKERS : PACKAGE_CONTENTS);
                }
            }
        }
    }

    @Override
    public boolean isSlotEnabled(int index) {
        return getUpgrades().getInstalledUpgrades(AEItems.CAPACITY_CARD) > index;
    }

    @Override
    protected void loadSettingsFromHost(appeng.api.util.IConfigManager manager) {
        if (manager.hasSetting(Settings.FUZZY_MODE)) {
            setFuzzyMode(manager.getSetting(Settings.FUZZY_MODE));
        }
        if (manager.hasSetting(Settings.ACCESS)) {
            rwMode = manager.getSetting(Settings.ACCESS);
        }
        if (manager.hasSetting(Settings.STORAGE_FILTER)) {
            storageFilter = manager.getSetting(Settings.STORAGE_FILTER);
        }
        if (manager.hasSetting(Settings.FILTER_ON_EXTRACT)) {
            filterOnExtract = manager.getSetting(Settings.FILTER_ON_EXTRACT);
        }
        if (manager.hasSetting(Settings.BLOCKING_MODE)) {
            blockingMode = manager.getSetting(Settings.BLOCKING_MODE);
        }
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            connectedTo = getHost().getConnectedToDescription();
            antiClogMode = getHost() instanceof PackageUnpackingBusPart unpackingBus
                    && unpackingBus.antiClogMode();
        }
        super.broadcastChanges();
    }

    public void clear() {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR);
        } else {
            applyClear();
        }
    }

    public void partition() {
        if (isClientSide()) {
            sendClientAction(ACTION_PARTITION);
        } else {
            applyPartition();
        }
    }

    public void toggleFuzzy(int row) {
        if (isClientSide()) {
            sendClientAction(ACTION_TOGGLE_FUZZY, row);
        } else {
            applyToggleFuzzy(row);
        }
    }

    public void toggleInverted(int row) {
        if (isClientSide()) {
            sendClientAction(ACTION_TOGGLE_INVERTED, row);
        } else {
            applyToggleInverted(row);
        }
    }

    public void setColor(int row, PackageColor color) {
        RowColorAction action = new RowColorAction(row, color.ordinal());
        if (isClientSide()) {
            sendClientAction(ACTION_SET_COLOR, action);
        } else {
            applySetColor(action);
        }
    }

    public void clearColor(int row) {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR_COLOR, row);
        } else {
            applyClearColor(row);
        }
    }

    public void toggleAntiClogMode() {
        if (isClientSide()) {
            sendClientAction(ACTION_TOGGLE_ANTI_CLOG);
        } else {
            applyToggleAntiClog();
        }
    }

    private void applyClear() {
        getHost().clearFilters();
    }

    private void applyPartition() {
        if (getHost() instanceof PackageStorageBusPart storageBus) {
            storageBus.partitionFromTarget();
        }
    }

    private void applyToggleFuzzy(Integer row) {
        getHost().toggleRowFuzzy(row == null ? -1 : row);
    }

    private void applyToggleInverted(Integer row) {
        getHost().toggleRowInverted(row == null ? -1 : row);
    }

    private void applySetColor(RowColorAction action) {
        if (action == null || action.color() < 0 || action.color() >= PackageColor.values().length) {
            return;
        }
        getHost().setRowColor(action.row(), PackageColor.values()[action.color()]);
    }

    private void applyClearColor(Integer row) {
        getHost().clearRowColor(row == null ? -1 : row);
    }

    private void applyToggleAntiClog() {
        if (getHost() instanceof PackageUnpackingBusPart unpackingBus) {
            unpackingBus.toggleAntiClogMode();
            antiClogMode = unpackingBus.antiClogMode();
            broadcastChanges();
        }
    }

    public boolean showsWorkingArea() {
        return getHost().showsWorkingArea();
    }

    public boolean isStorageBus() {
        return getHost() instanceof PackageStorageBusPart;
    }

    public boolean isUnpackingBus() {
        return getHost() instanceof PackageUnpackingBusPart;
    }

    public int progress() {
        return syncedProgress;
    }

    public boolean isWorking() {
        if (isClientSide()) {
            return (syncedWorkState & 1) != 0;
        }
        return getHost() instanceof PackageUnpackingBusPart unpackingBus && unpackingBus.isWorking();
    }

    public boolean unpackBlocked() {
        if (isClientSide()) {
            return (syncedWorkState & 2) != 0;
        }
        return getHost() instanceof PackageUnpackingBusPart unpackingBus && unpackingBus.unpackBlocked();
    }

    public boolean hasFuzzyCard() {
        return hasUpgrade(AEItems.FUZZY_CARD);
    }

    public boolean hasInverterCard() {
        return hasUpgrade(AEItems.INVERTER_CARD);
    }

    public boolean isRowEnabled(int row) {
        return row >= 0 && row < getHost().enabledRows();
    }

    public boolean isRowColorEnabled(int row) {
        return row >= 0 && row < rowStates.length && (rowStates[row] & 1) != 0;
    }

    public boolean isRowFuzzy(int row) {
        return row >= 0 && row < rowStates.length && (rowStates[row] & 2) != 0;
    }

    public boolean isRowInverted(int row) {
        return row >= 0 && row < rowStates.length && (rowStates[row] & 4) != 0;
    }

    public PackageColor rowColor(int row) {
        int ordinal = row >= 0 && row < rowStates.length ? rowStates[row] >>> 3 : 0;
        return ordinal >= 0 && ordinal < PackageColor.values().length
                ? PackageColor.values()[ordinal]
                : PackageColor.FLUIX;
    }

    public AccessRestriction getReadWriteMode() {
        return rwMode;
    }

    public StorageFilter getStorageFilter() {
        return storageFilter;
    }

    public YesNo getFilterOnExtract() {
        return filterOnExtract;
    }

    public YesNo getBlockingMode() {
        return blockingMode;
    }

    public boolean antiClogMode() {
        return antiClogMode;
    }

    public Component getConnectedTo() {
        return connectedTo;
    }

    public boolean supportsFuzzySearch() {
        return hasFuzzyCard();
    }

    public record RowColorAction(int row, int color) {
    }

    private final class HeldPackageSlot extends SlotItemHandler {
        private final PackageUnpackingBusPart unpackingBus;

        private HeldPackageSlot(IItemHandlerModifiable handler, PackageUnpackingBusPart unpackingBus) {
            super(handler, 0, 0, 0);
            this.unpackingBus = unpackingBus;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return unpackingBus != null && hasItem();
        }
    }

}

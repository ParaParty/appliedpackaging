package com.warmthdawn.appliedpackaging.world.menu;

import appeng.api.stacks.GenericStack;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.MenuOpener;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.guisync.PacketWritable;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.FakeSlot;
import appeng.parts.encoding.EncodingMode;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.part.AdvancedPatternEncodingState;
import com.warmthdawn.appliedpackaging.part.AdvancedPatternEncodingTerminalHost;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public class AdvancedPatternEncodingTermMenu extends PatternEncodingTermMenu {
    private static final String ACTION_ADD_COLUMN = "apAdvancedAddColumn";
    private static final String ACTION_SET_COLUMN_COLOR = "apAdvancedSetColumnColor";
    private static final String ACTION_CLEAR_OR_DELETE_COLUMN = "apAdvancedClearOrDeleteColumn";

    private final AdvancedPatternEncodingTerminalHost advancedHost;
    private final FakeSlot[] advancedInputSlots =
            new FakeSlot[AdvancedProcessingPatternDataStorage.MAX_INPUT_SLOTS];

    @GuiSync(76)
    public ColumnSyncData columnData = ColumnSyncData.empty();

    public AdvancedPatternEncodingTermMenu(
            int id,
            Inventory inventory,
            AdvancedPatternEncodingTerminalHost host) {
        super(APMenus.ADVANCED_PATTERN_ENCODING_TERMINAL.get(), id, inventory, host, true);
        this.advancedHost = host;

        var advancedInputs = host.getAdvancedPatternState().inputs().createMenuWrapper();
        for (int slot = 0; slot < advancedInputSlots.length; slot++) {
            FakeSlot inputSlot = new FakeSlot(advancedInputs, slot);
            advancedInputSlots[slot] = inputSlot;
            addSlot(inputSlot, SlotSemantics.PROCESSING_INPUTS);
        }
        getProcessingOutputSlots()[0].setIcon(null);

        registerClientAction(ACTION_ADD_COLUMN, this::applyAddColumn);
        registerClientAction(ACTION_SET_COLUMN_COLOR, ColumnColorAction.class, this::applyColumnColor);
        registerClientAction(ACTION_CLEAR_OR_DELETE_COLUMN, Integer.class, this::applyClearOrDeleteColumn);

        if (isServerSide()) {
            host.getLogic().setMode(EncodingMode.PROCESSING);
        }
        mode = EncodingMode.PROCESSING;
    }

    public static AdvancedPatternEncodingTermMenu fromNetwork(
            int id,
            Inventory inventory,
            FriendlyByteBuf buffer) {
        MenuLocator locator = MenuLocators.readFromPacket(buffer);
        AdvancedPatternEncodingTerminalHost host =
                locator.locate(inventory.player, AdvancedPatternEncodingTerminalHost.class);
        if (host == null) {
            throw new IllegalStateException("Could not find advanced pattern terminal at " + locator);
        }
        AdvancedPatternEncodingTermMenu menu = new AdvancedPatternEncodingTermMenu(id, inventory, host);
        menu.setReturnedFromSubScreen(buffer.readBoolean());
        return menu;
    }

    public static void registerOpener(MenuType<AdvancedPatternEncodingTermMenu> menuType) {
        MenuOpener.addOpener(menuType, AdvancedPatternEncodingTermMenu::open);
    }

    private static boolean open(Player player, MenuLocator locator, boolean fromSubMenu) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        AdvancedPatternEncodingTerminalHost host =
                locator.locate(player, AdvancedPatternEncodingTerminalHost.class);
        if (host == null) {
            return false;
        }
        var provider = new SimpleMenuProvider(
                (id, inventory, ignored) -> {
                    AdvancedPatternEncodingTermMenu menu =
                            new AdvancedPatternEncodingTermMenu(id, inventory, host);
                    menu.setLocator(locator);
                    return menu;
                },
                Component.translatable("gui.appliedpackaging.advanced_pattern_terminal.short_title"));
        NetworkHooks.openScreen(serverPlayer, provider, buffer -> {
            MenuLocators.writeToPacket(buffer, locator);
            buffer.writeBoolean(fromSubMenu);
        });
        return true;
    }

    @Override
    public void setMode(EncodingMode ignored) {
        super.setMode(EncodingMode.PROCESSING);
    }

    @Override
    public void clear() {
        super.clear();
        if (isServerSide()) {
            advancedState().reset();
        }
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            advancedHost.getLogic().setMode(EncodingMode.PROCESSING);
            mode = EncodingMode.PROCESSING;
            columnData = ColumnSyncData.from(advancedState());
        }
        super.broadcastChanges();
    }

    public ItemStack encodeAdvancedPattern() {
        GenericStack[] inputs = new GenericStack[AdvancedProcessingPatternDataStorage.MAX_INPUT_SLOTS];
        boolean hasInput = false;
        int activeInputSlots = advancedState().activeColumns()
                * AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
        for (int slot = 0; slot < activeInputSlots; slot++) {
            inputs[slot] = advancedState().inputs().getStack(slot);
            hasInput |= inputs[slot] != null && inputs[slot].amount() > 0;
        }
        if (!hasInput) {
            return null;
        }

        GenericStack[] outputs = new GenericStack[AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS];
        for (int slot = 0; slot < outputs.length; slot++) {
            outputs[slot] = advancedHost.getLogic().getEncodedOutputInv().getStack(slot);
        }
        if (outputs[0] == null || outputs[0].amount() <= 0) {
            return null;
        }

        ItemStack pattern = APItems.ADVANCED_PROCESSING_PATTERN.get().encode(inputs, outputs);
        AdvancedProcessingPatternDataStorage.write(
                pattern,
                new AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern(
                        advancedState().columns(outputs[0])));
        return pattern;
    }

    public int activeColumns() {
        return Math.max(1,
                Math.min(columnData.activeColumns(), AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS));
    }

    public PackageColor columnColor(int column) {
        if (column < 0 || column >= columnData.colors().size()) {
            return PackageColor.FLUIX;
        }
        return PackageColor.byId(columnData.colors().get(column)).orElse(PackageColor.FLUIX);
    }

    public FakeSlot[] getAdvancedInputSlots() {
        return advancedInputSlots;
    }

    public void addColumn() {
        if (isClientSide()) {
            sendClientAction(ACTION_ADD_COLUMN);
            return;
        }
        applyAddColumn();
    }

    public void setColumnColor(int column, PackageColor color) {
        ColumnColorAction action = new ColumnColorAction(column, (color == null ? PackageColor.FLUIX : color).id());
        if (isClientSide()) {
            sendClientAction(ACTION_SET_COLUMN_COLOR, action);
            return;
        }
        applyColumnColor(action);
    }

    public void clearOrDeleteColumn(int column) {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR_OR_DELETE_COLUMN, column);
            return;
        }
        applyClearOrDeleteColumn(column);
    }

    private void applyAddColumn() {
        advancedState().addColumn();
        columnData = ColumnSyncData.from(advancedState());
    }

    private void applyColumnColor(ColumnColorAction action) {
        if (action.column() < 0 || action.column() >= advancedState().activeColumns()) {
            return;
        }
        advancedState().setColor(
                action.column(),
                PackageColor.byId(action.color()).orElse(PackageColor.FLUIX));
        columnData = ColumnSyncData.from(advancedState());
    }

    private void applyClearOrDeleteColumn(Integer column) {
        if (column == null || column < 0 || column >= advancedState().activeColumns()) {
            return;
        }
        advancedState().clearOrDeleteColumn(column);
        columnData = ColumnSyncData.from(advancedState());
    }

    @Override
    public boolean canModifyAmountForSlot(Slot slot) {
        if (slot != null && slot.hasItem()) {
            for (FakeSlot advancedInputSlot : advancedInputSlots) {
                if (advancedInputSlot == slot) {
                    return true;
                }
            }
        }
        return super.canModifyAmountForSlot(slot);
    }

    @Override
    public boolean isProcessingPatternSlot(Slot slot) {
        if (slot != null) {
            for (FakeSlot advancedInputSlot : advancedInputSlots) {
                if (advancedInputSlot == slot) {
                    return true;
                }
            }
        }
        return super.isProcessingPatternSlot(slot);
    }

    private AdvancedPatternEncodingState advancedState() {
        return advancedHost.getAdvancedPatternState();
    }

    public record ColumnColorAction(int column, String color) {
    }

    public record ColumnSyncData(int activeColumns, List<String> colors)
            implements PacketWritable {
        public ColumnSyncData(FriendlyByteBuf buffer) {
            this(
                    buffer.readVarInt(),
                    buffer.readList(FriendlyByteBuf::readUtf));
        }

        public ColumnSyncData {
            activeColumns = Math.max(1,
                    Math.min(activeColumns, AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS));
            colors = List.copyOf(colors);
        }

        public static ColumnSyncData empty() {
            List<String> colors = new ArrayList<>();
            for (int i = 0; i < AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS; i++) {
                colors.add(PackageColor.FLUIX.id());
            }
            return new ColumnSyncData(1, colors);
        }

        public static ColumnSyncData from(AdvancedPatternEncodingState state) {
            List<String> colors = new ArrayList<>();
            for (int column = 0; column < AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS; column++) {
                colors.add(state.color(column).id());
            }
            return new ColumnSyncData(state.activeColumns(), colors);
        }

        @Override
        public void writeToPacket(FriendlyByteBuf buffer) {
            buffer.writeVarInt(activeColumns);
            buffer.writeCollection(colors, FriendlyByteBuf::writeUtf);
        }
    }
}

package com.warmthdawn.appliedpackaging.world.menu;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.guisync.PacketWritable;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.PatternTermSlot;
import appeng.parts.encoding.EncodingMode;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.part.AdvancedPatternEncodingState;
import com.warmthdawn.appliedpackaging.part.AdvancedPatternEncodingTerminalHost;
import com.warmthdawn.appliedpackaging.part.PackagePatternEncodingState;
import com.warmthdawn.appliedpackaging.part.SpecializedPatternMode;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

/**
 * One specialized terminal menu with two fully independent editors. The
 * advanced and package pages never reuse each other's input or output
 * inventories; only the blank/encoded carrier slots are shared.
 */
public class AdvancedPatternEncodingTermMenu extends PatternEncodingTermMenu {
    private static final String ACTION_SET_SPECIALIZED_MODE = "apSetSpecializedPatternMode";
    private static final String ACTION_CLEAR_ACTIVE = "apClearSpecializedPattern";
    private static final String ACTION_ENCODE_ADVANCED = "apAdvancedEncode";
    private static final String ACTION_ADD_COLUMN = "apAdvancedAddColumn";
    private static final String ACTION_SET_COLUMN_COLOR = "apAdvancedSetColumnColor";
    private static final String ACTION_CLEAR_OR_DELETE_COLUMN = "apAdvancedClearOrDeleteColumn";
    private static final String ACTION_CYCLE_ADVANCED_OUTPUT = "apAdvancedCycleOutput";
    private static final String ACTION_ENCODE_PACKAGE = "apEncodePackagePattern";
    private static final String ACTION_SET_PACKAGE_COLOR = "apSetPackagePatternColor";

    private final AdvancedPatternEncodingTerminalHost specializedHost;
    private final FakeSlot[] advancedInputSlots =
            new FakeSlot[AdvancedProcessingPatternDataStorage.MAX_INPUT_SLOTS];
    private final FakeSlot[] advancedOutputSlots =
            new FakeSlot[AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS];
    private final FakeSlot[] packageInputSlots =
            new FakeSlot[PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT];
    private final FakeSlot packageMarkerSlot;

    @GuiSync(75)
    public SpecializedPatternMode specializedMode = SpecializedPatternMode.ADVANCED;

    @GuiSync(76)
    public ColumnSyncData columnData = ColumnSyncData.empty();

    @GuiSync(77)
    public int packageColor = PackageColor.FLUIX.ordinal();

    public AdvancedPatternEncodingTermMenu(
            int id,
            Inventory inventory,
            AdvancedPatternEncodingTerminalHost host) {
        super(APMenus.ADVANCED_PATTERN_ENCODING_TERMINAL.get(), id, inventory, host, true);
        specializedHost = host;

        var advancedInputs = advancedState().inputs().createMenuWrapper();
        for (int slot = 0; slot < advancedInputSlots.length; slot++) {
            advancedInputSlots[slot] = new FakeSlot(advancedInputs, slot);
            addSlot(advancedInputSlots[slot], SlotSemantics.PROCESSING_INPUTS);
        }

        var advancedOutputs = advancedState().outputs().createMenuWrapper();
        for (int slot = 0; slot < advancedOutputSlots.length; slot++) {
            advancedOutputSlots[slot] = new FakeSlot(advancedOutputs, slot);
            addSlot(advancedOutputSlots[slot], SlotSemantics.PROCESSING_OUTPUTS);
        }
        advancedOutputSlots[0].setIcon(null);

        var packageInputs = packageState().inputs().createMenuWrapper();
        for (int slot = 0; slot < packageInputSlots.length; slot++) {
            packageInputSlots[slot] = new FakeSlot(packageInputs, slot);
            addSlot(packageInputSlots[slot], SlotSemantics.PROCESSING_INPUTS);
        }
        packageMarkerSlot = new FakeSlot(packageState().markerInventory(), 0);
        packageMarkerSlot.setHideAmount(true);
        addSlot(packageMarkerSlot, SlotSemantics.CONFIG);

        getProcessingOutputSlots()[0].setIcon(null);

        registerClientAction(ACTION_SET_SPECIALIZED_MODE, SpecializedPatternMode.class, this::applySpecializedMode);
        registerClientAction(ACTION_CLEAR_ACTIVE, this::clearActiveEditor);
        registerClientAction(ACTION_ADD_COLUMN, this::applyAddColumn);
        registerClientAction(ACTION_SET_COLUMN_COLOR, ColumnColorAction.class, this::applyColumnColor);
        registerClientAction(ACTION_CLEAR_OR_DELETE_COLUMN, Integer.class, this::applyClearOrDeleteColumn);
        registerClientAction(ACTION_CYCLE_ADVANCED_OUTPUT, this::cycleAdvancedOutput);
        registerClientAction(ACTION_ENCODE_ADVANCED, this::encodeAdvanced);
        registerClientAction(ACTION_ENCODE_PACKAGE, this::encodePackage);
        registerClientAction(ACTION_SET_PACKAGE_COLOR, String.class, this::applyPackageColor);

        forceProcessingMode();
        specializedMode = host.getSpecializedPatternMode();
        columnData = ColumnSyncData.from(advancedState());
        packageColor = packageState().color().ordinal();
        updateSpecializedSlotActivity();
        updatePackagePreview();
    }

    public static AdvancedPatternEncodingTermMenu fromNetwork(
            int id,
            Inventory inventory,
            FriendlyByteBuf buffer) {
        MenuLocator locator = MenuLocators.readFromPacket(buffer);
        AdvancedPatternEncodingTerminalHost host =
                locator.locate(inventory.player, AdvancedPatternEncodingTerminalHost.class);
        if (host == null) {
            throw new IllegalStateException("Could not find specialized pattern terminal at " + locator);
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
    protected boolean hideViewCells() {
        return true;
    }

    @Override
    public void setMode(EncodingMode ignored) {
        forceProcessingMode();
    }

    @Override
    public void encode() {
        if (isClientSide()) {
            sendClientAction(specializedMode == SpecializedPatternMode.ADVANCED
                    ? ACTION_ENCODE_ADVANCED
                    : ACTION_ENCODE_PACKAGE);
            return;
        }
        if (specializedMode == SpecializedPatternMode.ADVANCED) {
            encodeAdvanced();
        } else {
            encodePackage();
        }
    }

    @Override
    public void clear() {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR_ACTIVE);
            return;
        }
        clearActiveEditor();
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            forceProcessingMode();
            specializedMode = specializedHost.getSpecializedPatternMode();
            columnData = ColumnSyncData.from(advancedState());
            packageColor = packageState().color().ordinal();
        }
        super.broadcastChanges();
        updateSpecializedSlotActivity();
        updatePackagePreview();
    }

    @Override
    public void onServerDataSync() {
        super.onServerDataSync();
        mode = EncodingMode.PROCESSING;
        updateSpecializedSlotActivity();
        updatePackagePreview();
    }

    @Override
    public void setItem(int slotId, int stateId, ItemStack stack) {
        super.setItem(slotId, stateId, stack);
        updatePackagePreview();
    }

    @Override
    public void onSlotChange(Slot slot) {
        super.onSlotChange(slot);
        if (isServerSide() && slot == encodedPatternSlot()) {
            ItemStack pattern = slot.getItem();
            if (pattern.is(APItems.PACKAGE_PATTERN.get())) {
                loadPackagePattern(pattern);
            } else if (pattern.is(APItems.ADVANCED_PROCESSING_PATTERN.get())) {
                loadAdvancedPattern(pattern);
            }
        }
        if (slot == packageMarkerSlot || isPackageInput(slot) || slot == encodedPatternSlot()) {
            updatePackagePreview();
        }
    }

    @Override
    public boolean isValidForSlot(Slot slot, ItemStack stack) {
        if (slot == encodedPatternSlot()) {
            return stack.is(APItems.ADVANCED_PROCESSING_PATTERN.get())
                    || stack.is(APItems.PACKAGE_PATTERN.get());
        }
        return super.isValidForSlot(slot, stack);
    }

    @Override
    public boolean canModifyAmountForSlot(Slot slot) {
        return slot != null && slot.hasItem() && isSpecializedProcessingSlot(slot);
    }

    @Override
    public boolean isProcessingPatternSlot(Slot slot) {
        return slot != null && isSpecializedProcessingSlot(slot);
    }

    @Override
    public void cycleProcessingOutput() {
        if (specializedMode != SpecializedPatternMode.ADVANCED) {
            return;
        }
        if (isClientSide()) {
            sendClientAction(ACTION_CYCLE_ADVANCED_OUTPUT);
        } else {
            cycleAdvancedOutput();
        }
    }

    @Override
    public boolean canCycleProcessingOutputs() {
        if (specializedMode != SpecializedPatternMode.ADVANCED) {
            return false;
        }
        int count = 0;
        for (FakeSlot slot : advancedOutputSlots) {
            if (slot.hasItem()) {
                count++;
            }
        }
        return count > 1;
    }

    public SpecializedPatternMode getSpecializedMode() {
        return specializedMode;
    }

    public void setSpecializedMode(SpecializedPatternMode mode) {
        SpecializedPatternMode value = mode == null ? SpecializedPatternMode.ADVANCED : mode;
        if (isClientSide()) {
            sendClientAction(ACTION_SET_SPECIALIZED_MODE, value);
            specializedMode = value;
            updateSpecializedSlotActivity();
            updatePackagePreview();
            return;
        }
        applySpecializedMode(value);
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

    public PackageColor getPackageColor() {
        PackageColor[] values = PackageColor.values();
        return packageColor >= 0 && packageColor < values.length
                ? values[packageColor]
                : PackageColor.FLUIX;
    }

    public void setPackageColor(PackageColor color) {
        PackageColor value = color == null ? PackageColor.FLUIX : color;
        if (isClientSide()) {
            sendClientAction(ACTION_SET_PACKAGE_COLOR, value.id());
            packageColor = value.ordinal();
            updatePackagePreview();
            return;
        }
        applyPackageColor(value.id());
    }

    public FakeSlot[] getAdvancedInputSlots() {
        return advancedInputSlots;
    }

    public FakeSlot[] getAdvancedOutputSlots() {
        return advancedOutputSlots;
    }

    public FakeSlot[] getPackageInputSlots() {
        return packageInputSlots;
    }

    public FakeSlot getPackageMarkerSlot() {
        return packageMarkerSlot;
    }

    public void addColumn() {
        if (isClientSide()) {
            sendClientAction(ACTION_ADD_COLUMN);
        } else {
            applyAddColumn();
        }
    }

    public void setColumnColor(int column, PackageColor color) {
        ColumnColorAction action = new ColumnColorAction(column, (color == null ? PackageColor.FLUIX : color).id());
        if (isClientSide()) {
            sendClientAction(ACTION_SET_COLUMN_COLOR, action);
        } else {
            applyColumnColor(action);
        }
    }

    public void clearOrDeleteColumn(int column) {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR_OR_DELETE_COLUMN, column);
        } else {
            applyClearOrDeleteColumn(column);
        }
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
            outputs[slot] = advancedState().outputs().getStack(slot);
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

    private void applySpecializedMode(SpecializedPatternMode mode) {
        SpecializedPatternMode value = mode == null ? SpecializedPatternMode.ADVANCED : mode;
        specializedMode = value;
        if (isServerSide()) {
            specializedHost.setSpecializedPatternMode(value);
        }
        updateSpecializedSlotActivity();
        updatePackagePreview();
    }

    private void clearActiveEditor() {
        if (specializedMode == SpecializedPatternMode.ADVANCED) {
            advancedState().reset();
            columnData = ColumnSyncData.from(advancedState());
        } else {
            packageState().inputs().clear();
        }
        updatePackagePreview();
        broadcastChanges();
    }

    private void encodeAdvanced() {
        ItemStack pattern = encodeAdvancedPattern();
        if (pattern == null) {
            clearEncodedPattern(APItems.ADVANCED_PROCESSING_PATTERN.get());
            return;
        }
        if (!prepareCarrier(APItems.ADVANCED_PROCESSING_PATTERN.get())) {
            return;
        }
        encodedPatternSlot().set(pattern);
        broadcastChanges();
    }

    private void encodePackage() {
        Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> pattern = createPackagePattern();
        if (pattern.isEmpty()) {
            clearEncodedPattern(APItems.PACKAGE_PATTERN.get());
            return;
        }
        if (!prepareCarrier(APItems.PACKAGE_PATTERN.get())) {
            return;
        }
        encodedPatternSlot().set(PackageCraftingPatternDataStorage.encode(pattern.orElseThrow()));
        broadcastChanges();
    }

    private boolean prepareCarrier(net.minecraft.world.item.Item activePatternItem) {
        Slot encodedSlot = encodedPatternSlot();
        ItemStack existing = encodedSlot.getItem();
        if (!existing.isEmpty()
                && !existing.is(activePatternItem)
                && !AEItems.BLANK_PATTERN.isSameAs(existing)) {
            return false;
        }
        if (existing.isEmpty()) {
            Slot blankSlot = blankPatternSlot();
            ItemStack blanks = blankSlot.getItem();
            if (!AEItems.BLANK_PATTERN.isSameAs(blanks)) {
                return false;
            }
            blanks.shrink(1);
            if (blanks.isEmpty()) {
                blankSlot.set(ItemStack.EMPTY);
            }
        }
        return true;
    }

    private void clearEncodedPattern(net.minecraft.world.item.Item activePatternItem) {
        Slot encodedSlot = encodedPatternSlot();
        ItemStack existing = encodedSlot.getItem();
        if (existing.is(activePatternItem)) {
            encodedSlot.set(AEItems.BLANK_PATTERN.stack(existing.getCount()));
        }
    }

    private void loadAdvancedPattern(ItemStack pattern) {
        if (!AdvancedProcessingPatternDataStorage.hasData(pattern)) {
            return;
        }
        advancedState().loadFromPattern(pattern);
        applySpecializedMode(SpecializedPatternMode.ADVANCED);
        columnData = ColumnSyncData.from(advancedState());
        broadcastChanges();
    }

    private void loadPackagePattern(ItemStack pattern) {
        Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> encoded =
                PackageCraftingPatternDataStorage.read(pattern);
        if (encoded.isEmpty()) {
            return;
        }
        var state = packageState();
        var inputs = state.inputs();
        inputs.beginBatch();
        try {
            inputs.clear();
            GenericStack[] sparseInputs = encoded.orElseThrow().sparseInputs();
            for (int slot = 0; slot < Math.min(inputs.size(), sparseInputs.length); slot++) {
                inputs.setStack(slot, sparseInputs[slot]);
            }
        } finally {
            inputs.endBatch();
        }
        state.setColor(encoded.orElseThrow().color());
        state.loadMarker(encoded.orElseThrow().data().marker());
        packageColor = state.color().ordinal();
        applySpecializedMode(SpecializedPatternMode.PACKAGE);
        broadcastChanges();
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

    private void applyPackageColor(String colorId) {
        PackageColor color = PackageColor.byId(colorId).orElse(PackageColor.FLUIX);
        packageColor = color.ordinal();
        if (isServerSide()) {
            packageState().setColor(color);
        }
        updatePackagePreview();
    }

    private void cycleAdvancedOutput() {
        List<Integer> occupied = new ArrayList<>();
        for (int slot = 0; slot < advancedState().outputs().size(); slot++) {
            if (advancedState().outputs().getStack(slot) != null) {
                occupied.add(slot);
            }
        }
        if (occupied.size() < 2) {
            return;
        }
        List<GenericStack> previous = occupied.stream()
                .map(slot -> advancedState().outputs().getStack(slot))
                .toList();
        advancedState().outputs().beginBatch();
        try {
            for (int index = 0; index < occupied.size(); index++) {
                advancedState().outputs().setStack(
                        occupied.get(index),
                        previous.get((index + 1) % previous.size()));
            }
        } finally {
            advancedState().outputs().endBatch();
        }
    }

    private Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> createPackagePattern() {
        GenericStack[] sparseInputs = new GenericStack[PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT];
        var inputs = packageState().inputs();
        for (int slot = 0; slot < sparseInputs.length; slot++) {
            GenericStack stack = inputs.getStack(slot);
            if (stack != null && stack.amount() <= 0) {
                return Optional.empty();
            }
            sparseInputs[slot] = stack;
        }
        return PackageCraftingPatternDataStorage.create(getPackageColor(), sparseInputs, markerSpec());
    }

    private Optional<MarkerSpec> markerSpec() {
        ItemStack marker = packageState().markerInventory().getStackInSlot(0);
        AEItemKey key = AEItemKey.of(marker);
        if (marker.isEmpty() || key == null) {
            return Optional.empty();
        }
        return Optional.of(new MarkerSpec(new GenericStack(key, 1)));
    }

    private void updatePackagePreview() {
        ItemStack preview = specializedMode == SpecializedPatternMode.PACKAGE
                ? createPackagePattern()
                        .map(PackageCraftingPatternDataStorage::toPackageStack)
                        .orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        if (getSlots(SlotSemantics.CRAFTING_RESULT).get(0) instanceof PatternTermSlot result) {
            result.setDisplayedCraftingOutput(preview);
        }
    }

    private void forceProcessingMode() {
        mode = EncodingMode.PROCESSING;
        if (isServerSide()) {
            specializedHost.getLogic().setMode(EncodingMode.PROCESSING);
        }
    }

    private void updateSpecializedSlotActivity() {
        setActive(getCraftingGridSlots(), false);
        setActive(getProcessingInputSlots(), false);
        setActive(getProcessingOutputSlots(), false);
        setActive(SlotSemantics.SMITHING_TABLE_TEMPLATE, false);
        setActive(SlotSemantics.SMITHING_TABLE_BASE, false);
        setActive(SlotSemantics.SMITHING_TABLE_ADDITION, false);
        setActive(SlotSemantics.SMITHING_TABLE_RESULT, false);
        setActive(SlotSemantics.STONECUTTING_INPUT, false);

        boolean advanced = specializedMode == SpecializedPatternMode.ADVANCED;
        setActive(advancedInputSlots, advanced);
        setActive(advancedOutputSlots, advanced);
        setActive(packageInputSlots, !advanced);
        packageMarkerSlot.setActive(!advanced);
        if (getSlots(SlotSemantics.CRAFTING_RESULT).get(0) instanceof AppEngSlot result) {
            result.setActive(!advanced);
        }
    }

    private void setActive(FakeSlot[] slots, boolean active) {
        for (FakeSlot slot : slots) {
            slot.setActive(active);
        }
    }

    private void setActive(SlotSemantic semantic, boolean active) {
        for (Slot slot : getSlots(semantic)) {
            if (slot instanceof AppEngSlot appEngSlot) {
                appEngSlot.setActive(active);
            }
        }
    }

    private boolean isSpecializedProcessingSlot(Slot slot) {
        return contains(advancedInputSlots, slot)
                || contains(advancedOutputSlots, slot)
                || contains(packageInputSlots, slot);
    }

    private boolean isPackageInput(Slot slot) {
        return contains(packageInputSlots, slot);
    }

    private static boolean contains(FakeSlot[] slots, Slot candidate) {
        for (FakeSlot slot : slots) {
            if (slot == candidate) {
                return true;
            }
        }
        return false;
    }

    private Slot blankPatternSlot() {
        return getSlots(SlotSemantics.BLANK_PATTERN).get(0);
    }

    private Slot encodedPatternSlot() {
        return getSlots(SlotSemantics.ENCODED_PATTERN).get(0);
    }

    private AdvancedPatternEncodingState advancedState() {
        return specializedHost.getAdvancedPatternState();
    }

    private PackagePatternEncodingState packageState() {
        return specializedHost.getPackagePatternState();
    }

    public record ColumnColorAction(int column, String color) {
    }

    public record ColumnSyncData(int activeColumns, List<String> colors) implements PacketWritable {
        public ColumnSyncData(FriendlyByteBuf buffer) {
            this(buffer.readVarInt(), buffer.readList(FriendlyByteBuf::readUtf));
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

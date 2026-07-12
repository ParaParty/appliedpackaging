package com.warmthdawn.appliedpackaging.client.screen;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.EmptyingAction;
import appeng.api.config.ActionItems;
import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.TabButton;
import appeng.core.AEConfig;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.ResizableSlot;
import com.warmthdawn.appliedpackaging.client.widget.PackageColorPicker;
import com.warmthdawn.appliedpackaging.client.widget.ModernVerticalToolbar;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.mixin.client.MEStorageScreenAccessor;
import com.warmthdawn.appliedpackaging.mixin.client.ScrollbarAccessor;
import com.warmthdawn.appliedpackaging.mixin.client.SlotAccessor;
import com.warmthdawn.appliedpackaging.world.menu.AdvancedPatternEncodingTermMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class AdvancedPatternEncodingTermScreen
        extends MEStorageScreen<AdvancedPatternEncodingTermMenu> {
    private static final int VISIBLE_COLUMNS = 4;
    private static final int VISIBLE_ROWS = 3;
    private static final int INPUT_X = 23;
    private static final int INPUT_BOTTOM = 167;
    private static final int OUTPUT_X = 129;
    private static final int SLOT_STEP = 18;
    private static final int COLUMN_STEP = 19;
    private static final int HEADER_COLOR_X = 24;
    private static final int HEADER_ACTION_X = 33;
    private static final int HEADER_COLOR_BOTTOM = 177;
    private static final int HEADER_ACTION_BOTTOM = 176;
    private static final int HEADER_BUTTON_SIZE = 8;
    private static final int INPUT_PANEL_WIDTH = (VISIBLE_COLUMNS - 1) * COLUMN_STEP + SLOT_STEP;
    private static final int COLUMN_SCROLLBAR_X = 23;
    private static final int COLUMN_SCROLLBAR_BOTTOM = 113;
    private static final int COLUMN_SCROLLBAR_TRACK_WIDTH = 75;
    private static final int CLEAR_BUTTON_X = 99;
    private static final int CLEAR_BUTTON_BOTTOM = 177;
    private static final int ENCODE_BUTTON_X = 167;
    private static final int ENCODE_BUTTON_BOTTOM = 146;
    private static final int ENABLED_SLOT_BODY = 0xffadb0c4;
    private static final int DISABLED_SLOT_BODY = 0xff969cb1;

    private static final ResourceLocation SPRITES = new ResourceLocation(
            "appliedpackaging",
            "textures/gui/advanced_pattern_encoding_terminal_sprites.png");
    private static final ResourceLocation LATEST_AE2_STATES = new ResourceLocation(
            "appliedpackaging",
            "textures/gui/advanced_pattern_encoding_terminal_states.png");
    private static final ResourceLocation LATEST_NETWORK_SCROLLBAR = new ResourceLocation(
            "appliedpackaging",
            "textures/gui/advanced_pattern_encoding_terminal_scrollbar.png");
    private static final Scrollbar.Style NETWORK_SCROLLBAR_STYLE = Scrollbar.Style.create(
            LATEST_NETWORK_SCROLLBAR,
            12,
            15,
            0,
            0,
            12,
            0);
    private static final Scrollbar.Style ROW_SCROLLBAR_STYLE = Scrollbar.Style.create(
            SPRITES,
            7,
            15,
            0,
            32,
            16,
            32);
    private static final Blitter COLUMN_SCROLLBAR = Blitter.texture(SPRITES).src(0, 16, 15, 8);
    private static final Blitter CLEAR_BUTTON = Blitter.texture(SPRITES).src(0, 0, 8, 8);
    private static final Blitter COLOR_BUTTON = Blitter.texture(SPRITES).src(8, 0, 8, 8);
    private static final Blitter ADD_COLUMN_BUTTON = Blitter.texture(SPRITES).src(0, 8, 8, 8);
    private static final Blitter DELETE_COLUMN_BUTTON = Blitter.texture(SPRITES).src(8, 8, 8, 8);
    private static final Blitter LATEST_PRIMARY_OUTPUT = Blitter.texture(LATEST_AE2_STATES).src(224, 0, 16, 16);
    private static final Blitter LATEST_ENCODE_ICON = Blitter.texture(LATEST_AE2_STATES).src(128, 0, 16, 16);
    private static final Blitter LATEST_ENCODE_BUTTON = Blitter.texture(LATEST_AE2_STATES).src(176, 128, 18, 20);
    private static final Blitter LATEST_ENCODE_BUTTON_FOCUS =
            Blitter.texture(LATEST_AE2_STATES).src(194, 128, 18, 20);
    private static final Blitter LATEST_ENCODE_BUTTON_HOVER =
            Blitter.texture(LATEST_AE2_STATES).src(212, 128, 18, 20);
    private static final Blitter LATEST_ENCODED_PATTERN =
            Blitter.texture(LATEST_AE2_STATES).src(240, 112, 16, 16);
    private static final Blitter LATEST_BLANK_PATTERN =
            Blitter.texture(LATEST_AE2_STATES).src(240, 128, 16, 16);
    private static final Blitter LATEST_TAB_BUTTON =
            Blitter.texture(LATEST_AE2_STATES).src(160, 192, 20, 20);
    private static final Blitter LATEST_TAB_BUTTON_FOCUS =
            Blitter.texture(LATEST_AE2_STATES).src(160, 224, 22, 22);
    private static final Blitter LATEST_CRAFT_HAMMER =
            Blitter.texture(LATEST_AE2_STATES).src(48, 144, 16, 16);

    private final ColumnColorButton[] columnColorButtons = new ColumnColorButton[VISIBLE_COLUMNS];
    private final ColumnActionButton[] columnActionButtons = new ColumnActionButton[VISIBLE_COLUMNS];
    private final PackageColorPicker colorPicker = new PackageColorPicker();
    private final LatestEncodeButton encodeButton = new LatestEncodeButton();
    private final LatestCraftingStatusButton craftingStatusButton = new LatestCraftingStatusButton();
    private final CompactClearButton clearButton = new CompactClearButton();
    private final ModernVerticalToolbar modernToolbar = new ModernVerticalToolbar();
    private final ActionButton cycleOutputButton;
    private final Scrollbar rowScrollbar;
    private int scrollColumn;
    private int editedColumn = -1;
    private boolean draggingColumnScrollbar;
    private TabButton legacyCraftingStatusButton;

    public AdvancedPatternEncodingTermScreen(
            AdvancedPatternEncodingTermMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        ((ScrollbarAccessor) ((MEStorageScreenAccessor) this).appliedpackaging$getNetworkScrollbar())
                .appliedpackaging$setStyle(NETWORK_SCROLLBAR_STYLE);
        clearLegacyPatternSlotIcons();

        cycleOutputButton = new ActionButton(
                ActionItems.CYCLE_PROCESSING_OUTPUT,
                ignored -> menu.cycleProcessingOutput());
        cycleOutputButton.setHalfSize(true);
        cycleOutputButton.setDisableBackground(true);
        widgets.add("processingCycleOutput", cycleOutputButton);
        rowScrollbar = widgets.addScrollBar("processingPatternModeScrollbar", ROW_SCROLLBAR_STYLE);
        rowScrollbar.setRange(
                0,
                AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE - VISIBLE_ROWS,
                VISIBLE_ROWS);
        rowScrollbar.setCaptureMouseWheel(false);

        for (int visibleColumn = 0; visibleColumn < VISIBLE_COLUMNS; visibleColumn++) {
            columnColorButtons[visibleColumn] = new ColumnColorButton(visibleColumn);
            columnActionButtons[visibleColumn] = new ColumnActionButton(visibleColumn);
        }
    }

    @Override
    public void init() {
        super.init();
        replaceCraftingStatusButton();
        modernToolbar.captureIconButtons(children());
        for (Renderable renderer : modernToolbar.createIconButtonRenderers()) {
            addRenderableOnly(renderer);
        }
        for (ColumnColorButton button : columnColorButtons) {
            addRenderableWidget(button);
        }
        for (ColumnActionButton button : columnActionButtons) {
            addRenderableWidget(button);
        }
        addRenderableWidget(clearButton);
        addRenderableWidget(encodeButton);
        if (legacyCraftingStatusButton != null) {
            addRenderableWidget(craftingStatusButton);
        }
        layoutDynamicWidgets();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        setTextContent(TEXT_ID_DIALOG_TITLE, Component.translatable("gui.ae2.Terminal"));
        scrollColumn = Math.max(0, Math.min(scrollColumn, maxScrollColumn()));
        if (editedColumn >= menu.activeColumns()) {
            closeColumnEditor();
        }
        updateProcessingSlots();
        layoutDynamicWidgets();

        boolean editorClosed = !colorPicker.isOpen();
        encodeButton.visible = true;
        encodeButton.active = editorClosed;
        clearButton.visible = true;
        clearButton.active = editorClosed;
        cycleOutputButton.setVisibility(menu.canCycleProcessingOutputs());
        cycleOutputButton.active = editorClosed;
        rowScrollbar.setVisible(true);
        if (legacyCraftingStatusButton != null) {
            legacyCraftingStatusButton.visible = false;
            craftingStatusButton.visible = true;
            craftingStatusButton.active = legacyCraftingStatusButton.active;
        }
    }

    @Override
    public void drawBG(
            GuiGraphics graphics,
            int offsetX,
            int offsetY,
            int mouseX,
            int mouseY,
            float partialTicks) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        modernToolbar.layout(offsetX, offsetY);
        modernToolbar.drawPanel(graphics, offsetX, offsetY);
        drawEncodingSlotBackgrounds(graphics, offsetX, offsetY);
        drawColumnScrollbar(graphics, offsetX, offsetY);
        drawPrimaryOutputOverlay(graphics, offsetX, offsetY);
        drawSlotIcon(
                graphics,
                offsetX,
                offsetY,
                menu.getSlots(appeng.menu.SlotSemantics.BLANK_PATTERN),
                LATEST_BLANK_PATTERN);
        drawSlotIcon(
                graphics,
                offsetX,
                offsetY,
                menu.getSlots(appeng.menu.SlotSemantics.ENCODED_PATTERN),
                LATEST_ENCODED_PATTERN);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        colorPicker.render(graphics, font, mouseX, mouseY);
    }

    @Override
    public void renderCustomSlotHighlight(GuiGraphics graphics, int x, int y, int z) {
        // The 1.20.1 AE2 mixin invokes this while Vanilla is still rendering its
        // slot batch. The newer multi-part highlight is drawn before tooltips.
    }

    private void drawLatestSlotHighlight(GuiGraphics graphics) {
        if (hoveredSlot == null || !hoveredSlot.isActive()) {
            return;
        }

        graphics.flush();
        int width = 16;
        int height = 16;
        if (hoveredSlot instanceof ResizableSlot resizableSlot) {
            width = resizableSlot.getWidth();
            height = resizableSlot.getHeight();
        }

        int x = leftPos + hoveredSlot.x;
        int y = topPos + hoveredSlot.y;
        graphics.fill(x, y, x + width, y + height, 0x669cd3ff);
        graphics.hLine(x, x + width, y - 1, 0xffdaffff);
        graphics.hLine(x - 1, x + width, y + height, 0xffdaffff);
        graphics.vLine(x - 1, y - 2, y + height, 0xffdaffff);
        graphics.vLine(x + width, y - 2, y + height, 0xffdaffff);
        graphics.flush();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPicker.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (minecraft.options.keyPickItem.matchesMouse(button)) {
            Slot slot = getSlotUnderMouse();
            if (menu.canModifyAmountForSlot(slot)) {
                GenericStack currentStack = GenericStack.fromItemStack(slot.getItem());
                if (currentStack != null) {
                    var amountScreen = new AdvancedSetPatternAmountScreen(
                            this,
                            currentStack,
                            newStack -> NetworkHandler.instance().sendToServer(new InventoryActionPacket(
                                    InventoryAction.SET_FILTER,
                                    slot.index,
                                    GenericStack.wrapInItemStack(newStack))));
                    switchToScreen(amountScreen);
                    return true;
                }
            }
        }
        if (button == 0 && isOverColumnScrollbar(mouseX, mouseY)) {
            draggingColumnScrollbar = true;
            updateColumnScrollFromMouse(mouseX);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingColumnScrollbar) {
            draggingColumnScrollbar = false;
            return true;
        }
        if (colorPicker.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingColumnScrollbar) {
            updateColumnScrollFromMouse(mouseX);
            return true;
        }
        if (colorPicker.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (colorPicker.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        boolean horizontalScroll = isOverColumnScrollbar(mouseX, mouseY)
                || isOverColumnHeaders(mouseX, mouseY)
                || (Screen.hasShiftDown() && isOverInputPanel(mouseX, mouseY));
        if (horizontalScroll && maxScrollColumn() > 0) {
            scrollColumn = Math.max(0, Math.min(maxScrollColumn(), scrollColumn + (delta < 0 ? 1 : -1)));
            return true;
        }
        if (isOverProcessingGrid(mouseX, mouseY)
                && rowScrollbar.onMouseWheel(
                        new Point((int) mouseX - leftPos, (int) mouseY - topPos),
                        delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (colorPicker.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return colorPicker.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    protected EmptyingAction getEmptyingAction(Slot slot, ItemStack carried) {
        if (menu.isProcessingPatternSlot(slot)) {
            EmptyingAction action = ContainerItemStrategies.getEmptyingAction(carried);
            if (action != null) {
                return action;
            }
        }
        return super.getEmptyingAction(slot, carried);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        if (colorPicker.isOpen()) {
            return;
        }
        drawLatestSlotHighlight(graphics);
        if (menu.getCarried().isEmpty() && menu.canModifyAmountForSlot(hoveredSlot)) {
            List<Component> tooltip = new ArrayList<>(getTooltipFromContainerItem(hoveredSlot.getItem()));
            GenericStack stack = GenericStack.fromItemStack(hoveredSlot.getItem());
            if (stack != null) {
                tooltip.add(Tooltips.getAmountTooltip(ButtonToolTips.Amount, stack));
            }
            tooltip.add(Tooltips.getSetAmountTooltip());
            drawTooltip(graphics, x, y, tooltip);
        } else {
            super.renderTooltip(graphics, x, y);
        }
    }

    @Override
    public void onClose() {
        if (AEConfig.instance().isClearGridOnClose()) {
            menu.clear();
        }
        super.onClose();
    }

    private void updateProcessingSlots() {
        setSlotsActive(appeng.menu.SlotSemantics.CRAFTING_GRID, false);
        setSlotsActive(appeng.menu.SlotSemantics.CRAFTING_RESULT, false);
        setSlotsActive(appeng.menu.SlotSemantics.SMITHING_TABLE_TEMPLATE, false);
        setSlotsActive(appeng.menu.SlotSemantics.SMITHING_TABLE_BASE, false);
        setSlotsActive(appeng.menu.SlotSemantics.SMITHING_TABLE_ADDITION, false);
        setSlotsActive(appeng.menu.SlotSemantics.SMITHING_TABLE_RESULT, false);
        setSlotsActive(appeng.menu.SlotSemantics.STONECUTTING_INPUT, false);
        for (Slot slot : super.getMenu().getProcessingInputSlots()) {
            if (slot instanceof appeng.menu.slot.AppEngSlot appEngSlot) {
                appEngSlot.setActive(false);
            }
        }

        int inputY = imageHeight - INPUT_BOTTOM;
        int rowScroll = rowScrollbar.getCurrentScroll();
        var inputs = menu.getAdvancedInputSlots();
        for (int slotIndex = 0; slotIndex < inputs.length; slotIndex++) {
            var slot = inputs[slotIndex];
            int column = slotIndex / AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
            int row = slotIndex % AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
            int visibleColumn = column - scrollColumn;
            int visibleRow = row - rowScroll;
            boolean active = column < menu.activeColumns()
                    && visibleColumn >= 0
                    && visibleColumn < VISIBLE_COLUMNS
                    && visibleRow >= 0
                    && visibleRow < VISIBLE_ROWS;
            slot.setActive(active);
            if (active) {
                setSlotPosition(slot, INPUT_X + visibleColumn * COLUMN_STEP, inputY + visibleRow * SLOT_STEP);
            }
        }

        var outputs = menu.getProcessingOutputSlots();
        for (int slotIndex = 0; slotIndex < outputs.length; slotIndex++) {
            var slot = outputs[slotIndex];
            int visibleRow = slotIndex - rowScroll;
            boolean active = slotIndex < AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS
                    && visibleRow >= 0
                    && visibleRow < VISIBLE_ROWS;
            slot.setActive(active);
            if (active) {
                setSlotPosition(slot, OUTPUT_X, inputY + visibleRow * SLOT_STEP);
            }
        }
    }

    private void setSlotsActive(appeng.menu.SlotSemantic semantic, boolean active) {
        for (Slot slot : menu.getSlots(semantic)) {
            if (slot instanceof appeng.menu.slot.AppEngSlot appEngSlot) {
                appEngSlot.setActive(active);
            }
        }
    }

    private void layoutDynamicWidgets() {
        int colorButtonY = topPos + imageHeight - HEADER_COLOR_BOTTOM;
        int actionButtonY = topPos + imageHeight - HEADER_ACTION_BOTTOM;
        for (int visibleColumn = 0; visibleColumn < VISIBLE_COLUMNS; visibleColumn++) {
            int column = scrollColumn + visibleColumn;
            ColumnColorButton colorButton = columnColorButtons[visibleColumn];
            colorButton.column = column;
            colorButton.setX(leftPos + HEADER_COLOR_X + visibleColumn * COLUMN_STEP);
            colorButton.setY(colorButtonY);
            colorButton.visible = column < menu.activeColumns();
            colorButton.active = colorButton.visible && !colorPicker.isOpen();

            ColumnActionButton actionButton = columnActionButtons[visibleColumn];
            actionButton.column = column;
            actionButton.plus = column == menu.activeColumns();
            actionButton.setX(leftPos + HEADER_ACTION_X + visibleColumn * COLUMN_STEP);
            actionButton.setY(actionButtonY);
            actionButton.visible = column < menu.activeColumns()
                            || (actionButton.plus
                                    && menu.activeColumns()
                                            < AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS);
            actionButton.active = actionButton.visible && !colorPicker.isOpen();
            actionButton.setTooltip(Tooltip.create(Component.translatable(
                    actionButton.plus
                            ? "gui.appliedpackaging.advanced_pattern_terminal.add_column"
                            : "gui.appliedpackaging.advanced_pattern_terminal.clear_or_delete_column",
                    column + 1)));
        }

        clearButton.setX(leftPos + CLEAR_BUTTON_X);
        clearButton.setY(topPos + imageHeight - CLEAR_BUTTON_BOTTOM);
        encodeButton.setX(leftPos + ENCODE_BUTTON_X);
        encodeButton.setY(topPos + imageHeight - ENCODE_BUTTON_BOTTOM);

    }

    private void drawEncodingSlotBackgrounds(GuiGraphics graphics, int offsetX, int offsetY) {
        int inputY = imageHeight - INPUT_BOTTOM;
        for (int visibleColumn = 0; visibleColumn < VISIBLE_COLUMNS; visibleColumn++) {
            int column = scrollColumn + visibleColumn;
            boolean enabled = column < menu.activeColumns();
            for (int visibleRow = 0; visibleRow < VISIBLE_ROWS; visibleRow++) {
                int x = offsetX + INPUT_X + visibleColumn * COLUMN_STEP;
                int y = offsetY + inputY + visibleRow * SLOT_STEP;
                graphics.fill(x, y, x + 16, y + 16, enabled ? ENABLED_SLOT_BODY : DISABLED_SLOT_BODY);
            }
        }
        for (int visibleRow = 0; visibleRow < VISIBLE_ROWS; visibleRow++) {
            int x = offsetX + OUTPUT_X;
            int y = offsetY + inputY + visibleRow * SLOT_STEP;
            graphics.fill(x, y, x + 16, y + 16, ENABLED_SLOT_BODY);
        }
    }

    private void drawPrimaryOutputOverlay(GuiGraphics graphics, int offsetX, int offsetY) {
        Slot primaryOutput = menu.getProcessingOutputSlots()[0];
        if (primaryOutput.isActive() && !primaryOutput.hasItem()) {
            LATEST_PRIMARY_OUTPUT
                    .dest(offsetX + primaryOutput.x, offsetY + primaryOutput.y)
                    .blit(graphics);
        }
    }

    private void drawColumnScrollbar(GuiGraphics graphics, int offsetX, int offsetY) {
        int x = offsetX + COLUMN_SCROLLBAR_X;
        int y = offsetY + imageHeight - COLUMN_SCROLLBAR_BOTTOM;
        int max = maxScrollColumn();
        if (max <= 0) {
            COLUMN_SCROLLBAR.dest(x, y).blit(graphics);
            return;
        }
        int travel = COLUMN_SCROLLBAR_TRACK_WIDTH - 15;
        int thumbX = x + Math.round(travel * (scrollColumn / (float) max));
        COLUMN_SCROLLBAR.dest(thumbX, y).blit(graphics);
    }

    private void drawSlotIcon(
            GuiGraphics graphics,
            int offsetX,
            int offsetY,
            List<Slot> slots,
            Blitter icon) {
        if (!slots.isEmpty() && !slots.get(0).hasItem()) {
            Slot slot = slots.get(0);
            icon.dest(offsetX + slot.x, offsetY + slot.y).blit(graphics);
        }
    }

    private void clearLegacyPatternSlotIcons() {
        clearLegacyPatternSlotIcon(appeng.menu.SlotSemantics.BLANK_PATTERN);
        clearLegacyPatternSlotIcon(appeng.menu.SlotSemantics.ENCODED_PATTERN);
    }

    private void clearLegacyPatternSlotIcon(appeng.menu.SlotSemantic semantic) {
        for (Slot slot : menu.getSlots(semantic)) {
            if (slot instanceof AppEngSlot appEngSlot) {
                appEngSlot.setIcon(null);
            }
        }
    }

    private void replaceCraftingStatusButton() {
        legacyCraftingStatusButton = null;
        Component craftingStatus = GuiText.CraftingStatus.text();
        for (GuiEventListener child : children()) {
            if (child instanceof TabButton tabButton && tabButton.getMessage().equals(craftingStatus)) {
                legacyCraftingStatusButton = tabButton;
                break;
            }
        }
        if (legacyCraftingStatusButton == null) {
            return;
        }

        int x = legacyCraftingStatusButton.getX();
        int y = legacyCraftingStatusButton.getY();
        craftingStatusButton.setX(x);
        craftingStatusButton.setY(y);
        craftingStatusButton.setWidth(legacyCraftingStatusButton.getWidth());
        craftingStatusButton.setHeight(legacyCraftingStatusButton.getHeight());

        // The 1.20.1 foreground counter assumes a 16px icon. Offset its hidden
        // anchor so the count lands at the 18px icon position used by newer AE2.
        legacyCraftingStatusButton.setX(x - 1);
        legacyCraftingStatusButton.setY(y - 1);
        legacyCraftingStatusButton.visible = false;
    }

    private void openColumnEditor(int column, AbstractWidget anchor) {
        if (column >= 0 && column < menu.activeColumns()) {
            editedColumn = column;
            colorPicker.openNear(
                    anchor,
                    width,
                    height,
                    () -> menu.columnColor(column),
                    color -> menu.setColumnColor(column, color),
                    () -> {
                        editedColumn = -1;
                        setFocused(null);
                    });
        }
    }

    private void openColumnEditor(int column) {
        for (ColumnColorButton button : columnColorButtons) {
            if (button.column == column) {
                openColumnEditor(column, button);
                return;
            }
        }
    }

    private void closeColumnEditor() {
        colorPicker.close();
        editedColumn = -1;
        setFocused(null);
    }

    private int visibleColumnCount() {
        return Math.min(
                AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS,
                menu.activeColumns()
                        + (menu.activeColumns() < AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS ? 1 : 0));
    }

    private int maxScrollColumn() {
        return Math.max(0, visibleColumnCount() - VISIBLE_COLUMNS);
    }

    private boolean isOverInputPanel(double mouseX, double mouseY) {
        int x = leftPos + INPUT_X;
        int y = topPos + imageHeight - INPUT_BOTTOM;
        return mouseX >= x && mouseX < x + INPUT_PANEL_WIDTH
                && mouseY >= y && mouseY < y + VISIBLE_ROWS * SLOT_STEP;
    }

    private boolean isOverColumnHeaders(double mouseX, double mouseY) {
        int x = leftPos + HEADER_COLOR_X;
        int y = topPos + imageHeight - HEADER_COLOR_BOTTOM;
        return mouseX >= x && mouseX < x + INPUT_PANEL_WIDTH
                && mouseY >= y && mouseY < y + HEADER_BUTTON_SIZE;
    }

    private boolean isOverProcessingGrid(double mouseX, double mouseY) {
        int x = leftPos + INPUT_X;
        int y = topPos + imageHeight - INPUT_BOTTOM;
        return mouseX >= x && mouseX < leftPos + OUTPUT_X + 16
                && mouseY >= y && mouseY < y + VISIBLE_ROWS * SLOT_STEP;
    }

    private boolean isOverColumnScrollbar(double mouseX, double mouseY) {
        int x = leftPos + COLUMN_SCROLLBAR_X;
        int y = topPos + imageHeight - COLUMN_SCROLLBAR_BOTTOM;
        return mouseX >= x && mouseX < x + COLUMN_SCROLLBAR_TRACK_WIDTH
                && mouseY >= y && mouseY < y + 8;
    }

    private void updateColumnScrollFromMouse(double mouseX) {
        int max = maxScrollColumn();
        if (max <= 0) {
            scrollColumn = 0;
            return;
        }
        double relative = mouseX - (leftPos + COLUMN_SCROLLBAR_X) - 7.5;
        double travel = COLUMN_SCROLLBAR_TRACK_WIDTH - 15.0;
        scrollColumn = Math.max(0, Math.min(max, (int) Math.round(relative / travel * max)));
    }

    private static void setSlotPosition(Slot slot, int x, int y) {
        SlotAccessor accessor = (SlotAccessor) slot;
        accessor.appliedpackaging$setX(x);
        accessor.appliedpackaging$setY(y);
    }

    private final class ColumnColorButton extends AbstractButton {
        private int column;

        private ColumnColorButton(int visibleColumn) {
            super(
                    0,
                    0,
                    HEADER_BUTTON_SIZE,
                    HEADER_BUTTON_SIZE,
                    Component.translatable(
                            "gui.appliedpackaging.advanced_pattern_terminal.edit_color",
                            visibleColumn + 1));
        }

        @Override
        public void onPress() {
            openColumnEditor(column, this);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            COLOR_BUTTON.dest(getX(), getY()).blit(graphics);
            graphics.fill(
                    getX() + 1,
                    getY() + 1,
                    getX() + 7,
                    getY() + 7,
                    menu.columnColor(column).swatchArgb());
            if (isHoveredOrFocused()) {
                graphics.renderOutline(getX(), getY(), width, height, 0xffffffff);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class ColumnActionButton extends AbstractButton {
        private int column;
        private boolean plus;

        private ColumnActionButton(int visibleColumn) {
            super(
                    0,
                    0,
                    HEADER_BUTTON_SIZE,
                    HEADER_BUTTON_SIZE,
                    Component.translatable(
                            "gui.appliedpackaging.advanced_pattern_terminal.clear_or_delete_column",
                            visibleColumn + 1));
        }

        @Override
        public void onPress() {
            if (plus) {
                menu.addColumn();
            } else {
                menu.clearOrDeleteColumn(column);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            (plus ? ADD_COLUMN_BUTTON : DELETE_COLUMN_BUTTON).dest(getX(), getY()).blit(graphics);
            if (isHoveredOrFocused()) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x30ffffff);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class CompactClearButton extends AbstractButton {
        private CompactClearButton() {
            super(0, 0, 8, 8, ButtonToolTips.Clear.text());
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            menu.clear();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            CLEAR_BUTTON.dest(getX(), getY()).blit(graphics);
            if (isHoveredOrFocused()) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x30ffffff);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class LatestEncodeButton extends AbstractButton {
        private LatestEncodeButton() {
            super(0, 0, 16, 16, ButtonToolTips.Encode.text());
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            menu.encode();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int yOffset = isHovered() ? 1 : 0;
            Blitter background = isHovered()
                    ? LATEST_ENCODE_BUTTON_HOVER
                    : isFocused() ? LATEST_ENCODE_BUTTON_FOCUS : LATEST_ENCODE_BUTTON;
            background.dest(getX() - 1, getY() + yOffset).blit(graphics);
            LATEST_ENCODE_ICON.dest(getX(), getY() + 1 + yOffset).blit(graphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class LatestCraftingStatusButton extends AbstractButton {
        private LatestCraftingStatusButton() {
            super(0, 0, 20, 20, GuiText.CraftingStatus.text());
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            if (legacyCraftingStatusButton != null) {
                legacyCraftingStatusButton.onPress();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Blitter background = isFocused() ? LATEST_TAB_BUTTON_FOCUS : LATEST_TAB_BUTTON;
            background.dest(getX(), getY()).blit(graphics);
            LATEST_CRAFT_HAMMER.dest(getX() + 2, getY() + 1).blit(graphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}

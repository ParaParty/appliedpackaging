package com.warmthdawn.appliedpackaging.client.screen;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.EmptyingAction;
import appeng.api.config.ActionItems;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.Icon;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.items.SetProcessingPatternAmountScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.core.AEConfig;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.Tooltips;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import com.warmthdawn.appliedpackaging.client.widget.PackageColorButton;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.mixin.client.SlotAccessor;
import com.warmthdawn.appliedpackaging.world.menu.AdvancedPatternEncodingTermMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class AdvancedPatternEncodingTermScreen
        extends MEStorageScreen<AdvancedPatternEncodingTermMenu> {
    private static final int VISIBLE_COLUMNS = 4;
    private static final int INPUT_X = 22;
    private static final int INPUT_BOTTOM = 171;
    private static final int OUTPUT_X = 120;
    private static final int SLOT_STEP = 18;
    private static final int COLUMN_STEP = 22;
    private static final int HEADER_BUTTON_BOTTOM = 183;
    private static final int HEADER_BUTTON_SIZE = 10;
    private static final int INPUT_PANEL_WIDTH = (VISIBLE_COLUMNS - 1) * COLUMN_STEP + SLOT_STEP;
    private static final int SCROLLBAR_X = 10;
    private static final int SCROLLBAR_BOTTOM = INPUT_BOTTOM;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_HEIGHT = SLOT_STEP * AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
    private static final int POPUP_X = 9;
    private static final int POPUP_BOTTOM = 186;
    private static final int POPUP_WIDTH = 138;
    private static final int POPUP_HEIGHT = 83;
    private static final int POPUP_SWATCH_X = 9;
    private static final int POPUP_SWATCH_Y = 16;
    private static final int POPUP_SWATCH_COLUMNS = 9;
    private static final int POPUP_SWATCH_STEP = 9;
    private static final int POPUP_NAME_X = 9;
    private static final int POPUP_NAME_Y = 56;
    private static final int POPUP_NAME_WIDTH = 86;
    private static final int POPUP_MARKER_X = 108;
    private static final int POPUP_MARKER_Y = 53;

    private static final int SLOT_TOP = 0xffeeeef2;
    private static final int SLOT_INNER_TOP = 0xff858ba4;
    private static final int SLOT_BODY = 0xffa7acc0;
    private static final int SLOT_BOTTOM = 0xffc2c5cf;
    private static final int TRACK = 0xff6d718b;
    private static final int THUMB = 0xffa7acc0;
    private static final int THUMB_HIGHLIGHT = 0xffeeeef2;
    private static final int POPUP_BACKGROUND = 0xf0c8cad5;
    private static final int POPUP_BORDER = 0xff6d718b;

    private final ColumnHeaderButton[] columnButtons = new ColumnHeaderButton[VISIBLE_COLUMNS];
    private final List<PackageColorButton> colorButtons = new ArrayList<>();
    private final EditBox packageNameField;
    private final ActionButton clearButton;
    private final ActionButton cycleOutputButton;
    private int scrollColumn;
    private int editedColumn = -1;
    private boolean updatingName;
    private boolean draggingScrollbar;

    public AdvancedPatternEncodingTermScreen(
            AdvancedPatternEncodingTermMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        widgets.add("encodePattern", new ActionButton(ActionItems.ENCODE, ignored -> menu.encode()));
        clearButton = new ActionButton(ActionItems.CLOSE, ignored -> menu.clear());
        clearButton.setHalfSize(true);
        widgets.add("processingClearPattern", clearButton);
        cycleOutputButton = new ActionButton(
                ActionItems.CYCLE_PROCESSING_OUTPUT,
                ignored -> menu.cycleProcessingOutput());
        cycleOutputButton.setHalfSize(true);
        widgets.add("processingCycleOutput", cycleOutputButton);

        for (int visibleColumn = 0; visibleColumn < columnButtons.length; visibleColumn++) {
            columnButtons[visibleColumn] = new ColumnHeaderButton(visibleColumn);
        }
        for (PackageColor color : PackageColor.values()) {
            colorButtons.add(new PackageColorButton(color, button -> {
                if (editedColumn >= 0) {
                    menu.setColumnColor(editedColumn, color);
                }
            }));
        }

        packageNameField = new EditBox(
                font,
                0,
                0,
                POPUP_NAME_WIDTH,
                12,
                Component.translatable("gui.appliedpackaging.advanced_pattern_terminal.package_name"));
        packageNameField.setMaxLength(50);
        packageNameField.setResponder(value -> {
            if (!updatingName && editedColumn >= 0) {
                menu.setColumnName(editedColumn, value);
            }
        });
    }

    @Override
    public void init() {
        super.init();
        for (ColumnHeaderButton button : columnButtons) {
            addRenderableWidget(button);
        }
        for (PackageColorButton button : colorButtons) {
            addRenderableWidget(button);
        }
        addRenderableWidget(packageNameField);
        layoutDynamicWidgets();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        scrollColumn = Math.max(0, Math.min(scrollColumn, maxScrollColumn()));
        updateProcessingSlots();
        layoutDynamicWidgets();
        cycleOutputButton.setVisibility(menu.canCycleProcessingOutputs());

        if (editedColumn >= menu.activeColumns()) {
            closeColumnEditor();
        }
        if (editedColumn >= 0 && !packageNameField.isFocused()) {
            String name = menu.columnName(editedColumn);
            if (!packageNameField.getValue().equals(name)) {
                updatingName = true;
                try {
                    packageNameField.setValue(name);
                } finally {
                    updatingName = false;
                }
            }
        }
        for (PackageColorButton button : colorButtons) {
            boolean visible = editedColumn >= 0;
            button.visible = visible;
            button.active = visible;
            button.setSelected(visible && button.color() == menu.columnColor(editedColumn));
        }
        packageNameField.visible = editedColumn >= 0;
        packageNameField.active = editedColumn >= 0;
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

        if (editedColumn >= 0) {
            drawColumnEditorBackground(graphics, offsetX, offsetY);
        } else {
            drawEncodingSlotBackgrounds(graphics, offsetX, offsetY);
            drawColumnScrollbar(graphics, offsetX, offsetY);
        }
        drawSlotIcon(graphics, offsetX, offsetY, menu.getSlots(appeng.menu.SlotSemantics.BLANK_PATTERN),
                Icon.BACKGROUND_BLANK_PATTERN);
        drawSlotIcon(graphics, offsetX, offsetY, menu.getSlots(appeng.menu.SlotSemantics.ENCODED_PATTERN),
                Icon.BACKGROUND_ENCODED_PATTERN);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editedColumn >= 0) {
            if (!isInsidePopup(mouseX, mouseY)) {
                closeColumnEditor();
                return true;
            }
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (isOverColumnScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        if (editedColumn >= 0) {
            super.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        if (editedColumn >= 0) {
            super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (editedColumn >= 0) {
            return true;
        }
        if (isOverInputPanel(mouseX, mouseY) && maxScrollColumn() > 0) {
            scrollColumn = Math.max(0, Math.min(maxScrollColumn(), scrollColumn + (delta < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editedColumn >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeColumnEditor();
                return true;
            }
            super.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (editedColumn >= 0) {
            super.charTyped(codePoint, modifiers);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
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

        int inputY = imageHeight - INPUT_BOTTOM;
        var inputs = menu.getProcessingInputSlots();
        for (int slotIndex = 0; slotIndex < inputs.length; slotIndex++) {
            var slot = inputs[slotIndex];
            if (slotIndex >= AdvancedProcessingPatternDataStorage.MAX_INPUT_SLOTS || editedColumn >= 0) {
                slot.setActive(false);
                continue;
            }
            int column = slotIndex / AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
            int row = slotIndex % AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
            int visibleColumn = column - scrollColumn;
            boolean active = column < menu.activeColumns() && visibleColumn >= 0 && visibleColumn < VISIBLE_COLUMNS;
            slot.setActive(active);
            if (active) {
                setSlotPosition(slot, INPUT_X + visibleColumn * COLUMN_STEP, inputY + row * SLOT_STEP);
            }
        }

        var outputs = menu.getProcessingOutputSlots();
        for (int slotIndex = 0; slotIndex < outputs.length; slotIndex++) {
            var slot = outputs[slotIndex];
            boolean active = editedColumn < 0 && slotIndex < AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS;
            slot.setActive(active);
            if (active) {
                setSlotPosition(slot, OUTPUT_X, inputY + slotIndex * SLOT_STEP);
            }
        }

        for (int column = 0; column < menu.markerSlots().length; column++) {
            var marker = menu.markerSlot(column);
            boolean active = editedColumn == column;
            marker.setActive(active);
            if (active) {
                setSlotPosition(
                        marker,
                        POPUP_X + POPUP_MARKER_X,
                        imageHeight - POPUP_BOTTOM + POPUP_MARKER_Y);
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
        int buttonY = topPos + imageHeight - HEADER_BUTTON_BOTTOM;
        for (int visibleColumn = 0; visibleColumn < columnButtons.length; visibleColumn++) {
            ColumnHeaderButton button = columnButtons[visibleColumn];
            int column = scrollColumn + visibleColumn;
            button.column = column;
            button.setX(leftPos + INPUT_X + visibleColumn * COLUMN_STEP + 3);
            button.setY(buttonY);
            button.visible = editedColumn < 0
                    && (column < menu.activeColumns()
                            || (column == menu.activeColumns()
                                    && menu.activeColumns()
                                            < AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS));
            button.active = button.visible;
            button.plus = column == menu.activeColumns();
            button.setTooltip(Tooltip.create(Component.translatable(
                    button.plus
                            ? "gui.appliedpackaging.advanced_pattern_terminal.add_column"
                            : "gui.appliedpackaging.advanced_pattern_terminal.edit_column",
                    column + 1)));
        }

        int popupX = leftPos + POPUP_X;
        int popupY = topPos + imageHeight - POPUP_BOTTOM;
        for (int index = 0; index < colorButtons.size(); index++) {
            PackageColorButton button = colorButtons.get(index);
            button.setX(popupX + POPUP_SWATCH_X + (index % POPUP_SWATCH_COLUMNS) * POPUP_SWATCH_STEP);
            button.setY(popupY + POPUP_SWATCH_Y + (index / POPUP_SWATCH_COLUMNS) * POPUP_SWATCH_STEP);
        }
        packageNameField.setX(popupX + POPUP_NAME_X);
        packageNameField.setY(popupY + POPUP_NAME_Y);
    }

    private void drawEncodingSlotBackgrounds(GuiGraphics graphics, int offsetX, int offsetY) {
        int inputY = imageHeight - INPUT_BOTTOM;
        for (int visibleColumn = 0; visibleColumn < VISIBLE_COLUMNS; visibleColumn++) {
            int column = scrollColumn + visibleColumn;
            boolean enabled = column < menu.activeColumns();
            for (int row = 0; row < AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE; row++) {
                drawSlotBackground(
                        graphics,
                        offsetX + INPUT_X + visibleColumn * COLUMN_STEP - 1,
                        offsetY + inputY + row * SLOT_STEP - 1,
                        enabled);
            }
        }
        for (int row = 0; row < AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS; row++) {
            drawSlotBackground(
                    graphics,
                    offsetX + OUTPUT_X - 1,
                    offsetY + inputY + row * SLOT_STEP - 1,
                    true);
        }
        if (!menu.getProcessingOutputSlots()[0].hasItem()) {
            Icon.BACKGROUND_PRIMARY_OUTPUT.getBlitter()
                    .dest(offsetX + OUTPUT_X, offsetY + inputY)
                    .blit(graphics);
        }
    }

    private void drawColumnEditorBackground(GuiGraphics graphics, int offsetX, int offsetY) {
        int x = offsetX + POPUP_X;
        int y = offsetY + imageHeight - POPUP_BOTTOM;
        graphics.fill(x, y, x + POPUP_WIDTH, y + POPUP_HEIGHT, POPUP_BACKGROUND);
        graphics.renderOutline(x, y, POPUP_WIDTH, POPUP_HEIGHT, POPUP_BORDER);
        graphics.drawString(
                font,
                Component.translatable("gui.appliedpackaging.advanced_pattern_terminal.column", editedColumn + 1),
                x + 8,
                y + 5,
                0xff303038,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.appliedpackaging.advanced_pattern_terminal.marker"),
                x + 101,
                y + 43,
                0xff303038,
                false);
        drawSlotBackground(
                graphics,
                x + POPUP_MARKER_X - 1,
                y + POPUP_MARKER_Y - 1,
                true);
    }

    private void drawColumnScrollbar(GuiGraphics graphics, int offsetX, int offsetY) {
        int x = offsetX + SCROLLBAR_X;
        int y = offsetY + imageHeight - SCROLLBAR_BOTTOM;
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + SCROLLBAR_HEIGHT, TRACK);
        int max = maxScrollColumn();
        int totalColumns = Math.max(VISIBLE_COLUMNS, visibleColumnCount());
        int thumbHeight = max <= 0
                ? SCROLLBAR_HEIGHT
                : Math.max(12, SCROLLBAR_HEIGHT * VISIBLE_COLUMNS / totalColumns);
        int travel = Math.max(0, SCROLLBAR_HEIGHT - thumbHeight);
        int thumbY = y + (max <= 0 ? 0 : Math.round(travel * (scrollColumn / (float) max)));
        graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, THUMB);
        graphics.vLine(x + 1, thumbY, thumbY + thumbHeight - 1, THUMB_HIGHLIGHT);
    }

    private void drawSlotBackground(GuiGraphics graphics, int x, int y, boolean enabled) {
        int alpha = enabled ? 0xff : 0x33;
        graphics.fill(x, y, x + 18, y + 1, withAlpha(SLOT_TOP, alpha));
        graphics.fill(x, y + 1, x + 1, y + 18, withAlpha(SLOT_TOP, alpha));
        graphics.fill(x + 1, y + 1, x + 17, y + 2, withAlpha(SLOT_INNER_TOP, alpha));
        graphics.fill(x + 1, y + 2, x + 17, y + 17, withAlpha(SLOT_BODY, alpha));
        graphics.fill(x + 17, y + 1, x + 18, y + 18, withAlpha(SLOT_BOTTOM, alpha));
        graphics.fill(x + 1, y + 17, x + 18, y + 18, withAlpha(SLOT_BOTTOM, alpha));
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00ffffff);
    }

    private void drawSlotIcon(
            GuiGraphics graphics,
            int offsetX,
            int offsetY,
            List<Slot> slots,
            Icon icon) {
        if (!slots.isEmpty() && !slots.get(0).hasItem()) {
            Slot slot = slots.get(0);
            icon.getBlitter().dest(offsetX + slot.x, offsetY + slot.y).blit(graphics);
        }
    }

    private void openColumnEditor(int column) {
        if (column < 0 || column >= menu.activeColumns()) {
            return;
        }
        editedColumn = column;
        updatingName = true;
        try {
            packageNameField.setValue(menu.columnName(column));
        } finally {
            updatingName = false;
        }
        packageNameField.setFocused(false);
    }

    private void closeColumnEditor() {
        editedColumn = -1;
        packageNameField.setFocused(false);
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
        int y = topPos + imageHeight - HEADER_BUTTON_BOTTOM;
        return mouseX >= x && mouseX < x + INPUT_PANEL_WIDTH
                && mouseY >= y && mouseY < topPos + imageHeight - INPUT_BOTTOM + SCROLLBAR_HEIGHT;
    }

    private boolean isOverColumnScrollbar(double mouseX, double mouseY) {
        int x = leftPos + SCROLLBAR_X;
        int y = topPos + imageHeight - SCROLLBAR_BOTTOM;
        return mouseX >= x && mouseX < x + SCROLLBAR_WIDTH
                && mouseY >= y && mouseY < y + SCROLLBAR_HEIGHT;
    }

    private void updateScrollFromMouse(double mouseY) {
        int max = maxScrollColumn();
        if (max <= 0) {
            scrollColumn = 0;
            return;
        }
        double relative = mouseY - (topPos + imageHeight - SCROLLBAR_BOTTOM);
        scrollColumn = Math.max(0, Math.min(max, (int) Math.round(relative / SCROLLBAR_HEIGHT * max)));
    }

    private boolean isInsidePopup(double mouseX, double mouseY) {
        int x = leftPos + POPUP_X;
        int y = topPos + imageHeight - POPUP_BOTTOM;
        return mouseX >= x && mouseX < x + POPUP_WIDTH
                && mouseY >= y && mouseY < y + POPUP_HEIGHT;
    }

    private static void setSlotPosition(Slot slot, int x, int y) {
        SlotAccessor accessor = (SlotAccessor) slot;
        accessor.appliedpackaging$setX(x);
        accessor.appliedpackaging$setY(y);
    }

    private final class ColumnHeaderButton extends AbstractButton {
        private int column;
        private boolean plus;

        private ColumnHeaderButton(int visibleColumn) {
            super(
                    0,
                    0,
                    HEADER_BUTTON_SIZE,
                    HEADER_BUTTON_SIZE,
                    Component.translatable("gui.appliedpackaging.advanced_pattern_terminal.edit_column",
                            visibleColumn + 1));
        }

        @Override
        public void onPress() {
            if (plus) {
                menu.addColumn();
                return;
            }
            openColumnEditor(column);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int border = isHoveredOrFocused() ? 0xffffffff : 0xff6d718b;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xffa7acc0);
            if (plus) {
                graphics.drawCenteredString(font, "+", getX() + width / 2, getY() + 1, 0xffffffff);
            } else {
                graphics.fill(
                        getX() + 2,
                        getY() + 2,
                        getX() + width - 2,
                        getY() + height - 2,
                        menu.columnColor(column).swatchArgb());
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}

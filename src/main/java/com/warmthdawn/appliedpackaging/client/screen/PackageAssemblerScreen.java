package com.warmthdawn.appliedpackaging.client.screen;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.IconButton;
import appeng.menu.SlotSemantics;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.world.menu.PackageAssemblerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

public class PackageAssemblerScreen extends UpgradeableScreen<PackageAssemblerMenu> {
    private static final int COLOR_BUTTON_X = 67;
    private static final int COLOR_BUTTON_Y = 39;
    private static final int COLOR_BUTTON_SIZE = 12;
    private static final int COLOR_POPUP_X = 8;
    private static final int COLOR_POPUP_Y = 52;
    private static final int COLOR_POPUP_COLUMNS = 9;
    private static final int COLOR_POPUP_PADDING = 3;
    private static final int COLOR_SWATCH_SIZE = 8;
    private static final int COLOR_SWATCH_STEP = 10;
    private static final int COLOR_POPUP_BACKGROUND = 0xf0180a1f;
    private static final int COLOR_POPUP_BORDER = 0xff4d3f5c;
    private static final int SCROLLBAR_X = 13;
    private static final int SCROLLBAR_Y = PackageAssemblerMenu.SCROLLED_SLOT_Y;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_HEIGHT = PackageAssemblerMenu.VISIBLE_ROWS * PackageAssemblerMenu.SLOT_STEP - 2;
    private static final int SCROLLBAR_PANEL_X = 10;
    private static final int SCROLLBAR_PANEL_WIDTH = 143;
    private static final int SCROLLBAR_TRACK = 0xff8b93a6;
    private static final int SCROLLBAR_THUMB = 0xff4a5058;
    private static final int SCROLLBAR_HIGHLIGHT = 0xffd6dbde;
    private static final int SLOT_BACKGROUND_TOP = 0xff9a9fb4;
    private static final int SLOT_BACKGROUND_BODY = 0xffadb0c4;
    private static final int SLOT_DISABLED_OVERLAY = 0x99c7ccd5;
    private static final int SLOT_INVALID_OVERLAY = 0x55ff3333;
    private static final int SLOT_INVALID_BORDER = 0xffff5555;

    private final OutputModeToolbarButton outputModeButton;
    private final AETextField packageNameField;
    private boolean colorPopupOpen;
    private boolean updatingNameField;

    public PackageAssemblerScreen(
            PackageAssemblerMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
        outputModeButton = addToLeftToolbar(new OutputModeToolbarButton());

        packageNameField = widgets.addTextField("packageName");
        packageNameField.setMaxLength(50);
        packageNameField.setPlaceholder(Component.translatable(
                "gui.appliedpackaging.package_assembler.package_name.placeholder"));
        packageNameField.setTooltipMessage(java.util.List.of(Component.translatable(
                "gui.appliedpackaging.package_assembler.package_name")));
        packageNameField.setResponder(value -> {
            if (!updatingNameField) {
                menu.setPackageName(value);
            }
        });
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(new ColorPickerButton(leftPos + COLOR_BUTTON_X, topPos + COLOR_BUTTON_Y));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        outputModeButton.setMessage(outputModeMessage());
        if (!packageNameField.isFocused() && !packageNameField.getValue().equals(menu.packageName())) {
            updatingNameField = true;
            try {
                packageNameField.setValue(menu.packageName());
            } finally {
                updatingNameField = false;
            }
        }
    }

    @Override
    public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        renderScrolledSlotBackgrounds(graphics, offsetX, offsetY);
        drawSlotIcon(graphics, offsetX, offsetY, SlotSemantics.ENCODED_PATTERN, Icon.BACKGROUND_ENCODED_PATTERN);
        drawSlotIcon(graphics, offsetX, offsetY, SlotSemantics.STORAGE_CELL, Icon.BACKGROUND_STORAGE_COMPONENT);
    }

    @Override
    public void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        int inputSlot = menu.inputSlotForMenuSlotIndex(slot.index);
        if (inputSlot >= 0 && slot.hasItem() && !menu.isInputSlotValid(inputSlot)) {
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, SLOT_INVALID_OVERLAY);
            graphics.renderOutline(slot.x - 1, slot.y - 1, 18, 18, SLOT_INVALID_BORDER);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderScrollbar(graphics);
        if (colorPopupOpen) {
            renderColorPopup(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (colorPopupOpen) {
            return true;
        }
        if (isOverScrolledPanel(mouseX, mouseY)) {
            setScrollOffset(menu.scrollOffset() + (delta < 0 ? 1 : -1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPopupOpen) {
            if (isInColorButton(mouseX, mouseY)) {
                colorPopupOpen = false;
                return true;
            }

            PackageColor color = colorAt(mouseX, mouseY);
            if (color != null) {
                menu.setSelectedColor(color);
                colorPopupOpen = false;
                return true;
            }

            colorPopupOpen = false;
            return true;
        }

        if (isOverScrollbar(mouseX, mouseY)) {
            int relative = (int) (mouseY - topPos - SCROLLBAR_Y);
            int max = Math.max(1, menu.maxScrollOffset());
            int next = Math.round(relative / (float) SCROLLBAR_HEIGHT * max);
            setScrollOffset(next);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (colorPopupOpen) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (colorPopupOpen) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (colorPopupOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                colorPopupOpen = false;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (colorPopupOpen) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void renderScrolledSlotBackgrounds(GuiGraphics graphics, int offsetX, int offsetY) {
        for (int index = 0; index < PackageAssemblerMenu.VISIBLE_INPUT_COUNT; index++) {
            int inputSlot = menu.inputSlotForVisibleIndex(index);
            drawSlotBackground(
                    graphics,
                    offsetX,
                    offsetY,
                    menu.getSlot(menu.menuInputMenuSlotIndex(index)),
                    menu.isInputSlotEnabled(inputSlot));
        }
        for (int row = 0; row < PackageAssemblerMenu.VISIBLE_ROWS; row++) {
            drawSlotBackground(graphics, offsetX, offsetY, menu.getSlot(menu.outputMenuSlotIndex(row)), true);
        }
    }

    private void drawSlotBackground(GuiGraphics graphics, int offsetX, int offsetY, Slot slot, boolean enabled) {
        int x = offsetX + slot.x - 1;
        int y = offsetY + slot.y - 1;
        graphics.fill(x + 1, y + 1, x + 17, y + 2, SLOT_BACKGROUND_TOP);
        graphics.fill(x + 1, y + 2, x + 17, y + 17, SLOT_BACKGROUND_BODY);
        if (!enabled) {
            graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_DISABLED_OVERLAY);
        }
    }

    private void drawSlotIcon(GuiGraphics graphics, int offsetX, int offsetY, appeng.menu.SlotSemantic semantic, Icon icon) {
        java.util.List<Slot> slots = menu.getSlots(semantic);
        if (slots.isEmpty()) {
            return;
        }
        Slot slot = slots.get(0);
        if (!slot.hasItem()) {
            icon.getBlitter()
                    .dest(offsetX + slot.x, offsetY + slot.y)
                    .blit(graphics);
        }
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int x = leftPos + SCROLLBAR_X;
        int y = topPos + SCROLLBAR_Y;
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + SCROLLBAR_HEIGHT, SCROLLBAR_TRACK);
        int max = menu.maxScrollOffset();
        int thumbHeight = max <= 0
                ? SCROLLBAR_HEIGHT
                : Math.max(8, SCROLLBAR_HEIGHT * PackageAssemblerMenu.VISIBLE_ROWS / PackageAssemblerMenu.SCROLLED_ROW_COUNT);
        int travel = Math.max(0, SCROLLBAR_HEIGHT - thumbHeight);
        int thumbY = y + (max <= 0 ? 0 : Math.round(travel * (menu.scrollOffset() / (float) max)));
        graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_THUMB);
        graphics.hLine(x, x + SCROLLBAR_WIDTH - 1, thumbY + 1, SCROLLBAR_HIGHLIGHT);
    }

    private boolean isOverScrolledPanel(double mouseX, double mouseY) {
        int x = leftPos + SCROLLBAR_PANEL_X;
        int y = topPos + SCROLLBAR_Y;
        return mouseX >= x && mouseX < x + SCROLLBAR_PANEL_WIDTH
                && mouseY >= y && mouseY < y + SCROLLBAR_HEIGHT;
    }

    private void setScrollOffset(int offset) {
        int previous = menu.scrollOffset();
        menu.setScrollOffset(offset);
        if (menu.scrollOffset() != previous && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(
                    menu.containerId,
                    PackageAssemblerMenu.BUTTON_SCROLL_BASE + menu.scrollOffset());
        }
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        int x = leftPos + SCROLLBAR_X;
        int y = topPos + SCROLLBAR_Y;
        return mouseX >= x && mouseX < x + SCROLLBAR_WIDTH
                && mouseY >= y && mouseY < y + SCROLLBAR_HEIGHT;
    }

    private Component outputModeMessage() {
        return Component.translatable(
                "gui.appliedpackaging.package_assembler.output_mode."
                        + menu.outputMode().id());
    }

    private void renderColorPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        int panelX = leftPos + COLOR_POPUP_X - COLOR_POPUP_PADDING;
        int panelY = topPos + COLOR_POPUP_Y - COLOR_POPUP_PADDING;
        int panelWidth = COLOR_POPUP_COLUMNS * COLOR_SWATCH_STEP + COLOR_POPUP_PADDING * 2;
        int panelHeight = colorPopupRows() * COLOR_SWATCH_STEP + COLOR_POPUP_PADDING * 2;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_POPUP_BACKGROUND);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, COLOR_POPUP_BORDER);

        PackageColor[] colors = PackageColor.values();
        for (int index = 0; index < colors.length; index++) {
            PackageColor color = colors[index];
            int x = swatchX(index);
            int y = swatchY(index);
            boolean selected = menu.selectedColor() == color;
            boolean hovered = mouseX >= x && mouseX < x + COLOR_SWATCH_SIZE
                    && mouseY >= y && mouseY < y + COLOR_SWATCH_SIZE;
            int border = selected ? 0xffffffff : (hovered ? 0xffd6dbde : 0xff2a3036);
            graphics.fill(x, y, x + COLOR_SWATCH_SIZE, y + COLOR_SWATCH_SIZE, border);
            graphics.fill(x + 1, y + 1, x + COLOR_SWATCH_SIZE - 1, y + COLOR_SWATCH_SIZE - 1, color.swatchArgb());
            if (selected) {
                graphics.renderOutline(x - 1, y - 1, COLOR_SWATCH_SIZE + 2, COLOR_SWATCH_SIZE + 2, 0xff2a3036);
            }
        }
    }

    private boolean isInColorButton(double mouseX, double mouseY) {
        int x = leftPos + COLOR_BUTTON_X;
        int y = topPos + COLOR_BUTTON_Y;
        return mouseX >= x && mouseX < x + COLOR_BUTTON_SIZE
                && mouseY >= y && mouseY < y + COLOR_BUTTON_SIZE;
    }

    private PackageColor colorAt(double mouseX, double mouseY) {
        PackageColor[] colors = PackageColor.values();
        for (int index = 0; index < colors.length; index++) {
            int x = swatchX(index);
            int y = swatchY(index);
            if (mouseX >= x && mouseX < x + COLOR_SWATCH_SIZE
                    && mouseY >= y && mouseY < y + COLOR_SWATCH_SIZE) {
                return colors[index];
            }
        }
        return null;
    }

    private int swatchX(int index) {
        return leftPos + COLOR_POPUP_X + (index % COLOR_POPUP_COLUMNS) * COLOR_SWATCH_STEP;
    }

    private int swatchY(int index) {
        return topPos + COLOR_POPUP_Y + (index / COLOR_POPUP_COLUMNS) * COLOR_SWATCH_STEP;
    }

    private static int colorPopupRows() {
        return (PackageColor.values().length + COLOR_POPUP_COLUMNS - 1) / COLOR_POPUP_COLUMNS;
    }

    private class OutputModeToolbarButton extends IconButton {
        private OutputModeToolbarButton() {
            super(button -> menu.cycleOutputMode());
            setMessage(outputModeMessage());
        }

        @Override
        protected Icon getIcon() {
            return switch (menu.outputMode()) {
                case ME_NETWORK -> Icon.AUTO_EXPORT_ON;
                case ADJACENT_BLOCK -> Icon.ARROW_RIGHT;
                case NONE -> Icon.AUTO_EXPORT_OFF;
            };
        }
    }

    private class ColorPickerButton extends AbstractButton {
        private ColorPickerButton(int x, int y) {
            super(x, y, COLOR_BUTTON_SIZE, COLOR_BUTTON_SIZE,
                    Component.translatable("gui.appliedpackaging.package_assembler.color"));
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            colorPopupOpen = !colorPopupOpen;
            if (colorPopupOpen) {
                packageNameField.setFocused(false);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX() + 3, getY() + 3, getX() + 9, getY() + 9, menu.selectedColor().swatchArgb());
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}

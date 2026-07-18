package com.warmthdawn.appliedpackaging.client.screen;

import appeng.client.Point;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.ProgressBar;
import appeng.client.gui.widgets.ProgressBar.Direction;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import com.warmthdawn.appliedpackaging.client.widget.ModernScrollbarStyles;
import com.warmthdawn.appliedpackaging.client.widget.ModernSlotRendering;
import com.warmthdawn.appliedpackaging.client.widget.PackageColorPicker;
import com.warmthdawn.appliedpackaging.world.menu.PackageAssemblerMenu;
import java.util.Optional;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PackageAssemblerScreen extends ModernUpgradeableScreen<PackageAssemblerMenu> {
    private static final int SCROLLBAR_Y = 31;
    private static final int SCROLLBAR_HEIGHT = 72;
    private static final int SCROLLBAR_PANEL_X = 10;
    private static final int SCROLLBAR_PANEL_WIDTH = 83;
    private static final int SLOT_INVALID_OVERLAY = 0x55ff3333;
    private static final int SLOT_INVALID_BORDER = 0xffff5555;
    private static final int COLOR_BUTTON_X = 95;
    private static final int COLOR_BUTTON_Y = 29;
    private static final int COLOR_BUTTON_SIZE = 12;

    private final OutputModeToolbarButton outputModeButton;
    private final BlockingModeToolbarButton blockingModeButton;
    private final Scrollbar rowScrollbar;
    private final ProgressBar progressBar;
    private final PackageColorPicker colorPicker = new PackageColorPicker();
    private final PackageColorPicker.TriggerButton colorButton;

    public PackageAssemblerScreen(
            PackageAssemblerMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
        rowScrollbar = widgets.addScrollBar("packageQueueScrollbar", ModernScrollbarStyles.SMALL);
        rowScrollbar.setRange(0, menu.maxScrollOffset(), PackageAssemblerMenu.VISIBLE_ROWS);
        rowScrollbar.setCaptureMouseWheel(false);
        progressBar = new ProgressBar(menu, style.getImage("progressBar"), Direction.VERTICAL);
        widgets.add("progressBar", progressBar);
        outputModeButton = addToLeftToolbar(new OutputModeToolbarButton());
        blockingModeButton = addToLeftToolbar(new BlockingModeToolbarButton());
        colorButton = new PackageColorPicker.TriggerButton(
                COLOR_BUTTON_SIZE,
                COLOR_BUTTON_SIZE,
                true,
                menu::effectiveColor,
                this::openColorPicker,
                () -> {
                });
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(colorButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        outputModeButton.setMessage(outputModeMessage());
        blockingModeButton.setMessage(blockingModeMessage());
        boolean working = menu.isCrafting();
        boolean colorEditable = menu.canEditColor();
        if (!colorEditable && colorPicker.isOpen()) {
            colorPicker.close();
        }
        outputModeButton.active = !working;
        blockingModeButton.active = !working;
        colorButton.setX(leftPos + COLOR_BUTTON_X);
        colorButton.setY(topPos + COLOR_BUTTON_Y);
        colorButton.active = colorEditable && !colorPicker.isOpen();
        colorButton.setIdleTooltip(Tooltip.create(colorTooltip()));
        progressBar.visible = menu.isCrafting();
        rowScrollbar.setRange(0, menu.maxScrollOffset(), PackageAssemblerMenu.VISIBLE_ROWS);
        if (rowScrollbar.getCurrentScroll() != menu.scrollOffset()) {
            setScrollOffset(rowScrollbar.getCurrentScroll());
        }
    }

    @Override
    public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        drawInputSlotBackgrounds(graphics, offsetX, offsetY);
        ModernSlotRendering.drawEncodedPatternSlotIcon(
                graphics,
                offsetX,
                offsetY,
                firstSlot(SlotSemantics.ENCODED_PATTERN));
        ModernSlotRendering.drawStorageComponentSlotIcon(
                graphics,
                offsetX,
                offsetY,
                firstSlot(SlotSemantics.STORAGE_CELL));
        ModernSlotRendering.drawMarkerSlotIcon(
                graphics,
                offsetX,
                offsetY,
                firstSlot(SlotSemantics.BLANK_PATTERN),
                menu.isCrafting() ? 0.5F : 1.0F);
    }

    @Override
    public void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        if (slot.hasItem()
                && slot == firstSlot(SlotSemantics.ENCODED_PATTERN)
                && !menu.isPatternCapacityValid()) {
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, SLOT_INVALID_OVERLAY);
            graphics.renderOutline(slot.x - 1, slot.y - 1, 18, 18, SLOT_INVALID_BORDER);
        }
        int inputSlot = menu.inputSlotForMenuSlotIndex(slot.index);
        if (inputSlot >= 0 && slot.hasItem() && !menu.isInputSlotValid(inputSlot)) {
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, SLOT_INVALID_OVERLAY);
            graphics.renderOutline(slot.x - 1, slot.y - 1, 18, 18, SLOT_INVALID_BORDER);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean colorModalOpen = colorPicker.isOpen();
        super.render(graphics, colorModalOpen ? -1 : mouseX, colorModalOpen ? -1 : mouseY, partialTick);
        if (!colorModalOpen
                && menu.queuedOutputCount() > 0
                && mouseX >= leftPos + 149 && mouseX < leftPos + 174
                && mouseY >= topPos + 80 && mouseY < topPos + 101) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.appliedpackaging.package_assembler.remaining_packages",
                            menu.queuedOutputCount()),
                    mouseX,
                    mouseY);
        }
        colorPicker.render(graphics, font, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return colorPicker.mouseClicked(mouseX, mouseY, button)
                || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return colorPicker.mouseReleased(mouseX, mouseY, button)
                || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return colorPicker.mouseDragged(mouseX, mouseY, button, dragX, dragY)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(graphics, offsetX, offsetY, mouseX, mouseY);
        if (menu.queuedOutputCount() > 0) {
            graphics.drawString(font, "+" + menu.queuedOutputCount(), 150, 82, 0x404040, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (colorPicker.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        if (isOverScrolledPanel(mouseX, mouseY)) {
            boolean changed = rowScrollbar.onMouseWheel(
                    new Point((int) mouseX - leftPos, (int) mouseY - topPos),
                    delta);
            if (changed) {
                setScrollOffset(rowScrollbar.getCurrentScroll());
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return colorPicker.keyPressed(keyCode, scanCode, modifiers)
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return colorPicker.charTyped(codePoint, modifiers)
                || super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (colorPicker.isOpen()) {
            return;
        }
        Slot capacity = firstSlot(SlotSemantics.STORAGE_CELL);
        if (hoveredSlot == capacity) {
            drawTooltip(
                    graphics,
                    mouseX,
                    mouseY,
                    List.of(
                            Component.translatable("gui.appliedpackaging.package_assembler.capacity.title"),
                            Component.translatable("gui.appliedpackaging.package_assembler.capacity.description")
                                    .withStyle(ChatFormatting.GRAY),
                            Component.translatable(
                                            "gui.appliedpackaging.package_assembler.capacity.current",
                                            menu.capacityUnitLimit(),
                                            menu.capacityTypeLimit())
                                    .withStyle(ChatFormatting.GRAY)));
            return;
        }

        Slot marker = firstSlot(SlotSemantics.BLANK_PATTERN);
        if (hoveredSlot == marker) {
            ItemStack displayedMarker = marker instanceof AppEngSlot appEngSlot
                    ? appEngSlot.getDisplayStack()
                    : ItemStack.EMPTY;
            if (displayedMarker.isEmpty()) {
                drawEmptyMarkerTooltip(graphics, mouseX, mouseY, marker);
            } else {
                graphics.renderTooltip(font, displayedMarker, mouseX, mouseY);
            }
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private Slot firstSlot(appeng.menu.SlotSemantic semantic) {
        List<Slot> slots = menu.getSlots(semantic);
        return slots.isEmpty() ? null : slots.get(0);
    }

    private void drawInputSlotBackgrounds(GuiGraphics graphics, int offsetX, int offsetY) {
        for (int index = 0; index < PackageAssemblerMenu.VISIBLE_INPUT_COUNT; index++) {
            int inputSlot = menu.inputSlotForVisibleIndex(index);
            Slot slot = menu.getSlot(menu.menuInputMenuSlotIndex(index));
            float opacity = menu.isInputSlotEnabled(inputSlot) ? 1.0F : 0.2F;
            ModernSlotRendering.drawSlotBackground(graphics, offsetX, offsetY, slot, opacity);
        }
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
        rowScrollbar.setCurrentScroll(menu.scrollOffset());
        if (menu.scrollOffset() != previous && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(
                    menu.containerId,
                    PackageAssemblerMenu.BUTTON_SCROLL_BASE + menu.scrollOffset());
        }
    }

    private Component outputModeMessage() {
        return Component.translatable(
                "gui.appliedpackaging.package_assembler.output_mode."
                        + menu.outputMode().id());
    }

    private Component blockingModeMessage() {
        return Component.translatable(
                "gui.appliedpackaging.package_assembler.blocking_mode."
                        + (menu.blockingMode() ? "enabled" : "disabled"));
    }

    private void openColorPicker() {
        if (!menu.canEditColor()) {
            return;
        }
        setFocused(null);
        colorPicker.openNear(
                colorButton,
                width,
                height,
                false,
                () -> Optional.of(menu.selectedColor()),
                selection -> selection.ifPresent(menu::setSelectedColor),
                () -> colorButton.active = menu.canEditColor());
    }

    private Component colorTooltip() {
        if (menu.effectiveColor().isEmpty()) {
            return Component.translatable(
                    menu.canEditColor()
                            ? "gui.appliedpackaging.package_assembler.color.mixed"
                            : "gui.appliedpackaging.package_assembler.color.mixed_locked");
        }
        Component color = menu.effectiveColor().orElseThrow() == com.warmthdawn.appliedpackaging.item.PackageColor.FLUIX
                ? Component.translatable("gui.appliedpackaging.package_color.fluix")
                : Component.translatable(
                        "color.minecraft."
                                + menu.effectiveColor().orElseThrow().translationKeySuffix());
        return Component.translatable(
                menu.isCrafting()
                        ? "gui.appliedpackaging.package_assembler.color.locked"
                        : menu.canEditColor()
                                ? "gui.appliedpackaging.package_assembler.color.effective"
                                : "gui.appliedpackaging.package_assembler.color.pattern_locked",
                color);
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

    private class BlockingModeToolbarButton extends IconButton {
        private BlockingModeToolbarButton() {
            super(button -> menu.toggleBlockingMode());
            setMessage(blockingModeMessage());
        }

        @Override
        protected Icon getIcon() {
            return menu.blockingMode() ? Icon.BLOCKING_MODE_YES : Icon.BLOCKING_MODE_NO;
        }
    }

}

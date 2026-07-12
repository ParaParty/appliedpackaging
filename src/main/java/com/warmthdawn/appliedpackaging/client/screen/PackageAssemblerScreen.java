package com.warmthdawn.appliedpackaging.client.screen;

import appeng.client.Point;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.ProgressBar;
import appeng.client.gui.widgets.ProgressBar.Direction;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.SlotSemantics;
import com.warmthdawn.appliedpackaging.client.widget.ModernSlotRendering;
import com.warmthdawn.appliedpackaging.world.menu.PackageAssemblerMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class PackageAssemblerScreen extends ModernUpgradeableScreen<PackageAssemblerMenu> {
    private static final int SCROLLBAR_Y = 31;
    private static final int SCROLLBAR_HEIGHT = 72;
    private static final int SCROLLBAR_PANEL_X = 10;
    private static final int SCROLLBAR_PANEL_WIDTH = 83;
    private static final int SLOT_DISABLED_OVERLAY = 0x99c7ccd5;
    private static final int SLOT_INVALID_OVERLAY = 0x55ff3333;
    private static final int SLOT_INVALID_BORDER = 0xffff5555;
    private static final Scrollbar.Style LATEST_SCROLLBAR_STYLE = Scrollbar.Style.create(
            new ResourceLocation("appliedpackaging", "textures/gui/advanced_pattern_encoding_terminal_sprites.png"),
            7, 15, 0, 32, 7, 32);

    private final OutputModeToolbarButton outputModeButton;
    private final Scrollbar rowScrollbar;
    private final ProgressBar progressBar;

    public PackageAssemblerScreen(
            PackageAssemblerMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
        rowScrollbar = widgets.addScrollBar("packageQueueScrollbar", LATEST_SCROLLBAR_STYLE);
        rowScrollbar.setRange(0, menu.maxScrollOffset(), PackageAssemblerMenu.VISIBLE_ROWS);
        rowScrollbar.setCaptureMouseWheel(false);
        progressBar = new ProgressBar(menu, style.getImage("progressBar"), Direction.VERTICAL);
        widgets.add("progressBar", progressBar);
        outputModeButton = addToLeftToolbar(new OutputModeToolbarButton());

    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        outputModeButton.setMessage(outputModeMessage());
        progressBar.visible = menu.isCrafting();
        if (rowScrollbar.getCurrentScroll() != menu.scrollOffset()) {
            setScrollOffset(rowScrollbar.getCurrentScroll());
        }
    }

    @Override
    public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        renderDisabledInputOverlays(graphics, offsetX, offsetY);
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
        if (menu.queuedOutputCount() > 0
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


    private Slot firstSlot(appeng.menu.SlotSemantic semantic) {
        List<Slot> slots = menu.getSlots(semantic);
        return slots.isEmpty() ? null : slots.get(0);
    }

    private void renderDisabledInputOverlays(GuiGraphics graphics, int offsetX, int offsetY) {
        for (int index = 0; index < PackageAssemblerMenu.VISIBLE_INPUT_COUNT; index++) {
            int inputSlot = menu.inputSlotForVisibleIndex(index);
            if (!menu.isInputSlotEnabled(inputSlot)) {
                Slot slot = menu.getSlot(menu.menuInputMenuSlotIndex(index));
                int x = offsetX + slot.x;
                int y = offsetY + slot.y;
                graphics.fill(x, y, x + 16, y + 16, SLOT_DISABLED_OVERLAY);
            }
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

}

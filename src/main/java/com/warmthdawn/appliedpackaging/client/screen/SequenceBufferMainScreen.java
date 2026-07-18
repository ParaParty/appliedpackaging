package com.warmthdawn.appliedpackaging.client.screen;

import appeng.client.Point;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import com.warmthdawn.appliedpackaging.world.menu.AbstractSequenceBufferMenu;
import com.warmthdawn.appliedpackaging.world.menu.SequenceBufferMainMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class SequenceBufferMainScreen extends AbstractSequenceBufferScreen<SequenceBufferMainMenu> {
    private static final int STORAGE_X = 7;
    private static final int STORAGE_Y = 18;
    private static final int STORAGE_WIDTH = SequenceBufferMainMenu.COLUMNS * 18;
    private static final int STORAGE_HEIGHT = SequenceBufferMainMenu.VISIBLE_ROWS * 18;
    private final Scrollbar rowScrollbar;

    public SequenceBufferMainScreen(
            SequenceBufferMainMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
        rowScrollbar = widgets.addScrollBar("sequenceBufferScrollbar", Scrollbar.DEFAULT);
        rowScrollbar.setRange(0, menu.maxScrollOffset(), SequenceBufferMainMenu.VISIBLE_ROWS);
        rowScrollbar.setCaptureMouseWheel(false);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        rowScrollbar.setRange(0, menu.maxScrollOffset(), SequenceBufferMainMenu.VISIBLE_ROWS);
        if (rowScrollbar.getCurrentScroll() != menu.scrollOffset()) {
            setScrollOffset(rowScrollbar.getCurrentScroll());
        }
    }

    @Override
    protected void drawStorageSlotBackgrounds(GuiGraphics graphics, int offsetX, int offsetY) {
        int visibleIndex = 0;
        for (Slot slot : menu.getSlots(AbstractSequenceBufferMenu.BUFFER_CONTENTS)) {
            float alpha = menu.isStorageSlotEnabled(visibleIndex++) ? 1.0F : 0.2F;
            SLOT_BACKGROUND.copy()
                    .dest(offsetX + slot.x - 1, offsetY + slot.y - 1)
                    .color(1, 1, 1, alpha)
                    .blit(graphics);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= leftPos + STORAGE_X
                && mouseX < leftPos + STORAGE_X + STORAGE_WIDTH
                && mouseY >= topPos + STORAGE_Y
                && mouseY < topPos + STORAGE_Y + STORAGE_HEIGHT) {
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

    private void setScrollOffset(int offset) {
        int previous = menu.scrollOffset();
        menu.setScrollOffset(offset);
        rowScrollbar.setCurrentScroll(menu.scrollOffset());
        if (menu.scrollOffset() != previous && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(
                    menu.containerId,
                    SequenceBufferMainMenu.BUTTON_SCROLL_BASE + menu.scrollOffset());
        }
    }
}

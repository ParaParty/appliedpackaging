/*
 * Adapted from Applied Energistics 2's UpgradesPanel at commit
 * 45f315517ea346efc0babd02c85c6b9d32dc8acf (LGPL-3.0-or-later).
 */
package com.warmthdawn.appliedpackaging.client.widget;

import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Rects;
import appeng.client.gui.Tooltip;
import appeng.client.gui.style.Blitter;
import appeng.menu.slot.AppEngSlot;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.mixin.client.SlotAccessor;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

/** Backport of the current AE2 upgrade-panel layout for Applied Packaging screens. */
public final class ModernUpgradesPanel implements ICompositeWidget {
    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 5;
    private static final int MAX_ROWS = 8;

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            AppliedPackaging.MOD_ID, "textures/gui/package_bus_extra_panels.png");
    private static final Blitter BACKGROUND = Blitter.texture(TEXTURE, 128, 128);
    private static final Blitter INNER_CORNER = BACKGROUND.copy().src(12, 33, SLOT_SIZE, SLOT_SIZE);

    private final List<Slot> slots;
    private final Supplier<List<Component>> tooltipSupplier;
    private Point screenOrigin = Point.ZERO;
    private int x;
    private int y;

    public ModernUpgradesPanel(
            List<Slot> slots,
            Supplier<List<Component>> tooltipSupplier) {
        this.slots = slots;
        this.tooltipSupplier = tooltipSupplier;
    }

    @Override
    public void setPosition(Point position) {
        x = position.getX();
        y = position.getY();
    }

    @Override
    public void setSize(int width, int height) {
        // The panel derives its dimensions from the active upgrade slots.
    }

    @Override
    public Rect2i getBounds() {
        int slotCount = getUpgradeSlotCount();
        int height = 2 * PADDING + Math.min(MAX_ROWS, slotCount) * SLOT_SIZE;
        int width = 2 * PADDING + (slotCount + MAX_ROWS - 1) / MAX_ROWS * SLOT_SIZE;
        return new Rect2i(x, y, width, height);
    }

    @Override
    public void populateScreen(Consumer<AbstractWidget> addWidget, Rect2i bounds, AEBaseScreen<?> screen) {
        screenOrigin = Point.fromTopLeft(bounds);
    }

    @Override
    public void updateBeforeRender() {
        int slotOriginX = x;
        int slotOriginY = y + PADDING;
        for (Slot slot : slots) {
            if (!slot.isActive()) {
                continue;
            }
            SlotAccessor accessor = (SlotAccessor) slot;
            accessor.appliedpackaging$setX(slotOriginX + 1);
            accessor.appliedpackaging$setY(slotOriginY + 1);
            slotOriginY += SLOT_SIZE;
        }
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics graphics, Rect2i bounds, Point mouse) {
        int slotCount = getUpgradeSlotCount();
        if (slotCount <= 0) {
            return;
        }

        int slotOriginX = screenOrigin.getX() + x + PADDING;
        int slotOriginY = screenOrigin.getY() + y + PADDING;
        for (int i = 0; i < slotCount; i++) {
            int row = i % MAX_ROWS;
            int column = i / MAX_ROWS;
            int slotX = slotOriginX + column * SLOT_SIZE;
            int slotY = slotOriginY + row * SLOT_SIZE;
            boolean lastSlot = i + 1 >= slotCount;
            boolean lastRow = row + 1 >= MAX_ROWS;

            drawSlot(
                    graphics,
                    slotX,
                    slotY,
                    column == 0,
                    row == 0,
                    i >= slotCount - MAX_ROWS,
                    lastRow || lastSlot);

            if (column > 0 && lastSlot && !lastRow) {
                INNER_CORNER.dest(slotX, slotY + SLOT_SIZE).blit(graphics);
            }
        }

        graphics.hLine(slotOriginX - 4, slotOriginX + 11, slotOriginY, 0xfff2f2f2);
        graphics.hLine(
                slotOriginX - 4,
                slotOriginX + 11,
                slotOriginY + SLOT_SIZE * slotCount - 1,
                0xfff2f2f2);
        graphics.vLine(
                slotOriginX - 5,
                slotOriginY - 1,
                slotOriginY + SLOT_SIZE * slotCount,
                0xfff2f2f2);
        graphics.vLine(
                slotOriginX + 12,
                slotOriginY - 1,
                slotOriginY + SLOT_SIZE * slotCount,
                0xfff2f2f2);
        graphics.flush();
    }

    @Override
    public void addExclusionZones(List<Rect2i> exclusionZones, Rect2i screenBounds) {
        int slotCount = getUpgradeSlotCount();
        int fullColumns = slotCount / MAX_ROWS;
        int rightEdge = screenBounds.getX() + x;
        if (fullColumns > 0) {
            int fullColumnWidth = 2 * PADDING + fullColumns * SLOT_SIZE;
            exclusionZones.add(Rects.expand(new Rect2i(
                    rightEdge,
                    screenBounds.getY() + y,
                    fullColumnWidth,
                    2 * PADDING + MAX_ROWS * SLOT_SIZE), 2));
            rightEdge += fullColumnWidth;
        }

        int remaining = slotCount - fullColumns * MAX_ROWS;
        if (remaining > 0) {
            exclusionZones.add(Rects.expand(new Rect2i(
                    rightEdge,
                    screenBounds.getY() + y,
                    SLOT_SIZE + (fullColumns > 0 ? 0 : 2 * PADDING),
                    2 * PADDING + remaining * SLOT_SIZE), 2));
        }
    }

    @Override
    public Tooltip getTooltip(int mouseX, int mouseY) {
        if (getUpgradeSlotCount() == 0) {
            return null;
        }
        List<Component> tooltip = tooltipSupplier.get();
        return tooltip.isEmpty() ? null : new Tooltip(tooltip);
    }

    private static void drawSlot(
            GuiGraphics graphics,
            int x,
            int y,
            boolean borderLeft,
            boolean borderTop,
            boolean borderRight,
            boolean borderBottom) {
        int sourceX = PADDING;
        int sourceY = PADDING;
        int sourceWidth = SLOT_SIZE;
        int sourceHeight = SLOT_SIZE;
        if (borderLeft) {
            x -= PADDING;
            sourceX = 0;
            sourceWidth += PADDING;
        }
        if (borderRight) {
            sourceWidth += PADDING;
        }
        if (borderTop) {
            y -= PADDING;
            sourceY = 0;
            sourceHeight += PADDING;
        }
        if (borderBottom) {
            sourceHeight += PADDING + 2;
        }
        BACKGROUND.src(sourceX, sourceY, sourceWidth, sourceHeight)
                .dest(x, y)
                .blit(graphics);
    }

    private int getUpgradeSlotCount() {
        int count = 0;
        for (Slot slot : slots) {
            if (slot instanceof AppEngSlot appEngSlot && appEngSlot.isSlotEnabled()) {
                count++;
            }
        }
        return count;
    }
}

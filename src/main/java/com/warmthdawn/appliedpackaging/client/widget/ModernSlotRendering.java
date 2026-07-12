/*
 * Current slot-highlight and upgrade-placeholder presentation is adapted from
 * Applied Energistics 2 commit 45f315517ea346efc0babd02c85c6b9d32dc8acf
 * (LGPL-3.0-or-later). The marker icon itself is an Applied Packaging asset.
 */
package com.warmthdawn.appliedpackaging.client.widget;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.Blitter;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.ResizableSlot;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

/** Shared current-AE2-style slot presentation for Applied Packaging screens. */
public final class ModernSlotRendering {
    private static final ResourceLocation PACKAGE_SPRITES = new ResourceLocation(
            AppliedPackaging.MOD_ID, "textures/gui/package-storagebus-sprites.png");
    private static final ResourceLocation CURRENT_AE2_STATES = new ResourceLocation(
            AppliedPackaging.MOD_ID, "textures/gui/ae2-states.png");

    // User-drawn marker empty-slot icon in the supplied sprite sheet.
    private static final Blitter MARKER_SLOT_ICON =
            Blitter.texture(PACKAGE_SPRITES).src(32, 16, 16, 16);
    private static final Blitter STORAGE_COMPONENT_SLOT_ICON =
            Blitter.texture(CURRENT_AE2_STATES).src(240, 48, 16, 16);
    private static final Blitter ENCODED_PATTERN_SLOT_ICON =
            Blitter.texture(CURRENT_AE2_STATES).src(240, 112, 16, 16);
    private static final Blitter UPGRADE_SLOT_ICON =
            Blitter.texture(CURRENT_AE2_STATES).src(240, 208, 16, 16);

    private static final List<Component> MARKER_SLOT_TOOLTIP = List.of(
            Component.translatable("gui.appliedpackaging.marker_slot"),
            Component.translatable("gui.appliedpackaging.marker_slot.description")
                    .withStyle(ChatFormatting.GRAY));

    private ModernSlotRendering() {
    }

    public static void clearLegacyUpgradeIcons(List<Slot> slots) {
        for (Slot slot : slots) {
            if (slot instanceof AppEngSlot appEngSlot) {
                appEngSlot.setIcon(null);
            }
        }
    }

    /** Draws the current-main upgrade placeholder in slot-local coordinates. */
    public static void drawUpgradeSlotIcon(GuiGraphics graphics, Slot slot) {
        if (slot instanceof AppEngSlot appEngSlot
                && appEngSlot.isSlotEnabled()
                && slot.getItem().isEmpty()) {
            UPGRADE_SLOT_ICON.copy()
                    .dest(slot.x, slot.y)
                    .opacity(appEngSlot.getOpacityOfIcon())
                    .blit(graphics);
        }
    }

    public static void drawStorageComponentSlotIcon(
            GuiGraphics graphics,
            int offsetX,
            int offsetY,
            Slot slot) {
        drawEmptySlotIcon(graphics, offsetX, offsetY, slot, STORAGE_COMPONENT_SLOT_ICON);
    }

    public static void drawEncodedPatternSlotIcon(
            GuiGraphics graphics,
            int offsetX,
            int offsetY,
            Slot slot) {
        drawEmptySlotIcon(graphics, offsetX, offsetY, slot, ENCODED_PATTERN_SLOT_ICON);
    }

    /** Draws the user-provided marker placeholder in absolute screen coordinates. */
    public static void drawMarkerSlotIcon(
            GuiGraphics graphics,
            int offsetX,
            int offsetY,
            Slot slot,
            float opacity) {
        if (slot != null && slot.getItem().isEmpty()) {
            MARKER_SLOT_ICON.copy()
                    .dest(offsetX + slot.x, offsetY + slot.y)
                    .opacity(opacity)
                    .blit(graphics);
        }
    }

    private static void drawEmptySlotIcon(
            GuiGraphics graphics,
            int offsetX,
            int offsetY,
            Slot slot,
            Blitter icon) {
        if (slot != null && slot.getItem().isEmpty()) {
            icon.copy().dest(offsetX + slot.x, offsetY + slot.y).blit(graphics);
        }
    }

    /** Draws the current-main hover treatment during the window-coordinate tooltip pass. */
    public static void drawSlotHighlight(
            GuiGraphics graphics,
            int guiLeft,
            int guiTop,
            Slot slot) {
        if (slot == null || !slot.isActive()) {
            return;
        }

        drawSlotHighlightAt(graphics, guiLeft + slot.x, guiTop + slot.y, slot);
    }

    /** Draws the current-main hover treatment at an already resolved position. */
    public static void drawSlotHighlightAt(
            GuiGraphics graphics,
            int x,
            int y,
            Slot slot) {
        if (slot == null || !slot.isActive()) {
            return;
        }

        int width = 16;
        int height = 16;
        if (slot instanceof ResizableSlot resizableSlot) {
            width = resizableSlot.getWidth();
            height = resizableSlot.getHeight();
        }

        graphics.flush();
        graphics.fill(x, y, x + width, y + height, 0x669cd3ff);
        graphics.hLine(x, x + width, y - 1, 0xffdaffff);
        graphics.hLine(x - 1, x + width, y + height, 0xffdaffff);
        graphics.vLine(x - 1, y - 2, y + height, 0xffdaffff);
        graphics.vLine(x + width, y - 2, y + height, 0xffdaffff);
        graphics.flush();
    }

    public static void drawEmptyMarkerTooltip(
            AEBaseScreen<?> screen,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            Slot markerSlot) {
        if (markerSlot != null
                && markerSlot.isActive()
                && markerSlot.getItem().isEmpty()
                && screen.getMenu().getCarried().isEmpty()) {
            screen.drawTooltip(graphics, mouseX, mouseY, MARKER_SLOT_TOOLTIP);
        }
    }
}

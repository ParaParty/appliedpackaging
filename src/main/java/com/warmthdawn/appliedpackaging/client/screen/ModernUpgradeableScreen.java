/*
 * Adapted from Applied Energistics 2's UpgradeableScreen and current-main
 * slot/upgrade presentation at commit
 * 45f315517ea346efc0babd02c85c6b9d32dc8acf (LGPL-3.0-or-later).
 */
package com.warmthdawn.appliedpackaging.client.screen;

import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ToolboxPanel;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.UpgradeableMenu;
import com.warmthdawn.appliedpackaging.client.widget.ModernSlotRendering;
import com.warmthdawn.appliedpackaging.client.widget.ModernUpgradesPanel;
import com.warmthdawn.appliedpackaging.client.widget.ModernVerticalToolbar;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Backports current AE2 slot and upgrade visuals without changing the pinned menu API. */
public abstract class ModernUpgradeableScreen<T extends UpgradeableMenu<?>> extends AEBaseScreen<T> {
    private final ModernVerticalToolbar modernToolbar = new ModernVerticalToolbar();

    protected ModernUpgradeableScreen(
            T menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        widgets.add(
                "upgrades",
                new ModernUpgradesPanel(
                        menu.getSlots(SlotSemantics.UPGRADE),
                        this::getCompatibleUpgrades));
        if (menu.getToolbox().isPresent()) {
            widgets.add("toolbox", new ToolboxPanel(style, menu.getToolbox().getName()));
        }
        ModernSlotRendering.clearLegacyUpgradeIcons(menu.getSlots(SlotSemantics.UPGRADE));
    }

    @Override
    protected void init() {
        super.init();
        modernToolbar.captureIconButtons(children());
        for (Renderable renderer : modernToolbar.createIconButtonRenderers()) {
            addRenderableOnly(renderer);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled && modernToolbar.isToolbarButton(getFocused())) {
            setFocused(null);
        }
        return handled;
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
    }

    @Override
    public void renderCustomSlotHighlight(GuiGraphics graphics, int x, int y, int z) {
        // Suppress AE2 15's white overlay. The current treatment is rendered
        // in window coordinates immediately before the tooltip pass.
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ModernSlotRendering.drawSlotHighlight(graphics, leftPos, topPos, hoveredSlot);
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void renderSlot(GuiGraphics graphics, Slot slot) {
        if (menu.getSlotSemantic(slot) == SlotSemantics.UPGRADE) {
            ModernSlotRendering.drawUpgradeSlotIcon(graphics, slot);
        }
        super.renderSlot(graphics, slot);
    }

    protected void drawEmptyMarkerTooltip(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            Slot markerSlot) {
        ModernSlotRendering.drawEmptyMarkerTooltip(this, graphics, mouseX, mouseY, markerSlot);
    }

    private List<Component> getCompatibleUpgrades() {
        List<Component> lines = new ArrayList<>();
        lines.add(GuiText.CompatibleUpgrades.text());
        lines.addAll(Upgrades.getTooltipLinesForMachine(menu.getUpgrades().getUpgradableItem()));
        return lines;
    }
}

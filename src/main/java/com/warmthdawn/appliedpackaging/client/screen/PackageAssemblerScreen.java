package com.warmthdawn.appliedpackaging.client.screen;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.world.menu.PackageAssemblerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PackageAssemblerScreen extends AbstractContainerScreen<PackageAssemblerMenu> {
    private static final ResourceLocation AUTO_EXPORT_ICON =
            new ResourceLocation(AppliedPackaging.MOD_ID, "textures/gui/icons/auto_export.png");
    private static final int PANEL = 0xffd6dbde;
    private static final int PANEL_DARK = 0xff4a5058;
    private static final int PANEL_MID = 0xff879198;
    private static final int SLOT = 0xffb7c0c5;

    public PackageAssemblerScreen(PackageAssemblerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 188;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 96;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new AutoExportButton(leftPos + 144, topPos + 52));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.renderOutline(x, y, imageWidth, imageHeight, PANEL_DARK);
        graphics.fill(x + 6, y + 18, x + 170, y + 88, 0xffc9d0d4);
        graphics.renderOutline(x + 6, y + 18, 164, 70, PANEL_MID);
        renderMachineSlots(graphics, x, y);
        graphics.hLine(x + 86, x + 108, y + 34, PANEL_DARK);
        graphics.hLine(x + 134, x + 142, y + 34, PANEL_DARK);
        graphics.hLine(x + 104, x + 126, y + 62, PANEL_DARK);
        renderInventorySlots(graphics, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xff2a3036, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xff2a3036, false);
    }

    private static void renderMachineSlots(GuiGraphics graphics, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                renderSlot(graphics, left + 25 + column * 18, top + 17 + row * 18);
            }
        }
        renderSlot(graphics, left + 115, top + 23);
        renderSlot(graphics, left + 143, top + 23);
        renderSlot(graphics, left + 115, top + 51);
    }

    private static void renderSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 20, y + 20, PANEL_DARK);
        graphics.fill(x + 1, y + 1, x + 19, y + 19, SLOT);
        graphics.fill(x + 2, y + 2, x + 18, y + 18, 0xffe8ecee);
    }

    private static void renderInventorySlots(GuiGraphics graphics, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlot(graphics, left + 7 + column * 18, top + 106 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlot(graphics, left + 7 + column * 18, top + 164);
        }
    }

    private class AutoExportButton extends AbstractButton {
        private AutoExportButton(int x, int y) {
            super(x, y, 16, 16, Component.empty());
            setMessage(autoExportMessage());
            setTooltip(Tooltip.create(autoExportMessage()));
        }

        @Override
        public void onPress() {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PackageAssemblerMenu.BUTTON_AUTO_EXPORT);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Component message = autoExportMessage();
            setMessage(message);
            setTooltip(Tooltip.create(message));
            int border = isHoveredOrFocused() ? 0xffffffff : (menu.autoExport() ? 0xff5aa86a : 0xff2a3036);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
            graphics.blit(AUTO_EXPORT_ICON, getX(), getY(), 0, 0, width, height, width, height);
            if (!menu.autoExport()) {
                for (int offset = 0; offset < 11; offset++) {
                    graphics.fill(getX() + 3 + offset, getY() + 13 - offset, getX() + 5 + offset, getY() + 15 - offset, 0xffffffff);
                }
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        private Component autoExportMessage() {
            return Component.translatable(
                    "gui.appliedpackaging.package_assembler.auto_export."
                            + (menu.autoExport() ? "enabled" : "disabled"));
        }
    }
}

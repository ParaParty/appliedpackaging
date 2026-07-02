package com.warmthdawn.appliedpackaging.client.screen;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.world.menu.PackageBusMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PackageBusScreen extends AbstractContainerScreen<PackageBusMenu> {
    private static final ResourceLocation SET_FILTER_ICON =
            new ResourceLocation(AppliedPackaging.MOD_ID, "textures/gui/icons/package_filter.png");
    private static final ResourceLocation CLEAR_FILTER_ICON =
            new ResourceLocation(AppliedPackaging.MOD_ID, "textures/gui/icons/marker_clear.png");
    private static final int PANEL = 0xffd6dbde;
    private static final int PANEL_DARK = 0xff4a5058;
    private static final int PANEL_MID = 0xff879198;
    private static final int SLOT = 0xffb7c0c5;

    public PackageBusScreen(PackageBusMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 206;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 114;
    }

    @Override
    protected void init() {
        super.init();
        ImageButton setButton = new ImageButton(
                leftPos + 54,
                topPos + 26,
                16,
                16,
                0,
                0,
                0,
                SET_FILTER_ICON,
                16,
                16,
                button -> minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId,
                        PackageBusMenu.BUTTON_SET_FROM_CURSOR),
                Component.translatable("gui.appliedpackaging.package_bus.set_filter"));
        setButton.setTooltip(Tooltip.create(Component.translatable("gui.appliedpackaging.package_bus.set_filter")));
        addRenderableWidget(setButton);

        ImageButton clearButton = new ImageButton(
                leftPos + 106,
                topPos + 26,
                16,
                16,
                0,
                0,
                0,
                CLEAR_FILTER_ICON,
                16,
                16,
                button -> minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId,
                        PackageBusMenu.BUTTON_CLEAR_FILTER),
                Component.translatable("gui.appliedpackaging.package_bus.clear_filter"));
        clearButton.setTooltip(Tooltip.create(Component.translatable("gui.appliedpackaging.package_bus.clear_filter")));
        addRenderableWidget(clearButton);

        for (int index = 0; index < PackageColor.values().length; index++) {
            PackageColor color = PackageColor.values()[index];
            addRenderableWidget(new ColorSwatchButton(
                    leftPos + 10 + index * 9,
                    topPos + 100,
                    color));
        }
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
        graphics.fill(x + 6, y + 18, x + 170, y + 110, 0xffc9d0d4);
        graphics.renderOutline(x + 6, y + 18, 164, 92, PANEL_MID);
        renderSlot(graphics, x + 79, y + 24);
        renderSlot(graphics, x + 34, y + 57);
        for (int slot = 0; slot < 3; slot++) {
            renderSlot(graphics, x + 71 + slot * 18, y + 57);
        }
        graphics.fill(x + 8, y + 99, x + 164, y + 110, 0xffaeb7bd);
        graphics.renderOutline(x + 8, y + 99, 156, 11, PANEL_MID);
        graphics.hLine(x + 70, x + 78, y + 34, PANEL_DARK);
        graphics.hLine(x + 100, x + 106, y + 34, PANEL_DARK);
        graphics.hLine(x + 55, x + 70, y + 67, PANEL_DARK);
        renderInventorySlots(graphics, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xff2a3036, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xff2a3036, false);
    }

    private static void renderSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 20, y + 20, PANEL_DARK);
        graphics.fill(x + 1, y + 1, x + 19, y + 19, SLOT);
        graphics.fill(x + 2, y + 2, x + 18, y + 18, 0xffe8ecee);
    }

    private static void renderInventorySlots(GuiGraphics graphics, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlot(graphics, left + 7 + column * 18, top + 124 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlot(graphics, left + 7 + column * 18, top + 182);
        }
    }

    private class ColorSwatchButton extends AbstractButton {
        private final PackageColor color;

        private ColorSwatchButton(int x, int y, PackageColor color) {
            super(x, y, 8, 8, Component.translatable("item.appliedpackaging." + color.id() + "_package"));
            this.color = color;
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            minecraft.gameMode.handleInventoryButtonClick(
                    menu.containerId,
                    PackageBusMenu.BUTTON_COLOR_BASE + color.ordinal());
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean selected = menu.selectedColor() == color;
            int border = selected ? 0xffffffff : (isHoveredOrFocused() ? 0xffd6dbde : 0xff2a3036);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, color.swatchArgb());
            if (selected) {
                graphics.renderOutline(getX() - 1, getY() - 1, width + 2, height + 2, 0xff2a3036);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}

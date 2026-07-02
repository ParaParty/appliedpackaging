package com.warmthdawn.appliedpackaging.client.screen;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.world.menu.PackagePatternTerminalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PackagePatternTerminalScreen extends AbstractContainerScreen<PackagePatternTerminalMenu> {
    private static final ResourceLocation ENCODE_ICON =
            new ResourceLocation(AppliedPackaging.MOD_ID, "textures/gui/icons/pack_once.png");
    private static final int PANEL = 0xffd6dbde;
    private static final int PANEL_DARK = 0xff4a5058;
    private static final int PANEL_MID = 0xff879198;
    private static final int SLOT = 0xffb7c0c5;

    public PackagePatternTerminalScreen(PackagePatternTerminalMenu menu, Inventory playerInventory, Component title) {
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
        ImageButton encodeButton = new ImageButton(
                leftPos + 92,
                topPos + 52,
                16,
                16,
                0,
                0,
                0,
                ENCODE_ICON,
                16,
                16,
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PackagePatternTerminalMenu.BUTTON_ENCODE),
                Component.translatable("gui.appliedpackaging.package_pattern_terminal.encode"));
        encodeButton.setTooltip(Tooltip.create(Component.translatable("gui.appliedpackaging.package_pattern_terminal.encode")));
        addRenderableWidget(encodeButton);

        for (int index = 0; index < PackageColor.values().length; index++) {
            PackageColor color = PackageColor.values()[index];
            addRenderableWidget(new ColorSwatchButton(
                    leftPos + 10 + index * 9,
                    topPos + 82,
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
        graphics.fill(x + 6, y + 18, x + 170, y + 88, 0xffc9d0d4);
        graphics.renderOutline(x + 6, y + 18, 164, 70, PANEL_MID);
        renderMachineSlots(graphics, x, y);
        renderInventorySlots(graphics, x, y);
        graphics.fill(x + 8, y + 81, x + 164, y + 92, 0xffaeb7bd);
        graphics.renderOutline(x + 8, y + 81, 156, 11, PANEL_MID);
        graphics.hLine(x + 86, x + 108, y + 34, PANEL_DARK);
        graphics.hLine(x + 134, x + 142, y + 34, PANEL_DARK);
        graphics.hLine(x + 82, x + 114, y + 60, PANEL_DARK);
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
        renderSlot(graphics, left + 143, top + 51);
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
                    PackagePatternTerminalMenu.BUTTON_COLOR_BASE + color.ordinal());
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

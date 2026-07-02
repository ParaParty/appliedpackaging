package com.warmthdawn.appliedpackaging.client.screen;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.world.menu.MePackagerMenu;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MePackagerScreen extends AbstractContainerScreen<MePackagerMenu> {
    private static final ResourceLocation PACK_ONCE_ICON =
            new ResourceLocation(AppliedPackaging.MOD_ID, "textures/gui/icons/pack_once.png");
    private static final ResourceLocation MARKER_RETAIN_ICON =
            new ResourceLocation(AppliedPackaging.MOD_ID, "textures/gui/icons/marker_retain.png");
    private static final ResourceLocation MARKER_OVERRIDE_ICON =
            new ResourceLocation(AppliedPackaging.MOD_ID, "textures/gui/icons/marker_override.png");
    private static final ResourceLocation MARKER_CLEAR_ICON =
            new ResourceLocation(AppliedPackaging.MOD_ID, "textures/gui/icons/marker_clear.png");
    private static final int PANEL = 0xffd6dbde;
    private static final int PANEL_DARK = 0xff4a5058;
    private static final int PANEL_MID = 0xff879198;
    private static final int SLOT = 0xffb7c0c5;

    public MePackagerScreen(MePackagerMenu menu, Inventory playerInventory, Component title) {
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
        ImageButton packButton = new ImageButton(
                leftPos + 80,
                topPos + 35,
                16,
                16,
                0,
                0,
                0,
                PACK_ONCE_ICON,
                16,
                16,
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MePackagerMenu.BUTTON_PACK_ONCE),
                Component.translatable("gui.appliedpackaging.me_packager.pack_once"));
        packButton.setTooltip(Tooltip.create(Component.translatable("gui.appliedpackaging.me_packager.pack_once")));
        addRenderableWidget(packButton);
        addRenderableWidget(new MarkerModeButton(leftPos + 104, topPos + 35));

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
        renderSlot(graphics, x + 34, y + 33);
        renderSlot(graphics, x + 122, y + 33);
        renderSlot(graphics, x + 34, y + 59);
        renderSlot(graphics, x + 60, y + 59);
        renderSlot(graphics, x + 86, y + 59);
        graphics.hLine(x + 54, x + 118, y + 42, PANEL_DARK);
        graphics.fill(x + 88, y + 40, x + 91, y + 45, PANEL_DARK);
        graphics.fill(x + 91, y + 39, x + 94, y + 46, menu.selectedColor().swatchArgb());
        graphics.fill(x + 8, y + 81, x + 164, y + 92, 0xffaeb7bd);
        graphics.renderOutline(x + 8, y + 81, 156, 11, PANEL_MID);
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
                    MePackagerMenu.BUTTON_COLOR_BASE + color.ordinal());
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

    private class MarkerModeButton extends AbstractButton {
        private MarkerModeButton(int x, int y) {
            super(x, y, 16, 16, Component.empty());
            setMessage(markerModeMessage());
            setTooltip(Tooltip.create(markerModeMessage()));
        }

        @Override
        public void onPress() {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MePackagerMenu.BUTTON_MARKER_MODE);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Component message = markerModeMessage();
            setMessage(message);
            setTooltip(Tooltip.create(message));
            int border = isHoveredOrFocused() ? 0xffffffff : 0xff2a3036;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
            graphics.blit(markerModeIcon(), getX(), getY(), 0, 0, width, height, width, height);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        private Component markerModeMessage() {
            return Component.translatable(
                    "gui.appliedpackaging.me_packager.marker_mode."
                            + menu.markerMode().name().toLowerCase(Locale.ROOT));
        }

        private ResourceLocation markerModeIcon() {
            return switch (menu.markerMode()) {
                case RETAIN -> MARKER_RETAIN_ICON;
                case OVERRIDE -> MARKER_OVERRIDE_ICON;
                case CLEAR -> MARKER_CLEAR_ICON;
            };
        }
    }
}

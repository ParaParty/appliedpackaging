package com.warmthdawn.appliedpackaging.client.screen;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.world.menu.MePackagerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MePackagerScreen extends AbstractContainerScreen<MePackagerMenu> {
    private static final ResourceLocation PACK_ONCE_ICON =
            new ResourceLocation(AppliedPackaging.MOD_ID, "textures/gui/icons/pack_once.png");
    private static final int PANEL = 0xffd6dbde;
    private static final int PANEL_DARK = 0xff4a5058;
    private static final int PANEL_MID = 0xff879198;
    private static final int SLOT = 0xffb7c0c5;

    public MePackagerScreen(MePackagerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 73;
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
        graphics.fill(x + 6, y + 18, x + 170, y + 66, 0xffc9d0d4);
        graphics.renderOutline(x + 6, y + 18, 164, 48, PANEL_MID);
        renderSlot(graphics, x + 52, y + 33);
        renderSlot(graphics, x + 115, y + 33);
        graphics.hLine(x + 72, x + 104, y + 42, PANEL_DARK);
        graphics.fill(x + 88, y + 40, x + 91, y + 45, PANEL_DARK);
        graphics.fill(x + 91, y + 39, x + 94, y + 46, 0xff8a6dff);
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
                renderSlot(graphics, left + 7 + column * 18, top + 83 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlot(graphics, left + 7 + column * 18, top + 141);
        }
    }
}

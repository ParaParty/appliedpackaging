package com.warmthdawn.appliedpackaging.client.widget;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.ButtonToolTips;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class PackageModeWidgets {
    private static final ResourceLocation SPRITES = new ResourceLocation(
            "appliedpackaging",
            "textures/gui/advanced_pattern_encoding_terminal_sprites.png");
    private static final Blitter CLEAR_BUTTON = Blitter.texture(SPRITES).src(0, 0, 8, 8);
    private static final Blitter PACKAGE_TAB_ICON = Blitter.texture(SPRITES).src(32, 0, 16, 16);

    private PackageModeWidgets() {
    }

    public static final class ModeTabButton extends TabButton {
        public ModeTabButton(Component message, Button.OnPress onPress) {
            super(ItemStack.EMPTY, message, onPress);
            setStyle(Style.HORIZONTAL);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Icon backdrop = isFocused()
                    ? Icon.HORIZONTAL_TAB_FOCUS
                    : isSelected() ? Icon.HORIZONTAL_TAB_SELECTED : Icon.HORIZONTAL_TAB;
            backdrop.getBlitter().dest(getX(), getY()).blit(graphics);
            PACKAGE_TAB_ICON.dest(getX() + 1, getY() + 3).blit(graphics);
        }
    }

    public static final class ClearButton extends AbstractButton {
        private final Runnable onPress;

        public ClearButton(Runnable onPress) {
            super(0, 0, 8, 8, ButtonToolTips.Clear.text());
            this.onPress = onPress;
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            CLEAR_BUTTON.dest(getX(), getY()).blit(graphics);
            if (isHoveredOrFocused()) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x30ffffff);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

}

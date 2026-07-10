package com.warmthdawn.appliedpackaging.client.widget;

import com.warmthdawn.appliedpackaging.item.PackageColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public final class PackageColorButton extends Button {
    private final PackageColor color;
    private boolean selected;

    public PackageColorButton(PackageColor color, OnPress onPress) {
        super(0, 0, 8, 8, Component.translatable("color.minecraft." + color.translationKeySuffix()), onPress,
                Button.DEFAULT_NARRATION);
        this.color = color;
        setTooltip(Tooltip.create(getMessage()));
    }

    public PackageColor color() {
        return color;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int border = selected ? 0xffffffff : 0xff4a4a54;
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, border);
        guiGraphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, color.swatchArgb());
    }
}

package com.warmthdawn.appliedpackaging.mixinbridge;

import net.minecraft.client.gui.GuiGraphics;

public interface PackageCraftingPatternScreenBridge {
    void appliedpackaging$openPackageSettings();

    boolean appliedpackaging$handlePackageSettingsMouseClicked(double mouseX, double mouseY, int button);

    boolean appliedpackaging$handlePackageSettingsMouseReleased(double mouseX, double mouseY, int button);

    boolean appliedpackaging$handlePackageSettingsMouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY);

    boolean appliedpackaging$handlePackageSettingsMouseScrolled(double mouseX, double mouseY, double delta);

    boolean appliedpackaging$handlePackageSettingsKeyPressed(int keyCode, int scanCode, int modifiers);

    boolean appliedpackaging$handlePackageSettingsCharTyped(char codePoint, int modifiers);

    boolean appliedpackaging$isPackageSettingsOpen();

    void appliedpackaging$renderPackageSettingsOverlay(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick);
}

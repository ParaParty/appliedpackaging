package com.warmthdawn.appliedpackaging.mixin.client;

import appeng.client.gui.me.items.EncodingModePanel;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.parts.encoding.EncodingMode;
import com.warmthdawn.appliedpackaging.client.widget.PackageColorPicker;
import com.warmthdawn.appliedpackaging.client.widget.PackageModeWidgets;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPanelBridge;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternMenuBridge;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternScreenBridge;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageProcessingPanelBridge;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PatternEncodingTermScreen.class, remap = false)
public abstract class PatternEncodingTermScreenMixin<C extends PatternEncodingTermMenu> extends Screen
        implements PackageCraftingPatternScreenBridge {
    @Unique
    private static final int AP_PACKAGE_PANEL_X = 8;
    @Unique
    private static final int AP_PACKAGE_PANEL_BOTTOM = 165;
    @Unique
    private static final int AP_PACKAGE_CLEAR_REL_X = 72;
    @Unique
    private static final int AP_PACKAGE_COLOR_REL_X = 82;
    @Unique
    private static final int AP_PACKAGE_CONTROL_REL_Y = 7;
    @Unique
    private static final int AP_PACKAGE_GRID_REL_X = 16;
    @Unique
    private static final int AP_PACKAGE_GRID_REL_Y = 7;
    @Unique
    private static final int AP_PACKAGE_RESULT_REL_X = 98;
    @Unique
    private static final int AP_PACKAGE_RESULT_REL_Y = 31;
    @Unique
    private static final int AP_PACKAGE_MARKER_REL_X = 95;
    @Unique
    private static final int AP_PACKAGE_MARKER_REL_Y = 7;
    @Unique
    private static final int AP_V15_GRID_X = 18;
    @Unique
    private static final int AP_V15_GRID_BOTTOM = 156;
    @Unique
    private static final int AP_V15_RESULT_X = 110;
    @Unique
    private static final int AP_V15_RESULT_BOTTOM = 138;
    @Unique
    private static final int AP_SLOT_STEP = 18;
    @Unique
    private static final int AP_HIDDEN_SLOT_POSITION = -10_000;
    @Shadow
    @Final
    private Map<EncodingMode, EncodingModePanel> modePanels;
    @Shadow
    @Final
    private Map<EncodingMode, TabButton> modeTabButtons;

    @Unique
    private PackageModeWidgets.ModeTabButton appliedpackaging$packageTabButton;
    @Unique
    private PackageModeWidgets.ClearButton appliedpackaging$clearButton;
    @Unique
    private PackageColorPicker.TriggerButton appliedpackaging$colorButton;
    @Unique
    private final PackageColorPicker appliedpackaging$colorPicker = new PackageColorPicker();

    protected PatternEncodingTermScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void appliedpackaging$createPackageModeWidgets(
            C menu,
            Inventory playerInventory,
            Component title,
            appeng.client.gui.style.ScreenStyle style,
            CallbackInfo ci) {
        appliedpackaging$packageTabButton = new PackageModeWidgets.ModeTabButton(
                Component.translatable("gui.appliedpackaging.package_pattern.mode"),
                button -> ((PackageCraftingPatternMenuBridge) appliedpackaging$menu())
                        .appliedpackaging$setPackageCraftingMode(true));
        appliedpackaging$clearButton = new PackageModeWidgets.ClearButton(() -> appliedpackaging$menu().clear());
        appliedpackaging$colorButton = new PackageColorPicker.TriggerButton(
                8,
                8,
                () -> ((PackageCraftingPatternMenuBridge) appliedpackaging$menu())
                        .appliedpackaging$getPackageCraftingColor(),
                this::appliedpackaging$openColorPicker);
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"))
    private void appliedpackaging$updatePackageModeWidgets(CallbackInfo ci) {
        appliedpackaging$ensurePackageModeWidgetsAdded();
        PackageCraftingPatternMenuBridge bridge = (PackageCraftingPatternMenuBridge) appliedpackaging$menu();
        boolean packageMode = bridge.appliedpackaging$isPackageCraftingMode();
        if (!packageMode && appliedpackaging$isPackageSettingsOpen()) {
            appliedpackaging$closeAllEditors();
        }

        boolean modalOpen = appliedpackaging$isPackageSettingsOpen();
        appliedpackaging$packageTabButton.visible = true;
        appliedpackaging$packageTabButton.setSelected(packageMode);
        appliedpackaging$clearButton.visible = packageMode;
        appliedpackaging$clearButton.active = packageMode && !modalOpen;
        appliedpackaging$colorButton.visible = packageMode;
        appliedpackaging$colorButton.active = packageMode && !modalOpen;
        if (packageMode) {
            for (TabButton button : modeTabButtons.values()) {
                button.setSelected(false);
            }
            modePanels.get(EncodingMode.CRAFTING).setVisible(false);
            modePanels.get(EncodingMode.PROCESSING).setVisible(true);
        }
        if (modePanels.get(EncodingMode.CRAFTING) instanceof PackageCraftingPanelBridge bridgePanel) {
            bridgePanel.appliedpackaging$setPackageModeControlsHidden(packageMode);
        }
        if (modePanels.get(EncodingMode.PROCESSING) instanceof PackageProcessingPanelBridge bridgePanel) {
            bridgePanel.appliedpackaging$setPackageMode(packageMode);
        }
        appliedpackaging$layoutPackageModeWidgets(packageMode);

    }

    @Unique
    private void appliedpackaging$ensurePackageModeWidgetsAdded() {
        if (appliedpackaging$packageTabButton == null || children().contains(appliedpackaging$packageTabButton)) {
            return;
        }
        addRenderableWidget(appliedpackaging$packageTabButton);
        addRenderableWidget(appliedpackaging$clearButton);
        addRenderableWidget(appliedpackaging$colorButton);
        appliedpackaging$layoutPackageModeWidgets(false);
    }

    @Unique
    private void appliedpackaging$layoutPackageModeWidgets(boolean packageMode) {
        if (appliedpackaging$packageTabButton == null) {
            return;
        }
        AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
        int leftPos = screen.appliedpackaging$getLeftPos();
        int topPos = screen.appliedpackaging$getTopPos();
        int imageHeight = screen.appliedpackaging$getImageHeight();

        appliedpackaging$packageTabButton.setX(leftPos + 173);
        appliedpackaging$packageTabButton.setY(topPos + imageHeight - 90);
        appliedpackaging$packageTabButton.setWidth(20);
        appliedpackaging$packageTabButton.setHeight(20);

        int panelX = AP_PACKAGE_PANEL_X;
        int panelY = imageHeight - AP_PACKAGE_PANEL_BOTTOM;

        int controlY = topPos + panelY + AP_PACKAGE_CONTROL_REL_Y;
        appliedpackaging$clearButton.setX(leftPos + panelX + AP_PACKAGE_CLEAR_REL_X);
        appliedpackaging$clearButton.setY(controlY);
        appliedpackaging$colorButton.setX(leftPos + panelX + AP_PACKAGE_COLOR_REL_X);
        appliedpackaging$colorButton.setY(controlY);

        int gridX = packageMode ? panelX + AP_PACKAGE_GRID_REL_X : AP_V15_GRID_X;
        int gridY = packageMode ? panelY + AP_PACKAGE_GRID_REL_Y : imageHeight - AP_V15_GRID_BOTTOM;
        boolean craftingSlotsActive = !packageMode
                && appliedpackaging$menu().getMode() == EncodingMode.CRAFTING;
        var craftingSlots = appliedpackaging$menu().getCraftingGridSlots();
        for (int index = 0; index < craftingSlots.length; index++) {
            ((SlotAccessor) craftingSlots[index]).appliedpackaging$setX(packageMode
                    ? AP_HIDDEN_SLOT_POSITION
                    : gridX + (index % 3) * AP_SLOT_STEP);
            ((SlotAccessor) craftingSlots[index]).appliedpackaging$setY(packageMode
                    ? AP_HIDDEN_SLOT_POSITION
                    : gridY + (index / 3) * AP_SLOT_STEP);
            craftingSlots[index].setActive(craftingSlotsActive);
        }

        if (packageMode) {
            PatternEncodingTermScreen<?> screenObject = (PatternEncodingTermScreen<?>) (Object) this;
            screenObject.setSlotsHidden(SlotSemantics.PROCESSING_INPUTS, false);
            screenObject.setSlotsHidden(SlotSemantics.PROCESSING_OUTPUTS, true);
            screenObject.setSlotsHidden(SlotSemantics.CRAFTING_RESULT, false);
        }

        for (Slot slot : appliedpackaging$menu().slots) {
            if (appliedpackaging$menu().getSlotSemantic(slot) == SlotSemantics.CRAFTING_RESULT) {
                ((SlotAccessor) slot).appliedpackaging$setX(packageMode
                        ? panelX + AP_PACKAGE_RESULT_REL_X
                        : AP_V15_RESULT_X);
                ((SlotAccessor) slot).appliedpackaging$setY(packageMode
                        ? panelY + AP_PACKAGE_RESULT_REL_Y
                        : imageHeight - AP_V15_RESULT_BOTTOM);
                if (slot instanceof AppEngSlot appEngSlot) {
                    appEngSlot.setActive(packageMode || craftingSlotsActive);
                }
                break;
            }
        }

        var markerSlot = ((PackageCraftingPatternMenuBridge) appliedpackaging$menu())
                .appliedpackaging$getPackageCraftingMarkerSlot();
        if (markerSlot != null) {
            ((SlotAccessor) markerSlot).appliedpackaging$setX(panelX + AP_PACKAGE_MARKER_REL_X);
            ((SlotAccessor) markerSlot).appliedpackaging$setY(panelY + AP_PACKAGE_MARKER_REL_Y);
            markerSlot.setActive(packageMode);
        }
    }

    @Override
    public void appliedpackaging$openPackageSettings() {
        appliedpackaging$openColorPicker();
    }

    @Unique
    private void appliedpackaging$openColorPicker() {
        PackageCraftingPatternMenuBridge bridge = (PackageCraftingPatternMenuBridge) appliedpackaging$menu();
        appliedpackaging$colorPicker.openNear(
                appliedpackaging$colorButton,
                width,
                height,
                bridge::appliedpackaging$getPackageCraftingColor,
                bridge::appliedpackaging$setPackageCraftingColor,
                () -> setFocused(null));
        setFocused(null);
    }

    @Unique
    private void appliedpackaging$closeAllEditors() {
        appliedpackaging$colorPicker.close();
    }

    @Override
    public boolean appliedpackaging$isPackageSettingsOpen() {
        return appliedpackaging$colorPicker.isOpen();
    }

    @Override
    public void appliedpackaging$renderPackageSettingsOverlay(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick) {
        appliedpackaging$colorPicker.renderLast(graphics, minecraft.font, mouseX, mouseY);
    }

    @Override
    public boolean appliedpackaging$handlePackageSettingsMouseClicked(double mouseX, double mouseY, int button) {
        if (appliedpackaging$colorPicker.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean appliedpackaging$handlePackageSettingsMouseReleased(double mouseX, double mouseY, int button) {
        if (appliedpackaging$colorPicker.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean appliedpackaging$handlePackageSettingsMouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY) {
        if (appliedpackaging$colorPicker.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean appliedpackaging$handlePackageSettingsMouseScrolled(
            double mouseX,
            double mouseY,
            double delta) {
        return appliedpackaging$colorPicker.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean appliedpackaging$handlePackageSettingsKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (appliedpackaging$colorPicker.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean appliedpackaging$handlePackageSettingsCharTyped(char codePoint, int modifiers) {
        if (appliedpackaging$colorPicker.charTyped(codePoint, modifiers)) {
            return true;
        }
        return false;
    }

    @Unique
    @SuppressWarnings("unchecked")
    private C appliedpackaging$menu() {
        return ((PatternEncodingTermScreen<C>) (Object) this).getMenu();
    }
}

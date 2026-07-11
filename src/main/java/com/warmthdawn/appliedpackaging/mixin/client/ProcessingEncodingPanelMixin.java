package com.warmthdawn.appliedpackaging.mixin.client;

import appeng.client.Point;
import appeng.client.gui.me.items.EncodingModePanel;
import appeng.client.gui.me.items.ProcessingEncodingPanel;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.SlotSemantics;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageProcessingPanelBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ProcessingEncodingPanel.class, remap = false)
public abstract class ProcessingEncodingPanelMixin implements PackageProcessingPanelBridge {
    @Unique
    private static final ResourceLocation AP_LATEST_WIDGET_SPRITES = new ResourceLocation(
            "appliedpackaging",
            "textures/gui/advanced_pattern_encoding_terminal_sprites.png");
    @Unique
    private static final Scrollbar.Style AP_LATEST_SMALL_SCROLLBAR = Scrollbar.Style.create(
            AP_LATEST_WIDGET_SPRITES,
            7,
            15,
            0,
            32,
            16,
            32);
    @Unique
    private static final int AP_PACKAGE_SCROLLBAR_X = 15;
    @Unique
    private static final int AP_PACKAGE_SCROLLBAR_BOTTOM = 158;
    @Unique
    private static final int AP_V15_SCROLLBAR_X = 17;
    @Unique
    private static final int AP_V15_SCROLLBAR_BOTTOM = 156;
    @Unique
    private static final int AP_PACKAGE_INPUT_X = 24;
    @Unique
    private static final int AP_PACKAGE_INPUT_BOTTOM = 158;
    @Unique
    private static final int AP_VISIBLE_ROWS = 3;
    @Unique
    private static final int AP_COLUMNS = 3;
    @Unique
    private static final int AP_SLOT_STEP = 18;
    @Unique
    private static final int AP_HIDDEN_SLOT_POSITION = -10_000;
    @Unique
    private static final Blitter AP_PACKAGE_MODE_BACKGROUND = Blitter.texture(new ResourceLocation(
            "appliedpackaging",
            "textures/gui/pattern_mode_packaging.png"))
            .src(0, 0, 124, 66);

    @Shadow
    @Final
    private ActionButton clearBtn;
    @Shadow
    @Final
    private ActionButton cycleOutputBtn;
    @Shadow
    @Final
    private Scrollbar scrollbar;

    @Unique
    private boolean appliedpackaging$packageMode;

    @Override
    public void appliedpackaging$setPackageMode(boolean packageMode) {
        appliedpackaging$packageMode = packageMode;
        ((ScrollbarAccessor) scrollbar).appliedpackaging$setStyle(
                packageMode ? AP_LATEST_SMALL_SCROLLBAR : Scrollbar.SMALL);
        appliedpackaging$updateControls();
        appliedpackaging$updateTooltips();
    }

    @Override
    public int appliedpackaging$getCurrentScroll() {
        return scrollbar.getCurrentScroll();
    }

    @Inject(method = "setVisible", at = @At("TAIL"))
    private void appliedpackaging$updatePackageControlsOnVisibility(boolean visible, CallbackInfo ci) {
        appliedpackaging$updateControls();
        appliedpackaging$updateTooltips();
    }

    @Inject(method = "updateBeforeRender", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$replaceProcessingLayoutInPackageMode(CallbackInfo ci) {
        if (!appliedpackaging$packageMode) {
            return;
        }
        appliedpackaging$layoutPackageSlots();
        appliedpackaging$updateControls();
        appliedpackaging$updateTooltips();
        ci.cancel();
    }

    @Inject(method = "drawBackgroundLayer", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$drawPackageModeBackground(
            GuiGraphics graphics,
            Rect2i bounds,
            Point mouse,
            CallbackInfo ci) {
        if (!appliedpackaging$packageMode) {
            scrollbar.setPosition(new Point(
                    AP_V15_SCROLLBAR_X,
                    bounds.getHeight() - AP_V15_SCROLLBAR_BOTTOM));
            return;
        }
        scrollbar.setPosition(new Point(
                AP_PACKAGE_SCROLLBAR_X,
                bounds.getHeight() - AP_PACKAGE_SCROLLBAR_BOTTOM));
        AP_PACKAGE_MODE_BACKGROUND
                .dest(bounds.getX() + 8, bounds.getY() + bounds.getHeight() - 165)
                .blit(graphics);
        ci.cancel();
    }

    @Unique
    private void appliedpackaging$updateControls() {
        if (appliedpackaging$packageMode) {
            clearBtn.setVisibility(false);
            cycleOutputBtn.setVisibility(false);
        }
    }

    @Unique
    private void appliedpackaging$layoutPackageSlots() {
        if (!appliedpackaging$packageMode) {
            return;
        }

        var panel = (EncodingModePanelAccessor) (Object) this;
        var screen = panel.appliedpackaging$getScreen();
        var menu = panel.appliedpackaging$getMenu();
        int imageHeight = ((AbstractContainerScreenAccessor) screen).appliedpackaging$getImageHeight();
        int gridY = imageHeight - AP_PACKAGE_INPUT_BOTTOM;
        int rowScroll = scrollbar.getCurrentScroll();

        screen.setSlotsHidden(SlotSemantics.PROCESSING_INPUTS, false);
        screen.setSlotsHidden(SlotSemantics.PROCESSING_OUTPUTS, true);

        var inputs = menu.getProcessingInputSlots();
        for (int index = 0; index < inputs.length; index++) {
            int visibleRow = index / AP_COLUMNS - rowScroll;
            boolean visible = visibleRow >= 0 && visibleRow < AP_VISIBLE_ROWS;
            inputs[index].setActive(visible);
            ((SlotAccessor) inputs[index]).appliedpackaging$setX(visible
                    ? AP_PACKAGE_INPUT_X + (index % AP_COLUMNS) * AP_SLOT_STEP
                    : AP_HIDDEN_SLOT_POSITION);
            ((SlotAccessor) inputs[index]).appliedpackaging$setY(visible
                    ? gridY + visibleRow * AP_SLOT_STEP
                    : AP_HIDDEN_SLOT_POSITION);
        }

        for (var output : menu.getProcessingOutputSlots()) {
            ((SlotAccessor) output).appliedpackaging$setX(AP_HIDDEN_SLOT_POSITION);
            ((SlotAccessor) output).appliedpackaging$setY(AP_HIDDEN_SLOT_POSITION);
            output.setActive(false);
        }
    }

    @Unique
    private void appliedpackaging$updateTooltips() {
        if (!appliedpackaging$packageMode) {
            return;
        }
        var widgets = ((EncodingModePanelAccessor) (Object) this).appliedpackaging$getWidgets();
        widgets.setTooltipAreaEnabled("processing-primary-output", false);
        widgets.setTooltipAreaEnabled("processing-optional-output1", false);
        widgets.setTooltipAreaEnabled("processing-optional-output2", false);
        widgets.setTooltipAreaEnabled("processing-optional-output3", false);
    }
}

package com.warmthdawn.appliedpackaging.mixin.client;

import appeng.client.gui.me.items.CraftingEncodingPanel;
import appeng.client.gui.me.items.EncodingModePanel;
import appeng.client.Point;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ToggleButton;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPanelBridge;
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

@Mixin(value = CraftingEncodingPanel.class, remap = false)
public abstract class CraftingEncodingPanelMixin implements PackageCraftingPanelBridge {
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
    private ToggleButton substitutionsBtn;
    @Shadow
    @Final
    private ToggleButton fluidSubstitutionsBtn;

    @Unique
    private boolean appliedpackaging$hidePackageModeControls;

    @Override
    public void appliedpackaging$setPackageModeControlsHidden(boolean hidden) {
        this.appliedpackaging$hidePackageModeControls = hidden;
        appliedpackaging$updateControlVisibility();
    }

    @Inject(method = "setVisible", at = @At("TAIL"))
    private void appliedpackaging$hideCraftingControlsInPackageMode(boolean visible, CallbackInfo ci) {
        appliedpackaging$updateControlVisibility();
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"))
    private void appliedpackaging$keepCraftingControlsHiddenInPackageMode(CallbackInfo ci) {
        appliedpackaging$updateControlVisibility();
    }

    @Inject(method = "drawBackgroundLayer", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$drawPackageModeBackground(
            GuiGraphics graphics,
            Rect2i bounds,
            Point mouse,
            CallbackInfo ci) {
        if (!appliedpackaging$hidePackageModeControls) {
            return;
        }
        AP_PACKAGE_MODE_BACKGROUND
                .dest(bounds.getX() + 9, bounds.getY() + bounds.getHeight() - 164)
                .blit(graphics);
        ci.cancel();
    }

    @Unique
    private void appliedpackaging$updateControlVisibility() {
        boolean visible = ((EncodingModePanel) (Object) this).isVisible()
                && !appliedpackaging$hidePackageModeControls;
        clearBtn.setVisibility(visible);
        substitutionsBtn.setVisibility(visible);
        fluidSubstitutionsBtn.setVisibility(visible);
    }
}

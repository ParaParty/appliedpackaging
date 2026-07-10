package com.warmthdawn.appliedpackaging.mixin.client;

import appeng.client.gui.me.items.CraftingEncodingPanel;
import appeng.client.gui.me.items.EncodingModePanel;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ToggleButton;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPanelBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingEncodingPanel.class, remap = false)
public abstract class CraftingEncodingPanelMixin implements PackageCraftingPanelBridge {
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

    @Unique
    private void appliedpackaging$updateControlVisibility() {
        boolean visible = ((EncodingModePanel) (Object) this).isVisible()
                && !appliedpackaging$hidePackageModeControls;
        clearBtn.setVisibility(visible);
        substitutionsBtn.setVisibility(visible);
        fluidSubstitutionsBtn.setVisibility(visible);
    }
}

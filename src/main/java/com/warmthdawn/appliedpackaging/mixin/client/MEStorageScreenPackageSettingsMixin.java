package com.warmthdawn.appliedpackaging.mixin.client;

import appeng.client.gui.me.common.MEStorageScreen;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternScreenBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MEStorageScreen.class, remap = false)
public abstract class MEStorageScreenPackageSettingsMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$interceptPackageSettingsMouseClicked(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PackageCraftingPatternScreenBridge bridge
                && bridge.appliedpackaging$handlePackageSettingsMouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$interceptPackageSettingsMouseScrolled(
            double mouseX,
            double mouseY,
            double delta,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PackageCraftingPatternScreenBridge bridge
                && bridge.appliedpackaging$handlePackageSettingsMouseScrolled(mouseX, mouseY, delta)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$interceptPackageSettingsKeyPressed(
            int keyCode,
            int scanCode,
            int modifiers,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PackageCraftingPatternScreenBridge bridge
                && bridge.appliedpackaging$handlePackageSettingsKeyPressed(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$interceptPackageSettingsCharTyped(
            char codePoint,
            int modifiers,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PackageCraftingPatternScreenBridge bridge
                && bridge.appliedpackaging$handlePackageSettingsCharTyped(codePoint, modifiers)) {
            cir.setReturnValue(true);
        }
    }
}

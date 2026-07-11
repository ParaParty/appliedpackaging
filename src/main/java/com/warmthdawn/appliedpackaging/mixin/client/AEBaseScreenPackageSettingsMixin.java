package com.warmthdawn.appliedpackaging.mixin.client;

import appeng.client.gui.AEBaseScreen;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternScreenBridge;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AEBaseScreen.class, remap = false)
public abstract class AEBaseScreenPackageSettingsMixin {
    @Unique
    private Slot appliedpackaging$savedHoveredSlot;
    @Unique
    private boolean appliedpackaging$hoveredSlotSuppressed;

    @Inject(method = "renderTooltips", at = @At("HEAD"))
    private void appliedpackaging$suppressHoveredSlotBehindSettings(
            net.minecraft.client.gui.GuiGraphics graphics,
            int mouseX,
            int mouseY,
            CallbackInfo ci) {
        if ((Object) this instanceof PackageCraftingPatternScreenBridge bridge
                && bridge.appliedpackaging$isPackageSettingsOpen()) {
            AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
            appliedpackaging$savedHoveredSlot = screen.appliedpackaging$getHoveredSlot();
            appliedpackaging$hoveredSlotSuppressed = true;
            screen.appliedpackaging$setHoveredSlot(null);
        }
    }

    @Inject(method = "renderTooltips", at = @At("RETURN"))
    private void appliedpackaging$restoreHoveredSlotAfterSettings(
            net.minecraft.client.gui.GuiGraphics graphics,
            int mouseX,
            int mouseY,
            CallbackInfo ci) {
        if (appliedpackaging$hoveredSlotSuppressed) {
            ((AbstractContainerScreenAccessor) this)
                    .appliedpackaging$setHoveredSlot(appliedpackaging$savedHoveredSlot);
            appliedpackaging$savedHoveredSlot = null;
            appliedpackaging$hoveredSlotSuppressed = false;
        }
    }

    @ModifyVariable(method = "renderTooltips", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int appliedpackaging$moveTooltipMouseXOutsideSettings(int mouseX) {
        return appliedpackaging$tooltipCoordinate(mouseX);
    }

    @ModifyVariable(method = "renderTooltips", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int appliedpackaging$moveTooltipMouseYOutsideSettings(int mouseY) {
        return appliedpackaging$tooltipCoordinate(mouseY);
    }

    @Unique
    private int appliedpackaging$tooltipCoordinate(int coordinate) {
        if ((Object) this instanceof PackageCraftingPatternScreenBridge bridge
                && bridge.appliedpackaging$isPackageSettingsOpen()) {
            return -1;
        }
        return coordinate;
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$interceptPackageSettingsMouseReleased(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PackageCraftingPatternScreenBridge bridge
                && bridge.appliedpackaging$handlePackageSettingsMouseReleased(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$interceptPackageSettingsMouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PackageCraftingPatternScreenBridge bridge
                && bridge.appliedpackaging$handlePackageSettingsMouseDragged(
                        mouseX,
                        mouseY,
                        button,
                        dragX,
                        dragY)) {
            cir.setReturnValue(true);
        }
    }
}

package com.warmthdawn.appliedpackaging.mixin;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.warmthdawn.appliedpackaging.registry.APItems;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps both specialized carriers out of AE2's ordinary pattern terminal. The
 * specialized terminal overrides the validation method and explicitly accepts
 * both Applied Packaging carrier types.
 */
@Mixin(value = AEBaseMenu.class, remap = false)
abstract class PatternEncodingTermMenuMixin {
    @Inject(method = "isValidForSlot", at = @At("HEAD"), cancellable = true, remap = false)
    private void appliedpackaging$rejectPackagePatternInOrdinaryTerminal(
            Slot slot,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> callback) {
        AEBaseMenu menu = (AEBaseMenu) (Object) this;
        if (menu instanceof PatternEncodingTermMenu
                && (stack.is(APItems.PACKAGE_PATTERN.get())
                        || stack.is(APItems.ADVANCED_PROCESSING_PATTERN.get()))
                && menu.getSlotSemantic(slot) == SlotSemantics.ENCODED_PATTERN) {
            callback.setReturnValue(false);
        }
    }
}

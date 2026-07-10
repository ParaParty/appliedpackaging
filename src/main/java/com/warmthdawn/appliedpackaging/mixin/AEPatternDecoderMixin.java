package com.warmthdawn.appliedpackaging.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.AEPatternDecoder;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDetails;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AEPatternDecoder.class, remap = false)
public abstract class AEPatternDecoderMixin {
    @Inject(method = "decodePattern(Lappeng/api/stacks/AEItemKey;Lnet/minecraft/world/level/Level;)Lappeng/api/crafting/IPatternDetails;",
            at = @At("HEAD"),
            cancellable = true)
    private void appliedpackaging$decodePackageCraftingPatternKey(
            AEItemKey what,
            Level level,
            CallbackInfoReturnable<IPatternDetails> cir) {
        if (what == null || level == null || !PackageCraftingPatternDataStorage.hasData(what.toStack())) {
            return;
        }
        try {
            cir.setReturnValue(new PackageCraftingPatternDetails(what));
        } catch (IllegalArgumentException ignored) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "decodePattern(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Z)Lappeng/api/crafting/IPatternDetails;",
            at = @At("HEAD"),
            cancellable = true)
    private void appliedpackaging$decodePackageCraftingPatternStack(
            ItemStack what,
            Level level,
            boolean tryRecovery,
            CallbackInfoReturnable<IPatternDetails> cir) {
        if (what == null || level == null || !PackageCraftingPatternDataStorage.hasData(what)) {
            return;
        }
        AEItemKey definition = AEItemKey.of(what);
        if (definition == null) {
            cir.setReturnValue(null);
            return;
        }
        try {
            cir.setReturnValue(new PackageCraftingPatternDetails(definition));
        } catch (IllegalArgumentException ignored) {
            cir.setReturnValue(null);
        }
    }
}

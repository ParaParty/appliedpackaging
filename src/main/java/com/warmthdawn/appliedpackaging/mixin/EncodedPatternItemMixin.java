package com.warmthdawn.appliedpackaging.mixin;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AmountFormat;
import appeng.crafting.pattern.EncodedPatternItem;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EncodedPatternItem.class, remap = false)
public abstract class EncodedPatternItemMixin {
    @Inject(method = "getOutput", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$getPackageCraftingOutput(ItemStack item, CallbackInfoReturnable<ItemStack> cir) {
        PackageCraftingPatternDataStorage.read(item)
                .map(PackageCraftingPatternDataStorage::toPackageStack)
                .ifPresent(cir::setReturnValue);
    }

    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$appendPackageCraftingTooltip(
            ItemStack stack,
            Level level,
            List<Component> lines,
            TooltipFlag advancedTooltips,
            CallbackInfo ci) {
        var encoded = PackageCraftingPatternDataStorage.read(stack);
        if (encoded.isEmpty()) {
            return;
        }
        ItemStack output = PackageCraftingPatternDataStorage.toPackageStack(encoded.get());
        lines.add(Component.translatable("gui.appliedpackaging.package_pattern.crafts")
                .append(": ")
                .withStyle(ChatFormatting.GRAY)
                .append(output.getHoverName()));
        boolean first = true;
        for (var input : encoded.get().denseInputs()) {
            var key = input.what();
            String amount = key.formatAmount(input.amount(), AmountFormat.FULL);
            Component prefix = first
                    ? Component.translatable("gui.appliedpackaging.package_pattern.with")
                    : Component.translatable("gui.appliedpackaging.package_pattern.and");
            lines.add(prefix.copy()
                    .append(": ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(amount + " x "))
                    .append(key.getDisplayName()));
            first = false;
        }
        encoded.get().data().marker().ifPresent(marker -> {
            var key = marker.stack().what();
            String amount = key.formatAmount(marker.stack().amount(), AmountFormat.FULL);
            lines.add(Component.translatable("gui.appliedpackaging.package_pattern.marker")
                    .append(": ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(amount + " x "))
                    .append(key.getDisplayName()));
        });
        if (AEItemKey.of(output) == null) {
            lines.add(Component.translatable("gui.appliedpackaging.package_pattern.invalid").withStyle(ChatFormatting.RED));
        }
        ci.cancel();
    }
}

package com.warmthdawn.appliedpackaging.item;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.EncodedPatternItem;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDetails;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class PackageCraftingPatternItem extends EncodedPatternItem {
    public PackageCraftingPatternItem(Properties properties) {
        super(properties);
    }

    @Override
    public IPatternDetails decode(ItemStack stack, Level level, boolean tryRecovery) {
        AEItemKey key = AEItemKey.of(stack);
        return key == null ? null : decode(key, level);
    }

    @Override
    public IPatternDetails decode(AEItemKey what, Level level) {
        if (what == null || level == null || !PackageCraftingPatternDataStorage.hasData(what.toStack())) {
            return null;
        }
        try {
            return new PackageCraftingPatternDetails(what);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public ItemStack getOutput(ItemStack stack) {
        return PackageCraftingPatternDataStorage.read(stack)
                .map(PackageCraftingPatternDataStorage::toPackageStack)
                .orElseGet(() -> super.getOutput(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        PackageCraftingPatternDataStorage.read(stack).ifPresent(pattern -> {
            ItemStack output = PackageCraftingPatternDataStorage.toPackageStack(pattern);
            tooltip.add(Component.translatable("gui.appliedpackaging.package_pattern.crafts")
                    .append(": ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(output.getHoverName()));

            boolean first = true;
            for (GenericStack input : pattern.denseInputs()) {
                Component prefix = Component.translatable(first
                        ? "gui.appliedpackaging.package_pattern.with"
                        : "gui.appliedpackaging.package_pattern.and");
                tooltip.add(prefix.copy()
                        .append(": ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(input.what().formatAmount(input.amount(), AmountFormat.FULL) + " x "))
                        .append(input.what().getDisplayName()));
                first = false;
            }

            pattern.data().marker().ifPresent(marker -> {
                var key = marker.stack().what();
                tooltip.add(Component.translatable("gui.appliedpackaging.package_pattern.marker")
                        .append(": ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(
                                key.formatAmount(marker.stack().amount(), AmountFormat.FULL) + " x "))
                        .append(key.getDisplayName()));
            });
            if (AEItemKey.of(output) == null) {
                tooltip.add(Component.translatable("gui.appliedpackaging.package_pattern.invalid")
                        .withStyle(ChatFormatting.RED));
            }
        });
    }
}

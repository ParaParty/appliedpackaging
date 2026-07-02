package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class PackageTooltipBuilder {
    private static final int MAX_VISIBLE_CONTENT_LINES = 5;

    private PackageTooltipBuilder() {
    }

    public static void append(
            ItemStack stack,
            PackageColor color,
            PackageData data,
            List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.appliedpackaging.package.color",
                        Component.translatable("tooltip.appliedpackaging.color." + color.translationKeySuffix())
                                .withStyle(color.formatting()))
                .withStyle(ChatFormatting.GRAY));

        data.marker().ifPresent(marker -> tooltip.add(Component.translatable(
                        "tooltip.appliedpackaging.package.marker",
                        marker.stack().what().getDisplayName())
                .withStyle(ChatFormatting.GRAY)));

        tooltip.add(Component.translatable("tooltip.appliedpackaging.package.per_package")
                .withStyle(ChatFormatting.GRAY));
        appendContents(data.contents(), 1, tooltip);

        if (stack.getCount() > 1) {
            tooltip.add(Component.translatable("tooltip.appliedpackaging.package.stack_total")
                    .withStyle(ChatFormatting.GRAY));
            appendContents(data.contents(), stack.getCount(), tooltip);
        }

        if (flag.isAdvanced()) {
            tooltip.add(Component.translatable(
                            "tooltip.appliedpackaging.package.capacity",
                            data.usedUnits(),
                            data.usedTypes())
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable(
                            "tooltip.appliedpackaging.package.hash",
                            data.canonicalHash().substring(0, Math.min(8, data.canonicalHash().length())))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void appendContents(List<GenericStack> contents, int multiplier, List<Component> tooltip) {
        int shown = Math.min(MAX_VISIBLE_CONTENT_LINES, contents.size());
        for (int i = 0; i < shown; i++) {
            GenericStack entry = contents.get(i);
            long amount = entry.amount() * multiplier;
            tooltip.add(Component.translatable(
                            "tooltip.appliedpackaging.package.entry",
                            entry.what().getDisplayName(),
                            entry.what().formatAmount(amount, AmountFormat.FULL))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        int hidden = contents.size() - shown;
        if (hidden > 0) {
            tooltip.add(Component.translatable("tooltip.appliedpackaging.package.more_entries", hidden)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}

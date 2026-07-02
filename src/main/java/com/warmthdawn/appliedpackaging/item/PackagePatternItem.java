package com.warmthdawn.appliedpackaging.item;

import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackagedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageTooltipBuilder;
import com.warmthdawn.appliedpackaging.registry.APItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class PackagePatternItem extends Item {
    public PackagePatternItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.is(APItems.PACKAGED_PROCESSING_PATTERN.get())) {
            PackagedProcessingPatternDataStorage.read(stack).ifPresentOrElse(pattern -> {
                tooltip.add(Component.translatable("tooltip.appliedpackaging.processing_pattern.encoded")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable(
                                "tooltip.appliedpackaging.processing_pattern.package_count",
                                pattern.packages().size())
                        .withStyle(ChatFormatting.GRAY));
                PackageTooltipBuilder.append(stack, pattern.color(), pattern.packages().get(0), tooltip, flag);
                int hidden = pattern.packages().size() - 1;
                if (hidden > 0) {
                    tooltip.add(Component.translatable(
                                    "tooltip.appliedpackaging.processing_pattern.more_packages",
                                    hidden)
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
                appendProcessingOutputs(pattern.outputs(), tooltip);
            }, () -> tooltip.add(Component.translatable("tooltip.appliedpackaging.pattern.blank")
                    .withStyle(ChatFormatting.DARK_GRAY)));
            return;
        }
        PackagePatternDataStorage.read(stack).ifPresentOrElse(pattern -> {
            tooltip.add(Component.translatable("tooltip.appliedpackaging.pattern.encoded").withStyle(ChatFormatting.GRAY));
            PackageTooltipBuilder.append(stack, pattern.color(), pattern.data(), tooltip, flag);
        }, () -> tooltip.add(Component.translatable("tooltip.appliedpackaging.pattern.blank").withStyle(ChatFormatting.DARK_GRAY)));
    }

    private static void appendProcessingOutputs(List<GenericStack> outputs, List<Component> tooltip) {
        if (outputs.isEmpty()) {
            return;
        }
        tooltip.add(Component.translatable("tooltip.appliedpackaging.processing_pattern.outputs")
                .withStyle(ChatFormatting.GRAY));
        int shown = Math.min(3, outputs.size());
        for (int index = 0; index < shown; index++) {
            GenericStack output = outputs.get(index);
            tooltip.add(Component.translatable(
                            "tooltip.appliedpackaging.processing_pattern.output_entry",
                            output.what().getDisplayName(),
                            output.what().formatAmount(output.amount(), AmountFormat.FULL))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        int hidden = outputs.size() - shown;
        if (hidden > 0) {
            tooltip.add(Component.translatable(
                            "tooltip.appliedpackaging.processing_pattern.more_outputs",
                            hidden)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}

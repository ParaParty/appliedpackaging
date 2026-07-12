package com.warmthdawn.appliedpackaging.item;

import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
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
        appendPackagePatternTooltip(stack, tooltip, flag);
    }

    public static void appendPackagePatternTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        var craftingPattern = PackageCraftingPatternDataStorage.read(stack);
        if (craftingPattern.isPresent()) {
            appendCraftingPatternTooltip(craftingPattern.get(), tooltip);
        }
    }

    private static void appendCraftingPatternTooltip(
            PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern pattern,
            List<Component> tooltip) {
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
                    .append(Component.literal(key.formatAmount(marker.stack().amount(), AmountFormat.FULL) + " x "))
                    .append(key.getDisplayName()));
        });
        if (AEItemKey.of(output) == null) {
            tooltip.add(Component.translatable("gui.appliedpackaging.package_pattern.invalid")
                    .withStyle(ChatFormatting.RED));
        }
    }

}

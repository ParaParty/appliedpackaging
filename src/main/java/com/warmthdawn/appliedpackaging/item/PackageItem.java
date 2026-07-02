package com.warmthdawn.appliedpackaging.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class PackageItem extends Item {
    public static final String PACKAGE_TAG = "appliedpackaging.package";

    private final PackageColor color;

    public PackageItem(PackageColor color, Properties properties) {
        super(properties);
        this.color = color;
    }

    public PackageColor color() {
        return color;
    }

    public static boolean hasPackageData(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(PACKAGE_TAG);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasPackageData(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!hasPackageData(stack)) {
            tooltip.add(Component.translatable("tooltip.appliedpackaging.package.invalid")
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.appliedpackaging.package.invalid_hint")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("tooltip.appliedpackaging.package.color",
                        Component.translatable("tooltip.appliedpackaging.color." + color.translationKeySuffix())
                                .withStyle(color.formatting()))
                .withStyle(ChatFormatting.GRAY));
    }
}

package com.warmthdawn.appliedpackaging.item;

import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageTooltipBuilder;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class PackageItem extends Item {
    private final PackageColor color;

    public PackageItem(PackageColor color, Properties properties) {
        super(properties);
        this.color = color;
    }

    public PackageColor color() {
        return color;
    }

    public static boolean hasPackageData(ItemStack stack) {
        return PackageDataStorage.hasPackageData(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasPackageData(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        PackageDataStorage.read(stack).ifPresentOrElse(data -> {
            PackageTooltipBuilder.append(stack, color, data, tooltip, flag);
        }, () -> {
            tooltip.add(Component.translatable("tooltip.appliedpackaging.package.invalid")
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.appliedpackaging.package.invalid_hint")
                    .withStyle(ChatFormatting.GRAY));
        });
    }
}

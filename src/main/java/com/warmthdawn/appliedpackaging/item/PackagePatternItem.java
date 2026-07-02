package com.warmthdawn.appliedpackaging.item;

import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageTooltipBuilder;
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
        PackagePatternDataStorage.read(stack).ifPresentOrElse(pattern -> {
            tooltip.add(Component.translatable("tooltip.appliedpackaging.pattern.encoded").withStyle(ChatFormatting.GRAY));
            PackageTooltipBuilder.append(stack, pattern.color(), pattern.data(), tooltip, flag);
        }, () -> tooltip.add(Component.translatable("tooltip.appliedpackaging.pattern.blank").withStyle(ChatFormatting.DARK_GRAY)));
    }
}

package com.warmthdawn.appliedpackaging.item;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.EncodedPatternItem;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDetails;
import java.util.List;
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
        PackagePatternItem.appendPackagePatternTooltip(stack, tooltip, flag);
    }
}

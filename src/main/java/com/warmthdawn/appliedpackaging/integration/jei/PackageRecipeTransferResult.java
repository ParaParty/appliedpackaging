package com.warmthdawn.appliedpackaging.integration.jei;

import com.warmthdawn.appliedpackaging.core.pattern.PackagePatternTransferPlan;
import net.minecraft.network.chat.Component;

public record PackageRecipeTransferResult(
        PackagePatternTransferPlan plan,
        Component error) {
    public static PackageRecipeTransferResult success(PackagePatternTransferPlan plan) {
        return new PackageRecipeTransferResult(plan, null);
    }

    public static PackageRecipeTransferResult error(String translationKey, Object... arguments) {
        return new PackageRecipeTransferResult(null, Component.translatable(translationKey, arguments));
    }
}

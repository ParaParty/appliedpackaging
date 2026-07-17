package com.warmthdawn.appliedpackaging.integration.jei;

import com.warmthdawn.appliedpackaging.core.pattern.AdvancedPatternTransferPlan;
import net.minecraft.network.chat.Component;

public record AdvancedRecipeTransferResult(
        AdvancedPatternTransferPlan plan,
        Component error) {
    public static AdvancedRecipeTransferResult success(AdvancedPatternTransferPlan plan) {
        return new AdvancedRecipeTransferResult(plan, null);
    }

    public static AdvancedRecipeTransferResult error(String translationKey, Object... arguments) {
        return new AdvancedRecipeTransferResult(null, Component.translatable(translationKey, arguments));
    }
}

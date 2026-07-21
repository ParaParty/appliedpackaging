package com.warmthdawn.appliedpackaging.integration.recipe;

import com.warmthdawn.appliedpackaging.core.pattern.AdvancedPatternTransferPlan;
import com.warmthdawn.appliedpackaging.core.pattern.PackagePatternTransferPlan;
import net.minecraft.network.chat.Component;

public record RecipeTransferPlanResult(
        AdvancedPatternTransferPlan advancedPlan,
        PackagePatternTransferPlan packagePlan,
        Component error,
        boolean internalError) {
    public static RecipeTransferPlanResult advanced(AdvancedPatternTransferPlan plan) {
        return new RecipeTransferPlanResult(plan, null, null, false);
    }

    public static RecipeTransferPlanResult packaged(PackagePatternTransferPlan plan) {
        return new RecipeTransferPlanResult(null, plan, null, false);
    }

    public static RecipeTransferPlanResult userError(Component error) {
        return new RecipeTransferPlanResult(null, null, error, false);
    }

    public static RecipeTransferPlanResult userError(String key, Object... arguments) {
        return userError(Component.translatable(key, arguments));
    }

    public static RecipeTransferPlanResult unexpectedError() {
        return new RecipeTransferPlanResult(
                null,
                null,
                Component.translatable("gui.appliedpackaging.jei_transfer.internal_error"),
                true);
    }

    public boolean successful() {
        return advancedPlan != null || packagePlan != null;
    }
}

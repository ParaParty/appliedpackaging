package com.warmthdawn.appliedpackaging.integration.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;

/** Implemented in dependency-specific classes that are loaded only when their mod is present. */
public interface AdvancedRecipeTransferAdapter {
    boolean supports(Object recipe);

    default AdvancedRecipeTransferResult createPlan(Object recipe, IRecipeSlotsView recipeSlots) {
        return createPlan(recipe, recipeSlots, RecipeIngredientSelector.empty());
    }

    AdvancedRecipeTransferResult createPlan(
            Object recipe,
            IRecipeSlotsView recipeSlots,
            RecipeIngredientSelector ingredientSelector);
}

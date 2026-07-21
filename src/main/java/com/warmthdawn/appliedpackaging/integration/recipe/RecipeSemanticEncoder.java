package com.warmthdawn.appliedpackaging.integration.recipe;

/** Implemented in dependency-specific classes that are loaded only when their mod is present. */
public interface RecipeSemanticEncoder {
    boolean supports(Object recipe);

    default AdvancedRecipeTransferResult createPlan(Object recipe) {
        return createPlan(recipe, RecipeIngredientSelector.empty());
    }

    AdvancedRecipeTransferResult createPlan(
            Object recipe,
            RecipeIngredientSelector ingredientSelector);
}

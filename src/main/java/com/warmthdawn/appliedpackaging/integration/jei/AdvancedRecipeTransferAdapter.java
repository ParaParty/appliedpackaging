package com.warmthdawn.appliedpackaging.integration.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;

/** Implemented in dependency-specific classes that are loaded only when their mod is present. */
public interface AdvancedRecipeTransferAdapter {
    boolean supports(Object recipe);

    AdvancedRecipeTransferResult createPlan(Object recipe, IRecipeSlotsView recipeSlots);
}

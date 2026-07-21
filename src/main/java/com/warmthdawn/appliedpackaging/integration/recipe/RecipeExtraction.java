package com.warmthdawn.appliedpackaging.integration.recipe;

/**
 * Domain input produced by a recipe-viewer-specific extractor.
 *
 * <p>The semantic recipe is optional because some native viewer displays have no server recipe
 * object. The standard item/fluid data never retains viewer API objects.
 */
public record RecipeExtraction(Object semanticRecipe, StandardRecipeData standardData) {
    public RecipeExtraction {
        standardData = standardData == null ? StandardRecipeData.empty() : standardData;
    }
}

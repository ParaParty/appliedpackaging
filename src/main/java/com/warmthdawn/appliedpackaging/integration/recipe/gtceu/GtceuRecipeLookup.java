package com.warmthdawn.appliedpackaging.integration.recipe.gtceu;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import net.minecraft.resources.ResourceLocation;

/** Recovers GTCEu XEI display recipes through its public recipe-type/category registries. */
public final class GtceuRecipeLookup {
    private GtceuRecipeLookup() {
    }

    public static Object findById(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        GTRecipe match = null;
        for (GTRecipeType recipeType : GTRegistries.RECIPE_TYPES) {
            for (var category : recipeType.getCategories()) {
                for (GTRecipe recipe : recipeType.getRecipesInCategory(category)) {
                    if (!id.equals(recipe.getId())) {
                        continue;
                    }
                    if (hasLayeredData(recipe)) {
                        return recipe;
                    }
                    if (match == null) {
                        match = recipe;
                    }
                }
            }
        }
        return match;
    }

    private static boolean hasLayeredData(GTRecipe recipe) {
        return recipe.data != null
                && (recipe.data.contains("layered_steps")
                || recipe.data.contains("layered_xei")
                || recipe.data.contains("layered_info"));
    }
}

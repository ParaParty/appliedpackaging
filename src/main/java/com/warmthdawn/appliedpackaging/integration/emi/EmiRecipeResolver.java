package com.warmthdawn.appliedpackaging.integration.emi;

import com.warmthdawn.appliedpackaging.integration.recipe.gtceu.GtceuRecipeLookup;
import dev.emi.emi.api.recipe.EmiRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

/**
 * Resolves a native EMI display through public recipe APIs and public recipe
 * registries. Viewer implementation classes and their fields are never inspected.
 */
public final class EmiRecipeResolver {
    private EmiRecipeResolver() {
    }

    public static Object resolve(EmiRecipe recipe, Level level) {
        if (recipe == null) {
            return null;
        }
        try {
            Recipe<?> backingRecipe = recipe.getBackingRecipe();
            if (backingRecipe != null) {
                return backingRecipe;
            }
        } catch (RuntimeException | LinkageError ignored) {
        }

        ResourceLocation id = recipe.getId();
        if (id == null) {
            return null;
        }
        if (level != null) {
            Recipe<?> managedRecipe = level.getRecipeManager().byKey(id).orElse(null);
            if (managedRecipe != null) {
                return managedRecipe;
            }
        }
        return findGtceuDisplayRecipe(id);
    }

    private static Object findGtceuDisplayRecipe(ResourceLocation id) {
        if (!ModList.get().isLoaded("gtceu")) {
            return null;
        }
        return GtceuRecipeLookup.findById(id);
    }
}

package com.warmthdawn.appliedpackaging.integration.jei;

import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.integration.recipe.RecipeExtraction;
import com.warmthdawn.appliedpackaging.integration.recipe.StandardRecipeData;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Extracts JEI lifecycle data into the recipe-transfer domain model. */
public final class JeiRecipeExtractor {
    private static final String UNSUPPORTED_INGREDIENT =
            "gui.appliedpackaging.jei_transfer.unsupported_ingredient_type";
    private static final String AMBIGUOUS_OUTPUT =
            "gui.appliedpackaging.jei_transfer.ambiguous_output";

    private JeiRecipeExtractor() {
    }

    public static RecipeExtraction extract(Object recipe, IRecipeSlotsView slotsView) {
        return new RecipeExtraction(recipe, extractStandardData(slotsView));
    }

    public static StandardRecipeData extractStandardData(IRecipeSlotsView slotsView) {
        if (slotsView == null) {
            return StandardRecipeData.empty();
        }
        return new StandardRecipeData(
                convert(slotsView.getSlotViews(RecipeIngredientRole.INPUT), false),
                convert(slotsView.getSlotViews(RecipeIngredientRole.OUTPUT), true));
    }

    private static List<StandardRecipeData.Slot> convert(
            List<IRecipeSlotView> slots,
            boolean output) {
        List<StandardRecipeData.Slot> converted = new ArrayList<>(slots.size());
        for (IRecipeSlotView slot : slots) {
            List<ITypedIngredient<?>> ingredients = slot.getAllIngredients().toList();
            if (ingredients.isEmpty()) {
                converted.add(StandardRecipeData.Slot.supported(List.of(), null));
                continue;
            }

            List<GenericStack> candidates = ingredients.stream()
                    .map(JeiRecipeExtractor::toGenericStack)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (candidates.isEmpty()) {
                converted.add(StandardRecipeData.Slot.rejected(UNSUPPORTED_INGREDIENT));
                continue;
            }
            if (output && candidates.size() != ingredients.size()) {
                converted.add(StandardRecipeData.Slot.rejected(AMBIGUOUS_OUTPUT));
                continue;
            }
            GenericStack displayed = slot.getDisplayedIngredient()
                    .map(JeiRecipeExtractor::toGenericStack)
                    .orElse(null);
            converted.add(StandardRecipeData.Slot.supported(candidates, displayed));
        }
        return List.copyOf(converted);
    }

    private static GenericStack toGenericStack(ITypedIngredient<?> ingredient) {
        ItemStack itemStack = ingredient.getItemStack().orElse(ItemStack.EMPTY);
        GenericStack item = GenericStack.fromItemStack(itemStack);
        if (item != null && item.amount() > 0) {
            return item;
        }
        FluidStack fluidStack = ingredient.getIngredient(ForgeTypes.FLUID_STACK).orElse(FluidStack.EMPTY);
        GenericStack fluid = GenericStack.fromFluidStack(fluidStack);
        return fluid != null && fluid.amount() > 0 ? fluid : null;
    }
}

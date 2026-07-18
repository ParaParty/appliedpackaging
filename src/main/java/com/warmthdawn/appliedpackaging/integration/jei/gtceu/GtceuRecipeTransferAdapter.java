package com.warmthdawn.appliedpackaging.integration.jei.gtceu;

import appeng.api.stacks.GenericStack;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderFluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.pattern.AdvancedPatternTransferPlan;
import com.warmthdawn.appliedpackaging.integration.jei.AdvancedRecipeTransferAdapter;
import com.warmthdawn.appliedpackaging.integration.jei.AdvancedRecipeTransferResult;
import com.warmthdawn.appliedpackaging.integration.jei.RecipeIngredientSelector;
import com.warmthdawn.appliedpackaging.integration.jei.RecipeStackConversions;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.world.item.crafting.Ingredient;

public final class GtceuRecipeTransferAdapter implements AdvancedRecipeTransferAdapter {
    private static final String INVALID_INGREDIENT = "gui.appliedpackaging.jei_transfer.invalid_ingredient";

    @Override
    public boolean supports(Object recipe) {
        return recipe instanceof GTRecipe;
    }

    @Override
    public AdvancedRecipeTransferResult createPlan(
            Object rawRecipe,
            IRecipeSlotsView recipeSlots,
            RecipeIngredientSelector ingredientSelector) {
        GTRecipe recipe = (GTRecipe) rawRecipe;
        List<GenericStack> inputs = new ArrayList<>();
        List<GenericStack> outputs = new ArrayList<>();
        try {
            addItemContents(
                    recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of()),
                    1,
                    true,
                    inputs,
                    ingredientSelector);
            addFluidContents(
                    recipe.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of()),
                    1,
                    true,
                    inputs,
                    ingredientSelector);
            addItemContents(
                    recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of()),
                    1,
                    false,
                    outputs,
                    ingredientSelector);
            addFluidContents(
                    recipe.outputs.getOrDefault(FluidRecipeCapability.CAP, List.of()),
                    1,
                    false,
                    outputs,
                    ingredientSelector);

            if ((!recipe.tickInputs.getOrDefault(ItemRecipeCapability.CAP, List.of()).isEmpty()
                            || !recipe.tickInputs.getOrDefault(FluidRecipeCapability.CAP, List.of()).isEmpty()
                            || !recipe.tickOutputs.getOrDefault(ItemRecipeCapability.CAP, List.of()).isEmpty()
                            || !recipe.tickOutputs.getOrDefault(FluidRecipeCapability.CAP, List.of()).isEmpty())
                    && recipe.duration <= 0) {
                throw new PlanFailure("gui.appliedpackaging.jei_transfer.invalid_duration");
            }
            addItemContents(
                    recipe.tickInputs.getOrDefault(ItemRecipeCapability.CAP, List.of()),
                    recipe.duration,
                    true,
                    inputs,
                    ingredientSelector);
            addFluidContents(
                    recipe.tickInputs.getOrDefault(FluidRecipeCapability.CAP, List.of()),
                    recipe.duration,
                    true,
                    inputs,
                    ingredientSelector);
            addItemContents(
                    recipe.tickOutputs.getOrDefault(ItemRecipeCapability.CAP, List.of()),
                    recipe.duration,
                    false,
                    outputs,
                    ingredientSelector);
            addFluidContents(
                    recipe.tickOutputs.getOrDefault(FluidRecipeCapability.CAP, List.of()),
                    recipe.duration,
                    false,
                    outputs,
                    ingredientSelector);
        } catch (PlanFailure failure) {
            return AdvancedRecipeTransferResult.error(failure.translationKey);
        } catch (ArithmeticException overflow) {
            return AdvancedRecipeTransferResult.error("gui.appliedpackaging.jei_transfer.amount_overflow");
        }

        if (inputs.size() > AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS) {
            return AdvancedRecipeTransferResult.error(
                    "gui.appliedpackaging.jei_transfer.too_many_columns",
                    inputs.size(),
                    AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS);
        }
        if (outputs.size() > AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS) {
            return AdvancedRecipeTransferResult.error(
                    "gui.appliedpackaging.jei_transfer.too_many_outputs",
                    outputs.size(),
                    AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS);
        }
        try {
            return AdvancedRecipeTransferResult.success(
                    new AdvancedPatternTransferPlan(inputs.stream().map(List::of).toList(), outputs));
        } catch (IllegalArgumentException e) {
            return AdvancedRecipeTransferResult.error(INVALID_INGREDIENT);
        }
    }

    private static void addItemContents(
            List<Content> contents,
            long multiplier,
            boolean input,
            List<GenericStack> target,
            RecipeIngredientSelector ingredientSelector) {
        for (Content entry : contents) {
            if (entry.chance == 0) {
                continue;
            }
            if (entry.chance != entry.maxChance) {
                throw new PlanFailure(input
                        ? "gui.appliedpackaging.jei_transfer.random_input"
                        : "gui.appliedpackaging.jei_transfer.random_output");
            }
            if (!(entry.content instanceof Ingredient ingredient)) {
                throw new PlanFailure(INVALID_INGREDIENT);
            }
            long amountMultiplier = multiplier;
            if (ingredient instanceof IntProviderIngredient ranged) {
                int minimum = ranged.getCountProvider().getMinValue();
                int maximum = ranged.getCountProvider().getMaxValue();
                if (minimum != maximum) {
                    throw new PlanFailure("gui.appliedpackaging.jei_transfer.variable_amount");
                }
                amountMultiplier = Math.multiplyExact(amountMultiplier, minimum);
                ingredient = ranged.getInner();
            }
            GenericStack stack = input
                    ? ingredientSelector.select(ingredient)
                    : RecipeStackConversions.firstItem(ingredient);
            if (stack == null) {
                throw new PlanFailure(INVALID_INGREDIENT);
            }
            target.add(RecipeStackConversions.multiply(stack, amountMultiplier));
        }
    }

    private static void addFluidContents(
            List<Content> contents,
            long multiplier,
            boolean input,
            List<GenericStack> target,
            RecipeIngredientSelector ingredientSelector) {
        for (Content entry : contents) {
            if (entry.chance == 0) {
                continue;
            }
            if (entry.chance != entry.maxChance) {
                throw new PlanFailure(input
                        ? "gui.appliedpackaging.jei_transfer.random_input"
                        : "gui.appliedpackaging.jei_transfer.random_output");
            }
            if (!(entry.content instanceof FluidIngredient ingredient)) {
                throw new PlanFailure(INVALID_INGREDIENT);
            }
            long amountMultiplier = multiplier;
            if (ingredient instanceof IntProviderFluidIngredient ranged) {
                int minimum = ranged.getCountProvider().getMinValue();
                int maximum = ranged.getCountProvider().getMaxValue();
                if (minimum != maximum) {
                    throw new PlanFailure("gui.appliedpackaging.jei_transfer.variable_amount");
                }
                amountMultiplier = Math.multiplyExact(amountMultiplier, minimum);
                ingredient = ranged.getInner();
            }
            GenericStack stack = input
                    ? ingredientSelector.select(RecipeStackConversions.fluidCandidates(ingredient.getStacks()))
                    : RecipeStackConversions.firstFluid(ingredient.getStacks());
            if (stack == null) {
                throw new PlanFailure(INVALID_INGREDIENT);
            }
            target.add(RecipeStackConversions.multiply(stack, amountMultiplier));
        }
    }

    private static final class PlanFailure extends RuntimeException {
        private final String translationKey;

        private PlanFailure(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}

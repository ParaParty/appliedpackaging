package com.warmthdawn.appliedpackaging.integration.recipe.gtceu;

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
import com.warmthdawn.appliedpackaging.integration.recipe.AdvancedRecipeTransferResult;
import com.warmthdawn.appliedpackaging.integration.recipe.RecipeIngredientSelector;
import com.warmthdawn.appliedpackaging.integration.recipe.RecipeSemanticEncoder;
import com.warmthdawn.appliedpackaging.integration.recipe.RecipeStackConversions;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.Ingredient;

public final class GtceuRecipeSemanticEncoder implements RecipeSemanticEncoder {
    private static final String INVALID_INGREDIENT = "gui.appliedpackaging.jei_transfer.invalid_ingredient";
    private static final String UNSUPPORTED = "gui.appliedpackaging.jei_transfer.unsupported";
    private static final String LAYERED_RECIPE_HELPER =
            "com.gregtechceu.gtceu.api.recipe.LayeredRecipeHelper";
    private static final String LAYERED_STEPS_KEY = "layered_steps";
    private static final String LAYERED_XEI_KEY = "layered_xei";
    private static final String LAYERED_INFO_KEY = "layered_info";

    @Override
    public boolean supports(Object recipe) {
        return recipe instanceof GTRecipe;
    }

    @Override
    public AdvancedRecipeTransferResult createPlan(
            Object rawRecipe,
            RecipeIngredientSelector ingredientSelector) {
        GTRecipe recipe = (GTRecipe) rawRecipe;
        List<List<GenericStack>> inputColumns = new ArrayList<>();
        List<GenericStack> outputs = new ArrayList<>();
        try {
            List<GTRecipe> layeredSteps = getLayeredSteps(recipe);
            if (layeredSteps == null) {
                List<GenericStack> inputs = new ArrayList<>();
                addRecipeInputs(recipe, inputs, ingredientSelector);
                inputColumns.addAll(inputs.stream().map(List::of).toList());
            } else {
                for (GTRecipe layer : layeredSteps) {
                    List<GenericStack> layerInputs = new ArrayList<>();
                    addRecipeInputs(layer, layerInputs, ingredientSelector);
                    inputColumns.add(layerInputs);
                }
            }
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

            if ((!recipe.tickOutputs.getOrDefault(ItemRecipeCapability.CAP, List.of()).isEmpty()
                            || !recipe.tickOutputs.getOrDefault(FluidRecipeCapability.CAP, List.of()).isEmpty())
                    && recipe.duration <= 0) {
                throw new PlanFailure("gui.appliedpackaging.jei_transfer.invalid_duration");
            }
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

        if (inputColumns.size() > AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS) {
            return AdvancedRecipeTransferResult.error(
                    "gui.appliedpackaging.jei_transfer.too_many_columns",
                    inputColumns.size(),
                    AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS);
        }
        for (List<GenericStack> column : inputColumns) {
            if (column.size() > AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE) {
                return AdvancedRecipeTransferResult.error(
                        "gui.appliedpackaging.jei_transfer.too_many_inputs",
                        column.size(),
                        AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE);
            }
        }
        if (outputs.size() > AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS) {
            return AdvancedRecipeTransferResult.error(
                    "gui.appliedpackaging.jei_transfer.too_many_outputs",
                    outputs.size(),
                    AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS);
        }
        try {
            return AdvancedRecipeTransferResult.success(
                    new AdvancedPatternTransferPlan(inputColumns, outputs));
        } catch (IllegalArgumentException e) {
            return AdvancedRecipeTransferResult.error(INVALID_INGREDIENT);
        }
    }

    private static void addRecipeInputs(
            GTRecipe recipe,
            List<GenericStack> target,
            RecipeIngredientSelector ingredientSelector) {
        addItemContents(
                recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of()),
                1,
                true,
                target,
                ingredientSelector);
        addFluidContents(
                recipe.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of()),
                1,
                true,
                target,
                ingredientSelector);
        if ((!recipe.tickInputs.getOrDefault(ItemRecipeCapability.CAP, List.of()).isEmpty()
                        || !recipe.tickInputs.getOrDefault(FluidRecipeCapability.CAP, List.of()).isEmpty())
                && recipe.duration <= 0) {
            throw new PlanFailure("gui.appliedpackaging.jei_transfer.invalid_duration");
        }
        addItemContents(
                recipe.tickInputs.getOrDefault(ItemRecipeCapability.CAP, List.of()),
                recipe.duration,
                true,
                target,
                ingredientSelector);
        addFluidContents(
                recipe.tickInputs.getOrDefault(FluidRecipeCapability.CAP, List.of()),
                recipe.duration,
                true,
                target,
                ingredientSelector);
    }

    /**
     * StarT Fork exposes layered recipes as executable steps, an embedded XEI recipe, or pre-expansion
     * layered info. Reflection keeps all three fork-only representations outside the upstream GTCEu 7.5.3
     * linkage boundary.
     */
    private static List<GTRecipe> getLayeredSteps(GTRecipe recipe) {
        if (recipe.data == null
                || (!recipe.data.contains(LAYERED_STEPS_KEY)
                && !recipe.data.contains(LAYERED_XEI_KEY)
                && !recipe.data.contains(LAYERED_INFO_KEY))) {
            return null;
        }
        try {
            Class<?> helper = Class.forName(LAYERED_RECIPE_HELPER, false, recipe.getClass().getClassLoader());
            GTRecipe layeredRecipe = recipe;
            if (!recipe.data.contains(LAYERED_STEPS_KEY) && !recipe.data.contains(LAYERED_INFO_KEY)) {
                Method getXeiRecipe = helper.getMethod("getXeiLayeredRecipe", GTRecipe.class);
                Object decoded = getXeiRecipe.invoke(null, recipe);
                if (!(decoded instanceof GTRecipe gtRecipe)) {
                    throw new PlanFailure(UNSUPPORTED);
                }
                layeredRecipe = gtRecipe;
            }

            Object value;
            if (layeredRecipe.data != null && layeredRecipe.data.contains(LAYERED_STEPS_KEY)) {
                Method getSteps = helper.getMethod("getLayeredSteps", GTRecipe.class);
                value = getSteps.invoke(null, layeredRecipe);
            } else if (layeredRecipe.data != null && layeredRecipe.data.contains(LAYERED_INFO_KEY)) {
                Method calculateSteps = helper.getMethod("calculateRecipeSteps", GTRecipe.class);
                value = calculateSteps.invoke(null, layeredRecipe);
            } else {
                throw new PlanFailure(UNSUPPORTED);
            }
            if (!(value instanceof List<?> rawSteps) || rawSteps.isEmpty()) {
                throw new PlanFailure(UNSUPPORTED);
            }
            List<GTRecipe> steps = new ArrayList<>(rawSteps.size());
            for (Object step : rawSteps) {
                if (!(step instanceof GTRecipe gtRecipe)) {
                    throw new PlanFailure(UNSUPPORTED);
                }
                steps.add(gtRecipe);
            }
            return List.copyOf(steps);
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new PlanFailure(UNSUPPORTED);
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

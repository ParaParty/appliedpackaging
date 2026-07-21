package com.warmthdawn.appliedpackaging.integration.recipe.create;

import appeng.api.stacks.GenericStack;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.pattern.AdvancedPatternTransferPlan;
import com.warmthdawn.appliedpackaging.integration.recipe.AdvancedRecipeTransferResult;
import com.warmthdawn.appliedpackaging.integration.recipe.RecipeIngredientSelector;
import com.warmthdawn.appliedpackaging.integration.recipe.RecipeSemanticEncoder;
import com.warmthdawn.appliedpackaging.integration.recipe.RecipeStackConversions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

public final class CreateRecipeSemanticEncoder implements RecipeSemanticEncoder {
    private static final String INVALID_INGREDIENT = "gui.appliedpackaging.jei_transfer.invalid_ingredient";
    private static final String RANDOM_OUTPUT = "gui.appliedpackaging.jei_transfer.random_output";

    @Override
    public boolean supports(Object recipe) {
        return recipe instanceof SequencedAssemblyRecipe
                || recipe instanceof MechanicalCraftingRecipe
                || recipe instanceof ProcessingRecipe<?>;
    }

    @Override
    public AdvancedRecipeTransferResult createPlan(
            Object recipe,
            RecipeIngredientSelector ingredientSelector) {
        if (recipe instanceof SequencedAssemblyRecipe sequencedAssembly) {
            return createSequencedPlan(sequencedAssembly, ingredientSelector);
        }
        if (recipe instanceof MechanicalCraftingRecipe mechanicalCrafting) {
            return createMechanicalCraftingPlan(mechanicalCrafting, ingredientSelector);
        }
        return createProcessingPlan((ProcessingRecipe<?>) recipe, ingredientSelector);
    }

    private AdvancedRecipeTransferResult createMechanicalCraftingPlan(
            MechanicalCraftingRecipe recipe,
            RecipeIngredientSelector ingredientSelector) {
        int width = recipe.getWidth();
        int height = recipe.getHeight();
        List<Ingredient> ingredients = recipe.getIngredients();
        boolean[] nonEmptyRows = new boolean[height];
        boolean[] nonEmptyColumns = new boolean[width];
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                Ingredient ingredient = ingredients.get(row * width + column);
                if (!ingredient.isEmpty()) {
                    nonEmptyRows[row] = true;
                    nonEmptyColumns[column] = true;
                }
            }
        }
        int rowCount = countTrue(nonEmptyRows);
        int columnCount = countTrue(nonEmptyColumns);
        boolean splitByRows = rowCount <= columnCount;

        List<List<GenericStack>> columns = new ArrayList<>();
        int groups = splitByRows ? height : width;
        int positions = splitByRows ? width : height;
        for (int group = 0; group < groups; group++) {
            List<GenericStack> packageInputs = new ArrayList<>(positions);
            for (int position = 0; position < positions; position++) {
                int row = splitByRows ? group : position;
                int column = splitByRows ? position : group;
                Ingredient ingredient = ingredients.get(row * width + column);
                if (ingredient.isEmpty()) {
                    packageInputs.add(null);
                    continue;
                }
                GenericStack stack = ingredientSelector.select(ingredient);
                if (stack == null) {
                    return AdvancedRecipeTransferResult.error(INVALID_INGREDIENT);
                }
                packageInputs.add(stack);
            }
            trimTrailingNulls(packageInputs);
            if (!packageInputs.isEmpty()) {
                columns.add(Collections.unmodifiableList(packageInputs));
            }
        }

        GenericStack output = GenericStack.fromItemStack(recipe.getResultItem(RegistryAccess.EMPTY));
        if (output == null) {
            return AdvancedRecipeTransferResult.error(INVALID_INGREDIENT);
        }
        return finish(columns, List.of(output));
    }

    private static void trimTrailingNulls(List<GenericStack> stacks) {
        while (!stacks.isEmpty() && stacks.get(stacks.size() - 1) == null) {
            stacks.remove(stacks.size() - 1);
        }
    }

    private static int countTrue(boolean[] values) {
        int count = 0;
        for (boolean value : values) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    private AdvancedRecipeTransferResult createSequencedPlan(
            SequencedAssemblyRecipe recipe,
            RecipeIngredientSelector ingredientSelector) {
        if (recipe.resultPool.size() != 1
                || Float.compare(recipe.resultPool.get(0).getChance(), 1.0f) != 0) {
            return AdvancedRecipeTransferResult.error(RANDOM_OUTPUT);
        }
        GenericStack initialInput = ingredientSelector.select(recipe.getIngredient());
        GenericStack output = GenericStack.fromItemStack(recipe.resultPool.get(0).getStack());
        if (initialInput == null || output == null) {
            return AdvancedRecipeTransferResult.error(INVALID_INGREDIENT);
        }

        List<List<GenericStack>> columns = new ArrayList<>();
        columns.add(List.of(initialInput));
        for (int loop = 0; loop < recipe.getLoops(); loop++) {
            for (var sequencedRecipe : recipe.getSequence()) {
                ProcessingRecipe<?> processingRecipe = sequencedRecipe.getRecipe();
                IAssemblyRecipe assemblyRecipe = sequencedRecipe.getAsAssemblyRecipe();
                List<Ingredient> itemIngredients = new ArrayList<>();
                if (!(processingRecipe instanceof ItemApplicationRecipe itemApplication
                        && itemApplication.shouldKeepHeldItem())) {
                    assemblyRecipe.addAssemblyIngredients(itemIngredients);
                }
                List<com.simibubi.create.foundation.fluid.FluidIngredient> fluidIngredients = new ArrayList<>();
                assemblyRecipe.addAssemblyFluidIngredients(fluidIngredients);

                List<GenericStack> column = new ArrayList<>();
                for (Ingredient ingredient : itemIngredients) {
                    GenericStack stack = ingredientSelector.select(ingredient);
                    if (stack == null) {
                        return AdvancedRecipeTransferResult.error(INVALID_INGREDIENT);
                    }
                    column.add(stack);
                }
                for (var ingredient : fluidIngredients) {
                    GenericStack stack = ingredientSelector.select(
                            RecipeStackConversions.fluidCandidates(ingredient.getMatchingFluidStacks()));
                    if (stack == null) {
                        return AdvancedRecipeTransferResult.error(INVALID_INGREDIENT);
                    }
                    column.add(stack);
                }
                if (!column.isEmpty()) {
                    columns.add(List.copyOf(column));
                }
            }
        }
        return finish(columns, List.of(output));
    }

    private AdvancedRecipeTransferResult createProcessingPlan(
            ProcessingRecipe<?> recipe,
            RecipeIngredientSelector ingredientSelector) {
        List<GenericStack> inputs = new ArrayList<>();
        List<Ingredient> itemIngredients = recipe.getIngredients();
        boolean keepsTool = recipe instanceof ItemApplicationRecipe itemApplication
                && itemApplication.shouldKeepHeldItem();
        for (int index = 0; index < itemIngredients.size(); index++) {
            if (keepsTool && index == 1) {
                continue;
            }
            GenericStack stack = ingredientSelector.select(itemIngredients.get(index));
            if (stack == null) {
                return AdvancedRecipeTransferResult.error(INVALID_INGREDIENT);
            }
            inputs.add(stack);
        }
        for (var ingredient : recipe.getFluidIngredients()) {
            GenericStack stack = ingredientSelector.select(
                    RecipeStackConversions.fluidCandidates(ingredient.getMatchingFluidStacks()));
            if (stack == null) {
                return AdvancedRecipeTransferResult.error(INVALID_INGREDIENT);
            }
            inputs.add(stack);
        }

        List<GenericStack> outputs = new ArrayList<>();
        for (ProcessingOutput result : recipe.getRollableResults()) {
            if (Float.compare(result.getChance(), 1.0f) != 0) {
                return AdvancedRecipeTransferResult.error(RANDOM_OUTPUT);
            }
            ItemStack stack = result.getStack();
            GenericStack genericStack = GenericStack.fromItemStack(stack);
            if (genericStack == null) {
                return AdvancedRecipeTransferResult.error(INVALID_INGREDIENT);
            }
            outputs.add(genericStack);
        }
        for (FluidStack result : recipe.getFluidResults()) {
            GenericStack stack = GenericStack.fromFluidStack(result);
            if (stack == null) {
                return AdvancedRecipeTransferResult.error(INVALID_INGREDIENT);
            }
            outputs.add(stack);
        }
        return finish(inputs.stream().map(List::of).toList(), outputs);
    }

    private static AdvancedRecipeTransferResult finish(
            List<List<GenericStack>> columns,
            List<GenericStack> outputs) {
        if (columns.size() > AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS) {
            return AdvancedRecipeTransferResult.error(
                    "gui.appliedpackaging.jei_transfer.too_many_columns",
                    columns.size(),
                    AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS);
        }
        for (List<GenericStack> column : columns) {
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
            return AdvancedRecipeTransferResult.success(new AdvancedPatternTransferPlan(columns, outputs));
        } catch (IllegalArgumentException e) {
            return AdvancedRecipeTransferResult.error(INVALID_INGREDIENT);
        }
    }
}

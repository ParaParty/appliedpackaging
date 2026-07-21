package com.warmthdawn.appliedpackaging.integration.recipe;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.pattern.AdvancedPatternTransferPlan;
import com.warmthdawn.appliedpackaging.core.pattern.PackagePatternTransferPlan;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Builds transfer plans from frontend-extracted standard item/fluid recipe data. */
public final class StandardRecipePlanFactory {
    private static final String INVALID_INGREDIENT = "gui.appliedpackaging.jei_transfer.invalid_ingredient";
    private static final String UNSUPPORTED_INGREDIENT =
            "gui.appliedpackaging.jei_transfer.unsupported_ingredient_type";
    private static final String AMBIGUOUS_OUTPUT = "gui.appliedpackaging.jei_transfer.ambiguous_output";
    private static final String NO_OUTPUT = "gui.appliedpackaging.jei_transfer.no_output";

    public AdvancedRecipeTransferResult createAdvancedPlan(
            Object recipe,
            StandardRecipeData recipeData,
            RecipeIngredientSelector ingredientSelector) {
        ParseResult parsed = parse(recipe, recipeData, ingredientSelector);
        if (parsed.error() != null) {
            return new AdvancedRecipeTransferResult(null, parsed.error());
        }
        if (parsed.outputs().isEmpty()) {
            return AdvancedRecipeTransferResult.error(NO_OUTPUT);
        }
        if (parsed.inputs().size() > AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS) {
            return AdvancedRecipeTransferResult.error(
                    "gui.appliedpackaging.jei_transfer.too_many_columns",
                    parsed.inputs().size(),
                    AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS);
        }
        if (parsed.outputs().size() > AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS) {
            return AdvancedRecipeTransferResult.error(
                    "gui.appliedpackaging.jei_transfer.too_many_outputs",
                    parsed.outputs().size(),
                    AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS);
        }
        try {
            return AdvancedRecipeTransferResult.success(
                    new AdvancedPatternTransferPlan(
                            parsed.inputs().stream().map(List::of).toList(),
                            parsed.outputs()));
        } catch (IllegalArgumentException e) {
            return AdvancedRecipeTransferResult.error(INVALID_INGREDIENT);
        }
    }

    public PackageRecipeTransferResult createPackagePlan(Object recipe, StandardRecipeData recipeData) {
        return createPackagePlan(recipe, recipeData, RecipeIngredientSelector.empty());
    }

    public PackageRecipeTransferResult createPackagePlan(
            Object recipe,
            StandardRecipeData recipeData,
            RecipeIngredientSelector ingredientSelector) {
        ParseResult parsed = parse(recipe, recipeData, ingredientSelector);
        if (parsed.error() != null) {
            return new PackageRecipeTransferResult(null, parsed.error());
        }
        if (parsed.inputs().size() > PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT) {
            return PackageRecipeTransferResult.error(
                    "gui.appliedpackaging.jei_transfer.too_many_inputs",
                    parsed.inputs().size(),
                    PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT);
        }
        GenericStack marker = parsed.outputs().stream()
                .filter(stack -> stack.what() instanceof AEItemKey)
                .findFirst()
                .orElse(null);
        try {
            return PackageRecipeTransferResult.success(
                    new PackagePatternTransferPlan(parsed.inputs(), marker));
        } catch (IllegalArgumentException e) {
            return PackageRecipeTransferResult.error(INVALID_INGREDIENT);
        }
    }

    private static ParseResult parse(
            Object recipe,
            StandardRecipeData recipeData,
            RecipeIngredientSelector ingredientSelector) {
        String rejectionKey = RecipeTransferSemantics.rejectionKey(recipe);
        if (rejectionKey != null) {
            return ParseResult.error(rejectionKey);
        }
        if (recipeData == null) {
            return ParseResult.error(INVALID_INGREDIENT);
        }

        Object semanticRecipe = ThermalRecipeSemantics.unwrap(recipe);
        ThermalRecipeSemantics.InputCounts thermalInputs =
                ThermalRecipeSemantics.consumableInputCounts(semanticRecipe);
        int thermalItemInputs = 0;
        int thermalFluidInputs = 0;
        List<GenericStack> inputs = new ArrayList<>();
        for (StandardRecipeData.Slot slot : recipeData.inputs()) {
            SlotResult converted = convertInput(slot, ingredientSelector);
            if (converted.errorKey() != null) {
                return ParseResult.error(converted.errorKey());
            }
            if (converted.stack() != null) {
                if (thermalInputs != null) {
                    if (converted.stack().what() instanceof AEItemKey) {
                        if (thermalItemInputs >= thermalInputs.items()) {
                            continue;
                        }
                        thermalItemInputs++;
                    } else if (converted.stack().what() instanceof AEFluidKey) {
                        if (thermalFluidInputs >= thermalInputs.fluids()) {
                            continue;
                        }
                        thermalFluidInputs++;
                    }
                }
                inputs.add(converted.stack());
            }
        }
        if (thermalInputs != null) {
            if (thermalItemInputs != thermalInputs.items()
                    || thermalFluidInputs != thermalInputs.fluids()) {
                return ParseResult.error(INVALID_INGREDIENT);
            }
        }
        if (inputs.isEmpty()) {
            return ParseResult.error(INVALID_INGREDIENT);
        }

        List<GenericStack> outputs = new ArrayList<>();
        for (StandardRecipeData.Slot slot : recipeData.outputs()) {
            SlotResult converted = convertOutput(slot);
            if (converted.errorKey() != null) {
                return ParseResult.error(converted.errorKey());
            }
            if (converted.stack() != null) {
                outputs.add(converted.stack());
            }
        }
        return ParseResult.success(List.copyOf(inputs), List.copyOf(outputs));
    }

    private static SlotResult convertInput(
            StandardRecipeData.Slot slot,
            RecipeIngredientSelector ingredientSelector) {
        if (slot.errorKey() != null) {
            return SlotResult.error(slot.errorKey());
        }
        if (slot.candidates().isEmpty()) {
            return SlotResult.empty();
        }
        GenericStack selected = ingredientSelector.select(slot.candidates(), slot.displayed());
        if (selected != null) {
            return SlotResult.success(selected);
        }
        return SlotResult.error(UNSUPPORTED_INGREDIENT);
    }

    private static SlotResult convertOutput(StandardRecipeData.Slot slot) {
        if (slot.errorKey() != null) {
            return SlotResult.error(slot.errorKey());
        }
        if (slot.candidates().isEmpty()) {
            return SlotResult.empty();
        }
        LinkedHashSet<GenericStack> candidates = new LinkedHashSet<>(slot.candidates());
        if (candidates.isEmpty()) {
            return SlotResult.error(UNSUPPORTED_INGREDIENT);
        }
        if (candidates.size() != 1) {
            return SlotResult.error(AMBIGUOUS_OUTPUT);
        }
        return SlotResult.success(candidates.iterator().next());
    }

    private record ParseResult(List<GenericStack> inputs, List<GenericStack> outputs, Component error) {
        private static ParseResult success(List<GenericStack> inputs, List<GenericStack> outputs) {
            return new ParseResult(inputs, outputs, null);
        }

        private static ParseResult error(String key) {
            return new ParseResult(List.of(), List.of(), Component.translatable(key));
        }
    }

    private record SlotResult(GenericStack stack, String errorKey) {
        private static SlotResult success(GenericStack stack) {
            return new SlotResult(stack, null);
        }

        private static SlotResult empty() {
            return new SlotResult(null, null);
        }

        private static SlotResult error(String key) {
            return new SlotResult(null, key);
        }
    }

}

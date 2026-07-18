package com.warmthdawn.appliedpackaging.integration.jei;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.pattern.AdvancedPatternTransferPlan;
import com.warmthdawn.appliedpackaging.core.pattern.PackagePatternTransferPlan;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** JEI-role based fallback used for mods that expose normal item/fluid recipe slots. */
public final class StandardRecipeTransferAdapter implements AdvancedRecipeTransferAdapter {
    private static final String INVALID_INGREDIENT = "gui.appliedpackaging.jei_transfer.invalid_ingredient";
    private static final String UNSUPPORTED_INGREDIENT =
            "gui.appliedpackaging.jei_transfer.unsupported_ingredient_type";
    private static final String AMBIGUOUS_OUTPUT = "gui.appliedpackaging.jei_transfer.ambiguous_output";
    private static final String NO_OUTPUT = "gui.appliedpackaging.jei_transfer.no_output";

    @Override
    public boolean supports(Object recipe) {
        return recipe != null;
    }

    @Override
    public AdvancedRecipeTransferResult createPlan(
            Object recipe,
            IRecipeSlotsView recipeSlots,
            RecipeIngredientSelector ingredientSelector) {
        ParseResult parsed = parse(recipe, recipeSlots, ingredientSelector);
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

    public PackageRecipeTransferResult createPackagePlan(Object recipe, IRecipeSlotsView recipeSlots) {
        return createPackagePlan(recipe, recipeSlots, RecipeIngredientSelector.empty());
    }

    public PackageRecipeTransferResult createPackagePlan(
            Object recipe,
            IRecipeSlotsView recipeSlots,
            RecipeIngredientSelector ingredientSelector) {
        ParseResult parsed = parse(recipe, recipeSlots, ingredientSelector);
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
            IRecipeSlotsView recipeSlots,
            RecipeIngredientSelector ingredientSelector) {
        String rejectionKey = RecipeTransferSemantics.rejectionKey(recipe);
        if (rejectionKey != null) {
            return ParseResult.error(rejectionKey);
        }
        if (recipeSlots == null) {
            return ParseResult.error(INVALID_INGREDIENT);
        }

        Object semanticRecipe = unwrapRecipe(recipe);
        ThermalInputCounts thermalInputs = thermalConsumableInputCounts(semanticRecipe);
        int thermalItemInputs = 0;
        int thermalFluidInputs = 0;
        List<GenericStack> inputs = new ArrayList<>();
        for (IRecipeSlotView slot : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
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
        for (IRecipeSlotView slot : recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT)) {
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
            IRecipeSlotView slot,
            RecipeIngredientSelector ingredientSelector) {
        List<ITypedIngredient<?>> ingredients = slot.getAllIngredients().toList();
        if (ingredients.isEmpty()) {
            return SlotResult.empty();
        }
        GenericStack displayed = slot.getDisplayedIngredient()
                .map(StandardRecipeTransferAdapter::toGenericStack)
                .orElse(null);
        List<GenericStack> candidates = new ArrayList<>();
        for (ITypedIngredient<?> ingredient : ingredients) {
            GenericStack stack = toGenericStack(ingredient);
            if (stack != null) {
                candidates.add(stack);
            }
        }
        GenericStack selected = ingredientSelector.select(candidates, displayed);
        if (selected != null) {
            return SlotResult.success(selected);
        }
        return SlotResult.error(UNSUPPORTED_INGREDIENT);
    }

    private static SlotResult convertOutput(IRecipeSlotView slot) {
        List<ITypedIngredient<?>> ingredients = slot.getAllIngredients().toList();
        if (ingredients.isEmpty()) {
            return SlotResult.empty();
        }
        LinkedHashSet<GenericStack> candidates = new LinkedHashSet<>();
        boolean unsupported = false;
        for (ITypedIngredient<?> ingredient : ingredients) {
            GenericStack stack = toGenericStack(ingredient);
            if (stack == null) {
                unsupported = true;
            } else {
                candidates.add(stack);
            }
        }
        if (candidates.isEmpty()) {
            return SlotResult.error(UNSUPPORTED_INGREDIENT);
        }
        if (unsupported || candidates.size() != 1) {
            return SlotResult.error(AMBIGUOUS_OUTPUT);
        }
        return SlotResult.success(candidates.iterator().next());
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

    private static ThermalInputCounts thermalConsumableInputCounts(Object recipe) {
        if (recipe == null || !recipe.getClass().getName().startsWith("cofh.thermal.")) {
            return null;
        }
        try {
            return new ThermalInputCounts(
                    collectionSize(recipe, "getInputItems"),
                    collectionSize(recipe, "getInputFluids"));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static Object unwrapRecipe(Object recipe) {
        if (recipe == null || !recipe.getClass().getName().startsWith("cofh.thermal.")) {
            return recipe;
        }
        try {
            Method valueMethod = recipe.getClass().getMethod("value");
            Object value = valueMethod.invoke(recipe);
            return value != null && value != recipe ? value : recipe;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return recipe;
        }
    }

    private static int collectionSize(Object target, String methodName) throws ReflectiveOperationException {
        Method method;
        try {
            method = target.getClass().getMethod(methodName);
        } catch (NoSuchMethodException ignored) {
            return 0;
        }
        try {
            Object value = method.invoke(target);
            return value instanceof Collection<?> collection ? collection.size() : 0;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
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

    private record ThermalInputCounts(int items, int fluids) {
    }
}

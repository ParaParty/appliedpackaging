package com.warmthdawn.appliedpackaging.integration.emi;

import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.integration.recipe.RecipeExtraction;
import com.warmthdawn.appliedpackaging.integration.recipe.StandardRecipeData;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/** Extracts native EMI lifecycle data without retaining EMI objects in the domain layer. */
public final class EmiRecipeExtractor {
    private static final String UNSUPPORTED_INGREDIENT =
            "gui.appliedpackaging.jei_transfer.unsupported_ingredient_type";
    private static final String RANDOM_INPUT = "gui.appliedpackaging.jei_transfer.random_input";
    private static final String RANDOM_OUTPUT = "gui.appliedpackaging.jei_transfer.random_output";

    private EmiRecipeExtractor() {
    }

    public static RecipeExtraction extract(EmiRecipe recipe, Level level) {
        return new RecipeExtraction(
                EmiRecipeResolver.resolve(recipe, level),
                extractStandardData(recipe));
    }

    /** JEMI assigns the public {@code jei} namespace to recipes bridged from JEI. */
    public static boolean isJeiBridgeRecipe(EmiRecipe recipe) {
        ResourceLocation id = recipe == null ? null : recipe.getId();
        return id != null && "jei".equals(id.getNamespace());
    }

    private static StandardRecipeData extractStandardData(EmiRecipe recipe) {
        List<StandardRecipeData.Slot> inputs = new ArrayList<>();
        for (EmiIngredient ingredient : recipe.getInputs()) {
            inputs.add(convertIngredient(ingredient));
        }

        List<StandardRecipeData.Slot> outputs = new ArrayList<>();
        for (EmiStack stack : recipe.getOutputs()) {
            outputs.add(convertOutput(stack));
        }
        return new StandardRecipeData(inputs, outputs);
    }

    private static StandardRecipeData.Slot convertIngredient(EmiIngredient ingredient) {
        if (Float.compare(ingredient.getChance(), 1.0f) != 0) {
            return StandardRecipeData.Slot.rejected(RANDOM_INPUT);
        }
        if (ingredient.isEmpty()) {
            return StandardRecipeData.Slot.supported(List.of(), null);
        }
        long amount = ingredient.getAmount();
        List<GenericStack> candidates = ingredient.getEmiStacks().stream()
                .map(stack -> toGenericStack(stack, amount))
                .filter(java.util.Objects::nonNull)
                .toList();
        if (candidates.isEmpty()) {
            return StandardRecipeData.Slot.rejected(UNSUPPORTED_INGREDIENT);
        }
        return StandardRecipeData.Slot.supported(candidates, candidates.get(0));
    }

    private static StandardRecipeData.Slot convertOutput(EmiStack stack) {
        if (Float.compare(stack.getChance(), 1.0f) != 0) {
            return StandardRecipeData.Slot.rejected(RANDOM_OUTPUT);
        }
        GenericStack converted = toGenericStack(stack, stack.getAmount());
        return converted == null
                ? StandardRecipeData.Slot.rejected(UNSUPPORTED_INGREDIENT)
                : StandardRecipeData.Slot.supported(List.of(converted), converted);
    }

    private static GenericStack toGenericStack(EmiStack stack, long amount) {
        if (stack == null || stack.isEmpty() || amount <= 0) {
            return null;
        }
        GenericStack converted;
        if (stack.getKey() instanceof Item item) {
            ItemStack itemStack = new ItemStack(item);
            itemStack.setTag(stack.getNbt());
            converted = GenericStack.fromItemStack(itemStack);
        } else if (stack.getKey() instanceof Fluid fluid) {
            FluidStack fluidStack = new FluidStack(fluid, 1);
            fluidStack.setTag(stack.getNbt());
            converted = GenericStack.fromFluidStack(fluidStack);
        } else {
            return null;
        }
        return converted == null ? null : new GenericStack(converted.what(), amount);
    }
}

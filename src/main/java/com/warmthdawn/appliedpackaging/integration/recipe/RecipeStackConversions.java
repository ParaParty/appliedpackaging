package com.warmthdawn.appliedpackaging.integration.recipe;

import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

public final class RecipeStackConversions {
    private RecipeStackConversions() {
    }

    public static GenericStack firstItem(Ingredient ingredient) {
        return itemCandidates(ingredient).stream().findFirst().orElse(null);
    }

    public static List<GenericStack> itemCandidates(Ingredient ingredient) {
        if (ingredient == null) {
            return List.of();
        }
        List<GenericStack> result = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            GenericStack genericStack = GenericStack.fromItemStack(stack);
            if (genericStack != null && genericStack.amount() > 0) {
                result.add(genericStack);
            }
        }
        return List.copyOf(result);
    }

    public static GenericStack firstFluid(List<FluidStack> stacks) {
        return fluidCandidates(stacks).stream().findFirst().orElse(null);
    }

    public static List<GenericStack> fluidCandidates(List<FluidStack> stacks) {
        if (stacks == null) {
            return List.of();
        }
        List<GenericStack> result = new ArrayList<>();
        for (FluidStack stack : stacks) {
            GenericStack genericStack = GenericStack.fromFluidStack(stack);
            if (genericStack != null && genericStack.amount() > 0) {
                result.add(genericStack);
            }
        }
        return List.copyOf(result);
    }

    public static GenericStack firstFluid(FluidStack[] stacks) {
        return stacks == null ? null : firstFluid(List.of(stacks));
    }

    public static List<GenericStack> fluidCandidates(FluidStack[] stacks) {
        return stacks == null ? List.of() : fluidCandidates(List.of(stacks));
    }

    public static GenericStack multiply(GenericStack stack, long multiplier) {
        return new GenericStack(stack.what(), Math.multiplyExact(stack.amount(), multiplier));
    }
}

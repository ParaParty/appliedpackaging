package com.warmthdawn.appliedpackaging.integration.jei;

import appeng.api.stacks.GenericStack;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

public final class RecipeStackConversions {
    private RecipeStackConversions() {
    }

    public static GenericStack firstItem(Ingredient ingredient) {
        if (ingredient == null) {
            return null;
        }
        for (ItemStack stack : ingredient.getItems()) {
            GenericStack genericStack = GenericStack.fromItemStack(stack);
            if (genericStack != null && genericStack.amount() > 0) {
                return genericStack;
            }
        }
        return null;
    }

    public static GenericStack firstFluid(List<FluidStack> stacks) {
        if (stacks == null) {
            return null;
        }
        for (FluidStack stack : stacks) {
            GenericStack genericStack = GenericStack.fromFluidStack(stack);
            if (genericStack != null && genericStack.amount() > 0) {
                return genericStack;
            }
        }
        return null;
    }

    public static GenericStack firstFluid(FluidStack[] stacks) {
        return stacks == null ? null : firstFluid(List.of(stacks));
    }

    public static GenericStack multiply(GenericStack stack, long multiplier) {
        return new GenericStack(stack.what(), Math.multiplyExact(stack.amount(), multiplier));
    }
}

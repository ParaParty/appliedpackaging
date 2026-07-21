package com.warmthdawn.appliedpackaging.integration.emi;

import com.warmthdawn.appliedpackaging.integration.recipe.PatternTransferPlanFactory;
import com.warmthdawn.appliedpackaging.integration.recipe.RecipeTransferPlanResult;
import com.warmthdawn.appliedpackaging.world.menu.AdvancedPatternEncodingTermMenu;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/**
 * Thin native EMI lifecycle handler. It extracts {@link EmiRecipe} data for the
 * shared plan factory and writes only from the fill-button context; JEI-bridged
 * displays remain on EMI's built-in JEMI transfer path.
 */
public final class AdvancedEmiRecipeHandler
        implements EmiRecipeHandler<AdvancedPatternEncodingTermMenu> {
    private final PatternTransferPlanFactory planFactory = new PatternTransferPlanFactory();

    @Override
    public EmiPlayerInventory getInventory(
            AbstractContainerScreen<AdvancedPatternEncodingTermMenu> screen) {
        // Encoding a pattern never consumes the player's current ingredients.
        return new EmiPlayerInventory(List.of());
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe != null
                && !EmiRecipeExtractor.isJeiBridgeRecipe(recipe)
                && !recipe.getInputs().isEmpty()
                && !recipe.getOutputs().isEmpty();
    }

    @Override
    public boolean canCraft(
            EmiRecipe recipe,
            EmiCraftContext<AdvancedPatternEncodingTermMenu> context) {
        return isFillButton(context) && plan(recipe, context).successful();
    }

    @Override
    public boolean craft(
            EmiRecipe recipe,
            EmiCraftContext<AdvancedPatternEncodingTermMenu> context) {
        if (!isFillButton(context)) {
            return false;
        }
        RecipeTransferPlanResult result = plan(recipe, context);
        if (!result.successful()) {
            return false;
        }
        AdvancedPatternEncodingTermMenu menu = context.getScreenHandler();
        if (result.advancedPlan() != null) {
            menu.importAdvancedRecipe(result.advancedPlan());
        } else if (result.packagePlan() != null) {
            menu.importPackageRecipe(result.packagePlan());
        } else {
            return false;
        }
        return true;
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(
            EmiRecipe recipe,
            EmiCraftContext<AdvancedPatternEncodingTermMenu> context) {
        if (!isFillButton(context)) {
            return List.of();
        }
        RecipeTransferPlanResult result = plan(recipe, context);
        if (result.error() == null) {
            return List.of();
        }
        return List.of(ClientTooltipComponent.create(result.error().getVisualOrderText()));
    }

    private RecipeTransferPlanResult plan(
            EmiRecipe recipe,
            EmiCraftContext<AdvancedPatternEncodingTermMenu> context) {
        return planFactory.create(
                context.getScreenHandler(),
                EmiRecipeExtractor.extract(
                        recipe,
                        context.getScreenHandler().getPlayer().level()));
    }

    private static boolean isFillButton(
            EmiCraftContext<AdvancedPatternEncodingTermMenu> context) {
        return context.getType() == EmiCraftContext.Type.FILL_BUTTON;
    }
}

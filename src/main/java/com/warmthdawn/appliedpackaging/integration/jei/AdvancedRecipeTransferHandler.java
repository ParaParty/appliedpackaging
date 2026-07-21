package com.warmthdawn.appliedpackaging.integration.jei;

import com.warmthdawn.appliedpackaging.integration.recipe.PatternTransferPlanFactory;
import com.warmthdawn.appliedpackaging.integration.recipe.RecipeTransferPlanResult;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.world.menu.AdvancedPatternEncodingTermMenu;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

/** Thin JEI lifecycle handler that converts {@link IRecipeSlotsView} into a shared menu plan. */
public final class AdvancedRecipeTransferHandler
        implements IUniversalRecipeTransferHandler<AdvancedPatternEncodingTermMenu> {
    private final IRecipeTransferHandlerHelper helper;
    private final PatternTransferPlanFactory planFactory;

    public AdvancedRecipeTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
        this.planFactory = new PatternTransferPlanFactory();
    }

    @Override
    public Class<? extends AdvancedPatternEncodingTermMenu> getContainerClass() {
        return AdvancedPatternEncodingTermMenu.class;
    }

    @Override
    public Optional<MenuType<AdvancedPatternEncodingTermMenu>> getMenuType() {
        return Optional.of(APMenus.ADVANCED_PATTERN_ENCODING_TERMINAL.get());
    }

    @Override
    public IRecipeTransferError transferRecipe(
            AdvancedPatternEncodingTermMenu container,
            Object recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {
        RecipeTransferPlanResult result = planFactory.create(
                container,
                JeiRecipeExtractor.extract(recipe, recipeSlots));
        if (result.internalError()) {
            return helper.createInternalError();
        }
        if (result.error() != null) {
            return helper.createUserErrorWithTooltip(result.error());
        }
        if (doTransfer) {
            if (result.advancedPlan() != null) {
                container.importAdvancedRecipe(result.advancedPlan());
            } else if (result.packagePlan() != null) {
                container.importPackageRecipe(result.packagePlan());
            } else {
                return helper.createInternalError();
            }
        }
        return null;
    }
}

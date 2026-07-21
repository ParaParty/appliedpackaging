package com.warmthdawn.appliedpackaging.integration.recipe;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.pattern.AdvancedPatternTransferPlan;
import com.warmthdawn.appliedpackaging.core.pattern.PackagePatternTransferPlan;
import com.warmthdawn.appliedpackaging.integration.recipe.create.CreateRecipeSemanticEncoder;
import com.warmthdawn.appliedpackaging.integration.recipe.gtceu.GtceuRecipeSemanticEncoder;
import com.warmthdawn.appliedpackaging.part.SpecializedPatternMode;
import com.warmthdawn.appliedpackaging.world.menu.AdvancedPatternEncodingTermMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fml.ModList;

/**
 * The only shared JEI/EMI transfer boundary: builds validated menu plans from
 * viewer-extracted domain data. This class does not depend on viewer APIs or
 * discover integration modules through reflection.
 */
public final class PatternTransferPlanFactory {
    private final List<RecipeSemanticEncoder> semanticEncoders;
    private final StandardRecipePlanFactory standardFactory = new StandardRecipePlanFactory();

    public PatternTransferPlanFactory() {
        this.semanticEncoders = loadEncoders();
    }

    public RecipeTransferPlanResult create(
            AdvancedPatternEncodingTermMenu menu,
            RecipeExtraction extraction) {
        Object recipe = extraction.semanticRecipe();
        RecipeSemanticEncoder semanticEncoder = semanticEncoders.stream()
                .filter(candidate -> candidate.supports(recipe))
                .findFirst()
                .orElse(null);
        RecipeIngredientSelector ingredientSelector = RecipeIngredientSelector.fromMenu(menu);
        try {
            if (menu.getSpecializedMode() == SpecializedPatternMode.PACKAGE) {
                return createPackage(extraction, semanticEncoder, ingredientSelector);
            }
            AdvancedRecipeTransferResult result = semanticEncoder == null
                    ? standardFactory.createAdvancedPlan(
                            recipe,
                            extraction.standardData(),
                            ingredientSelector)
                    : semanticEncoder.createPlan(recipe, ingredientSelector);
            if (result.error() != null) {
                return RecipeTransferPlanResult.userError(result.error());
            }
            if (result.plan() == null) {
                return RecipeTransferPlanResult.unexpectedError();
            }
            if (payloadTooLarge(result.plan())) {
                return RecipeTransferPlanResult.userError(
                        "gui.appliedpackaging.jei_transfer.payload_too_large");
            }
            return RecipeTransferPlanResult.advanced(result.plan());
        } catch (RuntimeException e) {
            AppliedPackaging.LOGGER.warn("Failed to create an advanced-pattern recipe transfer plan for {}", recipe, e);
            return RecipeTransferPlanResult.unexpectedError();
        }
    }

    private RecipeTransferPlanResult createPackage(
            RecipeExtraction extraction,
            RecipeSemanticEncoder semanticEncoder,
            RecipeIngredientSelector ingredientSelector) {
        Object recipe = extraction.semanticRecipe();
        PackageRecipeTransferResult result;
        if (semanticEncoder == null) {
            result = standardFactory.createPackagePlan(
                    recipe,
                    extraction.standardData(),
                    ingredientSelector);
        } else {
            AdvancedRecipeTransferResult advanced = semanticEncoder.createPlan(
                    recipe,
                    ingredientSelector);
            if (advanced.error() != null) {
                result = new PackageRecipeTransferResult(null, advanced.error());
            } else if (advanced.plan() == null) {
                return RecipeTransferPlanResult.unexpectedError();
            } else {
                result = PackageRecipeTransferResult.success(
                        PackagePatternTransferPlan.fromAdvanced(advanced.plan()));
            }
        }
        if (result.error() != null) {
            return RecipeTransferPlanResult.userError(result.error());
        }
        if (result.plan() == null) {
            return RecipeTransferPlanResult.unexpectedError();
        }
        if (payloadTooLarge(result.plan())) {
            return RecipeTransferPlanResult.userError(
                    "gui.appliedpackaging.jei_transfer.payload_too_large");
        }
        return RecipeTransferPlanResult.packaged(result.plan());
    }

    private static boolean payloadTooLarge(AdvancedPatternTransferPlan plan) {
        return plan.toPayload().serializedLength()
                > AdvancedPatternTransferPlan.MAX_CLIENT_ACTION_JSON_LENGTH;
    }

    private static boolean payloadTooLarge(PackagePatternTransferPlan plan) {
        return plan.toPayload().serializedLength()
                > AdvancedPatternTransferPlan.MAX_CLIENT_ACTION_JSON_LENGTH;
    }

    private static List<RecipeSemanticEncoder> loadEncoders() {
        List<RecipeSemanticEncoder> result = new ArrayList<>();
        if (ModList.get().isLoaded("create")) {
            result.add(new CreateRecipeSemanticEncoder());
        }
        if (ModList.get().isLoaded("gtceu")) {
            result.add(new GtceuRecipeSemanticEncoder());
        }
        return List.copyOf(result);
    }
}

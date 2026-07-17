package com.warmthdawn.appliedpackaging.integration.jei;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.pattern.AdvancedPatternTransferPlan;
import com.warmthdawn.appliedpackaging.core.pattern.PackagePatternTransferPlan;
import com.warmthdawn.appliedpackaging.part.SpecializedPatternMode;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.world.menu.AdvancedPatternEncodingTermMenu;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.fml.ModList;

public final class AdvancedRecipeTransferHandler
        implements IUniversalRecipeTransferHandler<AdvancedPatternEncodingTermMenu> {
    private final IRecipeTransferHandlerHelper helper;
    private final List<AdvancedRecipeTransferAdapter> adapters;
    private final StandardRecipeTransferAdapter standardAdapter;

    public AdvancedRecipeTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
        this.adapters = loadAdapters();
        this.standardAdapter = new StandardRecipeTransferAdapter();
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
        AdvancedRecipeTransferAdapter adapter = adapters.stream()
                .filter(candidate -> candidate.supports(recipe))
                .findFirst()
                .orElse(null);
        if (container.getSpecializedMode() == SpecializedPatternMode.PACKAGE) {
            return transferPackageRecipe(container, recipe, recipeSlots, adapter, doTransfer);
        }

        if (adapter == null) {
            adapter = standardAdapter;
        }

        AdvancedRecipeTransferResult result;
        try {
            result = adapter.createPlan(recipe, recipeSlots);
        } catch (RuntimeException e) {
            AppliedPackaging.LOGGER.warn("Failed to create a JEI advanced-pattern transfer plan for {}", recipe, e);
            return helper.createInternalError();
        }
        if (result.error() != null) {
            return helper.createUserErrorWithTooltip(result.error());
        }
        AdvancedPatternTransferPlan plan = result.plan();
        if (plan == null) {
            return helper.createInternalError();
        }

        if (plan.toPayload().serializedLength() > AdvancedPatternTransferPlan.MAX_CLIENT_ACTION_JSON_LENGTH) {
            return userError("gui.appliedpackaging.jei_transfer.payload_too_large");
        }
        if (doTransfer) {
            container.importAdvancedRecipe(plan);
        }
        return null;
    }

    private IRecipeTransferError transferPackageRecipe(
            AdvancedPatternEncodingTermMenu container,
            Object recipe,
            IRecipeSlotsView recipeSlots,
            AdvancedRecipeTransferAdapter specializedAdapter,
            boolean doTransfer) {
        PackageRecipeTransferResult result;
        try {
            if (specializedAdapter != null) {
                AdvancedRecipeTransferResult advanced = specializedAdapter.createPlan(recipe, recipeSlots);
                if (advanced.error() != null) {
                    result = new PackageRecipeTransferResult(null, advanced.error());
                } else if (advanced.plan() == null) {
                    return helper.createInternalError();
                } else {
                    result = PackageRecipeTransferResult.success(
                            PackagePatternTransferPlan.fromAdvanced(advanced.plan()));
                }
            } else {
                result = standardAdapter.createPackagePlan(recipe, recipeSlots);
            }
        } catch (RuntimeException e) {
            AppliedPackaging.LOGGER.warn("Failed to create a JEI package-pattern transfer plan for {}", recipe, e);
            return helper.createInternalError();
        }
        if (result.error() != null) {
            return helper.createUserErrorWithTooltip(result.error());
        }
        PackagePatternTransferPlan plan = result.plan();
        if (plan == null) {
            return helper.createInternalError();
        }
        if (plan.toPayload().serializedLength() > AdvancedPatternTransferPlan.MAX_CLIENT_ACTION_JSON_LENGTH) {
            return userError("gui.appliedpackaging.jei_transfer.payload_too_large");
        }
        if (doTransfer) {
            container.importPackageRecipe(plan);
        }
        return null;
    }

    private IRecipeTransferError userError(String key, Object... arguments) {
        return helper.createUserErrorWithTooltip(Component.translatable(key, arguments));
    }

    private static List<AdvancedRecipeTransferAdapter> loadAdapters() {
        List<AdvancedRecipeTransferAdapter> result = new ArrayList<>();
        if (ModList.get().isLoaded("create")) {
            loadAdapter(result, "com.warmthdawn.appliedpackaging.integration.jei.create.CreateRecipeTransferAdapter");
        }
        if (ModList.get().isLoaded("gtceu")) {
            loadAdapter(result, "com.warmthdawn.appliedpackaging.integration.jei.gtceu.GtceuRecipeTransferAdapter");
        }
        return List.copyOf(result);
    }

    private static void loadAdapter(List<AdvancedRecipeTransferAdapter> target, String className) {
        try {
            Object adapter = Class.forName(className).getConstructor().newInstance();
            target.add((AdvancedRecipeTransferAdapter) adapter);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                 | IllegalAccessException | InvocationTargetException | LinkageError e) {
            AppliedPackaging.LOGGER.error("Could not load optional recipe transfer adapter {}", className, e);
        }
    }
}

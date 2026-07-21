package com.warmthdawn.appliedpackaging.gametest;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.me.common.GridInventoryEntry;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.warmthdawn.appliedpackaging.core.pattern.AdvancedPatternTransferPlan;
import com.warmthdawn.appliedpackaging.core.pattern.PackagePatternTransferPlan;
import com.warmthdawn.appliedpackaging.integration.jei.JeiRecipeExtractor;
import com.warmthdawn.appliedpackaging.integration.recipe.RecipeIngredientSelector;
import com.warmthdawn.appliedpackaging.integration.recipe.RecipeTransferSemantics;
import com.warmthdawn.appliedpackaging.integration.recipe.StandardRecipePlanFactory;
import com.warmthdawn.appliedpackaging.integration.recipe.create.CreateRecipeSemanticEncoder;
import com.warmthdawn.appliedpackaging.integration.recipe.gtceu.GtceuRecipeLookup;
import com.warmthdawn.appliedpackaging.integration.recipe.gtceu.GtceuRecipeSemanticEncoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

/**
 * Optional recipe-viewer assertions kept outside Forge's annotated GameTest holders.
 *
 * <p>Forge reflectively discovers annotated holders before filtering namespaces. Keeping optional
 * JEI signatures here leaves the annotated holder viewer-neutral. The holder skips these assertions
 * when JEI is absent, while GameTestServer still verifies that the main mod loads without a viewer.
 */
public final class OptionalRecipeIntegrationGameTests {
    private OptionalRecipeIntegrationGameTests() {
    }

    static void standardRecipeTransferUsesJeiRolesForBothPatternModes(GameTestHelper helper) {
        IRecipeSlotsView slots = new TestRecipeSlots(List.of(
                TestRecipeSlot.item(RecipeIngredientRole.INPUT, new ItemStack(Items.IRON_INGOT, 3)),
                TestRecipeSlot.fluid(RecipeIngredientRole.INPUT, new FluidStack(Fluids.WATER, 1000)),
                TestRecipeSlot.item(RecipeIngredientRole.CATALYST, new ItemStack(Items.IRON_PICKAXE)),
                TestRecipeSlot.item(RecipeIngredientRole.RENDER_ONLY, new ItemStack(Items.BARRIER)),
                TestRecipeSlot.item(RecipeIngredientRole.OUTPUT, new ItemStack(Items.DIAMOND, 2))));
        StandardRecipePlanFactory planFactory = new StandardRecipePlanFactory();

        var advanced = planFactory.createAdvancedPlan(
                new Object(),
                JeiRecipeExtractor.extractStandardData(slots),
                RecipeIngredientSelector.empty());
        helper.assertTrue(advanced.error() == null && advanced.plan() != null,
                "A normal deterministic JEI item recipe should produce an advanced pattern plan");
        helper.assertTrue(advanced.plan().columns().size() == 2
                        && advanced.plan().columns().get(0).size() == 1
                        && advanced.plan().columns().get(0).get(0).what().equals(AEItemKey.of(Items.IRON_INGOT))
                        && advanced.plan().columns().get(0).get(0).amount() == 3
                        && advanced.plan().columns().get(1).size() == 1
                        && advanced.plan().columns().get(1).get(0).what().equals(AEFluidKey.of(Fluids.WATER)),
                "Each JEI item or fluid INPUT slot should become its own package column");
        helper.assertTrue(advanced.plan().outputs().size() == 1
                        && advanced.plan().outputs().get(0).what().equals(AEItemKey.of(Items.DIAMOND))
                        && advanced.plan().outputs().get(0).amount() == 2,
                "The deterministic JEI OUTPUT slot should retain its amount");

        var packaged = planFactory.createPackagePlan(
                new Object(),
                JeiRecipeExtractor.extractStandardData(slots));
        helper.assertTrue(packaged.error() == null && packaged.plan() != null,
                "The same normal JEI recipe should produce a package-pattern plan");
        helper.assertTrue(packaged.plan().inputs().size() == 2
                        && packaged.plan().inputs().get(0).what().equals(AEItemKey.of(Items.IRON_INGOT)),
                "Package mode should omit JEI catalyst and render-only slots");
        helper.assertTrue(packaged.plan().marker() != null
                        && packaged.plan().marker().what().equals(AEItemKey.of(Items.DIAMOND))
                        && packaged.plan().marker().amount() == 1,
                "Package mode should use the primary deterministic item output as its marker");

        RecipeIngredientSelector currentInventory = new RecipeIngredientSelector(Map.of(
                AEItemKey.of(Items.SPRUCE_PLANKS), 0,
                AEFluidKey.of(Fluids.LAVA), 1));
        IRecipeSlotsView alternatives = new TestRecipeSlots(List.of(
                new TestRecipeSlot(
                        RecipeIngredientRole.INPUT,
                        List.of(
                                TestTypedIngredient.item(new ItemStack(Items.OAK_PLANKS)),
                                TestTypedIngredient.item(new ItemStack(Items.SPRUCE_PLANKS)))),
                new TestRecipeSlot(
                        RecipeIngredientRole.INPUT,
                        List.of(
                                TestTypedIngredient.fluid(new FluidStack(Fluids.WATER, 250)),
                                TestTypedIngredient.fluid(new FluidStack(Fluids.LAVA, 250)))),
                TestRecipeSlot.item(RecipeIngredientRole.OUTPUT, new ItemStack(Items.DIAMOND))));
        var preferred = planFactory.createAdvancedPlan(
                new Object(),
                JeiRecipeExtractor.extractStandardData(alternatives),
                currentInventory);
        helper.assertTrue(preferred.error() == null
                        && preferred.plan() != null
                        && preferred.plan().columns().get(0).get(0).what()
                                .equals(AEItemKey.of(Items.SPRUCE_PLANKS))
                        && preferred.plan().columns().get(1).get(0).what()
                                .equals(AEFluidKey.of(Fluids.LAVA)),
                "Recipe alternatives present in the current terminal inventory should outrank JEI's displayed candidates");
        helper.assertTrue(currentInventory.select(Ingredient.of(Items.OAK_PLANKS, Items.SPRUCE_PLANKS))
                        .what().equals(AEItemKey.of(Items.SPRUCE_PLANKS)),
                "Dependency-specific adapters should use the same current-inventory preference for raw Ingredients");

        RecipeIngredientSelector ae2Priorities = RecipeIngredientSelector.fromInventoryEntries(
                List.of(
                        new GridInventoryEntry(1, AEItemKey.of(Items.OAK_PLANKS), 2, 0, false),
                        new GridInventoryEntry(2, AEItemKey.of(Items.SPRUCE_PLANKS), 64, 0, false)),
                List.of(new ItemStack(Items.BIRCH_PLANKS)));
        helper.assertTrue(ae2Priorities.select(Ingredient.of(Items.OAK_PLANKS, Items.SPRUCE_PLANKS))
                        .what().equals(AEItemKey.of(Items.SPRUCE_PLANKS)),
                "Among equivalent network entries, the largest currently stored amount should be preferred");
        helper.assertTrue(ae2Priorities.select(Ingredient.of(Items.JUNGLE_PLANKS, Items.BIRCH_PLANKS))
                        .what().equals(AEItemKey.of(Items.BIRCH_PLANKS)),
                "Player-inventory ingredients should outrank unavailable declared candidates");

        IRecipeSlotsView ambiguousOutput = new TestRecipeSlots(List.of(
                TestRecipeSlot.item(RecipeIngredientRole.INPUT, new ItemStack(Items.IRON_INGOT)),
                new TestRecipeSlot(
                        RecipeIngredientRole.OUTPUT,
                        List.of(
                                TestTypedIngredient.item(new ItemStack(Items.DIAMOND)),
                                TestTypedIngredient.item(new ItemStack(Items.EMERALD))))));
        helper.assertTrue(planFactory.createAdvancedPlan(
                        new Object(),
                        JeiRecipeExtractor.extractStandardData(ambiguousOutput),
                        RecipeIngredientSelector.empty()).error() != null,
                "A JEI output slot with multiple alternatives must be rejected as ambiguous");
        helper.succeed();
    }

    static void genericRecipeTransferRejectsRandomOutputDefinitions(GameTestHelper helper) {
        helper.assertTrue(
                "gui.appliedpackaging.jei_transfer.random_output".equals(
                        RecipeTransferSemantics.rejectionKey(
                                new TestChanceRecipe(List.of(new TestChanceOutput(0.5d))))),
                "Generic JEI transfer should reject reflected chance outputs");
        helper.assertTrue(
                RecipeTransferSemantics.rejectionKey(
                        new TestChanceRecipe(List.of(new TestChanceOutput(1.0d)))) == null,
                "Generic JEI transfer should accept deterministic reflected outputs");
        helper.succeed();
    }

    static void createSequencedAssemblyBuildsOrderedAdvancedPlan(GameTestHelper helper) {
        ResourceLocation id = new ResourceLocation("create", "sequenced_assembly/sturdy_sheet");
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(id).orElseThrow();
        CreateRecipeSemanticEncoder encoder = new CreateRecipeSemanticEncoder();
        var result = encoder.createPlan(recipe);

        helper.assertTrue(encoder.supports(recipe),
                "Create sequenced assembly should be claimed by the Create transfer adapter");
        helper.assertTrue(result.error() == null && result.plan() != null,
                "Deterministic Create sequenced assembly should produce an advanced pattern plan");
        AdvancedPatternTransferPlan plan = result.plan();
        helper.assertTrue(plan.columns().size() >= 2,
                "Create sequenced assembly should retain initial input and later consumable stages");
        helper.assertTrue(plan.columns().get(0).size() == 1,
                "Create sequenced assembly should put its initial ingredient in the first package column");
        helper.assertTrue(plan.outputs().size() == 1
                        && plan.outputs().get(0).what() instanceof AEItemKey,
                "Create sequenced assembly should encode its deterministic primary item output");
        helper.succeed();
    }

    static void createMechanicalCraftingChoosesFewerRowOrColumnPackages(GameTestHelper helper) {
        ResourceLocation id = new ResourceLocation("create", "mechanical_crafting/extendo_grip");
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(id).orElseThrow();
        CreateRecipeSemanticEncoder encoder = new CreateRecipeSemanticEncoder();
        var result = encoder.createPlan(recipe);

        helper.assertTrue(encoder.supports(recipe),
                "Create mechanical crafting should be claimed by the Create transfer adapter");
        helper.assertTrue(result.error() == null && result.plan() != null,
                "Deterministic Create mechanical crafting should produce an advanced pattern plan");
        AdvancedPatternTransferPlan plan = result.plan();
        helper.assertTrue(plan.columns().size() == 3,
                "The 5-row by 3-column extendo grip recipe should split by columns into three packages");
        helper.assertTrue(plan.columns().get(0).size() == 4
                        && plan.columns().get(0).get(0) == null
                        && plan.columns().get(0).get(1) == null
                        && plan.columns().get(0).get(2) != null
                        && plan.columns().get(0).get(3) != null
                        && plan.columns().get(1).size() == 5
                        && plan.columns().get(1).stream().allMatch(java.util.Objects::nonNull)
                        && plan.columns().get(2).size() == 4
                        && plan.columns().get(2).get(0) == null
                        && plan.columns().get(2).get(1) == null
                        && plan.columns().get(2).get(2) != null
                        && plan.columns().get(2).get(3) != null,
                "Mechanical crafting should preserve leading empty positions within each selected column");
        helper.assertTrue(plan.outputs().size() == 1 && plan.outputs().get(0).what() instanceof AEItemKey,
                "Mechanical crafting should preserve its deterministic item output");

        PackagePatternTransferPlan packagePlan = PackagePatternTransferPlan.fromAdvanced(plan);
        helper.assertTrue(packagePlan.inputs().size() == 9 && packagePlan.marker() != null,
                "Package mode should flatten the selected row/column packages and retain the output marker");
        helper.succeed();
    }

    static void gtceuDeterministicRecipeBuildsAdvancedPlan(GameTestHelper helper) {
        GtceuRecipeSemanticEncoder encoder = new GtceuRecipeSemanticEncoder();
        var result = helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(encoder::supports)
                .map(GTRecipe.class::cast)
                .filter(recipe -> recipe.data == null
                        || (!recipe.data.contains("layered_steps")
                        && !recipe.data.contains("layered_xei")
                        && !recipe.data.contains("layered_info")))
                .map(encoder::createPlan)
                .filter(candidate -> candidate.error() == null
                        && candidate.plan() != null
                        && candidate.plan().columns().size() >= 2)
                .findFirst();

        helper.assertTrue(result.isPresent(),
                "GTCEu should expose at least one deterministic multi-material recipe that can be imported");
        AdvancedPatternTransferPlan plan = result.orElseThrow().plan();
        helper.assertTrue(plan.columns().stream().allMatch(column -> column.size() == 1),
                "GTCEu recipe import should map every deterministic material to its own package column");
        helper.assertTrue(!plan.outputs().isEmpty(),
                "GTCEu recipe import should preserve at least one deterministic output");
        helper.succeed();
    }

    static void gtceuPublicRegistryRecoversDisplayRecipeById(GameTestHelper helper) {
        GTRecipe candidate = null;
        boolean layeredCandidate = false;
        search:
        for (var recipeType : GTRegistries.RECIPE_TYPES) {
            for (var category : recipeType.getCategories()) {
                for (GTRecipe recipe : recipeType.getRecipesInCategory(category)) {
                    candidate = recipe;
                    if (recipe.data != null
                            && (recipe.data.contains("layered_steps")
                            || recipe.data.contains("layered_xei")
                            || recipe.data.contains("layered_info"))) {
                        layeredCandidate = true;
                        break search;
                    }
                }
            }
        }

        helper.assertTrue(candidate != null,
                "GTCEu should expose at least one public category recipe for EMI recovery");
        Object recovered = GtceuRecipeLookup.findById(candidate.getId());
        helper.assertTrue(recovered instanceof GTRecipe
                        && ((GTRecipe) recovered).getId().equals(candidate.getId()),
                "GTCEu EMI recovery must find display recipes by id through public registries");
        if (layeredCandidate) {
            GTRecipe layered = (GTRecipe) recovered;
            helper.assertTrue(layered.data != null
                            && (layered.data.contains("layered_steps")
                            || layered.data.contains("layered_xei")
                            || layered.data.contains("layered_info")),
                    "StarT layered display recovery must preserve public layered recipe data");
        }
        helper.succeed();
    }

    static void gtceuLayeredRecipeGroupsEachLayerIntoOnePackageColumn(GameTestHelper helper) {
        Class<?> layeredHelper;
        try {
            layeredHelper = Class.forName("com.gregtechceu.gtceu.api.recipe.LayeredRecipeHelper");
        } catch (ClassNotFoundException e) {
            helper.succeed();
            return;
        }

        GtceuRecipeSemanticEncoder encoder = new GtceuRecipeSemanticEncoder();
        GTRecipe seed = helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(GTRecipe.class::isInstance)
                .map(GTRecipe.class::cast)
                .filter(recipe -> recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of()).size() >= 4)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "StarT Fork should expose a GT recipe type that accepts four item inputs"));

        GTRecipe layeredInfoRecipe;
        GTRecipe xeiRecipe;
        GTRecipe xeiPointerRecipe;
        try {
            Class<?> recipeType = Class.forName("com.gregtechceu.gtceu.api.recipe.GTRecipeType");
            Class<?> recipeBuilder = Class.forName("com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder");
            Object builder = recipeBuilder.getMethod("of", ResourceLocation.class, recipeType)
                    .invoke(null, new ResourceLocation("appliedpackaging", "layered_xei_fixture"), seed.recipeType);
            recipeBuilder.getMethod("EUt", long.class).invoke(builder, 8L);
            recipeBuilder.getMethod("outputItems", ItemStack[].class)
                    .invoke(builder, (Object) new ItemStack[] { new ItemStack(Items.EMERALD) });

            Class<?> layeredBuilder =
                    Class.forName("com.gregtechceu.gtceu.data.recipe.builder.LayeredRecipeInfo$Builder");
            Object layers = layeredBuilder.getConstructor(recipeBuilder).newInstance(builder);
            var inputItems = layeredBuilder.getMethod("inputItems", ItemStack[].class);
            var duration = layeredBuilder.getMethod("duration", int.class);
            var next = layeredBuilder.getMethod("next");
            inputItems.invoke(layers, (Object) new ItemStack[] {
                    new ItemStack(Items.IRON_INGOT, 2), new ItemStack(Items.GOLD_INGOT, 3)
            });
            duration.invoke(layers, 20);
            next.invoke(layers);
            inputItems.invoke(layers, (Object) new ItemStack[] {
                    new ItemStack(Items.COPPER_INGOT, 4), new ItemStack(Items.DIAMOND, 5)
            });
            duration.invoke(layers, 30);
            layeredBuilder.getMethod("apply").invoke(layers);
            layeredInfoRecipe = ((GTRecipe) recipeBuilder.getMethod("buildRawRecipe").invoke(builder)).copy();

            layeredHelper.getMethod("applyLayeredRecipeModifications", recipeBuilder).invoke(null, builder);
            GTRecipe serverRecipe = (GTRecipe) recipeBuilder.getMethod("buildRawRecipe").invoke(builder);
            helper.assertTrue(serverRecipe.data.contains("layered_steps")
                            && serverRecipe.data.contains("layered_xei"),
                    "The Fork server recipe fixture should contain both executable steps and its XEI recipe");
            xeiPointerRecipe = serverRecipe.copy();
            xeiPointerRecipe.data.remove("layered_steps");
            xeiPointerRecipe.data.remove("layered_info");
            xeiRecipe = (GTRecipe) layeredHelper.getMethod("getXeiLayeredRecipe", GTRecipe.class)
                    .invoke(null, serverRecipe);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not construct the StarT Fork layered XEI recipe fixture", e);
        }
        helper.assertTrue(layeredInfoRecipe.data.contains("layered_info")
                        && !layeredInfoRecipe.data.contains("layered_steps"),
                "The pre-expansion XEI representation should retain layered_info without executable steps");
        helper.assertTrue(xeiRecipe != null,
                "The Fork server recipe should expose its embedded layered_xei representation");

        assertLayeredPlan(helper, encoder.createPlan(layeredInfoRecipe).plan(),
                "The layered_info XEI representation");
        assertLayeredPlan(helper, encoder.createPlan(xeiRecipe).plan(),
                "The embedded layered_xei representation");
        assertLayeredPlan(helper, encoder.createPlan(xeiPointerRecipe).plan(),
                "The layered_xei-only server representation");
        helper.succeed();
    }

    private static void assertLayeredPlan(
            GameTestHelper helper,
            AdvancedPatternTransferPlan plan,
            String representation) {
        helper.assertTrue(plan != null,
                representation + " should produce an advanced pattern plan");
        helper.assertTrue(plan.columns().size() == 2,
                representation + " should create exactly one package column for every layer");
        assertItemColumn(helper, plan.columns().get(0),
                Items.IRON_INGOT, 2, Items.GOLD_INGOT, 3);
        assertItemColumn(helper, plan.columns().get(1),
                Items.COPPER_INGOT, 4, Items.DIAMOND, 5);
    }

    private static void assertItemColumn(
            GameTestHelper helper,
            List<GenericStack> column,
            net.minecraft.world.item.Item firstItem,
            long firstAmount,
            net.minecraft.world.item.Item secondItem,
            long secondAmount) {
        helper.assertTrue(column.size() == 2
                        && column.get(0).what().equals(AEItemKey.of(firstItem))
                        && column.get(0).amount() == firstAmount
                        && column.get(1).what().equals(AEItemKey.of(secondItem))
                        && column.get(1).amount() == secondAmount,
                "Every layered XEI package column must preserve all materials and amounts from that layer");
    }

    public static final class TestChanceRecipe {
        private final List<TestChanceOutput> outputs;

        public TestChanceRecipe(List<TestChanceOutput> outputs) {
            this.outputs = outputs;
        }

        public List<TestChanceOutput> getOutputs() {
            return outputs;
        }
    }

    public record TestChanceOutput(double chance) {
    }

    private record TestRecipeSlots(List<IRecipeSlotView> slotViews) implements IRecipeSlotsView {
        @Override
        public List<IRecipeSlotView> getSlotViews() {
            return slotViews;
        }
    }

    private record TestRecipeSlot(
            RecipeIngredientRole role,
            List<ITypedIngredient<?>> ingredients) implements IRecipeSlotView {
        private static TestRecipeSlot item(RecipeIngredientRole role, ItemStack stack) {
            return new TestRecipeSlot(role, List.of(TestTypedIngredient.item(stack)));
        }

        private static TestRecipeSlot fluid(RecipeIngredientRole role, FluidStack stack) {
            return new TestRecipeSlot(role, List.of(TestTypedIngredient.fluid(stack)));
        }

        @Override
        public java.util.stream.Stream<ITypedIngredient<?>> getAllIngredients() {
            return ingredients.stream();
        }

        @Override
        public Optional<ITypedIngredient<?>> getDisplayedIngredient() {
            return ingredients.stream().findFirst();
        }

        @Override
        public RecipeIngredientRole getRole() {
            return role;
        }

        @Override
        public void drawHighlight(GuiGraphics guiGraphics, int color) {
        }

        @Override
        public Optional<String> getSlotName() {
            return Optional.empty();
        }
    }

    private record TestTypedIngredient<T>(
            IIngredientType<T> type,
            T ingredient) implements ITypedIngredient<T> {
        private static TestTypedIngredient<ItemStack> item(ItemStack stack) {
            return new TestTypedIngredient<>(VanillaTypes.ITEM_STACK, stack);
        }

        private static TestTypedIngredient<FluidStack> fluid(FluidStack stack) {
            return new TestTypedIngredient<>(ForgeTypes.FLUID_STACK, stack);
        }

        @Override
        public IIngredientType<T> getType() {
            return type;
        }

        @Override
        public T getIngredient() {
            return ingredient;
        }
    }
}

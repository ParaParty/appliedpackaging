package com.warmthdawn.appliedpackaging.gametest;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.util.AEColor;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackagePlan;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackageTransactions;
import com.warmthdawn.appliedpackaging.core.ae2.PackageItemStorage;
import com.warmthdawn.appliedpackaging.core.fluid_handler.FluidPackagePlan;
import com.warmthdawn.appliedpackaging.core.fluid_handler.FluidPackageTransactions;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackagePlan;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackageTransactions;
import com.warmthdawn.appliedpackaging.core.package_data.ColoredProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackagedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDetails;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanBuilder;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanFailure;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanResult;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.part.PackagePatternTerminalPart;
import com.warmthdawn.appliedpackaging.part.AdvancedPatternEncodingTerminalPart;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.world.block.AbstractHorizontalMachineBlock;
import com.warmthdawn.appliedpackaging.world.block.MePackagerBlock;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.PackageAssemblerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.PackageExportBusBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.terminal.PackagePatternTerminalBlockEntity;
import com.warmthdawn.appliedpackaging.world.entity.PackageEntity;
import com.warmthdawn.appliedpackaging.world.menu.MePackagerMenu;
import com.warmthdawn.appliedpackaging.world.menu.PackageAssemblerMenu;
import com.warmthdawn.appliedpackaging.world.menu.PackageBusMenu;
import com.warmthdawn.appliedpackaging.world.menu.PackagePatternTerminalMenu;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

@GameTestHolder(AppliedPackaging.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PackageDataGameTests {
    private PackageDataGameTests() {
    }

    @GameTest(template = "empty")
    public static void packageDataRoundTrips(GameTestHelper helper) {
        PackageData data = ironPackageData(PackageColor.RED, 64);
        ItemStack stack = new ItemStack(APItems.packageItems().get(PackageColor.RED).get());

        PackageDataStorage.write(stack, data);
        Optional<PackageData> read = PackageDataStorage.read(stack);

        helper.assertTrue(read.isPresent(), "Package data should be readable");
        helper.assertFalse(stack.hasFoil(), "Package contents should not add an enchantment glint");
        helper.assertTrue(read.get().canonicalHash().equals(data.canonicalHash()), "Canonical hash should round-trip");
        helper.assertTrue(read.get().usedUnits() == 1, "64 iron ingots should use one package unit");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageDataCanonicalOrderStacksEquivalentContents(GameTestHelper helper) {
        PackageData first = PackageData.create(
                PackageColor.RED,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        PackageData second = PackageData.create(
                PackageColor.RED,
                List.of(
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32),
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64)),
                Optional.empty(),
                0);
        ItemStack firstStack = new ItemStack(APItems.packageItems().get(PackageColor.RED).get());
        ItemStack secondStack = new ItemStack(APItems.packageItems().get(PackageColor.RED).get());
        PackageDataStorage.write(firstStack, first);
        PackageDataStorage.write(secondStack, second);

        helper.assertTrue(first.canonicalHash().equals(second.canonicalHash()),
                "Equivalent contents should produce the same canonical hash");
        helper.assertTrue(ItemStack.isSameItemSameTags(firstStack, secondStack),
                "Equivalent contents should write identical package tags for stacking");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageDataCanonicalHashSeparatesIdentity(GameTestHelper helper) {
        PackageData base = ironPackageData(PackageColor.RED, 64);
        PackageData differentContent = ironPackageData(PackageColor.RED, 65);
        PackageData differentMarker = markedIronPackageData(PackageColor.RED, Items.GOLD_INGOT);
        PackageData differentColor = ironPackageData(PackageColor.BLUE, 64);

        helper.assertFalse(base.canonicalHash().equals(differentContent.canonicalHash()),
                "Different contents should produce different canonical hashes");
        helper.assertFalse(base.canonicalHash().equals(differentMarker.canonicalHash()),
                "Different markers should produce different canonical hashes");
        helper.assertFalse(base.canonicalHash().equals(differentColor.canonicalHash()),
                "Different colors should produce different canonical hashes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void playerRecipesUseAe2BlankPatterns(GameTestHelper helper) {
        helper.assertFalse(hasRecipeOutput(helper, APItems.PACKAGE_PATTERN.get()),
                "Local package_pattern should remain as a compatibility carrier, not a player-craftable item");
        helper.assertFalse(hasRecipeOutput(helper, APItems.PACKAGED_PROCESSING_PATTERN.get()),
                "Local packaged_processing_pattern should remain as a compatibility carrier, not a player-craftable item");
        helper.assertFalse(hasRecipeOutput(helper, APItems.ADVANCED_PROCESSING_PATTERN.get()),
                "Advanced processing patterns should only be encoded in the advanced terminal");
        assertRecipeOutput(helper, "package_assembler", APItems.PACKAGE_ASSEMBLER.get());
        assertRecipeOutput(helper, "package_pattern_terminal", APItems.PACKAGE_PATTERN_TERMINAL.get());
        assertRecipeOutput(helper, "advanced_pattern_encoding_terminal",
                APItems.ADVANCED_PATTERN_ENCODING_TERMINAL.get());
        assertRecipeOutput(helper, "package_storage_bus", APItems.PACKAGE_STORAGE_BUS.get());
        assertRecipeOutput(helper, "package_export_bus", APItems.PACKAGE_EXPORT_BUS.get());
        assertRecipeOutput(helper, "package_unpacking_bus", APItems.PACKAGE_UNPACKING_BUS.get());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyPackageIsInvalid(GameTestHelper helper) {
        ItemStack stack = new ItemStack(APItems.packageItems().get(PackageColor.BLUE).get());

        helper.assertFalse(PackageDataStorage.read(stack).isPresent(), "Package without data should be invalid");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tamperedHashIsRejected(GameTestHelper helper) {
        PackageData data = ironPackageData(PackageColor.GREEN, 16);
        ItemStack stack = new ItemStack(APItems.packageItems().get(PackageColor.GREEN).get());
        PackageDataStorage.write(stack, data);

        CompoundTag packageTag = stack.getTagElement(PackageDataStorage.PACKAGE_TAG);
        helper.assertTrue(packageTag != null, "Package tag should exist");
        packageTag.putString("hash", "tampered");

        helper.assertFalse(PackageDataStorage.read(stack).isPresent(), "Tampered package hash should be rejected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void missingHashIsRejected(GameTestHelper helper) {
        PackageData data = ironPackageData(PackageColor.YELLOW, 16);
        ItemStack stack = new ItemStack(APItems.packageItems().get(PackageColor.YELLOW).get());
        PackageDataStorage.write(stack, data);

        CompoundTag packageTag = stack.getTagElement(PackageDataStorage.PACKAGE_TAG);
        helper.assertTrue(packageTag != null, "Package tag should exist");
        packageTag.remove("hash");

        helper.assertFalse(PackageDataStorage.read(stack).isPresent(), "Package data without a hash should be rejected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unsupportedVersionIsRejected(GameTestHelper helper) {
        PackageData data = ironPackageData(PackageColor.CYAN, 16);
        ItemStack stack = new ItemStack(APItems.packageItems().get(PackageColor.CYAN).get());
        PackageDataStorage.write(stack, data);

        CompoundTag packageTag = stack.getTagElement(PackageDataStorage.PACKAGE_TAG);
        helper.assertTrue(packageTag != null, "Package tag should exist");
        packageTag.putInt("version", PackageData.CURRENT_VERSION + 1);

        helper.assertFalse(PackageDataStorage.read(stack).isPresent(), "Unsupported package version should be rejected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageFilterMatchesUnsetRequirements(GameTestHelper helper) {
        PackageData data = ironPackageData(PackageColor.RED, 64);

        helper.assertTrue(PackageFilter.any().matches(PackageColor.RED, data), "Empty filter should accept a valid package");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageFilterRejectsWrongColor(GameTestHelper helper) {
        PackageData data = ironPackageData(PackageColor.RED, 64);
        PackageFilter filter = new PackageFilter(Optional.of(PackageColor.BLUE), Optional.empty(), List.of());

        helper.assertFalse(filter.matches(PackageColor.RED, data), "Color filter should reject other package colors");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageFilterMatchesMarkerAndContent(GameTestHelper helper) {
        GenericStack marker = new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1);
        PackageData data = PackageData.create(
                PackageColor.GREEN,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.of(new MarkerSpec(marker)),
                0);
        PackageFilter filter = new PackageFilter(
                Optional.of(PackageColor.GREEN),
                Optional.of(new MarkerSpec(marker)),
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 1)));

        helper.assertTrue(filter.matches(PackageColor.GREEN, data), "Filter should accept matching color, marker, and content");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageFilterRejectsUnlistedContent(GameTestHelper helper) {
        PackageData data = itemPackageData(PackageColor.RED, 32, 32);
        PackageFilter filter = new PackageFilter(
                Optional.empty(),
                Optional.empty(),
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1)));

        helper.assertFalse(filter.matches(PackageColor.RED, data), "Content filter should reject package contents outside the allowlist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageFilterMatchesFluidRequiredContent(GameTestHelper helper) {
        PackageData data = PackageData.create(
                PackageColor.BLUE,
                List.of(new GenericStack(AEFluidKey.of(Fluids.WATER), 1000)),
                Optional.empty(),
                0);
        PackageFilter filter = new PackageFilter(
                Optional.of(PackageColor.BLUE),
                Optional.empty(),
                List.of(new GenericStack(AEFluidKey.of(Fluids.WATER), 1000)));

        helper.assertTrue(filter.matches(PackageColor.BLUE, data),
                "Content filter should accept matching fluid requirements");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePlanFlattensSourcePackages(GameTestHelper helper) {
        PackageData source = ironPackageData(PackageColor.RED, 64);
        PackagePlanResult result = PackagePlanBuilder.build(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                List.of(source),
                MarkerMergeMode.RETAIN,
                Optional.empty(),
                PackageCapacityProfile.DEFAULT,
                0);

        helper.assertTrue(result.success(), "Package plan should flatten source packages into virtual contents");
        PackageData data = result.data().orElseThrow();
        helper.assertTrue(amountOf(data, AEItemKey.of(Items.IRON_INGOT)) == 64, "Flattened plan should contain source iron");
        helper.assertTrue(amountOf(data, AEItemKey.of(Items.COPPER_INGOT)) == 32, "Flattened plan should contain loose copper");
        helper.assertTrue(data.marker().isEmpty(), "Retain should keep no marker when sources have no marker");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePlanRejectsRetainMarkerConflict(GameTestHelper helper) {
        PackageData goldMarked = markedIronPackageData(PackageColor.RED, Items.GOLD_INGOT);
        PackageData diamondMarked = markedIronPackageData(PackageColor.BLUE, Items.DIAMOND);
        PackagePlanResult result = PackagePlanBuilder.build(
                PackageColor.FLUIX,
                List.of(),
                List.of(goldMarked, diamondMarked),
                MarkerMergeMode.RETAIN,
                Optional.empty(),
                PackageCapacityProfile.DEFAULT,
                0);

        helper.assertFalse(result.success(), "Retain mode should reject conflicting source markers");
        helper.assertTrue(result.failure().orElseThrow() == PackagePlanFailure.MARKER_CONFLICT, "Failure should be marker conflict");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePlanOverridesMarker(GameTestHelper helper) {
        PackageData source = markedIronPackageData(PackageColor.RED, Items.GOLD_INGOT);
        MarkerSpec override = new MarkerSpec(new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
        PackagePlanResult result = PackagePlanBuilder.build(
                PackageColor.FLUIX,
                List.of(),
                List.of(source),
                MarkerMergeMode.OVERRIDE,
                Optional.of(override),
                PackageCapacityProfile.DEFAULT,
                0);

        helper.assertTrue(result.success(), "Override mode should accept a replacement marker");
        helper.assertTrue(result.data().orElseThrow().marker().map(marker -> marker.sameAs(override)).orElse(false),
                "Output marker should be the override marker");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePlanClearsMarker(GameTestHelper helper) {
        PackageData source = markedIronPackageData(PackageColor.RED, Items.GOLD_INGOT);
        PackagePlanResult result = PackagePlanBuilder.build(
                PackageColor.FLUIX,
                List.of(),
                List.of(source),
                MarkerMergeMode.CLEAR,
                Optional.empty(),
                PackageCapacityProfile.DEFAULT,
                0);

        helper.assertTrue(result.success(), "Clear mode should accept marked source packages");
        helper.assertTrue(result.data().orElseThrow().marker().isEmpty(), "Clear mode should remove output marker");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePlanRejectsCapacityOverflow(GameTestHelper helper) {
        PackagePlanResult result = PackagePlanBuilder.build(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 640)),
                List.of(),
                MarkerMergeMode.RETAIN,
                Optional.empty(),
                PackageCapacityProfile.DEFAULT,
                0);

        helper.assertFalse(result.success(), "Default capacity should reject ten item units");
        helper.assertTrue(result.failure().orElseThrow() == PackagePlanFailure.CAPACITY_EXCEEDED,
                "Failure should be capacity exceeded");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemHandlerPackPlanExtractsPackageContents(GameTestHelper helper) {
        ItemStackHandler source = new ItemStackHandler(2);
        source.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        source.setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 32));

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                source,
                PackageColor.FLUIX,
                PackageCapacityProfile.DEFAULT);

        helper.assertTrue(plan.isPresent(), "Item handler should produce a package plan");
        helper.assertTrue(ItemPackageTransactions.canExtract(source, plan.get()), "Planned extraction should be simulatable");
        ItemPackageTransactions.commitExtract(source, plan.get());

        helper.assertTrue(source.getStackInSlot(0).isEmpty(), "Iron source slot should be extracted");
        helper.assertTrue(source.getStackInSlot(1).isEmpty(), "Copper source slot should be extracted");
        helper.assertTrue(amountOf(plan.get().data(), AEItemKey.of(Items.IRON_INGOT)) == 64, "Plan should contain iron");
        helper.assertTrue(amountOf(plan.get().data(), AEItemKey.of(Items.COPPER_INGOT)) == 32, "Plan should contain copper");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemHandlerUnpackInsertsAllContents(GameTestHelper helper) {
        PackageData data = PackageData.create(
                PackageColor.FLUIX,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        ItemStackHandler target = new ItemStackHandler(2);

        helper.assertTrue(ItemPackageTransactions.canInsertPackageContents(data, target),
                "Target should simulate accepting all package contents");
        helper.assertTrue(ItemPackageTransactions.insertPackageContents(data, target, false),
                "Target should accept all package contents");
        helper.assertTrue(itemAmountInHandler(target, Items.IRON_INGOT) == 64,
                "Target should contain all unpacked iron");
        helper.assertTrue(itemAmountInHandler(target, Items.COPPER_INGOT) == 32,
                "Target should contain all unpacked copper");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemHandlerUnpackRejectsFullTarget(GameTestHelper helper) {
        PackageData data = ironPackageData(PackageColor.FLUIX, 64);
        ItemStackHandler target = new ItemStackHandler(1);
        target.setStackInSlot(0, new ItemStack(Items.DIRT, 64));

        helper.assertFalse(ItemPackageTransactions.canInsertPackageContents(data, target),
                "Full incompatible target should reject complete package contents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemHandlerPackPlanRespectsDefaultCapacity(GameTestHelper helper) {
        ItemStackHandler source = new ItemStackHandler(1);
        source.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 640));

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                source,
                PackageColor.FLUIX,
                PackageCapacityProfile.DEFAULT);

        helper.assertTrue(plan.isPresent(), "Oversized source should still plan the largest default package");
        helper.assertTrue(amountOf(plan.get().data(), AEItemKey.of(Items.IRON_INGOT)) == 576,
                "Default package should hold nine iron stack units");
        helper.assertTrue(plan.get().data().usedUnits() == 9, "Default package should use nine units");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemHandlerPackPlanUsesContentFilter(GameTestHelper helper) {
        ItemStackHandler source = new ItemStackHandler(2);
        source.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 640));
        source.setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 64));
        PackageFilter filter = new PackageFilter(
                Optional.of(PackageColor.RED),
                Optional.empty(),
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1)));

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                source,
                PackageColor.RED,
                PackageCapacityProfile.DEFAULT,
                filter);

        helper.assertTrue(plan.isPresent(), "Filtered plan should be created from matching contents");
        helper.assertTrue(amountOf(plan.get().data(), AEItemKey.of(Items.IRON_INGOT)) == 576,
                "Filtered plan should spend capacity on allowed iron without using the ghost amount as a limit");
        helper.assertTrue(amountOf(plan.get().data(), AEItemKey.of(Items.COPPER_INGOT)) == 0,
                "Filtered plan should not spend capacity on unrelated loose items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemHandlerPackPlanRejectsMissingFilteredContent(GameTestHelper helper) {
        ItemStackHandler source = new ItemStackHandler(1);
        source.setStackInSlot(0, new ItemStack(Items.COPPER_INGOT, 64));
        PackageFilter filter = new PackageFilter(
                Optional.of(PackageColor.RED),
                Optional.empty(),
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64)));

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                source,
                PackageColor.RED,
                PackageCapacityProfile.DEFAULT,
                filter);

        helper.assertTrue(plan.isEmpty(), "Filtered plan should fail when required contents are absent");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemHandlerPackPlanOverridesMarkerFromFilter(GameTestHelper helper) {
        ItemStackHandler source = new ItemStackHandler(1);
        source.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        MarkerSpec marker = new MarkerSpec(new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1));
        PackageFilter filter = new PackageFilter(
                Optional.of(PackageColor.BLUE),
                Optional.of(marker),
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64)));

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                source,
                PackageColor.BLUE,
                PackageCapacityProfile.DEFAULT,
                filter);

        helper.assertTrue(plan.isPresent(), "Filtered plan should create a marked package");
        helper.assertTrue(plan.get().data().marker().map(actual -> actual.sameAs(marker)).orElse(false),
                "Filtered plan should override marker from the filter template");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemHandlerPackPlanRetainsMarkerFromExplicitMode(GameTestHelper helper) {
        ItemStackHandler source = new ItemStackHandler(1);
        MarkerSpec marker = new MarkerSpec(new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1));
        source.setStackInSlot(0, packageStack(
                PackageColor.RED,
                markedIronPackageData(PackageColor.RED, Items.GOLD_INGOT)));

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                source,
                PackageColor.BLUE,
                PackageCapacityProfile.DEFAULT,
                PackageFilter.any(),
                MarkerMergeMode.RETAIN,
                Optional.empty());

        helper.assertTrue(plan.isPresent(), "Explicit retain mode should package marked source packages");
        helper.assertTrue(plan.get().data().marker().map(actual -> actual.sameAs(marker)).orElse(false),
                "Explicit retain mode should keep the source marker");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemHandlerPackPlanOverridesMarkerFromExplicitMode(GameTestHelper helper) {
        ItemStackHandler source = new ItemStackHandler(1);
        source.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        MarkerSpec marker = new MarkerSpec(new GenericStack(AEItemKey.of(Items.DIAMOND), 1));

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                source,
                PackageColor.BLUE,
                PackageCapacityProfile.DEFAULT,
                PackageFilter.any(),
                MarkerMergeMode.OVERRIDE,
                Optional.of(marker));

        helper.assertTrue(plan.isPresent(), "Explicit override mode should package loose contents");
        helper.assertTrue(plan.get().data().marker().map(actual -> actual.sameAs(marker)).orElse(false),
                "Explicit override mode should write the configured marker");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemHandlerPackPlanClearsMarkerFromExplicitMode(GameTestHelper helper) {
        ItemStackHandler source = new ItemStackHandler(1);
        source.setStackInSlot(0, packageStack(
                PackageColor.RED,
                markedIronPackageData(PackageColor.RED, Items.GOLD_INGOT)));

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                source,
                PackageColor.BLUE,
                PackageCapacityProfile.DEFAULT,
                PackageFilter.any(),
                MarkerMergeMode.CLEAR,
                Optional.empty());

        helper.assertTrue(plan.isPresent(), "Explicit clear mode should package marked source packages");
        helper.assertTrue(plan.get().data().marker().isEmpty(), "Explicit clear mode should remove the marker");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemHandlerPackPlanUsesLargerCapacityProfile(GameTestHelper helper) {
        ItemStackHandler source = new ItemStackHandler(1);
        source.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 640));

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                source,
                PackageColor.FLUIX,
                PackageCapacityProfile.STORAGE_64K);

        helper.assertTrue(plan.isPresent(), "64k capacity should create a package plan");
        helper.assertTrue(amountOf(plan.get().data(), AEItemKey.of(Items.IRON_INGOT)) == 640,
                "64k capacity should hold ten iron stack units");
        helper.assertTrue(plan.get().data().usedUnits() == 10, "64k package should use ten units");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageFilterReadsEncodedPatternTemplate(GameTestHelper helper) {
        PackageData data = ironPackageData(PackageColor.RED, 64);
        ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(pattern, PackageColor.RED, data);

        Optional<PackageFilter> filter = PackageFilter.fromTemplate(pattern);

        helper.assertTrue(filter.isPresent(), "Encoded pattern should be usable as a package filter template");
        helper.assertTrue(filter.get().color().orElseThrow() == PackageColor.RED,
                "Pattern filter should keep encoded color");
        helper.assertTrue(filter.get().matches(PackageColor.RED, data), "Pattern filter should match equivalent package data");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mePackagerRecognizesAe2CapacityItems(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.tryParse("ae2:cell_component_64k");
        helper.assertTrue(id != null, "AE2 64k storage component id should parse");
        ItemStack component = new ItemStack(BuiltInRegistries.ITEM.get(id));

        helper.assertFalse(component.isEmpty(), "AE2 64k storage component should be registered");
        helper.assertTrue(MePackagerBlockEntity.capacityProfileFromItem(component)
                        .filter(profile -> profile == PackageCapacityProfile.STORAGE_64K)
                        .isPresent(),
                "ME Packager should map AE2 64k storage component to the 64k package profile");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageItemsCreatePackageEntityForDroppedStacks(GameTestHelper helper) {
        ItemStack packageStack = packageStack(PackageColor.BLUE, ironPackageData(PackageColor.BLUE, 64));
        ItemEntity original = new ItemEntity(
                helper.getLevel(),
                helper.absolutePos(BlockPos.ZERO).getX() + 0.5D,
                helper.absolutePos(BlockPos.ZERO).getY() + 1.0D,
                helper.absolutePos(BlockPos.ZERO).getZ() + 0.5D,
                packageStack.copy());
        original.setDeltaMovement(0.1D, 0.2D, 0.3D);

        var customEntity = packageStack.getItem().createEntity(helper.getLevel(), original, packageStack);

        helper.assertTrue(customEntity instanceof PackageEntity,
                "Package items should replace dropped ItemEntity with appliedpackaging:package");
        helper.assertFalse(customEntity instanceof ItemEntity,
                "Package entity should use Create-style dedicated entity semantics, not vanilla ItemEntity pickup semantics");
        PackageEntity packageEntity = (PackageEntity) customEntity;
        helper.assertTrue(closeTo(packageEntity.getDeltaMovement().x, 0.15D)
                        && closeTo(packageEntity.getDeltaMovement().y, 0.3D)
                        && closeTo(packageEntity.getDeltaMovement().z, 0.45D),
                "Package entity should inherit Create-style dropped item momentum scaling");
        helper.assertTrue(packageEntity.getPackageStack().is(packageStack.getItem()),
                "Package entity should preserve the concrete package item color");
        helper.assertTrue(PackageDataStorage.read(packageEntity.getPackageStack()).isPresent(),
                "Package entity should preserve package data");
        helper.assertTrue(Math.abs(packageEntity.getBbWidth() - PackageEntity.WIDTH) < 0.001F,
                "Package entity collision width should match the 10px package model width");
        helper.assertTrue(Math.abs(packageEntity.getBbHeight() - PackageEntity.HEIGHT) < 0.001F,
                "Package entity collision height should match the 8px package model height");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageEntitySettlesOnGroundWithoutHovering(GameTestHelper helper) {
        BlockPos groundPos = new BlockPos(1, 0, 1);
        helper.setBlock(groundPos, Blocks.STONE);

        ItemStack packageStack = packageStack(PackageColor.BLUE, ironPackageData(PackageColor.BLUE, 64));
        Vec3 spawnPos = helper.absoluteVec(new Vec3(1.5D, 2.0D, 1.5D));
        PackageEntity entity = new PackageEntity(helper.getLevel(), spawnPos.x, spawnPos.y, spawnPos.z, packageStack);
        helper.getLevel().addFreshEntity(entity);

        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(entity.isAlive(), "Package entity should remain alive while settling");
                    helper.assertTrue(entity.onGround(), "Package entity should settle on the ground");
                    helper.assertTrue(closeTo(entity.getY(), helper.absolutePos(groundPos).getY() + 1.0D),
                            "Package entity bottom should be flush with the supporting block");
                    helper.assertTrue(Math.abs(entity.getDeltaMovement().y) < 1.0e-6D,
                            "Settled package entity should not keep vertical bounce velocity");
                    helper.assertTrue(closeTo(
                                    entity.getBoundingBox().maxY - entity.getBoundingBox().minY,
                                    PackageEntity.HEIGHT),
                            "Package entity AABB height should match the visual package height");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void shiftRightClickPackageUnpacksAllPackagesToPlayer(GameTestHelper helper) {
        FakePlayer player = FakePlayerFactory.get(
                (ServerLevel) helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "APUnpack"));
        player.setShiftKeyDown(true);
        ItemStack packageStack = packageStack(PackageColor.BLUE, itemPackageData(PackageColor.BLUE, 64, 32));
        packageStack.setCount(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, packageStack);

        InteractionResultHolder<ItemStack> result = player.getItemInHand(InteractionHand.MAIN_HAND)
                .use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, result.getObject());

        helper.assertTrue(result.getResult().consumesAction(), "Shift-right-click should consume the package use action");
        helper.assertFalse(PackageDataStorage.hasPackageData(player.getItemInHand(InteractionHand.MAIN_HAND)),
                "Shift-right-click should remove the whole held package stack");
        helper.assertTrue(player.getInventory().countItem(Items.IRON_INGOT) == 128,
                "Shift-right-click should insert unpacked iron into the player inventory");
        helper.assertTrue(player.getInventory().countItem(Items.COPPER_INGOT) == 64,
                "Shift-right-click should insert unpacked copper into the player inventory");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void damagedPackageEntityUnpacksContentsToWorld(GameTestHelper helper) {
        ItemStack packageStack = packageStack(PackageColor.BLUE, itemPackageData(PackageColor.BLUE, 64, 32));
        Vec3 spawnPos = helper.absoluteVec(new Vec3(1.5D, 1.0D, 1.5D));
        PackageEntity entity = new PackageEntity(helper.getLevel(), spawnPos.x, spawnPos.y, spawnPos.z, packageStack);
        helper.getLevel().addFreshEntity(entity);

        boolean unpacked = entity.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.assertTrue(unpacked, "Damaging a package entity should unpack item contents");
        helper.assertTrue(entity.isRemoved(), "Damaged package entity should be removed after unpacking");
        helper.succeedWhen(() -> {
            int ironAmount = itemEntityAmount(helper, Items.IRON_INGOT, spawnPos, 4.0D);
            int copperAmount = itemEntityAmount(helper, Items.COPPER_INGOT, spawnPos, 4.0D);
            helper.assertTrue(ironAmount == 64,
                    "Damaged package entity should drop unpacked iron, found " + ironAmount);
            helper.assertTrue(copperAmount == 32,
                    "Damaged package entity should drop unpacked copper, found " + copperAmount);
        });
    }

    @GameTest(template = "empty")
    public static void mePackagerBaseCapacityUses1kAnd16Types(GameTestHelper helper) {
        helper.assertTrue(MePackagerBlockEntity.BASE_CAPACITY_PROFILE.unitLimit() == 1024,
                "ME Packager base profile should use 1k package units");
        helper.assertTrue(MePackagerBlockEntity.BASE_CAPACITY_PROFILE.typeLimit() == 16,
                "ME Packager base profile should allow up to 16 material types");

        List<GenericStack> sixteenTypes = List.of(
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1),
                new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 1),
                new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1),
                new GenericStack(AEItemKey.of(Items.DIAMOND), 1),
                new GenericStack(AEItemKey.of(Items.EMERALD), 1),
                new GenericStack(AEItemKey.of(Items.REDSTONE), 1),
                new GenericStack(AEItemKey.of(Items.LAPIS_LAZULI), 1),
                new GenericStack(AEItemKey.of(Items.QUARTZ), 1),
                new GenericStack(AEItemKey.of(Items.COAL), 1),
                new GenericStack(AEItemKey.of(Items.CHARCOAL), 1),
                new GenericStack(AEItemKey.of(Items.AMETHYST_SHARD), 1),
                new GenericStack(AEItemKey.of(Items.FLINT), 1),
                new GenericStack(AEItemKey.of(Items.CLAY_BALL), 1),
                new GenericStack(AEItemKey.of(Items.BRICK), 1),
                new GenericStack(AEItemKey.of(Items.NETHER_BRICK), 1),
                new GenericStack(AEItemKey.of(Items.PRISMARINE_SHARD), 1));

        PackagePlanResult fit = PackagePlanBuilder.build(
                PackageColor.FLUIX,
                sixteenTypes,
                List.of(),
                MarkerMergeMode.CLEAR,
                Optional.empty(),
                MePackagerBlockEntity.BASE_CAPACITY_PROFILE,
                0);
        helper.assertTrue(fit.success(), "ME Packager base profile should accept 16 types");

        List<GenericStack> seventeenTypes = List.of(
                sixteenTypes.get(0),
                sixteenTypes.get(1),
                sixteenTypes.get(2),
                sixteenTypes.get(3),
                sixteenTypes.get(4),
                sixteenTypes.get(5),
                sixteenTypes.get(6),
                sixteenTypes.get(7),
                sixteenTypes.get(8),
                sixteenTypes.get(9),
                sixteenTypes.get(10),
                sixteenTypes.get(11),
                sixteenTypes.get(12),
                sixteenTypes.get(13),
                sixteenTypes.get(14),
                sixteenTypes.get(15),
                new GenericStack(AEItemKey.of(Items.PRISMARINE_CRYSTALS), 1));
        PackagePlanResult tooManyTypes = PackagePlanBuilder.build(
                PackageColor.FLUIX,
                seventeenTypes,
                List.of(),
                MarkerMergeMode.CLEAR,
                Optional.empty(),
                MePackagerBlockEntity.BASE_CAPACITY_PROFILE,
                0);
        helper.assertFalse(tooManyTypes.success(),
                "ME Packager base profile should reject the seventeenth material type");
        helper.assertTrue(tooManyTypes.failure().orElseThrow() == PackagePlanFailure.CAPACITY_EXCEEDED,
                "Seventeenth type should fail as a capacity limit");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mePackagerExternalCapabilityExposesPackagesAwayFromNetworkSide(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(0, 1, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(packagerPos),
                APBlocks.ME_PACKAGER.get().defaultBlockState()
                        .setValue(AbstractHorizontalMachineBlock.FACING, Direction.EAST)
                        .setValue(MePackagerBlock.NETWORK_SIDE, Direction.WEST),
                3);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        helper.assertTrue(packager.networkSide() == Direction.WEST,
                "ME Packager should read its selected ME side from block state");
        helper.assertTrue(packager.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST).resolve().isEmpty(),
                "ME Packager network side should not expose normal item capability");
        IItemHandler sideHandler = packager.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                .resolve()
                .orElseThrow();

        ItemStack packageStack = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64));
        ItemStack insertRemainder = sideHandler.insertItem(0, packageStack.copy(), false);
        helper.assertTrue(ItemStack.isSameItemSameTags(packageStack, insertRemainder)
                        && insertRemainder.getCount() == packageStack.getCount(),
                "Non-network package input should reject packages when no AE target can unpack them");
        helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_INPUT).isEmpty(),
                "Rejected external package insertion should not fill the internal input slot");

        ItemStack component = ae2Item("cell_component_64k");
        ItemStack rejected = sideHandler.insertItem(0, component.copy(), true);
        helper.assertTrue(ItemStack.isSameItemSameTags(component, rejected) && rejected.getCount() == component.getCount(),
                "External package input should reject non-package configuration items");

        packager.getItems().setStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT, packageStack.copy());
        ItemStack extracted = sideHandler.extractItem(1, 1, false);
        helper.assertTrue(extracted.is(packageStack.getItem()), "Non-network sides should expose package output");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mePackagerExternalCapabilityDirectlyUnpacksAcceptedPackage(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(3, 1, 0);
        InterfaceBlockEntity aeInterface = placeMePackagerAe2Interface(helper, packagerPos, Direction.WEST);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAe2InterfaceReady(helper, aeInterface, "external direct-unpack ME Packager test");
                    assertMePackagerReady(helper, packager, "external direct-unpack ME Packager test");
                })
                .thenExecute(() -> {
                    IItemHandler sideHandler = packager.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                            .resolve()
                            .orElseThrow();
                    ItemStack packageStack = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64));
                    packageStack.setCount(2);

                    ItemStack simulatedRemainder = sideHandler.insertItem(0, packageStack.copy(), true);
                    helper.assertTrue(simulatedRemainder.getCount() == 1,
                            "External insert simulation should accept exactly one package while idle");
                    var storage = aeStorage(aeInterface);
                    var source = IActionSource.ofMachine(aeInterface);
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 0,
                            "Simulated external insert should not commit unpacked contents");

                    ItemStack remainder = sideHandler.insertItem(0, packageStack.copy(), false);
                    helper.assertTrue(remainder.getCount() == 1,
                            "External insert should consume exactly one accepted package");
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_INPUT).isEmpty(),
                            "External insert should not stage the package in the input slot");
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "External unpack should not create an output package");
                    helper.assertTrue(packager.workingOperation() == MePackagerBlockEntity.WorkingOperation.UNPACKING,
                            "External insert should enter unpacking work mode for animation");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 64,
                            "External insert should immediately unpack into the selected AE network");

                    ItemStack busyRejected = sideHandler.insertItem(0, packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64)), false);
                    helper.assertTrue(!busyRejected.isEmpty(),
                            "External input should reject new packages while the packager is working");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void mePackagerMenuShiftClickUnpacksOnePackageAndRejectsWhileWorking(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(3, 1, 0);
        InterfaceBlockEntity aeInterface = placeMePackagerAe2Interface(helper, packagerPos, Direction.WEST);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);
        FakePlayer player = newFakePlayer(helper);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAe2InterfaceReady(helper, aeInterface, "menu shift-click ME Packager test");
                    assertMePackagerReady(helper, packager, "menu shift-click ME Packager test");
                })
                .thenExecute(() -> {
                    ItemStack packageStack = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64));
                    packageStack.setCount(2);
                    player.getInventory().setItem(0, packageStack.copy());
                    MePackagerMenu menu = new MePackagerMenu(0, player.getInventory(), packager);
                    int playerPackageSlot = findMenuSlotWithStack(menu, packageStack);

                    ItemStack moved = menu.quickMoveStack(player, playerPackageSlot);
                    helper.assertTrue(moved.getCount() == 1 && PackageDataStorage.read(moved).isPresent(),
                            "Menu shift-click should consume and report exactly one package");
                    helper.assertTrue(player.getInventory().getItem(0).getCount() == 1,
                            "Menu shift-click should leave the second package in the player inventory");
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_INPUT).isEmpty(),
                            "Menu shift-click should not stage packages in the hidden input slot");
                    helper.assertTrue(packager.workingOperation() == MePackagerBlockEntity.WorkingOperation.UNPACKING,
                            "Menu shift-click should enter unpacking work mode");

                    var storage = aeStorage(aeInterface);
                    var source = IActionSource.ofMachine(aeInterface);
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 64,
                            "Menu shift-click should immediately unpack the first package into the AE network");

                    ItemStack busyMoved = menu.quickMoveStack(player, playerPackageSlot);
                    helper.assertTrue(busyMoved.isEmpty(),
                            "Menu shift-click should reject package input while the packager is working");
                    helper.assertTrue(player.getInventory().getItem(0).getCount() == 1,
                            "Rejected busy shift-click should leave the remaining package untouched");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 128, Actionable.SIMULATE, source) == 64,
                            "Rejected busy shift-click should not unpack a second package");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void mePackagerExternalCapabilityRequiresCurrentIdentityAndContentFilter(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(3, 1, 0);
        InterfaceBlockEntity aeInterface = placeMePackagerAe2Interface(helper, packagerPos, Direction.WEST);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);
        MarkerSpec diamondMarker = new MarkerSpec(new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
        MarkerSpec goldMarker = new MarkerSpec(new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1));

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAe2InterfaceReady(helper, aeInterface, "external filter-gate ME Packager test");
                    assertMePackagerReady(helper, packager, "external filter-gate ME Packager test");
                })
                .thenExecute(() -> {
                    packager.setSelectedColor(PackageColor.RED);
                    packager.getItems().setStackInSlot(MePackagerBlockEntity.SLOT_MARKER, new ItemStack(Items.DIAMOND));
                    packager.getContentFilter().setStack(0, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1));
                    IItemHandler sideHandler = packager.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                            .resolve()
                            .orElseThrow();

                    ItemStack wrongColor = packageStack(
                            PackageColor.BLUE,
                            PackageData.create(
                                    PackageColor.BLUE,
                                    List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64)),
                                    Optional.of(diamondMarker),
                                    0));
                    ItemStack wrongMarker = packageStack(
                            PackageColor.RED,
                            PackageData.create(
                                    PackageColor.RED,
                                    List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64)),
                                    Optional.of(goldMarker),
                                    0));
                    ItemStack wrongContent = packageStack(
                            PackageColor.RED,
                            PackageData.create(
                                    PackageColor.RED,
                                    List.of(new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 64)),
                                    Optional.of(diamondMarker),
                                    0));
                    ItemStack accepted = packageStack(
                            PackageColor.RED,
                            PackageData.create(
                                    PackageColor.RED,
                                    List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64)),
                                    Optional.of(diamondMarker),
                                    0));

                    helper.assertTrue(!sideHandler.insertItem(0, wrongColor.copy(), false).isEmpty(),
                            "External input should reject packages with a non-default color mismatch");
                    helper.assertTrue(!sideHandler.insertItem(0, wrongMarker.copy(), false).isEmpty(),
                            "External input should reject packages with a marker mismatch");
                    helper.assertTrue(!sideHandler.insertItem(0, wrongContent.copy(), false).isEmpty(),
                            "External input should reject packages containing keys outside the content allowlist");
                    helper.assertTrue(sideHandler.insertItem(0, accepted.copy(), false).isEmpty(),
                            "External input should accept packages matching current color, marker, and content allowlist");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void mePackagerInverterCardReversesContentFilter(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(3, 1, 0);
        InterfaceBlockEntity aeInterface = placeMePackagerAe2Interface(helper, packagerPos, Direction.WEST);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAe2InterfaceReady(helper, aeInterface, "inverter-card ME Packager test");
                    assertMePackagerReady(helper, packager, "inverter-card ME Packager test");
                })
                .thenExecute(() -> {
                    packager.getContentFilter().setStack(0, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1));
                    packager.getUpgrades().addItems(AEItems.INVERTER_CARD.stack());
                    IItemHandler sideHandler = packager.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                            .resolve()
                            .orElseThrow();
                    ItemStack ironPackage = packageStack(PackageColor.FLUIX, ironPackageData(PackageColor.FLUIX, 64));
                    ItemStack copperPackage = packageStack(
                            PackageColor.FLUIX,
                            PackageData.create(
                                    PackageColor.FLUIX,
                                    List.of(new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 64)),
                                    Optional.empty(),
                                    0));

                    helper.assertTrue(!sideHandler.insertItem(0, ironPackage.copy(), false).isEmpty(),
                            "Inverter card should reject package contents listed in the filter");
                    helper.assertTrue(sideHandler.insertItem(0, copperPackage.copy(), false).isEmpty(),
                            "Inverter card should accept package contents outside the filter");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void mePackagerPlacementDefaultsNetworkSideTowardClickedBlock(GameTestHelper helper) {
        FakePlayer player = newFakePlayer(helper);
        ItemStack stack = new ItemStack(APItems.ME_PACKAGER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos clickedPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(clickedPos),
                Direction.EAST,
                clickedPos,
                false);
        BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);

        BlockState state = APBlocks.ME_PACKAGER.get().getStateForPlacement(context);

        helper.assertTrue(state != null, "ME Packager placement should produce a block state");
        helper.assertTrue(state.getValue(MePackagerBlock.NETWORK_SIDE) == Direction.WEST,
                "ME Packager should default its network side toward the clicked block");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mePackagerNetworkSideRequiresWrench(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(1, 1, 0);
        BlockPos absolutePos = helper.absolutePos(relativePos);
        helper.getLevel().setBlock(
                absolutePos,
                APBlocks.ME_PACKAGER.get().defaultBlockState()
                        .setValue(AbstractHorizontalMachineBlock.FACING, Direction.SOUTH)
                        .setValue(MePackagerBlock.NETWORK_SIDE, Direction.WEST),
                3);
        FakePlayer player = newFakePlayer(helper);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        BlockHitResult northHit = new BlockHitResult(
                Vec3.atCenterOf(absolutePos),
                Direction.NORTH,
                absolutePos,
                false);
        BlockState state = helper.getLevel().getBlockState(absolutePos);
        APBlocks.ME_PACKAGER.get().use(state, helper.getLevel(), absolutePos, player, InteractionHand.MAIN_HAND, northHit);
        helper.assertTrue(helper.getLevel().getBlockState(absolutePos).getValue(MePackagerBlock.NETWORK_SIDE) == Direction.WEST,
                "Non-wrench right-click should not change the ME Packager network side");

        player.setItemInHand(InteractionHand.MAIN_HAND, AEItems.CERTUS_QUARTZ_WRENCH.stack());
        InteractionResult result = APBlocks.ME_PACKAGER.get().use(
                helper.getLevel().getBlockState(absolutePos),
                helper.getLevel(),
                absolutePos,
                player,
                InteractionHand.MAIN_HAND,
                northHit);
        helper.assertTrue(result == InteractionResult.CONSUME,
                "AE2 wrench should be consumed by ME Packager network-side switching");
        helper.assertTrue(helper.getLevel().getBlockState(absolutePos).getValue(MePackagerBlock.NETWORK_SIDE) == Direction.NORTH,
                "AE2 wrench should switch the ME Packager network side to the clicked face");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mePackagerPackagesFromSwitchableTopAe2Side(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(1, 1, 0);
        InterfaceBlockEntity aeInterface = placeMePackagerAe2Interface(helper, packagerPos, Direction.UP);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAe2InterfaceReady(helper, aeInterface, "top-side ME Packager test");
                    assertMePackagerReady(helper, packager, "top-side ME Packager test");
                })
                .thenExecute(() -> {
                    helper.assertTrue(packager.networkSide() == Direction.UP,
                            "ME Packager should support a top ME connection side");
                    var storage = aeStorage(aeInterface);
                    var source = IActionSource.ofMachine(aeInterface);
                    long inserted = storage.insert(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.MODULATE, source);
                    helper.assertTrue(inserted == 64,
                            "Top-side AE network should accept iron before ME Packager packs");

                    MePackagerBlockEntity.MachineResult result = packager.runOnce();
                    helper.assertTrue(result == MePackagerBlockEntity.MachineResult.PACKED,
                            "ME Packager should package from its selected top AE side");
                    helper.assertTrue(packager.workingOperation() == MePackagerBlockEntity.WorkingOperation.PACKING,
                            "ME Packager should enter packing work mode before exposing output");
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "ME Packager should not expose the output package before the animation finishes");
                })
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 2, () -> {
                    ItemStack output = packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).copy();
                    PackageData data = PackageDataStorage.read(output).orElseThrow();
                    helper.assertTrue(amountOf(data, AEItemKey.of(Items.IRON_INGOT)) == 64,
                            "Top-side package should contain iron");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void mePackagerPackagesThroughSelectedAeCableSide(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(0, 1, 0);
        BlockPos cablePos = packagerPos.relative(Direction.EAST);
        BlockPos drivePos = cablePos.relative(Direction.EAST);
        BlockPos energyCellPos = drivePos.relative(Direction.EAST);

        helper.getLevel().setBlock(
                helper.absolutePos(packagerPos),
                APBlocks.ME_PACKAGER.get().defaultBlockState()
                        .setValue(AbstractHorizontalMachineBlock.FACING, Direction.WEST)
                        .setValue(MePackagerBlock.NETWORK_SIDE, Direction.EAST),
                3);
        PartHelper.setPart(
                (ServerLevel) helper.getLevel(),
                helper.absolutePos(cablePos),
                null,
                null,
                AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
        helper.getLevel().setBlock(
                helper.absolutePos(drivePos),
                AEBlocks.DRIVE.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);
        DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(drivePos);
        drive.getInternalInventory().addItems(AEItems.ITEM_CELL_64K.stack());
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        helper.startSequence()
                .thenWaitUntil(() -> assertMePackagerReady(helper, packager, "selected cable-side ME Packager test"))
                .thenExecute(() -> {
                    helper.assertTrue(packager.networkSide() == Direction.EAST,
                            "ME Packager should expose its AE node only on the selected cable side");
                    MEStorage storage = packager.getMainNode().getGrid().getStorageService().getInventory();
                    long inserted = storage.insert(
                            AEItemKey.of(Items.IRON_INGOT),
                            64,
                            Actionable.MODULATE,
                            IActionSource.ofMachine(packager));
                    helper.assertTrue(inserted == 64,
                            "Selected-side cable network should accept iron before ME Packager packs");

                    MePackagerBlockEntity.MachineResult result = packager.runOnce();
                    helper.assertTrue(result == MePackagerBlockEntity.MachineResult.PACKED,
                            "ME Packager should package from the AE grid connected through its selected cable side");
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "ME Packager should keep the output slot empty while the packing animation runs");
                    helper.assertTrue(storage.extract(
                            AEItemKey.of(Items.IRON_INGOT),
                            1,
                            Actionable.SIMULATE,
                            IActionSource.ofMachine(packager)) == 0,
                            "ME Packager should remove packaged iron from the selected cable network");
                })
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 2, () -> {
                    ItemStack output = packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).copy();
                    PackageData data = PackageDataStorage.read(output).orElseThrow();
                    helper.assertTrue(amountOf(data, AEItemKey.of(Items.IRON_INGOT)) == 64,
                            "Cable-side package should contain iron");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void mePackagerAutoUnpacksInputPackageOnServerTick(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(3, 1, 0);
        InterfaceBlockEntity aeInterface = placeMePackagerAe2Interface(helper, packagerPos, Direction.WEST);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAe2InterfaceReady(helper, aeInterface, "auto-unpack ME Packager test");
                    assertMePackagerReady(helper, packager, "auto-unpack ME Packager test");
                })
                .thenExecute(() -> helper.getLevel().setBlock(
                        helper.absolutePos(packagerPos.above()),
                        Blocks.REDSTONE_BLOCK.defaultBlockState(),
                        3))
                .thenExecute(() -> packager.getItems().setStackInSlot(
                        MePackagerBlockEntity.SLOT_INPUT,
                        packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64))))
                .thenExecuteAfter(3, () -> {
                    var storage = aeStorage(aeInterface);
                    var source = IActionSource.ofMachine(aeInterface);
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_INPUT).isEmpty(),
                            "ME Packager should automatically consume a package from its input slot");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 64,
                            "ME Packager should automatically unpack into the selected AE network");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void mePackagerRedstoneNeverOnlyStopsPacking(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(3, 1, 0);
        InterfaceBlockEntity aeInterface = placeMePackagerAe2Interface(helper, packagerPos, Direction.WEST);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);
        packager.getUpgrades().addItems(AEItems.REDSTONE_CARD.stack());
        packager.setRedstoneMode(MePackagerBlockEntity.RedstoneMode.NEVER);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAe2InterfaceReady(helper, aeInterface, "redstone-never unpack ME Packager test");
                    assertMePackagerReady(helper, packager, "redstone-never unpack ME Packager test");
                })
                .thenExecute(() -> packager.getItems().setStackInSlot(
                        MePackagerBlockEntity.SLOT_INPUT,
                        packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64))))
                .thenExecuteAfter(3, () -> {
                    var storage = aeStorage(aeInterface);
                    var source = IActionSource.ofMachine(aeInterface);
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_INPUT).isEmpty(),
                            "ME Packager should unpack even when packing activation is set to always off");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 64,
                            "Redstone-disabled packing mode should not block unpacking into the AE network");
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "Redstone-disabled packing mode should not immediately repack unpacked contents");
                })
                .thenExecuteAfter(MePackagerBlockEntity.CYCLIC_REDSTONE_INTERVAL_TICKS + 5, () ->
                        helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                                "Redstone-disabled packing mode should keep automatic packing off"))
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void mePackagerDoesNotUseForgeItemHandlerAsNetworkTarget(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(1, 1, 0);
        BlockPos chestPos = packagerPos.relative(Direction.WEST);
        helper.getLevel().setBlock(helper.absolutePos(chestPos), Blocks.CHEST.defaultBlockState(), 3);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
        chest.setChanged();
        helper.getLevel().setBlock(
                helper.absolutePos(packagerPos),
                APBlocks.ME_PACKAGER.get().defaultBlockState()
                        .setValue(AbstractHorizontalMachineBlock.FACING, Direction.EAST)
                        .setValue(MePackagerBlock.NETWORK_SIDE, Direction.WEST),
                3);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        MePackagerBlockEntity.MachineResult result = packager.runOnce();

        helper.assertTrue(result == MePackagerBlockEntity.MachineResult.NO_TARGET,
                "ME Packager should not fall back to a Forge item handler on its selected ME side");
        helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                "Forge item handler fallback should not create a package");
        helper.assertTrue(ironAmountInChest(chest) == 64,
                "Forge item handler fallback should not consume chest contents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mePackagerMenuCyclesRedstoneMode(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(0, 1, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(packagerPos),
                APBlocks.ME_PACKAGER.get().defaultBlockState(),
                3);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);
        FakePlayer player = newFakePlayer(helper);
        MePackagerMenu menu = new MePackagerMenu(4, new Inventory(player), packager);

        boolean clicked = menu.clickMenuButton(player, MePackagerMenu.BUTTON_REDSTONE_MODE);

        helper.assertTrue(clicked, "ME Packager menu should accept the redstone mode button");
        helper.assertTrue(packager.redstoneMode() == MePackagerBlockEntity.RedstoneMode.LOW_SIGNAL,
                "ME Packager redstone mode should cycle from high-signal to low-signal activation");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mePackagerFilterRowsFollowCapacityCards(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(0, 1, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(packagerPos),
                APBlocks.ME_PACKAGER.get().defaultBlockState(),
                3);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        helper.assertTrue(packager.unlockedFilterRows() == 2,
                "ME Packager should unlock two filter rows by default");
        packager.getUpgrades().addItems(AEItems.CAPACITY_CARD.stack());
        helper.assertTrue(packager.unlockedFilterRows() == 3,
                "One capacity card should unlock the third filter row");
        packager.getUpgrades().addItems(AEItems.CAPACITY_CARD.stack());
        packager.getUpgrades().addItems(AEItems.CAPACITY_CARD.stack());
        packager.getUpgrades().addItems(AEItems.CAPACITY_CARD.stack());
        helper.assertTrue(packager.unlockedFilterRows() == 5,
                "ME Packager should cap filter rows at five");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mePackagerPulseRedstoneRunsOnce(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(3, 1, 0);
        InterfaceBlockEntity aeInterface = placeMePackagerAe2Interface(helper, packagerPos, Direction.WEST);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);
        packager.getUpgrades().addItems(AEItems.REDSTONE_CARD.stack());
        packager.setRedstoneMode(MePackagerBlockEntity.RedstoneMode.PULSE);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAe2InterfaceReady(helper, aeInterface, "pulse redstone ME Packager test");
                    assertMePackagerReady(helper, packager, "pulse redstone ME Packager test");
                })
                .thenExecute(() -> {
                    var storage = aeStorage(aeInterface);
                    var source = IActionSource.ofMachine(aeInterface);
                    long inserted = storage.insert(AEItemKey.of(Items.IRON_INGOT), 640, Actionable.MODULATE, source);
                    helper.assertTrue(inserted == 640,
                            "Pulse redstone AE network should accept iron before power is applied");
                    helper.getLevel().setBlock(
                            helper.absolutePos(packagerPos.above()),
                            Blocks.REDSTONE_BLOCK.defaultBlockState(),
                            3);
                })
                .thenExecuteAfter(5, () -> {
                    var storage = aeStorage(aeInterface);
                    var source = IActionSource.ofMachine(aeInterface);
                    helper.assertTrue(packager.workingOperation() == MePackagerBlockEntity.WorkingOperation.PACKING,
                            "Pulse redstone should start a packing work cycle");
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "Pulse redstone should not expose output until the packing animation finishes");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 1, Actionable.SIMULATE, source) == 0,
                            "Pulse redstone should consume the AE source contents that fit the base profile");
                })
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 2, () -> {
                    ItemStack firstOutput = packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).copy();
                    PackageData firstData = PackageDataStorage.read(firstOutput).orElseThrow();
                    helper.assertTrue(amountOf(firstData, AEItemKey.of(Items.IRON_INGOT)) == 640,
                            "Pulse redstone should package the full 1k-capacity batch");
                    packager.getItems().setStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
                })
                .thenExecuteAfter(MePackagerBlockEntity.CYCLIC_REDSTONE_INTERVAL_TICKS + 5, () -> {
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "Pulse redstone should not keep running while power remains high");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void mePackagerCyclicRedstoneStopsWhenSourceIsEmpty(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(3, 1, 0);
        InterfaceBlockEntity aeInterface = placeMePackagerAe2Interface(helper, packagerPos, Direction.WEST);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);
        packager.getUpgrades().addItems(AEItems.REDSTONE_CARD.stack());
        packager.setRedstoneMode(MePackagerBlockEntity.RedstoneMode.HIGH_SIGNAL);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAe2InterfaceReady(helper, aeInterface, "cyclic redstone ME Packager test");
                    assertMePackagerReady(helper, packager, "cyclic redstone ME Packager test");
                })
                .thenExecute(() -> {
                    var storage = aeStorage(aeInterface);
                    var source = IActionSource.ofMachine(aeInterface);
                    long inserted = storage.insert(AEItemKey.of(Items.IRON_INGOT), 640, Actionable.MODULATE, source);
                    helper.assertTrue(inserted == 640,
                            "Cyclic redstone AE network should accept iron before power is applied");
                    helper.getLevel().setBlock(
                            helper.absolutePos(packagerPos.above()),
                            Blocks.REDSTONE_BLOCK.defaultBlockState(),
                            3);
                })
                .thenExecuteAfter(5, () -> {
                    helper.assertTrue(packager.workingOperation() == MePackagerBlockEntity.WorkingOperation.PACKING,
                            "Cyclic redstone should start a packing work cycle");
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "Cyclic redstone should not expose output until the packing animation finishes");
                })
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 2, () -> {
                    ItemStack firstOutput = packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).copy();
                    PackageData firstData = PackageDataStorage.read(firstOutput).orElseThrow();
                    helper.assertTrue(amountOf(firstData, AEItemKey.of(Items.IRON_INGOT)) == 640,
                            "Cyclic redstone should package the full base-capacity batch");
                    packager.getItems().setStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
                })
                .thenExecuteAfter(MePackagerBlockEntity.CYCLIC_REDSTONE_INTERVAL_TICKS + 5, () -> {
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "Cyclic redstone should not create a second package after the source is empty");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void meStoragePackPlanExtractsGenericContents(GameTestHelper helper) {
        MemoryMEStorage source = new MemoryMEStorage();
        source.add(AEItemKey.of(Items.IRON_INGOT), 64);
        source.add(AEItemKey.of(Items.COPPER_INGOT), 32);

        Optional<MEStoragePackagePlan> plan = MEStoragePackageTransactions.planPack(
                source,
                PackageColor.FLUIX,
                PackageCapacityProfile.DEFAULT,
                PackageFilter.any());

        helper.assertTrue(plan.isPresent(), "MEStorage should produce a package plan");
        helper.assertTrue(MEStoragePackageTransactions.canExtract(source, plan.get()),
                "MEStorage extraction should simulate");
        MEStoragePackageTransactions.commitExtract(source, plan.get());

        helper.assertTrue(source.amount(AEItemKey.of(Items.IRON_INGOT)) == 0, "Iron should be extracted");
        helper.assertTrue(source.amount(AEItemKey.of(Items.COPPER_INGOT)) == 0, "Copper should be extracted");
        helper.assertTrue(amountOf(plan.get().data(), AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Package should contain iron");
        helper.assertTrue(amountOf(plan.get().data(), AEItemKey.of(Items.COPPER_INGOT)) == 32,
                "Package should contain copper");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void meStorageUnpackInsertsAllContents(GameTestHelper helper) {
        PackageData data = PackageData.create(
                PackageColor.FLUIX,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        MemoryMEStorage target = new MemoryMEStorage();

        helper.assertTrue(MEStoragePackageTransactions.canInsertPackageContents(data, target),
                "MEStorage target should simulate accepting package contents");
        helper.assertTrue(MEStoragePackageTransactions.insertPackageContents(data, target),
                "MEStorage target should accept package contents");
        helper.assertTrue(target.amount(AEItemKey.of(Items.IRON_INGOT)) == 64, "Target should contain iron");
        helper.assertTrue(target.amount(AEItemKey.of(Items.COPPER_INGOT)) == 32, "Target should contain copper");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void meStoragePackPlanFlattensSourcePackages(GameTestHelper helper) {
        MemoryMEStorage source = new MemoryMEStorage();
        ItemStack sourcePackage = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64));
        AEItemKey packageKey = AEItemKey.of(sourcePackage);
        source.add(packageKey, 1);
        source.add(AEItemKey.of(Items.COPPER_INGOT), 32);

        Optional<MEStoragePackagePlan> plan = MEStoragePackageTransactions.planPack(
                source,
                PackageColor.BLUE,
                PackageCapacityProfile.DEFAULT,
                PackageFilter.any());

        helper.assertTrue(plan.isPresent(), "MEStorage should plan from an existing package key");
        MEStoragePackageTransactions.commitExtract(source, plan.get());
        helper.assertTrue(source.amount(packageKey) == 0, "Source package should be extracted");
        helper.assertTrue(amountOf(plan.get().data(), AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Existing package contents should be flattened");
        helper.assertTrue(amountOf(plan.get().data(), AEItemKey.of(Items.COPPER_INGOT)) == 32,
                "Loose MEStorage contents should be merged");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void meStoragePackPlanClearsMarkerFromExplicitMode(GameTestHelper helper) {
        MemoryMEStorage source = new MemoryMEStorage();
        ItemStack sourcePackage = packageStack(
                PackageColor.RED,
                markedIronPackageData(PackageColor.RED, Items.GOLD_INGOT));
        source.add(AEItemKey.of(sourcePackage), 1);

        Optional<MEStoragePackagePlan> plan = MEStoragePackageTransactions.planPack(
                source,
                PackageColor.BLUE,
                PackageCapacityProfile.DEFAULT,
                PackageFilter.any(),
                MarkerMergeMode.CLEAR,
                Optional.empty());

        helper.assertTrue(plan.isPresent(), "MEStorage explicit clear mode should package marked source packages");
        helper.assertTrue(plan.get().data().marker().isEmpty(), "MEStorage explicit clear mode should remove the marker");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fluidHandlerPackPlanExtractsFluidContents(GameTestHelper helper) {
        FluidTank source = new FluidTank(4000);
        source.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);

        Optional<FluidPackagePlan> plan = FluidPackageTransactions.planPack(
                source,
                PackageColor.FLUIX,
                PackageCapacityProfile.DEFAULT,
                PackageFilter.any(),
                MarkerMergeMode.RETAIN,
                Optional.empty());

        helper.assertTrue(plan.isPresent(), "Fluid handler should produce a package plan");
        helper.assertTrue(FluidPackageTransactions.canExtract(source, plan.get()),
                "Fluid handler extraction should simulate");
        FluidPackageTransactions.commitExtract(source, plan.get());

        helper.assertTrue(source.getFluidAmount() == 0, "Source tank should be drained");
        helper.assertTrue(amountOf(plan.get().data(), AEFluidKey.of(Fluids.WATER)) == 2000,
                "Package should contain the drained water");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fluidHandlerUnpackInsertsAllContents(GameTestHelper helper) {
        PackageData data = PackageData.create(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEFluidKey.of(Fluids.WATER), 1000)),
                Optional.empty(),
                0);
        FluidTank target = new FluidTank(1000);

        helper.assertTrue(FluidPackageTransactions.canInsertPackageContents(data, target),
                "Fluid target should simulate accepting all package contents");
        helper.assertTrue(FluidPackageTransactions.insertPackageContents(data, target, false),
                "Fluid target should accept all package contents");
        helper.assertTrue(target.getFluidAmount() == 1000
                        && target.getFluid().isFluidEqual(new FluidStack(Fluids.WATER, 1000)),
                "Target tank should contain one bucket of water");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fluidHandlerUnpackRejectsFullTarget(GameTestHelper helper) {
        PackageData data = PackageData.create(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEFluidKey.of(Fluids.WATER), 1000)),
                Optional.empty(),
                0);
        FluidTank target = new FluidTank(1000);
        target.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);

        helper.assertFalse(FluidPackageTransactions.canInsertPackageContents(data, target),
                "Full incompatible fluid target should reject complete package contents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerCreatesPackageFromInputBuffer(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        assembler.getItems().setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 32));

        PackageAssemblerBlockEntity.AssemblyResult result = assembler.tryAssemble();
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);

        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.NO_CONTENTS,
                "Assembler should not accept loose input without a pattern");
        helper.assertTrue(output.isEmpty(), "Assembler should not create output without a pattern");
        helper.assertTrue(assembler.getItems().getStackInSlot(0).getCount() == 64,
                "Rejected loose input should remain in the input buffer");
        helper.assertTrue(assembler.getItems().getStackInSlot(1).getCount() == 32,
                "Rejected loose input should remain in the input buffer");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerKeepsInputsWhenOutputBlocked(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        fillAssemblerOutputSlots(assembler);

        PackageAssemblerBlockEntity.AssemblyResult result = assembler.tryAssemble();

        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.OUTPUT_BLOCKED,
                "Assembler should not assemble into a blocked output");
        helper.assertTrue(assembler.getItems().getStackInSlot(0).getCount() == 64,
                "Blocked assembler should not consume input");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerFlattensInputPackages(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData target = PackageData.create(
                PackageColor.FLUIX,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(pattern, PackageColor.FLUIX, target);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.getItems().setStackInSlot(0, packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64)));
        assembler.getItems().setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 32));

        PackageAssemblerBlockEntity.AssemblyResult result = assembler.tryAssemble();
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData data = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should accept a source package");
        helper.assertTrue(amountOf(data, AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Flattened source package should contribute iron");
        helper.assertTrue(amountOf(data, AEItemKey.of(Items.COPPER_INGOT)) == 32,
                "Loose input should contribute copper");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerUsesEncodedPackagePattern(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData data = PackageData.create(
                PackageColor.FLUIX,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(pattern, PackageColor.FLUIX, data);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        assembler.getItems().setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 32));

        PackageAssemblerBlockEntity.AssemblyResult result = assembler.tryAssemble();
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should assemble a matching encoded pattern");
        helper.assertTrue(!assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN).isEmpty(),
                "Encoded pattern should remain in the pattern slot");
        helper.assertTrue(outputData.canonicalHash().equals(data.canonicalHash()),
                "Output package should match the encoded pattern");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerUsesAe2BlankPatternCarrier(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData data = ironPackageData(PackageColor.BLUE, 64);
        ItemStack pattern = ae2Item("blank_pattern");
        helper.assertFalse(pattern.isEmpty(), "AE2 blank pattern should be registered");
        PackagePatternDataStorage.write(pattern, PackageColor.BLUE, data);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));

        PackageAssemblerBlockEntity.AssemblyResult result = assembler.tryAssemble();
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should read package pattern data from an AE2 blank pattern carrier");
        helper.assertTrue(output.is(APItems.packageItems().get(PackageColor.BLUE).get()),
                "Assembler should use the encoded carrier color");
        helper.assertTrue(outputData.canonicalHash().equals(data.canonicalHash()),
                "Output package should match the AE2-carried pattern");
        helper.assertTrue(PackagePatternDataStorage.isAe2BlankPattern(
                        assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN)),
                "AE2 pattern carrier should remain in the pattern slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageCraftingPatternDecodesAsAssemblerOnlyPattern(GameTestHelper helper) {
        MarkerSpec marker = new MarkerSpec(new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
        ItemStack pattern = packageCraftingPattern(
                PackageColor.PURPLE,
                Optional.of(marker),
                "Ore Kit",
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32));

        helper.assertTrue(PatternDetailsHelper.isEncodedPattern(pattern),
                "Package crafting pattern should be an AE2 encoded pattern");
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        helper.assertTrue(details instanceof PackageCraftingPatternDetails,
                "Package crafting pattern should decode to AP package pattern details");
        helper.assertFalse(details instanceof IMolecularAssemblerSupportedPattern,
                "Package crafting pattern should not be accepted by the Molecular Assembler");
        helper.assertFalse(details.supportsPushInputsToExternalInventory(),
                "Package crafting pattern should only be pushed to its pattern machine");
        helper.assertTrue(details.getOutputs().length == 1,
                "Package crafting pattern should expose one computed package output");
        helper.assertTrue(details.getOutputs()[0].what() instanceof AEItemKey,
                "Package crafting output should be an item key");
        AEItemKey outputKey = (AEItemKey) details.getOutputs()[0].what();
        ItemStack output = outputKey.toStack();
        PackageData data = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(output.is(APItems.packageItems().get(PackageColor.PURPLE).get()),
                "Computed package output should use the encoded color");
        helper.assertTrue(output.getHoverName().getString().equals("Ore Kit"),
                "Computed package output should use the encoded package name");
        helper.assertTrue(data.marker().isPresent() && data.marker().orElseThrow().sameAs(marker),
                "Computed package output should use the encoded marker");
        helper.assertTrue(amountOf(data, AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Computed package output should contain iron");
        helper.assertTrue(amountOf(data, AEItemKey.of(Items.COPPER_INGOT)) == 32,
                "Computed package output should contain copper");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerUsesAe2PackageCraftingPattern(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        MarkerSpec marker = new MarkerSpec(new GenericStack(AEItemKey.of(Items.EMERALD), 1));
        ItemStack pattern = packageCraftingPattern(
                PackageColor.RED,
                Optional.of(marker),
                "Red Ore Kit",
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32));
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        assembler.getItems().setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 32));

        PackageAssemblerBlockEntity.AssemblyResult result = assembler.tryAssemble();
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should execute an AE2-carried package crafting pattern");
        helper.assertTrue(output.is(APItems.packageItems().get(PackageColor.RED).get()),
                "Assembler should use the package crafting pattern color");
        helper.assertTrue(output.getHoverName().getString().equals("Red Ore Kit"),
                "Assembler should use the package crafting pattern name");
        helper.assertTrue(outputData.marker().isPresent() && outputData.marker().orElseThrow().sameAs(marker),
                "Assembler should use the package crafting pattern marker");
        helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Assembler output should contain the encoded iron input");
        helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.COPPER_INGOT)) == 32,
                "Assembler output should contain the encoded copper input");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerUsesLargeEncodedPackagePattern(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData data = PackageData.create(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 640)),
                Optional.empty(),
                0);
        ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(pattern, PackageColor.FLUIX, data);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.getItems().setStackInSlot(0, packageStack(PackageColor.FLUIX, data));

        PackageAssemblerBlockEntity.AssemblyResult result = assembler.tryAssemble();
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);

        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should assemble encoded source-package patterns larger than default capacity");
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();
        helper.assertTrue(outputData.canonicalHash().equals(data.canonicalHash()),
                "Large output package should match the encoded pattern");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerUsesCapacitySlotForLargeSourcePackage(GameTestHelper helper) {
        ItemStack component = ae2Item("cell_component_64k");
        helper.assertFalse(component.isEmpty(), "AE2 64k storage component should be registered");
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData sourceData = PackageData.create(
                PackageColor.RED,
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 640)),
                Optional.empty(),
                0);
        ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(pattern, PackageColor.RED, sourceData);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.getItems().setStackInSlot(0, packageStack(PackageColor.RED, sourceData));
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY, component);

        PackageAssemblerBlockEntity.AssemblyResult result = assembler.tryAssemble();
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should use its capacity slot when repacking a large source package");
        helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.IRON_INGOT)) == 640,
                "64k capacity should allow ten iron stack units");
        helper.assertTrue(!assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY).isEmpty(),
                "Assembler should not consume the capacity slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerPackageCraftingProviderPushUsesLargeEncodedPackage(GameTestHelper helper) {
        ItemStack component = ae2Item("cell_component_64k");
        helper.assertFalse(component.isEmpty(), "AE2 64k storage component should be registered");
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY, component);
        ItemStack pattern = packageCraftingPattern(
                PackageColor.BLUE,
                Optional.empty(),
                "",
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 640));
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        helper.assertTrue(details instanceof PackageCraftingPatternDetails,
                "AE2 should decode the large package crafting pattern");
        KeyCounter iron = new KeyCounter();
        iron.add(AEItemKey.of(Items.IRON_INGOT), 640);

        boolean accepted = assembler.pushPattern(details, new KeyCounter[] { iron }, Direction.UP);
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(accepted,
                "Assembler should accept large package crafting Pattern Provider pushes");
        helper.assertTrue(iron.isEmpty(), "Accepted large package crafting push should consume the input holder");
        helper.assertTrue(output.is(APItems.packageItems().get(PackageColor.BLUE).get()),
                "Large package crafting push should use the encoded package color");
        helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.IRON_INGOT)) == 640,
                "Large package crafting push should preserve all iron");
        helper.assertTrue(!assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY).isEmpty(),
                "Package crafting push should not consume the capacity slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageCraftingPatternRejectsContentsBeyondPackageCapacity(GameTestHelper helper) {
        GenericStack[] inputs = sparsePackageCraftingInputs(
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64L * 257L));

        helper.assertTrue(PackageCraftingPatternDataStorage.create(
                        PackageColor.FLUIX,
                        inputs,
                        Optional.empty(),
                        "")
                .isEmpty(), "Package crafting pattern should reject contents beyond package capacity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerColoredPatternProviderPushUsesCapacitySlot(GameTestHelper helper) {
        ItemStack component = ae2Item("cell_component_64k");
        helper.assertFalse(component.isEmpty(), "AE2 64k storage component should be registered");
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY, component);
        ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.IRON_INGOT), 640) },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        ColoredProcessingPatternDataStorage.write(pattern, Map.of(0, PackageColor.RED));
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        helper.assertTrue(details != null, "AE2 should decode the large colored processing pattern");
        KeyCounter iron = new KeyCounter();
        iron.add(AEItemKey.of(Items.IRON_INGOT), 640);

        boolean accepted = assembler.pushPattern(details, new KeyCounter[] { iron }, Direction.UP);
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(accepted,
                "Assembler should package large colored Pattern Provider pushes with the configured capacity slot");
        helper.assertTrue(iron.isEmpty(), "Accepted large colored push should consume the input holder");
        helper.assertTrue(output.is(APItems.packageItems().get(PackageColor.RED).get()),
                "Large colored push should use the slot color");
        helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.IRON_INGOT)) == 640,
                "Large colored Pattern Provider push should preserve all iron");
        helper.assertTrue(!assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY).isEmpty(),
                "Colored Pattern Provider push should not consume the capacity slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerAcceptsColoredFluidPatternProviderPush(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] { new GenericStack(AEFluidKey.of(Fluids.WATER), 1000) },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        ColoredProcessingPatternDataStorage.write(pattern, Map.of(0, PackageColor.BLUE));
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        helper.assertTrue(details != null, "AE2 should decode the colored fluid processing pattern");
        KeyCounter water = new KeyCounter();
        water.add(AEFluidKey.of(Fluids.WATER), 1000);

        boolean accepted = assembler.pushPattern(details, new KeyCounter[] { water }, Direction.UP);
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(accepted, "Assembler should accept colored fluid Pattern Provider pushes");
        helper.assertTrue(water.isEmpty(), "Accepted colored fluid push should consume the input holder");
        helper.assertTrue(output.is(APItems.packageItems().get(PackageColor.BLUE).get()),
                "Colored fluid push should use the configured slot color");
        helper.assertTrue(amountOf(outputData, AEFluidKey.of(Fluids.WATER)) == 1000,
                "Colored fluid push should package water");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerUsesPackagedProcessingPattern(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData iron = ironPackageData(PackageColor.FLUIX, 64);
        PackageData copper = PackageData.create(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        ItemStack pattern = new ItemStack(APItems.PACKAGED_PROCESSING_PATTERN.get());
        PackagedProcessingPatternDataStorage.write(pattern, PackageColor.FLUIX, List.of(iron, copper));
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        assembler.getItems().setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 32));

        PackageAssemblerBlockEntity.AssemblyResult firstResult = assembler.tryAssemble();
        ItemStack firstOutput = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
        PackageData firstData = PackageDataStorage.read(firstOutput).orElseThrow();
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        PackageAssemblerBlockEntity.AssemblyResult secondResult = assembler.tryAssemble();
        ItemStack secondOutput = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
        PackageData secondData = PackageDataStorage.read(secondOutput).orElseThrow();

        helper.assertTrue(firstResult == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should produce the first processing package");
        helper.assertTrue(secondResult == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should produce the second processing package after output is cleared");
        helper.assertTrue(firstData.canonicalHash().equals(iron.canonicalHash()),
                "First output should match the first processing package");
        helper.assertTrue(secondData.canonicalHash().equals(copper.canonicalHash()),
                "Second output should match the second processing package");
        helper.assertTrue(assembler.getItems().getStackInSlot(0).isEmpty(), "Iron input should be consumed");
        helper.assertTrue(assembler.getItems().getStackInSlot(1).isEmpty(), "Copper input should be consumed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerUsesAe2PackagedProcessingCarrier(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData iron = ironPackageData(PackageColor.RED, 64);
        PackageData copper = PackageData.create(
                PackageColor.RED,
                List.of(new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        ItemStack pattern = ae2Item("blank_pattern");
        helper.assertFalse(pattern.isEmpty(), "AE2 blank pattern should be registered");
        PackagedProcessingPatternDataStorage.write(pattern, PackageColor.RED, List.of(iron, copper));
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        assembler.getItems().setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 32));

        PackageAssemblerBlockEntity.AssemblyResult firstResult = assembler.tryAssemble();
        ItemStack firstOutput = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
        PackageData firstData = PackageDataStorage.read(firstOutput).orElseThrow();
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        PackageAssemblerBlockEntity.AssemblyResult secondResult = assembler.tryAssemble();
        ItemStack secondOutput = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
        PackageData secondData = PackageDataStorage.read(secondOutput).orElseThrow();

        helper.assertTrue(firstResult == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should produce the first AE2-carried processing package");
        helper.assertTrue(secondResult == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should produce the second AE2-carried processing package");
        helper.assertTrue(firstOutput.is(APItems.packageItems().get(PackageColor.RED).get()),
                "AE2-carried processing pattern should keep its encoded color");
        helper.assertTrue(firstData.canonicalHash().equals(iron.canonicalHash()),
                "First AE2-carried output should match the first processing package");
        helper.assertTrue(secondData.canonicalHash().equals(copper.canonicalHash()),
                "Second AE2-carried output should match the second processing package");
        helper.assertTrue(PackagePatternDataStorage.isAe2BlankPattern(
                        assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN)),
                "AE2 packaged-processing carrier should remain in the pattern slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerUsesAdditionalOutputSlots(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData iron = ironPackageData(PackageColor.FLUIX, 64);
        PackageData copper = PackageData.create(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        ItemStack pattern = new ItemStack(APItems.PACKAGED_PROCESSING_PATTERN.get());
        PackagedProcessingPatternDataStorage.write(pattern, PackageColor.FLUIX, List.of(iron, copper));
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        assembler.getItems().setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 32));

        PackageAssemblerBlockEntity.AssemblyResult firstResult = assembler.tryAssemble();
        PackageAssemblerBlockEntity.AssemblyResult secondResult = assembler.tryAssemble();
        ItemStack firstOutput = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        ItemStack secondOutput = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.outputHandlerSlot(1));

        helper.assertTrue(firstResult == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should produce the first processing package");
        helper.assertTrue(secondResult == PackageAssemblerBlockEntity.AssemblyResult.OUTPUT_BLOCKED,
                "Assembler should not start another craft while any output slot is occupied");
        helper.assertTrue(PackageDataStorage.read(firstOutput).orElseThrow().canonicalHash().equals(iron.canonicalHash()),
                "First output slot should contain the first package");
        helper.assertTrue(secondOutput.isEmpty(),
                "Second output slot should stay empty until the first output is cleared");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerAcceptsAe2EncodedPackagedProcessingPush(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData iron = ironPackageData(PackageColor.RED, 64);
        PackageData copper = PackageData.create(
                PackageColor.RED,
                List.of(new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] {
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)
                },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 2) });
        PackagedProcessingPatternDataStorage.write(
                pattern,
                PackageColor.RED,
                List.of(iron, copper),
                List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 2)));
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        helper.assertTrue(details != null, "AE2 should decode the packaged-processing carrier");
        helper.assertTrue(details.getOutputs().length == 1,
                "AE2 should expose the packaged-processing output to the planner");
        KeyCounter ironInput = new KeyCounter();
        ironInput.add(AEItemKey.of(Items.IRON_INGOT), 64);
        KeyCounter copperInput = new KeyCounter();
        copperInput.add(AEItemKey.of(Items.COPPER_INGOT), 32);

        boolean accepted = assembler.pushPattern(details, new KeyCounter[] { ironInput, copperInput }, Direction.UP);
        ItemStack firstOutput = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
        PackageData firstData = PackageDataStorage.read(firstOutput).orElseThrow();
        ItemStack secondOutput =
                assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.outputHandlerSlot(1)).copy();
        PackageData secondData = PackageDataStorage.read(secondOutput).orElseThrow();

        helper.assertTrue(accepted,
                "Assembler should accept AE2 encoded packaged-processing pushes");
        helper.assertTrue(ironInput.isEmpty(), "Accepted packaged-processing push should consume iron input");
        helper.assertTrue(copperInput.isEmpty(), "Accepted packaged-processing push should consume copper input");
        helper.assertTrue(firstOutput.is(APItems.packageItems().get(PackageColor.RED).get()),
                "First packaged-processing output should use the encoded color");
        helper.assertTrue(firstData.canonicalHash().equals(iron.canonicalHash()),
                "First packaged-processing output should match the first package");
        helper.assertTrue(!secondOutput.isEmpty(),
                "Assembler should put the second packaged-processing package in the next output slot");
        helper.assertTrue(secondData.canonicalHash().equals(copper.canonicalHash()),
                "Second packaged-processing output should match the second package");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerAcceptsFluidPackagedProcessingPush(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData waterPackage = PackageData.create(
                PackageColor.BLUE,
                List.of(new GenericStack(AEFluidKey.of(Fluids.WATER), 1000)),
                Optional.empty(),
                0);
        ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] { new GenericStack(AEFluidKey.of(Fluids.WATER), 1000) },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        PackagedProcessingPatternDataStorage.write(
                pattern,
                PackageColor.BLUE,
                List.of(waterPackage),
                List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)));
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        helper.assertTrue(details != null, "AE2 should decode the fluid packaged-processing carrier");
        KeyCounter water = new KeyCounter();
        water.add(AEFluidKey.of(Fluids.WATER), 1000);

        boolean accepted = assembler.pushPattern(details, new KeyCounter[] { water }, Direction.UP);
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(accepted,
                "Assembler should accept packaged-processing pushes with fluid package contents");
        helper.assertTrue(water.isEmpty(),
                "Accepted fluid packaged-processing push should consume water input");
        helper.assertTrue(output.is(APItems.packageItems().get(PackageColor.BLUE).get()),
                "Fluid packaged-processing output should use the encoded color");
        helper.assertTrue(outputData.canonicalHash().equals(waterPackage.canonicalHash()),
                "Fluid packaged-processing output should match the encoded package");
        helper.assertTrue(amountOf(outputData, AEFluidKey.of(Fluids.WATER)) == 1000,
                "Fluid packaged-processing output should contain water");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerAcceptsPackageCraftingPatternProviderPush(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        ItemStack pattern = packageCraftingPattern(
                PackageColor.FLUIX,
                Optional.empty(),
                "",
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32));
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        helper.assertTrue(details instanceof PackageCraftingPatternDetails,
                "AE2 should decode the package crafting pattern");
        KeyCounter iron = new KeyCounter();
        iron.add(AEItemKey.of(Items.IRON_INGOT), 64);
        KeyCounter copper = new KeyCounter();
        copper.add(AEItemKey.of(Items.COPPER_INGOT), 32);

        helper.assertTrue(assembler.acceptsPlans(), "Empty assembler should accept Pattern Provider plans");
        boolean accepted = assembler.pushPattern(details, new KeyCounter[] { iron, copper }, Direction.UP);
        helper.assertTrue(accepted, "Assembler should accept package crafting Pattern Provider inputs");
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(iron.isEmpty(), "Accepted push should consume the iron input holder");
        helper.assertTrue(copper.isEmpty(), "Accepted push should consume the copper input holder");
        helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Pushed iron should be packaged");
        helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.COPPER_INGOT)) == 32,
                "Pushed copper should be packaged");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerSplitsColoredProcessingPatternPush(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] {
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32),
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32)
                },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        ColoredProcessingPatternDataStorage.write(pattern, Map.of(0, PackageColor.RED, 1, PackageColor.BLUE));
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        helper.assertTrue(details != null, "AE2 should decode the colored processing pattern");
        KeyCounter iron = new KeyCounter();
        iron.add(AEItemKey.of(Items.IRON_INGOT), 64);

        boolean accepted = assembler.pushPattern(details, new KeyCounter[] { iron }, Direction.UP);
        ItemStack firstOutput = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
        PackageData firstData = PackageDataStorage.read(firstOutput).orElseThrow();
        ItemStack secondOutput =
                assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.outputHandlerSlot(1)).copy();
        PackageData secondData = PackageDataStorage.read(secondOutput).orElseThrow();

        helper.assertTrue(accepted, "Assembler should accept colored processing pattern pushes");
        helper.assertTrue(iron.isEmpty(), "Colored push should consume the aggregated input holder");
        helper.assertTrue(firstOutput.is(APItems.packageItems().get(PackageColor.RED).get()),
                "First colored package should use the first slot color");
        helper.assertTrue(!secondOutput.isEmpty(),
                "Assembler should put the second colored package in the next output slot");
        helper.assertTrue(secondOutput.is(APItems.packageItems().get(PackageColor.BLUE).get()),
                "Second colored package should use the second slot color");
        helper.assertTrue(amountOf(firstData, AEItemKey.of(Items.IRON_INGOT)) == 32,
                "First colored package should contain the first iron requirement");
        helper.assertTrue(amountOf(secondData, AEItemKey.of(Items.IRON_INGOT)) == 32,
                "Second colored package should contain the second iron requirement");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerRejectsPatternProviderPushWhenOutputBlocked(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        fillAssemblerOutputSlots(assembler);
        ItemStack pattern = packageCraftingPattern(
                PackageColor.FLUIX,
                Optional.empty(),
                "",
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64));
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        helper.assertTrue(details instanceof PackageCraftingPatternDetails,
                "AE2 should decode the package crafting pattern");
        KeyCounter iron = new KeyCounter();
        iron.add(AEItemKey.of(Items.IRON_INGOT), 64);

        boolean accepted = assembler.pushPattern(details, new KeyCounter[] { iron }, Direction.UP);

        helper.assertFalse(accepted, "Blocked output should reject Pattern Provider pushes");
        helper.assertTrue(iron.get(AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Rejected push should not consume input holders");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerRejectsUnmarkedFluidPatternProviderPush(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        KeyCounter water = new KeyCounter();
        water.add(AEFluidKey.of(Fluids.WATER), 1000);

        boolean accepted = assembler.pushPattern(
                new DummyPatternDetails(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)),
                new KeyCounter[] { water },
                Direction.UP);
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);

        helper.assertFalse(accepted, "Assembler should reject unmarked generic Pattern Provider fluid inputs");
        helper.assertTrue(water.get(AEFluidKey.of(Fluids.WATER)) == 1000,
                "Rejected unmarked fluid push should not consume input holders");
        helper.assertTrue(output.isEmpty(), "Rejected unmarked fluid push should not create output");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerLoadsLegacyElevenSlotInventory(GameTestHelper helper) {
        ItemStackHandler legacyItems = new ItemStackHandler(11);
        legacyItems.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 16));
        CompoundTag tag = new CompoundTag();
        tag.put("items", legacyItems.serializeNBT());
        PackageAssemblerBlockEntity assembler = newPackageAssembler();

        assembler.load(tag);

        helper.assertTrue(assembler.getItems().getSlots() == PackageAssemblerBlockEntity.SLOT_MARKER + 1,
                "Assembler should keep its current slot count when loading legacy NBT");
        helper.assertTrue(assembler.getItems().getStackInSlot(0).is(Items.IRON_INGOT),
                "Assembler should preserve legacy input slots");
        helper.assertTrue(assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY).isEmpty(),
                "Assembler should add an empty capacity slot for legacy NBT");
        helper.assertTrue(assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_MARKER).isEmpty(),
                "Assembler should add an empty marker slot for legacy NBT");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerMenuTogglesAutoExport(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = placePackageAssembler(helper, new BlockPos(0, 0, 0), Direction.NORTH);
        FakePlayer player = newFakePlayer(helper);
        PackageAssemblerMenu menu = new PackageAssemblerMenu(4, new Inventory(player), assembler);

        helper.assertTrue(menu.outputMode() == PackageAssemblerBlockEntity.OutputMode.ME_NETWORK,
                "Assembler output mode should default to ME network");
        boolean clicked = menu.clickMenuButton(player, PackageAssemblerMenu.BUTTON_AUTO_EXPORT);

        helper.assertTrue(clicked, "Assembler menu should accept the output mode button");
        helper.assertTrue(assembler.outputMode() == PackageAssemblerBlockEntity.OutputMode.ADJACENT_BLOCK,
                "Output mode button should cycle to adjacent block output");
        helper.assertTrue(menu.outputMode() == PackageAssemblerBlockEntity.OutputMode.ADJACENT_BLOCK,
                "Output mode button should update the synced menu state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerMenuInputUsesPatternFilterAndLargeAmount(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData target = PackageData.create(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 640)),
                Optional.empty(),
                0);
        ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(pattern, PackageColor.FLUIX, target);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY, ae2Item("cell_component_16k"));
        FakePlayer player = newFakePlayer(helper);
        Inventory inventory = new Inventory(player);
        PackageAssemblerMenu menu = new PackageAssemblerMenu(5, inventory, assembler);
        int firstHotbarSlot = menu.hotbarMenuSlotIndex(0);

        for (int stack = 0; stack < 10; stack++) {
            inventory.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
            ItemStack moved = menu.quickMoveStack(player, firstHotbarSlot);
            helper.assertTrue(moved.is(Items.IRON_INGOT),
                    "Menu shift-click should move matching iron into the assembler input buffer");
        }
        inventory.setItem(0, new ItemStack(Items.COPPER_INGOT, 64));
        ItemStack rejected = menu.quickMoveStack(player, firstHotbarSlot);
        PackageAssemblerBlockEntity.AssemblyResult result = assembler.tryAssemble();
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(rejected.isEmpty(), "Pattern filter should reject non-matching copper input");
        helper.assertTrue(inventory.getItem(0).is(Items.COPPER_INGOT),
                "Rejected copper should remain in the player inventory");
        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should assemble the large menu input when it matches the pattern");
        helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.IRON_INGOT)) == 640,
                "Output package should contain the full 640 iron input amount");
        helper.assertTrue(assembler.menuInputDisplay(0).isEmpty(),
                "Menu input buffer should be consumed after assembly");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerMenuInputInvalidAfterPatternRemoved(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData target = ironPackageData(PackageColor.FLUIX, 64);
        ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(pattern, PackageColor.FLUIX, target);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.insertMenuInput(0, new ItemStack(Items.IRON_INGOT, 64), 64, false);

        helper.assertTrue(assembler.isMenuInputSlotEnabled(0),
                "Menu input slot with matching pattern should be enabled");
        helper.assertTrue(assembler.isMenuInputSlotValid(0),
                "Menu input slot with matching pattern should be valid");

        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, ItemStack.EMPTY);

        helper.assertTrue(assembler.isMenuInputSlotEnabled(0),
                "Menu input slot with retained contents should stay enabled after pattern removal");
        helper.assertFalse(assembler.isMenuInputSlotValid(0),
                "Menu input slot with retained contents should become invalid after pattern removal");
        helper.assertFalse(assembler.isMenuInputSlotEnabled(1),
                "Empty menu input slots should lock again after pattern removal");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerUsesConfiguredPackageIdentity(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        assembler.setSelectedColor(PackageColor.RED);
        assembler.setPackageName("Assembler Batch");
        PackageData target = ironPackageData(PackageColor.RED, 64);
        ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(pattern, PackageColor.RED, target);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_MARKER, new ItemStack(Items.DIAMOND));
        assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));

        PackageAssemblerBlockEntity.AssemblyResult result = assembler.tryAssemble();
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();
        MarkerSpec expectedMarker = new MarkerSpec(new GenericStack(AEItemKey.of(Items.DIAMOND), 1));

        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should assemble using configured package identity");
        helper.assertTrue(output.is(APItems.packageItems().get(PackageColor.RED).get()),
                "Assembler output item should use the selected package color");
        helper.assertTrue(output.hasCustomHoverName()
                        && "Assembler Batch".equals(output.getHoverName().getString()),
                "Assembler output should use the configured package name");
        helper.assertTrue(outputData.marker().map(marker -> marker.sameAs(expectedMarker)).orElse(false),
                "Assembler output should use the configured marker item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerMenuUsesFourByFourInputAndFourOutputWindow(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData target = PackageData.create(
                PackageColor.FLUIX,
                List.of(
                        new GenericStack(AEItemKey.of(Items.COAL), 1),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 1),
                        new GenericStack(AEItemKey.of(Items.DIAMOND), 1),
                        new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1),
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32)),
                Optional.empty(),
                0);
        ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(pattern, PackageColor.FLUIX, target);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        FakePlayer player = newFakePlayer(helper);
        PackageAssemblerMenu menu = new PackageAssemblerMenu(6, new Inventory(player), assembler);
        ItemStack secondRowPackage = packageStack(PackageColor.FLUIX, ironPackageData(PackageColor.FLUIX, 16));
        ItemStack fifthFilter = assembler.menuInputFilterDisplay(PackageAssemblerBlockEntity.MENU_INPUT_COLUMNS);

        assembler.insertMenuInput(
                PackageAssemblerBlockEntity.MENU_INPUT_COLUMNS,
                fifthFilter,
                fifthFilter.getCount(),
                false);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.outputHandlerSlot(1), secondRowPackage);
        menu.setScrollOffset(1);

        helper.assertTrue(PackageAssemblerMenu.MENU_INPUT_END - PackageAssemblerMenu.MENU_INPUT_START == 16,
                "Assembler menu should expose a 4x4 visible input window");
        helper.assertTrue(PackageAssemblerMenu.OUTPUT_END - PackageAssemblerMenu.OUTPUT_START == 4,
                "Assembler menu should expose four visible output slots");
        helper.assertTrue(menu.maxScrollOffset() == PackageAssemblerBlockEntity.OUTPUT_SLOT_COUNT - PackageAssemblerMenu.VISIBLE_ROWS,
                "Assembler menu scroll range should follow the output rows");
        helper.assertTrue(menu.getSlot(menu.menuInputMenuSlotIndex(0)).getItem().is(fifthFilter.getItem()),
                "Scrolled 4x4 input window should map its first visible slot to the next input row");
        helper.assertTrue(menu.getSlot(menu.outputMenuSlotIndex(0)).getItem().is(secondRowPackage.getItem()),
                "Scrolled output window should map its first visible slot to the matching output row");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerAutoExportSettingPersists(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        assembler.setAutoExport(false);
        CompoundTag tag = assembler.saveWithoutMetadata();
        PackageAssemblerBlockEntity loaded = newPackageAssembler();

        loaded.load(tag);

        helper.assertFalse(loaded.autoExport(), "Assembler auto-export setting should persist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerExternalHandlerExtractsOnePackageInOrder(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        ItemStack first = packageStack(PackageColor.FLUIX, ironPackageData(PackageColor.FLUIX, 16));
        first.setCount(2);
        ItemStack second = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 16));
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT, first);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.outputHandlerSlot(1), second);
        IItemHandler handler = assembler.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                .orElseThrow(IllegalStateException::new);

        ItemStack skipped = handler.extractItem(PackageAssemblerBlockEntity.outputHandlerSlot(1), 64, false);
        ItemStack nonOutput = handler.extractItem(PackageAssemblerBlockEntity.SLOT_PATTERN, 64, false);
        ItemStack firstExtract = handler.extractItem(PackageAssemblerBlockEntity.SLOT_OUTPUT, 64, false);
        ItemStack secondExtract = handler.extractItem(PackageAssemblerBlockEntity.SLOT_OUTPUT, 64, false);
        ItemStack thirdExtract = handler.extractItem(PackageAssemblerBlockEntity.outputHandlerSlot(1), 64, false);

        helper.assertTrue(skipped.isEmpty(), "External handler should not skip earlier output slots");
        helper.assertTrue(nonOutput.isEmpty(), "External handler should not extract non-output slots");
        helper.assertTrue(firstExtract.getCount() == 1 && firstExtract.is(APItems.packageItems().get(PackageColor.FLUIX).get()),
                "External handler should extract one package from the first output slot");
        helper.assertTrue(secondExtract.getCount() == 1 && secondExtract.is(APItems.packageItems().get(PackageColor.FLUIX).get()),
                "External handler should extract the next package from the first output slot before later slots");
        helper.assertTrue(thirdExtract.getCount() == 1 && thirdExtract.is(APItems.packageItems().get(PackageColor.RED).get()),
                "External handler should move to the next output slot only after earlier slots are empty");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerAutoExportsToAdjacentItemHandler(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(0, 0, 0);
        BlockPos assemblerPos = new BlockPos(1, 0, 0);
        BlockPos energyCellPos = new BlockPos(2, 0, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(chestPos),
                Blocks.CHEST.defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);
        PackageAssemblerBlockEntity assembler = placePackageAssembler(helper, assemblerPos, Direction.EAST);
        PackageData target = ironPackageData(PackageColor.FLUIX, 64);
        ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(pattern, PackageColor.FLUIX, target);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.setOutputMode(PackageAssemblerBlockEntity.OutputMode.ADJACENT_BLOCK);
        assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));

        helper.startSequence()
                .thenWaitUntil(() -> assertPackageAssemblerReady(helper, assembler, "adjacent auto-export test"))
                .thenExecute(() -> {
                    tickPackageAssemblerToCompletion(assembler);

                    ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
                    ItemStack exported = chest.getItem(0);
                    PackageData exportedData = PackageDataStorage.read(exported).orElseThrow();
                    helper.assertTrue(assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "Auto-export should clear the assembler output slot");
                    helper.assertTrue(assembler.getItems().getStackInSlot(0).isEmpty(),
                            "Auto-export should only happen after assembly consumes input");
                    helper.assertTrue(amountOf(exportedData, AEItemKey.of(Items.IRON_INGOT)) == 64,
                            "Adjacent item handler should receive the assembled package");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerRequiresAeEnergyForProgress(GameTestHelper helper) {
        BlockPos assemblerPos = new BlockPos(0, 0, 0);
        PackageAssemblerBlockEntity assembler = placePackageAssembler(helper, assemblerPos, Direction.NORTH);
        PackageData target = ironPackageData(PackageColor.FLUIX, 64);
        ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(pattern, PackageColor.FLUIX, target);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));

        tickPackageAssemblerToCompletion(assembler);

        helper.assertTrue(assembler.isCrafting(),
                "Assembler should hold the planned package while waiting for AE energy");
        helper.assertTrue(assembler.craftingProgress() == 0,
                "Assembler should not advance progress without AE energy");
        helper.assertTrue(assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).isEmpty(),
                "Assembler should not output a package without AE energy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerSpeedCardsUseAePowerProgress(GameTestHelper helper) {
        BlockPos energyCellPos = new BlockPos(0, 0, 0);
        BlockPos assemblerPos = new BlockPos(1, 0, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);
        PackageAssemblerBlockEntity assembler = placePackageAssembler(helper, assemblerPos, Direction.NORTH);

        helper.startSequence()
                .thenWaitUntil(() -> assertPackageAssemblerReady(helper, assembler, "speed-card progress test"))
                .thenExecute(() -> {
                    PackageData target = ironPackageData(PackageColor.FLUIX, 64);
                    ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
                    PackagePatternDataStorage.write(pattern, PackageColor.FLUIX, target);
                    assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
                    assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
                    for (int card = 0; card < PackageAssemblerBlockEntity.UPGRADE_SLOT_COUNT; card++) {
                        assembler.getUpgrades().addItems(AEItems.SPEED_CARD.stack());
                    }

                    assembler.serverTick();
                    helper.assertTrue(assembler.isCrafting(),
                            "Assembler should start a progress craft before output appears");
                    helper.assertTrue(assembler.craftingProgress() == 0,
                            "Assembler should start at zero progress");

                    assembler.serverTick();
                    helper.assertTrue(assembler.craftingProgress() == 50,
                            "Five speed cards should advance by the molecular assembler powered speed value");
                    helper.assertTrue(assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "Assembler should not output before progress reaches 100");

                    assembler.serverTick();
                    helper.assertTrue(!assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "Assembler should output after powered progress reaches 100");
                    helper.assertTrue(assembler.craftingProgress() == 0,
                            "Assembler should reset progress after completing a package");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerAutoExportsToAe2Interface(GameTestHelper helper) {
        BlockPos energyCellPos = new BlockPos(0, 0, 0);
        BlockPos drivePos = new BlockPos(1, 0, 0);
        BlockPos interfacePos = new BlockPos(2, 0, 0);
        BlockPos assemblerPos = new BlockPos(3, 0, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(drivePos),
                AEBlocks.DRIVE.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(interfacePos),
                AEBlocks.INTERFACE.block().defaultBlockState(),
                3);
        PackageAssemblerBlockEntity assembler = placePackageAssembler(helper, assemblerPos, Direction.EAST);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    InterfaceBlockEntity aeInterface =
                            (InterfaceBlockEntity) helper.getBlockEntity(interfacePos);
                    helper.assertTrue(aeInterface.getMainNode().isActive(),
                            "AE2 Interface grid node should be active before assembler auto-export");
                    helper.assertTrue(aeInterface.getMainNode().hasGridBooted(),
                            "AE2 Interface grid should finish booting before assembler auto-export");
                    assertPackageAssemblerReady(helper, assembler, "AE2 auto-export test");
                })
                .thenExecute(() -> {
                    DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(drivePos);
                    InterfaceBlockEntity aeInterface =
                            (InterfaceBlockEntity) helper.getBlockEntity(interfacePos);
                    drive.getInternalInventory().addItems(AEItems.ITEM_CELL_64K.stack());
                    PackageData target = ironPackageData(PackageColor.FLUIX, 64);
                    ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
                    PackagePatternDataStorage.write(pattern, PackageColor.FLUIX, target);
                    assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
                    assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));

                    tickPackageAssemblerToCompletion(assembler);

                    var storage = aeInterface.getMainNode().getGrid().getStorageService().getInventory();
                    var source = IActionSource.ofMachine(aeInterface);
                    ItemStack expectedPackage = packageStack(PackageColor.FLUIX, ironPackageData(PackageColor.FLUIX, 64));
                    long storedPackages =
                            storage.extract(AEItemKey.of(expectedPackage), 1, Actionable.SIMULATE, source);
                    helper.assertTrue(assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "AE2 auto-export should clear the assembler output slot");
                    helper.assertTrue(storedPackages == 1,
                            "AE2 Interface network should receive the assembled package item");
                })
                .thenSucceed();
    }

    @GameTest(template = "ae_network_column")
    public static void ae2PatternProviderPushesPackageCraftingPatternIntoPackageAssembler(GameTestHelper helper) {
        BlockPos energyCellPos = new BlockPos(0, 0, 0);
        BlockPos providerPos = new BlockPos(0, 1, 0);
        BlockPos assemblerPos = new BlockPos(0, 2, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(providerPos),
                AEBlocks.PATTERN_PROVIDER.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(assemblerPos),
                APBlocks.PACKAGE_ASSEMBLER.get().defaultBlockState(),
                3);

        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    PatternProviderBlockEntity provider =
                            (PatternProviderBlockEntity) helper.getBlockEntity(providerPos);
                    PackageAssemblerBlockEntity assembler =
                            (PackageAssemblerBlockEntity) helper.getBlockEntity(assemblerPos);
                    ItemStack pattern = packageCraftingPattern(
                            PackageColor.FLUIX,
                            Optional.empty(),
                            "",
                            new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                            new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32));
                    provider.getLogic().getPatternInv().addItems(pattern);
                    provider.getLogic().updatePatterns();
                    helper.assertTrue(provider.getLogic().getAvailablePatterns().size() == 1,
                            "Pattern Provider should decode the package crafting pattern");
                    IPatternDetails details = provider.getLogic().getAvailablePatterns().get(0);
                    helper.assertTrue(details instanceof PackageCraftingPatternDetails,
                            "Pattern Provider should expose AP package crafting details");
                    KeyCounter iron = new KeyCounter();
                    iron.add(AEItemKey.of(Items.IRON_INGOT), 64);
                    KeyCounter copper = new KeyCounter();
                    copper.add(AEItemKey.of(Items.COPPER_INGOT), 32);

                    boolean accepted = provider.getLogic().pushPattern(details, new KeyCounter[] { iron, copper });

                    helper.assertTrue(accepted,
                            "AE2 Pattern Provider should push package crafting patterns into Package Assembler");
                    helper.assertTrue(iron.isEmpty(), "Accepted AE2 push should consume iron input");
                    helper.assertTrue(copper.isEmpty(), "Accepted AE2 push should consume copper input");
                })
                .thenWaitUntil(() -> {
                    PackageAssemblerBlockEntity assembler =
                            (PackageAssemblerBlockEntity) helper.getBlockEntity(assemblerPos);
                    ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
                    helper.assertTrue(!output.isEmpty(), "AE2-pushed package should appear after assembler progress");
                    PackageData outputData = PackageDataStorage.read(output).orElseThrow();
                    helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.IRON_INGOT)) == 64,
                            "AE2-pushed iron should be packaged");
                    helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.COPPER_INGOT)) == 32,
                            "AE2-pushed copper should be packaged");
                })
                .thenSucceed();
    }

    @GameTest(template = "ae_network_column")
    public static void ae2PatternProviderPushesColoredProcessingPatternIntoPackageAssembler(GameTestHelper helper) {
        BlockPos energyCellPos = new BlockPos(0, 0, 0);
        BlockPos providerPos = new BlockPos(0, 1, 0);
        BlockPos assemblerPos = new BlockPos(0, 2, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(providerPos),
                AEBlocks.PATTERN_PROVIDER.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(assemblerPos),
                APBlocks.PACKAGE_ASSEMBLER.get().defaultBlockState(),
                3);

        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    PatternProviderBlockEntity provider =
                            (PatternProviderBlockEntity) helper.getBlockEntity(providerPos);
                    PackageAssemblerBlockEntity assembler =
                            (PackageAssemblerBlockEntity) helper.getBlockEntity(assemblerPos);
                    ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                            new GenericStack[] {
                                    new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32),
                                    new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32)
                            },
                            new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
                    ColoredProcessingPatternDataStorage.write(
                            pattern,
                            Map.of(0, PackageColor.RED, 1, PackageColor.BLUE));
                    provider.getLogic().getPatternInv().addItems(pattern);
                    provider.getLogic().updatePatterns();
                    helper.assertTrue(provider.getLogic().getAvailablePatterns().size() == 1,
                            "Pattern Provider should decode the colored processing pattern");
                    IPatternDetails details = provider.getLogic().getAvailablePatterns().get(0);
                    KeyCounter iron = new KeyCounter();
                    iron.add(AEItemKey.of(Items.IRON_INGOT), 64);

                    boolean accepted = provider.getLogic().pushPattern(details, new KeyCounter[] { iron });
                    helper.assertTrue(accepted,
                            "AE2 Pattern Provider should push colored processing patterns into Package Assembler");
                    helper.assertTrue(iron.isEmpty(), "Accepted colored AE2 push should consume iron input");
                })
                .thenWaitUntil(() -> {
                    PackageAssemblerBlockEntity assembler =
                            (PackageAssemblerBlockEntity) helper.getBlockEntity(assemblerPos);
                    ItemStack firstOutput =
                            assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
                    helper.assertTrue(!firstOutput.isEmpty(),
                            "First colored package should appear after assembler progress");
                    PackageData firstData = PackageDataStorage.read(firstOutput).orElseThrow();
                    ItemStack secondOutput =
                            assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.outputHandlerSlot(1)).copy();
                    helper.assertTrue(!secondOutput.isEmpty(),
                            "Second colored package should appear after assembler progress");
                    PackageData secondData = PackageDataStorage.read(secondOutput).orElseThrow();

                    helper.assertTrue(firstOutput.is(APItems.packageItems().get(PackageColor.RED).get()),
                            "First AE2 colored output should be red");
                    helper.assertTrue(secondOutput.is(APItems.packageItems().get(PackageColor.BLUE).get()),
                            "Second AE2 colored output should be blue");
                    helper.assertTrue(amountOf(firstData, AEItemKey.of(Items.IRON_INGOT)) == 32,
                            "First AE2 colored output should contain half the iron");
                    helper.assertTrue(amountOf(secondData, AEItemKey.of(Items.IRON_INGOT)) == 32,
                            "Second AE2 colored output should contain half the iron");
                })
                .thenSucceed();
    }

    @GameTest(template = "ae_network_column")
    public static void ae2PatternProviderPushesPackagedProcessingPatternIntoPackageAssembler(GameTestHelper helper) {
        BlockPos energyCellPos = new BlockPos(0, 0, 0);
        BlockPos providerPos = new BlockPos(0, 1, 0);
        BlockPos assemblerPos = new BlockPos(0, 2, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(providerPos),
                AEBlocks.PATTERN_PROVIDER.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(assemblerPos),
                APBlocks.PACKAGE_ASSEMBLER.get().defaultBlockState(),
                3);

        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    PatternProviderBlockEntity provider =
                            (PatternProviderBlockEntity) helper.getBlockEntity(providerPos);
                    PackageAssemblerBlockEntity assembler =
                            (PackageAssemblerBlockEntity) helper.getBlockEntity(assemblerPos);
                    PackageData iron = ironPackageData(PackageColor.RED, 64);
                    PackageData copper = PackageData.create(
                            PackageColor.RED,
                            List.of(new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                            Optional.empty(),
                            0);
                    ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                            new GenericStack[] {
                                    new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                                    new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)
                            },
                            new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 2) });
                    PackagedProcessingPatternDataStorage.write(
                            pattern,
                            PackageColor.RED,
                            List.of(iron, copper),
                            List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 2)));
                    provider.getLogic().getPatternInv().addItems(pattern);
                    provider.getLogic().updatePatterns();
                    helper.assertTrue(provider.getLogic().getAvailablePatterns().size() == 1,
                            "Pattern Provider should decode the packaged-processing carrier");
                    IPatternDetails details = provider.getLogic().getAvailablePatterns().get(0);
                    helper.assertTrue(details.getOutputs().length == 1,
                            "Packaged-processing carrier should expose its AE2 processing output");
                    KeyCounter ironInput = new KeyCounter();
                    ironInput.add(AEItemKey.of(Items.IRON_INGOT), 64);
                    KeyCounter copperInput = new KeyCounter();
                    copperInput.add(AEItemKey.of(Items.COPPER_INGOT), 32);

                    boolean accepted =
                            provider.getLogic().pushPattern(details, new KeyCounter[] { ironInput, copperInput });
                    helper.assertTrue(accepted,
                            "AE2 Pattern Provider should push packaged-processing patterns into Package Assembler");
                    helper.assertTrue(ironInput.isEmpty(),
                            "Accepted packaged-processing AE2 push should consume iron input");
                    helper.assertTrue(copperInput.isEmpty(),
                            "Accepted packaged-processing AE2 push should consume copper input");
                })
                .thenWaitUntil(() -> {
                    PackageAssemblerBlockEntity assembler =
                            (PackageAssemblerBlockEntity) helper.getBlockEntity(assemblerPos);
                    ItemStack firstOutput =
                            assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
                    helper.assertTrue(!firstOutput.isEmpty(),
                            "First packaged-processing package should appear after assembler progress");
                    PackageData firstData = PackageDataStorage.read(firstOutput).orElseThrow();
                    ItemStack secondOutput =
                            assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.outputHandlerSlot(1)).copy();
                    helper.assertTrue(!secondOutput.isEmpty(),
                            "Second packaged-processing package should appear after assembler progress");
                    PackageData secondData = PackageDataStorage.read(secondOutput).orElseThrow();
                    PackageData expectedIron = ironPackageData(PackageColor.RED, 64);
                    PackageData expectedCopper = PackageData.create(
                            PackageColor.RED,
                            List.of(new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                            Optional.empty(),
                            0);

                    helper.assertTrue(firstOutput.is(APItems.packageItems().get(PackageColor.RED).get()),
                            "First packaged-processing AE2 output should use the encoded color");
                    helper.assertTrue(firstData.canonicalHash().equals(expectedIron.canonicalHash()),
                            "First packaged-processing AE2 output should match the first package");
                    helper.assertTrue(secondData.canonicalHash().equals(expectedCopper.canonicalHash()),
                            "Second packaged-processing AE2 output should match the second package");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void ae2CraftingCpuJobPushesIntoPackageAssembler(GameTestHelper helper) {
        BlockPos energyCellPos = new BlockPos(0, 0, 0);
        BlockPos drivePos = new BlockPos(1, 0, 0);
        BlockPos cpuPos = new BlockPos(2, 0, 0);
        BlockPos providerPos = new BlockPos(3, 0, 0);
        BlockPos assemblerPos = new BlockPos(4, 0, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(drivePos),
                AEBlocks.DRIVE.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(cpuPos),
                AEBlocks.CRAFTING_STORAGE_64K.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(providerPos),
                AEBlocks.PATTERN_PROVIDER.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(assemblerPos),
                APBlocks.PACKAGE_ASSEMBLER.get().defaultBlockState(),
                3);
        ItemStack cpuPattern = packageCraftingPattern(
                PackageColor.FLUIX,
                Optional.empty(),
                "",
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32));
        ItemStack expectedPackage = PackageCraftingPatternDataStorage.toPackageStack(
                PackageCraftingPatternDataStorage.read(cpuPattern).orElseThrow());
        CpuCraftingJob craftingJob = new CpuCraftingJob(helper, providerPos, AEItemKey.of(expectedPackage), 1);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    PatternProviderBlockEntity provider =
                            (PatternProviderBlockEntity) helper.getBlockEntity(providerPos);
                    PackageAssemblerBlockEntity assembler =
                            (PackageAssemblerBlockEntity) helper.getBlockEntity(assemblerPos);
                    helper.assertTrue(provider.getMainNode().isActive(),
                            "Pattern Provider grid node should be active before configuring the job");
                    helper.assertTrue(provider.getMainNode().hasGridBooted(),
                            "Pattern Provider grid should finish booting before configuring the job");
                    assertPackageAssemblerReady(helper, assembler, "crafting CPU job");
                })
                .thenExecute(() -> {
                    DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(drivePos);
                    PatternProviderBlockEntity provider =
                            (PatternProviderBlockEntity) helper.getBlockEntity(providerPos);
                    drive.getInternalInventory().addItems(AEItems.ITEM_CELL_64K.stack());
                    provider.getLogic().getPatternInv().addItems(cpuPattern.copy());
                    provider.getLogic().updatePatterns();
                    helper.assertTrue(provider.getLogic().getAvailablePatterns().size() == 1,
                            "Pattern Provider should decode the package crafting pattern for CPU crafting");
                    helper.assertTrue(provider.getLogic().getAvailablePatterns().get(0)
                                    instanceof PackageCraftingPatternDetails,
                            "CPU crafting should see AP package crafting details");
                    var grid = provider.getMainNode().getGrid();
                    var source = IActionSource.ofMachine(provider);
                    var storage = grid.getStorageService().getInventory();
                    long insertedIron = storage.insert(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.MODULATE, source);
                    long insertedCopper = storage.insert(AEItemKey.of(Items.COPPER_INGOT), 32, Actionable.MODULATE, source);
                    helper.assertTrue(insertedIron == 64, "AE2 network should accept iron crafting inputs");
                    helper.assertTrue(insertedCopper == 32, "AE2 network should accept copper crafting inputs");
                })
                .thenWaitUntil(craftingJob::tickUntilStarted)
                .thenIdle(2)
                .thenWaitUntil(() -> {
                    PatternProviderBlockEntity provider =
                            (PatternProviderBlockEntity) helper.getBlockEntity(providerPos);
                    var storage = provider.getMainNode().getGrid().getStorageService().getInventory();
                    long storedPackages = storage.extract(
                            AEItemKey.of(expectedPackage),
                            1,
                            Actionable.SIMULATE,
                            IActionSource.ofMachine(provider));
                    helper.assertTrue(storedPackages == 1,
                            "Crafting CPU job should push inputs into Package Assembler and auto-output a package to ME storage");
                    helper.assertTrue(craftingJob.submitted(),
                            "Crafting CPU job should have been submitted before pushing inputs");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void mePackagerPackagesAndUnpacksThroughAe2Interface(GameTestHelper helper) {
        BlockPos energyCellPos = new BlockPos(0, 0, 0);
        BlockPos drivePos = new BlockPos(1, 0, 0);
        BlockPos interfacePos = new BlockPos(2, 0, 0);
        BlockPos packagerPos = new BlockPos(3, 0, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(drivePos),
                AEBlocks.DRIVE.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(interfacePos),
                AEBlocks.INTERFACE.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(packagerPos),
                APBlocks.ME_PACKAGER.get().defaultBlockState()
                        .setValue(AbstractHorizontalMachineBlock.FACING, Direction.EAST)
                        .setValue(MePackagerBlock.NETWORK_SIDE, Direction.WEST),
                3);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    InterfaceBlockEntity aeInterface =
                            (InterfaceBlockEntity) helper.getBlockEntity(interfacePos);
                    MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);
                    helper.assertTrue(aeInterface.getMainNode().isActive(),
                            "AE2 Interface grid node should be active before ME Packager smoke");
                    helper.assertTrue(aeInterface.getMainNode().hasGridBooted(),
                            "AE2 Interface grid should finish booting before ME Packager smoke");
                    assertMePackagerReady(helper, packager, "ME Packager smoke");
                })
                .thenExecute(() -> {
                    DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(drivePos);
                    InterfaceBlockEntity aeInterface =
                            (InterfaceBlockEntity) helper.getBlockEntity(interfacePos);
                    MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);
                    drive.getInternalInventory().addItems(AEItems.ITEM_CELL_64K.stack());
                    var storage = aeInterface.getMainNode().getGrid().getStorageService().getInventory();
                    var source = IActionSource.ofMachine(aeInterface);
                    long insertedIron = storage.insert(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.MODULATE, source);
                    long insertedCopper = storage.insert(AEItemKey.of(Items.COPPER_INGOT), 32, Actionable.MODULATE, source);
                    helper.assertTrue(insertedIron == 64,
                            "AE2 Interface network should accept iron before packager smoke");
                    helper.assertTrue(insertedCopper == 32,
                            "AE2 Interface network should accept copper before packager smoke");

                    MePackagerBlockEntity.MachineResult packResult = packager.runOnce();
                    helper.assertTrue(packResult == MePackagerBlockEntity.MachineResult.PACKED,
                            "ME Packager should package from the adjacent AE2 Interface");
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "ME Packager should wait until animation end before exposing the Interface package");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 0,
                            "ME Packager should remove packaged iron from the AE2 Interface network");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.COPPER_INGOT), 32, Actionable.SIMULATE, source) == 0,
                            "ME Packager should remove packaged copper from the AE2 Interface network");
                })
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 2, () -> {
                    InterfaceBlockEntity aeInterface =
                            (InterfaceBlockEntity) helper.getBlockEntity(interfacePos);
                    MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);
                    var storage = aeInterface.getMainNode().getGrid().getStorageService().getInventory();
                    var source = IActionSource.ofMachine(aeInterface);
                    ItemStack output = packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).copy();
                    PackageData outputData = PackageDataStorage.read(output).orElseThrow();
                    helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.IRON_INGOT)) == 64,
                            "AE2 Interface package should contain iron");
                    helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.COPPER_INGOT)) == 32,
                            "AE2 Interface package should contain copper");
                    packager.getItems().setStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
                    packager.getItems().setStackInSlot(MePackagerBlockEntity.SLOT_INPUT, output);
                    MePackagerBlockEntity.MachineResult unpackResult = packager.runOnce();
                    helper.assertTrue(unpackResult == MePackagerBlockEntity.MachineResult.UNPACKED,
                            "ME Packager should unpack into the adjacent AE2 Interface");
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_INPUT).isEmpty(),
                            "ME Packager should consume the input package after AE2 Interface unpack");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 64,
                            "AE2 Interface network should contain unpacked iron");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.COPPER_INGOT), 32, Actionable.SIMULATE, source) == 32,
                            "AE2 Interface network should contain unpacked copper");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void mePackagerDoesNotUseForgeFluidHandlerAsNetworkTarget(GameTestHelper helper) {
        BlockPos tankPos = new BlockPos(0, 0, 0);
        BlockPos packagerPos = new BlockPos(1, 0, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(tankPos),
                Blocks.CHEST.defaultBlockState(),
                3);
        TestFluidTankBlockEntity tank = new TestFluidTankBlockEntity(
                helper.absolutePos(tankPos),
                helper.getLevel().getBlockState(helper.absolutePos(tankPos)),
                4000);
        helper.getLevel().setBlockEntity(tank);
        tank.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);
        helper.getLevel().setBlock(
                helper.absolutePos(packagerPos),
                APBlocks.ME_PACKAGER.get().defaultBlockState()
                        .setValue(AbstractHorizontalMachineBlock.FACING, Direction.EAST)
                        .setValue(MePackagerBlock.NETWORK_SIDE, Direction.WEST),
                3);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        MePackagerBlockEntity.MachineResult result = packager.runOnce();
        helper.assertTrue(result == MePackagerBlockEntity.MachineResult.NO_TARGET,
                "ME Packager should not fall back to a Forge fluid handler on its selected ME side");
        helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                "Forge fluid handler fallback should not create a package");
        helper.assertTrue(tank.getFluidAmount() == 2000
                        && tank.getFluid().isFluidEqual(new FluidStack(Fluids.WATER, 2000)),
                "Forge fluid handler fallback should not drain water");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageItemStorageExposesOnlyLegalPackages(GameTestHelper helper) {
        ItemStackHandler target = new ItemStackHandler(3);
        ItemStack legalPackage = packageStack(PackageColor.BLUE, ironPackageData(PackageColor.BLUE, 64));
        target.setStackInSlot(0, legalPackage.copy());
        target.setStackInSlot(1, new ItemStack(APItems.packageItems().get(PackageColor.RED).get()));
        target.setStackInSlot(2, new ItemStack(Items.IRON_INGOT, 64));

        PackageItemStorage storage = new PackageItemStorage(target, net.minecraft.network.chat.Component.literal("test"));
        KeyCounter available = new KeyCounter();
        storage.getAvailableStacks(available);

        helper.assertTrue(available.get(AEItemKey.of(legalPackage)) == 1, "Legal package should be visible");
        helper.assertTrue(available.size() == 1, "Invalid packages and loose items should not be visible");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageItemStorageAppliesFilter(GameTestHelper helper) {
        ItemStack redPackage = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64));
        ItemStack bluePackage = packageStack(PackageColor.BLUE, ironPackageData(PackageColor.BLUE, 64));
        PackageFilter redFilter = new PackageFilter(Optional.of(PackageColor.RED), Optional.empty(), List.of());

        ItemStackHandler source = new ItemStackHandler(2);
        source.setStackInSlot(0, redPackage.copy());
        source.setStackInSlot(1, bluePackage.copy());
        PackageItemStorage sourceStorage =
                new PackageItemStorage(source, net.minecraft.network.chat.Component.literal("test"), redFilter);
        KeyCounter available = new KeyCounter();
        sourceStorage.getAvailableStacks(available);

        long extractedBlue =
                sourceStorage.extract(AEItemKey.of(bluePackage), 1, Actionable.MODULATE, IActionSource.empty());
        long extractedRed =
                sourceStorage.extract(AEItemKey.of(redPackage), 1, Actionable.MODULATE, IActionSource.empty());

        ItemStackHandler target = new ItemStackHandler(2);
        PackageItemStorage targetStorage =
                new PackageItemStorage(target, net.minecraft.network.chat.Component.literal("test"), redFilter);
        long insertedBlue =
                targetStorage.insert(AEItemKey.of(bluePackage), 1, Actionable.MODULATE, IActionSource.empty());
        long insertedRed =
                targetStorage.insert(AEItemKey.of(redPackage), 1, Actionable.MODULATE, IActionSource.empty());

        helper.assertTrue(available.get(AEItemKey.of(redPackage)) == 1, "Matching package should be visible");
        helper.assertTrue(available.get(AEItemKey.of(bluePackage)) == 0, "Filtered package should be hidden");
        helper.assertTrue(extractedBlue == 0, "Filtered package should not extract");
        helper.assertTrue(extractedRed == 1, "Matching package should extract");
        helper.assertTrue(insertedBlue == 0, "Filtered package should not insert");
        helper.assertTrue(insertedRed == 1, "Matching package should insert");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageBusStoresFilterTemplate(GameTestHelper helper) {
        PackageExportBusBlockEntity bus = new PackageExportBusBlockEntity(
                BlockPos.ZERO,
                APBlocks.PACKAGE_EXPORT_BUS.get().defaultBlockState());
        ItemStack encodedPattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(encodedPattern, PackageColor.RED, ironPackageData(PackageColor.RED, 64));

        boolean accepted = bus.setFilterTemplate(encodedPattern);
        boolean rejected = bus.setFilterTemplate(new ItemStack(Items.DIRT));
        Optional<PackageFilter> filter = PackageFilter.fromTemplate(bus.getFilterTemplate());
        boolean cleared = bus.clearFilterTemplate();

        helper.assertTrue(accepted, "Encoded pattern should configure the bus filter");
        helper.assertFalse(rejected, "Non-template items should not configure the bus filter");
        helper.assertTrue(filter.isPresent(), "Stored bus filter template should remain readable");
        helper.assertTrue(filter.get().color().orElseThrow() == PackageColor.RED,
                "Stored bus filter template should keep encoded color");
        helper.assertTrue(cleared, "Configured bus filter should clear");
        helper.assertTrue(bus.getFilterTemplate().isEmpty(), "Cleared bus filter should be empty");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageBusMenuSetsFilterFromCursor(GameTestHelper helper) {
        PackageExportBusBlockEntity bus = placePackageExportBus(helper);
        FakePlayer player = newFakePlayer(helper);
        PackageBusMenu menu = new PackageBusMenu(1, new Inventory(player), bus);
        ItemStack encodedPattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(encodedPattern, PackageColor.RED, ironPackageData(PackageColor.RED, 64));

        menu.setCarried(encodedPattern.copy());
        boolean setClicked = menu.clickMenuButton(player, PackageBusMenu.BUTTON_SET_FROM_CURSOR);
        Optional<PackageFilter> configured = PackageFilter.fromTemplate(bus.getFilterTemplate());
        boolean clearClicked = menu.clickMenuButton(player, PackageBusMenu.BUTTON_CLEAR_FILTER);

        helper.assertTrue(setClicked, "Package bus filter menu should accept the set button");
        helper.assertTrue(configured.isPresent(), "Package bus filter menu should store a ghost template");
        helper.assertTrue(configured.get().color().orElseThrow() == PackageColor.RED,
                "Package bus filter menu should preserve template color");
        helper.assertTrue(!menu.getCarried().isEmpty(),
                "Package bus filter menu should not consume the carried template");
        helper.assertTrue(clearClicked, "Package bus filter menu should accept the clear button");
        helper.assertTrue(bus.getFilterTemplate().isEmpty(), "Package bus filter menu should clear the template");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageBusMenuShiftClickSetsGhostFilter(GameTestHelper helper) {
        PackageExportBusBlockEntity bus = placePackageExportBus(helper);
        FakePlayer player = newFakePlayer(helper);
        Inventory inventory = new Inventory(player);
        PackageBusMenu menu = new PackageBusMenu(2, inventory, bus);
        ItemStack encodedPattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(encodedPattern, PackageColor.BLUE, ironPackageData(PackageColor.BLUE, 64));
        inventory.setItem(0, encodedPattern.copy());

        ItemStack moved = menu.quickMoveStack(player, PackageBusMenu.HOTBAR_START);
        Optional<PackageFilter> configured = PackageFilter.fromTemplate(bus.getFilterTemplate());

        helper.assertTrue(moved.isEmpty(), "Package bus filter shift-click should not move real items");
        helper.assertTrue(configured.isPresent(), "Package bus filter shift-click should store a ghost template");
        helper.assertTrue(configured.get().color().orElseThrow() == PackageColor.BLUE,
                "Package bus filter shift-click should preserve template color");
        helper.assertTrue(!inventory.getItem(0).isEmpty(),
                "Package bus filter shift-click should not consume the inventory template");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageBusMenuEditsManualFilter(GameTestHelper helper) {
        PackageExportBusBlockEntity bus = placePackageExportBus(helper);
        FakePlayer player = newFakePlayer(helper);
        PackageBusMenu menu = new PackageBusMenu(3, new Inventory(player), bus);

        ItemStack markerStack = new ItemStack(Items.DIAMOND, 8);
        ItemStack contentStack = new ItemStack(Items.IRON_INGOT, 32);

        boolean colorClicked = menu.clickMenuButton(
                player,
                PackageBusMenu.BUTTON_COLOR_BASE + PackageColor.RED.ordinal());
        menu.setCarried(markerStack.copy());
        menu.clicked(PackageBusMenu.MARKER_FILTER_SLOT, 0, ClickType.PICKUP, player);
        menu.setCarried(contentStack.copy());
        menu.clicked(PackageBusMenu.CONTENT_FILTER_START, 0, ClickType.PICKUP, player);

        PackageFilter filter = bus.getConfiguredFilter();
        MarkerSpec expectedMarker = new MarkerSpec(new GenericStack(AEItemKey.of(Items.DIAMOND), 1));

        helper.assertTrue(colorClicked, "Package bus filter menu should accept color buttons");
        helper.assertTrue(filter.color().orElseThrow() == PackageColor.RED,
                "Manual package bus filter should store the selected color");
        helper.assertTrue(filter.marker().orElseThrow().sameAs(expectedMarker),
                "Manual package bus filter should store a marker ghost");
        helper.assertTrue(filter.requiredContents().size() == 1,
                "Manual package bus filter should store one required content ghost");
        helper.assertTrue(filter.requiredContents().get(0).what().equals(AEItemKey.of(Items.IRON_INGOT)),
                "Manual package bus filter should store the required item key");
        helper.assertTrue(filter.requiredContents().get(0).amount() == 32,
                "Manual package bus filter should store the required item count");
        helper.assertTrue(menu.markerFilter().is(Items.DIAMOND), "Marker ghost should be visible in the menu");
        helper.assertTrue(menu.contentFilter(0).is(Items.IRON_INGOT), "Content ghost should be visible in the menu");
        helper.assertTrue(menu.getCarried().getCount() == 32,
                "Manual package bus filter should not consume the carried content stack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageBusMenuEditsManualFluidFilter(GameTestHelper helper) {
        PackageExportBusBlockEntity bus = placePackageExportBus(helper);
        FakePlayer player = newFakePlayer(helper);
        PackageBusMenu menu = new PackageBusMenu(3, new Inventory(player), bus);
        ItemStack carried = new ItemStack(Items.WATER_BUCKET);

        menu.setCarried(carried.copy());
        menu.clicked(PackageBusMenu.CONTENT_FILTER_START, 0, ClickType.PICKUP, player);
        PackageFilter filter = bus.getConfiguredFilter();
        PackageData waterData = PackageData.create(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEFluidKey.of(Fluids.WATER), 1000)),
                Optional.empty(),
                0);

        helper.assertTrue(filter.requiredContents().size() == 1,
                "Manual package bus filter should store one required fluid ghost");
        helper.assertTrue(filter.requiredContents().get(0).what().equals(AEFluidKey.of(Fluids.WATER)),
                "Manual package bus filter should store the required fluid key");
        helper.assertTrue(filter.requiredContents().get(0).amount() == 1000,
                "Manual package bus filter should store one bucket of fluid");
        helper.assertTrue(filter.matches(PackageColor.FLUIX, waterData),
                "Manual package bus filter should match equivalent fluid package contents");
        helper.assertTrue(!menu.contentFilter(0).isEmpty(), "Fluid content ghost should be visible in the menu");
        helper.assertTrue(menu.getCarried().is(Items.WATER_BUCKET),
                "Manual package bus fluid filter should not consume the carried bucket");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageBusMenuAdjustsManualFluidFilterAmount(GameTestHelper helper) {
        PackageExportBusBlockEntity bus = placePackageExportBus(helper);
        FakePlayer player = newFakePlayer(helper);
        PackageBusMenu menu = new PackageBusMenu(3, new Inventory(player), bus);

        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        menu.clicked(PackageBusMenu.CONTENT_FILTER_START, 0, ClickType.PICKUP, player);
        boolean increased = menu.clickMenuButton(player, PackageBusMenu.BUTTON_CONTENT_AMOUNT_INCREASE_BASE);
        PackageFilter increasedFilter = bus.getConfiguredFilter();
        boolean decreased = menu.clickMenuButton(player, PackageBusMenu.BUTTON_CONTENT_AMOUNT_DECREASE_BASE);
        boolean clamped = menu.clickMenuButton(player, PackageBusMenu.BUTTON_CONTENT_AMOUNT_DECREASE_BASE);
        PackageFilter clampedFilter = bus.getConfiguredFilter();

        helper.assertTrue(increased, "Package bus menu should accept content amount increase buttons");
        helper.assertTrue(decreased, "Package bus menu should accept content amount decrease buttons");
        helper.assertTrue(clamped, "Package bus menu should accept clamped content amount decrease buttons");
        helper.assertTrue(increasedFilter.requiredContents().get(0).amount() == 2000,
                "Package bus fluid content amount should increase by one bucket");
        helper.assertTrue(clampedFilter.requiredContents().get(0).amount() == 1000,
                "Package bus fluid content amount should not decrement below one bucket");
        helper.assertTrue(menu.contentFilterAmount(0) == 1000,
                "Package bus menu should expose the synchronized content amount");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageBusManualFilterPersists(GameTestHelper helper) {
        PackageExportBusBlockEntity bus = new PackageExportBusBlockEntity(
                BlockPos.ZERO,
                APBlocks.PACKAGE_EXPORT_BUS.get().defaultBlockState());
        bus.setManualFilterColor(PackageColor.BLUE);
        bus.setManualFilterMarker(new ItemStack(Items.EMERALD));
        bus.setManualFilterContent(0, new ItemStack(Items.COPPER_INGOT, 24), 24);

        CompoundTag tag = new CompoundTag();
        bus.saveAdditional(tag);

        PackageExportBusBlockEntity loaded = new PackageExportBusBlockEntity(
                BlockPos.ZERO,
                APBlocks.PACKAGE_EXPORT_BUS.get().defaultBlockState());
        loaded.loadTag(tag);
        PackageFilter filter = loaded.getConfiguredFilter();

        helper.assertTrue(filter.color().orElseThrow() == PackageColor.BLUE,
                "Manual package bus filter color should persist");
        helper.assertTrue(filter.marker().orElseThrow().sameAs(
                        new MarkerSpec(new GenericStack(AEItemKey.of(Items.EMERALD), 1))),
                "Manual package bus filter marker should persist");
        helper.assertTrue(filter.requiredContents().size() == 1,
                "Manual package bus filter contents should persist");
        helper.assertTrue(filter.requiredContents().get(0).what().equals(AEItemKey.of(Items.COPPER_INGOT)),
                "Manual package bus filter content key should persist");
        helper.assertTrue(filter.requiredContents().get(0).amount() == 24,
                "Manual package bus filter content amount should persist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageBusManualFluidFilterPersists(GameTestHelper helper) {
        PackageExportBusBlockEntity bus = new PackageExportBusBlockEntity(
                BlockPos.ZERO,
                APBlocks.PACKAGE_EXPORT_BUS.get().defaultBlockState());
        bus.setManualFilterColor(PackageColor.BLUE);
        bus.setManualFilterContentFromGhostStack(0, new ItemStack(Items.WATER_BUCKET), false);

        CompoundTag tag = new CompoundTag();
        bus.saveAdditional(tag);

        PackageExportBusBlockEntity loaded = new PackageExportBusBlockEntity(
                BlockPos.ZERO,
                APBlocks.PACKAGE_EXPORT_BUS.get().defaultBlockState());
        loaded.loadTag(tag);
        PackageFilter filter = loaded.getConfiguredFilter();

        helper.assertTrue(filter.color().orElseThrow() == PackageColor.BLUE,
                "Manual package bus fluid filter color should persist");
        helper.assertTrue(filter.requiredContents().size() == 1,
                "Manual package bus fluid filter contents should persist");
        helper.assertTrue(filter.requiredContents().get(0).what().equals(AEFluidKey.of(Fluids.WATER)),
                "Manual package bus fluid filter key should persist");
        helper.assertTrue(filter.requiredContents().get(0).amount() == 1000,
                "Manual package bus fluid filter amount should persist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageItemStorageRejectsLooseItemInsert(GameTestHelper helper) {
        ItemStackHandler target = new ItemStackHandler(1);
        PackageItemStorage storage = new PackageItemStorage(target, net.minecraft.network.chat.Component.literal("test"));

        long inserted = storage.insert(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.MODULATE, IActionSource.empty());

        helper.assertTrue(inserted == 0, "Package storage should reject loose item insert");
        helper.assertTrue(target.getStackInSlot(0).isEmpty(), "Rejected insert should not mutate target");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageItemStorageInsertsAndExtractsPackages(GameTestHelper helper) {
        ItemStackHandler target = new ItemStackHandler(1);
        ItemStack legalPackage = packageStack(PackageColor.GREEN, ironPackageData(PackageColor.GREEN, 64));
        PackageItemStorage storage = new PackageItemStorage(target, net.minecraft.network.chat.Component.literal("test"));

        long simulatedInsert = storage.insert(AEItemKey.of(legalPackage), 1, Actionable.SIMULATE, IActionSource.empty());
        long inserted = storage.insert(AEItemKey.of(legalPackage), 1, Actionable.MODULATE, IActionSource.empty());
        long simulatedExtract = storage.extract(AEItemKey.of(legalPackage), 1, Actionable.SIMULATE, IActionSource.empty());
        long extracted = storage.extract(AEItemKey.of(legalPackage), 1, Actionable.MODULATE, IActionSource.empty());

        helper.assertTrue(simulatedInsert == 1, "Legal package insert should simulate");
        helper.assertTrue(inserted == 1, "Legal package insert should commit");
        helper.assertTrue(simulatedExtract == 1, "Legal package extract should simulate");
        helper.assertTrue(extracted == 1, "Legal package extract should commit");
        helper.assertTrue(target.getStackInSlot(0).isEmpty(), "Committed extract should empty target");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternDataRoundTrips(GameTestHelper helper) {
        ItemStack pattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackageData data = ironPackageData(PackageColor.FLUIX, 64);

        PackagePatternDataStorage.write(pattern, PackageColor.FLUIX, data);
        Optional<PackagePatternDataStorage.EncodedPackagePattern> read = PackagePatternDataStorage.read(pattern);

        helper.assertTrue(read.isPresent(), "Encoded package pattern should be readable");
        helper.assertTrue(read.get().color() == PackageColor.FLUIX, "Encoded color should round-trip");
        helper.assertTrue(read.get().data().canonicalHash().equals(data.canonicalHash()), "Encoded data should round-trip");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternDataRoundTripsOnAe2BlankPattern(GameTestHelper helper) {
        ItemStack pattern = ae2Item("blank_pattern");
        helper.assertFalse(pattern.isEmpty(), "AE2 blank pattern should be registered");
        PackageData data = ironPackageData(PackageColor.RED, 64);

        PackagePatternDataStorage.write(pattern, PackageColor.RED, data);
        Optional<PackagePatternDataStorage.EncodedPackagePattern> read = PackagePatternDataStorage.read(pattern);

        helper.assertTrue(PackagePatternDataStorage.isAe2BlankPattern(pattern),
                "AE2 blank pattern should be recognized as a package pattern carrier");
        helper.assertTrue(read.isPresent(), "Encoded AE2 blank pattern should be readable");
        helper.assertTrue(read.get().color() == PackageColor.RED, "Encoded AE2 carrier color should round-trip");
        helper.assertTrue(read.get().data().canonicalHash().equals(data.canonicalHash()),
                "Encoded AE2 carrier data should round-trip");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagedProcessingPatternDataRoundTrips(GameTestHelper helper) {
        ItemStack pattern = new ItemStack(APItems.PACKAGED_PROCESSING_PATTERN.get());
        PackageData iron = ironPackageData(PackageColor.FLUIX, 64);
        PackageData copper = PackageData.create(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);

        PackagedProcessingPatternDataStorage.write(pattern, PackageColor.FLUIX, List.of(iron, copper));
        Optional<PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern> read =
                PackagedProcessingPatternDataStorage.read(pattern);

        helper.assertTrue(read.isPresent(), "Encoded packaged processing pattern should be readable");
        helper.assertTrue(read.get().color() == PackageColor.FLUIX, "Encoded color should round-trip");
        helper.assertTrue(read.get().packages().size() == 2, "Encoded package list should round-trip");
        helper.assertTrue(read.get().packages().get(0).canonicalHash().equals(iron.canonicalHash()),
                "First encoded package should round-trip");
        helper.assertTrue(read.get().packages().get(1).canonicalHash().equals(copper.canonicalHash()),
                "Second encoded package should round-trip");
        helper.assertTrue(read.get().outputs().isEmpty(), "Legacy write overload should encode no processing outputs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagedProcessingPatternOutputsRoundTrip(GameTestHelper helper) {
        ItemStack pattern = new ItemStack(APItems.PACKAGED_PROCESSING_PATTERN.get());
        PackageData iron = ironPackageData(PackageColor.FLUIX, 64);

        PackagedProcessingPatternDataStorage.write(
                pattern,
                PackageColor.FLUIX,
                List.of(iron),
                List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 2)));
        Optional<PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern> read =
                PackagedProcessingPatternDataStorage.read(pattern);

        helper.assertTrue(read.isPresent(), "Encoded packaged processing pattern should be readable");
        helper.assertTrue(read.get().outputs().size() == 1, "Processing outputs should round-trip");
        helper.assertTrue(read.get().outputs().get(0).what().equals(AEItemKey.of(Items.DIAMOND)),
                "Processing output key should round-trip");
        helper.assertTrue(read.get().outputs().get(0).amount() == 2,
                "Processing output amount should round-trip");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagedProcessingPatternDataRoundTripsOnAe2BlankPattern(GameTestHelper helper) {
        ItemStack pattern = ae2Item("blank_pattern");
        helper.assertFalse(pattern.isEmpty(), "AE2 blank pattern should be registered");
        PackageData iron = ironPackageData(PackageColor.BLUE, 64);

        PackagedProcessingPatternDataStorage.write(
                pattern,
                PackageColor.BLUE,
                List.of(iron),
                List.of(new GenericStack(AEItemKey.of(Items.DIAMOND), 2)));
        Optional<PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern> read =
                PackagedProcessingPatternDataStorage.read(pattern);

        helper.assertTrue(PackagePatternDataStorage.isAe2BlankPattern(pattern),
                "AE2 blank pattern should be recognized as a packaged-processing carrier");
        helper.assertTrue(read.isPresent(), "AE2-carried packaged processing data should be readable");
        helper.assertTrue(read.get().color() == PackageColor.BLUE,
                "AE2-carried packaged processing color should round-trip");
        helper.assertTrue(read.get().packages().get(0).canonicalHash().equals(iron.canonicalHash()),
                "AE2-carried packaged processing package should round-trip");
        helper.assertTrue(read.get().outputs().size() == 1,
                "AE2-carried packaged processing outputs should round-trip");
        helper.assertTrue(read.get().outputs().get(0).what().equals(AEItemKey.of(Items.DIAMOND)),
                "AE2-carried packaged processing output key should round-trip");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void coloredProcessingPatternDataRoundTrips(GameTestHelper helper) {
        ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] {
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)
                },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });

        ColoredProcessingPatternDataStorage.write(pattern, Map.of(0, PackageColor.RED, 1, PackageColor.BLUE));
        Optional<ColoredProcessingPatternDataStorage.EncodedColoredProcessingPattern> read =
                ColoredProcessingPatternDataStorage.read(pattern);
        List<GenericStack> sparseInputs = ColoredProcessingPatternDataStorage.readSparseInputs(pattern);

        helper.assertTrue(read.isPresent(), "Colored processing pattern data should be readable");
        helper.assertTrue(read.get().colorForSlot(0) == PackageColor.RED, "First slot color should round-trip");
        helper.assertTrue(read.get().colorForSlot(1) == PackageColor.BLUE, "Second slot color should round-trip");
        helper.assertTrue(sparseInputs.size() == 2, "Sparse AE2 processing inputs should be readable");
        helper.assertTrue(sparseInputs.get(0).what().equals(AEItemKey.of(Items.IRON_INGOT)),
                "First sparse input should remain iron");
        helper.assertTrue(sparseInputs.get(1).what().equals(AEItemKey.of(Items.COPPER_INGOT)),
                "Second sparse input should remain copper");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ordinaryAe2ProcessingPatternHasNoAdvancedPackageMetadata(GameTestHelper helper) {
        ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32) },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        var metadata = new AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern(List.of(
                new AdvancedProcessingPatternDataStorage.PackageColumn(
                        0, PackageColor.FLUIX, "", Optional.empty())));
        boolean rejected = false;
        try {
            AdvancedProcessingPatternDataStorage.write(pattern, metadata);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }

        helper.assertTrue(PatternDetailsHelper.isEncodedPattern(pattern),
                "AE2 should encode an ordinary processing pattern");
        helper.assertFalse(AdvancedProcessingPatternDataStorage.canStore(pattern),
                "Ordinary AE2 processing patterns must not be advanced metadata carriers");
        helper.assertFalse(AdvancedProcessingPatternDataStorage.hasData(pattern),
                "Ordinary AE2 processing patterns must not contain advanced package metadata");
        helper.assertTrue(rejected,
                "Writing advanced package metadata to an ordinary AE2 processing pattern must be rejected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void advancedProcessingPatternMetadataRoundTrips(GameTestHelper helper) {
        ItemStack pattern = APItems.ADVANCED_PROCESSING_PATTERN.get().encode(
                new GenericStack[] {
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32),
                        null,
                        null,
                        null,
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 16)
                },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        MarkerSpec diamond = new MarkerSpec(new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
        MarkerSpec emerald = new MarkerSpec(new GenericStack(AEItemKey.of(Items.EMERALD), 1));
        AdvancedProcessingPatternDataStorage.write(
                pattern,
                new AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern(List.of(
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                0, PackageColor.RED, "Iron Route", Optional.of(diamond)),
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                1, PackageColor.BLUE, "Copper Route", Optional.of(emerald)))));

        var encoded = AdvancedProcessingPatternDataStorage.read(pattern).orElseThrow();
        helper.assertTrue(pattern.is(APItems.ADVANCED_PROCESSING_PATTERN.get()),
                "Advanced pattern metadata should be stored on the dedicated pattern item");
        helper.assertTrue(encoded.activeColumnCount() == 2,
                "Advanced processing pattern should preserve its active column count");
        helper.assertTrue(encoded.column(0).color() == PackageColor.RED,
                "First advanced package color should round-trip");
        helper.assertTrue(encoded.column(1).packageName().equals("Copper Route"),
                "Second advanced package name should round-trip");
        helper.assertTrue(encoded.column(0).marker().orElseThrow().sameAs(diamond),
                "First advanced package marker should round-trip");
        helper.assertTrue(encoded.column(1).marker().orElseThrow().sameAs(emerald),
                "Second advanced package marker should round-trip");
        helper.assertTrue(PatternDetailsHelper.isEncodedPattern(pattern)
                        && PatternDetailsHelper.decodePattern(pattern, helper.getLevel()) != null,
                "The dedicated advanced pattern should retain normal AE2 processing-pattern behavior");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerOrdinaryPatternUsesDefaultPackageIdentity(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        assembler.setSelectedColor(PackageColor.RED);
        assembler.setPackageName("Machine Override");
        assembler.getItems().setStackInSlot(
                PackageAssemblerBlockEntity.SLOT_MARKER,
                new ItemStack(Items.EMERALD));
        ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32) },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        KeyCounter iron = new KeyCounter();
        iron.add(AEItemKey.of(Items.IRON_INGOT), 32);

        boolean accepted = assembler.pushPattern(details, new KeyCounter[] { iron }, Direction.UP);
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData data = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(accepted, "Assembler should accept an ordinary AE2 processing pattern");
        helper.assertTrue(iron.isEmpty(), "Accepted ordinary pattern should consume its exact inputs");
        helper.assertTrue(output.is(APItems.packageItems().get(PackageColor.FLUIX).get()),
                "Ordinary processing patterns should use the default Fluix package color");
        helper.assertFalse(output.hasCustomHoverName(),
                "Ordinary processing patterns should leave the package name empty");
        helper.assertTrue(data.marker().isEmpty(),
                "Ordinary processing patterns should leave the package marker empty");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerAdvancedPatternPackagesEachColumnInOrder(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        ItemStack pattern = APItems.ADVANCED_PROCESSING_PATTERN.get().encode(
                new GenericStack[] {
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32),
                        null,
                        null,
                        null,
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 16)
                },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        MarkerSpec diamond = new MarkerSpec(new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
        MarkerSpec emerald = new MarkerSpec(new GenericStack(AEItemKey.of(Items.EMERALD), 1));
        AdvancedProcessingPatternDataStorage.write(
                pattern,
                new AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern(List.of(
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                0, PackageColor.RED, "Iron Route", Optional.of(diamond)),
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                1, PackageColor.RED, "Copper Route", Optional.of(emerald)))));
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        KeyCounter iron = new KeyCounter();
        iron.add(AEItemKey.of(Items.IRON_INGOT), 32);
        KeyCounter copper = new KeyCounter();
        copper.add(AEItemKey.of(Items.COPPER_INGOT), 16);

        boolean accepted = assembler.pushPattern(details, new KeyCounter[] { iron, copper }, Direction.UP);
        ItemStack firstOutput = assembler.getItems()
                .getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT)
                .copy();
        ItemStack secondOutput = assembler.getItems()
                .getStackInSlot(PackageAssemblerBlockEntity.outputHandlerSlot(1))
                .copy();
        PackageData firstData = PackageDataStorage.read(firstOutput).orElseThrow();
        PackageData secondData = PackageDataStorage.read(secondOutput).orElseThrow();

        helper.assertTrue(accepted, "Assembler should accept an advanced processing pattern");
        helper.assertTrue(iron.isEmpty() && copper.isEmpty(),
                "Accepted advanced pattern should consume every exact input");
        helper.assertTrue(firstOutput.is(APItems.packageItems().get(PackageColor.RED).get())
                        && secondOutput.is(APItems.packageItems().get(PackageColor.RED).get()),
                "Advanced columns with the same color should remain separate packages");
        helper.assertTrue(firstOutput.getHoverName().getString().equals("Iron Route"),
                "First package should preserve first-column name and order");
        helper.assertTrue(secondOutput.getHoverName().getString().equals("Copper Route"),
                "Second package should preserve second-column name and order");
        helper.assertTrue(firstData.marker().orElseThrow().sameAs(diamond),
                "First package should preserve first-column marker");
        helper.assertTrue(secondData.marker().orElseThrow().sameAs(emerald),
                "Second package should preserve second-column marker");
        helper.assertTrue(amountOf(firstData, AEItemKey.of(Items.IRON_INGOT)) == 32,
                "First package should contain only first-column inputs");
        helper.assertTrue(amountOf(secondData, AEItemKey.of(Items.COPPER_INGOT)) == 16,
                "Second package should contain only second-column inputs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void advancedPatternTerminalItemPlacesAe2PatternTerminalPart(GameTestHelper helper) {
        helper.assertTrue(APItems.ADVANCED_PATTERN_ENCODING_TERMINAL.get() instanceof IPartItem<?>,
                "Advanced pattern terminal item should be an AE2 part item");
        @SuppressWarnings("unchecked")
        IPartItem<AdvancedPatternEncodingTerminalPart> partItem =
                (IPartItem<AdvancedPatternEncodingTerminalPart>) APItems.ADVANCED_PATTERN_ENCODING_TERMINAL.get();
        BlockPos absolutePos = helper.absolutePos(new BlockPos(2, 2, 1));
        AdvancedPatternEncodingTerminalPart part = PartHelper.setPart(
                helper.getLevel(),
                absolutePos,
                Direction.NORTH,
                null,
                partItem);

        helper.assertTrue(part != null, "Advanced pattern terminal should place as an AE2 cable part");
        helper.assertTrue(part instanceof appeng.parts.encoding.PatternEncodingTerminalPart,
                "Advanced pattern terminal should reuse AE2 pattern terminal part behavior");
        part.getAdvancedPatternState().setActiveColumns(2);
        part.getAdvancedPatternState().setColor(0, PackageColor.GREEN);
        part.getAdvancedPatternState().setName(1, "Second Route");
        part.getAdvancedPatternState().markers().setStack(
                1,
                new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
        CompoundTag saved = new CompoundTag();
        part.writeToNBT(saved);
        part.getAdvancedPatternState().reset();
        part.readFromNBT(saved);

        helper.assertTrue(part.getAdvancedPatternState().activeColumns() == 2,
                "Advanced terminal should persist active columns");
        helper.assertTrue(part.getAdvancedPatternState().color(0) == PackageColor.GREEN,
                "Advanced terminal should persist column colors");
        helper.assertTrue(part.getAdvancedPatternState().name(1).equals("Second Route"),
                "Advanced terminal should persist column names");
        helper.assertTrue(part.getAdvancedPatternState().markers().getKey(1).equals(AEItemKey.of(Items.DIAMOND)),
                "Advanced terminal should persist column markers");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalItemPlacesAe2Part(GameTestHelper helper) {
        helper.assertTrue(APItems.PACKAGE_PATTERN_TERMINAL.get() instanceof IPartItem<?>,
                "Package pattern terminal item should be an AE2 part item");
        IPartItem<PackagePatternTerminalPart> partItem = packagePatternTerminalPartItem();
        BlockPos absolutePos = helper.absolutePos(new BlockPos(1, 2, 1));
        PackagePatternTerminalPart part = PartHelper.setPart(
                helper.getLevel(),
                absolutePos,
                Direction.NORTH,
                null,
                partItem);

        helper.assertTrue(part != null, "Package pattern terminal should place as an AE2 cable part");
        helper.assertTrue(PartHelper.getPart(partItem, helper.getLevel(), absolutePos, Direction.NORTH) == part,
                "Placed AE2 part should be retrievable from the cable bus side");

        part.setSelectedColor(PackageColor.BLUE);
        part.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        part.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                new ItemStack(APItems.PACKAGE_PATTERN.get()));

        PackagePatternTerminalBlockEntity.EncodeResult result = part.encodeOnce();
        ItemStack output = part.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        PackagePatternDataStorage.EncodedPackagePattern pattern = PackagePatternDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Package pattern terminal part should encode package patterns");
        helper.assertTrue(pattern.color() == PackageColor.BLUE,
                "Package pattern terminal part should preserve selected color in encoded output");
        helper.assertTrue(amountOf(pattern.data(), AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Package pattern terminal part should encode preview contents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalPartPersistsContents(GameTestHelper helper) {
        IPartItem<PackagePatternTerminalPart> partItem = packagePatternTerminalPartItem();
        PackagePatternTerminalPart part = partItem.createPart();
        part.setSelectedColor(PackageColor.GREEN);
        part.getItems().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT, 32));
        part.setProcessingOutputFromGhostStack(0, new ItemStack(Items.WATER_BUCKET), false);

        CompoundTag tag = new CompoundTag();
        part.writeToNBT(tag);

        PackagePatternTerminalPart loaded = partItem.createPart();
        loaded.readFromNBT(tag);

        helper.assertTrue(loaded.selectedColor() == PackageColor.GREEN,
                "Package pattern terminal part should persist selected color");
        helper.assertTrue(loaded.getItems().getStackInSlot(0).is(Items.COPPER_INGOT),
                "Package pattern terminal part should persist preview item slots");
        helper.assertTrue(loaded.processingOutput(0).is(Items.WATER_BUCKET),
                "Package pattern terminal part should persist processing output display stack");
        helper.assertTrue(loaded.processingOutputKey(0).what().equals(AEFluidKey.of(Fluids.WATER)),
                "Package pattern terminal part should persist fluid processing output key");
        helper.assertTrue(loaded.processingOutputKey(0).amount() == 1000,
                "Package pattern terminal part should persist fluid processing output amount");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalEncodesSelectedColorOntoAe2ProcessingPattern(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        terminal.setSelectedColor(PackageColor.GREEN);
        ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] {
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)
                },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        terminal.getItems().setStackInSlot(PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN, pattern);

        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
        ItemStack output = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        Optional<ColoredProcessingPatternDataStorage.EncodedColoredProcessingPattern> colored =
                ColoredProcessingPatternDataStorage.read(output);

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Terminal should encode selected color onto an AE2 processing pattern");
        helper.assertTrue(colored.isPresent(), "Output AE2 processing pattern should contain colored metadata");
        helper.assertTrue(colored.get().colorForSlot(0) == PackageColor.GREEN,
                "First processing input should use selected color");
        helper.assertTrue(colored.get().colorForSlot(1) == PackageColor.GREEN,
                "Second processing input should use selected color");
        helper.assertTrue(PatternDetailsHelper.decodePattern(output, helper.getLevel()) != null,
                "Colored AE2 processing pattern should still decode as an AE2 pattern");
        helper.assertTrue(terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN).isEmpty(),
                "Encoding should consume one source AE2 processing pattern");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalEncodesPerSlotColorsOntoAe2ProcessingPattern(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        terminal.setInputSlotColor(0, PackageColor.RED);
        terminal.setInputSlotColor(1, PackageColor.BLUE);
        ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] {
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32),
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32)
                },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        terminal.getItems().setStackInSlot(PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN, pattern);

        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
        ItemStack output = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        ColoredProcessingPatternDataStorage.EncodedColoredProcessingPattern colored =
                ColoredProcessingPatternDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Terminal should encode per-slot colors onto an AE2 processing pattern");
        helper.assertTrue(colored.colorForSlot(0) == PackageColor.RED,
                "First processing input should use its configured slot color");
        helper.assertTrue(colored.colorForSlot(1) == PackageColor.BLUE,
                "Second processing input should use its configured slot color");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalEncodesInputPreview(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        terminal.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        terminal.getItems().setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 32));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                new ItemStack(APItems.PACKAGE_PATTERN.get()));

        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
        ItemStack output = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        PackagePatternDataStorage.EncodedPackagePattern pattern = PackagePatternDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Terminal should encode a package pattern");
        helper.assertTrue(terminal.getItems().getStackInSlot(0).getCount() == 64,
                "Encoding should not consume preview input");
        helper.assertTrue(terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN).isEmpty(),
                "Encoding should consume one blank package pattern");
        helper.assertTrue(amountOf(pattern.data(), AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Encoded pattern should contain iron");
        helper.assertTrue(amountOf(pattern.data(), AEItemKey.of(Items.COPPER_INGOT)) == 32,
                "Encoded pattern should contain copper");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalEncodesAe2BlankPatternCarrier(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        ItemStack blankPattern = ae2Item("blank_pattern");
        helper.assertFalse(blankPattern.isEmpty(), "AE2 blank pattern should be registered");
        terminal.setSelectedColor(PackageColor.BLUE);
        terminal.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                blankPattern);

        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
        ItemStack output = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        PackagePatternDataStorage.EncodedPackagePattern pattern = PackagePatternDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Terminal should encode onto an AE2 blank pattern carrier");
        helper.assertTrue(PackagePatternDataStorage.isAe2BlankPattern(output),
                "Terminal should preserve the AE2 blank pattern item type");
        helper.assertTrue(pattern.color() == PackageColor.BLUE,
                "Encoded AE2 blank pattern should keep the selected color");
        helper.assertTrue(amountOf(pattern.data(), AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Encoded AE2 blank pattern should contain iron");
        helper.assertTrue(terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN).isEmpty(),
                "Encoding should consume one AE2 blank pattern carrier");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalEncodesAe2BlankPatternAsPackagedProcessing(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        ItemStack blankPattern = ae2Item("blank_pattern");
        helper.assertFalse(blankPattern.isEmpty(), "AE2 blank pattern should be registered");
        terminal.setSelectedColor(PackageColor.RED);
        terminal.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        terminal.setProcessingOutput(0, new ItemStack(Items.DIAMOND, 2));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                blankPattern);

        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
        ItemStack output = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern pattern =
                PackagedProcessingPatternDataStorage.read(output).orElseThrow();
        IPatternDetails details = PatternDetailsHelper.decodePattern(output, helper.getLevel());

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Terminal should encode AE2 blank patterns with processing outputs as AE2 processing patterns");
        helper.assertTrue(PatternDetailsHelper.isEncodedPattern(output),
                "Terminal should emit an AE2 encoded processing pattern when processing outputs are configured");
        helper.assertTrue(details != null,
                "AE2 should decode the terminal packaged-processing carrier");
        helper.assertTrue(details.getOutputs().length == 1,
                "AE2 should see the configured processing output");
        helper.assertTrue(details.getOutputs()[0].what().equals(AEItemKey.of(Items.DIAMOND)),
                "AE2 should see the processing output key");
        helper.assertTrue(details.getOutputs()[0].amount() == 2,
                "AE2 should see the processing output amount");
        helper.assertTrue(PackagePatternDataStorage.read(output).isEmpty(),
                "AE2 packaged-processing carriers should not also be read as single package patterns");
        helper.assertTrue(pattern.color() == PackageColor.RED,
                "AE2 packaged-processing carrier should keep the selected color");
        helper.assertTrue(pattern.outputs().size() == 1,
                "AE2 packaged-processing carrier should store processing outputs");
        helper.assertTrue(pattern.outputs().get(0).what().equals(AEItemKey.of(Items.DIAMOND)),
                "AE2 packaged-processing carrier should preserve output key");
        helper.assertTrue(pattern.outputs().get(0).amount() == 2,
                "AE2 packaged-processing carrier should preserve output amount");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalEncodesSelectedColor(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        terminal.setSelectedColor(PackageColor.RED);
        terminal.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                new ItemStack(APItems.PACKAGE_PATTERN.get()));

        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
        ItemStack output = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        PackagePatternDataStorage.EncodedPackagePattern pattern = PackagePatternDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Terminal should encode with a selected color");
        helper.assertTrue(pattern.color() == PackageColor.RED, "Encoded pattern should keep the selected color");
        helper.assertTrue(output.is(APItems.PACKAGE_PATTERN.get()), "Terminal should output a package pattern");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalEncodesMarkerSlot(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        MarkerSpec marker = new MarkerSpec(new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1));
        terminal.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_MARKER,
                new ItemStack(Items.GOLD_INGOT));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                new ItemStack(APItems.PACKAGE_PATTERN.get()));

        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
        ItemStack output = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        PackagePatternDataStorage.EncodedPackagePattern pattern = PackagePatternDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Terminal should encode with a marker slot");
        helper.assertTrue(pattern.data().marker().map(actual -> actual.sameAs(marker)).orElse(false),
                "Encoded pattern should store the marker slot key");
        helper.assertTrue(!terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_MARKER).isEmpty(),
                "Encoding should not consume the marker slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalUsesCapacitySlot(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.tryParse("ae2:cell_component_64k");
        helper.assertTrue(id != null, "AE2 64k storage component id should parse");
        ItemStack component = new ItemStack(BuiltInRegistries.ITEM.get(id));
        helper.assertFalse(component.isEmpty(), "AE2 64k storage component should be registered");

        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        terminal.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 640));
        terminal.getItems().setStackInSlot(PackagePatternTerminalBlockEntity.SLOT_CAPACITY, component);
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                new ItemStack(APItems.PACKAGE_PATTERN.get()));

        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
        ItemStack output = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        PackagePatternDataStorage.EncodedPackagePattern pattern = PackagePatternDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Terminal should use the configured capacity profile");
        helper.assertTrue(amountOf(pattern.data(), AEItemKey.of(Items.IRON_INGOT)) == 640,
                "64k capacity profile should allow ten iron stack units");
        helper.assertTrue(!terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_CAPACITY).isEmpty(),
                "Encoding should not consume the capacity slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalEncodesPackagedProcessingPattern(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        terminal.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                new ItemStack(APItems.PACKAGED_PROCESSING_PATTERN.get()));

        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
        ItemStack output = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern pattern =
                PackagedProcessingPatternDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Terminal should encode a packaged processing pattern shell");
        helper.assertTrue(output.is(APItems.PACKAGED_PROCESSING_PATTERN.get()),
                "Terminal should preserve the blank pattern item type");
        helper.assertTrue(pattern.packages().size() == 1,
                "Single preview package should encode as one processing package");
        helper.assertTrue(amountOf(pattern.packages().get(0), AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Encoded packaged processing pattern should contain iron");
        helper.assertTrue(pattern.outputs().isEmpty(),
                "Packaged processing pattern without ghost outputs should encode no processing outputs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalMenuEncodesProcessingOutputGhost(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = placePackagePatternTerminal(helper);
        FakePlayer player = newFakePlayer(helper);
        PackagePatternTerminalMenu menu = new PackagePatternTerminalMenu(3, new Inventory(player), terminal);
        ItemStack carried = new ItemStack(Items.DIAMOND, 2);
        terminal.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                new ItemStack(APItems.PACKAGED_PROCESSING_PATTERN.get()));

        menu.setCarried(carried.copy());
        menu.clicked(PackagePatternTerminalMenu.PROCESSING_OUTPUT_START, 0, ClickType.PICKUP, player);
        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
        ItemStack output = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern pattern =
                PackagedProcessingPatternDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Terminal should encode a packaged processing pattern with ghost outputs");
        helper.assertTrue(menu.getCarried().getCount() == 2,
                "Processing output ghost slot should not consume the carried stack");
        helper.assertTrue(terminal.processingOutput(0).is(Items.DIAMOND),
                "Terminal should store the ghost output item");
        helper.assertTrue(terminal.processingOutput(0).getCount() == 2,
                "Terminal should store the ghost output count");
        helper.assertTrue(terminal.processingOutputKey(0).what().equals(AEItemKey.of(Items.DIAMOND)),
                "Terminal should store the item output key");
        helper.assertTrue(terminal.processingOutputKey(0).amount() == 2,
                "Terminal should store the item output amount");
        helper.assertTrue(pattern.outputs().size() == 1,
                "Encoded packaged processing pattern should store processing outputs");
        helper.assertTrue(pattern.outputs().get(0).what().equals(AEItemKey.of(Items.DIAMOND)),
                "Encoded processing output should preserve the item key");
        helper.assertTrue(pattern.outputs().get(0).amount() == 2,
                "Encoded processing output should preserve the item amount");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalMenuEncodesFluidProcessingOutputGhost(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = placePackagePatternTerminal(helper);
        FakePlayer player = newFakePlayer(helper);
        PackagePatternTerminalMenu menu = new PackagePatternTerminalMenu(3, new Inventory(player), terminal);
        ItemStack blankPattern = ae2Item("blank_pattern");
        helper.assertFalse(blankPattern.isEmpty(), "AE2 blank pattern should be registered");
        terminal.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                blankPattern);

        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        menu.clicked(PackagePatternTerminalMenu.PROCESSING_OUTPUT_START, 0, ClickType.PICKUP, player);
        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
        ItemStack output = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern pattern =
                PackagedProcessingPatternDataStorage.read(output).orElseThrow();
        IPatternDetails details = PatternDetailsHelper.decodePattern(output, helper.getLevel());

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Terminal should encode an AE2 pattern with a fluid ghost output");
        helper.assertTrue(PatternDetailsHelper.isEncodedPattern(output),
                "Fluid ghost outputs on AE2 blank patterns should produce encoded processing patterns");
        helper.assertTrue(details != null,
                "AE2 should decode the fluid processing output pattern");
        helper.assertTrue(menu.getCarried().is(Items.WATER_BUCKET),
                "Fluid ghost slot should not consume the carried bucket");
        helper.assertTrue(terminal.processingOutput(0).is(Items.WATER_BUCKET),
                "Terminal should display the fluid container in the ghost slot");
        helper.assertTrue(terminal.processingOutputKey(0).what().equals(AEFluidKey.of(Fluids.WATER)),
                "Terminal should store the fluid key behind the displayed bucket");
        helper.assertTrue(terminal.processingOutputKey(0).amount() == 1000,
                "Terminal should store one bucket of fluid as the output amount");
        helper.assertTrue(pattern.outputs().size() == 1,
                "Packaged-processing carrier should persist the fluid output");
        helper.assertTrue(pattern.outputs().get(0).what().equals(AEFluidKey.of(Fluids.WATER)),
                "Packaged-processing carrier should preserve the fluid key");
        helper.assertTrue(pattern.outputs().get(0).amount() == 1000,
                "Packaged-processing carrier should preserve the fluid amount");
        helper.assertTrue(details.getOutputs().length == 1,
                "AE2 should see one fluid output");
        helper.assertTrue(details.getOutputs()[0].what().equals(AEFluidKey.of(Fluids.WATER)),
                "AE2 should see the water output key");
        helper.assertTrue(details.getOutputs()[0].amount() == 1000,
                "AE2 should see the water output amount");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalMenuAdjustsFluidProcessingOutputAmount(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = placePackagePatternTerminal(helper);
        FakePlayer player = newFakePlayer(helper);
        PackagePatternTerminalMenu menu = new PackagePatternTerminalMenu(3, new Inventory(player), terminal);
        ItemStack blankPattern = ae2Item("blank_pattern");
        helper.assertFalse(blankPattern.isEmpty(), "AE2 blank pattern should be registered");
        terminal.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                blankPattern);

        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        menu.clicked(PackagePatternTerminalMenu.PROCESSING_OUTPUT_START, 0, ClickType.PICKUP, player);
        boolean increased = menu.clickMenuButton(
                player,
                PackagePatternTerminalMenu.BUTTON_OUTPUT_AMOUNT_INCREASE_BASE);
        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
        ItemStack output = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern pattern =
                PackagedProcessingPatternDataStorage.read(output).orElseThrow();
        IPatternDetails details = PatternDetailsHelper.decodePattern(output, helper.getLevel());

        helper.assertTrue(increased,
                "Terminal menu should accept processing output amount increase buttons");
        helper.assertTrue(menu.processingOutputAmount(0) == 2000,
                "Terminal menu should expose the synchronized processing output amount");
        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Terminal should encode after increasing fluid processing output amount");
        helper.assertTrue(pattern.outputs().get(0).what().equals(AEFluidKey.of(Fluids.WATER)),
                "Packaged-processing carrier should preserve the adjusted fluid key");
        helper.assertTrue(pattern.outputs().get(0).amount() == 2000,
                "Packaged-processing carrier should preserve the adjusted fluid amount");
        helper.assertTrue(details != null && details.getOutputs()[0].amount() == 2000,
                "AE2 should see the adjusted fluid output amount");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalFluidProcessingOutputGhostPersists(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        terminal.setProcessingOutputFromGhostStack(0, new ItemStack(Items.WATER_BUCKET), false);

        CompoundTag tag = terminal.saveWithoutMetadata();
        PackagePatternTerminalBlockEntity loaded = newPackagePatternTerminal();
        loaded.load(tag);

        helper.assertTrue(loaded.processingOutput(0).is(Items.WATER_BUCKET),
                "Loaded terminal should keep the fluid container display stack");
        helper.assertTrue(loaded.processingOutputKey(0).what().equals(AEFluidKey.of(Fluids.WATER)),
                "Loaded terminal should keep the fluid output key");
        helper.assertTrue(loaded.processingOutputKey(0).amount() == 1000,
                "Loaded terminal should keep the fluid output amount");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalSplitsPackagedProcessingPattern(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        PackageData fullSource = PackageData.create(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 576)),
                Optional.empty(),
                0);
        terminal.getItems().setStackInSlot(0, packageStack(PackageColor.FLUIX, fullSource));
        terminal.getItems().setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 64));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                new ItemStack(APItems.PACKAGED_PROCESSING_PATTERN.get()));

        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();
        ItemStack output = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT);
        PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern pattern =
                PackagedProcessingPatternDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.ENCODED,
                "Terminal should encode a multi-package processing pattern");
        helper.assertTrue(pattern.packages().size() == 2,
                "Terminal should split preview contents into two default-capacity packages");
        helper.assertTrue(pattern.packages().get(0).canonicalHash().equals(fullSource.canonicalHash()),
                "First processing package should preserve the source package");
        helper.assertTrue(amountOf(pattern.packages().get(1), AEItemKey.of(Items.COPPER_INGOT)) == 64,
                "Second processing package should contain copper");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalSplitButtonConvertsPackagedProcessingPattern(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        PackageData iron = ironPackageData(PackageColor.FLUIX, 64);
        PackageData copper = PackageData.create(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        ItemStack encodedProcessingPattern = new ItemStack(APItems.PACKAGED_PROCESSING_PATTERN.get());
        PackagedProcessingPatternDataStorage.write(encodedProcessingPattern, PackageColor.FLUIX, List.of(iron, copper));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                encodedProcessingPattern);

        PackagePatternTerminalBlockEntity.SplitResult firstResult = terminal.splitOnce();
        ItemStack firstOutput = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT).copy();
        PackagePatternDataStorage.EncodedPackagePattern firstPattern =
                PackagePatternDataStorage.read(firstOutput).orElseThrow();
        terminal.getItems().setStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        PackagePatternTerminalBlockEntity.SplitResult secondResult = terminal.splitOnce();
        ItemStack secondOutput = terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT).copy();
        PackagePatternDataStorage.EncodedPackagePattern secondPattern =
                PackagePatternDataStorage.read(secondOutput).orElseThrow();
        terminal.getItems().setStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);

        helper.assertTrue(firstResult == PackagePatternTerminalBlockEntity.SplitResult.SPLIT,
                "Terminal split should emit the first package pattern");
        helper.assertTrue(secondResult == PackagePatternTerminalBlockEntity.SplitResult.SPLIT,
                "Terminal split should emit the queued package pattern");
        helper.assertTrue(PackagePatternDataStorage.isAe2BlankPattern(firstOutput),
                "Split output should use AE2 blank pattern carriers");
        helper.assertTrue(PackagePatternDataStorage.isAe2BlankPattern(secondOutput),
                "Queued split output should use AE2 blank pattern carriers");
        helper.assertFalse(firstOutput.is(APItems.PACKAGE_PATTERN.get()),
                "Split output should not create local package_pattern items in the normal player flow");
        helper.assertFalse(secondOutput.is(APItems.PACKAGE_PATTERN.get()),
                "Queued split output should not create local package_pattern items in the normal player flow");
        helper.assertTrue(amountOf(firstPattern.data(), AEItemKey.of(Items.IRON_INGOT)) == 64,
                "First split package pattern should contain iron");
        helper.assertTrue(amountOf(secondPattern.data(), AEItemKey.of(Items.COPPER_INGOT)) == 32,
                "Second split package pattern should contain copper");
        helper.assertTrue(terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN).isEmpty(),
                "Splitting should consume the encoded packaged processing pattern");
        helper.assertTrue(terminal.splitOnce() == PackagePatternTerminalBlockEntity.SplitResult.NO_PATTERN,
                "Terminal split should stop after the queue is drained");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalSplitQueuePersists(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        PackageData iron = ironPackageData(PackageColor.FLUIX, 64);
        PackageData copper = PackageData.create(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        ItemStack encodedProcessingPattern = new ItemStack(APItems.PACKAGED_PROCESSING_PATTERN.get());
        PackagedProcessingPatternDataStorage.write(encodedProcessingPattern, PackageColor.FLUIX, List.of(iron, copper));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                encodedProcessingPattern);

        PackagePatternTerminalBlockEntity.SplitResult firstResult = terminal.splitOnce();
        CompoundTag tag = terminal.saveWithoutMetadata();
        PackagePatternTerminalBlockEntity loaded = newPackagePatternTerminal();
        loaded.load(tag);
        loaded.getItems().setStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        PackagePatternTerminalBlockEntity.SplitResult secondResult = loaded.splitOnce();
        ItemStack secondOutput = loaded.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT).copy();
        PackagePatternDataStorage.EncodedPackagePattern secondPattern =
                PackagePatternDataStorage.read(secondOutput).orElseThrow();

        helper.assertTrue(firstResult == PackagePatternTerminalBlockEntity.SplitResult.SPLIT,
                "Terminal split should start before save");
        helper.assertTrue(secondResult == PackagePatternTerminalBlockEntity.SplitResult.SPLIT,
                "Loaded terminal should keep pending split package patterns");
        helper.assertTrue(PackagePatternDataStorage.isAe2BlankPattern(secondOutput),
                "Loaded split queue should keep AE2 blank pattern carriers");
        helper.assertFalse(secondOutput.is(APItems.PACKAGE_PATTERN.get()),
                "Loaded split queue should not emit local package_pattern items");
        helper.assertTrue(amountOf(secondPattern.data(), AEItemKey.of(Items.COPPER_INGOT)) == 32,
                "Loaded split queue should emit the pending copper pattern");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalClearsInputSlotColor(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        terminal.setInputSlotColor(0, PackageColor.RED);

        terminal.clearInputSlotColor(0);

        helper.assertTrue(terminal.inputSlotColor(0).isEmpty(),
                "Terminal should clear a configured input slot color");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalUsesPanelShape(GameTestHelper helper) {
        BlockState north = APBlocks.PACKAGE_PATTERN_TERMINAL.get()
                .defaultBlockState()
                .setValue(AbstractHorizontalMachineBlock.FACING, Direction.NORTH);
        BlockState east = APBlocks.PACKAGE_PATTERN_TERMINAL.get()
                .defaultBlockState()
                .setValue(AbstractHorizontalMachineBlock.FACING, Direction.EAST);

        AABB northBounds = north.getShape(helper.getLevel(), BlockPos.ZERO).bounds();
        AABB eastBounds = east.getShape(helper.getLevel(), BlockPos.ZERO).bounds();

        helper.assertTrue(closeTo(northBounds.minZ, 0.0D) && closeTo(northBounds.maxZ, 7.0D / 16.0D),
                "North-facing terminal should use a thin panel depth");
        helper.assertTrue(closeTo(eastBounds.minX, 9.0D / 16.0D) && closeTo(eastBounds.maxX, 1.0D),
                "East-facing terminal should rotate the panel shape");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalKeepsBlankWhenOutputBlocked(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        terminal.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                new ItemStack(APItems.PACKAGE_PATTERN.get()));
        terminal.getItems().setStackInSlot(
                PackagePatternTerminalBlockEntity.SLOT_OUTPUT,
                new ItemStack(APItems.PACKAGE_PATTERN.get()));

        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.OUTPUT_BLOCKED,
                "Blocked terminal should not encode");
        helper.assertTrue(!terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN).isEmpty(),
                "Blocked terminal should keep the blank pattern");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalRejectsEncodedBlankPattern(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        ItemStack encodedPattern = new ItemStack(APItems.PACKAGE_PATTERN.get());
        PackagePatternDataStorage.write(encodedPattern, PackageColor.FLUIX, ironPackageData(PackageColor.FLUIX, 64));
        terminal.getItems().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT, 32));
        terminal.getItems().setStackInSlot(PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN, encodedPattern.copy());

        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.NO_PATTERN,
                "Terminal should require an unencoded blank package pattern");
        helper.assertTrue(PackagePatternDataStorage.read(
                        terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN))
                .isPresent(), "Encoded pattern should not be overwritten");
        helper.assertTrue(terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT).isEmpty(),
                "Rejected encode should not create output");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagePatternTerminalRejectsEncodedProcessingBlankPattern(GameTestHelper helper) {
        PackagePatternTerminalBlockEntity terminal = newPackagePatternTerminal();
        ItemStack encodedPattern = new ItemStack(APItems.PACKAGED_PROCESSING_PATTERN.get());
        PackagedProcessingPatternDataStorage.write(
                encodedPattern,
                PackageColor.FLUIX,
                List.of(ironPackageData(PackageColor.FLUIX, 64)));
        terminal.getItems().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT, 32));
        terminal.getItems().setStackInSlot(PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN, encodedPattern.copy());

        PackagePatternTerminalBlockEntity.EncodeResult result = terminal.encodeOnce();

        helper.assertTrue(result == PackagePatternTerminalBlockEntity.EncodeResult.NO_PATTERN,
                "Terminal should require an unencoded blank packaged processing pattern");
        helper.assertTrue(PackagedProcessingPatternDataStorage.read(
                        terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN))
                .isPresent(), "Encoded processing pattern should not be overwritten");
        helper.assertTrue(terminal.getItems().getStackInSlot(PackagePatternTerminalBlockEntity.SLOT_OUTPUT).isEmpty(),
                "Rejected processing encode should not create output");
        helper.succeed();
    }

    private static ItemStack packageCraftingPattern(
            PackageColor color,
            Optional<MarkerSpec> marker,
            String packageName,
            GenericStack... inputs) {
        var encoded = PackageCraftingPatternDataStorage.create(
                        color,
                        sparsePackageCraftingInputs(inputs),
                        marker,
                        packageName)
                .orElseThrow();
        return PackageCraftingPatternDataStorage.encode(encoded);
    }

    private static GenericStack[] sparsePackageCraftingInputs(GenericStack... inputs) {
        GenericStack[] sparse = new GenericStack[PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT];
        if (inputs == null) {
            return sparse;
        }
        if (inputs.length > sparse.length) {
            throw new IllegalArgumentException("Package crafting test inputs exceed the crafting grid");
        }
        System.arraycopy(inputs, 0, sparse, 0, inputs.length);
        return sparse;
    }

    private static PackageData ironPackageData(PackageColor color, long amount) {
        return PackageData.create(
                color,
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), amount)),
                Optional.empty(),
                0);
    }

    private static PackageData itemPackageData(PackageColor color, long ironAmount, long copperAmount) {
        return PackageData.create(
                color,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), ironAmount),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), copperAmount)),
                Optional.empty(),
                0);
    }

    private static PackageData markedIronPackageData(PackageColor color, net.minecraft.world.item.Item markerItem) {
        return PackageData.create(
                color,
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64)),
                Optional.of(new MarkerSpec(new GenericStack(AEItemKey.of(markerItem), 1))),
                0);
    }

    private static int itemEntityAmount(GameTestHelper helper, Item item) {
        return itemEntityAmount(helper, item, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)), 8.0D);
    }

    private static int itemEntityAmount(GameTestHelper helper, Item item, Vec3 center, double radius) {
        AABB bounds = new AABB(center, center).inflate(radius);
        return helper.getLevel()
                .getEntitiesOfClass(ItemEntity.class, bounds, entity -> entity.getItem().is(item))
                .stream()
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }

    private static PackageAssemblerBlockEntity newPackageAssembler() {
        return new PackageAssemblerBlockEntity(BlockPos.ZERO, APBlocks.PACKAGE_ASSEMBLER.get().defaultBlockState());
    }

    private static void tickPackageAssemblerToCompletion(PackageAssemblerBlockEntity assembler) {
        for (int tick = 0; tick < 12; tick++) {
            assembler.serverTick();
        }
    }

    private static void fillAssemblerOutputSlots(PackageAssemblerBlockEntity assembler) {
        for (int slot = 0; slot < PackageAssemblerBlockEntity.OUTPUT_SLOT_COUNT; slot++) {
            assembler.getItems().setStackInSlot(
                    PackageAssemblerBlockEntity.outputHandlerSlot(slot),
                    packageStack(PackageColor.FLUIX, ironPackageData(PackageColor.FLUIX, 16)));
        }
    }

    private static PackageAssemblerBlockEntity placePackageAssembler(
            GameTestHelper helper,
            BlockPos pos,
            Direction facing) {
        helper.getLevel().setBlock(
                helper.absolutePos(pos),
                APBlocks.PACKAGE_ASSEMBLER.get().defaultBlockState()
                        .setValue(AbstractHorizontalMachineBlock.FACING, facing),
                3);
        return (PackageAssemblerBlockEntity) helper.getBlockEntity(pos);
    }

    private static PackageExportBusBlockEntity placePackageExportBus(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 0, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(pos),
                APBlocks.PACKAGE_EXPORT_BUS.get().defaultBlockState(),
                3);
        return (PackageExportBusBlockEntity) helper.getBlockEntity(pos);
    }

    private static PackagePatternTerminalBlockEntity placePackagePatternTerminal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 0, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(pos),
                APBlocks.PACKAGE_PATTERN_TERMINAL.get().defaultBlockState(),
                3);
        return (PackagePatternTerminalBlockEntity) helper.getBlockEntity(pos);
    }

    private static InterfaceBlockEntity placeMePackagerAe2Interface(
            GameTestHelper helper,
            BlockPos packagerPos,
            Direction networkSide) {
        BlockPos interfacePos = packagerPos.relative(networkSide);
        BlockPos drivePos = interfacePos.relative(networkSide);
        BlockPos energyCellPos = drivePos.relative(networkSide);
        Direction facing = networkSide.getAxis() == Direction.Axis.Y
                ? Direction.NORTH
                : networkSide.getOpposite();

        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(drivePos),
                AEBlocks.DRIVE.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(interfacePos),
                AEBlocks.INTERFACE.block().defaultBlockState(),
                3);
        helper.getLevel().setBlock(
                helper.absolutePos(packagerPos),
                APBlocks.ME_PACKAGER.get().defaultBlockState()
                        .setValue(AbstractHorizontalMachineBlock.FACING, facing)
                        .setValue(MePackagerBlock.NETWORK_SIDE, networkSide),
                3);
        DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(drivePos);
        drive.getInternalInventory().addItems(AEItems.ITEM_CELL_64K.stack());
        return (InterfaceBlockEntity) helper.getBlockEntity(interfacePos);
    }

    private static MEStorage aeStorage(InterfaceBlockEntity aeInterface) {
        return aeInterface.getMainNode().getGrid().getStorageService().getInventory();
    }

    private static void assertAe2InterfaceReady(
            GameTestHelper helper,
            InterfaceBlockEntity aeInterface,
            String label) {
        helper.assertTrue(aeInterface.getMainNode().isActive(),
                "AE2 Interface grid node should be active before " + label);
        helper.assertTrue(aeInterface.getMainNode().hasGridBooted(),
                "AE2 Interface grid should finish booting before " + label);
    }

    private static void assertMePackagerReady(
            GameTestHelper helper,
            MePackagerBlockEntity packager,
            String label) {
        helper.assertTrue(packager.getMainNode().isActive(),
                "ME Packager grid node should be active before " + label);
        helper.assertTrue(packager.getMainNode().hasGridBooted(),
                "ME Packager grid should finish booting before " + label);
        helper.assertTrue(packager.getMainNode().getGrid() != null,
                "ME Packager should have a grid before " + label);
    }

    private static void assertPackageAssemblerReady(
            GameTestHelper helper,
            PackageAssemblerBlockEntity assembler,
            String label) {
        helper.assertTrue(assembler.getMainNode().isActive(),
                "Package Assembler grid node should be active before " + label);
        helper.assertTrue(assembler.getMainNode().hasGridBooted(),
                "Package Assembler grid should finish booting before " + label);
        helper.assertTrue(assembler.getMainNode().getGrid() != null,
                "Package Assembler should have a grid before " + label);
    }

    private static int ironAmountInChest(ChestBlockEntity chest) {
        int amount = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(Items.IRON_INGOT)) {
                amount += stack.getCount();
            }
        }
        return amount;
    }

    private static int itemAmountInHandler(ItemStackHandler handler, Item item) {
        int amount = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item)) {
                amount += stack.getCount();
            }
        }
        return amount;
    }

    private static FakePlayer newFakePlayer(GameTestHelper helper) {
        return FakePlayerFactory.get(
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "appliedpackaging_test"));
    }

    private static int findMenuSlotWithStack(MePackagerMenu menu, ItemStack expected) {
        for (int index = 0; index < menu.slots.size(); index++) {
            ItemStack stack = menu.getSlot(index).getItem();
            if (stack.getCount() == expected.getCount() && ItemStack.isSameItemSameTags(stack, expected)) {
                return index;
            }
        }
        throw new GameTestAssertException("Expected menu slot containing " + expected);
    }

    private static PackagePatternTerminalBlockEntity newPackagePatternTerminal() {
        return new PackagePatternTerminalBlockEntity(
                BlockPos.ZERO,
                APBlocks.PACKAGE_PATTERN_TERMINAL.get().defaultBlockState());
    }

    @SuppressWarnings("unchecked")
    private static IPartItem<PackagePatternTerminalPart> packagePatternTerminalPartItem() {
        return (IPartItem<PackagePatternTerminalPart>) APItems.PACKAGE_PATTERN_TERMINAL.get();
    }

    private static ItemStack packageStack(PackageColor color, PackageData data) {
        ItemStack stack = new ItemStack(APItems.packageItems().get(color).get());
        PackageDataStorage.write(stack, data);
        return stack;
    }

    private static ItemStack ae2Item(String path) {
        ResourceLocation id = ResourceLocation.tryParse("ae2:" + path);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    private static long amountOf(PackageData data, AEKey key) {
        long amount = 0;
        for (GenericStack stack : data.contents()) {
            if (stack.what().equals(key)) {
                amount += stack.amount();
            }
        }
        return amount;
    }

    private static boolean closeTo(double actual, double expected) {
        return Math.abs(actual - expected) < 1.0e-6D;
    }

    private static boolean hasRecipeOutput(GameTestHelper helper, Item item) {
        return helper.getLevel().getRecipeManager().getRecipes().stream()
                .map(recipe -> recipe.getResultItem(helper.getLevel().registryAccess()))
                .anyMatch(result -> result.is(item));
    }

    private static void assertRecipeOutput(GameTestHelper helper, String recipeName, Item item) {
        ResourceLocation id = ResourceLocation.tryParse(AppliedPackaging.MOD_ID + ":" + recipeName);
        Optional<? extends Recipe<?>> recipe = id == null
                ? Optional.empty()
                : helper.getLevel().getRecipeManager().byKey(id);
        helper.assertTrue(recipe.isPresent(), "Recipe should exist: " + recipeName);
        ItemStack result = recipe.orElseThrow().getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(result.is(item), "Recipe should output " + BuiltInRegistries.ITEM.getKey(item));
    }

    private static final class CpuCraftingJob {
        private final GameTestHelper helper;
        private final BlockPos providerPos;
        private final AEKey output;
        private final long amount;
        private Future<ICraftingPlan> planFuture;
        private ICraftingPlan plan;
        private boolean submitted;

        private CpuCraftingJob(GameTestHelper helper, BlockPos providerPos, AEKey output, long amount) {
            this.helper = helper;
            this.providerPos = providerPos;
            this.output = output;
            this.amount = amount;
        }

        private void tickUntilStarted() {
            PatternProviderBlockEntity provider = (PatternProviderBlockEntity) helper.getBlockEntity(providerPos);
            var node = provider.getMainNode().getNode();
            if (node == null || !node.isActive() || !node.hasGridBooted()) {
                throw new GameTestAssertException("AE2 grid is not ready for crafting calculation");
            }
            var grid = node.getGrid();
            if (!grid.getCraftingService().isCraftable(output)) {
                throw new GameTestAssertException("AE2 crafting service has not indexed the processing pattern");
            }
            var source = IActionSource.ofMachine(provider);
            if (planFuture == null) {
                planFuture = grid.getCraftingService().beginCraftingCalculation(
                        helper.getLevel(),
                        () -> source,
                        output,
                        amount,
                        CalculationStrategy.REPORT_MISSING_ITEMS);
            }
            if (plan == null) {
                try {
                    plan = planFuture.get(0, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new GameTestAssertException("AE2 crafting calculation was interrupted");
                } catch (ExecutionException e) {
                    throw new GameTestAssertException("AE2 crafting calculation failed: " + e.getMessage());
                } catch (TimeoutException e) {
                    throw new GameTestAssertException("AE2 crafting calculation did not finish yet");
                }
            }
            if (plan.simulation()) {
                throw new GameTestAssertException("AE2 crafting plan is incomplete: " + plan.missingItems());
            }
            if (!submitted) {
                ICraftingSubmitResult result = grid.getCraftingService().submitJob(plan, null, null, true, source);
                if (!result.successful()) {
                    throw new GameTestAssertException("AE2 crafting job submit failed: " + result.errorCode());
                }
                submitted = true;
            }
        }

        private boolean submitted() {
            return submitted;
        }
    }

    private record DummyPatternDetails(GenericStack output) implements IPatternDetails {
        @Override
        public AEItemKey getDefinition() {
            return AEItemKey.of(Items.PAPER);
        }

        @Override
        public IInput[] getInputs() {
            return new IInput[0];
        }

        @Override
        public GenericStack[] getOutputs() {
            return new GenericStack[] { output };
        }
    }

    private static final class MemoryMEStorage implements MEStorage {
        private final KeyCounter contents = new KeyCounter();

        private void add(AEKey key, long amount) {
            contents.add(key, amount);
        }

        private long amount(AEKey key) {
            return contents.get(key);
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (amount <= 0) {
                return 0;
            }
            if (mode == Actionable.MODULATE) {
                contents.add(what, amount);
            }
            return amount;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            long extracted = Math.min(amount, contents.get(what));
            if (extracted > 0 && mode == Actionable.MODULATE) {
                contents.remove(what, extracted);
            }
            return extracted;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            out.addAll(contents);
        }

        @Override
        public Component getDescription() {
            return Component.literal("memory");
        }
    }

    private static final class TestFluidTankBlockEntity extends ChestBlockEntity {
        private final FluidTank tank;
        private final LazyOptional<IFluidHandler> fluidHandler;

        private TestFluidTankBlockEntity(BlockPos pos, BlockState blockState, int capacity) {
            super(pos, blockState);
            this.tank = new FluidTank(capacity);
            this.fluidHandler = LazyOptional.of(() -> tank);
        }

        private int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            return tank.fill(resource, action);
        }

        private FluidStack getFluid() {
            return tank.getFluid();
        }

        private int getFluidAmount() {
            return tank.getFluidAmount();
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            if (capability == ForgeCapabilities.FLUID_HANDLER) {
                return fluidHandler.cast();
            }
            return super.getCapability(capability, side);
        }

        @Override
        public void invalidateCaps() {
            super.invalidateCaps();
            fluidHandler.invalidate();
        }
    }
}

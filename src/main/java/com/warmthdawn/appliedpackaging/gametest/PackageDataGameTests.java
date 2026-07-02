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
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackagePlan;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackageTransactions;
import com.warmthdawn.appliedpackaging.core.ae2.PackageItemStorage;
import com.warmthdawn.appliedpackaging.core.fluid_handler.FluidPackagePlan;
import com.warmthdawn.appliedpackaging.core.fluid_handler.FluidPackageTransactions;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackagePlan;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackageTransactions;
import com.warmthdawn.appliedpackaging.core.package_data.ColoredProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackagedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanBuilder;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanFailure;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanResult;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.world.block.AbstractHorizontalMachineBlock;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.PackageAssemblerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.PackageExportBusBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.terminal.PackagePatternTerminalBlockEntity;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
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
        helper.assertTrue(read.get().canonicalHash().equals(data.canonicalHash()), "Canonical hash should round-trip");
        helper.assertTrue(read.get().usedUnits() == 1, "64 iron ingots should use one package unit");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void playerRecipesUseAe2BlankPatterns(GameTestHelper helper) {
        helper.assertFalse(hasRecipeOutput(helper, APItems.PACKAGE_PATTERN.get()),
                "Local package_pattern should remain as a compatibility carrier, not a player-craftable item");
        helper.assertFalse(hasRecipeOutput(helper, APItems.PACKAGED_PROCESSING_PATTERN.get()),
                "Local packaged_processing_pattern should remain as a compatibility carrier, not a player-craftable item");
        assertRecipeOutput(helper, "package_assembler", APItems.PACKAGE_ASSEMBLER.get());
        assertRecipeOutput(helper, "package_pattern_terminal", APItems.PACKAGE_PATTERN_TERMINAL.get());
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
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64)));

        helper.assertTrue(filter.matches(PackageColor.GREEN, data), "Filter should accept matching color, marker, and content");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageFilterRejectsMissingContent(GameTestHelper helper) {
        PackageData data = ironPackageData(PackageColor.RED, 32);
        PackageFilter filter = new PackageFilter(
                Optional.empty(),
                Optional.empty(),
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64)));

        helper.assertFalse(filter.matches(PackageColor.RED, data), "Content filter should require the whole requested amount");
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
        helper.assertTrue(target.getStackInSlot(0).getItem() == Items.IRON_INGOT
                        && target.getStackInSlot(0).getCount() == 64,
                "First target slot should contain iron");
        helper.assertTrue(target.getStackInSlot(1).getItem() == Items.COPPER_INGOT
                        && target.getStackInSlot(1).getCount() == 32,
                "Second target slot should contain copper");
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
        source.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        source.setStackInSlot(1, new ItemStack(Items.COPPER_INGOT, 64));
        PackageFilter filter = new PackageFilter(
                Optional.of(PackageColor.RED),
                Optional.empty(),
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64)));

        Optional<ItemPackagePlan> plan = ItemPackageTransactions.planPack(
                source,
                PackageColor.RED,
                PackageCapacityProfile.DEFAULT,
                filter);

        helper.assertTrue(plan.isPresent(), "Filtered plan should be created from matching contents");
        helper.assertTrue(amountOf(plan.get().data(), AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Filtered plan should include required iron");
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
        PackageData data = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should create one output package");
        helper.assertTrue(assembler.getItems().getStackInSlot(0).isEmpty(), "Iron input should be consumed");
        helper.assertTrue(assembler.getItems().getStackInSlot(1).isEmpty(), "Copper input should be consumed");
        helper.assertTrue(amountOf(data, AEItemKey.of(Items.IRON_INGOT)) == 64, "Package should contain iron");
        helper.assertTrue(amountOf(data, AEItemKey.of(Items.COPPER_INGOT)) == 32, "Package should contain copper");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerKeepsInputsWhenOutputBlocked(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        assembler.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        assembler.getItems().setStackInSlot(
                PackageAssemblerBlockEntity.SLOT_OUTPUT,
                packageStack(PackageColor.FLUIX, ironPackageData(PackageColor.FLUIX, 16)));

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
    public static void packageAssemblerPatternProviderPushUsesCapacitySlot(GameTestHelper helper) {
        ItemStack component = ae2Item("cell_component_64k");
        helper.assertFalse(component.isEmpty(), "AE2 64k storage component should be registered");
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY, component);
        KeyCounter iron = new KeyCounter();
        iron.add(AEItemKey.of(Items.IRON_INGOT), 640);

        boolean accepted = assembler.pushPattern(
                new DummyPatternDetails(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)),
                new KeyCounter[] { iron },
                Direction.UP);
        ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(accepted,
                "Assembler should package large Pattern Provider pushes with the configured capacity slot");
        helper.assertTrue(iron.isEmpty(), "Accepted large push should consume the input holder");
        helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.IRON_INGOT)) == 640,
                "Large Pattern Provider push should preserve all iron");
        helper.assertTrue(!assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY).isEmpty(),
                "Pattern Provider push should not consume the capacity slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerRejectsOversizedPatternProviderPushWithoutCapacity(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        KeyCounter iron = new KeyCounter();
        iron.add(AEItemKey.of(Items.IRON_INGOT), 640);

        boolean accepted = assembler.pushPattern(
                new DummyPatternDetails(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)),
                new KeyCounter[] { iron },
                Direction.UP);

        helper.assertFalse(accepted, "Default-capacity assembler should reject oversized Pattern Provider pushes");
        helper.assertTrue(iron.get(AEItemKey.of(Items.IRON_INGOT)) == 640,
                "Rejected oversized push should not consume input holders");
        helper.assertTrue(assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).isEmpty(),
                "Rejected oversized push should not create output");
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
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        PackageAssemblerBlockEntity.AssemblyResult secondResult = assembler.tryAssemble();
        ItemStack secondOutput = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
        PackageData secondData = PackageDataStorage.read(secondOutput).orElseThrow();

        helper.assertTrue(accepted,
                "Assembler should accept AE2 encoded packaged-processing pushes");
        helper.assertTrue(ironInput.isEmpty(), "Accepted packaged-processing push should consume iron input");
        helper.assertTrue(copperInput.isEmpty(), "Accepted packaged-processing push should consume copper input");
        helper.assertTrue(firstOutput.is(APItems.packageItems().get(PackageColor.RED).get()),
                "First packaged-processing output should use the encoded color");
        helper.assertTrue(firstData.canonicalHash().equals(iron.canonicalHash()),
                "First packaged-processing output should match the first package");
        helper.assertTrue(secondResult == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should output the queued packaged-processing package");
        helper.assertTrue(secondData.canonicalHash().equals(copper.canonicalHash()),
                "Second packaged-processing output should match the second package");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerAcceptsPatternProviderPush(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        KeyCounter iron = new KeyCounter();
        iron.add(AEItemKey.of(Items.IRON_INGOT), 64);
        KeyCounter copper = new KeyCounter();
        copper.add(AEItemKey.of(Items.COPPER_INGOT), 32);

        helper.assertTrue(assembler.acceptsPlans(), "Empty assembler should accept Pattern Provider plans");
        boolean accepted = assembler.pushPattern(
                new DummyPatternDetails(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)),
                new KeyCounter[] { iron, copper },
                Direction.UP);
        helper.assertTrue(accepted, "Assembler should accept Pattern Provider item inputs");
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
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        PackageAssemblerBlockEntity.AssemblyResult secondResult = assembler.tryAssemble();
        ItemStack secondOutput = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
        PackageData secondData = PackageDataStorage.read(secondOutput).orElseThrow();

        helper.assertTrue(accepted, "Assembler should accept colored processing pattern pushes");
        helper.assertTrue(iron.isEmpty(), "Colored push should consume the aggregated input holder");
        helper.assertTrue(firstOutput.is(APItems.packageItems().get(PackageColor.RED).get()),
                "First colored package should use the first slot color");
        helper.assertTrue(secondResult == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Assembler should output the queued colored package after the output is cleared");
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
        assembler.getItems().setStackInSlot(
                PackageAssemblerBlockEntity.SLOT_OUTPUT,
                packageStack(PackageColor.FLUIX, ironPackageData(PackageColor.FLUIX, 16)));
        KeyCounter iron = new KeyCounter();
        iron.add(AEItemKey.of(Items.IRON_INGOT), 64);

        boolean accepted = assembler.pushPattern(
                new DummyPatternDetails(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)),
                new KeyCounter[] { iron },
                Direction.UP);

        helper.assertFalse(accepted, "Blocked output should reject Pattern Provider pushes");
        helper.assertTrue(iron.get(AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Rejected push should not consume input holders");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerRejectsFluidPatternProviderPush(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        KeyCounter water = new KeyCounter();
        water.add(AEFluidKey.of(Fluids.WATER), 1000);

        boolean accepted = assembler.pushPattern(
                new DummyPatternDetails(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)),
                new KeyCounter[] { water },
                Direction.UP);

        helper.assertFalse(accepted, "Item-only assembler pushPattern should reject fluid inputs");
        helper.assertTrue(water.get(AEFluidKey.of(Fluids.WATER)) == 1000,
                "Rejected fluid push should not consume input holders");
        helper.assertTrue(assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).isEmpty(),
                "Rejected fluid push should not create output");
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

        helper.assertTrue(assembler.getItems().getSlots() == 12,
                "Assembler should keep its current slot count when loading legacy NBT");
        helper.assertTrue(assembler.getItems().getStackInSlot(0).is(Items.IRON_INGOT),
                "Assembler should preserve legacy input slots");
        helper.assertTrue(assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY).isEmpty(),
                "Assembler should add an empty capacity slot for legacy NBT");
        helper.succeed();
    }

    @GameTest(template = "ae_network_column")
    public static void ae2PatternProviderPushesIntoPackageAssembler(GameTestHelper helper) {
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
                                    new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                                    new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)
                            },
                            new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
                    provider.getLogic().getPatternInv().addItems(pattern);
                    provider.getLogic().updatePatterns();
                    helper.assertTrue(provider.getLogic().getAvailablePatterns().size() == 1,
                            "Pattern Provider should decode the processing pattern");
                    IPatternDetails details = provider.getLogic().getAvailablePatterns().get(0);
                    KeyCounter iron = new KeyCounter();
                    iron.add(AEItemKey.of(Items.IRON_INGOT), 64);
                    KeyCounter copper = new KeyCounter();
                    copper.add(AEItemKey.of(Items.COPPER_INGOT), 32);

                    boolean accepted = provider.getLogic().pushPattern(details, new KeyCounter[] { iron, copper });

                    helper.assertTrue(accepted, "AE2 Pattern Provider should push into Package Assembler");
                    helper.assertTrue(iron.isEmpty(), "Accepted AE2 push should consume iron input");
                    helper.assertTrue(copper.isEmpty(), "Accepted AE2 push should consume copper input");
                    ItemStack output = assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
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
                    ItemStack firstOutput =
                            assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
                    PackageData firstData = PackageDataStorage.read(firstOutput).orElseThrow();
                    assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
                    PackageAssemblerBlockEntity.AssemblyResult secondResult = assembler.tryAssemble();
                    ItemStack secondOutput =
                            assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
                    PackageData secondData = PackageDataStorage.read(secondOutput).orElseThrow();

                    helper.assertTrue(accepted,
                            "AE2 Pattern Provider should push colored processing patterns into Package Assembler");
                    helper.assertTrue(iron.isEmpty(), "Accepted colored AE2 push should consume iron input");
                    helper.assertTrue(firstOutput.is(APItems.packageItems().get(PackageColor.RED).get()),
                            "First AE2 colored output should be red");
                    helper.assertTrue(secondResult == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                            "Second AE2 colored output should be queued");
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
                    ItemStack firstOutput =
                            assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
                    PackageData firstData = PackageDataStorage.read(firstOutput).orElseThrow();
                    assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
                    PackageAssemblerBlockEntity.AssemblyResult secondResult = assembler.tryAssemble();
                    ItemStack secondOutput =
                            assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).copy();
                    PackageData secondData = PackageDataStorage.read(secondOutput).orElseThrow();

                    helper.assertTrue(accepted,
                            "AE2 Pattern Provider should push packaged-processing patterns into Package Assembler");
                    helper.assertTrue(ironInput.isEmpty(),
                            "Accepted packaged-processing AE2 push should consume iron input");
                    helper.assertTrue(copperInput.isEmpty(),
                            "Accepted packaged-processing AE2 push should consume copper input");
                    helper.assertTrue(firstOutput.is(APItems.packageItems().get(PackageColor.RED).get()),
                            "First packaged-processing AE2 output should use the encoded color");
                    helper.assertTrue(firstData.canonicalHash().equals(iron.canonicalHash()),
                            "First packaged-processing AE2 output should match the first package");
                    helper.assertTrue(secondResult == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                            "Second packaged-processing AE2 output should be queued");
                    helper.assertTrue(secondData.canonicalHash().equals(copper.canonicalHash()),
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
        CpuCraftingJob craftingJob = new CpuCraftingJob(helper, providerPos, AEItemKey.of(Items.DIAMOND), 1);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    PatternProviderBlockEntity provider =
                            (PatternProviderBlockEntity) helper.getBlockEntity(providerPos);
                    helper.assertTrue(provider.getMainNode().isActive(),
                            "Pattern Provider grid node should be active before configuring the job");
                    helper.assertTrue(provider.getMainNode().hasGridBooted(),
                            "Pattern Provider grid should finish booting before configuring the job");
                })
                .thenExecute(() -> {
                    DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(drivePos);
                    PatternProviderBlockEntity provider =
                            (PatternProviderBlockEntity) helper.getBlockEntity(providerPos);
                    drive.getInternalInventory().addItems(AEItems.ITEM_CELL_64K.stack());
                    ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                            new GenericStack[] {
                                    new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                                    new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)
                            },
                            new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
                    provider.getLogic().getPatternInv().addItems(pattern);
                    provider.getLogic().updatePatterns();
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
                    PackageAssemblerBlockEntity assembler =
                            (PackageAssemblerBlockEntity) helper.getBlockEntity(assemblerPos);
                    ItemStack output =
                            assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
                    helper.assertTrue(!output.isEmpty(), "Crafting CPU job should push inputs into Package Assembler");
                    PackageData outputData = PackageDataStorage.read(output).orElseThrow();
                    helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.IRON_INGOT)) == 64,
                            "CPU-pushed iron should be packaged");
                    helper.assertTrue(amountOf(outputData, AEItemKey.of(Items.COPPER_INGOT)) == 32,
                            "CPU-pushed copper should be packaged");
                    long requestedDiamonds = provider.getMainNode()
                            .getGrid()
                            .getCraftingService()
                            .getRequestedAmount(AEItemKey.of(Items.DIAMOND));
                    helper.assertTrue(requestedDiamonds == 1,
                            "Crafting CPU should wait for the processing pattern output after pushing inputs");
                })
                .thenSucceed();
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
        helper.assertTrue(pattern.outputs().size() == 1,
                "Encoded packaged processing pattern should store processing outputs");
        helper.assertTrue(pattern.outputs().get(0).what().equals(AEItemKey.of(Items.DIAMOND)),
                "Encoded processing output should preserve the item key");
        helper.assertTrue(pattern.outputs().get(0).amount() == 2,
                "Encoded processing output should preserve the item amount");
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
        helper.assertTrue(firstOutput.is(APItems.PACKAGE_PATTERN.get()),
                "Split output should use normal package pattern items");
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

    private static PackageData ironPackageData(PackageColor color, long amount) {
        return PackageData.create(
                color,
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), amount)),
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

    private static PackageAssemblerBlockEntity newPackageAssembler() {
        return new PackageAssemblerBlockEntity(BlockPos.ZERO, APBlocks.PACKAGE_ASSEMBLER.get().defaultBlockState());
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

    private static FakePlayer newFakePlayer(GameTestHelper helper) {
        return FakePlayerFactory.get(
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "appliedpackaging_test"));
    }

    private static PackagePatternTerminalBlockEntity newPackagePatternTerminal() {
        return new PackagePatternTerminalBlockEntity(
                BlockPos.ZERO,
                APBlocks.PACKAGE_PATTERN_TERMINAL.get().defaultBlockState());
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
}

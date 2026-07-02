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
import appeng.api.networking.security.IActionSource;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.core.definitions.AEBlocks;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackagePlan;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackageTransactions;
import com.warmthdawn.appliedpackaging.core.ae2.PackageItemStorage;
import com.warmthdawn.appliedpackaging.core.fluid_handler.FluidPackagePlan;
import com.warmthdawn.appliedpackaging.core.fluid_handler.FluidPackageTransactions;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackagePlan;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackageTransactions;
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
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.PackageAssemblerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.PackageExportBusBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.terminal.PackagePatternTerminalBlockEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
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

    private static long amountOf(PackageData data, AEKey key) {
        long amount = 0;
        for (GenericStack stack : data.contents()) {
            if (stack.what().equals(key)) {
                amount += stack.amount();
            }
        }
        return amount;
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

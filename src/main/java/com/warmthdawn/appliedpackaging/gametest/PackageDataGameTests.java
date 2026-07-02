package com.warmthdawn.appliedpackaging.gametest;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackagePlan;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackageTransactions;
import com.warmthdawn.appliedpackaging.core.ae2.PackageItemStorage;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackagePlan;
import com.warmthdawn.appliedpackaging.core.item_handler.ItemPackageTransactions;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
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
import com.warmthdawn.appliedpackaging.world.block.entity.terminal.PackagePatternTerminalBlockEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
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

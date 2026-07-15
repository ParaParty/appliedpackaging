package com.warmthdawn.appliedpackaging.gametest;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridFlags;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AEColor;
import appeng.api.util.AECableType;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.hooks.WrenchHook;
import appeng.core.definitions.AEParts;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.FakeSlot;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackagePlan;
import com.warmthdawn.appliedpackaging.core.ae2.MEStoragePackageTransactions;
import com.warmthdawn.appliedpackaging.core.ae2.PackageUnpackingOperations;
import com.warmthdawn.appliedpackaging.core.ae2.PackageItemStorage;
import com.warmthdawn.appliedpackaging.core.item_handler.PackageContentsInserter;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDetails;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanBuilder;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanFailure;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanResult;
import com.warmthdawn.appliedpackaging.core.package_data.PackageUnpacker;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternMenuBridge;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternLogicBridge;
import com.warmthdawn.appliedpackaging.part.AdvancedPatternEncodingTerminalPart;
import com.warmthdawn.appliedpackaging.part.AdvancedPatternEncodingState;
import com.warmthdawn.appliedpackaging.part.AbstractPackageBusPart;
import com.warmthdawn.appliedpackaging.part.PackageStorageBusPart;
import com.warmthdawn.appliedpackaging.part.PackageUnpackingBusPart;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.world.block.AbstractHorizontalMachineBlock;
import com.warmthdawn.appliedpackaging.world.block.MePackagerBlock;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.PackageAssemblerBlockEntity;
import com.warmthdawn.appliedpackaging.world.entity.PackageEntity;
import com.warmthdawn.appliedpackaging.world.menu.AdvancedPatternEncodingTermMenu;
import com.warmthdawn.appliedpackaging.world.menu.MePackagerMenu;
import com.warmthdawn.appliedpackaging.world.menu.PackageAssemblerMenu;
import com.warmthdawn.appliedpackaging.world.menu.PackageBusMenu;
import com.warmthdawn.appliedpackaging.world.menu.SplitIntDataSlots;
import java.util.ArrayList;
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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

@GameTestHolder(AppliedPackaging.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PackageDataGameTests {
    private PackageDataGameTests() {
    }

    @GameTest(template = "empty")
    public static void mePackagerShapeTracksFacing(GameTestHelper helper) {
        MePackagerBlock block = (MePackagerBlock) APBlocks.ME_PACKAGER.get();
        CollisionContext context = CollisionContext.empty();

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape expected = expectedMePackagerShape(facing);
            BlockState state = block.defaultBlockState()
                    .setValue(AbstractHorizontalMachineBlock.FACING, facing);
            VoxelShape outline = block.getShape(state, helper.getLevel(), BlockPos.ZERO, context);
            VoxelShape collision = block.getCollisionShape(state, helper.getLevel(), BlockPos.ZERO, context);

            helper.assertFalse(
                    Shapes.joinIsNotEmpty(outline, expected, BooleanOp.NOT_SAME),
                    "ME Packager outline must match its model for facing=" + facing);
            helper.assertFalse(
                    Shapes.joinIsNotEmpty(collision, expected, BooleanOp.NOT_SAME),
                    "ME Packager collision must match its model for facing=" + facing);
        }
        helper.succeed();
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
    public static void packageAssemblerDoesNotOccludeItsTransparentChamber(GameTestHelper helper) {
        BlockState state = APBlocks.PACKAGE_ASSEMBLER.get().defaultBlockState();
        helper.assertFalse(state.canOcclude(),
                "The cutout package assembler must not hide blocks visible through its chamber");
        helper.assertFalse(state.isRedstoneConductor(helper.getLevel(), BlockPos.ZERO),
                "The open package assembler frame must not behave as a full redstone conductor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void splitIntDataSlotsSurviveVanillaShortTransport(GameTestHelper helper) {
        SplitIntDataSlots unsyncedClient = new SplitIntDataSlots(true, () -> Integer.MAX_VALUE);
        helper.assertTrue(unsyncedClient.get() == 0,
                "Client split DataSlots should start empty until the server synchronizes them");

        int[] values = { 0, 32767, 32768, 65535, 65536, 100000, Integer.MAX_VALUE };
        for (int value : values) {
            SplitIntDataSlots server = new SplitIntDataSlots(false, () -> value);
            SplitIntDataSlots client = new SplitIntDataSlots(true, () -> 0);
            client.lowWordSlot().set((short) server.lowWordSlot().get());
            client.highWordSlot().set((short) server.highWordSlot().get());
            helper.assertTrue(client.get() == value,
                    "Split int DataSlots should preserve " + value + " through signed-short transport");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageDataPreservesOrderedEntries(GameTestHelper helper) {
        PackageData first = PackageData.create(
                PackageColor.RED,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 16),
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32)),
                Optional.empty(),
                0);
        PackageData second = PackageData.create(
                PackageColor.RED,
                List.of(
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 16),
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32),
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32)),
                Optional.empty(),
                0);
        ItemStack firstStack = new ItemStack(APItems.packageItems().get(PackageColor.RED).get());
        ItemStack secondStack = new ItemStack(APItems.packageItems().get(PackageColor.RED).get());
        PackageDataStorage.write(firstStack, first);
        PackageDataStorage.write(secondStack, second);

        helper.assertTrue(first.contents().size() == 3,
                "Package data should keep repeated keys as separate ordered entries");
        helper.assertTrue(first.contents().get(0).what().equals(AEItemKey.of(Items.IRON_INGOT))
                        && first.contents().get(1).what().equals(AEItemKey.of(Items.COPPER_INGOT))
                        && first.contents().get(2).what().equals(AEItemKey.of(Items.IRON_INGOT)),
                "Package data should preserve its input order exactly");
        helper.assertFalse(first.canonicalHash().equals(second.canonicalHash()),
                "Different content order should produce a different canonical hash");
        helper.assertFalse(ItemStack.isSameItemSameTags(firstStack, secondStack),
                "Packages with different content order should not stack");
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
    public static void encodedPatternsAreTerminalOnlyItems(GameTestHelper helper) {
        helper.assertFalse(hasRecipeOutput(helper, APItems.PACKAGE_PATTERN.get()),
                "Package patterns should only be produced by encoding, not ordinary crafting");
        helper.assertFalse(hasRecipeOutput(helper, APItems.ADVANCED_PROCESSING_PATTERN.get()),
                "Advanced processing patterns should only be encoded in the advanced terminal");
        assertRecipeOutput(helper, "package_assembler", APItems.PACKAGE_ASSEMBLER.get());
        assertRecipeOutput(helper, "advanced_pattern_encoding_terminal",
                APItems.ADVANCED_PATTERN_ENCODING_TERMINAL.get());
        assertRecipeOutput(helper, "package_storage_bus", APItems.PACKAGE_STORAGE_BUS.get());
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
    public static void packagePlanRejectsCapacityUnitOverflow(GameTestHelper helper) {
        List<GenericStack> overflowingContents = new ArrayList<>();
        for (int entry = 0; entry < 65; entry++) {
            overflowingContents.add(new GenericStack(AEItemKey.of(Items.IRON_INGOT), Long.MAX_VALUE));
        }
        PackagePlanResult result = PackagePlanBuilder.build(
                PackageColor.FLUIX,
                overflowingContents,
                List.of(),
                MarkerMergeMode.RETAIN,
                Optional.empty(),
                PackageCapacityProfile.STORAGE_256K,
                0);

        helper.assertFalse(result.success(), "Overflowing capacity units must not wrap into a valid package");
        helper.assertTrue(result.failure().orElseThrow() == PackagePlanFailure.INVALID_INPUT,
                "Overflowing capacity units should be rejected as invalid input");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageUnpackerRejectsStackAmountOverflow(GameTestHelper helper) {
        PackageData data = PackageData.create(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), Long.MAX_VALUE)),
                Optional.empty(),
                0);
        ItemStack packages = packageStack(PackageColor.FLUIX, data);
        packages.setCount(2);
        FakePlayer player = newFakePlayer(helper);

        helper.assertFalse(PackageUnpacker.unpackStackToPlayer(player, packages),
                "Overflowing stacked package contents must not partially unpack");
        helper.assertTrue(packages.getCount() == 2,
                "Rejected stacked package unpacking must preserve the complete package stack");
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

        helper.assertTrue(PackageContentsInserter.canInsert(data, target),
                "Target should simulate accepting all package contents");
        helper.assertTrue(PackageContentsInserter.insert(data, target),
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

        helper.assertFalse(PackageContentsInserter.canInsert(data, target),
                "Full incompatible target should reject complete package contents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemHandlerUnpackRejectsCumulativeSlotOverflow(GameTestHelper helper) {
        PackageData data = PackageData.create(
                PackageColor.FLUIX,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        ItemStackHandler target = new ItemStackHandler(1);

        helper.assertFalse(PackageContentsInserter.canInsert(data, target),
                "One empty slot must not independently simulate accepting both iron and copper");
        helper.assertTrue(target.getStackInSlot(0).isEmpty(),
                "Cumulative unpack simulation must not mutate the target");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageFilterReadsEncodedPatternTemplate(GameTestHelper helper) {
        PackageData data = ironPackageData(PackageColor.RED, 64);
        ItemStack pattern = packageCraftingPattern(PackageColor.RED, data);

        Optional<PackageFilter> filter = PackageFilter.fromTemplate(pattern);

        helper.assertTrue(filter.isPresent(), "Encoded pattern should be usable as a package filter template");
        helper.assertTrue(filter.get().color().orElseThrow() == PackageColor.RED,
                "Pattern filter should keep encoded color");
        helper.assertTrue(filter.get().matches(PackageColor.RED, data), "Pattern filter should match equivalent package data");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageMachinesRecognizeOnlySupportedCapacityComponents(GameTestHelper helper) {
        Map<String, PackageCapacityProfile> expected = Map.of(
                "cell_component_16k", PackageCapacityProfile.STORAGE_16K,
                "cell_component_64k", PackageCapacityProfile.STORAGE_64K,
                "cell_component_256k", PackageCapacityProfile.STORAGE_256K);
        for (var entry : expected.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse("ae2:" + entry.getKey());
            helper.assertTrue(id != null, "AE2 storage component id should parse: " + entry.getKey());
            ItemStack component = new ItemStack(BuiltInRegistries.ITEM.get(id));
            helper.assertFalse(component.isEmpty(), "AE2 storage component should be registered: " + entry.getKey());
            helper.assertTrue(PackageCapacityProfile.fromStorageComponent(component)
                            .filter(profile -> profile == entry.getValue())
                            .isPresent(),
                    "ME Packager should map " + entry.getKey() + " to its package capacity profile");
        }
        ItemStack oneKComponent = ae2Item("cell_component_1k");
        ItemStack sixteenKCell = ae2Item("item_storage_cell_16k");
        ItemStack sixteenKComponent = ae2Item("cell_component_16k");
        helper.assertFalse(oneKComponent.isEmpty(), "AE2 1k storage component should be registered");
        helper.assertFalse(sixteenKCell.isEmpty(), "AE2 16k item storage cell should be registered");
        helper.assertTrue(PackageCapacityProfile.fromStorageComponent(oneKComponent).isEmpty(),
                "Package capacity should not support the 1k storage component");
        helper.assertTrue(PackageCapacityProfile.fromStorageComponent(sixteenKCell).isEmpty(),
                "Package capacity should not accept a complete storage cell");

        MePackagerBlockEntity packager = new MePackagerBlockEntity(
                BlockPos.ZERO,
                APBlocks.ME_PACKAGER.get().defaultBlockState());
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        helper.assertTrue(packager.getItems().isItemValid(MePackagerBlockEntity.SLOT_CAPACITY, sixteenKComponent)
                        && assembler.getItems().isItemValid(
                                PackageAssemblerBlockEntity.SLOT_CAPACITY,
                                sixteenKComponent),
                "Both package machines should accept the 16k storage component");
        helper.assertFalse(packager.getItems().isItemValid(MePackagerBlockEntity.SLOT_CAPACITY, oneKComponent)
                        || assembler.getItems().isItemValid(
                                PackageAssemblerBlockEntity.SLOT_CAPACITY,
                                oneKComponent)
                        || packager.getItems().isItemValid(MePackagerBlockEntity.SLOT_CAPACITY, sixteenKCell)
                        || assembler.getItems().isItemValid(
                                PackageAssemblerBlockEntity.SLOT_CAPACITY,
                                sixteenKCell),
                "Package machine capacity slots should reject 1k components and complete cells");
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
        PackageData data = PackageData.create(
                PackageColor.BLUE,
                List.of(
                        new GenericStack(AEItemKey.of(Items.NETHER_STAR), 7),
                        new GenericStack(AEItemKey.of(Items.DRAGON_BREATH), 5)),
                Optional.empty(),
                0);
        ItemStack packageStack = packageStack(PackageColor.BLUE, data);
        Vec3 spawnPos = helper.absoluteVec(new Vec3(1.5D, 1.0D, 1.5D));
        PackageEntity entity = new PackageEntity(helper.getLevel(), spawnPos.x, spawnPos.y, spawnPos.z, packageStack);
        helper.getLevel().addFreshEntity(entity);

        boolean unpacked = entity.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.assertTrue(unpacked, "Damaging a package entity should unpack item contents");
        helper.assertTrue(entity.isRemoved(), "Damaged package entity should be removed after unpacking");
        helper.succeedWhen(() -> {
            int netherStarAmount = itemEntityAmount(helper, Items.NETHER_STAR, spawnPos, 4.0D);
            int dragonBreathAmount = itemEntityAmount(helper, Items.DRAGON_BREATH, spawnPos, 4.0D);
            helper.assertTrue(netherStarAmount == 7,
                    "Damaged package entity should drop unpacked nether stars, found " + netherStarAmount);
            helper.assertTrue(dragonBreathAmount == 5,
                    "Damaged package entity should drop unpacked dragon breath, found " + dragonBreathAmount);
        });
    }

    @GameTest(template = "empty")
    public static void packageCapacityDefaultsToNineAndUpgradesMonotonically(GameTestHelper helper) {
        helper.assertTrue(MePackagerBlockEntity.BASE_CAPACITY_PROFILE == PackageCapacityProfile.DEFAULT
                        && PackageCapacityProfile.DEFAULT.unitLimit() == 9
                        && PackageCapacityProfile.DEFAULT.typeLimit() == 9,
                "An empty package capacity slot should use the 9 unit / 9 type profile");
        helper.assertTrue(PackageCapacityProfile.STORAGE_16K.unitLimit() == 16
                        && PackageCapacityProfile.STORAGE_16K.typeLimit() == 16
                        && PackageCapacityProfile.STORAGE_64K.unitLimit() == 64
                        && PackageCapacityProfile.STORAGE_64K.typeLimit() == 63
                        && PackageCapacityProfile.STORAGE_256K.unitLimit() == 256
                        && PackageCapacityProfile.STORAGE_256K.typeLimit() == 63,
                "Supported capacity components should upgrade monotonically from 16k to 256k");

        List<GenericStack> nineTypes = List.of(
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1),
                new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 1),
                new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1),
                new GenericStack(AEItemKey.of(Items.DIAMOND), 1),
                new GenericStack(AEItemKey.of(Items.EMERALD), 1),
                new GenericStack(AEItemKey.of(Items.REDSTONE), 1),
                new GenericStack(AEItemKey.of(Items.LAPIS_LAZULI), 1),
                new GenericStack(AEItemKey.of(Items.QUARTZ), 1),
                new GenericStack(AEItemKey.of(Items.COAL), 1));

        PackagePlanResult fit = PackagePlanBuilder.build(
                PackageColor.FLUIX,
                nineTypes,
                List.of(),
                MarkerMergeMode.CLEAR,
                Optional.empty(),
                MePackagerBlockEntity.BASE_CAPACITY_PROFILE,
                0);
        helper.assertTrue(fit.success(), "The empty-slot profile should accept nine types");

        List<GenericStack> tenTypes = new ArrayList<>(nineTypes);
        tenTypes.add(new GenericStack(AEItemKey.of(Items.CHARCOAL), 1));
        PackagePlanResult tooManyTypes = PackagePlanBuilder.build(
                PackageColor.FLUIX,
                tenTypes,
                List.of(),
                MarkerMergeMode.CLEAR,
                Optional.empty(),
                MePackagerBlockEntity.BASE_CAPACITY_PROFILE,
                0);
        helper.assertFalse(tooManyTypes.success(),
                "The empty-slot profile should reject the tenth material type");
        helper.assertTrue(tooManyTypes.failure().orElseThrow() == PackagePlanFailure.CAPACITY_EXCEEDED,
                "The tenth type should fail as a capacity limit");

        PackagePlanResult tooManyUnits = PackagePlanBuilder.build(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 577)),
                List.of(),
                MarkerMergeMode.CLEAR,
                Optional.empty(),
                MePackagerBlockEntity.BASE_CAPACITY_PROFILE,
                0);
        helper.assertFalse(tooManyUnits.success(),
                "The empty-slot profile should reject more than nine item units");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mePackagerExternalCapabilityExcludesFixedMeConnectionSides(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(0, 1, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(packagerPos),
                APBlocks.ME_PACKAGER.get().defaultBlockState()
                        .setValue(AbstractHorizontalMachineBlock.FACING, Direction.EAST),
                3);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        helper.assertTrue(packager.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).resolve().isEmpty(),
                "ME Packager bottom ME connection should not expose package automation");
        helper.assertTrue(packager.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST).resolve().isEmpty(),
                "East-facing ME Packager model back should not expose package automation");
        for (Direction side : List.of(Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH)) {
            helper.assertTrue(packager.getCapability(ForgeCapabilities.ITEM_HANDLER, side).isPresent(),
                    "ME Packager non-ME side should expose package automation on side " + side);
        }
        IItemHandler sideHandler = packager.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                .resolve()
                .orElseThrow();

        ItemStack packageStack = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64));
        ItemStack insertRemainder = sideHandler.insertItem(0, packageStack.copy(), false);
        helper.assertTrue(ItemStack.isSameItemSameTags(packageStack, insertRemainder)
                        && insertRemainder.getCount() == packageStack.getCount(),
                "External package input should reject packages when no AE target can unpack them");
        helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_INPUT).isEmpty(),
                "Rejected external package insertion should not fill the internal input slot");

        ItemStack component = ae2Item("cell_component_64k");
        ItemStack rejected = sideHandler.insertItem(0, component.copy(), true);
        helper.assertTrue(ItemStack.isSameItemSameTags(component, rejected) && rejected.getCount() == component.getCount(),
                "External package input should reject non-package configuration items");

        packager.getItems().setStackInSlot(MePackagerBlockEntity.SLOT_INPUT, packageStack.copy());
        ItemStack extracted = sideHandler.extractItem(0, 1, false);
        helper.assertTrue(extracted.is(packageStack.getItem()), "Non-ME sides should expose package output");
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
                    helper.assertTrue(!packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_INPUT).isEmpty(),
                            "External insert should retain the package in heldBox until progress completes");
                    helper.assertTrue(packager.workingOperation() == MePackagerBlockEntity.WorkingOperation.UNPACKING,
                            "External insert should enter unpacking work mode for animation");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 0,
                            "External insert should not commit before unpacking progress completes");

                    ItemStack busyRejected = sideHandler.insertItem(0, packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64)), false);
                    helper.assertTrue(!busyRejected.isEmpty(),
                            "External input should reject new packages while the packager is working");
                })
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 2, () -> {
                    var storage = aeStorage(aeInterface);
                    var source = IActionSource.ofMachine(aeInterface);
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_INPUT).isEmpty(),
                            "Completed unpacking should clear heldBox");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 64,
                            "Completed unpacking should commit the full package to the selected AE network");
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
                    helper.assertTrue(!packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_INPUT).isEmpty(),
                            "Menu shift-click should retain one package in heldBox until progress completes");
                    helper.assertTrue(packager.workingOperation() == MePackagerBlockEntity.WorkingOperation.UNPACKING,
                            "Menu shift-click should enter unpacking work mode");

                    var storage = aeStorage(aeInterface);
                    var source = IActionSource.ofMachine(aeInterface);
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 0,
                            "Menu shift-click should not commit before unpacking progress completes");

                    ItemStack busyMoved = menu.quickMoveStack(player, playerPackageSlot);
                    helper.assertTrue(busyMoved.isEmpty(),
                            "Menu shift-click should reject package input while the packager is working");
                    helper.assertTrue(player.getInventory().getItem(0).getCount() == 1,
                            "Rejected busy shift-click should leave the remaining package untouched");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 128, Actionable.SIMULATE, source) == 0,
                            "Rejected busy shift-click should not unpack a second package");
                })
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 2, () -> {
                    var storage = aeStorage(aeInterface);
                    var source = IActionSource.ofMachine(aeInterface);
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 64,
                            "Menu heldBox should commit after unpacking progress completes");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void mePackagerHeldBoxSlotAcceptsNormalGuiClick(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(3, 1, 0);
        InterfaceBlockEntity aeInterface = placeMePackagerAe2Interface(helper, packagerPos, Direction.WEST);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);
        FakePlayer player = newFakePlayer(helper);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAe2InterfaceReady(helper, aeInterface, "heldBox GUI click test");
                    assertMePackagerReady(helper, packager, "heldBox GUI click test");
                })
                .thenExecute(() -> {
                    MePackagerMenu menu = new MePackagerMenu(11, new Inventory(player), packager);
                    ItemStack carried = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64));
                    carried.setCount(2);

                    ItemStack remainder = menu.getSlot(0).safeInsert(carried);

                    helper.assertTrue(remainder.getCount() == 1,
                            "Normal GUI insertion should consume exactly one package");
                    helper.assertTrue(packager.getHeldBoxItems() instanceof net.minecraftforge.items.IItemHandlerModifiable,
                            "heldBox GUI wrapper must support SlotItemHandler#set");
                    helper.assertTrue(PackageDataStorage.read(menu.getSlot(0).getItem()).isPresent(),
                            "The heldBox menu slot should immediately expose the inserted package");
                    helper.assertTrue(packager.workingOperation() == MePackagerBlockEntity.WorkingOperation.UNPACKING,
                            "Normal GUI insertion should start the unpacking progress");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void mePackagerBlockedHeldBoxRetriesAfterNetworkRecovers(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(3, 1, 0);
        InterfaceBlockEntity aeInterface = placeMePackagerAe2Interface(helper, packagerPos, Direction.WEST);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);
        packager.setBlockingMode(MePackagerBlockEntity.BlockingMode.BLOCK_UNPACK_WHEN_NETWORK_HAS_ITEMS);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAe2InterfaceReady(helper, aeInterface, "blocked heldBox retry test");
                    assertMePackagerReady(helper, packager, "blocked heldBox retry test");
                })
                .thenExecute(() -> {
                    IItemHandler sideHandler = packager.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                            .resolve()
                            .orElseThrow();
                    ItemStack input = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64));
                    helper.assertTrue(sideHandler.insertItem(0, input, false).isEmpty(),
                            "Initially empty network should accept one package into heldBox");
                    var storage = aeStorage(aeInterface);
                    long inserted = storage.insert(
                            AEItemKey.of(Items.COBBLESTONE),
                            1,
                            Actionable.MODULATE,
                            IActionSource.ofMachine(aeInterface));
                    helper.assertTrue(inserted == 1, "Network change should insert a blocking item during progress");
                })
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 3, () -> {
                    var storage = aeStorage(aeInterface);
                    var source = IActionSource.ofMachine(aeInterface);
                    helper.assertTrue(packager.unpackBlocked(),
                            "Failed final unpack commit should mark heldBox blocked");
                    helper.assertTrue(!packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_HELD_BOX).isEmpty(),
                            "Blocked unpack should retain the original package");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 0,
                            "Blocked unpack must not partially insert package contents");
                    storage.extract(AEItemKey.of(Items.COBBLESTONE), 1, Actionable.MODULATE, source);
                })
                .thenExecuteAfter(
                        MePackagerBlockEntity.CYCLIC_REDSTONE_INTERVAL_TICKS
                                + MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 6,
                        () -> {
                            var storage = aeStorage(aeInterface);
                            var source = IActionSource.ofMachine(aeInterface);
                            helper.assertFalse(packager.unpackBlocked(),
                                    "Recovered network should clear the heldBox blocked state");
                            helper.assertTrue(packager.getItems()
                                            .getStackInSlot(MePackagerBlockEntity.SLOT_HELD_BOX)
                                            .isEmpty(),
                                    "Successful retry should consume the held package");
                            helper.assertTrue(storage.extract(
                                            AEItemKey.of(Items.IRON_INGOT),
                                            64,
                                            Actionable.SIMULATE,
                                            source) == 64,
                                    "Successful retry should insert the complete package contents");
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
                    MePackagerMenu menu = new MePackagerMenu(31, new Inventory(newFakePlayer(helper)), packager);
                    var markerSlot = menu.getSlots(SlotSemantics.BLANK_PATTERN).get(0);
                    helper.assertTrue(markerSlot instanceof FakeSlot,
                            "ME Packager marker filter must be an AE2 fake slot");
                    markerSlot.set(new ItemStack(Items.DIAMOND));
                    helper.assertTrue(packager.getItems().getSlots() == 2
                                    && packager.getMarkerFilter().getStack(0) != null
                                    && packager.getMarkerFilter().getStack(0).what().equals(AEItemKey.of(Items.DIAMOND)),
                            "Marker configuration should live in the fake config inventory, not a real item slot");
                    MePackagerBlockEntity loaded = new MePackagerBlockEntity(
                            BlockPos.ZERO,
                            APBlocks.ME_PACKAGER.get().defaultBlockState());
                    loaded.load(packager.saveWithoutMetadata());
                    helper.assertTrue(loaded.getMarkerFilter().getStack(0) != null
                                    && loaded.getMarkerFilter().getStack(0).what().equals(AEItemKey.of(Items.DIAMOND)),
                            "Fake marker configuration should persist independently from the real item inventory");
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
    public static void mePackagerWrenchRotatesFacing(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(1, 1, 0);
        BlockPos absolutePos = helper.absolutePos(relativePos);
        helper.getLevel().setBlock(
                absolutePos,
                APBlocks.ME_PACKAGER.get().defaultBlockState()
                        .setValue(AbstractHorizontalMachineBlock.FACING, Direction.EAST),
                3);
        FakePlayer player = newFakePlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, AEItems.CERTUS_QUARTZ_WRENCH.stack());
        BlockHitResult topHit = new BlockHitResult(
                Vec3.atCenterOf(absolutePos).add(0.0, 0.5, 0.0),
                Direction.UP,
                absolutePos,
                false);

        InteractionResult result = WrenchHook.onPlayerUseBlock(
                player, helper.getLevel(), InteractionHand.MAIN_HAND, topHit);

        helper.assertTrue(result.consumesAction(),
                "AE2 wrench hook should rotate the ME Packager");
        helper.assertTrue(helper.getLevel().getBlockState(absolutePos)
                        .getValue(AbstractHorizontalMachineBlock.FACING) == Direction.SOUTH,
                "Clockwise top-face wrench use should rotate the model from east to south");
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(relativePos);
        helper.assertTrue(packager.getCableConnectionType(Direction.DOWN) == AECableType.SMART
                        && packager.getCableConnectionType(Direction.NORTH) == AECableType.SMART,
                "Wrench rotation should retain bottom and move model-back connectivity to north");
        helper.assertTrue(packager.getCableConnectionType(Direction.WEST) == AECableType.NONE,
                "The old east-facing model back should stop accepting ME cables after rotation");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mePackagerOnlyTransfersPackagesOnBeltSurface(GameTestHelper helper) {
        MePackagerBlock block = (MePackagerBlock) APBlocks.ME_PACKAGER.get();
        FakePlayer player = newFakePlayer(helper);
        int index = 0;

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos relativePos = new BlockPos(index++, 1, 0);
            BlockPos absolutePos = helper.absolutePos(relativePos);
            BlockState state = block.defaultBlockState().setValue(AbstractHorizontalMachineBlock.FACING, facing);
            helper.getLevel().setBlock(absolutePos, state, 3);
            MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(relativePos);
            ItemStack output = packageStack(PackageColor.FLUIX, ironPackageData(PackageColor.FLUIX, 64));

            packager.getItems().setStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT, output.copy());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            BlockHitResult beltHit = new BlockHitResult(
                    mePackagerModelLocation(absolutePos, facing, 0.75, 2.0 / 16.0, 0.5),
                    Direction.UP,
                    absolutePos,
                    false);
            block.use(state, helper.getLevel(), absolutePos, player, InteractionHand.MAIN_HAND, beltHit);
            helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                    "Belt surface should extract the package for facing=" + facing);

            packager.getItems().setStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT, output.copy());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            BlockHitResult frameHit = new BlockHitResult(
                    mePackagerModelLocation(absolutePos, facing, 0.75, 3.0 / 16.0, 1.0 / 16.0),
                    Direction.UP,
                    absolutePos,
                    false);
            block.use(state, helper.getLevel(), absolutePos, player, InteractionHand.MAIN_HAND, frameHit);
            helper.assertFalse(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                    "Frame click should open the GUI without extracting for facing=" + facing);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mePackagerPackagesFromBottomAe2Side(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(1, 3, 0);
        InterfaceBlockEntity aeInterface = placeMePackagerAe2Interface(helper, packagerPos, Direction.DOWN);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAe2InterfaceReady(helper, aeInterface, "bottom-side ME Packager test");
                    assertMePackagerReady(helper, packager, "bottom-side ME Packager test");
                })
                .thenExecute(() -> {
                    helper.assertTrue(packager.getCableConnectionType(Direction.DOWN) == AECableType.SMART,
                            "ME Packager should expose its fixed bottom ME connection");
                    helper.assertTrue(packager.getCableConnectionType(Direction.UP) == AECableType.NONE,
                            "ME Packager should not expose its top as an ME connection");
                    var storage = aeStorage(aeInterface);
                    var source = IActionSource.ofMachine(aeInterface);
                    long inserted = storage.insert(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.MODULATE, source);
                    helper.assertTrue(inserted == 64,
                            "Bottom-side AE network should accept iron before ME Packager packs");

                    MePackagerBlockEntity.MachineResult result = packager.runOnce();
                    helper.assertTrue(result == MePackagerBlockEntity.MachineResult.PACKED,
                            "ME Packager should package from its bottom AE side");
                    helper.assertTrue(packager.workingOperation() == MePackagerBlockEntity.WorkingOperation.PACKING,
                            "ME Packager should enter packing work mode before exposing output");
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "ME Packager should not expose the output package before the animation finishes");
                })
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 2, () -> {
                    ItemStack output = packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).copy();
                    PackageData data = PackageDataStorage.read(output).orElseThrow();
                    helper.assertTrue(amountOf(data, AEItemKey.of(Items.IRON_INGOT)) == 64,
                            "Bottom-side package should contain iron");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void mePackagerUsesFixedBottomAndModelBackAeConnections(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(0, 1, 0);
        BlockPos cablePos = packagerPos.relative(Direction.EAST);
        BlockPos drivePos = cablePos.relative(Direction.EAST);
        BlockPos energyCellPos = drivePos.relative(Direction.EAST);

        helper.getLevel().setBlock(
                helper.absolutePos(packagerPos),
                APBlocks.ME_PACKAGER.get().defaultBlockState()
                        .setValue(AbstractHorizontalMachineBlock.FACING, Direction.WEST),
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
                .thenWaitUntil(() -> assertMePackagerReady(helper, packager, "rear cable-side ME Packager test"))
                .thenExecute(() -> {
                    for (Direction side : Direction.values()) {
                        boolean expectedConnection = side == Direction.DOWN || side == Direction.EAST;
                        helper.assertTrue(
                                packager.getCableConnectionType(side)
                                        == (expectedConnection ? AECableType.SMART : AECableType.NONE),
                                "ME Packager should connect only on bottom and model back; checked side=" + side);
                    }
                    MEStorage storage = packager.getMainNode().getGrid().getStorageService().getInventory();
                    long inserted = storage.insert(
                            AEItemKey.of(Items.IRON_INGOT),
                            64,
                            Actionable.MODULATE,
                            IActionSource.ofMachine(packager));
                    helper.assertTrue(inserted == 64,
                            "Rear cable network should accept iron before ME Packager packs");

                    MePackagerBlockEntity.MachineResult result = packager.runOnce();
                    helper.assertTrue(result == MePackagerBlockEntity.MachineResult.PACKED,
                            "ME Packager should package from the AE grid connected through its rear side");
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "ME Packager should keep the output slot empty while the packing animation runs");
                    helper.assertTrue(storage.extract(
                            AEItemKey.of(Items.IRON_INGOT),
                            1,
                            Actionable.SIMULATE,
                            IActionSource.ofMachine(packager)) == 0,
                            "ME Packager should remove packaged iron from the rear cable network");
                })
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 2, () -> {
                    ItemStack output = packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).copy();
                    PackageData data = PackageDataStorage.read(output).orElseThrow();
                    helper.assertTrue(amountOf(data, AEItemKey.of(Items.IRON_INGOT)) == 64,
                            "Cable-side package should contain iron");
                    int expectedPhase = Math.floorMod(
                            -MePackagerBlockEntity.ANIMATION_CYCLE_TICKS,
                            MePackagerBlockEntity.BELT_SCROLL_PERIOD_PIXELS);
                    helper.assertTrue(packager.beltScrollPixels() == expectedPhase,
                            "Belt UV phase should remain at its completed outward-animation offset");

                    CompoundTag saved = packager.saveWithoutMetadata();
                    MePackagerBlockEntity loaded = new MePackagerBlockEntity(BlockPos.ZERO, packager.getBlockState());
                    loaded.load(saved);
                    helper.assertTrue(loaded.beltScrollPixels() == expectedPhase,
                            "Belt UV phase should persist across block entity save and load");
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
                .thenExecute(() -> packager.getItems().setStackInSlot(
                        MePackagerBlockEntity.SLOT_INPUT,
                        packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64))))
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 3, () -> {
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
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 3, () -> {
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
    public static void mePackagerRedstoneControlAppliesWithoutUpgradeGate(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(3, 1, 0);
        InterfaceBlockEntity aeInterface = placeMePackagerAe2Interface(helper, packagerPos, Direction.WEST);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    assertAe2InterfaceReady(helper, aeInterface, "redstone control ME Packager test");
                    assertMePackagerReady(helper, packager, "redstone control ME Packager test");
                })
                .thenExecute(() -> {
                    helper.assertFalse(packager.getUpgrades().isInstalled(AEItems.REDSTONE_CARD),
                            "Test packager should not contain a redstone card");
                    packager.setRedstoneMode(MePackagerBlockEntity.RedstoneMode.ALWAYS);
                    var storage = aeStorage(aeInterface);
                    long inserted = storage.insert(
                            AEItemKey.of(Items.IRON_INGOT),
                            64,
                            Actionable.MODULATE,
                            IActionSource.ofMachine(aeInterface));
                    helper.assertTrue(inserted == 64,
                            "No-card redstone test should insert source contents");
                })
                .thenExecuteAfter(4, () -> helper.assertTrue(
                        packager.workingOperation() == MePackagerBlockEntity.WorkingOperation.PACKING,
                        "The left-side redstone setting should control packing without a hidden upgrade gate"))
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
                        .setValue(AbstractHorizontalMachineBlock.FACING, Direction.EAST),
                3);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        MePackagerBlockEntity.MachineResult result = packager.runOnce();

        helper.assertTrue(result == MePackagerBlockEntity.MachineResult.NO_TARGET,
                "ME Packager should not treat a Forge item handler on its model back as an ME target");
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
    public static void mePackagerVisualSyncClearsRemovedPackage(GameTestHelper helper) {
        BlockPos packagerPos = new BlockPos(0, 1, 0);
        helper.getLevel().setBlock(
                helper.absolutePos(packagerPos),
                APBlocks.ME_PACKAGER.get().defaultBlockState(),
                3);
        MePackagerBlockEntity clientView = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);
        ItemStack packageStack = packageStack(PackageColor.BLUE, ironPackageData(PackageColor.BLUE, 64));
        clientView.getItems().setStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT, packageStack);
        helper.assertTrue(!clientView.getRenderedBox().isEmpty(),
                "ME Packager should render a synchronized output package");

        MePackagerBlockEntity emptyServerView = new MePackagerBlockEntity(
                helper.absolutePos(packagerPos),
                clientView.getBlockState());
        clientView.load(emptyServerView.getUpdateTag());

        helper.assertTrue(!clientView.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).isEmpty(),
                "Visual-only synchronization should leave the simulated stale client inventory untouched");
        helper.assertTrue(clientView.getRenderedBox().isEmpty(),
                "ME Packager must not render a removed package from stale client inventory data");
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
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 64,
                            "Pulse redstone should leave contents beyond the 9-unit base profile in the AE network");
                })
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 2, () -> {
                    ItemStack firstOutput = packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT).copy();
                    PackageData firstData = PackageDataStorage.read(firstOutput).orElseThrow();
                    helper.assertTrue(amountOf(firstData, AEItemKey.of(Items.IRON_INGOT)) == 576,
                            "Pulse redstone should package exactly nine item units without a component");
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
                    long inserted = storage.insert(AEItemKey.of(Items.IRON_INGOT), 576, Actionable.MODULATE, source);
                    helper.assertTrue(inserted == 576,
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
                    helper.assertTrue(amountOf(firstData, AEItemKey.of(Items.IRON_INGOT)) == 576,
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
        helper.assertTrue(MEStoragePackageTransactions.commitExtract(source, plan.get()),
                "Planned ME storage extraction should commit completely");

        helper.assertTrue(source.amount(AEItemKey.of(Items.IRON_INGOT)) == 0, "Iron should be extracted");
        helper.assertTrue(source.amount(AEItemKey.of(Items.COPPER_INGOT)) == 0, "Copper should be extracted");
        helper.assertTrue(amountOf(plan.get().data(), AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Package should contain iron");
        helper.assertTrue(amountOf(plan.get().data(), AEItemKey.of(Items.COPPER_INGOT)) == 32,
                "Package should contain copper");
        helper.assertTrue(plan.get().data().contents().size() == 2
                        && plan.get().data().contents().get(0).what().equals(AEItemKey.of(Items.COPPER_INGOT))
                        && plan.get().data().contents().get(1).what().equals(AEItemKey.of(Items.IRON_INGOT)),
                "ME Packager planning should sort package contents deterministically");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void meStoragePackCommitRollsBackWhenSourceChanges(GameTestHelper helper) {
        MemoryMEStorage backing = new MemoryMEStorage();
        backing.add(AEItemKey.of(Items.IRON_INGOT), 64);
        backing.add(AEItemKey.of(Items.COPPER_INGOT), 32);
        MEStorage changingSource = new MEStorage() {
            @Override
            public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
                return backing.insert(what, amount, mode, source);
            }

            @Override
            public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
                if (mode == Actionable.MODULATE && what.equals(AEItemKey.of(Items.COPPER_INGOT))) {
                    amount = Math.min(amount, 16);
                }
                return backing.extract(what, amount, mode, source);
            }

            @Override
            public void getAvailableStacks(KeyCounter out) {
                backing.getAvailableStacks(out);
            }

            @Override
            public Component getDescription() {
                return Component.literal("changing source");
            }
        };
        MEStoragePackagePlan plan = MEStoragePackageTransactions.planPack(
                        changingSource,
                        PackageColor.FLUIX,
                        PackageCapacityProfile.DEFAULT,
                        PackageFilter.any())
                .orElseThrow();

        helper.assertTrue(MEStoragePackageTransactions.canExtract(changingSource, plan),
                "Changing ME source should pass the initial simulation");
        helper.assertFalse(MEStoragePackageTransactions.commitExtract(changingSource, plan),
                "Short real ME extraction should fail the transaction");
        helper.assertTrue(backing.amount(AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Failed ME transaction should restore the first extracted key");
        helper.assertTrue(backing.amount(AEItemKey.of(Items.COPPER_INGOT)) == 32,
                "Failed ME transaction should restore the partially extracted key");
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
    public static void meStorageUnpackRollsBackSharedCapacityFailure(GameTestHelper helper) {
        PackageData data = PackageData.create(
                PackageColor.FLUIX,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        CapacityMEStorage target = new CapacityMEStorage(64);

        helper.assertTrue(MEStoragePackageTransactions.canInsertPackageContents(data, target),
                "Independent ME simulations expose the shared-capacity edge case");
        helper.assertFalse(MEStoragePackageTransactions.insertPackageContents(data, target),
                "Shared-capacity ME insertion should fail as one transaction");
        helper.assertTrue(target.amount(AEItemKey.of(Items.IRON_INGOT)) == 0,
                "Failed ME insertion should roll back the first content key");
        helper.assertTrue(target.amount(AEItemKey.of(Items.COPPER_INGOT)) == 0,
                "Failed ME insertion should leave the second content key absent");
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
        helper.assertTrue(MEStoragePackageTransactions.commitExtract(source, plan.get()),
                "Existing package extraction should commit completely");
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
    public static void packageCraftingPatternDecodesAsAssemblerOnlyPattern(GameTestHelper helper) {
        MarkerSpec marker = new MarkerSpec(new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
        ItemStack pattern = packageCraftingPattern(
                PackageColor.PURPLE,
                Optional.of(marker),
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32));

        helper.assertTrue(PatternDetailsHelper.isEncodedPattern(pattern),
                "Package crafting pattern should be an AE2 encoded pattern");
        helper.assertTrue(pattern.getItem() instanceof EncodedPatternItem,
                "Package crafting pattern should use AE2's encoded-pattern item API");
        ItemStack directOutput = ((EncodedPatternItem) pattern.getItem()).getOutput(pattern);
        helper.assertTrue(PackageDataStorage.read(directOutput).isPresent(),
                "Package pattern getOutput should work through the item override without a mixin decoder");
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
        helper.assertFalse(output.hasCustomHoverName(),
                "Computed package output should retain the package item's default name");
        helper.assertTrue(data.marker().isPresent() && data.marker().orElseThrow().sameAs(marker),
                "Computed package output should use the encoded marker");
        helper.assertTrue(amountOf(data, AEItemKey.of(Items.IRON_INGOT)) == 64,
                "Computed package output should contain iron");
        helper.assertTrue(amountOf(data, AEItemKey.of(Items.COPPER_INGOT)) == 32,
                "Computed package output should contain copper");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerPackageCraftingProviderPushUsesLargeEncodedPackage(GameTestHelper helper) {
        ItemStack component = ae2Item("cell_component_16k");
        helper.assertFalse(component.isEmpty(), "AE2 16k storage component should be registered");
        ItemStack pattern = packageCraftingPattern(
                PackageColor.BLUE,
                Optional.empty(),
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 640));
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        helper.assertTrue(details instanceof PackageCraftingPatternDetails,
                "AE2 should decode the large package crafting pattern");

        PackageAssemblerBlockEntity baseAssembler = newPackageAssembler();
        KeyCounter rejectedIron = new KeyCounter();
        rejectedIron.add(AEItemKey.of(Items.IRON_INGOT), 640);
        helper.assertFalse(baseAssembler.pushPattern(details, new KeyCounter[] { rejectedIron }, Direction.UP),
                "Assembler should reject a ten-unit package pattern while its capacity slot is empty");
        helper.assertTrue(rejectedIron.get(AEItemKey.of(Items.IRON_INGOT)) == 640,
                "Capacity rejection should not consume Pattern Provider inputs");

        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY, component);
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
                        Optional.empty())
                .isEmpty(), "Package crafting pattern should reject contents beyond package capacity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerAcceptsPackageCraftingPatternProviderPush(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        ItemStack pattern = packageCraftingPattern(
                PackageColor.FLUIX,
                Optional.empty(),
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
    public static void packageAssemblerRejectsPatternProviderPushWhenOutputBlocked(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        fillAssemblerOutputSlots(assembler);
        ItemStack pattern = packageCraftingPattern(
                PackageColor.FLUIX,
                Optional.empty(),
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
        ItemStack pattern = packageCraftingPattern(PackageColor.FLUIX, target);
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
    public static void packageAssemblerProviderPreflightsOrdinaryAndAdvancedCapacity(GameTestHelper helper) {
        ItemStack ordinaryPattern = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.IRON_INGOT), 640) },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        IPatternDetails ordinaryDetails = PatternDetailsHelper.decodePattern(ordinaryPattern, helper.getLevel());
        PackageAssemblerBlockEntity ordinaryAssembler = newPackageAssembler();
        KeyCounter ordinaryIron = new KeyCounter();
        ordinaryIron.add(AEItemKey.of(Items.IRON_INGOT), 640);

        helper.assertFalse(ordinaryAssembler.pushPattern(
                        ordinaryDetails,
                        new KeyCounter[] { ordinaryIron },
                        Direction.UP),
                "Ordinary provider pushes should be rejected before exceeding default package capacity");
        helper.assertTrue(ordinaryIron.get(AEItemKey.of(Items.IRON_INGOT)) == 640,
                "Rejected ordinary provider pushes must preserve every input");

        GenericStack[] advancedInputs = {
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 640)
        };
        ItemStack advancedPattern = APItems.ADVANCED_PROCESSING_PATTERN.get().encode(
                advancedInputs,
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        AdvancedProcessingPatternDataStorage.write(
                advancedPattern,
                new AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern(List.of(
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                0, PackageColor.BLUE, Optional.empty()))));
        IPatternDetails advancedDetails = PatternDetailsHelper.decodePattern(advancedPattern, helper.getLevel());
        PackageAssemblerBlockEntity advancedAssembler = newPackageAssembler();
        KeyCounter advancedIron = new KeyCounter();
        advancedIron.add(AEItemKey.of(Items.IRON_INGOT), 640);

        helper.assertFalse(advancedAssembler.pushPattern(
                        advancedDetails,
                        new KeyCounter[] { advancedIron },
                        Direction.UP),
                "Advanced provider pushes should preflight every output column against package capacity");
        helper.assertTrue(advancedIron.get(AEItemKey.of(Items.IRON_INGOT)) == 640,
                "Rejected advanced provider pushes must preserve every input");

        ItemStack component = ae2Item("cell_component_16k");
        ordinaryAssembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY, component.copy());
        advancedAssembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY, component.copy());
        helper.assertTrue(ordinaryAssembler.pushPattern(
                        ordinaryDetails,
                        new KeyCounter[] { ordinaryIron },
                        Direction.UP)
                        && advancedAssembler.pushPattern(
                                advancedDetails,
                                new KeyCounter[] { advancedIron },
                                Direction.UP),
                "The shared 16k package capacity profile should unlock both provider pattern types");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerExactPatternCannotBypassRemovedCapacityComponent(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData target = PackageData.create(
                PackageColor.FLUIX,
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 640)),
                Optional.empty(),
                0);
        assembler.getItems().setStackInSlot(
                PackageAssemblerBlockEntity.SLOT_PATTERN,
                packageCraftingPattern(PackageColor.FLUIX, target));
        assembler.getItems().setStackInSlot(
                PackageAssemblerBlockEntity.SLOT_CAPACITY,
                ae2Item("cell_component_16k"));
        for (int stack = 0; stack < 10; stack++) {
            helper.assertTrue(assembler.insertMenuInput(
                            0,
                            new ItemStack(Items.IRON_INGOT, 64),
                            64,
                            false) == 64,
                    "16k capacity should allow all ten input stacks before the component is removed");
        }
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY, ItemStack.EMPTY);

        PackageAssemblerBlockEntity.AssemblyResult result = assembler.tryAssemble();

        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.PATTERN_MISMATCH,
                "Removing the capacity component should restore the 9-unit limit before assembly");
        helper.assertTrue(assembler.menuInputAmountForDisplay(0) == 640,
                "Capacity rejection should preserve all buffered inputs");
        helper.assertTrue(assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).isEmpty(),
                "Capacity rejection should not create a package output");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerLocalPatternPreflightsCapacityAndSyncsMenuState(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = placePackageAssembler(
                helper,
                new BlockPos(0, 0, 0),
                Direction.NORTH);
        ItemStack pattern = packageCraftingPattern(
                PackageColor.FLUIX,
                Optional.empty(),
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 640));
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        PackageAssemblerMenu menu = new PackageAssemblerMenu(7, new Inventory(newFakePlayer(helper)), assembler);
        menu.broadcastChanges();
        IItemHandler external = assembler.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                .orElseThrow(IllegalStateException::new);

        helper.assertFalse(assembler.isPatternCapacityValid() || menu.isPatternCapacityValid(),
                "A local pattern above default package capacity should synchronize as invalid for GUI red state");
        helper.assertTrue(assembler.insertMenuInput(
                        0,
                        new ItemStack(Items.IRON_INGOT, 64),
                        64,
                        false) == 0
                        && !external.isItemValid(0, new ItemStack(Items.IRON_INGOT)),
                "Capacity-invalid local patterns should reject menu and external inputs before buffering them");

        assembler.getItems().setStackInSlot(
                PackageAssemblerBlockEntity.SLOT_CAPACITY,
                ae2Item("cell_component_16k"));
        menu.broadcastChanges();
        helper.assertTrue(assembler.isPatternCapacityValid()
                        && menu.isPatternCapacityValid()
                        && external.isItemValid(0, new ItemStack(Items.IRON_INGOT))
                        && assembler.insertMenuInput(
                                0,
                                new ItemStack(Items.IRON_INGOT, 64),
                                64,
                                false) == 64,
                "Installing the shared 16k capacity component should unlock the local pattern and its inputs");

        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_CAPACITY, ItemStack.EMPTY);
        menu.broadcastChanges();
        helper.assertFalse(menu.isPatternCapacityValid(),
                "Removing the capacity component should immediately restore the GUI invalid state");
        helper.assertTrue(assembler.tryAssemble() == PackageAssemblerBlockEntity.AssemblyResult.PATTERN_MISMATCH
                        && assembler.menuInputAmountForDisplay(0) == 64,
                "Capacity regression should block assembly without consuming retained local inputs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerMenuInputInvalidAfterPatternRemoved(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        PackageData target = ironPackageData(PackageColor.FLUIX, 64);
        ItemStack pattern = packageCraftingPattern(PackageColor.FLUIX, target);
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
    public static void packageAssemblerMenuUsesFourByFourDenseInputWindow(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = placePackageAssembler(
                helper,
                new BlockPos(0, 0, 0),
                Direction.NORTH);
        List<GenericStack> orderedInputs = new ArrayList<>();
        for (int input = 0; input < 17; input++) {
            orderedInputs.add(new GenericStack(AEItemKey.of(Items.IRON_INGOT), input + 1));
        }
        PackageData target = PackageData.create(
                PackageColor.FLUIX,
                orderedInputs,
                Optional.empty(),
                0);
        ItemStack pattern = packageCraftingPattern(PackageColor.FLUIX, target);
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
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT, secondRowPackage);
        menu.broadcastChanges();
        menu.setScrollOffset(1);

        helper.assertTrue(PackageAssemblerMenu.MENU_INPUT_END - PackageAssemblerMenu.MENU_INPUT_START == 16,
                "Assembler menu should expose a 4x4 visible input window");
        helper.assertTrue(PackageAssemblerMenu.OUTPUT_END - PackageAssemblerMenu.OUTPUT_START == 2,
                "Assembler menu should expose one real output and one preview slot");
        helper.assertTrue(menu.maxScrollOffset() == 1,
                "Assembler menu scroll range should follow the dense input rows");
        helper.assertTrue(menu.getSlot(menu.menuInputMenuSlotIndex(0)).getItem().is(fifthFilter.getItem()),
                "Scrolled 4x4 input window should map its first visible slot to the next input row");
        helper.assertTrue(menu.getSlot(menu.outputMenuSlotIndex(0)).getItem().is(secondRowPackage.getItem()),
                "Assembler main output should remain fixed while input rows scroll");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerOrderedOutputSupportsSlotSynchronization(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        ItemStack packageStack = packageStack(PackageColor.BLUE, ironPackageData(PackageColor.BLUE, 16));
        SlotItemHandler outputSlot = new SlotItemHandler(assembler.getOrderedOutputItems(), 0, 0, 0);

        outputSlot.set(packageStack.copy());

        helper.assertTrue(ItemStack.isSameItemSameTags(
                        assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT),
                        packageStack),
                "Assembler ordered output must support SlotItemHandler#set during menu synchronization");
        outputSlot.set(ItemStack.EMPTY);
        helper.assertTrue(assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).isEmpty(),
                "Assembler ordered output synchronization should update the backing output slot");
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
        assembler.getItems().setStackInSlot(
                PackageAssemblerBlockEntity.SLOT_PATTERN,
                packageCraftingPattern(PackageColor.FLUIX, ironPackageData(PackageColor.FLUIX, 16)));
        ItemStack first = packageStack(PackageColor.FLUIX, ironPackageData(PackageColor.FLUIX, 16));
        first.setCount(2);
        ItemStack second = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 16));
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT, first);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.outputHandlerSlot(1), second);
        CompoundTag saved = assembler.saveWithoutMetadata();
        PackageAssemblerBlockEntity loaded = newPackageAssembler();
        loaded.load(saved);
        IItemHandler handler = loaded.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                .orElseThrow(IllegalStateException::new);
        int outputSlot = loaded.externalOutputSlot();

        ItemStack skipped = handler.extractItem(0, 64, false);
        ItemStack firstExtract = handler.extractItem(outputSlot, 64, false);
        ItemStack secondExtract = handler.extractItem(outputSlot, 64, false);
        ItemStack thirdExtract = handler.extractItem(outputSlot, 64, false);

        helper.assertTrue(handler.getSlots() == 2 && outputSlot == 1 && skipped.isEmpty(),
                "External handler should expose only the installed pattern input plus ordered output");
        helper.assertTrue(firstExtract.getCount() == 1 && firstExtract.is(APItems.packageItems().get(PackageColor.FLUIX).get()),
                "External handler should extract one package from the first output slot");
        helper.assertTrue(secondExtract.getCount() == 1 && secondExtract.is(APItems.packageItems().get(PackageColor.FLUIX).get()),
                "External handler should extract the next package from the first output slot before later slots");
        helper.assertTrue(thirdExtract.getCount() == 1 && thirdExtract.is(APItems.packageItems().get(PackageColor.RED).get()),
                "External handler should promote the next queued package after earlier output is empty");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerExternalInputRequiresPatternAndPreservesPatternOrder(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        IItemHandler handler = assembler.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                .orElseThrow(IllegalStateException::new);
        helper.assertTrue(handler.getSlots() == 1 && assembler.externalOutputSlot() == 0,
                "Assembler without a pattern should expose no external input slots");
        ItemStack ironWithoutPattern = new ItemStack(Items.IRON_INGOT, 32);

        helper.assertTrue(handler.insertItem(0, ironWithoutPattern, false).getCount() == 32,
                "External input should reject items while no pattern is installed");

        assembler.getItems().setStackInSlot(
                PackageAssemblerBlockEntity.SLOT_PATTERN,
                packageCraftingPattern(
                        PackageColor.FLUIX,
                        Optional.empty(),
                        new GenericStack(AEFluidKey.of(Fluids.WATER), 1000)));
        helper.assertTrue(handler.getSlotLimit(0) == 0
                        && !handler.isItemValid(0, new ItemStack(Items.WATER_BUCKET)),
                "Forge item capability should not advertise a fluid pattern input as an item slot");

        PackageData target = PackageData.create(
                PackageColor.FLUIX,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 16),
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 8)),
                Optional.empty(),
                0);
        assembler.getItems().setStackInSlot(
                PackageAssemblerBlockEntity.SLOT_PATTERN,
                packageCraftingPattern(PackageColor.FLUIX, target));

        helper.assertTrue(handler.isItemValid(0, new ItemStack(Items.IRON_INGOT))
                        && !handler.isItemValid(0, new ItemStack(Items.COPPER_INGOT)),
                "External input slots should use the installed pattern as a positional filter");
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.COPPER_INGOT, 16), false).getCount() == 16,
                "External input should reject an item in the wrong pattern position");
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 32), false).isEmpty()
                        && handler.insertItem(1, new ItemStack(Items.COPPER_INGOT, 16), false).isEmpty()
                        && handler.insertItem(2, new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(),
                "External input should accept the complete installed pattern");

        PackageAssemblerBlockEntity.AssemblyResult result = assembler.tryAssemble();
        helper.assertTrue(handler.getSlots() == 4 && assembler.externalOutputSlot() == 3,
                "External input slot count should follow the three installed pattern inputs");
        ItemStack output = handler.getStackInSlot(assembler.externalOutputSlot());
        PackageData outputData = PackageDataStorage.read(output).orElseThrow();

        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED,
                "Pattern-filtered external inputs should assemble successfully");
        helper.assertTrue(outputData.contents().size() == 3
                        && outputData.contents().get(0).what().equals(AEItemKey.of(Items.IRON_INGOT))
                        && outputData.contents().get(0).amount() == 32
                        && outputData.contents().get(1).what().equals(AEItemKey.of(Items.COPPER_INGOT))
                        && outputData.contents().get(1).amount() == 16
                        && outputData.contents().get(2).what().equals(AEItemKey.of(Items.IRON_INGOT))
                        && outputData.contents().get(2).amount() == 8,
                "Pattern output should preserve every entry in exact pattern order");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void machineCapabilitiesReviveAfterInvalidation(GameTestHelper helper) {
        MePackagerBlockEntity packager = new MePackagerBlockEntity(
                BlockPos.ZERO,
                APBlocks.ME_PACKAGER.get().defaultBlockState());
        LazyOptional<IItemHandler> packagerInternal =
                packager.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
        LazyOptional<IItemHandler> packagerExternal =
                packager.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP);
        helper.assertTrue(packagerInternal.isPresent() && packagerExternal.isPresent(),
                "ME Packager item capabilities should initially be available");

        packager.invalidateCaps();
        helper.assertFalse(packagerInternal.isPresent() || packagerExternal.isPresent(),
                "Invalidating ME Packager capabilities should invalidate existing handles");
        packager.reviveCaps();
        helper.assertTrue(packager.getCapability(ForgeCapabilities.ITEM_HANDLER, null).isPresent()
                        && packager.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).isPresent(),
                "Reviving the ME Packager should recreate both item capabilities");

        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        LazyOptional<IItemHandler> assemblerItems =
                assembler.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP);
        LazyOptional<?> assemblerCrafting =
                assembler.getCapability(appeng.capabilities.Capabilities.CRAFTING_MACHINE, Direction.UP);
        helper.assertTrue(assemblerItems.isPresent() && assemblerCrafting.isPresent(),
                "Package Assembler item and crafting-machine capabilities should initially be available");

        assembler.invalidateCaps();
        helper.assertFalse(assemblerItems.isPresent() || assemblerCrafting.isPresent(),
                "Invalidating Package Assembler capabilities should invalidate existing handles");
        assembler.reviveCaps();
        helper.assertTrue(assembler.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).isPresent()
                        && assembler.getCapability(
                                        appeng.capabilities.Capabilities.CRAFTING_MACHINE,
                                        Direction.UP)
                                .isPresent(),
                "Reviving the Package Assembler should recreate item and crafting-machine capabilities");
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
        ItemStack pattern = packageCraftingPattern(PackageColor.FLUIX, target);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.setOutputMode(PackageAssemblerBlockEntity.OutputMode.ADJACENT_BLOCK);
        assembler.insertMenuInput(0, new ItemStack(Items.IRON_INGOT, 64), 64, false);

        helper.startSequence()
                .thenWaitUntil(() -> assertPackageAssemblerReady(helper, assembler, "adjacent auto-export test"))
                .thenExecute(() -> {
                    tickPackageAssemblerToCompletion(assembler);

                    ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
                    ItemStack exported = chest.getItem(0);
                    PackageData exportedData = PackageDataStorage.read(exported).orElseThrow();
                    helper.assertTrue(assembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT).isEmpty(),
                            "Auto-export should clear the assembler output slot");
                    helper.assertTrue(assembler.menuInputDisplay(0).isEmpty(),
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
        ItemStack pattern = packageCraftingPattern(PackageColor.FLUIX, target);
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
        assembler.insertMenuInput(0, new ItemStack(Items.IRON_INGOT, 64), 64, false);

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
                    ItemStack pattern = packageCraftingPattern(PackageColor.FLUIX, target);
                    assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
                    assembler.insertMenuInput(0, new ItemStack(Items.IRON_INGOT, 64), 64, false);
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
                    ItemStack pattern = packageCraftingPattern(PackageColor.FLUIX, target);
                    assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, pattern);
                    assembler.insertMenuInput(0, new ItemStack(Items.IRON_INGOT, 64), 64, false);

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
                        .setValue(AbstractHorizontalMachineBlock.FACING, Direction.EAST),
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
                    helper.assertTrue(!packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_INPUT).isEmpty(),
                            "ME Packager should retain the input package during unpacking progress");
                    helper.assertTrue(storage.extract(AEItemKey.of(Items.IRON_INGOT), 64, Actionable.SIMULATE, source) == 0,
                            "AE2 Interface network should not receive iron before progress completes");
                })
                .thenExecuteAfter(MePackagerBlockEntity.ANIMATION_CYCLE_TICKS + 2, () -> {
                    InterfaceBlockEntity aeInterface =
                            (InterfaceBlockEntity) helper.getBlockEntity(interfacePos);
                    MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);
                    var storage = aeInterface.getMainNode().getGrid().getStorageService().getInventory();
                    var source = IActionSource.ofMachine(aeInterface);
                    helper.assertTrue(packager.getItems().getStackInSlot(MePackagerBlockEntity.SLOT_INPUT).isEmpty(),
                            "ME Packager should consume heldBox after unpacking progress");
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
                        .setValue(AbstractHorizontalMachineBlock.FACING, Direction.EAST),
                3);
        MePackagerBlockEntity packager = (MePackagerBlockEntity) helper.getBlockEntity(packagerPos);

        MePackagerBlockEntity.MachineResult result = packager.runOnce();
        helper.assertTrue(result == MePackagerBlockEntity.MachineResult.NO_TARGET,
                "ME Packager should not treat a Forge fluid handler on its model back as an ME target");
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
    public static void packageItemStorageSimulationDoesNotOverbookSlot(GameTestHelper helper) {
        ItemStackHandler target = new ItemStackHandler(1);
        ItemStack legalPackage = packageStack(PackageColor.GREEN, ironPackageData(PackageColor.GREEN, 64));
        PackageItemStorage storage = new PackageItemStorage(
                target,
                net.minecraft.network.chat.Component.literal("test"));

        long simulated = storage.insert(
                AEItemKey.of(legalPackage),
                128,
                Actionable.SIMULATE,
                IActionSource.empty());

        helper.assertTrue(simulated == legalPackage.getMaxStackSize(),
                "Package storage simulation must report only the one-slot capacity");
        helper.assertTrue(target.getStackInSlot(0).isEmpty(),
                "Package storage simulation must not mutate the target");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageBusPackageCommitsOnlyAfterSeparateFinishStep(GameTestHelper helper) {
        ItemStack packageStack = packageStack(PackageColor.BLUE, ironPackageData(PackageColor.BLUE, 64));
        ItemStackHandler target = new ItemStackHandler(1);

        helper.assertTrue(PackageUnpackingOperations.canUnpack(packageStack, target),
                "Unpacking bus should accept a complete package before starting its work phase");
        helper.assertTrue(target.getStackInSlot(0).isEmpty(),
                "Validating a package must not insert contents before work finishes");
        helper.assertTrue(PackageUnpackingOperations.unpack(packageStack, target),
                "The separate finish step should commit a still-valid held package");
        helper.assertTrue(itemAmountInHandler(target, Items.IRON_INGOT) == 64,
                "The finish step should insert the complete package contents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageBusHeldPackageSurvivesFinalTargetChange(GameTestHelper helper) {
        ItemStack packageStack = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64));
        ItemStackHandler target = new ItemStackHandler(1);
        helper.assertTrue(PackageUnpackingOperations.canUnpack(packageStack, target),
                "The target should accept the package before the work phase starts");

        target.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
        boolean blocked = PackageUnpackingOperations.unpack(packageStack, target);

        helper.assertFalse(blocked,
                "Final unpack commit should fail when the destination changes during progress");
        helper.assertTrue(PackageItemStorage.isLegalPackageStack(packageStack),
                "A failed final commit must leave the original held package intact");
        helper.assertTrue(itemAmountInHandler(target, Items.IRON_INGOT) == 0,
                "A failed final commit must not partially insert package contents");

        target.setStackInSlot(0, ItemStack.EMPTY);
        helper.assertTrue(PackageUnpackingOperations.unpack(packageStack, target),
                "The same held package should commit after the destination becomes valid again");
        helper.assertTrue(itemAmountInHandler(target, Items.IRON_INGOT) == 64,
                "Retrying the held package should insert its complete contents");
        helper.succeed();
    }

    private static VoxelShape expectedMePackagerShape(Direction facing) {
        return switch (facing) {
            case EAST -> Shapes.or(
                    Block.box(0, 0, 0, 16, 1, 16),
                    Block.box(0, 1, 0, 4, 16, 16),
                    Block.box(4, 1, 0, 16, 3, 2),
                    Block.box(4, 1, 14, 16, 3, 16),
                    Block.box(1, 1, 2, 16, 2, 14)).optimize();
            case SOUTH -> Shapes.or(
                    Block.box(0, 0, 0, 16, 1, 16),
                    Block.box(0, 1, 0, 16, 16, 4),
                    Block.box(0, 1, 4, 2, 3, 16),
                    Block.box(14, 1, 4, 16, 3, 16),
                    Block.box(2, 1, 1, 14, 2, 16)).optimize();
            case WEST -> Shapes.or(
                    Block.box(0, 0, 0, 16, 1, 16),
                    Block.box(12, 1, 0, 16, 16, 16),
                    Block.box(0, 1, 0, 12, 3, 2),
                    Block.box(0, 1, 14, 12, 3, 16),
                    Block.box(0, 1, 2, 15, 2, 14)).optimize();
            case NORTH -> Shapes.or(
                    Block.box(0, 0, 0, 16, 1, 16),
                    Block.box(0, 1, 12, 16, 16, 16),
                    Block.box(0, 1, 0, 2, 3, 12),
                    Block.box(14, 1, 0, 16, 3, 12),
                    Block.box(2, 1, 0, 14, 2, 15)).optimize();
            default -> throw new IllegalArgumentException("ME Packager has no vertical facing: " + facing);
        };
    }

    private static Vec3 mePackagerModelLocation(
            BlockPos pos,
            Direction facing,
            double modelX,
            double modelY,
            double modelZ) {
        double worldX;
        double worldZ;
        switch (facing) {
            case SOUTH -> {
                worldX = 1.0 - modelZ;
                worldZ = modelX;
            }
            case WEST -> {
                worldX = 1.0 - modelX;
                worldZ = 1.0 - modelZ;
            }
            case NORTH -> {
                worldX = modelZ;
                worldZ = 1.0 - modelX;
            }
            default -> {
                worldX = modelX;
                worldZ = modelZ;
            }
        }
        return new Vec3(pos.getX() + worldX, pos.getY() + modelY, pos.getZ() + worldZ);
    }

    @GameTest(template = "empty")
    public static void packageUnpackingBlockingMatchesPatternProviderInputRule(GameTestHelper helper) {
        ItemStack packageStack = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 32));
        ItemStackHandler target = new ItemStackHandler(2);
        target.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 1));

        helper.assertTrue(PackageUnpackingOperations.canUnpack(packageStack, target, false),
                "Non-blocking unpacking should allow merging with an existing package input type");
        helper.assertFalse(PackageUnpackingOperations.canUnpack(packageStack, target, true),
                "Pattern Provider blocking should reject a target containing a package input type");

        target.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 1));
        helper.assertTrue(PackageUnpackingOperations.canUnpack(packageStack, target, true),
                "Blocking mode should ignore target stacks that are not package input types");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void packageUnpackingBusPartDelaysCommitUntilProgressCompletes(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 1, 0);
        BlockPos partPos = new BlockPos(1, 1, 1);
        helper.getLevel().setBlock(helper.absolutePos(chestPos), Blocks.CHEST.defaultBlockState(), 3);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
        PackageUnpackingBusPart part = placePoweredUnpackingBusPart(helper, partPos, Direction.NORTH);
        ItemStack packageStack = packageStack(PackageColor.GREEN, ironPackageData(PackageColor.GREEN, 64));
        AEItemKey packageKey = AEItemKey.of(packageStack);

        helper.startSequence()
                .thenWaitUntil(() -> assertPackageUnpackingPartStorageReady(helper, part, packageStack))
                .thenExecute(() -> {
                    MEStorage storage = part.getMainNode().getGrid().getStorageService().getInventory();
                    helper.assertTrue(storage.insert(
                                    packageKey,
                                    1,
                                    Actionable.MODULATE,
                                    IActionSource.ofMachine(part)) == 1,
                            "Unpacking part grid should accept the source package");
                })
                .thenWaitUntil(() -> helper.assertTrue(!part.heldPackage().isEmpty(),
                        "Unpacking part should reserve one package as its held work item"))
                .thenExecute(() -> {
                    MEStorage storage = part.getMainNode().getGrid().getStorageService().getInventory();
                    helper.assertTrue(part.isWorking(),
                            "A reserved package should enter the same progress phase as ME Packager unpacking");
                    helper.assertTrue(itemAmountInContainer(chest, Items.IRON_INGOT) == 0,
                            "Package contents must remain uncommitted while progress is running");
                    helper.assertTrue(storage.extract(
                                    packageKey,
                                    1,
                                    Actionable.SIMULATE,
                                    IActionSource.ofMachine(part)) == 0,
                            "The in-progress package should be reserved locally, not duplicated in ME storage");
                })
                .thenExecuteAfter(PackageUnpackingBusPart.ANIMATION_CYCLE_TICKS + 2, () -> {
                    helper.assertTrue(part.heldPackage().isEmpty(),
                            "A successful final commit should clear the held package");
                    helper.assertTrue(itemAmountInContainer(chest, Items.IRON_INGOT) == 64,
                            "The completed work phase should commit all package contents at once");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void packageUnpackingBusPartUsesFormationInputAndPatternBlocking(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 1, 0);
        BlockPos partPos = new BlockPos(1, 1, 1);
        helper.getLevel().setBlock(helper.absolutePos(chestPos), Blocks.CHEST.defaultBlockState(), 3);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.IRON_INGOT, 1));
        PackageUnpackingBusPart part = placePoweredUnpackingBusPart(helper, partPos, Direction.NORTH);
        part.getConfigManager().putSetting(Settings.BLOCKING_MODE, YesNo.YES);
        ItemStack packageStack = packageStack(PackageColor.BLUE, ironPackageData(PackageColor.BLUE, 32));
        AEItemKey packageKey = AEItemKey.of(packageStack);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(part.getMainNode().isOnline() && part.getMainNode().hasGridBooted(),
                            "Package unpacking bus should join its powered AE grid");
                    helper.assertTrue(part.getMainNode().getGrid() != null,
                            "Package unpacking bus should expose its connected grid");
                })
                .thenExecute(() -> {
                    MEStorage storage = part.getMainNode().getGrid().getStorageService().getInventory();
                    helper.assertTrue(storage.insert(
                                    packageKey,
                                    1,
                                    Actionable.SIMULATE,
                                    IActionSource.ofMachine(part)) == 0,
                            "Pattern Provider blocking should reject a package whose input type exists in target");
                    helper.assertTrue(part.heldPackage().isEmpty(),
                            "A blocked formation-style input must not become local storage");
                    chest.setItem(0, ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> {
                    MEStorage storage = part.getMainNode().getGrid().getStorageService().getInventory();
                    helper.assertTrue(storage.insert(
                                    packageKey,
                                    1,
                                    Actionable.SIMULATE,
                                    IActionSource.ofMachine(part)) == 1,
                            "The formation-style input should accept a package after blocking clears");
                })
                .thenExecute(() -> {
                    MEStorage storage = part.getMainNode().getGrid().getStorageService().getInventory();
                    helper.assertTrue(storage.insert(
                                    packageKey,
                                    1,
                                    Actionable.MODULATE,
                                    IActionSource.ofMachine(part)) == 1,
                            "The network should route one package directly into unpacking work");
                    helper.assertTrue(part.isWorking() && ItemStack.isSameItemSameTags(part.heldPackage(), packageStack),
                            "A committed insertion should immediately become the exclusive held work package");
                    helper.assertTrue(storage.extract(
                                    packageKey,
                                    1,
                                    Actionable.SIMULATE,
                                    IActionSource.ofMachine(part)) == 0,
                            "Package unpacking bus must not expose held work as network storage");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    @SuppressWarnings("unchecked")
    public static void packageUnpackingBusWinsEqualPriorityBeforeStorageBus(GameTestHelper helper) {
        BlockPos storageChestPos = new BlockPos(1, 1, 0);
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos unpackingChestPos = new BlockPos(2, 1, 1);
        BlockPos energyCellPos = new BlockPos(1, 1, 2);
        helper.getLevel().setBlock(helper.absolutePos(storageChestPos), Blocks.CHEST.defaultBlockState(), 3);
        helper.getLevel().setBlock(helper.absolutePos(unpackingChestPos), Blocks.CHEST.defaultBlockState(), 3);
        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);
        PartHelper.setPart(
                (ServerLevel) helper.getLevel(),
                helper.absolutePos(cablePos),
                null,
                null,
                AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));

        IPartItem<PackageStorageBusPart> storageItem =
                (IPartItem<PackageStorageBusPart>) (IPartItem<?>) APItems.PACKAGE_STORAGE_BUS.get();
        PackageStorageBusPart storagePart = PartHelper.setPart(
                helper.getLevel(),
                helper.absolutePos(cablePos),
                Direction.NORTH,
                null,
                storageItem);
        IPartItem<PackageUnpackingBusPart> unpackingItem =
                (IPartItem<PackageUnpackingBusPart>) (IPartItem<?>) APItems.PACKAGE_UNPACKING_BUS.get();
        PackageUnpackingBusPart unpackingPart = PartHelper.setPart(
                helper.getLevel(),
                helper.absolutePos(cablePos),
                Direction.EAST,
                null,
                unpackingItem);
        helper.assertTrue(unpackingPart != null && storagePart != null,
                "Both package buses should place on the shared powered cable");
        helper.assertTrue(storagePart.getPriority() == 0 && unpackingPart.getPriority() == 0,
                "Both package buses should default to the same player-controlled priority zero");

        ChestBlockEntity storageChest = (ChestBlockEntity) helper.getBlockEntity(storageChestPos);
        ItemStack packageStack = packageStack(PackageColor.BLUE, ironPackageData(PackageColor.BLUE, 32));
        AEItemKey packageKey = AEItemKey.of(packageStack);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        storagePart.getMainNode().isOnline()
                                && unpackingPart.getMainNode().isOnline()
                                && storagePart.getMainNode().getGrid() == unpackingPart.getMainNode().getGrid(),
                        "Equal-priority package buses should join the same powered grid"))
                .thenExecute(() -> {
                    MEStorage network = storagePart.getMainNode().getGrid().getStorageService().getInventory();
                    helper.assertTrue(network.insert(
                                    packageKey,
                                    1,
                                    Actionable.MODULATE,
                                    IActionSource.ofMachine(storagePart)) == 1,
                            "The shared grid should accept the package");
                    helper.assertTrue(ItemStack.isSameItemSameTags(unpackingPart.heldPackage(), packageStack),
                            "Package Unpacking Bus must receive a package before an equal-priority Storage Bus");
                    helper.assertTrue(storageChest.isEmpty(),
                            "Equal-priority routing must not store the package while Unpacking Bus can accept it");

                    helper.assertTrue(network.insert(
                                    packageKey,
                                    1,
                                    Actionable.MODULATE,
                                    IActionSource.ofMachine(storagePart)) == 1,
                            "Routing should fall through while the preferred Package Unpacking Bus is busy");
                    helper.assertTrue(ItemStack.isSameItemSameTags(storageChest.getItem(0), packageStack),
                            "An equal-priority Storage Bus should receive the package after unpacking rejects it");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    @SuppressWarnings("unchecked")
    public static void packageBusPlayerPriorityOverridesEqualPriorityTie(GameTestHelper helper) {
        BlockPos storageChestPos = new BlockPos(1, 1, 0);
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos unpackingChestPos = new BlockPos(2, 1, 1);
        BlockPos energyCellPos = new BlockPos(1, 1, 2);
        helper.getLevel().setBlock(helper.absolutePos(storageChestPos), Blocks.CHEST.defaultBlockState(), 3);
        helper.getLevel().setBlock(helper.absolutePos(unpackingChestPos), Blocks.CHEST.defaultBlockState(), 3);
        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);
        PartHelper.setPart(
                (ServerLevel) helper.getLevel(),
                helper.absolutePos(cablePos),
                null,
                null,
                AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));

        IPartItem<PackageStorageBusPart> storageItem =
                (IPartItem<PackageStorageBusPart>) (IPartItem<?>) APItems.PACKAGE_STORAGE_BUS.get();
        PackageStorageBusPart storagePart = PartHelper.setPart(
                helper.getLevel(),
                helper.absolutePos(cablePos),
                Direction.NORTH,
                null,
                storageItem);
        IPartItem<PackageUnpackingBusPart> unpackingItem =
                (IPartItem<PackageUnpackingBusPart>) (IPartItem<?>) APItems.PACKAGE_UNPACKING_BUS.get();
        PackageUnpackingBusPart unpackingPart = PartHelper.setPart(
                helper.getLevel(),
                helper.absolutePos(cablePos),
                Direction.EAST,
                null,
                unpackingItem);
        helper.assertTrue(unpackingPart != null && storagePart != null,
                "Both package buses should place on the shared powered cable");
        storagePart.setPriority(1);
        helper.assertTrue(storagePart.getPriority() == 1 && unpackingPart.getPriority() == 0,
                "The player-configured numeric priorities must remain unchanged");

        ChestBlockEntity storageChest = (ChestBlockEntity) helper.getBlockEntity(storageChestPos);
        ItemStack packageStack = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 16));
        AEItemKey packageKey = AEItemKey.of(packageStack);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        storagePart.getMainNode().isOnline()
                                && unpackingPart.getMainNode().isOnline()
                                && storagePart.getMainNode().getGrid() == unpackingPart.getMainNode().getGrid(),
                        "Differently prioritized package buses should join the same powered grid"))
                .thenExecute(() -> {
                    MEStorage network = storagePart.getMainNode().getGrid().getStorageService().getInventory();
                    helper.assertTrue(network.insert(
                                    packageKey,
                                    1,
                                    Actionable.MODULATE,
                                    IActionSource.ofMachine(storagePart)) == 1,
                            "The shared grid should accept the package");
                    helper.assertTrue(ItemStack.isSameItemSameTags(storageChest.getItem(0), packageStack),
                            "A player-configured priority 1 Storage Bus must beat priority 0 Unpacking Bus");
                    helper.assertTrue(unpackingPart.heldPackage().isEmpty(),
                            "Equal-priority tie-breaking must not override a higher player priority");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("unchecked")
    public static void packageBusPartsUseFiveSharedUpgradeSlots(GameTestHelper helper) {
        BlockPos partPos = new BlockPos(1, 1, 1);
        PartHelper.setPart(
                (ServerLevel) helper.getLevel(),
                helper.absolutePos(partPos),
                null,
                null,
                AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));

        IPartItem<PackageStorageBusPart> storageItem =
                (IPartItem<PackageStorageBusPart>) (IPartItem<?>) APItems.PACKAGE_STORAGE_BUS.get();
        PackageStorageBusPart storagePart = PartHelper.setPart(
                helper.getLevel(),
                helper.absolutePos(partPos),
                Direction.NORTH,
                null,
                storageItem);
        IPartItem<PackageUnpackingBusPart> unpackingItem =
                (IPartItem<PackageUnpackingBusPart>) (IPartItem<?>) APItems.PACKAGE_UNPACKING_BUS.get();
        PackageUnpackingBusPart unpackingPart = PartHelper.setPart(
                helper.getLevel(),
                helper.absolutePos(partPos),
                Direction.SOUTH,
                null,
                unpackingItem);

        helper.assertTrue(storagePart != null && unpackingPart != null,
                "Both package buses should install as AE2 cable parts");
        helper.assertTrue(storagePart.getUpgrades().size() == AbstractPackageBusPart.UPGRADE_SLOT_COUNT,
                "Package storage bus should expose exactly five shared upgrade slots");
        helper.assertTrue(unpackingPart.getUpgrades().size() == AbstractPackageBusPart.UPGRADE_SLOT_COUNT,
                "Package unpacking bus should expose exactly five shared upgrade slots");
        helper.assertTrue(storagePart.getPriority() == 0,
                "Package storage bus should keep AE2's default storage priority");
        helper.assertTrue(unpackingPart.getPriority() == PackageUnpackingBusPart.DEFAULT_PRIORITY
                        && unpackingPart.getPriority() == storagePart.getPriority(),
                "Both package buses should default to player-controlled priority zero");
        for (AbstractPackageBusPart part : List.of(storagePart, unpackingPart)) {
            helper.assertTrue(
                    part.getUpgrades().getMaxInstalled(AEItems.CAPACITY_CARD)
                            == AbstractPackageBusPart.MAX_CAPACITY_CARDS,
                    "Each package bus should accept up to five capacity cards");
            for (int card = 0; card < AbstractPackageBusPart.MAX_CAPACITY_CARDS; card++) {
                helper.assertTrue(part.getUpgrades().addItems(AEItems.CAPACITY_CARD.stack()).isEmpty(),
                        "The first five capacity cards should fit in the shared upgrade inventory");
                helper.assertTrue(
                        part.enabledRows() == AbstractPackageBusPart.BASE_FILTER_ROWS + card + 1,
                        "Each installed capacity card should unlock one additional filter row");
            }
            helper.assertTrue(
                    part.getUpgrades().getInstalledUpgrades(AEItems.CAPACITY_CARD)
                            == AbstractPackageBusPart.MAX_CAPACITY_CARDS,
                    "The package bus should report all five installed capacity cards");
            helper.assertTrue(part.enabledRows() == AbstractPackageBusPart.FILTER_ROWS,
                    "Five capacity cards should unlock all seven filter rows");
            helper.assertFalse(part.getUpgrades().addItems(AEItems.CAPACITY_CARD.stack()).isEmpty(),
                    "A sixth capacity card should be rejected");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void packageStorageBusPartMountsOnlyLegalPackages(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 1, 0);
        BlockPos partPos = new BlockPos(1, 1, 1);
        helper.getLevel().setBlock(helper.absolutePos(chestPos), Blocks.CHEST.defaultBlockState(), 3);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
        ItemStack packageStack = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64));
        chest.setItem(0, packageStack.copy());
        chest.setItem(1, new ItemStack(Items.DIRT, 16));
        PackageStorageBusPart part = placePoweredStorageBusPart(helper, partPos, Direction.NORTH);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(part.getMainNode().isOnline() && part.getMainNode().hasGridBooted(),
                            "Package storage bus part should join its powered AE grid");
                    helper.assertTrue(part.getMainNode().getGrid() != null,
                            "Package storage bus part should expose its connected grid");
                })
                .thenExecuteAfter(12, () -> {
                    MEStorage storage = part.getMainNode().getGrid().getStorageService().getInventory();
                    helper.assertTrue(storage.extract(
                                    AEItemKey.of(packageStack),
                                    1,
                                    Actionable.SIMULATE,
                                    IActionSource.ofMachine(part)) == 1,
                            "Package storage bus part should mount the adjacent legal package");
                    helper.assertTrue(storage.extract(
                                    AEItemKey.of(Items.DIRT),
                                    16,
                                    Actionable.SIMULATE,
                                    IActionSource.ofMachine(part)) == 0,
                            "Package storage bus part must not expose adjacent loose items");
                    helper.assertTrue(storage.extract(
                                    AEItemKey.of(packageStack),
                                    1,
                                    Actionable.MODULATE,
                                    IActionSource.ofMachine(part)) == 1,
                            "Mounted package storage should allow extracting the legal package");
                    helper.assertTrue(chest.getItem(0).isEmpty(),
                            "Extracting through the package storage part should update the adjacent inventory");
                    helper.assertTrue(chest.getItem(1).is(Items.DIRT) && chest.getItem(1).getCount() == 16,
                            "Extracting a package must leave adjacent loose items unchanged");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void packageStorageBusPartitionReadsContainerPackages(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 1, 0);
        BlockPos partPos = new BlockPos(1, 1, 1);
        helper.getLevel().setBlock(helper.absolutePos(chestPos), Blocks.CHEST.defaultBlockState(), 3);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
        ItemStack redIron = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64));
        ItemStack blueGold = packageStack(
                PackageColor.BLUE,
                PackageData.create(
                        PackageColor.BLUE,
                        List.of(
                                new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 8),
                                new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 8),
                                new GenericStack(AEItemKey.of(Items.DIAMOND), 8),
                                new GenericStack(AEItemKey.of(Items.EMERALD), 8),
                                new GenericStack(AEItemKey.of(Items.COAL), 8),
                                new GenericStack(AEItemKey.of(Items.QUARTZ), 8),
                                new GenericStack(AEItemKey.of(Items.LAPIS_LAZULI), 8)),
                        Optional.empty(),
                        0));
        chest.setItem(0, redIron.copy());
        chest.setItem(1, new ItemStack(Items.DIRT, 16));
        chest.setItem(2, blueGold.copy());
        PackageStorageBusPart part = placePoweredStorageBusPart(helper, partPos, Direction.NORTH);

        part.partitionFromTarget();

        helper.assertTrue(part.isRowColorEnabled(0) && part.rowColor(0) == PackageColor.RED,
                "Partition Storage should derive the first row color from the first package");
        helper.assertTrue(part.isRowColorEnabled(1) && part.rowColor(1) == PackageColor.BLUE,
                "Partition Storage should skip loose items and derive the next package row");
        helper.assertTrue(part.filterSet().matches(redIron) && part.filterSet().matches(blueGold),
                "Partition-generated rows should match samples even when content exceeds six visible slots");

        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, ItemStack.EMPTY);
        }
        part.partitionFromTarget();
        helper.assertTrue(part.filterSet().isAny(),
                "Partition Storage should clear filters when the attached inventory has no packages");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 250)
    public static void packageUnpackingBusPartKeepsAndRetriesSamePackageAfterFinalBlock(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 1, 0);
        BlockPos partPos = new BlockPos(1, 1, 1);
        helper.getLevel().setBlock(helper.absolutePos(chestPos), Blocks.CHEST.defaultBlockState(), 3);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
        PackageUnpackingBusPart part = placePoweredUnpackingBusPart(helper, partPos, Direction.NORTH);
        PackageData data = PackageData.create(
                PackageColor.RED,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 32)),
                Optional.empty(),
                0);
        ItemStack packageStack = packageStack(PackageColor.RED, data);
        AEItemKey packageKey = AEItemKey.of(packageStack);

        helper.startSequence()
                .thenWaitUntil(() -> assertPackageUnpackingPartStorageReady(helper, part, packageStack))
                .thenExecute(() -> {
                    MEStorage storage = part.getMainNode().getGrid().getStorageService().getInventory();
                    helper.assertTrue(storage.insert(
                                    packageKey,
                                    1,
                                    Actionable.MODULATE,
                                    IActionSource.ofMachine(part)) == 1,
                            "Unpacking part grid should accept the package used by the blocked retry test");
                })
                .thenWaitUntil(() -> helper.assertTrue(part.isWorking() && !part.heldPackage().isEmpty(),
                        "Unpacking part should begin progress before the destination is changed"))
                .thenExecute(() -> {
                    for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                        chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                    }
                })
                .thenExecuteAfter(PackageUnpackingBusPart.ANIMATION_CYCLE_TICKS + 2, () -> {
                    MEStorage storage = part.getMainNode().getGrid().getStorageService().getInventory();
                    helper.assertTrue(part.unpackBlocked(),
                            "A failed final commit should put the unpacking part into blocked state");
                    helper.assertTrue(ItemStack.isSameItemSameTags(part.heldPackage(), packageStack),
                            "Blocked work should retain the exact package that was originally reserved");
                    helper.assertTrue(storage.extract(
                                    packageKey,
                                    1,
                                    Actionable.SIMULATE,
                                    IActionSource.ofMachine(part)) == 0,
                            "A blocked held package must not also reappear in ME storage");
                    helper.assertTrue(itemAmountInContainer(chest, Items.IRON_INGOT) == 0
                                    && itemAmountInContainer(chest, Items.COPPER_INGOT) == 0,
                            "A blocked final commit must not insert partial contents");
                    for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                        chest.setItem(slot, ItemStack.EMPTY);
                    }
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(part.heldPackage().isEmpty(),
                            "The blocked unpacking part should retry and eventually clear the same held package");
                    helper.assertTrue(itemAmountInContainer(chest, Items.IRON_INGOT) == 64,
                            "Retry should insert all iron from the retained package");
                    helper.assertTrue(itemAmountInContainer(chest, Items.COPPER_INGOT) == 32,
                            "Retry should insert all copper from the retained package");
                })
                .thenExecute(() -> helper.assertFalse(part.unpackBlocked(),
                        "A successful retry should clear the blocked state"))
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void packageUnpackingBusPartPersistsAndDropsHeldPackage(GameTestHelper helper) {
        ItemStack packageStack = packageStack(PackageColor.PURPLE, ironPackageData(PackageColor.PURPLE, 64));
        PackageUnpackingBusPart part = placePoweredUnpackingBusPart(
                helper,
                new BlockPos(1, 1, 1),
                Direction.NORTH);
        part.getHeldPackageItems().setStackInSlot(0, packageStack.copy());
        CompoundTag saved = new CompoundTag();
        part.writeToNBT(saved);

        part.clearContent();
        part.readFromNBT(saved);
        List<ItemStack> drops = new ArrayList<>();
        part.addAdditionalDrops(drops, false);

        helper.assertTrue(ItemStack.isSameItemSameTags(part.heldPackage(), packageStack),
                "Package unpacking part should persist its locally held package in part NBT");
        helper.assertTrue(drops.stream().anyMatch(drop -> ItemStack.isSameItemSameTags(drop, packageStack)),
                "Removing a package unpacking part should return its locally held package as an additional drop");
        part.clearContent();
        helper.assertTrue(part.heldPackage().isEmpty(),
                "Clearing part contents after drops should clear the held package and prevent duplication");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void packageBusFilterRowsUseOrAndPerRowInversion(GameTestHelper helper) {
        var redIron = new com.warmthdawn.appliedpackaging.core.package_data.PackageBusFilterSet.Rule(
                Optional.of(PackageColor.RED),
                null,
                List.of(AEItemKey.of(Items.IRON_INGOT)),
                false,
                false);
        var blueWithoutGold = new com.warmthdawn.appliedpackaging.core.package_data.PackageBusFilterSet.Rule(
                Optional.of(PackageColor.BLUE),
                null,
                List.of(AEItemKey.of(Items.GOLD_INGOT)),
                false,
                true);
        var filters = new com.warmthdawn.appliedpackaging.core.package_data.PackageBusFilterSet(
                List.of(redIron, blueWithoutGold),
                appeng.api.config.FuzzyMode.IGNORE_ALL);

        ItemStack redPackage = packageStack(PackageColor.RED, ironPackageData(PackageColor.RED, 64));
        ItemStack blueIron = packageStack(PackageColor.BLUE, ironPackageData(PackageColor.BLUE, 64));
        ItemStack blueGold = packageStack(
                PackageColor.BLUE,
                PackageData.create(
                        PackageColor.BLUE,
                        List.of(new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1)),
                        Optional.empty(),
                        0));

        helper.assertTrue(filters.matches(redPackage), "First filter row should match a red iron package");
        helper.assertTrue(filters.matches(blueIron), "Second inverted row should allow blue packages without gold");
        helper.assertFalse(filters.matches(blueGold), "Second inverted row should reject listed gold contents");

        var anyColorIron = new com.warmthdawn.appliedpackaging.core.package_data.PackageBusFilterSet.Rule(
                Optional.empty(),
                null,
                List.of(AEItemKey.of(Items.IRON_INGOT)),
                false,
                false);
        var colorlessFilter = new com.warmthdawn.appliedpackaging.core.package_data.PackageBusFilterSet(
                List.of(anyColorIron),
                appeng.api.config.FuzzyMode.IGNORE_ALL);
        helper.assertTrue(colorlessFilter.matches(redPackage) && colorlessFilter.matches(blueIron),
                "An empty color mode should not filter package colors");
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
    public static void ordinaryAe2ProcessingPatternHasNoAdvancedPackageMetadata(GameTestHelper helper) {
        ItemStack pattern = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32) },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        var metadata = new AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern(List.of(
                new AdvancedProcessingPatternDataStorage.PackageColumn(
                        0, PackageColor.FLUIX, Optional.empty())));
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
        GenericStack[] advancedInputs =
                new GenericStack[AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE + 1];
        advancedInputs[0] = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32);
        advancedInputs[AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE] =
                new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 16);
        ItemStack pattern = APItems.ADVANCED_PROCESSING_PATTERN.get().encode(
                advancedInputs,
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        MarkerSpec diamond = new MarkerSpec(new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
        MarkerSpec emerald = new MarkerSpec(new GenericStack(AEItemKey.of(Items.EMERALD), 1));
        AdvancedProcessingPatternDataStorage.write(
                pattern,
                new AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern(List.of(
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                0, PackageColor.RED, Optional.of(diamond)),
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                1, PackageColor.BLUE, Optional.of(emerald)))));

        var encoded = AdvancedProcessingPatternDataStorage.read(pattern).orElseThrow();
        helper.assertTrue(pattern.is(APItems.ADVANCED_PROCESSING_PATTERN.get()),
                "Advanced pattern metadata should be stored on the dedicated pattern item");
        helper.assertTrue(encoded.activeColumnCount() == 2,
                "Advanced processing pattern should preserve its active column count");
        helper.assertTrue(encoded.column(0).color() == PackageColor.RED,
                "First advanced package color should round-trip");
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
                "Ordinary processing patterns should retain the package item's default name");
        helper.assertTrue(data.marker().isEmpty(),
                "Ordinary processing patterns should leave the package marker empty");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packageAssemblerAdvancedPatternPackagesEachColumnInOrder(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = newPackageAssembler();
        GenericStack[] advancedInputs =
                new GenericStack[AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE + 1];
        advancedInputs[0] = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32);
        advancedInputs[AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE] =
                new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 16);
        ItemStack pattern = APItems.ADVANCED_PROCESSING_PATTERN.get().encode(
                advancedInputs,
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        MarkerSpec diamond = new MarkerSpec(new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
        MarkerSpec emerald = new MarkerSpec(new GenericStack(AEItemKey.of(Items.EMERALD), 1));
        AdvancedProcessingPatternDataStorage.write(
                pattern,
                new AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern(List.of(
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                0, PackageColor.RED, Optional.of(diamond)),
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                1, PackageColor.RED, Optional.of(emerald)))));
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        KeyCounter iron = new KeyCounter();
        iron.add(AEItemKey.of(Items.IRON_INGOT), 32);
        KeyCounter copper = new KeyCounter();
        copper.add(AEItemKey.of(Items.COPPER_INGOT), 16);

        boolean accepted = assembler.pushPattern(details, new KeyCounter[] { iron, copper }, Direction.UP);
        ItemStack firstOutput = assembler.getItems()
                .getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT)
                .copy();
        ItemStack secondOutput = assembler.nextOutputPreview();
        PackageData firstData = PackageDataStorage.read(firstOutput).orElseThrow();
        PackageData secondData = PackageDataStorage.read(secondOutput).orElseThrow();

        helper.assertTrue(accepted, "Assembler should accept an advanced processing pattern");
        helper.assertTrue(iron.isEmpty() && copper.isEmpty(),
                "Accepted advanced pattern should consume every exact input");
        helper.assertTrue(firstOutput.is(APItems.packageItems().get(PackageColor.RED).get())
                        && secondOutput.is(APItems.packageItems().get(PackageColor.RED).get()),
                "Advanced columns with the same color should remain separate packages");
        helper.assertFalse(firstOutput.hasCustomHoverName(),
                "Advanced package output should retain its default item name");
        helper.assertFalse(secondOutput.hasCustomHoverName(),
                "Second package should retain its default item name and queue order");
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
    public static void packageAssemblerAdvancedMenuCompactsSparseInputsAndExposesAllFilters(GameTestHelper helper) {
        PackageAssemblerBlockEntity assembler = placePackageAssembler(
                helper,
                new BlockPos(0, 0, 0),
                Direction.NORTH);
        MarkerSpec diamondMarker = new MarkerSpec(new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
        MarkerSpec emeraldMarker = new MarkerSpec(new GenericStack(AEItemKey.of(Items.EMERALD), 1));
        int inputsPerPackage = AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
        GenericStack[] sparseInputs = new GenericStack[inputsPerPackage * 2];
        sparseInputs[0] = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32);
        sparseInputs[inputsPerPackage - 1] = new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 8);
        sparseInputs[inputsPerPackage + 40] = new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 16);
        sparseInputs[inputsPerPackage * 2 - 1] = new GenericStack(AEItemKey.of(Items.DIAMOND), 4);
        ItemStack compactPattern = APItems.ADVANCED_PROCESSING_PATTERN.get().encode(
                sparseInputs,
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.QUARTZ), 1) });
        AdvancedProcessingPatternDataStorage.write(
                compactPattern,
                new AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern(List.of(
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                0, PackageColor.RED, Optional.of(diamondMarker)),
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                1, PackageColor.BLUE, Optional.of(emeraldMarker)))));
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, compactPattern);
        assembler.getItems().setStackInSlot(
                PackageAssemblerBlockEntity.SLOT_CAPACITY,
                ae2Item("cell_component_256k"));

        helper.assertTrue(assembler.menuInputFilterDisplay(0).is(Items.IRON_INGOT)
                        && assembler.menuInputFilterDisplay(1).is(Items.GOLD_INGOT)
                        && assembler.menuInputFilterDisplay(2).is(Items.COPPER_INGOT)
                        && assembler.menuInputFilterDisplay(3).is(Items.DIAMOND)
                        && assembler.menuInputFilterDisplay(4).isEmpty(),
                "Advanced local input filters should skip sparse blanks while preserving column and row order");
        helper.assertTrue(assembler.isMenuInputSlotEnabled(3) && !assembler.isMenuInputSlotEnabled(4),
                "Only dense non-empty advanced inputs should unlock local slots");
        PackageAssemblerMenu menu = new PackageAssemblerMenu(32, new Inventory(newFakePlayer(helper)), assembler);
        IItemHandler external = assembler.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                .orElseThrow(IllegalStateException::new);
        menu.broadcastChanges();
        helper.assertTrue(assembler.menuInputSlotCount() == 4
                        && menu.menuInputSlotCount() == 4
                        && menu.maxScrollOffset() == 0
                        && external.getSlots() == 5
                        && assembler.externalOutputSlot() == 4,
                "Sparse advanced patterns should expose only their four dense inputs and should not scroll");

        GenericStack[] seventyInputs = new GenericStack[70];
        for (int slot = 0; slot < seventyInputs.length; slot++) {
            seventyInputs[slot] = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1);
        }
        ItemStack largePattern = APItems.ADVANCED_PROCESSING_PATTERN.get().encode(
                seventyInputs,
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.QUARTZ), 1) });
        AdvancedProcessingPatternDataStorage.write(
                largePattern,
                new AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern(List.of(
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                0, PackageColor.FLUIX, Optional.empty()))));
        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, largePattern);
        menu.broadcastChanges();
        menu.setScrollOffset(menu.maxScrollOffset());

        helper.assertTrue(assembler.menuInputSlotCount() == 70
                        && menu.menuInputSlotCount() == 70
                        && external.getSlots() == 71
                        && assembler.externalOutputSlot() == 70,
                "Assembler input storage and external capability should resize to the installed pattern");
        helper.assertTrue(!assembler.menuInputFilterDisplay(69).isEmpty()
                        && assembler.menuInputFilterDisplay(70).isEmpty()
                        && external.isItemValid(69, new ItemStack(Items.IRON_INGOT)),
                "Inputs beyond the former 68-slot limit should remain visible and externally addressable");
        helper.assertTrue(menu.maxScrollOffset() == 14
                        && menu.inputSlotForVisibleIndex(13) == 69
                        && menu.getSlot(menu.menuInputMenuSlotIndex(13)).getItem().is(Items.IRON_INGOT),
                "Dynamic scrolling should reach the final dense input without adding sparse blank rows");

        assembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, compactPattern);
        menu.broadcastChanges();
        helper.assertTrue(menu.menuInputSlotCount() == 4
                        && menu.maxScrollOffset() == 0
                        && menu.scrollOffset() == 0
                        && external.getSlots() == 5,
                "Replacing the pattern with a shorter input list should remove disabled tail rows and scrolling");

        PackageAssemblerBlockEntity localAssembler = newPackageAssembler();
        localAssembler.getItems().setStackInSlot(PackageAssemblerBlockEntity.SLOT_PATTERN, compactPattern);
        localAssembler.getItems().setStackInSlot(
                PackageAssemblerBlockEntity.SLOT_CAPACITY,
                ae2Item("cell_component_16k"));
        helper.assertTrue(localAssembler.insertMenuInput(0, new ItemStack(Items.IRON_INGOT, 32), 32, false) == 32
                        && localAssembler.insertMenuInput(1, new ItemStack(Items.GOLD_INGOT, 8), 8, false) == 8
                        && localAssembler.insertMenuInput(2, new ItemStack(Items.COPPER_INGOT, 16), 16, false) == 16
                        && localAssembler.insertMenuInput(3, new ItemStack(Items.DIAMOND, 4), 4, false) == 4,
                "Dense advanced filters should accept every non-empty sparse input in display order");
        PackageAssemblerBlockEntity.AssemblyResult result = localAssembler.tryAssemble();
        ItemStack first = localAssembler.getItems().getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT);
        ItemStack second = localAssembler.nextOutputPreview();
        PackageData firstData = PackageDataStorage.read(first).orElseThrow();
        PackageData secondData = PackageDataStorage.read(second).orElseThrow();

        helper.assertTrue(result == PackageAssemblerBlockEntity.AssemblyResult.ASSEMBLED
                        && first.is(APItems.packageItems().get(PackageColor.RED).get())
                        && second.is(APItems.packageItems().get(PackageColor.BLUE).get()),
                "Local advanced assembly should keep pattern-owned colors locked to their columns");
        helper.assertTrue(firstData.marker().orElseThrow().sameAs(diamondMarker)
                        && secondData.marker().orElseThrow().sameAs(emeraldMarker),
                "Local advanced assembly should keep pattern-owned markers locked to their columns");
        helper.assertTrue(firstData.contents().size() == 2
                        && firstData.contents().get(0).what().equals(AEItemKey.of(Items.IRON_INGOT))
                        && firstData.contents().get(1).what().equals(AEItemKey.of(Items.GOLD_INGOT))
                        && secondData.contents().size() == 2
                        && secondData.contents().get(0).what().equals(AEItemKey.of(Items.COPPER_INGOT))
                        && secondData.contents().get(1).what().equals(AEItemKey.of(Items.DIAMOND)),
                "Dense local inputs should rebuild packages by original advanced column and sparse row order");
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
        part.getAdvancedPatternState().inputs().setStack(
                AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE,
                new GenericStack(AEItemKey.of(Items.DIAMOND), 7));
        CompoundTag saved = new CompoundTag();
        part.writeToNBT(saved);
        part.getAdvancedPatternState().reset();
        part.readFromNBT(saved);

        helper.assertTrue(part.getAdvancedPatternState().activeColumns() == 2,
                "Advanced terminal should persist active columns");
        helper.assertTrue(part.getAdvancedPatternState().color(0) == PackageColor.GREEN,
                "Advanced terminal should persist column colors");
        helper.assertTrue(part.getAdvancedPatternState().inputs()
                        .getStack(AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE)
                        .what()
                        .equals(AEItemKey.of(Items.DIAMOND)),
                "Advanced terminal should persist its expanded package-column inputs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void patternEncodingTerminalPackageInputsSupportAmountEditing(GameTestHelper helper) {
        @SuppressWarnings("unchecked")
        IPartItem<PatternEncodingTerminalPart> partItem =
                (IPartItem<PatternEncodingTerminalPart>) AEParts.PATTERN_ENCODING_TERMINAL.asItem();
        PatternEncodingTerminalPart part = PartHelper.setPart(
                helper.getLevel(),
                helper.absolutePos(new BlockPos(2, 2, 1)),
                Direction.NORTH,
                null,
                partItem);
        helper.assertTrue(part != null, "Pattern encoding terminal should place for package-mode amount testing");

        PackageCraftingPatternLogicBridge logic = (PackageCraftingPatternLogicBridge) part.getLogic();
        logic.appliedpackaging$setPackageCraftingMode(true);
        part.getLogic().getEncodedInputInv().setStack(
                0,
                new GenericStack(AEItemKey.of(Items.OAK_LOG), 4));
        part.getLogic().getEncodedInputInv().setStack(
                PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT - 1,
                new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 7));
        part.getLogic().getEncodedInputInv().setStack(
                1,
                new GenericStack(AEFluidKey.of(Fluids.WATER), 1000));
        part.getLogic().getBlankPatternInv().setItemDirect(0, AEItems.BLANK_PATTERN.stack());

        FakePlayer player = newFakePlayer(helper);
        PatternEncodingTermMenu menu = new PatternEncodingTermMenu(8, new Inventory(player), part);
        PackageCraftingPatternMenuBridge packageMenu = (PackageCraftingPatternMenuBridge) menu;
        packageMenu.appliedpackaging$setPackageCraftingMode(true);
        var input = menu.getProcessingInputSlots()[0];
        var lastInput = menu.getProcessingInputSlots()[PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT - 1];
        var markerSlot = packageMenu.appliedpackaging$getPackageCraftingMarkerSlot();
        var automaticOutput = menu.getSlots(SlotSemantics.CRAFTING_RESULT).get(0);

        helper.assertTrue(markerSlot != null && markerSlot.isActive(),
                "Package mode should expose its dedicated marker slot");
        helper.assertTrue(automaticOutput.isActive(),
                "Package mode should expose exactly one automatic package output");
        helper.assertTrue(PackageDataStorage.read(automaticOutput.getItem()).isPresent(),
                "The package output should be calculated from configured inputs without an output filter");
        helper.assertTrue(java.util.Arrays.stream(menu.getProcessingOutputSlots()).noneMatch(slot -> slot.isActive()),
                "Package mode must not expose any configurable processing output slots");
        helper.assertTrue(java.util.Arrays.stream(menu.getProcessingInputSlots()).allMatch(slot -> slot.isActive()),
                "All 81 package inputs should remain available to the scrolling input window");
        helper.assertFalse(input.isHideAmount(),
                "Package-mode processing inputs should render configured amounts");
        helper.assertTrue(menu.isProcessingPatternSlot(input),
                "Package-mode inputs should use AE2 processing-slot container behavior");
        helper.assertTrue(menu.canModifyAmountForSlot(input),
                "Package-mode processing inputs should open AE2 amount editing on middle click");
        GenericStack configured = GenericStack.fromItemStack(input.getItem());
        helper.assertTrue(configured != null && configured.amount() == 4,
                "Package-mode processing inputs should preserve amounts larger than one");
        helper.assertTrue(menu.canModifyAmountForSlot(lastInput),
                "The 81st package input should support amount editing");

        menu.encode();
        ItemStack encodedStack = part.getLogic().getEncodedPatternInv().getStackInSlot(0);
        helper.assertTrue(encodedStack.is(APItems.PACKAGE_PATTERN.get()),
                "Ordinary pattern terminal package mode should encode the dedicated package pattern item");
        helper.assertFalse(AEItems.CRAFTING_PATTERN.isSameAs(encodedStack),
                "New package patterns should no longer masquerade as AE2 crafting patterns");
        helper.assertTrue(PatternDetailsHelper.decodePattern(encodedStack, helper.getLevel())
                        instanceof com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDetails,
                "Dedicated package patterns should decode through AE2 pattern details");
        var encoded = PackageCraftingPatternDataStorage.read(encodedStack).orElseThrow();
        GenericStack encodedLast = encoded.sparseInputs()[PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT - 1];
        GenericStack encodedFluid = encoded.sparseInputs()[1];
        helper.assertTrue(encodedLast != null
                        && encodedLast.what().equals(AEItemKey.of(Items.GOLD_INGOT))
                        && encodedLast.amount() == 7,
                "Package-mode encoding should preserve the 81st scrolled input");
        helper.assertTrue(encodedFluid != null
                        && encodedFluid.what().equals(AEFluidKey.of(Fluids.WATER))
                        && encodedFluid.amount() == 1000,
                "Package-mode encoding should preserve AE2 processing fluid inputs");

        packageMenu.appliedpackaging$setPackageCraftingMode(false);
        helper.assertFalse(menu.isProcessingPatternSlot(input),
                "Normal crafting mode should restore AE2 processing-slot classification");
        helper.assertFalse(menu.canModifyAmountForSlot(input),
                "Normal crafting mode should not treat processing inputs as editable package inputs");
        helper.assertTrue(menu.getCraftingGridSlots()[0].isHideAmount(),
                "Normal crafting mode should keep AE2's crafting-grid amounts hidden");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void advancedPatternTerminalEncodesDedicatedPattern(GameTestHelper helper) {
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
        helper.assertTrue(part != null, "Advanced pattern terminal should place for encoding");

        part.getAdvancedPatternState().setActiveColumns(2);
        part.getAdvancedPatternState().setColor(0, PackageColor.RED);
        part.getAdvancedPatternState().setColor(1, PackageColor.BLUE);
        part.getAdvancedPatternState().inputs().setStack(
                0,
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32));
        part.getAdvancedPatternState().inputs().setStack(
                AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE,
                new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 16));
        part.getAdvancedPatternState().inputs().setStack(
                AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE * 2,
                new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 64));
        part.getLogic().getEncodedOutputInv().setStack(
                0,
                new GenericStack(AEItemKey.of(Items.DIAMOND), 1));
        part.getLogic().getBlankPatternInv().setItemDirect(0, AEItems.BLANK_PATTERN.stack());

        FakePlayer player = newFakePlayer(helper);
        AdvancedPatternEncodingTermMenu menu =
                new AdvancedPatternEncodingTermMenu(7, new Inventory(player), part);
        helper.assertTrue(menu.canModifyAmountForSlot(menu.getAdvancedInputSlots()[0]),
                "Advanced terminal inputs should open amount editing on middle click");
        menu.encode();

        ItemStack encodedStack = part.getLogic().getEncodedPatternInv().getStackInSlot(0);
        Optional<AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern> encodedPattern =
                AdvancedProcessingPatternDataStorage.read(encodedStack);
        helper.assertTrue(encodedPattern.isPresent(),
                "Advanced terminal output should contain advanced package-column metadata");
        var encoded = encodedPattern.get();
        List<GenericStack> sparseInputs = AdvancedProcessingPatternDataStorage.readSparseInputs(encodedStack);

        helper.assertTrue(encodedStack.is(APItems.ADVANCED_PROCESSING_PATTERN.get()),
                "Advanced terminal should encode the dedicated advanced pattern item");
        helper.assertTrue(
                PatternDetailsHelper.decodePattern(encodedStack, helper.getLevel())
                        instanceof com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDetails,
                "Advanced terminal output should decode through its expanded processing-pattern details");
        helper.assertTrue(part.getLogic().getBlankPatternInv().getStackInSlot(0).isEmpty(),
                "Successful advanced encoding should consume one blank pattern");
        helper.assertTrue(encoded.activeColumnCount() == 2,
                "Advanced terminal should encode only active package columns");
        helper.assertTrue(encoded.column(0).color() == PackageColor.RED
                        && encoded.column(1).color() == PackageColor.BLUE,
                "Advanced terminal should preserve each active column color");
        helper.assertTrue(encoded.column(0).marker().orElseThrow().stack().what().equals(AEItemKey.of(Items.DIAMOND))
                        && encoded.column(1).marker().orElseThrow().stack().what().equals(AEItemKey.of(Items.DIAMOND)),
                "Advanced terminal should use the primary output as every package marker");
        helper.assertTrue(sparseInputs.get(0).what().equals(AEItemKey.of(Items.IRON_INGOT))
                        && sparseInputs.get(AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE)
                                .what()
                                .equals(AEItemKey.of(Items.COPPER_INGOT)),
                "Advanced terminal should preserve inputs in full-size AE2 package columns");
        helper.assertTrue(
                sparseInputs.size() <= AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE * 2,
                "Advanced terminal must ignore stale ghost inputs outside active columns");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void advancedPatternTerminalColumnDeleteClearsThenShifts(GameTestHelper helper) {
        AdvancedPatternEncodingState state = new AdvancedPatternEncodingState(() -> {
        });
        state.setActiveColumns(3);
        state.setColor(0, PackageColor.RED);
        state.setColor(1, PackageColor.BLUE);
        state.setColor(2, PackageColor.GREEN);
        int slotsPerColumn = AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
        state.inputs().setStack(
                slotsPerColumn,
                new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 16));
        state.inputs().setStack(
                slotsPerColumn * 2,
                new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 8));

        boolean removedWhilePopulated = state.clearOrDeleteColumn(1);
        helper.assertFalse(removedWhilePopulated,
                "The first X press should clear a populated package column");
        helper.assertTrue(state.activeColumns() == 3 && state.inputs().getStack(slotsPerColumn) == null,
                "Clearing a populated package column should preserve the column itself");

        boolean removedWhileEmpty = state.clearOrDeleteColumn(1);
        helper.assertTrue(removedWhileEmpty,
                "The second X press should remove the now-empty package column");
        helper.assertTrue(state.activeColumns() == 2 && state.color(1) == PackageColor.GREEN,
                "Removing an empty package column should shift following colors left");
        helper.assertTrue(state.inputs().getStack(slotsPerColumn).what().equals(AEItemKey.of(Items.GOLD_INGOT)),
                "Removing an empty package column should shift following inputs left");
        helper.assertTrue(state.inputs().getStack(slotsPerColumn * 2) == null,
                "Removing a package column should clear the old final column");

        state.reset();
        helper.assertFalse(state.clearOrDeleteColumn(0),
                "The final empty package column must not be removed");
        helper.assertTrue(state.activeColumns() == 1,
                "The advanced terminal must always retain one package column");
        helper.succeed();
    }


    private static ItemStack packageCraftingPattern(
            PackageColor color,
            Optional<MarkerSpec> marker,
            GenericStack... inputs) {
        var encoded = PackageCraftingPatternDataStorage.create(
                        color,
                        sparsePackageCraftingInputs(inputs),
                        marker)
                .orElseThrow();
        return PackageCraftingPatternDataStorage.encode(encoded);
    }

    private static ItemStack packageCraftingPattern(PackageColor color, PackageData data) {
        GenericStack[] inputs = data.contents().toArray(GenericStack[]::new);
        return PackageCraftingPatternDataStorage.encode(
                new PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern(
                        color,
                        sparsePackageCraftingInputs(inputs),
                        data));
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

    private static InterfaceBlockEntity placeMePackagerAe2Interface(
            GameTestHelper helper,
            BlockPos packagerPos,
            Direction connectionSide) {
        BlockPos interfacePos = packagerPos.relative(connectionSide);
        BlockPos drivePos = interfacePos.relative(connectionSide);
        BlockPos energyCellPos = drivePos.relative(connectionSide);
        Direction facing = connectionSide.getAxis() == Direction.Axis.Y
                ? Direction.NORTH
                : connectionSide.getOpposite();

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
                        .setValue(AbstractHorizontalMachineBlock.FACING, facing),
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

    private static int itemAmountInContainer(ChestBlockEntity container, Item item) {
        int amount = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) {
                amount += stack.getCount();
            }
        }
        return amount;
    }

    @SuppressWarnings("unchecked")
    private static PackageStorageBusPart placePoweredStorageBusPart(
            GameTestHelper helper,
            BlockPos partPos,
            Direction side) {
        BlockPos energyCellPos = partPos.relative(Direction.SOUTH);
        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);
        PartHelper.setPart(
                (ServerLevel) helper.getLevel(),
                helper.absolutePos(partPos),
                null,
                null,
                AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));

        IPartItem<PackageStorageBusPart> partItem =
                (IPartItem<PackageStorageBusPart>) (IPartItem<?>) APItems.PACKAGE_STORAGE_BUS.get();
        PackageStorageBusPart part = PartHelper.setPart(
                helper.getLevel(),
                helper.absolutePos(partPos),
                side,
                null,
                partItem);
        helper.assertTrue(part != null, "Package storage bus should place as an AE2 cable part");
        return part;
    }

    @SuppressWarnings("unchecked")
    private static PackageUnpackingBusPart placePoweredUnpackingBusPart(
            GameTestHelper helper,
            BlockPos partPos,
            Direction side) {
        BlockPos energyCellPos = partPos.relative(Direction.SOUTH);
        helper.getLevel().setBlock(
                helper.absolutePos(energyCellPos),
                AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState(),
                3);

        PartHelper.setPart(
                (ServerLevel) helper.getLevel(),
                helper.absolutePos(partPos),
                null,
                null,
                AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));

        IPartItem<PackageUnpackingBusPart> partItem =
                (IPartItem<PackageUnpackingBusPart>) (IPartItem<?>) APItems.PACKAGE_UNPACKING_BUS.get();
        PackageUnpackingBusPart part = PartHelper.setPart(
                helper.getLevel(),
                helper.absolutePos(partPos),
                side,
                null,
                partItem);
        helper.assertTrue(part != null, "Package unpacking bus should place as an AE2 cable part");
        return part;
    }

    private static void assertPackageUnpackingPartStorageReady(
            GameTestHelper helper,
            PackageUnpackingBusPart part,
            ItemStack packageStack) {
        helper.assertTrue(part.getMainNode().isOnline(),
                "Package unpacking part should join its powered AE grid");
        helper.assertTrue(part.getMainNode().getGrid() != null,
                "Package unpacking part should expose its connected grid");
        MEStorage storage = part.getMainNode().getGrid().getStorageService().getInventory();
        helper.assertTrue(storage.insert(
                        AEItemKey.of(packageStack),
                        1,
                        Actionable.SIMULATE,
                        IActionSource.ofMachine(part)) == 1,
                "Package unpacking part grid should have room for the test package");
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

    private static final class CapacityMEStorage implements MEStorage {
        private final KeyCounter contents = new KeyCounter();
        private final long capacity;
        private long used;

        private CapacityMEStorage(long capacity) {
            this.capacity = capacity;
        }

        private long amount(AEKey key) {
            return contents.get(key);
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            long inserted = Math.max(0, Math.min(amount, capacity - used));
            if (inserted > 0 && mode == Actionable.MODULATE) {
                contents.add(what, inserted);
                used += inserted;
            }
            return inserted;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            long extracted = Math.min(amount, contents.get(what));
            if (extracted > 0 && mode == Actionable.MODULATE) {
                contents.remove(what, extracted);
                used -= extracted;
            }
            return extracted;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            out.addAll(contents);
        }

        @Override
        public Component getDescription() {
            return Component.literal("capacity");
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

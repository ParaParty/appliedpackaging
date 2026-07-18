package com.warmthdawn.appliedpackaging.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.capabilities.Capabilities;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import com.mojang.authlib.GameProfile;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDetails;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDetails;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageLayout;
import com.warmthdawn.appliedpackaging.core.sequence_buffer.SequenceBufferTopology;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.part.PackageUnpackingBusPart;
import com.warmthdawn.appliedpackaging.world.block.SequenceBufferBlock;
import com.warmthdawn.appliedpackaging.world.block.SequenceBufferVisualState;
import com.warmthdawn.appliedpackaging.world.block.entity.PackageAssemblerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.SequenceBufferBlockEntity;
import com.warmthdawn.appliedpackaging.world.menu.SequenceBufferMainMenu;
import com.warmthdawn.appliedpackaging.world.menu.SequenceBufferSideMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

@GameTestHolder(AppliedPackaging.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SequenceBufferGameTests {
    private SequenceBufferGameTests() {
    }

    @GameTest(template = "sequence_buffer_empty")
    public static void packageLayoutRoundTripsAndChangesIdentity(GameTestHelper helper) {
        List<GenericStack> contents = List.of(
                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 8),
                new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 4));
        PackageData first = PackageData.create(
                PackageColor.FLUIX,
                contents,
                Optional.of(new PackageLayout(4, List.of(0, 2))),
                Optional.empty(),
                0);
        PackageData second = PackageData.create(
                PackageColor.FLUIX,
                contents,
                Optional.of(new PackageLayout(4, List.of(0, 3))),
                Optional.empty(),
                0);
        ItemStack firstStack = new ItemStack(APItems.packageItems().get(PackageColor.FLUIX).get());
        ItemStack secondStack = new ItemStack(APItems.packageItems().get(PackageColor.FLUIX).get());
        PackageDataStorage.write(firstStack, first);
        PackageDataStorage.write(secondStack, second);

        PackageData decoded = PackageDataStorage.read(firstStack).orElseThrow();
        helper.assertTrue(decoded.layout().isPresent(), "Sparse package layout must round-trip");
        helper.assertTrue(decoded.layout().orElseThrow().contentSlots().equals(List.of(0, 2)),
                "Package layout must retain its empty position");
        helper.assertFalse(first.canonicalHash().equals(second.canonicalHash()),
                "Different sparse positions must produce different identities");
        helper.assertFalse(ItemStack.isSameItemSameTags(firstStack, secondStack),
                "Packages with different empty positions must not stack");
        helper.succeed();
    }

    @GameTest(template = "sequence_buffer_empty")
    public static void singleBufferLatchesAcrossItemFluidAndMeViews(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        SequenceBufferBlockEntity buffer = placeBuffer(helper, pos, APBlocks.SEQUENCE_BUFFER.get().defaultBlockState());
        IItemHandler items = buffer.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(IllegalStateException::new);

        ItemStack simulatedRemainder = items.insertItem(0, new ItemStack(Items.IRON_INGOT, 32), true);
        helper.assertTrue(simulatedRemainder.isEmpty(), "A simulated first insertion should report full acceptance");
        helper.assertTrue(buffer.isEmpty(), "A simulated insertion must not latch the buffer");
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.IRON_INGOT, 32), false).isEmpty(),
                "The first real item insertion should succeed");
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.GOLD_INGOT, 1), false).getCount() == 1,
                "A latched buffer must reject the next insertion");

        helper.startSequence()
                .thenWaitUntil(() -> {
                    ItemStack extracted = items.extractItem(0, 32, false);
                    helper.assertTrue(extracted.getCount() == 32 && buffer.isEmpty(),
                            "Full extraction should empty the buffer after its configured delay");

                    IFluidHandler fluids = buffer.getCapability(ForgeCapabilities.FLUID_HANDLER)
                            .orElseThrow(IllegalStateException::new);
                    helper.assertTrue(fluids.fill(
                                            new FluidStack(Fluids.WATER, 750),
                                            IFluidHandler.FluidAction.EXECUTE)
                                    == 0,
                            "An emptied buffer must still reject another input in the extraction tick");
                    MEStorage storage = buffer.getCapability(Capabilities.STORAGE)
                            .orElseThrow(IllegalStateException::new);
                    helper.assertTrue(storage.insert(
                                            AEItemKey.of(Items.DIAMOND),
                                            1,
                                            Actionable.SIMULATE,
                                            IActionSource.empty())
                                    == 0,
                            "Every capability must observe the same-tick admission marker");
                })
                .thenExecuteAfter(1, () -> {
                    IFluidHandler fluids = buffer.getCapability(ForgeCapabilities.FLUID_HANDLER)
                            .orElseThrow(IllegalStateException::new);
                    helper.assertTrue(fluids.fill(
                                            new FluidStack(Fluids.WATER, 750),
                                            IFluidHandler.FluidAction.EXECUTE)
                                    == 750,
                            "A later empty-buffer tick must release the admission marker");
                    helper.assertTrue(fluids.fill(
                                            new FluidStack(Fluids.LAVA, 1),
                                            IFluidHandler.FluidAction.EXECUTE)
                                    == 0,
                            "FluidHandler must obey the same one-input latch");

                    MEStorage storage = buffer.getCapability(Capabilities.STORAGE)
                            .orElseThrow(IllegalStateException::new);
                    helper.assertTrue(storage.insert(
                                            AEItemKey.of(Items.DIAMOND),
                                            1,
                                            Actionable.SIMULATE,
                                            IActionSource.empty())
                                    == 0,
                            "MEStorage must see the same latched state as Forge capabilities");
                })
                .thenSucceed();
    }

    @GameTest(template = "sequence_buffer_empty")
    public static void menuExtractionBypassesOutputDelayAndKeepsAdmissionCooldown(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        SequenceBufferBlockEntity buffer = placeBuffer(
                helper,
                pos,
                APBlocks.SEQUENCE_BUFFER.get().defaultBlockState());
        var configuration = buffer.configurationCopy();
        configuration.setAutoOutput(false);
        configuration.setBlockingMode(true);
        configuration.setSynchronizedOutput(true);
        configuration.setInputDelayTicks(40);
        buffer.updateConfiguration(configuration);

        IItemHandler items = buffer.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(IllegalStateException::new);
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(),
                "The buffer should accept the delayed test input");
        helper.assertTrue(items.extractItem(0, 8, false).isEmpty() && buffer.storedAmount() == 8,
                "External capability extraction must remain blocked by the output delay");

        ItemStack simulated = buffer.extractMenuItem(8, true);
        helper.assertTrue(simulated.is(Items.IRON_INGOT)
                        && simulated.getCount() == 8
                        && buffer.storedAmount() == 8,
                "A simulated player GUI extraction must bypass output delay without changing storage");
        ItemStack extracted = buffer.extractMenuItem(8, false);
        helper.assertTrue(extracted.is(Items.IRON_INGOT)
                        && extracted.getCount() == 8
                        && buffer.isEmpty(),
                "Blocking, synchronized output, and output delay must not block player GUI extraction");

        long expectedOpenAt = helper.getLevel().getGameTime() + 1;
        helper.assertTrue(buffer.admissionOpenAtGameTime() == expectedOpenAt,
                "GUI extraction of the final item must close admission until the next game tick");
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.GOLD_INGOT), false).getCount() == 1,
                "The extraction tick must still reject a new input");

        SequenceBufferBlockEntity loaded = new SequenceBufferBlockEntity(BlockPos.ZERO, buffer.getBlockState());
        loaded.load(buffer.saveWithoutMetadata());
        helper.assertTrue(loaded.admissionOpenAtGameTime() == expectedOpenAt,
                "The absolute admission reopening time must round-trip through NBT");

        helper.startSequence()
                .thenExecuteAfter(1, () -> helper.assertTrue(
                        items.insertItem(0, new ItemStack(Items.GOLD_INGOT), false).isEmpty(),
                        "Admission must reopen by game time without requiring a buffer tick first"))
                .thenSucceed();
    }

    @GameTest(template = "sequence_buffer_empty")
    public static void bufferStateAndConfigurationRoundTrip(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        SequenceBufferBlockEntity buffer = placeBuffer(
                helper,
                pos,
                APBlocks.SEQUENCE_BUFFER.get().defaultBlockState());
        var configuration = buffer.configurationCopy();
        configuration.setAutoOutput(false);
        configuration.setBlockingMode(true);
        configuration.setSynchronizedOutput(true);
        configuration.setPatternMode(true);
        configuration.setInputDelayTicks(9);
        configuration.setAllowedInputs(List.of(AEFluidKey.of(Fluids.WATER)));
        buffer.updateConfiguration(configuration);
        MEStorage storage = buffer.getCapability(Capabilities.STORAGE)
                .orElseThrow(IllegalStateException::new);
        helper.assertTrue(storage.insert(
                                AEFluidKey.of(Fluids.WATER),
                                900,
                                Actionable.MODULATE,
                                IActionSource.empty())
                        == 900,
                "Persistence test should store one filtered fluid key");

        SequenceBufferBlockEntity loaded = new SequenceBufferBlockEntity(BlockPos.ZERO, buffer.getBlockState());
        loaded.load(buffer.saveWithoutMetadata());
        helper.assertTrue(loaded.storedKey().equals(AEFluidKey.of(Fluids.WATER))
                        && loaded.storedAmount() == 900
                        && loaded.releaseAtGameTime() == buffer.releaseAtGameTime(),
                "Sequence Buffer key, amount, and release barrier must round-trip through NBT");
        helper.assertTrue(loaded.configurationCopy().equals(configuration),
                "Every reserved first-version GUI configuration field must round-trip through NBT");
        helper.succeed();
    }

    @GameTest(template = "sequence_buffer_empty")
    public static void reservedFilterAndRedstoneUpgradeRoundTrip(GameTestHelper helper) {
        SequenceBufferBlockEntity buffer = placeBuffer(
                helper,
                new BlockPos(1, 1, 1),
                APBlocks.SEQUENCE_BUFFER.get().defaultBlockState());

        buffer.inputFilter().setStack(0, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 0));
        helper.assertTrue(buffer.configurationCopy().accepts(AEItemKey.of(Items.IRON_INGOT))
                        && !buffer.configurationCopy().accepts(AEItemKey.of(Items.GOLD_INGOT)),
                "The reserved input filter must update the authoritative allowed-input configuration");
        helper.assertTrue(buffer.getUpgrades().getMaxInstalled(AEItems.REDSTONE_CARD) == 1,
                "Sequence Buffer must expose exactly one compatible redstone-card upgrade");
        helper.assertTrue(buffer.getUpgrades().addItems(AEItems.REDSTONE_CARD.stack()).isEmpty(),
                "The Sequence Buffer upgrade slot must accept one redstone card");
        helper.assertFalse(buffer.getUpgrades().addItems(AEItems.REDSTONE_CARD.stack()).isEmpty(),
                "The Sequence Buffer upgrade slot must reject a second redstone card");

        SequenceBufferBlockEntity loaded = new SequenceBufferBlockEntity(BlockPos.ZERO, buffer.getBlockState());
        loaded.load(buffer.saveWithoutMetadata());
        helper.assertTrue(loaded.getUpgrades().isInstalled(AEItems.REDSTONE_CARD),
                "The redstone-card upgrade must survive NBT round-trip");
        helper.assertTrue(loaded.inputFilter().getKey(0).equals(AEItemKey.of(Items.IRON_INGOT))
                        && loaded.configurationCopy().equals(buffer.configurationCopy()),
                "The reserved filter inventory and configuration must reconstruct the same AEKey allow-list after load");
        helper.assertTrue(SequenceBufferMainMenu.maxScrollOffsetForMemberCount(27) == 0
                        && SequenceBufferMainMenu.maxScrollOffsetForMemberCount(28) == 1
                        && SequenceBufferMainMenu.maxScrollOffsetForMemberCount(36) == 1
                        && SequenceBufferMainMenu.maxScrollOffsetForMemberCount(37) == 2,
                "The main GUI scrollbar must stay disabled through 3x9 and advance in complete nine-slot rows");

        FakePlayer menuPlayer = FakePlayerFactory.getMinecraft((ServerLevel) helper.getLevel());
        menuPlayer.getInventory().clearContent();
        Inventory menuInventory = new Inventory(menuPlayer);
        helper.assertTrue(buffer.insertMenuItem(new ItemStack(Items.IRON_INGOT, 5), 5, false) == 5,
                "Side-menu transfer test requires one stored item stack");
        SequenceBufferSideMenu sideMenu = new SequenceBufferSideMenu(
                17,
                menuInventory,
                buffer,
                buffer);
        int displaySlot = sideMenu.slots.indexOf(
                sideMenu.getSlots(SequenceBufferMainMenu.BUFFER_CONTENTS).get(0));

        BlockPos endpointPos = new BlockPos(3, 1, 1);
        BlockPos memberPos = endpointPos.east();
        SequenceBufferBlockEntity endpoint = placeBuffer(
                helper,
                endpointPos,
                APBlocks.SEQUENCE_BUFFER.get().defaultBlockState()
                        .setValue(SequenceBufferBlock.STATE, SequenceBufferVisualState.UNFORMED_DIRECTED)
                        .setValue(SequenceBufferBlock.DIRECTIONAL, true)
                        .setValue(SequenceBufferBlock.FACING, Direction.EAST));
        SequenceBufferBlockEntity member = placeBuffer(
                helper,
                memberPos,
                APBlocks.SEQUENCE_BUFFER.get().defaultBlockState());
        helper.assertTrue(member.getUpgrades().addItems(AEItems.REDSTONE_CARD.stack()).isEmpty(),
                "Formation upgrade migration requires a card in the future member");
        helper.assertTrue(SequenceBufferTopology.tryForm(
                                helper.getLevel(),
                                helper.absolutePos(endpointPos),
                                Direction.EAST)
                        && endpoint.getUpgrades().isInstalled(AEItems.REDSTONE_CARD)
                        && !member.getUpgrades().isInstalled(AEItems.REDSTONE_CARD),
                "Formation must move a standalone member's physical upgrade into endpoint authority");
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    ItemStack moved = sideMenu.quickMoveStack(menuPlayer, displaySlot);
                    helper.assertTrue(moved.getCount() == 5
                                    && buffer.isEmpty()
                                    && menuInventory.countItem(Items.IRON_INGOT) == 5,
                            "Shift-clicking the side storage slot must target menu player slots after the input barrier");
                })
                .thenSucceed();
    }

    @GameTest(template = "sequence_buffer_empty", timeoutTicks = 80)
    public static void redstoneCardGatesAutomaticOutputAtAuthority(GameTestHelper helper) {
        BlockPos bufferPos = new BlockPos(2, 1, 2);
        BlockPos chestPos = bufferPos.north();
        BlockPos powerPos = bufferPos.west();
        SequenceBufferBlockEntity buffer = placeBuffer(
                helper,
                bufferPos,
                APBlocks.SEQUENCE_BUFFER.get().defaultBlockState());
        helper.getLevel().setBlock(helper.absolutePos(chestPos), Blocks.CHEST.defaultBlockState(), 3);
        helper.assertTrue(buffer.getUpgrades().addItems(AEItems.REDSTONE_CARD.stack()).isEmpty(),
                "Redstone gating test requires an installed redstone card");
        helper.assertTrue(buffer.insertMenuItem(new ItemStack(Items.IRON_INGOT, 4), 4, false) == 4,
                "Menu insertion should fill the single storage member before redstone output testing");

        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
                    helper.assertTrue(chest.isEmpty() && buffer.storedAmount() == 4,
                            "An installed redstone card must block automatic output without a signal");
                    helper.getLevel().setBlock(
                            helper.absolutePos(powerPos),
                            Blocks.REDSTONE_BLOCK.defaultBlockState(),
                            3);
                })
                .thenExecuteAfter(3, () -> {
                    ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
                    helper.assertTrue(chest.countItem(Items.IRON_INGOT) == 4 && buffer.isEmpty(),
                            "A signal at the authority must release automatic output for the redstone-controlled buffer");
                })
                .thenSucceed();
    }

    @GameTest(template = "sequence_buffer_empty")
    public static void formedEndpointHasNoStorageAndSequencesMembers(GameTestHelper helper) {
        List<SequenceBufferBlockEntity> blocks = formEastLine(helper, 4);
        SequenceBufferBlockEntity endpoint = blocks.get(0);
        List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(endpoint);

        helper.assertTrue(endpoint.isEndpoint() && endpoint.isEmpty(),
                "The controller endpoint must have no local storage");
        helper.assertTrue(members.size() == 3 && members.get(0).sequenceIndex() == 1,
                "The block after the endpoint must be logical storage slot 1");
        IItemHandler itemHandler = endpoint.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(IllegalStateException::new);
        helper.assertTrue(itemHandler.getSlots() == 3, "Endpoint item slots must exclude the endpoint block");
        helper.assertTrue(itemHandler.insertItem(0, new ItemStack(Items.IRON_INGOT, 5), false).isEmpty(),
                "First endpoint insertion should target the first member");
        helper.assertTrue(itemHandler.insertItem(1, new ItemStack(Items.GOLD_INGOT, 3), false).isEmpty(),
                "Second endpoint insertion should target the second member");
        helper.assertTrue(members.get(0).storedKey().equals(AEItemKey.of(Items.IRON_INGOT))
                        && members.get(1).storedKey().equals(AEItemKey.of(Items.GOLD_INGOT))
                        && members.get(2).isEmpty(),
                "Endpoint insertion order must be stable and must not use the endpoint as slot 1");
        helper.succeed();
    }

    @GameTest(template = "sequence_buffer_empty")
    public static void endpointAggregateExtractionMergesMembers(GameTestHelper helper) {
        List<SequenceBufferBlockEntity> blocks = formEastLine(helper, 4);
        SequenceBufferBlockEntity endpoint = blocks.get(0);
        MEStorage storage = endpoint.getCapability(Capabilities.STORAGE)
                .orElseThrow(IllegalStateException::new);
        helper.assertTrue(storage.insert(
                                AEItemKey.of(Items.IRON_INGOT),
                                5,
                                Actionable.MODULATE,
                                IActionSource.empty())
                        == 5,
                "First aggregate insertion should use logical member 1");
        helper.assertTrue(storage.insert(
                                AEItemKey.of(Items.GOLD_INGOT),
                                1,
                                Actionable.MODULATE,
                                IActionSource.empty())
                        == 1,
                "Second aggregate insertion should use logical member 2");
        helper.assertTrue(storage.insert(
                                AEItemKey.of(Items.IRON_INGOT),
                                7,
                                Actionable.MODULATE,
                                IActionSource.empty())
                        == 7,
                "Third aggregate insertion should use logical member 3 without merging inputs");

        helper.succeedWhen(() -> {
            long extracted = storage.extract(
                    AEItemKey.of(Items.IRON_INGOT),
                    12,
                    Actionable.MODULATE,
                    IActionSource.empty());
            List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(endpoint);
            helper.assertTrue(extracted == 12,
                    "Endpoint MEStorage should merge matching keys across ordered members for extraction");
            helper.assertTrue(members.get(0).isEmpty()
                            && members.get(1).storedKey().equals(AEItemKey.of(Items.GOLD_INGOT))
                            && members.get(2).isEmpty(),
                    "Aggregate extraction must leave unrelated member contents untouched");
        });
    }

    @GameTest(template = "sequence_buffer_empty")
    public static void menuSettingsUpdateEndpointAndEveryMember(GameTestHelper helper) {
        List<SequenceBufferBlockEntity> blocks = formEastLine(helper, 3);
        SequenceBufferBlockEntity endpoint = blocks.get(0);
        FakePlayer menuPlayer = FakePlayerFactory.getMinecraft((ServerLevel) helper.getLevel());
        menuPlayer.getInventory().clearContent();
        SequenceBufferMainMenu menu = new SequenceBufferMainMenu(
                18,
                new Inventory(menuPlayer),
                endpoint);

        menu.toggleAutoOutput();
        menu.toggleBlockingMode();
        menu.toggleSynchronizedOutput();
        menu.togglePatternMode();
        menu.cycleInputDelay(false);
        menu.broadcastChanges();

        var expected = endpoint.configurationCopy();
        helper.assertTrue(!expected.autoOutput()
                        && expected.blockingMode()
                        && expected.synchronizedOutput()
                        && expected.patternMode()
                        && expected.inputDelayTicks() == 5,
                "All five GUI settings must update the endpoint configuration");
        helper.assertTrue(menu.autoOutput() == expected.autoOutput()
                        && menu.blockingMode() == expected.blockingMode()
                        && menu.synchronizedOutput() == expected.synchronizedOutput()
                        && menu.patternMode() == expected.patternMode()
                        && menu.inputDelayTicks() == expected.inputDelayTicks(),
                "GuiSync fields must mirror the endpoint configuration");
        for (SequenceBufferBlockEntity member : SequenceBufferTopology.members(endpoint)) {
            helper.assertTrue(member.configurationCopy().equals(expected),
                    "Every storage member must receive settings changed through either GUI");
        }

        menu.cycleInputDelay(true);
        menu.broadcastChanges();
        helper.assertTrue(endpoint.configurationCopy().inputDelayTicks() == 1
                        && menu.inputDelayTicks() == 1,
                "Right-click delay cycling must move to the previous preset");
        menu.cycleInputDelay(true);
        menu.broadcastChanges();
        helper.assertTrue(endpoint.configurationCopy().inputDelayTicks() == 0
                        && menu.inputDelayTicks() == 0,
                "Input delay preset cycling must allow zero ticks");
        menu.cycleInputDelay(false);
        menu.broadcastChanges();
        helper.assertTrue(endpoint.configurationCopy().inputDelayTicks() == 1
                        && menu.inputDelayTicks() == 1,
                "Increasing the zero-tick preset must return to one tick");
        helper.succeed();
    }

    @GameTest(template = "sequence_buffer_empty", timeoutTicks = 120)
    public static void topologyFormsFromUpdatesExtendsAndDetaches(GameTestHelper helper) {
        BlockPos endpointPos = new BlockPos(1, 1, 1);
        BlockPos firstMemberPos = endpointPos.east();
        SequenceBufferBlockEntity endpoint = placeBuffer(
                helper,
                endpointPos,
                APBlocks.SEQUENCE_BUFFER.get()
                        .defaultBlockState()
                        .setValue(SequenceBufferBlock.STATE, SequenceBufferVisualState.UNFORMED_DIRECTED)
                        .setValue(SequenceBufferBlock.DIRECTIONAL, true)
                        .setValue(SequenceBufferBlock.FACING, Direction.EAST));
        var configuration = endpoint.configurationCopy();
        configuration.setBlockingMode(true);
        configuration.setInputDelayTicks(7);
        configuration.setAllowedInputs(List.of(AEItemKey.of(Items.IRON_INGOT)));
        endpoint.updateConfiguration(configuration);
        placeBuffer(helper, firstMemberPos, APBlocks.SEQUENCE_BUFFER.get().defaultBlockState());

        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(endpoint.isEndpoint(),
                            "A directed endpoint should form from scheduled block updates");
                    List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(endpoint);
                    helper.assertTrue(members.size() == 1
                                    && members.get(0).configurationCopy().equals(configuration)
                                    && members.get(0).getBlockState().getValue(SequenceBufferBlock.TAIL)
                                    && members.get(0).getBlockState().getValue(
                                            SequenceBufferBlock.SEQUENCE_DIRECTION) == Direction.EAST,
                            "Initial member must copy the endpoint configuration and render as the east tail");
                    placeBuffer(
                            helper,
                            endpointPos.east(2),
                            APBlocks.SEQUENCE_BUFFER.get().defaultBlockState());
                })
                .thenExecuteAfter(3, () -> {
                    List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(endpoint);
                    helper.assertTrue(members.size() == 2
                                    && members.get(1).sequenceIndex() == 2
                                    && members.get(1).configurationCopy().equals(configuration)
                                    && !members.get(0).getBlockState().getValue(SequenceBufferBlock.TAIL)
                                    && members.get(1).getBlockState().getValue(SequenceBufferBlock.TAIL)
                                    && members.get(1).getBlockState().getValue(
                                            SequenceBufferBlock.SEQUENCE_DIRECTION) == Direction.EAST,
                            "A continuous tail block should auto-join, inherit configuration, and move the tail model");
                    BlockPos competingPos = endpointPos.west();
                    placeBuffer(
                            helper,
                            competingPos,
                            APBlocks.SEQUENCE_BUFFER.get()
                                    .defaultBlockState()
                                    .setValue(SequenceBufferBlock.STATE, SequenceBufferVisualState.UNFORMED_DIRECTED)
                                    .setValue(SequenceBufferBlock.DIRECTIONAL, true)
                                    .setValue(SequenceBufferBlock.FACING, Direction.EAST));
                })
                .thenExecuteAfter(3, () -> {
                    BlockState competing = helper.getBlockState(endpointPos.west());
                    helper.assertTrue(competing.getValue(SequenceBufferBlock.STATE)
                                    == SequenceBufferVisualState.UNFORMED_DIRECTED,
                            "A directed candidate facing an existing formed endpoint must remain unformed");
                    helper.getLevel().destroyBlock(helper.absolutePos(endpointPos.west()), false);
                    helper.getLevel().destroyBlock(helper.absolutePos(firstMemberPos), false);
                })
                .thenExecuteAfter(3, () -> {
                    BlockState detachedEndpoint = helper.getBlockState(endpointPos);
                    helper.assertTrue(detachedEndpoint.getValue(SequenceBufferBlock.STATE)
                                    == SequenceBufferVisualState.UNFORMED_DIRECTED
                                    && detachedEndpoint.getValue(SequenceBufferBlock.DIRECTIONAL)
                                    && detachedEndpoint.getValue(SequenceBufferBlock.FACING) == Direction.EAST,
                            "Breaking the first member should disassemble the endpoint without clearing its direction");
                    SequenceBufferBlockEntity detachedTail =
                            (SequenceBufferBlockEntity) helper.getBlockEntity(endpointPos.east(2));
                    helper.assertTrue(detachedTail.sequenceIndex() == -1
                                    && detachedTail.configurationCopy().equals(configuration),
                            "Detached tail members must keep their synchronized configuration without a topology index");
                })
                .thenSucceed();
    }

    @GameTest(template = "sequence_buffer_empty", timeoutTicks = 120)
    public static void wrenchCyclesOppositeClickedAndNeutralDirections(GameTestHelper helper) {
        BlockPos endpointPos = new BlockPos(2, 1, 2);
        BlockPos memberPos = endpointPos.east();
        placeBuffer(helper, endpointPos, APBlocks.SEQUENCE_BUFFER.get().defaultBlockState());
        placeBuffer(helper, memberPos, APBlocks.SEQUENCE_BUFFER.get().defaultBlockState());
        FakePlayer player = FakePlayerFactory.get(
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "sequence_buffer_wrench_test"));
        player.setItemInHand(InteractionHand.MAIN_HAND, AEItems.CERTUS_QUARTZ_WRENCH.stack());

        InteractionResult first = wrenchBuffer(helper, player, endpointPos, Direction.WEST);
        BlockState firstState = helper.getBlockState(endpointPos);
        helper.assertTrue(first.consumesAction()
                        && firstState.getValue(SequenceBufferBlock.STATE)
                                == SequenceBufferVisualState.UNFORMED_DIRECTED
                        && firstState.getValue(SequenceBufferBlock.FACING) == Direction.EAST,
                "First wrench use must point toward the side opposite the clicked face");

        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    BlockState formedEndpoint = helper.getBlockState(endpointPos);
                    helper.assertTrue(formedEndpoint.getValue(SequenceBufferBlock.STATE)
                                    == SequenceBufferVisualState.ENDPOINT
                                    && formedEndpoint.getValue(SequenceBufferBlock.FACING) == Direction.EAST,
                            "The opposite-face direction should still form an adjacent line");

                    InteractionResult second = wrenchBuffer(helper, player, endpointPos, Direction.WEST);
                    BlockState secondState = helper.getBlockState(endpointPos);
                    helper.assertTrue(second.consumesAction()
                                    && secondState.getValue(SequenceBufferBlock.STATE)
                                            == SequenceBufferVisualState.UNFORMED_DIRECTED
                                    && secondState.getValue(SequenceBufferBlock.FACING) == Direction.WEST,
                            "Second wrench use must disassemble and point toward the clicked face");
                    helper.assertTrue(helper.getBlockState(memberPos).getValue(SequenceBufferBlock.STATE)
                                    == SequenceBufferVisualState.UNFORMED,
                            "Advancing a formed endpoint direction must disassemble its old members");

                    InteractionResult third = wrenchBuffer(helper, player, endpointPos, Direction.WEST);
                    helper.assertTrue(third.consumesAction()
                                    && helper.getBlockState(endpointPos).getValue(SequenceBufferBlock.STATE)
                                            == SequenceBufferVisualState.UNFORMED,
                            "Third wrench use must restore the undirected state");
                })
                .thenExecute(() -> wrenchBuffer(helper, player, endpointPos, Direction.WEST))
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(helper.getBlockState(memberPos).getValue(SequenceBufferBlock.STATE)
                                    == SequenceBufferVisualState.MEMBER,
                            "The line should reform before checking member output direction cycling");

                    wrenchBuffer(helper, player, memberPos, Direction.DOWN);
                    BlockState memberOpposite = helper.getBlockState(memberPos);
                    helper.assertTrue(memberOpposite.getValue(SequenceBufferBlock.STATE)
                                    == SequenceBufferVisualState.MEMBER_DIRECTED
                                    && memberOpposite.getValue(SequenceBufferBlock.FACING) == Direction.UP,
                            "A horizontal member's first vertical wrench use must point opposite the clicked face");

                    wrenchBuffer(helper, player, memberPos, Direction.DOWN);
                    BlockState memberClicked = helper.getBlockState(memberPos);
                    helper.assertTrue(memberClicked.getValue(SequenceBufferBlock.STATE)
                                    == SequenceBufferVisualState.MEMBER_DIRECTED
                                    && memberClicked.getValue(SequenceBufferBlock.FACING) == Direction.DOWN,
                            "A horizontal member's second vertical wrench use must point toward the clicked face");

                    wrenchBuffer(helper, player, memberPos, Direction.DOWN);
                    helper.assertTrue(helper.getBlockState(memberPos).getValue(SequenceBufferBlock.STATE)
                                    == SequenceBufferVisualState.MEMBER,
                            "A member's third wrench use must restore its undirected state");
                })
                .thenSucceed();
    }

    @GameTest(template = "sequence_buffer_empty", timeoutTicks = 120)
    public static void singleBufferWrenchSupportsVerticalDirections(GameTestHelper helper) {
        BlockPos bufferPos = new BlockPos(5, 1, 5);
        placeBuffer(helper, bufferPos, APBlocks.SEQUENCE_BUFFER.get().defaultBlockState());
        FakePlayer player = FakePlayerFactory.get(
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "sequence_buffer_vertical_wrench_test"));
        player.setItemInHand(InteractionHand.MAIN_HAND, AEItems.CERTUS_QUARTZ_WRENCH.stack());

        InteractionResult first = wrenchBuffer(helper, player, bufferPos, Direction.DOWN);
        BlockState firstState = helper.getBlockState(bufferPos);
        helper.assertTrue(first.consumesAction()
                        && firstState.getValue(SequenceBufferBlock.DIRECTIONAL)
                        && firstState.getValue(SequenceBufferBlock.STATE)
                                == SequenceBufferVisualState.UNFORMED_DIRECTED
                        && firstState.getValue(SequenceBufferBlock.FACING) == Direction.UP,
                "A single buffer must support an upward direction opposite a bottom-face wrench click");

        InteractionResult second = wrenchBuffer(helper, player, bufferPos, Direction.DOWN);
        BlockState secondState = helper.getBlockState(bufferPos);
        helper.assertTrue(second.consumesAction()
                        && secondState.getValue(SequenceBufferBlock.DIRECTIONAL)
                        && secondState.getValue(SequenceBufferBlock.FACING) == Direction.DOWN,
                "The second bottom-face wrench click must point the single buffer downward");

        InteractionResult third = wrenchBuffer(helper, player, bufferPos, Direction.DOWN);
        BlockState thirdState = helper.getBlockState(bufferPos);
        helper.assertTrue(third.consumesAction()
                        && !thirdState.getValue(SequenceBufferBlock.DIRECTIONAL)
                        && thirdState.getValue(SequenceBufferBlock.STATE) == SequenceBufferVisualState.UNFORMED,
                "The third bottom-face wrench click must restore the single buffer's neutral state");
        helper.succeed();
    }

    @GameTest(template = "sequence_buffer_empty", timeoutTicks = 120)
    public static void directedVisualStateMigratesDirectionFlag(GameTestHelper helper) {
        BlockPos bufferPos = new BlockPos(5, 1, 5);
        placeBuffer(
                helper,
                bufferPos,
                APBlocks.SEQUENCE_BUFFER.get()
                        .defaultBlockState()
                        .setValue(SequenceBufferBlock.STATE, SequenceBufferVisualState.UNFORMED_DIRECTED)
                        .setValue(SequenceBufferBlock.DIRECTIONAL, false)
                        .setValue(SequenceBufferBlock.FACING, Direction.DOWN));

        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    BlockState migrated = helper.getBlockState(bufferPos);
                    helper.assertTrue(migrated.getValue(SequenceBufferBlock.DIRECTIONAL)
                                    && migrated.getValue(SequenceBufferBlock.FACING) == Direction.DOWN
                                    && migrated.getValue(SequenceBufferBlock.STATE)
                                            == SequenceBufferVisualState.UNFORMED_DIRECTED,
                            "A saved directed visual state must recover its direction marker without rotating facing");
                })
                .thenSucceed();
    }

    @GameTest(template = "sequence_buffer_empty", timeoutTicks = 120)
    public static void verticalTopologyPreservesEveryBlocksOwnDirection(GameTestHelper helper) {
        BlockPos endpointPos = new BlockPos(2, 1, 2);
        BlockPos firstMemberPos = endpointPos.above();
        BlockPos secondMemberPos = endpointPos.above(2);
        SequenceBufferBlockEntity endpoint = placeBuffer(
                helper,
                endpointPos,
                APBlocks.SEQUENCE_BUFFER.get()
                        .defaultBlockState()
                        .setValue(SequenceBufferBlock.STATE, SequenceBufferVisualState.UNFORMED_DIRECTED)
                        .setValue(SequenceBufferBlock.DIRECTIONAL, true)
                        .setValue(SequenceBufferBlock.FACING, Direction.UP));
        placeBuffer(
                helper,
                firstMemberPos,
                APBlocks.SEQUENCE_BUFFER.get()
                        .defaultBlockState()
                        .setValue(SequenceBufferBlock.STATE, SequenceBufferVisualState.UNFORMED_DIRECTED)
                        .setValue(SequenceBufferBlock.DIRECTIONAL, true)
                        .setValue(SequenceBufferBlock.FACING, Direction.NORTH));

        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    BlockState endpointState = helper.getBlockState(endpointPos);
                    BlockState memberState = helper.getBlockState(firstMemberPos);
                    helper.assertTrue(endpoint.isEndpoint()
                                    && endpointState.getValue(SequenceBufferBlock.FACING) == Direction.UP
                                    && endpointState.getValue(SequenceBufferBlock.SEQUENCE_DIRECTION) == Direction.UP
                                    && endpointState.getValue(SequenceBufferBlock.AXIS) == Direction.Axis.Y,
                            "A vertical line must form upward without replacing the endpoint's own direction");
                    helper.assertTrue(memberState.getValue(SequenceBufferBlock.DIRECTIONAL)
                                    && memberState.getValue(SequenceBufferBlock.FACING) == Direction.NORTH
                                    && memberState.getValue(SequenceBufferBlock.STATE)
                                            == SequenceBufferVisualState.MEMBER_DIRECTED
                                    && memberState.getValue(SequenceBufferBlock.TAIL),
                            "A vertical member must retain its perpendicular north direction while formed");
                    placeBuffer(
                            helper,
                            secondMemberPos,
                            APBlocks.SEQUENCE_BUFFER.get()
                                    .defaultBlockState()
                                    .setValue(SequenceBufferBlock.STATE, SequenceBufferVisualState.UNFORMED_DIRECTED)
                                    .setValue(SequenceBufferBlock.DIRECTIONAL, true)
                                    .setValue(SequenceBufferBlock.FACING, Direction.UP));
                })
                .thenExecuteAfter(3, () -> {
                    BlockState firstMember = helper.getBlockState(firstMemberPos);
                    BlockState secondMember = helper.getBlockState(secondMemberPos);
                    helper.assertTrue(!firstMember.getValue(SequenceBufferBlock.TAIL)
                                    && secondMember.getValue(SequenceBufferBlock.TAIL),
                            "Extending a vertical line must move its tail marker upward");
                    helper.assertTrue(secondMember.getValue(SequenceBufferBlock.DIRECTIONAL)
                                    && secondMember.getValue(SequenceBufferBlock.FACING) == Direction.UP
                                    && secondMember.getValue(SequenceBufferBlock.STATE)
                                            == SequenceBufferVisualState.MEMBER,
                            "A parallel pre-formation direction must remain stored while its connection model is visible");
                    SequenceBufferTopology.disassembleEndpoint(helper.getLevel(), helper.absolutePos(endpointPos));
                    helper.assertTrue(helper.getBlockState(endpointPos).getValue(SequenceBufferBlock.FACING)
                                            == Direction.UP
                                    && helper.getBlockState(firstMemberPos).getValue(SequenceBufferBlock.FACING)
                                            == Direction.NORTH
                                    && helper.getBlockState(secondMemberPos).getValue(SequenceBufferBlock.FACING)
                                            == Direction.UP,
                            "Disassembly must restore every block with exactly its original wrench direction");
                    helper.assertTrue(helper.getBlockState(endpointPos).getValue(SequenceBufferBlock.STATE)
                                            == SequenceBufferVisualState.UNFORMED_DIRECTED
                                    && helper.getBlockState(firstMemberPos).getValue(SequenceBufferBlock.STATE)
                                            == SequenceBufferVisualState.UNFORMED_DIRECTED
                                    && helper.getBlockState(secondMemberPos).getValue(SequenceBufferBlock.STATE)
                                            == SequenceBufferVisualState.UNFORMED_DIRECTED,
                            "Every direction preserved through formation must become visible again after disassembly");
                })
                .thenSucceed();
    }

    @GameTest(template = "sequence_buffer_empty", timeoutTicks = 120)
    public static void blockingModeWaitsForEmptyTarget(GameTestHelper helper) {
        BlockPos bufferPos = new BlockPos(2, 1, 2);
        BlockPos chestPos = bufferPos.north();
        SequenceBufferBlockEntity buffer = placeBuffer(
                helper,
                bufferPos,
                APBlocks.SEQUENCE_BUFFER.get()
                        .defaultBlockState()
                        .setValue(SequenceBufferBlock.STATE, SequenceBufferVisualState.UNFORMED_DIRECTED)
                        .setValue(SequenceBufferBlock.DIRECTIONAL, true)
                        .setValue(SequenceBufferBlock.FACING, Direction.NORTH));
        helper.getLevel().setBlock(helper.absolutePos(chestPos), Blocks.CHEST.defaultBlockState(), 3);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 1));
        var configuration = buffer.configurationCopy();
        configuration.setBlockingMode(true);
        buffer.updateConfiguration(configuration);
        IItemHandler handler = buffer.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(IllegalStateException::new);
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 5), false).isEmpty(),
                "Blocking-mode test input should latch in the single buffer");

        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    helper.assertTrue(buffer.storedKey().equals(AEItemKey.of(Items.IRON_INGOT))
                                    && buffer.storedAmount() == 5
                                    && chest.countItem(Items.IRON_INGOT) == 0,
                            "Blocking mode must wait while any target slot is non-empty");
                    chest.clearContent();
                })
                .thenExecuteAfter(3, () -> helper.assertTrue(
                        buffer.isEmpty() && chest.countItem(Items.IRON_INGOT) == 5,
                        "Blocking mode should output after the complete target becomes empty"))
                .thenSucceed();
    }

    @GameTest(template = "sequence_buffer_empty")
    public static void patternAndPackageInputsPreserveEmptyPositions(GameTestHelper helper) {
        List<SequenceBufferBlockEntity> blocks = formEastLine(helper, 5);
        SequenceBufferBlockEntity endpoint = blocks.get(0);
        GenericStack[] sparse = new GenericStack[PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT];
        sparse[0] = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 8);
        sparse[2] = new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 4);
        var encoded = PackageCraftingPatternDataStorage.create(PackageColor.BLUE, sparse, Optional.empty())
                .orElseThrow();
        ItemStack patternStack = PackageCraftingPatternDataStorage.encode(encoded);
        PackageCraftingPatternDetails details = new PackageCraftingPatternDetails(AEItemKey.of(patternStack));
        KeyCounter input = new KeyCounter();
        input.add(AEItemKey.of(Items.IRON_INGOT), 8);
        input.add(AEItemKey.of(Items.GOLD_INGOT), 4);

        helper.assertTrue(endpoint.isEndpoint() && SequenceBufferTopology.members(endpoint).size() == 4,
                "Sparse pattern test requires one endpoint and four storage members");
        List<SequenceBufferBlockEntity> emptyMembers = SequenceBufferTopology.members(endpoint);
        helper.assertTrue(emptyMembers.stream().allMatch(SequenceBufferBlockEntity::isEmpty),
                "Sparse package preflight requires every target member to be empty");
        helper.assertTrue(encoded.data().contents().size() == 2
                        && encoded.data().contents().get(0).what().equals(AEItemKey.of(Items.IRON_INGOT))
                        && encoded.data().contents().get(0).amount() == 8
                        && encoded.data().contents().get(1).what().equals(AEItemKey.of(Items.GOLD_INGOT))
                        && encoded.data().contents().get(1).amount() == 4,
                "Sparse package data must retain both requested input stacks");
        helper.assertTrue(encoded.data().layout().orElseThrow().contentSlots().equals(List.of(0, 2)),
                "Sparse package layout must map contents to logical members 1 and 3");
        helper.assertTrue(endpoint.configurationCopy().accepts(AEItemKey.of(Items.IRON_INGOT))
                        && endpoint.configurationCopy().accepts(AEItemKey.of(Items.GOLD_INGOT))
                        && emptyMembers.get(0).configurationCopy().accepts(AEItemKey.of(Items.IRON_INGOT))
                        && emptyMembers.get(2).configurationCopy().accepts(AEItemKey.of(Items.GOLD_INGOT)),
                "Endpoint and target members must accept the sparse package keys");
        helper.assertTrue(endpoint.acceptPackage(encoded.data(), true),
                "The same sparse layout should pass the endpoint package preflight");
        helper.assertTrue(input.get(AEItemKey.of(Items.IRON_INGOT)) == 8
                        && input.get(AEItemKey.of(Items.GOLD_INGOT)) == 4,
                "Pattern input holder must contain the exact encoded resources");
        helper.assertTrue(endpoint.pushPattern(details, new KeyCounter[] { input }, Direction.WEST),
                "Pattern push should atomically accept a sparse package pattern");
        List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(endpoint);
        helper.assertTrue(members.get(0).storedKey().equals(AEItemKey.of(Items.IRON_INGOT)),
                "Pattern slot 1 must map to logical member 1");
        helper.assertTrue(members.get(1).isEmpty(), "The empty pattern position must remain empty");
        helper.assertTrue(members.get(2).storedKey().equals(AEItemKey.of(Items.GOLD_INGOT)),
                "Pattern slot 3 must map to logical member 3");
        helper.assertTrue(input.isEmpty(), "A successful pattern plan must consume every input holder entry");

        PackageData packageData = encoded.data();
        helper.assertTrue(packageData.layout().orElseThrow().contentSlots().equals(List.of(0, 2)),
                "The package produced by a package pattern must retain sparse positions");
        helper.succeed();
    }

    @GameTest(template = "sequence_buffer_empty")
    public static void advancedPatternAlwaysUsesDenseInputOrder(GameTestHelper helper) {
        List<SequenceBufferBlockEntity> blocks = formEastLine(helper, 4);
        SequenceBufferBlockEntity endpoint = blocks.get(0);
        var configuration = endpoint.configurationCopy();
        configuration.setPatternMode(true);
        endpoint.updateConfiguration(configuration);

        ItemStack patternStack = new ItemStack(APItems.ADVANCED_PROCESSING_PATTERN.get());
        ListTag inputs = new ListTag();
        inputs.add(GenericStack.writeTag(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 8)));
        inputs.add(new CompoundTag());
        inputs.add(GenericStack.writeTag(new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 4)));
        patternStack.getOrCreateTag().put("in", inputs);
        ListTag outputs = new ListTag();
        outputs.add(GenericStack.writeTag(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)));
        patternStack.getOrCreateTag().put("out", outputs);
        AdvancedProcessingPatternDataStorage.write(
                patternStack,
                new AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern(List.of(
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                0,
                                PackageColor.FLUIX,
                                Optional.empty()))));
        AdvancedProcessingPatternDetails details = new AdvancedProcessingPatternDetails(AEItemKey.of(patternStack));
        KeyCounter input = new KeyCounter();
        input.add(AEItemKey.of(Items.IRON_INGOT), 8);
        input.add(AEItemKey.of(Items.GOLD_INGOT), 4);

        helper.assertTrue(endpoint.pushPattern(details, new KeyCounter[] { input }, Direction.WEST),
                "Advanced patterns should remain accepted while pattern mode is enabled");
        List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(endpoint);
        helper.assertTrue(members.get(0).storedKey().equals(AEItemKey.of(Items.IRON_INGOT))
                        && members.get(1).storedKey().equals(AEItemKey.of(Items.GOLD_INGOT))
                        && members.get(2).isEmpty(),
                "Advanced patterns must ignore sparse holes and use dense ordinary input order");
        helper.succeed();
    }

    @GameTest(template = "sequence_buffer_empty")
    public static void packageLayoutDoesNotSkipSlotsWhenPatternModeIsDisabled(GameTestHelper helper) {
        List<SequenceBufferBlockEntity> blocks = formEastLine(helper, 4);
        SequenceBufferBlockEntity endpoint = blocks.get(0);
        PackageData packageData = PackageData.create(
                PackageColor.FLUIX,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 8),
                        new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 4)),
                Optional.of(new PackageLayout(3, List.of(0, 2))),
                Optional.empty(),
                0);

        helper.assertTrue(endpoint.acceptPackage(packageData, false),
                "A package with sparse metadata should still be accepted outside pattern mode");
        List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(endpoint);
        helper.assertTrue(members.get(0).storedKey().equals(AEItemKey.of(Items.IRON_INGOT))
                        && members.get(1).storedKey().equals(AEItemKey.of(Items.GOLD_INGOT))
                        && members.get(2).isEmpty(),
                "Pattern mode off must distribute package contents densely without skipping the recorded blank");
        helper.succeed();
    }

    @GameTest(template = "sequence_buffer_empty")
    public static void endpointFluidHandlerSequencesTanks(GameTestHelper helper) {
        List<SequenceBufferBlockEntity> blocks = formEastLine(helper, 3);
        SequenceBufferBlockEntity endpoint = blocks.get(0);
        IFluidHandler fluids = endpoint.getCapability(ForgeCapabilities.FLUID_HANDLER)
                .orElseThrow(IllegalStateException::new);

        helper.assertTrue(fluids.getTanks() == 2, "Endpoint fluid tanks must exclude the endpoint block");
        helper.assertTrue(fluids.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                "First fluid input should fill the first member");
        helper.assertTrue(fluids.fill(new FluidStack(Fluids.LAVA, 500), IFluidHandler.FluidAction.EXECUTE) == 500,
                "Second fluid input should fill the second member");
        List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(endpoint);
        helper.assertTrue(members.get(0).storedKey().equals(AEFluidKey.of(Fluids.WATER))
                        && members.get(1).storedKey().equals(AEFluidKey.of(Fluids.LAVA)),
                "FluidHandler and MEStorage must share the ordered member state");
        helper.succeed();
    }

    @GameTest(template = "sequence_buffer_empty", timeoutTicks = 120)
    public static void synchronizedOutputWaitsForEveryMember(GameTestHelper helper) {
        List<SequenceBufferBlockEntity> blocks = formEastLineAt(helper, new BlockPos(1, 1, 1), 3);
        SequenceBufferBlockEntity endpoint = blocks.get(0);
        var configuration = endpoint.configurationCopy();
        configuration.setSynchronizedOutput(true);
        configuration.setInputDelayTicks(4);
        endpoint.updateConfiguration(configuration);
        BlockPos firstChestPos = new BlockPos(2, 1, 0);
        BlockPos secondChestPos = new BlockPos(3, 1, 0);
        helper.getLevel().setBlock(helper.absolutePos(firstChestPos), Blocks.CHEST.defaultBlockState(), 3);

        IItemHandler endpointItems = endpoint.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(IllegalStateException::new);
        endpointItems.insertItem(0, new ItemStack(Items.IRON_INGOT, 5), false);
        endpointItems.insertItem(1, new ItemStack(Items.GOLD_INGOT, 3), false);

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    ChestBlockEntity firstChest = (ChestBlockEntity) helper.getBlockEntity(firstChestPos);
                    helper.assertTrue(firstChest.isEmpty(),
                            "Configured input delay must block the whole synchronized structure");
                })
                .thenExecuteAfter(4, () -> {
                    ChestBlockEntity firstChest = (ChestBlockEntity) helper.getBlockEntity(firstChestPos);
                    helper.assertTrue(firstChest.isEmpty(),
                            "Synchronized output must not commit when another member has no target");
                    helper.getLevel().setBlock(
                            helper.absolutePos(secondChestPos),
                            Blocks.CHEST.defaultBlockState(),
                            3);
                })
                .thenExecuteAfter(2, () -> {
                    ChestBlockEntity firstChest = (ChestBlockEntity) helper.getBlockEntity(firstChestPos);
                    ChestBlockEntity secondChest = (ChestBlockEntity) helper.getBlockEntity(secondChestPos);
                    helper.assertTrue(firstChest.countItem(Items.IRON_INGOT) == 5,
                            "First synchronized member should commit after every target is ready");
                    helper.assertTrue(secondChest.countItem(Items.GOLD_INGOT) == 3,
                            "Second synchronized member should commit in the same output round");
                })
                .thenSucceed();
    }

    @GameTest(template = "sequence_buffer_empty", timeoutTicks = 120)
    public static void formedEndpointOwnsMemberTicksAndPreventsSameTickReentry(GameTestHelper helper) {
        BlockPos endpointPos = new BlockPos(1, 1, 1);
        List<SequenceBufferBlockEntity> blocks = formEastLineAt(helper, endpointPos, 3);
        SequenceBufferBlockEntity endpoint = blocks.get(0);
        List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(endpoint);
        var configuration = endpoint.configurationCopy();
        configuration.setInputDelayTicks(0);
        endpoint.updateConfiguration(configuration);

        BlockPos firstChestPos = new BlockPos(2, 1, 0);
        BlockPos secondChestPos = new BlockPos(3, 1, 0);
        helper.getLevel().setBlock(helper.absolutePos(firstChestPos), Blocks.CHEST.defaultBlockState(), 3);
        helper.getLevel().setBlock(helper.absolutePos(secondChestPos), Blocks.CHEST.defaultBlockState(), 3);

        IItemHandler endpointItems = endpoint.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(IllegalStateException::new);
        helper.assertTrue(endpointItems.insertItem(0, new ItemStack(Items.IRON_INGOT, 5), false).isEmpty()
                        && endpointItems.insertItem(1, new ItemStack(Items.GOLD_INGOT, 3), false).isEmpty(),
                "The endpoint should sequence both zero-delay inputs into its members");

        for (SequenceBufferBlockEntity member : members) {
            SequenceBufferBlockEntity.serverTick(
                    helper.getLevel(),
                    member.getBlockPos(),
                    member.getBlockState(),
                    member);
        }
        ChestBlockEntity firstChest = (ChestBlockEntity) helper.getBlockEntity(firstChestPos);
        ChestBlockEntity secondChest = (ChestBlockEntity) helper.getBlockEntity(secondChestPos);
        helper.assertTrue(firstChest.isEmpty() && secondChest.isEmpty(),
                "Formed members must not perform their own automatic-output tick");

        SequenceBufferBlockEntity.serverTick(
                helper.getLevel(),
                endpoint.getBlockPos(),
                endpoint.getBlockState(),
                endpoint);
        helper.assertTrue(firstChest.countItem(Items.IRON_INGOT) == 5
                        && secondChest.countItem(Items.GOLD_INGOT) == 3
                        && members.stream().allMatch(SequenceBufferBlockEntity::isEmpty),
                "One endpoint tick must proxy ordinary automatic output for every formed member");

        for (SequenceBufferBlockEntity member : members) {
            MEStorage storage = member.getCapability(Capabilities.STORAGE)
                    .orElseThrow(IllegalStateException::new);
            helper.assertTrue(storage.insert(
                                    AEItemKey.of(Items.DIAMOND),
                                    1,
                                    Actionable.SIMULATE,
                                    IActionSource.empty())
                            == 0,
                    "A member emptied by the endpoint must still reject another input in the same tick");
        }

        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    for (SequenceBufferBlockEntity member : members) {
                        MEStorage storage = member.getCapability(Capabilities.STORAGE)
                                .orElseThrow(IllegalStateException::new);
                        helper.assertTrue(storage.insert(
                                                AEItemKey.of(Items.DIAMOND),
                                                1,
                                                Actionable.SIMULATE,
                                                IActionSource.empty())
                                        == 1,
                                "Game time must reopen every empty member without another endpoint tick");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = "sequence_buffer_empty", timeoutTicks = 160)
    @SuppressWarnings("unchecked")
    public static void unpackingBusPreservesAdvancedPatternLayoutIntoSequenceBuffer(GameTestHelper helper) {
        BlockPos endpointPos = new BlockPos(1, 1, 0);
        List<SequenceBufferBlockEntity> blocks = formEastLineAt(helper, endpointPos, 4);
        SequenceBufferBlockEntity endpoint = blocks.get(0);
        var configuration = endpoint.configurationCopy();
        configuration.setPatternMode(true);
        endpoint.updateConfiguration(configuration);
        BlockPos partPos = new BlockPos(1, 1, 1);
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
                Direction.NORTH,
                null,
                partItem);
        helper.assertTrue(part != null, "Package Unpacking Bus should place beside the sequence endpoint");

        GenericStack plank = new GenericStack(AEItemKey.of(Items.OAK_PLANKS), 1);
        List<GenericStack> sparseInputs = new ArrayList<>();
        sparseInputs.add(plank);
        sparseInputs.add(null);
        sparseInputs.add(plank);
        ItemStack patternStack = APItems.ADVANCED_PROCESSING_PATTERN.get().encode(
                new GenericStack[] { plank, plank },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.DIAMOND), 1) });
        AdvancedProcessingPatternDataStorage.write(
                patternStack,
                new AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern(List.of(
                        new AdvancedProcessingPatternDataStorage.PackageColumn(
                                0,
                                PackageColor.GREEN,
                                Optional.empty(),
                                sparseInputs))));
        AdvancedProcessingPatternDetails details = new AdvancedProcessingPatternDetails(AEItemKey.of(patternStack));
        KeyCounter input = new KeyCounter();
        input.add(AEItemKey.of(Items.OAK_PLANKS), 2);
        PackageAssemblerBlockEntity assembler = new PackageAssemblerBlockEntity(
                BlockPos.ZERO,
                APBlocks.PACKAGE_ASSEMBLER.get().defaultBlockState());
        helper.assertTrue(assembler.pushPattern(details, new KeyCounter[] { input }, Direction.UP),
                "Package Assembler should accept the sparse advanced pattern");
        ItemStack packageStack = assembler.getItems()
                .getStackInSlot(PackageAssemblerBlockEntity.SLOT_OUTPUT)
                .copy();
        PackageData packageData = PackageDataStorage.read(packageStack).orElseThrow();
        helper.assertTrue(input.isEmpty()
                        && packageData.contents().size() == 2
                        && packageData.layout().orElseThrow().contentSlots().equals(List.of(0, 2)),
                "The advanced wood, blank, wood column must become a package with sparse row metadata");
        AEItemKey packageKey = AEItemKey.of(packageStack);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(part.getMainNode().isOnline() && part.getMainNode().getGrid() != null,
                            "Package Unpacking Bus must join its powered grid");
                    MEStorage network = part.getMainNode().getGrid().getStorageService().getInventory();
                    helper.assertTrue(network.insert(
                                    packageKey,
                                    1,
                                    Actionable.SIMULATE,
                                    IActionSource.ofMachine(part))
                                    == 1,
                            "The sequence endpoint should accept the held package atomically");
                })
                .thenExecute(() -> {
                    MEStorage network = part.getMainNode().getGrid().getStorageService().getInventory();
                    helper.assertTrue(network.insert(
                                    packageKey,
                                    1,
                                    Actionable.MODULATE,
                                    IActionSource.ofMachine(part))
                                    == 1,
                            "Network insertion should start unpacking work");
                })
                .thenExecuteAfter(PackageUnpackingBusPart.ANIMATION_CYCLE_TICKS + 2, () -> {
                    List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(endpoint);
                    helper.assertTrue(part.heldPackage().isEmpty(),
                            "Successful sequence input must consume the held package");
                    helper.assertTrue(members.get(0).storedKey().equals(AEItemKey.of(Items.OAK_PLANKS)),
                            "Advanced pattern row 1 should enter sequence member 1");
                    helper.assertTrue(members.get(1).isEmpty(),
                            "Advanced pattern mode must preserve the empty second row");
                    helper.assertTrue(members.get(2).storedKey().equals(AEItemKey.of(Items.OAK_PLANKS)),
                            "Advanced pattern row 3 should enter sequence member 3");
                })
                .thenSucceed();
    }

    @GameTest(template = "sequence_buffer_empty", timeoutTicks = 180)
    @SuppressWarnings("unchecked")
    public static void unpackingBusKeepsPackageWhenSequenceCommitBecomesBlocked(GameTestHelper helper) {
        BlockPos endpointPos = new BlockPos(1, 1, 0);
        List<SequenceBufferBlockEntity> blocks = formEastLineAt(helper, endpointPos, 4);
        SequenceBufferBlockEntity endpoint = blocks.get(0);
        BlockPos partPos = new BlockPos(1, 1, 1);
        helper.getLevel().setBlock(
                helper.absolutePos(partPos.relative(Direction.SOUTH)),
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
                Direction.NORTH,
                null,
                partItem);
        helper.assertTrue(part != null, "Atomic-failure test requires a Package Unpacking Bus");

        PackageData packageData = PackageData.create(
                PackageColor.CYAN,
                List.of(
                        new GenericStack(AEItemKey.of(Items.IRON_INGOT), 8),
                        new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 4)),
                Optional.of(new PackageLayout(4, List.of(0, 2))),
                Optional.empty(),
                0);
        ItemStack packageStack = new ItemStack(APItems.packageItems().get(PackageColor.CYAN).get());
        PackageDataStorage.write(packageStack, packageData);
        AEItemKey packageKey = AEItemKey.of(packageStack);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        part.getMainNode().isOnline() && part.getMainNode().getGrid() != null,
                        "Atomic-failure Package Unpacking Bus must join its powered grid"))
                .thenExecute(() -> {
                    MEStorage network = part.getMainNode().getGrid().getStorageService().getInventory();
                    helper.assertTrue(network.insert(
                                            packageKey,
                                            1,
                                            Actionable.MODULATE,
                                            IActionSource.ofMachine(part))
                                    == 1,
                            "Initially empty sequence members should accept the package work item");
                    SequenceBufferBlockEntity firstMember = SequenceBufferTopology.members(endpoint).get(0);
                    MEStorage memberStorage = firstMember.getCapability(Capabilities.STORAGE)
                            .orElseThrow(IllegalStateException::new);
                    helper.assertTrue(memberStorage.insert(
                                            AEItemKey.of(Items.DIAMOND),
                                            1,
                                            Actionable.MODULATE,
                                            IActionSource.empty())
                                    == 1,
                            "A competing insertion should occupy a target member before unpack commit");
                })
                .thenExecuteAfter(PackageUnpackingBusPart.ANIMATION_CYCLE_TICKS + 2, () -> {
                    List<SequenceBufferBlockEntity> members = SequenceBufferTopology.members(endpoint);
                    helper.assertTrue(!part.heldPackage().isEmpty()
                                    && part.unpackBlocked()
                                    && !part.isWorking(),
                            "Failed sequence commit must retain the held package in blocked retry state");
                    helper.assertTrue(members.get(0).storedKey().equals(AEItemKey.of(Items.DIAMOND))
                                    && members.get(0).storedAmount() == 1
                                    && members.get(1).isEmpty()
                                    && members.get(2).isEmpty(),
                            "Failed package commit must not partially insert any package contents");
                })
                .thenSucceed();
    }

    private static List<SequenceBufferBlockEntity> formEastLine(GameTestHelper helper, int physicalLength) {
        return formEastLineAt(helper, new BlockPos(1, 1, 1), physicalLength);
    }

    private static List<SequenceBufferBlockEntity> formEastLineAt(
            GameTestHelper helper,
            BlockPos endpointRelative,
            int physicalLength) {
        List<SequenceBufferBlockEntity> blocks = new java.util.ArrayList<>();
        for (int offset = 0; offset < physicalLength; offset++) {
            BlockPos relative = endpointRelative.east(offset);
            var state = APBlocks.SEQUENCE_BUFFER.get().defaultBlockState();
            if (offset == 0) {
                state = state
                        .setValue(SequenceBufferBlock.STATE, SequenceBufferVisualState.UNFORMED_DIRECTED)
                        .setValue(SequenceBufferBlock.DIRECTIONAL, true)
                        .setValue(SequenceBufferBlock.FACING, Direction.EAST);
            }
            blocks.add(placeBuffer(helper, relative, state));
        }
        helper.assertTrue(
                SequenceBufferTopology.tryForm(
                        helper.getLevel(),
                        helper.absolutePos(endpointRelative),
                        Direction.EAST),
                "Sequence buffer line should form");
        return List.copyOf(blocks);
    }

    private static SequenceBufferBlockEntity placeBuffer(
            GameTestHelper helper,
            BlockPos relativePos,
            net.minecraft.world.level.block.state.BlockState state) {
        helper.getLevel().setBlock(helper.absolutePos(relativePos), state, 3);
        return (SequenceBufferBlockEntity) helper.getBlockEntity(relativePos);
    }

    private static InteractionResult wrenchBuffer(
            GameTestHelper helper,
            FakePlayer player,
            BlockPos relativePos,
            Direction clickedSide) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        BlockState state = helper.getLevel().getBlockState(absolutePos);
        Vec3 hitLocation = Vec3.atCenterOf(absolutePos).add(
                clickedSide.getStepX() * 0.5,
                clickedSide.getStepY() * 0.5,
                clickedSide.getStepZ() * 0.5);
        return ((SequenceBufferBlock) state.getBlock()).use(
                state,
                helper.getLevel(),
                absolutePos,
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(hitLocation, clickedSide, absolutePos, false));
    }
}

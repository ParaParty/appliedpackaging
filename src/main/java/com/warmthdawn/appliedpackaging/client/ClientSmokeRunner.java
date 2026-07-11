package com.warmthdawn.appliedpackaging.client;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.util.AEColor;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.client.screen.MePackagerScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackageAssemblerScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackageBusScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackagePatternTerminalScreen;
import com.warmthdawn.appliedpackaging.client.screen.AdvancedPatternEncodingTermScreen;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.part.PackagePatternTerminalPart;
import com.warmthdawn.appliedpackaging.part.AdvancedPatternEncodingTerminalPart;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternMenuBridge;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternLogicBridge;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternScreenBridge;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.world.block.AbstractHorizontalMachineBlock;
import com.warmthdawn.appliedpackaging.world.block.MePackagerBlock;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.AbstractPackageBusBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.terminal.PackagePatternTerminalBlockEntity;
import com.warmthdawn.appliedpackaging.world.entity.PackageEntity;
import com.warmthdawn.appliedpackaging.world.menu.PackagePatternTerminalMenu;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.network.NetworkHooks;
import org.lwjgl.glfw.GLFW;

public final class ClientSmokeRunner {
    private static final String ENABLED_PROPERTY = "appliedpackaging.clientSmoke.enabled";
    private static final String QUIT_PROPERTY = "appliedpackaging.clientSmoke.quit";
    private static final String WORLD_PROPERTY = "appliedpackaging.clientSmoke.world";
    private static final int WORLD_READY_TICKS = 40;
    private static final int WORLD_SCREENSHOT_READY_TICKS = 20;
    private static final int SCREEN_READY_TICKS = 8;
    private static final int SCREEN_TIMEOUT_TICKS = 200;
    private static final int QUIT_DELAY_TICKS = 20;
    private static final int ME_PACKAGER_SMOKE_ANIMATION_TICKS = 8;
    private static final String WORLD_PACKAGER_SCREENSHOT_NAME = "appliedpackaging-client-smoke-world-me_packager.png";
    private static final String WORLD_PACKAGER_LINK_SCREENSHOT_NAME =
            "appliedpackaging-client-smoke-world-me_packager_link.png";
    private static final String WORLD_ALL_MACHINES_SCREENSHOT_NAME =
            "appliedpackaging-client-smoke-world-all_machines.png";
    private static final Field ME_PACKAGER_ANIMATION_TICKS =
            mePackagerField("animationTicks");
    private static final Field ME_PACKAGER_ANIMATION_INWARD =
            mePackagerField("animationInward");
    private static final Field ME_PACKAGER_RENDERED_BOX =
            mePackagerField("renderedBox");
    private static final Method ADVANCED_PATTERN_OPEN_COLUMN_EDITOR =
            advancedPatternScreenMethod("openColumnEditor", int.class);
    private static final Method MOUSE_HANDLER_ON_MOVE =
            mouseHandlerMethod("onMove", long.class, double.class, double.class);
    private static final PackageColor[] WORLD_SMOKE_PACKAGE_COLORS = {
            PackageColor.FLUIX,
            PackageColor.BLUE,
            PackageColor.RED,
            PackageColor.GREEN,
            PackageColor.YELLOW
    };
    private static final SmokeStep[] STEPS = {
            SmokeStep.block("package_assembler", APBlocks.PACKAGE_ASSEMBLER, PackageAssemblerScreen.class),
            SmokeStep.block("me_packager", APBlocks.ME_PACKAGER, MePackagerScreen.class),
            SmokeStep.ae2PatternEncodingTerminal("ae2_pattern_encoding_terminal", PatternEncodingTermScreen.class),
            SmokeStep.advancedPatternEncodingTerminal(
                    "advanced_pattern_encoding_terminal",
                    AdvancedPatternEncodingTermScreen.class),
            SmokeStep.part("package_pattern_terminal", PackagePatternTerminalScreen.class),
            SmokeStep.block("package_storage_bus", APBlocks.PACKAGE_STORAGE_BUS, PackageBusScreen.class),
            SmokeStep.block("package_export_bus", APBlocks.PACKAGE_EXPORT_BUS, PackageBusScreen.class),
            SmokeStep.block("package_unpacking_bus", APBlocks.PACKAGE_UNPACKING_BUS, PackageBusScreen.class)
    };

    private static final ClientSmokeRunner INSTANCE = new ClientSmokeRunner();

    private final String worldName = System.getProperty(WORLD_PROPERTY, "New World");
    private final boolean quitWhenDone = Boolean.parseBoolean(System.getProperty(QUIT_PROPERTY, "true"));
    private int ticksInWorld;
    private int currentStep = -1;
    private int screenTicks;
    private int worldScreenshotTicks;
    private int worldScreenshotIndex;
    private int finishTicks;
    private boolean screenPrepared;
    private boolean screenCaptureRequested;
    private boolean packageSettingsCapturePending;
    private boolean advancedEditorCapturePending;
    private volatile boolean screenshotPending;
    private volatile String pendingScreenCaptureId;
    private boolean setupRequested;
    private volatile boolean setupComplete;
    private volatile RuntimeException setupFailure;
    private boolean packagerLinkCameraRequested;
    private boolean allMachinesCameraRequested;
    private BlockPos basePos;
    private State state = State.WAITING_FOR_WORLD;

    private ClientSmokeRunner() {
    }

    public static void register() {
        if (Boolean.getBoolean(ENABLED_PROPERTY)) {
            AppliedPackaging.LOGGER.info(
                    "Applied Packaging client smoke enabled; waiting for quick-play world '{}'",
                    System.getProperty(WORLD_PROPERTY, "New World"));
            MinecraftForge.EVENT_BUS.addListener(INSTANCE::clientTick);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, INSTANCE::screenRendered);
        }
    }

    private void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        try {
            tick(minecraft);
        } catch (RuntimeException exception) {
            AppliedPackaging.LOGGER.error("Applied Packaging client smoke failed", exception);
            throw exception;
        }
    }

    private void tick(Minecraft minecraft) {
        if (state == State.DONE) {
            tickDone(minecraft);
            return;
        }
        if (minecraft.level == null || minecraft.player == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        if (setupFailure != null) {
            throw setupFailure;
        }
        ticksInWorld++;
        if (ticksInWorld < WORLD_READY_TICKS) {
            return;
        }
        if (!setupRequested) {
            setupRequested = true;
            requestSetup(minecraft);
            return;
        }
        if (!setupComplete) {
            return;
        }
        if (state == State.WAITING_FOR_WORLD) {
            tickWorldScreenshot(minecraft);
            return;
        }
        if (state == State.WAITING_BETWEEN_SCREENS) {
            openNextScreen(minecraft);
            return;
        }
        if (state == State.WAITING_FOR_SCREEN) {
            tickScreen(minecraft);
        }
    }

    private void requestSetup(Minecraft minecraft) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(minecraft.player.getUUID());
        if (serverPlayer == null) {
            setupRequested = false;
            return;
        }
        BlockPos playerPos = serverPlayer.blockPosition();
        basePos = playerPos.offset(2, 0, 0);
        server.execute(() -> {
            try {
                ServerLevel level = serverPlayer.serverLevel();
                level.setDayTime(1000L);
                level.setWeatherParameters(0, 0, false, false);
                serverPlayer.getInventory().clearContent();
                prepareSmokeArea(level);
                for (int index = 0; index < STEPS.length; index++) {
                    SmokeStep step = STEPS[index];
                    BlockPos pos = posForStep(index);
                    if (step.isAdvancedPatternEncodingTerminalPart()) {
                        AdvancedPatternEncodingTerminalPart part = PartHelper.setPart(
                                level,
                                pos,
                                Direction.NORTH,
                                serverPlayer,
                                advancedPatternEncodingTerminalPartItem());
                        if (part == null) {
                            throw new IllegalStateException(
                                    "Could not place advanced pattern encoding terminal part at " + pos);
                        }
                        part.getAdvancedPatternState().setActiveColumns(3);
                        part.getAdvancedPatternState().setColor(0, PackageColor.RED);
                        part.getAdvancedPatternState().setColor(1, PackageColor.BLUE);
                        part.getAdvancedPatternState().setColor(2, PackageColor.GREEN);
                        part.getAdvancedPatternState().inputs().setStack(
                                0,
                                new GenericStack(AEItemKey.of(Items.IRON_INGOT), 32));
                        part.getAdvancedPatternState().inputs().setStack(
                                AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE,
                                new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 16));
                        part.getAdvancedPatternState().inputs().setStack(
                                AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE * 2,
                                new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 8));
                        ItemStack packageOutput = smokePackage(PackageColor.FLUIX, true);
                        part.getLogic().getEncodedOutputInv().setStack(
                                0,
                                new GenericStack(AEItemKey.of(packageOutput), 1));
                        part.getLogic().getBlankPatternInv().setItemDirect(0, AEItems.BLANK_PATTERN.stack());
                    } else if (step.isAe2PatternEncodingTerminalPart()) {
                        PatternEncodingTerminalPart part = PartHelper.setPart(
                                level,
                                pos,
                                Direction.NORTH,
                                serverPlayer,
                                ae2PatternEncodingTerminalPartItem());
                        if (part == null) {
                            throw new IllegalStateException("Could not place AE2 pattern encoding terminal part at " + pos);
                        }
                        PackageCraftingPatternLogicBridge packageLogic =
                                (PackageCraftingPatternLogicBridge) part.getLogic();
                        packageLogic.appliedpackaging$setPackageCraftingMode(true);
                        packageLogic.appliedpackaging$setPackageCraftingColor(PackageColor.BLUE);
                        packageLogic.appliedpackaging$getPackageCraftingMarkerInv()
                                .setItemDirect(0, new ItemStack(Items.DIAMOND));
                        part.getLogic().getEncodedInputInv().setStack(
                                0,
                                new GenericStack(AEItemKey.of(Items.OAK_LOG), 4));
                    } else if (step.isPart()) {
                        PackagePatternTerminalPart part = PartHelper.setPart(
                                level,
                                pos,
                                Direction.NORTH,
                                serverPlayer,
                                packagePatternTerminalPartItem());
                        if (part == null) {
                            throw new IllegalStateException("Could not place package pattern terminal part at " + pos);
                        }
                        part.setSelectedColor(PackageColor.BLUE);
                        part.setInputSlotColor(0, PackageColor.RED);
                        part.getItems().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 32));
                        part.getItems().setStackInSlot(
                                PackagePatternTerminalBlockEntity.SLOT_BLANK_PATTERN,
                                AEItems.BLANK_PATTERN.stack());
                        part.setProcessingOutputFromGhostStack(0, new ItemStack(Items.DIAMOND, 64), false);
                        part.adjustProcessingOutputAmount(0, true);
                    } else {
                        var state = step.block().defaultBlockState()
                                .setValue(AbstractHorizontalMachineBlock.FACING, Direction.NORTH);
                        if (step.id().equals("me_packager")) {
                            state = state.setValue(MePackagerBlock.NETWORK_SIDE, Direction.SOUTH);
                        }
                        level.setBlock(pos, state, 3);
                        if (step.id().equals("me_packager")
                                && level.getBlockEntity(pos) instanceof MePackagerBlockEntity packager) {
                            ItemStack packageStack = smokePackage(PackageColor.FLUIX, true);
                            primeMePackagerSmokeAnimation(packager, packageStack);
                        } else if (level.getBlockEntity(pos) instanceof AbstractPackageBusBlockEntity bus) {
                            bus.setManualFilterColor(PackageColor.RED);
                            bus.setManualFilterMarker(new ItemStack(Items.DIAMOND));
                            bus.setManualFilterContentFromGhostStack(0, new ItemStack(Items.WATER_BUCKET), false);
                            bus.adjustManualFilterContentAmount(0, true);
                        }
                    }
                }
                PartHelper.setPart(
                        level,
                        posForStep(1).relative(Direction.SOUTH),
                        null,
                        null,
                        AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
                for (int index = 0; index < WORLD_SMOKE_PACKAGE_COLORS.length; index++) {
                    PackageColor color = WORLD_SMOKE_PACKAGE_COLORS[index];
                    ItemStack droppedPackage = smokePackage(color, index == 0);
                    BlockPos packagePos = posForStep(1)
                            .relative(Direction.SOUTH, 2)
                            .relative(Direction.WEST, 2 - index);
                    PackageEntity packageEntity = new PackageEntity(
                            level,
                            packagePos.getX() + 0.5D,
                            packagePos.getY(),
                            packagePos.getZ() + 0.5D,
                            droppedPackage);
                    packageEntity.setYRot(index == 0 ? 180.0F : 35.0F + index * 25.0F);
                    level.addFreshEntity(packageEntity);
                }
                setupComplete = true;
                AppliedPackaging.LOGGER.info(
                        "Applied Packaging client smoke placed {} test targets and {} package entities near {} in '{}'",
                        STEPS.length,
                        WORLD_SMOKE_PACKAGE_COLORS.length,
                        basePos,
                        worldName);
            } catch (RuntimeException exception) {
                setupFailure = exception;
            }
        });
    }

    private void prepareSmokeArea(ServerLevel level) {
        BlockPos origin = basePos.offset(-3, -1, -8);
        BlockPos end = basePos.offset(STEPS.length + 3, 4, 5);
        level.getEntitiesOfClass(PackageEntity.class, new AABB(origin, end)).forEach(PackageEntity::discard);
        for (int x = -3; x <= STEPS.length + 3; x++) {
            for (int z = -8; z <= 5; z++) {
                level.setBlock(basePos.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                for (int y = 0; y <= 4; y++) {
                    level.setBlock(basePos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private void requestAllMachinesCamera(Minecraft minecraft) {
        if (allMachinesCameraRequested || minecraft.player == null || basePos == null) {
            return;
        }
        MinecraftServer server = minecraft.getSingleplayerServer();
        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(minecraft.player.getUUID());
        if (serverPlayer == null) {
            return;
        }
        allMachinesCameraRequested = true;
        double cameraX = basePos.getX() + (STEPS.length - 1) / 2.0D + 0.5D;
        double cameraY = basePos.getY();
        double cameraZ = basePos.getZ() - 6.5D;
        server.execute(() -> serverPlayer.teleportTo(
                serverPlayer.serverLevel(),
                cameraX,
                cameraY,
                cameraZ,
                serverPlayer.getYRot(),
                serverPlayer.getXRot()));
    }

    private void requestPackagerLinkCamera(Minecraft minecraft) {
        if (packagerLinkCameraRequested || minecraft.player == null || basePos == null) {
            return;
        }
        MinecraftServer server = minecraft.getSingleplayerServer();
        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(minecraft.player.getUUID());
        if (serverPlayer == null) {
            return;
        }
        packagerLinkCameraRequested = true;
        BlockPos target = posForStep(1);
        double cameraX = target.getX() + 0.5D;
        double cameraY = target.getY();
        double cameraZ = target.getZ() + 2.2D;
        server.execute(() -> serverPlayer.teleportTo(
                serverPlayer.serverLevel(),
                cameraX,
                cameraY,
                cameraZ,
                serverPlayer.getYRot(),
                serverPlayer.getXRot()));
    }

    private void tickWorldScreenshot(Minecraft minecraft) {
        minecraft.options.pauseOnLostFocus = false;
        if (minecraft.screen != null) {
            minecraft.setScreen(null);
            worldScreenshotTicks = 0;
            return;
        }
        minecraft.options.hideGui = true;
        String screenshotName;
        switch (worldScreenshotIndex) {
            case 0 -> {
                orientPlayerToMePackager(minecraft);
                screenshotName = WORLD_PACKAGER_SCREENSHOT_NAME;
            }
            case 1 -> {
                orientPlayerToMePackagerLink(minecraft);
                screenshotName = WORLD_PACKAGER_LINK_SCREENSHOT_NAME;
            }
            default -> {
                orientPlayerToAllMachines(minecraft);
                screenshotName = WORLD_ALL_MACHINES_SCREENSHOT_NAME;
            }
        }
        primeClientMePackagerSmokeAnimation(minecraft);
        worldScreenshotTicks++;
        if (worldScreenshotTicks < WORLD_SCREENSHOT_READY_TICKS) {
            return;
        }
        Screenshot.grab(minecraft.gameDirectory, screenshotName, minecraft.getMainRenderTarget(), message ->
                AppliedPackaging.LOGGER.info(
                        "Applied Packaging client smoke captured {}: {}",
                        screenshotName,
                        message.getString()));
        worldScreenshotIndex++;
        worldScreenshotTicks = 0;
        if (worldScreenshotIndex == 1) {
            requestPackagerLinkCamera(minecraft);
            return;
        }
        if (worldScreenshotIndex == 2) {
            requestAllMachinesCamera(minecraft);
            return;
        }
        minecraft.options.hideGui = false;
        state = State.WAITING_BETWEEN_SCREENS;
    }

    private void primeClientMePackagerSmokeAnimation(Minecraft minecraft) {
        if (minecraft.level == null || basePos == null) {
            return;
        }
        BlockEntity blockEntity = minecraft.level.getBlockEntity(posForStep(1));
        if (!(blockEntity instanceof MePackagerBlockEntity packager)) {
            return;
        }
        ItemStack packageStack = new ItemStack(APItems.packageItems().get(PackageColor.FLUIX).get());
        primeMePackagerSmokeAnimation(packager, packageStack);
    }

    private void orientPlayerToMePackager(Minecraft minecraft) {
        if (minecraft.player == null || basePos == null) {
            return;
        }
        BlockPos target = posForStep(1);
        orientPlayerTo(minecraft, target.getX() + 0.5D, target.getY() + 0.55D, target.getZ() + 0.5D);
    }

    private void orientPlayerToMePackagerLink(Minecraft minecraft) {
        if (minecraft.player == null || basePos == null) {
            return;
        }
        BlockPos target = posForStep(1);
        orientPlayerTo(minecraft, target.getX() + 0.5D, target.getY() + 0.75D, target.getZ() + 0.5D);
    }

    private void orientPlayerToAllMachines(Minecraft minecraft) {
        if (minecraft.player == null || basePos == null) {
            return;
        }
        double targetX = basePos.getX() + (STEPS.length - 1) / 2.0D + 0.5D;
        double targetY = basePos.getY() + 0.75D;
        double targetZ = basePos.getZ() + 0.5D;
        orientPlayerTo(minecraft, targetX, targetY, targetZ);
    }

    private void orientPlayerTo(Minecraft minecraft, double targetX, double targetY, double targetZ) {
        double dx = targetX - minecraft.player.getX();
        double dy = targetY - minecraft.player.getEyeY();
        double dz = targetZ - minecraft.player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, horizontal) * Mth.RAD_TO_DEG);
        minecraft.player.setYRot(yaw);
        minecraft.player.setXRot(pitch);
        minecraft.player.yRotO = yaw;
        minecraft.player.xRotO = pitch;
    }

    private void openNextScreen(Minecraft minecraft) {
        currentStep++;
        screenTicks = 0;
        screenPrepared = false;
        screenCaptureRequested = false;
        packageSettingsCapturePending = false;
        advancedEditorCapturePending = false;
        screenshotPending = false;
        pendingScreenCaptureId = null;
        if (currentStep >= STEPS.length) {
            state = State.DONE;
            AppliedPackaging.LOGGER.info("Applied Packaging client smoke completed all screen captures");
            return;
        }

        MinecraftServer server = minecraft.getSingleplayerServer();
        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(minecraft.player.getUUID());
        if (serverPlayer == null) {
            throw new IllegalStateException("Client smoke could not find the integrated-server player");
        }
        SmokeStep step = STEPS[currentStep];
        BlockPos pos = posForStep(currentStep);
        state = State.WAITING_FOR_SCREEN;
        server.execute(() -> {
            if (step.isAdvancedPatternEncodingTerminalPart()) {
                AdvancedPatternEncodingTerminalPart part = PartHelper.getPart(
                        advancedPatternEncodingTerminalPartItem(),
                        serverPlayer.serverLevel(),
                        pos,
                        Direction.NORTH);
                if (part == null) {
                    setupFailure = new IllegalStateException(
                            "Expected advanced pattern encoding terminal part at " + pos);
                    return;
                }
                if (!MenuOpener.open(part.getMenuType(serverPlayer), serverPlayer, MenuLocators.forPart(part))) {
                    setupFailure = new IllegalStateException(
                            "Could not open advanced pattern encoding terminal at " + pos);
                }
                return;
            }
            if (step.isAe2PatternEncodingTerminalPart()) {
                PatternEncodingTerminalPart part = PartHelper.getPart(
                        ae2PatternEncodingTerminalPartItem(),
                        serverPlayer.serverLevel(),
                        pos,
                        Direction.NORTH);
                if (part == null) {
                    setupFailure = new IllegalStateException("Expected AE2 pattern encoding terminal part at " + pos);
                    return;
                }
                if (!MenuOpener.open(part.getMenuType(serverPlayer), serverPlayer, MenuLocators.forPart(part))) {
                    setupFailure = new IllegalStateException("Could not open AE2 pattern encoding terminal at " + pos);
                }
                return;
            }
            if (step.isPart()) {
                PackagePatternTerminalPart part = PartHelper.getPart(
                        packagePatternTerminalPartItem(),
                        serverPlayer.serverLevel(),
                        pos,
                        Direction.NORTH);
                if (part == null) {
                    setupFailure = new IllegalStateException("Expected package pattern terminal part at " + pos);
                    return;
                }
                NetworkHooks.openScreen(serverPlayer, part,
                        buffer -> PackagePatternTerminalMenu.writePartHost(buffer, pos, Direction.NORTH));
                return;
            }
            BlockEntity blockEntity = serverPlayer.serverLevel().getBlockEntity(pos);
            if (!(blockEntity instanceof MenuProvider provider)) {
                setupFailure = new IllegalStateException("Expected menu provider for " + step.id() + " at " + pos);
                return;
            }
            if (blockEntity instanceof PackagePatternTerminalBlockEntity) {
                NetworkHooks.openScreen(serverPlayer, provider,
                        buffer -> PackagePatternTerminalMenu.writeBlockHost(buffer, pos));
                return;
            }
            NetworkHooks.openScreen(serverPlayer, provider, pos);
        });
    }

    private void tickScreen(Minecraft minecraft) {
        SmokeStep step = STEPS[currentStep];
        if (minecraft.screen == null || !step.screenClass().isInstance(minecraft.screen)) {
            screenTicks++;
            if (screenTicks > SCREEN_TIMEOUT_TICKS) {
                Screen screen = minecraft.screen;
                String actual = screen == null ? "null" : screen.getClass().getName();
                throw new IllegalStateException(
                        "Timed out waiting for " + step.screenClass().getSimpleName() + ", current screen is " + actual);
            }
            return;
        }

        if (!screenPrepared) {
            prepareOpenedScreen(minecraft.screen, step);
            screenPrepared = true;
            screenTicks = 0;
            return;
        }

        if (step.isAdvancedPatternEncodingTerminalPart()
                && !screenCaptureRequested
                && !advancedEditorCapturePending
                && minecraft.screen instanceof AdvancedPatternEncodingTermScreen advancedScreen) {
            hoverAdvancedInputSlot(advancedScreen);
        }
        if (screenCaptureRequested) {
            screenTicks++;
            if (screenshotPending) {
                if (screenTicks > SCREEN_TIMEOUT_TICKS) {
                    throw new IllegalStateException("Timed out saving client smoke screenshot for " + step.id());
                }
                return;
            }
            screenCaptureRequested = false;
            if (packageSettingsCapturePending
                    && minecraft.screen instanceof PatternEncodingTermScreen<?> patternEncodingScreen) {
                packageSettingsCapturePending = false;
                ((PackageCraftingPatternScreenBridge) patternEncodingScreen)
                        .appliedpackaging$handlePackageSettingsKeyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
                patternEncodingScreen.getMenu().clear();
                closeCurrentScreen(minecraft);
                return;
            }
            if (advancedEditorCapturePending) {
                closeCurrentScreen(minecraft);
                return;
            }
            if (step.isAe2PatternEncodingTerminalPart()
                    && minecraft.screen instanceof PatternEncodingTermScreen<?> patternEncodingScreen) {
                ((PackageCraftingPatternScreenBridge) patternEncodingScreen)
                        .appliedpackaging$openPackageSettings();
                packageSettingsCapturePending = true;
                screenTicks = 0;
                return;
            }
            if (step.isAdvancedPatternEncodingTerminalPart()
                    && minecraft.screen instanceof AdvancedPatternEncodingTermScreen advancedScreen) {
                openAdvancedPatternColumnEditor(advancedScreen);
                advancedEditorCapturePending = true;
                screenTicks = 0;
                return;
            }
            closeCurrentScreen(minecraft);
            return;
        }

        screenTicks++;
        if (screenTicks < SCREEN_READY_TICKS) {
            return;
        }
        String captureId = packageSettingsCapturePending
                ? step.id() + "_settings"
                : advancedEditorCapturePending ? step.id() + "_editor" : step.id();
        requestScreenCapture(captureId);
        screenCaptureRequested = true;
        screenTicks = 0;
    }

    private void requestScreenCapture(String id) {
        if (screenshotPending || pendingScreenCaptureId != null) {
            throw new IllegalStateException("Client smoke requested a second screenshot while one was still pending");
        }
        pendingScreenCaptureId = id;
        screenshotPending = true;
    }

    private void screenRendered(ScreenEvent.Render.Post event) {
        String id = pendingScreenCaptureId;
        if (id == null || event.getScreen() != Minecraft.getInstance().screen) {
            return;
        }
        pendingScreenCaptureId = null;
        String fileName = "appliedpackaging-client-smoke-" + id + ".png";
        try {
            Minecraft minecraft = Minecraft.getInstance();
            event.getGuiGraphics().flush();
            minecraft.renderBuffers().bufferSource().endBatch();
            Screenshot.grab(minecraft.gameDirectory, fileName, minecraft.getMainRenderTarget(), message -> {
                try {
                    AppliedPackaging.LOGGER.info(
                            "Applied Packaging client smoke captured {}: {}",
                            fileName,
                            message.getString());
                } finally {
                    screenshotPending = false;
                }
            });
        } catch (RuntimeException exception) {
            screenshotPending = false;
            throw exception;
        }
    }

    private void closeCurrentScreen(Minecraft minecraft) {
        if (minecraft.player != null) {
            minecraft.player.closeContainer();
        }
        minecraft.setScreen(null);
        state = State.WAITING_BETWEEN_SCREENS;
    }

    private void prepareOpenedScreen(Screen screen, SmokeStep step) {
        if (step.isAe2PatternEncodingTerminalPart()
                && screen instanceof PatternEncodingTermScreen<?> patternEncodingScreen) {
            PackageCraftingPatternMenuBridge bridge =
                    (PackageCraftingPatternMenuBridge) patternEncodingScreen.getMenu();
            bridge.appliedpackaging$setPackageCraftingMode(true);
            bridge.appliedpackaging$setPackageCraftingColor(PackageColor.BLUE);
        }
        if (step.isAdvancedPatternEncodingTerminalPart()
                && screen instanceof AdvancedPatternEncodingTermScreen advancedScreen) {
            advancedScreen.getMenu().encode();
            hoverAdvancedInputSlot(advancedScreen);
        }
    }

    private static void hoverAdvancedInputSlot(AdvancedPatternEncodingTermScreen screen) {
        Slot[] slots = screen.getMenu().getAdvancedInputSlots();
        if (slots.length == 0) {
            return;
        }
        Slot slot = slots[Math.min(1, slots.length - 1)];
        hoverSlot(screen.getGuiLeft(), screen.getGuiTop(), slot);
    }

    private static void hoverSlot(int guiLeft, int guiTop, Slot slot) {
        Minecraft minecraft = Minecraft.getInstance();
        double scale = minecraft.getWindow().getGuiScale();
        double targetX = (guiLeft + slot.x + 8) * scale;
        double targetY = (guiTop + slot.y + 8) * scale;
        GLFW.glfwSetCursorPos(
                minecraft.getWindow().getWindow(),
                targetX,
                targetY);
        try {
            MOUSE_HANDLER_ON_MOVE.invoke(
                    minecraft.mouseHandler,
                    minecraft.getWindow().getWindow(),
                    targetX,
                    targetY);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not move the client smoke cursor", exception);
        }
    }

    private void tickDone(Minecraft minecraft) {
        finishTicks++;
        if (finishTicks < QUIT_DELAY_TICKS) {
            return;
        }
        if (quitWhenDone) {
            AppliedPackaging.LOGGER.info("Applied Packaging client smoke stopping client");
            minecraft.stop();
        }
    }

    private static void primeMePackagerSmokeAnimation(MePackagerBlockEntity packager, ItemStack packageStack) {
        ItemStack renderedBox = packageStack.copy();
        renderedBox.setCount(1);
        packager.getItems().setStackInSlot(MePackagerBlockEntity.SLOT_INPUT, renderedBox.copy());
        packager.getItems().setStackInSlot(MePackagerBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        try {
            ME_PACKAGER_ANIMATION_TICKS.setInt(packager, ME_PACKAGER_SMOKE_ANIMATION_TICKS);
            ME_PACKAGER_ANIMATION_INWARD.setBoolean(packager, true);
            ME_PACKAGER_RENDERED_BOX.set(packager, renderedBox);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not prime ME Packager smoke animation", exception);
        }
    }

    private static ItemStack smokePackage(PackageColor color, boolean marked) {
        ItemStack stack = new ItemStack(APItems.packageItems().get(color).get());
        Optional<MarkerSpec> marker = marked
                ? Optional.of(new MarkerSpec(new GenericStack(AEItemKey.of(Items.DIAMOND), 1)))
                : Optional.empty();
        PackageDataStorage.write(stack, PackageData.create(
                color,
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 64)),
                marker,
                0));
        return stack;
    }

    private static Field mePackagerField(String name) {
        try {
            Field field = MePackagerBlockEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("ME Packager smoke animation field is missing: " + name, exception);
        }
    }

    private static Method advancedPatternScreenMethod(String name, Class<?>... parameterTypes) {
        try {
            Method method = AdvancedPatternEncodingTermScreen.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Advanced Pattern Terminal smoke method is missing: " + name, exception);
        }
    }

    private static Method mouseHandlerMethod(String name, Class<?>... parameterTypes) {
        try {
            Method method = MouseHandler.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Client smoke mouse handler method is missing: " + name, exception);
        }
    }

    private static void openAdvancedPatternColumnEditor(AdvancedPatternEncodingTermScreen screen) {
        try {
            ADVANCED_PATTERN_OPEN_COLUMN_EDITOR.invoke(screen, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not open Advanced Pattern Terminal column editor", exception);
        }
    }

    private BlockPos posForStep(int index) {
        return basePos.offset(index, 0, 0);
    }

    @SuppressWarnings("unchecked")
    private static IPartItem<PackagePatternTerminalPart> packagePatternTerminalPartItem() {
        return (IPartItem<PackagePatternTerminalPart>) APItems.PACKAGE_PATTERN_TERMINAL.get();
    }

    @SuppressWarnings("unchecked")
    private static IPartItem<PatternEncodingTerminalPart> ae2PatternEncodingTerminalPartItem() {
        return (IPartItem<PatternEncodingTerminalPart>) AEParts.PATTERN_ENCODING_TERMINAL.asItem();
    }

    @SuppressWarnings("unchecked")
    private static IPartItem<AdvancedPatternEncodingTerminalPart> advancedPatternEncodingTerminalPartItem() {
        return (IPartItem<AdvancedPatternEncodingTerminalPart>) APItems.ADVANCED_PATTERN_ENCODING_TERMINAL.get();
    }

    private enum State {
        WAITING_FOR_WORLD,
        WAITING_FOR_SCREEN,
        WAITING_BETWEEN_SCREENS,
        DONE
    }

    private record SmokeStep(
            String id,
            Supplier<? extends Block> blockSupplier,
            Class<? extends Screen> screenClass,
            boolean isPart,
            boolean isAe2PatternEncodingTerminalPart,
            boolean isAdvancedPatternEncodingTerminalPart) {
        private static SmokeStep block(
                String id,
                Supplier<? extends Block> blockSupplier,
                Class<? extends Screen> screenClass) {
            return new SmokeStep(id, blockSupplier, screenClass, false, false, false);
        }

        private static SmokeStep part(String id, Class<? extends Screen> screenClass) {
            return new SmokeStep(id, null, screenClass, true, false, false);
        }

        private static SmokeStep ae2PatternEncodingTerminal(String id, Class<? extends Screen> screenClass) {
            return new SmokeStep(id, null, screenClass, true, true, false);
        }

        private static SmokeStep advancedPatternEncodingTerminal(String id, Class<? extends Screen> screenClass) {
            return new SmokeStep(id, null, screenClass, true, false, true);
        }

        private Block block() {
            return blockSupplier.get();
        }
    }
}

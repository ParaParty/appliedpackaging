package com.warmthdawn.appliedpackaging.client;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.client.screen.MePackagerScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackageAssemblerScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackageBusScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackagePatternTerminalScreen;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.world.block.AbstractHorizontalMachineBlock;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.NetworkHooks;

public final class ClientSmokeRunner {
    private static final String ENABLED_PROPERTY = "appliedpackaging.clientSmoke.enabled";
    private static final String QUIT_PROPERTY = "appliedpackaging.clientSmoke.quit";
    private static final String WORLD_PROPERTY = "appliedpackaging.clientSmoke.world";
    private static final int WORLD_READY_TICKS = 40;
    private static final int SCREEN_READY_TICKS = 8;
    private static final int SCREEN_TIMEOUT_TICKS = 200;
    private static final int QUIT_DELAY_TICKS = 20;
    private static final SmokeStep[] STEPS = {
            new SmokeStep("package_assembler", APBlocks.PACKAGE_ASSEMBLER, PackageAssemblerScreen.class),
            new SmokeStep("me_packager", APBlocks.ME_PACKAGER, MePackagerScreen.class),
            new SmokeStep("package_pattern_terminal", APBlocks.PACKAGE_PATTERN_TERMINAL, PackagePatternTerminalScreen.class),
            new SmokeStep("package_storage_bus", APBlocks.PACKAGE_STORAGE_BUS, PackageBusScreen.class)
    };

    private static final ClientSmokeRunner INSTANCE = new ClientSmokeRunner();

    private final String worldName = System.getProperty(WORLD_PROPERTY, "New World");
    private final boolean quitWhenDone = Boolean.parseBoolean(System.getProperty(QUIT_PROPERTY, "true"));
    private int ticksInWorld;
    private int currentStep = -1;
    private int screenTicks;
    private int finishTicks;
    private boolean setupRequested;
    private volatile boolean setupComplete;
    private volatile RuntimeException setupFailure;
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
        if (state == State.WAITING_FOR_WORLD || state == State.WAITING_BETWEEN_SCREENS) {
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
                for (int index = 0; index < STEPS.length; index++) {
                    SmokeStep step = STEPS[index];
                    BlockPos pos = posForStep(index);
                    level.setBlock(pos, step.block().defaultBlockState()
                            .setValue(AbstractHorizontalMachineBlock.FACING, Direction.NORTH), 3);
                }
                setupComplete = true;
                AppliedPackaging.LOGGER.info(
                        "Applied Packaging client smoke placed {} test blocks near {} in '{}'",
                        STEPS.length,
                        basePos,
                        worldName);
            } catch (RuntimeException exception) {
                setupFailure = exception;
            }
        });
    }

    private void openNextScreen(Minecraft minecraft) {
        currentStep++;
        screenTicks = 0;
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
            BlockEntity blockEntity = serverPlayer.serverLevel().getBlockEntity(pos);
            if (!(blockEntity instanceof MenuProvider provider)) {
                setupFailure = new IllegalStateException("Expected menu provider for " + step.id() + " at " + pos);
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

        screenTicks++;
        if (screenTicks < SCREEN_READY_TICKS) {
            return;
        }
        String fileName = "appliedpackaging-client-smoke-" + step.id() + ".png";
        Screenshot.grab(minecraft.gameDirectory, fileName, minecraft.getMainRenderTarget(), message ->
                AppliedPackaging.LOGGER.info(
                        "Applied Packaging client smoke captured {}: {}",
                        fileName,
                        message.getString()));
        if (minecraft.player != null) {
            minecraft.player.closeContainer();
        }
        minecraft.setScreen(null);
        state = State.WAITING_BETWEEN_SCREENS;
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

    private BlockPos posForStep(int index) {
        return basePos.offset(index, 0, 0);
    }

    private enum State {
        WAITING_FOR_WORLD,
        WAITING_FOR_SCREEN,
        WAITING_BETWEEN_SCREENS,
        DONE
    }

    private record SmokeStep(String id, Supplier<? extends Block> blockSupplier, Class<? extends Screen> screenClass) {
        private Block block() {
            return blockSupplier.get();
        }
    }
}

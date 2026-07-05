package com.warmthdawn.appliedpackaging.client;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.client.renderer.MePackagerRenderer;
import com.warmthdawn.appliedpackaging.client.renderer.PackageEntityRenderer;
import com.warmthdawn.appliedpackaging.client.renderer.PackageMarkerRenderer;
import com.warmthdawn.appliedpackaging.client.screen.MePackagerScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackageAssemblerScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackageBusScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackagePatternTerminalScreen;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackagedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackagePatternItem;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APEntityTypes;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class AppliedPackagingClient {
    private static final String CLIENT_SMOKE_ENABLED_PROPERTY = "appliedpackaging.clientSmoke.enabled";
    private static final String CLIENT_SMOKE_RUNNER_CLASS =
            "com.warmthdawn.appliedpackaging.client.ClientSmokeRunner";

    private AppliedPackagingClient() {
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(AppliedPackagingClient::clientSetup);
        eventBus.addListener(AppliedPackagingClient::registerAdditionalModels);
        MinecraftForge.EVENT_BUS.addListener(AppliedPackagingClient::appendTooltip);
        registerClientSmokeRunner();
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(APMenus.ME_PACKAGER.get(), MePackagerScreen::new);
            MenuScreens.register(APMenus.PACKAGE_ASSEMBLER.get(), PackageAssemblerScreen::new);
            MenuScreens.register(APMenus.PACKAGE_PATTERN_TERMINAL.get(), PackagePatternTerminalScreen::new);
            MenuScreens.register(APMenus.PACKAGE_BUS.get(), PackageBusScreen::new);
            ItemBlockRenderTypes.setRenderLayer(APBlocks.ME_PACKAGER.get(), RenderType.cutoutMipped());
            EntityRenderers.register(APEntityTypes.PACKAGE.get(), PackageEntityRenderer::new);
            BlockEntityRenderers.register(APBlockEntities.ME_PACKAGER.get(), MePackagerRenderer::new);
            registerPackageItemProperties();
        });
    }

    private static void registerPackageItemProperties() {
        APItems.packageItems().values().forEach(item -> ItemProperties.register(
                item.get(),
                PackageMarkerRenderer.HAS_MARKER_PROPERTY,
                (stack, level, entity, seed) -> PackageMarkerRenderer.hasItemMarker(stack) ? 1.0F : 0.0F));
    }

    private static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MePackagerRenderer.TRAY_MODEL);
        event.register(MePackagerRenderer.HATCH_CLOSED_MODEL);
        event.register(MePackagerRenderer.HATCH_OPEN_MODEL);
    }

    private static void appendTooltip(ItemTooltipEvent event) {
        if (!PackagePatternDataStorage.isAe2BlankPattern(event.getItemStack())) {
            return;
        }
        if (PackagePatternDataStorage.read(event.getItemStack()).isEmpty()
                && PackagedProcessingPatternDataStorage.read(event.getItemStack()).isEmpty()) {
            return;
        }
        PackagePatternItem.appendPackagePatternTooltip(event.getItemStack(), event.getToolTip(), event.getFlags());
    }

    private static void registerClientSmokeRunner() {
        if (!Boolean.getBoolean(CLIENT_SMOKE_ENABLED_PROPERTY)) {
            return;
        }
        try {
            Class<?> runnerClass = Class.forName(CLIENT_SMOKE_RUNNER_CLASS);
            runnerClass.getMethod("register").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Client smoke runner is not available in this build", exception);
        }
        AppliedPackaging.LOGGER.debug("Applied Packaging client smoke runner loaded for this development run.");
    }
}

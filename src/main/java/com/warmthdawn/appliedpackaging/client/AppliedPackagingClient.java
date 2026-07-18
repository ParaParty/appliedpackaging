package com.warmthdawn.appliedpackaging.client;

import com.warmthdawn.appliedpackaging.client.renderer.MePackagerRenderer;
import com.warmthdawn.appliedpackaging.client.renderer.PackageEntityRenderer;
import com.warmthdawn.appliedpackaging.client.renderer.PackageMarkerRenderer;
import com.warmthdawn.appliedpackaging.client.renderer.PackageAssemblerRenderer;
import com.warmthdawn.appliedpackaging.client.screen.MePackagerScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackageAssemblerScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackageBusScreen;
import com.warmthdawn.appliedpackaging.client.screen.SequenceBufferMainScreen;
import com.warmthdawn.appliedpackaging.client.screen.SequenceBufferSideScreen;
import com.warmthdawn.appliedpackaging.client.screen.AdvancedPatternEncodingTermScreen;
import com.warmthdawn.appliedpackaging.part.AdvancedPatternEncodingTerminalPart;
import com.warmthdawn.appliedpackaging.part.PackageStorageBusPart;
import com.warmthdawn.appliedpackaging.part.PackageUnpackingBusPart;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APEntityTypes;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import appeng.init.client.InitScreens;
import appeng.api.util.AEColor;
import appeng.client.render.StaticItemColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class AppliedPackagingClient {
    private AppliedPackagingClient() {
    }

    public static void register(IEventBus eventBus) {
        AdvancedPatternEncodingTerminalPart.registerModels();
        PackageStorageBusPart.registerModels();
        PackageUnpackingBusPart.registerModels();
        eventBus.addListener(AppliedPackagingClient::clientSetup);
        eventBus.addListener(AppliedPackagingClient::registerAdditionalModels);
        eventBus.addListener(AppliedPackagingClient::registerItemDecorations);
        eventBus.addListener(AppliedPackagingClient::registerItemColors);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Minecraft.getInstance().getMainRenderTarget().enableStencil();
            InitScreens.register(
                    APMenus.ME_PACKAGER.get(),
                    MePackagerScreen::new,
                    "/screens/appliedpackaging/me_packager.json");
            InitScreens.register(
                    APMenus.PACKAGE_ASSEMBLER.get(),
                    PackageAssemblerScreen::new,
                    "/screens/appliedpackaging/package_assembler.json");
            InitScreens.register(
                    APMenus.ADVANCED_PATTERN_ENCODING_TERMINAL.get(),
                    AdvancedPatternEncodingTermScreen::new,
                    "/screens/appliedpackaging/advanced_pattern_encoding_terminal.json");
            InitScreens.register(
                    APMenus.PACKAGE_BUS.get(),
                    PackageBusScreen::new,
                    "/screens/appliedpackaging/package_bus.json");
            InitScreens.register(
                    APMenus.SEQUENCE_BUFFER_MAIN.get(),
                    SequenceBufferMainScreen::new,
                    "/screens/appliedpackaging/sequence_buffer_main.json");
            InitScreens.register(
                    APMenus.SEQUENCE_BUFFER_SIDE.get(),
                    SequenceBufferSideScreen::new,
                    "/screens/appliedpackaging/sequence_buffer_side.json");
            ItemBlockRenderTypes.setRenderLayer(APBlocks.ME_PACKAGER.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(APBlocks.PACKAGE_ASSEMBLER.get(), RenderType.cutout());
            EntityRenderers.register(APEntityTypes.PACKAGE.get(), PackageEntityRenderer::new);
            BlockEntityRenderers.register(APBlockEntities.ME_PACKAGER.get(), MePackagerRenderer::new);
            BlockEntityRenderers.register(
                    APBlockEntities.PACKAGE_ASSEMBLER.get(), PackageAssemblerRenderer::new);
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
        event.register(MePackagerRenderer.BELT_MODEL);
        event.register(MePackagerRenderer.CURTAIN_FLAP_MODEL);
        event.register(PackageAssemblerRenderer.LIGHTS_MODEL);
    }

    private static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        APItems.packageItems().values().forEach(item ->
                event.register(item.get(), PackageMarkerRenderer.SHIFT_MARKER_DECORATOR));
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                new StaticItemColor(AEColor.TRANSPARENT),
                APItems.ADVANCED_PATTERN_ENCODING_TERMINAL.get());
    }

}

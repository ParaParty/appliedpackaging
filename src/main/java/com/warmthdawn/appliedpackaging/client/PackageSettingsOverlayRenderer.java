package com.warmthdawn.appliedpackaging.client;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternScreenBridge;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AppliedPackaging.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PackageSettingsOverlayRenderer {
    private PackageSettingsOverlayRenderer() {
    }

    @SubscribeEvent
    public static void afterScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof PackageCraftingPatternScreenBridge bridge) {
            event.getGuiGraphics().flush();
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
            bridge.appliedpackaging$renderPackageSettingsOverlay(
                    event.getGuiGraphics(),
                    event.getMouseX(),
                    event.getMouseY(),
                    event.getPartialTick());
            event.getGuiGraphics().flush();
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        }
    }
}

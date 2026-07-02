package com.warmthdawn.appliedpackaging.client;

import com.warmthdawn.appliedpackaging.client.screen.MePackagerScreen;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class AppliedPackagingClient {
    private AppliedPackagingClient() {
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(AppliedPackagingClient::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(APMenus.ME_PACKAGER.get(), MePackagerScreen::new));
    }
}

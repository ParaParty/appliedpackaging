package com.warmthdawn.appliedpackaging.client;

import com.warmthdawn.appliedpackaging.client.screen.MePackagerScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackageAssemblerScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackageBusScreen;
import com.warmthdawn.appliedpackaging.client.screen.PackagePatternTerminalScreen;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackagedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackagePatternItem;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class AppliedPackagingClient {
    private AppliedPackagingClient() {
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(AppliedPackagingClient::clientSetup);
        MinecraftForge.EVENT_BUS.addListener(AppliedPackagingClient::appendTooltip);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(APMenus.ME_PACKAGER.get(), MePackagerScreen::new);
            MenuScreens.register(APMenus.PACKAGE_ASSEMBLER.get(), PackageAssemblerScreen::new);
            MenuScreens.register(APMenus.PACKAGE_PATTERN_TERMINAL.get(), PackagePatternTerminalScreen::new);
            MenuScreens.register(APMenus.PACKAGE_BUS.get(), PackageBusScreen::new);
        });
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
}

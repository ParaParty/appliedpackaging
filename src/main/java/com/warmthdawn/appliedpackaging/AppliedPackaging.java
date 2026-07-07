package com.warmthdawn.appliedpackaging;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import com.mojang.logging.LogUtils;
import com.warmthdawn.appliedpackaging.client.AppliedPackagingClient;
import com.warmthdawn.appliedpackaging.part.PackagePatternTerminalPart;
import com.warmthdawn.appliedpackaging.registry.APBlockEntities;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.registry.APCreativeTabs;
import com.warmthdawn.appliedpackaging.registry.APEntityTypes;
import com.warmthdawn.appliedpackaging.registry.APItems;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import com.warmthdawn.appliedpackaging.world.entity.PackageEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(AppliedPackaging.MOD_ID)
public class AppliedPackaging {
    public static final String MOD_ID = "appliedpackaging";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AppliedPackaging() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        PackagePatternTerminalPart.registerModels();

        APBlocks.register(modEventBus);
        APItems.register(modEventBus);
        APEntityTypes.register(modEventBus);
        APBlockEntities.register(modEventBus);
        APMenus.register(modEventBus);
        APCreativeTabs.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerEntityAttributes);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            AppliedPackagingClient.register(modEventBus);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Upgrades.add(AEItems.REDSTONE_CARD, APBlocks.ME_PACKAGER.get(), 1);
            Upgrades.add(AEItems.CAPACITY_CARD, APBlocks.ME_PACKAGER.get(), 3);
            Upgrades.add(AEItems.SPEED_CARD, APBlocks.ME_PACKAGER.get(), 6);
            Upgrades.add(AEItems.INVERTER_CARD, APBlocks.ME_PACKAGER.get(), 1);
            Upgrades.add(AEItems.SPEED_CARD, APBlocks.PACKAGE_ASSEMBLER.get(), 5);
        });
        LOGGER.info("Applied Packaging initialized.");
    }

    private void registerEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(APEntityTypes.PACKAGE.get(), PackageEntity.createPackageAttributes().build());
    }
}

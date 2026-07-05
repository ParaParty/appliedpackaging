package com.warmthdawn.appliedpackaging.registry;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.world.entity.PackageEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class APEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AppliedPackaging.MOD_ID);

    public static final RegistryObject<EntityType<PackageEntity>> PACKAGE = ENTITY_TYPES.register(
            "package",
            () -> EntityType.Builder.<PackageEntity>of(PackageEntity::new, MobCategory.MISC)
                    .sized(PackageEntity.WIDTH, PackageEntity.HEIGHT)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .setShouldReceiveVelocityUpdates(true)
                    .setCustomClientFactory(PackageEntity::spawn)
                    .build(AppliedPackaging.MOD_ID + ":package"));

    private APEntityTypes() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}

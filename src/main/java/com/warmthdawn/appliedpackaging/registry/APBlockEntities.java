package com.warmthdawn.appliedpackaging.registry;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.PackageAssemblerBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.PackageExportBusBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.PackageStorageBusBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.bus.PackageUnpackingBusBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.terminal.PackagePatternTerminalBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class APBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AppliedPackaging.MOD_ID);

    public static final RegistryObject<BlockEntityType<MePackagerBlockEntity>> ME_PACKAGER = BLOCK_ENTITIES.register(
            "me_packager",
            () -> BlockEntityType.Builder.of(MePackagerBlockEntity::new, APBlocks.ME_PACKAGER.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<PackageAssemblerBlockEntity>> PACKAGE_ASSEMBLER = BLOCK_ENTITIES.register(
            "package_assembler",
            () -> BlockEntityType.Builder.of(PackageAssemblerBlockEntity::new, APBlocks.PACKAGE_ASSEMBLER.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<PackageStorageBusBlockEntity>> PACKAGE_STORAGE_BUS = BLOCK_ENTITIES.register(
            "package_storage_bus",
            () -> BlockEntityType.Builder.of(PackageStorageBusBlockEntity::new, APBlocks.PACKAGE_STORAGE_BUS.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<PackageExportBusBlockEntity>> PACKAGE_EXPORT_BUS = BLOCK_ENTITIES.register(
            "package_export_bus",
            () -> BlockEntityType.Builder.of(PackageExportBusBlockEntity::new, APBlocks.PACKAGE_EXPORT_BUS.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<PackageUnpackingBusBlockEntity>> PACKAGE_UNPACKING_BUS = BLOCK_ENTITIES.register(
            "package_unpacking_bus",
            () -> BlockEntityType.Builder.of(PackageUnpackingBusBlockEntity::new, APBlocks.PACKAGE_UNPACKING_BUS.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<PackagePatternTerminalBlockEntity>> PACKAGE_PATTERN_TERMINAL = BLOCK_ENTITIES.register(
            "package_pattern_terminal",
            () -> BlockEntityType.Builder.of(PackagePatternTerminalBlockEntity::new, APBlocks.PACKAGE_PATTERN_TERMINAL.get())
                    .build(null));

    private APBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

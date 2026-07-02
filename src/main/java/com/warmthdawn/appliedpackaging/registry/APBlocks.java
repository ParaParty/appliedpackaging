package com.warmthdawn.appliedpackaging.registry;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.world.block.MePackagerBlock;
import com.warmthdawn.appliedpackaging.world.block.PackageAssemblerBlock;
import com.warmthdawn.appliedpackaging.world.block.PackageBusBlock;
import com.warmthdawn.appliedpackaging.world.block.PackagePatternTerminalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class APBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AppliedPackaging.MOD_ID);

    public static final RegistryObject<Block> ME_PACKAGER = BLOCKS.register(
            "me_packager",
            () -> new MePackagerBlock(machineProperties()));

    public static final RegistryObject<Block> PACKAGE_ASSEMBLER = BLOCKS.register(
            "package_assembler",
            () -> new PackageAssemblerBlock(machineProperties()));

    public static final RegistryObject<Block> PACKAGE_STORAGE_BUS = BLOCKS.register(
            "package_storage_bus",
            () -> new PackageBusBlock(machineProperties(), PackageBusBlock.BusKind.STORAGE));

    public static final RegistryObject<Block> PACKAGE_EXPORT_BUS = BLOCKS.register(
            "package_export_bus",
            () -> new PackageBusBlock(machineProperties(), PackageBusBlock.BusKind.EXPORT));

    public static final RegistryObject<Block> PACKAGE_UNPACKING_BUS = BLOCKS.register(
            "package_unpacking_bus",
            () -> new PackageBusBlock(machineProperties(), PackageBusBlock.BusKind.UNPACKING));

    public static final RegistryObject<Block> PACKAGE_PATTERN_TERMINAL = BLOCKS.register(
            "package_pattern_terminal",
            () -> new PackagePatternTerminalBlock(machineProperties()));

    private APBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }
}

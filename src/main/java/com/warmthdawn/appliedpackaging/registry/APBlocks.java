package com.warmthdawn.appliedpackaging.registry;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.world.block.MePackagerBlock;
import com.warmthdawn.appliedpackaging.world.block.PackageAssemblerBlock;
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
            () -> new MePackagerBlock(cutoutMachineProperties()));

    public static final RegistryObject<Block> PACKAGE_ASSEMBLER = BLOCKS.register(
            "package_assembler",
            () -> new PackageAssemblerBlock(cutoutMachineProperties()));

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

    private static BlockBehaviour.Properties cutoutMachineProperties() {
        return machineProperties()
                .noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false);
    }

}

package com.warmthdawn.appliedpackaging.registry;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.world.menu.MePackagerMenu;
import com.warmthdawn.appliedpackaging.world.menu.PackageAssemblerMenu;
import com.warmthdawn.appliedpackaging.world.menu.PackageBusMenu;
import com.warmthdawn.appliedpackaging.world.menu.SequenceBufferMainMenu;
import com.warmthdawn.appliedpackaging.world.menu.SequenceBufferSideMenu;
import com.warmthdawn.appliedpackaging.world.menu.AdvancedPatternEncodingTermMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class APMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, AppliedPackaging.MOD_ID);

    public static final RegistryObject<MenuType<MePackagerMenu>> ME_PACKAGER = MENUS.register(
            "me_packager",
            () -> IForgeMenuType.create(MePackagerMenu::new));

    public static final RegistryObject<MenuType<PackageAssemblerMenu>> PACKAGE_ASSEMBLER = MENUS.register(
            "package_assembler",
            () -> IForgeMenuType.create(PackageAssemblerMenu::new));

    public static final RegistryObject<MenuType<PackageBusMenu>> PACKAGE_BUS = MENUS.register(
            "package_bus",
            () -> IForgeMenuType.create(PackageBusMenu::fromNetwork));

    public static final RegistryObject<MenuType<SequenceBufferMainMenu>> SEQUENCE_BUFFER_MAIN = MENUS.register(
            "sequence_buffer_main",
            () -> IForgeMenuType.create(SequenceBufferMainMenu::new));

    public static final RegistryObject<MenuType<SequenceBufferSideMenu>> SEQUENCE_BUFFER_SIDE = MENUS.register(
            "sequence_buffer_side",
            () -> IForgeMenuType.create(SequenceBufferSideMenu::new));

    public static final RegistryObject<MenuType<AdvancedPatternEncodingTermMenu>> ADVANCED_PATTERN_ENCODING_TERMINAL =
            MENUS.register(
                    "advanced_pattern_encoding_terminal",
                    () -> IForgeMenuType.create(AdvancedPatternEncodingTermMenu::fromNetwork));

    private APMenus() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}

package com.warmthdawn.appliedpackaging.registry;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.world.menu.MePackagerMenu;
import com.warmthdawn.appliedpackaging.world.menu.PackageAssemblerMenu;
import com.warmthdawn.appliedpackaging.world.menu.PackageBusMenu;
import com.warmthdawn.appliedpackaging.world.menu.PackagePatternTerminalMenu;
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

    public static final RegistryObject<MenuType<PackagePatternTerminalMenu>> PACKAGE_PATTERN_TERMINAL = MENUS.register(
            "package_pattern_terminal",
            () -> IForgeMenuType.create(PackagePatternTerminalMenu::new));

    public static final RegistryObject<MenuType<PackageBusMenu>> PACKAGE_BUS = MENUS.register(
            "package_bus",
            () -> IForgeMenuType.create(PackageBusMenu::new));

    private APMenus() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}

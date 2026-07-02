package com.warmthdawn.appliedpackaging.registry;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.item.PackagePatternItem;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class APItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AppliedPackaging.MOD_ID);

    private static final EnumMap<PackageColor, RegistryObject<Item>> PACKAGE_ITEMS = new EnumMap<>(PackageColor.class);

    public static final RegistryObject<Item> PACKAGE_PATTERN = ITEMS.register(
            "package_pattern",
            () -> new PackagePatternItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PACKAGED_PROCESSING_PATTERN = ITEMS.register(
            "packaged_processing_pattern",
            () -> new PackagePatternItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ME_PACKAGER = ITEMS.register(
            "me_packager",
            () -> new BlockItem(APBlocks.ME_PACKAGER.get(), new Item.Properties()));

    public static final RegistryObject<Item> PACKAGE_ASSEMBLER = ITEMS.register(
            "package_assembler",
            () -> new BlockItem(APBlocks.PACKAGE_ASSEMBLER.get(), new Item.Properties()));

    public static final RegistryObject<Item> PACKAGE_STORAGE_BUS = ITEMS.register(
            "package_storage_bus",
            () -> new BlockItem(APBlocks.PACKAGE_STORAGE_BUS.get(), new Item.Properties()));

    public static final RegistryObject<Item> PACKAGE_EXPORT_BUS = ITEMS.register(
            "package_export_bus",
            () -> new BlockItem(APBlocks.PACKAGE_EXPORT_BUS.get(), new Item.Properties()));

    public static final RegistryObject<Item> PACKAGE_UNPACKING_BUS = ITEMS.register(
            "package_unpacking_bus",
            () -> new BlockItem(APBlocks.PACKAGE_UNPACKING_BUS.get(), new Item.Properties()));

    public static final RegistryObject<Item> PACKAGE_PATTERN_TERMINAL = ITEMS.register(
            "package_pattern_terminal",
            () -> new BlockItem(APBlocks.PACKAGE_PATTERN_TERMINAL.get(), new Item.Properties()));

    static {
        for (PackageColor color : PackageColor.values()) {
            PACKAGE_ITEMS.put(color, ITEMS.register(
                    color.id() + "_package",
                    () -> new PackageItem(color, new Item.Properties().stacksTo(64))));
        }
    }

    private APItems() {
    }

    public static Map<PackageColor, RegistryObject<Item>> packageItems() {
        return Collections.unmodifiableMap(PACKAGE_ITEMS);
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

package com.warmthdawn.appliedpackaging.registry;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.Item;
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
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PACKAGED_PROCESSING_PATTERN = ITEMS.register(
            "packaged_processing_pattern",
            () -> new Item(new Item.Properties().stacksTo(1)));

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

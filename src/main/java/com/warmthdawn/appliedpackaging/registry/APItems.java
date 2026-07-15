package com.warmthdawn.appliedpackaging.registry;

import appeng.items.parts.PartItem;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.item.AdvancedProcessingPatternItem;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageCraftingPatternItem;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.part.AdvancedPatternEncodingTerminalPart;
import com.warmthdawn.appliedpackaging.part.PackageStorageBusPart;
import com.warmthdawn.appliedpackaging.part.PackageUnpackingBusPart;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
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
            () -> new PackageCraftingPatternItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<AdvancedProcessingPatternItem> ADVANCED_PROCESSING_PATTERN = ITEMS.register(
            "advanced_processing_pattern",
            () -> new AdvancedProcessingPatternItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ME_PACKAGER = ITEMS.register(
            "me_packager",
            () -> new BlockItem(APBlocks.ME_PACKAGER.get(), new Item.Properties()));

    public static final RegistryObject<Item> PACKAGE_ASSEMBLER = ITEMS.register(
            "package_assembler",
            () -> new BlockItem(APBlocks.PACKAGE_ASSEMBLER.get(), new Item.Properties()));

    public static final RegistryObject<Item> SEQUENCE_BUFFER = ITEMS.register(
            "sequence_buffer",
            () -> new BlockItem(APBlocks.SEQUENCE_BUFFER.get(), new Item.Properties()));

    public static final RegistryObject<Item> PACKAGE_STORAGE_BUS = ITEMS.register(
            "package_storage_bus",
            () -> new PartItem<>(
                    new Item.Properties(),
                    PackageStorageBusPart.class,
                    PackageStorageBusPart::new));

    public static final RegistryObject<Item> PACKAGE_UNPACKING_BUS = ITEMS.register(
            "package_unpacking_bus",
            () -> new PartItem<>(
                    new Item.Properties(),
                    PackageUnpackingBusPart.class,
                    PackageUnpackingBusPart::new));

    public static final RegistryObject<Item> ADVANCED_PATTERN_ENCODING_TERMINAL = ITEMS.register(
            "advanced_pattern_encoding_terminal",
            () -> new PartItem<>(
                    new Item.Properties(),
                    AdvancedPatternEncodingTerminalPart.class,
                    AdvancedPatternEncodingTerminalPart::new));

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

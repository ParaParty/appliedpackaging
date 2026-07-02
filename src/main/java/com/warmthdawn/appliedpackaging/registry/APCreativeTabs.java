package com.warmthdawn.appliedpackaging.registry;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class APCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AppliedPackaging.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.appliedpackaging"))
            .icon(() -> APItems.PACKAGE_PATTERN.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(APItems.PACKAGE_PATTERN.get());
                output.accept(APItems.PACKAGED_PROCESSING_PATTERN.get());
            })
            .build());

    private APCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}

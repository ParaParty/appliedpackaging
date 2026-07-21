package com.warmthdawn.appliedpackaging.integration.jei;

import com.warmthdawn.appliedpackaging.AppliedPackaging;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class AppliedPackagingJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = AppliedPackaging.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addUniversalRecipeTransferHandler(
                new AdvancedRecipeTransferHandler(registration.getTransferHelper()));
    }
}

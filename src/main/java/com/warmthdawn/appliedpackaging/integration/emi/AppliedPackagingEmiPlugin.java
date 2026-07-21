package com.warmthdawn.appliedpackaging.integration.emi;

import com.warmthdawn.appliedpackaging.registry.APMenus;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public final class AppliedPackagingEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(
                APMenus.ADVANCED_PATTERN_ENCODING_TERMINAL.get(),
                new AdvancedEmiRecipeHandler());
    }
}

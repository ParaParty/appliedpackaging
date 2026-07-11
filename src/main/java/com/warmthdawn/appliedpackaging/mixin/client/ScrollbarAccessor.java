package com.warmthdawn.appliedpackaging.mixin.client;

import appeng.client.gui.widgets.Scrollbar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Scrollbar.class, remap = false)
public interface ScrollbarAccessor {
    @Mutable
    @Final
    @Accessor("style")
    void appliedpackaging$setStyle(Scrollbar.Style style);
}

package com.warmthdawn.appliedpackaging.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int appliedpackaging$getLeftPos();

    @Accessor("topPos")
    int appliedpackaging$getTopPos();

    @Accessor("imageHeight")
    int appliedpackaging$getImageHeight();

    @Accessor("hoveredSlot")
    Slot appliedpackaging$getHoveredSlot();

    @Accessor("hoveredSlot")
    void appliedpackaging$setHoveredSlot(Slot slot);
}

package com.warmthdawn.appliedpackaging.mixin;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import java.util.function.Consumer;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AEBaseMenu.class, remap = false)
public interface AEBaseMenuAccessor {
    @Invoker("addSlot")
    Slot appliedpackaging$addSlot(Slot slot, SlotSemantic semantic);

    @Invoker("registerClientAction")
    <T> void appliedpackaging$registerClientAction(String name, Class<T> argClass, Consumer<T> handler);

    @Invoker("sendClientAction")
    <T> void appliedpackaging$sendClientAction(String action, T arg);
}

package com.warmthdawn.appliedpackaging.mixin.client;

import appeng.client.gui.WidgetContainer;
import appeng.client.gui.me.items.EncodingModePanel;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.menu.me.items.PatternEncodingTermMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = EncodingModePanel.class, remap = false)
public interface EncodingModePanelAccessor {
    @Accessor("widgets")
    WidgetContainer appliedpackaging$getWidgets();

    @Accessor("screen")
    PatternEncodingTermScreen<?> appliedpackaging$getScreen();

    @Accessor("menu")
    PatternEncodingTermMenu appliedpackaging$getMenu();
}

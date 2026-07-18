package com.warmthdawn.appliedpackaging.client.screen;

import appeng.client.gui.style.ScreenStyle;
import com.warmthdawn.appliedpackaging.world.menu.SequenceBufferSideMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SequenceBufferSideScreen extends AbstractSequenceBufferScreen<SequenceBufferSideMenu> {
    public SequenceBufferSideScreen(
            SequenceBufferSideMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }
}

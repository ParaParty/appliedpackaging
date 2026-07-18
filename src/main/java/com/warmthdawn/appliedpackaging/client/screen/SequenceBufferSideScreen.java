package com.warmthdawn.appliedpackaging.client.screen;

import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import com.warmthdawn.appliedpackaging.world.menu.SequenceBufferSideMenu;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SequenceBufferSideScreen extends AbstractSequenceBufferScreen<SequenceBufferSideMenu> {
    private final OpenMainButton openMainButton;

    public SequenceBufferSideScreen(
            SequenceBufferSideMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
        openMainButton = addToLeftToolbar(new OpenMainButton());
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        openMainButton.visible = menu.canOpenMain();
        openMainButton.active = menu.canOpenMain();
    }

    private final class OpenMainButton extends IconButton {
        private OpenMainButton() {
            super(ignored -> menu.openMain());
        }

        @Override
        protected Icon getIcon() {
            return Icon.ENTER;
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(
                    Component.translatable("gui.appliedpackaging.sequence_buffer.open_main"),
                    Component.translatable("gui.appliedpackaging.sequence_buffer.open_main.hint"));
        }
    }
}

package com.warmthdawn.appliedpackaging.client.screen;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.ToggleButton;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.world.menu.AbstractSequenceBufferMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public abstract class AbstractSequenceBufferScreen<T extends AbstractSequenceBufferMenu>
        extends ModernUpgradeableScreen<T> {
    protected static final Blitter SLOT_BACKGROUND = Blitter.texture(new ResourceLocation(
            AppliedPackaging.MOD_ID,
            "textures/gui/package-storagebus-sprites.png")).src(0, 64, 18, 18);

    private final ToggleButton autoOutputButton;
    private final ToggleButton blockingModeButton;
    private final ToggleButton antiClogModeButton;
    private final ToggleButton synchronizedOutputButton;
    private final ToggleButton patternModeButton;
    private final InputDelayButton inputDelayButton;

    protected AbstractSequenceBufferScreen(
            T menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        autoOutputButton = addSettingToggle(
                Icon.AUTO_EXPORT_ON,
                Icon.AUTO_EXPORT_OFF,
                "gui.appliedpackaging.sequence_buffer.setting.auto_output",
                "gui.appliedpackaging.sequence_buffer.setting.auto_output.enabled",
                "gui.appliedpackaging.sequence_buffer.setting.auto_output.disabled",
                menu::toggleAutoOutput);
        blockingModeButton = addSettingToggle(
                Icon.BLOCKING_MODE_YES,
                Icon.BLOCKING_MODE_NO,
                "gui.appliedpackaging.sequence_buffer.setting.blocking",
                "gui.appliedpackaging.sequence_buffer.setting.blocking.enabled",
                "gui.appliedpackaging.sequence_buffer.setting.blocking.disabled",
                menu::toggleBlockingMode);
        antiClogModeButton = addSettingToggle(
                Icon.AUTO_EXPORT_ON,
                Icon.AUTO_EXPORT_OFF,
                "gui.appliedpackaging.anti_clog_mode",
                "gui.appliedpackaging.anti_clog_mode.enabled",
                "gui.appliedpackaging.anti_clog_mode.disabled",
                menu::toggleAntiClogMode);
        synchronizedOutputButton = addSettingToggle(
                Icon.SCHEDULING_ROUND_ROBIN,
                Icon.SCHEDULING_DEFAULT,
                "gui.appliedpackaging.sequence_buffer.setting.synchronized_output",
                "gui.appliedpackaging.sequence_buffer.setting.synchronized_output.enabled",
                "gui.appliedpackaging.sequence_buffer.setting.synchronized_output.disabled",
                menu::toggleSynchronizedOutput);
        patternModeButton = addSettingToggle(
                Icon.VIEW_MODE_CRAFTING,
                Icon.VIEW_MODE_ALL,
                "gui.appliedpackaging.sequence_buffer.setting.pattern_mode",
                "gui.appliedpackaging.sequence_buffer.setting.pattern_mode.enabled",
                "gui.appliedpackaging.sequence_buffer.setting.pattern_mode.disabled",
                menu::togglePatternMode);
        inputDelayButton = addToLeftToolbar(new InputDelayButton());
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        autoOutputButton.setState(menu.autoOutput());
        blockingModeButton.setState(menu.blockingMode());
        antiClogModeButton.setState(menu.antiClogMode());
        synchronizedOutputButton.setState(menu.synchronizedOutput());
        patternModeButton.setState(menu.patternMode());
        inputDelayButton.setMessage(Component.translatable(
                "gui.appliedpackaging.sequence_buffer.setting.input_delay.value",
                menu.inputDelayTicks()));
    }

    @Override
    public void drawBG(
            GuiGraphics graphics,
            int offsetX,
            int offsetY,
            int mouseX,
            int mouseY,
            float partialTicks) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        graphics.flush();
        drawStorageSlotBackgrounds(graphics, offsetX, offsetY);
        graphics.flush();
    }

    protected void drawStorageSlotBackgrounds(GuiGraphics graphics, int offsetX, int offsetY) {
    }

    private ToggleButton addSettingToggle(
            Icon enabledIcon,
            Icon disabledIcon,
            String titleKey,
            String enabledHintKey,
            String disabledHintKey,
            Runnable action) {
        ToggleButton button = new ToggleButton(enabledIcon, disabledIcon, ignored -> action.run());
        Component title = Component.translatable(titleKey);
        button.setTooltipOn(List.of(title, Component.translatable(enabledHintKey)));
        button.setTooltipOff(List.of(title, Component.translatable(disabledHintKey)));
        return addToLeftToolbar(button);
    }

    private final class InputDelayButton extends IconButton {
        private InputDelayButton() {
            super(button -> menu.cycleInputDelay(isHandlingRightClick()));
        }

        @Override
        protected Icon getIcon() {
            return Icon.REDSTONE_PULSE;
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(
                    Component.translatable("gui.appliedpackaging.sequence_buffer.setting.input_delay"),
                    Component.translatable(
                            "gui.appliedpackaging.sequence_buffer.setting.input_delay.value",
                            menu.inputDelayTicks()),
                    Component.translatable("gui.appliedpackaging.sequence_buffer.setting.input_delay.hint"));
        }
    }
}

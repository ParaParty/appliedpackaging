package com.warmthdawn.appliedpackaging.client.screen;

import appeng.api.config.ActionItems;
import appeng.client.gui.Icon;
import appeng.client.guidebook.PageAnchor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.ProgressBar;
import appeng.client.gui.widgets.ProgressBar.Direction;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.IOptionalSlot;
import com.warmthdawn.appliedpackaging.client.widget.ModernSlotRendering;
import com.warmthdawn.appliedpackaging.client.widget.PackageColorPicker;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import com.warmthdawn.appliedpackaging.world.menu.MePackagerMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class MePackagerScreen extends ModernUpgradeableScreen<MePackagerMenu> {
    private static final int COLOR_BUTTON_X = 18;
    private static final int COLOR_BUTTON_Y = 28;
    private static final int COLOR_BUTTON_SIZE = 8;
    private static final int SLOT_BACKGROUND_TOP = 0xff9a9fb4;
    private static final int SLOT_BACKGROUND_BODY = 0xffadb0c4;
    private static final int OPTIONAL_SLOT_DISABLED_ALPHA = 0x33;

    private final FilterModeButton filterModeButton;
    private final ActivationModeButton activationModeButton;
    private final BlockingModeButton blockingModeButton;
    private final PackageColorPicker colorPicker = new PackageColorPicker();
    private final PackageColorPicker.TriggerButton colorButton;
    private final ProgressBar packingProgress;
    private final ProgressBar unpackingProgress;

    public MePackagerScreen(
            MePackagerMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        colorButton = new PackageColorPicker.TriggerButton(
                COLOR_BUTTON_SIZE,
                COLOR_BUTTON_SIZE,
                menu::selectedColor,
                this::openColorPicker);
        packingProgress = new ProgressBar(menu, style.getImage("packingProgress"), Direction.VERTICAL);
        unpackingProgress = new ProgressBar(menu, style.getImage("unpackingProgress"), Direction.VERTICAL);
        widgets.add("packingProgress", packingProgress);
        widgets.add("unpackingProgress", unpackingProgress);

        addToLeftToolbar(new LocalHelpButton());
        addToLeftToolbar(new ActionButton(ActionItems.CLOSE, ignored -> menu.clear()));
        addToLeftToolbar(new ActionButton(ActionItems.WRENCH, ignored -> menu.partition()));
        filterModeButton = addToLeftToolbar(new FilterModeButton());
        activationModeButton = addToLeftToolbar(new ActivationModeButton());
        blockingModeButton = addToLeftToolbar(new BlockingModeButton());

    }

    @Override
    protected void init() {
        super.init();

        colorButton.setX(leftPos + COLOR_BUTTON_X);
        colorButton.setY(topPos + COLOR_BUTTON_Y);
        addRenderableWidget(colorButton);
    }

    @Override
    protected PageAnchor getHelpTopic() {
        return null;
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        filterModeButton.setMessage(Component.translatable(
                "gui.appliedpackaging.me_packager.filter_mode." + menu.filterMode().id()));
        activationModeButton.setMessage(Component.translatable(
                "gui.appliedpackaging.me_packager.redstone_mode." + menu.activationMode().id()));
        blockingModeButton.setMessage(Component.translatable(
                "gui.appliedpackaging.me_packager.blocking_mode." + menu.blockingMode().id()));
        colorButton.active = !colorPicker.isOpen();
        packingProgress.visible = menu.workingOperation() == MePackagerBlockEntity.WorkingOperation.PACKING;
        unpackingProgress.visible = menu.workingOperation() == MePackagerBlockEntity.WorkingOperation.UNPACKING;

    }

    @Override
    public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        drawOptionalConfigSlotBackgrounds(graphics, offsetX, offsetY);
        ModernSlotRendering.drawStorageComponentSlotIcon(
                graphics,
                offsetX,
                offsetY,
                firstSlot(SlotSemantics.STORAGE_CELL));
        ModernSlotRendering.drawMarkerSlotIcon(
                graphics,
                offsetX,
                offsetY,
                markerSlot(),
                1.0f);
    }

    @Override
    public void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        if (menu.unpackBlocked() && menu.getSlotSemantic(slot) == SlotSemantics.MACHINE_OUTPUT) {
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x66ff3333);
            graphics.renderOutline(slot.x - 1, slot.y - 1, 18, 18, 0xffff5555);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        colorPicker.render(graphics, font, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPicker.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (colorPicker.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (colorPicker.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (colorPicker.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (colorPicker.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return colorPicker.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!colorPicker.isOpen()) {
            super.renderTooltip(graphics, mouseX, mouseY);
            Slot markerSlot = markerSlot();
            if (hoveredSlot == markerSlot) {
                drawEmptyMarkerTooltip(graphics, mouseX, mouseY, markerSlot);
            }
        }
    }

    private Slot markerSlot() {
        return firstSlot(SlotSemantics.BLANK_PATTERN);
    }

    private Slot firstSlot(appeng.menu.SlotSemantic semantic) {
        List<Slot> slots = menu.getSlots(semantic);
        return slots.isEmpty() ? null : slots.get(0);
    }

    private void drawOptionalConfigSlotBackgrounds(GuiGraphics graphics, int offsetX, int offsetY) {
        for (Slot slot : menu.getSlots(SlotSemantics.CONFIG)) {
            if (slot instanceof IOptionalSlot optionalSlot) {
                int alpha = optionalSlot.isSlotEnabled() ? 0xff : OPTIONAL_SLOT_DISABLED_ALPHA;
                int x = offsetX + slot.x - 1;
                int y = offsetY + slot.y - 1;
                graphics.fill(x + 1, y + 1, x + 17, y + 2, withAlpha(SLOT_BACKGROUND_TOP, alpha));
                graphics.fill(x + 1, y + 2, x + 17, y + 17, withAlpha(SLOT_BACKGROUND_BODY, alpha));
            }
        }
    }

    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00ffffff) | (alpha << 24);
    }

    private void openColorPicker() {
        setFocused(null);
        colorPicker.openNear(
                colorButton,
                width,
                height,
                menu::selectedColor,
                menu::setSelectedColor,
                () -> colorButton.active = true);
    }

    private class FilterModeButton extends IconButton {
        private FilterModeButton() {
            super(button -> menu.cycleFilterMode());
        }

        @Override
        protected Icon getIcon() {
            return switch (menu.filterMode()) {
                case BOTH -> Icon.TYPE_FILTER_ALL;
                case PACK_ONLY -> Icon.STORAGE_FILTER_EXTRACTABLE_NONE;
                case UNPACK_ONLY -> Icon.FILTER_ON_EXTRACT_ENABLED;
            };
        }
    }

    private class LocalHelpButton extends IconButton {
        private LocalHelpButton() {
            super(button -> {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.translatable("gui.appliedpackaging.me_packager.help"),
                            false);
                }
            });
            setMessage(Component.translatable("gui.appliedpackaging.me_packager.help.title"));
        }

        @Override
        protected Icon getIcon() {
            return Icon.HELP;
        }
    }

    private class ActivationModeButton extends IconButton {
        private ActivationModeButton() {
            super(button -> menu.cycleActivationMode());
        }

        @Override
        protected Icon getIcon() {
            return switch (menu.activationMode()) {
                case HIGH_SIGNAL -> Icon.REDSTONE_HIGH;
                case LOW_SIGNAL -> Icon.REDSTONE_LOW;
                case ALWAYS -> Icon.REDSTONE_IGNORE;
                case PULSE -> Icon.REDSTONE_PULSE;
                case NEVER -> Icon.CLEAR;
            };
        }
    }

    private class BlockingModeButton extends IconButton {
        private BlockingModeButton() {
            super(button -> menu.cycleBlockingMode());
        }

        @Override
        protected Icon getIcon() {
            return switch (menu.blockingMode()) {
                case IGNORE_NETWORK_CONTENTS -> Icon.BLOCKING_MODE_NO;
                case BLOCK_UNPACK_WHEN_NETWORK_HAS_ITEMS -> Icon.BLOCKING_MODE_YES;
            };
        }
    }

}

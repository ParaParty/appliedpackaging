package com.warmthdawn.appliedpackaging.client.screen;

import appeng.api.config.ActionItems;
import appeng.client.gui.Icon;
import appeng.client.guidebook.PageAnchor;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.IconButton;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.IOptionalSlot;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.world.menu.MePackagerMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

public class MePackagerScreen extends UpgradeableScreen<MePackagerMenu> {
    private static final int COLOR_BUTTON_X = 67;
    private static final int COLOR_BUTTON_Y = 39;
    private static final int COLOR_BUTTON_SIZE = 12;
    private static final int COLOR_POPUP_X = 8;
    private static final int COLOR_POPUP_Y = 52;
    private static final int COLOR_POPUP_COLUMNS = 9;
    private static final int COLOR_POPUP_PADDING = 3;
    private static final int COLOR_SWATCH_SIZE = 8;
    private static final int COLOR_SWATCH_STEP = 10;
    private static final int COLOR_POPUP_BACKGROUND = 0xf0180a1f;
    private static final int COLOR_POPUP_BORDER = 0xff4d3f5c;
    private static final int WORK_PROGRESS_X = 107;
    private static final int WORK_PROGRESS_Y = 44;
    private static final int WORK_PROGRESS_WIDTH = 28;
    private static final int WORK_PROGRESS_HEIGHT = 6;
    private static final int WORK_PROGRESS_TRACK = 0xff27313a;
    private static final int WORK_PROGRESS_PACKING = 0xff4fc3f7;
    private static final int WORK_PROGRESS_UNPACKING = 0xff7bd66f;
    private static final int WORK_PROGRESS_HIGHLIGHT = 0x99ffffff;
    private static final int SLOT_BACKGROUND_TOP = 0xff9a9fb4;
    private static final int SLOT_BACKGROUND_BODY = 0xffadb0c4;
    private static final int OPTIONAL_SLOT_DISABLED_ALPHA = 0x33;

    private final AETextField packageNameField;
    private final FilterModeButton filterModeButton;
    private final ActivationModeButton activationModeButton;
    private final BlockingModeButton blockingModeButton;
    private boolean colorPopupOpen;
    private boolean updatingNameField;

    public MePackagerScreen(
            MePackagerMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        addToLeftToolbar(new LocalHelpButton());
        addToLeftToolbar(new ActionButton(ActionItems.CLOSE, ignored -> menu.clear()));
        addToLeftToolbar(new ActionButton(ActionItems.WRENCH, ignored -> menu.partition()));
        filterModeButton = addToLeftToolbar(new FilterModeButton());
        activationModeButton = addToLeftToolbar(new ActivationModeButton());
        blockingModeButton = addToLeftToolbar(new BlockingModeButton());

        packageNameField = widgets.addTextField("packageName");
        packageNameField.setMaxLength(50);
        packageNameField.setPlaceholder(Component.translatable(
                "gui.appliedpackaging.me_packager.package_name.placeholder"));
        packageNameField.setTooltipMessage(List.of(Component.translatable(
                "gui.appliedpackaging.me_packager.package_name")));
        packageNameField.setResponder(value -> {
            if (!updatingNameField) {
                menu.setPackageName(value);
            }
        });
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(new ColorPickerButton(leftPos + COLOR_BUTTON_X, topPos + COLOR_BUTTON_Y));
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

        if (!packageNameField.isFocused() && !packageNameField.getValue().equals(menu.packageName())) {
            updatingNameField = true;
            try {
                packageNameField.setValue(menu.packageName());
            } finally {
                updatingNameField = false;
            }
        }
    }

    @Override
    public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        drawOptionalConfigSlotBackgrounds(graphics, offsetX, offsetY);
        drawSlotIcon(graphics, offsetX, offsetY, SlotSemantics.STORAGE_CELL, Icon.BACKGROUND_STORAGE_COMPONENT);
        drawWorkProgress(graphics, offsetX, offsetY, partialTicks);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (colorPopupOpen) {
            renderColorPopup(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPopupOpen) {
            if (isInColorButton(mouseX, mouseY)) {
                colorPopupOpen = false;
                return true;
            }

            PackageColor color = colorAt(mouseX, mouseY);
            if (color != null) {
                menu.setSelectedColor(color);
                colorPopupOpen = false;
                return true;
            }

            colorPopupOpen = false;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (colorPopupOpen) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (colorPopupOpen) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (colorPopupOpen) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (colorPopupOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                colorPopupOpen = false;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (colorPopupOpen) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void drawSlotIcon(GuiGraphics graphics, int offsetX, int offsetY, appeng.menu.SlotSemantic semantic, Icon icon) {
        List<Slot> slots = menu.getSlots(semantic);
        if (slots.isEmpty()) {
            return;
        }
        Slot slot = slots.get(0);
        if (!slot.hasItem()) {
            icon.getBlitter()
                    .dest(offsetX + slot.x, offsetY + slot.y)
                    .blit(graphics);
        }
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

    private void drawWorkProgress(GuiGraphics graphics, int offsetX, int offsetY, float partialTicks) {
        if (!menu.isWorking()) {
            return;
        }
        int x = offsetX + WORK_PROGRESS_X;
        int y = offsetY + WORK_PROGRESS_Y;
        int innerWidth = WORK_PROGRESS_WIDTH - 2;
        int fillWidth = Math.max(1, Math.round(innerWidth * menu.workProgress(partialTicks)));
        int fillColor = switch (menu.workingOperation()) {
            case PACKING -> WORK_PROGRESS_PACKING;
            case UNPACKING -> WORK_PROGRESS_UNPACKING;
            case NONE -> WORK_PROGRESS_PACKING;
        };

        graphics.fill(x, y, x + WORK_PROGRESS_WIDTH, y + WORK_PROGRESS_HEIGHT, 0xff12171c);
        graphics.fill(x + 1, y + 1, x + WORK_PROGRESS_WIDTH - 1, y + WORK_PROGRESS_HEIGHT - 1, WORK_PROGRESS_TRACK);
        graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + WORK_PROGRESS_HEIGHT - 1, fillColor);
        graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + 2, WORK_PROGRESS_HIGHLIGHT);
    }

    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00ffffff) | (alpha << 24);
    }

    private void renderColorPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        int panelX = leftPos + COLOR_POPUP_X - COLOR_POPUP_PADDING;
        int panelY = topPos + COLOR_POPUP_Y - COLOR_POPUP_PADDING;
        int panelWidth = COLOR_POPUP_COLUMNS * COLOR_SWATCH_STEP + COLOR_POPUP_PADDING * 2;
        int panelHeight = colorPopupRows() * COLOR_SWATCH_STEP + COLOR_POPUP_PADDING * 2;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_POPUP_BACKGROUND);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, COLOR_POPUP_BORDER);

        PackageColor[] colors = PackageColor.values();
        for (int index = 0; index < colors.length; index++) {
            PackageColor color = colors[index];
            int x = swatchX(index);
            int y = swatchY(index);
            boolean selected = menu.selectedColor() == color;
            boolean hovered = mouseX >= x && mouseX < x + COLOR_SWATCH_SIZE
                    && mouseY >= y && mouseY < y + COLOR_SWATCH_SIZE;
            int border = selected ? 0xffffffff : (hovered ? 0xffd6dbde : 0xff2a3036);
            graphics.fill(x, y, x + COLOR_SWATCH_SIZE, y + COLOR_SWATCH_SIZE, border);
            graphics.fill(x + 1, y + 1, x + COLOR_SWATCH_SIZE - 1, y + COLOR_SWATCH_SIZE - 1, color.swatchArgb());
            if (selected) {
                graphics.renderOutline(x - 1, y - 1, COLOR_SWATCH_SIZE + 2, COLOR_SWATCH_SIZE + 2, 0xff2a3036);
            }
        }
    }

    private boolean isInColorButton(double mouseX, double mouseY) {
        int x = leftPos + COLOR_BUTTON_X;
        int y = topPos + COLOR_BUTTON_Y;
        return mouseX >= x && mouseX < x + COLOR_BUTTON_SIZE
                && mouseY >= y && mouseY < y + COLOR_BUTTON_SIZE;
    }

    private boolean isInColorPopup(double mouseX, double mouseY) {
        int x = leftPos + COLOR_POPUP_X - COLOR_POPUP_PADDING;
        int y = topPos + COLOR_POPUP_Y - COLOR_POPUP_PADDING;
        int width = COLOR_POPUP_COLUMNS * COLOR_SWATCH_STEP + COLOR_POPUP_PADDING * 2;
        int height = colorPopupRows() * COLOR_SWATCH_STEP + COLOR_POPUP_PADDING * 2;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private PackageColor colorAt(double mouseX, double mouseY) {
        PackageColor[] colors = PackageColor.values();
        for (int index = 0; index < colors.length; index++) {
            int x = swatchX(index);
            int y = swatchY(index);
            if (mouseX >= x && mouseX < x + COLOR_SWATCH_SIZE
                    && mouseY >= y && mouseY < y + COLOR_SWATCH_SIZE) {
                return colors[index];
            }
        }
        return null;
    }

    private int swatchX(int index) {
        return leftPos + COLOR_POPUP_X + (index % COLOR_POPUP_COLUMNS) * COLOR_SWATCH_STEP;
    }

    private int swatchY(int index) {
        return topPos + COLOR_POPUP_Y + (index / COLOR_POPUP_COLUMNS) * COLOR_SWATCH_STEP;
    }

    private static int colorPopupRows() {
        return (PackageColor.values().length + COLOR_POPUP_COLUMNS - 1) / COLOR_POPUP_COLUMNS;
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
                case HIGH_SIGNAL, CYCLIC -> Icon.REDSTONE_HIGH;
                case LOW_SIGNAL -> Icon.REDSTONE_LOW;
                case ALWAYS -> Icon.REDSTONE_IGNORE;
                case PULSE -> Icon.REDSTONE_PULSE;
                case NEVER, DISABLED -> Icon.CLEAR;
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

    private class ColorPickerButton extends AbstractButton {
        private ColorPickerButton(int x, int y) {
            super(x, y, COLOR_BUTTON_SIZE, COLOR_BUTTON_SIZE,
                    Component.translatable("gui.appliedpackaging.me_packager.color"));
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            colorPopupOpen = !colorPopupOpen;
            if (colorPopupOpen) {
                packageNameField.setFocused(false);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX() + 3, getY() + 3, getX() + 9, getY() + 9, menu.selectedColor().swatchArgb());
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}

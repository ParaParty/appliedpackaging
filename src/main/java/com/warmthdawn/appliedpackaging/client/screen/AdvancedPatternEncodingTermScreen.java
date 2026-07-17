package com.warmthdawn.appliedpackaging.client.screen;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.EmptyingAction;
import appeng.api.config.ActionItems;
import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.me.common.RepoSlot;
import appeng.client.gui.me.common.StackSizeRenderer;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.TabButton;
import appeng.core.AEConfig;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.ResizableSlot;
import com.warmthdawn.appliedpackaging.client.widget.PackageColorPicker;
import com.warmthdawn.appliedpackaging.client.widget.ModernSlotRendering;
import com.warmthdawn.appliedpackaging.client.widget.ModernVerticalToolbar;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.part.SpecializedPatternMode;
import com.warmthdawn.appliedpackaging.mixin.client.MEStorageScreenAccessor;
import com.warmthdawn.appliedpackaging.mixin.client.ScrollbarAccessor;
import com.warmthdawn.appliedpackaging.mixin.client.SlotAccessor;
import com.warmthdawn.appliedpackaging.world.menu.AdvancedPatternEncodingTermMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class AdvancedPatternEncodingTermScreen
        extends MEStorageScreen<AdvancedPatternEncodingTermMenu> {
    private static final int VISIBLE_COLUMNS = 4;
    private static final int VISIBLE_ROWS = 3;
    private static final int INPUT_X = 21;
    private static final int INPUT_BOTTOM = 164;
    private static final int OUTPUT_X = 119;
    private static final int SLOT_STEP = 18;
    private static final int COLUMN_STEP = 19;
    private static final int HEADER_COLOR_X = 22;
    private static final int HEADER_ACTION_X = 31;
    private static final int HEADER_COLOR_BOTTOM = 174;
    private static final int HEADER_ACTION_BOTTOM = 173;
    private static final int HEADER_BUTTON_SIZE = 8;
    private static final int INPUT_PANEL_WIDTH = (VISIBLE_COLUMNS - 1) * COLUMN_STEP + SLOT_STEP;
    private static final int COLUMN_SCROLLBAR_X = 21;
    private static final int COLUMN_SCROLLBAR_BOTTOM = 110;
    private static final int COLUMN_SCROLLBAR_TRACK_WIDTH = 75;
    private static final int CLEAR_BUTTON_X = 97;
    private static final int CLEAR_BUTTON_BOTTOM = 174;
    private static final int ENCODE_BUTTON_X = 150;
    private static final int ENCODE_BUTTON_BOTTOM = 145;
    private static final int SCREEN_WIDTH = 195;
    private static final int TERMINAL_HEADER_HEIGHT = 17;
    private static final int TERMINAL_ROW_HEIGHT = 18;
    private static final int BOTTOM_HEIGHT = 192;
    private static final int PATTERN_TITLE_BOTTOM = 189;
    private static final int MODE_BUTTON_SIZE = 22;
    private static final int MODE_BUTTON_STEP = 21;
    private static final int MODE_BUTTON_TOP_GAP = 6;
    private static final int HIDDEN_SLOT = -10_000;
    private static final int PACKAGE_PANEL_LEFT = 8;
    private static final int PACKAGE_PANEL_BOTTOM = 177;
    private static final int PACKAGE_PANEL_WIDTH = 132;
    private static final int PACKAGE_PANEL_HEIGHT = 78;
    private static final int PACKAGE_INPUT_X = 24;
    private static final int PACKAGE_INPUT_BOTTOM = 164;
    private static final int PACKAGE_RESULT_X = 112;
    private static final int PACKAGE_RESULT_RELATIVE_Y = 37;
    private static final int PACKAGE_MARKER_X = 109;
    private static final int PACKAGE_MARKER_RELATIVE_Y = 13;
    private static final int BLANK_PATTERN_X = 150;
    private static final int BLANK_PATTERN_BOTTOM = 165;
    private static final int ENCODED_PATTERN_X = 150;
    private static final int ENCODED_PATTERN_BOTTOM = 118;
    private static final int PACKAGE_VISIBLE_ROWS = 3;
    private static final int PACKAGE_COLUMNS = 3;

    private static final ResourceLocation SPRITES = new ResourceLocation(
            "appliedpackaging",
            "textures/gui/advanced_pattern_encoding_terminal_sprites.png");
    private static final ResourceLocation LATEST_AE2_STATES = new ResourceLocation(
            "appliedpackaging",
            "textures/gui/advanced_pattern_encoding_terminal_states.png");
    private static final ResourceLocation LATEST_NETWORK_SCROLLBAR = new ResourceLocation(
            "appliedpackaging",
            "textures/gui/advanced_pattern_encoding_terminal_scrollbar.png");
    private static final ResourceLocation PACKAGE_PANEL_TEXTURE = new ResourceLocation(
            "appliedpackaging",
            "textures/gui/pattern_mode_packaging.png");
    private static final ResourceLocation ADVANCED_SCREEN_TEXTURE = new ResourceLocation(
            "appliedpackaging",
            "textures/gui/advanced_pattern_encoding_terminal.png");
    private static final ResourceLocation ADVANCED_MIDDLE_ROW_TEXTURE = new ResourceLocation(
            "appliedpackaging",
            "textures/gui/advanced_pattern_encoding_terminal_middle_row.png");
    private static final Scrollbar.Style NETWORK_SCROLLBAR_STYLE = Scrollbar.Style.create(
            LATEST_NETWORK_SCROLLBAR,
            12,
            15,
            0,
            0,
            12,
            0);
    private static final Scrollbar.Style ROW_SCROLLBAR_STYLE = Scrollbar.Style.create(
            SPRITES,
            7,
            15,
            0,
            32,
            16,
            32);
    private static final Scrollbar.Style PACKAGE_ROW_SCROLLBAR_STYLE = Scrollbar.Style.create(
            SPRITES,
            7,
            15,
            0,
            32,
            16,
            32);
    private static final Blitter COLUMN_SCROLLBAR = Blitter.texture(SPRITES).src(0, 16, 15, 8);
    private static final Blitter CLEAR_BUTTON = Blitter.texture(SPRITES).src(0, 0, 8, 8);
    private static final Blitter ADD_COLUMN_BUTTON = Blitter.texture(SPRITES).src(0, 8, 8, 8);
    private static final Blitter DELETE_COLUMN_BUTTON = Blitter.texture(SPRITES).src(8, 8, 8, 8);
    private static final Blitter LATEST_SLOT_BACKGROUND =
            Blitter.texture(LATEST_AE2_STATES).src(192, 192, 18, 18);
    private static final Blitter LATEST_PRIMARY_OUTPUT = Blitter.texture(LATEST_AE2_STATES).src(224, 0, 16, 16);
    private static final Blitter LATEST_ENCODE_ICON = Blitter.texture(LATEST_AE2_STATES).src(128, 0, 16, 16);
    private static final Blitter LATEST_ENCODE_BUTTON = Blitter.texture(LATEST_AE2_STATES).src(176, 128, 18, 20);
    private static final Blitter LATEST_ENCODE_BUTTON_FOCUS =
            Blitter.texture(LATEST_AE2_STATES).src(194, 128, 18, 20);
    private static final Blitter LATEST_ENCODE_BUTTON_HOVER =
            Blitter.texture(LATEST_AE2_STATES).src(212, 128, 18, 20);
    private static final Blitter LATEST_ENCODED_PATTERN =
            Blitter.texture(LATEST_AE2_STATES).src(240, 112, 16, 16);
    private static final Blitter LATEST_BLANK_PATTERN =
            Blitter.texture(LATEST_AE2_STATES).src(240, 128, 16, 16);
    private static final Blitter LATEST_TAB_BUTTON =
            Blitter.texture(LATEST_AE2_STATES).src(160, 192, 20, 20);
    private static final Blitter LATEST_TAB_BUTTON_FOCUS =
            Blitter.texture(LATEST_AE2_STATES).src(160, 224, 22, 22);
    private static final Blitter LATEST_CRAFT_HAMMER =
            Blitter.texture(LATEST_AE2_STATES).src(48, 144, 16, 16);
    private static final Blitter HORIZONTAL_MODE_TAB =
            Blitter.texture(LATEST_AE2_STATES).src(128, 128, 22, 22);
    private static final Blitter HORIZONTAL_MODE_TAB_SELECTED =
            Blitter.texture(LATEST_AE2_STATES).src(128, 150, 22, 22);
    private static final Blitter HORIZONTAL_MODE_TAB_FOCUS =
            Blitter.texture(LATEST_AE2_STATES).src(150, 128, 22, 22);
    private static final Blitter PROCESSING_MODE_ICON =
            Blitter.texture(LATEST_AE2_STATES).src(16, 32, 16, 16);
    private static final Blitter PACKAGE_MODE_ICON = Blitter.texture(SPRITES).src(32, 0, 16, 16);
    private static final Blitter PACKAGE_PANEL =
            Blitter.texture(PACKAGE_PANEL_TEXTURE).src(0, 0, PACKAGE_PANEL_WIDTH, PACKAGE_PANEL_HEIGHT);

    private final PackageColorPicker.TriggerButton[] columnColorButtons =
            new PackageColorPicker.TriggerButton[VISIBLE_COLUMNS];
    private final ScreenStyle screenStyle;
    private final ColumnActionButton[] columnActionButtons = new ColumnActionButton[VISIBLE_COLUMNS];
    private final PackageColorPicker colorPicker = new PackageColorPicker();
    private final LatestEncodeButton encodeButton = new LatestEncodeButton();
    private final LatestCraftingStatusButton craftingStatusButton = new LatestCraftingStatusButton();
    private final CompactClearButton clearButton = new CompactClearButton();
    private final ModernVerticalToolbar modernToolbar = new ModernVerticalToolbar();
    private final ActionButton cycleOutputButton;
    private final Scrollbar rowScrollbar;
    private final Scrollbar packageRowScrollbar;
    private final PackageColorPicker.TriggerButton packageColorButton;
    private final PatternModeButton advancedModeButton;
    private final PatternModeButton packageModeButton;
    private int scrollColumn;
    private int editedColumn = -1;
    private boolean draggingColumnScrollbar;
    private TabButton legacyCraftingStatusButton;
    private SpecializedPatternMode renderedMode;

    public AdvancedPatternEncodingTermScreen(
            AdvancedPatternEncodingTermMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.screenStyle = style;

        ((ScrollbarAccessor) ((MEStorageScreenAccessor) this).appliedpackaging$getNetworkScrollbar())
                .appliedpackaging$setStyle(NETWORK_SCROLLBAR_STYLE);
        clearLegacyPatternSlotIcons();

        cycleOutputButton = new ActionButton(
                ActionItems.CYCLE_PROCESSING_OUTPUT,
                ignored -> menu.cycleProcessingOutput());
        cycleOutputButton.setHalfSize(true);
        cycleOutputButton.setDisableBackground(true);
        widgets.add("processingCycleOutput", cycleOutputButton);
        rowScrollbar = widgets.addScrollBar("processingPatternModeScrollbar", ROW_SCROLLBAR_STYLE);
        rowScrollbar.setRange(
                0,
                AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE - VISIBLE_ROWS,
                VISIBLE_ROWS);
        rowScrollbar.setCaptureMouseWheel(false);

        packageRowScrollbar = widgets.addScrollBar("packagePatternModeScrollbar", PACKAGE_ROW_SCROLLBAR_STYLE);
        packageRowScrollbar.setRange(
                0,
                menu.getPackageInputSlots().length / PACKAGE_COLUMNS - PACKAGE_VISIBLE_ROWS,
                PACKAGE_VISIBLE_ROWS);
        packageRowScrollbar.setCaptureMouseWheel(false);

        packageColorButton = new PackageColorPicker.TriggerButton(
                HEADER_BUTTON_SIZE,
                HEADER_BUTTON_SIZE,
                false,
                () -> Optional.of(menu.getPackageColor()),
                this::openPackageColorPicker,
                () -> {
                });
        packageColorButton.setTooltip(Tooltip.create(
                Component.translatable("gui.appliedpackaging.package_pattern.settings")));

        advancedModeButton = new PatternModeButton(
                PROCESSING_MODE_ICON,
                Component.translatable("gui.appliedpackaging.advanced_pattern_terminal.mode"),
                () -> menu.setSpecializedMode(SpecializedPatternMode.ADVANCED));
        packageModeButton = new PatternModeButton(
                PACKAGE_MODE_ICON,
                Component.translatable("gui.appliedpackaging.package_pattern.mode"),
                () -> menu.setSpecializedMode(SpecializedPatternMode.PACKAGE));

        for (int visibleColumn = 0; visibleColumn < VISIBLE_COLUMNS; visibleColumn++) {
            int buttonIndex = visibleColumn;
            columnColorButtons[visibleColumn] = new PackageColorPicker.TriggerButton(
                    HEADER_BUTTON_SIZE,
                    HEADER_BUTTON_SIZE,
                    false,
                    () -> Optional.of(menu.columnColor(scrollColumn + buttonIndex)),
                    () -> openColumnEditor(
                            scrollColumn + buttonIndex,
                            columnColorButtons[buttonIndex]),
                    () -> {
                    });
            columnActionButtons[visibleColumn] = new ColumnActionButton(visibleColumn);
        }
    }

    @Override
    public void init() {
        super.init();

        applyScreenProfile(leftPos, topPos);
        setTextHidden("crafting_grid_title", true);
        replaceCraftingStatusButton();
        modernToolbar.captureIconButtons(children());
        for (Renderable renderer : modernToolbar.createIconButtonRenderers()) {
            addRenderableOnly(renderer);
        }
        for (PackageColorPicker.TriggerButton button : columnColorButtons) {
            addRenderableWidget(button);
        }
        for (ColumnActionButton button : columnActionButtons) {
            addRenderableWidget(button);
        }
        addRenderableWidget(packageColorButton);
        addRenderableWidget(advancedModeButton);
        addRenderableWidget(packageModeButton);
        addRenderableWidget(clearButton);
        addRenderableWidget(encodeButton);
        if (legacyCraftingStatusButton != null) {
            addRenderableWidget(craftingStatusButton);
        }
        if (renderedMode == null) {
            renderedMode = menu.getSpecializedMode();
        }
        updateSpecializedSlots(menu.getSpecializedMode() == SpecializedPatternMode.ADVANCED);
        layoutDynamicWidgets();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        setTextContent(TEXT_ID_DIALOG_TITLE, Component.translatable("gui.ae2.Terminal"));
        SpecializedPatternMode activeMode = menu.getSpecializedMode();
        if (renderedMode != activeMode) {
            closeColumnEditor();
            renderedMode = activeMode;
        }

        boolean advanced = activeMode == SpecializedPatternMode.ADVANCED;
        scrollColumn = Math.max(0, Math.min(scrollColumn, maxScrollColumn()));
        if (advanced && editedColumn >= menu.activeColumns()) {
            closeColumnEditor();
        }
        updateSpecializedSlots(advanced);
        layoutDynamicWidgets();

        boolean editorClosed = !colorPicker.isOpen();
        encodeButton.visible = true;
        encodeButton.active = editorClosed;
        clearButton.visible = true;
        clearButton.active = editorClosed;
        cycleOutputButton.setVisibility(advanced && menu.canCycleProcessingOutputs());
        cycleOutputButton.active = advanced && editorClosed;
        rowScrollbar.setVisible(advanced);
        packageRowScrollbar.setVisible(!advanced);
        packageColorButton.visible = !advanced;
        packageColorButton.active = !advanced && editorClosed;
        advancedModeButton.setSelected(advanced);
        packageModeButton.setSelected(!advanced);
        advancedModeButton.active = !advanced && editorClosed;
        packageModeButton.active = advanced && editorClosed;
        for (PackageColorPicker.TriggerButton button : columnColorButtons) {
            button.visible &= advanced;
        }
        for (ColumnActionButton button : columnActionButtons) {
            button.visible &= advanced;
        }
        if (legacyCraftingStatusButton != null) {
            legacyCraftingStatusButton.visible = false;
            craftingStatusButton.visible = true;
            craftingStatusButton.active = legacyCraftingStatusButton.active;
        }
    }

    @Override
    public void drawBG(
            GuiGraphics graphics,
            int offsetX,
            int offsetY,
            int mouseX,
            int mouseY,
            float partialTicks) {
        boolean advanced = menu.getSpecializedMode() == SpecializedPatternMode.ADVANCED;
        drawTerminalBackground(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        modernToolbar.layout(offsetX, offsetY);
        modernToolbar.drawPanel(graphics, offsetX, offsetY);
        if (advanced) {
            drawAdvancedInputSlotBackgrounds(graphics, offsetX, offsetY);
            drawColumnScrollbar(graphics, offsetX, offsetY);
            drawPrimaryOutputOverlay(graphics, offsetX, offsetY);
        } else {
            drawPackagePanel(graphics, offsetX, offsetY);
        }
        drawSlotIcon(
                graphics,
                offsetX,
                offsetY,
                menu.getSlots(appeng.menu.SlotSemantics.BLANK_PATTERN),
                LATEST_BLANK_PATTERN);
        drawSlotIcon(
                graphics,
                offsetX,
                offsetY,
                menu.getSlots(appeng.menu.SlotSemantics.ENCODED_PATTERN),
                LATEST_ENCODED_PATTERN);
    }

    @Override
    public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(graphics, offsetX, offsetY, mouseX, mouseY);
        int color = screenStyle.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
        graphics.drawString(
                font,
                Component.translatable("gui.ae2.PatternEncoding"),
                8,
                imageHeight - PATTERN_TITLE_BOTTOM,
                color,
                false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        colorPicker.render(graphics, font, mouseX, mouseY);
    }

    @Override
    public void renderCustomSlotHighlight(GuiGraphics graphics, int x, int y, int z) {
        // The 1.20.1 AE2 mixin invokes this while Vanilla is still rendering its
        // slot batch. The newer multi-part highlight is drawn before tooltips.
    }

    private void drawLatestSlotHighlight(GuiGraphics graphics) {
        if (hoveredSlot == null || !hoveredSlot.isActive()) {
            return;
        }

        graphics.flush();
        int width = 16;
        int height = 16;
        if (hoveredSlot instanceof ResizableSlot resizableSlot) {
            width = resizableSlot.getWidth();
            height = resizableSlot.getHeight();
        }

        int x = leftPos + hoveredSlot.x;
        int y = topPos + hoveredSlot.y;
        graphics.fill(x, y, x + width, y + height, 0x669cd3ff);
        graphics.hLine(x, x + width, y - 1, 0xffdaffff);
        graphics.hLine(x - 1, x + width, y + height, 0xffdaffff);
        graphics.vLine(x - 1, y - 2, y + height, 0xffdaffff);
        graphics.vLine(x + width, y - 2, y + height, 0xffdaffff);
        graphics.flush();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPicker.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (minecraft.options.keyPickItem.matchesMouse(button)) {
            Slot slot = getSlotUnderMouse();
            if (menu.canModifyAmountForSlot(slot)) {
                GenericStack currentStack = GenericStack.fromItemStack(slot.getItem());
                if (currentStack != null) {
                    var amountScreen = new AdvancedSetPatternAmountScreen(
                            this,
                            currentStack,
                            newStack -> NetworkHandler.instance().sendToServer(new InventoryActionPacket(
                                    InventoryAction.SET_FILTER,
                                    slot.index,
                                    GenericStack.wrapInItemStack(newStack))));
                    switchToScreen(amountScreen);
                    return true;
                }
            }
        }
        if (menu.getSpecializedMode() == SpecializedPatternMode.ADVANCED
                && button == 0
                && isOverColumnScrollbar(mouseX, mouseY)) {
            draggingColumnScrollbar = true;
            updateColumnScrollFromMouse(mouseX);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingColumnScrollbar) {
            draggingColumnScrollbar = false;
            return true;
        }
        if (colorPicker.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingColumnScrollbar) {
            updateColumnScrollFromMouse(mouseX);
            return true;
        }
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
        if (menu.getSpecializedMode() == SpecializedPatternMode.PACKAGE) {
            if (isOverPackageGrid(mouseX, mouseY)
                    && packageRowScrollbar.onMouseWheel(
                            new Point((int) mouseX - leftPos, (int) mouseY - topPos),
                            delta)) {
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        boolean horizontalScroll = isOverColumnScrollbar(mouseX, mouseY)
                || isOverColumnHeaders(mouseX, mouseY)
                || (Screen.hasShiftDown() && isOverInputPanel(mouseX, mouseY));
        if (horizontalScroll && maxScrollColumn() > 0) {
            scrollColumn = Math.max(0, Math.min(maxScrollColumn(), scrollColumn + (delta < 0 ? 1 : -1)));
            return true;
        }
        if (isOverProcessingGrid(mouseX, mouseY)
                && rowScrollbar.onMouseWheel(
                        new Point((int) mouseX - leftPos, (int) mouseY - topPos),
                        delta)) {
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
    protected EmptyingAction getEmptyingAction(Slot slot, ItemStack carried) {
        if (menu.isProcessingPatternSlot(slot)) {
            EmptyingAction action = ContainerItemStrategies.getEmptyingAction(carried);
            if (action != null) {
                return action;
            }
        }
        return super.getEmptyingAction(slot, carried);
    }

    @Override
    public void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        if (isCraftablePackageInput(slot)) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 100);
            StackSizeRenderer.renderSizeLabel(graphics, font, slot.x - 11, slot.y - 11, "+", false);
            graphics.pose().popPose();
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = super.getTooltipFromContainerItem(stack);
        if (isCraftablePackageInput(hoveredSlot)) {
            lines = new ArrayList<>(lines);
            lines.add(ButtonToolTips.Craftable.text().withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        if (colorPicker.isOpen()) {
            return;
        }
        drawLatestSlotHighlight(graphics);
        AppEngSlot marker = menu.getPackageMarkerSlot();
        if (menu.getSpecializedMode() == SpecializedPatternMode.PACKAGE
                && hoveredSlot == marker
                && marker.getItem().isEmpty()) {
            ModernSlotRendering.drawEmptyMarkerTooltip(this, graphics, x, y, marker);
            return;
        }
        if (menu.getCarried().isEmpty() && menu.canModifyAmountForSlot(hoveredSlot)) {
            List<Component> tooltip = new ArrayList<>(getTooltipFromContainerItem(hoveredSlot.getItem()));
            GenericStack stack = GenericStack.fromItemStack(hoveredSlot.getItem());
            if (stack != null) {
                tooltip.add(Tooltips.getAmountTooltip(ButtonToolTips.Amount, stack));
            }
            tooltip.add(Tooltips.getSetAmountTooltip());
            drawTooltip(graphics, x, y, tooltip);
        } else {
            super.renderTooltip(graphics, x, y);
        }
    }

    @Override
    public void onClose() {
        if (AEConfig.instance().isClearGridOnClose()) {
            menu.clear();
        }
        super.onClose();
    }

    private void applyScreenProfile(int styledLeft, int styledTop) {
        int rows = networkRows();
        imageWidth = SCREEN_WIDTH;
        imageHeight = TERMINAL_HEADER_HEIGHT
                + rows * TERMINAL_ROW_HEIGHT
                + BOTTOM_HEIGHT;

        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        int deltaX = leftPos - styledLeft;
        int deltaY = topPos - styledTop;
        for (GuiEventListener child : children()) {
            if (child instanceof AbstractWidget widget) {
                widget.setX(widget.getX() + deltaX);
                widget.setY(widget.getY() + deltaY);
            }
        }
    }

    private int networkRows() {
        long repoSlots = menu.slots.stream().filter(RepoSlot.class::isInstance).count();
        return Math.max(2, (int) Math.ceil(repoSlots / 9.0));
    }

    private void drawTerminalBackground(
            GuiGraphics graphics,
            int offsetX,
            int offsetY,
            int mouseX,
            int mouseY,
            float partialTicks) {
        ResourceLocation baseTexture = ADVANCED_SCREEN_TEXTURE;
        int y = offsetY;
        drawBackgroundSegment(
                graphics,
                baseTexture,
                256,
                256,
                0,
                TERMINAL_HEADER_HEIGHT,
                offsetX,
                y);
        y += TERMINAL_HEADER_HEIGHT;

        int rows = networkRows();
        for (int row = 0; row < rows; row++) {
            if (row == 0) {
                drawBackgroundSegment(graphics, baseTexture, 256, 256, 17, 18, offsetX, y);
            } else if (row == rows - 1) {
                drawBackgroundSegment(graphics, baseTexture, 256, 256, 35, 18, offsetX, y);
            } else {
                drawBackgroundSegment(
                        graphics,
                        ADVANCED_MIDDLE_ROW_TEXTURE,
                        195,
                        18,
                        0,
                        18,
                        offsetX,
                        y);
            }
            y += TERMINAL_ROW_HEIGHT;
        }

        drawBackgroundSegment(
                graphics,
                baseTexture,
                256,
                256,
                53,
                BOTTOM_HEIGHT,
                offsetX,
                y);

        var storageAccessor = (MEStorageScreenAccessor) this;
        if (storageAccessor.appliedpackaging$getRepo().hasPinnedRow()) {
            Blitter.texture("guis/terminal.png")
                    .src(0, 204, 162, 18)
                    .dest(offsetX + 7, offsetY + TERMINAL_HEADER_HEIGHT)
                    .blit(graphics);
        }
        storageAccessor.appliedpackaging$getSearchField().render(graphics, mouseX, mouseY, partialTicks);
    }

    private static void drawBackgroundSegment(
            GuiGraphics graphics,
            ResourceLocation texture,
            int textureWidth,
            int textureHeight,
            int sourceY,
            int segmentHeight,
            int destinationX,
            int destinationY) {
        Blitter.texture(texture, textureWidth, textureHeight)
                .src(0, sourceY, SCREEN_WIDTH, segmentHeight)
                .dest(destinationX, destinationY)
                .blit(graphics);
    }

    private void updateSpecializedSlots(boolean advanced) {
        setSlotsActive(appeng.menu.SlotSemantics.CRAFTING_GRID, false);
        setSlotsActive(appeng.menu.SlotSemantics.SMITHING_TABLE_TEMPLATE, false);
        setSlotsActive(appeng.menu.SlotSemantics.SMITHING_TABLE_BASE, false);
        setSlotsActive(appeng.menu.SlotSemantics.SMITHING_TABLE_ADDITION, false);
        setSlotsActive(appeng.menu.SlotSemantics.SMITHING_TABLE_RESULT, false);
        setSlotsActive(appeng.menu.SlotSemantics.STONECUTTING_INPUT, false);
        for (Slot slot : super.getMenu().getProcessingInputSlots()) {
            if (slot instanceof appeng.menu.slot.AppEngSlot appEngSlot) {
                appEngSlot.setActive(false);
                setSlotPosition(slot, HIDDEN_SLOT, HIDDEN_SLOT);
            }
        }
        for (Slot slot : super.getMenu().getProcessingOutputSlots()) {
            if (slot instanceof appeng.menu.slot.AppEngSlot appEngSlot) {
                appEngSlot.setActive(false);
                setSlotPosition(slot, HIDDEN_SLOT, HIDDEN_SLOT);
            }
        }

        if (advanced) {
            updateAdvancedSlots();
        } else {
            updatePackageSlots();
        }
    }

    private void updateAdvancedSlots() {
        setSlotsActive(appeng.menu.SlotSemantics.CRAFTING_RESULT, false);
        Slot result = menu.getSlots(appeng.menu.SlotSemantics.CRAFTING_RESULT).get(0);
        setSlotPosition(result, HIDDEN_SLOT, HIDDEN_SLOT);
        menu.getPackageMarkerSlot().setActive(false);
        setSlotPosition(menu.getPackageMarkerSlot(), HIDDEN_SLOT, HIDDEN_SLOT);
        for (AppEngSlot slot : menu.getPackageInputSlots()) {
            slot.setActive(false);
            setSlotPosition(slot, HIDDEN_SLOT, HIDDEN_SLOT);
        }

        int inputY = imageHeight - INPUT_BOTTOM;
        int rowScroll = rowScrollbar.getCurrentScroll();
        var inputs = menu.getAdvancedInputSlots();
        for (int slotIndex = 0; slotIndex < inputs.length; slotIndex++) {
            var slot = inputs[slotIndex];
            int column = slotIndex / AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
            int row = slotIndex % AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
            int visibleColumn = column - scrollColumn;
            int visibleRow = row - rowScroll;
            boolean active = column < menu.activeColumns()
                    && visibleColumn >= 0
                    && visibleColumn < VISIBLE_COLUMNS
                    && visibleRow >= 0
                    && visibleRow < VISIBLE_ROWS;
            slot.setActive(active);
            setSlotPosition(
                    slot,
                    active ? INPUT_X + visibleColumn * COLUMN_STEP : HIDDEN_SLOT,
                    active ? inputY + visibleRow * SLOT_STEP : HIDDEN_SLOT);
        }

        var outputs = menu.getAdvancedOutputSlots();
        for (int slotIndex = 0; slotIndex < outputs.length; slotIndex++) {
            var slot = outputs[slotIndex];
            int visibleRow = slotIndex - rowScroll;
            boolean active = slotIndex < AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS
                    && visibleRow >= 0
                    && visibleRow < VISIBLE_ROWS;
            slot.setActive(active);
            setSlotPosition(slot, active ? OUTPUT_X : HIDDEN_SLOT, active ? inputY + visibleRow * SLOT_STEP : HIDDEN_SLOT);
        }
    }

    private void updatePackageSlots() {
        for (AppEngSlot slot : menu.getAdvancedInputSlots()) {
            slot.setActive(false);
            setSlotPosition(slot, HIDDEN_SLOT, HIDDEN_SLOT);
        }
        for (AppEngSlot slot : menu.getAdvancedOutputSlots()) {
            slot.setActive(false);
            setSlotPosition(slot, HIDDEN_SLOT, HIDDEN_SLOT);
        }

        int firstRow = packageRowScrollbar.getCurrentScroll();
        int inputY = imageHeight - PACKAGE_INPUT_BOTTOM;
        AppEngSlot[] inputs = menu.getPackageInputSlots();
        for (int slotIndex = 0; slotIndex < inputs.length; slotIndex++) {
            int visibleRow = slotIndex / PACKAGE_COLUMNS - firstRow;
            boolean active = visibleRow >= 0 && visibleRow < PACKAGE_VISIBLE_ROWS;
            AppEngSlot slot = inputs[slotIndex];
            slot.setActive(active);
            slot.setHideAmount(false);
            setSlotPosition(
                    slot,
                    active ? PACKAGE_INPUT_X + slotIndex % PACKAGE_COLUMNS * SLOT_STEP : HIDDEN_SLOT,
                    active ? inputY + visibleRow * SLOT_STEP : HIDDEN_SLOT);
        }

        Slot result = menu.getSlots(appeng.menu.SlotSemantics.CRAFTING_RESULT).get(0);
        if (result instanceof AppEngSlot appEngSlot) {
            appEngSlot.setActive(true);
        }
        setSlotPosition(result, PACKAGE_RESULT_X, imageHeight - PACKAGE_PANEL_BOTTOM + PACKAGE_RESULT_RELATIVE_Y);

        AppEngSlot marker = menu.getPackageMarkerSlot();
        marker.setActive(true);
        setSlotPosition(marker, PACKAGE_MARKER_X, imageHeight - PACKAGE_PANEL_BOTTOM + PACKAGE_MARKER_RELATIVE_Y);
    }

    private void setSlotsActive(appeng.menu.SlotSemantic semantic, boolean active) {
        for (Slot slot : menu.getSlots(semantic)) {
            if (slot instanceof appeng.menu.slot.AppEngSlot appEngSlot) {
                appEngSlot.setActive(active);
            }
        }
    }

    private void layoutDynamicWidgets() {
        boolean advanced = menu.getSpecializedMode() == SpecializedPatternMode.ADVANCED;
        layoutProfileSlots();

        int colorButtonY = topPos + imageHeight - HEADER_COLOR_BOTTOM;
        int actionButtonY = topPos + imageHeight - HEADER_ACTION_BOTTOM;
        for (int visibleColumn = 0; visibleColumn < VISIBLE_COLUMNS; visibleColumn++) {
            int column = scrollColumn + visibleColumn;
            PackageColorPicker.TriggerButton colorButton = columnColorButtons[visibleColumn];
            colorButton.setX(leftPos + HEADER_COLOR_X + visibleColumn * COLUMN_STEP);
            colorButton.setY(colorButtonY);
            colorButton.visible = advanced && column < menu.activeColumns();
            colorButton.active = colorButton.visible && !colorPicker.isOpen();

            ColumnActionButton actionButton = columnActionButtons[visibleColumn];
            actionButton.column = column;
            actionButton.plus = column == menu.activeColumns();
            actionButton.setX(leftPos + HEADER_ACTION_X + visibleColumn * COLUMN_STEP);
            actionButton.setY(actionButtonY);
            actionButton.visible = advanced && (column < menu.activeColumns()
                            || (actionButton.plus
                                    && menu.activeColumns()
                                            < AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS));
            actionButton.active = actionButton.visible && !colorPicker.isOpen();
            actionButton.setTooltip(Tooltip.create(Component.translatable(
                    actionButton.plus
                            ? "gui.appliedpackaging.advanced_pattern_terminal.add_column"
                            : "gui.appliedpackaging.advanced_pattern_terminal.clear_or_delete_column",
                    column + 1)));
        }

        clearButton.setX(leftPos + (advanced ? CLEAR_BUTTON_X : PACKAGE_PANEL_LEFT + 72));
        clearButton.setY(topPos + imageHeight - (advanced ? CLEAR_BUTTON_BOTTOM : PACKAGE_INPUT_BOTTOM));
        encodeButton.setX(leftPos + ENCODE_BUTTON_X);
        encodeButton.setY(topPos + imageHeight - ENCODE_BUTTON_BOTTOM);

        packageColorButton.setX(leftPos + PACKAGE_PANEL_LEFT + 82);
        packageColorButton.setY(topPos + imageHeight - PACKAGE_INPUT_BOTTOM);

        int modeButtonX = leftPos + imageWidth - MODE_BUTTON_SIZE;
        int modeButtonY = topPos
                + TERMINAL_HEADER_HEIGHT
                + networkRows() * TERMINAL_ROW_HEIGHT
                + MODE_BUTTON_TOP_GAP;
        packageModeButton.setX(modeButtonX);
        packageModeButton.setY(modeButtonY);
        advancedModeButton.setX(modeButtonX);
        advancedModeButton.setY(modeButtonY + MODE_BUTTON_STEP);

        rowScrollbar.setPosition(new Point(12, imageHeight - INPUT_BOTTOM));
        packageRowScrollbar.setPosition(new Point(15, imageHeight - PACKAGE_INPUT_BOTTOM));
        cycleOutputButton.setX(leftPos + 106);
        cycleOutputButton.setY(topPos + imageHeight - HEADER_COLOR_BOTTOM);

        var storageAccessor = (MEStorageScreenAccessor) this;
        var networkScrollbar = storageAccessor.appliedpackaging$getNetworkScrollbar();
        networkScrollbar.setPosition(new Point(175, 18));
        var searchField = storageAccessor.appliedpackaging$getSearchField();
        searchField.setX(leftPos + 80);
        searchField.setY(topPos + 4);
        searchField.setWidth(89);

        if (legacyCraftingStatusButton != null) {
            int statusX = leftPos + SCREEN_WIDTH - 24;
            int statusY = topPos - 5;
            craftingStatusButton.setX(statusX);
            craftingStatusButton.setY(statusY);
            legacyCraftingStatusButton.setX(statusX - 1);
            legacyCraftingStatusButton.setY(statusY - 1);
        }

    }

    private void layoutProfileSlots() {
        positionGrid(menu.getSlots(appeng.menu.SlotSemantics.PLAYER_INVENTORY), 8, imageHeight - 84, 9);
        positionGrid(menu.getSlots(appeng.menu.SlotSemantics.PLAYER_HOTBAR), 8, imageHeight - 26, 9);

        List<Slot> blankPatterns = menu.getSlots(appeng.menu.SlotSemantics.BLANK_PATTERN);
        if (!blankPatterns.isEmpty()) {
            setSlotPosition(blankPatterns.get(0), BLANK_PATTERN_X, imageHeight - BLANK_PATTERN_BOTTOM);
        }
        List<Slot> encodedPatterns = menu.getSlots(appeng.menu.SlotSemantics.ENCODED_PATTERN);
        if (!encodedPatterns.isEmpty()) {
            setSlotPosition(encodedPatterns.get(0), ENCODED_PATTERN_X, imageHeight - ENCODED_PATTERN_BOTTOM);
        }
    }

    private void positionGrid(List<Slot> slots, int firstX, int firstY, int columns) {
        for (int index = 0; index < slots.size(); index++) {
            setSlotPosition(
                    slots.get(index),
                    firstX + index % columns * SLOT_STEP,
                    firstY + index / columns * SLOT_STEP);
        }
    }

    private void drawAdvancedInputSlotBackgrounds(GuiGraphics graphics, int offsetX, int offsetY) {
        int inputY = imageHeight - INPUT_BOTTOM;
        for (int visibleColumn = 0; visibleColumn < VISIBLE_COLUMNS; visibleColumn++) {
            int column = scrollColumn + visibleColumn;
            if (column >= menu.activeColumns()) {
                continue;
            }
            for (int visibleRow = 0; visibleRow < VISIBLE_ROWS; visibleRow++) {
                // AE2 v19 draws optional slot material as the complete 18x18
                // SLOT_BACKGROUND sprite at slot - 1. Inactive columns retain the
                // artwork already present in the terminal atlas.
                LATEST_SLOT_BACKGROUND
                        .dest(
                                offsetX + INPUT_X + visibleColumn * COLUMN_STEP - 1,
                                offsetY + inputY + visibleRow * SLOT_STEP - 1)
                        .blit(graphics);
            }
        }
    }

    private void drawPrimaryOutputOverlay(GuiGraphics graphics, int offsetX, int offsetY) {
        Slot primaryOutput = menu.getAdvancedOutputSlots()[0];
        if (primaryOutput.isActive() && !primaryOutput.hasItem()) {
            LATEST_PRIMARY_OUTPUT
                    .dest(offsetX + primaryOutput.x, offsetY + primaryOutput.y)
                    .blit(graphics);
        }
    }

    private void drawPackagePanel(GuiGraphics graphics, int offsetX, int offsetY) {
        int panelX = offsetX + PACKAGE_PANEL_LEFT;
        int panelY = offsetY + imageHeight - PACKAGE_PANEL_BOTTOM;
        PACKAGE_PANEL.dest(panelX, panelY).blit(graphics);
        ModernSlotRendering.drawMarkerSlotIcon(
                graphics,
                offsetX,
                offsetY,
                menu.getPackageMarkerSlot(),
                1.0F);
    }

    private void drawColumnScrollbar(GuiGraphics graphics, int offsetX, int offsetY) {
        int x = offsetX + COLUMN_SCROLLBAR_X;
        int y = offsetY + imageHeight - COLUMN_SCROLLBAR_BOTTOM;
        int max = maxScrollColumn();
        if (max <= 0) {
            COLUMN_SCROLLBAR.dest(x, y).blit(graphics);
            return;
        }
        int travel = COLUMN_SCROLLBAR_TRACK_WIDTH - 15;
        int thumbX = x + Math.round(travel * (scrollColumn / (float) max));
        COLUMN_SCROLLBAR.dest(thumbX, y).blit(graphics);
    }

    private void drawSlotIcon(
            GuiGraphics graphics,
            int offsetX,
            int offsetY,
            List<Slot> slots,
            Blitter icon) {
        if (!slots.isEmpty() && !slots.get(0).hasItem()) {
            Slot slot = slots.get(0);
            icon.dest(offsetX + slot.x, offsetY + slot.y).blit(graphics);
        }
    }

    private void clearLegacyPatternSlotIcons() {
        clearLegacyPatternSlotIcon(appeng.menu.SlotSemantics.BLANK_PATTERN);
        clearLegacyPatternSlotIcon(appeng.menu.SlotSemantics.ENCODED_PATTERN);
    }

    private void clearLegacyPatternSlotIcon(appeng.menu.SlotSemantic semantic) {
        for (Slot slot : menu.getSlots(semantic)) {
            if (slot instanceof AppEngSlot appEngSlot) {
                appEngSlot.setIcon(null);
            }
        }
    }

    private void replaceCraftingStatusButton() {
        legacyCraftingStatusButton = null;
        Component craftingStatus = GuiText.CraftingStatus.text();
        for (GuiEventListener child : children()) {
            if (child instanceof TabButton tabButton && tabButton.getMessage().equals(craftingStatus)) {
                legacyCraftingStatusButton = tabButton;
                break;
            }
        }
        if (legacyCraftingStatusButton == null) {
            return;
        }

        int x = legacyCraftingStatusButton.getX();
        int y = legacyCraftingStatusButton.getY();
        craftingStatusButton.setX(x);
        craftingStatusButton.setY(y);
        craftingStatusButton.setWidth(legacyCraftingStatusButton.getWidth());
        craftingStatusButton.setHeight(legacyCraftingStatusButton.getHeight());

        // The 1.20.1 foreground counter assumes a 16px icon. Offset its hidden
        // anchor so the count lands at the 18px icon position used by newer AE2.
        legacyCraftingStatusButton.setX(x - 1);
        legacyCraftingStatusButton.setY(y - 1);
        legacyCraftingStatusButton.visible = false;
    }

    private void openColumnEditor(int column, AbstractWidget anchor) {
        if (column >= 0 && column < menu.activeColumns()) {
            editedColumn = column;
            colorPicker.openNear(
                    anchor,
                    width,
                    height,
                    false,
                    () -> Optional.of(menu.columnColor(column)),
                    selection -> selection.ifPresent(color -> menu.setColumnColor(column, color)),
                    () -> {
                        editedColumn = -1;
                        setFocused(null);
                    });
        }
    }

    private void openPackageColorPicker() {
        colorPicker.openNear(
                packageColorButton,
                width,
                height,
                false,
                () -> Optional.of(menu.getPackageColor()),
                selection -> selection.ifPresent(menu::setPackageColor),
                () -> setFocused(null));
        setFocused(null);
    }

    private void openColumnEditor(int column) {
        for (int visibleColumn = 0; visibleColumn < columnColorButtons.length; visibleColumn++) {
            if (scrollColumn + visibleColumn == column) {
                openColumnEditor(column, columnColorButtons[visibleColumn]);
                return;
            }
        }
    }

    private void closeColumnEditor() {
        colorPicker.close();
        editedColumn = -1;
        setFocused(null);
    }

    private int visibleColumnCount() {
        return Math.min(
                AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS,
                menu.activeColumns()
                        + (menu.activeColumns() < AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS ? 1 : 0));
    }

    private int maxScrollColumn() {
        return Math.max(0, visibleColumnCount() - VISIBLE_COLUMNS);
    }

    private boolean isOverInputPanel(double mouseX, double mouseY) {
        int x = leftPos + INPUT_X;
        int y = topPos + imageHeight - INPUT_BOTTOM;
        return mouseX >= x && mouseX < x + INPUT_PANEL_WIDTH
                && mouseY >= y && mouseY < y + VISIBLE_ROWS * SLOT_STEP;
    }

    private boolean isOverColumnHeaders(double mouseX, double mouseY) {
        int x = leftPos + HEADER_COLOR_X;
        int y = topPos + imageHeight - HEADER_COLOR_BOTTOM;
        return mouseX >= x && mouseX < x + INPUT_PANEL_WIDTH
                && mouseY >= y && mouseY < y + HEADER_BUTTON_SIZE;
    }

    private boolean isOverProcessingGrid(double mouseX, double mouseY) {
        int x = leftPos + INPUT_X;
        int y = topPos + imageHeight - INPUT_BOTTOM;
        return mouseX >= x && mouseX < leftPos + OUTPUT_X + 16
                && mouseY >= y && mouseY < y + VISIBLE_ROWS * SLOT_STEP;
    }

    private boolean isOverPackageGrid(double mouseX, double mouseY) {
        int x = leftPos + PACKAGE_INPUT_X;
        int y = topPos + imageHeight - PACKAGE_INPUT_BOTTOM;
        return mouseX >= x && mouseX < x + PACKAGE_COLUMNS * SLOT_STEP
                && mouseY >= y && mouseY < y + PACKAGE_VISIBLE_ROWS * SLOT_STEP;
    }

    private boolean isCraftablePackageInput(Slot slot) {
        if (menu.getSpecializedMode() != SpecializedPatternMode.PACKAGE || slot == null) {
            return false;
        }
        boolean packageInput = false;
        for (Slot input : menu.getPackageInputSlots()) {
            if (input == slot) {
                packageInput = true;
                break;
            }
        }
        if (!packageInput) {
            return false;
        }
        GenericStack content = GenericStack.fromItemStack(slot.getItem());
        return content != null && repo.isCraftable(content.what());
    }

    private boolean isOverColumnScrollbar(double mouseX, double mouseY) {
        int x = leftPos + COLUMN_SCROLLBAR_X;
        int y = topPos + imageHeight - COLUMN_SCROLLBAR_BOTTOM;
        return mouseX >= x && mouseX < x + COLUMN_SCROLLBAR_TRACK_WIDTH
                && mouseY >= y && mouseY < y + 8;
    }

    private void updateColumnScrollFromMouse(double mouseX) {
        int max = maxScrollColumn();
        if (max <= 0) {
            scrollColumn = 0;
            return;
        }
        double relative = mouseX - (leftPos + COLUMN_SCROLLBAR_X) - 7.5;
        double travel = COLUMN_SCROLLBAR_TRACK_WIDTH - 15.0;
        scrollColumn = Math.max(0, Math.min(max, (int) Math.round(relative / travel * max)));
    }

    private static void setSlotPosition(Slot slot, int x, int y) {
        SlotAccessor accessor = (SlotAccessor) slot;
        accessor.appliedpackaging$setX(x);
        accessor.appliedpackaging$setY(y);
    }

    /** Current-AE Pattern Encoding Terminal horizontal side-tab presentation. */
    private final class PatternModeButton extends AbstractButton {
        private final Blitter icon;
        private final Runnable action;
        private boolean selected;

        private PatternModeButton(Blitter icon, Component message, Runnable action) {
            super(0, 0, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, message);
            this.icon = icon;
            this.action = action;
            setTooltip(Tooltip.create(message));
        }

        private void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Blitter background = isFocused()
                    ? HORIZONTAL_MODE_TAB_FOCUS
                    : selected ? HORIZONTAL_MODE_TAB_SELECTED : HORIZONTAL_MODE_TAB;
            background.dest(getX(), getY()).blit(graphics);

            icon.dest(getX() + 3, getY() + 2).blit(graphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class ColumnActionButton extends AbstractButton {
        private int column;
        private boolean plus;

        private ColumnActionButton(int visibleColumn) {
            super(
                    0,
                    0,
                    HEADER_BUTTON_SIZE,
                    HEADER_BUTTON_SIZE,
                    Component.translatable(
                            "gui.appliedpackaging.advanced_pattern_terminal.clear_or_delete_column",
                            visibleColumn + 1));
        }

        @Override
        public void onPress() {
            if (plus) {
                menu.addColumn();
            } else {
                menu.clearOrDeleteColumn(column);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            (plus ? ADD_COLUMN_BUTTON : DELETE_COLUMN_BUTTON).dest(getX(), getY()).blit(graphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class CompactClearButton extends AbstractButton {
        private CompactClearButton() {
            super(0, 0, 8, 8, ButtonToolTips.Clear.text());
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            menu.clear();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            CLEAR_BUTTON.dest(getX(), getY()).blit(graphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class LatestEncodeButton extends AbstractButton {
        private LatestEncodeButton() {
            super(0, 0, 16, 16, ButtonToolTips.Encode.text());
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            menu.encode();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int yOffset = isHovered() ? 1 : 0;
            Blitter background = isHovered()
                    ? LATEST_ENCODE_BUTTON_HOVER
                    : isFocused() ? LATEST_ENCODE_BUTTON_FOCUS : LATEST_ENCODE_BUTTON;
            background.dest(getX() - 1, getY() + yOffset).blit(graphics);
            LATEST_ENCODE_ICON.dest(getX(), getY() + 1 + yOffset).blit(graphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class LatestCraftingStatusButton extends AbstractButton {
        private LatestCraftingStatusButton() {
            super(0, 0, 20, 20, GuiText.CraftingStatus.text());
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            if (legacyCraftingStatusButton != null) {
                legacyCraftingStatusButton.onPress();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Blitter background = isFocused() ? LATEST_TAB_BUTTON_FOCUS : LATEST_TAB_BUTTON;
            background.dest(getX(), getY()).blit(graphics);
            LATEST_CRAFT_HAMMER.dest(getX() + 2, getY() + 1).blit(graphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}

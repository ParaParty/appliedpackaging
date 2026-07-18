/*
 * Portions of the toolbar, priority-tab, and Storage Bus presentation are
 * adapted from Applied Energistics 2 commit
 * 45f315517ea346efc0babd02c85c6b9d32dc8acf (LGPL-3.0-or-later).
 */
package com.warmthdawn.appliedpackaging.client.screen;

import appeng.api.config.AccessRestriction;
import appeng.api.config.ActionItems;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.api.config.YesNo;
import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.TabButton;
import appeng.client.gui.widgets.ToolboxPanel;
import appeng.client.gui.widgets.ToggleButton;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.SwitchGuisPacket;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.PriorityMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.client.widget.ModernSlotRendering;
import com.warmthdawn.appliedpackaging.client.widget.ModernUpgradesPanel;
import com.warmthdawn.appliedpackaging.client.widget.ModernVerticalToolbar;
import com.warmthdawn.appliedpackaging.client.widget.PackageColorPicker;
import com.warmthdawn.appliedpackaging.client.widget.PackageToolbarSprites;
import com.warmthdawn.appliedpackaging.client.widget.SpriteToggleButton;
import com.warmthdawn.appliedpackaging.mixin.client.SlotAccessor;
import com.warmthdawn.appliedpackaging.part.AbstractPackageBusPart;
import com.warmthdawn.appliedpackaging.world.menu.PackageBusMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Package-bus UI with the current AE2 visual behavior backported to the
 * project's AE2 15 / Minecraft 1.20.1 baseline.
 */
public class PackageBusScreen extends AEBaseScreen<PackageBusMenu> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(
            AppliedPackaging.MOD_ID, "textures/gui/package-storagebus.png");
    private static final ResourceLocation SPRITES = new ResourceLocation(
            AppliedPackaging.MOD_ID, "textures/gui/package-storagebus-sprites.png");
    private static final ResourceLocation LATEST_AE2_STATES = new ResourceLocation(
            AppliedPackaging.MOD_ID, "textures/gui/ae2-states.png");

    // User-provided Package Bus sprites. These coordinates are never baked into
    // or copied over the supplied background texture.
    private static final Blitter SLOT_BACKGROUND = Blitter.texture(SPRITES).src(0, 64, 18, 18);
    private static final Blitter FUZZY_ON = Blitter.texture(SPRITES).src(16, 0, 8, 8);
    private static final Blitter FUZZY_OFF = Blitter.texture(SPRITES).src(24, 0, 8, 8);
    private static final Blitter INVERTED_ON = Blitter.texture(SPRITES).src(16, 8, 8, 8);
    private static final Blitter INVERTED_OFF = Blitter.texture(SPRITES).src(24, 8, 8, 8);

    // Current AE2 assets remain a separate texture, exactly as upstream does.
    private static final Blitter PRIORITY_ICON = Blitter.texture(LATEST_AE2_STATES).src(144, 64, 16, 16);
    private static final Blitter PRIORITY_TAB = Blitter.texture(LATEST_AE2_STATES).src(160, 192, 20, 20);
    private static final Blitter PRIORITY_TAB_FOCUS =
            Blitter.texture(LATEST_AE2_STATES).src(160, 224, 22, 22);
    // The work slot, empty progress frame, and active progress sprite are all
    // supplied in the unused source area of the original Package Bus atlas.
    private static final Blitter WORK_SLOT_BACKGROUND = Blitter.texture(BACKGROUND).src(176, 0, 18, 18);
    private static final Blitter PROGRESS_FRAME = Blitter.texture(BACKGROUND).src(196, 0, 6, 18);
    private static final Blitter PROGRESS_SPRITE = Blitter.texture(BACKGROUND).src(176, 32, 6, 18);

    private static final int BUTTON_X_RIGHT = 30;
    private static final int ROW_Y = 29;
    private static final int ROW_STEP = 18;
    private static final int BUTTON_SIZE = 8;
    private static final int BUTTON_TOP_MARGIN = 2;
    private static final int PROGRESS_X = 139;
    private static final int WORK_SLOT_Y = 8;

    private final PackageColorPicker colorPicker = new PackageColorPicker();
    private final List<PackageColorPicker.TriggerButton> colorButtons = new ArrayList<>();
    private final List<Button> toolbarButtons = new ArrayList<>();
    private final ModernVerticalToolbar modernToolbar = new ModernVerticalToolbar();
    private final SettingToggleButton<AccessRestriction> rwMode;
    private final SettingToggleButton<StorageFilter> storageFilter;
    private final SettingToggleButton<YesNo> filterOnExtract;
    private final SettingToggleButton<FuzzyMode> fuzzyMode;
    private final SettingToggleButton<YesNo> blockingMode;
    private final SpriteToggleButton antiClogMode;

    public PackageBusScreen(
            PackageBusMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        widgets.add("openPriority", new NewPriorityTabButton());
        widgets.add(
                "upgrades",
                new ModernUpgradesPanel(
                        menu.getSlots(SlotSemantics.UPGRADE),
                        () -> {
                            List<Component> lines = new ArrayList<>();
                            lines.add(GuiText.CompatibleUpgrades.text());
                            lines.addAll(Upgrades.getTooltipLinesForMachine(
                                    menu.getUpgrades().getUpgradableItem()));
                            return lines;
                        }));
        if (menu.getToolbox().isPresent()) {
            widgets.add("toolbox", new ToolboxPanel(style, menu.getToolbox().getName()));
        }

        // RestrictedInputSlot comes from the pinned AE2 15 dependency and
        // therefore carries the old grayscale BACKGROUND_UPGRADE icon. The
        // panel itself is backported from current main, so suppress the old
        // icon and draw the matching current-main placeholder in renderSlot.
        ModernSlotRendering.clearLegacyUpgradeIcons(menu.getSlots(SlotSemantics.UPGRADE));

        // AE2 renamed WRENCH to COG in newer lines; the 15.4.10 action is the
        // same partition operation and uses the same cog artwork.
        toolbarButtons.add(new ModernActionButton(ActionItems.CLOSE, button -> menu.clear()));
        if (menu.isUnpackingBus()) {
            blockingMode = new ModernServerSettingToggleButton<>(Settings.BLOCKING_MODE, YesNo.NO);
            toolbarButtons.add(blockingMode);
            antiClogMode = new SpriteToggleButton(
                    PackageToolbarSprites.ANTI_CLOG_ON,
                    PackageToolbarSprites.ANTI_CLOG_OFF,
                    ignored -> menu.toggleAntiClogMode());
            Component antiClogTitle = Component.translatable("gui.appliedpackaging.anti_clog_mode");
            antiClogMode.setTooltipOn(List.of(
                    antiClogTitle,
                    Component.translatable("gui.appliedpackaging.anti_clog_mode.enabled")));
            antiClogMode.setTooltipOff(List.of(
                    antiClogTitle,
                    Component.translatable("gui.appliedpackaging.anti_clog_mode.disabled")));
            toolbarButtons.add(antiClogMode);
            storageFilter = null;
            filterOnExtract = null;
            fuzzyMode = null;
            rwMode = null;
        } else {
            toolbarButtons.add(new ModernActionButton(ActionItems.WRENCH, button -> menu.partition()));
            storageFilter = new ModernServerSettingToggleButton<>(
                    Settings.STORAGE_FILTER, StorageFilter.EXTRACTABLE_ONLY);
            filterOnExtract = new ModernServerSettingToggleButton<>(Settings.FILTER_ON_EXTRACT, YesNo.YES);
            fuzzyMode = new ModernServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
            rwMode = new ModernServerSettingToggleButton<>(Settings.ACCESS, AccessRestriction.READ_WRITE);
            toolbarButtons.add(storageFilter);
            toolbarButtons.add(filterOnExtract);
            toolbarButtons.add(fuzzyMode);
            toolbarButtons.add(rwMode);
            blockingMode = null;
            antiClogMode = null;
        }
        modernToolbar.setButtons(toolbarButtons);

        for (int row = 0; row < AbstractPackageBusPart.FILTER_ROWS; row++) {
            final int rowIndex = row;
            var button = new PackageColorPicker.TriggerButton(
                    BUTTON_SIZE,
                    BUTTON_SIZE,
                    true,
                    () -> menu.isRowColorEnabled(rowIndex)
                            ? Optional.of(menu.rowColor(rowIndex))
                            : Optional.empty(),
                    () -> openColorPicker(rowIndex),
                    () -> menu.clearColor(rowIndex));
            button.setIdleTooltip(Tooltip.create(Component.translatable(
                    "gui.appliedpackaging.package_bus.color", rowIndex + 1)));
            colorButtons.add(button);
        }

    }

    @Override
    protected void init() {
        super.init();
        for (Button button : toolbarButtons) {
            addRenderableWidget(button);
        }
        for (Renderable renderer : modernToolbar.createIconButtonRenderers()) {
            addRenderableOnly(renderer);
        }
        for (var button : colorButtons) {
            addRenderableWidget(button);
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (blockingMode != null) {
            blockingMode.set(menu.getBlockingMode());
            antiClogMode.setState(menu.antiClogMode());
        }
        if (storageFilter != null) {
            storageFilter.set(menu.getStorageFilter());
            filterOnExtract.set(menu.getFilterOnExtract());
            fuzzyMode.set(menu.getFuzzyMode());
            fuzzyMode.setVisibility(menu.supportsFuzzySearch());
            rwMode.set(menu.getReadWriteMode());
        }

        var contentSlots = menu.getSlots(PackageBusMenu.PACKAGE_CONTENTS);
        for (int index = 0; index < contentSlots.size(); index++) {
            SlotAccessor accessor = (SlotAccessor) contentSlots.get(index);
            accessor.appliedpackaging$setX(62 + (index % AbstractPackageBusPart.CONTENTS_PER_ROW) * 18);
            accessor.appliedpackaging$setY(ROW_Y + (index / AbstractPackageBusPart.CONTENTS_PER_ROW) * ROW_STEP);
        }

        setSlotsHidden(PackageBusMenu.WORKING_PACKAGE, !menu.showsWorkingArea());
        for (int row = 0; row < colorButtons.size(); row++) {
            var button = colorButtons.get(row);
            button.setX(leftPos + BUTTON_X_RIGHT);
            button.setY(topPos + rowButtonY(row));
            button.visible = menu.isRowEnabled(row);
            button.active = menu.isRowEnabled(row) && !colorPicker.isOpen();
        }
        modernToolbar.layout(leftPos, topPos);
    }

    @Override
    public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        // Legacy Blitter does not reset Minecraft's global ColorModulator.
        // Current AE2 stores an explicit ARGB color in every BlitRenderState,
        // so normalize the legacy equivalent before the background itself.
        graphics.flush();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        // Current AE2 starts a new GUI stratum before/after this layer and
        // stores an independent TextureSetup per element. GuiGraphics 1.20.1
        // has neither facility, so flushing here is the exact layer boundary
        // needed before the following immediate Blitter submissions.
        graphics.flush();
        modernToolbar.drawPanel(graphics, offsetX, offsetY);
        drawFilterSlotBackgrounds(graphics, offsetX, offsetY);
        if (menu.showsWorkingArea()) {
            drawWorkingArea(graphics, offsetX, offsetY);
        }
        for (int row = 0; row < AbstractPackageBusPart.FILTER_ROWS; row++) {
            drawRowModeButtons(graphics, offsetX, offsetY, row, mouseX, mouseY);
        }

        // GuiGraphics fill/outline is buffered on 1.20.1. Finish this layer
        // before AE2 draws widgets with immediate Blitter texture changes.
        graphics.flush();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.defaultBlendFunc();
    }

    @Override
    public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(graphics, offsetX, offsetY, mouseX, mouseY);

        // Current StorageBusScreen still renders the connected-target hint at
        // this exact position and scale.
        var poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(10, 17, 0);
        poseStack.scale(0.6f, 0.6f, 1);
        int color = style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
        if (menu.getConnectedTo() != null) {
            graphics.drawString(font, GuiText.AttachedTo.text(menu.getConnectedTo()), 0, 0, color, false);
        } else {
            graphics.drawString(font, GuiText.Unattached.text(), 0, 0, color, false);
        }
        poseStack.popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean colorModalOpen = colorPicker.isOpen();
        super.render(graphics, colorModalOpen ? -1 : mouseX, colorModalOpen ? -1 : mouseY, partialTick);
        if (!colorModalOpen) {
            renderModeTooltip(graphics, mouseX, mouseY);
        }
        colorPicker.renderLast(graphics, font, mouseX, mouseY);
    }

    @Override
    public void renderCustomSlotHighlight(GuiGraphics graphics, int x, int y, int z) {
        // Suppress the 1.20.1 highlight; the current AE2 treatment is emitted
        // immediately before tooltips below.
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        if (colorPicker.isOpen()) {
            return;
        }
        ModernSlotRendering.drawSlotHighlight(graphics, leftPos, topPos, hoveredSlot);
        super.renderTooltip(graphics, x, y);
        if (hoveredSlot != null
                && menu.getSlotSemantic(hoveredSlot) == PackageBusMenu.PACKAGE_MARKERS) {
            ModernSlotRendering.drawEmptyMarkerTooltip(this, graphics, x, y, hoveredSlot);
        }
    }

    private void drawFilterSlotBackgrounds(GuiGraphics graphics, int offsetX, int offsetY) {
        List<Slot> markers = menu.getSlots(PackageBusMenu.PACKAGE_MARKERS);
        List<Slot> contents = menu.getSlots(PackageBusMenu.PACKAGE_CONTENTS);
        for (int row = 0; row < AbstractPackageBusPart.FILTER_ROWS; row++) {
            float alpha = menu.isRowEnabled(row) ? 1.0f : 0.2f;
            if (row < markers.size()) {
                Slot marker = markers.get(row);
                drawSlotBackground(graphics, marker, offsetX, offsetY, alpha);
                ModernSlotRendering.drawMarkerSlotIcon(
                        graphics,
                        offsetX,
                        offsetY,
                        marker,
                        alpha);
            }
            for (int column = 0; column < AbstractPackageBusPart.CONTENTS_PER_ROW; column++) {
                int index = row * AbstractPackageBusPart.CONTENTS_PER_ROW + column;
                if (index < contents.size()) {
                    drawSlotBackground(graphics, contents.get(index), offsetX, offsetY, alpha);
                }
            }
        }
    }

    private static void drawSlotBackground(
            GuiGraphics graphics,
            Slot slot,
            int offsetX,
            int offsetY,
            float alpha) {
        SLOT_BACKGROUND.copy()
                .dest(offsetX + slot.x - 1, offsetY + slot.y - 1)
                .color(1, 1, 1, alpha)
                .blit(graphics);
    }

    private void drawWorkingArea(GuiGraphics graphics, int offsetX, int offsetY) {
        WORK_SLOT_BACKGROUND.dest(offsetX + 119, offsetY + WORK_SLOT_Y).blit(graphics);
        PROGRESS_FRAME.dest(offsetX + PROGRESS_X, offsetY + WORK_SLOT_Y).blit(graphics);

        int progress = Math.max(0, Math.min(15, menu.progress()));
        if (progress > 0) {
            int spriteHeight = PROGRESS_SPRITE.getSrcHeight();
            int visibleHeight = spriteHeight * progress / 15;
            int hiddenHeight = spriteHeight - visibleHeight;
            PROGRESS_SPRITE.copy()
                    .src(
                            PROGRESS_SPRITE.getSrcX(),
                            PROGRESS_SPRITE.getSrcY() + hiddenHeight,
                            PROGRESS_SPRITE.getSrcWidth(),
                            visibleHeight)
                    .dest(
                            offsetX + PROGRESS_X,
                            offsetY + WORK_SLOT_Y + hiddenHeight)
                    .blit(graphics);
        }
    }

    @Override
    public void renderSlot(GuiGraphics graphics, Slot slot) {
        if (menu.getSlotSemantic(slot) == SlotSemantics.UPGRADE) {
            ModernSlotRendering.drawUpgradeSlotIcon(graphics, slot);
        }
        super.renderSlot(graphics, slot);
        if (menu.unpackBlocked() && menu.getSlotSemantic(slot) == PackageBusMenu.WORKING_PACKAGE) {
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x66ff3333);
            graphics.renderOutline(slot.x - 1, slot.y - 1, 18, 18, 0xffff5555);
        }
    }

    private static final class NewPriorityTabButton extends TabButton {
        private NewPriorityTabButton() {
            super(ItemStack.EMPTY, GuiText.Priority.text(), button -> NetworkHandler.instance()
                    .sendToServer(SwitchGuisPacket.openSubMenu(PriorityMenu.TYPE)));
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!visible) {
                return;
            }
            (isFocused() ? PRIORITY_TAB_FOCUS : PRIORITY_TAB)
                    .dest(getX(), getY())
                    .blit(graphics);
            PRIORITY_ICON.dest(getX() + 2, getY() + 1).blit(graphics);
        }
    }

    private static final class ModernActionButton extends ActionButton {
        private ModernActionButton(ActionItems action, java.util.function.Consumer<ActionItems> onPress) {
            super(action, onPress);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ModernVerticalToolbar.renderButton(graphics, this, getIcon());
        }
    }

    private static final class ModernServerSettingToggleButton<T extends Enum<T>>
            extends ServerSettingToggleButton<T> {
        private ModernServerSettingToggleButton(appeng.api.config.Setting<T> setting, T initialValue) {
            super(setting, initialValue);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ModernVerticalToolbar.renderButton(graphics, this, getIcon());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPicker.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        int x = leftPos + BUTTON_X_RIGHT;
        if (menu.hasInverterCard()) {
            x -= BUTTON_SIZE + 2;
            int row = rowAt(mouseX, mouseY, x - leftPos);
            if (row >= 0 && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                menu.toggleInverted(row);
                return true;
            }
        }
        if (menu.hasFuzzyCard()) {
            x -= BUTTON_SIZE + 2;
            int row = rowAt(mouseX, mouseY, x - leftPos);
            if (row >= 0 && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                menu.toggleFuzzy(row);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return colorPicker.mouseReleased(mouseX, mouseY, button)
                || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return colorPicker.mouseDragged(mouseX, mouseY, button, dragX, dragY)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return colorPicker.mouseScrolled(mouseX, mouseY, delta)
                || super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return colorPicker.keyPressed(keyCode, scanCode, modifiers)
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return colorPicker.charTyped(codePoint, modifiers)
                || super.charTyped(codePoint, modifiers);
    }

    private void drawRowModeButtons(
            GuiGraphics graphics,
            int offsetX,
            int offsetY,
            int row,
            int mouseX,
            int mouseY) {
        if (!menu.isRowEnabled(row)) {
            return;
        }
        int x = BUTTON_X_RIGHT;
        int y = rowButtonY(row);
        if (menu.hasInverterCard()) {
            x -= BUTTON_SIZE + 2;
            (menu.isRowInverted(row) ? INVERTED_ON : INVERTED_OFF)
                    .dest(offsetX + x, offsetY + y)
                    .blit(graphics);
            renderHover(graphics, offsetX + x, offsetY + y, mouseX, mouseY);
        }
        if (menu.hasFuzzyCard()) {
            x -= BUTTON_SIZE + 2;
            (menu.isRowFuzzy(row) ? FUZZY_ON : FUZZY_OFF)
                    .dest(offsetX + x, offsetY + y)
                    .blit(graphics);
            renderHover(graphics, offsetX + x, offsetY + y, mouseX, mouseY);
        }
    }

    private void renderModeTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + BUTTON_X_RIGHT;
        if (menu.hasInverterCard()) {
            x -= BUTTON_SIZE + 2;
            int row = rowAt(mouseX, mouseY, x - leftPos);
            if (row >= 0) {
                graphics.renderTooltip(font, Component.translatable(
                        menu.isRowInverted(row)
                                ? "gui.appliedpackaging.package_bus.inverted_on"
                                : "gui.appliedpackaging.package_bus.inverted_off"), mouseX, mouseY);
                return;
            }
        }
        if (menu.hasFuzzyCard()) {
            x -= BUTTON_SIZE + 2;
            int row = rowAt(mouseX, mouseY, x - leftPos);
            if (row >= 0) {
                graphics.renderTooltip(font, Component.translatable(
                        menu.isRowFuzzy(row)
                                ? "gui.appliedpackaging.package_bus.fuzzy_on"
                                : "gui.appliedpackaging.package_bus.fuzzy_off"), mouseX, mouseY);
            }
        }
    }

    private void openColorPicker(int row) {
        var button = colorButtons.get(row);
        colorPicker.openNear(
                button,
                width,
                height,
                true,
                () -> menu.isRowColorEnabled(row)
                        ? Optional.of(menu.rowColor(row))
                        : Optional.empty(),
                color -> color.ifPresentOrElse(
                        selected -> menu.setColor(row, selected),
                        () -> menu.clearColor(row)),
                () -> button.active = menu.isRowEnabled(row));
        for (var colorButton : colorButtons) {
            colorButton.active = false;
        }
    }

    private int rowAt(double mouseX, double mouseY, int relativeX) {
        for (int row = 0; row < AbstractPackageBusPart.FILTER_ROWS; row++) {
            int x = leftPos + relativeX;
            int y = topPos + rowButtonY(row);
            if (menu.isRowEnabled(row)
                    && mouseX >= x && mouseX < x + BUTTON_SIZE
                    && mouseY >= y && mouseY < y + BUTTON_SIZE) {
                return row;
            }
        }
        return -1;
    }

    private static int rowButtonY(int row) {
        return ROW_Y + row * ROW_STEP + BUTTON_TOP_MARGIN;
    }

    private static void renderHover(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (mouseX >= x && mouseX < x + BUTTON_SIZE && mouseY >= y && mouseY < y + BUTTON_SIZE) {
            graphics.renderOutline(x, y, BUTTON_SIZE, BUTTON_SIZE, 0xffdaffff);
        }
    }
}

package com.warmthdawn.appliedpackaging.client.widget;

import appeng.client.gui.style.Blitter;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public final class PackageColorPicker {
    private static final ResourceLocation SPRITES = new ResourceLocation(
            AppliedPackaging.MOD_ID, "textures/gui/package-storagebus-sprites.png");
    private static final Blitter DEFAULT_SWATCH = Blitter.texture(SPRITES).src(48, 0, 8, 8);
    private static final Blitter NONE_SWATCH = Blitter.texture(SPRITES).src(56, 0, 8, 8);
    private static final Blitter SELECTED_SWATCH = Blitter.texture(SPRITES).src(48, 8, 8, 8);
    private static final Blitter FLUIX_INTERIOR = Blitter.texture(SPRITES).src(49, 1, 6, 6);
    private static final Blitter NONE_INTERIOR = Blitter.texture(SPRITES).src(57, 1, 6, 6);

    private static final int PADDING = 3;
    private static final int SWATCH_SIZE = 8;
    private static final int SWATCH_STEP = 9;
    private static final int STANDARD_COLUMNS = 8;
    private static final int GROUP_GAP = 4;
    private static final int STANDARD_GRID_X = PADDING + SWATCH_SIZE + GROUP_GAP;
    private static final int STANDARD_GRID_WIDTH = (STANDARD_COLUMNS - 1) * SWATCH_STEP + SWATCH_SIZE;

    public static final int WIDTH = PADDING * 2 + SWATCH_SIZE + GROUP_GAP + STANDARD_GRID_WIDTH;
    public static final int HEIGHT = PADDING * 2 + SWATCH_STEP + SWATCH_SIZE;

    private static final int BORDER = 0xff6d718b;
    private static final int BACKGROUND = 0xffc8cad5;
    private static final int OVERLAY_Z = 400;

    private int x;
    private int y;
    private boolean open;
    private boolean allowNone;
    private boolean consumePointerGesture;
    private TriggerButton triggerAnchor;
    private Supplier<Optional<PackageColor>> selectedColor = () -> Optional.of(PackageColor.FLUIX);
    private Consumer<Optional<PackageColor>> onSelected = ignored -> {
    };
    private Runnable onClosed = () -> {
    };

    public boolean isOpen() {
        return open;
    }

    public void openNear(
            AbstractWidget anchor,
            int screenWidth,
            int screenHeight,
            boolean allowNone,
            Supplier<Optional<PackageColor>> selectedColor,
            Consumer<Optional<PackageColor>> onSelected,
            Runnable onClosed) {
        close();
        Objects.requireNonNull(anchor, "anchor");
        this.selectedColor = Objects.requireNonNull(selectedColor, "selectedColor");
        this.onSelected = Objects.requireNonNull(onSelected, "onSelected");
        this.onClosed = Objects.requireNonNull(onClosed, "onClosed");
        this.allowNone = allowNone;
        if (anchor instanceof TriggerButton triggerButton) {
            this.triggerAnchor = triggerButton;
            triggerButton.setPickerOpen(true);
        }

        int preferredX = anchor.getX();
        int below = anchor.getY() + anchor.getHeight() + 2;
        int above = anchor.getY() - HEIGHT - 2;
        this.x = Mth.clamp(preferredX, 2, Math.max(2, screenWidth - WIDTH - 2));
        this.y = below + HEIGHT <= screenHeight - 2 ? below : Math.max(2, above);
        this.consumePointerGesture = false;
        this.open = true;
    }

    public void close() {
        if (!open) {
            return;
        }
        open = false;
        if (triggerAnchor != null) {
            triggerAnchor.setPickerOpen(false);
            triggerAnchor = null;
        }
        Runnable callback = onClosed;
        onClosed = () -> {
        };
        callback.run();
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        renderAtZ(graphics, font, mouseX, mouseY, OVERLAY_Z);
    }

    public void renderLast(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        renderAtZ(graphics, font, mouseX, mouseY, OVERLAY_Z);
    }

    @SuppressWarnings("deprecation")
    private void renderAtZ(GuiGraphics graphics, Font font, int mouseX, int mouseY, int overlayZ) {
        if (!open) {
            return;
        }

        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, overlayZ);
            PackageColor hovered = colorAt(mouseX, mouseY);
            boolean noneHovered = isNoneSwatch(mouseX, mouseY);
            Optional<PackageColor> selected = currentSelection();
            graphics.drawManaged(() -> {
                graphics.fill(x, y, x + WIDTH, y + HEIGHT, BORDER);
                graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, BACKGROUND);
                graphics.vLine(x + PADDING + SWATCH_SIZE + 1, y + 3, y + HEIGHT - 4, BORDER);
                graphics.flush();

                drawColorSwatch(
                        graphics,
                        x + PADDING,
                        y + PADDING,
                        PackageColor.FLUIX,
                        selected.filter(value -> value == PackageColor.FLUIX).isPresent());
                if (allowNone) {
                    drawNoneSwatch(
                            graphics,
                            x + PADDING,
                            y + PADDING + SWATCH_STEP,
                            selected.isEmpty());
                }

                for (PackageColor color : PackageColor.values()) {
                    if (color == PackageColor.FLUIX) {
                        continue;
                    }
                    drawColorSwatch(
                            graphics,
                            swatchX(color),
                            swatchY(color),
                            color,
                            selected.filter(value -> value == color).isPresent());
                }
                graphics.flush();
            });

            if (noneHovered) {
                graphics.renderTooltip(
                        font,
                        Component.translatable("gui.appliedpackaging.package_color.none"),
                        mouseX,
                        mouseY);
            } else if (hovered != null) {
                graphics.renderTooltip(font, colorName(hovered), mouseX, mouseY);
            }
            graphics.flush();
        } finally {
            graphics.pose().popPose();
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open) {
            return false;
        }
        consumePointerGesture = true;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (isNoneSwatch(mouseX, mouseY)) {
                onSelected.accept(Optional.empty());
                close();
                return true;
            }
            PackageColor color = colorAt(mouseX, mouseY);
            if (color != null) {
                onSelected.accept(Optional.of(color));
                close();
                return true;
            }
        }
        if (!contains(mouseX, mouseY, x, y, WIDTH, HEIGHT)) {
            close();
        }
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (open) {
            consumePointerGesture = false;
            return true;
        }
        if (consumePointerGesture) {
            consumePointerGesture = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return open || consumePointerGesture;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return open;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!open) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
        }
        return true;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return open;
    }

    private Optional<PackageColor> currentSelection() {
        Optional<PackageColor> selection = selectedColor.get();
        return selection == null ? Optional.empty() : selection;
    }

    private PackageColor colorAt(double mouseX, double mouseY) {
        for (PackageColor color : PackageColor.values()) {
            if (contains(mouseX, mouseY, swatchX(color), swatchY(color), SWATCH_SIZE, SWATCH_SIZE)) {
                return color;
            }
        }
        return null;
    }

    private int swatchX(PackageColor color) {
        if (color == PackageColor.FLUIX) {
            return x + PADDING;
        }
        int index = color.ordinal() - 1;
        return x + STANDARD_GRID_X + (index % STANDARD_COLUMNS) * SWATCH_STEP;
    }

    private int swatchY(PackageColor color) {
        if (color == PackageColor.FLUIX) {
            return y + PADDING;
        }
        int index = color.ordinal() - 1;
        return y + PADDING + (index / STANDARD_COLUMNS) * SWATCH_STEP;
    }

    private boolean isNoneSwatch(double mouseX, double mouseY) {
        return allowNone && contains(
                mouseX,
                mouseY,
                x + PADDING,
                y + PADDING + SWATCH_STEP,
                SWATCH_SIZE,
                SWATCH_SIZE);
    }

    private static Component colorName(PackageColor color) {
        if (color == PackageColor.FLUIX) {
            return Component.translatable("gui.appliedpackaging.package_color.fluix");
        }
        return Component.translatable("color.minecraft." + color.translationKeySuffix());
    }

    private static void drawColorSwatch(
            GuiGraphics graphics,
            int swatchX,
            int swatchY,
            PackageColor color,
            boolean selected) {
        (selected ? SELECTED_SWATCH : DEFAULT_SWATCH).dest(swatchX, swatchY).blit(graphics);
        if (color == PackageColor.FLUIX) {
            if (selected) {
                FLUIX_INTERIOR.dest(swatchX + 1, swatchY + 1).blit(graphics);
            }
            return;
        }
        graphics.fill(
                swatchX + 1,
                swatchY + 1,
                swatchX + SWATCH_SIZE - 1,
                swatchY + SWATCH_SIZE - 1,
                color.swatchArgb());
    }

    private static void drawNoneSwatch(GuiGraphics graphics, int swatchX, int swatchY, boolean selected) {
        (selected ? SELECTED_SWATCH : NONE_SWATCH).dest(swatchX, swatchY).blit(graphics);
        if (selected) {
            NONE_INTERIOR.dest(swatchX + 1, swatchY + 1).blit(graphics);
        }
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static final class TriggerButton extends AbstractButton {
        private final boolean allowNone;
        private final Supplier<Optional<PackageColor>> color;
        private final Runnable onPress;
        private final Runnable onNone;
        private Tooltip idleTooltip;

        public TriggerButton(
                int width,
                int height,
                boolean allowNone,
                Supplier<Optional<PackageColor>> color,
                Runnable onPress,
                Runnable onNone) {
            super(0, 0, width, height, Component.translatable("gui.appliedpackaging.package_color.select"));
            this.allowNone = allowNone;
            this.color = Objects.requireNonNull(color, "color");
            this.onPress = Objects.requireNonNull(onPress, "onPress");
            this.onNone = Objects.requireNonNull(onNone, "onNone");
            setIdleTooltip(Tooltip.create(getMessage()));
        }

        public void setIdleTooltip(Tooltip tooltip) {
            this.idleTooltip = Objects.requireNonNull(tooltip, "tooltip");
            super.setTooltip(tooltip);
        }

        private void setPickerOpen(boolean pickerOpen) {
            super.setTooltip(pickerOpen ? null : idleTooltip);
        }

        @Override
        public void onPress() {
            onPress.run();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && allowNone && active && visible && isMouseOver(mouseX, mouseY)) {
                onNone.run();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int swatchX = getX() + (width - SWATCH_SIZE) / 2;
            int swatchY = getY() + (height - SWATCH_SIZE) / 2;
            Optional<PackageColor> selected = color.get();
            if (allowNone && (selected == null || selected.isEmpty())) {
                drawNoneSwatch(graphics, swatchX, swatchY, false);
            } else {
                drawColorSwatch(
                        graphics,
                        swatchX,
                        swatchY,
                        selected == null ? PackageColor.FLUIX : selected.orElse(PackageColor.FLUIX),
                        false);
            }
            graphics.flush();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}

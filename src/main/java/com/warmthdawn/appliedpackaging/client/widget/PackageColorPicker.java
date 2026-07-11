package com.warmthdawn.appliedpackaging.client.widget;

import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public final class PackageColorPicker {
    private static final int PADDING = 3;
    private static final int SWATCH_SIZE = 8;
    private static final int SWATCH_STEP = 9;
    private static final int STANDARD_COLUMNS = 8;
    private static final int GROUP_GAP = 4;
    private static final int STANDARD_GRID_WIDTH = (STANDARD_COLUMNS - 1) * SWATCH_STEP + SWATCH_SIZE;

    public static final int WIDTH = PADDING * 2 + SWATCH_SIZE + GROUP_GAP + STANDARD_GRID_WIDTH;
    public static final int HEIGHT = PADDING * 2 + SWATCH_STEP + SWATCH_SIZE;

    private static final int BORDER = 0xff6d718b;
    private static final int BACKGROUND = 0xffc8cad5;
    private static final int SWATCH_BORDER = 0xff303543;
    private static final int SWATCH_HOVER = 0xffdaffff;
    private static final int SELECTED_BORDER = 0xffffffff;
    private static final int OVERLAY_Z = 400;
    private int x;
    private int y;
    private boolean open;
    private boolean consumePointerGesture;
    private Supplier<PackageColor> selectedColor = () -> PackageColor.FLUIX;
    private Consumer<PackageColor> onSelected = ignored -> {
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
            Supplier<PackageColor> selectedColor,
            Consumer<PackageColor> onSelected,
            Runnable onClosed) {
        Objects.requireNonNull(anchor, "anchor");
        this.selectedColor = Objects.requireNonNull(selectedColor, "selectedColor");
        this.onSelected = Objects.requireNonNull(onSelected, "onSelected");
        this.onClosed = Objects.requireNonNull(onClosed, "onClosed");

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
            graphics.drawManaged(() -> {
                graphics.fill(x, y, x + WIDTH, y + HEIGHT, BORDER);
                graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, BACKGROUND);
                int separatorX = x + PADDING + SWATCH_SIZE + 1;
                graphics.vLine(separatorX, y + 3, y + HEIGHT - 4, BORDER);

                for (PackageColor color : PackageColor.values()) {
                    int swatchX = swatchX(color);
                    int swatchY = swatchY(color);
                    boolean isHovered = color == hovered;
                    boolean selected = selectedColor.get() == color;
                    int border = selected ? SELECTED_BORDER : isHovered ? SWATCH_HOVER : SWATCH_BORDER;
                    graphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, border);
                    graphics.fill(
                            swatchX + 1,
                            swatchY + 1,
                            swatchX + SWATCH_SIZE - 1,
                            swatchY + SWATCH_SIZE - 1,
                            color.swatchArgb());
                    if (selected) {
                        graphics.renderOutline(
                                swatchX - 1,
                                swatchY - 1,
                                SWATCH_SIZE + 2,
                                SWATCH_SIZE + 2,
                                SWATCH_BORDER);
                    }
                }
            });

            if (hovered != null) {
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
            PackageColor color = colorAt(mouseX, mouseY);
            if (color != null) {
                onSelected.accept(color);
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
        return x + PADDING + SWATCH_SIZE + GROUP_GAP + (index % STANDARD_COLUMNS) * SWATCH_STEP;
    }

    private int swatchY(PackageColor color) {
        if (color == PackageColor.FLUIX) {
            return y + (HEIGHT - SWATCH_SIZE) / 2;
        }
        int index = color.ordinal() - 1;
        return y + PADDING + (index / STANDARD_COLUMNS) * SWATCH_STEP;
    }

    private static Component colorName(PackageColor color) {
        if (color == PackageColor.FLUIX) {
            return Component.translatable("gui.appliedpackaging.package_color.fluix");
        }
        return Component.translatable("color.minecraft." + color.translationKeySuffix());
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static final class TriggerButton extends AbstractButton {
        private final Supplier<PackageColor> color;
        private final Runnable onPress;

        public TriggerButton(int width, int height, Supplier<PackageColor> color, Runnable onPress) {
            super(0, 0, width, height, Component.translatable("gui.appliedpackaging.package_color.select"));
            this.color = Objects.requireNonNull(color, "color");
            this.onPress = Objects.requireNonNull(onPress, "onPress");
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        public void onPress() {
            onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int size = Math.min(6, Math.max(2, Math.min(width, height) - 2));
            int swatchX = getX() + (width - size) / 2;
            int swatchY = getY() + (height - size) / 2;
            graphics.fill(swatchX - 1, swatchY - 1, swatchX + size + 1, swatchY + size + 1, SWATCH_BORDER);
            graphics.fill(swatchX, swatchY, swatchX + size, swatchY + size, color.get().swatchArgb());
            if (isHoveredOrFocused()) {
                graphics.renderOutline(getX(), getY(), width, height, SWATCH_HOVER);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}

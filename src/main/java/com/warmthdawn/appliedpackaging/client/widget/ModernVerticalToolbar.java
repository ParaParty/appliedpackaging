/*
 * Backport of Applied Energistics 2 current-main VerticalButtonBar and
 * IconButton presentation at commit
 * 45f315517ea346efc0babd02c85c6b9d32dc8acf (LGPL-3.0-or-later).
 */
package com.warmthdawn.appliedpackaging.client.widget;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import com.mojang.blaze3d.systems.RenderSystem;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Shared current-AE2 toolbar layout and rendering for custom screens. */
public final class ModernVerticalToolbar {
    private static final MethodHandle ICON_BUTTON_GET_ICON = findIconButtonGetter(
            "getIcon", Icon.class);
    private static final MethodHandle ICON_BUTTON_GET_ITEM_OVERLAY = findIconButtonGetter(
            "getItemOverlay", Item.class);

    private static final ResourceLocation LATEST_AE2_STATES = new ResourceLocation(
            AppliedPackaging.MOD_ID, "textures/gui/ae2-states.png");
    private static final ResourceLocation VERTICAL_BUTTONS = new ResourceLocation(
            AppliedPackaging.MOD_ID, "textures/gui/package_bus_vertical_buttons_bg.png");

    private static final Blitter BUTTON = Blitter.texture(LATEST_AE2_STATES).src(176, 128, 18, 20);
    private static final Blitter BUTTON_HOVER = Blitter.texture(LATEST_AE2_STATES).src(212, 128, 18, 20);
    private static final Blitter PANEL_TOP = Blitter.texture(VERTICAL_BUTTONS, 21, 26).src(0, 0, 21, 2);
    private static final Blitter PANEL_MIDDLE = Blitter.texture(VERTICAL_BUTTONS, 21, 26).src(0, 2, 21, 20);
    private static final Blitter PANEL_BOTTOM = Blitter.texture(VERTICAL_BUTTONS, 21, 26).src(0, 22, 21, 4);

    // These values match current-main common.json and VerticalButtonBar.
    public static final int POSITION_X = 3;
    public static final int POSITION_Y = 1;
    private static final int MARGIN = 2;
    private static final int VERTICAL_SPACING = 6;

    private final List<Button> buttons = new ArrayList<>();

    public void setButtons(Iterable<? extends Button> source) {
        buttons.clear();
        for (Button button : source) {
            buttons.add(button);
        }
    }

    public void appendButton(Button button) {
        buttons.remove(button);
        buttons.add(button);
        if (button instanceof IconButton iconButton) {
            iconButton.setDisableBackground(true);
        }
    }

    /**
     * Reuses the full-size IconButtons populated by AEBaseScreen's native
     * toolbar while replacing only their presentation and final layout.
     */
    public void captureIconButtons(Iterable<? extends GuiEventListener> children) {
        buttons.clear();
        for (GuiEventListener child : children) {
            if (child instanceof IconButton button && !button.isHalfSize()) {
                button.setDisableBackground(true);
                buttons.add(button);
            }
        }
    }

    /**
     * Creates render-only overlays after AE2 has populated its native buttons.
     * The modern opaque background replaces the legacy icon pass, then the
     * current states atlas is sampled with the button's live icon coordinates.
     */
    public List<Renderable> createIconButtonRenderers() {
        List<Renderable> renderers = new ArrayList<>();
        for (Button button : buttons) {
            if (button instanceof IconButton iconButton) {
                renderers.add((graphics, mouseX, mouseY, partialTick) -> {
                    Icon icon = getIcon(iconButton);
                    Blitter sprite = iconButton instanceof ModernToolbarSpriteProvider provider
                            ? provider.getModernToolbarSprite()
                            : getCurrentIconOverride(icon);
                    renderButton(
                            graphics,
                            iconButton,
                            icon,
                            sprite,
                            getItemOverlay(iconButton));
                });
            }
        }
        return renderers;
    }

    /** Applies the exact current-main position calculation in window space. */
    public void layout(int screenLeft, int screenTop) {
        int currentY = screenTop + POSITION_Y + MARGIN;
        int xEdge = screenLeft + POSITION_X - MARGIN;
        for (Button button : buttons) {
            if (!button.visible) {
                continue;
            }
            button.setX(xEdge - button.getWidth());
            button.setY(currentY);
            currentY += button.getHeight() + VERTICAL_SPACING;
        }
    }

    /** Draws current-main's stretched vertical_buttons_bg behind the stack. */
    public void drawPanel(GuiGraphics graphics, int screenLeft, int screenTop) {
        int visible = 0;
        int maxWidth = 0;
        int totalButtonHeight = 0;
        for (Button button : buttons) {
            if (!button.visible) {
                continue;
            }
            visible++;
            maxWidth = Math.max(maxWidth, button.getWidth());
            totalButtonHeight += button.getHeight();
        }
        if (visible == 0) {
            return;
        }

        int destinationX = screenLeft + POSITION_X - maxWidth - 2 * MARGIN - 2;
        int destinationY = screenTop + POSITION_Y - 1;
        int destinationWidth = maxWidth + 2 * MARGIN + 1;
        int destinationHeight = 2 * MARGIN
                + totalButtonHeight
                + visible * VERTICAL_SPACING
                + 2;
        PANEL_TOP.dest(destinationX, destinationY, destinationWidth, 2).blit(graphics);
        PANEL_MIDDLE.dest(
                destinationX,
                destinationY + 2,
                destinationWidth,
                destinationHeight - 6).blit(graphics);
        PANEL_BOTTOM.dest(
                destinationX,
                destinationY + destinationHeight - 4,
                destinationWidth,
                4).blit(graphics);
    }

    /** Exact current-main IconButton renderer used by locally owned buttons. */
    public static void renderButton(GuiGraphics graphics, Button button, Icon icon) {
        renderButton(graphics, button, icon, getCurrentIconOverride(icon), null);
    }

    private static void renderButton(
            GuiGraphics graphics,
            Button button,
            Icon icon,
            Blitter spriteOverride,
            Item itemOverlay) {
        if (!button.visible) {
            return;
        }

        int yOffset = button.isHovered() ? 1 : 0;
        // 1.20.1 keeps mouse-clicked widgets focused after the pointer leaves.
        // Rendering that persistent mouse focus with the current-AE2 focus sprite
        // looks like an extra border, so this backport only uses normal/hover states.
        Blitter background = button.isHovered() ? BUTTON_HOVER : BUTTON;

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        background.dest(button.getX() - 1, button.getY() + yOffset).blit(graphics);
        if (itemOverlay != null) {
            graphics.renderItem(
                    new ItemStack(itemOverlay),
                    button.getX(),
                    button.getY() + 1 + yOffset,
                    0,
                    3);
        } else if (spriteOverride != null) {
            spriteOverride.copy()
                    .dest(button.getX(), button.getY() + 1 + yOffset)
                    .blit(graphics);
        } else if (icon != null) {
            Blitter.texture(LATEST_AE2_STATES)
                    .src(icon.x, icon.y, icon.width, icon.height)
                    .dest(button.getX(), button.getY() + 1 + yOffset)
                    .blit(graphics);
        }
        RenderSystem.enableDepthTest();
    }

    private static Blitter getCurrentIconOverride(Icon icon) {
        if (icon == Icon.TYPE_FILTER_ITEMS) {
            return PackageToolbarSprites.ITEMS_ONLY;
        }
        if (icon == Icon.TYPE_FILTER_FLUIDS) {
            return PackageToolbarSprites.FLUIDS_ONLY;
        }
        return null;
    }

    private static MethodHandle findIconButtonGetter(String name, Class<?> returnType) {
        try {
            return MethodHandles.publicLookup().findVirtual(
                    IconButton.class,
                    name,
                    MethodType.methodType(returnType));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("AE2 IconButton access transformer was not applied", e);
        }
    }

    private static Icon getIcon(IconButton button) {
        try {
            return (Icon) ICON_BUTTON_GET_ICON.invokeExact(button);
        } catch (Throwable e) {
            throw new IllegalStateException("Cannot read AE2 IconButton icon", e);
        }
    }

    private static Item getItemOverlay(IconButton button) {
        try {
            return (Item) ICON_BUTTON_GET_ITEM_OVERLAY.invokeExact(button);
        } catch (Throwable e) {
            throw new IllegalStateException("Cannot read AE2 IconButton item overlay", e);
        }
    }
}

package com.warmthdawn.appliedpackaging.client.widget;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ToggleButton;

/** AE2 toggle behavior with user-authored on/off sprites in the modern toolbar. */
public final class SpriteToggleButton extends ToggleButton implements ModernToolbarSpriteProvider {
    private final Blitter spriteOn;
    private final Blitter spriteOff;
    private boolean state;

    public SpriteToggleButton(Blitter spriteOn, Blitter spriteOff, Listener listener) {
        // The modern toolbar renderer replaces this compatibility icon.
        super(Icon.CLEAR, Icon.CLEAR, listener);
        this.spriteOn = spriteOn;
        this.spriteOff = spriteOff;
    }

    @Override
    public void setState(boolean state) {
        super.setState(state);
        this.state = state;
    }

    @Override
    public Blitter getModernToolbarSprite() {
        return (state ? spriteOn : spriteOff).copy();
    }
}

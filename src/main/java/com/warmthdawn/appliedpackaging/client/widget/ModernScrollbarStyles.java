package com.warmthdawn.appliedpackaging.client.widget;

import appeng.client.gui.widgets.Scrollbar;
import com.warmthdawn.appliedpackaging.AppliedPackaging;

/** Shared backport of the current-AE2 small scrollbar sprites. */
public final class ModernScrollbarStyles {
    public static final Scrollbar.Style SMALL = Scrollbar.Style.create(
            AppliedPackaging.id("textures/gui/advanced_pattern_encoding_terminal_sprites.png"),
            7,
            15,
            0,
            32,
            16,
            32);

    private ModernScrollbarStyles() {
    }
}

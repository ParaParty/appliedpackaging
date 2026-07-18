package com.warmthdawn.appliedpackaging.client.widget;

import appeng.client.gui.style.Blitter;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import net.minecraft.resources.ResourceLocation;

/** User-authored 16x16 toolbar icons stored in the shared package sprite atlas. */
public final class PackageToolbarSprites {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            AppliedPackaging.MOD_ID,
            "textures/gui/package-storagebus-sprites.png");

    public static final Blitter ANTI_CLOG_ON = icon(0, 96);
    public static final Blitter ANTI_CLOG_OFF = icon(0, 112);
    public static final Blitter SYNCHRONIZED_OUTPUT_ON = icon(16, 96);
    public static final Blitter SYNCHRONIZED_OUTPUT_OFF = icon(16, 112);
    public static final Blitter PATTERN_SYNC_ON = icon(32, 96);
    public static final Blitter PATTERN_SYNC_OFF = icon(32, 112);
    public static final Blitter INPUT_DELAY = icon(0, 128);
    public static final Blitter ITEMS_ONLY = icon(16, 128);
    public static final Blitter FLUIDS_ONLY = icon(32, 128);

    private PackageToolbarSprites() {
    }

    private static Blitter icon(int x, int y) {
        return Blitter.texture(TEXTURE).src(x, y, 16, 16);
    }
}

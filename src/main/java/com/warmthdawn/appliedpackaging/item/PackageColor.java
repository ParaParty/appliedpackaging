package com.warmthdawn.appliedpackaging.item;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;

public enum PackageColor {
    FLUIX("fluix", ChatFormatting.LIGHT_PURPLE, 0x8a6dff),
    WHITE("white", ChatFormatting.WHITE, 0xf0f0f0),
    ORANGE("orange", ChatFormatting.GOLD, 0xf9801d),
    MAGENTA("magenta", ChatFormatting.LIGHT_PURPLE, 0xc74ebd),
    LIGHT_BLUE("light_blue", ChatFormatting.AQUA, 0x3ab3da),
    YELLOW("yellow", ChatFormatting.YELLOW, 0xfed83d),
    LIME("lime", ChatFormatting.GREEN, 0x80c71f),
    PINK("pink", ChatFormatting.LIGHT_PURPLE, 0xf38baa),
    GRAY("gray", ChatFormatting.DARK_GRAY, 0x474f52),
    LIGHT_GRAY("light_gray", ChatFormatting.GRAY, 0x9d9d97),
    CYAN("cyan", ChatFormatting.DARK_AQUA, 0x169c9c),
    PURPLE("purple", ChatFormatting.DARK_PURPLE, 0x8932b8),
    BLUE("blue", ChatFormatting.BLUE, 0x3c44aa),
    BROWN("brown", ChatFormatting.GOLD, 0x835432),
    GREEN("green", ChatFormatting.DARK_GREEN, 0x5e7c16),
    RED("red", ChatFormatting.RED, 0xb02e26),
    BLACK("black", ChatFormatting.BLACK, 0x1d1d21);

    private final String id;
    private final ChatFormatting formatting;
    private final int swatchRgb;

    PackageColor(String id, ChatFormatting formatting, int swatchRgb) {
        this.id = id;
        this.formatting = formatting;
        this.swatchRgb = swatchRgb;
    }

    public String id() {
        return id;
    }

    public ChatFormatting formatting() {
        return formatting;
    }

    public int swatchArgb() {
        return 0xff000000 | swatchRgb;
    }

    public String translationKeySuffix() {
        return id.toLowerCase(Locale.ROOT);
    }

    public static Optional<PackageColor> byId(String id) {
        for (PackageColor color : values()) {
            if (color.id.equals(id)) {
                return Optional.of(color);
            }
        }
        return Optional.empty();
    }
}

package com.warmthdawn.appliedpackaging.item;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;

public enum PackageColor {
    FLUIX("fluix", ChatFormatting.LIGHT_PURPLE),
    WHITE("white", ChatFormatting.WHITE),
    ORANGE("orange", ChatFormatting.GOLD),
    MAGENTA("magenta", ChatFormatting.LIGHT_PURPLE),
    LIGHT_BLUE("light_blue", ChatFormatting.AQUA),
    YELLOW("yellow", ChatFormatting.YELLOW),
    LIME("lime", ChatFormatting.GREEN),
    PINK("pink", ChatFormatting.LIGHT_PURPLE),
    GRAY("gray", ChatFormatting.DARK_GRAY),
    LIGHT_GRAY("light_gray", ChatFormatting.GRAY),
    CYAN("cyan", ChatFormatting.DARK_AQUA),
    PURPLE("purple", ChatFormatting.DARK_PURPLE),
    BLUE("blue", ChatFormatting.BLUE),
    BROWN("brown", ChatFormatting.GOLD),
    GREEN("green", ChatFormatting.DARK_GREEN),
    RED("red", ChatFormatting.RED),
    BLACK("black", ChatFormatting.BLACK);

    private final String id;
    private final ChatFormatting formatting;

    PackageColor(String id, ChatFormatting formatting) {
        this.id = id;
        this.formatting = formatting;
    }

    public String id() {
        return id;
    }

    public ChatFormatting formatting() {
        return formatting;
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

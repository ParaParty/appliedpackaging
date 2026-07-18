package com.warmthdawn.appliedpackaging.part;

import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.List;

/** Controls the automatic color assigned to generated advanced-pattern columns. */
public enum AdvancedPatternColorMode {
    DEFAULT,
    CYCLING;

    public PackageColor colorForNewColumn(List<PackageColor> precedingColors) {
        if (this == DEFAULT || precedingColors == null || precedingColors.isEmpty()) {
            return PackageColor.FLUIX;
        }

        PackageColor[] colors = PackageColor.values();
        boolean[] used = new boolean[colors.length];
        for (PackageColor color : precedingColors) {
            if (color != null) {
                used[color.ordinal()] = true;
            }
        }
        for (PackageColor color : colors) {
            if (!used[color.ordinal()]) {
                return color;
            }
        }

        PackageColor last = precedingColors.get(precedingColors.size() - 1);
        int lastOrdinal = last == null ? PackageColor.FLUIX.ordinal() : last.ordinal();
        return colors[(lastOrdinal + 1) % colors.length];
    }
}

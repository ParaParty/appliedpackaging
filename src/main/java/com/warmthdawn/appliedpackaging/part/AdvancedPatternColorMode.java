package com.warmthdawn.appliedpackaging.part;

import com.warmthdawn.appliedpackaging.item.PackageColor;

/** Controls the automatic color assigned to generated advanced-pattern columns. */
public enum AdvancedPatternColorMode {
    DEFAULT,
    CYCLING;

    public PackageColor colorForColumn(int column) {
        if (this == DEFAULT) {
            return PackageColor.FLUIX;
        }
        PackageColor[] colors = PackageColor.values();
        return colors[Math.floorMod(column, colors.length)];
    }
}

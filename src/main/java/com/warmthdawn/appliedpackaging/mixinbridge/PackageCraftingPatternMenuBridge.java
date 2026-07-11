package com.warmthdawn.appliedpackaging.mixinbridge;

import appeng.menu.slot.FakeSlot;
import com.warmthdawn.appliedpackaging.item.PackageColor;

public interface PackageCraftingPatternMenuBridge {
    boolean appliedpackaging$isPackageCraftingMode();

    void appliedpackaging$setPackageCraftingMode(boolean packageMode);

    PackageColor appliedpackaging$getPackageCraftingColor();

    void appliedpackaging$setPackageCraftingColor(PackageColor color);

    FakeSlot appliedpackaging$getPackageCraftingMarkerSlot();
}

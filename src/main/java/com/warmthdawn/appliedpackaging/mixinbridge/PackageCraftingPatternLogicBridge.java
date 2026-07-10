package com.warmthdawn.appliedpackaging.mixinbridge;

import appeng.api.inventories.InternalInventory;
import com.warmthdawn.appliedpackaging.item.PackageColor;

public interface PackageCraftingPatternLogicBridge {
    boolean appliedpackaging$isPackageCraftingMode();

    void appliedpackaging$setPackageCraftingMode(boolean packageMode);

    PackageColor appliedpackaging$getPackageCraftingColor();

    void appliedpackaging$setPackageCraftingColor(PackageColor color);

    String appliedpackaging$getPackageCraftingName();

    void appliedpackaging$setPackageCraftingName(String name);

    InternalInventory appliedpackaging$getPackageCraftingMarkerInv();
}

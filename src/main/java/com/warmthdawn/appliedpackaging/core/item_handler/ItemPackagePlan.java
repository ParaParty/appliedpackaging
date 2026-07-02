package com.warmthdawn.appliedpackaging.core.item_handler;

import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import java.util.List;

public record ItemPackagePlan(PackageData data, List<SlotExtraction> extractions) {
    public ItemPackagePlan {
        extractions = List.copyOf(extractions);
        if (data == null) {
            throw new IllegalArgumentException("Package data cannot be null");
        }
        if (extractions.isEmpty()) {
            throw new IllegalArgumentException("Package plans must extract at least one stack");
        }
    }
}

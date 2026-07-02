package com.warmthdawn.appliedpackaging.core.ae2;

import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import java.util.List;

public record MEStoragePackagePlan(PackageData data, List<GenericStack> extractions) {
    public MEStoragePackagePlan {
        extractions = List.copyOf(extractions);
    }
}

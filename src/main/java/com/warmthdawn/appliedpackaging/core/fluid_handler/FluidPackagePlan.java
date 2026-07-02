package com.warmthdawn.appliedpackaging.core.fluid_handler;

import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import java.util.List;
import net.minecraftforge.fluids.FluidStack;

public record FluidPackagePlan(PackageData data, List<FluidStack> extractions) {
    public FluidPackagePlan {
        extractions = extractions.stream().map(FluidStack::copy).toList();
        if (data == null) {
            throw new IllegalArgumentException("Package data cannot be null");
        }
        if (extractions.isEmpty()) {
            throw new IllegalArgumentException("Package plans must extract at least one fluid stack");
        }
    }
}

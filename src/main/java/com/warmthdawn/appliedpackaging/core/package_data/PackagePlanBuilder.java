package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PackagePlanBuilder {
    private PackagePlanBuilder() {
    }

    public static PackagePlanResult build(
            PackageColor color,
            List<GenericStack> looseContents,
            List<PackageData> sourcePackages,
            MarkerMergeMode markerMode,
            Optional<MarkerSpec> overrideMarker,
            PackageCapacityProfile capacityProfile,
            int flags) {
        List<GenericStack> contents = new ArrayList<>();
        if (looseContents != null) {
            contents.addAll(looseContents);
        }
        if (sourcePackages != null) {
            for (PackageData sourcePackage : sourcePackages) {
                if (sourcePackage == null) {
                    return PackagePlanResult.failure(PackagePlanFailure.INVALID_INPUT);
                }
                contents.addAll(sourcePackage.contents());
            }
        }
        if (contents.isEmpty()) {
            return PackagePlanResult.failure(PackagePlanFailure.EMPTY_CONTENTS);
        }

        Optional<MarkerSpec> marker = resolveMarker(sourcePackages, markerMode, overrideMarker);
        if (marker == null) {
            return PackagePlanResult.failure(PackagePlanFailure.MARKER_CONFLICT);
        }

        PackageData data;
        try {
            data = PackageData.create(color, contents, marker, flags);
        } catch (IllegalArgumentException e) {
            return PackagePlanResult.failure(PackagePlanFailure.INVALID_INPUT);
        }

        PackageCapacityProfile profile = capacityProfile == null ? PackageCapacityProfile.DEFAULT : capacityProfile;
        if (!profile.fits(data.usedUnits(), data.usedTypes())) {
            return PackagePlanResult.failure(PackagePlanFailure.CAPACITY_EXCEEDED);
        }
        return PackagePlanResult.success(data);
    }

    private static Optional<MarkerSpec> resolveMarker(
            List<PackageData> sourcePackages,
            MarkerMergeMode markerMode,
            Optional<MarkerSpec> overrideMarker) {
        MarkerMergeMode mode = markerMode == null ? MarkerMergeMode.RETAIN : markerMode;
        return switch (mode) {
            case CLEAR -> Optional.empty();
            case OVERRIDE -> overrideMarker == null ? Optional.empty() : overrideMarker;
            case RETAIN -> retainMarker(sourcePackages);
        };
    }

    private static Optional<MarkerSpec> retainMarker(List<PackageData> sourcePackages) {
        Optional<MarkerSpec> retained = Optional.empty();
        if (sourcePackages == null) {
            return retained;
        }
        for (PackageData sourcePackage : sourcePackages) {
            if (sourcePackage == null || sourcePackage.marker().isEmpty()) {
                continue;
            }
            MarkerSpec sourceMarker = sourcePackage.marker().get();
            if (retained.isEmpty()) {
                retained = Optional.of(sourceMarker);
            } else if (!retained.get().sameAs(sourceMarker)) {
                return null;
            }
        }
        return retained;
    }
}

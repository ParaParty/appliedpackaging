package com.warmthdawn.appliedpackaging.core.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanBuilder;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MEStoragePackageTransactions {
    private MEStoragePackageTransactions() {
    }

    public static Optional<MEStoragePackagePlan> planPack(
            MEStorage source,
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            PackageFilter filter) {
        PackageFilter effectiveFilter = filter == null ? PackageFilter.any() : filter;
        Optional<MarkerSpec> overrideMarker = effectiveFilter.marker();
        MarkerMergeMode markerMode = overrideMarker.isPresent()
                ? MarkerMergeMode.OVERRIDE
                : MarkerMergeMode.RETAIN;
        return planPack(source, color, capacityProfile, effectiveFilter, markerMode, overrideMarker);
    }

    public static Optional<MEStoragePackagePlan> planPack(
            MEStorage source,
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            PackageFilter filter,
            MarkerMergeMode markerMode,
            Optional<MarkerSpec> overrideMarker) {
        return planPack(source, color, capacityProfile, filter, markerMode, overrideMarker, false);
    }

    public static Optional<MEStoragePackagePlan> planPack(
            MEStorage source,
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            PackageFilter filter,
            MarkerMergeMode markerMode,
            Optional<MarkerSpec> overrideMarker,
            boolean invertContents) {
        List<GenericStack> looseContents = new ArrayList<>();
        List<PackageData> sourcePackages = new ArrayList<>();
        List<GenericStack> extractions = new ArrayList<>();
        PackageFilter effectiveFilter = filter == null ? PackageFilter.any() : filter;
        MarkerMergeMode effectiveMarkerMode = markerMode == null ? MarkerMergeMode.RETAIN : markerMode;
        Optional<MarkerSpec> effectiveOverrideMarker = overrideMarker == null ? Optional.empty() : overrideMarker;

        for (var entry : source.getAvailableStacks()) {
            AEKey key = entry.getKey();
            long available = entry.getLongValue();
            if (available <= 0) {
                continue;
            }

            Optional<PackageData> packageData = packageDataFromKey(key);
            if (packageData.isPresent()) {
                if (!effectiveFilter.matchesContents(packageData.get(), invertContents)) {
                    continue;
                }
                if (tryPackageCandidate(
                        color,
                        capacityProfile,
                        effectiveMarkerMode,
                        effectiveOverrideMarker,
                        effectiveFilter,
                        looseContents,
                        sourcePackages,
                        packageData.get())) {
                    sourcePackages.add(packageData.get());
                    extractions.add(new GenericStack(key, 1));
                }
                continue;
            }

            long maxAmount = filteredMaxAmount(effectiveFilter, key, available, invertContents);
            long amount = largestFittingAmount(
                    color,
                    capacityProfile,
                    effectiveMarkerMode,
                    effectiveOverrideMarker,
                    effectiveFilter,
                    looseContents,
                    sourcePackages,
                    key,
                    maxAmount);
            if (amount > 0) {
                looseContents.add(new GenericStack(key, amount));
                extractions.add(new GenericStack(key, amount));
            }
        }

        if (extractions.isEmpty()) {
            return Optional.empty();
        }
        return PackagePlanBuilder.build(
                        color,
                        looseContents,
                        sourcePackages,
                        effectiveMarkerMode,
                        effectiveOverrideMarker,
                        capacityProfile,
                        0)
                .data()
                .filter(data -> effectiveFilter.matches(color, data, invertContents))
                .map(data -> new MEStoragePackagePlan(data, extractions));
    }

    public static boolean canExtract(MEStorage source, MEStoragePackagePlan plan) {
        for (GenericStack extraction : plan.extractions()) {
            long extracted = source.extract(
                    extraction.what(),
                    extraction.amount(),
                    Actionable.SIMULATE,
                    IActionSource.empty());
            if (extracted != extraction.amount()) {
                return false;
            }
        }
        return true;
    }

    public static boolean commitExtract(MEStorage source, MEStoragePackagePlan plan) {
        List<GenericStack> committed = new ArrayList<>();
        for (GenericStack extraction : plan.extractions()) {
            long extracted = source.extract(
                    extraction.what(),
                    extraction.amount(),
                    Actionable.MODULATE,
                    IActionSource.empty());
            if (extracted > 0) {
                committed.add(new GenericStack(extraction.what(), extracted));
            }
            if (extracted != extraction.amount()) {
                if (!rollbackExtractions(source, committed)) {
                    AppliedPackaging.LOGGER.error("ME storage package extraction rollback did not restore all extracted resources");
                }
                return false;
            }
        }
        return true;
    }

    public static boolean canInsertPackageContents(PackageData data, MEStorage target) {
        for (GenericStack entry : data.contents()) {
            long inserted = target.insert(entry.what(), entry.amount(), Actionable.SIMULATE, IActionSource.empty());
            if (inserted != entry.amount()) {
                return false;
            }
        }
        return true;
    }

    public static boolean insertPackageContents(PackageData data, MEStorage target) {
        if (!canInsertPackageContents(data, target)) {
            return false;
        }
        List<GenericStack> committed = new ArrayList<>();
        for (GenericStack entry : data.contents()) {
            long inserted = target.insert(entry.what(), entry.amount(), Actionable.MODULATE, IActionSource.empty());
            if (inserted > 0) {
                committed.add(new GenericStack(entry.what(), inserted));
            }
            if (inserted != entry.amount()) {
                if (!rollbackInsertions(target, committed)) {
                    AppliedPackaging.LOGGER.error("ME storage package insertion rollback did not remove all committed resources");
                }
                return false;
            }
        }
        return true;
    }

    private static boolean rollbackExtractions(MEStorage source, List<GenericStack> committed) {
        boolean complete = true;
        for (int index = committed.size() - 1; index >= 0; index--) {
            GenericStack extraction = committed.get(index);
            long restored = source.insert(
                    extraction.what(), extraction.amount(), Actionable.MODULATE, IActionSource.empty());
            complete &= restored == extraction.amount();
        }
        return complete;
    }

    private static boolean rollbackInsertions(MEStorage target, List<GenericStack> committed) {
        boolean complete = true;
        for (int index = committed.size() - 1; index >= 0; index--) {
            GenericStack insertion = committed.get(index);
            long removed = target.extract(
                    insertion.what(), insertion.amount(), Actionable.MODULATE, IActionSource.empty());
            complete &= removed == insertion.amount();
        }
        return complete;
    }

    private static Optional<PackageData> packageDataFromKey(AEKey key) {
        if (!AEItemKey.is(key)) {
            return Optional.empty();
        }
        return PackageDataStorage.read(((AEItemKey) key).toStack());
    }

    private static boolean tryPackageCandidate(
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            MarkerMergeMode markerMode,
            Optional<MarkerSpec> overrideMarker,
            PackageFilter filter,
            List<GenericStack> looseContents,
            List<PackageData> sourcePackages,
            PackageData candidate) {
        List<PackageData> trialPackages = new ArrayList<>(sourcePackages);
        trialPackages.add(candidate);
        return PackagePlanBuilder.build(
                        color,
                        looseContents,
                        trialPackages,
                        markerMode,
                        overrideMarker,
                        capacityProfile,
                        0)
                .success();
    }

    private static long filteredMaxAmount(
            PackageFilter filter,
            AEKey key,
            long available,
            boolean invertContents) {
        return filter.allowsContent(key, invertContents) ? available : 0;
    }

    private static long largestFittingAmount(
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            MarkerMergeMode markerMode,
            Optional<MarkerSpec> overrideMarker,
            PackageFilter filter,
            List<GenericStack> looseContents,
            List<PackageData> sourcePackages,
            AEKey key,
            long maxAmount) {
        long low = 0;
        long high = maxAmount;
        while (low < high) {
            long mid = (low + high + 1) / 2;
            if (fitsLooseAmount(
                    color,
                    capacityProfile,
                    markerMode,
                    overrideMarker,
                    filter,
                    looseContents,
                    sourcePackages,
                    key,
                    mid)) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    private static boolean fitsLooseAmount(
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            MarkerMergeMode markerMode,
            Optional<MarkerSpec> overrideMarker,
            PackageFilter filter,
            List<GenericStack> looseContents,
            List<PackageData> sourcePackages,
            AEKey key,
            long amount) {
        if (amount <= 0) {
            return true;
        }
        List<GenericStack> trialContents = new ArrayList<>(looseContents);
        trialContents.add(new GenericStack(key, amount));
        return PackagePlanBuilder.build(
                        color,
                        trialContents,
                        sourcePackages,
                        markerMode,
                        overrideMarker,
                        capacityProfile,
                        0)
                .success();
    }
}

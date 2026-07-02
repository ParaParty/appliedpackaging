package com.warmthdawn.appliedpackaging.core.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanBuilder;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MEStoragePackageTransactions {
    private MEStoragePackageTransactions() {
    }

    public static Optional<MEStoragePackagePlan> planPack(
            MEStorage source,
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            PackageFilter filter) {
        List<GenericStack> looseContents = new ArrayList<>();
        List<PackageData> sourcePackages = new ArrayList<>();
        List<GenericStack> extractions = new ArrayList<>();
        PackageFilter effectiveFilter = filter == null ? PackageFilter.any() : filter;
        MarkerMergeMode markerMode = effectiveFilter.marker().isPresent()
                ? MarkerMergeMode.OVERRIDE
                : MarkerMergeMode.RETAIN;

        for (var entry : source.getAvailableStacks()) {
            AEKey key = entry.getKey();
            long available = entry.getLongValue();
            if (available <= 0) {
                continue;
            }

            Optional<PackageData> packageData = packageDataFromKey(key);
            if (packageData.isPresent()) {
                if (!effectiveFilter.isAny()
                        && !contributesToRequiredContents(effectiveFilter, looseContents, sourcePackages, packageData.get())) {
                    continue;
                }
                if (tryPackageCandidate(
                        color,
                        capacityProfile,
                        markerMode,
                        effectiveFilter,
                        looseContents,
                        sourcePackages,
                        packageData.get())) {
                    sourcePackages.add(packageData.get());
                    extractions.add(new GenericStack(key, 1));
                }
                continue;
            }

            long maxAmount = filteredMaxAmount(effectiveFilter, looseContents, sourcePackages, key, available);
            long amount = largestFittingAmount(
                    color,
                    capacityProfile,
                    markerMode,
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
                        markerMode,
                        effectiveFilter.marker(),
                        capacityProfile,
                        0)
                .data()
                .filter(data -> effectiveFilter.matches(color, data))
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

    public static void commitExtract(MEStorage source, MEStoragePackagePlan plan) {
        for (GenericStack extraction : plan.extractions()) {
            source.extract(extraction.what(), extraction.amount(), Actionable.MODULATE, IActionSource.empty());
        }
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
        for (GenericStack entry : data.contents()) {
            long inserted = target.insert(entry.what(), entry.amount(), Actionable.MODULATE, IActionSource.empty());
            if (inserted != entry.amount()) {
                return false;
            }
        }
        return true;
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
                        filter.marker(),
                        capacityProfile,
                        0)
                .success();
    }

    private static long filteredMaxAmount(
            PackageFilter filter,
            List<GenericStack> looseContents,
            List<PackageData> sourcePackages,
            AEKey key,
            long available) {
        if (filter.requiredContents().isEmpty()) {
            return available;
        }
        long required = requiredAmount(filter, key);
        if (required <= 0) {
            return 0;
        }
        long current = availableAmount(looseContents, sourcePackages).getOrDefault(key, 0L);
        long remaining = required - current;
        if (remaining <= 0) {
            return 0;
        }
        return Math.min(available, remaining);
    }

    private static boolean contributesToRequiredContents(
            PackageFilter filter,
            List<GenericStack> looseContents,
            List<PackageData> sourcePackages,
            PackageData candidate) {
        if (filter.requiredContents().isEmpty()) {
            return true;
        }
        Map<AEKey, Long> current = availableAmount(looseContents, sourcePackages);
        for (GenericStack candidateStack : candidate.contents()) {
            if (requiredAmount(filter, candidateStack.what()) > current.getOrDefault(candidateStack.what(), 0L)) {
                return true;
            }
        }
        return false;
    }

    private static long requiredAmount(PackageFilter filter, AEKey key) {
        long amount = 0;
        for (GenericStack stack : filter.requiredContents()) {
            if (stack.what().equals(key)) {
                amount += stack.amount();
            }
        }
        return amount;
    }

    private static Map<AEKey, Long> availableAmount(
            List<GenericStack> looseContents,
            List<PackageData> sourcePackages) {
        Map<AEKey, Long> amounts = new HashMap<>();
        for (GenericStack stack : looseContents) {
            amounts.merge(stack.what(), stack.amount(), Long::sum);
        }
        for (PackageData packageData : sourcePackages) {
            for (GenericStack stack : packageData.contents()) {
                amounts.merge(stack.what(), stack.amount(), Long::sum);
            }
        }
        return amounts;
    }

    private static long largestFittingAmount(
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            MarkerMergeMode markerMode,
            PackageFilter filter,
            List<GenericStack> looseContents,
            List<PackageData> sourcePackages,
            AEKey key,
            long maxAmount) {
        long low = 0;
        long high = maxAmount;
        while (low < high) {
            long mid = (low + high + 1) / 2;
            if (fitsLooseAmount(color, capacityProfile, markerMode, filter, looseContents, sourcePackages, key, mid)) {
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
                        filter.marker(),
                        capacityProfile,
                        0)
                .success();
    }
}

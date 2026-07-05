package com.warmthdawn.appliedpackaging.core.item_handler;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanBuilder;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanResult;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public final class ItemPackageTransactions {
    private ItemPackageTransactions() {
    }

    public static Optional<ItemPackagePlan> planPack(
            IItemHandler source,
            PackageColor color,
            PackageCapacityProfile capacityProfile) {
        return planPack(source, color, capacityProfile, PackageFilter.any());
    }

    public static Optional<ItemPackagePlan> planPack(
            IItemHandler source,
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

    public static Optional<ItemPackagePlan> planPack(
            IItemHandler source,
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            PackageFilter filter,
            MarkerMergeMode markerMode,
            Optional<MarkerSpec> overrideMarker) {
        return planPack(source, color, capacityProfile, filter, markerMode, overrideMarker, false);
    }

    private static Optional<ItemPackagePlan> planPack(
            IItemHandler source,
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            PackageFilter filter,
            MarkerMergeMode markerMode,
            Optional<MarkerSpec> overrideMarker,
            boolean limitFilterAmounts) {
        List<GenericStack> looseContents = new ArrayList<>();
        List<PackageData> sourcePackages = new ArrayList<>();
        List<SlotExtraction> extractions = new ArrayList<>();
        PackageFilter effectiveFilter = filter == null ? PackageFilter.any() : filter;
        MarkerMergeMode effectiveMarkerMode = markerMode == null ? MarkerMergeMode.RETAIN : markerMode;
        Optional<MarkerSpec> effectiveOverrideMarker = overrideMarker == null ? Optional.empty() : overrideMarker;

        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack stack = source.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Optional<PackageData> packageData = PackageDataStorage.read(stack);
            if (packageData.isPresent()) {
                if (limitFilterAmounts
                        ? !contributesToRequiredContents(effectiveFilter, looseContents, sourcePackages, packageData.get())
                        : !effectiveFilter.matchesContents(packageData.get(), false)) {
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
                    ItemStack extraction = stack.copy();
                    extraction.setCount(1);
                    sourcePackages.add(packageData.get());
                    extractions.add(new SlotExtraction(slot, extraction));
                }
                continue;
            }

            AEItemKey key = AEItemKey.of(stack);
            int maxAmount = filteredMaxAmount(
                    effectiveFilter,
                    looseContents,
                    sourcePackages,
                    key,
                    stack.getCount(),
                    limitFilterAmounts);
            int amount = largestFittingAmount(
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
                ItemStack extraction = stack.copy();
                extraction.setCount(amount);
                extractions.add(new SlotExtraction(slot, extraction));
            }
        }

        if (extractions.isEmpty()) {
            return Optional.empty();
        }
        PackagePlanResult result = PackagePlanBuilder.build(
                color,
                looseContents,
                sourcePackages,
                effectiveMarkerMode,
                effectiveOverrideMarker,
                capacityProfile,
                0);
        return result.data()
                .filter(data -> limitFilterAmounts
                        ? effectiveFilter.matchesRequiredAmounts(color, data)
                        : effectiveFilter.matches(color, data))
                .map(data -> new ItemPackagePlan(data, extractions));
    }

    public static List<ItemPackagePlan> planAllPackages(
            IItemHandler source,
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            PackageFilter filter,
            MarkerMergeMode markerMode,
            Optional<MarkerSpec> overrideMarker) {
        SimulatedItemHandler simulated = SimulatedItemHandler.copyOf(source);
        List<ItemPackagePlan> plans = new ArrayList<>();
        int guard = 0;
        while (guard++ < 4096) {
            Optional<ItemPackagePlan> plan = planPack(
                    simulated,
                    color,
                    capacityProfile,
                    filter,
                    markerMode,
                    overrideMarker);
            if (plan.isEmpty()) {
                break;
            }
            plans.add(plan.get());
            commitExtract(simulated, plan.get());
        }
        return List.copyOf(plans);
    }

    public static Optional<ItemPackagePlan> planExactPackage(
            IItemHandler source,
            PackageColor color,
            PackageData target) {
        Optional<PackageCapacityProfile> capacityProfile = capacityProfileFor(target);
        if (capacityProfile.isEmpty()) {
            return Optional.empty();
        }
        MarkerMergeMode markerMode = target.marker().isPresent()
                ? MarkerMergeMode.OVERRIDE
                : MarkerMergeMode.CLEAR;
        PackageFilter filter = new PackageFilter(Optional.of(color), Optional.empty(), target.contents());
        return planPack(
                        source,
                        color,
                        capacityProfile.get(),
                        filter,
                        markerMode,
                        target.marker(),
                        true)
                .filter(plan -> plan.data().canonicalHash().equals(target.canonicalHash()));
    }

    public static boolean canExtract(IItemHandler source, ItemPackagePlan plan) {
        for (SlotExtraction extraction : plan.extractions()) {
            ItemStack extracted = source.extractItem(extraction.slot(), extraction.stack().getCount(), true);
            if (!ItemStack.isSameItemSameTags(extraction.stack(), extracted)
                    || extracted.getCount() != extraction.stack().getCount()) {
                return false;
            }
        }
        return true;
    }

    public static void commitExtract(IItemHandler source, ItemPackagePlan plan) {
        for (SlotExtraction extraction : plan.extractions()) {
            source.extractItem(extraction.slot(), extraction.stack().getCount(), false);
        }
    }

    public static boolean canInsertPackageContents(PackageData data, IItemHandler target) {
        SimulatedItemHandler simulated = SimulatedItemHandler.copyOf(target);
        return insertPackageContents(data, simulated, true);
    }

    public static boolean insertPackageContents(PackageData data, IItemHandler target, boolean simulate) {
        for (GenericStack entry : data.contents()) {
            if (!AEItemKey.is(entry.what()) || entry.amount() > Integer.MAX_VALUE) {
                return false;
            }
            AEItemKey key = (AEItemKey) entry.what();
            long remaining = entry.amount();
            while (remaining > 0) {
                int amount = (int) Math.min(remaining, key.getMaxStackSize());
                ItemStack stack = key.toStack(amount);
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, stack, simulate);
                if (!remainder.isEmpty()) {
                    return false;
                }
                remaining -= amount;
            }
        }
        return true;
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

    private static int filteredMaxAmount(
            PackageFilter filter,
            List<GenericStack> looseContents,
            List<PackageData> sourcePackages,
            AEItemKey key,
            int stackCount,
            boolean limitFilterAmounts) {
        if (filter.requiredContents().isEmpty()) {
            return stackCount;
        }
        if (!filter.allowsContent(key, false)) {
            return 0;
        }
        if (!limitFilterAmounts) {
            return stackCount;
        }
        long required = 0;
        for (GenericStack stack : filter.requiredContents()) {
            if (stack.what().equals(key)) {
                required += stack.amount();
            }
        }
        if (required <= 0) {
            return 0;
        }
        long available = availableAmount(looseContents, sourcePackages).getOrDefault(key, 0L);
        long remaining = required - available;
        if (remaining <= 0) {
            return 0;
        }
        return (int) Math.min(stackCount, remaining);
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
            long required = requiredAmount(filter, candidateStack.what());
            if (required > current.getOrDefault(candidateStack.what(), 0L)) {
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

    private static Optional<PackageCapacityProfile> capacityProfileFor(PackageData data) {
        for (PackageCapacityProfile profile : PackageCapacityProfile.values()) {
            if (profile.fits(data.usedUnits(), data.usedTypes())) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    private static int largestFittingAmount(
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            MarkerMergeMode markerMode,
            Optional<MarkerSpec> overrideMarker,
            PackageFilter filter,
            List<GenericStack> looseContents,
            List<PackageData> sourcePackages,
            AEItemKey key,
            int maxAmount) {
        int low = 0;
        int high = maxAmount;
        while (low < high) {
            int mid = (low + high + 1) / 2;
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
            AEItemKey key,
            int amount) {
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

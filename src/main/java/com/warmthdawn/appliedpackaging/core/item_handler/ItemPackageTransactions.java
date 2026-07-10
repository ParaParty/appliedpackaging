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
            if (!commitExtract(simulated, plan.get())) {
                break;
            }
            plans.add(plan.get());
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

    public static boolean commitExtract(IItemHandler source, ItemPackagePlan plan) {
        List<SlotExtraction> committed = new ArrayList<>();
        for (SlotExtraction extraction : plan.extractions()) {
            ItemStack extracted = source.extractItem(extraction.slot(), extraction.stack().getCount(), false);
            if (!extracted.isEmpty()) {
                committed.add(new SlotExtraction(extraction.slot(), extracted.copy()));
            }
            if (!ItemStack.isSameItemSameTags(extraction.stack(), extracted)
                    || extracted.getCount() != extraction.stack().getCount()) {
                rollbackExtractions(source, committed);
                return false;
            }
        }
        return true;
    }

    private static boolean rollbackExtractions(IItemHandler source, List<SlotExtraction> committed) {
        boolean complete = true;
        for (int index = committed.size() - 1; index >= 0; index--) {
            SlotExtraction extraction = committed.get(index);
            ItemStack remainder = source.insertItem(extraction.slot(), extraction.stack().copy(), false);
            if (!remainder.isEmpty()) {
                complete = false;
            }
        }
        return complete;
    }

    public static boolean canInsertPackageContents(PackageData data, IItemHandler target) {
        return planPackageContentsInsertion(data, target).isPresent();
    }

    public static boolean insertPackageContents(PackageData data, IItemHandler target, boolean simulate) {
        Optional<List<SlotInsertion>> plan = planPackageContentsInsertion(data, target);
        if (plan.isEmpty()) {
            return false;
        }
        if (simulate) {
            return true;
        }
        return commitPackageContentsInsertion(target, plan.get());
    }

    private static Optional<List<SlotInsertion>> planPackageContentsInsertion(
            PackageData data,
            IItemHandler target) {
        SimulatedItemHandler simulated = SimulatedItemHandler.copyOf(target);
        for (GenericStack entry : data.contents()) {
            if (!AEItemKey.is(entry.what()) || entry.amount() > Integer.MAX_VALUE) {
                return Optional.empty();
            }
            AEItemKey key = (AEItemKey) entry.what();
            long remaining = entry.amount();
            while (remaining > 0) {
                int amount = (int) Math.min(remaining, key.getMaxStackSize());
                ItemStack stack = key.toStack(amount);
                ItemStack actualSimulation = ItemHandlerHelper.insertItemStacked(target, stack.copy(), true);
                if (!actualSimulation.isEmpty()) {
                    return Optional.empty();
                }
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(simulated, stack, false);
                if (!remainder.isEmpty()) {
                    return Optional.empty();
                }
                remaining -= amount;
            }
        }

        List<SlotInsertion> insertions = new ArrayList<>();
        for (int slot = 0; slot < target.getSlots(); slot++) {
            ItemStack before = target.getStackInSlot(slot);
            ItemStack after = simulated.getStackInSlot(slot);
            int beforeCount = ItemStack.isSameItemSameTags(before, after) ? before.getCount() : 0;
            int inserted = after.getCount() - beforeCount;
            if (inserted > 0) {
                ItemStack stack = after.copy();
                stack.setCount(inserted);
                insertions.add(new SlotInsertion(slot, stack));
            }
        }
        return Optional.of(List.copyOf(insertions));
    }

    private static boolean commitPackageContentsInsertion(
            IItemHandler target,
            List<SlotInsertion> plan) {
        List<SlotInsertion> committed = new ArrayList<>();
        for (SlotInsertion insertion : plan) {
            ItemStack requested = insertion.stack().copy();
            ItemStack remainder = target.insertItem(insertion.slot(), requested, false);
            int accepted = acceptedAmount(requested, remainder);
            if (accepted > 0) {
                ItemStack acceptedStack = requested.copy();
                acceptedStack.setCount(accepted);
                committed.add(new SlotInsertion(insertion.slot(), acceptedStack));
            }
            if (!remainder.isEmpty()) {
                rollbackPackageContentsInsertion(target, committed);
                return false;
            }
        }
        return true;
    }

    private static int acceptedAmount(ItemStack requested, ItemStack remainder) {
        if (remainder.isEmpty()) {
            return requested.getCount();
        }
        if (!ItemStack.isSameItemSameTags(requested, remainder)) {
            return 0;
        }
        return Math.max(0, requested.getCount() - remainder.getCount());
    }

    private static boolean rollbackPackageContentsInsertion(
            IItemHandler target,
            List<SlotInsertion> committed) {
        boolean complete = true;
        for (int index = committed.size() - 1; index >= 0; index--) {
            SlotInsertion insertion = committed.get(index);
            int remaining = insertion.stack().getCount();
            while (remaining > 0) {
                ItemStack extracted = target.extractItem(insertion.slot(), remaining, false);
                if (extracted.isEmpty() || !ItemStack.isSameItemSameTags(insertion.stack(), extracted)) {
                    complete = false;
                    break;
                }
                remaining -= extracted.getCount();
            }
        }
        return complete;
    }

    private record SlotInsertion(int slot, ItemStack stack) {
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

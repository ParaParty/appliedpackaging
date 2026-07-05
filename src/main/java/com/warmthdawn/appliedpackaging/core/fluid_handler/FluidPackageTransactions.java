package com.warmthdawn.appliedpackaging.core.fluid_handler;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageFilter;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanBuilder;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanResult;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class FluidPackageTransactions {
    private FluidPackageTransactions() {
    }

    public static Optional<FluidPackagePlan> planPack(
            IFluidHandler source,
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            PackageFilter filter,
            MarkerMergeMode markerMode,
            Optional<MarkerSpec> overrideMarker) {
        List<GenericStack> looseContents = new ArrayList<>();
        List<FluidStack> extractions = new ArrayList<>();
        PackageFilter effectiveFilter = filter == null ? PackageFilter.any() : filter;
        MarkerMergeMode effectiveMarkerMode = markerMode == null ? MarkerMergeMode.RETAIN : markerMode;
        Optional<MarkerSpec> effectiveOverrideMarker = overrideMarker == null ? Optional.empty() : overrideMarker;

        for (int tank = 0; tank < source.getTanks(); tank++) {
            FluidStack stack = source.getFluidInTank(tank);
            if (stack.isEmpty()) {
                continue;
            }

            AEFluidKey key = AEFluidKey.of(stack);
            int maxAmount = filteredMaxAmount(effectiveFilter, looseContents, key, stack.getAmount());
            int amount = largestFittingAmount(
                    color,
                    capacityProfile,
                    effectiveMarkerMode,
                    effectiveOverrideMarker,
                    effectiveFilter,
                    looseContents,
                    key,
                    maxAmount);
            if (amount > 0) {
                looseContents.add(new GenericStack(key, amount));
                extractions.add(new FluidStack(stack, amount));
            }
        }

        if (extractions.isEmpty()) {
            return Optional.empty();
        }
        PackagePlanResult result = PackagePlanBuilder.build(
                color,
                looseContents,
                List.of(),
                effectiveMarkerMode,
                effectiveOverrideMarker,
                capacityProfile,
                0);
        return result.data()
                .filter(data -> effectiveFilter.matches(color, data))
                .map(data -> new FluidPackagePlan(data, extractions));
    }

    public static boolean canExtract(IFluidHandler source, FluidPackagePlan plan) {
        for (FluidStack extraction : plan.extractions()) {
            FluidStack extracted = source.drain(extraction, IFluidHandler.FluidAction.SIMULATE);
            if (!extracted.isFluidStackIdentical(extraction)) {
                return false;
            }
        }
        return true;
    }

    public static void commitExtract(IFluidHandler source, FluidPackagePlan plan) {
        for (FluidStack extraction : plan.extractions()) {
            source.drain(extraction, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public static boolean canInsertPackageContents(PackageData data, IFluidHandler target) {
        SimulatedFluidHandler simulated = SimulatedFluidHandler.copyOf(target);
        return insertPackageContents(data, simulated, false);
    }

    public static boolean insertPackageContents(PackageData data, IFluidHandler target, boolean simulate) {
        IFluidHandler.FluidAction action = simulate
                ? IFluidHandler.FluidAction.SIMULATE
                : IFluidHandler.FluidAction.EXECUTE;
        for (GenericStack entry : data.contents()) {
            if (!AEFluidKey.is(entry.what()) || entry.amount() > Integer.MAX_VALUE) {
                return false;
            }
            AEFluidKey key = (AEFluidKey) entry.what();
            FluidStack stack = key.toStack((int) entry.amount());
            int inserted = target.fill(stack, action);
            if (inserted != stack.getAmount()) {
                return false;
            }
        }
        return true;
    }

    private static int filteredMaxAmount(
            PackageFilter filter,
            List<GenericStack> looseContents,
            AEFluidKey key,
            int stackAmount) {
        return filter.allowsContent(key, false) ? stackAmount : 0;
    }

    private static int largestFittingAmount(
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            MarkerMergeMode markerMode,
            Optional<MarkerSpec> overrideMarker,
            PackageFilter filter,
            List<GenericStack> looseContents,
            AEFluidKey key,
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
            AEFluidKey key,
            int amount) {
        if (amount <= 0) {
            return true;
        }
        List<GenericStack> trialContents = new ArrayList<>(looseContents);
        trialContents.add(new GenericStack(key, amount));
        return PackagePlanBuilder.build(
                        color,
                        trialContents,
                        List.of(),
                        markerMode,
                        overrideMarker,
                        capacityProfile,
                        0)
                .success();
    }
}

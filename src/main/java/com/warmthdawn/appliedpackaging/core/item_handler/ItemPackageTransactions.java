package com.warmthdawn.appliedpackaging.core.item_handler;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerMergeMode;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCapacityProfile;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanBuilder;
import com.warmthdawn.appliedpackaging.core.package_data.PackagePlanResult;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.ArrayList;
import java.util.List;
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
        List<GenericStack> looseContents = new ArrayList<>();
        List<PackageData> sourcePackages = new ArrayList<>();
        List<SlotExtraction> extractions = new ArrayList<>();

        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack stack = source.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Optional<PackageData> packageData = PackageDataStorage.read(stack);
            if (packageData.isPresent()) {
                if (tryPackageCandidate(color, capacityProfile, looseContents, sourcePackages, packageData.get())) {
                    ItemStack extraction = stack.copy();
                    extraction.setCount(1);
                    sourcePackages.add(packageData.get());
                    extractions.add(new SlotExtraction(slot, extraction));
                }
                continue;
            }

            AEItemKey key = AEItemKey.of(stack);
            int amount = largestFittingAmount(color, capacityProfile, looseContents, sourcePackages, key, stack.getCount());
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
                MarkerMergeMode.RETAIN,
                Optional.empty(),
                capacityProfile,
                0);
        return result.data().map(data -> new ItemPackagePlan(data, extractions));
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
            List<GenericStack> looseContents,
            List<PackageData> sourcePackages,
            PackageData candidate) {
        List<PackageData> trialPackages = new ArrayList<>(sourcePackages);
        trialPackages.add(candidate);
        return PackagePlanBuilder.build(
                        color,
                        looseContents,
                        trialPackages,
                        MarkerMergeMode.RETAIN,
                        Optional.empty(),
                        capacityProfile,
                        0)
                .success();
    }

    private static int largestFittingAmount(
            PackageColor color,
            PackageCapacityProfile capacityProfile,
            List<GenericStack> looseContents,
            List<PackageData> sourcePackages,
            AEItemKey key,
            int maxAmount) {
        int low = 0;
        int high = maxAmount;
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (fitsLooseAmount(color, capacityProfile, looseContents, sourcePackages, key, mid)) {
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
                        MarkerMergeMode.RETAIN,
                        Optional.empty(),
                        capacityProfile,
                        0)
                .success();
    }
}

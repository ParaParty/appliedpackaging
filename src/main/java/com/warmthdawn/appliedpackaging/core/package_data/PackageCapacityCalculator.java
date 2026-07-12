package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PackageCapacityCalculator {
    private static final long DEFAULT_FLUID_UNIT = 1000L;

    private PackageCapacityCalculator() {
    }

    public static long usedUnits(List<GenericStack> contents) {
        long total = 0;
        for (GenericStack stack : contents) {
            try {
                total = Math.addExact(total, usedUnits(stack));
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Package capacity units overflow", exception);
            }
        }
        return total;
    }

    public static int usedTypes(List<GenericStack> contents) {
        Set<AEKey> keys = new HashSet<>();
        for (GenericStack stack : contents) {
            keys.add(stack.what());
        }
        return keys.size();
    }

    public static long usedUnits(GenericStack stack) {
        long unit = unitSize(stack.what());
        return Math.max(1, divideRoundUp(stack.amount(), unit));
    }

    private static long unitSize(AEKey key) {
        if (AEItemKey.is(key)) {
            return Math.max(1, ((AEItemKey) key).getMaxStackSize());
        }
        if (AEFluidKey.is(key)) {
            return DEFAULT_FLUID_UNIT;
        }
        return Math.max(1, key.getAmountPerUnit());
    }

    private static long divideRoundUp(long value, long divisor) {
        return value <= 0 ? 0 : 1 + (value - 1) / divisor;
    }
}

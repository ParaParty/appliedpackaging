package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record PackageData(
        int version,
        List<GenericStack> contents,
        Optional<MarkerSpec> marker,
        long usedUnits,
        int usedTypes,
        String canonicalHash,
        int flags) {
    public static final int CURRENT_VERSION = 1;

    public PackageData {
        contents = List.copyOf(contents);
        marker = marker == null ? Optional.empty() : marker;
        if (contents.isEmpty()) {
            throw new IllegalArgumentException("A package must contain at least one entry");
        }
        for (GenericStack stack : contents) {
            if (stack == null || stack.amount() <= 0) {
                throw new IllegalArgumentException("Package entries must be non-null and positive");
            }
        }
    }

    public static PackageData create(PackageColor color, List<GenericStack> contents, Optional<MarkerSpec> marker, int flags) {
        if (color == null) {
            throw new IllegalArgumentException("Package color cannot be null");
        }
        marker = marker == null ? Optional.empty() : marker;
        List<GenericStack> normalized = normalize(contents);
        long usedUnits = PackageCapacityCalculator.usedUnits(normalized);
        int usedTypes = PackageCapacityCalculator.usedTypes(normalized);
        String hash = PackageCanonicalizer.hash(color, CURRENT_VERSION, normalized, marker, flags);
        return new PackageData(CURRENT_VERSION, normalized, marker, usedUnits, usedTypes, hash, flags);
    }

    private static List<GenericStack> normalize(List<GenericStack> contents) {
        if (contents == null) {
            throw new IllegalArgumentException("Package contents cannot be null");
        }
        Map<AEKey, Long> amounts = new LinkedHashMap<>();
        for (GenericStack stack : contents) {
            if (stack == null || stack.what() == null || stack.amount() <= 0) {
                throw new IllegalArgumentException("Package entries must be non-null and positive");
            }
            try {
                amounts.merge(stack.what(), stack.amount(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Package entry amount overflow", exception);
            }
        }

        List<GenericStack> normalized = new ArrayList<>();
        amounts.forEach((key, amount) -> {
            if (amount > 0) {
                normalized.add(new GenericStack(key, amount));
            }
        });
        normalized.sort(java.util.Comparator.comparing(PackageCanonicalizer::canonicalStack));
        return normalized;
    }
}

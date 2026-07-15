package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record PackageData(
        int version,
        List<GenericStack> contents,
        Optional<PackageLayout> layout,
        Optional<MarkerSpec> marker,
        long usedUnits,
        int usedTypes,
        String canonicalHash,
        int flags) {
    public static final int CURRENT_VERSION = 2;

    public PackageData {
        contents = List.copyOf(contents);
        layout = layout == null ? Optional.empty() : layout;
        marker = marker == null ? Optional.empty() : marker;
        if (contents.isEmpty()) {
            throw new IllegalArgumentException("A package must contain at least one entry");
        }
        for (GenericStack stack : contents) {
            if (stack == null || stack.amount() <= 0) {
                throw new IllegalArgumentException("Package entries must be non-null and positive");
            }
        }
        if (layout.isPresent() && layout.orElseThrow().contentSlots().size() != contents.size()) {
            throw new IllegalArgumentException("Package layout must have one slot index per content entry");
        }
    }

    public static PackageData create(PackageColor color, List<GenericStack> contents, Optional<MarkerSpec> marker, int flags) {
        return create(color, contents, Optional.empty(), marker, flags);
    }

    public static PackageData create(
            PackageColor color,
            List<GenericStack> contents,
            Optional<PackageLayout> layout,
            Optional<MarkerSpec> marker,
            int flags) {
        if (color == null) {
            throw new IllegalArgumentException("Package color cannot be null");
        }
        layout = layout == null ? Optional.empty() : layout;
        marker = marker == null ? Optional.empty() : marker;
        List<GenericStack> orderedContents = copyOrderedContents(contents);
        if (layout.isPresent() && layout.orElseThrow().contentSlots().size() != orderedContents.size()) {
            throw new IllegalArgumentException("Package layout must have one slot index per content entry");
        }
        long usedUnits = PackageCapacityCalculator.usedUnits(orderedContents);
        int usedTypes = PackageCapacityCalculator.usedTypes(orderedContents);
        String hash = PackageCanonicalizer.hash(color, CURRENT_VERSION, orderedContents, layout, marker, flags);
        return new PackageData(CURRENT_VERSION, orderedContents, layout, marker, usedUnits, usedTypes, hash, flags);
    }

    private static List<GenericStack> copyOrderedContents(List<GenericStack> contents) {
        if (contents == null) {
            throw new IllegalArgumentException("Package contents cannot be null");
        }
        List<GenericStack> orderedContents = new ArrayList<>(contents.size());
        for (GenericStack stack : contents) {
            if (stack == null || stack.what() == null || stack.amount() <= 0) {
                throw new IllegalArgumentException("Package entries must be non-null and positive");
            }
            orderedContents.add(new GenericStack(stack.what(), stack.amount()));
        }
        return orderedContents;
    }
}

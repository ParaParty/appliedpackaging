package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record PackageFilter(
        Optional<PackageColor> color,
        Optional<MarkerSpec> marker,
        List<GenericStack> requiredContents) {
    public PackageFilter {
        color = color == null ? Optional.empty() : color;
        marker = marker == null ? Optional.empty() : marker;
        requiredContents = List.copyOf(requiredContents == null ? List.of() : requiredContents);
        for (GenericStack stack : requiredContents) {
            if (stack == null || stack.amount() <= 0) {
                throw new IllegalArgumentException("Required content entries must be non-null and positive");
            }
        }
    }

    public static PackageFilter any() {
        return new PackageFilter(Optional.empty(), Optional.empty(), List.of());
    }

    public boolean matches(PackageColor packageColor, PackageData data) {
        if (color.isPresent() && color.get() != packageColor) {
            return false;
        }
        if (marker.isPresent() && !data.marker().map(actual -> sameStack(actual.stack(), marker.get().stack())).orElse(false)) {
            return false;
        }
        if (requiredContents.isEmpty()) {
            return true;
        }

        Map<AEKey, Long> available = aggregate(data.contents());
        for (GenericStack required : requiredContents) {
            long amount = available.getOrDefault(required.what(), 0L);
            if (amount < required.amount()) {
                return false;
            }
        }
        return true;
    }

    private static Map<AEKey, Long> aggregate(List<GenericStack> stacks) {
        Map<AEKey, Long> amounts = new HashMap<>();
        for (GenericStack stack : stacks) {
            amounts.merge(stack.what(), stack.amount(), Long::sum);
        }
        return amounts;
    }

    private static boolean sameStack(GenericStack left, GenericStack right) {
        return left.what().equals(right.what()) && left.amount() == right.amount();
    }
}

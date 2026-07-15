package com.warmthdawn.appliedpackaging.core.package_data;

import java.util.List;

/**
 * Preserves the sparse source positions for the dense {@code PackageData.contents()} list.
 */
public record PackageLayout(int slotCount, List<Integer> contentSlots) {
    public PackageLayout {
        if (slotCount <= 0) {
            throw new IllegalArgumentException("Package layout slot count must be positive");
        }
        if (contentSlots == null || contentSlots.isEmpty()) {
            throw new IllegalArgumentException("Package layout must contain at least one content slot");
        }
        contentSlots = List.copyOf(contentSlots);
        int previous = -1;
        for (Integer slot : contentSlots) {
            if (slot == null || slot < 0 || slot >= slotCount || slot <= previous) {
                throw new IllegalArgumentException("Package layout slots must be strictly increasing and in range");
            }
            previous = slot;
        }
    }
}

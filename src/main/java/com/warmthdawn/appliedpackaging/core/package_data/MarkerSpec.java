package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.GenericStack;

public record MarkerSpec(GenericStack stack) {
    public MarkerSpec {
        if (stack == null) {
            throw new IllegalArgumentException("Marker stack cannot be null");
        }
        if (stack.amount() <= 0) {
            throw new IllegalArgumentException("Marker amount must be positive");
        }
    }

    public boolean sameAs(MarkerSpec other) {
        return other != null
                && stack.what().equals(other.stack().what())
                && stack.amount() == other.stack().amount();
    }
}

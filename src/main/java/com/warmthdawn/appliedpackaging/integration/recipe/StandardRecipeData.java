package com.warmthdawn.appliedpackaging.integration.recipe;

import appeng.api.stacks.GenericStack;
import java.util.List;

/** Item/fluid data extracted by one recipe-viewer frontend for the generic fallback. */
public record StandardRecipeData(List<Slot> inputs, List<Slot> outputs) {
    private static final StandardRecipeData EMPTY = new StandardRecipeData(List.of(), List.of());

    public StandardRecipeData {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }

    public static StandardRecipeData empty() {
        return EMPTY;
    }

    public record Slot(
            List<GenericStack> candidates,
            GenericStack displayed,
            String errorKey) {
        public Slot {
            candidates = List.copyOf(candidates);
        }

        public static Slot supported(List<GenericStack> candidates, GenericStack displayed) {
            return new Slot(candidates, displayed, null);
        }

        public static Slot rejected(String errorKey) {
            return new Slot(List.of(), null, errorKey);
        }
    }
}

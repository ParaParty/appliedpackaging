package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.level.Level;

public final class PackageCraftingPatternDetails implements IPatternDetails {
    private final AEItemKey definition;
    private final PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern pattern;
    private final IInput[] inputs;
    private final GenericStack[] outputs;

    public PackageCraftingPatternDetails(AEItemKey definition) {
        this.definition = definition;
        this.pattern = PackageCraftingPatternDataStorage.read(definition.toStack())
                .orElseThrow(() -> new IllegalArgumentException("Missing package crafting pattern data"));
        this.inputs = condensedInputs(pattern);
        this.outputs = new GenericStack[] {
                new GenericStack(AEItemKey.of(PackageCraftingPatternDataStorage.toPackageStack(pattern)), 1)
        };
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public GenericStack[] getOutputs() {
        return outputs;
    }

    public PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern encodedPattern() {
        return pattern;
    }

    public GenericStack[] sparseInputs() {
        return pattern.sparseInputs();
    }

    @Override
    public boolean supportsPushInputsToExternalInventory() {
        return false;
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PackageCraftingPatternDetails other && definition.equals(other.definition);
    }

    private static IInput[] condensedInputs(PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern pattern) {
        Map<AEKey, Long> amounts = new LinkedHashMap<>();
        for (GenericStack input : pattern.sparseInputs()) {
            if (input != null && input.amount() > 0) {
                amounts.merge(input.what(), input.amount(), Long::sum);
            }
        }
        return amounts.entrySet().stream()
                .map(entry -> new ExactInput(new GenericStack(entry.getKey(), entry.getValue())))
                .toArray(IInput[]::new);
    }

    private static final class ExactInput implements IInput {
        private final GenericStack[] template;
        private final long multiplier;

        private ExactInput(GenericStack stack) {
            this.template = new GenericStack[] { new GenericStack(stack.what(), 1) };
            this.multiplier = stack.amount();
        }

        @Override
        public GenericStack[] getPossibleInputs() {
            return template;
        }

        @Override
        public long getMultiplier() {
            return multiplier;
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return input != null && input.equals(template[0].what());
        }

        @Override
        public AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}

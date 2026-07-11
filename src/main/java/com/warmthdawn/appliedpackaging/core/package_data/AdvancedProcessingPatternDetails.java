package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

/** Processing-pattern details with one full AE2 input matrix per package column. */
public final class AdvancedProcessingPatternDetails implements IPatternDetails {
    private static final String INPUTS = "in";
    private static final String OUTPUTS = "out";

    private final AEItemKey definition;
    private final GenericStack[] sparseInputs;
    private final GenericStack[] sparseOutputs;
    private final IInput[] inputs;
    private final GenericStack[] outputs;

    public AdvancedProcessingPatternDetails(AEItemKey definition) {
        this.definition = Objects.requireNonNull(definition);
        CompoundTag tag = Objects.requireNonNull(definition.getTag(), "Advanced pattern must have a tag");
        this.sparseInputs = readSparseStacks(
                tag,
                INPUTS,
                AdvancedProcessingPatternDataStorage.MAX_INPUT_SLOTS);
        this.sparseOutputs = readSparseStacks(
                tag,
                OUTPUTS,
                AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS);
        if (AdvancedProcessingPatternDataStorage.read(definition.toStack()).isEmpty()) {
            throw new IllegalArgumentException("Advanced pattern metadata is missing or invalid");
        }
        if (java.util.Arrays.stream(sparseInputs).noneMatch(Objects::nonNull)) {
            throw new IllegalArgumentException("Advanced pattern requires at least one input");
        }
        if (sparseOutputs.length == 0 || sparseOutputs[0] == null) {
            throw new IllegalArgumentException("Advanced pattern requires a primary output");
        }

        List<GenericStack> condensedInputs = condense(sparseInputs);
        this.inputs = new IInput[condensedInputs.size()];
        for (int i = 0; i < condensedInputs.size(); i++) {
            this.inputs[i] = new Input(condensedInputs.get(i));
        }
        this.outputs = condense(sparseOutputs).toArray(GenericStack[]::new);
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

    public GenericStack[] getSparseInputs() {
        return sparseInputs;
    }

    public GenericStack[] getSparseOutputs() {
        return sparseOutputs;
    }

    @Override
    public void pushInputsToExternalInventory(KeyCounter[] inputHolder, PatternInputSink inputSink) {
        KeyCounter availableInputs = new KeyCounter();
        for (KeyCounter counter : inputHolder) {
            availableInputs.addAll(counter);
        }
        for (GenericStack input : sparseInputs) {
            if (input == null) {
                continue;
            }
            long available = availableInputs.get(input.what());
            if (available < input.amount()) {
                throw new IllegalStateException(
                        "Advanced pattern expected %d of %s, but only %d was available"
                                .formatted(input.amount(), input.what(), available));
            }
            inputSink.pushInput(input.what(), input.amount());
            availableInputs.remove(input.what(), input.amount());
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AdvancedProcessingPatternDetails details
                && definition.equals(details.definition);
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    private static GenericStack[] readSparseStacks(CompoundTag tag, String key, int maxSize) {
        if (!tag.contains(key, Tag.TAG_LIST)) {
            throw new IllegalArgumentException("Advanced pattern is missing " + key);
        }
        var list = tag.getList(key, Tag.TAG_COMPOUND);
        if (list.size() > maxSize) {
            throw new IllegalArgumentException("Advanced pattern exceeds the supported " + key + " capacity");
        }
        GenericStack[] result = new GenericStack[list.size()];
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.isEmpty()) {
                continue;
            }
            GenericStack stack = GenericStack.readTag(entry);
            if (stack == null || stack.amount() <= 0) {
                throw new IllegalArgumentException("Advanced pattern contains an invalid stack in " + key);
            }
            result[i] = stack;
        }
        return result;
    }

    private static List<GenericStack> condense(GenericStack[] sparseStacks) {
        Map<AEKey, Long> amounts = new LinkedHashMap<>();
        for (GenericStack stack : sparseStacks) {
            if (stack != null) {
                amounts.merge(stack.what(), stack.amount(), Math::addExact);
            }
        }
        List<GenericStack> result = new ArrayList<>(amounts.size());
        amounts.forEach((key, amount) -> result.add(new GenericStack(key, amount)));
        return List.copyOf(result);
    }

    private static final class Input implements IInput {
        private final GenericStack[] template;
        private final long multiplier;

        private Input(GenericStack stack) {
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
            return template[0].what().equals(input);
        }

        @Override
        public AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}

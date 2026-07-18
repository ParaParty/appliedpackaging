package com.warmthdawn.appliedpackaging.core.pattern;

import appeng.api.stacks.GenericStack;
import com.google.gson.Gson;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.TagParser;

/**
 * A validated, dependency-neutral recipe import plan for the advanced pattern terminal.
 */
public record AdvancedPatternTransferPlan(
        List<List<GenericStack>> columns,
        List<GenericStack> outputs) {
    public static final int MAX_CLIENT_ACTION_JSON_LENGTH = 32767;
    private static final int MAX_STACK_SNBT_LENGTH = 4096;
    private static final Gson GSON = new Gson();

    public AdvancedPatternTransferPlan {
        if (columns == null || columns.isEmpty()
                || columns.size() > AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS) {
            throw new IllegalArgumentException("Advanced pattern import must contain 1 to "
                    + AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS + " columns");
        }

        boolean hasInput = false;
        List<List<GenericStack>> copiedColumns = new ArrayList<>(columns.size());
        for (List<GenericStack> column : columns) {
            if (column == null
                    || column.size() > AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE) {
                throw new IllegalArgumentException("Advanced pattern import column exceeds "
                        + AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE + " inputs");
            }
            List<GenericStack> copiedColumn = new ArrayList<>(column.size());
            for (GenericStack stack : column) {
                if (stack == null) {
                    copiedColumn.add(null);
                    continue;
                }
                validateStack(stack);
                copiedColumn.add(stack);
                hasInput = true;
            }
            trimTrailingNulls(copiedColumn);
            copiedColumns.add(Collections.unmodifiableList(copiedColumn));
        }
        if (!hasInput) {
            throw new IllegalArgumentException("Advanced pattern import has no inputs");
        }

        if (outputs == null || outputs.isEmpty()
                || outputs.size() > AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS) {
            throw new IllegalArgumentException("Advanced pattern import must contain 1 to "
                    + AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS + " outputs");
        }
        List<GenericStack> copiedOutputs = new ArrayList<>(outputs.size());
        for (GenericStack stack : outputs) {
            validateStack(stack);
            copiedOutputs.add(stack);
        }

        columns = List.copyOf(copiedColumns);
        outputs = List.copyOf(copiedOutputs);
    }

    public Payload toPayload() {
        return new Payload(
                columns.stream()
                        .map(column -> column.stream()
                                .map(stack -> stack == null
                                        ? null
                                        : GenericStack.writeTag(stack).toString())
                                .toList())
                        .toList(),
                outputs.stream()
                        .map(stack -> GenericStack.writeTag(stack).toString())
                        .toList());
    }

    private static void validateStack(GenericStack stack) {
        if (stack == null || stack.what() == null || stack.amount() <= 0) {
            throw new IllegalArgumentException("Advanced pattern import contains an empty stack");
        }
    }

    /** GSON-safe client-action payload; AE keys are encoded with AE2's generic NBT format. */
    public record Payload(List<List<String>> columns, List<String> outputs) {
        public int serializedLength() {
            return GSON.toJson(this).length();
        }

        public AdvancedPatternTransferPlan decode() throws CommandSyntaxException {
            if (columns == null || outputs == null) {
                throw new IllegalArgumentException("Advanced pattern import payload is incomplete");
            }
            if (serializedLength() > MAX_CLIENT_ACTION_JSON_LENGTH) {
                throw new IllegalArgumentException("Advanced pattern import payload exceeds the client-action limit");
            }
            if (columns.isEmpty()
                    || columns.size() > AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS
                    || outputs.isEmpty()
                    || outputs.size() > AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS) {
                throw new IllegalArgumentException("Advanced pattern import payload has invalid recipe dimensions");
            }
            List<List<GenericStack>> decodedColumns = new ArrayList<>(columns.size());
            for (List<String> column : columns) {
                if (column == null
                        || column.size() > AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE) {
                    throw new IllegalArgumentException("Advanced pattern import payload has an invalid column");
                }
                List<GenericStack> decodedColumn = new ArrayList<>(column.size());
                for (String stack : column) {
                    decodedColumn.add(stack == null ? null : decodeStack(stack));
                }
                decodedColumns.add(decodedColumn);
            }

            List<GenericStack> decodedOutputs = new ArrayList<>(outputs.size());
            for (String stack : outputs) {
                decodedOutputs.add(decodeStack(stack));
            }
            return new AdvancedPatternTransferPlan(decodedColumns, decodedOutputs);
        }

        private static GenericStack decodeStack(String serialized) throws CommandSyntaxException {
            if (serialized == null || serialized.length() > MAX_STACK_SNBT_LENGTH) {
                throw new IllegalArgumentException("Advanced pattern import stack payload is invalid");
            }
            GenericStack stack = GenericStack.readTag(TagParser.parseTag(serialized));
            if (stack == null) {
                throw new IllegalArgumentException("Advanced pattern import stack could not be decoded");
            }
            return stack;
        }
    }

    private static void trimTrailingNulls(List<GenericStack> stacks) {
        while (!stacks.isEmpty() && stacks.get(stacks.size() - 1) == null) {
            stacks.remove(stacks.size() - 1);
        }
    }
}

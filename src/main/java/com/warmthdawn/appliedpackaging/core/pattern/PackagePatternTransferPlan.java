package com.warmthdawn.appliedpackaging.core.pattern;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.google.gson.Gson;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.TagParser;

/** A dependency-neutral JEI import plan for the package-pattern page. */
public record PackagePatternTransferPlan(
        List<GenericStack> inputs,
        GenericStack marker) {
    private static final int MAX_STACK_SNBT_LENGTH = 4096;
    private static final Gson GSON = new Gson();

    public PackagePatternTransferPlan {
        if (inputs == null || inputs.isEmpty()
                || inputs.size() > PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT) {
            throw new IllegalArgumentException("Package pattern import must contain 1 to "
                    + PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT + " inputs");
        }
        List<GenericStack> copiedInputs = new ArrayList<>(inputs.size());
        for (GenericStack input : inputs) {
            validateStack(input);
            copiedInputs.add(input);
        }
        inputs = List.copyOf(copiedInputs);

        if (marker != null) {
            validateStack(marker);
            if (!(marker.what() instanceof AEItemKey)) {
                throw new IllegalArgumentException("Package pattern marker must be an item");
            }
            marker = new GenericStack(marker.what(), 1);
        }
    }

    public Payload toPayload() {
        return new Payload(
                inputs.stream()
                        .map(stack -> GenericStack.writeTag(stack).toString())
                        .toList(),
                marker == null ? null : GenericStack.writeTag(marker).toString());
    }

    public static PackagePatternTransferPlan fromAdvanced(AdvancedPatternTransferPlan plan) {
        List<GenericStack> flattened = plan.columns().stream()
                .flatMap(List::stream)
                .filter(stack -> stack != null)
                .toList();
        GenericStack marker = plan.outputs().stream()
                .filter(stack -> stack.what() instanceof AEItemKey)
                .findFirst()
                .orElse(null);
        return new PackagePatternTransferPlan(flattened, marker);
    }

    private static void validateStack(GenericStack stack) {
        if (stack == null || stack.what() == null || stack.amount() <= 0) {
            throw new IllegalArgumentException("Package pattern import contains an empty stack");
        }
    }

    /** GSON-safe client-action payload; AE keys are encoded with AE2's generic NBT format. */
    public record Payload(List<String> inputs, String marker) {
        public int serializedLength() {
            return GSON.toJson(this).length();
        }

        public PackagePatternTransferPlan decode() throws CommandSyntaxException {
            if (inputs == null) {
                throw new IllegalArgumentException("Package pattern import payload is incomplete");
            }
            if (serializedLength() > AdvancedPatternTransferPlan.MAX_CLIENT_ACTION_JSON_LENGTH) {
                throw new IllegalArgumentException("Package pattern import payload exceeds the client-action limit");
            }
            if (inputs.isEmpty() || inputs.size() > PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT) {
                throw new IllegalArgumentException("Package pattern import payload has invalid dimensions");
            }
            List<GenericStack> decodedInputs = new ArrayList<>(inputs.size());
            for (String input : inputs) {
                decodedInputs.add(decodeStack(input));
            }
            GenericStack decodedMarker = marker == null ? null : decodeStack(marker);
            return new PackagePatternTransferPlan(decodedInputs, decodedMarker);
        }

        private static GenericStack decodeStack(String serialized) throws CommandSyntaxException {
            if (serialized == null || serialized.length() > MAX_STACK_SNBT_LENGTH) {
                throw new IllegalArgumentException("Package pattern import stack payload is invalid");
            }
            GenericStack stack = GenericStack.readTag(TagParser.parseTag(serialized));
            if (stack == null) {
                throw new IllegalArgumentException("Package pattern import stack could not be decoded");
            }
            return stack;
        }
    }
}

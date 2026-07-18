package com.warmthdawn.appliedpackaging.core.sequence_buffer;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class SequenceBufferConfiguration {
    private static final int CURRENT_VERSION = 1;
    private static final String VERSION = "version";
    private static final String AUTO_OUTPUT = "auto_output";
    private static final String BLOCKING_MODE = "blocking_mode";
    private static final String ANTI_CLOG_MODE = "anti_clog_mode";
    private static final String SYNCHRONIZED_OUTPUT = "synchronized_output";
    private static final String PATTERN_MODE = "pattern_mode";
    private static final String INPUT_DELAY = "input_delay";
    private static final String ALLOWED_INPUTS = "allowed_inputs";

    public static final int DEFAULT_INPUT_DELAY_TICKS = 1;
    public static final int MAX_INPUT_DELAY_TICKS = 72_000;

    private boolean autoOutput = true;
    private boolean blockingMode;
    private boolean antiClogMode;
    private boolean synchronizedOutput;
    private boolean patternMode;
    private int inputDelayTicks = DEFAULT_INPUT_DELAY_TICKS;
    private final LinkedHashSet<AEKey> allowedInputs = new LinkedHashSet<>();

    public SequenceBufferConfiguration() {
    }

    private SequenceBufferConfiguration(SequenceBufferConfiguration source) {
        copyFrom(source);
    }

    public SequenceBufferConfiguration copy() {
        return new SequenceBufferConfiguration(this);
    }

    public void copyFrom(SequenceBufferConfiguration source) {
        Objects.requireNonNull(source, "source");
        autoOutput = source.autoOutput;
        blockingMode = source.blockingMode;
        antiClogMode = source.antiClogMode;
        synchronizedOutput = source.synchronizedOutput;
        patternMode = source.patternMode;
        inputDelayTicks = source.inputDelayTicks;
        allowedInputs.clear();
        allowedInputs.addAll(source.allowedInputs);
    }

    public boolean autoOutput() {
        return autoOutput;
    }

    public void setAutoOutput(boolean value) {
        autoOutput = value;
    }

    public boolean blockingMode() {
        return blockingMode;
    }

    public void setBlockingMode(boolean value) {
        blockingMode = value;
    }

    public boolean antiClogMode() {
        return antiClogMode;
    }

    public void setAntiClogMode(boolean value) {
        antiClogMode = value;
    }

    public boolean synchronizedOutput() {
        return synchronizedOutput;
    }

    public void setSynchronizedOutput(boolean value) {
        synchronizedOutput = value;
    }

    public boolean patternMode() {
        return patternMode;
    }

    public void setPatternMode(boolean value) {
        patternMode = value;
    }

    public int inputDelayTicks() {
        return inputDelayTicks;
    }

    public void setInputDelayTicks(int value) {
        inputDelayTicks = Math.max(0, Math.min(MAX_INPUT_DELAY_TICKS, value));
    }

    public Set<AEKey> allowedInputs() {
        return Collections.unmodifiableSet(allowedInputs);
    }

    public void setAllowedInputs(Iterable<AEKey> values) {
        allowedInputs.clear();
        if (values != null) {
            for (AEKey value : values) {
                if (value != null) {
                    allowedInputs.add(value);
                }
            }
        }
    }

    public boolean accepts(AEKey key) {
        return key != null && (allowedInputs.isEmpty() || allowedInputs.contains(key));
    }

    public CompoundTag writeTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION, CURRENT_VERSION);
        tag.putBoolean(AUTO_OUTPUT, autoOutput);
        tag.putBoolean(BLOCKING_MODE, blockingMode);
        tag.putBoolean(ANTI_CLOG_MODE, antiClogMode);
        tag.putBoolean(SYNCHRONIZED_OUTPUT, synchronizedOutput);
        tag.putBoolean(PATTERN_MODE, patternMode);
        tag.putInt(INPUT_DELAY, inputDelayTicks);
        ListTag filters = new ListTag();
        for (AEKey key : allowedInputs) {
            filters.add(GenericStack.writeTag(new GenericStack(key, 1)));
        }
        tag.put(ALLOWED_INPUTS, filters);
        return tag;
    }

    public void readTag(CompoundTag tag) {
        autoOutput = !tag.contains(AUTO_OUTPUT, Tag.TAG_BYTE) || tag.getBoolean(AUTO_OUTPUT);
        blockingMode = tag.getBoolean(BLOCKING_MODE);
        antiClogMode = tag.getBoolean(ANTI_CLOG_MODE);
        synchronizedOutput = tag.getBoolean(SYNCHRONIZED_OUTPUT);
        patternMode = tag.getBoolean(PATTERN_MODE);
        setInputDelayTicks(tag.contains(INPUT_DELAY, Tag.TAG_INT)
                ? tag.getInt(INPUT_DELAY)
                : DEFAULT_INPUT_DELAY_TICKS);
        allowedInputs.clear();
        ListTag filters = tag.getList(ALLOWED_INPUTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < filters.size(); i++) {
            GenericStack stack = GenericStack.readTag(filters.getCompound(i));
            if (stack != null) {
                allowedInputs.add(stack.what());
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SequenceBufferConfiguration that)) {
            return false;
        }
        return autoOutput == that.autoOutput
                && blockingMode == that.blockingMode
                && antiClogMode == that.antiClogMode
                && synchronizedOutput == that.synchronizedOutput
                && patternMode == that.patternMode
                && inputDelayTicks == that.inputDelayTicks
                && allowedInputs.equals(that.allowedInputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                autoOutput,
                blockingMode,
                antiClogMode,
                synchronizedOutput,
                patternMode,
                inputDelayTicks,
                allowedInputs);
    }
}

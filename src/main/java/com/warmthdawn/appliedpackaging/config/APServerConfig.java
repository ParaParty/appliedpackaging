package com.warmthdawn.appliedpackaging.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class APServerConfig {
    public static final long DEFAULT_SEQUENCE_BUFFER_CAPACITY = 1024L;
    public static final int DEFAULT_MAX_SEQUENCE_BUFFER_LENGTH = 128;
    public static final int HARD_MAX_SEQUENCE_BUFFER_LENGTH = 2048;

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.LongValue SEQUENCE_BUFFER_CAPACITY;
    private static final ForgeConfigSpec.IntValue MAX_SEQUENCE_BUFFER_LENGTH;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("sequence_buffer");
        SEQUENCE_BUFFER_CAPACITY = builder
                .comment("Maximum amount of one AEKey stored by one sequence buffer after its one accepted input.")
                .defineInRange("capacity", DEFAULT_SEQUENCE_BUFFER_CAPACITY, 1L, Long.MAX_VALUE);
        MAX_SEQUENCE_BUFFER_LENGTH = builder
                .comment("Maximum number of blocks in one horizontal sequence-buffer structure.")
                .defineInRange(
                        "max_length",
                        DEFAULT_MAX_SEQUENCE_BUFFER_LENGTH,
                        2,
                        HARD_MAX_SEQUENCE_BUFFER_LENGTH);
        builder.pop();
        SPEC = builder.build();
    }

    private APServerConfig() {
    }

    public static long sequenceBufferCapacity() {
        return SEQUENCE_BUFFER_CAPACITY.get();
    }

    public static int maxSequenceBufferLength() {
        return MAX_SEQUENCE_BUFFER_LENGTH.get();
    }
}

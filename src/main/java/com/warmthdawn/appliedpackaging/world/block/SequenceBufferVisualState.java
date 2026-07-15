package com.warmthdawn.appliedpackaging.world.block;

import net.minecraft.util.StringRepresentable;

public enum SequenceBufferVisualState implements StringRepresentable {
    UNFORMED("unformed"),
    UNFORMED_DIRECTED("unformed_directed"),
    ENDPOINT("endpoint"),
    MEMBER("member"),
    MEMBER_DIRECTED("member_directed");

    private final String serializedName;

    SequenceBufferVisualState(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public boolean isFormed() {
        return this == ENDPOINT || this == MEMBER || this == MEMBER_DIRECTED;
    }

    public boolean isDirected() {
        return this == UNFORMED_DIRECTED || this == ENDPOINT || this == MEMBER_DIRECTED;
    }

    public boolean isMember() {
        return this == MEMBER || this == MEMBER_DIRECTED;
    }
}

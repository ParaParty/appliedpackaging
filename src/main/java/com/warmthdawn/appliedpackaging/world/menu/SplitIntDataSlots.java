package com.warmthdawn.appliedpackaging.world.menu;

import java.util.Objects;
import java.util.function.IntSupplier;
import net.minecraft.world.inventory.DataSlot;

public final class SplitIntDataSlots {
    private static final int WORD_MASK = 0xffff;

    private final boolean clientSide;
    private final IntSupplier serverValue;
    private int clientLowWord;
    private int clientHighWord;
    private final DataSlot lowWordSlot = new DataSlot() {
        @Override
        public int get() {
            return clientSide ? clientLowWord : serverValue.getAsInt() & WORD_MASK;
        }

        @Override
        public void set(int value) {
            clientLowWord = value & WORD_MASK;
        }
    };
    private final DataSlot highWordSlot = new DataSlot() {
        @Override
        public int get() {
            return clientSide ? clientHighWord : serverValue.getAsInt() >>> 16 & WORD_MASK;
        }

        @Override
        public void set(int value) {
            clientHighWord = value & WORD_MASK;
        }
    };

    public SplitIntDataSlots(boolean clientSide, IntSupplier serverValue) {
        this.clientSide = clientSide;
        this.serverValue = Objects.requireNonNull(serverValue);
    }

    public DataSlot lowWordSlot() {
        return lowWordSlot;
    }

    public DataSlot highWordSlot() {
        return highWordSlot;
    }

    public int get() {
        return clientSide
                ? (clientHighWord << 16) | clientLowWord
                : serverValue.getAsInt();
    }
}

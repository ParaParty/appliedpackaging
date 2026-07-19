package com.warmthdawn.appliedpackaging.world.menu;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.ConfigInventory;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.part.AdvancedPatternEncodingState;
import java.util.function.IntSupplier;

/**
 * A four-column menu window over the terminal's full sparse 81x81 editor.
 *
 * <p>The server maps each window slot to its current absolute column. The client deliberately keeps only a
 * window-local copy: vanilla synchronizes dynamic slots by comparing the previous and next visible window, so
 * retaining hidden absolute columns client-side would leave stale entries behind when the scroll position changes.
 */
final class AdvancedPatternInputWindow extends ConfigInventory {
    static final int VISIBLE_COLUMNS = 4;
    static final int WINDOW_SIZE = VISIBLE_COLUMNS * AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;

    private final AdvancedPatternEncodingState state;
    private final IntSupplier firstColumn;
    private final boolean clientSideWindow;

    AdvancedPatternInputWindow(
            AdvancedPatternEncodingState state,
            IntSupplier firstColumn,
            boolean clientSideWindow) {
        super(null, GenericStackInv.Mode.CONFIG_STACKS, WINDOW_SIZE, null, true);
        this.state = state;
        this.firstColumn = firstColumn;
        this.clientSideWindow = clientSideWindow;
    }

    @Override
    public GenericStack getStack(int slot) {
        if (clientSideWindow) {
            return super.getStack(slot);
        }
        int absolute = absoluteSlot(slot);
        return absolute < 0 ? null : state.inputs().getStack(absolute);
    }

    @Override
    public AEKey getKey(int slot) {
        GenericStack stack = getStack(slot);
        return stack == null ? null : stack.what();
    }

    @Override
    public long getAmount(int slot) {
        GenericStack stack = getStack(slot);
        return stack == null ? 0 : stack.amount();
    }

    @Override
    public void setStack(int slot, GenericStack stack) {
        if (clientSideWindow) {
            super.setStack(slot, stack);
            return;
        }
        int absolute = absoluteSlot(slot);
        if (absolute >= 0) {
            state.inputs().setStack(absolute, stack);
        }
    }

    int absoluteSlot(int windowSlot) {
        if (windowSlot < 0 || windowSlot >= WINDOW_SIZE) {
            return -1;
        }
        int column = firstColumn.getAsInt()
                + windowSlot / AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
        // Active-column count is synchronized separately by the menu. Keep the server window
        // addressable across the full matrix and let the menu/screen disable inactive columns.
        if (column < 0 || column >= AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS) {
            return -1;
        }
        int row = windowSlot % AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
        return column * AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE + row;
    }
}

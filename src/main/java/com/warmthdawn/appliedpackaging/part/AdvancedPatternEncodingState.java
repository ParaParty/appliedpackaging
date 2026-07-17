package com.warmthdawn.appliedpackaging.part;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.util.ConfigInventory;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.pattern.AdvancedPatternTransferPlan;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class AdvancedPatternEncodingState {
    private static final String STATE_TAG = "appliedpackagingAdvancedPattern";
    private static final String ACTIVE_COLUMNS = "activeColumns";
    private static final String COLUMNS = "columns";
    private static final String INDEX = "index";
    private static final String COLOR = "color";
    private static final String INPUTS = "inputs";
    private static final String OUTPUTS = "outputs";

    private final Runnable changeListener;
    private final PackageColor[] colors = new PackageColor[AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS];
    private final ConfigInventory inputs;
    private final ConfigInventory outputs;
    private int activeColumns = 1;
    private boolean loading;

    public AdvancedPatternEncodingState(Runnable changeListener) {
        this.changeListener = changeListener;
        Arrays.fill(colors, PackageColor.FLUIX);
        inputs = ConfigInventory.configStacks(
                null,
                AdvancedProcessingPatternDataStorage.MAX_INPUT_SLOTS,
                this::changed,
                true);
        outputs = ConfigInventory.configStacks(
                null,
                AdvancedProcessingPatternDataStorage.MAX_OUTPUT_SLOTS,
                this::changed,
                true);
    }

    public int activeColumns() {
        return activeColumns;
    }

    public void setActiveColumns(int activeColumns) {
        int value = Math.max(1, Math.min(activeColumns, colors.length));
        if (this.activeColumns != value) {
            this.activeColumns = value;
            changed();
        }
    }

    public boolean addColumn() {
        if (activeColumns >= colors.length) {
            return false;
        }
        colors[activeColumns] = PackageColor.FLUIX;
        clearColumnInputs(activeColumns);
        setActiveColumns(activeColumns + 1);
        return true;
    }

    /** Clears a non-empty column; an empty column is removed and following columns move left. */
    public boolean clearOrDeleteColumn(int column) {
        checkActiveColumn(column);
        if (columnHasInput(column)) {
            clearColumnInputs(column);
            return false;
        }
        if (activeColumns == 1) {
            return false;
        }

        loading = true;
        inputs.beginBatch();
        try {
            for (int current = column; current < activeColumns - 1; current++) {
                colors[current] = colors[current + 1];
                copyColumnInputs(current + 1, current);
            }
            int oldLastColumn = activeColumns - 1;
            colors[oldLastColumn] = PackageColor.FLUIX;
            int oldLastStart = oldLastColumn * AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
            for (int row = 0; row < AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE; row++) {
                inputs.setStack(oldLastStart + row, null);
            }
            activeColumns--;
        } finally {
            inputs.endBatch();
            loading = false;
        }
        changed();
        return true;
    }

    public boolean columnHasInput(int column) {
        checkColumn(column);
        int firstSlot = column * AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
        int endSlot = firstSlot + AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
        for (int slot = firstSlot; slot < endSlot; slot++) {
            if (inputs.getStack(slot) != null) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        loading = true;
        try {
            resetValues();
        } finally {
            loading = false;
        }
        changed();
    }

    /** Replaces only recipe contents; existing colors are retained for columns that still exist. */
    public void replaceRecipe(AdvancedPatternTransferPlan plan) {
        loading = true;
        inputs.beginBatch();
        outputs.beginBatch();
        try {
            inputs.clear();
            outputs.clear();
            activeColumns = plan.columns().size();
            for (int column = 0; column < plan.columns().size(); column++) {
                List<GenericStack> columnInputs = plan.columns().get(column);
                int firstSlot = column * AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
                for (int row = 0; row < columnInputs.size(); row++) {
                    inputs.setStack(firstSlot + row, columnInputs.get(row));
                }
            }
            for (int output = 0; output < plan.outputs().size(); output++) {
                outputs.setStack(output, plan.outputs().get(output));
            }
            for (int column = activeColumns; column < colors.length; column++) {
                colors[column] = PackageColor.FLUIX;
            }
        } finally {
            outputs.endBatch();
            inputs.endBatch();
            loading = false;
        }
        changed();
    }

    public PackageColor color(int column) {
        checkColumn(column);
        return colors[column];
    }

    public void setColor(int column, PackageColor color) {
        checkActiveColumn(column);
        PackageColor value = color == null ? PackageColor.FLUIX : color;
        if (colors[column] != value) {
            colors[column] = value;
            changed();
        }
    }

    public ConfigInventory inputs() {
        return inputs;
    }

    public ConfigInventory outputs() {
        return outputs;
    }

    public List<AdvancedProcessingPatternDataStorage.PackageColumn> columns(GenericStack primaryOutput) {
        Optional<MarkerSpec> marker = primaryOutput != null && primaryOutput.what() instanceof AEItemKey
                ? Optional.of(new MarkerSpec(new GenericStack(primaryOutput.what(), 1)))
                : Optional.empty();
        List<AdvancedProcessingPatternDataStorage.PackageColumn> result = new ArrayList<>(activeColumns);
        for (int column = 0; column < activeColumns; column++) {
            result.add(new AdvancedProcessingPatternDataStorage.PackageColumn(
                    column,
                    colors[column],
                    marker));
        }
        return List.copyOf(result);
    }

    public void loadFromPattern(ItemStack pattern) {
        loading = true;
        try {
            resetValues();
            Optional<AdvancedProcessingPatternDataStorage.EncodedAdvancedProcessingPattern> encoded =
                    AdvancedProcessingPatternDataStorage.read(pattern);
            if (encoded.isPresent()) {
                List<GenericStack> sparseInputs = AdvancedProcessingPatternDataStorage.readSparseInputs(pattern);
                inputs.beginBatch();
                try {
                    for (int slot = 0; slot < Math.min(sparseInputs.size(), inputs.size()); slot++) {
                        inputs.setStack(slot, sparseInputs.get(slot));
                    }
                } finally {
                    inputs.endBatch();
                }
                activeColumns = encoded.get().activeColumnCount();
                for (var column : encoded.get().columns()) {
                    colors[column.index()] = column.color();
                }
                List<GenericStack> sparseOutputs = AdvancedProcessingPatternDataStorage.readSparseOutputs(pattern);
                outputs.beginBatch();
                try {
                    for (int slot = 0; slot < Math.min(sparseOutputs.size(), outputs.size()); slot++) {
                        outputs.setStack(slot, sparseOutputs.get(slot));
                    }
                } finally {
                    outputs.endBatch();
                }
            }
        } finally {
            loading = false;
        }
        changed();
    }

    public void readFromNBT(CompoundTag data) {
        loading = true;
        try {
            resetValues();
            CompoundTag state = data.getCompound(STATE_TAG);
            activeColumns = Math.max(1, Math.min(state.getInt(ACTIVE_COLUMNS), colors.length));
            for (Tag element : state.getList(COLUMNS, Tag.TAG_COMPOUND)) {
                if (!(element instanceof CompoundTag columnTag)) {
                    continue;
                }
                int column = columnTag.getInt(INDEX);
                if (column < 0 || column >= colors.length) {
                    continue;
                }
                colors[column] = PackageColor.byId(columnTag.getString(COLOR)).orElse(PackageColor.FLUIX);
            }
            inputs.readFromChildTag(state, INPUTS);
            outputs.readFromChildTag(state, OUTPUTS);
        } finally {
            loading = false;
        }
    }

    public void writeToNBT(CompoundTag data) {
        CompoundTag state = new CompoundTag();
        state.putInt(ACTIVE_COLUMNS, activeColumns);
        ListTag columns = new ListTag();
        for (int column = 0; column < activeColumns; column++) {
            CompoundTag columnTag = new CompoundTag();
            columnTag.putInt(INDEX, column);
            columnTag.putString(COLOR, colors[column].id());
            columns.add(columnTag);
        }
        state.put(COLUMNS, columns);
        inputs.writeToChildTag(state, INPUTS);
        outputs.writeToChildTag(state, OUTPUTS);
        data.put(STATE_TAG, state);
    }

    private void copyColumnInputs(int sourceColumn, int targetColumn) {
        int sourceStart = sourceColumn * AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
        int targetStart = targetColumn * AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
        for (int row = 0; row < AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE; row++) {
            inputs.setStack(targetStart + row, inputs.getStack(sourceStart + row));
        }
    }

    private void clearColumnInputs(int column) {
        int firstSlot = column * AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE;
        inputs.beginBatch();
        try {
            for (int row = 0; row < AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE; row++) {
                inputs.setStack(firstSlot + row, null);
            }
        } finally {
            inputs.endBatch();
        }
    }

    private void resetValues() {
        activeColumns = 1;
        Arrays.fill(colors, PackageColor.FLUIX);
        inputs.clear();
        outputs.clear();
    }

    private void checkActiveColumn(int column) {
        if (column < 0 || column >= activeColumns) {
            throw new IndexOutOfBoundsException(column);
        }
    }

    private void checkColumn(int column) {
        if (column < 0 || column >= colors.length) {
            throw new IndexOutOfBoundsException(column);
        }
    }

    private void changed() {
        if (!loading) {
            changeListener.run();
        }
    }
}

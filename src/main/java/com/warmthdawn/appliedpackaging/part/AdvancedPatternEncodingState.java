package com.warmthdawn.appliedpackaging.part;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.util.ConfigInventory;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
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
    private static final String NAME = "name";
    private static final String MARKERS = "markers";

    private final Runnable changeListener;
    private final PackageColor[] colors = new PackageColor[AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS];
    private final String[] names = new String[AdvancedProcessingPatternDataStorage.MAX_PACKAGE_COLUMNS];
    private final ConfigInventory markers;
    private int activeColumns = 1;
    private boolean loading;

    public AdvancedPatternEncodingState(Runnable changeListener) {
        this.changeListener = changeListener;
        Arrays.fill(colors, PackageColor.FLUIX);
        Arrays.fill(names, "");
        markers = ConfigInventory.configTypes(this::isAllowedMarker, colors.length, this::changed);
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
        setActiveColumns(activeColumns + 1);
        return true;
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

    public PackageColor color(int column) {
        checkColumn(column);
        return colors[column];
    }

    public void setColor(int column, PackageColor color) {
        checkColumn(column);
        PackageColor value = color == null ? PackageColor.FLUIX : color;
        if (colors[column] != value) {
            colors[column] = value;
            changed();
        }
    }

    public String name(int column) {
        checkColumn(column);
        return names[column];
    }

    public void setName(int column, String name) {
        checkColumn(column);
        String value = PackageCraftingPatternDataStorage.sanitizePackageName(name);
        if (!names[column].equals(value)) {
            names[column] = value;
            changed();
        }
    }

    public ConfigInventory markers() {
        return markers;
    }

    public List<AdvancedProcessingPatternDataStorage.PackageColumn> columns() {
        List<AdvancedProcessingPatternDataStorage.PackageColumn> result = new ArrayList<>(activeColumns);
        for (int column = 0; column < activeColumns; column++) {
            result.add(new AdvancedProcessingPatternDataStorage.PackageColumn(
                    column,
                    colors[column],
                    names[column],
                    marker(column)));
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
                activeColumns = encoded.get().activeColumnCount();
                for (var column : encoded.get().columns()) {
                    colors[column.index()] = column.color();
                    names[column.index()] = column.packageName();
                    column.marker().ifPresent(marker -> markers.setStack(
                            column.index(),
                            new GenericStack(marker.stack().what(), 1)));
                }
            } else {
                List<GenericStack> sparseInputs =
                        com.warmthdawn.appliedpackaging.core.package_data.ColoredProcessingPatternDataStorage
                                .readSparseInputs(pattern);
                int lastInput = -1;
                for (int slot = 0; slot < Math.min(sparseInputs.size(),
                        AdvancedProcessingPatternDataStorage.MAX_INPUT_SLOTS); slot++) {
                    if (sparseInputs.get(slot) != null) {
                        lastInput = slot;
                    }
                }
                activeColumns = Math.max(1,
                        Math.min(colors.length,
                                lastInput < 0 ? 1
                                        : lastInput / AdvancedProcessingPatternDataStorage.INPUTS_PER_PACKAGE + 1));
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
                names[column] = PackageCraftingPatternDataStorage.sanitizePackageName(columnTag.getString(NAME));
            }
            markers.readFromChildTag(state, MARKERS);
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
            if (!names[column].isBlank()) {
                columnTag.putString(NAME, names[column]);
            }
            columns.add(columnTag);
        }
        state.put(COLUMNS, columns);
        markers.writeToChildTag(state, MARKERS);
        data.put(STATE_TAG, state);
    }

    private Optional<MarkerSpec> marker(int column) {
        GenericStack marker = markers.getStack(column);
        if (marker == null || marker.amount() <= 0 || !AEItemKey.is(marker.what())) {
            return Optional.empty();
        }
        return Optional.of(new MarkerSpec(new GenericStack(marker.what(), 1)));
    }

    private boolean isAllowedMarker(appeng.api.stacks.AEKey key) {
        if (!(key instanceof AEItemKey itemKey)) {
            return false;
        }
        ItemStack stack = itemKey.toStack();
        return !stack.isEmpty()
                && !(stack.getItem() instanceof PackageItem)
                && !AEItems.BLANK_PATTERN.isSameAs(stack)
                && !PatternDetailsHelper.isEncodedPattern(stack);
    }

    private void resetValues() {
        activeColumns = 1;
        Arrays.fill(colors, PackageColor.FLUIX);
        Arrays.fill(names, "");
        markers.clear();
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

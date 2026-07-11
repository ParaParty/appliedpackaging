package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.registry.APItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Package-column metadata owned exclusively by the advanced processing pattern item. */
public final class AdvancedProcessingPatternDataStorage {
    public static final String PATTERN_TAG = "appliedpackaging.advanced_processing_pattern";
    public static final int MAX_PACKAGE_COLUMNS = 17;
    public static final int INPUTS_PER_PACKAGE = AEProcessingPattern.MAX_INPUT_SLOTS;
    public static final int MAX_INPUT_SLOTS = MAX_PACKAGE_COLUMNS * INPUTS_PER_PACKAGE;
    public static final int MAX_OUTPUT_SLOTS = 4;

    private static final String VERSION = "version";
    private static final String COLUMNS = "columns";
    private static final String INDEX = "index";
    private static final String COLOR = "color";
    private static final String MARKER = "marker";
    private static final int CURRENT_VERSION = 1;

    private AdvancedProcessingPatternDataStorage() {
    }

    public static boolean canStore(ItemStack stack) {
        return stack.is(APItems.ADVANCED_PROCESSING_PATTERN.get());
    }

    public static boolean hasData(ItemStack stack) {
        return canStore(stack) && stack.hasTag() && stack.getTagElement(PATTERN_TAG) != null;
    }

    public static Optional<EncodedAdvancedProcessingPattern> read(ItemStack stack) {
        if (!hasData(stack)) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTagElement(PATTERN_TAG);
        if (tag == null || tag.getInt(VERSION) != CURRENT_VERSION || !tag.contains(COLUMNS, Tag.TAG_LIST)) {
            return Optional.empty();
        }

        List<PackageColumn> columns = new ArrayList<>();
        Set<Integer> indexes = new HashSet<>();
        for (Tag element : tag.getList(COLUMNS, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag columnTag)) {
                return Optional.empty();
            }
            int index = columnTag.getInt(INDEX);
            if (index < 0 || index >= MAX_PACKAGE_COLUMNS || !indexes.add(index)) {
                return Optional.empty();
            }
            Optional<PackageColor> color = PackageColor.byId(columnTag.getString(COLOR));
            if (color.isEmpty()) {
                return Optional.empty();
            }
            Optional<MarkerSpec> marker = Optional.empty();
            if (columnTag.contains(MARKER, Tag.TAG_COMPOUND)) {
                GenericStack markerStack = GenericStack.readTag(columnTag.getCompound(MARKER));
                if (markerStack == null || markerStack.amount() <= 0 || !AEItemKey.is(markerStack.what())) {
                    return Optional.empty();
                }
                marker = Optional.of(new MarkerSpec(new GenericStack(markerStack.what(), 1)));
            }
            columns.add(new PackageColumn(index, color.get(), marker));
        }
        if (columns.isEmpty()) {
            return Optional.empty();
        }
        columns.sort(Comparator.comparingInt(PackageColumn::index));
        for (int index = 0; index < columns.size(); index++) {
            if (columns.get(index).index() != index) {
                return Optional.empty();
            }
        }
        return Optional.of(new EncodedAdvancedProcessingPattern(columns));
    }

    public static void write(ItemStack stack, EncodedAdvancedProcessingPattern pattern) {
        if (!canStore(stack)) {
            throw new IllegalArgumentException("Advanced package metadata requires an advanced processing pattern");
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION, CURRENT_VERSION);
        ListTag columns = new ListTag();
        for (PackageColumn column : pattern.columns()) {
            CompoundTag columnTag = new CompoundTag();
            columnTag.putInt(INDEX, column.index());
            columnTag.putString(COLOR, column.color().id());
            column.marker().ifPresent(marker -> columnTag.put(MARKER, GenericStack.writeTag(marker.stack())));
            columns.add(columnTag);
        }
        tag.put(COLUMNS, columns);
        stack.getOrCreateTag().put(PATTERN_TAG, tag);
    }

    public record PackageColumn(
            int index,
            PackageColor color,
            Optional<MarkerSpec> marker) {
        public PackageColumn {
            if (index < 0 || index >= MAX_PACKAGE_COLUMNS) {
                throw new IllegalArgumentException("Advanced package column is outside the supported range");
            }
            if (color == null) {
                throw new IllegalArgumentException("Advanced package column color cannot be null");
            }
            marker = marker == null ? Optional.empty() : marker;
            if (marker.isPresent() && !AEItemKey.is(marker.get().stack().what())) {
                throw new IllegalArgumentException("Advanced package markers must be items");
            }
            marker = marker.map(value -> new MarkerSpec(new GenericStack(value.stack().what(), 1)));
        }
    }

    public record EncodedAdvancedProcessingPattern(List<PackageColumn> columns) {
        public EncodedAdvancedProcessingPattern {
            if (columns == null || columns.isEmpty() || columns.size() > MAX_PACKAGE_COLUMNS) {
                throw new IllegalArgumentException("Advanced processing patterns require one to seventeen columns");
            }
            if (columns.stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalArgumentException("Advanced processing pattern columns cannot be null");
            }
            List<PackageColumn> sorted = new ArrayList<>(columns);
            sorted.sort(Comparator.comparingInt(PackageColumn::index));
            for (int index = 0; index < sorted.size(); index++) {
                PackageColumn column = sorted.get(index);
                if (column == null || column.index() != index) {
                    throw new IllegalArgumentException(
                            "Advanced processing pattern columns must be contiguous from zero");
                }
            }
            columns = List.copyOf(sorted);
        }

        public int activeColumnCount() {
            return columns.size();
        }

        public PackageColumn column(int index) {
            if (index < 0 || index >= columns.size()) {
                throw new IllegalArgumentException("Missing advanced package column " + index);
            }
            return columns.get(index);
        }
    }
}

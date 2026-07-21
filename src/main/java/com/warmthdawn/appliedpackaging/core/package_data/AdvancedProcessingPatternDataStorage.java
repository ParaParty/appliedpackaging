package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.registry.APItems;
import java.util.ArrayList;
import java.util.Collections;
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
    /**
     * A square 81x81 editor keeps transpose lossless while raising the previous
     * 17-column limit without creating thousands of menu slots.
     */
    public static final int MAX_PACKAGE_COLUMNS = AEProcessingPattern.MAX_INPUT_SLOTS;
    public static final int INPUTS_PER_PACKAGE = AEProcessingPattern.MAX_INPUT_SLOTS;
    public static final int MAX_INPUT_SLOTS = MAX_PACKAGE_COLUMNS * INPUTS_PER_PACKAGE;
    public static final int MAX_OUTPUT_SLOTS = 4;

    private static final String VERSION = "version";
    private static final String COLUMNS = "columns";
    private static final String INDEX = "index";
    private static final String COLOR = "color";
    private static final String MARKER = "marker";
    private static final String INPUTS = "inputs";
    private static final String AE2_PROCESSING_INPUTS = "in";
    private static final String AE2_PROCESSING_OUTPUTS = "out";
    private static final int CURRENT_VERSION = 2;

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
        if (tag == null || !tag.contains(COLUMNS, Tag.TAG_LIST)) {
            return Optional.empty();
        }
        int version = tag.getInt(VERSION);
        if (version != CURRENT_VERSION) {
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

            List<GenericStack> inputs = readColumnInputs(columnTag);
            if (inputs == null) {
                return Optional.empty();
            }
            columns.add(new PackageColumn(index, color.get(), marker, inputs));
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
        try {
            return Optional.of(new EncodedAdvancedProcessingPattern(columns));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static void write(ItemStack stack, EncodedAdvancedProcessingPattern pattern) {
        if (!canStore(stack)) {
            throw new IllegalArgumentException("Advanced package metadata requires an advanced processing pattern");
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION, CURRENT_VERSION);
        ListTag columns = new ListTag();
        List<GenericStack> encodedInputs = readRawStacks(stack, AE2_PROCESSING_INPUTS);
        for (PackageColumn column : pattern.columns()) {
            CompoundTag columnTag = new CompoundTag();
            columnTag.putInt(INDEX, column.index());
            columnTag.putString(COLOR, column.color().id());
            column.marker().ifPresent(marker -> columnTag.put(MARKER, GenericStack.writeTag(marker.stack())));
            List<GenericStack> columnInputs = column.inputs().isEmpty()
                    ? encodedColumnInputs(encodedInputs, column.index())
                    : column.inputs();
            columnTag.put(INPUTS, writeSparseStacks(columnInputs));
            columns.add(columnTag);
        }
        tag.put(COLUMNS, columns);
        stack.getOrCreateTag().put(PATTERN_TAG, tag);
    }

    /** Returns a flattened 81-slot-per-column view of the current column schema. */
    public static List<GenericStack> readSparseInputs(ItemStack stack) {
        Optional<EncodedAdvancedProcessingPattern> encoded = read(stack);
        if (encoded.isPresent()) {
            List<GenericStack> sparse = new ArrayList<>();
            for (PackageColumn column : encoded.get().columns()) {
                int start = column.index() * INPUTS_PER_PACKAGE;
                ensureSize(sparse, start + column.inputs().size());
                for (int row = 0; row < column.inputs().size(); row++) {
                    sparse.set(start + row, column.inputs().get(row));
                }
            }
            trimTrailingNulls(sparse);
            return Collections.unmodifiableList(sparse);
        }
        return List.of();
    }

    public static List<GenericStack> readSparseOutputs(ItemStack stack) {
        return readRawStacks(stack, AE2_PROCESSING_OUTPUTS);
    }

    /** Dense AE2 processing inputs used by pattern details and encoding. */
    public static List<GenericStack> denseInputs(EncodedAdvancedProcessingPattern pattern) {
        List<GenericStack> result = new ArrayList<>();
        for (PackageColumn column : pattern.columns()) {
            for (GenericStack stack : column.inputs()) {
                if (stack != null) {
                    result.add(stack);
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<GenericStack> readColumnInputs(CompoundTag columnTag) {
        if (!columnTag.contains(INPUTS, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag inputTags = columnTag.getList(INPUTS, Tag.TAG_COMPOUND);
        if (inputTags.size() > INPUTS_PER_PACKAGE) {
            return null;
        }
        List<GenericStack> inputs = new ArrayList<>(inputTags.size());
        for (Tag element : inputTags) {
            if (!(element instanceof CompoundTag inputTag) || inputTag.isEmpty()) {
                inputs.add(null);
                continue;
            }
            GenericStack input = GenericStack.readTag(inputTag);
            if (input == null || input.amount() <= 0) {
                return null;
            }
            inputs.add(input);
        }
        trimTrailingNulls(inputs);
        return immutableSparse(inputs);
    }

    private static List<GenericStack> readRawStacks(ItemStack stack, String key) {
        if (!stack.hasTag() || !stack.getTag().contains(key, Tag.TAG_LIST)) {
            return List.of();
        }
        List<GenericStack> stacks = new ArrayList<>();
        for (Tag element : stack.getTag().getList(key, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag entry) || entry.isEmpty()) {
                stacks.add(null);
                continue;
            }
            GenericStack value = GenericStack.readTag(entry);
            stacks.add(value != null && value.amount() > 0 ? value : null);
        }
        return Collections.unmodifiableList(stacks);
    }

    private static List<GenericStack> encodedColumnInputs(List<GenericStack> sparse, int column) {
        int start = column * INPUTS_PER_PACKAGE;
        int end = Math.min(sparse.size(), start + INPUTS_PER_PACKAGE);
        if (start >= end) {
            return List.of();
        }
        List<GenericStack> inputs = new ArrayList<>(sparse.subList(start, end));
        trimTrailingNulls(inputs);
        return immutableSparse(inputs);
    }

    private static ListTag writeSparseStacks(List<GenericStack> stacks) {
        ListTag result = new ListTag();
        for (GenericStack stack : stacks) {
            result.add(GenericStack.writeTag(stack));
        }
        while (!result.isEmpty() && result.getCompound(result.size() - 1).isEmpty()) {
            result.remove(result.size() - 1);
        }
        return result;
    }

    private static List<GenericStack> immutableSparse(List<GenericStack> stacks) {
        return Collections.unmodifiableList(new ArrayList<>(stacks));
    }

    private static void ensureSize(List<GenericStack> stacks, int size) {
        while (stacks.size() < size) {
            stacks.add(null);
        }
    }

    private static void trimTrailingNulls(List<GenericStack> stacks) {
        while (!stacks.isEmpty() && stacks.get(stacks.size() - 1) == null) {
            stacks.remove(stacks.size() - 1);
        }
    }

    public record PackageColumn(
            int index,
            PackageColor color,
            Optional<MarkerSpec> marker,
            List<GenericStack> inputs) {
        public PackageColumn(int index, PackageColor color, Optional<MarkerSpec> marker) {
            this(index, color, marker, List.of());
        }

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
            if (inputs == null || inputs.size() > INPUTS_PER_PACKAGE) {
                throw new IllegalArgumentException("Advanced package column has too many inputs");
            }
            List<GenericStack> copiedInputs = new ArrayList<>(inputs.size());
            for (GenericStack input : inputs) {
                if (input != null && input.amount() <= 0) {
                    throw new IllegalArgumentException("Advanced package column contains an invalid input");
                }
                copiedInputs.add(input);
            }
            trimTrailingNulls(copiedInputs);
            inputs = immutableSparse(copiedInputs);
        }
    }

    public record EncodedAdvancedProcessingPattern(List<PackageColumn> columns) {
        public EncodedAdvancedProcessingPattern {
            if (columns == null || columns.isEmpty() || columns.size() > MAX_PACKAGE_COLUMNS) {
                throw new IllegalArgumentException("Advanced processing patterns require one to eighty-one columns");
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

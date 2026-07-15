package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class PackageDataStorage {
    public static final String PACKAGE_TAG = "appliedpackaging.package";

    private static final String VERSION = "version";
    private static final String FLAGS = "flags";
    private static final String HASH = "hash";
    private static final String USED_UNITS = "used_units";
    private static final String USED_TYPES = "used_types";
    private static final String MARKER = "marker";
    private static final String CONTENTS = "contents";
    private static final String LAYOUT = "layout";
    private static final String SLOT_COUNT = "slot_count";
    private static final String CONTENT_SLOTS = "content_slots";

    private PackageDataStorage() {
    }

    public static boolean hasPackageData(ItemStack stack) {
        return stack.getItem() instanceof PackageItem
                && stack.hasTag()
                && stack.getTag().contains(PACKAGE_TAG, Tag.TAG_COMPOUND);
    }

    public static Optional<PackageData> read(ItemStack stack) {
        if (!(stack.getItem() instanceof PackageItem packageItem) || !hasPackageData(stack)) {
            return Optional.empty();
        }

        return readTag(stack.getTagElement(PACKAGE_TAG), packageItem.color());
    }

    public static Optional<PackageData> readTag(CompoundTag tag, PackageColor color) {
        if (tag == null || !tag.contains(CONTENTS, Tag.TAG_LIST)) {
            return Optional.empty();
        }
        if (tag.getInt(VERSION) != PackageData.CURRENT_VERSION) {
            return Optional.empty();
        }

        List<GenericStack> contents = readContents(tag.getList(CONTENTS, Tag.TAG_COMPOUND));
        if (contents.isEmpty()) {
            return Optional.empty();
        }

        Optional<MarkerSpec> marker = Optional.empty();
        if (tag.contains(MARKER, Tag.TAG_COMPOUND)) {
            GenericStack markerStack = GenericStack.readTag(tag.getCompound(MARKER));
            if (markerStack != null && markerStack.amount() > 0) {
                marker = Optional.of(new MarkerSpec(markerStack));
            }
        }

        Optional<PackageLayout> layout = Optional.empty();
        if (tag.contains(LAYOUT, Tag.TAG_COMPOUND)) {
            CompoundTag layoutTag = tag.getCompound(LAYOUT);
            int[] rawSlots = layoutTag.getIntArray(CONTENT_SLOTS);
            List<Integer> contentSlots = new ArrayList<>(rawSlots.length);
            for (int rawSlot : rawSlots) {
                contentSlots.add(rawSlot);
            }
            try {
                layout = Optional.of(new PackageLayout(layoutTag.getInt(SLOT_COUNT), contentSlots));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }

        int flags = tag.getInt(FLAGS);
        PackageData computed;
        try {
            computed = PackageData.create(color, contents, layout, marker, flags);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
        String storedHash = tag.getString(HASH);
        if (storedHash.isBlank() || !storedHash.equals(computed.canonicalHash())) {
            return Optional.empty();
        }

        return Optional.of(computed);
    }

    public static void write(ItemStack stack, PackageData data) {
        if (!(stack.getItem() instanceof PackageItem)) {
            throw new IllegalArgumentException("Package data can only be written to package items");
        }
        stack.getOrCreateTag().put(PACKAGE_TAG, writeTag(data));
    }

    public static CompoundTag writeTag(PackageData data) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION, data.version());
        tag.putInt(FLAGS, data.flags());
        tag.putString(HASH, data.canonicalHash());
        tag.putLong(USED_UNITS, data.usedUnits());
        tag.putInt(USED_TYPES, data.usedTypes());

        data.marker().ifPresent(marker -> tag.put(MARKER, GenericStack.writeTag(marker.stack())));
        data.layout().ifPresent(layout -> {
            CompoundTag layoutTag = new CompoundTag();
            layoutTag.putInt(SLOT_COUNT, layout.slotCount());
            layoutTag.putIntArray(CONTENT_SLOTS, layout.contentSlots());
            tag.put(LAYOUT, layoutTag);
        });

        ListTag contents = new ListTag();
        for (GenericStack stack : data.contents()) {
            contents.add(GenericStack.writeTag(stack));
        }
        tag.put(CONTENTS, contents);
        return tag;
    }

    private static List<GenericStack> readContents(ListTag list) {
        List<GenericStack> contents = new ArrayList<>();
        for (Tag element : list) {
            if (element instanceof CompoundTag entryTag) {
                GenericStack stack = GenericStack.readTag(entryTag);
                if (stack != null && stack.amount() > 0) {
                    contents.add(stack);
                }
            }
        }
        return contents;
    }
}

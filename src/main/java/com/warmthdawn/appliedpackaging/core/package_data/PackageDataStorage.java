package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageItem;
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

        CompoundTag tag = stack.getTagElement(PACKAGE_TAG);
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

        int flags = tag.getInt(FLAGS);
        PackageData computed = PackageData.create(packageItem.color(), contents, marker, flags);
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

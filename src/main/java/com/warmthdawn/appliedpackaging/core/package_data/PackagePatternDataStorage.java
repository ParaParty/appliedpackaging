package com.warmthdawn.appliedpackaging.core.package_data;

import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.registry.APItems;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class PackagePatternDataStorage {
    public static final String PATTERN_TAG = "appliedpackaging.package_pattern";
    private static final ResourceLocation AE2_BLANK_PATTERN_ID = ResourceLocation.tryParse("ae2:blank_pattern");

    private static final String VERSION = "version";
    private static final String COLOR = "color";
    private static final String PACKAGE = "package";
    private static final int CURRENT_VERSION = 1;

    private PackagePatternDataStorage() {
    }

    public static boolean canStore(ItemStack stack) {
        return stack.is(APItems.PACKAGE_PATTERN.get())
                || stack.is(APItems.PACKAGED_PROCESSING_PATTERN.get())
                || isAe2BlankPattern(stack);
    }

    public static boolean isAe2BlankPattern(ItemStack stack) {
        return !stack.isEmpty()
                && AE2_BLANK_PATTERN_ID != null
                && AE2_BLANK_PATTERN_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static Optional<EncodedPackagePattern> read(ItemStack stack) {
        if (!canStore(stack) || !stack.hasTag()) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTagElement(PATTERN_TAG);
        if (tag == null || tag.getInt(VERSION) != CURRENT_VERSION || !tag.contains(PACKAGE, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        Optional<PackageColor> color = PackageColor.byId(tag.getString(COLOR));
        if (color.isEmpty()) {
            return Optional.empty();
        }
        return PackageDataStorage.readTag(tag.getCompound(PACKAGE), color.get())
                .map(data -> new EncodedPackagePattern(color.get(), data));
    }

    public static void write(ItemStack stack, PackageColor color, PackageData data) {
        if (!canStore(stack)) {
            throw new IllegalArgumentException("Package pattern data can only be written to package pattern carriers");
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION, CURRENT_VERSION);
        tag.putString(COLOR, color.id());
        tag.put(PACKAGE, PackageDataStorage.writeTag(data));
        stack.getOrCreateTag().put(PATTERN_TAG, tag);
    }

    public record EncodedPackagePattern(PackageColor color, PackageData data) {
    }
}

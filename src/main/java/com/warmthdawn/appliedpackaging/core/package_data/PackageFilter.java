package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public record PackageFilter(
        Optional<PackageColor> color,
        Optional<MarkerSpec> marker,
        List<GenericStack> requiredContents) {
    private static final String COLOR = "color";
    private static final String MARKER = "marker";
    private static final String REQUIRED_CONTENTS = "required_contents";

    public PackageFilter {
        color = color == null ? Optional.empty() : color;
        marker = marker == null ? Optional.empty() : marker;
        requiredContents = normalizeRequiredContents(requiredContents == null ? List.of() : requiredContents);
        for (GenericStack stack : requiredContents) {
            if (stack == null || stack.amount() <= 0) {
                throw new IllegalArgumentException("Required content entries must be non-null and positive");
            }
        }
    }

    public static PackageFilter any() {
        return new PackageFilter(Optional.empty(), Optional.empty(), List.of());
    }

    public static Optional<PackageFilter> fromTemplate(ItemStack stack) {
        Optional<PackagedProcessingPatternDataStorage.EncodedPackagedProcessingPattern> processingPattern =
                PackagedProcessingPatternDataStorage.read(stack);
        if (processingPattern.isPresent()) {
            List<GenericStack> requiredContents = new ArrayList<>();
            Optional<MarkerSpec> marker = commonMarker(processingPattern.get().packages());
            for (PackageData data : processingPattern.get().packages()) {
                requiredContents.addAll(data.contents());
            }
            return Optional.of(new PackageFilter(
                    Optional.of(processingPattern.get().color()),
                    marker,
                    PackageData.create(processingPattern.get().color(), requiredContents, Optional.empty(), 0).contents()));
        }

        Optional<PackagePatternDataStorage.EncodedPackagePattern> pattern = PackagePatternDataStorage.read(stack);
        if (pattern.isPresent()) {
            PackageData data = pattern.get().data();
            return Optional.of(new PackageFilter(
                    Optional.of(pattern.get().color()),
                    data.marker(),
                    data.contents()));
        }

        if (stack.getItem() instanceof PackageItem packageItem) {
            return PackageDataStorage.read(stack).map(data -> new PackageFilter(
                    Optional.of(packageItem.color()),
                    data.marker(),
                    data.contents()));
        }
        return Optional.empty();
    }

    public static Optional<PackageFilter> readTag(CompoundTag tag) {
        if (tag == null) {
            return Optional.empty();
        }

        Optional<PackageColor> color = Optional.empty();
        if (tag.contains(COLOR, Tag.TAG_STRING)) {
            color = PackageColor.byId(tag.getString(COLOR));
            if (color.isEmpty()) {
                return Optional.empty();
            }
        }

        Optional<MarkerSpec> marker = Optional.empty();
        if (tag.contains(MARKER, Tag.TAG_COMPOUND)) {
            GenericStack markerStack = GenericStack.readTag(tag.getCompound(MARKER));
            if (markerStack != null && markerStack.amount() > 0) {
                marker = Optional.of(new MarkerSpec(markerStack));
            }
        }

        List<GenericStack> requiredContents = new ArrayList<>();
        if (tag.contains(REQUIRED_CONTENTS, Tag.TAG_LIST)) {
            for (Tag element : tag.getList(REQUIRED_CONTENTS, Tag.TAG_COMPOUND)) {
                if (element instanceof CompoundTag entryTag) {
                    GenericStack stack = GenericStack.readTag(entryTag);
                    if (stack != null && stack.amount() > 0) {
                        requiredContents.add(stack);
                    }
                }
            }
        }

        try {
            return Optional.of(new PackageFilter(color, marker, requiredContents));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public CompoundTag writeTag() {
        CompoundTag tag = new CompoundTag();
        color.ifPresent(value -> tag.putString(COLOR, value.id()));
        marker.ifPresent(value -> tag.put(MARKER, GenericStack.writeTag(value.stack())));

        ListTag contents = new ListTag();
        for (GenericStack stack : requiredContents) {
            contents.add(GenericStack.writeTag(stack));
        }
        tag.put(REQUIRED_CONTENTS, contents);
        return tag;
    }

    public boolean isAny() {
        return color.isEmpty() && marker.isEmpty() && requiredContents.isEmpty();
    }

    public boolean matches(PackageColor packageColor, PackageData data) {
        return matches(packageColor, data, false);
    }

    public boolean matches(PackageColor packageColor, PackageData data, boolean invertContents) {
        if (color.isPresent() && color.get() != packageColor) {
            return false;
        }
        if (marker.isPresent() && !data.marker().map(actual -> actual.sameAs(marker.get())).orElse(false)) {
            return false;
        }
        return matchesContents(data, invertContents);
    }

    public boolean matchesRequiredAmounts(PackageColor packageColor, PackageData data) {
        if (color.isPresent() && color.get() != packageColor) {
            return false;
        }
        if (marker.isPresent() && !data.marker().map(actual -> actual.sameAs(marker.get())).orElse(false)) {
            return false;
        }
        if (requiredContents.isEmpty()) {
            return true;
        }

        Map<AEKey, Long> available = aggregate(data.contents());
        for (GenericStack required : requiredContents) {
            long amount = available.getOrDefault(required.what(), 0L);
            if (amount < required.amount()) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesContents(PackageData data, boolean invertContents) {
        if (requiredContents.isEmpty()) {
            return true;
        }
        for (GenericStack content : data.contents()) {
            if (!allowsContent(content.what(), invertContents)) {
                return false;
            }
        }
        return true;
    }

    public boolean allowsContent(AEKey key, boolean invertContents) {
        if (requiredContents.isEmpty()) {
            return true;
        }
        boolean listed = containsContentKey(key);
        return invertContents ? !listed : listed;
    }

    public boolean containsContentKey(AEKey key) {
        for (GenericStack stack : requiredContents) {
            if (stack.what().equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<MarkerSpec> commonMarker(List<PackageData> packages) {
        Optional<MarkerSpec> marker = Optional.empty();
        for (PackageData data : packages) {
            if (data.marker().isEmpty()) {
                return Optional.empty();
            }
            if (marker.isEmpty()) {
                marker = data.marker();
            } else if (!marker.get().sameAs(data.marker().get())) {
                return Optional.empty();
            }
        }
        return marker;
    }

    private static List<GenericStack> normalizeRequiredContents(List<GenericStack> contents) {
        Map<AEKey, Long> amounts = new LinkedHashMap<>();
        for (GenericStack stack : contents) {
            if (stack == null || stack.amount() <= 0) {
                continue;
            }
            amounts.merge(stack.what(), stack.amount(), Long::sum);
        }

        List<GenericStack> normalized = new ArrayList<>();
        amounts.forEach((key, amount) -> {
            if (amount > 0) {
                normalized.add(new GenericStack(key, amount));
            }
        });
        return List.copyOf(normalized);
    }

    private static Map<AEKey, Long> aggregate(List<GenericStack> stacks) {
        Map<AEKey, Long> amounts = new HashMap<>();
        for (GenericStack stack : stacks) {
            amounts.merge(stack.what(), stack.amount(), Long::sum);
        }
        return amounts;
    }

}

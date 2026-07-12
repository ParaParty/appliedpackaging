package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/** Immutable OR-combination of the enabled package-bus filter rows. */
public record PackageBusFilterSet(List<Rule> rules, FuzzyMode fuzzyMode) {
    public PackageBusFilterSet {
        rules = rules == null ? List.of() : List.copyOf(rules);
        fuzzyMode = fuzzyMode == null ? FuzzyMode.IGNORE_ALL : fuzzyMode;
    }

    public static PackageBusFilterSet any() {
        return new PackageBusFilterSet(List.of(), FuzzyMode.IGNORE_ALL);
    }

    public boolean isAny() {
        return rules.stream().noneMatch(Rule::active);
    }

    public boolean matches(ItemStack stack) {
        if (!(stack.getItem() instanceof PackageItem packageItem)) {
            return false;
        }
        Optional<PackageData> data = PackageDataStorage.read(stack);
        if (data.isEmpty()) {
            return false;
        }
        boolean hasActiveRule = false;
        for (Rule rule : rules) {
            if (!rule.active()) {
                continue;
            }
            hasActiveRule = true;
            if (rule.matches(packageItem.color(), data.get(), fuzzyMode)) {
                return true;
            }
        }
        return !hasActiveRule;
    }

    public record Rule(
            Optional<PackageColor> color,
            GenericStack marker,
            List<AEKey> contents,
            boolean fuzzy,
            boolean inverted) {
        public Rule {
            color = color == null ? Optional.empty() : color;
            contents = contents == null ? List.of() : List.copyOf(contents);
        }

        public boolean active() {
            return color.isPresent() || marker != null || !contents.isEmpty();
        }

        private boolean matches(PackageColor packageColor, PackageData data, FuzzyMode fuzzyMode) {
            if (color.isPresent() && color.get() != packageColor) {
                return false;
            }
            if (marker != null) {
                if (data.marker().isEmpty()
                        || !keysMatch(data.marker().get().stack().what(), marker.what(), fuzzy, fuzzyMode)) {
                    return false;
                }
            }
            if (contents.isEmpty()) {
                return true;
            }
            for (GenericStack actual : data.contents()) {
                boolean listed = contents.stream()
                        .anyMatch(configured -> keysMatch(actual.what(), configured, fuzzy, fuzzyMode));
                if (inverted ? listed : !listed) {
                    return false;
                }
            }
            return true;
        }

        private static boolean keysMatch(AEKey actual, AEKey configured, boolean fuzzy, FuzzyMode fuzzyMode) {
            return fuzzy ? actual.fuzzyEquals(configured, fuzzyMode) : actual.equals(configured);
        }
    }
}

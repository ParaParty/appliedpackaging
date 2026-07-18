package com.warmthdawn.appliedpackaging.integration.jei;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.MEStorageMenu;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Chooses recipe alternatives with the same inventory priorities as AE2's pattern terminal.
 * Network entries outrank player-inventory entries, and unavailable candidates are only fallbacks.
 */
public final class RecipeIngredientSelector {
    private static final Comparator<GridInventoryEntry> ENTRY_COMPARATOR = Comparator
            .comparing(GridInventoryEntry::isCraftable)
            .thenComparing(RecipeIngredientSelector::isUndamaged)
            .thenComparing(GridInventoryEntry::getStoredAmount);

    private static final RecipeIngredientSelector EMPTY = new RecipeIngredientSelector(Map.of());

    private final Map<AEKey, Integer> priorities;

    public RecipeIngredientSelector(Map<AEKey, Integer> priorities) {
        this.priorities = Map.copyOf(priorities);
    }

    public static RecipeIngredientSelector empty() {
        return EMPTY;
    }

    public static RecipeIngredientSelector fromMenu(MEStorageMenu menu) {
        var clientRepo = menu.getClientRepo();
        Collection<GridInventoryEntry> networkEntries =
                clientRepo == null ? List.of() : clientRepo.getAllEntries();
        return fromInventoryEntries(networkEntries, menu.getPlayerInventory().items);
    }

    public static RecipeIngredientSelector fromInventoryEntries(
            Collection<GridInventoryEntry> networkEntries,
            Iterable<ItemStack> playerItems) {
        Map<AEKey, Integer> priorities = new HashMap<>();
        if (networkEntries != null && !networkEntries.isEmpty()) {
            List<GridInventoryEntry> orderedEntries = new ArrayList<>(networkEntries);
            orderedEntries.sort(ENTRY_COMPARATOR);
            for (int index = 0; index < orderedEntries.size(); index++) {
                AEKey key = orderedEntries.get(index).getWhat();
                if (key != null) {
                    priorities.put(key, index);
                }
            }
        }

        if (playerItems != null) {
            for (ItemStack stack : playerItems) {
                AEItemKey key = AEItemKey.of(stack);
                if (key != null) {
                    priorities.putIfAbsent(key, -1);
                }
            }
        }
        return priorities.isEmpty() ? EMPTY : new RecipeIngredientSelector(priorities);
    }

    /**
     * Selects a network/player-owned candidate. When none is owned, the displayed candidate wins,
     * followed by the first declared candidate.
     */
    public GenericStack select(List<GenericStack> candidates, GenericStack displayedFallback) {
        GenericStack best = null;
        int bestPriority = Integer.MIN_VALUE;
        for (GenericStack candidate : distinctValidCandidates(candidates)) {
            int priority = priorities.getOrDefault(candidate.what(), Integer.MIN_VALUE);
            if (priority > bestPriority) {
                best = candidate;
                bestPriority = priority;
            }
        }
        if (best != null) {
            return best;
        }
        if (displayedFallback != null && displayedFallback.amount() > 0) {
            return displayedFallback;
        }
        return distinctValidCandidates(candidates).stream().findFirst().orElse(null);
    }

    public GenericStack select(List<GenericStack> candidates) {
        return select(candidates, null);
    }

    /**
     * Ingredient matching also considers NBT/damage variants present in the AE2 repository, matching
     * the crafting-pattern path instead of limiting the search to Ingredient#getItems().
     */
    public GenericStack select(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return null;
        }
        List<GenericStack> declaredCandidates = RecipeStackConversions.itemCandidates(ingredient);
        if (declaredCandidates.isEmpty()) {
            return null;
        }

        AEItemKey bestKey = null;
        int bestPriority = Integer.MIN_VALUE;
        for (Map.Entry<AEKey, Integer> entry : priorities.entrySet()) {
            if (entry.getKey() instanceof AEItemKey itemKey
                    && itemKey.matches(ingredient)
                    && entry.getValue() > bestPriority) {
                bestKey = itemKey;
                bestPriority = entry.getValue();
            }
        }
        if (bestKey != null) {
            long amount = declaredCandidates.get(0).amount();
            for (GenericStack candidate : declaredCandidates) {
                if (candidate.what().equals(bestKey)) {
                    amount = candidate.amount();
                    break;
                }
            }
            return new GenericStack(bestKey, amount);
        }
        return declaredCandidates.get(0);
    }

    private static List<GenericStack> distinctValidCandidates(List<GenericStack> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Map<AEKey, GenericStack> distinct = new LinkedHashMap<>();
        for (GenericStack candidate : candidates) {
            if (candidate != null && candidate.amount() > 0) {
                distinct.putIfAbsent(candidate.what(), candidate);
            }
        }
        return List.copyOf(distinct.values());
    }

    private static boolean isUndamaged(GridInventoryEntry entry) {
        return !(entry.getWhat() instanceof AEItemKey itemKey) || !itemKey.isDamaged();
    }
}

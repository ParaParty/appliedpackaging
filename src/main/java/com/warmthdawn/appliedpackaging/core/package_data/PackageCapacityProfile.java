package com.warmthdawn.appliedpackaging.core.package_data;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public enum PackageCapacityProfile {
    DEFAULT(9, 9),
    STORAGE_16K(16, 16),
    STORAGE_64K(64, 63),
    STORAGE_256K(256, 63);

    private final long unitLimit;
    private final int typeLimit;

    PackageCapacityProfile(long unitLimit, int typeLimit) {
        this.unitLimit = unitLimit;
        this.typeLimit = typeLimit;
    }

    public long unitLimit() {
        return unitLimit;
    }

    public int typeLimit() {
        return typeLimit;
    }

    public boolean fits(long usedUnits, int usedTypes) {
        return usedUnits <= unitLimit && usedTypes <= typeLimit;
    }

    public static Optional<PackageCapacityProfile> fromStorageComponent(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"ae2".equals(id.getNamespace())) {
            return Optional.empty();
        }
        return switch (id.getPath()) {
            case "cell_component_16k" -> Optional.of(STORAGE_16K);
            case "cell_component_64k" -> Optional.of(STORAGE_64K);
            case "cell_component_256k" -> Optional.of(STORAGE_256K);
            default -> Optional.empty();
        };
    }
}

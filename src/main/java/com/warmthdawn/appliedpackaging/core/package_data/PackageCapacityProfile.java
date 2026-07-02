package com.warmthdawn.appliedpackaging.core.package_data;

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
}

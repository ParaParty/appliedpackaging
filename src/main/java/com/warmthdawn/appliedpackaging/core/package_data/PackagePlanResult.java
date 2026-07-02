package com.warmthdawn.appliedpackaging.core.package_data;

import java.util.Optional;

public record PackagePlanResult(Optional<PackageData> data, Optional<PackagePlanFailure> failure) {
    public PackagePlanResult {
        data = data == null ? Optional.empty() : data;
        failure = failure == null ? Optional.empty() : failure;
        if (data.isPresent() == failure.isPresent()) {
            throw new IllegalArgumentException("A package plan result must contain either data or failure");
        }
    }

    public static PackagePlanResult success(PackageData data) {
        return new PackagePlanResult(Optional.of(data), Optional.empty());
    }

    public static PackagePlanResult failure(PackagePlanFailure failure) {
        return new PackagePlanResult(Optional.empty(), Optional.of(failure));
    }

    public boolean success() {
        return data.isPresent();
    }
}

package com.warmthdawn.appliedpackaging.core.package_data;

import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

public final class PackageCanonicalizer {
    private PackageCanonicalizer() {
    }

    public static String hash(
            PackageColor color,
            int version,
            List<GenericStack> contents,
            Optional<MarkerSpec> marker,
            int flags) {
        String canonical = canonicalString(color, version, contents, marker, flags);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", e);
        }
    }

    public static String canonicalString(
            PackageColor color,
            int version,
            List<GenericStack> contents,
            Optional<MarkerSpec> marker,
            int flags) {
        StringBuilder builder = new StringBuilder();
        builder.append("v=").append(version).append(';');
        builder.append("color=").append(color.id()).append(';');
        builder.append("flags=").append(flags).append(';');
        builder.append("marker=");
        builder.append(marker.map(MarkerSpec::stack).map(PackageCanonicalizer::canonicalStack).orElse("none"));
        builder.append(';');

        contents.stream()
                .sorted(Comparator.comparing(PackageCanonicalizer::canonicalStack))
                .forEach(stack -> builder.append("entry=").append(canonicalStack(stack)).append(';'));
        return builder.toString();
    }

    static String canonicalStack(GenericStack stack) {
        return stack.what().getType().getId()
                + "|"
                + stack.what().getId()
                + "|"
                + stack.what().toTagGeneric()
                + "|"
                + stack.amount();
    }
}

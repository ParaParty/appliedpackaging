# Changelog

All notable Applied Packaging release notes are recorded here.

## 0.1.0-dev

Initial publishable development build for Minecraft 1.20.1 Forge and Applied Energistics 2 15.4.10.

### Added

- 17 colored package items with stack-safe package data semantics.
- Package data serialization, canonical hashing, capacity calculation, marker handling, filtering, and tooltips.
- ME Package Assembler with item/fluid AE2 pattern push support and optional auto-export to adjacent AE2 storage or item inventories.
- ME Packager with adjacent item handler, fluid handler, AE2 storage endpoint packaging and unpacking, and manual/pulse/cyclic redstone operation.
- Package Pattern Terminal as an AE2 cable part item with Applied Packaging-owned part artwork for encoding package patterns on AE2 blank patterns.
- AE2 encoded processing pattern carrier support for packaged-processing package automation.
- Packaged-processing output ghost slots for item stacks and Forge fluid containers, including adjustable fluid amounts.
- Package Storage Bus, Package Export Bus, and Package Unpacking Bus with valid-package-only behavior.
- Package Bus configuration UI with ghost package template, color, marker, required-content filtering, and adjustable fluid required amounts.
- English and Simplified Chinese language files.
- Recipes, loot tables, models, item textures, block textures, GUI icons, and Forge mod metadata.
- Forge mod metadata declares the required Minecraft, Forge, AE2, and GuideME version ranges.
- GameTest coverage for package data, transactions, filters, capacity, package machines, package buses, AE2 carriers, AE2 Pattern Provider integration, and fluid transactions.
- Automated `runClientSmoke` development run for opening key in-game menus, capturing screenshots, and exiting the client.

### Changed

- Player-facing package pattern workflows now use AE2 `blank_pattern` as the main carrier.
- Local `package_pattern` and `packaged_processing_pattern` items remain registered for compatibility and tests, but are not craftable and are not shown in the creative tab.
- Package Pattern Terminal presentation was changed from a full machine cube to an AE2 cable part item with a compatible block path retained for old saves and tests.
- Package Pattern Terminal part rendering now uses Applied Packaging-owned body, face, side, back, and overlay mask textures.

### Verification

- `.\gradlew.bat compileJava` passed.
- `.\gradlew.bat runGameTestServer` passed with 112 required GameTests.
- `.\gradlew.bat runData` passed.
- `.\gradlew.bat build` passed and generated `build/libs/appliedpackaging-0.1.0-dev.jar`.
- `.\gradlew.bat runClient` smoke reached Applied Packaging initialization, SoundEngine startup, and block atlas creation with no recorded missing model or missing texture errors.
- `.\gradlew.bat runClientSmoke` passed and captured the Package Assembler, ME Packager, Package Pattern Terminal, Package Storage Bus, Package Export Bus, and Package Unpacking Bus screens.
- `.\gradlew.bat runServer --stacktrace` reached dedicated server world-load after local EULA acceptance; `latest.log` recorded `Done (2.724s)!` and no Applied Packaging client-class loading errors.
- `scripts/run-release-checks.ps1 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke` passed, reached `Done (2.413s)!`, cleaned up port 25565, and audited the refreshed server world-load log.
- `scripts/run-release-checks.ps1 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke` passed again after GuideME metadata was made explicit in `mods.toml`.
- `scripts/run-release-checks.ps1 -AuditOnly -RequireAssetContracts -RequireClientSmokeScreenshots -RequireServerWorldLoad` passed for the current baseline.
- `scripts/write-release-manifest.ps1` generated the release manifest with jar SHA-256 and git commit metadata.
- `scripts/verify-release-manifest.ps1` passed and confirmed the release manifest matches the current jar, `gradle.properties`, and git HEAD.
- `scripts/write-release-bundle.ps1` and `scripts/verify-release-bundle.ps1` generated and audited a zip containing the jar, release manifest, README, CHANGELOG, LICENSE, and SHA256SUMS.
- `scripts/verify-docs.ps1` passed required document, document index, and local Markdown link checks.
- `scripts/run-release-checks.ps1 -AuditOnly -RequireCleanGit` passed for the current committed baseline.

### Known Limitations

- Manual filter ghost editing supports item and Forge fluid-container required content; arbitrary AEKey filter editing is not yet implemented.
- Packaged-processing output ghost slots support item stacks and Forge fluid containers; arbitrary AEKey output editing is not yet implemented.
- Final release tagging is paused until the pre-release requirement and asset intake is frozen and revalidated.

# Changelog

All notable Applied Packaging release notes are recorded here.

## 0.1.0-dev

Initial publishable development build for Minecraft 1.20.1 Forge and Applied Energistics 2 15.4.10.

### Added

- 17 colored package items with stack-safe package data semantics.
- Package data serialization, canonical hashing, capacity calculation, marker handling, filtering, and tooltips.
- ME Package Assembler with item/fluid AE2 pattern push support and optional auto-export to adjacent AE2 storage or item inventories.
- ME Packager with adjacent item handler, fluid handler, AE2 storage endpoint packaging and unpacking, and manual/pulse/cyclic redstone operation.
- Package Pattern Terminal as an AE2 cable part item for encoding package patterns on AE2 blank patterns.
- AE2 encoded processing pattern carrier support for packaged-processing package automation.
- Packaged-processing output ghost slots for item stacks and Forge fluid containers, including adjustable fluid amounts.
- Package Storage Bus, Package Export Bus, and Package Unpacking Bus with valid-package-only behavior.
- Package Bus configuration UI with ghost package template, color, marker, required-content filtering, and adjustable fluid required amounts.
- English and Simplified Chinese language files.
- Recipes, loot tables, models, item textures, block textures, GUI icons, and Forge mod metadata.
- GameTest coverage for package data, transactions, filters, capacity, package machines, package buses, AE2 carriers, AE2 Pattern Provider integration, and fluid transactions.
- Automated `runClientSmoke` development run for opening key in-game menus, capturing screenshots, and exiting the client.

### Changed

- Player-facing package pattern workflows now use AE2 `blank_pattern` as the main carrier.
- Local `package_pattern` and `packaged_processing_pattern` items remain registered for compatibility and tests, but are not craftable and are not shown in the creative tab.
- Package Pattern Terminal presentation was changed from a full machine cube to an AE2 cable part item with a compatible block path retained for old saves and tests.

### Verification

- `.\gradlew.bat compileJava` passed.
- `.\gradlew.bat runGameTestServer` passed with 112 required GameTests.
- `.\gradlew.bat runData` passed.
- `.\gradlew.bat build` passed and generated `build/libs/appliedpackaging-0.1.0-dev.jar`.
- `.\gradlew.bat runClient` smoke reached Applied Packaging initialization, SoundEngine startup, and block atlas creation with no recorded missing model or missing texture errors.
- `.\gradlew.bat runClientSmoke` passed and captured the Package Assembler, ME Packager, Package Pattern Terminal, Package Storage Bus, Package Export Bus, and Package Unpacking Bus screens.

### Known Limitations

- The terminal part currently reuses the existing Applied Packaging terminal face and AE2-style part layers; final bespoke part artwork is still pending.
- Manual filter ghost editing supports item and Forge fluid-container required content; arbitrary AEKey filter editing is not yet implemented.
- Packaged-processing output ghost slots support item stacks and Forge fluid containers; arbitrary AEKey output editing is not yet implemented.
- Full dedicated-server world-load smoke is pending explicit local EULA acceptance.

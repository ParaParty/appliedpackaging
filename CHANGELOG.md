# Changelog

All notable Applied Packaging release notes are recorded here.

## 0.1.0-dev

Initial publishable development build for Minecraft 1.20.1 Forge and Applied Energistics 2 15.4.10.

### Added

- 17 colored package items with stack-safe package data semantics.
- Package data serialization, canonical hashing, capacity calculation, marker handling, filtering, and tooltips.
- ME Package Assembler with AE2 pattern push support.
- ME Packager with adjacent item handler, fluid handler, AE2 storage endpoint packaging and unpacking, and manual/pulse/cyclic redstone operation.
- Package Pattern Terminal for encoding package patterns on AE2 blank patterns.
- AE2 encoded processing pattern carrier support for packaged-processing package automation.
- Package Storage Bus, Package Export Bus, and Package Unpacking Bus with valid-package-only behavior.
- Package Bus configuration UI with ghost package template, color, marker, and required-content filtering.
- English and Simplified Chinese language files.
- Recipes, loot tables, models, item textures, block textures, GUI icons, and Forge mod metadata.
- GameTest coverage for package data, transactions, filters, capacity, package machines, package buses, AE2 carriers, AE2 Pattern Provider integration, and fluid transactions.
- Automated `runClientSmoke` development run for opening key in-game menus, capturing screenshots, and exiting the client.

### Changed

- Player-facing package pattern workflows now use AE2 `blank_pattern` as the main carrier.
- Local `package_pattern` and `packaged_processing_pattern` items remain registered for compatibility and tests, but are not craftable and are not shown in the creative tab.
- Package Pattern Terminal presentation was changed to an AE2-style thin panel block rather than a full machine cube.

### Verification

- `.\gradlew.bat compileJava` passed.
- `.\gradlew.bat runGameTestServer` passed with 95 required GameTests.
- `.\gradlew.bat runData` passed.
- `.\gradlew.bat build` passed and generated `build/libs/appliedpackaging-0.1.0-dev.jar`.
- `.\gradlew.bat runClient` smoke reached Applied Packaging initialization, SoundEngine startup, and block atlas creation with no recorded missing model or missing texture errors.
- `.\gradlew.bat runClientSmoke` passed and captured the Package Assembler, ME Packager, Package Pattern Terminal, Package Storage Bus, Package Export Bus, and Package Unpacking Bus screens.

### Known Limitations

- The terminal is a panel-like block, not a true AE2 cable part.
- Advanced manual ghost editors for fluid and arbitrary AEKey filters are not yet implemented.
- Packaged-processing output ghost slots are item-focused in the current UI.
- Full dedicated-server world-load smoke is pending explicit local EULA acceptance.

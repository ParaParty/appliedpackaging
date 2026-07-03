# Applied Packaging

Applied Packaging / 应用封装 is an Applied Energistics 2 addon for Minecraft 1.20.1 Forge. It adds colored, stackable logistics packages that let AE2 pattern automation and adjacent-storage workflows move a whole batch of resources as one routable unit.

This mod is not a general-purpose storage compression mod. A package is a logistics contract: contents, color, optional marker, capacity profile, and a canonical hash are stored together so package routing, unpacking, and AE2 automation can stay transactional.

## Current Version

```text
Mod version: 0.1.0-dev
Minecraft:   1.20.1
Forge:       47.4.10 or newer 47.x for 1.20.1
AE2:         15.4.10 to before 16.0.0
GuideME:     20.1.7
Java:        17
Loader:      Forge / javafml 47
License:     All Rights Reserved
```

The current build artifact is generated at:

```text
build/libs/appliedpackaging-0.1.0-dev.jar
```

## Features

- 17 independent package items: Fluix plus all 16 Minecraft dye colors.
- No normal empty-package gameplay path. Logistics systems reject package items without valid `PackageData`.
- Stackable packages, where a stack represents multiple identical packages rather than one larger package.
- Package contents modeled as AE2 `GenericStack` values, with item and fluid transaction paths in the core model.
- Nested packages are flattened when repackaged, so packages do not become recursive containers.
- ME Package Assembler for AE2 pattern-driven package output with optional auto-export to adjacent AE2 storage or item inventories.
- ME Packager for adjacent inventory, adjacent fluid tank, and adjacent AE2 storage endpoint packaging and unpacking, with manual, pulse, and cyclic redstone operation.
- Package Pattern Terminal as an AE2 cable part item with Applied Packaging-owned part artwork for encoding package patterns on AE2 blank patterns.
- AE2 encoded processing pattern carrier support for packaged-processing automation.
- Package Storage Bus, Package Export Bus, and Package Unpacking Bus that expose or move only valid packages.
- Color, marker, required-content, and package-template filtering for routing and unpacking.
- Adjustable fluid amounts for fluid-container processing output ghosts and package-bus required-content ghosts.
- English and Simplified Chinese language files.
- Models, item textures, block textures, GUI icons, recipes, loot tables, and Forge mod metadata.

## Gameplay Flow

Use an AE2 blank pattern in the Package Pattern Terminal to encode package data. Simple package patterns stay on the AE2 blank-pattern carrier with Applied Packaging NBT. Packaged-processing patterns with item or fluid-container output ghosts are emitted as AE2 encoded processing patterns that also carry Applied Packaging package data.

The ME Package Assembler accepts AE2 pattern pushes and outputs one or more valid package items. It supports item and fluid AE2 processing inputs, colored processing input metadata, package patterns, packaged-processing patterns, and auto-export from its back side into adjacent AE2 storage or item inventories.

The ME Packager handles adjacent storage endpoints. It can package item handlers, fluid handlers, and adjacent AE2 storage endpoints such as an ME Interface-backed subnet, then unpack complete packages transactionally into compatible targets. Its redstone mode can ignore redstone, run once on a pulse, or run repeatedly while powered.

Package buses are routing tools. They only accept valid packages, never expose package internals as loose AE2 inventory, and never auto-package loose input items.

## Installation

Install these mods together on Minecraft 1.20.1 Forge:

```text
Applied Packaging
Applied Energistics 2 15.4.10+
GuideME 20.1.7
```

For multiplayer, install Applied Packaging on both client and dedicated server. The mod registers gameplay blocks, items, menus, and AE2 integrations, so client-only or server-only installation is not a supported gameplay configuration.

## Development

Design and implementation documents live in `docs/`. Agent-specific working rules live in `AGENTS.md`.

Useful commands:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat runGameTestServer
.\gradlew.bat runData
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runClientSmoke
```

The project uses ModDevGradle Legacy with Java 17. GameTest structures are copied from `gameteststructures/` before `runGameTestServer`.

`runClientSmoke` quick-plays a local singleplayer world, places the key Applied Packaging blocks, opens their real menus, saves screenshots under `run/screenshots/`, and exits. It defaults to the local world named `New World`; override it with:

```powershell
.\gradlew.bat runClientSmoke -Pappliedpackaging.clientSmoke.world="Your World Name"
```

## Verification Status

Latest recorded verification:

```text
compileJava:       passed
runGameTestServer: passed, 112 required GameTests
runData:           passed
build:             passed
runClient smoke:   reached Minecraft client startup, Applied Packaging init, SoundEngine, and block atlas
runClientSmoke:    opened and captured package assembler, packager, pattern terminal, and all three package bus screens
runServer smoke:   reached dedicated server world-load, Done (2.724s), without Applied Packaging client-class loading errors
```

The current baseline dedicated server smoke has passed after local EULA acceptance. Final release tagging is paused until the pre-release requirement and asset intake is frozen and revalidated.

## Known Limitations

- The Package Pattern Terminal keeps a compatible block path for old saves and tests, but the player-facing item is now an AE2 cable part.
- Manual filter editing supports item and Forge fluid-container required content ghosts. A direct arbitrary AEKey ghost editor is not yet provided.
- Packaged-processing pattern output ghost slots support item stacks and Forge fluid containers. A direct arbitrary AEKey output editor is not yet provided.
- Final release tagging is pending the pre-release requirement and asset intake, followed by a fresh full verification pass.

## Documentation

- `docs/01-requirements.md` defines requirements and scope.
- `docs/02-system-architecture.md` defines module boundaries.
- `docs/03-detailed-design.md` defines data, machine, pattern, bus, filter, and transaction details.
- `docs/04-asset-spec.md` defines asset requirements and validation.
- `docs/05-implementation-plan.md` tracks implementation phases.
- `docs/06-verification-release.md` tracks tests, smoke checks, and release criteria.
- `docs/08-change-intake.md` tracks pre-release requirement and asset additions before they are migrated into the formal specs.
- `CHANGELOG.md` contains release notes.

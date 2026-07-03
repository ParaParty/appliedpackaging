# Applied Packaging

Applied Packaging / 应用封装 is an Applied Energistics 2 addon for Minecraft 1.20.1 Forge. It adds colored, stackable logistics packages that let AE2 pattern automation and adjacent-storage workflows move a whole batch of resources as one routable unit.

This mod is not a general-purpose storage compression mod. A package is a logistics contract: contents, color, optional marker, capacity profile, and a canonical hash are stored together so package routing, unpacking, and AE2 automation can stay transactional.

## Current Version

```text
Mod version: 0.1.0-dev
Minecraft:   1.20.1
Forge:       47.4.10 or newer 47.x for 1.20.1
AE2:         15.4.10 to before 16.0.0
GuideME:     20.1.7 to before 20.2.0
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
GuideME 20.1.7 to before 20.2.0
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
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-server-smoke.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireCleanGit
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\write-release-manifest.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-manifest.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\write-release-bundle.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-bundle.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-check-plan.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireAssetContracts
```

The project uses ModDevGradle Legacy with Java 17. GameTest structures are copied from `gameteststructures/` before `runGameTestServer`.

`runClientSmoke` quick-plays a local singleplayer world, places the key Applied Packaging blocks, opens their real menus, saves screenshots under `run/screenshots/`, and exits. It defaults to the local world named `New World`; override it with:

```powershell
.\gradlew.bat runClientSmoke -Pappliedpackaging.clientSmoke.world="Your World Name"
```

`scripts/verify-release.ps1` performs mechanical release checks for version metadata, jar contents, local path leaks, resource JSON, PNGs, asset contracts, language keys, model texture references, optional client smoke screenshot files, optional `latest.log` server world-load evidence, and optional clean git working-tree evidence. It also verifies Forge, Minecraft, AE2, and GuideME dependency ranges in `mods.toml` against `gradle.properties`. It does not replace the Gradle, GameTest, client smoke, or server smoke runs. Known external Yggdrasil public-key fetch failures are reported as warnings during log diagnostics; Applied Packaging, classloading, crash, missing texture, and other diagnostic keywords still fail the audit.

`scripts/run-release-checks.ps1` orchestrates the release check sequence: `build`, `runData`, `runGameTestServer`, optional `runClientSmoke`, optional `run-server-smoke.ps1`, the mechanical release audit, documentation audit, optional tag-readiness audit, optional release manifest generation/audit, and optional release bundle generation/audit. Use `-ReleaseCandidate -RequireCleanGit` to run the full technical release-candidate gate. Use `-ReleaseCandidate -RequireCleanGit -RequireReadyForTag` only after the requirement/asset intake is frozen and all release changes are committed; this also requires `scripts/verify-release-readiness.ps1 -RequireReadyForTag` to pass. The readiness gate rejects pending/blocking intake and, once blockers are gone, requires explicit positive release signals in the docs: frozen scope, final dedicated-server world-load complete, release tag allowed, goal completable, and tag-readiness gate passed. Use `scripts/test-release-check-plan.ps1` after changing the release runner or release-candidate preset to self-test the plan order, every forbidden skip flag, and server world-load guardrails without running Minecraft. Use `scripts/test-release-readiness.ps1` after changing readiness rules to self-test ready, blocked, structural-failure, and missing-positive-signal fixtures without editing the real docs. `-ReleaseCandidate` rejects `-AuditOnly` and skip flags. When `-RunClientSmoke` is used, the audit also requires all 6 smoke screenshots to exist as valid PNG files. When `-RunServerSmoke` is used, the server smoke runs after other Gradle runs, refreshes `run/logs/latest.log`, and the audit also requires dedicated server world-load evidence. Use `-RequireServerWorldLoad` only with `-AuditOnly` unless `-RunServerSmoke` is set. Use `-WriteReleaseManifest` to write `build/release/appliedpackaging-<version>-release-manifest.json` with jar size, SHA-256, version ranges, and git commit. Use `-RequireReleaseManifest` to verify that the manifest still matches the current jar, `gradle.properties`, and git HEAD. Use `-WriteReleaseBundle` and `-RequireReleaseBundle` to create and verify `build/release/appliedpackaging-<version>-release-bundle.zip` containing the jar, manifest, README, CHANGELOG, LICENSE, and SHA256SUMS; with `-RequireCleanGit`, the bundle audit also verifies that the bundled manifest git commit, branch, clean flag, and status lines match the current clean checkout.

`scripts/verify-docs.ps1` checks that the required design, release, asset-brief, asset-contract, asset-report documents, and key release scripts exist, that `docs/design.md` and `docs/00-document-index.md` still cover the document set, and that local inline Markdown links resolve.

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
runServerSmoke:    passed via release runner, Done (2.413s), and port 25565 cleaned up
release audit:     passed dependency metadata, asset contracts, client smoke screenshots, and dedicated server world-load evidence
release manifest:  generated and audited with jar SHA-256 and git commit metadata
release bundle:    generated and audited with jar, manifest, docs, license, and SHA256SUMS
release candidate: passed for the current committed baseline; final re-run pending intake freeze
tag readiness:     blocked as expected while requirement/asset intake remains open
docs audit:        passed required document and local Markdown link checks
clean git audit:   passed for the current committed baseline
```

The current baseline full release-candidate gate has passed after local EULA acceptance. Final release tagging is paused until the pre-release requirement and asset intake is frozen and revalidated.

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

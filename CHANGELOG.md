# Changelog

## 0.1.0-dev

Initial development release for Minecraft 1.20.1 Forge and Applied Energistics 2 15.4.10.

### Added

- Fluix and 16 dye-colored package items with ordered AEKey contents, sparse layouts, canonical identity, capacity accounting, marker support, and stack-safe semantics.
- ME Package Assembler for local encoded patterns and AE2 Pattern Provider pushes.
- ME Packager for adjacent item, fluid, and AE2 storage packaging/unpacking.
- Dedicated package and advanced-processing pattern carriers.
- Advanced Pattern Encoding Terminal with isolated advanced and package pages.
- Package Storage Bus and Package Unpacking Bus AE2 cable parts.
- Sequence Buffer X/Y/Z linear multiblock for ordered machine input.
- Color, marker, content, fuzzy, inverted, blocking, speed, capacity, redstone, and anti-clog controls.
- Optional JEI and EMI transfer with deterministic Create and GTCEu/Star Technology semantics.
- English, Simplified Chinese, and GuideME documentation.
- GameTest coverage for data, transactions, machines, buses, redstone, capabilities, patterns, optional recipe integrations, and sequence buffering.

### Changed

- Package contents preserve order and duplicate positions instead of merging like storage inventory.
- ME Package Assembler input uses one generic AEKey inventory; item and fluid views are derived by AE2.
- ME Packager capacity follows one quarter of the matching AE2 storage-component tier.
- Advanced patterns support 81 package columns with 81 sparse positions per column while syncing only a visible window.
- Package Unpacking Bus resolves general targets through AE2's external-storage extension path and uses an atomic position plan for Sequence Buffers.
- JEI and EMI use separate public API frontends and share only viewer-neutral recipe planning.

### Removed

- Empty-package gameplay paths and recursive package nesting.
- Package Export Bus and the separate Package Pattern Terminal.
- Development-save, old registry, old carrier, and old NBT migration paths.
- Automated client screenshot/smoke runners, mock release fixtures, manifest/bundle generators, and process-history documentation.

# Applied Packaging

Applied Packaging / 应用封装 is an Applied Energistics 2 addon for Minecraft 1.20.1 Forge. It turns ordered groups of AE2 resources into colored, stackable logistics packages that can be routed, filtered, buffered, and unpacked as one transactional unit.

Packages are not compressed storage. Their contents remain hidden from normal AE2 inventory views until a compatible machine unpacks the complete package.

## Requirements

```text
Minecraft: 1.20.1
Forge: 47.4.10+
Applied Energistics 2: [15.4.10,16)
GuideME: [20.1.7,20.2.0)
Java: 17
```

Install Applied Packaging on both the client and dedicated server. JEI, EMI, Create, and GTCEu integrations are optional.

## Features

- Fluix and all 16 dye-colored package items.
- Ordered AEKey contents with item, fluid, duplicate-entry, and sparse-layout support.
- ME Package Assembler for local encoded patterns and AE2 Pattern Provider pushes.
- ME Packager for packaging or unpacking adjacent item, fluid, and AE2 storage endpoints.
- Advanced Pattern Encoding Terminal with independent advanced-processing and package-pattern pages.
- Package Storage Bus and Package Unpacking Bus AE2 cable parts.
- Sequence Buffer linear multiblock for ordered machine input.
- Color, marker, content, fuzzy, inverted, blocking, and anti-clog routing rules.
- Optional JEI/EMI recipe transfer with explicit Create and GTCEu/Star Technology semantics.
- English, Simplified Chinese, and in-game GuideME documentation.

The mod intentionally does not provide empty packages, recursive package nesting, a Package Export Bus, or a separate Package Pattern Terminal.

## Development

The current design is documented in [docs/design.md](docs/design.md). Validation requirements are in [docs/verification.md](docs/verification.md).

```powershell
.\gradlew.bat compileJava --stacktrace
.\gradlew.bat runGameTestServer --stacktrace
.\gradlew.bat runData --stacktrace
.\gradlew.bat build --stacktrace
.\gradlew.bat runClient --stacktrace
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1
```

The built jar is written to `build/libs/`. GameTest classes are excluded from the release jar.

## License

Applied Packaging is All Rights Reserved. Bundled or adapted AE2 resources retain their upstream notices under `src/main/resources/META-INF/licenses/`.

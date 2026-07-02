# Terminal And Buses Asset Report

## Scope

Generated first-pass resources for:

- `package_pattern_terminal`
- `package_storage_bus`
- `package_export_bus`
- `package_unpacking_bus`

These resources are staged ahead of Java registration. No Java, Gradle, core design docs, language files, or other asset-package reports were modified.

## Source Inputs

- `docs/assets/asset-briefs/terminal-and-buses.md`
- `docs/assets/palette.md`
- `docs/assets/acceptance.md`
- `docs/assets/contracts/terminal_and_buses.yaml`
- Relevant semantics from `docs/03-detailed-design.md` section 9 and section 10

## Generation Method

Method: deterministic local pixel drawing with PowerShell and `System.Drawing`.

The assetgen MCP tools were not available in this session, and `assetgen` was not found on `PATH`, so this pass used direct hand-drawn 32x32 pixel textures under the contract constraints. No imagegen output was used.

Prompt / visual brief followed:

```text
AE2 fluix packaging style, quartz panel shell, dark metal frame, fluix purple-blue highlights, low-noise Minecraft pixel texture, no text labels, no cardboard-box logistics look, no brass gears.

Package Pattern Terminal:
AE2 terminal face with dark screen, small colored package cards, side 17-color lamp rail, fluix highlight frame.

Package Storage Bus:
AE2 cable-part style compact bus, sealed package symbol, dark filter slot, no unpacking or loose inventory imagery.

Package Export Bus:
AE2 cable-part style compact bus, package shifted toward an output port, fluix light rails and bright right-side port to imply output flow without text labels.

Package Unpacking Bus:
AE2 cable-part style compact bus, opened package seal, contained fluix particles, no scattered item drops and no fake loose inventory.
```

## Output Files

Textures:

- `src/main/resources/assets/appliedpackaging/textures/block/package_pattern_terminal_front.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_pattern_terminal_side.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_pattern_terminal_top.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_storage_bus_front.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_storage_bus_side.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_export_bus_front.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_export_bus_side.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_unpacking_bus_front.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_unpacking_bus_side.png`

Block models:

- `src/main/resources/assets/appliedpackaging/models/block/package_pattern_terminal.json`
- `src/main/resources/assets/appliedpackaging/models/block/package_storage_bus.json`
- `src/main/resources/assets/appliedpackaging/models/block/package_export_bus.json`
- `src/main/resources/assets/appliedpackaging/models/block/package_unpacking_bus.json`

Item models:

- `src/main/resources/assets/appliedpackaging/models/item/package_pattern_terminal.json`
- `src/main/resources/assets/appliedpackaging/models/item/package_storage_bus.json`
- `src/main/resources/assets/appliedpackaging/models/item/package_export_bus.json`
- `src/main/resources/assets/appliedpackaging/models/item/package_unpacking_bus.json`

## Model Notes

- `package_pattern_terminal` uses a full 16x16x16 block model with separate front, side, and top textures.
- The three bus models share a compact AE2 part silhouette: a 12x12x3 face plate plus a small rear connector, with all element coordinates in `0..16`.
- Each bus uses distinct front and side textures while retaining shared family proportions.
- Particle textures point to the corresponding front texture.

## Readability Notes

- Storage bus: centered sealed package plus dark bottom filter slot.
- Export bus: sealed package, right-side output port, and horizontal fluix flow rails.
- Unpacking bus: opened package flaps, split seal, and contained fluix particles.
- No text labels, numbers, watermarks, cardboard body, brass gears, or full machine bodies were used.

## Preview / Inspection

Preview paths are the generated front textures:

- `src/main/resources/assets/appliedpackaging/textures/block/package_pattern_terminal_front.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_storage_bus_front.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_export_bus_front.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_unpacking_bus_front.png`

Visual inspection was done against the 32x32 PNGs. A renderer-backed model sheet was not produced because the assetgen harness was unavailable and these blocks/parts are not registered yet.

## Verification

Commands run:

```powershell
Get-Command assetgen -ErrorAction SilentlyContinue | Format-List *
```

Result: no `assetgen` command found on `PATH`.

Local checks run after generation:

```powershell
Add-Type -AssemblyName System.Drawing
# Open each generated PNG and assert 32x32.

Get-Content -Raw src/main/resources/assets/appliedpackaging/models/block/package_pattern_terminal.json | ConvertFrom-Json
Get-Content -Raw src/main/resources/assets/appliedpackaging/models/block/package_storage_bus.json | ConvertFrom-Json
Get-Content -Raw src/main/resources/assets/appliedpackaging/models/block/package_export_bus.json | ConvertFrom-Json
Get-Content -Raw src/main/resources/assets/appliedpackaging/models/block/package_unpacking_bus.json | ConvertFrom-Json
Get-Content -Raw src/main/resources/assets/appliedpackaging/models/item/package_pattern_terminal.json | ConvertFrom-Json
Get-Content -Raw src/main/resources/assets/appliedpackaging/models/item/package_storage_bus.json | ConvertFrom-Json
Get-Content -Raw src/main/resources/assets/appliedpackaging/models/item/package_export_bus.json | ConvertFrom-Json
Get-Content -Raw src/main/resources/assets/appliedpackaging/models/item/package_unpacking_bus.json | ConvertFrom-Json

# Assert block model texture references exist and model coordinates are in 0..16.
```

Results:

- All generated PNG files are readable 32x32 images.
- All generated block and item model JSON files parse successfully.
- All generated block model texture references resolve to generated texture files.
- All generated block model element coordinates are within `0..16`.

`git status --porcelain=v1 -uall` also showed unrelated untracked assets from other concurrent asset packages, so this report only treats the terminal-and-buses file list above as this subagent's scope.

GameTest was not run for this asset-only pass because no behavior, transaction, network, storage, or serialization code changed.

## Known Limitations

- No blockstates were added because the task scope only allowed textures, block models, item models, and this report.
- Language keys were not added because language files were outside the allowed write scope.
- The bus textures include transparent corners and are intended for a cutout/part-style renderer or future AE2 part integration.
- The export bus uses light rails and an output port instead of a literal text/label arrow, following the package brief.

# Machines Asset Report

## Scope

Generated first-pass block resources for:

```text
package_assembler
me_packager
```

Only machines-scoped resources were written. Java, Gradle, core design documents, language files, and other asset package reports were not modified.

## Source Contracts

```text
docs/assets/asset-briefs/machines.md
docs/assets/palette.md
docs/assets/acceptance.md
docs/assets/contracts/package_assembler.yaml
docs/assets/contracts/me_packager.yaml
```

Both contracts were checked with the local skill wrapper:

```powershell
python "C:\Users\warmt\.codex\skills\minecraft-mod-asset-generation\scripts\assetgen" validate-contract "docs/assets/contracts/package_assembler.yaml"
python "C:\Users\warmt\.codex\skills\minecraft-mod-asset-generation\scripts\assetgen" validate-contract "docs/assets/contracts/me_packager.yaml"
```

Result: both returned `ok: true` with no warnings or errors.

## Generation Method

Textures were hand-drawn by script using `System.Drawing` rectangles and the project palette. No external image generation was used.

Visual brief used for `package_assembler`:

```text
AE2-style molecular assembler silhouette, quartz panel shell, dark metal frame, transparent-looking Fluix assembly chamber, suspended sealed package projection, pattern interface slot, 17-color indicator strip, output/blocking status lights, no cardboard, no brass gears, no text.
```

Visual brief used for `me_packager`:

```text
AE2-style packager machine, quartz panel shell, dark metal frame, north face package output hatch, side scanning intake grid, top redstone status light, capacity component window, 17-color indicator strip, no cardboard, no brass gears, no text.
```

## Output Files

```text
src/main/resources/assets/appliedpackaging/textures/block/package_assembler_front.png
src/main/resources/assets/appliedpackaging/textures/block/package_assembler_side.png
src/main/resources/assets/appliedpackaging/textures/block/package_assembler_top.png
src/main/resources/assets/appliedpackaging/textures/block/package_assembler_frame.png
src/main/resources/assets/appliedpackaging/textures/block/package_assembler_preview_sheet.png
src/main/resources/assets/appliedpackaging/textures/block/me_packager_front.png
src/main/resources/assets/appliedpackaging/textures/block/me_packager_side.png
src/main/resources/assets/appliedpackaging/textures/block/me_packager_top.png
src/main/resources/assets/appliedpackaging/textures/block/me_packager_frame.png
src/main/resources/assets/appliedpackaging/textures/block/me_packager_preview_sheet.png
src/main/resources/assets/appliedpackaging/blockstates/package_assembler.json
src/main/resources/assets/appliedpackaging/blockstates/me_packager.json
src/main/resources/assets/appliedpackaging/models/block/package_assembler.json
src/main/resources/assets/appliedpackaging/models/block/me_packager.json
src/main/resources/assets/appliedpackaging/models/item/package_assembler.json
src/main/resources/assets/appliedpackaging/models/item/me_packager.json
docs/assets/reports/machines.md
```

## Preview Records

Each preview sheet is a 2x2 concept/equivalent preview:

```text
top-left: front
top-right: right side
bottom-left: top
bottom-right: isometric
```

Preview paths:

```text
src/main/resources/assets/appliedpackaging/textures/block/package_assembler_preview_sheet.png
src/main/resources/assets/appliedpackaging/textures/block/me_packager_preview_sheet.png
```

## Model Structure

Both block models are fixed with front facing north until Java registration defines an actual facing property.

Each model uses two cuboids:

```text
body:        from [0, 0, 1]  to [16, 16, 16]
front face:  from [0, 0, 0]  to [16, 16, 1]
```

Coordinates stay inside `0..16`. The north/front face uses the functional machine texture; east/west/south use the side texture; top uses the top texture; bottom and thin panel edges use the frame texture.

## Verification

Commands run:

```powershell
python "C:\Users\warmt\.codex\skills\minecraft-mod-asset-generation\scripts\assetgen" validate-contract "docs/assets/contracts/package_assembler.yaml"
python "C:\Users\warmt\.codex\skills\minecraft-mod-asset-generation\scripts\assetgen" validate-contract "docs/assets/contracts/me_packager.yaml"
Get-ChildItem "src/main/resources/assets/appliedpackaging/textures/block/*.png" | ForEach-Object { $img=[System.Drawing.Image]::FromFile($_.FullName); [pscustomobject]@{ Name=$_.Name; Width=$img.Width; Height=$img.Height }; $img.Dispose() }
Get-ChildItem "src/main/resources/assets/appliedpackaging/blockstates/package_assembler.json", "src/main/resources/assets/appliedpackaging/blockstates/me_packager.json", "src/main/resources/assets/appliedpackaging/models/block/package_assembler.json", "src/main/resources/assets/appliedpackaging/models/block/me_packager.json", "src/main/resources/assets/appliedpackaging/models/item/package_assembler.json", "src/main/resources/assets/appliedpackaging/models/item/me_packager.json" | ForEach-Object { Get-Content -Raw $_.FullName | ConvertFrom-Json -AsHashtable | Out-Null; $_.FullName }
.\gradlew.bat build
```

Results:

```text
contract validation: ok
PNG read/dimensions: ok, machine face/frame textures are 32x32, preview sheets are 68x68
JSON parse: ok
model bounds: ok, all from/to coordinates stay in 0..16
texture references: ok
Gradle build: BUILD SUCCESSFUL in 2s
```

## Known Limitations

```text
The blocks are not registered in Java yet, so no in-game block placement, item model, or missing-texture client screenshot was possible.
Blockstates intentionally use the empty default variant instead of facing variants until the final block property shape is known.
Language keys were not added because the machines subagent write scope does not include lang files.
runData was not run because this project writes generated resources to src/generated/resources, outside the machines write scope.
runGameTestServer and runClient were not run because the current change is static resource-only and the two blocks have no registered behavior or placement path yet.
The preview sheets are hand-drawn equivalent previews, not renderer-backed assetgen model render sheets.
```

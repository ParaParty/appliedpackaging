# Terminal And Buses Asset Report

## 2026-07-13 Shared Marker Slot And Current Slot Presentation

- ME Packager and ME Package Assembler now render their empty capacity-component slot with the unchanged current-main `ae2-states.png` cell `(240,48,16,16)`; the assembler's empty encoded-pattern slot uses `(240,112,16,16)`. These are runtime atlas slices, not edits or additions to the user sprite.
- The Package Bus fuzzy, inverter, and color buttons now share a fixed `2px` top margin inside every `18px` filter row. Their sprites, click rectangles, and hover outlines use the same `rowY + 2` origin instead of the previous vertically centered `5px` origin.
- An isolated-world `runClientSmoke` refreshed all 11 screenshots. The machine captures show both current-main empty-slot cells aligned in their frames, and both bus captures show the fixed 2px logical button margin while retaining marker tooltip/hover behavior. `build`, asset audit, documentation audit, release audit with asset contracts, and `git diff --check` all pass; GameTest was not repeated because this change is client-only.
- The user clarified that the empty marker-slot artwork is their own sprite, not an AE2 source asset. Runtime now samples `package-storagebus-sprites.png` at `(32,16,16,16)` for empty marker slots in Package Storage/Unpacking Bus rows, ME Packager, ME Package Assembler, and the package mode added to AE2 Pattern Encoding Terminal.
- The supplied source `E:/resources/textures/appliedpackaging/ret/sprite.png` and runtime copy remain byte-identical with SHA-256 `14D7D26A93BF46D1BA0EF33A5408197718D0AF5BD3ADE662AA8A46E8DE662281`. No source PNG was edited, no AE2 pixels were baked into the user sprite, and the marker cell is explicitly excluded from AE2 provenance in `LICENSE.md`.
- Interactive empty marker slots show a bilingual two-line tooltip. Locked Package Bus rows render both the slot background and marker icon at the same `0.2` opacity and do not expose an interactive tooltip.
- ME Packager and ME Package Assembler now use a shared 1.20.1 backport of current-main slot hover, upgrade panel, toolbox panel, and empty-upgrade placeholder. The copied `ae2-states.png` and `package_bus_extra_panels.png` remain unchanged at SHA-256 `0996B0084C7BF37F65A97A745982AB681EBD86F142FADE526F14C823C4727E55` and `C67FED0F98C9CA67A0602B5589A5191D59D5DD2BD3848C62DE0E209E0E44B8B0`.
- Package Bus hover was corrected from menu-relative coordinates to window coordinates by applying the GUI origin before the tooltip-stage current-main highlight. The smoke cursor targets the second, empty default row; the screenshot shows the highlight on that marker slot instead of the screen's upper-left corner.
- `compileJava processResources` and an isolated-world `runClientSmoke` completed successfully. All 11 screenshots were captured; the two machines, original pattern terminal, and both buses show the user marker icon and tooltip, while machine/bus upgrade placeholders and current blue hover render correctly. The multi-image viewer again produced a black-block artifact for the Unpacking Bus, but decoding that PNG alone showed a complete normal frame.
- Final verification passed: `build`, `verify-assets.ps1`, `verify-docs.ps1`, and `verify-release.ps1 -RequireAssetContracts`. The release audit matched 237 runtime resources to the JAR, validated five asset contracts, confirmed 154 non-empty PNGs, and matched all 153 bilingual language keys/placeholders.

## 2026-07-13 Updated Package Bus Background Integration

- The updated user source `E:/resources/textures/appliedpackaging/ret/package-storagebus.png` was copied byte-for-byte to the runtime resource. Both files are 256x256 RGBA, 1583 bytes, with SHA-256 `506BE44EF826C14C1DBE37C076EDC7955C0DBFE35A7DB9B157EABA8E241787DE`.
- Relative to the prior runtime atlas, 11262 pixels changed inside the main `176x253` GUI: 10408 `#ADB0C4` pixels became current-main body color `#CBCCD4`, and 854 `#CBCCD4` pixels became current-main outer border `#413F54`.
- Ignoring RGB values under fully transparent pixels and excluding the custom center rectangle (`x=7..168`, `y=28..154`), the updated main GUI has zero visible-pixel differences from AE2 current-main `textures/guis/storagebus.png`.
- The work-slot source `[176,0,18,18]` and progress-frame source `[196,0,6,18]` are pixel-identical to the previous atlas. All seven filter rows remain present at `y=29,47,65,83,101,119,137`; `package-storagebus-sprites.png` and the three copied AE2 GUI textures were not changed.
- The `terminal_and_buses` asset contract, `verify-assets.ps1`, the full `test-assets-audit.ps1` negative-fixture suite, `runClientSmoke`, `build`, documentation audit, and release audit all pass. Both 960x540 Package Bus screenshots were inspected independently: the corrected body/border colors render normally, seven filter rows remain aligned, Storage Bus still omits the work area, and Unpacking Bus still renders it. GameTest was considered and skipped for this byte-only GUI texture replacement because no menu, network, storage, or server behavior changed.

## 2026-07-13 Current-Main Outer-Chrome Pixel Audit (Superseded)

- At the time of this diagnostic audit, the runtime `package-storagebus.png` remained byte-identical to the previous user source (`7253977C9792F7BB86D1B826688DD067AF5F242E3279A71E7409442428B53EB5`). This section is retained as the diagnosis that motivated the updated atlas above.
- Against AE2 current-main `textures/guis/storagebus.png`, the area outside the custom center rectangle (`x=7..168`, `y=28..154`) has exactly two systematic substitutions: 10246 current-main body pixels `#CBCCD4` became `#ADB0C4`, and 854 current-main outer-border pixels `#413F54` became `#CBCCD4`. This explains the darker body and missing dark outline in the in-game comparison.
- The remaining outer colors (`#F2F2F2`, `#9A9FB4`, `#ADB0C4`, `#878FA5`) are correct where they occur. The user sprite's `(0,64,18,18)` slot background is pixel-identical to current-main `states.png` `(192,192,18,18)`.
- `package_bus_extra_panels.png`, `package_bus_vertical_buttons_bg.png`, and `ae2-states.png` remain byte-identical to current main. The code-side mismatches were instead the `top=-1` upgrade anchor, the missing Help button, and the old AE2 15 grayscale empty-upgrade icon. These were corrected without changing the supplied PNGs.
- Filled upgrade cards still use the pinned AE2 15.4.10 item textures. Current-main `card_capacity.png`, `card_fuzzy.png`, `card_inverter.png`, and `card_speed.png` all differ from the old versions; they were not copied or globally overridden in this pass.
- The custom center contains seven 18px filter rows starting at `y=29`. Runtime behavior now uses the full seven rows: two base rows plus five capacity-card rows.
- A client-smoke fixture rendered an unmarked package, a marked package, and the same marked package in the held-work slot. At GUI scale 2, all three item silhouettes had the same slot-relative bounding box `(9..22,11..23)`; the marker changed three pixels but introduced no rendering offset.
- Verification passed: `runGameTestServer` (174/174), `runClientSmoke`, `build`, `verify-assets.ps1`, `verify-docs.ps1`, and `verify-release.ps1 -RequireAssetContracts`.

## 2026-07-12 GUI And Part Migration

- Package Export Bus and the standalone Package Pattern Terminal were removed from the current player-facing scope.
- Package Storage Bus now uses the AE2 Storage Bus item/part silhouette as a temporary model.
- Package Unpacking Bus now uses the AE2 ME P2P Tunnel item/part silhouette as a temporary model.
- At this initial migration point, the user-provided GUI and sprite were separate, byte-identical 256x256 RGBA files. Their then-current project hashes were `7253977C9792F7BB86D1B826688DD067AF5F242E3279A71E7409442428B53EB5` for `package-storagebus.png` and `14D7D26A93BF46D1BA0EF33A5408197718D0AF5BD3ADE662AA8A46E8DE662281` for `package-storagebus-sprites.png`; no pixels were baked into either file.
- AE2 current-main commit `45f315517ea346efc0babd02c85c6b9d32dc8acf` was read before the correction. Its `states.png`, `extra_panels.png`, and `vertical_buttons_bg.png` remain independent runtime textures, exactly as upstream does. They were copied byte-for-byte with hashes `0996B0084C7BF37F65A97A745982AB681EBD86F142FADE526F14C823C4727E55`, `C67FED0F98C9CA67A0602B5589A5191D59D5DD2BD3848C62DE0E209E0E44B8B0`, and `62150F9869EE17CBD15BDA963542287BF798482CEED1F18F0E24DD82381F7715`; the same bytes are present in `neoforge/v19.2.17`.
- The priority tab uses the current Storage Bus anchor `(152,-5,20,20)`. Unpacking Bus draws the supplied work-slot and progress-frame source regions at `(119,8)` and `(139,8)`; Storage Bus omits that layer. Disabled filter rows draw the supplied slot background at 0.2 alpha.
- The current `IconButton`, `VerticalButtonBar`, `UpgradesPanel`, and `StorageBusScreen` visual behavior was backported: 6px toolbar spacing, normal/hover/focus button sprites, current state icons, 5px upgrade-panel padding, and the connected-target hint. On the 1.20.1 renderer, explicit flush/state boundaries provide the independent-texture isolation that current AE2 obtains from per-element render states.
- Source inputs: `E:/resources/textures/appliedpackaging/ret/package-storagebus.png`, `E:/resources/textures/appliedpackaging/ret/sprite.png`, and the official AE2 checkout under `build/reference/ae2-latest`.

## Scope

Revised block texture resources for:

- `package_pattern_terminal`
- `package_storage_bus`
- `package_export_bus`
- `package_unpacking_bus`

This revision changed only the assigned terminal/bus block texture PNGs and this report. No Java, Gradle, models, item textures, GUI icons, language files, core design docs, or other asset-package reports were modified.

## Source Inputs

- `docs/assets/asset-briefs/terminal-and-buses.md`
- `docs/assets/palette.md`
- `docs/assets/acceptance.md`
- `docs/assets/contracts/terminal_and_buses.yaml`
- `build/asset-reference/ae2/ae2-parts-visual.png`
- `build/asset-reference/ae2/ae2-machines-visual.png`
- `build/asset-reference/concepts/applied-packaging-ae2-style-board.png`
- Relevant semantics from `docs/03-detailed-design.md` section 9 and section 10

## Generation Method

Method: deterministic local pixel drawing with Python and Pillow.

The AE2 reference sheets were used only for material and silhouette cues: dark part bases, gray bracket frames, quartz panels, blue glass slots, and sparse Fluix highlights. The Applied Packaging concept board was used only as broad shape/material direction for white panels, black corner caps, purple/cyan bands, and dark bus face layout. No AE2 or concept-board pixels were copied or pasted into the final Applied Packaging textures. No ImageGen output was used for the final 32x32 PNGs.

Prompt / visual brief followed:

```text
AE2-related Applied Packaging style, original pixels, white quartz panel shell, black corner caps, dark part endpoints, gray metal brackets, calm blue glass slots, sparse Fluix purple-blue highlights, low-noise Minecraft pixel texture, no text labels, no cardboard-box logistics look, no brass gears.

Package Pattern Terminal:
Block-sized terminal/encoding panel with white face, black corner caps, recessed dark screen, 3x3 purple/cyan pattern grid, compact color rail, and Fluix encode slot.

Package Storage Bus:
Dark endpoint-style full face with calm blue vault/grid slot and a sealed package mark, no motion or unpacking cue.

Package Export Bus:
Dark endpoint-style full face with package shifted toward a right-side cyan output port and short Fluix rails, no text labels.

Package Unpacking Bus:
Dark endpoint-style full face with purple open port, split package flaps, and contained Fluix seam, no scattered item drops and no fake loose inventory.
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

AE2 part textures added by the latest main-thread integration pass:

- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_front.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_sides.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_sides_status.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_back.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_bright.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_medium.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_dark.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_colored.png`

AE2 part model added by the latest main-thread integration pass:

- `src/main/resources/assets/appliedpackaging/models/part/package_pattern_terminal_base.json`

## Model Notes

- `package_pattern_terminal` now uses an AE2-style thin panel block model: a 14x14x3 face plate plus an 8x8x4 rear connector, with separate front, side, and top textures.
- The three bus models share a compact AE2 part silhouette: a 12x12x3 face plate plus a small rear connector, with all element coordinates in `0..16`.
- Each bus uses distinct front and side textures while retaining shared family proportions.
- Particle textures point to the corresponding front texture.

## Readability Notes

- Storage bus: calm blue vault/slot plus sealed package mark.
- Export bus: package shifted toward a right-side output port with short Fluix rails.
- Unpacking bus: opened package flaps, split Fluix seam, and contained particles.
- No text labels, numbers, watermarks, cardboard body, brass gears, or full machine bodies were used.

## Preview / Inspection

Preview paths are the generated front textures:

- `src/main/resources/assets/appliedpackaging/textures/block/package_pattern_terminal_front.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_storage_bus_front.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_export_bus_front.png`
- `src/main/resources/assets/appliedpackaging/textures/block/package_unpacking_bus_front.png`

Visual inspection was done against the 32x32 PNGs, the supplied AE2 no-label reference sheets, and the Applied Packaging concept board. The latest terminal pass changed model geometry from a full cube to a thin panel; runtime verification was done through client atlas/model loading rather than a rendered sheet.

## Verification

Commands run:

```powershell
python C:\Users\warmt\.codex\skills\minecraft-mod-asset-generation\scripts\assetgen validate-contract docs\assets\contracts\terminal_and_buses.yaml
```

Result: contract validation returned `ok: true` for `terminal_and_buses`.

Local checks run after generation:

```powershell
# Python/Pillow check:
# - open the nine assigned PNGs
# - assert 32x32 RGBA
# - assert alpha values are clean
# - parse relevant blockstate, block model, and item model JSON
# - assert block model texture references resolve
# - assert block model coordinates remain in 0..16
```

Results:

- All nine revised PNG files are readable 32x32 RGBA images.
- Alpha values are clean opaque (`255`) for all nine textures.
- Relevant blockstate, block model, and item model JSON files parse successfully.
- Relevant block model texture references resolve to the revised texture files.
- All generated block model element coordinates are within `0..16`.

`git status --porcelain=v1 -uall` also showed unrelated untracked assets from other concurrent asset packages, so this report only treats the terminal-and-buses file list above as this subagent's scope.

GameTest was not run for this texture-only pass because no behavior, transaction, network, storage, or serialization code changed.

## Known Limitations

- No models, blockstates, item models, language files, or code were changed in this revision because they were outside the assigned write scope.
- The bus textures are opaque full-face endpoint textures while preserving AE2/Applied Packaging visual cues through dark bases, brackets, compact center motifs, and cyan/purple feature bands.
- The export bus uses rails and an output port instead of text labels, following the package brief.

## Main-thread Integration Validation

```text
Second-pass terminal and bus textures were reviewed against AE2 forge/v15.4.10 reference sheets and the Applied Packaging ImageGen concept board.
No AE2 pixels were copied into project assets.
53 project PNG dimensions/modes/model references passed.
.\gradlew.bat runData succeeded.
.\gradlew.bat build succeeded.
.\gradlew.bat runGameTestServer succeeded with 29 required tests passing.
```

Latest terminal model pass:

```text
.\gradlew.bat compileJava succeeded.
.\gradlew.bat runGameTestServer succeeded with 79 required tests passing, including packagePatternTerminalUsesPanelShape.
.\gradlew.bat build succeeded.
.\gradlew.bat runClient reached Applied Packaging initialization, SoundEngine startup, block atlas creation, and a local world; the client was then manually terminated.
run/logs/latest.log did not contain ERROR, FATAL, Missing model, Unable to load model, preview_sheet, or mip level entries for the latest smoke.
```

Latest AE2 part artwork pass:

```text
The Package Pattern Terminal player-facing AE2 part now uses Applied Packaging-owned 16x16 body/front/back/sides textures and AP-owned overlay mask textures.
The new base part model is registered by PackagePatternTerminalPart and combined with the AP-owned off/on overlay models.
AE2 15.4.10 assets were inspected only as reference for panel geometry, dark base proportions, and overlay layering. No AE2 pixels were copied into Applied Packaging assets.
python C:\Users\warmt\.codex\skills\minecraft-mod-asset-generation\scripts\assetgen validate-contract docs\assets\contracts\terminal_and_buses.yaml succeeded.
60 project PNG files are non-empty and readable; the 8 new part PNG files are 16x16 RGBA.
55 project JSON files parse successfully; model element coordinates remain within 0..16.
.\gradlew.bat build succeeded.
.\gradlew.bat runData succeeded.
.\gradlew.bat runClientSmoke succeeded and opened the Package Pattern Terminal through the real AE2 part host.
.\gradlew.bat runGameTestServer succeeded with 112 required tests passing.
```

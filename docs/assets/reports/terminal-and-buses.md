# Terminal And Buses Asset Report

## 2026-07-17 Combined Specialized Pattern Terminal

- Package pattern editing is now a second page of `AdvancedPatternEncodingTermScreen`; no standalone Package Pattern Terminal and no package extension of AE2's ordinary Pattern Encoding Terminal remain. The combined screen uses the existing advanced terminal base and one namespaced ScreenStyle.
- Both pages now use the same 195x245 two-row frame and the same 192px bottom. The supplied base contains a 132x78 advanced panel at `(8,68)`; the supplied mode atlas contains a 132x78 package panel at `[0,0]` and a pixel-identical copy of the advanced panel at `[0,128]`. Switching pages replaces only this panel and the active editor-slot geometry, without resizing or recreating the Screen.
- The two mode controls match AE2 v19 Pattern Encoding Terminal `TabButton.Style.HORIZONTAL`: `left=173`, 22x22 normal/selected/focus backgrounds and a 21px vertical step, with package above advanced. They draw sprites rather than ItemStacks: the advanced page uses the states-atlas processing/furnace glyph `[16,32,16,16]`, and the package page uses the user sprite `[32,0,16,16]`, both at the horizontal-tab `(3,2)` icon origin.
- The combined menu suppresses `VIEW_CELL` slot creation, so the screen intentionally has no display-component panel or empty view-cell backgrounds. The ScreenStyle keeps the inherited view-cell widget off-screen only as a compatibility safeguard.
- The package panel still uses the user marker sprite, 3x3 visible input window and 27-row small scrollbar. Advanced inputs/outputs start at `(21,bottom=164)` / `(119,bottom=164)`; package inputs/marker/output start at `(24,bottom=164)` / `(109,bottom=164)` / `(112,bottom=140)`. Both pages share carrier slots `(150,bottom=165)` / `(150,bottom=118)`, Encode `(150,bottom=145)`, status controls and vertical toolbar while retaining separate editor slot inventories. The byte-preserved base and mode atlas hashes are `9586E6422D039A58C1188F5DA4F504FDE04870E4383F29E56FA9FE2752CCDD00` and `65DE82E33052D1F941182863D8303C4D22BA52C07528AC69702B9BA685153096`.
- Runtime does not paint slot interiors with flat colors. Active advanced input columns draw the AE2 v19 `SLOT_BACKGROUND [192,192,18,18]` sprite at `(slot.x-1,slot.y-1)`; inactive columns and all output slots retain the user atlas pixels. Advanced header color/clear/cycle controls use `bottom=174` and column actions use `bottom=173`, ending above the grid's `y=80` top border instead of overlapping it at the former 172/171 anchors.

## 2026-07-16 Package-mode Full-screen Delegate Base

- AE2's original `PatternEncodingTermScreen` and native-mode resources remain active outside package mode. The byte-preserved v19.2.17 `pattern.png` copy at `textures/gui/pattern_encoding_terminal.png` is used only by the same-menu package-only Screen. Its local style is namespaced at `assets/ae2/screens/appliedpackaging/pattern_encoding_terminal.json`; it does not override AE2's native terminal style, and no native `pattern_modes.png` is shipped.
- The base remains 256x256 RGBA with SHA-256 `573E8852E2590262FD5405121549F48B7B78ED79199F615FC0B068C773A1F6BE`. Dimension, required-path, fixed-hash, modified-source, and malformed-dimension fixtures are part of the asset audit.
- The package-only Screen composes this base with the existing LGPL-tracked `ae2-states.png`, current upgrade panel, vertical toolbar background, network scrollbar, user 124x66 package panel, and user marker sprite. Its style moves inherited 15.4.10 view-cell/upgrades panels outside the visible region so old and new side panels cannot overlap. Switching back to a native mode restores the retained AE2 15.4.10 Screen and its client state rather than drawing this asset.

## 2026-07-16 Advanced Pattern Encoding Terminal v19 Part Integration

- `AdvancedPatternEncodingTerminalPart` now selects its own off/on/has-channel `PartModel` sets instead of inheriting AE2 15.4.10 Pattern Encoding Terminal models. After in-game review showed that the pinned v19 world `display_base` is geometrically identical to 15.4.10, the placed Part base was corrected to use the pinned v19 six-element item display geometry translated by -7 on Z. Item and placed Part now share the front base, monitor-colored tint layer, three user-mask layers, back geometry and UVs; status layers use the v19 four-segment indicator.
- User sources `adv_pattern_encoding_terminal_dark.png`, `_medium.png`, and `_bright.png` are preserved byte-for-byte as 16x16 RGBA part textures with SHA-256 `36D633037B7B40A5B289457533F63B817F098F9DDBF0F99CBCCA47002D12D4A3`, `FE7A93FC055AD74AF9113711F799E56FE600A44E129B3D354D9F89C9BF2CCB98`, and `1488EC1F42AFFA736CB5E5687C29B9F4EC7A41C7AAC2FD28C86AE1E6EF4914CF`. They are simultaneous dark/medium/bright tint layers, not machine-state textures.
- The powered model retains v19 full-bright behavior through Forge 1.20.1 `forge_data`; the unpowered model remains environment-lit. Item colors are explicitly registered for the Applied Packaging PartItem so all v19 tint layers render outside AE2's own item registry.
- The adapted model map, eight byte-preserved AE2 texture hashes, three user-source hashes, pinned upstream commit, compatibility boundary, and LGPL license location are recorded in `META-INF/licenses/ae2-terminal-part-source.txt`.
- `assetgen validate-contract` accepts `terminal_and_buses.yaml`. `assetgen render-model` loaded the complete item model and wrote the four-view geometry sheet to `build/asset-preview/advanced_pattern_encoding_terminal-v19-sheet.png/model_render_sheet.png`; this standalone renderer does not execute Forge item-color handlers, so its tint colors are not an in-game color reference.
- The first client startup only proved resource loading and did not prove the placed model. A later focused client scene placed the real Part on a powered transparent cable, logged the client-selected custom base/on/has-channel model IDs, and captured `run/screenshots/appliedpackaging-advanced-terminal-part-v19.png`. This exposed the equal-geometry mistake above and is now the required visual path for this correction.
- GameTest was considered but not repeated: this replacement changes only client model selection, model registration, item tint registration, JSON geometry, and textures; terminal menus, persistence, network state, and server behavior are unchanged.

## 2026-07-16 Package Bus Progress Sprite Integration

- Package Unpacking Bus no longer stretches and recolors a single pixel from the empty progress frame. The active bar now samples the byte-preserved GUI atlas cell `[176,32,6,18]`, matching the unpacking-progress artwork already used by ME Packager.
- The synchronized 0-15 progress value reveals the original sprite from bottom to top with the same integer crop rule as AE2's vertical `ProgressBar`. The source width and height are never scaled, and the empty frame remains `[196,0,6,18]` at screen position `(139,8)`.
- Neither `package-storagebus.png` nor `package-storagebus-sprites.png` changed. A deterministic six-level composite (`0/3/6/9/12/15`) confirmed that the green striped sprite replaces the previous solid cyan fill without disturbing the frame.
- `compileJava`, `build`, the asset/document audits, the release audit with required asset contracts, and `git diff --check` pass. This is a client-only sampling change, so it does not alter menu synchronization, work duration, storage, filtering, or server transactions and does not require a new GameTest.

## 2026-07-15 Unified Package Color Picker Sprites

- Every player-facing package-color trigger now uses the shared `PackageColorPicker.TriggerButton`; the Advanced Pattern Terminal's private color-button renderer was removed. The same picker receives an explicit `allowNone` flag, enabled only for Package Storage/Unpacking Bus filter rows.
- The left group has a fixed two-row layout: Fluix at the top and None below, separated from the unchanged 8x2 dye grid. Hiding None reserves its row, so all picker call sites retain the same 89x23 geometry and dye coordinates. Package Bus triggers accept right-click to select None.
- The user screenshot's 6x-scaled cells were reduced exactly to 8x8 and written into previously empty atlas cells: default Fluix `(48,0)`, None `(56,0)`, and selected background `(48,8)`. The deterministic source is `scripts/update-package-color-picker-sprites.ps1`; the updated atlas SHA-256 is `632A686B6F8EC7B712326DC52E639CE43CF8E1B55C44D00309B62B672B766635`. A pixel comparison against the previous atlas confirms 192 changed pixels inside those three cells and zero changes elsewhere, including transparent RGB.
- Selection replaces only the in-cell background and never draws an external outline. Hover changes no pixels. While the popup is open, its anchor trigger suspends its own hover tooltip and restores it on close; color-name tooltips inside the popup remain available.

## 2026-07-13 AE2 v19 Bus Model Replacement

Package Storage Bus and Package Unpacking Bus now use models extracted from official AE2
`neoforge/v19.2.17` at commit `79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a`:

- Storage Bus uses `models/part/storage_bus_base.json`, its official sides/back/status materials, and the
  byte-preserved user front `pacakge_storage_bus.png` (SHA-256
  `B682F316CB77A407736E4FD73D1CAE5104F679918090F23E1D994F3E63DBA1AB`).
- Unpacking Bus uses the panel-shaped `models/part/pattern_provider_base.json`, its official side/status materials,
  and the byte-preserved user front/back textures `unpacking_panel.png` / `unpacking_panel_back.png` (SHA-256
  `A6FB292B206693865094DF901A4A0789F051630C14001C9003423F5B3E44E96F` and
  `3086B228171D19F1DFB55FDF6384165FDB78CEABB25B09C4706CE2E23599CD07`).
- World part models and inventory item models use the same v19 geometry. No global AE2 texture is overridden.

The adapted upstream models and copied official material textures remain covered by the bundled
`META-INF/licenses/ae2-LGPL-3.0-or-later.txt`.

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

The block/terminal paths in this section document the initial asset pass. They were retired on 2026-07-13 when the
standalone blocks and terminal part were removed; only the current Storage Bus and Unpacking Bus item/part paths remain
runtime assets. Their obsolete block loot tables were removed with the standalone block registrations.

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

Retired Package Pattern Terminal part textures from that integration pass:

- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_front.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_sides.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_sides_status.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_back.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_bright.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_medium.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_dark.png`
- `src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_colored.png`

Retired Package Pattern Terminal part model from that integration pass:

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

Historical AE2 part artwork pass (retired 2026-07-13):

```text
This pass described the former standalone Package Pattern Terminal AE2 part. The part implementation and its dedicated models/textures were removed on 2026-07-13 after the player entry was consolidated into the AE2 Pattern Encoding Terminal package mode. The following lines are retained only as historical validation evidence and are not current runtime claims.
AE2 15.4.10 assets were inspected only as reference for panel geometry, dark base proportions, and overlay layering. No AE2 pixels were copied into Applied Packaging assets.
python C:\Users\warmt\.codex\skills\minecraft-mod-asset-generation\scripts\assetgen validate-contract docs\assets\contracts\terminal_and_buses.yaml succeeded.
60 project PNG files are non-empty and readable; the 8 new part PNG files are 16x16 RGBA.
55 project JSON files parse successfully; model element coordinates remain within 0..16.
.\gradlew.bat build succeeded.
.\gradlew.bat runData succeeded.
.\gradlew.bat runClientSmoke succeeded and opened the Package Pattern Terminal through the real AE2 part host.
.\gradlew.bat runGameTestServer succeeded with 112 required tests passing.
```

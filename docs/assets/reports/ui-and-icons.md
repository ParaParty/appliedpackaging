# UI And Icons Asset Report

## Scope

Revised the Applied Packaging GUI icon set and scoped GUI logo after AE2 reference review.

Sources inspected:

- `docs/assets/asset-briefs/ui-and-icons.md`
- `docs/assets/palette.md`
- `docs/assets/acceptance.md`
- `docs/assets/contracts/ui_icons.yaml`
- `docs/04-asset-spec.md` UI/icon section
- `build/asset-reference/ae2/ae2-guis-visual.png`
- `build/asset-reference/ae2/ae2-items-visual.png`
- `build/asset-reference/ae2/ae2-parts-visual.png`
- `build/asset-reference/concepts/applied-packaging-ae2-style-board.png`
- `E:\resources\textures\appliedpackaging\ret\adv-pattern-terminal-base.png`
- `E:\resources\textures\appliedpackaging\ret\sprite.png`
- `E:\resources\textures\appliedpackaging\ret\pattern_mode_packaging.png`
- `build/reference/ae2-1.21.1/src/main/resources/assets/ae2/screens/terminals/pattern_encoding_terminal.json`
- `build/reference/ae2-1.21.1/src/main/java/appeng/client/gui/me/items/ProcessingEncodingPanel.java`

The original icon-set pass used the AE2 sheets only as style/material references. The later Advanced Pattern Terminal pass is an explicit exception: the user-provided base/sprite atlases contain adapted AE2 high-version GUI pixels for compact controls and scrollers, and are marked separately under `LGPL-3.0-or-later`.

## Output

Icon files are 16x16 RGBA PNGs with transparent backgrounds:

```text
src/main/resources/assets/appliedpackaging/textures/gui/icons/color_select.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/marker.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/capacity.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/blocking_mode.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/auto_export.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/pack_once.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/marker_retain.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/marker_override.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/marker_clear.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/package_filter.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/unpack_filter.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/status_error.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/status_ready.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/status_blocked.png
```

Scoped GUI logo file:

```text
src/main/resources/assets/appliedpackaging/textures/gui/logo.png
```

Advanced Pattern Terminal files:

```text
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_sprites.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_states.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_middle_row.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_scrollbar.png
src/main/resources/assets/appliedpackaging/textures/gui/pattern_mode_packaging.png
src/main/resources/META-INF/licenses/ae2-LGPL-3.0-or-later.txt
```

The four full GUI atlases are 256x256 RGBA PNGs. The advanced terminal effective two-network-row profile is 217x250: the byte-preserved 195x250 body remains unchanged and a 22px right-side horizontal-tab region is added at runtime; the package profile is 195x233 and its effective package panel is 124x66. The base, shared sprite, and package-mode files were copied byte-for-byte from the user-provided `ret` directory; the current package-mode source and project copy share SHA-256 `AB254596C0AADE263DFB5816ED4824186BCDE69DCAA8B24CF3C00BF3B7EA6256`. The states atlas was copied byte-for-byte from AE2 main/1.21.1 (SHA-256 `0996B0084C7BF37F65A97A745982AB681EBD86F142FADE526F14C823C4727E55`) for the latest primary-output, pattern-slot, encode-button, crafting-status, horizontal mode-tab, and hover-adjacent pixels. The 195x18 middle-row strip (SHA-256 `74AD33FC264C1251BE97B01731C8C99BAF393BA84630433603434690883F4962`) combines the lower row's first 17px with the upper row's final 1px so tall terminal styles repeat neither endpoint. The 256x256 scrollbar compatibility atlas (SHA-256 `CD278B712C30419ECAC84AC2FF4E27E94A08655552B232DF427270F53F82D53B`) contains the current AE2 12x15 big-scroller enabled and disabled sprites. The bundled LGPL text is byte-identical to the AE2 source checkout license.

The scoped GUI logo is a 128x128 RGBA PNG with transparent background. The existing root `src/main/resources/assets/appliedpackaging/logo.png` was not changed in this pass because it is outside the assigned write scope.

## Generation Method

Method: deterministic hand-drawn pixel art generated locally with Python and Pillow.

Advanced Pattern Terminal method: direct import of the user-provided final base/sprite atlases, followed by ScreenStyle and runtime-control adaptation. The imported files remain unchanged; only the deterministic middle-row strip and 1.20.1-compatible current-AE2 scrollbar atlas are derived resources.

Package pattern mode method: direct import of the user-provided mode atlas and reuse of the marked shared sprite. No generated resize, recolor, or cleanup was applied.

Built-in ImageGen was not invoked for final pixels because these are 16x16 UI controls; the imagegen skill guidance favors direct native editing for small icon/logo systems that must match existing project assets. The minecraft asset skill contract was inspected manually and the final file set was compared against `conversion_requirements.icon_ids`.

The revised icons use AE2-style UI language: light-gray slot panels, dark 1px frames, sharp inventory-grid silhouettes, small cyan/fluix accents, and compact status lights on transparent backgrounds.

Icon visual notes:

- `color_select`: four small color swatches plus a selected cyan/fluix chip.
- `marker`: ghost slot with a centered fluix marker chip.
- `capacity`: stacked slot/band meter with cyan and fluix rows.
- `blocking_mode`: dark slot with amber blocking plate.
- `auto_export`: package tile moving toward a cyan network-part chip.
- `pack_once`: one package tile pushed into a single output slot.
- `marker_retain`: marker chip retained in-slot with a green corner rail.
- `marker_override`: old gray marker replaced by a cyan/fluix marker in the same slot.
- `marker_clear`: marker slot crossed with a red clear slash.
- `package_filter`: filter grid containing a closed package tile.
- `unpack_filter`: filter grid with an opened package and output pixels.
- `status_error`: red status light with white cross.
- `status_ready`: green status light with check mark.
- `status_blocked`: amber status light with white blocking bar.

Logo visual note: original Applied Packaging emblem using AE2-like quartz panel material, dark corner caps, cyan/fluix bus band, colored package straps, and central package seal.

## Verification

Preflight:

```powershell
git status --short
```

Result: many unrelated Java, block texture, item texture, language, and document edits were already present. This pass only wrote the assigned GUI icon files, scoped GUI logo, and this report.

Contract tooling check:

```powershell
where.exe assetgen
```

Result: no `assetgen` command was available in PATH, and no assetgen MCP tool was available in this session. Contract validation was therefore manual against `docs/assets/contracts/ui_icons.yaml`.

PNG verification:

```powershell
@'
# Python/Pillow verification script run through stdin.
# Checked exact icon file set, dimensions, RGBA mode, clean alpha,
# transparent corners, scaled 12x12/8x8 non-transparent silhouettes,
# and scoped GUI logo transparency.
'@ | python -
```

Result:

```text
icons_expected 14
icons_actual 14
missing []
extra []
all icons: size=(16, 16), mode=RGBA, alpha_values=[0, 255], transparent_corners=[0, 0, 0, 0]
scoped gui logo: size=(128, 128), mode=RGBA, alpha=(0, 255), transparent_corners=[0, 0, 0, 0]
verification_ok
```

## Preview Paths

A local contact sheet was generated for visual inspection at:

```text
C:\Users\warmt\AppData\Local\Temp\appliedpackaging_ui_icons_ae2_refined.png
```

No project preview sheet was written because this pass was constrained to the icon directory, scoped GUI logo, and this report.

## Known Limits

- Full client resource loading was not run by this pass.
- `assetgen validate-contract` was not run because `assetgen` was unavailable in this environment.

## Main-thread Integration Validation

```text
Second-pass GUI icons and GUI logo were reviewed against AE2 forge/v15.4.10 GUI/item/part reference sheets and the Applied Packaging ImageGen concept board.
No AE2 pixels were copied into project assets.
53 project PNG dimensions/modes/model references passed.
.\gradlew.bat runData succeeded.
.\gradlew.bat build succeeded.
.\gradlew.bat runGameTestServer succeeded with 29 required tests passing.
```

## Advanced Pattern Terminal Integration

```text
The 195px-wide layout follows the AE2 1.21.1 Pattern Encoding Terminal title, search, 9-column network inventory, player inventory, blank/encoded pattern, and encode-button baselines.
Four package columns are visible at once and each retains 81 logical AE2 processing-input positions, while only three rows are visible at once; an AE2 Scrollbar using the local high-version sprite scrolls input and output rows together.
A separate horizontal scrollbar scrolls package columns. Enabled columns use a color swatch plus compact edit button; the first inactive column uses the compact add button; disabled columns contain no ghost item.
```

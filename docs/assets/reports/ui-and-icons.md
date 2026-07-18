# UI And Icons Asset Report

## Current Runtime Strategy

Applied Packaging now reuses a small set of shared atlases instead of publishing one 16x16 PNG for every proposed action. This keeps the runtime resource set aligned with the widgets that actually exist:

```text
src/main/resources/assets/appliedpackaging/logo.png
src/main/resources/assets/appliedpackaging/textures/gui/ae2-states.png
src/main/resources/assets/appliedpackaging/textures/gui/ae2-terminal.png
src/main/resources/assets/ae2/textures/guis/text_field.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_middle_row.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_scrollbar.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_sprites.png
src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus-sprites.png
src/main/resources/assets/appliedpackaging/textures/gui/pattern_mode_packaging.png
```

The root logo is the actual 128x128 mod logo. There is no second GUI-scoped logo.

## Current-AE2 Cached Resources

`ae2-states.png` remains the shared current-AE2 state atlas for slot backgrounds, terminal actions, status controls, horizontal tabs, and modern toolbar visuals. The retired byte-identical `advanced_pattern_encoding_terminal_states.png` copy was removed; all live call sites now use the shared atlas.

`ae2-terminal.png` is a byte-preserved copy of current AE2 `textures/guis/terminal.png`, SHA-256 `9CE91ECCF149E1703960906B349093AF726E6DEB753985C7E85B5D0DB359B3E4`. The Advanced Pattern Encoding Terminal uses it for the header behind the search widget and the pinned crafting row. Source and LGPL boundary are recorded in `META-INF/licenses/ae2-current-terminal-source.txt`.

`assets/ae2/textures/guis/text_field.png` is the byte-preserved current-AE2 128x128 field atlas, SHA-256 `73BBA41174D3EC15D83947E439915873611735FE436AD0CBC7653ECA15E23AD1`. It intentionally keeps AE2's namespace because the pinned `AETextField` hardcodes `guis/text_field.png`; this replaces the old dependency frame without a rendering mixin.

`advanced_pattern_encoding_terminal_scrollbar.png` is the 1.20.1-compatible current-AE2 big-scroller cache. Sequence Buffer uses its 12x15 enabled/disabled handle through `ModernScrollbarStyles.BIG`; a disabled scrollbar remains visible. The Package Assembler continues to use the 7x15 current-AE2 small-scroller pixels in the user terminal sprite atlas, with its ScreenStyle x-coordinate shifted one pixel right to align to the background track.

## Applied Packaging Sprites

The user atlases remain authoritative for package-specific visuals:

- `package-storagebus-sprites.png` provides color-picker cells, the None display, marker empty-slot art, optional-slot visuals, and the 3x3 toolbar block at `[0,96,48,48]`.
- `advanced_pattern_encoding_terminal_sprites.png` provides compact column controls, transpose, package tab art, and the small scrollbar.
- `pattern_mode_packaging.png` provides the package editor panel.
- `advanced_pattern_encoding_terminal.png` and its deterministic middle-row strip provide the combined terminal body.

The Advanced terminal's color mode does not need a new asset. It is a left-toolbar `IconButton` using the current-AE2 scheduling default/round-robin icons. Per-column color buttons still use the shared package color picker. Terminal item-only and fluid-only filters use the supplied bottom-middle and bottom-right sprites because current-AE2's type-filter presentation does not match this 1.20.1 backport.

The new toolbar block is mapped without scaling: anti-clog at `(0,96)/(0,112)`, synchronized output at `(16,96)/(16,112)`, pattern sync at `(32,96)/(32,112)`, input delay at `(0,128)`, item-only at `(16,128)`, and fluid-only at `(32,128)`. Toggle sprites are selected during every render so a state change is visible immediately. Its integrated atlas SHA-256 is `1E5A223CBBE07D14CE9A97389596E188C668B4A44F0011EA8AA64D9E99EC3EC6`.

The Package Assembler's former decorative color/marker region now hosts live controls without adding new PNGs: a 16-dye-plus-Fluix picker trigger and a non-consuming AE2 fake marker slot. The menu synchronizes effective color and marker display stacks for inserted patterns and active `pushPattern` jobs without replacing the persisted machine configuration. Mixed advanced-pattern colors are represented with the existing None cell. Its capacity slot tooltip is text plus synchronized limits, not a new status icon.

## Removed Unused or Duplicate Resources

The cleanup removed 17 runtime files that had no valid live use:

- 14 proposed standalone files under `textures/gui/icons/` (`auto_export`, `blocking_mode`, `capacity`, `color_select`, `marker`, three marker policies, `pack_once`, two filters, and three status icons);
- `textures/gui/logo.png`, a redundant GUI-scoped logo;
- `advanced_pattern_encoding_terminal_states.png`, a byte duplicate of `ae2-states.png`;
- `pattern_encoding_terminal.png`, a retired package-only terminal base no longer referenced by the combined screen.

The associated required-file, dimension, hash, contract, brief, report, and license claims were removed or redirected. No block, item, model, ScreenStyle, Java resource constant, or GuideME page refers to these deleted paths.

## Acceptance

The release asset audit must confirm:

```text
ae2-terminal.png exists, is 256x256 RGBA, and matches the fixed source hash
assets/ae2/textures/guis/text_field.png exists, is 128x128 RGBA, and matches the fixed source hash
the retained shared atlases and root logo are visible, non-placeholder resources
the deleted standalone icon directory, GUI logo, duplicate states atlas, and retired terminal base are not required or referenced
Sequence Buffer uses ModernScrollbarStyles.BIG at its existing centered geometry
Package Assembler uses its current small sprite at packageQueueScrollbar.left=12
Package Assembler input rows alone use left=21; color and marker controls align to their corrected user frames
Advanced terminal current search/pinned resources and shared ae2-states atlas remain wired
Advanced terminal paints the advanced panel after its corrected gray base
package-specific toolbar controls and terminal item/fluid filters use the user sprite block at atlas origin 0,96
Advanced terminal color mode follows native AE2 toolbar functions; mouse-click focus does not leave an extra border
all AE2-derived caches have source/license records
```

Verification results are recorded in `docs/development-log.md` after the asset audit, its negative-fixture suite, build, GameTest, and client resource-load path complete.

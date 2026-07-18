# Sequence Buffer Asset Report

## Source

The release textures come from the user-authored sheet:

```text
E:/resources/textures/appliedpackaging/ret/sequance_buffer_all.png
64x64 RGBA
SHA-256 66A26C07983D8E3CD1866B0D4EE723F2A68B1C257FCD936BCC0C3C57EECF7B8F
```

The sheet is a strict 4x4 grid of 16x16 cells. No scaling, recoloring, quantization, interpolation, or generated replacement pixels are allowed.

## Deterministic Split

Run:

```powershell
python .\scripts\split-sequence-buffer-textures.py
```

The script validates the exact 64x64 RGBA source, exports all 16 cells to one folder, reconstructs the original sheet pixel-for-pixel, and writes an ignored proof sheet and SHA-256 manifest under `build/asset-reference/sequence-buffer/user-sheet/`.

Runtime output folder:

```text
src/main/resources/assets/appliedpackaging/textures/block/sequence_buffer/faces/
```

Rows are `undirected`, `directed_front`, `directed_side`, and `directed_back`. Columns are `unformed`, `formed_middle_side`, `formed_edge_side`, and `special`. Special cells map to controller back, the occluded link face, controller side, and tail back respectively.

## Model Mapping

The models use every split texture:

- `unformed` and `unformed_directed` use the first-column faces;
- `endpoint` uses controller side/back plus the occluded inward face;
- non-tail members use the second-column middle faces;
- tail members use the third-column edge faces plus the special tail back;
- directed member models select matching front/side/back faces.

`tail` and `sequence_direction` are rendering properties in addition to the existing five-value visual state. Topology reconciliation moves `tail=true` whenever a new block joins the end of the line, so the old tail changes to the middle model and the new last block receives the tail back.

`scripts/generate-sequence-buffer-models.py` emits 57 explicit full-cube models and a 58-entry multipart blockstate. The matrix covers X/Y/Z structure axes, all six endpoint/tail directions, all six standalone facings, and every perpendicular formed-member facing. The source directed-side arrow follows texture `+U`; each generated face applies a per-face UV transform so `+U` points toward that block's own `facing`. Middle-segment texture `+V` follows the positive structure axis. Edge/tail texture `+V` points inward toward the controller because the source cell is capped at `-V` and open at `+V`; directed tail sides use a V-only mirror where opposite-face handedness would otherwise reverse that cap while preserving the arrow direction.

Structure formation never replaces the block's own direction. `sequence_direction` controls endpoint, connection, and tail geometry, while `directional + facing` remains the block-local wrench state. A pre-existing direction parallel to the structure axis is retained but hidden by the formed connection model and becomes visible again after detachment.

## Acceptance

`scripts/verify-assets.ps1` requires all 16 files, 16x16 RGBA dimensions, visible non-placeholder content, references from the actual Sequence Buffer models, all six tail directions, X/Y/Z axes, all six block facings, 57 generated models, the complete 58-entry orientation matrix, and the standard Minecraft 3D block-item display inheritance. `scripts/test-assets-audit.ps1` contains matching negative dimension, missing-state, missing-vertical-model, reversed-tail, and flat-inventory-item fixtures.

The split proof sheet was inspected at nearest-neighbor scale. All cell borders are aligned, the source is reconstructed byte-for-byte at the pixel level, and no cell is cropped or shifted. Direct exported-model renders under `build/asset-preview/sequence-buffer/` were also inspected for a north-facing standalone block and vertical directed middle/tail blocks; the renderer resolved the real JSON and PNG assets rather than a missing-model placeholder.

## GUI Assets

The endpoint main GUI is the user-edited `E:/resources/textures/appliedpackaging/ret/seq_buffer_ui.png`, copied byte-for-byte to `textures/gui/sequence_buffer.png` with SHA-256 `075E3329882A3AAE7FE7EBDAAB32EBF799531DC4224F3F37B563CD6B537A2C67`. The 256x256 atlas exposes a 195x170 high-version terminal area with the 3x9 storage slot frames intentionally removed. Runtime draws the existing Package Bus `SLOT_BACKGROUND` at `(7,18)` in an 18px grid; unavailable positions use the same sprite at 0.2 alpha.

The member/standalone side GUI uses the byte-preserved AE2 neoforge/v19.2.17 `me_chest.png`, copied to `textures/gui/sequence_buffer_side.png` with SHA-256 `2749D7BDAB5E3B9BFF240B6F618AB55AE14A3C2252D9DEB63D959874456D91A0`. Its visible 176x168 area keeps the single storage slot at `(80,37)`. The source is LGPL-3.0-or-later and is recorded in the project reference/license documentation.

Both ScreenStyle files deliberately omit only the deferred 3x3 input-filter panel. The main and side screens share the project's current-style AE2 vertical button bar for automatic output, blocking, synchronized output, pattern mode, and the `0/1/5/10/20/40/100 tick` input-delay presets, while the one-slot redstone-card upgrade panel remains at `{right:2,top:0}`. The main scrollbar uses AE2 `Scrollbar.DEFAULT`: its 12x15 active or disabled handle starts at `(175,18)`, symmetrically covering the background track at `x=178..183`; a zero scroll range still renders the disabled handle. The asset audit fixes both PNG hashes/dimensions, the absence of the rejected filter panel, and these main/side layout contracts.

On 2026-07-18, `scripts/verify-assets.ps1` and the complete `scripts/test-assets-audit.ps1` suite passed. The negative fixtures include a replaced user main atlas, invalid GUI dimensions, an incorrect main scrollbar rectangle, a missing setting action, and an incorrect side storage-slot origin.

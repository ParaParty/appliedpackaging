---
navigation:
  parent: index.md
  title: Advanced Pattern Terminal
  icon: appliedpackaging:advanced_pattern_encoding_terminal
  position: 20
item_ids:
- appliedpackaging:advanced_pattern_encoding_terminal
- appliedpackaging:package_pattern
- appliedpackaging:advanced_processing_pattern
categories:
- applied packaging devices
- applied packaging items
---

# Advanced Pattern Encoding Terminal

<GameScene zoom="8" background="transparent">
  <ImportStructure src="assets/blocks/advanced_pattern_encoding_terminal.snbt" />
  <IsometricCamera yaw="180" />
</GameScene>

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
  <ItemImage id="appliedpackaging:advanced_processing_pattern" scale="4" />
</Row>

This cable part has two isolated pages. The **Package** page encodes a package pattern with ordered inputs, color, marker,
and sparse positions. The **Advanced** page records up to 81 colored package columns with 81 sparse input positions per
column. The screen displays four neighboring columns at a time; this visible window is not the encoded column limit.

## Package Page

1. Insert a blank AE2 pattern in the blank-pattern slot.
2. Place or transfer the package contents into the input grid. Empty positions are meaningful and are recorded as sparse
   layout when the pattern is encoded.
3. Choose the package color and optional marker. The marker identifies the intended recipe/result but is not consumed.
4. Encode to create a <ItemLink id="appliedpackaging:package_pattern" />.

Repeated copies of the same resource in different positions remain separate entries. This matters when the receiving
machine has position-specific slots.

## Advanced Page

Every enabled advanced column has its own color and becomes one package. The page can encode up to four normal processing
outputs; the first, primary output becomes the shared marker of the generated packages. Package-column count and normal
output count are independent.

Use the horizontal column window and vertical row window to reach the full 81x81 sparse editor. This is useful when one
processing operation needs many ordered input groups that must later reach different sides or exact slots.

The column order is the package output order. Sparse row positions within each column are retained.

## Editing and Page Isolation

The two pages keep separate inventories. Inserting an encoded Applied Packaging pattern selects its matching page. The
ordinary AE2 Pattern Encoding Terminal intentionally does not edit these specialized patterns.

Switching pages never copies or clears the other page. Re-encoding updates the pattern currently in the encoded-pattern
slot. A damaged or unsupported pattern is rejected rather than being interpreted as a different pattern type.

## Recipe Viewer Transfer

Recipe-viewer transfer supports deterministic JEI/EMI recipes. Optional Create and GTCEu integrations preserve known
ordered item/fluid inputs and reject ambiguous or random-output recipes instead of silently encoding the wrong plan.

Like AE2's <ItemLink id="ae2:pattern_encoding_terminal" />, this is a cable subpart and requires a powered channel.

## Recipe

<RecipeFor id="appliedpackaging:advanced_pattern_encoding_terminal" />

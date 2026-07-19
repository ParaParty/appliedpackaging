---
navigation:
  parent: devices/index.md
  title: Advanced Pattern Encoding Terminal
  icon: appliedpackaging:advanced_pattern_encoding_terminal
  position: 10
item_ids:
- appliedpackaging:advanced_pattern_encoding_terminal
- appliedpackaging:package_pattern
- appliedpackaging:advanced_processing_pattern
categories:
- applied packaging devices
- applied packaging items
---

# The Advanced Pattern Encoding Terminal

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/advanced_pattern_encoding_terminal.snbt" />
  <IsometricCamera yaw="180" />
</GameScene>

The Advanced Pattern Encoding Terminal encodes package patterns and advanced processing patterns. It has two independent pages: **Package** and **Advanced**. Each page keeps its own inventory — switching pages does not copy or clear items between them.

This is a cable subpart and requires a channel. Regular AE2 pattern encoding terminals cannot read or edit packaging patterns.

## The Package Page

Encodes one <ItemLink id="appliedpackaging:package_pattern" /> at a time:

1. Insert a blank AE2 pattern into the blank-pattern slot.
2. Place items into the input grid. Empty slots are recorded as part of the layout.
3. Choose a color from the color picker.
4. Optionally place an item in the marker slot. The marker is never consumed.
5. Click Encode.

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
</Row>

The same item placed in different slots produces separate entries. This matters when the pattern encodes, for example, coal in both slot 1 and slot 3 — the two coals remain independent entries that can be routed to different destinations by a Sequence Buffer.

## The Advanced Page

Encodes an <ItemLink id="appliedpackaging:advanced_processing_pattern" /> with up to 81 package columns, each with its own color and up to 81 sparse input slots. The screen displays four columns at a time.

Each column becomes one package. The column order is the package output order. This is useful for recipes that produce multiple packages from one pattern, where each package carries a different set of ingredients for a different destination.

### Color Mode

The left toolbar controls how new columns are colored:

*   **Default** assigns Fluix to every new column.
*   **Cycling** assigns the next unused color in sequence, cycling through all 17 colors.

Changing the mode does not affect existing columns — only columns added afterwards.

### Recipe Viewer Transfer

Recipes can be imported from JEI or EMI into either page. Deterministic recipes are supported; ambiguous or random-output recipes are rejected.

## Recipe

<RecipeFor id="appliedpackaging:advanced_pattern_encoding_terminal" />

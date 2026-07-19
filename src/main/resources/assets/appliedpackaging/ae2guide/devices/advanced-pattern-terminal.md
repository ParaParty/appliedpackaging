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

The Advanced Pattern Encoding Terminal encodes package patterns and advanced processing patterns. It has two independent pages: Package and Advanced. Each page keeps its own inventory — switching pages does not copy or clear items between them.

This is a cable subpart and requires a channel. Regular AE2 pattern encoding terminals cannot read or edit packaging patterns.

## The UI

The terminal has two pages, switched via tabs at the top. A slot on the right side accepts blank patterns. An arrow button encodes the pattern. A slot holds the encoded pattern — place an already-encoded pattern in this slot to edit it, then click the encode arrow.

<a name="package-page"></a>

### Package Page

The Package page encodes one package pattern at a time.

*   Left-click or drag from JEI/REI the items to form the package contents. Right-click to remove an item.
*   Empty slots are recorded as part of the layout.
*   Choose a color from the color picker and optionally place an item in the marker slot. The marker is never consumed.
*   You can also directly encode a pattern from the JEI/REI recipe screen.

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
</Row>

The same item placed in different slots produces separate entries. When a pattern encodes coal in both slot 1 and slot 3, the two coal entries remain independent.

<a name="advanced-page"></a>

### Advanced Page

The Advanced page encodes an advanced processing pattern with up to 81 package columns, each with its own color and up to 81 sparse input slots. The screen displays four columns at a time.

Each column becomes one package. The column order is the package output order.

The color mode button in the left toolbar controls how new columns are colored. **Default** assigns Fluix to every new column. **Cycling** assigns the next unused color in sequence, cycling through all 17 colors. Changing the mode does not affect existing columns.

You can also directly encode a pattern from the JEI/REI recipe screen.

## Recipe

<RecipeFor id="appliedpackaging:advanced_pattern_encoding_terminal" />

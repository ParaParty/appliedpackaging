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

You know AE2's <ItemLink id="ae2:pattern_encoding_terminal" />? This is that, but for packages.

It has two independent pages: **Package** and **Advanced**. Each page keeps its own inventory — switching between them won't mix or copy anything. You'll spend most of your time on the Package page.

This is a [cable subpart](ae2:ae2-mechanics/cable-subparts.md) and needs a [channel](ae2:ae2-mechanics/channels.md), same as AE2's regular pattern terminal.

**Regular AE2 pattern terminals cannot read or edit packaging patterns.** You need this one.

## Package Page

This is where you make one package pattern at a time.

1. Put a blank AE2 pattern in the blank-pattern slot on the right.
2. Place items in the input grid. **Empty slots count.** If you put coal in slot 1, nothing in slot 2, and iron in slot 3, the package remembers slot 2 was intentionally left empty. This is called a sparse layout.
3. Pick a color from the color picker.
4. Optionally, put an item in the Marker slot. The marker is just a label — it's never consumed. You might mark a furnace-input package with an iron ingot.
5. Hit **Encode**. You get a <ItemLink id="appliedpackaging:package_pattern" />.

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
</Row>

**The same item in different slots stays as separate entries.** Put coal in slot 1 and coal in slot 3, and you get two independent coal entries — not one merged stack. This matters when the receiving machine has multiple slots that all accept coal.

## Advanced Page

This is for when you need more than one package from a recipe — up to 81 of them, each with its own color and inputs.

The screen shows four columns at a time. Each column becomes one package. The column order is the order the packages are produced in. Use the scroll bars to reach the full 81×81 grid.

This is useful when one processing operation needs multiple ordered input groups that each go to a different machine face. For example: column 1 for the furnace's fuel side, column 2 for the ore top, column 3 for the output extraction. Each column becomes its own colored package.

### Color Mode

The first button in the left toolbar controls what color new columns get:

* **Default** — Every new column starts as Fluix. Boring but predictable.
* **Cycling** — New columns get the next unused color, cycling through all 17. Great for keeping different columns visually distinct.

**Changing the color mode never touches existing columns.** It only affects columns you add after switching. To change a specific column's color, click its individual color button.

### Recipe Viewer Transfer

You can import recipes directly from JEI or EMI into either page. Deterministic recipes work fine. Random-output or ambiguous recipes are rejected — you'll get a clear error message explaining why.

## Recipe

<RecipeFor id="appliedpackaging:advanced_pattern_encoding_terminal" />

---
navigation:
  parent: packaging-concepts/index.md
  title: Packages
  icon: appliedpackaging:fluix_package
  position: 10
item_ids:
- appliedpackaging:fluix_package
- appliedpackaging:white_package
- appliedpackaging:orange_package
- appliedpackaging:magenta_package
- appliedpackaging:light_blue_package
- appliedpackaging:yellow_package
- appliedpackaging:lime_package
- appliedpackaging:pink_package
- appliedpackaging:gray_package
- appliedpackaging:light_gray_package
- appliedpackaging:cyan_package
- appliedpackaging:purple_package
- appliedpackaging:blue_package
- appliedpackaging:brown_package
- appliedpackaging:green_package
- appliedpackaging:red_package
- appliedpackaging:black_package
categories:
- applied packaging items
---

# Packages

<Row gap="8">
  <ItemImage id="appliedpackaging:fluix_package" scale="3" />
  <ItemImage id="appliedpackaging:red_package" scale="3" />
  <ItemImage id="appliedpackaging:green_package" scale="3" />
  <ItemImage id="appliedpackaging:blue_package" scale="3" />
  <ItemImage id="appliedpackaging:black_package" scale="3" />
</Row>

A package is exactly what it sounds like — a box you put items in. But unlike a regular chest, the box remembers the exact order you put things in. Coal in slot 1, iron in slot 2 — that order never changes, no matter where the package goes.

## What Goes In a Package?

Every package stores four things:

* **Contents** — An ordered list of items. Two coal followed by one iron is different from one iron followed by two coal. The order is always preserved.
* **Color** — One of 17 colors. Purely for organization — use different colors for different production lines, or to filter which packages go where.
* **Marker** — An optional label item. Put an iron ingot in the marker slot when encoding, and the package says "Marker: Iron Ingot." The marker item is never consumed — it's just a tag.
* **Layout** — If you left empty slots between items when encoding, the package remembers those gaps. This matters when delivering to machines with specific slot positions.

**Packages cannot hold other packages.** If you try, the inner package is unpacked first and its contents are included directly.

## Capacity

By default, each package holds up to **9 types of items** and **9 total items**. You can increase this by installing AE2 storage components in the machine that creates the package:

| Component | Max Types | Max Total |
|-----------|-----------|-----------|
| None | 9 | 9 |
| <ItemLink id="ae2:cell_component_16k" /> | 16 | 16 |
| <ItemLink id="ae2:cell_component_64k" /> | 63 | 64 |
| <ItemLink id="ae2:cell_component_256k" /> | 63 | 256 |

**Completed storage cells and 1k components won't work.** Only 16k, 64k, and 256k raw components are accepted.

## Making Packages

Three ways:

| Method | Device | When to use it |
|--------|--------|----------------|
| Autocrafting | <ItemLink id="appliedpackaging:package_assembler" /> + pattern provider | The standard way — integrates into AE2 autocrafting |
| Direct packing | <ItemLink id="appliedpackaging:me_packager" /> | When you want to pack items from ME storage without patterns |
| Local assembly | <ItemLink id="appliedpackaging:package_assembler" /> with local pattern | Manual or single-batch use |

## Unpacking Packages

* **Sneak-right-click** a package stack in your hand to unpack one into your inventory. Quickest way to test things.
* Use a [Package Unpacking Bus](packaging-concepts/../devices/package-unpacking-bus.md) to automatically unpack packages into machines.
* The [ME Packager](packaging-concepts/../machines/me-packager.md) can also unpack back into ME storage.

## Colors

<Row gap="8">
  <ItemImage id="appliedpackaging:fluix_package" scale="2" />
  <ItemImage id="appliedpackaging:white_package" scale="2" />
  <ItemImage id="appliedpackaging:orange_package" scale="2" />
  <ItemImage id="appliedpackaging:magenta_package" scale="2" />
  <ItemImage id="appliedpackaging:light_blue_package" scale="2" />
  <ItemImage id="appliedpackaging:yellow_package" scale="2" />
  <ItemImage id="appliedpackaging:lime_package" scale="2" />
  <ItemImage id="appliedpackaging:pink_package" scale="2" />
  <ItemImage id="appliedpackaging:gray_package" scale="2" />
</Row>
<Row gap="8">
  <ItemImage id="appliedpackaging:light_gray_package" scale="2" />
  <ItemImage id="appliedpackaging:cyan_package" scale="2" />
  <ItemImage id="appliedpackaging:purple_package" scale="2" />
  <ItemImage id="appliedpackaging:blue_package" scale="2" />
  <ItemImage id="appliedpackaging:brown_package" scale="2" />
  <ItemImage id="appliedpackaging:green_package" scale="2" />
  <ItemImage id="appliedpackaging:red_package" scale="2" />
  <ItemImage id="appliedpackaging:black_package" scale="2" />
</Row>

17 colors: Fluix (the default) plus all 16 dye colors. Two packages with identical contents but different colors **won't stack.** This is actually useful — you can use color to control which storage bus a package goes to.

## Pattern Types

You encode packages from patterns. Two types:

* **<ItemLink id="appliedpackaging:package_pattern" />** — One package per pattern. One color, one marker, one set of inputs. This is your daily driver.
* **<ItemLink id="appliedpackaging:advanced_processing_pattern" />** — Up to 81 packages from a single pattern, each with its own color. For complex multi-output recipes.

Both are made in the [Advanced Pattern Encoding Terminal](packaging-concepts/../devices/advanced-pattern-terminal.md). Regular AE2 pattern terminals can't read or edit them.

## Tips

* **Color-code your production lines.** Red packages = smelting, blue = crafting. It makes storage bus filters instantly readable.
* **Shift-right-click** unpacks a package anywhere. Essential for testing.
* **Packages are items, not storage cells.** They go through your ME network just like cobblestone or iron — store them in drives, export them, route them.
* **A package can't contain another package.** If you try, the inner one is flattened first.

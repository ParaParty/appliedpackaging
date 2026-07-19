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

Packages are made in a [Package Assembler](../machines/package-assembler.md) or [ME Packager](../machines/me-packager.md), from patterns encoded in an [Advanced Pattern Encoding Terminal](../devices/advanced-pattern-terminal.md). They group multiple items into a single stackable item that travels through your ME network, carrying a color, an optional marker, and ordered contents.

A package cannot contain another package. If you use a package as an ingredient, its contents are expanded in place before the outer package is assembled. Sneak-right-click a package stack to unpack one into your inventory.

There are two types of patterns for different purposes:

*   <ItemLink id="appliedpackaging:package_pattern" /> encodes a single package with one color, one marker, and one input layout. This is the most common type — use it when you need to group one set of ingredients for routing to a single destination.

*   <ItemLink id="appliedpackaging:advanced_processing_pattern" /> encodes up to 81 packages from a single pattern, where each column has its own color and inputs. The assembler outputs the packages in column order. This is useful when a recipe needs its ingredients split into multiple groups that each go to a different destination through color-filtered buses.

Both pattern types are encoded in the [Advanced Pattern Encoding Terminal](../devices/advanced-pattern-terminal.md). Regular AE2 pattern encoding terminals cannot read or edit packaging patterns.

## Capacity

By default each package holds up to 9 item types and 9 total items. AE2 storage components installed in the machine that creates the package increase these limits:

| Component | Max Types | Max Total |
|-----------|-----------|-----------|
| None | 9 | 9 |
| <ItemLink id="ae2:cell_component_16k" /> | 16 | 16 |
| <ItemLink id="ae2:cell_component_64k" /> | 63 | 64 |
| <ItemLink id="ae2:cell_component_256k" /> | 63 | 256 |

Storage cells and 1k components are not accepted. Only 16k, 64k, and 256k raw components work.

## Contents, Color, and Marker

Each package stores an ordered list of its contents, a color from a set of 17, and an optional marker item. The color and marker are used for filtering on Package Storage Buses and Package Unpacking Buses. Empty slots between items in the encoded pattern are preserved as part of the package layout.

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

17 colors are available: Fluix (the default) plus all 16 dye colors. Packages with identical contents but different colors will not stack in your inventory. Colors are used for filtering on Package Storage Buses and Package Unpacking Buses — a red package can go to one destination, a blue package to another.

## Unpacking

*   Sneak-right-click a package stack to unpack one into your inventory.
*   A [Package Unpacking Bus](../devices/package-unpacking-bus.md) unpacks a package into an adjacent inventory in the encoded order.
*   The [ME Packager](../machines/me-packager.md) can unpack packages back into ME storage directly.

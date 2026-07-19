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

Packages group multiple items together into a single item that travels through your ME network. This lets you route a complete set of ingredients as one unit — for example, through a subnetwork that distributes them to multiple machines, or to split one recipe's ingredients into separate groups that each go to a different destination.

A package cannot contain another package. If you use a package as an ingredient, its contents are expanded in place before the outer package is assembled.

Sneak-right-click a package stack to unpack one into your inventory.

## Why Use Packages

### Group Routing

A normal AE2 pattern provider can push ingredients to one adjacent inventory. With packages, you encode the ingredients as a single package, route that package through a subnetwork, and unpack it at the destination. This allows one pattern to feed multiple machines: the package travels as one item through the network, and an Unpacking Bus with a Sequence Buffer splits its contents across machine faces.

### Splitting a Recipe

Some recipes need different ingredients delivered to different locations. You can encode two or more package patterns, each carrying a subset of the recipe's ingredients, and use a parent processing pattern whose inputs are those packages. Alternatively, use an advanced processing pattern with multiple columns — each column becomes one package. When routed through color-filtered Unpacking Buses, each group goes to its correct destination.

## Contents, Color, and Marker

Each package stores an ordered list of contents, a color (one of 17), and an optional marker item. The color and marker are used for filtering on Package Storage Buses and Package Unpacking Buses. Colors allow you to route different groups of ingredients to different destinations.

Empty slots between items in the encoded pattern are preserved as part of the package layout. Combined with a Sequence Buffer in pattern mode, this allows sparse slot positions to be mapped to specific buffer members.

## Capacity

By default each package holds up to 9 item types and 9 total items. AE2 storage components installed in the machine creating the package increase these limits:

| Component | Max Types | Max Total |
|-----------|-----------|-----------|
| None | 9 | 9 |
| <ItemLink id="ae2:cell_component_16k" /> | 16 | 16 |
| <ItemLink id="ae2:cell_component_64k" /> | 63 | 64 |
| <ItemLink id="ae2:cell_component_256k" /> | 63 | 256 |

Storage cells and 1k components are not accepted. Only 16k, 64k, and 256k raw components work.

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

17 colors are available: Fluix (default) plus the 16 dye colors. Packages with identical contents but different colors will not stack. Colors are used for filtering on buses — different colors can route different groups to different destinations.

## Pattern Types

*   <ItemLink id="appliedpackaging:package_pattern" /> encodes one package with a single color, marker, and input layout.
*   <ItemLink id="appliedpackaging:advanced_processing_pattern" /> encodes up to 81 packages from one pattern, each column with its own color and inputs.

Both are encoded in the [Advanced Pattern Encoding Terminal](packaging-concepts/../devices/advanced-pattern-terminal.md). Regular AE2 pattern terminals cannot read or edit them.

## Unpacking

*   Sneak-right-click a package stack to unpack one into your inventory.
*   A [Package Unpacking Bus](packaging-concepts/../devices/package-unpacking-bus.md) unpacks into an adjacent inventory in encoded order.
*   A [Sequence Buffer](packaging-concepts/../machines/sequence-buffer.md) preserves the order of package entries and pattern provider pushes, enabling sparse slot mapping to specific faces.
*   The [ME Packager](packaging-concepts/../machines/me-packager.md) unpacks back into ME storage.

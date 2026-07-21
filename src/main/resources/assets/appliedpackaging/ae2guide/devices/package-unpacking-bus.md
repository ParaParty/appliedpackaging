---
navigation:
  parent: devices/index.md
  title: Package Unpacking Bus
  icon: appliedpackaging:package_unpacking_bus
  position: 30
item_ids:
- appliedpackaging:package_unpacking_bus
categories:
- applied packaging devices
---

# The Package Unpacking Bus

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/package_unpacking_bus.snbt" />
</GameScene>

The Package Unpacking Bus receives packages from network storage and inserts their contents into the compatible storage target it is touching, in the order they were encoded. It supports the same extensible external-storage targets as an AE2 Pattern Provider, including items, fluids, and additional AE key types registered by addons.

The bus also functions as a sequenced supplier, similar to how a pattern provider pushes a complete batch of ingredients to an adjacent target. It checks every content type before starting the push and preserves the encoded order during delivery.

They are cable subparts.

## Filtering

By default the bus will accept any package. Items inserted into its filter rows will act as a whitelist, only allowing packages matching those criteria to be unpacked.

Each filter row can combine a color, a marker, and up to six content items. A row with no color selected does not filter by color. A row with no marker selected matches any marker.

Items can be dragged into the filter slots from JEI/REI even if you do not actually have any of that item.

## Priority

Priorities can be set by clicking the wrench in the top-right of the GUI. Packages entering the network will start at the highest priority destination. At equal priority, an available Package Unpacking Bus is preferred over a Package Storage Bus.

## Settings

*   The bus can be set to check whether the complete contents can be inserted before accepting a package (labeled "Anti-Clog Mode" in the GUI, on by default). When enabled, the bus rejects the package if the check fails, allowing the network to route it to another destination. When disabled, the bus accepts the package and holds it in the working slot until the target is ready.
*   Blocking mode can be enabled to prevent unpacking while the target contains anything. The target must be completely empty; unrelated contents also block.
*   The package in the working slot is a real item. It can be extracted from the GUI at any time. Breaking the bus returns it.

Package contents are never reported to network storage until the unpack commits successfully.

## Upgrades

The Package Unpacking Bus supports the following upgrades:

*   <ItemLink id="ae2:speed_card" /> shortens the unpack work cycle
*   <ItemLink id="ae2:capacity_card" /> increases the amount of filter rows
*   <ItemLink id="ae2:fuzzy_card" /> enables fuzzy matching per row
*   <ItemLink id="ae2:inverter_card" /> switches the filter from a whitelist to a blacklist

## Recipe

<RecipeFor id="appliedpackaging:package_unpacking_bus" />

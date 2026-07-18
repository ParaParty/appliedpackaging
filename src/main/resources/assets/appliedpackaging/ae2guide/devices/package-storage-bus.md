---
navigation:
  parent: devices/index.md
  title: Package Storage Bus
  icon: appliedpackaging:package_storage_bus
  position: 20
item_ids:
- appliedpackaging:package_storage_bus
categories:
- applied packaging devices
---

# The Package Storage Bus

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/package_storage_bus.snbt" />
</GameScene>

Ever wanted to keep your packages in a chest instead of cluttering up your ME drives? That's what this does.

The Package Storage Bus works exactly like AE2's <ItemLink id="ae2:storage_bus" /> — same priority system, same routing behavior, same [channel](ae2:ae2-mechanics/channels.md) requirement — but for packages. It exposes the packages in the attached inventory to your ME network **as packages**, not as their individual contents. Your network sees "3 red furnace-input packages," not "3 coal and 3 iron."

Loose items in the same chest are completely ignored.

## Filtering by Package Properties

Instead of filtering by item type like a normal storage bus, you filter by what's *in* the package:

* **Color** — Only accept packages of a specific color. Leave empty to not filter by color.
* **Marker** — Only accept packages with a specific marker item. Leave empty to match any marker.
* **Content filters** — Up to 6 per row. Only accept packages containing specific items.

Rows work as OR conditions — a package matching **any** enabled row is accepted.

You start with 2 filter rows. Each <ItemLink id="ae2:capacity_card" /> adds one, up to 7 rows with 5 capacity cards.

## Partitioning

Click the partition button and the bus scans the adjacent inventory. For each distinct package it finds, it creates one complete filter row with color, marker, and content filters all filled in. If the inventory has no packages, partitioning clears everything.

## Priority

Same as AE2's storage bus: higher priority fills first. At equal priority, AE2 prefers storage that already contains matching packages.

**A common routing trick:** give <ItemLink id="appliedpackaging:package_unpacking_bus" /> a higher priority than this bus if you want packages unpacked on arrival. Give this bus higher priority if you want packages stored for later.

## Upgrades

* <ItemLink id="ae2:capacity_card" /> — More filter rows (up to 5)
* <ItemLink id="ae2:fuzzy_card" /> — Fuzzy matching per row
* <ItemLink id="ae2:inverter_card" /> — Invert content filtering per row

No speed cards — this bus doesn't do any timed operations. It's a filter, not a processor.

## Recipe

<RecipeFor id="appliedpackaging:package_storage_bus" />

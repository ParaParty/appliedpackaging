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

The Package Storage Bus works like AE2's <ItemLink id="ae2:storage_bus" /> but only sees package items in the adjacent inventory. Loose items like cobblestone or iron are ignored. The network can't see what's inside a package — it sees "3 red packages," not "3 coal and 3 iron."

Unlike a regular storage bus, this one can filter by what a package looks like: its color, its marker, and up to six items from its contents. Red packages go here, blue packages go there.

They are [cable subparts](ae2:ae2-mechanics/cable-subparts.md).

## Filter Rows

Two filter rows are available initially. Each <ItemLink id="ae2:capacity_card" /> adds one row, reaching seven rows with five cards. Rows are alternatives: a package matching any enabled row is accepted.

Each row can combine:

*   A color filter. Leave empty to not filter by color.
*   A marker filter. Leave empty to match any marker.
*   Up to six content filters.

## Partitioning

Clicking the partition button scans the adjacent inventory and creates one filter row per distinct package found. Loose items are ignored. If the inventory contains no packages, partitioning clears all filters.

## Priority

Priorities can be set by clicking the wrench in the top-right of the GUI. Higher priority storage receives packages first. At equal priority, existing matching storage is preferred.

You can also use the bus on a [subnetwork](ae2:ae2-mechanics/subnetworks.md). Make it the only storage on the subnetwork, and the bus's filters control exactly which packages enter that network. This is how you split packages across different destinations based on their color or marker.

## Upgrades

The Package Storage Bus supports the following [upgrades](ae2:items-blocks-machines/upgrade_cards.md):

*   <ItemLink id="ae2:capacity_card" /> increases the number of filter rows
*   <ItemLink id="ae2:fuzzy_card" /> enables fuzzy matching per row
*   <ItemLink id="ae2:inverter_card" /> inverts content filtering per row

Speed cards are not accepted — the bus does not run any processing, it just stores and filters.

## Recipe

<RecipeFor id="appliedpackaging:package_storage_bus" />

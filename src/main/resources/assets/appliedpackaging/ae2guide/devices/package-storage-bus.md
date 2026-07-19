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

The Package Storage Bus turns the inventory it is touching into network storage for packages. It allows the network to see the packages in that inventory, and to push to and pull from that inventory in order to fulfill devices pushing to and pulling from network storage.

Due to AE2's philosophy of emergent mechanics through interaction of the functions of the devices, you do not necessarily have to use a Package Storage Bus for storage. By using subnetworks to make it the only storage on a network, you can use it as a source or destination for package routing.

They are cable subparts.

## Filtering

By default the bus will store every package it finds. Items inserted into its filter rows will act as a whitelist, only allowing packages matching those criteria to be stored.

Each filter row can combine a color, a marker, and up to six content items. A row with no color selected does not filter by color. A row with no marker selected matches any marker.

Items can be dragged into the filter slots from JEI/REI even if you do not actually have any of that item.

## Priority

Priorities can be set by clicking the wrench in the top-right of the GUI. Packages entering the network will start at the highest priority storage as their first destination. In the case of two storages having the same priority, if one already contains matching packages, it will be preferred. Any filtered storages will be treated as already containing the package when in the same priority group as other storages.

## Partitioning

The bus can be partitioned to what is currently in the adjacent inventory. Clicking the partition button scans the inventory and creates one filter row per distinct package found. Loose items are ignored. If the inventory contains no packages, partitioning clears all filters.

## Upgrades

The Package Storage Bus supports the following upgrades:

*   <ItemLink id="ae2:capacity_card" /> increases the amount of filter rows
*   <ItemLink id="ae2:fuzzy_card" /> enables fuzzy matching per row
*   <ItemLink id="ae2:inverter_card" /> switches the filter from a whitelist to a blacklist

## Recipe

<RecipeFor id="appliedpackaging:package_storage_bus" />

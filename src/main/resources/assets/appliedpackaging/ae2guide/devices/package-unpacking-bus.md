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

The Package Unpacking Bus takes packages from the network and puts their contents into the inventory it's touching — in the order they were packed. The network sends a package to the bus, the bus opens it, and each item goes into the inventory in sequence.

They are [cable subparts](ae2:ae2-mechanics/cable-subparts.md).

The bus only inserts items when the full package can go in. If the inventory is full or can't take everything, the bus waits and tries again. It never inserts half a package.

## Filter Rows

Two filter rows are available initially. Each <ItemLink id="ae2:capacity_card" /> adds one row, reaching seven rows with five cards. Rows are alternatives: a package matching any enabled row is accepted.

Each row can combine:

*   A color filter. Leave empty to not filter by color.
*   A marker filter. Leave empty to match any marker.
*   Up to six content filters.

## Pre-Admission Check

The bus has an option that decides when it checks whether the target can take the items. The GUI labels it "Anti-Clog Mode." It is on by default:

*   **On:** The bus checks the target inventory before taking the package out of the network. If the items won't fit, the package stays in the network and can go to a different destination.
*   **Off:** The bus takes the package and holds it in its working slot. If the target isn't ready yet, the package sits there until it is. You can pull it out of the GUI at any time.

If blocking mode is also on, the check includes the blocking rule: the bus won't take a package if the target already has any of the same items.

## Held Package

The package in the working slot is a real item. You can take it out of the GUI at any time, even while the bus is working. Breaking the bus also returns it. The items inside a package never show up in ME storage until the unpack finishes.

## Using a Subnetwork

If you put the bus on a subnetwork with a Sequence Buffer, one package can deliver different items to different machine faces. Each buffer member gets one entry from the package and outputs it through its own face. For example, a furnace can get coal through the side and ore through the top — all from one package.

You can also use filter rows to accept different types of packages and send each type to a different buffer chain. Configure one row for red packages, another for blue, and each color takes a different path through the subnetwork.

## Upgrades

The Package Unpacking Bus supports the following [upgrades](ae2:items-blocks-machines/upgrade_cards.md):

*   <ItemLink id="ae2:speed_card" /> shortens the unpack work cycle (up to 4)
*   <ItemLink id="ae2:capacity_card" /> increases the number of filter rows (up to 5, for 7 total rows)
*   <ItemLink id="ae2:fuzzy_card" /> enables fuzzy matching per row
*   <ItemLink id="ae2:inverter_card" /> inverts content filtering per row

## Recipe

<RecipeFor id="appliedpackaging:package_unpacking_bus" />

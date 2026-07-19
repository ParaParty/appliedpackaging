---
navigation:
  parent: machines/index.md
  title: ME Packager
  icon: appliedpackaging:me_packager
  position: 20
item_ids:
- appliedpackaging:me_packager
categories:
- applied packaging machines
---

# The ME Packager

<BlockImage id="appliedpackaging:me_packager" scale="8" />

The ME Packager pulls items from ME storage and packs them into a package — no pattern needed. It can also unpack packages back into ME storage. You set the color, marker, and content filters in the GUI to control what gets packed.

The packager connects to AE2 through its bottom and its model back face. The belt on the front holds exactly one package. Right-click the belt to insert or retrieve the held package; right-click any other face to open the configuration GUI. The belt slot and the GUI slot are the same storage.

## Packing

Configure a color, a marker, content filters, and an activation mode. When packing triggers, the packager scans ME storage for items matching the filters and creates one package. The items are extracted from the network, not duplicated.

Content filters support two rows initially. Each <ItemLink id="ae2:capacity_card" /> adds one row (up to 3 cards, for 5 total rows). An <ItemLink id="ae2:inverter_card" /> inverts content matching.

## Activation Modes

*   **Pack with redstone signal** — packs while receiving redstone power.
*   **Pack without redstone signal** — packs while not receiving redstone power.
*   **Always pack** — packs continuously.
*   **Pack once on redstone pulse** — performs one operation per rising edge.
*   **Packing off** — only packs when the manual pack button is clicked.

## Unpacking

Put a package into the packager and its contents go into ME storage. Blocking mode stops unpacking if the network already has items that match the package contents. The pre-admission check (labeled "Anti-Clog Mode" in the GUI, on by default) checks that everything fits before taking the package.

## Filter Modes

*   **Filter packing and unpacking** — content filters apply to both directions.
*   **Filter packing only** — content filters apply only during packing; any package can be unpacked.
*   **Filter unpacking only** — content filters apply only during unpacking; any items can be packed.

## Using with Subnetworks

You can put the packager on a subnetwork with an Unpacking Bus and Sequence Buffer. The packager unpacks into the network, the bus sends items to the buffer, and the buffer splits them across machine faces. With advanced processing patterns, each column of the pattern becomes a package for a different destination — the buffer delivers each column's items to the right face. This setup works well with machines like Create's mechanical crafters that need specific items in specific input slots.

## Upgrades

The ME Packager supports the following [upgrades](ae2:items-blocks-machines/upgrade_cards.md):

*   <ItemLink id="ae2:speed_card" /> shortens packing/unpacking work (up to 6)
*   <ItemLink id="ae2:capacity_card" /> adds one filter row (up to 3, for 5 total rows)
*   <ItemLink id="ae2:inverter_card" /> inverts content filtering

The packager also has a storage component slot accepting 16k, 64k, or 256k components for package capacity.

## Recipe

<RecipeFor id="appliedpackaging:me_packager" />

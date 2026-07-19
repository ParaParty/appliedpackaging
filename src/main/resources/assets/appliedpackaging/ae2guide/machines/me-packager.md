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

The ME Packager pulls items from [network storage](ae2:ae2-mechanics/import-export-storage.md) and packs them into a package. It can also take a package and unpack its contents back into network storage.

The packager connects to AE2 through its bottom face and its model back face. The belt on the front holds the current package. Right-click the belt surface to insert or retrieve the held package; right-click any other face to open the configuration GUI. The belt slot and the GUI display the same storage.

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/me_packager_network.snbt" />
  <IsometricCamera yaw="195" pitch="30" />

  <BoxAnnotation color="#dddddd" min="0 0 0" max="2 1 1">
    (1) Chest and AE2 Storage Bus: Expose loose items as network storage.
  </BoxAnnotation>
  <BoxAnnotation color="#66aaff" min="1 0 0" max="4 1 1">
    (2) Smart Cable: Connects the storage, packager, and power.
  </BoxAnnotation>
  <BoxAnnotation color="#cc88ff" min="3 1 0" max="4 2 1">
    (3) ME Packager: Connected through its bottom face.
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="4 0 0" max="5 1 1">
    (4) Energy Cell: Powers the example network.
  </BoxAnnotation>
</GameScene>

## Packing

Configure a color, an optional marker, content filters, and an activation mode. When the packager triggers, it scans ME storage for items matching the filters and assembles one package. Items are extracted from the network, not duplicated.

Content filters start with two rows. Each <ItemLink id="ae2:capacity_card" /> adds one row, up to three cards for five total rows. An <ItemLink id="ae2:inverter_card" /> inverts content matching.

## Activation Modes

The packager can be triggered by redstone or configured to run continuously:

*   **Pack with redstone signal** — packs while receiving redstone power.
*   **Pack without redstone signal** — packs while not receiving redstone power.
*   **Always pack** — packs continuously.
*   **Pack once on redstone pulse** — performs one operation per rising edge.
*   **Packing off** — only packs when the manual pack button is clicked in the GUI.

## Unpacking

Place a package into the packager through the belt or the GUI to unpack its contents back into ME storage. Blocking mode prevents unpacking while the network already contains items matching the package contents. The pre-admission check, labeled "Anti-Clog Mode" in the GUI and on by default, verifies that the full contents will fit in ME storage before accepting the package.

## Filter Modes

The packager can be set to apply content filters to packing only, unpacking only, or both directions:

*   **Filter packing and unpacking** — filters apply in both directions.
*   **Filter packing only** — filters only restrict what gets packed.
*   **Filter unpacking only** — filters only restrict what gets unpacked.

## Upgrades

The ME Packager supports the following [upgrades](ae2:items-blocks-machines/upgrade_cards.md):

*   <ItemLink id="ae2:speed_card" /> shortens the packing and unpacking work cycle (up to 6)
*   <ItemLink id="ae2:capacity_card" /> adds one content filter row (up to 3, for 5 total rows)
*   <ItemLink id="ae2:inverter_card" /> inverts content filtering

The packager also has a storage component slot that accepts 16k, 64k, and 256k raw components to increase the capacity of the packages it produces.

## Recipe

<RecipeFor id="appliedpackaging:me_packager" />

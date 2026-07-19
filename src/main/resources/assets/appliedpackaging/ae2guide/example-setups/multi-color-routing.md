---
navigation:
  parent: example-setups/index.md
  title: Multi-Color Routing
  icon: appliedpackaging:red_package
  position: 30
---

# Multi-Color Routing

When you have multiple production lines, package colors can be used to route packages to different destinations. The Package Storage Bus and Package Unpacking Bus both accept color filters, so a red package can go to one destination and a blue package to another.

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/package_routing.snbt" />
  <IsometricCamera yaw="195" pitch="25" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="2 1 2">
    (1) Package Storage Bus: Filtered to red packages. Stores them in the left chest.
  </BoxAnnotation>
  <BoxAnnotation color="#ffbb55" min="3 0 1" max="5 1 2">
    (2) Package Unpacking Bus: Filtered to blue packages. Unpacks them into the right target.
  </BoxAnnotation>
  <BoxAnnotation color="#dddddd" min="2 0 1" max="3 2 2">
    (3) ME Drive: Fallback storage for packages that match neither filter.
  </BoxAnnotation>
</GameScene>

## Configurations

*   The <ItemLink id="appliedpackaging:package_storage_bus" /> (1) has one filter row with the red color selected.
*   The <ItemLink id="appliedpackaging:package_unpacking_bus" /> (2) has one filter row with the blue color selected.
*   The ME Drive (3) is ordinary fallback storage.

## How It Works

1.  A red package enters the network. The Storage Bus matches the red filter; the package goes into the chest. The Unpacking Bus never receives it.
2.  A blue package enters the network. The Storage Bus filter does not match, so the network tries the next destination. The Unpacking Bus matches the blue filter; the package is unpacked.
3.  A green package enters the network. Neither bus filter matches, so the package remains in network storage.

## Priority-Based Routing

Adjust priorities to control the order destinations are tried:

*   Higher priority on the Unpacking Bus: packages are unpacked on arrival. Storage Buses are fallbacks.
*   Higher priority on the Storage Bus: packages are stored first. Unpacking Buses only receive overflow.

## Advanced Pattern with Multi-Color Routing

An [advanced processing pattern](../devices/advanced-pattern-terminal.md) can encode up to 81 columns, each with its own color. Combined with color-filtered Unpacking Buses and Sequence Buffers on a subnetwork, each column's package routes to a different buffer chain, enabling complex multi-destination assembly from a single pattern. This is particularly useful for modded machines that require specific items in specific input slots, like Create's mechanical crafters.

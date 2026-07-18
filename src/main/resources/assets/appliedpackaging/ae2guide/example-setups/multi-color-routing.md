---
navigation:
  parent: example-setups/index.md
  title: Multi-Color Routing
  icon: appliedpackaging:red_package
  position: 30
---

# Multi-Color Routing

Got multiple production lines? Use different package colors to route them to different places. Red packages go to the smeltery, blue packages go to the assembler, green packages go to storage.

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/package_routing.snbt" />
  <IsometricCamera yaw="195" pitch="25" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="2 1 2">
    (1) Package Storage Bus: Filtered to red packages only. Stores them in the left chest.
  </BoxAnnotation>
  <BoxAnnotation color="#ffbb55" min="3 0 1" max="5 1 2">
    (2) Package Unpacking Bus: Filtered to blue packages only. Unpacks them into the right target.
  </BoxAnnotation>
</GameScene>

## Configurations

* The <ItemLink id="appliedpackaging:package_storage_bus" /> (1) has one filter row with the red color swatch selected. Now it only stores red packages.
* The <ItemLink id="appliedpackaging:package_unpacking_bus" /> (2) has one filter row with the blue color swatch selected. Now it only unpacks blue packages.

**The marker and content filter slots are all empty in this example.** There's no reason to add them unless you want even finer control.

## How It Works

1. A red package enters the ME network. The Storage Bus matches → package goes into the chest. The Unpacking Bus never sees it.
2. A blue package enters. The Storage Bus filter doesn't match → the network tries the next destination. The Unpacking Bus matches → package is unpacked.
3. A green package enters. Neither bus matches → the package stays in network storage (or goes to wherever else you've set up).

## Priority-Based Routing

Play with priorities to control what happens when a package could go to multiple places:

* **Unpacking Bus at higher priority:** Packages are unpacked as soon as they arrive. Only overflow goes to storage.
* **Storage Bus at higher priority:** Packages go to chests first. Only extract them to unpack when you're ready.
* **Same priority, two Storage Buses:** AE2 prefers whichever bus already has matching packages. If both are empty, either one might get the package.

## Going Further: Filter by Contents Too

Color is just the simplest dimension. You can mix color, marker, and content filters in each row:

* **A row with red color + iron ingot marker:** Only accepts red packages tagged with "Iron Ingot"
* **A row with blue color + coal in content filter:** Only accepts blue packages that *contain* coal

Rows are OR conditions — a package matching *any* row is accepted. This means you can have one bus accept "red with iron marker" AND "blue with coal contents" if you use two separate rows.

## What About the Pre-Admission Check?

With the pre-admission check on (default), a blue package that matches the Unpacking Bus's filter but can't be unpacked right now (target full, blocking mode active) is rejected. The network then tries the next destination — so the package might end up in a Storage Bus instead. This prevents packages from getting stuck waiting in the wrong bus.

---
navigation:
  parent: machines/index.md
  title: ME Package Assembler
  icon: appliedpackaging:package_assembler
  position: 10
item_ids:
- appliedpackaging:package_assembler
categories:
- applied packaging machines
---

# The ME Package Assembler

<BlockImage id="appliedpackaging:package_assembler" scale="8" />

The ME Package Assembler accepts items, fluids, and other AEKey-compatible inputs and carries out the operation defined by an adjacent <ItemLink id="ae2:pattern_provider" />, or the inserted [package pattern](../devices/advanced-pattern-terminal.md), [advanced processing pattern](../devices/advanced-pattern-terminal.md), or regular AE2 pattern, then outputs the resulting packages. Like an ME Interface, it exposes one generic AEKey inventory and does not implement a separate item-handler view; AE2 derives resource-specific capabilities from that inventory. By default, the output goes to the connected ME storage.

## Inserted Pattern and Hopper I/O

This assembler has a package pattern that specifies coal in slot 1 and cobblestone in slot 2. When coal and cobblestone arrive through the input hopper, the assembler assembles a package and outputs it into the lower hopper.

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/package_assembler_hopper.snbt" />
  <IsometricCamera yaw="195" pitch="30" />

  <BoxAnnotation color="#66aaff" min="2 1 0" max="3 2 1">
    (1) Input hopper: Feeds ingredients into the assembler.
  </BoxAnnotation>
  <BoxAnnotation color="#cc88ff" min="1 1 0" max="2 2 1">
    (2) ME Package Assembler: Pattern inserted; output set to adjacent block.
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="1 0 0" max="2 1 1">
    (3) Output hopper: Receives the completed package.
  </BoxAnnotation>
</GameScene>

This is the appropriate place to use "Output to adjacent block": the pattern is inside the assembler, and one-way external transport handles both input and output.

## Package Pattern Grid

However, the main use is next to a <ItemLink id="ae2:pattern_provider" />, arranged in a grid similar to molecular assemblers. Pattern providers push ingredients to adjacent inventories, and the assembler assembles them into packages. Since the assembler outputs packages to ME storage by default, the packages return directly to the network — an assembler on a pattern provider is all that is needed to integrate package assembly into autocrafting.

The example is a 2×2×2 checkerboard: 4 Pattern Providers and 4 Package Assemblers. The providers use 4 channels. A Smart Cable is left attached as the connection to the main network.

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="30" />

  <BoxAnnotation color="#dddddd" min="0 0 0" max="2 2 2">
    2×2×2 grid: 4 Pattern Providers and 4 Package Assemblers.
  </BoxAnnotation>
  <BoxAnnotation color="#66aaff" min="2 0 0" max="3 1 1">
    Smart Cable: Connect this end to the main network.
  </BoxAnnotation>
</GameScene>

In this mode, use package patterns only. Advanced processing patterns and ordinary AE2 crafting or processing patterns belong in the subnetwork setup below.

## Directional Provider and Routing Subnetwork

When you want to route packages by color, use a directional pattern provider. Right-click the provider with a <ItemLink id="ae2:certus_quartz_wrench" /> so it does not form a network connection on the face touching the assembler. Keep the assembler's output set to "Output to ME network" (the default), and connect it to a separate subnetwork whose only storage is one or more color-filtered Package Storage Buses.

<GameScene zoom="5" background="transparent">
  <ImportStructure src="../assets/assemblies/package_assembler_subnetwork.snbt" />
  <IsometricCamera yaw="195" pitch="30" />

  <BoxAnnotation color="#66aaff" min="0 0 0" max="1 1 2">
    (1) Main network and directional Pattern Provider. Its selected face points east.
  </BoxAnnotation>
  <BoxAnnotation color="#cc88ff" min="1 0 1" max="2 1 2">
    (2) Package Assembler: Belongs to the subnetwork and outputs to ME storage.
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="2 0 1" max="5 1 2">
    (3) Routing subnetwork.
  </BoxAnnotation>
  <BoxAnnotation color="#ff7777" min="4 0 0" max="5 1 1">
    (4) Package Storage Bus: Filtered to red packages.
  </BoxAnnotation>
  <BoxAnnotation color="#6688ff" min="4 0 2" max="5 1 3">
    (5) Package Storage Bus: Filtered to blue packages.
  </BoxAnnotation>
</GameScene>

Do not set the assembler to "Output to adjacent block" in this setup. The directional provider is still an adjacent inventory, so adjacent output can place the completed package back into the main network and bypass the routing subnetwork.

This is also the recommended setup when using regular AE2 patterns (crafting or processing) with the assembler, since the assembler can apply its own color and marker to the resulting packages for routing.

## Advanced Pattern Output

When processing an advanced pattern, the assembler produces one package per column and outputs them in column order. Assign a different color to each column in the encoding terminal, then use one filtered Package Storage Bus for each destination on the subnetwork. This makes routing independent of output order.

## Output Modes

*   **Output to ME network** (default). Packages go into the connected ME storage.
*   **Output to adjacent block.** Packages are placed into a selected neighboring container. The assembler picks one accepting container from the six adjacent faces and uses that direction for the entire batch.
*   **Disabled.** Packages stay in the assembler's output slots for manual extraction.

Use adjacent output for the inserted-pattern setup with one-way external transport. Keep ME-network output for both the package-pattern grid and the directional-provider subnetwork.

## Capacity

Before consuming any ingredients, the assembler checks that every package fits within the capacity limit:

| Component | Max Types | Max Units |
|-----------|-----------|-----------|
| None (default 1k tier) | 9 | 256 |
| <ItemLink id="ae2:cell_component_16k" /> | 16 | 4,096 |
| <ItemLink id="ae2:cell_component_64k" /> | 63 | 16,384 |
| <ItemLink id="ae2:cell_component_256k" /> | 63 | 65,536 |

The empty default 1k tier and each supported component provide one quarter of their nominal ME capacity in package units.

Storage cells and 1k components are not accepted; the empty slot already provides the 1k tier. Install a supported upgrade component in the assembler's component slot.

## Color and Marker

The assembler has its own color picker and marker slot. When using package or advanced patterns, the pattern's own color and marker are used and the assembler's settings are ignored. When using regular AE2 patterns, the assembler's color and marker are applied. The marker item is never consumed.

## Blocking Mode

When enabled, the assembler waits until the output target is empty before starting a new batch. Once a batch is admitted, it drains completely — every package is output in order without rechecking.

## Comparator Output

The comparator emits 0 while idle, 1 while working, and 2 while completed packages remain in the output.

## Upgrades

The ME Package Assembler supports the following [upgrades](ae2:items-blocks-machines/upgrade_cards.md):

*   <ItemLink id="ae2:speed_card" /> (up to 5)
*   AE2 16k, 64k, or 256k storage components for package capacity

## Recipe

<RecipeFor id="appliedpackaging:package_assembler" />

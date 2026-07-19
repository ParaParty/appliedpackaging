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

The ME Package Assembler takes items input into it and carries out the operation defined by an adjacent <ItemLink id="ae2:pattern_provider" />, or the inserted [package pattern](devices/advanced-pattern-terminal.md), [advanced processing pattern](devices/advanced-pattern-terminal.md), or regular AE2 pattern, then outputs the resulting packages. By default, output goes to the connected ME storage.

This assembler has a package pattern that specifies coal in slot 1 and cobblestone in slot 2. When coal and cobblestone are in ME storage, the assembler produces a package with both items.

## The Main Use of the ME Package Assembler

The main use is next to a <ItemLink id="ae2:pattern_provider" />. Pattern providers push ingredients to adjacent inventories, and the assembler turns them into packages. Since output goes to ME storage by default, a provider on an assembler is all you need to add package assembly to autocrafting.

**Note:** If you want the packages on a subnetwork instead, use a directional pattern provider (right-click with a <ItemLink id="ae2:certus_quartz_wrench" />) so the provider and assembler don't share a network connection. The assembler's output can then go through a subnetwork to Unpacking Buses and Sequence Buffers that split the package across machine faces.

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="25" />
</GameScene>

## Output Modes

*   **Output to ME network** (default). Packages go directly into the connected ME storage.
*   **Output to adjacent block.** Packages go into a neighboring container. The assembler picks one from the six adjacent faces and uses that direction for the whole batch.
*   **Disabled.** Packages stay in the assembler's output slots for manual extraction.

## Capacity

Before taking any ingredients, the assembler checks that every package in the plan fits the capacity limit:

| Component | Max Types | Max Total |
|-----------|-----------|-----------|
| None | 9 | 9 |
| <ItemLink id="ae2:cell_component_16k" /> | 16 | 16 |
| <ItemLink id="ae2:cell_component_64k" /> | 63 | 64 |
| <ItemLink id="ae2:cell_component_256k" /> | 63 | 256 |

Storage cells and 1k components are not accepted. Install the component in the assembler's component slot.

## Color and Marker

The assembler has its own color picker and marker slot. When using a package or advanced pattern, the pattern's own colors and markers are used and the assembler's settings are ignored. When using a regular AE2 pattern, the assembler's color and marker are applied. The marker item is never consumed.

## Blocking Mode

When enabled, the assembler waits until the output target is empty before starting a new batch. Once a batch is admitted, it drains completely — every package in the batch is output in order without rechecking.

## Comparator Output

The comparator emits 0 while idle, 1 while working, and 2 while completed packages remain in the output.

## Upgrades

The ME Package Assembler supports the following [upgrades](ae2:items-blocks-machines/upgrade_cards.md):

*   <ItemLink id="ae2:speed_card" /> (up to 5)
*   AE2 16k, 64k, or 256k storage components for package capacity

## Recipe

<RecipeFor id="appliedpackaging:package_assembler" />

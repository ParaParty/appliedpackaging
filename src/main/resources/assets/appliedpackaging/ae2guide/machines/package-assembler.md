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

The ME Package Assembler is your main package factory. It takes a pattern and produces matching packages — with the exact order, color, and layout you encoded.

If you're familiar with AE2's autocrafting: place it next to a <ItemLink id="ae2:pattern_provider" /> and it slots right in. The provider pushes ingredients, the assembler makes the package, and the result goes back into the network (or an adjacent chest, if you prefer).

## Getting It Running

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="25" />
</GameScene>

* 1. Place the assembler and plug it into your ME network.
* 2. Put a <ItemLink id="ae2:pattern_provider" /> next to it (any face works).
* 3. Load your encoded [package pattern](devices/advanced-pattern-terminal.md) into the provider.
* 4. Make sure the ingredients are in your ME network.
* 5. When autocrafting calls for the package, the assembler does its thing.

You can also skip the provider entirely: put a pattern directly in the assembler's pattern slot and place ingredients in the GUI input grid. Good for testing or one-off batches.

## What It Actually Does

Before it takes a single item, the assembler checks whether the complete package fits the current capacity. If the pattern is too big, the pattern stays visible for diagnosis but its inputs stay locked — nothing is wasted.

Then it produces the packages. One package for a regular pattern. One per column for an [advanced pattern](devices/advanced-pattern-terminal.md).

## Color and Marker

The assembler has its own color picker and marker slot. Whether you can use them depends on the pattern:

* **Package or advanced pattern:** The pattern's own colors and markers are used. The assembler's settings are ignored.
* **Regular AE2 crafting or processing pattern:** The assembler's color and marker are applied. If the marker slot is empty, the pattern's output item becomes the marker.
* **No pattern loaded:** The color picker shows the persistent machine setting — for when you change your mind.

The marker item sits in its slot forever and is never consumed. It just tags every produced package with that label.

## Capacity

The assembler enforces a capacity limit based on what storage component you install:

| Component | Types Max | Total Max |
|-----------|-----------|-----------|
| None | 9 | 9 |
| <ItemLink id="ae2:cell_component_16k" /> | 16 | 16 |
| <ItemLink id="ae2:cell_component_64k" /> | 63 | 64 |
| <ItemLink id="ae2:cell_component_256k" /> | 63 | 256 |

Install the component in the assembler's component slot. Storage cells and 1k components won't work — only raw 16k, 64k, and 256k components.

## Where Packages Go

Three options, selectable in the GUI:

* **Output to ME network** — Packages go straight into network storage. Simplest option; the assembler needs a network connection.
* **Output to adjacent block** — Packages are pushed into the neighboring container you select. The assembler picks one from the six adjacent faces and sticks with it for the whole batch.
* **Disabled** — Packages stay in the assembler. You pull them out yourself through the GUI.

## Blocking Mode

Turn this on and the assembler won't start a new batch while the output target already has packages in it. Once a batch is allowed, it empties completely — every package in the batch goes out in order without rechecking.

## Comparator Output

The assembler gives you a redstone signal you can use for your own automation:

* **0** — Idle
* **1** — Working (progress bar is moving)
* **2** — Done (packages are waiting in the output)

## Upgrades

* <ItemLink id="ae2:speed_card" /> — Faster assembly (up to 5)
* AE2 16k/64k/256k storage components — Bigger packages

## Recipe

<RecipeFor id="appliedpackaging:package_assembler" />

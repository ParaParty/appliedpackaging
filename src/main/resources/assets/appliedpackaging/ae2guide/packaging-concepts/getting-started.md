---
navigation:
  parent: packaging-concepts/index.md
  title: Getting Started
  icon: appliedpackaging:package_pattern
  position: 5
---

# Getting Started with Applied Packaging

This guide walks you through making your very first package — from encoding a pattern to watching it unpack into a chest. By the end you'll understand the whole workflow and can start designing your own packaging setups.

## Why Packages?

A vanilla furnace needs coal in the side slot and ore in the top slot. Normal AE2 patterns dump everything wherever they land — coal might go in the top, your ore goes nowhere, and the furnace just sits there.

A package solves this. You encode coal in slot 1, ore in slot 2. The package remembers that order. When it's unpacked into the furnace, coal goes exactly where it should, every time.

Packages also help when you're running lots of production lines. Red packages for smelting, blue for crafting — you can tell what's going where with a glance.

## What You'll Need

* Your ingredients list:
  * 1x <ItemLink id="appliedpackaging:advanced_pattern_encoding_terminal" />
  * 1x <ItemLink id="appliedpackaging:package_assembler" />
  * 1x <ItemLink id="appliedpackaging:package_unpacking_bus" />
  * A few ME [cables](ae2:items-blocks-machines/cables.md) (glass or covered, doesn't matter)
  * An ME network with power and at least one storage cell
  * A few <ItemLink id="ae2:blank_pattern" />s
  * Some cobblestone and coal (or whatever items you want to practice with)
  * A chest to serve as the output target
  * A <ItemLink id="ae2:pattern_provider" /> (optional — the assembler can also use its own pattern slot)

## Step 1: Encode a Package Pattern

1. Place the <ItemLink id="appliedpackaging:advanced_pattern_encoding_terminal" /> on an ME cable connected to your network. It needs a channel, same as AE2's regular pattern terminal.

2. Open the terminal. You'll see two tabs at the top: **Package Pattern** and **Advanced**. You want the Package Pattern tab — click it if it's not already selected.

3. Put a blank AE2 pattern in the blank-pattern slot on the right.

4. Place **coal** in the first slot of the input grid, and **cobblestone** in the second.

   **Empty slots matter.** If you put coal in slot 1, leave slot 2 empty, and put iron in slot 3, the package remembers that slot 2 was intentionally left open. This is important when your machine has fixed slot positions.

   You can drag items in from JEI/EMI, or use AE2's middle-click shortcut.

5. Pick a color from the color picker — red, blue, whatever you like. This is just for organizing; it doesn't change how the package works.

6. Optionally, drop an item in the **Marker** slot. The marker is basically a label — you might put an iron ingot here to tag this package as "furnace input." The marker item is never consumed, it just shows up on the package's tooltip.

7. Click **Encode**. You'll see a message in chat. Grab the encoded pattern from the output slot.

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
</Row>

## Step 2: Assemble the Package

Now let's turn that pattern into an actual package you can hold.

1. Place the <ItemLink id="appliedpackaging:package_assembler" /> and connect it to your ME network with a cable.

2. Open its GUI. Put your encoded pattern in the pattern slot on the right.

3. Make sure the cobblestone and coal are in your ME network. The assembler pulls them from network storage automatically.

4. The assembler starts working — you'll see a progress bar. When it finishes, a colored package appears in the output area.

5. Pull the package out into your inventory. Hover over it and hold Shift — you'll see the color, the contents in order, and the capacity.

<BlockImage id="appliedpackaging:package_assembler" scale="8" />

You just made your first package. Congrats — but the real fun is getting it into a machine.

## Step 3: Unpack Into a Chest

1. Place a chest on the ground. This stands in for whatever machine you'd actually be using.

2. Put the <ItemLink id="appliedpackaging:package_unpacking_bus" /> on an ME cable pointing at the chest. It needs a channel.

3. Drop your package into the ME network through any terminal. The network will route it to the Unpacking Bus.

4. A moment later, the coal and cobblestone appear in the chest — coal first, then cobblestone. In that exact order.

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/package_unpacking_bus.snbt" />
</GameScene>

5. Stick a hopper under the chest to pull items into a machine, and you've automated ordered item delivery.

That's it. **Encode → Assemble → Unpack.** Everything else is variations on this loop.

## What's Next?

* Learn more about [packages](packages.md) — capacity, colors, markers, and manual unpacking
* The [Advanced Pattern Encoding Terminal](devices/advanced-pattern-terminal.md) can encode up to 81 packages at once
* Look at [Example Setups](example-setups/index.md) for ready-to-build designs
* If something goes wrong, check [Troubleshooting](troubleshooting.md)

Packages travel through your ME network exactly like any other item — store them in drives, route them with storage buses, export them with export buses. The packaging layer just adds order and structure on top of normal AE2 logistics.

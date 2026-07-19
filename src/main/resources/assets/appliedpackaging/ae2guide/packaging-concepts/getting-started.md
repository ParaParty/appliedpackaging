---
navigation:
  parent: packaging-concepts/index.md
  title: Getting Started
  icon: appliedpackaging:package_pattern
  position: 5
---

# Getting Started

## Your First Package

Normal AE2 patterns push ingredients to one adjacent inventory. If you want to feed multiple machines from one pattern, or split a recipe's ingredients into separate groups that each go to a different destination, you need to group the ingredients first. That's what packages do.

A package bundles a set of ingredients into a single item that can be routed through your ME network. An Unpacking Bus at the destination opens the package and delivers the contents in order. A Sequence Buffer preserves that order and maps each item to a specific machine face.

## What You'll Need

*   1x <ItemLink id="appliedpackaging:advanced_pattern_encoding_terminal" />
*   1x <ItemLink id="appliedpackaging:package_assembler" />
*   1x <ItemLink id="appliedpackaging:package_unpacking_bus" />
*   A few ME cables and a powered ME network with storage
*   A few <ItemLink id="ae2:blank_pattern" />s
*   Items to package (cobblestone and coal for this example)
*   A chest to receive the output

## Step 1: Encode a Package Pattern

1. Place the <ItemLink id="appliedpackaging:advanced_pattern_encoding_terminal" /> on an ME cable connected to your network. It needs a channel.

2. Open the terminal and select the **Package Pattern** tab.

3. Insert a blank AE2 pattern into the blank-pattern slot.

4. Place cobblestone in slot 1 and coal in slot 2. The slot positions are recorded in the pattern.

   You can drag items from JEI/EMI or use AE2's middle-click shortcut.

5. Pick a color from the color picker. Colors let you classify packages for routing — red packages to one destination, blue to another.

6. Click **Encode**. Take the <ItemLink id="appliedpackaging:package_pattern" /> from the output slot.

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
</Row>

## Step 2: Assemble the Package

1. Place the <ItemLink id="appliedpackaging:package_assembler" /> and connect it to your ME network with a cable.

2. Open the GUI and put your encoded pattern in the pattern slot. The assembler pulls ingredients from ME storage and produces the package. By default, output goes to ME storage.

3. Once the progress bar completes, the package is in your ME storage, visible from any terminal.

<BlockImage id="appliedpackaging:package_assembler" scale="8" />

## Step 3: Route and Unpack to a Chest

1. Place a chest and put the <ItemLink id="appliedpackaging:package_unpacking_bus" /> on an ME cable facing the chest. The bus receives packages from the network and inserts their contents into the adjacent inventory in the encoded order.

2. The package routes to the bus. Cobblestone and coal appear in the chest in order.

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/package_unpacking_bus.snbt" />
</GameScene>

## Going Further

### Group Routing

Place the Unpacking Bus on a subnetwork with a Sequence Buffer. The bus unpacks the package into the buffer, and each buffer member delivers one item to a specific machine face. This lets one package feed multiple faces of a machine — useful for modded machines like Create's mechanical crafters that need specific items in specific input slots.

### Splitting a Recipe

Encode two package patterns — one for each group of ingredients. Then encode a regular AE2 processing pattern whose inputs are those two packages. When autocrafting requests the result, the assembler produces both packages, and each can be routed to a different destination through color-filtered Unpacking Buses. Alternatively, use an advanced processing pattern with two columns — each column becomes one independently-colorable package.

## Next Steps

*   Learn more about [packages](packages.md) — capacity, colors, markers, and how to unpack by hand
*   The [Advanced Pattern Encoding Terminal](devices/advanced-pattern-terminal.md) can encode up to 81 packages from one pattern
*   See [Example Setups](example-setups/index.md) for ready-to-build designs
*   [Troubleshooting](troubleshooting.md) if something doesn't work

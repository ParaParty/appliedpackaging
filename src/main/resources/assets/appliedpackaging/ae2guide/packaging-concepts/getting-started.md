---
navigation:
  parent: packaging-concepts/index.md
  title: Getting Started
  icon: appliedpackaging:package_pattern
  position: 5
---

# Getting Started

## Your First Package

A normal AE2 pattern provider pushes ingredients into a single adjacent inventory. Packages let you group those ingredients into one item, then route that item through subnetworks to one or more destinations.

The full loop is: encode a pattern, assemble a package, and route it to an output chest.

## What You Will Need

*   1x <ItemLink id="appliedpackaging:advanced_pattern_encoding_terminal" />
*   1x <ItemLink id="appliedpackaging:package_assembler" />
*   1x <ItemLink id="appliedpackaging:package_unpacking_bus" />
*   A few ME cables and an ME network with power and at least one storage cell
*   A few <ItemLink id="ae2:blank_pattern" />s
*   Some cobblestone and coal
*   A chest

## Step 1: Encode a Package Pattern

1. Place the <ItemLink id="appliedpackaging:advanced_pattern_encoding_terminal" /> on an ME cable connected to your network. Like other AE2 terminals, it needs a channel.

2. Open the terminal and select the **Package Pattern** tab.

3. Insert a blank AE2 pattern into the blank-pattern slot.

4. Place cobblestone into slot 1 and coal into slot 2. The slot order is recorded in the pattern.

   You can drag items from JEI or EMI, or use AE2's middle-click shortcut.

5. Choose a color from the color picker. Colors are used later for filtering and routing.

6. Click **Encode**. Take the <ItemLink id="appliedpackaging:package_pattern" /> from the output slot.

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
</Row>

## Step 2: Assemble the Package

1. Place the <ItemLink id="appliedpackaging:package_assembler" /> and connect it to your ME network with a cable.

2. Open the GUI and put the encoded pattern in the pattern slot. The assembler pulls ingredients from ME storage. By default, the completed package goes directly into ME storage.

3. Once the progress bar fills, the package is in your network — visible from any terminal.

<BlockImage id="appliedpackaging:package_assembler" scale="8" />

## Step 3: Unpack the Package

1. Place a chest and put the <ItemLink id="appliedpackaging:package_unpacking_bus" /> on an ME cable facing the chest.

2. The network routes the package to the bus. Cobblestone and coal appear in the chest in the encoded order.

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/package_unpacking_bus.snbt" />
</GameScene>

## Going Further

### Group Routing

Place an Unpacking Bus on a subnetwork. Route a package through that subnetwork and the bus unpacks it at the destination. See the [Ordered Machine Inputs](../example-setups/ordered-machine-inputs.md) example for a step-by-step build.

### Splitting a Recipe

Encode two package patterns, each with a different color, each carrying a subset of the recipe's ingredients. Then encode a regular AE2 processing pattern whose inputs are those two packages. When autocrafting runs, each package can be routed to a different destination by filtering on its color.

Alternatively, use an advanced processing pattern with two columns in the encoding terminal. Each column becomes one package with its own color.

## Next Steps

*   [Packages](packages.md) covers capacity, colors, markers, and pattern types.
*   The [Advanced Pattern Encoding Terminal](../devices/advanced-pattern-terminal.md) page explains both pages of the encoding terminal in detail.
*   [Example Setups](../example-setups/index.md) has annotated builds with step-by-step instructions.
*   [Troubleshooting](../troubleshooting.md) if something stops working.

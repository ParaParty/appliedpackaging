---
navigation:
  parent: example-setups/index.md
  title: Basic Packaging Line
  icon: appliedpackaging:package_assembler
  position: 10
---

# Basic Packaging Line

This setup shows the Pattern Provider grid of the Package Assembler. The provider pushes ingredients to the assembler, which assembles the package and returns it to ME storage by default.

Note that since this uses a <ItemLink id="ae2:pattern_provider" />, it is meant to integrate into your [autocrafting](ae2:ae2-mechanics/autocrafting.md) setup. In this grid, use only package patterns — for ordinary AE2 patterns, use the [directional-provider setup](../machines/package-assembler.md) with a subnetwork.

For standalone usage with external item input, put the pattern directly in the assembler's pattern slot and feed items in through hoppers or pipes.

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="30" />

  <BoxAnnotation color="#dddddd" min="0 0 0" max="4 4 4">
    (1) 4×4×4 checkerboard: 32 Pattern Providers and 32 Package Assemblers.
  </BoxAnnotation>
</GameScene>

## Configurations

*   The 32 <ItemLink id="ae2:pattern_provider" /> blocks use their default configuration and contain [package patterns](../devices/advanced-pattern-terminal.md). Together they consume 32 channels, so connect the grid with dense cable.
*   The 32 <ItemLink id="appliedpackaging:package_assembler" /> blocks use the default ME-network output mode. Packages go directly into ME storage.
*   Do not use advanced or ordinary AE2 patterns in this grid. Use the directional-provider subnetwork described on the [Package Assembler](../machines/package-assembler.md) page instead.

## How It Works

1.  Autocrafting requests a package. The Pattern Provider pushes the ingredients into the assembler.
2.  The assembler checks that the package fits the capacity limit before consuming any items.
3.  The assembler produces the package.
4.  The package is output to ME storage.

To route packages further — for example, to unpack them through a subnetwork — use the [directional-provider setup](../machines/package-assembler.md) instead.

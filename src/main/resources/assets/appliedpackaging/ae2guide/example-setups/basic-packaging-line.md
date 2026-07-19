---
navigation:
  parent: example-setups/index.md
  title: Basic Packaging Line
  icon: appliedpackaging:package_assembler
  position: 10
---

# Basic Packaging Line

Note that since this uses a <ItemLink id="ae2:pattern_provider" />, it is meant to integrate into your [autocrafting](ae2:ae2-mechanics/autocrafting.md) setup. If you want to assemble packages standalone, put the pattern directly in the assembler's pattern slot and place ingredients in the GUI input grid.

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="25" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="2 1 2">
    (1) Pattern Provider: In its default configuration, with an encoded package or advanced pattern inside.
  </BoxAnnotation>
  <BoxAnnotation color="#cc88ff" min="2 0 1" max="3 1 2">
    (2) ME Package Assembler: Output mode set to "Output to ME network" (default).
  </BoxAnnotation>
</GameScene>

## Configurations

*   The <ItemLink id="ae2:pattern_provider" /> (1) is in its default configuration, with the relevant [package or advanced pattern](devices/advanced-pattern-terminal.md) inside.
*   The <ItemLink id="appliedpackaging:package_assembler" /> (2) uses the default output mode (ME network). Packages go directly into ME storage.

## How It Works

1.  Autocrafting requests a package. The Pattern Provider pushes the ingredients into the assembler.
2.  The assembler validates that the complete package fits the capacity limit before consuming any item.
3.  The assembler produces the package.
4.  The package is output to ME storage.

## Output to Adjacent Block

To output to a chest or other container instead, change the assembler's output mode to "Output to adjacent block" and select the target direction. The assembler picks one accepting container from the six adjacent faces and uses that direction for the entire batch.

## Using a Subnetwork

To route the package output through a subnetwork (for example, to a line of Unpacking Buses and Sequence Buffers), use a directional pattern provider adjusted with a <ItemLink id="ae2:certus_quartz_wrench" />. This prevents the provider and assembler from forming a network connection, so the assembler can be on a separate subnetwork that receives the packages and distributes contents to multiple machine faces.

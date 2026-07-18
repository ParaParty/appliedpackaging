---
navigation:
  parent: example-setups/index.md
  title: Basic Packaging Line
  icon: appliedpackaging:package_assembler
  position: 10
---

# Basic Packaging Line

This is the simplest autocrafting setup you can build: a Pattern Provider pushes ingredients into a Package Assembler, and completed packages go into a chest.

Note that since this uses a <ItemLink id="ae2:pattern_provider" />, it's meant to integrate into your [autocrafting](ae2:ae2-mechanics/autocrafting.md) setup. If you just want to make packages manually, put a pattern directly in the assembler and feed it items through the GUI.

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="25" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="2 1 2">
    (1) Pattern Provider: In its default configuration, with an encoded package or advanced pattern inside.
  </BoxAnnotation>
  <BoxAnnotation color="#cc88ff" min="2 0 1" max="3 1 2">
    (2) ME Package Assembler: Output mode set to "Output to adjacent block." Accepts 5 speed cards.
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="3 0 1" max="4 1 2">
    (3) Chest: Catches the completed packages. Replace with any machine or ME interface.
  </BoxAnnotation>
</GameScene>

## Configurations

* The <ItemLink id="ae2:pattern_provider" /> (1) is in its default configuration, with your encoded [package or advanced pattern](devices/advanced-pattern-terminal.md) inside.
* The <ItemLink id="appliedpackaging:package_assembler" /> (2) has its output mode set to "Output to adjacent block," pointing at the chest. Install speed cards to go faster.
* The chest (3) needs no configuration. It just receives packages.

## How It Works

1. Autocrafting requests a package → the Pattern Provider pushes ingredients into the assembler.
2. The assembler checks capacity **before** consuming anything. If the package is too big, nothing is wasted.
3. The progress bar fills. <ItemLink id="ae2:speed_card" />s shorten this.
4. The completed package is pushed into the chest.
5. If blocking mode is on, the assembler waits until the chest is empty before starting the next batch.

## Variations

* **ME Network output:** Change the assembler's output mode to "Output to ME network." The package goes straight into network storage. No chest needed.
* **Local mode:** Skip the Pattern Provider entirely — put a pattern in the assembler's own slot and place ingredients in the GUI.

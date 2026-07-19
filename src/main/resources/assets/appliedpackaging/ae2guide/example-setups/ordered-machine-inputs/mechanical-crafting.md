---
navigation:
  parent: example-setups/ordered-machine-inputs.md
  title: "Example 1: 5×5 Mechanical Crafting"
  icon: appliedpackaging:sequence_buffer
  position: 10
---

# Example 1: 5×5 Mechanical Crafting

Put one 5×5 advanced pattern in the Pattern Provider. Its five columns use different colors, and each column records five ordered positions including empty ones. Point the provider's selected output face at the Package Assembler, enable Blocking Mode on the assembler, then use five color-filtered Package Unpacking Buses to send the five packages to their matching buffer rows.

<GameScene zoom="3.25" background="transparent">
  <ImportStructure src="../../assets/assemblies/sequence_mechanical_crafting_5x5.snbt" />
  <IsometricCamera yaw="215" pitch="25" />

  <BoxAnnotation color="#dd88cc" min="0 0 4" max="3 1 6">
    (1) Main network: Pattern Provider with the 5×5 advanced pattern.
  </BoxAnnotation>
  <BoxAnnotation color="#eeeeee" min="0 0 2" max="3 6 4">
    (2) Processing subnet: Blocking-mode assembler and five unpacking branches.
  </BoxAnnotation>
  <BoxAnnotation color="#66aaff" min="2 1 2" max="8 6 3">
    (3) Five Sequence Buffer rows: Each has one endpoint and five members.
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="3 1 3" max="8 6 4">
    (4) 5×5 front chest wall: One chest stands in for each Mechanical Crafter.
  </BoxAnnotation>
</GameScene>

All 25 chests are placed in front of the Sequence Buffer members. They only stand in for Mechanical Crafters, so this scene can load without Create installed. In the real build, replace them one-for-one with Mechanical Crafters facing the crafting center. The items in the sample pattern only make its sparse 5×5 layout visible; replace them with the real recipe.

For a horizontal Mechanical Crafter layout, first transfer the recipe from JEI into the Advanced Pattern Encoding Terminal, then use **Transpose Recipe** to exchange rows and columns so the pattern's package columns line up with the horizontal buffer rows. Outputs are not transposed.

Every endpoint has Automatic Output, Pattern Mode, and Synchronized Output enabled. Pattern Mode preserves empty package positions, so positions 1–5 of a column always reach the five machine faces in that row. If it is disabled, the ingredients are compacted forward and a recipe with empty positions shifts out of place.

The processing subnet uses 6 channels (the Package Assembler and five Package Unpacking Buses), so regular Smart Cable is sufficient. The subnet receives power from the pink main network through Quartz Fiber only. Blocking Mode prevents the assembler from sending the next batch while matching packages from the previous one are still in the processing subnet.

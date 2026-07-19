---
navigation:
  parent: example-setups/ordered-machine-inputs.md
  title: "Example 3: Multi-Step Crafting"
  icon: appliedpackaging:advanced_processing_pattern
  position: 30
---

# Example 3: Multi-Step Crafting

Put an advanced pattern with four columns in the Pattern Provider. The columns hold the ingredients for four processing steps and are colored red, blue, green, and yellow. Point the provider's selected output face at the Package Assembler. The assembler creates one package per column, and four matching Package Storage Buses route those packages into the four step-input chests.

<GameScene zoom="3.25" background="transparent">
  <ImportStructure src="../../assets/assemblies/advanced_multistep_crafting.snbt" />
  <IsometricCamera yaw="205" pitch="35" />

  <BoxAnnotation color="#dd88cc" min="0 0 4" max="2 1 6">
    (1) Main network: Pattern Provider with the four-column advanced pattern.
  </BoxAnnotation>
  <BoxAnnotation color="#eeeeee" min="0 0 2" max="8 1 4">
    (2) Processing subnet: Package Assembler and four color-filtered Package Storage Buses.
  </BoxAnnotation>
  <BoxAnnotation color="#b02e26" min="3 0 1" max="4 1 3">
    (3) Red bus: Step 1 input chest.
  </BoxAnnotation>
  <BoxAnnotation color="#3c44aa" min="4 0 2" max="5 1 4">
    (4) Blue bus: Step 2 input chest.
  </BoxAnnotation>
  <BoxAnnotation color="#5e7c16" min="6 0 1" max="7 1 3">
    (5) Green bus: Step 3 input chest.
  </BoxAnnotation>
  <BoxAnnotation color="#fed83d" min="7 0 2" max="8 1 4">
    (6) Yellow bus: Step 4 input chest.
  </BoxAnnotation>
</GameScene>

The chests represent the package-input inventory for each step. A Package Storage Bus stores the complete package; it does not insert the package contents into the chest. In a real build, let each step's own unpacking path consume it. The sample ingredients only make the four-column grouping visible and can be replaced with the real recipe.

The white processing subnet uses 5 channels (the Package Assembler and four Package Storage Buses) and receives power from the pink main network through Quartz Fiber only. Column order controls the order in which the assembler creates packages, while color controls their destinations. If the steps have real dependencies, return the preceding step's result to the ME network before triggering the next processing pattern instead of relying only on the four-column output order.

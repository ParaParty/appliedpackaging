---
navigation:
  parent: example-setups/ordered-machine-inputs.md
  title: "Example 2: Parallel Furnace Group"
  icon: appliedpackaging:sequence_buffer
  position: 20
---

# Example 2: Parallel Furnace Group

Put one processing pattern in the Pattern Provider: input 1 is 1 Coal, input 2 is 8 Raw Iron, and the output is 8 Iron Ingots. Point the provider's selected output face at the Package Assembler. The assembler wraps the complete input batch into one package, then the white processing subnet routes that package to one of four available furnace branches.

<GameScene zoom="3.5" background="transparent">
  <ImportStructure src="../../assets/assemblies/sequence_furnace_array.snbt" />
  <IsometricCamera yaw="215" pitch="30" />

  <BoxAnnotation color="#dd88cc" min="0 1 2" max="1 2 5">
    (1) Main network: Pattern Provider with the Coal + 8 Raw Iron processing pattern.
  </BoxAnnotation>
  <BoxAnnotation color="#eeeeee" min="0 1 0" max="6 3 3">
    (2) Processing subnet: Package Assembler and four parallel unpacking branches.
  </BoxAnnotation>
  <BoxAnnotation color="#ff9944" min="1 0 2" max="6 2 5">
    (3) Return subnet: Import Buses return the furnace outputs through the Pattern Provider.
  </BoxAnnotation>
</GameScene>

In each branch, member 1 drops Coal into the hopper, which inserts it through the furnace side into the fuel slot. Member 2 inserts 8 Raw Iron through the top. The Import Bus beneath the furnace collects the Iron Ingots.

Set the four Package Unpacking Buses from left to right to priorities 4, 3, 2, and 1. The network first tries the highest-priority branch that can accept the complete package, then falls back through the remaining branches while earlier ones are busy.

The pink cable is the main network. The white processing subnet uses 5 channels (the Package Assembler and four Package Unpacking Buses); the orange return subnet also uses 5 (four Import Buses and one ordinary Storage Bus). Both subnets receive power through Quartz Fiber only. Do not join either colored subnet directly to the main network: the selected provider face keeps the assembler subnet separate, while the Storage Bus against another provider face returns the processing result without merging channels.

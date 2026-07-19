---
navigation:
  parent: example-setups/index.md
  title: Ordered Machine Inputs
  icon: appliedpackaging:package_unpacking_bus
  position: 20
---

# Ordered Machine Inputs

Machines with position-specific input slots — like Create's mechanical crafters — need specific items in specific positions. A Sequence Buffer preserves the order and sparse layout of pattern inputs or package contents, delivering each item to the correct machine face.

This setup can also handle pattern provider pushes directly: the provider pushes ingredients in order, and the buffer maps each ingredient to a member by position.

## Package Unpacking Bus Input

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/sequence_line.snbt" />
  <IsometricCamera yaw="205" pitch="30" />

  <BoxAnnotation color="#ffbb55" min="0 0 0" max="1 1 2">
    (1) Package Unpacking Bus and network cable: Feed the package into the endpoint.
  </BoxAnnotation>
  <BoxAnnotation color="#66aaff" min="1 0 1" max="2 1 2">
    (2) Endpoint: Holds configuration and does not store an item.
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="2 0 1" max="5 1 2">
    (3) Members: Each holds one item and outputs north into its own target.
  </BoxAnnotation>
</GameScene>

## Pattern Provider Input

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/sequence_line_pattern_provider.snbt" />
  <IsometricCamera yaw="205" pitch="30" />

  <BoxAnnotation color="#ffbb55" min="0 0 0" max="1 1 2">
    (1) Pattern Provider and network cable: The selected output face points into the endpoint.
  </BoxAnnotation>
  <BoxAnnotation color="#66aaff" min="1 0 1" max="2 1 2">
    (2) Endpoint: Receives the ordered pattern ingredients.
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="2 0 1" max="5 1 2">
    (3) Members: Each preserves one position and outputs north into its own target.
  </BoxAnnotation>
</GameScene>

## Examples

The four builds below are kept on separate pages. Start with the two input structures here to understand the endpoint, members, and input source, then open the setup you need.

<SubPages />

## Configurations

*   With a <ItemLink id="appliedpackaging:package_unpacking_bus" /> (1), place the bus against the endpoint (2). Pre-admission check is on (default).
*   With a Pattern Provider (1), use an AE2 wrench to point its selected output face into the endpoint (2).
*   The endpoint has Automatic Output enabled. Enable Pattern Mode when using packages with sparse slot layouts.
*   Each member (3) has its output face set to point at the corresponding input slot on the target machine.

The Sequence Buffer itself does not need AE power. The cable in each structure belongs to the input source's ME network.

## How It Works

1.  A package (or pattern provider push) delivers items to the Sequence Buffer endpoint.
2.  The first item goes to member 1, the second to member 2, and so on.
3.  Each member outputs through its configured face to the target machine.
4.  With Synchronized Output enabled, all members wait for each other — the entire batch delivers together or not at all.

## The One-Item Rule

If a package contains coal in entry 1 and coal in entry 3, member 1 receives the first coal and member 3 receives the second. The buffer's rule of accepting only one item per member until emptied is what keeps these identical items separate. Without this rule, the two coal would merge and lose their distinct positions.

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

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/sequence_line.snbt" />
  <IsometricCamera yaw="205" pitch="30" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="1 1 2">
    (1) Endpoint: Holds configuration. Place the Unpacking Bus (or pattern provider) against this face.
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="1 0 1" max="4 1 2">
    (2) Members: Each holds one item. Configure each member's output face to point at the target machine's corresponding input slot.
  </BoxAnnotation>
</GameScene>

## Configurations

*   The <ItemLink id="appliedpackaging:package_unpacking_bus" /> is placed on the endpoint face (1). Pre-admission check is on (default).
*   The endpoint has Automatic Output enabled. Enable Pattern Mode when using packages with sparse slot layouts.
*   Each member's output face is set to point at the corresponding input slot on the target machine.

## How It Works

1.  A package (or pattern provider push) delivers items to the Sequence Buffer endpoint.
2.  The first item goes to member 1, the second to member 2, and so on.
3.  Each member outputs through its configured face to the target machine.
4.  With Synchronized Output enabled, all members wait for each other — the entire batch delivers together or not at all.

## The One-Item Rule

If a package contains coal in entry 1 and coal in entry 3, member 1 receives the first coal and member 3 receives the second. The buffer's rule of accepting only one item per member until emptied is what keeps these identical items separate. Without this rule, the two coal would merge and lose their distinct positions.

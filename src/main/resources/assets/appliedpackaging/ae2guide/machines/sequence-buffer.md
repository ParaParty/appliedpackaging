---
navigation:
  parent: machines/index.md
  title: Sequence Buffer
  icon: appliedpackaging:sequence_buffer
  position: 30
item_ids:
- appliedpackaging:sequence_buffer
categories:
- applied packaging machines
---

# The Sequence Buffer

<BlockImage id="appliedpackaging:sequence_buffer" scale="8" />

The Sequence Buffer stores one item type per block and outputs it to a configured adjacent face. Its function is to preserve the order of incoming items — whether from a package or directly from a pattern provider push — and deliver each item to the correct machine face.

Each buffer member accepts only one insertion until fully emptied. This constraint prevents identical items from merging. For example, if a pattern specifies two coal at positions 1 and 3, the buffer keeps them separate so position 1 goes to one face and position 3 to another. Package contents, pattern provider ingredient order, and sparse slot layouts are all preserved by the buffer.

Two or more Sequence Buffers in a straight X, Y, or Z line can form a multiblock. Use an AE2 wrench on the end face to form the structure. The wrenched block becomes the endpoint, which holds the configuration but stores no items. The remaining blocks are storage members, ordered from the endpoint outward. Member 1 receives the first item in sequence, member 2 receives the second, and so on.

A member that finishes outputting pauses briefly before it can accept another item, preventing the same member from being refilled in the same tick.

## Setting It Up

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/sequence_line.snbt" />
  <IsometricCamera yaw="205" pitch="30" />
</GameScene>

1. Place two or more buffer blocks in a straight line.
2. Right-click the end face with an AE2 wrench to form the multiblock.
3. Open the endpoint's GUI to configure all settings for the structure.
4. Set each member's output face in its own side GUI. If no direction is set, the member searches for any adjacent compatible target besides other Sequence Buffers.
5. Place a Package Unpacking Bus against the endpoint face to receive packages.

## Settings

*   **Automatic Output** — each member pushes its item to its output face.
*   **Blocking Mode** — a member waits until its target is empty before outputting.
*   **Synchronized Output** — all members must be able to output before any of them do. If one member cannot, all members wait.
*   **Pattern Mode** — preserves sparse slot positions from the encoded pattern or package layout. When off, items fill members sequentially, skipping empty slots.
*   **Pre-Admission Check** (labeled "Anti-Clog Mode" in the GUI, off by default) — checks that every member can output before accepting a package or pattern push. If any member would fail, the entire input is rejected.
*   **Input Delay** — waits 0 to 100 ticks before accepting or outputting. Manual extraction from the GUI is never delayed.
*   **Input Filter** — up to 9 item or fluid types; leave empty to accept anything.

## Capacity

Each member stores up to 1024 of its item by default. This is server-configurable.

## Upgrades

The Sequence Buffer supports:

*   <ItemLink id="ae2:redstone_card" /> — makes automatic output require a redstone signal at the endpoint.

## Recipe

<RecipeFor id="appliedpackaging:sequence_buffer" />

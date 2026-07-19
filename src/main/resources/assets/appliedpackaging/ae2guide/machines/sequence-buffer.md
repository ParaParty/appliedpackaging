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

The Sequence Buffer stores one item type per block and outputs it to a configured adjacent face. It preserves the order of incoming items and delivers each item to a specific machine face, one position at a time. It works with any source that pushes items in sequence — pattern providers, Package Unpacking Buses, or any other device that supplies a batch.

Each buffer member accepts only one item until it is fully emptied, and will reject anything else in the meantime. This rule is what prevents identical items from merging. When a pattern specifies coal at position 1 and coal at position 3, the buffer keeps them as two separate entries. Position 1 goes to one machine face and position 3 goes to another. Without this constraint, the two coal stacks would combine and lose their distinct positions.

Two or more Sequence Buffers placed in a straight line along the X, Y, or Z axis can be formed into a multiblock. Right-click the end face of the last block with an AE2 wrench. The block you wrenched becomes the endpoint — it holds all configuration settings but does not store any items. The remaining blocks become storage members, numbered in order starting from the endpoint outward. Member 1 receives the first item, member 2 receives the second, and so on down the line.

After a member finishes outputting its item, there is a brief pause before it can accept another. This prevents the same member from being refilled in the same tick it was just emptied.

## Setting Up a Multiblock

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/sequence_line.snbt" />
  <IsometricCamera yaw="205" pitch="30" />
</GameScene>

1. Place at least two Sequence Buffer blocks in a straight line.
2. Right-click the end face of the last block with an AE2 wrench to form the structure.
3. Open the endpoint's GUI to configure all settings. These settings apply to every member in the multiblock.
4. For each member, open its individual side GUI and set its output face. If you do not set a direction, the member searches for any adjacent container it can output to, excluding other Sequence Buffers.
5. Place a Package Unpacking Bus against the endpoint face to feed packages into the buffer. You can also place a pattern provider here to push ingredients directly.

## Settings

The endpoint GUI provides controls for these settings:

*   **Automatic Output** — when enabled, each member automatically pushes its stored item to its configured output face. When disabled, items stay in the members until you extract them manually through the GUI.
*   **Blocking Mode** — when enabled, a member will wait until its output target is completely empty before sending its item. When disabled, the member outputs regardless of what is already in the target.
*   **Synchronized Output** — when enabled, all occupied members must be able to output before any single member will commit. If even one member's output would fail, every member waits. This guarantees the entire batch delivers as one unit or not at all.
*   **Pattern Mode** — when enabled, the buffer uses sparse slot positions from the encoded pattern or package layout. Items are mapped to members based on their recorded positions, preserving any gaps. When disabled, items are assigned to members in dense order, skipping past any empty slots.
*   **Pre-Admission Check** (labeled "Anti-Clog Mode" in the GUI, off by default) — when enabled, the buffer verifies that every assigned member can successfully output before accepting any input. If any member would fail, the entire package or pattern push is rejected.
*   **Input Delay** — adds a delay of 0 to 100 ticks between accepting an item and allowing it to be output. Manual extraction from the GUI is never delayed by this setting.
*   **Input Filter** — accepts up to 9 item or fluid types. When a filter is set, the buffer only accepts items matching one of the listed types. Leave the filter empty to accept anything.

## Capacity

Each member stores up to 1024 of its assigned item by default. This limit can be changed by the server administrator.

## Upgrades

The Sequence Buffer supports one upgrade:

*   <ItemLink id="ae2:redstone_card" /> — when installed in the endpoint, automatic output only occurs while the endpoint is receiving a redstone signal.

## Recipe

<RecipeFor id="appliedpackaging:sequence_buffer" />

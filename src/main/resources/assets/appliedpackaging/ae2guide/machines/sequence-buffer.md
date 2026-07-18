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

A package arrives with coal, iron, and redstone inside — in that careful order. But your machine has three separate input faces, each expecting a different item. How do you split one package across three destinations?

The Sequence Buffer does exactly that.

Each buffer block holds exactly one type of item. Line them up in a row, wrench the end, and they form a multiblock. Package items go in order: entry 1 goes to the first member, entry 2 to the second, entry 3 to the third. Each member outputs through its own configured face. One package, multiple machine faces, all in the right order.

## Single Block vs Multiblock

**A single Sequence Buffer works fine on its own.** It stores one item type and can auto-output through any face you choose. Great for simple cases where you just need to deliver one item to one place.

**A multiblock needs at least two buffers in a straight line** (X, Y, or Z axis). Wrench the end face of the last block to form the structure.

* **The endpoint** (where you used the wrench) holds all the configuration but **stores nothing.**
* **The members** (the other blocks) each store one item type, in order from the endpoint outward.

## Setting It Up

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/sequence_line.snbt" />
  <IsometricCamera yaw="205" pitch="30" />
</GameScene>

1. Place two or more buffers in a straight line.
2. Right-click the far end with an AE2 wrench to form the structure.
3. Open the endpoint's GUI to configure the whole thing.
4. For each member, open its side GUI to pick where it outputs. Undirected members search for any compatible adjacent target.
5. Stick a [Package Unpacking Bus](devices/package-unpacking-bus.md) against the endpoint to feed packages in.

## The Important Settings

### Automatic Output
Turn on and each member pushes its item out automatically. Turn off and items sit there until you pull them out yourself. Simple.

### Blocking Mode
On: a member waits until its output target is completely empty. Off: it'll stuff items in regardless. Same as every other blocking mode you've seen in AE2.

### Synchronized Output
On: **every** occupied member must be able to output before **any** of them do. Think "all or nothing." Off: each member does its own thing independently.

### Pattern Mode
When your package was encoded with empty slots between items (a sparse layout), turn this on. The package's recorded layout decides which slot goes to which member. Leave it off and items fill members densely in order.

### Pre-Admission Check
The GUI calls this "Anti-Clog Mode" (off by default). Turn it on and the buffer checks every assigned member's output feasibility before accepting a package. If any member would fail, the whole package is rejected.

### Input Delay
Adds a gap between accepting items and outputting them. 0 to 100 ticks (0 to 5 seconds). Manual extraction from the GUI is never delayed.

| Setting | What It Does |
|---------|--------------|
| Automatic Output | Members push items automatically |
| Blocking Mode | Wait for empty target before output |
| Synchronized Output | All members output together or none do |
| Pattern Mode | Preserve sparse slot positions |
| Pre-Admission Check | Reject packages that can't fully output |
| Input Delay | Gap before accepting/outputting (0–100 ticks) |
| Input Filter | Up to 9 item types; empty = accept anything |

## Tips

* **Breaking the endpoint dissolves the whole structure.** Breaking a middle member keeps the near segment intact.
* Each member holds up to **1024 items** by default (server admin can change this).
* A **redstone card** on the endpoint makes automatic output require a redstone signal.
* The endpoint has its own GUI and each member has a side GUI. Both show the same real storage — extract from either.
* For furnace setups: member 1 faces the side (fuel), member 2 faces the top (ore), hopper on the bottom pulls the result.

## Recipe

<RecipeFor id="appliedpackaging:sequence_buffer" />

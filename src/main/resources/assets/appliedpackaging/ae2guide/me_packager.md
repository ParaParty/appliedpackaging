---
navigation:
  parent: index.md
  title: ME Packager
  icon: appliedpackaging:me_packager
  position: 40
item_ids:
- appliedpackaging:me_packager
categories:
- applied packaging machines
---

# ME Packager

<BlockImage id="appliedpackaging:me_packager" scale="8" />

The ME Packager connects to AE2 only through its bottom and model back. It never scans adjacent inventories. Packing reads
the connected ME storage, applies color/marker/content settings, and creates one deterministic package. The redstone mode
controls automatic packing; speed cards shorten the real work cycle.

## Network Connections and Interaction

The horizontal facing rotates the model and its back connection together; the bottom connection always remains valid.
Other sides do not connect to ME cable. Use an AE2 wrench to rotate the machine.

Interact with the belt surface to insert or retrieve the one visible package. Interacting with the frame opens the GUI.
The GUI and world interaction operate the same real held slot; neither path creates a hidden staging copy.

## Packing

Configure a marker, color, content filters, and activation mode, then trigger one packing operation manually or with
redstone. Packing extracts from connected ME storage only after capacity and extraction simulation succeeds. If the
machine expands an input package, package contents are flattened in place—packages never truly contain other packages.

Content filters and the marker filter are independent. An inverter card reverses content filtering, not marker matching.

## Configuration Reference

| Setting | Effect |
| --- | --- |
| Color | Selects the output package color unless an applicable filter supplies one. |
| Retain marker | Keeps a compatible marker inherited from source packages; conflicting inherited markers reject the plan. |
| Override marker | Uses the marker fake slot, or the applicable filter marker when that slot is empty. |
| Clear marker | Produces a package without an inherited marker. |
| Filter both / packing only / unpacking only | Chooses which operation uses the content filter. Packing-only mode rejects package input because unpacking is disabled. |
| High / low / always / pulse / off | Packs while powered, while unpowered, continuously, once per rising pulse, or only by manual action. |

## Unpacking, Blocking, and Anti-Clog

Inserting a package starts unpacking into the same ME storage. Existing **blocking mode** can forbid unpacking while the
network contains visible resources. **Anti-clog mode** is a separate input rule: when enabled (the default), the package is
accepted only if the complete contents pass the current automatic-output rules, including blocking mode and capacity.
When disabled, a valid package may wait in the visible held slot and is retried without losing its contents.

Blocking mode is part of the real automatic-unpack rule. Therefore, with anti-clog enabled, a blocking failure means the
machine cannot accept the package. With anti-clog disabled, the same package is accepted into the held slot and waits.
Removing it through the GUI is always allowed while waiting.

The final unpack operation rechecks the complete target capacity. A failed retry never inserts part of the contents.

The belt surface accepts or returns the held package; interacting with the frame opens the GUI.

## Upgrades

* <ItemLink id="ae2:speed_card" /> shortens packing and unpacking work.
* <ItemLink id="ae2:capacity_card" /> unlocks one filter row: two rows are available initially and three cards reach five.
* <ItemLink id="ae2:fuzzy_card" /> and <ItemLink id="ae2:inverter_card" /> change content matching.

The separate storage-component slot accepts 16k, 64k, or 256k components. It does not accept a 1k component, a completed
storage cell, or a portable cell.

## Recipe

<RecipeFor id="appliedpackaging:me_packager" />

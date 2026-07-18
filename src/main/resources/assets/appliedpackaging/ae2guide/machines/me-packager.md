---
navigation:
  parent: machines/index.md
  title: ME Packager
  icon: appliedpackaging:me_packager
  position: 20
item_ids:
- appliedpackaging:me_packager
categories:
- applied packaging machines
---

# The ME Packager

<BlockImage id="appliedpackaging:me_packager" scale="8" />

Don't want to mess with patterns? The ME Packager pulls items straight from your ME network and packages them — no patterns needed. It can also unpack packages back into network storage.

Think of it as a configurable pack/unpack station that reads from and writes to your ME system directly. Set up some filters, choose a color, and it chugs along.

## Connecting It

**The packager only connects to ME cable through its bottom and its back face.** The front (where you see the belt) and the sides don't connect at all. Use an AE2 wrench to rotate it — the front and back turn together, but the bottom always stays connected.

## The Belt

The belt on the front holds exactly one package. You interact with it two ways:

* **Right-click the belt surface** — Put a package in or take one out. This is for manual interaction.
* **Right-click anywhere else** — Open the configuration GUI.

The belt slot and the GUI's held-package slot are the same storage. There's no hidden inventory. You can also push and pull from the belt with hoppers and item pipes.

## Packing Mode

1. Open the GUI. Pick an output color and optionally a marker item.
2. Add content filters if you only want specific items. You get 2 filter rows to start; each <ItemLink id="ae2:capacity_card" /> adds one more (up to 3 cards = 5 rows).
3. Choose how to trigger packing:

| Mode | What It Does |
|------|-------------|
| Pack with redstone signal | Packs while receiving redstone power |
| Pack without redstone signal | Packs while NOT receiving redstone power |
| Always pack | Runs continuously |
| Pack once on redstone pulse | One cycle per rising edge |
| Packing off | Manual only — click the button in the GUI |

When packing fires, the packager scans ME storage for items matching your filters and creates one package.

## Unpacking Mode

Drop a package into the packager (belt or GUI) and it unpacks the contents into ME storage.

* **Blocking mode** blocks unpacking if the network already contains items that match what's in the package.
* **The pre-admission check** (labeled "Anti-Clog Mode" in the GUI, on by default) checks that everything fits *before* taking the package. Turn it off to let a package wait in the belt slot until conditions are right.

## Filter Modes

Decide when your content filters actually matter:

* **Filter packing and unpacking** — Filters apply both ways. Most restrictive.
* **Filter packing only** — Filters only affect what gets packed. Any package can be unpacked.
* **Filter unpacking only** — Filters only affect what gets unpacked. Any items can be packed.

## Upgrades

* <ItemLink id="ae2:speed_card" /> — Faster packing and unpacking (up to 6)
* <ItemLink id="ae2:capacity_card" /> — More filter rows (up to 3, for 5 total)
* <ItemLink id="ae2:inverter_card" /> — Invert content filtering

The packager also has a storage component slot for 16k/64k/256k components to increase package capacity.

## Recipe

<RecipeFor id="appliedpackaging:me_packager" />

---
navigation:
  title: Applied Packaging
  icon: appliedpackaging:fluix_package
  position: 500
---

# Applied Packaging

Some machines don't care where items land. A furnace does. It wants coal in the side and ore in the top, in that order, every time — and normal AE2 patterns just dump everything into the first available slot.

Applied Packaging fixes this with **packages** — real, stackable items that bundle multiple item types together in the exact order you specify. They come in 17 colors so you can color-code different production lines, and they travel through your ME network just like any other item. Once they reach their destination, they unpack in order.

If you've ever wished AE2's autocrafting knew about slot positions and item order, this is the mod for you.

## Getting Started

* [Getting Started](packaging-concepts/getting-started.md) — Your first package, step by step

## Core Concepts

* [Packages](packaging-concepts/packages.md) — What packages are, what they can hold, and how to use them

## Devices

Cable subparts that connect directly to your ME network. They all need a channel.

* [Advanced Pattern Encoding Terminal](devices/advanced-pattern-terminal.md)
* [Package Storage Bus](devices/package-storage-bus.md)
* [Package Unpacking Bus](devices/package-unpacking-bus.md)

## Machines

Full-block machines for making, packing, and routing packages.

* [ME Package Assembler](machines/package-assembler.md)
* [ME Packager](machines/me-packager.md)
* [Sequence Buffer](machines/sequence-buffer.md)

## More

* [Example Setups](example-setups/index.md) — Annotated builds you can copy directly into your world
* [Troubleshooting](troubleshooting.md) — When things don't go as planned

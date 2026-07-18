---
navigation:
  parent: index.md
  title: Troubleshooting
  icon: appliedpackaging:me_packager
  position: 70
---

# Troubleshooting

Stuff not working? Here are the most common problems and how to fix them.

## My package won't go into the Unpacking Bus

**Check your filters first.** An empty or mismatched filter will reject the package silently.

**Check the pre-admission check.** It's on by default (the GUI calls it "Anti-Clog Mode"). It blocks a package if:
* The target inventory is full
* Blocking mode is on and the target already has matching items

To fix: clear space in the target, turn off blocking mode, or turn off the pre-admission check to let the package wait inside the bus. You can always retrieve a waiting package from the GUI.

## My package went to storage instead of being unpacked

This is a **priority issue.** The network delivers to the highest-priority accepting destination. Give your Unpacking Bus a higher number than your Storage Bus.

At equal priority, Unpacking Buses are preferred — unless they reject the package (full target, filter mismatch, pre-admission check fails). If that happens, the Storage Bus is next in line.

## The Sequence Buffer order is wrong

**Member order starts from the block after the endpoint** and goes to the tail. The endpoint itself holds nothing.

**Check Pattern Mode.** If your package has empty slots between items and Pattern Mode is off, items fill members with no gaps. Turn Pattern Mode on to preserve the sparse layout.

**Each member holds one type at a time.** If member 1 already has coal and the package wants to send more coal to member 1, the whole insertion fails. Empty member 1 first.

## The Package Assembler won't start

* **Is the pattern too big?** Hover over the component slot to see the current capacity. Install a larger component or reduce the pattern.
* **Are ingredients available?** For provider pushes they need to be in ME storage. For local mode they need to be in the GUI slots.
* **Are outputs blocking it?** Extract completed packages from the output, or make sure the output target is empty (especially with blocking mode on).
* **Oversized patterns** stay visible but their inputs are locked. Swap in a bigger component to fix.

## The ME Packager can't find anything to pack

The packager scans ME storage using your filters. If no items match, it says so. Try:
* Removing some content filters
* Checking that the items actually exist in ME storage
* Verifying the packager is connected (bottom or back face to ME cable)

## The packager rejects my package for unpacking

* **Pre-admission check is on** and the network is full. Make room or turn it off.
* **Filter mode** might be set to "packing only." Switch to "both" or "unpacking only."
* **Blocking mode** is on and the network already contains matching items. Turn it off or clear those items.

## A cable subpart has no power

Terminals and buses are cable subparts — they need:
* Power (check with a network tool)
* An available channel (8 max without a controller)
* An actual ME cable (colored cables can block connections between different colors)

## I broke a Sequence Buffer and my structure fell apart

Breaking the endpoint dissolves everything. Breaking a middle member keeps the segment closest to the endpoint intact but dissolves the rest. Just place the blocks back and wrench the endpoint to re-form.

Still stuck? Go back through the [Getting Started](packaging-concepts/getting-started.md) guide or look at the [Example Setups](example-setups/index.md) for known-good reference builds.

---
navigation:
  parent: index.md
  title: Troubleshooting
  icon: appliedpackaging:me_packager
  position: 70
---

# Troubleshooting

## A package goes to the wrong bus

Check AE2 storage priority first. The highest-priority accepting storage receives the package. At equal priority, an available Package Unpacking Bus is preferred over a Package Storage Bus. If a bus rejects the package — because its filter does not match, its held slot is already occupied, blocking mode is active, or the target is full — routing continues to the next destination.

To fix this, raise the priority of the bus you want the package to go to, or lower the priority of the bus that is currently receiving it. Use a <ItemLink id="ae2:network_tool" /> to inspect the current priorities in your network.

## Input is rejected immediately

There are two kinds of rejection. The first is a permanent rule: an invalid package, a color or marker mismatch, a content filter mismatch, an occupied held slot, or an occupied Sequence Buffer member will always be rejected regardless of any other settings.

The second kind of rejection comes from pre-admission checking, labeled "Anti-Clog Mode" in the GUI. This is on by default on the Package Unpacking Bus and the ME Packager. When enabled, the device checks whether the complete output can actually be delivered before accepting the input. If the target is missing, capacity is insufficient, or blocking mode is active, the input is rejected before it changes any storage.

If you want a package to wait inside the device instead of being rejected, disable pre-admission checking on that device. This does not bypass any other rules — filters and final validation still apply. It simply allows a valid input to sit in local storage until conditions improve.

## A package is waiting inside a device

This happens when pre-admission checking is off and the output target is not currently available. Once you restore the target, free up capacity, or turn off blocking mode, the device retries automatically. The waiting package can always be retrieved from the GUI.

## Sequence Buffer order or slots are wrong

Member order starts from the first block after the endpoint and follows the structure line to the tail. The endpoint block stores nothing itself — it only holds the configuration.

If your package or pattern was encoded with empty slots between items, turn Pattern Mode on. Leave it off if you want items to fill members in dense sequential order with no gaps.

Each member accepts only one item at a time and will not accept another until the first one is fully output. If member 1 already holds coal and a new input also has coal in the first position, the entire insertion is rejected. This is the intended behavior: it prevents identical items from merging together, which would cause them to lose their distinct slot positions.

After a member finishes outputting, there is a brief pause before it can accept the next item. This prevents a member from being refilled in the same tick it was just emptied.

## The Package Assembler does not start

If the pattern is too large for the current capacity, the pattern remains visible in the slot but the assembler will not begin. Install a supported storage component — 16k, 64k, or 256k — or reduce the size of the pattern. Hover over the component slot to see the current capacity limit.

Verify that all required ingredients are available. For pattern provider pushes, they must be in ME storage. For local mode, they must be placed in the GUI input slots.

If completed packages are already sitting in the output, extract them. With blocking mode enabled, the output target must be completely empty before a new batch will start.

## The ME Packager finds no packable contents

The packager scans ME storage based on your configured content filters. If nothing matches, check that the filters are not overly restrictive, confirm that the items actually exist in ME storage, and verify that the packager has a valid network connection through its bottom or back face.

## A cable subpart has no power

Terminals and buses are cable subparts and require power, a channel, and a valid ME cable connection. Check with a <ItemLink id="ae2:network_tool" /> that the cable has power and an available channel. Colored cables of different colors will not connect to each other, which can break what visually looks like a single cable run.

If you are still unable to resolve the issue, return to the [Getting Started](packaging-concepts/getting-started.md) guide and work through each step again. The [Example Setups](example-setups/index.md) also provide verified reference builds that you can compare against your own setup.

---
navigation:
  parent: index.md
  title: Troubleshooting
  icon: appliedpackaging:me_packager
  position: 70
---

# Troubleshooting

## A Package Goes to the Wrong Bus

Check AE2 storage priority first. The highest-priority accepting storage receives the package. At equal priority, an available Unpacking Bus is preferred over a Package Storage Bus. If a bus rejects the package (filter mismatch, occupied held slot, blocking mode active, or capacity full), routing continues to the next destination.

To fix: raise the priority of the intended destination bus, or lower the priority of the bus currently receiving the package. Use a <ItemLink id="ae2:network_tool" /> to inspect current priorities.

## Input Is Rejected Immediately

An invalid package, color/marker/content filter mismatch, occupied held slot, or occupied Sequence Buffer member is always rejected.

With pre-admission checking enabled (labeled "Anti-Clog Mode," on by default on the Unpacking Bus and Packager), a missing target, insufficient capacity, or active blocking rule also rejects the insertion before it changes storage.

To allow intentional waiting, disable pre-admission checking on the receiving device. This does not disable filters or final validation; it only permits an otherwise valid input to enter local storage and wait.

## A Package Is Waiting Inside a Device

This is expected when pre-admission checking is disabled and the output target is not currently available. Restore the target, free capacity, or disable blocking and the device will retry. The package remains extractable from the GUI at any time.

## Sequence Buffer Order or Slots Are Wrong

Member order starts from the block after the endpoint and follows the structure direction to the tail. The endpoint itself stores nothing.

Use Pattern Mode when a package or pattern contains sparse slot positions. Leave it off for dense sequential order.

Each member accepts only one insertion until emptied. If member 1 already has coal and a new input also has coal in the first position, the entire insertion fails. This is the intended behavior — it prevents identical items from merging, preserving their distinct positions.

A member that finishes outputting pauses briefly before it can accept another item, preventing immediate refill in the same tick.

## The Package Assembler Does Not Start

An oversized pattern remains visible but locks its input slots. Install a supported storage component (16k, 64k, or 256k) or reduce the pattern size. Verify that all required ingredients are available in ME storage.

If completed packages are sitting in the output, extract them. With blocking mode on, the output target must be empty before a new batch starts.

## The ME Packager Cannot Find Packable Contents

The packager scans ME storage using your configured filters. Check the filters, confirm the items are in ME storage, and verify the packager is connected (bottom or back face to an ME cable).

## Cable Subpart Has No Power

Terminals and buses are cable subparts. Verify the cable has power, a channel is available, and the cable connection is not blocked by a color mismatch.

Still stuck? Return to the [Getting Started](packaging-concepts/getting-started.md) guide or check the [Example Setups](example-setups/index.md) for verified reference builds.

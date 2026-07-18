---
navigation:
  parent: index.md
  title: Troubleshooting
  icon: appliedpackaging:me_packager
  position: 80
---

# Troubleshooting

## Input Is Rejected Immediately

First distinguish a constant input rule from an output-feasibility rule. An invalid package, color/marker/content filter
mismatch, occupied held slot, or occupied Sequence Buffer member is always rejected. With anti-clog enabled, a missing
target, insufficient full-batch capacity, or active blocking rule also rejects the insertion before it changes storage.

To allow intentional waiting, disable anti-clog on the receiving ME Packager, Package Unpacking Bus, or Sequence Buffer.
This does not disable filters or final validation; it only permits an otherwise legal input to enter real local storage.

## A Package Is Waiting Inside a Device

This is expected when anti-clog is disabled and the real output rule is not currently satisfied. Restore the target,
capacity, or blocking condition and the device will retry. The ME Packager belt/GUI and the Unpacking Bus working slot
expose the same held package, so it can be removed intact instead. A completed Package Assembler batch is likewise a real
ordered list and remains extractable while automatic output is blocked.

## A Package Reaches the Wrong Bus

Check AE2 storage priority first. The higher-priority accepting storage receives the package. At equal priority, an
available Unpacking Bus is preferred over a Package Storage Bus; rejection by filter, held state, blocking, or capacity
allows routing to continue. A waiting package is reported as real storage, so inspect and extract it before changing a
large routing network.

## Sequence Order or Slots Look Wrong

Member order starts after the endpoint and follows the structure direction to the tail. The endpoint itself stores
nothing. Use pattern mode when a package contains recorded sparse positions; leave it off for dense entry order. Remember
that each member accepts one insertion only and, after becoming empty in tick `t`, cannot accept the next input until
`t + 1`. Synchronized output coordinates commits, while anti-clog controls admission; neither setting changes member order.

## The Package Assembler Does Not Start

An oversized local pattern remains visible but locks its inputs. Install a supported 16k, 64k, or 256k component or reduce
the planned package. Also verify that every real position-filtered input is present. Temporarily removing a material pauses
the plan; changing the pattern cancels it. If completed packages already remain, extract them or restore the selected
automatic-output target.

Return to [Example Setups](example_setups.md) after correcting the failing rule.

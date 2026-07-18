---
navigation:
  parent: index.md
  title: Sequence Buffer
  icon: appliedpackaging:sequence_buffer
  position: 60
item_ids:
- appliedpackaging:sequence_buffer
categories:
- applied packaging machines
---

# Sequence Buffer

<BlockImage id="appliedpackaging:sequence_buffer" scale="8" />

A single buffer latches one AE resource key and amount. It accepts no second input until fully emptied, and a buffer emptied
in game tick `t` reopens at `t + 1`. Item, fluid, and ME capabilities all use this same real state.

## Input and Storage

One member stores one AE key from one real insertion. Once occupied, it rejects every later insertion, including the same
key; the original insertion itself may accept up to the server-configured capacity (1024 by default). Item, fluid, and
generic ME transfers all share this latch. The one-tick reopen rule prevents a buffer emptied by automatic output from
receiving a replacement batch again in the same tick.

The main GUI maps the ordered members of a formed structure. A side GUI maps the clicked physical block. Both expose the
same real storage, and players may extract from the GUI even when automatic-output delay, blocking, or synchronization is
active. Only the endpoint main GUI edits configuration. A formed member side GUI only offers a jump to the main block;
an unformed standalone block has neither configuration nor navigation buttons.

## Multiblock Ownership

Two or more buffers form a straight X/Y/Z multiblock. The endpoint owns configuration and ticks every member in stable
order; the endpoint itself stores nothing. Repeated wrench interaction on one face cycles the direction opposite the
clicked face, the clicked face itself, and then no direction.

Only a straight line along one axis can form. The endpoint owns configuration and performs all member ticks; formed member
blocks do not tick independently. Extending the tail preserves stable member order. Breaking the endpoint dissolves the
structure; breaking the middle retains only the endpoint-side contiguous segment.

## Output Scheduling

Formed-multiblock automatic output can be independent or synchronized. Blocking mode requires the output target to be
empty. Input delay can be zero; redstone cards gate automatic output. Synchronized output, pattern mode, and input delay
only apply while the multiblock is formed; standalone blocks apply no input delay. GUI extraction is deliberately
unaffected by delay, blocking, or sync.

Each directed member outputs only through its configured face. An undirected member searches compatible adjacent targets
other than Sequence Buffers. Synchronized output requires every occupied member to find a complete target before any
member commits; independent mode lets each member proceed separately.

Input delay and redstone state schedule automatic output. They are not anti-clog acceptance conditions: otherwise a valid
input could never enter a deliberately delayed buffer.

## Configuration Reference

| Setting | Effect |
| --- | --- |
| Automatic output | Enables member-to-target output attempts. |
| Blocking | Requires the selected target to be completely empty before automatic output. |
| Synchronized output | Preflights every occupied member before any member commits. |
| Pattern mode | Uses a package's recorded sparse positions instead of dense entry order. |
| Anti-clog | Requires complete real output feasibility before accepting ordinary or atomic input. |
| Input delay | Delays capability extraction and automatic output; GUI extraction remains allowed. Presets are 0, 1, 5, 10, 20, 40, and 100 ticks. |
| Input filter | Up to nine AE resource keys; an empty filter accepts any supported key. |

A redstone card makes automatic output require a signal at the endpoint. Formed members read endpoint authority live and
ignore their own local configuration. A member GUI cannot modify settings and only navigates to the endpoint main GUI.

## Pattern Mode and Anti-Clog

Anti-clog mode defaults off. When enabled, every ordinary or atomic pattern/package input is accepted only if each assigned
member can completely output through its real direction, target, blocking, and capacity rules. Pattern mode preserves known
sparse positions; advanced-pattern direct pushes remain dense by design.

For package input, the Unpacking Bus constructs one atomic assignment plan. With pattern mode and a recorded layout, sparse
positions select corresponding members; without it, entries fill members densely in order. If any assigned member is busy,
filtered, too small, or fails anti-clog output preflight, no member is changed.

## Forming a Sequence

Place at least two buffers in one straight X, Y, or Z line. Use an AE2 wrench on an endpoint face to cycle the output
direction and form the structure. The endpoint owns configuration; the remaining blocks are ordered storage members.

## Recipe

<RecipeFor id="appliedpackaging:sequence_buffer" />

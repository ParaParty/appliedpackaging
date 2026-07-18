---
navigation:
  parent: index.md
  title: ME Package Assembler
  icon: appliedpackaging:package_assembler
  position: 30
item_ids:
- appliedpackaging:package_assembler
categories:
- applied packaging machines
---

# ME Package Assembler

<BlockImage id="appliedpackaging:package_assembler" scale="8" />

The assembler executes any AE2-decodable encoded pattern. Package and advanced patterns retain their special color,
marker, order, and sparse layout; ordinary crafting or processing patterns use the assembler's selected color and marker.

## Accepted Patterns and Capacity

The local pattern slot accepts encoded patterns that AE2 can decode. Package patterns produce one configured package;
advanced patterns produce one package for each enabled column; ordinary crafting and processing patterns produce one
package using the pattern's ordered inputs, the selected machine color, and either the configured marker or the primary
output when the marker slot is empty.

Before any input is consumed, every package in the plan must fit the active capacity profile. With no component installed,
the limit is 9 units and 9 resource types. AE2 16k, 64k, or 256k storage components select larger profiles. An oversized
pattern stays visible for diagnosis, but its input slots remain locked.

## Two Input Paths

It accepts Pattern Provider pushes atomically and also exposes real, position-filtered local input slots. Materials remain
available to the player until the progress bar finishes. Changing the pattern cancels the current local plan; temporarily
removing an ingredient pauses it.

Pattern Provider pushes are atomic: the complete batch and all output packages are validated before the provider's input
counter is consumed. Local mode exposes real position-filtered slots. Ingredients are consumed only when progress reaches
100% and the entire plan still matches.

The local GUI shows a 4x4 window over the actual expected input positions. Empty sparse positions are skipped; scrolling
opens only when more real inputs exist beyond the visible window.

## Color, Marker, and Capacity Controls

The color picker offers Fluix plus all 16 dye colors. With no local pattern or with an ordinary pattern, it displays and
edits the persistent machine selection. A package pattern temporarily displays its encoded color. An advanced pattern
displays its single shared column color, or **None** when its columns use different colors. While a package or advanced
pattern is inserted, the color button rejects clicks and the temporary display never writes the machine configuration.
Removing the pattern restores the previous machine selection. Pattern Provider pushes use the same effective-color
display while their batch is active.

The marker slot is a non-consuming configuration slot. It overrides an ordinary pattern's primary-output marker, but does
not override marker metadata in package or advanced patterns. Hovering the capacity component area explains the component
and shows the active unit/type limits.

While a batch is working, color, marker, output mode, and blocking mode cannot be changed. Their hover tooltips remain
available. The real material slots and completed ordered output remain player-accessible.

## Automatic Output

Automatic output targets either the connected ME network or one selected adjacent container. Blocking mode checks a new
batch once, then a successful batch drains continuously in the same tick as far as the target accepts it. The GUI always
operates on the same real ordered output list.

**ME Network** writes directly to the assembler's connected ME storage. **Adjacent Block** chooses one accepting container
from the six neighboring positions and keeps that direction for the rest of the batch; the assembler has no special back
face for container output. **Disabled** leaves completed packages in the real output list for GUI or capability extraction.

Blocking mode looks for an existing item of any package type in the new batch. Unrelated items do not block it. A batch
is admitted once blocking passes and the real head package can be inserted in full; later packages are not preflighted.
It then drains in order without rechecking blocking. If the target refuses the current head, the remaining real list stays
available to the GUI.

Changing the configured output mode does not duplicate or replace a package already in that list. Player extraction is
allowed between automatic output attempts because the GUI and automation share the same ordered storage.

## Comparator States

The comparator output is 0 while idle, 1 while packaging, and 2 while completed packages remain. It represents lifecycle,
not package count.

Its main automation partner is an AE2 <ItemLink id="ae2:pattern_provider" />. Speed cards shorten the work cycle, while
16k, 64k, and 256k storage components select the shared package capacity profile.

## Recipe

<RecipeFor id="appliedpackaging:package_assembler" />

---
navigation:
  parent: index.md
  title: Packaging Workflow
  icon: appliedpackaging:package_pattern
  position: 5
---

# Packaging Workflow

1. Encode a package or advanced pattern in the [Advanced Pattern Encoding Terminal](advanced_pattern_terminal.md).
2. Put that pattern in a Pattern Provider facing an [ME Package Assembler](package_assembler.md), or install it locally.
3. Route completed packages through normal ME storage. A [Package Storage Bus](package_buses.md) stores them as packages;
   a Package Unpacking Bus consumes one routed package and commits all contents together.
4. Use a [Sequence Buffer](sequence_buffer.md) when downstream machines require stable ordered or sparse-position inputs.

For jam-resistant routing, enable anti-clog on the receiving device. It does not replace blocking mode: it promotes the
device's complete current automatic-output preflight into an input admission rule. Disable it when intentional local waiting
is part of the design.

See [Example Setups](example_setups.md) for small annotated layouts and a blocking/anti-clog decision table.
If a package is rejected, waits, or reaches the wrong destination, continue with [Troubleshooting](troubleshooting.md).

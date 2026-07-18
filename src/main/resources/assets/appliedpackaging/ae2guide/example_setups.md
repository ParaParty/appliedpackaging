---
navigation:
  parent: index.md
  title: Example Setups
  icon: appliedpackaging:sequence_buffer
  position: 70
---

# Example Setups

These layouts explain component roles. They are deliberately small; add normal AE2 storage, crafting CPUs, channels, and
machine-specific input faces as required by your network.

## Pattern Provider to Package Assembler

<GameScene zoom="6" background="transparent">
  <ImportStructure src="assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="25" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="2 1 2">
    (1) The powered Pattern Provider pushes one complete pattern batch.
  </BoxAnnotation>
  <BoxAnnotation color="#cc88ff" min="2 0 1" max="3 1 2">
    (2) The ME Package Assembler validates capacity before consuming any input.
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="3 0 1" max="4 1 2">
    (3) Adjacent-container output keeps the ordered package list real and extractable.
  </BoxAnnotation>
</GameScene>

Install the encoded [package or advanced pattern](advanced_pattern_terminal.md) in the Pattern Provider or in the
assembler's local pattern slot. For ME-network output, connect the assembler and select ME Network instead of using the
illustrated chest. Blocking is checked once for each newly completed batch; after admission, the batch drains continuously.

## Storing Packages or Unpacking Them

<GameScene zoom="6" background="transparent">
  <ImportStructure src="assets/assemblies/package_routing.snbt" />
  <IsometricCamera yaw="195" pitch="25" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="2 1 2">
    (1) The Package Storage Bus exposes real packages from the left inventory.
  </BoxAnnotation>
  <BoxAnnotation color="#ffbb55" min="3 0 1" max="5 1 2">
    (2) The Package Unpacking Bus accepts a routed package and atomically fills the right target.
  </BoxAnnotation>
</GameScene>

Use priorities to decide which destination receives a package first. With anti-clog enabled, the Unpacking Bus rejects the
network insertion when its complete target preflight fails, allowing AE2 to try another destination. Disable anti-clog when
one visible held package is intentionally allowed to wait. The held package remains extractable until final commit.

## Ordered Inputs with a Sequence Buffer

<GameScene zoom="6" background="transparent">
  <ImportStructure src="assets/assemblies/sequence_line.snbt" />
  <IsometricCamera yaw="205" pitch="30" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="1 1 2">
    (1) The endpoint owns configuration and never stores an input.
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="1 0 1" max="4 1 2">
    (2) Members latch entries in stable order and output through their own configured faces.
  </BoxAnnotation>
</GameScene>

Put the [Package Unpacking Bus](package_buses.md) against the endpoint to insert a complete package atomically. Enable
pattern mode when recorded sparse positions must be preserved. Synchronized output makes every occupied member pass its
preflight before any member commits; anti-clog moves that same output feasibility check to input admission.

## Choosing Blocking and Anti-Clog

* **Blocking off, anti-clog off:** accept valid input and wait for capacity if necessary.
* **Blocking on, anti-clog off:** accept valid input, then wait until the target satisfies blocking.
* **Blocking off, anti-clog on:** reject input unless the full output fits now.
* **Blocking on, anti-clog on:** reject input unless the target also passes the machine's real blocking rule.

Anti-clog never replaces final commit validation. It determines whether a new input may enter; automatic output still
rechecks its normal target rules before committing.

"Blocking" is device-specific: the ME Packager checks whether its ME target has visible contents, the Unpacking Bus checks
for any package input type already present in its target, and the Sequence Buffer requires its selected target to be empty.
Anti-clog always incorporates the real blocking rule of the receiving device.

For symptom-driven diagnosis and safe recovery, see [Troubleshooting](troubleshooting.md).

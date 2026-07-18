---
navigation:
  parent: example-setups/index.md
  title: Ordered Machine Inputs
  icon: appliedpackaging:package_unpacking_bus
  position: 20
---

# Ordered Machine Inputs

A furnace wants coal in the side and ore in the top. A brewing stand wants each ingredient in a specific slot. These machines care about *which item goes where* — and that's exactly what packages are good at.

This setup uses a Package Unpacking Bus with a Sequence Buffer to split one package across multiple machine faces.

## The Setup

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/sequence_line.snbt" />
  <IsometricCamera yaw="205" pitch="30" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="1 1 2">
    (1) Endpoint: Owns the configuration. Put the Unpacking Bus against this face.
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="1 0 1" max="4 1 2">
    (2) Members: Each holds one item type. Configure each one's output face to point at the machine.
  </BoxAnnotation>
</GameScene>

## For a Furnace

The scene above shows the generic layout. For a furnace specifically:

* Place the Unpacking Bus against the endpoint (1).
* Member 1 → furnace side face (coal / fuel slot)
* Member 2 → furnace top face (ore / input slot)
* Place a hopper under the furnace to extract the smelted result.

## Configurations

* The <ItemLink id="appliedpackaging:package_unpacking_bus" /> (placed on the endpoint) needs no filter unless you want to restrict which packages arrive. Make sure the pre-admission check is on (it is by default).
* The endpoint has Automatic Output enabled. Enable Pattern Mode if your package uses sparse slot positions.
* Each member has its output face set to point at the correct spot on the target machine.

## How It Works

1. A package enters the ME network and is routed to the Unpacking Bus.
2. The Unpacking Bus feeds the package's contents into the Sequence Buffer endpoint.
3. Package entry 1 → member 1, entry 2 → member 2, and so on.
4. Each member outputs through its configured face to the machine.
5. With Synchronized Output on, all members wait for each other — no partial deliveries.

## Tips

* Turn on **Synchronized Output** and the **Pre-Admission Check** together: the package won't even enter the buffer unless every member can output successfully. This gives you fully atomic delivery.
* Add **Input Delay** if the machine needs a moment between batches.
* For furnace-style machines, make sure to encode the fuel item in the **first** slot — it'll go to the member closest to the endpoint.

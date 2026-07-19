---
navigation:
  parent: example-setups/ordered-machine-inputs.md
  title: "Example 4: Routing"
  icon: appliedpackaging:package_storage_bus
  position: 40
---

# Example 4: Routing

Connect four Package Storage Buses to the same orange routing network and filter them for red, blue, green, and yellow. Each bus is attached to an ME Packager whose color selection matches the bus. The pink cable on the other side of each packager connects to a separate destination network.

<GameScene zoom="2.75" background="transparent">
  <ImportStructure src="../../assets/assemblies/packager_color_routing.snbt" />
  <IsometricCamera yaw="200" pitch="55" />

  <BoxAnnotation color="#dd8833" min="3 0 0" max="10 1 6">
    (1) Orange routing network: Connect its top stub to the package source.
  </BoxAnnotation>
  <BoxAnnotation color="#b02e26" min="2 0 1" max="4 1 2">
    (2) Red Storage Bus + red ME Packager.
  </BoxAnnotation>
  <BoxAnnotation color="#3c44aa" min="2 0 5" max="4 1 6">
    (3) Blue Storage Bus + blue ME Packager.
  </BoxAnnotation>
  <BoxAnnotation color="#5e7c16" min="9 0 1" max="11 1 2">
    (4) Green Storage Bus + green ME Packager.
  </BoxAnnotation>
  <BoxAnnotation color="#fed83d" min="9 0 5" max="11 1 6">
    (5) Yellow Storage Bus + yellow ME Packager.
  </BoxAnnotation>
</GameScene>

When a package enters the orange network, AE2 only tries Package Storage Buses whose color filter matches it. The bus inserts the package into its matching packager, which unpacks the contents into the outer pink network. The bus and packager must use the same color. For strict routing, do not leave the packager on the default Fluix no-color filter.

The four pink cable stubs represent four separate destination networks and are not connected to each other. In the real build, connect each one to its production line and connect the top stub of the orange network to the package source.

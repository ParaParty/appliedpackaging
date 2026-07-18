---
navigation:
  parent: index.md
  title: Packages
  icon: appliedpackaging:fluix_package
  position: 10
item_ids:
- appliedpackaging:fluix_package
- appliedpackaging:white_package
- appliedpackaging:orange_package
- appliedpackaging:magenta_package
- appliedpackaging:light_blue_package
- appliedpackaging:yellow_package
- appliedpackaging:lime_package
- appliedpackaging:pink_package
- appliedpackaging:gray_package
- appliedpackaging:light_gray_package
- appliedpackaging:cyan_package
- appliedpackaging:purple_package
- appliedpackaging:blue_package
- appliedpackaging:brown_package
- appliedpackaging:green_package
- appliedpackaging:red_package
- appliedpackaging:black_package
categories:
- applied packaging items
---

# Packages

<Row gap="8">
  <ItemImage id="appliedpackaging:fluix_package" scale="3" />
  <ItemImage id="appliedpackaging:red_package" scale="3" />
  <ItemImage id="appliedpackaging:green_package" scale="3" />
  <ItemImage id="appliedpackaging:blue_package" scale="3" />
  <ItemImage id="appliedpackaging:black_package" scale="3" />
</Row>

Packages are real items, not views of loose ME inventory. Their contents are an ordered list; repeated resource keys stay
as separate entries. Color, marker, order, and sparse slot layout are part of package identity and stacking.

Packages cannot contain packages. When a package is used as an input to packaging, its contents are expanded in place.
Sneak-use a held package stack to unpack it, or use an [ME Packager](me_packager.md) or
[Package Unpacking Bus](package_buses.md) for automation.

The basic capacity is 9 units and 9 types. An AE2 16k, 64k, or 256k storage component in a supported machine selects a
larger shared capacity profile. Storage cells and 1k components are not valid upgrades.

See [Packaging Workflow](workflow.md) for the route from encoded pattern to an ordered downstream input.

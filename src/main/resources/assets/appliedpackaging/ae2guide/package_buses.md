---
navigation:
  parent: index.md
  title: Package Buses
  icon: appliedpackaging:package_unpacking_bus
  position: 50
item_ids:
- appliedpackaging:package_storage_bus
- appliedpackaging:package_unpacking_bus
categories:
- applied packaging devices
---

# Package Buses

## Package Storage Bus

<GameScene zoom="8" background="transparent">
  <ImportStructure src="assets/blocks/package_storage_bus.snbt" />
</GameScene>

The **Package Storage Bus** exposes only legal package items in the adjacent inventory. It never exposes package contents
as loose ME resources. Partitioning reads adjacent packages into color, marker, and content filter rows.

Each enabled row can combine color, marker, and up to six content filters. Rows are alternatives: a package matching any
enabled row is accepted. Two rows are available initially; each of up to five capacity cards adds one row, reaching seven.
Fuzzy and inverter cards expose their corresponding per-row controls. A row with no selected color does not filter by
color.

It follows the priority and storage-filter model of AE2's <ItemLink id="ae2:storage_bus" />, but packages remain opaque.

Partitioning walks the adjacent inventory in slot order and creates one complete row per distinct legal package. Loose
items are ignored. If the target contains no packages, partitioning clears the package filters.

## Package Unpacking Bus

<GameScene zoom="8" background="transparent">
  <ImportStructure src="assets/blocks/package_unpacking_bus.snbt" />
</GameScene>

The **Package Unpacking Bus** is a Formation-Plane-style destination. It accepts one routed package, reports that real held
package to ME storage, and permits ME or GUI extraction until the final atomic unpack commit. It does not scan the network
for packages.

The bus is a storage destination, not an export bus: AE2 chooses it when a package is inserted into network storage. Higher
priority receives packages first. At equal priority, an available Unpacking Bus is preferred over Package Storage Bus;
when it rejects for filter, held state, blocking, or capacity, routing can continue to another destination.

Speed cards shorten the visible work cycle. At completion, the bus rechecks filters, blocking, and cumulative target
capacity before inserting every package entry in order. A failed final check retains the whole package and retries later.

Pattern Provider-style blocking rejects a package when the target already contains one of its input types. Anti-clog mode
is independent and defaults on: enabled rejects network input unless the complete package passes the same blocking and
capacity preflight used by automatic unpacking; disabled accepts one valid package to wait and retry. A Sequence Buffer
target receives package entries atomically and can preserve sparse positions in pattern mode.

The Unpacking Bus accepts up to four speed cards. Capacity, fuzzy, and inverter cards share the same upgrade panel used by
the filter rows.

## Held Package Recovery

The working slot is real storage reported as one package item. Network extraction, GUI extraction, or breaking the part
returns the package and atomically cancels work, progress, blocked state, and retry state. Package contents are never
reported to ME until the final unpack commits them to the target.

Both buses are cable subparts and require a powered channel.

## Recipes

<RecipeFor id="appliedpackaging:package_storage_bus" />

<RecipeFor id="appliedpackaging:package_unpacking_bus" />

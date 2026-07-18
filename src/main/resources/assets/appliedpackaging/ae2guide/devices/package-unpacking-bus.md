---
navigation:
  parent: devices/index.md
  title: Package Unpacking Bus
  icon: appliedpackaging:package_unpacking_bus
  position: 30
item_ids:
- appliedpackaging:package_unpacking_bus
categories:
- applied packaging devices
---

# The Package Unpacking Bus

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/package_unpacking_bus.snbt" />
</GameScene>

You've made a package. Now you need it to arrive at the right machine, in the right order. This is where the Unpacking Bus comes in.

Think of it as AE2's Formation Plane, but with delivery order control. The network routes a package to it, and it inserts each item into the attached inventory — coal first, iron second, exactly as you encoded them.

This is a [cable subpart](ae2:ae2-mechanics/cable-subparts.md) and needs a [channel](ae2:ae2-mechanics/channels.md).

## How It Works, Step by Step

1. A package enters your ME network (you drop it in a terminal, autocrafting produces it, whatever).
2. The network routes it to the Unpacking Bus based on priority.
3. The bus holds the package in its visible working slot while it processes. You can see it sitting there.
4. When processing finishes, each item is inserted into the attached inventory, in order.
5. If the inventory can't take everything (it's full, or blocking mode says no), the bus waits and retries. **Nothing is ever partially inserted or lost.**

## Priority and Routing

Higher priority destinations receive packages first. At equal priority, an available Unpacking Bus is preferred over a Package Storage Bus. But if the bus rejects a package (filter says no, target is full, pre-admission check fails), the network tries the next destination — which might be a storage bus.

**Practical tip:** if you want packages unpacked automatically, give your Unpacking Buses higher priority than your Storage Buses. If you want packages stored and only unpacked on demand, swap the priorities.

## The Pre-Admission Thing

This bus has a setting that controls *when* it checks whether the target can actually receive the output. The GUI calls it "Anti-Clog Mode." Here's what it actually does:

* **On (default):** The bus checks that the complete package contents will fit in the target *before* taking the package out of network storage. If the check fails, the package stays in the network and can be routed elsewhere. **On is the safe choice.**
* **Off:** The bus takes the package immediately and holds it. If the target isn't ready, the package just waits. You can pull it out at any time through the GUI. **Off is the "I know what I'm doing" choice.**

This also interacts with blocking mode: with the pre-admission check on, if blocking mode prevents unpacking, the bus simply rejects the package instead of holding it.

## Blocking Mode

Enable this and the bus refuses to unpack if the target already contains any of the package's content types. Useful for preventing duplicate inputs when the machine hasn't finished processing the last batch yet.

## The Held Package

The package sitting in the working slot is a real item. You can pull it out through the GUI at any time — even mid-processing. Breaking the bus returns it too. The package contents are never exposed to ME storage until the unpack commits successfully.

## Upgrades

* <ItemLink id="ae2:speed_card" /> — Faster unpacking (up to 4)
* <ItemLink id="ae2:capacity_card" /> — More filter rows (up to 5, for 7 total rows)
* <ItemLink id="ae2:fuzzy_card" /> — Fuzzy matching per filter row
* <ItemLink id="ae2:inverter_card" /> — Invert content filtering per row

## Recipe

<RecipeFor id="appliedpackaging:package_unpacking_bus" />

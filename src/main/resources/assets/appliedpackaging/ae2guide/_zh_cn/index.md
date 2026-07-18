---
navigation:
  title: 应用封装
  icon: appliedpackaging:fluix_package
  position: 500
---

# 应用封装（Applied Packaging）

有些机器不在乎物品落到哪个槽。熔炉在乎——它要煤炭在侧面、矿石在顶部，每次都得这个顺序。而普通的 AE2 样板只会把所有东西丢进第一个可用槽位。

应用封装用**包裹**来解决这个问题——包裹是一种真实的、可堆叠物品，按你指定的精确顺序把多种物品打包在一起。它们有 17 种颜色，方便你给不同生产线做颜色标记。包裹像普通物品一样在 ME 网络中传输，只在到达目的地时才按顺序拆包。

如果你觉得 AE2 的自动合成应该知道槽位位置和物品顺序——就是这个模组了。

## 入门指南

* [入门指南](packaging-concepts/getting-started.md) — 一步一步做出你的第一个包裹

## 核心概念

* [包裹](packaging-concepts/packages.md) — 包裹是什么、能装什么、怎么用

## 设备

直接挂在 ME 线缆上的子部件，都需要频道。

* [高级样板编码终端](devices/advanced-pattern-terminal.md)
* [包裹存储总线](devices/package-storage-bus.md)
* [包裹卸货总线](devices/package-unpacking-bus.md)

## 机器

制作、打包和路由包裹的完整方块机器。

* [ME 包裹装配室](machines/package-assembler.md)
* [ME 打包机](machines/me-packager.md)
* [序列缓存器](machines/sequence-buffer.md)

## 更多

* [示例搭建](example-setups/index.md) — 可以直接照搬到你的世界的搭建方案
* [故障排除](troubleshooting.md) — 出了问题看这里

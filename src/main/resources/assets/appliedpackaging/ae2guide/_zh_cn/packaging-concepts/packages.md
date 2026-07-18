---
navigation:
  parent: packaging-concepts/index.md
  title: 包裹
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

# 包裹

<Row gap="8">
  <ItemImage id="appliedpackaging:fluix_package" scale="3" />
  <ItemImage id="appliedpackaging:red_package" scale="3" />
  <ItemImage id="appliedpackaging:green_package" scale="3" />
  <ItemImage id="appliedpackaging:blue_package" scale="3" />
  <ItemImage id="appliedpackaging:black_package" scale="3" />
</Row>

包裹就是你想的那样——一个装东西的盒子。但跟普通箱子不同，它能记住你放东西的精确顺序。煤炭在槽位 1、铁锭在槽位 2——不管包裹运到哪里，这个顺序永远不变。

## 包裹里有什么？

每个包裹保存四个信息：

* **内容物**——有序的物品列表。两个煤炭跟一个铁，和一个铁跟两个煤炭是不同的。顺序始终保留。
* **颜色**——17 种颜色之一。纯粹为了组织管理——不同生产线用不同颜色，或按颜色过滤路由。
* **标记**——可选的识别标签。编码时在标记槽放个铁锭，包裹就会显示"标记：铁锭"。标记物品永远不消耗——就是个标签。
* **布局**——如果编码时在物品之间留了空槽位，包裹会记住这些缺口。向有固定槽位的机器投递时很关键。

**包裹不能装包裹。** 如果你尝试，内部包裹会先被拆包，内容物直接纳入外层。

## 容量

默认每个包裹最多容纳 **9 种物品类型**和 **9 件物品**。在创建包裹的机器中安装 AE2 存储组件可以增加容量：

| 组件 | 最多类型 | 最多件数 |
|------|---------|---------|
| 无 | 9 | 9 |
| <ItemLink id="ae2:cell_component_16k" /> | 16 | 16 |
| <ItemLink id="ae2:cell_component_64k" /> | 63 | 64 |
| <ItemLink id="ae2:cell_component_256k" /> | 63 | 256 |

**成品存储元件和 1k 组件不能用。** 只有 16k、64k、256k 的原始组件被接受。

## 怎么制作包裹

三种方式：

| 方式 | 设备 | 什么时候用 |
|------|------|----------|
| 自动合成 | <ItemLink id="appliedpackaging:package_assembler" /> + 样板供应器 | 标准方式——集成到 AE2 自动合成 |
| 直接打包 | <ItemLink id="appliedpackaging:me_packager" /> | 想直接从 ME 存储打包，不想折腾样板 |
| 本地装配 | <ItemLink id="appliedpackaging:package_assembler" /> + 本地样板 | 手动或单批次使用 |

## 怎么拆包

* **潜行+右键**手里的包裹堆叠，拆出一个到物品栏。最快速的测试方式。
* 用[包裹卸货总线](../devices/package-unpacking-bus.md)自动向机器拆包。
* [ME 打包机](../machines/me-packager.md)也能拆包回 ME 存储。

## 颜色

<Row gap="8">
  <ItemImage id="appliedpackaging:fluix_package" scale="2" />
  <ItemImage id="appliedpackaging:white_package" scale="2" />
  <ItemImage id="appliedpackaging:orange_package" scale="2" />
  <ItemImage id="appliedpackaging:magenta_package" scale="2" />
  <ItemImage id="appliedpackaging:light_blue_package" scale="2" />
  <ItemImage id="appliedpackaging:yellow_package" scale="2" />
  <ItemImage id="appliedpackaging:lime_package" scale="2" />
  <ItemImage id="appliedpackaging:pink_package" scale="2" />
  <ItemImage id="appliedpackaging:gray_package" scale="2" />
</Row>
<Row gap="8">
  <ItemImage id="appliedpackaging:light_gray_package" scale="2" />
  <ItemImage id="appliedpackaging:cyan_package" scale="2" />
  <ItemImage id="appliedpackaging:purple_package" scale="2" />
  <ItemImage id="appliedpackaging:blue_package" scale="2" />
  <ItemImage id="appliedpackaging:brown_package" scale="2" />
  <ItemImage id="appliedpackaging:green_package" scale="2" />
  <ItemImage id="appliedpackaging:red_package" scale="2" />
  <ItemImage id="appliedpackaging:black_package" scale="2" />
</Row>

17 种颜色：福鲁伊克斯（默认）加上全部 16 种染料色。两个内容相同但颜色不同的包裹**不能叠加。** 这其实很有用——你可以用颜色来控制包裹去哪个存储总线。

## 样板类型

包裹从样板编码而来。两种类型：

* **<ItemLink id="appliedpackaging:package_pattern" />**——一个样板一个包裹。一种颜色、一个标记、一组输入。你最常用的。
* **<ItemLink id="appliedpackaging:advanced_processing_pattern" />**——一个样板最多 81 个包裹，每个有自己的颜色。用于复杂的多输出配方。

两者都在[高级样板编码终端](../devices/advanced-pattern-terminal.md)中制作。普通 AE2 样板终端读不了也改不了它们。

## 小贴士

* **按颜色标记生产线。** 红包裹 = 熔炼，蓝 = 合成。存储总线过滤器一目了然。
* **Shift+右键**随时随地拆包。测试必备。
* **包裹是物品，不是存储元件。** 像圆石或铁锭一样在 ME 网络中传输——存在驱动器里、导出、路由。
* **包裹不能装包裹。** 尝试的话，内部的会先被展平。

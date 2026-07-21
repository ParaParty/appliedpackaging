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

包裹在[包裹装配室](../machines/package-assembler.md)或 [ME 打包机](../machines/me-packager.md)中制作，由[高级样板编码终端](../devices/advanced-pattern-terminal.md)编码的样板定义。包裹将多种物品分组为一个可堆叠物品，在 ME 网络中传输，携带一种颜色、一个可选标记和有序内容。

包裹不能包含另一个包裹。如果使用包裹作为原料，其内容物会先被展平，再参与外层包裹的装配。潜行并右键包裹堆叠可拆出一个包裹到你的物品栏。

有两种样板类型用于不同目的：

*   <ItemLink id="appliedpackaging:package_pattern" /> 编码一个包裹，包含一种颜色、一个标记和输入布局。这是最常用的类型——当你需要将一组原料打包路由到单个目的地时使用。

*   <ItemLink id="appliedpackaging:advanced_processing_pattern" /> 从一个样板编码最多 81 个包裹，每列拥有独立的颜色和输入。装配室按列顺序输出包裹。当配方需要将原料拆分为多个组，通过颜色过滤的总线分别路由到不同目的地时使用。

两种样板都在[高级样板编码终端](../devices/advanced-pattern-terminal.md)中编码。普通 AE2 样板编码终端无法读取或编辑包裹样板。

## 容量

未安装组件时，机器使用默认 1k 档：最多 9 种类型和 256 包裹单位。在创建包裹的机器中安装 AE2 存储组件可提升上限：

| 组件 | 最多类型 | 最多单位 |
|------|---------|---------|
| 无（默认 1k 档） | 9 | 256 |
| <ItemLink id="ae2:cell_component_16k" /> | 16 | 4,096 |
| <ItemLink id="ae2:cell_component_64k" /> | 63 | 16,384 |
| <ItemLink id="ae2:cell_component_256k" /> | 63 | 65,536 |

空槽默认 1k 档与每种受支持的元件都提供其 ME 名义容量四分之一的包裹单位上限。

成品存储元件和 1k 组件不可使用；空槽已经提供 1k 档。只有 16k、64k 和 256k 原始组件可作为升级。

## 内容、颜色和标记

每个包裹存储有序内容列表、一种颜色（17 种之一）和一个可选标记物品。颜色和标记用于包裹存储总线和包裹卸货总线的过滤。编码时空槽位作为包裹布局的一部分被保留。

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

共有 17 种颜色：福鲁伊克斯（默认）加上全部 16 种染料颜色。内容相同但颜色不同的包裹不会叠加。颜色用于包裹存储总线和卸货总线的过滤——红色包裹去一处，蓝色包裹去另一处。

## 拆包方式

*   潜行并右键包裹堆叠可拆出一个包裹到物品栏。
*   [包裹卸货总线](../devices/package-unpacking-bus.md)按编码顺序向相邻容器拆包。
*   [ME 打包机](../machines/me-packager.md)可直接将包裹拆包回 ME 存储。

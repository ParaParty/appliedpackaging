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

包裹将多种物品打包为一个单独物品，在 ME 网络中传输。这使得你可以将一整套原料作为一个整体进行路由——例如通过子网络分发到多台设备，或将一个配方的原料拆分为多个组，每组去往不同的目的地。

包裹不能包含另一个包裹。如果使用包裹作为原料，其内容物会先被拆出，再参与外层包裹的装配。

潜行并右键点击包裹堆叠可以拆出一个包裹到你的物品栏中。

## 为什么使用包裹

### 分组路由

普通的 AE2 样板供应器只能将原料推送到一个相邻容器。使用包裹时，你将原料编码为一个包裹，将该包裹通过子网络路由，并在目的地拆包。这使得一个样板可以同时服务于多台机器：包裹作为一个整体在网络中传输，卸货总线配合序列缓存器将其内容分发到不同机器面。

### 拆分配方

有些配方需要将不同原料送往不同位置。你可以编码两个或更多包裹样板，每个携带配方的一部分原料，然后使用一个父处理样板，其输入为这些包裹。或者使用高级处理样板的多列模式——每一列成为一个独立的包裹。通过颜色过滤的卸货总线路由后，每个组将去往正确的目的地。

## 内容、颜色和标记

每个包裹存储有序内容列表、一种颜色（17 种之一）和一个可选的标记物品。颜色和标记用于包裹存储总线和卸货总线的过滤。颜色使你可以将不同的原料组路由到不同的目的地。

编码时物品之间的空槽位作为包裹布局的一部分被保留。配合序列缓存器的样板模式，可以将稀疏槽位位置映射到具体的缓存成员。

## 容量

默认每个包裹最多容纳 9 种物品类型和 9 件物品。在创建包裹的机器中安装 AE2 存储组件可提升上限：

| 组件 | 最多类型 | 最多件数 |
|------|---------|---------|
| 无 | 9 | 9 |
| <ItemLink id="ae2:cell_component_16k" /> | 16 | 16 |
| <ItemLink id="ae2:cell_component_64k" /> | 63 | 64 |
| <ItemLink id="ae2:cell_component_256k" /> | 63 | 256 |

成品存储元件和 1k 组件不可使用。仅 16k、64k 和 256k 原始组件有效。

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

共有 17 种颜色：默认的福鲁伊克斯色及 16 种染料色。内容相同但颜色不同的包裹不会叠加。颜色用于总线的过滤——不同颜色可以将不同组路由到不同目的地。

## 样板类型

*   <ItemLink id="appliedpackaging:package_pattern" /> 编码一个包裹，包含一种颜色、一个标记和输入布局。
*   <ItemLink id="appliedpackaging:advanced_processing_pattern" /> 从一个样板编码最多 81 个包裹，每列拥有独立的颜色和输入。

两者都在[高级样板编码终端](../devices/advanced-pattern-terminal.md)中编码。普通 AE2 样板终端无法读取或编辑它们。

## 拆包方式

*   潜行并右键点击包裹堆叠可拆出一个包裹到物品栏。
*   [包裹卸货总线](../devices/package-unpacking-bus.md)按编码顺序向相邻容器拆包。
*   [序列缓存器](../machines/sequence-buffer.md)保留包裹条目和样板供应器推送的顺序，支持将稀疏槽位映射到特定面。
*   [ME 打包机](../machines/me-packager.md)将包裹拆包回 ME 存储。

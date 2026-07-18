---
navigation:
  parent: machines/index.md
  title: ME 包裹装配室
  icon: appliedpackaging:package_assembler
  position: 10
item_ids:
- appliedpackaging:package_assembler
categories:
- applied packaging machines
---

# ME 包裹装配室

<BlockImage id="appliedpackaging:package_assembler" scale="8" />

ME 包裹装配室是你的主要包裹工厂。它接受样板，按你编码的精确顺序、颜色和布局产出对应的包裹。

如果你熟悉 AE2 的自动合成：把它放在 <ItemLink id="ae2:pattern_provider" /> 旁边就行了。供应器推送原料，装配室制作包裹，结果回到网络（或者直接推进旁边的箱子）。

## 让它跑起来

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../../assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="25" />
</GameScene>

* 1. 放置装配室，用线缆连入 ME 网络。
* 2. 在旁边放一个 <ItemLink id="ae2:pattern_provider" />（任何面都可以）。
* 3. 把编码好的[包裹或高级样板](devices/advanced-pattern-terminal.md)放入供应器。
* 4. 确保原料在你的 ME 网络里。
* 5. 自动合成请求包裹时，装配室自动干活。

你也可以跳过供应器：把样板直接放进装配室自己的样板槽，原料放进 GUI 输入网格。测试或单批次用很方便。

## 它实际干了什么

在拿走一件物品之前，装配室先检查完整包裹是否在容量限制内。样板太大了，样板留在槽里看得见，但输入槽锁住——不会浪费任何东西。

然后产出包裹。普通样板一个，[高级样板](devices/advanced-pattern-terminal.md)每列一个。

## 颜色和标记

装配室有自己的颜色选择器和标记槽。能不能用，取决于样板类型：

* **包裹或高级样板：** 用样板自己的颜色和标记。装配室的设置被忽略。
* **普通 AE2 合成或处理样板：** 颜色和标记由装配室决定。标记槽为空时，样板的输出物品成为标记。
* **没装样板：** 颜色选择器显示机器的持久设置——改了主意随时调。

标记物品永远放在槽里，绝不消耗。只是给每个产出的包裹打上标签。

## 容量

装配室按照你安装的存储组件决定容量上限：

| 组件 | 最多类型 | 最多件数 |
|------|---------|---------|
| 无 | 9 | 9 |
| <ItemLink id="ae2:cell_component_16k" /> | 16 | 16 |
| <ItemLink id="ae2:cell_component_64k" /> | 63 | 64 |
| <ItemLink id="ae2:cell_component_256k" /> | 63 | 256 |

组件装在装配室的组件槽里。存储元件和 1k 组件不能用——只有原始 16k、64k、256k 组件。

## 包裹去哪里

三种选择，GUI 里选：

* **输出到 ME 网络**——包裹直接进网络存储。最简单；装配室需要连网。
* **输出到相邻方块**——包裹推进你选择的相邻容器。装配室从六个相邻面中选一个能接收的，整个批次都用那个方向。
* **不自动输出**——包裹留在装配室里。你自己从 GUI 取。

## 阻挡模式

开启后，输出目标里已经有包裹就不开新批次。一个批次一旦允许进入，会完全排空——每个包裹按顺序出去，不重新检查。

## 比较器输出

装配室给出可用于红石控制的信号：

* **0**——空闲
* **1**——工作中（进度条在动）
* **2**——完成（包裹在输出槽等）

## 升级

* <ItemLink id="ae2:speed_card" />——加速装配（最多 5 张）
* AE2 16k/64k/256k 存储组件——更大包裹

## 合成配方

<RecipeFor id="appliedpackaging:package_assembler" />

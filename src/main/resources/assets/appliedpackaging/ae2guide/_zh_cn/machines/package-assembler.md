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

ME 包裹装配室接收输入的物品，根据相邻 <ItemLink id="ae2:pattern_provider" /> 或插入的[包裹样板](devices/advanced-pattern-terminal.md)、[高级处理样板](devices/advanced-pattern-terminal.md)或普通 AE2 样板中定义的操作进行装配，然后输出生成的包裹。默认情况下，输出直接进入相连的 ME 存储。

例如，装配室中有一个包裹样板，指定了煤炭在槽位 1、圆石在槽位 2。当 ME 存储中存在煤炭和圆石时，装配室会生成一个包含这两样物品的包裹。

## 主要用途

装配室的主要用途是放置在 <ItemLink id="ae2:pattern_provider" /> 旁边。样板供应器会将原料输入到相邻容器中，装配室则将其装配成包裹。由于装配室默认将包裹输出到 ME 存储，只需将样板供应器放在装配室旁边，即可将包裹装配集成到自动合成流程中。

**注意：** 如果你希望包裹进入子网络而非主网络，需要使用方向性样板供应器（用 <ItemLink id="ae2:certus_quartz_wrench" /> 右键调整方向），以使供应器和装配室不形成网络连接。这样装配室的输出就可以通过子网络路由到卸货总线和序列缓存器，将包裹内容物分发到不同的机器面。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../../assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="25" />
</GameScene>

## 输出模式

*   **输出到 ME 网络**（默认）。包裹直接进入相连的 ME 存储。
*   **输出到相邻方块。** 包裹放入选定的相邻容器中。装配室从六个相邻面中选择一个可接收输出的容器，并在整个批次中保持该方向。
*   **禁用。** 包裹留在装配室的输出槽中，供手动提取。

## 容量

装配室在消耗任何原料之前，会先确认计划中的每个包裹是否在容量限制之内：

| 组件 | 最多类型 | 最多件数 |
|------|---------|---------|
| 无 | 9 | 9 |
| <ItemLink id="ae2:cell_component_16k" /> | 16 | 16 |
| <ItemLink id="ae2:cell_component_64k" /> | 63 | 64 |
| <ItemLink id="ae2:cell_component_256k" /> | 63 | 256 |

成品存储元件和 1k 组件不可使用。存储组件安装在装配室的组件槽中。

## 颜色和标记

装配室自带了颜色选择器和标记槽。当使用包裹样板或高级样板时，样板自己的颜色和标记生效，装配室的设置会被忽略。当使用普通 AE2 样板时，装配室的颜色和标记才会被应用。标记物品永远不会被消耗。

## 阻挡模式

开启后，装配室会等待输出目标清空后再启动新批次。批次一旦开始，就会完整执行——批次中的每个包裹按顺序输出，中途不会重新检查。

## 比较器输出

比较器发出的信号为：0 表示空闲，1 表示工作中，2 表示输出中有已完成的包裹。

## 升级

ME 包裹装配室支持以下[升级](ae2:items-blocks-machines/upgrade_cards.md)：

*   <ItemLink id="ae2:speed_card" />（最多 5 张）
*   AE2 16k、64k 或 256k 存储组件，用于提升包裹容量

## 合成配方

<RecipeFor id="appliedpackaging:package_assembler" />

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

ME 包裹装配室接收物品、流体和其它与 AEKey 兼容的输入，根据相邻 <ItemLink id="ae2:pattern_provider" /> 或插入的[包裹样板](../devices/advanced-pattern-terminal.md)、[高级处理样板](../devices/advanced-pattern-terminal.md)或普通 AE2 样板执行装配操作，然后输出生成的包裹。与 ME 接口一样，它只实现一份通用 AEKey 库存，不另行实现物品 handler 视图；各资源类型的外部 capability 由 AE2 从该库存派生。默认情况下，输出进入相连的 ME 存储。

## 插入样板并使用漏斗输入输出

例如，装配室中有一个包裹样板，指定煤炭在槽位 1、圆石在槽位 2。当煤炭和圆石从输入漏斗送入时，装配室装配出一个包裹，并将其输出到下方的漏斗中。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/package_assembler_hopper.snbt" />
  <IsometricCamera yaw="195" pitch="30" />

  <BoxAnnotation color="#66aaff" min="2 1 0" max="3 2 1">
    (1) 输入漏斗：将原料送入装配室。
  </BoxAnnotation>
  <BoxAnnotation color="#cc88ff" min="1 1 0" max="2 2 1">
    (2) ME 包裹装配室：内部放入样板，输出设为相邻方块。
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="1 0 0" max="2 1 1">
    (3) 输出漏斗：接收完成的包裹。
  </BoxAnnotation>
</GameScene>

这是适合使用“输出到相邻方块”的场景：样板放在装配室内部，并由单向外部物流负责输入和输出。

## 包裹样板网格

不过，装配室的主要用法是放在 <ItemLink id="ae2:pattern_provider" /> 旁边，以类似分子装配室的网格方式排列。样板供应器将原料推送到相邻容器中，装配室将其装配为包裹。由于装配室默认将包裹输出到 ME 存储，包裹会直接回到网络中——只需将样板供应器放在装配室旁边，即可将包裹装配集成到自动合成系统中。

示例是一个 2×2×2 棋盘式网格：4 个样板供应器和 4 个包裹装配室。供应器会占用 4 个频道。结构留出了一段智能线缆，用于接入主网络。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="30" />

  <BoxAnnotation color="#dddddd" min="0 0 0" max="2 2 2">
    2×2×2 网格：4 个样板供应器和 4 个包裹装配室。
  </BoxAnnotation>
  <BoxAnnotation color="#66aaff" min="2 0 0" max="3 1 1">
    智能线缆：从这一端接入主网络。
  </BoxAnnotation>
</GameScene>

该模式只使用包裹样板。高级处理样板和普通 AE2 合成或处理样板请使用下方的子网络方案。

## 方向性供应器与路由子网络

当你需要按颜色路由包裹时，请使用方向性样板供应器。用 <ItemLink id="ae2:certus_quartz_wrench" /> 右键调整供应器，使朝向装配室的选中面不提供网络连接。保持装配室输出为“输出到 ME 网络”（默认），并将它连接到独立子网络；该子网络只使用一个或多个按不同颜色过滤的包裹存储总线作为存储。

<GameScene zoom="5" background="transparent">
  <ImportStructure src="../assets/assemblies/package_assembler_subnetwork.snbt" />
  <IsometricCamera yaw="195" pitch="30" />

  <BoxAnnotation color="#66aaff" min="0 0 0" max="1 1 2">
    (1) 主网络与方向性样板供应器：选中面朝东。
  </BoxAnnotation>
  <BoxAnnotation color="#cc88ff" min="1 0 1" max="2 1 2">
    (2) 包裹装配室：属于子网络，并输出到 ME 存储。
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="2 0 1" max="5 1 2">
    (3) 路由子网络。
  </BoxAnnotation>
  <BoxAnnotation color="#ff7777" min="4 0 0" max="5 1 1">
    (4) 包裹存储总线：过滤红色包裹。
  </BoxAnnotation>
  <BoxAnnotation color="#6688ff" min="4 0 2" max="5 1 3">
    (5) 包裹存储总线：过滤蓝色包裹。
  </BoxAnnotation>
</GameScene>

在此方案中，不要将装配室设为“输出到相邻方块”。方向性供应器仍是相邻容器；相邻输出可能把成品重新放回主网络，从而绕过路由子网络。

这也是在装配室中使用普通 AE2 样板（合成或处理样板）时的推荐方案，因为装配室可以将自己的颜色和标记应用到生成的包裹上以用于路由。

## 高级样板输出

处理高级样板时，装配室为每一列生成一个包裹，并按列顺序输出。在编码终端中为每列设置不同颜色，再在子网络上为每个目的地设置一条对应颜色的包裹存储总线。这样路由不再依赖输出顺序。

## 输出模式

*   **输出到 ME 网络**（默认）。包裹进入相连的 ME 存储。
*   **输出到相邻方块。** 包裹放入选定的相邻容器中。装配室从六个相邻面中选择一个可接收输出的容器，并在整个批次中保持该方向。
*   **禁用。** 包裹留在装配室的输出槽中，供手动提取。

插入样板并使用单向外部物流时可以选择相邻输出；包裹样板网格和方向性供应器子网络都应保持输出到 ME 网络。

## 容量

装配室在消耗任何原料之前，会先确认每个包裹是否在容量限制内：

| 组件 | 最多类型 | 最多单位 |
|------|---------|---------|
| 无（默认 1k 档） | 9 | 256 |
| <ItemLink id="ae2:cell_component_16k" /> | 16 | 4,096 |
| <ItemLink id="ae2:cell_component_64k" /> | 63 | 16,384 |
| <ItemLink id="ae2:cell_component_256k" /> | 63 | 65,536 |

空槽默认 1k 档与每种受支持的元件都提供其 ME 名义容量四分之一的包裹单位上限。

成品存储元件和 1k 组件不可使用；空槽已经提供 1k 档。将受支持的升级组件安装在装配室的组件槽中。

## 颜色和标记

装配室自带了颜色选择器和标记槽。使用包裹或高级样板时，样板自己的颜色和标记生效，装配室的设置被忽略。使用普通 AE2 样板时，装配室的颜色和标记才会被应用。标记物品永远不会被消耗。

## 阻挡模式

开启后，装配室会等待输出目标清空后再启动新批次。批次一旦开始就会完整执行——每个包裹按顺序输出，途中不重新检查。

## 比较器输出

比较器发出的信号：0 表示空闲，1 表示工作中，2 表示输出中有已完成的包裹。

## 升级

ME 包裹装配室支持以下[升级](ae2:items-blocks-machines/upgrade_cards.md)：

*   <ItemLink id="ae2:speed_card" />（最多 5 张）
*   AE2 16k、64k 或 256k 存储组件，用于提升包裹容量

## 合成配方

<RecipeFor id="appliedpackaging:package_assembler" />

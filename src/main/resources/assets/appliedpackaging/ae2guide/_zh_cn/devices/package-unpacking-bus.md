---
navigation:
  parent: devices/index.md
  title: 包裹卸货总线
  icon: appliedpackaging:package_unpacking_bus
  position: 30
item_ids:
- appliedpackaging:package_unpacking_bus
categories:
- applied packaging devices
---

# 包裹卸货总线

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../../assets/blocks/package_unpacking_bus.snbt" />
</GameScene>

包裹卸货总线从网络存储中接收包裹，并按照编码顺序将内容物插入到它所贴附的容器中。网络将包裹路由到总线后，总线会处理该包裹，将每件物品按顺序放入目标容器。

这是[线缆子部件](ae2:ae2-mechanics/cable-subparts.md)。

总线只有在所有内容物都能被完整插入时才会执行操作。如果目标容器空间不足或无法接收全部物品，总线会等待并重试。它不会只插入包裹中的一部分物品。

## 过滤行

初始提供两行过滤。每张 <ItemLink id="ae2:capacity_card" /> 增加一行，使用五张容量卡最多可以达到七行。各行之间是"或"的关系：包裹只要匹配任意一个已启用的过滤行，就会被接收。

每行可以组合以下条件：

*   颜色过滤。留空表示不按颜色过滤。
*   标记过滤。留空表示匹配任意标记。
*   最多六个内容过滤。

## 预接收检查

总线有一个选项用于决定何时检查目标容器是否能容纳所有输出。该选项在 GUI 中标注为"防堵塞模式"，默认处于开启状态：

*   **开启：** 总线在从网络中取出包裹之前，先检查目标容器是否有足够空间容纳全部内容物。如果检查不通过，包裹会留在网络存储中，可以被路由到其他目的地。
*   **关闭：** 总线会先取走包裹并保留在工作槽中。如果目标容器尚未就绪，包裹会在槽位中等待直到条件满足。你可以随时从 GUI 中取出等待中的包裹。

如果阻挡模式也处于开启状态，预接收检查会同时遵循阻挡规则：当目标容器中已存在包裹内容物中的任何类型物品时，总线不会接收该包裹。

## 滞留包裹

工作槽中的包裹是一个实际存在的物品。你可以随时从 GUI 中取出它，即使总线正在处理过程中。破坏总线也会返回该包裹。包裹的内容物在拆包成功完成之前不会暴露给 ME 存储。

## 用于子网络

将卸货总线与序列缓存器一起放在子网络上，单个卸货总线就可以将一个包裹的各个条目拆分到多个目的地。序列中的每个缓存成员接收一个包裹条目，并通过自己配置的输出面将其送出。例如，一个熔炉可以从同一个包裹中分别接收煤炭到侧面槽位和矿石到顶部槽位。

你也可以通过配置不同颜色或标记的过滤行，使卸货总线接收不同类型的包裹，并将每种类型路由到不同的缓存序列。所有路由由一个总线和子网络统一控制。

## 升级

包裹卸货总线支持以下[升级](ae2:items-blocks-machines/upgrade_cards.md)：

*   <ItemLink id="ae2:speed_card" /> 缩短拆包工作周期（最多 4 张）
*   <ItemLink id="ae2:capacity_card" /> 增加过滤行数量（最多 5 张，共 7 行）
*   <ItemLink id="ae2:fuzzy_card" /> 启用每行模糊匹配
*   <ItemLink id="ae2:inverter_card" /> 反转每行内容过滤

## 合成配方

<RecipeFor id="appliedpackaging:package_unpacking_bus" />

---
navigation:
  parent: packaging-concepts/index.md
  title: 入门指南
  icon: appliedpackaging:package_pattern
  position: 5
---

# 入门指南

## 你的第一个包裹

普通的 AE2 样板供应器将原料推送到单个相邻容器。包裹让你将这些原料打包为一个物品，然后通过子网络路由到一个或多个目的地。

完整流程是：编码样板、装配包裹、路由到输出箱子。

## 需要准备的材料

*   1x <ItemLink id="appliedpackaging:advanced_pattern_encoding_terminal" />
*   1x <ItemLink id="appliedpackaging:package_assembler" />
*   1x <ItemLink id="appliedpackaging:package_unpacking_bus" />
*   若干 ME 线缆，以及一个已通电并带有至少一个存储元件的 ME 网络
*   几个 <ItemLink id="ae2:blank_pattern" />
*   一些圆石和煤炭
*   一个箱子

## 第一步：编码包裹样板

1. 将 <ItemLink id="appliedpackaging:advanced_pattern_encoding_terminal" /> 放置在与网络相连的 ME 线缆上。与其他 AE2 终端一样，它需要一个频道。

2. 打开终端，选择**包裹样板**标签页。

3. 将一份空白 AE2 样板放入空白样板槽。

4. 将圆石放入槽位 1，煤炭放入槽位 2。槽位顺序会被记录在样板中。

   你可以从 JEI 或 EMI 拖拽物品，或使用 AE2 的中键快捷选取。

5. 从颜色选择器中选择一种颜色。颜色之后用于过滤和路由。

6. 点击**编码**。从输出槽取出 <ItemLink id="appliedpackaging:package_pattern" />。

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
</Row>

## 第二步：装配包裹

1. 放置 <ItemLink id="appliedpackaging:package_assembler" />，使用线缆连接到 ME 网络。

2. 打开 GUI，将编码好的样板放入样板槽。装配室从 ME 存储提取原料。默认情况下，完成的包裹直接进入 ME 存储。

3. 进度条走完后，包裹在你的网络中——可在任意终端查看。

<BlockImage id="appliedpackaging:package_assembler" scale="8" />

## 第三步：拆包

1. 放置一个箱子，将 <ItemLink id="appliedpackaging:package_unpacking_bus" /> 放在面向箱子的 ME 线缆上。

2. 网络将包裹路由到总线。圆石和煤炭按编码顺序出现在箱子中。

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../../assets/blocks/package_unpacking_bus.snbt" />
</GameScene>

## 进一步探索

### 分组路由

将卸货总线放在子网络上。将包裹通过该子网络路由，总线路由到目的地后拆包。参见[有序机器输入](../example-setups/ordered-machine-inputs.md)示例获取分步搭建说明。

### 拆分配方

编码两个包裹样板，每个使用不同颜色，各自携带配方的一部分原料。然后编码一个普通 AE2 处理样板，其输入为这两个包裹。自动合成执行时，每个包裹可通过颜色过滤路由到不同目的地。

也可以使用高级处理样板，在编码终端中写两列。每列成为一个独立颜色的包裹。

## 下一步

*   [包裹](packages.md)涵盖容量、颜色、标记和样板类型。
*   [高级样板编码终端](../devices/advanced-pattern-terminal.md)页面详细介绍了编码终端的两个页面。
*   [示例搭建](../example-setups/index.md)提供带标注场景和分步说明的搭建方案。
*   [故障排除](../troubleshooting.md)如果遇到问题。

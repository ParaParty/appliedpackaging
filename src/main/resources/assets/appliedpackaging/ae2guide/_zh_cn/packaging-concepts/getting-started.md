---
navigation:
  parent: packaging-concepts/index.md
  title: 入门指南
  icon: appliedpackaging:package_pattern
  position: 5
---

# 入门指南

## 第一个包裹

普通的 AE2 样板只能将原料推送到一个相邻容器。如果你希望一个样板同时服务于多台机器，或者将一个配方的原料拆分为多个组分别路由到不同目的地，就需要先将原料打包分组。这就是包裹的作用。

包裹将一组原料捆绑为一个单独物品，可以在 ME 网络中路由。目的地的卸货总线打开包裹并按顺序投递内容物。序列缓存器保留这个顺序，将每个物品映射到指定的机器面。

## 需要准备的材料

*   1x <ItemLink id="appliedpackaging:advanced_pattern_encoding_terminal" />
*   1x <ItemLink id="appliedpackaging:package_assembler" />
*   1x <ItemLink id="appliedpackaging:package_unpacking_bus" />
*   若干 ME 线缆，以及一个已通电并带有存储的 ME 网络
*   几个 <ItemLink id="ae2:blank_pattern" />
*   要打包的物品（本教程使用圆石和煤炭作为示例）
*   一个接收输出的箱子

## 第一步：编码包裹样板

1. 将 <ItemLink id="appliedpackaging:advanced_pattern_encoding_terminal" /> 放置在与网络相连的 ME 线缆上。终端需要一个频道。

2. 打开终端，选择**包裹样板**标签页。

3. 将空白 AE2 样板放入空白样板槽。

4. 将圆石放入槽位 1，煤炭放入槽位 2。槽位位置会被记录在样板中。

   可以从 JEI 或 EMI 中拖拽物品，也可以使用 AE2 的中键快捷选取。

5. 从颜色选择器中选择一种颜色。颜色用于后续对包裹进行分类路由。

6. 点击**编码**。从输出槽取出 <ItemLink id="appliedpackaging:package_pattern" />。

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
</Row>

## 第二步：装配包裹

1. 放置 <ItemLink id="appliedpackaging:package_assembler" />，使用线缆连接到 ME 网络。

2. 打开 GUI，将编码好的样板放入样板槽。装配室会从 ME 存储中提取原料并生成包裹。默认情况下输出直接进入 ME 存储。

3. 进度条完成后，包裹会出现在 ME 存储中，可在任何终端查看。

<BlockImage id="appliedpackaging:package_assembler" scale="8" />

## 第三步：路由并拆包到箱子

1. 放置一个箱子，将 <ItemLink id="appliedpackaging:package_unpacking_bus" /> 放在指向该箱子的 ME 线缆上。总线从网络接收包裹，按编码顺序将内容物放入相邻容器。

2. 包裹被路由到总线。圆石和煤炭按顺序出现在箱子中。

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../../assets/blocks/package_unpacking_bus.snbt" />
</GameScene>

## 进一步探索

### 分组路由

将卸货总线放在带有序列缓存器的子网络上。总线将包裹拆包到缓存器，每个缓存成员将一件物品投递到指定的机器面。这使得一个包裹可以同时向机器的多个面投递物品——非常适合需要在特定输入槽放入特定物品的模组机器，例如 Create 的机械合成器。

### 拆分配方

编码两个包裹样板——每个对应一组原料。然后编码一个普通的 AE2 处理样板，其输入为这两个包裹。当自动合成请求结果时，装配室产生两个包裹，每个包裹可以通过颜色过滤的卸货总线分别路由到不同目的地。也可以使用高级处理样板的两列模式——每一列成为一个独立颜色的包裹。

## 下一步

*   进一步了解[包裹](packages.md)——容量、颜色、标记和手动拆包
*   [高级样板编码终端](devices/advanced-pattern-terminal.md)可以从一个样板编码最多 81 个包裹
*   查看[示例搭建](example-setups/index.md)获取现成的搭建方案
*   遇到问题请参考[故障排除](troubleshooting.md)

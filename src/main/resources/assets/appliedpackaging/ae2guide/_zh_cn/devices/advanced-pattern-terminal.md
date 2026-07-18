---
navigation:
  parent: devices/index.md
  title: 高级样板编码终端
  icon: appliedpackaging:advanced_pattern_encoding_terminal
  position: 10
item_ids:
- appliedpackaging:advanced_pattern_encoding_terminal
- appliedpackaging:package_pattern
- appliedpackaging:advanced_processing_pattern
categories:
- applied packaging devices
- applied packaging items
---

# 高级样板编码终端

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../../assets/blocks/advanced_pattern_encoding_terminal.snbt" />
  <IsometricCamera yaw="180" />
</GameScene>

你知道 AE2 的 <ItemLink id="ae2:pattern_encoding_terminal" /> 吧？这个就是它的包裹版。

它有两个独立的页面：**包裹**和**高级**。每个页面有自己的存储空间——切换页面不会混入或复制任何东西。你大部分时间都会用在包裹页面上。

这是一个[线缆子部件](ae2:ae2-mechanics/cable-subparts.md)，需要[频道](ae2:ae2-mechanics/channels.md)，和 AE2 的普通样板终端一样。

**普通 AE2 样板终端读不了也改不了包裹样板。** 必须用这个。

## 包裹页面

一次做一个包裹样板。

1. 把空白 AE2 样板放入右侧的空白样板槽。
2. 把物品放入输入网格。**空槽位有意义。** 煤炭在槽位 1、槽位 2 留空、铁在槽位 3——包裹记得槽位 2 是故意留的。这叫稀疏布局。
3. 从颜色选择器选一个颜色。
4. 可选：在标记槽放个物品。标记就是标签——你可以用铁锭标记一个"熔炉输入"包裹。标记物品永远不消耗。
5. 点**编码**。得到 <ItemLink id="appliedpackaging:package_pattern" />。

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
</Row>

**同一个物品放不同槽位会被当成不同的条目。** 槽位 1 和槽位 3 都放煤炭，你会得到两个独立的煤炭条目——不会合成一堆。当接收机器有多个都接受煤炭的输入槽时，这很重要。

## 高级页面

当一个配方需要产出不止一个包裹时——最多 81 个，每个有自己的颜色和输入。

屏幕一次显示四列。每一列成为一个包裹。列的顺序就是包裹的产出顺序。用滚动条浏览完整的 81×81 网格。

一个配方需要多个有序输入组、每组去往不同机器面时，这就很有用。比如：第 1 列给熔炉的燃料面，第 2 列给矿石顶部，第 3 列给输出提取。每列成为一个独立颜色的包裹。

### 颜色模式

左侧工具栏第一个按钮控制新增列的颜色：

* **默认色**——新列永远从福鲁伊克斯开始。稳定但无聊。
* **循环颜色**——新列取下一个未用过的颜色，在 17 色中循环。让不同列一目了然。

**切换颜色模式不会动已有列。** 只影响切换后新加的列。要改特定列的颜色，点它自己的颜色按钮。

### 配方查看器填充

可以从 JEI 或 EMI 直接导入配方到任一页面。确定性配方都能用。随机输出或模糊不清的配方会被拒绝——你会看到清晰明了的错误提示。

## 合成配方

<RecipeFor id="appliedpackaging:advanced_pattern_encoding_terminal" />

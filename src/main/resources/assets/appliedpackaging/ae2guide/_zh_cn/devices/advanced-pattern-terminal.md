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

高级样板编码终端用于编码包裹样板和高级处理样板。它有两个独立的页面：**包裹**和**高级**。每个页面维护自己的物品栏——切换页面不会复制或清除另一个页面的物品。

这是一个线缆子部件，需要频道。普通的 AE2 样板编码终端无法读取或编辑包裹样板。

## 包裹页面

每次编码一个 <ItemLink id="appliedpackaging:package_pattern" />：

1. 将空白 AE2 样板放入空白样板槽中。
2. 在输入网格中放入物品。空槽位会作为布局的一部分被记录。
3. 从颜色选择器中选择一种颜色。
4. 可以选择在标记槽中放入一个物品。标记永远不会被消耗。
5. 点击编码按钮。

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
</Row>

同一物品放入不同槽位会作为独立的条目存在。例如，当样板在槽位 1 和槽位 3 都编码了煤炭时，这两个煤炭会保持为独立的条目，可以通过序列缓存器路由到不同的目的地。

## 高级页面

编码一个最多包含 81 个包裹列的 <ItemLink id="appliedpackaging:advanced_processing_pattern" />，每列拥有独立的颜色和最多 81 个稀疏输入槽位。屏幕一次显示四列。

每一列成为一个包裹，列的顺序就是包裹的输出顺序。当一个配方需要从单个样板生成多个包裹，且每个包裹携带不同原料去往不同目的地时，高级页面非常有用。

### 颜色模式

左侧工具栏中的按钮控制新增列的颜色分配方式：

*   **默认色**为每个新列分配福鲁伊克斯色。
*   **循环颜色**按序列分配下一个未使用的颜色，在全部 17 种颜色中循环。

切换颜色模式不会影响已有的列，只会影响之后添加的列。

### 配方查看器填充

可以从 JEI 或 EMI 中将配方导入到任意页面。确定性配方受支持，模糊或随机输出的配方会被拒绝，并显示明确的错误提示。

## 合成配方

<RecipeFor id="appliedpackaging:advanced_pattern_encoding_terminal" />

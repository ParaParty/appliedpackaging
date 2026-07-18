---
navigation:
  parent: index.md
  title: 高级样板编码终端
  icon: appliedpackaging:advanced_pattern_encoding_terminal
  position: 20
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
  <ImportStructure src="../assets/blocks/advanced_pattern_encoding_terminal.snbt" />
  <IsometricCamera yaw="180" />
</GameScene>

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
  <ItemImage id="appliedpackaging:advanced_processing_pattern" scale="4" />
</Row>

该线缆部件有两个彼此隔离的页面。**包裹**页编码带有有序输入、颜色、标记和稀疏位置的包裹样板；**高级**页最多记录 81 个带颜色的包裹列，每列有 81 个稀疏输入位置。界面一次只显示相邻四列，这只是可见窗口，并不是编码上限。

## 包裹页面

1. 把 AE2 空白样板放入空白样板槽。
2. 在输入网格中放置或从配方查看器转入包裹内容。空位置有实际意义，编码时会记录为稀疏布局。
3. 选择包裹颜色和可选标记。标记用于表示预期配方或产物，但不会被消耗。
4. 编码得到 <ItemLink id="appliedpackaging:package_pattern" />。

同一种资源出现在不同位置时会保留为多个独立条目，方便后续机器按位置接收。

## 高级页面

每个启用的高级列拥有独立颜色并生成一个包裹。页面最多编码四个普通处理产物，其中第一个主产物会成为全部生成包裹共用的标记；包裹列数量与普通产物数量彼此独立。

通过横向列窗口和纵向行窗口可以访问完整的 81x81 稀疏编辑区。它适合把一次处理所需的大量有序输入组分别送到不同侧面或精确槽位。

列顺序就是包裹输出顺序，每列内部的稀疏行位置也会保留。

## 编辑与页面隔离

两个页面保存独立库存。放入本模组的已编码样板会自动切换到对应页面；AE2 原版样板编码终端不会编辑这些专用样板。

切换页面不会复制或清空另一页。重新编码只更新已编码样板槽中的当前样板；损坏或不支持的样板会被拒绝，不会被误解码成另一种类型。

## 配方查看器转移

配方查看器传输支持确定性的 JEI/EMI 配方。可选 Create 与 GTCEu 集成会保留已知物品/流体输入顺序；面对歧义或随机输出时会拒绝导入，避免静默生成错误计划。

与 AE2 的 <ItemLink id="ae2:pattern_encoding_terminal" /> 相同，它是需要供电频道的线缆部件。

## 配方

<RecipeFor id="appliedpackaging:advanced_pattern_encoding_terminal" />

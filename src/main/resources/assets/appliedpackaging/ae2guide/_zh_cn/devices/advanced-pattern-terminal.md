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

高级样板编码终端编码包裹样板和高级处理样板。它有两个独立页面：包裹和高级。每个页面维护自己的物品栏——切换页面不会复制或清除另一个页面的物品。

这是线缆子部件，需要频道。普通 AE2 样板编码终端无法读取或编辑包裹样板。

## 界面

终端有两个页面，通过顶部标签切换。右侧有一个槽位接受空白样板。箭头按钮编码样板。一个槽位存放已编码样板——将已编码样板放入此处可编辑，再次点击编码箭头即可。

<a name="package-page"></a>

### 包裹页面

包裹页面每次编码一个包裹样板。

*   左键点击或从 JEI/REI 拖拽物品构建包裹内容。右键移除物品。
*   空槽位作为布局的一部分被记录。
*   从颜色选择器选择颜色，可选在标记槽放入物品。标记永不消耗。
*   也可以直接从 JEI/REI 配方界面编码样板。

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
</Row>

同一物品放入不同槽位产生独立条目。当样板在槽位 1 和槽位 3 都编码了煤炭时，两个煤炭条目保持独立。

<a name="advanced-page"></a>

### 高级页面

高级页面编码高级处理样板，最多 81 个包裹列，每列独立颜色和最多 81 个稀疏输入槽。屏幕一次显示四列。

每列成为一个包裹。列顺序即包裹输出顺序。

左侧工具栏的颜色模式按钮控制新增列的颜色。默认色为每个新列分配福鲁伊克斯色。循环颜色按序列分配下一个未使用颜色，在全部 17 色中循环。切换模式不影响已有列。

也可以直接从 JEI/REI 配方界面编码样板。

## 合成配方

<RecipeFor id="appliedpackaging:advanced_pattern_encoding_terminal" />

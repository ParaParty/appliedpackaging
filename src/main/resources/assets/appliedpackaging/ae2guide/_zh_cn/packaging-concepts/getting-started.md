---
navigation:
  parent: packaging-concepts/index.md
  title: 入门指南
  icon: appliedpackaging:package_pattern
  position: 5
---

# 应用封装入门指南

本指南带你从零开始制作第一个包裹——从编码样板到看着它拆包进入箱子。读完你会理解整个工作流程，可以开始设计自己的打包方案。

## 为什么要用包裹？

原版熔炉需要煤炭放进侧面、矿石放进顶部。普通 AE2 样板把东西乱丢一气——煤炭可能进了顶部，矿石无处可去，熔炉就干等着。

包裹解决了这个问题。你编码时把煤炭放槽位 1、矿石放槽位 2。包裹记住这个顺序。拆包到熔炉时，煤炭精准进入侧面、矿石进入顶部，每次都一样。

多条生产线时包裹也好用。红色包裹 = 熔炼，蓝色 = 合成——一眼就能看清东西去哪。

## 你需要准备

* 物品清单：
  * 1x <ItemLink id="appliedpackaging:advanced_pattern_encoding_terminal" />
  * 1x <ItemLink id="appliedpackaging:package_assembler" />
  * 1x <ItemLink id="appliedpackaging:package_unpacking_bus" />
  * 若干 ME [线缆](ae2:items-blocks-machines/cables.md)（玻璃线缆或包层线缆都行）
  * 一个已通电、至少有一个存储元件的 ME 网络
  * 几个 <ItemLink id="ae2:blank_pattern" />
  * 一些圆石和煤炭（或你想练习用的任何物品）
  * 一个箱子作为输出目标
  * 一个 <ItemLink id="ae2:pattern_provider" />（可选——装配室也能用自带的样板槽）

## 第一步：编码包裹样板

1. 把 <ItemLink id="appliedpackaging:advanced_pattern_encoding_terminal" /> 挂在连入网络的 ME 线缆上。需要频道，和 AE2 的普通样板终端一样。

2. 打开终端。顶部有两个标签页：**包裹样板**和**高级**。你需要包裹样板页面——如果不是默认选中的，点它。

3. 把空白 AE2 样板放入右侧的空白样板槽。

4. 在输入网格的第一个槽位放**煤炭**，第二个槽位放**圆石**。

   **空槽位有意义。** 如果你在槽位 1 放煤炭、槽位 2 留空、槽位 3 放铁锭，包裹会记住槽位 2 是你故意留空的。当目标机器有固定的槽位位置时，这一点很重要。

   你可以从 JEI/EMI 拖拽物品到槽位，或用 AE2 的中键快捷选取。

5. 从颜色选择器挑一个颜色——红、蓝，随你喜欢。这只是为了组织管理，不影响包裹的功能。

6. 可选：在**标记**槽放一个物品。标记就是个标签——你可以放个铁锭进去，让这个包裹标记为"熔炉输入"。标记物品永远不会消耗，只显示在提示文字里。

7. 点击**编码**。聊天栏会有提示。从输出槽取出编码好的样板。

<Row gap="20">
  <ItemImage id="appliedpackaging:package_pattern" scale="4" />
</Row>

## 第二步：装配包裹

现在把样板变成能拿到手里的真包裹。

1. 放置 <ItemLink id="appliedpackaging:package_assembler" />，用线缆连入 ME 网络。

2. 打开 GUI。把编码好的样板放入右侧的样板槽。

3. 确保圆石和煤炭在你的 ME 网络里。装配室会自动从网络存储中提取。

4. 装配室开始工作——你会看到进度条。完成后，一个带颜色的包裹出现在输出区域。

5. 把包裹取出来放进物品栏。鼠标悬停按住 Shift——你能看到颜色、按顺序的内容物、以及容量信息。

<BlockImage id="appliedpackaging:package_assembler" scale="8" />

你做出了第一个包裹。恭喜——但真正好玩的是把它送进机器。

## 第三步：拆包到箱子

1. 在地上放一个箱子。暂时用它代表你实际要用的机器。

2. 把 <ItemLink id="appliedpackaging:package_unpacking_bus" /> 挂在指向箱子的 ME 线缆上。需要频道。

3. 把包裹从任意终端丢进 ME 网络。网络会自动把它路由到卸货总线。

4. 片刻之后，煤炭和圆石出现在箱子里——煤炭在前、圆石在后。就是这个顺序。

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../../assets/blocks/package_unpacking_bus.snbt" />
</GameScene>

5. 在箱子下面放个漏斗把物品导入机器，你就完成了有序投递的自动化。

就是这样。**编码 → 装配 → 拆包。** 其余都是这个循环的变体。

## 接下来做什么？

* 深入了解[包裹](packages.md)——容量、颜色、标记、手动拆包
* [高级样板编码终端](devices/advanced-pattern-terminal.md)能一次编码最多 81 个包裹
* 查看[示例搭建](example-setups/index.md)获取现成的设计方案
* 出问题了？看[故障排除](troubleshooting.md)

包裹像其他任何物品一样在 ME 网络中传输——存在驱动器里、用存储总线路由、用输出总线导出。包裹层只是在普通 AE2 物流之上增加了顺序和结构。

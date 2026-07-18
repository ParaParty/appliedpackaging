# Brief: UI And Icons

## 范围

本资产包维护 Applied Packaging 的 GUI sprite 复用、current-AE2 缓存与实际 mod logo，不再维护未被运行时代码引用的独立图标占位集。

当前交付：

```text
src/main/resources/assets/appliedpackaging/logo.png
src/main/resources/assets/appliedpackaging/textures/gui/ae2-states.png
src/main/resources/assets/appliedpackaging/textures/gui/ae2-terminal.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_scrollbar.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_sprites.png
src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus-sprites.png
docs/assets/reports/ui-and-icons.md
```

## 复用规则

```text
工具栏按钮、状态、槽位、搜索框、置顶合成行和滚动条优先复用语义一致的 current-AE2 sprite
包裹语义或高版本类型过滤没有合适 current-AE2 对应时，使用 package-storagebus-sprites.png [0,96,48,48] 用户工具栏图标块
颜色格、marker 空槽、包裹模式、转置、防堵塞、同步输出、样板同步、输入延迟、仅物品和仅流体按钮继续复用 Applied Packaging 用户 atlas
不发布没有 Screen/Widget/模型引用的 textures/gui/icons/*.png
不发布未被界面使用的 textures/gui/logo.png；实际 mod logo 只保留 assets/appliedpackaging/logo.png
AE2 原字节缓存必须使用 Applied Packaging namespace、固定哈希和独立 LGPL 来源记录
```

## 视觉

```text
深灰 AE2 面板底色
浅灰槽位
Fluix 紫蓝高亮
符号优先，不使用文字
8x8、12x12、16x16 下保持可读
```

## 当前界面重点

```text
序列缓存器主滚动条使用缓存的 current-AE2 12x15 big scroller enabled/disabled sprite
包裹装配室小滚动条使用 current-AE2 7x15 sprite，并在底图轨道上右移 1px 对齐
高级终端搜索框和置顶合成行读取 ae2-terminal.png，不读取 AE2 15.4.10 旧 terminal atlas
高级终端颜色模式使用最左侧 current-AE2 scheduling 图标，不增加独立小图标资产
包裹装配室颜色、marker、容量和工作锁定使用现有 widget/sprite，不增加占位 icon
包裹装配室四行输入槽单独右移 1px；颜色 swatch 和 marker 内容必须落入更新后的底图框
高级终端 base 的编辑区是灰底，当前模式面板必须在 base 之后后绘制
```

## 验收

```text
所有运行时 GUI 资源都被代码、ScreenStyle 或模型实际引用
被删除的独立图标、GUI logo、重复 states atlas 与退役终端底图不再出现在资产合同或发布门禁中
current-AE2 缓存尺寸、RGBA、固定 SHA-256 与许可证记录通过资产审计
实际 mod logo 保持 128x128 RGBA 且可见
docs/assets/reports/ui-and-icons.md 记录当前来源、清理清单和验证结果
```

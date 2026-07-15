# Brief: Terminal And Buses

## 范围

本资产包当前负责合并高级/包裹两页的 Advanced Pattern Encoding Terminal、Package Storage Bus 与 Package Unpacking Bus 三个 AE2 cable part 及其 GUI。Package Export Bus 与独立 Package Pattern Terminal 已取消，普通 AE2 Pattern Encoding Terminal 不再增加包裹模式。用户 sprite 内的 marker 空槽图标同时作为两台机器和高级终端包裹页的共享 UI 图标使用。

输出文件：

```text
src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus.png
src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus-sprites.png
src/main/resources/assets/appliedpackaging/textures/gui/ae2-states.png
  src/main/resources/assets/appliedpackaging/textures/gui/package_bus_extra_panels.png
  src/main/resources/assets/appliedpackaging/textures/gui/package_bus_vertical_buttons_bg.png
  src/main/resources/assets/ae2/screens/appliedpackaging/advanced_pattern_encoding_terminal.json
src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_dark.png
src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_medium.png
src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_bright.png
src/main/resources/assets/appliedpackaging/models/item/advanced_pattern_encoding_terminal.json
src/main/resources/assets/appliedpackaging/models/item/package_storage_bus.json
src/main/resources/assets/appliedpackaging/models/item/package_unpacking_bus.json
docs/assets/reports/terminal-and-buses.md
```

## 高级样板编码终端

```text
物品形态使用 AE2 neoforge/v19.2.17 六段 item display 几何；世界 Part 使用同一几何沿 Z 轴 -7 平移后的 cable-part 版本，不使用与 15.4.10 同形的两段 part display base
正面按 dark、medium、bright 三张透明遮罩同时叠加，并分别使用 tint index 1、2、3
用户提供的三张 16x16 RGBA 遮罩保持原文件字节
通电模型使用 1.20.1 Forge 等价的 forge_data 全亮光照；断电模型保留普通环境光
状态灯使用 v19 四段式几何，不回退到 AE2 15.4.10 的整圈状态层
基础外壳和状态灯贴图从固定 v19.2.17 源复制并保留 LGPL 来源记录；物品与世界 Part 都保留 monitor_colored tint index 4
```

## 总线

Package Storage Bus：

```text
Storage Bus 轮廓
封闭包裹图标
暗色过滤槽
```

Package Unpacking Bus：

```text
总线轮廓
打开的包裹封印或散出粒子感
保持整包事务心智，不表现散乱掉落物
```

## 约束

```text
与 AE2 cable part 风格兼容
高级终端不得把 dark/medium/bright 误作断电、通电、有频道三态
小尺寸下能区分两种 bus
不要把 bus 做成完整机器方块
不要加入文字标签
GUI 必须保持 AE2 1.21.1 的紧凑按钮和透明禁用槽语言
Advanced Pattern Screen 右侧只放高级/包裹两个模式标签；两行网络库存时高级/包裹 profile 分别为 217x250 与 195x233，高级页保留 195px 主体并增加 22px 标签区，高级编辑框宽 146px，包裹 panel 保持 124x66 原尺寸且不拉伸
右侧模式标签直接对照 current-AE Pattern Encoding Terminal 的 `TabButton.Style.HORIZONTAL`：22x22 normal/selected/focus 背景、21px 步进、ItemStack 偏移 `(3,3)`；禁止右上角外接 VerticalButtonBar/IconButton
合并终端不创建 VIEW_CELL 槽、不绘制显示元件面板；普通 AE Pattern Screen 不增加 package tab，也不得覆盖 AE2 原生 terminal style
GUI 中心过滤区固定为 7 行：基础 2 行，5 张容量卡逐行解锁到第 7 行
外围底板必须保留 AE2 current-main Storage Bus 的 `#CBCCD4` 主体色与 `#413F54` 最外圈边框，不得把主体误用为 `#ADB0C4` 槽内色
用户 sprite `(32,16,16,16)` 是 Applied Packaging 自绘 marker 空槽图标，不得标记为 AE2 来源或替换为 AE2 原版 sprite
```

## 验收

```text
storage/unpacking 两种 part 都有可加载的临时模型和报告
高级终端世界/物品模型使用 v19 几何，三张用户遮罩按 tint 1/2/3 同时渲染并保持源文件哈希
用户提供的 256x256 RGBA GUI 与 sprite 分别保持原文件字节，禁止合图、重绘或烘入 AE2 像素
`ae2-states.png`、`package_bus_extra_panels.png` 与 `package_bus_vertical_buttons_bg.png` 保持 AE2 当前 main 原文件字节与 LGPL 来源记录
Advanced Pattern ScreenStyle 同时声明高级布局和包裹页小滚动条，把继承的 view-cell 区域移出可见区；运行时按当前页选择高级 197px bottom 或包裹 180px bottom 并完整重排；`pattern_modes.png` 和 AE2 原生 terminal ScreenStyle override 必须缺席
空升级槽使用 current-main `BACKGROUND_UPGRADE`；实际卡片若要求新版外观，应单独提供新版 fuzzy/inverter/capacity/speed card 图，不从旧依赖材质冒充
ME Packager 容量元件空槽和 ME Package Assembler 的已编码样板/容量元件空槽分别使用原样 current-main `ae2-states.png` 的 `(240,48,16,16)`、`(240,112,16,16)` / `(240,48,16,16)`
总线、ME Packager 与高级终端包裹页的空 marker 槽均显示用户图标，并在可交互状态下提供说明 tooltip；ME Package Assembler 不提供 marker fallback 槽
docs/assets/reports/terminal-and-buses.md 记录生成提示和预览路径
```

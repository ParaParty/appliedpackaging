# Brief: Terminal And Buses

## 范围

本资产包当前负责 Package Storage Bus 与 Package Unpacking Bus 两个 AE2 cable part 及其共用 GUI。Package Export Bus 与独立 Package Pattern Terminal 已取消。用户 sprite 内的 marker 空槽图标同时作为两台机器和原版样板终端包裹模式的共享 UI 图标使用。

输出文件：

```text
src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus.png
src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus-sprites.png
src/main/resources/assets/appliedpackaging/textures/gui/ae2-states.png
src/main/resources/assets/appliedpackaging/textures/gui/package_bus_extra_panels.png
src/main/resources/assets/appliedpackaging/textures/gui/package_bus_vertical_buttons_bg.png
src/main/resources/assets/appliedpackaging/models/item/package_storage_bus.json
src/main/resources/assets/appliedpackaging/models/item/package_unpacking_bus.json
docs/assets/reports/terminal-and-buses.md
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
小尺寸下能区分两种 bus
不要把 bus 做成完整机器方块
不要加入文字标签
GUI 必须保持 AE2 1.21.1 的紧凑按钮、透明禁用槽和升级面板语言
GUI 中心过滤区固定为 7 行：基础 2 行，5 张容量卡逐行解锁到第 7 行
外围底板必须保留 AE2 current-main Storage Bus 的 `#CBCCD4` 主体色与 `#413F54` 最外圈边框，不得把主体误用为 `#ADB0C4` 槽内色
用户 sprite `(32,16,16,16)` 是 Applied Packaging 自绘 marker 空槽图标，不得标记为 AE2 来源或替换为 AE2 原版 sprite
```

## 验收

```text
storage/unpacking 两种 part 都有可加载的临时模型和报告
用户提供的 256x256 RGBA GUI 与 sprite 分别保持原文件字节，禁止合图、重绘或烘入 AE2 像素
`ae2-states.png`、`package_bus_extra_panels.png` 与 `package_bus_vertical_buttons_bg.png` 保持 AE2 当前 main 原文件字节与 LGPL 来源记录
空升级槽使用 current-main `BACKGROUND_UPGRADE`；实际卡片若要求新版外观，应单独提供新版 fuzzy/inverter/capacity/speed card 图，不从旧依赖材质冒充
ME Packager 容量元件空槽和 ME Package Assembler 的已编码样板/容量元件空槽分别使用原样 current-main `ae2-states.png` 的 `(240,48,16,16)`、`(240,112,16,16)` / `(240,48,16,16)`
总线、ME Packager 与原版样板终端包裹模式的空 marker 槽均显示用户图标，并在可交互状态下提供说明 tooltip；ME Package Assembler 不提供 marker fallback 槽
docs/assets/reports/terminal-and-buses.md 记录生成提示和预览路径
```

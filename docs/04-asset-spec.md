# 资产规格

## 1. 视觉定位

Applied Packaging 是 AE2 的 Fluix 数据包裹系统，不是纸箱物流。

关键词：

```text
浅灰石英面板
深灰金属框架
Fluix 紫蓝光
彩色束带
小型封签
数据槽
网格纹理
```

避免：

```text
纯纸箱
黄铜齿轮
重工业机械
过度 RGB
水印/文字标签
```

## 2. 颜色表

| 名称 | ID | 用途 |
| --- | --- | --- |
| Fluix | `fluix` | 默认包裹、核心封印、AE2 风格高亮 |
| White | `white` | 白色包裹束带 |
| Orange | `orange` | 橙色包裹束带 |
| Magenta | `magenta` | 品红包裹束带 |
| Light Blue | `light_blue` | 淡蓝包裹束带 |
| Yellow | `yellow` | 黄色包裹束带 |
| Lime | `lime` | 黄绿包裹束带 |
| Pink | `pink` | 粉色包裹束带 |
| Gray | `gray` | 灰色包裹束带 |
| Light Gray | `light_gray` | 淡灰包裹束带 |
| Cyan | `cyan` | 青色包裹束带 |
| Purple | `purple` | 紫色包裹束带 |
| Blue | `blue` | 蓝色包裹束带 |
| Brown | `brown` | 棕色包裹束带 |
| Green | `green` | 绿色包裹束带 |
| Red | `red` | 红色包裹束带 |
| Black | `black` | 黑色包裹束带 |

## 3. 包裹资产

物品与实体模型：

```text
包裹不再使用独立平面 item PNG。
17 色包裹 item 与掉落实体共用 10x10x8 盒体模型。
六面贴图来自 package_box_pixel_v7：
  front/back/side: 10x8
  top/bottom: 10x10
颜色由对应变体束带决定。
模型为单个 10x10x8 cuboid；每个 face 绑定独立的完整 face 贴图，JSON faces 必须使用 full-face uv [0,0,16,16]。
10x8 与 10x10 只描述 PNG 像素尺寸，不用于裁切 JSON UV。
不使用 atlas 裁切；不把基础箱体与束带拆成重叠模型层。
包裹 item model 与实体渲染必须使用 cutout_mipped，避免透明像素被 solid 路径渲染成黑块。
包裹存在物品 marker 时，静态模型仍渲染盒体，客户端 renderer 在前脸右下角、距外边框 1px 的 4x4 标记框内叠加 3x3 marker item；marker 中心必须与该 4x4 框中心对齐，保持 0.5px 内边距。
```

世界模型：

```text
10x10x8 盒体
微妙边框
侧面颜色束带
顶部居中束带
掉落时使用 appliedpackaging:package 实体渲染同一 item model
```

资源路径：

```text
src/main/resources/assets/appliedpackaging/models/item/<color>_package.json
src/main/resources/assets/appliedpackaging/models/item/package_box/<color>.json
src/main/resources/assets/appliedpackaging/textures/block/package_box/<color>/package_box_front.png
src/main/resources/assets/appliedpackaging/textures/block/package_box/<color>/package_box_back.png
src/main/resources/assets/appliedpackaging/textures/block/package_box/<color>/package_box_side.png
src/main/resources/assets/appliedpackaging/textures/block/package_box/<color>/package_box_top.png
src/main/resources/assets/appliedpackaging/textures/block/package_box/<color>/package_box_bottom.png
```

## 4. 机器资产

ME 包裹装配室：

```text
分子装配室轮廓
透明 Fluix 装配腔
悬浮包裹投影
样板接口纹理
17 色小灯条
输出口/阻挡灯
```

ME 打包机：

```text
临时使用 Create Packager 同款模型和贴图语言。
ME 网络连接方向由 network_side 方块状态决定；me_packager facing 保留为水平交互朝向，不驱动临时 Create 外壳旋转。
当前临时 Create 风格外壳按 network_side 选择水平/竖直 linked 外观，使链接/工作侧与 AE 连接方向一致，静态开口必须朝向 network_side；正式单面 AE 连接 overlay 等待后续新模型。
正式 Applied Packaging 打包机模型等待后续替换。
临时 Create 风格静态外壳使用 cutout_mipped 方块渲染层并保持 noOcclusion，避免透明 linked 贴图和内部光照被完整方块遮挡。
动态 hatch/iris 按 Create 原始 renderer 使用 solid pass；动态 tray 使用 cutout_mipped。
动画期间动态 tray 与包裹使用单独 immediate pass，并通过主 framebuffer stencil mask 限定在打包机方块体积内；mask 必须覆盖完整方块体积，不得在 network_side 方向额外向内收缩，避免链接口后方露出静态内部暗面；hatch/iris/链接口等边缘固定视觉仍走普通 pass，不得通过裁剪模型顶点实现。
network_side 为链接/工作侧和 AE 连接面；包裹动画口使用 `network_side`。network_side 连接面无论是否播放动画都必须在静态 block 模型中提供 `network_side_cover` 背板，用于挡住动画期间会变黑的输入口；该背板必须内缩于原透明面之后，不得作为未注册的 dynamic partial，也不得复用 hatch/iris 模型，避免 missing model 或第二个工作口。包裹显示栈按 Create 半程语义渲染：拆包入内时前半段显示输入包裹，打包外送时后半段显示输出包裹。
普通不透明机器 block/part 模型不得声明 render_type，保持默认 solid；只有确实带透明遮罩的模型或 overlay 才使用 cutout_mipped。
薄型 bus/terminal 方块必须保持 noOcclusion，避免按完整方块遮挡地面和光照。
```

资源路径：

```text
src/main/resources/assets/appliedpackaging/textures/block/package_assembler*.png
src/main/resources/assets/appliedpackaging/textures/block/me_packager*.png
src/main/resources/assets/appliedpackaging/blockstates/package_assembler.json
src/main/resources/assets/appliedpackaging/blockstates/me_packager.json
src/main/resources/assets/appliedpackaging/models/block/package_assembler.json
src/main/resources/assets/appliedpackaging/models/block/me_packager.json
src/main/resources/assets/appliedpackaging/models/item/package_assembler.json
src/main/resources/assets/appliedpackaging/models/item/me_packager.json
```

## 5. 终端与总线资产

包裹样板终端：

```text
AE2 终端面板
屏幕内多个彩色包裹卡片
侧边 17 色灯条
```

总线：

```text
Package Storage Bus: Storage Bus 轮廓 + 封闭包裹图标
Package Export Bus: Export Bus 轮廓 + 箭头包裹图标
Package Unpacking Bus: Bus 轮廓 + 打开包裹图标
```

## 6. UI 与图标

需要的 UI 图标：

```text
颜色选择
marker
容量档
阻挡模式
自动导出输出包裹
打包一次
保留/覆盖/清除 marker
打包过滤
拆包过滤
拒绝原因状态
```

UI 风格：

```text
深灰背景
浅灰/白色 AE2 面板
Fluix 紫蓝高亮
清晰 ghost slot
彩色小灯表示包裹颜色
ME Packager GUI 使用 AE2 ScreenStyle 加载：style JSON 放在 assets/ae2/screens/appliedpackaging/me_packager.json，背景贴图放在 assets/appliedpackaging/textures/gui/mepackager.png。
ME Packager 过滤区背景只绘制基础启用行；容量卡解锁的可选行按 AE2 高版本 slot background 效果由代码渲染，禁用状态使用新版 0.2 alpha，不把全部 slot 烘进背景图。
ME Package Assembler GUI 使用 AE2 ScreenStyle 加载：style JSON 放在 `assets/ae2/screens/appliedpackaging/package_assembler.json`，背景 atlas 放在 `assets/appliedpackaging/textures/gui/mepackageassembler.png`，并保持用户提供的 256x256 atlas 原图。上半部分的名称输入、颜色 swatch、marker 槽和右上容量元件槽按原图坐标接入与 ME Packager 一致的逻辑；自动导出等配置开关走 AE2 左侧悬浮 toolbar；右侧升级区走 AE2 `UpgradesPanel`。
ME Package Assembler 的下半部分为输入/输出同步滚动区与下半区样板槽；可见窗口为左侧 4x4 输入格和右侧 4 个输出格。滚动条位于输入栏左侧，参考 AE2 样板终端 processing 模式的小滚动条；滚动输入/输出槽背景由客户端按 AE2 slot background 风格绘制，不烘进 atlas；atlas 提供面板、标题区、下半区样板槽、容量槽、marker 槽、左侧滚动条轨道、滚动槽容器和玩家背包区域。
```

## 7. 资源验收

必须满足：

```text
16x16 item 图标颜色可辨识
17 色包裹在 JEI/创造栏中能快速区分
机器模型无 missing texture
方块破坏粒子和 item model 正常
语言文件无缺 key
包裹图标不像普通纸箱，也不像 Create 黄铜机械
```

每组资产应保留：

```text
brief
来源或生成提示
材质文件
模型文件
blockstate/item model 文件
预览或截图
验收记录
```

## 8. 执行文件

资产生成和验收使用 `docs/assets/` 下的执行文件：

```text
docs/assets/palette.md
docs/assets/acceptance.md
docs/assets/asset-briefs/packages.md
docs/assets/asset-briefs/machines.md
docs/assets/asset-briefs/terminal-and-buses.md
docs/assets/asset-briefs/ui-and-icons.md
docs/assets/contracts/package_items.yaml
docs/assets/contracts/me_packager.yaml
docs/assets/contracts/package_assembler.yaml
docs/assets/contracts/terminal_and_buses.yaml
docs/assets/contracts/ui_icons.yaml
scripts/verify-assets.ps1
scripts/test-assets-audit.ps1
```

当前 5 个 contract 已通过本地 `assetgen validate-contract`。
当前发布资源 PNG 和 package_box 模型门禁由 `scripts/verify-assets.ps1` 自动检查：常规 item/block 资源为 32x32，package_box 六面贴图为 10x8 或 10x10，Create-style 临时打包机细节贴图可为 16x16，GUI icon 与 AE2 part 资源为 16x16，root/gui logo 为 128x128，ME Packager 与 ME Package Assembler GUI atlas 为 256x256，要求资源 PNG 使用 RGBA color type，并拒绝全透明或整张单一 RGBA 像素的占位图；package_box 模型还会检查 10x10x8 bounds、3D item parent、cutout_mipped render type、marker custom-render override 和每个 face 使用 full-face uv [0,0,16,16]；普通不透明 block/part 模型还会检查不得声明 render_type。

## 9. 当前资产交付状态

已交付：

```text
17 个包裹 item 已切换为 10x10x8 3D package_box 模型和 package_box_pixel_v7 的 85 张六面贴图；包裹掉落实体共用该 item model
package_pattern 与 packaged_processing_pattern 仍使用 32x32 item 图标
ME Packager 临时切换为 Create Packager 同款模型/贴图资源
ME 包裹装配室仍使用 32x32 block textures/blockstate/block model/item model，并新增 256x256 GUI atlas `textures/gui/mepackageassembler.png`
包裹样板终端、包裹存储总线、包裹输出总线、包裹拆包总线 32x32 textures/model
Package Pattern Terminal 玩家入口使用 AE2 cable part item，并已交付自有 16x16 part body/front/back/sides/overlay mask 材质与 part model
14 个 16x16 GUI 图标
128x128 GUI logo.png
docs/assets/reports/*.md
```

材质质量修订：

```text
旧版初稿因生产质量不足被退回。
主线程 clone AE2 forge/v15.4.10 源码到 build/reference/ae2，仅作为临时参考。
从 AE2 item/machine/part/gui 资产生成 reference sheet 到 build/asset-reference/ae2。
调用 ImageGen 基于 AE2 reference sheet 生成 Applied Packaging 风格概念板。
4 个 subagent 分别负责 packages、machines、terminal-and-buses、ui-and-icons 二轮重做。
最终提交资源不复制 AE2 像素，只参考石英面板、深灰框架、Fluix 高光、终端网格和 GUI 槽位语言。
```

主线程已验证：

```text
5 个 asset contract 均 validate ok
143 个 PNG 尺寸符合预期
scripts/verify-assets.ps1 通过，确认必需 PNG 存在、路径归类正确、PNG header 有效、RGBA 类型、可见非占位像素内容和尺寸符合规格，并覆盖 ME Package Assembler GUI atlas
70 个 JSON 可解析
block model 坐标保持在 0..16
texture/model 引用存在
抽样视觉检查通过
.\gradlew.bat build 成功
.\gradlew.bat runData 成功
.\gradlew.bat runGameTestServer 成功，112 个必需 GameTest 全部通过
.\gradlew.bat runClientSmoke 成功，Package Pattern Terminal 使用真实 AE2 part host 打开
```

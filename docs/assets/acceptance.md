# 资产验收

本文定义 subagent 交付和主 agent 整合时的资产验收标准。

## 1. 通用检查

每个资产包必须包含：

```text
目标资源文件
对应模型或 blockstate 文件
docs/assets/reports/<asset-package>.md
生成提示或绘制说明
预览路径或渲染记录
已知限制
```

文件要求：

```text
PNG 可被读取
透明背景 alpha 干净
文件路径使用 appliedpackaging namespace
JSON 可解析
语言 key 与物品/方块 id 对齐
不覆盖其他 subagent 的文件
不修改 Java、Gradle 或核心设计文档
```

## 2. Item 图标与包裹模型

必须满足：

```text
package_pattern 与 advanced_processing_pattern 使用用户提供的原字节 16x16 RGBA PNG
17 色包裹不使用独立平面 item PNG，item model 直接引用 3D package_box 模型
包裹 front/back/side 贴图为 10x8，top/bottom 贴图为 10x10
每个 package_box face 使用独立完整贴图，模型 JSON 必须声明 full-face uv [0,0,16,16]
不使用 atlas 裁切，不使用基础盒体与束带重叠层
存在物品 marker 时，marker 在前脸右下角、距外边框 1px 的 4x4 框内以 3x3 居中叠加，四周保留 0.5px 内边距
透明背景
图标居中，有 1-2 像素安全边距
17 色包裹只改变束带/封签色，不改变主体轮廓
GUI 变换统一为 rotation [30,135,0]、scale [0.75,0.75,0.75]，north/front 面位于屏幕左侧；有无 marker 不得改变大小或朝向
package_pattern 与 advanced_processing_pattern 在小尺寸下可区分
```

路径：

```text
src/main/resources/assets/appliedpackaging/models/item/<item_id>.json
src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png
src/main/resources/assets/appliedpackaging/textures/item/advanced_processing_pattern.png
src/main/resources/assets/appliedpackaging/models/item/package_box/<color>.json
src/main/resources/assets/appliedpackaging/textures/block/package_box/<color>/package_box_<face>.png
```

## 3. 方块和机器

必须满足：

```text
方块纹理来源可追溯到 front/right/top/isometric 概念或手绘说明
模型坐标在 0..16
无 missing texture
物品模型能正常显示
破坏粒子使用合理纹理
前向面能明确表示功能
```

路径：

```text
src/main/resources/assets/appliedpackaging/textures/block/<block_id>*.png
src/main/resources/assets/appliedpackaging/blockstates/<block_id>.json
src/main/resources/assets/appliedpackaging/models/block/<block_id>.json
src/main/resources/assets/appliedpackaging/models/item/<block_id>.json
```

## 4. UI 和图标

必须满足：

```text
图标语义清楚，不依赖文字标签
8x8、12x12、16x16 尺寸下仍可识别
颜色、marker、容量、阻挡、过滤、打包、拆包状态可区分
颜色选择器默认、None 与选中背景在 8x8 下可区分；选中只改变格内背景，不添加外边框或 hover 像素
与 AE2/Applied Packaging 面板风格一致
Advanced Pattern Encoding Terminal 使用唯一 ScreenStyle 入口承载高级/包裹两页，但必须绘制两套完整 profile：两行网络库存时高级页 217x250（195px 主体加 22px 右侧标签区）、包裹页 195x233；高级编辑框宽 146px，包裹面板保持 124x66 原尺寸且不得拉伸。模式切换后同一 Screen 重新居中并重排背景、标题、搜索、滚动条、玩家栏、载体和当前页槽位。右侧两个模式标签必须具有 current-AE Pattern Encoding Terminal 同款 22x22 horizontal normal/selected/focus 背景、21px 步进与 `(3,3)` ItemStack 偏移，并贴合各自 profile 的编码区右边缘。Screen 不得出现任何 `VIEW_CELL` 槽或显示元件面板；普通 AE2 Pattern Encoding Terminal 保持原始视觉，不增加包裹按钮或覆盖资源
```

路径：

```text
src/main/resources/assets/appliedpackaging/textures/gui/<screen_id>.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/<icon_id>.png
```

高级样板终端 Part 必须满足：

```text
物品形态使用固定 AE2 neoforge/v19.2.17 六段 item display 几何；世界 Part 使用同一几何沿 Z 轴 -7 平移后的 cable-part 版本，不得回退到与 15.4.10 同形的两段 part display base
dark、medium、bright 三张 16x16 RGBA 用户遮罩保持原字节，并在同一终端状态下按 tint index 1/2/3 同时渲染
通电模型只使用 Forge 1.20.1 `forge_data` 全亮字段，断电模型不带全亮字段，不残留 `neoforge_data`
断电、通电和有频道状态层均为 v19 四段式几何
物品模型具有 AE2 part 变换、v19 六段几何和完整的四个 tint layer；世界 Part 同样保留 monitor_colored tint index 4
基础外壳与状态贴图具有固定上游提交、逐文件哈希和 LGPL 来源记录
```

## 5. 主 agent 验收命令

整合后至少运行：

```powershell
.\gradlew.bat build
.\gradlew.bat runData
.\gradlew.bat runGameTestServer
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1
```

当方块/GUI 已注册后追加：

```powershell
.\gradlew.bat runClient
```

客户端验收应检查：

```text
创造栏和物品栏无 missing texture
包裹 17 色可区分
机器方块无紫黑贴图
GUI 元素无错位
日志无资源加载错误
```

`scripts/verify-assets.ps1` 会检查发布资源 PNG 的必需文件、已知资源路径、RGBA PNG header 和尺寸：普通 item/block 为 32x32，包裹盒体 face 为 10x8 或 10x10，ME Packager 主体 atlas 为 64x64、帘子为 16x16、双周期传送带为 32x32，GUI icon 与 AE2 part 为 16x16，root/gui logo 为 128x128，ME Packager 与 ME Package Assembler GUI atlas 为 256x256；同时检查 17 色 package_box 模型仍为 v7 的 10x10x8 bounds、3D item parent、cutout_mipped render type、marker custom-render override，且 faces 声明 full-face uv [0,0,16,16]；检查合并后的 Advanced Pattern Terminal ScreenStyle 包含包裹页小滚动条、隐藏继承 view-cell 区域且不覆盖 AE2 原生 terminal style/atlas；检查高级终端 v19 世界/物品模型的元素数、三层 tint 顺序、Forge 全亮字段、四段状态灯和用户遮罩哈希；ME Packager blockstate 必须且只能声明四个水平 `facing` 变体，并让完整物品模型继承 `minecraft:block/block` 的标准方块显示变换；普通不透明 block/part 模型不得声明 render_type。修改资产验收脚本或尺寸规则时同步运行 `scripts/test-assets-audit.ps1`。

序列缓存器追加要求：`textures/block/sequence_buffer/faces/` 必须包含从用户 64x64 RGBA 源图无损拆出的 16 张可见非占位 16x16 RGBA；所有文件必须被实际模型引用。blockstate 必须覆盖 `unformed`、`unformed_directed`、`endpoint`、`member`、`member_directed` 五种视觉状态，并以 `tail` 和 `sequence_direction` 覆盖中间/尾部及上下、四个水平尾向；生成矩阵必须包含 X/Y/Z 三轴、六个方块 facing、57 个显式模型和 58 个 multipart 项。有方向成员只声明与结构轴垂直的可见组合，侧面箭头必须指向方块自身正面；尾部侧面的封口朝结构外、开口朝主方块。模型保持一个 `0..16` 完整方块 cuboid，物品模型使用未成型外观，并经 `minecraft:block/block` 继承标准三维物品展示变换，不得显示为正面的平面方格。

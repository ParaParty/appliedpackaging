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
包裹 GUI display 使用 `rotation [30,135,0]`、`translation [0,2,0]` 与 `scale [0.75,0.75,0.75]`：盒体几何范围 `y=1..9`、中心为 `y=5`，Y 差值经过 30° 旋转和 0.75 缩放后的屏幕投影约为 1.95 个模型像素，因此使用 2px 位移使整个图标居中；有 marker 与无 marker 模型必须使用同一变换。`PackageItemRenderer` 中进入递归普通模型渲染前的 `translate [0.5,0.5,0.5]` 只抵消两层 `ItemRenderer` 各自的 `-0.5` 原点变换，不是额外视觉偏移。
包裹存在物品 marker 时，静态模型仍渲染盒体，客户端 renderer 在前脸右下角、距外边框 1px 的 4x4 标记框内叠加 3x3 marker item；marker 中心必须与该 4x4 框中心对齐，保持 0.5px 内边距。物品 GUI 中按住 Shift 时，Forge item decorator 还要在原包裹图标左下角叠加一个独立 8x8 marker 物品图标，避开右下角数量文本；无 marker 包裹不绘制该附加层。
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
采用 AE2 neoforge/v19.2.17 分子装配室的 2px 框架与 12x12x12 内腔模型。
完整表面使用用户提供的 16x16 package_assembler.png；工作时叠加同版本分子装配室发光层。
工作动画在内腔显示当前正在生成的包裹，并生成向中心汇聚的装配粒子；静止时不显示动态层。
方块属性必须保持 noOcclusion 且不作为红石导体，避免空心 cutout 内腔错误剔除相邻方块表面而显示黑底。
AE2 派生模型、发光贴图和渲染语义保留 LGPL-3.0-or-later 来源记录。
```

ME 打包机：

```text
正式模型以 `docs/assets/source/me_packager/model.bbmodel` 为可编辑源，源模型工作口朝本地 +X/east；运行时只按水平 `facing` 旋转主体、传送带、帘子和包裹。blockstate JSON 中 north/east/south/west 对应 Y 旋转 270/0/90/180 度；BER 的 `PoseStack` 采用相反符号的等效旋转 90/0/270/180 度。底部与模型背面是固定 AE 接线面；背面随 `facing` 旋转，底部保持不变。
主体由 6 个 cube 组成：1px 底板、4px 后部模块、反向内表面、1px 黑色背板和两条侧框；保持用户模型的空心结构与 AE2 风格分层边框。
传送带为独立 1 个 cube，几何范围 x=1..16、y=1..2、z=2..14；只渲染上表面和工作口正面。单个 UV 窗口严格由上表面 15px 和工作口正面 1px 连续组成；`belt_scroll.png` 为 32x32 双周期贴图，工作期间由 renderer 仅修改 U/X offset，禁止逐帧替换 PNG。16px 周期内的滚动相位由方块实体保存和同步；停止时不得归零，后续动画从已保存相位继续。滚动相位直接使用包裹移动的同一进度增量换算，包裹完整进出 9px 时传送带也恰好移动 9px，不使用固定每 tick 位移。
帘子由 4 条 1x12x3 flap 组成，静态位置内缩在 x=3..4；运行时复用一个 `curtain_flap` baked model，在各自顶部转轴绕 Z 轴摆动，拆包向内、打包向外。帘子保存独立的有符号偏转和回弹速度，包裹实际移动速度只作为推动权重，之后由自身弹性与阻尼回到垂直位置；不得把完整开合曲线压缩到工作周期内，工作结束时允许保留未完成的回弹并继续自然恢复。
主体、传送带和帘子使用 cutout_mipped；主体始终由按 `facing` 旋转的 blockstate baked model 渲染，传送带、帘子和包裹的 BER 使用同一水平变换。包裹在该机器局部变换内额外绕 Y 轴旋转 `+90°`，把 item `FIXED` 变换后的 +Z 正面转向模型工作口 +X；四个方块朝向都必须保持包裹正面朝工作口。底盘始终保持水平、背板始终保持竖直。
选择轮廓与碰撞体由底板、4px 后部模块、两条 2px 高侧框和传送带实体组成，并按 `facing` 水平旋转；帘子和动画中的包裹不参与碰撞。
包裹使用最长 20 tick 的可见移动窗口：实际工作周期超过 20 tick 时只在最后 20 tick 播放，20 tick 及以下时覆盖整个工作周期；本地内部点为 `x=1/16`，使包裹约 3px 的前半深度在端点处收进 `x=3..4/16` 的帘子后，拆包在整个窗口内从工作口移入，打包在整个窗口内从内部移到 `x=10/16,z=8/16`，即去除 4px 后部模块后的 12x16 前部区域中心。包裹模型范围为 `y=1..9`，`FIXED` 变换缩放 0.5，机器额外缩放 1.49；BER 必须由这些参数反算渲染原点，使模型底面精确落在传送带顶面 `y=2/16`。包裹在工作态和静止态都使用 stencil immediate pass，帘子使用独立 stencil immediate pass；裁剪盒使用模型本地坐标 `x=1/16..16/16,y=0..1,z=0..1` 并随 `facing` 旋转，只保留从工作口向内 15px 的体积，禁止进入最后 1px 背板；传送带不进入 stencil pass。stencil immediate pass 与裁剪盒必须复用 renderer 生命周期内的原生 `BufferBuilder`，禁止在每帧或每台机器的 `render` 调用中创建新缓冲。
用户提供的 `base.png`、`curtain.png`、`belt_scroll.png` 必须原字节复制；导入脚本只拆分模型、换算 Java block model UV 和生成运行时 JSON，不重绘像素。
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

序列缓存器：

```text
方块 ID 与资源根：appliedpackaging:sequence_buffer
五个视觉状态：unformed、unformed_directed、endpoint、member、member_directed；member 另按 tail 区分中间/尾部模型
blockstate 由 state/facing/axis/sequence_direction/tail 组合选择显式六向模型；endpoint 只按 sequence_direction 定向，不能用结构方向覆盖方块自身 facing
unformed 是完整独立外壳；unformed_directed 在六个方向之一显示输出/指向口
endpoint 只在指向结构内部的一侧显示端点连接口，不绘制自动输出口
member 在结构轴两侧使用连续连接材质；member_directed 额外在垂直于轴的一侧显示输出口
物品模型使用 unformed 独立外壳，不显示虚假的连接或输出方向；外壳必须继承 `minecraft:block/block` 的标准 GUI/手持/地面三维变换，物品栏中不得正投影成单张平面方格
```

正式资源路径：

```text
assets/appliedpackaging/blockstates/sequence_buffer.json
assets/appliedpackaging/models/block/sequence_buffer/*.json
assets/appliedpackaging/models/item/sequence_buffer.json
assets/appliedpackaging/textures/block/sequence_buffer/faces/*.png
assets/appliedpackaging/textures/gui/sequence_buffer.png
assets/appliedpackaging/textures/gui/sequence_buffer_side.png
assets/ae2/screens/appliedpackaging/sequence_buffer_main.json
assets/ae2/screens/appliedpackaging/sequence_buffer_side.json
```

序列缓存器正式贴图来自 `E:/resources/textures/appliedpackaging/ret/sequance_buffer_all.png`，源图固定为 64x64 RGBA、4x4 网格，每格 16x16。拆分时不得缩放、重采样、调色或量化；16 个格子按从上到下“无方向、方向正面、方向侧面、方向背面”，从左到右“未成型、中间成员侧面、边缘/尾部成员侧面、特殊面”命名。特殊列依次是主方块背面、连接遮挡面、主方块侧面和尾部背面。方向侧面原图的箭头沿贴图 `+U`；每个显式模型必须旋转该面，使箭头指向本方块 `facing` 的正面。中间侧面贴图的 `+V` 沿结构正轴，边缘/尾部侧面贴图的 `+V` 朝结构内部、`-V` 封口朝外；为同时满足箭头和尾部封口方向，必要时允许仅镜像 V。五类状态、X/Y/Z 轴、六个 facing、中间/尾部和六个结构方向的全部合法组合都要经过资源审计；方向与结构轴平行时保留逻辑方向，但 blockstate 不选择带可见输出面的成员模型。

序列缓存器主 GUI 底图来自用户处理后的 `E:/resources/textures/appliedpackaging/ret/seq_buffer_ui.png`，运行时副本保持 256x256、32-bit ARGB 与原字节 SHA-256 `075E3329882A3AAE7FE7EBDAAB32EBF799531DC4224F3F37B563CD6B537A2C67`。可见主区域固定 `[0,0,195,170]`；用户已删除顶部 3x9 槽框，Screen 必须从 `package-storagebus-sprites.png [0,64,18,18]` 在 `(slot.x-1,slot.y-1)` 动态绘制，禁用槽使用 0.2 alpha，不能重新写回底图。滚动条位于 `(175,18)`、高 54，只在成员数超过 27 时具有非零范围。

侧面 GUI 底图 `sequence_buffer_side.png` 原字节复用 AE2 neoforge/v19.2.17 `textures/guis/me_chest.png`，运行时为 256x256、32-bit ARGB，SHA-256 `2749D7BDAB5E3B9BFF240B6F618AB55AE14A3C2252D9DEB63D959874456D91A0`，可见区域 `[0,0,176,168]`，中央存储槽为 `(80,37)`。第一版两套界面均不得绘制独立 3x3 过滤面板或过滤假槽；五项模式设置复用项目的 AE2 current-style 竖向按钮栏，升级面板统一使用 `{right:2,top:0}` 附着主面板右侧。主界面滚动条组件矩形为 `(175,18,12,54)`，使用 AE2 `Scrollbar.DEFAULT` 的 12x15 enabled/disabled handle；底图轨道外框为 `x=178..183`，因此 handle 在 x=175 时左右各外扩 3 px 并保持居中，禁用状态不得隐藏。

## 5. 终端与总线资产

独立包裹样板终端已取消；包裹样板编辑入口合并到 Advanced Pattern Encoding Terminal。AE2 原版 Pattern Encoding Terminal 不增加包裹模式，也不覆盖其 ScreenStyle、底图或按钮。

合并终端的包裹页：

```text
唯一 ScreenStyle 入口: assets/ae2/screens/appliedpackaging/advanced_pattern_encoding_terminal.json
共享 full-screen base: assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png
高级页和包裹页共享同一个 195px 宽 Screen/Menu、网络数据、左侧工具栏与 192px bottom。两行网络库存时两页都为 195x245；切换只替换中部 132x78 模式面板并重排当前页 slot，不调用 resize/init，不重新居中。右侧模式按钮使用 current-AE Pattern Encoding Terminal 的 22x22 `HORIZONTAL_TAB` / selected / focus sprites，固定 `left=173`、相邻步进 21px，包裹在上、高级在下；`package_bus_vertical_buttons_bg.png` 只用于左侧工具栏，不用于模式标签。菜单不创建 `VIEW_CELL` 槽，ScreenStyle 把继承的 view-cell 区域移出可见区，不绘制显示元件面板或空槽。
GUI atlas: assets/appliedpackaging/textures/gui/pattern_mode_packaging.png
atlas 固定为 256x256 RGBA；包裹面板位于 `[0,0,132,78]`，高级面板位于 `[0,128,132,78]`，后者与 full-screen base 的 `(8,68,132,78)` 逐像素相同。运行时把包裹面板原尺寸绘制到 `left=8,bottom=177`；不得缩放、裁成旧 124x66 或另换 full-screen base。运行时副本分别与用户 `adv-pattern-terminal-base.png`、`pattern_mode_packaging.png` 保持字节一致，SHA-256 为 `9586E6422D039A58C1188F5DA4F504FDE04870E4383F29E56FA9FE2752CCDD00`、`65DE82E33052D1F941182863D8303C4D22BA52C07528AC69702B9BA685153096`。
高级输入/输出首槽分别为 `(21,bottom=164)`、`(119,bottom=164)`；高级列头颜色/清空/循环按钮为 `bottom=174`，列操作按钮为 `bottom=173`，不得与输入框 `y=80` 顶边重叠。高级槽不得以纯色重绘槽内区：动态启用的输入列必须使用 `advanced_pattern_encoding_terminal_states.png [192,192,18,18]` 的 AE2 v19 `SLOT_BACKGROUND` 完整精灵，并绘制到 `(slot.x-1,slot.y-1)`；未启用列不叠加内容，输出槽使用 full-screen base 中已有的槽位材质。包裹 3x3 输入首槽为 `(24,bottom=164)`，marker 为 `(109,bottom=164)`，自动输出物品原点为 `(112,bottom=140)`。包裹滚动条为 `(15,bottom=164)`，清空/颜色按钮为 `(80,bottom=164)` / `(90,bottom=164)`；两页共同的空白样板、已编码样板与 Encode 为 `(150,bottom=165)`、`(150,bottom=118)`、`(150,bottom=145)`。marker 为空时叠加用户 `package-storagebus-sprites.png` 的 `(32,16,16,16)` 自绘槽图标，并提供 current-main hover 与双行说明 tooltip。
清空按钮使用新版 states sprite `[224,200,8,8]`；用户新增的转置按钮图标固定为 `advanced_pattern_encoding_terminal_sprites.png [16,0,8,8]`，按钮与清空按钮同 x 并位于其正下方，不缩放或重绘该 8x8 像素。颜色设置按钮复用统一 `PackageColorPicker.TriggerButton` 并在内部绘制当前颜色。模式标签不得渲染 ItemStack：高级标签使用 `advanced_pattern_encoding_terminal_states.png [16,32,16,16]` processing/furnace sprite，包裹标签使用 `advanced_pattern_encoding_terminal_sprites.png [32,0,16,16]` package sprite，按 horizontal tab 的 `(3,2)` 原点绘制。
共享 sprite 中的 AE2 高版本滚动条适配像素以 LGPL-3.0-or-later 例外标记。
```

高级样板终端：

```text
物品模型采用 AE2 neoforge/v19.2.17 的六段 item display 几何；世界 Part 不使用与 15.4.10 实际同形的 v19 `part/display_base`，而是把同一六段 item display 几何沿 Z 轴平移 -7 到 cable-part 坐标，并与 v19 四段状态灯组合，确保放置形态与已更新物品形态使用同一新版模型；固定来源为提交 79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a
正面 dark、medium、bright 是同一终端状态下同时叠加的三张染色遮罩，不是断电、通电和有频道三态；世界模型与物品模型都保留 tint index 1/2/3 及 v19 monitor_colored 的 tint index 4
用户原图 adv_pattern_encoding_terminal_dark.png、adv_pattern_encoding_terminal_medium.png、adv_pattern_encoding_terminal_bright.png 以 16x16 RGBA 原字节接入 textures/part/advanced_pattern_encoding_terminal_dark.png、_medium.png、_bright.png；SHA-256 分别为 36D633037B7B40A5B289457533F63B817F098F9DDBF0F99CBCCA47002D12D4A3、FE7A93FC055AD74AF9113711F799E56FE600A44E129B3D354D9F89C9BF2CCB98、1488EC1F42AFFA736CB5E5687C29B9F4EC7A41C7AAC2FD28C86AE1E6EF4914CF
通电/有频道时三层遮罩使用 Forge 1.20.1 `forge_data` block_light=15、sky_light=15；断电时保留环境光。状态灯采用 v19 四段几何，不回退到 AE2 15.4.10 的整圈状态层
基础外壳、monitor_colored 和三种状态灯贴图从固定 v19.2.17 源原字节复制；完整模型映射、哈希和 LGPL 来源记录位于 META-INF/licenses/ae2-terminal-part-source.txt
高级终端高级页编码 `advanced_processing_pattern`，包裹页编码 `package_pattern`；两种产物均使用独立 item id/model，不复用 AE2 原版样板物品 id
ScreenStyle: assets/ae2/screens/appliedpackaging/advanced_pattern_encoding_terminal.json
GUI texture: assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png
GUI base 与 sprite atlas 均固定为 256x256 RGBA；两行网络库存时两个模式都是 195x245，使用同一个 `advanced_pattern_encoding_terminal.png` full-screen base 和 192px bottom。ScreenStyle 同时声明两页小滚动条，但不声明可见的 view-cell 面板。
终端标题、搜索框、9 列 AE 网络库存、网络滚动条、玩家物品栏与样板编码区坐标按 AE2 1.21.1 Pattern Encoding Terminal 布局适配到 AE2 15.4.10 ScreenStyle。
每个包裹列包含 81 个逻辑处理输入位置，中间区同时显示 4 列 x 3 行；菜单只为 4 个可见列创建 4×81 个窗口 FakeSlot，并随底部水平滚动同步重映射，底部滚动条覆盖最多 81 个包裹列。
列间距为 1px；每个启用列显示 8x8 颜色 swatch 与 8x8 编辑按钮，第一未启用列只显示加号，后续列保持禁用底色且不绘制 ghost 物品。
`advanced_pattern_encoding_terminal_sprites.png` 是用户提供的本地 sprite atlas，其小滚动条/紧凑按钮包含 AE2 高版本 GUI 适配像素；`advanced_pattern_encoding_terminal_states.png` 从 AE2 当前 main/1.21.1 同字节 `states.png` 复制，用于主产物叠加层、样板槽背景、Encode 按钮、右上角合成状态按钮与左侧工具栏 18x20 normal/hover/focus 背景。左侧工具栏同时复用 current-main `vertical_buttons_bg`，ScreenStyle 锚点使用高版本 `left=3,top=1`，按钮间距为 6px；高级终端与 Package Bus 必须共用 `ModernVerticalToolbar` 的布局、外框和按钮背景绘制，不得为高级终端另设 `IconButton` / `VerticalButtonBar` Mixin。两行 base 不含独立中间行，`advanced_pattern_encoding_terminal_middle_row.png` 使用末行顶部 17px 与首行底边 1px 合成 195x18 可重复中间段；`advanced_pattern_encoding_terminal_scrollbar.png` 将新版 AE2 原字节 big scroller 启用/禁用图标装入 1.20.1 Blitter 兼容的 256x256 atlas。以上资源均以 `LGPL-3.0-or-later` 例外标记，完整许可证收录于 `META-INF/licenses/ae2-LGPL-3.0-or-later.txt`。
```

总线：

```text
Package Storage Bus 与 Package Unpacking Bus 均改为 AE2 cable part。
Package Storage Bus 使用 AE2 neoforge/v19.2.17 Storage Bus 模型、侧面、背面和状态灯，正面替换为用户 pacakge_storage_bus.png。
Package Unpacking Bus 使用同版本面板形态 Pattern Provider 模型、侧面和状态灯，正面与背面分别替换为用户 unpacking_panel.png 与 unpacking_panel_back.png。
两种 part 的物品模型同步使用对应新版几何和表面，不再复用当前依赖版本的 Storage Bus / ME P2P Tunnel item model。
两种总线 GUI 使用独立纹理：`textures/gui/package-storagebus.png`（用户提供且字节不变，主界面 `[0,0,176,253]`、工作槽/空进度框/活动进度 sprite 位于右侧空闲区）与 `textures/gui/package-storagebus-sprites.png`（由原 `sprite.png` 扩展）。不得把二者合图或向其中烘入 AE2 像素；sprite 图集除本轮颜色选择器三个指定单元外保持原像素。
Package Storage Bus 不绘制右上工作包裹槽和进度条；Package Unpacking Bus 绘制。
两者右上优先级标签按 AE2 当前 main Storage Bus 使用 `(152,-5,20,20)`。`states.png`、`extra_panels.png`、`vertical_buttons_bg.png` 分别原样复制为 `ae2-states.png`、`package_bus_extra_panels.png`、`package_bus_vertical_buttons_bg.png`，独立用于新版 toolbar/priority、升级与工具箱面板、竖向工具栏外框，并记录 LGPL 来源。Package Storage Bus 左侧工具栏绘制 Help、清空、Partition Storage 与四个存储设置按钮；Package Unpacking Bus 只绘制 Pattern Provider Help、清空和阻挡模式三个按钮。右侧 5 格升级面板使用 current-main 的 `right=2, top=0`；空升级槽图标使用 `ae2-states.png` 的 `(240,208,16,16)`，不再让 AE2 15.4.10 的旧灰阶 `BACKGROUND_UPGRADE` 混入新版面板。卸货总线运行时把原背景右侧 `[176,0,18,18]` 工作槽与 `[196,0,6,18]` 空进度框绘制到 `(119,8)` / `(139,8)`，再把 `[176,32,6,18]` 活动进度 sprite 按 15 级进度从底部原像素裁切到同一进度框；禁止取单色像素拉伸或代码重新着色。存储总线不绘制该层。
七行过滤槽从底图第一行 `y=29` 开始、步长 18px；8px 模糊/反转/颜色按钮统一使用相对行顶 2px 的固定上边距，不做垂直居中。统一拾色弹窗固定为 89x23：分隔线左侧 Fluix/None 在 `(3,3)`、`(3,12)` 上下排列，分隔线右侧 16 色从 `(15,3)` 开始按 8x2 排列；None 仅在过滤区显示，隐藏时不回收其位置。禁用过滤行以用户 sprite `(0,64,18,18)` 的 `0.2` alpha 叠加实现，不交付独立 disabled-slot 贴图。用户 sprite `(32,16,16,16)` 是自绘 marker 空槽图标，不属于 AE2 派生像素；总线按行透明度绘制，两台机器与高级终端包裹页按全不透明绘制。颜色格默认 Fluix、None 与选中背景使用同一图集 `(48,0,8,8)`、`(56,0,8,8)`、`(48,8,8,8)`；三个单元从用户截图按 6x 网格精确还原，分别使用 `#C0C0C0/#915DCD/#E2A3E3`、`#C0C0C0/#696D88/#ADB0C4`、`#F2F2F2/#ADB0C4`，选中只替换格内背景，不画额外边框或 hover 状态。更新后 sprite SHA-256 为 `632A686B6F8EC7B712326DC52E639CE43CF8E1B55C44D00309B62B672B766635`；用户 background 与三张 AE2 原样资源继续以各自 SHA-256 门禁防止无意改写。

2026-07-13 更新后的用户 `package-storagebus.png` 已接入，源文件与运行时副本 SHA-256 均为 `506BE44EF826C14C1DBE37C076EDC7955C0DBFE35A7DB9B157EABA8E241787DE`。新图相对旧图将 10408 个 `#ADB0C4` 像素恢复为 AE2 current-main 主体色 `#CBCCD4`，并将 854 个 `#CBCCD4` 外圈像素恢复为深色边框 `#413F54`；排除自定义中心 `x=7..168, y=28..154` 后，主界面 `[0,0,176,253]` 与 current-main `storagebus.png` 的可见像素差异为 0。右侧工作槽 `[176,0,18,18]`、空进度框 `[196,0,6,18]` 与活动进度 sprite `[176,32,6,18]` 均与旧图逐像素一致，七行过滤区和独立 sprite 均未被破坏。透明像素仅保留用户图既有的透明黑 RGB，与 current-main 的透明白 RGB 在 nearest-neighbor GUI 中均不可见。

当前实际插入的 fuzzy/inverter/capacity/speed card 仍由依赖 AE2 15.4.10 的 `card_fuzzy.png`、`card_inverter.png`、`card_capacity.png`、`card_speed.png` 渲染；这四张物品贴图与 current-main 均不相同。若要求“装入卡后”也逐像素接近新版，需要另行提供或授权加入这四张新版卡图，并明确采用仅本界面覆盖还是全局 AE2 物品资源覆盖；本轮只修复空升级槽占位，不擅自覆盖 AE2 物品贴图。
```

## 6. UI 与图标

需要的 UI 图标：

```text
颜色选择
marker
容量档
输出模式
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
ME Packager GUI 使用 AE2 ScreenStyle 加载：style JSON 放在 assets/ae2/screens/appliedpackaging/me_packager.json，背景贴图放在 assets/appliedpackaging/textures/gui/mepackager.png。槽位 hover、右侧升级面板、工具箱面板与空升级槽使用 current-main 回移视觉；左侧按钮通过机器共用 `ModernUpgradeableScreen` 接入 `ModernVerticalToolbar`，与高级终端和 Package Bus 共用 current-main 外框、6px 间距、18x20 背景及新版图标绘制；容量元件空槽图标使用 `ae2-states.png (240,48,16,16)`；空 marker 槽使用用户 sprite `(32,16,16,16)` 与双行说明 tooltip。
ME Packager 过滤区背景只绘制基础启用行；容量卡解锁的可选行必须使用共用的 `package-storagebus-sprites.png [0,64,18,18]` 完整 slot sprite，由代码绘制到 `(slot.x-1,slot.y-1)`，禁用状态使用新版 0.2 alpha，不得用两块纯色 fill 仿画槽位，也不把全部 slot 烘进背景图。
ME Package Assembler GUI 使用 AE2 ScreenStyle 加载：style JSON 放在 `assets/ae2/screens/appliedpackaging/package_assembler.json`，背景 atlas 放在 `assets/appliedpackaging/textures/gui/mepackageassembler.png`，并保持用户提供的 256x256 atlas 原图。样板与容量元件槽并列位于顶部；右侧为主输出、只读副预览和进度条。输出模式配置保留 AE2 原生 `IconButton` 的状态、点击和 tooltip，并由机器共用 `ModernUpgradeableScreen` 接入 `ModernVerticalToolbar` 绘制 current-main 左侧工具栏；槽位 hover、右侧升级/工具箱面板与空升级槽使用 current-main 回移视觉，装配室只显示/接受 speed card；已编码样板与容量元件空槽图标分别使用 `ae2-states.png (240,112,16,16)`、`(240,48,16,16)`。原 atlas 中的旧颜色/marker 区域只作为背景保留，不注册无效果控件。
ME Package Assembler 的下半部分为输入/输出同步滚动区与下半区样板槽；可见窗口为左侧 4x4 输入格和右侧 4 个输出格。滚动条位于输入栏左侧，使用 `advanced_pattern_encoding_terminal_sprites.png` 的 current-AE2 7x15 小滚动柄，enabled/disabled 分别取 `[0,32,7,15]` / `[16,32,7,15]`。输入槽由客户端把 `package-storagebus-sprites.png [0,64,18,18]` 完整 sprite 绘制到 `(slot.x-1,slot.y-1)`，禁用状态为 0.2 opacity；sprite 的 1px 透明外围必须保留 atlas 所有外框与分隔线。菜单不得通过 `IOptionalSlot.isRenderDisabled=true` 触发 AE2 15 `Icon.SLOT_BACKGROUND`，Screen 也不得以纯色覆盖槽内区。atlas 提供面板、标题区、下半区样板槽、容量槽、左侧滚动条轨道、滚动槽容器、输出格和玩家背包区域，并保持用户原图 SHA-256 `118681C89EED078494D4C7371309543AC0F39184FE9F20D30B2D5A874AD5F18D`。
高级样板终端 GUI 使用 AE2 `ScreenStyle`，style JSON 位于 `assets/ae2/screens/appliedpackaging/advanced_pattern_encoding_terminal.json`，256x256 base/sprite/states atlas 位于 `assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal*.png`；主体保持高版本 AE2 的 195px 宽度、9 列网络库存和玩家栏基线，中间为 4 列 x 3 行可见真实输入、3 行可见真实输出、左侧行滚动条、底部列滚动条和纯颜色弹窗。每列逻辑容量为 AE2 默认 processing pattern 的 81 个输入槽。右侧空白/已编码样板逻辑槽分别位于 `left=150,bottom=165` 与 `left=150,bottom=118`；编码按钮 widget 位于 `left=150,bottom=145`，三者的 16px 内容、图标及点击区共用同一竖直中心线；slot hover 颜色严格采用 AE2 1.21.1 的 `0x669cd3ff` 填充和 `0xffdaffff` 边线。
Advanced Pattern Encoding Terminal 的包裹页在同一 full-screen base 上使用 `textures/gui/pattern_mode_packaging.png [0,0,132,78]`，固定绘制到 `left=8,bottom=177`。3x3 可见输入、marker、小滚动条和自动输出使用上一段固定坐标；自动包裹预览不使用 AE2 processing primary-output overlay 或 tooltip。第一枚模式标签位于网络行结束后 6px，第二枚向下 21px，图标只取 sprite。颜色按钮只打开统一拾色弹窗，关闭状态不显示大色板。全项目拾色弹窗复用 `package-storagebus-sprites.png` 的三个 8x8 颜色选择单元：Fluix/None 固定在分隔线左侧上下排列，16 色在右侧 8x2 排列，无标题；`allowNone` 只控制 None 绘制/命中，不改变布局。弹窗使用独立高 Z 层覆盖 slot/item，底层物品保持正常渲染。
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
当前发布资源 PNG、package_box 模型和 ME Packager 朝向由 `scripts/verify-assets.ps1` 自动检查：常规 item/block 资源为 32x32，package_box 六面贴图为 10x8 或 10x10，ME Packager 主体 atlas 为 64x64、帘子为 16x16、双周期传送带为 32x32，GUI icon 与 AE2 part 资源为 16x16，root/gui logo 为 128x128，ME Packager、ME Package Assembler、高级终端与包裹模式 GUI atlas 为 256x256，要求资源 PNG 使用 RGBA color type，并拒绝全透明或整张单一 RGBA 像素的占位图；package_box 模型还会检查 10x10x8 bounds、3D item parent、cutout_mipped render type、marker custom-render override、共享与 marked 模型一致使用 GUI `rotation [30,135,0]`、`translation [0,2,0]`、`scale [0.75,0.75,0.75]`，以及每个 face 使用 full-face uv [0,0,16,16]；ME Packager 会检查 blockstate 只声明四个水平 `facing` 变体、各朝向旋转正确，以及完整物品模型继承 `minecraft:block/block` 的标准显示变换；普通不透明 block/part 模型还会检查不得声明 render_type。

## 9. 当前资产交付状态

已交付：

```text
17 个包裹 item 已切换为 10x10x8 3D package_box 模型和 package_box_pixel_v7 的 85 张六面贴图；包裹掉落实体共用该 item model
package_pattern 与 advanced_processing_pattern 使用用户提供的原字节 16x16 RGBA item 图标
ME Packager 已切换为用户提供的正式空心框架模型、32x32 双周期滚动传送带和四条动态帘子
ME 包裹装配室仍使用 32x32 block textures/blockstate/block model/item model，并新增 256x256 GUI atlas `textures/gui/mepackageassembler.png`
包裹存储总线与包裹卸货总线使用 AE2 cable part 占位模型和共用总线 GUI atlas
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
.\gradlew.bat runGameTestServer 成功，138 个必需 GameTest 全部通过
历史人工客户端截图确认 Package Pattern Terminal 使用真实 AE2 part host 打开
```

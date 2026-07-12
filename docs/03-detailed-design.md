# 详细设计

## 1. 包裹颜色

颜色枚举：

```text
fluix
white
orange
magenta
light_blue
yellow
lime
pink
gray
light_gray
cyan
purple
blue
brown
green
red
black
```

注册物品 ID：

```text
fluix_package
white_package
orange_package
...
black_package
```

颜色权威来自 item id。`PackageData` 中不重复保存颜色，只在 canonical hash 输入中附带颜色值，避免不同颜色的相同内容误堆叠。

## 2. 包裹数据

```java
public record PackageData(
    int version,
    List<PackageEntry> contents,
    Optional<MarkerSpec> marker,
    long usedUnits,
    int usedTypes,
    String canonicalHash,
    int flags
) {}

public record PackageEntry(
    AEKey key,
    long amount
) {}

public record MarkerSpec(
    AEKey key,
    long amount
) {}
```

1.20.1 NBT 结构：

```text
tag.appliedpackaging.package.version: int
tag.appliedpackaging.package.flags: int
tag.appliedpackaging.package.hash: string
tag.appliedpackaging.package.used_units: long
tag.appliedpackaging.package.used_types: int
tag.appliedpackaging.package.marker: compound optional
tag.appliedpackaging.package.contents: list<compound>
```

每个 content compound：

```text
key_type: string
key: compound
amount: long
```

实现注意：

```text
AE2 ItemKey 可通过 item registry id + item NBT/components 表达。
AE2 FluidKey 在 1.20.1 需要保存 fluid registry id + NBT。
未知 AEKey 类型必须保守保存 type id 和 opaque payload。
如果无法还原，则包裹标记为 invalid，不允许拆包提交。
```

## 3. Canonical Hash

Canonical hash 输入：

```text
schema version
package item color
flags
marker canonical form
contents sorted by:
  key type id
  registry id
  normalized key payload hash
  amount
```

`PackageData` 创建时会先合并相同 AEKey，再按同一 canonical stack key 排序 contents；`PackageDataStorage.writeTag` 写入的是该规范化顺序，保证同内容不同输入顺序不仅 hash 相同，也能得到相同 NBT 并自然堆叠。

Hash 算法：

```text
SHA-256 -> hex lowercase
tooltip 显示前 8 位
NBT 保存完整 64 位 hex
```

用途：

```text
包裹堆叠判断
快速过滤缓存 key
调试 tooltip
GameTest 断言
```

## 4. 堆叠规则

包裹物品的 `isSameItemSameTags` / Forge 等价判断必须包含完整 NBT，因此同色、同 marker、同 contents、同 version、同 flags 的包裹才能自然堆叠。

包裹 ItemStack count 语义：

```text
stack count = 包裹个数
PackageData.contents = 每一个包裹的内容
```

拆包一组包裹时必须按 count 重复单包事务，不允许把一个包裹的数据乘 count 后作为一个大事务提交。

玩家手持包裹并蹲下右键时执行手动拆包：

```text
每次消耗手中 stack 的整叠同款包裹
内容全部为 AEItemKey 时，按 stack count 展开全部内容并拆成普通 ItemStack，优先进入玩家背包，溢出由 Forge 玩家发物品逻辑掉落
内容包含非物品 AEKey 时不执行，不消耗包裹，避免 fluid/未知 key 丢失
```

掉落实体包裹受到伤害时执行世界拆包：

```text
实体 ItemStack count 表示多个相同包裹
受伤拆包会按 count 展开全部包裹内容并掉落到世界
只有内容全部可转换为普通 ItemStack 时才移除包裹实体
```

## 5. 容量模型

容量单位：

```text
item: ceil(amount / maxStackSize)
fluid: ceil(amount / 1000 mB)
other AEKey: adapter 提供单位，未知类型默认 amount=1 为 1 unit
```

容量档：

| 档位 | 单包单位上限 | 类型上限 |
| --- | ---: | ---: |
| default | 9 | 9 |
| 16k | 16 | 16 |
| 64k | 64 | 63 |
| 256k | 256 | 63 |

容量元件识别：

```text
AE2 16k Storage Component / Cell -> 16k 档
AE2 64k Storage Component / Cell -> 64k 档
AE2 256k Storage Component / Cell -> 256k 档
```

最终实现以 AE2 1.20.1 注册名为准，先匹配 AE2 item registry id，再 fallback 到 tag。

## 6. 包裹套包裹

包裹不允许真实嵌套。任何进入打包计划的包裹都先展开为虚拟内容。

合并：

```text
红色包裹：铁锭 x64
蓝色包裹：铜锭 x64
机器颜色：Fluix

结果：
Fluix 包裹：铁锭 x64, 铜锭 x64
```

改色：

```text
红色包裹：铁锭 x64
机器颜色：蓝色

结果：
蓝色包裹：铁锭 x64
```

追加：

```text
红色包裹：铁锭 x64
散装：金锭 x64

结果：
红色或机器设定色包裹：铁锭 x64, 金锭 x64
```

Marker 策略：

```text
retain:
  所有源包裹 marker 相同则保留。
  marker 冲突则拒绝计划。

override:
  使用机器 ghost marker。

clear:
  输出包裹无 marker。
```

默认：`retain`。

当前核心实现：

```text
PackagePlanBuilder 接收 looseContents 与 sourcePackages。
sourcePackages 的 contents 会展开为虚拟内容后再创建新的 PackageData。
retain 模式下，所有非空 source marker 必须相同，否则返回 MARKER_CONFLICT。
override 模式使用传入 overrideMarker。
clear 模式移除输出 marker。
capacityProfile 在计划阶段检查 usedUnits 与 usedTypes，超限返回 CAPACITY_EXCEEDED。
```

## 7. ME 包裹装配室

职责：

```text
通过 AE2 ICraftingMachine/pushPattern 接收 Pattern Provider 推入的一批输入
读取普通/彩色/包裹/封装处理样板
生成一个或多个包裹
维护本机输入缓存与输出缓存
按分子装配室风格维护样板门禁、AE 网络能量驱动的合成进度和加速卡税率
按输出模式把输出口包裹导入 ME 网络、相邻方块或留在本机
```

不做：

```text
相邻库存打包
拆包
扫描所在 AE 网络库存
扫描相邻 Interface 子网
执行 ME 打包机逻辑
```

AE2 Pattern Provider 集成：

```text
package_assembler 暴露 appeng.capabilities.Capabilities.CRAFTING_MACHINE。
AE2 Pattern Provider 与装配室相邻时，通过 ICraftingMachine.pushPattern 推入样板输入。
acceptsPlans 仅在本地样板槽为空、本机 legacy 输入缓冲为空、GUI 真实输入缓冲为空、所有输出槽为空、待输出队列为空且没有合成进度时返回 true。
pushPattern 遵循 AE2 分子装配室语义：Pattern Provider 推入时可用本次 pattern 临时决定 recipe/plan，不要求把样板写入本机样板槽。
pushPattern 会直接读取 KeyCounter 中的 GenericStack；AEItemKey、AEFluidKey 与其它 AEKey 均可进入 PackagePlanBuilder，只受容量档和类型数约束。
普通 AE2 processing pattern 在本地样板槽为空时，使用 Pattern Provider 的 KeyCounter 内容和本次 pattern 生成临时包裹计划，避免本地物品槽数量限制。
带 Applied Packaging 包裹样板 NBT 的 AE2 crafting_pattern 不实现 AE2 `IMolecularAssemblerSupportedPattern`，不会被分子装配室当作普通 crafting pattern 执行；只有 ME Package Assembler 通过 AE2 pattern decoder hook 解码后按包裹样板语义执行。
独立 `advanced_processing_pattern` 物品会按连续的 81 槽输入列临时生成有序多包裹计划；每列颜色只作用于该列，名称固定为空，marker 固定取主产物。该物品继承 AE2 `EncodedPatternItem` 并提供兼容 `IPatternDetails` 的独立处理样板解码器，以绕开原版单样板 81 输入上限；高级列 NBT 不写入 AE2 原版 `processing_pattern`。带 appliedpackaging.colored_processing_pattern 或 appliedpackaging.packaged_processing_pattern 扩展 NBT 的既有样板继续作为兼容路径执行。
全部校验通过后，先从 KeyCounter 扣减输入，再进入本机合成进度；进度完成后才提交输出包裹。
任何一步失败都保持 all-or-nothing：不消耗 Pattern Provider 输入，不生成半包裹。
本地样板槽只接受已编码 package_pattern、packaged_processing_pattern、advanced_processing_pattern、带 Applied Packaging 包裹样板 NBT 的 AE2 crafting_pattern 或 AE2 encoded processing pattern；AE2 encoded processing pattern 只包括普通与既有兼容扩展处理样板。空白/未编码样板不能放入。
本地 package_pattern、packaged_processing_pattern 与 AE2 encoded processing pattern 共用 GUI 输入过滤逻辑：没有样板时所有输入槽锁定；有样板时只解锁样板输入项数量对应的格子。
已编码 package_pattern 使用目标 PackageData 做 exact package plan；每个输入格必须严格匹配同位置样板内容的 AEKey 与数量，不允许额外输入。
普通 AE2 encoded processing pattern 读取 decoded inputs 作为本地输入过滤；每个输入格必须严格匹配同位置样板输入，不允许额外输入。
advanced_processing_pattern 保留 AE2 processing sparse input 语义；装配室按 `column * 81 + row` 映射 17 个连续输入列，严格匹配启用列内每个非空槽，并按列顺序生成多个包裹。同色列不得合并。
带 appliedpackaging.colored_processing_pattern 扩展 NBT 的 AE2 encoded processing pattern 走彩色拆包路径。
彩色拆包读取 AE2 processing pattern 的 sparse input 槽位，按槽位颜色生成一个或多个包裹。
彩色拆包不依赖 AE2 已压缩的 IInput 顺序；即使相同 AEKey 被 AE2 汇总，仍按原始 sparse 槽位拆成不同颜色包。
带 appliedpackaging.packaged_processing_pattern 扩展 NBT 的 AE2 encoded processing pattern 走封装处理推送路径。
封装处理推送路径使用 AE2 原版 processing outputs 暴露给 Pattern Provider/Planner，装配室读取 packages[] 并输出一个或多个包裹。
封装处理 pushPattern 按 packages[] 中的 GenericStack 精确消费输入；包裹内容可包含 AEItemKey 或 AEFluidKey，输入不足或存在额外输入时整批拒绝。
一次计划产生多个包裹时，进度完成后按顺序写入 17 个真实输出槽；超过当前空输出槽数量的余量写入待输出队列，输出槽腾出后 server tick 继续顺序吐出。
待输出队列和进行中的合成包裹写入方块实体 NBT，破坏方块时以合法包裹掉落。
只要任意输出槽已有物品，装配室不会启动新的本地合成或接受新的 Pattern Provider plan。
已通过 GameTest 验证真实 AE2 Creative Energy Cell + Pattern Provider 方块网络可推送到 package_assembler。
已通过 GameTest 验证真实 AE2 Creative Energy Cell + Pattern Provider 方块网络可解码并推送带 packaged_processing_pattern NBT 的 AE2 encoded processing pattern。
已通过 GameTest 验证真实 AE2 Drive + 64k item cell + Crafting CPU + Pattern Provider 自动合成 job 会从 AE 网络抽取输入，并把 processing pattern 输入推入 package_assembler。
已通过 GameTest 验证装配室输出模式、NBT 持久化、相邻 item handler 导出、真实 AE2 Interface 网络导出和外部 item handler 顺序抽取。
```

方块实体状态：

```text
legacyInputSlots: 9 个旧物品输入槽，保留给旧存档与内部兼容路径
menuInputBuffer: 17 行 x 4 列真实 GUI 输入缓冲，共 68 个输入格，每格保存 ItemStack identity 与 long amount
outputSlots: 17 个 item slots，只允许合法包裹
patternSlot: 本地包裹样板/普通处理样板/高级处理样板/兼容扩展处理样板
capacitySlot: 可选容量元件
markerSlot: 可选 marker 物品槽
selectedColor: PackageColor
upgradeSlots: 5 格 AE2 upgrade inventory，只允许 speed card
craftProgress: 0-100
activePackages: 进行中合成输出队列
outputMode: ME_NETWORK | ADJACENT_BLOCK | NONE，默认 ME_NETWORK
pendingPackages: 输出槽被占用时等待吐出的有序包裹队列
lastFailure: enum/string
```

当前 0.1.0-dev 落地状态：

```text
legacy input slots 0-8
SLOT_PATTERN = 9
SLOT_OUTPUT = 10
SLOT_CAPACITY = 11
SLOT_MARKER = 28
extra output slots 12-27；outputHandlerSlot(0)=10，outputHandlerSlot(1..16)=12..27
Forge item handler capability 暴露完整 29 格机器库存；GUI menuInputBuffer 单独保存，不暴露给外部 item handler
AE2 CRAFTING_MACHINE capability 暴露装配室本体
方块实体 capability invalidation 后会在 revive 时重建 item handler 与 CRAFTING_MACHINE `LazyOptional`，区块卸载/重新激活后自动化入口不得永久失效
pending package queue 与 active package queue 持久化保存
容量槽识别 AE2 16k/64k/256k storage component、item/fluid storage cell 与 portable cell
marker 槽接受非包裹、非样板物品；只有样板或 Pattern Provider 临时 plan 没有包裹 marker 时，marker 槽才作为输出 marker fallback
selectedColor 和 markerSlot 保留给机器配置与旧载体兼容；正式普通处理样板输出固定为 Fluix、空 marker，不读取这些机器配置；包裹样板和高级样板以样板自身元数据为权威
selectedColor、marker 槽、outputMode、craftProgress、严格有序输出队列与 upgrade inventory 均持久化保存
输出由一个真实主输出和一个只读的下一包预览组成；其余成品保存在严格有序队列中。GUI、玩家、自动导出和 Forge item handler 每次都只能从主输出取 1 个包裹，取出后立即把队首提升为新的主输出
输出模式为 ME_NETWORK 时只尝试写入本机接入的 AE 网络存储服务，为 ADJACENT_BLOCK 时只尝试背面 Forge item handler，为 NONE 时不自动导出；自动导出可在同一 tick 循环多次，但每次仍按队首顺序提交
```

普通处理样板：

```text
输入 A+B+C，输出 X
装配室生成 1 个 Fluix 包裹，名称和 marker 均为空
AE2 Pattern Provider / Planner 视角的可见输出仍是原 processing pattern 的 X
装配室不会把包裹伪装成 X，也不会把包裹内容登记为 ME 散装库存
生成的包裹只是中间物流单元，必须由后续拆包/机器处理真正产出 X 后，AE2 作业才会完成
```

彩色处理样板：

```text
普通处理样板 + 输入格颜色元数据
按输入格颜色分组生成多个包裹
颜色跟随样板输入格，不跟随 AEKey
同一种 AEKey 位于两个颜色格时，必须生成两个不同颜色的包裹
颜色元数据保存于 AE2 encoded processing pattern 的 appliedpackaging.colored_processing_pattern NBT。
NBT 中的 inputs[] 以 AE2 processing pattern sparse input 槽位为索引。
未标色槽位按 Fluix 处理；无颜色 NBT 的原版 processing pattern 走普通默认打包路径，不读取机器 selectedColor 或 markerSlot。
当前服务端执行使用装配室容量槽；无容量元件时回退到 default package capacity。
```

高级处理样板：

```text
载体是 Applied Packaging 独立物品 `advanced_processing_pattern`，其 item 类继承 AE2 `EncodedPatternItem` 并返回 Applied Packaging 的处理样板详情；AE2 输入、输出、tooltip、Pattern Provider 和 Crafting CPU 仍按处理样板接口工作。
专属 NBT 描述 0..16 连续包裹列，每列映射 81 个 sparse processing input 槽并保存 PackageColor；当前高级终端写入空名称，并把处理样板主产物归一为每列 marker。AE2 原版 `processing_pattern` 不允许承载这段数据。
装配室按列顺序生成 1..17 个包裹；空列不生成包裹，同色列保持为独立包裹。
Pattern Provider push 时必须精确消费样板 sparse inputs，缺少输入或存在额外输入均整批拒绝。
颜色和高级终端生成的主产物 marker 以列元数据为权威，不回退机器配置；高级终端不提供名称编辑。
```

阻挡模式：

```text
如果 17 个 outputSlots 任意一个非空：
  不启动新的本地合成
  不接受新的 Pattern Provider plan
  server tick 先按 outputMode 尝试输出/导出既有包裹
```

阻挡只检查本机输出槽，不扫描输出 AE 网络、包裹子网、相邻接口、目标机器或主网库存。

UI：

```text
使用用户提供的 ME Package Assembler atlas 原图；颜色与 marker 位于输入区右侧，样板与容量元件并列位于顶部，输出模式等配置开关走 AE2 左侧悬浮 toolbar
输入区参考新版 AE2 样板终端 processing 模式滚动栏，左侧为 17 行 x 4 列真实输入缓冲；滚动条位于输入栏左侧并显示 4 行
右侧固定显示一个真实主输出和一个不可交互的下一包预览；预览旁显示剩余队列数量，hover 显示剩余包裹提示
下半区中部样板槽参考分子装配室：槽内只允许已编码样板；放入样板后，输入栏只按样板顺序解锁同位置真实输入槽并限制相同材料与数量，客户端不渲染过滤 ghost 物品
左侧输入栏不是 fake slot；点击或 shift-click 会真实转移玩家物品，可累计超过普通 stack size 的数量，只受包裹容量档和样板过滤约束
```

当前基础实现：

```text
package_assembler 已注册为方块、方块物品和方块实体。
方块实体提供 9 格 legacy 输入缓冲、68 格 GUI 真实输入缓冲、1 格样板槽、17 格输出槽、1 格容量槽、1 格 marker 槽与 5 格 AE2 speed-card upgrade inventory。
非潜行右键打开 Package Assembler GUI。
GUI 菜单继续使用 AE2 `UpgradeableMenu` 和 `ScreenStyle`，客户端改由 `ModernUpgradeableScreen` 回移 current-main 槽位 hover、升级面板与空升级槽视觉；style JSON 位于 `assets/ae2/screens/appliedpackaging/package_assembler.json`，背景贴图位于 `assets/appliedpackaging/textures/gui/mepackageassembler.png`。
背景 atlas 保持用户提供的 256x256 PNG 原图，ScreenStyle 使用主界面 `srcRect` 176x239；玩家物品栏、hotbar、标题和上半区控件按贴图实测坐标写入 style JSON。
可见区显示 4x4 输入格和 4 个输出格，左侧滚动条同步浏览全部 17 行；AE2 1.20.1 style grid 没有 4 列枚举，因此 4 行输入槽在菜单中拆成多组 AE2 slot semantics，由 style JSON 分别定位。
滚动输入/输出槽为真实菜单槽位，不是 fake slot；槽背景由客户端按 AE2 slot background 风格绘制，避免把动态滚动槽全部烘进背景图。
GUI shift-click 会优先把已编码 package_pattern / packaged_processing_pattern / AE2 encoded processing pattern 放入样板槽，把 AE2 容量元件放入容量槽，其它物品只在样板过滤允许时进入 GUI 真实输入缓冲；marker 槽通过真实槽手动放入，避免普通输入物品被误路由。
样板槽为空时，服务端和菜单输入均拒绝物品输入，不再自由封装；若样板被取走但输入槽仍有残留物品，残留槽保持可取出并渲染为红色错误状态，空输入槽重新锁定。
服务端 tick 在输出侧没有阻挡且输入严格匹配本地样板或 Pattern Provider 临时 plan 时启动合成进度；进度达到 100 后提交输出。
输入中的合法包裹会先展开，再与散装物品合并封装。
任意输出槽已有物品时阻挡，不消耗任何输入。
样板槽可放入已编码 package_pattern、packaged_processing_pattern、带 Applied Packaging 包裹样板 NBT 的 AE2 crafting_pattern 或 AE2 encoded processing pattern。
容量槽使用与 ME Packager 相同的 AE2 16k/64k/256k 容量元件映射，并且不消耗容量元件。
如果 package_pattern 已编码，装配室只接受与样板 canonical hash 完全一致的输入计划。
已编码 package_pattern 不会被消耗，输出包裹颜色跟随样板颜色。
如果 packaged_processing_pattern 已编码，装配室读取有序 package list，每次生成一个当前输入可满足且 canonical hash 匹配的包裹，并写入第一个可用输出槽。
packaged_processing_pattern 不会被消耗；其它输出槽仍可接收后续可满足包裹。
如果 AE2 encoded processing pattern 带彩色输入槽元数据，Pattern Provider pushPattern 会按 sparse input 槽位拆成对应颜色包裹，输入槽可包含物品或流体 AEKey。
如果 Pattern Provider pushPattern 推入的是带 Applied Packaging 包裹样板 NBT 的 AE2 crafting_pattern，装配室临时使用该 pattern 中的 sparse crafting inputs 规划单个包裹，严格匹配输入 AEKey 与数量，输出为 pattern 自动计算出的包裹物品。
彩色 Pattern Provider 推送可产生多个包裹；17 格输出槽优先接收，超过可用输出槽的余量进入 pending queue。
普通 Pattern Provider pushPattern 在本地样板槽为空时直接从 KeyCounter 和本次 AE2 pattern 规划一个 Fluix、空名称、空 marker 的包裹，可承载超过 9 个物品栈或流体输入，并受容量档约束。
输出模式默认 ME_NETWORK，可在装配室 GUI 左侧 AE2 toolbar 中循环切换 ME_NETWORK、ADJACENT_BLOCK 和 NONE，并保存到 NBT。
自动导出按输出槽顺序一次只处理 1 个包裹；目标拒绝或容量不足时保留输出槽内容，不丢弃、不消耗新的输入。
右侧升级面板使用 current-main `UpgradesPanel` 的 1.20.1 回移实现与 5px padding；空槽从独立原样 `ae2-states.png` 绘制 current-main `BACKGROUND_UPGRADE`，不再混入 AE2 15 灰阶占位。顶部已编码样板槽和容量元件槽不再调用依赖 AE2 15 的 `Icon`，而是分别从同一原样 `ae2-states.png` 绘制 current-main `BACKGROUND_ENCODED_PATTERN (240,112,16,16)` 与 `BACKGROUND_STORAGE_COMPONENT (240,48,16,16)`。升级兼容性通过 `Upgrades.add(AEItems.SPEED_CARD, package_assembler, 5)` 注册，只允许 5 张加速卡，并由方块实体的真实 upgrade inventory 保存、读取和掉落。空 marker 槽从用户 sprite `(32,16,16,16)` 绘制自有图标，hover 使用 current-main 蓝色填充/边线并提供双行说明 tooltip。
合成进度使用 AE2 分子装配室速度表与能量税率：0/1/2/3/4/5 张加速卡每 tick 分别尝试推进 10/13/17/20/25/50，能量倍率分别为 1.0/1.3/1.7/2.0/2.5/5.0；每 tick 先从本机 AE grid energy service 抽取 AE 能量，网络不可用或能量不足时不推进，最大进度 100。
```

## 8. ME 打包机

职责：

```text
识别所选连接面相邻 AE2 MEStorage
识别相邻 ME Interface 背后的存储子网
从端点生成包裹
把输入包裹拆入端点
包裹展开再封装
基础 1k 容量、16 类型上限、过滤、marker、颜色策略
GUI、快速交互和红石触发
```

不做：

```text
读取 Pattern Provider
读取/执行任何样板
扫描自己所在任意 AE 网络
回落到 Forge item handler 或 Forge fluid handler 作为打包/拆包端点
充当 AE 网络设备提供库存
```

方块实体状态：

```text
inputSlot: 只允许合法包裹
outputSlot: 只允许合法包裹
capacitySlot: 只允许 AE2 16k/64k/256k storage component；为空时使用基础 1k/16 类型
contentFilter: 45 格 AE2 GenericStack 配置，基础 2 行，最多 3 张容量卡各解锁 1 行
upgradeSlots: 6 格 AE2 upgrade inventory，当前支持 capacity/speed/inverter card
networkSide: Direction
selectedColor
markerItem
legacyPackageFilterSlot
filterApplicationMode: both / pack_only / unpack_only
blockingMode: ignore_network_contents / block_unpack_when_network_has_items
redstoneMode
workingOperation: none / packing / unpacking
workingStack: 工作动画中的 1 个包裹；packing 时是已抽取但尚未进入 outputSlot 的待输出包裹，unpacking 时仅作视觉记录
pendingPackTrigger: 工作态期间收到的单次打包触发
lastEndpointInfo
lastFailure
```

端点接口：

```java
interface GenericStorageEndpoint {
    List<PackageEntry> listAvailable();
    long simulateExtract(PackageEntry entry, long amount);
    long extract(PackageEntry entry, long amount);
    long simulateInsert(PackageEntry entry, long amount);
    long insert(PackageEntry entry, long amount);
    EndpointCapabilities capabilities();
}
```

端点类型：

```text
AE2 MEStorage capability
AE2 ME Interface adjacent subnet
未来扩展：AE 线缆或其它 AE 设备的 MEStorage endpoint
```

打包触发：

```text
服务端 tick 根据有效激活模式自动尝试
红石脉冲模式在上升沿执行一次
快速右键可放入输入包裹或取出输出包裹；无快速动作时打开 GUI
```

打包事务提交前必须全部模拟：

```text
1. 端点抽取所有源内容可行
2. 输出槽为空且可接收生成包裹
3. 机器不在 workingOperation
4. marker 策略无冲突
5. 容量与类型数未超限
```

提交顺序：

```text
模拟全部通过
从端点抽取源内容
写入 workingOperation=packing 与 workingStack
播放打包动画
动画结束同 tick 把 workingStack 放入 outputSlot
清空工作态
```

任何一步失败都不得改变端点、输入槽或输出槽。打包已抽取但动画尚未结束时，workingStack 属于机器状态；保存/读取后继续完成，破坏方块时作为待输出包裹掉落。

拆包事务：

```text
外部 capability insert 每次最多接受 1 个包裹
允许输入前必须满足：
  机器不在 workingOperation
  outputSlot 为空
  filterApplicationMode 不是 pack_only
  如果 selectedColor 不是 fluix，包裹 item 颜色必须等于 selectedColor
  如果 markerItem 存在，包裹 PackageData.marker 必须等于 markerItem
  包裹内容满足内容过滤；反转卡存在时反转内容过滤
  阻挡模式允许拆包
  目标 MEStorage 可一次性接收包裹全部内容
提交时先把包裹内容完整插入目标 MEStorage
再写入 workingOperation=unpacking 与 workingStack
播放拆包动画；动画结束只清空工作态，不产生 outputSlot
```

单个包裹永远不能拆一半。

当前基础实现：

```text
me_packager 已注册为方块、方块实体和方块物品。
方块状态包含水平朝向 facing 与可切换连接面 network_side。
放置时 network_side 默认设为 facing 的反向；潜行右键任意方块面会把 network_side 切换到被点击面。
GUI 作为主入口，菜单继承 AE2 `UpgradeableMenu` 并使用 `ScreenStyle`，客户端改由 `ModernUpgradeableScreen` 回移 current-main 槽位 hover、升级面板与空升级槽视觉。
样式文件位于 `assets/ae2/screens/appliedpackaging/me_packager.json`，背景贴图位于 `assets/appliedpackaging/textures/gui/mepackager.png`。
右侧升级面板使用 current-main `UpgradesPanel` 的 1.20.1 回移实现与 5px padding；空升级槽使用独立原样 `ae2-states.png` 的 current-main `BACKGROUND_UPGRADE`。容量元件槽不再调用依赖 AE2 15 的 `Icon.BACKGROUND_STORAGE_COMPONENT`，改从同一原样资源绘制 current-main `(240,48,16,16)`。升级兼容性通过 `Upgrades.add` 注册到 ME Packager 方块物品。空 marker 槽从用户 sprite `(32,16,16,16)` 绘制自有图标，hover 使用 current-main 蓝色填充/边线并提供双行说明 tooltip。
非潜行右键执行快速交互：
  手持合法包裹时走与外部 capability 相同的立即拆包输入规则。
  空手或非包裹物品时先尝试取出输出槽。
  输出槽为空且没有快速动作时打开 GUI。
其它非 network_side 面暴露 2 槽普通 item capability：slot 0 只在包裹可立即完整拆包时接收 1 个合法包裹，接收后不暂存到 inputSlot；slot 1 只导出 outputSlot 中的输出包裹；network_side 不暴露普通 item capability。
方块实体 capability invalidation 后会在 revive 时同时重建内部与外部 item handler `LazyOptional`。
GUI 左侧按钮区包含帮助、清除配置、基于网络现存物品配置分区、过滤应用模式、打包激活模式和阻挡模式。
过滤区为 5 行 9 列 ConfigInventory；默认启用 2 行，最多 3 张容量卡各启用 1 行，未启用行由 AE2 OptionalFakeSlot 控制渲染和交互。
包裹配置区只包含颜色选择按钮和 marker 物品槽；marker 槽有物品时直接作为输出 marker 覆盖来源 marker。
容量元件槽只接受 AE2 16k/64k/256k storage component。包裹输入和打包输出共用一个真实 heldBox，通过 EMPTY、UNPACK_INPUT、PACK_OUTPUT 状态区分语义，禁止输出回流到拆包路径。
GUI shift-click 与外部 capability 每次最多接受 1 个包裹；输入先保留在 heldBox，进度结束时才事务性写入 ME。若网络变化导致最终提交失败，heldBox 保留输入并显示阻塞红底，定时重新验证并重启进度，且允许玩家取回。
GUI 工作态期间在包裹配置区与输出口之间显示进度条；进度来源为服务端同步的 workingOperation 与剩余动画 tick。
GUI 左侧打包激活模式直接控制实际红石逻辑，可在 `HIGH_SIGNAL`、`LOW_SIGNAL`、`ALWAYS`、`PULSE`、`NEVER` 间切换；默认 `HIGH_SIGNAL`，不使用额外红石卡作为隐藏门槛。
反转卡安装后只反转内容过滤，不反转 selectedColor 或 markerItem 门禁；内容过滤为空时仍表示不过滤。
红石只控制自动打包；输入包裹拆包不受红石关闭影响，仍受拆包过滤、阻挡模式、目标容量和目标在线状态约束。
工作态期间拒绝新的输入；持续打包触发等机器空闲后再重试，红石脉冲或手动单次打包触发在工作态期间记录为 pendingPackTrigger，空闲后尝试一次。
持续打包和自动拆包按基础 20 tick 间隔重试；加速卡会降低间隔但不低于 2 tick。
所选 network_side 只识别 AE2 `MEStorage` capability，可接入相邻 ME Interface 暴露的子网存储；无 AE2 storage 时返回 NO_TARGET，不回落 Forge item handler / fluid handler。
真实 AE2 Creative Energy Cell + Drive + Interface + ME Packager GameTest 覆盖从相邻 Interface 网络打包、抽走网络内容、再整包拆回网络。
真实 AE2 顶面 network_side GameTest 覆盖连接面可切换到非背面方向。
真实世界相邻 Forge item handler / fluid handler 反例 GameTest 覆盖无 MEStorage 时不打包、不拆包、不消耗相邻 Forge 端点。
隐藏 inputSlot 只用于旧存档或内部兼容；旧存档或内部 inputSlot 存在合法包裹时执行整包拆包。外部 capability 输入、快速右键输入和 GUI shift-click 输入都不再填充 inputSlot。
输入侧为空且机器空闲时，从所选 AE 网络选择基础 1k 容量、16 类型上限可承载的内容生成包裹；生成包裹先进入 packing 工作态，动画结束后进入唯一 outputSlot。
容量元件槽可把当前单包容量提升到 16k/64k/256k；容量卡只解锁过滤行，不改变包裹容量档。
旧过滤槽保留为 NBT/旧存档兼容输入；新 GUI 主要使用 45 格 contentFilter。
过滤模板接受已编码 package_pattern、packaged_processing_pattern 或带 PackageData 的包裹。
过滤模板会提供颜色、marker 与 requiredContents：
  打包时颜色优先使用过滤模板颜色，否则使用 GUI swatch selectedColor。
  打包内容过滤只选择 requiredContents 中出现的 AEKey，忽略 ghost amount 作为数量上限；反转卡存在时选择 requiredContents 之外的 AEKey。
  marker 策略保留旧 NBT 兼容；新 GUI 中 marker 槽物品优先作为覆盖 marker。
  retain 会保留源包裹 marker；多个源包裹 marker 冲突时计划失败。
  override 优先使用 marker 槽物品作为输出 marker；marker 槽为空时兼容使用过滤模板 marker。
  clear 会生成无 marker 的输出包裹。
  过滤应用模式为 both 时打包和拆包都使用内容过滤；pack_only 只约束打包且外部包裹输入被拒绝；unpack_only 只约束拆包。
  拆包过滤启用时，输入包裹的全部内容 AEKey 必须满足 allowlist 或反转后的 denylist；包裹 item 颜色和 markerItem 作为独立门禁同时满足。
阻挡模式为 block_unpack_when_network_has_items 时，如果目标 ME 网络已有任意可见内容，则拒绝拆包且不消耗输入包裹。
AE2 MEStorage 端点直接处理 AEKey/GenericStack，并会把 MEStorage 中已有包裹展开后再封装。
红石卡门槛、脉冲打包、持续高信号打包、红石关闭仍允许拆包、过滤行容量卡解锁和 client smoke GUI 截图已有验证覆盖。
```

## 9. 样板与终端

包裹样板物品 ID：`package_pattern`

样板承载约束：

```text
当前实现仍保留本地 package_pattern / packaged_processing_pattern 作为兼容载体。
本地 package_pattern / packaged_processing_pattern 不在创造栏显示，也没有普通合成配方；玩家主入口是 AE2 原版 blank_pattern。
AE2 原版 blank_pattern 已可作为 package_pattern 数据载体：终端可写入，装配室、ME Packager 过滤模板和 Package Bus 过滤模板可读取。
当终端预览输入需要多个包裹计划但没有处理输出 ghost 时，AE2 原版 blank_pattern 会作为 packaged_processing_pattern 数据载体；装配室可按该载体逐包输出。
当终端存在处理输出 ghost 时，AE2 原版 blank_pattern 会编码为 AE2 encoded processing pattern，并附带 packaged_processing_pattern NBT；AE2 可读取 outputs[]，装配室按 packages[] 逐包输出。
已编码 AE2 blank_pattern 通过客户端 tooltip hook 显示 package_pattern 或 packaged_processing_pattern 内容；普通 AE2 blank_pattern 不额外显示本 mod 文案。
独立 `advanced_processing_pattern` 物品承载同名包裹列元数据，并通过自定义 `IPatternDetails` 处理样板解码路径参与 Pattern Provider 与 Crafting CPU；AE2 原版 `processing_pattern` 不承载这段数据。既有 colored_processing_pattern 和 packaged_processing_pattern 载体继续保留兼容。
AE2 原版 crafting_pattern 可作为包裹样板载体：原版 Pattern Encoding Terminal 的包裹样板模式写入 Applied Packaging 专属 NBT，AE2 pattern decoder 读取后返回只被 ME Package Assembler 使用的 PackageCraftingPatternDetails。
包裹样板模式沿用 AE2 原版 crafting grid 和输出计算流程；额外配置只包含包裹颜色和 marker 物品，输出包裹由输入内容自动计算。
packaged_processing_pattern 本地物品仍保留读取兼容；AE2 encoded processing pattern 载体已覆盖物品与流体容器处理输出语义，普通玩家流程不再要求新增样板物品。
```

包裹样板数据：

```text
PackageColor
MarkerSpec optional
List<PackageEntry>
CapacityProfile
```

彩色处理样板：

```text
slotColor[0..8] = PackageColor
```

彩色处理样板不改变 AE2 的输入输出，只在 AE2 encoded processing pattern NBT 中写入输入格颜色数组。普通机器忽略这段数据；装配室读取它。

1.20.1 NBT 扩展：

```text
tag.appliedpackaging.colored_processing_pattern.version: int
tag.appliedpackaging.colored_processing_pattern.inputs: list<compound>
inputs[].slot: int，指向 AE2 processing pattern sparse input slot
inputs[].color: string，PackageColor.id()
```

高级处理样板物品与 NBT：

```text
item id: appliedpackaging:advanced_processing_pattern
tag.appliedpackaging.advanced_processing_pattern.version: int = 1
tag.appliedpackaging.advanced_processing_pattern.columns: list<compound>
columns[].index: int，必须从 0 开始连续且小于 17
columns[].color: string，PackageColor.id()
columns[].marker: optional GenericStack compound，只允许数量为 1 的 AEItemKey marker
根 NBT 的 in/out 沿用 AE2 processing pattern 编码；第 n 列输入槽固定映射 sparse inputs[n * 4 .. n * 4 + 3]
高级终端只写入启用列范围；未启用列中的旧 ghost 数据不得进入新样板
```

封装处理样板物品 ID：`packaged_processing_pattern`

封装处理样板数据：

```text
version
PackageColor
ordered List<PackageData>
outputs: List<GenericStack>，当前 UI 覆盖物品输出 ghost slots 与 Forge 流体容器 ghost slots
每个 PackageData 使用现有 canonical hash 校验
```

终端 tab：

```text
Package Pattern:
  17 色选择、marker 槽、GenericStack ghost slots、容量档槽、编码

Colored Processing Pattern:
  普通处理样板输入格、每格颜色、处理输出、编码

Packaged Processing Pattern:
  多个包裹样板、处理输出、空白样板、编码

Split:
  封装处理样板、空白样板若干、拆出由 AE2 blank_pattern 承载的包裹样板
```

当前基础实现：

```text
package_pattern 与 packaged_processing_pattern 使用 PackagePatternItem，tooltip 会区分空白/已编码状态。
已编码 AE2 blank_pattern 使用客户端 tooltip event 追加 package_pattern 或 packaged_processing_pattern 内容；未编码 AE2 blank_pattern 保持 AE2 原版 tooltip。
PackagePatternDataStorage 在 ItemStack NBT 中写入 version、color 与嵌套 PackageData。
读取已编码样板时会按样板颜色复验嵌套 PackageData canonical hash。
PackagedProcessingPatternDataStorage 在 ItemStack NBT 中写入 version、color、packages[] 与可选 outputs[]。
读取封装处理样板时会逐个复验嵌套 PackageData canonical hash，并兼容旧的单包裹 PackagePatternDataStorage 写法。
package_pattern_terminal 物品 id 已改为 AE2 cable part item，可贴到 AE2 cable bus 侧面；兼容用 package_pattern_terminal 方块、方块实体、菜单和客户端 screen 仍保留给旧存档/测试路径。
终端菜单逻辑已抽象为 PackagePatternTerminalHost，普通方块实体与 AE2 part 共用同一套菜单、编码、Split、ghost output 与保存逻辑；客户端菜单打开包会显式携带 block host 或 part host 定位。
终端基础外形已调整为 AE2 风格薄面板；玩家合成与创造栏入口不再是普通方块物品，而是 AE2 part 物品。
终端 GUI 当前提供 9 格预览输入、1 格样板槽、1 格输出、容量槽、marker 槽、3 个处理输出 ghost slots、17 色 swatch、9 个输入槽色标按钮，以及 Encode/Split 按钮。
样板槽接受未编码 package_pattern、未编码 packaged_processing_pattern、AE2 原版 blank_pattern、AE2 encoded processing pattern，或已编码 packaged_processing_pattern 作为 Split 来源。
输出尽量保留输入样板语义；AE2 blank_pattern 的单包裹无输出场景会输出为带 package_pattern NBT 的 AE2 blank_pattern，多包裹且无处理输出时会输出为带 packaged_processing_pattern NBT 的 AE2 blank_pattern，有处理输出时会输出为 AE2 encoded processing pattern 并附带 packaged_processing_pattern NBT；AE2 processing pattern 会复制 1 个输出并写入 colored processing metadata。
编码 package_pattern 时写入单个 PackageData；编码 packaged_processing_pattern 时按容量档把预览输入拆成有序 packages[]。
编码 packaged_processing_pattern 时会把处理输出 ghost slots 写入 outputs[]；点击 ghost slot 会复制光标物品与数量，右键复制 1 个，空光标点击清除，均不消耗玩家物品。若光标物品是 Forge 流体容器，则 ghost 槽显示容器物品，但 outputs[] 写入对应 AEFluidKey 与流体数量；例如水桶写入 1000 mB water。鼠标悬停处理输出 ghost 槽并滚轮调整数量时，流体每步调整 1000 mB，物品/其它已存在 key 每步调整 1；数量叠字只在真实数量大于显示栈数量时显示。
编码只读取预览输入，不消耗预览输入、容量槽或 marker 槽；只消耗 1 个未编码空白样板。
终端保存并同步 selectedColor，GUI 使用统一拾色弹窗选择 17 色，编码样板颜色跟随当前选择。
终端保存并同步 9 个输入槽颜色；左键点击输入槽角落色标会打开同一拾色弹窗并直接设置该槽颜色，右键点击清除该槽颜色。
编码 AE2 processing pattern 时，如果没有逐槽设色，则所有非空 sparse input slot 使用 selectedColor；如果存在逐槽设色，则只写入对应槽位颜色，未设槽位由装配室按 Fluix 默认处理。
AE2 原版 Pattern Encoding Terminal 通过 mixin 增加包裹样板模式 tab，与 crafting / stonecutting / smithing 使用同一层级的模式入口；tab 从本地高版本 sprite 读取 `(32,0,16,16)` 完整图标单元，并与 AE2 `TabButton.Style.HORIZONTAL` 一样绘制在 tab 原点 `x+1,y+3`，不裁掉图标单元自带 padding。该模式以用户提供的 124x66 面板替换 AE2 15.4.10 crafting panel。面板与槽位统一采用 AE2 1.21.1 processing 布局基准：面板绘制原点为 screen 的 `x+8`、`bottom-165`，滚动条 widget 为 screen 的 `x=15`、`bottom=158`，3x3 可见输入窗口首个 slot 为 screen 的 `(24,bottom-158)`，即面板内 `(16,7)`。清空与颜色按钮分别位于面板内 `(72,7)`、`(82,7)`，与输入区保留 2px 边距；marker slot 为 `(95,7)`，24x24 输出框中的唯一自动包裹输出 slot 为 `(98,31)`。包裹模式不启用任何 processing output 配置槽；离开后 crafting/processing panel 与对应槽位恢复 AE2 15.4.10 原布局。
点击颜色按钮只打开统一拾色弹窗。弹层打开时 crafting grid、marker 和输出槽保持 active 并继续正常绘制物品，底层 tooltip 取消，鼠标点击/释放/拖拽/滚轮、键盘和字符输入由弹层拦截，点击外部只关闭弹层且不透传；主面板按钮保持可见但暂时停用。弹层通过前景 Z 层遮挡与其重叠的 slot/item，不通过隐藏物品制造遮挡。marker 仍在主面板真实 fake 配置槽中直接设置。
包裹样板模式编码时输出 AE2 crafting_pattern 物品，并写入 appliedpackaging.package_crafting_pattern NBT；AE2 tooltip、pattern decoder、Pattern Provider 和 Crafting CPU 通过该 NBT 识别输出包裹，装配室之外的机器不会把它当作可执行的分子装配室 crafting pattern。
包裹模式复用 AE2 的 81 个 processing input fake slot 与 processing scrollbar，按每行 3 格显示连续 3 行；滚动范围覆盖 27 行。scrollbar handle 使用已复制并标记来源的 AE2 1.21.1 small scroller sprite，而不是 1.20.1 `Scrollbar.SMALL` 图形。processing output slots 在该模式隐藏并移出渲染区域，包裹 marker 使用专属 marker slot，包裹预览继续使用 crafting result slot；非当前滚动窗口的 processing input 与停用的 crafting grid 同样移出渲染区域，避免其 ghost item 被容器层继续绘制。输入保留 AE2 `ConfigInventory`/`GenericStack` sparse 数据与数量显示，数量可大于 1，并允许 AEItemKey、AEFluidKey 等 AE processing input 类型。`PatternEncodingTermMenu.isProcessingPatternSlot` 在包裹模式识别全部 81 个 processing input，使原版 `getEmptyingAction` 支持物品/流体容器写入，原版 `canModifyAmountForSlot` 对非空输入开放中键 `SetProcessingPatternAmountScreen`。退出包裹模式时恢复原 processing/crafting 槽位分类、可见性、数量和编辑语义。自动包裹预览不是 processing primary output，空槽不注册 `primary_processing_result_tooltip`；存在包裹时只显示物品自身 tooltip。
高级样板终端注册为 AE2 cable part item，part 继承 AE2 PatternEncodingTerminalPart，复用原版静态模型、网络终端库存、搜索栏、AE 左侧工具栏和右侧 view-cell 区域；菜单继承 PatternEncodingTermMenu 并强制保持 PROCESSING 模式。
高级样板终端 GUI 使用 AE2 ScreenStyle/MEStorageScreen；两行网络库存时主体宽 195px、高 250px，顶部网络库存为 9 列，标题、搜索、网络滚动条、玩家栏与样板编码区按 AE2 高版本 Pattern Encoding Terminal 基线布局。动态加高时首行和中间重复行使用 atlas 第一网络行，末行只使用 atlas 第二网络行，避免重复第二行造成接缝。中间编码区为 4 个可见包裹列，每列拥有 81 个真实 processing input 槽但只同时显示 3 行，列之间保留 1px 间距；右侧 4 个真实 processing output 槽同样只显示 3 行。左侧使用 AE2 高版本小滚动 indicator 同步滚动输入/输出，底部水平滚动条只滚动包裹列；第一未启用列显示加号，后续列显示禁用背景且无颜色按钮/无 ghost 物品。右侧 `BLANK_PATTERN` 与 `ENCODED_PATTERN` 逻辑槽分别使用 `left=167,bottom=167` 和 `left=167,bottom=120`，使槽内物品相对用户底图槽框各向右、向上 1px。编码按钮 widget 保持 `left=167,bottom=146`：两个 16px 槽内容区、16px 图标和 16px 点击区共用 `x=167` 中心线，按钮 18px 新版背景从 `x=166` 开始，与 18px 槽框同轴；其它控件不继承原版终端包裹模式的 1px 偏移。高级输入 fake slot 允许修改数量；中键打开复用 AE2 数量输入 style 和包语义的 `AdvancedSetPatternAmountScreen`，确认后通过 `InventoryAction.SET_FILTER` 回写对应真实菜单槽。

高级终端覆盖 1.20.1 AE2 的白色槽位 hover。旧 `renderCustomSlotHighlight` 注入只取消旧效果，不在 Vanilla 尚未结束的槽位批次中追加绘制；在 AE tooltip 阶段前先提交背景纹理批次，再按 AE2 1.21.1 使用 `0x669cd3ff` 淡蓝填充和 `0xffdaffff` 四边高亮，随后提交高亮批次并继续 tooltip。该顺序同时适用于空槽和有物品槽，避免旧版延迟 blit 出现黑色矩形。
每个启用列上方包含颜色按钮和 X 按钮。颜色按钮只打开统一拾色弹窗，不提供名称或 marker 编辑；X 在列非空时清空该列，在列为空时删除该列并让后续输入与颜色前移，最后一列不会删除。弹窗在 `super.render(...)` 完成后作为不透明高 Z 前景绘制；可见 processing input/output slots 保持 active，物品继续在弹窗下方渲染。打开时按钮保持可见但停用，鼠标点击/释放/拖拽/滚轮、键盘与字符输入均不会透传到底层 AE 网络库存、processing slots 或编码按钮，点击外部只关闭弹层。

统一 `PackageColorPicker` 是所有包裹颜色入口的唯一弹窗实现：无标题、无名称或 marker 控件，Fluix 默认色在最左侧独立居中，其余 16 个染料色在右侧按 8x2 排列。颜色格由弹窗单次手工绘制而不注册为普通 widget，只有弹窗自身绘制一次 hover tooltip；父 Screen 先完整绘制槽位、物品和普通 widget，再取消底层 tooltip。原版 AE 终端在 `ScreenEvent.Render.Post` 中先提交既有物品批次，再由 picker 以与 Vanilla tooltip 相同的 `z=400` 和 `GuiGraphics.drawManaged` 整批绘制；其它父 Screen 在自身末尾使用同一 managed batch。打开 picker 不改变底层 slot active 状态，但 picker 必须吞掉 mouse click/release/drag/scroll、keyPressed 与 charTyped。ME Packager、ME Package Assembler、AE2 原版包裹模式、Advanced Pattern Terminal、Package Pattern Terminal 的全局/逐槽颜色入口，以及 Package Storage/Export/Unpacking Bus 均复用该控件。
高级终端编码结果是独立 `advanced_processing_pattern` 物品，并写入 AE2 processing in/out 与 appliedpackaging.advanced_processing_pattern 列数据；默认 AE2 Pattern Encoding Terminal 和其它普通编码路径只输出原版样板，不写高级列 NBT。
高级终端读取普通 processing pattern 时装入第一列；读取高级 processing pattern 时恢复列数、颜色和最多 17x81 个 sparse inputs。编码时名称固定为空，每列 marker 固定取主产物并归一为 1 个 AEItemKey。
容量槽使用与 ME Packager 相同的 AE2 16k/64k/256k 容量元件映射。
marker 槽写入 package_pattern 的 MarkerSpec，可作为 ME Packager 过滤或 override 回退模板。
marker 槽写入 packaged_processing_pattern 时会应用到拆出的每个包裹计划。
Split 会把已编码 packaged_processing_pattern 拆回多个由 AE2 原版 blank_pattern 承载的 package_pattern；输出槽逐张吐出，剩余拆分结果保存在终端 pending queue，保存/读取后可继续输出。本地 package_pattern 仍只作为旧存档/测试兼容载体，不再由正常 Split 玩家流程产出。
输出槽非空时不消耗空白样板；空白槽中的已编码 package_pattern 或 packaged_processing_pattern 会被拒绝。
默认初始选择为 Fluix。
当前已支持 AE2 原版 blank_pattern 作为 package_pattern 与无输出 packaged_processing_pattern 数据载体；也已支持 item/fluid-container packaged_processing_pattern 通过 AE2 encoded processing pattern 暴露 processing outputs 给 Pattern Provider/Planner。玩家配方入口与 Split 输出已收敛到 AE2 原版 blank_pattern；本地 package_pattern / packaged_processing_pattern 只保留旧存档/测试兼容。仍不含任意 AEKey 处理输出 ghost editor。
```

## 10. 包裹总线

包裹存储总线：

```text
只枚举相邻库存中的合法包裹
过滤不匹配的包裹不可见
插入时只允许合法包裹
不拆包
```

包裹卸货总线：

```text
从 ME 网络选择匹配包裹
模拟完整插入相邻目标
从网络抽取一个包裹并保存在本地 heldPackage
播放与 ME 打包机拆包模式相同的工作进度
进度结束时重新验证过滤与目标容量
成功时一次性提交全部散装内容并清空 heldPackage
失败时保留同一个 heldPackage，进入阻塞并定时重试
```

目标实现：

```text
Package Storage Bus 与 Package Unpacking Bus 的玩家入口均为 AE2 `PartItem`，安装在 cable bus 面上并各自要求一个 channel；part 安装面指向相邻 Forge item handler。旧方块只允许作为迁移期兼容实现存在，不进入配方、创造栏或正式模型验收。
Package Export Bus 与独立 Package Pattern Terminal 已从正式范围删除，不保留玩家注册、配方、创造栏入口或客户端界面。

package_storage_bus:
  作为 IStorageProvider 挂载 PackageItemStorage。
  只枚举带 PackageData 的合法包裹。
  insert/extract 均拒绝散装物品和无 PackageData 的包裹。
  设置过滤模板后，只暴露、插入、抽取匹配过滤的包裹。
  SIMULATE 插入使用保留真实 slot limit 与 isItemValid 规则的累计库存快照，多个包裹不能重复占用同一份空余容量。
  服务端每 10 tick 请求重新挂载 IStorageProvider；相邻 handler 中包裹增删、目标方块移除/替换或运行中修改过滤器后，AE storage cache 会刷新可见 key，并卸载旧目标留下的 key。
  不暴露包裹内部内容。

package_unpacking_bus:
  周期性从 AE 网络选择合法包裹。
  设置过滤模板后，只选择匹配过滤的包裹。
  首次选择时先在同一累计快照中模拟完整拆入目标面库存，再从网络精确抽取 1 个包裹并转移到 part 自己的 `heldPackage`；此时目标仍不获得任何内容。
  `heldPackage`、工作态、剩余工作 tick、阻塞态和重试冷却写入 part NBT；工作槽直接同步该真实 held 状态，不再使用空预览占位。
  工作周期固定复用 ME 打包机的 20 tick 拆包进度。进度结束时重新校验当前过滤规则、目标存在性和累计容量，全部通过后才事务性写入全部内容并清空 held 状态。
  最终提交失败时不把包裹写回 ME 网络，也不切换到其它包裹；保持同一个 `heldPackage`，清空进度并进入阻塞态，等待下一次重新验证后从完整 20 tick 进度重新开始。
  加速卡与 ME 打包机一致只缩短扫描/阻塞重试间隔：`max(2, 20 - speedCards * 3)`；不缩短 20 tick 工作动画本身。
  工作中拒绝玩家取出 held 包裹；阻塞或其它空闲 held 状态允许从 GUI 工作槽取回。part 被拆除时 held 包裹作为额外掉落返还，`clearContent` 同步清空，避免复制或丢失。
  不接受部分拆包。

两个总线共用 7 行 `PackageBusFilterRule`。每行保存颜色是否启用及颜色值、marker item key、6 个普通 item key、模糊开关和反转开关；空行不参与匹配，所有非空行按 OR 合并。内容过滤在反转关闭时要求包裹内容均属于该行白名单，在反转开启时要求包裹内容不属于该行列表；模糊开启时使用 AE2 `FuzzyMode` 比较物品 key。
基础启用第 0、1 行；它们在 GUI 中对应底图最上方 `y=29`、`y=47` 两行。5 张容量卡依次启用第 2、3、4、5、6 行，第 5 张达到 7 行过滤容量上限。移除容量卡时被锁定行必须清空，菜单使用 `OptionalFakeSlot`/`IOptionalSlot` 提供禁用状态，并按 AE2 新版风格对正常 slot background 使用透明叠加，不制作独立 disabled-slot 贴图。
模糊卡与反转卡各最多 1 张。对应按钮只在卡存在时显示；两者都存在时顺序为模糊、反转、颜色，只存在一个时该按钮紧靠颜色。模糊、反转、颜色三个 8px 按钮在每个 18px 过滤行内统一使用固定 2px 上边距，不按行高垂直居中。按钮分别保存每一行状态，绿色表示启用、红色表示停用。颜色按钮始终存在；右键颜色按钮清除该行颜色约束。
两个界面保留用户提供的 `package-storagebus.png` 与 `package-storagebus-sprites.png` 为两张独立、字节不变的运行时纹理；工作槽与空进度框从背景文件 `[176,0,18,18]` / `[196,0,6,18]` 取样到 `(119,8)` / `(139,8)`，Package Storage Bus 不提交该工作区。过滤槽和模糊/反转按钮来自用户 sprite，其中锁定行使用 `SLOT_BACKGROUND` 的 `0.2` alpha；同一用户 sprite 的 `(32,16,16,16)` 是自绘 marker 空槽图标，空 marker 过滤槽按所在行透明度绘制，已解锁槽 hover 时显示双行说明 tooltip。新版 `states.png`、`extra_panels.png` 与 `vertical_buttons_bg.png` 作为三张未修改的 LGPL 资源独立加载，分别承担新版 toolbar 按钮/优先级标签、升级与工具箱面板、竖向按钮组外框；不向用户原图烘入任何 AE2 像素。AE2 当前 main 的 `Blitter` 为每个元素提交独立 `TextureSetup`/ARGB render state；1.20.1 回移使用立即式 `Blitter`，在 ScreenStyle 背景与自定义层之间 `GuiGraphics.flush()` 并复位混合/颜色状态，达到等价的跨纹理隔离。两者均在右上角 `(152,-5)` 放置 `20x20` 新版优先级标签并打开 AE2 Priority 子菜单；左侧工具栏包含 Storage Bus Help、清除、分区和四个设置按钮，使用新版 toolbar normal/hover/focus 背景、新版状态图标及 6px 间距，并保留新版 Storage Bus 的连接目标提示。右侧升级面板相对主界面使用 current-main 的 `top=0` 与 5px padding；旧依赖的灰色空升级槽图标被禁用，空槽改绘 `ae2-states.png` 的 current-main `BACKGROUND_UPGRADE` `(240,208,16,16)`。两个 part 都只提供 5 格共享升级库存，并允许这 5 格全部装入 capacity card。卸货总线在同一库存中额外接受最多 4 张 speed card，不把 fuzzy/inverter/capacity/speed 的兼容上限相加成更多物理槽。tooltip 阶段绘制 hover 时必须把菜单内相对槽坐标转换为 `leftPos/topPos` 窗口坐标，防止高亮落到屏幕左上角。
过滤器变化时 Package Storage Bus 请求重新挂载；Package Unpacking Bus 在首次预留、最终提交和阻塞重试时都使用当前规则。过滤变化不会把 held 包裹替换成另一个包裹；不再匹配时保持阻塞，玩家可以取回。两者均保留整包事务边界，不能部分暴露或拆出包裹内容。
```

## 11. 过滤规则

包裹过滤维度：

```text
颜色
marker
内容物
```

三者 AND。未设置的过滤项忽略。内容过滤只决定整包是否通过，绝不只处理包裹的一部分内容。

1.0 固定内容过滤语义：

```text
requiredContents 作为内容 AEKey allowlist 使用；ghost amount 只用于 UI/模板展示，不作为普通通过数量门槛。
requiredContents 为空表示不过滤。
非反转模式下，包裹内每个 AEKey 都必须存在于 requiredContents。
反转模式下，包裹内每个 AEKey 都不得存在于 requiredContents。
内容过滤只反转内容 AEKey，不反转颜色或 marker。
精确样板封装等内部路径可使用 PackageFilter 的 required amount 匹配，但普通总线/打包机过滤不使用数量作为通过条件。
packaged_processing_pattern 只有在每一个包裹都存在且使用同一个 marker 时才产生公共 marker 过滤条件；混合“有 marker/无 marker”或 marker 不同的多包裹样板不设置公共 marker 门禁。
```

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
阻挡模式
可选把输出口包裹导入相连 AE 网络
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
acceptsPlans 仅在本机 legacy 输入缓冲为空、GUI 真实输入缓冲为空、至少一个输出槽可接收包裹且待输出队列为空时返回 true。
pushPattern 的空样板槽、彩色处理样板与封装处理样板载体路径直接读取 KeyCounter 中的 GenericStack；AEItemKey、AEFluidKey 与其它 AEKey 均可进入 PackagePlanBuilder，只受容量档和类型数约束。
普通 AE2 processing pattern 且本地样板槽为空时，直接从 Pattern Provider 的 KeyCounter 内容生成包裹计划，避免 9 格临时输入缓存限制。
本地 package_pattern / packaged_processing_pattern 样板槽路径仍把 KeyCounter 转成临时 9 格物品输入缓冲，并复用本地装配计划逻辑，因此该兼容路径只接受可转成 ItemStack 的 AEItemKey。
全部校验通过后，才提交输出包裹、从 KeyCounter 扣减输入。
任何一步失败都保持 all-or-nothing：不消耗 Pattern Provider 输入，不生成半包裹。
本地 package_pattern 和 packaged_processing_pattern 与 GUI 输入共用同一套计划逻辑。
本地自由封装、普通 Pattern Provider 推送和彩色 Pattern Provider 推送均读取容量槽；无容量元件时使用 default 档。
已编码 package_pattern 使用目标 PackageData 做 exact package plan，不再先按默认容量自由规划后比对 hash。
带 appliedpackaging.colored_processing_pattern 扩展 NBT 的 AE2 encoded processing pattern 走彩色拆包路径。
彩色拆包读取 AE2 processing pattern 的 sparse input 槽位，按槽位颜色生成一个或多个包裹。
彩色拆包不依赖 AE2 已压缩的 IInput 顺序；即使相同 AEKey 被 AE2 汇总，仍按原始 sparse 槽位拆成不同颜色包。
带 appliedpackaging.packaged_processing_pattern 扩展 NBT 的 AE2 encoded processing pattern 走封装处理推送路径。
封装处理推送路径使用 AE2 原版 processing outputs 暴露给 Pattern Provider/Planner，装配室读取 packages[] 并输出一个或多个包裹。
封装处理 pushPattern 按 packages[] 中的 GenericStack 精确消费输入；包裹内容可包含 AEItemKey 或 AEFluidKey，输入不足或存在额外输入时整批拒绝。
一次 pushPattern 产生多个包裹时，优先填入 17 个真实输出槽；超过当前可用输出槽的包裹才写入待输出队列，输出槽腾出后 server tick/tryAssemble 继续吐出。
待输出队列写入方块实体 NBT，破坏方块时以合法包裹掉落。
自动导出开启时，server tick 会优先把任意输出槽中的包裹导出到机器背面端点；背面优先识别 AE2 MEStorage capability，其次回落到 Forge item handler。
已通过 GameTest 验证真实 AE2 Creative Energy Cell + Pattern Provider 方块网络可推送到 package_assembler。
已通过 GameTest 验证真实 AE2 Creative Energy Cell + Pattern Provider 方块网络可解码并推送带 packaged_processing_pattern NBT 的 AE2 encoded processing pattern。
已通过 GameTest 验证真实 AE2 Drive + 64k item cell + Crafting CPU + Pattern Provider 自动合成 job 会从 AE 网络抽取输入，并把 processing pattern 输入推入 package_assembler。
已通过 GameTest 验证装配室默认自动导出开关、NBT 持久化、相邻 item handler 导出和真实 AE2 Interface 网络导出。
```

方块实体状态：

```text
legacyInputSlots: 9 个旧物品输入槽，保留给旧存档与内部兼容路径
menuInputBuffer: 17 行 x 4 列真实 GUI 输入缓冲，共 68 个输入格，每格保存 ItemStack identity 与 long amount
outputSlots: 17 个 item slots，只允许合法包裹
patternSlot: 本地包裹样板/封装处理样板
capacitySlot: 可选容量元件
markerSlot: 可选 marker 物品槽
packageName: string
selectedColor: PackageColor
upgradeSlots: 6 格 AE2 upgrade inventory
blockingMode: boolean
autoExportToNetwork: boolean
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
colored processing pending package queue 持久化保存
容量槽识别 AE2 16k/64k/256k storage component、item/fluid storage cell 与 portable cell
marker 槽接受非包裹、非样板物品；自由封装、普通 Pattern Provider 和彩色 Pattern Provider 路径会把 marker 槽物品作为输出 marker
packageName、selectedColor、marker 槽与 upgrade inventory 均持久化保存；输出包裹有名称时写入 hover name
17 格输出缓存已落地；输出自动导出遍历全部输出槽，且只在存在待导出包裹时解析背面 AE2 MEStorage / Forge item handler 端点
```

普通处理样板：

```text
输入 A+B+C，输出 X
装配室生成 1 个包裹，颜色使用机器 selectedColor；默认 selectedColor 为 Fluix
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
未标色槽位按 Fluix 处理；无颜色 NBT 的原版 processing pattern 走普通默认打包路径并使用机器 selectedColor。
当前服务端执行使用装配室容量槽；无容量元件时回退到 default package capacity。
```

阻挡模式：

```text
如果 17 个 outputSlots 全部无法接收新的合法包裹：
  拒绝新的输入/新样板执行
```

阻挡模式不检查输出 AE 网络、包裹子网、相邻接口、目标机器或主网库存。

UI：

```text
上半部分：使用用户提供的 ME Package Assembler atlas 原图；包裹名称、颜色、marker 与右上容量元件槽逻辑对齐 ME Packager，自动导出等配置开关走 AE2 左侧悬浮 toolbar
下半部分：参考新版 AE2 样板终端 processing 模式滚动栏，左侧为 17 行 x 4 列真实输入缓冲，右侧为 17 行真实输出槽
滚动条位于输入栏左侧，并同时移动输入栏与输出栏的可见 4 行；每个可见行左侧显示 4 个输入格，右侧显示 1 个输出格
下半区中部样板槽参考分子装配室：放入 package_pattern 或 packaged_processing_pattern 后，输入栏显示过滤 ghost，并只允许插入样板要求的材料与数量
左侧输入栏不是 fake slot；点击或 shift-click 会真实转移玩家物品，可累计超过普通 stack size 的数量，只受包裹容量档和样板过滤约束
```

当前基础实现：

```text
package_assembler 已注册为方块、方块物品和方块实体。
方块实体提供 9 格 legacy 输入缓冲、68 格 GUI 真实输入缓冲、1 格样板槽、17 格输出槽、1 格容量槽、1 格 marker 槽与 6 格 AE2 upgrade inventory。
非潜行右键打开 Package Assembler GUI。
GUI 接入 AE2 `UpgradeableScreen`、`UpgradeableMenu` 和 `ScreenStyle` 体系；style JSON 位于 `assets/ae2/screens/appliedpackaging/package_assembler.json`，背景贴图位于 `assets/appliedpackaging/textures/gui/mepackageassembler.png`。
背景 atlas 保持用户提供的 256x256 PNG 原图，ScreenStyle 使用主界面 `srcRect` 176x239；玩家物品栏、hotbar、标题和上半区控件按贴图实测坐标写入 style JSON。
可见区显示 4x4 输入格和 4 个输出格，左侧滚动条同步浏览全部 17 行；AE2 1.20.1 style grid 没有 4 列枚举，因此 4 行输入槽在菜单中拆成多组 AE2 slot semantics，由 style JSON 分别定位。
滚动输入/输出槽为真实菜单槽位，不是 fake slot；槽背景由客户端按 AE2 slot background 风格绘制，避免把动态滚动槽全部烘进背景图。
GUI shift-click 会优先把 package_pattern / packaged_processing_pattern 放入样板槽，把 AE2 容量元件放入容量槽，其它物品进入 GUI 真实输入缓冲；marker 槽通过真实槽手动放入，避免普通输入物品被误路由。
服务端 tick 自动尝试把输入缓冲完整封装为 1 个包裹。
输入中的合法包裹会先展开，再与散装物品合并封装。
全部输出槽无法接收新包裹时阻挡，不消耗任何输入。
样板槽可放入 package_pattern 或 packaged_processing_pattern。
容量槽使用与 ME Packager 相同的 AE2 16k/64k/256k 容量元件映射，并且不消耗容量元件。
如果 package_pattern 已编码，装配室只接受与样板 canonical hash 完全一致的输入计划。
已编码 package_pattern 不会被消耗，输出包裹颜色跟随样板颜色。
如果 packaged_processing_pattern 已编码，装配室读取有序 package list，每次生成一个当前输入可满足且 canonical hash 匹配的包裹，并写入第一个可用输出槽。
packaged_processing_pattern 不会被消耗；其它输出槽仍可接收后续可满足包裹。
如果 AE2 encoded processing pattern 带彩色输入槽元数据，Pattern Provider pushPattern 会按 sparse input 槽位拆成对应颜色包裹，输入槽可包含物品或流体 AEKey。
彩色 Pattern Provider 推送可产生多个包裹；17 格输出槽优先接收，超过可用输出槽的余量进入 pending queue。
未编码样板或空样板槽时，装配室使用机器 selectedColor、marker 槽和 packageName，并按容量槽档位规划；selectedColor 默认 Fluix。
普通 Pattern Provider pushPattern 在空样板槽时直接从 KeyCounter 规划包裹，可承载超过 9 个物品栈或流体输入，受容量档、selectedColor、marker 槽和 packageName 约束。
自动导出默认开启，可在装配室 GUI 左侧 AE2 toolbar 中通过 auto-export 图标切换并保存到 NBT。
自动导出遍历全部输出槽中的合法包裹；目标拒绝或容量不足时保留输出槽内容，不丢弃、不消耗新的输入。
自动导出端点为机器背面，优先 AE2 MEStorage，其次 Forge item handler。
右侧升级面板使用 AE2 `UpgradesPanel`；升级兼容性通过 `Upgrades.add` 注册到 ME Package Assembler 方块物品，并由方块实体的真实 upgrade inventory 保存、读取和掉落。
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
upgradeSlots: 6 格 AE2 upgrade inventory，当前支持 redstone/capacity/speed/inverter card
networkSide: Direction
packageName
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
GUI 作为主入口，客户端使用 AE2 `UpgradeableScreen` + `ScreenStyle`，菜单继承 AE2 `UpgradeableMenu`。
样式文件位于 `assets/ae2/screens/appliedpackaging/me_packager.json`，背景贴图位于 `assets/appliedpackaging/textures/gui/mepackager.png`。
右侧升级面板使用 AE2 `UpgradesPanel`；升级兼容性通过 `Upgrades.add` 注册到 ME Packager 方块物品。
非潜行右键执行快速交互：
  手持合法包裹时走与外部 capability 相同的立即拆包输入规则。
  空手或非包裹物品时先尝试取出输出槽。
  输出槽为空且没有快速动作时打开 GUI。
其它非 network_side 面暴露 2 槽普通 item capability：slot 0 只在包裹可立即完整拆包时接收 1 个合法包裹，接收后不暂存到 inputSlot；slot 1 只导出 outputSlot 中的输出包裹；network_side 不暴露普通 item capability。
GUI 左侧按钮区包含帮助、清除配置、基于网络现存物品配置分区、过滤应用模式、打包激活模式和阻挡模式。
过滤区为 5 行 9 列 ConfigInventory；默认启用 2 行，最多 3 张容量卡各启用 1 行，未启用行由 AE2 OptionalFakeSlot 控制渲染和交互。
包裹配置区包含包裹名称输入、左侧小颜色选择按钮和右侧 marker 物品槽；marker 槽有物品时直接作为输出 marker 覆盖来源 marker。
容器区上方为容量元件过滤器槽，下方为包裹输入/输出口；容量元件槽只接受 AE2 16k/64k/256k storage component。
GUI shift-click 玩家背包内包裹时走与外部 capability 相同的直接拆包规则，每次最多接受 1 个包裹；机器处于 workingOperation 时拒绝该输入，不写入隐藏 inputSlot。
GUI 工作态期间在包裹配置区与输出口之间显示进度条；进度来源为服务端同步的 workingOperation 与剩余动画 tick。
红石卡未安装时，自动打包激活逻辑固定为 `HIGH_SIGNAL`：有红石信号时打包。
红石卡安装后，GUI 打包激活模式可在 `HIGH_SIGNAL`、`LOW_SIGNAL`、`ALWAYS`、`PULSE`、`NEVER` 间切换。
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
AE2 encoded processing pattern 可作为 colored_processing_pattern metadata 载体，也可作为 packaged_processing_pattern 处理输出载体。
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
终端保存并同步 selectedColor，GUI 提供 17 色 swatch，编码样板颜色跟随当前选择。
终端保存并同步 9 个输入槽颜色；选中颜色后左键点击输入槽角落色标即可把该槽设为当前颜色，右键点击清除该槽颜色。
编码 AE2 processing pattern 时，如果没有逐槽设色，则所有非空 sparse input slot 使用 selectedColor；如果存在逐槽设色，则只写入对应槽位颜色，未设槽位由装配室按 Fluix 默认处理。
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

包裹输出总线：

```text
从 AE 网络取已有包裹
按颜色/marker/内容过滤输出到相邻库存
不把散装物品打成包裹
不请求自动合成
不拆包
```

包裹拆包总线：

```text
网络尝试插入包裹
总线过滤包裹
展开包裹内容
模拟完整插入相邻目标
成功才接受包裹
提交后目标得到散装内容
```

当前基础实现：

```text
总线家族当前实现为 AE2 可连接方块端点，而不是 cable part。
三种总线均注册为水平朝向方块、方块物品、方块实体、配方、loot table 和 blockstate。
方块实体继承 AE2 AENetworkBlockEntity，持有 IManagedGridNode，可连接 AE2 网络。
总线背面作为相邻 Forge item handler 目标端点。

package_storage_bus:
  作为 IStorageProvider 挂载 PackageItemStorage。
  只枚举带 PackageData 的合法包裹。
  insert/extract 均拒绝散装物品和无 PackageData 的包裹。
  设置过滤模板后，只暴露、插入、抽取匹配过滤的包裹。
  不暴露包裹内部内容。

package_export_bus:
  周期性从 AE 网络缓存中选择合法包裹。
  设置过滤模板后，只选择匹配过滤的包裹。
  只输出已有包裹到背面库存。
  不把散装库存自动打成包裹。

package_unpacking_bus:
  周期性从 AE 网络选择合法包裹。
  设置过滤模板后，只选择匹配过滤的包裹。
  先模拟完整拆入背面库存，成功后才从网络抽取 1 个包裹并提交散装插入。
  不接受部分拆包。

过滤模板当前为 ghost 配置：手持已编码 package_pattern、packaged_processing_pattern 或合法包裹右键总线写入模板；
潜行空手右键清除模板；模板物品不被消耗，也不会作为实体库存掉落。
普通空手右键打开共享 Package Bus 配置 UI；UI 显示当前 ghost 模板，支持从光标物品复制模板、清除模板，以及从玩家背包 shift-click 有效模板复制为 ghost filter。
UI 仍复用 PackageFilter.fromTemplate，因此 package_pattern、packaged_processing_pattern 和合法包裹的颜色、marker、内容过滤语义与右键快捷配置完全一致。
Package Bus 配置 UI 也支持手工过滤器编辑：17 色 swatch 设置颜色过滤，marker ghost 槽从光标复制 1 个物品作为 marker，3 个 required content ghost 槽从光标复制物品/流体容器，右键复制 1 个物品或 1 个容器量，空光标点击清除；这些 ghost 编辑不消耗玩家物品。若 required content 光标物品是 Forge 流体容器，则过滤器保存对应 AEFluidKey 与流体数量，例如水桶保存 1000 mB water。鼠标悬停 required content ghost 槽并滚轮调整数量时，流体每步调整 1000 mB，物品/其它已存在 key 每步调整 1；数量不会降到小于一个调整步长。
手工过滤器以 PackageFilter NBT 保存到总线方块实体，保留旧 filter_template 读取兼容；复制真实模板时仍保存 ghost 模板物品用于显示，手工编辑后清除模板来源显示但保留实际过滤条件。

包裹总线家族当前不含批量 required content 编辑、任意 AEKey 直接手工过滤输入，也不提供 AE2 cable part 形态；Package Pattern Terminal 已单独实现为 AE2 cable part item。
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
```

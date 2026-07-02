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
acceptsPlans 仅在本机输入缓冲为空、输出槽为空且待输出队列为空时返回 true。
pushPattern 当前只接受 AEItemKey 输入；遇到 AEFluidKey 或其它非物品 AEKey 时整批拒绝。
普通 AE2 processing pattern 且本地样板槽为空时，直接从 Pattern Provider 的 KeyCounter 内容生成包裹计划，避免 9 格临时输入缓存限制。
本地 package_pattern / packaged_processing_pattern 路径仍把 KeyCounter 转成临时 9 格输入缓冲，并复用本地装配计划逻辑。
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
封装处理 pushPattern 当前只接受 item-only packages；遇到包裹内容中的非物品 AEKey 或额外输入时整批拒绝。
一次 pushPattern 产生多个包裹时，先输出第一个，剩余包裹写入待输出队列；输出槽清空后 server tick/tryAssemble 继续吐出。
待输出队列写入方块实体 NBT，破坏方块时以合法包裹掉落。
已通过 GameTest 验证真实 AE2 Creative Energy Cell + Pattern Provider 方块网络可推送到 package_assembler。
已通过 GameTest 验证真实 AE2 Creative Energy Cell + Pattern Provider 方块网络可解码并推送带 packaged_processing_pattern NBT 的 AE2 encoded processing pattern。
已通过 GameTest 验证真实 AE2 Drive + 64k item cell + Crafting CPU + Pattern Provider 自动合成 job 会从 AE 网络抽取输入，并把 processing pattern 输入推入 package_assembler。
```

方块实体状态：

```text
inputBuffer: GenericStackBuffer
outputSlots: 当前 1 item slot；目标扩展为 17 item slots，只允许合法包裹
patternSlot: 本地包裹样板/封装处理样板
capacitySlot: 可选容量元件
defaultColor: PackageColor
defaultMarker: MarkerSpec optional
blockingMode: boolean
autoExportToNetwork: boolean
lastFailure: enum/string
```

当前 0.1.0-dev 落地状态：

```text
input slots 0-8
SLOT_PATTERN = 9
SLOT_OUTPUT = 10
SLOT_CAPACITY = 11
Forge item handler capability 暴露完整 12 格机器库存
AE2 CRAFTING_MACHINE capability 暴露装配室本体
colored processing pending package queue 持久化保存
容量槽识别 AE2 16k/64k/256k storage component、item/fluid storage cell 与 portable cell
17 格输出缓存和自动导入 AE 网络仍属于后续实现项；当前为 1 格输出槽
```

普通处理样板：

```text
输入 A+B+C，输出 X
装配室生成 1 个 Fluix/default 包裹，内容为 A+B+C
AE2 自动合成仍然等待 X
```

彩色处理样板：

```text
普通处理样板 + 输入格颜色元数据
按输入格颜色分组生成多个包裹
颜色跟随样板输入格，不跟随 AEKey
同一种 AEKey 位于两个颜色格时，必须生成两个不同颜色的包裹
颜色元数据保存于 AE2 encoded processing pattern 的 appliedpackaging.colored_processing_pattern NBT。
NBT 中的 inputs[] 以 AE2 processing pattern sparse input 槽位为索引。
未标色槽位按 Fluix 处理；无颜色 NBT 的原版 processing pattern 走普通默认打包路径。
当前服务端执行使用装配室容量槽；无容量元件时回退到 default package capacity。
```

阻挡模式：

```text
如果 outputSlots 中存在任意合法包裹：
  拒绝新的输入/新样板执行
```

阻挡模式不检查输出 AE 网络、包裹子网、相邻接口、目标机器或主网库存。

UI：

```text
左侧：样板槽、本地输入缓存、容量元件槽
中间：包裹计划预览和彩色分组
右侧：当前 1 格输出口；目标 17 格输出口
下方：默认颜色、default marker、阻挡模式、自动导入 AE 网络、状态文本
```

当前基础实现：

```text
package_assembler 已注册为方块、方块物品和方块实体。
方块实体提供 9 格输入缓冲、1 格样板槽、1 格输出槽与 1 格容量槽。
非潜行右键打开 Package Assembler GUI。
GUI 提供 9 格输入缓冲、样板槽、输出槽、容量槽与玩家背包；shift-click 会优先把 package_pattern / packaged_processing_pattern 放入样板槽，把 AE2 容量元件放入容量槽，其它物品进入输入缓冲。
服务端 tick 自动尝试把输入缓冲完整封装为 1 个包裹。
输入中的合法包裹会先展开，再与散装物品合并封装。
输出槽非空时阻挡，不消耗任何输入。
样板槽可放入 package_pattern 或 packaged_processing_pattern。
容量槽使用与 ME Packager 相同的 AE2 16k/64k/256k 容量元件映射，并且不消耗容量元件。
如果 package_pattern 已编码，装配室只接受与样板 canonical hash 完全一致的输入计划。
已编码 package_pattern 不会被消耗，输出包裹颜色跟随样板颜色。
如果 packaged_processing_pattern 已编码，装配室读取有序 package list，每次在输出槽为空时生成一个当前输入可满足且 canonical hash 匹配的包裹。
packaged_processing_pattern 不会被消耗；输出被取走后可继续生成该处理样板中的下一个可满足包裹。
如果 AE2 encoded processing pattern 带彩色输入槽元数据，Pattern Provider pushPattern 会按 sparse input 槽位拆成对应颜色包裹。
彩色 Pattern Provider 推送可产生多个包裹；当前 1 格输出槽通过 pending queue 顺序吐出后续包裹。
未编码样板或空样板槽时，装配室使用 Fluix 包裹行为，并按容量槽档位规划。
普通 Pattern Provider pushPattern 在空样板槽时直接从 KeyCounter 规划包裹，可承载超过 9 个物品栈的输入，只受容量档约束。
当前不含 AE2 网络自动导出。
```

## 8. ME 打包机

职责：

```text
识别相邻存储端点
识别相邻 ME Interface 背后的存储子网
从端点生成包裹
把输入包裹拆入端点
包裹展开再封装
容量、过滤、marker、颜色策略
红石/按钮触发
```

不做：

```text
读取 Pattern Provider
读取/执行任何样板
扫描自己所在任意 AE 网络
充当 AE 网络设备提供库存
```

方块实体状态：

```text
inputSlot: 只允许合法包裹
outputSlot: 只允许合法包裹
capacitySlot
targetSide: Direction
packageColor
markerMode
overrideMarker
contentFilter
packageFilter
redstoneMode
sortMode
strictWholeEndpointMode
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
Forge item handler
Forge fluid handler
AE2 ME Interface adjacent subnet
未来扩展：其他 AEKey endpoint
```

打包触发：

```text
GUI Pack Once 按钮
红石脉冲 Pack Once
红石持续周期 Pack，默认关闭
```

打包事务提交前必须全部模拟：

```text
1. 端点抽取所有源内容可行
2. 源包裹作为输入可被完整消耗
3. 输出槽可接收生成包裹
4. marker 策略无冲突
5. 容量与类型数未超限
```

任何一步失败都不得改变端点、输入槽或输出槽。

拆包事务：

```text
accepted = 0
while stack.count > 0:
  if filter fails: break
  if target cannot accept every entry: break
  commit target insert for one package
  stack.shrink(1)
  accepted++
```

单个包裹永远不能拆一半。

当前基础实现：

```text
me_packager 已注册为方块、方块实体和方块物品。
非潜行右键打开 ME Packager GUI。
GUI 提供输入槽、输出槽、容量槽、过滤槽、marker 槽、玩家背包、17 色 swatch、Pack Once 图标按钮、marker 策略图标按钮和红石模式图标按钮。
潜行右键保留快速交互：
  手持合法包裹时放入输入槽。
  空手或非包裹物品时先尝试取出输出槽。
  输出槽为空时触发一次 pack/unpack。
红石模式可在 ignored/pulse/cyclic 三档切换。
pulse 为默认兼容模式，红石上升沿触发一次 pack/unpack。
cyclic 在持续供电时每 20 tick 尝试一次 pack/unpack；输出槽堵塞或端点不可用时不会改变源库存。
机器背面优先识别 AE2 `MEStorage` capability，可接入相邻 ME Interface 暴露的子网存储；若无 AE2 storage，则回落到 Forge item handler / fluid handler。
输入槽存在合法包裹时执行整包拆包。
输入槽为空时从背面库存或流体槽选择当前容量档可承载的内容生成包裹。
容量槽识别 AE2 16k/64k/256k storage component、item/fluid storage cell 与 portable cell。
过滤槽接受已编码 package_pattern、packaged_processing_pattern 或带 PackageData 的包裹。
过滤模板会提供颜色、marker 与 requiredContents：
  打包时颜色优先使用过滤模板颜色，否则使用 GUI swatch selectedColor。
  打包内容过滤只选择 requiredContents 中仍缺少的 loose item，避免无关物品先占满容量。
  marker 策略由 GUI 独立设置为 retain、override 或 clear。
  retain 会保留源包裹 marker；多个源包裹 marker 冲突时计划失败。
  override 优先使用 marker 槽物品作为输出 marker；marker 槽为空时兼容使用过滤模板 marker。
  clear 会生成无 marker 的输出包裹。
  拆包时输入包裹必须匹配过滤模板，否则不消耗包裹。
AE2 MEStorage 端点直接处理 AEKey/GenericStack，并会把 MEStorage 中已有包裹展开后再封装。
Forge fluid handler 端点处理 AEFluidKey/FluidStack，支持相邻流体槽打包和整包拆入流体槽；没有 MEStorage 时，混合物品+流体包裹不会拆入单一 Forge 端点。
周期红石模式已有菜单按钮、服务端 ticker、NBT 持久化和 GameTest 覆盖。
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
packaged_processing_pattern 本地物品仍保留读取兼容；AE2 encoded processing pattern 载体已覆盖 item-only 处理输出语义，普通玩家流程不再要求新增样板物品。
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
outputs: List<GenericStack>，当前 UI 先覆盖物品输出 ghost slots
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
  封装处理样板、空白样板若干、拆出包裹样板，可选普通处理样板副本
```

当前基础实现：

```text
package_pattern 与 packaged_processing_pattern 使用 PackagePatternItem，tooltip 会区分空白/已编码状态。
已编码 AE2 blank_pattern 使用客户端 tooltip event 追加 package_pattern 或 packaged_processing_pattern 内容；未编码 AE2 blank_pattern 保持 AE2 原版 tooltip。
PackagePatternDataStorage 在 ItemStack NBT 中写入 version、color 与嵌套 PackageData。
读取已编码样板时会按样板颜色复验嵌套 PackageData canonical hash。
PackagedProcessingPatternDataStorage 在 ItemStack NBT 中写入 version、color、packages[] 与可选 outputs[]。
读取封装处理样板时会逐个复验嵌套 PackageData canonical hash，并兼容旧的单包裹 PackagePatternDataStorage 写法。
package_pattern_terminal 已注册为水平朝向方块、方块物品、方块实体、菜单和客户端 screen。
终端基础外形已调整为 AE2 风格薄面板/part-like block model，而不是完整机器方块；当前仍以普通方块承载方块实体、菜单和 screen，真正 AE2 cable part 形态后置。
终端 GUI 当前提供 9 格预览输入、1 格样板槽、1 格输出、容量槽、marker 槽、3 个处理输出 ghost slots、17 色 swatch、9 个输入槽色标按钮，以及 Encode/Split 按钮。
样板槽接受未编码 package_pattern、未编码 packaged_processing_pattern、AE2 原版 blank_pattern、AE2 encoded processing pattern，或已编码 packaged_processing_pattern 作为 Split 来源。
输出尽量保留输入样板语义；AE2 blank_pattern 的单包裹无输出场景会输出为带 package_pattern NBT 的 AE2 blank_pattern，多包裹且无处理输出时会输出为带 packaged_processing_pattern NBT 的 AE2 blank_pattern，有处理输出时会输出为 AE2 encoded processing pattern 并附带 packaged_processing_pattern NBT；AE2 processing pattern 会复制 1 个输出并写入 colored processing metadata。
编码 package_pattern 时写入单个 PackageData；编码 packaged_processing_pattern 时按容量档把预览输入拆成有序 packages[]。
编码 packaged_processing_pattern 时会把处理输出 ghost slots 写入 outputs[]；点击 ghost slot 会复制光标物品与数量，右键复制 1 个，空光标点击清除，均不消耗玩家物品。
编码只读取预览输入，不消耗预览输入、容量槽或 marker 槽；只消耗 1 个未编码空白样板。
终端保存并同步 selectedColor，GUI 提供 17 色 swatch，编码样板颜色跟随当前选择。
终端保存并同步 9 个输入槽颜色；选中颜色后左键点击输入槽角落色标即可把该槽设为当前颜色，右键点击清除该槽颜色。
编码 AE2 processing pattern 时，如果没有逐槽设色，则所有非空 sparse input slot 使用 selectedColor；如果存在逐槽设色，则只写入对应槽位颜色，未设槽位由装配室按 Fluix 默认处理。
容量槽使用与 ME Packager 相同的 AE2 16k/64k/256k 容量元件映射。
marker 槽写入 package_pattern 的 MarkerSpec，可作为 ME Packager 过滤或 override 回退模板。
marker 槽写入 packaged_processing_pattern 时会应用到拆出的每个包裹计划。
Split 会把已编码 packaged_processing_pattern 拆回多个普通 package_pattern；输出槽逐张吐出，剩余拆分结果保存在终端 pending queue，保存/读取后可继续输出。
输出槽非空时不消耗空白样板；空白槽中的已编码 package_pattern 或 packaged_processing_pattern 会被拒绝。
默认初始选择为 Fluix。
当前已支持 AE2 原版 blank_pattern 作为 package_pattern 与无输出 packaged_processing_pattern 数据载体；也已支持 item-only packaged_processing_pattern 通过 AE2 encoded processing pattern 暴露 processing outputs 给 Pattern Provider/Planner。玩家配方入口已收敛到 AE2 原版 blank_pattern；本地 package_pattern / packaged_processing_pattern 只保留旧存档/测试兼容。仍不含流体/任意 AEKey 处理输出 ghost editor 和真正 AE2 cable part 形态。
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
Package Bus 配置 UI 也支持手工过滤器编辑：17 色 swatch 设置颜色过滤，marker ghost 槽从光标复制 1 个物品作为 marker，3 个 required content ghost 槽从光标复制物品和数量或右键复制 1 个，空光标点击清除；这些 ghost 编辑不消耗玩家物品。
手工过滤器以 PackageFilter NBT 保存到总线方块实体，保留旧 filter_template 读取兼容；复制真实模板时仍保存 ghost 模板物品用于显示，手工编辑后清除模板来源显示但保留实际过滤条件。

当前不含批量 required content 编辑、流体/任意 AEKey 手工过滤输入，也未实现 AE2 cable part 形态。
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
requiredContents 中每个 AEKey 都必须在包裹内存在至少指定 amount。
包裹可以包含额外内容。
不提供 any/all/exact 模式切换。
```

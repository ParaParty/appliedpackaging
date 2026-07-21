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
    List<GenericStack> contents,
    Optional<PackageLayout> layout,
    Optional<MarkerSpec> marker,
    long usedUnits,
    int usedTypes,
    String canonicalHash,
    int flags
) {}

public record PackageLayout(
    int slotCount,
    List<Integer> contentSlots
) {}

public record MarkerSpec(
    GenericStack stack
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
tag.appliedpackaging.package.layout.slot_count: int optional
tag.appliedpackaging.package.layout.content_slots: int[] optional
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
optional layout slot count and content slot indexes
contents in stored list order, each entry encoded by:
  key type id
  registry id
  normalized key payload
  amount
```

`PackageData` 创建时逐项复制输入 contents，不合并同一 AEKey，也不改变条目顺序；`PackageDataStorage.writeTag` 按该顺序写入 NBT，canonical hash 同样对顺序敏感。`PackageLayout.contentSlots` 的长度必须与 contents 相等、严格递增、全部位于 `0..slotCount-1`；不存在布局表示普通稠密顺序。布局存在与否、总槽数或任一原槽位不同都会产生不同身份，因此只有有序列表和布局完全一致的包裹才能自然堆叠。ME 打包机在调用包裹数据层之前按 canonical stack key 主动排序选中的虚拟条目且不生成布局；普通/包裹样板路径保留 sparse 布局；高级处理样板生成各列包裹时同样把该列非空条目对应的原行索引写入布局。

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
| default（空槽，1k 档） | 256 | 9 |
| 16k | 4096 | 16 |
| 64k | 16384 | 63 |
| 256k | 65536 | 63 |

三个容量元件档的单位上限分别等于其 AE2 名义容量的四分之一；类型上限继续使用 16/63/63，不把单位容量换算与类型数合并为一个近似值。

容量元件识别：

```text
AE2 16k Storage Component -> 16k 档
AE2 64k Storage Component -> 64k 档
AE2 256k Storage Component -> 256k 档
1k Storage Component、完整 Storage Cell 与 Portable Cell -> 拒绝
```

最终实现只匹配 AE2 1.20.1 的三个 storage component registry id，不使用 tag 或附属 Mod fallback。

`PackageCapacityProfile` 是 ME Packager 与 Package Assembler 唯一的容量元件映射入口；两台机器不得各自复制 registry id 判断。样板容量预检复用实际包裹计划的单位/类型计算：包裹样板检查编码的目标包裹，普通 AE2 已编码样板检查一个带主输出 marker 的 Fluix 包裹，高级处理样板按列分别检查每个预计输出包裹，任何一包超限都使整批计划无效。

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
acceptsPlans 仅在本地样板槽为空、GUI 真实输入缓冲为空、所有输出槽为空、待输出队列为空且没有合成进度时返回 true。
pushPattern 遵循 AE2 分子装配室语义：Pattern Provider 推入时可用本次 pattern 临时决定 recipe/plan，不要求把样板写入本机样板槽。
pushPattern 会直接读取 KeyCounter 中的 GenericStack；AEItemKey、AEFluidKey 与其它 AEKey 均可进入 PackagePlanBuilder，只受容量档和类型数约束。生成预计包裹后必须先逐包执行当前 `PackageCapacityProfile` 预检，预检失败时立即返回且不得扣减任何 KeyCounter。
普通 AE2 crafting / processing / stonecutting / smithing 等可解码样板在本地样板槽为空时，使用 Pattern Provider 的 KeyCounter 内容和本次 pattern 生成临时包裹计划，避免本地物品槽数量限制；包裹颜色取装配室持久化的 `selectedColor`，marker 优先取非空 `markerFilter`，否则把主输出归一为数量 1。
独立 `package_pattern` 不实现 AE2 `IMolecularAssemblerSupportedPattern`；只有 ME Package Assembler 通过 AE2 pattern decoder hook 解码后按包裹样板语义执行。
独立 `advanced_processing_pattern` 物品会按连续的 81 槽输入列临时生成有序多包裹计划；每列颜色只作用于该列，名称固定为空，marker 固定取主产物。该物品继承 AE2 `EncodedPatternItem` 并提供独立 `IPatternDetails` 处理样板解码器，以绕开原版单样板 81 输入上限；高级列 NBT 不写入 AE2 原版 `processing_pattern`。
全部校验和容量预检通过后，才从 KeyCounter 扣减输入并进入本机合成进度；进度完成后才提交输出包裹。
任何一步失败都保持 all-or-nothing：不消耗 Pattern Provider 输入，不生成半包裹。
本地样板槽接受 package_pattern、advanced_processing_pattern 与任意 AE2 已编码样板；空白或未编码样板不能放入，无法解码的损坏样板不能解锁输入或开始装配。
全部正式样板共用 GUI 输入过滤逻辑：没有样板时所有输入槽锁定；有样板时按样板顺序生成连续的非空输入过滤列表，只解锁实际输入项数量对应的真实空格。过滤列表只参与槽位数量、启用状态和插入校验，不作为菜单槽内容同步，GUI 不绘制过滤物品或过滤数量。
已编码 package_pattern 使用目标 PackageData 做 exact package plan；每个输入格必须严格匹配同位置样板内容的 AEKey 与数量，不允许额外输入。
普通 AE2 已编码样板读取非空编码输入作为本地输入过滤；crafting pattern 直接保留 3×3 编码槽位顺序和重复项，processing pattern 保留 sparse 输入顺序，其它可解码样板按 PatternDetails 输入顺序展开；每个输入格必须严格匹配同位置样板输入，不允许额外输入。
advanced_processing_pattern 的列元数据保留 processing sparse input 语义；本地装配直接读取最多 81 个连续列各自的 `inputs`，跳过空白后生成带原 column 索引的稠密过滤列表。GUI 和外部 item capability 使用该稠密索引，提交时再按 column 归组并保持原 row 顺序；同色列不得合并。
全部样板生成的每个包裹均按样板输入位置写入 contents；同一 AEKey 在多个位置出现时，装配室的稠密输入过滤、真实输入槽和最终 contents 都分别保留对应位置，不合并、不排序。普通样板完成校验逐位置比较 key 与 amount，不允许仅聚合总量后放行。
一次计划产生多个包裹时，进度完成后按顺序写入真实主输出；其余产物写入待输出队列。自动输出把这份真实有序列表视为一个完成批次：准入成功后在同一 server tick 内反复提升队首并提交，目标拒收时保留剩余列表和已准入状态供后续 tick 重试，列表清空后才结束批次。GUI/外部 capability 始终直接抽取同一真实队首，不存在 Pattern Provider `sendList` 的隐藏副本。
待输出队列和进行中的合成包裹写入方块实体 NBT。Pattern Provider 已经扣料的进行中包裹在破坏方块时作为合法包裹掉落；本地合成的材料在完成前仍位于真实输入缓冲，因此破坏时只掉落材料，不额外掉落尚未提交的预览包裹。
本地计划启动时只保存预计包裹与样板快照，不扣除真实输入。server tick 每次推进前重新规划并核对当前输入；缺料或数量不足时保留 `craftProgress` 并暂停，补齐后继续。样板快照改变时取消本地计划并把进度归零；达到 100 时再次逐位置核对，通过临时副本原子计算全部扣料，全部槽位都能扣除后才替换真实输入状态并提交输出包裹。任一扣料失败时保留输入与进行中计划，不生成输出。
只要任意输出槽已有物品，装配室不会启动新的本地合成或接受新的 Pattern Provider plan。
已通过 GameTest 验证真实 AE2 Creative Energy Cell + Pattern Provider 方块网络可推送到 package_assembler。
已通过 GameTest 验证真实 AE2 Drive + 64k item cell + Crafting CPU + Pattern Provider 自动合成 job 会从 AE 网络抽取输入，并把 processing pattern 输入推入 package_assembler。
已通过 GameTest 验证装配室输出模式、NBT 持久化、相邻 item handler 导出、真实 AE2 Interface 网络导出和外部 item handler 顺序抽取。
```

方块实体状态：

```text
menuInputBuffer: 按当前样板稠密非空输入数动态确定逻辑真实输入格，每格保存 ItemStack identity 与 long amount；81×81 是高级样板逻辑上限而不是装配室预分配槽数，缓冲按实际输入增长；GUI 只用 4×4 代理窗口显示当前稠密输入行
outputSlots: 17 个 item slots，只允许合法包裹
patternSlot: 本地包裹样板/高级处理样板/任意 AE2 可解码已编码样板
capacitySlot: 可选容量元件；hover 同时说明容量元件用途和当前 `unitLimit/typeLimit`
upgradeSlots: 5 格 AE2 upgrade inventory，只允许 speed card
craftProgress: 0-100
activePackages: 进行中合成输出队列
activeMenuPattern: 本地合成开始时的样板快照；非空表示 activePackages 尚未扣除 menuInputBuffer
outputMode: ME_NETWORK | ADJACENT_BLOCK | NONE，默认 ME_NETWORK；工作期间不可修改但仍显示 tooltip
blockingMode: boolean，默认 false；只约束自动输出启动，不约束玩家或外部 capability 主动抽取；工作期间不可修改但仍显示 tooltip
autoExportBatchActive: boolean；表示当前真实完成列表已经通过一次自动输出准入，跨 tick 重试期间不重复读取阻挡条件
autoExportBatchMode: ME_NETWORK | ADJACENT_BLOCK | NONE；记录已准入批次的目标类别
autoExportBatchDirection: 仅 ADJACENT_BLOCK 批次使用，记录从六个相邻面中选中的容器方向；ME_NETWORK 不保存相邻方向
pendingPackages: 输出槽被占用时等待吐出的有序包裹队列
selectedColor: PackageColor，默认 Fluix；作为普通/无样板配置持久保存，不因样板临时覆盖而改变
markerFilter: 一格 AE2 config-types 假槽；不消耗物品、不随方块掉落，非空时覆盖普通样板的主输出 marker
lastFailure: enum/string
```

当前 0.1.0-dev 落地状态：

```text
SLOT_PATTERN = 0
SLOT_OUTPUT = 1
SLOT_CAPACITY = 2
extra output slots 3-18；机器内部 ItemStackHandler 共 19 槽
装配室按普通 AE2 ME Interface 的 capability 架构只实现 `GenericInternalInventory` 与 `Capabilities.STORAGE`：当前样板动态形成 N 个 `AEKey + long amount` 的 menuInputBuffer 输入位和紧随其后的 1 个严格有序输出位。装配室不注册自定义 `IItemHandler`；物品、流体以及附属类型的 Forge capability 如有需要，统一由 AE2 的 generic inventory wrapper 从同一份 AEKey 库存派生。没有样板且没有残留输入时只暴露输出位。只有本地样板槽中存在有效且符合当前容量档的样板时，输入位才按稠密非空样板位置、任意 AEKey 与目标数量接受外部插入。外部不能抽取输入位，也不能插入输出位，只能从输出位按队首顺序抽取；样板、容量和升级配置槽始终不暴露
AE2 CRAFTING_MACHINE capability 暴露装配室本体
方块实体 capability invalidation 后会在 revive 时重建 `GenericInternalInventory`、`Capabilities.STORAGE` 与 `CRAFTING_MACHINE` 的 `LazyOptional`，区块卸载/重新激活后自动化入口不得永久失效
pending package queue 与 active package queue 持久化保存
容量槽只识别 AE2 16k/64k/256k storage component；空槽等同默认 1k 档并使用 256/9，三个元件的单位/类型上限依次为 4096/16、16384/63、65536/63。1k component 因与空槽同档而不接受，完整 storage cell 与 portable cell 同样拒绝
装配室持久保存 `selectedColor` 与 `markerFilter`。普通 AE2 已编码样板使用所选颜色，并在 marker 过滤槽非空时覆盖主输出；包裹样板读取自身颜色/marker，高级样板读取各列颜色和主产物 marker。菜单另同步只读的有效颜色与有效 marker：本地样板存在时显示该样板将使用的值，Pattern Provider `pushPattern` 工作期间从 `activePackages` 显示活动批次的共同值；多包颜色不同则显示 None，多包 marker 不同或全部无 marker 则显示空 marker。显示覆盖不写入真实 `selectedColor/markerFilter`，样板移除或任务结束后恢复机器配置
selectedColor、markerFilter、outputMode、blockingMode、autoExportBatchActive/Mode/Direction、craftProgress、严格有序输出队列与 upgrade inventory 均持久化保存
输出由一个真实主输出和一个只读的下一包预览组成；其余成品保存在严格有序队列中。GUI、玩家、自动导出和 Forge item handler 每次都只能从主输出取 1 个包裹，取出后立即把队首提升为新的主输出
输出模式为 ME_NETWORK 时直接写入本机接入网格的 AE 网络存储服务，为 ADJACENT_BLOCK 时按 `Direction.values()` 的稳定顺序扫描六个相邻面的 Forge item handler 并选择第一个可准入目标，为 NONE 时不自动导出。容器批次准入后锁定该相邻方向，目标暂时消失或拒收时不切换到其它容器；列表清空后下一批重新扫描。自动导出覆盖真实主输出和 pendingPackages，不建立隐藏发送队列
PackageAssemblerBlock 通过原版 `hasAnalogOutputSignal/getAnalogOutputSignal` 暴露三态比较器值：没有完成成品且未打包时为 0，`activePackages` 非空且没有完成成品时为 1，真实输出槽或 `pendingPackages` 中存在任一完成包裹时为 2；完成态优先于工作态，数值不表示包裹数量，同批多个包裹也只返回 2，不保存或返回虚拟历史信号。开始打包、成功提交首个真实输出以及自动导出清空完成列表时，装配室查找直连或隔一个红石导体、且输入面朝向装配室的原版比较器：清除其尚未执行的旧方块计划刻并以 HIGH 优先级预约 0-delay 计划刻。计划刻在下一世界 tick 的方块实体 tick 前采样相应真实状态，使最快连续装配仍逐次形成 `0 -> 1 -> 2 -> 0`；输出变化同时继续调用 `Level.updateNeighbourForOutputSignal`。自动导出若清空整个完成列表，该 server tick 不再启动下一次装配，下一 tick 才允许开始。该路径不延迟物品自动输出，也不保存比较器锁存值
```

普通 AE2 合成/处理等已编码样板：

```text
输入 A+B+C，输出 X
装配室生成 1 个所选颜色包裹，名称为空，marker 为 marker 配置槽的值；配置槽为空时回退到归一为数量 1 的主输出 X
AE2 Pattern Provider / Planner 视角的可见输出仍是原样板主输出 X
装配室不会把包裹伪装成 X，也不会把包裹内容登记为 ME 散装库存
生成的包裹只是中间物流单元，必须由后续拆包/机器处理真正产出 X 后，AE2 作业才会完成
```

高级处理样板：

```text
载体是 Applied Packaging 独立物品 `advanced_processing_pattern`，其 item 类继承 AE2 `EncodedPatternItem` 并返回 Applied Packaging 的处理样板详情；AE2 输入、输出、tooltip、Pattern Provider 和 Crafting CPU 仍按处理样板接口工作。
专属 NBT version 2 描述 0..80 连续包裹列，每列保存最多 81 个 sparse processing input、PackageColor 和可选 marker；AE2 根 `in` 只保存稠密执行输入，避免为末列材料写入数千个空 compound。读取器继续迁移 version 1 的扁平 `column * 81 + row` 数据。当前高级终端写入空名称，并把处理样板主产物归一为每列 marker；AE2 原版 `processing_pattern` 不允许承载这段数据。
装配室按列顺序生成 1..81 个包裹；空列不生成包裹，同色列保持为独立包裹。内部一个真实主输出加持久有序 `pendingPackages` 队列负责顺序吐出，固定 GUI 输出位数量不构成高级样板列数上限。
Pattern Provider push 时必须精确消费样板 sparse inputs，缺少输入或存在额外输入均整批拒绝。
高级样板的颜色和高级终端生成的主产物 marker 以列元数据为权威，不回退机器配置；若各列颜色不同，GUI 有效颜色显示 None；各列 marker 相同时 marker 假槽显示该共同物品，不同或缺失时显示空 marker。高级终端不提供名称编辑。
```

本机输出阻挡与目标阻挡：

```text
如果 17 个 outputSlots 任意一个非空：
  不启动新的本地合成
  不接受新的 Pattern Provider plan
  server tick 先按 outputMode 尝试输出/导出既有包裹

blockingMode 默认关闭：
  关闭时不检查所选输出目标已有内容
  开启时，从真实主输出、其它真实输出槽和 pendingPackages 计算本完成批次的包裹物品 key 集合，并对每个 key 执行 `dropSecondary()`；目标只要已有任一匹配类型就拒绝本次准入，无关物品不阻挡
  ME_NETWORK 直接检查本机所连网格的 MEStorage；ADJACENT_BLOCK 不读取机器 facing，也没有背面概念，而是依次检查六个相邻面的 item handler
  无论是否开启阻挡，新批次准入时只模拟当前真实队首包裹能否被目标完整接受；不得提前模拟队列中的后续包裹。ME 网络只有一个直接目标；相邻容器则据此选择并锁定首个能接收当前队首的方向
  准入后记录 autoExportBatchActive 和目标模式；容器模式额外锁定相邻方向。本 tick 继续输出同一真实完成列表，后续包裹不会被刚写入的同批包裹反向阻挡
  每次提交前只模拟当前真实队首包裹；目标容量或插入规则拒绝时立即停止，未提交包裹保持原有顺序和已准入状态等待后续 tick；重试不重新执行批次阻挡
  GUI/外部 capability 可在活动批次中抽取真实队首；只要列表仍非空，剩余成品保持原批次状态，列表取空后清除批次状态
  下一批成品提交前重置旧状态，必须重新执行目标选择、阻挡检查与队首单包裹模拟
```

ME_NETWORK 不通过相邻 Interface 的 item capability 间接输出，而是直接使用装配室主节点所属网格的存储服务；相邻 Interface 只可能作为该节点加入网格的连接方式。ADJACENT_BLOCK 只扫描六个紧邻方块，不递归扫描其它物流路径。玩家和外部 capability 的主动抽取仍只受严格队首顺序约束。活动批次中把 outputMode 切为 NONE 只暂停自动输出；改到另一目标类别时终止旧准入，恢复输出时对新目标重新准入。

UI：

```text
使用用户提供的 ME Package Assembler atlas 原图；样板与容量元件并列位于顶部，输出模式与阻挡模式配置走 AE2 左侧悬浮 toolbar；原图颜色区注册 16+1 色拾色触发按钮，marker 区注册不消耗物品的 AE2 FakeSlot
输入区参考新版 AE2 样板终端 processing 模式滚动栏；滚动条位于输入栏左侧并显示 4 行×4 列，最大行数按样板非空输入数和残留真实输入动态计算
输入格由用户 atlas 持有外框与分隔线，Screen 只在 `(slot.x-1,slot.y-1)` 叠加 `package-storagebus-sprites.png [0,64,18,18]` 的新版透明边框槽位 sprite；禁用格使用 0.2 opacity。菜单必须令 `IOptionalSlot.isRenderDisabled=false`，不得重新启用 AE2 15 `Icon.SLOT_BACKGROUND` 的旧 `states.png` 回退，也不得用纯色 fill 重建槽位内部
装配室小滚动柄使用项目回移的 current-AE2 sprite：enabled 为 `advanced_pattern_encoding_terminal_sprites.png [0,32,7,15]`，disabled 为 `[16,32,7,15]`；滚动条维持 `left=12`，更新后的底图只要求四行输入槽的 `left` 从 20 变为 21，不平移滚动条或其它槽
右侧固定显示一个真实主输出和一个不可交互的下一包预览；预览旁显示剩余队列数量，hover 显示剩余包裹提示
下半区中部样板槽参考分子装配室：槽内只允许已编码样板；放入样板后，输入栏按样板非空输入顺序建立对应数量的真实槽并跳过 sparse 空白，服务端用样板材料与数量过滤插入。空槽不显示样板过滤物品或数量，只有已真实投入的输入才作为槽内容同步和绘制。样板可在容量不足时保留于槽内供检查，但菜单同步 `patternCapacityValid=false`，客户端以红色覆盖样板槽并锁定全部空输入位；已有残留输入保持可取出。颜色触发按钮显示有效颜色：普通/无样板时显示所选颜色，包裹样板显示自身颜色，高级样板同色显示该色、混色显示 None；包裹/高级样板的覆盖仅存在于 `effectiveColor` 显示与读取路径，不写入 `selectedColor`，样板取出后自然恢复原选择。插入包裹或高级样板时颜色按钮保留 hover tooltip，但客户端按钮、客户端 action 和服务端 action 都必须拒绝单击修改。marker 假槽仍操作持久配置库存，但其 `getDisplayStack` 使用菜单同步的有效 marker，因此本地样板和无本地槽的 `pushPattern` 工作批次均能临时替换显示，移除样板或任务结束后恢复此前选择。有效 marker tooltip 跟随显示物品。工作期间颜色、marker、输出模式和阻挡模式禁止点击/修改但保留 hover tooltip，材料输入与真实输出交互不受该配置锁影响
左侧输入栏不是 fake slot；点击或 shift-click 会真实转移玩家物品，可累计超过普通 stack size 的数量，只受包裹容量档和样板过滤约束
```

当前基础实现：

```text
package_assembler 已注册为方块、方块物品和方块实体。
方块实体提供随当前样板稠密输入数变化的逻辑真实输入缓冲、1 格样板槽、17 格内部有序输出槽、1 格容量槽、1 格持久化 marker 配置假槽与 5 格 AE2 speed-card upgrade inventory，并持久保存普通样板所用的 17 色选择。
非潜行右键打开 Package Assembler GUI。
GUI 菜单继续使用 AE2 `UpgradeableMenu` 和 `ScreenStyle`，客户端改由 `ModernUpgradeableScreen` 回移 current-main 槽位 hover、升级面板与空升级槽视觉；style JSON 位于 `assets/ae2/screens/appliedpackaging/package_assembler.json`，背景贴图位于 `assets/appliedpackaging/textures/gui/mepackageassembler.png`。
背景 atlas 保持用户提供的 256x256 PNG 原图，ScreenStyle 使用主界面 `srcRect` 176x203；玩家物品栏、hotbar、标题和上半区控件按贴图实测坐标写入 style JSON。颜色触发按钮位于 `(95,29,12,12)`，8x8 swatch 落在 `(97,31)`；marker FakeSlot 原点为 `(108,32)`，与 `[107,31,18,18]` 框对齐。
可见区显示 4x4 输入代理格、1 个主输出和 1 个下一包预览，左侧滚动条只浏览由当前样板实际非空输入计算出的稠密行；后续全为 disabled 代理格时滚动范围为 0。AE2 1.20.1 style grid 没有 4 列枚举，因此 4 行输入槽在菜单中拆成多组 AE2 slot semantics，四组 `left=21`、top 依次为 33/51/69/87；本次右移只作用于输入槽，不改变样板、容量、marker、颜色、输出或滚动条。容量槽 hover 显示当前容量档的单位/类型上限。
滚动输入/输出槽为真实菜单槽位，不是 fake slot；槽背景由客户端按 AE2 slot background 风格绘制，避免把动态滚动槽全部烘进背景图。
GUI shift-click 会优先把任意 AE2 可解码已编码样板放入样板槽，把 AE2 容量元件放入容量槽，其它物品只在样板过滤允许时进入 GUI 真实输入缓冲。
样板槽为空时，服务端和菜单输入均拒绝物品输入，不再自由封装；若样板被取走但输入槽仍有残留物品，残留槽保持可取出并渲染为红色错误状态，空输入槽重新锁定。
服务端 tick 在输出侧没有阻挡且输入严格匹配本地样板或 Pattern Provider 临时 plan 时启动合成进度。本地输入在进度期间保留并可由 GUI 交互，缺料时暂停；进度达到 100 后重新核对、扣除输入并提交输出。
输入中的合法包裹会在其输入位置展开有序 contents，再与前后输入按原位置封装；同类条目不合并，展开内容不移动到列表末尾。
任意输出槽已有物品时阻挡，不消耗任何输入。
样板槽可放入 package_pattern、advanced_processing_pattern 与任意 AE2 可解码已编码样板。
容量槽通过 `PackageCapacityProfile` 使用与 ME Packager 相同的 AE2 16k/64k/256k storage component 映射，并且不消耗容量元件；空槽严格使用默认 1k 档的 256/9。全部本地样板在解锁输入槽前就计算预计包裹容量；超限时样板槽标红、GUI 与外部输入锁定、合成拒绝。全部 Pattern Provider push 均在消费输入前逐个预计包裹复验当前档位，高级样板按输出列独立计算，任一包超限即整批拒绝。
如果 package_pattern 已编码，装配室只接受与样板 canonical hash 完全一致的输入计划。
已编码 package_pattern 不会被消耗，输出包裹颜色跟随样板颜色。
如果 advanced_processing_pattern 已编码，装配室按连续列读取有序多包裹计划；17 格输出槽优先接收，超过可用输出槽的余量进入 pending queue。
普通 Pattern Provider pushPattern 在本地样板槽为空时直接从 KeyCounter 和本次 AE2 pattern 规划一个 Fluix、空名称、主输出 marker 的包裹，可承载超过 9 个物品栈或流体输入，并受容量档约束。
输出模式默认 ME_NETWORK，可在装配室 GUI 左侧 AE2 toolbar 中循环切换 ME_NETWORK、ADJACENT_BLOCK 和 NONE，并保存到 NBT。
自动导出按输出槽顺序一次只处理 1 个包裹；目标拒绝或容量不足时保留输出槽内容，不丢弃、不消耗新的输入。
右侧升级面板使用 current-main `UpgradesPanel` 的 1.20.1 回移实现与 5px padding；空槽从独立原样 `ae2-states.png` 绘制 current-main `BACKGROUND_UPGRADE`，不再混入 AE2 15 灰阶占位。顶部已编码样板槽和容量元件槽不再调用依赖 AE2 15 的 `Icon`，而是分别从同一原样 `ae2-states.png` 绘制 current-main `BACKGROUND_ENCODED_PATTERN (240,112,16,16)` 与 `BACKGROUND_STORAGE_COMPONENT (240,48,16,16)`。升级兼容性通过 `Upgrades.add(AEItems.SPEED_CARD, package_assembler, 5)` 注册，只允许 5 张加速卡，并由方块实体的真实 upgrade inventory 保存、读取和掉落。
合成进度使用 AE2 分子装配室速度表与能量税率：0/1/2/3/4/5 张加速卡每 tick 分别尝试推进 10/13/17/20/25/50，能量倍率分别为 1.0/1.3/1.7/2.0/2.5/5.0；每 tick 先从本机 AE grid energy service 抽取 AE 能量，网络不可用或能量不足时不推进，最大进度 100。
```

## 8. ME 打包机

职责：

```text
通过固定底部与模型背面加入 AE 网格并识别该网格的 MEStorage
识别相邻 ME Interface 背后的存储子网
从端点生成包裹
把输入包裹拆入端点
包裹展开再封装
空容量槽默认 1k 档的 256 单位/9 类型，以及 16k/64k/256k 容量元件档、过滤、marker、颜色策略
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
heldBox: 唯一包裹工作槽，`heldBoxState` 区分 EMPTY / UNPACK_INPUT / PACK_OUTPUT
capacitySlot: 只允许 AE2 16k/64k/256k storage component；为空时使用默认 1k 档的 256 单位/9 类型
contentFilter: 45 格 AE2 GenericStack 配置，基础 2 行，最多 3 张容量卡各解锁 1 行
upgradeSlots: 6 格 AE2 upgrade inventory，当前支持 capacity/speed/inverter card
selectedColor
markerFilter: 1 格 AE2 config-types fake slot，只保存 marker AEItemKey，不保存真实物品
markerMode: retain / override / clear
filterApplicationMode: both / pack_only / unpack_only
blockingMode: ignore_network_contents / block_unpack_when_network_has_items
redstoneMode
workingOperation: none / packing / unpacking
workingStack: 工作动画中的 1 个包裹；packing 时是已抽取但尚未进入 heldBox 的待输出包裹，unpacking 时仅作视觉记录
pendingPackTrigger: 工作态期间收到的单次打包触发
lastEndpointInfo
lastFailure
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
仅右键传送带上表面时可放入输入包裹或取出输出包裹；命中其它模型表面时打开 GUI
```

打包提交前必须全部模拟：

```text
1. 端点抽取所有源内容可行
2. heldBox 为空且可接收生成包裹
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
动画结束同 tick 把 workingStack 放入 heldBox，并标记 PACK_OUTPUT
清空工作态
```

任何一步失败都不得改变端点或 heldBox。打包已抽取但动画尚未结束时，workingStack 属于机器状态；保存/读取后继续完成，破坏方块时作为待输出包裹掉落。

MEStorage 拆包提交：

```text
外部 capability insert 每次最多接受 1 个包裹
允许输入前必须满足：
  机器不在 workingOperation
  heldBox 不处于 PACK_OUTPUT
  filterApplicationMode 不是 pack_only
  如果 selectedColor 不是 fluix，包裹 item 颜色必须等于 selectedColor
  如果 markerFilter 存在，包裹 PackageData.marker 必须等于 markerFilter
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
方块状态只包含水平朝向 `facing`。底部 `DOWN` 与模型背面 `facing.getOpposite()` 是固定 AE 连接面；不存在独立可切换的连接面状态。
方块实现 AE2 `IOrientableBlock` 并使用 `OrientationStrategies.horizontalFacing()`；AE2 扳手通过全局 `WrenchHook` 旋转 `facing`，不维护自定义扳手识别或接线切换分支。
GUI 作为主入口，菜单继承 AE2 `UpgradeableMenu` 并使用 `ScreenStyle`，客户端改由 `ModernUpgradeableScreen` 回移 current-main 槽位 hover、升级面板与空升级槽视觉。
样式文件位于 `assets/ae2/screens/appliedpackaging/me_packager.json`，背景贴图位于 `assets/appliedpackaging/textures/gui/mepackager.png`。
右侧升级面板使用 current-main `UpgradesPanel` 的 1.20.1 回移实现与 5px padding；空升级槽使用独立原样 `ae2-states.png` 的 current-main `BACKGROUND_UPGRADE`。容量元件槽不再调用依赖 AE2 15 的 `Icon.BACKGROUND_STORAGE_COMPONENT`，改从同一原样资源绘制 current-main `(240,48,16,16)`。升级兼容性通过 `Upgrades.add` 注册到 ME Packager 方块物品。空 marker 槽从用户 sprite `(32,16,16,16)` 绘制自有图标，hover 使用 current-main 蓝色填充/边线并提供双行说明 tooltip。
右键传送带上表面执行快速交互：
  手持合法包裹时走与外部 capability 相同的立即拆包输入规则。
  空手或非包裹物品时尝试取出输出槽。
  传送带命中但没有可执行动作时不打开 GUI；右键机框、背板、侧面、底面等其它位置打开 GUI。
底部与模型背面不暴露普通 item capability；其它四个非 AE 连接面暴露 1 槽普通 item capability：slot 0 在空闲时可接收 1 个能够完整拆包的合法包裹，也可导出 heldBox 中的 PACK_OUTPUT。
方块实体 capability invalidation 后会在 revive 时同时重建内部与外部 item handler `LazyOptional`。
GUI 左侧按钮区包含帮助、清除配置、基于网络现存物品配置分区、过滤应用模式、打包激活模式和阻挡模式。
过滤区为 5 行 9 列 ConfigInventory；默认启用 2 行，最多 3 张容量卡各启用 1 行，未启用行由 AE2 OptionalFakeSlot 控制渲染和交互。
包裹配置区只包含颜色选择按钮和 marker fake/config slot；marker 配置存在时直接作为输出 marker 覆盖来源 marker，不消耗或掉落玩家物品。
容量元件槽只接受 AE2 16k/64k/256k storage component。包裹输入和打包输出共用一个真实 heldBox，通过 EMPTY、UNPACK_INPUT、PACK_OUTPUT 状态区分语义，禁止输出回流到拆包路径。
GUI shift-click 与外部 capability 每次最多接受 1 个包裹；输入先保留在 heldBox，进度结束时才在完整模拟通过后写入 ME。若网络变化导致最终提交失败，heldBox 保留输入并显示阻塞红底，定时重新验证并重启进度，且允许玩家取回。
GUI 工作态期间在包裹配置区与输出口之间显示进度条；进度来源为服务端同步的 workingOperation 与剩余动画 tick。
GUI 左侧打包激活模式直接控制实际红石逻辑，可在 `HIGH_SIGNAL`、`LOW_SIGNAL`、`ALWAYS`、`PULSE`、`NEVER` 间切换；默认 `HIGH_SIGNAL`，不使用额外红石卡作为隐藏门槛。
反转卡安装后只反转内容过滤，不反转 selectedColor 或 markerFilter 门禁；内容过滤为空时仍表示不过滤。
红石只控制自动打包；输入包裹拆包不受红石关闭影响，仍受拆包过滤、阻挡模式、目标容量和目标在线状态约束。
工作态期间拒绝新的输入；持续打包触发等机器空闲后再重试，红石脉冲或手动单次打包触发在工作态期间记录为 pendingPackTrigger，空闲后尝试一次。
ME 打包机使用两套离散速度表：打包 0-6 张加速卡为 40/30/20/15/10/6/4 tick；拆包只识别前 4 张，为 20/15/10/6/4 tick，5-6 张仍按 4 张计算。开始工作时锁定本次周期长度，方块实体 NBT 与 visual stream 同步剩余 tick 和周期总长，GUI 进度按实际周期计算。包裹与传送带的可见移动窗口最多 20 tick，周期超过 20 tick 时只使用最后 20 tick；包裹内侧中心位于本地 `x=1/16`，使出现/消失端点收在 `x=3..4/16` 的帘子后方，外侧中心仍为 `x=10/16`。传送带滚动量由同一包裹位移增量换算，完整进出均为 9px，不再按每 tick 固定滚动。帘子另存有符号偏转和回弹速度，包裹经过时由实际位移速度加权推动，再以独立弹性和阻尼恢复；工作结束不强制归零，未完成的回弹继续自然运行。
底部与模型背面共同暴露同一个 AE2 主节点；只有这两个面可以接入 AE2 线缆或相邻 ME Interface 网络，无 AE2 storage 时返回 NO_TARGET，不回落 Forge item handler / fluid handler。
真实 AE2 Creative Energy Cell + Drive + Interface + ME Packager GameTest 覆盖从相邻 Interface 网络打包、抽走网络内容、再整包拆回网络。
真实 AE2 底面 Interface GameTest 覆盖固定底部接线；独立真实线缆 GameTest 覆盖固定模型背面接线，并逐面断言其它四面为 `AECableType.NONE`。
真实世界相邻 Forge item handler / fluid handler 反例 GameTest 覆盖无 MEStorage 时不打包、不拆包、不消耗相邻 Forge 端点。
唯一 heldBox 是当前工作项：拆包时保存输入包裹，打包时保存已生成待输出包裹；`held_box_state` 明确区分两种语义。
输入侧为空且机器空闲时，从已连接 AE 网络选择空容量槽默认 1k 档的 256 单位、9 类型上限可承载的内容生成包裹；生成包裹先进入 packing 工作态，动画结束后进入唯一 outputSlot，客户端停在本地 `(x=10/16,z=8/16)` 的 12x16 前部区域中心，物品模型底面精确贴合传送带顶面 `y=2/16`。BER 在机器局部坐标内为包裹额外施加 `Y +90°`，使 `FIXED` item transform 后的正面朝向本地工作口 +X，再统一随方块 `facing` 旋转。BER 只渲染方块实体 stream 明确同步的 `renderedBox`；服务端清空该视觉栈后必须立即停止渲染，不得回退到客户端可能过期的 heldBox NBT。
容量元件槽可把当前单包单位上限提升到对应元件名义容量四分之一的 4096/16384/65536；容量卡只解锁过滤行，不改变包裹容量档。
过滤配置只使用当前 45 格 contentFilter，不读取隐藏旧槽。
过滤模板接受已编码 package_pattern 或带 PackageData 的包裹。
过滤模板会提供颜色、marker 与 requiredContents：
  打包时颜色优先使用过滤模板颜色，否则使用 GUI swatch selectedColor。
  打包内容过滤只选择 requiredContents 中出现的 AEKey，忽略 ghost amount 作为数量上限；反转卡存在时选择 requiredContents 之外的 AEKey。
  marker 策略是当前打包配置；marker 槽物品在 override 模式下优先作为输出 marker。
  retain 会保留源包裹 marker；多个源包裹 marker 冲突时计划失败。
  override 优先使用 marker 槽物品作为输出 marker；marker 槽为空时使用过滤模板 marker。
  clear 会生成无 marker 的输出包裹。
  过滤应用模式为 both 时打包和拆包都使用内容过滤；pack_only 只约束打包且外部包裹输入被拒绝；unpack_only 只约束拆包。
  拆包过滤启用时，输入包裹的全部内容 AEKey 必须满足 allowlist 或反转后的 denylist；包裹 item 颜色和 markerItem 作为独立门禁同时满足。
阻挡模式为 block_unpack_when_network_has_items 时，如果目标 ME 网络已有任意可见内容，则拒绝拆包且不消耗输入包裹。

防堵塞模式使用独立 boolean 与 NBT key，默认开启。输入先执行包裹数据、颜色、marker、内容过滤等恒定合法性检查，再调用真实自动拆包的输出判定；该判定包含 ME 目标存在/在线、整包累计容量以及上述阻挡模式。开启时只有输出判定通过才允许 capability、GUI shift-click 或世界交互接收；关闭时可把一个合法包裹写入同一真实 held 槽，输出判定未通过则不启动动画、标记阻塞并由自动拆包 tick 重试。防堵塞只决定是否接收，不改变最终进度终点的完整复验，也不禁止玩家取回等待包裹。
AE2 MEStorage 端点直接处理 AEKey/GenericStack，并会把 MEStorage 中已有包裹展开后再封装。
红石卡门槛、脉冲打包、持续高信号打包、红石关闭仍允许拆包和过滤行容量卡解锁已有 GameTest 覆盖；GUI 视觉按需通过人工客户端检查。
```

## 9. 样板与终端

包裹样板物品 ID：`package_pattern`

样板承载约束：

```text
独立 `appliedpackaging:package_pattern` 是高级样板终端包裹页的唯一编码产物；AE2 pattern decoder 返回只被 ME Package Assembler 使用的 PackageCraftingPatternDetails。
包裹页拥有独立的 81 个 sparse 输入、颜色与 marker 状态，输出包裹由该页输入内容自动计算，不复用 AE2 crafting grid 或高级页输出。
独立 `advanced_processing_pattern` 物品承载包裹列元数据，并通过自定义 `IPatternDetails` 处理样板解码路径参与 Pattern Provider 与 Crafting CPU；普通 AE2 `processing_pattern` 不承载高级列数据。
发布前不读取 AE2 blank/crafting pattern 的旧包裹载体，也不读取 colored/packaged processing 扩展 NBT。
普通 Pattern Encoding Terminal 的 `ENCODED_PATTERN` 槽拒绝 `package_pattern` 与 `advanced_processing_pattern`；只有 `AdvancedPatternEncodingTermMenu` 允许这两种物品，以保证普通终端不会接收无法编辑的专用状态。
```

包裹样板数据：

```text
PackageColor
MarkerSpec optional
List<GenericStack>
CapacityProfile
```

高级处理样板物品与 NBT：

```text
item id: appliedpackaging:advanced_processing_pattern
tag.appliedpackaging.advanced_processing_pattern.version: int = 2
tag.appliedpackaging.advanced_processing_pattern.columns: list<compound>
columns[].index: int，必须从 0 开始连续且小于 81
columns[].color: string，PackageColor.id()
columns[].marker: optional GenericStack compound，只允许数量为 1 的 AEItemKey marker
columns[].inputs: sparse GenericStack list，尾部空位裁剪，长度最多 81
根 NBT 的 in/out 沿用 AE2 processing pattern 编码，但 `in` 是按列/行顺序压缩后的稠密执行输入；version 1 仍按 `n * 81 + row` 读取并迁移到列数据
高级终端只写入启用列范围；未启用列中的旧 ghost 数据不得进入新样板
```

当前基础实现：

```text
package_pattern 的 tooltip、解码与输出预览都由正式注册的 `PackageCraftingPatternItem` 自己实现，并由 `PackageCraftingPatternDataStorage` 写入/读取当前 package_crafting_pattern 数据；不得通过未注册的遗留物品类静态转发 tooltip。
AE2 原版 Pattern Encoding Terminal 继续由 AE2 的 `InitScreens` factory 创建原生 `PatternEncodingTermScreen`，四种原生模式、ScreenStyle、终端底图、recipe preview、数量编辑、tooltip 和编码按钮全部执行 AE2 原路径。本模组不向该 Screen 注册包裹按钮或包裹 panel，也不替换、委托或条件接管其绘制。唯一原版终端注入位于 `AEBaseMenu.isValidForSlot`：仅当实际菜单是普通 `PatternEncodingTermMenu`、语义是 `ENCODED_PATTERN` 且物品是两个 Applied Packaging 专用载体之一时返回 false；其它菜单和槽位继续走 AE2 原逻辑。
`AdvancedPatternEncodingTerminalPart` 同时持有 `AdvancedPatternEncodingState`、`PackagePatternEncodingState` 与持久化 `SpecializedPatternMode`。高级状态逻辑上保存 81×81 个 sparse 输入、4 个输出、启用列数、列颜色与 `AdvancedPatternColorMode`；菜单只创建 4×81 个输入窗口槽，避免为 6561 个逻辑位置创建 FakeSlot。服务端窗口按同步首列动态映射到完整 sparse 状态；客户端只缓存当前 4×81 个窗口槽，不保留不可见绝对列。滚动、清空、删列或列数收缩时，Vanilla/AE2 的窗口槽差量同步因此始终以“上一窗口对下一窗口”工作，不会在滚动归零后重新暴露旧第一列缓存。包裹状态独立保存 81 个输入、颜色和 marker。页面切换只改变模式字段和两组槽位的 active/屏幕坐标，不复制、迁移、清空或复用任一页数据。
`AdvancedPatternEncodingTermMenu` 仍继承 AE2 `PatternEncodingTermMenu` 以复用网络库存、空白/已编码载体、容器交互与数量包语义，但强制底层 `EncodingMode.PROCESSING`，并把继承的 crafting、smithing、stonecutting 和默认 processing 编辑槽全部停用。菜单为高级页和包裹页分别创建自有 fake slot；两个页面唯一共享的是 `BLANK_PATTERN` 与 `ENCODED_PATTERN` 载体槽。覆盖 `hideViewCells()` 返回 true，使构造阶段完全不创建 `VIEW_CELL` 槽。
模式权威只有 `SpecializedPatternMode.ADVANCED/PACKAGE`。点击右侧模式按钮时客户端先做同值本地投影并发送一次 `apSetSpecializedPatternMode`，服务端保存到 part 后由 GuiSync 回传最终状态；切换过程中 Screen 和 Menu 实例始终不变。已编码槽放入 `advanced_processing_pattern` 时只解码到高级状态并切至 ADVANCED，放入 `package_pattern` 时只解码到包裹状态并切至 PACKAGE。载入包裹样板后，预览直接从包裹状态的 sparse 输入、颜色和 marker 生成，不调用 crafting recipe 或高级输出逻辑。
`AdvancedPatternEncodingTermScreen` 是合并终端唯一 Screen。两页共享同一个外框尺寸和所有外围锚点：两行网络库存时均为 195x245、bottom 区 192px；网络行增加时只按每行 18px 向下扩展。更新后的 `advanced_pattern_encoding_terminal.png` 在 screen `(8,68)` 提供 132x78 灰色底板；`pattern_mode_packaging.png` 的上半区 `[0,0,132,78]` 是包裹面板，下半区 `[0,128,132,78]` 是高级面板。两页均在 full-screen base 后把对应面板绘制到 `(8,68)`，再绘制动态槽背景和物品；切换不改变窗口中心、标题、搜索、网络滚动条、玩家栏、载体槽、Encode 或合成状态位置。
模式改变时禁止调用 `resize/init` 或创建新 Screen；当前 Screen 在同一帧切换面板、widget 可见性和 slot active/坐标。高级输入首槽为 `(21,bottom=164)`、输出首槽为 `(119,bottom=164)`；包裹输入首槽为 `(24,bottom=164)`、marker 为 `(109,bottom=164)`、自动输出物品原点为 `(112,bottom=140)`。高级列头颜色/清空/循环按钮使用 `bottom=174`，列操作按钮使用 `bottom=173`，全部结束在输入框 `y=80` 顶边之前；不得沿用旧布局的 172/171。高级槽不得使用纯色填充覆盖用户底图；动态启用的输入列按 AE2 v19 `Icon.SLOT_BACKGROUND [192,192,18,18]` 在 `(slot.x-1,slot.y-1)` 绘制完整槽位精灵，未启用列不额外绘制并保留底图原像素，输出槽直接使用底图已有槽位材质。两页编辑槽库存完全隔离，非当前页槽移到屏幕外，切换不复制、迁移或清空内容。共同的空白样板、已编码样板和 Encode 分别为 `(150,bottom=165)`、`(150,bottom=118)`、`(150,bottom=145)`。
右侧两个模式按钮逐项对应 AE2 v19 Pattern Encoding Terminal 的 `TabButton.Style.HORIZONTAL`：位于 `left=173`，使用 22x22 normal/selected/focus 背景，相邻标签使用 21px 步进；包裹按钮在上并固定于网络行结束后 6px，高级按钮在下。按钮不得渲染 ItemStack；高级页图标取共享 `ae2-states.png [16,32,16,16]` 的 processing/furnace sprite，包裹页图标取用户 `advanced_pattern_encoding_terminal_sprites.png [32,0,16,16]`，两者都按 horizontal tab 的 `(3,2)` sprite 原点绘制。包裹页显示 3x3 输入窗口并用小滚动条覆盖 27 行，marker 与自动输出使用包裹页专属槽；高级页显示四个可见列和三行输入/输出；无显示元件面板。
点击颜色按钮只打开统一拾色弹窗。弹层打开时当前页输入、marker 和输出槽保持 active 并继续正常绘制物品，但父 Screen 调用 `super.render(...)` 时必须传入屏外鼠标坐标，使被遮挡 slot 不产生 hover、高亮或 tooltip；鼠标点击/释放/拖拽/滚轮、键盘和字符输入由弹层拦截，点击外部只关闭弹层且不透传；主面板按钮保持可见但暂时停用。弹层通过前景 Z 层遮挡与其重叠的 slot/item，不通过隐藏物品制造遮挡。
包裹样板模式编码时输出独立 `appliedpackaging:package_pattern` 物品，并写入 appliedpackaging.package_crafting_pattern NBT；tooltip、AE2 pattern decoder、Pattern Provider 和 Crafting CPU 通过该 NBT 识别输出包裹，装配室之外的机器不会把它当作可执行的分子装配室 crafting pattern。
包裹页按每行 3 格显示连续 3 行，滚动范围覆盖 27 行；中键数量编辑与高级页统一打开 `AdvancedSetPatternAmountScreen`，复用 AE2 `set_processing_pattern_amount.json`、数值范围和 `InventoryAction.SET_FILTER` 回写语义。输入保留 AE2 `ConfigInventory`/`GenericStack` sparse 数据与数量显示，允许 AEItemKey、AEFluidKey 等 AE processing input 类型。自动包裹预览不是 processing primary output，空槽不注册 `primary_processing_result_tooltip`；存在包裹时只显示物品自身 tooltip。
高级样板终端注册为 AE2 cable part item，part 继承 AE2 PatternEncodingTerminalPart，复用网络终端库存、搜索栏与 AE 左侧工具栏，但不创建右侧 view-cell 区域。两行网络库存时主体宽 195px、高 245px，顶部网络库存为 9 列，标题、搜索、网络滚动条、玩家栏与样板编码区按用户新版底图布局。动态加高时首行、可重复中间行和末行分别使用固定网络切片，避免接缝。高级页中间编码区为 4 个可见包裹列，每列拥有 81 个输入槽但只同时显示 3 行，列之间保留 1px 间距；右侧输出同样只显示 3 行。左侧使用 AE2 高版本小滚动 indicator 同步滚动输入/输出，底部水平滚动条只滚动包裹列；第一未启用列显示加号，后续列显示禁用背景且无颜色按钮/无 ghost 物品。右侧 `BLANK_PATTERN` 与 `ENCODED_PATTERN` 逻辑槽分别使用 `left=150,bottom=165` 和 `left=150,bottom=118`；编码按钮 widget 使用 `left=150,bottom=145`，三者共用中心线。高级与包裹输入 fake slot 均允许修改数量。

高级终端覆盖 1.20.1 AE2 的白色槽位 hover。旧 `renderCustomSlotHighlight` 注入只取消旧效果，不在 Vanilla 尚未结束的槽位批次中追加绘制；在 AE tooltip 阶段前先提交背景纹理批次，再按 AE2 1.21.1 使用 `0x669cd3ff` 淡蓝填充和 `0xffdaffff` 四边高亮，随后提交高亮批次并继续 tooltip。该顺序同时适用于空槽和有物品槽，避免旧版延迟 blit 出现黑色矩形。
每个启用列上方包含颜色按钮和 X 按钮。颜色按钮只打开统一拾色弹窗，不提供名称或 marker 编辑；X 在列非空时清空该列，在列为空时删除该列并让后续输入与颜色前移，最后一列不会删除。弹窗在 `super.render(...)` 完成后作为不透明高 Z 前景绘制；可见 processing input/output slots 保持 active，物品继续在弹窗下方渲染。打开时父 Screen 以屏外鼠标坐标完成底层渲染，因此底层槽既不产生 hover/高亮也不产生 tooltip；按钮保持可见但停用，鼠标点击/释放/拖拽/滚轮、键盘与字符输入均不会透传到底层 AE 网络库存、processing slots 或编码按钮，点击外部只关闭弹层。

统一 `PackageColorPicker` 是所有包裹颜色入口的唯一弹窗与触发按钮实现：无标题、无名称或 marker 控件，调用方显式传入 `allowNone`。左侧分隔组固定为两格竖排：Fluix 在上、None 在下；`allowNone=false` 时不绘制也不命中 None，但保留整格空间，因此弹窗宽高、分隔线和右侧颜色位置不移动。其余 16 个染料色固定在右侧按 8x2 排列。只有 Package Storage/Unpacking Bus 过滤行传入 `allowNone=true`，且其触发按钮右键直接清除颜色条件为 None；ME Packager、ME Package Assembler 和 Advanced Pattern Terminal 两页均传入 false。装配室触发按钮可用 None 图标只表示高级样板混色，不允许把 None 写为机器选择。默认、None、选中背景分别读取 `package-storagebus-sprites.png` 的 `(48,0,8,8)`、`(56,0,8,8)`、`(48,8,8,8)`；选中态只替换色格内部背景，不在格外增加 outline，hover 只用于颜色名称 tooltip，不改变像素。颜色格由弹窗单次手工绘制而不注册为普通 widget，只有弹窗自身绘制一次颜色名称 tooltip；父 Screen 先完整绘制槽位、物品和普通 widget，再取消底层 tooltip。打开时 picker 同时暂停锚点触发按钮自身的 hover tooltip，关闭后恢复，避免弹窗与“选择包裹颜色”提示重叠。Advanced Pattern Terminal 与其它父 Screen 一样在自身 `super.render(...)` 之后绘制 picker，不使用原 AE Screen 的 Render.Post 事件。打开 picker 不改变底层 slot active 状态，但 picker 必须吞掉 mouse click/release/drag/scroll、keyPressed 与 charTyped。
高级终端编码结果是独立 `advanced_processing_pattern` 物品，并写入 AE2 processing in/out 与 appliedpackaging.advanced_processing_pattern 列数据；默认 AE2 Pattern Encoding Terminal 和其它普通编码路径只输出原版样板，不写高级列 NBT。
高级终端读取普通 processing pattern 时装入第一列；读取高级 processing pattern 时恢复列数、颜色、颜色模式和最多 81×81 个 sparse inputs。编码时名称固定为空，每列 marker 固定取主产物并归一为 1 个 AEItemKey。新增列发生在当前水平窗口末尾时，客户端等待服务端 activeColumns 回传后自动滚到新的末尾。
高级页的“转置配方”把活动矩阵 `(column,row)` 写到 `(row,column)`，新列数为原矩阵最高非空行加一，原活动列数不超过 81 因而可无损进入每列 81 行；输出不变，仍对应既有数据的列保留原颜色，仅对转置产生的新增列应用当前颜色模式。颜色模式是左侧功能栏中排在 AE2 原生功能按钮之后的默认/循环切换，而不是列头小色块，也不修改既有列：默认模式始终给新增列分配 Fluix；循环模式先选前面活动列尚未使用的第一种 `PackageColor`，17 色全被使用后从最后一列颜色的下一种继续循环。手动新增列和 JEI/EMI 配方填充新增列使用同一分配器。左侧工具栏在 1.20.1 只绘制 normal/hover 背景，不把鼠标点击后残留的 widget focus 绘制为持续外框。空手按住已占用输入格至少 350ms 后绘制随鼠标移动的拾起图标，松手于另一活动输入格时服务端执行移动；目标非空则交换。短按仍委托原 FakeSlot PICKUP。
默认初始颜色模式为 DEFAULT；现有列颜色只由逐列编辑或其创建时的颜色模式决定。
当前正式载体只包含 package_pattern、普通 AE2 processing_pattern 与 advanced_processing_pattern；不含任意 AEKey 处理输出 ghost editor。
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
作为默认优先级 0 的 Formation Plane 式只写入端点接收网络路由包裹
模拟过滤、目标完全为空的阻挡条件和完整相邻目标容量
把接受的一个包裹直接保存在本地 heldPackage，并把这个未拆完的整包作为数量 1 的可抽取网络库存报告
播放与 ME 打包机拆包模式相同的工作进度
进度结束时重新验证过滤、阻挡与目标容量
成功时按 Pattern Provider 的 check-then-push 约定，模拟阶段按精确 AEKey 汇总重复条目，提交阶段仍按 contents 条目顺序逐条推入全部散装内容并清空 heldPackage
失败时保留同一个 heldPackage，进入阻塞并定时重试
```

目标实现：

```text
Package Storage Bus 与 Package Unpacking Bus 的玩家入口和运行时实现均为 AE2 `PartItem`，安装在 cable bus 面上并各自要求一个 channel。Package Storage Bus 的安装面读取相邻 Forge item handler；Package Unpacking Bus 的安装面优先解析序列缓存器端点，否则通过与 AE2 Pattern Provider 相同的外部存储扩展链建立通用目标。旧的三个 Package Bus 方块/方块实体及其资源已删除，不再作为并行兼容实现注册。
Package Export Bus 与独立 Package Pattern Terminal 已从正式范围删除，不保留玩家注册、配方、创造栏入口或客户端界面。

package_storage_bus:
  作为 IStorageProvider 挂载 PackageItemStorage。
  默认优先级为 0。
  保持普通 MEStorage 的非 preferred 语义；与 Package Unpacking Bus 数值相同时只在拆包端点拒绝后作为回退目标。
  只枚举带 PackageData 的合法包裹。
  insert/extract 均拒绝散装物品和无 PackageData 的包裹。
  设置过滤模板后，只暴露、插入、抽取匹配过滤的包裹。
  SIMULATE 插入使用保留真实 slot limit 与 isItemValid 规则的累计库存快照，多个包裹不能重复占用同一份空余容量。
  服务端每 10 tick 请求重新挂载 IStorageProvider；相邻 handler 中包裹增删、目标方块移除/替换或运行中修改过滤器后，AE storage cache 会刷新可见 key，并卸载旧目标留下的 key。
  Partition Storage 按相邻 item handler 槽位顺序读取不同的合法包裹，每个样本写入一个已启用过滤行并跳过散装物品与重复包裹；目标存在但没有合法包裹时清空过滤。颜色与 marker 总是从样本写入；只有样本内容全部为物品且类型不超过每行 6 槽时才完整写入内容 allowlist，包含非物品 key 或超过 6 种时保留颜色/marker 规则，不能生成一个反而拒绝原样本的不完整 allowlist。
  不暴露包裹内部内容。

package_unpacking_bus:
  自身注册 IStorageProvider，挂载使用一个只保存唯一 held 工作包裹的 MEStorage；与 package_storage_bus 一样默认优先级为 0，挂载时直接使用玩家在右上角 Priority 子菜单配置的数值，不增加隐藏偏移。
  对合法包裹返回 `isPreferredStorageFor=true`；因此与 package_storage_bus 数值相同时先尝试拆包端点，只有拆包端点因 held 忙碌、过滤、阻挡或目标容量拒绝时才继续路由到存储总线。
  该挂载不周期扫描或主动抽取 Drive、Storage Bus、Cell 等其它网络存储；仅当本机 `heldPackage` 非空时把它按精确 `AEItemKey`、数量 1 加入 available stack，并允许网络模拟或提交抽取同一个整包。空闲时不报告容量，不接收无法立即进入拆包流程的包裹，因此不具备一般存储功能。
  网络 insert 每次最多接受 1 个合法包裹；只有空闲、过滤匹配、相邻通用目标存在、整包模拟通过且阻挡条件满足时，SIMULATE 才返回 1。MODULATE 把同一包裹直接转移到 part 自己的 `heldPackage` 并开始工作，此时目标仍不获得任何内容。
  `heldPackage`、工作态、剩余工作 tick、阻塞态和重试冷却写入 part NBT；工作槽直接同步该真实 held 状态，不再使用空预览占位。held 从空到非空或从非空到空时请求重新挂载 IStorageProvider，使 ME 可见包裹和阻挡检查及时更新。
  阻挡模式默认关闭。开启时要求通用目标的全部可见 AEKey 为空；任意物品、流体或附属模组 key 已存在都拒绝首次接收或最终提交，不能只比较本包裹的输入类型。序列缓存器端点同样要求全部成员为空，即使已有内容位于本包裹不会使用的位置也必须阻挡。
  实际工作周期复用 ME 打包机的拆包速度表，0-4 张加速卡分别为 20/15/10/6/4 tick，最多识别 4 张且最快 4 tick。开始工作时锁定本次周期总长并与剩余 tick 一起持久化；菜单进度按该总长缩放为 15 级。进度结束时重新校验当前过滤规则、阻挡条件和目标存在性；模拟阶段按精确 AEKey 汇总重复条目并要求目标完整接收每个汇总量，全部通过后才按原始 contents 顺序逐项真实插入并清空 held 状态。
  普通目标通过 `PackageUnpackingTarget` 按 AE2 Pattern Provider 的同一扩展链解析：先读取 AE2 `Capabilities.STORAGE`，否则由 `StackWorldBehaviors` 构造所有已注册 `ExternalStorageStrategy`，按 `AEKeyType` 包装并组成 `CompositeStorage`。该目标直接保留统一 `MEStorage`，使阻挡判定能够检查完整可见库存，而不受 `PatternProviderTarget.containsPatternInput` 只能匹配指定输入集合的语义限制。实现不枚举固定 handler 类型；AE2 默认注册物品与流体策略，附属模组注册的其它 AEKey 类型自动沿用同一路径。
  防堵塞模式使用独立 boolean 与 Part NBT key，默认开启。开启时网络插入的 SIMULATE/MODULATE 都必须调用与自动拆包相同的 `canUnpackIntoTarget`，因此现有阻挡模式、序列缓存器原子计划、目标类型和整包容量任一失败都拒绝接收。关闭时通过包裹合法性与过滤后即可写入唯一 held 槽；预检失败时设置阻塞但不开始工作，定时重试通过后才进入完整进度。等待、工作和最终失败状态都继续枚举同一真实包裹并允许网络/GUI 取回。
  最终模拟失败时不写入任何内容、不把包裹写回 ME 网络，也不切换到其它包裹；保持同一个 `heldPackage`，清空进度并进入阻塞态，按同一加速卡公式等待下一次重新验证，随后从新的完整加速周期重新开始。第三方目标必须遵守其 AE2 外部存储策略的 simulate/execute 一致性约定；本 Mod 不再构造逐槽计划或反向抽取回滚。
  工作、阻塞或其它 held 状态都允许网络按精确 key 抽取，也允许玩家从 GUI 工作槽取回。SIMULATE 不改变状态；MODULATE 或玩家真实取回时返回完整的一个包裹并原子清空 working、blocked、进度、周期总长和 retry cooldown，目标不得获得任何部分内容。part 被拆除时 held 包裹作为额外掉落返还，`clearContent` 同步清空，避免复制或丢失。
  不接受部分拆包。

两个总线共用 7 行 `PackageBusFilterRule`。每行保存颜色是否启用及颜色值、marker item key、6 个普通 item key、模糊开关和反转开关；颜色未启用是可见的空模式，表示不按颜色过滤，而不是 Fluix。空行不参与匹配，所有非空行按 OR 合并。内容过滤在反转关闭时要求包裹内容均属于该行白名单，在反转开启时要求包裹内容不属于该行列表；模糊开启时使用 AE2 `FuzzyMode` 比较物品 key。
基础启用第 0、1 行；它们在 GUI 中对应底图最上方 `y=29`、`y=47` 两行。5 张容量卡依次启用第 2、3、4、5、6 行，第 5 张达到 7 行过滤容量上限。移除容量卡时被锁定行必须清空，菜单使用 `OptionalFakeSlot`/`IOptionalSlot` 提供禁用状态，并按 AE2 新版风格对正常 slot background 使用透明叠加，不制作独立 disabled-slot 贴图。
模糊卡与反转卡各最多 1 张。对应按钮只在卡存在时显示；两者都存在时顺序为模糊、反转、颜色，只存在一个时该按钮紧靠颜色。模糊、反转、颜色三个 8px 按钮在每个 18px 过滤行内统一使用固定 2px 上边距，不按行高垂直居中。按钮分别保存每一行状态，绿色表示启用、红色表示停用。颜色按钮始终存在；颜色为空时绘制无色标记，统一拾色弹窗提供“任意颜色”空项，右键颜色按钮也可清除该行颜色约束。
两个界面保留用户提供的 `package-storagebus.png` 与 `package-storagebus-sprites.png` 为两张独立、字节不变的运行时纹理；工作槽、空进度框与活动进度 sprite 分别从背景文件 `[176,0,18,18]`、`[196,0,6,18]`、`[176,32,6,18]` 取样到 `(119,8)` / `(139,8)`，活动 sprite 按 15 级进度从底部裁切原像素，不缩放、不重新着色，Package Storage Bus 不提交该工作区。过滤槽和模糊/反转按钮来自用户 sprite，其中锁定行使用 `SLOT_BACKGROUND` 的 `0.2` alpha；同一用户 sprite 的 `(32,16,16,16)` 是自绘 marker 空槽图标，空 marker 过滤槽按所在行透明度绘制，已解锁槽 hover 时显示双行说明 tooltip。新版 `states.png`、`extra_panels.png` 与 `vertical_buttons_bg.png` 作为三张未修改的 LGPL 资源独立加载，分别承担新版 toolbar 按钮/优先级标签、升级与工具箱面板、竖向按钮组外框；不向用户原图烘入任何 AE2 像素。AE2 当前 main 的 `Blitter` 为每个元素提交独立 `TextureSetup`/ARGB render state；1.20.1 回移使用立即式 `Blitter`，在 ScreenStyle 背景与自定义层之间 `GuiGraphics.flush()` 并复位混合/颜色状态，达到等价的跨纹理隔离。两者均在右上角 `(152,-5)` 放置 `20x20` 新版优先级标签并打开 AE2 Priority 子菜单。Package Storage Bus 左侧工具栏包含 Storage Bus Help、清除、Partition Storage 和四个存储设置按钮；Package Unpacking Bus 左侧固定为 Pattern Provider Help、清除和阻挡模式。两者均使用新版 toolbar normal/hover/focus 背景、新版状态图标及 6px 间距，并保留连接目标提示。右侧升级面板相对主界面使用 current-main 的 `top=0` 与 5px padding；旧依赖的灰色空升级槽图标被禁用，空槽改绘 `ae2-states.png` 的 current-main `BACKGROUND_UPGRADE` `(240,208,16,16)`。两个 part 都只提供 5 格共享升级库存，并允许这 5 格全部装入 capacity card。卸货总线在同一库存中额外接受最多 4 张 speed card，不把 fuzzy/inverter/capacity/speed 的兼容上限相加成更多物理槽。tooltip 阶段绘制 hover 时必须把菜单内相对槽坐标转换为 `leftPos/topPos` 窗口坐标，防止高亮落到屏幕左上角。
右上角 Priority 子菜单直接读写两个 part 的 `IPriorityHost` 数值，不存在另一套隐藏用户优先级；两个 part 的默认值都为 0。AE2 `NetworkStorage` 先按数值从高到低分组，再在同一数值内先调用 `isPreferredStorageFor`；Package Unpacking Bus 的受限输入端点对合法包裹进入该首轮，Package Storage Bus 保持普通第二轮存储，所以同值时稳定先拆包，拆包拒绝后仍可落到存储总线。过滤器或优先级变化时两个 IStorageProvider 都请求重新挂载；Package Unpacking Bus 在网络接收、最终提交和阻塞重试时都使用当前规则。过滤变化不会把 held 包裹替换成另一个包裹；不再匹配时保持阻塞，网络或玩家可以取回。两者均以整包为操作边界；卸货总线只暴露唯一 held 包裹本身，不能部分暴露或主动部分拆出包裹内容。
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

## 12. 序列缓存器

### 12.1 方块状态与拓扑

`SequenceBufferBlock` 使用一个五值状态属性表达模型族：

```text
unformed              未成型、无方向
unformed_directed     未成型、有六向 facing
endpoint              已成型无存储端点，sequence_direction 指向逻辑第 1 格
member                已成型普通成员、无输出方向
member_directed       已成型普通成员，facing 是输出方向
```

方块另以 `directional` 保存自身方向是否启用，`facing` 支持六个方向；已成型状态保存 X/Y/Z `axis`，并以独立 `sequence_direction` 表示端点到尾部的结构方向。成员还保存仅用于模型选择的 `tail`，每次拓扑重建只允许最后一个成员为 `tail=true`，尾部扩展时旧尾自动改为中间模型。成型、扩展、重建和解体只能修改 `state/axis/sequence_direction/tail/controllerPos`，不得清空或旋转 `directional/facing`。当已有方向与结构轴平行时，成员使用无可见方向的连接模型，但保留内部方向；脱离结构后重新显示原方向。未成型方块对同一侧连续使用扳手时，方向严格循环为 `clickedSide.getOpposite()`、`clickedSide`、`unformed`；如果第一或第二段已经形成结构，下一次点击先解散该端点控制的结构，再把原端点推进到下一段。成员只接受垂直于结构轴的新输出侧，并使用相同的“对面方向、点击面方向、无方向”循环；换到与当前方向无关的另一合法侧时，从该新点击面的对面方向重新开始循环。

每个方块实体保存 `controllerPos`、`sequenceDirection` 和配置副本。端点满足 `controllerPos == worldPosition`；成员按 `controllerPos + sequenceDirection * index` 排序。成型扫描只读取已加载区块，最少 2 格，达到服务端 `maxSequenceBufferLength` 后停止；默认 128，允许配置到 2048。候选连续线中出现已成型方块、第二个端点、未加载位置或超过上限时不提交部分结构。新放置方块只在当前尾端后一格自动加入；端点背面和成员侧面不吸附。断裂后端点侧连续片段长度仍至少为 2 时保留；不足 2 或没有端点时全部转为对应的未成型状态。脱离成员保留缓存内容和最后配置副本，不丢弃 AEKey。

### 12.2 单格缓存与持久化

单格状态：

```text
storedKey: optional AEKey
storedAmount: long, 0..configuredCapacity
releaseAtGameTime: long
admissionOpenAtGameTime: long
```

一次 `Actionable.MODULATE` / 非 simulate item insertion 最多接收 `min(requested, configuredCapacity)`；首次实际接收后，内容非空期间所有 insert 都返回 0，包括相同 key。`SIMULATE` 只返回当前可接收量，不写 key、数量或延迟。抽取可以部分执行；数量在 game time `t` 从正数归零时清空 key，并把本格 `admissionOpenAtGameTime` 更新为至少 `t + 1`。所有输入路径在查询时直接比较当前 game time：`current < admissionOpenAtGameTime` 时拒绝，达到该时间且内容仍为空时开放，不依赖本格或端点的 server tick 阶段。该绝对时间写入 NBT，保证清空后同 tick 卸载/载入也不会提前重入。NBT 通过 `GenericStack.writeTag` / `readTag` 保存 key 与数量；无效或非正数载入值视为空。若管理员在已有内容后降低容量，超过新容量的旧值保留并记录警告，只允许继续抽取而不截断或复制资源；缓存清空后的下一 game tick 使用新容量。

已成型端点不保存 `storedKey/storedAmount`，成型时若候选端点已有内容则拒绝成型，避免隐式搬运或删除。端点指向的下一个方块才是逻辑第 1 格；结构物理长度至少 2，即一个端点加至少一个存储成员。

服务端配置 `sequenceBufferCapacity` 默认 1024，范围 1..Long.MAX_VALUE。过滤器是精确 `Set<AEKey>` allowlist；空集合表示允许全部。第一版配置包含：

```text
autoOutput = true
blockingMode = false
synchronizedOutput = false
patternMode = false
inputDelayTicks = 1
allowedInputs = empty (allow all)
```

配置修改只在服务端进行。端点修改后逐成员复制并 `setChanged`；加入成员先复制端点配置。普通成员本地配置不反向覆盖端点。解散后各单块继续使用最后副本。

### 12.3 Capability 视图

每个方块暴露 AE2 `Capabilities.STORAGE`。未成型和普通成员的视图只操作本格；端点自身没有本地槽，插入按逻辑第 1 格到尾部选择目标成员，抽取与 `getAvailableStacks` 只合并全部存储成员。端点同时暴露 `ICraftingMachine`。所有 capability 在移除时 invalidate、恢复时重新创建；视图在每次调用时动态解析当前端点与成员顺序，因此拓扑变化不保留旧成员快照。

Forge `IItemHandler` 只映射 `AEItemKey`。单格视图固定为一个槽；端点合并视图的每个槽对应一个稳定顺序的存储成员，不包含端点。插入端点任意公开槽仍走结构顺序分配；抽取槽按成员索引定位。非物品 key 不出现在 item handler 中，也不会被 item handler 抽取或删除。非端点成员仍允许直接输入/抽取自己的单格。

Forge `IFluidHandler` 只映射 `AEFluidKey`。单格视图固定为一个 tank；端点合并视图的每个 tank 对应一个存储成员，不包含端点。`fill` 按结构顺序分配到目标成员，`drain(FluidStack)` 按成员顺序抽取同类流体，`drain(maxDrain)` 从第一个可抽取流体成员开始；非流体 key 对该 capability 不可见。流体 amount 使用 Forge `int` 边界适配内部 long 容量，模拟和实际执行必须保持同样的锁存语义。

### 12.4 输出、阻挡、同步与延迟

成型结构的实际输入发生在 game time `t` 时，把端点的全结构屏障更新为 `max(existing, t + max(0,inputDelayTicks))`。当前时间早于屏障时，主动自动输出和外部 capability 被动抽取都返回 0，但仍允许继续向其它已开放空成员输入。未成型单块不应用输入延迟，输入后可立即被 capability 抽取。玩家在主/侧 GUI 中从真实物品缓存槽手动抽取走专用路径，不检查该输出屏障，也不检查阻挡或同步输出设置；若因此取空，仍按正常清空路径记录 `admissionOpenAtGameTime=t+1`。多方块延迟为 0 时允许当前 tick 自动输出；成员即使随后被清空，也不会在该 tick 再次接收。

未成型单块由自身方块实体 tick 执行自动输出。结构成型后，成员方块实体的 server tick 不执行任何操作；普通模式由唯一端点依次代理每个成员的自动输出，同步模式仍由端点构造全结构计划。输入重新开放不属于 tick 维护工作，而由每个成员在输入预检时按绝对 game time 判定。由此同一结构的输出只有一个端点 tick 时相，输入开放又不依赖端点、成员、包裹装配室或拆包总线之间的方块实体 tick 顺序。

无方向单块/成员按固定 `DOWN, UP, NORTH, SOUTH, WEST, EAST` 顺序寻找除序列缓存器外的兼容目标；有方向状态只检查 `facing`。先尝试目标 `MEStorage`，没有时按 key 类型尝试 Forge item handler 或 fluid handler。阻挡模式使用目标完整可见内容为空作为门禁，不只检查同类 key。普通模式允许目标部分接收并保留余量；端点不参与自动输出且没有可输出内容。

防堵塞模式属于端点持久化配置，默认关闭；成型成员运行时读取端点当前值，不复制到成员本地配置。开启时普通 item/fluid/ME 插入先按单格容量计算本次实际接收量，再用该成员真实 `findTransferTarget(key, accepted, requireFull=true)` 预检；方向、MEStorage 优先级、目标 key 类型、现有阻挡模式和累计容量均与自动输出一致。端点顺序输入遇到不能完整输出的空成员时继续尝试后续空成员。样板或包裹的多格 `applyInputPlan` 必须逐成员完成同一完整预检，任一失败时整批返回 false。关闭时保持原有“先锁存、等待自动输出”语义。输入延迟、红石门禁和自动输出开关属于调度条件，不参与防堵塞的目标可接收性预检，否则默认输入延迟会使任何新输入永远无法通过；GUI 抽取继续不受影响。

同步模式收集所有非端点、非空、已到释放时间且启用自动输出的成员。每个成员必须找到目标，并对其完整 amount 通过阻挡和累计容量模拟；任一失败则本 tick 不提交任何成员。全部通过后按成员顺序执行真实插入并扣除实际成功量。第三方 capability 若在模拟后拒绝真实提交，不实现跨第三方 handler 回滚；未提交余量继续保留并在下一 tick 重新规划。

### 12.5 Pattern Provider push

端点 `acceptsPlans` 只在结构有效且存在可分配成员时为真。`pushPattern` 对传入 `KeyCounter[]` 建副本并构造 `SequenceBufferPatternPlan`，不得在规划阶段修改原 holder。

普通模式对可识别的普通样板仍按 sparse 原槽位映射，每个样板位置对应一个存储成员，空位置不前移后续输入。样板模式开启时使用同一映射，但未知 pattern details 无法恢复位置就严格拒绝；关闭时未知 details 才回退到其公开输入顺序。可识别布局来源：

```text
AECraftingPattern.getSparseInputs()             9 格
AEProcessingPattern.getSparseInputs()          81 格
PackageCraftingPatternDetails.sparseInputs()   81 格
其它 IPatternDetails                           关闭样板模式时使用 getInputs() 执行顺序
```

`AdvancedProcessingPatternDetails` 是明确例外：无论样板模式开关如何，都忽略高级列矩阵的 sparse 空位，只把公开的实际输入按普通非空顺序映射到连续成员。其它已知 sparse 布局保留成员间隔，最高非空索引必须小于存储成员数量。第一版对可识别 sparse 样板要求 `KeyCounter` 中的 key 和数量与样板位置精确对应，不在序列缓存器内执行替代物选择；高级样板则按其公开的实际非空输入精确匹配。重复模板位置分别生成独立成员分配，不合并。全部 holder 内容必须恰好被计划消费；结构过短、目标成员已锁存、过滤拒绝、单格超容量、位置不匹配或存在剩余输入时整批返回 false。提交时先写入全部成员并设置全结构延迟，再按计划从原 `KeyCounter[]` 扣除；服务器主线程内成员状态再次校验失败时不得消费输入。

### 12.6 拆包总线与包裹位置布局

`PackageData.layout` 缺省时，Package Unpacking Bus 按 contents 有序列表把每个条目映射到连续存储成员。布局存在且端点开启样板模式时，第 `contentSlots[i]` 格只接收 `contents[i]`，未列出的槽位保持空白；端点不计入位置。端点关闭样板模式时即使包裹带布局也按 contents 连续输入，不跳过空位置。拆包总线直接调用同 Mod 的端点原子计划入口，而不是把布局降级为普通 `IItemHandler` 插入。预检同时覆盖结构长度、成员内容锁存、`admissionOpenAtGameTime`、AEKey filter 和单格容量；输入延迟只阻挡输出，不阻挡向其它已开放空成员继续输入。任一失败时 held 包裹保持原样，目标成员也不得出现部分内容。

### 12.7 双界面菜单、槽映射与升级权威

序列缓存器使用两个独立菜单类型，语义对应高版本 AE2 ME Chest 的 main/side 两个入口：

```text
SequenceBufferMainMenu  仅由成型端点打开；host=viewed=端点
SequenceBufferSideMenu  由普通成员或未成型单块打开；host=解析后的端点权威或自身，viewed=被点击方块
```

主菜单固定创建 27 个显示槽，按 `memberIndex = scrollRow * 9 + visibleIndex` 映射逻辑成员，不包含端点。`memberCount <= 27` 时 `maxScrollOffset=0`；否则 `maxScrollOffset=max(0,ceil(memberCount/9)-3)`。最后一个可视页仍按整行移动，因此前两行可以与上一页重叠，只有 `memberIndex >= memberCount` 的位置禁用。Screen 对有效位置绘制完整 `SLOT_BACKGROUND`，对不足 3x9 和末行越界位置以 0.2 alpha 绘制同一精灵；禁用位置不响应 hover、放入、取出或快捷移动。侧面菜单只创建一个显示槽，服务端始终映射 `viewed` 本格，升级库存来自端点 `host`；两套菜单都不创建过滤假槽。只有端点主菜单允许配置 action，侧面菜单在客户端与服务端都拒绝配置修改；成型成员侧面菜单额外提供跳转端点主菜单的 action，未成型单块不提供跳转。

显示槽通过 `GenericStack.wrapInItemStack` 同步 `AEKey + long amount`，所以物品、流体和其它 AEKey 使用 AE2 的通用图标与数量渲染。普通光标只允许对 `AEItemKey` 执行真实放入/取出；放入调用本格一次输入锁存路径，取出调用仅供菜单使用的真实缓存抽取路径并绕过输出延迟、阻挡与同步输出门禁。主菜单从玩家物品栏快捷放入时从逻辑第 1 格开始选择首个已开放空成员，侧面菜单只尝试被点击成员；从显示槽快捷取出先模拟玩家物品栏容量，再按实际移动量提交，最后一份被取走时照常阻止同 tick 重入。非物品通用 key 只显示，不伪装成普通 ItemStack 搬运，仍通过 `IFluidHandler` / `MEStorage` 操作。

每个方块实体保存 9 格 `GenericStackInv.Mode.CONFIG_TYPES` 过滤库存的可重建镜像，持久化权威仍是 `SequenceBufferConfiguration.allowedInputs`。该库存只作为后续 GUI 接口的预留数据结构：内部或后续接口写入时重建精确 AEKey allowlist，再调用端点 `updateConfiguration` 同步成员；外部配置或 NBT 载入则在抑制 change callback 的批处理中反向重建库存镜像。每个方块实体另保存 1 格 `IUpgradeInventory`，菜单对成型结构只打开端点库存；`Upgrades` 注册只允许一张红石卡。形成结构前先验证全部方块实体存在，再把未来成员已有的物理升级卡移入端点；端点槽已占用产生的重复卡在原成员位置掉落，不允许形成隐藏且不可访问的成员升级库存。未安装卡时自动输出忽略红石，安装后所有成员每 tick 解析端点并以端点 `hasNeighborSignal` 作为整组自动输出门禁；该门禁不绕过锁存、阻挡、同步或延迟规则，也不改变 capability 被动抽取语义。拆除方块时其本地升级卡与本格存储内容一起掉落。

第一版 main/side 菜单不调用 `addExpandableConfigSlots`，两套 ScreenStyle 也不声明 `CONFIG` 槽或过滤背景；允许输入的精确 AEKey 过滤库存继续由端点 `SequenceBufferConfiguration` 和 `inputFilter` 保存，但不显示独立 3x3 面板。仅当菜单声明 `configurationEditable=true` 时，`AbstractSequenceBufferScreen` 才使用项目现有 AE2 竖向按钮栏创建自动输出、阻挡、防堵塞、同步输出、样板模式和输入延迟按钮；延迟按钮以 `0/1/5/10/20/40/100 tick` 循环并读取 AEBaseScreen 的右键方向。服务端再次检查该权限，防止隐藏按钮后仍可伪造 action。成员/单块侧面 Screen 不创建这些按钮；成型成员只创建 `Icon.ENTER` 跳转按钮，`ModernVerticalToolbar` 从 current-AE2 `ae2-states.png [112,0]` 绘制图标，单块时按钮隐藏。端点更新只写端点自身 `SequenceBufferConfiguration`；成员运行路径通过 `effectiveConfiguration()` 实时解析端点，成型/更新均不复制成员 NBT。红石卡升级面板使用 `{right:2,top:0}` 附着主面板右侧。主界面滚动条使用项目缓存 current-AE2 `ModernScrollbarStyles.BIG` 的标准 12x15 handle，组件起点 `(175,18)` 使其相对底图 `x=178..183` 的窄轨道对称居中；范围为 0 时仍绘制 current-AE2 disabled handle，不得隐藏。

## 13. JEI / EMI 双前端配方导入

### 13.1 计划与传输

`AdvancedPatternTransferPlan` 与 `PackagePatternTransferPlan` 是配方查看器和菜单状态之间的依赖中立模型。高级计划的 `columns` 最多 81 项，每列最多 81 个 sparse 位置；非空位置保存 `GenericStack`，前导和内部空位保存 `null`，末尾空位规范化裁掉，`outputs` 最多 4 项。包裹计划最多 81 个有序非空输入和一个可空 item marker。计划不保存第三方 recipe object、JEI/EMI ingredient type 或运行时 capability。网络 payload 使用 JSON，保存每个非空位置的 `GenericStack.writeTag()` SNBT、空位 `null` 和列边界，序列化长度不得超过 AE2 client action 的 32767 字符上限。

`AdvancedPatternEncodingTermMenu.importAdvancedRecipe` / `importPackageRecipe` 只在客户端发送对应 action；服务端 action handler 解码出完整计划后才调用当前状态的 `replaceRecipe`。高级替换先清空旧输入/输出、写入新列和输出、保留重叠列颜色、给新增列设 Fluix，并切到 ADVANCED。包裹替换清空旧内容和 marker、写入新输入/marker、保留原包裹颜色，并切到 PACKAGE。两者都在完整验证后只触发一次状态变化并广播，另一页不参与替换。

### 13.2 JEI 标准槽位回退

未被专用适配器认领的 recipe 使用 `IRecipeSlotsView`：

```text
INPUT       -> 消耗资源；每槽先选终端当前网络/玩家库存中最高优先级的可表示候选，无匹配再用 JEI 当前显示项
OUTPUT      -> 结果；该槽全部候选转换后必须只有一个不同 GenericStack
CATALYST    -> 跳过
RENDER_ONLY -> 跳过
其它 ingredient type -> 拒绝
```

高级页按槽位顺序把每个确定性 item/fluid 输入分别写入一个列，要求至少一个确定性输出；包裹页仍把所有输入保持顺序写入单个 contents，并使用第一个 item 输出作为 marker。通用层不会把多个 output 候选擅自选成当前动画帧，也不会把无输出燃料/世界生成信息伪装为加工配方。Thermal Series 的部分 JEI 分类把可选 catalyst 标为 INPUT，因此先从 recipe 的 `getInputItems` / `getInputFluids` 取得实际消耗数量，再分别保留前 N 个 item/fluid 输入。

`RecipeIngredientSelector` 在 JEI transfer 的可行性检查与实际导入时都从 `AdvancedPatternEncodingTermMenu.getClientRepo()` 读取当前 AE2 网络条目，并按 AE2 15.4.10 `EncodingHelper.ENTRY_COMPARATOR` 的同序规则生成递增优先级：可合成优先于不可合成、未损坏优先于已损坏、现存数量较多优先于较少；所有网络条目优先于玩家物品栏，玩家物品栏又优先于完全不存在的候选。物品 `Ingredient` 不只枚举声明栈，还让 client repo 中满足 `AEItemKey.matches(ingredient)` 的 NBT/损伤变体参与选择。标准 JEI 槽在库存无匹配时回退当前显示项，再回退第一个可表示候选；Create/GTCEu 原生 ingredient 在库存无匹配时回退声明首项。流体只在 recipe 声明的可表示 fluid candidates 中按相同 AEKey 优先级选择。输出不使用输入替代选择；歧义输出仍按确定性门禁拒绝。

### 13.3 Create 映射

`SequencedAssemblyRecipe` 的第 0 列是 `getIngredient()` 按共享库存优先级选择的候选；之后按 `loops` 外层、`sequence` 内层的执行顺序追加每个工序的外部消耗。工序若包含多个 item/fluid ingredient，则该工序作为一列写入；`ItemApplicationRecipe` 的 held item 作为非消耗工具跳过。最终结果只允许一个确定性 item stack；结果池存在多个候选、概率不为 1 或结果为空时拒绝。该规则会接受确定性的 `create:sequenced_assembly/sturdy_sheet`，并保守拒绝带随机最终池的精密构件一类配方。

`MechanicalCraftingRecipe` 先按配方实际 width/height 标记非空行和非空列。若非空行数小于等于非空列数，则按行拆分；否则按列拆分。选择方向中的每个非空行/列按网格自然顺序成为一个包裹列；首个 ingredient 前和 ingredient 之间的空位作为 sparse `null` 保留，只有最后一个 ingredient 后的空位裁掉。因此 Create 自带 5 行、3 列均非空的 `mechanical_crafting/extendo_grip` 会产生 3 个列包裹：左右列各为“空、空、木棍、木棍”，中列为 5 个连续材料，而不是把左右列的木棍压到第 0、1 格。该策略只最小化包裹数，不改变选中行/列的原始坐标；相同包裹数时固定按行，保证结果稳定。

其它 Create `ProcessingRecipe` 按 declared order 把每个可表示 item/fluid ingredient 分别映射成一个包裹列，输出按 declared order 写入 item/fluid result。chance 不为 1 的输出、区间/随机数量、空 ingredient 候选和超过终端上限的内容都拒绝。Create 未安装或 recipe object 不属于受支持类型时适配器返回 not-applicable，不触发 Create 类加载。包裹页复用专用适配得到的确定性结果，再把各列按顺序展平成一个包裹 contents，并把首个 item 输出用作 marker。

### 13.4 GTCEu 映射

GTCEu 适配器只把确定性的 item/fluid capability content 写入样板。每个一次性输入独占一个包裹列；每个 tick input 先把固定 amount 乘 recipe duration 后同样独占一列；输入的替代 item/fluid 候选使用共享库存优先级，输出保持配方声明结果；输出采用相同数量换算。每个 `Content` 使用独立乘数，不能让前一个区间 ingredient 的固定数量污染后续 content。`chance == 0` 的输入视为不消耗催化剂而跳过；其它概率值、`min != max` 的区间数量、乘法溢出、缺少可表示候选或超出终端边界时拒绝。能量等机器运行 capability 不是待封装资源，不写入高级样板。

StarT Fork 的 layered recipe 是上述逐材料规则的专用例外。服务器配方含 `layered_steps` 时，适配器调用 `LayeredRecipeHelper.getLayeredSteps`；JEI/JEMI 传入只含 `layered_info` 的预展开 XEI 展示配方时，调用 `calculateRecipeSteps` 按索引映射重建步骤；只有 `layered_xei` 时先通过 `getXeiLayeredRecipe` 解码，再按解码对象实际携带的 `layered_steps` 或 `layered_info` 选择同一路径。返回列表中的每个 `GTRecipe` 就是机器实际执行的一个 layer，按列表顺序写入一个包裹列；该步骤的一次性 item、一次性 fluid、按该步骤 duration 展开的 tick item/fluid 全部留在同一列，步骤内不再按材料拆包。输出仍读取 layered recipe 的确定性最终输出。由于该公开 helper 只存在于 StarT Fork、不存在于上游 GTCEu 7.5.3 编译 API，Fork 专用桥在 `GtceuRecipeSemanticEncoder` 内按固定类名反射调用公开方法；它不是 viewer wrapper 解包、handler 发现或 EMI recipe recovery。只要 layer 数据存在却无法取得完整步骤，就明确拒绝，不能回退到已经丢失层边界的扁平输入。

原生 GTCEu EMI 分类虽使用 `GTEmiRecipe` 展示，但 Applied Packaging 不读取其非公开 `recipe` 字段。`EmiRecipeResolver` 先读取公开 `EmiRecipe#getBackingRecipe()`；为空时按 `EmiRecipe#getId()` 查询当前 `RecipeManager`；仍为空且 GTCEu 已加载时，调用隔离的 `GtceuRecipeLookup` 遍历公开 `GTRegistries.RECIPE_TYPES`、每个 `GTRecipeType#getCategories()` 与 `getRecipesInCategory()`，按 ID 找回 category/XEI-only `GTRecipe`。同 ID 存在普通与 layered 表示时优先携带 `layered_steps`、`layered_xei` 或 `layered_info` 的对象。三步均失败才允许原生 EMI 标准 item/fluid 数据回退；不能用私有字段反射伪造成功。

开发编译固定使用 GTCEu 7.5.3 API。GregTech Modern - StarT Fork 1.7.0b 仍声明 `modId=gtceu`，且本集成使用的 `GTRecipe`、`Content`、`ItemRecipeCapability`、`FluidRecipeCapability` 公共成员保持兼容；`-PgtceuRuntimeJar=<jar>` 只替换开发运行时，不改变发布 metadata 或引入 fork 硬依赖。fork 运行使用独立目录，避免与上游 GTCEu world registry 快照互相产生 missing mapping 噪音。

### 13.5 已审查的通用兼容边界

```text
Mekanism Sawmill                       secondaryChance 只能为 0 或 1
Immersive Engineering Crusher/Arc     StackWithChance 必须为 1
Thermal machine recipes               output chance 必须是正整数；可选 catalyst 不计输入
Botania Mana/Petal/Terra Plate        按 JEI INPUT/CATALYST/OUTPUT；Orechid 拒绝
PneumaticCraft Explosion Crafting     lossRate 必须为 0；Pressure/Assembly/Fluid Mixer 按标准角色
Ars Nouveau Crush                     chance 必须为 1 且 maxRange 必须为 1；动态 reagent NBT 拒绝
Industrial Foregoing Laser Drill      拒绝非消耗/权重生成器；普通机器按标准角色
Ender IO Sag Mill                     chance 必须为 1 且 grinding-ball bonus 为 NONE；切片工具按 CATALYST 跳过
```

检查通过反射读取公开语义，不对这些 Mod 添加编译或发布依赖。反射异常一律返回不支持，不回退为可能错误的确定性计划。

### 13.6 JEI 转移结果

`AdvancedRecipeTransferHandler` 只注册 `AdvancedPatternEncodingTermMenu.TYPE` 和 `RecipeType<?>` 通配入口，避免覆盖 AE2 原版终端已有转移器。专用适配器成功时保留其高级语义；没有专用适配器时进入 JEI 标准槽位回退。适配器明确拒绝时返回用户可见的本地化 error tooltip。JEI 调用 `doTransfer=false` 只进行完整可行性检查，不修改菜单；`doTransfer=true` 也只发送一次当前页面 action，最终状态仍以服务端校验结果为准。

### 13.7 JEI / EMI 双前端领域共享边界

Applied Packaging 发布 `AppliedPackagingJeiPlugin` 与 `AppliedPackagingEmiPlugin` 两个独立入口。JEI universal handler 调用 `JeiRecipeExtractor`，直接接收传入 recipe object 并把 `IRecipeSlotsView` 转为 `StandardRecipeData`；原生 EMI handler 调用 `EmiRecipeExtractor`，只在 `EmiCraftContext.Type.FILL_BUTTON` 上下文工作，并在 EMI API 边界处理 chance、amount、tooltip 与 craft 生命周期。`RecipeExtraction` 只保存可空 semantic recipe 和 item/fluid 领域数据；`PatternTransferPlanFactory` 是两条前端唯一共享边界，Create/GTCEu 语义编码器、标准计划工厂、候选选择、payload 上限和菜单写入均不依赖 JEI/EMI 类型。handler、extractor、EMI resolver 与计划工厂直接调用编译期公共 API，不使用 `Class.forName`、反射字段或 viewer 实现类判断。EMI 1.1.24 为 JEMI bridge recipe 的公开 ID 分配 `jei` namespace；原生 EMI handler 只据此返回不支持，继续由 EMI 内置 `JemiRecipeHandler` 调用 JEI `transferRecipe()`，避免同一点击存在两条竞争 transfer 路径。Gradle 对 JEI 与 EMI API 都只做 compile-only；`recipeViewerRuntime=jei` 与 `recipeViewerRuntime=emi` 分别验证两条可选类加载边界，整合包另验证 JEI+EMI/JEMI 共存，两个查看器都不写入发布硬依赖。

### 13.8 Star Technology 星门装配边界

Star Technology 的 `stargate_component_assembly` 由 KubeJS startup script 注册为 GTCEu recipe type，实际 server recipe 通过 `.layeredRecipe(...)` 声明层序列；它不是可直接依赖的独立 Java recipe category。分组权威来自 StarT Fork 已序列化的 `layered_steps` / `layered_xei` / `layered_info` 和 `LayeredRecipeHelper`，不是 recipe type 名称，也不是已经压平的 capability content。高级样板固定按“每个 layer 一个包裹列”编码；layer 内物品和流体保持同包。普通 GTCEu 配方仍逐材料分包，泛 KubeJS 接口只在其它脚本配方确实需要显式声明分组时再设计。

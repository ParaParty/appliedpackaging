# 概要设计

## 1. 版本与依赖基线

```properties
minecraft_version=1.20.1
forge_version=47.4.10
java=17
ae2_version=15.4.10
guideme_version=20.1.7
guideme_version_range=[20.1.7,20.2.0)
jei_version=15.20.0.134
create_version=6.0.8-291
gtceu_version=7.5.3
moddevgradle_legacyforge=2.0.91 或更新的兼容 2.x
```

项目元数据：

```text
mod_id: appliedpackaging
mod_name: Applied Packaging
display_name_zh_cn: 应用封装
package: com.warmthdawn.appliedpackaging
license: All Rights Reserved，除非发布前另行确定
version: 0.1.0-dev 起步，首个发布版本为 1.0.0
```

发布依赖范围：

```text
Minecraft: [1.20.1, 1.21)
Forge: [47.4.10,)
AE2: [15.4.10,16)
GuideME: [20.1.7,20.2.0)
```

开发运行时还需显式加入 GuideME 20.1.7。AE2 15.4.10 的 mod metadata 要求 `guideme` 版本范围 `[20.1.7,20.2.0)`；当前 Gradle 使用的 AE2 Modrinth runtime 坐标不会自动带出这个传递依赖。因此 Applied Packaging 的发布 metadata 也直接声明 `guideme` mandatory dependency，避免发布页或整合包解析时漏装 GuideME。

JEI 15.20.0.134、Create 6.0.8-291 和 GTCEu 7.5.3 只用于可选配方导入。Gradle 对 JEI 使用 API compile-only 加完整 Forge runtime，对 Create 使用 slim compile-only 加 all runtime，对 GTCEu 使用 compile-only 加可替换 runtime；它们不进入 Applied Packaging 的 mandatory 发布依赖范围，`mods.toml` 不声明三者。运行时只有 JEI 会发现 `@JeiPlugin`；Create/GTCEu 专用适配器位于独立类中，只在 `ModList` 确认对应 mod id 后通过类名加载。其它 Mod 不加入 Gradle 依赖，而是经 JEI `IRecipeSlotsView` 的标准角色和 typed ingredient API 通用兼容。`-PgtceuRuntimeJar=<versioned-jar>` 可把开发运行时替换为本地兼容 fork，并使用独立 `run-gtceu-fork` 目录；编译 API baseline 仍保持 GTCEu 7.5.3。

## 2. 核心架构

```text
ME Package Assembler / ME 包裹装配室：
  类 AE2 Molecular Assembler。
  接收 AE2 样板供应器推入的一批输入，或按本地任意可解码已编码样板锁定真实输入槽；安装有效本地样板时，Forge item capability 同步暴露这些按位置过滤的输入位与有序输出位。同一资源出现在多个样板位置时仍对应多个输入位和多个包裹 contents 条目。机器消耗本机 AE 网络能量推进合成进度；本地输入在完成前保留且可交互，缺料暂停、补齐继续，完成时先原子扣除全部对应槽位，成功后才提交包裹。

ME Packager / ME 打包机：
  类 Create Packager。
  只通过固定底部与模型背面接入 AE 网络，在所连网格的 MEStorage 内容和包裹之间做整包转换；空容量槽为 9 单位/9 类型，只支持 16k/64k/256k storage component 升级。

Package Buses / 包裹总线家族：
  Package Storage Bus 与 Package Unpacking Bus 均为 AE2 cable part，只暴露合法包裹或在完整模拟通过后把合法包裹内容推入目标端点，并各自占用 channel。
  Package Export Bus 已移除；不再保留独立输出包裹到相邻库存的设备。

Sequence Buffer / 序列缓存器：
  单块是一次输入锁存的泛型 AEKey 缓存；沿 X/Y/Z 任一轴的直线结构由不存储资源的唯一端点协调拓扑、配置、顺序输入、合并抽取、同步输出、样板位置映射和全结构输入延迟。结构方向使用独立 `sequence_direction`，不会覆盖各方块自身六向 `facing`。Forge item/fluid capability 分别适配物品与流体，AE2 MEStorage 保留泛型 key，Pattern Provider 通过专用 ICraftingMachine 路径原子提交。
```

## 3. 模块划分

```text
core.package_data
  PackageData / PackageDataStorage / PackageCanonicalizer
  PackagePlanBuilder / PackageCapacityProfile / PackageCapacityCalculator / PackageFilter
  package-pattern and advanced-pattern data adapters

core.item_handler / core.fluid_handler / core.ae2
  cumulative simulation, commit and rollback helpers
  MEStorage package planning and package-only storage adapters

world.block.entity
  MePackagerBlockEntity
  PackageAssemblerBlockEntity
  SequenceBufferBlockEntity

core.sequence_buffer
  SequenceBufferConfig / SequenceBufferTopology
  SequenceBufferPatternPlanner / SequenceBufferTransferPlan
  sparse pattern layout adapters and check-then-push output planning

pattern integration
  AE2 PatternEncodingTermScreen and its four native mode panels remain entirely original
  ordinary terminals reject package_pattern and advanced_processing_pattern at their encoded-pattern slot
  AdvancedPatternEncodingTerminalPart/Menu/Screen owns both ADVANCED and PACKAGE pages
  one persisted SpecializedPatternMode selects the page; carrier insertion selects the matching page automatically
  advanced inputs/outputs and package inputs/marker/preview are separate persisted inventories and are never migrated on page switch
  the screen has complete advanced/package geometry profiles: a 217x250 advanced profile with a 195px body plus 22px mode-tab region versus the native 195x233 package profile with its 124x66 panel, at two network rows
  switching profiles reinitializes and recenters the same Screen instance; background, search, scrollbars, inventory, carriers, controls, and active slots all receive page-specific positions
  the two right-side mode controls use high-version Pattern Encoding Terminal `TabButton.Style.HORIZONTAL` sprites and placement: 22x22 tabs with 21px step, attached to each profile's encoding-area edge
  the specialized menu suppresses VIEW_CELL slot creation, so the screen has no display-component panel
  no ScreenEvent extension, PatternEncodingTermScreen behavior mixin, delegate screen, or InitScreens factory replacement
  three narrow accessors expose MEStorageScreen client state, Scrollbar style, and Slot coordinates
  one narrow AEBaseMenu slot-validation mixin rejects specialized carriers only in ordinary PatternEncodingTermMenu
  dedicated decoders implemented by the current package and advanced pattern item subclasses

part
  PackageStorageBusPart
  PackageUnpackingBusPart
  shared PackageBusMenu/Screen and filter state

registry / data / gametest
  Forge registries for the two machines, package entity, parts and menus
  recipes, loot, tags, models and language resources
  deterministic transaction, machine, pattern and real AE2-part GameTests
```

## 4. 架构原则

1. `PackageData` 是纯数据，不直接调用 Forge 或 AE2 网络；`contents` 是身份相关的有序列表，数据层不合并同类条目也不排序；可选 `PackageLayout` 保存 sparse 样板的总槽数及每个条目的原槽位，并参与包裹身份。
2. `PackageDataStorage` 是 1.20.1 NBT 与未来 Data Component 的唯一读写入口。
3. 两台包裹机器共享 `PackageCapacityProfile` 的 storage component 映射和单包容量计算；包裹规划与 MEStorage 操作先模拟后提交，Package Assembler 的本地样板与 Pattern Provider 推送也必须在接收输入前预检全部预计包裹。Forge item handler 拆包采用 Pattern Provider 式 check-then-push，只在整包累计模拟通过后按 contents 顺序逐条真实推入，重复资源条目不预先聚合；不维护自定义跨 handler 回滚层。
4. 打包和拆包以单个包裹为最小操作单位。
5. ME 打包机只通过固定底部与模型背面加入 AE 网格并使用该网格的 MEStorage；其它面不接入 ME 线缆，不扫描相邻 Forge item/fluid handler，也不回落到 Forge item/fluid handler。
6. 装配室只处理样板语义，不处理相邻存储打包和拆包。
7. 总线家族只路由包裹，不暴露包裹内部散装资源。
8. 客户端类必须隔离，dedicated server 不得加载 screen/render/client event 类。
9. 序列缓存器不建立独立全局多方块 SavedData；端点位置、顺序方向和本地配置副本由方块实体持久化，结构成员只在已加载的连续方块内重建，避免强制加载区块。
10. 序列缓存器的模拟不得改变锁存、冷却、拓扑或输入 holder；多格输入、同步输出和 Pattern Provider push 都先构造完整计划，再按服务器主线程顺序提交。

## 5. 关键流程

普通/彩色自动合成：

```text
AE2 Pattern Provider
-> adjacent ICraftingMachine.pushPattern(...)
-> PackageAssembler uses the pushed pattern as a temporary plan, Molecular Assembler style
-> validate local assembler is empty, output slots are empty, and KeyCounter exactly satisfies the plan
-> preflight every predicted package against the shared current capacity profile without consuming KeyCounter input
-> input holder all-or-nothing consume
-> assembler craft progress advances with speed cards
-> output package(s) insert into local output slots in order
-> output mode exports one package at a time to ME network, adjacent item handler, or leaves output local
```

样板类型路由：

```text
AE2 crafting_pattern + appliedpackaging.package_crafting_pattern
-> PackageCraftingPatternDetails
-> Package Assembler exact single-package plan preserving encoded input order

ordinary AE2 crafting / processing / stonecutting / smithing pattern
-> ordinary encoded-pattern plan
-> one Fluix package with empty name and normalized primary-output marker, preserving non-empty encoded input-slot order

Applied Packaging advanced_processing_pattern
-> inherits AE2 processing-pattern decoding and preserves normal inputs/outputs for planning
-> Package Assembler keeps 17 contiguous 81-slot sparse columns only in pattern identity, then allocates a pattern-sized dense non-empty local-input view carrying each entry's original column
-> ordered multi-package plan using each column's color and marker; each package preserves row order
```

序列缓存器结构与输入：

```text
wrench cycles an unformed endpoint's six-direction block-local facing
-> neighbor update snapshots that facing as sequence_direction and scans a loaded, contiguous, unformed X/Y/Z line up to the configured limit
-> one endpoint plus ordered members are committed and endpoint configuration is copied
-> formation and topology repair preserve every block's original directional/facing state
-> tail placement extends the same controller; gaps truncate the endpoint fragment and detach the tail fragment

item pipe / MEStorage inserts at endpoint
-> choose first unlocked member in endpoint-to-tail order
-> apply exact-key filter and per-member capacity
-> real insertion latches that member and raises the structure output barrier

Pattern Provider ICraftingMachine.pushPattern
-> recover sparse positions for known ordinary AE2/Applied Packaging pattern details, including empty positions
-> advanced processing patterns explicitly use their dense public input order and ignore pattern mode
-> copy and preflight KeyCounter contents, member occupancy, filters and capacity
-> consume input holders only after the complete member assignment succeeds

Package Unpacking Bus -> Sequence Buffer endpoint
-> validate PackageData and its optional positional layout
-> map layout positions to storage members (endpoint is not slot 1), or use dense contents order when absent
-> commit the whole held package only after every member assignment succeeds
```

ME 打包机打包：

```text
redstone/button trigger
-> access the connected grid through the fixed bottom or model-back node side
-> resolve that grid's AE2 MEStorage
-> enumerate endpoint contents
-> unpack source packages into virtual entries
-> apply content filter
-> sort all selected virtual entries by canonical stack key
-> build package plan
-> simulate endpoint extraction
-> simulate output slot insertion
-> commit extraction
-> insert generated package stack
```

ME 打包机拆包：

```text
incoming package stack
-> validate PackageData
-> apply package filter
-> expand contents
-> simulate complete insertion into the connected grid MEStorage
-> commit the complete package contents
-> clear the held package only after a successful commit
```

包裹卸货总线路由：

```text
AE2 cable host -> part grid node
part mounted side -> adjacent Forge item handler
mount a Formation-Plane-style insertion-only MEStorage at the exact player-configured priority, default 0
use the same player-configured priority shown by the top-right Priority submenu; at an equal value, the unpacking sink enters AE2's preferred-storage pass before PackageItemStorage
accept at most one network-routed package only after filter, Pattern Provider blocking and cumulative target simulation pass
move the accepted package directly into the part's persisted held state without exposing storage or extraction
run the same 20-tick unpacking work phase as ME Packager
revalidate filter, blocking and adjacent target capacity at the final tick
push all package contents after the cumulative simulation succeeds, then clear held state
if final simulation rejects the package, retain the same held package locally and retry after the speed-card-adjusted interval
allow idle/blocked GUI recovery and add the held package to part-removal drops
```

## 6. 版本适配

```text
1.20.1:
  ItemStack NBT adapter
  custom advanced processing pattern stores package-column metadata in namespaced ItemStack NBT
  ordinary AE2 processing_pattern never stores advanced package-column metadata
  Forge 47.4.x
  AE2 15.x

1.20.5+:
  Data Component adapter
  重新评估 item/component hash

1.21+:
  重新评估 AE2 API、Forge/NeoForge 分支和菜单/网络 API
```

包裹规划业务逻辑集中在：

```java
PackageData
PackageDataStorage
PackagePlanBuilder
```

`PackageDataStorage` 隔离 NBT；实际网络读写只在 MEStorage 适配层中调用 AE2 runtime API。项目不保留尚未实现的 `AEGenericStackAdapter` 或 `GenericStorageEndpoint` 抽象。

## 7. JEI 通用配方导入架构

```text
JEI recipe category / transfer context
  -> AppliedPackagingJeiPlugin
  -> AdvancedRecipeTransferHandler（只绑定 AdvancedPatternEncodingTermMenu）
  -> 专用 AdvancedRecipeTransferAdapter 链（优先）
       CreateRecipeTransferAdapter
       GtceuRecipeTransferAdapter
  -> StandardRecipeTransferAdapter（JEI INPUT/OUTPUT 通用回退）
  -> AdvancedPatternTransferPlan / PackagePatternTransferPlan
  -> 32 KiB 上限的 JSON action payload
  -> AdvancedPatternEncodingTermMenu 服务端重新解码和完整校验
  -> 当前页面状态的原子 replaceRecipe
```

JEI handler 不新增或复制第三方 recipe category，而是从当前分类提供的 recipe object 与 `IRecipeSlotsView` 构造 Applied Packaging 编辑状态。Create/GTCEu 专用适配器优先保留工序、tick content、随机概率等 recipe object 语义；其余配方由标准适配器读取 JEI `INPUT` / `OUTPUT`，自然跳过 `CATALYST` / `RENDER_ONLY`。通用层仅接受物品和 Forge 流体 typed ingredient；输入候选沿用 JEI 当前显示值，输出候选必须收敛到唯一 `GenericStack`。通用 JEI handler 自身只在 JEI 发现插件后加载；未安装 JEI 时主模组不引用该插件类。

客户端计划先经过列数、每列输入数、输出数、数量、AEKey 和 32767 字符 AE2 client-action 上限检查；服务端收到 action 后先重复 payload 大小与结构边界检查，再通过 `TagParser` / `GenericStack.readTag` 重建每个 stack。高级页成功后强制模式为 ADVANCED，保留同索引列颜色并批量替换列/输出；包裹页成功后强制模式为 PACKAGE，保留当前包裹颜色并批量替换最多 81 个内容和可选 item marker。两个状态层都只发出一次 change callback，服务端校验失败只记录拒绝，不应用任何部分计划。

`RecipeTransferSemantics` 是通用槽位层前的保守语义闸门。当前源码审查覆盖 Mekanism、Immersive Engineering、Thermal Series、Botania、PneumaticCraft、Ars Nouveau、Industrial Foregoing 与 Ender IO：明确拒绝概率副产物、动态数量/NBT、Orechid/激光钻等非普通消耗型生成器；修正 Thermal 把可选 catalyst 标成 INPUT 的已知分类问题，按实际 item/fluid 输入数量分别截取。未知 Mod 若遵守 JEI 标准角色且输出确定，可直接兼容；若把工具错误标为 INPUT 或把概率只画在 tooltip、recipe object 又不暴露语义，则不宣称已验证，需新增窄适配或拒绝规则。

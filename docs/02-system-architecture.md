# 概要设计

## 1. 版本与依赖基线

```properties
minecraft_version=1.20.1
forge_version=47.4.10
java=17
ae2_version=15.4.10
guideme_version=20.1.7
guideme_version_range=[20.1.7,20.2.0)
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

## 2. 核心架构

```text
ME Package Assembler / ME 包裹装配室：
  类 AE2 Molecular Assembler。
  接收 AE2 样板供应器推入的一批输入，或按本地已编码样板锁定真实输入槽，消耗本机 AE 网络能量推进合成进度并生成包裹输出。

ME Packager / ME 打包机：
  类 Create Packager。
  只通过固定底部与模型背面接入 AE 网络，在所连网格的 MEStorage 内容和包裹之间做整包转换，并支持 16k/64k/256k 容量元件。

Package Buses / 包裹总线家族：
  Package Storage Bus 与 Package Unpacking Bus 均为 AE2 cable part，只暴露合法包裹或在完整模拟通过后把合法包裹内容推入目标端点，并各自占用 channel。
  Package Export Bus 已移除；不再保留独立输出包裹到相邻库存的设备。
```

## 3. 模块划分

```text
core.package_data
  PackageData / PackageDataStorage / PackageCanonicalizer
  PackagePlanBuilder / PackageCapacityCalculator / PackageFilter
  package-pattern and advanced-pattern data adapters

core.item_handler / core.fluid_handler / core.ae2
  cumulative simulation, commit and rollback helpers
  MEStorage package planning and package-only storage adapters

world.block.entity
  MePackagerBlockEntity
  PackageAssemblerBlockEntity

pattern integration
  AE2 Pattern Encoding Terminal mixins for package mode
  AdvancedPatternEncodingTerminalPart/Menu/Screen
  read-only compatibility decoders for previously encoded pattern carriers

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

1. `PackageData` 是纯数据，不直接调用 Forge 或 AE2 网络。
2. `PackageDataStorage` 是 1.20.1 NBT 与未来 Data Component 的唯一读写入口。
3. 包裹规划与 MEStorage 操作先模拟后提交；Forge item handler 拆包采用 Pattern Provider 式 check-then-push，只在整包累计模拟通过后执行真实插入，不维护自定义跨 handler 回滚层。
4. 打包和拆包以单个包裹为最小操作单位。
5. ME 打包机只通过固定底部与模型背面加入 AE 网格并使用该网格的 MEStorage；其它面不接入 ME 线缆，不扫描相邻 Forge item/fluid handler，也不回落到 Forge item/fluid handler。
6. 装配室只处理样板语义，不处理相邻存储打包和拆包。
7. 总线家族只路由包裹，不暴露包裹内部散装资源。
8. 客户端类必须隔离，dedicated server 不得加载 screen/render/client event 类。

## 5. 关键流程

普通/彩色自动合成：

```text
AE2 Pattern Provider
-> adjacent ICraftingMachine.pushPattern(...)
-> PackageAssembler uses the pushed pattern as a temporary plan, Molecular Assembler style
-> validate local assembler is empty, output slots are empty, and KeyCounter exactly satisfies the plan
-> input holder all-or-nothing consume
-> assembler craft progress advances with speed cards
-> output package(s) insert into local output slots in order
-> output mode exports one package at a time to ME network, adjacent item handler, or leaves output local
```

样板类型路由：

```text
AE2 crafting_pattern + appliedpackaging.package_crafting_pattern
-> PackageCraftingPatternDetails
-> Package Assembler exact single-package plan

AE2 processing_pattern without Applied Packaging column metadata
-> ordinary processing plan
-> one Fluix package with empty name and marker

Applied Packaging advanced_processing_pattern
-> inherits AE2 processing-pattern decoding and preserves normal inputs/outputs for planning
-> Package Assembler groups sparse inputs by contiguous 4-slot columns
-> ordered multi-package plan using each column's color, name, and marker
```

ME 打包机打包：

```text
redstone/button trigger
-> access the connected grid through the fixed bottom or model-back node side
-> resolve that grid's AE2 MEStorage
-> enumerate endpoint contents
-> unpack source packages into virtual entries
-> apply content filter
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

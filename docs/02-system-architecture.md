# 概要设计

## 1. 版本与依赖基线

```properties
minecraft_version=1.20.1
forge_version=47.4.10
java=17
ae2_version=15.4.10
guideme_version=20.1.7
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
```

开发运行时还需显式加入 GuideME 20.1.7。AE2 15.4.10 的 mod metadata 要求 `guideme` 版本范围 `[20.1.7,20.2.0)`；当前 Gradle 使用的 AE2 Modrinth runtime 坐标不会自动带出这个传递依赖。

## 2. 核心架构

```text
ME Package Assembler / ME 包裹装配室：
  类 AE2 Molecular Assembler。
  接收 AE2 样板供应器推入的一批输入，按普通/彩色/封装处理样板生成包裹。

ME Packager / ME 打包机：
  类 Create Packager。
  只贴着相邻存储端点工作，在相邻存储内容和包裹之间做事务转换。

Package Buses / 包裹总线家族：
  只路由合法包裹，或把合法包裹事务拆入目标端点。
```

## 3. 模块划分

```text
core.package
  PackageData
  PackageDataStorage
  PackageEntry
  PackageColor
  MarkerSpec
  PackageCanonicalizer
  PackageCapacityProfile
  PackageTooltipBuilder

core.plan
  PackagePlan
  PackagePlanner
  RepackPlanner
  CapacityCalculator
  PackageFilter
  PackageTransactionResult

ae2
  AEKeySerializer
  AEGenericStackAdapter
  AEStorageEndpoint
  AEPatternAdapter
  AECellComponentMatcher
  AEBusAdapter

registry
  APItems
  APBlocks
  APBlockEntities
  APMenus
  APCreativeTabs

machine.assembler
  PackageAssemblerBlock
  PackageAssemblerBlockEntity
  PackageAssemblerMenu
  PackageAssemblerScreen

machine.packager
  PackagerBlock
  PackagerBlockEntity
  PackagerMenu
  PackagerScreen

pattern
  PackagePatternItem
  PackagedProcessingPatternItem
  PatternDataStorage
  PackagePatternTerminalBlock/Menu/Screen

bus
  PackageStorageBusPart
  PackageExportBusPart
  PackageUnpackingBusPart
  PackageBusMenu/Screen

data
  recipes
  loot
  tags
  models
  lang

gametest
  PackageDataTests
  PackagerTransactionTests
  FilterTests
```

## 4. 架构原则

1. `PackageData` 是纯数据，不直接调用 Forge 或 AE2 网络。
2. `PackageDataStorage` 是 1.20.1 NBT 与未来 Data Component 的唯一读写入口。
3. 所有会改变世界或库存的行为先生成 `PackagePlan`，再做模拟，最后提交。
4. 打包和拆包以单个包裹为最小事务单位。
5. ME 打包机只扫描相邻端点，不扫描自身所在任意 ME 网络。
6. 装配室只处理样板语义，不处理相邻存储打包和拆包。
7. 总线家族只路由包裹，不暴露包裹内部散装资源。
8. 客户端类必须隔离，dedicated server 不得加载 screen/render/client event 类。

## 5. 关键流程

普通/彩色自动合成：

```text
Pattern Provider pushPattern
-> PackageAssembler.acceptInputs(...)
-> PatternAdapter.resolvePackagePlan(...)
-> PackagePlanner.validateCapacity(...)
-> outputBuffer.simulateInsert(packages)
-> inputBuffer.consume(...)
-> outputBuffer.insert(packages)
-> optional AE network insert
```

ME 打包机打包：

```text
redstone/button trigger
-> detect adjacent endpoint
-> enumerate endpoint contents
-> unpack source packages into virtual entries
-> apply content filter
-> build package plan
-> simulate endpoint extraction
-> simulate output slot insertion
-> commit extraction
-> insert generated package stack
```

ME 打包机/拆包总线拆包：

```text
incoming package stack
-> validate PackageData
-> apply package filter
-> expand contents
-> simulate full insert into target endpoint
-> accept N whole packages
-> commit insert for accepted packages
-> return remainder
```

## 6. 版本适配

```text
1.20.1:
  ItemStack NBT adapter
  Forge 47.4.x
  AE2 15.x

1.20.5+:
  Data Component adapter
  重新评估 item/component hash

1.21+:
  重新评估 AE2 API、Forge/NeoForge 分支和菜单/网络 API
```

业务逻辑只依赖：

```java
PackageDataStorage
AEGenericStackAdapter
GenericStorageEndpoint
```

不直接把 NBT、Data Component 或具体 AE2 runtime API 泄漏到核心规划逻辑中。

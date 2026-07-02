# Applied Packaging 工程设计文档

状态：开发基线  
目标版本：Minecraft 1.20.1 Forge  
目标加载器与工具链：Forge 47.4.x + ModDevGradle Legacy  
目标 AE2 版本：Applied Energistics 2 15.4.10 Forge  
Mod ID：`appliedpackaging`  
Java 包名：`com.warmthdawn.appliedpackaging`  

本文档是 `docs/chat-summary.md` 中多轮设计讨论的工程化定稿，用于指导代码实现、资源制作、测试和发布。原始讨论记录只保留推导过程；开发时以本文为准。

---

## 1. 项目目标

Applied Packaging / 应用封装是一个 AE2 附属 Mod。它给 AE2 自动化体系增加一层“包裹化物流”：

```text
AE2 样板语义 / 相邻存储内容
-> 包裹
-> 包裹路由、过滤、暂存、拆包
-> 目标机器 / 目标网络
```

这个 Mod 不做通用压缩库存，也不做普通箱子替代品。它只解决一个明确问题：把一批应该被一起移动、过滤、等待或拆入机器的资源封装成一个可路由的包裹单元，并且让这个单元仍然尊重 AE2 的样板、网络、总线和存储心智。

核心玩法拆成两条互不混淆的生产路径：

```text
ME Package Assembler / ME 包裹装配室：
  类 AE2 Molecular Assembler。
  接收 AE2 样板供应器推入的一批输入，按普通/彩色/封装处理样板生成包裹。

ME Packager / ME 打包机：
  类 Create Packager。
  只贴着相邻存储端点工作，在相邻存储内容和包裹之间做事务转换。
```

AE2 官方 1.20.1 指南明确说明 Pattern Provider 会把样板输入推入相邻库存，且必须一次推入全部输入，不能推半批；方向型 Pattern Provider 的推入面不提供网络连接，适合隔离主网和子网。这是装配室的语义基础。AE2 1.20.1 仍使用 Forge 平台，AE2 官方下载页列出 1.20.1 的 15.4.10 版本，Modrinth 页面也标注 15.4.10 支持 Minecraft 1.20.1 Forge。

---

## 2. 需求分析

### 2.1 目标用户

目标玩家已经熟悉 AE2 自动合成、Pattern Provider、Storage Bus、Export Bus、Interface 子网和存储元件。他们希望把复杂机器输入拆成有颜色和标记的物流包，并通过 AE2 网络或其他物流 Mod 进行路由。

### 2.2 核心用户场景

场景 A：彩色自动合成

```text
主网 Pattern Provider
-> ME 包裹装配室
-> 红/蓝/Fluix 等多个包裹
-> 包裹子网或普通物流线
-> 包裹拆包总线 / ME 打包机
-> 目标机器
-> 产物回主网
```

场景 B：Create 式相邻库存打包

```text
箱子 / 机器缓存 / ME Interface 子网
-> ME 打包机
-> 包裹
-> 物流线
-> ME 打包机或包裹拆包总线拆入目标
```

场景 C：包裹路由与暂存

```text
AE 网络中已经存在红色包裹、蓝色包裹、带 marker 的包裹
-> 包裹存储总线只暴露包裹物品
-> 包裹输出总线只输出匹配包裹
-> 总线绝不把包裹内部内容伪装为散装库存
```

### 2.3 功能性需求

R1. Mod 必须注册 17 个独立包裹物品：Fluix 默认色 + 16 个 Minecraft 染料色。颜色由 item id 决定，不仅写入 NBT。

R2. 玩家不能正常获得“空包裹”。没有 `PackageData` 的包裹只能作为内部/调试状态存在，不进创造标签，不进正常配方链，不被物流系统接受。

R3. 包裹允许堆叠，但只有完全相同的包裹才能堆叠。一个 `红色包裹 x16` 表示 16 个相同包裹，而不是一个包裹装了 16 倍内容。

R4. 包裹内容必须以 AE2 泛型资源建模，核心结构是 `List<GenericStack>`，其中每项是 `AEKey + amount`。实现层可以先覆盖物品 key，但数据模型和事务接口不能写死成 `ItemStack`。

R5. 包裹不允许真实嵌套。当打包输入中出现包裹时，必须展开后再封装，表现为合并、改色或追加内容。

R6. ME 包裹装配室必须支持：

```text
普通处理样板 -> 1 个默认色包裹
彩色处理样板 -> 按输入格颜色生成多个包裹
包裹样板 -> 指定颜色/marker/contents 的包裹
封装处理样板 -> 多个包裹计划
输出口缓冲
阻挡模式
```

R7. ME 打包机必须支持：

```text
相邻库存识别
相邻 ME Interface 背后的存储子网识别
手动/红石触发打包
包裹拆包
源包裹展开后再封装
容量元件
颜色与 marker 策略
打包内容过滤
拆包包裹过滤
```

R8. 包裹样板终端必须支持：

```text
编码包裹样板
给普通处理样板添加输入格颜色元数据
多个包裹样板合成封装处理样板
封装处理样板拆分回多个包裹样板
```

R9. 包裹存储总线、包裹输出总线、包裹拆包总线必须只允许包裹通过。它们不能把包裹内部内容伪装成散装库存，也不能把散装库存自动打成包裹。

R10. 所有打包和拆包必须是事务性的。单个包裹不能被部分拆开；一次打包如果抽取、输出、容量任意一步模拟失败，则不得提交任何部分结果。

R11. Tooltip 必须明确显示“每包内容”和“堆叠总计”。高级信息使用 Shift/Ctrl 展示容量、类型数、hash、marker。

R12. 所有玩家可见内容必须有英文与简体中文语言文件。

R13. 必须提供可发布 jar、基础配方、loot table、模型、材质、logo/图标和 mods.toml 元数据。

### 2.4 非功能性需求

NF1. 目标平台先固定为 Minecraft 1.20.1 Forge。由于 1.20.1 没有 Data Component，包裹数据使用 ItemStack NBT 保存；业务代码必须通过 `PackageDataStorage` 抽象读写，为未来 1.20.5+ Data Component 适配留接口。

NF2. 公共逻辑必须和 Minecraft/Forge API 解耦到可单元测试的程度。包裹 canonical hash、容量计算、过滤、合并计划应能在普通 JVM 测试中验证。

NF3. 客户端渲染、菜单、屏幕、颜色处理必须和服务端逻辑分离，确保 dedicated server 不加载客户端类。

NF4. 行为敏感逻辑必须考虑 GameTest。至少覆盖包裹序列化、打包事务、拆包事务、过滤与容量失败回滚。

NF5. 资源制作必须保留资产契约、来源和验证记录，避免临时图片无法复现。

NF6. 发布构建不得包含调试物品、临时测试资源、未使用模板包或本地绝对路径。

### 2.5 范围边界

正式做：

```text
17 色独立包裹
无空包裹玩法
包裹堆叠
包裹套包裹时展开再封装
GenericStack 数据模型
ME 包裹装配室
ME 打包机
ME Interface 子网相邻识别
16k / 64k / 256k 容量元件
包裹样板
封装处理样板
包裹样板终端
包裹存储总线
包裹输出总线
包裹拆包总线
统一过滤系统
基础材质、模型、配方、语言文件、GameTest
```

暂时不做：

```text
1k / 4k 容量档
MEGA Cells 等附属大容量兼容
复杂动画
加速升级
多输出口装配室
批量编辑终端
多包裹路线可视化
Fabric/NeoForge 现代版多加载器
```

明确砍掉：

```text
空包裹
真实包裹嵌套
包裹本身有有序/无序模式
ME 打包机读取 Pattern Provider / pushPattern
ME 打包机理解普通/彩色/包裹/封装处理样板
ME 包裹装配室执行相邻存储打包
ME 包裹装配室拆包
包裹存储总线把包裹内容伪装成散装库存
包裹输出总线把散装库存自动打成包裹
any / all / exact 过滤模式
装配室阻挡模式扫描整个输出网络
打包机直接扫描自己所在的任意 AE 网络
```

---

## 3. 版本与依赖基线

### 3.1 目标版本选择

开发基线：

```properties
minecraft_version=1.20.1
forge_version=47.4.10
java=17
ae2_version=15.4.10
moddevgradle_legacyforge=2.0.91 或更新的兼容 2.x
```

说明：

```text
Forge 官方 1.20.1 页面当前列出 latest 47.4.20、recommended 47.4.10。
为了兼容性和发布稳定性，编译基线使用 recommended 47.4.10。
依赖范围可放宽到 [47.4.10,)。
```

AE2 依赖策略：

```groovy
repositories {
    mavenCentral()
    maven {
        name = "Modrinth"
        url = "https://api.modrinth.com/maven"
    }
}

dependencies {
    modImplementation "maven.modrinth:XxWD5pD3:7KVs6HMQ" // AE2 15.4.10 Forge runtime
    compileOnly "org.appliedenergistics:appliedenergistics2:15.4.10:api"
}
```

如果 Maven Central 的 AE2 API classifier 与 Forge runtime 坐标在本机解析失败，则以 AE2 GitHub Packages 或 Modrinth runtime jar 为 fallback，并在 `docs/development-log.md` 记录最终可解析坐标。

### 3.2 项目元数据

```text
mod_id: appliedpackaging
mod_name: Applied Packaging
display_name_zh_cn: 应用封装
package: com.warmthdawn.appliedpackaging
license: All Rights Reserved，除非发布前另行确定
version: 0.1.0-dev 起步，首个发布版本为 1.0.0
```

### 3.3 兼容性策略

发布版本依赖：

```text
Minecraft: [1.20.1, 1.21)
Forge: [47.4.10,)
AE2: [15.4.10,16)
```

未来版本策略：

```text
1.20.1：NBT adapter
1.20.5+：Data Component adapter
1.21+：重新评估 AE2 API、Forge/NeoForge 分支和菜单/网络 API
```

---

## 4. 概要设计

### 4.1 模块划分

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

### 4.2 架构原则

1. `PackageData` 是纯数据，不直接调用 Forge 或 AE2 网络。
2. `PackageDataStorage` 是 1.20.1 NBT 与未来 Data Component 的唯一读写入口。
3. 所有会改变世界或库存的行为先生成 `PackagePlan`，再做模拟，最后提交。
4. 打包和拆包以单个包裹为最小事务单位。
5. ME 打包机只扫描相邻端点，不扫描自身所在任意 ME 网络。
6. 装配室只处理样板语义，不处理相邻存储打包和拆包。
7. 总线家族只路由包裹，不暴露包裹内部散装资源。

### 4.3 关键流程图

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

---

## 5. 详细设计：包裹数据

### 5.1 颜色

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

### 5.2 数据结构

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
未知 AEKey 类型必须保守保存 type id 和 opaque payload；如果无法还原，则包裹标记为 invalid，不允许拆包提交。
```

### 5.3 Canonical Hash

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

### 5.4 堆叠规则

包裹物品的 `isSameItemSameTags` / Forge 等价判断必须包含完整 NBT，因此同色、同 marker、同 contents、同 version、同 flags 的包裹才能自然堆叠。

包裹 ItemStack count 语义：

```text
stack count = 包裹个数
PackageData.contents = 每一个包裹的内容
```

拆包一组包裹时必须按 count 重复单包事务，不允许把一个包裹的数据乘 count 后作为一个大事务提交。

### 5.5 容量模型

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

---

## 6. 详细设计：包裹套包裹

包裹不允许真实嵌套。任何进入打包计划的包裹都先展开为虚拟内容。

### 6.1 合并

```text
红色包裹：铁锭 x64
蓝色包裹：铜锭 x64
机器颜色：Fluix

结果：
Fluix 包裹：铁锭 x64, 铜锭 x64
```

### 6.2 改色

```text
红色包裹：铁锭 x64
机器颜色：蓝色

结果：
蓝色包裹：铁锭 x64
```

### 6.3 追加

```text
红色包裹：铁锭 x64
散装：金锭 x64

结果：
红色或机器设定色包裹：铁锭 x64, 金锭 x64
```

### 6.4 Marker 策略

ME 打包机提供三种 marker 模式：

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

---

## 7. 详细设计：ME 包裹装配室

### 7.1 职责

做：

```text
接收 Pattern Provider 推入的一批输入
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

### 7.2 方块实体状态

```text
inputBuffer: GenericStackBuffer
outputSlots: 17 item slots，只允许合法包裹
patternSlot: 本地包裹样板/封装处理样板
capacitySlot: 可选容量元件
defaultColor: PackageColor
defaultMarker: MarkerSpec optional
blockingMode: boolean
autoExportToNetwork: boolean
lastFailure: enum/string
```

输出口使用 17 格，原因是彩色处理样板一次最多按 17 色生成 17 个包裹。

### 7.3 输入来源

```text
Pattern Provider pushPattern
玩家手动放入本地样板输入
外部物品/流体/AEKey 适配输入
```

在 1.0 实现中，优先完成物品输入路径；流体和其他 AEKey 的数据模型、tooltip、过滤和拒绝原因要完整，实际 endpoint adapter 可以分阶段接入。

### 7.4 普通处理样板

普通处理样板输入 `A+B+C`，输出 `X`。

装配室行为：

```text
A+B+C -> 1 个 Fluix/default 包裹
```

AE2 自动合成仍然等待 `X`，包裹只是中间物流，不改变 AE2 对处理样板最终输出的理解。

### 7.5 彩色处理样板

彩色处理样板是普通处理样板 + 输入格颜色元数据。

规则：

```text
普通机器使用时等同普通处理样板。
ME 包裹装配室使用时按输入格颜色分组生成多个包裹。
颜色跟随样板输入格，不跟随 AEKey。
同一种 AEKey 如果位于两个颜色格，必须生成两个不同颜色的包裹。
```

### 7.6 包裹样板与封装处理样板

包裹样板：

```text
color
marker
contents
capacity tier
```

封装处理样板：

```text
普通处理样板输出
多个 PackagePlan
AE2 inputs = package plan contents 展平
AE2 outputs = 玩家设置的普通处理输出
```

### 7.7 阻挡模式

阻挡模式开启时，只检查本机输出口：

```text
如果 outputSlots 中存在任意合法包裹：
  拒绝新的输入/新样板执行
```

不检查：

```text
输出 AE 网络
包裹子网
相邻接口
目标机器
主网库存
```

### 7.8 UI

主界面：

```text
左侧：样板槽、本地输入缓存、容量元件槽
中间：包裹计划预览和彩色分组
右侧：17 格输出口
下方：默认颜色、default marker、阻挡模式、自动导入 AE 网络、状态文本
```

状态文本：

```text
等待样板
等待输入
输出口阻挡
容量不足
输出口已满
输出网络不可用
样板无法解析
```

---

## 8. 详细设计：ME 打包机

### 8.1 职责

做：

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

### 8.2 方块实体状态

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

### 8.3 存储端点

端点类型：

```text
Forge item handler
Forge fluid handler
AE2 ME Interface adjacent subnet
未来扩展：其他 AEKey endpoint
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

### 8.4 打包触发

```text
GUI Pack Once 按钮
红石脉冲 Pack Once
红石持续周期 Pack，默认关闭
```

单次触发只生成一次计划。若内容超出容量：

```text
默认：选择能完整放入的前若干内容。
strictWholeEndpointMode：候选内容不能全部放入时拒绝。
```

### 8.5 打包事务

提交前必须全部模拟：

```text
1. 端点抽取所有源内容可行
2. 源包裹作为输入可被完整消耗
3. 输出槽可接收生成包裹
4. marker 策略无冲突
5. 容量与类型数未超限
```

任何一步失败都不得改变端点、输入槽或输出槽。

### 8.6 拆包事务

输入包裹堆叠时按单包重复：

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

### 8.7 UI

主界面：

```text
左侧：相邻端点状态、端点类型、支持资源类型、可打包条目数
中间：打包预览、合并/改色/追加结果
右侧：包裹输入/输出槽、容量元件槽
下方：颜色、marker 模式、排序、红石模式、打包按钮
```

失败提示：

```text
未找到可用存储端点
目标不支持流体
目标无法完整接收
容量元件不足
输出槽已满
包裹 marker 冲突
包裹过滤不匹配
无可打包内容
```

---

## 9. 详细设计：样板与终端

### 9.1 包裹样板

物品 ID：`package_pattern`

数据：

```text
PackageColor
MarkerSpec optional
List<PackageEntry>
CapacityProfile
```

包裹样板的“输出”是对应包裹。

### 9.2 彩色处理样板

彩色处理样板不改变 AE2 的输入输出，只在 pattern NBT 中写入输入格颜色数组。

```text
slotColor[0..8] = PackageColor
```

普通机器忽略这段数据；装配室读取它。

### 9.3 封装处理样板

物品 ID：`packaged_processing_pattern`

数据：

```text
vanilla/AE2 processing pattern payload
List<PackagePlan>
flattenedInputsHash
processingOutputs
```

### 9.4 包裹样板终端

功能 tab：

```text
Package Pattern:
  17 色选择、marker ghost、GenericStack ghost slots、容量档、编码

Colored Processing Pattern:
  普通处理样板输入格、每格颜色、处理输出、编码

Packaged Processing Pattern:
  多个包裹样板、处理输出、空白样板、编码

Split:
  封装处理样板、空白样板若干、拆出包裹样板，可选普通处理样板副本
```

实现次序：

```text
1. Package Pattern item data 与 tooltip
2. 简化终端菜单，先支持物品 ghost slots
3. Packaged Processing Pattern 数据结构
4. AE2 Pattern Provider/装配室集成
5. 完整 UI polish
```

---

## 10. 详细设计：包裹总线

### 10.1 包裹存储总线

网络看到的是包裹物品，不是内部内容。

行为：

```text
只枚举相邻库存中的合法包裹
过滤不匹配的包裹不可见
插入时只允许合法包裹
不拆包
```

### 10.2 包裹输出总线

行为：

```text
从 AE 网络取已有包裹
按颜色/marker/内容过滤输出到相邻库存
不把散装物品打成包裹
不请求自动合成
不拆包
```

### 10.3 包裹拆包总线

行为：

```text
网络尝试插入包裹
总线过滤包裹
展开包裹内容
模拟完整插入相邻目标
成功才接受包裹
提交后目标得到散装内容
```

它是高效拆包路径，相当于“包裹存储总线仅输入模式 + ME 打包机拆包逻辑”。

### 10.4 过滤统一规则

包裹过滤维度：

```text
颜色
marker
内容物
```

三者 AND。未设置的过滤项忽略。

内容过滤只决定整包是否通过，绝不只处理包裹的一部分内容。

---

## 11. 资源与美术设计

### 11.1 风格

整体是 AE2 的 Fluix 数据包裹系统，不是纸箱物流。

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

### 11.2 17 色包裹

图标：

```text
小型 AE2 封装盒
主体浅灰
边框深灰
中心 Fluix 菱形封印
一条对应颜色束带
```

世界模型：

```text
10x10x10 或 12x12x12 盒体
四角金属包边
侧面颜色束带
顶部小封签
```

### 11.3 机器与总线

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
AE2 化 Packager
一面扫描相邻存储
一面包裹投递口
顶部红石灯
容量元件小窗
颜色灯条
```

包裹样板终端：

```text
AE2 终端面板
屏幕内多个彩色包裹卡片
侧边 17 色灯条
```

总线：

```text
Storage Bus + 封闭包裹图标
Export Bus + 箭头包裹图标
Bus + 打开包裹图标
```

### 11.4 资产制作约束

每组 block/item 资源必须有：

```text
asset contract
source concept 或手工像素图来源说明
textures
models
blockstates
lang keys
preview/report
```

材质验收：

```text
16x16 item 图标颜色可辨识
包裹 17 色在 JEI/创造栏中能快速区分
机器模型无 missing texture
方块破坏粒子和 item model 正常
语言文件无缺 key
```

### 11.5 材质生成协作方式

材质生成允许使用 subagent 并行处理。主 agent 的职责只包括：

```text
拆分资产包
写清楚每个资产包的 visual brief
提供颜色表、命名表、尺寸、输出路径和验收标准
审阅 subagent 交付的文件和报告
把通过验收的资源纳入项目
```

subagent 的职责包括：

```text
根据主 agent 给出的 brief 生成或绘制材质
保持输出路径与命名一致
记录来源、生成提示、修改说明和预览
不修改 Java/Gradle/设计文档
不覆盖其他 subagent 的资产
```

资产分包建议：

```text
asset-packages:
  packages:
    17 色包裹 item 图标、包裹基础模型、颜色替换表

  machines:
    ME 包裹装配室、ME 打包机、方块模型与 block/item 贴图

  terminal-and-buses:
    包裹样板终端、包裹存储总线、包裹输出总线、包裹拆包总线

  ui-and-icons:
    GUI 图标、按钮、状态灯、marker/过滤图标、logo
```

主 agent 在派发前必须准备：

```text
docs/assets/asset-briefs/*.md
docs/assets/palette.md
docs/assets/acceptance.md
```

subagent 交付必须包含：

```text
src/main/resources/assets/appliedpackaging/...
docs/assets/reports/<package-name>.md
preview image 或 renderer/screenshot 记录
```

---

## 12. 开发实现顺序

阶段 0：仓库与设计

```text
git init
保留原始 docs baseline
补齐 design.md 工程规格
补齐 chat-summary.md 当前结论
记录外部版本来源
```

阶段 1：项目骨架

```text
从 NeoForgeMDKs/MDK-Forge-1.20.1-ModDevGradle 初始化
切换 mod_id/package/metadata
配置 Forge 47.4.10、AE2 15.4.10
建立注册、数据生成、GameTest run
确认 gradlew build 可跑
```

阶段 2：包裹核心

```text
17 色包裹物品
PackageDataStorage NBT adapter
canonical hash
capacity profile
tooltip
package filter
unit tests/GameTest
```

阶段 3：资源与基础玩法

```text
基础材质/模型/语言/创造标签
包裹样板与封装处理样板数据
基础 recipe/loot/datagen
```

阶段 4：ME 打包机

```text
方块/方块实体/菜单
相邻 item handler endpoint
打包事务
拆包事务
容量元件
红石触发
GameTest
```

阶段 5：装配室与 AE2 样板

```text
方块/方块实体/菜单
Pattern Provider 输入适配
普通处理样板 -> 包裹
彩色处理样板 -> 多包裹
封装处理样板 -> package plan
阻挡模式
GameTest/客户端验证
```

阶段 6：终端与总线

```text
包裹样板终端
包裹存储总线
包裹输出总线
包裹拆包总线
过滤 UI
AE2 网络集成
GameTest/服务器 smoke test
```

阶段 7：发布

```text
runData
build
runGameTestServer
runClient smoke
检查 jar、mods.toml、license、changelog
生成发布清单
tag 1.0.0
```

---

## 13. 测试与验收

### 13.1 自动化测试

普通 JVM 测试：

```text
PackageData canonical hash 稳定
同内容不同顺序 hash 相同
不同颜色 hash 不同
marker 冲突拒绝
容量单位计算正确
过滤 AND 规则正确
```

GameTest：

```text
合法包裹 NBT 可读写
无 PackageData 的包裹被拒绝
打包事务成功提交
输出槽满时打包不改变源库存
拆包目标容量不足时不消耗包裹
包裹堆叠拆包只接受完整数量
过滤不匹配拒绝
容量不足拒绝
```

手动/客户端验证：

```text
创造栏条目正确
17 色包裹图标可区分
Tooltip 每包/总计正确
机器 GUI 无错位
方块模型无 missing texture
语言文件英文/中文正常
```

Dedicated server 验证：

```text
服务端可启动
客户端类不被服务端加载
注册、recipe、datapack 加载无异常
```

### 13.2 发布验收标准

必须全部满足：

```text
git 工作树干净，发布 tag 可追溯
./gradlew.bat build 成功
./gradlew.bat runData 成功且生成资源已纳入 git
./gradlew.bat runGameTestServer 成功，或记录无法运行的明确阻塞
生成 build/libs/appliedpackaging-<version>.jar
jar 在 Minecraft 1.20.1 Forge + AE2 15.4.10 客户端中可进入游戏
核心玩法按本文 R1-R13 验收
docs/design.md、docs/chat-summary.md、docs/development-log.md 与实现一致
```

---

## 14. 文档与 Git 管理

分支策略：

```text
master/main：可构建主线
feature/*：较大功能分支，必要时使用
```

提交粒度：

```text
docs: 工程设计与开发记录
build: Gradle/模板/依赖
feat: 可运行功能
test: GameTest/JVM test
assets: 材质/模型/语言/数据资源
fix: 缺陷修复
```

推荐提交点：

```text
1. initial docs baseline
2. engineering design consolidation
3. ModDevGradle project scaffold
4. core package data
5. packager vertical slice
6. assembler vertical slice
7. terminal and buses
8. release assets and verification
```

`docs/development-log.md` 记录：

```text
日期
本次目标
关键决策
命令与结果
未解决问题
下一步
```

---

## 15. 开放风险

风险 A：AE2 1.20.1 API 对自定义 Pattern Provider/Molecular Assembler 风格集成的公开入口可能不足。  
应对：先用 AE2 API 实现可插入/可导出包裹的网络与存储端点，再逐步接入样板语义；必要时把装配室第一版做成可由 Pattern Provider 推入材料的普通 Forge inventory 机器。

风险 B：AE2 总线 Part API 可能需要内部类或较重集成。  
应对：先交付方块形态 Package Storage/Export/Unpacking Port，再在 API 确认可行后实现真正 cable part。发布前如果 Part API 阻塞，需在需求中明确改名和玩法等价性。

风险 C：GenericStack 覆盖非物品资源工作量较大。  
应对：数据模型完整支持 AEKey，1.0 垂直切片先验证物品，流体 adapter 在 Packager endpoint 完成后接入；对未知 key 保守拒绝拆包，避免吞资源。

风险 D：材质数量较多。  
应对：先做统一模板 + 17 色 palette 替换，之后再制作更精细模型；每批资源都生成预览并记录来源。

---

## 16. 最终定案

Applied Packaging 的 1.0 目标是一个可发布的 Minecraft 1.20.1 Forge + AE2 15.4.10 附属：

```text
包裹：
  17 色独立物品，无空壳，可堆叠，不真实嵌套。

ME 包裹装配室：
  处理 AE2 样板语义和彩色分包。

ME 打包机：
  处理相邻存储端点与包裹互转，可识别相邻 ME Interface 背后的存储子网。

包裹样板终端：
  负责包裹样板、彩色处理样板、封装处理样板的编辑、合成和拆分。

包裹总线家族：
  只允许包裹通过，负责包裹存储、包裹输出和事务拆包。
```

所有实现必须遵守两个边界：打包机不读样板，装配室不扫描相邻存储；总线不伪装包裹内部内容。这样玩家心智、AE2 自动合成语义和包裹物流语义才能保持清晰。

---

## 17. 参考来源

- NeoForgeMDKs `MDK-Forge-1.20.1-ModDevGradle`：用于 1.20.1 Forge + ModDevGradle Legacy 项目骨架。
- NeoForged ModDevGradle 文档：用于 MDG/LegacyForge 工具链判断。
- Forge 1.20.1 下载页：用于 Forge 47.4.10 recommended 与 47.4.20 latest 判断。
- Applied Energistics 2 官方下载页：用于 AE2 1.20.1 的 15.4.10 版本判断。
- Modrinth AE2 15.4.10 页面：用于确认 15.4.10 支持 Minecraft 1.20.1 Forge、client/server。
- AE2 1.20.1 Pattern Provider 指南：用于 Pattern Provider all-or-nothing、方向型 Provider 与子网推入语义。
- AE2 1.20.1 Storage Cells 指南：用于 16k/64k/256k 和 63 类型心智。

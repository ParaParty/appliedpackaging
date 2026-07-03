# Applied Packaging 开发日志

## 2026-07-02

目标：

```text
把现有讨论文档整理成可执行工程规格。
初始化 git 并保留原始文档基线。
核实 Minecraft 1.20.1 下 Forge/AE2/ModDevGradle 的版本方向。
明确材质阶段的 agent 协作规则归档到 AGENTS.md。
```

已完成：

```text
git init
提交原始 docs baseline：docs: add initial packaging design notes
重写 docs/design.md 为工程设计文档
在 docs/chat-summary.md 顶部添加当前工程结论
按文档类型拆分：
  docs/00-document-index.md
  docs/01-requirements.md
  docs/02-system-architecture.md
  docs/03-detailed-design.md
  docs/04-asset-spec.md
  docs/05-implementation-plan.md
  docs/06-verification-release.md
  docs/07-references.md
新增 AGENTS.md，集中维护 AI/agent 指令
确认目标项目身份：
  mod_id = appliedpackaging
  package = com.warmthdawn.appliedpackaging
  mod name = Applied Packaging
从 NeoForgeMDKs/MDK-Forge-1.20.1-ModDevGradle 初始化项目骨架
替换 MDK 示例源码为 Applied Packaging 主类、注册类、17 色包裹物品和基础样板物品
配置 Forge 47.4.10、AE2 15.4.10、GuideME 20.1.7
验证 .\gradlew.bat build 成功
验证 .\gradlew.bat runData 成功
实现 PackageData / PackageDataStorage / canonical hash / capacity calculator / tooltip builder
PackageDataStorage 只接受当前 schema version，且必须通过完整 canonical hash 校验
新增 PackageDataGameTests：
  packageDataRoundTrips
  emptyPackageIsInvalid
  tamperedHashIsRejected
  missingHashIsRejected
  unsupportedVersionIsRejected
新增 gameteststructures/empty.snbt，并由 copyGameTestStructures 在 runGameTestServer 前复制到 run/gameteststructures
验证 .\gradlew.bat runGameTestServer 成功，5 个必需 GameTest 全部通过
实现 PackageFilter：
  颜色、marker、requiredContents 三者 AND
  未设置条件忽略
  requiredContents 要求包裹内至少包含指定数量
  不实现 any/all/exact 模式切换
验证 .\gradlew.bat build 成功
验证 .\gradlew.bat runGameTestServer 成功，9 个必需 GameTest 全部通过
实现 PackagePlanBuilder / MarkerMergeMode / PackagePlanResult：
  sourcePackages 展开为虚拟内容，避免真实包裹嵌套
  retain/override/clear marker 策略
  capacity profile 计划阶段检查
  EMPTY_CONTENTS / INVALID_INPUT / MARKER_CONFLICT / CAPACITY_EXCEEDED 失败原因
验证 .\gradlew.bat build 成功
验证 .\gradlew.bat runGameTestServer 成功，14 个必需 GameTest 全部通过
建立 docs/assets 执行文档：
  palette.md
  acceptance.md
  asset-briefs/packages.md
  asset-briefs/machines.md
  asset-briefs/terminal-and-buses.md
  asset-briefs/ui-and-icons.md
  contracts/*.yaml
验证 5 个资产 contract 均通过 assetgen validate-contract
派发并整合 4 个材质 subagent 交付：
  packages
  machines
  terminal-and-buses
  ui-and-icons
主线程资产验收：
  5 个 asset contract 均 validate ok
  53 个 PNG 尺寸符合预期
  33 个 JSON 可解析
  block model 坐标保持在 0..16
  texture/model 引用存在
  抽样视觉检查通过
验证 .\gradlew.bat build 成功
验证 .\gradlew.bat runData 成功
验证 .\gradlew.bat runGameTestServer 成功，14 个必需 GameTest 全部通过
注册 me_packager 与 package_assembler 方块、方块物品和方块实体。
me_packager 当前基础玩法：
  内部输入/输出 item handler
  玩家右键放入合法包裹、取出输出、触发一次操作
  红石上升沿触发一次操作
  背面 Forge item handler 打包/拆包
  默认输出 Fluix 包裹
新增 item handler 事务 GameTest：
  itemHandlerPackPlanExtractsPackageContents
  itemHandlerUnpackInsertsAllContents
  itemHandlerUnpackRejectsFullTarget
  itemHandlerPackPlanRespectsDefaultCapacity
验证 .\gradlew.bat build 成功
验证 .\gradlew.bat runGameTestServer 成功，18 个必需 GameTest 全部通过
验证 .\gradlew.bat runData 成功
```

关键决策：

```text
目标平台先固定为 Minecraft 1.20.1 Forge。
工具链使用 NeoForgeMDKs/MDK-Forge-1.20.1-ModDevGradle 的 LegacyForge 模板。
Forge 编译基线优先使用 47.4.10 recommended，而不是更激进的 47.4.20 latest。
AE2 目标版本使用 15.4.10 Forge。
AE2 15.4.10 runtime 需要 GuideME 20.1.7；只加入 AE2 Modrinth 坐标时 runData 会缺少 guideme。
1.20.1 数据保存使用 ItemStack NBT；业务层通过 PackageDataStorage 抽象，为未来 Data Component 适配保留接口。
包裹 NBT 缺失 hash、hash 被篡改或 schema version 不匹配时一律视为 invalid。
GameTest 模板结构不由 Forge 自动从源码目录读取；项目保留 gameteststructures/empty.snbt，并在 prepareGameTestServerRun 前复制到 run/gameteststructures。
设计文档和 AI 指令分离；AI/agent 工作规则只维护在 AGENTS.md。
设计文档按需求、概要设计、详细设计、资产规格、实施计划、验证发布、参考来源分类维护。
材质生成阶段的 agent 协作细则只维护在 AGENTS.md。
```

外部来源：

```text
NeoForgeMDKs/MDK-Forge-1.20.1-ModDevGradle
NeoForged ModDevGradle 文档
Forge 1.20.1 下载页
Applied Energistics 2 官方下载页
Modrinth AE2 15.4.10 页面
AE2 1.20.1 Pattern Provider 指南
AE2 1.20.1 Storage Cells 指南
```

最新进展：

```text
新增基础配方：
  me_packager
  package_assembler
  package_pattern
  packaged_processing_pattern
新增 appliedpackaging:packages item tag。
新增 APMenus 与 ME Packager GUI：
  非潜行右键打开 GUI
  GUI 包含输入槽、输出槽、玩家背包和 Pack Once 图标按钮
  潜行右键保留快速交互
新增 Package Assembler 基础行为：
  9 格输入缓冲
  1 格输出槽
  自动将输入缓冲封装为默认 Fluix 包裹
  合法输入包裹会展开后再封装
  输出槽阻挡时不消耗输入
新增装配室 GameTest：
  packageAssemblerCreatesPackageFromInputBuffer
  packageAssemblerKeepsInputsWhenOutputBlocked
  packageAssemblerFlattensInputPackages
新增 AE2 方块总线家族：
  package_storage_bus
  package_export_bus
  package_unpacking_bus
当前总线实现为 AE2 可连接方块端点：
  AENetworkBlockEntity + IManagedGridNode
  package_storage_bus 挂载 IStorageProvider
  package_export_bus 从 AE 网络输出已有合法包裹
  package_unpacking_bus 先模拟完整拆包再提交
新增 PackageItemStorage GameTest：
  packageItemStorageExposesOnlyLegalPackages
  packageItemStorageRejectsLooseItemInsert
  packageItemStorageInsertsAndExtractsPackages
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runData 成功
验证 .\gradlew.bat runGameTestServer 成功，24 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
新增包裹样板终端基础功能：
  package_pattern_terminal 方块、方块物品、方块实体、菜单、客户端 screen
  9 格预览输入 + 空白 package_pattern 槽 + 输出槽
  Encode 按钮将预览输入写入已编码 package_pattern
  编码不消耗预览输入，只消耗 1 个未编码空白 package_pattern
  输出槽阻挡时不消耗空白样板
  已编码 package_pattern 不能作为空白样板被覆盖
  17 色 swatch 可选择 selectedColor，编码样板颜色跟随当前选择
新增 PackagePatternDataStorage：
  在 ItemStack NBT 写入 version、color、嵌套 PackageData
  读取时按样板颜色复验嵌套 PackageData canonical hash
Package Assembler 接入已编码 package_pattern：
  样板槽接受 package_pattern / packaged_processing_pattern
  已编码 package_pattern 精确匹配输入计划 canonical hash
  匹配成功后生成对应颜色包裹且不消耗样板
新增 PackagePatternItem tooltip：
  空白样板显示空白提示
  已编码样板显示包裹内容摘要
新增包裹样板终端 GameTest：
  packagePatternDataRoundTrips
  packagePatternTerminalEncodesInputPreview
  packagePatternTerminalEncodesSelectedColor
  packagePatternTerminalKeepsBlankWhenOutputBlocked
  packagePatternTerminalRejectsEncodedBlankPattern
  packageAssemblerUsesEncodedPackagePattern
按生产质量重做材质：
  clone AppliedEnergistics/Applied-Energistics-2 forge/v15.4.10 到 build/reference/ae2
  生成 AE2 item/machine/part/gui reference sheet 到 build/asset-reference/ae2
  使用 ImageGen 基于 AE2 reference sheet 生成 Applied Packaging 风格概念板
  4 个 subagent 分别重做 packages、machines、terminal-and-buses、ui-and-icons
  最终资源不复制 AE2 像素，只参考石英面板、深灰框架、Fluix 高光和 GUI 语言
验证 53 个 PNG 尺寸/模式/模型引用全部通过
验证 .\gradlew.bat runData 成功
验证 .\gradlew.bat build 成功
验证 .\gradlew.bat runGameTestServer 成功，30 个必需 GameTest 全部通过
验证 .\gradlew.bat runClient 已进入客户端主流程：
  Applied Packaging 初始化完成
  ResourceManager 重载完成
  OpenAL/SoundEngine 启动
  block atlas 创建完成
  未发现 appliedpackaging 相关 missing model/texture、客户端类加载异常或崩溃
验证 .\gradlew.bat runServer 已进入专用服务端启动流程：
  服务端按 Mojang EULA 要求在 run/eula.txt 未同意时停止
  停止前未发现 Applied Packaging 客户端类误加载、注册崩溃或 mod 扫描异常
```

最新进展：

```text
补齐 ME Packager 第一版配置层：
  新增容量槽，识别 AE2 16k/64k/256k storage component、item/fluid storage cell 与 portable cell
  新增过滤槽，接受已编码 package_pattern、packaged_processing_pattern 或合法包裹作为过滤模板
  新增 GUI 17 色 swatch，selectedColor 控制无过滤模板时的输出包裹颜色
  打包时过滤模板提供输出颜色、marker override 和 requiredContents 内容过滤
  拆包时输入包裹必须匹配过滤模板，不匹配则不消耗包裹
  shift-click 会按输入、容量、过滤槽类型分流
新增 GameTest：
  itemHandlerPackPlanUsesContentFilter
  itemHandlerPackPlanRejectsMissingFilteredContent
  itemHandlerPackPlanOverridesMarkerFromFilter
  itemHandlerPackPlanUsesLargerCapacityProfile
  packageFilterReadsEncodedPatternTemplate
  mePackagerRecognizesAe2CapacityItems
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，36 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
补齐 ME Packager 的 AE2 MEStorage endpoint：
  打包机背面优先识别 appeng.capabilities.Capabilities.STORAGE
  可接入相邻 AE2 Interface/ME storage 暴露的子网存储
  无 AE2 storage 时回落到 Forge item handler
  MEStorage 打包计划直接处理 AEKey/GenericStack
  MEStorage 中已有合法包裹会展开后再封装
  MEStorage 拆包先模拟完整插入，成功后再消耗输入包裹
新增 MEStoragePackageTransactions 与 MEStoragePackagePlan
新增 GameTest：
  meStoragePackPlanExtractsGenericContents
  meStorageUnpackInsertsAllContents
  meStoragePackPlanFlattensSourcePackages
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，39 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
补齐 Package Assembler GUI/Menu：
  package_assembler 右键打开 GUI
  GUI 显示 9 格输入缓冲、样板槽、输出槽和玩家背包
  shift-click 会把 package_pattern / packaged_processing_pattern 优先放入样板槽
  其它物品 shift-click 进入 9 格输入缓冲
  输出槽禁止玩家放入物品
新增 PackageAssemblerMenu 与 PackageAssemblerScreen
注册 APMenus.PACKAGE_ASSEMBLER 与客户端 MenuScreens
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，39 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
补齐 ME Packager 独立 marker 策略 UI：
  新增 marker 槽，接受非包裹、非样板物品作为 override marker key
  新增 retain/override/clear 策略状态，并写入方块实体 NBT
  ME Packager GUI 新增 marker 策略图标按钮，使用 marker_retain/marker_override/marker_clear 图标
  retain 保留源包裹 marker，冲突由 PackagePlanBuilder 拒绝
  override 优先使用 marker 槽物品；marker 槽为空时兼容回退到过滤模板 marker
  clear 生成无 marker 的输出包裹
  item handler 与 AE2 MEStorage 打包事务均新增显式 marker 策略入口
新增 GameTest：
  itemHandlerPackPlanRetainsMarkerFromExplicitMode
  itemHandlerPackPlanOverridesMarkerFromExplicitMode
  itemHandlerPackPlanClearsMarkerFromExplicitMode
  meStoragePackPlanClearsMarkerFromExplicitMode
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，43 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
补齐 ME Packager Forge fluid handler endpoint：
  新增 FluidPackageTransactions 与 FluidPackagePlan
  新增 SimulatedFluidHandler，用于拆包前累计模拟多项流体插入
  ME Packager 背面无 AE2 MEStorage 时同时识别 Forge item handler 与 fluid handler
  输入包裹只含物品时拆入 item handler，只含流体时拆入 fluid handler
  没有 MEStorage 时，混合物品+流体包裹保守拒绝拆入单一 Forge endpoint
  打包时物品 endpoint 优先；物品无可打包内容时可从相邻 fluid handler 打包 AEFluidKey
新增 GameTest：
  fluidHandlerPackPlanExtractsFluidContents
  fluidHandlerUnpackInsertsAllContents
  fluidHandlerUnpackRejectsFullTarget
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，46 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
补齐 Package Pattern Terminal marker/capacity 编码能力：
  终端方块实体新增容量槽和 marker 槽
  编码 package_pattern 时容量槽使用 ME Packager 的 AE2 16k/64k/256k 映射
  marker 槽物品写入样板 PackageData marker，编码时不消耗 marker 槽
  容量槽编码时不消耗容量元件
  GUI 高度扩展到 188，新增容量槽、marker 槽并下移玩家背包
  shift-click 会把 AE2 容量元件送入容量槽；marker 槽保持手动放入，避免普通预览物品误分流
新增 GameTest：
  packagePatternTerminalEncodesMarkerSlot
  packagePatternTerminalUsesCapacitySlot
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，48 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
补齐 packaged_processing_pattern 基础编码路径：
  Package Pattern Terminal 空白样板槽现在接受未编码 package_pattern 或 packaged_processing_pattern
  编码输出会保留空白样板的物品类型
  packaged_processing_pattern 当前复用 PackagePatternDataStorage，先支持单包裹 PackageData 编码
  shift-click 会把两类可存储样板送入空白样板槽
新增 GameTest：
  packagePatternTerminalEncodesPackagedProcessingPattern
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，49 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
补齐封装处理样板多包裹可用路径：
  新增 PackagedProcessingPatternDataStorage，写入 version、color、packages[]，并逐个复验 PackageData canonical hash
  packaged_processing_pattern 兼容旧的单包裹 PackagePatternDataStorage 读取
  Package Pattern Terminal 编码 packaged_processing_pattern 时会按容量档生成有序多包裹计划
  Package Assembler 可读取 packaged_processing_pattern，并在输出槽为空时逐包生成匹配包裹
  已编码 packaged_processing_pattern 不会被终端当空白样板覆盖
  tooltip 增加封装处理样板包裹数量和首包内容预览
新增 GameTest：
  packagedProcessingPatternDataRoundTrips
  packagePatternTerminalSplitsPackagedProcessingPattern
  packageAssemblerUsesPackagedProcessingPattern
  packagePatternTerminalRejectsEncodedProcessingBlankPattern
验证 .\gradlew.bat compileJava --rerun-tasks 成功
验证 .\gradlew.bat runGameTestServer 成功，53 个必需 GameTest 全部通过
设计约束更新：
  功能优先完成；材质与 AE2 风格面板/part 外形后置
  后续应评估扩展 AE2 原版 blank/encoded pattern 作为样板承载，避免继续新增样板物品
```

最新进展：

```text
补齐 Package Assembler 与 AE2 Pattern Provider 的基础可用集成：
  Package Assembler 实现 AE2 ICraftingMachine
  方块实体暴露 appeng.capabilities.Capabilities.CRAFTING_MACHINE
  acceptsPlans 在输入缓冲为空且输出槽为空时接受 Pattern Provider 计划
  pushPattern 将 item-only KeyCounter 输入转换为本机 9 格输入缓冲并复用装配计划逻辑
  成功装配后才从 KeyCounter 扣减输入，失败路径保持 all-or-nothing
  输出阻挡、输入缓冲非空、fluid/non-item AEKey、规划失败或提交失败时整批拒绝
新增 GameTest：
  packageAssemblerAcceptsPatternProviderPush
  packageAssemblerRejectsPatternProviderPushWhenOutputBlocked
  packageAssemblerRejectsFluidPatternProviderPush
修复：
  planAssembly 现在使用传入的 IItemHandler 规划，避免 pushPattern 的临时输入被成员 inputView 覆盖
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，56 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
补齐总线最小可用过滤：
  PackageItemStorage 新增 PackageFilter，可限制可见、插入、抽取的包裹
  package_storage_bus 挂载 PackageItemStorage 时传入当前总线过滤模板
  package_export_bus 从 AE 网络拉取包裹前按当前过滤模板筛选
  package_unpacking_bus 从 AE 网络拉取并拆包前按当前过滤模板筛选
  总线方块支持手持已编码 package_pattern、packaged_processing_pattern 或合法包裹右键设置 ghost 过滤模板
  潜行空手右键可清除 ghost 过滤模板
  过滤模板写入 AE2 AENetworkBlockEntity 的 loadTag/saveAdditional 生命周期，不作为实体库存掉落
新增 GameTest：
  packageItemStorageAppliesFilter
  packageBusStoresFilterTemplate
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，58 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
补齐真实 AE2 Pattern Provider 方块到 Package Assembler 的端到端 push smoke：
  新增 gameteststructures/ae_network_column.snbt，用于放置 AE2 Creative Energy Cell、Pattern Provider 和 Package Assembler
  GameTest 内构建真实 AE2 方块网络，等待 grid 初始化后写入 processing pattern
  通过 PatternProviderBlockEntity.getLogic().pushPattern 走 AE2 PatternProviderLogic 的真实相邻 ICraftingMachine 探测路径
  Package Assembler 接收 Pattern Provider 推入的 iron/copper KeyCounter 并生成包裹
新增 GameTest：
  ae2PatternProviderPushesIntoPackageAssembler
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，59 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
修正 Package Assembler 执行已编码 package_pattern 的规划路径：
  普通已编码 package_pattern 现在直接使用 ItemPackageTransactions.planExactPackage
  没有本地样板时仍使用默认 Fluix 自由打包
  这避免大于默认容量的已编码源包裹被默认容量规划提前挡掉
新增 GameTest：
  packageAssemblerUsesLargeEncodedPackagePattern
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，60 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
归档 AE2 源码材质作为 Applied Packaging 后续分割参考：
  来源：AppliedEnergistics/Applied-Energistics-2 forge/v15.4.10
  commit：b4b08d9941e3faecb520d76be617629bb56661e1
  源路径：src/main/resources/assets/ae2/textures
  目标：E:\resources\textures\appliedpackaging\ae2-forge-v15.4.10
  保留 raw PNG 源码目录结构到 textures/
  同步已有 AE2 reference sheets 到 reference-sheets/
  生成 manifest.csv 与 README.md
验证：
  textures/ 下 PNG 数量 626
  manifest.csv 条目 626
  reference-sheets/ 文件 9
  分类计数：block 221、item 133、part 225、guis 38、gui 1、entity 2、guide 2、particle 2、patchouli 2
未运行 Gradle/GameTest：本次只归档外部参考材质，未修改代码、数据生成或发布资源。
```

最新进展：

```text
补齐 AE2 原版处理样板的彩色服务器端执行路径：
  新增 ColoredProcessingPatternDataStorage，颜色元数据写入 AE2 encoded processing pattern 的 appliedpackaging.colored_processing_pattern NBT
  颜色元数据按 AE2 processing pattern sparse input 槽位保存，未标色槽位默认 Fluix
  Package Assembler pushPattern 检测到彩色元数据时，直接从 pattern definition 读取 sparse inputs
  同 AEKey 位于不同颜色槽时，即使 Pattern Provider 输入持有者已按 AEKey 汇总，也会按 sparse 槽位拆成不同颜色包裹
  彩色 pushPattern 一次产生多个包裹时，先输出第一个，剩余包裹进入 pending queue
  pending queue 写入方块实体 NBT，输出槽清空后由 tryAssemble/server tick 继续吐包
  破坏装配室时，pending queue 中的包裹按合法包裹物品掉落
新增 GameTest：
  coloredProcessingPatternDataRoundTrips
  packageAssemblerSplitsColoredProcessingPatternPush
  ae2PatternProviderPushesColoredProcessingPatternIntoPackageAssembler
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，63 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
归档 AE2 1.21+ 源码材质作为 Applied Packaging 后续分割参考：
  来源：AppliedEnergistics/Applied-Energistics-2 neoforge/v19.2.17
  Minecraft version：1.21.1
  NeoForge version：21.1.169
  commit：79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a
  源路径：src/main/resources/assets/ae2/textures
  目标：E:\resources\textures\appliedpackaging\ae2-mc1.21-plus-neoforge-v19.2.17
  保留 raw PNG 源码目录结构到 textures/
  生成 manifest.csv、README.md 与 source-gradle.properties
验证：
  textures/ 下 PNG 数量 614
  manifest.csv 条目 614
  源/目标哈希不一致 0
  PNG 头校验失败 0
  分类计数：block 207、item 130、part 222、guis 38、gui 11、entity 2、particle 2、patchouli 2
未运行 Gradle/GameTest：本次只归档外部参考材质，未修改代码、数据生成或发布资源。
```

最新进展：

```text
补齐 Package Pattern Terminal 对 AE2 原版处理样板的基础彩色编辑/编码入口：
  样板槽现在可接受 AE2 encoded processing pattern
  终端保存并同步 9 个输入槽颜色，客户端 screen 在输入槽角落提供小色标按钮
  玩家选择 17 色 swatch 后点击输入槽色标，可把该槽设为当前颜色
  编码 AE2 processing pattern 时，终端复制 1 个输入样板到输出槽，并写入 appliedpackaging.colored_processing_pattern NBT
  未逐槽设色时，终端会把 selectedColor 写入该 AE2 processing pattern 的全部非空 sparse input slot
  已逐槽设色时，终端只写入已配置槽位颜色；未配置槽由装配室按 Fluix 默认处理
  输出槽阻挡时不消耗源 AE2 processing pattern
新增 GameTest：
  packagePatternTerminalEncodesSelectedColorOntoAe2ProcessingPattern
  packagePatternTerminalEncodesPerSlotColorsOntoAe2ProcessingPattern
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，65 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
补齐 Package Assembler 容量槽与大输入 Pattern Provider 路径：
  Package Assembler 新增容量槽，使用与 ME Packager 相同的 AE2 16k/64k/256k 映射
  GUI 高度扩展到 188，新增容量槽并下移玩家背包
  shift-click 会把 AE2 容量元件送入容量槽
  本地自由封装和已编码彩色处理样板 pushPattern 均读取容量槽
  空样板槽的普通 Pattern Provider pushPattern 直接从 KeyCounter 生成包裹计划，避免 9 格临时输入缓存限制
  默认容量不足时仍整批拒绝，不消耗 Pattern Provider 输入
  装配室加载旧 11 槽 NBT 时迁移到当前 12 槽库存，补空容量槽
新增 GameTest：
  packageAssemblerUsesCapacitySlotForLargeSourcePackage
  packageAssemblerPatternProviderPushUsesCapacitySlot
  packageAssemblerRejectsOversizedPatternProviderPushWithoutCapacity
  packageAssemblerColoredPatternProviderPushUsesCapacitySlot
  packageAssemblerLoadsLegacyElevenSlotInventory
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，70 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
补齐真实 AE2 crafting CPU 自动合成 job smoke：
  新增 ae2CraftingCpuJobPushesIntoPackageAssembler GameTest
  测试内构建真实 AE2 Creative Energy Cell、Drive、64k item cell、Crafting Storage、Pattern Provider 和 Package Assembler 网络
  通过 AE2 ICraftingService.beginCraftingCalculation 计算 diamond processing pattern job
  通过 AE2 ICraftingService.submitJob 提交到真实 Crafting CPU
  Crafting CPU 从 AE 网络库存抽取 iron/copper，并经 Pattern Provider 推送到 Package Assembler
  Package Assembler 输出包含 iron/copper 的 Fluix 包裹
  AE2 CraftingService 进入等待 diamond 输出的真实 processing pattern 状态
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，71 个必需 GameTest 全部通过
```

最新进展：

```text
补齐 Package Pattern Terminal Split 操作与输入槽颜色清除：
  新增 Split 按钮，把已编码 packaged_processing_pattern 拆回普通 package_pattern
  Split 输出槽逐张吐出拆分结果，剩余结果写入 pending queue
  pending queue 写入终端 NBT，保存/读取后可继续输出
  输入槽角落色标左键设置当前颜色，右键清除该槽颜色
  样板槽允许已编码 packaged_processing_pattern 作为 Split 来源，但 encode 仍拒绝覆盖已编码样板
新增 GameTest：
  packagePatternTerminalSplitButtonConvertsPackagedProcessingPattern
  packagePatternTerminalSplitQueuePersists
  packagePatternTerminalClearsInputSlotColor
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，74 个必需 GameTest 全部通过
```

最新进展：

```text
补跑客户端启动 smoke：
  执行 .\gradlew.bat runClient
  客户端启动到 Applied Packaging 初始化、ResourceManager 重载、SoundEngine 启动和 block atlas 创建阶段
  run/logs/latest.log 未发现 ERROR、FATAL、Missing model 或 Unable to load model
  smoke 在 atlas 创建完成后手动 Ctrl+C 中断；退出码来自人工终止
  已观察到 me_packager_preview_sheet 68x68 mip level 降级警告，后续资源整理时可改为 64x64 或 128x128
```

最新进展：

```text
清理客户端 block atlas 发布噪音：
  将 me_packager_preview_sheet.png 和 package_assembler_preview_sheet.png 从 src/main/resources/assets/.../textures/block 移到 docs/assets/previews
  preview sheet 保留为文档审查资产，不再随 mod 资源包进入 Minecraft block atlas
  更新 docs/assets/reports/machines.md 中的预览图路径说明
验证 .\gradlew.bat build 成功
再次执行 .\gradlew.bat runClient，客户端启动到 Applied Packaging 初始化、SoundEngine 启动和 block atlas 创建阶段
验证 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、preview_sheet 或 mip level
smoke 在 atlas 创建完成后手动终止客户端；退出码来自人工终止
```

最新进展：

```text
补齐 Package Bus ghost filter 配置 UI：
  AbstractPackageBusBlockEntity 实现 MenuProvider
  package_storage_bus、package_export_bus、package_unpacking_bus 共用 PackageBusMenu / PackageBusScreen
  普通空手右键打开配置 UI；手持有效模板右键快速设置、潜行空手清除的旧路径保留
  UI 显示当前 ghost filter 模板，支持从光标物品复制模板、清除模板
  shift-click 玩家背包中的已编码 package_pattern、packaged_processing_pattern 或合法包裹可设置 ghost filter
  UI 设置与 shift-click 都不消耗玩家模板物品
新增 GameTest：
  packageBusMenuSetsFilterFromCursor
  packageBusMenuShiftClickSetsGhostFilter
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，76 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
再次执行 .\gradlew.bat runClient，客户端启动到 Applied Packaging 初始化、SoundEngine 启动和 block atlas 创建阶段
验证 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、preview_sheet 或 mip level
smoke 在 atlas 创建完成后手动终止客户端；退出码来自人工终止
```

最新进展：

```text
补齐 packaged_processing_pattern 基础处理输出 ghost UI：
  PackagedProcessingPatternDataStorage 升级到 version 2，新增可选 outputs[]
  旧 write(color, packages) 入口保留，旧 version 1 数据可继续读取为无 outputs
  Package Pattern Terminal 新增 3 个物品处理输出 ghost slots
  左键 ghost slot 复制光标物品与数量，右键复制 1 个，空光标点击清除
  ghost 输出只写入终端方块实体与样板 NBT，不进入 Forge item handler，不消耗也不掉落玩家物品
  packaged_processing_pattern tooltip 会显示处理输出
新增 GameTest：
  packagedProcessingPatternOutputsRoundTrip
  packagePatternTerminalMenuEncodesProcessingOutputGhost
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，78 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
再次执行 .\gradlew.bat runClient，客户端启动到 Applied Packaging 初始化、SoundEngine 启动和 block atlas 创建阶段
验证 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、preview_sheet 或 mip level
smoke 在 atlas 创建完成后手动终止客户端；退出码来自人工终止
```

最新进展：

```text
补齐 Package Pattern Terminal AE2 风格薄面板外形：
  package_pattern_terminal block model 从完整方块改为 14x14x3 前面板 + 8x8x4 后接头
  PackagePatternTerminalBlock 提供按 FACING 旋转的薄面板 VoxelShape
  保留现有方块实体、菜单和 screen；真正 AE2 cable part 形态后置
新增 GameTest：
  packagePatternTerminalUsesPanelShape
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，79 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
再次执行 .\gradlew.bat runClient，客户端启动到 Applied Packaging 初始化、SoundEngine 启动、block atlas 创建，并进入本地世界
验证 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、preview_sheet 或 mip level
smoke 在进入世界后手动终止客户端；退出码来自人工终止
```

最新进展：

```text
补齐 Package Bus 手工过滤器编辑：
  PackageFilter 新增 NBT read/write，并合并重复 required content
  Package Bus 方块实体新增直接保存的 PackageFilter NBT，保留旧 filter_template 兼容读取
  Package Bus 配置 UI 新增 17 色 swatch、marker ghost 槽和 3 个 required content ghost slots
  ghost 编辑从光标复制物品/数量或右键复制 1 个，不消耗玩家物品；空光标点击清除
新增 GameTest：
  packageBusMenuEditsManualFilter
  packageBusManualFilterPersists
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，81 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
再次执行 .\gradlew.bat runClient，客户端启动到 Applied Packaging 初始化、SoundEngine 启动和 block atlas 创建阶段
验证 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、preview_sheet 或 mip level
smoke 在 atlas 创建完成后手动终止客户端；退出码来自人工终止
```

最新进展：

```text
补齐 AE2 原版 blank_pattern 作为 package_pattern / packaged_processing_pattern 载体的可用路径：
  PackagePatternDataStorage 识别 ae2:blank_pattern，并允许写入/读取 package_pattern NBT
  PackagedProcessingPatternDataStorage 识别 ae2:blank_pattern，并允许写入/读取 packaged_processing_pattern NBT
  Package Pattern Terminal 可把 AE2 blank_pattern 编码为封装样板，输出保留 AE2 物品类型
  AE2 blank_pattern 在单包裹且无处理输出时写入 package_pattern NBT；存在处理输出 ghost 或多包裹计划时写入 packaged_processing_pattern NBT
  Package Assembler 样板槽与 shift-click 统一使用样板载体判断，可读取 AE2 blank_pattern 承载的 package_pattern NBT
  Package Assembler 可读取 AE2 blank_pattern 承载的 packaged_processing_pattern NBT，并逐包输出
  已编码 AE2 blank_pattern 在客户端通过 tooltip event 复用 PackagePatternItem tooltip，显示 package_pattern 或 packaged_processing_pattern 内容
  本地 package_pattern / packaged_processing_pattern 保持兼容；AE2 encoded pattern/Planner 深集成仍后置
新增 GameTest：
  packagePatternDataRoundTripsOnAe2BlankPattern
  packagePatternTerminalEncodesAe2BlankPatternCarrier
  packageAssemblerUsesAe2BlankPatternCarrier
  packagedProcessingPatternDataRoundTripsOnAe2BlankPattern
  packagePatternTerminalEncodesAe2BlankPatternAsPackagedProcessing
  packageAssemblerUsesAe2PackagedProcessingCarrier
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，87 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功，生成 build/libs/appliedpackaging-0.1.0-dev.jar
再次执行 .\gradlew.bat runClient，客户端启动到 Applied Packaging 初始化、SoundEngine 启动和 block atlas 创建阶段
验证 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、preview_sheet 或 mip level
smoke 在 atlas 创建完成后手动终止客户端；退出码来自人工终止
```

最新进展：

```text
补齐 AE2 encoded processing pattern 作为封装处理样板载体的可用路径：
  Package Pattern Terminal 在 AE2 blank_pattern 有处理输出 ghost 时输出 AE2 原版 encoded processing pattern
  encoded processing pattern 同时保存 AE2 原版 inputs/outputs 和 appliedpackaging.packaged_processing_pattern NBT
  PackagedProcessingPatternDataStorage 可写入 AE2 encoded processing pattern，并且只把已带本 mod NBT 的 encoded pattern 视作封装处理载体
  Package Assembler pushPattern 优先识别 packaged_processing_pattern NBT
  Pattern Provider 推入带 packaged_processing_pattern NBT 的 AE2 encoded processing pattern 时，装配室按 packages[] 输出一个或多个包裹
  item-only 封装处理推送保持 all-or-nothing：包裹内容非物品、输入不足或存在额外输入时整批拒绝
新增 GameTest：
  packageAssemblerAcceptsAe2EncodedPackagedProcessingPush
  ae2PatternProviderPushesPackagedProcessingPatternIntoPackageAssembler
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，89 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功，生成 build/libs/appliedpackaging-0.1.0-dev.jar
再次执行 .\gradlew.bat runClient，客户端启动到 Applied Packaging 初始化、SoundEngine 启动和 block atlas 创建阶段
验证 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture、preview_sheet 或 mip level
smoke 在 atlas 创建完成后手动终止客户端；退出码来自人工终止
```

最新进展：

```text
收敛样板玩家入口到 AE2 原版 blank_pattern：
  本地 package_pattern / packaged_processing_pattern 保留注册和读取兼容，但不再在创造栏显示
  删除 package_pattern 与 packaged_processing_pattern 普通合成配方
  package_assembler、package_pattern_terminal、package_storage_bus、package_export_bus、package_unpacking_bus 配方改用 ae2:blank_pattern
  Applied Packaging 创造栏图标改为 Fluix Package，避免以本地样板作为主入口
新增 GameTest：
  playerRecipesUseAe2BlankPatterns
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，90 个必需 GameTest 全部通过
验证 .\gradlew.bat runData 成功
验证 .\gradlew.bat build 成功，生成 build/libs/appliedpackaging-0.1.0-dev.jar
再次执行 .\gradlew.bat runClient，客户端启动到 Applied Packaging 初始化、SoundEngine 启动和 block atlas 创建阶段
验证 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture、preview_sheet 或 mip level
smoke 在 atlas 创建完成后手动终止客户端；退出码来自人工终止
```

下一步：

```text
补齐彩色 AE2 processing pattern 更完整的输出 UI、封装处理样板流体/任意 AEKey 输出 ghost editor、批量/流体/任意 AEKey 高级过滤器编辑器和 AE2 part 形态。
用户显式同意 EULA 后重新运行 .\gradlew.bat runServer，完成专用服务端完整启动验收。
```

最新进展：

```text
补齐发布交付物：
  README.md 扩展为面向玩家/整合包作者/开发者的发布说明，包含版本兼容、核心功能、安装、玩法流程、验证状态和已知限制
  新增 CHANGELOG.md，记录 0.1.0-dev 初始可发布开发版本、功能、变更、验证和已知限制
  新增 LICENSE.md，按当前设计约定提供 All Rights Reserved 许可声明
  更新 docs/06-verification-release.md，将发布清单从待准备项改为当前状态记录
本次仅变更发布文档与许可声明，未改动玩法逻辑；GameTest 已按规则考虑，未新增行为测试。
验证 .\gradlew.bat build 成功，资源模板和发布 jar 生成链路仍可用。
```

最新进展：

```text
补齐可重复客户端 GUI screenshot smoke：
  新增 runClientSmoke Gradle run，默认 --quickPlaySingleplayer "New World"
  可通过 -Pappliedpackaging.clientSmoke.world="世界名" 覆盖 quick-play 世界
  新增 ClientSmokeRunner，仅在 appliedpackaging.clientSmoke.enabled=true 时注册
  smoke 进入单人世界后自动摆放 Package Assembler、ME Packager、Package Pattern Terminal、Package Storage Bus
  smoke 通过真实 ServerPlayer + NetworkHooks.openScreen 打开对应菜单，并使用 Minecraft Screenshot 保存画面
  smoke 完成后按 appliedpackaging.clientSmoke.quit=true 自动退出客户端
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runClientSmoke 成功，生成 4 张截图：
  run/screenshots/appliedpackaging-client-smoke-package_assembler.png
  run/screenshots/appliedpackaging-client-smoke-me_packager.png
  run/screenshots/appliedpackaging-client-smoke-package_pattern_terminal.png
  run/screenshots/appliedpackaging-client-smoke-package_storage_bus.png
人工查看 4 张截图，确认均为真实 Minecraft 客户端菜单画面
验证 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture 或客户端 smoke timeout
本次新增客户端验证工具和 Gradle run；GameTest 已按规则考虑，未新增行为 GameTest
验证 .\gradlew.bat build 成功
验证 .\gradlew.bat runGameTestServer 成功，90 个必需 GameTest 全部通过
验证 .\gradlew.bat runServer 成功到达 EULA gate，未出现 ClientSmokeRunner 或其他客户端类误加载；完整 dedicated server world-load 仍需用户显式同意 EULA 后执行
```

最新进展：

```text
补齐 Package Assembler 输出自动导出：
  PackageAssemblerBlockEntity 新增 autoExport 设置，默认开启，保存到 NBT
  装配室 server tick 会先尝试导出现有输出，再执行装配，装配成功后再次尝试导出
  输出导出端点为机器背面，优先 AE2 MEStorage capability，其次 Forge item handler
  AE2 与 item handler 导出均先模拟可接收数量；实际成功插入多少，才从输出槽扣除多少
  目标不可用或容量不足时保留输出槽包裹，不丢弃、不继续消耗新输入
  Package Assembler GUI 新增 auto_export 图标按钮，DataSlot 同步当前开关状态
新增 4 个 GameTest：
  packageAssemblerMenuTogglesAutoExport
  packageAssemblerAutoExportSettingPersists
  packageAssemblerAutoExportsToAdjacentItemHandler
  packageAssemblerAutoExportsToAe2Interface
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，101 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功，生成 build/libs/appliedpackaging-0.1.0-dev.jar
验证 .\gradlew.bat runData 成功，未写出新的 generated resources 内容
验证 .\gradlew.bat runClientSmoke 成功，生成并人工查看 6 张真实菜单截图；Package Assembler 自动导出按钮显示正常且未遮挡槽位
验证 runClientSmoke 后 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture、client smoke timeout、Timed out 或 Exception
验证 .\gradlew.bat runServer 成功到达 EULA gate，未出现客户端类误加载；完整 dedicated server world-load 仍需用户显式同意 EULA 后执行
```

最新进展：

```text
补齐包裹 canonical hash / NBT 堆叠稳定性：
  PackageData normalize 现在合并同 AEKey 后按 canonical stack key 排序 contents
  同内容不同输入顺序会写入相同 package NBT，确保 ItemStack 可自然堆叠
  颜色、marker 或内容不同仍会产生不同 canonical hash，避免误堆叠
新增 2 个 GameTest：
  packageDataCanonicalOrderStacksEquivalentContents
  packageDataCanonicalHashSeparatesIdentity
首次 runGameTestServer 发现旧 itemHandlerUnpackInsertsAllContents 依赖槽位顺序；已改为按目标 handler 总量断言完整拆包
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，97 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
发布侧复核：
  在 canonical contents 修复后重新执行 runData、runClientSmoke 与 runServer smoke
验证 .\gradlew.bat runData 成功，未写出新的 generated resources 内容
验证 .\gradlew.bat runClientSmoke 成功，生成并人工查看 6 张真实菜单截图：
  run/screenshots/appliedpackaging-client-smoke-package_assembler.png
  run/screenshots/appliedpackaging-client-smoke-me_packager.png
  run/screenshots/appliedpackaging-client-smoke-package_pattern_terminal.png
  run/screenshots/appliedpackaging-client-smoke-package_storage_bus.png
  run/screenshots/appliedpackaging-client-smoke-package_export_bus.png
  run/screenshots/appliedpackaging-client-smoke-package_unpacking_bus.png
验证 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture、client smoke timeout、Timed out 或 Exception
验证 .\gradlew.bat runServer 成功到达 EULA gate，未出现客户端类误加载；完整 dedicated server world-load 仍需用户显式同意 EULA 后执行
```

最新进展：

```text
补齐 ME Packager 真实 Forge fluid handler 世界内 smoke：
  新增 mePackagerPackagesAndUnpacksThroughWorldFluidHandler GameTest
  测试在世界内放置带 Forge FLUID_HANDLER capability 的临时 tank 方块实体与 ME Packager
  ME Packager 从相邻 fluid handler 打包 2000 mB water，验证源槽被抽空
  再把输出包裹放回输入槽，ME Packager 整包拆回相邻 fluid handler
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，95 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
补齐 ME Packager 周期红石模式：
  MePackagerBlockEntity 新增 RedstoneMode：disabled/pulse/cyclic
  默认保持 pulse，上升沿触发一次，兼容旧行为
  cyclic 模式在持续供电时每 20 tick 尝试一次 pack/unpack
  MePackagerBlock 接入服务端 ticker，周期模式可在真实世界内运行
  ME Packager GUI 新增红石模式图标按钮，使用 minecraft redstone 图标和模式标记
  红石模式保存到 NBT，并通过 menu DataSlot 同步到客户端
  en_us/zh_cn 补齐红石模式 tooltip 文案
新增 3 个 GameTest：
  mePackagerMenuCyclesRedstoneMode
  mePackagerPulseRedstoneRunsOnce
  mePackagerCyclicRedstoneRepeatsWhilePowered
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，93 个必需 GameTest 全部通过
验证 .\gradlew.bat runClientSmoke 成功，6 张真实菜单截图均生成；人工查看 ME Packager 截图，红石模式按钮正常显示且未挤压布局
验证 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture 或客户端 smoke timeout
验证 .\gradlew.bat build 成功
验证 .\gradlew.bat runServer 成功到达 EULA gate，完整 dedicated server world-load 仍需用户显式同意 EULA 后执行
```

最新进展：

```text
补齐 ME Packager 真实 AE2 Interface 世界内 smoke：
  新增 mePackagerPackagesAndUnpacksThroughAe2Interface GameTest
  测试摆放 AE2 Creative Energy Cell、Drive、Interface 与 ME Packager
  Drive 插入 AE2 64k item cell，通过 Interface 所在真实 grid storage 注入 iron/copper
  ME Packager 从相邻 Interface 的 MEStorage capability 打包，验证 AE2 网络内容被抽走
  再把输出包裹放回输入槽，ME Packager 整包拆回相邻 Interface 网络
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，94 个必需 GameTest 全部通过
验证 .\gradlew.bat build 成功
```

最新进展：

```text
扩展客户端 GUI screenshot smoke：
  runClientSmoke 现在同时摆放并打开 Package Storage Bus、Package Export Bus、Package Unpacking Bus
  客户端 smoke 覆盖 Package Assembler、ME Packager、Package Pattern Terminal 和三种 Package Bus 真实菜单
  docs/05 中 Package Assembler 客户端验证待办已按 runClientSmoke 当前覆盖状态校准
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runClientSmoke 成功，生成 6 张截图：
  run/screenshots/appliedpackaging-client-smoke-package_assembler.png
  run/screenshots/appliedpackaging-client-smoke-me_packager.png
  run/screenshots/appliedpackaging-client-smoke-package_pattern_terminal.png
  run/screenshots/appliedpackaging-client-smoke-package_storage_bus.png
  run/screenshots/appliedpackaging-client-smoke-package_export_bus.png
  run/screenshots/appliedpackaging-client-smoke-package_unpacking_bus.png
人工查看 6 张截图，确认均为真实 Minecraft 客户端菜单画面
验证 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture 或客户端 smoke timeout
本次仅扩展客户端 smoke 覆盖面；GameTest 已按规则考虑，未新增行为 GameTest
验证 .\gradlew.bat build 成功
验证 .\gradlew.bat runGameTestServer 成功，90 个必需 GameTest 全部通过
验证 .\gradlew.bat runServer 成功到达 EULA gate，未出现 ClientSmokeRunner 或其他客户端类误加载；完整 dedicated server world-load 仍需用户显式同意 EULA 后执行
```

最新进展：

```text
补齐 packaged_processing_pattern 流体容器处理输出 ghost：
  Package Pattern Terminal 的处理输出 ghost 槽现在保存 display ItemStack 与真实 GenericStack 输出
  普通物品 ghost 继续按 AEItemKey + 数量编码
  Forge 流体容器 ghost 会显示容器物品，但编码为 AEFluidKey + 流体数量；水桶编码为 1000 mB water
  processing_outputs NBT 新增 key 字段保存 GenericStack，并兼容旧的仅 stack 字段物品 ghost 存档
  AE2 blank_pattern 有流体处理输出 ghost 时会输出 AE2 原版 encoded processing pattern，并附带 packaged_processing_pattern NBT
新增 2 个 GameTest：
  packagePatternTerminalMenuEncodesFluidProcessingOutputGhost
  packagePatternTerminalFluidProcessingOutputGhostPersists
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，103 个必需 GameTest 全部通过
```

最新进展：

```text
补齐 Package Assembler 的 AE2 Pattern Provider 流体输入封装：
  空样板槽的普通 Pattern Provider pushPattern 直接从 KeyCounter 读取 GenericStack，不再限制为 AEItemKey
  彩色处理样板 pushPattern 可按 AE2 sparse input 槽位把 AEFluidKey 拆入对应颜色包裹
  packaged_processing_pattern carrier pushPattern 可按 packages[] 精确消费流体 GenericStack 并输出对应包裹
  本地 package_pattern / packaged_processing_pattern 样板槽兼容路径仍通过 9 格物品缓冲执行，因此仍只接受可转成 ItemStack 的 AEItemKey
新增 2 个 GameTest，并将原 fluid reject 测试改为 accept：
  packageAssemblerAcceptsFluidPatternProviderPush
  packageAssemblerAcceptsColoredFluidPatternProviderPush
  packageAssemblerAcceptsFluidPackagedProcessingPush
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，105 个必需 GameTest 全部通过
```

最新进展：

```text
补齐 Package Bus 手工 required content 流体过滤：
  Package Bus required content ghost 槽现在可从 Forge 流体容器编码 AEFluidKey 过滤条件
  水桶会保存为 1000 mB water required content，ghost 编辑不消耗玩家光标容器
  手工流体过滤条件继续使用 PackageFilter NBT 保存/读取
  任意 AEKey 直接编辑器仍后置；当前流体通过容器作为可用玩家入口
新增 3 个 GameTest：
  packageFilterMatchesFluidRequiredContent
  packageBusMenuEditsManualFluidFilter
  packageBusManualFluidFilterPersists
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，108 个必需 GameTest 全部通过
```

最新进展：

```text
发布侧复核：
  在 Package Bus 流体 required content ghost 完成后重新执行 DataGen 与客户端菜单 smoke
验证 .\gradlew.bat runData 成功，未写出新的 generated resources 内容
验证 .\gradlew.bat runClientSmoke 成功，生成 6 张真实菜单截图：
  run/screenshots/appliedpackaging-client-smoke-package_assembler.png
  run/screenshots/appliedpackaging-client-smoke-me_packager.png
  run/screenshots/appliedpackaging-client-smoke-package_pattern_terminal.png
  run/screenshots/appliedpackaging-client-smoke-package_storage_bus.png
  run/screenshots/appliedpackaging-client-smoke-package_export_bus.png
  run/screenshots/appliedpackaging-client-smoke-package_unpacking_bus.png
人工抽看 Package Pattern Terminal 与 Package Storage Bus 截图，确认菜单非空屏、核心控件和槽位显示正常
验证 run/logs/latest.log 未发现 ERROR、FATAL、Exception、Missing model、Unable to load model、missing texture、Timed out 或 timeout
本次仅补验证证据与文档；GameTest 已按规则考虑，行为覆盖仍使用刚完成的 108 个必需 GameTest
```

最新进展：

```text
补齐流体 ghost 数量调整：
  Package Pattern Terminal 处理输出 ghost 槽支持滚轮调整已设置 key 的数量
  Package Bus required content ghost 槽支持滚轮调整已设置 key 的数量
  流体 key 每步调整 1000 mB，物品/其它已存在 key 每步调整 1
  数量不会降到小于一个调整步长；空光标点击清除仍保留
  客户端在 ghost 显示栈无法表达真实数量时绘制紧凑数量叠字，例如 2B 表示 2000 mB
新增 2 个 GameTest：
  packagePatternTerminalMenuAdjustsFluidProcessingOutputAmount
  packageBusMenuAdjustsManualFluidFilterAmount
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，110 个必需 GameTest 全部通过
验证 .\gradlew.bat runData 成功，未写出新的 generated resources 内容
验证 .\gradlew.bat runClientSmoke 成功，生成 6 张真实菜单截图并正常退出客户端
验证 run/logs/latest.log 未发现 ERROR、FATAL、Exception、Missing model、Unable to load model、missing texture、Timed out 或 timeout
```

最新进展：

```text
补齐 Package Pattern Terminal 的 AE2 cable part 形态：
  package_pattern_terminal 物品 id 从 BlockItem 改为 AE2 PartItem，不新增重复终端物品
  新增 PackagePatternTerminalPart，可贴到 AE2 cable bus 侧面并打开同一 PackagePatternTerminalScreen
  PackagePatternTerminalMenu 通过 host 类型标记支持 block host 与 part host 两种定位
  PackagePatternTerminalBlockEntity 继续保留兼容方块路径，并提供内容掉落/清空 API 供 part 拆除使用
  PackagePatternTerminalPart 保存/读取终端库存、selectedColor、输入槽颜色、处理输出 ghost 和 Split pending queue
  runClientSmoke 已改为放置真实 Package Pattern Terminal AE2 part，而不是旧终端方块
新增 2 个 GameTest：
  packagePatternTerminalItemPlacesAe2Part
  packagePatternTerminalPartPersistsContents
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，112 个必需 GameTest 全部通过
验证 .\gradlew.bat runClientSmoke 成功，生成 6 张真实菜单截图并正常退出客户端，其中 Package Pattern Terminal 截图来自真实 AE2 part 菜单
验证 run/logs/latest.log 未发现 ERROR、FATAL、Exception、Missing model、Unable to load model、missing texture、Timed out 或 timeout
```

最新进展：

```text
补齐发布 jar 元数据与随包文件：
  README.md / CHANGELOG.md 的 Package Pattern Terminal 状态已更新为 AE2 cable part item，不再误写为未实现 true cable part
  README.md / CHANGELOG.md / docs/chat-summary.md / docs/06-verification-release.md 的 GameTest 数量更新为 112
  Gradle jar 任务现在随包包含 LICENSE.md、README.md、CHANGELOG.md
  jar manifest 写入 Applied Packaging 的 specification/implementation title、version、vendor
  已检查 build/libs/appliedpackaging-0.1.0-dev.jar 内含 META-INF/mods.toml、META-INF/MANIFEST.MF、LICENSE.md、README.md、CHANGELOG.md 与 logo.png
  当前资源轻量审计：英文/简体中文语言 key 对齐，src/main/resources 下 52 个 PNG 均非空，54 个 JSON 可解析
验证 .\gradlew.bat build 成功，重新生成 build/libs/appliedpackaging-0.1.0-dev.jar
验证 .\gradlew.bat runData 成功，未写出新的 generated resources 内容
验证 .\gradlew.bat runGameTestServer 成功，112 个必需 GameTest 全部通过
验证 .\gradlew.bat runClientSmoke 成功，生成 6 张真实菜单截图并正常退出客户端，其中 Package Pattern Terminal 仍通过 AE2 part host 打开
```

最新进展：

```text
补齐 Package Pattern Terminal 的 Applied Packaging 自有 AE2 part 材质：
  新增 textures/part/package_pattern_terminal_front/sides/sides_status/back 以及 bright/medium/dark/colored overlay mask
  新增 models/part/package_pattern_terminal_base.json，PackagePatternTerminalPart 注册并使用该 AP 自有 body model
  package_pattern_terminal_off/on/item model 的发光与前脸纹理改为 appliedpackaging:part/*，不再引用 AE2 pattern terminal 纹理层
  本轮 AE2 资产仅作为形体和材质语言参考，未复制 AE2 像素
验证 assetgen validate-contract docs/assets/contracts/terminal_and_buses.yaml 成功
验证资源审计通过：60 个 PNG 非空，55 个 JSON 可解析，模型坐标保持在 0..16
验证 .\gradlew.bat build 成功
验证 .\gradlew.bat runData 成功，未写出新的 generated resources 内容
验证 .\gradlew.bat runClientSmoke 成功，Package Pattern Terminal 仍通过真实 AE2 part host 打开，6 张真实菜单截图生成并正常退出客户端
验证 run/logs/latest.log 未发现 ERROR、FATAL、Exception、Missing model、Unable to load model、missing texture、Timed out 或 timeout
验证 .\gradlew.bat runGameTestServer 成功，112 个必需 GameTest 全部通过
```

最新进展：

```text
发布验收审计：
  修正文档中 Package Bus 与 Package Pattern Terminal 的 AE2 cable part 表述，明确当前只有终端是 AE2 part，总线仍是 AE2 可连接方块端点
  将任意 AEKey 直接 ghost editor / required-content editor 归类为发布后增强，不作为 0.1.0-dev R1-R13 阻塞项
  在 docs/06-verification-release.md 增加 R1-R13 当前完成度审计和剩余 dedicated server full world-load 阻塞说明
验证 .\gradlew.bat runServer 成功到达 EULA gate
验证 run/eula.txt 当前为 eula=false，AI 未自动修改 EULA
验证 run/logs/latest.log 未发现 ERROR、FATAL、ClientSmokeRunner、NoClassDefFoundError、ClassNotFoundException 或客户端类误加载关键字
完整 dedicated server world-load 仍需用户显式同意 EULA 后执行
```

最新进展：

```text
发布 jar 清洁度修复：
  jar 任务排除 com/warmthdawn/appliedpackaging/client/ClientSmokeRunner*.class 和 com/warmthdawn/appliedpackaging/gametest/**
  AppliedPackagingClient 仅在 appliedpackaging.clientSmoke.enabled=true 时通过反射加载 ClientSmokeRunner，因此发布 jar 缺少该类不会影响普通客户端
  runClientSmoke 开发运行仍可从 build/classes 加载 ClientSmokeRunner
验证 .\gradlew.bat build 成功，重新生成 build/libs/appliedpackaging-0.1.0-dev.jar
验证 jar tf 未发现 ClientSmokeRunner、gametest、build/tmp/reference/preview/docs/assets/run 等 dev/test entries
验证 release jar 文本资源未发现 E:\、C:\Users、build/reference、build/asset-reference、.codex 或 asset-reference
验证 .\gradlew.bat runGameTestServer 成功，112 个必需 GameTest 全部通过
验证 .\gradlew.bat runClientSmoke 成功，反射加载 smoke runner 并生成 6 张真实菜单截图
验证 run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture、NoClassDefFoundError、ClassNotFoundException、InvocationTargetException、IllegalStateException、Timed out 或 timeout
验证 .\gradlew.bat runData 成功，未写出新的 generated resources 内容
验证 .\gradlew.bat runServer 成功到达 EULA gate，未出现客户端类误加载关键字；run/eula.txt 仍为 eula=false，完整 dedicated server world-load 仍需用户显式同意 EULA 后执行
```

最新进展：

```text
补齐目标级验收审计：
  在 docs/06-verification-release.md 增加最初目标到当前证据的逐项映射
  明确需求分析、概要设计、详细设计、设计入口、讨论记录、AGENTS.md 指令分离、1.20.1 Forge/AE2 基线、材质、R1-R13 功能、GameTest、DataGen、客户端 smoke 和发布 jar 均已有证据
  明确 dedicated server full world-load 仍因 run/eula.txt 为 eula=false 缺少最终证据
  明确发布 tag 应等待 dedicated server full world-load 通过后创建
本次仅补齐验收证据文档；GameTest 已按规则考虑，未新增行为测试。
```

最新进展：

```text
为 2026-07-05 新增需求和材质补充建立接收入口：
  新增 docs/08-change-intake.md，用于暂存发布前新增需求、材质替换、影响判定和范围冻结状态
  更新 docs/00-document-index.md 和 docs/design.md，把 08-change-intake 纳入文档体系
  更新 docs/06-verification-release.md，将 EULA 状态从 eula=false 阻塞改为用户已同意且 run/eula.txt 为 eula=true
  最终 dedicated server full world-load 和发布 tag 暂缓到新增需求/材质冻结、实现并验证之后
本次只改文档和验收状态记录；GameTest 已按规则考虑，未新增行为测试。
```

最新进展：

```text
补齐当前基线 dedicated server full world-load smoke：
  用户已明确同意 EULA，run/eula.txt 为 eula=true
  执行 .\gradlew.bat runServer --stacktrace
  服务端越过 EULA gate，加载 world，并在 run/logs/latest.log 记录 Done (2.724s)! For help, type "help"
  日志确认 Applied Packaging initialized、Starting minecraft server version 1.20.1、Preparing level "world"、Preparing start region for dimension minecraft:overworld、Enabled Gametest Namespaces: [appliedpackaging]
  run/logs/latest.log 未发现 ERROR、FATAL、ClientSmokeRunner、NoClassDefFoundError、ClassNotFoundException、InvocationTargetException、IllegalStateException、Dist.CLIENT、OnlyIn、Missing model、Unable to load model、missing texture、Exception、Crash 或 crash
  25565 未残留监听
  Gradle/Minecraft 控制台未接收 stop 命令，本次通过 Ctrl+C 终止 run，因此 Gradle 返回码不是发布判定依据；world-load 证据以 latest.log 为准
更新 docs/06-verification-release.md、docs/08-change-intake.md、README.md、CHANGELOG.md，把 EULA 阻塞改为当前基线服务端 world-load 已通过
最终发布 tag 仍等待 2026-07-05 新增需求/材质冻结、实现并重新验证
本次为 dedicated server smoke；GameTest 已按规则考虑，未新增行为测试，现有 GameTest 基线仍为 112 个必需 GameTest 全部通过
```

最新进展：

```text
新增机械发布审计脚本：
  新增 scripts/verify-release.ps1
  脚本检查 release jar 必需条目、dev/test/reference/preview 条目、jar 文本中的本机绝对路径或参考素材路径、资源 JSON、PNG 非空、英文/简体中文语言 key、Applied Packaging 模型贴图引用
  脚本支持 -RequireServerWorldLoad，要求 run/logs/latest.log 包含 Applied Packaging 初始化、world 准备和 Done (...) 服务端世界加载证据
  更新 docs/06-verification-release.md、docs/08-change-intake.md、README.md、AGENTS.md，将脚本纳入发布机械审计流程
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireServerWorldLoad 成功
脚本不替代 build、runData、runGameTestServer、runClientSmoke 或 runServer。
本次为验证工具和文档变更；GameTest 已按规则考虑，未新增行为测试。
```

最新进展：

```text
增强机械发布审计脚本：
  scripts/verify-release.ps1 新增 -RequireAssetContracts 和 -AssetgenPath 参数
  脚本会验证 docs/assets/contracts/*.yaml；默认自动寻找 PATH 中的 assetgen 或当前用户 Codex skill 下的 minecraft-mod-asset-generation/scripts/assetgen
  使用 -RequireAssetContracts 时，找不到 assetgen 或 contract 校验失败都会让脚本失败
  更新 docs/06-verification-release.md、docs/08-change-intake.md、README.md、AGENTS.md
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireAssetContracts 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireAssetContracts -RequireServerWorldLoad 成功
本次为验证工具和文档变更；GameTest 已按规则考虑，未新增行为测试。
```

最新进展：

```text
增强机械发布审计脚本：
  scripts/verify-release.ps1 现在读取 gradle.properties，并检查 jar 文件名、META-INF/mods.toml、META-INF/MANIFEST.MF 是否与 mod_id、mod_version、mod_name、mod_authors、mod_license、loader_version_range、forge_version_range、minecraft_version_range、ae2_version_range 对齐
  更新 docs/06-verification-release.md、docs/08-change-intake.md、README.md
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireAssetContracts -RequireServerWorldLoad 成功
本次为验证工具和文档变更；GameTest 已按规则考虑，未新增行为测试。
```

最新进展：

```text
新增发布检查编排脚本：
  新增 scripts/run-release-checks.ps1
  默认编排 .\gradlew.bat build --stacktrace、.\gradlew.bat runData --stacktrace、.\gradlew.bat runGameTestServer --stacktrace、scripts/verify-release.ps1 -RequireAssetContracts
  支持 -RunClientSmoke、-RequireServerWorldLoad、-AuditOnly、-PlanOnly、-SkipBuild、-SkipData、-SkipGameTest、-SkipAssetContracts
  该脚本不会自动运行长期驻留的 runServer；最终发布前仍需要手动运行 runServer 刷新 latest.log，再用 -RequireServerWorldLoad 检查证据
  更新 docs/06-verification-release.md、docs/08-change-intake.md、README.md、AGENTS.md
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -RunClientSmoke -RequireServerWorldLoad 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireAssetContracts -RequireServerWorldLoad 成功
GameTest：已考虑。未新增或运行 GameTest，原因是本次只增加发布检查编排脚本和文档引用，不改变 mod 运行行为。
```

最新进展：

```text
修正发布检查编排脚本的服务端日志审计模式：
  执行 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -RunClientSmoke -RequireServerWorldLoad 时发现最后的机械审计失败
  失败原因不是 mod 功能失败，而是 runClientSmoke 覆盖 run/logs/latest.log，导致 dedicated server world-load 审计读到客户端 smoke 日志
  scripts/run-release-checks.ps1 现在提前拒绝非 -AuditOnly 的 -RequireServerWorldLoad 组合
  scripts/verify-release.ps1 现在把 Mojang/Yggdrasil external public-key fetch failure 作为 WARN 忽略，其他 release-blocking 诊断关键字仍会失败
  正确流程为先执行 scripts/run-release-checks.ps1 -RunClientSmoke，再手动执行 .\gradlew.bat runServer 刷新 latest.log，最后执行 scripts/run-release-checks.ps1 -AuditOnly -RequireServerWorldLoad
  更新 docs/06-verification-release.md、docs/08-change-intake.md、README.md、AGENTS.md
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -RunClientSmoke -RequireServerWorldLoad 早失败成功，错误信息要求改用 -AuditOnly -RequireServerWorldLoad；PlanOnly 同样执行该组合检查
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -RunClientSmoke 成功，完成 build、runData、runGameTestServer、runClientSmoke 和 verify-release.ps1 -RequireAssetContracts
验证 .\gradlew.bat runServer --stacktrace 进入 dedicated server world-load，run/logs/latest.log 出现 Done (2.400s)! For help, type "help"
验证 run/logs/latest.log 确认 Applied Packaging initialized、Starting minecraft server version 1.20.1、Preparing level "world"、Preparing start region for dimension minecraft:overworld、Enabled Gametest Namespaces: [appliedpackaging]
本次 runServer 出现 1 条 Mojang/Yggdrasil external public-key fetch ERROR/WARN 栈；服务端仍进入 world-load，且该外部认证服务噪声不代表 Applied Packaging 失败
验证 25565 未残留监听；Gradle/Minecraft 控制台未接收 stop 命令，本次通过 Ctrl+C 终止 run，因此 Gradle 返回码不是发布判定依据
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireAssetContracts -RequireServerWorldLoad 成功，输出 1 条 ignored external Yggdrasil public-key fetch WARN
GameTest：已考虑。发现并运行现有 runGameTestServer；未新增或扩展 GameTest，原因是本次只修正发布验证脚本和文档，不改变 mod 运行行为。
```

最新进展：

```text
补齐 client smoke 截图机械审计：
  scripts/verify-release.ps1 新增 -RequireClientSmokeScreenshots
  该审计要求 6 张 run/screenshots/appliedpackaging-client-smoke-*.png 均存在、非空且带 PNG 签名
  scripts/run-release-checks.ps1 新增 -RequireClientSmokeScreenshots，并在使用 -RunClientSmoke 时自动传递该审计项
  更新 docs/06-verification-release.md、docs/08-change-intake.md、README.md、AGENTS.md
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -RunClientSmoke 成功，计划中的机械审计包含 -RequireClientSmokeScreenshots
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireAssetContracts -RequireClientSmokeScreenshots -RequireServerWorldLoad 成功，确认 6 张 client smoke 截图存在且为有效 PNG
GameTest：已考虑。发现现有 runGameTestServer；本次只增强发布验证脚本和文档，不改变 mod 运行行为，因此未新增、扩展或运行 GameTest。
```

最新进展：

```text
校准发布阶段实施计划与目标级验收证据：
  更新 docs/05-implementation-plan.md 阶段 7，记录当前已完成的 release runner、mechanical audit、client smoke 截图审计、dedicated server two-step world-load 审计和发布 tag 暂缓状态
  更新 docs/06-verification-release.md 目标级验收审计中的 dedicated server full world-load 证据，使用最新 Done (2.400s) 与 audit-only 验证结果
  保留发布 tag 暂缓到新增需求/材质冻结、实现并重新验证之后
GameTest：已考虑。发现现有 runGameTestServer；本次只修正文档状态，不改变 mod 运行行为，因此未新增、扩展或运行 GameTest。
```

最新进展：

```text
补齐 clean git 发布门禁：
  scripts/verify-release.ps1 新增 -RequireCleanGit，可选执行 git status --porcelain=v1 --untracked-files=all，并要求工作树无输出
  scripts/run-release-checks.ps1 新增 -RequireCleanGit，并传递给机械发布审计
  更新 docs/05-implementation-plan.md、docs/06-verification-release.md、docs/08-change-intake.md、README.md、AGENTS.md
  该门禁只用于最终范围冻结、所有变更提交后、发布 tag 创建前；默认发布检查流程不因开发中的脏工作树失败
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -AuditOnly -RequireCleanGit 成功，计划中的机械审计包含 -RequireCleanGit
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireAssetContracts -RequireClientSmokeScreenshots -RequireServerWorldLoad 成功，确认默认发布审计不受开发中脏工作树影响
验证提交 10b59b2 后执行 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireCleanGit 成功，确认当前提交基线工作树干净
GameTest：已考虑。发现现有 runGameTestServer；本次只增强发布验证脚本和文档，不改变 mod 运行行为，因此未新增、扩展或运行 GameTest。
```

最新进展：

```text
补齐 dedicated server world-load 自动 smoke：
  新增 scripts/run-server-smoke.ps1
  脚本要求 run/eula.txt 中 eula=true，启动 .\gradlew.bat runServer --stacktrace，等待 latest.log 出现 Applied Packaging initialized、Preparing level "world" 和 Done (...) world-load 标记
  world-load 成功后，脚本终止自己启动的 runServer 进程树，并检查 25565 未保持监听
  scripts/run-release-checks.ps1 新增 -RunServerSmoke 和 -ServerSmokeTimeoutSeconds
  使用 -RunServerSmoke 时，服务端 smoke 在 build/runData/runGameTestServer/runClientSmoke 之后执行，随后机械审计自动包含 -RequireServerWorldLoad
  更新 docs/05-implementation-plan.md、docs/06-verification-release.md、docs/08-change-intake.md、README.md、AGENTS.md
验证 PowerShell parser 解析 scripts/run-server-smoke.ps1、scripts/run-release-checks.ps1 和 scripts/verify-release.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -RunClientSmoke -RunServerSmoke 成功，确认执行顺序为 build、runData、runGameTestServer、runClientSmoke、server smoke、mechanical audit
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -RequireServerWorldLoad 按预期早失败，提示改用 -AuditOnly 或 -RunServerSmoke
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke 成功，server smoke 进入 Done (2.413s)!，终止本次 runServer 进程树，确认 25565 未监听，并通过 verify-release.ps1 -RequireAssetContracts -RequireServerWorldLoad
GameTest：已考虑。发现现有 runGameTestServer；本次增强 dedicated server smoke 编排，不改变 mod 运行行为，因此未新增、扩展或运行 GameTest。
```

最新进展：

```text
补齐发布清单生成：
  新增 scripts/write-release-manifest.ps1
  发布清单输出到 build/release/appliedpackaging-<version>-release-manifest.json
  清单记录 mod 版本、Minecraft/Forge/AE2/GuideME 版本范围、jar 路径、jar 大小、SHA-256、jar mtime、git branch、git commit 和 clean 状态
  scripts/run-release-checks.ps1 新增 -WriteReleaseManifest，并在机械发布审计之后调用 write-release-manifest.ps1
  如果同时使用 -RequireCleanGit，发布清单脚本也会要求 git 工作树干净
  更新 docs/05-implementation-plan.md、docs/06-verification-release.md、docs/08-change-intake.md、README.md、AGENTS.md
验证 PowerShell parser 解析 scripts/write-release-manifest.ps1、scripts/run-release-checks.ps1 和 scripts/verify-release.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -AuditOnly -WriteReleaseManifest 成功，确认机械审计后会执行发布清单生成
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\write-release-manifest.ps1 成功，生成 build/release/appliedpackaging-0.1.0-dev-release-manifest.json
验证发布清单 JSON 可解析，artifact.sha256 与 build/libs/appliedpackaging-0.1.0-dev.jar 的 SHA-256 一致，git commit、branch 和 shortCommit 与当前仓库一致
验证 git diff --check 成功
最终发布 tag 前可执行 run-release-checks.ps1 -AuditOnly -RequireCleanGit -WriteReleaseManifest 验证 clean git + manifest 组合
GameTest：已考虑。发现现有 runGameTestServer；本次只增强发布清单脚本和发布编排，不改变 mod 运行行为，因此未新增、扩展或运行 GameTest。
```

最新进展：

```text
补齐文档完整性发布审计：
  新增 scripts/verify-docs.ps1
  脚本检查 AGENTS.md、README.md、CHANGELOG.md、LICENSE.md、docs/design.md、00-08 分类文档、chat-summary、development-log、资产 brief、资产 contract 和资产报告是否存在
  脚本检查 docs/design.md 和 docs/00-document-index.md 是否覆盖当前文档集合
  脚本扫描仓库 Markdown 中的本地 inline link 是否可解析
  scripts/run-release-checks.ps1 默认新增 Documentation audit 步骤，可用 -SkipDocs 跳过
  更新 docs/05-implementation-plan.md、docs/06-verification-release.md、docs/08-change-intake.md、README.md、CHANGELOG.md、AGENTS.md
验证 PowerShell parser 解析 scripts/verify-docs.ps1 和 scripts/run-release-checks.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功，确认必需文档存在、design/index 覆盖文档集合、20 个本地 Markdown 链接可解析
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -AuditOnly -WriteReleaseManifest 成功，确认 release runner 顺序为机械审计、文档审计、发布清单
验证 git diff --check 成功
GameTest：已考虑。发现现有 runGameTestServer；本次只增强文档审计脚本和发布编排，不改变 mod 运行行为，因此未新增、扩展或运行 GameTest。
```

最新进展：

```text
补齐发布清单复验门禁：
  新增 scripts/verify-release-manifest.ps1
  脚本读取 release manifest、gradle.properties、release jar 和当前 git 状态
  校验 schema、mod 元数据、Minecraft/Forge/AE2/GuideME 版本范围、jar 路径、文件名、大小、mtime、SHA-256、git commit/shortCommit/branch/clean/statusPorcelain 和 manifest 路径
  scripts/write-release-manifest.ps1 默认 jar 路径改为根据 gradle.properties 的 mod_id/mod_version 推导，避免版本号调整后脚本默认值滞后
  scripts/run-release-checks.ps1 新增 -RequireReleaseManifest，并在 -WriteReleaseManifest 后执行 release manifest audit
  更新 docs/05-implementation-plan.md、docs/06-verification-release.md、docs/08-change-intake.md、README.md、CHANGELOG.md、AGENTS.md
验证 PowerShell parser 解析 scripts/verify-release-manifest.ps1、scripts/write-release-manifest.ps1 和 scripts/run-release-checks.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -AuditOnly -WriteReleaseManifest -RequireReleaseManifest 成功，确认顺序为机械审计、文档审计、发布清单、发布清单审计
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\write-release-manifest.ps1 成功，生成的清单记录当前 a531d35、jar SHA-256 和开发中 dirty 状态
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-manifest.ps1 成功，确认清单匹配当前 jar、gradle.properties 和 git 状态
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest 成功，确认机械发布审计、文档审计、发布清单和发布清单审计可串联通过
GameTest：已考虑。发现现有 runGameTestServer；本次只增强发布清单脚本、发布编排和文档，不改变 mod 运行行为，因此未新增、扩展或运行 GameTest。
```

最新进展：

```text
补齐 GuideME 发布依赖元数据：
  gradle.properties 新增 guideme_version_range=[20.1.7,20.2.0)
  build.gradle 的 generateModMetadata 替换表新增 guideme_version_range
  src/main/templates/META-INF/mods.toml 新增 guideme mandatory dependency，ordering=AFTER，side=BOTH
  scripts/verify-release.ps1 新增 guideme_version_range 必填属性和 mods.toml GuideME dependency range 审计
  scripts/write-release-manifest.ps1 新增 dependencies.guideMeVersionRange
  scripts/verify-release-manifest.ps1 新增 dependencies.guideMeVersionRange 审计
  更新 README.md、CHANGELOG.md、AGENTS.md、docs/design.md、docs/01-requirements.md、docs/02-system-architecture.md、docs/05-implementation-plan.md、docs/06-verification-release.md、docs/07-references.md、docs/08-change-intake.md
验证 PowerShell parser 解析 verify-release.ps1、write-release-manifest.ps1、verify-release-manifest.ps1 和 run-release-checks.ps1 成功
验证 .\gradlew.bat build --stacktrace 成功，generateModMetadata 和 jar 重新执行，发布 jar 的 META-INF/mods.toml 包含 guideme [20.1.7,20.2.0) mandatory dependency
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest 成功，确认 mods.toml GuideME dependency range 与 gradle.properties 一致，发布清单中的 guideMeVersionRange 也匹配
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke 成功，确认显式 GuideME dependency metadata 不破坏 dedicated server world-load
GameTest：已考虑。发现现有 runGameTestServer；本次只修改发布 metadata、发布审计脚本和文档，不改变包裹、机器、总线、菜单、网络或事务行为，因此未新增、扩展或运行 GameTest。由于 mod metadata 会影响 dedicated server 依赖加载，本次改用 build + release audit + server smoke 验证。
```

最新进展：

```text
补齐发布附件包生成与复验：
  新增 scripts/write-release-bundle.ps1
  新增 scripts/verify-release-bundle.ps1
  scripts/run-release-checks.ps1 新增 -WriteReleaseBundle 和 -RequireReleaseBundle
  发布附件包输出到 build/release/appliedpackaging-<version>-release-bundle.zip
  zip 内包含 appliedpackaging-<version>.jar、release manifest、README.md、CHANGELOG.md、LICENSE.md 和 SHA256SUMS.txt
  bundle audit 会检查 zip 条目集合、每个条目的 SHA-256、SHA256SUMS 内容，以及 bundle 内 manifest 的 artifact sha256 是否匹配 bundle 内 jar
  更新 README.md、CHANGELOG.md、AGENTS.md、docs/05-implementation-plan.md、docs/06-verification-release.md、docs/08-change-intake.md
验证 PowerShell parser 解析 write-release-bundle.ps1、verify-release-bundle.ps1 和 run-release-checks.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认 release runner 顺序为机械审计、文档审计、发布清单、发布清单审计、发布附件包、发布附件包审计
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，生成并复验 release bundle
GameTest：已考虑。发现现有 runGameTestServer；本次只增强发布附件包脚本、发布编排和文档，不改变 mod 运行行为，因此未新增、扩展或运行 GameTest。
```

最新进展：

```text
补齐候选发布一键门禁：
  scripts/run-release-checks.ps1 新增 -ReleaseCandidate
  -ReleaseCandidate 禁止与 -AuditOnly 或 skip flags 组合
  -ReleaseCandidate 自动启用 -RunClientSmoke、-RunServerSmoke、-WriteReleaseManifest、-RequireReleaseManifest、-WriteReleaseBundle 和 -RequireReleaseBundle
  最终发布 tag 前的推荐命令收敛为 run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit
  更新 README.md、CHANGELOG.md、AGENTS.md、docs/05-implementation-plan.md、docs/06-verification-release.md、docs/08-change-intake.md
验证 PowerShell parser 解析 scripts/run-release-checks.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -ReleaseCandidate -RequireCleanGit 成功，确认完整顺序为 build、runData、runGameTestServer、runClientSmoke、run-server-smoke、mechanical release audit、documentation audit、release manifest、release manifest audit、release bundle、release bundle audit
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -ReleaseCandidate -SkipGameTest 按预期失败，错误为 -ReleaseCandidate cannot be combined with skip flags: -SkipGameTest
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -ReleaseCandidate -AuditOnly 按预期失败，错误为 -ReleaseCandidate cannot be combined with -AuditOnly
验证 .\gradlew.bat build --stacktrace 成功，刷新包含 README.md 和 CHANGELOG.md 的发布 jar
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认机械审计、文档审计、manifest 生成/复验和 bundle 生成/复验仍可串联通过
GameTest：已考虑。发现现有 runGameTestServer；本次只增强发布门禁预设和文档，不改变包裹、机器、总线、菜单、网络、事务或数据生成行为，因此未新增、扩展或运行 GameTest。最终候选发布预设会在范围冻结后运行 runGameTestServer。
```

最新进展：

```text
验证完整候选发布门禁：
  执行 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit 成功
  build 成功
  runData 成功
  runGameTestServer 成功，112 个必需 GameTest 全部通过
  runClientSmoke 成功，捕获 6 张截图：
    appliedpackaging-client-smoke-package_assembler.png
    appliedpackaging-client-smoke-me_packager.png
    appliedpackaging-client-smoke-package_pattern_terminal.png
    appliedpackaging-client-smoke-package_storage_bus.png
    appliedpackaging-client-smoke-package_export_bus.png
    appliedpackaging-client-smoke-package_unpacking_bus.png
  run-server-smoke.ps1 成功，run/logs/latest.log 出现 Done (2.471s)!，25565 清理完成
  verify-release.ps1 -RequireAssetContracts -RequireServerWorldLoad -RequireClientSmokeScreenshots -RequireCleanGit 成功
  verify-docs.ps1 成功
  write-release-manifest.ps1 -RequireCleanGit 成功，manifest 记录当时提交基线且 clean=true
  verify-release-manifest.ps1 -RequireCleanGit 成功
  write-release-bundle.ps1 -RequireCleanGit 成功
  verify-release-bundle.ps1 -RequireCleanGit 成功
  当前完整候选门禁只证明 2026-07-04 提交基线；用户 2026-07-05 补充需求和材质后仍需重新执行。
GameTest：已考虑并运行。发现现有 runGameTestServer；本次通过 run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit 间接运行 .\gradlew.bat runGameTestServer，112 个必需 GameTest 全部通过。本次未新增或扩展 GameTest。
```

最新进展：

```text
补齐发布 tag 就绪门禁：
  新增 scripts/verify-release-readiness.ps1
  脚本读取 docs/08-change-intake.md 和 docs/06-verification-release.md
  blocker 匹配限制为 intake 表行、发布 tag 状态行、最终服务端 world-load 状态行和当前目标完成判定行，避免说明文字误触发
  默认模式会报告待输入/待判定 intake blocker 但退出 0，用于预冻结状态检查
  -RequireReadyForTag 模式遇到待输入/待判定 intake、开放接收窗口或验证文档仍标记发布未完成时退出 1
  scripts/run-release-checks.ps1 新增 -RequireReadyForTag，并在文档审计后执行 verify-release-readiness.ps1 -RequireReadyForTag
  最终发布 tag 前推荐命令更新为 run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag
验证 PowerShell parser 解析 verify-release-readiness.ps1 和 run-release-checks.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 成功，报告当前 4 个非致命 blocker：IN-001 待输入、IN-002 待输入、变更接收窗口仍开放、验证文档仍标记发布未完成
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag 按预期失败，确认 tag 就绪门禁会阻止当前未冻结范围发布
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -ReleaseCandidate -RequireCleanGit -RequireReadyForTag 成功，确认完整候选发布计划会在文档审计后、manifest/bundle 生成前执行 Release readiness audit
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer；本次只增强发布 tag 就绪门禁和发布编排，不改变包裹、机器、总线、菜单、网络、事务或数据生成行为，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补齐发布 tag readiness 自测：
  verify-release-readiness.ps1 新增 ChangeIntakePath 和 VerificationPath 参数，默认仍读取 docs/08-change-intake.md 与 docs/06-verification-release.md
  新增 scripts/test-release-readiness.ps1，使用临时 Markdown fixture 覆盖 ready、blocked 和 structural failure 三种路径
  ready fixture 使用已迁移 intake、已完成服务端 world-load 和可创建发布 tag 状态，-RequireReadyForTag 退出 0
  blocked fixture 使用待输入 intake、发布 tag 等待和不能标记完成状态，-RequireReadyForTag 退出 1
  structural failure fixture 缺少必需新增项暂存表标题，-RequireReadyForTag 退出 1
验证 PowerShell parser 解析 verify-release-readiness.ps1、test-release-readiness.ps1 和 run-release-checks.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 成功，当前正式文档仍报告 4 个非致命 blocker
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag 按预期失败，当前正式文档仍阻止最终 tag
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer；本次只增加发布 readiness 脚本自测和文档记录，不改变游戏行为、数据生成、资源、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。
```

最新进展：

```text
补齐文档审计中的发布脚本存在性检查：
  scripts/verify-docs.ps1 的 required path 集合新增关键发布脚本：
    scripts/run-release-checks.ps1
    scripts/run-server-smoke.ps1
    scripts/verify-release.ps1
    scripts/verify-docs.ps1
    scripts/verify-release-readiness.ps1
    scripts/test-release-readiness.ps1
    scripts/write-release-manifest.ps1
    scripts/verify-release-manifest.ps1
    scripts/write-release-bundle.ps1
    scripts/verify-release-bundle.ps1
  Assert-PathExists 输出从 Required document exists 调整为 Required path exists，以覆盖文档与脚本两类路径
  AGENTS.md、README.md、CHANGELOG.md 和 docs/06-verification-release.md 同步说明 verify-docs 会检查关键发布脚本
验证 PowerShell parser 解析 verify-docs.ps1、verify-release-readiness.ps1、test-release-readiness.ps1 和 run-release-checks.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功，确认必需文档、资产文档、关键发布脚本、文档入口和本地 Markdown 链接均通过
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 成功，当前正式文档仍报告 4 个非致命 blocker
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag 按预期失败，当前正式文档仍阻止最终 tag
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer；本次只增强发布脚本文档审计，不改变游戏行为、数据生成、资源、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。
```

最新进展：

```text
补齐 release candidate plan 自测：
  新增 scripts/test-release-check-plan.ps1
  自测调用 run-release-checks.ps1 -PlanOnly -ReleaseCandidate -RequireCleanGit -RequireReadyForTag
  检查完整候选发布步骤顺序：
    Gradle build
    Data generation
    GameTest server
    Client smoke screenshots
    Dedicated server world-load smoke
    Mechanical release audit
    Documentation audit
    Release readiness audit
    Release manifest
    Release manifest audit
    Release bundle
    Release bundle audit
  检查关键命令参数包含 runGameTestServer、runClientSmoke、run-server-smoke.ps1、verify-release.ps1 的 asset/server/client/clean-git 审计、verify-release-readiness.ps1 -RequireReadyForTag、manifest/bundle clean-git 审计
  检查 -ReleaseCandidate -SkipGameTest 和 -ReleaseCandidate -AuditOnly 均失败
  scripts/verify-docs.ps1 将 scripts/test-release-check-plan.ps1 纳入必需路径
  AGENTS.md、README.md、CHANGELOG.md 和 docs/06-verification-release.md 同步说明 release plan 自测
验证 PowerShell parser 解析 test-release-check-plan.ps1、run-release-checks.ps1 和 verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-check-plan.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功，确认新增 test-release-check-plan.ps1 已纳入必需发布脚本路径
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 成功，当前正式文档仍报告 4 个非致命 blocker
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag 按预期失败，当前正式文档仍阻止最终 tag
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer；本次只增加 release runner plan 自测和文档记录，不改变游戏行为、数据生成、资源、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
增强 release candidate plan 自测覆盖：
  scripts/test-release-check-plan.ps1 从只检查 -SkipGameTest 扩展为检查全部 release candidate 禁止的 skip flags：
    -SkipBuild
    -SkipData
    -SkipGameTest
    -SkipDocs
    -SkipAssetContracts
  新增检查 -AuditOnly -RunServerSmoke 必须失败
  新增检查普通执行模式下 -RequireServerWorldLoad 不搭配 -RunServerSmoke 必须失败，避免使用陈旧 latest.log 误当主动服务端验证
  AGENTS.md、README.md、CHANGELOG.md 和 docs/06-verification-release.md 同步说明 release plan 自测覆盖所有 skip flags 和 server world-load guardrails
验证 PowerShell parser 解析 test-release-check-plan.ps1 和 run-release-checks.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-check-plan.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 成功，当前正式文档仍报告 4 个非致命 blocker
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag 按预期失败，当前正式文档仍阻止最终 tag
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer；本次只增强 release runner plan 自测和文档记录，不改变游戏行为、数据生成、资源、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
增强发布 tag readiness 正向信号保护：
  scripts/verify-release-readiness.ps1 新增 Test-PositiveReadinessSignals
  当负面 blocker 全部清除后，仍要求 docs/08-change-intake.md 明确记录：
    已冻结。
    最终服务端 world-load：已完成。
    发布 tag：可创建。
  同时要求 docs/06-verification-release.md 明确记录：
    可以标记完成。
    发布 tag 就绪门禁已通过。
  这样可以防止只删除“待输入/未完成/等待”文字但没有真实冻结证据时误放行发布 tag。
  scripts/test-release-readiness.ps1 新增 missing positive signals fixture，并扩展 Invoke-ReadinessCase 以检查期望输出文本。
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md。
验证 PowerShell parser 解析 verify-release-readiness.ps1、test-release-readiness.ps1 和 run-release-checks.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1 成功，确认 ready fixture 退出 0，blocked fixture、structural failure fixture 和 missing positive signals fixture 退出 1
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 成功，当前正式文档仍报告 4 个非致命 blocker
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag 按预期失败，当前正式文档仍阻止最终 tag
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-check-plan.ps1 成功
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer；本次只增强发布 readiness 门禁、自测脚本和文档记录，不改变游戏行为、数据生成、资源、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
增强 release bundle manifest 交叉校验：
  scripts/verify-release-bundle.ps1 新增 bundle manifest JSON 字段读取与断言 helper
  bundle audit 现在检查 bundle 内 manifest 的 mod.id 和 mod.version 匹配 gradle.properties
  bundle audit 继续检查 bundle 内 manifest artifact.fileName 和 artifact.sha256 匹配 bundle 内 jar
  使用 -RequireCleanGit 时，bundle audit 还会检查 bundle 内 manifest 的 git.commit、git.shortCommit、git.branch、git.clean 和 git.statusPorcelain 匹配当前 checkout
  这样单独复验发布 zip 时，不只验证 zip 条目和 SHA256SUMS，也能确认 bundle 内 manifest 仍指向当前提交
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md
验证 PowerShell parser 解析 verify-release-bundle.ps1、write-release-bundle.ps1 和 run-release-checks.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-bundle.ps1 曾按预期失败，原因是旧 bundle 中 README.md / CHANGELOG.md 与本轮文档更新后的源文件哈希不一致
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，重新生成并复验 release manifest 和 release bundle；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer；本次只增强发布 bundle 审计脚本和文档记录，不改变游戏行为、数据生成、资源、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补齐 release bundle 自测：
  新增 scripts/test-release-bundle.ps1
  自测使用系统临时目录生成 release manifest 和 release bundle fixture，不写入正式 build/release
  覆盖有效 bundle 可通过 verify-release-bundle.ps1
  覆盖 bundle 内 release manifest 的 mod.id 被篡改时 verify-release-bundle.ps1 失败
  覆盖 bundle 内 README.md 内容被篡改时 verify-release-bundle.ps1 失败
  工作区干净时额外覆盖 verify-release-bundle.ps1 -RequireCleanGit 的 git 元数据校验路径
  修复 write-release-manifest.ps1 和 write-release-bundle.ps1 对绝对 ManifestPath / BundlePath 的输出路径处理，避免把绝对路径错误拼接到 repo 路径下
  scripts/verify-docs.ps1 将 scripts/test-release-bundle.ps1 纳入必需发布脚本路径
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md
验证 PowerShell parser 解析 write-release-manifest.ps1、write-release-bundle.ps1、verify-release-bundle.ps1 和 test-release-bundle.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-bundle.ps1 成功；开发中工作区 dirty，clean-git fixture 按预期跳过，提交后需重跑覆盖
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功，确认 scripts/test-release-bundle.ps1 已纳入必需发布脚本路径
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认 manifest/bundle 正式路径仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
提交后验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-bundle.ps1 成功，clean-git bundle fixture 退出 0
提交后验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireCleanGit -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，manifest 记录当前提交且 clean=true
GameTest：已考虑。发现现有 runGameTestServer；本次只增强发布 bundle 自测、发布脚本绝对路径处理和文档记录，不改变游戏行为、数据生成、资源、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补齐 release manifest 自测：
  新增 scripts/test-release-manifest.ps1
  自测使用系统临时目录生成 release manifest fixture，不写入正式 build/release
  覆盖有效 manifest 可通过 verify-release-manifest.ps1
  覆盖 manifest 的 mod.id 被篡改时 verify-release-manifest.ps1 失败
  覆盖 manifest 的 artifact.sha256 被篡改时 verify-release-manifest.ps1 失败
  工作区干净时额外覆盖 write-release-manifest.ps1 -RequireCleanGit 和 verify-release-manifest.ps1 -RequireCleanGit 的 git 元数据校验路径
  scripts/verify-docs.ps1 将 scripts/test-release-manifest.ps1 纳入必需发布脚本路径
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md
验证 PowerShell parser 解析 write-release-manifest.ps1、verify-release-manifest.ps1、test-release-manifest.ps1 和 verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-manifest.ps1 成功；开发中工作区 dirty，clean-git fixture 按预期跳过，提交后需重跑覆盖
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功，确认 scripts/test-release-manifest.ps1 已纳入必需发布脚本路径
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认 manifest/bundle 正式路径仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer；本次只增强发布 manifest 自测和文档记录，不改变游戏行为、数据生成、资源、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补齐 release 自测聚合入口：
  新增 scripts/test-release-self-tests.ps1
  聚合运行 scripts/test-release-readiness.ps1
  聚合运行 scripts/test-release-check-plan.ps1
  聚合运行 scripts/test-release-manifest.ps1
  聚合运行 scripts/test-release-bundle.ps1
  该脚本不运行 Gradle、客户端或服务端，只验证发布脚本自测套件和负路径 guardrails
  scripts/verify-docs.ps1 将 scripts/test-release-self-tests.ps1 纳入必需发布脚本路径
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md
验证 PowerShell parser 解析 test-release-self-tests.ps1、test-release-readiness.ps1、test-release-check-plan.ps1、test-release-manifest.ps1、test-release-bundle.ps1 和 verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功；开发中工作区 dirty，manifest/bundle 子测试的 clean-git fixture 按预期跳过，提交后需重跑覆盖
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功，确认 scripts/test-release-self-tests.ps1 已纳入必需发布脚本路径
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认 manifest/bundle 正式路径仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer；本次只增加发布脚本自测聚合入口和文档记录，不改变游戏行为、数据生成、资源、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补齐 documentation audit 自测：
  scripts/verify-docs.ps1 新增 -RootPath 参数，可对临时 fixture 执行同一套文档审计
  新增 scripts/test-docs-audit.ps1
  自测使用系统临时目录生成最小文档 fixture，不修改正式 docs
  覆盖有效 docs fixture 可通过 verify-docs.ps1 -RootPath
  覆盖缺少 docs/04-asset-spec.md 时 verify-docs.ps1 失败
  覆盖 README.md 本地 Markdown 链接指向不存在文件时 verify-docs.ps1 失败
  scripts/verify-docs.ps1 将 scripts/test-docs-audit.ps1 纳入必需发布脚本路径
  scripts/test-release-self-tests.ps1 已纳入 test-docs-audit.ps1
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md
验证 PowerShell parser 解析 verify-docs.ps1、test-docs-audit.ps1 和 test-release-self-tests.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-docs-audit.ps1 成功，确认 valid fixture 退出 0，missing required path fixture 和 broken markdown link fixture 退出 1
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功，确认 docs audit、readiness、release plan、manifest 和 bundle 自测均可由聚合入口串行通过；开发中工作区 dirty，manifest/bundle 子测试的 clean-git fixture 按预期跳过，提交后需重跑覆盖
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功，确认 scripts/test-docs-audit.ps1 已纳入必需发布脚本路径
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认 manifest/bundle 正式路径仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer；本次只增强文档审计脚本、自测和发布脚本聚合入口，不改变游戏行为、数据生成、资源、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补齐 mechanical release audit 自测：
  scripts/verify-release.ps1 新增 -RootPath 参数，可对临时 fixture 执行同一套机械发布审计
  新增 scripts/test-release-audit.ps1
  自测使用系统临时目录生成最小 release fixture 和假 jar，不修改正式 build/release
  覆盖有效 release audit fixture 可通过 verify-release.ps1 -RootPath
  覆盖 jar 缺少 README.md 时 verify-release.ps1 失败
  覆盖 META-INF/mods.toml 的 mod id 被篡改时 verify-release.ps1 失败
  覆盖 jar 文本资源泄漏本机/reference 路径时 verify-release.ps1 失败
  scripts/verify-docs.ps1 将 scripts/test-release-audit.ps1 纳入必需发布脚本路径
  scripts/test-release-self-tests.ps1 已纳入 test-release-audit.ps1
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md
验证 PowerShell parser 解析 verify-release.ps1、test-release-audit.ps1、verify-docs.ps1、test-docs-audit.ps1 和 test-release-self-tests.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-audit.ps1 成功，确认 valid fixture 退出 0，missing README、tampered metadata 和 local path leak fixture 均退出 1
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功，确认 docs audit、release audit、readiness、release plan、manifest 和 bundle 自测均可由聚合入口串行通过；开发中工作区 dirty，manifest/bundle 子测试的 clean-git fixture 按预期跳过，提交后需重跑覆盖
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功，确认 scripts/test-release-audit.ps1 已纳入必需发布脚本路径
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认机械发布审计、文档审计、manifest 生成/审计和 bundle 生成/审计仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer；本次只增强发布机械审计脚本、自测聚合入口和文档记录，不改变游戏行为、数据生成、资源、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补齐 asset resource audit：
  新增 scripts/verify-assets.ps1
  审计 src/main/resources/assets/appliedpackaging 下的发布 PNG 资源
  检查必需 PNG 是否存在
  检查 PNG header 有效且 color type 为 RGBA
  检查 PNG 路径位于已知 release asset 目录
  检查 item/block 为 32x32，GUI icon 与 AE2 part 为 16x16，root/gui logo 为 128x128
  新增 scripts/test-assets-audit.ps1
  自测使用系统临时目录复制当前资源 fixture，不修改正式资源
  覆盖有效资源、错尺寸、坏 PNG header 和缺必需 PNG
  scripts/run-release-checks.ps1 新增 Asset resource audit 步骤，位于 Mechanical release audit 之后、Documentation audit 之前
  scripts/test-release-check-plan.ps1 已检查候选发布计划包含 Asset resource audit
  scripts/verify-docs.ps1 将 verify-assets.ps1 和 test-assets-audit.ps1 纳入必需发布脚本路径
  scripts/test-release-self-tests.ps1 已纳入 test-assets-audit.ps1
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/04-asset-spec.md、docs/assets/acceptance.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md
验证 PowerShell parser 解析 verify-assets.ps1、test-assets-audit.ps1、run-release-checks.ps1、test-release-check-plan.ps1、verify-docs.ps1、test-docs-audit.ps1 和 test-release-self-tests.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1 成功，确认 60 个发布 PNG 的 header、RGBA 类型、路径归类和尺寸符合规格
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-assets-audit.ps1 成功，确认 valid fixture 退出 0，bad dimension、bad PNG header 和 missing required PNG fixture 均退出 1
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-check-plan.ps1 成功，确认 ReleaseCandidate 计划包含 Asset resource audit
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功，确认 docs audit、asset audit、release audit、readiness、release plan、manifest 和 bundle 自测均可由聚合入口串行通过；开发中工作区 dirty，manifest/bundle 子测试的 clean-git fixture 按预期跳过，提交后需重跑覆盖
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功，确认 verify-assets.ps1 和 test-assets-audit.ps1 已纳入必需发布脚本路径
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认机械发布审计、Asset resource audit、文档审计、manifest 生成/审计和 bundle 生成/审计仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer；本次只增强资产验收脚本、发布检查编排、自测聚合入口和文档记录，不改变游戏行为、数据生成、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
收敛 Split 输出到 AE2 blank_pattern carrier：
  PackagePatternDataStorage 新增 newBlankPatternCarrier，用于创建 AE2 原版 blank_pattern 数据载体
  PackagePatternTerminalBlockEntity Split 输出改为 AE2 blank_pattern 承载 package_pattern 数据
  本地 package_pattern / packaged_processing_pattern 继续保留注册和读取兼容，但正常 Split 玩家流程不再产出本地 package_pattern
  PackageDataGameTests 更新 Split 和 pending queue 断言，确认输出是 AE2 blank_pattern carrier 且不是本地 package_pattern
  更新 CHANGELOG.md、docs/design.md、docs/03-detailed-design.md、docs/05-implementation-plan.md 和 docs/06-verification-release.md
验证 .\gradlew.bat compileJava --stacktrace 成功
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，112 个必需 GameTest 全部通过
验证 .\gradlew.bat build --stacktrace 成功，重新生成 build/libs/appliedpackaging-0.1.0-dev.jar
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，机械发布审计、Asset resource audit、文档审计、manifest 生成/审计和 bundle 生成/审计均通过；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer 与 PackageDataGameTests；本次变更影响样板终端 Split 行为与玩家可获得样板载体，属于行为敏感变更。已扩展现有 Split 相关 GameTest 断言，并已执行 .\gradlew.bat runGameTestServer --stacktrace。
```

最新进展：

```text
补齐玩家入口产品不变量 release audit：
  scripts/verify-release.ps1 新增产品不变量审计
  检查本地 package_pattern / packaged_processing_pattern 不作为 recipe 输出
  检查 Applied Packaging 创造栏不暴露本地 package_pattern / packaged_processing_pattern
  检查 package_pattern_terminal 仍注册为 AE2 PartItem，且没有退回 BlockItem
  scripts/test-release-audit.ps1 的临时 fixture 补齐最小 recipe/source 输入
  scripts/test-release-audit.ps1 新增本地样板 recipe 输出、创造栏本地样板和终端 BlockItem 回退三个负例
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md
验证 PowerShell parser 解析 scripts/verify-release.ps1 和 scripts/test-release-audit.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-audit.ps1 成功，确认 valid fixture 退出 0，missing README、tampered metadata、local path leak、local pattern recipe output、creative local pattern 和 terminal block item fixture 均按预期失败
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireAssetContracts 成功，确认当前项目满足新增产品不变量审计
GameTest：已考虑。发现现有 runGameTestServer 与 PackageDataGameTests；本次只增强机械发布审计脚本、自测 fixture 和文档记录，不改变游戏行为、数据生成、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补齐发布 PNG 像素内容门禁：
  scripts/verify-assets.ps1 新增 RGBA PNG 像素解码
  保留既有必需 PNG、路径归类、RGBA header 和尺寸检查
  新增全透明 PNG 拒绝，避免无可见像素的资源进入发布候选
  新增整张单一 RGBA 像素拒绝，避免纯色占位图进入发布候选
  合法 AE2 part overlay mask 只要求存在透明像素与可见像素，不要求多色，避免误伤单色遮罩
  scripts/test-assets-audit.ps1 新增 transparent PNG fixture 和 single-color PNG fixture
  更新 README.md、CHANGELOG.md、docs/04-asset-spec.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md
验证 PowerShell parser 解析 scripts/verify-assets.ps1 和 scripts/test-assets-audit.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1 成功，确认 60 个发布 PNG 均包含可见非占位像素内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-assets-audit.ps1 成功，确认 valid fixture 退出 0，bad dimension、bad PNG header、transparent PNG、single-color PNG 和 missing required PNG fixture 均按预期失败
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功，确认 docs audit、asset audit、release audit、readiness、release plan、manifest 和 bundle 自测均可由聚合入口串行通过；开发中工作区 dirty，manifest/bundle 子测试的 clean-git fixture 按预期跳过
验证 .\gradlew.bat build --stacktrace 成功，刷新 release jar
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认机械发布审计、Asset resource audit、文档审计、manifest 生成/审计和 bundle 生成/审计仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer 与 PackageDataGameTests；本次只增强资产发布审计脚本、自测 fixture 和文档记录，不改变游戏行为、数据生成、资源文件、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补齐正式设计文档占位清理门禁：
  docs/03-detailed-design.md 补齐普通 processing pattern 的 AE2 可见输出语义
  明确普通 processing pattern 下 AE2 Pattern Provider / Planner 仍等待原输出 X
  明确装配室输出的包裹只是中间物流单元，不伪装为 X，也不把包裹内容登记为 ME 散装库存
  scripts/verify-docs.ps1 新增正式设计文档 unresolved placeholder 审计
  审计正式分类文档中的 TODO、FIXME、TBD、待定、待补充、等待 X 等占位
  docs/08-change-intake.md 和 docs/chat-summary.md 保留待输入/历史讨论语义，不纳入正式占位审计范围
  scripts/test-docs-audit.ps1 新增 unresolved placeholder fixture
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md
验证 PowerShell parser 解析 scripts/verify-docs.ps1 和 scripts/test-docs-audit.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功，确认正式设计文档不含 unresolved placeholder
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-docs-audit.ps1 成功，确认 valid fixture 退出 0，missing required path、broken markdown link 和 unresolved placeholder fixture 均按预期失败
验证 .\gradlew.bat build --stacktrace 成功，刷新 release jar
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功，确认 docs audit、asset audit、release audit、readiness、release plan、manifest 和 bundle 自测均可由聚合入口串行通过；开发中工作区 dirty，manifest/bundle 子测试的 clean-git fixture 按预期跳过
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认机械发布审计、Asset resource audit、文档审计、manifest 生成/审计和 bundle 生成/审计仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
验证提交后 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireCleanGit -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认 clean-git 下 release manifest 与 release bundle 可按当前 HEAD 生成并复验
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag 仍按预期失败，阻止 IN-001/IN-002 待输入和未冻结状态下创建发布 tag
GameTest：已考虑。发现现有 runGameTestServer 与 PackageDataGameTests；本次只补齐详细设计语义、文档审计脚本、自测 fixture 和文档记录，不改变游戏行为、数据生成、资源、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补齐发布资源源文件同步 release audit：
  scripts/verify-release.ps1 新增 Applied Packaging 发布资源同步审计
  审计范围为 src/main/resources 与 src/generated/resources 下的 assets/appliedpackaging/** 和 data/appliedpackaging/**
  对每个源码/生成资源要求 jar 内同名条目存在且 SHA-256 一致
  如 main/generated 中出现同名发布资源，审计会报告重复源路径，避免 Gradle 资源覆盖关系变成隐性风险
  scripts/test-release-audit.ps1 的有效 fixture 补齐模型 JSON、item texture 和 recipe 条目
  scripts/test-release-audit.ps1 新增 jar 内发布资源缺失和 jar 内发布资源过期两个负例
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md
验证 PowerShell parser 解析 scripts/verify-release.ps1 和 scripts/test-release-audit.ps1 成功
验证 git diff --check 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-audit.ps1 成功，确认 valid fixture 退出 0，missing README、stale jar README、tampered metadata、local path leak、language placeholder mismatch、stale jar language、missing jar release resource、stale jar release resource、local pattern recipe output、creative local pattern 和 terminal block item fixture 均按预期失败
验证 .\gradlew.bat build --stacktrace 成功，刷新 release jar
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireAssetContracts 成功，确认 115 个 Applied Packaging 发布资源与 jar 条目 SHA-256 一致
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功，确认 docs audit、asset audit、release audit、readiness、release plan、manifest 和 bundle 自测均可由聚合入口串行通过；开发中工作区 dirty，manifest/bundle 子测试的 clean-git fixture 按预期跳过
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认机械发布审计、Asset resource audit、文档审计、manifest 生成/审计和 bundle 生成/审计仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag 按预期失败，阻止 IN-001/IN-002 待输入和未冻结状态下创建发布 tag
GameTest：已考虑。发现现有 runGameTestServer 与 PackageDataGameTests；本次只增强机械发布审计脚本、自测 fixture 和文档记录，不改变游戏行为、数据生成、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补齐 jar 源文件同步 release audit：
  scripts/verify-release.ps1 新增 jar 内 README.md、CHANGELOG.md、LICENSE.md 与仓库源文件 SHA-256 同步审计
  scripts/verify-release.ps1 新增 jar 内 en_us.json、zh_cn.json 与 src/main/resources 源文件 SHA-256 同步审计
  scripts/test-release-audit.ps1 的临时 fixture 改为精确 UTF-8 写入，避免自测字节比对被 PowerShell 自动换行影响
  scripts/test-release-audit.ps1 的有效 fixture 补齐仓库根 README/CHANGELOG/LICENSE 和 jar 内语言文件
  scripts/test-release-audit.ps1 新增 jar 内 README 过期和 jar 内语言文件过期两个负例
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md
验证 PowerShell parser 解析 scripts/verify-release.ps1 和 scripts/test-release-audit.ps1 成功
验证 git diff --check 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-audit.ps1 成功，确认 valid fixture 退出 0，missing README、stale jar README、tampered metadata、local path leak、language placeholder mismatch、stale jar language、local pattern recipe output、creative local pattern 和 terminal block item fixture 均按预期失败
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireAssetContracts 成功，确认当前 jar 内 README/CHANGELOG/LICENSE 和 en_us/zh_cn 均与源文件同步
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功，确认 docs audit、asset audit、release audit、readiness、release plan、manifest 和 bundle 自测均可由聚合入口串行通过；开发中工作区 dirty，manifest/bundle 子测试的 clean-git fixture 按预期跳过，提交后需重跑覆盖
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认机械发布审计、Asset resource audit、文档审计、manifest 生成/审计和 bundle 生成/审计仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer 与 PackageDataGameTests；本次只增强机械发布审计脚本、自测 fixture 和文档记录，不改变游戏行为、数据生成、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补齐语言占位符 release audit：
  scripts/verify-release.ps1 新增英文/简体中文语言占位符一致性审计
  保留既有语言 key 对齐检查，并在共同 key 上比较 %s/%d 等格式占位符序列
  scripts/test-release-audit.ps1 的有效 fixture 增加带 %s 的语言项
  scripts/test-release-audit.ps1 新增语言占位符不一致负例
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md
验证 PowerShell parser 解析 scripts/verify-release.ps1 和 scripts/test-release-audit.ps1 成功
验证 git diff --check 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-audit.ps1 成功，确认 valid fixture 退出 0，missing README、tampered metadata、local path leak、language placeholder mismatch、local pattern recipe output、creative local pattern 和 terminal block item fixture 均按预期失败
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireAssetContracts 成功，确认当前项目满足语言 key 和占位符审计
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功，确认 docs audit、asset audit、release audit、readiness、release plan、manifest 和 bundle 自测均可由聚合入口串行通过；开发中工作区 dirty，manifest/bundle 子测试的 clean-git fixture 按预期跳过，提交后需重跑覆盖
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG/LICENSE 等打包内容
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认机械发布审计、Asset resource audit、文档审计、manifest 生成/审计和 bundle 生成/审计仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer 与 PackageDataGameTests；本次只增强机械发布审计脚本、自测 fixture 和文档记录，不改变游戏行为、数据生成、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补齐资产资源审计说明：
  AGENTS.md 明确 scripts/verify-assets.ps1 当前审计范围包含必需 PNG、已知路径、RGBA PNG header、可见非占位像素内容和尺寸规则
  docs/06-verification-release.md 明确发布 PNG 变更覆盖路径归类、RGBA header、可见非占位像素内容和尺寸规则
  保持新增需求和材质输入等待 docs/08-change-intake.md 中的 IN-001/IN-002 后续补充
验证 git diff --check 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
GameTest：已考虑。发现现有 runGameTestServer 与 PackageDataGameTests；本次只调整 agent 指令与发布验收文档说明，不改变游戏行为、数据生成、资源文件、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补强 tag readiness intake 负面状态门禁：
  scripts/verify-release-readiness.ps1 的 intake 行状态/验证要求现在会把阻塞、失败、未通过、不可、不能、blocked、failed 等负面状态视为 blocker
  scripts/test-release-readiness.ps1 新增 blocked intake state fixture，确认即使文档带有正向冻结信号，阻塞/失败 intake 行也不能通过 -RequireReadyForTag
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md 中的 readiness 规则说明
验证 PowerShell parser 解析 scripts/verify-release-readiness.ps1 和 scripts/test-release-readiness.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1 成功，确认 ready fixture 退出 0，blocked fixture、structural failure fixture、missing positive signals fixture 和 blocked intake state fixture 均退出 1
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 成功，当前正式文档仍报告 4 个非致命 blocker
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag 按预期失败，当前 IN-001/IN-002 与未冻结发布状态仍阻止最终 tag
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功，确认 blocked intake state fixture 已纳入聚合 readiness 自测；开发中工作区 dirty，manifest/bundle clean-git fixture 按预期跳过
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认机械发布审计、Asset resource audit、文档审计、manifest 生成/审计和 bundle 生成/审计仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer 与 PackageDataGameTests；本次只增强发布 tag readiness 脚本、自测 fixture 和文档记录，不改变游戏行为、数据生成、资源文件、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

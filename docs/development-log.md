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

最新进展：

```text
补齐包裹手动拆包与受伤拆包：
  新增 PackageUnpacker，统一把合法包裹内容展开为普通 ItemStack
  PackageItem 蹲下右键时拆开手中整叠同款包裹，优先把内容放入玩家背包，溢出按 Forge 玩家发物品逻辑掉落
  PackageEntity 受到伤害时按实体 ItemStack count 展开全部同款包裹内容并掉落到世界
  手动/受伤拆包仅在内容全部为 AEItemKey 时执行；包含 fluid 或未知 AEKey 时不消耗/销毁包裹，避免资源丢失
  marker item 渲染中心从 package front 外角 4x4 调整为距外边框 1px 的 4x4 标记框中心，保持 3x3 item 和 0.5px margin
  更新 docs/01-requirements.md、docs/03-detailed-design.md、docs/04-asset-spec.md、docs/assets/acceptance.md、docs/assets/asset-briefs/packages.md、docs/assets/contracts/package_items.yaml 和 docs/assets/reports/packages.md
新增 GameTest：
  shiftRightClickPackageUnpacksAllPackagesToPlayer
  damagedPackageEntityUnpacksContentsToWorld
验证 .\gradlew.bat compileJava --stacktrace 成功
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，124 个必需 GameTest 全部通过
尝试验证 .\gradlew.bat runClientSmoke --stacktrace；失败原因是当前已有 IntelliJ 启动的 appliedpackaging dev client 占用 run/New World 世界锁，quickPlay 无法进入世界，未能产出本轮可用截图。未终止用户的 IDE 客户端。
GameTest：已考虑并执行。本次修改涉及玩家 item use、实体受伤、掉落物生成和包裹内容提交语义，属于行为敏感变更；已新增并运行 GameTest 覆盖。
```

最新进展：

```text
修正 package_box face UV 与 marker 渲染方案：
  确认 package_box_pixel_v7 已提供 base_faces、band_masks 和 variants/<color>/package_box_<face>.png；当前发布资源继续使用 variants 中已合成的独立 face 贴图，不修改 PNG、不合并 atlas、不拆基础盒体与束带重叠层
  17 色 package_box/<color>.json 保持单个 10x10x8 cuboid，并为 north/south/east/west/up/down 每面声明 full-face uv [0,0,16,16]，让 10x8 或 10x10 独立贴图完整铺满对应 face
  顶层 <color>_package.json 增加 appliedpackaging:has_marker override；只有存在物品 marker 的包裹切入共享 builtin/entity renderer
  新增 PackageItemRenderer 和 PackageMarkerRenderer；renderer 根据 PackageItem 颜色渲染原 package_box 模型，并将 AEItemKey marker 以 3x3 尺寸居中叠加到前脸右下角 4x4 框内
  非物品 marker 暂不渲染贴片；包裹本体仍是 MC 常规模型 JSON，动态 marker 属于运行时 ItemStack 渲染，不能由静态 JSON 表达
  包裹 GUI display 调整为 rotation [30,225,0]、scale [0.5,0.5,0.5]，让物品栏视角更接近常规方块物品且正面朝左前
  scripts/verify-assets.ps1 改为检查 full-face uv 和 marker custom-render override；scripts/test-assets-audit.ps1 增加 cropped UV 与 missing marker override 负例
验证 .\gradlew.bat compileJava --stacktrace 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1 成功，确认 17 色 package_box 模型均使用 full-face uv [0,0,16,16]，且顶层包裹 item 均声明 marker custom-render override
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-assets-audit.ps1 成功，cropped UV 与 missing marker override 负例均按预期失败
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，最新截图 appliedpackaging-client-smoke-world-me_packager.png 确认包裹贴图不再错位，marked package 不再显示缺失模型，marker 贴片位于前脸右下角
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 git diff --check 成功，仅报告仓库当前 LF/CRLF 提示
GameTest：已考虑并执行。本次最终工作区包含包裹实体、完整打包流程和客户端模型/渲染相关改动；验证 .\gradlew.bat runGameTestServer --stacktrace 成功，122 个必需 GameTest 全部通过。
```

最新进展：

```text
修正 ME 打包机与普通机器渲染路径：
  ME 打包机方块属性改为 noOcclusion 且不作为 redstone conductor，避免 Create 风格透明外壳按完整实心方块遮挡内部光照
  客户端只给 ME 打包机注册 cutout_mipped 方块渲染层；动态 hatch 改回 Create renderer 的 solid，动态 tray 继续 cutout_mipped
  package_assembler、package buses、package_pattern_terminal block 和 package_pattern_terminal_base part 移除错误 render_type，普通不透明模型回到默认 solid
  package buses 与 package_pattern_terminal 方块使用 noOcclusion，避免薄模型按完整方块遮挡地面造成蓝色缺面
  ClientSmokeRunner 新增 appliedpackaging-client-smoke-world-all_machines.png，并在第二张世界截图前移动到机器排正前方，覆盖普通机器渲染检查
  scripts/verify-assets.ps1 新增普通不透明 block/part 模型不得声明 render_type 的门禁；scripts/test-assets-audit.ps1 新增 bad opaque model render_type 负例
  docs/04-asset-spec.md 与 docs/assets/acceptance.md 同步记录 Create 风格 packager 的静态/动态 render type 分界和普通模型 solid 规则
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1 成功，确认新增 opaque model render_type 门禁通过
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-assets-audit.ps1 成功，确认 bad opaque model render_type fixture 按预期失败
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，生成 appliedpackaging-client-smoke-world-me_packager.png 与 appliedpackaging-client-smoke-world-all_machines.png；人工查看确认打包机正面不再是整块黑洞，薄 bus/terminal 下方地面不再发蓝缺面
验证 run/logs/latest.log 中未发现 ERROR、Exception、Missing texture、missing model、Unable to load model、Could not load 或 ModelBakery 相关资源加载错误
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 git diff --check 成功，只有既有 LF/CRLF 工作区提示
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，122 个必需 GameTest 全部通过
验证 .\gradlew.bat build --stacktrace 成功
```

最新进展：

```text
修复包裹材质、包裹实体和 ME Packager smoke 验证：
  确认最新截图中的包裹贴图错误来自模型层手写 face uv；17 色 package_box 模型已恢复为 package_box_pixel_v7 源模型语义，不再在 faces 中声明 uv
  逐字节比对 C:\Users\warmt\Downloads\package_box_pixel_v7.zip 与仓库内 17 色 x 5 面 PNG，确认当前 PNG 本体未被改色、重采样或污染
  scripts/verify-assets.ps1 新增 17 色 package_box 模型门禁：检查 3D parent、cutout_mipped、10x10x8 bounds、face texture 绑定和不得声明显式 uv
  scripts/test-assets-audit.ps1 新增 explicit UV 负例，确认坏 package_box JSON 会被资产审计拒绝
  PackageEntity 改为 Create-style 独立 LivingEntity，注册实体属性，fromDroppedItem 沿用 Create 初速放大策略，实体保存 PackageItem NBT 并共用 item model 渲染
  PackageEntity 落地后无条件清零 Y 速度，避免包裹落地后继续慢速漂浮/弹动；实体尺寸固定为 10px x 8px，并允许准星选中和空手右键拿取
  PackageEntityRenderer 渲染 PackageEntity 自身的 PackageItem model，保持模型底部贴合实体底部
  ClientSmokeRunner 的世界截图场景现在放置 5 个不同颜色包裹实体并贴地排开，避免只靠半遮挡单包裹判断贴图和落地高度
验证 python 比对 package_box_pixel_v7.zip 与仓库 PNG 成功，85 张 face PNG 均逐字节一致
验证 rg -n '"uv"' src\main\resources\assets\appliedpackaging\models\item\package_box 无输出，17 色 package_box 模型均不再声明显式 uv
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1 成功，141 个 PNG 和 17 色 package_box 模型门禁均通过
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-assets-audit.ps1 成功，explicit UV 负例按预期失败
验证 .\gradlew.bat runGameTestServer --stacktrace 首次失败，暴露 PackageEntity 落地后仍有垂直速度残留
修复落地 Y 速度后验证 .\gradlew.bat runGameTestServer --stacktrace 成功，122 个必需 GameTest 全部通过
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，生成世界图和 6 张 GUI smoke 截图；世界图确认包裹实体可见、未埋入地面且无附魔闪光，日志未检出缺模型、缺贴图、parent loop、ERROR、FATAL、ClassCastException 或 crash
GameTest：已考虑并执行。本次改变 PackageEntity 继承、掉落物理、实体尺寸、右键交互和保存/同步语义，属于行为敏感变更；已运行 runGameTestServer 覆盖包裹实体和完整打包流程。
```

最新进展：

```text
修复 2026-07-05 客户端渲染与包裹实体物理回归：
  PackageEntity 改为继承 ItemEntity，沿用原版掉落实体重力、阻力、拾取延迟和生命周期；fromDroppedItem 不再放大初速，并在落地后清零极小垂直反弹速度，避免包裹慢慢飞或漂浮
  PackageEntity 兼容读取旧版 Package NBT，避免旧世界残留实体变成 Air 后被立刻清理
  PackageEntityRenderer 改回 ItemRenderer 的真实 item model 获取路径，并用 +7px Y 预位移抵消 ItemRenderer 内部 -0.5 变换，使 10x10x8 包裹模型底部贴合实体底部，不再埋进地面
  ME Packager 动态 hatch 和 Create 临时 block/item 模型改用 cutout_mipped；Create packager linked/iris 贴图有透明像素，solid 渲染会把透明区显示成黑洞
  17 色 package_box 模型 UV 恢复为整张贴图域 [0,0,16,16]；10x8/10x10 只作为 PNG 尺寸和资产审计规则，不作为 JSON UV 坐标
  修正错误的中间状态：之前把 PNG 像素尺寸误写进 JSON UV，导致包裹贴图被裁切错位；随后批量重写还误把 _transforms.json 当成颜色模型，形成 package_box parent loop
  _transforms.json 现在只保留 item display transforms，不再声明 parent、textures 或 elements；17 色 package_box/<color>.json 继续继承该 display 模板并各自声明真实贴图
  PackageEntityRenderer 改成 T extends ItemEntity 的泛型渲染器，只读取 entity.getItem()；避免拾取粒子路径用 appliedpackaging:package renderer 渲染 ItemEntity 语义对象时触发 PackageEntity 强转崩溃
  package_assembler、package buses 和 package_pattern_terminal block/part 模型补 render_type=cutout_mipped，避免透明像素或遮罩渲染异常
验证 .\gradlew.bat compileJava --stacktrace 成功
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，All 122 required tests passed
验证 .\gradlew.bat runClientSmoke --stacktrace 首次失败，暴露 package_box parent loop 与 PackageEntityRenderer 拾取粒子强转崩溃；修复后再次运行成功，run/logs/latest.log 未检出 parent loop、missing model、missing texture、ERROR、FATAL 或加载异常；截图 appliedpackaging-client-smoke-world-me_packager.png 确认 ME Packager 中心不再发黑，包裹掉落实体不再埋入地面、裁错贴图或显示为缺失模型
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1 成功，141 个 PNG 通过资源审计
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-assets-audit.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 assets/appliedpackaging/models 下 JSON 全量解析成功
验证 .\gradlew.bat build --stacktrace 成功
验证 git diff --check 成功；仅输出工作区 LF 将被 Git 转为 CRLF 的提示
GameTest：已考虑并运行。本次改变 PackageEntity 物理继承、掉落速度、落地状态和旧 NBT 迁移，属于行为敏感变更；修复前 GameTest 捕获到落地后仍有垂直反弹速度，修复后 runGameTestServer 全部通过。
```

最新进展：

```text
修复 2026-07-05 ME Packager Create 渲染与包裹实体表现：
  将 ME Packager 机器贴图和模型恢复为 build/reference/create 中的 Create Packager 原始资源，仅做 appliedpackaging namespace/path 重映射；不再对机器材质做自定义改色或亮度处理
  MePackagerRenderer 对齐 Create PackagerRenderer 的渲染策略：hatch 使用 solid，tray 使用 cutoutMipped，tray/package 位移、旋转和缩放沿用 Create 数值，动态渲染方向直接使用 network_side
  me_packager blockstate 改用 Create linked 模型，按 network_side 反推静态模型旋转，使世界方块外观与 AE 连接面一致
  ClientSmokeRunner 新增世界截图阶段，并在开发截图中临时 prime ME Packager 输出动画，便于验证 hatch/tray/package 的方向；该 runner 仍被 jar 排除
  包裹材质替换为 C:\Users\warmt\Downloads\package_box_pixel_v7.zip 版本；包裹 GUI transform 保持缩小并让正面朝左前
  PackageEntityRenderer 将模型 Y 偏移改为 -1px，使 y=1..9px 的包裹模型视觉上贴合 0..8px 实体碰撞箱
  packageEntitySettlesOnGroundWithoutHovering 改为第 40 tick 检查最终状态，不再因实体第 26 tick 提前落地而误判失败
  AE2 CPU -> Package Assembler GameTest 保留“CPU 已提交 + 装配室收到输入并产出包裹”的本 mod 流程断言，不再依赖 AE2 getRequestedAmount 的瞬时内部状态
  scripts/verify-assets.ps1 将 Create vault_front_small.png 归入 16x16 detail texture；packager_particle.png 与 vault_front_small.png 转为 RGBA PNG，视觉内容不重绘
验证 .\gradlew.bat compileJava --stacktrace 成功
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，122 个必需 GameTest 全部通过
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，进入 quick-play 单人世界并捕获世界图和 6 张 GUI smoke 截图
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1 成功，141 个 PNG 均通过 RGBA、尺寸和可见内容审计
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-assets-audit.ps1 成功
GameTest：已考虑并执行。本次修复涉及渲染状态、包裹实体视觉/碰撞对应和 AE2 CPU 推送流程断言；已运行 runGameTestServer 覆盖完整行为流程。
```

最新进展：

```text
接入 2026-07-05 包裹材质和 ME Packager 行为变更：
  使用 C:\Users\warmt\Downloads\package_box_pixel_v6.zip 替换 17 色包裹资源，包裹物品模型改为 10x10x8 package_box 三维模型，物品和丢出实体共用该模型渲染
  新增 appliedpackaging:package 实体类型、实体渲染器和 PackageItem 自定义掉落实体路径，参考 Create package entity 策略保留包裹 ItemStack/NBT
  ME Packager 临时切换到 Create Packager 风格 block/item 模型和贴图；packager_particle.png 已重存为 RGBA 以满足资产审计
  ME Packager 基础容量改为 1k/16 类型；容量升级后续再做
  ME Packager 新增 network_side 方块状态，放置时默认 facing 反向，潜行右键被点击面可切换连接面
  ME Packager 只通过 network_side 查询 AE2 MEStorage；无 MEStorage 时返回 NO_TARGET，不回落 Forge item handler / fluid handler
  非 network_side 面只暴露 2 槽普通 item capability：slot 0 输入合法包裹，slot 1 输出包裹
  输入槽有包裹时 server tick 自动尝试拆入所选 AE 网络；红石 pulse/cyclic 均在所选 AE 网络上执行
  新增/扩展 GameTest 覆盖包裹实体掉落、1k/16 容量、network_side capability 隔离、顶面 AE 网络打包、自动拆包、Forge item/fluid handler 反例，以及 ME 红石在 AE 网络上的完整流程
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 ResourceLocation/FMLJavaModLoadingContext deprecation warning
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1 成功，140 个 PNG 均通过 RGBA、尺寸和可见内容审计
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-assets-audit.ps1 成功
验证 asset JSON parse 成功，70 个 assets JSON 可解析
验证 .\gradlew.bat runData --stacktrace 成功
验证 .\gradlew.bat runGameTestServer --stacktrace 首次失败，原因是新 AE2 helper 未向 Drive 放入 storage cell，导致网络容量为 0
修复后验证 .\gradlew.bat runGameTestServer --stacktrace 成功，118 个必需 GameTest 全部通过
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，进入 quick-play 单人世界并捕获 6 张客户端 smoke 截图；日志未出现缺模型/缺贴图错误，仅出现 package_box 10x10 贴图限制 mip level 的普通警告
GameTest：已考虑并执行。本次行为涉及实体、能力暴露、MEStorage 目标选择、红石和自动拆包；已新增/扩展 PackageDataGameTests，并运行 runGameTestServer 覆盖完整打包流程。
```

最新进展：

```text
补强 tag readiness 类型目标族门禁：
  scripts/verify-release-readiness.ps1 现在读取 intake 表的 类型 列，并在迁移目标路径存在且不越界后执行类型目标族校验
  需求类 intake 迁移目标必须落在 docs/01-requirements.md、docs/02-system-architecture.md、docs/03-detailed-design.md、docs/05-implementation-plan.md、docs/06-verification-release.md 或 docs/07-references.md
  材质类 intake 迁移目标必须落在 docs/04-asset-spec.md、docs/assets/acceptance.md、docs/assets/palette.md、docs/assets/asset-briefs/、docs/assets/contracts/、docs/assets/previews/、docs/assets/reports/ 或 src/main/resources/assets/appliedpackaging/
  未知类型暂时只执行既有路径解析、存在性和边界检查，避免阻断未来新增分类
  scripts/test-release-readiness.ps1 新增 misclassified requirement migration target 和 misclassified asset migration target 两个负例
  readiness 自测的错位目标断言改为 ASCII 前缀匹配，避免子进程控制台编码把 需求/材质 输出成 ?? 后造成误判
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md 中的 readiness 规则说明
验证 PowerShell parser 解析 scripts/verify-release-readiness.ps1 和 scripts/test-release-readiness.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1 成功，确认 ready fixture 退出 0，blocked、structural failure、missing positive signals、blocked intake state、unresolved migration target、missing migration target path、traversal migration target path、misclassified requirement migration target 和 misclassified asset migration target fixture 均按预期退出
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 成功，当前正式文档仍报告 IN-001/IN-002 待输入等 4 个非致命 blocker
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag 按预期失败，当前 IN-001/IN-002 与未冻结发布状态仍阻止最终 tag
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 git diff --check 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功，确认 docs audit、asset audit、release audit、readiness、release plan、manifest 和 bundle 自测均可由聚合入口串行通过；开发中工作区 dirty，manifest/bundle clean-git fixture 按预期跳过
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认机械发布审计、Asset resource audit、文档审计、manifest 生成/审计和 bundle 生成/审计仍可串联通过；开发中 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer 与 PackageDataGameTests；本次只增强发布 tag readiness 脚本、自测 fixture 和文档记录，不改变游戏行为、数据生成、资源文件、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补强 tag readiness intake 迁移目标门禁：
  scripts/verify-release-readiness.ps1 现在同时检查 intake 表的状态、迁移目标和验证要求三列
  迁移目标仍为待输入、待判定、等待、阻塞、失败、未通过、不可、不能、blocked、failed 等负面状态时会阻止 -RequireReadyForTag
  scripts/test-release-readiness.ps1 新增 unresolved migration target fixture，确认未迁移到正式分类文档的 intake 项不能通过最终 tag 门禁
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md 中的 readiness 规则说明
验证 PowerShell parser 解析 scripts/verify-release-readiness.ps1 和 scripts/test-release-readiness.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1 成功，确认 ready fixture 退出 0，blocked fixture、structural failure fixture、missing positive signals fixture、blocked intake state fixture 和 unresolved migration target fixture 均按预期退出
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 成功，当前正式文档仍报告 4 个非致命 blocker，且 IN-001/IN-002 输出中包含 migrationTarget='待判定'
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功，确认 unresolved migration target fixture 已纳入聚合 readiness 自测；开发中工作区 dirty，manifest/bundle clean-git fixture 按预期跳过
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认机械发布审计、Asset resource audit、文档审计、manifest 生成/审计和 bundle 生成/审计仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer 与 PackageDataGameTests；本次只增强发布 tag readiness 脚本、自测 fixture 和文档记录，不改变游戏行为、数据生成、资源文件、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补强 tag readiness 迁移目标路径存在性门禁：
  scripts/verify-release-readiness.ps1 新增迁移目标路径解析和存在性检查
  已迁移的 intake 行必须在迁移目标列指向 AGENTS.md、README.md、CHANGELOG.md、docs/... 或 src/main/resources/... 下的仓库内既有文件
  scripts/test-release-readiness.ps1 新增 missing migration target path fixture，确认 docs/99-missing.md 这类不存在目标不能通过 -RequireReadyForTag
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md 中的 readiness 规则说明
验证 PowerShell parser 解析 scripts/verify-release-readiness.ps1 和 scripts/test-release-readiness.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1 成功，确认 ready fixture 退出 0，blocked fixture、structural failure fixture、missing positive signals fixture、blocked intake state fixture、unresolved migration target fixture 和 missing migration target path fixture 均按预期退出
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 成功，当前正式文档仍报告 4 个非致命 blocker
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag 按预期失败，当前 IN-001/IN-002 与未冻结发布状态仍阻止最终 tag
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功，确认 missing migration target path fixture 已纳入聚合 readiness 自测；开发中工作区 dirty，manifest/bundle clean-git fixture 按预期跳过
验证 git diff --check 成功
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认机械发布审计、Asset resource audit、文档审计、manifest 生成/审计和 bundle 生成/审计仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer 与 PackageDataGameTests；本次只增强发布 tag readiness 脚本、自测 fixture 和文档记录，不改变游戏行为、数据生成、资源文件、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
补强 tag readiness 迁移目标路径边界：
  scripts/verify-release-readiness.ps1 现在拒绝迁移目标路径中的 .. 父级遍历段
  同时通过 GetFullPath 确认迁移目标解析后的绝对路径仍位于仓库根目录下
  scripts/test-release-readiness.ps1 新增 traversal migration target path fixture，确认 docs/../docs/01-requirements.md 即使最终指向既有文件也不能通过 -RequireReadyForTag
  更新 AGENTS.md、README.md、CHANGELOG.md、docs/05-implementation-plan.md、docs/06-verification-release.md 和 docs/08-change-intake.md 中的 readiness 规则说明
验证 PowerShell parser 解析 scripts/verify-release-readiness.ps1 和 scripts/test-release-readiness.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1 成功，确认 ready fixture 退出 0，blocked fixture、structural failure fixture、missing positive signals fixture、blocked intake state fixture、unresolved migration target fixture、missing migration target path fixture 和 traversal migration target path fixture 均按预期退出
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 成功，当前正式文档仍报告 4 个非致命 blocker
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag 按预期失败，当前 IN-001/IN-002 与未冻结发布状态仍阻止最终 tag
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1 成功，确认 traversal migration target path fixture 已纳入聚合 readiness 自测；开发中工作区 dirty，manifest/bundle clean-git fixture 按预期跳过
验证 git diff --check 成功
验证 .\gradlew.bat build --stacktrace 成功，刷新发布 jar 内 README/CHANGELOG
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 成功，确认机械发布审计、Asset resource audit、文档审计、manifest 生成/审计和 bundle 生成/审计仍可串联通过；提交前 manifest 记录 clean=false 属于预期状态
GameTest：已考虑。发现现有 runGameTestServer 与 PackageDataGameTests；本次只增强发布 tag readiness 脚本、自测 fixture 和文档记录，不改变游戏行为、数据生成、资源文件、菜单、网络、事务或服务端加载，因此未新增、扩展或运行 GameTest。最终候选发布预设仍会运行 runGameTestServer。
```

最新进展：

```text
调整玩家手动拆包为整叠拆包：
  PackageUnpacker.unpackStackToPlayer 按手中 ItemStack count 展开每包内容，成功时消耗整叠同款包裹
  PackageItem.use 成功后返回玩家当前手中物品，避免 Forge 发物品逻辑把输出放回原手槽后又被旧空包裹栈覆盖
  手动拆包仍只接受全部内容可转换为普通 ItemStack 的包裹；包含 fluid 或未知 AEKey 时不消耗包裹
  更新 R14 与详细设计中的手动拆包语义，明确蹲下右键拆开手中的整叠同款包裹
  扩展 GameTest shiftRightClickPackageUnpacksAllPackagesToPlayer，覆盖 count=2 的包裹栈并断言输出总量翻倍
验证 .\gradlew.bat compileJava --stacktrace 成功
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，124 个必需 GameTest 全部通过
验证 .\gradlew.bat build --stacktrace 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 git diff --check 成功，仅报告仓库当前 LF/CRLF 提示
GameTest：已考虑并执行。本次改变玩家 item use 的提交语义与整叠内容展开数量，属于行为敏感变更；已扩展并运行 PackageDataGameTests。
```

最新进展：

```text
调整 ME Packager 动画裁切方案：
  撤回上一版顶点裁剪后，改为动画 active 期间使用单独 immediate render pass
  客户端初始化阶段预先为主 RenderTarget 启用 stencil，避免世界渲染中途重建 framebuffer
  MePackagerRenderer 在动画期间先写入 1x1x1 方块体积的不可见 stencil mask，再在 stencil test 下立即 flush 动态 hatch、tray 和包裹
  动态模型仍使用原 Create partial 几何、RenderType.solid / cutout_mipped 和原 item renderer，不再修改顶点或模型 UV
  stencil mask 写入时关闭 color/depth write，仅使用当前世界 depth test；动态 pass 恢复正常 color/depth write，完成后清理 stencil 并恢复 depth/cull 状态
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，进入 quick-play 单人世界并捕获 world-me_packager、world-all_machines 和 6 张 GUI 截图
验证 run/logs/latest.log 未发现 ERROR、FATAL、Exception、Crash、missing texture、Missing model、Unable to load model、GL_INVALID 或 framebuffer/stencil 关键错误
人工查看 run/screenshots/appliedpackaging-client-smoke-world-me_packager.png 与 world-all_machines.png，确认 ME Packager 无中间黑块，动画 pass 未出现完全不渲染回归
GameTest：已考虑。本次只调整客户端渲染 pass、stencil 状态和资产规格记录，不改变服务端事务、红石、MEStorage、实体物理或数据结构，因此未新增或运行 GameTest。
```

最新进展：

```text
修正 ME Packager 裁切范围与静止包裹显示：
  动画裁切 pass 缩小为 tray 与包裹，hatch/iris/链接口继续走普通 block entity render pass，避免边缘链接器被 stencil mask 意外隐藏
  ME Packager 静止时 getRenderedBox 不再套用动画半程隐藏规则
  静止显示栈改为输入槽合法包裹优先、输出槽包裹其次、renderedBox 缓存兜底
  输入槽或输出槽在无动画时变化会刷新 renderedBox 并同步 block update
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，进入 quick-play 单人世界并捕获 world-me_packager、world-all_machines 和 6 张 GUI 截图
人工查看 run/screenshots/appliedpackaging-client-smoke-world-me_packager.png 与 world-all_machines.png，确认边缘链接器未被裁切隐藏，ME Packager 内当前包裹可见
验证 run/logs/latest.log 未发现 FATAL、Exception、Crash、missing texture、Missing model、Unable to load model、Failed to read Screen JSON、Failed to find Screen JSON、GL_INVALID、framebuffer 或 stencil 关键错误；runClientSmoke 开头出现旧 latest/debug log 文件被占用无法删除的 logger ERROR，但客户端仍完整启动、截图并正常退出
GameTest：已考虑。本次改变客户端展示栈选择与 block entity 同步，不改变打包/拆包事务结果，因此未新增或运行 GameTest。
```

最新进展：

```text
基于 AE2 Screen 重做 ME Packager GUI：
  调研 AE2 1.20.1 / 1.21.1 / latest 后，采用 AE2 `UpgradeableScreen`、`UpgradeableMenu`、`ScreenStyle` 和 `UpgradesPanel`，不再手绘完整 Screen
  ME Packager 菜单改为 AE2 upgradeable menu，右侧 6 格升级槽支持红石卡、容量卡和加速卡
  新增 45 格 AE2 GenericStack contentFilter，GUI 默认启用 2 行，最多 3 张容量卡各解锁 1 行，未启用行由 AE2 OptionalFakeSlot 控制渲染/交互
  新增包裹名称、marker 槽、颜色弹窗、过滤应用模式、激活模式和阻挡模式；marker 槽物品优先作为输出 marker
  红石卡未安装时有效逻辑固定为有红石信号时激活；安装红石卡后可切换高信号、低信号、总是、脉冲和关闭；加速卡降低持续激活间隔
  容量元件槽只接受 AE2 16k/64k/256k storage component，容量卡只解锁过滤行
  非潜行右键保留快速放入包裹/取出输出；无快速动作时通过 NetworkHooks 打开 GUI
  新增 AE2 style JSON `assets/ae2/screens/appliedpackaging/me_packager.json`，背景贴图使用 `assets/appliedpackaging/textures/gui/mepackager.png`
  更新 GameTest 覆盖红石卡门槛、激活模式循环、容量卡过滤行解锁和默认高信号自动拆包语义
  更新资产审计规则，允许并要求 256x256 ME Packager GUI atlas
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，125 个必需 GameTest 全部通过
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，进入 quick-play 单人世界并捕获 world-me_packager、world-all_machines 和 6 张 GUI 截图
人工查看 run/screenshots/appliedpackaging-client-smoke-me_packager.png，确认 AE 左工具栏、右升级面板、5 行过滤区、默认 2 行启用状态和玩家背包可见
验证 run/logs/latest.log 未发现 ERROR、FATAL、Exception、Crash、missing texture、Missing model、Unable to load model、Failed to read Screen JSON、Failed to find Screen JSON、GL_INVALID、framebuffer 或 stencil 关键错误
GameTest：已考虑并执行。本次改变 ME Packager 菜单、升级卡、红石激活、过滤行、自动拆包和拆包阻挡语义，属于行为敏感变更；已扩展并运行 PackageDataGameTests。
```

最新进展：

```text
修正 ME Packager AE2 GUI 对齐与红石语义：
  按 mepackager.png 贴图框重排 ScreenStyle 槽位：容量元件过滤器移到容器区上方框，包裹输入/输出口移到下方容器框，marker 物品槽移到包裹配置区右侧框
  颜色选择器改为包裹配置区左侧小按钮，不再使用 16x16 工具栏按钮覆盖 marker/slot 区域
  打包激活按钮文案改为打包语义；红石卡/红石模式只控制自动打包
  输入槽存在合法包裹时自动拆包不受红石模式限制，仍受拆包过滤、阻挡模式、目标容量和目标在线状态约束
  新增 GameTest mePackagerRedstoneNeverOnlyStopsPacking，覆盖关闭打包时仍可拆包且不会随后自动重新打包
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，126 个必需 GameTest 全部通过
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，进入 quick-play 单人世界并捕获 world-me_packager、world-all_machines 和 6 张 GUI 截图
人工查看 run/screenshots/appliedpackaging-client-smoke-me_packager.png，确认颜色小按钮、marker 槽、容量元件过滤器和包裹输入/输出口已对齐到贴图目标框
GameTest：已考虑并执行。本次改变自动 tick 红石 gate 与拆包行为，属于行为敏感变更；已扩展并运行 PackageDataGameTests。
```

最新进展：

```text
修正 ME Packager 动画期间链接面短暂发黑：
  动画 stencil mask 仍只约束 tray 与包裹，不裁剪 hatch/iris/链接口等固定视觉
  mask 根据当前链接方向在链接面内收 1px，避免动态 tray/包裹 pass 覆盖透明链接器背后的静态视觉
  其余五个方向仍保留原 1x1x1 方块体积裁剪边界，继续隐藏方块外裸露动画
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，进入 quick-play 单人世界并捕获 world-me_packager、world-all_machines 和 6 张 GUI 截图；world 截图前会 prime ME Packager 动画状态
人工查看 run/screenshots/appliedpackaging-client-smoke-world-me_packager.png 与 world-all_machines.png，确认 ME Packager 链接面和内部包裹可见，未复现链接背后短暂黑块
验证 run/logs/latest.log 未发现 ERROR、FATAL、Exception、Crash、missing texture、Missing model、Unable to load model、Failed to read Screen JSON、Failed to find Screen JSON、GL_INVALID、framebuffer 或 stencil 关键错误；runClientSmoke 开头出现旧 latest/debug log 文件被占用无法删除的 logger ERROR，但客户端仍完整启动、截图并正常退出
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 git diff --check 成功，仅报告仓库当前 LF/CRLF 提示
GameTest：已考虑。本次只调整客户端渲染裁剪体积和资产规格记录，不改变服务端事务、红石、MEStorage、实体物理或数据结构，因此未新增或运行 GameTest。
```

最新进展：

```text
修正 ME Packager 动画期间链接口黑面残留：
  确认 hatch_closed/open 的 iris 贴图存在透明像素，模型 JSON 也声明 cutout_mipped
  MePackagerRenderer 的 hatch/iris 渲染层从 solid 改为 cutout_mipped，避免透明像素在动画期间被 solid 路径写成黑面
  tray/package 继续使用单独 stencil immediate pass；hatch/iris/链接口仍不进入裁剪 pass
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，进入 quick-play 单人世界并捕获 world-me_packager、world-all_machines 和 6 张 GUI 截图；world 截图前会 prime ME Packager 动画状态
人工查看 run/screenshots/appliedpackaging-client-smoke-world-me_packager.png 与 world-all_machines.png，确认动画期间链接口和内部包裹正常可见，未复现黑面
验证 run/logs/latest.log 未发现 ERROR、FATAL、Exception、Crash、missing texture、Missing model、Unable to load model、Failed to read Screen JSON、Failed to find Screen JSON、GL_INVALID、framebuffer 或 stencil 关键错误
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 git diff --check 成功，仅报告仓库当前 LF/CRLF 提示
GameTest：已考虑。本次只调整客户端渲染层和资产规格记录，不改变服务端事务、红石、MEStorage、实体物理或数据结构，因此未新增或运行 GameTest。
```

最新进展：

```text
修正 ME Packager 入包动画后半段链接口仍发黑：
  根因不是 hatch/iris 透明像素，而是 getRenderedBox 在动画半程返回空栈；拆包/入包动画后半段包裹被隐藏，链接口只剩 tray/内部黑面
  getRenderedBox 改为动画 active 期间始终返回 renderedBox，动画结束后由现有 inward 清理逻辑清空显示栈
  ClientSmokeRunner 的 ME Packager 世界截图改为 prime 入包动画后半段，覆盖此前绕过的黑面时机；runner 仍被 jar 排除
验证：待执行 compileJava、runClientSmoke、日志扫描、文档审计和 diff 空白检查
GameTest：已考虑。本次只调整客户端视觉显示栈和开发截图覆盖，不改变服务端事务、红石、MEStorage、实体物理或数据结构，计划不新增 GameTest。
```

最新进展：

```text
修正 ME Packager GUI 对齐与容量行视觉：
  按 mepackager.png 贴图重新对齐 ScreenStyle 槽位和标题：过滤区、玩家物品栏、hotbar、包裹名称输入框均贴合背景框
  移除 Package 与 Container 区域标题，只保留 ME Packager、Filter 和 Inventory 文本
  颜色选择器改为只在左侧小按钮中心绘制 6x6 色块，marker 槽为空时不再绘制占位图标
  容量卡解锁的可选过滤行不再沿用 AE2 1.20.1 OptionalFakeSlot 旧底图；改为按 AE2 高版本 slot background 颜色手动绘制，并使用新版 disabled alpha 0.2
验证 .\gradlew.bat compileJava --stacktrace 成功
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，进入 quick-play 单人世界并捕获 world-me_packager、world-all_machines 和 6 张 GUI 截图
人工查看 run/screenshots/appliedpackaging-client-smoke-me_packager.png，确认过滤区、玩家物品栏、hotbar、包裹名称输入框、6x6 色块和容量行新式淡化效果均已对齐
验证 run/logs/latest.log 未发现 ERROR、FATAL、Exception、Crash、missing texture、Missing model、Unable to load model、Failed to read Screen JSON、Failed to find Screen JSON、GL_INVALID、framebuffer 或 stencil 关键错误
GameTest：已考虑。本次只调整客户端 GUI 布局和 slot 背景绘制，不改变服务端事务、红石、MEStorage、过滤判定或物品移动语义，因此未新增或运行 GameTest。
```

最新进展：

```text
微调 ME Packager 包裹名称输入框与颜色选择弹层：
  包裹名称输入框从 x=10,width=93 调整为 x=11,width=89，使其在名称区域内左右留白一致，保持 12px 高度和上下边距
  颜色选择不再把每个色块注册成普通 renderable widget，改为 Screen 最后绘制的前景弹层，避免被 AE2/Vanilla tooltip 覆盖
  颜色弹层打开时优先拦截鼠标点击、拖拽、滚轮、字符输入和按键；点击色块会选择颜色，点击按钮或外部会关闭弹层且不把事件透传到底层 slot
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，进入 quick-play 单人世界并捕获 world-me_packager、world-all_machines 和 6 张 GUI 截图
人工查看 run/screenshots/appliedpackaging-client-smoke-me_packager.png，确认包裹名称输入框短于上一版且基础 GUI 布局未回退
验证 run/logs/latest.log 未发现 ERROR、FATAL、Exception、Crash、missing texture、Missing model、Unable to load model、Failed to read Screen JSON、Failed to find Screen JSON、GL_INVALID、framebuffer 或 stencil 关键错误；runClientSmoke 开头出现旧 latest/debug log 文件被占用无法删除的 logger ERROR，但客户端仍完整启动、截图并正常退出
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1 成功
GameTest：已考虑。本次只调整客户端 GUI 布局、前景绘制顺序和输入事件拦截，不改变服务端事务、红石、MEStorage、过滤判定或物品移动语义，因此不新增 GameTest；用客户端截图烟测验证。
```

最新进展：

```text
收敛 ME Packager 输入、输出与过滤状态机语义：
  红石模式只影响打包；拆包输入改为 external capability / 快速右键提交时立即验证并拆包，不再把包裹暂存到输入槽
  ME Packager 增加 idle / working 状态，working 区分 packing 与 unpacking；工作期间拒绝新输入，打包触发会排队到工作结束后再尝试
  打包先从 MEStorage 抽取并写入 workingStack，动画结束后无空闲间隙写入唯一输出槽；拆包先提交目标 MEStorage 插入，再播放拆包动画
  拆包输入同时检查包裹内容过滤、filter mode、当前颜色、marker、输出槽为空、目标可完整接收和阻挡模式；不满足则 capability 直接拒绝插入
  内容过滤改为 AEKey allowlist / denylist 语义，ghost amount 不限制普通打包数量；反转卡只反转内容过滤，不反转颜色或 marker 门禁
  保留 exact package / encoded pattern 路径的旧数量匹配能力，避免影响样板精确解码
  注册 ME Packager 反转卡升级，并补充 working 状态提示语言 key
  扩展 GameTest 覆盖 capability 直接拆包、颜色/marker/内容过滤组合、反转卡、工作中拒绝输入、打包动画结束后才进入输出槽，以及真实 AE2 Interface 往返流程
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，129 个必需 GameTest 全部通过
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 git diff --check 成功，仅报告仓库当前 LF/CRLF 提示
GameTest：已考虑并执行。本次改变 ME Packager 输入 capability、MEStorage 事务提交顺序、过滤语义、升级卡语义、红石触发和工作状态机，属于行为敏感变更；已扩展并运行 PackageDataGameTests。
```

最新进展：

```text
修正 ME Packager GUI shift-click 输入和工作进度显示：
  GUI 隐藏 inputSlot 改为不接受菜单放入或取出，只保留旧存档/内部兼容用途
  玩家背包内包裹 shift-click 改为调用与外部 capability 相同的直接拆包入口，每次最多消耗 1 个包裹，工作态期间直接拒绝且不写入 inputSlot
  菜单同步 workingOperation 和剩余动画 tick，ME Packager GUI 在工作期间于 marker 与输出槽之间绘制进度条
  新增 GameTest mePackagerMenuShiftClickUnpacksOnePackageAndRejectsWhileWorking，覆盖 2 个包裹 shift-click 只拆 1 个、inputSlot 保持为空、working 期间第二次 shift-click 被拒绝
  ClientSmokeRunner 中 ME Packager 连接面截图用的 AE2 cable 从 west 改到 south，避免覆盖 Package Assembler smoke 目标
验证 .\gradlew.bat compileJava --stacktrace --rerun-tasks 成功，仅既有 deprecation warning
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，130 个必需 GameTest 全部通过
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 git diff --check 成功，仅报告仓库当前 LF/CRLF 提示
验证 .\gradlew.bat runClientSmoke --stacktrace 首次失败，原因是开发截图用 AE2 cable 覆盖了 Package Assembler smoke 目标
修复 smoke 连接面后验证 .\gradlew.bat compileJava --stacktrace 成功
复跑 .\gradlew.bat runClientSmoke --stacktrace 成功，进入 quick-play 单人世界并捕获 world-me_packager、world-me_packager_link、world-all_machines 和 6 张 GUI 截图
GameTest：已考虑并执行。本次改变 ME Packager 菜单 shift-click 物品移动、隐藏 inputSlot 输入门禁、工作态拒绝输入和菜单工作状态同步，属于行为敏感变更；已新增并运行 PackageDataGameTests。
```

最新进展：

```text
修正 ME Packager 链接口动画黑面排查方向：
  对照 build/reference/create 的 PackagerRenderer，确认 Create 动态 hatch 使用 solid，动态 tray 使用 cutout_mipped
  MePackagerRenderer 恢复 hatch solid pass，避免继续偏离 Create 原始动态渲染策略
  移除 stencil mask 在 network_side 方向额外向内收 1px 的裁剪；该裁剪会让链接口后方露出静态内部暗面，属于过度裁剪
  docs/04-asset-spec.md 同步记录：动态裁剪 mask 覆盖完整方块体积，不得再对 network_side 做内缩
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
未运行 runClientSmoke：本轮按反馈停止使用无意义客户端 smoke，从渲染代码和 Create 对照实现修正。
GameTest：已考虑。本次只调整客户端 BlockEntityRenderer 渲染 pass、stencil mask 范围和资产规格记录，不改变服务端事务、MEStorage、红石、菜单或物品移动语义，因此不新增或运行 GameTest。
```

最新进展：

```text
修正 ME Packager 链接口黑面根因：
  对照 Create packager blockstate 与 renderer，确认原版静态外壳朝向和动态 hatch/tray/package 朝向必须配对；本项目此前用 network_side 旋转整个静态 Create 外壳，会让 AE 连接面成为 Create 开口，但动态 partial 不一定渲在该面，表现为链接面只剩透明开口和内部暗面。
  me_packager blockstate 改为所有 network_side 变体都按 facing 旋转 Create linked 外壳；network_side 只保留为 AE 连接方向，正式单面连接视觉等待后续独立 overlay 或新模型。
  MePackagerRenderer 的包裹动画口继续按 facing 反方向派生，与静态 Create 外壳开口保持一致。
  MePackagerBlockEntity 调整包裹显示半程：打包外送只在动画前半段显示包裹，拆包入内只在动画后半段显示包裹，避免打包表现成包裹缩进机器。
  docs/04-asset-spec.md 同步记录：不得用 network_side 旋转整个临时 Create 外壳，否则会复现链接面缺少动态补面导致的发黑。
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 me_packager blockstate JSON 可解析
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 git diff --check 成功，仅报告仓库当前 LF/CRLF 提示
未运行 runClientSmoke：本轮按反馈从渲染代码和 Create 对照路径定位，不使用客户端 smoke 作为判断依据。
GameTest：已考虑。本次调整 blockstate 资源、客户端渲染方向、包裹显示半程和资产规格记录，不改变服务端 MEStorage 事务、红石触发、菜单或物品移动语义，因此不新增或运行 GameTest。
```

最新进展：

```text
修正上一轮 ME Packager 渲染方向误判：
  恢复 me_packager blockstate 按 network_side 旋转 Create linked 外壳，使背面方向继续与 AE 线缆连接方向一致。
  MePackagerRenderer 将 network_side 视为背面，包裹动画口改为 network_side.getOpposite()，正面负责显示 hatch/tray/package 动画。
  背面无论是否播放动画都额外 immediate 渲染 closed hatch cover，并在动画前写入深度，使从背面看时内部 tray/package 和正面开口效果被背面遮挡。
  getRenderedBox 恢复 Create 半程语义：拆包入内前半段显示输入包裹，打包外送后半段显示输出包裹，撤回上一轮反向半程判断。
  docs/04-asset-spec.md 同步更正临时 Create 模型约束：network_side 决定背面/AE 连接面，正面动画在反面，背面必须补 cover。
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 me_packager blockstate JSON 可解析
未运行 runClientSmoke：本轮继续按反馈从渲染代码和模型职责定位，不使用客户端 smoke 作为判断依据。
GameTest：已考虑。本次只调整客户端渲染、blockstate 资源和资产规格记录，不改变服务端 MEStorage 事务、红石触发、菜单或物品移动语义，因此不新增或运行 GameTest。
```

最新进展：

```text
修正 ME Packager 水平静态外壳开口方向：
  确认 appliedpackaging:block/me_packager_create/block_linked 未旋转时 Create 外壳开口朝 north；上一轮 blockstate 把开口直接对齐到 network_side，导致背面/线缆面和正面动画口仍有半圈反向。
  调整 me_packager blockstate 的水平 network_side 映射，使静态外壳开口朝 network_side.getOpposite()，背面仍与 AE 线缆连接方向一致。
  docs/04-asset-spec.md 同步记录：network_side 决定背面，静态开口和包裹动画口都必须朝 network_side.getOpposite()。
验证 .\gradlew.bat compileJava --stacktrace 成功，任务均 up-to-date
验证 me_packager blockstate JSON 可解析
未运行 runClientSmoke：本轮继续按反馈从 blockstate/model 坐标关系定位。
GameTest：已考虑。本次只调整客户端资源 blockstate 与资产规格记录，不改变服务端 MEStorage 事务、红石触发、菜单或物品移动语义，因此不新增或运行 GameTest。
```

最新进展：

```text
修正 ME Packager 双背面与动画朝向：
  用户截图确认上一轮仍把静态开口和动画放在 network_side.getOpposite()，导致对侧看起来像第二个背面，动画也朝向错误对侧。
  新增 me_packager_create/back_cover.json，使用 Create vault_front_small 灰色面板作为专用背板；背板不再复用 hatch_closed，避免背面出现第二个 hatch/工作口。
  MePackagerRenderer 改为：包裹动画口使用 network_side；对侧 network_side.getOpposite() 始终渲染 back_cover 并写入深度，用于从背侧遮挡内部动画。
  me_packager blockstate 水平映射恢复为静态 Create 开口朝 network_side，使静态开口、hatch/tray/package 动画和 AE 连接方向一致。
  docs/04-asset-spec.md 同步改为：network_side 是链接/工作侧，背板在对侧，back_cover 不得复用 hatch/iris 模型。
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 me_packager blockstate JSON 与 back_cover model JSON 可解析
未运行 runClientSmoke：本轮继续按反馈从 blockstate/model/renderer 关系定位。
GameTest：已考虑。本次只调整客户端渲染、资源 JSON 和资产规格记录，不改变服务端 MEStorage 事务、红石触发、菜单或物品移动语义，因此不新增或运行 GameTest。
```

最新进展：

```text
修正 ME Packager back_cover 位置和 missing model：
  根据用户反馈，back_cover 应当在 network_side 连接面位置无条件渲染，用于挡住动画期间会变黑的输入口；上一轮误放在 network_side.getOpposite()。
  MePackagerRenderer 将 back_cover 渲染面改回 network_side，包裹动画口仍使用 network_side。
  AppliedPackagingClient 在 ModelEvent.RegisterAdditional 中注册 BACK_COVER_MODEL，修复动态 renderer 获取未 bake 模型导致的紫黑 missing model 方块。
  docs/04-asset-spec.md 同步记录：back_cover 必须注册为 additional model，并且在 network_side 连接面渲染。
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 me_packager blockstate JSON 与 back_cover model JSON 可解析
未运行 runClientSmoke：本轮继续按反馈从动态模型注册和 renderer 面位置定位。
GameTest：已考虑。本次只调整客户端渲染、additional model 注册、资源 JSON 和资产规格记录，不改变服务端 MEStorage 事务、红石触发、菜单或物品移动语义，因此不新增或运行 GameTest。
```

最新进展：

```text
将 ME Packager 连接面遮挡改为静态模型补面：
  根据用户反馈，连接面遮挡不是靠 BE renderer 额外 draw partial 能稳定解决；应当从模型层在该面补一块背板，并把原透明工作面后的遮挡面略微内缩。
  在 me_packager_create/block.json 中新增 network_side_cover 元素，位于默认 north 工作口后方 z=0.08..0.1；blockstate 旋转后该元素跟随静态开口落到当前 network_side，用于挡住动画期间会变黑的输入口。
  移除 MePackagerRenderer 中动态 back_cover 渲染、BACK_COVER_MODEL 常量和 AppliedPackagingClient additional model 注册；删除 me_packager_create/back_cover.json，避免再次出现紫黑 missing model 或动态遮挡面缺失。
  docs/04-asset-spec.md 同步记录：network_side_cover 属于静态 block 模型，必须内缩于原透明面之后，不作为 dynamic partial。
验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 me_packager blockstate、block 和 block_linked JSON 可解析
未运行 runClientSmoke：本轮按反馈从静态模型结构修正，不使用客户端 smoke 作为判断依据。
GameTest：已考虑。本次只调整客户端静态模型资源、移除动态 partial 注册和资产规格记录，不改变服务端 MEStorage 事务、红石触发、菜单或物品移动语义，因此不新增或运行 GameTest。
```

最新进展：

```text
调整 ME Package Assembler GUI 与槽位语义：
  新增 `textures/gui/mepackageassembler.png` 作为装配室 256x256 GUI atlas。
  Package Assembler Screen 改为贴图背景，提供与 ME Packager 同类的容量槽/自动导出按钮区域。
  下半部分改为 4 行可见、17 行总量的同步滚动输入/输出区；每个可见行左侧显示 4 个输入格，右侧显示 1 个输出格。
  GUI 输入不是 fake slot：点击与 shift-click 会真实转移玩家物品，BlockEntity 以 ItemStack identity + long amount 持久化，可累计超过普通 stack size 的数量。
  样板槽放入 package_pattern 或 packaged_processing_pattern 后，当时通过客户端过滤提示表达可输入内容，并只允许插入样板匹配材料与数量；该显示方式后续已被修正为不绘制过滤物品、改用槽位状态表达。
  方块实体保留 9 格 legacy 输入槽用于旧存档/内部兼容，同时新增 68 格 menu input buffer（17 行 x 4 列）和 17 个输出槽。
  Pattern Provider 多包裹输出优先写入 17 个输出槽；超过可用输出槽的余量才进入 pending queue。
  自动导出遍历全部输出槽，并改为只有存在输出包裹时才解析旧的背面 AE2 存储接口或 Forge item handler，避免相邻 AE2 接口未 ready 时空 capability 崩服；后续 ME_NETWORK 输出已改为本机 AE 网络存储服务。
  资产审计将 ME Package Assembler GUI atlas 纳入必需 256x256 PNG，并新增错误尺寸自测 fixture。
  docs/02、03、04、05、06 和 docs/assets/acceptance.md 已同步当前 4x4 输入、4 输出可见窗口、样板过滤和资产门禁语义。
验证 git status --short --branch 初始为 clean master
验证 .\gradlew.bat compileJava 成功，仅既有 deprecation warning
验证 .\gradlew.bat runGameTestServer 成功，133 个 required GameTest 全部通过
验证 .\gradlew.bat runClientSmoke 成功，生成 6 张菜单截图和 3 张世界截图；人工抽看 Package Assembler 截图，新背景、滚动条、样板槽、容量槽、自动导出按钮和左右输入/输出区域正常显示
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1 成功，143 个 PNG 通过必需文件、RGBA、尺寸、可见非占位像素和模型门禁
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-assets-audit.ps1 成功，包含坏装配室 GUI atlas 尺寸负例
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
GameTest：已考虑并运行。本轮改变服务端槽位、菜单真实输入、样板过滤、Pattern Provider 多包裹输出和自动导出 capability 解析，已新增/更新相关 GameTest。
```

最新进展：

```text
纠正 ME Package Assembler GUI 的 AE2 UI 接入方式：
  PackageAssemblerMenu 从普通 AbstractContainerMenu 改为 AE2 UpgradeableMenu，PackageAssemblerScreen 从 AbstractContainerScreen 改为 AE2 UpgradeableScreen。
  新增 `assets/ae2/screens/appliedpackaging/package_assembler.json`，背景、标题、玩家背包、样板槽、容量槽、4 行输入槽和输出列坐标改由 ScreenStyle 管理。
  AE2 1.20.1 style grid 没有 4 列枚举，因此 4x4 可见输入区拆成 4 个 AE2 slot semantic 行分组；业务上仍是 68 格真实 menu input buffer，不改成 fake slot。
  滚动输入/输出槽背景由客户端按 AE2 slot background 风格绘制，避免把动态滚动槽烘进背景 atlas。
验证 .\gradlew.bat compileJava 成功，仅既有 deprecation warning
验证 .\gradlew.bat runClientSmoke 成功，生成 6 张菜单截图和 3 张世界截图；人工查看 Package Assembler 截图，确认 4x4 输入格、4 输出格、滚动条、样板槽、容量槽和自动导出按钮可见
验证 rg -n -i "ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON" run\logs\latest.log 无命中
GameTest：已考虑。本轮主要纠正客户端 Screen/Menu 基类、ScreenStyle 布局和滚动槽背景绘制，不改变服务端事务、红石、过滤或物品移动语义，因此不新增或复跑 GameTest。
```

最新进展：

```text
按 AE2 源码修正装配室配置开关位置：
  查阅 AE2 15.4.10 `UpgradeableScreen`、`UpgradeableMenu`、`UpgradesPanel`、`ToolboxPanel`、`InscriberScreen` 和 `IOBusScreen` 源码，确认升级槽由 `SlotSemantics.UPGRADE` + `UpgradesPanel` 管理在右侧，配置开关通过 `addToLeftToolbar` 放在左侧悬浮 toolbar。
  删除 PackageAssemblerScreen 主面板内自绘 auto_export 按钮，不再使用自定义 GUI icon 作为普通 widget。
  PackageAssemblerScreen 新增 AE2 `IconButton` toolbar 按钮，使用 AE2 `Icon.AUTO_EXPORT_ON/OFF`；PackageAssemblerMenu 新增 AE client action `toggleAutoExport` 处理切换。
  该轮未新增升级卡行为；后续若新增升级卡，只通过真实 `IUpgradeInventory` + `SlotSemantics.UPGRADE` 进入 AE2 右侧升级面板。
验证 .\gradlew.bat compileJava 成功，仅既有 deprecation warning
验证 .\gradlew.bat runClientSmoke 成功，生成 6 张菜单截图和 3 张世界截图；人工查看 Package Assembler 截图，确认主面板内无额外奇怪按钮，auto-export 位于左侧 AE2 toolbar
```

最新进展：

```text
修正 AE2 UpgradeableMenu 槽位顺序带来的装配室菜单回归：
  首次复跑 .\gradlew.bat runGameTestServer 失败，失败用例为 packageAssemblerMenuInputUsesPatternFilterAndLargeAmount；原因是 AE2 `createPlayerInventorySlots` 先加入 hotbar 再加入主背包，旧 `HOTBAR_START` 仍按 vanilla 背包优先顺序计算，导致 shift-click 没有点到玩家热键栏铁锭。
  PackageAssemblerMenu 改为记录 AE2 实际分配给 4x4 可见输入槽和 4 个可见输出槽的 menu slot index；点击、shift-click 和客户端绘制都通过这些实际 index 访问滚动槽，而不是假设机器槽永远从 0 开始。
  玩家侧移动改为按 AE2 `SlotSemantics.PLAYER_HOTBAR` / `PLAYER_INVENTORY` 计算真实玩家槽范围，避免后续 Network Tool 工具箱或右侧真实升级槽改变 slot 顺序时误判。
  PackageAssemblerScreen 的滚动槽背景和样板 ghost 改为读取菜单提供的实际 slot index。
  装配室 GameTest 改为通过菜单查询 hotbar/input/output 实际 slot index，覆盖新的 AE2 menu 契约。
验证 .\gradlew.bat compileJava 成功，仅既有 deprecation warning
验证 .\gradlew.bat runGameTestServer 成功，133 个 required GameTest 全部通过
验证 .\gradlew.bat runClientSmoke 成功，生成 6 张菜单截图和 3 张世界截图；人工查看 Package Assembler 截图，确认主面板内无额外按钮，auto-export 位于左侧 AE2 toolbar，右侧没有自造升级控件
验证 rg -n -i "ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON" run\logs\latest.log 无命中
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1 成功，143 个 PNG 通过必需文件、RGBA、尺寸、可见非占位像素和模型门禁
GameTest：已考虑并运行。本轮改变了菜单按钮 action、slot index 判断和 shift-click 物品移动路径，属于菜单/物品移动语义变更；GameTest 首次捕获回归，修复后 133 个 required GameTest 全部通过。
```

最新进展：

```text
纠正 ME Package Assembler GUI 实现方向：
  恢复用户提供的 `mepackageassembler.png` 原始 256x256 atlas；仓库文件与源图 SHA-256 一致，不再重拼或改写 GUI 图。
  ScreenStyle 使用原图主界面 `srcRect` 176x239；名称输入框、颜色 swatch、marker 槽、右上容量元件槽、下半区样板槽、4x4 输入窗口、4 输出窗口、玩家物品栏和 hotbar 均按原图像素坐标写入 style JSON。
  上半区对齐的是 ME Packager 逻辑而不是贴图：装配室新增 `packageName`、`selectedColor`、真实 marker 槽和右上容量元件槽；默认自由封装、普通 Pattern Provider 和彩色 Pattern Provider 路径使用这些配置，已编码 package_pattern / packaged_processing_pattern 仍以样板自身颜色和 marker 为权威。
  配置按钮保持 AE2 左侧 toolbar，目前只有 auto-export；没有新增主面板奇怪按钮。
  方块实体新增 6 格真实 AE2 upgrade inventory，注册 PACKAGE_ASSEMBLER 的 redstone/capacity/speed/inverter 兼容升级，右侧由 AE2 `UpgradesPanel` 渲染和交互。
  marker 槽通过真实槽手动放入；shift-click 普通物品继续进入左侧真实大数量输入缓冲，避免 marker 槽抢走材料。
  新增 GameTest `packageAssemblerUsesConfiguredPackageIdentity` 覆盖装配室输出颜色、hover name 和 marker；更新 legacy NBT 测试以覆盖新增 marker 槽。
验证 .\gradlew.bat compileJava 成功，仅既有 deprecation warning
验证 .\gradlew.bat runGameTestServer 首次失败，失败项为 packageAssemblerMenuInputUsesPatternFilterAndLargeAmount 与 packageAssemblerLoadsLegacyElevenSlotInventory；修复 marker shift-click 路由和 legacy slot count 断言后复跑成功，134 个 required GameTest 全部通过
验证 .\gradlew.bat runClientSmoke 成功，生成 6 张菜单截图和 3 张世界截图；人工查看 Package Assembler 截图，确认使用原图布局、左侧只有 AE toolbar auto-export、右侧为 AE2 升级面板、滚动条位于输入栏左侧、玩家物品栏与 hotbar 对齐
GameTest：已考虑并运行。本轮改变装配室输出身份配置、真实 marker 槽、升级库存、NBT 保存/读取和 shift-click 路由，属于行为敏感变更；GameTest 已覆盖并通过。
```

最新进展：

```text
按 AE2 分子装配室语义修正 ME Package Assembler 逻辑：
  阅读 AE2 15.4.10 `MolecularAssemblerBlockEntity`、`MolecularAssemblerMenu`、`MolecularAssemblerPatternSlot`、`IMolecularAssemblerSupportedPattern`、`InitUpgrades` 和分子装配室模型 JSON，确认输入门禁、Pattern Provider 临时 plan、合成进度和 speed card 升级规则。
  装配室样板槽只接受已编码 package_pattern、packaged_processing_pattern 或 AE2 encoded processing pattern；无样板时本地输入槽锁定，不允许自由输入或自由封装。
  放入样板后，真实 GUI 输入槽按样板输入数量解锁，并按同位置 AEKey 与数量严格匹配；样板槽增加 AE2 encoded-pattern 背景标记。
  Pattern Provider pushPattern 改为分子装配室风格临时使用本次 pattern 规划，不写入本地样板槽；本地样板槽、输入、输出、pending queue 或合成进度非空时拒绝新 plan。
  装配室新增 0-100 合成进度和 active package queue；只允许 5 张 AE2 speed card，进度步进使用分子装配室 10/13/17/20/25/50 速度表。
  只要任意输出槽非空就不启动新合成；输出模式改为 ME_NETWORK（默认）、ADJACENT_BLOCK 和 NONE，左侧 AE toolbar 循环切换。
  自动输出按输出槽顺序一次只导出 1 个包裹；当时 ME_NETWORK 按背面 AE2 存储接口处理，ADJACENT_BLOCK 只写入背面 Forge item handler，NONE 不自动输出；后续 ME_NETWORK 已改为写入本机 AE 网络存储服务。
  外部 Forge item handler 可见机器库存，但只允许从输出槽按顺序每次抽取 1 个合法包裹，非输出槽不可抽取。
  packageName、selectedColor 和 marker 只在样板或临时 pattern plan 没有对应包裹标记时作为 fallback 生效。
  方块模型临时采用 AE2 分子装配室同款几何轮廓，换用 Applied Packaging 自有 package_assembler_side 贴图；未修改用户提供的 `mepackageassembler.png` GUI atlas。
  更新 GameTest 覆盖无样板拒绝输入、样板严格输入、输出占用阻挡、输出模式循环、Pattern Provider 进度输出、相邻方块/ME 网络导出和外部 handler 顺序抽取；旧 damaged package entity 掉落测试改为掉落点附近等待式断言，避免新增测试改变 GameTest 排布后统计范围不稳。
  docs/02、03、04、05、06 已同步当前装配室契约、模型临时策略和验证结果。
验证 .\gradlew.bat compileJava 成功，仅既有 ItemBlockRenderTypes deprecation warning
验证 .\gradlew.bat runGameTestServer 首次失败，失败项为 damagedPackageEntityUnpacksContentsToWorld；原因是旧测试同 tick/宽范围统计掉落实体，在新增测试改变 GameTest 排布后不稳定。改为掉落点附近等待式断言后复跑成功，当时 required GameTest 全部通过
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1 成功，143 个 PNG 通过必需文件、RGBA、尺寸、可见非占位像素和模型门禁
验证 .\gradlew.bat runClientSmoke 成功，生成 6 张菜单截图和 3 张世界截图；人工查看 Package Assembler 截图，确认左侧只有 AE toolbar 输出模式按钮、右侧为 5 格 speed-card 升级面板、样板槽有 encoded-pattern 背景标记、无样板时输入槽禁用；人工查看 world-all_machines 截图，确认装配室临时分子装配室轮廓模型正常渲染
验证 rg -n -i "ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON" run\logs\latest.log 无命中
GameTest：已考虑并运行。本轮改变装配室样板门禁、Pattern Provider pushPattern、合成进度、升级限制、输出模式、外部 item handler 抽取和物品移动语义，属于行为敏感变更；GameTest 已覆盖并通过。
```

最新进展：

```text
修正 ME Package Assembler 样板过滤显示与 AE 能量进度语义：
  阅读并对照 AE2 15.4.10 分子装配室代码，确认本地输入槽不应绘制过滤 ghost 物品，过滤状态应通过 slot enable/invalid state 表达。
  Package Assembler Screen 移除输入过滤 ghost 渲染；样板取走后如果输入槽仍有残留物品，槽位保持可取出并绘制红色错误状态，空输入槽重新锁定。
  菜单新增可见输入槽到真实输入 index 的映射与有效性查询，客户端按服务端样板/残留状态判断红色错误标记。
  Package Assembler 方块实体改为 AE 网络方块实体，合成进度每 tick 从本机 AE grid energy service 抽取能量；无 AE 网络或能量不足时不推进。
  加速卡沿用 AE2 分子装配室表：0/1/2/3/4/5 张 speed card 对应 10/13/17/20/25/50 进度，并按 1.0/1.3/1.7/2.0/2.5/5.0 能量倍率消耗 AE 能量。
  ME_NETWORK 输出改为写入本机接入的 AE 网络存储服务，ADJACENT_BLOCK 仍只写入背面 Forge item handler，NONE 不自动导出。
  更新 GameTest 覆盖样板移除后残留输入 invalid、无 AE 能量不推进、有 Creative Energy Cell 与 5 张 speed card 时按 50/50 两 tick 完成，并修正 CPU job 断言为默认 ME_NETWORK 输出后进入 AE storage。
验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 首次失败，失败项为 ae2CraftingCpuJobPushesIntoPackageAssembler；原因是 ME_NETWORK 默认输出已进入 AE storage，不再停留在输出槽。修正断言后复跑成功，138 个 required GameTest 全部通过
验证 .\gradlew.bat runClientSmoke 成功，生成 6 张菜单截图和 3 张世界截图；人工查看 Package Assembler 截图，确认输入槽不再绘制过滤 ghost 物品，左侧 AE toolbar 与右侧 AE2 speed-card 升级面板仍正常显示
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 rg -n "ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON" run/logs/latest.log run/logs/debug.log 仅发现 Netty/JDK Unsafe 访问探测栈，不含 missing texture、Missing model 或 Failed to read Screen JSON
GameTest：已考虑并运行。本轮改变装配室样板过滤显示、AE 网络接入、合成进度能量消耗、加速卡税率、默认 ME_NETWORK 输出目标和 Pattern Provider/CPU 产出路径，属于行为敏感变更；GameTest 已覆盖并通过。
```

最新进展：

```text
提交既有工作区基线：
  按用户要求先提交现有代码，创建提交 bbf49bb（fix: align package assembler with AE2 behavior），再开始本轮新需求。

新增 AE2 原版 Pattern Encoding Terminal 的包裹样板模式：
  阅读 AE2 15.4.10 PatternEncodingTermMenu、PatternEncodingTermScreen、PatternEncodingLogic、EncodingMode、CraftingPatternItem、EncodedPatternItem、AEPatternDecoder、AECraftingPattern、IMolecularAssemblerSupportedPattern、PatternProviderLogic、RestrictedInputSlot 和 MolecularAssemblerBlockEntity 源码，确认 1.20.1 AE2 样板模式为 enum/switch 硬编码，需要 mixin 接入。
  build.gradle 启用 Mixin 配置与 refmap，新增 appliedpackaging.mixins.json。
  在 AE2 Pattern Encoding Terminal 中增加包裹样板 tab；该模式与 crafting / stonecutting / smithing 同级，复用 AE2 crafting grid，隐藏原版 crafting-only 控件，只补包裹名称输入、颜色 swatch 和 marker 槽。
  新增 package_crafting_pattern 数据载体：输出使用 AE2 crafting_pattern 物品，并在 NBT 写入 Applied Packaging 专属包裹样板数据。
  AE2 pattern decoder、tooltip 和 encoded-pattern output hook 可识别 package_crafting_pattern NBT；解码结果是 PackageCraftingPatternDetails，不实现 IMolecularAssemblerSupportedPattern，只允许 ME Package Assembler 执行，不进入分子装配室。
  Package Assembler 样板槽、过滤、Pattern Provider pushPattern、Crafting CPU job 和本地合成路径均接入 AE2 crafting_pattern 承载的包裹样板；输出包裹的颜色、名称和 marker 以样板数据为权威，样板缺失时才回退机器配置。
  ClientSmokeRunner 新增真实 AE2 Pattern Encoding Terminal part 步骤，通过 AE2 MenuOpener 打开原版 PatternEncodingTermScreen，并在截图前切换到包裹样板模式。
  verify-release.ps1 的 -RequireClientSmokeScreenshots 必需清单扩展为 8 张，新增 appliedpackaging-client-smoke-ae2_pattern_encoding_terminal.png。
  docs/01、03、05、06、08 已同步包裹样板模式、装配室专属执行语义和 client smoke 截图审计数量。

验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，140 个 required GameTest 全部通过
验证 .\gradlew.bat runClientSmoke --stacktrace 成功，生成 3 张世界截图和 7 张真实菜单截图；人工查看 AE2 Pattern Encoding Terminal 截图，确认包裹样板 tab、名称输入、颜色 swatch、marker 槽和 AE2 crafting grid 可见
验证 rg -n "ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON|Mixin apply failed|InvalidInjectionException|IllegalClassLoadError|Timed out|timeout" run/logs/latest.log 无命中
GameTest：已考虑并运行。本轮改变 AE2 样板解码、Pattern Provider pushPattern、Crafting CPU job 和装配室样板执行语义，属于行为敏感变更；GameTest 已扩展并通过。
```

最新进展：

```text
新增 AE2 原版处理样板可选包裹列元数据和高级样板终端：
  阅读 AE2 15.4.10 与 1.21.1 PatternEncodingTerminalPart、PatternEncodingLogic、PatternEncodingTermMenu、PatternEncodingTermScreen、ProcessingEncodingPanel、processing ScreenStyle、ProcessingPatternItem 和 AEProcessingPattern 源码。
  1.20.1 没有 Data Component，因此在 AE2 原版 processing_pattern ItemStack NBT 中写入 appliedpackaging.advanced_processing_pattern；普通 AE2 终端编码路径不写该 NBT。
  metadata 使用 0..16 连续包裹列，每列映射 4 个 AE2 sparse processing input 槽，并保存颜色、可选名称与可选 marker；编码只读取启用列，忽略不可见旧 ghost 数据。
  新增 Advanced Pattern Terminal AE2 PartItem、Part、Menu、Screen 与 MenuOpener；part/model/终端网络库存复用 AE2 Pattern Encoding Terminal，菜单强制 processing mode。
  GUI 显示 4 个可见 4x1 输入列、4 行输出、列头色块、第一未启用列加号、禁用列和水平滚动条；列编辑层提供 17 色、名称与 marker fake slot，并拦截弹层输入透传。
  GUI 使用 195x260 AE2 ScreenStyle 背景，总高固定 240px；修正初版扩图不透明横条、长标题与 480px smoke 视口裁切/标题重叠，最终顶部使用短标题 Advanced/高级。
  Package Assembler 正式路由三种样板：package_crafting_pattern 精确生成单包裹；普通 processing_pattern 固定生成 Fluix/空名称/空 marker 单包裹；advanced processing pattern 按列顺序生成多个包裹，同色列不合并。
  Advanced/ordinary Pattern Provider push 均严格匹配样板输入与 KeyCounter，不足或额外输入整批拒绝；仍经过装配室 AE 能量、合成进度、speed card、输出阻挡和顺序输出逻辑。
  旧 colored_processing_pattern 与 packaged_processing_pattern 路径继续保留兼容。
  发布门禁新增高级终端 PartItem/创造栏不变量、195x260 GUI 必需资源与尺寸负例，以及第 9 张必需 client smoke 截图。

验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，145 个 required GameTest 全部通过
验证 .\gradlew.bat runClientSmoke --stacktrace 成功；人工查看 advanced_pattern_encoding_terminal 截图，确认 854x480、GUI scale 2 下界面完整、无重叠、无扩图色带
验证日志关键字 ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON|Mixin apply failed|InvalidInjectionException|IllegalClassLoadError|Timed out|timeout 无命中
验证 .\gradlew.bat build --stacktrace 成功
验证 scripts/verify-assets.ps1 成功，144 个 PNG 通过必需文件、RGBA、可见内容和尺寸门禁
验证 scripts/test-assets-audit.ps1、scripts/test-release-audit.ps1、scripts/verify-docs.ps1 成功
验证 scripts/verify-release.ps1 -RequireClientSmokeScreenshots 成功，230 个发布资源与 jar 同步，9 张必需截图有效
GameTest：已考虑并运行。本轮改变 AE2 processing pattern 元数据、Pattern Provider 输入消费、装配室多包裹顺序和样板路由，属于行为敏感变更；新增测试并通过全部 145 个 required GameTest。
```

最新进展：

```text
将高级样板从原版 AE2 processing_pattern 扩展改为独立物品：
  新增 appliedpackaging:advanced_processing_pattern，item 类继承 AE2 ProcessingPatternItem，继续复用 AE2 processing in/out、Pattern Provider、Crafting CPU、清除为空白样板和输出预览行为。
  AdvancedProcessingPatternDataStorage 只接受新物品；对 AE2 原版 processing_pattern 写入高级列 NBT 会抛出 IllegalArgumentException，默认原版终端不写该数据。
  高级终端编码路径改为输出独立高级处理样板；装配室普通 AE2 processing pattern 路由保持 Fluix/空名称/空 marker，高级路由只识别新物品。
  GameTest 增加原版样板拒绝高级元数据、新物品 AE2 解码和装配室按列顺序执行断言。

重排 Advanced Pattern Terminal GUI：
  atlas 改为本 mod 自绘 230x260 RGBA，不逐像素复制 AE2 资源；ScreenStyle 实际主体为 230x240。
  顶部 AE 网络库存增为 10 列，搜索框、终端滚动条和 crafting status 随宽度重排；9 列玩家背包与 hotbar 在主体内居中，并采用 1.21.1 bottom 基线。
  4 个 4x1 输入列之间保留 4px 间距；输入内容仍按列水平滚动，但滚动条改为位于输入区左侧的竖向外观。
  样板槽、编码按钮、清除/循环按钮和 4 行输出在加宽区域重新对齐；样板图标不再与贴图槽框分离。
  禁用列使用约 0.2 alpha 的新版效果，不绘制 ghost 物品；第一未启用列保留加号。

验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，145 个 required GameTest 全部通过
验证 .\gradlew.bat runClientSmoke --stacktrace 成功；人工查看 advanced_pattern_encoding_terminal 截图，确认 854x480、GUI scale 2 下 230px 加宽主体、左侧竖向滚动条、4px 列间距、10 列网络库存、独立样板输出预览和居中玩家栏完整显示
验证 .\gradlew.bat build --stacktrace 成功
验证 scripts/verify-assets.ps1、scripts/test-assets-audit.ps1、scripts/test-release-audit.ps1 和 scripts/verify-docs.ps1 成功；144 个 PNG 通过资源门禁
验证 scripts/verify-release.ps1 -RequireClientSmokeScreenshots 成功，231 个发布资源与 jar 同步，9 张必需截图有效，日志无发布阻断关键字
GameTest：已考虑并运行。本轮改变样板物品身份、AE2 解码、Pattern Provider 输入消费和装配室路由，属于行为敏感变更；相关 GameTest 已扩展并通过。GUI 布局与事件改动使用 client smoke 和截图验证。
```

最新进展：

```text
修正高级样板终端标题左侧重复铁锭：
  AE2 PatternEncodingTermMenu 会为 crafting、processing、smithing 和 stonecutting 模式创建共享配置库存的多组槽；原版 Screen 由模式面板控制这些槽的可见性。
  自定义 AdvancedPatternEncodingTermScreen 现在显式禁用所有非 processing 语义槽，避免共享输入索引 0 的铁锭由默认坐标 (0,0) 重复绘制。

验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 deprecation warning
验证 .\gradlew.bat runClientSmoke --stacktrace 成功；人工查看 advanced_pattern_encoding_terminal 截图，确认标题左侧重复铁锭已消失，处理中铁/铜/金输入和钻石输出仍正常显示
验证日志关键字 ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON|Mixin apply failed|InvalidInjectionException|IllegalClassLoadError|Timed out|timeout 无命中
GameTest：已考虑。本轮只修正客户端槽位可见性，不改变样板编码、物品移动、Pattern Provider 或装配室事务语义，因此不新增或复跑 GameTest。
```

最新进展：

```text
加固包裹端点与总线事务：
  ItemPackageTransactions 使用保留 slot limit/isItemValid 的累计库存快照规划完整插入，并按真实 slot 记录提交步骤；提交中途失败时回滚已插入/抽取内容。
  MEStoragePackageTransactions 与 FluidPackageTransactions 的真实抽取提交现在校验源状态，并在部分失败时恢复此前抽取；MEStorage 拆包提交可回滚此前插入。
  PackageItemStorage 的 SIMULATE 插入使用累计快照，避免多个包裹重复预占同一 slot 空余容量。
  新增 PackageBusTransactions，输出与拆包总线统一执行目标/源模拟、单包顺序提交和失败恢复；目标面从 AE grid 可连接面中排除，旋转后刷新连接面。
  packaged_processing_pattern 只有每个包裹都共享同一 marker 时才生成公共 marker 过滤条件。
  Package Bus 与 Package Pattern Terminal 的颜色/数量 DataSlot 改为服务端权威值 + 客户端菜单缓存，避免客户端 setter 修改本地 host 或忽略 amount 更新。
  ClientSmokeRunner 为终端和三种总线预填可辨识颜色、marker、item/fluid amount，截图可覆盖同步状态。

新增 GameTest 覆盖累计 slot 容量、PackageItemStorage 模拟预占、item/MEStorage 源变化回滚、MEStorage 共享容量回滚、总线纯事务、真实 AE2 Drive 端点、目标面不接 AE、目标容量不足保持原包裹和多包裹公共 marker 语义。
首次执行新增测试时，既有 damagedPackageEntityUnpacksContentsToWorld 用铁/铜统计附近掉落而被并行测试布局污染；改用该场景唯一的 NETHER_STAR/DRAGON_BREATH 后稳定。总线真实端点初版 bus -> Drive -> energy 拓扑未使总线上线，改为总线直连 Creative Energy Cell、Drive 接另一面后通过。

验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 13 个 deprecation warning
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，159 个 required GameTest 全部通过
验证 .\gradlew.bat runClientSmoke --stacktrace 成功；人工检查 Advanced Pattern Terminal、Package Pattern Terminal 和三种 Package Bus 截图，确认标题左侧无重复铁锭，颜色、marker、32 item、65 item output 和 2000 mB water 同步状态位于正确槽位
验证 rg -n -i "ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON|Mixin apply failed|InvalidInjectionException|IllegalClassLoadError|Timed out|timeout" run/logs/latest.log 无命中
验证 .\gradlew.bat build --stacktrace 成功
验证 scripts/verify-assets.ps1 成功，144 个 PNG 通过资源门禁
验证 scripts/verify-docs.ps1 成功
验证 scripts/verify-release.ps1 -RequireClientSmokeScreenshots 成功，231 个发布资源与 jar 同步，9 张必需截图有效，日志无发布阻断关键字
GameTest：已考虑并运行。本轮改变 item handler、MEStorage、流体、PackageItemStorage、总线源/目标提交、过滤和 AE grid 连接面，属于行为敏感变更；新增/扩展 GameTest 并通过全部 159 个 required GameTest。
```

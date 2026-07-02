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

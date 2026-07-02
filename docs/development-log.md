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
```

下一步：

```text
提交包裹样板终端、已编码样板支持和 AE2 参考材质重做。
继续实现过滤 UI，并补客户端 runClient 冒烟验证。
```

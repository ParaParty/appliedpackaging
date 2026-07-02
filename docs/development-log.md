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
建立 docs/assets 执行文档：
  palette.md
  acceptance.md
  asset-briefs/packages.md
  asset-briefs/machines.md
  asset-briefs/terminal-and-buses.md
  asset-briefs/ui-and-icons.md
  contracts/*.yaml
验证 5 个资产 contract 均通过 assetgen validate-contract
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

下一步：

```text
提交资产 brief 和 contract。
按 AGENTS.md 派发材质 subagent，并整合当前已注册物品的图标和 item model。
```

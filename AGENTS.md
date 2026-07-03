# AGENTS.md

本文件只放 AI/agent 工作指令。产品需求、架构设计、详细设计、资产规格、实施计划和验收标准分别维护在 `docs/` 下对应文档中。

## 1. 必读文档

开始工作前按需读取：

```text
docs/design.md
docs/00-document-index.md
docs/01-requirements.md
docs/02-system-architecture.md
docs/03-detailed-design.md
docs/04-asset-spec.md
docs/05-implementation-plan.md
docs/06-verification-release.md
docs/07-references.md
docs/08-change-intake.md
docs/development-log.md
```

`docs/chat-summary.md` 是历史讨论记录，只在需要追溯命名、美术或玩法推导时阅读；实现以分类文档为准。

## 2. 项目基线

```text
mod_id: appliedpackaging
mod_name: Applied Packaging
中文名: 应用封装
package: com.warmthdawn.appliedpackaging
target Minecraft: 1.20.1
loader: Forge
toolchain: ModDevGradle Legacy
Forge baseline: 47.4.10
AE2 baseline: 15.4.10 Forge
GuideME dev/runtime dependency: 20.1.7, range [20.1.7,20.2.0)
Java: 17
```

## 3. 文档维护规则

1. 不把 AI 操作步骤、subagent 分工、工具使用规则写进设计文档；这些内容只写在本文件。
2. 不把开发流水账写进需求/架构/详细设计；开发过程写入 `docs/development-log.md`。
3. 新需求写入 `docs/01-requirements.md`。
4. 模块、数据流、版本适配写入 `docs/02-system-architecture.md`。
5. 数据结构、事务、机器状态、总线和样板规则写入 `docs/03-detailed-design.md`。
6. 材质、模型、颜色、UI 图标、资源路径和资源验收写入 `docs/04-asset-spec.md`。
7. 阶段计划和风险写入 `docs/05-implementation-plan.md`。
8. 测试、GameTest、构建、客户端/服务端验证和发布标准写入 `docs/06-verification-release.md`。
9. 外部版本和语义来源写入 `docs/07-references.md`。
10. 发布前临时新增需求、材质替换和范围冻结状态写入 `docs/08-change-intake.md`，确认后再迁移到正式分类文档。
11. 修改文档结构时同步更新 `docs/design.md` 与 `docs/00-document-index.md`。

## 4. 开发工作流

1. 先检查 git 状态，保护用户已有改动。
2. 修改前读取相关分类文档。
3. 代码实现优先遵循现有项目结构；没有项目结构时先从 1.20.1 Forge ModDevGradle Legacy 模板初始化。
4. 不把未验证的网络/版本事实写成当前事实；版本选择改变前重新核实来源。
5. 行为敏感变更必须考虑 GameTest；未运行或未添加时在 `docs/development-log.md` 记录原因。
6. 每个阶段完成后运行最窄验证命令，并记录命令与结果。
7. 保持提交粒度清晰，优先使用：

```text
docs:
build:
feat:
test:
assets:
fix:
```

## 5. 材质与 subagent 协作

材质生成可以使用 subagent 并行执行。主 agent 只负责描述需求、拆分任务、验收和整合。

主 agent 职责：

```text
拆分资产包
为每个资产包编写 visual brief
提供颜色表、命名表、尺寸、输出路径和验收标准
审阅 subagent 交付文件和报告
把通过验收的资源纳入项目
```

subagent 职责：

```text
根据 brief 生成或绘制材质
保持输出路径与命名一致
记录来源、生成提示、修改说明和预览
不修改 Java/Gradle/设计文档
不覆盖其他 subagent 的资产
```

推荐资产分包：

```text
packages:
  17 色包裹 item 图标、包裹基础模型、颜色替换表

machines:
  ME 包裹装配室、ME 打包机、方块模型与 block/item 贴图

terminal-and-buses:
  包裹样板终端、包裹存储总线、包裹输出总线、包裹拆包总线

ui-and-icons:
  GUI 图标、按钮、状态灯、marker/过滤图标、logo
```

主 agent 派发前应准备：

```text
docs/assets/asset-briefs/*.md
docs/assets/palette.md
docs/assets/acceptance.md
build/asset-reference/ae2/*.png（如需 AE2 风格参考）
```

AE2 风格参考流程：

```text
如材质目标是 AE2 风格，先使用目标 AE2 版本源码生成参考 sheet。
当前项目参考源为 AppliedEnergistics/Applied-Energistics-2 forge/v15.4.10。
源码 clone 与参考 sheet 只放在 build/reference 或 build/asset-reference，不纳入发布资源。
调用 ImageGen 时使用无文字 reference sheet；带标签 sheet 只给人和 agent 核对来源。
参考图只用于材质语言、比例、调色和构图，不得逐像素复制 AE2 贴图。
```

subagent 交付应包含：

```text
src/main/resources/assets/appliedpackaging/...
docs/assets/reports/<package-name>.md
preview image 或 renderer/screenshot 记录
```

## 6. 验证要求

常规顺序：

```powershell
.\gradlew.bat build
.\gradlew.bat runData
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
.\gradlew.bat runServer
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-server-smoke.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireCleanGit
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\write-release-manifest.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-manifest.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\write-release-bundle.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-bundle.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-docs-audit.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-assets-audit.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-audit.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-check-plan.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-manifest.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-bundle.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireAssetContracts
```

如果项目阶段还没有对应任务，记录原因，不要把 build-only 当成行为验证。
`scripts/run-release-checks.ps1` 编排 `build`、`runData`、`runGameTestServer`、可选 `runClientSmoke`、可选 `run-server-smoke.ps1`、机械发布审计、资产资源审计、文档审计、可选 tag 就绪审计、可选发布清单生成/审计和可选发布附件包生成/审计。使用 `-ReleaseCandidate` 时会禁止 `-AuditOnly` 和 skip flags，并自动启用 `-RunClientSmoke`、`-RunServerSmoke`、发布清单生成/审计和发布附件包生成/审计；最终发布 tag 前推荐在所有变更提交后执行 `-ReleaseCandidate -RequireCleanGit -RequireReadyForTag`。修改 release runner 顺序、候选发布预设、skip flag 或 server world-load 参数保护时同步运行 `scripts/test-release-check-plan.ps1`。使用 `-RequireReadyForTag` 时会执行 `scripts/verify-release-readiness.ps1 -RequireReadyForTag`，确认变更接收表没有待输入/待判定项，验证文档不再标记发布 tag 未完成，并且在无负面 blocker 时要求 `docs/08-change-intake.md` 和 `docs/06-verification-release.md` 明确记录范围已冻结、最终服务端 world-load 已完成、发布 tag 可创建、目标可以标记完成和 tag 就绪门禁已通过；修改 readiness 规则时同步运行 `scripts/test-release-readiness.ps1`。使用 `-RunClientSmoke` 时会自动要求 6 张 client smoke 截图存在且为有效 PNG。使用 `-RunServerSmoke` 时会在其他 Gradle run 后刷新 `run/logs/latest.log`，并自动要求 dedicated server world-load 证据。使用 `-WriteReleaseManifest` 时会在 `build/release/` 写入包含 jar SHA-256、版本范围和 git commit 的发布 JSON 清单。使用 `-RequireReleaseManifest` 时会执行 `scripts/verify-release-manifest.ps1`，确认清单仍匹配当前 jar、`gradle.properties` 和 git HEAD；修改 manifest writer 或 manifest audit 时同步运行 `scripts/test-release-manifest.ps1`。使用 `-WriteReleaseBundle` / `-RequireReleaseBundle` 时会生成并复验包含 jar、manifest、README、CHANGELOG、LICENSE 和 SHA256SUMS 的发布 zip；bundle 审计会核对 bundle 内 manifest 的 mod id/version、jar SHA-256，并在 `-RequireCleanGit` 下核对 manifest 的 git commit/branch/clean/statusPorcelain；修改 bundle writer 或 bundle audit 时同步运行 `scripts/test-release-bundle.ps1`。修改机械发布审计规则、jar 源文件同步、语言 key/占位符、玩家入口不变量、本地样板可获得性或终端 PartItem 注册规则时同步运行 `scripts/test-release-audit.ps1`。修改发布 PNG 资源、资产尺寸规则或必需资源清单时同步运行 `scripts/verify-assets.ps1` 和 `scripts/test-assets-audit.ps1`。`scripts/test-release-self-tests.ps1` 会聚合 docs audit、asset audit、release audit、readiness、release plan、manifest 和 bundle 自测，适合作为修改发布脚本后的快速非 Minecraft 自测入口。`scripts/verify-docs.ps1` 会检查必需文档、关键发布脚本、文档入口和本地 Markdown 链接；修改 docs audit 规则时同步运行 `scripts/test-docs-audit.ps1`。`-RequireServerWorldLoad` 只能与 `-AuditOnly` 组合使用，或与 `-RunServerSmoke` 同时使用。`-RequireCleanGit` 只用于最终冻结后强制检查 git 工作树干净。
`scripts/verify-release.ps1` 只做机械发布审计，可用 `-RootPath` 指向临时 fixture；会检查 jar 内 README/CHANGELOG/LICENSE 与仓库源文件一致，检查 jar 内英文/简体中文语言文件与源码一致，检查语言 key 和占位符一致，检查本地 package_pattern / packaged_processing_pattern 不作为 recipe/creative-tab 玩家入口，并检查 package_pattern_terminal 仍注册为 AE2 PartItem；不替代 `build`、`runData`、`runGameTestServer`、`runClientSmoke` 或 `runServer`；`-RequireCleanGit` 只用于最终冻结后的发布门禁。

## 7. 禁止事项

```text
不要把包裹内容伪装成 AE2 散装库存。
不要让 ME 打包机读取 Pattern Provider 或执行样板。
不要让 ME 包裹装配室扫描相邻存储或拆包。
不要创建玩家可获得的空包裹。
不要实现真实包裹嵌套。
不要把 agent 指令写入设计正文。
不要让材质 subagent 修改代码、Gradle 或核心设计文档。
```

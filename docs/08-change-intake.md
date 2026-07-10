# 变更接收与范围冻结

本文件用于接收发布前新增需求、材质替换和验收范围变化。它不是最终规格源；确认后的内容必须迁移到对应分类文档。

当前接收窗口：

```text
2026-07-05 用户已补充包裹材质、包裹实体/模型渲染、ME-only 可切换连接面打包机需求。
2026-07-06 左右用户将补充正式打包机模型/动画资产；当前打包机模型临时使用 Create 同款风格资源。
0.1.0-dev 最终发布 tag 暂停创建。
Dedicated server full world-load 当前基线已通过，但等待新增范围冻结后再作为最终验收重新执行。
```

## 1. 输入格式

新增需求请按以下信息记录：

```text
需求名称：
玩家场景：
必须行为：
不允许行为：
影响对象：包裹 / 装配室 / 打包机 / 终端 / 总线 / UI / 资源 / 发布
是否行为敏感：是 / 否
需要 GameTest：是 / 否 / 待判定
需要客户端截图 smoke：是 / 否 / 待判定
验收标准：
```

材质补充请按以下信息记录：

```text
资产包：packages / machines / terminal-and-buses / ui-and-icons / logo / other
目标资源路径：
参考图或描述：
必须保留的视觉元素：
必须避免的视觉元素：
尺寸与格式：
替换还是新增：
需要更新的 contract：
需要截图或 renderer preview：
验收标准：
```

## 2. 迁移规则

需求类变更：

```text
玩家目标、范围、验收项 -> docs/01-requirements.md
模块边界、数据流、版本适配 -> docs/02-system-architecture.md
数据结构、事务、机器状态、菜单、过滤 -> docs/03-detailed-design.md
实施顺序、风险、阶段计划 -> docs/05-implementation-plan.md
验证命令、GameTest、客户端 smoke、服务端验收 -> docs/06-verification-release.md
```

材质类变更：

```text
视觉风格、资源路径、验收规则 -> docs/04-asset-spec.md
调色板与资源验收说明 -> docs/assets/palette.md / docs/assets/acceptance.md
资产 brief -> docs/assets/asset-briefs/*.md
资产 contract -> docs/assets/contracts/*.yaml
资源预览 -> docs/assets/previews/*
交付报告 -> docs/assets/reports/*.md
实际资源 -> src/main/resources/assets/appliedpackaging/...
参考素材 -> build/reference 或 build/asset-reference，不纳入发布 jar
```

讨论记录：

```text
只把推导过程或历史选择写入 docs/chat-summary.md。
不要把未确认的新需求当成最终规格写入 design.md。
```

AI/agent 指令：

```text
新增协作规则、subagent 分工、工具约束只写入 AGENTS.md。
不要写入 01-07 设计和验收文档。
```

## 3. 影响判定

每个新增项都必须做影响判定：

```text
只改文档：运行 markdown/diff 检查即可。
改资源：运行 asset contract/resource audit，必要时 runData 和 runClientSmoke。
改注册、配方、loot、模型路径：运行 runData、build，必要时 runClientSmoke。
改机器、菜单、事务、过滤、网络、能力、红石、样板、总线：必须考虑 GameTest。
改客户端类或资源加载：运行 runClientSmoke，并检查 latest.log 与 10 张必需 client smoke 截图。
改服务端公共加载、注册、网络、能力、数据包：运行 runGameTestServer 和 runServer。
改发布包内容：运行 build，并执行 scripts/verify-release.ps1 审计 jar 条目、jar 内文档/语言/Applied Packaging 发布资源源文件同步、文本资源路径、语言占位符和玩家入口产品不变量。
改 asset contract：执行 scripts/verify-release.ps1 -RequireAssetContracts 或直接执行 assetgen validate-contract。
改发布 PNG 资源：执行 scripts/verify-assets.ps1，确认必需 PNG、路径归类、RGBA、可见非占位像素内容和尺寸规则；修改资产审计规则时同步执行 scripts/test-assets-audit.ps1。
最终范围冻结后：执行 scripts/run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag；该预设会执行 build、runData、runGameTestServer、runClientSmoke、run-server-smoke、机械发布审计、资产资源审计、文档审计、tag 就绪审计、发布 JSON 清单生成/审计和发布 zip 生成/审计。默认会执行 verify-assets.ps1 资产资源审计和 verify-docs.ps1 文档完整性审计。使用 -RequireReadyForTag 时会执行 verify-release-readiness.ps1 -RequireReadyForTag，确保本表没有状态、迁移目标或验证要求仍为待输入、待判定、阻塞或失败的项，确保已填写的迁移目标是仓库内已存在文件的规范相对路径且不包含父级遍历，并确保需求类目标落在需求/架构/详细设计/实施/验证/参考文档，材质类目标落在资产规格、docs/assets 或 src/main/resources/assets/appliedpackaging 资源路径；验证文档不再标记发布 tag 未完成，并在没有负面 blocker 后要求本表明确写出“已冻结。”、“最终服务端 world-load：已完成。”、“发布 tag：可创建。”，同时要求 docs/06-verification-release.md 明确写出“可以标记完成。”和“发布 tag 就绪门禁已通过。”。使用 -RunClientSmoke 时会自动审计 10 张必需 client smoke 截图。使用 -RunServerSmoke 时会在其他 run 后刷新 latest.log 并自动执行 -RequireServerWorldLoad 审计。修改机械发布审计规则、jar 源文件同步、语言 key/占位符、玩家入口产品不变量、本地样板可获得性或终端 PartItem 注册规则时运行 scripts/test-release-audit.ps1 覆盖有效 release audit fixture、jar 必需条目缺失、jar 内 README/lang 过期、jar 内发布资源缺失或过期、mods.toml 元数据篡改、本机路径泄漏、语言占位符不一致、本地样板 recipe 输出、创造栏本地样板和终端 BlockItem 回退。使用 -WriteReleaseManifest 时会在 build/release/ 写入发布 JSON 清单；使用 -RequireReleaseManifest 时会复验清单匹配当前 jar、gradle.properties 和 git HEAD；修改发布清单生成或审计时运行 scripts/test-release-manifest.ps1 覆盖有效清单、篡改 mod id、篡改 artifact hash 和 clean-git 路径；使用 -WriteReleaseBundle / -RequireReleaseBundle 时会生成并复验包含 jar、manifest、README、CHANGELOG、LICENSE 和 SHA256SUMS 的发布 zip，并在 -RequireCleanGit 下确认 bundle 内 manifest 的 git 元数据指向当前干净提交。也可以单独运行 scripts/run-server-smoke.ps1 刷新服务端 world-load 证据后，再执行 scripts/run-release-checks.ps1 -AuditOnly -RequireServerWorldLoad 审计。
```

GameTest 输出契约：

```text
说明是否考虑 GameTest。
说明是否发现已有 GameTest run/source。
说明是否新增、扩展、运行或跳过 GameTest。
说明使用的 Gradle 命令，或跳过原因。
```

## 4. 当前冻结状态

截至 2026-07-04：

```text
当前功能基线：docs/06-verification-release.md R1-R13 均记录为已满足。
当前验证基线：build、runData、runGameTestServer、runClientSmoke 均已有通过记录；2026-07-04 当前基线 runServer 已进入 world 并出现 Done (2.400s)，自动 runServerSmoke 已进入 world 并出现 Done (2.413s)；完整 run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit 已通过，包含 112 个 GameTest、6 张 client smoke 截图和 dedicated server Done (2.471s)。
当前发布 jar：build/libs/appliedpackaging-0.1.0-dev.jar 已存在并通过 dev/test 条目审计。
当前机械审计：scripts/verify-release.ps1 已加入；普通模式、-RequireAssetContracts、-RequireServerWorldLoad 和两者组合模式均已通过；最强模式已确认 jar 文件名、mods.toml、manifest 与 gradle.properties 版本元数据一致，并会把 Mojang/Yggdrasil 外部公钥获取失败作为 WARN 忽略；当前已新增并验证 GuideME dependency range 审计；当前已新增 jar 内文档/语言/Applied Packaging 发布资源源文件同步审计；当前已新增语言占位符审计；当前已新增玩家入口产品不变量审计，确认本地样板不是 recipe/creative-tab 输出且终端仍为 AE2 PartItem；-RequireCleanGit 已作为最终发布 tag 前的可选门禁，当前提交基线已通过。
当前发布检查编排：scripts/run-release-checks.ps1 已加入；-ReleaseCandidate 会禁止 -AuditOnly 和 skip flags，并自动启用 -RunClientSmoke、-RunServerSmoke、-WriteReleaseManifest、-RequireReleaseManifest、-WriteReleaseBundle 和 -RequireReleaseBundle；-RequireReadyForTag 会执行 verify-release-readiness.ps1 -RequireReadyForTag，并在本表存在状态、迁移目标或验证要求仍为待输入、待判定、阻塞或失败的项、迁移目标不是规范仓库内既有文件路径、迁移目标使用父级遍历、需求/材质迁移目标与类型目标族不匹配、验证文档仍标记发布未完成，或负面 blocker 清除后缺少正向冻结/完成/tag 可创建信号时失败；-RunClientSmoke 会自动要求 10 张必需 client smoke 截图存在且为有效 PNG；-RunServerSmoke 会刷新 dedicated server world-load 日志并自动要求 -RequireServerWorldLoad；-RequireCleanGit 会传递到机械审计、发布清单/附件包生成和发布清单/附件包审计；verify-release.ps1 支持 -RootPath 用于临时 fixture 审计；test-release-audit.ps1 会自测有效 release audit fixture、jar 必需 README 条目缺失、jar 内 README/lang 过期、jar 内发布资源缺失或过期、mods.toml 元数据篡改、本机路径泄漏、语言占位符不一致、本地样板 recipe 输出、创造栏本地样板和终端 BlockItem 回退；verify-assets.ps1 会审计必需 PNG、路径归类、RGBA PNG header、可见非占位像素内容和 item/block/gui/part/logo 尺寸；test-assets-audit.ps1 会自测有效资源、错尺寸、坏 PNG header、全透明 PNG、单色占位 PNG 和缺必需 PNG；-WriteReleaseManifest 会生成 build/release 发布清单；-RequireReleaseManifest 会执行 verify-release-manifest.ps1；test-release-manifest.ps1 会自测有效 manifest、mod id 篡改、artifact hash 篡改和 clean-git manifest 路径；-WriteReleaseBundle 会生成 build/release 发布 zip；-RequireReleaseBundle 会执行 verify-release-bundle.ps1，并核对 bundle 内 manifest 的 mod/version、jar SHA-256 与 clean-git 元数据；test-release-bundle.ps1 会自测有效 bundle、manifest 篡改和 bundled README 篡改路径；test-docs-audit.ps1 会自测文档审计的有效 fixture、缺必需文件、正式文档未清理占位和断链路径；test-release-self-tests.ps1 会聚合 docs audit、asset audit、release audit、readiness、release plan、manifest 和 bundle 自测；默认执行 verify-assets.ps1 和 verify-docs.ps1；2026-07-04 执行 -RunClientSmoke 完整编排通过，执行 -AuditOnly -RequireAssetContracts -RequireServerWorldLoad 通过，提交 10b59b2 后执行 -AuditOnly -RequireCleanGit 通过，执行 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke 通过，write-release-manifest.ps1 已生成并核对发布清单，verify-docs.ps1 已通过，执行 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest 已通过；GuideME metadata 显式化后，build、-AuditOnly -WriteReleaseManifest -RequireReleaseManifest 和 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke 均已通过；执行 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 已通过；-ReleaseCandidate -RequireCleanGit 的 plan 已验证为最终完整门禁顺序；完整 -ReleaseCandidate -RequireCleanGit 已通过，release manifest 记录 clean=true；当前 -RequireReadyForTag 会按预期阻止发布 tag。
EULA 状态：run/eula.txt 已为 eula=true。
最终服务端 world-load：当前基线已通过；尚未在新增需求/材质冻结后重新执行。
发布 tag：等待新增范围实现、验证和最终服务端 world-load 后创建。
```

## 5. 新增项暂存表

| ID | 类型 | 标题 | 状态 | 迁移目标 | 验证要求 |
| --- | --- | --- | --- | --- | --- |
| IN-001 | 需求 | 包裹物品/实体同模型渲染、ME Packager 只连接 AE2 MEStorage、network_side 可切换、1k/16 基础容量和完整打包流程 | 已迁移并通过当前验证 | docs/01-requirements.md; docs/02-system-architecture.md; docs/03-detailed-design.md; docs/05-implementation-plan.md; docs/06-verification-release.md | 已运行 compileJava、runData、runGameTestServer、runClientSmoke、verify-assets；当前 GameTest 122 个必需测试通过 |
| IN-002 | 材质 | package_box_pixel_v7 17 色包裹材质替换；ME Packager 临时 Create 同款模型/贴图 | 已迁移并通过当前验证 | docs/04-asset-spec.md; docs/assets/acceptance.md; docs/assets/asset-briefs/packages.md; docs/assets/contracts/package_items.yaml; docs/assets/reports/packages.md; docs/assets/reports/machines.md; src/main/resources/assets/appliedpackaging/ | 已运行 asset JSON parse、verify-assets、test-assets-audit、runData、runClientSmoke；package_box 模型当前要求 full-face uv [0,0,16,16] 与 marker custom-render override；后续正式打包机模型到位后需重新跑资源审计和客户端 smoke |
| IN-003 | 材质 | 正式 ME Packager 模型/动画替换 | 待输入 | 待判定 | 待判定 |
| IN-004 | 需求/材质 | 独立 advanced_processing_pattern、高级样板终端和装配室三种样板执行模式 | 已迁移并通过当前验证 | docs/01-requirements.md; docs/02-system-architecture.md; docs/03-detailed-design.md; docs/04-asset-spec.md; docs/05-implementation-plan.md; docs/06-verification-release.md; src/main/resources/assets/appliedpackaging/ | compileJava、build、164 个必需 GameTest、230px 加宽版 runClientSmoke、资产/文档自测和发布审计均已通过；真实编码测试覆盖 marker 数量归一与未启用列残留，已人工检查左侧竖向滚动条、列间距、独立样板输出和玩家栏对齐 |

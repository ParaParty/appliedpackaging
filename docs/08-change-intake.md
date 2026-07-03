# 变更接收与范围冻结

本文件用于接收发布前新增需求、材质替换和验收范围变化。它不是最终规格源；确认后的内容必须迁移到对应分类文档。

当前接收窗口：

```text
2026-07-05 用户将补充需求和材质。
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
资产 brief -> docs/assets/asset-briefs/*.md
资产 contract -> docs/assets/contracts/*.yaml
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
改客户端类或资源加载：运行 runClientSmoke，并检查 latest.log 与 6 张 client smoke 截图。
改服务端公共加载、注册、网络、能力、数据包：运行 runGameTestServer 和 runServer。
改发布包内容：运行 build，并执行 scripts/verify-release.ps1 审计 jar 条目和文本资源路径。
改 asset contract：执行 scripts/verify-release.ps1 -RequireAssetContracts 或直接执行 assetgen validate-contract。
最终范围冻结后：执行 scripts/run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag；该预设会执行 build、runData、runGameTestServer、runClientSmoke、run-server-smoke、机械发布审计、文档审计、tag 就绪审计、发布 JSON 清单生成/审计和发布 zip 生成/审计。默认会执行 verify-docs.ps1 文档完整性审计。使用 -RequireReadyForTag 时会执行 verify-release-readiness.ps1 -RequireReadyForTag，确保本表没有待输入/待判定项，验证文档不再标记发布 tag 未完成，并在没有负面 blocker 后要求本表明确写出“已冻结。”、“最终服务端 world-load：已完成。”、“发布 tag：可创建。”，同时要求 docs/06-verification-release.md 明确写出“可以标记完成。”和“发布 tag 就绪门禁已通过。”。使用 -RunClientSmoke 时会自动审计 6 张 client smoke 截图。使用 -RunServerSmoke 时会在其他 run 后刷新 latest.log 并自动执行 -RequireServerWorldLoad 审计。使用 -WriteReleaseManifest 时会在 build/release/ 写入发布 JSON 清单；使用 -RequireReleaseManifest 时会复验清单匹配当前 jar、gradle.properties 和 git HEAD；使用 -WriteReleaseBundle / -RequireReleaseBundle 时会生成并复验包含 jar、manifest、README、CHANGELOG、LICENSE 和 SHA256SUMS 的发布 zip，并在 -RequireCleanGit 下确认 bundle 内 manifest 的 git 元数据指向当前干净提交。也可以单独运行 scripts/run-server-smoke.ps1 刷新服务端 world-load 证据后，再执行 scripts/run-release-checks.ps1 -AuditOnly -RequireServerWorldLoad 审计。
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
当前机械审计：scripts/verify-release.ps1 已加入；普通模式、-RequireAssetContracts、-RequireServerWorldLoad 和两者组合模式均已通过；最强模式已确认 jar 文件名、mods.toml、manifest 与 gradle.properties 版本元数据一致，并会把 Mojang/Yggdrasil 外部公钥获取失败作为 WARN 忽略；当前已新增并验证 GuideME dependency range 审计；-RequireCleanGit 已作为最终发布 tag 前的可选门禁，当前提交基线已通过。
当前发布检查编排：scripts/run-release-checks.ps1 已加入；-ReleaseCandidate 会禁止 -AuditOnly 和 skip flags，并自动启用 -RunClientSmoke、-RunServerSmoke、-WriteReleaseManifest、-RequireReleaseManifest、-WriteReleaseBundle 和 -RequireReleaseBundle；-RequireReadyForTag 会执行 verify-release-readiness.ps1 -RequireReadyForTag，并在本表存在待输入/待判定项、验证文档仍标记发布未完成，或负面 blocker 清除后缺少正向冻结/完成/tag 可创建信号时失败；-RunClientSmoke 会自动要求 6 张 client smoke 截图存在且为有效 PNG；-RunServerSmoke 会刷新 dedicated server world-load 日志并自动要求 -RequireServerWorldLoad；-RequireCleanGit 会传递到机械审计、发布清单/附件包生成和发布清单/附件包审计；-WriteReleaseManifest 会生成 build/release 发布清单；-RequireReleaseManifest 会执行 verify-release-manifest.ps1；-WriteReleaseBundle 会生成 build/release 发布 zip；-RequireReleaseBundle 会执行 verify-release-bundle.ps1，并核对 bundle 内 manifest 的 mod/version、jar SHA-256 与 clean-git 元数据；test-release-bundle.ps1 会自测有效 bundle、manifest 篡改和 bundled README 篡改路径；默认执行 verify-docs.ps1 文档完整性审计；2026-07-04 执行 -RunClientSmoke 完整编排通过，执行 -AuditOnly -RequireAssetContracts -RequireServerWorldLoad 通过，提交 10b59b2 后执行 -AuditOnly -RequireCleanGit 通过，执行 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke 通过，write-release-manifest.ps1 已生成并核对发布清单，verify-docs.ps1 已通过，执行 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest 已通过；GuideME metadata 显式化后，build、-AuditOnly -WriteReleaseManifest -RequireReleaseManifest 和 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke 均已通过；执行 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle 已通过；-ReleaseCandidate -RequireCleanGit 的 plan 已验证为最终完整门禁顺序；完整 -ReleaseCandidate -RequireCleanGit 已通过，release manifest 记录 clean=true；当前 -RequireReadyForTag 会按预期阻止发布 tag。
EULA 状态：run/eula.txt 已为 eula=true。
最终服务端 world-load：当前基线已通过；尚未在新增需求/材质冻结后重新执行。
发布 tag：等待新增范围实现、验证和最终服务端 world-load 后创建。
```

## 5. 新增项暂存表

| ID | 类型 | 标题 | 状态 | 迁移目标 | 验证要求 |
| --- | --- | --- | --- | --- | --- |
| IN-001 | 需求 | 待用户 2026-07-05 补充 | 待输入 | 待判定 | 待判定 |
| IN-002 | 材质 | 待用户 2026-07-05 补充 | 待输入 | 待判定 | 待判定 |

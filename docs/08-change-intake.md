# 变更接收与范围冻结

本文件用于接收发布前新增需求、材质替换和验收范围变化。它不是最终规格源；确认后的内容必须迁移到对应分类文档。

当前接收窗口：

```text
2026-07-15 ME Packager 方向与交互最终修正已迁移到正式文档：删除可独立切换的接线状态，仅保留水平 `facing`；固定底部与模型背面接入 ME 网络；AE2 扳手只旋转模型方向；只有传送带上表面执行手动包裹存取，其它模型位置打开 GUI。
2026-07-13 总线最终 GUI 范围修正：移除 Package Export Bus 与独立 Package Pattern Terminal；Package Storage Bus/Package Unpacking Bus 改为 AE2 part，并采用基础 2 行加 5 张容量卡、最多 7 行的过滤界面。
2026-07-15 总线运行逻辑修正已迁移到正式文档：Package Unpacking Bus 改为默认优先级 0 的 Formation Plane 式只写入端点并采用 Pattern Provider 阻挡按钮/语义；两个总线默认值都为 0，数值完全由右上 Priority 设置控制，同值时 Package Unpacking Bus 通过 AE2 preferred-storage 语义优先，拆包拒绝后才回落到 Package Storage Bus；两种总线增加颜色空模式，Package Storage Bus 的 Partition Storage 改为读取相邻容器包裹生成过滤。
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
验证命令、GameTest、客户端人工验收、服务端验收 -> docs/06-verification-release.md
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
改资源：运行 asset contract/resource audit，必要时 runData，并按视觉风险人工运行 runClient。
改注册、配方、loot、模型路径：运行 runData、build，并按视觉风险人工运行 runClient。
改机器、菜单、事务、过滤、网络、能力、红石、样板、总线：必须考虑 GameTest。
改客户端类或资源加载：按风险运行 runClient，人工检查目标界面、动画与 latest.log；不再维护自动客户端 smoke 和固定截图门禁。
改服务端公共加载、注册、网络、能力、数据包：运行 runGameTestServer 和 runServer。
改发布包内容：运行 build，并执行 scripts/verify-release.ps1 审计 jar 条目、jar 内文档/语言/Applied Packaging 发布资源源文件同步、文本资源路径、语言占位符和玩家入口产品不变量。
改 asset contract：执行 scripts/verify-release.ps1 -RequireAssetContracts 或直接执行 assetgen validate-contract。
改发布 PNG 资源：执行 scripts/verify-assets.ps1，确认必需 PNG、路径归类、RGBA、可见非占位像素内容和尺寸规则；修改资产审计规则时同步执行 scripts/test-assets-audit.ps1。
最终范围冻结后：执行 scripts/run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag；该预设会执行 build、runData、runGameTestServer、run-server-smoke、机械发布审计、资产资源审计、文档审计、tag 就绪审计、发布 JSON 清单生成/审计和发布 zip 生成/审计。默认会执行 verify-assets.ps1 资产资源审计和 verify-docs.ps1 文档完整性审计。使用 -RequireReadyForTag 时会执行 verify-release-readiness.ps1 -RequireReadyForTag，确保本表没有状态、迁移目标或验证要求仍为待输入、待判定、阻塞或失败的项，确保已填写的迁移目标是仓库内已存在文件的规范相对路径且不包含父级遍历，并确保需求类目标落在需求/架构/详细设计/实施/验证/参考文档，材质类目标落在资产规格、docs/assets 或 src/main/resources/assets/appliedpackaging 资源路径；验证文档不再标记发布 tag 未完成，并在没有负面 blocker 后要求本表明确写出“已冻结。”、“最终服务端 world-load：已完成。”、“发布 tag：可创建。”，同时要求 docs/06-verification-release.md 明确写出“可以标记完成。”和“发布 tag 就绪门禁已通过。”。使用 -RunServerSmoke 时会在其他 run 后刷新 latest.log 并自动执行 -RequireServerWorldLoad 审计。修改机械发布审计规则、jar 源文件同步、语言 key/占位符、玩家入口产品不变量、本地样板可获得性或终端 PartItem 注册规则时运行 scripts/test-release-audit.ps1。使用 -WriteReleaseManifest / -RequireReleaseManifest 时会生成并复验发布 JSON 清单；使用 -WriteReleaseBundle / -RequireReleaseBundle 时会生成并复验发布 zip。也可以单独运行 scripts/run-server-smoke.ps1 刷新服务端 world-load 证据后，再执行 scripts/run-release-checks.ps1 -AuditOnly -RequireServerWorldLoad 审计。
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
当前验证基线：build、runData、runGameTestServer 均已有通过记录；自动客户端 smoke 已于 2026-07-15 删除，既往截图只作为历史证据，后续客户端视觉改动按风险人工运行 runClient 验收；2026-07-04 候选发布基线完成 dedicated server Done (2.471s) 和当时的完整 ReleaseCandidate 门禁；2026-07-15 当前行为基线通过 106 个必需 GameTest。
当前发布 jar：build/libs/appliedpackaging-0.1.0-dev.jar 已存在并通过 dev/test 条目审计。
当前机械审计：scripts/verify-release.ps1 已加入；普通模式、-RequireAssetContracts、-RequireServerWorldLoad 和两者组合模式均已通过；最强模式已确认 jar 文件名、mods.toml、manifest 与 gradle.properties 版本元数据一致，并会把 Mojang/Yggdrasil 外部公钥获取失败作为 WARN 忽略；当前已新增并验证 GuideME dependency range 审计；当前已新增 jar 内文档/语言/Applied Packaging 发布资源源文件同步审计；当前已新增语言占位符审计；当前已新增玩家入口产品不变量审计，确认本地样板不是 recipe/creative-tab 输出且终端仍为 AE2 PartItem；-RequireCleanGit 已作为最终发布 tag 前的可选门禁，当前提交基线已通过。
当前发布检查编排：scripts/run-release-checks.ps1 已加入；-ReleaseCandidate 会禁止 -AuditOnly 和 skip flags，并自动启用 -RunServerSmoke、-WriteReleaseManifest、-RequireReleaseManifest、-WriteReleaseBundle 和 -RequireReleaseBundle；自动客户端 smoke、固定截图门禁及对应命令参数已删除。-RequireReadyForTag 会执行 verify-release-readiness.ps1 -RequireReadyForTag；-RunServerSmoke 会刷新 dedicated server world-load 日志并自动要求 -RequireServerWorldLoad；-RequireCleanGit 会传递到机械审计、发布清单/附件包生成和发布清单/附件包审计。test-release-self-tests.ps1 聚合 docs audit、asset audit、release audit、readiness、release plan、manifest 和 bundle 自测；默认执行 verify-assets.ps1 和 verify-docs.ps1。既往 runClientSmoke 记录只保留为历史证据，不再表示当前可用任务。
EULA 状态：run/eula.txt 已为 eula=true。
最终服务端 world-load：当前基线已通过；尚未在新增需求/材质冻结后重新执行。
发布 tag：等待新增范围实现、验证和最终服务端 world-load 后创建。
```

## 5. 新增项暂存表

| ID | 类型 | 标题 | 状态 | 迁移目标 | 验证要求 |
| --- | --- | --- | --- | --- | --- |
| IN-001 | 需求 | 包裹物品/实体同模型渲染、ME Packager 仅从固定底部与模型背面连接 AE2 MEStorage、空容量槽 9/9 和完整打包流程 | 已迁移并通过当前验证 | docs/01-requirements.md; docs/02-system-architecture.md; docs/03-detailed-design.md; docs/05-implementation-plan.md; docs/06-verification-release.md | 2026-07-16 已运行 compileJava 与 runGameTestServer；当前 109 个必需 GameTest 通过，覆盖底部/背面接网、扳手旋转、四朝向传送带点击和容量档边界 |
| IN-002 | 材质 | package_box_pixel_v7 17 色包裹材质替换；ME Packager 临时 Create 同款模型/贴图 | 已迁移并通过当前验证 | docs/04-asset-spec.md; docs/assets/acceptance.md; docs/assets/asset-briefs/packages.md; docs/assets/contracts/package_items.yaml; docs/assets/reports/packages.md; docs/assets/reports/machines.md; src/main/resources/assets/appliedpackaging/ | 已运行 asset JSON parse、verify-assets、test-assets-audit 与 runData；package_box 模型当前要求 full-face uv [0,0,16,16] 与 marker custom-render override；正式打包机模型已由 IN-006 完成，后续视觉改动按风险人工运行 runClient |
| IN-003 | 需求/材质 | 正式 UI/模型/动画调整 | 合并高级/包裹样板终端及全项目统一拾色弹窗已迁移并通过当前行为验证；其余范围待输入 | docs/01-requirements.md; docs/03-detailed-design.md; docs/04-asset-spec.md; docs/05-implementation-plan.md; docs/06-verification-release.md; docs/assets/reports/ui-and-icons.md; src/main/resources/assets/appliedpackaging/ | 合并终端只保留 Advanced Pattern Terminal 一个 part/menu/screen；高级/包裹两页槽位隔离、载体自动切页、页面持久化、普通终端拒绝两种专用载体及无 VIEW_CELL 槽由 GameTest 覆盖。两行网络库存时两页共享 195x245 外框和 192px bottom，base 编辑区只保留灰底，Screen 后绘制当前页 132x78 完整面板且切页不 resize/init。右侧模式标签使用 Pattern Encoding Terminal 同款 22x22 horizontal normal/selected/focus 背景、21px 步进和直接 sprite 图标。统一弹窗继续要求无标题、Fluix 左置、16 色右侧两行、底层 slot/item 正常渲染且不透传输入。2026-07-18 追加用户工具栏图标块、装配室输入/color/marker 坐标和两张更新底图，最终客户端像素仍以开发客户端重启后的人工复核为准。 |
| IN-004 | 需求/材质 | 独立 advanced_processing_pattern、高级样板终端和装配室三种样板执行模式 | 已迁移并通过当前自动验证 | docs/01-requirements.md; docs/02-system-architecture.md; docs/03-detailed-design.md; docs/04-asset-spec.md; docs/05-implementation-plan.md; docs/06-verification-release.md; src/main/resources/assets/appliedpackaging/ | 每列保留 81 个输入位置，列上限提高到 81；v2 逐列 sparse NBT 兼容读取 v1，菜单只同步 4 列窗口；新增转置、长按移动/交换、末尾新增自动滚动和默认色/循环颜色。上游 GTCEu 与 StarT Fork 两套运行时均通过 147/147 required GameTest；独立客户端、构建、资源、文档和发布审计通过。长按拾起手感、按钮像素和横向滚动仍需世界内人工验收。 |
| IN-005 | 需求/材质 | 删除 Package Export Bus/独立 Package Pattern Terminal；Storage/Unpacking Bus 七行过滤 GUI 与 AE2 part 迁移 | 行为与 GUI 底图已迁移并通过验证；正式 part 模型/新版卡图待后续输入 | docs/01-requirements.md; docs/02-system-architecture.md; docs/03-detailed-design.md; docs/04-asset-spec.md; docs/05-implementation-plan.md; docs/06-verification-release.md; src/main/resources/assets/appliedpackaging/ | compileJava、runData、资源审计、发布审计自测与历史客户端截图人工检查通过；两个总线均允许 5 格容量卡并扩展到 7 行过滤；卸货总线已接入真实 held 包裹、20 tick 进度、最终提交阻塞保留/重试、NBT 持久化与拆除返还。2026-07-18 增量要求 held 包裹在工作/阻塞期间以数量 1 报告给 ME 并允许网络或 GUI 取回，取回原子取消拆包且不产生部分内容，使装配室阻挡能看到正在拆的匹配包裹；最终验证结果记录于开发日志，server smoke 仍随发布候选门禁执行。 |
| IN-006 | 材质/客户端 | ME Packager 正式空心框架、32x32 双周期传送带与四条帘子动画 | 已迁移并通过本轮验证 | docs/04-asset-spec.md; docs/assets/contracts/me_packager.yaml; docs/assets/reports/machines.md; src/main/resources/assets/appliedpackaging/ | asset contract、verify-assets、test-assets-audit、runData、历史客户端截图人工检查、build 和 release asset-contract audit 已通过；`facing` 独立控制水平模型与全部动态件，方块状态只有四个水平朝向；15px 上表面+1px 正面连续 UV、四条帘子顶部转轴、包裹体积裁切和无 missing model 均通过，完整物品模型使用标准方块 display；三张用户 PNG 原字节 SHA-256 一致 |
| IN-007 | 需求/材质 | 序列缓存器方块、三轴直线多方块、一次输入锁存缓存、顺序分配、合并抽取、同步/样板/阻挡/输入延迟模式、五类模型状态及 ME Chest 双界面 GUI | 已迁移、实现并通过当前自动验证 | docs/01-requirements.md; docs/02-system-architecture.md; docs/03-detailed-design.md; docs/04-asset-spec.md; docs/05-implementation-plan.md; docs/06-verification-release.md | 端点无存储且不计入序列第 1 格；同时支持 IItemHandler、IFluidHandler 与 MEStorage；普通样板保留 sparse 空位，高级样板直接 push 仍按实际输入稠密顺序，但装配室生成的每个高级样板列包裹保留 sparse 行布局；Package Unpacking Bus 仅在序列缓存器开启样板模式时按布局原子跳过空位，关闭时连续输入。2026-07-18 增量加入端点 3x9/整行滚动主界面、成员中央单槽侧面界面和右侧一格红石卡升级；过滤与模式设置继续只保留逻辑，不在第一版 GUI 显示。禁用滚动条保留 disabled handle，红石卡以端点信号门禁整组自动输出。自动验证结果见本轮开发日志；Screen JSON/资源初始化继续由真实客户端启动检查，最终世界内像素与交互按人工验收。 |
| IN-008 | 需求/兼容 | 可选 JEI、EMI、Create、GTCEu/StarT Fork 与高级/包裹配方导入，包括序列组装和动力合成 | 已迁移并通过当前自动验证 | docs/01-requirements.md; docs/02-system-architecture.md; docs/03-detailed-design.md; docs/05-implementation-plan.md; docs/06-verification-release.md; docs/07-references.md | 只维护一套 JEI 插件与 handler；JEI 环境直接加载，EMI 环境由 EMI+TMRV 在没有完整 JEI 时映射同一实现，三者都不是发布硬依赖。通用、Create ProcessingRecipe 与 GTCEu item/fluid/tick input 默认每种材料一个包裹；Create 序列组装和动力合成保留专用分组，动力合成同时保留选中行/列的 sparse 前导与中间空位。上游 GTCEu 7.5.3 与 StarT Fork 1.7.0b 当前均通过 148/148 required GameTest；JEI-only 和 EMI+TMRV 客户端分别确认同一 Applied Packaging transfer handler 直接注册或经 TMRV 映射。Star Technology 星门装配仍按 IN-009 只评审、不实现。 |
| IN-009 | 需求/兼容 | Star Technology 星门装配 layered recipe 分组 | 待评审 | docs/01-requirements.md; docs/03-detailed-design.md; docs/07-references.md | 需确认每个 layer 一个包裹，还是 layer 内逐材料分包并额外保存层边界；确认后再决定 GTCEu Fork 专用适配及是否需要泛 KubeJS 声明接口。 |

IN-002 中“后续正式打包机模型到位后需复验”的历史条件已由 IN-006 完成，不再是当前待办。

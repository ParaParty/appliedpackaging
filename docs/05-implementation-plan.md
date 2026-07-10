# 实施计划

## 阶段 0：仓库与设计

交付：

```text
git init
保留原始 docs baseline
按文档类型拆分设计文档
建立 AGENTS.md
记录外部版本来源
```

验收：

```text
docs/design.md 只作为入口
需求、架构、详细设计、资产、实施、验证、参考来源各自独立
AGENTS.md 存在并作为仓库级 agent 操作入口
git 有清晰提交
```

## 阶段 1：项目骨架

交付：

```text
从 NeoForgeMDKs/MDK-Forge-1.20.1-ModDevGradle 初始化
切换 mod_id/package/metadata
配置 Forge 47.4.10、AE2 15.4.10
建立注册、数据生成、GameTest run
确认 gradlew build 可跑
```

验收：

```text
./gradlew.bat build 成功
mods.toml 元数据正确
包名与 mod_id 一致
runData/gameTestServer 任务存在或有明确替代任务
```

## 阶段 2：包裹核心

交付：

```text
17 色包裹物品
PackageDataStorage NBT adapter
canonical hash
capacity profile
tooltip
package filter
unit tests/GameTest
```

当前状态：

```text
已实现：
  17 色包裹物品
  PackageDataStorage NBT adapter
  canonical hash
  capacity calculator/profile
  tooltip
  package filter
  package plan builder
  marker retain/override/clear plan logic
  package flattening
  item handler / Forge fluid handler / AE2 MEStorage endpoint 事务接入
  同内容不同顺序 canonical hash 稳定，并写入可堆叠的规范化 NBT
  颜色、marker、内容差异会产生不同 canonical hash
  PackageData GameTest

待实现：
  无
```

验收：

```text
无 PackageData 的包裹被判无效
同内容不同顺序 canonical hash 稳定
不同颜色/marker/content 不能误堆叠
tooltip 显示每包/总计
```

## 阶段 3：资源与基础玩法

交付：

```text
基础材质/模型/语言/创造标签
包裹样板与封装处理样板数据
基础 recipe/loot/datagen
```

当前状态：

```text
已交付当前注册 item 图标和 item model。
已按 AE2 forge/v15.4.10 reference sheet 交付机器、终端、总线、UI 图标和 logo 二轮生产质量资源。
已交付资产 reports。
已交付 me_packager、package_assembler、package_pattern_terminal 和总线基础配方。
样板相关玩家配方已收敛到 AE2 原版 blank_pattern；本地 package_pattern / packaged_processing_pattern 不再作为普通合成输出。
已交付 me_packager/package_assembler loot table。
已交付 appliedpackaging:packages item tag。
终端、总线资源已接入 Java 注册与基础玩法。
```

验收：

```text
runData 成功
17 色包裹图标存在
英文和简体中文语言 key 完整
没有 missing texture
```

## 阶段 4：ME 打包机

交付：

```text
方块/方块实体/菜单
相邻 item handler endpoint
打包事务
拆包事务
容量元件
红石触发
GameTest
```

当前状态：

```text
已实现：
  me_packager 方块/方块物品/方块实体注册
  水平朝向 blockstate 与可切换 network_side blockstate
  方块掉落表
  内部输入/输出 item handler；非 network_side 面暴露包裹输入/输出 capability
  GUI/Menu 改为 AE2 UpgradeableScreen + UpgradeableMenu，主入口为右键打开 GUI
  GUI 包含 AE2 左工具栏、5 行过滤区、包裹名称、左侧颜色选择小按钮、右侧 marker 槽、包裹输入/输出口、容量元件过滤器槽、右侧 6 格升级面板和玩家背包
  非潜行右键保留快速放入包裹与取出输出；无快速动作时打开 GUI
  潜行右键切换 network_side 到被点击面
  未安装红石卡时默认有红石信号打包；安装红石卡后可切换高信号、低信号、总是、脉冲和关闭，红石只控制打包
  拆包在输入槽存在合法包裹时自动工作，仍受过滤、阻挡和目标容量约束
  持续打包与自动拆包基础每 20 tick 重试一次；加速卡降低间隔
  容量元件槽读取 AE2 16k/64k/256k storage component；无元件时使用基础 1k 容量与 16 类型上限
  容量卡最多 3 张，每张解锁 1 行过滤槽，默认启用 2 行，最多 5 行
  selectedColor 控制无过滤模板时的输出包裹颜色
  contentFilter 使用 AE2 GenericStack fake slots；旧过滤槽保留为存档兼容
  过滤应用模式可在打包拆包都启用、仅打包、仅拆包之间切换
  marker 槽物品优先作为输出 marker；旧 marker retain/override/clear 状态保留为 NBT 兼容
  阻挡模式可在忽略网络内容与网络内已有物品时禁止拆包之间切换
  所选 network_side 只识别 AE2 MEStorage capability，可接入相邻 ME Interface 暴露的子网存储
  AE2 MEStorage 打包/拆包事务，支持 GenericStack/AEKey 和源包裹展开
  item-only GameTest，覆盖显式 marker retain/override/clear
  MEStorage endpoint GameTest
  底层 fluid handler transaction GameTest 保留；ME Packager 不以 Forge fluid handler 作为目标
  ME Packager 红石模式菜单、红石卡门槛、红石脉冲、高信号持续打包、红石关闭仍允许拆包和容量卡过滤行 GameTest
  ME Packager AE2 GUI client smoke 截图
  真实 AE2 Creative Energy Cell + Drive + Interface + ME Packager 世界内打包/拆包 GameTest smoke
  真实 AE2 顶面 network_side 世界内打包 GameTest smoke
  真实世界相邻 Forge item/fluid handler + ME Packager 反例 GameTest smoke，确认无 MEStorage 时不回落、不消耗

待实现：
  无
```

验收：

```text
输出槽满时打包不改变源库存
目标容量不足时拆包不消耗包裹
堆叠包裹按完整包裹数量拆入
marker 冲突按策略拒绝或覆盖
```

## 阶段 5：装配室与 AE2 样板

交付：

```text
方块/方块实体/菜单
Pattern Provider 输入适配
普通处理样板 -> 包裹
彩色处理样板 -> 多包裹
封装处理样板 -> package plan
输出阻挡与输出模式
GameTest/客户端验证
```

当前状态：

```text
已实现：
  package_assembler 方块/方块物品/方块实体注册
  水平朝向 blockstate
  方块掉落表
  Package Assembler GUI/Menu
  9 格 legacy 输入缓冲 + 68 格 GUI 真实输入缓冲（17 行 x 4 列）+ 1 格样板槽 + 17 格输出槽 + 1 格容量槽 + 1 格 marker 槽 + 5 格 AE2 加速卡升级槽
  shift-click 已编码样板进样板槽，AE2 容量元件进容量槽，其它物品只有在样板过滤允许时进入 GUI 真实输入缓冲
  样板槽为空时拒绝本地输入和本地合成，不再自由封装
  输入合法包裹展开后再封装
  容量槽识别 AE2 16k/64k/256k storage component、item/fluid storage cell 与 portable cell
  任意输出槽非空时不启动新合成或新 Pattern Provider plan，不消耗输入
  已编码 package_pattern 精确匹配输入计划后生成对应颜色包裹
  已编码 package_pattern 走 exact package plan，可重封装大于默认容量的源包裹
  已编码 package_pattern 不消耗，可重复作为本地装配计划
  已编码 packaged_processing_pattern 保存有序多包裹计划
  package_assembler 可按 packaged_processing_pattern 逐包生成匹配包裹
  package_assembler 暴露 AE2 ICraftingMachine capability
  Pattern Provider pushPattern 可按分子装配室语义临时使用本次 pattern 规划配方，把 KeyCounter 中的物品/流体 GenericStack 输入装配为包裹
  空本地样板槽的普通 Pattern Provider pushPattern 直接从 KeyCounter 规划包裹，避免 9 格临时输入缓存限制
  本地样板与 Pattern Provider pushPattern 均使用容量槽档位
  pushPattern 在输出阻挡、输入缓冲非空或规划失败时整批拒绝且不消耗输入；本地样板槽兼容路径遇到无法转成 ItemStack 的 AEKey 时同样拒绝
  ColoredProcessingPatternDataStorage 可在 AE2 encoded processing pattern 上保存输入槽颜色元数据
  彩色 Pattern Provider pushPattern 读取 AE2 sparse input 槽位，按输入槽颜色拆成多个包裹
  彩色 Pattern Provider pushPattern 支持流体 AEKey 输入
  同 AEKey 位于不同颜色槽时按 sparse 槽位拆分，不被 AE2 condensed input 提前合并
  彩色 pushPattern 产生多个包裹时按顺序写入空输出槽，超过可用输出槽的余量通过 pending queue 顺序输出并持久化保存
  装配室存在 0-100 合成进度，只允许 5 张 AE2 speed card；0/1/2/3/4/5 张按分子装配室速度表每 tick 尝试推进 10/13/17/20/25/50，并按 1.0/1.3/1.7/2.0/2.5/5.0 能量倍率从本机 AE 网络抽取能量
  装配室输出模式默认 ME_NETWORK，可通过 GUI 左侧 AE2 toolbar 图标循环切换 ME_NETWORK、ADJACENT_BLOCK 和 NONE 并持久化保存
  装配室 server tick 会按输出槽顺序一次导出 1 个包裹；ME_NETWORK 只导出到本机接入的 AE 网络存储服务，ADJACENT_BLOCK 只导出到背面 Forge item handler，NONE 不自动导出
  自动导出失败时保留输出槽包裹，不丢弃、不继续消耗新输入
  外部 Forge item handler 可见完整机器库存，但只允许按输出槽顺序每次抽取 1 个合法包裹，非输出槽不可抽取
  包裹名称、颜色和 marker 只在样板或临时 pattern plan 没有对应包裹标记时作为 fallback 生效
  真实 AE2 Creative Energy Cell + Pattern Provider + Package Assembler GameTest smoke
  真实 AE2 Creative Energy Cell + Pattern Provider + Package Assembler 彩色处理样板 GameTest smoke
  真实 AE2 Drive + 64k item cell + Crafting CPU + Pattern Provider + Package Assembler 自动合成 job smoke
  真实 AE2 Creative Energy Cell + Drive + Interface + Package Assembler ME_NETWORK 输出 GameTest smoke
  装配室基础 GameTest

客户端验证：
  runClientSmoke 已覆盖 Package Assembler GUI 打开与截图；人工检查包括无过滤 ghost 物品、样板移除后残留输入红色错误状态、左侧 toolbar 与右侧 AE2 升级面板
```

验收：

```text
普通处理样板生成默认色包裹
彩色输入格生成对应颜色包裹
同 AEKey 位于不同颜色格时不会提前合并
输出阻挡只检查本机输出口，输出模式不扫描目标网络内容
```

## 阶段 6：终端与总线

交付：

```text
包裹样板终端
包裹存储总线
包裹输出总线
包裹拆包总线
过滤 UI
AE2 网络集成
GameTest/服务器 smoke test
```

当前状态：

```text
已实现：
  package_storage_bus/package_export_bus/package_unpacking_bus 方块、方块物品、方块实体注册
  三种总线 blockstate、item model、loot table、recipe、语言文件
  AE2 可连接方块端点：AENetworkBlockEntity + IManagedGridNode
  package_storage_bus 通过 IStorageProvider 挂载 PackageItemStorage
  PackageItemStorage 只暴露、插入、抽取合法包裹
  PackageItemStorage 支持 PackageFilter 限制可见、可插入、可抽取包裹
  PackageItemStorage 模拟插入使用累计库存快照，避免多个包裹重复预占同一 slot 容量
  package_export_bus 只从 AE 网络输出已有合法包裹
  package_unpacking_bus 整包事务性拆入目标面库存
  总线目标面只暴露相邻 Forge item handler，并从 AE grid 连接面与线缆渲染中排除；其它面连接 AE 网络
  输出/拆包事务统一由 PackageBusTransactions 执行源/目标双向模拟、顺序提交与失败恢复
  总线支持手持已编码样板/合法包裹设置 ghost 过滤模板，潜行空手清除
  总线提供共享 Package Bus 配置 UI，可显示 ghost filter、从光标复制模板、清除模板、shift-click 背包模板设置 ghost filter
  Package Bus 配置 UI 不消耗玩家光标或背包中的模板物品
  Package Bus 配置 UI 支持手工编辑颜色、marker ghost 和 3 个 required content ghost slots
  Package Bus required content ghost slots 可从 Forge 流体容器编码 AEFluidKey 过滤条件
  手工 Package Bus 过滤器以 PackageFilter NBT 保存，并兼容旧 filter_template 读取
  Package Bus 与 Package Pattern Terminal 的颜色/数量 DataSlot 使用服务端权威值和客户端菜单缓存同步
  runClientSmoke 可 quick-play 单人世界、摆放关键方块与 AE2 terminal parts、打开真实菜单、截图 Package Assembler/ME Packager/原版 Pattern Encoding Terminal/Advanced Pattern Terminal/Package Pattern Terminal/Package Storage Bus/Package Export Bus/Package Unpacking Bus 后退出
  PackageItemStorage/总线过滤 GameTest
  PackageItemStorage 累计容量、总线真实 AE 网络导出/拆包、目标不足保持原包裹、源变化与目标部分提交回滚 GameTest
  package_pattern_terminal AE2 cable part item、part host、兼容方块、方块实体、菜单、客户端 screen
  package_pattern_terminal 物品 id 改为 AE2 part item，不新增重复终端物品；既有方块路径保留给兼容/测试
  Package Pattern Terminal 菜单通过 PackagePatternTerminalHost 同时支持方块 host 与 AE2 part host
  Package Pattern Terminal AE2 part 可通过 PartHelper 放置到 cable bus 侧面，并保存/读取终端库存、颜色和处理输出 ghost
  package_pattern_terminal 可从 9 格预览输入编码 package_pattern
  package_pattern_terminal 支持 17 色 swatch 选择，编码样板颜色跟随 selectedColor
  package_pattern_terminal 支持 9 个输入槽颜色色标，并保存/同步槽位颜色
  package_pattern_terminal 支持 marker 槽与容量槽编码 package_pattern
  package_pattern_terminal 可把 AE2 encoded processing pattern 克隆为带 colored_processing_pattern 元数据的彩色处理样板
  package_pattern_terminal 在未逐槽设色时可把 selectedColor 应用到 AE2 processing pattern 全部非空输入槽
  package_pattern_terminal 可把 AE2 原版 blank_pattern 编码为带 package_pattern NBT 的封装样板载体，并保留 AE2 物品类型
  package_pattern_terminal 在 AE2 blank_pattern 存在多包裹计划且无处理输出 ghost 时写入 packaged_processing_pattern NBT，并保留 AE2 物品类型
  package_pattern_terminal 在 AE2 blank_pattern 存在处理输出 ghost 时编码 AE2 原版 processing pattern，并附带 packaged_processing_pattern NBT
  package_pattern_terminal 可把空白 packaged_processing_pattern 编码为有序多包裹样板
  advanced_pattern_encoding_terminal 作为 AE2 cable part item 注册，复用原版 Pattern Encoding Terminal part/model/terminal body，只保留 processing mode
  advanced_pattern_encoding_terminal 中间区显示 4 个可见的 4x1 输入列、4 个垂直输出槽、列头颜色按钮、第一未启用列加号和左侧竖向列滚动条，最多 17 列；主体加宽至 230px，输入列间距为 4px
  advanced_pattern_encoding_terminal 为每列保存颜色、名称和 marker，并编码独立 advanced_processing_pattern 物品；AE2 原版 processing_pattern 不写也不接受该高级列元数据
  Package Assembler 按包裹样板、普通处理样板和高级处理样板三路执行；高级样板按列顺序生成多个包裹，普通处理样板固定 Fluix/空名称/空 marker
  packaged_processing_pattern NBT 支持可选 outputs[]，终端提供 3 个处理输出 ghost slots
  处理输出 ghost slots 可从光标复制物品/流体容器，右键复制 1 个物品或 1 个容器量，空光标清除，且不消耗玩家物品
  处理输出 ghost slots 可把 Forge 流体容器编码为 AEFluidKey 输出，例如水桶编码为 1000 mB water
  packaged_processing_pattern tooltip 显示已编码处理输出
  已编码 AE2 blank_pattern 通过客户端 tooltip hook 显示 package_pattern 或 packaged_processing_pattern 内容，未编码 AE2 blank_pattern 保持原版 tooltip
  package_pattern_terminal 已调整为 AE2 风格薄面板 block model，并提供按朝向旋转的薄面板 VoxelShape
  package_pattern_terminal AE2 part 已使用 Applied Packaging 自有 body/front/back/sides/overlay mask 材质和 base part model，不再依赖 AE2 pattern terminal 纹理层
  Package Pattern Terminal 处理输出 ghost 槽支持滚轮调整已设置 key 的数量，流体每步 1000 mB
  Package Bus required content ghost 槽支持滚轮调整已设置 key 的数量，流体每步 1000 mB
  package_pattern_terminal 可通过 Split 按钮把已编码 packaged_processing_pattern 拆回 AE2 blank_pattern 承载的 package_pattern 数据
  package_pattern_terminal Split pending queue 会保存/读取，输出槽清空后可继续吐出后续 AE2 blank_pattern carrier
  package_pattern_terminal 输入槽颜色支持右键清除
  package_pattern / packaged_processing_pattern tooltip 显示空白或已编码包裹内容
  装配室可读取 package_pattern_terminal 产出的已编码 package_pattern
  装配室可读取 AE2 blank_pattern 承载的 package_pattern NBT，样板槽和 shift-click 验证共用统一载体判断
  装配室可读取 AE2 blank_pattern 承载的 packaged_processing_pattern NBT，并逐包输出
  装配室可接受 AE2 encoded processing pattern 承载的 packaged_processing_pattern Pattern Provider push，并逐包输出
  装配室可接受带流体内容的 packaged_processing_pattern Pattern Provider push，并逐包输出
  真实 AE2 Pattern Provider 可解码并推送带 packaged_processing_pattern NBT 的 AE2 encoded processing pattern
  装配室可读取 packaged_processing_pattern 并逐包输出
  已编码 packaged_processing_pattern 不会被终端当空白样板覆盖

发布后增强，不阻塞 0.1.0-dev 发布：
  彩色处理样板更完整的处理输出 UI
  封装处理样板任意 AEKey 处理输出 ghost editor
  批量 required content / 任意 AEKey 高级过滤器编辑器
```

验收：

```text
总线只允许包裹通过
存储总线不暴露包裹内部内容
输出总线不把散装库存自动打成包裹
拆包总线只做整包事务
```

## 阶段 7：发布

交付：

```text
runData
build
runGameTestServer
runClient smoke
runClientSmoke GUI screenshot smoke
检查 jar、mods.toml、license、changelog
生成发布清单
范围冻结后按当前版本生成发布 tag
```

当前状态：

```text
已完成：
  CHANGELOG.md、README.md、LICENSE.md 与 Forge/AE2/GuideME 版本声明已补齐
  mods.toml 已声明 Minecraft、Forge、AE2 与 GuideME 发布依赖范围
  build/libs/appliedpackaging-0.1.0-dev.jar 已生成
  scripts/verify-release.ps1 已覆盖 jar 元数据、必需条目、jar 内文档/语言/Applied Packaging 发布资源源文件同步、dev/test/reference 条目排除、本机路径泄漏、资源 JSON/PNG、玩家入口产品不变量、asset contract、语言 key/占位符、模型贴图引用、client smoke 截图、dedicated server latest.log 证据和可选 git clean 证据
  scripts/run-release-checks.ps1 已编排 build、runData、runGameTestServer、可选 runClientSmoke、可选 run-server-smoke、机械发布审计、文档审计、发布清单和发布附件包
  scripts/run-release-checks.ps1 -ReleaseCandidate 已作为最终候选发布预设，自动启用 client smoke、server smoke、manifest 和 bundle 审计
  scripts/verify-release-readiness.ps1 已作为 tag 就绪审计，-RequireReadyForTag 会阻止状态、迁移目标或验证要求仍为待输入、待判定、阻塞或失败的 intake 项创建发布 tag，并要求已填写的迁移目标是仓库内已存在文件的规范相对路径、不包含父级遍历，且与类型目标族匹配：需求类落在 docs/01、02、03、05、06 或 07，材质类落在 docs/04、docs/assets 或 src/main/resources/assets/appliedpackaging；负面 blocker 清除后还要求文档明确记录范围已冻结、最终服务端 world-load 已完成、发布 tag 可创建、目标可以标记完成和 tag 就绪门禁已通过
  使用 -RunClientSmoke 时会自动审计 9 张必需 client smoke 截图存在、非空且为有效 PNG
  dedicated server world-load 已在当前基线通过；服务端 latest.log 审计可由 run-server-smoke.ps1 或 run-release-checks.ps1 -RunServerSmoke 刷新后执行
  当前验证基线已通过 run-release-checks.ps1 -RunClientSmoke
  当前服务端证据已通过 run-release-checks.ps1 -AuditOnly -RequireAssetContracts -RequireClientSmokeScreenshots -RequireServerWorldLoad
  当前自动服务端 smoke 已通过 run-release-checks.ps1 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke
  当前提交基线已通过 run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit，包含 build、runData、112 个 GameTest、6 张 client smoke 截图、dedicated server Done (2.471s)、release audit、asset audit、docs audit、manifest audit 和 bundle audit
  GuideME 发布依赖范围已进入 mods.toml，并通过 build、release audit、manifest audit 和 server smoke 验证
  当前提交基线已通过 run-release-checks.ps1 -AuditOnly -RequireCleanGit
  资产资源审计可由 verify-assets.ps1 覆盖必需 PNG、路径归类、RGBA PNG header、可见非占位像素内容和 item/block/gui/part/logo 尺寸
  资产资源审计自测可由 test-assets-audit.ps1 覆盖有效资产 fixture、错尺寸、坏 PNG header、全透明 PNG、单色占位 PNG 和缺必需 PNG
  发布清单已由 write-release-manifest.ps1 生成到 build/release/ 并核对 jar SHA-256 与 git commit
  发布清单可由 verify-release-manifest.ps1 复验，确认当前 jar、gradle.properties 和 git HEAD 与清单一致
  机械发布审计自测可由 test-release-audit.ps1 覆盖有效 release audit fixture、jar 必需条目缺失、jar 内 README/lang 过期、jar 内发布资源缺失或过期、mods.toml 元数据篡改、本机路径泄漏、语言占位符不一致、本地样板 recipe 输出、创造栏暴露本地样板和终端退回 BlockItem
  发布清单自测可由 test-release-manifest.ps1 覆盖有效 manifest、mod id 篡改、artifact hash 篡改和 clean-git manifest 路径
  发布附件包可由 write-release-bundle.ps1 生成到 build/release/ 并由 verify-release-bundle.ps1 复验 jar、manifest、README、CHANGELOG、LICENSE、SHA256SUMS、bundle manifest mod/version、jar SHA-256 和 clean-git 元数据
  发布附件包自测可由 test-release-bundle.ps1 覆盖有效 bundle、manifest 篡改和 bundled README 篡改路径
  文档审计自测可由 test-docs-audit.ps1 覆盖有效 fixture、缺必需文件、正式文档未清理占位和本地 Markdown 断链路径
  发布脚本自测套件可由 test-release-self-tests.ps1 聚合运行 docs audit、asset audit、release audit、readiness、release plan、manifest 和 bundle 自测
  文档完整性已由 verify-docs.ps1 覆盖必需文档、文档入口、正式设计文档未清理占位和本地 Markdown 链接
  最终发布 tag 前可在全部变更提交后执行 run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag

暂缓：
  0.1.0-dev 发布 tag 暂缓到用户补充需求和材质、范围冻结、实现并重新验证之后
```

验收：

```text
git 工作树干净，且最终冻结后可由 run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag 机械验证
build/libs/appliedpackaging-<version>.jar 存在，且 META-INF/mods.toml 声明 Minecraft、Forge、AE2 与 GuideME 发布依赖范围
build/release/appliedpackaging-<version>-release-manifest.json 可生成并复验，且记录 jar SHA-256 与 git commit
build/release/appliedpackaging-<version>-release-bundle.zip 可生成并复验，且包含 jar、manifest、README、CHANGELOG、LICENSE 和 SHA256SUMS，并在 clean-git 发布门禁中确认 bundle 内 manifest 仍指向当前提交
run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag 成功
jar 可在 Minecraft 1.20.1 Forge + AE2 15.4.10 客户端进入游戏
docs 与实现一致
verify-docs.ps1 成功
发布 tag 可追溯且只在最终范围冻结后创建
```

## 风险

AE2 样板集成风险：

```text
AE2 1.20.1 API 对自定义 Pattern Provider/Molecular Assembler 风格集成的公开入口可能不足。
先实现可由 Pattern Provider 推入材料的普通 Forge inventory 机器，再逐步接入深层样板语义。
```

AE2 总线 Part API 风险：

```text
Package Pattern Terminal 已实现为 AE2 cable part item，并保留兼容方块路径。
Package Storage/Export/Unpacking Bus 当前交付为 AE2 可连接方块端点，不作为 cable part 发布。
后续如果要把总线也迁移为 AE2 cable part，需要重新设计放置、持久化、菜单定位和掉落迁移，不阻塞 0.1.0-dev。
```

GenericStack 范围风险：

```text
1.0 垂直切片优先验证物品。
数据模型完整支持 AEKey；对未知 key 保守拒绝拆包，避免吞资源。
流体 adapter 保留在底层 transaction 与装配室/样板路径中；ME Packager 当前只接入 AE2 MEStorage，不再把 Forge fluid handler 作为世界端点。
```

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
  AE2 MEStorage 打包/拆包适配与 Forge item handler 整包插入
  有序 contents 不合并同类条目；同内容不同顺序产生不同 canonical hash 与 NBT
  颜色、marker、内容差异会产生不同 canonical hash
  PackageData GameTest

待实现：
  无
```

验收：

```text
无 PackageData 的包裹被判无效
contents 顺序与重复条目经 NBT/hash 往返后保持不变
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
已交付 me_packager、package_assembler 和两个 AE2 cable part 总线的基础配方。
package_pattern 与 advanced_processing_pattern 均只由对应编码终端产出，不作为普通合成输出。
已交付 me_packager/package_assembler loot table。
已交付 appliedpackaging:packages item tag。
合并高级/包裹两页的 Advanced Pattern Encoding Terminal 和两个 cable part 总线资源已接入 Java 注册与基础玩法；普通 AE2 样板终端不增加包裹 UI。
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
相邻 AE2 MEStorage endpoint
打包/拆包模拟与提交
16k/64k/256k 容量元件
红石触发
GameTest
```

当前状态：

```text
已实现：
  me_packager 方块/方块物品/方块实体注册
  只包含四个水平 facing 的 blockstate；固定底部与模型背面接入 AE 网络
  方块掉落表
  单一 heldBox item handler；底部与模型背面之外的四面暴露同一包裹 capability，并由状态区分待拆输入与待取输出
  GUI/Menu 改为 AE2 UpgradeableScreen + UpgradeableMenu，主入口为右键打开 GUI
  GUI 包含 AE2 左工具栏、5 行过滤区、颜色选择小按钮、marker fake/config slot、共享 heldBox、容量元件过滤器槽、右侧 6 格升级面板和玩家背包
  只有右键传送带上表面才快速放入包裹或取出输出；右键其它模型位置打开 GUI
  AE2 扳手按普通水平可定向机器规则旋转 facing，背面接线与模型同步旋转
  未安装红石卡时默认有红石信号打包；安装红石卡后可切换高信号、低信号、总是、脉冲和关闭，红石只控制打包
  拆包在输入槽存在合法包裹时自动工作，仍受过滤、阻挡和目标容量约束
  持续打包与自动拆包基础每 20 tick 重试一次；加速卡降低间隔
  容量元件槽只读取 AE2 16k/64k/256k storage component；无元件时使用 9 单位与 9 类型上限
  容量卡最多 3 张，每张解锁 1 行过滤槽，默认启用 2 行，最多 5 行
  selectedColor 控制无过滤模板时的输出包裹颜色
  contentFilter 使用 AE2 GenericStack fake slots，不读取隐藏旧过滤槽
  过滤应用模式可在打包拆包都启用、仅打包、仅拆包之间切换
  marker fake/config slot 在 override 模式下优先作为输出 marker；retain/override/clear 是当前正式配置
  阻挡模式可在忽略网络内容与网络内已有物品时禁止拆包之间切换
  固定底部与模型背面只接入 AE2 主节点，可连接线缆或相邻 ME Interface 网络
  AE2 MEStorage 打包/拆包操作，支持 GenericStack/AEKey 和源包裹展开
  MEStorage endpoint GameTest
  已删除无运行时调用的 Forge item handler 打包规划与 Forge fluid handler 适配；ME Packager 只以 AE2 MEStorage 作为目标
  ME Packager 红石模式菜单、红石卡门槛、红石脉冲、高信号持续打包、红石关闭仍允许拆包和容量卡过滤行 GameTest
  ME Packager AE2 GUI 人工截图
  真实 AE2 Creative Energy Cell + Drive + Interface + ME Packager 世界内打包/拆包 GameTest smoke
  真实 AE2 底面 Interface 与模型背面线缆世界内打包 GameTest smoke
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
普通 AE2 合成/处理等已编码样板 -> Fluix 包裹 + 主输出 marker
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
  按样板实际非空输入数动态分配的逻辑输入缓冲（高级样板编码上限 17×81）+ 4×4 稠密可见窗口 + 1 格样板槽 + 17 格输出槽 + 1 格容量槽 + 5 格 AE2 加速卡升级槽
  shift-click 已编码样板进样板槽，AE2 容量元件进容量槽，其它物品只有在样板过滤允许时进入 GUI 真实输入缓冲
  样板槽为空时拒绝本地输入和本地合成，不再自由封装
  输入合法包裹展开后再封装
  容量槽只识别 AE2 16k/64k/256k storage component；拒绝 1k component、完整 storage cell 与 portable cell
  任意输出槽非空时不启动新合成或新 Pattern Provider plan，不消耗输入
  已编码 package_pattern 精确匹配输入计划后生成对应颜色包裹
  两台包裹机器共用 PackageCapacityProfile 的 default/16k/64k/256k 映射与容量计算
  已编码 package_pattern 走 exact package plan，并在本地执行与 Pattern Provider push 时复验当前容量档；不能绕过空槽 9/9 上限
  已编码 package_pattern 不消耗，可重复作为本地装配计划
  已编码 advanced_processing_pattern 保存连续列的有序多包裹计划
  package_assembler 可按 advanced_processing_pattern 逐包生成匹配包裹
  本地样板槽接受任意 AE2 可解码已编码样板；普通 crafting/processing/stonecutting/smithing 等样板按非空输入槽顺序生成 Fluix 包裹，主输出归一为 marker
  advanced_processing_pattern 本地输入显示跳过 sparse 空白并保留原列归属，滚动行数按全部非空输入动态计算，超过旧 68 格的输入仍可显示、插入和装配
  package_assembler 暴露 AE2 ICraftingMachine capability
  Pattern Provider pushPattern 可按分子装配室语义临时使用本次 pattern 规划配方，把 KeyCounter 中的物品/流体 GenericStack 输入装配为包裹
  空本地样板槽的普通 Pattern Provider pushPattern 直接从 KeyCounter 规划包裹，避免 9 格临时输入缓存限制
  本地三类样板先逐个预计输出包裹做容量预检；超限样板保留显示但槽位标红，GUI/外部输入与装配锁定
  Pattern Provider pushPattern 在消费 KeyCounter 前执行同一预检；高级样板逐列检查，任一包超限时整批拒绝
  pushPattern 在容量不足、输出阻挡、输入缓冲非空或规划失败时整批拒绝且不消耗输入；本地样板槽兼容路径遇到无法转成 ItemStack 的 AEKey 时同样拒绝
  彩色 Pattern Provider pushPattern 读取 AE2 sparse input 槽位，按输入槽颜色拆成多个包裹
  彩色 Pattern Provider pushPattern 支持流体 AEKey 输入
  同 AEKey 位于不同颜色槽时按 sparse 槽位拆分，不被 AE2 condensed input 提前合并
  彩色 pushPattern 产生多个包裹时按顺序写入空输出槽，超过可用输出槽的余量通过 pending queue 顺序输出并持久化保存
  装配室存在 0-100 合成进度，只允许 5 张 AE2 speed card；0/1/2/3/4/5 张按分子装配室速度表每 tick 尝试推进 10/13/17/20/25/50，并按 1.0/1.3/1.7/2.0/2.5/5.0 能量倍率从本机 AE 网络抽取能量
  本地合成期间输入槽保持真实可交互，材料到达 100 进度才提交；取出必需材料时保留进度并暂停，补齐后继续，样板变化时取消计划并归零
  装配室输出模式默认 ME_NETWORK，可通过 GUI 左侧 AE2 toolbar 图标循环切换 ME_NETWORK、ADJACENT_BLOCK 和 NONE 并持久化保存
  装配室 server tick 会按输出槽顺序一次导出 1 个包裹；ME_NETWORK 只导出到本机接入的 AE 网络存储服务，ADJACENT_BLOCK 只导出到背面 Forge item handler，NONE 不自动导出
  自动导出失败时保留输出槽包裹，不丢弃、不继续消耗新输入
  外部 Forge item handler 按本地样板动态暴露 N 个稠密过滤输入位与紧随其后的 1 个有序输出位；输入位不可抽取，输出位每次只抽取 1 个合法包裹
  放入本地样板后颜色和 marker 以样板为权威；装配室不提供机器 fallback 配置或可编辑槽
  真实 AE2 Creative Energy Cell + Pattern Provider + Package Assembler GameTest smoke
  真实 AE2 Creative Energy Cell + Pattern Provider + Package Assembler 彩色处理样板 GameTest smoke
  真实 AE2 Drive + 64k item cell + Crafting CPU + Pattern Provider + Package Assembler 自动合成 job smoke
  真实 AE2 Creative Energy Cell + Drive + Interface + Package Assembler ME_NETWORK 输出 GameTest smoke
  装配室基础 GameTest

客户端验证：
  当前输入窗口按样板非空输入数建立真实槽并跳过高级样板 sparse 空白；过滤物品与数量只用于插入校验、不在空槽内绘制，只有真实输入显示为槽内容；样板移除后残留输入保持红色错误状态，左侧 toolbar 与右侧 AE2 升级面板继续沿用现有布局；后续视觉改动按需使用 runClient 人工复验
```

验收：

```text
普通 AE2 合成/处理等样板生成默认 Fluix 包裹并以主输出为 marker
本地合成中取料不产出且暂停，补回后继续并在完成时一次性扣料
本地完成扣料必须原子成功后才能提交包裹；重复样板输入位置在真实槽、contents 与拆包推入中保持分离
彩色输入格生成对应颜色包裹
同 AEKey 位于不同颜色格时不会提前合并
输出阻挡只检查本机输出口，输出模式不扫描目标网络内容
```

## 阶段 6：终端与总线

当前交付：

```text
合并后的高级/包裹样板终端
包裹存储总线 AE2 cable part
包裹卸货总线 AE2 cable part
七行包裹过滤 UI 与 AE2 网络集成
GameTest 与客户端人工验证
```

2026-07-12 范围修订：独立 Package Pattern Terminal 与 Package Export Bus 取消；物品注册、配方、创造栏入口、loot 和对应客户端自动截图步骤均删除。2026-07-13 代码审查进一步删除了三个旧 Package Bus 方块/方块实体和独立终端方块/part/菜单的无入口兼容壳。2026-07-17 最终交互边界改为：包裹页合并进 Advanced Pattern Encoding Terminal，普通 AE2 Pattern Encoding Terminal 不再增加包裹页面。

当前状态：

```text
原版 Pattern Encoding Terminal、Screen factory 与四种原生模式保持 AE2 实现；只用一个窄菜单校验注入拒绝 package_pattern 与 advanced_processing_pattern，不增加按钮或绘制
Advanced Pattern Encoding Terminal 使用同一个 part/menu/screen 承载 ADVANCED 与 PACKAGE 两页，右侧 current-AE Pattern Encoding Terminal 水平侧标签切换并持久化页面；两页各自拥有完整屏幕 profile（两行网络库存时 217x250 / 195x233）和完全隔离的槽位库存，切换时在同一 Screen 上 resize/init 并重排全部几何，放入对应载体时自动切换，菜单不创建 VIEW_CELL 槽
package_storage_bus 使用新版 Storage Bus 形态，通过默认优先级 0 的 IStorageProvider 挂载仅接受合法包裹的 PackageItemStorage；Partition Storage 从相邻容器包裹生成过滤
package_unpacking_bus 使用新版 Pattern Provider 面板形态，通过默认优先级 0 的 Formation Plane 式只写入 IStorageProvider 接收网络路由包裹，不扫描、抽取或枚举 ME 存储
两个默认值都为 0，且就是右上 Priority 子菜单显示/修改的数值；数值相同时由卸货总线只写入端点的 preferred-storage 语义先尝试拆包，拆包拒绝后再尝试存储总线
两个总线均为 PartItem，复用同一 176x253 AE2 ScreenStyle 与右侧 5 格共享升级面板；存储总线保留 Storage Bus 工具栏，卸货总线左侧只有 Help、清空和 Pattern Provider 阻挡模式
七行过滤每行包含动态模糊/反转按钮、可为空的颜色选择、marker ghost 和 6 个物品 ghost；颜色空模式不过滤，行间 OR、行内 AND；所有颜色入口复用统一触发按钮/弹窗，只有两种总线过滤区启用 None 与右键清空，Fluix/None 固定在分隔线左侧上下排列且隐藏 None 不改变布局
默认解锁底图最上方两行，每张容量卡额外解锁一行，五张容量卡时达到七行上限；未解锁行使用 OptionalFakeSlot 半透明叠加
模糊/反转按钮仅在对应升级卡存在时显示并始终紧邻颜色按钮，模糊/反转/颜色三个 8px 按钮在 18px 行内统一使用固定 2px 上边距；卸货总线在同一 5 格升级库存中额外接受最多 4 张加速卡
存储总线遮掉右上工作区；卸货总线工作槽同步真实 held 包裹并显示 15 级进度条，工作中不可取、阻塞时可由玩家取回
网络接收与最终提交都校验过滤、整包累计容量和 Pattern Provider 阻挡条件；最终目标变化时保留原 held 包裹并阻塞重试，held 状态写入 Part NBT，拆除 part 时作为额外掉落返还
总线视觉变更按需在 runClient 中放置真实 AE2 part 并人工检查
```

验收：

```text
总线只允许包裹通过
存储总线不暴露包裹内部内容
存储总线 Partition Storage 按容器内合法包裹生成过滤，空容器清空过滤
卸货总线与存储总线的默认优先级都为 0；数值不同时由玩家设置的较高值优先，数值相同时卸货总线优先，卸货拒绝后存储总线接收
卸货总线只按整包执行 check-then-push
卸货总线在进度完成前不写入目标；阻挡或最终模拟失败时不写入并重试同一个包裹
取消项不存在玩家入口
```

## 阶段 7：序列缓存器

交付：

```text
SequenceBufferBlock / SequenceBufferBlockEntity 注册、物品、配方与 loot
五值模型状态、六向 facing、X/Y/Z axis 与扳手交互
端点权威的直线拓扑、尾端自动加入、断裂/解散和配置同步
一次输入锁存的 AEKey 存储、Forge item/fluid handler 与 AE2 MEStorage
端点顺序输入、合并抽取、自动输出、阻挡、同步和全结构输入延迟
PackageData sparse 布局身份扩展、Pattern Provider 与拆包总线原子位置输入
第一版服务端配置/过滤接口，不注册 GUI
GameTest、runData、build、资源/文档/发布审计
```

实施顺序：

```text
1. 固定 state/config/NBT schema 和纯计划类
2. 注册方块、方块实体与单格 capability，验证锁存/延迟/保存读取
3. 实现端点拓扑、成员排序、尾端加入和断裂恢复
4. 实现端点聚合 capability 与普通/同步自动输出
5. 扩展 PackageData 布局并实现 Pattern Provider/拆包总线的 sparse 位置输入
6. 实现高级样板稠密顺序例外和三套 capability
7. 补齐五类模型、语言、配方、loot 和资产 contract
8. 扩展真实 Pattern Provider、拆包总线与多方块 GameTest，执行发布相关审计
```

验收：

```text
单格只接收一次并在完全清空后解锁
结构成型/拒绝/扩展/断裂后的端点、顺序、配置和缓存内容正确
X/Y/Z 三轴都可成型，且成型、扩展和解体不改写各方块原有方向
端点不存储，逻辑第 1 格从首个成员开始；输入和抽取顺序稳定
阻挡、同步和输入延迟同时约束主动与被动输出
普通样板 push 保留 sparse 空位，高级样板只用稠密顺序；失败不消费 KeyCounter
拆包总线把包裹布局原子映射到序列缓存器，失败不消费 held 包裹
物品、流体与其它 AEKey 都有不丢失的受支持路径
五类模型在三轴、六向合法组合下无 missing model/texture，侧面箭头始终指向自身正面
```

当前状态：

```text
已完成当前首版：方块/方块实体、五类模型状态、端点拓扑、三套存储能力、一次输入锁存、顺序输入/合并抽取、自动输出、阻挡、同步、输入延迟、普通 sparse 样板、高级样板稠密顺序、包裹布局身份与拆包总线原子保序输入均已实现。
2026-07-16 全量 runGameTestServer 132/132 通过，包含真实 AE2 扳手三段方向循环；build、runData、资源审计及负例、文档审计、机械发布审计及负例通过；真实 runClient 完成资源重载、OpenAL 与图集创建，未发现 sequence_buffer missing model/texture 或 ModelBakery 错误。第一版按范围不含 GUI。
2026-07-17 已用确定性脚本把用户 64x64 原图拆成 16 张 16x16 面贴图，并生成覆盖 X/Y/Z 三轴和六向 facing 的 57 个显式模型、58 个 multipart 项；结构方向与方块自身方向分离，成型/扩展/解体均保留原 `facing`。135/135 GameTest、build、资源审计及全部负例、文档审计、asset contract/发布资源审计通过；现有修改前启动的 IntelliJ 客户端未被中断，最终世界内像素效果需重启该客户端后人工复核。
```

## 阶段 8：发布

交付：

```text
runData
build
runGameTestServer
runClient（需要视觉验收时人工执行）
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
  scripts/verify-release.ps1 已覆盖 jar 元数据、必需条目、jar 内文档/语言/Applied Packaging 发布资源源文件同步、dev/test/reference 条目排除、本机路径泄漏、资源 JSON/PNG、玩家入口产品不变量、asset contract、语言 key/占位符、模型贴图引用、dedicated server latest.log 证据和可选 git clean 证据
  scripts/run-release-checks.ps1 已编排 build、runData、runGameTestServer、可选 run-server-smoke、机械发布审计、文档审计、发布清单和发布附件包
  scripts/run-release-checks.ps1 -ReleaseCandidate 已作为最终候选发布预设，自动启用 server smoke、manifest 和 bundle 审计
  scripts/verify-release-readiness.ps1 已作为 tag 就绪审计，-RequireReadyForTag 会阻止状态、迁移目标或验证要求仍为待输入、待判定、阻塞或失败的 intake 项创建发布 tag，并要求已填写的迁移目标是仓库内已存在文件的规范相对路径、不包含父级遍历，且与类型目标族匹配：需求类落在 docs/01、02、03、05、06 或 07，材质类落在 docs/04、docs/assets 或 src/main/resources/assets/appliedpackaging；负面 blocker 清除后还要求文档明确记录范围已冻结、最终服务端 world-load 已完成、发布 tag 可创建、目标可以标记完成和 tag 就绪门禁已通过
  dedicated server world-load 已在当前基线通过；服务端 latest.log 审计可由 run-server-smoke.ps1 或 run-release-checks.ps1 -RunServerSmoke 刷新后执行
  当前服务端证据已通过 run-release-checks.ps1 -AuditOnly -RequireAssetContracts -RequireServerWorldLoad
  当前自动服务端 smoke 已通过 run-release-checks.ps1 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke
  2026-07-04 候选发布基线已通过 run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit，包含当时的 build、runData、112 个 GameTest、dedicated server Done (2.471s)、release audit、asset audit、docs audit、manifest audit 和 bundle audit
  当前非 UI 收尾基线为提交 57a9688；compileJava、170 个必需 GameTest、build、docs audit、release audit 与 clean-git audit 均有通过记录
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
  0.1.0-dev 发布 tag 暂缓到 IN-003 正式 UI/模型范围与 IN-007 序列缓存器均实现、验收并完成最终服务端与 tag 就绪门禁之后
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

GenericStack 范围风险：

```text
1.0 垂直切片优先验证物品。
数据模型完整支持 AEKey；对未知 key 保守拒绝拆包，避免吞资源。
流体内容继续由 `GenericStack` / `AEFluidKey` 数据模型和 AE2 MEStorage 路径承载；没有正式运行时调用的 Forge fluid handler adapter 不纳入发布代码。
```

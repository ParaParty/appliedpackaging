# Applied Packaging 开发日志

## 2026-07-19 高级终端清空后的第一列客户端残影

用户复测发现：高级样板列数较多并滚动后点击总清空，服务端状态已经清空，但当前 Screen 的第一列仍显示旧物品；关闭重开后
恢复正常。根因不是清空事务或第一列漏清，而是 `AdvancedPatternInputWindow` 在客户端与服务端都直接映射完整 81×81
`AdvancedPatternEncodingState`。Vanilla 菜单只对 4×81 个动态窗口槽做前后差量同步；客户端滚到后续列时，隐藏的第一列仍留在
完整状态缓存。服务端清空并把首列归零后，只需要发送相对上一可见窗口发生变化的槽，客户端随后重新读取完整缓存，便把未被
本次差量覆盖的旧第一列重新显示出来。

当前窗口改为明确的双端语义：服务端继续把 4×81 个槽映射到当前绝对列，客户端则只在窗口自身保存这 324 个槽的显示缓存，
不再保存或读取隐藏列。滚动前后的未变化槽可以安全沿用，变化槽由原生差量包覆盖；清空、删列和滚动归零也不会重新暴露旧
绝对列。没有增加“清空第一列”特判，也没有在每次滚动时发送完整 6561 槽矩阵。

`compileJava` 与完整 `build` 通过；现有 `runGameTestServer` 全部 166/166 required tests 通过，确认服务端高级样板状态、编码、
清空及列操作没有回归。真实 `runClient` 完成 Applied Packaging 初始化、资源重载、OpenAL 与全部 texture atlas 创建后，仅终止
本轮启动的客户端进程；用户原有开发客户端未被停止。由于问题发生在客户端动态菜单槽缓存，服务端 GameTest 无法直接断言
屏幕残影且项目没有自动操纵该 Screen 的客户端测试，最终复现路径保留为人工验收：创建多列、滚到后续列、点击总清空，确认
第一列立即为空且重开前后显示一致。

## 2026-07-19 Package Bus 帮助按钮与工具栏统一所有权

实机截图中的重叠不是帮助按钮坐标本身错误，而是 `PackageBusScreen` 绕过了其它界面已经使用的统一工具栏捕获流程。
`AEBaseScreen` 在构造阶段把 GuideME `OpenGuideButton` 注册进原生 `VerticalButtonBar`；Package Bus 另建
`toolbarButtons` 并通过 `ModernVerticalToolbar.setButtons` 只交付自己的清除、分区和配置按钮。两组列表都从左侧第一格
开始布局，因此帮助按钮与第一枚自有按钮占用同一坐标。Package Bus 的 `ModernActionButton` /
`ModernServerSettingToggleButton` 还会自行绘制新版背景，随后又被 toolbar renderer 覆盖一次，形成第二条重复绘制路径。

当前实现先调用 `super.init()` 注册帮助按钮，再把 Package Bus 自有按钮注册为 Screen children，最后由
`ModernVerticalToolbar.captureIconButtons(children())` 一次性捕获、排序、禁用旧背景、布局并创建 overlay。帮助按钮因此固定
排在最前，后续自有按钮按注册顺序排列。Package Bus 的两种专用自绘子类、`setButtons`、`appendButton` 和公开手动画法均已
删除；高级终端也改为先注册颜色模式按钮再捕获。review 确认现代工具栏只有三类持有者：高级终端、
`ModernUpgradeableScreen` 及 Package Bus，三者现全部走同一 capture-based ownership 路径。

`compileJava`、完整 `build`、`verify-assets.ps1` 和完整 `test-assets-audit.ps1` 已通过；新增负例删除 Package Bus 的统一捕获
调用后按预期失败，另有门禁禁止重新增加独立按钮列表或公开手动渲染入口。本次只修改客户端按钮注册、布局和绘制所有权，
没有服务端菜单状态或机器事务变化，因此未运行 GameTest。真实 `runClient` 完成 Applied Packaging 初始化、资源重载、OpenAL
和全部纹理图集创建后主动停止；控制台没有 Applied Packaging 类加载或资源错误。项目没有自动打开 Package Bus 并点击按钮的
client test，最终按钮间距和点击后像素仍由重启后的开发客户端人工验收。`verify-docs.ps1` 已执行，但当前被并行进行的
GuideME 分层目录重构阻塞：审计仍要求旧版扁平页面并报告 29 项旧路径、分类与相对链接问题；本轮不修改或回退该范围。

## 2026-07-18 GUI 资源清理、装配室配置与高级样板颜色模式

清理 17 个未被运行时引用或已被共享资源替代的 GUI PNG：删除独立 `textures/gui/icons/` 占位图标、GUI 内重复 logo、
旧 `pattern_encoding_terminal.png` 和重复的 advanced terminal states atlas。缓存 current AE2 `terminal.png` 为
`ae2-terminal.png` 并固定来源、许可证与 SHA-256；高级终端搜索框、置顶合成行和状态按钮统一读取新版缓存。序列缓存器
改用 current-AE2 big scrollbar sprite，包裹装配室的小滚动条向右校正 1px。资源审计新增旧 terminal atlas、旧序列缓存器
滚动条、装配室旧坐标和颜色模式按钮错误绘制顺序的负例。

包裹装配室新增持久化的 17 色选择与 marker 过滤槽。无样板或普通 AE2 样板使用玩家选择色；包裹样板使用编码色；
高级样板仅在所有列同色时显示该色，混色显示“无”；进行中的 provider/local 任务以活动包裹颜色为准，任务结束或样板
取出后恢复玩家选择。marker 过滤仅覆盖普通样板的 marker，包裹样板和高级样板仍以编码数据为准；菜单同步只读的
有效 marker 显示栈，因此放入样板或执行无本地样板的 `pushPattern` 时，marker 槽与 tooltip 临时显示实际任务 marker，
混合 marker 显示为空，任务结束后恢复原配置。该显示覆盖不改写真实 FakeSlot，工作期间颜色、marker、
自动输出和阻挡配置在客户端禁用且服务端拒绝修改，但 tooltip 保持可见；玩家物品栏和真实材料槽仍可操作。容量元件槽
tooltip 说明用途并同步显示当前单位与类型上限。

高级样板颜色模式迁移为左侧 AE toolbar 中排在原生功能之后的功能按钮，不再占据帮助按钮之前的第一位，也不再伪装成当前列颜色。默认模式只给新增列分配 Fluix；循环模式
优先取前面列尚未使用的颜色，17 色均已使用后从最后一列的下一色继续循环。切换模式不重染已有列，手动新增、扩列、
转置新增和 JEI 填充新增列共用同一分配规则。左侧按钮先由原生 widget 建立交互，再由 current-AE2 overlay 最后绘制，
避免旧 AE2 图标覆盖新版 sprite。点击后残留的额外边框来自 Minecraft 1.20.1 持久保留 widget focus：覆盖层虽然只按
normal/hover 选择背景，但 AE2 15 的原生 `IconButton` 在禁用旧背景后仍会单独绘制 1px 白色 focus 框，覆盖层没有盖住的
上边缘因此继续可见。现代工具栏现报告自己的按钮成员；高级终端、包裹总线和共用机器 Screen 在鼠标点击由工具栏按钮
处理后立即释放该按钮的 Screen focus，其它控件和键盘焦点不受影响。

新增和扩展 GameTest 覆盖装配室颜色/marker 保存往返、普通与包裹/高级样板的有效显示覆盖、活动混色/共同 marker、
配置不被显示覆盖改写，以及进度为 0 时的工作锁，
以及颜色模式切换、17 色循环和 JEI 式 recipe replace；全量 `runGameTestServer` 的 165/165 required tests 通过。
`build`、`verify-assets.ps1`、完整 `test-assets-audit.ps1`、`verify-docs.ps1`、`test-release-audit.ps1`、
`verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。使用隔离的 `run-gtceu-fork` 执行普通
`runClient`，客户端完成 Applied Packaging 初始化、资源重载、声音与全部纹理图集创建；日志未发现 Applied Packaging 的
缺失 texture/model/ScreenStyle 或加载异常，仅保留既有 10x8 magenta package side 降低 mip level 的普通警告。项目当前无
自动菜单截图 runner，因此交互布局仍需在用户开发客户端重载后做最终实机视觉确认。

针对原生 focus 框的补充修复已通过 `compileJava`、完整 `build`、`verify-assets.ps1` 和完整
`test-assets-audit.ps1`；新增负例会移除高级终端的点击后焦点释放并确认审计失败。真实 `runClient` 完成 Applied Packaging
初始化、资源重载、OpenAL 和全部纹理图集创建后主动停止；用户现有客户端占用 `latest.log/debug.log` 仍只产生既知轮换
警告。本次只修改客户端按钮焦点生命周期，没有服务端状态或机器事务变化，因此未重复运行 GameTest；界面内点击一次后的
像素结果仍需在重启后的开发客户端人工验收。

## 2026-07-16 包裹渲染二次居中与帘后显隐

按用户实机复测继续收敛三个位置。ME Packager 的包裹内侧中心从本地 `x=2.5/16` 后移到 `x=1/16`；包裹经
`FIXED 0.5 × BER 1.49` 缩放后的前半深度约为 3px，因此端点前缘现在收在 `x=3..4/16` 的帘子后。外侧静止中心
仍为 `x=10/16`，完整位移相应从 7.5px 改为 9px，传送带 UV 相位继续使用同一位移而不产生速度漂移。

包裹 GUI transform 从上次过度校正的 `translation [0,3,0]` 收回为 `[0,2,0]`。盒体中心到 GUI 枢轴的 3px
差值经过 30° 旋转和 0.75 缩放后投影约为 1.95px，2px 比直接补满原始 3px 更接近视觉中心；共享模型与 marked
BEWLR 模型保持一致。Shift `IItemDecorator` 的 8x8 marker 从右下角移到左下角，避开右下角数量文本；前脸 3x3
marker 不变。资源审计固定新 transform，负例改为确认旧 `[0,3,0]` 会被拒绝。

`.\gradlew.bat compileJava processResources --stacktrace`、`.\gradlew.bat runGameTestServer --stacktrace` 与
`.\gradlew.bat build --stacktrace` 成功，118/118 required GameTest 全部通过；传送带测试新增精确 9px 位移断言并继续
覆盖完整相位及保存往返。`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1`、
`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。自动客户端 smoke 已从项目删除，当前
没有能自动操作打包机动画、Shift 键和物品数量叠加的客户端场景，因此最终像素位置留给下一次开发客户端人工复核。

## 2026-07-16 包裹物品居中、Shift marker 与打包机朝向

对照 Minecraft 1.20.1 `ItemRenderer` 源码排查包裹物品偏下：marked package 的 BEWLR `+0.5` 平移只是抵消递归
普通 baked model 渲染产生的第二次 `-0.5`，不能删除。实际盒体 `y=1..9` 的几何中心为 5，GUI 枢轴为 8，因而在
共享 `_transforms.json` 与 marked custom model 的 GUI display 中统一增加 `translation [0,3,0]`，不影响地面、手持、
物品框或机器中的高度。

为 17 色包裹注册 Forge `IItemDecorator`：物品 GUI 中按住 Shift 且包裹存在物品 marker 时，在原图标右下角额外绘制
8x8 marker；前脸原有 3x3 marker 仍保持。ME Packager 只调整客户端包裹姿态，在机器局部坐标中增加 `Y +90°`，
让 `FIXED` 变换后的包裹正面朝向本地 +X 工作口，再跟随四向 `facing` 统一旋转。

本轮只改客户端渲染，已考虑现有 GameTest 但不新增或重复运行。自动 `runClientSmoke` 已从项目删除，且用户调试客户端
正在占用同一 `run` 目录，因此未并发启动新客户端，保留重启调试客户端后的人工视觉确认。执行 `compileJava
processResources`、`build`、`verify-assets.ps1`、完整 `test-assets-audit.ps1`、`verify-docs.ps1`、
`verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过；资源审计新增 GUI Y 偏移回退负例。

## 2026-07-16 重复样板输入分离与装配扣料修复

普通样板的输出一致性校验由按 AEKey 聚合总量改为逐位置比较 key 与 amount，确保原样板中分开的重复物品在装配室
真实输入槽和包裹 contents 中仍是独立条目。拆包 item handler 提交明确按 contents 的原始顺序逐条推入，不在推入前
聚合重复 key；拆入带对应样板的装配室时会分别填充对应过滤槽。

本地合成完成扣料改为先在输入数组副本中验证并计算全部扣除，只有所有槽位都能精确扣除时才替换真实状态并提交
包裹；任一扣料失败时保留输入和进行中计划，不允许出现未扣料却生成包裹。新增拆包到装配室的端到端 GameTest，覆盖
两个独立橡木板条目、两个真实输入槽、合成中保留、完成时同时扣除和输出条目保持分离。执行
`.\gradlew.bat compileJava` 与 `.\gradlew.bat runGameTestServer` 成功，116/116 个 required GameTest 全部通过。

## 2026-07-16 装配室任意样板与可交互合成输入

包裹装配室的本地样板槽从三种硬编码载体扩展为任意可由 AE2 `PatternDetailsHelper` 解码的已编码样板。普通
crafting / processing / stonecutting / smithing 等样板按非空输入槽的编码顺序生成默认 Fluix 包裹，主输出归一为
数量 1 的 marker；crafting pattern 的相同物品重复槽保持为多个有序 contents 条目。Pattern Provider 的普通样板路径
同步采用该语义，并继续在容量预检通过后才消费 `KeyCounter`。

本地合成改为分子装配室式延迟扣料：开始进度时保存样板快照和预计包裹，但真实输入继续留在槽中并允许玩家交互；
缺少必需输入时暂停且保留当前进度，补齐后继续，样板变化时取消计划并把进度归零。只有到达 100 且输入重新核对
通过后才一次性扣料并提交输出。进行中的本地预览包裹不在破坏方块时额外掉落，避免与仍存在的输入重复。

新增 GameTest 覆盖普通 crafting pattern 的 Pattern Provider 推送、Fluix 色、主输出 marker、重复输入顺序、本地
合成中取料暂停、补回继续和完成时扣料，以及待处理本地计划的持久化与破坏掉落去重。执行 `.\gradlew.bat compileJava`
与 `.\gradlew.bat runGameTestServer` 成功，115/115 个 required GameTest 全部通过。本轮没有修改 ME Packager 代码或语义。

## 2026-07-13 包裹样板独立物品

将普通 AE2 Pattern Encoding Terminal 包裹模式的编码产物切换为独立 `appliedpackaging:package_pattern` 物品；保留旧
AE2 crafting_pattern + Applied Packaging NBT 的读取与解码兼容，并让新物品直接显示完整包裹样板 tooltip。

## 2026-07-13 包裹装配室透明内腔黑底修复

用户近距离截图显示新版 Molecular Assembler 空心模型会把下方地面顶面剔除，透明内腔因而显示黑色。根因是
`APBlocks.PACKAGE_ASSEMBLER` 在模型替换后仍使用完整遮挡的 `machineProperties()`。注册改用与 ME Packager 相同的
`cutoutMachineProperties()`，即 `noOcclusion()` 且 `isRedstoneConductor=false`；保留上游模型的底面 UV/cullface，不以模型补面
掩盖错误方块属性。

新增 `packageAssemblerDoesNotOccludeItsTransparentChamber` GameTest，直接断言装配室不参与遮挡且不是完整红石导体；
`runGameTestServer` 成功，175 个 required GameTest 全部通过。隔离世界 `AP Smoke Assembler Occlusion` 的 client smoke
成功刷新 11 张截图，世界总览中装配室透明内腔下方地面正常显示，未再出现黑底。

## 2026-07-13 两种包裹样板用户材质替换

将普通 Pattern Encoding Terminal 包裹模式产生的 `package_pattern` 图标替换为用户
`pacakge_pattern.png`，将 Advanced Pattern Encoding Terminal 产生的 `advanced_processing_pattern` 图标替换为用户
`adv_processing_pattern.png`。后者既有 item model 指向 `packaged_processing_pattern.png`，因此只替换运行时材质，不改变
注册 ID 或编码逻辑。两张图片均以原字节 16x16 RGBA 接入，资产合同、规格和 `verify-assets.ps1` 同步记录尺寸与 SHA-256。

## 2026-07-13 AE2 v19 总线与包裹装配室模型替换

按用户确认以 AE2 `neoforge/v19.2.17`（Minecraft 1.21.1）为高版本模型来源，固定 commit
`79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a`。Package Storage Bus 改用新版 Storage Bus part 几何、侧面、
背面和状态灯并替换用户正面；Package Unpacking Bus 改用新版面板形态 Pattern Provider part 几何，替换用户正反面；
两者 item model 同步切换。ME Package Assembler 改用新版 Molecular Assembler 13-cuboid cutout 模型、用户完整表面、
动画发光层，并新增 Forge 1.20.1 BER，在工作期间显示当前包裹和 AE2 原装 crafting particle 动画。动态状态在开始/完成时
通过 block update 同步客户端。

资产合同记录四张用户 PNG 的 SHA-256、上游 tag/commit/路径和 LGPL 要求。`verify-assets.ps1` 新增新版模型结构、用户图哈希、
16x16 base 与 16x192 动画灯带尺寸门禁，并将装配室从旧 32x32 opaque 假设迁移为 cutout 模型规则；
`test-assets-audit.ps1` 的 opaque 负例改由仍为 solid 的 Package Export Bus 承担。

验证：两个 assetgen contract validation 成功；`compileJava processResources`、`build`、完整资产审计负例套件、文档审计、
带资产合同的机械发布审计均成功。隔离世界 `AP Smoke V19 Models` 的 client smoke 成功生成 11 张界面/世界截图，
资源加载日志没有 missing model/texture；世界总览确认新版透明装配室模型与两种 cable part 均成功烘焙。
`runGameTestServer` 成功，174 个 required GameTest 全部通过。

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

最新进展：

```text
继续收尾终端与三种总线的非资产缺口：
  对照 AE2 15.4.10 StorageBusPart、UpgradeablePart、IStorageProvider 和 ConfigInventory 源码复查当前实现。
  三种包裹总线节点加入 GridFlags.REQUIRE_CHANNEL，保持目标面不接 AE、其它面接网的既有边界。
  新增真实存储总线在线刷新测试：初始挂载后向相邻箱子增删包裹、启用 RED 过滤和清除过滤，AE storage cache 都在轮询重新挂载后得到正确 key。
  Package Bus 与 Package Pattern Terminal 菜单在服务端 broadcastChanges 前从 host 重建 ghost display，另一个菜单或外部逻辑修改 marker/content/processing output 后，已打开菜单不再显示旧图标。
  AdvancedProcessingPatternDataStorage 在 PackageColumn 数据边界把 marker 固定归一为数量 1。
  新增真实 Advanced Pattern Terminal 编码测试，从 AE2 blank pattern 编出独立 advanced_processing_pattern，并验证两列输入、颜色、名称、marker、空白样板消耗和未启用列残留忽略。

首次执行 164 个 GameTest 时，advancedPatternTerminalEncodesDedicatedPattern 失败并暴露真实 marker 丢失。原因是 AdvancedPatternEncodingState 使用 ConfigInventory.CONFIG_TYPES，AE2 会把类型槽 GenericStack amount 固定为 0，旧 marker() 误用 amount <= 0 判断空槽。改为读取 getKey() 并在编码数据中写入数量 1 后复跑通过；没有删除或放宽断言。

验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 13 个 deprecation warning
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，164 个 required GameTest 全部通过
验证 .\gradlew.bat runClientSmoke --stacktrace 成功；人工复查 Advanced Pattern Terminal、Package Pattern Terminal 和 Package Storage Bus 截图，布局与预填同步状态保持完整
验证客户端日志发布阻断关键字无命中
验证 .\gradlew.bat build --stacktrace 成功
验证 scripts/verify-assets.ps1 成功，144 个 PNG 通过资源门禁
验证 scripts/verify-docs.ps1 成功
验证 scripts/verify-release.ps1 -RequireClientSmokeScreenshots 成功，231 个发布资源与 jar 同步，9 张必需截图有效，日志无发布阻断关键字
验证 scripts/verify-release-readiness.ps1 预审完成；仅保留 3 个预期 blocker：IN-003 正式 UI/模型待输入、变更接收/最终服务端验证仍开放、产品目标与 release tag 尚未完成
GameTest：已考虑并运行。本轮改变 AE channel、IStorageProvider 在线缓存刷新、菜单 ghost 同步和高级 marker 编码语义，属于行为敏感变更；新增 5 个 GameTest 并通过全部 164 个 required GameTest。
```

最新进展：

```text
深审 Advanced Pattern Terminal 列编辑层后修正 modal 层级与输入边界：
  旧实现从 drawBG 绘制弹层，并在弹层内继续调用 super.mouseClicked/released/dragged/keyPressed/charTyped；AE RepoSlot、processing slot、底层按钮和 tooltip 仍可能覆盖或接收输入。
  弹层改为 super.render 完成后绘制不透明前景；只手工分发色板、名称框与当前 marker fake slot 输入，其余鼠标/滚轮/键盘/字符事件全部吞掉，外部点击只关闭弹层。
  弹层打开时隐藏 encode/clear/cycle 控件，处理输入/输出槽继续停用；marker 物品、光标携带物和弹层 tooltip 在前景重绘。
  ClientSmokeRunner 在同一 Advanced Pattern Terminal 中依次拍主界面与列编辑层，并等待 Screenshot.grab 回调完成后才切换 screen，避免异步 GPU 截图读取与打开/关闭菜单竞态。
  verify-release.ps1 必需清单加入 appliedpackaging-client-smoke-advanced_pattern_encoding_terminal_editor.png，当前门禁为 10 张截图。

验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 13 个 deprecation warning
验证 .\gradlew.bat runClientSmoke --stacktrace 成功；人工检查主界面与列编辑层截图均无黑块，弹层覆盖网络库存，底层编码控件未穿出，色板、名称框和 marker 位于前景
验证过程中先后暴露两类独立黑块：未封口的 post-super GuiGraphics 前景批次，以及 Screenshot.grab 回调前切换 screen；分别通过前后 flush 与截图完成握手修正，未把黑块误记为通过
验证 .\gradlew.bat build --stacktrace 成功
验证 scripts/test-release-audit.ps1 成功，release audit 2 条正例与 13 条负例全部通过；新增 10 张截图齐全正例和缺少 advanced editor 截图负例
验证 scripts/verify-docs.ps1 成功
验证 scripts/verify-release.ps1 -RequireClientSmokeScreenshots 成功，231 个发布资源与 jar 同步，144 个 PNG 非空，10 张必需截图有效；仅忽略 1 条外部 Yggdrasil 公钥获取警告，日志无发布阻断关键字
GameTest：已考虑。本轮只改变客户端绘制、输入拦截与 smoke 截图时序，不改变样板编码、物品移动、总线事务或服务端状态，因此不新增或复跑 GameTest。
```

最新进展：

```text
收紧 Package Pattern Terminal 自动化边界并补齐端点生命周期测试：
  对照 AE2 15.4.10 PatternEncodingTerminalPart 与 EnergyAcceptorPart 源码，确认原版样板终端只向 Forge item handler 暴露空白样板槽，持久 LazyOptional 在 part 拆除时必须失效。
  Package Pattern Terminal 的兼容方块与 AE2 part 均改为只暴露空白样板槽；预览输入、编码输出、容量和 marker 不再能被外部管道访问。
  兼容方块在 invalidateCaps/reviveCaps 间失效并重建受限 capability；AE2 part 在 removeFromWorld 时失效旧 capability，并为可能的重新加入创建新 capability。
  新增真实存储总线目标替换测试：相邻箱子移除后旧 package key 从 AE cache 卸载，新箱子放回后只挂载新 package key。
  复查 PackageFilter 与 Package Bus 手工过滤 UI：requiredContents 会按 key 合并并压紧，三个 ghost 槽是三个过滤条件容量，不承诺稀疏位置，因此未引入无依据的索引语义修改。

验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 13 个 deprecation warning
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，167 个 required GameTest 全部通过
验证 .\gradlew.bat build --stacktrace 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireClientSmokeScreenshots 成功，231 个发布资源与 jar 同步，144 个 PNG 非空，10 张必需截图有效；仅忽略 1 条外部 Yggdrasil 公钥获取警告
GameTest：已考虑并运行。本轮改变 Forge item capability 暴露范围、LazyOptional 生命周期和 IStorageProvider 相邻目标替换行为，属于行为敏感变更；新增 3 个 GameTest 并通过全部 167 个 required GameTest。
```

最新进展：

```text
修正在线路由总线修改过滤器时的非法 storage provider 刷新：
  审计 AbstractPackageBusBlockEntity、PackageExportBusBlockEntity、PackageUnpackingBusBlockEntity 与 AE2 IStorageProvider.requestUpdate / StorageService.refreshNodeStorageProvider 源码。
  旧基类在三种总线过滤器变化时一律请求重新挂载，但只有 Package Storage Bus 注册 IStorageProvider；在线 Export/Unpacking Bus 会触发 AE2 IllegalArgumentException。
  onFilterChanged 现在仍统一保存配置，但只对实际实现 IStorageProvider 的存储总线请求重挂载；输出与拆包总线在后续路由 tick 直接使用新过滤器。
  新增真实 powered/channel GameTest，同时上线 Package Export Bus 与 Package Unpacking Bus 并修改颜色过滤，确认两者保存新条件且不抛异常。

验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 13 个 deprecation warning
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，168 个 required GameTest 全部通过
验证 .\gradlew.bat build --stacktrace 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireClientSmokeScreenshots 成功，231 个发布资源与 jar 同步，144 个 PNG 非空，10 张必需截图有效；仅忽略 1 条外部 Yggdrasil 公钥获取警告
GameTest：已考虑并运行。本轮修正在线 AE grid 节点的过滤配置与 IStorageProvider 服务调用，属于行为敏感变更；新增 1 个真实端点 GameTest 并通过全部 168 个 required GameTest。
```

最新进展：

```text
补齐 ME Packager 与 Package Assembler 方块实体 capability revive 生命周期：
  全仓扫描持久 LazyOptional、invalidateCaps、reviveCaps 与 AE2 capability 实现，确认两台机器旧实现失效 capability 后没有重建路径。
  ME Packager 的内部/外部 item handler capability 改为可重建字段，reviveCaps 同时恢复两者。
  Package Assembler 的外部 item handler 与 AE2 CRAFTING_MACHINE capability 改为可重建字段，reviveCaps 同时恢复两者。
  新增 lifecycle GameTest，保留四个旧 LazyOptional handle 并确认 invalidate 后全部失效，再确认 revive 后重新获取的四个 capability 全部可用。

验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 13 个 deprecation warning
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，169 个 required GameTest 全部通过
验证 .\gradlew.bat build --stacktrace 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireClientSmokeScreenshots 成功，231 个发布资源与 jar 同步，144 个 PNG 非空，10 张必需截图有效，日志无发布阻断关键字
GameTest：已考虑并运行。本轮改变物品自动化与 AE2 crafting-machine capability 的区块实体生命周期，属于行为敏感变更；新增 1 个 GameTest 并通过全部 169 个 required GameTest。
```

最新进展：

```text
修复 Package Bus 与 Package Pattern Terminal 大数量 ghost 状态的菜单同步：
  反编译确认 Minecraft 1.20.1 ClientboundContainerSetDataPacket 对 DataSlot id/value 均使用 writeShort/readShort；旧单 DataSlot 在 32768 以上会截断或变成负值。
  新增 SplitIntDataSlots，把非负 int 拆成低/高两个 unsigned 16-bit word；客户端收到 signed short 后以 0xffff 归一并重组。
  客户端双 word 缓存固定从 0 开始，不从本地 host 预填；避免服务端真实值为 0 且初始 DataSlot 不发包时保留客户端陈旧数量。
  Package Bus 三个 required-content amount 与 Package Pattern Terminal 四个 processing-output amount 均改走双 DataSlot，同步范围覆盖到 Integer.MAX_VALUE。
  新增 splitIntDataSlotsSurviveVanillaShortTransport GameTest，覆盖 0、32767、32768、65535、65536、100000 和 Integer.MAX_VALUE。
  删除 ClientSmokeRunner 中仅为观察 100000 同步而额外写入的铁锭；标准三种 Package Bus 截图恢复为只显示 RED、钻石 marker 与 2000 mB water。
  人工查看截图时图片查看工具曾显示分块深色伪影；直接读取源 PNG 后确认玩家物品栏格子内部为 #E8ECEE，三张总线核心 GUI 区域 99600 像素中仅有 284-492 个正常差异，因此没有为查看工具伪影修改产品 Screen。

验证 .\gradlew.bat compileJava --stacktrace 成功，仅既有 13 个 deprecation warning
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，170 个 required GameTest 全部通过
验证 .\gradlew.bat runClientSmoke --stacktrace 成功；三种 Package Bus 源 PNG 均无额外铁锭，玩家物品栏与过滤槽像素完整
验证 .\gradlew.bat build --stacktrace 成功
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1 成功
最终 smoke 日志按 ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON|Mixin apply failed|InvalidInjectionException|IllegalClassLoadError|Timed out|timeout 扫描无命中；前一轮曾遇到外部 Yggdrasil AuthenticationUnavailableException/SSLHandshakeException，刷新 smoke 后消失
验证 pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireClientSmokeScreenshots 成功，231 个发布资源与 jar 同步，144 个 PNG 非空，10 张必需截图有效，日志无发布阻断关键字
GameTest：已考虑并运行。本轮改变菜单网络同步边界，属于行为敏感变更；新增 1 个 signed-short 往返 GameTest 并通过全部 170 个 required GameTest。
```

最新进展：

```text
执行正式 UI/模型调整前的非 UI 收尾审计：
  工作树从提交 57a9688 开始保持干净，未提前集成或推测 E:\resources\textures\appliedpackaging\ret 下新 UI/模型资产的用途。
  对照 docs/01 R8-R10 和 docs/03 第 10 节复查 Package Pattern Terminal、Package Storage Bus、Package Export Bus 与 Package Unpacking Bus。
  Package Pattern Terminal 已覆盖编码/拆分、AE2 part 持久化、blank-pattern-only 自动化 capability 及 invalidate/revive/remove 生命周期。
  三种总线已覆盖 REQUIRE_CHANNEL、目标面隔离、Storage Bus 在线重挂载、Export/Unpacking 过滤在线更新、真实 AE Drive 端点与失败恢复。
  全仓 TODO/FIXME/UnsupportedOperationException 扫描未发现新的功能占位；命中的 placeholder 仅为合法输入框文案，return null 仅为无命中/无可编码结果语义。
  docs/05、06、08 已把过期的 112/138 GameTest、6 张截图和 IN-001/IN-002 待输入记录更新为当前 170 个 GameTest、10 张截图与唯一 IN-003 待输入项。
当前没有已知未实现的非 UI 项；未完成项明确为 IN-003 正式 UI/模型/动画描述与实现，以及该范围后的 client smoke、最终 dedicated server world-load 和 tag 就绪门禁。

验证 .\gradlew.bat compileJava --stacktrace 成功，任务 up-to-date
首次单独 verify-release 读取 client latest.log 时，仅因 Mojang Realms 对开发占位 token 0 的 SignedJWT 解析异常命中通用 Exception 关键字；资源、模型、jar 和 10 张截图均已通过。
验证 scripts/run-release-checks.ps1 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke 成功；dedicated server 进入世界并出现 Done (3.435s)，25565 端口清理完成，刷新后的 release audit、asset audit 和 docs audit 全部通过。
GameTest：已复查既有覆盖；本轮仅修正收尾文档，不改变样板、总线、事务、网络或物品移动语义，因此不新增 GameTest；最新行为基线仍为 170 个必需 GameTest 全部通过。
```

最新进展：

```text
按用户最终视觉接入 Advanced Pattern Terminal：
  直接导入 E:\resources\textures\appliedpackaging\ret\adv-pattern-terminal-base.png 与 sprite.png，没有缩放、重绘或清理像素；目标文件 SHA-256 分别保持 660EF8C5379F1131E4D3D773FD43EE9954DE1F0FCE278DF78C30F75D9B5563F6 和 07D81B889A00D2E80113DCEDF58EAB188A694BFB47DB1D2F5B488AF242D901CF。
  对照本地 AE2 1.21.1 PatternEncodingTermScreen、ProcessingEncodingPanel 与 ScreenStyle JSON，将终端恢复为 195x250、9 列网络库存、AE2 标题/搜索/玩家栏基线；运行时仍保持 AE2 15.4.10 Forge。
  中间编码区按最终图显示 4 列 x 3 行真实输入和 3 行真实输出；逻辑上的第 4 行由左侧 AE2 Scrollbar 同步滚动输入/输出，底部水平滚动条只滚动最多 17 个包裹列。
  启用列使用 8x8 颜色 swatch 与编辑按钮，第一未启用列只显示加号，后续列只保留禁用底色；不绘制 processing ghost 物品。编码、清空、网络滚动与左侧工具栏继续复用 AE2 控件/行为。
  base/sprite atlas 作为 AE2 高版本适配资源标记为 LGPL-3.0-or-later；打包许可证与本地 AE2 1.21.1 LICENSE 的 SHA-256 均为 39676552FD16D3317F6E6AF4CF810778060CDA238F75DAC9B27C0FCBE848D3D4。
  clientSmoke 视口改为 960x540，使 250px 高主体在 GUI scale 2 下完整显示，不压缩用户提供的 atlas。

验证 .\gradlew.bat compileJava --stacktrace 成功
验证 .\gradlew.bat runClientSmoke --stacktrace 成功；12 张截图全部生成并正常退出，人工检查高级终端主界面完整无裁切，标题、9 列网络库存、输入/输出区、左右滚动语义、样板区与玩家栏对齐最终视觉
验证编辑层源 PNG 像素正常；图片查看工具显示的顶部/玩家栏黑块不是源截图内容，实际黑色仅为包裹名称输入框
验证 run/logs/latest.log 按 ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON|Mixin apply failed|InvalidInjectionException|IllegalClassLoadError|Timed out|timeout 扫描无命中
验证 scripts/verify-assets.ps1 成功，145 个 PNG 通过资源门禁
验证 scripts/test-assets-audit.ps1 成功，新增错误 advanced base/sprite 尺寸 fixture 均按预期失败
验证 scripts/verify-docs.ps1 成功
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，170 个 required GameTest 全部通过
验证 .\gradlew.bat build --stacktrace 成功
验证 scripts/verify-release.ps1 -RequireClientSmokeScreenshots 成功，232 个发布资源与 jar 同步、145 个 PNG 非空、10 张必需截图有效，日志无发布阻断关键字
GameTest：已考虑并运行。UI 调整本身不需要新增服务端测试；为确认现有高级样板编码、菜单和装配室行为未回归，复跑全部 170 个必需 GameTest。
未完成项：等待用户对本次高级终端视觉验收；IN-003 其余 UI/模型/动画范围仍等待后续描述，最终 dedicated server world-load 与 tag 门禁继续保留到完整范围冻结后执行。
```

最新进展：

```text
按用户最终视觉重做 AE2 原版 Pattern Encoding Terminal 的包裹样板模式：
  直接导入 E:\resources\textures\appliedpackaging\ret\pattern_mode_packaging.png 到 textures/gui/pattern_mode_packaging.png，没有缩放、重绘或清理像素；源/目标 SHA-256 均为 EC8BCE5C68A1DFB36ADB7A3BA2321600AF189089C87AF9B2CF41ED9D9EB2B9D7。
  对照 AE2 1.21.1 CraftingEncodingPanel 与 encoding/crafting.json，以 124x66 用户 atlas 替换 1.20.1 的 126x68 crafting panel；3x3 输入、marker 和自动输出分别使用高版本 left/bottom 坐标 15/158、106/155、106/140。
  主面板删除常驻名称框和整块 17 色色板，只保留 sprite 清空按钮、当前颜色设置按钮、3x3 输入、marker 与自动输出；包裹模式 tab 使用 sprite 中的 12x14 包裹图标。
  点击颜色设置按钮会打开覆盖同一模式面板的颜色/名称编辑层；打开期间 crafting grid、marker 和输出槽停用，点击、释放、拖拽、滚轮、按键和字符输入全部由编辑层拦截，点击外部只关闭弹层。
  client smoke 新增 appliedpackaging-client-smoke-ae2_pattern_encoding_terminal_settings.png，发布门禁从 10 张必需截图扩展到 11 张。
  主界面截图从物理坐标 (300,170) 读取 124x66 面板，排除两个动态按钮后与用户 atlas 为 0/8056 像素差异；示例颜色不同仅因为 smoke 固定使用 BLUE。
  第一次 client smoke 暴露 Mixin 内部 widget 直接加载导致 IllegalClassLoadError；将控件迁到正常 client.widget 包后复跑通过，没有保留失败结构。

验证 .\gradlew.bat compileJava --stacktrace 成功
验证 .\gradlew.bat runClientSmoke --stacktrace 成功；主界面与颜色/名称设置层截图均生成，人工和源 PNG 像素检查通过
验证 run/logs/latest.log 发布阻断关键字扫描无命中
验证 scripts/verify-assets.ps1 成功，146 个 PNG 通过资源门禁
验证 scripts/test-assets-audit.ps1 成功，错误 package pattern mode atlas 尺寸 fixture 按预期失败
验证 scripts/test-release-audit.ps1 成功，11 张截图正例及现有缺图/资源/元数据负例均按预期通过或失败
验证 scripts/verify-docs.ps1 成功
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，170 个 required GameTest 全部通过
验证 .\gradlew.bat build --stacktrace 成功
验证 scripts/verify-release.ps1 -RequireClientSmokeScreenshots 成功，233 个发布资源与 jar 同步、146 个 PNG 非空、11 张必需截图有效、145 个双语 key 对齐，日志无发布阻断关键字
GameTest：已考虑并运行。本轮只改变客户端面板、控件与事件拦截，不新增服务端行为测试；复跑全部 170 个必需 GameTest，确认 client-only Mixin 不污染服务端加载且既有包裹样板语义无回归。
未完成项：等待用户对原版终端包裹模式和高级终端视觉验收；IN-003 其余 UI/模型/动画范围仍等待后续描述。
```

最新进展：

```text
修正 Advanced Pattern Encoding Terminal 的拉伸、控件样式、列编辑和编码语义：
  动态终端背景改为 header、首行、重复中间行、末行和固定底部五段组合；重复段不再截取包含下边框的第二行。额外合成六行网络库存预览并人工检查，行间无重复边框或断层。
  右侧 blank/encoded pattern 槽与 Encode 按钮统一右移 1px，对齐用户 atlas 中的绘制位置；主产物叠加层、滚动 indicator、样板槽背景和 Encode 按钮改用当前 AE2 states atlas，左侧工具栏仍保留 1.20.1 AE2 控件。
  将当前 AE2 states.png 复制为 advanced_pattern_encoding_terminal_states.png；本地 AE2 1.21.1 参考与官方当前源码资源 SHA-256 均为 0996B0084C7BF37F65A97A745982AB681EBD86F142FADE526F14C823C4727E55。
  每列编辑区只保留颜色按钮和 X。颜色弹层只修改颜色，不再编辑名称；marker 固定为处理样板主产物，名称固定为空。
  X 在列有物品时只清空该列；空列再次点击时删除并左移后续列；最后一列始终保留。新增 GameTest 覆盖清空后删除与后续列左移。
  每列输入容量改为 AE2 默认处理样板上限 81，最多 17 列，共 1377 个配置槽。独立 advanced_processing_pattern 使用自定义 IPatternDetails 解码，避免 AE2 AEProcessingPattern 的 81 个总输入上限截断跨列输入，同时保持 Pattern Provider 与 Crafting CPU 的标准 IPatternDetails 路径。
  EncodePattern 从扩展输入状态和 4 个处理输出编码独立高级样板物品，并在下方 encoded pattern 槽显示；编码测试覆盖第 81 索引的第二列输入、颜色、空名称、主产物 marker 及未启用列残留忽略。
  首次 GameTest 复跑暴露旧测试仍把第二列输入写在索引 4；迁移到真实列起点 81 后全部通过，没有放宽断言。
  首次 client smoke 暴露颜色弹层前后 flush 导致黑色批次块；移除多余 flush 后复跑，主界面与 17 色颜色弹层截图均正常。

验证 .\gradlew.bat compileJava --stacktrace 成功
验证 .\gradlew.bat runGameTestServer --stacktrace 成功，171 个 required GameTest 全部通过
验证 .\gradlew.bat runClientSmoke --stacktrace 成功；人工检查高级终端主界面、颜色弹层和六行网络库存合成图，确认拉伸、槽位、按钮、滚动 indicator 与高级样板输出正确
验证 run/logs/latest.log 发布阻断关键字扫描无命中
验证 scripts/verify-assets.ps1 成功，147 个 PNG 通过资源门禁
验证 scripts/test-assets-audit.ps1 成功，新增错误 states atlas 尺寸 fixture 按预期失败
验证 scripts/test-release-audit.ps1 成功
验证 .\gradlew.bat build --stacktrace 成功
验证 scripts/verify-release.ps1 -RequireClientSmokeScreenshots 成功，234 个发布资源与 jar 同步、147 个 PNG 非空、11 张必需截图有效、144 个双语 key 对齐，日志无发布阻断关键字
GameTest：已考虑、扩展并运行。本轮改变高级样板容量、解码、编码和列删除语义，新增列清空/删除/左移测试并扩展真实编码与持久化覆盖；全部 171 个必需 GameTest 通过。
未完成项：等待用户对本轮高级终端视觉与交互验收；IN-003 其余 UI/模型/动画范围仍等待后续描述，最终 dedicated server world-load 与 tag 门禁保留到完整范围冻结后执行。
```

最新进展：

```text
继续按用户截图修正 Advanced Pattern Encoding Terminal：
  两行 base 的 y=17 首行与 y=35 末行都不能整段作为动态中间行。新增 195x18 middle-row strip：取末行顶部 17px 保留中间行上边语义，再接首行最后 1px 去掉末行封底；六行合成图确认四个重复段连续。
  BLANK_PATTERN 与 ENCODED_PATTERN 的 AppEngSlot 旧 icon 在客户端置空，只保留 copied current-AE2 states atlas 的单层背景图标，消除 1.20.1 与新版 ghost 叠加。
  父类网络库存 Scrollbar 通过 client accessor 换成 current-AE2 12x15 big scroller；首轮 24x15 atlas 被 1.20.1 Blitter 按 256 图集采样而出现黑块，改为 256x256 兼容 atlas 后复跑消失。
  右上角 crafting status 隐藏旧 CORNER TabButton 视觉，保留其点击和任务计数逻辑，前景改绘新版 20x20 BOX 背景与新版 hammer icon。
  覆盖 1.20.1 白色 slot hover，按新版 AE2 绘制淡蓝填充和亮青边线；高级终端页面整体右下移动 1px，底图、slot、widget、文本与交互命中区保持同一坐标系。
  client smoke 将主产物改为真实包裹，处理输出与下方 encoded pattern 预览中的包裹均位于槽中心。

验证 .\gradlew.bat compileJava --stacktrace 成功
验证 .\gradlew.bat runClientSmoke --stacktrace 成功；第二轮截图确认网络滚动条黑块消失、样板 ghost 只绘制一层、新版 crafting status 按钮生效、真实包裹输出与样板预览居中
验证六行动态背景合成图，首行、四个 middle strip、末行和固定 bottom 连续无断层
验证 scripts/verify-assets.ps1 成功，149 个 PNG 通过资源门禁
验证 scripts/test-assets-audit.ps1 成功，新增错误 middle-row 与 scrollbar atlas 尺寸 fixture 均按预期失败
验证 scripts/verify-docs.ps1 成功
验证 .\gradlew.bat build --stacktrace 成功
验证 scripts/verify-release.ps1 -RequireClientSmokeScreenshots 成功，236 个发布资源与 jar 同步、149 个 PNG 非空、11 张必需截图有效、144 个双语 key 对齐，日志无发布阻断关键字
GameTest：已考虑。本轮仅改变客户端 Screen 绘制、client-only accessor、smoke 取样和 GUI 资源，不改变菜单事务、样板编码或服务端行为，因此不新增或复跑 GameTest；最新行为基线仍为 171 个必需 GameTest 全部通过。
```

最新进展：

```text
统一所有包裹颜色拾取控件：
  新增共享 PackageColorPicker，弹窗不含标题、名称或 marker；Fluix 默认色单独置于最左，其余 16 个染料色在右侧按 8x2 排列。
  AE2 原版样板终端包裹模式将颜色和名称拆成独立按钮；颜色按钮只打开共享拾色弹窗，名称按钮只打开单行名称输入层，既有包裹名称能力保持不变。
  Advanced Pattern Terminal、ME Packager、ME Package Assembler、Package Pattern Terminal 和三种 Package Bus 全部迁移到共享弹窗；Package Pattern Terminal 的 9 个逐槽颜色按钮也改为直接打开同一弹窗，右键继续快速清除该槽颜色。
  颜色格不再注册为 17 个普通 widget，而由弹窗单次绘制；弹窗在父 Screen 的 slot/tooltip 后渲染，打开时取消底层 tooltip 并吞掉点击、释放、拖拽、滚轮、按键和字符输入，消除 tooltip 重复、底层点击穿透和按钮消失。
  删除旧 PackageColorButton 与旧式常驻 17 色条；新增双语 Fluix 与选色控件文本。

验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runClientSmoke 成功；人工检查 ae2_pattern_encoding_terminal_settings 与 advanced_pattern_encoding_terminal_editor 截图，确认两个终端弹窗布局一致、无标题、Fluix 位于最左、其余颜色为两行且底层按钮仍可见
验证 .\gradlew.bat runGameTestServer 成功，171 个 required GameTest 全部通过
验证 .\gradlew.bat build 成功
验证 scripts/verify-docs.ps1、scripts/verify-assets.ps1 与 scripts/verify-release.ps1 -RequireClientSmokeScreenshots 成功；236 个发布资源与 jar 同步、149 个 PNG 非空、11 张必需截图有效、146 个双语 key 对齐，客户端日志无发布阻断关键字
GameTest：已考虑并运行。本轮唯一服务端改动是 Package Pattern Terminal 菜单新增“输入槽 + 颜色”按钮编号映射，复用既有 setInputSlotColor 语义；不改变编码、事务或物品移动规则。完整 171 个必需 GameTest 全部通过。
```

最新进展：

```text
补齐高级样板终端此前遗漏的三项验收：
  保持整页 background、slot、widget、文本和交互命中区右下各偏移 1px；处理主产物与 encoded pattern 预览的真实包裹继续按槽中心渲染。
  1.20.1 AE2 的 renderCustomSlotHighlight 注入时机位于 Vanilla 槽位批次内，直接追加新版线框会污染延迟 atlas blit。改为旧注入只取消白色 hover，在 tooltip 前提交背景、绘制 AE2 1.21.1 的 0x669cd3ff 填充与 0xffdaffff 边线，再提交高亮批次。
  ClientSmokeRunner 原先只调用 GLFW 设置无焦点窗口光标，MouseHandler 坐标仍停在窗口中心，导致截图没有真实 hover。smoke 现在同步调用 Vanilla onMove，并持续悬停第一列空输入槽，截图直接显示新版 hover 且不被 tooltip 遮挡。
  中间诊断分别复现了槽位批次内绘制和只提交高亮前批次造成的黑色矩形；最终使用高亮前后成对批次边界后，空槽与有 tooltip 槽均不再污染背景。

验证 .\gradlew.bat runClientSmoke 成功；人工检查 advanced_pattern_encoding_terminal 主截图，确认新版 slot hover、两个包裹槽居中、整页右下偏移且无黑块
GameTest：已考虑。本轮仅修改高级终端客户端 hover 绘制顺序与 client smoke 鼠标定位，不改变菜单、样板编码、事务或服务端行为，因此不新增或复跑 GameTest；最新完整行为基线仍为 171 个必需 GameTest 全部通过。
```

最新进展：

```text
纠正两个样板终端的需求归属并补齐数量编辑：
  确认此前将整页右下 1px、blank/encoded pattern 与 Encode 额外右移归到 Advanced Pattern Terminal 是错误实现；撤回这些高级终端偏移，只保留新版 AE2 slot hover。
  AE2 原版 Pattern Encoding Terminal 包裹模式的 124x66 背景、3x3 输入、输出、marker、清空/颜色/名称按钮及命中区统一右下移动 1px；普通 crafting 模式继续使用 AE2 15.4.10 原坐标。
  PackagePattern tab 的 12x14 sprite 改为在 22x22 tab 内按 x+5、y+4 居中。
  包裹模式 crafting fake slot 显示 GenericStack 数量并允许中键打开 AE2 SetProcessingPatternAmountScreen；退出包裹模式恢复隐藏数量和普通 crafting 不可编辑语义。
  Advanced Pattern Terminal 输入槽增加同款中键数量编辑子页面，确认后通过 InventoryAction.SET_FILTER 回写对应槽。
  ClientSmokeRunner 为原版包裹模式预置 Oak Log x4、钻石 marker、BLUE 和名称，截图可直接检查数量、输出与 marker 对齐。

验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，172 个 required GameTest 全部通过；新增测试覆盖包裹模式 x4、数量显示、中键编辑资格和退出模式恢复，高级编码测试覆盖高级输入中键编辑资格
验证 .\gradlew.bat runClientSmoke 成功；人工检查 ae2_pattern_encoding_terminal 主截图确认 Oak Log x4 可见、marker/输出落入各自槽框、tab 图标居中，并确认 advanced_pattern_encoding_terminal 保持原页面原点与新版 hover
```

最新进展：

```text
按用户复测截图重新推导原版样板终端包裹模式坐标。此前把系统坐标错误当成 1px 微调是误判：124x66 贴图中 marker 是标准 18x18 框，16px slot 原点应为面板内 `(95,7)`；输出是 24x24 框，16px slot 居中原点应为 `(98,31)`，旧实现的输出 Y 仅为 26。PackagePattern 图标也不应裁成 12x14 手摆，现恢复 sprite `(32,0,16,16)` 完整单元并复用 AE2 水平 tab 的 `x+1,y+3` 偏移。没有移动输入格、配置按钮或高级终端。

验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runClientSmoke 成功；人工检查 ae2_pattern_encoding_terminal 主截图确认三处独立偏移生效且其他布局未变化
GameTest：已考虑。本轮只改变客户端槽位与图标绘制坐标，不改变菜单、数量编辑、编码或服务端语义，因此沿用上一轮 172 个 required GameTest 全通过的行为基线。
```

最新进展：

```text
修正统一拾色弹窗的物品隐藏与层级策略：
  原版样板终端包裹模式不再因颜色/名称弹层打开而停用 crafting grid、marker 和输出 slot。
  Advanced Pattern Terminal 不再因列颜色弹窗打开而停用可见 processing input/output slots。
  PackageColorPicker 在父 Screen 完整绘制 slot、物品和 widget 后，以 z=500 独立 pose 绘制，并在 push/pop 两侧 flush；底层物品继续存在，只在重叠区域被不透明弹窗正常遮挡。
  既有 modal 输入拦截与底层 tooltip 取消保持不变，避免点击穿透和重复 tooltip。

验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runClientSmoke 成功；人工检查 ae2_pattern_encoding_terminal_settings，Oak Log x4 保持可见且 marker/输出仍在弹窗下渲染；检查 advanced_pattern_encoding_terminal_editor，处理输入物品保持在底层且不穿透弹窗
GameTest：已考虑。本轮仅修改客户端 slot 可见性与 picker 绘制层级，不改变菜单、编码或服务端语义，因此沿用 172 个 required GameTest 全通过的行为基线。
```

最新进展：

```text
接入用户更新的 pattern_mode_packaging.png：
  源文件与项目资源 SHA-256 均为 AB254596C0AADE263DFB5816ED4824186BCDE69DCAA8B24CF3C00BF3B7EA6256。
  新贴图在面板左侧增加滚动条轨道，输入窗口首槽移动到面板内 (16,7)；marker 保持 (95,7)，输出保持 (98,31)。
  原版 Pattern Encoding Terminal 包裹模式改为复用 AE processing panel 的 81 个 processing input fake slots 与 SMALL scrollbar，显示 3x3 窗口并滚动 27 行；processing output 隐藏，marker 与包裹预览继续使用专属槽和 crafting result。
  包裹样板输入 NBT 容量从 9 扩展为 81，旧版稀疏 NBT 继续兼容；中键数量编辑改为作用于 processing input。
  统一拾色弹窗最终使用 z=400、父界面尾部统一批次提交；原版终端只取消底层 tooltip。截图确认原版终端无黑块，高级终端物品不穿透弹窗。

验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runGameTestServer 成功，172 个 required GameTest 全部通过；真实编码覆盖第 81 个输入槽与数量
验证 .\gradlew.bat runClientSmoke 成功；人工检查原版终端主界面、设置弹窗与高级终端编辑弹窗截图
```

最新进展：

```text
对照 AE2 原版处理样板交互并补齐包裹模式 tooltip：
  空包裹预览槽新增与 AE2 primary processing result 相同的 18x18 ITooltip 区域，复用原版标题/说明 key 和提示颜色；有预览物品时仍优先显示物品 tooltip。
  包裹模式输入改由 PatternEncodingTermMenu.isProcessingPatternSlot 识别，不再只特判 canModifyAmountForSlot；因此空槽的物品/流体容器写入和非空槽的中键数量编辑都直接走 AE2 原路径。
  PackageCraftingPatternDataStorage 与预览编码取消 AEItemKey-only 限制，允许 AEFluidKey 等 AE processing GenericStack；GameTest 增加 1000 mB water 编码与模式退出分类恢复断言。
  原版终端拾色弹窗改在 ScreenEvent.Render.Post 中绘制，前后提交 item buffer；弹窗使用 Vanilla tooltip 同款 z=400 + GuiGraphics.drawManaged 整批绘制，避免 marker 穿透和高 Z 黑块。
  ClientSmokeRunner 在最终 Render.Post flush 后截图，并新增 primary_output_tooltip 截图。

验证 .\gradlew.bat compileJava 成功
验证 .\gradlew.bat runClientSmoke 成功；人工检查设置弹窗无黑块且遮挡重叠 marker，主产物 tooltip 标题和说明完整，高级终端弹窗无回归
验证 .\gradlew.bat runGameTestServer 成功，172 个 required GameTest 全部通过
验证 .\gradlew.bat build 成功
验证 scripts/verify-docs.ps1、scripts/verify-assets.ps1 与 scripts/verify-release.ps1 -RequireClientSmokeScreenshots 成功；236 个发布资源与 jar 同步、149 个 PNG 非空、11 张必需截图有效、146 个双语 key 对齐
验证 client latest.log 扫描 ERROR、Exception、missing texture、Missing model、Failed to read Screen JSON 无命中；git diff --check 无空白错误
GameTest：已扩展 patternEncodingTerminalPackageInputsSupportAmountEditing，覆盖 processing slot 分类、AEFluidKey 编码、第 81 个输入及退出模式恢复
```

### 2026-07-11 机器单槽与有序输出重构

包裹自定义名称能力已整体移除：机器、菜单、样板 NBT、高级样板列元数据、终端按钮和语言 key 均不再读写名称；旧存档中的名称字段仅被忽略。ME Packager 改用单一真实 `heldBox`，由持久化状态区分待拆输入与打包输出。拆包输入先保留到进度结束，最终事务提交失败时保持输入并进入红色阻塞状态，后续定时重新验证，玩家可取回；打包输出不会进入拆包路径。

ME Package Assembler 改为一个真实主输出和一个只读下一包预览，其余高级样板产物保存在严格有序队列中。GUI、玩家、Forge capability 与自动导出每次只取队首一个包裹，自动导出允许同 tick 循环但不跳序。旧版 17 输出槽存档在加载时按原槽位顺序迁移到队列。两台机器的 ScreenStyle 与槽位坐标已按用户更新的 256x256 atlas 重新测量，进度条改用 atlas 右侧 sprite；本次按用户要求不运行 client screenshot smoke，视觉验收由用户执行。

验证 `./gradlew.bat compileJava` 成功；验证 `./gradlew.bat runGameTestServer` 成功，173 个 required GameTest 全部通过；验证 `scripts/verify-assets.ps1`、`scripts/verify-docs.ps1` 与 `git diff --check` 成功。

### 2026-07-11 原版样板终端客户端 Mixin 启动修复

修复 `PatternEncodingTermScreenMixin` 中 `modePanels` 目标字段同时标注 `@Unique` 与 `@Shadow` 导致的客户端启动崩溃。该字段继续以 `@Shadow @Final` 映射 AE2 原字段；全项目扫描未发现其他相同的冲突注解组合。

验证 `.\gradlew.bat compileJava` 成功，Mixin annotation processor 与 refmap 正常生成；验证冲突注解静态扫描无命中，目标文件 `git diff --check` 无空白错误。GameTest 已考虑，但该修复仅影响客户端 Screen Mixin 类加载，GameTest 不加载该 client mixin，故未运行；按当前 UI 验收约束未运行客户端截图冒烟。

### 2026-07-12 打包机 heldBox 与原版终端包裹模式修复

ME Packager 的 heldBox 菜单包装器改为 `IItemHandlerModifiable`，修复 Forge `SlotItemHandler#set` 强转导致的普通 GUI 点击崩溃；客户端菜单同步可写入显示镜像，服务端放入仍先模拟完整拆包事务并只接受一个包裹。左侧红石模式直接控制实际打包逻辑，不再受未显示的红石卡门槛影响；默认仍为高电平，避免拆包完成后立即回流重打包，打包机不再接受无作用的红石卡升级。

AE2 原版样板终端包裹模式统一使用“面板原点 + 贴图内坐标”定位，修复清空/颜色按钮少加面板原点而与第三列输入重叠的问题。菜单在服务端和客户端都明确启用 81 个滚动输入、专属 marker 与唯一自动计算的 crafting result，同时禁用全部 processing output 配置槽；marker 变化会刷新自动输出预览。

验证 `.\gradlew.bat compileJava` 成功。首次 `runGameTestServer` 暴露“无红石卡默认持续工作”会导致拆包后立即重打包，因此未采用该行为；修正为所选模式直接生效后，`.\gradlew.bat runGameTestServer` 成功，175 个 required GameTest 全部通过。新增测试覆盖 heldBox 普通 GUI `safeInsert`、红石设置无隐藏升级门槛、包裹模式 marker/81 输入/唯一自动输出与 processing output 禁用。按用户要求未运行客户端截图冒烟，视觉坐标由用户验收。

### 2026-07-12 原版终端包裹模式新版布局修正

对照 AE2 1.21.1 `ProcessingEncodingPanel` 与 processing ScreenStyle，撤销包裹面板混入的 1.20.1 `(x+9,bottom-164)` 基准，统一改为新版 `(x+8,bottom-165)`；输入首槽改为 screen `(24,bottom-158)`，滚动条改为 `(15,bottom-158)` 并使用项目中已标记来源的新版 small scroller sprite。清空与颜色按钮改到面板内 `(72,7)`、`(82,7)`，在 3x3 输入区右侧保留 2px 边距且相对复测截图整体右移 1px。

包裹模式下停用的 crafting grid、不可见 processing inputs 与全部 processing outputs 现在同时移出渲染区域，避免 Vanilla 容器层继续绘制其 ghost item；marker 与唯一自动包裹预览仍使用专属槽。移除把自动包裹预览错误标记为 processing primary output 的自定义 tooltip，并删除对应的 client smoke tooltip 截图分支。

验证 `.\gradlew.bat compileJava --stacktrace` 成功，Mixin refmap 正常生成；验证 `scripts/verify-docs.ps1` 与 `git diff --check` 成功。该轮只改客户端布局、槽位绘制隔离与错误 tooltip，不改变菜单/编码/服务端行为，因此未重复运行 GameTest；按用户要求未运行客户端截图冒烟，视觉结果由用户验收。

用户复测发现上一轮仍未生效：`AEBaseScreen.render()` 会先执行 Screen 的 `updateBeforeRender()`，再执行 `widgets.updateBeforeRender()`，因此 Screen mixin 写入的新版输入坐标和隐藏输出位置随后被 AE2 15.4.10 `ProcessingEncodingPanel` 恢复为旧 processing 布局，表现为输入进一步右下偏移、processing output ghost 重新覆盖 marker 区。修正后，包裹模式在 `ProcessingEncodingPanel.updateBeforeRender()` 入口直接取消原 processing 更新，只执行包裹模式自己的 81 格输入滚动布局，并在同一最终阶段把全部 processing outputs 移出渲染区；普通 processing 模式保持原逻辑。重新执行 `.\gradlew.bat compileJava` 成功，Mixin refmap 正常生成；仍按用户要求不执行客户端截图冒烟。

### 2026-07-12 Package Bus 最终 GUI 与 AE2 part 迁移

按 IN-005 取消 Package Export Bus 与独立 Package Pattern Terminal 的玩家入口，删除对应物品注册、配方、loot 和客户端 smoke 步骤；兼容方块 id 暂保留但不再提供菜单。Package Storage Bus 和 Package Unpacking Bus 改为 AE2 cable `PartItem`，暂分别复用 AE2 Storage Bus/P2P 模型并显式注册 PartModels，修复首次客户端 smoke 的未注册模型崩溃。

共享 Package Bus 菜单改为五行过滤，每行提供动态模糊/反转、颜色、marker 和 6 个物品 ghost；默认两行、最多三张容量卡逐行解锁，未解锁行由 `OptionalFakeSlot` 半透明显示。卸货总线支持四张加速卡；存储总线隐藏右上工作区，卸货总线在主界面渲染结束后绘制只读工作槽外观和 15 级进度条，避开 AE2 15.x `drawBG` 中追加 atlas 导致的渲染状态污染。工作包裹物品预览网络同步未接入，保留为后续细化项。

验证过程中先后修复旧 loot table 引用已删除物品、P2P/Storage Bus PartModels 未注册、`UpgradeableMenu` 强制读取未支持红石设置、ScreenStyle 缺失 `openPriority` 锚点、未解锁颜色按钮仍可见及工作区渲染黑屏。`compileJava`、`runData`、167 个必需 GameTest、release audit 自测与多轮真实 `runClientSmoke` 均通过；最终两个总线截图已人工检查。GameTest 世界与 client smoke 世界的旧 registry 映射只存在于生成目录，清理后复验未再作为当前注册事实。

补充修正两个包裹总线的优先级入口：对照 AE2 `neoforge/v19.2.17` 的 Storage Bus/Formation Plane，把右上标签恢复为 `(152,-5,20,20)`，并把新版 `states.png` `(144,64,16,16)` 的专用 priority glyph 补入项目 sprite 空闲单元 `(48,16,16,16)`，替代 AE2 15.4.10 旧扳手。卸货总线工作槽和进度条左移到 `(119,8)` 与 `(139,8)`；存储总线仍不绘制工作区。新版入口继续打开 AE2 Priority 子菜单，服务端优先级由既有 `IPriorityHost` 保存和应用。

客户端 smoke 揭示 AE2 15.4.10 在总线 widget 批次结束后追加 fill 会刷新旧渲染状态并使卸货主面板变黑；透明的第二 atlas priority glyph 会更容易暴露该问题。最终实现保留 sprite 中与 AE2 19.2.17 完全一致的规范图标，用纯色水平 run 重现同一 16x16 像素，并把工作槽/进度条移入既有 `drawBG` 背景批次，不再在 `super.render()` 后追加 fill。最终 `runClientSmoke` 截图要求同时确认正确 priority 网络分叉图标、左移工作区和无黑屏。

最终验证：`.\gradlew.bat build --stacktrace`、`.\gradlew.bat runClientSmoke --stacktrace`、`scripts/verify-assets.ps1`、`scripts/verify-docs.ps1` 与 `scripts/test-assets-audit.ps1` 均成功；人工检查存储/卸货总线截图，确认两者右上专用 priority 标签、卸货工作区避让及完整背景。GameTest 已考虑；本次只改变客户端 widget、绘制坐标和 GUI sprite，不改变既有 `IPriorityHost` 保存、优先级应用或总线事务，故未新增或复跑 GameTest。client smoke 同时仍记录到 Package Assembler 预览 item handler 的既有 `ClassCastException`；该装配室槽问题不在本轮总线 GUI 范围内，尚未修复，因此本轮不宣称客户端日志或发布候选门禁完全干净。

根据复核意见移除 Package Bus 跨 atlas 临时方案：将用户 `sprite.png` 以 `(176,64)` 偏移烘入 `package-storagebus.png`，并把 AE2 `neoforge/v19.2.17` 完整 `states.png` 同字节复制为 `ae2-states.png`。新版 `SLOT_BACKGROUND`、`PRIORITY`、`TAB_BUTTON_BACKGROUND` 与 focus 状态逐像素烘入主 atlas，来源、SHA-256 和矩形映射记录于 `LICENSE.md` 与 `META-INF/licenses/ae2-states-source.txt`。客户端删除字符 glyph、纯色工作槽及独立 Package Bus sprite atlas 依赖；背景、过滤按钮、工作区和 Priority 标签现只绑定 `package-storagebus.png`。

首次单 atlas client smoke 仍在卸货总线复现黑屏，排除了“跨 atlas 是唯一根因”：AE2 15.4.10 `Blitter` 在 `super.drawBG()` 后追加提交同一 atlas 子图也会污染/提前刷新背景批次。过滤按钮的同 atlas `GuiGraphics.blit` 路径正常，因此工作槽和进度框改用直接 blit；Priority normal/focus/glyph 保留在正常 widget 批次内使用同 atlas Blitter。

第二次单 atlas smoke 证明 `super.drawBG()` 后的直接纹理 blit 同样会触发卸货主面板黑屏，根因进一步收敛为旧版背景批次结束后追加纹理提交，而非 `Blitter` 类本身。最终把新版工作槽与空进度框直接烘入可见背景 `(119,8)` / `(139,8)`；存储总线用背景色覆盖，卸货总线仅用纯色 fill 更新动态进度，不再在背景回调内追加任何纹理采样。

第三次 smoke 在工作区完全静态烘焙后仍只让卸货界面变黑，而存储界面的遮罩 fill 正常，最终确认 AE2 15.4.10 `super.drawBG()` 返回时 ScreenStyle 背景仍未提交：存储遮罩会隐式 flush，卸货在零进度时不会。`PackageBusScreen.drawBG` 现于 `super.drawBG()` 后立即调用 `GuiGraphics.flush()`，固定在主 atlas 仍为当前纹理时提交背景，再进入自定义绘制。

最终复验又发现仅在入口 flush 仍不足：存储总线最后一批纯色遮罩会等到 AE2 widget pass 切换着色器后才提交，导致整块主体变黑；在自定义背景绘制结束处再次 `flush()` 后，两种总线均正常。最终 `runClientSmoke --stacktrace` 成功并人工检查两张 960x540 截图，无黑屏，过滤区、新版槽位、右上 Priority 标签及卸货工作区均正常。随后 `build --stacktrace`、`verify-assets.ps1`、`verify-docs.ps1` 与 `git diff --check` 成功；`test-assets-audit.ps1` 的单独复跑在 120 秒工具期限内未返回结果，但本轮较早执行已通过，最终变更仅为 Java 批次提交和本段日志。GameTest 未复跑，因为最终改动仍纯属客户端渲染，不改变菜单或总线行为。

### 2026-07-12 Package Bus 高版本源码复核与错误合图方案撤销

用户指出不应修改已提供材质后，重新以官方 AE2 当前 main 提交 `45f315517ea346efc0babd02c85c6b9d32dc8acf` 为准读取 `AEBaseScreen`、`Blitter`、`StorageBusScreen`、`IconButton`、`VerticalButtonBar`、`UpgradesPanel` 与对应 ScreenStyle/纹理。源码确认高版本没有在加载阶段把 `states.png` 动态烘进界面背景：`states.png` 仍是独立纹理，`Blitter` 为每个元素提交包含独立 `TextureSetup` 和 ARGB 的 render state；禁用可选槽直接以 `SLOT_BACKGROUND` 的 `0.2` alpha 绘制。此前 3024-3032 的“单 atlas 是目标方案”结论作废，相关试错仅保留为历史记录。

恢复 `E:/resources/textures/appliedpackaging/ret/package-storagebus.png` 与 `sprite.png` 的原始字节，仓库副本 SHA-256 分别为 `7253977C9792F7BB86D1B826688DD067AF5F242E3279A71E7409442428B53EB5`、`14D7D26A93BF46D1BA0EF33A5408197718D0AF5BD3ADE662AA8A46E8DE662281`；不再合图、重绘或向其烘入 AE2 像素。新增独立的 current-AE2 `states.png`、`extra_panels.png`、`vertical_buttons_bg.png` 原样副本及 LGPL 来源记录，并在资产审计中锁定五张文件的 SHA-256。

客户端按源码逐项回移：Priority 标签 `(152,-5,20,20)`、toolbar 6px 间距与 nine-slice 外框、按钮 normal/hover/focus 背景及新版状态图标、5px padding 的新版升级面板、Storage Bus 的连接目标提示，以及禁用过滤行 0.2 alpha。1.20.1 没有当前 `GuiGraphicsExtractor.nextStratum()`/每元素 render state，因此使用立即式 `Blitter`，并在 ScreenStyle 背景和自定义层边界显式 flush、复位颜色与默认混合状态；这是版本适配层，不是合图替代。工作槽和进度框从用户背景右侧原有素材区独立取样，存储总线不绘制该层。

最终客户端 smoke 在两个 Package Bus part 前放置真实 Chest，截图确认新版 `Attached to: Chest`、toolbar 图标/外框、Priority 标签、三行启用/两行 0.2 alpha 禁用、存储总线无工作区，以及拆包总线左移工作区与 8+1 升级槽分栏。`.\gradlew.bat build --stacktrace`、`.\gradlew.bat runGameTestServer --stacktrace`、`.\gradlew.bat runClientSmoke --stacktrace`、`scripts/verify-assets.ps1`、`scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 均成功；167 个 required GameTest 全部通过。资产自测新增并通过“替换用户 Package Bus 原图必须由 SHA-256 门禁拒绝”的负例。工作包裹物品预览的数据源仍未接入，本轮截图按空只读槽验收，不改写此前的未完成状态。

### 2026-07-13 Package Unpacking Bus held 工作态与装配室输出槽修复

按“拆包总线工作原理与 ME 打包机拆包模式完全相同”修正 Package Unpacking Bus：总线先模拟相邻目标，从 ME 网络精确抽取一个匹配包裹并保存为 part 的真实 `heldPackage`，随后运行固定 20 tick 工作进度；进度完成前不向目标写入任何内容。最终 tick 重新校验当前过滤、目标存在性和完整累计容量，成功才事务性提交；失败时保留同一个 held 包裹、本地阻塞并定时重试，不提前消耗、不回写后换包。加速卡沿用 ME Packager 的 `max(2, 20 - speedCards * 3)` 扫描/重试间隔，不缩短工作动画。

held 包裹、working、剩余 tick、blocked 和 retry cooldown 均写入 Part NBT；菜单工作槽绑定真实 `IItemHandlerModifiable` held 包装器并同步 working/blocked 标志，工作中不可取，阻塞/空闲时可取回，阻塞视觉复用 ME Packager 的半透明红底和红框。Part 拆除通过 AE2 `addAdditionalDrops` 返还 held 包裹，`clearContent` 同步清空。Package Assembler 的有序主输出包装器也改为 `IItemHandlerModifiable` 并映射 `setStackInSlot`，修复 `SlotItemHandler#set` 客户端同步时的 `ClassCastException`。

新增 6 项 GameTest：装配室输出槽 `SlotItemHandler#set` 同步、事务预留与最终提交分离、最终目标变化时 held 包裹不丢失、真实 AE2 Part 在进度前不提交、真实 Part 阻塞后保留并重试同包，以及 held 包裹 NBT/拆除掉落/`clearContent` 防复制。第一次完整 GameTest 的两项 Part 场景失败是测试夹具仅放侧面 Part、没有中心玻璃线缆，导致 Part 未加入供电网格；补齐中心线缆后产品逻辑无需改动。持久性测试首次以脱离 host 的裸 `createPart()` 读 NBT，又触发 AE2 `UpgradeablePart` 要求合法 host 的生命周期约束；改为真实安装 Part 后写入、清空、读取和取 drops。最终 `.\gradlew.bat runGameTestServer` 成功，173 个 required GameTest 全部通过。`.\gradlew.bat compileJava`、`.\gradlew.bat runClientSmoke` 与 `.\gradlew.bat build` 成功；装配室、存储总线和卸货总线截图全部生成，client smoke 时的 `run/logs/latest.log` 扫描 `ClassCastException|IItemHandlerModifiable|ERROR|Exception` 无命中。`scripts/verify-docs.ps1`、`scripts/verify-assets.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。

### 2026-07-13 Package Bus 四项小布局修正

用户截图指出五行过滤整体错从第三行开始、小按钮贴行顶边、右侧升级面板低 1px，以及卸货总线升级槽数量与底部孤立块错误。根因分别为 marker/content 与客户端动态内容槽共同使用了第三行原点 `ROW_Y=65`、8px 按钮复用槽位顶边、style 中 upgrades `top=0`，以及错误地把各兼容卡上限相加后令 `PackageUnpackingBusPart#getUpgradeSlots()` 返回 9。对照 AE2 current-main `IOBusPart#getUpgradeSlots()` 后确认总线升级库存本身固定为 5 格，卡种上限只约束同一库存中的各卡数量。

实现将五行原点统一改为 `y=29`，内容槽动态定位、marker ScreenStyle 和禁用槽背景同步对齐；按钮使用 `(18-8)/2=5px` 上边距，并让绘制、widget、点击命中和 tooltip 共用同一行坐标。升级面板改为 `top=-1`，两个 part 共用 `UPGRADE_SLOT_COUNT=5`；卸货总线的 4 张加速卡兼容上限保持不变。新增真实 Cable Bus GameTest 同时断言 Storage/Unpacking 两个 part 均只有 5 个升级槽。

`.\gradlew.bat compileJava --stacktrace` 成功；`.\gradlew.bat runGameTestServer --stacktrace` 成功，174 个 required GameTest 全部通过。首次 client smoke 被 IDE 中既有调试客户端持有的 `New World/session.lock` 阻止，外层等待超时；只终止本轮残留进程，不终止用户 IDE 进程，复制排除 `session.lock` 的隔离世界后复跑成功，生成两张新总线截图并自动退出，随后删除临时世界。截图确认默认行从最上方开始、按钮垂直居中、升级面板上移、两种总线均为 5 格且无底部孤立块。双图同批查看曾让卸货截图显示大面积黑块，直接读取源 PNG 的 ARGB 像素并单图重新解码后确认只是查看器伪影，没有修改渲染代码或材质。

最终 `.\gradlew.bat build --stacktrace`、`scripts/verify-docs.ps1`、`scripts/verify-assets.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。两张用户 Package Bus PNG 的 SHA-256 与资产门禁值一致，本轮只修改代码、ScreenStyle 和文档。

### 2026-07-13 Package Bus 容量卡上限与高级终端像素复核

Package Storage Bus 与 Package Unpacking Bus 的容量卡兼容上限由 3 提升为 5，仍只使用各自固定的 5 格共享升级库存。过滤行规则不变：默认 2 行，前三张容量卡各解锁一行，第四、第五张只允许安装而不继续扩展超过 5 行。扩展 `packageBusPartsUseFiveSharedUpgradeSlots`，在真实 Cable Bus host 上对两个 part 分别验证 5 张均可装入、安装计数为 5、过滤行封顶 5 且第 6 张被拒绝。`.\gradlew.bat compileJava --stacktrace` 成功；`.\gradlew.bat runGameTestServer --stacktrace` 成功，174 个 required GameTest 全部通过。

高级样板终端编码按钮由 `x=166` 调整到 `x=167`；自定义新版按钮按 AE2 `IconButton` 语义在 widget `x-1` 绘制 18px 背景，因此最终背景从 `x=166` 开始，图标和点击区域从 `x=167` 开始。同步修改 Java 常量与 ScreenStyle anchor。保留用户正在运行的 IDE 客户端，使用排除 `session.lock` 的 `AP Smoke Isolated` 世界执行 client smoke；11 张截图全部捕获并自动退出，人工检查高级终端截图确认右移生效，随后安全删除隔离世界。

本轮没有修改 `adv-pattern-terminal-base.png` 或仓库运行时副本，两者长度均为 1791 字节且 SHA-256 同为 `660EF8C5379F1131E4D3D773FD43EE9954DE1F0FCE278DF78C30F75D9B5563F6`。与 AE2 current-main 逐像素比较后确认：用户图顶部 52 行和官方 terminal 背景一致，不透明调色板也一致；截图中看似偏色的禁用列来自 `AdvancedPatternEncodingTermScreen.DISABLED_SLOT_BODY = 0xff969cb1`，不是 PNG 颜色；右侧空白/已编码样板槽框的背景平移为 `(+20,-2)`，对应逻辑槽平移为 `(+19,-1)`，所以物品相对槽框仍左 1px、下 1px。另有透明像素 RGB 为透明黑、而 AE2 current-main 主要为透明白的差异，正常 nearest-neighbor GUI 绘制不可见，仅在线性过滤时可能形成暗边。本轮按用户要求只移动编码按钮，以上两项未擅自修正。

### 2026-07-13 Package Bus 七行与外围源码/像素复核

用户指出 Package Bus 实际应为 7 行而非 5 行。`AbstractPackageBusPart.FILTER_ROWS` 改为 7，基础 2 行加 5 张容量卡逐行解锁到 7；marker、6 个内容槽、行状态、颜色按钮、禁用背景、NBT 数组和菜单同步均由该常量扩展。既有真实 Cable Bus GameTest 增加逐卡断言，两个 part 均验证行数 3、4、5、6、7 依次增长，第 6 张容量卡仍拒绝。`.\gradlew.bat compileJava --stacktrace` 与 `.\gradlew.bat runGameTestServer --stacktrace` 成功，174 个 required GameTest 全部通过。

重新读取 AE2 current-main 源码后确认上一轮把升级面板从 `top=0` 改成 `top=-1` 与当前源码相反，本轮恢复 `top=0`。Package Bus 左侧手工回移工具栏此前遗漏原 Storage Bus 的 Help，现补入 Help 后保持 current-main 的 7 个按钮顺序与 6px 间距。自定义 `ModernPackageBusUpgradesPanel` 的 `extra_panels.png`、5px padding 和 slot 坐标本来已与 current-main 一致；真正的槽材质错误来自旧依赖 `RestrictedInputSlot` 自动携带 AE2 15 灰阶 `BACKGROUND_UPGRADE`，现清除旧图标并从原样 `ae2-states.png` 绘制 current-main `(240,208,16,16)` 空升级槽占位。

像素对比发现用户 `package-storagebus.png` 的外围确实画错，而不是渲染器统一偏色。以中心 `x=7..168,y=28..154` 为自定义区排除后，10246 个官方 `#CBCCD4` 主体像素被替换成 `#ADB0C4` 槽内色，854 个官方 `#413F54` 最外圈像素被替换成 `#CBCCD4`；其余外围主调色和用户 sprite `(0,64,18,18)` slot background 正确。原图与运行时副本仍保持相同 SHA-256，本轮不代替用户修图。

client smoke 在卸货总线中增加无 marker/带 marker 包裹相邻对照，并在 held 工作槽显示同一带 marker 包裹。`.\gradlew.bat runClientSmoke --stacktrace` 成功，存储与卸货截图均显示完整 7 行、新 Help、`top=0` 升级面板和新版空槽占位。三种包裹绘制的槽内包围盒在 GUI scale 2 下完全相同，均为 `(9..22,11..23)`；marker 只改变 3 个像素，因此未发现 PackageItemRenderer 额外 offset。实际装入的四类升级卡仍由 AE2 15.4.10 物品贴图渲染，与 current-main 文件确实不同，本轮未做全局 AE2 资源覆盖。

最终 `.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 均通过；5 个资产合同、237 个 jar 发布资源、154 张 PNG、151 个双语语言 key 和客户端日志审计全部通过。

### 2026-07-13 高级终端右侧样板槽对齐纠正

用户澄清截图圈选的是上下两个样板槽，不是编码按钮；上一轮操作对象判断错误。`BLANK_PATTERN` 由 `(166,166)` 改为 `(167,167)`，`ENCODED_PATTERN` 由 `(166,119)` 改为 `(167,120)`，两槽内容各向右、向上 1px，与底图槽框使用相同的 current-main 平移。编码按钮不再继续移动，保留 `left=167,bottom=146`；其 16px 图标与点击区和两槽内容区共享 `x=167`，18px 背景从 `x=166` 开始，与 18px 槽框同轴。重新执行隔离世界 client smoke 成功，11 张截图全部生成并自动退出；人工检查高级终端截图确认三者中心线一致。随后 `build`、文档审计、资产审计、带资产合同的发布审计与 `git diff --check` 全部通过；本次是纯客户端 ScreenStyle 坐标修正，不改变菜单或服务端行为，故未重复运行 GameTest。仍未修改用户 base PNG，其源文件与运行时副本 SHA-256 都是 `660EF8C5379F1131E4D3D773FD43EE9954DE1F0FCE278DF78C30F75D9B5563F6`。

### 2026-07-13 Package Bus 用户更新底图接入

用户更新 `E:/resources/textures/appliedpackaging/ret/package-storagebus.png` 后，将该 256x256 RGBA 文件原字节同步到运行时资源，并把资产哈希门禁及来源记录更新为 `506BE44EF826C14C1DBE37C076EDC7955C0DBFE35A7DB9B157EABA8E241787DE`。旧图到新图的 11262 个差异全部位于主界面 `176x253`：10408 个 `#ADB0C4` 改为 `#CBCCD4`，854 个 `#CBCCD4` 改为 `#413F54`。排除七行自定义中心后与 AE2 current-main 外围可见像素差异为 0；右侧工作槽、进度素材与旧图相同，sprite 和三张 AE2 独立资源未改。Asset contract 与资源审计通过。

首次 client smoke 错误指定了已删除的 `AP Smoke Isolated`，资源加载后因 quick-play 目标不存在而等待至工具超时；清理本轮残留后确认默认 `New World` 未锁定，改用 `.\gradlew.bat runClientSmoke --stacktrace` 在 40 秒内成功，11 张截图全部刷新。存储/卸货两张图单独解码均正常，确认新版外围色、七行过滤、Priority、升级区和卸货工作区无回归；多图预览里的卸货黑块仍是查看器伪影。`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1` 负例套件、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 均成功；源图、运行时资源与 JAR 条目哈希一致。GameTest 已考虑且未复跑，因为此次没有行为或服务端变更。

### 2026-07-13 Marker 空槽、hover 与机器升级区统一

用户澄清圈选的 marker 空槽图标是自绘资源，不是 AE2 sprite。新增共享 `ModernSlotRendering`，从字节保持的 `package-storagebus-sprites.png` `(32,16,16,16)` 取样，在 Package Storage/Unpacking Bus 七行 marker 过滤槽、ME Packager、ME Package Assembler 与原版 Pattern Encoding Terminal 包裹模式中绘制；可交互空槽提供双语双行说明 tooltip，锁定总线行图标使用与槽背景相同的 `0.2` alpha。源 `E:/resources/textures/appliedpackaging/ret/sprite.png` 与运行时副本 SHA-256 仍同为 `14D7D26A93BF46D1BA0EF33A5408197718D0AF5BD3ADE662AA8A46E8DE662281`，未修改任何用户 PNG，也未把该区域列为 AE2 派生材质。

Package Bus 错位 hover 的根因是 tooltip 阶段已退出 GUI 局部坐标系，却直接使用 `hoveredSlot.x/y`；现统一加上 `leftPos/topPos` 后绘制 current-main 蓝色填充与浅色边线。ME Packager 与 Package Assembler 从旧 AE2 15 `UpgradeableScreen` 切换到共享 `ModernUpgradeableScreen`，使用相同 current-main hover、自定义 `ModernUpgradesPanel`、新版工具箱区域和 `ae2-states.png` 空升级槽占位；原 `ModernPackageBusUpgradesPanel` 同步泛化重命名。AE2 Pattern Encoding Terminal 的包裹 marker 槽也在 mixin 中获得同一用户图标、current-main hover 和 tooltip。

`.\gradlew.bat compileJava processResources --stacktrace` 成功。首次默认 client smoke 因用户客户端占用 `New World/session.lock` 无法 quick-play；未触碰用户进程，只终止本轮残留 wrapper/client，然后复制仓库未占用测试世界为 `AP Smoke GUI`。`.\gradlew.bat runClientSmoke '-Pappliedpackaging.clientSmoke.world=AP Smoke GUI' --stacktrace` 在 41 秒内成功，11 张截图全部生成并自动退出。逐张检查两台机器、原版样板终端、存储总线和卸货总线，marker 图标/tooltip、新版 hover 与升级空槽均正常；bus hover 落在第二行 marker 槽，单独解码卸货 PNG 无黑块。GameTest 已考虑但未重复运行：本轮没有菜单、服务端、事务、过滤、网络或升级兼容行为变更，既有 174 个 required GameTest 继续覆盖行为路径。

最终 `.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、`scripts/verify-docs.ps1` 与 `scripts/verify-release.ps1 -RequireAssetContracts` 全部通过；JAR 内 237 个 Applied Packaging 发布资源与源码一致，5 个资产合同、154 张 PNG、153 个双语语言 key/占位符均通过审计。

### 2026-07-13 新版容量/样板空槽图标与 Bus 2px 上边距

按用户复测修正两项纯客户端视觉差异。共享 `ModernSlotRendering` 新增 current-main `ae2-states.png` 精确切片：ME Packager 和 ME Package Assembler 的容量元件空槽使用 `(240,48,16,16)`，装配室已编码样板空槽使用 `(240,112,16,16)`；两台 Screen 不再调用依赖 AE2 15.4.10 `Icon` 坐标的旧空槽绘制。Package Bus 的模糊、反转、颜色三个 8px 按钮由 5px 垂直居中改为每个 18px 行固定 `2px` 上边距，绘制、点击命中及 hover 共用同一 `rowButtonY()`。用户 PNG 与原样 current-main `ae2-states.png` 均未改动，后者 SHA-256 仍为 `0996B0084C7BF37F65A97A745982AB681EBD86F142FADE526F14C823C4727E55`。

`.\gradlew.bat compileJava processResources --stacktrace` 成功。为避开用户可能仍在使用的世界，从仓库测试世界复制临时 `AP Smoke Slot Icons`，执行 `.\gradlew.bat runClientSmoke '-Pappliedpackaging.clientSmoke.world=AP Smoke Slot Icons' --stacktrace` 在 41.5 秒内成功，11 张截图全部生成并自动退出，随后验证路径位于 `run/saves` 后删除隔离世界。人工检查两台机器截图，容量元件和已编码样板新版空槽完整显示；两种总线截图确认按钮逻辑上边距为 2px，现有 marker tooltip、hover、七行过滤和升级面板无回归。卸货截图在四图同批查看时出现黑块伪影，单图读取正常。

GameTest 已考虑但未重复运行：本轮不改变菜单、网络、存储、过滤、升级兼容或服务端事务，既有 174 个 required GameTest 已覆盖对应行为。最终 `.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过；237 个 JAR 资源、5 个资产合同、154 张 RGBA PNG 与 153 个双语语言 key/占位符均通过审计。

### 2026-07-13 全仓死代码、临时兼容与事务审查

本轮按“只清理项目自身无意义实现，不追逐 Forge/Minecraft 1.20.1 依赖 API 警告”的边界完成全仓审查。删除三个旧 Package Bus 方块/方块实体实现、独立 Package Pattern Terminal 的方块/方块实体/part/menu/screen、对应注册、语言、模型、贴图和失效 GameTest；Package Storage Bus 与 Package Unpacking Bus 只保留真实 AE2 part，包裹样板编辑只保留 AE2 Pattern Encoding Terminal 包裹模式。另删除 8 张从未被当前模型引用、仅被旧资产清单强制保留的机器方块面贴图，以及两个 part 物品遗留的 block loot table。GameTest 文件同时清理大段注释代码与只覆盖被删方块壳的测试。

事务审查补齐 Forge fluid handler 多项插入失败时的逆序回滚，并让 item/fluid/MEStorage 回滚不完整时留下错误证据；包裹内容归并、容量累计、过滤需求归并和整叠手动拆包增加 `long` 溢出保护。新增真实 Storage Bus part 挂载、流体第二项提交失败回滚、归并溢出拒绝和整叠拆包乘法溢出不消耗测试。发布审计新增旧 block/block entity 注册、part 遗留 block loot 和 11 张 client smoke 截图门禁，相关负例自测均通过。

无法由代码审查单独决定的事项记录在 `docs/09-code-review-audit.md`：Package Assembler 隐藏 legacy 输入路径、ME Packager 旧槽/枚举、其它存档迁移、容量升级与需求范围冲突、通用第三方 handler 的强原子性边界，以及仍在使用的临时 Create Packager 外壳。未据依赖 deprecation/removal 编译警告修改生命周期、资源定位或渲染 API。

验证结果：`.\gradlew.bat build --stacktrace` 与 `runData --stacktrace` 成功；`runGameTestServer --stacktrace` 的 144 个 required GameTest 全部通过；`runClientSmoke --stacktrace` 在历史开发世界完成一次被删 block ID 清理后复跑成功，11 张必需截图全部刷新且第二次日志无 missing registry/model/texture 或客户端类加载阻断项；dedicated server smoke 成功到达完整 world-load。`verify-docs.ps1`、`test-docs-audit.ps1`、`verify-assets.ps1`、`test-assets-audit.ps1`、`verify-release.ps1 -RequireAssetContracts -RequireClientSmokeScreenshots -RequireServerWorldLoad`、`test-release-audit.ps1`、`test-release-self-tests.ps1` 与 `git diff --check` 全部通过；最终 JAR 中 215 个 Applied Packaging 发布资源与源码一致、79 个资源 JSON 可解析、140 张 PNG 通过审计，且不包含被删实现的 class/resource 条目。

### 2026-07-13 发布前旧存档与旧样板兼容直接删除

用户明确项目尚未正式发布，不需要开发版存档迁移或旧载体兼容，也不设置保留期限。基于该决定继续删除上一轮审查中暂缓的自有兼容：移除 `packaged_processing_pattern` 注册/物品/模型/语言与 `PackagePatternDataStorage`、`ColoredProcessingPatternDataStorage`、`PackagedProcessingPatternDataStorage`；不再把 AE2 blank/crafting pattern 或带 colored/packaged 扩展 NBT 的 processing pattern 当作包裹载体。正式样板语义只保留独立 `package_pattern`、普通 AE2 processing pattern 与独立 `advanced_processing_pattern`。

Package Assembler 删除 9 格隐藏输入、旧槽位布局迁移、旧 `auto_export` NBT、旧多样板计划和外部输入 capability，内部机器库存压缩为当前 20 槽；外部 item capability 收敛为一个严格有序输出槽，并修复主输出取空后没有从后续内部输出槽提升下一包裹的问题。ME Packager 删除旧 output/filter 槽、旧 held-box 推断和 `DISABLED` / `CYCLIC` 旧红石枚举。PackageEntity 删除旧 `Package` NBT key；高级终端删除旧 AE2 输入迁移。对应旧兼容 GameTest 被删除，发布审计增加旧注册、旧存储类和机器兼容符号不得重新出现的门禁及负例。

第一次 GameTest 与 client smoke 读取历史开发世界时按预期报告已删除 `appliedpackaging:packaged_processing_pattern` registry ID；没有添加 missing-mapping remap，Forge 备份并移除旧 ID。清理后的同一世界再次运行不再出现该缺失映射。最终 `.\gradlew.bat compileJava --rerun-tasks`、`build --stacktrace`、`runData --stacktrace` 成功；`runGameTestServer --stacktrace` 连续两次均为 118/118 required tests 通过；`runClientSmoke --stacktrace` 连续两次完成 11 张截图；dedicated server smoke 到达 world-load。`verify-assets.ps1`、`test-assets-audit.ps1`、`verify-docs.ps1`、`verify-release.ps1 -RequireServerWorldLoad`、`test-release-audit.ps1`、`test-release-self-tests.ps1` 与 `git diff --check` 均通过。编译仍有 25 条 Forge/Minecraft 1.20.1 removal 警告，按用户边界不处理。

最终差异复核发现 Package Assembler 的颜色按钮与 marker 槽在旧彩色/封装载体删除后已没有任何计划路径读取，属于能设置却不产生效果的假功能。同步删除方块实体槽位/字段/NBT、菜单 action/GuiSync、screen 拾色器/marker 绘制、ScreenStyle 槽和专用语言；内部 ItemStackHandler 进一步压缩为 19 槽，输出槽校验同时收紧为必须带有效 PackageData。普通 AE2 processing pattern 测试不再先写入无效机器配置，仍直接验证固定 Fluix、空 marker 语义。发布审计把 `selected_color`、`SLOT_MARKER` 和 `setSelectedColor` 纳入装配室兼容/冗余回归门禁。再次执行 `compileJava` 成功、118/118 GameTest 通过、client smoke 11 张截图完成；人工检查装配室截图，无槽位重叠、黑块或失效控件残留。

仍未由本轮擅自决定的问题继续记录在 `docs/09-code-review-audit.md`：ME Packager 容量升级范围冲突、第三方通用 handler 只能提供 best-effort rollback 的原子性边界，以及临时 Create Packager 外壳尚未替换为正式 AE2 风格模型。

### 2026-07-13 ME Packager 容量升级与 Pattern Provider 式插入

用户确认 ME Packager 容量升级进入正式范围，并明确相邻物品库存不需要自定义“事务”抽象，应采用样板供应器相同的先 simulate、后实际推送逻辑。需求现统一为基础 1k/16 类型，容量槽支持 AE2 16k/64k/256k storage component；4k 与附属大容量兼容继续排除。三档容量元件映射均纳入 GameTest。

运行时 item handler 代码收敛为 `PackageContentsInserter`：先使用累计 `SimulatedItemHandler` 防止多种内容重复占用同一空槽，同时查询真实 handler 的 simulate 结果；全量通过后才按内容调用 `ItemHandlerHelper.insertItemStacked` 提交。删除原 `ItemPackageTransactions` 的逐槽计划、提交记录与反向抽取回滚，卸货总线操作改名为 `PackageUnpackingOperations` 并只保留预留、最终复验和插入。全仓引用检查确认旧 item handler 打包规划、Forge fluid handler 适配、Package Export/即时拆包 API 都只有测试调用，因此连同 `ItemPackagePlan`、`SlotExtraction`、三个 fluid helper 和对应 18 个无效 GameTest 一并删除；GenericStack/AEFluidKey 与 AE2 MEStorage 流体路径不受影响。发布审计新增这些无用源文件不得重新出现的门禁。

GameTest 已按行为变更要求执行：`.\gradlew.bat runGameTestServer --stacktrace` 成功，100/100 required tests 全部通过，保留覆盖整包累计模拟拒绝、held 包裹目标变化重试、真实 Package Unpacking Bus part 和 MEStorage 操作。`.\gradlew.bat build --stacktrace`、`scripts/verify-docs.ps1`、`scripts/test-release-audit.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 均通过。发布 JAR 与 214 个资源一致，78 个 JSON、140 张 PNG、5 个资产合同和 140 个双语 key/占位符通过审计。既有 25 条目标平台 removal 警告未处理。

本轮同时解决审查文档中的容量范围冲突和第三方 handler 强原子性伪问题；`docs/09-code-review-audit.md` 当前只剩 ME Packager 仍使用临时 Create Packager 外壳这一项正式发布前问题。

### 2026-07-14 高级样板终端新版 AE2 控件与包裹预览校正

按用户截图复核高级样板终端四项视觉偏差。左侧工具栏原因是终端仍继承 AE2 15.4.10 `VerticalButtonBar` 的 4px 间距和旧 `IconButton` 16x16 背景；新增仅对 `AdvancedPatternEncodingTermScreen` 生效的客户端适配，使用 current-main 18x20 normal/hover/focus 按钮背景、1px hover 位移、6px 间距及三段式 `vertical_buttons_bg` 外框，不全局覆盖 AE2 15 其它界面。网络库存滚动条 atlas 的既有 UV 保持不变，启用/禁用图块改为官方 current-main `big_scroller.png` 与 `big_scroller_disabled.png` 的 12x15 原样像素，并补充 LGPL 来源记录。

右侧 `BLANK_PATTERN` 和 `ENCODED_PATTERN` 的 bottom anchor 分别由 167/120 改为 166/119，两个槽内物品统一向下 1px。包裹基础与 marker 模型的 GUI transform 从 Y 旋转 225度、缩放 0.5 改为 Y 旋转 135度、缩放 0.6，使正面朝左且预览更大，两种包裹外观保持一致。

`\.\gradlew.bat compileJava processResources --stacktrace` 成功；首次编译发现项目无 JetBrains `Nullable` 依赖后移除非必要注解，复跑成功。`scripts/verify-assets.ps1` 通过，且逐像素比较确认 atlas 两个 12x15 图块与新版 AE2 来源完全一致。`\.\gradlew.bat runClientSmoke --stacktrace` 在 57 秒内成功，11 张必需截图全部刷新；人工检查高级终端截图，新版按钮/外框、滚动条、两个样板槽和包裹预览均已生效，最新日志扫描无 Mixin apply、缺失模型/贴图、`ERROR` 或异常命中。最终 `\.\gradlew.bat build --stacktrace`、`scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1` 与 `git diff --check` 全部通过。GameTest 已考虑但未复跑：本轮只改变客户端 widget、ScreenStyle 坐标、GUI sprite 与物品模型显示变换，不改变菜单、网络、存储或服务端行为。

### 2026-07-14 高级终端工具栏共享实现纠正

用户复测指出高级终端工具栏与主面板之间留有过大空档，并明确要求必须先阅读高版本 AE2 源码、复用其它 GUI 的已有逻辑且不必使用 Mixin。直接比较 `ae2-latest` 与 `forge/v15.4.10` 的 `AEBaseScreen`、`VerticalButtonBar`、`IconButton` 及 `screens/common/common.json` 后确认：高版本公共工具栏锚点已从旧版 `left=-2,top=6` 改为 `left=3,top=1`；上一轮只回移了 18x20 按钮、6px 布局和外框，却仍让高级终端继承旧 ScreenStyle 锚点，因而产生水平 5px 和垂直 5px 的整组偏移。

新增共享 `ModernVerticalToolbar`，逐行对应高版本源码的锚点、margin、可见按钮布局、nine-slice `vertical_buttons_bg` 和 normal/focus/hover 背景；Package Bus 删除本地重复实现并调用该共享类，高级终端复用 AE2 原生按钮的点击、tooltip、图标与 item overlay，只由共享类接管最终布局及新版背景。删除上一轮新增的 `IconButtonMixin` 和 `VerticalButtonBarMixin` 及其配置项，高级终端 ScreenStyle 显式改为 `verticalToolbar left=3,top=1`。

`\.\gradlew.bat compileJava processResources --stacktrace` 成功；`\.\gradlew.bat runClientSmoke --stacktrace` 成功，11 张必需截图全部刷新。人工对比高级终端与 Package Storage Bus 截图，两者的工具栏右边缘均与主面板直接衔接，共享外框、按钮背景及布局一致，旧锚点造成的 5px 空档消失。卸货总线批量图片预览一度显示黑块，单图重新解码正常，确认仍是查看器伪影。最终 `\.\gradlew.bat build --stacktrace`、`scripts/verify-docs.ps1`、客户端错误日志扫描和 `git diff --check` 全部通过。GameTest 已考虑但未执行：本轮仅改变客户端工具栏布局和绘制，不改变菜单、按钮 action、网络或服务端行为。

### 2026-07-14 高级终端新版按钮图标与包裹预览二次放大

按用户复测继续补齐共享工具栏：上一轮只替换了 current-main 的 18x20 normal/focus/hover 背景，原生 `IconButton` 仍先绘制 AE2 15.4.10 的旧图标。重新读取高版本 `IconButton` 后，将图标/item overlay 的绘制顺序、hover 下移 1px 和不透明背景覆盖顺序一并回移。按钮对象、动态状态、点击和 tooltip 仍使用 AE2 原对象；仅用 Forge Access Transformer 暴露其既有 `getIcon()` / `getItemOverlay()`，通过启动时一次性绑定的 `MethodHandle` 读取，不增加 `IconButton` / `VerticalButtonBar` Mixin，也不维护第二套按钮状态表。Package Bus 本地按钮继续调用同一 `ModernVerticalToolbar` 渲染入口。

最初尝试把只读桥放进 `appeng.client.gui.widgets` 包，客户端被 Java 模块系统以 split-package `ResolutionException` 正确拒绝；该文件已删除，最终实现不向 AE2 模块包写入类。旧开发客户端随后正常保存退出，以释放 ModDevGradle 重新生成 AT 处理产物所需的 Forge JAR 文件锁。包裹基础模型与 marker 模型的 GUI scale 由 0.6 继续提高到 0.75，Y 旋转保持 135 度，截图中的两个包裹预览均放大且未越出槽框。

`\.\gradlew.bat compileJava processResources --stacktrace`、`\.\gradlew.bat runClientSmoke --stacktrace` 与 `\.\gradlew.bat build --stacktrace` 均成功；11 张必需截图全部刷新并自动退出。人工检查高级终端默认/编辑器截图，左侧按钮背景与图标均来自 current-main `ae2-states.png`，按钮组与主面板无空档，两个 0.75 缩放包裹均保持正面朝左；Package Storage Bus 截图确认共享工具栏无回归。`scripts/verify-assets.ps1`、`scripts/verify-docs.ps1`、两份模型 JSON 解析、JAR 内 AT 配置检查、客户端异常日志扫描与 `git diff --check` 均通过。GameTest 已考虑但未执行：本轮仍只改变客户端表现和物品 GUI display，不改变菜单、按钮 action、网络、存储或服务端行为。

### 2026-07-15 ME Package Assembler 与机器工具栏统一

用户截图指出 ME Package Assembler 左侧输出模式按钮仍是 AE2 15.4.10 的旧 16x16 外观。根因是机器共用 `ModernUpgradeableScreen` 此前只回移了 current-main 槽位 hover、升级面板和空升级槽，没有接入已经用于高级终端和 Package Bus 的 `ModernVerticalToolbar`；因此装配室的 `OutputModeToolbarButton` 虽然使用 AE2 原生状态/点击/tooltip，仍由旧 `VerticalButtonBar/IconButton` 完成布局与绘制。

本轮在 `ModernUpgradeableScreen` 的统一生命周期中捕获全尺寸原生 `IconButton`，追加共享 current-main render overlay，并在背景阶段调用同一 `layout()` 与 `drawPanel()`。ME Package Assembler 和 ME Packager 现在共同复用 `ModernVerticalToolbar` 的 `left=3,top=1`、2px margin、6px 间距、三段式 `vertical_buttons_bg`、18x20 normal/focus/hover 背景、hover 下移 1px及 current-main `ae2-states.png` 图标；没有新增 Mixin，也没有为装配室复制按钮状态逻辑。首次编译发现 ME Packager 既有 `init()` 为 `protected`，将共用基类 override 从 `public` 恢复为同等权限后复编译成功。

检测到用户从 IntelliJ 启动的开发客户端仍在运行，未关闭或修改该进程；从 `New World` 复制排除 `session.lock` 的临时 `AP Smoke Toolbar 20260715` 世界，执行 `\.\gradlew.bat runClientSmoke '-Pappliedpackaging.clientSmoke.world=AP Smoke Toolbar 20260715' --stacktrace` 成功，11 张截图全部刷新并自动退出，随后校验目标路径位于 `run/saves` 且无 smoke Java 进程占用后删除临时世界。人工检查装配室截图，左侧按钮现显示新版外框、背景和白色状态图标并紧贴主面板；ME Packager 截图确认其 6 个按钮也使用同一共享实现。`\.\gradlew.bat compileJava processResources --stacktrace` 与 `\.\gradlew.bat build --stacktrace` 成功。GameTest 已考虑且发现现有 `runGameTestServer` 路径，但本轮未运行或新增 GameTest：改动只涉及客户端 Screen 初始化、按钮布局与绘制，不改变菜单 action、网络、存储、机器或服务端行为。

### 2026-07-15 ME Packager 正式模型、滚动传送带与帘子动画

将用户提供的 `model.bbmodel` 拆为 6-cube 静态主体、1-cube 动态传送带和复用四次的单条帘子 partial；静止 item model 保留 11 个 cube。`base.png`、`curtain.png`、`belt_scroll.png` 原字节复制并加入 SHA-256 门禁。32x32 belt 使用两个横向 16px 周期；运行时 UV 窗口规范为上表面 15px + 工作口正面 1px，BER 以 1px/tick 修改 atlas U offset。四条帘子在 x=3..4 内缩位置绕各自顶部转轴摆动，拆包向内、打包向外；包裹按原 20 tick 事务时序沿传送带移动并继续使用 1x1x1 stencil 裁切。最终方向规范为 `facing` 控制水平工作口方向，`network_side` 只控制 AE 接线面。

删除旧 `me_packager_create` 模型/贴图、Create hatch/tray additional model 和对应 renderer 分支；机器行为与事务提交未改变，只新增客户端可读的归一化动画进度。GameTest 已按行为敏感边界执行：`\.\gradlew.bat runGameTestServer --stacktrace` 的 100/100 required tests 全部通过。`assetgen validate-contract`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1`、`runData`、客户端 smoke、`build` 与 `scripts/verify-release.ps1 -RequireAssetContracts` 均通过；15px+1px UV 后刷新 11 张截图，无紫黑 missing model，三张用户 PNG 的运行时 SHA-256 与来源一致。

### 2026-07-15 ME Packager 朝向与方块物品显示修正

用户实机截图确认首次正式模型接入错误地使用 `network_side` 旋转整台机器：放在地面时 placement 产生 `network_side=down`，导致底盘被转成立墙、背板被转成顶板；传送带、帘子和包裹也跟随了错误轴。修正后 blockstate 及 BER 全部只读取水平 `facing`；源模型本地 +X 是工作口，north/east/south/west 分别使用 Y 旋转 270/0/90/180 度。`network_side` 继续支持六向 AE 接线，但不再改变机器姿态。

同一截图还确认完整 11-cube item model 没有继承原版方块 `display`，第一人称呈现为贴脸的大平面。生成器现为 item model 增加 `minecraft:block/block` 父模型，恢复标准方块的 GUI、ground、fixed、第三人称和第一人称显示变换。`verify-assets.ps1` 新增四个 facing 在全部六种 network_side 下旋转不变、以及 item parent 的回归门禁。

验证：`\.\gradlew.bat compileJava --stacktrace` 成功；`\.\gradlew.bat runClientSmoke --stacktrace` 成功并刷新 11 张截图。人工检查 `facing=north,network_side=south` 场景，底盘保持水平、背板保持竖直，工作口与动画部件朝 north、AE 接线位于 south。GameTest 已考虑且发现现有 `runGameTestServer` 路径；本次仅修复客户端模型朝向、blockstate 资源和 item display，不改变 placement 状态、AE 接线、菜单、存储、网络事务或服务端行为，因此未新增或重复运行 GameTest。

### 2026-07-15 Package Unpacking Bus 路由、阻挡与 Package Storage Bus 分区修正

用户明确拆包总线应更接近 Formation Plane/Pattern Provider，而不是 Storage Bus。对照项目锁定的 AE2 `forge/v15.4.10` 本地源码后，将 Package Unpacking Bus 从“周期扫描 ME 存储并抽取包裹”改为优先级 1 的只写入 `IStorageProvider`：挂载的 `MEStorage` 只实现 `insert`，每次最多接收一个网络路由的合法包裹，且只在过滤、整包累计目标模拟和可选阻挡条件全部通过时接受；held 工作包裹不枚举、不允许网络抽取，也不再需要 Drive/Cell 作为扫描源。Package Storage Bus 默认优先级保持 0，因此新进入网络的包裹优先进入可工作的拆包端点。

Package Unpacking Bus 左侧工具栏收敛为 Pattern Provider 指南、清空过滤与 `Settings.BLOCKING_MODE` 三项。阻挡模式复用 Pattern Provider 语义：把包裹内容 item key 的 `dropSecondary` 集合作为 pattern inputs，只要相邻目标已有任一输入类型，就在网络接收和最终提交两个阶段拒绝；最终条件变化仍保留原 held 包裹并按既有 20 tick 工作周期重试。删除只服务于旧扫描路径的 `reserveOnePackage` 辅助逻辑和对应测试调用，没有保留旧行为兼容分支。

两种总线的颜色过滤补齐可见空模式：颜色按钮在未启用时绘制无色标记，总线专用拾色弹窗在 Fluix 左侧提供“任意颜色”，右键仍可清除；空模式不限制包裹颜色。Package Storage Bus 的 Partition Storage 改为按相邻容器槽位顺序读取不同合法包裹，每个样本生成一个已启用过滤行，跳过散装物品与重复包裹，目标中没有合法包裹时清空过滤。每行 6 个内容槽无法完整表达含非物品 key 或超过 6 种物品内容的样本时只保留颜色/marker，避免生成一个反而拒绝原样本的不完整 allowlist。

首次 `\.\gradlew.bat compileJava --stacktrace` 正确失败于两项旧 GameTest 仍调用已删除的扫描预留 API；迁移断言后复编译成功。新增/扩展真实 AE 网格测试覆盖默认优先级、Formation Plane 式直接接收、held 不可抽取、Pattern Provider 阻挡、颜色空模式和容器多包裹分区；最终 `\.\gradlew.bat runGameTestServer --stacktrace` 成功，103/103 required tests 全部通过。

检测到用户开发客户端继续占用 `run/logs`，未关闭或修改该进程；`runData --stacktrace` 和 GameTest 均在日志轮转警告后正常完成。从未占用测试世界复制排除 `session.lock` 的临时 `AP Smoke Unpacking 20260715`，执行 `\.\gradlew.bat runClientSmoke '-Pappliedpackaging.clientSmoke.world=AP Smoke Unpacking 20260715' --stacktrace` 成功，11 张截图全部刷新并自动退出；检查 Storage/Unpacking Bus 截图确认前者保留存储工具栏，后者只显示帮助、清空过滤和阻挡模式三项，随后验证路径与进程并删除隔离世界。最终 `runData --stacktrace`、`build --stacktrace`、`scripts/verify-docs.ps1`、`scripts/verify-assets.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过；发布 JAR 的 199 个资源、73 个 JSON、130 张 PNG、5 个资产合同和 141 个双语 key/占位符均通过审计。
### 2026-07-15 Package Bus 默认优先级与同级路由最终修正

前一版把“拆包总线比存储总线优先”错误实现为卸货默认优先级 1、存储默认 0，随后又把用户对同级决胜的说明误读为存储优先。最终需求明确为：两个总线默认值都必须是 0，右上 Priority 子菜单中的数值完全由玩家控制；只有当数值相同时，Package Unpacking Bus 必须先于 Package Storage Bus，拆包端点拒绝后才回落到存储总线。

重新对照锁定的 AE2 15.4.10 `FormationPlanePart`：其字段默认值为 0，挂载时直接使用当前玩家优先级，没有默认加一或隐藏偏移。再对照 `NetworkStorage` 确认同一数值组会先尝试 `isPreferredStorageFor` 为真的端点。实现因此把 Package Unpacking Bus 的默认值恢复为 0，并让它的实际只写入 `packageInput` 对合法包裹返回 preferred；同时移除 `PackageItemStorage` 上误加的 preferred 覆盖。数值不同时继续完全服从玩家设置的较高值，同值时拆包稳定优先；若拆包因 held 忙碌、过滤、阻挡或目标容量拒绝，网络会继续尝试普通的包裹存储总线。

GameTest 改为在真实共享 cable grid 中先挂载 Package Storage Bus、后挂载 Package Unpacking Bus，确认两个新 part 都默认 0、第一包仍优先进入卸货 held；随后在卸货忙碌时插入第二包，确认它回落到存储端。另增真实网格测试把存储总线设为 1、卸货保持 0，确认同值决胜不会覆盖玩家设置的更高数值。`.\\gradlew.bat compileJava --stacktrace` 成功；`.\\gradlew.bat runGameTestServer --stacktrace` 成功，105/105 required tests 全部通过；日志开头因用户开发客户端占用 `run/logs` 出现既有轮转警告，但不影响测试。`.\\gradlew.bat build --stacktrace`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。此次没有改变 GUI、纹理或客户端交互，因此不重复 client screenshot smoke。

### 2026-07-15 ME Packager 四向轮廓与动态件朝向修正

ME Packager 不再继承完整 16x16x16 方块的默认选择框和碰撞体。四个水平朝向分别使用与正式模型一致的组合体：1px 底板、4px 后部模块、两条 2px 高侧框和 1px 传送带；帘子与运动中的包裹不参与碰撞。形状只读取 `facing`，切换六种 `network_side` 均不改变机体轮廓。

动态传送带、帘子和包裹此前直接复用了 blockstate JSON 的正角度，忽略 JSON baked-model 旋转与 `PoseStack Axis.YP` 的符号相反，导致南北朝向动态件落到静态主体另一端。本轮把 BER 的 north/east/south/west 等效角度定为 90/0/270/180 度；静态 blockstate 仍保持 270/0/90/180 度，最终工作口方向一致。

新增 GameTest 遍历 4 个 `facing` × 6 个 `network_side`，分别精确比较选择轮廓和碰撞体。`.\\gradlew.bat compileJava`、`.\\gradlew.bat build`、`scripts/verify-docs.ps1` 与 `scripts/verify-assets.ps1` 成功；`.\\gradlew.bat runGameTestServer` 成功，106/106 required tests 全部通过；`git diff --check` 无空白错误，仅输出工作区 LF/CRLF 提示。未启动第二个 client smoke：同一 `run` 目录已有用户从 IntelliJ 启动的调试客户端，进程占用 `run/logs`；本轮没有终止或覆盖该用户进程。

### 2026-07-15 ME Packager 输出位置、裁剪、连续传送带与背面接网

按用户实机反馈把打包输出的静止点从后部内部改到本地 `x=10/16,z=8/16`，即 4px 后部模块之外 12x16 前部区域的几何中心；拆包从该点向 `x=2.5/16` 内部移动，打包从内部向该点移动，动画结束不再发生位置跳变。包裹不再只在工作 tick 期间裁剪，静止 output 也经过 1x1x1 stencil immediate pass；四条帘子使用相同边界的独立 immediate pass，主体不透明背板继续依靠深度测试遮挡内部动态件。

传送带的 16px 相位从 renderer 的“按本轮进度从 0 计算”改为方块实体状态：工作期间按拆包 `+1px/tick`、打包 `-1px/tick` 累加，写入 `belt_scroll_pixels` NBT 并通过 AE2 block entity stream 同步。停止时保留当前相位，下一轮继续累加。AE 主节点连接面从单一 `network_side` 扩展为 `network_side` 与机器背面 `facing.getOpposite()` 的并集；地面放置默认仍选 bottom，但背面同时可接线，两个 ME 面都不暴露普通 item capability。

修改现有真实 AE2 Cable + Drive + Creative Energy Cell GameTest：机器朝 west、`network_side=down`、线缆只位于 east 背面，确认背面节点上线并完成打包；动画完成后断言相位为 `12/16`，再保存和读取方块实体确认相位未复位。`.\\gradlew.bat compileJava`、`.\\gradlew.bat build --stacktrace`、`.\\gradlew.bat runGameTestServer --stacktrace`、`scripts/verify-docs.ps1`、`scripts/verify-assets.ps1` 与 `git diff --check` 全部通过，106/106 required GameTest 成功。GameTest 启动日志因用户 IntelliJ 调试客户端占用 `run/logs` 出现既有日志轮转警告，但测试本身正常完成；未终止用户进程，也未并发启动 client smoke。

### 2026-07-15 ME Packager 包裹贴带与背板裁剪修正

用户实机截图确认输出包裹仍悬空，且包裹/帘子可能穿过最后 1px 背板。根因一是 BER 直接把 item 渲染原点放到固定 Y，忽略包裹模型最低点以及 `fixed`、BER 两层缩放；根因二是旧 stencil 使用未随机器旋转的完整世界方块盒，没有扣除背板体积。

修正后根据包裹模型 `y=1..9`、item `fixed scale=0.5` 与 BER `scale=1.49` 反算渲染原点，使最终几何底面严格等于传送带顶面 `y=2/16`。stencil 改为机器本地 `x=1/16..16/16` 的 15px 深盒，并通过统一的 `rotateMachineToFacing` 对齐 north/east/south/west；包裹和四条帘子共用该边界，最后 1px 背板不再属于动态件可见区域。

`.\\gradlew.bat compileJava --rerun-tasks`、`.\\gradlew.bat runClientSmoke --stacktrace` 与 `.\\gradlew.bat build --stacktrace` 成功，11 张截图全部刷新；日志未发现缺失模型/贴图、OpenGL/stencil、崩溃或超时错误。`scripts/verify-docs.ps1`、`scripts/verify-assets.ps1` 与 `git diff --check` 全部通过。GameTest 已考虑但未执行：本轮仅改变客户端 BER 变换和裁剪掩码，不改变服务端行为，真实客户端 smoke 是对应验证路径。

### 2026-07-15 所有包裹颜色入口统一与 sprite 更新

将 ME Packager、AE2 原版包裹样板模式、Advanced Pattern Terminal、Package Storage Bus 和 Package Unpacking Bus 全部收敛到 `PackageColorPicker.TriggerButton` 与单一 `openNear(..., allowNone, ...)` API，删除高级终端自有 `ColumnColorButton` 绘制。只有两种总线过滤行传入 `allowNone=true`，并由共享触发按钮处理右键清除；其它入口必须选择实际包裹颜色。

弹窗固定为 89x23，分隔线左侧 Fluix/None 在 `(3,3)`、`(3,12)` 竖排，右侧 16 色从 `(15,3)` 开始保持 8x2；None 不允许时只隐藏绘制和命中，不回收布局。用户截图的默认、None、选中效果按 6x 网格精确还原到 `package-storagebus-sprites.png` 三个原空白 8x8 单元 `(48,0)`、`(56,0)`、`(48,8)`，最终 hash 为 `632A686B6F8EC7B712326DC52E639CE43CF8E1B55C44D00309B62B672B766635`。逐像素对照原 atlas 确认三个单元内改变 192 像素、其它区域改变 0 像素。选中只改变格内背景，不画外 outline；popup 与触发按钮 hover 均不改变视觉。

补充处理用户指出的 tooltip 穿透：picker 打开期间暂停锚点 `TriggerButton` 自己的 tooltip，关闭后恢复其原 tooltip；父 Screen 既有底层 slot tooltip 拦截保持不变，弹窗内颜色名称提示保留。Client smoke 的 Package Storage Bus 步骤在截图前自动打开首行 picker，并把鼠标停在首行颜色按钮，实际截图没有“选择包裹颜色”提示穿透。

验证：`gradlew.bat compileJava --stacktrace`、`gradlew.bat build --stacktrace`、隔离世界 `runClientSmoke '-Pappliedpackaging.clientSmoke.world=AP Smoke Color Picker 20260715' --stacktrace`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部成功。11 张必需截图刷新；人工检查总线弹窗与两个终端编辑弹窗，确认 None 显隐不移动右侧颜色、选中/hover 无外框、没有 tooltip 穿透或黑块。GameTest 已考虑但未执行：修改范围完全位于客户端控件、渲染、提示拦截与资源，不改变服务端行为。

### 2026-07-15 删除自动客户端 smoke

按用户决定删除耗时且收益有限的自动客户端 smoke。移除 `ClientSmokeRunner`、Gradle `runClientSmoke` 运行配置、release runner 的 `-RunClientSmoke` / `-RequireClientSmokeScreenshots` 参数、固定截图存在性门禁和对应发布自测 fixture；客户端视觉变更改为按风险人工运行 `runClient` 并检查目标界面、动画与日志。行为验证继续由 GameTest 承担，dedicated server smoke 和最终 world-load 门禁保留。

删除 smoke 不改变总线服务端逻辑：Package Storage Bus 与 Package Unpacking Bus 默认优先级仍均为 0，同优先级时卸货总线先接收，玩家设置的数值优先级继续生效。合并前重新执行 build、106 项必需 GameTest、发布自测和 asset-contract release audit，全部通过。

### 2026-07-15 ME Packager 固定接线、标准扳手旋转与表面交互

按最终交互要求删除 ME Packager 的独立 `network_side` 方块状态、放置推导、自定义扳手识别和接线切换提示。方块现在实现 AE2 `IOrientableBlock` 并使用 `OrientationStrategies.horizontalFacing()`；AE2 全局 `WrenchHook` 只旋转水平 `facing`。主体、动态件、轮廓、模型背面接线面和右键命中区域都从同一个 `facing` 推导，底部接线固定不变。AE 主节点只在 `DOWN` 与 `facing.getOpposite()` 暴露，`getCableConnectionType` 在其它四面返回 `NONE`；底部和模型背面不暴露普通 item capability，其余四面保留包裹自动化 capability。

手动包裹交互改为模型局部坐标命中：只有传送带上表面 `x=1..16,y=2,z=2..14` 执行放入/取出，四个水平朝向先逆变换到源模型本地 +X 坐标再判断。右键机框、背板、侧面和底面等其它位置打开 GUI；传送带命中但没有可执行的包裹动作时不打开 GUI。blockstate 与确定性导入脚本收敛为四个 `facing` 变体，资源审计同步拒绝多余变体。

新增或改写 GameTest 覆盖 AE2 扳手从 east 转到 south 后背面连接从 west 移到 north、固定底部真实 Interface 网络、固定模型背面真实线缆网络、其它四面拒绝 ME 线缆、底部/背面拒绝普通 item capability，以及四个 `facing` 下仅传送带上表面取包。`.\gradlew.bat runGameTestServer` 两次成功，最终 106/106 required tests 通过；`.\gradlew.bat runData`、`.\gradlew.bat build`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 通过。自动客户端 smoke 已按同日用户决定从项目移除，`runClientSmoke` 不再是可用任务，本轮没有恢复该入口。

### 2026-07-15 ME Packager 已取走包裹残影修正

用户实机发现打包完成并取走包裹后，BER 仍绘制一个已不存在的包裹。根因是 `getRenderedBox()` 的静止分支优先读取客户端 heldBox 库存副本；AE2 方块实体 visual stream 只同步 `renderedBox` 等视觉字段，不会同时刷新该库存副本，因此服务端已发送空视觉栈后，旧库存又把包裹补回渲染。

修正后静止与动画渲染都只使用明确同步的 `renderedBox`，真实 heldBox 只在服务端库存回调中生成下一次视觉状态。新增 `mePackagerVisualSyncClearsRemovedPackage` GameTest，构造“旧客户端库存非空、服务端视觉更新为空”的精确回归场景；`.\gradlew.bat compileJava --stacktrace`、`.\gradlew.bat runGameTestServer --stacktrace` 与 `.\gradlew.bat build --stacktrace` 成功，107/107 required GameTest 全部通过。自动客户端 smoke 已从项目删除，本轮未恢复。

### 2026-07-15 包裹有序 contents 与装配室外部样板输入

按最终语义撤销 `PackageData` 的同 AEKey 合并和全局 canonical 排序。`contents` 现在按输入逐项保存，重复 AEKey 保持为独立条目，canonical hash 与 NBT 都对列表顺序敏感；因此内容总量相同但条目顺序不同的包裹不再堆叠。ME Packager 的 MEStorage 规划在生成最终包裹前单独按 canonical stack key 排序所有选中及展开后的内容，排序职责不再泄漏到包裹数据层。Package Assembler 增加 ordered plan 路径，源包裹在原输入位置展开，包裹样板、普通处理样板和高级处理样板均保持样板输入顺序，同一 AEKey 在多个样板位置出现时不合并。

对照项目锁定的 AE2 15.4.10 `MolecularAssemblerBlockEntity.CraftingGridFilter`：有效本地样板存在时才按位置接受外部输入，输入位不允许外部抽取，输出位允许抽取。Package Assembler 的 Forge item capability 因此改为 68 个按本地样板位置过滤的输入位加 1 个有序输出位；无样板、错误位置、超出样板数量、非物品 GenericStack 或向输出插入均拒绝，外部仍只能抽取输出。样板、容量元件和升级配置槽不暴露。

新增/改写 GameTest 覆盖重复 AEKey 与顺序敏感 hash/NBT、ME Packager 确定性排序、无样板外部输入拒绝、流体样板不伪装为 item capability 槽、有样板按位置输入、输入位不可抽取，以及最终包裹严格保持 `iron 32 -> copper 16 -> iron 8` 的样板顺序。`.\gradlew.bat compileJava`、`.\gradlew.bat runGameTestServer --stacktrace`、`.\gradlew.bat build --stacktrace`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过，108/108 required GameTest 成功。本轮没有客户端视觉变更，不运行 `runClient`。

### 2026-07-16 样板 Mixin 与 PackageCapacity 复核收口

逐项处理前轮代码复核点。对照 AE2 15.4.10 `AEPatternDecoder` 后确认它会按物品动态分派到 `PackageCraftingPatternItem.decode(...)`，因此删除重复拦截两个 decoder overload 的 `AEPatternDecoderMixin`。`EncodedPatternItemMixin` 的 tooltip 注入不会命中已覆盖 `appendHoverText` 的包裹样板子类；其余 `getOutput` 行为改为由 `PackageCraftingPatternItem` 直接 override，保留无客户端 Level 时也可从当前 NBT 生成包裹预览的能力，随后删除整个 Mixin 和配置项。概要设计中旧的 previous-carrier compatibility decoder 表述同步改为当前样板物品自己的 decoder。

`PackageCapacityProfile` 删除非单调的 `STORAGE_1K(1024,16)`，正式档位收敛为 default 9/9、16k 16/16、64k 64/63、256k 256/63。ME Packager 与 Package Assembler 空容量槽都使用 default 9/9，容量槽只接受 AE2 16k/64k/256k storage component；1k component、完整 item/fluid storage cell、portable cell、4k 和附属容量均拒绝。Package Assembler 的菜单分流、槽位校验和容量读取统一使用 component-only 映射；package_pattern 本地 exact plan 不再按目标包裹反推一个更大档位，Pattern Provider push 也在消费输入前复验当前容量档。

GameTest 新增/改写容量档单调性、空槽九单位/九类型、1k 与完整 cell 拒绝、16k component 接受、ME Packager 九单位实际抽取、包裹样板原生 decoder/getOutput、Pattern Provider 容量拒绝不消费，以及填入十单位后移除组件仍拒绝本地执行并保留输入。`.\gradlew.bat compileJava`、`.\gradlew.bat runGameTestServer --stacktrace`、`.\gradlew.bat build --stacktrace`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部成功，109/109 required GameTest 通过。本轮服务端行为和 Mixin 收口没有新增客户端视觉，不重复运行 `runClient`；同工作树内独立的 renderer 原生缓冲修正按其后单独记录完成客户端长时验证。

### 2026-07-16 ME Packager stencil 原生缓冲泄漏修正

用户实机开发客户端在 ME Packager 旁运行约 6 分钟后因 `MePackagerRenderer.renderCurtains()` 抛出 `OutOfMemoryError: Failed to resize buffer from 1536 bytes to 2098688 bytes`。崩溃报告同时显示 Java 堆仍有充足余量（约 995 MiB used / 2720 MiB committed / 16320 MiB max），但系统 virtual memory 已使用 176419.38/176446.06 MiB。对照项目生成的 Forge 1.20.1 源码确认 `BufferBuilder` 使用 `MemoryTracker` 的 LWJGL allocator，首次容量不足时固定增长 2 MiB，且该版本没有释放底层原生缓冲的 API；原 BER 在每台机器的每帧帘子 pass、包裹 pass 和裁剪盒 pass 中创建新实例，因此持续泄漏原生内存。

修正后 renderer 只在构造时创建一个共享 stencil animation `BufferSource` 与一个裁剪盒 `BufferBuilder`，帘子、包裹和裁剪盒每帧结束后复用同一底层缓冲；stencil 的绘制顺序、裁剪体积和视觉语义不变。`docs/04-asset-spec.md` 同步把 renderer 生命周期复用写为正式约束，禁止在 `render` 调用中分配新的原生 `BufferBuilder`。

验证：`.\gradlew.bat compileJava --stacktrace` 与 `scripts/verify-docs.ps1` 成功，`git diff --check` 无空白错误。随后复制实际崩溃世界为隔离 quick-play 世界并执行真实 `runClient`，玩家在原坐标 `(-14.303,70.871,-18.642)`、即崩溃报告所指 `(-17,68,-14)` ME Packager 旁持续渲染；客户端 CPU 时间持续增长，私有内存从 00:18:52 的 5158.8 MiB 稳定到 00:19:39 的 4884.0 MiB，并在延长观察后降至 00:20:44 的 4442.6 MiB，没有修复前的线性增长。日志未命中 `OutOfMemory`、buffer resize、crash、FATAL/ERROR、missing model/texture、OpenGL 或 stencil 错误。测试客户端正常终止，隔离世界与临时启动脚本均已删除。GameTest 已考虑且发现现有 `runGameTestServer` 路径；本轮只改变客户端 BER 原生缓冲生命周期，不改变机器、菜单、事务、同步或服务端行为，因此未新增或运行 GameTest。

### 2026-07-16 Marker fake slot 与高级样板稠密输入修正

ME Packager 的 marker 配置从真实 `ItemStackHandler` 第三槽迁移为 1 格 AE2 `ConfigInventory.configTypes`，菜单使用 `FakeSlot`。marker 只保存 AEItemKey，独立写入 `marker_filter` NBT，不消耗、不掉落真实物品；颜色、marker 与 contents 仍分别执行独立门禁。真实机器物品库存收敛为 heldBox 与容量组件两槽。

Package Assembler 不再把高级样板的 17×81 sparse 输入直接映射到旧 68 格缓冲。方块实体先按 `column * 81 + row` 扫描有效列，跳过空白并生成带原列索引的稠密输入过滤；GUI 与 Forge item capability 使用按当前样板实际非空项数动态确定的稠密输入索引，1377 只保留为高级样板编码格式上限，提交时再按原列归组。4×4 可见代理窗口显示过滤物品与数量，后续全为 disabled 槽时不滚动；只有实际输入超过可见区或样板切换后仍有尾部残留物品时才保留对应行。高级样板的颜色与 marker 继续只读列元数据，装配室不恢复已删除的机器 fallback 控件。

新增/改写 GameTest 验证 marker 菜单槽确为 `FakeSlot`、配置 NBT 往返、错误 marker 拒绝、sparse 空白压缩、跨第 69 个输入显示与外部过滤、动态滚动，以及本地稠密输入重建两个包裹时仍保持原列颜色、marker 和 row 顺序。后续按用户复核把装配室固定 1377 格后备数组和固定 capability 输出索引进一步收口为样板尺寸：测试先装入 4 项 sparse 样板并确认 4 个有效输入/0 滚动，再切换 70 项确认 70 个输入/14 行偏移，最后切回 4 项确认 disabled 尾行、滚动偏移和 capability 槽数立即收缩。第一次复跑暴露 AE2 config-types 用 amount 0 表示“仅类型”的细节，以及离线方块实体菜单主动 broadcast 的测试夹具问题；修正为按 key 是否存在判 marker、菜单同步测试使用真实世界方块实体。最终 `.\gradlew.bat compileJava --stacktrace`、`.\gradlew.bat runGameTestServer --stacktrace` 与 `.\gradlew.bat build --stacktrace` 成功，110/110 required GameTest 全部通过；`scripts/verify-docs.ps1` 与 `git diff --check` 也通过。自动客户端 smoke 已按项目决定删除；本轮用菜单实际 slot stack、同步的有效槽数、稠密滚动映射和 Screen 动态范围覆盖显示数据路径，未恢复已删除流程。

### 2026-07-16 装配室与打包机共享容量预检

ME Packager 与 Package Assembler 的容量元件识别收敛到 `PackageCapacityProfile.fromStorageComponent`，两台机器不再分别维护 AE2 registry id 映射。装配室新增统一的样板容量预检：包裹样板检查编码目标包裹，普通处理样板检查预计 Fluix 包裹，高级处理样板按列构造预计输出并逐包检查。容量不足的本地样板仍允许留在样板槽供玩家查看，但菜单同步为无效容量状态，客户端对样板槽绘制红色覆盖；空 GUI 输入位和外部 capability 输入同时锁定，本地合成拒绝，已有残留输入仍可取出。安装足够容量元件会立即解锁，移除后立即恢复锁定。

Pattern Provider 的 `pushPattern` 在任何 `KeyCounter` 扣减前执行同一预检；普通与高级处理样板任一预计包裹超限时整批返回 false，所有输入保持不变。新增 GameTest 覆盖普通/高级供应器推送的拒绝不消费与 16k 解锁，以及本地样板无效状态的菜单同步、GUI/外部输入门禁、容量安装/移除回退和残留输入保留。首次完整复跑中，既有动态输入测试的 70 个独立有序条目按新规则正确超过 16k 单包上限；夹具改装 256k 元件，继续单独验证超过旧 68 槽限制的显示、滚动和外部寻址，而没有放宽容量规则。`.\gradlew.bat compileJava --stacktrace` 成功；`.\gradlew.bat runGameTestServer --stacktrace` 最终 112/112 required GameTest 全部通过；`.\gradlew.bat build --stacktrace`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 均成功，发布审计确认 198 个发布资源、72 个 JSON、130 张 PNG、5 个资产合同及 140 个双语 key/占位符有效。自动客户端 smoke 已从项目删除；红色状态的数据同步与输入门禁由 GameTest 覆盖，未恢复已删除的 smoke 流程。

### 2026-07-16 开发 runtime 加入 JEI

为方便客户端检查物品与配方，在官方 Jared Maven 上锁定 Minecraft 1.20.1 Forge 的 JEI `15.20.0.134`，通过项目已有的 `modLocalRuntime` 配置加入开发运行时。仓库使用 exclusive content 只解析 `mezz.jei` group；JEI 不进入编译 API、发布 POM 或 `mods.toml` 必需依赖。`dependencyInsight --configuration runtimeClasspath --dependency jei-1.20.1-forge` 确认 `mezz.jei:jei-1.20.1-forge:15.20.0.134` 已进入运行类路径，`.\gradlew.bat writeClientLegacyClasspath --stacktrace` 与 `.\gradlew.bat build --stacktrace` 成功，Gradle cache 中已生成 remapped JEI jar；`scripts/verify-docs.ps1` 与 `git diff --check` 通过。检测到用户当前已有 IntelliJ 开发客户端进程，未关闭该进程或覆盖其日志；现有进程不会热加载新依赖，刷新 Gradle 后的下一次客户端启动生效。此次只修改开发依赖，不改变游戏行为，GameTest 已考虑但无需新增或重跑。

### 2026-07-16 装配室真实输入与样板过滤显示纠正

用户复测指出，前一轮高级样板稠密输入把空输入槽的样板过滤栈直接作为 `Slot#getItem()` 返回，导致容器同步和客户端把过滤物品绘制成真实槽内容，无法判断物品是否已经投入。这违反了项目此前已对照 AE2 分子装配室确认的语义：本地样板只过滤和启用真实输入槽，不在槽内显示 ghost 过滤物品。

修正后 `MenuInputDisplaySlot#getItem()` 只返回 `menuInputBuffer` 的真实内容；过滤栈查询改名为 `menuInputFilterStack` / `inputFilterForSlot`，只用于后端过滤、有效性检查和测试，不再参与槽内容同步。空槽保持视觉为空，实际插入后才显示物品；样板决定的动态槽数、sparse 空白压缩、滚动范围、外部 capability 过滤及容量门禁均不变。GameTest 扩展为先确认第 70 个有效过滤槽为空但仍启用和可外部寻址，再插入 1 个铁锭确认槽内出现真实物品，取出后恢复为空。

首次 `runGameTestServer` 的 112 个用例中有 1 项正确失败：旧的 4×4 滚动夹具使用超过 default 9/9 的 17 条目样板，真实插入一直被容量门禁拒绝，过去却由过滤 ghost 伪装成槽内已有物品。夹具改装 64k 元件并新增真实插入数量断言后复跑，`.\gradlew.bat runGameTestServer --stacktrace` 成功，112/112 required GameTest 全部通过。

最终 `.\gradlew.bat build --stacktrace`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过；发布审计确认 198 个资源、72 个 JSON、130 张 PNG、5 个资产合同和 140 个双语 key/占位符有效。客户端实际绘制读取的就是本次 GameTest 断言的菜单 `Slot#getItem()`，因此空过滤槽与真实输入的内容边界已有确定性覆盖；未恢复已删除的自动客户端 smoke。

### 2026-07-16 加速卡实际工作周期修正

用户实机指出多种设备安装加速卡后处理速度没有变化。排查确认 Package Assembler 已正确使用 AE2 分子装配室的 10/13/17/20/25/50 能量进度表；问题集中在 ME Packager 与 Package Unpacking Bus：两者虽然读取加速卡，但只把 `max(2, 20 - speedCards * 3)` 用于空闲扫描或阻塞重试，真正的打包/拆包工作动画仍硬编码为 20 tick，因此单次正常工作看不到加速。

ME Packager 现在在每次开始打包或拆包时按 0-6 张加速卡锁定 20/17/14/11/8/5/2 tick 的实际周期；剩余 tick 与本次周期总长同时写入方块实体 NBT 和 visual stream，菜单进度、BER 动画与包裹显隐都按该总长计算。Package Unpacking Bus 使用同一公式，0-4 张卡对应 20/17/14/11/8 tick，并持久化本次周期总长；15 级 GUI 进度与阻塞重试都复用同一速度语义。已有工作在中途增减卡不会跳变，下一次工作才使用新的卡数。

新增 `mePackagerSpeedCardsShortenWorkCycle` 与 `packageUnpackingBusSpeedCardsShortenWorkCycle` GameTest，分别用 6 张卡验证打包机在 2 tick 完成真实输出、用 4 张卡验证卸货总线在 8 tick 完成真实目标提交；装配室既有 `packageAssemblerSpeedCardsUseAePowerProgress` 继续验证其独立能量进度表。`.\gradlew.bat compileJava --stacktrace`、`.\gradlew.bat runGameTestServer --stacktrace`、`.\gradlew.bat build --stacktrace` 与 `scripts/verify-docs.ps1` 成功，118/118 required GameTest 全部通过。本轮没有改贴图或布局；客户端进度与 BER 只改为读取已同步的实际周期总长，未恢复已删除的自动客户端 smoke。

### 2026-07-16 Package Unpacking Bus 进度改用 UI sprite

根据实机截图检查 `PackageBusScreen` 与用户 GUI atlas，确认右上进度此前没有使用已有的绿色活动 sprite：实现从空框 `[196,0,6,18]` 取 1x1 像素，染成青色再拉伸为 4px 宽。现改为直接读取 `package-storagebus.png` 的 `[176,32,6,18]`，按同步的 0-15 级进度从底部裁切原始 6x18 像素；空框仍使用 `[196,0,6,18]`，不修改或重绘任何 PNG。

确定性像素合成检查覆盖 0/3/6/9/12/15 六档，确认条纹与颜色均来自 UI sprite 且没有缩放。`.\gradlew.bat compileJava --stacktrace`、`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部成功。项目已删除自动 `runClientSmoke`，最终游戏内显示留待重启开发客户端后人工确认；本轮不改变总线工作周期、同步、菜单或服务端事务，不新增或重跑 GameTest。

### 2026-07-16 加速卡速度表与打包机动画二次修正

用户进一步确认实际规则并取代上一节的临时线性公式：ME Packager 打包 0-6 张加速卡使用 40/30/20/15/10/6/4 tick；拆包只识别前 4 张，0-4 张使用 20/15/10/6/4 tick，5-6 张仍为 4 tick。Package Unpacking Bus 复用拆包表并保持最多 4 张。每次开始工作锁定本次总长；成功启动工作后不再额外叠加一轮空闲冷却，失败扫描与阻塞重试才使用对应速度表。

世界动画与实际事务分离：包裹移动窗口最长 20 tick，40 tick 默认打包的前 20 tick 保持静止，只在后 20 tick 从机内移动到前部输出点；20 tick 及以下的工作使用整个周期。传送带不再固定每 tick 移动 1px，而是复用包裹的同一位移增量，完整进出包裹和 UV 相位都移动 7.5px。滚动相位改为 float 并写入 NBT/visual stream，停止和保存后保持连续。

帘子不再读取归一化工作进度并强制播放完整正弦开合。方块实体保存独立的有符号偏转与回弹速度；包裹经过时按实际每 tick 位移加权推动，随后用独立弹性和阻尼恢复。4 tick 快速工作结束时允许帘子仍处于回弹中，后续 tick 自然归位；反向工作开始时也从当前物理状态连续响应，不瞬间切换方向或强制归零。

GameTest `mePackagerSpeedCardsUsePackingWorkTable` 覆盖完整打包表、2 卡实际 20 tick、6 卡实际 4 tick、6 卡拆包仍按 4 卡截断和快速工作结束时帘子仍有残余偏转；默认打包与背面接线测试继续覆盖 40 tick 前半段包裹/传送带/帘子保持静止，以及传送带 7.5px 相位保存往返。`packageUnpackingBusSpeedCardsUseUnpackingWorkTable` 覆盖完整拆包表和超过 4 卡的截断。`.\gradlew.bat runGameTestServer --stacktrace` 成功，118/118 required GameTest 全部通过；`.\gradlew.bat build --stacktrace`、`scripts/verify-docs.ps1` 与 `git diff --check` 也通过。现有开发客户端进程不会热加载本次 Java 修改，最终手感需在下一次客户端重启后复核；未中断或覆盖用户并行进行的终端改动。

### 2026-07-16 原版样板终端完整五模式替换（同日按实现边界撤销）

用户复测指出原版终端快速切换时会先闪到 crafting 再返回选中模式，并且编码后有概率崩溃。前者由客户端 package boolean 与 AE2 原生 `mode` 分开变化造成，旧实现又同时 Mixin Screen、crafting panel、processing panel、MEStorageScreen 和 Render.Post，使显示结果依赖多层更新顺序。后者由 `PackageCraftingPatternItem` 的 tooltip 静态调用未注册遗留 `PackagePatternItem` 引起；只有编码产物进入鼠标下方并开始构建 tooltip 时才触发，因此表现为概率性。

客户端重构为一个本地 `AppliedPatternEncodingTermScreen`，完整拥有 AE2 新版风格的 crafting、processing、smithing、stonecutting 和 package 五个 panel。只在 AE2 15.4.10 `InitScreens.register(MenuType, StyledScreenFactory, String)` 的精确 descriptor 上用一个 Mixin 替换原版终端 factory；三个 accessor 只暴露网络滚动条样式和 Vanilla slot 坐标。删除旧 Screen/panel/overlay/client-event Mixin 与对应桥接类。终端底图改用 AE2 `neoforge/v19.2.17` 原字节 `pattern.png`，避免高级样板终端中部框线从五模式 panel 周围泄漏；source/runtime SHA-256 均为 `573E8852E2590262FD5405121549F48B7B78ED79199F615FC0B068C773A1F6BE`。复核还发现用户 `pattern_mode_packaging.png` 只有左上 package 面板有效、其它 atlas 区域透明，因此四种原生模式改读同版本原字节 `pattern_modes.png`（SHA-256 `2D90F978971946833532B0ABF12F73975ED5ACA9F9F67362F80C34A2A489B86E`），package 单独保留用户面板。

服务端菜单新增五值 `PatternTerminalMode` 复合状态和一次 `apSelectTerminalMode` action，原子更新 AE2 mode 与 package logic；客户端 pending 模式在服务器确认前保持最后选择并禁用编码。正式 `PackageCraftingPatternItem` 直接实现 tooltip，孤儿 `PackagePatternItem` 删除。GameTest 扩展模式原子投影与正式物品 tooltip 两条回归，`.\gradlew.bat runGameTestServer --stacktrace` 最终 118/118 required tests 通过；`compileJava processResources`、`build`、资产合同、完整资产审计和 24 个资产正/负自测夹具成功。真实 `runClient` 完成资源重载、声音与图集初始化，日志没有 Mixin、类加载、缺失资源或崩溃阻断；自动 client smoke 已按项目决定删除，五模式实际快速点击与最终像素仍留给下一次开发客户端人工打开终端复核。

### 2026-07-16 原版样板终端改为 package-only delegate

用户明确实现边界：AE2 原版终端及其四种模式不应被本模组 Screen 替换；原版终端只额外通过事件增加包裹模式按钮，只有切换到包裹模式后才委托自定义包裹区域绘制。已删除 `AppliedPatternEncodingTermScreen`、`PatternSetAmountScreen`、`InitScreensMixin`、替换 ScreenStyle、复制的终端底图/四模式 atlas 及其资产门禁；正式 tooltip 崩溃修复、菜单复合模式原子 action 和 GameTest 保留。

当前客户端结构为 `ScreenEvent.Init.Post` 添加包裹 tab/清空/颜色控件，`PackagePatternScreenDelegate` 只管理 package 的 124x66 面板、processing 输入窗口、滚动、marker、预览和颜色弹层。`PatternEncodingTermScreenMixin` 只在原 Screen 更新末尾选择条件 delegate；`ProcessingEncodingPanelMixin` 只在 package 显示时委托背景和布局，非 package 分支完整执行 AE2 原实现。切换确认前由 delegate 保持最后请求的 panel 并临时禁用 tab/编码，避免闪到 crafting；中键数量编辑重新直接使用 AE2 原生子 Screen。

最终将 `ProcessingEncodingPanelMixin` 改为直接继承 AE2 `EncodingModePanel` 并访问受保护的 `screen/widgets`，删除额外 `EncodingModePanelAccessor`；包裹终端只新增两个行为 Mixin。完整 `compileJava --rerun-tasks` 通过，真实 `runClient` 完成 OpenAL 和全部纹理图集创建，日志没有终端 Mixin、类加载或已删除整屏资源请求错误；本次客户端只报告并行开发中 `sequence_buffer` 的缺模型。局部委托完成检查点的 118 个 required GameTest 全部通过；稍后并行 Package Assembler/包裹数据改动进入工作树后重跑全量 GameTest，9 个装配室断言失败并在其自动导出测试中止，失败列表不含终端模式或 tooltip 用例，故当前全量门禁如实记为未通过。自动 client smoke 已按项目决定删除，本轮未恢复。

用户再次纠正：这里的 delegate 指接管包裹模式下的整个屏幕，而不是只接管 124x66 package panel；上一轮把整屏新版 UI 全撤回属于实现边界误读。修正后继续保留 AE2 原 `PatternEncodingTermScreen` 与 factory，原生四模式只多一个事件添加的 package tab；package 模式由 `ScreenEvent.Render.Pre` 取消外层绘制并用同一个原 Screen 实例完成 delegate 帧。该帧恢复新版整屏底图、五 tab、工具栏、网络 scrollbar、package panel、样板槽、Encode、合成状态、hover、tooltip 与拾色弹层；切回原生模式在同一帧恢复原 geometry、widget、scrollbar 与 slot positioning。

删除 `ProcessingEncodingPanelMixin`，客户端只保留一个行为 Mixin 作为条件绘制/geometry bridge；`AppliedPatternEncodingTermScreen`、`InitScreensMixin`、替代 ScreenStyle 和四原生模式 atlas 仍保持删除。字节保持的 v19.2.17 `pattern_encoding_terminal.png` 仅作为 package 整屏 delegate 底图恢复，固定 SHA-256 为 `573E8852E2590262FD5405121549F48B7B78ED79199F615FC0B068C773A1F6BE` 并恢复资源门禁；LGPL 底图和派生整屏绘制代码新增独立来源记录。package 小滚动条加入 client tick，补齐按住轨道时的原生重复翻页语义。`compileJava --rerun-tasks` 与 `build` 成功；完整 `runGameTestServer` 132/132 required tests 通过；资产审计、资产负例自测、文档审计、发布资源/asset contract 审计和 `git diff --check` 全部通过。真实 `runClient` 完成资源重载、OpenAL 与全部图集创建，debug 日志确认 `PatternEncodingTermScreenMixin` 成功应用且无 Mixin/类加载错误。自动 client smoke 已删除，因此整屏像素和实际快速点击仍待开发客户端人工复核。

### 2026-07-16 Advanced Pattern Encoding Terminal v19 Part 渲染替换

将高级样板编码终端从继承 AE2 15.4.10 原终端模型改为自有三组 `PartModel`：断电、供电、频道状态继续走 v19 同款 `selectModel` 路径，世界模型与物品模型几何来自固定 `neoforge/v19.2.17` 提交 `79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a`，状态灯改为 v19 四段式。NeoForge 模型全亮字段按 Forge 1.20.1 等价改为 `forge_data`；客户端注册六个自有模型，并为本模组 PartItem 注册 AE2 `StaticItemColor(AEColor.TRANSPARENT)`。

用户源 `adv_pattern_encoding_terminal_dark.png`、`_medium.png`、`_bright.png` 以原字节接入并按 tint 1/2/3 同时叠加；八张 v19 外壳/状态贴图原字节复制。新增 `ae2-terminal-part-source.txt` 记录模型映射、提交、逐文件 SHA-256 与 LGPL，资产合同、brief、规格、验收、报告和发布许可证例外同步更新。资源审计新增世界/物品几何、tint 顺序、Forge 全亮、四段状态灯、源哈希及两个负例；AE2 固定哈希的透明 `monitor_colored.png` 作为精确例外保留，普通透明/纯色占位仍失败。

`assetgen validate-contract` 成功；`assetgen render-model` 加载完整物品模型并输出四视图几何预览，但不执行 Forge tint handler，因此不作为游戏内色彩证据。`.\gradlew.bat build --rerun-tasks --stacktrace` 强制重编译成功；`scripts/verify-assets.ps1`、包含 24 个夹具的 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1` 和 `scripts/verify-release.ps1 -RequireAssetContracts` 全部通过。真实 `.\gradlew.bat runClient --stacktrace` 到达完整资源重载、OpenAL 和 block atlas 创建；日志未命中 missing model/texture、ModelBakery、Mixin、类加载、ERROR 或 FATAL，随后手动终止本次客户端。该变更不修改菜单、存档、网络或服务端行为，GameTest 已考虑但未重复运行；世界内最终颜色/朝向截图仍需人工放置终端后确认。

### 2026-07-16 Advanced Pattern Encoding Terminal 放置模型复核修正

用户指出物品模型已经变化，但放置后的 Part 模型没有变化。建立隔离客户端场景后，真实放置并供电的 `AdvancedPatternEncodingTerminalPart` 在客户端确实选择了本模组 `base/on/status_has_channel` 三个模型，用户 dark/medium/bright 遮罩也已出现在世界模型；但继续对比上游确认 v19.2.17 的 `models/part/display_base.json` 与 15.4.10 主体几何完全相同。因此上一节把“模型 ID 和贴图已替换”误判成“Part 几何已升级”，用户反馈正确。

世界 Part 基础层现改为直接采用固定 v19.2.17 六段 item display 几何并沿 Z 轴平移 -7 到 cable-part 坐标：保留显式 UV、`monitor_colored` tint index 4、正面基础层和背部 8x8x1 几何，再叠加用户 tint 1/2/3 遮罩与 v19 四段状态灯。物品和放置形态由此使用同一套新版几何语义，不再复用与旧版同形的两段 world display base。既有真实 Part GameTest 增加自有 world model ID 断言；资产审计增加三段 translated base、tint 4 和背部坐标断言。客户端诊断 runner 仅用于本次截图验证，完成后从正式源码移除。

隔离客户端实机复核记录了断电 `base/off/status_off` 与供电 `base/on/status_has_channel` 两组自有模型选择，并生成 `run/screenshots/appliedpackaging-advanced-terminal-part-v19.png`。临时 runner 与隔离世界已删除。`.\gradlew.bat runGameTestServer --stacktrace` 通过 118/118 required GameTest；`.\gradlew.bat build --rerun-tasks --stacktrace`、`scripts/verify-assets.ps1`、包含 24 个夹具的 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。

### 2026-07-16 序列缓存器首版实现

按补充语义完成序列缓存器：端点只是结构控制器和聚合能力入口，不保存资源，也不计入逻辑第 1 格；后续连续成员各自保存一种 AEKey/数量，并在第一次真实插入后锁存到完全清空。默认单格容量 1024、默认最大结构长度 128，配置降低后不删除已有超限内容，而是保留并限制为只可抽取直到重新落入容量范围。`IItemHandler`、`IFluidHandler` 与 `MEStorage` 共用同一状态，模拟插入不锁存，端点顺序输入、聚合抽取且不自动输出。

拓扑由水平 facing 与方块更新驱动，覆盖成型拒绝、尾端自动加入、端点侧保留/尾侧解散、成员顺序和端点配置同步。自动输出支持无方向固定侧面顺序或指定垂直方向；阻挡检查目标整体为空，同步模式在任一成员阻挡/无法完整输出时禁止整组提交，输入延迟在倒计时期间同时阻断主动和被动输出。第一版已持久化自动输出、阻挡、同步、样板模式、延迟和 AEKey filter，但按需求不注册 GUI。

`PackageData` 升级为 schema v2，新增可选 `PackageLayout` 并纳入 NBT、规范哈希和身份比较。普通 Crafting/Processing/Package 样板按 sparse 原位置映射且不跳过空位；高级样板不处理样板模式，只按实际非空输入稠密顺序处理。Package Unpacking Bus 检测序列缓存器端点后走同 Mod 原子计划入口，按包裹布局保留空位；阻塞、容量或提交校验失败时 held 包裹不消费，目标成员不部分写入。

新增 `SequenceBufferGameTests` 与独立 8x5x8 空结构模板，消除长直线用例并发时的空间重叠。全量 `.\gradlew.bat runGameTestServer` 通过 131/131 required tests；`.\gradlew.bat build`、`.\gradlew.bat runData`、`scripts/verify-assets.ps1`、`scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts`、`scripts/test-release-audit.ps1` 与 `git diff --check` 通过。真实 `.\gradlew.bat runClient` 完成资源重载、OpenAL 和全部图集创建，日志没有序列缓存器 missing model/texture、ModelBakery、ERROR 或 FATAL；仅出现开发账号 Realms 鉴权提示，随后只终止本次测试客户端。确定性 32x32 RGBA 贴图、模型、blockstate、配方、loot、asset contract、验证规则与资产报告均已纳入仓库；asset contract 同时由发布审计的 assetgen 路径验证。

### 2026-07-16 序列缓存器扳手方向循环修正

用户补充明确同一水平侧的扳手循环顺序：第一次指向点击面的对面方向，第二次才指向点击面方向，第三次恢复无方向。`SequenceBufferBlock` 的未成型方块和普通成员统一使用该三段状态机；如果第一段已经形成结构，第二次点击端点会先解散旧结构，再把原端点推进到点击面方向，因此不会被 `endpoint` 状态截断循环。切换到与当前方向无关的新水平侧时，从新点击面的对面方向重新开始；成员仍拒绝结构轴方向。

新增真实 AE2 Certus Quartz Wrench + FakePlayer GameTest，覆盖外侧面第一次点击向内成型、第二次解散并转向外侧、第三次无方向，以及重新成型后成员的对面/点击面/无方向完整循环。`.\gradlew.bat compileJava --stacktrace`、`.\gradlew.bat runGameTestServer --stacktrace` 与 `.\gradlew.bat build --stacktrace` 成功，全量 132/132 required tests 通过；`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 通过。

### 2026-07-16 包裹模式改为同 menu 的独立整屏

用户实机截图证明上一版 Render.Pre 整屏委托仍没有真正隔离 AE2 `WidgetContainer`：旧 VerticalButtonBar 在 delegate 定位后再次被 AE 更新移回远端，右上旧 view-cell/upgrades 面板继续绘制，原 slot icon 也与新版占位叠加。实现边界最终确定为：原 `PatternEncodingTermScreen` 与 factory 完全保留，原生 Screen 只通过 `ScreenEvent.Init.Post` 多一个 package tab；点击后临时显示一个使用同一 `PatternEncodingTermMenu` 的 package-only `MEStorageScreen`，而不是让两套 Screen 同帧绘制。

新的 `PackagePatternScreenDelegate` 在构造时复制原 Screen 的 Repo 条目/供电、搜索词和网络滚动，接管 menu `clientRepo/gui` 绑定，并清除只属于旧 presentation 的 slot icon。包裹 Screen 自己加载 namespaced ScreenStyle、五个 current-AE tab、左侧 toolbar、右上 `ModernUpgradesPanel`、网络区、124x66 package panel、Encode/合成状态、hover/tooltip 与颜色弹层；旧 view-cell/upgrades panel 在 style 中移出可见区。右上新版面板绑定终端真实的五个 `VIEW_CELL` 槽而非不存在的 `UPGRADE` 槽，并从 current-AE states 绘制空显示元件图标。点击任一原生 tab 时先做单一 `PatternTerminalMode` 本地/服务端投影，再复制实时客户端状态、恢复原 slot icon 和 menu 绑定，通过 AE 同菜单子 Screen 路径回到保留的原 Screen。中键数量编辑使用 package-only 父级的 `PatternSetAmountScreen`，仍复用 AE 数量 style 与 `SET_FILTER` packet。

删除 `PatternEncodingTermScreenMixin`、Screen host bridge 与 `InitScreensMixin`；客户端配置只剩 `MEStorageScreenAccessor`、`ScrollbarAccessor`、`SlotAccessor` 三个窄访问器。资产门禁新增 package-only style 必需项，并拒绝 AE 原生 terminal style override、`pattern_modes.png`、复制原生 mode panel 和旧/new side panel 同屏。`compileJava --rerun-tasks`、`build`、132/132 required GameTest、资产审计及全部正负夹具、文档审计、带资产合同的发布审计和 `git diff --check` 全部通过。真实 `runClient` 完成资源重载、OpenAL 与图集创建，日志确认 delegate 已自动订阅且无 Mixin/类加载/缺失资源错误；没有自动 UI 驱动，最终 package 像素和快速点击留待人工打开终端复核。

### 2026-07-16 样板终端载体限制与自动页面同步

普通 Pattern Encoding Terminal 的已编码样板槽增加精确载体限制，只拒绝无法在该终端编辑的 `advanced_processing_pattern`；专用 Advanced Pattern Terminal 继续接受并编码该载体。终端菜单不再把自身 GuiSync 字段作为样板载入瞬间的模式权威：构造、broadcast 和 encoded slot 同步后都从持久化 `PatternEncodingLogic` 一次性投影 AE2 mode、package 标志、颜色、复合 `PatternTerminalMode` 与槽位状态。

客户端 package-only Screen 跟随同步后的复合模式：原生 Screen 发现 PACKAGE 时在下一帧前用同一个 menu 打开 delegate，package Screen 发现任一原生模式时恢复保留的原 Screen。由此覆盖关闭时位于 package 后重新打开、放入 package pattern 自动进入 package、放入 AE2 crafting pattern 自动回到 crafting。package preview/encode 拦截也改为直接检查 logic 的 package 标志并读取 logic 颜色，避免 encoded slot 已载入而 menu 字段晚一步时短暂显示木棍等原版 recipe 输出。右侧新版面板从错误绑定空 `UPGRADE` 槽改为绑定五个真实 `VIEW_CELL` 槽，并改绘 current-AE view-cell 空槽图标；复用既有 composite panel，没有增加访问器或行为 Mixin。

扩展真实终端 GameTest 覆盖普通/高级槽位正反限制、package/crafting 样板双向自动切页、蓝色包裹预览内容与 logic NBT 恢复。`.\gradlew.bat runGameTestServer --stacktrace` 通过 132/132 required tests；`.\gradlew.bat build --rerun-tasks --stacktrace`、`scripts/verify-assets.ps1`、完整资产正/负夹具、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 通过。检测到用户 IntelliJ 开发客户端仍在运行，本轮没有中断该进程；最终 UI 需重启客户端加载新 class 后复核。

### 2026-07-17 高级/包裹终端合并与双 profile 布局修正

按最终范围撤销对 AE2 原版 Pattern Encoding Terminal 的包裹页面/Screen 替换；普通终端只保留一个菜单槽位验证 Mixin，并同时拒绝 `package_pattern` 与 `advanced_processing_pattern`。独立 Package Pattern Terminal 注册、物品、part、菜单、Screen 与资源入口删除；现有 Advanced Pattern Encoding Terminal 的同一个 part/menu/screen 同时持有 ADVANCED 与 PACKAGE。高级状态保存 17x81 输入、32 输出、列数与列色；包裹状态保存 81 输入、marker 与颜色。两组配置库存完全隔离，切页不复制、迁移或清空；载体槽放入两种专用样板时自动切到对应页，页面持久化后重开保持上次选择，包裹输出由包裹状态直接生成而不走原版 crafting result。菜单覆盖 `hideViewCells()`，不创建显示元件槽。

用户截图复核后取消“只在高级编辑框上覆盖 124x66 panel”的实现。`AdvancedPatternEncodingTermScreen` 现拥有两套完整几何 profile：相同网络行数下高级 bottom 为 197px、包裹 bottom 为 180px，因此两行网络库存时分别为 195x250 与 195x233。模式切换仍保留同一个 Screen/Menu，只在同步模式改变后调用同 Screen 的 resize/init，重新居中并重排 full-screen base、标题、搜索、网络滚动条、玩家物品栏、载体槽、Encode、合成状态和当前页槽位。包裹 panel 使用原始 `left=8,bottom=165`，输入/marker/小滚动条使用 bottom=158，输出保持面板内 `(98,31)`，Encode bottom=145；全部为 bottom-relative，网络行数增加时不会向上叠入网络库存。

右侧两个模式按钮不再使用孤立的 22x22 `TabButton`。实现直接对照固定 AE2 `neoforge/v19.2.17` 的 `PatternAccessTermScreen`、`VerticalButtonBar` 与 `IconButton`，把相同竖向控件镜像到主界面右侧：连续九宫格 `vertical_buttons_bg` 外框、16x16 逻辑命中区、18x20 normal/hover/focus 背景、6px 间距；选中页使用 focus 背景。原版终端不增加按钮或客户端绘制注入；客户端 mixin 列表仍只有三个窄 accessor。

验证执行 `.\gradlew.bat compileJava --rerun-tasks --stacktrace`、`.\gradlew.bat runGameTestServer --stacktrace` 与 `.\gradlew.bat build --stacktrace` 成功，132/132 required GameTest 全部通过；真实菜单测试新增普通终端同时拒绝两种专用载体的断言。`scripts/verify-assets.ps1`、`scripts/test-assets-audit.ps1`（含错误 package profile 负例）、`scripts/verify-docs.ps1`、`scripts/test-release-audit.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。用户现有 IntelliJ 客户端从本轮 class 编译前已在运行且占用 `run/logs`，本轮未终止或并发启动第二客户端；最终像素和快速切换需在该客户端重启并加载新 class 后人工复核。

### 2026-07-17 合并终端右侧模式标签位置纠正

用户提供局部截图后重新核对 AE2 `neoforge/v19.2.17` 的 `PatternEncodingTermScreen`、`TabButton`、`states.png` 与 `screens/terminals/pattern_encoding_terminal.json`，确认上一段把右侧模式控件判定为 Pattern Access Terminal `VerticalButtonBar/IconButton` 是错误的。参考图实际是 Pattern Encoding Terminal 的 `TabButton.Style.HORIZONTAL`：22x22 normal/selected/focus 背景，标签坐标以 21px 步进连续排列，ItemStack 图标偏移 `(3,3)`，并位于编码区右边缘而非右上角外接工具栏。

包裹 profile 保持原生 195x233，并把两个标签放在纹理已有的右侧预留区域；高级 profile 的 195px 主体已被编辑区和载体槽占满，因此总宽改为 217x250，在不移动或覆盖载体槽的前提下增加 22px 右侧模式标签区。两个标签都从网络行结束后 6px 开始，第二枚向下 21px；高级/包裹页面切换仍只对同一 Screen 执行 resize/init。`entriesShown` 从相对总宽的 right anchor 改为主体内固定 left anchor，避免高级页加宽后网络计数漂入标签区。删除模式专用九宫格竖向外框绘制，左侧公共工具栏仍继续使用既有 `ModernVerticalToolbar`。

离线按真实 atlas 和运行时分段规则拼接 217x250 / 195x233 两页预览，确认标签贴合各自右侧区域且不覆盖高级页载体槽；`.\gradlew.bat compileJava --rerun-tasks --stacktrace` 成功。真实客户端进程仍为修改前启动，本轮不终止用户进程，最终图标、tooltip 和点击区域仍需客户端重启后人工复核。

### 2026-07-17 序列缓存器用户贴图拆分与完整模型映射

接入用户绘制的 `E:/resources/textures/appliedpackaging/ret/sequance_buffer_all.png`。源文件为 64x64 RGBA、SHA-256 `66A26C07983D8E3CD1866B0D4EE723F2A68B1C257FCD936BCC0C3C57EECF7B8F`；新增 `scripts/split-sequence-buffer-textures.py`，按 4x4 网格把原始像素确定性拆成 16 张 16x16 RGBA，集中输出到 `textures/block/sequence_buffer/faces/`。脚本不缩放、重绘、插值、量化或改色，并在每次执行时重组全部格子做逐像素往返校验，同时在 `build/asset-reference/sequence-buffer/user-sheet/` 生成 proof sheet 和逐格 SHA-256 manifest。

移除首版程序生成的四张临时 32x32 贴图和旧生成脚本，重写 Sequence Buffer blockstate/model 映射，使 16 张用户贴图全部进入运行时模型。主方块使用专用背面/侧面及朝结构内的遮挡面，中间成员使用第二列，边缘尾部使用第三列与专用尾部背面；定向模型按正面/侧面/背面分别映射。由于既有五类 visual state 不足以判断成员是不是末格以及尾背朝向，新增仅用于渲染的 `tail` 和 `sequence_direction` blockstate 属性；拓扑成型、延长和断裂时同步维护，GameTest 增加初始尾部、延长后尾部迁移和结构方向断言。

验证结果：`.\gradlew.bat runGameTestServer --stacktrace` 132/132 required tests 全部通过；`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、`scripts/test-assets-audit.ps1` 全部正/负夹具和 `scripts/verify-release.ps1 -RequireAssetContracts` 通过。资产门禁要求 16 个精确文件、16x16 RGBA、可见非占位内容、模型实际引用、27 个 multipart 组合、四个结构方向和中间/尾部旋转；发布审计确认 245 个发布资源与 JAR 一致、91 个 JSON 可解析、158 张 PNG 有效、6 个 asset contract 通过。修改前启动的 IntelliJ 客户端 PID 3220 未被终止或覆盖，本次不并发启动第二客户端，最终世界内像素效果需在现有客户端重启后人工复核。

### 2026-07-17 序列缓存器六向结构与本地方向保持修正

根据用户实机截图和补充语义，把序列缓存器的本地方向与多方块结构方向彻底拆开。`SequenceBufferBlock` 改用六向 `BlockStateProperties.FACING`，新增独立 `directional` 标记；`sequence_direction` 扩展到六向，`axis` 支持 X/Y/Z。邻居调度扫描六面，竖直连续线可与水平线一样成型、延长和断裂。拓扑提交只更新 `state/axis/sequence_direction/tail/controllerPos`，不旋转或清空任何方块原有 `facing`；平行于结构轴的预存方向在成型时使用普通连接模型隐藏，解体后按原值恢复。旧 directed visual state 会迁移方向标记但不改变 facing。

用户贴图的方向侧面箭头确认沿纹理 `+U`。新增 `scripts/generate-sequence-buffer-models.py`，按 Minecraft 1.20.1 六个面的 UV 方向显式生成 57 个模型和 58 个 multipart 条目，覆盖六向单块/端点/尾部、X/Y/Z 中段和全部合法定向成员；箭头 `+U` 指向本地方块正面，中段条带 `+V` 沿结构正轴，边缘/尾部条带 `+V` 朝结构内部。移除依赖水平 blockstate 旋转的旧顶层定向模型，保留一个完整 `0..16` cuboid。直接导出模型预览解析了真实 JSON 与 16x16 PNG，north 单块、Y 轴 east 输出中段和向上尾部均不再显示 missing-model 占位。

第一次全量 GameTest 暴露旧断言仍要求断裂后的端点失去方向；实现实际已正确保留 EAST 且没有跨缺口重组，因此把断言改为新需求。重跑 `.\gradlew.bat runGameTestServer --stacktrace` 后 135/135 required tests 全部通过。`.\gradlew.bat compileJava --stacktrace`、`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、包含缺失 Y 轴模型新负例的完整 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1` 与 `scripts/verify-release.ps1 -RequireAssetContracts` 全部通过；发布审计确认 295 个资源、141 个 JSON、158 张 PNG、6 个 asset contract 和 143 个双语 key。修改前启动的 IntelliJ 客户端 PID 3204 仍在运行，本轮没有终止用户进程或并发启动第二客户端，需重启后进行最终世界内视觉复核。

### 2026-07-17 合并终端新 GUI 坐标与 sprite 标签

同步用户更新的 `E:/resources/textures/appliedpackaging/ret/adv-pattern-terminal-base.png` 和 `pattern_mode_packaging.png` 原始字节到运行时资源，SHA-256 分别为 `9586E6422D039A58C1188F5DA4F504FDE04870E4383F29E56FA9FE2752CCDD00`、`65DE82E33052D1F941182863D8303C4D22BA52C07528AC69702B9BA685153096`。像素测量确认两页统一使用 195px 宽、192px bottom，两行网络库存时总高 245px；模式 atlas 下半区 `[0,128,132,78]` 与底图 `(8,68,132,78)` 高级面板逐像素一致，上半区 `[0,0,132,78]` 是同位置包裹面板。

ScreenStyle 与动态槽位按新图重算：高级输入/输出为 `(21,bottom=164)` / `(119,bottom=164)`，包裹输入/marker/输出为 `(24,bottom=164)` / `(109,bottom=164)` / `(112,bottom=140)`，空白/编码载体为 `(150,bottom=165)` / `(150,bottom=118)`，Encode 为 `(150,bottom=145)`。两页继续使用彼此隔离的菜单槽组；模式改变只切 active/position 和 132x78 panel，不再因相同尺寸调用 Screen `resize/init`，避免切换时重建 widget 或干扰输入物品。

按用户澄清，右侧模式按钮不再使用包裹样板、高级样板等物品图标。`PatternModeButton` 只持有 `Blitter`：上方包裹标签读取 `advanced_pattern_encoding_terminal_sprites.png` 的 `[32,0,16,16]`，下方高级标签读取 states atlas 的 processing/furnace `[16,32,16,16]`，并按 AE2 v19 水平标签 `(3,2)` 偏移绘制；代码中没有对这两个标签调用 `renderItem` 或物品装饰渲染。离线预览输出到忽略目录 `build/asset-preview/combined-terminal-new-gui/`，确认包裹在上、熔炉在下及两页 selected 状态。

验证结果：`.\gradlew.bat compileJava --rerun-tasks --stacktrace` 强制重编译成功，`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1` 正/负夹具与 `scripts/verify-docs.ps1` 通过。GameTest 已考虑；本轮不改变菜单、编码数据、网络同步、状态持久化或服务端事务，故不新增或重复运行。最终游戏内像素缩放、hover/focus 与点击区域仍需重启开发客户端后人工确认。

### 2026-07-17 JEI、Create 与 GTCEu 高级配方导入

按 Minecraft 1.20.1 官方发布线加入 JEI `15.20.0.134`、Create `6.0.8-291` 和 GTCEu `7.5.3` 开发依赖。三者都不写入 `mods.toml` mandatory dependency：JEI API 与 Create/GTCEu API 只参与编译，完整 Mod 只进入开发 runtime；JEI 插件仅在 JEI 存在时发现，Create/GTCEu 适配器又分别由 ModList 和反射隔离加载。最初用 Create slim jar 启动真实 runtime 时暴露缺少 mandatory Ponder 的问题，随后保持 slim compileOnly、把完整 `all` jar 用于 runtime，客户端与 GameTest server 均能正常启动。

新增 Advanced Pattern Encoding Terminal 专用 JEI 通用 recipe-transfer handler。客户端先把 recipe 转换为无世界副作用的 `AdvancedPatternTransferPlan`，检查最多 17 列、每列 81 输入、4 输出、单项 SNBT 4096 字符和 AE2 client action 32767 字符上限；服务端先验证总 payload 再解析各 GenericStack，并通过一次 `replaceRecipe` 原子替换，失败时不留下部分终端状态。Create Sequenced Assembly 把初始输入与各循环/步骤的外部消耗按列展开，普通 ProcessingRecipe 映射为单列；GTCEu 把确定性 item/fluid 一次性 content 与 tick content 乘 duration 后映射。随机输出、区间数量、多结果池、不可表示能力和任何越界 recipe 都以双语 JEI tooltip 保守拒绝；非消耗 Create held tool 与 GTCEu chance 0 catalyst 不错误写入输入。

验证结果：`.\gradlew.bat compileJava`、`.\gradlew.bat build`、`.\gradlew.bat runData` 成功；`.\gradlew.bat runGameTestServer` 138/138 required tests 通过，新增测试覆盖计划 payload 往返/原子替换、真实 `create:sequenced_assembly/sturdy_sheet` 顺序展开和真实 GTCEu 确定性 recipe 映射。真实 `.\gradlew.bat runClient` 同时加载 JEI、Create、GTCEu 与 Applied Packaging，完成 Forge 初始化、资源重载、OpenAL 和图集创建并到达主菜单，随后主动结束客户端；没有 Applied Packaging classloading、missing model/texture、ERROR 或 FATAL。JAR 复核确认 JEI plugin、Create adapter、GTCEu adapter 已打包，同时 `mods.toml` 没有三项硬依赖。

发布日志审计第一次准确发现 Create/GTCEu 自带的已知第三方 optional-integration 缺类告警。`verify-release.ps1` 现在只忽略明确列出的 `PonderWorld`、Xaero Map 和 ModernFix client integration 三类第三方 warning，其他未知缺类（尤其 Applied Packaging 自身类）仍失败；`test-release-audit.ps1` 增加对应正负夹具。机械发布审计、发布审计自测、文档/资产审计和聚合 release self-tests 均纳入本轮最终复验。当前唯一未自动完成的是进入世界后的 JEI “+”按钮、拒绝 tooltip 和导入后页面/列状态；仓库没有自动 UI 驱动，这些项目保留为明确的人工客户端验收，不伪写成已通过。

用户继续要求“包裹模式依照 JEI 常规兼容方式做万能适配”后，handler 扩展为按当前 ADVANCED/PACKAGE 页导入。标准 fallback 直接解释 JEI `INPUT`、`OUTPUT`、`CATALYST`、`RENDER_ONLY`：只有 INPUT 进入消耗列表，OUTPUT 进入确定性结果，催化剂和展示槽跳过；高级页生成一个普通列，包裹页把输入展平到 81 格并用第一个确定性物品输出作 marker。两页分别使用 `AdvancedPatternTransferPlan` / `PackagePatternTransferPlan` 和原子 `replaceRecipe`，导入当前页不会清空另一页。输出槽有多个不同候选、未知类型、超限或 payload 过大时在发送 action 前拒绝。

Create 专用路径新增 Mechanical Crafting：裁去空行/列后统计非空行和非空列，选择能产生更少包裹的方向，数量相同固定按行；每个分组保持原网格次序，随后高级页保留为列、包裹页按列顺序展平。GTCEu 修正 ranged content 的倍率变量泄漏，且保持 chance/区间保守门禁。Gradle 新增可选 `-PgtceuRuntimeJar=<versioned-jar>`，compile API 仍固定上游 7.5.3，替代 runtime 使用 ModDevGradle 可重映射的本地 flatDir module，并把全部 run 隔离到 `run-gtceu-fork`，避免上游/Fork 注册表存档互相污染。

兼容探索对照了 Mekanism、Immersive Engineering、Thermal、Botania、PneumaticCraft、Ars Nouveau、Industrial Foregoing 和 Ender IO 的 1.20.x/1.20.1 官方源码。通用 JEI role 已覆盖它们的常规 item/fluid 输入输出；额外反射门禁拒绝 Mekanism 锯木副产概率、IE/Thermal/Ender IO/Ars 的公开概率结果、PneumaticCraft 爆炸损耗、Botania Orechid 权重产出、IF Laser Drill 世界/权重产出、Ars reagent NBT 保留等不能精确编码的语义。Thermal 部分 category 把非消耗 catalyst 也标为 INPUT，因此按 recipe 暴露的 item/fluid 输入数量配额只取真实消耗槽。仍不能可靠判断“模组把工具误标 INPUT 且没有任何公开语义”或“概率只藏在 tooltip”这两类不规范实现；发现具体 recipe 后应加窄规则而不是猜测。

新增 GameTest 覆盖包裹计划 payload/颜色/marker 原子替换、标准 JEI 四种 role、歧义输出、反射概率拒绝、真实 Create Mechanical Crafting 行列选择与包裹展平；总数扩展到 142。上游 GTCEu 7.5.3 runtime 和独立 run-gtceu-fork 中的 StarT Fork 1.7.0b runtime 均完成 142/142；Fork debug.log 确认实际选中 StarT jar，且没有 version differences、missing id 或 unidentified mapping。build、runData 成功；真实 runClient 完成 JEI/Create/GTCEu/Applied Packaging 初始化、资源重载、OpenAL、block atlas 和 JEI GUI atlas 创建后主动终止，日志没有 Applied Packaging 类加载、ERROR/FATAL 或 missing model/texture。发布脚本聚合自测、资产审计、文档审计、verify-release.ps1 -RequireAssetContracts 和 git diff --check 全部通过；JAR 包含全部计划/适配器，mods.toml 没有 JEI/Create/GTCEu 硬依赖。世界内 JEI “+”点击和 tooltip 仍按事实保留为人工验收。

### 2026-07-17 序列缓存器尾部 UV 与方块物品模型回归修正

用户世界截图确认边缘/尾部第三列贴图曾把开口朝向结构外侧。源格实际为 `-V` 封口、`+V` 开口，生成器现把所有边缘/尾部侧面的 `+V` 指向主方块；定向尾部在相对面的 UV 手性冲突处使用仅 V 镜像，使本地方向箭头和结构封口同时正确。水平向东、竖直向上和带方向向东尾部均加入固定资源断言，负例把 east 尾部旋回旧角度后必须失败。

同一轮截图还暴露序列缓存器物品栏外观被正投影成平面方格。原因是自定义 `sequence_buffer/shell.json` 没有继承 Minecraft 的基础方块模型，因而整个 item parent 链缺少 GUI/手持/地面展示变换。外壳现继承 `minecraft:block/block`，物品仍使用未成型六面外观，但 GUI 会取得原版 `[30,225,0]`、`0.625` 等距三维变换。资产审计增加标准父模型断言以及删除 parent 后必须失败的 flat-item 负例。

`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1` 正/负夹具、`scripts/verify-docs.ps1` 和 `scripts/verify-release.ps1 -RequireAssetContracts` 均通过；JAR 内 295 个发布资源与源码一致，141 个 JSON 可解析，158 张 PNG、6 个资产合同有效。本轮仅修改模型 UV/展示继承和资产门禁，不改变方块状态或服务端行为，因此不重复运行 GameTest。游戏中已加载的旧模型需要资源重载或重启客户端后再查看物品栏。

### 2026-07-17 合并终端槽位材质与列头按钮回归修正

用户实机截图确认高级编辑区的横向分隔线和底边被运行时绘制覆盖。根因是 Screen 曾对输入/输出槽内区调用 `GuiGraphics.fill`，这既绕过了 AE2 的槽位材质，也会覆盖用户 atlas 的边界像素。实现现改为 AE2 v19 `Icon.SLOT_BACKGROUND` 路径：只对当前启用的高级输入列，把 states atlas `[192,192,18,18]` 完整精灵绘制到 `(slot.x-1,slot.y-1)`；未启用列不额外绘制，输出槽直接保留 full-screen base 已有材质。列操作和清空按钮上没有上游依据的白色 hover 填充也已删除；Screen 内唯一剩余纯色绘制是逐项对照 AE2 v19 `AEBaseScreen.renderSlotHighlight` 的槽位悬停高亮。

高级列头颜色/清空/循环按钮改为 `bottom=174`，列操作按钮改为 `bottom=173`：两行网络库存时分别占 y=71..78 和 y=72..79，编辑框顶边为 y=80。源图与运行时副本 SHA-256 仍分别一致为 `9586E6422D039A58C1188F5DA4F504FDE04870E4383F29E56FA9FE2752CCDD00`、`65DE82E33052D1F941182863D8303C4D22BA52C07528AC69702B9BA685153096`；states 槽位精灵的 256 个不透明像素与底图首个槽位完全相同。`.\gradlew.bat compileJava --rerun-tasks --stacktrace`、`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1` 和 `scripts/verify-docs.ps1` 通过，`git diff --check` 仅有现存换行提示。真实 `.\gradlew.bat runClient --stacktrace` 完成 Applied Packaging 初始化、资源重载、OpenAL 和全部图集创建；日志无本模组类加载、missing model/texture、ERROR 或 FATAL，随后主动结束客户端。该变更只影响客户端绘制与 ScreenStyle 坐标，不改变菜单、网络或服务端状态，故不重复运行 GameTest；仓库没有自动打开终端的 UI 驱动，最终编辑区像素仍需人工进入世界复核。

### 2026-07-18 序列缓存器 ME Chest 双界面与红石升级

按用户提供的 `seq_buffer_ui.png` 接入序列缓存器主界面，并复用 AE2 `neoforge/v19.2.17` ME Chest 侧面底图。成型端点打开 195x170 terminal 风格界面：端点没有存储槽，存储成员按结构顺序映射到 9 列、3 行窗口，超过 27 个成员才启用整行滚动；不足 27 格和末行多余位置使用同一槽位 sprite 的 0.2 alpha 禁用绘制。成员和未成型单块打开 176x168 侧面界面，中央槽只操作被点击本格。两者均提供 3x3 AEKey 类型过滤假槽和一格升级面板；后续截图复核修正了两者的外围布局与设置呈现，见下节。

序列缓存器实现 `IUpgradeableObject` 并注册一张红石卡上限。红石卡存在时，以端点邻居信号门控全结构自动输出；既有阻挡、同步和输入延迟仍在同一输出计划之前生效。成员携卡成型时先迁移到端点，避免不可见升级库存；重复卡溢出在原位置掉落。过滤假槽、升级库存、菜单显示和物品光标插入/抽取均接入现有端点权威状态，FluidHandler 与 MEStorage 泛型输入路径保持不变。

验证：`.\gradlew.bat runGameTestServer --stacktrace` 最终通过 144/144 required tests；`.\gradlew.bat build --stacktrace` 成功；`scripts/verify-assets.ps1` 与完整 `scripts/test-assets-audit.ps1` 成功。新增 GameTest 覆盖过滤配置/NBT、红石卡上限与 NBT、成型迁移、红石门控输出、侧面存储槽等待输入屏障后的 Shift 抽取和 27/28/36/37 滚动边界。菜单复核期间修正了玩家背包菜单索引与底层容器索引混用；第一次即时抽取测试又准确命中既有至少下一 tick 才能输出的屏障，夹具改为等待屏障而没有放宽行为。两次全量复跑还暴露既有损坏包裹测试会累计持久 GameTest 世界里的旧掉落，夹具改为断言本轮新增 7 个 nether star 和 5 个 dragon breath，最终全绿。GUI 主图保持用户文件字节和 SHA-256，侧图保持固定上游字节；ScreenStyle 坐标、图集尺寸、哈希及主/侧错误坐标负例均进入资产门禁。最终真实 `.\gradlew.bat runClient --stacktrace` 使用最新 class 完成初始化、资源重载、OpenAL、block atlas 与 JEI GUI atlas 创建；日志无 Screen JSON、Sequence Buffer missing model/texture、本模组类加载、ERROR 或 FATAL。`scripts/verify-release.ps1 -RequireAssetContracts` 确认 297 个发布资源、143 个 JSON、160 张 PNG、6 个资产合同和 159 个双语 key，并只忽略 4 条已知第三方 optional-integration 警告。由于项目已删除自动客户端 smoke，不恢复耗时 UI 驱动，世界内最终像素与滚轮/点击交互保留为人工验收。

### 2026-07-18 序列缓存器外围面板与设置工具栏截图修正

用户实机截图确认三项 GUI 偏差：红石升级槽错误地放在左侧，零滚动范围时只剩轨道而没有 disabled handle，3x3 裸假槽被误当作设置呈现。对照项目现有 `me_packager.json` / `package_assembler.json`、`ModernVerticalToolbar`，以及 AE2 v15.4.10 与 neoforge/v19.2.17 的 `UpgradeableScreen`、`PatternProviderScreen`、`InterfaceScreen` 和 `Scrollbar` 后，升级面板改为项目统一的 `{right:2,top:0}`；3x3 allowlist 仍留在左侧，但使用 `package_bus_extra_panels.png [69,62,59,66]` 补全外框；自动输出、阻挡、同步输出、样板模式和输入延迟改为左侧五个 AE2 风格 `IconButton`。工具栏锚到过滤面板外侧，不与假槽重叠。五个设置通过 `GuiSync` 和 client action 修改端点权威配置，输入延迟按 `1/5/10/20/40/100 tick` 循环并支持右键反向。

滚动条错误来自本地 Style 把 disabled sprite X 写成 `7`；同 atlas 的现有高级终端实现和 AE2 `Scrollbar.drawForegroundLayer` 均要求无范围时绘制独立 disabled handle，本界面现改为 `[16,32,7,15]`。资源门禁新增右侧升级面板、带框过滤面板和 disabled handle UV 断言，并增加错误升级侧/错误过滤面板负例。

验证通过：`.\gradlew.bat runGameTestServer --stacktrace --no-configuration-cache` 的 146/146 required tests、`.\gradlew.bat build --stacktrace --no-configuration-cache`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1` 与 `git diff --check`。GameTest 新增五项设置 action 写入端点权威配置及恢复默认值的覆盖。第一次 GameTest 因 EMI Maven 分块响应中断未进入游戏，重试下载成功；一次测试断言误把既有 allowlist 与空配置比较，已收紧为只核对五个设置字段，最终全绿。真实 `runClient` 完成 Applied Packaging/JEI/EMI/Create/GTCEu 初始化、资源重载、OpenAL、block atlas 和 GUI atlas 创建，日志没有 Applied Packaging 类加载、ScreenStyle、missing model/texture、ERROR 或 FATAL，随后只终止本轮客户端进程。仓库仍没有自动打开序列缓存器菜单的驱动，世界内最终像素由人工复核。

### 2026-07-18 高级样板 81x81、逐材料包裹与 JEI/TMRV 单入口

高级样板不再以 17 个物理输出槽限制可编码包裹数。逻辑状态改为最多 81 列、每列 81 个 sparse 输入；新 v2 NBT 把输入保存在列元数据内，仍生成 AE2 执行需要的稠密根 `in`，并可读取既有 v1 展平矩阵。菜单通过 `AdvancedPatternInputWindow` 只同步四个可见列，装配室消费逐列数据并把超过实体槽位的产物放入既有持久 pending 队列。新增转置、移动/交换、末尾自动滚动、默认色/循环色；循环色按 17 色 `PackageColor` 顺序分配。用户提供的转置 sprite 以最近邻像素精确写入本地图集 `[16,0,8,8]`，没有重绘或插值。

配方导入改为材料优先：标准查看器配方、Create 普通 processing 和 GTCEu item/fluid/tick content 都把每个确定性输入放进独立包裹。Create Sequenced Assembly 仍按步骤分列；Mechanical Crafting 仍裁掉空边后比较非空行/列数量，选择包裹数更少的方向。查看器最终收敛为唯一 JEI 插件与 handler：JEI 环境直接加载，EMI 环境使用 EMI+TMRV 在没有 JEI 的情况下映射同一实现；项目不编译 EMI API、不保留原生 EMI handler，也不声明任何查看器发布硬依赖。

新增 GameTest 覆盖 81 列 v2 NBT、v1 兼容、转置、移动/交换、循环色、通用 item/fluid 逐材料列和 GT 多材料逐列。第一次实际运行发现转置坐标断言写错及 recipe 替换未保留重叠列颜色，分别修正夹具和实现后，全量上游 GTCEu 7.5.3 与 StarT Fork 1.7.0b 两套运行时均通过 146/146 required tests。EMI Maven 首次分块响应中断，校验并补全官方 1.1.24 jar 后全部后续命令使用 offline 模式稳定完成。

`.\gradlew.bat compileJava`、`.\gradlew.bat build --offline --no-configuration-cache`、`scripts/verify-assets.ps1`、`scripts/verify-docs.ps1`、完整 `scripts/test-release-self-tests.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 通过。独立 JEI-only 与 EMI+TMRV 客户端都完成资源重载和插件注册；只停止本轮验证实例，未干扰用户从 IntelliJ 启动的客户端。仓库没有终端 UI 自动驱动，因此长按拖移、转置按钮位置、颜色按钮和自动滚动的最终手感/像素仍需人工进入世界验收。

前一轮曾探索原生 EMI handler；用户确认应通过通用兼容层避免双实现后，已删除该 handler 与 EMI compile API，开发 runtime 改为 EMI+TMRV。`-PrecipeViewerRuntime=emi` 的真实客户端命令行确认没有完整 JEI、同时存在 EMI 与 TMRV；隔离 smoke 世界 recipe reload 中，TMRV 列出 `appliedpackaging:jei_plugin`、按 `SYNC_EMI` 分发 transfer handler，并在 17ms 内完成插件加载。JEI-only 客户端也确认同一高级终端 handler 直接注册。首次无完整 JEI 的开发启动暴露 Forge 即使禁用 GameTest namespace 仍会反射发现注解 holder；把直接引用 JEI API 的五个测试体移入无注解 helper 后解决类加载问题。迁移时概率夹具误降为非 public 导致一次 145/146，恢复反射可见性后通过；最终单插件架构下，上游 GTCEu 7.5.3 与 StarT Fork 1.7.0b 均通过 147/147。

Star Technology 调研确认星门部件装配不是独立 Java category API，而是 KubeJS startup script 注册的 GTCEu `stargate_component_assembly` type；server scripts 调用 `layeredRecipe` 保存层级。现有 GT adapter 可读取其平面输入但无法恢复 layer 边界。本轮按用户要求只记录评审项：确认每层一个包裹，或层内仍逐材料分包并新增层边界元数据之后，再决定是否加入 Fork 专用适配或通用 KubeJS 声明接口。

### 2026-07-18 高级样板列经拆包总线写入序列缓存器时保留空位

用户用高级样板“木板 + 空白 + 木板”复现实机问题：序列缓存器开启样板模式后，拆包总线仍把第二个木板写入逻辑第 2 格。根因不在拆包总线提交事务，而在更早的装配室高级样板分包路径：v2 高级样板已在逐列元数据中保存 sparse 行，但 `planAdvancedProcessingPush`、本地装配和容量预检都只把非空输入稠密列表传给 `buildPatternPackage`，输出 `PackageData` 因而没有 `PackageLayout`。既有拆包总线测试又直接手工构造带布局包裹，绕过了这段丢失路径；既有高级样板测试只验证 contents/marker，没有验证布局。

装配室现在从每个 `PackageColumn.inputs()` 恢复非空条目的原行索引，并在本地装配、Pattern Provider push 与容量预检三条路径中把同一 `PackageLayout` 传入包裹计划。序列缓存器端点的包裹入口同时改为真正受样板模式控制：开启且包裹带布局时按 `contentSlots` 跳过空位；关闭或包裹无布局时按 contents 连续写入。高级样板直接推给序列缓存器的既定稠密行为不变，修复只改变“高级样板列 -> 装配室包裹 -> 拆包总线”的位置语义。

GameTest 把装配室高级列用例改为 v2 “木板 + 空白 + 木板”，直接断言输出包裹布局为 `slotCount=3, contentSlots=[0,2]`；拆包总线用例不再手工伪造布局，而是实际编码高级样板、由装配室接收 Pattern Provider 输入生成包裹、通过 ME 网络交给拆包总线，最终断言序列成员 1/3 保存木板而成员 2 为空。另增关闭样板模式时同一布局连续写入成员 1/2 的反向用例。`.\gradlew.bat compileJava compileTestJava --stacktrace`、`.\gradlew.bat build --stacktrace` 和 `scripts/verify-docs.ps1` 成功；`.\gradlew.bat runGameTestServer --stacktrace` 的 147/147 required tests 全部通过；`git diff --check` 通过且仅输出工作树既有 LF/CRLF 提示。

### 2026-07-18 序列缓存器第一版 GUI 范围纠正

用户实机截图确认上一轮新增的左侧 3x3 过滤面板和五按钮设置工具栏不属于第一版 GUI。重新逐行读取 AE2 neoforge/v19.2.17 `MEChestScreen` 与 `me_chest.json` 后确认，上游 ME Chest 主体没有这些外围控件；序列缓存器只额外保留用户明确要求的右侧一格红石卡升级面板和主界面滚动条。两套 ScreenStyle 已删除 `CONFIG` 槽、`inputFilter` 图片/控件，Screen 删除五个自造 `IconButton`，菜单删除对应假槽、`GuiSync` 与 client action。`SequenceBufferConfiguration` 和 `inputFilter` 的方块逻辑及 NBT 仍保留，等待后续明确 GUI 规格再接入。

共享 `ModernUpgradeableScreen` 为此前外挂面板加入的 toolbar 原点扩展也已撤销，避免影响其它机器。资产门禁从“必须存在过滤面板”改为“第一版不得声明预留配置 UI”，并新增向侧面 ScreenStyle 偷加 `CONFIG` 槽的负例；右侧升级面板和 visible disabled scrollbar 断言继续保留。

验证通过：`.\gradlew.bat compileJava processResources --rerun-tasks --stacktrace --no-configuration-cache`、`.\gradlew.bat build --stacktrace --no-configuration-cache`、`scripts/verify-assets.ps1` 和完整 `scripts/test-assets-audit.ps1`。两次全量 GameTest 均确认全部序列缓存器测试通过；完整套件为 145/146，唯一失败是并行配方导入改动中的 `generic_recipe_transfer_rejects_random_output_definitions`，与本次 GUI 撤销无关且未越界修改。GameTest 启动时用户客户端仍占用 `run/logs/latest.log` / `debug.log`，因此本轮不并发启动第二个客户端；现有客户端需资源重载或重启后查看最终界面。

### 2026-07-18 Create 动力合成稀疏前导空位修正

用户截图指出动力合成按行/列拆包后，首个材料之前的空位被错误压缩。根因是 `CreateRecipeTransferAdapter` 在选中行/列内遇到空 ingredient 时直接 `continue`，`AdvancedPatternTransferPlan` 也只允许非空 `GenericStack`，因此原网格坐标无法进入网络 payload 和高级终端状态。适配器现在把选中行/列的前导及内部空位保存为 sparse `null`，仅裁掉最后一个材料后的空位；高级传输计划、JSON payload 与服务端解码同步允许这些空位。包裹页从高级计划展平时仍过滤空位，保持既有“一个普通包裹 contents”的万能回退语义。

真实 `create:mechanical_crafting/extendo_grip` 回归断言左右列均为“空、空、木棍、木棍”，不再是两个从第 0 格开始的稠密材料；通用高级计划测试同时覆盖 sparse 空位经过 SNBT/JSON action payload 往返后仍落在原槽位。`.\gradlew.bat compileJava --no-configuration-cache --stacktrace` 成功；上游 GTCEu 7.5.3 的 `.\gradlew.bat runGameTestServer --offline --no-configuration-cache --stacktrace` 与 StarT Fork 1.7.0b 的隔离运行均通过 148/148 required tests。上游运行开始时用户客户端占用 `run/logs/latest.log` / `debug.log`，Log4j 无法轮换这两个文件，但 GameTest 使用控制台继续完成且全绿；Fork 使用独立 `run-gtceu-fork`，确认实际选中 `gtceu-st-1.20.1-1.7.0b.jar`。

### 2026-07-18 序列缓存器设置恢复与标准滚动条对齐

用户进一步确认只否决此前擅自加入的 3x3 过滤面板，并没有取消自动输出、阻挡、同步输出、样板模式和输入延迟设置。上一轮把“删除过滤面板”错误扩大成了“删除全部设置入口”。本轮恢复 `AbstractSequenceBufferMenu` 的五项 `GuiSync` 与 client action；主/侧菜单都以端点为配置权威，服务端通过既有 `SequenceBufferBlockEntity.updateConfiguration` 同步所有成员。两套 Screen 共享项目现有 AE2 current-style 竖向按钮栏，四个布尔设置使用 AE2 状态图标，输入延迟按 `1/5/10/20/40/100 tick` 循环并支持右键反向。被否决的 3x3 allowlist 假槽与面板没有恢复，精确 AEKey 过滤仍只保留方块逻辑和持久化。

截图中的滚动条同时混用了为 12px 标准 handle 预留的坐标和 7px SMALL handle。逐像素检查用户底图确认轨道外框位于 `x=178..183`；旧 handle 从 `x=175` 绘制时只有 7px 宽，因此整体偏左且显得过小。`SequenceBufferMainScreen` 现直接使用 AE2 `Scrollbar.DEFAULT` 的 12x15 enabled/disabled handle，仍从 `(175,18)` 绘制，左右各覆盖轨道 3px 并精确居中；零范围仍显示 disabled handle。资产合同与审计改为锁定标准 handle 路径，同时继续禁止两套 ScreenStyle 声明 3x3 过滤 UI。

验证通过：`.\gradlew.bat runGameTestServer --stacktrace --no-configuration-cache` 的 148/148 required tests、`.\gradlew.bat build --stacktrace --no-configuration-cache`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1` 和 `scripts/verify-docs.ps1`。新增 GameTest 覆盖五项 GUI 设置写入端点、`GuiSync` 镜像、成员同步以及输入延迟反向循环。GameTest 启动时现有客户端继续占用 `run/logs/latest.log` / `debug.log`，但测试正常完成；为避免干扰用户实例，本轮不再并发启动第二个客户端，代码类变更需重启该开发客户端后进行最终像素复核。

### 2026-07-18 GUI 槽位边框旧版覆盖根因修正

用户提供的 `mepackageassembler.png` 与仓库运行时副本原本就字节一致，边框错误不是 atlas 被替换，而是 `PackageAssemblerMenu.MenuInputDisplaySlot.isRenderDisabled()` 返回 true，使 AE2 15 `AEBaseScreen` 在底图之后又绘制旧 `states.png [192,192,18,18]`。随后 Screen 还以纯色覆盖禁用区，形成 atlas、AE2 基类和自定义 Screen 三个渲染所有者；此前只检查资源存在/尺寸和“槽位可见”的客户端截图，因此同一错误可以反复回归。

实现现把输入槽位所有权收口到项目共用 `ModernSlotRendering`：装配室菜单固定关闭 AE2 15 回退，装配室和 ME Packager 都绘制用户 sprites 中 `[0,64,18,18]` 的 current-AE2 完整槽位，禁用态仅为 0.2 opacity，删除纯色近似层。装配室 7x15 小滚动柄也集中为 `ModernScrollbarStyles.SMALL`，修正 disabled sprite 从错误的 x=7 到 x=16。Sequence Buffer 的 12x15 标准滚动柄是用户底图轨道的已确认几何，未机械替换为 SMALL。

为避免再次靠截图主观放行，`verify-assets.ps1` 新增装配室 atlas SHA-256、共用 sprite UV、装配室/打包机渲染绑定以及全项目禁止 `isRenderDisabled=true` / `Icon.SLOT_BACKGROUND` 的源码门禁；`test-assets-audit.ps1` 新增同尺寸 atlas 替换和恢复旧可选槽位渲染的失败夹具。`.\gradlew.bat compileJava`、`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1` 和 `scripts/verify-release.ps1 -RequireAssetContracts` 全部通过。该变更不修改服务端行为，故不重复运行 GameTest。修改前启动的 IntelliJ 客户端 PID 33220 仍在运行，本轮未终止用户进程或并发启动第二客户端；需重启后加载新 class 并人工打开装配室复核最终像素。

### 2026-07-18 配方导入现存材料选择对齐原版样板终端

用户指出当前高级终端配方导入的材料选择与 AE2 样板终端不同：标准 JEI 适配固定采用当前显示候选，Create/GTCEu 专用适配固定采用声明首项，即使终端网络中已经存在其它合法替代材料也不会选择。对照固定 AE2 15.4.10 源码 `EncodePatternTransferHandler`、`EncodingHelper` 与 `GridInventoryEntry` 后，新增共享 `RecipeIngredientSelector`。handler 从当前菜单的 client repo 构造一次优先级快照，严格保持网络高于玩家物品栏、网络内 craftable/undamaged/stored amount 递增优先；完全不存在时才使用展示项/声明首项。原生物品 Ingredient 还枚举 client repo 中所有 `AEItemKey.matches` 变体，从而不会把当前已有的 NBT/损伤变体排除在声明栈数组之外。

标准 JEI item/fluid INPUT、Create Sequenced Assembly/Mechanical Crafting/ProcessingRecipe 输入和 GTCEu 一次性/tick item/fluid 输入都已接到该选择器。GTCEu 输出继续使用 recipe 声明结果；标准多候选输出仍拒绝，未把输入替代物选择错误扩大为输出动画帧选择。`RecipeStackConversions` 新增完整 item/fluid 候选转换，旧 first 方法保留为明确回退。现有可选集成 GameTest 增加四类断言：网络现存 item/fluid 覆盖 JEI 第一显示项、同类网络条目取存量更多者、玩家物品栏候选优先于完全不存在的声明首项、Create/GTCEu 使用的 raw Ingredient 走同一选择规则。

验证：`.\gradlew.bat compileJava --stacktrace` 与 `.\gradlew.bat build --offline --no-configuration-cache --stacktrace` 成功；上游 GTCEu 7.5.3 的 `.\gradlew.bat runGameTestServer --offline --no-configuration-cache --stacktrace` 与 StarT Fork 1.7.0b 的 `.\gradlew.bat runGameTestServer "-PgtceuRuntimeJar=build/reference/compat-src/gtceu-st-1.20.1-1.7.0b.jar" --offline --no-configuration-cache --stacktrace` 均通过 148/148 required tests。`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 和完整 `scripts/test-release-self-tests.ps1` 通过；机械审计确认 297 个发布资源、143 个 JSON、160 张 PNG、6 个资产合同和 179 个双语 key/占位符有效。Fork 使用独立 `run-gtceu-fork` 并确认实际选中 1.7.0b；上游日志轮换仍被用户现有客户端占用，但控制台测试完整通过，本轮没有终止该客户端。GameTest 已按行为敏感变更扩展并真实执行，未把 build-only 作为行为证明。

### 2026-07-18 包裹装配室阻挡模式与单 tick 连续输出

包裹装配室新增独立 `blockingMode`，默认关闭并写入方块实体 NBT。菜单通过独立 DataSlot 和 client action 同步，界面在既有输出模式按钮后增加 AE2 `BLOCKING_MODE_YES/NO` 图标按钮及双语状态提示。开启后，自动输出开始前检查当前 `outputMode` 选中的直接目标：ME 网络要求可见存储为空，相邻方块要求 item handler 所有槽位为空；非空时主输出和 `pendingPackages` 原样等待。

连续输出以一次 drain 为事务边界。目标为空并允许启动后，同一 tick 内反复提升严格队首并提交，覆盖真实主输出与 `pendingPackages`，直到已完成批次清空或目标拒收；阻挡条件不在每个包裹后重新读取，避免第一包写入后被自身造成的“目标非空”卡住。玩家与外部 capability 的主动队首抽取不受该自动输出门禁影响。

新增 `packageAssemblerBlockingWaitsThenDrainsCompletedBatchInOneTick` GameTest：真实 Chest 预放金锭时，两列高级样板完成后的两个包裹均留在装配室；清空 Chest 后只调用一次 `serverTick()`，两个不同颜色/marker 包裹全部进入目标，主输出和 pending 队列同时归零。`packageAssemblerBlockingWaitsForEmptyAeNetwork` 使用真实 Drive、64k cell 与 Interface，确认网络已有铁锭时保留包裹，清空网络后下一 tick 才导出。菜单切换测试同步覆盖默认关闭、服务端按钮切换与客户端镜像，既有 NBT 测试增加阻挡状态往返。`.\gradlew.bat compileJava compileTestJava --stacktrace --no-configuration-cache`、`.\gradlew.bat runGameTestServer --stacktrace --no-configuration-cache`（150/150 required）、`.\gradlew.bat build --stacktrace --no-configuration-cache`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 均通过。GameTest 已覆盖服务端语义；本轮没有启动第二个客户端或干扰现有开发实例，新增按钮复用已在用的 AE2 toolbar/IconButton 路径。

### 2026-07-18 包裹装配室比较器计数与自动输出脉冲

先核对 ModDevGradle 为 Forge 47.4.10 / Minecraft 1.20.1 解析出的本地源码：方块可通过 `hasAnalogOutputSignal/getAnalogOutputSignal` 提供模拟输出，`Level.updateNeighbourForOutputSignal` 会通知相邻比较器，而 `ComparatorBlock.checkTickOnNeighbor` 在输入变化后以 2 game tick 延迟重新采样。因此装配室可以原生支持比较器，但若产出与自动导出发生在同一 server tick，仅在槽变化时即时通知仍会让比较器采样到 0。

`PackageAssemblerBlock` 现声明模拟输出支持。方块实体把真实输出槽和已完成 `pendingPackages` 中的包裹数相加，信号钳制为 0-15；输出槽或队列变化时通知比较器。新成品提交同时设置 4 server tick 的运行时锁存，实际缓存已被同 tick 自动输出清空时仍返回最小强度 1；锁存归零后再次通知比较器，使其完成一次高低脉冲。锁存不改变物品、队列、阻挡模式或 NBT，只补足比较器的采样窗口。

新增 `packageAssemblerComparatorReportsCompletedPackageCount`，覆盖原版模拟输出声明、3 个包裹输出强度 3、17 个包裹钳制为 15 和清空回到 0；既有两包阻挡用例增加主输出加 pending 队列等于强度 2 的断言。`packageAssemblerAutoExportProducesComparatorPulse` 放置真实 Chest、Creative Energy Cell、Package Assembler 和原版 Comparator，等待自然世界 tick 完成装配与自动导出，确认装配室缓存已归零后比较器仍实际经历 `0 -> 1 -> 0`。第一次测试错误地在同一游戏 tick 内手动调用 12 次 `serverTick()`，把按 tick 设计的锁存也一次耗尽；改为只依赖自然 tick 后，`.\gradlew.bat runGameTestServer --stacktrace --no-configuration-cache` 的 152/152 required tests 全部通过。`.\gradlew.bat compileJava --stacktrace --no-configuration-cache`、`.\gradlew.bat build --stacktrace --no-configuration-cache`、`scripts/verify-docs.ps1` 与 `git diff --check` 同样通过。GameTest 启动时用户现有客户端占用 `run/logs/latest.log` / `debug.log`，但控制台测试完整完成，本轮没有终止或干扰该客户端。

### 2026-07-18 比较器虚拟锁存风险复核与真实输出驻留

用户指出比较器锁存存在风险。复核确认该担忧成立：锁存让 `getAnalogOutputSignal` 在真实输出已空时仍返回历史强度 1，使比较器读数不再严格等于输出缓存；它还引入区块卸载、测试直接调用 `serverTick()` 和后续输出重构时必须共同维护的隐藏状态。实现因此撤销虚拟信号锁存，比较器返回值重新收口为真实输出槽与已完成 pending 队列中的当前包裹数。

自动输出改为真实成品驻留采样窗口：新批次提交后先把真实包裹保留在输出缓存 3 个 server tick，原版比较器在自身 2 game tick 延迟后读取实际数量；窗口结束后再按原有语义在同一 tick 内连续导出整个允许批次，输出清空的邻居通知使比较器回落。延迟计时不写 NBT；若区块加载时已有持久化成品，首个 server tick 会重新建立完整采样窗口，因此不会因卸载丢失待观察的真实包裹。阻挡模式、输出目标和批次内连续 drain 规则不变。

真实比较器 GameTest 改为依次断言 Chest 仍空且装配室真实缓存为 1、Comparator 实际升到 1、采样后 Chest 收到包裹且缓存归零、Comparator 最终回到 0。另增 NBT 重载用例，确认带成品装配室加载后的首个 server tick 仍保留真实输出，重新走完 3 tick 采样窗口后才导出。既有直接 tick helper 从 12 次扩为 16 次，以覆盖新增的 3 tick 自动输出等待；它只用于既有确定性测试，不再承担比较器自然时序证明。

`.\gradlew.bat runGameTestServer --stacktrace --no-configuration-cache` 的 153/153 required tests 与 `.\gradlew.bat build --stacktrace --no-configuration-cache` 均通过；`scripts/verify-docs.ps1` 和 `git diff --check` 同样通过。新增的重载边界用例与既有相邻输出、AE 网络输出、阻挡模式和批次连续 drain 用例共同确认：撤销锁存后，比较器只读真实包裹数量，原有输出行为保持成立。

### 2026-07-18 比较器逐次脉冲探索与三态计划刻方案

用户进一步指出，为比较器延迟自动物流同样不合理，并把验收口径收紧为“比较器脉冲数量必须等于打包次数”。临时 GameTest 先验证 `打包中=1、完成=2、清空=0` 状态编码：最快连续 6 次打包只产生 3 个比较器上升沿，证明原版比较器会把相邻 2 tick 内的状态变化合并，该编码不能逐次计数。另一临时探针在真实输出提交前为朝向装配室的直连/隔实体方块原版比较器预约 0-delay 计划刻，6 次完成分别得到 6/6 个上升沿；探针完成后已撤销。

正式实现据此移除 3 tick 自动输出驻留及其运行时字段，并落实用户指定的三态语义：空闲或成品已清空为 0，正在打包为 1，存在未清空完成成品为 2；数值不再表示包裹数量，高级样板同批多个包裹仍为 2。开始打包、成功提交和自动输出清空三个边界都会用单方块 `BoundingBox` 清除目标比较器尚未执行的旧计划刻，再预约 HIGH 优先级 0-delay 计划刻，避免原版 2 tick 合并吞掉任一状态；范围只包含输入面确实朝向装配室的原版比较器。若完成列表被清空，该 tick 不启动下一次装配，下一 tick 才开始，从而形成逐次可采样的 `0 -> 1 -> 2 -> 0` 周期。

正式连续测试最初把 6 个材料一次投入只要求 1 个材料的精确样板槽，实际过滤规则只接收 1 个，因此首轮超时属于 fixture 错误而不是脉冲丢失。改为每次输入槽释放后再补入下一个材料后，`packageAssemblerContinuousAutoExportPulseCountMatchesCraftCount` 用 6 次独立真实装配确认：直连比较器与隔一个红石导体的比较器都只出现 0/1/2，分别得到恰好 6 个 `0 -> 1` 上升沿和 6 次状态 2，Chest 同时收到 6 个包裹。`packageAssemblerLoadedOutputExportsWithoutComparatorDelay` 另确认载入成品在首个 server tick 立即导出且保持状态 0，第二个 tick 才开始下一次装配并进入状态 1。纠正此前误把初始“按包裹数输出”带回正式实现的问题后，`.\gradlew.bat runGameTestServer --stacktrace --no-configuration-cache` 的 156/156 required GameTest、`.\gradlew.bat build --stacktrace --no-configuration-cache`、`scripts/verify-docs.ps1` 与 `git diff --check` 全部通过。

### 2026-07-18 包裹装配室输出批次对齐 Pattern Provider

用户复现实机多份高级样板时第二份材料路由错位，并明确装配室输出只需复用 Pattern Provider 的批次准入逻辑，不应复制其 `sendList`：装配室成品仍是 GUI 和外部 capability 可随时抽取的真实有序列表；ME 模式直接写本机节点所属网格的 MEStorage；容器模式也没有“背面”概念。

自动输出新增持久化的批次准入状态。每批首次输出前，从真实输出槽与 `pendingPackages` 收集包裹物品类型并执行 `dropSecondary()`；阻挡模式只在目标已有匹配包裹类型时拒绝，无关物品不阻挡。随后逐项模拟目标能否接受全部当前成品，准入后在同一 tick 尽量清空；容量或规则中途拒收时保留真实列表、目标模式和容器方向，后续 tick 继续而不重查阻挡或模拟。GUI 在活动批次中取走队首不会复制或隐藏物品，剩余列表继续属于该批；列表取空后清除状态，下一完成批次重新准入。ME 模式不经相邻 Interface 的 item capability，Interface 只作为接网方式；容器模式按稳定方向顺序扫描六个相邻 item handler，选择首个可准入目标并在本批内锁定该方向。

既有阻挡测试改为同时保留无关金锭和匹配红色包裹，确认只移除匹配包裹即可在一个 tick 内导出两列。新增容量受限 Chest 回归：三份同色不同 marker 包裹的整批模拟均可单独通过，但目标只实际容纳第一包；GUI 取出第二包后，下一 tick 在第一包仍位于目标时成功输出第三包，证明同一批不重查阻挡；再次投入材料生成的下一批则完整等待，证明批次边界会重新检查。相邻输出测试把 Chest 放在顶面，真实 Drive + Interface 测试同时保留无关铁锭和匹配 Fluix 包裹，覆盖六面容器与直接 MEStorage 两条目标路径。机械发布审计的旧字段检查同步收窄为只拒绝遗留 `auto_export`，允许当前 `auto_export_batch_*` 批次字段，并以正反 fixture 固化。最终 `.\gradlew.bat runGameTestServer --stacktrace --no-configuration-cache` 的 155/155 required GameTest、`.\gradlew.bat build --stacktrace --no-configuration-cache`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts`、`scripts/test-release-audit.ps1` 与 `git diff --check` 全部通过。

### 2026-07-18 拆包总线报告并允许取回 held 包裹

用户明确拆包总线正在处理的包裹仍应作为包裹本身报告给 ME，且在内容尚未提交前允许网络或玩家中途取回；这样装配室的 Pattern Provider 式阻挡才能识别网络中正在拆的同类包裹。实现保留 Formation Plane 式受限输入：总线仍不扫描或主动抽取其它网络存储，也不接收无法立即进入拆包流程的任意包裹作为一般库存。唯一变化是挂载的 `MEStorage` 在 `heldPackage` 非空时以精确 `AEItemKey`、数量 1 枚举该整包并允许模拟/真实抽取，仍不暴露包裹内部内容。

网络真实抽取或 GUI 工作槽取回不再受 working 状态限制。两条路径都返回完整的一个包裹并原子清空 working、blocked、进度、周期总长和 retry cooldown；服务器单线程下后续工作 tick 因 held 已空不会提交任何内容。held 接收、提交或取回继续调用 `IStorageProvider.requestUpdate`，使可见库存及时刷新。菜单 `HeldPackageSlot` 同步允许工作中拾取，仍拒绝玩家向工作槽放入任意物品。

既有真实总线测试改为断言工作中和阻塞中的 held 包裹均可由 ME 枚举/模拟抽取。新增 `packageAssemblerBlockingSeesExtractablePackageHeldByUnpackingBus`：在同一真实 Drive + Interface 网格中让拆包总线持有匹配包裹，确认装配室输出被阻挡；ME 取走后装配室立即输出下一包并由拆包总线接收；再从 GUI 后端工作槽中途取走，目标 Chest 仍没有任何部分内容。`.\gradlew.bat compileJava --stacktrace --no-configuration-cache`、`.\gradlew.bat runGameTestServer --stacktrace --no-configuration-cache`（156/156 required）、`.\gradlew.bat build --stacktrace --no-configuration-cache`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。

### 2026-07-18 序列缓存器端点统一 tick 与同 tick 输入标记

用户确认采用序列缓存器侧的确定时相方案：结构成型后成员方块实体不再各自执行 server tick，唯一端点每 tick 统一代理全部成员的空槽标记维护、普通自动输出和同步输出。这样同一结构不会因成员方块实体在装配室前后分散 tick 而只提前清空部分槽位。未成型单块仍保留自己的 tick；成型成员的 capability 输入/抽取仍直接操作真实本格内容，不建立隐藏库存。

每次真实输入现在同时设置瞬时 `inputAdmissionLocked`，模拟输入不设置。内容在同 tick 被 0 tick 延迟自动输出或外部抽取清空后，该标记继续阻止再次输入；端点在后续 tick 统一观察所有空成员并清除旧标记。标记不写 NBT，载入非空内容时从真实内容恢复运行态锁定。输入延迟下限改为 0，GUI 预设为 `0/1/5/10/20/40/100 tick`；0 只取消输出等待，不绕过输入标记。诊断日志的成员快照同步输出 `inputLocked`。

新增 `formedEndpointOwnsMemberTicksAndPreventsSameTickReentry`，直接确认成员 tick 不输出、一次端点 tick 同时代理所有普通输出、同 tick 所有已清空成员拒绝输入且后续端点 tick 一起开放；既有单格锁存测试改为覆盖抽空同 tick 拒绝和下一 tick 开放，菜单测试覆盖 0 tick 预设。第一次完整运行仅旧“抽空立即开放”断言失败，按新需求修正后 `.\gradlew.bat runGameTestServer --stacktrace --no-configuration-cache` 的 157/157 required GameTest 全部通过。`.\gradlew.bat compileJava --stacktrace --no-configuration-cache`、`.\gradlew.bat build --stacktrace --no-configuration-cache`、`scripts/verify-docs.ps1` 与 `git diff --check` 通过。GameTest 启动时用户现有客户端继续占用 `run/logs/latest.log` / `debug.log`，只产生日志轮换警告，控制台结果完整，本轮未停止用户客户端。

### 2026-07-18 序列缓存器按 game time 开放与 GUI 抽取分离

后续实机复测确认，上一轮的瞬时 `inputAdmissionLocked` 仍把正确性绑定到端点 tick 的执行阶段：多个成员即使在同一个 game tick 清空，若下一轮输入发生在端点重置 bool 之前，就仍会看到不一致的空槽可用集合。按最终需求，本轮撤销“输入时设 bool、后续权威 tick 观察并清除”的模型。每个成员现在只在内容于 game time `t` 从正数归零时记录持久化的绝对 `admissionOpenAtGameTime=t+1`；所有 item/fluid/ME、样板和拆包原子输入预检都按查询时的世界时间判断，因此清空 tick 的剩余阶段始终拒绝，而下一 game tick 无需端点或成员先 tick 即自动开放。成型成员仍不独立 tick，端点仍是普通/同步自动输出的唯一代理；拆包总线没有增加额外 cooldown。

玩家 GUI 抽取同时从输出延迟门禁中分离。外部 `IItemHandler`、`IFluidHandler`、`MEStorage` 抽取和自动输出继续受全结构 `releaseAtGameTime` 限制；主/侧菜单使用的 `extractMenuItem` 只绕过该输出延迟，不检查阻挡或同步输出设置，并继续操作同一份真实列表/单格缓存。GUI 取走最后一份内容仍走统一清空路径，所以同 tick 输入门禁没有被绕过，下一 tick 才允许再次输入。

新增 `menuExtractionBypassesOutputDelayAndKeepsAdmissionCooldown`，在自动输出关闭、阻挡开启、同步开启和 40 tick 输出延迟下确认外部抽取返回空、GUI 模拟与真实抽取立即成功、清空同 tick 拒绝输入、下一 tick 无需方块 tick 即开放，并确认绝对开放时间经 NBT 往返。`formedEndpointOwnsMemberTicksAndPreventsSameTickReentry` 去掉后续手动端点 tick，直接固定查询时开放语义。`.\gradlew.bat compileJava --stacktrace --no-configuration-cache`、`.\gradlew.bat runGameTestServer --stacktrace --no-configuration-cache`（158/158 required）、`.\gradlew.bat build --stacktrace --no-configuration-cache`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。

### 2026-07-18 防堵塞输入模式与 AE2 GuideME 文档补齐

ME 打包机、包裹卸货总线和序列缓存器统一增加“防堵塞输入”配置，但保留各设备真实输出规则。ME 打包机默认开启：内部存在未输出成品时拒绝新输入；若同时开启阻挡模式且当前目标不满足输出准入，也视为“不能输出”，因此继续拒绝输入。包裹卸货总线默认开启：只要仍持有正在拆解、等待重试或可被网络/GUI 取回的完整包裹，就拒绝下一包。序列缓存器默认关闭；开启后，本格非空或仍处于 `admissionOpenAtGameTime` 同 tick 冷却时拒绝输入。三者都只约束自动化输入，不禁止玩家从真实 GUI 库存取走内容，也不把内容转移到隐藏队列。该行为及其 NBT、菜单同步、阻挡组合和下一 game tick 重开边界已作为 `6053bdc feat: add anti-clog input admission modes` 单独提交；完整 `runGameTestServer` 为 161/161 required tests。

按照 AE2 15.4.10 的原生 GuideME 组织方式，在 `assets/appliedpackaging/ae2guide/` 新增 10 个英文页面及一一对应的 10 个简体中文页面。内容不是按钮提示的扩写，而是可独立阅读的使用手册：总览、完整工作流、包裹与样板、高级样板编码终端、ME 包裹装配室、ME 打包机、两类包裹总线、序列缓存器、示例布局和故障排查。设备页写明容量、模式、默认值、阻挡与防堵塞的组合、GUI/外部抽取边界、批次或 tick 时相，并用 `item_ids` 让 AE2 自带的唯一问号按钮直接定位到对应页面；没有为 Screen 再添加第二个帮助按钮。

示例页提供三份可旋转的 `GameScene` 结构：Pattern Provider → ME 包裹装配室 → 容器的装配线、包裹存储总线与包裹卸货总线的路由对照、序列缓存器成型队列。场景旁逐项解释方向、目标、优先级、阻挡/防堵塞组合和适用目的；另有终端与总线单设备结构，共计 6 份 SNBT。故障排查按“输入被拒绝、输出等待、包裹路由错误、序列顺序异常、装配室不工作”组织检查顺序。本轮明确范围不包含 Ponder，现有 GuideME 场景承担机器与布局示例；Ponder 作为独立后续需求处理。

文档门禁同步扩展：`verify-docs.ps1` 现在检查 20 个双语页面、6 份结构、英文/中文页面集合一致、导航元数据、26 个物品/方块索引映射、分类索引、最少 `BlockImage` / `GameScene` / `RecipeFor` 数量、结构引用和本地 Markdown 链接；`test-docs-audit.ps1` 新增缺失中文页面和损坏结构引用两个负例。`runData build`、`verify-assets.ps1`、`verify-release.ps1 -RequireAssetContracts`、`verify-docs.ps1`、完整 `test-docs-audit.ps1` 与 `git diff --check` 全部通过，构建 JAR 已确认包含完整 `ae2guide/` 资源。

真实客户端验证使用项目现有 `run-gtceu-fork` 隔离目录，不触碰用户正在运行的 PID 33464。通过 GuideME 20.1.7 的开发源映射、`guideme.validateAtStartup=ae2:guide` 和 `guideme.showOnStartup=ae2:guide!appliedpackaging:index.md`，分别以 `en_us`、`zh_cn` 启动：每次都加载对应语言的 10 个页面，逐页编译全部页面及配方/场景标签，并实际渲染出 AE2 导航树中的 Applied Packaging / 应用封装分类和首页；日志没有 GuideME、Applied Packaging、missing model/texture、ERROR 或 FATAL。启动日志仍有既知 PonderJS、Xaero、KubeJS、ModernFix 可选集成缺类警告，与本轮 Guide 资源无关。

### 2026-07-18 新版工具栏图标与 GUI 底图对齐

按用户提供的新版 sprite 替换工具栏图标：`package-storagebus-sprites.png` 的 `(0,96)` 起始区域现依次包含防堵塞模式开/关、同步输出开/关、样板同步开/关、输入延迟、仅显示物品和仅显示流体，并以实时状态 sprite provider 接入序列缓存器、ME 打包机、包裹总线和终端类型过滤按钮。资源当前 SHA-256 为 `1E5A223CBBE07D14CE9A97389596E188C668B4A44F0011EA8AA64D9E99EC3EC6`。

高级样板编码终端底图与包裹装配室底图均以用户修正版原图逐字节替换，SHA-256 分别为 `AEDD18C31813DC23287EF0C53FF57274672AFCA803CF7CBA755AC757B360062A` 和 `C96749C3F8EF43DDB63B5F2F6A1E4B769319F52B9964ACD0AEAC7053481B5F33`。高级终端在通用底图之后、动态槽位之前覆盖绘制高级编辑区，保留新的灰色区域底色。包裹装配室只将四行 4x4 输入槽的 `left` 从 20 右移到 21，`top=33/51/69/87` 不变；滚动条、样板、容量升级与输出槽不移动。颜色按钮按新底图设为 `(95,29)`，marker 内容槽设为 `(108,32)`。

`scripts/verify-assets.ps1` 和 `scripts/test-assets-audit.ps1` 已固化三份 PNG 哈希、图标坐标、高级面板绘制顺序及装配室槽位坐标，正向审计与所有新负例均通过。`compileJava`、`build`、`verify-assets.ps1`、`test-assets-audit.ps1`、`verify-docs.ps1` 与 `git diff --check` 通过。隔离客户端完成资源重载和全部 texture atlas 创建，日志中没有 Applied Packaging 缺失材质/模型、Screen JSON、ERROR 或 FATAL。本轮仅修改客户端资源、绘制与布局坐标，未改动菜单或服务端行为，因此未重跑 GameTest；项目暂无自动打开这些 GUI 的 client smoke 任务，实机按钮点击与像素级观感仍作为最后人工验收项。

### 2026-07-19 包裹装配室自动输出改为队首单包裹预检

移除自动输出准入阶段对整个已完成包裹列表的目标容量预检。新批次仍先执行一次阻挡检查，但目标可接收性只模拟当前真实队首包裹；队首可完整输入即锁定该批次及目标。之后沿同一真实有序列表连续提交，后续当前队首被目标拒收时停止并保留剩余成品，后续 tick 继续时不重新检查阻挡。Pattern Provider 输入侧的整批容量预检没有改变。

回归测试 `packageAssemblerPreflightsOnlyHeadAndRetriesWithoutRecheckingBlocking` 使用几乎填满的相邻箱子：仅第一包能与既有同数据包裹合并，第二包无空间。测试确认第一包仍会输出、余下两包保持真实队列且允许 GUI 抽取；释放空间并在中途开启阻挡后，活动批次仍继续；下一批则重新执行阻挡并等待。`compileJava` 通过，完整 `runGameTestServer` 为 165/165 required tests；`scripts/verify-docs.ps1` 与 `git diff --check` 通过，后者仅输出工作树既有 LF/CRLF 提示。

### 2026-07-19 序列缓存器配置权威、装配室临时颜色与新版搜索框

序列缓存器的六项配置控件现在只在成型端点主界面创建。成员与未成型单块的侧界面不注册配置 action，也在服务端拒绝对应方法调用；成型成员只显示使用 current-AE2 `Icon.ENTER [112,0]` 的“打开主方块”按钮，服务端重新解析当前拓扑后才切换菜单，未成型单块不显示该按钮。样板模式、同步输出和输入延迟明确为多方块专属：未成型单块即使旧 NBT 中仍有这些值也不应用输入延迟；成型后所有成员实时读取端点配置，成员自身保存的配置既不复制到端点、也不被端点覆盖，脱离结构后仍保留自己的本地值。

包裹装配室把持久化的手动颜色与样板/Pattern Provider 当前工作的有效颜色完全分离。包裹样板、高级样板或活动工作包裹只覆盖显示与打包读取，不调用 `setSelectedColor`；样板取出或任务结束后立即恢复原手动颜色。上述临时覆盖期间颜色按钮为不可用状态，客户端不发送 action，服务端也独立拒绝伪造的颜色修改。颜色选择弹窗打开时，装配室、ME 打包机、包裹总线和高级终端都以屏幕外鼠标坐标执行底层 Screen 渲染，避免被弹窗遮住的槽位继续产生 hover 高亮或 tooltip。

AE2 15 的 `AETextField` 把搜索框材质固定为 `ae2:textures/guis/text_field.png`，因此仅替换本模组终端底图不会改变控件。项目现以同一资源位置缓存 current-AE2 的 128x128 原图，SHA-256 为 `73BBA41174D3EC15D83947E439915873611735FE436AD0CBC7653ECA15E23AD1`，并在许可证说明、资产规格、尺寸门禁、源哈希门禁及负例自测中固定来源与完整性。构建 JAR 已确认包含该 487-byte 条目及相同哈希。

行为敏感回归位于主 source set 的 `src/main/java/.../gametest/`。`.\gradlew.bat runGameTestServer` 明确完成 166/166 required GameTest，覆盖未成型单块忽略多方块专属设置、侧界面不能越权修改、成型成员实时读取端点且不复制本地配置、样板颜色临时覆盖及服务端拒绝修改。`.\gradlew.bat build`、`scripts/verify-assets.ps1`（145 张 PNG）、完整 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。真实 `.\gradlew.bat runClient --stacktrace --no-configuration-cache` 完成 Applied Packaging 初始化、资源重载、OpenAL 和全部图集创建后由本轮主动停止；日志只有既有第三方可选集成警告及用户 IntelliJ 客户端占用 `latest.log/debug.log` 造成的轮换警告，没有 Applied Packaging 类加载或资源错误。本轮未停止用户的 IntelliJ 开发客户端 PID 43708；实际打开各界面的像素与点击手感仍需人工验收。

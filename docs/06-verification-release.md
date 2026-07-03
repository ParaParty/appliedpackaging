# 验证与发布

## 1. JVM 测试

必须覆盖：

```text
PackageData canonical hash 稳定
同内容不同顺序 hash 相同
不同颜色 hash 不同
marker 冲突拒绝
容量单位计算正确
过滤 AND 规则正确
```

## 2. GameTest

必须覆盖：

```text
合法包裹 NBT 可读写
无 PackageData 的包裹被拒绝
打包事务成功提交
输出槽满时打包不改变源库存
拆包目标容量不足时不消耗包裹
包裹堆叠拆包只接受完整数量
过滤不匹配拒绝
容量不足拒绝
```

当前已覆盖：

```text
PackageData 合法 NBT 可读写
无 PackageData 的包裹被拒绝
同内容不同顺序的包裹生成相同 canonical hash，并写入可堆叠的相同 NBT
颜色、marker 或内容不同的包裹生成不同 canonical hash
canonical hash 被篡改时拒绝
缺失 canonical hash 时拒绝
schema version 不支持时拒绝
空过滤接受合法包裹
颜色不匹配时过滤拒绝
颜色/marker/内容同时匹配时过滤接受
内容数量不足时过滤拒绝
包裹计划会展开源包裹内容
retain marker 冲突时计划拒绝
override marker 时计划使用覆盖 marker
clear marker 时计划清除 marker
默认容量超限时计划拒绝
item handler 打包计划可模拟并提交抽取
item handler 拆包可完整插入目标
目标满且不兼容时拆包拒绝
默认容量下超量源库存只规划可承载的最大包裹
装配室可从输入缓冲生成包裹
装配室输出阻挡时不消耗输入
装配室输入包裹会展开后再封装
装配室可使用已编码 package_pattern 精确匹配输入计划
装配室可使用已编码 package_pattern 重封装大于默认容量的源包裹
装配室可用容量槽重封装超过默认容量的源包裹
装配室可使用 packaged_processing_pattern 逐包生成有序处理包裹
装配室可接受 AE2 Pattern Provider pushPattern 的物品输入
装配室可接受 AE2 Pattern Provider pushPattern 的流体输入
装配室普通 Pattern Provider pushPattern 可用容量槽承载超过 9 个物品栈的输入
装配室默认容量下拒绝超量 Pattern Provider pushPattern 且不消耗输入
装配室可读取 AE2 encoded processing pattern 彩色输入槽元数据并拆成不同颜色包裹
装配室彩色 pushPattern 在同 AEKey 被输入持有者汇总时仍按 sparse input 槽位拆分
装配室彩色 Pattern Provider pushPattern 可用容量槽承载超过默认容量的输入
装配室彩色 Pattern Provider pushPattern 可封装流体 AEKey 输入
装配室彩色 pushPattern 可通过 pending queue 在输出槽清空后继续输出后续包裹
装配室可读取 AE2 encoded processing pattern 承载的 packaged_processing_pattern NBT 并按 packages[] 逐包输出
装配室可读取带流体包裹内容的 packaged_processing_pattern NBT 并按 packages[] 逐包输出
装配室输出阻挡时拒绝 Pattern Provider pushPattern 且不消耗输入
装配室本地样板槽兼容路径仍拒绝无法转入 9 格物品缓冲的非物品输入且不消耗输入
装配室可把旧 11 槽库存 NBT 迁移为当前 12 槽库存并补空容量槽
装配室自动导出开关默认开启，可通过菜单按钮切换并同步到 menu state
装配室自动导出设置可保存/读取
装配室 server tick 可把输出包裹导出到相邻 Forge item handler
真实 AE2 Interface 网络可接收装配室自动导出的包裹物品
真实 AE2 Creative Energy Cell + Pattern Provider 方块网络可推送处理样板输入到装配室
真实 AE2 Creative Energy Cell + Pattern Provider 方块网络可推送彩色处理样板输入到装配室
真实 AE2 Creative Energy Cell + Pattern Provider 方块网络可解码并推送带 packaged_processing_pattern NBT 的 AE2 encoded processing pattern
真实 AE2 Drive + 64k item cell + Crafting CPU + Pattern Provider 方块网络可提交自动合成 job，并把 processing pattern 输入推送到装配室
AE2 PackageItemStorage 只暴露合法包裹
AE2 PackageItemStorage 拒绝散装物品插入
AE2 PackageItemStorage 可模拟并提交合法包裹插入/抽取
AE2 PackageItemStorage 按 PackageFilter 限制可见、插入、抽取包裹
包裹总线可保存、拒绝非法项并清除 ghost 过滤模板
Package Bus 配置 UI 可从光标物品设置 ghost filter 且不消耗光标模板
Package Bus 配置 UI 可从玩家背包 shift-click 设置 ghost filter 且不消耗背包模板
package_pattern 数据可读写
package_pattern 数据可在 AE2 原版 blank_pattern 上读写
packaged_processing_pattern 多包裹数据可读写
packaged_processing_pattern 处理输出 outputs[] 可读写
packaged_processing_pattern 数据可在 AE2 原版 blank_pattern 上读写
colored_processing_pattern 输入槽颜色数据可读写，并可读取 AE2 sparse processing inputs
package_pattern_terminal 可从预览输入编码 package_pattern
package_pattern_terminal 可把 AE2 原版 blank_pattern 编码为 package_pattern 载体并保留 AE2 物品类型
package_pattern_terminal 可把带处理输出 ghost 的 AE2 原版 blank_pattern 编码为 AE2 encoded processing pattern，并附带 packaged_processing_pattern NBT
package_pattern_terminal 可用 selectedColor 编码非默认颜色样板
package_pattern_terminal 可把 marker 槽物品编码为样板 marker
package_pattern_terminal 可用容量槽编码超过默认容量的样板
package_pattern_terminal 可把 selectedColor 写入 AE2 encoded processing pattern 的所有非空输入槽
package_pattern_terminal 可把逐槽配置的颜色写入 AE2 encoded processing pattern
package_pattern_terminal 可编码空白 packaged_processing_pattern 并保留物品类型
package_pattern_terminal 可把 packaged_processing_pattern 拆成多包裹计划
package_pattern_terminal 配置 UI 可从光标设置 packaged_processing_pattern 处理输出 ghost slot，编码时写入 outputs[] 且不消耗光标物品
package_pattern_terminal 配置 UI 可从 Forge 流体容器设置处理输出 ghost slot，编码时写入 AEFluidKey 输出且不消耗光标容器
package_pattern_terminal 处理输出 fluid ghost 可保存/读取后保持 display stack 与 GenericStack 输出
package_pattern_terminal 配置 UI 可调整流体处理输出 ghost 数量，调整后 AE2 processing pattern outputs[] 可见 2000 mB water
package_pattern_terminal Split 可把已编码 packaged_processing_pattern 逐张拆为 package_pattern
package_pattern_terminal Split pending queue 可保存/读取后继续输出
package_pattern_terminal 输入槽颜色可清除
package_pattern_terminal 输出阻挡时保留空白样板
package_pattern_terminal 拒绝把已编码 package_pattern 当空白样板覆盖
package_pattern_terminal 拒绝把已编码 packaged_processing_pattern 当空白样板覆盖
package_pattern_terminal 使用按朝向旋转的薄面板 VoxelShape
package_pattern_terminal 物品是 AE2 part item，可通过 PartHelper 放置到 cable bus 侧面并打开/使用同一终端逻辑
package_pattern_terminal AE2 part 可保存/读取 selectedColor、预览输入槽和流体处理输出 ghost
玩家配方不再产出本地 package_pattern / packaged_processing_pattern，关键机器、终端和总线配方仍可加载
装配室可读取 AE2 blank_pattern 承载的 package_pattern NBT 并生成匹配包裹
装配室可读取 AE2 blank_pattern 承载的 packaged_processing_pattern NBT 并逐包生成匹配包裹
装配室可接受 AE2 Pattern Provider 推送的 AE2 encoded packaged-processing carrier
package_bus 配置 UI 可手工编辑颜色、marker ghost 和 required content ghost，且不消耗玩家光标物品
package_bus 手工过滤器保存/读取后保留 color、marker 和 required content
package_bus 配置 UI 可从 Forge 流体容器设置 required content ghost，编码时写入 AEFluidKey 过滤条件且不消耗光标容器
package_bus 配置 UI 可调整流体 required content ghost 数量，且不会降到小于一桶
package_bus 手工流体过滤器保存/读取后保留 color、required content key 和 amount
PackageFilter 可匹配流体 required content
item handler 打包计划可按内容过滤只选择 requiredContents
item handler 打包计划在 requiredContents 缺失时拒绝
item handler 打包计划可从过滤模板 override marker
item handler 打包计划在显式 retain 模式保留源 marker
item handler 打包计划在显式 override 模式写入配置 marker
item handler 打包计划在显式 clear 模式移除 marker
item handler 打包计划可使用 64k 容量档
过滤系统可从已编码 package_pattern 读取过滤模板
ME Packager 可识别 AE2 64k storage component 为 64k 包裹容量档
ME Packager 菜单可切换红石模式
ME Packager 红石上升沿只执行一次
ME Packager 周期红石模式在持续供电时重复执行
MEStorage 打包计划可从 AE2 storage 抽取 GenericStack 内容
MEStorage 拆包可把包裹完整插入 AE2 storage
MEStorage 打包计划会展开 storage 中已有源包裹再封装
MEStorage 打包计划在显式 clear 模式移除源包裹 marker
真实 AE2 Interface 网络 smoke 可从 Drive 存储打包并整包拆回网络
fluid handler 打包计划可从 Forge FluidTank 抽取 AEFluidKey 内容
fluid handler 拆包可把包裹完整插入 Forge FluidTank
fluid handler 拆包在目标流体不兼容且已满时拒绝
真实世界相邻 Forge fluid handler smoke 可由 ME Packager 打包并整包拆回流体槽
当前最新执行：.\gradlew.bat runGameTestServer 成功，112 个必需 GameTest 全部通过。
2026-07-03 06:15 再次执行 `.\gradlew.bat runGameTestServer` 成功，112 个必需 GameTest 全部通过。
2026-07-03 06:27 再次执行 `.\gradlew.bat runGameTestServer` 成功，112 个必需 GameTest 全部通过。
2026-07-03 06:40 在发布 jar 排除 dev verification classes 后再次执行 `.\gradlew.bat runGameTestServer` 成功，112 个必需 GameTest 全部通过。
```

1.20.1 运行要求：

```text
GameTest template 存放在 gameteststructures/*.snbt。
copyGameTestStructures 会在 prepareGameTestServerRun 前复制到 run/gameteststructures。
当前空模板为 gameteststructures/empty.snbt。
```

GameTest 决策规则：

```text
打包、拆包、库存、过滤、容量、网络、数据包、保存读取、红石触发等行为敏感变更必须考虑 GameTest。
如果未添加或未运行 GameTest，必须在 development-log.md 记录原因。
```

## 3. DataGen 验证

必须运行：

```powershell
.\gradlew.bat runData
```

检查：

```text
生成资源纳入 git
recipe 可加载
loot table 可加载
blockstate/model/item model 路径正确
language key 完整
```

当前资产验证：

```text
5 个 docs/assets/contracts/*.yaml 均通过 assetgen validate-contract
PNG/JSON/model 引用机械检查通过
资源抽样视觉检查通过
已按 AE2 forge/v15.4.10 源码资产生成参考 sheet，并基于参考完成二轮材质重做
60 个 PNG 尺寸/模式/模型引用检查通过
55 个 JSON 可解析
.\gradlew.bat runData 成功
2026-07-03 05:35 再次执行 .\gradlew.bat runData 成功，未写出新的 generated resources 内容
2026-07-03 05:43 再次执行 .\gradlew.bat runData 成功，未写出新的 generated resources 内容
2026-07-03 06:14 再次执行 .\gradlew.bat runData 成功，未写出新的 generated resources 内容
2026-07-03 06:16 轻量资源审计通过：英文/简体中文语言 key 对齐，52 个 PNG 非空。
2026-07-03 06:26 再次执行 .\gradlew.bat runData 成功，未写出新的 generated resources 内容。
2026-07-03 06:27 资源审计通过：60 个 PNG 非空，55 个 JSON 可解析；Package Pattern Terminal part 新增 8 个 16x16 RGBA PNG 和 1 个 base part model。
2026-07-03 06:42 再次执行 .\gradlew.bat runData 成功，未写出新的 generated resources 内容。
```

## 4. 构建验证

必须运行：

```powershell
.\gradlew.bat build
```

检查：

```text
编译成功
测试成功
jar 生成在 build/libs
jar 文件名包含 mod id 和版本
```

当前最新执行：`.\gradlew.bat build` 成功，生成 `build/libs/appliedpackaging-0.1.0-dev.jar`。

2026-07-03 06:13 再次执行 `.\gradlew.bat build` 成功。
2026-07-03 06:25 再次执行 `.\gradlew.bat build` 成功。
2026-07-03 06:39 再次执行 `.\gradlew.bat build` 成功，重新生成 `build/libs/appliedpackaging-0.1.0-dev.jar`。
本次发布 jar 重新打包后已确认包含 `META-INF/mods.toml`、`META-INF/MANIFEST.MF`、`LICENSE.md`、`README.md`、`CHANGELOG.md` 和 `assets/appliedpackaging/logo.png`。
发布 jar 审计通过：`jar tf` 未发现 `ClientSmokeRunner`、`gametest`、`build/tmp`、reference、preview、`docs/assets`、`run/` 等 dev/test 条目。
发布 jar 文本资源审计通过：未发现 `E:\`、`C:\Users`、`build/reference`、`build/asset-reference`、`.codex` 或 `asset-reference` 等本机绝对路径和参考素材路径。

## 5. 客户端验证

必须验证：

```text
Minecraft 1.20.1 Forge + AE2 15.4.10 可进入游戏
创造栏条目正确
17 色包裹图标可区分
Tooltip 每包/总计正确
机器 GUI 无错位
方块模型无 missing texture
```

推荐命令：

```powershell
.\gradlew.bat runClient
.\gradlew.bat runClientSmoke
```

当前客户端 smoke：

```text
2026-07-03 再次执行 .\gradlew.bat runClient，已启动到 Minecraft 客户端主流程。
日志确认 Applied Packaging 初始化完成、ResourceManager 重载完成、OpenAL/SoundEngine 启动、block atlas 创建完成。
Package Bus screen/menu 注册后、Package Bus 手工过滤 UI 布局调整后、Package Pattern Terminal 处理输出 ghost slots 布局调整后、Package Pattern Terminal 薄面板模型调整后、AE2 blank_pattern 已编码 tooltip hook 接入后、AE2 encoded packaged-processing carrier 接入后，以及样板配方入口收敛到 AE2 blank_pattern 后的客户端启动 smoke 通过。
run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture、preview_sheet 或 mip level。
机器 preview sheet 已移至 docs/assets/previews，不再作为 block atlas 资源加载；最新 smoke 未再出现 preview_sheet/mip level 警告。
本次 smoke 在 atlas 创建完成后手动终止；Gradle 退出码来自人工终止，不代表客户端启动失败。
已观察到的剩余警告为 Forge/AE2/Vanilla 常规开发环境警告。

2026-07-03 新增并执行 .\gradlew.bat runClientSmoke 成功。
runClientSmoke 使用 --quickPlaySingleplayer 进入本地单人世界，自动摆放 6 个关键方块，依次通过真实服务器玩家和 NetworkHooks.openScreen 打开菜单，截图后退出客户端。
已生成并人工检查以下截图，均为真实 Minecraft 客户端菜单画面，不是原型图或静态 mock：
  run/screenshots/appliedpackaging-client-smoke-package_assembler.png
  run/screenshots/appliedpackaging-client-smoke-me_packager.png
  run/screenshots/appliedpackaging-client-smoke-package_pattern_terminal.png
  run/screenshots/appliedpackaging-client-smoke-package_storage_bus.png
  run/screenshots/appliedpackaging-client-smoke-package_export_bus.png
  run/screenshots/appliedpackaging-client-smoke-package_unpacking_bus.png
run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture 或客户端 smoke timeout。
默认 quick-play 世界名为 New World；可用 -Pappliedpackaging.clientSmoke.world="世界名" 覆盖。

2026-07-03 05:35 在 Package Bus 流体 required content ghost 完成后再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 仍自动摆放 6 个关键方块，打开 Package Assembler、ME Packager、Package Pattern Terminal、Package Storage Bus、Package Export Bus、Package Unpacking Bus 真实菜单并截图后退出。
6 张截图均已生成；人工抽看 Package Pattern Terminal 与 Package Storage Bus，确认菜单非空屏、核心控件和槽位显示正常。
run/logs/latest.log 未发现 ERROR、FATAL、Exception、Missing model、Unable to load model、missing texture、Timed out 或 timeout。

2026-07-03 05:44 在流体 ghost 数量滚轮调整 UI 完成后再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 生成 6 张真实菜单截图并正常退出客户端。
run/logs/latest.log 未发现 ERROR、FATAL、Exception、Missing model、Unable to load model、missing texture、Timed out 或 timeout。

2026-07-03 06:04 在 Package Pattern Terminal 改为 AE2 cable part item 后再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 的 Package Pattern Terminal 步骤通过 PartHelper.setPart 放置真实 AE2 part，再用 part host buffer 打开 PackagePatternTerminalScreen。
6 张截图均重新生成并正常退出客户端；run/logs/latest.log 未发现 ERROR、FATAL、Exception、Missing model、Unable to load model、missing texture、Timed out 或 timeout。

2026-07-03 06:08 再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 仍通过 PartHelper.setPart 放置 Package Pattern Terminal AE2 part，并生成 6 张真实菜单截图；人工抽看 Package Pattern Terminal 截图，确认菜单非空屏、核心控件和槽位显示正常。

2026-07-03 06:18 再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 自动摆放 6 个目标，Package Pattern Terminal 步骤继续使用 AE2 part host，6 张真实菜单截图均重新生成并正常退出客户端。

2026-07-03 06:27 在 Package Pattern Terminal part 切换为 Applied Packaging 自有 part body/front/back/sides/overlay mask 材质后，再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 继续通过真实 AE2 part host 打开 Package Pattern Terminal 菜单，生成 6 张真实菜单截图并正常退出客户端；人工抽看 Package Pattern Terminal 截图，确认菜单非空屏、核心控件和槽位显示正常。
run/logs/latest.log 未发现 ERROR、FATAL、Exception、Missing model、Unable to load model、missing texture、Timed out 或 timeout。

2026-07-03 06:41 在 ClientSmokeRunner 改为按 `appliedpackaging.clientSmoke.enabled=true` 反射加载且从 release jar 排除后，再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 仍生成 Package Assembler、ME Packager、Package Pattern Terminal、Package Storage Bus、Package Export Bus、Package Unpacking Bus 共 6 张真实菜单截图并正常退出客户端。
run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture、NoClassDefFoundError、ClassNotFoundException、InvocationTargetException、IllegalStateException、Timed out 或 timeout。
```

## 6. Dedicated Server 验证

必须验证：

```text
服务端可启动
客户端类不被服务端加载
注册、recipe、datapack 加载无异常
```

推荐命令：

```powershell
.\gradlew.bat runServer
```

当前服务端 smoke：

```text
.\gradlew.bat runServer 已执行到专用服务端启动阶段。
服务端在读取 run/eula.txt 时按 Mojang EULA 要求停止，未继续进入世界加载。
停止前未出现 Applied Packaging 客户端类误加载、注册崩溃或 mod 扫描异常。
2026-07-03 新增 ClientSmokeRunner 后再次执行 .\gradlew.bat runServer，仍正常到达 EULA gate，未出现客户端 smoke 类误加载。
2026-07-03 06:35 再次执行 .\gradlew.bat runServer，服务端仍正常到达 EULA gate；run/logs/latest.log 未发现 ERROR、FATAL、ClientSmokeRunner、NoClassDefFoundError、ClassNotFoundException 或客户端类误加载关键字。
2026-07-03 06:42 在发布 jar 排除 ClientSmokeRunner 和 gametest classes 后再次执行 .\gradlew.bat runServer，服务端仍正常到达 EULA gate；run/logs/latest.log 未发现 ERROR、FATAL、ClientSmokeRunner、NoClassDefFoundError、ClassNotFoundException、InvocationTargetException、IllegalStateException、Dist.CLIENT 或 OnlyIn。
验收专用服务端完整启动前，需要用户显式同意 EULA 后再重新运行；AI 不自动修改 eula.txt。
```

## 7. 发布验收

必须全部满足：

```text
git 工作树干净，发布 tag 可追溯
.\gradlew.bat build 成功
.\gradlew.bat runData 成功且生成资源已纳入 git
.\gradlew.bat runGameTestServer 成功，或记录无法运行的明确阻塞
生成 build/libs/appliedpackaging-<version>.jar
jar 在 Minecraft 1.20.1 Forge + AE2 15.4.10 客户端中可进入游戏
核心玩法按 01-requirements.md 的 R1-R13 验收
docs 与实现一致
```

## 8. 发布清单

当前发布前准备状态：

```text
CHANGELOG.md：已补齐 0.1.0-dev 发布记录、验证结果和已知限制
LICENSE.md：已补齐 All Rights Reserved 许可声明
README.md：已补齐安装要求、玩法流程、功能清单、验证状态和已知限制
release jar：已包含 README.md、CHANGELOG.md、LICENSE.md、META-INF/MANIFEST.MF 与 META-INF/mods.toml，且不包含 ClientSmokeRunner、gametest classes、reference sheets、build/tmp、docs/assets 或本机绝对路径
logo/icon：assets/appliedpackaging/logo.png、textures/gui/logo.png 和包裹/机器/总线图标已存在
release notes：已写入 CHANGELOG.md
known limitations：已写入 README.md 与 CHANGELOG.md
compatible Minecraft/Forge/AE2 version list：已写入 README.md，并由 gradle.properties / mods.toml 模板声明
```

## 9. 当前完成度审计

以 `docs/01-requirements.md` R1-R13 和本文件发布验收为准，当前状态：

```text
R1 17 色独立包裹物品：已满足，注册项、item tag、语言、图标和 GameTest 覆盖。
R2 无正常空包裹玩法：已满足，空包裹不进玩家配方/创造栏，物流和 GameTest 均拒绝无 PackageData 包裹。
R3 相同包裹才可堆叠：已满足，canonical hash 和规范化 NBT GameTest 覆盖。
R4 GenericStack 数据模型：已满足，PackageData 使用 AEKey/GenericStack，item、fluid 和 MEStorage 路径已覆盖。
R5 不允许真实嵌套：已满足，打包计划和 MEStorage 端点会展开源包裹，GameTest 覆盖。
R6 ME 包裹装配室：已满足，普通/彩色/包裹/封装处理载体、pending queue、阻挡和自动导出均已覆盖。
R7 ME 打包机：已满足，相邻 item/fluid/AE2 storage 端点、红石模式、容量、过滤、marker 和拆包事务均已覆盖。
R8 包裹样板终端：已满足，AE2 blank_pattern 载体、colored metadata、packaged-processing、Split、AE2 part host 均已覆盖。
R9 包裹总线：已满足，Storage/Export/Unpacking Bus 仅处理合法包裹，不暴露内部散装内容。
R10 事务性：已满足，打包/拆包模拟失败不提交、完整包裹拆入和容量失败回滚均由 GameTest 覆盖。
R11 Tooltip：已满足，包裹、样板、AE2 blank_pattern carrier 和 packaged-processing 输出提示已接入。
R12 英文与简体中文语言：已满足，语言 key 对齐审计通过。
R13 发布资源与元数据：已满足，jar、recipe、loot table、模型、材质、logo、mods.toml、README/CHANGELOG/LICENSE 均存在并已打包，发布 jar 已排除 dev verification classes 和参考素材路径。
```

当前仍未完成的发布验收项：

```text
Dedicated server full world-load：未完成。
原因：用户已在 2026-07-04 明确同意 EULA，run/eula.txt 当前为 eula=true；但用户将在 2026-07-05 补充需求和材质，最终 dedicated server world-load 等新增范围冻结后执行。
需要在新增需求和材质实现并验证后重新执行 .\gradlew.bat runServer，确认专用服务端进入世界加载并无客户端类误加载。
```

当前记录的非阻塞发布后增强：

```text
任意 AEKey 直接输出 ghost editor
任意 AEKey 直接 required-content filter editor
批量 required content 编辑
```

## 10. 目标级验收审计

本节对照最初目标进行完成度判定。只有所有必需项都有当前证据时，才允许把目标标记为完成。

```text
需求分析：已完成。证据：docs/01-requirements.md 定义目标用户、核心场景、R1-R13、NF1-NF6 和范围边界。
概要设计：已完成。证据：docs/02-system-architecture.md 定义模块划分、数据流、版本适配和客户端/服务端边界。
详细设计：已完成。证据：docs/03-detailed-design.md 定义 PackageData、事务、机器、样板、总线、过滤和菜单规则。
设计入口：已完成。证据：docs/design.md 只保留当前定案摘要和分类文档入口。
讨论记录：已完成。证据：docs/chat-summary.md 保留历史讨论，并声明不作为最新实现规格。
AI 指令分离：已完成。证据：AGENTS.md 只放 agent 工作规则，docs/00-document-index.md 声明设计文档不承载 AI 指令。
Minecraft 1.20.1 优先：已完成。证据：gradle.properties、build.gradle、README.md 和 docs/design.md 均固定 Minecraft 1.20.1 Forge / AE2 15.4.10。
材质准备：已完成。证据：docs/04-asset-spec.md、docs/assets/*、src/main/resources/assets/appliedpackaging 下 PNG/模型/语言文件，以及资源审计记录。
功能实现：已完成到 0.1.0-dev 范围。证据：R1-R13 完成度审计均为已满足，112 个必需 GameTest 全部通过。
Git 初始化和文档管理：已完成。证据：仓库有连续提交记录，文档按 00-07 分类维护，开发流水记录在 docs/development-log.md。
发布 jar：已完成。证据：build/libs/appliedpackaging-0.1.0-dev.jar 存在，已通过 build、jar 内容审计和 release metadata 审计。
客户端可用性：已完成。证据：runClientSmoke 进入真实单人世界并打开 6 个关键菜单截图，无 missing model/texture/classloading 关键错误。
GameTest 验证：已完成。证据：.\gradlew.bat runGameTestServer 成功，112 个必需 GameTest 全部通过。
DataGen 验证：已完成。证据：.\gradlew.bat runData 成功，未写出新的 generated resources 内容。
Dedicated server EULA 前 classloading smoke：已完成。证据：.\gradlew.bat runServer 到达 EULA gate，未发现客户端类误加载关键字。
Dedicated server full world-load：未完成。证据不足原因：用户已在 2026-07-04 明确同意 EULA，run/eula.txt 当前为 eula=true；但用户将在 2026-07-05 补充需求和材质，最终服务端验收等待新增范围冻结后执行。
发布 tag：未完成。原因：新增需求和材质尚待输入，最终 dedicated server full world-load 尚未验收；发布 tag 应在新增范围完成且服务端验收通过后创建。
```

当前目标完成判定：

```text
不能标记完成。
当前不再是 EULA 阻塞；用户将在 2026-07-05 补充需求和材质，最终发布范围尚未冻结。
发布 tag 应等待新增范围完成、重新验证并通过 dedicated server full world-load 后再创建。
```

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
  水平朝向 blockstate
  方块掉落表
  内部输入/输出 item handler
  非潜行右键打开 GUI/Menu
  GUI 输入槽、输出槽、容量槽、过滤槽、marker 槽、玩家背包、17 色 swatch、Pack Once 图标按钮、marker 策略图标按钮和红石模式图标按钮
  潜行右键放入包裹、取出输出、触发一次操作
  红石上升沿触发一次操作
  红石模式可在忽略/上升沿/周期之间切换，默认上升沿以兼容旧行为
  周期红石模式在持续供电时每 20 tick 尝试一次 pack/unpack
  背面 Forge item handler 打包/拆包事务
  容量槽识别 AE2 16k/64k/256k storage component、item/fluid storage cell 与 portable cell
  selectedColor 控制无过滤模板时的输出包裹颜色
  过滤槽接受已编码 package_pattern、packaged_processing_pattern 或合法包裹
  过滤模板用于打包输出颜色、requiredContents 打包过滤和拆包包裹过滤
  marker retain/override/clear 策略由 GUI 独立配置，override 可使用 marker 槽物品或过滤模板 marker 作为兼容回退
  背面优先识别 AE2 MEStorage capability，可接入相邻 ME Interface 暴露的子网存储
  AE2 MEStorage 打包/拆包事务，支持 GenericStack/AEKey 和源包裹展开
  Forge fluid handler 打包/拆包事务，支持 AEFluidKey/FluidStack 和相邻流体槽
  item-only GameTest，覆盖显式 marker retain/override/clear
  MEStorage endpoint GameTest
  fluid handler endpoint GameTest
  ME Packager 红石模式菜单、红石上升沿和周期红石 GameTest
  真实 AE2 Creative Energy Cell + Drive + Interface + ME Packager 世界内打包/拆包 GameTest smoke
  真实世界相邻 Forge fluid handler + ME Packager 打包/拆包 GameTest smoke

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
阻挡模式
GameTest/客户端验证
```

当前状态：

```text
已实现：
  package_assembler 方块/方块物品/方块实体注册
  水平朝向 blockstate
  方块掉落表
  Package Assembler GUI/Menu
  9 格输入缓冲 + 1 格样板槽 + 1 格输出槽 + 1 格容量槽
  shift-click 样板进样板槽，AE2 容量元件进容量槽，其它物品进入输入缓冲
  输入缓冲自动封装为 Fluix 包裹
  输入合法包裹展开后再封装
  容量槽识别 AE2 16k/64k/256k storage component、item/fluid storage cell 与 portable cell
  输出非空时阻挡且不消耗输入
  已编码 package_pattern 精确匹配输入计划后生成对应颜色包裹
  已编码 package_pattern 走 exact package plan，可重封装大于默认容量的源包裹
  已编码 package_pattern 不消耗，可重复作为本地装配计划
  已编码 packaged_processing_pattern 保存有序多包裹计划
  package_assembler 可按 packaged_processing_pattern 逐包生成匹配包裹
  package_assembler 暴露 AE2 ICraftingMachine capability
  Pattern Provider pushPattern 可把 KeyCounter 中的物品/流体 GenericStack 输入装配为包裹
  空样板槽的普通 Pattern Provider pushPattern 直接从 KeyCounter 规划包裹，避免 9 格临时输入缓存限制
  本地自由封装、普通 Pattern Provider pushPattern、彩色 Pattern Provider pushPattern 均使用容量槽档位
  pushPattern 在输出阻挡、输入缓冲非空、非物品 AEKey 或规划失败时整批拒绝且不消耗输入
  ColoredProcessingPatternDataStorage 可在 AE2 encoded processing pattern 上保存输入槽颜色元数据
  彩色 Pattern Provider pushPattern 读取 AE2 sparse input 槽位，按输入槽颜色拆成多个包裹
  彩色 Pattern Provider pushPattern 支持流体 AEKey 输入
  同 AEKey 位于不同颜色槽时按 sparse 槽位拆分，不被 AE2 condensed input 提前合并
  彩色 pushPattern 产生多个包裹时通过 pending queue 顺序输出并持久化保存
  装配室输出自动导出默认开启，可通过 GUI 图标按钮切换并持久化保存
  装配室 server tick 会把输出槽包裹优先导出到背面 AE2 MEStorage，其次回落到背面 Forge item handler
  自动导出失败时保留输出槽包裹，不丢弃、不继续消耗新输入
  真实 AE2 Creative Energy Cell + Pattern Provider + Package Assembler GameTest smoke
  真实 AE2 Creative Energy Cell + Pattern Provider + Package Assembler 彩色处理样板 GameTest smoke
  真实 AE2 Drive + 64k item cell + Crafting CPU + Pattern Provider + Package Assembler 自动合成 job smoke
  真实 AE2 Creative Energy Cell + Drive + Interface + Package Assembler 自动导出 GameTest smoke
  装配室基础 GameTest

客户端验证：
  runClientSmoke 已覆盖 Package Assembler GUI 打开与截图
```

验收：

```text
普通处理样板生成默认色包裹
彩色输入格生成对应颜色包裹
同 AEKey 位于不同颜色格时不会提前合并
阻挡模式只检查本机输出口
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
  package_export_bus 只从 AE 网络输出已有合法包裹
  package_unpacking_bus 整包事务性拆入背面库存
  总线支持手持已编码样板/合法包裹设置 ghost 过滤模板，潜行空手清除
  总线提供共享 Package Bus 配置 UI，可显示 ghost filter、从光标复制模板、清除模板、shift-click 背包模板设置 ghost filter
  Package Bus 配置 UI 不消耗玩家光标或背包中的模板物品
  Package Bus 配置 UI 支持手工编辑颜色、marker ghost 和 3 个 required content ghost slots
  Package Bus required content ghost slots 可从 Forge 流体容器编码 AEFluidKey 过滤条件
  手工 Package Bus 过滤器以 PackageFilter NBT 保存，并兼容旧 filter_template 读取
  runClientSmoke 可 quick-play 单人世界、摆放关键方块与 Package Pattern Terminal AE2 part、打开真实菜单、截图 Package Assembler/ME Packager/Package Pattern Terminal/Package Storage Bus/Package Export Bus/Package Unpacking Bus 后退出
  PackageItemStorage/总线过滤 GameTest
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
  packaged_processing_pattern NBT 支持可选 outputs[]，终端提供 3 个处理输出 ghost slots
  处理输出 ghost slots 可从光标复制物品/流体容器，右键复制 1 个物品或 1 个容器量，空光标清除，且不消耗玩家物品
  处理输出 ghost slots 可把 Forge 流体容器编码为 AEFluidKey 输出，例如水桶编码为 1000 mB water
  packaged_processing_pattern tooltip 显示已编码处理输出
  已编码 AE2 blank_pattern 通过客户端 tooltip hook 显示 package_pattern 或 packaged_processing_pattern 内容，未编码 AE2 blank_pattern 保持原版 tooltip
  package_pattern_terminal 已调整为 AE2 风格薄面板 block model，并提供按朝向旋转的薄面板 VoxelShape
  package_pattern_terminal AE2 part 已使用 Applied Packaging 自有 body/front/back/sides/overlay mask 材质和 base part model，不再依赖 AE2 pattern terminal 纹理层
  Package Pattern Terminal 处理输出 ghost 槽支持滚轮调整已设置 key 的数量，流体每步 1000 mB
  Package Bus required content ghost 槽支持滚轮调整已设置 key 的数量，流体每步 1000 mB
  package_pattern_terminal 可通过 Split 按钮把已编码 packaged_processing_pattern 拆回普通 package_pattern
  package_pattern_terminal Split pending queue 会保存/读取，输出槽清空后可继续吐出后续 package_pattern
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
tag 1.0.0
```

验收：

```text
git 工作树干净
build/libs/appliedpackaging-<version>.jar 存在
jar 可在 Minecraft 1.20.1 Forge + AE2 15.4.10 客户端进入游戏
docs 与实现一致
```

## 风险

AE2 样板集成风险：

```text
AE2 1.20.1 API 对自定义 Pattern Provider/Molecular Assembler 风格集成的公开入口可能不足。
先实现可由 Pattern Provider 推入材料的普通 Forge inventory 机器，再逐步接入深层样板语义。
```

AE2 总线 Part API 风险：

```text
Part API 可能需要较重集成。
先交付方块形态 Package Storage/Export/Unpacking Port，再实现真正 cable part。
如果发布前 Part API 阻塞，需在需求中明确改名和玩法等价性。
```

GenericStack 范围风险：

```text
1.0 垂直切片优先验证物品。
数据模型完整支持 AEKey；对未知 key 保守拒绝拆包，避免吞资源。
流体 adapter 已在 Packager Forge fluid handler endpoint 接入，并已通过真实世界相邻 fluid handler smoke 覆盖基础打包/拆包。
```

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
  PackageData GameTest

待实现：
  机器事务与 Forge/AE2 endpoint 对接
  hash 稳定性更细粒度测试
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
已交付 me_packager、package_assembler、package_pattern、packaged_processing_pattern 基础配方。
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
  GUI 输入槽、输出槽、容量槽、过滤槽、marker 槽、玩家背包、17 色 swatch、Pack Once 图标按钮和 marker 策略图标按钮
  潜行右键放入包裹、取出输出、触发一次操作
  红石上升沿触发一次操作
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

待实现：
  周期红石模式
  真实 AE 网络/Interface 世界内 smoke
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
  9 格输入缓冲 + 1 格样板槽 + 1 格输出槽
  shift-click 样板进样板槽，其它物品进入输入缓冲
  输入缓冲自动封装为 Fluix 包裹
  输入合法包裹展开后再封装
  输出非空时阻挡且不消耗输入
  已编码 package_pattern 精确匹配输入计划后生成对应颜色包裹
  已编码 package_pattern 不消耗，可重复作为本地装配计划
  装配室基础 GameTest

待实现：
  彩色处理样板元数据读取
  封装处理样板拆分为多包裹计划
  容量元件槽
  AE2 Pattern Provider/pushPattern 深集成
  客户端验证
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
  package_export_bus 只从 AE 网络输出已有合法包裹
  package_unpacking_bus 整包事务性拆入背面库存
  PackageItemStorage GameTest
  package_pattern_terminal 方块、方块物品、方块实体、菜单、客户端 screen
  package_pattern_terminal 可从 9 格预览输入编码 package_pattern
  package_pattern_terminal 支持 17 色 swatch 选择，编码样板颜色跟随 selectedColor
  package_pattern_terminal 支持 marker 槽与容量槽编码 package_pattern
  package_pattern / packaged_processing_pattern tooltip 显示空白或已编码包裹内容
  装配室可读取 package_pattern_terminal 产出的已编码 package_pattern

待实现：
  彩色处理样板编辑
  封装处理样板合成/拆分
  颜色/marker/content 过滤 UI
  AE2 cable part 形态
  真实 AE 网络服务器 smoke test
  客户端模型/GUI 冒烟验证
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
流体 adapter 已在 Packager Forge fluid handler endpoint 接入；后续仍需真实世界 smoke。
```

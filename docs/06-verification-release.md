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
当前最新执行：.\gradlew.bat runGameTestServer 成功，21 个必需 GameTest 全部通过。
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
.\gradlew.bat runData 成功
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

发布前准备：

```text
CHANGELOG.md
LICENSE 或许可声明
README.md
logo/icon
release notes
known limitations
compatible Minecraft/Forge/AE2 version list
```

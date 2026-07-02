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
装配室可使用已编码 package_pattern 精确匹配输入计划
AE2 PackageItemStorage 只暴露合法包裹
AE2 PackageItemStorage 拒绝散装物品插入
AE2 PackageItemStorage 可模拟并提交合法包裹插入/抽取
package_pattern 数据可读写
package_pattern_terminal 可从预览输入编码 package_pattern
package_pattern_terminal 可用 selectedColor 编码非默认颜色样板
package_pattern_terminal 输出阻挡时保留空白样板
package_pattern_terminal 拒绝把已编码 package_pattern 当空白样板覆盖
item handler 打包计划可按内容过滤只选择 requiredContents
item handler 打包计划在 requiredContents 缺失时拒绝
item handler 打包计划可从过滤模板 override marker
item handler 打包计划可使用 64k 容量档
过滤系统可从已编码 package_pattern 读取过滤模板
ME Packager 可识别 AE2 64k storage component 为 64k 包裹容量档
MEStorage 打包计划可从 AE2 storage 抽取 GenericStack 内容
MEStorage 拆包可把包裹完整插入 AE2 storage
MEStorage 打包计划会展开 storage 中已有源包裹再封装
当前最新执行：.\gradlew.bat runGameTestServer 成功，39 个必需 GameTest 全部通过。
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
53 个 PNG 尺寸/模式/模型引用检查通过
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

当前客户端 smoke：

```text
.\gradlew.bat runClient 已启动到 Minecraft 客户端主流程。
日志确认 Applied Packaging 初始化完成、ResourceManager 重载完成、OpenAL/SoundEngine 启动、block atlas 创建完成。
未发现 appliedpackaging 相关 missing model/texture、客户端类加载异常或崩溃。
本次 smoke 后客户端正常 Stopping。
已观察到的警告为 Forge/AE2/Vanilla 常规开发环境警告，以及 me_packager_preview_sheet 68x68 导致 mip level 降至 2；不阻塞发布，但后续可把预览 sheet 改成 64x64 或 128x128。
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

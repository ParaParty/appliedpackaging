# 验证与发布

本文只定义当前验证门禁，不保存逐次执行记录、截图历史、构建哈希或发布状态。

## 1. 变更对应验证

| 变更 | 必须执行 |
| --- | --- |
| Java 编译或 Gradle 配置 | `compileJava`，完成前执行 `build` |
| 机器、事务、能力、网络、红石、样板、总线、序列缓存器 | `runGameTestServer` + `build` |
| datagen、配方、loot、tag、模型生成 | `runData`，检查生成差异，再执行 `build` |
| Screen、renderer、Mixin、资源加载 | `build` + 实际 `runClient` 路径 |
| PNG、模型、ScreenStyle | `verify-assets.ps1` + `build`，并按视觉风险运行客户端 |
| GuideME、语言或设计文档 | `verify-docs.ps1`；语言/发布资源变更再执行 `verify-release.ps1` |
| 发布候选 | GameTest、DataGen、build、三个校验脚本，以及独立 dedicated server 启动 |

GameTest 不能证明像素、动画、hover、点击命中或客户端类加载；`build` 也不能替代行为验证。

## 2. 常用命令

```powershell
.\gradlew.bat compileJava --stacktrace
.\gradlew.bat runGameTestServer --stacktrace
.\gradlew.bat runData --stacktrace
.\gradlew.bat build --stacktrace
.\gradlew.bat runClient --stacktrace
.\gradlew.bat runServer --stacktrace
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1
```

Star Technology Fork 的可选验证使用已有 Gradle 属性，不修改发布依赖：

```powershell
& .\gradlew.bat 'runGameTestServer' '-PgtceuRuntimeJar=<versioned-jar>' --stacktrace
```

查看器客户端可通过 `-PrecipeViewerRuntime=jei|emi|none` 选择运行时。

## 3. GameTest 范围

现有 GameTest 位于：

```text
src/main/java/com/warmthdawn/appliedpackaging/gametest/PackageDataGameTests.java
src/main/java/com/warmthdawn/appliedpackaging/gametest/SequenceBufferGameTests.java
src/main/java/com/warmthdawn/appliedpackaging/gametest/OptionalRecipeIntegrationGameTests.java
```

发布 jar 通过 Gradle 排除 `gametest/**`。测试结构来自仓库根目录 `gameteststructures/`，`prepareGameTestServerRun` 会复制到对应开发运行目录。

行为变更应扩展最接近的现有测试，至少覆盖成功、容量/过滤拒绝、模拟不修改状态、提交失败不产生部分结果和 NBT 往返中与本次变更相关的边界。

## 4. 客户端检查

涉及客户端时，至少检查：

- 游戏完成初始化、资源重载和纹理图集创建；
- 目标 Screen 可打开，控件、滚动、tooltip、数量编辑和模式切换可用；
- 方块、part、物品和动态 renderer 无 missing model/texture；
- dedicated server 不加载客户端类；
- `latest.log` 中没有与本 Mod 相关的异常。

客户端由人工关闭；不维护自动截图、Quick Play、固定世界或专用 smoke runner。

## 5. 三个仓库校验脚本

- `verify-docs.ps1`：只检查两份核心文档、GuideME 中英文页面对齐、必要 frontmatter 与本地 Markdown 链接。
- `verify-assets.ps1`：检查发布 PNG、模型、ScreenStyle 与关键 UI/模型不变量。
- `verify-release.ps1`：检查 jar 元数据、发布资源与源码同步、JSON、语言 key/占位符、模型贴图引用和玩家入口不变量。

这些脚本只验证仓库与产物，不生成发布 manifest/bundle，也不模拟负例 fixture。

## 6. 发布候选

按以下顺序执行并检查真实输出：

1. 确认工作树中的变更范围明确。
2. `runData`，确认没有意外生成差异。
3. `runGameTestServer`。
4. `build`。
5. 依次运行三个校验脚本。
6. 对客户端变更运行 `runClient` 并完成人工交互检查。
7. 运行 `runServer`，确认 dedicated server 完成 world load 后正常关闭。
8. 检查 `git diff --check` 与最终 `git status --short`。

发布是否可打 tag 由当次验证结果决定，不在文档中维护状态机或历史“已通过”记录。

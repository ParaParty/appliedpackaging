# 参考来源

本文件记录版本选择、工具链和 AE2 玩法语义的外部来源。涉及“latest/current/recommended”的信息会随时间变化；修改版本基线前应重新核实。

## 工具链

NeoForgeMDKs `MDK-Forge-1.20.1-ModDevGradle`

```text
用途：1.20.1 Forge + ModDevGradle Legacy 项目骨架。
地址：https://github.com/NeoForgeMDKs/MDK-Forge-1.20.1-ModDevGradle
```

NeoForged ModDevGradle 文档

```text
用途：ModDevGradle / LegacyForge 工具链判断。
地址：https://docs.neoforged.net/toolchain/docs/plugins/mdg/
```

Forge GameTest 文档

```text
用途：确认 Forge GameTest 注解、template 和 runGameTestServer 验证路径。
地址：https://docs.minecraftforge.net/en/latest/misc/gametest/
```

Forge 1.20.1 下载页

```text
用途：Forge 47.4.10 recommended 与 47.4.20 latest 判断。
地址：https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html
```

## AE2 版本

Applied Energistics 2 官方下载页

```text
用途：确认 AE2 1.20.1 可用版本包含 15.4.10。
地址：https://appliedenergistics.org/download
```

Modrinth AE2 15.4.10 页面

```text
用途：确认 AE2 15.4.10 支持 Minecraft 1.20.1 Forge，环境为 client/server。
地址：https://modrinth.com/mod/ae2/version/15.4.10
```

GuideME 20.1.7

```text
用途：AE2 15.4.10 runtime 必需依赖。runData 首次验证显示 AE2 要求 guideme [20.1.7,20.2.0)，因此 Gradle 显式加入 org.appliedenergistics:guideme:20.1.7，并在 Applied Packaging 的 mods.toml 发布 metadata 中声明 guideme [20.1.7,20.2.0)。
```

AE2 GitHub / Maven 信息

```text
用途：确认 AE2 API Maven 用法和 API-only classifier 方向。
地址：https://github.com/AppliedEnergistics/Applied-Energistics-2
```

## AE2 玩法语义

AE2 1.21.1 Pattern Encoding Terminal UI reference

```text
本地源码：build/reference/ae2-1.21.1
主要文件：src/main/java/appeng/client/gui/me/items/PatternEncodingTermScreen.java
             src/main/java/appeng/client/gui/me/items/CraftingEncodingPanel.java
             src/main/java/appeng/client/gui/me/items/ProcessingEncodingPanel.java
             src/main/resources/assets/ae2/screens/terminals/pattern_encoding_terminal.json
             src/main/resources/assets/ae2/screens/terminals/encoding/crafting.json
用途：高级样板终端的 9 列终端主体、搜索/玩家栏基线、processing slot 行滚动与紧凑按钮视觉；2026-07-11 复核 AE2 main 后确认 small scroller、states 与 1.21.1 参考文件 SHA-256 相同，因此运行时复制的 states atlas 可同时代表当前 main 样式。
版本边界：仅作客户端视觉/交互参考，运行时依赖仍是 AE2 15.4.10 Forge。
许可证：LGPL-3.0-or-later；本地适配 atlas 的许可证文本打包至 META-INF/licenses/ae2-LGPL-3.0-or-later.txt。
```

AE2 1.20.1 Pattern Provider 指南

```text
用途：Pattern Provider 推入相邻库存、all-or-nothing 输入、方向型 Provider 不在推入面提供网络连接、Interface 子网特殊交互。
地址：https://guide.appliedenergistics.org/1.20.1/items-blocks-machines/pattern_provider
```

AE2 1.20.1 Storage Cells 指南

```text
用途：16k / 64k / 256k 容量层级、63 类型心智。
地址：https://guide.appliedenergistics.org/1.20.1/items-blocks-machines/storage_cells
```

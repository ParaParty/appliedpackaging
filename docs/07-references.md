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

JEI 15.20.0.134

```text
用途：Minecraft 1.20.1 Forge 开发客户端的配方与物品测试辅助。只通过 modLocalRuntime 加入本地运行类路径，不属于 Applied Packaging 的编译 API 或发布必需依赖，mods.toml 不声明 JEI。
版本核对：https://maven.blamejared.com/mezz/jei/jei-1.20.1-forge/maven-metadata.xml
官方仓库：https://github.com/mezz/JustEnoughItems/tree/1.20.1
```

AE2 GitHub / Maven 信息

```text
用途：确认 AE2 API Maven 用法和 API-only classifier 方向。
地址：https://github.com/AppliedEnergistics/Applied-Energistics-2
```

## AE2 玩法语义

AE2 15.4.10 Formation Plane / Pattern Provider / Storage Bus 源码

```text
本地源码：build/reference/ae2
固定版本：forge/v15.4.10
主要文件：src/main/java/appeng/parts/automation/FormationPlanePart.java
             src/main/java/appeng/helpers/patternprovider/PatternProviderLogic.java
             src/main/java/appeng/helpers/patternprovider/PatternProviderTarget.java
             src/main/java/appeng/parts/storagebus/StorageBusPart.java
             src/main/java/appeng/menu/implementations/StorageBusMenu.java
             src/main/java/appeng/me/storage/NetworkStorage.java
             src/main/java/appeng/api/storage/MEStorage.java
用途：Formation Plane 的默认优先级为 0，并把玩家配置的原始数值直接用于挂载；它通过只实现 insert 的 MEStorage 成为网络输出端点而不提供库存/抽取。Pattern Provider blocking 在目标包含任一 pattern input 时拒绝 push；Storage Bus Partition Storage 从目标可用 key 重建配置槽。NetworkStorage 先按挂载优先级从高到低遍历，并在每个相同优先级组内先调用 `isPreferredStorageFor` 为真的存储，再尝试其余端点；Package Unpacking Bus 与 Package Storage Bus 的同值决胜以卸货端点的该正式扩展点实现，不修改玩家数值，也不依赖 part 挂载顺序。
```

AE2 Pattern Encoding Terminal runtime and newer UI references

```text
运行时源码：build/reference/ae2-v15.4.10
固定提交：b4b08d9941e3faecb520d76be617629bb56661e1（forge/v15.4.10）
主要文件：src/main/java/appeng/init/client/InitScreens.java
             src/main/java/appeng/menu/me/items/PatternEncodingTermMenu.java
             src/main/java/appeng/client/gui/me/items/PatternEncodingTermScreen.java
             src/main/java/appeng/client/gui/me/items/CraftingEncodingPanel.java
             src/main/java/appeng/client/gui/me/items/ProcessingEncodingPanel.java
             src/main/resources/assets/ae2/screens/terminals/pattern_encoding_terminal.json
用途：锁定原版 Screen、四种 panel、Menu 槽位验证和数量编辑调用链；普通终端只增加专用载体拒绝，不增加 package 入口、不替换 factory 或复制原生 panel。

新版源码：build/reference/ae2-neoforge-v19.2.17
固定提交：79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a（neoforge/v19.2.17）
主要文件：src/main/java/appeng/client/gui/me/items/PatternEncodingTermScreen.java
             src/main/java/appeng/client/gui/me/items/CraftingEncodingPanel.java
             src/main/java/appeng/client/gui/me/items/ProcessingEncodingPanel.java
             src/main/java/appeng/client/gui/me/items/SmithingTableEncodingPanel.java
             src/main/java/appeng/client/gui/me/items/StonecuttingEncodingPanel.java
             src/main/java/appeng/client/gui/me/patternaccess/PatternAccessTermScreen.java
             src/main/java/appeng/client/gui/widgets/TabButton.java
             src/main/java/appeng/client/gui/widgets/VerticalButtonBar.java
             src/main/java/appeng/client/gui/widgets/IconButton.java
             src/main/resources/assets/ae2/screens/terminals/pattern_encoding_terminal.json
用途：参考合并后 Advanced Pattern Encoding Terminal 的两套主体 profile、Encode/合成状态、样板槽占位、网络与 small scrollbar，以及保持 124x66 原宽的 package 面板；右侧模式按钮明确复用 Pattern Encoding Terminal 的 `TabButton.Style.HORIZONTAL` 视觉与坐标规则：22x22 normal/selected/focus、21px 步进和 `(3,3)` ItemStack 偏移。Pattern Access Terminal/VerticalButtonBar 只继续作为左侧公共工具栏参考。继续不采用新版 `VIEW_CELL` 面板，也不重实现四种原生模式。
版本边界：新版源码只作专用高级终端的视觉与布局参考；普通终端 factory、Screen/模式行为、Menu、网络与 recipe API 均严格使用 AE2 15.4.10 Forge。高级/包裹页在同一自有 Screen/Menu 内切换，不切换到原版 Screen。
许可证：LGPL-3.0-or-later；共享 sprite 中适配的上游像素许可证文本打包至 META-INF/licenses/ae2-LGPL-3.0-or-later.txt。
```

AE2 v19 Advanced Pattern Encoding Terminal part model reference

```text
官方仓库：https://github.com/AppliedEnergistics/Applied-Energistics-2
本地源码：build/reference/ae2-neoforge-v19.2.17
固定提交：79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a（neoforge/v19.2.17）
主要模型：src/main/resources/assets/ae2/models/part/display_base.json
          src/main/resources/assets/ae2/models/part/display_off.json
          src/main/resources/assets/ae2/models/part/pattern_encoding_terminal_on.json
          src/main/resources/assets/ae2/models/part/display_status_off.json
          src/main/resources/assets/ae2/models/part/display_status_on.json
          src/main/resources/assets/ae2/models/part/display_status_has_channel.json
          src/main/resources/assets/ae2/models/item/display_base.json
用途：高级样板终端的世界 Part 几何、物品几何、三层染色面和四段状态灯的固定上游来源。
兼容边界：只把模型中的 NeoForge `neoforge_data` 全亮字段等价改写为 Forge 1.20.1 `forge_data`；几何、UV、tint index 与光照值不变。运行时 Part 状态选择仍调用 AE2 15.4.10 API。
许可证与逐文件来源：META-INF/licenses/ae2-terminal-part-source.txt；LGPL-3.0-or-later 文本打包至 META-INF/licenses/ae2-LGPL-3.0-or-later.txt。
```

AE2 current main Storage Bus GUI reference

```text
官方仓库：https://github.com/AppliedEnergistics/Applied-Energistics-2
本地源码：build/reference/ae2-latest
固定提交：45f315517ea346efc0babd02c85c6b9d32dc8acf
主要文件：src/client/java/appeng/client/gui/AEBaseScreen.java
             src/client/java/appeng/client/gui/implementations/StorageBusScreen.java
             src/client/java/appeng/client/gui/widgets/IconButton.java
             src/client/java/appeng/client/gui/widgets/UpgradesPanel.java
             src/client/java/appeng/client/gui/widgets/VerticalButtonBar.java
             src/client/java/appeng/client/gui/style/Blitter.java
             src/main/resources/assets/ae2/screens/storage_bus.json
             src/main/resources/assets/ae2/textures/guis/states.png
             src/main/resources/assets/ae2/textures/guis/extra_panels.png
             src/main/resources/assets/ae2/textures/gui/sprites/vertical_buttons_bg.png
用途：Package Bus 的独立纹理渲染、0.2 alpha 可选槽、6px toolbar 间距、按钮 normal/hover/focus、连接目标提示、5px upgrade panel 和 (152,-5,20,20) Priority tab。
版本边界：仅回移客户端表现与布局；运行时依赖仍是 AE2 15.4.10 Forge。当前 main 使用每元素 TextureSetup/ARGB render state，1.20.1 通过立即式 Blitter 与明确 flush/state 边界实现等价隔离，不进行运行时合图。
资源核验：states、extra_panels、vertical_buttons_bg 与 neoforge/v19.2.17 对应文件 SHA-256 分别相同；复制文件保持原字节并按 LGPL-3.0-or-later 记录来源。
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

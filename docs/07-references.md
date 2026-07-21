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
用途：Minecraft 1.20.1 Forge 开发客户端的配方浏览，以及 Advanced Pattern Encoding Terminal 高级/包裹两页的可选 recipe transfer API。通用入口使用 JEI 标准 INPUT、OUTPUT、CATALYST、RENDER_ONLY 角色；Gradle 使用 common-api/forge-api compile-only 和完整 Forge runtime；不属于 Applied Packaging 的发布必需依赖，mods.toml 不声明 JEI。
AE2 JEI universal handler 参考：https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/integration/modules/jei/JEIPlugin.java
版本核对：https://maven.blamejared.com/mezz/jei/jei-1.20.1-forge/maven-metadata.xml
官方仓库：https://github.com/mezz/JustEnoughItems/tree/1.20.1
```

EMI 1.1.24+1.20.1 与 JEMI bridge

```text
用途：Minecraft 1.20.1 Forge 的 EMI 查看器与 Star Technology 的 JEI+EMI/JEMI 共存验证。Applied Packaging 对 EMI API compile-only 并声明可选 @EmiEntrypoint；原生 EMI handler 只处理 `FILL_BUTTON`，通过公开 backing recipe、ID 与 recipe manager/GTCEu registry 恢复语义配方。EMI 1.1.24 内置 JEMI 已取得 JEI recipe transfer handler 并调用 `transferRecipe()`，且 JEMI bridge recipe 的公开 ID 使用 `jei` namespace，因此 Applied Packaging 原生 EMI handler 可通过公共 API 让出该路径，无需判断 JEMI 实现类或解包 display。EMI 与 JEI 都不写入 mods.toml 硬依赖。
AE2 EMI handler 参考：https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/integration/modules/emi/EmiEncodePatternHandler.java
AE2 EMI plugin 参考：https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/integration/modules/emi/AppEngEmiPlugin.java
AE2 recipe recovery 参考：https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/integration/modules/emi/AbstractRecipeHandler.java
EMI 官方仓库 1.20 分支固定提交：23d251ea8ea3a5fd7d760948f36014b185eac69f
EMI Maven：https://maven.terraformersmc.com/releases/dev/emi/emi-forge/1.1.24%2B1.20.1/
核对日期：2026-07-20
```

Create 6.0.8-291（Minecraft 1.20.1）

```text
用途：Advanced Pattern Encoding Terminal 可选导入确定性的 Sequenced Assembly、ProcessingRecipe 和 Mechanical Crafting。动力合成按非空行/列中分组更少的一侧拆成包裹，数量相同时按行；Gradle 使用 slim compile-only 和 all runtime；mods.toml 不声明 Create。
官方 release：https://github.com/Creators-of-Create/Create/releases/tag/mc1.20.1-6.0.8
官方源码：https://github.com/Creators-of-Create/Create/tree/mc1.20.1-6.0.8
Maven metadata：https://maven.createmod.net/com/simibubi/create/create-1.20.1/maven-metadata.xml
核对日期：2026-07-17
```

GTCEu 7.5.3（Minecraft 1.20.1）

```text
用途：Advanced Pattern Encoding Terminal 可选导入确定性的 item/fluid GT recipe，包括 tick content × duration。Gradle 使用 full jar compile-only/runtime；mods.toml 不声明 GTCEu。
官方 release：https://github.com/GregTechCEu/GregTech-Modern/releases/tag/v7.5.3-1.20.1
官方仓库：https://github.com/GregTechCEu/GregTech-Modern
Maven metadata：https://maven.gtceu.com/releases/com/gregtechceu/gtceu/gtceu-1.20.1/maven-metadata.xml
核对日期：2026-07-17
```

GregTech Modern - StarT Fork 1.7.0b（Minecraft 1.20.1）

```text
用途：验证 GTCEu 可选语义编码器可在 StarT Fork 上运行。正常 compile API 固定为上游 GTCEu 7.5.3；PowerShell 命令使用 & .\gradlew.bat 'runGameTestServer' '-PgtceuRuntimeJar=<versioned-jar>'，把全部 ModDevGradle run 切换到独立 run-gtceu-fork 目录，不把 Fork 写成发布硬依赖。属性参数必须整体加引号，避免 .bat 调用把带版本号的路径拆开。本轮对照 Fork 与上游 jar，确认编码器使用的 GTRecipe、Content、ItemRecipeCapability、FluidRecipeCapability 以及公开 `GTRegistries.RECIPE_TYPES`、`GTRecipeType#getCategories()` / `getRecipesInCategory()` 保持兼容；Fork 额外提供 `LayeredRecipeHelper.getLayeredSteps`、`getXeiLayeredRecipe`、`calculateRecipeSteps` 及 `layered_steps` / `layered_xei` / `layered_info` 数据键，Applied Packaging 只对这些 Fork layer API 使用反射。原生 EMI recipe recovery 不读取 `GTEmiRecipe.recipe`，而是按公开 ID 从 recipe manager 或 GTCEu category registry 找回配方。
项目页：https://www.curseforge.com/minecraft/mc-mods/gregtech-modern-start-fork
核对文件：https://www.curseforge.com/minecraft/mc-mods/gregtech-modern-start-fork/files/8160693
核对日期：2026-07-17
```

Star Technology 星门装配配方

```text
用途：确认 `stargate_component_assembly` 的真实来源和分组语义。它由整合包 KubeJS startup script 注册为 GTCEu recipe type，category 为 `gate_construction`，recipe 通过 `event.recipes.gtceu.stargate_component_assembly(...).layeredRecipe(...)` 添加；因此不是独立 Java Mod category，也不能仅靠扁平 GTRecipe content 保留 layer 边界。实现读取 Fork 保存的真实 layer step，每个 layer 编成一个包裹列，layer 内全部材料保持同包，不增加直接依赖或泛 KubeJS API。
官方仓库 main 固定提交：a2527835993e33971a0f0127f94894cbe27b70ad
recipe type 注册：https://github.com/StarT-Dev-Team/Star-Technology/blob/main/kubejs/startup_scripts/machines/multiblocks/stargate_related_multiblocks/stargate_component_assembly.js
layered recipe 示例：https://github.com/StarT-Dev-Team/Star-Technology/blob/main/kubejs/server_scripts/systems/gate_based/dsg.js
核对日期：2026-07-18
```

常见模组 JEI/配方语义源码审计

```text
用途：通用 JEI 角色映射之外的保守语义门禁。下列仓库只用于核对 1.20.1 配方公开字段和 JEI role；它们不是 Applied Packaging 的 Gradle 或发布依赖。
Mekanism 1.20.x @ 96c736241e308a9cf1c73f7dcfa9aa221ea559d3：https://github.com/mekanism/Mekanism/tree/1.20.x
Immersive Engineering 1.20.1 @ e63e4824800945eccf3684200a2c2270e2e1cdf2：https://github.com/BluSunrize/ImmersiveEngineering/tree/1.20.1
CoFH ThermalCore 1.20.x @ cc7c230146a71d177767a8f9d6b26f0a52e9aeed：https://github.com/CoFH/ThermalCore/tree/1.20.x
CoFH ThermalExpansion 1.20.1 @ 9dc6061173c6732fd3bcdbf4d3c2baf3dc82f089：https://github.com/CoFH/ThermalExpansion/tree/1.20.1
Botania 1.20.x @ 62a548d8dbf511f87c769b0af633846e23f37c05：https://github.com/VazkiiMods/Botania/tree/1.20.x
PneumaticCraft 1.20.1 @ 2639a983eb9e3352517aed6dc03994a50e84883f：https://github.com/TeamPneumatic/pnc-repressurized/tree/1.20.1
Ars Nouveau 1.20 @ 2c74064bc753600d9b6600f102f8163e2d6764dd：https://github.com/baileyholl/Ars-Nouveau/tree/1.20
Industrial Foregoing 1.20 @ 1369806de4ceee84a86fcc0e57cc8f4724ad716f：https://github.com/InnovativeOnlineIndustries/Industrial-Foregoing/tree/1.20
Ender IO 1.20.1 @ 18ee1b17bf6879298e911fff751204b07f2e4228：https://github.com/Team-EnderIO/EnderIO/tree/1.20.1
结论：正确标记 CATALYST/RENDER_ONLY 的工具和展示槽不会被消费；公开 chance/probability、结果池、PneumaticCraft explosion loss、Ars reagent NBT 保留、IF laser drill 世界/权重产出、Botania Orechid 权重产出等无法精确编码的语义保守拒绝。若某模组把非消耗工具误标 INPUT，或只在 tooltip 隐藏概率而未暴露公开语义，需要再增加窄规则，不能宣称已自动推断。
核对日期：2026-07-17
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
用途：Formation Plane 的默认优先级为 0，并把玩家配置的原始数值直接用于挂载；它通过只实现 insert 的 MEStorage 成为网络输出端点而不提供库存/抽取。Package Unpacking Bus 保留该受限输入与 preferred-storage 路由语义，但按本 Mod 明确需求额外把本机唯一 held 工作包裹作为数量 1 的可抽取整包报告，不开放其它一般存储。Pattern Provider blocking 只在目标包含任一 pattern input 时拒绝 push；Package Unpacking Bus 仅复用其外部存储解析与 check-then-push 边界，阻挡模式按本 Mod 契约要求整个目标为空，不能直接复用 `containsPatternInput`。Storage Bus Partition Storage 从目标可用 key 重建配置槽。NetworkStorage 先按挂载优先级从高到低遍历，并在每个相同优先级组内先调用 `isPreferredStorageFor` 为真的存储，再尝试其余端点；Package Unpacking Bus 与 Package Storage Bus 的同值决胜以卸货端点的该正式扩展点实现，不修改玩家数值，也不依赖 part 挂载顺序。
```

AE2 Pattern Encoding Terminal runtime and newer UI references

```text
运行时源码：build/reference/ae2-v15.4.10
固定提交：b4b08d9941e3faecb520d76be617629bb56661e1（forge/v15.4.10）
主要文件：src/main/java/appeng/init/client/InitScreens.java
             src/main/java/appeng/menu/me/items/PatternEncodingTermMenu.java
             src/main/java/appeng/integration/modules/jei/transfer/EncodePatternTransferHandler.java
             src/main/java/appeng/integration/modules/jeirei/EncodingHelper.java
             src/main/java/appeng/menu/me/common/GridInventoryEntry.java
             src/main/java/appeng/client/gui/me/items/PatternEncodingTermScreen.java
             src/main/java/appeng/client/gui/me/items/CraftingEncodingPanel.java
             src/main/java/appeng/client/gui/me/items/ProcessingEncodingPanel.java
             src/main/resources/assets/ae2/screens/terminals/pattern_encoding_terminal.json
用途：锁定原版 Screen、四种 panel、Menu 槽位验证和数量编辑调用链；同时锁定 recipe transfer 的替代材料选择顺序：网络条目按 craftable、undamaged、stored amount 排序，玩家物品栏为网络之后的后备，物品 Ingredient 会匹配 client repo 中的实际 AEItemKey 变体。普通终端只增加专用载体拒绝，不增加 package 入口、不替换 factory 或复制原生 panel。

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
用途：参考合并后 Advanced Pattern Encoding Terminal 的共享 195x245 主体、Encode/合成状态、样板槽占位、网络与 small scrollbar，以及同一 `(8,68,132,78)` 锚点的高级/包裹面板后绘制；右侧模式按钮复用 Pattern Encoding Terminal 的 `TabButton.Style.HORIZONTAL` 视觉与坐标规则：22x22 normal/selected/focus、21px 步进，但图标直接绘制 sprite 而非 ItemStack。Pattern Access Terminal/VerticalButtonBar 只继续作为左侧公共工具栏参考。继续不采用新版 `VIEW_CELL` 面板，也不重实现四种原生模式。
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

AE2 v19 ME Chest dual-menu GUI reference

```text
官方仓库：https://github.com/AppliedEnergistics/Applied-Energistics-2
本地源码：build/reference/ae2-neoforge-v19.2.17
固定提交：79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a（neoforge/v19.2.17）
主要文件：src/main/java/appeng/block/storage/MEChestBlock.java
          src/main/java/appeng/menu/implementations/MEChestMenu.java
          src/main/java/appeng/menu/implementations/BasicCellChestMenu.java
          src/client/java/appeng/client/gui/implementations/MEChestScreen.java
          src/main/resources/assets/ae2/screens/me_chest.json
          src/main/resources/assets/ae2/screens/terminals/base_terminal.json
          src/main/resources/assets/ae2/textures/guis/me_chest.png
用途：序列缓存器按“端点主库存/其它成员中央单槽”复用 ME Chest 的 main/side 菜单语义、3x9 terminal 切片坐标与侧面槽坐标。
版本边界：运行时 Menu、网络和 Screen API 仍使用 AE2 15.4.10 Forge；仅回移布局与视觉语义。`me_chest.png` 原字节副本按 LGPL-3.0-or-later 记录，用户主底图继续视为用户提供资产。
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

AE2 15.4.10 ME Interface 通用 capability 与容量元件源码

```text
本地源码：build/reference/ae2-v15.4.10
主要文件：src/main/java/appeng/blockentity/misc/InterfaceBlockEntity.java
          src/main/java/appeng/helpers/InterfaceLogic.java
          src/main/java/appeng/api/behaviors/GenericInternalInventory.java
          src/main/java/appeng/init/InitCapabilities.java
          src/main/java/appeng/items/storage/StorageTier.java
用途：Package Assembler 只复用 ME Interface 的 `GenericInternalInventory` + `Capabilities.STORAGE` 暴露方式，不自行实现 `IItemHandler`；item/fluid 和附属类型 capability 由 AE2 generic wrapper 派生。`StorageTier` 给出 1k/16k/64k/256k 名义容量，空槽采用 1k 档，其余容量元件档的包裹单位上限同样按产品规则取四分之一。
```

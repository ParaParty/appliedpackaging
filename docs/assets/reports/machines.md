# Machines Asset Report

## 2026-07-15 ME Packager 正式模型与动画接入

正式 `me_packager` 直接使用用户在 `E:/resources/textures/appliedpackaging/ret/model` 提供的 Blockbench 模型和三张贴图。PNG 仅做原字节复制，运行时与来源 SHA-256 一致：

```text
model.bbmodel    58a824e49f1e5b814956e2bec6f422ecd71a44877567e970daf4ca6dcb5cbf26
base.png         c98e01d32207cd77d50c1b5aee5176fbd40264e16badf2569737003a7dd6385e
curtain.png      d4a4bcc86b497cad066f364ce8e187d616283925fe6d58140934b9ebe1893f02
belt_scroll.png  edd93ff96c09554b23b5b70266f5d2320c2a19aac6f66445f8a4b9240379ba04
```

模型拆分与运行时职责：

```text
body.json          6 个静态主体 cube
belt.json          1 个动态传送带 cube；上表面和工作口正面共用连续 UV
curtain_flap.json  1 条帘子局部模型；renderer 平移复用为 4 条并绕顶部转轴摆动
item.json          11 个 cube 的静止物品预览；继承 minecraft:block/block 标准物品显示变换
```

`belt_scroll.png` 是 32x32、横向两个连续 16px 周期；一个运行时窗口严格为上表面 15px + 工作口正面 1px，工作期间按 1px/tick 计算 U/X offset，因此滚动到周期末也不会采样到 atlas 相邻 sprite。帘子不使用逐帧贴图，而是按 20 tick 机器进度做单峰摆角：拆包向内、打包向外。包裹沿传送带移动并保留方块体积 stencil 裁切，机器事务提交时序未修改。

方向约定：源模型本地 +X 是工作口；运行时 `facing` 是工作口方向，north/east/south/west 对应 Y 旋转 270/0/90/180 度。`network_side` 仅指定 AE 接线面，不参与主体或动画旋转；因此放在地面得到 `network_side=down` 时，底盘仍保持水平。方块物品的完整 11-cube 模型继承 `minecraft:block/block`，使用原版标准 GUI、地面、固定、第三人称和第一人称方块变换。

可编辑源和确定性导入记录保存在：

```text
docs/assets/source/me_packager/model.bbmodel
docs/assets/source/me_packager/belt_scroll.aseprite
docs/assets/source/me_packager/import-report.json
scripts/import-me-packager-model.py
```

旧 `models/block/me_packager_create/`、`textures/block/me_packager_create/` 和 hatch/tray additional models 已删除；下方 2026-07-05 小节只保留历史来源记录。

验证：`assetgen validate-contract`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1`、`runData`、100/100 required GameTest、客户端 smoke、`build` 和 `verify-release.ps1 -RequireAssetContracts` 均通过。朝向修正后的客户端 smoke 以 `facing=north,network_side=south` 刷新 11 张截图，确认工作口朝 north、背面接线朝 south，主体保持直立且传送带/帘子与主体共用同一朝向，无 missing model/texture。

## 2026-07-13 AE2 v19 Package Assembler Replacement

`package_assembler` now uses the official AE2 `neoforge/v19.2.17` molecular assembler model at commit
`79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a`, adapted to the Applied Packaging namespace and Forge 1.20.1 model data.
The complete base surface is the byte-preserved user file `E:/resources/textures/appliedpackaging/ret/package_assembler.png`
(SHA-256 `345A070081B556D2EF44AE0DAB65210F7728C33BB7C29FD46B526C607605FCE0`). The official animated
light strip and light model are retained under the bundled AE2 LGPL license. `PackageAssemblerRenderer` follows the
upstream animation: while crafting it renders the active package in the chamber, the animated full-bright light layer,
and AE2 crafting particles moving toward the chamber center.

The superseded `package_assembler_front/side/top/frame.png` files were removed on 2026-07-13 because no runtime model
referenced them. Their historical preview remains under `docs/assets/previews/` only.

Runtime resource paths:

```text
src/main/resources/assets/appliedpackaging/models/block/package_assembler.json
src/main/resources/assets/appliedpackaging/models/block/package_assembler_lights.json
src/main/resources/assets/appliedpackaging/textures/block/package_assembler.png
src/main/resources/assets/appliedpackaging/textures/block/package_assembler_lights.png
src/main/resources/assets/appliedpackaging/textures/block/package_assembler_lights.png.mcmeta
```

## 2026-07-05 ME Packager 临时 Create 同款替换

本次按用户要求将 `me_packager` 临时切换为 Create Packager 同款模型/贴图资源，等待后续正式打包机模型替换。

参考来源：

```text
build/reference/create
Create mc1.20.1/dev
assets/create/models/block/packager/*.json
assets/create/textures/block/packager_*.png
assets/create/textures/block/factory_panel_packager_mode.png
```

整合方式：

```text
将 packager 模型与贴图复制到 appliedpackaging namespace 下：
  src/main/resources/assets/appliedpackaging/models/block/me_packager_create/
  src/main/resources/assets/appliedpackaging/textures/block/me_packager_create/
models/block/me_packager.json parent 指向 me_packager_create/block_linked。
models/item/me_packager.json parent 指向 me_packager_create/item。
blockstates/me_packager.json 已包含 facing 与 network_side 组合；network_side=up/down 使用 vertical linked 临时模型，其它方向使用 horizontal linked 临时模型。
```

验收点：

```text
不再引用 create: namespace。
保留 Create-style linked 外观作为临时连接方向提示；当前资源只能区分水平/竖直连接，不表达精确单面动画。
旧 Applied Packaging `me_packager_front/side/top/frame.png` 已于 2026-07-13 删除；它们没有被当前模型引用，不再作为发布资源或资产门禁输入。历史预览只保留在 `docs/assets/previews/`。
```

## Scope

Production texture pass for machine block faces only:

```text
me_packager
package_assembler
```

Only these paths were intentionally written:

```text
src/main/resources/assets/appliedpackaging/textures/block/me_packager_front.png
src/main/resources/assets/appliedpackaging/textures/block/me_packager_side.png
src/main/resources/assets/appliedpackaging/textures/block/me_packager_top.png
src/main/resources/assets/appliedpackaging/textures/block/me_packager_frame.png
docs/assets/previews/me_packager_preview_sheet.png
src/main/resources/assets/appliedpackaging/textures/block/package_assembler_front.png
src/main/resources/assets/appliedpackaging/textures/block/package_assembler_side.png
src/main/resources/assets/appliedpackaging/textures/block/package_assembler_top.png
src/main/resources/assets/appliedpackaging/textures/block/package_assembler_frame.png
docs/assets/previews/package_assembler_preview_sheet.png
docs/assets/reports/machines.md
```

Java, Gradle, item textures, terminal/bus textures, GUI icons, models, blockstates, language files, and other documents were not modified by this pass.

## Source References

Project contracts and briefs:

```text
docs/assets/palette.md
docs/assets/asset-briefs/machines.md
docs/assets/contracts/me_packager.yaml
docs/assets/contracts/package_assembler.yaml
```

AE2 visual reference sheets, used for style and material language only:

```text
build/asset-reference/ae2/ae2-machines-visual.png
build/asset-reference/ae2/ae2-parts-visual.png
build/asset-reference/ae2/ae2-guis-visual.png
```

Additional broad concept reference from ImageGen:

```text
build/asset-reference/concepts/applied-packaging-ae2-style-board.png
```

The AE2 assets and the concept-board art were not copied, traced, pasted, or downsampled into the final PNGs. They were used only for broad shape, material, contrast, and role-readability direction.

## Method

The final files were hand-pixeled with a deterministic local Pillow script at native 32x32 resolution so the existing block model texture names remain unchanged.

Shared material language:

```text
pale quartz block body
dark beveled machine frame and corner caps
subdued metal recesses
cyan/fluix/purple glow accents
large readable modules
low micro-detail
no text, labels, arrows, watermarks, cardboard body, or brass gears
```

`me_packager` visual decisions:

```text
front: dark recessed inventory/package I/O slot with cyan and purple process lanes
side: adjacent-inventory scanner grille with cyan/purple scan lanes
top: quartz top plate with redstone status lamp and broad purple package band
frame: dark machine underside/edge texture
```

`package_assembler` visual decisions:

```text
front: white framed assembly chamber with a purple 3x3 chamber/grid read and cyan/fluix cells
side: vertical fluix glass chamber channel in quartz paneling
top: central fluix chamber port with cyan core
frame: dark recessed assembly-frame texture
```

## Preview Records

Each preview sheet is a 2x2 equivalent preview:

```text
top-left: front
top-right: right side
bottom-left: top
bottom-right: isometric sketch preview
```

Preview paths:

```text
docs/assets/previews/me_packager_preview_sheet.png
docs/assets/previews/package_assembler_preview_sheet.png
```

## Validation

Commands run:

```powershell
python "C:\Users\warmt\.codex\skills\minecraft-mod-asset-generation\scripts\assetgen" validate-contract "docs/assets/contracts/me_packager.yaml"
python "C:\Users\warmt\.codex\skills\minecraft-mod-asset-generation\scripts\assetgen" validate-contract "docs/assets/contracts/package_assembler.yaml"
```

Result:

```text
me_packager contract: ok, no warnings, no errors
package_assembler contract: ok, no warnings, no errors
```

PNG validation:

```text
me_packager_front.png: 32x32 RGBA, alpha 255..255
me_packager_side.png: 32x32 RGBA, alpha 255..255
me_packager_top.png: 32x32 RGBA, alpha 255..255
me_packager_frame.png: 32x32 RGBA, alpha 255..255
me_packager_preview_sheet.png: 68x68 RGBA, alpha 255..255
package_assembler_front.png: 32x32 RGBA, alpha 255..255
package_assembler_side.png: 32x32 RGBA, alpha 255..255
package_assembler_top.png: 32x32 RGBA, alpha 255..255
package_assembler_frame.png: 32x32 RGBA, alpha 255..255
package_assembler_preview_sheet.png: 68x68 RGBA, alpha 255..255
```

JSON and texture-reference validation:

```text
blockstates/me_packager.json parsed
blockstates/package_assembler.json parsed
models/block/me_packager.json parsed
models/block/package_assembler.json parsed
models/item/me_packager.json parsed
models/item/package_assembler.json parsed
model element coordinates stay in 0..16
me_packager particle/front/side/top/frame texture refs exist
package_assembler particle/front/side/top/frame texture refs exist
```

Visual inspection:

```text
me_packager_preview_sheet.png inspected
package_assembler_preview_sheet.png inspected
```

## Known Limitations

```text
This pass did not run Gradle build, runData, GameTest, runClient, or runServer because it changed only static block PNG textures and the machines asset report.
The preview sheets are lightweight equivalent previews, not renderer-backed model renders.
Preview sheets live under docs/assets/previews and are not shipped as Minecraft block atlas resources.
```

## Main-thread Integration Validation

```text
Second-pass machine textures were reviewed against AE2 forge/v15.4.10 reference sheets and the Applied Packaging ImageGen concept board.
No AE2 pixels were copied into project assets.
53 project PNG dimensions/modes/model references passed.
.\gradlew.bat runData succeeded.
.\gradlew.bat build succeeded.
.\gradlew.bat runGameTestServer succeeded with 29 required tests passing.
```

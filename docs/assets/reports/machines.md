# Machines Asset Report

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
旧 Applied Packaging me_packager_* PNG 暂留在资源树中，等待正式模型替换时清理或复用。
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

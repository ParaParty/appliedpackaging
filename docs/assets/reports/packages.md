# Packages 资产报告

## 2026-07-05 package_box_pixel_v7 修复

本次将 17 色包裹从独立平面 item PNG 切换为 3D package_box item/entity 共用模型，未修改 `package_pattern` 与 `packaged_processing_pattern` 图标。

来源：

```text
C:\Users\warmt\Downloads\package_box_pixel_v7.zip
package_box_pixel_v7/manifest.json
package_box_pixel_v7/models/block/package_box_10x10x8_fluix.json
package_box_pixel_v7/textures/variants/<color>/package_box_<face>.png
```

整合方式：

```text
删除旧的 src/main/resources/assets/appliedpackaging/textures/item/<color>_package.png 平面图标。
为每个颜色生成 src/main/resources/assets/appliedpackaging/models/item/<color>_package.json。
为每个颜色生成 src/main/resources/assets/appliedpackaging/models/item/package_box/<color>.json。
复制每个颜色 front/back/side/top/bottom 五张 face 贴图到 textures/block/package_box/<color>/。
包裹掉落实体 appliedpackaging:package 渲染同一 item model。
package_box 模型使用单个 10x10x8 cuboid；每个 face 绑定独立完整 face 贴图并声明 full-face uv [0,0,16,16]。
没有使用 atlas 裁切，也没有把基础盒体与束带拆成重叠模型层。
存在物品 marker 时，顶层 item model 通过 has_marker override 切入客户端 renderer，在前脸右下角、距外边框 1px 的 4x4 框内叠加 3x3 marker item。
```

验收点：

```text
17 色包裹仍保留独立 item id 和独立名称。
item 与实体不再使用独立平面包裹图标。
front/back/side 为 10x8，top/bottom 为 10x10。
模型使用 appliedpackaging namespace，不引用 create: 或外部 namespace。
当前仓库内 17 色 x 5 面 PNG 与 package_box_pixel_v7.zip 内对应 PNG 逐字节一致。
`scripts/verify-assets.ps1` 会检查 package_box 模型使用 full-face uv [0,0,16,16] 和 marker custom-render override，`scripts/test-assets-audit.ps1` 包含 cropped UV 与 missing marker override 负例。
```

## 2026-07-02 AE2 参考二次修复

本次只重绘 package 与 pattern item 纹理，未修改 Java、Gradle、block texture、GUI icon、model 或其他设计文档。

参考来源：

```text
build/asset-reference/ae2/ae2-items-visual.png
build/asset-reference/ae2/ae2-parts-visual.png
build/asset-reference/concepts/applied-packaging-ae2-style-board.png
```

使用方式：

```text
仅作为材质语言和形状方向参考：quartz-white plates、dark gray bevels、cyan/fluix/purple glow、clean 32x32 readability。
未复制 AE2 或概念图像素，未从参考图抠图、缩放或派生逐像素资产。
最终纹理由确定性手绘像素脚本生成，保持 Applied Packaging 原创轮廓。
```

二次修复重点：

```text
包裹从小型平板感改为更清晰的 sealed logistics cube item silhouette。
主体保留浅色 quartz body、深灰角扣、顶部 fluix seal、正面 fluix latch。
17 色使用更强的水平 wrap band，并保留同一 alpha 轮廓。
pattern items 继续使用 AE2 风格暗色卡壳与金色触点，但中心语义改为 package glyph / multi-package marks。
```

验证：

```powershell
python C:\Users\warmt\.codex\skills\minecraft-mod-asset-generation\scripts\assetgen validate-contract docs/assets/contracts/package_items.yaml
```

结果：

```text
ok: true
warnings: []
errors: []
```

已运行本地 PNG 校验：

```text
19 个目标 PNG 均为 32x32 RGBA
四角 alpha 均为 0
alpha 只包含 0/255
17 个 *_package.png alpha 轮廓完全一致
每个包裹变体包含可见的精确 palette band color，最少 19 px，white/yellow/light_gray 为 30 px
16x16 BOX downsample 后仍有清晰非透明覆盖
最近 palette color 距离为 60.11，brown_package 与 red_package 仍可区分
package_pattern 与 packaged_processing_pattern 非同图
```

抽样视觉检查：

```text
fluix_package.png
red_package.png
black_package.png
white_package.png
yellow_package.png
package_pattern.png
packaged_processing_pattern.png
```

未运行：

```text
未运行 Gradle/client 验证；本次范围限定为静态 item PNG，且工作树已有他人 Java/resource 改动，避免把无关状态混入本次材质验收。
```

## 2026-07-02 生产阻塞修复

本次只重绘 package 与 pattern item 纹理，未修改 Java、Gradle、block texture、GUI icon、model 或其他设计文档。

方法：

```text
读取 minecraft-mod-asset-generation 与 imagegen skill 指令及相关 prompt/quality 文档。
以 docs/assets/contracts/package_items.yaml、docs/assets/asset-briefs/packages.md、docs/assets/palette.md 为约束。
采用 skill-consistent 的确定性手绘像素流程生成最终 32x32 RGBA PNG，没有调用外部 provider。
17 色包裹共享同一 alpha 轮廓，只替换束带/封签颜色。
```

修复重点：

```text
包裹图标放大为更清晰的 AE2 封装数据盒轮廓。
主体使用浅色 quartz panel，四角使用 dark metal 角扣。
每个变体保留明显彩色束带/封签，并固定小型 Fluix latch。
black/gray/white/light_gray/yellow 等低对比颜色增加可读高光或阴影。
package_pattern 绘制为空白 AE2 pattern card + 小包裹封印。
packaged_processing_pattern 绘制为 processing card + 多个小型包裹标记。
```

验证：

```powershell
python C:\Users\warmt\.codex\skills\minecraft-mod-asset-generation\scripts\assetgen validate-contract docs/assets/contracts/package_items.yaml
```

结果：

```text
ok: true
warnings: []
errors: []
```

已运行本地 PNG 校验：

```text
19 个目标 PNG 均为 32x32 RGBA
四角 alpha 均为 0
alpha 只包含 0/255
17 个 *_package.png alpha 轮廓完全一致
16x16 BOX downsample 后仍有清晰非透明覆盖
采样束带颜色最近距离为 46.04，17 色仍可区分
```

抽样视觉检查：

```text
fluix_package.png
black_package.png
package_pattern.png
packaged_processing_pattern.png
```

未运行：

```text
未运行 Gradle/client 验证；本次范围限定为静态 item PNG，且工作树已有他人 Java/resource 改动，避免把无关状态混入本次材质验收。
```

## 范围

本次交付 `packages` 资产包内当前已注册的 19 个 item：

```text
fluix_package
white_package
orange_package
magenta_package
light_blue_package
yellow_package
lime_package
pink_package
gray_package
light_gray_package
cyan_package
purple_package
blue_package
brown_package
green_package
red_package
black_package
package_pattern
packaged_processing_pattern
```

## 生成方法

使用 `docs/assets/contracts/package_items.yaml` 作为 contract，按 `docs/assets/asset-briefs/packages.md` 和 `docs/assets/palette.md` 手绘式脚本生成 32x32 RGBA 像素图。

没有使用随机图片生成输出。绘制约束相当于以下 brief：

```text
绘制透明背景、居中的 Minecraft item icon。
17 色包裹使用同一 AE2 风格封装数据盒轮廓：浅灰石英盒体、深灰金属角扣、中心 Fluix 菱形封印、一条清晰颜色束带和小型数据封签。
每个颜色变体只替换束带和小封签颜色。
package_pattern 是空白样板卡片加小包裹封印。
packaged_processing_pattern 是处理样板卡片加多个小包裹标记。
避免纸箱、文字、数字、箭头、水印和黄铜齿轮。
```

## 输出文件

纹理路径：

```text
src/main/resources/assets/appliedpackaging/textures/item/fluix_package.png
src/main/resources/assets/appliedpackaging/textures/item/white_package.png
src/main/resources/assets/appliedpackaging/textures/item/orange_package.png
src/main/resources/assets/appliedpackaging/textures/item/magenta_package.png
src/main/resources/assets/appliedpackaging/textures/item/light_blue_package.png
src/main/resources/assets/appliedpackaging/textures/item/yellow_package.png
src/main/resources/assets/appliedpackaging/textures/item/lime_package.png
src/main/resources/assets/appliedpackaging/textures/item/pink_package.png
src/main/resources/assets/appliedpackaging/textures/item/gray_package.png
src/main/resources/assets/appliedpackaging/textures/item/light_gray_package.png
src/main/resources/assets/appliedpackaging/textures/item/cyan_package.png
src/main/resources/assets/appliedpackaging/textures/item/purple_package.png
src/main/resources/assets/appliedpackaging/textures/item/blue_package.png
src/main/resources/assets/appliedpackaging/textures/item/brown_package.png
src/main/resources/assets/appliedpackaging/textures/item/green_package.png
src/main/resources/assets/appliedpackaging/textures/item/red_package.png
src/main/resources/assets/appliedpackaging/textures/item/black_package.png
src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png
src/main/resources/assets/appliedpackaging/textures/item/packaged_processing_pattern.png
```

模型路径：

```text
src/main/resources/assets/appliedpackaging/models/item/fluix_package.json
src/main/resources/assets/appliedpackaging/models/item/white_package.json
src/main/resources/assets/appliedpackaging/models/item/orange_package.json
src/main/resources/assets/appliedpackaging/models/item/magenta_package.json
src/main/resources/assets/appliedpackaging/models/item/light_blue_package.json
src/main/resources/assets/appliedpackaging/models/item/yellow_package.json
src/main/resources/assets/appliedpackaging/models/item/lime_package.json
src/main/resources/assets/appliedpackaging/models/item/pink_package.json
src/main/resources/assets/appliedpackaging/models/item/gray_package.json
src/main/resources/assets/appliedpackaging/models/item/light_gray_package.json
src/main/resources/assets/appliedpackaging/models/item/cyan_package.json
src/main/resources/assets/appliedpackaging/models/item/purple_package.json
src/main/resources/assets/appliedpackaging/models/item/blue_package.json
src/main/resources/assets/appliedpackaging/models/item/brown_package.json
src/main/resources/assets/appliedpackaging/models/item/green_package.json
src/main/resources/assets/appliedpackaging/models/item/red_package.json
src/main/resources/assets/appliedpackaging/models/item/black_package.json
src/main/resources/assets/appliedpackaging/models/item/package_pattern.json
src/main/resources/assets/appliedpackaging/models/item/packaged_processing_pattern.json
```

每个 item model 使用：

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "appliedpackaging:item/<item_id>"
  }
}
```

## 预览记录

预览以最终 PNG 为准：

```text
src/main/resources/assets/appliedpackaging/textures/item/fluix_package.png
src/main/resources/assets/appliedpackaging/textures/item/white_package.png
src/main/resources/assets/appliedpackaging/textures/item/black_package.png
src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png
src/main/resources/assets/appliedpackaging/textures/item/packaged_processing_pattern.png
```

主 agent 整合时可在创造栏或物品栏复查 17 色区分度。

## 验证

已运行：

```powershell
python C:\Users\warmt\.codex\skills\minecraft-mod-asset-generation\scripts\assetgen validate-contract docs/assets/contracts/package_items.yaml
```

结果：

```text
ok: true
warnings: []
errors: []
```

已运行本地文件校验：

```text
validated 19 item textures/models; package alpha silhouettes match
```

校验内容：

```text
所有 PNG 存在
所有 PNG 为 32x32 RGBA
所有 PNG 四角透明且非空
所有 item model JSON 可解析
所有 item model 使用 minecraft:item/generated
所有 layer0 指向 appliedpackaging:item/<item_id>
17 色包裹 alpha 轮廓一致
```

已运行：

```powershell
.\gradlew.bat build
```

结果：

```text
BUILD SUCCESSFUL in 2s
```

## 已知限制

```text
未运行 runClient；本次 subagent 只做静态资源交付，尚未在游戏创造栏中截图复查。
未运行 runData；该任务可能写入 src/generated/resources，超出本次 packages subagent 的源文件写入范围。
未运行 runGameTestServer；本次变更只新增 item 纹理和 item model，不涉及行为逻辑。
```

## 主线程集成验证

```text
二轮材质已按 AE2 forge/v15.4.10 reference sheet 和 Applied Packaging ImageGen 概念板复审。
未逐像素复制 AE2 资产。
53 个项目 PNG 尺寸/模式/模型引用检查通过。
.\gradlew.bat runData 成功。
.\gradlew.bat build 成功。
.\gradlew.bat runGameTestServer 成功，29 个必需 GameTest 全部通过。
```

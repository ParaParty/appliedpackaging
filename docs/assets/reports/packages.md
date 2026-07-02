# Packages 资产报告

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

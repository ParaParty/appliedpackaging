# Brief: Packages

## 范围

本资产包负责当前已注册的包裹和样板物品。

输出文件：

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
src/main/resources/assets/appliedpackaging/models/item/*.json
docs/assets/reports/packages.md
```

## 视觉

包裹不是纸箱。它是 AE2 风格的封装数据盒：

```text
浅灰石英盒体
深灰金属角扣
中心 Fluix 菱形封印
一条明显的颜色束带
小型封签或数据槽
透明背景
```

17 色包裹应共享同一轮廓，只替换束带/封签颜色。`fluix_package` 使用 Fluix 紫蓝束带，不使用纯白或纸箱棕色。

样板物品：

```text
package_pattern:
  空白样板卡片 + 小包裹封印

packaged_processing_pattern:
  普通处理样板卡片 + 多个小型包裹标记
```

## 约束

```text
生成/绘制时可使用 32x32 源图，但最终 Minecraft item texture 需可在 16x16 读清
不要添加文字、数字、水印或箭头
不要改变 17 色包裹主体形状
不要创建玩家可获得的空纸箱图标心智
```

## 验收

```text
所有 19 个当前注册物品都有 PNG 和 item model JSON
17 色包裹在小尺寸下仍能区分
两个 pattern 物品与包裹明显不同
docs/assets/reports/packages.md 记录生成提示、修改说明和预览路径
```

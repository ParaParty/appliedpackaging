# Brief: Packages

## 范围

本资产包负责当前已注册的包裹和样板物品。

输出文件：

```text
src/main/resources/assets/appliedpackaging/models/item/<color>_package.json
src/main/resources/assets/appliedpackaging/models/item/package_box/<color>.json
src/main/resources/assets/appliedpackaging/textures/block/package_box/<color>/package_box_front.png
src/main/resources/assets/appliedpackaging/textures/block/package_box/<color>/package_box_back.png
src/main/resources/assets/appliedpackaging/textures/block/package_box/<color>/package_box_side.png
src/main/resources/assets/appliedpackaging/textures/block/package_box/<color>/package_box_top.png
src/main/resources/assets/appliedpackaging/textures/block/package_box/<color>/package_box_bottom.png
src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png
src/main/resources/assets/appliedpackaging/textures/item/advanced_processing_pattern.png
docs/assets/reports/packages.md
```

## 视觉

当前 17 色包裹使用用户提供的 package_box_pixel_v7 盒体版本。它是可作为 item 和掉落实体渲染的 10x10x8 包裹模型：

```text
front/back/side 为 10x8
top/bottom 为 10x10
同一盒体轮廓
颜色由对应束带变体决定
item 不再有独立平面 PNG，直接渲染模型
```

17 色包裹应共享同一轮廓，只替换束带/封签颜色。`fluix_package` 使用 Fluix 紫蓝束带，不使用纯白或纸箱棕色。

样板物品：

```text
package_pattern:
  空白样板卡片 + 小包裹封印

advanced_processing_pattern:
  普通处理样板卡片 + 多个小型包裹标记
```

## 约束

```text
包裹模型贴图必须保持 package_box_pixel_v7 的 face 尺寸，不要重新生成平面 item 图标
package_box 模型使用单个 10x10x8 cuboid；每个 face 绑定独立完整 face 贴图并声明 full-face uv [0,0,16,16]
不要合并 atlas 裁切，不要把基础盒体与束带拆成重叠模型层
存在物品 marker 时，由客户端 renderer 在前脸右下角、距外边框 1px 的 4x4 框内叠加 3x3 marker item
不要添加文字、数字、水印或箭头
不要改变 17 色包裹主体形状
不要创建玩家可获得的空纸箱图标心智
```

## 验收

```text
17 色包裹都有 item model JSON、package_box model JSON 和五面贴图
package_pattern 与 advanced_processing_pattern 有 PNG 和 item model JSON
17 色包裹在小尺寸下仍能区分
两个 pattern 物品与包裹明显不同
docs/assets/reports/packages.md 记录生成提示、修改说明和预览路径
```

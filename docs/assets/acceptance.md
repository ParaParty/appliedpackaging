# 资产验收

本文定义 subagent 交付和主 agent 整合时的资产验收标准。

## 1. 通用检查

每个资产包必须包含：

```text
目标资源文件
对应模型或 blockstate 文件
docs/assets/reports/<asset-package>.md
生成提示或绘制说明
预览路径或渲染记录
已知限制
```

文件要求：

```text
PNG 可被读取
透明背景 alpha 干净
文件路径使用 appliedpackaging namespace
JSON 可解析
语言 key 与物品/方块 id 对齐
不覆盖其他 subagent 的文件
不修改 Java、Gradle 或核心设计文档
```

## 2. Item 图标

必须满足：

```text
最终 PNG 为 16x16 或 32x32，优先 32x32 源图再 downsample/检查 16x16 可读性
透明背景
图标居中，有 1-2 像素安全边距
17 色包裹只改变束带/封签色，不改变主体轮廓
package_pattern 与 packaged_processing_pattern 在小尺寸下可区分
```

路径：

```text
src/main/resources/assets/appliedpackaging/textures/item/<item_id>.png
src/main/resources/assets/appliedpackaging/models/item/<item_id>.json
```

## 3. 方块和机器

必须满足：

```text
方块纹理来源可追溯到 front/right/top/isometric 概念或手绘说明
模型坐标在 0..16
无 missing texture
物品模型能正常显示
破坏粒子使用合理纹理
前向面能明确表示功能
```

路径：

```text
src/main/resources/assets/appliedpackaging/textures/block/<block_id>*.png
src/main/resources/assets/appliedpackaging/blockstates/<block_id>.json
src/main/resources/assets/appliedpackaging/models/block/<block_id>.json
src/main/resources/assets/appliedpackaging/models/item/<block_id>.json
```

## 4. UI 和图标

必须满足：

```text
图标语义清楚，不依赖文字标签
8x8、12x12、16x16 尺寸下仍可识别
颜色、marker、容量、阻挡、过滤、打包、拆包状态可区分
与 AE2/Applied Packaging 面板风格一致
```

路径：

```text
src/main/resources/assets/appliedpackaging/textures/gui/<screen_id>.png
src/main/resources/assets/appliedpackaging/textures/gui/icons/<icon_id>.png
```

## 5. 主 agent 验收命令

整合后至少运行：

```powershell
.\gradlew.bat build
.\gradlew.bat runData
.\gradlew.bat runGameTestServer
```

当方块/GUI 已注册后追加：

```powershell
.\gradlew.bat runClient
```

客户端验收应检查：

```text
创造栏和物品栏无 missing texture
包裹 17 色可区分
机器方块无紫黑贴图
GUI 元素无错位
日志无资源加载错误
```

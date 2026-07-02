# Brief: Machines

## 范围

本资产包负责两个核心机器的未来方块资产。

输出文件：

```text
src/main/resources/assets/appliedpackaging/textures/block/package_assembler*.png
src/main/resources/assets/appliedpackaging/textures/block/me_packager*.png
src/main/resources/assets/appliedpackaging/blockstates/package_assembler.json
src/main/resources/assets/appliedpackaging/blockstates/me_packager.json
src/main/resources/assets/appliedpackaging/models/block/package_assembler.json
src/main/resources/assets/appliedpackaging/models/block/me_packager.json
src/main/resources/assets/appliedpackaging/models/item/package_assembler.json
src/main/resources/assets/appliedpackaging/models/item/me_packager.json
docs/assets/reports/machines.md
```

## ME 包裹装配室

职责心智：

```text
读取 AE2 样板输入
按颜色生成一个或多个包裹
不扫描相邻存储
不拆包
```

视觉：

```text
分子装配室轮廓
透明 Fluix 装配腔
中心悬浮包裹投影
样板接口纹理
17 色小灯条
输出/阻挡状态灯
```

## ME 打包机

职责心智：

```text
扫描相邻存储端点
把散装内容打成包裹
把包裹整包拆回端点
不读取样板
```

视觉：

```text
AE2 化 Packager
一面扫描/输入口
一面包裹投递口
顶部红石状态灯
容量元件小窗
颜色灯条
```

## 约束

```text
每个方块应能放入 16x16x16 block volume
模型应由简单 cuboid 组成
每个方块 6-8 个主要视觉部件以内
避免齿轮、黄铜、纸箱、复杂线缆
必须保留 front/right/top/isometric 概念或等价预览
```

## 验收

```text
blockstate/model/item model JSON 可解析
方块正面功能可辨认
没有 missing texture
docs/assets/reports/machines.md 记录生成提示、模型结构和预览路径
```

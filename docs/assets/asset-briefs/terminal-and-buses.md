# Brief: Terminal And Buses

## 范围

本资产包负责终端和总线家族的未来资产。

输出文件：

```text
src/main/resources/assets/appliedpackaging/textures/block/package_pattern_terminal*.png
src/main/resources/assets/appliedpackaging/textures/block/package_storage_bus*.png
src/main/resources/assets/appliedpackaging/textures/block/package_export_bus*.png
src/main/resources/assets/appliedpackaging/textures/block/package_unpacking_bus*.png
src/main/resources/assets/appliedpackaging/models/block/package_pattern_terminal.json
src/main/resources/assets/appliedpackaging/models/block/package_storage_bus.json
src/main/resources/assets/appliedpackaging/models/block/package_export_bus.json
src/main/resources/assets/appliedpackaging/models/block/package_unpacking_bus.json
src/main/resources/assets/appliedpackaging/models/item/package_pattern_terminal.json
src/main/resources/assets/appliedpackaging/models/item/package_storage_bus.json
src/main/resources/assets/appliedpackaging/models/item/package_export_bus.json
src/main/resources/assets/appliedpackaging/models/item/package_unpacking_bus.json
docs/assets/reports/terminal-and-buses.md
```

## 终端

视觉：

```text
AE2 终端面板
屏幕中有小型彩色包裹卡片
侧边 17 色灯条
Fluix 高亮边框
```

终端不应像普通箱子或加工机器，应明显是编码/配置界面。

## 总线

Package Storage Bus：

```text
Storage Bus 轮廓
封闭包裹图标
暗色过滤槽
```

Package Export Bus：

```text
Export Bus 轮廓
包裹向外输出的方向感
不要使用文字或箭头标签，可用形状和亮边表达流向
```

Package Unpacking Bus：

```text
总线轮廓
打开的包裹封印或散出粒子感
保持整包事务心智，不表现散乱掉落物
```

## 约束

```text
与 AE2 cable part 风格兼容
小尺寸下能区分三种 bus
不要把 bus 做成完整机器方块
不要加入文字标签
```

## 验收

```text
终端与三种 bus 都有纹理、模型和报告
storage/export/unpacking 三者图标语义可区分
docs/assets/reports/terminal-and-buses.md 记录生成提示和预览路径
```

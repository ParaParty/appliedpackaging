# 资产规格

## 1. 视觉定位

Applied Packaging 是 AE2 的 Fluix 数据包裹系统，不是纸箱物流。

关键词：

```text
浅灰石英面板
深灰金属框架
Fluix 紫蓝光
彩色束带
小型封签
数据槽
网格纹理
```

避免：

```text
纯纸箱
黄铜齿轮
重工业机械
过度 RGB
水印/文字标签
```

## 2. 颜色表

| 名称 | ID | 用途 |
| --- | --- | --- |
| Fluix | `fluix` | 默认包裹、核心封印、AE2 风格高亮 |
| White | `white` | 白色包裹束带 |
| Orange | `orange` | 橙色包裹束带 |
| Magenta | `magenta` | 品红包裹束带 |
| Light Blue | `light_blue` | 淡蓝包裹束带 |
| Yellow | `yellow` | 黄色包裹束带 |
| Lime | `lime` | 黄绿包裹束带 |
| Pink | `pink` | 粉色包裹束带 |
| Gray | `gray` | 灰色包裹束带 |
| Light Gray | `light_gray` | 淡灰包裹束带 |
| Cyan | `cyan` | 青色包裹束带 |
| Purple | `purple` | 紫色包裹束带 |
| Blue | `blue` | 蓝色包裹束带 |
| Brown | `brown` | 棕色包裹束带 |
| Green | `green` | 绿色包裹束带 |
| Red | `red` | 红色包裹束带 |
| Black | `black` | 黑色包裹束带 |

## 3. 包裹资产

物品图标：

```text
尺寸：16x16 PNG
主体：小型 AE2 封装盒
外壳：浅灰石英
边框：深灰金属
中心：Fluix 菱形封印
颜色：对应颜色束带
```

世界模型：

```text
10x10x10 或 12x12x12 盒体
四角金属包边
侧面颜色束带
顶部小封签
```

资源路径：

```text
src/main/resources/assets/appliedpackaging/textures/item/<color>_package.png
src/main/resources/assets/appliedpackaging/models/item/<color>_package.json
```

## 4. 机器资产

ME 包裹装配室：

```text
分子装配室轮廓
透明 Fluix 装配腔
悬浮包裹投影
样板接口纹理
17 色小灯条
输出口/阻挡灯
```

ME 打包机：

```text
AE2 化 Packager
一面扫描相邻存储
一面包裹投递口
顶部红石灯
容量元件小窗
颜色灯条
```

资源路径：

```text
src/main/resources/assets/appliedpackaging/textures/block/package_assembler*.png
src/main/resources/assets/appliedpackaging/textures/block/me_packager*.png
src/main/resources/assets/appliedpackaging/blockstates/package_assembler.json
src/main/resources/assets/appliedpackaging/blockstates/me_packager.json
src/main/resources/assets/appliedpackaging/models/block/package_assembler.json
src/main/resources/assets/appliedpackaging/models/block/me_packager.json
src/main/resources/assets/appliedpackaging/models/item/package_assembler.json
src/main/resources/assets/appliedpackaging/models/item/me_packager.json
```

## 5. 终端与总线资产

包裹样板终端：

```text
AE2 终端面板
屏幕内多个彩色包裹卡片
侧边 17 色灯条
```

总线：

```text
Package Storage Bus: Storage Bus 轮廓 + 封闭包裹图标
Package Export Bus: Export Bus 轮廓 + 箭头包裹图标
Package Unpacking Bus: Bus 轮廓 + 打开包裹图标
```

## 6. UI 与图标

需要的 UI 图标：

```text
颜色选择
marker
容量档
阻挡模式
自动导入 AE 网络
打包一次
保留/覆盖/清除 marker
打包过滤
拆包过滤
拒绝原因状态
```

UI 风格：

```text
深灰背景
浅灰/白色 AE2 面板
Fluix 紫蓝高亮
清晰 ghost slot
彩色小灯表示包裹颜色
```

## 7. 资源验收

必须满足：

```text
16x16 item 图标颜色可辨识
17 色包裹在 JEI/创造栏中能快速区分
机器模型无 missing texture
方块破坏粒子和 item model 正常
语言文件无缺 key
包裹图标不像普通纸箱，也不像 Create 黄铜机械
```

每组资产应保留：

```text
brief
来源或生成提示
材质文件
模型文件
blockstate/item model 文件
预览或截图
验收记录
```

## 8. 执行文件

资产生成和验收使用 `docs/assets/` 下的执行文件：

```text
docs/assets/palette.md
docs/assets/acceptance.md
docs/assets/asset-briefs/packages.md
docs/assets/asset-briefs/machines.md
docs/assets/asset-briefs/terminal-and-buses.md
docs/assets/asset-briefs/ui-and-icons.md
docs/assets/contracts/package_items.yaml
docs/assets/contracts/me_packager.yaml
docs/assets/contracts/package_assembler.yaml
docs/assets/contracts/terminal_and_buses.yaml
docs/assets/contracts/ui_icons.yaml
```

当前 5 个 contract 已通过本地 `assetgen validate-contract`。

## 9. 当前资产交付状态

已交付：

```text
19 个当前注册 item 的 32x32 图标与 item model
ME 包裹装配室与 ME 打包机初版 block textures/blockstate/block model/item model
包裹样板终端、包裹存储总线、包裹输出总线、包裹拆包总线初版 textures/model
14 个 GUI 图标
logo.png
docs/assets/reports/*.md
```

主线程已验证：

```text
5 个 asset contract 均 validate ok
53 个 PNG 尺寸符合预期
33 个 JSON 可解析
block model 坐标保持在 0..16
texture/model 引用存在
抽样视觉检查通过
.\gradlew.bat build 成功
.\gradlew.bat runData 成功
.\gradlew.bat runGameTestServer 成功，14 个必需 GameTest 全部通过
```

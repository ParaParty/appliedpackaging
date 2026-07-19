---
navigation:
  parent: devices/index.md
  title: 包裹存储总线
  icon: appliedpackaging:package_storage_bus
  position: 20
item_ids:
- appliedpackaging:package_storage_bus
categories:
- applied packaging devices
---

# 包裹存储总线

<GameScene zoom="8" background="transparent">
<ImportStructure src="../../assets/blocks/package_storage_bus.snbt" />
</GameScene>

包裹存储总线将其贴附的容器转化为包裹的网络存储。它让网络看到该容器中的包裹，并根据网络中设备的推拉需求从该容器中存取包裹。

基于 AE2 设备功能交互产生涌现机制的理念，包裹存储总线并非只能用于存储。通过子网络将其设为网络上的唯一存储，你可以将它用作包裹路由的来源或目的地。

它们是线缆子部件。

## 过滤

默认情况下总线会存储找到的所有包裹。放入过滤行的物品作为白名单，只有匹配那些条件的包裹才会被存储。

每行过滤可以组合颜色、标记和最多六个内容物品。未选择颜色的行不按颜色过滤。未选择标记的行匹配任意标记。

即使你不持有该物品，也可以从 JEI/REI 将其拖入过滤槽中。

## 优先级

点击 GUI 右上角的扳手可以设置优先级。进入网络的包裹从最高优先级存储开始尝试。两个存储优先级相同时，如果其中一个已包含匹配的包裹，网络会优先选择它。在同一优先级组中，已配置过滤的存储会被视为已包含该包裹。

## 分区

总线可以根据相邻容器中的内容进行分区。点击分区按钮扫描容器，为找到的每种不同包裹创建一行过滤。零散物品被忽略。如果容器中没有包裹，分区会清除所有过滤。

## 升级

包裹存储总线支持以下升级：

*   <ItemLink id="ae2:capacity_card" /> 增加过滤行数量
*   <ItemLink id="ae2:fuzzy_card" /> 启用每行模糊匹配
*   <ItemLink id="ae2:inverter_card" /> 将过滤从白名单切换为黑名单

## 合成配方

<RecipeFor id="appliedpackaging:package_storage_bus" />

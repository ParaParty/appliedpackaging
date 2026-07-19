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

包裹存储总线的工作方式与 AE2 的 <ItemLink id="ae2:storage_bus" /> 相同，但只处理相邻容器中的包裹物品。非包裹物品（如圆石、铁锭等）会被直接忽略。网络看不到包裹内部的内容——它看到的是"3 个红色包裹"，而不是"3 个煤炭和 3 个铁锭"。

与普通存储总线不同，该总线可以按包裹的属性进行过滤：颜色、标记以及每行最多六个内容物品。这使得你可以将不同颜色的包裹分类路由到不同的目的地。

这是[线缆子部件](ae2:ae2-mechanics/cable-subparts.md)。

## 过滤行

初始提供两行过滤。每张 <ItemLink id="ae2:capacity_card" /> 增加一行，使用五张容量卡最多可以达到七行。各行之间是"或"的关系：包裹只要匹配任意一个已启用的过滤行，就会被接收。

每行可以组合以下条件：

*   颜色过滤。留空表示不按颜色过滤。
*   标记过滤。留空表示匹配任意标记。
*   最多六个内容过滤。

## 分区

点击分区按钮后，总线会扫描相邻容器，为找到的每种不同包裹创建一行完整的过滤配置。非包裹物品会被忽略。如果容器中没有包裹，分区操作会清除所有过滤器。

## 优先级

点击 GUI 右上角的扳手图标可以设置优先级。优先级更高的存储会优先接收包裹。优先级相同时，已有同类包裹的存储会被优先选择。

你也可以将总线用于[子网络](ae2:ae2-mechanics/subnetworks.md)中。将其设为子网络上唯一的存储，总线的过滤器就可以完全控制哪些包裹进入该网络。通过这种方式可以按颜色或标记将包裹分配到不同的目的地。

## 升级

包裹存储总线支持以下[升级](ae2:items-blocks-machines/upgrade_cards.md)：

*   <ItemLink id="ae2:capacity_card" /> 增加过滤行数量
*   <ItemLink id="ae2:fuzzy_card" /> 启用每行的模糊匹配
*   <ItemLink id="ae2:inverter_card" /> 反转每行的内容过滤

该总线不接受加速卡——它只执行存储和过滤操作，不涉及任何需要加速的流程。

## 合成配方

<RecipeFor id="appliedpackaging:package_storage_bus" />

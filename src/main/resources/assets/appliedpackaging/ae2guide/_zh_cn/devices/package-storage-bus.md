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

想把包裹存在箱子里，而不是占满 ME 驱动器？这就是答案。

包裹存储总线的工作方式和 AE2 的 <ItemLink id="ae2:storage_bus" /> 完全一样——同样的优先级系统、同样的路由行为、同样需要[频道](ae2:ae2-mechanics/channels.md)——但专门针对包裹。它把相邻容器里的包裹以**包裹的形式**展示给 ME 网络，而不是把里面的东西拆开来展示。你的网络看到的是"3 个红色熔炉输入包裹"，而不是"3 个煤炭和 3 个铁锭"。

同一个箱子里的零散物品被完全忽略。

## 按包裹属性过滤

不是像普通存储总线那样按物品类型过滤，而是按包裹**里有什么**来过滤：

* **颜色**——只接受特定颜色的包裹。不填 = 不按颜色过滤。
* **标记**——只接受带特定标记的包裹。不填 = 匹配任意标记。
* **内容过滤器**——每行最多 6 个。只接受包含特定物品的包裹。

各行之间是"或"的关系——匹配**任意**启用行的包裹就会被接受。

初始有 2 个过滤行。每张 <ItemLink id="ae2:capacity_card" /> 增加一行，5 张卡最多 7 行。

## 分区

点分区按钮，总线扫描相邻容器。找到的每种不同包裹会生成一行完整过滤器——颜色、标记、内容全填好。容器里没包裹的话，分区清空所有过滤器。

## 优先级

和 AE2 存储总线一样：优先级高的先填。优先级相同，AE2 优先选已有匹配包裹的存储。

**一个常见路由技巧：** 给 <ItemLink id="appliedpackaging:package_unpacking_bus" /> 设更高优先级，包裹到货就拆包。给本总线设更高优先级，包裹先存起来。

## 升级

* <ItemLink id="ae2:capacity_card" />——更多过滤行（最多 5 张）
* <ItemLink id="ae2:fuzzy_card" />——每行模糊匹配
* <ItemLink id="ae2:inverter_card" />——反转每行内容过滤

不带加速卡——这个总线没有计时操作。它是过滤器，不是处理器。

## 合成配方

<RecipeFor id="appliedpackaging:package_storage_bus" />

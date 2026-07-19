---
navigation:
  parent: devices/index.md
  title: 包裹卸货总线
  icon: appliedpackaging:package_unpacking_bus
  position: 30
item_ids:
- appliedpackaging:package_unpacking_bus
categories:
- applied packaging devices
---

# 包裹卸货总线

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../../assets/blocks/package_unpacking_bus.snbt" />
</GameScene>

包裹卸货总线从网络存储中接收包裹，并按编码顺序将内容物插入其所贴附的容器中。它作为包裹的目的地——当包裹存入网络时，总线取走它并将每件物品按顺序放入目标容器。

该总线也可作为有序供应器使用，类似于样板供应器将完整原料批次推送到相邻容器的方式。它在操作完成之前会交付完整的包裹内容物。

它们是线缆子部件。

## 过滤

默认情况下总线接受任何包裹。放入过滤行的物品作为白名单，只有匹配那些条件的包裹才会被拆包。

每行过滤可以组合颜色、标记和最多六个内容物品。未选择颜色的行不按颜色过滤。未选择标记的行匹配任意标记。

即使你不持有该物品，也可以从 JEI/REI 将其拖入过滤槽中。

## 优先级

点击 GUI 右上角的扳手可以设置优先级。进入网络的包裹从最高优先级目的地开始尝试。优先级相同时，可用的包裹卸货总线优先于包裹存储总线。

## 设置

*   总线可以设置为在接受包裹之前检查完整内容能否插入（GUI 中标注为"防堵塞模式"，默认开启）。开启时，检查失败则拒绝包裹，让网络将其路由至其他目的地。关闭时，总线接受包裹并将其保留在工作槽中，直到目标就绪。
*   可以启用阻挡模式，在目标已含有包裹内容物类型时阻止拆包。
*   工作槽中的包裹是实际物品。可随时从 GUI 取出。破坏总线会返回该包裹。

包裹内容物在拆包成功提交之前不会暴露给网络存储。

## 升级

包裹卸货总线支持以下升级：

*   <ItemLink id="ae2:speed_card" /> 缩短拆包工作周期
*   <ItemLink id="ae2:capacity_card" /> 增加过滤行数量
*   <ItemLink id="ae2:fuzzy_card" /> 启用每行模糊匹配
*   <ItemLink id="ae2:inverter_card" /> 将过滤从白名单切换为黑名单

## 合成配方

<RecipeFor id="appliedpackaging:package_unpacking_bus" />

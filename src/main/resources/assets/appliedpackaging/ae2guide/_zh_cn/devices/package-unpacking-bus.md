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

你做出了包裹。现在需要它精准到达正确的机器，按正确的顺序。这就是卸货总线干的事。

可以把它想象成 AE2 的 Formation Plane，但带投递顺序控制。网络把包裹路由给它，它把每件物品按你编码的顺序插入相邻容器——煤炭在前、铁在后。

这是[线缆子部件](ae2:ae2-mechanics/cable-subparts.md)，需要[频道](ae2:ae2-mechanics/channels.md)。

## 怎么工作，一步一步

1. 包裹进入 ME 网络（你丢进终端、自动合成产出，随便哪种方式）。
2. 网络按优先级把它路由到卸货总线。
3. 总线在处理期间把包裹放在可见的工作槽里。你能看到它搁在那。
4. 处理完成，每件物品按顺序插入相邻容器。
5. 如果容器装不下（满了，或阻挡模式拦截），总线等待并重试。**绝不会部分插入或丢失物品。**

## 优先级和路由

优先级高的目的地先收包裹。优先级相同，可用的卸货总线优先于包裹存储总线。但如果总线拒绝包裹（过滤器不匹配、目标满、预接收检查不通过），网络会尝试下一个目的地——可能是存储总线。

**实用建议：** 想让包裹自动拆包，给卸货总线设比存储总线更高的优先级。想先存起来要用时再拆，反过来设。

## 那个"预接收检查"

这个总线有个设置，控制检测目标能不能接收输出的**时机**。GUI 上叫"防堵塞模式"。实际作用：

* **开着（默认）：** 总线在从网络取走包裹**之前**检查完整内容能不能放入目标。不通过的话，包裹留在网络里，可以路由到别处。**开着是安全选项。**
* **关着：** 总线立刻拿走包裹并持有。目标没准备好就等着。你随时可以从 GUI 取回。**关着是"我知道我在干嘛"选项。**

阻挡模式也受这个影响：预接收检查开着时，如果阻挡模式拦截了，总线直接拒绝包裹而不是持有它。

## 阻挡模式

开启后，目标已经有包裹内容物中的任何类型物品时，总线拒绝拆包。防止机器还没处理完上一批就收到重复输入。

## 滞留包裹

工作槽里的是真实物品。随时可以从 GUI 取出——甚至处理过程中也行。破坏总线也能取回。包裹内容物在拆包成功提交之前绝不对 ME 存储可见。

## 升级

* <ItemLink id="ae2:speed_card" />——更快拆包（最多 4 张）
* <ItemLink id="ae2:capacity_card" />——更多过滤行（最多 5 张，共 7 行）
* <ItemLink id="ae2:fuzzy_card" />——每行模糊匹配
* <ItemLink id="ae2:inverter_card" />——反转每行内容过滤

## 合成配方

<RecipeFor id="appliedpackaging:package_unpacking_bus" />

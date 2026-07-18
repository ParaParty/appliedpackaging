---
navigation:
  parent: index.md
  title: 包裹总线
  icon: appliedpackaging:package_unpacking_bus
  position: 50
item_ids:
- appliedpackaging:package_storage_bus
- appliedpackaging:package_unpacking_bus
categories:
- applied packaging devices
---

# 包裹总线

## 包裹存储总线

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/package_storage_bus.snbt" />
</GameScene>

**包裹存储总线**只向网络暴露相邻库存中的合法包裹，不会把包裹内容伪装成散装 ME 资源。分区功能会读取相邻包裹，生成颜色、标记与内容过滤行。

每个启用行可以组合颜色、标记和最多六个内容过滤项；各行之间是“任一匹配”。初始开放两行，最多五张容量卡各增加一行，最终达到七行。模糊卡和反转卡会显示对应的逐行控制；颜色为空时，该行不按颜色过滤。

它沿用 AE2 <ItemLink id="ae2:storage_bus" /> 的优先级和存储过滤模型，但包裹内容始终保持不透明。

分区功能按相邻库存槽位顺序读取，每种不同合法包裹生成一条完整过滤行；散装物品会被忽略。目标中没有包裹时，分区会清空包裹过滤。

## 包裹卸货总线

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/package_unpacking_bus.snbt" />
</GameScene>

**包裹卸货总线**是类似成型面的目标端点：接收网络路由来的一个包裹，把真实 held 包裹报告给 ME，并允许网络或 GUI 在最终原子提交前取回。它不会主动扫描网络寻找包裹。

它是网络存储目的地而不是输出总线：当包裹被插入 ME 存储时由 AE2 选择目的地。高优先级先接收；同优先级下，可用的卸货总线优先于存储总线。若因过滤、held 忙碌、阻挡或容量拒绝，路由可继续尝试其它目的地。

加速卡缩短可见工作周期。进度完成时，总线重新检查过滤、阻挡与累计目标容量，再按包裹条目顺序全部插入；最终检查失败时保留整包并稍后重试。

样板供应器式阻挡会在目标已有任一输入类型时拒绝。防堵塞模式独立且默认开启：开启时，只有整个包裹满足自动拆包使用的相同阻挡与容量预检才接收；关闭时可接收一个合法包裹等待重试。目标为序列缓存器时会使用原子计划，并可在样板模式下保留稀疏位置。

包裹卸货总线最多接受四张加速卡；容量卡、模糊卡和反转卡与过滤行共用同一个升级面板。

## held 包裹恢复

工作槽是向网络报告为数量 1 包裹的真实存储。网络抽取、GUI 抽取或拆除部件都会返还包裹，并原子取消工作、进度、阻塞与重试状态。最终提交前，包裹内部内容绝不会作为散装 ME 库存报告。

两种总线都是需要供电频道的线缆部件。

## 配方

<RecipeFor id="appliedpackaging:package_storage_bus" />

<RecipeFor id="appliedpackaging:package_unpacking_bus" />

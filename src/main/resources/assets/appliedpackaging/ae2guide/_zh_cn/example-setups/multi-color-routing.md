---
navigation:
  parent: example-setups/index.md
  title: 多颜色路由
  icon: appliedpackaging:red_package
  position: 30
---

# 多颜色路由

将一个配方拆分为多个包裹组时，可以用颜色来标记不同组的去向。包裹存储总线和卸货总线都支持按颜色过滤——例如，红色包裹的一组原料去一个目的地，蓝色包裹的另一组去另一个目的地。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../../assets/assemblies/package_routing.snbt" />
  <IsometricCamera yaw="195" pitch="25" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="2 1 2">
    (1) 包裹存储总线：过滤为仅接受红色包裹。将包裹存入左侧箱子。
  </BoxAnnotation>
  <BoxAnnotation color="#ffbb55" min="3 0 1" max="5 1 2">
    (2) 包裹卸货总线：过滤为仅接受蓝色包裹。将包裹拆包到右侧目标。
  </BoxAnnotation>
</GameScene>

## 配置方法

*   <ItemLink id="appliedpackaging:package_storage_bus" /> (1) 设有一行过滤，颜色选择为红色。
*   <ItemLink id="appliedpackaging:package_unpacking_bus" /> (2) 设有一行过滤，颜色选择为蓝色。

## 工作原理

1.  红色包裹进入网络。存储总线匹配红色过滤条件，包裹进入箱子。卸货总线不会接收该包裹。
2.  蓝色包裹进入网络。存储总线的过滤不匹配，网络尝试下一个目的地。卸货总线匹配蓝色过滤条件，包裹被拆包。
3.  绿色包裹进入网络。两个总线的过滤条件都不匹配，包裹保留在网络存储中。

## 通过优先级控制路由

调整优先级可以控制目的地的尝试顺序：

*   将卸货总线设为更高优先级：包裹到达时优先拆包，存储总线作为后备。
*   将存储总线设为更高优先级：包裹优先存入存储，卸货总线仅接收溢出部分。

## 高级样板配合多颜色路由

[高级处理样板](devices/advanced-pattern-terminal.md)可以编码最多 81 列，每列拥有独立的颜色。配合子网络中按颜色过滤的卸货总线和序列缓存器，每一列的包裹被路由到不同的缓存链，通过单个样板即可完成复杂的多组分配。该方案特别适合需要在特定输入槽放入特定物品的模组机器，例如 Create 的机械合成器。

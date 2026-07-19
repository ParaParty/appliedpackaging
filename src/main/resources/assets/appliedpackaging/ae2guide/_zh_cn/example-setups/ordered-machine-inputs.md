---
navigation:
  parent: example-setups/index.md
  title: 有序机器输入
  icon: appliedpackaging:package_unpacking_bus
  position: 20
---

# 有序机器输入

像 Create 机械合成器这样有位置特定输入槽的机器，需要将特定物品放入特定槽位。序列缓存器保留包裹内容物或样板供应器推送的顺序和稀疏布局，将每件物品投递到正确的机器面。

该方案也可以直接处理样板供应器的推送：供应器按顺序推送原料，缓存器将每个原料按位置映射到对应成员。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../../assets/assemblies/sequence_line.snbt" />
  <IsometricCamera yaw="205" pitch="30" />

  <BoxAnnotation color="#ffbb55" min="0 0 1" max="1 1 2">
    (1) 包裹卸货总线：将包裹送入端点。
  </BoxAnnotation>
  <BoxAnnotation color="#66aaff" min="1 0 1" max="2 1 2">
    (2) 端点：拥有配置，本身不存储物品。
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="2 0 1" max="5 1 2">
    (3) 成员：每个存放一件物品，并向北输出到各自目标。
  </BoxAnnotation>
</GameScene>

## 配置方法

*   <ItemLink id="appliedpackaging:package_unpacking_bus" /> (1) 贴放在端点 (2) 上。预接收检查保持开启（默认）。
*   端点开启自动输出。使用带稀疏槽位布局的包裹时，开启样板模式。
*   每个成员 (3) 的输出面指向目标机器的对应输入槽位。

## 工作原理

1.  包裹（或样板供应器推送）将物品送达序列缓存器端点。
2.  第一个物品进入成员 1，第二个进入成员 2，依此类推。
3.  每个成员通过配置的输出面将物品输出到目标机器。
4.  开启同步输出时，所有成员协调行动——整个批次要么全部提交，要么全都不提交。

## "一次一个"规则

当包裹的条目 1 和条目 3 都包含煤炭时，成员 1 接收第一个煤炭，成员 3 接收第二个。缓存器每个成员只接受一件物品的规则，正是让相同物品保持独立的原因。没有这个规则，两份煤炭就会合并，丢失各自独立的槽位位置。

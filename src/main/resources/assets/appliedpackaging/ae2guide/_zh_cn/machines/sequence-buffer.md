---
navigation:
  parent: machines/index.md
  title: 序列缓存器
  icon: appliedpackaging:sequence_buffer
  position: 30
item_ids:
- appliedpackaging:sequence_buffer
categories:
- applied packaging machines
---

# 序列缓存器

<BlockImage id="appliedpackaging:sequence_buffer" scale="8" />

序列缓存器每个方块存储一种物品类型，输出到配置的相邻面。它保留输入物品的顺序并按位置逐一投递到指定机器面。它接受任何按序推送物品的来源——样板供应器、包裹卸货总线或任何供应批次的设备。

每个缓存成员完全清空前只接受一个物品，期间拒绝任何其他输入。此规则阻止相同物品合并。当样板指定煤炭在位置 1 和位置 3 时，缓存器保持它们独立。位置 1 去一个机器面，位置 3 去另一个。没有此约束，两份煤炭会合并为一个堆叠。

两个或更多序列缓存器沿 X、Y 或 Z 轴排成直线可组成多方块。在最后一个方块的端面用 AE2 扳手右键点击组建。被扳手操作的方块成为端点——保存配置但不存储物品。其余方块为存储成员，从端点向外编号。成员 1 接收第一个物品，成员 2 第二个，以此类推。

成员输出完成后有短暂间隔才能接受下一个物品，防止同一成员在同一刻内被重新填入。

## 搭建多方块结构

### 包裹卸货总线输入

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../../assets/assemblies/sequence_line.snbt" />
  <IsometricCamera yaw="205" pitch="30" />

  <BoxAnnotation color="#ffbb55" min="0 0 0" max="1 1 2">
    (1) 包裹卸货总线：将包裹送入端点。
  </BoxAnnotation>
  <BoxAnnotation color="#66aaff" min="1 0 1" max="2 1 2">
    (2) 端点：拥有配置，本身不存储物品。
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="2 0 1" max="5 1 2">
    (3) 成员：每个存放一种物品，并向北输出到各自目标。
  </BoxAnnotation>
</GameScene>

### 样板供应器输入

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../../assets/assemblies/sequence_line_pattern_provider.snbt" />
  <IsometricCamera yaw="205" pitch="30" />

  <BoxAnnotation color="#ffbb55" min="0 0 0" max="1 1 2">
    (1) 样板供应器：选定输出面朝向端点。
  </BoxAnnotation>
  <BoxAnnotation color="#66aaff" min="1 0 1" max="2 1 2">
    (2) 端点：接收按序推送的样板原料。
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="2 0 1" max="5 1 2">
    (3) 成员：保留槽位位置并投递到各自目标。
  </BoxAnnotation>
</GameScene>

序列缓存器本身不使用 AE 能源。两种结构中的线缆都只用于将包裹卸货总线或样板供应器接入其 ME 网络。

1. 将至少两个序列缓存器方块排成直线。
2. 在最后一个方块的端面用 AE2 扳手右键组建。
3. 打开端点 GUI 配置所有设置。设置应用于多方块中的每个成员。
4. 依次打开每个成员的侧边 GUI 设置输出面。未设置时成员搜索除其他序列缓存器外的任意相邻容器。
5. 选择一种输入来源：将包裹卸货总线贴在端点上，或将样板供应器放在端点旁并用 AE2 扳手把选定输出面转向端点。

## 设置

端点 GUI 提供以下设置项：

*   **自动输出**——开启后成员自动推送物品到配置的输出面。关闭后物品留在成员中直到手动取出。
*   **阻挡模式**——开启后成员等待输出目标完全清空后才发送。关闭后无论目标中已有何物均输出。
*   **同步输出**——开启后所有已占用成员必须能输出，任何单个成员才会执行。一个成员无法输出则全体等待。
*   **样板模式**——开启后使用编码样板或包裹布局中的稀疏槽位位置。物品按记录位置映射到成员。关闭后按紧凑顺序分配，跳过空槽。
*   **预接收检查**（GUI 标注"防堵塞模式"，默认关闭）——开启后在接受输入前验证每个分配成员能否成功输出。任何成员失败则整个输入被拒。
*   **输入延迟**——在接受和输出之间添加 0 到 100 刻延迟。手动取出不受影响。
*   **输入过滤器**——最多 9 种物品或流体类型。设置后只接受匹配类型的物品。留空接受任意物品。

## 容量

每个成员默认存储最多 1024 件物品。服务器管理员可修改此值。

## 升级

序列缓存器支持：

*   <ItemLink id="ae2:redstone_card" />——安装在端点时，自动输出仅在有红石信号时发生。

## 合成配方

<RecipeFor id="appliedpackaging:sequence_buffer" />

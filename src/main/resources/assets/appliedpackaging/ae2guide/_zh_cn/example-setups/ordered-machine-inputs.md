---
navigation:
  parent: example-setups/index.md
  title: 有序机器输入
  icon: appliedpackaging:package_unpacking_bus
  position: 20
---

# 有序机器输入

熔炉要煤炭在侧面、矿石在顶部。酿造台要每样原料放在特定槽位。这些机器关心**什么物品去什么位置**——这正是包裹最擅长的。

这个方案用包裹卸货总线配合序列缓存器，把一个包裹拆到多个机器面上。

## 搭建方案

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../../assets/assemblies/sequence_line.snbt" />
  <IsometricCamera yaw="205" pitch="30" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="1 1 2">
    (1) 端点：拥有配置。卸货总线贴在这个面上。
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="1 0 1" max="4 1 2">
    (2) 成员：每个保存一种物品。分别配好输出面指向机器。
  </BoxAnnotation>
</GameScene>

## 熔炉场景

上图是通用布局。熔炉场景具体来说：

* 卸货总线贴在端点 (1) 上。
* 成员 1 → 熔炉侧面（煤炭/燃料槽）
* 成员 2 → 熔炉顶部（矿石/输入槽）
* 熔炉下方放漏斗抽出熔炼结果。

## 配置方法

* <ItemLink id="appliedpackaging:package_unpacking_bus" />（贴在端点上）无需过滤，除非要限制哪些包裹到达。确保预接收检查开启（默认开着的）。
* 端点开启自动输出。如果你的包裹有稀疏槽位，开启样板模式。
* 每个成员的输出面设置指向目标机器的正确位置。

## 工作原理

1. 包裹进入 ME 网络，路由到卸货总线。
2. 卸货总线把包裹内容喂进序列缓存器端点。
3. 包裹条目 1 → 成员 1，条目 2 → 成员 2，以此类推。
4. 每个成员通过配好的输出面送到机器。
5. 同步输出开着的话，所有成员等彼此——没有部分投递。

## 小贴士

* **同步输出 + 预接收检查**一起开：包裹只有在每个成员都能成功输出时才进入缓存器。完全原子投递。
* 机器需要批间隔时加**输入延迟**。
* 熔炉类机器一定要把燃料放**第一个槽位**编码——它会去最靠近端点的那个成员。

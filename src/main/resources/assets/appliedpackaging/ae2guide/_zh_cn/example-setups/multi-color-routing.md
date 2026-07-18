---
navigation:
  parent: example-setups/index.md
  title: 多颜色路由
  icon: appliedpackaging:red_package
  position: 30
---

# 多颜色路由

多条生产线？用不同的包裹颜色路由到不同地点。红色去熔炼厂，蓝色去装配机，绿色进存储。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../../assets/assemblies/package_routing.snbt" />
  <IsometricCamera yaw="195" pitch="25" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="2 1 2">
    (1) 包裹存储总线：只过滤红色包裹。存进左边的箱子。
  </BoxAnnotation>
  <BoxAnnotation color="#ffbb55" min="3 0 1" max="5 1 2">
    (2) 包裹卸货总线：只过滤蓝色包裹。拆包到右边目标。
  </BoxAnnotation>
</GameScene>

## 配置方法

* <ItemLink id="appliedpackaging:package_storage_bus" /> (1) 有一行过滤，选了红色样本。现在它只存红色包裹。
* <ItemLink id="appliedpackaging:package_unpacking_bus" /> (2) 有一行过滤，选了蓝色样本。现在它只拆蓝色包裹。

**标记和内容过滤在这例子里都没填。** 不需要，除非你想更精细的控制。

## 工作原理

1. 红色包裹进 ME 网络。存储总线匹配 → 包裹进箱子。卸货总线根本看不到。
2. 蓝色包裹进网络。存储总线不匹配 → 网络试下一个目的地。卸货总线匹配 → 包裹被拆包。
3. 绿色包裹进网络。两个总线都不匹配 → 包裹留在网络存储（或去你设置的其他地方）。

## 基于优先级的路由

调整优先级控制包裹在有多个可选目的地时的行为：

* **卸货总线优先级更高：** 包裹一到就拆包。只有溢出的进存储。
* **存储总线优先级更高：** 包裹先进箱子。准备好了再取出拆包。
* **优先级相同，两个存储总线：** AE2 优先选已有匹配包裹的那个。都是空的就随便哪个。

## 进阶：按内容物过滤

颜色只是最简单的维度。每行可以混搭颜色、标记和内容过滤：

* **一行红色 + 铁锭标记：** 只接受标记了"铁锭"的红色包裹
* **一行蓝色 + 煤炭内容过滤：** 只接受*包含*煤炭的蓝色包裹

行之间是"或"关系——匹配*任意*行的包裹即被接受。所以一个总线可以同时接受"红+铁标记"和"蓝+含煤炭"，只要用两行不同的过滤。

## 预接收检查有什么影响？

预接收检查开着（默认），一个匹配卸货总线过滤的蓝色包裹如果当前不能拆（目标满、阻挡模式拦截），总线就拒绝它。网络接着试下一个目的地——包裹可能最终进了存储总线。这能防止包裹卡在错误的总线里等。

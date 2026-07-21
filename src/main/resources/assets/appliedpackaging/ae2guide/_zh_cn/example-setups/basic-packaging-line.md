---
navigation:
  parent: example-setups/index.md
  title: 基础打包线
  icon: appliedpackaging:package_assembler
  position: 10
---

# 基础打包线

该方案展示了包裹装配室的样板供应器网格模式。供应器将原料推送到装配室，装配室装配包裹并默认将其返回 ME 存储。

注意，这里使用了 <ItemLink id="ae2:pattern_provider" />，因此该搭建需要集成到[自动合成](ae2:ae2-mechanics/autocrafting.md)系统中。在该模式下，推荐仅放入包裹样板——普通 AE2 样板请使用方向性供应器配合子网络。

如果使用独立外部输入，直接将样板放入装配室的样板槽，并通过漏斗或管道从外部输入物品。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="30" />

  <BoxAnnotation color="#dddddd" min="0 0 0" max="2 2 2">
    (1) 2×2×2 棋盘式网格：4 个样板供应器和 4 个包裹装配室。
  </BoxAnnotation>
  <BoxAnnotation color="#66aaff" min="2 0 0" max="3 1 1">
    (2) 智能线缆：从这一端接入主网络。
  </BoxAnnotation>
</GameScene>

## 配置方法

*   4 个 <ItemLink id="ae2:pattern_provider" /> 使用默认配置并放入[包裹样板](../devices/advanced-pattern-terminal.md)。它们共占用 4 个频道；将预留的智能线缆 (2) 接入主网络。
*   4 个 <ItemLink id="appliedpackaging:package_assembler" /> 使用默认的 ME 网络输出模式。包裹直接进入 ME 存储。
*   不要在该网格中使用高级样板或普通 AE2 样板；这两类样板请改用[包裹装配室](../machines/package-assembler.md)页面中的方向性供应器子网络。

## 工作原理

1.  自动合成请求包裹后，样板供应器将原料输入装配室。
2.  装配室在消耗物品前先确认包裹在容量限制内。
3.  装配室生产包裹。
4.  包裹输出到 ME 存储。

如需进一步路由包裹——例如通过子网络拆包——请使用方向性样板供应器搭配子网络。

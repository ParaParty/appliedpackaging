---
navigation:
  parent: example-setups/index.md
  title: 基础打包线
  icon: appliedpackaging:package_assembler
  position: 10
---

# 基础打包线

注意，该方案使用了 <ItemLink id="ae2:pattern_provider" />，因此需要集成到你的[自动合成](ae2:ae2-mechanics/autocrafting.md)系统中。如果只是想单独装配包裹，可以直接将样板放入装配室的样板槽中，并在 GUI 输入网格中放入原料。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../../assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="25" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="2 1 2">
    (1) 样板供应器：默认配置，内部放置编码好的包裹或高级样板。
  </BoxAnnotation>
  <BoxAnnotation color="#cc88ff" min="2 0 1" max="3 1 2">
    (2) ME 包裹装配室：输出模式为"输出到 ME 网络"（默认）。
  </BoxAnnotation>
</GameScene>

## 配置方法

*   <ItemLink id="ae2:pattern_provider" /> (1) 使用默认配置，内部放置相应的[包裹或高级样板](devices/advanced-pattern-terminal.md)。
*   <ItemLink id="appliedpackaging:package_assembler" /> (2) 使用默认输出模式。包裹直接进入 ME 存储。

## 工作原理

1.  自动合成系统请求包裹后，样板供应器将原料输入装配室。
2.  装配室在消耗任何物品之前，先确认完整包裹是否在容量限制内。
3.  装配室生产包裹。
4.  包裹输出到 ME 存储。

产生包裹后，你可以通过子网络将其路由到卸货总线拆包，或将多个包裹分别路由到不同目的地。

## 输出到相邻方块

如需输出到箱子或其他容器，将装配室的输出模式改为"输出到相邻方块"并选择目标方向。

## 使用子网络

如果希望包裹输出通过子网络路由，使用 <ItemLink id="ae2:certus_quartz_wrench" /> 将样板供应器调整为方向性模式，使供应器和装配室不形成同一个网络连接。装配室就可以放在独立的子网络上，将包裹输出到卸货总线和序列缓存器进行分发。

---
navigation:
  parent: example-setups/ordered-machine-inputs.md
  title: 示例 4：路由
  icon: appliedpackaging:package_storage_bus
  position: 40
---

# 示例 4：路由

将四个包裹存储总线连接到同一条橙色路由网络，并分别过滤为红、蓝、绿、黄。每个总线都贴在一台 ME 打包机上；打包机的颜色选择也设为与总线相同的颜色，另一侧粉色线缆则连接各自独立的目标网络。

<GameScene zoom="2.75" background="transparent">
  <ImportStructure src="../../assets/assemblies/packager_color_routing.snbt" />
  <IsometricCamera yaw="200" pitch="55" />

  <BoxAnnotation color="#dd8833" min="3 0 0" max="10 1 6">
    (1) 橙色路由网络：顶部接口连接包裹来源。
  </BoxAnnotation>
  <BoxAnnotation color="#b02e26" min="2 0 1" max="4 1 2">
    (2) 红色存储总线 + 红色 ME 打包机。
  </BoxAnnotation>
  <BoxAnnotation color="#3c44aa" min="2 0 5" max="4 1 6">
    (3) 蓝色存储总线 + 蓝色 ME 打包机。
  </BoxAnnotation>
  <BoxAnnotation color="#5e7c16" min="9 0 1" max="11 1 2">
    (4) 绿色存储总线 + 绿色 ME 打包机。
  </BoxAnnotation>
  <BoxAnnotation color="#fed83d" min="9 0 5" max="11 1 6">
    (5) 黄色存储总线 + 黄色 ME 打包机。
  </BoxAnnotation>
</GameScene>

包裹进入橙色网络后，AE2 只会尝试颜色匹配的包裹存储总线。总线将包裹送入对应打包机，打包机再把内容拆入外侧粉色网络。总线与打包机必须使用相同颜色；如果需要严格分流，不要把打包机保留为默认的 Fluix 无颜色过滤。

结构中的四段粉色线缆只是四个目标网络的接口，彼此没有连接。实际搭建时，将它们分别接入对应生产线；橙色网络顶部的接口则接入产生包裹的网络。

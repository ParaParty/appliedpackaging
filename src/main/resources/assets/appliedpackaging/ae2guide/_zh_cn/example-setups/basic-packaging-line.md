---
navigation:
  parent: example-setups/index.md
  title: 基础打包线
  icon: appliedpackaging:package_assembler
  position: 10
---

# 基础打包线

你能搭的最简单的自动合成方案：样板供应器向包裹装配室推送原料，完成的包裹进箱子。

注意，这里用到了 <ItemLink id="ae2:pattern_provider" />，所以这个方案是用来集成到你的[自动合成](ae2:ae2-mechanics/autocrafting.md)里的。如果只是想手动做包裹，把样板直接放装配室里、通过 GUI 喂原料就行。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../../assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="25" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="2 1 2">
    (1) 样板供应器：默认配置，里面放编码好的包裹或高级样板。
  </BoxAnnotation>
  <BoxAnnotation color="#cc88ff" min="2 0 1" max="3 1 2">
    (2) ME 包裹装配室：输出模式设为"输出到相邻方块"。最多装 5 张加速卡。
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="3 0 1" max="4 1 2">
    (3) 箱子：接收完成的包裹。换成任何机器或 ME 接口都行。
  </BoxAnnotation>
</GameScene>

## 配置方法

* <ItemLink id="ae2:pattern_provider" /> (1) 使用默认配置，里面放编码好的[包裹或高级样板](devices/advanced-pattern-terminal.md)。
* <ItemLink id="appliedpackaging:package_assembler" /> (2) 输出模式设为"输出到相邻方块"，指向箱子。装加速卡提升速度。
* 箱子 (3) 不需要配置。它只负责接收包裹。

## 工作原理

1. 自动合成请求包裹 → 样板供应器把原料推进装配室。
2. 装配室在消耗任何物品**之前**检查容量。包裹太大就什么都不浪费。
3. 进度条走完。<ItemLink id="ae2:speed_card" /> 缩短这个过程。
4. 完成的包裹推进箱子。
5. 如果开启了阻挡模式，装配室等箱子清空再开下一批。

## 变体

* **ME 网络输出：** 装配室输出模式改"输出到 ME 网络"。包裹直接进网络存储，不需要箱子。
* **本地模式：** 跳过样板供应器——把样板放装配室自己的槽里，原料放 GUI。

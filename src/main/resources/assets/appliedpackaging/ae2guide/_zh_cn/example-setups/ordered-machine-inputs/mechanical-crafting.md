---
navigation:
  parent: example-setups/ordered-machine-inputs.md
  title: 示例 1：5×5 动力合成
  icon: appliedpackaging:sequence_buffer
  position: 10
---

# 示例 1：5×5 动力合成

在样板供应器内放入一个 5×5 的高级样板。5 列分别使用不同颜色，每列中的 5 个位置按顺序写入，空位也保留。用扳手将供应器的选定输出面转向包裹装配室；装配室开启阻挡模式，再由 5 个按颜色过滤的包裹卸货总线将 5 个包裹送入对应缓存行。

<GameScene zoom="3.25" background="transparent">
  <ImportStructure src="../../assets/assemblies/sequence_mechanical_crafting_5x5.snbt" />
  <IsometricCamera yaw="215" pitch="25" />

  <BoxAnnotation color="#dd88cc" min="0 0 4" max="3 1 6">
    (1) 主网络：放有 5×5 高级样板的样板供应器。
  </BoxAnnotation>
  <BoxAnnotation color="#eeeeee" min="0 0 2" max="3 6 4">
    (2) 处理子网：阻挡模式装配室与五条拆包支路。
  </BoxAnnotation>
  <BoxAnnotation color="#66aaff" min="2 1 2" max="8 6 3">
    (3) 五条序列缓存器：每条包含一个端点和 5 个成员。
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="3 1 3" max="8 6 4">
    (4) 5×5 正面箱子墙：逐格代替动力合成器。
  </BoxAnnotation>
</GameScene>

结构中的 25 个箱子都放在序列成员的正面，只是动力合成器的占位，不要求安装 Create 也能加载这张场景。实际搭建时，将箱子逐格替换为朝向合成中心的动力合成器；示例样板中的材料只用于显示带空位的 5×5 布局，按实际配方替换即可。

如果将动力合成器水平铺设，可以先从 JEI 将配方填入高级样板编码终端，再使用“转置配方”交换行列，使样板的包裹列与水平缓存行对应；产物不会随转置改变。

每条缓存器的端点都开启自动输出、样板模式与同步输出。样板模式会保留包裹中的空位，使同一列的第 1～5 个位置始终对应这一行的 5 个机器面；关闭后原料会向前压紧，带空位的配方就会错位。

处理子网占用 6 个频道（包裹装配室与 5 个包裹卸货总线），普通智能线缆即可承载。子网只通过石英纤维从粉色主网取电。装配室的阻挡模式可以避免上一批同类包裹仍在处理子网中时继续送入下一批。

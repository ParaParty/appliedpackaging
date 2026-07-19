---
navigation:
  parent: example-setups/ordered-machine-inputs.md
  title: 示例 2：并行熔炉组
  icon: appliedpackaging:sequence_buffer
  position: 20
---

# 示例 2：并行熔炉组

在样板供应器内放入一个加工样板：输入 1 为煤炭 ×1，输入 2 为粗铁 ×8，输出为铁锭 ×8。用扳手将供应器的选定输出面转向包裹装配室。装配室把这一整批原料封成一个包裹，再由白色处理子网将包裹送入四条熔炉支路中当前能够完整接收它的一条。

<GameScene zoom="3.5" background="transparent">
  <ImportStructure src="../../../assets/assemblies/sequence_furnace_array.snbt" />
  <IsometricCamera yaw="215" pitch="30" />

  <BoxAnnotation color="#dd88cc" min="0 1 2" max="1 2 5">
    (1) 主网络：放有“煤炭 + 粗铁 ×8”加工样板的样板供应器。
  </BoxAnnotation>
  <BoxAnnotation color="#eeeeee" min="0 1 0" max="6 3 3">
    (2) 处理子网：包裹装配室与四条并行拆包支路。
  </BoxAnnotation>
  <BoxAnnotation color="#ff9944" min="1 0 2" max="6 2 5">
    (3) 回收子网：输入总线收回熔炉产物，并经样板供应器返回主网。
  </BoxAnnotation>
</GameScene>

每条支路中，成员 1 向下把煤炭交给漏斗，漏斗再从熔炉侧面送入燃料槽；成员 2 从顶面送入粗铁 ×8。熔炉下方的输入总线负责收回铁锭。

四个包裹卸货总线从左到右分别设置为优先级 4、3、2、1。网络先尝试优先级最高且能够完整接收包裹的支路；该支路忙碌时，再依次尝试后面的支路。

粉色线缆属于主网络。白色处理子网占用 5 个频道（包裹装配室与四个包裹卸货总线），橙色回收子网同样占用 5 个频道（四个输入总线与一个普通存储总线）；两条子网都只通过石英纤维取电。不要把白色或橙色线缆直接接回主网：样板供应器的选定输出面让装配室所在子网保持独立，另一面的普通存储总线则在不合并频道的情况下把加工结果送回供应器。

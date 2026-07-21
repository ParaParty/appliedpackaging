---
navigation:
  parent: example-setups/ordered-machine-inputs.md
  title: 示例 3：多步骤合成
  icon: appliedpackaging:advanced_processing_pattern
  position: 30
---

# 示例 3：多步骤合成

在样板供应器内放入一个包含四列的高级样板，四列分别记录四个步骤需要接收的原料，并设置为红、蓝、绿、黄。用扳手将供应器的选定输出面转向包裹装配室；装配室按列生成四个包裹，再由四个对应颜色的包裹存储总线送入各自的步骤输入箱。

<GameScene zoom="3.25" background="transparent">
  <ImportStructure src="../../assets/assemblies/advanced_multistep_crafting.snbt" />
  <IsometricCamera yaw="205" pitch="35" />

  <BoxAnnotation color="#dd88cc" min="0 0 4" max="2 1 6">
    (1) 主网络：放有四列高级样板的样板供应器。
  </BoxAnnotation>
  <BoxAnnotation color="#eeeeee" min="0 0 2" max="8 1 4">
    (2) 处理子网：包裹装配室与四个按颜色过滤的包裹存储总线。
  </BoxAnnotation>
  <BoxAnnotation color="#b02e26" min="3 0 1" max="4 1 3">
    (3) 红色总线：步骤 1 输入箱。
  </BoxAnnotation>
  <BoxAnnotation color="#3c44aa" min="4 0 2" max="5 1 4">
    (4) 蓝色总线：步骤 2 输入箱。
  </BoxAnnotation>
  <BoxAnnotation color="#5e7c16" min="6 0 1" max="7 1 3">
    (5) 绿色总线：步骤 3 输入箱。
  </BoxAnnotation>
  <BoxAnnotation color="#fed83d" min="7 0 2" max="8 1 4">
    (6) 黄色总线：步骤 4 输入箱。
  </BoxAnnotation>
</GameScene>

结构中的箱子代表四个步骤各自的包裹输入容器。包裹存储总线写入的是完整包裹，不会把内容直接放进箱子；实际搭建时，后续应由该步骤自己的拆包方式接收。示例样板中的材料只用于展示四列分组，按实际配方替换即可。

白色处理子网占用 5 个频道（包裹装配室与四个包裹存储总线），并只通过石英纤维从粉色主网取电。高级样板的列顺序决定装配室生成包裹的顺序，颜色决定每个包裹的目的地。如果步骤之间存在真正的前后依赖，仍应让上一步产物返回 ME 网络后再触发下一份加工样板，不要只依赖四列的输出顺序。

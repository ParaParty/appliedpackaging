---
navigation:
  parent: index.md
  title: 包裹
  icon: appliedpackaging:fluix_package
  position: 10
item_ids:
- appliedpackaging:fluix_package
- appliedpackaging:white_package
- appliedpackaging:orange_package
- appliedpackaging:magenta_package
- appliedpackaging:light_blue_package
- appliedpackaging:yellow_package
- appliedpackaging:lime_package
- appliedpackaging:pink_package
- appliedpackaging:gray_package
- appliedpackaging:light_gray_package
- appliedpackaging:cyan_package
- appliedpackaging:purple_package
- appliedpackaging:blue_package
- appliedpackaging:brown_package
- appliedpackaging:green_package
- appliedpackaging:red_package
- appliedpackaging:black_package
categories:
- applied packaging items
---

# 包裹

<Row gap="8">
  <ItemImage id="appliedpackaging:fluix_package" scale="3" />
  <ItemImage id="appliedpackaging:red_package" scale="3" />
  <ItemImage id="appliedpackaging:green_package" scale="3" />
  <ItemImage id="appliedpackaging:blue_package" scale="3" />
  <ItemImage id="appliedpackaging:black_package" scale="3" />
</Row>

包裹是真实物品，不是散装 ME 库存的视图。内容是有序列表，相同资源键的重复条目不会合并；颜色、标记、顺序和稀疏槽位布局都会参与包裹身份与堆叠判断。

包裹不能嵌套。打包输入中遇到包裹时，会在原位置展开其内容。潜行使用手持包裹可手动拆包，也可用 [ME 打包机](me_packager.md)或[包裹卸货总线](package_buses.md)自动处理。

基础容量为 9 单位、9 类型。受支持机器可安装 AE2 16k、64k 或 256k 存储元件来选择更高的共用容量档；存储元件成品和 1k 元件无效。

从样板编码到下游有序输入的完整路线见[封装工作流](workflow.md)。

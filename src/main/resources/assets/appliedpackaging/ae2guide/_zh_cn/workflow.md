---
navigation:
  parent: index.md
  title: 封装工作流
  icon: appliedpackaging:package_pattern
  position: 5
---

# 封装工作流

1. 在[高级样板编码终端](advanced_pattern_terminal.md)中编码包裹样板或高级样板。
2. 将样板放入朝向 [ME 包裹装配室](package_assembler.md)的样板供应器，或直接安装到装配室本地槽。
3. 让成品包裹进入普通 ME 存储；[包裹存储总线](package_buses.md)按包裹保存，包裹卸货总线则接收一个路由包裹并整批提交内容。
4. 下游机器要求稳定顺序或稀疏位置时，使用[序列缓存器](sequence_buffer.md)。

需要防止内部滞留时，在接收设备上开启防堵塞。它不会替代阻挡模式，而是把设备当前完整的自动输出预检提升为输入门禁；如果设计本就需要在本地等待，则关闭它。

可继续查看[示例布局](example_setups.md)，其中包含带标注的小型搭建示例和阻挡/防堵塞组合表。
若包裹被拒收、停在设备内或进入错误目的地，可继续查看[故障排查](troubleshooting.md)。

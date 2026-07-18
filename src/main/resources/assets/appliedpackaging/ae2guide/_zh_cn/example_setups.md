---
navigation:
  parent: index.md
  title: 示例布局
  icon: appliedpackaging:sequence_buffer
  position: 70
---

# 示例布局

这些布局用于说明各部件的职责，因此刻意保持精简。实际网络仍需按规模补充普通 AE2 存储、合成 CPU、频道以及机器对应的输入面。

## 样板供应器连接包裹装配室

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/package_assembly_line.snbt" />
  <IsometricCamera yaw="195" pitch="25" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="2 1 2">
    (1) 已供电的样板供应器一次推送完整样板批次。
  </BoxAnnotation>
  <BoxAnnotation color="#cc88ff" min="2 0 1" max="3 1 2">
    (2) ME 包裹装配室在消耗任何输入前先验证全部包裹容量。
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="3 0 1" max="4 1 2">
    (3) 相邻容器输出始终使用真实、可抽取的有序包裹列表。
  </BoxAnnotation>
</GameScene>

把已编码的[包裹样板或高级样板](advanced_pattern_terminal.md)放入样板供应器，或直接放入装配室本地样板槽。若使用 ME 网络输出，应连接装配室并选择“ME 网络”，不需要图中的箱子。每个新完成批次只检查一次阻挡；准入后会连续排空。

## 保存包裹或拆开包裹

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/package_routing.snbt" />
  <IsometricCamera yaw="195" pitch="25" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="2 1 2">
    (1) 包裹存储总线把左侧库存中的真实包裹暴露给网络。
  </BoxAnnotation>
  <BoxAnnotation color="#ffbb55" min="3 0 1" max="5 1 2">
    (2) 包裹卸货总线接收网络路由来的包裹，并原子填入右侧目标。
  </BoxAnnotation>
</GameScene>

用优先级决定包裹首先尝试哪个目的地。开启防堵塞后，卸货总线在完整目标预检失败时拒绝网络插入，让 AE2 继续尝试其它目的地；若设计允许一个可见 held 包裹等待，则关闭防堵塞。最终提交前，这个 held 包裹始终可以整包取回。

## 用序列缓存器维持输入顺序

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/sequence_line.snbt" />
  <IsometricCamera yaw="205" pitch="30" />

  <BoxAnnotation color="#66aaff" min="0 0 1" max="1 1 2">
    (1) 端点持有配置，自身不存储输入。
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="1 0 1" max="4 1 2">
    (2) 成员按稳定顺序锁存条目，并通过各自配置的方向输出。
  </BoxAnnotation>
</GameScene>

把[包裹卸货总线](package_buses.md)贴在端点上，即可原子输入一个完整包裹。需要保留已记录的稀疏位置时开启样板模式；同步输出要求所有已占用成员先同时通过预检，再开始提交；防堵塞则把相同的输出可行性判断提前到输入准入阶段。

## 如何组合阻挡与防堵塞

* **阻挡关、防堵塞关：** 接收合法输入；容量暂时不足时在本地等待。
* **阻挡开、防堵塞关：** 先接收合法输入，再等待目标满足阻挡条件。
* **阻挡关、防堵塞开：** 只有全部输出当前可容纳时才接收。
* **阻挡开、防堵塞开：** 目标还必须通过该机器真实的阻挡规则，否则拒收。

防堵塞不会代替最终提交校验。它只决定新输入能否进入；自动输出在真正提交前仍按原规则重新检查目标。

“阻挡”的具体含义因设备而异：ME 打包机检查目标 ME 存储是否存在可见内容；包裹卸货总线检查目标是否已有包裹中的任一输入类型；序列缓存器则要求所选目标完全为空。防堵塞始终纳入接收设备自身真实的阻挡规则。

需要按现象定位或安全取回内容时，参见[故障排查](troubleshooting.md)。

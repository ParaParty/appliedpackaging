---
navigation:
  parent: machines/index.md
  title: ME 打包机
  icon: appliedpackaging:me_packager
  position: 20
item_ids:
- appliedpackaging:me_packager
categories:
- applied packaging machines
---

# ME 打包机

<BlockImage id="appliedpackaging:me_packager" scale="8" />

ME 打包机从[网络存储](ae2:ae2-mechanics/import-export-storage.md)中提取物品并将其打包为包裹。也可以接收包裹并将其内容物拆包回网络存储。

打包机通过底面和背面连接 AE2。正面传送带存放当前包裹。右键传送带表面可放入或取出包裹；右键其他面打开配置 GUI。传送带槽与 GUI 显示同一存储。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../../assets/assemblies/me_packager_network.snbt" />
  <IsometricCamera yaw="195" pitch="30" />

  <BoxAnnotation color="#dddddd" min="0 0 0" max="2 1 1">
    (1) 箱子与 AE2 存储总线：将散装物品接入网络存储。
  </BoxAnnotation>
  <BoxAnnotation color="#66aaff" min="1 0 0" max="4 1 1">
    (2) 智能线缆：连接存储、打包机和供电。
  </BoxAnnotation>
  <BoxAnnotation color="#cc88ff" min="3 1 0" max="4 2 1">
    (3) ME 打包机：通过底面接入网络。
  </BoxAnnotation>
  <BoxAnnotation color="#66dd88" min="4 0 0" max="5 1 1">
    (4) 能源元件：为示例网络供电。
  </BoxAnnotation>
</GameScene>

## 打包

配置颜色、可选标记、内容过滤器和激活模式。打包机触发时，扫描 ME 存储中匹配过滤器的物品，装配一个包裹。物品从网络提取，非复制。

内容过滤器初始两行。每张 <ItemLink id="ae2:capacity_card" /> 增加一行，最多三张卡共五行。<ItemLink id="ae2:inverter_card" /> 反转内容匹配。

## 激活模式

打包机可由红石触发或设为持续运行：

*   **有红石信号时打包**——接收到红石能量时打包。
*   **无红石信号时打包**——未接收红石能量时打包。
*   **总是打包**——持续打包。
*   **红石脉冲时打包一次**——每次上升沿执行一次。
*   **关闭打包**——仅在 GUI 中点击手动打包按钮时执行。

## 拆包

通过传送带或 GUI 将包裹放入打包机，其内容物拆包回 ME 存储。阻挡模式在网络已含有匹配物品时阻止拆包。预接收检查（GUI 标注"防堵塞模式"，默认开启）在接受包裹前验证完整内容物能否装入 ME 存储。

## 过滤模式

打包机可设为仅对打包、仅对拆包或双向应用内容过滤：

*   **打包和拆包都启用过滤**——过滤器双向生效。
*   **只对打包启用过滤**——过滤器仅限制打包内容。
*   **只对拆包启用过滤**——过滤器仅限制拆包内容。

## 升级

ME 打包机支持以下[升级](ae2:items-blocks-machines/upgrade_cards.md)：

*   <ItemLink id="ae2:speed_card" /> 缩短打包和拆包工作周期（最多 6 张）
*   <ItemLink id="ae2:capacity_card" /> 增加内容过滤行（最多 3 张，共 5 行）
*   <ItemLink id="ae2:inverter_card" /> 反转内容过滤

打包机也有存储组件槽，接受 16k、64k 和 256k 原始组件以提升包裹容量。

## 合成配方

<RecipeFor id="appliedpackaging:me_packager" />

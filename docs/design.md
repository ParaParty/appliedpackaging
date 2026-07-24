# Applied Packaging 设计规格

本文是 Applied Packaging 的唯一产品与实现设计真源。它只描述当前正式实现，不记录讨论过程、阶段计划、历史兼容方案或已经撤销的设计。

## 1. 基线与目标

```text
mod_id: appliedpackaging
Minecraft: 1.20.1
loader: Forge 47.4.10+
toolchain: ModDevGradle Legacy
AE2: 15.4.10, range [15.4.10,16)
GuideME: 20.1.7, range [20.1.7,20.2.0)
Java: 17
```

Applied Packaging 为 AE2 增加“包裹化物流”：把应当一起移动、路由、等待或拆入机器的一批资源封装为一个有颜色、可标记、可过滤的物流单元。

它不是通用压缩库存，也不是普通箱子替代品。包裹内部内容不能作为 AE2 散装库存暴露。

## 2. 产品边界

- 注册 Fluix 与 16 种染料色，共 17 个独立包裹物品。
- 玩家没有正常获得空包裹的路径；没有有效 `PackageData` 的物品不参与物流。
- 包裹允许堆叠，但只有颜色、marker、flags、有序 contents 和布局完全一致时才能合并堆叠。
- 不允许真实包裹嵌套；再次封装时按原位置展开源包裹内容。
- 不提供 Package Export Bus、独立 Package Pattern Terminal 或旧开发版方块/载体。
- ME 打包机不读取 Pattern Provider，也不执行样板。
- ME 包裹装配室不扫描相邻存储，也不主动拆包。
- 正式发布前不维护开发版注册表 ID、NBT schema 或存档迁移。

## 3. 包裹数据模型

`PackageData` 的核心内容是有序 `List<GenericStack>`，每项为 `AEKey + amount`。数据层不自动合并同类条目，也不排序；同一个 AEKey 可以在多个位置重复出现。

需要恢复样板空位时，包裹额外保存：

```text
layout.slotCount
layout.positions[]
```

布局参与 canonical hash、NBT 身份和堆叠判断。marker 只接受数量归一为 1 的物品 AEKey。容量按 AE2 cell 单位计算，类型数按去重后的 AEKey 计算。

有效包裹必须满足：

- schema 为当前版本；
- contents 非空，所有 amount 为正；
- layout 与 contents 数量、位置范围一致；
- usedUnits、usedTypes 与重新计算结果一致；
- canonical hash 与颜色、flags、marker、contents、layout 一致。

## 4. 容量档

ME 包裹装配室与 ME 打包机共用容量规则：

| 容量元件 | 单位上限 | 类型上限 |
| --- | ---: | ---: |
| 空槽（默认 1k 档） | 256 | 9 |
| 16k component | 4,096 | 16 |
| 64k component | 16,384 | 63 |
| 256k component | 65,536 | 63 |

1k component 与空槽同档，不作为升级物接受。完整 storage cell、portable cell、4k component 与未列出的附属容量档不接受。

## 5. ME 包裹装配室

装配室是样板驱动的打包机器，支持本地已编码样板与 AE2 Pattern Provider 的 `pushPattern`。

样板语义：

- `package_pattern` 生成一个保留颜色、marker、顺序和 sparse 布局的包裹。
- `advanced_processing_pattern` 按列生成多个包裹；每列保留自己的颜色、marker 与 sparse 行布局。
- AE2 可解码的 crafting、processing、stonecutting、smithing 等普通样板按编码顺序生成一个包裹，主输出默认归一为 marker。
- 空白、未编码或无法解码的样板不能解锁输入或开始工作。

输入与输出只维护一份 AEKey 库存：

- 通过 `GenericInternalInventory` 与 `Capabilities.STORAGE` 暴露；
- 不实现独立 `IItemHandler` capability；
- 物品、流体及其它 AEKey capability 由 AE2 的通用包装层派生；
- 输入槽按样板位置精确过滤，重复 AEKey 仍是多个独立位置；
- 外部只可从真实有序输出抽取。

本地合成在进度完成前不预扣材料。材料不足时暂停并保留计划；样板改变时取消计划。提交时重新验证全部位置并原子扣料，失败不得产生部分结果。

任何输入消费前都要对将生成的每个包裹做容量预检。超限本地样板可以留在槽内供检查，但输入锁定且不能工作；`pushPattern` 超限时整批拒绝且不消费 `KeyCounter`。

输出目标为当前 ME 网络或六个相邻目标之一。阻挡模式只在新批次准入时检查一次；准入后按真实队首顺序尽可能提交，目标中途拒绝时保留剩余成品并重试。比较器只表达空闲 0、工作 1、成品待取 2。

工作开始与结束必须通过 AE2 block-entity 更新流同步客户端；工作期间渲染动画灯光、当前包裹和向中心汇聚的合成粒子。

## 6. ME 打包机

打包机处理相邻存储端点，不执行样板。

- 方块只有水平 `facing`；底部与模型背面固定接入同一个 AE2 节点。
- 其它面不能接入 ME 线缆；扳手旋转同时旋转模型、动态件、背面接线面和交互区域。
- 只在传送带上表面执行手动放入/取出；其它点击打开 GUI。
- 可从相邻 item handler、fluid handler 或 AE2 `MEStorage` 打包，也可把合法包裹完整拆入目标。
- 重新打包源包裹时先展开内容；只有打包机路径允许为稳定输出执行确定性排序。
- marker 过滤与内容过滤是两条独立门禁；反转卡只反转内容过滤。
- 防堵塞默认开启，接收包裹前复用真实自动拆包的完整目标、容量和阻挡判定；关闭后允许一个合法包裹在 held 槽等待重试。
- 模拟和提交必须一致；失败保留原包裹，不允许部分拆包。

动态渲染只读取服务端同步的工作包裹。传送带停止后保留 UV 相位，后续工作从该相位继续。

## 7. 样板与高级终端

正式自有载体只有：

```text
appliedpackaging:package_pattern
appliedpackaging:advanced_processing_pattern
```

两种样板都在 Advanced Pattern Encoding Terminal 中编辑。终端是 AE2 cable part，在同一个 Screen、Menu 与容器中提供高级页和包裹页：

- 页面切换不重新打开、resize 或初始化 Screen；
- 两页输入、输出、marker 与颜色状态完全隔离；
- 放入对应载体时自动切到对应页面并载入状态；
- 普通 AE2 Pattern Encoding Terminal 只通过窄菜单验证拒绝这两种专用载体，不增加页面、不替换 Screen；
- 高级页逻辑为最多 81 列、每列最多 81 个 sparse 输入位置，菜单只同步 4 列可见输入窗口；输出容量遵循 AE2 processing pattern 的 27 槽上限并通过纵向滚动编辑；
- 两页输入槽都按当前 ME 网络的可合成状态显示 `+` 叠加与 Craftable 提示，不把该提示写入样板；
- 编码 NBT 始终使用当前逐列 schema，AE2 根 `in` 只保存稠密执行输入；
- 包裹页提供最多 81 个 sparse 输入、颜色、marker 和包裹预览；
- 配置修改经过服务端 action，完整计划校验通过后一次替换，不能部分写入。

颜色选择统一使用一个弹窗。只有 Package Storage Bus 与 Package Unpacking Bus 的过滤行允许 None；其它机器与终端只能选择 Fluix 或 16 种染料色。

## 8. Package Bus

只保留两个 AE2 cable part：

- Package Storage Bus：只向 AE2 枚举相邻 item handler 中的合法包裹物品，不暴露内部内容。
- Package Unpacking Bus：像 Formation Plane 一样接收网络路由来的一个包裹，并把内容完整推入相邻目标。

两者共用最多 7 行 OR 过滤规则。每行包含可为空的颜色、marker、6 个内容项，以及在对应升级卡存在时可用的 fuzzy/inverted 状态。默认 2 行，每张 capacity card 增加 1 行，最多 5 张。

卸货总线：

- held 包裹始终作为数量 1 的包裹向网络可见并可整包取回；内部内容不可见；
- 支持最多 4 张 speed card，工作周期为 20/15/10/6/4 tick；
- 阻挡开启时目标必须完全为空，而不是只检查包裹中的同类 key；
- 普通目标通过 AE2 Pattern Provider 的外部存储扩展链解析，支持已注册的 AEKey 类型；
- 序列缓存器使用专用原子位置计划；其它目标先累计模拟再按原 contents 顺序提交；
- 最终检查失败时保留 held 包裹并重试；取回或拆除 part 时原子取消工作且不提交内容。

同优先级下，能接收该包裹的卸货总线先于存储总线；卸货总线忙碌、过滤拒绝、阻挡或容量不足时才继续路由到存储总线。

## 9. 序列缓存器

序列缓存器是沿 X、Y 或 Z 轴成型的直线多方块。端点负责配置、菜单、AE2 接口和自动输出；后续成员各自保存一个通用 AEKey 锁存槽，端点自身不计入存储序列。

- 单格首次实际插入后，在内容清空前拒绝继续插入，即使 AEKey 相同。
- 抽取可部分执行；清空后到下一 game tick 才重新开放输入，开放时间写入 NBT。
- 端点的 `MEStorage` 聚合全部成员；普通成员与未成型单块只暴露本格。
- 自动输出、阻挡、同步输出、样板模式和输入延迟由端点统一控制。
- 样板模式按包裹 layout 恢复空位；关闭时按 contents 顺序连续写入。
- 阻挡模式要求目标完整可见库存为空。
- 成型、扩展、重建和解体不覆盖每个方块自己的扳手方向。
- 端点主界面显示成员窗口和设置；成员/单块界面只显示本格存储，成员可以跳转端点。

## 10. JEI、EMI、Create 与 GTCEu

JEI 与 EMI 是两个独立的薄前端：

- JEI 只读取 `IRecipeSlotsView`；
- EMI 只读取 `EmiRecipe` / `EmiCraftContext`，且只处理 `FILL_BUTTON`；
- JEMI 交给 EMI 自带的 JEI transfer 路径；
- 两者只共享不依赖 viewer API 的提取结果、候选选择、语义编码与 `PatternTransferPlanFactory`；
- 不读取 viewer 私有字段，不反射发现本项目集成模块，也不把查看器声明为发布硬依赖。

通用确定性配方、Create processing、Create Sequenced Assembly、Mechanical Crafting 与 GTCEu item/fluid/tick 输入分别使用明确语义。随机、范围、动态世界、不可确定输出、超容量或无法完整表达的配方必须拒绝，终端状态不得被部分修改。

GTCEu 编译基线为 7.5.3。Star Technology Fork 的 layered recipe 只通过固定公开 layer API 兼容；每个 layer 对应一个包裹列，layer 内材料保持同列。公开路径无法完整恢复 layer 边界时拒绝扁平化回退。

## 11. 客户端与资产

- 客户端 Screen、renderer 和查看器集成必须与 dedicated server 类加载隔离。
- UI 使用项目资源与已记录来源的 AE2 回移资源，不覆盖 AE2 原版终端 ScreenStyle。
- 包裹物品与世界实体使用同一 3D 模型。
- ME Packager 使用正式空心框架、双周期传送带和四条帘子模型。
- ME Package Assembler、Advanced Pattern Terminal、Package Bus 和 Sequence Buffer 使用各自正式模型与 GUI；不保留概念图、历史预览或替换前资源。
- 发布 PNG 必须是已知路径、有效 RGBA、尺寸符合运行时约束、具有可见非占位内容；模型贴图引用必须可解析。
- 复用 AE2 资源时保留对应 LGPL 来源记录于 `src/main/resources/META-INF/licenses/`。

## 12. 实现原则

1. 先模拟完整操作，再提交；无法原子完成时保留源状态。
2. contents 的顺序与重复位置是业务数据，不能为方便而合并。
3. 包裹只作为包裹路由，不伪装成散装 AE2 库存。
4. 当前实现优先于历史讨论；删除的设计不得以兼容壳重新引入。
5. 可选集成只能在目标 Mod 存在时加载，发布元数据保持软依赖。
6. 客户端视觉改动必须实际启动客户端检查；服务端行为改动必须通过 GameTest。

## 13. 上游依据

- Forge 1.20.1 / 47.4.10 与 ModDevGradle Legacy 2.0.x。
- AE2 Forge 15.4.10 是运行基线；部分 UI 与模型回移固定参考 AE2 `neoforge/v19.2.17` 或已记录的 current-main 资源。
- GuideME 20.1.7。
- JEI 15.20.0.134、EMI 1.1.24+1.20.1、Create 6.0.8、GTCEu 7.5.3。
- Star Technology Fork 1.7.0b 只作为可选运行时兼容验证，不进入发布硬依赖。

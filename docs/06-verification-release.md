# 验证与发布

## 1. JVM 测试

必须覆盖：

```text
PackageData canonical hash 稳定
同内容不同顺序 hash 不同
不同颜色 hash 不同
marker 冲突拒绝
容量单位计算正确
过滤 AND 规则正确
```

## 2. GameTest

必须覆盖：

```text
合法包裹 NBT 可读写
无 PackageData 的包裹被拒绝
打包事务成功提交
输出槽满时打包不改变源库存
拆包目标容量不足时不消耗包裹
包裹堆叠拆包只接受完整数量
过滤不匹配拒绝
容量不足拒绝
序列缓存器模拟输入不锁存，真实输入一次锁存，完全清空后解锁
序列缓存器成型、拒绝与尾端扩展保持唯一端点和稳定顺序
序列缓存器断裂不丢失缓存，端点侧保留、尾侧解散
序列缓存器端点不存储、首个成员为逻辑第 1 格、端点顺序输入/合并抽取且不自动输出
序列缓存器阻挡、同步和输入延迟同时约束主动/被动输出
序列缓存器普通样板保留 sparse 空位、高级样板直接 push 使用稠密顺序且 Pattern Provider 原子消费
普通/包裹/高级列样板包裹布局参与身份/NBT/hash；拆包总线在样板模式下原子保留空位，关闭时连续输入
序列缓存器 item/fluid handler 分别拒绝错误 key 类型，MEStorage 保留泛型 key
序列缓存器主/侧菜单分别映射端点成员序列与被点击本格，端点本身不生成存储槽
序列缓存器过滤假槽、红石卡 NBT/成型迁移和红石门控自动输出正确
序列缓存器 27 格不滚动、28 格开始滚动，未占用显示位置保持禁用
JEI 高级/包裹计划 payload 往返、原子替换旧状态并保留页面自身颜色
标准 JEI INPUT/OUTPUT/CATALYST/RENDER_ONLY 角色映射、歧义输出和随机产出拒绝
真实 Create 确定性 Sequenced Assembly 按初始输入和工序顺序展开
真实 Create Mechanical Crafting 自动选择更少分组的行或列，并保持组内顺序、前导空位和中间空位
真实 GTCEu 确定性 item/fluid recipe 映射且不超过终端边界
```

当前已覆盖：

```text
PackageData 合法 NBT 可读写
无 PackageData 的包裹被拒绝
包裹保留同类重复条目和输入顺序；同内容不同顺序的包裹生成不同 canonical hash、不同 NBT 且不可堆叠
颜色、marker 或内容不同的包裹生成不同 canonical hash
canonical hash 被篡改时拒绝
缺失 canonical hash 时拒绝
schema version 不支持时拒绝
空过滤接受合法包裹
颜色不匹配时过滤拒绝
颜色/marker/内容同时匹配时过滤接受
内容不在 allowlist 中时过滤拒绝
包裹计划会展开源包裹内容
retain marker 冲突时计划拒绝
override marker 时计划使用覆盖 marker
clear marker 时计划清除 marker
默认容量超限时计划拒绝
item handler 拆包可完整插入目标
目标满且不兼容时拆包拒绝
默认容量下超量源库存只规划可承载的最大包裹
装配室可从输入缓冲生成包裹
装配室输出阻挡时不消耗输入
装配室输入包裹会展开后再封装
装配室可使用已编码 package_pattern 精确匹配输入计划
装配室安装支持的容量元件后，可使用已编码 package_pattern 重封装大于空槽 9 单位上限的源包裹
装配室在 package_pattern 输入已缓存后移除容量元件，会恢复空槽 9/9 并拒绝本地执行且保留输入
装配室可用容量槽重封装超过默认容量的源包裹
装配室可在不清空首个输出槽时使用额外输出槽生成后续处理包裹
装配室可接受 AE2 Pattern Provider pushPattern 的物品输入
装配室可接受 AE2 Pattern Provider pushPattern 的流体输入
装配室普通 Pattern Provider pushPattern 可用容量槽承载超过 9 个物品栈的输入
装配室默认容量下拒绝超量普通与高级 Pattern Provider pushPattern，且在容量预检失败时不消耗任一输入
装配室输出阻挡时拒绝 Pattern Provider pushPattern 且不消耗输入
装配室自动导出开关默认开启，可通过菜单按钮切换并同步到 menu state
装配室 GUI 可见窗口为 4x4 输入格和 4 个输出格，滚动时输入/输出行同步映射
装配室 GUI 真实输入缓冲可按 package_pattern 过滤材料并累计超过普通 stack size 的数量
装配室空输入槽不把样板过滤物品或数量作为槽内容显示；真实插入后才显示对应输入物品
装配室 Forge item capability 在无本地样板时拒绝输入；安装样板后按位置过滤外部输入且只允许从有序输出位抽取
装配室本地样板超出当前容量档时同步 GUI 无效状态、拒绝 GUI/外部输入与本地装配；安装足够容量元件后立即解锁，移除后重新锁定并保留已有输入
装配室 package_pattern 的外部输入输出保留重复 AEKey 条目和样板输入顺序
装配室自动导出设置可保存/读取
装配室 server tick 可把输出包裹导出到相邻 Forge item handler
装配室方块声明原版模拟输出支持，比较器只输出空闲或已清空 0、正在打包 1、存在未清空完成成品 2，同批包裹数量不得改变完成态数值
装配室不锁存比较器值也不为比较器延迟物品自动输出；开始打包、成功提交与自动清空三个边界均预约朝向本机的直连/隔实体方块原版比较器 0-delay 计划刻，使其逐次采样 `0 -> 1 -> 2 -> 0`
装配室连续自动输出的 GameTest 必须同时覆盖直连与隔一个实体方块的原版比较器，并断言成功装配提交次数、导出包裹数、`0 -> 1` 上升沿次数和状态 2 采样次数相等；一次多包批次按一次成功提交计脉冲
装配室自动清空完成列表的当 tick 不得启动下一次装配；带成品从 NBT/区块重新加载后不额外等待比较器窗口，首个 server tick 可立即自动导出
ME 打包机内/外 item capability 与装配室 item/CRAFTING_MACHINE capability 在 invalidate 后使旧 handle 失效，并在 revive 后恢复可用
真实 AE2 Interface 网络可接收装配室自动导出的包裹物品
真实 AE2 Creative Energy Cell + Pattern Provider 方块网络可推送处理样板输入到装配室
真实 AE2 Drive + 64k item cell + Crafting CPU + Pattern Provider 方块网络可提交自动合成 job，并把 processing pattern 或 package_pattern 输入推送到装配室
AE2 PackageItemStorage 只暴露合法包裹
AE2 PackageItemStorage 拒绝散装物品插入
AE2 PackageItemStorage 可模拟并提交合法包裹插入/抽取
AE2 PackageItemStorage 按 PackageFilter 限制可见、插入、抽取包裹
AE2 PackageItemStorage 累计模拟不会让多个包裹重复占用同一 slot 空余容量
item handler 拆包累计模拟会拒绝多种内容共同超过同一 slot 容量
MEStorage 打包提交遇到源库存变化时回滚此前真实抽取
MEStorage 拆包在共享目标容量于提交阶段不足时回滚此前真实插入
包裹总线可保存、拒绝非法项并清除 ghost 过滤模板
包裹卸货总线以 Formation Plane 式受限 IStorageProvider 直接接收网络路由包裹，接收阶段不提前写入目标；唯一 held 工作包裹以数量 1 枚举，并可由网络或 GUI 在工作中整包取回
真实 AE2 Package Unpacking Bus 与 Package Storage Bus part 的默认优先级都为 0；在 20 tick 进度完成前保持目标不变，不扫描或主动抽取其它 ME 存储
两个 part 使用右上角 Priority 子菜单对应的同一 `IPriorityHost` 数值；数值不同时由较高值先接收，同值时 Package Unpacking Bus 先接收，拆包忙碌/拒绝时才回落到 Package Storage Bus
Package Unpacking Bus 的 Pattern Provider 阻挡模式会在目标已有任一包裹内容物类型时拒绝网络接收；阻挡清除后直接接收并开始 held 工作
真实 AE2 Package Unpacking Bus part 在最终目标变化或阻挡恢复时保留同一个 held 包裹、保持全量未提交，并在条件恢复后重新执行完整进度并成功提交；工作或阻塞期间由 ME/GUI 取回则原子取消且不产生部分内容
Package Storage Bus 与 Package Unpacking Bus part 均只暴露 5 个共享升级槽，不按兼容卡种上限扩成 9 格；两者允许 5 格全部安装容量卡，第 6 张拒绝，并按基础 2 行加 5 张容量卡解锁到 7 行
两个 Package Bus 的颜色空模式不限制包裹颜色；Package Storage Bus 的 Partition Storage 从相邻容器中的不同合法包裹生成多行过滤、跳过散装物品并在没有包裹时清空过滤
真实 AE2 Package Storage Bus part 只挂载相邻合法包裹，可从网络抽取包裹但不暴露同一库存中的散装物品
Package Assembler 有序输出 handler 实现 `IItemHandlerModifiable`，可由 `SlotItemHandler#set` 完成客户端菜单槽同步
package_pattern 数据可读写；普通 AE2 Pattern Encoding Terminal 的已编码样板槽拒绝 package_pattern 与 advanced_processing_pattern
advanced_pattern_encoding_terminal 是 AE2 part item，使用同一 part/menu/screen 提供 ADVANCED 与 PACKAGE 两页，右侧按钮切换且关闭重开后恢复上次页面
高级页与包裹页输入输出库存完全隔离；手动切换不复制或清空另一页状态，放入 advanced_processing_pattern/package_pattern 时自动切到对应页并只载入对应状态
包裹页可从真实 AE2 blank pattern 编码带颜色和 marker 的独立 package_pattern；自动预览必须是包裹而不是相同输入对应的 crafting 产物
高级页可从真实 AE2 blank pattern 编码独立 advanced_processing_pattern，保留每列 81 槽映射与颜色，名称固定为空、marker 固定取主产物并归一为数量 1，且忽略未启用列残留输入
advanced_pattern_encoding_terminal 菜单不创建 VIEW_CELL 槽，客户端不绘制显示元件面板
advanced_pattern_encoding_terminal 列编辑层覆盖 AE 网络库存前景，底层 RepoSlot/processing slot/编码按钮不可见穿透或接收弹层输入；外部点击只关闭弹层
装配室执行普通 AE2 crafting/processing 等可解码样板时固定输出 Fluix、空名称，并把主输出归一为数量 1 的 marker，不读取机器身份配置
装配室执行 advanced processing pattern 时按连续 81 槽列生成有序多包裹，同色列不合并，并严格消费 Pattern Provider 输入
全部样板输出的包裹 contents 与各自样板输入顺序完全一致，不合并重复 AEKey，不经过 ME 打包机排序
package_pattern 与 advanced_processing_pattern 不作为普通配方或创造栏直接产物，关键机器、终端和总线配方仍可加载
装配室可读取独立 package_pattern 并生成匹配包裹，且该 pattern 不支持分子装配室执行
PackageFilter 可按流体 key 匹配内容 allowlist
过滤系统可从已编码 package_pattern 读取过滤模板
ME Packager 可识别 AE2 16k/64k/256k storage component 并映射到对应包裹容量档
两台包裹机器容量槽拒绝 AE2 1k component、完整 storage cell 与 portable cell
包裹物品丢出后替换为 appliedpackaging:package 实体并保留包裹数据
两台包裹机器空容量槽固定为 9 单位/9 类型
两台包裹机器通过同一 PackageCapacityProfile 映射 16k/64k/256k storage component
ME Packager 底部与模型背面不暴露普通 item capability，其它四面暴露包裹输入/输出 capability；外部输入在无 AE 目标或不可完整拆包时拒绝
ME Packager 外部 capability 接受包裹时直接提交拆包、保持 inputSlot 为空并进入 unpacking 工作态
ME Packager 菜单 shift-click 玩家背包包裹时每次只直接拆包 1 个，保持 inputSlot 为空，并在 working 期间拒绝继续输入
ME Packager 外部 capability 输入同时校验当前非默认颜色、marker 槽、内容 allowlist 和目标可接收性
ME Packager 安装反转卡后反转内容过滤，但不反转颜色或 marker 门禁
ME Packager 只有底部与模型背面可接入 ME 网络，并分别通过真实 AE2 Interface 与真实 AE2 线缆网络完成打包；扳手旋转后背面连接随 facing 改变
ME Packager 打包先抽取源内容并进入 packing 工作态，动画结束后才把包裹放入 outputSlot
ME Packager 传送带完成工作动画后保留 UV 滚动相位，且该相位经过方块实体保存/读取后不复位
ME Packager 不会回落到相邻 Forge item handler 或 Forge fluid handler
ME Packager 菜单可切换新版打包激活模式
ME Packager 安装红石卡后红石上升沿只执行一次
ME Packager 默认/高信号持续打包模式在供电时周期执行
ME Packager 红石关闭模式只停止打包，不阻止输入包裹自动拆回 AE 网络
ME Packager 容量卡按 2+3 行规则解锁过滤槽
MEStorage 打包计划可从 AE2 storage 抽取 GenericStack 内容
MEStorage 打包计划在生成包裹前按 canonical stack key 确定性排序所选内容
MEStorage 拆包可把包裹完整插入 AE2 storage
MEStorage 打包计划会展开 storage 中已有源包裹再封装
MEStorage 打包计划在显式 clear 模式移除源包裹 marker
真实 AE2 Interface 网络 smoke 可从 Drive 存储打包并整包拆回网络
包裹规划拒绝容量单位累计发生 long 溢出的输入，同一 AEKey 的多个条目保持分离
手动整叠拆包拒绝单包数量乘包裹数发生 long 溢出的输入，并保留完整包裹堆叠
真实世界相邻 Forge fluid handler smoke 反例确认 ME Packager 无 MEStorage 时不回落、不消耗流体槽
当前最新执行：2026-07-10 补齐总线 REQUIRE_CHANNEL、存储总线在线缓存刷新、菜单 host 驱动 ghost 刷新和高级终端真实编码/marker 语义后，执行 `.\gradlew.bat runGameTestServer --stacktrace` 成功，164 个必需 GameTest 全部通过。
2026-07-15 Package Unpacking Bus 改为 Formation Plane 式只写入接收并加入 Pattern Provider 阻挡，Package Storage Bus 补齐容器包裹分区，同时补齐颜色空模式后，执行 `.\gradlew.bat runGameTestServer --stacktrace` 成功，103 个必需 GameTest 全部通过。
2026-07-15 包裹 contents 改为顺序敏感且不合并同类条目，ME Packager 独立执行确定性排序，Package Assembler 增加本地样板门控的外部输入 capability 后，执行 `.\gradlew.bat runGameTestServer --stacktrace` 成功，108 个必需 GameTest 全部通过。
2026-07-16 删除重复的 AEPatternDecoder/EncodedPatternItem Mixin、将包裹样板解码与输出展示收回 `PackageCraftingPatternItem` 正常 override，并将容量档收敛为 9/16/64/256 后，执行 `.\gradlew.bat runGameTestServer --stacktrace` 成功，109 个必需 GameTest 全部通过。
2026-07-16 装配室与打包机容量元件识别收敛到共享 `PackageCapacityProfile`，装配室增加本地样板 GUI/外部输入门禁与 Pattern Provider 消费前逐包容量预检后，执行 `.\gradlew.bat runGameTestServer --stacktrace` 成功，112 个必需 GameTest 全部通过。首次复跑中既有 70 输入动态槽用例仍安装 16k 元件而按新规则正确超限，测试夹具改用 256k 以继续只验证超过旧 68 槽上限的动态输入能力，未放宽产品容量断言。
2026-07-16 装配室本地样板槽扩展为任意 AE2 可解码已编码样板，普通 crafting/processing 样板改为 Fluix 包裹并使用归一化主输出 marker；本地输入改为合成完成时才扣除，合成中可取出，缺料暂停并在补齐后继续。新增 crafting pattern 重复输入顺序、Pattern Provider 推送、主输出 marker、本地取料暂停/恢复与待处理本地计划持久化/破坏掉落 GameTest 后，执行 `.\gradlew.bat runGameTestServer` 成功，115 个必需 GameTest 全部通过。
2026-07-18 包裹装配室阻挡与连续输出最终对齐 AE2 Pattern Provider 的批次准入语义，同时保留装配室真实有序输出列表。容器测试在目标放置无关金锭和匹配红色包裹，确认只有匹配包裹类型阻挡；移除匹配包裹但保留金锭后，同一 tick 清空两列批次。容量受限 Chest 测试让三包批次只写入第一包，GUI 从活动列表取走第二包，下一 tick 在目标仍有同色第一包时继续写入第三包；随后生成的第二批则重新阻挡，且三个队首仍可由 GUI-backed handler 逐个取走。相邻输出测试把 Chest 放在装配室顶面，确认容器模式没有背面概念。真实 Drive + Interface 测试向本机网格同时放入无关铁锭和匹配 Fluix 包裹，只移除匹配包裹后装配室即直接写入所连 MEStorage。`.\gradlew.bat runGameTestServer --stacktrace --no-configuration-cache` 成功，154/154 required GameTest 全部通过。

2026-07-16 收紧装配室普通样板的逐位置校验和完成扣料事务：重复 AEKey 的样板位置必须对应独立输入槽与独立包裹条目，完成时先在副本中验证全部槽位扣料，任一失败不得提交输出。拆包 item handler 路径明确按 contents 顺序逐条推入且不预聚合重复 key。新增“两个独立橡木板条目拆入两个装配槽、合成中保留、完成时同时扣除、输出仍为两个条目”的联动 GameTest；执行 `.\gradlew.bat runGameTestServer` 成功，116 个必需 GameTest 全部通过。
本轮新增高级终端编码测试首次失败并暴露真实 marker 丢失：AE2 `ConfigInventory.CONFIG_TYPES` 会把类型槽 GenericStack amount 固定为 0，旧代码误用 `amount <= 0` 判断空槽。改为读取 `getKey()` 并在样板数据中归一为 1 后复跑通过；未删除或放宽该断言。
本轮首次执行 152 个测试时，既有 `damagedPackageEntityUnpacksContentsToWorld` 因用铁/铜统计附近掉落而被新增测试布局污染；改用该场景唯一的 NETHER_STAR/DRAGON_BREATH 后稳定。总线端点测试初版还暴露 bus -> Drive -> energy 拓扑未让目标总线上线，改为总线直连 Creative Energy Cell、Drive 接另一面后通过；这些均为测试场景修正，不放宽产品断言。
2026-07-08 首次复跑 `.\gradlew.bat runGameTestServer` 暴露旧 `damagedPackageEntityUnpacksContentsToWorld` 测试对掉落实体统计范围/时序过宽的问题；收紧为掉落点附近等待式断言后复跑通过。
2026-07-06 在 ME Package Assembler 接入 AE2 `UpgradeableMenu` 后改为按 AE2 实际 slot index / slot semantic 处理滚动槽和玩家背包，执行 `.\gradlew.bat runGameTestServer` 成功，133 个必需 GameTest 全部通过。
2026-07-03 06:15 再次执行 `.\gradlew.bat runGameTestServer` 成功，112 个必需 GameTest 全部通过。
2026-07-03 06:27 再次执行 `.\gradlew.bat runGameTestServer` 成功，112 个必需 GameTest 全部通过。
2026-07-03 06:40 在发布 jar 排除 dev verification classes 后再次执行 `.\gradlew.bat runGameTestServer` 成功，112 个必需 GameTest 全部通过。
2026-07-04 06:19 在 Package Pattern Terminal Split 输出收敛到 AE2 blank_pattern carrier 后再次执行 `.\gradlew.bat runGameTestServer --stacktrace` 成功，112 个必需 GameTest 全部通过。
2026-07-06 首次复跑 `.\gradlew.bat runGameTestServer` 暴露 `packageAssemblerMenuInputUsesPatternFilterAndLargeAmount` 失败，原因是 AE2 `UpgradeableMenu` 玩家槽顺序为 hotbar 优先，旧 `HOTBAR_START` 常量仍按主背包优先顺序计算；修正菜单为按 AE2 实际 slot index / `SlotSemantics.PLAYER_HOTBAR` / `SlotSemantics.PLAYER_INVENTORY` 处理后复跑通过。
```

1.20.1 运行要求：

```text
GameTest template 存放在 gameteststructures/*.snbt。
copyGameTestStructures 会在 prepareGameTestServerRun 前复制到 run/gameteststructures。
当前空模板为 gameteststructures/empty.snbt。
```

GameTest 决策规则：

```text
打包、拆包、库存、过滤、容量、网络、数据包、保存读取、红石触发等行为敏感变更必须考虑 GameTest。
如果未添加或未运行 GameTest，必须在 development-log.md 记录原因。
```

## 3. DataGen 验证

必须运行：

```powershell
.\gradlew.bat runData
```

检查：

```text
生成资源纳入 git
recipe 可加载
loot table 可加载
blockstate/model/item model 路径正确
language key 完整
```

当前资产验证：

```text
5 个 docs/assets/contracts/*.yaml 均通过 assetgen validate-contract
PNG/JSON/model 引用机械检查通过
资源抽样视觉检查通过
已按 AE2 forge/v15.4.10 源码资产生成参考 sheet，并基于参考完成二轮材质重做
143 个 PNG 尺寸/模式/模型引用检查通过
55 个 JSON 可解析
.\gradlew.bat runData 成功
2026-07-03 05:35 再次执行 .\gradlew.bat runData 成功，未写出新的 generated resources 内容
2026-07-03 05:43 再次执行 .\gradlew.bat runData 成功，未写出新的 generated resources 内容
2026-07-03 06:14 再次执行 .\gradlew.bat runData 成功，未写出新的 generated resources 内容
2026-07-03 06:16 轻量资源审计通过：英文/简体中文语言 key 对齐，52 个 PNG 非空。
2026-07-03 06:26 再次执行 .\gradlew.bat runData 成功，未写出新的 generated resources 内容。
2026-07-03 06:27 资源审计通过：60 个 PNG 非空，55 个 JSON 可解析；Package Pattern Terminal part 新增 8 个 16x16 RGBA PNG 和 1 个 base part model。
2026-07-03 06:42 再次执行 .\gradlew.bat runData 成功，未写出新的 generated resources 内容。
2026-07-06 资源审计通过：143 个 PNG 通过必需文件、RGBA、尺寸、可见非占位像素和模型门禁；ME Package Assembler GUI atlas 纳入 256x256 必需 PNG。
```

## 4. 构建验证

必须运行：

```powershell
.\gradlew.bat build
```

检查：

```text
编译成功
测试成功
jar 生成在 build/libs
jar 文件名包含 mod id 和版本
```

当前最新执行：`.\gradlew.bat build` 成功，生成 `build/libs/appliedpackaging-0.1.0-dev.jar`。

2026-07-03 06:13 再次执行 `.\gradlew.bat build` 成功。
2026-07-03 06:25 再次执行 `.\gradlew.bat build` 成功。
2026-07-03 06:39 再次执行 `.\gradlew.bat build` 成功，重新生成 `build/libs/appliedpackaging-0.1.0-dev.jar`。
2026-07-04 在 `mods.toml` 显式声明 GuideME 发布依赖范围后执行 `.\gradlew.bat build --stacktrace` 成功，`generateModMetadata` 和 `jar` 均重新执行；发布 jar 的 `META-INF/mods.toml` 已包含 `guideme` `[20.1.7,20.2.0)` mandatory dependency。
2026-07-04 在 Package Pattern Terminal Split 输出收敛到 AE2 blank_pattern carrier 后执行 `.\gradlew.bat build --stacktrace` 成功，重新生成 `build/libs/appliedpackaging-0.1.0-dev.jar`。
本次发布 jar 重新打包后已确认包含 `META-INF/mods.toml`、`META-INF/MANIFEST.MF`、`LICENSE.md`、`README.md`、`CHANGELOG.md` 和 `assets/appliedpackaging/logo.png`。
发布 jar 审计通过：`jar tf` 未发现 `gametest`、`build/tmp`、reference、preview、`docs/assets`、`run/` 等 dev/test 条目。
发布 jar 文本资源审计通过：未发现 `E:\`、`C:\Users`、`build/reference`、`build/asset-reference`、`.codex` 或 `asset-reference` 等本机绝对路径和参考素材路径。

机械发布审计脚本：

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -RunServerSmoke
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireCleanGit
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-server-smoke.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\write-release-manifest.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-manifest.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-manifest.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\write-release-bundle.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-bundle.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-docs-audit.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-assets-audit.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-audit.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-check-plan.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-bundle.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireAssetContracts
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireServerWorldLoad
```

`scripts/run-release-checks.ps1` 编排 `build`、`runData`、`runGameTestServer`、可选 `run-server-smoke.ps1`、机械发布审计、资产资源审计、文档审计、可选 tag 就绪审计、可选发布清单生成/审计和可选发布附件包生成/审计。它支持 `-ReleaseCandidate`、`-AuditOnly`、`-PlanOnly`、`-RunServerSmoke`、`-ServerSmokeTimeoutSeconds`、`-RequireServerWorldLoad`、`-RequireCleanGit`、`-RequireReadyForTag`、`-WriteReleaseManifest`、`-RequireReleaseManifest`、`-WriteReleaseBundle`、`-RequireReleaseBundle`、`-SkipDocs`、`-SkipBuild`、`-SkipData`、`-SkipGameTest` 和 `-SkipAssetContracts`。使用 `-ReleaseCandidate` 时会禁止 `-AuditOnly` 与 skip flags，并自动启用 `-RunServerSmoke`、`-WriteReleaseManifest`、`-RequireReleaseManifest`、`-WriteReleaseBundle` 和 `-RequireReleaseBundle`，用于候选发布技术门禁；最终发布 tag 前推荐与 `-RequireCleanGit -RequireReadyForTag` 一起使用。`scripts/test-release-check-plan.ps1` 会通过 `-PlanOnly` 自测完整候选发布步骤顺序，并确认 `-ReleaseCandidate` 会拒绝所有 skip flags 和 `-AuditOnly`，`-AuditOnly` 会拒绝 `-RunServerSmoke`，普通执行模式下 `-RequireServerWorldLoad` 必须搭配 `-RunServerSmoke`。使用 `-RequireReadyForTag` 时会调用 `scripts/verify-release-readiness.ps1 -RequireReadyForTag`，确认变更接收表没有状态、迁移目标或验证要求仍为待输入、待判定、阻塞或失败的项，确认已填写的迁移目标是仓库内已存在文件的规范相对路径且不包含父级遍历，并确认需求类目标落在 `docs/01`、`02`、`03`、`05`、`06` 或 `07`，材质类目标落在 `docs/04-asset-spec.md`、`docs/assets/` 或 `src/main/resources/assets/appliedpackaging/`，本文件不再标记发布 tag 未完成，并且在没有负面 blocker 后必须存在明确正向发布信号。使用 `-RunServerSmoke` 时会在其他 Gradle run 后运行 dedicated server world-load smoke，刷新 `run/logs/latest.log`，并自动要求 `-RequireServerWorldLoad` 审计。使用 `-WriteReleaseManifest` 时会在机械发布审计、资产资源审计和文档审计之后写入 `build/release/appliedpackaging-<version>-release-manifest.json`。使用 `-RequireReleaseManifest` 时会调用 `scripts/verify-release-manifest.ps1`，确认发布清单匹配当前 jar、`gradle.properties` 和 git HEAD；修改发布清单生成或审计规则时同步运行 `scripts/test-release-manifest.ps1`。使用 `-WriteReleaseBundle` 时会生成 `build/release/appliedpackaging-<version>-release-bundle.zip`；使用 `-RequireReleaseBundle` 时会复验 zip 只包含 jar、manifest、README、CHANGELOG、LICENSE 和 SHA256SUMS，且哈希与当前源文件一致，并确认 bundle 内 manifest 的 mod id/version 与 jar SHA-256 一致；使用 `-RequireCleanGit` 时 bundle 审计还会确认 bundle 内 manifest 的 git commit、shortCommit、branch、clean 和 statusPorcelain 与当前干净工作区一致。修改机械发布审计规则时同步运行 `scripts/test-release-audit.ps1`。修改发布 PNG 资源、资产尺寸规则或必需资源清单时同步运行 `scripts/verify-assets.ps1` 和 `scripts/test-assets-audit.ps1`。`scripts/test-release-self-tests.ps1` 会聚合 docs audit、asset audit、release audit、release readiness、release plan、manifest 和 bundle 自测，适合在修改发布脚本或文档门禁后快速验证脚本负路径。`-RequireServerWorldLoad` 只检查 `run/logs/latest.log` 证据，只能与 `-AuditOnly` 组合使用，或与 `-RunServerSmoke` 同时使用。

`scripts/run-server-smoke.ps1` 检查 `run/eula.txt` 已明确 `eula=true`，启动 `.\gradlew.bat runServer --stacktrace`，等待 `run/logs/latest.log` 出现 Applied Packaging 初始化、`Preparing level "world"` 和 `Done (...)! For help, type "help"`，随后终止本脚本启动的 runServer 进程树，并确认 25565 不再监听。脚本产生的 stdout/stderr 记录写入 `build/server-smoke/`，不纳入发布资源。

`scripts/write-release-manifest.ps1` 读取 `gradle.properties`、release jar 和 git 状态，输出 release manifest JSON。清单包含 mod id/name/version、Minecraft/Forge/AE2/GuideME 版本与版本范围、jar 路径、jar 大小、SHA-256、jar mtime、git branch、git commit 和 clean 状态。使用 `-RequireCleanGit` 时，如果工作树不干净会失败。

`scripts/verify-release-manifest.ps1` 读取 release manifest、`gradle.properties`、release jar 和当前 git 状态，检查 schema、mod 元数据、Minecraft/Forge/AE2/GuideME 版本与版本范围、jar 路径、文件名、大小、mtime、SHA-256、git commit/shortCommit/branch/clean/statusPorcelain 和 manifest 路径。使用 `-RequireCleanGit` 时，如果当前 git 工作树不干净会失败。

`scripts/test-release-manifest.ps1` 使用临时输出路径调用 release manifest writer，确认有效 manifest fixture 可通过 `verify-release-manifest.ps1`，并确认篡改 manifest mod id 或 artifact SHA-256 会被 manifest audit 拒绝。脚本只写入系统临时目录，不运行 Gradle、客户端或服务端；工作区干净时还会额外运行 `write-release-manifest.ps1 -RequireCleanGit` 和 `verify-release-manifest.ps1 -RequireCleanGit` 覆盖 git 元数据校验路径。

`scripts/write-release-bundle.ps1` 读取 release jar、release manifest、README.md、CHANGELOG.md 和 LICENSE.md，输出 `build/release/appliedpackaging-<version>-release-bundle.zip`。zip 内以 `appliedpackaging-<version>/` 为根目录，包含 jar、manifest、README、CHANGELOG、LICENSE 和 SHA256SUMS。使用 `-RequireCleanGit` 时，如果工作树不干净会失败。

`scripts/verify-release-bundle.ps1` 读取 release bundle 和当前源文件，检查 zip 条目集合、每个条目的 SHA-256、SHA256SUMS 内容，以及 bundle 内 manifest 的 mod id/version、artifact fileName/sha256 是否匹配 bundle 内 jar。使用 `-RequireCleanGit` 时，如果当前 git 工作树不干净会失败，并且会进一步确认 bundle 内 manifest 的 git commit、shortCommit、branch、clean 和 statusPorcelain 与当前 checkout 一致。

`scripts/test-release-bundle.ps1` 使用临时输出路径调用 release manifest/bundle writer，确认有效 bundle fixture 可通过 `verify-release-bundle.ps1`，并确认篡改 bundle 内 manifest mod id 或 README.md 内容会被 bundle audit 拒绝。脚本只写入系统临时目录，不运行 Gradle、客户端或服务端；工作区干净时还会额外运行 `verify-release-bundle.ps1 -RequireCleanGit` 覆盖 git 元数据校验路径。

当前资产资源审计规则中的发布 PNG 变更包括必需资源清单、路径归类、RGBA header、可见非占位像素内容和尺寸规则；修改这些规则时同步运行 `scripts/verify-assets.ps1` 与 `scripts/test-assets-audit.ps1`。

`scripts/verify-docs.ps1` 检查必需的设计文档、变更接收文档、开发日志、资产 brief、资产 contract、资产报告和关键发布脚本是否存在，检查 `docs/design.md` 与 `docs/00-document-index.md` 是否覆盖文档集合，检查正式设计文档中不含 `TODO` / `FIXME` / `TBD` / `待定` / `待补充` / `等待 X` 类未清理占位，并扫描仓库 Markdown 中的本地 inline link 是否可解析。使用 `-RootPath` 时可对临时 fixture 执行同一套审计。

`scripts/test-docs-audit.ps1` 使用临时文档 fixture 调用 `verify-docs.ps1 -RootPath`，覆盖有效 fixture、缺少必需文档路径、正式文档未清理占位和本地 Markdown 断链四种路径。该脚本只写入系统临时目录，不修改正式设计文档。

`scripts/verify-assets.ps1` 检查发布资源 PNG：必需资源存在、PNG header 有效、RGBA color type、资源路径在已知 release asset 目录内，像素内容不是全透明或整张单一 RGBA 占位图，并确认 item/block 为 32x32、GUI icon 与 AE2 part 为 16x16、root/gui logo 为 128x128、ME Packager 与 ME Package Assembler GUI atlas 为 256x256。使用 `-RootPath` 时可对临时 fixture 执行同一套资产资源审计。

`scripts/test-assets-audit.ps1` 使用临时资源 fixture 调用 `verify-assets.ps1 -RootPath`，覆盖有效资产 fixture、item 贴图尺寸错误、PNG header 损坏、全透明 PNG、单一 RGBA 占位 PNG 和必需 PNG 缺失六种路径。该脚本只写入系统临时目录，不修改正式资源或发布产物。

`scripts/test-release-audit.ps1` 使用临时 release fixture 调用 `verify-release.ps1 -RootPath`，覆盖有效 release audit fixture、缺少 jar 必需 README 条目、jar 内 README 过期、jar 内语言文件过期、jar 内发布资源缺失或过期、`mods.toml` mod id 被篡改、jar 文本资源泄漏本机/reference 路径、语言占位符不一致、本地样板被 recipe 产出、创造栏暴露本地样板，以及包裹样板终端退回 BlockItem。该脚本不运行 Gradle、客户端或服务端，也不修改正式资源或发布产物。

`scripts/verify-release-readiness.ps1` 检查 `docs/08-change-intake.md` 的新增项暂存表，以及本文件的目标完成/发布 tag 判定。默认模式用于预冻结审计，发现 blocker 时输出 WARN 但退出 0；使用 `-RequireReadyForTag` 时，任何状态、迁移目标或验证要求仍为待输入、待判定、阻塞或失败的 intake、迁移目标无法解析到仓库内已存在文件、迁移目标包含父级遍历、需求/材质迁移目标与类型目标族不匹配、开放接收窗口、发布 tag 未完成或目标不能标记完成都会导致失败。当负面 blocker 全部清除后，脚本还要求 `docs/08-change-intake.md` 明确记录范围已冻结、最终服务端 world-load 已完成、发布 tag 可创建，并要求本文件明确记录可以标记完成、发布 tag 就绪门禁已通过；缺少这些正向信号也会阻止 tag。

`scripts/test-release-readiness.ps1` 使用临时 Markdown fixture 调用 `verify-release-readiness.ps1 -RequireReadyForTag`，覆盖 ready、pending/blocked、failed intake state、unresolved migration target、missing migration target path、traversal migration target path、misclassified requirement migration target、misclassified asset migration target、structural failure 和 missing positive signals 十种路径，确认 readiness 规则既能在范围冻结后放行，也能在待输入、阻塞/失败、迁移目标未明确、迁移目标文件不存在、迁移目标使用父级遍历、需求/材质迁移目标类别错位、结构缺失或只删除负面文字但缺少正向完成信号时失败。该脚本不修改正式设计文档。

`scripts/test-release-check-plan.ps1` 使用 `run-release-checks.ps1 -PlanOnly -ReleaseCandidate -RequireCleanGit -RequireReadyForTag` 检查完整候选发布门禁的步骤顺序，并检查 `-ReleaseCandidate` 拒绝所有 skip flags、`-ReleaseCandidate -AuditOnly` 失败、`-AuditOnly -RunServerSmoke` 失败、非 audit-only 且未运行 server smoke 时 `-RequireServerWorldLoad` 失败。该脚本还确认候选发布计划包含资产资源审计。该脚本不运行 Gradle、客户端或服务端，只验证发布编排自身。

`scripts/test-release-self-tests.ps1` 串行运行 `test-docs-audit.ps1`、`test-assets-audit.ps1`、`test-release-audit.ps1`、`test-release-readiness.ps1`、`test-release-check-plan.ps1`、`test-release-manifest.ps1` 和 `test-release-bundle.ps1`。它不运行 Gradle、客户端或服务端，只验证发布脚本自测套件本身；工作区干净时 manifest/bundle 子测试会额外覆盖 clean-git 路径。

`scripts/verify-release.ps1` 检查 `gradle.properties`、jar 文件名、jar manifest、`META-INF/mods.toml`、jar 必需条目、jar 内 README/CHANGELOG/LICENSE 与仓库源文件同步、jar 内语言文件与源码同步、jar 内 Applied Packaging `assets/` / `data/` 发布资源与 `src/main/resources` / `src/generated/resources` 源文件同步、dev/test/reference 条目、jar 文本本机路径泄漏、资源 JSON、玩家入口产品不变量、PNG 非空、asset contract、英文/简体中文语言 key 和占位符、Applied Packaging 模型贴图引用、可选 latest.log 服务端 world-load 关键证据，以及可选 git 工作树干净证据。它会确认 `mods.toml` 中的 Minecraft、Forge、AE2 和 GuideME dependency range 与 `gradle.properties` 一致，确认 `package_pattern` / `advanced_processing_pattern` 没有作为 recipe/creative-tab 玩家入口，确认旧 packaged/colored pattern 存储与机器存档兼容路径没有重新引入，确认已取消的 `package_pattern_terminal` / `package_export_bus` 不再注册或进入创造栏，并确认 `package_storage_bus` / `package_unpacking_bus` 与高级样板终端继续注册为 AE2 `PartItem`。asset contract 校验会自动寻找 PATH 中的 `assetgen` 或当前用户 Codex skill 中的 `minecraft-mod-asset-generation/scripts/assetgen`；使用 `-RequireAssetContracts` 时找不到或校验失败都会让脚本失败。使用 `-RootPath` 时可对临时 fixture 执行同一套机械发布审计。使用 `-RequireCleanGit` 时会执行 `git status --porcelain=v1 --untracked-files=all` 并要求无输出，适合全部变更提交后、发布 tag 创建前运行。日志诊断会把 Mojang/Yggdrasil 外部公钥获取失败，以及 Create/AE2/GTCEu 对 PonderWorld、Xaero GuiMap、ModernFixClientIntegration 三个已核实的第三方可选兼容类缺失警告作为 WARN 忽略；其它 `ClassNotFoundException`、Applied Packaging 错误、客户端类加载、崩溃、missing texture 等关键字仍会失败。它不替代 `build`、`runData`、`runGameTestServer`、人工 `runClient` 或 `runServer`。

当前玩家入口产品不变量还要求 `advanced_pattern_encoding_terminal` 出现在创造栏，并继续注册为 AE2 `PartItem`；高级终端 base/sprite atlas 纳入必需 PNG 与 256x256 尺寸审计，独立 `advanced_processing_pattern` 不得作为配方或创造栏直接产物。

2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1` 成功。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-audit.ps1` 成功，确认新增的 jar 内发布资源缺失和 jar 内发布资源过期 fixture 均按预期失败。
2026-07-04 执行 `.\gradlew.bat build --stacktrace` 成功，刷新包含 README/CHANGELOG/LICENSE 和发布资源的 release jar。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireAssetContracts` 成功，确认 115 个 Applied Packaging `assets/` / `data/` 发布资源与 jar 条目 SHA-256 一致。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1` 成功，确认新增 release resource sync 负例已纳入聚合 release audit 自测；开发中工作区 dirty，manifest/bundle clean-git fixture 按预期跳过。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，确认机械发布审计、资产资源审计、文档审计、发布清单生成/审计和发布附件包生成/审计串联通过；开发中 manifest 记录 `clean=false` 属于预期状态。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag` 按预期失败，阻止 IN-001/IN-002 待输入和未冻结状态下创建发布 tag。
2026-07-04 补齐 `docs/03-detailed-design.md` 中普通 processing pattern 语义，明确 AE2 可见输出仍是原 processing pattern 的 X，装配室输出包裹只是中间物流单元，不伪装为 X 或散装库存。
2026-07-04 增强 `scripts/verify-docs.ps1`，新增正式设计文档未清理占位审计；`scripts/test-docs-audit.ps1` 新增 unresolved placeholder fixture。
2026-07-04 执行 PowerShell parser 检查 `verify-docs.ps1` 和 `test-docs-audit.ps1` 成功；执行 `verify-docs.ps1` 成功，确认正式设计文档没有 unresolved placeholder；执行 `test-docs-audit.ps1` 成功，确认 unresolved placeholder fixture 按预期失败。
2026-07-04 执行 `.\gradlew.bat build --stacktrace` 成功，刷新包含 README/CHANGELOG/LICENSE 和文档审计说明的 release jar。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1` 成功，确认新增 docs unresolved placeholder 负例已纳入聚合 docs audit 自测；开发中工作区 dirty，manifest/bundle clean-git fixture 按预期跳过。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，确认机械发布审计、资产资源审计、文档审计、发布清单生成/审计和发布附件包生成/审计串联通过；开发中 manifest 记录 `clean=false` 属于预期状态。
2026-07-04 提交后执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireCleanGit -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，确认 clean-git 下 manifest/bundle 可按当前 HEAD 生成并复验。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag` 仍按预期失败，阻止 IN-001/IN-002 待输入和未冻结状态下创建发布 tag。
2026-07-04 增强 `scripts/verify-assets.ps1`，解码 RGBA PNG 像素并拒绝全透明或整张单一 RGBA 像素的占位图；`scripts/test-assets-audit.ps1` 新增 transparent PNG 和 single-color PNG 负例。
2026-07-04 执行 PowerShell parser 检查 `verify-assets.ps1` 和 `test-assets-audit.ps1` 成功；执行 `scripts/verify-assets.ps1` 成功，确认 60 个发布 PNG 含可见非占位像素内容；执行 `scripts/test-assets-audit.ps1` 成功，确认 transparent PNG 和 single-color PNG fixture 均按预期失败。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-self-tests.ps1` 成功，确认新增 asset audit 透明/单色负例已纳入聚合自测；开发中工作区 dirty，manifest/bundle clean-git fixture 按预期跳过。
2026-07-04 执行 `.\gradlew.bat build --stacktrace` 成功，刷新包含 README/CHANGELOG 的 release jar。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，确认机械发布审计、资产像素内容审计、文档审计、发布清单生成/审计和发布附件包生成/审计串联通过；开发中 manifest 记录 `clean=false` 属于预期状态。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireAssetContracts` 成功，确认 5 个 asset contract 通过 `assetgen validate-contract`。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireServerWorldLoad` 成功，确认当前 latest.log 包含 Applied Packaging 初始化、world 准备和 dedicated server world-load。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 -RequireAssetContracts -RequireServerWorldLoad` 成功，确认 jar 文件名、mods.toml、manifest 与 `gradle.properties` 的 mod id、版本、名称、作者、license、loader/Forge/Minecraft/AE2 版本范围一致。
2026-07-04 新增 `scripts/run-release-checks.ps1`，用于最终范围冻结后的发布检查编排。
2026-07-04 执行旧组合 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -RunClientSmoke -RequireServerWorldLoad` 时发现 `runClientSmoke` 会覆盖 `run/logs/latest.log`，因此将 `-RequireServerWorldLoad` 收敛为 `-AuditOnly` 专用模式。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -RunClientSmoke` 成功，完成 `build`、`runData`、`runGameTestServer`、`runClientSmoke` 和 `verify-release.ps1 -RequireAssetContracts`。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireAssetContracts -RequireServerWorldLoad` 成功；本次 latest.log 包含 dedicated server world-load，并有 1 条外部 Yggdrasil public-key fetch WARN 被忽略。
2026-07-04 新增 `-RequireClientSmokeScreenshots` 审计项；执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireAssetContracts -RequireClientSmokeScreenshots -RequireServerWorldLoad` 成功，确认 6 张 client smoke 截图存在且为有效 PNG。
2026-07-04 新增 `-RequireCleanGit` 审计项；该项用于最终范围冻结、全部变更提交后的发布 tag 前门禁。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -AuditOnly -RequireCleanGit` 成功，确认 release runner 会把 `-RequireCleanGit` 传递给 `verify-release.ps1`。
2026-07-04 在提交 `10b59b2 build: add clean git release audit` 后执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -RequireCleanGit` 成功，确认当前提交基线工作树干净；最终发布前仍需在新增需求和材质全部提交后重跑。
2026-07-04 新增 `scripts/run-server-smoke.ps1`、`run-release-checks.ps1 -RunServerSmoke` 和 `-ServerSmokeTimeoutSeconds`，用于自动刷新 dedicated server world-load 日志并进入 `-RequireServerWorldLoad` 审计。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -RunClientSmoke -RunServerSmoke` 成功，确认执行顺序为 build、runData、runGameTestServer、runClientSmoke、run-server-smoke、mechanical release audit。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke` 成功，确认自动 server smoke 进入 `Done (2.413s)!`，停止本次 runServer 进程树，25565 未保持监听，并通过 `verify-release.ps1 -RequireAssetContracts -RequireServerWorldLoad`。
2026-07-04 新增 `scripts/write-release-manifest.ps1` 和 `run-release-checks.ps1 -WriteReleaseManifest`，用于生成 `build/release/appliedpackaging-0.1.0-dev-release-manifest.json`。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -AuditOnly -WriteReleaseManifest` 成功，确认 release runner 会在机械审计后生成发布清单。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\write-release-manifest.ps1` 成功，并核对清单中的 jar SHA-256、git commit、branch 和 short commit 字段与当前仓库一致。
2026-07-04 新增 `scripts/verify-docs.ps1`，并默认接入 `scripts/run-release-checks.ps1`。执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1` 成功，确认必需文档存在、`design.md` 与 `00-document-index.md` 覆盖文档集合、20 个本地 Markdown 链接可解析。
2026-07-04 新增 `scripts/verify-release-manifest.ps1` 和 `run-release-checks.ps1 -RequireReleaseManifest`，用于复验发布清单是否匹配当前 jar、`gradle.properties` 和 git HEAD。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest` 成功，确认机械发布审计、文档审计、发布清单生成和发布清单复验可串联通过。
2026-07-04 新增 `guideme_version_range=[20.1.7,20.2.0)`、`mods.toml` 的 `guideme` mandatory dependency、release manifest 的 `guideMeVersionRange` 字段，以及 `verify-release.ps1` / `verify-release-manifest.ps1` 对 GuideME 范围的审计。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest` 成功，确认 `mods.toml GuideME dependency range matches gradle.properties`，并确认发布清单中的 `dependencies.guideMeVersionRange` 匹配 `gradle.properties`。
2026-07-04 新增 `scripts/write-release-bundle.ps1`、`scripts/verify-release-bundle.ps1`、`run-release-checks.ps1 -WriteReleaseBundle` 和 `-RequireReleaseBundle`，用于生成并复验发布附件包。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，确认 bundle 包含 jar、release manifest、README、CHANGELOG、LICENSE 和 SHA256SUMS，且 bundle 内 manifest 的 artifact sha256 匹配 bundle 内 jar。
2026-07-04 新增 `run-release-checks.ps1 -ReleaseCandidate`，作为最终候选发布预设；它会展开为 build、runData、runGameTestServer、runClientSmoke、run-server-smoke、机械发布审计、文档审计、发布清单生成/审计和发布附件包生成/审计。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -ReleaseCandidate -RequireCleanGit` 成功，确认候选发布预设会传递 clean-git 门禁并按完整顺序展开。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -ReleaseCandidate -SkipGameTest` 和 `... -ReleaseCandidate -AuditOnly` 均按预期失败，确认候选发布预设不能跳过 GameTest 或降级为 audit-only。
2026-07-04 在 README/CHANGELOG 更新后执行 `.\gradlew.bat build --stacktrace` 成功，并执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，确认候选发布预设相关文档变更不破坏现有 manifest/bundle 链路。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit` 成功，完成 build、runData、112 个 GameTest、6 张 client smoke 截图、dedicated server world-load、机械发布审计、文档审计、发布清单生成/审计和发布附件包生成/审计；server smoke 刷新的 `run/logs/latest.log` 出现 `Done (2.471s)!`，release manifest 记录当时提交基线且 `clean=true`。
2026-07-04 新增 `scripts/verify-release-readiness.ps1` 和 `run-release-checks.ps1 -RequireReadyForTag`，用于阻止发布 tag 在变更接收表仍有待输入/待判定项时创建。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1` 成功，报告当前 4 个非致命 blocker：IN-001 待输入、IN-002 待输入、变更接收窗口仍开放、验证文档仍标记发布未完成。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release-readiness.ps1 -RequireReadyForTag` 按预期失败，确认 tag 就绪门禁会阻止当前未冻结范围发布。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -PlanOnly -ReleaseCandidate -RequireCleanGit -RequireReadyForTag` 成功，确认完整候选发布计划会在文档审计后、manifest/bundle 生成前执行 Release readiness audit。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-readiness.ps1` 成功，确认 ready fixture 退出 0，blocked fixture、structural failure fixture 和 missing positive signals fixture 退出 1。
2026-07-04 执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-release-check-plan.ps1` 成功，确认完整候选发布步骤顺序、clean-git/tag-readiness 参数传递、`-ReleaseCandidate` 拒绝所有 skip flags 和 `-AuditOnly`，并确认 server world-load 审计参数保护。
2026-07-04 增强 `scripts/verify-release-readiness.ps1`：当负面 blocker 清除后，`-RequireReadyForTag` 仍要求变更接收文档和本文件明确记录范围冻结、最终服务端 world-load 完成、发布 tag 可创建、目标可完成和 tag 就绪门禁通过，避免只删除阻塞文字就误放行。
2026-07-04 执行 PowerShell parser 检查 `verify-release-readiness.ps1`、`test-release-readiness.ps1` 和 `run-release-checks.ps1` 成功；执行 `test-release-readiness.ps1`、`verify-release-readiness.ps1`、`verify-release-readiness.ps1 -RequireReadyForTag`、`verify-docs.ps1`、`test-release-check-plan.ps1`、`.\gradlew.bat build --stacktrace` 和 `run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 均符合预期，其中严格 readiness 仍因 IN-001/IN-002 与未冻结发布状态失败。
2026-07-04 再次增强 `scripts/verify-release-readiness.ps1`：intake 行状态或验证要求包含阻塞、失败、未通过、不可、不能、blocked、failed 等负面状态时也会阻止 `-RequireReadyForTag`，避免失败项被误判为已冻结。
2026-07-04 执行 PowerShell parser 检查 `verify-release-readiness.ps1` 和 `test-release-readiness.ps1` 成功；执行 `test-release-readiness.ps1` 成功，确认 ready fixture 退出 0，blocked fixture、structural failure fixture、missing positive signals fixture 和 blocked intake state fixture 均退出 1；执行 `verify-release-readiness.ps1` 成功，正式文档仍报告 4 个非致命 blocker；执行 `verify-release-readiness.ps1 -RequireReadyForTag` 按预期失败；执行 `verify-docs.ps1` 和 `test-release-self-tests.ps1` 成功。
2026-07-04 执行 `.\gradlew.bat build --stacktrace` 成功，刷新 jar 内 README/CHANGELOG；执行 `run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，确认机械发布审计、资产审计、文档审计、manifest 和 bundle 链路在 readiness 规则补强后仍通过；开发中 manifest 记录 `clean=false` 属于预期状态。
2026-07-04 继续增强 `scripts/verify-release-readiness.ps1`：intake 行的 `迁移目标` 列也纳入负面状态检查，避免新增需求或材质没有迁移到正式分类文档却通过最终 tag 门禁。
2026-07-04 执行 PowerShell parser 检查 `verify-release-readiness.ps1` 和 `test-release-readiness.ps1` 成功；执行 `test-release-readiness.ps1` 成功，确认 ready fixture 退出 0，blocked fixture、structural failure fixture、missing positive signals fixture、blocked intake state fixture 和 unresolved migration target fixture 均按预期退出；执行 `verify-release-readiness.ps1` 成功，正式文档仍报告 4 个非致命 blocker，且 IN-001/IN-002 输出中包含 `migrationTarget='待判定'`。
2026-07-04 执行 `verify-docs.ps1`、`test-release-self-tests.ps1`、`.\gradlew.bat build --stacktrace` 和 `run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，确认迁移目标门禁补强后 release self-test、jar 文档同步、release manifest 和 release bundle 链路仍通过；开发中 manifest 记录 `clean=false` 属于预期状态。
2026-07-04 增强 `scripts/verify-release-bundle.ps1`：bundle audit 现在会检查 bundle 内 manifest 的 mod id/version 和 artifact sha256，并在 `-RequireCleanGit` 下检查 bundle 内 manifest 的 git commit、shortCommit、branch、clean 和 statusPorcelain 是否匹配当前 checkout。
2026-07-04 执行 PowerShell parser 检查 `verify-release-bundle.ps1`、`write-release-bundle.ps1` 和 `run-release-checks.ps1` 成功；直接执行 `verify-release-bundle.ps1` 曾按预期发现旧 bundle 内 README/CHANGELOG 哈希已因本轮文档更新过期；随后执行 `.\gradlew.bat build --stacktrace` 和 `run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，重新生成并复验发布 manifest/bundle。
2026-07-04 新增 `scripts/test-release-bundle.ps1`，覆盖临时 manifest/bundle 生成、有效 bundle audit、bundle 内 manifest mod id 篡改失败、bundle 内 README 内容篡改失败，以及干净工作区下的 `verify-release-bundle.ps1 -RequireCleanGit` 路径。
2026-07-04 执行 PowerShell parser 检查 `write-release-manifest.ps1`、`write-release-bundle.ps1`、`verify-release-bundle.ps1`、`test-release-bundle.ps1` 和 `verify-docs.ps1` 成功；执行 `test-release-bundle.ps1`、`verify-docs.ps1`、`.\gradlew.bat build --stacktrace` 和 `run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，其中开发中工作区 dirty，`test-release-bundle.ps1` 的 clean-git fixture 按预期跳过，提交后需重跑覆盖。
2026-07-04 提交后执行 `scripts/test-release-bundle.ps1` 成功，clean-git bundle fixture 退出 0；执行 `run-release-checks.ps1 -AuditOnly -RequireCleanGit -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，release manifest 记录当前提交且 `clean=true`。
2026-07-04 新增 `scripts/test-release-manifest.ps1`，覆盖临时 manifest 生成、有效 manifest audit、manifest mod id 篡改失败、artifact SHA-256 篡改失败，以及干净工作区下的 `write-release-manifest.ps1 -RequireCleanGit` / `verify-release-manifest.ps1 -RequireCleanGit` 路径。
2026-07-04 执行 PowerShell parser 检查 `write-release-manifest.ps1`、`verify-release-manifest.ps1`、`test-release-manifest.ps1` 和 `verify-docs.ps1` 成功；执行 `test-release-manifest.ps1`、`verify-docs.ps1`、`.\gradlew.bat build --stacktrace` 和 `run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，其中开发中工作区 dirty，`test-release-manifest.ps1` 的 clean-git fixture 按预期跳过，提交后需重跑覆盖。
2026-07-04 新增 `scripts/test-release-self-tests.ps1`，聚合运行 release readiness、release check plan、release audit、documentation audit、release manifest 和 release bundle 自测。
2026-07-04 执行 PowerShell parser 检查 `test-release-self-tests.ps1`、`test-release-readiness.ps1`、`test-release-check-plan.ps1`、`test-release-manifest.ps1`、`test-release-bundle.ps1` 和 `verify-docs.ps1` 成功；执行 `test-release-self-tests.ps1`、`verify-docs.ps1`、`.\gradlew.bat build --stacktrace` 和 `run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，其中开发中工作区 dirty，manifest/bundle 子测试的 clean-git fixture 按预期跳过，提交后需重跑覆盖。
2026-07-04 增强 `scripts/verify-docs.ps1`：新增 `-RootPath` 参数用于临时 fixture 审计；新增 `scripts/test-docs-audit.ps1` 覆盖有效文档 fixture、缺少必需路径和本地 Markdown 断链。
2026-07-04 执行 PowerShell parser 检查 `verify-docs.ps1`、`test-docs-audit.ps1` 和 `test-release-self-tests.ps1` 成功；执行 `test-docs-audit.ps1` 成功，确认 valid fixture 退出 0，missing required path fixture 和 broken markdown link fixture 退出 1。
2026-07-04 执行 `scripts/test-release-self-tests.ps1` 成功，确认 docs audit、release audit、readiness、release plan、manifest 和 bundle 自测可从聚合入口串行通过；执行 `verify-docs.ps1`、`.\gradlew.bat build --stacktrace` 和 `run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，其中开发中工作区 dirty，manifest/bundle 子测试的 clean-git fixture 按预期跳过，提交后需重跑覆盖。
2026-07-04 新增 `scripts/test-release-audit.ps1`，覆盖有效 release audit fixture、jar 必需 README 条目缺失、`mods.toml` mod id 被篡改，以及 jar 文本资源泄漏本机/reference 路径四种路径；`scripts/verify-release.ps1` 同步新增 `-RootPath` 用于临时 fixture 审计。
2026-07-04 执行 PowerShell parser 检查 `verify-release.ps1`、`test-release-audit.ps1`、`verify-docs.ps1`、`test-docs-audit.ps1` 和 `test-release-self-tests.ps1` 成功；执行 `test-release-audit.ps1` 成功，确认 valid fixture 退出 0，missing README、tampered metadata 和 local path leak fixture 均退出 1。
2026-07-04 `verify-release.ps1` 新增玩家入口产品不变量审计，确认本地 `package_pattern` / `packaged_processing_pattern` 不作为 recipe/creative-tab 输出，且包裹样板终端仍注册为 AE2 `PartItem`；`test-release-audit.ps1` 同步新增本地样板 recipe 输出、创造栏本地样板和终端 BlockItem 回退三个负例。
2026-07-04 执行 `scripts/test-release-self-tests.ps1` 成功，确认 docs audit、release audit、readiness、release plan、manifest 和 bundle 自测可从聚合入口串行通过；执行 `verify-docs.ps1`、`.\gradlew.bat build --stacktrace` 和 `run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，其中开发中工作区 dirty，manifest/bundle 子测试的 clean-git fixture 按预期跳过，提交后需重跑覆盖。
2026-07-04 `verify-release.ps1` 新增英文/简体中文语言占位符一致性审计；`test-release-audit.ps1` 同步新增语言占位符不一致负例。
2026-07-04 `verify-release.ps1` 新增 jar 内 README/CHANGELOG/LICENSE 与仓库源文件同步审计，以及 jar 内英文/简体中文语言文件与源码同步审计；`test-release-audit.ps1` 同步新增 jar 内 README 过期和 jar 内语言文件过期负例。
2026-07-04 新增 `scripts/verify-assets.ps1`，检查发布资源 PNG 的必需文件、已知路径、PNG header、RGBA color type 和尺寸规则；新增 `scripts/test-assets-audit.ps1`，覆盖有效资产 fixture、错尺寸、坏 PNG header 和缺必需 PNG。
2026-07-04 `scripts/run-release-checks.ps1` 新增 Asset resource audit 步骤，位于机械发布审计之后、文档审计之前；`scripts/test-release-check-plan.ps1` 同步确认候选发布计划包含该步骤。
2026-07-04 执行 `scripts/verify-assets.ps1` 成功，确认 60 个发布 PNG 的 header、RGBA 类型、路径归类和尺寸符合规格；执行 `scripts/test-assets-audit.ps1` 成功，确认 valid fixture 退出 0，bad dimension、bad PNG header 和 missing required PNG fixture 均退出 1。
2026-07-04 执行 PowerShell parser 检查 `verify-assets.ps1`、`test-assets-audit.ps1`、`run-release-checks.ps1`、`test-release-check-plan.ps1`、`verify-docs.ps1`、`test-docs-audit.ps1` 和 `test-release-self-tests.ps1` 成功；执行 `test-release-check-plan.ps1`、`test-release-self-tests.ps1`、`verify-docs.ps1`、`.\gradlew.bat build --stacktrace` 和 `run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，其中开发中工作区 dirty，manifest/bundle 子测试的 clean-git fixture 按预期跳过，提交后需重跑覆盖。
2026-07-04 增强 `scripts/verify-release-readiness.ps1`：已迁移的 intake 行现在必须在 `迁移目标` 列提供可解析到仓库内已存在文件的路径；新增 `scripts/test-release-readiness.ps1` 的 missing migration target path fixture，确认 `docs/99-missing.md` 这类不存在目标会阻止 `-RequireReadyForTag`。
2026-07-04 执行 PowerShell parser 检查 `verify-release-readiness.ps1` 和 `test-release-readiness.ps1` 成功；执行 `test-release-readiness.ps1` 成功，确认 ready fixture 退出 0，blocked、structural failure、missing positive signals、blocked intake state、unresolved migration target 和 missing migration target path fixture 均退出 1；执行 `verify-release-readiness.ps1` 成功，正式文档仍报告 IN-001/IN-002 待输入等 4 个非致命 blocker；执行 `verify-release-readiness.ps1 -RequireReadyForTag` 按预期失败，继续阻止当前未冻结范围发布 tag。
2026-07-04 执行 `verify-docs.ps1`、`test-release-self-tests.ps1`、`git diff --check`、`.\gradlew.bat build --stacktrace` 和 `run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，确认迁移目标路径门禁补强后，文档审计、自测聚合、jar 打包、release manifest 和 release bundle 链路仍通过；开发中 manifest 记录 `clean=false` 属于预期状态。
2026-07-04 继续增强 `scripts/verify-release-readiness.ps1`：迁移目标路径现在会拒绝 `..` 父级遍历段，并通过 `GetFullPath` 确认解析后的绝对路径仍位于仓库根目录下；需求/材质类 intake 还会校验迁移目标是否落在对应文档或资源目标族。
2026-07-04 执行 PowerShell parser 检查 `verify-release-readiness.ps1` 和 `test-release-readiness.ps1` 成功；执行 `test-release-readiness.ps1` 成功，确认 ready fixture 退出 0，blocked、structural failure、missing positive signals、blocked intake state、unresolved migration target、missing migration target path 和 traversal migration target path fixture 均退出 1；执行 `verify-release-readiness.ps1` 成功，正式文档仍报告 IN-001/IN-002 待输入等 4 个非致命 blocker。
2026-07-04 执行 `verify-docs.ps1`、`test-release-self-tests.ps1`、`git diff --check`、`.\gradlew.bat build --stacktrace` 和 `run-release-checks.ps1 -AuditOnly -WriteReleaseManifest -RequireReleaseManifest -WriteReleaseBundle -RequireReleaseBundle` 成功，确认迁移目标路径边界补强后，文档审计、自测聚合、jar 打包、release manifest 和 release bundle 链路仍通过；开发中 manifest 记录 `clean=false` 属于预期状态。

## 5. 客户端验证

必须验证：

```text
Minecraft 1.20.1 Forge + AE2 15.4.10 可进入游戏
创造栏条目正确
17 色包裹图标可区分
Tooltip 每包/总计正确
机器 GUI 无错位
方块模型无 missing texture
JEI、Create 6.0.8 与 GTCEu 7.5.3 同时加载且无 Applied Packaging optional-integration classloading 错误
打开 Advanced Pattern Encoding Terminal 时，当前高级页可导入常规确定性 JEI 配方；当前包裹页可把同一配方的输入展平为一个包裹计划
Create Sequenced Assembly 保持工序列；Mechanical Crafting 按非空行/列中较少者分包，数量相同时按行，选中行/列首个材料前及材料之间的空位不得压缩
GTCEu 上游 7.5.3 与 StarT Fork 1.7.0b 的确定性 item/fluid recipe 均可导入
随机、区间、动态世界产出、歧义、不可表示或越界 recipe 显示本地化拒绝 tooltip，不改变终端内容
导入后当前页的列/输入顺序、数量、输出/marker 和颜色正确；另一页状态保持不变
```

推荐命令：

```powershell
.\gradlew.bat runClient
```

2026-07-17 执行真实 `runClient`，JEI 15.20.0.134、Create 6.0.8、GTCEu 7.5.3 与 Applied Packaging 同时完成 Forge 初始化、资源重载、OpenAL 和图集创建并到达主菜单；日志未记录 Applied Packaging classloading、missing model/texture、ERROR 或 FATAL。确认主菜单稳定后主动结束本次开发客户端，因此 Gradle 进程的 `-1` 是人工终止结果，不表示启动失败。JEI transfer handler 的注册回调和“+”按钮只在进入世界并同步 recipe 后可交互，本轮没有自动 UI 驱动，按钮导入、拒绝 tooltip 及导入后页面/列状态继续作为人工验收项，不能写成已通过。

已删除的自动客户端 smoke 历史记录（仅保留既往验收证据，不是当前命令）：

```text
2026-07-03 再次执行 .\gradlew.bat runClient，已启动到 Minecraft 客户端主流程。
日志确认 Applied Packaging 初始化完成、ResourceManager 重载完成、OpenAL/SoundEngine 启动、block atlas 创建完成。
Package Bus screen/menu 注册后、Package Bus 手工过滤 UI 布局调整后、Package Pattern Terminal 处理输出 ghost slots 布局调整后、Package Pattern Terminal 薄面板模型调整后、AE2 blank_pattern 已编码 tooltip hook 接入后、AE2 encoded packaged-processing carrier 接入后，以及样板配方入口收敛到 AE2 blank_pattern 后的客户端启动 smoke 通过。
run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture、preview_sheet 或 mip level。
机器 preview sheet 已移至 docs/assets/previews，不再作为 block atlas 资源加载；最新 smoke 未再出现 preview_sheet/mip level 警告。
本次 smoke 在 atlas 创建完成后手动终止；Gradle 退出码来自人工终止，不代表客户端启动失败。
已观察到的剩余警告为 Forge/AE2/Vanilla 常规开发环境警告。

2026-07-03 新增并执行 .\gradlew.bat runClientSmoke 成功。
runClientSmoke 使用 --quickPlaySingleplayer 进入本地单人世界，自动摆放 6 个关键方块，依次通过真实服务器玩家和 NetworkHooks.openScreen 打开菜单，截图后退出客户端。
已生成并人工检查以下截图，均为真实 Minecraft 客户端菜单画面，不是原型图或静态 mock：
  run/screenshots/appliedpackaging-client-smoke-package_assembler.png
  run/screenshots/appliedpackaging-client-smoke-me_packager.png
  run/screenshots/appliedpackaging-client-smoke-package_pattern_terminal.png
  run/screenshots/appliedpackaging-client-smoke-package_storage_bus.png
  run/screenshots/appliedpackaging-client-smoke-package_export_bus.png
  run/screenshots/appliedpackaging-client-smoke-package_unpacking_bus.png
run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture 或客户端 smoke timeout。
默认 quick-play 世界名为 New World；可用 -Pappliedpackaging.clientSmoke.world="世界名" 覆盖。

2026-07-03 05:35 在 Package Bus 流体 required content ghost 完成后再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 仍自动摆放 6 个关键方块，打开 Package Assembler、ME Packager、Package Pattern Terminal、Package Storage Bus、Package Export Bus、Package Unpacking Bus 真实菜单并截图后退出。
6 张截图均已生成；人工抽看 Package Pattern Terminal 与 Package Storage Bus，确认菜单非空屏、核心控件和槽位显示正常。
run/logs/latest.log 未发现 ERROR、FATAL、Exception、Missing model、Unable to load model、missing texture、Timed out 或 timeout。

2026-07-03 05:44 在流体 ghost 数量滚轮调整 UI 完成后再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 生成 6 张真实菜单截图并正常退出客户端。
run/logs/latest.log 未发现 ERROR、FATAL、Exception、Missing model、Unable to load model、missing texture、Timed out 或 timeout。

2026-07-03 06:04 在 Package Pattern Terminal 改为 AE2 cable part item 后再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 的 Package Pattern Terminal 步骤通过 PartHelper.setPart 放置真实 AE2 part，再用 part host buffer 打开 PackagePatternTerminalScreen。
6 张截图均重新生成并正常退出客户端；run/logs/latest.log 未发现 ERROR、FATAL、Exception、Missing model、Unable to load model、missing texture、Timed out 或 timeout。

2026-07-03 06:08 再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 仍通过 PartHelper.setPart 放置 Package Pattern Terminal AE2 part，并生成 6 张真实菜单截图；人工抽看 Package Pattern Terminal 截图，确认菜单非空屏、核心控件和槽位显示正常。

2026-07-03 06:18 再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 自动摆放 6 个目标，Package Pattern Terminal 步骤继续使用 AE2 part host，6 张真实菜单截图均重新生成并正常退出客户端。

2026-07-03 06:27 在 Package Pattern Terminal part 切换为 Applied Packaging 自有 part body/front/back/sides/overlay mask 材质后，再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 继续通过真实 AE2 part host 打开 Package Pattern Terminal 菜单，生成 6 张真实菜单截图并正常退出客户端；人工抽看 Package Pattern Terminal 截图，确认菜单非空屏、核心控件和槽位显示正常。
run/logs/latest.log 未发现 ERROR、FATAL、Exception、Missing model、Unable to load model、missing texture、Timed out 或 timeout。

2026-07-03 06:41 在 ClientSmokeRunner 改为按 `appliedpackaging.clientSmoke.enabled=true` 反射加载且从 release jar 排除后，再次执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 仍生成 Package Assembler、ME Packager、Package Pattern Terminal、Package Storage Bus、Package Export Bus、Package Unpacking Bus 共 6 张真实菜单截图并正常退出客户端。
run/logs/latest.log 未发现 ERROR、FATAL、Missing model、Unable to load model、missing texture、NoClassDefFoundError、ClassNotFoundException、InvocationTargetException、IllegalStateException、Timed out 或 timeout。

2026-07-06 在 ME Packager GUI 工作进度条和菜单 shift-click 输入修正后，再次执行 .\gradlew.bat runClientSmoke --stacktrace 成功。
本次 smoke 先发现 ME Packager 连接面世界截图用的 AE2 cable 覆盖了 Package Assembler smoke 目标，已将该开发截图连接面移动到不覆盖 6 个菜单目标的位置；复跑后生成 world-me_packager、world-me_packager_link、world-all_machines 和 6 张真实菜单截图并正常退出客户端。

2026-07-06 在 ME Package Assembler GUI 改为 256x256 atlas、4x4 输入格 + 4 输出格同步滚动窗口和真实 menu input buffer 后，执行 .\gradlew.bat runClientSmoke 成功。
本次 smoke 生成 world-me_packager、world-me_packager_link、world-all_machines 和 6 张真实菜单截图并正常退出客户端；人工抽看 Package Assembler 截图，确认新背景、滚动条、样板槽、容量槽、自动导出开关和左右输入/输出区域正常显示。

2026-07-06 在 ME Package Assembler GUI 从独立 `AbstractContainerScreen` 修正为 AE2 `UpgradeableScreen` + `ScreenStyle`，并由客户端按 AE2 slot background 绘制滚动输入/输出槽背景后，执行 .\gradlew.bat compileJava 成功。
随后执行 .\gradlew.bat runClientSmoke 成功；人工查看 `run/screenshots/appliedpackaging-client-smoke-package_assembler.png`，确认 4x4 输入格、4 个输出格、滚动条、样板槽、容量槽和左侧 toolbar auto-export 开关可见。
run/logs/latest.log 按 `ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON` 扫描无命中。

2026-07-06 在 auto-export 从主面板普通 widget 改为 AE2 左侧 toolbar `IconButton`，并将滚动槽渲染改为读取菜单实际 slot index 后，再次执行 .\gradlew.bat runClientSmoke 成功。
人工查看 `run/screenshots/appliedpackaging-client-smoke-package_assembler.png`，确认主面板内无额外按钮，auto-export 位于左侧 AE2 toolbar，右侧没有自造升级控件；run/logs/latest.log 按 `ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON` 扫描无命中。

2026-07-06 纠正 ME Package Assembler GUI 处理方式：恢复用户提供的 `mepackageassembler.png` 原始 256x256 atlas，ScreenStyle 使用主界面 `srcRect` 176x239，并按原图像素对齐名称输入、颜色 swatch、marker 槽、容量元件槽、4x4 输入格、4 输出格、玩家物品栏和 hotbar；配置按钮保持 AE2 左侧 toolbar，右侧升级使用 AE2 `UpgradesPanel`。
本次同时补齐装配室 packageName、selectedColor、marker 槽、真实 upgrade inventory 与 `Upgrades.add` 注册；执行 .\gradlew.bat compileJava 成功，执行 .\gradlew.bat runGameTestServer 成功，134 个必需 GameTest 全部通过；执行 .\gradlew.bat runClientSmoke 成功，人工查看 `run/screenshots/appliedpackaging-client-smoke-package_assembler.png`，确认 GUI 使用原图布局、滚动条位于输入栏左侧、左侧仅显示 AE toolbar 配置按钮、右侧显示 AE2 升级面板。

2026-07-08 在 ME Package Assembler 改为只接受已编码样板、本地输入严格按样板槽位解锁、Pattern Provider pushPattern 临时 plan、只允许 5 张 speed card、输出模式三态和外部 handler 按序抽取后，执行 .\gradlew.bat compileJava 成功，执行 .\gradlew.bat runGameTestServer 成功，当时的必需 GameTest 全部通过；后续同日能量进度修正已扩展为 138 个必需 GameTest。
随后执行 .\gradlew.bat runClientSmoke 成功，生成 6 张菜单截图和 3 张世界截图；人工查看 `run/screenshots/appliedpackaging-client-smoke-package_assembler.png`，确认左侧只有 AE toolbar 输出模式按钮、右侧为 5 格 AE2 speed-card 升级面板、样板槽有 encoded-pattern 背景标记、无样板时输入槽显示禁用状态；人工查看 `run/screenshots/appliedpackaging-client-smoke-world-all_machines.png`，确认装配室临时分子装配室轮廓模型正常渲染。
run/logs/latest.log 按 `ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON` 扫描无命中。

2026-07-08 在 ME Package Assembler 移除输入过滤 ghost 渲染、增加样板移除后残留输入红色错误状态、接入本机 AE 网络能量服务并按 AE2 分子装配室 speed-card 表消耗能量后，执行 .\gradlew.bat compileJava 成功，执行 .\gradlew.bat runGameTestServer 成功，138 个必需 GameTest 全部通过。
随后执行 .\gradlew.bat runClientSmoke 成功；人工查看 `run/screenshots/appliedpackaging-client-smoke-package_assembler.png`，确认输入槽不再绘制过滤物品、左侧 AE toolbar 和右侧 AE2 speed-card 升级面板仍正常显示。
执行 `rg -n "ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON" run/logs/latest.log run/logs/debug.log` 后仅发现 Netty/JDK Unsafe 访问探测栈，不含 missing texture、Missing model 或 Failed to read Screen JSON。

2026-07-08 在 AE2 原版 Pattern Encoding Terminal 增加包裹样板模式后，执行 .\gradlew.bat runClientSmoke --stacktrace 成功。
本次 smoke 自动摆放 7 个菜单目标，额外放置真实 AE2 Pattern Encoding Terminal part，并通过 AE2 `MenuOpener` 打开原版 PatternEncodingTermScreen；截图前切换到包裹样板模式，人工查看 `run/screenshots/appliedpackaging-client-smoke-ae2_pattern_encoding_terminal.png`，确认同级包裹样板 tab、包裹名称输入、颜色 swatch、marker 槽和 AE2 crafting grid 可见。
本次 smoke 生成 3 张世界截图和 7 张真实菜单截图；`verify-release.ps1 -RequireClientSmokeScreenshots` 的必需截图清单已扩展为 8 张，包含 world-me_packager、Package Assembler、ME Packager、AE2 Pattern Encoding Terminal、Package Pattern Terminal 和三种 Package Bus。执行 `rg -n "ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON|Mixin apply failed|InvalidInjectionException|IllegalClassLoadError|Timed out|timeout" run/logs/latest.log` 无命中。

2026-07-10 增加 Advanced Pattern Terminal 后执行 `.\gradlew.bat runClientSmoke --stacktrace` 成功，生成 3 张世界截图和 8 张真实菜单截图。人工查看 `run/screenshots/appliedpackaging-client-smoke-advanced_pattern_encoding_terminal.png`，确认 854x480、GUI scale 2 下顶部短标题、搜索框、两行网络库存、三列颜色按钮、灰色加号列、4x4 输入、4 行输出、水平滚动区和玩家物品栏完整显示且无重叠；高级终端右侧保持 AE2 透明轮廓，没有扩图色带。日志按 `ERROR|Exception|missing texture|Missing model|Failed to read Screen JSON|Mixin apply failed|InvalidInjectionException|IllegalClassLoadError|Timed out|timeout` 扫描无命中。
本次 `verify-release.ps1 -RequireClientSmokeScreenshots` 必需清单扩展为 9 张并通过，新增 `appliedpackaging-client-smoke-advanced_pattern_encoding_terminal.png`；`build`、`verify-assets.ps1`、`test-assets-audit.ps1`、`test-release-audit.ps1`、`verify-docs.ps1` 和机械发布审计均通过。
2026-07-10 将高级列数据迁移到独立 `advanced_processing_pattern` 后再次执行 `.\gradlew.bat runClientSmoke --stacktrace` 成功。人工查看同一截图，确认高级终端主体加宽为 230px、顶部 10 列网络库存、左侧竖向列滚动条、带 4px 间距的 4 个输入列、4 行输出、独立样板输出预览与居中玩家栏均完整可见；GUI 总高仍为 240px，没有超出 854x480、GUI scale 2 视口。
2026-07-10 在 Package Bus / Package Pattern Terminal `DataSlot` 同步改为服务端权威值与客户端菜单缓存后再次执行 `.\gradlew.bat runClientSmoke --stacktrace` 成功。人工检查 Advanced Pattern Terminal、Package Pattern Terminal 和三种 Package Bus 截图：标题左侧不再重复渲染共享输入索引 0 的铁锭；高级终端输入区铁/铜/金仅各显示在真实 processing slot；Package Pattern Terminal 可见 RED 输入列、32 个铁锭与 65 个钻石输出；三种总线可见 RED、钻石 marker 与 2000 mB water 过滤状态。日志关键字扫描零命中。
2026-07-10 在三种总线加入 `REQUIRE_CHANNEL`、两个菜单加入 host 驱动 ghost 刷新并修复高级终端 marker 编码后，再次执行 `.\gradlew.bat runClientSmoke --stacktrace` 成功。人工复查 Advanced Pattern Terminal、Package Pattern Terminal 和 Package Storage Bus 截图，现有布局与预填状态保持完整；日志发布阻断关键字零命中。
2026-07-10 修正 Advanced Pattern Terminal 列编辑层层级与事件透传后执行 `.\gradlew.bat runClientSmoke --stacktrace` 成功。runner 在同一菜单依次保存主界面和 `appliedpackaging-client-smoke-advanced_pattern_encoding_terminal_editor.png`，并等待每张截图回调完成后才切换 screen；人工检查两张截图均无黑块，弹层不透明覆盖网络库存，底层编码控件未穿出，色板、名称框和 marker 槽位于前景。必需截图清单扩展为 10 张。
2026-07-10 修正大数量 ghost menu 同步后执行 `.\gradlew.bat runClientSmoke --stacktrace` 成功；标准 Package Bus smoke 夹具仍只显示 RED、钻石 marker 和 2000 mB water，不再为了验证 100000 数量额外显示铁锭。三张总线源 PNG 的玩家物品栏内部抽样均为 `#E8ECEE`，核心 GUI 区域逐像素比较仅保留标题与正常渲染差异。
2026-07-11 按用户提供的 `adv-pattern-terminal-base.png`、`sprite.png` 与最终视觉重做 Advanced Pattern Terminal；两行网络库存时主体为 195x250、9 列网络库存，中间显示 4 列 x 3 行真实输入与 3 行真实输出，左侧滚动条同步查看第 4 行，底部滚动条水平滚动包裹列。执行 `.\gradlew.bat runClientSmoke --stacktrace` 成功；在 960x540、GUI scale 2 下人工检查主界面完整无裁切，标题、搜索、网络栏、双滚动条、样板区和玩家栏与目标 atlas 对齐；编辑层源 PNG 像素正常。日志按发布阻断关键字扫描无命中。`verify-assets.ps1`、`test-assets-audit.ps1`、`verify-docs.ps1`、170 个必需 GameTest 与 `build` 均通过。
2026-07-11 修正 Advanced Pattern Terminal 两行 base 的动态拉伸：中间重复段改为末行顶部 17px 加首行底边 1px 的独立 195x18 strip；六行合成图人工检查无首/末行边框重复。样板槽清除 1.20.1 自带 ghost 后只绘制新版背景，网络滚动条、右上角合成状态按钮和所有 slot hover 改为 AE2 1.21.1 样式。执行 `.\gradlew.bat runClientSmoke --stacktrace` 成功；截图以真实包裹作为主产物，样板槽不再双层叠加，滚动条无黑块。该轮误把后续提出的 1px 页面偏移和两个槽位对齐归到高级终端，已在同日归属纠正中撤回这些高级终端偏移。
2026-07-11 按用户提供的 `pattern_mode_packaging.png` 重做 AE2 原版 Pattern Encoding Terminal 包裹模式；有效 124x66 面板与目标图逐像素核对，在客户端截图物理坐标 `(300,170)` 排除两个动态按钮后为 `0/8056` 像素差异。主界面只保留 sprite 清空按钮、当前颜色设置按钮、3x3 输入、marker 与自动输出；颜色/名称编辑层另存 `appliedpackaging-client-smoke-ae2_pattern_encoding_terminal_settings.png`，确认底层槽位不穿出且事件不透传。必需截图清单扩展为 11 张。
```

## 6. Dedicated Server 验证

必须验证：

```text
服务端可启动
客户端类不被服务端加载
注册、recipe、datapack 加载无异常
```

推荐命令：

```powershell
.\gradlew.bat runServer
```

当前服务端 smoke：

```text
.\gradlew.bat runServer 已执行到专用服务端启动阶段。
服务端在读取 run/eula.txt 时按 Mojang EULA 要求停止，未继续进入世界加载。
停止前未出现 Applied Packaging 客户端类误加载、注册崩溃或 mod 扫描异常。
2026-07-03 新增 ClientSmokeRunner 后再次执行 .\gradlew.bat runServer，仍正常到达 EULA gate，未出现客户端 smoke 类误加载。
2026-07-03 06:35 再次执行 .\gradlew.bat runServer，服务端仍正常到达 EULA gate；run/logs/latest.log 未发现 ERROR、FATAL、ClientSmokeRunner、NoClassDefFoundError、ClassNotFoundException 或客户端类误加载关键字。
2026-07-03 06:42 在发布 jar 排除 ClientSmokeRunner 和 gametest classes 后再次执行 .\gradlew.bat runServer，服务端仍正常到达 EULA gate；run/logs/latest.log 未发现 ERROR、FATAL、ClientSmokeRunner、NoClassDefFoundError、ClassNotFoundException、InvocationTargetException、IllegalStateException、Dist.CLIENT 或 OnlyIn。
2026-07-04 02:39 在用户明确同意 EULA 且 run/eula.txt 为 eula=true 后执行 .\gradlew.bat runServer --stacktrace，服务端进入 world 加载并出现 Done (2.724s)! For help, type "help"。
本次 run/logs/latest.log 确认 Applied Packaging initialized、Starting minecraft server version 1.20.1、Preparing level "world"、Preparing start region for dimension minecraft:overworld、Enabled Gametest Namespaces: [appliedpackaging]。
本次 run/logs/latest.log 未发现 ERROR、FATAL、ClientSmokeRunner、NoClassDefFoundError、ClassNotFoundException、InvocationTargetException、IllegalStateException、Dist.CLIENT、OnlyIn、Missing model、Unable to load model、missing texture、Exception、Crash 或 crash。
因为 Gradle/Minecraft 控制台未接收 stop 命令，本次通过 Ctrl+C 终止 run；服务端 world-load 证据已落盘，25565 未残留监听。
2026-07-04 03:07 再次执行 .\gradlew.bat runServer --stacktrace，服务端进入 world 加载并出现 Done (2.400s)! For help, type "help"。
本次 run/logs/latest.log 确认 Applied Packaging initialized、Starting minecraft server version 1.20.1、Preparing level "world"、Preparing start region for dimension minecraft:overworld、Enabled Gametest Namespaces: [appliedpackaging]。
本次出现 1 条 Mojang/Yggdrasil external public-key fetch ERROR/WARN 栈，服务端仍正常进入 world-load；verify-release 将该外部认证服务噪声作为 WARN 忽略。
除该外部 Yggdrasil 噪声外，run/logs/latest.log 未发现 ClientSmokeRunner、NoClassDefFoundError、ClassNotFoundException、InvocationTargetException、IllegalStateException、Dist.CLIENT、OnlyIn、Missing model、Unable to load model、missing texture、Crash 或 crash。
因为 Gradle/Minecraft 控制台未接收 stop 命令，本次通过 Ctrl+C 终止 run；服务端 world-load 证据已落盘，25565 未残留监听。
2026-07-04 在 `mods.toml` 显式声明 GuideME mandatory dependency 后执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke` 成功；自动 server smoke 进入 world-load，停止本次 runServer 进程树，确认 25565 未监听，并通过 `verify-release.ps1 -RequireAssetContracts -RequireServerWorldLoad`，其中 GuideME dependency range 审计通过。
```

## 7. 发布验收

必须全部满足：

```text
git 工作树干净，发布 tag 可追溯；最终冻结后可用 `run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag` 机械验证
.\gradlew.bat build 成功
.\gradlew.bat runData 成功且生成资源已纳入 git
.\gradlew.bat runGameTestServer 成功，或记录无法运行的明确阻塞
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag 成功，或逐项记录等价命令结果
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 成功
生成 build/libs/appliedpackaging-<version>.jar
jar 在 Minecraft 1.20.1 Forge + AE2 15.4.10 客户端中可进入游戏
核心玩法按 01-requirements.md 的 R1-R13 验收
docs 与实现一致
```

## 8. 发布清单

当前发布前准备状态：

```text
CHANGELOG.md：已补齐 0.1.0-dev 发布记录、验证结果和已知限制
LICENSE.md：已补齐 All Rights Reserved 许可声明
README.md：已补齐安装要求、玩法流程、功能清单、验证状态和已知限制
release jar：已包含 README.md、CHANGELOG.md、LICENSE.md、META-INF/MANIFEST.MF 与 META-INF/mods.toml，README/CHANGELOG/LICENSE 和语言文件与当前仓库源文件同步，mods.toml 声明 Minecraft/Forge/AE2/GuideME 发布依赖范围，且不包含 ClientSmokeRunner、gametest classes、reference sheets、build/tmp、docs/assets 或本机绝对路径
release manifest：已生成到 build/release/，记录 jar SHA-256、版本范围和 git commit，并可由 verify-release-manifest.ps1 复验；manifest 自测覆盖有效清单、mod id 篡改、artifact hash 篡改和 clean-git 路径
release bundle：可生成到 build/release/，包含 jar、release manifest、README、CHANGELOG、LICENSE 和 SHA256SUMS，并可由 verify-release-bundle.ps1 复验
documentation audit：必需文档、文档入口、正式设计文档未清理占位和本地 Markdown 链接检查通过
asset resource audit：必需 PNG、路径归类、RGBA PNG header、可见非占位像素内容和尺寸检查通过
logo/icon：assets/appliedpackaging/logo.png、textures/gui/logo.png 和包裹/机器/总线图标已存在
release notes：已写入 CHANGELOG.md
known limitations：已写入 README.md 与 CHANGELOG.md
compatible Minecraft/Forge/AE2/GuideME version list：已写入 README.md，并由 gradle.properties / mods.toml 模板声明
```

## 9. 当前完成度审计

以 `docs/01-requirements.md` R1-R13 和本文件发布验收为准，当前状态：

```text
R1 17 色独立包裹物品：已满足，注册项、item tag、语言、图标和 GameTest 覆盖。
R2 无正常空包裹玩法：已满足，空包裹不进玩家配方/创造栏，物流和 GameTest 均拒绝无 PackageData 包裹。
R3 相同包裹才可堆叠：已满足，有序 contents、canonical hash 和 NBT GameTest 覆盖。
R4 GenericStack 数据模型：已满足，PackageData 使用 AEKey/GenericStack；物品由 Forge item handler 插入路径覆盖，物品/流体等泛型资源由 AE2 MEStorage 路径覆盖。
R5 不允许真实嵌套：已满足，打包计划和 MEStorage 端点会展开源包裹，GameTest 覆盖。
R6 ME 包裹装配室：已满足，任意 AE2 可解码已编码样板、普通样板 Fluix + 主输出 marker、按样板动态确定的稠密输入槽、81×81 高级样板逻辑上限、4x4 输入代理窗口、完成时扣料与缺料暂停、主输出/有序队列、样板门禁、样板权威颜色/marker、合成进度、pending queue、输出模式和顺序抽取均已覆盖。
R7 ME 打包机：已满足，只接入固定底部与模型背面的 AE2 MEStorage，空容量槽 9 单位/9 类型和 16k/64k/256k storage component、红石模式、独立的 fake marker 与内容过滤、反转过滤和整包拆包均已覆盖。
R8 样板终端：已满足，Advanced Pattern Encoding Terminal 使用一个 part/menu/screen 承载高级与包裹两页；右侧按钮切换并持久化当前页，放入两个专用载体会自动选择对应页。高级页的 1377 输入/4 输出与包裹页的 81 输入/marker/颜色/预览完全隔离；高级页使用 146px 编辑框，包裹页保留 124x66 面板原宽，菜单不创建 VIEW_CELL 槽。普通 AE2 Pattern Encoding Terminal 不增加包裹 UI，并拒绝 package_pattern 与 advanced_processing_pattern。独立 advanced_processing_pattern 使用自定义处理样板详情突破原版单样板 81 输入总上限，原版 AE2 processing_pattern 不承载高级列数据。
R9 包裹总线：已满足，正式玩家入口只保留 Storage/Unpacking Bus 两个 AE2 cable part，均只处理合法包裹且不把内部内容暴露为 ME 散装库存；两者要求 AE channel，Storage Bus 在线缓存增删与过滤刷新已有真实网络测试。Unpacking Bus 已按 ME Packager 拆包模式接入真实 held 包裹与 20 tick 进度，最终提交失败时保留并重试同一个包裹，NBT、GUI 取回和 part 拆除返还边界已实现。
R10 整包验证：已满足，MEStorage 打包/拆包先模拟后提交；Forge item handler 路径覆盖整包累计模拟拒绝与 check-then-push，不包含自定义逐槽回滚事务。
R11 Tooltip：已满足，包裹、样板、AE2 blank_pattern carrier 和 packaged-processing 输出提示已接入。
R12 英文与简体中文语言：已满足，语言 key 与占位符对齐审计通过。
R13 发布资源与元数据：已满足，jar、recipe、loot table、模型、材质、logo、mods.toml、README/CHANGELOG/LICENSE 均存在并已打包且关键文档/语言文件与源码同步；mods.toml 声明 Minecraft、Forge、AE2 与 GuideME 发布依赖范围；发布 jar 已排除 dev verification classes 和参考素材路径。
```

当前仍未完成的发布验收项：

```text
正式 UI/模型范围后的 Dedicated server full world-load：未完成。
原因：2026-07-04 候选发布基线已通过完整 `run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit`，并完成 dedicated server world-load smoke；高级样板终端 UI 已通过当前验证，但 IN-003 其余正式 UI/模型/动画范围仍等待用户描述与实现。
需要在 IN-003 剩余范围实现、验收并提交后重新执行 `run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag` 或等价命令，确认专用服务端进入世界加载、无客户端类误加载，且 tag 就绪门禁通过。
最终 clean git 发布门禁：未完成。
原因：当前非 UI 提交基线已通过 clean-git audit；但最终候选发布门禁应在 IN-003 全部范围实现、验收且全部文件提交之后重新执行。
最终 tag 就绪门禁：未完成。
原因：当前 `verify-release-readiness.ps1 -RequireReadyForTag` 会因 IN-003 仍有 UI/模型/动画范围待输入、接收窗口开放和发布 tag 未完成判定而失败；IN-003 冻结并迁移后才能通过。
```

当前记录的非阻塞发布后增强：

```text
任意 AEKey 直接输出 ghost editor
任意 AEKey 直接 required-content filter editor
批量 required content 编辑
```

## 10. 目标级验收审计

本节对照最初目标进行完成度判定。只有所有必需项都有当前证据时，才允许把目标标记为完成。

```text
需求分析：已完成。证据：docs/01-requirements.md 定义目标用户、核心场景、R1-R13、NF1-NF6 和范围边界。
概要设计：已完成。证据：docs/02-system-architecture.md 定义模块划分、数据流、版本适配和客户端/服务端边界。
详细设计：已完成。证据：docs/03-detailed-design.md 定义 PackageData、事务、机器、样板、总线、过滤和菜单规则。
设计入口：已完成。证据：docs/design.md 只保留当前定案摘要和分类文档入口。
讨论记录：已完成。证据：docs/chat-summary.md 保留历史讨论，并声明不作为最新实现规格。
AI 指令分离：已完成。证据：AGENTS.md 只放 agent 工作规则，docs/00-document-index.md 声明设计文档不承载 AI 指令。
Minecraft 1.20.1 优先：已完成。证据：gradle.properties、build.gradle、README.md 和 docs/design.md 均固定 Minecraft 1.20.1 Forge / AE2 15.4.10。
当前基线资源：已完成。证据：docs/04-asset-spec.md、docs/assets/*、src/main/resources/assets/appliedpackaging 下 PNG/模型/语言文件，以及资源审计记录；高级样板终端已纳入当前资源基线，IN-003 其余 UI/模型/动画替换尚未冻结。
功能实现：已完成当前 0.1.0-dev 非 UI 范围。证据：R1-R13 完成度审计均为已满足，170 个必需 GameTest 全部通过。
Git 初始化和文档管理：已完成。证据：仓库有连续提交记录，文档按 00-07 分类维护，开发流水记录在 docs/development-log.md。
发布 jar：已完成。证据：build/libs/appliedpackaging-0.1.0-dev.jar 存在，已通过 build、jar 内容审计和 release metadata 审计。
客户端可用性：历史基线已完成；2026-07-17 合并终端的资源加载与类加载需以本节末尾最新验证记录为准。旧 runClientSmoke 中原版终端包裹模式和独立包裹终端截图只作为历史证据，不代表当前 UI。
GameTest 验证：已完成。证据：`.\gradlew.bat runGameTestServer` 成功，170 个必需 GameTest 全部通过。
DataGen 验证：已完成。证据：.\gradlew.bat runData 成功，未写出新的 generated resources 内容。
Dedicated server EULA 前 classloading smoke：已完成。证据：.\gradlew.bat runServer 到达 EULA gate，未发现客户端类误加载关键字。
Dedicated server full world-load：当前基线已完成，最终发布前仍需重跑。证据：2026-07-04 runServer 进入 world，latest.log 出现 Done (2.400s)!；`scripts/run-release-checks.ps1 -AuditOnly -RequireAssetContracts -RequireClientSmokeScreenshots -RequireServerWorldLoad` 已通过，除外部 Yggdrasil public-key fetch WARN 外未发现客户端类误加载和关键错误；随后 `scripts/run-release-checks.ps1 -SkipBuild -SkipData -SkipGameTest -RunServerSmoke` 自动刷新 latest.log 并出现 Done (2.413s)!，25565 清理完成，`verify-release.ps1 -RequireAssetContracts -RequireServerWorldLoad` 通过；最新完整 `scripts/run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit` 记录刷新 dedicated server smoke，latest.log 出现 Done (2.471s)! 并通过完整 release audit；最终服务端验收等待 IN-003 正式 UI/模型范围冻结并完成后执行。
发布 tag：未完成。原因：高级样板终端已实现，但 IN-003 其余正式 UI/模型/动画范围尚待用户描述，最终 dedicated server full world-load 尚未在完整范围后验收；发布 tag 应在 IN-003 完成且服务端验收通过后创建。
Clean git 发布门禁：当前基线已完成，最终发布前仍需重跑。证据：当前非 UI 提交基线已通过 clean-git audit；IN-003 仍有范围待输入，最终冻结并提交后必须重新执行 `scripts/run-release-checks.ps1 -ReleaseCandidate -RequireCleanGit -RequireReadyForTag`，同时刷新并复验发布清单与发布附件包。
Tag 就绪门禁：未完成。证据：当前变更接收表仍包含 IN-003 部分待输入项，`scripts/verify-release-readiness.ps1 -RequireReadyForTag` 应失败并阻止发布 tag。
```

当前目标完成判定：

```text
不能标记完成。
当前不再是 EULA 或非 UI 功能阻塞；高级样板终端 UI 已完成当前实现与验证，IN-003 其余 UI/模型/动画描述尚待接收，最终发布范围尚未冻结。
发布 tag 应等待 IN-003 完成、重新验证并通过 dedicated server full world-load 与 tag 就绪门禁后再创建。
```

### 2026-07-11 统一包裹拾色弹窗

两个样板终端、ME Packager、ME Package Assembler、Package Pattern Terminal 全局/逐槽颜色入口及三种 Package Bus 已统一使用 `PackageColorPicker`。弹窗只包含颜色，无标题、名称或 marker；Fluix 在最左侧独立显示，其余 16 色按 8x2 排列。颜色格不注册为普通 widget，父 Screen 在弹窗打开时取消底层 tooltip 并拦截全部鼠标与键盘输入，因此每次 hover 只绘制一条 tooltip，触发按钮保持可见且点击不会透传。

2026-07-11 层级修正：拾色弹窗打开时不再把原版包裹模式输入/marker/output slots 或高级终端 processing slots 设为 inactive。`PackageColorPicker` 在父 Screen 完整绘制 slot/item 后，以 `z=400` 独立 pose 追加并由父界面统一提交批次；底层物品仍存在，只在与不透明弹窗重叠处被正常遮挡。原版 AE 终端在弹窗打开时只取消底层 tooltip，不取消槽位或物品绘制。重新执行 `compileJava` 与 `runClientSmoke` 成功；人工检查 `appliedpackaging-client-smoke-ae2_pattern_encoding_terminal_settings.png` 与 `appliedpackaging-client-smoke-advanced_pattern_encoding_terminal_editor.png`，确认两种终端均无黑块、物品不穿透弹窗且没有重复 tooltip。日志无发布阻断关键字。

执行 `.\gradlew.bat compileJava`、`.\gradlew.bat runClientSmoke`、`.\gradlew.bat runGameTestServer` 与 `.\gradlew.bat build` 均成功；171 个 required GameTest 全部通过，客户端生成全部 13 张 smoke 截图。人工检查 `appliedpackaging-client-smoke-ae2_pattern_encoding_terminal_settings.png` 与 `appliedpackaging-client-smoke-advanced_pattern_encoding_terminal_editor.png`，确认两个终端共享相同的无标题 17 色布局、按钮仍可见且弹窗位于槽位之上。`verify-docs.ps1`、`verify-assets.ps1` 与 `verify-release.ps1 -RequireClientSmokeScreenshots` 均通过：236 个发布资源与 jar 同步、149 个 PNG 非空、11 张必需截图有效、146 个双语 key 对齐，客户端日志无发布阻断关键字。

### 2026-07-11 原版包裹模式归属纠正与数量编辑

重新核对用户截图后确认：此前把“页面右下偏移 1px”和“输出/marker 槽位对齐”归到 Advanced Pattern Terminal 是错误的。已撤回高级终端 `leftPos/topPos` 整页偏移、blank/encoded pattern 与 Encode 按钮的额外右移；高级终端只保留 AE2 1.21.1 的新版 slot hover。1px 偏移改为只作用于 AE2 原版 Pattern Encoding Terminal 的包裹模式背景、3x3 输入、输出、marker、三个配置按钮及命中区；PackagePattern 的 12x14 图标在 22x22 tab 内重新居中。

包裹模式 processing fake slot 现在显示 `GenericStack` 数量并允许大于 1；菜单在包裹模式把 81 个输入识别为 AE2 processing pattern slots，使原版容器写入和中键数量编辑路径同时生效。Advanced Pattern Terminal 输入槽同样加入中键数量编辑页并通过 `SET_FILTER` 回写。执行 `.\gradlew.bat compileJava`、`.\gradlew.bat runGameTestServer` 和 `.\gradlew.bat runClientSmoke` 成功，172 个 required GameTest 全部通过。后续复测确认不是 1px 微调问题，重新按 124x66 贴图推导 marker `(95,7)` 与输出 `(98,31)` 的面板内 slot 原点；PackagePattern 图标恢复完整 16x16 sprite 单元并使用 AE2 水平 tab 的 `x+1,y+3` 偏移，再次执行 client smoke 验证。`appliedpackaging-client-smoke-advanced_pattern_encoding_terminal.png` 保持高级终端既有原点和新版 hover。

### 2026-07-11 原版包裹模式滚动输入

用户更新的 `pattern_mode_packaging.png` 已按原始字节覆盖项目资源，源/目标 SHA-256 均为 `AB254596C0AADE263DFB5816ED4824186BCDE69DCAA8B24CF3C00BF3B7EA6256`。有效面板仍为 124x66；新增左侧滚动条轨道，3x3 可见输入窗口首槽移动到面板内 `(16,7)`，marker 与输出分别保持 `(95,7)`、`(98,31)`。

原版 Pattern Encoding Terminal 包裹模式现在复用 AE processing panel 的 81 个 processing input fake slots 与 SMALL scrollbar，显示连续 3 行并可滚动全部 27 行；processing output slots 在该模式隐藏，专属 marker 与 crafting result 继续承担标记和包裹预览。包裹样板 NBT 输入容量同步扩展为 81，旧版少量稀疏槽数据保持可读；GameTest 通过真实 `menu.encode()` 验证第 81 个输入与数量保留。

执行 `.\gradlew.bat compileJava`、`.\gradlew.bat runGameTestServer` 与 `.\gradlew.bat runClientSmoke` 成功，172 个 required GameTest 全部通过。人工检查原版终端主截图确认左侧滚动条、Oak Log x4、marker 与输出对齐；检查原版设置截图和高级终端编辑截图，确认统一拾色弹窗在 `z=400` 时无黑块、底层物品保持存在且不穿透弹窗、底层 tooltip 被抑制。

### 2026-07-11 原版 processing 交互与 tooltip 对齐

对照 AE2 15.4.10 `PatternEncodingTermMenu`、`PatternEncodingTermScreen`、`ProcessingEncodingPanel`，以及 AE2 1.21.1 processing ScreenStyle 后完成复核。包裹模式的 81 个输入现在由 `isProcessingPatternSlot` 识别，因而空槽的物品/流体容器写入、非空槽的中键数量编辑、fake slot 的 `GenericStack` 数量和滚动行为均复用 AE2 原路径；编码数据不再限制为 `AEItemKey`，GameTest 覆盖 1000 mB water 与第 81 个输入的真实编码。空主产物槽注册 AE2 同 key 的 18x18 tooltip 区域，有物品时仍由 AEBaseScreen 的物品 tooltip 优先返回。

该阶段的旧原版终端叠绘实现曾在 `ScreenEvent.Render.Post` 绘制颜色弹窗，并由当时的 `ClientSmokeRunner` 在最终 flush 后保存 primary-output tooltip 截图；这条历史验证路径已由下文“包裹终端同 menu 独立 Screen 复核”取代。当前包裹 Screen 在自身 `super.render(...)` 后绘制弹窗，不再向原 AE Screen 注册 Render.Post 绘制。

### 2026-07-11 ME Packager heldBox 与装配室有序输出

本轮规则取代此前关于包裹名称、打包机隐藏输入/输出双槽和装配室 17 个实体输出槽的记录。包裹不再允许自定义名称；旧名称 NBT 仅忽略。ME Packager 使用单一 heldBox 和持久化状态区分待拆输入/待取输出，拆包在进度终点事务提交，失败保留输入并进入可重试阻塞状态。ME Package Assembler 使用一个主输出、一个只读下一包预览和严格有序持久队列，所有提取每次最多一个包裹。

执行 `./gradlew.bat compileJava` 成功；执行 `./gradlew.bat runGameTestServer` 成功，173 个 required GameTest 全部通过，其中新增网络在拆包进度中变化、heldBox 阻塞保留、网络恢复后重试成功的覆盖。执行 `scripts/verify-assets.ps1` 与 `scripts/verify-docs.ps1` 成功，`git diff --check` 无空白错误。按用户要求，本轮不执行 `runClientSmoke` 或截图验证，GUI 视觉由用户验收。

### 2026-07-12 原版终端包裹模式新版坐标与渲染隔离

包裹模式 panel、3x3 输入与滚动条已从混用的 AE2 15.4.10 坐标统一到 AE2 1.21.1 processing 基准：panel `left=8,bottom=165`，输入首槽 `left=24,bottom=158`，scrollbar `left=15,bottom=158`。滚动条 handle 使用项目内已标记来源的 AE2 1.21.1 small scroller sprite；清空/颜色按钮位于面板内 `(72,7)`、`(82,7)`。停用槽位移出渲染区域，防止 processing outputs 和 crafting grid ghost item 叠入 marker/输出区域。自动包裹预览不再注册 AE2 processing primary-output tooltip，client smoke 也不再生成该错误 tooltip 的截图。

执行 `.\gradlew.bat compileJava --stacktrace` 成功，Mixin refmap 正常生成；执行 `scripts/verify-docs.ps1` 与 `git diff --check` 成功。该轮不改变服务端行为，未重复运行 GameTest；按用户要求不运行客户端截图冒烟，视觉结果由用户验收。

用户复测暴露首次修正写入时机过早：AE2 的 `widgets.updateBeforeRender()` 在 Screen 更新之后再次执行原 processing 槽位定位，导致新版输入坐标和隐藏输出被覆盖。当前实现已在包裹模式下取消原 `ProcessingEncodingPanel.updateBeforeRender()`，改为由包裹布局在 widget 阶段最终定位 81 格滚动输入并彻底移出全部 processing outputs；普通 processing 模式不受影响。重新执行 `.\gradlew.bat compileJava` 成功；按用户要求未运行客户端截图冒烟。

### 2026-07-12 Package Bus AE2 part 与五行 GUI

取消 Package Export Bus 和独立 Package Pattern Terminal 的物品注册、配方、创造栏入口、loot 与 smoke 步骤；Package Storage Bus 与 Package Unpacking Bus 改为真实 AE2 `PartItem`，分别暂用 AE2 Storage Bus 与 P2P Tunnel 模型层。共享 GUI 按新版 AE2 视觉组织五行过滤：默认两行、每张容量卡增加一行、最多五行；模糊/反转按钮按升级卡动态出现，颜色按钮始终与其相邻；未解锁行使用 `OptionalFakeSlot` 半透明状态。卸货总线额外允许四张加速卡，显示右上只读工作槽外观与 15 级进度条；工作包裹物品预览网络同步尚未实现，不记为已完成。

执行 `.\gradlew.bat compileJava`、`.\gradlew.bat runData` 与 `.\gradlew.bat runClientSmoke --stacktrace` 成功；客户端自动放置两个真实 cable part，插入模糊/反转/容量卡（卸货另插加速卡）并生成 `appliedpackaging-client-smoke-package_storage_bus.png`、`appliedpackaging-client-smoke-package_unpacking_bus.png`。人工确认存储总线遮掉右上工作区，卸货总线显示工作槽/进度区，三行已解锁、两行半透明禁用，未解锁行不显示颜色按钮。行为验证执行 167 个必需 GameTest 全部通过。

### 2026-07-12 Package Bus 当前 AE2 源码回移复验

撤销错误的单 atlas 烘焙方案并恢复用户 GUI/sprite 原字节后，以官方 AE2 current-main 提交 `45f315517ea346efc0babd02c85c6b9d32dc8acf` 为客户端视觉基线。`states.png`、`extra_panels.png`、`vertical_buttons_bg.png` 作为独立原样资源加载；Package Bus 回移了新版 Priority tab、toolbar normal/hover/focus 与 6px 间距、连接目标提示、升级面板和禁用槽 0.2 alpha。客户端 smoke 在两个 part 前放置真实 Chest，截图确认两者显示 `Attached to: Chest`，存储总线不显示工作区，拆包总线工作槽/进度条避让 Priority，升级槽按 8+1 分栏。

验证 `.\gradlew.bat build --stacktrace` 成功；`.\gradlew.bat runGameTestServer --stacktrace` 成功，167 个 required GameTest 全部通过；`.\gradlew.bat runClientSmoke --stacktrace` 成功并生成两张 960x540 截图。`scripts/verify-assets.ps1`、`scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 均成功。资产自测包含篡改 `package-storagebus.png` 后必须因 SHA-256 门禁失败的负例。工作包裹物品预览的数据源仍未实现，因此只读槽本轮按空槽验收，不把该功能记为完成。

### 2026-07-13 Package Unpacking Bus held 状态与装配室菜单同步修复

本节取代上一段“工作包裹数据源未实现”的当前状态。Package Unpacking Bus 现在先模拟相邻目标并从 MEStorage 精确抽取一个匹配包裹，将其保存为 part 的真实 `heldPackage`；目标在按 0/1/2/3/4 张加速卡对应 20/15/10/6/4 tick 的工作进度结束前不获得内容，更多卡不再缩短周期。最终 tick 会重新校验过滤和累计目标容量，成功时全量提交并清空 held 状态；失败时保持同一个包裹、本地阻塞并按同一拆包速度表等待后重新验证，恢复后重新执行完整进度。held 包裹及工作/阻塞/冷却状态写入 NBT，working/blocked 标志同步到菜单；工作中不可取，阻塞时可由菜单取回，并使用与 ME Packager 相同的半透明红底和红框提示，part 拆除时 held 包裹进入额外掉落。

Package Assembler 的有序主输出包装器改为 `IItemHandlerModifiable`，其 `setStackInSlot` 映射真实主输出，修复客户端菜单同步调用 `SlotItemHandler#set` 时的 `ClassCastException`。新增回归直接调用该崩溃路径，并覆盖预留/最终提交分离、目标中途变化、真实带中心线缆的 AE2 part 延迟提交与阻塞恢复，以及 held 包裹 NBT/拆除掉落/清空防复制。首次 GameTest 运行的两项失败来自测试 CableBus 只放侧面 part、遗漏中心玻璃线缆；补齐真实网格夹具后产品逻辑通过。新增持久性测试首次又使用了脱离 host 的裸 `createPart()`，触发 AE2 `UpgradeablePart` 的合法生命周期前置条件；改为真实安装 Part 后复验，`.\gradlew.bat runGameTestServer` 成功，173 个 required GameTest 全部通过。`.\gradlew.bat runClientSmoke` 成功生成全部截图；扫描 `run/logs/latest.log`，`ClassCastException|IItemHandlerModifiable|ERROR|Exception` 无命中，装配室、存储总线和卸货总线截图均成功生成。最终 `.\gradlew.bat build`、`verify-docs.ps1`、`verify-assets.ps1`、`verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。

### 2026-07-13 Package Bus 行原点与升级面板校正

本节取代上一版“拆包总线升级槽按 8+1 分栏”的当前视觉验收。五行 marker/content 槽从底图第一行 `y=29` 开始，默认解锁的第 0、1 行因此显示在最上方两行；8px 模糊/反转/颜色按钮使用 5px 行内上边距。右侧升级面板由 `top=0` 调整为 `top=-1`。对照 AE2 current-main `IOBusPart#getUpgradeSlots()` 的固定 5 格语义，Package Storage Bus 与 Package Unpacking Bus 均改为 5 格共享升级库存；卸货总线仍允许最多 4 张加速卡，但不再把 fuzzy/inverter/capacity/speed 的兼容上限相加成 9 格。

新增 `packageBusPartsUseFiveSharedUpgradeSlots` GameTest，通过真实 Cable Bus host 同时安装两个 part 并断言各自升级库存为 5 格。执行 `.\gradlew.bat runGameTestServer --stacktrace` 成功，174 个 required GameTest 全部通过。第一次 `.\gradlew.bat runClientSmoke --stacktrace` 因 IDE 中既有调试客户端占用 `New World` 的 `session.lock` 而无法进入世界；未终止该用户进程，改为复制一个排除锁文件的隔离 smoke 世界后执行 `.\gradlew.bat "-Pappliedpackaging.clientSmoke.world=AP Smoke Isolated" runClientSmoke --stacktrace` 成功，并在退出后删除临时世界。人工检查两张总线截图，确认第一行起始、按钮上边距、升级面板上移、5 格总数及底部无孤立槽块；双图同批预览曾出现查看器黑块伪影，源 PNG ARGB 抽样和单图重新解码均正常。

`.\gradlew.bat compileJava --stacktrace`、`.\gradlew.bat build --stacktrace`、`scripts/verify-docs.ps1`、`scripts/verify-assets.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。用户提供的 `package-storagebus.png` / `package-storagebus-sprites.png` SHA-256 仍分别为 `7253977C9792F7BB86D1B826688DD067AF5F242E3279A71E7409442428B53EB5` / `14D7D26A93BF46D1BA0EF33A5408197718D0AF5BD3ADE662AA8A46E8DE662281`，本轮未修改材质。

### 2026-07-13 Package Bus 五张容量卡与高级终端槽位/按钮复验

Package Storage Bus 与 Package Unpacking Bus 继续共用 5 格升级库存，但容量卡的单卡种安装上限由 3 调整为 5。扩展既有真实 Cable Bus GameTest，逐个 part 验证前 5 张容量卡均可插入、安装计数为 5、第 6 张被拒绝，并验证 `enabledRows()` 仍封顶 5 行，因此第 4、5 张容量卡不再增加过滤行。执行 `.\gradlew.bat runGameTestServer --stacktrace` 成功，174 个 required GameTest 全部通过。

首次把用户截图圈选对象误读为编码按钮，因而只把按钮 widget 从 `left=166` 调整到 `left=167`。用户澄清圈选的是上下两个样板槽后，最终修正为：`BLANK_PATTERN` 从 `(166,166)` 调整到 `(167,167)`，`ENCODED_PATTERN` 从 `(166,119)` 调整到 `(167,120)`；两槽内容各向右、向上 1px。按钮不再继续移动，保持 `left=167,bottom=146`；按钮的 16x16 图标/点击区与两个 16x16 槽内容区共用 `x=167`，18x20 背景按 `x-1` 规则从 `x=166` 开始，与底图的 18px 槽框同轴。执行 `.\gradlew.bat "-Pappliedpackaging.clientSmoke.world=AP Smoke Isolated" runClientSmoke --stacktrace` 成功，11 张截图全部生成并自动退出；人工检查新的 `appliedpackaging-client-smoke-advanced_pattern_encoding_terminal.png`，确认两个槽内容已进入槽框中心且按钮仍位于同一竖直中心线。

未修改用户提供的高级终端 base PNG；源文件与运行时文件 SHA-256 均为 `660EF8C5379F1131E4D3D773FD43EE9954DE1F0FCE278DF78C30F75D9B5563F6`。像素复核结论为：顶部 `y=0..51` 与 AE2 current-main `terminal.png` 对应区域逐像素一致，所有不透明颜色也都来自 current-main 调色板，因此不存在整体偏色；截图中的禁用列 `#969CB1` 来自客户端常量而非 PNG，且该色不在用户图或 current-main 不透明调色板中。右侧空白/已编码样板槽框相对 current-main 背景平移为 `(+20,-2)`，修正后的两个逻辑槽使用相同平移，不再保留此前 `(+19,-1)` 导致的物品左 1px、下 1px 偏差。

### 2026-07-13 Package Bus 七行与外围 current-main 复核

本节取代此前“容量卡后两张不增加过滤行”和“升级面板 `top=-1`”的当前结论。Package Storage Bus 与 Package Unpacking Bus 的 `FILTER_ROWS` 改为 7；基础第 0、1 行默认启用，5 张容量卡依次解锁第 2 至第 6 行。扩展既有真实 Cable Bus GameTest，在两个 part 上逐张断言 `enabledRows()` 从 3 增长到 7，5 张均可插入且第 6 张拒绝。执行 `.\gradlew.bat runGameTestServer --stacktrace` 成功，174 个 required GameTest 全部通过。

对照 AE2 current-main `StorageBusScreen`、`VerticalButtonBar`、`UpgradesPanel`、`TabButton`、`states.png`、`extra_panels.png`、`vertical_buttons_bg.png` 与 `storagebus.png` 后修正三项代码侧差异：左侧工具栏补回 Help 并保持 6px 间距；升级面板恢复 current-main `right=2,top=0`；空升级槽不再使用依赖 AE2 15.4.10 的灰阶 `BACKGROUND_UPGRADE`，改绘项目内原样 current-main `ae2-states.png` `(240,208,16,16)`。Priority tab 的 `(152,-5,20,20)`、图标偏移、工具栏按钮背景/图标坐标、升级面板 5px padding 和真实升级物品槽偏移均已与 current-main 源码一致。

执行 `.\gradlew.bat runClientSmoke --stacktrace` 成功，11 张截图全部生成并自动退出。存储/卸货总线截图确认 7 行完整显示，当前 1 张容量卡夹具表现为上方 3 行启用、下方 4 行 0.2 alpha 禁用；Help、Priority、5 格升级面板和新版空槽占位均正常。卸货截图并排放置无 marker/带 marker 包裹并在 held 槽放置同一带 marker 包裹，三者在 GUI scale 2 下的槽内非背景像素包围盒均为 `(9..22,11..23)`，证明自定义 package renderer 没有额外 offset。

用户 `package-storagebus.png` 未被修改，源/运行时 SHA-256 仍为 `7253977C9792F7BB86D1B826688DD067AF5F242E3279A71E7409442428B53EB5`。像素审计确认中心外有两类待修图问题：10246 个 current-main `#CBCCD4` 主体像素被画成 `#ADB0C4`，854 个 `#413F54` 最外圈像素被画成 `#CBCCD4`。用户 sprite 的 `SLOT_BACKGROUND` 与 current-main 完全一致，无需重画。实际插入后的 fuzzy/inverter/capacity/speed 卡仍使用 AE2 15.4.10 物品贴图；若要求装卡状态也匹配新版，需另行补充或授权覆盖四张新版 card item texture。

最终 `.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 均通过；5 个资产合同验证成功，jar 内 237 个 Applied Packaging 发布资源与源码一致，154 张 PNG 非空，客户端日志无发布阻断关键字。

### 2026-07-13 Package Bus 更新底图接入

将用户更新的 `E:/resources/textures/appliedpackaging/ret/package-storagebus.png` 原字节复制到运行时资源；两者均为 256x256 RGBA、1583 字节，SHA-256 同为 `506BE44EF826C14C1DBE37C076EDC7955C0DBFE35A7DB9B157EABA8E241787DE`。相对旧图共变化 11262 个主界面像素：10408 个 `#ADB0C4` 恢复为 current-main 主体色 `#CBCCD4`，854 个 `#CBCCD4` 恢复为最外圈 `#413F54`。忽略全透明像素的 RGB 并排除自定义中心 `x=7..168,y=28..154` 后，新图与 AE2 current-main `storagebus.png` 的可见像素差异为 0；七行过滤区、右侧工作槽/进度素材和独立 sprite 保持完整。

第一次使用已被上一轮删除的 `AP Smoke Isolated` 名称运行 client smoke，客户端完成资源加载后等待不存在的 quick-play 世界，244 秒后被工具终止，未生成新截图；只清理了本轮 wrapper/client 进程，未终止用户进程。确认默认 `New World/session.lock` 未被占用后执行 `.\gradlew.bat runClientSmoke --stacktrace` 成功，11 张截图全部生成并自动退出。分别检查 `appliedpackaging-client-smoke-package_storage_bus.png` 与 `appliedpackaging-client-smoke-package_unpacking_bus.png`，确认新版主体/外框颜色、七行过滤、Priority、5 格升级区以及卸货工作区均正常；卸货图在双图同批预览中的黑块再次由单图解码确认只是查看器伪影。

`terminal_and_buses` asset contract、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1` 负例套件、`.\gradlew.bat build --stacktrace`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 均通过；负例套件确认把 Package Bus 底图替换为其它 PNG 时仍会被 byte-preserved 哈希门禁拒绝。源图、运行时资源和 JAR 内条目三者 SHA-256 完全一致。GameTest 已考虑；项目已有 `runGameTestServer` 与 Package Bus 行为测试，但本次只替换字节保持的客户端 GUI atlas 并更新哈希/文档，不改变菜单、网络、存储或服务端行为，因此未新增或重复运行 GameTest。

### 2026-07-13 Marker 槽、hover 与升级区统一

Package Bus tooltip 阶段的 current-main 槽位 hover 已从错误的菜单相对坐标改为 `leftPos/topPos` 窗口坐标。ME Packager 与 ME Package Assembler 不再由 AE2 15 `UpgradeableScreen` 自动注入旧版 `UpgradesPanel`，改用共享 `ModernUpgradeableScreen`：槽位 hover 使用 `0x669cd3ff` 填充与 `0xffdaffff` 边线，升级/工具箱面板使用 current-main 布局和独立 `package_bus_extra_panels.png`，空升级槽使用 `ae2-states.png` `(240,208,16,16)`。总线、两台机器和原版 Pattern Encoding Terminal 包裹模式的空 marker 槽统一绘制用户 sprite `(32,16,16,16)`，并在可交互空槽上显示双行双语说明 tooltip；锁定总线行的 marker 图标跟随 `0.2` alpha。

执行 `.\gradlew.bat compileJava processResources --stacktrace` 成功。第一次默认 `runClientSmoke` 因用户既有客户端占用 `New World/session.lock` 未能进入世界；未终止用户进程，终止的仅是本轮 wrapper/client，并从仓库未占用的测试世界复制出隔离世界。执行 `.\gradlew.bat runClientSmoke '-Pappliedpackaging.clientSmoke.world=AP Smoke GUI' --stacktrace` 成功，11 张截图全部生成并自动退出。人工检查 Package Assembler、ME Packager、AE2 Pattern Encoding Terminal、Package Storage Bus 与 Package Unpacking Bus 截图，确认 marker 图标/tooltip、机器新版升级区与 hover 正常，Bus hover 位于第二行 marker 槽而非屏幕左上角；卸货总线单图解码正常，多图黑块是查看器伪影。

GameTest 已考虑。此次只修改客户端 Screen/Widget/Mixin 绘制、ScreenStyle 视觉锚点、语言和 smoke 鼠标定位，不改变菜单槽数量、服务器事务、网络同步、存储、过滤或升级兼容行为；既有 174 个 required GameTest 已覆盖相关服务端行为，因此本轮不新增也不重复运行 GameTest。

最终 `.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、`scripts/verify-docs.ps1` 与 `scripts/verify-release.ps1 -RequireAssetContracts` 全部通过；237 个 Applied Packaging 发布资源与 JAR 一致，5 个资产合同、154 张 PNG、153 个双语 key 及占位符均通过审计。

### 2026-07-13 新版容量/样板空槽图标与 Bus 2px 按钮边距

ME Packager 与 ME Package Assembler 的容量元件空槽改为从项目内原样 current-main `ae2-states.png` 取样 `(240,48,16,16)`；装配室的已编码样板空槽改用 `(240,112,16,16)`。总线每行的模糊、反转与颜色按钮统一使用固定 `2px` 上边距，绘制原点、widget/点击区域及 hover 外框共用 `rowY + 2`，不再用 `(18-8)/2=5px` 的垂直居中位置。没有修改用户提供的 `package-storagebus.png` 或 `package-storagebus-sprites.png`，也没有修改原样复制的 `ae2-states.png`；其 SHA-256 仍为 `0996B0084C7BF37F65A97A745982AB681EBD86F142FADE526F14C823C4727E55`。

执行 `.\gradlew.bat runClientSmoke '-Pappliedpackaging.clientSmoke.world=AP Smoke Slot Icons' --stacktrace` 成功，隔离世界在退出后安全删除，11 张截图全部刷新。人工检查 ME Packager 与 Package Assembler 截图，两个新版空槽图标完整显示且与槽框对齐；检查 Storage/Unpacking Bus 截图，三枚按钮在 GUI scale 2 下保留 4 个物理像素的行顶间距，等价于逻辑坐标 `2px`，marker tooltip/hover 与七行锁定状态无回归。卸货总线多图查看时的黑块仍只出现在查看器合批结果，单图重新解码完整正常。

GameTest 已考虑但未重复运行：本轮只改变客户端空槽图标切片与按钮视觉/命中 Y 坐标，不改变菜单槽数量、升级卡上限、过滤语义、总线事务、网络同步或服务端状态；既有 174 个 required GameTest 继续覆盖这些行为路径。最终 `.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过；JAR 内 237 个发布资源与源码一致，5 个资产合同、154 张 RGBA PNG、153 个双语 key/占位符均通过审计。

### 2026-07-13 全仓清理与事务加固复验

删除旧 Package Bus 方块族、独立 Package Pattern Terminal 实现及对应资源/测试后，执行 `.\gradlew.bat runGameTestServer --stacktrace` 成功，144 个 required GameTest 全部通过。新增覆盖包括归并金额溢出拒绝、整叠拆包乘法溢出保持原栈、流体多项提交失败回滚，以及真实 Package Storage Bus part 只挂载合法包裹存储。

执行 `.\gradlew.bat runClientSmoke --stacktrace`：历史开发世界第一次载入时按 Forge 标准流程备份并移除四个已删除 block ID；同一世界完成迁移后立即复跑成功，11 张必需截图全部刷新，第二次日志无 missing registry/model/texture、mixin 或客户端类加载阻断项。随后 dedicated server smoke 到达完整 world-load，端口清理成功。

最终执行 `build`、`runData`、文档/资产审计及其负例自测、release audit 自测和完整 release self-tests 均成功。`verify-release.ps1 -RequireAssetContracts -RequireClientSmokeScreenshots -RequireServerWorldLoad` 确认 JAR 与 215 个发布资源一致、79 个资源 JSON 可解析、140 张 PNG 非空、5 个资产合同有效、148 个双语 key/占位符一致、11 张截图有效，客户端和服务端日志均无发布阻断关键字；JAR 额外检查确认不包含被删实现的 class、block model/state/texture 或遗留 block loot table。

### 2026-07-13 ME Packager 容量升级定稿与 item handler 插入收敛

容量档最终收敛为 default 9/9、16k 16/16、64k 64/63、256k 256/63。ME Packager 与 Package Assembler 的容量槽都只接受 AE2 16k、64k、256k storage component；1k component、完整 storage cell、portable cell、4k 与附属 Mod 大容量档均拒绝。GameTest 覆盖三档组件映射、拒绝边界、空槽 9/9，以及 package_pattern 本地执行和 Pattern Provider push 无法绕过当前容量档。

卸货总线向 Forge item handler 拆包改为 Pattern Provider 式 check-then-push：完整包裹先在保留 slot limit / isItemValid 的累计快照上模拟，并额外调用真实 handler 的 simulate；全部通过后才用 `ItemHandlerHelper.insertItemStacked` 逐项真实插入。删除逐槽提交计划、反向抽取回滚和重复的提交前模拟。同步删除没有运行时调用的 item handler 打包规划、Forge fluid handler 适配、Package Export/即时拆包操作及其专用测试；包裹流体数据仍由 GenericStack/AEFluidKey 与 AE2 MEStorage 正式路径支持。

执行 `.\gradlew.bat runGameTestServer --stacktrace` 成功，100/100 required GameTest 全部通过；`.\gradlew.bat build --stacktrace`、`scripts/verify-docs.ps1`、`scripts/test-release-audit.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 均通过。发布审计确认 214 个发布资源与 JAR 一致、78 个资源 JSON 可解析、140 张 PNG 非空、5 个资产合同有效、140 个双语 key/占位符一致，并确认已删除的适配类没有重新出现。编译输出仍只有既有 25 条 Forge/Minecraft 1.20.1 removal 警告，按项目边界不处理。
### 2026-07-15 Package Bus 同优先级路由验证

确认右上角 Priority 子菜单直接读写 part 的 `IPriorityHost`，与 IStorageProvider 挂载使用同一数值，两个 part 默认值都为 0。对照 AE2 15.4.10 `FormationPlanePart` 确认默认值与挂载值都不应隐藏加一；再按 `NetworkStorage` 的同组两轮插入规则，让 Package Unpacking Bus 的实际只写入端点对合法包裹进入 preferred 首轮，`PackageItemStorage` 保持普通第二轮。真实共享 cable grid 先挂载存储、后挂载拆包，验证同值时卸货端先持有包裹，卸货端忙碌拒绝下一包时才正常回落到存储端；另一条真实网格回归把存储总线设为 1、卸货总线保持 0，确认玩家设置的更高数值仍优先。`.\\gradlew.bat runGameTestServer --stacktrace` 成功，105/105 required tests 全部通过。

### 2026-07-15 ME Packager 轮廓与四向动态渲染

ME Packager 的选择轮廓和碰撞体已从完整方块改为与正式模型一致的四向组合体，并明确排除帘子和运动包裹。BER 南北方向改用与 blockstate baked model 等效、符号相反的 `PoseStack` 角度，传送带、帘子和包裹现与静态主体工作口一致。新增回归覆盖 4 个 `facing`、6 个 `network_side` 及两种 shape；执行 `.\\gradlew.bat compileJava`、`.\\gradlew.bat build`、`scripts/verify-docs.ps1` 与 `scripts/verify-assets.ps1` 成功，执行 `.\\gradlew.bat runGameTestServer` 成功，106/106 required GameTest 全部通过。因用户调试客户端正在占用同一 `run` 目录，本轮保留该进程且未并发启动 client smoke。

### 2026-07-15 ME Packager 输出位置、连续传送带与背面接网

打包完成后的包裹静止点改为本地 `x=10/16,z=8/16`，对应去除 4px 后部模块后的 12x16 前部区域中心；包裹在静止和运动状态下都保留 1x1x1 stencil 裁剪，四条帘子也改用独立 immediate stencil pass，避免动态几何穿出机器背面。传送带 16px UV 相位进入方块实体 NBT 与客户端流同步，每个工作 tick 在既有相位上累加，停止和下一轮开始时不再归零。

AE 主节点连接面改为 `network_side + facing.getOpposite()`：地面放置默认形成底部与背面两个可接线面，两者都不暴露普通 item capability。真实线缆 GameTest 明确设置 `network_side=down`、仅从机器背面接入 AE2 Cable + Drive + Creative Energy Cell，成功完成打包；同一测试确认 20 tick 向外动画结束后相位保持为 `12/16`，方块实体保存/读取后仍为 `12/16`。`.\\gradlew.bat compileJava`、`.\\gradlew.bat build --stacktrace`、`.\\gradlew.bat runGameTestServer --stacktrace`、`scripts/verify-docs.ps1`、`scripts/verify-assets.ps1` 与 `git diff --check` 全部通过，106/106 required GameTest 成功。用户从 IntelliJ 启动的调试客户端仍占用同一 `run` 目录，本轮未终止该进程，也未并发启动 client smoke。

### 2026-07-15 ME Packager 包裹落点与 15px 动态裁剪复验

包裹渲染底面必须精确落在传送带顶面 `y=2/16`。验证按包裹模型最低点 `y=1/16`、item `fixed` 缩放 `0.5` 和机器 BER 外层缩放 `1.49` 反算渲染原点，不以截图目测替代坐标约束。包裹与帘子的 stencil 范围使用机器本地坐标 `x=1/16..16/16, y/z=0..1`，即只允许进入扣除 1px 背板后的 15px 深工作区，并随四个水平 `facing` 使用与动态模型相同的旋转。

执行 `.\\gradlew.bat compileJava --rerun-tasks`、`.\\gradlew.bat runClientSmoke --stacktrace` 与 `.\\gradlew.bat build --stacktrace` 成功，11 张必需截图全部刷新并正常退出。客户端日志扫描未发现缺失模型/贴图、OpenGL、framebuffer、stencil、崩溃或超时错误；唯一异常关键字是 smoke 主动退出后的服务端 `ClosedChannelException`。`scripts/verify-docs.ps1`、`scripts/verify-assets.ps1` 与 `git diff --check` 全部通过。本轮是纯客户端坐标和 stencil 修正，不改变机器状态、事务、网络、存储或碰撞，故不新增或重复运行 GameTest，以 client smoke 覆盖真实渲染路径。

### 2026-07-15 统一包裹颜色选择器与 None 布局验证

ME Packager、AE2 原版包裹样板模式、Advanced Pattern Terminal 和两种 Package Bus 已统一使用 `PackageColorPicker.TriggerButton` 与同一弹窗；高级终端旧的私有颜色按钮已删除。调用方显式传入 `allowNone`，只有两种总线过滤行启用 None 与右键清除。弹窗固定为 89x23，Fluix/None 在分隔线左侧上下排列；None 隐藏时保留空位，两个终端截图中的右侧 16 色与总线截图使用相同坐标。选中态只使用 8x8 背景 sprite，不增加外框，hover 不改变色格像素。

`package-storagebus-sprites.png` 的 `(48,0,8,8)`、`(56,0,8,8)`、`(48,8,8,8)` 分别写入用户截图中的默认 Fluix、None 和选中背景；确定性脚本为 `scripts/update-package-color-picker-sprites.ps1`，最终 SHA-256 为 `632A686B6F8EC7B712326DC52E639CE43CF8E1B55C44D00309B62B672B766635`。与修改前图集逐像素比较，仅三个单元内 192 像素变化，单元外包括透明 RGB 的变化数为 0。`scripts/verify-assets.ps1` 与完整 `scripts/test-assets-audit.ps1` 正/负夹具均通过。

颜色弹窗打开时会暂停锚点触发按钮自身的 hover tooltip，关闭时恢复。隔离世界 `AP Smoke Color Picker 20260715` 的 `runClientSmoke` 成功刷新 11 张必需截图；`package_storage_bus` 烟测现在保持允许 None 的颜色弹窗打开，并把鼠标停在首行颜色按钮上。人工检查该截图确认没有触发按钮 tooltip 穿透，同时检查两个终端编辑截图确认 None 隐藏不移动布局、无额外 hover/选中外框或绘制黑块。客户端日志未命中错误关键字。

`gradlew.bat compileJava --stacktrace`、`gradlew.bat build --stacktrace`、`scripts/verify-docs.ps1`、`scripts/verify-assets.ps1`、`scripts/test-assets-audit.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。GameTest 已考虑并确认现有 `runGameTestServer` 路径；本轮只修改客户端 widget、输入提示拦截和 GUI sprite，不改变菜单 action、网络、存储、过滤或服务端行为，因此未新增或重复运行 GameTest。

### 2026-07-15 ME Packager 方向、接线与右键区域最终验证

本节取代上方关于可切换接线状态的历史实现记录。ME Packager 当前 blockstate 只有四个水平 `facing`；固定底部与模型背面接入同一个 AE 主节点，其它四面 `AECableType.NONE`。AE2 标准扳手只旋转 `facing`，旋转后模型背面接线和模型坐标命中区域同步变化。只有传送带上表面允许手动放入/取出包裹，右键其它模型位置打开 GUI；底部和模型背面不暴露普通 item capability，其余四面保留包裹自动化 capability。

`.\gradlew.bat runGameTestServer` 最终 106/106 required tests 通过，覆盖固定底部真实 Interface、固定模型背面真实线缆、逐面接线类型、扳手旋转后的连接迁移、四朝向轮廓和四朝向传送带点击。`.\gradlew.bat runData`、`.\gradlew.bat build`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 通过。自动客户端 smoke 已按项目同日决定删除，当前不存在 `runClientSmoke` Gradle task，本轮不恢复该已删除流程。

### 2026-07-15 ME Packager 输出包裹残影回归

ME Packager 的 BER 静止态只读取方块实体 stream 同步的 `renderedBox`，不再优先读取客户端 heldBox 库存副本。服务端在玩家或自动化取走输出后同步空视觉栈时，即使客户端仍保留未同步的旧库存 NBT，也必须立即停止绘制包裹。

新增 `mePackagerVisualSyncClearsRemovedPackage` GameTest：先在模拟客户端保留包裹库存，再加载只包含空 `renderedBox` 的 AE2 visual update，确认视觉更新不改库存副本但 `getRenderedBox()` 已为空。`.\gradlew.bat compileJava --stacktrace`、`.\gradlew.bat runGameTestServer --stacktrace` 与 `.\gradlew.bat build --stacktrace` 成功，107/107 required GameTest 全部通过。

### 2026-07-16 Marker fake slot 与装配室高级样板输入回归

ME Packager marker 配置改为 AE2 `FakeSlot` + config-types inventory 后，GameTest 直接检查菜单槽类型、真实 item handler 只剩两槽、marker 配置 NBT 往返，以及错误颜色、marker、contents 分别拒绝、三项全部匹配才接受。Package Assembler 高级样板本地输入改为 sparse 到 dense 映射后，GameTest 使用两列 162 个 sparse 位置只填 4 项，确认显示顺序为 iron、gold、copper、diamond、第 5 格锁定、有效输入槽数为 4 且不滚动；另用 70 项确认输入槽/capability 动态扩展、第 69 个输入仍可显示和滚动，再切回 4 项样板确认尾部 disabled 行与滚动范围立即收缩。最终本地装配验证两个输出包裹继续分别使用样板列保存的颜色、marker 与 row 顺序。

`.\gradlew.bat compileJava --stacktrace`、`.\gradlew.bat runGameTestServer --stacktrace` 与 `.\gradlew.bat build --stacktrace` 成功，110/110 required GameTest 全部通过。项目已删除自动 `runClientSmoke`，本轮没有恢复；客户端可见数据由真实菜单 slot stack、4×4 代理窗口、动态滚动上限和 Screen 每帧 range 刷新的确定性测试覆盖。

### 2026-07-16 包裹物品居中、Shift marker 与打包机正面朝向

对照 Forge 1.20.1/Minecraft `ItemRenderer` 的标准物品路径确认，marked package 的 BEWLR 在递归渲染普通 baked model 前执行的 `+0.5` 平移只抵消嵌套 `ItemRenderer` 的第二次 `-0.5`，不是包裹偏下的来源。真正原因是盒体几何 `y=1..9` 的中心为 5，而 GUI display 围绕模型坐标 8 旋转；共享与 marked GUI transform 统一增加 `translation [0,3,0]`。Forge `IItemDecorator` 为全部 17 色包裹注册 Shift 附加层：仅有物品 marker 且按住 Shift 时，在原图标右下角绘制 8x8 marker，不替代前脸既有 3x3 标记。ME Packager BER 在机器局部坐标额外绕 Y 轴旋转 `+90°`，使包裹正面朝本地 +X 工作口。

本轮是纯客户端 item model、GUI decorator 与 BER 姿态调整，不改变服务端事务、容量、过滤、网络同步、碰撞或物品移动语义。已检查现有 GameTest 覆盖范围，未新增或重复运行 GameTest。项目已删除自动 `runClientSmoke`，且用户的 IntelliJ 调试客户端正在使用同一 `run` 目录，因此保留该进程且不并发启动客户端；最终方向与叠加位置需在重启调试客户端后人工确认。`.\gradlew.bat compileJava processResources`、`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过；新增负例确认 GUI translation 回退到 `[0,0,0]` 时资源审计会失败。

### 2026-07-16 Package Unpacking Bus 进度 sprite 修正

用户实机截图指出卸货总线右上进度条没有使用 UI atlas 已提供的活动 sprite。根因是 `PackageBusScreen` 从空进度框 `[196,0,6,18]` 取一个像素，代码染成 `#6fffe9` 后拉伸为 4px 宽纯色块；这绕过了同一 `package-storagebus.png` 右侧已经存在的绿色条纹进度素材。

修正后活动进度直接读取 `[176,32,6,18]`，把同步的 0-15 级进度按 AE2 竖向 `ProgressBar` 的整数规则从底部裁切；源图保持 6x18 原尺寸，不缩放、不重新着色，空框继续使用 `[196,0,6,18]`。确定性合成检查覆盖 0/3/6/9/12/15 六档，确认绿色条纹逐级覆盖空框；两个用户 GUI PNG 均没有产生 diff。

执行 `.\gradlew.bat compileJava --stacktrace`、`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部成功；发布审计确认 198 个发布资源与 JAR 一致、72 个 JSON 可解析、130 张 PNG 非空、5 个资产合同有效、140 个双语 key/占位符一致。项目已删除自动 `runClientSmoke`，本轮不恢复；最终游戏内像素可在重启开发客户端后于 Package Unpacking Bus 工作态人工确认。本轮只修改客户端 atlas 取样和裁切，不改变工作周期同步、菜单、存储、过滤或服务端事务，因此不新增或重跑 GameTest。

### 2026-07-16 原版样板终端包裹模式整屏委托与编码崩溃回归

AE2 Pattern Encoding Terminal 保持原 `PatternEncodingTermScreen` 实例和 Screen factory。原生 crafting、processing、smithing table、stonecutting 模式下，原 ScreenStyle、终端主体、panel/tab、数量子 Screen、recipe preview、tooltip 与编码按钮继续完整执行 AE2 15.4.10 路径，本模组只通过 `ScreenEvent.Init.Post` 多加一个包裹 tab。切入 package 后，客户端临时显示使用同一个 `PatternEncodingTermMenu` 的 `PackagePatternScreenDelegate`；原 Screen 保留为返回目标但不在下层渲染或更新，包裹 Screen 独立拥有新版终端底图、五个 tab、左侧工具栏、网络 scrollbar、右上显示元件面板、package panel、样板槽、Encode/合成状态、slot hover、tooltip 与颜色弹层。切换不关闭容器、不重新创建 menu，也不替换 AE2 factory。

模式同步使用单一 `PatternTerminalMode` 与一次 `apSelectTerminalMode` action。客户端发送 action 后立即执行同值本地投影，服务端在一次处理内再次投影 AE2 原生 `EncodingMode`、package logic、slot active/amount 和 preview；GameTest 在真实 Pattern Encoding Terminal menu 上断言 PACKAGE -> PROCESSING -> CRAFTING 三次选择都原子更新复合模式、原生模式和 package logic。Screen 返回路径还需人工验证 Repo 条目/供电、搜索词、网络滚动与原生 slot 图标连续，避免双字段不同步时短暂绘制 crafting 或用错误模式编码。

最新编码崩溃报告中的 `NoClassDefFoundError: PackagePatternItem` 来自正式 `PackageCraftingPatternItem.appendHoverText` 静态转发到未注册遗留类。tooltip 已移入正式物品类，孤儿类及编译产物删除；既有原生 decoder/getOutput GameTest 增加直接调用正式物品 tooltip 并检查四行输出的回归断言。早期并行修改尚未收口时曾有 9 项 Package Assembler/包裹数据断言失败；本次整屏委托完成后在完整当前工作树重新执行 `.\gradlew.bat runGameTestServer --stacktrace`，132/132 required GameTest 全部通过，终端模式、tooltip、Package Assembler、包裹数据与 Sequence Buffer 回归均通过。

包裹整屏不再使用 `PatternEncodingTermScreenMixin`、`ProcessingEncodingPanelMixin`、Render.Pre/Render.Post 条件接管或 `InitScreensMixin`；客户端只保留读取 `MEStorageScreen` 状态、设置包裹 scrollbar 样式和定位包裹槽位的三个窄 accessor。验证必须扫描 mixin 配置与启动日志，确认上述行为 Mixin 不存在，并确认 package-only ScreenStyle 可加载、AE2 原生 ScreenStyle 未被覆盖。项目已删除自动 `runClientSmoke`，本轮不恢复，因此 package tab 的实际快速点击、整屏最终像素、原 AE 侧栏完全消失、右上 current-AE 显示元件面板和 slot 背景仍需在开发客户端人工打开终端复核。

本地 `pattern_encoding_terminal.png` 作为 package-only Screen 的字节保持底图纳入 256x256、固定 SHA-256 与负例资产门禁；`pattern_modes.png` 和对 `assets/ae2/screens/terminals/pattern_encoding_terminal.json` 的覆盖仍禁止，因为四种原生模式继续读取 AE2 自己的资源。资产审计同时要求 `assets/ae2/screens/appliedpackaging/pattern_encoding_terminal.json` 存在、只声明 `modePanel4`、移出旧 view-cell/upgrades 面板并保留绑定 `VIEW_CELL` 的 current-AE 面板。切回原生模式时恢复原 slot 图标和 menu 客户端绑定，禁止包裹资源影响原生模式。v19.2.17 底图、package-only style 与派生绘制代码的 LGPL 来源统一记录在 `META-INF/licenses/ae2-pattern-screen-source.txt`。

### 2026-07-16 包裹终端同 menu 独立 Screen 复核

针对实机截图中的重复右侧按钮、旧五格元件栏和混合 slot 背景，最终实现不再在 AE Screen 上条件叠绘：`PackagePatternScreenDelegate` 自身继承 `MEStorageScreen<PatternEncodingTermMenu>`，点击 package tab 后只替换 `Minecraft.screen`，并继续使用原 menu；原 `PatternEncodingTermScreen` 不参与 package 帧。包裹 Screen 在原生 widget 更新完成后统一重排/重绘左侧 toolbar，style 隐藏旧 view-cell/upgrades 面板并只放置绑定五个真实 `VIEW_CELL` 槽的 current-AE `ModernUpgradesPanel`，进入时清除空白/编码样板、显示元件槽和 marker 的旧图标，退出时恢复。返回原生 tab 前复制 Repo、供电、搜索和网络滚动并恢复 menu `clientRepo/gui` 绑定；中键数量页同样只更换 Screen，不关闭 menu。

`appliedpackaging.mixins.json` 的 client 列表只剩 `MEStorageScreenAccessor`、`ScrollbarAccessor` 与 `SlotAccessor`，没有 Pattern Screen、panel、render-event 或 factory 行为 Mixin。`.\gradlew.bat compileJava --rerun-tasks`、`.\gradlew.bat build --stacktrace`、132/132 required GameTest、`scripts/verify-assets.ps1`、全部资产审计正负夹具、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 通过。真实 `.\gradlew.bat runClient --stacktrace` 到达资源重载、OpenAL 和全部纹理图集创建，日志确认 `PackagePatternScreenDelegate` 已自动订阅 Forge bus，且没有 `InvalidMixin`、`MixinTransformerError`、类加载或缺失资源错误；随后只终止本轮客户端。项目没有自动 UI 驱动，因此点击 package 后的最终像素、快速来回切换和 Encode 连点仍需开发客户端人工复核。

### 2026-07-16 高级样板终端 v19 Part 模型回移

Advanced Pattern Encoding Terminal 的世界 Part 与物品形态已从运行时 AE2 15.4.10 继承模型切换为固定 AE2 `neoforge/v19.2.17` display / Pattern Encoding Terminal 几何。Part 继续使用高版本相同的 `selectModel(off, on, hasChannel)` 三组模型选择路径；通电层的 NeoForge 全亮字段等价改为 Forge 1.20.1 `forge_data`，状态灯使用 v19 四段几何。物品形态显式注册 AE2 `StaticItemColor(AEColor.TRANSPARENT)`，使本模组 PartItem 的全部 tint layer 生效。

用户提供的 dark、medium、bright 三张 16x16 RGBA 遮罩保持原字节并在同一状态同时叠加，不作为三种机器状态。八张 v19 外壳/状态贴图同样保持上游字节；逐文件来源、哈希、模型映射和 LGPL 边界记录在 `META-INF/licenses/ae2-terminal-part-source.txt`。正式全量资产审计和 24 个正/负自测夹具均通过；固定哈希的上游 `monitor_colored.png` 是 AE2 15.4.10 与 v19.2.17 共同使用的透明兼容层，只对该精确路径允许透明，其它透明/纯色占位负例继续失败。

`assetgen render-model` 成功加载完整物品模型并输出四视图几何 sheet；该独立渲染器不执行 Forge item color handler，因此不把预览色彩记作游戏内颜色验收。`gradlew build --rerun-tasks` 强制重编译并重建 JAR；`verify-assets.ps1`、`test-assets-audit.ps1`、`verify-docs.ps1` 与 `verify-release.ps1 -RequireAssetContracts` 通过。真实 `runClient` 到达完整资源重载、OpenAL 初始化和 block atlas 创建，日志没有 missing model/texture、ModelBakery、Mixin、类加载、ERROR 或 FATAL 命中；随后手动终止本次客户端，因此不把世界内放置截图记作已完成。此变更只涉及客户端模型/材质和注册，不改变终端菜单、存档、网络或服务端行为，未重复运行 GameTest。

### 2026-07-16 包裹渲染二次位置回归

用户实机确认上次 GUI `translation [0,3,0]` 已从偏下变成偏上。本轮按 30° 旋转与 0.75 缩放后的投影中心把共享及 marked package 统一收回到 `[0,2,0]`，资源审计固定新值，资产负例确认旧 `[0,3,0]` 会失败。Shift marker 的 8x8 decorator 锚点从右下角移到左下角，避开数量文本；前脸既有 3x3 marker 不变。

ME Packager 内侧端点从 `x=2.5/16` 后移到 `x=1/16`，使出现/消失处的包裹前缘收在 `x=3..4/16` 帘子后方；外侧 `x=10/16` 不变。完整位移和传送带 UV 相位同步改为 9px。现有 GameTest 增加精确 9px 断言，并继续覆盖相位保存往返；`.\gradlew.bat runGameTestServer --stacktrace` 成功，118/118 required tests 全部通过。

`.\gradlew.bat compileJava processResources --stacktrace`、`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过；发布审计确认 217 个发布资源与 JAR 一致、79 个 JSON 可解析、143 张 PNG 非空、5 个资产合同及 140 个双语 key/占位符有效。项目没有能自动操作打包机动画、Shift 键和堆叠数量的客户端 smoke 场景，因此这三处最终像素位置仍需在下一次开发客户端中人工复核。

### 2026-07-16 序列缓存器首版完整验证

序列缓存器已完成五类方块状态、端点权威的水平直线拓扑、尾端自动加入与断裂恢复。端点不保存资源且不计入逻辑第 1 格；存储成员共用一次真实输入锁存语义，并通过 `IItemHandler`、`IFluidHandler` 与 `MEStorage` 暴露同一份 AEKey/数量状态。端点按成员顺序输入并聚合抽取，端点自身不自动输出；成员实现定向/无方向自动输出、阻挡、同步预检和全结构输入延迟。

普通样板及样板包裹保留 sparse 空位，不把后续输入前移；高级样板无视样板模式并按实际非空输入稠密映射。`PackageData` schema 升为 v2，把可选 `PackageLayout` 纳入 NBT 与 canonical identity；Package Unpacking Bus 对序列缓存器使用原子位置计划，失败时不消费 held 包裹且不留下部分成员写入。第一版按既定范围只提供持久化配置和服务端入口，不注册 GUI。

全量 `.\gradlew.bat runGameTestServer` 通过 132/132 required tests，覆盖单格 item/fluid/ME 锁存、NBT 往返、端点聚合、成型/拒绝/扩展/断裂、真实 AE2 扳手“点击面对面 -> 点击面 -> 无方向”三段循环、配置同步、普通 sparse/高级 dense 样板、同步/阻挡/延迟，以及真实拆包总线成功、阻塞重试和提交失败原子性。`.\gradlew.bat build`、`.\gradlew.bat runData`、`scripts/verify-assets.ps1`、`scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts`、`scripts/test-release-audit.ps1` 与 `git diff --check` 通过；发布审计确认 229 个发布资源与 JAR 一致、88 个 JSON 可解析、145 张 PNG、6 个 asset contract 和 141 个双语 key/占位符有效。

真实 `.\gradlew.bat runClient` 到达资源重载、OpenAL 初始化和全部图集创建，日志未命中 sequence_buffer missing model/texture、Unable to load model、ModelBakery、ERROR 或 FATAL；随后只终止本次测试客户端。该结果验证注册与资源烘焙，不替代后续 GUI 阶段或世界内最终美术主观验收。

### 2026-07-16 样板终端载体限制、模式恢复与载入预览回归

普通 Pattern Encoding Terminal 的 encoded-pattern 槽现在拒绝专用 `advanced_processing_pattern`；Advanced Pattern Terminal 继续允许其自身载体。菜单在构造、服务端广播和已编码槽同步后统一从 `PatternEncodingLogic` 投影复合模式，因此关闭时保留 PACKAGE 后重新打开会直接跟随到 package Screen，放入 package pattern 会自动切入 package，放入 AE2 crafting pattern 会自动返回 crafting。包裹预览的模式与颜色直接读取同一 logic 权威状态，消除样板槽已更新但 menu 字段尚未同步时误算原版合成产物的窗口。右上侧栏同时从错误的空 `UPGRADE` 语义改为终端真实的五个 `VIEW_CELL` 槽；旧面板继续在 style 中隐藏，新面板与空槽图标使用 current-AE 资源，不新增 Mixin。

`patternEncodingTerminalPackageInputsSupportAmountEditing` 新增真实普通终端槽位拒绝、package -> crafting -> package 自动模式切换、蓝色包裹内容/marker 载入预览，以及 logic NBT 往返后新 menu 初始 PACKAGE 的断言；`advancedPatternTerminalEncodesDedicatedPattern` 新增专用终端仍接受高级载体的反向断言。`.\gradlew.bat runGameTestServer --stacktrace` 成功，132/132 required GameTest 全部通过；`.\gradlew.bat build --rerun-tasks --stacktrace`、`scripts/verify-assets.ps1`、完整资产正/负夹具、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 通过。当前已有 IntelliJ 开发客户端占用运行目录，本轮未终止用户进程或并发启动第二客户端；Screen 自动切换与五格显示元件侧栏的最终视觉仍需在该客户端重启、加载新 class 后人工复核。

### 2026-07-17 合并高级/包裹终端双 profile 最终验证

本节取代上一节关于普通终端 package Screen、原生 crafting 自动返回和五格显示元件侧栏的当前实现描述。普通 AE2 Pattern Encoding Terminal 现保持原始 Screen/factory/四种模式，只在 encoded-pattern 槽拒绝 `package_pattern` 与 `advanced_processing_pattern`。Advanced Pattern Encoding Terminal 是唯一专用终端，ADVANCED/PACKAGE 在同一个 part/menu/screen 内切换；页面状态持久化、两页槽位完全隔离、两种载体自动切页、包裹直接预览和无 `VIEW_CELL` 槽均由真实菜单 GameTest 覆盖。

客户端按两套完整 profile 验证坐标约束：两行网络库存时高级页 217x250、包裹页 195x233；高级/包裹 bottom 分别为 197/180。高级页保持原 195px 主体并把 22px 模式标签区计入总宽，包裹页直接使用原纹理右侧预留区。包裹 124x66 panel 使用 `left=8,bottom=165`，输入、marker 和小滚动条使用 bottom=158，自动输出保持面板内 `(98,31)`，Encode bottom=145。所有相关坐标按 bottom 计算，网络行数增加时编码区、模式标签与标题整体下移。同一个 Screen 在服务端同步模式改变时执行 resize/init，重新居中并重排背景、搜索、滚动条、玩家栏、载体与当前页控件，不更换 Screen/Menu 或转移槽内物品。右侧模式控件对照 AE2 v19 Pattern Encoding Terminal 的 `TabButton.Style.HORIZONTAL`，使用 22x22 normal/selected/focus 背景、21px 连续步进与 `(3,3)` ItemStack 偏移，不再使用右上角外接 VerticalButtonBar/IconButton。

`.\gradlew.bat compileJava --rerun-tasks --stacktrace`、`.\gradlew.bat runGameTestServer --stacktrace` 和 `.\gradlew.bat build --stacktrace` 成功；132/132 required GameTest 全部通过。`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1` 正/负夹具、`scripts/verify-docs.ps1`、`scripts/test-release-audit.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与 `git diff --check` 全部通过。资产审计新增 package profile 小滚动条 bottom=158 的固定检查及错误值负例。用户当前 IntelliJ 客户端在新 class 编译前已经启动并继续占用 `run/logs`，本轮未终止该进程或并发启动第二客户端；因此最终像素对齐、按钮 hover/selected 状态和快速来回点击仍需重启该客户端后人工复核。

### 2026-07-17 序列缓存器用户贴图拆分与模型接入

用户原图 `E:/resources/textures/appliedpackaging/ret/sequance_buffer_all.png` 是 64x64 RGBA、4x4 严格网格，SHA-256 为 `66A26C07983D8E3CD1866B0D4EE723F2A68B1C257FCD936BCC0C3C57EECF7B8F`。`scripts/split-sequence-buffer-textures.py` 按原始像素边界拆出 16 张 16x16 RGBA 到 `textures/block/sequence_buffer/faces/`，不缩放、重绘、插值或改色，并通过重组像素比较确认与原图完全一致；忽略的 proof sheet 和逐格 SHA-256 manifest 写入 `build/asset-reference/sequence-buffer/user-sheet/`。

全部 16 张面贴图都被实际模型引用：未成型、定向未成型、主方块、中间成员、边缘尾部和定向成员分别使用对应普通/中间/边缘/特殊格。为区分原有五类逻辑外观内的中间/尾部和尾部朝向，blockstate 增加仅用于渲染的 `tail` 与 `sequence_direction`；拓扑扩展时旧尾部会自动转为中间、新末格获得尾部背面。GameTest 在既有成型/扩展/断裂用例中增加尾部迁移和结构方向断言。

`.\gradlew.bat runGameTestServer --stacktrace` 通过 132/132 required tests；`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1` 正/负夹具和 `scripts/verify-release.ps1 -RequireAssetContracts` 通过。发布审计确认 245 个发布资源与 JAR 一致、91 个 JSON 可解析、158 张 PNG 有效、6 个 asset contract 通过且所有模型贴图引用可解析。修改前启动的 IntelliJ 客户端 PID 3220 仍占用运行目录，本轮未终止用户进程或并发启动第二客户端；新贴图最终世界内像素效果需在该客户端重启后人工复核。

### 2026-07-17 序列缓存器六向拓扑、方向保持与模型朝向修正

序列缓存器结构轴从仅 X/Z 扩展为 X/Y/Z，`facing` 与 `directional` 只表达每个方块自己的扳手方向，结构顺序单独使用 `sequence_direction`。成型、尾端扩展、拓扑重建和解体都不再改写本地方向；已有方向与结构轴平行时只隐藏带箭头的成员外观，方向值继续保留并在解体后恢复显示。旧存档中仅有 directed visual state、缺少新方向标记的方块会补回标记，同时保持原 `facing` 不变。单块和成员对上下/水平合法侧继续执行“点击面对面 -> 点击面 -> 无方向”三段循环。

`scripts/generate-sequence-buffer-models.py` 依据 Minecraft 1.20.1 每个面的 UV 基向量生成 57 个完整方块模型和 58 个 multipart 条目，覆盖六个未成型方向、六个端点方向、X/Y/Z 中段、六个尾部方向及所有垂直于结构轴的定向成员组合。方向侧面贴图的 `+U` 箭头统一旋转到方块自身正面；中段侧面 `+V` 沿结构正轴，边缘/尾部侧面 `+V` 朝主方块、`-V` 封口朝结构外。直接模型渲染已解析真实 JSON/PNG，并复核 north 单块、Y 轴定向中段及向上尾部；资产门禁同时新增缺失 Y 轴模型负例。

验证结果：`.\gradlew.bat compileJava --stacktrace`、`.\gradlew.bat runGameTestServer --stacktrace` 和 `.\gradlew.bat build --stacktrace` 通过，135/135 required GameTest 全部成功。新增测试覆盖单块上下方向、竖直成型/扩展、端点和成员方向保持、平行方向隐藏后恢复、旧 directed 状态迁移，以及断裂解体后端点仍保留原方向。`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1` 与 `scripts/verify-release.ps1 -RequireAssetContracts` 通过；发布审计确认 295 个发布资源与 JAR 一致、141 个 JSON 可解析、158 张 PNG、6 个 asset contract 和 143 个双语 key 有效。IntelliJ 开发客户端 PID 3204 仍是修改前启动的进程，本轮没有终止或并发启动第二客户端；它必须重启后才会载入新的 Java 状态属性和模型资源。

### 2026-07-17 合并终端统一尺寸与 sprite 模式图标验证

本节取代上方“双 profile”关于 217x250/195x233、切页 `resize/init` 和 ItemStack 模式图标的当前实现描述。用户更新后的 `adv-pattern-terminal-base.png` 与 `pattern_mode_packaging.png` 使高级/包裹两页统一为两行网络库存时 195x245、bottom 192px；同一个 Screen/Menu 切页时不再 resize 或重建 widget。基础图高级面板和模式图集包裹面板均为 screen `(8,68)` 的 132x78 区域；切页只替换该区域、当前页 widget 状态和完全隔离的输入/输出槽坐标。

右侧标签固定为 `x=173`，从网络区结束后 6px 开始，以 21px 步进排列：包裹在上，高级在下。两个标签仍使用 AE2 v19 水平 `TabButton` 的 22x22 normal/selected/focus 背景，但内容完全由 sprite 绘制，不渲染物品栈：包裹图标读取本地 sprite `[32,0,16,16]`，高级图标读取 states atlas 的 processing/furnace `[16,32,16,16]`，两者均按水平标签偏移 `(3,2)` 绘制。离线合成预览确认两页的上下顺序、选中背景和图标裁片一致。

`.\gradlew.bat compileJava --rerun-tasks --stacktrace` 已强制重编译通过；`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1` 正/负夹具与 `scripts/verify-docs.ps1` 通过。资源审计固定两张用户 PNG 的 SHA-256，并断言统一 frame、修正后的高级槽位、包裹滚动条、载体槽和 Encode 坐标。本轮只改变客户端底图分段、widget/slot geometry、模式标签 sprite 与同尺寸切页生命周期，不改变菜单 action、状态持久化、编码数据或服务端行为，因此不新增或重复运行 GameTest；最终游戏内缩放、hover 与点击命中仍需在重启开发客户端后人工复核。

### 2026-07-17 JEI 通用高级/包裹适配与 StarT Fork 验证

Advanced Pattern Encoding Terminal 的唯一 universal transfer handler 现按当前页面选择目标。标准 fallback 依照 JEI 常规 role，只消费 INPUT、读取确定性 OUTPUT 并跳过 CATALYST / RENDER_ONLY；高级页生成单列，包裹页展平到最多 81 项并选第一个确定性物品输出为 marker。Create Sequenced Assembly 继续按步骤列展开；Mechanical Crafting 裁去空边后比较非空行/列数量，选择更少包裹的方向并保持组内网格顺序，平局按行。GTCEu 专用映射继续支持 item/fluid 与 tick content，同时修正不同 content 间的区间倍率泄漏。

源代码语义审计覆盖 Mekanism、Immersive Engineering、Thermal、Botania、PneumaticCraft、Ars Nouveau、Industrial Foregoing 和 Ender IO。公开概率、结果池、爆炸损耗、动态世界/权重产出、reagent NBT 保留等不能被样板精确表达的 recipe 在 client action 前保守拒绝；Thermal 的额外 INPUT catalyst 按 recipe 公开的真实 item/fluid 输入数量配额跳过。若第三方把工具错误标为 INPUT 且没有公开语义，或只在 tooltip 隐藏概率，仍需对具体 recipe 增加窄规则。

验证结果：

- `.\gradlew.bat runGameTestServer --stacktrace`：上游 GTCEu 7.5.3 组合 142/142 required tests 通过。

- `& .\gradlew.bat 'runGameTestServer' '-PgtceuRuntimeJar=build/reference/compat-src/gtceu-st-1.20.1-1.7.0b.jar' '--stacktrace'`：StarT Fork 1.7.0b 组合 142/142 通过。

- Fork 日志确认 `GAMEDIR=run-gtceu-fork`、实际选中 gtceu-st 1.7.0b，且没有 version differences、missing id from the world 或 Unidentified mapping。

- `.\gradlew.bat build --stacktrace` 与 `.\gradlew.bat runData --stacktrace` 通过。

- `.\gradlew.bat runClient --stacktrace`：JEI 15.20.0.134、Create 6.0.8-291、GTCEu 7.5.3 与 Applied Packaging 完成资源重载、OpenAL、block atlas 和 JEI GUI atlas 创建后主动终止。

- `scripts/test-release-self-tests.ps1`、`scripts/verify-assets.ps1`、`scripts/verify-docs.ps1` 与 `scripts/verify-release.ps1 -RequireAssetContracts` 通过；机械审计确认 295 个发布资源、141 个 JSON、158 张 PNG、6 个资产合同和 157 个双语 key。

- `git diff --check` 通过，仅输出现有 LF/CRLF 提示。

- JAR 包含两种 transfer plan、JEI plugin、通用/Create/GTCEu adapter；`mods.toml` 中 JEI/Create/GTCEu 硬依赖行数为 0。

客户端启动验证覆盖注册、类加载和资源初始化，不等同于世界内交互。JEI “+”按钮在高级页/包裹页的实际点击、拒绝 tooltip 和导入后的槽位/颜色/marker 最终显示仍是人工验收项。

### 2026-07-17 序列缓存器尾部与物品栏模型视觉回归

用户截图触发两项资源回归修正。边缘/尾部第三列贴图的 `+V` 开口现在始终朝主方块、`-V` 封口朝外；普通 east/up 尾部及带方向 east/up 尾部均有固定 JSON 断言，反转 east 尾部角度的负例会被拒绝。序列缓存器物品仍直接引用未成型方块模型，但公共外壳新增 `minecraft:block/block` 父模型，从而继承原版 GUI `[30,225,0]` 旋转、`0.625` 缩放以及手持/地面变换，不再显示为正面的平面方格；删除父模型的负例会被资产自测拒绝。

验证通过：`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、`scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts`。本次不改变 Java、方块状态或服务端事务，GameTest 不重复运行；客户端需资源重载或重启后才会丢弃已烘焙的旧物品模型。

### 2026-07-17 合并终端槽位 sprite 与列头边界验证

高级编辑区不再以纯色填充输入/输出槽。动态启用的输入列使用 AE2 v19 states atlas `[192,192,18,18]` 的完整 `SLOT_BACKGROUND` 精灵并按 `(slot.x-1,slot.y-1)` 对齐；未启用列和输出槽保留用户底图。像素检查确认该精灵的 256 个不透明像素与底图首槽一致，两张用户 GUI PNG 与运行时副本 SHA-256 未变化。列头颜色/清空/循环按钮为 `bottom=174`、列操作按钮为 `bottom=173`，其底边均严格结束在 y=80 输入框顶边前；资产审计加入旧 `bottom=172` 必须失败的负例。

`.\gradlew.bat compileJava --rerun-tasks --stacktrace`、`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、`scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1` 和 `git diff --check` 通过。真实 `.\gradlew.bat runClient --stacktrace` 完成初始化、资源重载、OpenAL 和全部图集创建；日志仅有既知第三方 Ponder/Xaero/ModernFix optional-integration 缺类警告，没有 Applied Packaging 类加载、missing model/texture、ERROR 或 FATAL。该修正不改变服务端行为，GameTest 不重复运行；实际打开终端后的像素观感仍为人工客户端验收项。

### 2026-07-18 序列缓存器双界面、预留过滤配置与红石卡验证

序列缓存器新增两套独立菜单和 Screen：右键成型端点打开高版本 ME Chest terminal 风格主界面，端点本身不生成存储槽，后续存储成员按序映射到 3x9 可视窗口；成员或未成型单块打开 ME Chest 侧面风格界面，中央唯一存储槽只映射被点击本格。主界面成员数不超过 27 时滚动范围为 0，但 disabled handle 仍显示；超过 27 后按整行滚动。窗口内超过实际成员数的位置继续绘制槽框但以 0.2 alpha 禁用。第一版两套界面均不显示仍处于预留阶段的输入过滤和模式设置控件，右侧只提供一格红石卡升级面板。

红石卡注册为序列缓存器一格升级。成型时成员携带的卡会迁移到端点，避免成员升级库存被主界面隐藏；端点有卡时，整组自动输出只在端点收到红石信号时执行，并继续受阻挡、同步与输入延迟约束。预留 `inputFilter` 与端点权威配置仍随 NBT 保存，但菜单不创建 `CONFIG` 假槽。物品光标可直接插入/抽取物品 AEKey；流体和其它 AEKey 仍通过既有 FluidHandler/MEStorage 路径进入，并能在通用显示槽中显示。

本轮撤销可见预留配置 UI 后，两次 `.\gradlew.bat runGameTestServer --stacktrace --no-configuration-cache` 均确认全部序列缓存器测试通过；完整套件为 145/146，唯一失败是当前并行配方导入改动中的 `generic_recipe_transfer_rejects_random_output_definitions`，不属于序列缓存器 GUI 范围。序列测试覆盖预留过滤配置与 NBT、红石卡单槽上限和 NBT 往返、成员卡成型迁移、无信号禁止自动输出、有信号释放输出、侧面存储槽在输入屏障后 Shift 抽取，以及 27/28/36/37 个成员的滚动边界。`.\gradlew.bat compileJava processResources --rerun-tasks --stacktrace --no-configuration-cache`、`.\gradlew.bat build --stacktrace --no-configuration-cache`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1` 与 `scripts/verify-docs.ps1` 通过；资源负例固定用户主图哈希、两张 GUI 尺寸、主界面 scrollbar 矩形、disabled handle UV、右侧升级面板、禁止出现预留配置 UI 和侧界面中央槽坐标。用户客户端占用 `run/logs/latest.log` / `debug.log`，本轮不并发启动第二个客户端；最终世界内槽位像素、滚轮和点击手感由现有客户端资源重载或重启后验收。

### 2026-07-18 高级样板扩容、单插件双查看器与逐材料分包验证

高级样板逻辑容量从 17 列提高到 81 列，每列保留 81 个 sparse 输入位置；持久化 schema 升为 v2，按列保存输入并兼容读取 v1 展平数据，AE2 根 `in` 继续保存供执行使用的稠密输入。菜单只创建并同步 4x81 个窗口 FakeSlot，横向滚动映射到 81 个逻辑列；装配室按列元数据生成包裹，既有 17 个实体输出槽后的包裹进入持久 pending 队列，不再把输出槽数当作高级样板列上限。

终端新增转置、空手长按拾起后移动/交换、末尾新增列自动滚动，以及高级页独有的默认色/循环颜色模式。转置按钮使用用户提供 sprite 的 8x8 裁片并位于清空按钮下方。GameTest 覆盖 v1/v2 往返、81 列末端数据、转置、移动/交换、循环色、通用物品/流体逐材料分包与 GT 多材料逐列分包；客户端启动覆盖 sprite/Screen 类加载与资源烘焙，世界内长按手感、按钮像素和滚动行为仍保留为人工验收。

查看器兼容只维护一套 JEI 插件与 recipe transfer handler：Gradle 只编译 JEI API，`mods.toml` 不声明 JEI、EMI 或 TMRV 硬依赖。`recipeViewerRuntime=jei` 直接加载 JEI；`recipeViewerRuntime=emi` 加载 EMI+TMRV，由 TMRV 在没有 JEI 的环境中提供兼容 API，并把同一套 `AppliedPackagingJeiPlugin` 映射到 EMI；项目不再发布原生 EMI handler，也不测试 JEI+EMI/JEMI 共存。通用配方、Create 普通 processing 与 GTCEu item/fluid/tick content 默认每个不同输入各占一个包裹；Create Sequenced Assembly 保留步骤列，Mechanical Crafting 继续按行/列选择较少包裹的方向。

验证结果：

- 上游 GTCEu 7.5.3：`.\gradlew.bat runGameTestServer --offline --no-configuration-cache`，147/147 required tests 通过。
- StarT Fork 1.7.0b：`& .\gradlew.bat 'runGameTestServer' '-PgtceuRuntimeJar=build/reference/compat-src/gtceu-st-1.20.1-1.7.0b.jar' '--offline' '--no-configuration-cache'`，147/147 required tests 通过。
- `.\gradlew.bat build --offline --no-configuration-cache`、`scripts/verify-assets.ps1`、`scripts/verify-docs.ps1`、`scripts/test-release-self-tests.ps1` 与 `scripts/verify-release.ps1 -RequireAssetContracts` 全部通过；发布审计确认 297 个发布资源、143 个 JSON、160 张 PNG、6 个资产合同和 164 个双语 key/占位符有效。
- JEI-only 真实客户端通过 Quick Play 进入隔离 smoke 世界，日志确认 `Registered JEI advanced recipe transfer for the Advanced Pattern Encoding Terminal.`，证明同一 handler 在 JEI 下直接注册。
- EMI+TMRV 真实客户端命令行确认没有完整 JEI、同时存在 EMI 与 TMRV；Quick Play 进入隔离 smoke 世界后，TMRV 日志把 `appliedpackaging:jei_plugin` 列入 JEI plugins，按 `SYNC_EMI` 分发 `registerRecipeTransferHandlers`，并记录该插件在 17ms 内加载完成。Forge 会在客户端反射发现 GameTest holder，因此带 JEI 类型的测试体已隔离到无注解 helper；修正夹具反射可见性后，两套 147/147 GameTest 再次通过。日志中的 GTCEu synthetic recipe/tag 警告来自 GTCEu 侧，不涉及 Applied Packaging 插件异常。

Star Technology 的星门部件装配由 KubeJS startup script 注册 GTCEu `stargate_component_assembly` recipe type，配方通过 `layeredRecipe` 表达层级。现有 GT adapter 能读取其平面 `GTRecipe` 输入，但不能保留 layer 边界；该项在确认“每层一个包裹”或“层内逐材料分包并额外保存层边界”前不实现，也不提前增加泛 KubeJS 接口。

### 2026-07-18 高级样板 sparse 包裹到序列缓存器回归

高级样板的逐列 sparse 元数据现在会进入装配室生成包裹的 `PackageLayout`；序列缓存器的包裹入口仅在样板模式开启时应用该布局，关闭时保持连续输入。回归用例使用真实 v2 高级样板“木板 + 空白 + 木板”，覆盖装配室 Pattern Provider push、包裹 NBT、ME 网络插入、Package Unpacking Bus 动画提交和序列成员最终状态，并以关闭样板模式的反向用例固定“不跳过空位置”的行为。高级样板直接推给序列缓存器的稠密输入例外保持不变。

验证结果：`.\gradlew.bat compileJava compileTestJava --stacktrace`、`.\gradlew.bat build --stacktrace` 和 `scripts/verify-docs.ps1` 成功；`.\gradlew.bat runGameTestServer --stacktrace` 成功，147/147 required tests 全部通过；`git diff --check` 通过且仅输出工作树既有 LF/CRLF 提示。

### 2026-07-18 装配室与打包机新版槽位渲染回归

装配室输入格不再允许 AE2 15 `AEBaseScreen` 通过 `IOptionalSlot.isRenderDisabled=true` 在用户 atlas 之后叠加旧 `Icon.SLOT_BACKGROUND`。菜单固定返回 false，Screen 统一从 `package-storagebus-sprites.png [0,64,18,18]` 绘制 current-AE2 透明边框槽位；透明外围保留 atlas 的外框和分隔线，禁用格仅把 sprite 降到 0.2 opacity。ME Packager 可选过滤槽同步改用同一完整 sprite，删除两块纯色 fill 的近似重建。装配室小滚动柄集中到共用 style，disabled UV 固定为 `[16,32,7,15]`。Sequence Buffer 仍按其用户底图轨道保留 12x15 `Scrollbar.DEFAULT`，不套用 7px 小滚动柄。

验证通过：`.\gradlew.bat compileJava`、`.\gradlew.bat build --stacktrace`、`scripts/verify-assets.ps1`、完整 `scripts/test-assets-audit.ps1`、`scripts/verify-docs.ps1` 与 `scripts/verify-release.ps1 -RequireAssetContracts`。资产门禁固定用户装配室 atlas 的 SHA-256，并断言共用槽位/滚动柄 UV、禁止 `isRenderDisabled=true`、禁止 `Icon.SLOT_BACKGROUND`、禁止 ME Packager 纯色槽位；同尺寸替换 atlas 和恢复旧可选槽位渲染的隔离负例均按预期失败。发布审计确认 297 个发布资源、143 个 JSON、160 张 PNG、6 个资产合同和 179 个双语 key/占位符有效。变更只影响客户端渲染选择，不改变机器事务或服务端状态，因此不重复运行 GameTest。IntelliJ 开发客户端 PID 33220 是修改前启动的 JVM，本轮没有终止或并发启动第二客户端；必须重启后再进行装配室边框最终像素复核。

### 2026-07-18 配方导入对齐 AE2 样板终端的现存材料优先级

高级/包裹配方导入不再让 JEI 当前动画帧或专用适配器声明首项无条件决定替代材料。`RecipeIngredientSelector` 在每次 transfer 检查/导入时读取高级终端当前 AE2 client repository 与玩家物品栏，并复刻 AE2 15.4.10 `EncodingHelper` 的顺序：网络条目整体高于玩家物品栏；网络内部依次比较 craftable、undamaged、stored amount；库存均无匹配才回退 JEI 显示项或 recipe 声明首项。物品 `Ingredient` 同时允许 client repo 中满足 `AEItemKey.matches` 的实际 NBT/损伤变体参与选择，流体在配方声明的候选中按同一 AEKey 优先级选择。标准 JEI、Create Sequenced Assembly/Mechanical Crafting/ProcessingRecipe 和 GTCEu 一次性/tick 输入共用该选择器；输出确定性规则没有放宽。

现有 `standardRecipeTransferUsesJeiRolesForBothPatternModes` GameTest 已扩展，覆盖 item/fluid 现存候选覆盖 JEI 展示候选、同类网络材料取现存数量更多者、玩家物品栏后备和专用适配器 raw Ingredient 共用选择规则。`.\gradlew.bat compileJava --stacktrace` 与 `.\gradlew.bat build --offline --no-configuration-cache --stacktrace` 通过；上游 GTCEu 7.5.3 的 `.\gradlew.bat runGameTestServer --offline --no-configuration-cache --stacktrace` 与 StarT Fork 1.7.0b 的同任务隔离运行均为 148/148 required tests。`scripts/verify-docs.ps1`、`scripts/verify-release.ps1 -RequireAssetContracts` 与完整 `scripts/test-release-self-tests.ps1` 通过，发布审计确认 297 个发布资源、143 个 JSON、160 张 PNG、6 个资产合同和 179 个双语 key/占位符有效。Fork 日志确认 `GAMEDIR=run-gtceu-fork` 且实际选中 `gtceu-st-1.20.1-1.7.0b.jar`。上游运行开始时用户既有客户端仍占用 `run/logs/latest.log` / `debug.log`，仅导致 Log4j 文件轮换警告；控制台 GameTest 正常完成，本轮未停止用户客户端。

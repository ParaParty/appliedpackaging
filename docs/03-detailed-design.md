# 详细设计

## 1. 包裹颜色

颜色枚举：

```text
fluix
white
orange
magenta
light_blue
yellow
lime
pink
gray
light_gray
cyan
purple
blue
brown
green
red
black
```

注册物品 ID：

```text
fluix_package
white_package
orange_package
...
black_package
```

颜色权威来自 item id。`PackageData` 中不重复保存颜色，只在 canonical hash 输入中附带颜色值，避免不同颜色的相同内容误堆叠。

## 2. 包裹数据

```java
public record PackageData(
    int version,
    List<PackageEntry> contents,
    Optional<MarkerSpec> marker,
    long usedUnits,
    int usedTypes,
    String canonicalHash,
    int flags
) {}

public record PackageEntry(
    AEKey key,
    long amount
) {}

public record MarkerSpec(
    AEKey key,
    long amount
) {}
```

1.20.1 NBT 结构：

```text
tag.appliedpackaging.package.version: int
tag.appliedpackaging.package.flags: int
tag.appliedpackaging.package.hash: string
tag.appliedpackaging.package.used_units: long
tag.appliedpackaging.package.used_types: int
tag.appliedpackaging.package.marker: compound optional
tag.appliedpackaging.package.contents: list<compound>
```

每个 content compound：

```text
key_type: string
key: compound
amount: long
```

实现注意：

```text
AE2 ItemKey 可通过 item registry id + item NBT/components 表达。
AE2 FluidKey 在 1.20.1 需要保存 fluid registry id + NBT。
未知 AEKey 类型必须保守保存 type id 和 opaque payload。
如果无法还原，则包裹标记为 invalid，不允许拆包提交。
```

## 3. Canonical Hash

Canonical hash 输入：

```text
schema version
package item color
flags
marker canonical form
contents sorted by:
  key type id
  registry id
  normalized key payload hash
  amount
```

Hash 算法：

```text
SHA-256 -> hex lowercase
tooltip 显示前 8 位
NBT 保存完整 64 位 hex
```

用途：

```text
包裹堆叠判断
快速过滤缓存 key
调试 tooltip
GameTest 断言
```

## 4. 堆叠规则

包裹物品的 `isSameItemSameTags` / Forge 等价判断必须包含完整 NBT，因此同色、同 marker、同 contents、同 version、同 flags 的包裹才能自然堆叠。

包裹 ItemStack count 语义：

```text
stack count = 包裹个数
PackageData.contents = 每一个包裹的内容
```

拆包一组包裹时必须按 count 重复单包事务，不允许把一个包裹的数据乘 count 后作为一个大事务提交。

## 5. 容量模型

容量单位：

```text
item: ceil(amount / maxStackSize)
fluid: ceil(amount / 1000 mB)
other AEKey: adapter 提供单位，未知类型默认 amount=1 为 1 unit
```

容量档：

| 档位 | 单包单位上限 | 类型上限 |
| --- | ---: | ---: |
| default | 9 | 9 |
| 16k | 16 | 16 |
| 64k | 64 | 63 |
| 256k | 256 | 63 |

容量元件识别：

```text
AE2 16k Storage Component / Cell -> 16k 档
AE2 64k Storage Component / Cell -> 64k 档
AE2 256k Storage Component / Cell -> 256k 档
```

最终实现以 AE2 1.20.1 注册名为准，先匹配 AE2 item registry id，再 fallback 到 tag。

## 6. 包裹套包裹

包裹不允许真实嵌套。任何进入打包计划的包裹都先展开为虚拟内容。

合并：

```text
红色包裹：铁锭 x64
蓝色包裹：铜锭 x64
机器颜色：Fluix

结果：
Fluix 包裹：铁锭 x64, 铜锭 x64
```

改色：

```text
红色包裹：铁锭 x64
机器颜色：蓝色

结果：
蓝色包裹：铁锭 x64
```

追加：

```text
红色包裹：铁锭 x64
散装：金锭 x64

结果：
红色或机器设定色包裹：铁锭 x64, 金锭 x64
```

Marker 策略：

```text
retain:
  所有源包裹 marker 相同则保留。
  marker 冲突则拒绝计划。

override:
  使用机器 ghost marker。

clear:
  输出包裹无 marker。
```

默认：`retain`。

## 7. ME 包裹装配室

职责：

```text
接收 Pattern Provider 推入的一批输入
读取普通/彩色/包裹/封装处理样板
生成一个或多个包裹
维护本机输入缓存与输出缓存
阻挡模式
可选把输出口包裹导入相连 AE 网络
```

不做：

```text
相邻库存打包
拆包
扫描所在 AE 网络库存
扫描相邻 Interface 子网
执行 ME 打包机逻辑
```

方块实体状态：

```text
inputBuffer: GenericStackBuffer
outputSlots: 17 item slots，只允许合法包裹
patternSlot: 本地包裹样板/封装处理样板
capacitySlot: 可选容量元件
defaultColor: PackageColor
defaultMarker: MarkerSpec optional
blockingMode: boolean
autoExportToNetwork: boolean
lastFailure: enum/string
```

普通处理样板：

```text
输入 A+B+C，输出 X
装配室生成 1 个 Fluix/default 包裹，内容为 A+B+C
AE2 自动合成仍然等待 X
```

彩色处理样板：

```text
普通处理样板 + 输入格颜色元数据
按输入格颜色分组生成多个包裹
颜色跟随样板输入格，不跟随 AEKey
同一种 AEKey 位于两个颜色格时，必须生成两个不同颜色的包裹
```

阻挡模式：

```text
如果 outputSlots 中存在任意合法包裹：
  拒绝新的输入/新样板执行
```

阻挡模式不检查输出 AE 网络、包裹子网、相邻接口、目标机器或主网库存。

UI：

```text
左侧：样板槽、本地输入缓存、容量元件槽
中间：包裹计划预览和彩色分组
右侧：17 格输出口
下方：默认颜色、default marker、阻挡模式、自动导入 AE 网络、状态文本
```

## 8. ME 打包机

职责：

```text
识别相邻存储端点
识别相邻 ME Interface 背后的存储子网
从端点生成包裹
把输入包裹拆入端点
包裹展开再封装
容量、过滤、marker、颜色策略
红石/按钮触发
```

不做：

```text
读取 Pattern Provider
读取/执行任何样板
扫描自己所在任意 AE 网络
充当 AE 网络设备提供库存
```

方块实体状态：

```text
inputSlot: 只允许合法包裹
outputSlot: 只允许合法包裹
capacitySlot
targetSide: Direction
packageColor
markerMode
overrideMarker
contentFilter
packageFilter
redstoneMode
sortMode
strictWholeEndpointMode
lastEndpointInfo
lastFailure
```

端点接口：

```java
interface GenericStorageEndpoint {
    List<PackageEntry> listAvailable();
    long simulateExtract(PackageEntry entry, long amount);
    long extract(PackageEntry entry, long amount);
    long simulateInsert(PackageEntry entry, long amount);
    long insert(PackageEntry entry, long amount);
    EndpointCapabilities capabilities();
}
```

端点类型：

```text
Forge item handler
Forge fluid handler
AE2 ME Interface adjacent subnet
未来扩展：其他 AEKey endpoint
```

打包触发：

```text
GUI Pack Once 按钮
红石脉冲 Pack Once
红石持续周期 Pack，默认关闭
```

打包事务提交前必须全部模拟：

```text
1. 端点抽取所有源内容可行
2. 源包裹作为输入可被完整消耗
3. 输出槽可接收生成包裹
4. marker 策略无冲突
5. 容量与类型数未超限
```

任何一步失败都不得改变端点、输入槽或输出槽。

拆包事务：

```text
accepted = 0
while stack.count > 0:
  if filter fails: break
  if target cannot accept every entry: break
  commit target insert for one package
  stack.shrink(1)
  accepted++
```

单个包裹永远不能拆一半。

## 9. 样板与终端

包裹样板物品 ID：`package_pattern`

包裹样板数据：

```text
PackageColor
MarkerSpec optional
List<PackageEntry>
CapacityProfile
```

彩色处理样板：

```text
slotColor[0..8] = PackageColor
```

彩色处理样板不改变 AE2 的输入输出，只在 pattern NBT 中写入输入格颜色数组。普通机器忽略这段数据；装配室读取它。

封装处理样板物品 ID：`packaged_processing_pattern`

封装处理样板数据：

```text
vanilla/AE2 processing pattern payload
List<PackagePlan>
flattenedInputsHash
processingOutputs
```

终端 tab：

```text
Package Pattern:
  17 色选择、marker ghost、GenericStack ghost slots、容量档、编码

Colored Processing Pattern:
  普通处理样板输入格、每格颜色、处理输出、编码

Packaged Processing Pattern:
  多个包裹样板、处理输出、空白样板、编码

Split:
  封装处理样板、空白样板若干、拆出包裹样板，可选普通处理样板副本
```

## 10. 包裹总线

包裹存储总线：

```text
只枚举相邻库存中的合法包裹
过滤不匹配的包裹不可见
插入时只允许合法包裹
不拆包
```

包裹输出总线：

```text
从 AE 网络取已有包裹
按颜色/marker/内容过滤输出到相邻库存
不把散装物品打成包裹
不请求自动合成
不拆包
```

包裹拆包总线：

```text
网络尝试插入包裹
总线过滤包裹
展开包裹内容
模拟完整插入相邻目标
成功才接受包裹
提交后目标得到散装内容
```

## 11. 过滤规则

包裹过滤维度：

```text
颜色
marker
内容物
```

三者 AND。未设置的过滤项忽略。内容过滤只决定整包是否通过，绝不只处理包裹的一部分内容。

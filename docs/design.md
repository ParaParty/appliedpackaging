# Applied Packaging 设计文档入口

本文是 Applied Packaging / 应用封装的文档入口，不再承载完整设计正文。完整内容按文档种类拆分在同目录下；开发、实现、材质、测试时请读取对应文档。

## 当前定案

```text
目标版本：Minecraft 1.20.1 Forge
工具链：ModDevGradle Legacy
Forge 基线：47.4.10 recommended
AE2 基线：Applied Energistics 2 15.4.10 Forge
开发运行依赖：GuideME 20.1.7
Mod ID：appliedpackaging
Java 包名：com.warmthdawn.appliedpackaging
Mod 名称：Applied Packaging / 应用封装
```

## 文档分类

| 文档 | 类型 | 用途 |
| --- | --- | --- |
| [00-document-index.md](00-document-index.md) | 文档索引 | 说明文档体系、阅读顺序和维护规则 |
| [01-requirements.md](01-requirements.md) | 需求文档 | 玩家目标、功能需求、非功能需求、范围边界 |
| [02-system-architecture.md](02-system-architecture.md) | 概要设计 | 模块划分、架构原则、关键流程和版本适配 |
| [03-detailed-design.md](03-detailed-design.md) | 详细设计 | 包裹数据、机器、样板、总线和事务规则 |
| [04-asset-spec.md](04-asset-spec.md) | 资产规格 | 材质、模型、UI 图标、颜色表和资源验收 |
| [05-implementation-plan.md](05-implementation-plan.md) | 实施计划 | 阶段拆分、里程碑、交付顺序和风险 |
| [06-verification-release.md](06-verification-release.md) | 验证与发布 | JVM 测试、GameTest、客户端/服务端验证和发布标准 |
| [07-references.md](07-references.md) | 参考来源 | 外部版本、AE2 语义和工具链来源 |
| [chat-summary.md](chat-summary.md) | 讨论记录 | 保留历史命名、美术和玩法讨论，不作为实现源文件 |
| [development-log.md](development-log.md) | 开发日志 | 记录阶段性决策、命令、结果和下一步 |

AI/agent 相关操作规则不放在设计文档中，统一放在仓库根目录 [AGENTS.md](../AGENTS.md)。

## 核心边界

```text
ME 包裹装配室：
  处理 AE2 样板语义和彩色分包。
  不扫描相邻存储，不拆包。

ME 打包机：
  处理相邻存储端点与包裹互转。
  可识别相邻 ME Interface 背后的存储子网。
  不读取 Pattern Provider，不执行样板。

包裹总线家族：
  只允许包裹通过或事务拆包。
  不把包裹内部内容伪装成散装库存。

包裹：
  17 色独立物品。
  无空包裹玩法。
  可堆叠。
  不真实嵌套；包裹套包裹时展开后再封装。

样板：
  AE2 原版 blank pattern 可作为 package_pattern 数据载体。
  当终端需要多包裹计划时，AE2 原版 blank pattern 也可作为 packaged_processing_pattern 数据载体。
  当终端配置处理输出 ghost 时，AE2 blank pattern 会编码为 AE2 原版 processing pattern，并附带 packaged_processing_pattern NBT。
  本地 package_pattern / packaged_processing_pattern 仅保留兼容读取，不作为玩家主入口、创造栏条目或普通合成输出。
```

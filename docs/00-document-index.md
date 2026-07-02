# 文档索引

本目录把 Applied Packaging 的产品设计、工程设计、资产规范、实施计划和验证发布分开维护。任何后续修改都应落到对应文档类型中，避免把操作规则、开发日志和设计正文混在一起。

## 阅读顺序

1. [01-requirements.md](01-requirements.md)
2. [02-system-architecture.md](02-system-architecture.md)
3. [03-detailed-design.md](03-detailed-design.md)
4. [04-asset-spec.md](04-asset-spec.md)
5. [05-implementation-plan.md](05-implementation-plan.md)
6. [06-verification-release.md](06-verification-release.md)
7. [07-references.md](07-references.md)

`design.md` 只作为入口和当前定案摘要。`chat-summary.md` 是讨论记录，不应被当成最新实现规格。仓库级 agent 操作入口是根目录 `AGENTS.md`。

## 文档职责

需求文档回答：

```text
为什么做
给谁用
做什么
不做什么
验收时哪些功能必须存在
```

概要设计回答：

```text
系统分成哪些模块
模块如何交互
关键数据流是什么
版本适配怎么隔离
```

详细设计回答：

```text
数据结构是什么
机器状态是什么
事务如何模拟和提交
样板/总线/过滤如何工作
```

资产规格回答：

```text
视觉风格是什么
需要哪些材质和模型
颜色表是什么
资源路径和验收标准是什么
```

实施计划回答：

```text
先做什么
每个阶段交付什么
哪些风险需要前置验证
```

验证与发布回答：

```text
哪些测试必须跑
哪些手动检查必须做
发布 jar 满足什么标准
```

参考来源回答：

```text
版本选择和 AE2 语义来自哪里
哪些外部来源需要重新核实时更新
```

## 维护规则

1. 不把开发过程流水账写进需求/架构/详细设计；这些内容写入 `development-log.md`。
2. 不在 `chat-summary.md` 继续扩展正式规格；讨论结论应迁移到对应分类文档。
3. 版本、依赖、工具链发生变化时，同时更新 `design.md` 摘要、`01-requirements.md`、`07-references.md` 和 `development-log.md`。
4. 新增资产时更新 `04-asset-spec.md`；新增验证任务时更新 `06-verification-release.md`。

# AGENTS.md

本文件只定义 agent 工作规则。产品设计以 `docs/design.md` 为唯一真源，验证门禁见 `docs/verification.md`。

## 项目基线

```text
mod_id: appliedpackaging
package: com.warmthdawn.appliedpackaging
Minecraft: 1.20.1
Forge: 47.4.10+
AE2: 15.4.10 Forge
GuideME: 20.1.7
Java: 17
toolchain: ModDevGradle Legacy
```

## 工作规则

1. 开始前检查 `git status --short`，保护用户已有改动。
2. 修改产品行为前读取 `docs/design.md`；选择验证前读取 `docs/verification.md`。
3. 以当前代码、GameTest 和可复现运行结果为事实依据，不恢复已删除的历史设计或兼容壳。
4. 新需求直接更新 `docs/design.md` 的对应模块；不要新增实施计划、变更接收表、聊天摘要、开发日志、资产报告或重复索引。
5. 验证规则只更新 `docs/verification.md`；不要在文档中累计逐次命令结果、构建哈希、截图记录或发布状态。
6. 资源来源说明只保留必要的运行时 license 文件；概念图、参考 sheet 和临时导入产物放在忽略的 `build/` 下。
7. 行为敏感变更必须考虑现有 GameTest；客户端 UI/渲染变更必须走真实 `runClient` 路径。
8. 每次完成后运行与风险匹配的最窄验证，并执行 `git diff --check`。

## 常用验证

```powershell
.\gradlew.bat compileJava --stacktrace
.\gradlew.bat runGameTestServer --stacktrace
.\gradlew.bat runData --stacktrace
.\gradlew.bat build --stacktrace
.\gradlew.bat runClient --stacktrace
.\gradlew.bat runServer --stacktrace
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-docs.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-assets.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1
```

`build` 不替代 GameTest 或客户端验证。没有运行适用门禁时，应在交付说明中明确原因，不新增流水账文档。

## 禁止事项

- 不把包裹内容伪装成 AE2 散装库存。
- 不让 ME 打包机读取 Pattern Provider 或执行样板。
- 不让 ME 包裹装配室扫描相邻存储或拆包。
- 不创建玩家可获得的空包裹。
- 不实现真实包裹嵌套。
- 不恢复 Package Export Bus、独立 Package Pattern Terminal 或开发版 NBT/注册表迁移。
- 不把 agent 协作、工具使用或阶段流水写进产品设计。

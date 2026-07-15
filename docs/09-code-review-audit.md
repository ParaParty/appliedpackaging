# 整体代码审查与剩余问题

本文记录 2026-07-13 全仓代码审查的结论。项目尚未正式发布，因此不承诺开发版存档、旧注册表 ID、旧 NBT schema 或旧样板载体兼容；已经被当前设计替代的自有实现直接删除，不设置兼容期限。

## 1. 本轮已修复

- 删除三个旧 Package Bus 方块/方块实体实现。Package Storage Bus 与 Package Unpacking Bus 只保留 AE2 cable part；已取消的 Package Export Bus 不再注册方块、方块实体或资源。
- 删除独立 Package Pattern Terminal 的方块、方块实体、part、菜单、screen 和资源。包裹样板入口只保留 AE2 原版 Pattern Encoding Terminal 包裹模式，高级处理入口只保留 Advanced Pattern Encoding Terminal part。
- 删除旧 `packaged_processing_pattern` 物品、模型、语言、注册与数据存储；其图标资源改名为当前 `advanced_processing_pattern`。
- 删除旧 AE2 `crafting_pattern` 包裹载体、`colored_processing_pattern` / `packaged_processing_pattern` 扩展 NBT 解码，以及 AE2 blank pattern 兼容载体。正式载体只保留本 Mod `package_pattern`、AE2 普通 processing pattern 和本 Mod `advanced_processing_pattern`。
- 删除 Package Assembler 的 9 格隐藏输入、旧槽位布局迁移、旧 `auto_export` NBT 和旧多样板执行分支。继续删除失去所有执行语义的颜色按钮、marker fallback 槽及其同步/NBT；当前外部 item capability 按 AE2 分子装配室语义暴露样板过滤输入与严格有序输出，不恢复无样板自由输入。
- 删除 ME Packager 的旧 output/filter 槽迁移、旧 held-box 状态推断，以及 `DISABLED` / `CYCLIC` 旧红石枚举归一化。
- 删除 PackageEntity 的旧 `Package` NBT key 和高级终端旧输入迁移。
- 删除上述路径对应的失效 GameTest；保留并更新当前三种正式样板语义、整包模拟/提交、真实 AE2 网络和客户端人工验收记录。
- 删除已取消功能对应的无引用资源与测试，发布审计新增旧注册、旧数据存储和机器兼容路径的回归门禁。
- 两台包裹机器的 16k / 64k / 256k storage component 容量升级确定为正式范围；空槽为 9 单位/9 类型，1k component、完整 storage cell、portable cell、4k 与附属容量档均不做。
- 删除没有运行时调用的 item handler 打包规划、Forge fluid handler 打包/拆包适配、旧 Package Export/即时拆包操作及其测试。卸货总线的 item handler 路径只保留整包累计模拟与 Pattern Provider 式 check-then-push，不再生成逐槽提交计划或反向抽取回滚。
- 容量单位累计和整叠手动拆包增加 `long` 溢出保护；包裹 contents 不执行同类合并。
- 用用户提供的正式空心框架模型、双周期传送带和四条动态帘子替换 ME Packager 的临时 Create 外壳；旧 `me_packager_create` 模型、贴图和 hatch/tray renderer 适配已删除。

## 2. 发布前兼容政策

当前版本发布前不保留自有旧实现的兼容读取或迁移：

```text
旧注册表 ID：删除
旧机器槽位与 NBT key：删除
旧样板物品与扩展 NBT：删除
旧开发存档迁移：不提供
```

`PackageDataStorage` 仍作为 1.20.1 NBT 持久化边界存在，因为它承载当前数据模型，不是旧存档兼容层。正式发布后若产生兼容承诺，应先定义 schema/version 和支持期限，再引入迁移代码。

## 3. 仍需决策或后续实现的问题

本轮代码审查列出的临时实现问题均已有正式迁移目标；最终是否发布仍以 `docs/08-change-intake.md` 的范围冻结和 `docs/06-verification-release.md` 的完整候选发布门禁为准。

## 4. 明确不处理的警告与当前实现

- Forge/Minecraft 1.20.1 API 的 deprecation/removal 编译警告属于目标平台常见状态，本审查不据此改写生命周期或渲染路径。
- AE2 current-main UI 的 1.20.1 回移层是当前视觉规格实现，不是旧功能兼容壳。

## 5. 后续收口顺序

1. 对正式 ME Packager 模型执行资源审计、GameTest，并按视觉风险人工运行客户端检查动画。
2. 在新增范围冻结后重新执行完整 release candidate 门禁。

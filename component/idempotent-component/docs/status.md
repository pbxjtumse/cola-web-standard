# 当前状态

> 基线：V1.5。本文只记录当前状态，不再保留逐版本 Changelog 文档。

## 1. 已完成

- `WINDOWED` / `DURABLE` 两种幂等生命周期；
- `IdempotencyRequest` 与 `IdempotencyPolicy` 分离；
- `IdempotencyOptions` 已删除；
- 主 Executor API 不需要 `Class<T>`；
- ResultPolicy：`NONE / SNAPSHOT / REFERENCE`；
- Repository capabilities；
- JDBC：UNIQUE / row lock / owner-version CAS；
- Redis：Lua 原子状态机；
- 显式 StateMachine；
- optional short DistributedLock；
- ownerToken + generationVersion stale owner 拒绝；
- Reliable Recovery：扫描候选 + expectedOwner/version 二次 CAS；
- Transaction Integration：Tx-A / Tx-B / Tx-C；
- `COMMIT_UNKNOWN` 特殊语义；
- requestHash / routeKey；
- WINDOWED：processingTimeout / idempotencyWindow / recordRetentionTtl 分离；
- API/Core/Provider 已按职责分包。

## 2. 当前默认原则

```text
Repository CAS/Lua = 正确性核心
DistributedLock    = 竞争收敛优化
StateMachine       = 原子结果解释器
Transaction        = 同库 Business + SUCCESS 原子性
ResultPolicy       = SUCCESS 结果回放
Recovery           = 外部可靠任务显式接管
```

## 3. 尚未内置

- 扫描调度中心 / Reliable Task Scheduler；
- 跨库分布式事务；
- Redis + DB 原子事务；
- 银行 / HTTP / MQ 外部副作用自动补偿；
- 自动 Saga / TCC / Outbox；
- 生产管理后台 / 人工重放平台。

这些属于其他组件或业务层能力，不应直接塞进 idempotent-core。

## 4. 兼容代码

代码中仍存在少量 deprecated 兼容符号，例如：

```text
IdempotencyMode.SHORT_TERM
IdempotencyResultCodec
部分旧属性访问器
```

它们不属于当前推荐用法。文档只使用 `WINDOWED`、ResultPolicy 和当前 Policy API。

## 5. 当前验证状态

已做过 Java 17 静态编译 / import / POM XML / ZIP 完整性检查。

生成环境未完整执行 Maven Reactor，因此最终发布前仍应：

```bash
mvn clean test
```

重点验证：

```text
Redis WINDOWED
JDBC DURABLE
owner/version stale write
Recovery stale candidate
Tx-B rollback + Tx-C FAILED
COMMIT_UNKNOWN
NONE / SNAPSHOT / REFERENCE
```

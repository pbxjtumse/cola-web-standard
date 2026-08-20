# 当前状态

> 本文只记录当前实现状态。

## 1. 已完成

- `WINDOWED` / `DURABLE` 两种幂等生命周期；
- `IdempotencyRequest` 与 `IdempotencyPolicy` 分离；
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
- API/Core/Provider 已按职责分包；
- 已移除全部幂等组件 历史兼容入口与旧结果编解码协议。

## 2. 当前 API 原则

```text
Repository CAS/Lua = 正确性核心
DistributedLock    = 竞争收敛优化
StateMachine       = 原子结果解释器
Transaction        = 同库 Business + SUCCESS 原子性
ResultPolicy       = SUCCESS 结果回放
Recovery           = 外部可靠任务显式接管
```

当前只保留一套正式 API：

```text
WINDOWED / DURABLE
IdempotencyPolicy
IdempotencyResultPolicy
policyName recovery query
generationVersion
```

公共 API 只保留当前正式入口；result_payload 只接受当前 envelope 协议。

## 3. 尚未内置

- 扫描调度中心 / Reliable Task Scheduler；
- 跨库分布式事务；
- Redis + DB 原子事务；
- 银行 / HTTP / MQ 外部副作用自动补偿；
- 自动 Saga / TCC / Outbox；
- 生产管理后台 / 人工重放平台。

这些属于其他组件或业务层能力，不应直接塞进 idempotent-core。

## 4. 当前验证状态

已执行源码级兼容符号扫描、Java 17 静态编译、内部 import、POM XML 与 ZIP 完整性检查。

最终发布前仍建议在完整 Maven 环境执行：

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

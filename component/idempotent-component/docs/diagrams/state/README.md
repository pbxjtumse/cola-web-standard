# Idempotent State Diagram

`00-idempotency-full-state.puml` 是 `idempotent-component v2 — Shard-Ready Storage` 的唯一权威状态图。

本图合并并替代此前拆分的 persisted / decision / lifecycle / recovery / windowed 状态图，避免多个状态图分别演进后出现语义漂移。

阅读时必须区分五类概念：

```text
[PERSISTED]   IdempotencyStatus：真正写入 JDBC / Redis 的状态
[DERIVED]     由持久记录、时间和 failureRetryable 计算出的判定状态
[REPO RESULT] Repository.tryAcquire / tryRecover / markXxx 的原子操作结果
[CALL RESULT] IdempotencyExecutor / IdempotencyOperations 返回给调用方的一次调用结果
[DOC ONLY]    仅用于解释生命周期的概念节点
```

V2 真正的持久状态只有：

```text
PROCESSING
SUCCESS
FAILED
DISCARDED
```

`PROCESSING_EXPIRED`、`FAILED_RETRYABLE`、`REPLAYED`、`STALE_CANDIDATE` 等都不是数据库 `status`。

完整图同时覆盖：普通 `execute()`、owner/version CAS、Tx-B/Tx-C 完成语义、Reliable Recovery 二次 CAS、WINDOWED generation rollover、`DISCARDED`、Shard-Ready `storeName/shardKey/scanBucket` 以及低层 `IdempotencyOperations` 状态投影。

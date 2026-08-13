# Idempotent Component V1.1 Design

## 1. 三层状态模型

### 持久状态

```text
PROCESSING
SUCCESS
FAILED
```

### Repository 判定状态

```text
ACQUIRED
SUCCESS
PROCESSING_ACTIVE
PROCESSING_EXPIRED
FAILED_RETRYABLE
FAILED_FINAL
KEY_CONFLICT
PROVIDER_ERROR
```

### Executor 最终结果

```text
EXECUTED / RECOVERED / REPLAYED
PROCESSING / PROCESSING_EXPIRED
PREVIOUS_FAILED_RETRYABLE / PREVIOUS_FAILED_FINAL
RECOVERY_NOT_ALLOWED / STALE_RECOVERY_CANDIDATE
KEY_CONFLICT / OWNERSHIP_LOST / ...
```

这样避免把“数据库状态”“当前判断”“本次调用结果”混在一个 enum 里。

## 2. 普通执行

普通 `execute()` 永远不会把 `PROCESSING_EXPIRED` 自动接管成新的 PROCESSING。

## 3. 可靠恢复

`recoveryMode=EXTERNAL_TASK` 时：

1. 外部 Reliable Task 调用 `IdempotencyRecoveryQueryService` 查询候选；
2. 持久化/投递恢复任务；
3. 任务执行时携带扫描时看到的 owner/version；
4. 调用 `IdempotencyExecutor.recover()`；
5. Repository 原子校验 candidate 是否仍有效；
6. 新 owner 接管，version+1；
7. 旧扫描任务会得到 `STALE_CANDIDATE`。

## 4. routeKey

routeKey 只是路由元数据，不由幂等组件决定具体分片算法。

推荐：

```text
routeKey       = merchantId / userId / orderShardKey
idempotencyKey = operation + requestId
requestHash    = canonical business fingerprint SHA-256
```

## 5. 事务边界

当前 JDBC Provider 默认仍是 DataSource 直接连接；V1.1 只增加 `JdbcExecutionManager` 扩展点。

未来 Transaction Template 需要提供 transaction-aware 实现，才能实现：

```text
Business writes + markSuccess = 同一事务
```

而 `tryAcquire / tryRecover` 应继续保持独立短事务。

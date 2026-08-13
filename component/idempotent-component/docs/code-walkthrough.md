# Code Walkthrough - V1.1

建议按以下顺序阅读：

1. `IdempotencyOptions`
   - processingTimeout / idempotencyWindow / recordRetentionTtl / recoveryMode；
2. `IdempotencyRequest` / `IdempotencyRecoveryRequest`
   - key / requestHash / routeKey / stale candidate guard；
3. `IdempotencyAcquireStatus`
   - PROCESSING_ACTIVE 与 PROCESSING_EXPIRED；
4. `DefaultIdempotencyExecutor`
   - execute() 与 recover() 两条显式主流程；
5. `JdbcIdempotencyRepository`
   - UNIQUE、短事务、SELECT FOR UPDATE、owner+version CAS；
6. `RedisIdempotencyRepository` + Lua
   - SHORT_TERM 固定/滑动窗口与绝对过期时间；
7. `JdbcExecutionManager`
   - 当前 DataSource 默认实现与未来 transaction-component 接口；
8. `IdempotencyRecoveryQueryService`
   - 为 Reliable Task 提供候选查询，但不内置扫描器；
9. `JacksonSha256IdempotencyRequestHasher`
   - canonical JSON + SHA-256。

V1.1 仍刻意避免把每个私有步骤抽成独立 Service。真正的扩展点才单独成类：Repository、Recovery Query、Hasher、FailureClassifier、ResultCodec、JdbcExecutionManager。

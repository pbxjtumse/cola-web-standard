# V1 代码阅读顺序

建议按下面顺序阅读，不要从 Starter 开始：

1. `idempotent-api/IdempotencyStatus`
   - 先理解 PROCESSING / SUCCESS / FAILED 三个持久状态。
2. `idempotent-api/IdempotencyOptions`
   - 区分 `processingTimeout` 与 `recordTtl`。
3. `idempotent-api/repository/IdempotencyRecord`
   - 重点看 `ownerToken + version + processingExpireAt`。
4. `idempotent-api/repository/IdempotencyRepository`
   - 理解 Repository 的原子契约，这是正确性根基。
5. `idempotent-core/DefaultIdempotencyExecutor`
   - 看 `prepare -> acquireState -> callback -> markSuccess/markFailed` 主流程。
6. `idempotent-provider-jdbc/JdbcIdempotencyRepository`
   - 看 Unique Key、`SELECT ... FOR UPDATE`、owner/version 条件更新。
7. `idempotent-provider-redis/try-acquire.lua`
   - 看 SHORT_TERM 如何在一个 Lua 中完成读/判定/状态转换。
8. `idempotent-provider-redis/mark-success.lua` / `mark-failed.lua`
   - 看旧 owner 如何被拒绝。
9. `idempotent-starter/IdempotencyAutoConfiguration`
   - 最后看 Spring 如何把 Repository、DistributedLockClient、Codec、事件和指标装配起来。

## 为什么没有拆更多类

V1 刻意没有创建 `AcquireService / CompletionService / TimeoutRecoveryService / ResultReplayService` 等大量小类。
`DefaultIdempotencyExecutor` 保留可读的主编排；真正变化较大的边界通过 SPI 抽出：

- `IdempotencyRepository`
- `IdempotencyFailureClassifier`
- `IdempotencyResultCodec`
- `IdempotencyEventPublisher`
- `IdempotencyMetrics`
- `IdempotencyOwnerTokenGenerator`

这样避免“为了抽而抽”，同时保留未来扩展能力。

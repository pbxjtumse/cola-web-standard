# V1.1 代码注释阅读指南

本版本只增强代码注释、源码排版和阅读说明，不改变 V1.1 的设计语义。

建议按照下面顺序阅读：

1. `idempotent-api/IdempotencyStatus`：先区分持久状态与派生状态。
2. `idempotent-api/IdempotencyOptions`：理解 processingTimeout、idempotencyWindow、recordRetentionTtl、recoveryMode。
3. `idempotent-api/IdempotencyContext`：理解 ownerToken、version、fencingVersion、routeKey。
4. `idempotent-api/repository/IdempotencyRepository`：理解正确性为什么在 Repository，而不是 DistributedLock。
5. `idempotent-core/DefaultIdempotencyExecutor`：看正常 execute 与 recover 两条主流程，以及为什么短锁只包 tryAcquire/tryRecover。
6. `idempotent-provider-jdbc/JdbcIdempotencyRepository`：看 UNIQUE、短事务、FOR UPDATE、owner/version CAS。
7. `idempotent-provider-jdbc/JdbcExecutionManager`：这是下一步 Transaction Template 的衔接点。
8. `idempotent-provider-redis/RedisIdempotencyRepository` 和四个 Lua：看 SHORT_TERM 状态机如何原子执行。
9. `idempotent-starter/IdempotencyAutoConfiguration`：最后看 Spring 如何把 API/Core/Provider 组装起来。

## 阅读时重点记住的四条边界

- `DistributedLockClient` 是可选并发协调层，不是幂等正确性根基。
- `PROCESSING` 要在业务开始前通过独立短事务/原子 Lua 对其他节点可见。
- `markSuccess` 未来要和业务写放进同一个业务事务；当前 V1.1 只预留 `JdbcExecutionManager`，还没有完成这块集成。
- 扫描、调度、MQ 投递属于 Reliable Task；幂等 Core 只提供 `recover()` 和原子恢复能力。

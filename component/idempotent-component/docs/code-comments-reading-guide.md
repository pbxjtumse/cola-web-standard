# V1.3 代码注释阅读指南

本版本已经完成 Transaction Integration，并进一步完成 API Cleanup。

阅读时重点记住六条边界：

1. `Repository CAS/Lua` 是幂等执行权的正确性根基；
2. `DistributedLockClient` 只做短临界区热点竞争收敛；
3. `StateMachine` 在 Repository 原子操作之后解释结果，不取代 CAS；
4. `Tx-A` 让 PROCESSING 尽快独立可见；
5. `Tx-B REQUIRED` 把本地 Business + ResultPolicy.capture + SUCCESS 放到同一事务；
6. Recovery 由外部 Reliable Task 驱动，Core 不创建扫描线程。

推荐阅读顺序：

```text
IdempotencyMode
IdempotencyPolicy
IdempotencyResultPolicy
IdempotencyRepository
Jdbc/Redis Provider
DefaultIdempotencyStateMachine
DefaultIdempotencyExecutor
Transaction Integration
Recovery Query Service
Starter
```

如要理解整体时序，优先看：

```text
v1.3-architecture.md
result-replay-and-result-policy.md
transaction-boundary.md
recovery-and-routing.md
```

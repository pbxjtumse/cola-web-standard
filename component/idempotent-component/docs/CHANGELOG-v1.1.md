# V1.1 - Recovery / Window / Routing Refinement

- 普通 execute() 不再 Lazy Recovery；
- 新增 recover()，仅供显式 Reliable Task 恢复；
- PROCESSING_ACTIVE / PROCESSING_EXPIRED 拆为判定状态；
- FAILED_RETRYABLE / FAILED_FINAL 拆为判定状态；
- 持久状态仍只有 PROCESSING / SUCCESS / FAILED；
- 新增 recoveryMode=NONE / EXTERNAL_TASK；
- 新增恢复候选查询 SPI，不内置调度线程；
- 新增 routeKey 全链路透传与持久化；
- 新增 expectedOwnerToken + expectedVersion，拒绝过期恢复任务；
- `recordTtl` 拆为 `idempotencyWindow + recordRetentionTtl`；
- 新增 FIXED / SLIDING 窗口策略；
- Redis 使用绝对 `windowExpireAt / retentionExpireAt` 与 `PEXPIREAT`；
- 新增 requestHash SPI：Canonical JSON + SHA-256；
- JDBC 新增 JdbcExecutionManager，为后续 transaction-component 事务参与预留；
- 状态图拆层，避免把持久状态、判定状态、执行结果画在一张混乱图里。

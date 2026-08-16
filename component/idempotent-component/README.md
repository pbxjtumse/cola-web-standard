# idempotent-component V1.2

定位：统一管理幂等状态、普通执行判定、可靠恢复原语，以及 SHORT_TERM / DURABLE 两类存储语义；V1.2 正式接入 `transaction-component`，让 JDBC DURABLE 模式可以形成“状态抢占、业务提交、失败落状态”三个清晰的本地事务边界。

## 核心原则

1. `IdempotencyRepository` 仍然是幂等正确性核心，分布式锁与事务模板都不是唯一正确性来源；
2. `DistributedLockClient` 仅是可选的短临界区并发收敛，callback 永远在分布式锁外执行；
3. JDBC DURABLE + transaction-component 时采用 `Tx-A / Tx-B / Tx-C`：
   - Tx-A `REQUIRES_NEW`：抢占/恢复 `PROCESSING` 并独立提交；
   - Tx-B `REQUIRED`：业务 callback 与 `markSuccess` 参加同一个本地事务；
   - Tx-C `REQUIRES_NEW`：Tx-B 失败后独立写 `FAILED`；
4. `markSuccess` 如果因为 owner/version 已失效而更新 0 行，必须让 Tx-B 回滚，不能允许“业务已提交但 SUCCESS 被拒绝”；
5. `COMMIT_UNKNOWN` 不写 `FAILED`，因为业务与 SUCCESS 可能已经一起提交，也可能一起回滚；记录保持 `PROCESSING`，由后续查询/恢复收敛；
6. 普通 `execute()` 不自动接管超时 PROCESSING；外部 Reliable Task 通过 `findCandidates + recover()` 恢复；
7. 持久状态仍只保留 `PROCESSING / SUCCESS / FAILED`；`PROCESSING_EXPIRED`、`FAILED_RETRYABLE` 等是判定状态；
8. SHORT_TERM 同时区分 `idempotencyWindow` 和 `recordRetentionTtl`；
9. `routeKey` 贯穿状态记录，为分库分表与恢复任务预留；
10. 本地事务闭环只解决“同一个本地事务资源”上的原子性，跨库、跨服务与外部副作用仍需下游幂等、事务消息、Saga/TCC 等机制。

## 模块

```text
idempotent-component
├── idempotent-api
├── idempotent-core
├── idempotent-provider
│   ├── idempotent-provider-redis
│   └── idempotent-provider-jdbc
├── idempotent-integration-transaction
├── idempotent-starter
├── idempotent-demo
└── docs
```

其中 `idempotent-integration-transaction` 是可选集成层：

- `TransactionTemplateIdempotencyTransactionCoordinator`：把 Tx-B 映射到 `TransactionExecutor(REQUIRED)`；
- `SpringTransactionJdbcExecutionManager`：使用 Spring `DataSourceUtils` 取得当前事务绑定 Connection，并通过 `TransactionExecutor(REQUIRES_NEW)` 执行 Tx-A / Tx-C；
- `idempotent-core` 不直接依赖 Spring 事务实现，只依赖自己的最小事务协调 SPI。

## 两类模式

### SHORT_TERM

- Redis Hash + Lua；
- 固定或滑动幂等窗口；
- 物理 Retention 与语义 Window 分离；
- 默认 `recoveryMode=NONE`；
- Redis Repository 不参与 JDBC 本地业务事务，因此不会启用 Tx-B 原子闭环。

### DURABLE

- JDBC；
- UNIQUE + `SELECT FOR UPDATE` + owner/version CAS；
- 默认 `recoveryMode=EXTERNAL_TASK`；
- transaction-component 可用时，业务写与 `markSuccess` 通过 Tx-B 保证同一事务提交/回滚；
- 为订单、支付、结算、重要消息消费等长期业务幂等设计。

## V1.2 推荐事务流程

```text
请求
  ↓
Tx-A REQUIRES_NEW
tryAcquire / tryRecover
PROCESSING 落库并提交
  ↓
可选 DistributedLock 短临界区结束
  ↓
Tx-B REQUIRED
业务 callback
  +
markSuccess(owner + version CAS)
  ↓
成功：一起 COMMIT
失败/owner 失效：一起 ROLLBACK
  ↓
若 Tx-B 失败
Tx-C REQUIRES_NEW
markFailed
  ↓
返回最终 IdempotencyResult
```

注意：`REQUIRED` 可能加入调用方已经存在的外层事务。因此 `transactionApplied=true` 表示“业务与 SUCCESS 已参加同一个事务边界”，不等于它一定已经在 `IdempotencyExecutor.execute()` 返回前物理提交。

## 事务配置

```yaml
xjtu:
  iron:
    idempotent:
      transaction:
        enabled: true
        require-template: false
```

- `enabled=true`：存在 `TransactionExecutor` 且 JDBC Repository 支持当前事务参与时启用 V1.2 事务闭环；
- `require-template=true`：应用明确要求 transaction-component 必须存在，否则启动失败。支付、结算等强一致本地场景建议生产环境开启。

## 推荐阅读顺序

1. `docs/transaction-boundary.md`
2. `docs/CHANGELOG-v1.2.md`
3. `docs/short-critical-section-vs-full-lock.md`
4. `docs/window-and-retention.md`
5. `docs/recovery-and-routing.md`
6. `docs/diagrams/state/*`
7. `docs/diagrams/sequence/*`

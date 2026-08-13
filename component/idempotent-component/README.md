# idempotent-component V1.1

定位：统一管理幂等状态、普通执行判定、可靠恢复原语，以及 SHORT_TERM / DURABLE 两类存储语义。

## 核心原则

1. `IdempotencyRepository` 是幂等正确性核心；
2. `DistributedLockClient` 仅是可选的短临界区并发收敛；
3. 普通 `execute()` 不自动接管超时 PROCESSING；
4. 外部 Reliable Task 通过 `findCandidates + recover()` 恢复；
5. 持久状态仅 `PROCESSING / SUCCESS / FAILED`；
6. `PROCESSING_EXPIRED`、`FAILED_RETRYABLE` 等是判定状态，不写进数据库 status；
7. SHORT_TERM 同时区分 `idempotencyWindow` 和 `recordRetentionTtl`；
8. `routeKey` 贯穿状态记录，为分库分表与恢复任务预留；
9. JDBC 通过 `JdbcExecutionManager` 为未来 Transaction Template 事务参与预留。

## 模块

```text
idempotent-component
├── idempotent-api
├── idempotent-core
├── idempotent-provider
│   ├── idempotent-provider-redis
│   └── idempotent-provider-jdbc
├── idempotent-starter
├── idempotent-demo
└── docs
```

## 两类模式

### SHORT_TERM

- Redis Hash + Lua；
- 固定或滑动幂等窗口；
- 物理 Retention 与语义 Window 分离；
- 默认 `recoveryMode=NONE`。

### DURABLE

- JDBC；
- UNIQUE + `SELECT FOR UPDATE` + owner/version CAS；
- 默认 `recoveryMode=EXTERNAL_TASK`；
- 为订单、支付、结算、重要消息消费等长期业务幂等设计。

## 推荐阅读顺序

1. `docs/short-critical-section-vs-full-lock.md`
2. `docs/window-and-retention.md`
3. `docs/recovery-and-routing.md`
4. `docs/transaction-boundary.md`
5. `docs/diagrams/state/*`
6. `docs/diagrams/sequence/*`

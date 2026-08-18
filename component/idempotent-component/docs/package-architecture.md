# 分包结构

## 1. 原则

按稳定职责分包，但不为了目录漂亮把主流程拆成大量 Manager / Handler。

`DefaultIdempotencyExecutor` 保持主编排器，方便直接阅读：

```text
prepare
→ lock
→ tryAcquire / tryRecover
→ StateMachine
→ execute / replay / return
→ transaction
→ final CAS
```

## 2. API

```text
com.xjtu.iron.idempotent.api
├── execution
├── policy
├── recovery
├── result
├── state
├── repository
│   ├── acquire
│   ├── recovery
│   └── write
└── spi
```

主要职责：

- `execution`：Executor / Request / Context / Result；
- `policy`：WINDOWED / DURABLE、窗口、锁等稳定策略；
- `recovery`：显式 Recovery 公共协议；
- `result`：NONE / SNAPSHOT / REFERENCE；
- `repository.acquire`：普通请求原子抢占协议；
- `repository.recovery`：可靠任务二次 CAS 协议；
- `repository.write`：SUCCESS / FAILED 条件写协议；
- `spi`：Hasher、FailureClassifier 等扩展点。

`IdempotencyOptions` 已删除，Request 只选择 `policyName` 或 inline `IdempotencyPolicy`。

## 3. Core

```text
com.xjtu.iron.idempotent.core
├── execution
├── policy
├── repository
├── recovery
├── owner
├── failure
├── state
├── result
├── transaction
└── observation
```

- `execution`：主流程编排；
- `policy`：Policy Registry；
- `repository`：Repository Registry；
- `recovery`：Recovery Query Service；
- `owner`：ownerToken 生成；
- `failure`：失败分类；
- `state`：纯 StateMachine；
- `result`：StoredResultEnvelope；
- `transaction`：与 transaction-component 解耦的最小 SPI；
- `observation`：Event / Metrics。

## 4. JDBC Provider

```text
provider.jdbc
├── execution
│   ├── JdbcExecutionManager
│   ├── DataSourceJdbcExecutionManager
│   └── JdbcWork
└── repository
    └── JdbcIdempotencyRepository
```

`execution` 负责 Connection / 当前事务参与；`repository` 负责 UNIQUE、row lock、CAS 和幂等 SQL。

## 5. Redis Provider

```text
provider.redis
├── key
│   └── RedisIdempotencyKeyBuilder
└── repository
    └── RedisIdempotencyRepository
```

Lua 位于 resources，Repository 负责脚本调用与结果映射。

## 6. Transaction Integration

```text
integration.transaction
├── TransactionTemplateIdempotencyTransactionCoordinator
└── SpringTransactionJdbcExecutionManager
```

前者负责 Tx-B REQUIRED；后者负责 Tx-A / Tx-C REQUIRES_NEW 以及 transaction-bound JDBC Connection。

## 7. Starter

Starter 负责：

```text
properties
Repository 注册
Policy 注册
transaction-aware JDBC 组装
Jackson SnapshotPolicyFactory
hash / observation
```

Starter 不承载领域 StateMachine。

## 8. 依赖方向

```text
Business
   ↓
idempotent-api
   ↓
idempotent-core
   ↓
Repository SPI
   ├── JDBC Provider
   └── Redis Provider

idempotent-core
   ├── optional distributed-lock-api
   └── transaction SPI
          ↑
          └── idempotent-integration-transaction
                    ↓
             transaction-component
```

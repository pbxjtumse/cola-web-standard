# Transaction Component 最终路线

## L1：当前实现

目标：显式、安全、可理解的本地事务执行模板。

完成：

- TransactionExecutor
- TransactionOptions
- REQUIRED / REQUIRES_NEW / MANDATORY
- isolation / timeout / readOnly
- rollbackOnly
- TransactionStage
- COMMIT_UNKNOWN
- Spring Provider
- Starter
- MyBatis XML Demo
- JPA Demo
- executionId SPI

停止：不继续加治理能力。

## L2：事务治理

- Named Transaction Policy
- TransactionPolicyRegistry
- 多 PlatformTransactionManager 选择
- RouteKey 在事务开始前确定
- afterCommit / afterRollback / afterCompletion
- Metrics / Trace / slow transaction
- 配置中心

## L3：可靠性集成

### Retry + Transaction

```text
Retry
  ↓
Transaction Tx-1
失败 rollback
  ↓
Retry
  ↓
Transaction Tx-2
```

完整事务单元重试，不在一个已经失败的事务内部反复重试 SQL。

### Idempotency + Transaction

- Atomic 模式
- Durable PROCESSING 模式
- COMMIT_UNKNOWN 最终状态确认

### Outbox + Transaction

```text
Local Tx
├── business data
└── outbox event
```

### Concurrency + Transaction

线程池在外，TransactionExecutor 在线程任务内部。

## L4：高级能力

- NESTED / Savepoint
- Reactive
- Advanced Provider
- 事务诊断与规范检查

## Distributed Transaction

永远独立：

```text
transaction-component
= Local Transaction

distributed-transaction-component
= Saga / TCC / Seata / XA（未来按真实需求建设）
```

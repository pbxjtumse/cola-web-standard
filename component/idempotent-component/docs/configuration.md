# Configuration - V1.2

```yaml
xjtu:
  iron:
    idempotent:
      enabled: true
      default-mode: DURABLE
      default-short-term-repository: redis
      default-durable-repository: jdbc
      processing-timeout: 30s
      store-result: false

      short-term:
        idempotency-window: 10m
        window-policy: FIXED_FROM_FIRST_ACQUIRE
        record-retention-ttl: 0s
        recovery-mode: NONE

      durable:
        recovery-mode: EXTERNAL_TASK
        recover-failed: true

      lock:
        enabled: true
        provider-name: redisson
        wait-time: 0s
        lease-time: 5s
        fallback-to-state-on-failure: true

      transaction:
        enabled: true
        require-template: false

      redis:
        enabled: true
        key-prefix: iron:idempotency

      jdbc:
        enabled: true
        table-name: iron_idempotency_record
```

基础设施连接继续使用：

```yaml
spring:
  data:
    redis: ...
  datasource: ...
```

transaction-component 自身的 Provider、事务管理器与其他配置仍由 transaction-component / Spring 容器负责；幂等组件只通过 `TransactionExecutor` 组合，不复制事务 Provider 配置。

## transaction

### `enabled`

默认 `true`。

当以下条件同时满足时，JDBC DURABLE 模式启用 V1.2 的 Tx-A / Tx-B / Tx-C：

1. 存在 `TransactionExecutor` Bean；
2. 当前 `IdempotencyRepository` 声明支持业务事务参与；
3. `transaction.enabled=true`。

如果没有 TransactionExecutor，且 `require-template=false`，组件会退回 V1.1 兼容行为，不阻止应用启动。

### `require-template`

默认 `false`。

设为 `true` 后，如果 JDBC 幂等已经启用但容器中没有 `TransactionExecutor`，Starter 直接启动失败，避免支付、结算等场景误以为“业务写 + SUCCESS”已经在同一事务内。

生产中的强一致本地事务场景建议：

```yaml
xjtu:
  iron:
    idempotent:
      transaction:
        enabled: true
        require-template: true
```

## Tx-A / Tx-B / Tx-C

```text
Tx-A REQUIRES_NEW
  tryAcquire / tryRecover -> PROCESSING -> commit

Tx-B REQUIRED
  business callback + markSuccess -> commit / rollback together

Tx-C REQUIRES_NEW
  markFailed after Tx-B failure -> independent commit
```

`Tx-B` 的 `REQUIRED` 允许加入调用方已有外层事务。如果确实需要“IdempotencyExecutor 返回前必须完成独立物理提交”，不能仅依赖 REQUIRED，需要由业务层重新定义更高层事务边界。

## JDBC 与 DataSource 限制

`business callback` 与 `JdbcIdempotencyRepository.markSuccess()` 必须命中同一个可参与的本地事务资源（通常是同一个 Spring `DataSource` / 同一路由后的物理数据库）。

以下情况不属于 V1.2 本地事务能力覆盖范围：

- 业务 SQL 在数据库 A，幂等表在数据库 B；
- 业务 callback 调用远程 HTTP/RPC；
- callback 内发送无法与数据库原子提交的外部消息；
- 第三方支付、短信、发券等已经产生外部副作用。

这些仍需要下游幂等、Outbox/事务消息、Saga/TCC、对账补偿等机制。

## COMMIT_UNKNOWN

transaction-component 如果报告 `COMMIT_UNKNOWN`，幂等组件返回：

```text
IdempotencyResultStatus.TRANSACTION_COMMIT_UNKNOWN
```

此时不会执行 Tx-C 把状态写成 FAILED。原因是数据库提交结果已经不确定：

- 可能业务 + SUCCESS 一起提交；
- 也可能一起回滚。

错误地写 FAILED 反而可能覆盖真实成功事实。后续重复请求先读状态；如果最终仍停留 PROCESSING，再按 `processingTimeout + recover()` 机制收敛。

## 短期窗口策略

固定窗口：

```yaml
window-policy: FIXED_FROM_FIRST_ACQUIRE
```

滑动窗口：

```yaml
window-policy: SLIDING_ON_ACCESS
```

滑动窗口才表达“每次有效访问后，再往后顺延 N 分钟”。

`record-retention-ttl` 只控制语义窗口结束后的物理记录额外保留时间，不应该替代 idempotencyWindow。

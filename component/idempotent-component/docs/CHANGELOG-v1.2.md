# Idempotent Component V1.2 - Transaction Template Integration

## 1. 版本目标

V1.2 不改变 V1.1 的幂等正确性根基：

```text
IdempotencyRepository
+ UNIQUE / Lua / CAS
+ ownerToken / version
```

本版本只补齐 DURABLE JDBC 最关键的本地事务原子性：

```text
Tx-A REQUIRES_NEW
    tryAcquire / tryRecover
    -> PROCESSING
    -> COMMIT

Tx-B REQUIRED
    Business Writes
    + markSuccess(ownerToken, version)
    -> COMMIT / ROLLBACK together

Tx-C REQUIRES_NEW
    Tx-B failure
    -> markFailed(ownerToken, version)
    -> COMMIT
```

## 2. 为什么需要 V1.2

V1.1 的默认 `DataSourceJdbcExecutionManager` 直接调用 `dataSource.getConnection()`。
即使业务外层存在 Spring Transaction，也可能出现：

```text
Business SQL  -> Connection-A / Tx-A
markSuccess   -> Connection-B / auto commit
```

于是存在两个危险窗口：

1. 业务提交成功，SUCCESS 写失败，幂等记录长期停留 PROCESSING；
2. SUCCESS 先提交，业务事务随后回滚，重复请求错误地 REPLAYED。

V1.2 通过 transaction-component + Spring transaction-bound Connection 消除这两个本地数据库窗口。

## 3. 新增模块

```text
idempotent-integration-transaction
```

包含：

- `TransactionTemplateIdempotencyTransactionCoordinator`
  - 使用 `TransactionExecutor` 建立 Tx-B REQUIRED；
- `SpringTransactionJdbcExecutionManager`
  - `inNewTransaction()` -> TransactionExecutor REQUIRES_NEW；
  - `inCurrentTransaction()` -> DataSourceUtils 获取当前事务绑定 Connection；
  - 不再直接用 `dataSource.getConnection()` 冒充事务参与。

## 4. JDBC Provider 事务语义调整

### tryAcquire / tryRecover

继续使用：

```text
JdbcExecutionManager.inNewTransaction(...)
```

即 Tx-A，确保 PROCESSING 在业务执行前独立提交、其他节点立即可见。

### markSuccess

从 V1.1 的：

```text
withConnection(...)
```

调整为：

```text
inCurrentTransaction(...)
```

transaction-aware 实现必须拿到 Tx-B 的当前 Connection。

### markFailed

从 V1.1 的：

```text
withConnection(...)
```

调整为：

```text
inNewTransaction(...)
```

即 Tx-C。业务事务回滚以后，FAILED 必须单独提交，否则失败状态会一起被回滚。

## 5. Core 事务闭环

只有同时满足：

```text
IdempotencyTransactionCoordinator != null
&& repository.supportsBusinessTransactionParticipation()
```

才启用 Tx-B。

Redis Repository 默认返回 false，因此不会错误地把 Redis SUCCESS 写入描述成数据库本地事务原子操作。

## 6. markSuccess 失败为什么必须让业务事务回滚

V1.2 在 Tx-B 内部规定：

```text
markSuccess == UPDATED
    -> 允许事务完成

STALE_OWNER / ALREADY_FINAL / PROVIDER_ERROR
    -> 抛内部 CompletionRejectedException
    -> Tx-B rollback
```

这意味着：

```text
业务 SQL 已执行
但 owner 已经过期
```

不会再出现“业务提交了，但幂等状态不能由当前 owner 完成”的半成功结果。

## 7. COMMIT_UNKNOWN

transaction-component 在 commit 基础设施异常时可能返回：

```text
COMMIT_UNKNOWN
```

此时业务写与 SUCCESS 由于处于同一个 Tx-B，只可能整体提交或整体回滚，但调用方暂时不知道是哪一个。

V1.2 的策略是：

```text
不立刻 markFailed
保留 PROCESSING
返回 TRANSACTION_COMMIT_UNKNOWN
```

后续重复请求：

- 如果实际已经提交，会读到 SUCCESS 并 REPLAYED；
- 如果实际回滚，PROCESSING 最终超时，由 Reliable Task recover() 接管。

## 8. 新增结果语义

```text
TRANSACTION_FAILED
TRANSACTION_COMMIT_UNKNOWN
```

`IdempotencyResult.transactionApplied=true` 表示本次 ACQUIRED / RECOVERY_ACQUIRED 执行真正启用了 Tx-B。

注意：Tx-B 使用 REQUIRED。如果调用方已有更外层事务，幂等执行会加入外层事务。
因此 `transactionApplied=true` 不代表 `IdempotencyExecutor.execute()` 返回时已经发生独立物理 COMMIT。

## 9. Spring Boot 配置

```yaml
xjtu:
  iron:
    idempotent:
      transaction:
        enabled: true
        require-template: true
```

- `enabled=true`：有 `TransactionExecutor` 时自动启用事务集成；
- `require-template=true`：要求 transaction-component 必须真正提供 `TransactionExecutor`，否则启动失败。

支付、结算、订单等强一致 DURABLE 场景建议生产环境开启 `require-template=true`。

## 10. 仍然没有解决什么

本版本只解决“同一个本地数据库事务资源”的原子性。

不解决：

- 跨库原子事务；
- Redis + DB 原子事务；
- HTTP / 银行 / MQ 等外部副作用；
- XA / TCC / Saga；
- Reliable Task 扫描调度本身。

外部副作用仍必须依赖下游幂等号、事务消息、Outbox、补偿或业务状态查询。

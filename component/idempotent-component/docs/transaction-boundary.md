# JDBC Idempotency 与 Transaction Template 边界（V1.2）

## 1. V1.1 的问题

直接调用：

```java
Connection connection = dataSource.getConnection();
```

并不会因为外层存在 Spring Transaction，就自动拿到事务已经绑定的那条 Connection。
因此 V1.1 可能出现：

```text
业务更新     -> Connection-A / Transaction-A
markSuccess  -> Connection-B / 独立自动提交
```

这不能保证业务数据与幂等 SUCCESS 原子提交。

---

## 2. V1.2 固定为三段事务

```text
Tx-A：状态抢占短事务
Tx-B：业务 + SUCCESS 业务事务
Tx-C：失败状态独立事务
```

### Tx-A：REQUIRES_NEW

```text
tryAcquire / tryRecover
    -> PROCESSING
    -> COMMIT
```

目的：PROCESSING 必须在 callback 开始以前独立提交，让其他节点立即看到“已有执行者”。

### Tx-B：REQUIRED

```text
Business Writes
+ markSuccess(ownerToken, version)
COMMIT / ROLLBACK together
```

目的：不能再出现“业务成功、SUCCESS 没写进去”或“SUCCESS 已提交、业务随后回滚”。

### Tx-C：REQUIRES_NEW

```text
Tx-B callback / transaction failed
    -> markFailed(ownerToken, version)
    -> COMMIT
```

目的：Tx-B 已经回滚，FAILED 不能继续依赖同一事务，否则 FAILED 自己也会一起回滚。

---

## 3. 为什么需要两个集成对象

### `TransactionTemplateIdempotencyTransactionCoordinator`

属于 Core 的业务事务边界适配：

```text
IdempotencyExecutor
    -> TransactionExecutor(REQUIRED)
        -> callback
        -> markSuccess
```

它负责 Tx-B。

### `SpringTransactionJdbcExecutionManager`

属于 JDBC Connection 参与适配：

```text
inCurrentTransaction
    -> DataSourceUtils.getConnection(dataSource)
    -> 必须是 transaction-bound Connection
```

以及：

```text
inNewTransaction
    -> TransactionExecutor(REQUIRES_NEW)
    -> inCurrentTransaction
```

它负责 Tx-A / Tx-C 和 JDBC Connection 的真实参与。

两个对象不能合成一个，因为“业务事务编排”和“JDBC Connection 获取”是两个不同层次的职责。

---

## 4. 为什么 markSuccess 不能继续用 `withConnection`

V1.2 的 JDBC SPI 明确拆成：

```text
withConnection        普通查询
inCurrentTransaction  Tx-B SUCCESS
inNewTransaction      Tx-A / Tx-C
```

`markSuccess()` 固定调用：

```java
jdbc.inCurrentTransaction(...)
```

transaction-aware 实现如果发现没有真实事务，直接失败，而不是偷偷借一条新 Connection 假装参与事务。

---

## 5. 为什么 markSuccess 返回 STALE_OWNER 也必须 rollback

假设：

```text
A generation version=10
A callback 已经写业务表
A 完成时发现 version=11 已经被 B 接管
```

如果只返回 OWNERSHIP_LOST，但让 Tx-B commit：

```text
A 的业务写仍然提交
A 的 SUCCESS 又被拒绝
```

结果仍然是脏数据。

所以 V1.2 在 Tx-B 内部规定：

```text
markSuccess != UPDATED
    -> 抛内部异常
    -> rollback 整个业务事务
```

owner/version CAS 因此不再只是“保护幂等状态表”，而会决定整个本地业务事务能不能提交。

---

## 6. REQUIRED 加入外层事务时怎么理解

Tx-B 使用 REQUIRED，而不是 REQUIRES_NEW。

如果没有外层事务：

```text
IdempotencyExecutor
    -> 创建 Tx-B
    -> callback + SUCCESS
    -> commit
```

如果已经存在外层事务：

```text
Outer Tx
    -> IdempotencyExecutor
        -> Tx-B REQUIRED 加入 Outer Tx
        -> callback + SUCCESS
    -> IdempotencyExecutor 返回
    -> Outer Tx 最后 commit / rollback
```

因此：

> `IdempotencyResult.transactionApplied=true` 只表示 callback + SUCCESS 已参与同一事务，
> 不承诺当前 `execute()` 返回点一定已经发生独立物理 COMMIT。

如果外层最终 rollback，业务写和 SUCCESS 会一起 rollback，Tx-A 的 PROCESSING 仍然存在，后续由超时恢复处理。

---

## 7. COMMIT_UNKNOWN

如果 transaction-component 在 commit 阶段得到不确定结果：

```text
TRANSACTION_COMMIT_UNKNOWN
```

幂等组件不能立刻写 FAILED。

因为真实结果可能是：

```text
业务 + SUCCESS 已经全部提交
```

也可能是：

```text
业务 + SUCCESS 全部回滚
```

所以 V1.2 保留 PROCESSING，等待：

1. 后续重复请求重新查询；
2. 或 processingTimeout 到期后由 Reliable Task recover()。

---

## 8. 边界条件

本方案成立必须满足：

```text
Business DB
Idempotency JDBC DB
使用同一个本地事务资源 / 同一个 DataSource 路由
```

如果幂等表和业务表跨两个数据库：

```text
Tx-B 无法靠本地 TransactionExecutor 保证原子提交
```

这时需要另行设计 Outbox、事务消息、TCC/Saga 或其他分布式一致性方案。

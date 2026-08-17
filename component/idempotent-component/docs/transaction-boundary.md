# V1.3 事务边界：Tx-A / Tx-B / Tx-C

## 1. 本文回答什么

幂等组件接入 transaction-component 以后，不是把整个：

```text
tryAcquire
Business
SUCCESS/FAILED
```

粗暴包成一个大事务。

V1.3 延续 V1.2 已验证的三段事务模型：

```text
Tx-A REQUIRES_NEW
    PROCESSING 抢占/恢复
    COMMIT

Tx-B REQUIRED
    Business
    ResultPolicy.capture
    markSuccess CAS
    COMMIT / ROLLBACK

Tx-C REQUIRES_NEW
    markFailed
    COMMIT
```

---

## 2. Tx-A：为什么必须短且独立提交

如果做成：

```text
BEGIN
  PROCESSING
  Business 20s
  SUCCESS
COMMIT
```

那么其他线程在 Business 执行期间可能：

- 看不到已经提交的 PROCESSING；
- 被唯一索引/行锁长时间阻塞；
- 数据库事务持续占用资源；
- PROCESSING “告诉别人我正在处理”的意义被削弱。

因此 JDBC Repository 的：

```text
tryAcquire / tryRecover
```

走 `inNewTransaction()`：

```text
REQUIRES_NEW
PROCESSING
COMMIT
```

让当前 generation 尽快对其他节点可见。

---

## 3. Tx-B：为什么 Business + SUCCESS 必须在一起

错误双写：

```text
Business DB COMMIT
        ↓
服务崩溃
        ↓
markSuccess 没执行
```

后续看到：

```text
PROCESSING_EXPIRED
```

可能进入恢复，造成业务再次执行。

所以在同一个本地事务资源场景：

```text
Business SQL
+
result capture
+
markSuccess(owner, version)
```

必须参加同一个 Tx-B。

`SpringTransactionJdbcExecutionManager` 使用：

```java
DataSourceUtils.getConnection(dataSource)
```

拿 Spring 当前事务绑定 Connection，而不是重新 `dataSource.getConnection()`。

如果当前事务并没有绑定幂等 JDBC 使用的 DataSource，集成层 fail-fast，不假装原子。

---

## 4. 为什么 Tx-B 是 REQUIRED

假设调用方已经有事务：

```text
Outer Tx
  updateA
  idempotency.execute(...)
  updateC
```

如果幂等内部强制 `REQUIRES_NEW`：

```text
Outer suspend
  Tx-B COMMIT Business + SUCCESS
Outer resume
updateC fails
Outer rollback
```

最终可能出现：

```text
幂等 SUCCESS 已提交
外层主要业务却 rollback
```

因此 Tx-B 必须 `REQUIRED`：

```text
Outer Tx
  updateA
  Business + SUCCESS  // join
  updateC
Outer COMMIT/ROLLBACK
```

事务组件最新版刻意不向公共 API 暴露 OWNER/PARTICIPANT 概念。

所以幂等组件也不制造一套新的 Owner/Participant API。

`transactionApplied=true` 只表示：

> Business 与 SUCCESS 已经处于 transaction-component 管理的同一本地事务边界。

它不等于：

> `IdempotencyExecutor.execute()` 返回时数据库一定已经物理 COMMIT。

如果 REQUIRED 加入外层事务，最终提交仍由外层决定。

---

## 5. owner/version 失效为什么必须让 Tx-B 整体 rollback

A：

```text
owner=A
version=10
```

执行期间超时。

B：

```text
recover
owner=B
version=11
```

A 恢复，先执行了 Business SQL，然后：

```text
markSuccess(A,10)
```

Repository CAS：

```sql
WHERE status='PROCESSING'
  AND owner_token='A'
  AND version=10
```

返回 0 行。

如果 Executor 只是返回：

```text
OWNERSHIP_LOST
```

但 Tx-B 仍 COMMIT，那么 A 的旧业务写会泄漏出去。

所以 V1.3 保留：

```text
markSuccess != UPDATED
    ↓
throw CompletionRejectedException
    ↓
Tx-B rollback
```

事务回滚以后再把内部异常翻译为：

```text
OWNERSHIP_LOST
```

---

## 6. Tx-C：为什么 FAILED 不能写在已经失败的 Tx-B

业务异常：

```text
Tx-B
 Business
 throw
 rollback
```

如果：

```text
markFailed()
```

也参加这个 Tx-B，它自己也会 rollback。

所以：

```text
Tx-B 完成 rollback
    ↓
Tx-C REQUIRES_NEW
markFailed(owner,version)
COMMIT
```

FAILED 仍然受 owner/version CAS 保护。

旧执行者连“把任务改成失败”都没有资格。

---

## 7. ResultPolicy 为什么在 Tx-B 里面

如果需要 SNAPSHOT：

```text
Business -> T
T -> serialize
markSuccess(payload)
```

序列化失败不应该出现：

```text
Business 已提交
但是 SUCCESS 结果保存失败
```

所以 transaction-aware 场景顺序是：

```text
Tx-B
  Business
  ResultPolicy.capture
  markSuccess
```

任意一步失败：

```text
rollback
```

REFERENCE 的 capture 同理。

---

## 8. COMMIT_UNKNOWN

事务基础设施可能出现：

```text
数据库实际已经 COMMIT
但确认响应在网络中丢失
```

应用无法确认真实状态。

transaction-component 将其表达为：

```text
COMMIT_UNKNOWN
```

幂等组件处理原则：

```text
不要 markFailed
不要自动重新执行业务
返回 TRANSACTION_COMMIT_UNKNOWN
等待后续查询/对账/Recovery 收敛
```

因为真实情况可能是：

```text
Business + SUCCESS 全部已经提交
```

再写 FAILED 会制造假事实。

---

## 9. 什么场景事务闭环成立

要求：

```text
Business SQL
+
Idempotency JDBC Repository
```

属于同一个本地事务资源，典型是同一个：

```text
DataSource + PlatformTransactionManager
```

如果：

```text
Business DB = mysql-A
Idempotency DB = mysql-B
```

普通本地事务不能把两个库变成原子提交。

如果 callback 有：

```text
银行扣款
HTTP 调用
MQ 外部副作用
```

数据库 rollback 也不能撤销外部系统已经发生的效果。

这类仍需要：

```text
下游幂等
业务状态查询
Outbox / 事务消息
Saga / TCC
补偿 / 对账
```

---

## 10. 三段事务与 Lock 的关系

真实顺序：

```text
optional lock
    ↓
Tx-A tryAcquire
    ↓
unlock
    ↓
StateMachine EXECUTE
    ↓
Tx-B Business + SUCCESS
    ↓ failure
Tx-C FAILED
```

分布式锁绝不覆盖 Tx-B 整段业务。

它们分别解决：

```text
Lock -> 状态抢占热点竞争
Tx-A -> PROCESSING 可见性
Tx-B -> Business + SUCCESS 原子性
Tx-C -> 失败状态独立落库
CAS -> generation owner 正确性
```

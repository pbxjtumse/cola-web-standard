# Transaction Component L1 最终设计

## 1. 核心原则

1. `transaction-component` = 本地事务组件。
2. 分布式事务未来独立为 `distributed-transaction-component`。
3. 业务唯一入口是 `TransactionExecutor`。
4. Spring 负责真实事务管理；组件不重写 TransactionManager。
5. REQUIRED 的“新建还是复用”是 Spring 内部事实，不再映射 OWNER/PARTICIPANT。
6. `TransactionStage` 保留，用于基础设施故障定位。
7. `COMMIT_UNKNOWN` 保留，用于 Retry/幂等可靠性语义。
8. 业务异常保持原类型。
9. rollback 自身失败作为 suppressed exception 保留。
10. 一期同步执行，不在事务内部切线程。
11. ORM 不做专属 Provider。
12. executionId 允许未来由 foundation-id 接管。

## 2. API

```text
TransactionExecutor
TransactionCallback
TransactionRunnable
TransactionOptions
TransactionPropagation
TransactionIsolation
TransactionContext
TransactionStage
TransactionOutcome
TransactionExecutionException
TransactionEvent
TransactionEventListener
```

## 3. SPI

```text
TransactionProvider
TransactionProviderCallback
ProviderTransactionContext
TransactionExecutionIdGenerator
ProviderTransactionException
```

## 4. 为什么 TransactionProvider 仍然使用 callback

Provider 必须表达的是一个完整事务边界：

```text
拿到 TransactionOptions
  ↓
建立 / 复用事务
  ↓
执行 callback
  ↓
commit / rollback
```

如果把 begin/commit/rollback 拆给 core 控制，core 就会重新承担 REQUIRED、REQUIRES_NEW、挂起/恢复等 Spring 已经解决的问题。

所以保留：

```java
<T> T execute(TransactionOptions options,
              TransactionProviderCallback<T> callback);
```

## 5. TransactionContext 为什么很小

```java
String executionId();
String transactionName();
boolean isRollbackOnly();
void setRollbackOnly();
```

业务不应该根据“我是新事务还是复用已有事务”分叉业务逻辑。

## 6. Event 语义

```text
STARTED
COMPLETED
BUSINESS_FAILED
INFRASTRUCTURE_FAILED
```

`COMPLETED` = 当前逻辑 execute 正常完成。

不等于：一定发生独立数据库 COMMIT。

## 7. TransactionOutcome

一期仅用于异常路径：

```text
ROLLED_BACK
COMMIT_UNKNOWN
FAILED
```

不再用 `PARTICIPATED / ROLLBACK_ONLY / COMMITTED` 建模每个嵌套逻辑事务范围。

## 8. L1 结束标准

- API / SPI / Core 稳定
- Spring Provider 稳定
- Starter 可自动装配
- MyBatis XML Demo 通过
- JPA Demo 通过
- REQUIRED / REQUIRES_NEW / rollbackOnly 场景验证
- 真实 MySQL Demo 配置可运行
- 单测默认不碰远端 MySQL

达到这些条件后，立刻回到幂等组件。

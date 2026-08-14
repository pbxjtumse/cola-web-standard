# Transaction Component V1 - 逐流程理解

## 1. 主链路

```text
业务 Service
  -> TransactionExecutor
  -> DefaultTransactionExecutor
  -> TransactionProvider SPI
  -> SpringTransactionProvider
  -> PlatformTransactionManager
  -> DataSourceTransactionManager / JpaTransactionManager
  -> MyBatis XML / JPA Repository
  -> Database
```

## 2. 正常 REQUIRED

1. Service 调用 `transactionExecutor.executeWithoutResult(...)`。
2. `DefaultTransactionExecutor` 校验 `TransactionOptions` 并生成 `executionId`。
3. `SpringTransactionProvider` 把 Options 映射成 Spring `DefaultTransactionDefinition`。
4. `PlatformTransactionManager#getTransaction` 创建物理事务，当前 participation=OWNER。
5. callback 执行 MyBatis XML 或 JPA Repository。
6. callback 正常返回。
7. Provider 调用 `commit(status)`。
8. OWNER 最终 outcome=COMMITTED。

## 3. 业务异常回滚

1. 事务正常 BEGIN。
2. SQL 已经执行，但尚未 COMMIT。
3. callback 抛 `IllegalStateException` 等业务异常。
4. Provider 调用 `rollback(status)`。
5. SQL 变更回滚。
6. 原业务异常继续向上抛出，不包装成 `TransactionExecutionException`。

## 4. REQUIRED 嵌套

1. outer REQUIRED 创建 Tx-A，outer=OWNER。
2. inner REQUIRED 调用 `getTransaction` 时发现 Tx-A 已存在。
3. inner=PARTICIPANT，继续使用 Tx-A 的连接/EntityManager。
4. inner `execute()` 返回不等于 COMMIT；它最多只能得到 PARTICIPATED。
5. outer 最终失败时 Tx-A 整体回滚，outer/inner SQL 一起消失。

## 5. REQUIRES_NEW 嵌套

1. outer REQUIRED 创建 Tx-A。
2. inner REQUIRES_NEW 到来。
3. Spring 挂起 Tx-A，并创建 Tx-B。
4. inner SQL 在 Tx-B 执行并 COMMIT。
5. Spring 恢复 Tx-A。
6. outer 随后失败，Tx-A ROLLBACK。
7. Tx-B 已经提交，因此 inner 数据保留。

这就是后续幂等 `claim PROCESSING` 独立短事务的基础模型。

## 6. rollbackOnly

1. callback SQL 正常执行。
2. 业务不抛异常，但调用 `ctx.setRollbackOnly()`。
3. Provider 完成 callback 后看到 rollbackOnly=true。
4. 调用 Spring `commit(status)` 时，Spring 实际按 rollback-only 语义完成回滚。
5. OWNER outcome=ROLLED_BACK；PARTICIPANT outcome=ROLLBACK_ONLY。

## 7. MyBatis XML 为什么不需要自己的事务 Provider

`DemoRecordMapper.xml` 只定义 SQL。

事务由：

```text
TransactionExecutor
  -> SpringTransactionProvider
  -> DataSourceTransactionManager
```

建立。

MyBatis-Spring 在执行 Mapper 时使用当前 Spring 事务绑定的数据库连接，所以 Mapper XML 不需要 BEGIN/COMMIT/ROLLBACK。

## 8. JPA 为什么同一套 API 可以工作

JPA Demo 中 Spring 提供的是 `JpaTransactionManager`。

我们的 `SpringTransactionProvider` 只依赖 `PlatformTransactionManager`，所以 API/core 不需要知道底层究竟是 MyBatis 还是 JPA。

## 9. 与幂等组件衔接时重点看什么

后续先实现两种模式：

### Atomic

```text
Tx-A
  claim
  business
  mark SUCCESS
COMMIT
```

### Durable Processing

```text
Tx-1 REQUIRES_NEW: claim PROCESSING -> COMMIT
Business/Tx-2:      execute business
Tx-3 REQUIRES_NEW: mark SUCCESS -> COMMIT
```

第二种模式会产生 PROCESSING 已提交但业务/状态更新中断的恢复窗口，因此才需要 timeout scanner / claim / recovery。

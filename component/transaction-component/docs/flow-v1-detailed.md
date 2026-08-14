# Transaction Component V1.3 全流程

## 1. 最普通的 REQUIRED 成功

业务：

```java
transactionExecutor.execute(ctx -> {
    orderMapper.insert(order);
    return order;
});
```

流程：

```text
1. 创建 TransactionCallback（只是定义业务代码）
2. 进入 DefaultTransactionExecutor
3. 校验 TransactionOptions
4. 生成 executionId
5. 发布 STARTED
6. 调用 TransactionProvider.execute
7. SpringTransactionProvider 映射 TransactionOptions
8. transactionManager.getTransaction(...)
9. 当前没有事务，因此 REQUIRED 建立 Tx-A
10. Provider 调用 TransactionProviderCallback
11. Core 创建 DefaultTransactionContext
12. Core 调用用户 TransactionCallback.execute
13. MyBatis/JPA 在 Tx-A 中执行 SQL
14. callback 正常返回
15. Provider 调用 transactionManager.commit(status)
16. Tx-A 提交
17. Provider 返回业务值
18. Executor 发布 COMPLETED
19. 返回业务结果
```

## 2. REQUIRED 嵌套仍然只有 Tx-A

```java
transactionExecutor.execute(ctx -> {
    orderMapper.insert(order);

    transactionExecutor.execute(inner -> {
        idempotencyMapper.insert(record);
        return null;
    });

    throw new IllegalStateException();
});
```

流程：

```text
第一次 REQUIRED
  ↓
当前没有事务
  ↓
创建 Tx-A
  ↓
INSERT order
  ↓
第二次 REQUIRED
  ↓
当前线程已经处于 Tx-A
  ↓
继续使用 Tx-A
  ↓
INSERT idempotency
  ↓
第二次 execute 正常返回
  ↓
第一层业务抛异常
  ↓
rollback Tx-A
```

结果：

```text
order        回滚
idempotency  回滚
```

注意：第二次 `execute()` 返回，不代表数据已经独立提交；它只是完成了当前逻辑调用。

## 3. REQUIRES_NEW 创建独立 Tx-B

```text
Tx-A
  INSERT order
  ↓
REQUIRES_NEW
  ↓
挂起 Tx-A
  ↓
Tx-B
  INSERT idempotency
  COMMIT Tx-B
  ↓
恢复 Tx-A
  ↓
业务异常
  ↓
ROLLBACK Tx-A
```

最终：

```text
order        不存在
idempotency  存在
```

## 4. callback 业务异常

```text
Tx-A
  ↓
INSERT
  ↓
TransactionCallback 抛 BusinessException
  ↓
SpringTransactionProvider catch
  ↓
transactionManager.rollback(status)
  ↓
原 BusinessException 继续抛出
  ↓
DefaultTransactionExecutor 发布 BUSINESS_FAILED
```

事务组件不把业务异常统一包装成 `TransactionExecutionException`。

## 5. rollback 自身又失败

```text
BusinessException A
  ↓
rollback
  ↓
rollback infrastructure exception B
```

处理：

```text
A = 主异常
B = A.getSuppressed() 中的 ProviderTransactionException
```

这样调用方首先看到真正业务根因，同时诊断信息仍然保留。

## 6. setRollbackOnly

```java
transactionExecutor.execute(ctx -> {
    mapper.insert(...);
    ctx.setRollbackOnly();
    return null;
});
```

流程：

```text
SQL 正常执行
  ↓
TransactionStatus 标记 rollback-only
  ↓
callback 正常返回
  ↓
Provider 仍调用 commit(status)
  ↓
Spring 根据 rollback-only 完成回滚语义
```

用户主动要求 rollback-only，所以业务代码可以正常返回；事务结果由 Spring 完成。

## 7. COMMIT_UNKNOWN

```text
SQL 已执行
  ↓
commit(status)
  ↓
数据库可能已提交
  ↓
网络/连接异常
  ↓
调用方收到 TransactionException
```

Provider 转换：

```text
stage   = COMMIT
outcome = COMMIT_UNKNOWN
```

后续幂等/Retry 必须先确认业务最终状态，不能把它当成确定失败。

## 8. MyBatis XML 链路

```text
TransactionExecutor
  ↓
SpringTransactionProvider
  ↓
DataSource/JdbcTransactionManager
  ↓
当前线程绑定 Connection
  ↑
MyBatis-Spring
  ↑
Mapper Interface
  ↓
Mapper XML
  ↓
SQL
```

事务不写在 MyBatis XML 里；XML 只负责 SQL。

## 9. JPA 链路

```text
TransactionExecutor
  ↓
SpringTransactionProvider
  ↓
JpaTransactionManager
  ↓
EntityManager
  ↓
Repository / Hibernate
  ↓
Database
```

## 10. 线程池组合

正确：

```text
concurrency executor
  ↓
Thread-B
  ↓
TransactionExecutor
  ↓
Tx-B
```

不推荐：

```text
Thread-A 已经开启 Tx-A
  ↓
把 SQL 提交给 Thread-B
```

一期 transaction-component 内部不切线程。

## 11. 后续嵌入幂等

### Atomic

```text
Tx-A
├── claim
├── business
└── mark SUCCESS
```

任何异常导致 Tx-A 回滚时，属于 Tx-A 的业务与幂等记录一起回滚。

### Durable PROCESSING

```text
Tx-1 REQUIRES_NEW
└── claim PROCESSING
    COMMIT

业务 Tx-2

Tx-3 REQUIRES_NEW
└── mark SUCCESS
    COMMIT
```

这时才会自然产生：

```text
PROCESSING 已提交
业务成功
SUCCESS 未写入前进程宕机
```

然后再引出 Scanner / Claim / Recovery。

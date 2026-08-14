# transaction-component V1.3

> 本地事务统一执行组件。V1.3 是一期最终收敛基线：保留真正需要的事务语义，删除一期里不必要的 OWNER / PARTICIPANT / TransactionExecutionState 模型。

## 1. 最终定位

`transaction-component` 只解决**本地事务**：

```text
业务代码
   ↓
TransactionExecutor
   ↓
TransactionProvider
   ↓
SpringTransactionProvider
   ↓
PlatformTransactionManager
   ↓
MyBatis/JPA/JDBC
   ↓
MySQL/PostgreSQL
```

它不提供 XA、TCC、Saga、Seata 等分布式事务协调能力。以后如果确实需要，单独建设 `distributed-transaction-component`。

## 2. V1.3 为什么删掉 OWNER / PARTICIPANT

一期只需要把传播规则讲清楚：

```text
REQUIRED
- 当前没有事务：创建 Tx-A
- 当前已经有 Tx-A：继续使用 Tx-A

REQUIRES_NEW
- 当前没有事务：创建 Tx-A
- 当前已经有 Tx-A：挂起 Tx-A，创建独立 Tx-B

MANDATORY
- 当前有事务：继续使用
- 当前没有事务：直接失败
```

Spring 内部当然知道 `TransactionStatus.isNewTransaction()`，但这个事实不需要成为业务 API 的新术语。

所以 V1.3 删除：

- `TransactionParticipation`
- `OWNER`
- `PARTICIPANT`
- `TransactionExecutionState`
- `TransactionProviderResult`

业务只关心：**我的代码是否在事务环境中执行、正常结束是否完成事务、异常是否触发回滚、提交结果是否可能未知。**

## 3. 一期保留的核心能力

- 显式 `TransactionExecutor`
- `TransactionCallback<T>` / `TransactionRunnable`
- `TransactionOptions`
- `REQUIRED / REQUIRES_NEW / MANDATORY`
- 隔离级别
- timeout
- readOnly
- `TransactionContext#setRollbackOnly()`
- `TransactionStage`
- `COMMIT_UNKNOWN`
- 业务异常原样传播
- rollback 二次失败保留为 suppressed exception
- 基础事件
- Spring Boot Starter
- MyBatis XML Demo
- JPA Demo
- 可替换 executionId 生成 SPI

一期明确不做：

- 自定义 `@Transactional`
- AOP 事务语法糖
- NESTED / Savepoint
- afterCommit / afterRollback hook
- 多 TransactionManager 路由
- Reactive Transaction
- 分布式事务
- 事务补偿
- 异步事务 Executor

## 4. 核心模块

```text
transaction-component
├── transaction-api
├── transaction-spi
├── transaction-core
├── transaction-provider-spring
├── transaction-spring-boot-starter
├── transaction-demo-mybatis
└── transaction-demo-jpa
```

### transaction-api

```text
execution/
  TransactionExecutor
  TransactionCallback
  TransactionRunnable

definition/
  TransactionOptions
  TransactionPropagation
  TransactionIsolation

context/
  TransactionContext

status/
  TransactionStage
  TransactionOutcome

event/
  TransactionEvent
  TransactionEventType
  TransactionEventListener

exception/
  TransactionExecutionException
```

### transaction-spi

```text
provider/
  TransactionProvider
  TransactionProviderCallback
  ProviderTransactionContext

id/
  TransactionExecutionIdGenerator

exception/
  ProviderTransactionException
```

## 5. 最小业务使用方式

```java
Order order = transactionExecutor.execute(
        TransactionOptions.builder()
                .name("create-order")
                .build(),
        ctx -> {
            orderMapper.insert(order);
            accountMapper.update(account);
            return order;
        }
);
```

最核心的执行顺序：

```text
TransactionExecutor
  ↓
校验 TransactionOptions
  ↓
生成 executionId
  ↓
TransactionProvider.execute(...)
  ↓
Spring getTransaction(...)
  ↓
执行 TransactionCallback
  ↓
正常 → Spring commit(status)
异常 → Spring rollback(status) → 原业务异常继续抛出
```

## 6. REQUIRED 不再讲 OWNER/PARTICIPANT，只讲 Tx-A

```text
第一次 REQUIRED
当前没有事务
  ↓
创建 Tx-A
  ↓
insert order

第二次 REQUIRED
当前已经有 Tx-A
  ↓
继续使用 Tx-A
  ↓
insert idempotency

后续异常
  ↓
Tx-A 整体 rollback
```

只要订单和幂等 SQL 都属于 Tx-A，Tx-A rollback 时两者一起回滚。

## 7. REQUIRES_NEW 只讲 Tx-A / Tx-B

```text
Tx-A
  insert order
  ↓
REQUIRES_NEW
  ↓
挂起 Tx-A
  ↓
Tx-B
  insert idempotency
  COMMIT Tx-B
  ↓
恢复 Tx-A
  ↓
Tx-A 后续异常
  ↓
ROLLBACK Tx-A
```

最终：

- Tx-A 中的订单回滚
- Tx-B 中已经独立提交的数据保留

这正是后续幂等 `PROCESSING` 独立短事务可能使用的能力。

## 8. 为什么还保留 TransactionStage

异常必须能够定位发生在哪一步：

```text
VALIDATE
RESOLVE
BEGIN
EXECUTE
COMMIT
ROLLBACK
COMPLETION
```

业务 callback 抛出的异常仍然保持原类型；BEGIN/COMMIT/ROLLBACK 等基础设施异常转换为 `TransactionExecutionException`。

## 9. 为什么还保留 COMMIT_UNKNOWN

```text
客户端发起 COMMIT
  ↓
数据库可能已经提交
  ↓
响应返回途中连接断开
  ↓
调用方只看到 commit 异常
```

这时不能武断认为“事务肯定没提交”。因此：

```text
stage   = COMMIT
outcome = COMMIT_UNKNOWN
```

后续 Retry / Idempotency 遇到这个状态时，应先查询最终业务状态，而不是直接重放副作用操作。

## 10. MyBatis / JPA 不做专属 Provider

### MyBatis/JDBC

```text
TransactionExecutor
  ↓
SpringTransactionProvider
  ↓
PlatformTransactionManager
  ↓
DataSource/JdbcTransactionManager
  ↓
MyBatis Mapper + Mapper XML
```

### JPA/Hibernate

```text
TransactionExecutor
  ↓
SpringTransactionProvider
  ↓
PlatformTransactionManager
  ↓
JpaTransactionManager
  ↓
EntityManager / Repository
```

事务组件只适配 `PlatformTransactionManager`，ORM 通过 Spring 自然参与事务。

MyBatis Demo 已使用 XML SQL，不使用 `@Insert/@Select/@Update/@Delete`。

## 11. MySQL Demo

主 Demo 使用：

```text
jdbc:mysql://www.xjtu-iron.online:30306/test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
```

用户名和密码继续通过环境变量注入：

```text
TX_DEMO_MYSQL_USERNAME
TX_DEMO_MYSQL_PASSWORD
```

自动测试使用 H2 `test` profile，避免 `mvn test` 直接修改远程 MySQL 测试库。

## 12. executionId 与 foundation-id

V1.3 新增：

```java
TransactionExecutionIdGenerator
```

默认：

```text
UuidTransactionExecutionIdGenerator
```

Starter 使用 `@ConditionalOnMissingBean`，所以以后 `foundation-id` 只需要提供一个适配 Bean 即可替换默认 UUID，无需让 `transaction-core` 强依赖具体 ID 算法。

## 13. 线程池边界

一期事务 Executor 是同步的，不内部切线程。

正确组合：

```text
Concurrency Component
  ↓
工作线程
  ↓
TransactionExecutor
  ↓
BEGIN → SQL → COMMIT/ROLLBACK
```

不要：

```text
Transaction 已经开始
  ↓
把事务中的 SQL 扔到另一个线程
```

传统 Spring imperative transaction 的资源通常绑定当前线程，跨线程后不能假设仍然是同一个事务。

## 14. 四期路线

### L1：本地事务核心闭环（当前版本）

把显式本地事务执行做好，然后停止扩张。

### L2：事务治理

- Named Policy
- PolicyRegistry
- 多 TransactionManager
- afterCommit / afterRollback / afterCompletion
- Metrics / Trace / 慢事务
- 动态配置

### L3：可靠性组合

- Retry + Transaction
- Idempotency + Transaction
- Outbox + Transaction
- Message + Transaction
- Retry boundary validation

### L4：高级能力

- NESTED / Savepoint
- Reactive Transaction
- 更多 Provider
- 事务诊断与规范检查

分布式事务始终是独立组件，不进入这里。

## 15. 下一步

V1.3 一期稳定后，不继续膨胀 transaction-component，直接回到 `idempotency-component`，用 Tx-A / Tx-B / Tx-C 重新实现并验证：

1. Atomic 模式
2. Durable PROCESSING 模式
3. PROCESSING_TIMEOUT
4. Scanner / Claim / Recovery
5. COMMIT_UNKNOWN 下的状态确认
6. Retry 在事务外重新开启完整事务单元

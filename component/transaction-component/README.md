# transaction-component v1

> 定位：统一的本地事务执行与治理基础组件。  
> 当前阶段：L1 / 第一期 —— 本地事务核心闭环。  
> 非目标：XA、TCC、Saga、跨服务事务协调。

## 1. 为什么仍然叫 transaction-component

本项目把 `transaction-component` 默认定义为本地事务组件；未来如果确有跨资源/跨服务协调需求，独立建设 `distributed-transaction-component`。
因此 API 保持 `TransactionExecutor / TransactionOptions / TransactionContext`，避免 Local 前缀污染所有调用代码。

## 2. 一期核心边界

一期只解决一个本地 `PlatformTransactionManager` 范围内的事务：

- 显式 TransactionExecutor；
- REQUIRED / REQUIRES_NEW / MANDATORY；
- 隔离级别；
- timeout；
- readOnly；
- rollbackOnly；
- OWNER / PARTICIPANT 区分；
- BEGIN / EXECUTE / COMMIT / ROLLBACK 等阶段；
- COMMIT_UNKNOWN；
- 最小生命周期事件；
- Spring Boot 自动配置；
- MyBatis / JPA Demo 验证。

一期明确不做：

- 自定义 @Transactional/AOP；
- 多 TransactionManager 选择/路由；
- NESTED / Savepoint；
- Reactive Transaction；
- XA/TCC/Saga；
- 持久化事务日志与扫描器；
- ORM 专属 Provider。

## 3. 模块结构

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

稳定公共语义，不依赖 Spring/MyBatis/JPA：

- TransactionExecutor
- TransactionCallback / TransactionRunnable
- TransactionOptions
- TransactionContext
- TransactionPropagation
- TransactionIsolation
- TransactionParticipation
- TransactionStage
- TransactionOutcome
- TransactionExecutionException
- TransactionEvent / TransactionEventListener

### transaction-spi

Provider 适配协议。它不把 SPI 拆成 begin/commit/rollback/suspend/resume，避免重新发明事务管理器。

### transaction-core

实现参数校验、逻辑 executionId、API/SPI 桥接、异常语义与事件发布。

### transaction-provider-spring

唯一一期 Provider，适配 `PlatformTransactionManager`。

MyBatis/JDBC 通常走 `DataSourceTransactionManager/JdbcTransactionManager`；JPA 通常走 `JpaTransactionManager`。
因此一期没有 `transaction-provider-mybatis` 或 `transaction-provider-jpa`。

## 4. 基本使用

```java
transactionExecutor.executeWithoutResult(
        TransactionOptions.builder()
                .name("create-order")
                .build(),
        tx -> {
            orderMapper.insert(...);
            accountMapper.update(...);
        }
);
```

### rollbackOnly

```java
transactionExecutor.executeWithoutResult(tx -> {
    orderMapper.insert(...);
    if (needRollback) {
        tx.setRollbackOnly();
    }
});
```

### REQUIRES_NEW

```java
TransactionOptions claimTx = TransactionOptions.builder()
        .name("idempotency-claim")
        .propagation(TransactionPropagation.REQUIRES_NEW)
        .build();

transactionExecutor.executeWithoutResult(claimTx, tx -> {
    idempotencyRepository.claimProcessing(...);
});
```

这正是后续 Durable Idempotency 中 “PROCESSING 独立短事务提交” 的基础。

## 5. OWNER 与 PARTICIPANT

```text
outer REQUIRED                    physical Tx-A
  |
  +-- inner REQUIRED              PARTICIPANT of Tx-A

inner execute() 返回 != Tx-A 已经 COMMIT
```

因此 `TransactionContext.isNewTransaction()` 很重要：

- OWNER：当前调用创建新物理事务；
- PARTICIPANT：当前调用只加入外层事务。

## 6. Retry + Transaction 的正确边界

推荐：

```text
Retry
  -> Transaction Tx-1
       failed -> rollback
  -> Transaction Tx-2
```

代码：

```java
retryExecutor.execute(() ->
        transactionExecutor.execute(tx -> {
            updateOrder();
            updateAccount();
            return null;
        })
);
```

不要在一个已经失败/rollback-only 的事务内部只重试单条 SQL。

## 7. COMMIT_UNKNOWN

commit 抛出基础设施异常时，本地调用方并不总能仅凭异常证明数据库一定没有提交。
因此 Provider 对一般 commit 基础设施异常保守映射为：

```text
stage   = COMMIT
outcome = COMMIT_UNKNOWN
```

后续 Retry/Idempotency 看到该结果时应先确认业务/幂等最终状态，不能直接把它当作“肯定失败然后无脑重试”。

`UnexpectedRollbackException` 则表示 Spring 已明确发生 rollback，映射为 `ROLLED_BACK`。

## 8. MyBatis/JPA 为什么没有专属 Provider

事务组件适配的是事务管理器，不是 ORM：

```text
TransactionExecutor
        |
SpringTransactionProvider
        |
PlatformTransactionManager
   /                 \
JDBC TM             JPA TM
  |                   |
MyBatis/JDBC       JPA/Hibernate
```

两个 Demo 用同一个 TransactionExecutor API 验证 ORM 无关性。

## 9. 当前版本依赖基线

示例工程为了可独立构建，根 POM 使用 Java 17 兼容代码并导入 Spring Boot 3.5.16 BOM；
这不是要求你的总工程必须升级/降级到该版本。接入现有技术组件总 POM 时，应优先复用总工程 BOM 和 pluginManagement。

## 10. 下一步

事务一期完成并验证后，回到 JDBC 幂等组件，重点拆清：

1. Atomic 模式：claim + business + success 同事务；
2. Durable Processing 模式：claim(REQUIRES_NEW) -> business -> success(REQUIRES_NEW)；
3. PROCESSING_TIMEOUT；
4. 扫描恢复；
5. COMMIT_UNKNOWN 与重试/状态确认；
6. Retry 在 Transaction 外层的完整闭环。

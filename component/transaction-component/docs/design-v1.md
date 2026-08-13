# Transaction Component L1 设计说明

## 核心原则

1. transaction-component 默认表示本地事务。
2. 不重新实现数据库事务协议，Provider 适配成熟事务管理器。
3. ORM 不是事务 Provider 的扩展维度。
4. REQUIRED 的“内部 execute 返回”不等于外层物理事务已提交。
5. REQUIRES_NEW 的 suspend/resume 由底层事务管理器负责。
6. callback 业务异常触发 rollback 后原样抛出。
7. rollback 自身失败时，原始业务异常保持 primary，rollback 基础设施异常作为 suppressed。
8. 普通 commit 基础设施异常按 COMMIT_UNKNOWN 保守处理。
9. 事件监听属于观测能力，监听器失败不能影响事务结果。
10. 一期只支持单一默认 PlatformTransactionManager。

## 为什么不提供 MyBatis/JPA Provider

MyBatis-Spring 通过 Spring 管理的 DataSource 事务参与事务；JPA 通过 JpaTransactionManager 管理 EntityManager。
Transaction Component 只需适配 PlatformTransactionManager。

## 对幂等组件的直接价值

### Atomic

```text
BEGIN
 claim
 business
 success
COMMIT
```

### Durable Processing

```text
Tx-1(REQUIRES_NEW): claim PROCESSING -> COMMIT
business
Tx-2(REQUIRES_NEW): mark SUCCESS -> COMMIT
```

第二种模式暴露 PROCESSING 中间状态，因此需要 timeout / scanner / recovery；这是事务边界带来的必然复杂度，不应由 Transaction Component 隐藏。

# JDBC Connection 与未来 Transaction Template 的边界

## 为什么 `dataSource.getConnection()` 不能自动加入外层事务

Spring/事务框架通常会把“当前事务 Connection”绑定到当前执行上下文。只有使用事务框架提供的 transaction-aware 获取方式，Repository 才会拿到那一条已经开始事务的 Connection。

直接调用：

```java
Connection connection = dataSource.getConnection();
```

通常意味着重新从连接池借一条 Connection。即使外层已经：

```java
transactionTemplate.execute(() -> {
    businessRepository.update();
    idempotencyRepository.markSuccess();
});
```

如果 `markSuccess()` 内部重新 `dataSource.getConnection()`，就可能出现：

```text
业务更新     -> Connection-A / Transaction-A
markSuccess  -> Connection-B / 独立自动提交
```

两者不是同一事务。

## V1.1 的准备

JDBC Provider 新增：

```text
JdbcExecutionManager
├── withConnection(...)
└── inNewTransaction(...)
```

默认 `DataSourceJdbcExecutionManager` 仍直接使用 DataSource，因此当前版本没有虚假承诺“Business + SUCCESS 已经同事务”。

未来 transaction-component 接入时，应提供 transaction-aware `JdbcExecutionManager`：

- `tryAcquire / tryRecover` -> `inNewTransaction`，使用独立短事务；
- `markSuccess` -> `withConnection`，复用业务当前事务 Connection；
- 业务失败后 `markFailed` -> 独立新事务记录失败状态。

最终目标：

```text
Tx-A (短)
PROCESSING
COMMIT

Tx-B (业务)
Business writes
+ markSuccess
COMMIT/ROLLBACK together

Tx-C (业务 Tx-B 失败后)
markFailed
COMMIT
```

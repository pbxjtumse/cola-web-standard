# CHANGELOG

## V1.3 - 一期最终收敛基线

### 删除

- 删除 `TransactionParticipation`
- 删除 `OWNER / PARTICIPANT`
- 删除 `TransactionExecutionState`
- 删除 `TransactionProviderResult`
- `TransactionContext` 不再暴露 `isNewTransaction()` / `isParticipating()`

### 保留并强化

- REQUIRED / REQUIRES_NEW / MANDATORY
- TransactionStage
- COMMIT_UNKNOWN
- 业务异常原样传播
- rollback 二次失败使用 suppressed exception
- MyBatis XML / JPA Demo
- Spring PlatformTransactionManager Provider

### 新增

- `TransactionExecutionIdGenerator` SPI
- `UuidTransactionExecutionIdGenerator` 默认实现
- Starter 支持通过自定义 Bean 替换 executionId 生成器，为 foundation-id 集成预留接口

### 设计原则

正常 `COMPLETED` 只表示本次 `TransactionExecutor.execute(...)` 调用正常结束，不宣称一定发生独立物理 COMMIT。底层事务传播事实交给 Spring，不再映射为业务公共术语。

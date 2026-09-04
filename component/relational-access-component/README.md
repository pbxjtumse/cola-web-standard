# Relational Access Component

> 当前阶段：**v1-models / Design First**  
> 本阶段只建立 Maven 模块、公开 API、扩展 SPI 与 PlantUML 设计图，**不提供 relational-core 执行实现**。

## 1. 组件定位

Relational Access Component 是 Iron Components 面向关系型数据库的轻量访问底座。

它不尝试替代 MyBatis、JPA、jOOQ，也不实现 ORM。它解决的是基础组件内部重复出现的 JDBC 基础设施问题：SQL 执行入口、参数模型、结果映射契约、连接获取扩展点、异常翻译、方言与可观测扩展点。

典型依赖关系：

```text
Business Template
    |
    +--> IdempotencyExecutor / MessageTemplate / TransactionTemplate
    |           |
    |           +--> JdbcXXXStorage
    |                     |
    |                     +--> relational-api
    |
    +--> Business Repository --> MyBatis / JPA / jOOQ

relational-core (后续阶段)
    |
    +--> relational-spi --> JDBC / DataSource
```

## 2. API 分级

### Level A：业务模板依赖的高层能力 API

业务模板通常依赖其他技术组件暴露的高层接口，例如：

- `IdempotencyExecutor`
- `MessageTemplate`
- `RetryExecutor`
- `TransactionTemplate`
- 业务自己的 `OrderRepository` / `PaymentRepository`

业务模板默认**不直接依赖** `RelationalTemplate`。

### Level B：技术组件 Storage Adapter 使用的 Relational API

例如 `JdbcIdempotencyStorage`、`JdbcOutboxStorage`、`JdbcTaskStorage` 使用：

- `RelationalTemplate`
- `SqlStatement`
- `BatchSqlStatement`
- `SqlParameter`
- `SqlExecutionOptions`
- `RowMapper<T>`
- `UpdateResult`
- `BatchResult`
- `GeneratedKey<T>`
- `RelationalAccessException`
- `RelationalFailureType`

### Level C：基础设施集成使用的 SPI

普通 Storage 不应直接依赖这些 SPI。它们面向后续 `relational-core`、事务集成、分库分表集成和可观测集成：

- `ConnectionProvider`
- `ConnectionHandle`
- `DataSourceResolver`
- `SqlExceptionTranslator`
- `RelationalDialect`
- `SqlExecutionListener`
- `SqlExecutionContext`

## 3. SQL 的归属

Relational Access **不生成业务 SQL，也不理解幂等/Outbox/任务领域**。

SQL 应属于具体 JDBC Storage Adapter，例如：

```text
Idempotency Core
    -> IdempotencyStorage (领域存储 Port)
    -> JdbcIdempotencyStorage (把领域操作转换成 SQL / Route)
    -> RelationalTemplate (可靠执行已经确定的 SQL)
    -> JDBC
```

因此：

- `tryAcquire()` 是幂等领域语义；
- `UPDATE ... WHERE status = ?` 是 `JdbcIdempotencyStorage` 的物理存储语义；
- `prepareStatement / bind / execute / close` 是 Relational Access 的执行语义。

## 4. 当前明确不做

- ORM / Entity / Repository 自动实现
- `@Table` / `@Column`
- SQL DSL / SQL AST / SQL Parser
- 自动分页 SQL 改写
- 自动重试
- 分库分表算法
- 数据库连接池
- Schema Migration
- relational-core 实现

## 5. UML

见 `docs/`：

- `component/`：组件边界和 API/SPI 依赖
- `state/`：SQL 执行与 ConnectionHandle 生命周期
- `sequence/`：技术组件 Storage、普通查询、业务 MyBatis + 技术组件事务协同

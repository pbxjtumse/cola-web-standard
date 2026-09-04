# Relational Access Component

> 当前阶段：**v1 JDBC Foundation / Core Main Flow**  
> 当前已经包含 `relational-api`、`relational-spi`、`relational-core` 与 `relational-integration-spring`；Spring Boot Starter、数据库厂商 Translator、Sharding Integration 仍未实现。

## 1. 组件定位

Relational Access Component 是 Iron Components 面向关系型数据库的轻量访问底座。

它不替代 MyBatis、JPA、jOOQ，也不实现 ORM。它解决基础组件内部重复出现的 JDBC 基础设施问题：SQL 执行、参数绑定、结果映射、Connection 生命周期、DataSource 解析、异常翻译、事务 Connection 参与和可观测扩展。

```text
Business Template
    |
    +--> IdempotencyExecutor / MessageTemplate / TransactionExecutor
    |           |
    |           +--> JdbcXXXStorage
    |                     |
    |                     +--> RelationalTemplate
    |
    +--> Business Repository --> MyBatis / JPA / jOOQ

RelationalTemplate
    -> ConnectionProvider
        -> DefaultConnectionProvider          -> DataSource.getConnection()
        -> SpringTransactionAwareProvider     -> DataSourceUtils / transaction-bound Connection
    -> JDBC
```

## 2. Maven modules

```text
relational-access-component
├── relational-api
├── relational-spi
├── relational-core
├── relational-integration-spring
└── docs
```

### relational-api

技术组件 JDBC Storage Adapter 的稳定公开 API：

- `RelationalTemplate`
- `SqlStatement` / `BatchSqlStatement`
- `SqlParameter`
- `SqlExecutionOptions`
- `SqlRoute`
- `RowMapper<T>`
- `UpdateResult` / `BatchResult` / `GeneratedKey<T>`
- `RelationalAccessException` / `RelationalFailureType`

### relational-spi

只面向 Core / Integration：

- `ConnectionProvider`
- `ConnectionHandle` / `ConnectionOwnership`
- `DataSourceResolver`
- `SqlExceptionTranslator`
- `SqlExecutionListener`
- `SqlExecutionContext` / `SqlExecutionKind`

普通 Storage 不应直接拿这些 SPI。

### relational-core

纯 JDBC 主执行实现：

- `DefaultRelationalTemplate`
- `SqlStatementValidator`
- `JdbcParameterBinder`
- `JdbcStatementConfigurer`
- `DefaultConnectionProvider`
- `DefaultConnectionHandle`
- `SingleDataSourceResolver`
- `StandardSqlExceptionTranslator`

### relational-integration-spring

Spring 本地事务参与桥：

- `SpringTransactionAwareConnectionProvider`
- `SpringConnectionHandle`

它不 begin/commit/rollback；只通过 `DataSourceUtils` 获取和释放 transaction-bound Connection。

## 3. API 分级

### Level A：业务模板依赖高层技术能力

例如：

- `IdempotencyExecutor`
- `MessageTemplate`
- `RetryExecutor`
- `TransactionExecutor / TransactionTemplate`
- 业务自己的 Repository Port

业务模板默认不直接依赖 `RelationalTemplate`。

### Level B：技术组件 Storage Adapter 依赖 relational-api

例如：

```text
JdbcIdempotencyStorage
JdbcOutboxStorage
JdbcTaskStorage
```

它们负责：

```text
领域存储操作
  -> 最终 SQL
  -> 参数
  -> SqlRoute
  -> RelationalTemplate
  -> 解释 UpdateResult / 查询结果
```

### Level C：框架集成依赖 relational-spi

`relational-core`、Spring transaction integration、未来 observability/sharding integration 才依赖 `ConnectionProvider` 等 SPI。

## 4. SQL 的归属

SQL 仍然存在，但不散到 Relational Core。

```text
tryAcquire()                         幂等领域语义
    ↓
JdbcIdempotencyStorage              存储语义
    ↓
UPDATE ... WHERE status = ?          SQL / 物理表语义
    ↓
RelationalTemplate.update(...)      通用关系型执行语义
    ↓
PreparedStatement.executeUpdate()    JDBC 机制
```

Relational Access 不知道 `ownerToken / PROCESSING / Outbox / Order` 是什么。

## 5. 事务边界

Relational Core **不创建事务**。

```text
TransactionExecutor(REQUIRED / REQUIRES_NEW)
        ↓ 建立并绑定 Connection
MyBatis ------------------------------┐
                                      ├── same transaction-bound Connection
RelationalTemplate -> Spring Provider ┘
```

因此 Tx-A / Tx-B / Tx-C 的传播策略仍属于 idempotency + transaction integration，而不是 `RelationalTemplate`。

## 6. v1 明确不做

- ORM / Entity / Repository 自动实现
- `@Table` / `@Column`
- SQL DSL / SQL AST / SQL Parser
- 自动分页 SQL 改写
- 自动重试
- 分库分表算法
- 在 relational-core 内 begin/commit/rollback
- 数据库连接池
- Schema Migration
- MySQL/PostgreSQL vendor-specific Translator（下一阶段按需求增加）
- Spring Boot Starter 自动装配（下一阶段）

## 7. 代码和 UML 阅读入口

从 `docs/README.md` 开始。

最推荐顺序：

```text
class responsibility
  -> core class diagram
  -> update flow
  -> queryOne flow
  -> SQL lifecycle
  -> Connection ownership
  -> Spring shared transaction
  -> Idempotency Tx-A / Tx-B / Tx-C migration
```

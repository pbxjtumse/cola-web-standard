# Relational Access UML Index

当前文档只描述设计边界与接口关系，不代表已有执行实现。

## Component

- `component/00-relational-access-overview.puml`：Iron Components 总体位置，明确业务模板、技术组件 Storage 与 Relational Access 的依赖层级。
- `component/01-api-spi-boundary.puml`：relational-api / relational-spi 契约边界以及未来 core/integration 的位置。

## State

- `state/00-sql-execution-lifecycle.puml`：一次 SQL 执行从 CREATED 到资源释放的完整生命周期。
- `state/01-connection-handle-lifecycle.puml`：OWNED / BORROWED Connection 的所有权状态模型。

## Sequence

- `sequence/00-query-one-flow.puml`：普通 queryOne 的未来执行主链。
- `sequence/01-idempotency-storage-flow.puml`：Idempotency 领域语义如何转换为 SQL/Route，再交给 Relational Access。
- `sequence/02-business-mybatis-shared-transaction.puml`：业务使用 MyBatis、基础组件使用 RelationalTemplate 时如何通过同一事务 Connection 协同。

## 阅读顺序

建议：`00-overview -> 01-api-spi-boundary -> idempotency-storage-flow -> shared-transaction -> state diagrams`。

# foundation-id 集成方式

V1.3 的事务组件不直接依赖 foundation-id 具体 API，而是提供：

```java
TransactionExecutionIdGenerator
```

Starter 默认注册：

```java
UuidTransactionExecutionIdGenerator
```

如果 foundation-id 最终暴露例如：

```java
public interface IdGenerator {
    String nextId(String namespace);
}
```

业务工程可以适配：

```java
@Bean
TransactionExecutionIdGenerator transactionExecutionIdGenerator(IdGenerator idGenerator) {
    return () -> idGenerator.nextId("transaction-execution");
}
```

因为 Starter 使用 `@ConditionalOnMissingBean(TransactionExecutionIdGenerator.class)`，自定义 Bean 会自动替换默认 UUID。

建议 transaction execution ID 使用纯本地或高可用策略，例如 UUID / Snowflake / ULID 类方案；不建议为了一个日志关联 ID 强制访问远端号段服务。

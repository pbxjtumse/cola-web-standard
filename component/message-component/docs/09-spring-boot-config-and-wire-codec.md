# 09 Spring Boot 配置结构与消息线级协议

## 1. AutoConfiguration 分层

message-starter 不再使用一个巨大的 `MessageTemplateAutoConfiguration` 承接所有 Bean，而是按职责拆成五个自动配置类：

```text
message-starter
└── com.xjtu.iron.message.spring.boot.autoconfigure
    ├── MessageCoreAutoConfiguration
    ├── MessageProviderAutoConfiguration
    ├── MessageSendAutoConfiguration
    ├── MessageConsumeAutoConfiguration
    └── MessageTemplateAutoConfiguration
```

分层职责：

| 自动配置类 | 负责内容 |
|---|---|
| `MessageCoreAutoConfiguration` | `MessageProperties`、`Serializer`、`MessageComponentOptions`、`MessageWireCodec`、`MessageContextAccessor`、`MessageEnvelopeEnricher`、消息 ID 生成器 |
| `MessageProviderAutoConfiguration` | `DestinationRouteRegistry`、`DestinationResolver`、`MessageProviderRegistry` |
| `MessageSendAutoConfiguration` | `MessageSendReliabilityOptions`、`MessageSendExecutor` |
| `MessageConsumeAutoConfiguration` | `ConsumeExceptionClassifier`、消费事务执行器、消费幂等执行器、`MessageConsumeExecutor`、`MessageConsumerAdapter` |
| `MessageTemplateAutoConfiguration` | 最终组装 `MessageTemplate` 门面 |

这样拆分以后，starter 的职责边界更清楚：core 对象由 core 配置类负责，发送链由 send 配置类负责，消费链由 consume 配置类负责，最终门面由 template 配置类负责。

## 2. Properties 分包

整个组件仍然只有一个配置绑定入口：

```java
@ConfigurationProperties(prefix = "xjtu.iron.message")
public final class MessageProperties
```

其它配置类都是它下面的嵌套配置对象，不再单独声明 `@ConfigurationProperties`。这可以保证业务配置入口始终只有一个：`xjtu.iron.message.*`。

```text
properties
├── MessageProperties
├── consume
│   ├── MessageConsumeProperties
│   ├── MessageConsumeTransactionProperties
│   └── idempotency
│       └── MessageConsumeIdempotencyProperties
├── reliability
│   ├── MessageReliabilityProperties
│   └── MessageSendReliabilityProperties
├── route
│   └── MessageRouteProperties
└── serializer
    └── MessageSerializerProperties
```

Demo 配置不放在 starter 的 properties 包中。Demo 是业务应用示例，它的配置应直接放在 `message-demo-springboot/application.yml` 或 demo 自己的配置类里。

## 3. application.yml 跳转

IDEA 对 `application.yml` 的跳转依赖 Spring Boot 配置元数据。starter 保留：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

本地执行：

```bash
mvn clean compile -DskipTests
```

之后应生成配置元数据。IDEA 重新加载 Maven 后，`xjtu.iron.message.*`、`xjtu.iron.message.kafka.*`、`xjtu.iron.message.pulsar.*`、`xjtu.iron.message.rocketmq.*` 应能提示和跳转。

## 4. 消息线级协议与 payload 序列化

消息组件刻意区分两层：

```text
Serializer
    只负责 payload <-> byte[]

MessageWireCodec
    负责 MessageEnvelope <-> ProviderSendRequest / ProviderInboundMessage
```

`MessageWireCodec` 负责的线级协议包括：

- `x-iron-message-id`
- `x-iron-message-type`
- `x-iron-schema-version`
- `x-iron-occurred-at`
- `x-iron-created-at`
- `x-iron-source`
- `x-iron-correlation-id`
- `x-iron-causation-id`
- `x-iron-tenant-id`
- `x-iron-destination-namespace`
- `x-iron-destination-name`
- `content-type`

因此 payload 序列化器可以替换，但 `MessageWireCodec` 不能删除。

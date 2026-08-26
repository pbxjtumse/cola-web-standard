# 09 Spring Boot 配置结构与消息线级协议

## 1. 配置类分层

本版本将 Spring Boot 配置类按职责拆分：

```text
message-starter
└── com.xjtu.iron.message.spring.boot.autoconfigure
    ├── MessageAutoConfiguration
    └── properties
        ├── MessageProperties
        ├── MessageRouteProperties
        ├── MessageSerializerProperties
        └── MessageDemoProperties

message-integrations
├── message-integration-kafka
├── message-integration-pulsar
└── message-integration-rocketmq
```

`MessageProperties` 只描述 message-core 的通用配置，例如默认 Provider、应用名、确认超时、路由模式和逻辑目的地路由表。

各 Provider properties 只描述对应 MQ 的原生连接配置，例如 Kafka bootstrapServers、Pulsar serviceUrl、RocketMQ nameServer。

因此 Demo 或业务应用的 `application.yml` 应该放在应用模块中，而不是放在 starter 中。Starter 只提供字段定义和自动装配逻辑。

## 2. application.yml 跳转

IDEA 对 `application.yml` 的跳转依赖 Spring Boot 配置元数据。

当前两个模块都已声明：

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

之后应生成：

```text
META-INF/spring-configuration-metadata.json
```

IDEA 重新加载 Maven 后，`xjtu.iron.message.*`、`xjtu.iron.message.kafka.*`、`xjtu.iron.message.pulsar.*`、`xjtu.iron.message.rocketmq.*` 应能提示和跳转。

## 3. 消息线级协议与 payload 序列化

消息组件刻意区分两层：

```text
MessageSerializer
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

因此后续即使 payload 序列化改为 foundation-component，`MessageWireCodec` 也不能删除。

## 4. foundation-component 序列化接入方式

当前代码保留 `MessageSerializer` 作为消息组件的 payload 序列化端口。

`JacksonMessageSerializer` 已合并到 `message-core.codec`。Starter 默认提供 Jackson 实现，并使用 `@ConditionalOnMissingBean`。因此业务或后续 foundation 序列化适配模块只要提供一个 `MessageSerializer` Bean，默认 Jackson Bean 就会自动让位。

推荐后续接入方式：

```text
foundation-serialization
    ↓
FoundationMessageSerializer implements MessageSerializer
    ↓
MessageWireCodec
    ↓
ProviderSendRequest / ProviderInboundMessage
```

当前版本允许 core 持有默认 Jackson 实现，但不应该让业务代码直接依赖 Jackson。后续如果 foundation-serialization 完全稳定，可以通过 `MessageSerializer` Bean 替换默认实现。

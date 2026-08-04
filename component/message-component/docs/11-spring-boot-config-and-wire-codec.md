# 11 Spring Boot 配置结构与消息线级协议

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

message-integration-pulsar
└── com.xjtu.iron.message.integration.pulsar.autoconfigure
    ├── PulsarMessageAutoConfiguration
    └── PulsarMessageProperties
```

`MessageProperties` 只描述 message-core 的通用配置，例如默认 Provider、应用名、确认超时、路由模式和逻辑目的地路由表。

`PulsarMessageProperties` 只描述 Pulsar Provider 的原生连接配置，例如 serviceUrl、operationTimeout、negativeAckRedeliveryDelay 和 receiverQueueSize。

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

IDEA 重新加载 Maven 后，`xjtu.iron.message.*` 和 `xjtu.iron.message.pulsar.*` 应能提示和跳转。

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

Starter 中默认提供 Jackson 实现，但使用了 `@ConditionalOnMissingBean`。因此业务或后续 `message-codec-foundation` 模块只要提供一个 `MessageSerializer` Bean，默认 Jackson Bean 就会自动让位。

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

不要让 `message-core` 直接依赖 foundation 的具体序列化实现，避免消息核心模块和基础组件实现细节耦合。

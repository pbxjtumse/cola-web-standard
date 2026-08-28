# 07 代码索引

## 1. message-api

### `api.model`

| 类 | 说明 |
|---|---|
| `MessageEnvelope` | 消息聚合根，组合 metadata、context、headers、payload |
| `MessageMetadata` | messageId、messageType、schemaVersion、messageKey、occurredAt、createdAt |
| `MessageContext` | source、correlationId、causationId、tenantId |
| `MessageHeaders` | 不可变消息头集合 |
| `MessageHeaderNames` | 线级系统头常量 |
| `MessageDestination` | namespace、name、providerHint、qualifiedName |

### `api.publish`

| 类 | 说明 |
|---|---|
| `MessagePublisher` | 同步/异步发送入口 |
| `SendOptions` | 单次发送确认等待配置 |
| `SendResult` | 标准发送结果 |
| `SendStatus` | `CONFIRMED / FAILED / REJECTED / UNKNOWN` |
| `SendStage` | 发送失败或结果所处阶段 |
| `SendFailureType` | 标准失败原因 |
| `SendReliabilityInfo` | 二期可靠发送信息：retryId、retryPolicy、retryStatus、attempts |

### `api.consume`

| 类 | 说明 |
|---|---|
| `ConsumerDefinition` | 消费者定义 |
| `MessageConsumerRegistrar` | 消费注册入口 |
| `MessageHandler` | 消费处理回调 |
| `MessageSubscription` | 订阅句柄 |
| `ConsumeContext` | 当前 Provider 投递上下文 |
| `ConsumeDecision` | `ACK / RETRY / DISCARD / DEAD_LETTER` |

### 其他

| 包 | 类 | 说明 |
|---|---|---|
| `api.codec` | `MessageSerializer` | payload 序列化接口 |
| `api.exception` | `MessageException` | API 层统一异常 |
| `api.annotation` | `MessageListener` | 注解式消费入口 |

## 2. message-spi

| 类 | 说明 |
|---|---|
| `MessageProvider` | Provider 最小扩展点 |
| `MessageCapability` | Provider 能力声明 |
| `ProviderDestination` | 已解析物理目的地 |
| `ProviderSendRequest` | Provider 发送请求 |
| `ProviderSendResult` | Provider 标准发送结果 |
| `ProviderInboundMessage` | Provider 入站消息 |
| `ProviderMessageListener` | Provider 到 core 的消息回调 |
| `ProviderSubscriptionRequest` | Provider 订阅请求 |
| `ProviderSubscription` | Provider 订阅句柄 |

## 3. message-core

| 包 | 类 | 说明 |
|---|---|---|
| `core` | `MessageTemplate` | 发送和消费生命周期门面 |
| `core` | `MessageComponentOptions` | 组件运行参数 |
| `core.enrich` | `MessageEnvelopeEnricher` | 补齐 messageId、时间、上下文 |
| `core.id` | `MessageIdGenerator` | 消息 ID 生成接口 |
| `core.id` | `FoundationMessageIdGenerator` | foundation-id 适配器 |
| `core.id` | `UuidMessageIdGenerator` | 本地 fallback ID 生成器 |
| `core.routing` | `DestinationResolver` / `DefaultDestinationResolver` | 逻辑目的地解析 |
| `core.routing` | `DestinationRoute` / `DestinationRouteRegistry` | 显式路由配置 |
| `core.routing` | `DestinationRoutingMode` | STRICT / IMPLICIT_DEFAULT |
| `core.provider` | `MessageProviderRegistry` | Provider 注册和选择 |
| `core.codec` | `MessageWireCodec` | envelope 线级协议编解码 |
| `core.codec` | `JacksonMessageSerializer` | 默认 JSON payload 序列化 |
| `core.context` | `CurrentMessage` | 当前入站消息快照 |
| `core.context` | `MessageContextAccessor` / `ThreadLocalMessageContextAccessor` | 消费作用域上下文 |
| `core.send` | `PreparedMessageSend` | 发送准备完成后的不可变快照 |
| `core.send` | `MessageSendExecutor` | 发送执行器抽象 |
| `core.send` | `DirectMessageSender` | 直发执行器 |
| `core.send` | `MessageSendReliabilityOptions` | core 层可靠发送配置 |
| `core.send.reliability` | `DefaultReliableMessageSender` | 可靠发送执行器 |
| `core.send.reliability` | `MessageSendRetryClassifier` | ProviderSendResult 到 RetryDecision 的分类器 |

## 4. integrations

| Provider | 主要类 | 说明 |
|---|---|---|
| Kafka | `KafkaMessageProvider` | Kafka 发送和消费适配 |
| Pulsar | `PulsarMessageProvider` | Pulsar 发送和消费适配 |
| RocketMQ4 | `RocketMqMessageProvider` | RocketMQ4 Remoting 发送和消费适配 |

## 5. message-starter

| 类 | 说明 |
|---|---|
| `MessageAutoConfiguration` | message-component 自动装配入口 |
| `MessageProperties` | `xjtu.iron.message` 配置根 |
| `MessageSendReliabilityProperties` | `xjtu.iron.message.reliability.send` 配置 |
| `MessageRouteProperties` | routes 配置 |
| `MessageDemoProperties` | demo 配置 |

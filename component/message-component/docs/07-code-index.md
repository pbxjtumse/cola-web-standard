# 07 代码索引

## message-api

- `MessageEnvelope`：消息聚合根，组合 metadata、context、headers、payload。
- `MessageMetadata`：messageId、messageType、schemaVersion、messageKey、时间。
- `MessageContext`：source、correlationId、causationId、tenantId。
- `MessageHeaders`：业务和技术扩展头不可变值对象。
- `MessageHeaderNames`：线级系统头常量。
- `MessageDestination`：namespace、name、providerHint。
- `MessagePublisher`：同步和异步普通发送。
- `MessageConsumerRegistrar`：消费者注册入口。
- `ConsumerDefinition`：单逻辑目的地消费者定义。
- `ConsumeContext`：Provider 投递运行时信息；一期无法统一获得次数时使用基础值 1。
- `ConsumeDecision`：SUCCESS、RETRY。
- `SendOptions`：单次调用确认等待选项。
- `SendResult`、`SendStatus`、`SendStage`、`SendFailureType`：标准发送结果模型。

## message-spi

- `MessageProvider`：Provider 最小 SPI。
- `ProviderDestination`：已解析物理目的地。
- `ProviderSendRequest`：普通不可变类，显式完成发送请求校验和防御性复制。
- `ProviderSendResult`：Provider 标准结果。
- `ProviderInboundMessage`：Provider 原始入站消息。
- `ProviderSubscriptionRequest`、`ProviderSubscription`、`ProviderMessageListener`：订阅 SPI。
- `MessageCapability`：一期基础能力声明。

## message-core

- `MessageTemplate`：发送和消费生命周期编排。
- `MessageEnvelopeEnricher`：补齐消息元数据与父子上下文。
- `MessageWireCodec`：唯一的对象与线级格式编码、解码和契约校验实现。
- `MessageWireMapper`：兼容入口，已标记 Deprecated，内部委托 MessageWireCodec。
- `DestinationResolver`、`DefaultDestinationResolver`：逻辑目的地解析。
- `DestinationRoute`、`DestinationRouteRegistry`：精确路由配置。
- `MessageProviderRegistry`：Provider 注册与选择。
- `MessageContextAccessor`、`ThreadLocalMessageContextAccessor`：同步消费作用域。
- `CurrentMessage`：当前入站消息快照。
- `MessageComponentOptions`：组件运行参数。

## integrations

### Kafka

- `KafkaMessageProvider`
- `KafkaMessageProviderConfig`
- `KafkaMetadataKeys`

### RocketMQ

- `RocketMqMessageProvider`
- `RocketMqMessageProviderConfig`
- `RocketMqMetadataKeys`

### Pulsar

- `PulsarMessageProvider`
- `PulsarMessageProviderConfig`
- `PulsarMetadataKeys`

## testkit / demo

- `InMemoryMessageProvider`
- `InMemoryMessageRecord`
- `InMemoryMetadataKeys`
- `Utf8StringMessageSerializer`
- `InMemoryMessageDemo`
- `MessageModelContractVerifier`

## diagrams

- `docs/diagrams/sequence/L0`：整体生命周期。
- `docs/diagrams/sequence/L1`：core 主流程。
- `docs/diagrams/sequence/L2`：Provider 分支。
- `docs/diagrams/sequence/L3`：异常和边界分支。

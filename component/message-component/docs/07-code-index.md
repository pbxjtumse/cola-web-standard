# 07. 代码索引

## Java 类型

### message-api

- `ConsumeContext` — `message-api/src/main/java/com/xjtu/iron/message/api/ConsumeContext.java`
- `ConsumeDecision` — `message-api/src/main/java/com/xjtu/iron/message/api/ConsumeDecision.java`
- `ConsumerDefinition` — `message-api/src/main/java/com/xjtu/iron/message/api/ConsumerDefinition.java`
- `MessageCategory` — `message-api/src/main/java/com/xjtu/iron/message/api/MessageCategory.java`
- `MessageConsumerRegistrar` — `message-api/src/main/java/com/xjtu/iron/message/api/MessageConsumerRegistrar.java`
- `MessageContext` — `message-api/src/main/java/com/xjtu/iron/message/api/MessageContext.java`
- `MessageDestination` — `message-api/src/main/java/com/xjtu/iron/message/api/MessageDestination.java`
- `MessageEnvelope` — `message-api/src/main/java/com/xjtu/iron/message/api/MessageEnvelope.java`
- `MessageHandler` — `message-api/src/main/java/com/xjtu/iron/message/api/MessageHandler.java`
- `MessageHeaders` — `message-api/src/main/java/com/xjtu/iron/message/api/MessageHeaders.java`
- `MessagePublisher` — `message-api/src/main/java/com/xjtu/iron/message/api/MessagePublisher.java`
- `MessageSerializer` — `message-api/src/main/java/com/xjtu/iron/message/api/MessageSerializer.java`
- `MessageSubscription` — `message-api/src/main/java/com/xjtu/iron/message/api/MessageSubscription.java`
- `SendFailureType` — `message-api/src/main/java/com/xjtu/iron/message/api/SendFailureType.java`
- `SendOptions` — `message-api/src/main/java/com/xjtu/iron/message/api/SendOptions.java`
- `SendResult` — `message-api/src/main/java/com/xjtu/iron/message/api/SendResult.java`
- `SendStage` — `message-api/src/main/java/com/xjtu/iron/message/api/SendStage.java`
- `SendStatus` — `message-api/src/main/java/com/xjtu/iron/message/api/SendStatus.java`

### message-spi

- `MessageCapability` — `message-spi/src/main/java/com/xjtu/iron/message/spi/MessageCapability.java`
- `MessageProvider` — `message-spi/src/main/java/com/xjtu/iron/message/spi/MessageProvider.java`
- `ProviderDestination` — `message-spi/src/main/java/com/xjtu/iron/message/spi/ProviderDestination.java`
- `ProviderInboundMessage` — `message-spi/src/main/java/com/xjtu/iron/message/spi/ProviderInboundMessage.java`
- `ProviderMessageListener` — `message-spi/src/main/java/com/xjtu/iron/message/spi/ProviderMessageListener.java`
- `ProviderSendRequest` — `message-spi/src/main/java/com/xjtu/iron/message/spi/ProviderSendRequest.java`
- `ProviderSendResult` — `message-spi/src/main/java/com/xjtu/iron/message/spi/ProviderSendResult.java`
- `ProviderSubscription` — `message-spi/src/main/java/com/xjtu/iron/message/spi/ProviderSubscription.java`
- `ProviderSubscriptionRequest` — `message-spi/src/main/java/com/xjtu/iron/message/spi/ProviderSubscriptionRequest.java`

### message-core

- `CurrentMessage` — `message-core/src/main/java/com/xjtu/iron/message/core/CurrentMessage.java`
- `DefaultDestinationResolver` — `message-core/src/main/java/com/xjtu/iron/message/core/DefaultDestinationResolver.java`
- `DestinationResolver` — `message-core/src/main/java/com/xjtu/iron/message/core/DestinationResolver.java`
- `DestinationRoute` — `message-core/src/main/java/com/xjtu/iron/message/core/DestinationRoute.java`
- `DestinationRouteRegistry` — `message-core/src/main/java/com/xjtu/iron/message/core/DestinationRouteRegistry.java`
- `DestinationRoutingMode` — `message-core/src/main/java/com/xjtu/iron/message/core/DestinationRoutingMode.java`
- `MessageComponentOptions` — `message-core/src/main/java/com/xjtu/iron/message/core/MessageComponentOptions.java`
- `MessageContextAccessor` — `message-core/src/main/java/com/xjtu/iron/message/core/MessageContextAccessor.java`
- `MessageEnvelopeEnricher` — `message-core/src/main/java/com/xjtu/iron/message/core/MessageEnvelopeEnricher.java`
- `MessageIdGenerator` — `message-core/src/main/java/com/xjtu/iron/message/core/MessageIdGenerator.java`
- `MessageProviderRegistry` — `message-core/src/main/java/com/xjtu/iron/message/core/MessageProviderRegistry.java`
- `MessageTemplate` — `message-core/src/main/java/com/xjtu/iron/message/core/MessageTemplate.java`
- `MessageWireMapper` — `message-core/src/main/java/com/xjtu/iron/message/core/MessageWireMapper.java`
- `ThreadLocalMessageContextAccessor` — `message-core/src/main/java/com/xjtu/iron/message/core/ThreadLocalMessageContextAccessor.java`
- `UuidMessageIdGenerator` — `message-core/src/main/java/com/xjtu/iron/message/core/UuidMessageIdGenerator.java`

### message-codec-jackson

- `JacksonMessageSerializer` — `message-codec-jackson/src/main/java/com/xjtu/iron/message/codec/jackson/JacksonMessageSerializer.java`

### message-integrations/message-integration-kafka

- `KafkaMessageProvider` — `message-integrations/message-integration-kafka/src/main/java/com/xjtu/iron/message/integration/kafka/KafkaMessageProvider.java`
- `KafkaMessageProviderConfig` — `message-integrations/message-integration-kafka/src/main/java/com/xjtu/iron/message/integration/kafka/KafkaMessageProviderConfig.java`

### message-integrations/message-integration-rocketmq

- `RocketMqMessageProvider` — `message-integrations/message-integration-rocketmq/src/main/java/com/xjtu/iron/message/integration/rocketmq/RocketMqMessageProvider.java`
- `RocketMqMessageProviderConfig` — `message-integrations/message-integration-rocketmq/src/main/java/com/xjtu/iron/message/integration/rocketmq/RocketMqMessageProviderConfig.java`

### message-integrations/message-integration-pulsar

- `PulsarMessageProvider` — `message-integrations/message-integration-pulsar/src/main/java/com/xjtu/iron/message/integration/pulsar/PulsarMessageProvider.java`
- `PulsarMessageProviderConfig` — `message-integrations/message-integration-pulsar/src/main/java/com/xjtu/iron/message/integration/pulsar/PulsarMessageProviderConfig.java`

### message-testkit

- `InMemoryMessageProvider` — `message-testkit/src/main/java/com/xjtu/iron/message/testkit/InMemoryMessageProvider.java`
- `InMemoryMessageRecord` — `message-testkit/src/main/java/com/xjtu/iron/message/testkit/InMemoryMessageRecord.java`
- `Utf8StringMessageSerializer` — `message-testkit/src/main/java/com/xjtu/iron/message/testkit/Utf8StringMessageSerializer.java`

### message-demo

- `InMemoryMessageDemo` — `message-demo/src/main/java/com/xjtu/iron/message/demo/InMemoryMessageDemo.java`
- `MessageModelContractVerifier` — `message-demo/src/main/java/com/xjtu/iron/message/demo/MessageModelContractVerifier.java`


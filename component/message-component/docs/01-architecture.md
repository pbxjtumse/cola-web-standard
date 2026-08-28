# 01 总体架构

## 1. 组件定位

message-component 统一的是：

```text
消息模型
发送生命周期
消费生命周期
Provider SPI
发送结果语义
可靠发送编排
Spring Boot 装配
```

它不试图把 Kafka、Pulsar、RocketMQ4 的所有高级能力抹平，而是先抽象一套稳定的公共消息组件基线，再把 Provider 差异隔离在 integration 层和 L4 文档中。

## 2. 总体模块

```text
业务系统 / Demo
  ↓
message-api
  ↓
message-core
  ↓
message-spi
  ↓
message-integrations
  ↓
Kafka / Pulsar / RocketMQ4
```

| 模块 | 职责 |
|---|---|
| `message-api` | 面向业务侧的稳定契约，包括消息模型、发送、消费、序列化接口和异常 |
| `message-spi` | Provider 扩展契约，Kafka/Pulsar/RocketMQ4 都通过 SPI 接入 |
| `message-core` | 生命周期编排、消息补齐、路由、线级编码、Provider 选择、可靠发送 |
| `message-integrations` | 具体 MQ 客户端适配 |
| `message-starter` | Spring Boot 自动装配、配置属性和 Bean 组合 |
| `message-demo-springboot` | 三 Provider 收发验证入口 |

## 3. message-api 分包

```text
com.xjtu.iron.message.api
├── model       # MessageEnvelope / Metadata / Context / Headers / Destination
├── publish     # MessagePublisher / SendOptions / SendResult / SendStatus
├── consume     # ConsumerDefinition / MessageHandler / ConsumeDecision
├── codec       # MessageWireCodec
└── annotation  # MessageListener
```

分包原则：

```text
model：描述消息是什么
publish：描述如何发送、发送结果是什么
consume：描述如何消费、消费结果怎么表达
codec：描述 payload 序列化契约
exception：描述 API 层异常
annotation：描述注解式消费入口
```

## 4. message-core 分包

```text
com.xjtu.iron.message.core
├── MessageTemplate
├── MessageComponentOptions
├── codec
├── context
├── enrich
├── id
├── provider
├── routing
└── send
    └── reliability
```

| 包 | 职责 |
|---|---|
| `core` | 核心门面和组件运行参数 |
| `core.context` | 当前消息上下文、ThreadLocal 消费作用域 |
| `core.routing` | 逻辑目的地到 Provider 物理目的地的路由 |
| `core.codec` | 线级协议编解码和默认 JSON payload 序列化 |
| `foundation-id` | `StringIdGenerator` 统一生成 messageId，message-core 不再维护本地 ID 生成抽象 |
| `core.provider` | Provider 注册表和选择 |
| `core.enrich` | 发送前补齐 messageId、时间、上下文 |
| `core.send` | 发送执行抽象、直发执行器、发送快照 |
| `core.send.reliability` | 可靠发送、retry 接入、发送重试分类 |

## 5. 二期发送链路

```text
MessageTemplate.send()
  -> validate
  -> MessageEnvelopeEnricher
  -> DestinationResolver
  -> MessageProviderRegistry
  -> MessageWireCodec
  -> PreparedMessageSend
  -> MessageSendExecutor
  -> DirectMessageSender / DefaultReliableMessageSender
  -> MessageProvider
  -> SendResult
```

可靠发送启用时：

```text
DefaultReliableMessageSender
  -> RetryExecutor
  -> Provider.send()
  -> ProviderSendResult
  -> MessageSendRetryClassifier
  -> RetryResult
  -> SendResult + SendReliabilityInfo
```

## 6. 关键边界

| 边界 | 决策 |
|---|---|
| `message-api` 是否依赖 retry | 不依赖，`SendReliabilityInfo` 使用字符串表达 retryStatus |
| `message-core` 是否依赖 retry-core | 不依赖，只依赖 `retry-api` |
| `message-starter` 是否静默降级 | 不静默。启用可靠发送但缺少 RetryExecutor 时启动失败 |
| `message-codec-jackson` 是否独立 | 不独立，已合并到 `message-core.codec` |
| `MessageWireCodec` 是否删除 | 不删除。它负责 envelope 线级协议，不等于 JSON 序列化 |
| messageId 是否使用 foundation | 直接使用 foundation-id StringIdGenerator，默认 fallback 使用 NanoIdStringIdGenerator |

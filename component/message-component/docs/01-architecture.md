# 01 总体架构

## 1. 定位

消息组件统一的是消息模型、发送和消费生命周期、结果与决策，不把 Kafka、RocketMQ、Pulsar 的高级能力强行抹平。

```text
业务代码
  ↓
message-api
  ↓
message-core
  ├── core.MessageTemplate
  ├── core.enrich.MessageEnvelopeEnricher
  ├── core.routing.DestinationResolver
  ├── core.codec.MessageWireCodec
  ├── core.id.MessageIdGenerator
  ├── core.provider.MessageProviderRegistry
  └── core.send.MessageSendExecutor
  ↓
message-spi
  ↓
Kafka / RocketMQ / Pulsar Provider
  ↓
Broker
```

## 2. 模块职责

| 模块 | 职责 |
|---|---|
| `message-api` | 业务稳定契约，不依赖具体 MQ，也不依赖 core 实现 |
| `message-spi` | Provider 开发契约和线级数据载体 |
| `message-core` | 丰富、路由、编码、Provider 选择、可靠发送编排、消费作用域 |
| `message-integrations` | Kafka、Pulsar、RocketMQ4 原生客户端适配 |
| `message-starter` | Spring Boot 自动装配和配置属性绑定 |
| `message-demo-springboot` | 可运行示例和契约验证 |

> `message-codec-jackson` 模块已合并进 `message-core.codec`，默认 `JacksonMessageSerializer` 和 `MessageWireCodec` 同包管理。

## 3. message-core 分包

| 包 | 职责 |
|---|---|
| `com.xjtu.iron.message.core` | 核心门面和组件运行参数 |
| `com.xjtu.iron.message.core.context` | 当前消息上下文、ThreadLocal 上下文作用域 |
| `com.xjtu.iron.message.core.routing` | 逻辑目的地到 Provider 物理目的地路由 |
| `com.xjtu.iron.message.core.codec` | 线级协议编解码和默认 JSON payload 序列化 |
| `com.xjtu.iron.message.core.id` | 消息 ID 生成策略与 foundation ID 适配 |
| `com.xjtu.iron.message.core.provider` | Provider 注册表 |
| `com.xjtu.iron.message.core.enrich` | 发送前补齐 messageId、时间、上下文 |
| `com.xjtu.iron.message.core.send` | 发送执行抽象、直发执行器、发送快照 |
| `com.xjtu.iron.message.core.send.reliability` | 可靠发送、retry 接入、发送重试分类 |

## 4. 二期克制原则

- `MessageTemplate` 仍是统一生命周期入口，但发送动作委托给 `MessageSendExecutor`。
- `MessageWireCodec` 独立存在，因为线级契约、序列化和 Provider SPI 都依赖它。
- `JacksonMessageSerializer` 只负责 payload JSON 序列化，不处理 messageId、headers、context 和 destination。
- messageId 通过 `MessageIdGenerator` 生成，生产工程可用 `FoundationMessageIdGenerator` 适配 foundation-component。
- `message-core` 只依赖 `retry-api`，不依赖 retry-core/retry-config 的具体实现。

## 5. 依赖边界

`message-core` 不直接依赖幂等、事务、可观测性和并行组件。二期通过稳定 API 或 Spring Bean 组合，避免基础组件形成循环依赖。

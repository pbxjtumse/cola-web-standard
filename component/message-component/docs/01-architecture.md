# 01 总体架构

## 1. 定位

消息组件统一的是消息模型、发送和消费生命周期、结果与决策，不把 Kafka、RocketMQ、Pulsar 的高级能力强行抹平。

```text
业务代码
  ↓
message-api
  ↓
message-core
  ├── MessageTemplate
  ├── MessageEnvelopeEnricher
  ├── DestinationResolver
  ├── MessageWireCodec
  └── MessageContextAccessor
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
| `message-api` | 业务稳定契约，不依赖具体 MQ |
| `message-spi` | Provider 开发契约和线级数据载体 |
| `message-core` | 丰富、路由、编码、Provider 选择、结果映射、消费作用域 |
| `message-codec-jackson` | JSON payload 序列化 |
| `message-integrations` | 三种原生客户端适配 |
| `message-testkit` | 内存 Provider 和测试记录 |
| `message-demo` | 可运行示例和契约验证 |

## 3. 一期克制原则

一期只提取已经存在独立变化方向的类，不创建大量 Validator、Executor、HandlerRegistry 空壳。

- `MessageTemplate` 仍是唯一生命周期编排者。
- `PreparationException` 保持内部类，因为它只是发送准备阶段的内部控制流。
- `MessageWireCodec` 独立存在，因为线级契约、序列化和 Provider SPI 都依赖它。
- Provider 元数据继续使用开放 Map，但每个集成模块用常量定义键名。

## 4. 依赖边界

`message-core` 不直接依赖幂等、事务、可观测性和并行组件。二期通过 integration 模块连接，避免基础组件形成循环依赖。

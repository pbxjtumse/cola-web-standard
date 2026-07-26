# 05. 三期规划

## 一期：Kafka、RocketMQ、Pulsar 普通消息基础闭环

### 公共模型

- MessageEnvelope
- MessageContext
- MessageDestination
- SendOptions
- SendResult
- ConsumerDefinition
- ConsumeContext
- ConsumeDecision

### 核心生命周期

- 发送前丰富
- 逻辑路由
- Provider 选择
- 能力校验
- 序列化
- 发送确认
- 入站重建
- Handler 调用
- SUCCESS/RETRY 映射

### Provider

- Kafka 普通发布与消费
- RocketMQ 普通发布与 PushConsumer
- Pulsar 普通发布与 Shared Consumer
- InMemory Testkit

### 一期验收标准

- 公共 API 不包含三家原生 SDK 类型；
- 三家 Provider 实现同一普通消息 SPI；
- 发送结果有 CONFIRMED/REJECTED/FAILED/UNKNOWN；
- 父子消息关联和因果关系可重建；
- 逻辑目的地与物理 Topic 解耦；
- 默认严格路由；
- 内存完整闭环和契约验证通过。

## 二期：可靠性增强

### 发送可靠性

- ProducerRetryPolicy
- 可重试与不可重试异常分类
- UNKNOWN 结果处理策略
- 发送事件和审计
- Outbox integration
- Broker/客户端限流与熔断集成

### 消费可靠性

- ConsumerRetryPolicy
- 最大重试次数
- 指数退避与 jitter
- Retry Topic / 重试队列
- Dead Letter
- Poison Message 分类
- 消费超时
- 幂等组件 integration
- 人工补偿入口

### 可观测性

- MessageInterceptor
- OpenTelemetry context propagation
- MDC 白名单传播
- Micrometer 指标
- 发送耗时和成功率
- 消费耗时和失败率
- 重试次数
- 消费积压
- 死信数量

### 工程化

- Spring Boot Starter
- 配置绑定和启动校验
- Testcontainers
- 三种 Broker 集成测试
- 故障注入测试
- 契约兼容测试

## 三期：高级能力

高级能力不塞入一个巨大的 `SendOptions`，而采用 Provider 专属接口。

### RocketMQ

```text
RocketMqFifoPublisher
RocketMqDelayedPublisher
RocketMqTransactionalPublisher
RocketMqFilterSubscription
```

### Kafka

```text
KafkaPartitionPublisher
KafkaTransactionalProcessor
KafkaReplayController
KafkaOffsetController
```

### Pulsar

```text
PulsarKeySharedSubscriber
PulsarDelayedPublisher
PulsarReaderFactory
PulsarTransactionalPublisher
```

### 平台能力

- 多 Provider 灰度迁移
- 双写与核对
- 跨集群路由
- 消息回放
- Schema Registry integration
- 消息管理与补偿控制台

## 为什么一期就做三个 Provider

优点：

- 能尽早发现 API 是否偏向某一家中间件；
- 验证普通消息语义是否真正可移植；
- 避免 RocketMQ 的 Tag、Kafka 的 Offset、Pulsar 的 Subscription 被错误放入公共模型。

代价：

- 一期只能做普通能力；
- 不能同时把每家高级功能做深；
- 必须严格控制公共 API。

这一取舍合理：一期的目标是验证抽象和闭环，不是实现三家功能全集。

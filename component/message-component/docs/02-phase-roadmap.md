# 三期路线规划

## 一期：三种 Provider 基础闭环

### 目标

用 Kafka、RocketMQ、Pulsar 同时验证公共普通消息语义，确保公共 API 不偏向任何一家中间件。

### 公共能力

- `MessageEnvelope<T>`。
- `MessageDestination`。
- `MessagePublisher`。
- `MessageConsumerRegistrar`。
- `MessageSerializer`。
- 同步和异步发送。
- `CONFIRMED / FAILED / REJECTED / UNKNOWN`。
- 基础订阅。
- `SUCCESS / RETRY`。
- Provider 注册和选择。
- 系统消息头。
- 内存测试 Provider。

### Provider 实现

#### Kafka

- Producer 普通发送。
- Headers 和 Key。
- Consumer Group。
- 禁用自动提交。
- SUCCESS 后提交当前记录下一 offset。
- RETRY 时 seek 当前 offset。

#### RocketMQ

- 5.x gRPC Java Client。
- 普通消息 Producer。
- PushConsumer。
- User Properties 和 Message Key。
- SUCCESS/FAILURE 消费结果映射。

#### Pulsar

- 稳定 Java Client。
- BYTES Producer。
- Shared Subscription。
- Properties 和 Key。
- acknowledgeAsync/negativeAcknowledge 映射。

### 一期验收标准

- 三种 Provider 都能使用同一 `MessageTemplate` 发送普通消息。
- 三种 Provider 都能使用同一 `ConsumerDefinition` 处理普通消息。
- 业务代码不引用三家原生消息对象。
- 发送超时不会被错误标记为确定失败。
- 消费 Handler 异常不会被错误 ACK。
- testkit 能在不启动 MQ 时验证 core 生命周期。

## 二期：可靠性增强

### 发送可靠性

- Producer 异常分类器。
- 明确可重试与不可重试异常。
- `UNKNOWN` 结果治理。
- Producer 重试策略。
- 发送审计事件。
- Outbox 集成。
- 消息体大小和消息头约束校验。
- 批量发送。

### 消费可靠性

- `ConsumerRetryPolicy`。
- 最大尝试次数。
- 固定、指数和自定义退避。
- Kafka retry topic/DLT。
- RocketMQ 重试与 DLQ 统一事件。
- Pulsar redelivery 和 dead letter policy。
- `REJECT / DEAD_LETTER` 完整消费决策。
- 消费超时。
- 幂等组件集成。
- 消费执行记录和补偿入口。

### 可观测性

- Micrometer 指标。
- OpenTelemetry Trace 传播。
- MDC 恢复。
- 发送和消费生命周期事件。
- 消费积压、重试、死信和确认延迟指标。
- Kafka rebalance、RocketMQ 消费异常、Pulsar redelivery 监控。

### 配置与工程能力

- `message-config`。
- `message-spring-boot-starter`。
- Provider 自动装配。
- 配置校验。
- JSON 序列化实现。
- Testcontainers 集成测试。

## 三期：高级能力

### RocketMQ

- FIFO 消息与 message group。
- 延时/定时消息。
- 事务消息和事务回查。
- Tag/SQL 过滤。

### Kafka

- 指定 partition。
- seek 和 replay。
- read-process-write 事务。
- 生产幂等参数治理。
- Consumer Rebalance Listener 扩展。
- 大批量和流式消费模式。

### Pulsar

- Key_Shared。
- Exclusive/Failover 订阅。
- 延时投递。
- Reader 和指定 MessageId 回放。
- Pulsar Transaction。
- 多租户 Namespace 映射。

### 平台级能力

- Provider 专属能力接口。
- Native Accessor 逃生口。
- 多 Provider 路由。
- 双写和迁移。
- 跨集群容灾。
- 消息回放平台。
- Schema Registry 与消息兼容性治理。
- 人工补偿和审计后台。

## 关键约束

三期高级能力不能继续堆入一个巨大 `SendOptions`。应采用：

```text
MessagePublisher                  普通公共能力
RocketMqFifoPublisher             RocketMQ 专属能力
RocketMqTransactionalPublisher    RocketMQ 专属能力
KafkaTransactionalProcessor       Kafka 专属能力
KafkaReplayController             Kafka 专属能力
PulsarKeySharedSubscriber          Pulsar 专属能力
PulsarReaderFactory                Pulsar 专属能力
```

只有业务明确使用专属接口时，才接受与某个 Provider 绑定。

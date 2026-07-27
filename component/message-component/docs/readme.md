# 三期设计范围

## 一期：三 Provider 基础闭环

包含：

- Kafka、RocketMQ、Pulsar 普通发送
- 普通消费
- 同步和异步发送
- `CONFIRMED / REJECTED / FAILED / UNKNOWN`
- `SUCCESS / RETRY`
- 逻辑路由
- 序列化
- 父子消息上下文传播
- Testkit 和时序图

一期采用业务成功后再 commit 或 ack 的至少一次兼容基线。

`ConsumeContext` 当前仍保留 `deliveryAttempt` 字段。由于 Kafka 普通消费没有统一可靠的投递次数，`MessageWireCodec` 在无法获得统一值时使用基础值 `1`；RocketMQ 和 Pulsar 的原生重投信息继续保存在：

```java
consumeContext.metadata()
```

例如：

```text
rocketmq.delivery-attempt
pulsar.redelivery-count
```

一期不完成：

- 统一 DeliveryInfo
- 最大重试次数
- Retry Topic
- DLQ
- 幂等
- Outbox
- exactly-once

## 二期：可靠性治理

二期正式处理：

- DeliveryInfo
- 首次投递和重新投递识别
- 最大重试次数
- 退避策略
- Kafka Retry Topic 和 DLT
- RocketMQ retry 和 DLQ
- Pulsar DeadLetterPolicy
- 幂等组件集成
- Outbox
- 毒消息
- 消费超时
- Trace、指标、审计和告警
- Kafka 重平衡安全
- 分区有界并行和批量位点提交

至少一次的基本风险一期就存在，至少一次的完整治理放在二期。

## 三期：高级能力

包含：

- Kafka 事务 read-process-write
- Kafka replay、seek
- RocketMQ FIFO、延时、事务消息
- Pulsar Key_Shared、延时、Reader、事务
- 批量消息
- Provider 原生扩展
- 可选 at-most-once
- Provider 局部 exactly-once

at-most-once 并不是比 at-least-once 更可靠，它只是允许消息丢失以换取不重复，所以作为特殊高级模式处理。

Kafka 的局部 EOS 也不代表数据库、HTTP 外部调用等副作用天然 exactly-once。端到端仍然需要幂等、Outbox 或条件更新。

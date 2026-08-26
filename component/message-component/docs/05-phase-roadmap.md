# 05 阶段路线

## 当前阶段判断

```text
message-component 一期普通收发：已完成
message-component 二期发送可靠性：工程验证阶段
```

一期已经完成 Kafka、Pulsar、RocketMQ4 单独收发，以及三 Provider 同时收发。

二期已经完成设计、代码骨架、API/core 分包、PlantUML 图和 FakeProvider 测试，但还需要本地 Maven 编译、三 MQ 可靠发送联调和异常场景继续验收。

## 当前路线

```text
第 1 步：回到 message-component 二期可靠发送
        ↓
第 2 步：二期前置验收
        ↓
第 3 步：修复工程问题 / 编译问题 / 分包问题
        ↓
第 4 步：三 MQ 可靠发送联调
        ↓
第 5 步：FakeProvider 异常场景测试
        ↓
第 6 步：message-component 二期可靠发送冻结
        ↓
第 7 步：进入 message-component 消费可靠性 或 idempotency 接入
```

## 一期：普通消息基础闭环

目标：验证公共模型可以同时承载 Kafka、Pulsar、RocketMQ4。

包含：

- 统一 MessageEnvelope 模型
- 逻辑目的地和 Provider 路由
- 普通同步/异步发送
- 普通消费
- JSON payload 序列化
- MessageWireCodec 线级协议
- Kafka / Pulsar / RocketMQ4 Provider
- 三 Provider 并行收发 Demo

不包含：

- retry-component 接入
- Outbox
- 消费幂等
- 消费重试治理
- 死信队列

## 二期 A：发送可靠性

目标：让一次 `MessageTemplate.send()` 调用具备有限重试、明确状态、可解释失败原因。

包含：

- `MessageSendExecutor`
- `DefaultReliableMessageSender`
- `RetryExecutor` 接入
- `MessageSendRetryClassifier`
- `SendReliabilityInfo`
- `UNKNOWN` 默认不重试
- `RETRY_EXHAUSTED` 作为失败原因
- Provider 结果映射精修
- FakeProvider 测试

不包含：

- 宕机后补偿发送
- 数据库事务和消息一致性
- Outbox
- 消费可靠性

## 二期 B：消费可靠性

发送可靠性冻结之后再进入。

可能包含：

- 消费失败分类
- 最大消费重试次数
- Kafka Retry Topic / DLT
- RocketMQ retry / DLQ
- Pulsar DeadLetterPolicy
- 幂等组件接入
- 毒消息处理
- 消费并发与背压

## 三期：事务、Outbox 与高级能力

包含：

- Outbox 持久化投递
- 本地事务与消息最终一致性
- 消息补偿扫描
- Provider 高级能力：顺序、延时、事务消息、回放
- 可观测性、Dashboard、告警规则

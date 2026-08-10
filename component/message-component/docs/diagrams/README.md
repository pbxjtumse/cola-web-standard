# message-component PlantUML 图分层说明

本文档约定 message-component 的图示分层，后续 retry、idempotency、distributed-lock、cache、outbox 等技术组件也建议沿用同一规范。

| 层级 | 含义 | 典型内容 |
|---|---|---|
| L0 | 组件总览 / 生命周期总览 | 组件在体系中的位置、完整生命周期、发送到消费的全局闭环 |
| L1 | 核心主流程 | 业务入口到组件出口的正常主链路，不展开过多异常分支 |
| L2 | 核心内部协作 | core 内部模块、策略、扩展点和跨组件协作，例如 retry-component 接入 |
| L3 | 异常 / 边界 / 可靠性流程 | 超时、UNKNOWN、重试耗尽、不可重试、启动保护等边界场景 |
| L4 | 具体 Provider / 中间件实现细节 | Kafka、Pulsar、RocketMQ4 的结果映射和协议差异 |

二期可靠发送后，发送主链路已经从一期的 `MessageTemplate -> Provider.send` 升级为：

```text
MessageTemplate
  -> prepare(validate/enrich/resolve/serialize)
  -> PreparedMessageSend
  -> MessageSendExecutor
  -> DirectMessageSender / DefaultReliableMessageSender
  -> RetryExecutor
  -> MessageProvider
  -> SendResult
```

因此本目录中的 L1/L2/L3/L4 图均已围绕 `MessageSendExecutor`、`DefaultReliableMessageSender`、`RetryExecutor`、`MessageSendRetryClassifier`、`SendResult` 重新更新。

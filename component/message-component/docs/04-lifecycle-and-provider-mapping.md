# 04 生命周期与 Provider 映射

## 1. 发送生命周期

二期以后，发送生命周期分为两个部分：发送前准备和发送执行。

### 发送前准备

```text
VALIDATE
  -> ENRICH
  -> RESOLVE
  -> SERIALIZE
  -> PreparedMessageSend
```

含义：

| 阶段 | 负责对象 | 说明 |
|---|---|---|
| `VALIDATE` | `MessageTemplate` | 校验 destination、message、options |
| `ENRICH` | `MessageEnvelopeEnricher` | 补齐 messageId、createdAt、context |
| `RESOLVE` | `DestinationResolver` / `MessageProviderRegistry` | 解析 ProviderDestination，选择 Provider |
| `SERIALIZE` | `MessageWireCodec` | 将 envelope 编码为 ProviderSendRequest |

### 发送执行

```text
PreparedMessageSend
  -> MessageSendExecutor
  -> DirectMessageSender / DefaultReliableMessageSender
  -> Provider.send
  -> ProviderSendResult
  -> SendResult
```

| 发送执行器 | 使用场景 |
|---|---|
| `DirectMessageSender` | `reliability.send.enabled=false`，保持一期直发逻辑 |
| `DefaultReliableMessageSender` | `reliability.send.enabled=true`，复用 retry-component 做可靠发送 |

## 2. 可靠发送生命周期

```text
CREATE_RETRY_EXECUTION
  -> ATTEMPT
  -> WAIT_CONFIRM
  -> CLASSIFY
  -> SUCCESS / STOP / RETRY / EXHAUSTED
```

`MessageSendRetryClassifier` 负责把 `ProviderSendResult` 转成 `RetryDecision`。

## 3. 消费生命周期

```text
Provider 原生消息
  -> ProviderInboundMessage
  -> MessageWireCodec.decode
  -> CurrentMessage scope
  -> MessageHandler
  -> ConsumeDecision
  -> Provider ack / retry / commit
```

消费可靠性不是本轮重点。目前消费侧仍保持一期语义：业务处理成功后再提交/ACK，失败时按 Provider 当前策略重试。

## 4. Provider 发送结果公共映射

| ProviderSendResult | RetryDecision | 最终 SendResult |
|---|---|---|
| `CONFIRMED + NONE` | `SUCCESS` | `CONFIRMED` |
| `REJECTED + *` | `STOP` | `REJECTED` |
| `FAILED + NETWORK_ERROR` | `RETRY` | 成功则 `CONFIRMED`，耗尽则 `FAILED + RETRY_EXHAUSTED` |
| `FAILED + CLIENT_ERROR` | V1 暂允许短重试 | 成功则 `CONFIRMED`，耗尽则 `FAILED + RETRY_EXHAUSTED` |
| `UNKNOWN + TIMEOUT` | 默认 `STOP` | `UNKNOWN + TIMEOUT` |
| `UNKNOWN + UNKNOWN_OUTCOME` | 默认 `STOP` | `UNKNOWN + UNKNOWN_OUTCOME` |

## 5. Provider 差异隔离原则

Kafka、Pulsar、RocketMQ4 的具体异常、ack、messageId、sendStatus 不能污染 API 层。

```text
Kafka / Pulsar / RocketMQ4 原生结果
  -> Integration 层转换为 ProviderSendResult
  -> Core 层统一转换为 SendResult
```

具体差异放在 `docs/diagrams/sequence/L4` 的 Provider 映射图中维护。

# 11 二期发送可靠性设计

## 1. 二期解决什么

二期发送可靠性解决的是一次业务调用 `MessageTemplate.send()` 时，组件如何做到：

```text
有限重试
明确超时
保留 UNKNOWN
失败原因可解释
结果可观测
```

它不解决：

```text
数据库事务和消息一致性
服务宕机后的补偿发送
Outbox
消费失败重试
消费幂等
死信队列
Exactly Once
```

## 2. 核心链路

```text
业务代码
  -> MessageTemplate.send()
  -> prepare(validate/enrich/resolve/serialize)
  -> PreparedMessageSend
  -> MessageSendExecutor
  -> DefaultReliableMessageSender
  -> RetryExecutor
  -> Provider.send()
  -> ProviderSendResult
  -> MessageSendRetryClassifier
  -> RetryResult
  -> SendResult
```

## 3. 状态模型

| SendStatus | 语义 | 是否成功 | 是否建议立即重发 |
|---|---|---:|---:|
| `CONFIRMED` | Broker 或 Provider 明确确认接收成功 | 是 | 否 |
| `FAILED` | 组件可确认发送失败或 retry 执行失败 | 否 | 看 failureType |
| `REJECTED` | 参数、路由、权限、能力等被明确拒绝 | 否 | 否 |
| `UNKNOWN` | 无法确认 Broker 是否已经收到 | 否 | 默认不建议 |

## 4. 失败原因

| SendFailureType | 场景 | 默认是否重试 |
|---|---|---:|
| `NONE` | 成功 | 否 |
| `NETWORK_ERROR` | 网络连接失败、Broker 临时不可达 | 是 |
| `CLIENT_ERROR` | Provider 客户端本地错误 | V1 允许短重试 |
| `TIMEOUT` | 等待确认超时 | 否，返回 UNKNOWN |
| `UNKNOWN_OUTCOME` | 无法确认 Broker 是否收到 | 否，返回 UNKNOWN |
| `RETRY_EXHAUSTED` | 多次尝试仍未成功 | 否 |
| `RETRY_EXECUTION_ERROR` | retry 基础设施异常 | 否 |
| `SERIALIZATION_ERROR` | payload 或 wire 编码失败 | 否 |
| `ROUTING_ERROR` | 找不到物理目的地或 Provider | 否 |
| `AUTHORIZATION_ERROR` | 权限不足 | 否 |
| `BROKER_REJECTED` | Broker 明确拒绝 | 否 |

## 5. ProviderSendResult 到 RetryDecision

| ProviderSendResult | RetryDecision | 说明 |
|---|---|---|
| `CONFIRMED + NONE` | `SUCCESS` | 明确成功 |
| `REJECTED + *` | `STOP` | 明确拒绝，重试不能修复 |
| `FAILED + NETWORK_ERROR` | `RETRY` | 临时失败，允许短重试 |
| `FAILED + CLIENT_ERROR` | `RETRY` | V1 暂允许短重试，后续继续细分 |
| `UNKNOWN + TIMEOUT` | `STOP` | 默认停止，避免重复消息 |
| `UNKNOWN + UNKNOWN_OUTCOME` | `STOP` | 默认停止，避免重复消息 |
| `UNKNOWN + retryWhenUnknown=true` | `RETRY` | 显式接受重复风险才允许 |

## 6. RetryStatus 到 SendResult

| RetryStatus | SendStatus | SendFailureType | 说明 |
|---|---|---|---|
| `SUCCESS` | `CONFIRMED` | `NONE` | Provider 明确确认 |
| `EXHAUSTED` + last=`FAILED` | `FAILED` | `RETRY_EXHAUSTED` | 多次明确失败后耗尽 |
| `EXHAUSTED` + last=`UNKNOWN` | `UNKNOWN` | `RETRY_EXHAUSTED` | 最后一次未知，保守返回 UNKNOWN |
| `NOT_RETRYABLE` | Provider 原始状态 | Provider 原始 failureType | 停止重试，保留原始语义 |
| `TIMED_OUT` | `UNKNOWN` | `TIMEOUT` | retry 总预算耗尽，不证明 Broker 没收到 |
| `INTERRUPTED` | `UNKNOWN` | `INTERRUPTED` | 中断时不确定 Broker 状态 |
| `CANCELLED` | `FAILED` | `RETRY_EXECUTION_ERROR` | 调用方取消 |
| `ABORTED` | `FAILED` | `RETRY_EXECUTION_ERROR` | 分类器要求中止 |
| `EXECUTION_FAILED` | `FAILED` | `RETRY_EXECUTION_ERROR` | retry 执行器异常 |

## 7. UNKNOWN 默认不重试

UNKNOWN 的含义是：

```text
客户端没有收到明确确认，但 Broker 可能已经收到消息。
```

所以 V1 默认：

```yaml
xjtu:
  iron:
    message:
      reliability:
        send:
          retry-when-unknown: false
```

只有在业务方明确接受重复消息风险，并且后续具备幂等/去重能力时，才考虑打开 `retry-when-unknown=true`。

## 8. 配置

```yaml
xjtu:
  iron:
    message:
      reliability:
        send:
          enabled: true
          retry-policy: message-send
          retry-when-unknown: false
          include-reliability-info: true

    retry:
      enabled: true
      policies:
        message-send:
          max-attempts: 3
          max-duration: 8s
          backoff:
            type: EXPONENTIAL_FULL_JITTER
            initial-delay: 100ms
            max-delay: 1s
            multiplier: 2.0
```

## 9. 验收返回

三 Provider 正常可靠发送通过时，应看到：

```json
{
  "status": "CONFIRMED",
  "reliabilityEnabled": true,
  "retryPolicy": "message-send",
  "retryStatus": "SUCCESS",
  "attempts": 1
}
```

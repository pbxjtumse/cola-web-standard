# message-component 二期：发送可靠性第一版

## 1. 二期解决的问题

二期只解决发送端可靠性增强：业务调用 `MessageTemplate.send()` 后，组件对 Provider 发送进行统一超时、重试、结果分类和结果解释。

二期不解决数据库事务、Outbox、消费失败重试、死信、消费幂等和 Exactly Once。

## 2. 新增核心类

| 模块 | 类 | 作用 |
|---|---|---|
| message-api | `SendReliabilityInfo` | 对外展示 retryId、retryPolicy、retryStatus、attempts、lastFailureCode 等可靠性信息。 |
| message-api | `SendFailureType` | 新增 `RETRY_EXHAUSTED`、`RETRY_EXECUTION_ERROR`、`UNKNOWN_OUTCOME`。 |
| message-api | `SendStage` | 新增 `RETRY`，表示可靠发送编排阶段。 |
| message-core | `PreparedMessageSend` | 将发送前置处理后的快照从 `MessageTemplate` 内部类提升为独立类。 |
| message-core | `MessageSendExecutor` | 发送执行器抽象，隔离直发和可靠发送。 |
| message-core | `DirectMessageSender` | 一期直发逻辑，用于 `reliability.send.enabled=false`。 |
| message-core | `DefaultReliableMessageSender` | 可靠发送核心，调用 retry-component 包装 Provider 发送。 |
| message-core | `MessageSendRetryClassifier` | 将 `ProviderSendResult` 映射成 `RetryDecision`。 |
| message-core | `MessageSendReliabilityOptions` | core 层可靠发送选项。 |
| message-starter | `MessageSendReliabilityProperties` | Spring Boot 配置绑定对象。 |

## 3. 发送链路

```text
业务代码
  -> MessageTemplate.send()
  -> prepare(validate/enrich/route/serialize)
  -> PreparedMessageSend
  -> MessageSendExecutor
  -> DefaultReliableMessageSender
  -> RetryExecutor
  -> Provider.send()
  -> ProviderSendResult
  -> RetryDecision
  -> RetryResult
  -> SendResult
```

## 4. SendStatus 语义

| SendStatus | 含义 | 是否可以当作成功 | 是否建议业务立即重发 |
|---|---|---:|---:|
| `CONFIRMED` | Broker 或 Provider 已明确确认接收成功。 | 是 | 否 |
| `FAILED` | 组件可确认发送失败或 retry 执行失败。 | 否 | 视 failureType 而定 |
| `REJECTED` | 请求被明确拒绝，例如校验、路由、权限或能力不支持。 | 否 | 否 |
| `UNKNOWN` | 组件无法确认 Broker 是否已经收到消息。 | 否 | 默认不建议 |

## 5. SendFailureType 增强

| SendFailureType | 场景 | 默认是否重试 |
|---|---|---:|
| `NETWORK_ERROR` | 网络连接、连接重置、暂时通信失败。 | 是 |
| `CLIENT_ERROR` | Provider 客户端本地临时错误。 | 第一版暂按可重试，后续再细分 |
| `TIMEOUT` | 等待确认超时，Broker 可能已经收到。 | 否，返回 UNKNOWN |
| `UNKNOWN_OUTCOME` | Provider 异常导致无法确认最终结果。 | 否，返回 UNKNOWN |
| `BROKER_REJECTED` | Broker 明确拒绝。 | 否 |
| `SERIALIZATION_ERROR` | 序列化失败。 | 否 |
| `ROUTING_ERROR` | 目的地路由失败。 | 否 |
| `PROVIDER_NOT_FOUND` | Provider 不存在。 | 否 |
| `UNSUPPORTED_CAPABILITY` | Provider 不支持发布能力。 | 否 |
| `RETRY_EXHAUSTED` | 多次尝试后仍未确认成功。 | 已经耗尽 |
| `RETRY_EXECUTION_ERROR` | retry-component 自身执行失败。 | 否 |

## 6. ProviderSendResult 到 RetryDecision 映射

| ProviderSendResult | RetryDecision | 说明 |
|---|---|---|
| `CONFIRMED` | `SUCCESS` | 明确成功。 |
| `REJECTED` | `STOP` | 明确拒绝，不重试。 |
| `UNKNOWN` + `retryWhenUnknown=false` | `STOP` | 默认避免重复消息。 |
| `UNKNOWN` + `retryWhenUnknown=true` | `RETRY` | 显式接受重复投递风险后才允许。 |
| `FAILED + NETWORK_ERROR` | `RETRY` | 网络类临时失败。 |
| `FAILED + CLIENT_ERROR` | `RETRY` | 第一版按临时客户端错误处理。 |
| `FAILED + 其他不可重试类型` | `STOP` | 参数、路由、序列化、权限等问题。 |

## 7. RetryStatus 到 SendResult 映射

| RetryStatus | SendStatus | SendFailureType | 说明 |
|---|---|---|---|
| `SUCCESS` | `CONFIRMED` | `NONE` | 使用最后一次 Provider 成功结果。 |
| `EXHAUSTED` + last=`FAILED` | `FAILED` | `RETRY_EXHAUSTED` | 多次明确失败后耗尽。 |
| `EXHAUSTED` + last=`UNKNOWN` | `UNKNOWN` | `RETRY_EXHAUSTED` | 最后一次未知时保守返回 UNKNOWN。 |
| `NOT_RETRYABLE` | Provider 原始状态 | Provider 原始 failureType | 分类器停止，保留原始语义。 |
| `TIMED_OUT` | `UNKNOWN` | `TIMEOUT` | retry 总预算耗尽，不证明 Broker 未收到。 |
| `INTERRUPTED` | `UNKNOWN` | `INTERRUPTED` | 中断时不确定 Broker 状态。 |
| `CANCELLED` | `FAILED` | `RETRY_EXECUTION_ERROR` | 调用方取消。 |
| `ABORTED` | `FAILED` | `RETRY_EXECUTION_ERROR` | 分类器要求中止。 |
| `EXECUTION_FAILED` | `FAILED` | `RETRY_EXECUTION_ERROR` | retry 基础设施异常。 |

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
          operation-safety: IDEMPOTENCY_PROTECTED
          safety-mode: WARN
          traverse-causes: true
          max-cause-depth: 8
          backoff:
            type: EXPONENTIAL_FULL_JITTER
            initial-delay: 100ms
            max-delay: 1s
            multiplier: 2.0
```

## 9. 启动保护

| 配置 | 行为 |
|---|---|
| `reliability.send.enabled=false` | 使用 `DirectMessageSender`，不依赖 retry-component。 |
| `reliability.send.enabled=true` 且存在 `RetryExecutor`、`RetryPolicyRegistry` | 使用 `DefaultReliableMessageSender`。 |
| `reliability.send.enabled=true` 但缺少 retry Bean | 启动失败，防止业务误以为可靠发送已生效。 |

## 10. PlantUML 图

- `docs/diagrams/phase2-send-reliability-sequence.puml`
- `docs/diagrams/phase2-send-reliability-state.puml`

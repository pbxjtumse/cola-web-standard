# message-component 二期发送可靠性最小支持说明

## 一、结论

`retry-component` V1 不继续扩展异步、持久化、死信或分布式重试。当前版本只作为：

```text
进程内、同步、短时间、有限次数、可观测的通用重试执行器
```

这已经足够支撑 `message-component` 二期的发送可靠性增强。

## 二、RetryStatus 是否需要 UNKNOWN

不需要。

`RetryStatus` 描述的是 retry 执行器自己的终态：

```text
SUCCESS
EXHAUSTED
NOT_RETRYABLE
TIMED_OUT
INTERRUPTED
CANCELLED
ABORTED
EXECUTION_FAILED
```

`UNKNOWN` 是消息发送语义，表示 Provider 调用结束后无法确认 Broker 是否已经收到消息。这个状态应该留在 `message-component` 的发送结果模型里，而不是放进 `retry-component`。

## 三、message-send 推荐策略

```yaml
xjtu:
  iron:
    retry:
      policies:
        message-send:
          max-attempts: 3
          max-duration: 5s
          operation-safety: IDEMPOTENCY_PROTECTED
          safety-mode: WARN
          traverse-causes: true
          max-cause-depth: 8
          retry-failure-category: TRANSIENT
          retry-failure-code: MESSAGE_SEND_RETRYABLE_FAILURE
          retry-on:
            - java.io.IOException
            - java.util.concurrent.TimeoutException
          stop-on:
            - java.lang.IllegalArgumentException
          abort-on:
            - java.lang.SecurityException
          backoff:
            type: EXPONENTIAL_FULL_JITTER
            initial-delay: 100ms
            max-delay: 1s
            multiplier: 2.0
```

## 四、边界

`retry-component` 只负责回答：

```text
这次操作失败后，是否需要短时间内再尝试一次？
```

它不负责回答：

```text
Kafka/Pulsar/RocketMQ Broker 是否已经收到消息？
ProviderSendResult 是否应该被解释为 UNKNOWN？
重试耗尽后是否进入 Outbox 或补偿表？
消费失败是否进入 DLQ？
```

这些属于 `message-component`、`idempotency-component` 和后续 `outbox` 能力。

## 五、message-component 推荐映射

```text
RetryStatus.SUCCESS
  -> 使用最后一次 ProviderSendResult 映射为 CONFIRMED

RetryStatus.EXHAUSTED
  -> 如果最后一次发送结果是 UNKNOWN，则 SendStatus.UNKNOWN + SendFailureType.RETRY_EXHAUSTED
  -> 否则 SendStatus.FAILED + SendFailureType.RETRY_EXHAUSTED

RetryStatus.NOT_RETRYABLE
  -> 使用最后一次 ProviderSendResult 原始语义

RetryStatus.TIMED_OUT / INTERRUPTED
  -> SendStatus.UNKNOWN

RetryStatus.EXECUTION_FAILED
  -> SendStatus.FAILED + SendFailureType.RETRY_EXECUTION_ERROR
```

V1 默认不对 `UNKNOWN` 继续重试，避免在没有幂等和 Outbox 的情况下制造重复消息。

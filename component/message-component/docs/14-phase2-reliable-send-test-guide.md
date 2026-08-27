# 14. 二期可靠发送测试说明

本文档记录二期可靠发送冻结前需要通过的核心测试范围。

## 1. 正常路径

真实 MQ 联调已经覆盖 Kafka、Pulsar、RocketMQ4 三个 Provider 的同步发送确认路径。期望结果为：

```text
status=CONFIRMED
failureType=NONE
stage=COMPLETE
reliabilityEnabled=true
retryStatus=SUCCESS
attempts=1
```

## 2. FakeProvider 异常路径

真实 MQ 很难稳定制造所有异常，因此 message-core 使用 `ScriptedFakeMessageProvider` 构造可重复的 Provider 结果序列。

已覆盖场景：

| 场景 | 期望 |
|---|---|
| 第一次 `FAILED + NETWORK_ERROR`，第二次 `CONFIRMED` | 最终 `CONFIRMED`，`attempts=2` |
| 持续 `FAILED + NETWORK_ERROR` | 最终 `FAILED + RETRY_EXHAUSTED`，`attempts=3` |
| `UNKNOWN + TIMEOUT` 且默认配置 | 不重试，最终 `UNKNOWN` |
| `UNKNOWN + TIMEOUT` 且 `retryWhenUnknown=true` | 可以重试，但该配置需要业务接受重复消息风险 |
| `REJECTED + ROUTING_ERROR` | 不重试，最终 `REJECTED` |
| Provider Future 以 `IOException` 异常完成 | 映射为 `FAILED + NETWORK_ERROR`，允许有限重试 |
| Provider 返回 null CompletionStage | 映射为 `FAILED + CLIENT_ERROR`，允许有限重试 |
| 成功场景可靠性信息 | `lastFailureCode` 和 `lastFailureCategory` 均为空字符串 |

## 3. Starter 装配保护

`message-starter` 需要保证：

1. `reliability.send.enabled=true` 时，如果缺少 `RetryExecutor` 或 `RetryPolicyRegistry`，必须启动失败。
2. `reliability.send.enabled=false` 时，即使没有 retry-component Bean，也应该使用 `DirectMessageSender`。

这个保护避免配置上看似启用可靠发送，实际却静默退回一期直发。

## 4. 建议执行命令

```bash
mvn -pl :message-core -am test -Dtest=DefaultReliableMessageSenderTest,MessageSendRetryClassifierTest
mvn -pl :message-starter -am test -Dtest=MessageAutoConfigurationTest
mvn -pl :message-demo-springboot -am clean package -DskipTests
```

如果需要全量验证：

```bash
mvn clean test
```

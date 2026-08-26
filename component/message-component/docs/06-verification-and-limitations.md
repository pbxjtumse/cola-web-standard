# 06 验证与限制

## 1. 已验证

### 一期普通收发

```text
Kafka 单独收发：通过
Pulsar 单独收发：通过
RocketMQ4 单独收发：通过
Kafka + Pulsar + RocketMQ4 同时收发：通过
```

三 Provider 并行验证中，`POST /demo/messages/send/all` 返回三条 `CONFIRMED`，`GET /demo/messages/received-summary` 返回 Kafka、Pulsar、RocketMQ 各 1 条，说明普通发送和普通消费闭环已经成立。

### 二期可靠发送代码层验证

当前版本已经补充可靠发送核心测试：

```text
message-core/src/test/java/com/xjtu/iron/message/core/send/reliability/
├── MessageSendRetryClassifierTest.java
└── DefaultReliableMessageSenderTest.java
```

覆盖：

- `CONFIRMED -> SUCCESS`
- `FAILED + NETWORK_ERROR -> RETRY`
- `UNKNOWN + TIMEOUT -> STOP`
- `UNKNOWN + retryWhenUnknown=true -> RETRY`
- `REJECTED -> STOP`
- 第一次网络失败、第二次成功
- 多次网络失败后 `RETRY_EXHAUSTED`
- UNKNOWN 默认不重试

## 2. 未完成验证

二期可靠发送还需要在本地完整验证：

```bash
mvn -pl component/message-component/message-demo-springboot -am clean package -DskipTests
```

或者在 message-component 聚合根目录：

```bash
mvn -pl :message-demo-springboot -am clean package -DskipTests
```

随后重新验证：

```http
POST /demo/messages/send/all
GET  /demo/messages/received-summary
```

期望每个 Provider 返回：

```json
{
  "status": "CONFIRMED",
  "reliabilityEnabled": true,
  "retryStatus": "SUCCESS",
  "attempts": 1
}
```

## 3. 当前限制

| 限制 | 说明 |
|---|---|
| 不保证端到端 Exactly Once | 还没有 Outbox、幂等消费和事务组件接入 |
| UNKNOWN 默认不重试 | 避免 Broker 可能已收到消息时重复发送 |
| 只做进程内短重试 | 服务宕机后的补偿发送留给 Outbox |
| 消费可靠性未展开 | 当前消费仍保持一期 ACK/commit 基线 |
| Provider 映射还需实测 | Kafka/Pulsar/RocketMQ4 异常映射需要结合真实集群继续校验 |
| Maven 编译需本地执行 | 当前交付环境不具备完整 Maven/Jackson/Spring/MQ 客户端依赖 |

## 4. 验收标准

二期发送可靠性冻结前必须满足：

```text
1. 全工程 Maven 编译通过
2. message-core 可靠发送测试通过
3. 三 Provider send/all 在可靠发送链路下通过
4. SendMessageResponse 展示 reliabilityEnabled=true
5. UNKNOWN 默认不重试测试通过
6. RETRY_EXHAUSTED 映射测试通过
7. docs/diagrams 与代码链路一致
```

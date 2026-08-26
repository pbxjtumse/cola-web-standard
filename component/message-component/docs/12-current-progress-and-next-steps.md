# 12 当前进度与下一步

## 1. 当前完成度

```text
message-component 一期普通收发：100%
message-component 二期发送可靠性：约 75% ~ 80%
```

已经完成：

- 三 Provider 普通收发验证
- API 分包
- core 分包
- 可靠发送设计
- `MessageSendExecutor` 接入
- `DefaultReliableMessageSender` 接入 retry-component
- `UNKNOWN` 默认不重试
- `SendReliabilityInfo`
- PlantUML L0-L4 图集
- FakeProvider 可靠发送测试
- Provider 映射第一轮精修

待完成：

- 本地 Maven 完整编译
- 三 MQ 可靠发送链路重新联调
- Provider 异常映射继续结合真实集群校验
- 二期发送可靠性冻结

## 2. 当前路线

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

## 3. 立刻要做的验证

```bash
mvn -pl component/message-component/message-demo-springboot -am clean package -DskipTests
```

或者：

```bash
mvn -pl :message-demo-springboot -am clean package -DskipTests
```

然后执行：

```http
POST /demo/messages/send/all
GET  /demo/messages/received-summary
```

期望：

```text
Kafka     CONFIRMED retryStatus=SUCCESS attempts=1
Pulsar    CONFIRMED retryStatus=SUCCESS attempts=1
RocketMQ4 CONFIRMED retryStatus=SUCCESS attempts=1
```

## 4. 下一阶段不要急着做什么

在二期发送可靠性冻结前，不建议展开：

- 消费可靠性
- Outbox
- 事务消息
- 死信队列
- 幂等消费接入
- 监控大盘

原因是发送可靠性还需要最后一轮编译与联调验收。

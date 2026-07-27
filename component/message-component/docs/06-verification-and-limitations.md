# 06 验证与限制

## 已验证

`verify-core.sh` 已使用：

```text
javac --release 17 -Xlint:all -Werror
```

验证模块：

```text
message-api
message-spi
message-core
message-testkit
message-demo
```

验证场景：

- 根消息默认 correlationId
- source 可选行为
- messageId 与 messageKey 不同语义
- 父子消息 correlationId/causationId 传播
- 严格路由拒绝
- 系统头保护
- 逻辑目的地串线拒绝

## 未完成

当前环境没有 Maven 和外部依赖缓存，因此没有完成三种真实 Provider 的全模块 Maven 编译和真实 Broker 集成测试。

需要在本地执行：

```bash
mvn clean verify
```

并分别连接 Kafka、RocketMQ 5.x、Pulsar 集群。

## 一期已知限制

- Kafka Handler 在 poll 线程同步执行。
- Kafka 每条成功消息执行一次同步位点提交，吞吐不是最终形态。
- Kafka RETRY 的本地 sleep 会阻塞 poll 线程。
- 没有最大重试次数、Retry Topic 和 DLQ。
- 没有统一 deliveryAttempt。
- 没有幂等、Outbox、事务、顺序、延时和回放。

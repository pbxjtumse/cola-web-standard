# 06. 验证结果与已知限制

## 1. 已完成验证

通过 `verify-core.sh` 和 `verify-core.ps1`，使用 Java 17 编译：

- message-api
- message-spi
- message-core
- message-testkit
- message-demo

实际运行通过：

1. 根消息发送并得到 CONFIRMED；
2. 内存消费者收到根消息；
3. Handler 内发送子消息；
4. 子消息继承 correlationId；
5. 子消息 causationId 等于父消息 messageId；
6. applicationName 为空时不生成 source；
7. 根消息缺少 correlationId 时默认等于 messageId；
8. STRICT 模式缺少路由时返回 REJECTED + ROUTING_ERROR；
9. 用户不能覆盖 `x-iron-message-*` 系统头；
10. 入站逻辑目的地必须和 ConsumerDefinition 一致。

## 2. Provider 代码检查

三种 Provider 已按各自官方客户端 API 结构编写，并完成 Java 源码语法检查和公共 SPI 对照。

当前执行环境没有 Maven，也无法访问外部制品仓库，因此没有在该环境中完成：

- 三个外部客户端依赖的完整 Maven 编译；
- 真实 Kafka 集群集成测试；
- 真实 RocketMQ 5.x Proxy 集成测试；
- 真实 Pulsar 集群集成测试。

拿到工程后必须在可访问 Maven Central 的环境执行：

```bash
mvn clean verify
```

然后分别进行真实 Broker 验证。

## 3. 一期已知限制

### 毒消息可能无限重试

反序列化失败、逻辑目的地不匹配、Handler 异常目前都返回 RETRY。二期完成最大次数和死信前，不适合直接承载无法人工干预的生产毒消息。

### 没有幂等承诺

组件采用至少一次取向。ACK 丢失、网络异常和客户端重启都可能带来重复消息。业务必须暂时自行保证幂等，二期再接入独立幂等组件。

### ThreadLocal 只覆盖同步 Handler 调用栈

Handler 内自行提交异步线程后，父消息上下文不会自动跨线程传播。二期需要与 concurrency-component 的上下文传播扩展集成。

### Kafka 重试是本地 seek

一期仅适合验证闭环：

- 失败分区会被阻塞；
- 没有 Retry Topic；
- 没有最大次数；
- 没有 delivery attempt 标准字段。

### RocketMQ 依赖 5.x gRPC 接入

该 Provider 不是旧版 Remoting `DefaultMQProducer` 适配器，需要正确的 RocketMQ 5.x endpoints/Proxy 配置，并在 Producer 创建时声明 Topic。

### Pulsar 使用 Shared

一期不承诺相同 key 的顺序。需要 key 局部顺序时应在三期实现 Key_Shared 专属订阅。

## 4. 下一步验证顺序

1. Maven 全模块编译；
2. Kafka 单 Broker 普通发送消费；
3. Kafka 多分区失败 offset 验证；
4. RocketMQ 5.x Proxy 普通发送消费；
5. RocketMQ 未声明 Topic 拒绝验证；
6. Pulsar Shared 多消费者负载验证；
7. Pulsar ACK 失败重投验证；
8. 三家重复投递场景；
9. 进程终止和恢复；
10. 网络超时下 SendStatus.UNKNOWN 验证。

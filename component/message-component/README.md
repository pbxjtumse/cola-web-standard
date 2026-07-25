# message-component 第一版

## 1. 这一版解决什么问题

这一版不是对 `KafkaProducer`、RocketMQ `Producer`、Pulsar `Producer` 简单再包装一层，而是先建立一套稳定的普通消息生命周期：

```text
业务对象
  -> MessageEnvelope
  -> MessageTemplate 校验与系统字段增强
  -> MessageSerializer 序列化
  -> MessageProviderRegistry 选择 Provider
  -> Kafka / RocketMQ / Pulsar 发送
  -> ProviderSendResult
  -> SendResult
```

消费链路为：

```text
Kafka / RocketMQ / Pulsar 原生消息
  -> ProviderInboundMessage
  -> MessageTemplate 反序列化
  -> MessageHandler
  -> ConsumeDecision.SUCCESS / RETRY
  -> 原生 ACK、offset commit、negative acknowledgement 或重新投递
```

## 2. 第一期范围

第一版代码已经包含以下基础闭环：

- Kafka、RocketMQ、Pulsar 三种 Provider 的普通消息发送。
- 同步发送与异步发送。
- `CONFIRMED / FAILED / REJECTED / UNKNOWN` 标准发送状态。
- 普通消息订阅。
- `SUCCESS / RETRY` 基础消费决策。
- 业务对象序列化 SPI。
- Provider 选择与能力校验。
- 系统消息头增强。
- 内存 Provider、字符串序列化器和可运行 Demo。

第一期明确不做：

- 业务级重试策略、重试 Topic 和重试队列。
- 死信统一治理。
- 消费幂等。
- Outbox。
- 事务消息。
- FIFO、严格顺序和 Key_Shared 顺序语义。
- 延时或定时消息。
- 消息回放和 Seek。
- Spring Boot 自动装配。
- 动态多 Provider 路由。

这些能力过早进入公共 API，会导致三种中间件语义被错误抹平。

## 3. 模块结构

```text
message-component
├── message-api
│   ├── 业务侧稳定 API
│   └── Provider 最小 SPI
├── message-core
│   ├── MessageTemplate
│   └── MessageProviderRegistry
├── message-integrations
│   ├── message-integration-kafka
│   ├── message-integration-rocketmq
│   └── message-integration-pulsar
├── message-testkit
│   ├── InMemoryMessageProvider
│   └── StringMessageSerializer
└── message-demo
    └── InMemoryMessageDemo
```

第一版没有单独建立 `message-provider-spi` 模块。SPI 暂时放在 `message-api` 的 `api.spi` 包中，因为 API 和 SPI 总量都很小。等到 SPI 需要独立版本演进或第三方 Provider 开发包时再拆模块，而不是现在为了目录整齐增加理解成本。

## 4. 最小使用方式

```java
InMemoryMessageProvider provider = new InMemoryMessageProvider();
MessageProviderRegistry registry = new MessageProviderRegistry(List.of(provider));
MessageTemplate template = new MessageTemplate(
        registry,
        new StringMessageSerializer(),
        "order-service");

MessageDestination destination = MessageDestination.of("memory", "order-paid");

MessageConsumer consumer = template.subscribe(new ConsumerDefinition<>(
        destination,
        "order-consumer-group",
        String.class,
        (payload, context) -> ConsumeDecision.SUCCESS));

SendResult result = template.send(
        destination,
        MessageEnvelope.of("OrderPaid", "orderId=10001").withKey("10001"),
        SendOptions.defaults());
```

## 5. 三种 Provider 的第一版映射

| 公共语义 | Kafka | RocketMQ | Pulsar |
|---|---|---|---|
| 逻辑目的地 | Topic | Topic | Topic |
| 消息键 | Record Key | Message Key | Message Key |
| 消息头 | Headers | User Properties | Properties |
| 成功确认 | Producer callback metadata | SendReceipt | MessageId |
| 消费组 | group.id | consumerGroup | subscriptionName |
| 成功消费 | 手动提交 offset | ConsumeResult.SUCCESS | acknowledgeAsync |
| 失败重投 | seek 当前 offset | ConsumeResult.FAILURE | negativeAcknowledge |
| 第一版消费模式 | Consumer Group | PushConsumer | Shared Subscription |

## 6. 注释原则

用户要求“每一行代码都需要有注释”。这一版按照审阅版标准处理：

- 每个类型、字段、方法、分支和关键语句都有中文注释。
- 没有给 `package`、`import`、单独的大括号和空行添加无意义注释。
- 注释重点解释设计意图、状态语义和为什么这样做，而不是机械复述 Java 语法。

在正式合并生产代码前，建议保留类型和方法级注释，删除显而易见的逐语句注释，否则代码噪声会长期增加维护成本。

## 7. 本地验证

当前环境没有 Maven 依赖下载能力，因此仓库提供了只依赖 JDK 的核心验证脚本。它会编译：

- `message-api`
- `message-core`
- `message-testkit`
- `message-demo`

并运行 `InMemoryMessageDemo`。

Linux/macOS：

```bash
./verify-core.sh
```

Windows PowerShell：

```powershell
./verify-core.ps1
```

三种外部 Provider 的源代码按照各自官方 Java Client API 编写，但仍需要在你的 Maven 环境中执行完整 `mvn clean verify`，并连接真实或容器化集群完成集成测试。

## 8. 推荐阅读顺序

1. `docs/01-architecture.md`
2. `message-api`
3. `message-core/MessageTemplate.java`
4. `message-testkit/InMemoryMessageProvider.java`
5. 三个 `message-integration-*`
6. `docs/02-phase-roadmap.md`
7. `docs/03-design-decisions.md`

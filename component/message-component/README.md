# Iron Message Component V2

面向业务系统的统一消息访问与生命周期治理组件。

这一版不是对 Kafka、RocketMQ、Pulsar 客户端做同名方法包装，而是把三种中间件中真正稳定的普通消息语义收敛为公共模型，同时通过 Provider SPI 保留物理中间件差异。

## 1. 为什么重做 V2

上一版存在一个实质问题：设计说明里描述了 `source`、`correlationId`、`causationId`、逻辑目的地等概念，但代码没有完整实现这些概念，导致“文档是一套模型，代码是另一套模型”。

V2 不在旧代码上追加字段，而是重新划分四个层次：

1. 业务消息信封：稳定、跨 Provider 的业务语义。
2. 消息传播上下文：来源、业务关联、直接因果、租户。
3. 用户与技术消息头：可扩展传播信息，例如 traceparent、tracestate、baggage、MDC 白名单。
4. 物理 Provider 路由：Kafka Topic、RocketMQ Topic、Pulsar Topic 等实际资源。

## 2. 一期边界

一期同时提供以下三个 Provider 的普通消息基础闭环：

- Kafka
- RocketMQ 5.x gRPC Java Client
- Pulsar

公共闭环包括：

- 统一不可变消息信封；
- 逻辑目的地；
- 精确路由；
- 同步发送；
- 异步发送；
- 发送确认结果；
- 普通订阅消费；
- `SUCCESS / RETRY` 消费决策；
- 消息体序列化；
- Provider 注册与能力校验；
- 当前入站消息上下文传播；
- 内存 Provider 和契约验证程序。

一期刻意不承诺：

- 业务级重试策略；
- 最大重试次数；
- 死信闭环；
- 消费幂等；
- Outbox；
- 事务消息；
- 严格顺序消息；
- 延时消息；
- Kafka 回放与 seek 管理 API；
- Pulsar Key_Shared、Reader、事务；
- Spring Boot 自动装配。

这些能力分别进入二期和三期，避免一期出现大量只有接口、没有完整语义的空壳抽象。

## 3. 模块结构

```text
message-component-v2
├── message-api
│   └── 业务可见的稳定公共契约
├── message-spi
│   └── Provider 实现所需的最小线级 SPI
├── message-core
│   ├── 发送与消费生命周期
│   ├── 消息丰富
│   ├── 逻辑路由
│   ├── Provider 选择
│   └── 线级映射
├── message-codec-jackson
│   └── Jackson JSON 序列化器
├── message-integrations
│   ├── message-integration-kafka
│   ├── message-integration-rocketmq
│   └── message-integration-pulsar
├── message-testkit
│   └── 内存 Provider 与 UTF-8 字符串序列化器
├── message-demo
│   ├── 完整父子消息链路示例
│   └── 公共模型契约验证
└── docs
    ├── 01-architecture.md
    ├── 02-message-model.md
    ├── 03-destination-routing.md
    ├── 04-lifecycle-and-provider-mapping.md
    ├── 05-phase-roadmap.md
    └── 06-verification-and-limitations.md
```

## 4. `MessageEnvelope` 最终字段

```java
MessageEnvelope<T>
├── messageId
├── messageType
├── schemaVersion
├── payload
├── key
├── context
│   ├── source
│   ├── correlationId
│   ├── causationId
│   └── tenantId
├── headers
├── occurredAt
└── createdAt
```

稳定字段没有全部塞入 `headers`。原因是：

- 这些字段有明确、长期稳定的业务语义；
- core 需要对它们执行自动补齐和继承；
- 监控、审计、幂等和链路分析需要结构化访问；
- 如果全部放进 Map，业务容易拼错名称、覆盖系统值或产生类型不一致。

`headers` 继续承担开放扩展，包括：

- `traceparent`
- `tracestate`
- `baggage`
- 允许传播的 MDC 字段
- 业务扩展头

组件保留 `x-iron-message-*` 前缀，业务代码不能伪造这些系统头。

## 5. `source` 的规则

`source` 表示“当前这条消息由哪个应用生产”，不是整个业务链路最初来自哪个系统。

优先级：

1. 业务在 `MessageContext` 中显式指定；
2. 使用 `MessageComponentOptions.applicationName`；
3. 两者都没有时保持为空。

子消息不会继承父消息的 `source`。例如：

```text
order-service 产生 M1：source=order-service
points-service 消费 M1 后产生 M2：source=points-service
notification-service 消费 M2 后产生 M3：source=notification-service
```

这可以准确表示每一跳真正的生产者，不能把 M1 的来源一路继承到 M3。

## 6. `correlationId` 与 `causationId`

假设一个订单流程依次产生三条消息：

```text
M1 OrderPaid
M2 PointsGranted
M3 NotificationRequested
```

推荐关系：

```text
M1
messageId     = M1
correlationId = order-10001
causationId   = null

M2
messageId     = M2
correlationId = order-10001
causationId   = M1

M3
messageId     = M3
correlationId = order-10001
causationId   = M2
```

含义：

- `correlationId` 回答“这些消息属于哪一个完整业务过程”；
- `causationId` 回答“当前消息由哪一条直接上游消息触发”；
- `messageId` 回答“当前这一条消息是谁”。

core 的默认规则：

- 根消息没有 `correlationId` 时，使用生成后的自身 `messageId`；
- 在消费者 Handler 中发送子消息时，自动继承父消息 `correlationId`；
- 子消息没有显式 `causationId` 时，自动使用当前入站消息 `messageId`；
- 业务显式值优先于自动值。

## 7. `MessageDestination` 最终字段

```java
MessageDestination
├── name
├── namespace
├── category
└── providerHint
```

字段含义：

- `name`：逻辑消息名称，例如 `order-paid`；
- `namespace`：业务域或边界，例如 `trade`，不是 `dev/sit/prod`；
- `category`：`EVENT / COMMAND / NOTIFICATION`；
- `providerHint`：可选 Provider 覆盖提示，例如 `kafka`。

逻辑身份示例：

```text
trade:event:order-paid
```

它不直接等于任何物理 Topic。环境隔离和物理命名通过 `DestinationRoute` 配置：

```text
trade:event:order-paid
├── Kafka    -> bank-prod-trade-order-paid-v1
├── RocketMQ -> TRADE_ORDER_PAID
└── Pulsar   -> persistent://bank/trade/order-paid
```

默认采用 `STRICT` 路由模式。没有精确路由时发送被拒绝，避免拼写错误自动创建或误投 Topic。

本地开发确实需要自动物理名称时，可以显式使用 `IMPLICIT_DEFAULT`，但不建议用于生产。

## 8. 快速运行公共闭环验证

Linux/macOS：

```bash
./verify-core.sh
```

Windows PowerShell：

```powershell
./verify-core.ps1
```

脚本不依赖 Maven，也不依赖三个真实 Broker，会编译并运行：

- `message-api`
- `message-spi`
- `message-core`
- `message-testkit`
- `message-demo`

验证内容：

- 普通发送确认；
- 普通订阅消费；
- 父子消息上下文传播；
- `correlationId` 继承；
- `causationId` 自动建立；
- `source` 可选行为；
- 根消息默认关联 ID；
- 严格路由拒绝行为。

## 9. Maven 构建

本机具备 Maven 和外部依赖访问时执行：

```bash
mvn clean verify
```

当前依赖基线：

```text
Java                         17
Kafka Client                 4.3.1
RocketMQ gRPC Java Client    5.2.1
Pulsar Java Client LTS       4.0.12
Jackson                      2.20.0
```

## 10. 最小使用示例

```java
MessageDestination orderPaid = MessageDestination.event(
        "trade",
        "order-paid");

MessageEnvelope<OrderPaidEvent> message = MessageEnvelope.builder(
                "OrderPaid",
                new OrderPaidEvent("10001"))
        .schemaVersion("1")
        .key("10001")
        .context(MessageContext.builder()
                .correlationId("order-10001")
                .tenantId("tenant-a")
                .build())
        .header("traceparent", "00-...")
        .occurredAt(Instant.now())
        .build();

SendResult result = messageTemplate.send(
        orderPaid,
        message,
        SendOptions.defaults());
```

消费者：

```java
MessageSubscription subscription = messageTemplate.subscribe(
        ConsumerDefinition.of(
                orderPaid,
                "trade-order-consumer",
                OrderPaidEvent.class),
        (envelope, consumeContext) -> {
            orderService.handle(envelope.payload());
            return ConsumeDecision.SUCCESS;
        });
```

## 11. 重要生产说明

一期的 `RETRY` 是基础能力，不是完整可靠性方案。

在二期完成以下内容之前，不建议把不可反序列化的毒消息直接用于生产：

- 最大重试次数；
- 指数退避；
- 重试 Topic 或重投策略；
- 死信；
- 人工补偿；
- 幂等集成；
- 消费失败指标和告警。

当前普通消费整体采用至少一次处理取向。业务 Handler 必须接受重复调用的可能性。

## 12. 详细文档

- [总体架构](docs/01-architecture.md)
- [消息模型与上下文](docs/02-message-model.md)
- [逻辑目的地与路由](docs/03-destination-routing.md)
- [生命周期与 Provider 映射](docs/04-lifecycle-and-provider-mapping.md)
- [三期规划](docs/05-phase-roadmap.md)
- [验证结果与已知限制](docs/06-verification-and-limitations.md)

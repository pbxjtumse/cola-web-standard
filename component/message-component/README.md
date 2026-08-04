# Iron Message Component V4

V4 基于用户提供的最新 V3 工程进行代码表达层重构，没有重新拆模块，也没有删除 Kafka、RocketMQ、Pulsar、Testkit、Demo、文档和时序图。

## 本版核心变化

1. `message-component` 中全部 Java `record` 声明已经转换为普通 `final` 不可变类。
2. 原有构造校验、默认值、静态工厂、Builder、方法、字段语义和中文注释全部保留，并补充了显式字段赋值、访问器、`equals`、`hashCode` 和 `toString`。
3. 为避免无关 API 破坏，V4 普通类继续保留原 record 风格的显式访问器，例如 `messageId()`、`destination()`、`status()`；这些方法现在都是源码中可以直接看到的普通 Java 方法。
4. `MessageWireCodec` 作为唯一线级协议实现，负责序列化、系统头编码、入站解码和契约校验。
5. 原本已经失效的 `MessageWireMapper` 改成兼容适配器，内部统一委托 `MessageWireCodec`，不再维护一套过时的重复实现。
6. `MessageWireCodec` 创建 `ConsumeContext` 时补齐一期基础投递次数，修复构造参数不一致导致的编译错误。
7. RocketMQ 和 Pulsar 配置类的 `toString()` 不再输出密钥或认证 Token。
8. 公共模块已经使用 Java 17、`-Xlint:all -Werror` 重新编译，并通过内存发送、消费和父子消息上下文传播验证。

## 消息模型

```text
MessageEnvelope<T>
├── metadata : MessageMetadata
│   ├── messageId
│   ├── messageType
│   ├── schemaVersion
│   ├── messageKey
│   ├── occurredAt
│   └── createdAt
├── context : MessageContext
│   ├── source
│   ├── correlationId
│   ├── causationId
│   └── tenantId
├── headers : MessageHeaders
└── payload : T
```

### messageId 与 messageKey

```text
OrderCreated: messageId=M1, messageKey=order-10001
OrderPaid:    messageId=M2, messageKey=order-10001
```

`messageId` 回答“当前消息是谁”，因此两条消息必须不同。`messageKey` 回答“当前消息主要围绕哪个业务实体”，因此同一订单的多条消息可以相同。

Provider 映射：

- Kafka：Record Key，可影响分区选择；公共 API 不承诺顺序。
- RocketMQ：Message Keys，用于检索；FIFO 顺序需要独立 MessageGroup。
- Pulsar：Message Key，可用于键路由；Shared Subscription 不承诺顺序。

## MessageCategory 当前状态

源码中保留了 `MessageCategory` 作为兼容类型，但 `MessageDestination`、目的地路由和 Provider SPI 当前都不依赖它。Event、Command、Notification 的业务语义暂时通过消息命名和契约规范表达，后续是否删除该兼容枚举应单独评审，不在本次 record 转换中混合修改。

## 模块

```text
message-component
├── message-api
├── message-spi
├── message-core
├── message-codec-jackson
├── message-integrations
│   ├── message-integration-kafka
│   ├── message-integration-rocketmq
│   └── message-integration-pulsar
├── message-testkit
├── message-demo
└── docs
    └── diagrams
        ├── _common
        └── sequence/L0-L3
```

## 最小使用示例

```java
MessageDestination orderPaid = MessageDestination.of("trade", "order-paid");

MessageEnvelope<OrderPaid> message = MessageEnvelope
        .builder("OrderPaid", new OrderPaid("10001"))
        .messageKey("order-10001")
        .context(MessageContext.builder()
                .correlationId("order-flow-10001")
                .build())
        .header("biz-scene", "normal-payment")
        .build();

SendResult result = messagePublisher.send(orderPaid, message);
```

## 三期范围

### 一期：三 Provider 普通消息基础闭环

- 普通同步和异步发送
- 普通消费
- 统一消息模型、逻辑路由、序列化和上下文传播
- `CONFIRMED / REJECTED / FAILED / UNKNOWN`
- `SUCCESS / RETRY`
- Kafka、RocketMQ、Pulsar、InMemory Provider
- 基础内存验证和 L0-L3 时序图

一期代码采取“业务成功后再确认”的至少一次兼容基线。`ConsumeContext` 当前仍保留 `deliveryAttempt` 兼容字段；当 Provider 没有统一次数时，core 使用基础值 `1`，Provider 原生重投信息继续放在 `providerMetadata`。完整投递次数治理放到二期。

### 二期：可靠性治理

- `DeliveryInfo` 与可选重投次数
- 最大重试次数、退避、Retry Topic、DLQ、毒消息
- 幂等组件集成
- Outbox 与本地事务协作
- Trace、指标、审计、积压、告警
- Kafka Rebalance 安全、分区有界执行器、批量位点提交
- 三 Provider 的至少一次语义测试矩阵

### 三期：高级能力

- 顺序、延时、事务、批量、回放、seek、reader
- Kafka read-process-write 事务和 Provider 局部 EOS
- RocketMQ FIFO、延时、事务消息
- Pulsar Key_Shared、延时、Reader、事务
- 可选 at-most-once 原生模式
- Native escape hatch

## 验证

无第三方依赖的公共模块可执行：

```bash
bash verify-core.sh
```

该脚本使用 Java 17、`-Xlint:all -Werror` 编译 `message-api`、`message-spi`、`message-core`、`message-testkit`、`message-demo`，并运行父子消息传播和公共契约验证。

真实 Provider 仍需在有 Maven 和真实 Broker 的环境执行：

```bash
mvn clean verify
```

## 文档入口

- `docs/02-message-model.md`：模型设计
- `docs/04-lifecycle-and-provider-mapping.md`：发送、消费和 Kafka Worker
- `docs/05-phase-roadmap.md`：三期边界
- `docs/08-java-class-and-worker-guide.md`：普通不可变类和 poll 线程说明
- `docs/09-record-to-class-migration.md`：V4 转换规则和类清单
- `docs/diagrams/sequence/README.md`：L0-L3 时序图索引

## Pulsar 真实集群调试

当前工程提供：

```text
com.xjtu.iron.message.demo.PulsarMessageDemo
```

默认连接：

```text
pulsar://pulsar.xjtu-iron.online:6650
```

完整步骤见：

```text
docs/10-pulsar-debug-guide.md
```

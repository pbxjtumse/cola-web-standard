# 第一版关键设计决策

## 1. `MessageDestination` 同时包含 providerName 和 logicalName

第一版不做复杂动态路由，而是让调用方明确指定 Provider：

```java
MessageDestination.of("kafka", "order-paid")
MessageDestination.of("rocketmq", "order-paid")
MessageDestination.of("pulsar", "order-paid")
```

优点：

- 路由结果明确。
- 不需要提前设计动态规则引擎。
- 同一 Topic 名可以用于三种 Provider 的对照测试。

二期或三期再增加逻辑目的地到 Provider/物理 Topic 的配置映射。

## 2. 同步和异步发送共用 Provider 的异步 SPI

Provider 只有：

```java
CompletionStage<ProviderSendResult> send(ProviderSendRequest request)
```

同步发送由 `MessageTemplate` 等待这个异步结果实现。

这样避免每个 Provider 同时实现同步和异步两套近似逻辑，也使同步超时能够统一返回 `UNKNOWN`。

## 3. Provider 不依赖 message-core

Provider 只依赖 `message-api` 和原生 MQ Client。

它只负责：

- 原生消息转换。
- 原生发送。
- 原生消费。
- ACK/重投决策翻译。
- 原生异常粗粒度分类。

它不负责：

- 业务对象序列化。
- messageId 生成。
- 系统消息头增强。
- 业务 Handler 注册模型。
- 统一生命周期编排。

## 4. 序列化在 core 中完成

三个 Provider 接收的都是 `byte[]`。

原因：

- 保证三种中间件发送完全相同的业务字节。
- Provider 不需要理解业务类型。
- JSON、Protobuf、Avro 等实现可以独立演进。
- 反序列化失败能够在统一消费阶段处理。

## 5. Kafka 第一版采用手动 offset 提交

Kafka Consumer 禁用自动提交。

```text
Handler SUCCESS
  -> commitSync(offset + 1)

Handler RETRY
  -> seek(current offset)
  -> 本地退避
```

这是最小可理解实现，但还不是生产级重试方案。它存在以下限制：

- 一个失败消息可能阻塞当前分区后续消息。
- 长时间业务处理可能触发 max.poll.interval 问题。
- 多分区批次中需要更精细的暂停与恢复。
- commit 失败、rebalance 和进程崩溃需要完整治理。

二期应引入 retry topic/DLT 或受控 pause/resume 策略。

## 6. RocketMQ 第一版预声明 Topic

RocketMQ 5.x Producer 在创建时接收 Topic 集合。第一版配置中明确保存 `topics`，发送到未声明 Topic 时返回 `REJECTED`。

这比运行时为每条消息创建 Producer 更合理，也能在启动阶段发现 Topic 配置错误。

## 7. Pulsar 第一版采用 Shared Subscription

Shared Subscription 最接近公共“同一消费组内负载均衡”的基础语义。

第一版不承诺同 Key 顺序。需要同 Key 顺序时，应在三期明确使用 Pulsar Key_Shared 专属能力，而不是让普通公共接口暗中改变订阅类型。

## 8. 业务异常第一版统一 RETRY

第一版没有业务异常分类器，也没有死信闭环，因此 Handler 抛出运行时异常时统一返回 RETRY。

这是保守行为：宁可重复投递，也不错误确认。

但生产环境不能无限重试。二期必须加入：

- 最大尝试次数。
- 可重试异常分类。
- 不可重试异常。
- 死信。
- 人工补偿。
- 幂等。

## 9. 为什么没有 `DistributedMessageClient` 接口和多个门面实现

第一版只有最终类 `MessageTemplate`。

目前不会存在多个生命周期编排实现，因此抽象一个只有单实现的 `DistributedMessageClient` 没有价值。真正需要扩展的是 Provider、Serializer 和后续 Interceptor，而不是替换整个核心门面。

等出现明确的第二种门面语义，例如：

- 纯事件发布门面。
- Request/Reply 门面。
- 流式处理门面。

再建立对应窄接口，而不是现在创建一个模糊总接口。

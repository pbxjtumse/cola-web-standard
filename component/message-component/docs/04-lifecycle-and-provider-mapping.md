# 04 生命周期与 Provider 映射

## 1. 发送主流程

```text
validate
→ enrich metadata/context
→ resolve destination
→ select provider
→ capability check
→ MessageWireCodec.encode
→ provider.send
→ standardize result
```

同步 `send` 只是在异步结果上等待 `SendOptions.confirmTimeout`。等待超时返回 `UNKNOWN`，因为 Broker 之后仍可能完成写入。

## 2. 消费主流程

```text
provider native message
→ ProviderInboundMessage
→ MessageWireCodec.decode
→ open CurrentMessage scope
→ MessageHandler
→ close scope
→ SUCCESS / RETRY
→ native commit / ack / retry
```

一期不暴露统一 `deliveryAttempt`。RocketMQ 和 Pulsar 的原生次数仍保留在 `providerMetadata`，Kafka 普通记录没有可靠统一值。

## 3. Kafka Worker

当前映射：

```text
一个 ConsumerDefinition
= 一个逻辑目的地
= 一个物理 Topic
= 一个 KafkaConsumerWorker
= 一个 KafkaConsumer
= 一个专用 poll 线程
```

`poll()` 返回的是该 Consumer 已分配分区的一批记录，不是“拉完一个 Topic 再拉下一个 Topic”。当前 Worker 只订阅一个 Topic，但一次 poll 可以同时返回 P0、P1、P2 等多个分区。

处理规则：

- 分区内按 offset 顺序处理。
- SUCCESS 提交当前分区 `offset + 1`。
- RETRY 对当前分区 `seek(failedOffset)`，停止当前分区本轮后续记录。
- 本轮其他分区仍继续处理，避免它们已拉取的记录被静默越过。

注册多个 `ConsumerDefinition` 会创建多个 Worker，因此可并发消费多个 Topic。当前单 Worker 内不并发处理多个分区；二期将升级为 poll 线程 + 分区有界执行器。

## 4. 原生映射

| 公共字段 | Kafka | RocketMQ | Pulsar |
|---|---|---|---|
| physical destination | Topic | Topic | Topic |
| messageKey | Record Key | Message Keys | Message Key |
| headers | Headers | Properties | Properties |
| SUCCESS | commit offset+1 | ConsumeResult.SUCCESS | acknowledgeAsync |
| RETRY | seek failed offset | ConsumeResult.FAILURE | negativeAcknowledge |

## 5. 元数据键

Provider 元数据开放但不散落魔法值：

- Kafka：`KafkaMetadataKeys`
- RocketMQ：`RocketMqMetadataKeys`
- Pulsar：`PulsarMetadataKeys`
- Testkit：`InMemoryMetadataKeys`

使用字符串常量而非统一 enum，是因为不同 Provider 的诊断字段集合天然开放，enum 会把 SPI 固定成最低公共集合。

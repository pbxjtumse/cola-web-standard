# 18. Provider 消费确认映射

| ConsumeDecision | Kafka | Pulsar | RocketMQ 4 |
|---|---|---|---|
| ACK | commit offset + 1 | acknowledge | CONSUME_SUCCESS |
| RETRY | 不提交 offset / 可 seek 当前 offset | negativeAcknowledge | RECONSUME_LATER |
| DISCARD | commit offset + 1 | acknowledge | CONSUME_SUCCESS |
| DEAD_LETTER | 二期 DLQ 后 commit | 二期 DLQ policy / 手动 DLQ 后 ack | 二期 broker DLQ / 手动 DLQ |

Kafka v13 同一 partition 内串行处理。RocketMQ v13 建议 consumeMessageBatchMaxSize = 1。

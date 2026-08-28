# 15. 可靠消费一期总览

本期目标：AT_LEAST_ONCE + 消费幂等 + 事务模板接入。

核心链路：Provider 原生 Consumer 收到消息后，转换为 ProviderInboundMessage，交给 MessageConsumeDispatcher 解码、创建 ConsumeContext，再由 ConsumeExecutionTemplate 调用 MessageIdempotencyExecutor 包住业务 Handler，最终返回 ConsumeDecision。Provider 再把 ConsumeDecision 映射为 Kafka commit、Pulsar ack / negativeAck、RocketMQ CONSUME_SUCCESS / RECONSUME_LATER。

本期不承诺 Broker 级严格 exactly-once。EFFECTIVELY_ONCE 表示至少一次投递 + 消费幂等 + 本地事务边界后，尽量保证业务效果只生效一次。

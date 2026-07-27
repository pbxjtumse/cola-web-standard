# 消息组件时序图索引

时序图沿用分布式锁组件的分层方式：从全局视角逐步深入到 Provider 和异常细节，而不是把所有参与者堆在一张图里。

| 层级 | 目标 | 图 |
|---|---|---|
| L0 | 看完整消息生命周期和组件边界 | `L0/00-message-lifecycle-overview.puml` |
| L1 | 看 core 发送、消费和父子上下文主流程 | `L1/01-send-core-flow.puml`、`02-consume-core-flow.puml`、`03-child-message-context.puml` |
| L2 | 看 Kafka、RocketMQ、Pulsar 的原生映射 | `L2/01-kafka-send.puml`、`02-kafka-consume-partitions.puml`、`03-rocketmq-basic.puml`、`04-pulsar-basic.puml` |
| L3 | 看准备失败、确认超时、消费异常和分区失败 | `L3/01-send-preparation-errors.puml`、`02-send-confirm-timeout.puml`、`03-consume-errors.puml`、`04-kafka-partition-failure.puml` |

`_common/style.puml` 统一颜色、字体和序列图样式。图文件只描述当前一期代码实际行为；二期和三期尚未实现的可靠性与高级能力只通过 note 标记，不伪造调用链。

批量入口：`all.puml`。PlantUML 会展开其中引用的 L0-L3 图文件。

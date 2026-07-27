# 05 三期路线最终版

项目固定为三期，不再反复变化。

## 一期：三 Provider 普通消息基础闭环

目标是验证公共模型确实能同时承载 Kafka、RocketMQ、Pulsar。

包含：

- 普通同步和异步发送
- 普通消费
- `MessageMetadata / MessageContext / MessageHeaders / payload`
- 逻辑目的地和严格路由
- JSON 序列化
- 父子消息关联传播
- 标准发送结果和消费决策
- 三 Provider 基础适配
- Testkit、Demo、L0-L3 时序图

交付语义：业务成功后再 commit/ack，属于至少一次兼容基线；但一期不提供投递次数、重试次数、DLQ 或 exactly-once 公共承诺。

## 二期：可靠性治理

包含：

- 可选 `DeliveryInfo`，明确 unknown/first/redelivery
- Provider 重投次数映射
- 最大重试次数和异常分类
- Kafka Retry Topic / DLT
- RocketMQ retry / DLQ
- Pulsar negative ack / DeadLetterPolicy
- 幂等组件集成
- Outbox 与事务组件集成
- 消费超时、限流、背压
- Trace、MDC、指标、审计和告警
- Kafka RebalanceListener、分区 pause/resume、有界并行、批量位点提交
- 至少一次语义的故障测试矩阵

`at-least-once` 的治理和可观测性属于二期，但它的基本风险从一期就必须被接受。

## 三期：高级能力

包含：

- Kafka 分区指定、事务 read-process-write、回放和 seek
- RocketMQ FIFO、延时和事务消息
- Pulsar Key_Shared、延时、Reader 和事务
- 批量消息
- Provider capability API
- Native accessor
- 可选 at-most-once 原生模式
- Provider 局部 exactly-once 能力

注意：Provider 局部 EOS 不等于数据库和外部系统副作用的端到端 exactly-once；后者仍需要幂等、Outbox 或条件更新。

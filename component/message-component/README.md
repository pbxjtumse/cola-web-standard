# message-component

> 技术组件体系中的统一消息组件。当前版本聚焦 Kafka / Pulsar / RocketMQ4 的统一消息模型、普通收发闭环，以及二期发送可靠性增强。

## 当前状态

```text
一期普通收发：已完成
- Kafka 单独收发通过
- Pulsar 单独收发通过
- RocketMQ4 单独收发通过
- Kafka + Pulsar + RocketMQ4 同时收发通过

二期发送可靠性：工程验证阶段
- RetryExecutor 接入设计已完成
- MessageSendExecutor / DefaultReliableMessageSender 已接入
- UNKNOWN 默认不重试语义已确定
- FakeProvider 测试已补充
- 待本地 Maven 编译与三 MQ 可靠发送联调
```

## 模块结构

```text
message-component
├── message-api                # 稳定 API：model / publish / consume / codec / exception / annotation
├── message-spi                # Provider 扩展契约
├── message-core               # 生命周期编排、路由、编解码、可靠发送
├── message-integrations       # Kafka / Pulsar / RocketMQ4 Provider
├── message-starter            # Spring Boot 自动装配
├── message-demo-springboot    # 三 Provider 验证 Demo
└── docs                       # 设计文档和 PlantUML 图
```

## 核心链路

```text
业务代码
  -> MessageTemplate
  -> MessageEnvelopeEnricher
  -> DestinationResolver
  -> MessageWireCodec
  -> MessageSendExecutor
  -> DirectMessageSender / DefaultReliableMessageSender
  -> MessageProvider
  -> Kafka / Pulsar / RocketMQ4
```

二期可靠发送启用后，发送动作会经过 `DefaultReliableMessageSender`，由它复用 `retry-component` 的 `RetryExecutor` 完成有限次数、短时间、可解释的发送重试。

## 文档入口

- `docs/README.md`：文档目录和阅读顺序
- `docs/01-architecture.md`：总体架构
- `docs/11-phase2-send-reliability.md`：二期可靠发送设计
- `docs/diagrams/README.md`：L0-L4 图示规范

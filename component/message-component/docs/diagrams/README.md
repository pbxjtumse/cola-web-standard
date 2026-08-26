# message-component PlantUML 图分层说明

本目录采用和 distributed-lock-component 一致的图示规范：

```text
L0：组件总览 / 生命周期总览
L1：核心主流程
L2：核心内部协作
L3：异常、边界、可靠性流程
L4：具体 Provider / 中间件 / 协议映射
```

## 1. 重要约定

1. **颜色保持一致**：API、Core、Core Send、SPI、Provider、Retry、Foundation、Broker 使用固定颜色。
2. **时序图到类级别即可**：参与者写类名和所在包，箭头文字写协作语义，不再写 `类.方法名`。
3. **class 图补充结构理解**：复杂流程先看 class，再看 sequence，再看 state。
4. **L4 不污染 L1 / L2**：Kafka、Pulsar、RocketMQ4 的专属状态只放 L4。

## 2. 模块颜色约定

| 模块 | 颜色 | 说明 |
|---|---|---|
| Business / Demo | `#E8F5E9` | 业务调用、Demo Controller / Listener |
| API | `#E3F2FD` | `message-api` |
| Core | `#EDE7F6` | `message-core` 基础编排 |
| Core Send | `#FFF3E0` | `core.send` / `core.send.reliability` |
| SPI | `#ECEFF1` | `message-spi` |
| Provider | `#FFEBEE` | Kafka / Pulsar / RocketMQ4 integration |
| Retry Component | `#FCE4EC` | `retry-component` |
| Foundation | `#E0F2F1` | foundation id / serialization |
| Broker | `#FFFDE7` | Kafka / Pulsar / RocketMQ4 |
| Starter / Config | `#F3E8FF` | Spring Boot AutoConfiguration |

## 3. 当前图结构

```text
docs/diagrams
├── class
│   ├── L0
│   ├── L1
│   ├── L2
│   ├── L3
│   └── L4
├── sequence
│   ├── L0
│   ├── L1
│   ├── L2
│   ├── L3
│   └── L4
└── state
    ├── L1
    ├── L2
    ├── L3
    └── L4
```

## 4. 推荐阅读顺序

```text
class/L0/01-module-overview.puml
sequence/L1/01-send-core-flow.puml
class/L2/01-reliable-send-collaboration.puml
sequence/L2/05-retry-executor-flow.puml
sequence/L3/05-unknown-stop-no-retry.puml
sequence/L3/06-retry-exhausted.puml
sequence/L4/03-rocketmq4-send-result-mapping.puml
state/L2/01-reliable-send-retry-state.puml
```
## 6. Class 图补全说明

V10 对 class 图做了补全，不再只是几张主干入口图。当前 class 图覆盖：

```text
class/L0/01-module-overview.puml                模块总览
class/L0/02-demo-entry-view.puml                Demo 入口与三 Provider 验证
class/L1/01-api-core-key-classes.puml           API / Core 主入口
class/L1/02-api-full-package-classes.puml       message-api 全量分包
class/L2/01-reliable-send-collaboration.puml    可靠发送核心协作
class/L2/02-core-package-classes.puml           message-core 全量分包
class/L2/03-routing-class-view.puml             路由模型
class/L2/04-codec-class-view.puml               编解码边界
class/L2/05-id-enrich-class-view.puml           messageId 与 enrich
class/L2/06-spi-contract-class-view.puml        SPI 契约
class/L3/01-send-result-boundary-classes.puml   发送结果边界
class/L3/02-starter-autoconfig-class-view.puml  自动装配边界
class/L4/01-provider-implementation-view.puml   Provider 总览
class/L4/02-kafka-provider-class-view.puml      Kafka Provider
class/L4/03-pulsar-provider-class-view.puml     Pulsar Provider
class/L4/04-rocketmq4-provider-class-view.puml  RocketMQ4 Provider
```

这些图用于对齐 distributed-lock-component 的文档质量：既有主链路时序，也有模块与类结构。


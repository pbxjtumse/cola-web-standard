# message-component 文档目录

本目录按照和 distributed-lock-component 一致的方式进行收口：

1. 保留当前版本仍然有效的设计文档。
2. 删除已经失效的历史迁移文档、临时 debug 文档、一次性验证记录。
3. 图目录统一采用 L0-L4 分层、模块着色、类级参与者展示。

---

## 1. 推荐阅读顺序

| 顺序 | 文档 | 说明 |
|---:|---|---|
| 1 | `01-architecture.md` | 组件定位、模块职责、API / Core / SPI / Provider 关系 |
| 2 | `02-message-model.md` | MessageEnvelope、MessageMetadata、MessageHeaders、MessageContext |
| 3 | `03-destination-routing.md` | 逻辑目的地、providerHint、physical destination 的解析方式 |
| 4 | `04-lifecycle-and-provider-mapping.md` | 发送 / 消费生命周期、统一状态映射原则 |
| 5 | `05-phase-roadmap.md` | 路线图：一期收口 -> 二期可靠发送 -> 后续消费可靠性 |
| 6 | `06-verification-and-limitations.md` | 已验证内容、当前限制、尚未进入生产级的部分 |
| 7 | `07-code-index.md` | 代码分包索引，快速定位关键类 |
| 8 | `08-java-class-and-worker-guide.md` | 不再使用 record 的原因、Worker / Listener 的阅读入口 |
| 9 | `09-spring-boot-config-and-wire-codec.md` | Starter 配置绑定、wire codec 边界 |
| 10 | `10-tri-provider-parallel-demo.md` | Kafka / Pulsar / RocketMQ 三 Provider 并行验证 |
| 11 | `11-phase2-send-reliability.md` | 二期可靠发送设计、状态语义、类职责 |
| 12 | `12-current-progress-and-next-steps.md` | 当前进度、前置验收点、下一步动作 |
| 13 | `13-diagram-method-level-convention.md` | 时序图方法级表达约定，与分布式锁组件风格对齐 |
| 14 | `diagrams/README.md` | PlantUML 图层规范、颜色规范、推荐图阅读顺序 |

---

## 2. 本轮文档优化点

### 2.1 保留

保留上面 13 份文档，原因是它们都仍然和当前代码对应。

### 2.2 删除 / 不再保留的类型

以下类型的文档不建议继续留在最终 docs 目录：

| 类型 | 说明 |
|---|---|
| 一次性 debug 文档 | 例如临时的 Pulsar / RocketMQ 排障过程 |
| 历史迁移文档 | 例如 record -> class 的迁移细节 |
| 阶段补丁文档 | 例如某一轮修补某个 bug 的临时说明 |
| 与最终结构重复的 readme | 避免多个 README 表达不一致 |

### 2.3 图目录规范化

本轮图目录重点做了三件事：

1. **颜色与 distributed-lock-component 对齐**：API、Core、SPI、Provider、Retry、Foundation 等模块保持固定配色。
2. **增加 class 图**：不再只有时序图和状态图，补充关键 class 视图，阅读结构更清晰。
3. **时序图参与者保留类级别，箭头保留关键真实方法名**：箭头文字表达“协作语义”，不再写 `类.方法名`，避免图太碎。

---

## 3. 建议的阅读方式

如果你现在是要继续推进二期可靠发送，建议这样看：

```text
先看 11-phase2-send-reliability.md
再看 diagrams/class/L2/01-reliable-send-collaboration.puml
再看 diagrams/sequence/L1/01-send-core-flow.puml
再看 diagrams/sequence/L2/01-reliable-send-collaboration.puml
最后看 diagrams/state/L2/01-reliable-send-retry-state.puml
```

如果你是要总览整个 message-component，则建议：

```text
先看 01-architecture.md
再看 diagrams/class/L0/01-module-overview.puml
再看 diagrams/sequence/L0/01-message-lifecycle-overview.puml
再看 07-code-index.md
```
## 4. V10 class 图补全

V10 补全了 class 图体系。之前的 class 图主要是主干视图，本轮新增 API 全量分包、Core 全量分包、Routing、Codec、Id Enrich、SPI、Starter 自动装配、Kafka / Pulsar / RocketMQ4 Provider 专属类图。

这样 docs 同时具备：

```text
时序图：说明实际方法调用链路。
状态图：说明可靠发送状态流转。
类图：说明模块、分包、接口和实现之间的静态关系。
```


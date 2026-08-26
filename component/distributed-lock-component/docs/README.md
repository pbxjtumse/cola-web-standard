# Distributed Lock Component 文档与图表

本文档目录对应当前最终收口版代码结构：

```text
distributed-lock-api
distributed-lock-spi
distributed-lock-core
distributed-lock-provider
  ├── distributed-lock-provider-redis
  ├── distributed-lock-provider-redisson
  └── distributed-lock-fencing-provider-jdbc
distributed-lock-starter
distributed-lock-demo
```

## 核心文档

- `configuration.md`：锁语义、Provider、watchdog 与 JDBC fencing 配置。
- `metrics.md`：Micrometer 指标与告警建议。
- `FAQ.md`：常见问题与边界说明。
- `sequence/sequence-final-review.md`：时序图最终边界检查说明。

## Component Diagrams

```text
component/
├── L0-overview/module-structure.puml
├── L1-architecture/core-components.puml
├── L2-extension/
│   ├── redis-lock-extension-components.puml
│   ├── fencing-token-extension-components.puml
│   └── future-provider-extension-components.puml
└── L3-internal/
    ├── default-lock-handle-structure.puml
    └── lock-options-structure.puml
```

## Sequence Diagrams

```text
sequence/
├── L0-overview
├── L1-main-flow
├── L2-scenario-flow
└── L3-internal-flow
```

统一调用方向：

```text
API -> Core -> SPI -> Provider -> Resource
```

## State Diagrams

```text
state/
├── L0-vocabulary
├── L1-lifecycle
└── L3-mapping
```

## 维护原则

- API 只暴露业务入口和模型。
- Core 只做编排，不绑定具体 Redis/Redisson/JDBC 实现。
- SPI 只放 Provider 契约和协议对象。
- Provider 只实现 SPI，不反向依赖 Core。
- Starter 当前是 all-in-one starter，负责装配 Core 与已内置 Provider。

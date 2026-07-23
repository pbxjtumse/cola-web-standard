# Distributed Lock Component 文档与图表

## 核心文档

- `configuration.md`：连接、锁语义和 JDBC fencing 配置。
- `metrics.md`：Micrometer 指标与告警建议。
- `phase2-fencing-token.md`：二期完整设计、失败语义和业务防旧写模板。
- `v24-balanced-client-refactor.md`：Client、acquire、execute 的平衡拆分与合并说明。

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

## Phase 2 Sequence Diagrams

- `sequence/L1-main-flow/external-jdbc-fencing-sequence.puml`
- `sequence/L2-scenario-flow/fencing-provider-error-sequence.puml`
- `sequence/L2-scenario-flow/fencing-rejected-sequence.puml`
- `state/L3-mapping/fencing-status-mapping.puml`

图表保持自然曲线布局，不在主架构图中堆积所有 DTO、Lua 文件和状态细节。

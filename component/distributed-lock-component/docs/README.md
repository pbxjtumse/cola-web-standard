# Distributed Lock Component Diagrams

这一版采用“自然曲线布局”：
- 不使用 `skinparam linetype ortho`
- 核心架构图只保留主依赖线
- Redis、Future Provider、内部字段结构拆到独立图
- 避免在主架构图里放 Lua 文件、Redis 细节和大量 note

## Component Diagrams

```text
component/
├── L0-overview/
│   └── module-structure.puml
├── L1-architecture/
│   └── core-components.puml
├── L2-extension/
│   ├── redis-lock-extension-components.puml
│   └── future-provider-extension-components.puml
└── L3-internal/
    ├── default-lock-handle-structure.puml
    └── lock-options-structure.puml
```

## 配置与可观测性

- 配置说明：见 `docs/configuration.md`
- 指标说明：见 `docs/metrics.md`

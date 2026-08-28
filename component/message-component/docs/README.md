# Message Component UML Diagrams v13

本目录按照图类型优先组织：`class`、`sequence`、`state`。
每个类型下再区分 `send` 和 `consume`，内部按需要继续使用 `L0` 到 `L4` 分层。

## 目录结构

```text
diagrams/
├── _common/
│   ├── style.puml          # 类图 / 时序图 / 组件图使用，包含模块颜色
│   └── state-style.puml    # 预留，状态图默认不使用颜色
├── class/
│   ├── send/L0-L4/
│   └── consume/L0-L4/
├── sequence/
│   ├── send/L0-L4/
│   └── consume/L0-L4/
└── state/
    ├── send/L1-L4/
    └── consume/L1-L4/
```

## 规则

- 类图、时序图、组件图使用模块颜色，保持和分布式锁组件一致。
- 状态图不使用模块颜色，保持默认黑白状态流转风格。
- 类图以 `send/L0/00-send-full-class.puml` 和 `consume/L0/00-consume-full-class.puml` 作为发送/消费两个全局入口。
- 时序图按 L0-L4 展示发送、消费主流程与 provider 细节。
- 状态图只表达状态流转，不表达模块边界。

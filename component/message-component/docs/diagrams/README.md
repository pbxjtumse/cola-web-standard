# Message Component v13 UML Diagrams

本目录是 message-component v13 的 UML 最终整理版。

## 目录规则

```text
diagrams/
├── _common/
├── class/
├── sequence/
└── state/
```

## 图类型规则

- 类图：使用模块颜色，作为结构全貌入口。
- 时序图：使用模块颜色，按 send / consume 和 L0-L4 表达流程深度。
- 状态图：不使用颜色，按 send / consume 下的状态机主题组织。

## 状态图标记规则

状态图不再按 L1-L4 拆分，而是每张图内部尽可能完整表达一个状态机。
状态图中使用 stereotype 区分“代码枚举状态”和“演进推导状态”：

```text
<<stored_enum>>      数据库存储状态 / 记录状态枚举
<<decision_enum>>    ConsumeDecisionType 等代码枚举
<<status_enum>>      SendStatus 等代码枚举
<<stage_enum>>       SendStage 等代码枚举
<<failure_enum>>     FailureType 等代码枚举
<<result_enum>>      acquire / mark 返回结果枚举
<<retry_enum>>       RetryStatus 代码枚举
<<logical>>          根据时间、次数、配置推导出的逻辑状态，枚举中不存在
<<runtime>>          过程节点，不是枚举
<<provider_action>>  Provider 确认动作，不是业务状态枚举
```

重点：`ABSENT`、`PROCESSING_TIMEOUT`、`RECORD_EXPIRED`、`MAX_ATTEMPTS_EXCEEDED` 这类是逻辑判断状态，不建议放进数据库 `status` 字段。

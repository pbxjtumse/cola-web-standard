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

## 状态图可视化标记规则

状态图不再只依赖 PlantUML stereotype，因为隐式节点渲染后不容易看出类型。
本版要求每个关键状态框直接显示分类和来源，例如：

```text
VALIDATE
[STAGE ENUM]
SendStage.VALIDATE

VALIDATION_ERROR
[FAILURE ENUM]
SendFailureType.VALIDATION_ERROR

PROCESSING_TIMEOUT
[LOGICAL]
PROCESSING + processing_expire_time < now
```

常用标记：

```text
[STAGE ENUM]              SendStage 等阶段枚举
[STATUS ENUM]             SendStatus 等结果枚举
[FAILURE ENUM]            SendFailureType / ConsumeFailureType 等失败类型枚举
[DECISION ENUM]           ConsumeDecision 枚举
[RETRY ENUM]              RetryStatus 枚举
[ACQUIRE ENUM]            IdempotentAcquireStatus 枚举
[IDEMPOTENT DB ENUM]      IdempotencyStatus 当前真实持久状态枚举
[WRITE ENUM]              IdempotencyWriteStatus 条件写结果枚举
[POLICY ENUM]             MessageIdempotencyFailurePolicy / ConsumerReliabilityMode 等策略枚举
[PROVIDER ACTION ENUM]    Kafka/Pulsar/RocketMQ consume mapper action enum
[LOGICAL]                 根据查询、时间、次数、配置推导，不是枚举
[RUNTIME]                 执行过程节点，不是枚举
[MESSAGE SEMANTIC]        message-component 消费语义，不一定是 idempotent-api 持久枚举
```

## 当前代码枚举核对结论

- `SendFailureType` 当前没有 `INVALID_OPTIONS`，参数或选项非法应使用 `VALIDATION_ERROR`。
- `IdempotencyStatus` 当前只有 `PROCESSING / SUCCESS / FAILED`。
- `DISCARDED` 是当前 message-component 消费语义：`ConsumeDecision.DISCARD`、`markDiscarded(...)`、`DUPLICATE_DISCARDED`。
  但当前 idempotent-component 的持久状态枚举里没有 `DISCARDED`，因此状态图标记为 `[MESSAGE SEMANTIC]`，不标记为 `[IDEMPOTENT DB ENUM]`。

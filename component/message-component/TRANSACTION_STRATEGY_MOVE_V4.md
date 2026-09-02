# Transaction Move V4

本版本完成消费事务边界调整。

## Before

```
MessageIdempotencyDecisionHandler
        |
        +-- TransactionExecutor
```

## After

```
MessageConsumeExecutor
        |
        +-- IdempotencyStrategy
        |
        +-- TransactionStrategy
        |
        +-- HandlerInvoker

MessageIdempotencyExecutor
        |
        +-- ContextFactory
        +-- StateManager
        +-- DecisionHandler
```

事务不再属于幂等能力。

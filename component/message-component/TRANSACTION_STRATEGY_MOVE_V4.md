# Transaction Strategy Move V4 Final

## Final Consumer Flow

```text
ProviderInboundMessage
        |
MessageConsumerAdapter
        |
MessageConsumeExecutor
        |
        +-- IdempotencyStrategy
        |       |
        |       +-- MessageIdempotencyExecutor
        |
        +-- TransactionStrategy
        |       |
        |       +-- MessageConsumeTransactionExecutor
        |
        +-- MessageHandlerInvoker
                |
                +-- MessageHandler
```

## Key Decision

`MessageIdempotencyExecutor` no longer depends on `MessageConsumeTransactionExecutor`.

Transaction is now explicitly assembled by `MessageConsumeExecutor` through `TransactionStrategy`.

## Rollback Rule

When transaction is enabled:

- `ACK` and `DISCARD` allow commit.
- `RETRY` and `DEAD_LETTER` are converted to an internal runtime exception to trigger rollback, then converted back to the original `ConsumeDecision`.

This avoids the issue where `MessageHandlerInvoker` catches a business exception and converts it to `RETRY`, causing a real transaction executor to mistakenly commit.

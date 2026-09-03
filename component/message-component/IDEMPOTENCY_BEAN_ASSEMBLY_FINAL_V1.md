# Idempotency Bean Assembly Final

## No idempotent storage

```text
No MessageIdempotentOperations
        |
NoopMessageIdempotencyExecutor
        |
DefaultIdempotencyStrategy
```

If `xjtu.iron.message.consume.idempotency.enabled=true`, startup fails fast when `MessageIdempotentOperations` is missing.

## With idempotent storage

```text
MessageIdempotentOperations
        |
MessageIdempotencyStateManager
        |
MessageIdempotencyDecisionHandler
        |
DefaultMessageIdempotencyExecutor
        |
DefaultIdempotencyStrategy
```

## V4 boundary

`MessageIdempotencyExecutor` only handles idempotency state.

Transaction is handled by:

```text
TransactionStrategy
        |
MessageConsumeTransactionExecutor
```

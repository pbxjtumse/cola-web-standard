# Idempotency Bean Assembly Final V1

修复消费幂等 Spring 装配边界。

规则：

## 无 idempotent-component

```
No MessageIdempotentOperations
        |
        v
NoopMessageIdempotencyExecutor
```

## 有 idempotent-component

```
MessageIdempotentOperations
        |
        v
MessageIdempotencyStateManager
        |
        v
MessageIdempotencyDecisionHandler
        |
        v
DefaultMessageIdempotencyExecutor
```

不允许 Bean 方法返回 null。

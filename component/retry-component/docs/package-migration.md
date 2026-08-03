# 1.3 版本到 Phase 1 RC1 包迁移

本次分包属于源码不兼容调整，但不改变重试执行语义。当前仍处于一期 API 冻结前阶段，因此现在完成一次迁移比后续长期保留错误包结构更合理。

## API 迁移

| 原包 | 新包 |
|---|---|
| `com.xjtu.iron.retry.api.RetryExecutor` | `com.xjtu.iron.retry.api.execution.RetryExecutor` |
| `com.xjtu.iron.retry.api.RetryExecution` | `com.xjtu.iron.retry.api.execution.RetryExecution` |
| `com.xjtu.iron.retry.api.RetryContext` | `com.xjtu.iron.retry.api.execution.RetryContext` |
| `com.xjtu.iron.retry.api.RetryAttempt` | `com.xjtu.iron.retry.api.execution.RetryAttempt` |
| `com.xjtu.iron.retry.api.RetryResult` | `com.xjtu.iron.retry.api.execution.RetryResult` |
| `com.xjtu.iron.retry.api.RetryPolicy` | `com.xjtu.iron.retry.api.policy.RetryPolicy` |
| `com.xjtu.iron.retry.api.RetryDecision` | `com.xjtu.iron.retry.api.policy.RetryDecision` |
| `com.xjtu.iron.retry.api.RetryFailureCategory` | `com.xjtu.iron.retry.api.policy.RetryFailureCategory` |
| `com.xjtu.iron.retry.api.BackoffStrategy` | `com.xjtu.iron.retry.api.backoff.BackoffStrategy` |
| `com.xjtu.iron.retry.api.support.BackoffStrategies` | `com.xjtu.iron.retry.api.backoff.BackoffStrategies` |
| `com.xjtu.iron.retry.api.RetryEvent` | `com.xjtu.iron.retry.api.event.RetryEvent` |
| `com.xjtu.iron.retry.api.RetryListener` | `com.xjtu.iron.retry.api.event.RetryListener` |

其余类型按照相同职责包迁移。

## Core 迁移

| 原包 | 新包 |
|---|---|
| `com.xjtu.iron.retry.core.DefaultRetryExecutor` | `com.xjtu.iron.retry.core.executor.DefaultRetryExecutor` |
| `com.xjtu.iron.retry.core.DefaultRetryPolicyRegistry` | `com.xjtu.iron.retry.core.policy.DefaultRetryPolicyRegistry` |
| `com.xjtu.iron.retry.core.UuidRetryIdGenerator` | `com.xjtu.iron.retry.core.id.UuidRetryIdGenerator` |

业务模块不应直接导入以上 Core 实现，优先注入 API 接口。

## IDEA 操作

替换目录后执行：

```text
Maven → Reload All Maven Projects
Build → Rebuild Project
```

如果旧包仍被缓存：

```text
File → Invalidate Caches → Invalidate and Restart
```

不要同时保留新旧两个源码目录，否则会产生重复类型或错误自动导入。

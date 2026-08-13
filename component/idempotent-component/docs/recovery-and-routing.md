# Recovery、Reliable Task 与 routeKey

## 1. 不再使用 Lazy Recovery

普通 `execute()`：

```text
PROCESSING 未超时 -> PROCESSING
PROCESSING 已超时 -> PROCESSING_EXPIRED
```

不会修改状态，也不会自动重做业务。

只有外部 Reliable Task 调用：

```java
idempotencyExecutor.recover(...)
```

才允许接管。

## 2. recoveryMode

```text
NONE
```

不允许 Reliable Task 恢复。适合普通 SHORT_TERM 去重。

```text
EXTERNAL_TASK
```

允许外部任务组件查询候选项并调用 recover。默认推荐给 DURABLE。

幂等组件只提供：

- `IdempotencyRecoveryQueryService.findCandidates(...)`；
- `IdempotencyExecutor.recover(...)`；
- Repository 原子接管。

它不提供定时器、分页调度、MQ 投递、任务抢占器。

## 3. routeKey

routeKey 和 idempotencyKey 职责不同：

```text
routeKey       -> 数据在哪个分片
idempotencyKey -> 这是不是同一个逻辑请求
requestHash    -> 同一个 key 的请求内容是否一致
```

V1.1 已把 routeKey 贯穿：

```text
IdempotencyRequest
IdempotencyRecoveryRequest
IdempotencyAcquireRequest
IdempotencyRecord
JDBC 表
Redis Hash
RecoveryCandidate
IdempotencyContext
```

当前版本没有实现具体分库分表算法。未来 Routing 组件/路由 DataSource 应在 Repository 访问数据库之前根据 routeKey 选中正确物理库。

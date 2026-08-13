# Idempotency Window 与 Record Retention TTL

两者都保留，但职责必须彻底区分。

## Idempotency Window

业务语义：多长时间内，相同 idempotencyKey 仍属于同一个逻辑请求。

SHORT_TERM 支持：

- `FIXED_FROM_FIRST_ACQUIRE`：第一次抢占时固定 `windowExpireAt`；
- `SLIDING_ON_ACCESS`：窗口仍有效时，每次有效访问/状态完成都推进到 `now + idempotencyWindow`。

所以“已有操作后再往后顺延 N 分钟”应该由 `SLIDING_ON_ACCESS` 表达，而不是滥用 Redis TTL。

## Record Retention TTL

存储语义：语义窗口结束后，物理记录额外保留多久用于排障/观测。

```text
windowExpireAt = 10:10
recordRetentionTtl = 5m
retentionExpireAt = 10:15
```

10:10 之后同 key 已经可以开启新 generation；10:10~10:15 旧物理记录存在并不继续阻止新执行。

Redis 的真实 `PEXPIREAT` 使用 `retentionExpireAt`，而 Repository 在 `windowExpireAt` 到期后会把旧记录原子重置为新的 PROCESSING generation。

因此：

- `idempotencyWindow` 决定业务去重；
- `recordRetentionTtl` 决定物理数据保留；
- 两个概念不要再次合并。

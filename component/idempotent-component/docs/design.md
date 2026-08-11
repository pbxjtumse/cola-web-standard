# 分布式幂等组件 v1 设计

## 1. 正确性边界

`Idempotency State` 记录的是“某个逻辑请求历史上执行到了什么阶段”，不是分布式锁状态。

V1 的正确性根基：

1. `(namespace, idempotencyKey)` 唯一；
2. `NOT_EXIST / FAILED / EXPIRED_PROCESSING -> PROCESSING` 必须原子；
3. `PROCESSING -> SUCCESS / FAILED` 必须校验 `ownerToken + version`；
4. 旧 owner 不能覆盖新 owner；
5. 真正业务资源如需防止旧执行者恢复后继续写，使用 `IdempotencyContext.fencingVersion()` 做业务条件更新。

## 2. 三个持久状态

### PROCESSING

表示某个 owner 获得执行权：

- ownerToken
- version
- processingExpireAt

`processingTimeout` 是执行权租约，不是删除幂等记录的 TTL。

### SUCCESS

表示业务已经完成。重复请求不再执行 callback，返回 `REPLAYED`。`storeResult=true` 时可保存第一次业务结果快照。

### FAILED

表示上一次明确失败。是否允许再次抢占由：

`options.retryFailed && record.failureRetryable`

共同决定。默认业务异常不可重试，保持保守。

## 3. PROCESSING 超时

V1 不做后台扫描器，而采用 lazy recovery：下一次相同 Key 请求进入时，Repository 原子判断 `processingExpireAt <= now`。

语义状态流转：

`PROCESSING -> FAILED(PROCESSING_TIMEOUT, retryable=true)`

如果 `retryOnProcessingTimeout=true`，同一事务/Lua 中继续：

`FAILED -> PROCESSING(newOwner, version+1)`

因此不会因为服务宕机留下永远不可恢复的 PROCESSING。

## 4. SHORT_TERM

V1 Redis Repository 只支持 SHORT_TERM：

- Redis Hash 保存状态
- Lua 完成原子判断/转换
- `recordTtl` 控制有限去重窗口
- SUCCESS/FAILED 后重新刷新 TTL
- TTL 到期后 Key 删除，同一个幂等 Key 可以再次作为“新请求”执行

适合：按钮连点、API 5 分钟去重、短期 requestId、防刷、临时任务防重。

它不应该作为支付/订单等长期业务事实的唯一存储。

## 5. DURABLE

V1 JDBC Repository 只支持 DURABLE：

- Unique(namespace, idempotency_key) 保证首次创建唯一
- 已存在记录使用 `SELECT ... FOR UPDATE` 串行化状态判断
- 最终写使用 ownerToken + version 条件更新
- SUCCESS/FAILED 不依赖 TTL 自动删除

适合：订单、支付、退款、结算、消息消费产生不可重复业务结果。

## 6. DistributedLockClient

锁不是正确性根基。V1 只使用：

`DistributedLockClient -> Repository.tryAcquire -> 立即释放 -> callback`

而不是：

`加锁 -> 整个业务 callback -> 解锁`

原因：

- 不长时间持有 Redisson/Redis 租约；
- 锁失效不会破坏幂等状态机正确性；
- 锁只是减少同 Key 下数据库唯一键/行锁/Lua 竞争。

`fallbackToStateOnFailure=true` 时，即使分布式锁没拿到/不可用，也退化到 Repository 原子抢占。

生产可以让 DistributedLockClient 的默认 Provider 使用 Redisson，但 idempotent-core 不依赖 Redisson。

## 7. version 与 fencing

例：A version=1 卡住，超时后 B 接管 version=2，B 已成功。A 恢复后：

- `markSuccess(owner=A, version=1)` 更新 0 行 -> OWNERSHIP_LOST；
- 但如果 A 已经对业务表产生副作用，仅保护幂等记录还不够。

强一致业务可把 `context.fencingVersion()` 带入业务 SQL：

```sql
UPDATE business_resource
SET payload = ?, last_idempotency_version = ?
WHERE business_key = ?
  AND last_idempotency_version < ?;
```

## 8. requestHash

同一个 Key + 不同请求体必须返回 `KEY_CONFLICT`。组件不猜怎么序列化业务请求，因此 requestHash 由调用方生成。DURABLE 强烈建议必填。

## 9. result snapshot

默认 `storeResult=false`，避免把大对象默认写数据库/Redis。

开启后 Starter 使用 Jackson：

- 首次 SUCCESS 保存 payload；
- 后续 SUCCESS 请求返回 REPLAYED，并用 `Class<T>` 恢复结果。

V1 暂不解决复杂泛型 TypeReference，后续可扩展。

## 10. V1 暂不做

- `@Idempotent` 注解/AOP
- PROCESSING heartbeat
- 后台超时扫描/清理中心
- 管理后台、人工重放
- 自动事务模板把业务事务与幂等 SUCCESS 绑定
- Redis DURABLE 模式
- 分布式任务恢复

这些放后续版本，避免第一版膨胀成工作流平台。


## 短期防重复与限流的边界

`SHORT_TERM` 解决的是“同一个幂等 key 在 TTL 窗口内只产生一次逻辑结果”，例如按钮连点、客户端重试、requestId 重复提交。它**不等同于限流/防刷治理**：攻击者不断更换 key 时，幂等组件不会限制 QPS；真正的接口防刷仍应由 RateLimiter、验证码、风控或网关治理组件负责。

## 11. DURABLE 与业务事务的一致性边界

V1 的 `JdbcIdempotencyRepository` 会把 PROCESSING 抢占作为独立短事务提交，使其他并发请求能及时看到 PROCESSING。

但业务 callback 的数据库事务与 executor 在 callback 返回后执行的 `markSuccess` **并不会自动处在同一个事务中**。因此存在典型窗口：

1. 业务数据提交成功；
2. 应用在 `markSuccess` 前崩溃；
3. 幂等记录仍为 PROCESSING；
4. PROCESSING 超时后新的 owner 可能接管并再次执行。

V1 对这个问题提供两道基础保护：

- 幂等记录自身通过 ownerToken + version 拒绝旧 owner 覆盖；
- `IdempotencyContext.fencingVersion()` 可传给业务资源，通过业务 CAS/唯一键拒绝旧执行者或重复副作用。

V1.1 建议增加 transaction integration，明确支持“业务事务 + 幂等最终状态”的组合模板。第一版不把这个复杂问题偷偷隐藏在 Repository 内部。

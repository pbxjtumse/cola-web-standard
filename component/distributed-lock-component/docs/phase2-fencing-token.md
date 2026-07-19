# 二期：Fencing Token 设计与实现

## 1. 要解决的问题

ownerToken 能保证旧 owner 不能释放、续期新 owner 的锁，但不能阻止旧 owner 在租约过期后继续写业务资源。

```text
A 获得锁(owner=A, token=10) -> A 长时间停顿
A 的锁过期
B 获得锁(owner=B, token=11) -> B 写入成功
A 恢复 -> 若业务写入不校验 token，A 可能覆盖 B
```

fencing token 的目标是让资源端只接受更大的 token：

```text
accepted = incomingToken > lastAcceptedToken
```

## 2. ownerToken 与 fencingToken

| 属性 | ownerToken | fencingToken |
|---|---|---|
| 类型 | 随机唯一字符串 | 单调递增 long |
| 保护对象 | 锁 key/path | 业务资源 |
| 使用位置 | acquire/release/renew/check | SQL 条件更新、状态机版本检查 |
| 能否替代对方 | 不能 | 不能 |

## 3. 二期组件结构

```text
DefaultDistributedLockClient
  -> FencingTokenCoordinator
       -> NATIVE: LockProvider 原生生成
       -> EXTERNAL: FencingTokenProviderRegistry
                         -> JdbcSequenceFencingTokenProvider
```

### 核心类型

- `FencingTokenProvider`：独立发号 SPI。
- `FencingTokenProviderRegistry`：名称注册和默认选择。
- `FencingTokenCoordinator`：根据 LockOptions 与 ProviderCapabilities 选择计划。
- `FencingTokenPlan`：`NONE / NATIVE / EXTERNAL`。
- `FencingTokenResponse`：`ISSUED / NOT_SUPPORTED / PROVIDER_ERROR`。
- `FencingTokenGuard`：把业务条件更新失败映射为 `FENCING_REJECTED`。

## 4. Redis 原生流程

```text
Client -> RedisLockProvider.acquire(nativeFencing=true)
RedisLockProvider -> acquire.lua(lockKey, fenceKey)
acquire.lua -> INCR fenceKey
acquire.lua -> PSETEX lockKey ownerToken lease
acquire.lua --> Provider: {1, fencingToken}
Provider --> Client: LockLease(ownerToken, fencingToken, source=redis)
```

只有成功拿到锁时才递增 fencing key；竞争失败不会消耗 token。

## 5. JDBC 独立流程

```text
Client -> RedisLockProvider: acquire(nativeFencing=false)
RedisLockProvider --> Client: LockLease(ownerToken, no token)
Client -> JdbcSequenceFencingTokenProvider: nextToken(namespace, lockName)
Jdbc Provider -> DB: UPDATE current_token = current_token + 1
alt row does not exist
  Jdbc Provider -> DB: INSERT current_token = 1
  alt duplicate insert
    Jdbc Provider -> DB: rollback and retry UPDATE
  end
end
Jdbc Provider -> DB: SELECT current_token
Jdbc Provider -> DB: COMMIT
Jdbc Provider --> Client: issued(token)
Client -> Client: lease.withFencingToken(token, "jdbc-sequence")
```

JDBC Provider 使用独立事务。业务事务回滚时 token 不回退，这是正确行为；fencing 只要求单调，不要求连续。

## 6. 发号失败语义

独立发号发生在锁已经成功获取之后，因此失败结果必须保留：

```text
status   = PROVIDER_ERROR
stage    = FENCING
acquired = true
```

Core 会立即尝试释放锁。释放异常作为 suppressed error 保留，同时发布释放失败事件，但 fencing Provider 异常仍是主错误。

## 7. 业务拒绝语义

业务更新 0 行时：

```java
FencingTokenGuard.requireAccepted(handle,
        token -> repository.updateIfNewer(resourceId, payload, token));
```

模板捕获 `FencingTokenRejectedException` 后返回：

```text
status   = FENCING_REJECTED
stage    = FENCING
acquired = true
```

这不是 Provider 错误，而是资源端明确证明当前 token 已经过期。

## 8. 数据库写入模板

更新已有资源：

```sql
UPDATE resource
SET payload = ?, last_fencing_token = ?
WHERE resource_id = ?
  AND last_fencing_token < ?;
```

首次写入需要处理 INSERT 并发：

1. 条件 UPDATE；
2. UPDATE=0 时尝试 INSERT；
3. INSERT duplicate 时再次条件 UPDATE；
4. 最终仍为 0 才判定旧 token。

直接在 DuplicateKeyException 后返回 false 会错误拒绝并发场景中的更大 token，Demo Repository 已实现二次条件 UPDATE。

## 9. 安全边界

- 不要使用 `>=`；同一 token 的重复写是否允许应由业务幂等单独决定。
- 不要只在应用内比较 token，必须由最终资源存储执行原子条件。
- 不要给 token key/row 设置普通 TTL。
- 不要清空 token 表或 Redis fencing key。
- Redis 原生 fencing 不适合无法容忍 Redis 数据回退的核心账务场景。
- 即便三期使用 ZK/Etcd，也仍建议关键写入使用 fencing。

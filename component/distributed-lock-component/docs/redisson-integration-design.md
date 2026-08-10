# Redisson Provider 融入设计（v26）

## 1. 定位

Redisson 不是新的 DistributedLockClient，也不是替换原有 Redis Lua Provider。
它是第二个 `LockProvider`：

```text
DistributedLockClient
        ↓
LockAcquisitionService
        ↓
LockProviderRegistry
        ├── redis       -> Spring Data Redis + 自研 Lua
        └── redisson    -> Redisson RLock / RFencedLock
```

业务不直接依赖 `RedissonClient` / `RLock`。

## 2. 为什么这时接入

一期/二期已经建立 ownerToken、LockLease、watchdog、fencing、事件指标等统一语义。
现在接入第二个成熟 Provider，可以在幂等组件成为上层消费者之前验证这些 SPI 是否真正可替换。

## 3. ownerToken 与 Redisson owner 的适配

iron-lock 的 owner 是一次 lease 的 `ownerToken`，`LockHandle` 可以跨 Java 线程；Redisson 的 `RLock` owner 则是 Redisson client id + acquire 时的 threadId。

因此 v26 使用 `RedissonOwnershipRegistry` 保存：

```text
ownerToken
   ↓
(lockKey, acquireThreadId)
```

acquire 时使用当前 Java threadId；release/check 即使发生在另一个 Java 线程，也通过 Redisson 的显式 threadId API 使用原始 owner 身份。

同时，Redisson `RLock` 原生支持 reentrant，而 iron-lock 当前没有正式暴露可重入语义。Registry 额外维护 `lockKey + threadId` 占位：同一线程在同一锁尚未结束时，用新的 ownerToken 再次 acquire 会返回 NOT_ACQUIRED，避免底层 reentrant 语义偷偷穿透统一 API。

固定 lease 到期后本地可能暂时留下旧映射；下一次同 threadId + lockKey acquire 时 Provider 会先调用 `isHeldByThread(threadId)` 校验远端真实状态，确认旧 owner 已失效后清理陈旧映射并重新申请。

这里不把本地 Registry 当作正确性依据。真正的 owner-safe unlock 仍由 Redisson 的锁脚本完成；Registry 只解决两个 owner 模型之间的适配。

## 4. 等待模型

API 在正式冻结前把原来的 `PUBSUB_BACKOFF` 泛化成 `PROVIDER_NATIVE`：

- `NO_WAIT`：Provider acquire 一次，wait=0；
- `BACKOFF`：Core 使用 BackoffLockWaiter 多次调用 Provider acquire；
- `PROVIDER_NATIVE`：Core 只调用一次 Provider.acquire，完整 waitTime 交给 Provider。

Redisson 的普通锁/FencedLock 使用原生 Pub/Sub；未来 ZK/Etcd 可把 watch 映射到同一个模式。

## 5. 自动续期模型

新增 `LockAutoRenewMode`：

- `UNSUPPORTED`
- `CORE_MANAGED`：自研 Redis Lua，由 ScheduledLockWatchdog 调 renew.lua；
- `PROVIDER_MANAGED`：Redisson 由内部 watchdog 真正续期，Core watchdog 只 checkHeld + 控制 maxRenewTime。

Redisson watchdog timeout 是 RedissonClient 级配置。因此 v26 规定：

```text
autoRenew=true
=> LockOptions.leaseTime == xjtu.iron.distributed-lock.redisson.watchdog-timeout
```

达到 `maxRenewTime` 后 Core 标记 handle lost，并主动 unlock，防止 Redisson watchdog 无限续期。

## 6. 为什么 Redisson manual renew 明确不支持

公开 RLock API 没有“指定 owner threadId 后原子 compare-owner + reset-TTL”操作。
如果用 `isHeldByThread(threadId)` 再 `expire()`，两个命令之间锁可能已经过期并被别人获得，会错误延长新 owner 的锁。
所以 `manualRenewSupported=false`；需要自动续期时使用 Redisson watchdog。

## 7. Native fencing

当：

```text
providerName=redisson
fencingRequired=true
fencingTokenProviderName=redisson
```

`FencingTokenCoordinator` 产生 `NATIVE` plan，`LockAcquireRequest.nativeFencingRequired=true`，Redisson Provider 使用 `RFencedLock`。

这里必须注意一个非常关键的竞态：不能先 `tryLock()` 成功，再单独调用 `getToken()`。在两个调用之间固定 lease 可能已经过期，并由其他 owner 再次获取锁；此时 `getToken()` 可能读到后来 owner 的 token。

因此 v26 使用 Redisson 公共 API `tryLockAndGetToken(...)`，让本次 acquire 直接返回与本次锁获取对应的 fencing token：

```text
RFencedLock.tryLockAndGetToken(...)
        ↓
获得锁 + 本次 token
        ↓
LockLease.fencingToken
```

随后继续复用我们已有的：

```text
LockLease -> LockHandle -> LockResult -> FencingTokenGuard -> 业务条件写
```

JDBC external fencing 不受影响，仍可组合：

```text
lock provider = redisson
fencing provider = jdbc-sequence
```

此时 `nativeFencingRequired=false`，Redisson 只使用普通 RLock；token 仍由 JDBC sequence 生成。

## 8. Redis 连接配置

仍坚持“连接参数只配置一次”：

```yaml
spring:
  data:
    redis:
      host: ...
      port: ...
      password: ...
```

Redisson 自动配置会读取 Spring Boot 3.5 的 `RedisConnectionDetails`，支持 standalone / sentinel / cluster。
它与 Lettuce 共享配置值，但不会共享物理连接池。
如果应用已有 RedissonClient，则优先复用已有 Bean，并可通过 `client-bean-name` 指定。

## 9. Provider 能力矩阵

| 能力 | redis Lua | redisson |
|---|---|---|
| owner-safe release | yes | yes |
| check | yes | yes |
| manual renew | yes | no |
| auto renew mode | CORE_MANAGED | PROVIDER_MANAGED |
| native fencing | Redis INCR | RFencedLock |
| native wait | no | yes |
| fair lock | API 尚未暴露 | API 尚未暴露 |
| reentrant | API 尚未暴露 | API 尚未暴露 |

## 10. v26 暂不做

- FairLock
- ReadWriteLock
- MultiLock
- SpinLock
- 公共可重入语义

先把普通互斥、native wait、provider watchdog、RFencedLock 和统一状态映射做稳，再扩展能力。

## v26：手工 tryLock 与 execute 的 autoRenew 语义统一

上一版 watchdog 只在 `LockExecutionTemplate.execute()` 中启动，导致：

- `execute(autoRenew=true)`：自研 Redis 会自动续期；
- `tryLock(autoRenew=true)`：自研 Redis 不会自动续期；
- Redisson 因 Provider 内部 watchdog 又会续期。

这会形成跨 Provider、跨 API 的语义不一致。v26 把 watchdog 启动点前移到
`AcquiredLockAcquireOutcomeHandler -> LockHandleFactory`：只要 acquire 最终成功并创建 Handle，
`autoRenew=true` 就立即生效。`DefaultLockHandle.releaseWithOutcome()` 在 finally 中立即停止 watchdog。

因此现在：

```text
tryLock + redis     -> CORE_MANAGED watchdog
execute + redis     -> CORE_MANAGED watchdog
tryLock + redisson  -> PROVIDER_MANAGED TTL + Core check/maxRenewTime
execute + redisson  -> PROVIDER_MANAGED TTL + Core check/maxRenewTime
```

四条路径统一。

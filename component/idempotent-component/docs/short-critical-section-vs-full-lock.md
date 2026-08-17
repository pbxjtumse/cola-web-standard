# 为什么 DistributedLock 只保护 tryAcquire / tryRecover，而不包住整个业务

## 1. V1.3 推荐模型

```text
DistributedLockClient（可选）
        ↓
IdempotencyRepository.tryAcquire()
        ↓
数据库 INSERT / SELECT FOR UPDATE / CAS 已经完成
        ↓
释放 DistributedLock
        ↓
只有 ACQUIRED owner 执行业务 callback
        ↓
markSuccess / markFailed
```

`tryAcquire()` 返回 `ACQUIRED` 时，执行权已经持久化为：

```text
status=PROCESSING
ownerToken=A
version=1
processingExpireAt=...
```

因此从这一刻起，后续 B/C 请求不需要依赖前面的 Redisson 锁是否仍存在；它们再次进入 Repository 时，会看到 PROCESSING 并被状态机挡住。

## 2. 为什么不把锁包住完整业务

### 短锁模型

A：

```text
lock
  tryAcquire -> INSERT PROCESSING
unlock
业务执行 10s
markSuccess
```

B 在 A 执行业务期间到达：

```text
lock
  tryAcquire -> 看到 PROCESSING
unlock
返回 PROCESSING
```

优点：

- 分布式锁只持有毫秒级；
- 业务慢不会长期占 Redis/ZK 锁；
- PROCESSING 可以被其他请求立即观察；
- 即使锁失效，Repository 仍能独立保证正确性。

### 全业务锁模型

```text
lock
  tryAcquire
  business 10s
  markSuccess
unlock
```

问题：

- B/C 会先阻塞在分布式锁，甚至根本看不到 PROCESSING；
- 业务耗时决定锁租期，必须引入 watchdog/续期；
- GC、网络抖动、锁租约失效后仍可能出现旧执行者继续运行；
- 组件正确性会错误地依赖 Lock Provider；
- 高并发同 key 场景下等待线程/请求会堆积。

因此分布式锁在幂等组件中的定位是“瞬时竞争收敛”，不是完整业务执行权的唯一载体。

## 3. 数据库里到底已经完成了什么

对于 JDBC DURABLE，`tryAcquire()` 已经完成数据库级状态操作：

- 首次请求：`INSERT PROCESSING` + UNIQUE 唯一键抢占；
- 已存在请求：短事务 `SELECT ... FOR UPDATE` 后判断 SUCCESS / PROCESSING / FAILED；
- WINDOWED 窗口结束：可原子开启新 generation；
- 普通请求发现 PROCESSING 超时：只返回 `PROCESSING_EXPIRED`，不自动接管。

所以业务 callback 开始前，数据库已经有一个其他节点可见的幂等执行事实。


> V1.3 补充：Lock 发生在 Repository 原子状态抢占外围；StateMachine 在 CAS/Lua 之后解释结果；Transaction Integration 发生在获得 EXECUTE generation 之后。三者不在同一层。

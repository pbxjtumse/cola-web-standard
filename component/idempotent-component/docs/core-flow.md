# 从零到一理解幂等核心流程

## 1. 幂等解决什么

幂等不保证请求只到达一次，而是保证：

> 同一个逻辑请求无论被 HTTP 重试、MQ 重投、用户连点或多个 Pod 同时处理多少次，都不能产生多个合法业务效果。

## 2. 识别同一个逻辑请求

### key

逻辑请求身份证，例如：

```text
order:create:REQ-1001
payment:PAY-001
message:MSG-888
```

### namespace

隔离不同业务动作：

```text
(order-create, 1001)
(refund, 1001)
```

是两个不同幂等请求。

### requestHash

防止同一个 key 被不同业务内容错误复用：

```text
key=PAY-001, amount=100 -> hash=A
key=PAY-001, amount=500 -> hash=B
```

同 key 不同 hash 返回 `KEY_CONFLICT`。

### routeKey

用于数据库/任务/事务路由，不替代 idempotencyKey。Recovery 必须沿用原 routeKey。

## 3. Request 与 Policy

Request 只描述本次请求：

```text
key
requestHash
routeKey
policyName / inline policy
```

Policy 描述一类业务的稳定策略：

```text
mode
namespace
repositoryName
processingTimeout
idempotencyWindow
recordRetentionTtl
RecoveryPolicy
LockOptions
```

## 4. execute() 总流程

```text
Request
  ↓
Policy Resolution
  ↓
Repository Resolution + capabilities check
  ↓
生成 ownerToken
  ↓
optional short DistributedLock
  ↓
Repository.tryAcquire()
  ↓
StateMachine
  ├─ EXECUTE
  ├─ REPLAY
  └─ RETURN
```

## 5. Repository.tryAcquire() 是正确性核心

### JDBC 首次请求

```sql
INSERT ... status='PROCESSING', owner_token=?, version=1
```

通过：

```text
UNIQUE(namespace, idempotency_key)
```

保证并发首次请求只能一个成功。

Duplicate 后进入短事务并 `SELECT ... FOR UPDATE`，再原子判断当前状态、窗口、hash、routeKey 等。

### Redis

同样的读取、判断、必要状态转换通过 Lua 一次完成，避免 GET -> Java 判断 -> SET 的竞态。

## 6. Repository 判定后的 StateMachine

典型映射：

```text
ACQUIRED           -> EXECUTE
SUCCESS            -> REPLAY
PROCESSING_ACTIVE  -> RETURN PROCESSING
PROCESSING_EXPIRED -> RETURN PROCESSING_EXPIRED
FAILED_RETRYABLE   -> RETURN PREVIOUS_FAILED_RETRYABLE
FAILED_FINAL       -> RETURN PREVIOUS_FAILED_FINAL
KEY_CONFLICT       -> RETURN KEY_CONFLICT
```

StateMachine 不访问数据库，也不替代 CAS。

## 7. 为什么普通 execute() 不自动接管过期任务

`PROCESSING_EXPIRED` 只说明 execution lease 过期，不表示旧 Java 线程已经死亡。旧线程可能只是 Full GC、STW、网络停顿或长时间调度不到。

因此普通请求只返回状态；真正 Recovery 必须由外部 Reliable Task 显式触发。

## 8. ACQUIRED 后执行 Business

只有 `EXECUTE` 才创建 `IdempotencyContext` 并执行 callback。

Context 主要包含：

```text
namespace
key
routeKey
ownerToken
generationVersion
mode
processingExpireAt
recoveryExecution
```

`generationVersion` 是幂等 generation 版本，不等同于分布式锁全生命周期 fencing token。

## 9. 成功路径

支持本地事务时：

```text
Tx-B REQUIRED
  Business
  ResultPolicy.capture
  markSuccess(ownerToken, version)
```

`markSuccess` 必须使用 owner/version 条件更新。

如果旧 owner 已被 Recovery 淘汰，CAS 更新 0 行，Core 通过内部异常让整个 Tx-B rollback，防止 stale owner 的业务 SQL 泄漏提交。

## 10. 失败路径

Business / ResultPolicy.capture / markSuccess 任一步失败：

```text
Tx-B rollback
    ↓
Tx-C REQUIRES_NEW
    ↓
markFailed(ownerToken, version)
```

FAILED 同样受 owner/version CAS 保护。

## 11. 重复 SUCCESS

重复请求命中 SUCCESS 时 callback 不执行：

```text
SUCCESS
  ↓
StateMachine -> REPLAY
  ↓
ResultPolicy.replay
  ↓
REPLAYED
```

这就是 Replay，不是 Retry。

## 12. Recovery

```text
External scanner
  ↓
Candidate(owner=A, version=10)
  ↓
Reliable Task
  ↓
recover(expectedOwner=A, expectedVersion=10)
  ↓
optional short lock
  ↓
Repository.tryRecover CAS
```

如果 current 已变成 C/11，返回 `STALE_CANDIDATE`；只有仍匹配且 RecoveryPolicy 允许时，才原子产生新 generation：

```text
owner=B
version=11
status=PROCESSING
```

然后复用与普通执行相同的 Tx-B / Tx-C 主链。

## 13. COMMIT_UNKNOWN

如果数据库实际可能已经 COMMIT，但确认响应丢失：

```text
COMMIT_UNKNOWN
```

不能立即写 FAILED，也不能自动重执行业务。当前返回 `TRANSACTION_COMMIT_UNKNOWN`，后续通过查询、对账或 Recovery 收敛。

## 14. 一句话总结

```text
Repository 决定谁有资格执行；
StateMachine 决定知道结果后做什么；
Transaction 决定业务数据和 SUCCESS 能否一起提交；
ResultPolicy 决定历史 SUCCESS 怎样返回；
Recovery 决定异常 generation 谁能成为下一代。
```

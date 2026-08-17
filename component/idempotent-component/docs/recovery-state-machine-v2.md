# Idempotent Component V2 - Recovery State Machine

## 1. 先定边界

Recovery 不是普通接口请求的“自动重试”。普通 `execute()` 遇到 `PROCESSING_EXPIRED` 或 `FAILED_RETRYABLE` 只返回判定结果，不会自动接管。真正的接管必须由外部 Reliable Task / 调度中心显式调用 `recover()`。

核心原因很简单：

```text
普通请求线程不知道是否应该重新执行业务。
恢复线程必须带着扫描时看到的 owner/version，进入 Repository CAS 再判断一次。
```

所以 Recovery 的正确性不靠“扫描任务认为它可以恢复”，而靠 Repository 的原子状态机最终决定。

---

## 2. Recovery 主链路

```text
External Scanner / Reliable Task
        │
        │ 1. findRecoveryCandidates(namespace, routeKey, now, limit)
        ▼
Candidate Snapshot
        │
        │ 携带 key / requestHash / routeKey / expectedOwner / expectedVersion
        ▼
IdempotencyExecutor.recover()
        │
        ▼
optional DistributedLock
contention reduction only
        │
        ▼
Repository CAS
tryRecover(expectedOwner, expectedVersion)
        │
        ├── STALE_CANDIDATE
        ├── NOT_RECOVERABLE
        ├── PROCESSING_ACTIVE
        ├── SUCCESS
        ├── FAILED_FINAL
        ├── KEY_CONFLICT
        └── RECOVERY_ACQUIRED(newOwner, version+1)
                                │
                                ▼
                         Transaction integration
                         Tx-B REQUIRED
                                │
                                ▼
                       Business + final state
                       callback + markSuccess
                                │
                ┌───────────────┴───────────────┐
                │                               │
          SUCCESS / RECOVERED              Tx-B failure
                │                               │
                ▼                               ▼
        duplicate can replay             Tx-C REQUIRES_NEW
        status/result                    markFailed
```

这张图里的顺序不能反：

1. `optional Lock` 只能减少同 key 并发打到 Repository 的压力；
2. 真正能决定是否接管的是 `Repository CAS`；
3. 只有 `RECOVERY_ACQUIRED` 才能执行业务；
4. JDBC + transaction-component 时，业务写和 `markSuccess` 必须在 Tx-B 同一个本地事务中；
5. 业务失败后 `markFailed` 必须是 Tx-C 独立事务，否则失败状态会跟着业务事务一起回滚。

---

## 3. Repository CAS 的判定表

| 当前持久状态 | 条件 | tryRecover 返回 | 是否执行业务 |
|---|---|---|---|
| 不存在 | 记录已经清理或从未创建 | `NOT_FOUND` | 否 |
| 任意 | `recoveryMode != EXTERNAL_TASK` | `NOT_RECOVERABLE` | 否 |
| 任意 | SHORT_TERM 窗口已经结束 | `NOT_RECOVERABLE` | 否 |
| 任意 | `expectedOwner/version` 与当前不一致 | `STALE_CANDIDATE` | 否 |
| 任意 | `requestHash/routeKey` 冲突 | `KEY_CONFLICT` | 否 |
| `SUCCESS` | 已完成 | `SUCCESS` | 否，走 replay |
| `PROCESSING` | 未超时 | `PROCESSING_ACTIVE` | 否 |
| `PROCESSING` | 已超时 | `RECOVERY_ACQUIRED` | 是 |
| `FAILED` | `failureRetryable=false` | `FAILED_FINAL` | 否 |
| `FAILED` | `failureRetryable=true && recoverFailed=true` | `RECOVERY_ACQUIRED` | 是 |

注意：`RECOVERY_ACQUIRED` 会产生新的 `ownerToken`，并让 `version + 1`。旧 owner 后续即使恢复，也无法通过 `markSuccess(owner, version)` 条件写覆盖新 generation。

---

## 4. 与事务模板的关系

V2 的 JDBC DURABLE 推荐事务分段是：

```text
Tx-A REQUIRES_NEW
  tryAcquire / tryRecover
  PROCESSING 落库并提交

Tx-B REQUIRED
  business callback
  + markSuccess(ownerToken, version)

Tx-C REQUIRES_NEW
  Tx-B 失败后 markFailed(ownerToken, version)
```

这三个事务不能揉成一个长事务。Tx-A 必须短，因为其他节点需要尽快看到 `PROCESSING`；Tx-B 必须覆盖业务写和 `SUCCESS`，因为不能出现“业务提交了但幂等还没成功”的窗口；Tx-C 必须独立，因为 Tx-B 已经回滚，失败状态不能再加入已经失败的事务。

---

## 5. ResultPolicy 与 Recovery 的关系

Recovery 成功后也会进入 `SUCCESS`。如果 `resultPolicy=STORE_AND_REPLAY`，恢复执行的 callback 返回值同样会被保存，后续重复请求可以回放恢复后的结果。

如果 `resultPolicy=STATUS_ONLY`，后续重复请求仍会返回 `REPLAYED`，但 `value` 为空。它只表示“这件事历史上已经成功”，不是“返回第一次的响应体”。

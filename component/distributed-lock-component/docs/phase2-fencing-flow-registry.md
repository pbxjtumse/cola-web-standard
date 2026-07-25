# Phase 2 Fencing Flow Registry Refactor

本次重构的目标是把 `DefaultDistributedLockClient` 从 Redis native / JDBC external 的细节中解耦出来。

## 三个扩展点

| 扩展点 | 职责 | 示例 |
| --- | --- | --- |
| `LockProviderRegistry` | 选择谁负责加锁、解锁、续期、check | `redis`、未来 `zookeeper`、`etcd` |
| `FencingTokenProviderRegistry` | 选择谁负责外部发号 | `jdbc-sequence`、未来 `postgres-sequence` |
| `FencingTokenFlowRegistry` | 选择某种 fencing 模式如何执行 | `NONE`、`NATIVE`、`EXTERNAL` |

## 当前 Flow

```text
NoFencingTokenFlow
    mode = NONE
    拿到锁后直接返回 lease。

NativeFencingTokenFlow
    mode = NATIVE
    Redis / Etcd 等 LockProvider 原生发号。
    acquire 成功后 lease 必须已经携带 fencingToken。

ExternalFencingTokenFlow
    mode = EXTERNAL
    Redis 先加锁，JDBC sequence 等外部 Provider 再发号。
    发号完成后必须重新 check ownerToken 仍然持锁。
```

## 主流程变化

原来：

```text
DefaultDistributedLockClient
  switch fencingPlan.mode()
    NONE
    NATIVE
    EXTERNAL
```

现在：

```text
LockAcquireOutcomeHandlerRegistry
  -> AcquiredLockAcquireOutcomeHandler
  -> FencingTokenFlowRegistry.getRequired(fencingPlan.mode())
  -> FencingTokenFlow.complete(context)
```

因此新增 ZK / Etcd 时，如果它只是标准 `LockProvider`，不需要修改 `DefaultDistributedLockClient` 或 acquire 主流程。
如果它支持原生 fencing，只需要在 `acquire()` 返回的 `LockLease` 中携带 token，并声明 capability。

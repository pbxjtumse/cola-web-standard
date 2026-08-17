# CHANGELOG V2.1

## 定位

V2.1 以当前 V2 工程为基线，一次性完成此前讨论的 V1.3 API 增强与职责分包；不改变 Tx-A / Tx-B / Tx-C、owner/version CAS、Reliable Recovery 等正确性原则。

## API

- 主 `IdempotencyExecutor` 移除 `Class<T>` 参数。
- `IdempotencyRequest` 与 `IdempotencyPolicy` 解耦。
- 新增命名 `IdempotencyPolicyRegistry`。
- `SHORT_TERM` 兼容保留，正式语义使用 `WINDOWED`。
- `IdempotencyOptions` 标记为兼容 API。

## Result Replay

新增类型安全 `IdempotencyResultPolicy<T>`：

- `NONE`
- `SNAPSHOT`
- `REFERENCE`

新增：

- `IdempotencyTypeRef<T>`
- `IdempotencySnapshotPolicyFactory`
- `StoredResultEnvelope`
- `RESULT_REPLAY_UNAVAILABLE`
- `RESULT_POLICY_MISMATCH`
- `RESULT_POLICY_ERROR`

## State Machine

新增纯决策层：

- `IdempotencyStateMachine`
- `DefaultIdempotencyStateMachine`
- `IdempotencyStateDecision`
- `IdempotencyStateAction`

Repository 仍负责原子正确性；StateMachine 只翻译为 EXECUTE / REPLAY / RETURN。

## Recovery

新增 `IdempotencyRecoveryPolicy`，集中表达：

- recovery mode
- processing timeout recovery
- retryable failure recovery

普通 execute() 仍不会自动接管 expired PROCESSING / retryable FAILED。

## Repository

新增 `IdempotencyRepositoryCapabilities`，Provider 显式声明：

- WINDOWED
- DURABLE
- result payload
- local business transaction participation
- recovery query

## Package Refactor

API、Core、JDBC/Redis Provider、Transaction Integration、Starter 均按职责拆包。详见 `v2.1-package-architecture.md`。

## Transaction

保留并强化：

```text
Tx-A REQUIRES_NEW : PROCESSING / recovery claim
Tx-B REQUIRED     : Business + ResultPolicy.capture + markSuccess
Tx-C REQUIRES_NEW : markFailed
```

旧 owner 的 markSuccess CAS 失败仍通过异常触发整个 Tx-B rollback。

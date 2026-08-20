# 源码阅读指南：按主流程读，不按包名乱跳

这份指南配合代码中的 `[01]~[11]`、`[A1]~[A5]`、`[R1]~[R3]` 注释使用。

## 1. 第一遍：只看普通 execute() 主链

推荐顺序：

```text
IdempotencyExecutor
  ↓
IdempotencyRequest
  ↓
IdempotencyPolicy
  ↓
DefaultIdempotencyPolicyRegistry
  ↓
DefaultIdempotencyExecutor.execute()
  ↓
invokeWithOptionalLock()
  ↓
IdempotencyRepository.tryAcquire()
  ↓
JdbcIdempotencyRepository.tryAcquireInTransaction()
  或 RedisIdempotencyRepository.tryAcquire()
  ↓
DefaultIdempotencyStateMachine.onAcquire()
  ↓
DefaultIdempotencyExecutor.executeOwned()
  ↓
executeOwnedTransactionally() / executeOwnedWithoutBusinessTransaction()
  ↓
markSuccess(ownerToken, version)
  ↓
replay()
```

第一遍只回答五个问题：

1. 请求怎样被识别为“同一个逻辑请求”；
2. 谁真正决定当前 generation 的 owner；
3. StateMachine 为什么必须在 Repository 原子判断之后；
4. Business 为什么只有 ACQUIRED 才执行；
5. SUCCESS 为什么仍然必须带 ownerToken + version 条件。

## 2. 第二遍：只看 JDBC Tx-A / Tx-B / Tx-C

```text
JdbcIdempotencyRepository.tryAcquire()
  ↓
JdbcExecutionManager.inNewTransaction()
  ↓
Tx-A：PROCESSING 先提交

DefaultIdempotencyExecutor.executeOwnedTransactionally()
  ↓
TransactionTemplateIdempotencyTransactionCoordinator.executeRequired()
  ↓
Tx-B：Business + capture + markSuccess

business / capture / final CAS 失败
  ↓
Tx-B rollback
  ↓
JdbcIdempotencyRepository.markFailed()
  ↓
Tx-C：FAILED 独立提交
```

重点看两个桥梁：

- `SpringTransactionJdbcExecutionManager`
- `TransactionTemplateIdempotencyTransactionCoordinator`

## 3. 第三遍：再看 Result Replay

```text
tryAcquire() -> SUCCESS
  ↓
StateMachine -> REPLAY
  ↓
DefaultIdempotencyExecutor.replay()
  ↓
NONE / SNAPSHOT / REFERENCE
```

记住：

```text
Retry  = 再执行 Business
Replay = 不执行 Business，复用历史 SUCCESS
```

## 4. 第四遍：最后看 Recovery

```text
DefaultIdempotencyRecoveryQueryService
  ↓
candidate(owner=A, version=10)
  ↓
IdempotencyExecutor.recover()
  ↓
Repository.tryRecover(expectedOwner=A, expectedVersion=10)
  ↓
STALE_CANDIDATE / RECOVERY_ACQUIRED
  ↓
如果成功，复用 executeOwned()
```

Recovery 不是普通重试，它是“旧 generation 异常以后，通过二次 CAS 安全地产生下一代 generation”。

## 5. 代码格式约定

当前源码采用“阅读型”格式：

- 一条完整表达式在约 140 字符内能清晰表达时，不为了形式对齐强行拆成多行；
- Builder 链、SQL、参数很多的方法仍按语义分组换行；
- 不为了追求单行而把复杂逻辑压成难读的一坨；
- 关键流程优先写“为什么这么做”的注释，而不是重复代码字面含义。

`.editorconfig` 已写入 `max_line_length = 140`，便于后续保持一致。

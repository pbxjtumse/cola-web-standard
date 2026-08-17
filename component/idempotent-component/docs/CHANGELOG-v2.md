# Idempotent Component V2 Changelog

## 1. 版本定位

V2 不是把幂等、锁、事务、重试、任务调度揉成一个大组件，而是把分布式幂等的核心链路定死：

```text
Repository CAS
   ├── optional DistributedLock：只减少热点竞争
   └── transaction-component：只负责本地事务边界
          ↓
Business + final state
```

幂等正确性仍然由 Repository 的原子状态转换保证；分布式锁不是正确性来源；事务模板只解决 JDBC DURABLE 场景下“业务写 + SUCCESS”同本地事务的问题。

## 2. 主要修改

### 2.1 引入 ResultPolicy

新增：

```java
IdempotencyResultPolicy.STATUS_ONLY
IdempotencyResultPolicy.STORE_AND_REPLAY
```

旧的 `storeResult(boolean)` 保留兼容，但已经不建议新代码继续使用。

原因：`storeResult=true/false` 只能回答“存不存”，不能清楚表达“重复请求命中 SUCCESS 时到底回放什么”。V2 改为：

- `STATUS_ONLY`：只回放成功语义，`value` 为空；
- `STORE_AND_REPLAY`：保存 resultPayload，重复请求回放第一次的业务返回值。

### 2.2 明确 Recovery 状态机

新增文档：

```text
docs/recovery-state-machine-v2.md
docs/diagrams/state/L2-recovery-state.puml
```

Recovery 链路明确为：

```text
findRecoveryCandidates
  -> recover(expectedOwner, expectedVersion)
  -> optional lock
  -> Repository CAS
  -> RECOVERY_ACQUIRED
  -> Tx-B business + markSuccess
  -> Tx-C markFailed on failure
```

普通 `execute()` 仍不会自动接管 `PROCESSING_EXPIRED` 或 `FAILED_RETRYABLE`。

### 2.3 事务集成口径收敛为 V2

`idempotent-integration-transaction` 继续保留两个关键类：

- `TransactionTemplateIdempotencyTransactionCoordinator`
- `SpringTransactionJdbcExecutionManager`

职责分工：

- Tx-A：`tryAcquire / tryRecover` 使用 `REQUIRES_NEW` 短事务；
- Tx-B：业务 callback + `markSuccess` 使用 `REQUIRED`；
- Tx-C：业务失败后 `markFailed` 使用 `REQUIRES_NEW`；
- `COMMIT_UNKNOWN` 不写 FAILED，保留 PROCESSING，等待后续查询或恢复收敛。

### 2.4 补充结果回放测试

新增 Core 测试：重复请求命中 SUCCESS 时，如果 `resultPolicy=STORE_AND_REPLAY`，第二次不会执行 callback，而是直接返回第一次保存的结果。

## 3. 兼容性说明

### 3.1 Java API

旧写法仍可用：

```java
IdempotencyOptions.builder()
        .storeResult(true)
        .build();
```

推荐新写法：

```java
IdempotencyOptions.builder()
        .resultPolicy(IdempotencyResultPolicy.STORE_AND_REPLAY)
        .build();
```

### 3.2 配置文件

旧配置仍可绑定：

```yaml
xjtu:
  iron:
    idempotent:
      store-result: true
```

推荐新配置：

```yaml
xjtu:
  iron:
    idempotent:
      result-policy: STORE_AND_REPLAY
```

如果同时配置，`store-result` 作为兼容字段优先生效。生产建议只保留 `result-policy`，避免歧义。

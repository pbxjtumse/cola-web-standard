# CHANGELOG V1.3 — API Cleanup + Result Replay Policy + Recovery/StateMachine Boundary

## 1. 版本目标

V1.3 基于已经存在 `idempotent-integration-transaction` 的 V1.2 继续演进，不推翻三段事务模型。

本次主要解决：

1. `Class<T>` 不应该出现在幂等 Executor 主 API；
2. `storeResult boolean` 表达能力不足；
3. 请求身份与稳定执行策略混在一个 Options 中；
4. `SHORT_TERM` 命名容易误导；
5. Mode 与具体 Provider 不应硬编码绑定；
6. Recovery 的“扫描候选 / 原子接管 / 状态解释”边界需要显式；
7. 分布式锁、Repository CAS、StateMachine、Transaction 的先后关系需要固定。

---

## 2. API

### Removed from recommended API

```java
execute(request, Class<T>, callback)
```

### New

```java
execute(request, callback)

execute(request, IdempotencyResultPolicy<T>, callback)
```

新增：

```text
IdempotencyResultPolicy<T>
IdempotencyResultPolicyType
IdempotencyResultPolicies
IdempotencyResultSerializer<T>
IdempotencyResultReference<T>
IdempotencySnapshotPolicyFactory
IdempotencyTypeRef<T>
```

---

## 3. Result replay

新增三种策略：

```text
NONE
SNAPSHOT
REFERENCE
```

持久 payload 使用内部 envelope 区分策略类型，避免错误解释历史 payload。

新增结果状态：

```text
RESULT_POLICY_ERROR
RESULT_REPLAY_UNAVAILABLE
RESULT_POLICY_MISMATCH
```

---

## 4. Policy

新增：

```text
IdempotencyPolicy
IdempotencyRecoveryPolicy
IdempotencyPolicyRegistry
DefaultIdempotencyPolicyRegistry
IdempotencyExecutionDefinition<T>
```

请求支持：

```text
policyName
inline policy
```

优先级：

```text
inline policy > legacy options > policyName > default policy
```

---

## 5. Lifecycle

正式增加：

```java
IdempotencyMode.WINDOWED
```

旧：

```java
SHORT_TERM
```

保留 deprecated alias。

---

## 6. Repository capabilities

新增：

```java
IdempotencyRepositoryCapabilities
```

Provider 显式声明：

```text
WINDOWED support
DURABLE support
result payload support
business transaction participation
recovery query support
```

Registry 不再靠 Provider 名称猜能力。

---

## 7. State Machine

新增：

```text
IdempotencyStateMachine
DefaultIdempotencyStateMachine
IdempotencyStateAction
IdempotencyStateDecision
```

State Machine 只解释 Repository 已经完成的原子 CAS/Lua 结果。

它不替代 Repository 原子性。

---

## 8. Recovery

新增命名 Policy 扫描入口：

```java
recoveryQueryService.findCandidates(policyName, query)
```

`RecoveryPolicy` 明确区分：

```text
recoverProcessingTimeout
recoverRetryableFailure
```

普通 `execute()` 仍不自动接管。

---

## 9. Transaction

V1.2 三段事务完整保留：

```text
Tx-A REQUIRES_NEW
Tx-B REQUIRED
Tx-C REQUIRES_NEW
```

并把 ResultPolicy.capture 纳入 Tx-B：

```text
Business
+
ResultPolicy.capture
+
markSuccess
```

`markSuccess` owner/version 失效仍会强制让 Tx-B rollback。

`COMMIT_UNKNOWN` 仍不写 FAILED。

---

## 10. Starter

新增命名 Policy 配置：

```yaml
xjtu.iron.idempotent.policies.<name>
```

内建：

```text
windowed-default
durable-default
```

用户可用同名自定义 Policy 显式覆盖内建默认。

Jackson 不再注册为 Executor 全局 ResultCodec，而是提供：

```text
IdempotencySnapshotPolicyFactory
```

支持完整泛型 Type。

---

## 11. Compatibility

暂时保留 deprecated：

```text
IdempotencyOptions
IdempotencyMode.SHORT_TERM
IdempotencyResultCodec
JacksonIdempotencyResultCodec
shortTerm Java property accessors
```

它们用于迁移，不是 V1.3 推荐入口。

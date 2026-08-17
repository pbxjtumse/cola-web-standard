# Recovery、可靠任务与路由

## 1. Recovery 的定位

Recovery 不等于：

```text
普通请求发现 PROCESSING 超时
→ 当场重试业务
```

V1.3 固定原则：

> 普通 `execute()` 负责“去重和状态返回”；异常 generation 的接管由显式 `recover()` 完成。

这样可以避免请求线程在看到一个时间条件后擅自把旧 Owner 踢掉。

---

## 2. 正常执行看到异常状态

### PROCESSING_ACTIVE

```text
当前 generation 仍在租约内
→ 返回 PROCESSING
→ 不执行业务
```

### PROCESSING_EXPIRED

```text
当前 generation 租约已过期
→ 返回 PROCESSING_EXPIRED
→ 不自动接管
```

### FAILED_RETRYABLE

```text
历史失败允许恢复
→ 普通请求仍返回 PREVIOUS_FAILED_RETRYABLE
→ 不自动执行
```

恢复策略只意味着“可靠任务可以尝试接管”，不是“所有来访请求都自动重试”。

---

## 3. Reliable Task 扫描

推荐：

```java
List<IdempotencyRecoveryCandidate> candidates =
        recoveryQueryService.findCandidates(
                "order-create",
                new IdempotencyRecoveryQuery(
                        "order",
                        routeKey,
                        now,
                        100
                ));
```

V1.3 优先使用 `policyName`，避免扫描器重新手工维护：

```text
mode
repositoryName
namespace
```

造成策略漂移。`policyName` 入口会以 Policy 中的 namespace 为准；Query 中的 routeKey / now / limit 仍由扫描任务提供。

扫描结果只是候选项。

---

## 4. 扫描与执行之间必须二次 CAS

候选：

```text
key = K
owner = A
version = 10
```

调度有延迟。

真正执行恢复时：

```java
IdempotencyRecoveryRequest.builder()
        .key("K")
        .policyName("order-create")
        .expectedOwnerToken("A")
        .expectedVersion(10)
        .build();
```

Repository 在一个原子边界里再次检查：

```text
current.owner == A
current.version == 10
```

如果已经被别人接管成：

```text
B / 11
```

返回：

```text
STALE_CANDIDATE
```

绝不执行业务。

---



## 4.1 namespace 是 Recovery 隔离边界

当前 V1.3 的持久记录没有额外保存 `policyName` 字段，Recovery 查询最终仍按：

```text
Repository + namespace + recovery_mode + state/time
```

筛选。

因此同一个 `Repository + namespace` 内的 key 应使用兼容的生命周期/Recovery 语义。
推荐直接把 namespace 设计成稳定的业务动作域，例如：

```text
order-create
payment-submit
settlement-batch
```

不要让两个处理语义完全不同的命名 Policy 共用同一个 Repository + namespace。

如果未来需要在同一 namespace 内混用大量不同 Recovery Policy，再考虑把 `policyName/policyId`
持久化为记录字段并纳入扫描索引；V1.3 不为了这个未来场景提前扩表。

## 5. RecoveryPolicy

V1.3：

```java
IdempotencyRecoveryPolicy
```

明确两个允许接管的来源：

```text
recoverProcessingTimeout
recoverRetryableFailure
```

并由：

```text
IdempotencyRecoveryMode.NONE
IdempotencyRecoveryMode.EXTERNAL_TASK
```

决定是否允许外部可靠任务。

例如 DURABLE：

```text
EXTERNAL_TASK
processing timeout -> allowed
retryable failure  -> allowed
```

WINDOWED 默认：

```text
NONE
```

因为大量短窗口接口并不值得引入扫描恢复体系。

---

## 6. Recovery 状态转换

```text
PROCESSING(owner=A, version=10)
      │
      │ timeout + policy allows
      ▼
PROCESSING(owner=B, version=11)
```

或者：

```text
FAILED(owner=A, version=10, retryable=true)
      │
      │ policy allows
      ▼
PROCESSING(owner=B, version=11)
```

注意持久状态始终只有：

```text
PROCESSING
SUCCESS
FAILED
```

`PROCESSING_EXPIRED` / `FAILED_RETRYABLE` / `STALE_CANDIDATE` 是运行时判定结果，不是新持久状态。

---

## 7. routeKey

`routeKey` 不是幂等唯一键。

它用于：

```text
数据库分片
任务分片
未来事务路由
恢复任务路由
```

幂等唯一身份仍然主要是：

```text
namespace + idempotencyKey
```

相同 key 如果后续突然使用不同 routeKey，Repository 应判为：

```text
KEY_CONFLICT
```

而不是悄悄跨分片处理。

---

## 8. Recovery 与分布式锁

恢复也可以使用短锁：

```text
lock(K)
  ↓
tryRecover CAS
  ↓
unlock
```

但正确性仍然来自：

```text
expectedOwner + expectedVersion
```

即使锁失效，过时恢复任务也必须被 Repository 拒绝。

---

## 9. Recovery 与 Transaction

恢复成功以后得到新 generation：

```text
owner=B
version=11
```

后续与普通 ACQUIRED 完全相同：

```text
StateMachine -> EXECUTE
    ↓
Tx-B REQUIRED
Business
+
ResultPolicy.capture
+
markSuccess(B,11)
```

失败则：

```text
Tx-B rollback
Tx-C markFailed(B,11)
```

Recovery 只负责“换 Owner”，不会另造一套业务执行模型。

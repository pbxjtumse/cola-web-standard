# V1.3 配置说明

## 1. 推荐配置

```yaml
xjtu:
  iron:
    idempotent:
      enabled: true

      # request 未显式指定 policyName / inline policy 时使用
      default-policy: durable-default

      default-windowed-repository: redis
      default-durable-repository: jdbc
      processing-timeout: 30s

      windowed:
        idempotency-window: 10m
        window-policy: FIXED_FROM_FIRST_ACQUIRE
        record-retention-ttl: 0s
        recovery-mode: NONE
        recover-processing-timeout: false
        recover-failed: false

      durable:
        recovery-mode: EXTERNAL_TASK
        recover-processing-timeout: true
        recover-failed: true

      lock:
        enabled: false
        wait-time: 0s
        lease-time: 5s
        fallback-to-state-on-failure: true

      transaction:
        enabled: true
        require-template: false

      redis:
        enabled: true
        key-prefix: iron:idempotency

      jdbc:
        enabled: true
        table-name: iron_idempotency_record

      policies:
        api-submit:
          mode: WINDOWED
          namespace: api
          repository-name: redis
          processing-timeout: 10s
          idempotency-window: 5m
          window-policy: FIXED_FROM_FIRST_ACQUIRE
          record-retention-ttl: 1h
          recovery-mode: NONE
          lock-enabled: true
          lock-wait-time: 0s
          lock-lease-time: 3s

        order-create:
          mode: DURABLE
          namespace: order
          repository-name: jdbc
          processing-timeout: 30s
          recovery-mode: EXTERNAL_TASK
          recover-processing-timeout: true
          recover-failed: true
          lock-enabled: true
```

---

## 2. Policy 解析优先级

```text
inline IdempotencyPolicy
    >
legacy IdempotencyOptions
    >
policyName
    >
default-policy
```

V1.3 新代码优先使用：

```java
IdempotencyRequest.builder()
        .key(requestId)
        .policyName("order-create")
        .build();
```

而不是每次在业务代码中拼：

```java
IdempotencyOptions.builder()...
```

`IdempotencyOptions` 只作为 V1.2 兼容层保留。

---

## 3. WINDOWED 配置

### processing-timeout

当前 generation 的执行权租约。

它不是幂等窗口。

### idempotency-window

相同 key 在多长时间内仍然属于同一个逻辑幂等请求。

必须：

```text
idempotencyWindow > processingTimeout
```

否则一个 generation 还可能合法 PROCESSING，整个幂等窗口却已经结束，语义冲突。

### window-policy

`FIXED_FROM_FIRST_ACQUIRE`

```text
T0 首次 acquire
窗口固定为 [T0, T0 + window]
重复访问不延长
```

`SLIDING_ON_ACCESS`

```text
窗口有效期间每次有效访问/完成都会推进到 now + window
```

### record-retention-ttl

语义窗口结束后额外保留旧物理记录的时间。

例如：

```text
window = 5m
retention = 1h
```

5 分钟后允许新 generation；
旧记录可继续存在用于审计/诊断，直到物理 retention 到期。

---

## 4. DURABLE 配置

DURABLE 没有有限幂等语义 TTL。

默认：

```text
recovery-mode = EXTERNAL_TASK
recover-processing-timeout = true
recover-failed = true
```

注意：

```text
recover-failed=true
```

仍然只会接管：

```text
failure_retryable = true
```

的 FAILED。

失败是否 retryable 由：

```java
IdempotencyFailureClassifier
```

决定。

---

## 5. Lock 配置

Lock 是可选并发优化。

默认推荐：

```text
wait-time = 0
lease-time = 数秒
fallback-to-state-on-failure = true
```

它只保护：

```text
Repository.tryAcquire / tryRecover
```

不会把业务 callback 整段包进分布式锁。

命名 Policy 可以覆盖全局 lock 配置。

---

## 6. Transaction 配置

```yaml
transaction:
  enabled: true
  require-template: true
```

当：

```text
TransactionExecutor bean exists
+
JDBC Repository 使用 transaction-aware JdbcExecutionManager
```

时启用：

```text
Tx-A REQUIRES_NEW
Tx-B REQUIRED
Tx-C REQUIRES_NEW
```

`require-template=true` 适合支付/结算等明确要求“Business + SUCCESS 同本地事务”的应用：
TransactionExecutor 缺失时启动直接失败，而不是悄悄降级。

---

## 7. ResultPolicy 不放在 YAML

结果策略带 Java 泛型与业务查询逻辑：

```text
ResultPolicy<OrderResult>
ResultPolicy<List<OrderResult>>
ResultPolicy<PaymentResponse>
```

所以它在调用处显式选择。

默认：

```java
execute(request, callback)
```

等价于：

```text
ResultPolicy.NONE
```

快照：

```java
execute(
    request,
    snapshotPolicyFactory.snapshot(
        new IdempotencyTypeRef<OrderResult>() {}),
    callback
);
```

引用：

```java
execute(
    request,
    IdempotencyResultPolicies.reference(...),
    callback
);
```

---

## 8. V1.2 兼容项

代码中暂时保留：

```text
IdempotencyMode.SHORT_TERM
IdempotencyOptions
default-short-term-repository Java accessors
shortTerm Java properties getter
storeResult legacy setter/getter
IdempotencyResultCodec
```

它们均不是 V1.3 新代码推荐入口。

其中全局 `storeResult` 已不再驱动运行时结果保存；
结果保存必须由 `ResultPolicy<T>` 明确决定。

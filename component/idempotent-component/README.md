# idempotent-component V2.1 — V1.3 API Enhancements + Package Refactor

> 定位：统一提供 **有限窗口幂等（WINDOWED）**、**长期业务幂等（DURABLE）**、状态机判定、Repository 原子抢占、显式恢复、结果回放，以及可选的分布式锁 / 本地事务集成。

V2.1 基于当前真实 V2 工程，完整落地此前讨论的 V1.3 API / StateMachine / ResultPolicy 增强，并在功能语义稳定后完成分包。重点不是继续增加“更多 if / boolean”，而是把 V1.2 已经跑通的正确性能力重新整理成稳定边界：

```text
Request：这一次请求是谁
Policy：这一类请求平时怎么执行
ResultPolicy<T>：第一次成功后，重复请求要拿到什么
RecoveryPolicy：异常 generation 允许如何被外部可靠任务接管
Repository CAS/Lua：谁拥有当前 generation 的最终正确性
DistributedLock：可选的热点并发收敛
Transaction Integration：Business + SUCCESS 的本地事务原子性
```

---

## 0. 本版本基线与原则

本版本以用户当前 `idempotent-component.zip` 为基线，保留既有：

- Redis WINDOWED / JDBC DURABLE；
- ownerToken + version generation；
- requestHash / routeKey；
- optional DistributedLock 短临界区；
- Tx-A / Tx-B / Tx-C transaction-component 集成；
- Reliable Task 显式 recover；
- 事件、指标、Starter 与 Demo。

在此基础上一次性完成：

1. 主 API 移除 `Class<T>`；
2. Request / Policy 解耦；
3. `SHORT_TERM` 正式语义化为 `WINDOWED`，旧值仅兼容；
4. 引入 `ResultPolicy<T>`：NONE / SNAPSHOT / REFERENCE；
5. 引入纯 `StateMachine` 决策层；
6. 引入 `RepositoryCapabilities`；
7. 引入命名 `IdempotencyPolicyRegistry`；
8. Recovery boolean 收敛为 `IdempotencyRecoveryPolicy`；
9. API / Core / Provider / Integration / Starter 按职责重新分包。

正确性核心仍未改变：**Repository 原子状态转换 + ownerToken/version CAS + 必要的本地事务边界**。

---

## 1. V2.1 最重要的 API 变化

### 1.1 主执行 API 不再要求 `Class<T>`

推荐：

```java
IdempotencyResult<OrderResult> result =
        idempotencyExecutor.execute(
                IdempotencyRequest.of("order:req-1001", "order-create"),
                context -> orderService.create(command)
        );
```

如果需要把第一次成功结果保存下来，并在重复请求时返回同一个结果：

```java
IdempotencyResultPolicy<OrderResult> replayPolicy =
        snapshotPolicyFactory.snapshot(
                new IdempotencyTypeRef<OrderResult>() {});

IdempotencyResult<OrderResult> result =
        idempotencyExecutor.execute(
                IdempotencyRequest.of("order:req-1001", "order-create"),
                replayPolicy,
                context -> orderService.create(command)
        );
```

复杂泛型同样可以：

```java
IdempotencyResultPolicy<List<OrderResult>> replayPolicy =
        snapshotPolicyFactory.snapshot(
                new IdempotencyTypeRef<List<OrderResult>>() {});
```

不再出现 `List.class` 丢失泛型信息的问题。

### 1.2 `storeResult=true/false` 不再是核心语义

结果回放改为显式 `ResultPolicy<T>`：

```text
NONE       只复用“已经成功”这个事实，不保存业务返回值
SNAPSHOT   保存第一次成功返回值快照，重复请求反序列化后返回
REFERENCE  只保存稳定业务引用，重复请求根据引用重新查询/组装结果
```

### 1.3 `SHORT_TERM` 正式命名为 `WINDOWED`

`SHORT_TERM` 仍作为 deprecated 兼容枚举存在，但 Core / Provider 内部统一归一成：

```text
WINDOWED
DURABLE
```

“窗口幂等”更准确，因为它描述的是**幂等事实的有效时间有限**，不是业务代码执行得快。

### 1.4 请求与策略拆开

`IdempotencyRequest` 只描述本次请求：

```text
key
requestHash
routeKey
policyName / inline policy
```

稳定执行策略由：

```java
IdempotencyPolicy
```

描述：

```text
mode
namespace
repository
processingTimeout
idempotencyWindow
recordRetentionTtl
RecoveryPolicy
LockOptions
```

Starter 支持命名策略，避免业务代码每次重复 builder 一大串配置。

---

## 2. 正确性核心

必须明确：

```text
DistributedLock != 幂等正确性核心
TransactionTemplate != 幂等唯一性核心
ResultPolicy != 幂等正确性核心
```

真正决定“当前谁有资格执行 / 完成”的是：

```text
Idempotency Key
        +
Repository 原子状态转换
        +
UNIQUE / Lua / SELECT FOR UPDATE / CAS
        +
ownerToken + version
        +
PROCESSING / SUCCESS / FAILED 状态机
```

例如旧执行者 A：

```text
owner=A, version=10
```

超时后 B 接管：

```text
owner=B, version=11
```

A 恢复以后无论写 SUCCESS 还是 FAILED，都必须带：

```sql
WHERE owner_token = 'A'
  AND version = 10
  AND status = 'PROCESSING'
```

更新 0 行，A 被淘汰。

---

## 3. V1.3 主执行流程

```text
Request
  │
  ▼
Policy Resolution
  │
  ▼
Repository Resolution by capabilities
  │
  ▼
[optional short DistributedLock]
  │
  ▼
Repository.tryAcquire(...)
  │
  │  Redis Lua / JDBC short Tx-A
  │  原子判断 + 必要状态转换
  ▼
State Machine
  ├─ SUCCESS              → REPLAY
  ├─ PROCESSING_ACTIVE    → RETURN PROCESSING
  ├─ PROCESSING_EXPIRED   → RETURN PROCESSING_EXPIRED
  ├─ FAILED_*             → RETURN previous failure
  ├─ KEY_CONFLICT         → RETURN conflict
  └─ ACQUIRED             → EXECUTE
                               │
                               ▼
                     [Transaction Integration]
                               │
                       ┌───────┴────────┐
                       │                │
                  supported        unsupported
                       │                │
                       ▼                ▼
                 Tx-B REQUIRED      callback
                 Business           markSuccess CAS
                   +
                 ResultPolicy.capture
                   +
                 markSuccess CAS
                       │
                    success
                       ▼
                    EXECUTED
```

**注意：Lock 和 Transaction 不是 CAS 后面的两个并列替代方案。**

- Lock 位于 `tryAcquire / tryRecover` 外围，用来减少热点竞争；
- Repository CAS/Lua 是状态正确性核心；
- State Machine 解释 Repository 的原子结果；
- Transaction Integration 只在已经获得 execution generation 后，保护 `Business + final SUCCESS`。

---

## 4. Recovery 不是普通 execute() 自动重试

普通 `execute()` 发现：

```text
PROCESSING_EXPIRED
FAILED_RETRYABLE
```

默认只返回状态，不自动偷走执行权。

长期可靠恢复由外部 Reliable Task：

```text
scan candidates
    ↓
candidate(namespace,key,owner,version)
    ↓
IdempotencyExecutor.recover(...)
    ↓
Repository.tryRecover(...)
    ↓
CAS expectedOwner + expectedVersion
    ↓
newOwner + version+1
```

这样扫描结果即使过期，也不会误接管。

推荐扫描入口：

```java
recoveryQueryService.findCandidates(
        "order-create",
        query
);
```

扫描器按 `policyName` 使用与正常执行一致的 Repository / 生命周期配置。

---

## 5. WINDOWED 与 DURABLE

### WINDOWED

适合：

```text
按钮连点
短时间 API 重复提交
短周期任务去重
```

关键时间：

```text
processingTimeout
    = 当前 generation 的执行权租约

idempotencyWindow
    = 相同 key 被认为“仍是同一次逻辑请求”的语义窗口

recordRetentionTtl
    = 语义窗口结束后，旧物理记录额外保留多久
```

默认 Redis + Lua。

窗口结束后，即使 retention 让旧 Hash / 行还存在，也可以开启新的 generation。

### DURABLE

适合：

```text
订单
支付
退款
结算
长期消息消费幂等
```

默认 JDBC：

```text
UNIQUE(namespace, idempotency_key)
SELECT ... FOR UPDATE
owner/version CAS
```

不依靠短 TTL 自动忘记业务事实。

---

## 6. ResultPolicy / 结果回放

“结果回放”不是再次执行业务。

第一次：

```text
key=req-1001
  ↓
Business 真执行
  ↓
SUCCESS
  ↓
按 ResultPolicy 保存可回放信息
```

第二次相同 key：

```text
Repository 返回 SUCCESS
  ↓
callback 不再执行
  ↓
ResultPolicy.replay(...)
  ↓
返回历史成功语义/结果
```

### NONE

```text
SUCCESS 已存在
→ REPLAYED
→ value = null
```

适合消息消费、只关心“做没做过”的场景。

### SNAPSHOT

保存第一次返回值的序列化快照。

适合短窗口 HTTP 请求：

```text
第一次返回什么
重复请求尽量返回同一个响应
```

### REFERENCE

第一次只保存稳定业务引用：

```text
orderId
paymentId
settlementId
```

重复请求使用引用重新查询业务数据。

对 DURABLE 长期幂等通常比永久保存 DTO JSON 更健康。

详细见：

```text
docs/result-replay-and-result-policy.md
```

---

## 7. Transaction Integration：V1.2 正确设计继续保留

JDBC DURABLE + transaction-component：

```text
Tx-A REQUIRES_NEW
tryAcquire / tryRecover
PROCESSING
COMMIT
      │
      ▼
Tx-B REQUIRED
Business
  +
ResultPolicy.capture
  +
markSuccess(owner/version CAS)
COMMIT / ROLLBACK
      │ failure
      ▼
Tx-C REQUIRES_NEW
markFailed
COMMIT
```

### Tx-A 为什么短事务独立提交

必须尽快让其他请求看到：

```text
PROCESSING
owner
version
processingExpireAt
```

不能把 PROCESSING 和几十秒业务放进同一个长事务最后才提交。

### Tx-B 为什么 REQUIRED

如果调用方已有外层事务，Tx-B 应参加它：

```text
Outer Tx
  updateA
  idempotency callback + SUCCESS
  updateC
Outer COMMIT / ROLLBACK
```

不应强制 REQUIRES_NEW 提前提交幂等 SUCCESS。

因此：

```text
transactionApplied=true
```

只代表 Business + SUCCESS 处在同一个事务边界，**不承诺 execute() 返回前一定发生物理 COMMIT**。

### Tx-C 为什么 REQUIRES_NEW

Tx-B 已经失败并回滚以后，FAILED 必须在一个新的短事务里记录下来，否则 FAILED 自己也会跟着回滚。

### COMMIT_UNKNOWN

如果数据库是否提交无法确认：

```text
不写 FAILED
保留/等待 PROCESSING 状态收敛
后续查询、对账或 Recovery 处理
```

不能把“提交结果未知”伪装成明确失败。

---

## 8. Repository Capabilities

V1.3 不再把：

```text
WINDOWED = Redis
DURABLE = JDBC
```

写死成 Core 语义。

Provider 显式声明能力：

```java
IdempotencyRepositoryCapabilities
```

例如：

```text
Redis
  windowed = true
  durable = false
  resultPayload = true
  businessTransactionParticipation = false
  recoveryQuery = false

JDBC
  windowed = true
  durable = true
  resultPayload = true
  businessTransactionParticipation = depends on JdbcExecutionManager
  recoveryQuery = true
```

Starter 仍提供合理默认映射：

```text
WINDOWED -> redis
DURABLE  -> jdbc
```

但这是默认配置，不是 Core 的硬编码。

---

## 9. 推荐命名 Policy

```yaml
xjtu:
  iron:
    idempotent:
      default-policy: durable-default

      policies:
        api-submit:
          mode: WINDOWED
          repository-name: redis
          processing-timeout: 10s
          idempotency-window: 5m

        order-create:
          mode: DURABLE
          repository-name: jdbc
          processing-timeout: 30s
          recovery-mode: EXTERNAL_TASK
          recover-processing-timeout: true
          recover-failed: true
          lock-enabled: true
```

业务代码只负责：

```java
IdempotencyRequest.builder()
        .key(requestId)
        .requestHash(hash)
        .routeKey(merchantId)
        .policyName("order-create")
        .build();
```

---

## 10. 模块

```text
idempotent-component
├── idempotent-api
│   ├── request / policy / result
│   └── repository SPI
├── idempotent-core
│   ├── DefaultIdempotencyExecutor
│   ├── PolicyRegistry
│   ├── ExecutionDefinition
│   ├── StateMachine
│   └── Transaction Coordinator SPI
├── idempotent-provider
│   ├── idempotent-provider-redis
│   └── idempotent-provider-jdbc
├── idempotent-integration-transaction
├── idempotent-starter
├── idempotent-demo
└── docs
```

---

## 11. 推荐阅读

1. `docs/v1.3-architecture.md`
2. `docs/result-replay-and-result-policy.md`
3. `docs/recovery-and-routing.md`
4. `docs/transaction-boundary.md`
5. `docs/window-and-retention.md`
6. `docs/configuration.md`
7. `docs/CHANGELOG-v1.3.md`

---

## 12. 仍然不承诺什么

本地幂等 + 本地事务不能自动回滚：

```text
HTTP/RPC 已经成功的外部调用
银行扣款
已发送短信
已发布到不可回滚外部系统的事件
跨数据库资源
```

这些场景仍需要：

```text
下游幂等键
状态查询
Outbox / 事务消息
Saga / TCC
补偿 / 对账
```

幂等组件的职责是让**重复执行和执行权切换可控**，而不是把所有跨系统副作用变成本地 ACID。

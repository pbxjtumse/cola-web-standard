# Recovery 与 Reliable Task

## 1. Recovery 的边界

Recovery 不是普通请求的自动重试。

普通 `execute()` 遇到：

```text
PROCESSING_EXPIRED
FAILED_RETRYABLE
```

只返回状态，不自动接管。

真正接管必须由外部 Reliable Task 显式调用 `recover()`。

## 2. 两阶段恢复

```text
阶段一：扫描候选
阶段二：Repository 二次 CAS 接管
```

扫描只产生候选，不授予执行权。

## 3. 扫描

推荐按 `policyName` 找候选：

```java
recoveryQueryService.findCandidates(
        "order-create",
        query
);
```

候选至少携带：

```text
namespace
key
requestHash
routeKey
ownerToken
version
```

## 4. 真正 recover()

任务执行时必须携带扫描时看到的 generation：

```text
expectedOwner=A
expectedVersion=10
```

Repository 在原子边界再次检查：

```text
current owner/version 仍匹配
RecoveryPolicy 允许
requestHash / routeKey 不冲突
PROCESSING 已超时
或 FAILED retryable
```

全部满足才：

```text
owner=B
version=11
status=PROCESSING
new processingExpireAt
```

返回 `RECOVERY_ACQUIRED`。

## 5. 为什么必须二次 CAS

扫描与执行之间存在时间差：

```text
T1 scanner 看到 A/10 过期
T2 C 已接管成 C/11
T3 旧任务才执行
```

此时：

```text
expected=A/10
current=C/11
    ↓
STALE_CANDIDATE
```

旧恢复任务直接结束，不执行业务。

## 6. Recovery 状态转换

PROCESSING timeout：

```text
PROCESSING(A,10)
    ↓
recover CAS
    ↓
PROCESSING(B,11)
```

retryable FAILED：

```text
FAILED(A,10,retryable=true)
    ↓
recover CAS
    ↓
PROCESSING(B,11)
```

持久状态仍然只有 PROCESSING / SUCCESS / FAILED。

## 7. routeKey

routeKey 用于数据库/任务/事务路由。Recovery 必须沿用原 routeKey。

相同 key 后续突然使用不同 routeKey 应判为 `KEY_CONFLICT`，不能悄悄跨分片执行。

## 8. namespace

当前恢复查询以 Repository + namespace + 状态/时间等条件为主要隔离边界。

同一 Repository + namespace 内应保持兼容的生命周期和 Recovery 语义。推荐 namespace 直接使用稳定业务动作域：

```text
order-create
payment-submit
settlement-batch
```

## 9. Recovery 与 Lock / Transaction

恢复也只使用短锁：

```text
lock
  tryRecover CAS
unlock
```

真正正确性来自 expectedOwner + expectedVersion。

接管成功后复用正常业务链：

```text
RECOVERY_ACQUIRED
    ↓
StateMachine -> EXECUTE
    ↓
Tx-B Business + ResultPolicy.capture + markSuccess
    ↓ failure
Tx-C markFailed
```

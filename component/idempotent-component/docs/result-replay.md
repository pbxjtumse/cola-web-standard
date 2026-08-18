# 结果回放与 ResultPolicy

## 1. Replay 是什么

第一次请求：

```text
tryAcquire -> ACQUIRED
Business -> OrderResult(orderId=888)
markSuccess -> SUCCESS
```

重复请求：

```text
tryAcquire -> SUCCESS
StateMachine -> REPLAY
callback 不执行
ResultPolicy.replay(...)
```

Replay 的定义：

> 不重新执行业务，而是利用第一次 SUCCESS 保存的信息完成重复请求返回。

```text
Replay != Retry
```

## 2. ResultPolicy 的职责

`IdempotencyResultPolicy<T>` 只回答：

```text
第一次成功：T 应保存什么？
重复 SUCCESS：保存的信息如何恢复成 T？
```

它不参与抢锁、CAS、Recovery 或事务传播。

## 3. NONE

默认策略。

第一次只保存 SUCCESS；重复请求返回：

```text
REPLAYED
value = null
```

适合 MQ 消费、任务防重等只关心“是否做过”的场景。

## 4. SNAPSHOT

第一次把返回值序列化保存：

```text
OrderResult -> JSON -> result_payload
```

重复请求：

```text
result_payload -> deserialize -> OrderResult
```

示例：

```java
IdempotencyResultPolicy<List<OrderResult>> snapshot =
        snapshotPolicyFactory.snapshot(
                new IdempotencyTypeRef<List<OrderResult>>() {});
```

通过 `IdempotencyTypeRef<T>` 支持复杂泛型，主 Executor API 不需要 `Class<T>`。

推荐：短窗口 HTTP API、按钮重复提交、明确要求返回第一次响应快照的场景。

## 5. REFERENCE

REFERENCE 只保存稳定业务引用，例如：

```text
OrderResult -> orderId=888
```

重复请求：

```text
888 -> orderQueryService.find(888) -> OrderResult
```

DURABLE 场景通常比长期保存 DTO JSON 更适合 REFERENCE，因为业务 ID 往往比历史 Java DTO 结构稳定。

`capture()` 应尽量是纯函数，只从业务返回值提取引用，不要在其中额外发 MQ、调 HTTP 或写其他数据库。

## 6. 策略选择

| 场景 | 推荐 |
|---|---|
| MQ 消费防重 | NONE |
| 定时任务防重 | NONE |
| 短窗口 HTTP 重复提交 | SNAPSHOT |
| 订单创建长期幂等 | REFERENCE |
| 支付/退款长期幂等 | REFERENCE / NONE |
| 必须重复返回第一次完全相同响应 | SNAPSHOT |

## 7. StoredResultEnvelope

内部 payload 带策略 envelope，用于区分：

```text
SNAPSHOT
REFERENCE
```

避免 SNAPSHOT 保存的数据被 REFERENCE 调用错误解释。

策略不一致返回 `RESULT_POLICY_MISMATCH`；需要 payload 但记录没有保存时返回 `RESULT_REPLAY_UNAVAILABLE`。
当前只接受带 envelope 的 payload；格式非法返回 `RESULT_POLICY_ERROR`。

## 8. capture 失败

transaction-aware JDBC 模式下：

```text
Tx-B
  Business
  ResultPolicy.capture
  markSuccess
```

capture 失败会让 Tx-B rollback，然后 Tx-C 记录 FAILED。

非事务场景 callback 可能已经产生外部副作用，因此 capture 失败默认不能被当成安全自动重试条件。

# 结果回放与 ResultPolicy

## 1. 什么叫“结果回放”

假设第一次请求：

```text
idempotencyKey = create-order:req-1001
```

第一次真实执行：

```text
A
 ↓
tryAcquire -> ACQUIRED
 ↓
createOrder()
 ↓
OrderResult(orderId=888)
 ↓
markSuccess
```

一分钟后因为调用方没收到响应，又来了同一个 key：

```text
B
 ↓
tryAcquire -> SUCCESS
```

幂等的首要要求是：

```text
B 不能再次 createOrder()
```

但 B 仍然需要一个返回值。

“结果回放”就是：

> **不重新执行业务，而是根据第一次 SUCCESS 留下的信息完成这次重复请求的返回。**

所以：

```text
Replay != Retry
Replay != Business re-execution
```

---

## 2. 为什么旧 API 要传 Class<T>

旧 V1.2：

```java
execute(
    request,
    OrderResult.class,
    callback
)
```

不是事务模板要求 `Class<T>`。

原因仅仅是：

```text
第一次 OrderResult
  ↓
JSON String
  ↓
result_payload

第二次 result_payload
  ↓
JSON deserialize
  ↓
需要知道 T 是 OrderResult
```

Java 泛型擦除以后，Executor 运行时不知道 `T`。

`Class<T>` 是最直接的运行时类型提示，但它有两个缺陷：

1. 普通不保存结果的调用也被迫看到类型参数；
2. `List<OrderResult>` 只能传 `List.class`，内部泛型丢失。

因此 V1.3 把它移出主 Executor API。

---

## 3. ResultPolicy 是什么

`IdempotencyResultPolicy<T>` 回答两个问题：

```text
第一次成功时：
T 应该保存什么？

重复 SUCCESS 时：
保存的信息应该如何恢复成 T？
```

接口：

```java
public interface IdempotencyResultPolicy<T> {

    IdempotencyResultPolicyType type();

    String capture(T value) throws Exception;

    T replay(String storedValue) throws Exception;
}
```

它不是 Redis/JDBC Provider，也不是事务策略。

---

## 4. NONE

```java
idempotencyExecutor.execute(
        request,
        context -> consumeMessage(message)
);
```

默认就是：

```text
ResultPolicy = NONE
```

第一次：

```text
Business success
markSuccess(result_payload = null)
```

第二次：

```text
SUCCESS
 ↓
REPLAYED
 ↓
value = null
```

适合：

```text
消息消费
任务防重
调用方只关心“这件事是否已经做过”
```

这是最轻量的结果策略。

---

## 5. SNAPSHOT

第一次把返回结果本身序列化保存：

```text
OrderResult
 ↓
JSON
 ↓
result_payload
```

重复请求：

```text
result_payload
 ↓
deserialize
 ↓
OrderResult
```

使用：

```java
IdempotencyResultPolicy<OrderResult> snapshot =
        snapshotPolicyFactory.snapshot(
                new IdempotencyTypeRef<OrderResult>() {});

IdempotencyResult<OrderResult> result =
        idempotencyExecutor.execute(
                request,
                snapshot,
                context -> createOrder(command)
        );
```

复杂泛型：

```java
IdempotencyResultPolicy<List<OrderResult>> snapshot =
        snapshotPolicyFactory.snapshot(
                new IdempotencyTypeRef<List<OrderResult>>() {});
```

API 本身不依赖 Jackson 的 `TypeReference` / `JavaType`。

### SNAPSHOT 最适合什么

短窗口 HTTP API：

```text
第一次返回：
{
  "orderId": "888",
  "message": "created"
}

重复请求：
尽量返回第一次相同响应
```

### SNAPSHOT 的长期问题

如果一条 DURABLE 记录保存很多年：

```text
CreateOrderResponse V1 JSON
```

几年以后 DTO 已经升级成 V4，历史快照可能：

```text
字段已删除
枚举已变化
结构已升级
```

因此长期业务幂等不应无脑永久保存 DTO 快照。

---

## 6. REFERENCE

REFERENCE 保存的不是整个响应，而是一个稳定业务引用。

例如第一次创建订单：

```text
OrderResult(orderId=888, ...)
```

capture：

```text
"888"
```

数据库：

```text
result_payload = reference:888
```

重复请求：

```text
reference=888
 ↓
query order 888
 ↓
assemble OrderResult
```

示例：

```java
IdempotencyResultPolicy<OrderResult> reference =
        IdempotencyResultPolicies.reference(
                new IdempotencyResultReference<>() {
                    @Override
                    public String capture(OrderResult value) {
                        return value.orderId();
                    }

                    @Override
                    public OrderResult resolve(String orderId) {
                        return orderQueryService.find(orderId);
                    }
                });

return idempotencyExecutor.execute(
        request,
        reference,
        context -> orderService.create(command)
);
```

### 为什么 DURABLE 更推荐 REFERENCE

业务 ID：

```text
orderId=888
```

通常比：

```text
CreateOrderResponse Java DTO V1 JSON
```

稳定得多。

它还允许重复请求读取当前业务事实，而不是永远回放几年前的表示层结构。


### REFERENCE 的 capture 应该尽量是纯函数

推荐：

```text
OrderResult -> orderId
```

不推荐在 `capture()` 里再做：

```text
发 MQ
调 HTTP
写另一个数据库
```

因为 transaction-aware 场景会把 `capture()` 放进 Tx-B 完成协议；它应该只是从已生成的业务结果中提取稳定引用。

---

## 7. NONE / SNAPSHOT / REFERENCE 怎么选

| 场景 | 推荐 |
|---|---|
| MQ 消费防重 | NONE |
| 定时任务防重 | NONE |
| API 5 分钟重复提交 | SNAPSHOT |
| 按钮连点 | SNAPSHOT / NONE |
| 订单创建长期幂等 | REFERENCE |
| 支付长期幂等 | REFERENCE / NONE |
| 退款长期幂等 | REFERENCE |
| 真正要求“重复请求永远返回第一次完全相同响应” | SNAPSHOT |

---

## 8. ResultPolicy 不进入 IdempotencyPolicy 的原因

`IdempotencyPolicy` 是稳定、可配置、无泛型的运行策略：

```text
mode
repository
timeout
window
recovery
lock
```

而：

```text
ResultPolicy<OrderResult>
ResultPolicy<List<OrderResult>>
ResultPolicy<PaymentResponse>
```

带着具体 Java 返回类型与业务查询器。

把它们硬塞到 application.yml 或普通 Policy 里，会重新引入：

```text
Class<T>
类名
反射
泛型擦除
```

因此 V1.3 保持：

```text
Policy = 稳定基础设施策略
ResultPolicy<T> = 类型安全调用级结果策略
```

两者职责更干净。

---

## 9. result_payload 为什么有 envelope

V1.3 内部用 envelope 区分：

```text
SNAPSHOT
REFERENCE
```

避免第一次按 SNAPSHOT 保存，第二次却误用 REFERENCE 解读同一串字符串。

概念格式：

```text
IR1|SNAPSHOT|...
IR1|REFERENCE|...
```

如果发现类型不匹配，返回：

```text
RESULT_POLICY_MISMATCH
```

如果新调用明确要求 SNAPSHOT/REFERENCE，但历史记录没有 payload：

```text
RESULT_REPLAY_UNAVAILABLE
```

不会默默重新执行业务。

---

## 10. 一个重要安全点：capture 失败

如果 Transaction Integration 可用：

```text
Tx-B
 Business
 ResultPolicy.capture
 markSuccess
```

capture 失败：

```text
Tx-B rollback
Tx-C -> FAILED(non-retryable result-policy error)
```

本地业务 SQL 不会单独提交。

如果没有事务能力，callback 可能已经产生业务副作用，随后 capture 才失败。

这种情况下组件不会把它标成“可自动重试”，因为再次执行可能重复产生副作用。

所以 ResultPolicy 不是一个随便可以失败的旁路日志功能，它属于 SUCCESS 完成协议的一部分。

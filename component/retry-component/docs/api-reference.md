# API 参考

## 一、RetryExecutor

### 推荐入口

```java
<T> RetryResult<T> execute(RetryExecution<T> execution)
```

适合需要：

- 自定义 `retryId`；
- 协作式取消；
- 上下文属性；
- 显式不可变执行请求。

### 便捷入口

- `execute(operationName, attributes, operation, RetryPolicy)`：显式策略。
- `execute(operationName, attributes, operation, policyName)`：命名策略。
- `execute(operationName, operation, RetryPolicy)`：无属性。
- `execute(operationName, Callable, RetryPolicy)`：适配 Callable。
- `run(operationName, Runnable, RetryPolicy)`：无返回值操作。
- `executeAndGet(...)`：最终失败时抛 `RetryExecutionException`。

## 二、RetryExecution<T>

必填：

- `operationName`
- `RetryOperation<T>`
- `RetryPolicy`

可选：

- `attributes`
- `RetryCancellationToken`
- `retryId`

属性会防御性复制并返回不可变映射。

## 三、RetryPolicy

关键 Builder 方法：

```text
maxAttempts
maxDuration
operationSafety
safetyMode
traverseCauses
maxCauseDepth
retryOn
stopOn
abortOn
retryIfResult
classifier
backoffStrategy
```

限制：

- 自定义 `classifier` 不能与声明式规则混用。
- 同一异常类型不能同时配置为多个动作。
- `retryOn` 至少需要一个异常类型。
- `NON_IDEMPOTENT + REJECT + maxAttempts > 1` 构建失败。

## 四、RetryDecision

- `success(reason)`：最终成功。
- `retry(reason, code, category)`：使用策略退避。
- `retryAfter(delay, source, reason, code, category)`：覆盖策略退避。
- `stop(reason, code, category)`：最终 `NOT_RETRYABLE`。
- `abort(reason, code)`：最终 `ABORTED`。

不变量：

- 只有 RETRY 可设置延迟。
- RETRY 不能使用 `NON_RETRYABLE` 分类。
- 延迟来源必须和延迟覆盖一起出现。

## 五、RetryResult<T>

常用字段：

```text
retryId
operationName
policyName
status
value
failure
attempts
elapsedTime
lastAttempt
lastDecision
```

常用方法：

- `isSuccess()`
- `isExhausted()`
- `getOrThrow()`

状态：

- `SUCCESS`
- `EXHAUSTED`
- `NOT_RETRYABLE`
- `TIMED_OUT`
- `CANCELLED`
- `INTERRUPTED`
- `ABORTED`
- `EXECUTION_FAILED`

## 六、BackoffStrategies

- `none()`
- `fixed(delay)`
- `exponential(initial, max, multiplier)`
- `exponentialWithFullJitter(initial, max, multiplier)`
- `exponentialWithFullJitter(initial, max, multiplier, randomGenerator)`
- `categoryAware(map, defaultStrategy)`

退避策略只计算 `RetryDelay`，实际等待由 `RetrySleeper` 完成。

## 七、RetryCancellationToken

```java
RetryCancellationToken.none()
RetryCancellationToken.from(AtomicBoolean)
```

也可以直接使用 lambda：

```java
() -> requestContext.isCancelled()
```

取消令牌应快速、无阻塞、无副作用。令牌自己抛异常会被视为执行基础设施失败。

## 八、RetryPolicyRegistry

- `register(policy)`：只允许新增，同名存在时失败。
- `replace(policy)`：显式新增或替换。
- `find(name)`：可选查询。
- `getRequired(name)`：不存在时失败。
- `policyNames()`：排序后的不可变名称集合。
- `snapshot()`：排序后的不可变映射快照。

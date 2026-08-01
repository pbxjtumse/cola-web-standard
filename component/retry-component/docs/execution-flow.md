# 同步执行流程

## 一、完整流程

```text
1. 校验 RetryExecution
2. 读取不可变 RetryPolicy
3. 使用调用方 retryId 或 RetryIdGenerator 生成标识
4. 记录逻辑开始墙上时间与单调时间
5. 发布 EXECUTION_STARTED
6. 必要时发布 SAFETY_WARNING
7. 检查取消令牌
8. 检查线程中断
9. 检查 maxDuration
10. 构建 RetryContext
11. 发布 ATTEMPT_STARTED
12. 执行 RetryOperation
13. 构建 RetryAttempt
14. 发布 ATTEMPT_COMPLETED
15. 特殊处理 InterruptedException
16. 调用 RetryClassifier
17. 校验 RetryDecision 与 RetryAttempt 是否一致
18. 发布 DECISION_MADE
19. 处理终态决策：
      SUCCESS -> SUCCESS
      STOP    -> NOT_RETRYABLE
      ABORT   -> ABORTED
      RETRY   -> 继续
20. 再次检查取消令牌
21. 检查 maxAttempts
22. 检查 maxDuration
23. 读取 decision.delayOverride；没有时调用 BackoffStrategy
24. 校验 RetryDelay
25. 判断等待后是否会耗尽总时长
26. 发布 RETRY_SCHEDULED
27. 等待前再次检查取消
28. RetrySleeper 同步等待
29. 等待中断时恢复线程中断标记
30. 等待后再次检查取消
31. 更新 previousAttempt
32. 进入下一次尝试
```

## 二、为什么多次检查取消

取消可能发生在：

- 调用执行器之前；
- 第一次业务操作执行过程中；
- 分类和退避计算期间；
- 同步等待期间；
- 等待结束、下一次操作开始之前。

因此只在入口检查一次是不够的。当前实现会在不增加额外线程的前提下，尽早阻止尚未开始的工作。

## 三、总时长语义

`maxDuration` 包含：

- 业务操作耗时；
- 分类器耗时；
- 退避策略计算耗时；
- 同步等待耗时；
- 监听器同步执行耗时；
- 执行器内部处理耗时。

它不承诺强制停止已经开始的同步业务代码。例如业务方法阻塞 30 秒，而 `maxDuration` 为 5 秒，执行器只能在业务方法返回后判定不再继续下一次尝试。

## 四、重试等待边界

当下一次等待时间大于或等于剩余总时长时，直接返回 `TIMED_OUT`。这样不会为了一个已经无法开始的下一次尝试继续阻塞当前线程。

零等待是合法的，并且可以保留真实来源，例如服务端明确要求立即重试。

## 五、扩展点异常处理

- `RetryClassifier` 抛异常或违反契约：`EXECUTION_FAILED`。
- `BackoffStrategy` 抛异常或返回非法结果：`EXECUTION_FAILED`。
- `RetryListener` 抛异常：记录后隔离，不改变主流程。
- `RetrySleeper` 被中断：恢复中断标记，返回 `INTERRUPTED`。

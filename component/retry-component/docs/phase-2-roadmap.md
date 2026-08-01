# 二期演进路线

二期应建立在 1.3.0 的 `RetryExecution`、`RetryAttempt`、`RetryDecision` 和取消协议之上，不能另建一套不兼容模型。

## 一、异步执行器

建议新增独立接口：

```text
AsyncRetryExecutor
    -> CompletionStage<RetryResult<T>>
```

要求：

- 不通过工作线程 sleep 等待；
- 使用并行组件的受管 Scheduler；
- 未执行调度任务可取消；
- Scheduler 拒绝有稳定状态；
- Trace、MDC 和业务上下文传播；
- 监听器执行方式明确。

## 二、单次尝试超时

新增 `attemptTimeout` 前必须定义：

- 超时是逻辑超时还是物理停止；
- 前一次任务没有停止时能否开始下一次；
- 是否允许线程中断；
- 不响应中断的遗留任务如何治理；
- 超时与总预算的先后关系。

不能仅使用 `Future.get(timeout)` 后立刻开始下一次尝试。

## 三、RetryBudget

建议协议：

```text
tryAcquire before every additional attempt
onSuccess after logical success
onFailure after terminal failure
```

原则：

- 第一次正常调用不消耗预算；
- 额外尝试消耗预算；
- 成功逐步恢复；
- 按下游资源隔离；
- 预算不足返回 `BUDGET_REJECTED`；
- 与治理组件协同，不重复实现自适应限流。

## 四、协议分类器模块

建议增加独立集成模块：

```text
retry-integration-http
retry-integration-jdbc
retry-integration-redis
retry-integration-kafka
retry-integration-rocketmq
retry-integration-pulsar
```

每个模块只负责把协议错误映射成：

```text
RetryDecision
RetryFailureCategory
failureCode
delayOverride
```

协议知识不进入 `retry-core`。

## 五、动态配置

刷新流程：

```text
new config
    -> resolve and validate all policies
    -> build immutable policy snapshots
    -> atomically replace registry references
```

任意策略失败时不应部分更新。已经开始的执行继续使用旧快照。

## 六、组件集成顺序

建议顺序：

1. 消息发送短重试。
2. 事务模板完整事务重试。
3. 并行组件异步调度。
4. HTTP/JDBC 预设分类器。
5. RetryBudget。
6. 动态配置。
7. Reactor 适配。

## 七、三期边界

以下仍不进入二期核心：

- 数据库持久化任务；
- 分布式扫描和抢占；
- Lease、心跳和续租；
- 人工重放；
- 管理后台；
- 工作流补偿。

这些能力需要与任务组件、幂等组件和可观测组件共同设计。

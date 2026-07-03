# 并行组件一期完整测试用例清单 v1

> 适用范围：`concurrency-component` 一期能力。  
> 文档目标：在进入二期动态线程池治理、健康检查、指标增强之前，先把一期所有核心接口、核心状态、核心分支、异常边界、并发竞争场景列成可执行的测试用例清单。  
> 推荐落地方式：先按本文创建测试类骨架，再逐个补充 JUnit 5 测试实现。

---

## 0. 测试目标总览

并行组件一期不是普通工具类，它的风险点主要集中在：

```text
任务状态机
Future 是否正确收口
拒绝策略是否感知 TaskCommand
timeout 与 success 是否竞争安全
fallback 成功/失败/拒绝/关闭是否都能收口
cancel queued/running/fallback 是否都正确
shutdownNow pending task 是否不会导致 Future 永久 pending
Listener / Metrics / Registry / ErrorClassifier 是否异常隔离
```

因此测试目标不是单纯验证“方法能调用”，而是验证：

```text
1. 状态正确
2. 事件正确
3. Future 正确
4. 异常正确
5. 线程正确
6. 资源释放正确
7. 并发竞争正确
```

---

## 1. 测试分层

### 1.1 Core 单元测试

覆盖组件内核：

```text
TaskDefinition
TaskExecutionRuntime
TaskExecutionContext
TaskCommand
TaskResultPipeline
TaskControl
TaskCancellationManager
RejectedExecutionHandler
AsyncTemplate
TaskLifecyclePublisher
TaskExecutionRegistry
AsyncErrorClassifier
ThreadPoolManager
```

这是优先级最高的一层。

---

### 1.2 Spring Boot Starter 自动装配测试

覆盖：

```text
配置绑定
Bean 创建
默认配置
自定义扩展点
非法配置失败
自动装配条件
```

---

### 1.3 Demo / Controller 冒烟测试

覆盖：

```text
接口能访问
返回结构合理
Demo 场景可演示
```

Controller 不承担全分支覆盖。

---

### 1.4 并发竞争测试

覆盖：

```text
SUCCESS vs TIMEOUT
SUCCESS vs CANCELLED
FAILED vs CANCELLED
REJECTED vs CANCELLED
FALLBACK_SUCCESS vs CANCELLED
shutdownNow vs queued task
Registry 并发 update
```

---

## 2. 推荐测试类结构

```text
concurrency-core/src/test/java/com/xjtu/iron/concurrency/core/

├── runtime/
│   └── TaskExecutionRuntimeStateMachineTest.java
│
├── task/
│   ├── TaskDefinitionSnapshotTest.java
│   ├── TaskExecutionContextTest.java
│   ├── TaskCommandTest.java
│   ├── TaskControlTest.java
│   └── TaskCancellationManagerTest.java
│
├── pipeline/
│   └── DefaultTaskResultPipelineTest.java
│
├── execution/
│   ├── DefaultTaskExecutionTemplateTest.java
│   ├── ThreadPoolRegistryTest.java
│   ├── ThreadPoolManagerTest.java
│   └── ThreadPoolSpecValidationTest.java
│
├── execution/rejection/
│   ├── AwareAbortRejectedExecutionHandlerTest.java
│   ├── CallerRunsRejectedExecutionHandlerTest.java
│   ├── DiscardRejectedExecutionHandlerTest.java
│   ├── DiscardOldestRejectedExecutionHandlerTest.java
│   └── BlockingWaitRejectedExecutionHandlerTest.java
│
├── async/
│   └── DefaultAsyncTemplateTest.java
│
├── lifecycle/
│   ├── DefaultTaskLifecyclePublisherTest.java
│   └── CompositeTaskExecutionListenerTest.java
│
├── error/
│   ├── CompositeAsyncErrorClassifierTest.java
│   ├── DefaultAsyncErrorClassifierTest.java
│   └── CompletableFutureExceptionUtilsTest.java
│
├── registry/
│   └── DefaultTaskExecutionRegistryTest.java
│
└── testfixture/
    ├── RecordingTaskExecutionListener.java
    ├── RecordingTaskLifecyclePublisher.java
    ├── RecordingAsyncUncaughtExceptionHandler.java
    ├── TestAsyncErrorClassifier.java
    ├── TestTaskFactory.java
    ├── TestExecutors.java
    └── AwaitTestSupport.java
```

---

# 3. 测试用例总表

## 3.1 编号规则

```text
DEF   = TaskDefinition
CTX   = TaskExecutionContext
RT    = TaskExecutionRuntime
CMD   = TaskCommand
PIPE  = TaskResultPipeline
EXEC  = DefaultTaskExecutionTemplate / AsyncExecutor
REJ   = RejectedExecutionHandler
CAN   = TaskControl / Cancellation
TPL   = AsyncTemplate
LIFE  = LifecyclePublisher / Listener
ERR   = ErrorClassifier
REG   = Registry
POOL  = ThreadPool infrastructure
BOOT  = Spring Boot starter
DEMO  = Controller smoke
RACE  = 并发竞争专项
```

优先级：

```text
P0 = 必须优先覆盖，影响主链路正确性
P1 = 必须覆盖，影响生产稳定性
P2 = 建议覆盖，影响易用性和演示
```

---

# 4. TaskDefinitionSnapshotTest

## 4.1 测试目标

验证 `TaskDefinition` 是 `AsyncTask` 在提交时生成的不可变快照。  
提交之后，主链路应该只读取 `TaskDefinition`，不再读取可变 `AsyncTask`。

---

## 4.2 测试用例

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| DEF-001 | `fromShouldCopyMetadata` | P0 | 从 AsyncTask 创建 TaskDefinition | executorName、taskName、taskId、bizKey、description 正确复制 |
| DEF-002 | `fromShouldCopyTags` | P0 | AsyncTask 带 tags | Definition 中 tags 正确 |
| DEF-003 | `definitionShouldDefensivelyCopyTags` | P0 | from 后修改原 tags | Definition 不受影响 |
| DEF-004 | `fromShouldCopyTimeoutSettings` | P0 | 设置 timeout、queueTimeout | Definition 正确复制 |
| DEF-005 | `fromShouldCopyCancelSettings` | P0 | 设置 cancelOnTimeout、interruptOnTimeout | Definition 正确复制 |
| DEF-006 | `fromShouldCopyFallback` | P0 | 设置 fallback | Definition 中 fallback 可用 |
| DEF-007 | `fromShouldCopyContextPropagation` | P1 | 设置 contextPropagation=true | Definition 正确复制 |
| DEF-008 | `fromShouldCopyRetryPolicyMetadata` | P1 | 设置 retryPolicy | Definition 正确复制 |
| DEF-009 | `modifyingAsyncTaskAfterSnapshotShouldNotAffectTimeout` | P0 | from 后修改 AsyncTask.timeout | Definition timeout 不变 |
| DEF-010 | `modifyingAsyncTaskAfterSnapshotShouldNotAffectFallback` | P0 | from 后修改 fallback | Definition fallback 不变 |
| DEF-011 | `modifyingAsyncTaskAfterSnapshotShouldNotAffectTags` | P0 | from 后修改 tags | Definition tags 不变 |
| DEF-012 | `definitionShouldExposeMetadataConvenienceGetters` | P1 | 调用 getTaskId/getTaskName/getExecutorName | 返回值正确 |
| DEF-013 | `fromShouldRejectNullTask` | P0 | TaskDefinition.from(null) | 抛出 NullPointerException 或 IllegalArgumentException |
| DEF-014 | `definitionShouldKeepOperationFromSubmitMoment` | P0 | from 后替换 AsyncTask.operation | Definition operation 仍是快照时 operation |
| DEF-015 | `definitionShouldNotAllowExternalMetadataMutation` | P1 | 尝试修改 metadata 内部集合 | 不影响 Definition |

---

## 4.3 关键断言

```text
1. Definition 字段值稳定。
2. tags 防御性复制。
3. fallback 引用按提交时确定。
4. timeout / queueTimeout 不受提交后修改影响。
5. 主链路后续应只读取 TaskDefinition。
```

---

# 5. TaskExecutionContextTest

## 5.1 测试目标

验证 `TaskExecutionContext` 是一次任务执行的上下文容器，负责聚合：

```text
TaskDefinition
executable
baseFuture
TaskExecutionRuntime
```

---

## 5.2 测试用例

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| CTX-001 | `contextShouldHoldTaskDefinition` | P1 | 创建 Context | definition 正确保存 |
| CTX-002 | `contextShouldHoldExecutable` | P1 | 创建 Context | executable 正确保存 |
| CTX-003 | `contextShouldHoldBaseFuture` | P1 | 创建 Context | baseFuture 正确保存 |
| CTX-004 | `contextShouldHoldRuntime` | P1 | 创建 Context | runtime 正确保存 |
| CTX-005 | `metadataShouldDelegateToDefinition` | P1 | 调用 context.getMetadata | 返回 Definition metadata |
| CTX-006 | `taskIdShouldDelegateToMetadata` | P2 | 调用 context.getTaskId | 返回 taskId |
| CTX-007 | `contextShouldRejectNullDefinition` | P1 | definition=null | 抛异常 |
| CTX-008 | `contextShouldRejectNullExecutable` | P1 | executable=null | 抛异常 |
| CTX-009 | `contextShouldRejectNullBaseFuture` | P1 | baseFuture=null | 抛异常 |
| CTX-010 | `contextShouldRejectNullRuntime` | P1 | runtime=null | 抛异常 |

---

# 6. TaskExecutionRuntimeStateMachineTest

## 6.1 测试目标

验证任务状态机和并发裁决逻辑。  
`TaskExecutionRuntime` 是一期正确性的地基。

---

## 6.2 基础状态转换

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| RT-001 | `initialStatusShouldBeCreated` | P0 | new Runtime | 初始状态 CREATED |
| RT-002 | `createdShouldMarkSubmitted` | P0 | CREATED -> SUBMITTED | 返回 true，状态 SUBMITTED |
| RT-003 | `submittedShouldMarkRunning` | P0 | SUBMITTED -> RUNNING | 返回 true，状态 RUNNING |
| RT-004 | `runningShouldResolveSuccess` | P0 | RUNNING -> SUCCESS | baseOutcomeResolved=true |
| RT-005 | `runningShouldResolveFailure` | P0 | RUNNING -> FAILED | baseOutcomeResolved=true |
| RT-006 | `runningShouldResolveTimeout` | P0 | RUNNING -> TIMEOUT | baseOutcomeResolved=true |
| RT-007 | `submittedShouldResolveRejected` | P0 | SUBMITTED -> REJECTED | baseOutcomeResolved=true |
| RT-008 | `submittedShouldCancel` | P0 | SUBMITTED -> CANCELLED | baseOutcomeResolved=true |
| RT-009 | `runningShouldCancel` | P0 | RUNNING -> CANCELLED | baseOutcomeResolved=true |

---

## 6.3 非法状态转换

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| RT-010 | `successShouldNotBecomeTimeout` | P0 | SUCCESS 后 TIMEOUT | 返回 false，状态仍 SUCCESS |
| RT-011 | `timeoutShouldNotBecomeSuccess` | P0 | TIMEOUT 后 SUCCESS | 返回 false，状态仍 TIMEOUT |
| RT-012 | `failedShouldNotBecomeSuccess` | P0 | FAILED 后 SUCCESS | 返回 false |
| RT-013 | `cancelledShouldNotBecomeSubmitted` | P0 | CANCELLED 后 SUBMITTED | 返回 false |
| RT-014 | `rejectedShouldNotBecomeRunning` | P0 | REJECTED 后 RUNNING | 返回 false |
| RT-015 | `successShouldNotBecomeCancelled` | P0 | SUCCESS 后 CANCELLED | 返回 false |
| RT-016 | `finalizedShouldNotEnterFallbackAgain` | P0 | FALLBACK_SUCCESS 后 FALLBACK | 返回 false |
| RT-017 | `submittedShouldNotBeMarkedTwice` | P1 | 重复 submitted | 第二次 false |
| RT-018 | `runningShouldNotBeMarkedBeforeSubmitted` | P1 | CREATED -> RUNNING | 返回 false 或按设计禁止 |

---

## 6.4 fallback 状态转换

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| RT-019 | `failedShouldEnterFallback` | P0 | FAILED -> FALLBACK | 返回 true |
| RT-020 | `timeoutShouldEnterFallback` | P0 | TIMEOUT -> FALLBACK | 返回 true |
| RT-021 | `rejectedShouldEnterFallback` | P0 | REJECTED -> FALLBACK | 返回 true |
| RT-022 | `cancelledShouldNotEnterFallbackByDefault` | P0 | CANCELLED -> FALLBACK | 返回 false |
| RT-023 | `fallbackShouldFinalizeSuccess` | P0 | FALLBACK -> FALLBACK_SUCCESS | finalOutcomeResolved=true |
| RT-024 | `fallbackShouldFinalizeFailed` | P0 | FALLBACK -> FALLBACK_FAILED | finalOutcomeResolved=true |
| RT-025 | `fallbackSuccessShouldNotBecomeFallbackFailed` | P0 | FALLBACK_SUCCESS 后 FALLBACK_FAILED | 返回 false |
| RT-026 | `fallbackFailedShouldNotBecomeFallbackSuccess` | P0 | FALLBACK_FAILED 后 FALLBACK_SUCCESS | 返回 false |

---

## 6.5 outcome 竞争

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| RT-027 | `baseOutcomeShouldOnlyResolveOnce` | P0 | SUCCESS/TIMEOUT 连续调用 | 只有第一次 true |
| RT-028 | `finalOutcomeShouldOnlyResolveOnce` | P0 | FALLBACK_SUCCESS/FALLBACK_FAILED 连续调用 | 只有第一次 true |
| RT-029 | `successAndTimeoutRaceShouldResolveOnlyOnce` | P0 | 多线程同时 SUCCESS/TIMEOUT | 只能一个赢 |
| RT-030 | `failureAndCancelRaceShouldResolveOnlyOnce` | P0 | 多线程同时 FAILED/CANCELLED | 只能一个赢 |
| RT-031 | `rejectAndCancelRaceShouldResolveOnlyOnce` | P0 | 多线程同时 REJECTED/CANCELLED | 只能一个赢 |

---

## 6.6 线程和执行模式

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| RT-032 | `threadPoolExecutionModeShouldBeSetWhenRunning` | P1 | 普通线程池执行 | executionMode=THREAD_POOL |
| RT-033 | `callerThreadExecutionModeShouldNotBeOverwrittenByRunning` | P1 | CALLER_RUNS 后 run | executionMode=CALLER_THREAD |
| RT-034 | `runningThreadShouldBeRecorded` | P1 | markRunning | runningThread=当前线程 |
| RT-035 | `runningThreadShouldBeClearedAfterCompletion` | P1 | complete 后 | runningThread 清理或按设计保持快照 |
| RT-036 | `fallbackThreadShouldBeRecorded` | P1 | fallback 开始 | fallbackThread=当前线程 |
| RT-037 | `fallbackThreadShouldBeClearedAfterCompletion` | P1 | fallback 完成 | fallbackThread 清理或按设计保持快照 |

---

## 6.7 时间快照

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| RT-038 | `submittedTimeShouldBeRecorded` | P1 | submitted | submittedAt 存在 |
| RT-039 | `startedTimeShouldBeRecorded` | P1 | running | startedAt 存在 |
| RT-040 | `completedTimeShouldBeRecorded` | P1 | success | completedAt 存在 |
| RT-041 | `queueCostShouldBeNonNegative` | P1 | submitted -> running | queueCost >= 0 |
| RT-042 | `runCostShouldBeNonNegative` | P1 | running -> success | runCost >= 0 |
| RT-043 | `totalCostShouldBeNonNegative` | P1 | submitted -> final | totalCost >= 0 |

---

# 7. TaskCommandTest

## 7.1 测试目标

验证 `TaskCommand` 作为真正提交给线程池的 `Runnable`，能够正确执行 operation、发布事件、完成 baseFuture，并处理 reject、timeout、cancel、shutdown abort。

---

## 7.2 正常执行

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| CMD-001 | `runShouldPublishRunningAndSuccess` | P0 | operation 成功 | 事件 RUNNING -> SUCCESS |
| CMD-002 | `runShouldCompleteBaseFutureWithValue` | P0 | operation 返回值 | baseFuture 完成成功 |
| CMD-003 | `runShouldRecordRunningThread` | P1 | operation 执行中 | runtime.runningThread 为工作线程 |
| CMD-004 | `runShouldClearOrFinalizeRunningThreadAfterSuccess` | P1 | operation 成功后 | runningThread 按设计清理或状态结束 |
| CMD-005 | `runShouldNotExecuteTwiceWhenCalledTwiceAfterSuccess` | P1 | run 重复调用 | 第二次不应重复成功事件 |

---

## 7.3 失败执行

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| CMD-006 | `runShouldPublishFailedWhenOperationThrows` | P0 | operation 抛异常 | 事件 FAILED |
| CMD-007 | `runShouldCompleteBaseFutureExceptionallyWhenOperationThrows` | P0 | operation 抛异常 | baseFuture 异常完成 |
| CMD-008 | `runShouldClassifyFailureAtRunStage` | P1 | operation 抛异常 | error stage=RUN |
| CMD-009 | `fireAndForgetFailureShouldCallUncaughtHandler` | P0 | FIRE_AND_FORGET 抛异常 | uncaught handler 被调用 |
| CMD-010 | `resultAwareFailureShouldNotCallUncaughtHandler` | P1 | RESULT_AWARE 抛异常 | 通过 Future 表达，不调用 uncaught 或按设计断言 |

---

## 7.4 queueTimeout

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| CMD-011 | `runShouldNotExecuteWhenQueueTimeoutExceeded` | P0 | submitted 后等待超过 queueTimeout 再 run | operation 不执行 |
| CMD-012 | `queueTimeoutShouldCompleteBaseFutureExceptionally` | P0 | queueTimeout | baseFuture 异常完成 |
| CMD-013 | `queueTimeoutShouldPublishTimeout` | P0 | queueTimeout | 发布 TIMEOUT 或对应 QUEUE_TIMEOUT 状态，按设计断言 |
| CMD-014 | `queueTimeoutShouldClassifyAtQueueStage` | P1 | queueTimeout | error stage=QUEUE 或 SUBMIT/RUN 前置阶段，按设计断言 |
| CMD-015 | `queueTimeoutShouldBeIdempotent` | P1 | queueTimeout 与 cancel 竞争 | 只收口一次 |

---

## 7.5 completeTimeout

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| CMD-016 | `completeTimeoutShouldReturnTrueWhenTimeoutWins` | P0 | base outcome 未确定 | 返回 true，状态 TIMEOUT |
| CMD-017 | `completeTimeoutShouldReturnFalseWhenAlreadySuccess` | P0 | SUCCESS 后 timeout | 返回 false |
| CMD-018 | `completeTimeoutShouldReturnFalseWhenAlreadyFailed` | P0 | FAILED 后 timeout | 返回 false |
| CMD-019 | `completeTimeoutShouldReturnFalseWhenAlreadyCancelled` | P0 | CANCELLED 后 timeout | 返回 false |
| CMD-020 | `completeTimeoutShouldCompleteBaseFutureExceptionally` | P0 | timeout 赢 | baseFuture 异常完成 |
| CMD-021 | `completeTimeoutShouldPublishTimeoutOnce` | P0 | timeout 重复调用 | TIMEOUT 事件一次 |
| CMD-022 | `completeTimeoutShouldClassifyAtGivenStage` | P1 | stage=WAIT_RESULT | AsyncError stage 正确 |

---

## 7.6 reject

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| CMD-023 | `rejectShouldResolveRejectedAndCompleteBaseFuture` | P0 | command.reject | 状态 REJECTED，baseFuture 异常 |
| CMD-024 | `rejectShouldPublishRejectedOnce` | P0 | 重复 reject | REJECTED 事件一次 |
| CMD-025 | `rejectShouldReturnFalseAfterSuccess` | P0 | SUCCESS 后 reject | 不覆盖 SUCCESS |
| CMD-026 | `rejectShouldReturnFalseAfterCancelled` | P0 | CANCELLED 后 reject | 不覆盖 CANCELLED |
| CMD-027 | `rejectShouldClassifyAtSubmitStage` | P1 | reject | error stage=SUBMIT |
| CMD-028 | `isBaseOutcomeResolvedShouldBeTrueAfterReject` | P0 | reject 后 | isBaseOutcomeResolved=true |

---

## 7.7 cancel

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| CMD-029 | `completeCancelledShouldCancelTask` | P0 | command.completeCancelled | 状态 CANCELLED |
| CMD-030 | `completeCancelledShouldCompleteBaseFuture` | P0 | cancel | baseFuture 完成取消或异常，按设计断言 |
| CMD-031 | `completeCancelledShouldPublishCancelledOnce` | P0 | 重复 cancel | CANCELLED 一次 |
| CMD-032 | `completeCancelledShouldReturnFalseAfterSuccess` | P0 | success 后 cancel | 返回 false |
| CMD-033 | `completeCancelledShouldReturnFalseAfterRejected` | P0 | rejected 后 cancel | 返回 false |
| CMD-034 | `cancelShouldNotTriggerFallback` | P0 | cancel 后 pipeline | 不触发 fallback |

---

## 7.8 CALLER_RUNS 与 shutdown abort

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| CMD-035 | `markCallerThreadExecutionShouldSetExecutionMode` | P1 | CallerRuns handler 调用 | executionMode=CALLER_THREAD |
| CMD-036 | `callerThreadExecutionShouldStillPublishRunningAndSuccess` | P1 | caller runs 成功 | RUNNING/SUCCESS 正常 |
| CMD-037 | `abortOnShutdownShouldCompleteCancelled` | P0 | shutdownNow 返回 command | 状态 CANCELLED |
| CMD-038 | `abortOnShutdownShouldCompleteBaseFuture` | P0 | pending command 被 abort | baseFuture 不 pending |
| CMD-039 | `abortOnShutdownShouldBeIdempotent` | P1 | abort 重复调用 | 只生效一次 |

---

# 8. DefaultTaskResultPipelineTest

## 8.1 测试目标

验证：

```text
baseFuture -> applyTimeout -> applyFallback -> finalFuture
```

全链路正确。

---

## 8.2 基础透传

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| PIPE-001 | `applyShouldReturnBaseFutureWhenNoTimeoutAndNoFallback` | P1 | 无 timeout/fallback | 透传或等价 finalFuture |
| PIPE-002 | `applyShouldPassThroughSuccessWithoutFallback` | P0 | baseFuture 成功 | finalFuture 成功 |
| PIPE-003 | `applyShouldPassThroughFailureWithoutFallback` | P0 | baseFuture 失败，无 fallback | finalFuture 异常 |

---

## 8.3 timeout

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| PIPE-004 | `applyTimeoutShouldCompleteNormallyWhenSourceCompletesFirst` | P0 | source 先成功 | finalFuture 成功 |
| PIPE-005 | `applyTimeoutShouldForwardExceptionWhenSourceFailsFirst` | P0 | source 先失败 | finalFuture 异常 |
| PIPE-006 | `applyTimeoutShouldCancelTimeoutFutureWhenSourceCompletesFirst` | P0 | source 先完成 | timeoutFuture 被 cancel |
| PIPE-007 | `applyTimeoutShouldCompleteTimeoutWhenTimeoutWins` | P0 | timeout 先触发 | 状态 TIMEOUT |
| PIPE-008 | `applyTimeoutShouldCompleteFinalFutureExceptionallyWhenNoFallback` | P0 | timeout 且无 fallback | finalFuture 异常 |
| PIPE-009 | `applyTimeoutShouldNotInterruptWhenTimeoutLoses` | P0 | success 先赢，timeout 后到 | 不 interrupt |
| PIPE-010 | `applyTimeoutShouldInterruptWhenTimeoutWinsAndConfigured` | P0 | timeout 赢且 cancelOnTimeout=true/interrupt=true | interrupt runningThread |
| PIPE-011 | `applyTimeoutShouldNotInterruptWhenCancelOnTimeoutDisabled` | P0 | cancelOnTimeout=false | 不 interrupt |
| PIPE-012 | `applyTimeoutShouldCancelWithoutInterruptWhenInterruptDisabled` | P1 | cancelOnTimeout=true/interrupt=false | 不 interrupt |
| PIPE-013 | `timeoutExceptionShouldContainTimeoutMillis` | P2 | 超时异常 | message 包含 timeout |
| PIPE-014 | `timeoutShouldClassifyAtWaitResultStage` | P1 | 结果等待超时 | error stage=WAIT_RESULT |
| PIPE-015 | `timeoutShouldPublishTimeoutBeforeFallback` | P0 | timeout 后 fallback | 事件 TIMEOUT 在 FALLBACK 前 |

---

## 8.4 fallback

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| PIPE-016 | `applyFallbackShouldPassThroughSuccess` | P0 | source 成功 | 不触发 fallback |
| PIPE-017 | `applyFallbackShouldRecoverFailure` | P0 | source failed | finalFuture 返回 fallback 值 |
| PIPE-018 | `applyFallbackShouldRecoverTimeout` | P0 | source timeout | finalFuture 返回 fallback 值 |
| PIPE-019 | `applyFallbackShouldRecoverRejected` | P0 | source rejected | finalFuture 返回 fallback 值 |
| PIPE-020 | `applyFallbackShouldCompleteExceptionallyWhenFallbackThrows` | P0 | fallback 抛异常 | finalFuture 异常 |
| PIPE-021 | `applyFallbackShouldCompleteExceptionallyWhenFallbackExecutorRejects` | P0 | fallbackExecutor 拒绝 | finalFuture 异常 |
| PIPE-022 | `queuedFallbackShouldFailWhenFallbackExecutorShutdownNow` | P0 | fallback 排队后 shutdownNow | FALLBACK_FAILED，finalFuture 不 pending |
| PIPE-023 | `cancelledTaskShouldNotTriggerFallback` | P0 | 主动取消 | 不触发 fallback |
| PIPE-024 | `fallbackShouldReceiveRootCause` | P1 | CompletionException -> BusinessException | fallback 入参是 rootCause |
| PIPE-025 | `fallbackShouldFallbackToUnwrappedThrowableWhenRootCauseNull` | P2 | rootCause 为空 | 使用 unwrap |
| PIPE-026 | `fallbackFailureShouldBeClassifiedAsFallbackStage` | P1 | fallback 抛异常 | stage=FALLBACK |
| PIPE-027 | `fallbackSuccessShouldPublishFallbackSuccessAndCompleted` | P0 | fallback 成功 | 发布 FALLBACK_SUCCESS 和 completed |
| PIPE-028 | `fallbackFailureShouldPublishFallbackFailureAndCompleted` | P0 | fallback 失败 | 发布 FALLBACK_FAILED 和 completed |
| PIPE-029 | `fallbackShouldRunInFallbackExecutorThread` | P1 | fallback 执行 | 线程名为 fallback executor |
| PIPE-030 | `fallbackShouldNotRunInTimeoutSchedulerThread` | P1 | timeout 后 fallback | fallback 不在 timeoutScheduler 执行 |

---

# 9. DefaultTaskExecutionTemplateTest

## 9.1 测试目标

覆盖 `submitInternal` 主链路和 `AsyncExecutor` 对外入口。

---

## 9.2 Public API 入口

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| EXEC-001 | `submitShouldReturnFinalFuture` | P0 | submit AsyncTask | 返回 finalFuture |
| EXEC-002 | `submitShouldCompleteWithValue` | P0 | submit 成功任务 | Future 成功 |
| EXEC-003 | `submitShouldCompleteExceptionallyWhenTaskFails` | P0 | submit 失败任务 | Future 异常 |
| EXEC-004 | `submitHandleShouldReturnHandle` | P0 | submitHandle | 返回 TaskHandle |
| EXEC-005 | `submitHandleShouldExposeTaskId` | P1 | submitHandle | handle.taskId 正确 |
| EXEC-006 | `submitHandleCancelShouldCancelTask` | P0 | handle.cancel | 任务取消 |
| EXEC-007 | `supplyShouldReturnValue` | P0 | supply | Future 返回值 |
| EXEC-008 | `runShouldReturnVoidFuture` | P0 | run Runnable | 返回 CompletableFuture<Void> |
| EXEC-009 | `runShouldReturnNullValueWhenCompleted` | P1 | run 完成 | Future 结果为 null |
| EXEC-010 | `executeShouldNotReturnFuture` | P1 | execute | void，不返回 Future |
| EXEC-011 | `executeShouldStillCreateCommandAndBaseFuture` | P1 | execute | 仍走组件托管 |
| EXEC-012 | `tryExecuteShouldReturnTrueWhenAccepted` | P0 | tryExecute 成功 | true |
| EXEC-013 | `tryExecuteShouldReturnFalseWhenRejectedByDiscard` | P0 | DISCARD 拒绝不抛 | false |
| EXEC-014 | `cancelShouldReturnNotFoundWhenTaskMissing` | P1 | cancel unknown | NOT_FOUND |
| EXEC-015 | `cancelShouldReturnAlreadyCompletedWhenTaskDone` | P1 | 完成后 cancel | ALREADY_COMPLETED |

---

## 9.3 submitInternal 主链路

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| EXEC-016 | `submitInternalShouldValidateTask` | P0 | task 非法 | 抛异常 |
| EXEC-017 | `submitInternalShouldCreateTaskDefinitionSnapshot` | P0 | 提交任务 | 使用 TaskDefinition |
| EXEC-018 | `submitInternalShouldUseDefinitionAfterSnapshot` | P0 | 提交后修改 AsyncTask | 不影响执行 |
| EXEC-019 | `submitInternalShouldGetExecutorByDefinitionExecutorName` | P0 | executorName | 从 Registry 获取正确线程池 |
| EXEC-020 | `submitInternalShouldCreateBaseFuture` | P1 | 提交 | baseFuture 创建 |
| EXEC-021 | `submitInternalShouldDecorateOperationWhenContextPropagationEnabled` | P1 | contextPropagation=true | operation 被装饰 |
| EXEC-022 | `submitInternalShouldNotDecorateWhenContextPropagationDisabled` | P1 | contextPropagation=false | operation 不装饰 |
| EXEC-023 | `submitInternalShouldCreateRuntimeWithResultMode` | P1 | RESULT_AWARE/FIRE_AND_FORGET | runtime.resultMode 正确 |
| EXEC-024 | `submitInternalShouldCreateContext` | P1 | 提交 | context 字段正确 |
| EXEC-025 | `submitInternalShouldCreateCommand` | P1 | 提交 | command 创建 |
| EXEC-026 | `submittedShouldBePublishedBeforePipeline` | P0 | 极短 timeout | SUBMITTED 早于 TIMEOUT/FALLBACK |
| EXEC-027 | `resultAwareShouldApplyPipeline` | P0 | RESULT_AWARE | resultPipeline.apply 被调用 |
| EXEC-028 | `fireAndForgetShouldNotApplyPipeline` | P0 | FIRE_AND_FORGET | resultPipeline.apply 不调用 |
| EXEC-029 | `submitShouldRegisterTaskControl` | P0 | 提交 | TaskControlRegistry 有记录 |
| EXEC-030 | `finalFutureCompleteShouldRemoveTaskControl` | P0 | 任务完成 | TaskControl 移除 |
| EXEC-031 | `removeControlShouldUseExpectedControl` | P1 | 同 taskId 复用 | 不误删新 control |
| EXEC-032 | `executorExecuteShouldReceiveTaskCommand` | P0 | 提交 | executor.execute(command) |

---

## 9.4 拒绝兜底

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| EXEC-033 | `rejectedByAwareHandlerShouldNotDoubleReject` | P0 | 增强拒绝策略已通知 command | catch 不重复 reject |
| EXEC-034 | `rejectedByNativeAbortPolicyShouldBeCompensatedByCatch` | P0 | JDK AbortPolicy 只抛不通知 | catch 调用 command.reject |
| EXEC-035 | `fireAndForgetRejectedShouldThrowConcurrencyRejectedException` | P0 | execute 同步拒绝 | 抛 ConcurrencyRejectedException |
| EXEC-036 | `resultAwareRejectedShouldCompleteFutureExceptionally` | P0 | submit 同步拒绝 | Future 异常完成 |
| EXEC-037 | `discardRejectedShouldNotThrowButTryExecuteReturnFalse` | P0 | DISCARD 不抛 | tryExecute=false |
| EXEC-038 | `rejectedTaskShouldNotLeakTaskControl` | P0 | 拒绝后 | control 移除 |

---

# 10. 拒绝策略测试

## 10.1 AwareAbortRejectedExecutionHandlerTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| REJ-ABORT-001 | `abortShouldNotifyRejectedTaskAware` | P0 | 队列满触发 ABORT | command.reject 被调用 |
| REJ-ABORT-002 | `abortShouldThrowRejectedExecutionException` | P0 | 队列满 | 同步抛 RejectedExecutionException |
| REJ-ABORT-003 | `abortShouldCompleteFutureExceptionally` | P0 | 拒绝 | Future 异常完成 |
| REJ-ABORT-004 | `abortShouldPublishRejectedOnce` | P0 | catch 兜底存在 | REJECTED 一次 |
| REJ-ABORT-005 | `abortShouldClassifySubmitStage` | P1 | 拒绝 | stage=SUBMIT |
| REJ-ABORT-006 | `abortShouldNotExecuteRejectedCommand` | P0 | 拒绝 | operation 不执行 |

---

## 10.2 CallerRunsRejectedExecutionHandlerTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| REJ-CALLER-001 | `callerRunsShouldExecuteInSubmittingThread` | P0 | 队列满 CALLER_RUNS | 提交线程执行 |
| REJ-CALLER-002 | `callerRunsShouldMarkCallerThreadExecutionMode` | P0 | CALLER_RUNS | executionMode=CALLER_THREAD |
| REJ-CALLER-003 | `callerRunsShouldNotMarkRejectedWhenExecutorRunning` | P0 | executor 未 shutdown | 不发布 REJECTED |
| REJ-CALLER-004 | `callerRunsShouldCompleteFutureNormallyWhenTaskSucceeds` | P0 | caller 执行成功 | Future 成功 |
| REJ-CALLER-005 | `callerRunsShouldCompleteFutureExceptionallyWhenTaskFails` | P0 | caller 执行失败 | Future 异常 |
| REJ-CALLER-006 | `callerRunsShouldRejectWhenExecutorShutdown` | P0 | executor shutdown | 拒绝 |
| REJ-CALLER-007 | `callerRunsShutdownShouldNotifyRejectedTaskAware` | P0 | shutdown 后提交 | command.reject |
| REJ-CALLER-008 | `callerRunsShouldRespectQueueTimeoutIfAlreadyExpired` | P1 | caller 执行前 queueTimeout 已过 | 不执行 operation 或按设计断言 |

---

## 10.3 DiscardRejectedExecutionHandlerTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| REJ-DISCARD-001 | `discardShouldNotifyRejectedTaskAware` | P0 | 队列满 DISCARD | command.reject |
| REJ-DISCARD-002 | `discardShouldNotThrow` | P0 | DISCARD | executor.execute 不抛 |
| REJ-DISCARD-003 | `discardShouldCompleteFutureExceptionally` | P0 | DISCARD | Future 不 pending |
| REJ-DISCARD-004 | `discardShouldPublishRejectedOnce` | P0 | DISCARD | REJECTED 一次 |
| REJ-DISCARD-005 | `tryExecuteShouldReturnFalseForDiscard` | P0 | tryExecute + DISCARD | false |
| REJ-DISCARD-006 | `discardShouldNotExecuteOperation` | P0 | DISCARD | operation 不执行 |

---

## 10.4 DiscardOldestRejectedExecutionHandlerTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| REJ-OLDEST-001 | `discardOldestShouldRejectOldestTask` | P0 | 队列满 | oldest command rejected |
| REJ-OLDEST-002 | `discardOldestShouldCompleteOldestFutureExceptionally` | P0 | oldest 被丢弃 | oldest Future 不 pending |
| REJ-OLDEST-003 | `discardOldestShouldEnqueueCurrentTask` | P0 | 丢弃 oldest 后有空位 | current 入队 |
| REJ-OLDEST-004 | `discardOldestShouldRejectCurrentWhenQueueStillFull` | P0 | 空位被抢占 | current rejected |
| REJ-OLDEST-005 | `discardOldestShouldRejectCurrentWhenExecutorShutdown` | P0 | executor shutdown | current rejected |
| REJ-OLDEST-006 | `discardOldestShouldCheckShutdownAfterEnqueue` | P0 | 入队后 executor shutdown | current 从队列移除并 rejected |
| REJ-OLDEST-007 | `discardOldestShouldHandleNonAwareOldestRunnable` | P1 | oldest 非 RejectedTaskAware | 不崩溃 |
| REJ-OLDEST-008 | `discardOldestShouldNotLoseInterruptedStatus` | P2 | 线程中断场景 | 中断标记合理 |

---

## 10.5 BlockingWaitRejectedExecutionHandlerTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| REJ-BLOCK-001 | `blockingWaitShouldEnqueueWhenSpaceAvailable` | P0 | 等待期间队列出现空位 | 入队成功 |
| REJ-BLOCK-002 | `blockingWaitShouldRejectWhenTimeout` | P0 | 等待超时 | current rejected |
| REJ-BLOCK-003 | `blockingWaitShouldCompleteFutureExceptionallyWhenTimeout` | P0 | 等待超时拒绝 | Future 不 pending |
| REJ-BLOCK-004 | `blockingWaitShouldRestoreInterruptWhenInterrupted` | P0 | 等待被中断 | 恢复中断标记 |
| REJ-BLOCK-005 | `blockingWaitShouldRejectWhenExecutorShutdownBeforeWait` | P0 | 等待前 shutdown | rejected |
| REJ-BLOCK-006 | `blockingWaitShouldRejectWhenShutdownAfterEnqueue` | P0 | 入队后 shutdown | remove 并 rejected |
| REJ-BLOCK-007 | `blockingWaitShouldUseConfiguredRejectionWaitTime` | P1 | 配置等待时间 | 等待时间符合预期 |
| REJ-BLOCK-008 | `blockingWaitShouldRejectNonAwareRunnableByThrowing` | P1 | runnable 非 aware | 抛 RejectedExecutionException |

---

# 11. TaskControlTest

## 11.1 测试目标

验证 `TaskControl` 作为内部取消控制对象，能够关联：

```text
executor
command
finalFuture
```

并为 `TaskCancellationManager` 提供取消能力。

---

## 11.2 测试用例

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| CAN-CTRL-001 | `taskControlShouldHoldExecutorCommandAndFuture` | P1 | 创建 TaskControl | 字段正确 |
| CAN-CTRL-002 | `cancelQueuedTaskShouldRemoveCommandFromQueue` | P0 | command 在队列中 | executor.remove(command)=true |
| CAN-CTRL-003 | `cancelQueuedTaskShouldCompleteCommandCancelled` | P0 | queued cancel | command CANCELLED |
| CAN-CTRL-004 | `cancelRunningTaskShouldCallCommandCancel` | P0 | running cancel | command completeCancelled |
| CAN-CTRL-005 | `cancelCompletedTaskShouldReturnAlreadyCompleted` | P0 | already final | ALREADY_COMPLETED |
| CAN-CTRL-006 | `cancelShouldCompleteFinalFuture` | P0 | cancel | finalFuture 收口 |
| CAN-CTRL-007 | `cancelShouldBeIdempotent` | P0 | 重复 cancel | 只生效一次 |
| CAN-CTRL-008 | `cancelTrueShouldInterruptRunningThread` | P0 | mayInterrupt=true | interrupt |
| CAN-CTRL-009 | `cancelFalseShouldNotInterruptRunningThread` | P0 | mayInterrupt=false | 不 interrupt |
| CAN-CTRL-010 | `cancelFallbackRunningShouldInterruptFallbackThread` | P1 | fallback 中取消 | interrupt fallbackThread |

---

# 12. TaskCancellationManagerTest

## 12.1 测试目标

验证通过 taskId 取消任务的统一入口。

---

## 12.2 测试用例

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| CAN-MGR-001 | `cancelShouldReturnNotFoundWhenTaskMissing` | P0 | registry 无 taskId | NOT_FOUND |
| CAN-MGR-002 | `cancelShouldDelegateToTaskControl` | P0 | registry 有 control | 调用 control.cancel |
| CAN-MGR-003 | `cancelShouldReturnCancelledWhenControlCancels` | P0 | control 成功取消 | CANCELLED |
| CAN-MGR-004 | `cancelShouldReturnAlreadyCompletedWhenControlCompleted` | P0 | control 已完成 | ALREADY_COMPLETED |
| CAN-MGR-005 | `cancelShouldRemoveControlAfterCancelled` | P1 | cancel 完成 | registry 移除 |
| CAN-MGR-006 | `cancelShouldNotRemoveDifferentControlWithSameTaskId` | P1 | 同 taskId 新旧 control | 不误删 |

---

# 13. TaskControlCancellationIntegrationTest

## 13.1 测试目标

集成验证 cancel queued / running / fallback 全链路。

---

## 13.2 测试用例

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| CAN-INT-001 | `cancelQueuedTaskShouldRemoveFromQueue` | P0 | 单线程池占住线程，第二任务排队后取消 | 从队列移除 |
| CAN-INT-002 | `cancelQueuedTaskShouldNotExecuteOperation` | P0 | queued cancel | operation 不执行 |
| CAN-INT-003 | `cancelQueuedTaskShouldPublishCancelled` | P0 | queued cancel | CANCELLED |
| CAN-INT-004 | `cancelRunningTaskShouldMarkCancelled` | P0 | running cancel | CANCELLED |
| CAN-INT-005 | `cancelRunningTaskWithInterruptShouldInterruptThread` | P0 | cancel(true) | 运行线程收到 interrupt |
| CAN-INT-006 | `cancelRunningTaskWithoutInterruptShouldNotInterruptThread` | P0 | cancel(false) | 不 interrupt |
| CAN-INT-007 | `cancelCompletedTaskShouldReturnAlreadyCompleted` | P0 | 完成后 cancel | ALREADY_COMPLETED |
| CAN-INT-008 | `cancelUnknownTaskShouldReturnNotFound` | P0 | unknown taskId | NOT_FOUND |
| CAN-INT-009 | `cancelFallbackRunningShouldInterruptFallbackThread` | P1 | fallback 正在跑 | fallback 线程 interrupt |
| CAN-INT-010 | `cancelShouldNotTriggerFallback` | P0 | cancel | 不触发 fallback |
| CAN-INT-011 | `cancelShouldCompleteFinalFuture` | P0 | cancel | finalFuture 不 pending |
| CAN-INT-012 | `cancelShouldPublishCancelledOnce` | P0 | 重复 cancel | CANCELLED 一次 |
| CAN-INT-013 | `cancelShouldRemoveTaskControlAfterFinalFutureComplete` | P1 | cancel 完成 | TaskControl 移除 |

---

# 14. DefaultAsyncTemplateTest

## 14.1 测试目标

覆盖所有 `CompletableFuture` 编排能力。

---

## 14.2 allOf

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| TPL-ALL-001 | `allOfShouldCompleteWhenAllSuccess` | P0 | 全成功 | all 完成成功 |
| TPL-ALL-002 | `allOfShouldFailWhenAnyFails` | P0 | 一个失败 | all 异常 |
| TPL-ALL-003 | `allOfShouldCompleteImmediatelyForEmptyInput` | P1 | 空输入 | 立即完成 |
| TPL-ALL-004 | `allOfShouldRejectNullInputArray` | P1 | input=null | 抛异常 |
| TPL-ALL-005 | `allOfShouldRejectNullFutureElement` | P1 | 含 null future | 抛异常 |

---

## 14.3 allOfOutcome

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| TPL-OUT-001 | `allOfOutcomeShouldCollectSuccessAndFailure` | P0 | 有成功有失败 | 全部 outcome 返回 |
| TPL-OUT-002 | `allOfOutcomeShouldKeepInputOrder` | P0 | 完成顺序不同 | 结果顺序与输入一致 |
| TPL-OUT-003 | `allOfOutcomeShouldMarkSuccessOutcome` | P1 | 成功 future | outcome.success=true |
| TPL-OUT-004 | `allOfOutcomeShouldMarkFailureOutcome` | P1 | 失败 future | outcome.failure=true |
| TPL-OUT-005 | `allOfOutcomeShouldUnwrapCompletionException` | P1 | CompletionException | outcome.error 为 unwrap 后异常 |
| TPL-OUT-006 | `allOfOutcomeShouldReturnEmptyListForEmptyInput` | P1 | 空输入 | 空列表 |

---

## 14.4 allOfFailFast

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| TPL-FAST-001 | `allOfFailFastShouldFailOnFirstFailure` | P0 | 某个 future 先失败 | 立即异常完成 |
| TPL-FAST-002 | `allOfFailFastShouldPassWhenAllSuccess` | P0 | 全成功 | 成功 |
| TPL-FAST-003 | `allOfFailFastShouldNotWaitForSlowSuccessAfterFailure` | P1 | 一个快速失败，一个慢成功 | 不等慢成功 |
| TPL-FAST-004 | `allOfFailFastShouldHandleEmptyInput` | P1 | 空输入 | 立即成功 |

---

## 14.5 anyOf / anySuccess

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| TPL-ANY-001 | `anyOfShouldCompleteWithFirstCompleted` | P0 | 第一个完成成功 | 返回第一个结果 |
| TPL-ANY-002 | `anyOfShouldCompleteExceptionallyWhenFirstFails` | P0 | 第一个完成失败 | 整体失败 |
| TPL-ANY-003 | `anySuccessShouldCompleteWithFirstSuccess` | P0 | 第一个成功 | 返回成功值 |
| TPL-ANY-004 | `anySuccessShouldIgnoreEarlyFailures` | P0 | 先失败后成功 | 最终成功 |
| TPL-ANY-005 | `anySuccessShouldFailWhenAllFailed` | P0 | 全失败 | 整体失败 |
| TPL-ANY-006 | `anySuccessShouldRejectEmptyInput` | P1 | 空输入 | 抛异常或失败 Future，按设计断言 |
| TPL-ANY-007 | `anySuccessShouldUnwrapAllFailures` | P1 | 全失败 | 聚合异常包含 root/unwrap 信息 |

---

## 14.6 withTimeout

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| TPL-TIME-001 | `withTimeoutShouldCompleteNormallyWhenSourceFirst` | P0 | source 先完成 | wrapper 成功 |
| TPL-TIME-002 | `withTimeoutShouldCompleteExceptionallyWhenTimeoutFirst` | P0 | timeout 先到 | wrapper TimeoutException |
| TPL-TIME-003 | `withTimeoutShouldNotModifySourceFuture` | P0 | wrapper 超时 | sourceFuture 仍可后续完成 |
| TPL-TIME-004 | `withTimeoutShouldCancelTimerWhenSourceCompletes` | P0 | source 先完成 | timer cancel |
| TPL-TIME-005 | `withTimeoutShouldRejectNullFuture` | P1 | source=null | 抛异常 |
| TPL-TIME-006 | `withTimeoutShouldRejectNonPositiveTimeout` | P1 | timeout<=0 | 按设计抛异常或立即 timeout |
| TPL-TIME-007 | `withTimeoutShouldUnwrapSourceFailure` | P1 | source CompletionException | wrapper 异常为 unwrap 后或按设计断言 |

---

## 14.7 withFallback

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| TPL-FB-001 | `withFallbackShouldPassThroughSuccess` | P0 | source 成功 | 不调用 fallback |
| TPL-FB-002 | `withFallbackShouldRecoverFailure` | P0 | source 失败 | 返回 fallback 值 |
| TPL-FB-003 | `withFallbackShouldFailWhenFallbackThrows` | P0 | fallback 抛异常 | wrapper 异常 |
| TPL-FB-004 | `withFallbackShouldPassUnwrappedError` | P1 | source CompletionException | fallback 入参 unwrap |
| TPL-FB-005 | `withFallbackShouldRejectNullFallback` | P1 | fallback=null | 抛异常 |

---

# 15. DefaultTaskExecutionRegistryTest

## 15.1 测试目标

覆盖任务状态注册表。

---

## 15.2 基本查询

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| REG-001 | `updateShouldStoreSnapshot` | P0 | update snapshot | get 能查到 |
| REG-002 | `getShouldReturnSnapshot` | P0 | get(taskId) | 返回对应 snapshot |
| REG-003 | `getUnknownShouldReturnEmpty` | P1 | unknown taskId | Optional.empty |
| REG-004 | `removeShouldDeleteSnapshot` | P0 | remove | get empty |
| REG-005 | `clearShouldRemoveAll` | P1 | clear | 全部清空 |

---

## 15.3 recent 与容量

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| REG-006 | `recentShouldReturnLatestFirst` | P0 | 多任务 update | recent 最新优先 |
| REG-007 | `recentShouldRespectLimit` | P1 | recent(2) | 返回 2 条 |
| REG-008 | `recentShouldSkipStaleIndexEntries` | P0 | 同 task 多次 update | 跳过旧索引 |
| REG-009 | `sameTaskMultipleUpdatesShouldKeepLatestVersion` | P0 | 同 taskId 多版本 | get 返回最新 |
| REG-010 | `registryShouldEvictOldestWhenOverCapacity` | P0 | 超容量 | 淘汰最老 |
| REG-011 | `evictionShouldRemoveSnapshotAndIndexEventually` | P1 | 淘汰后 recent | 不返回已淘汰 |

---

## 15.4 并发

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| REG-012 | `concurrentUpdateShouldNotLoseLatestSnapshot` | P0 | 多线程 update 同 task | 最新快照不丢 |
| REG-013 | `concurrentRecentShouldNotThrow` | P1 | update 与 recent 并发 | 不抛异常 |
| REG-014 | `concurrentRemoveShouldNotRemoveNewerSnapshot` | P1 | remove 旧版本时新版本已写入 | 不误删 |
| REG-015 | `publishCompletedShouldNotDuplicateRegistryUpdate` | P1 | completed 事件 | 不重复 update 版本 |

---

# 16. DefaultTaskLifecyclePublisherTest

## 16.1 测试目标

验证生命周期发布器统一发布事件到：

```text
Registry
Metrics
Listener
Diagnostics
```

---

## 16.2 状态事件

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| LIFE-001 | `publishSubmittedShouldUpdateRegistryAndListener` | P0 | SUBMITTED | registry update + listener |
| LIFE-002 | `publishRunningShouldRecordStartedMetric` | P0 | RUNNING | started metric |
| LIFE-003 | `publishSuccessShouldRecordSuccess` | P0 | SUCCESS | success metric/listener |
| LIFE-004 | `publishFailureShouldRecordFailure` | P0 | FAILED | failure metric/listener |
| LIFE-005 | `publishRejectedShouldRecordRejected` | P0 | REJECTED | rejected metric/listener |
| LIFE-006 | `publishTimeoutShouldRecordTimeout` | P0 | TIMEOUT | timeout metric/listener |
| LIFE-007 | `publishCancelledShouldRecordCancelled` | P0 | CANCELLED | cancelled metric/listener |
| LIFE-008 | `publishFallbackShouldRecordFallbackTriggered` | P1 | FALLBACK | fallback triggered |
| LIFE-009 | `publishFallbackSuccessShouldRecordFallbackSuccess` | P0 | FALLBACK_SUCCESS | fallback success |
| LIFE-010 | `publishFallbackFailureShouldRecordFallbackFailure` | P0 | FALLBACK_FAILED | fallback failure |
| LIFE-011 | `publishCompletedShouldOnlyCallCompletedAndMetrics` | P0 | completed | 不重复 registry update |

---

## 16.3 异常隔离

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| LIFE-012 | `listenerExceptionShouldNotBreakPublish` | P0 | listener 抛异常 | publish 不失败 |
| LIFE-013 | `metricsExceptionShouldNotBreakPublish` | P1 | metrics 抛异常 | publish 不失败 |
| LIFE-014 | `registryExceptionShouldNotBreakPublish` | P1 | registry 抛异常 | publish 不失败 |
| LIFE-015 | `internalDiagnosticsShouldRecordListenerFailure` | P1 | listener 抛异常 | diagnostics 记录 |
| LIFE-016 | `internalDiagnosticsShouldRecordMetricsFailure` | P1 | metrics 抛异常 | diagnostics 记录 |
| LIFE-017 | `eventsShouldBePublishedInExpectedOrder` | P0 | success 链路 | SUBMITTED/RUNNING/SUCCESS/COMPLETED |

---

# 17. CompositeTaskExecutionListenerTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| LSN-001 | `shouldInvokeAllListeners` | P1 | 多 listener | 全部调用 |
| LSN-002 | `shouldInvokeListenersInOrder` | P1 | listener 有 order | 顺序正确 |
| LSN-003 | `listenerFailureShouldNotAffectNextListener` | P0 | 第一个 listener 抛异常 | 后续 listener 仍调用 |
| LSN-004 | `shouldUseEventCopyForEachListener` | P1 | listener 修改 event | 不影响其他 listener |
| LSN-005 | `lazyResolutionShouldAvoidEarlyBeanCreation` | P2 | Spring 懒加载 | 不提前创建 |
| LSN-006 | `shouldFilterSelfComposite` | P1 | composite 自身被注入 | 避免递归 |
| LSN-007 | `emptyListenersShouldNotFail` | P1 | 无 listener | 不失败 |

---

# 18. CompositeAsyncErrorClassifierTest

## 18.1 规则编排

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| ERR-001 | `shouldUseFirstSupportedRule` | P0 | 多规则支持 | 使用第一条 |
| ERR-002 | `shouldOrderRulesByOrderValue` | P0 | rules order 不同 | order 小的先 |
| ERR-003 | `shouldFallbackToDefaultWhenNoRuleMatches` | P0 | 无规则命中 | 默认分类 |
| ERR-004 | `ruleSupportsThrowsShouldFallThrough` | P1 | supports 抛异常 | 继续后续规则 |
| ERR-005 | `ruleClassifyThrowsShouldFallThrough` | P1 | classify 抛异常 | 继续后续规则 |
| ERR-006 | `emptyRulesShouldUseDefault` | P1 | 无 rules | 默认分类 |

---

## 18.2 上下文与异常解包

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| ERR-007 | `shouldExposeTaskMetadataInContext` | P0 | classify | context 中是 TaskMetadata |
| ERR-008 | `shouldNotExposeMutableAsyncTaskInContext` | P0 | classify | 不依赖可变 AsyncTask |
| ERR-009 | `shouldKeepAsyncTaskExceptionWhenUnwrap` | P0 | CompletionException -> AsyncTaskException | unwrap 保留 AsyncTaskException |
| ERR-010 | `rootCauseShouldReturnBusinessException` | P0 | AsyncTaskException -> BusinessException | rootCause 为业务异常 |
| ERR-011 | `unwrapShouldOnlyUnwrapCompletionExceptionAndExecutionException` | P0 | 普通 RuntimeException 有 cause | 不过度剥离 |
| ERR-012 | `rootCauseShouldHandleNull` | P2 | null throwable | 返回 null 或按设计断言 |

---

## 18.3 阶段分类

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| ERR-013 | `shouldClassifySubmitRejectedStage` | P1 | SUBMIT 拒绝 | stage=SUBMIT |
| ERR-014 | `shouldClassifyRunFailureStage` | P1 | RUN 失败 | stage=RUN |
| ERR-015 | `shouldClassifyWaitResultTimeoutStage` | P1 | 等待结果超时 | stage=WAIT_RESULT |
| ERR-016 | `shouldClassifyFallbackFailureStage` | P1 | fallback 失败 | stage=FALLBACK |
| ERR-017 | `shouldClassifyQueueTimeoutStage` | P1 | queueTimeout | stage 按设计正确 |

---

# 19. CompletableFutureExceptionUtilsTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| UTIL-001 | `unwrapShouldReturnCauseForCompletionException` | P0 | CompletionException(cause) | 返回 cause |
| UTIL-002 | `unwrapShouldReturnCauseForExecutionException` | P0 | ExecutionException(cause) | 返回 cause |
| UTIL-003 | `unwrapShouldReturnOriginalForAsyncTaskException` | P0 | AsyncTaskException(cause) | 返回原异常 |
| UTIL-004 | `unwrapShouldReturnOriginalForRuntimeExceptionWithCause` | P0 | RuntimeException(cause) | 返回原异常 |
| UTIL-005 | `rootCauseShouldReturnDeepestCause` | P0 | 多层 cause | 最底层 |
| UTIL-006 | `rootCauseShouldReturnOriginalWhenNoCause` | P1 | 无 cause | 返回原异常 |
| UTIL-007 | `rootCauseShouldHandleNull` | P2 | null | null |

---

# 20. ThreadPoolSpecValidationTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| POOL-SPEC-001 | `boundedQueueShouldRejectZeroCapacity` | P0 | 有界队列 queueCapacity=0 | 抛异常 |
| POOL-SPEC-002 | `boundedQueueShouldRejectNegativeCapacity` | P0 | queueCapacity<0 | 抛异常 |
| POOL-SPEC-003 | `directHandoffShouldAllowZeroCapacity` | P1 | DIRECT_HANDOFF | queueCapacity 可忽略 |
| POOL-SPEC-004 | `maxPoolSizeShouldBeGreaterOrEqualCore` | P0 | max < core | 抛异常 |
| POOL-SPEC-005 | `corePoolSizeShouldBePositive` | P0 | core<=0 | 抛异常 |
| POOL-SPEC-006 | `keepAliveShouldNotBeNegative` | P0 | keepAlive<0 | 抛异常 |
| POOL-SPEC-007 | `allowCoreTimeoutRequiresPositiveKeepAlive` | P0 | allowCoreThreadTimeout=true + keepAlive=0 | 抛异常 |
| POOL-SPEC-008 | `blockingWaitRequiresPositiveWaitTime` | P0 | BLOCKING_WAIT 未配置 waitTime | 抛异常 |
| POOL-SPEC-009 | `awaitTerminationShouldBePositiveWhenWaitOnShutdown` | P1 | waitForTasksToComplete=true + await<=0 | 抛异常 |
| POOL-SPEC-010 | `threadNamePrefixShouldHaveDefaultWhenBlank` | P2 | prefix blank | 使用默认前缀 |
| POOL-SPEC-011 | `unknownRejectionPolicyShouldFailFast` | P1 | 未知策略 | 快速失败 |
| POOL-SPEC-012 | `queueTypeShouldNotBeNull` | P1 | queueType=null | 使用默认或抛异常，按设计断言 |

---

# 21. ThreadPoolRegistryTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| POOL-REG-001 | `registerShouldStoreExecutor` | P0 | 注册 executor | 可查询 |
| POOL-REG-002 | `registerDuplicateNameShouldFail` | P0 | 同名不同实例 | 抛异常 |
| POOL-REG-003 | `registerSameInstanceShouldBeIdempotentIfAllowed` | P1 | 同名同实例 | 幂等或按设计断言 |
| POOL-REG-004 | `getUnknownShouldThrow` | P0 | 未知 pool | 抛异常 |
| POOL-REG-005 | `listShouldReturnAllRegisteredPools` | P1 | 多 pool | 全部返回 |
| POOL-REG-006 | `snapshotShouldBeCopyNotLiveView` | P1 | 查询 snapshot 后 pool 变化 | snapshot 不自动变 |
| POOL-REG-007 | `removeShouldRemoveExecutor` | P1 | remove | 查询不到 |
| POOL-REG-008 | `shutdownShouldShutdownAllRegisteredExecutors` | P1 | shutdown registry | 所有 executor shutdown |
| POOL-REG-009 | `shutdownNowShouldAbortPendingShutdownAbortAwareTasks` | P0 | pending command | abortOnShutdown 被调用 |
| POOL-REG-010 | `shutdownNowShouldHandleNonAwareRunnable` | P1 | pending 普通 Runnable | 不崩溃 |

---

# 22. ThreadPoolManagerTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| POOL-MGR-001 | `snapshotShouldExposeRuntimeInfo` | P0 | 查询线程池 | active/queue/poolSize 正确 |
| POOL-MGR-002 | `updateCoreAndMaxShouldRespectOrderWhenIncreasing` | P0 | core/max 同时扩容 | 先 max 后 core |
| POOL-MGR-003 | `updateCoreAndMaxShouldRespectOrderWhenDecreasing` | P0 | core/max 同时缩容 | 先 core 后 max |
| POOL-MGR-004 | `updateUnknownPoolShouldFail` | P0 | unknown pool | 抛异常 |
| POOL-MGR-005 | `updateInvalidSpecShouldFail` | P0 | 非法参数 | 抛异常 |
| POOL-MGR-006 | `updateKeepAliveShouldApply` | P1 | 修改 keepAlive | executor 生效 |
| POOL-MGR-007 | `updateAllowCoreThreadTimeoutShouldApply` | P1 | 修改 allowCoreTimeout | executor 生效 |
| POOL-MGR-008 | `updateShouldReturnBeforeAndAfterSnapshot` | P1 | 更新线程池 | 返回前后快照 |
| POOL-MGR-009 | `updateFailureShouldNotLeavePartialInvalidState` | P0 | 更新中失败 | 不留下非法中间态 |
| POOL-MGR-010 | `snapshotUnknownPoolShouldFail` | P1 | unknown pool snapshot | 抛异常 |
| POOL-MGR-011 | `listSnapshotsShouldReturnAllPools` | P1 | 多 pool | 返回全部 snapshots |

---

# 23. Spring Boot Starter 测试

## 23.1 XjtuIronConcurrencyAutoConfigurationTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| BOOT-AUTO-001 | `contextShouldStartWithDefaultConfiguration` | P0 | 默认配置 | Spring 启动成功 |
| BOOT-AUTO-002 | `shouldCreateAsyncExecutor` | P0 | 自动装配 | AsyncExecutor Bean 存在 |
| BOOT-AUTO-003 | `shouldCreateAsyncTemplate` | P0 | 自动装配 | AsyncTemplate Bean 存在 |
| BOOT-AUTO-004 | `shouldCreateTaskResultPipeline` | P0 | 自动装配 | Pipeline Bean 存在 |
| BOOT-AUTO-005 | `shouldCreateThreadPoolsFromProperties` | P0 | 配置多个 pool | ThreadPoolRegistry 有 pool |
| BOOT-AUTO-006 | `shouldCreateFallbackExecutor` | P1 | 默认配置 | fallbackExecutor 存在 |
| BOOT-AUTO-007 | `shouldCreateTimeoutScheduler` | P1 | 默认配置 | timeoutScheduler 存在 |
| BOOT-AUTO-008 | `customTaskExecutionListenerShouldBeCollected` | P1 | 用户自定义 listener | 被组合监听器收集 |
| BOOT-AUTO-009 | `customErrorRuleShouldBeCollected` | P1 | 用户自定义 error rule | 被 classifier 收集 |
| BOOT-AUTO-010 | `customFallbackExecutorShouldOverrideDefault` | P1 | 用户提供同名 Bean | 默认不创建 |
| BOOT-AUTO-011 | `customTaskDecoratorShouldBeApplied` | P1 | 用户 TaskDecorator | 生效 |
| BOOT-AUTO-012 | `customUncaughtExceptionHandlerShouldBeApplied` | P1 | 用户 handler | 生效 |
| BOOT-AUTO-013 | `invalidThreadPoolSpecShouldFailStartup` | P0 | 非法配置 | 启动失败 |
| BOOT-AUTO-014 | `shouldNotCreateCircularDependencyWithListener` | P1 | listener 依赖组件 | 不循环依赖 |

---

## 23.2 XjtuIronConcurrencyPropertiesBindingTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| BOOT-PROP-001 | `shouldBindThreadPoolProperties` | P0 | 配置 thread-pools | 绑定正确 |
| BOOT-PROP-002 | `shouldBindPipelineProperties` | P1 | 配置 pipeline | 绑定正确 |
| BOOT-PROP-003 | `shouldBindFallbackExecutorProperties` | P1 | 配置 fallbackExecutor | 绑定正确 |
| BOOT-PROP-004 | `shouldBindRegistryProperties` | P1 | 配置 registry | 绑定正确 |
| BOOT-PROP-005 | `shouldUseDefaultValuesWhenPropertiesMissing` | P1 | 缺省配置 | 默认值正确 |
| BOOT-PROP-006 | `shouldRejectInvalidDuration` | P1 | 非法 Duration | 启动失败或绑定失败 |
| BOOT-PROP-007 | `shouldBindRejectionWaitTime` | P1 | BLOCKING_WAIT waitTime | 正确绑定 |
| BOOT-PROP-008 | `shouldBindShutdownAwaitTermination` | P1 | awaitTermination | 正确绑定 |

---

## 23.3 CustomExtensionBeanTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| BOOT-EXT-001 | `customListenerShouldBeInvoked` | P1 | 用户 listener | 任务完成后被调用 |
| BOOT-EXT-002 | `customErrorRuleShouldClassifyException` | P1 | 用户 error rule | 分类结果来自用户规则 |
| BOOT-EXT-003 | `customTaskDecoratorShouldPropagateContext` | P1 | MDC/traceId | 工作线程拿到上下文 |
| BOOT-EXT-004 | `customUncaughtExceptionHandlerShouldBeInvoked` | P1 | execute 失败 | handler 被调用 |
| BOOT-EXT-005 | `customInternalDiagnosticsShouldRecordSideEffectError` | P1 | listener 抛异常 | diagnostics 记录 |

---

## 23.4 InvalidConfigurationTest

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| BOOT-INV-001 | `zeroQueueCapacityForBoundedQueueShouldFail` | P0 | 有界队列容量 0 | 启动失败 |
| BOOT-INV-002 | `maxLessThanCoreShouldFail` | P0 | max < core | 启动失败 |
| BOOT-INV-003 | `blockingWaitWithoutWaitTimeShouldFail` | P0 | BLOCKING_WAIT 未配置等待 | 启动失败 |
| BOOT-INV-004 | `allowCoreTimeoutWithoutKeepAliveShouldFail` | P0 | core timeout + keepAlive 非法 | 启动失败 |
| BOOT-INV-005 | `duplicateThreadPoolNameShouldFail` | P1 | 重复线程池名 | 启动失败或配置合并按设计断言 |
| BOOT-INV-006 | `unknownQueueTypeShouldFail` | P1 | 未知 queueType | 启动失败 |
| BOOT-INV-007 | `unknownRejectionPolicyShouldFail` | P1 | 未知 rejectionPolicy | 启动失败 |

---

# 24. Controller 冒烟测试

Controller 只做少量端到端验证，不承担全分支覆盖。

| ID | 接口 | 优先级 | 覆盖目标 |
|---|---|---:|---|
| DEMO-001 | `/demo/basic/success` | P2 | 正常成功 |
| DEMO-002 | `/demo/basic/failure` | P2 | 正常失败 |
| DEMO-003 | `/demo/timeout/result-timeout` | P2 | 结果超时 |
| DEMO-004 | `/demo/timeout/queue-timeout` | P2 | 排队超时 |
| DEMO-005 | `/demo/fallback/success` | P2 | fallback 成功 |
| DEMO-006 | `/demo/fallback/failure` | P2 | fallback 失败 |
| DEMO-007 | `/demo/rejection/abort` | P2 | ABORT 拒绝 |
| DEMO-008 | `/demo/rejection/caller-runs` | P2 | CALLER_RUNS |
| DEMO-009 | `/demo/rejection/discard` | P2 | DISCARD |
| DEMO-010 | `/demo/cancel/queued` | P2 | 队列中取消 |
| DEMO-011 | `/demo/cancel/running` | P2 | 运行中取消 |
| DEMO-012 | `/demo/thread-pool/snapshot` | P2 | 线程池快照 |
| DEMO-013 | `/demo/registry/recent` | P2 | 最近任务查询 |
| DEMO-014 | `/demo/async-template/all-of` | P2 | AsyncTemplate allOf |
| DEMO-015 | `/demo/async-template/any-success` | P2 | AsyncTemplate anySuccess |

---

# 25. 并发竞争专项测试

## 25.1 timeout vs success

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| RACE-001 | `successAndTimeoutRaceShouldResolveOnlyOnce` | P0 | success 与 timeout 同时抢 base outcome | 只能一个终态 |
| RACE-002 | `timeoutLosesShouldNotInterruptRunningThread` | P0 | success 先赢，timeout 后执行 | 不 interrupt |
| RACE-003 | `timeoutWinsShouldInterruptWhenConfigured` | P0 | timeout 赢 | interrupt |
| RACE-004 | `eventsShouldNotContainBothSuccessAndTimeout` | P0 | 竞争 | 事件不能同时有 SUCCESS 和 TIMEOUT |
| RACE-005 | `finalFutureShouldNotRemainPendingAfterRace` | P0 | 竞争 | finalFuture 必须完成 |

---

## 25.2 cancel vs running

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| RACE-006 | `cancelAndSuccessRaceShouldResolveOnlyOnce` | P0 | cancel/success 并发 | 只能一个赢 |
| RACE-007 | `cancelAndFailureRaceShouldResolveOnlyOnce` | P0 | cancel/failure 并发 | 只能一个赢 |
| RACE-008 | `cancelShouldNotTriggerFallbackWhenItWins` | P0 | cancel 赢 | 不 fallback |
| RACE-009 | `successShouldNotBecomeCancelledWhenAlreadyCompleted` | P0 | success 后 cancel | ALREADY_COMPLETED |
| RACE-010 | `finalFutureShouldNotRemainPendingAfterCancelRace` | P0 | cancel 竞争 | finalFuture 完成 |

---

## 25.3 reject vs cancel

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| RACE-011 | `rejectAndCancelRaceShouldResolveOnlyOnce` | P0 | reject/cancel 并发 | 只能一个赢 |
| RACE-012 | `rejectedTaskCancelShouldReturnConsistentResult` | P1 | rejected 后 cancel | ALREADY_COMPLETED 或按设计 |
| RACE-013 | `rejectShouldNotLeaveControlRegistryLeaked` | P0 | 拒绝任务 | control 移除 |
| RACE-014 | `finalFutureShouldNotRemainPendingAfterRejectCancelRace` | P0 | 竞争 | finalFuture 完成 |

---

## 25.4 fallback vs cancel

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| RACE-015 | `cancelDuringFallbackShouldResolveOnlyOnce` | P0 | fallback 执行中 cancel | 只能一个最终结果 |
| RACE-016 | `cancelFallbackShouldInterruptFallbackThreadWhenConfigured` | P1 | cancel fallback | interrupt fallbackThread |
| RACE-017 | `fallbackSuccessAfterCancelShouldNotCompleteFinalFuture` | P0 | cancel 已赢，fallback 后完成 | 不能重新 success |
| RACE-018 | `fallbackFailureAfterCancelShouldNotOverrideCancelled` | P0 | cancel 已赢，fallback 后失败 | 不能覆盖 CANCELLED |
| RACE-019 | `finalFutureShouldNotRemainPendingAfterFallbackCancelRace` | P0 | 竞争 | finalFuture 完成 |

---

## 25.5 Registry 并发

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| RACE-020 | `concurrentRegistryUpdateShouldKeepLatestVersion` | P0 | 多线程 update 同 taskId | 保留最新 |
| RACE-021 | `concurrentRecentAndUpdateShouldNotThrow` | P1 | recent 与 update 并发 | 不抛异常 |
| RACE-022 | `concurrentEvictShouldNotLoseNewSnapshot` | P1 | 淘汰与新 update 并发 | 不丢最新 |
| RACE-023 | `concurrentRemoveOldControlShouldNotRemoveNewControl` | P1 | remove 旧 control，新 control 已注册 | 不误删 |

---

# 26. shutdown 生命周期测试

## 26.1 业务线程池 shutdown

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| SHUT-001 | `shutdownShouldWaitForRunningTasksWhenConfigured` | P1 | waitForTasksToComplete=true | 等待运行任务 |
| SHUT-002 | `shutdownShouldAbortPendingTasksWhenTimeout` | P0 | await 超时后 shutdownNow | pending command abort |
| SHUT-003 | `shutdownNowShouldCompletePendingCommandAsCancelled` | P0 | pending TaskCommand | CANCELLED，Future 不 pending |
| SHUT-004 | `shutdownNowShouldNotBreakForNonAwareRunnable` | P1 | pending 普通 Runnable | 不崩溃 |
| SHUT-005 | `shutdownShouldBeIdempotent` | P1 | 重复 shutdown | 不重复 abort |

---

## 26.2 fallbackExecutor shutdown

| ID | 用例名 | 优先级 | 场景 | 期望 |
|---|---|---:|---|---|
| SHUT-006 | `fallbackExecutorShutdownShouldWaitForRunningFallback` | P1 | fallback 正在运行 | 按配置等待 |
| SHUT-007 | `fallbackExecutorShutdownNowShouldAbortPendingFallbackTasks` | P0 | pending fallback | FALLBACK_FAILED |
| SHUT-008 | `fallbackPendingAbortShouldCompleteFinalFutureExceptionally` | P0 | pending fallback 被 abort | finalFuture 异常，不 pending |
| SHUT-009 | `fallbackShutdownShouldClassifyAsFallbackStage` | P1 | fallback abort | stage=FALLBACK |
| SHUT-010 | `fallbackShutdownShouldPublishFallbackFailedAndCompleted` | P0 | pending fallback 被 abort | FALLBACK_FAILED + completed |

---

# 27. 测试工具类建议

## 27.1 RecordingTaskExecutionListener

用于记录事件顺序。

建议能力：

```java
List<TaskExecutionEvent> events();
List<AsyncTaskStatus> statuses();
long count(AsyncTaskStatus status);
boolean contains(AsyncTaskStatus status);
TaskExecutionEvent first(AsyncTaskStatus status);
```

---

## 27.2 RecordingTaskLifecyclePublisher

用于主链路测试，不想引入真实 Metrics/Registry 时记录事件。

建议能力：

```java
publish(event)
publishCompleted(event)
events()
completedEvents()
```

---

## 27.3 TestAsyncErrorClassifier

建议能力：

```java
固定返回 AsyncError
记录 classify 调用次数
记录 stage
支持 classify 抛异常
```

---

## 27.4 TestTaskFactory

建议能力：

```java
successTask(value)
failureTask(exception)
blockingTask(startedLatch, releaseLatch)
neverCompleteTask()
timeoutTask(timeout)
fallbackTask(fallbackValue)
cancelAwareTask()
```

---

## 27.5 TestExecutors

建议能力：

```java
singleThreadExecutor()
singleThreadExecutorWithQueue(size)
saturatedExecutor()
shutdownExecutor()
directExecutor()
manualScheduledExecutor()
fallbackExecutorWithQueue(size)
```

---

## 27.6 AwaitTestSupport

建议能力：

```java
awaitFutureDone(future)
awaitStatus(registry, taskId, status)
awaitEvent(listener, status)
assertFutureNotPending(future)
```

---

# 28. 测试写法规范

## 28.1 Future 等待必须带超时

不要写：

```java
future.get();
```

应该写：

```java
future.get(1, TimeUnit.SECONDS);
```

避免测试永久卡死。

---

## 28.2 少用 Thread.sleep

不推荐：

```java
Thread.sleep(3000);
```

推荐：

```java
CountDownLatch latch = new CountDownLatch(1);
assertTrue(latch.await(1, TimeUnit.SECONDS));
```

---

## 28.3 并发竞争测试不要强行指定谁赢

例如 SUCCESS/TIMEOUT 竞争，不要断言必须 SUCCESS 赢或 TIMEOUT 赢。

应该断言：

```text
只能一个赢
事件不能矛盾
Future 不能 pending
状态不能倒退
```

---

## 28.4 每个终态都要验证 Future 收口

必须覆盖：

```text
SUCCESS
FAILED
TIMEOUT
REJECTED
CANCELLED
FALLBACK_SUCCESS
FALLBACK_FAILED
```

每个终态都要断言：

```text
baseFuture 或 finalFuture 不 pending
状态正确
事件正确
Registry 正确
```

---

## 28.5 事件顺序要断言

典型顺序：

```text
成功：
SUBMITTED -> RUNNING -> SUCCESS -> COMPLETED

失败 fallback 成功：
SUBMITTED -> RUNNING -> FAILED -> FALLBACK -> FALLBACK_SUCCESS -> COMPLETED

超时 fallback 失败：
SUBMITTED -> RUNNING -> TIMEOUT -> FALLBACK -> FALLBACK_FAILED -> COMPLETED

拒绝：
SUBMITTED -> REJECTED -> COMPLETED

取消：
SUBMITTED -> CANCELLED -> COMPLETED
```

---

# 29. 覆盖率目标

建议目标：

```text
核心模块行覆盖率 >= 80%
核心模块分支覆盖率 >= 70%

状态机相关分支接近 100%
拒绝策略相关分支接近 100%
timeout/fallback/cancel 相关分支接近 100%
```

不要盲目追求全项目 100%。  
但是下面这些必须尽可能全覆盖：

```text
TaskExecutionRuntime
TaskCommand
DefaultTaskResultPipeline
DefaultTaskExecutionTemplate
RejectedExecutionHandler
TaskControl / TaskCancellationManager
```

---

# 30. 推进顺序

建议按下面顺序落地。

## 第一阶段：测试骨架

```text
1. 创建测试目录
2. 创建 testfixture
3. 创建所有测试类空壳
4. 按本文复制测试方法名
```

---

## 第二阶段：状态机和快照

```text
1. TaskExecutionRuntimeStateMachineTest
2. TaskDefinitionSnapshotTest
3. TaskExecutionContextTest
```

---

## 第三阶段：主链路

```text
1. DefaultTaskExecutionTemplateTest
2. TaskCommandTest
3. DefaultTaskResultPipelineTest
```

---

## 第四阶段：拒绝和取消

```text
1. 五种拒绝策略测试
2. TaskControlTest
3. TaskCancellationManagerTest
4. TaskControlCancellationIntegrationTest
```

---

## 第五阶段：旁路能力

```text
1. DefaultTaskLifecyclePublisherTest
2. CompositeTaskExecutionListenerTest
3. CompositeAsyncErrorClassifierTest
4. DefaultTaskExecutionRegistryTest
```

---

## 第六阶段：Spring Boot 和 Demo

```text
1. AutoConfigurationTest
2. PropertiesBindingTest
3. CustomExtensionBeanTest
4. InvalidConfigurationTest
5. Controller Smoke Test
```

---

# 31. 进入二期前验收标准

进入二期前，建议满足：

```text
1. P0 测试全部完成。
2. P1 测试完成 80% 以上。
3. 所有拒绝策略均有测试。
4. 所有终态 Future 均验证不 pending。
5. timeout/success 竞争有测试。
6. cancel/running 竞争有测试。
7. fallback/shutdownNow 有测试。
8. Listener / ErrorClassifier 异常隔离有测试。
9. Spring Boot 默认配置能启动。
10. 文档中 Public API 均有对应测试。
```

---

# 32. 最终建议

本测试用例清单可以作为二期开始前的第一份基线文档。

建议文件名：

```text
docs/TEST_CASES_PHASE1.md
```

原来的 `TESTING.md` 继续作为测试方法论文档。  
本文专门作为“一期测试用例全集”。

推荐现在先执行：

```text
1. 按本文创建测试类骨架。
2. 先实现 P0。
3. 再实现 P1。
4. Controller 最后做冒烟。
5. 测试补齐后再进入动态线程池治理二期。
```

# TESTING：测试指南

## 本文适合谁看

适合组件维护者和希望验证并行组件正确性的人。

## 读完你会知道什么

- 并行组件为什么必须重视测试。
- 测试应该分哪几层。
- 每个核心分支应该怎么覆盖。
- 为什么不要依赖大量 `Thread.sleep`。
- 如何测试状态机、拒绝策略、timeout、fallback、cancel、shutdown。

## 目录

- [1. 测试分层](#1-测试分层)
- [2. 测试工具建议](#2-测试工具建议)
- [3. 状态机测试](#3-状态机测试)
- [4. 拒绝策略测试](#4-拒绝策略测试)
- [5. timeout 测试](#5-timeout-测试)
- [6. fallback 测试](#6-fallback-测试)
- [7. cancel 测试](#7-cancel-测试)
- [8. shutdown 测试](#8-shutdown-测试)
- [9. Registry 测试](#9-registry-测试)
- [10. Spring 自动装配测试](#10-spring-自动装配测试)
- [11. Controller 冒烟测试](#11-controller-冒烟测试)
- [12. 测试清单](#12-测试清单)

## 1. 测试分层

```text
第一层：core 单元测试
  不启动 Spring，直接测试状态机、Pipeline、拒绝策略。

第二层：Spring 自动装配测试
  使用 ApplicationContextRunner 或 SpringBootTest 验证 Bean 和配置。

第三层：Controller 冒烟测试
  启动 Demo，人工或自动调用 HTTP 接口。

第四层：压测
  验证队列、拒绝、超时、指标和吞吐。
```

不要一开始就用 Controller 测所有并发逻辑。Controller 测试失败时，很难判断是 HTTP 问题、Spring 问题、线程调度问题还是核心逻辑问题。

## 2. 测试工具建议

推荐：

```text
JUnit 5
AssertJ
CountDownLatch
CyclicBarrier
Phaser
CompletableFuture.get(timeout)
Awaitility，可选
```

不推荐大量依赖：

```java
Thread.sleep(3000);
```

`Thread.sleep` 会让测试慢，而且容易因为机器负载产生偶发失败。

## 3. 状态机测试

覆盖：

```text
CREATED → SUBMITTED
SUBMITTED → RUNNING
RUNNING → SUCCESS
RUNNING → FAILED
RUNNING → TIMEOUT
RUNNING → CANCELLED
FAILED → FALLBACK → FALLBACK_SUCCESS
FAILED → FALLBACK → FALLBACK_FAILED
终态不能回退
```

示例：

```java
@Test
void terminalStatusShouldNotRollbackToSubmitted() {
    TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

    assertThat(runtime.tryCancel()).isTrue();
    assertThat(runtime.getStatus()).isEqualTo(AsyncTaskStatus.CANCELLED);

    assertThat(runtime.tryMarkSubmitted()).isFalse();
    assertThat(runtime.getStatus()).isEqualTo(AsyncTaskStatus.CANCELLED);
}
```

## 4. 拒绝策略测试

每种拒绝策略都要验证：

```text
Future 是否完成
状态是否正确
同步提交方是否抛异常
Listener 是否收到 REJECTED
Registry 是否更新
```

### ABORT

```text
当前任务 REJECTED
Future 异常完成
execute 同步抛异常
```

### CALLER_RUNS

```text
线程池运行中：任务由提交线程执行，executionMode=CALLER_THREAD。
线程池关闭后：任务 REJECTED。
```

### DISCARD

```text
任务不执行。
Future 异常完成。
提交方法不一定同步抛异常。
```

### DISCARD_OLDEST

```text
旧任务 REJECTED。
新任务尝试入队。
如果新任务无法入队，新任务也 REJECTED。
```

### BLOCKING_WAIT

```text
等待成功：任务入队。
等待超时：REJECTED。
等待中断：恢复中断标记并 REJECTED。
等待期间 shutdown：REJECTED。
```

## 5. timeout 测试

覆盖：

```text
任务超时后进入 TIMEOUT
timeout 触发 fallback
timeout 后 cancelOnTimeout 中断运行线程
任务先成功时 timeoutFuture 被取消
超时和成功竞争时只有一个结果获胜
```

## 6. fallback 测试

覆盖：

```text
FAILED + fallback success
FAILED + fallback failed
TIMEOUT + fallback success
REJECTED + fallback success
fallbackExecutor 拒绝
fallbackExecutor shutdownNow pending task
```

fallbackExecutor shutdown 测试关键：

```text
让 fallback 任务进入 fallbackExecutor 队列但不执行。
调用 shutdownNow。
处理 pending FallbackTask。
验证 finalFuture 异常完成且状态为 FALLBACK_FAILED。
```

## 7. cancel 测试

覆盖：

```text
队列中取消
运行中取消
fallback 中取消
完成后取消返回 ALREADY_COMPLETED
找不到任务返回 NOT_FOUND
cancel(true) 尝试 interrupt
```

## 8. shutdown 测试

覆盖：

```text
业务线程池 shutdownNow 返回 pending TaskCommand
pending TaskCommand 被 abortOnShutdown
状态变为 CANCELLED
Future 不 pending
```

## 9. Registry 测试

覆盖：

```text
update 后 get 能查到最新快照
recent 返回最近任务
超过 maxSize 后淘汰旧任务
并发 update 不丢最新快照
publishCompleted 不重复写 Registry
```

## 10. Spring 自动装配测试

建议使用：

```java
ApplicationContextRunner
```

验证：

```text
默认 Bean 能创建
配置能绑定
自定义 Bean 能覆盖
非法配置启动失败
多个 Listener 能组合
Controller 中定义 Listener 不会造成循环依赖
```

## 11. Controller 冒烟测试

Controller 只用于最后验证：

```text
接口是否正常返回 JSON
线程名是否符合预期
任务状态是否可查询
管理接口是否可用
Actuator 指标是否暴露
```

## 12. 测试清单

最低覆盖清单：

```text
TaskExecutionRuntimeStateMachineTest
TaskDefinitionSnapshotTest
DefaultAsyncTemplateTest
RejectedExecutionHandlersTest
DefaultTaskResultPipelineTest
DefaultTaskExecutionRegistryTest
ThreadPoolInfrastructureTest
AsyncErrorClassifierTest
DefaultTaskLifecyclePublisherTest
DefaultTaskExecutionTemplateTest
SpringAutoConfigurationTest
```

# FAQ：常见问题

## 本文适合谁看

适合使用并行组件过程中遇到疑问的人。

## 目录

- [1. 为什么不用原生 CompletableFuture](#1-为什么不用原生-completablefuture)
- [2. CALLER_RUNS 为什么不是 REJECTED](#2-caller_runs-为什么不是-rejected)
- [3. DISCARD 为什么还会让 Future 异常完成](#3-discard-为什么还会让-future-异常完成)
- [4. timeout 和 queueTimeout 区别](#4-timeout-和-queuetimeout-区别)
- [5. cancel(true) 能强制停止线程吗](#5-canceltrue-能强制停止线程吗)
- [6. fallback 在哪个线程执行](#6-fallback-在哪个线程执行)
- [7. 为什么不用 future.orTimeout](#7-为什么不用-futureortimeout)
- [8. 为什么需要 TaskDefinition](#8-为什么需要-taskdefinition)
- [9. Listener 抛异常会不会导致任务失败](#9-listener-抛异常会不会导致任务失败)
- [10. shutdownNow 为什么要处理返回值](#10-shutdownnow-为什么要处理返回值)

## 1. 为什么不用原生 CompletableFuture

原生 `CompletableFuture` 能完成异步编排，但不负责：

```text
线程池治理
拒绝感知
任务状态
统一错误分类
生命周期监听
Micrometer 指标
任务取消与注册表
```

并行组件是在 `CompletableFuture` 之上补齐生产治理能力。

## 2. CALLER_RUNS 为什么不是 REJECTED

`CALLER_RUNS` 在线程池满但未关闭时，会由提交线程直接执行任务。

任务最终确实执行了，所以状态应是：

```text
SUCCESS / FAILED
```

执行模式是：

```text
CALLER_THREAD
```

不是：

```text
REJECTED
```

## 3. DISCARD 为什么还会让 Future 异常完成

JDK 原生 DISCARD 会静默丢弃任务。

但组件不能这么做，因为业务可能持有：

```java
CompletableFuture<T> future
```

如果任务被静默丢弃，Future 可能永远不完成。

所以组件里的 DISCARD 是拒绝感知版：

```text
不执行任务，但明确完成 Future 为异常，并更新状态为 REJECTED。
```

## 4. timeout 和 queueTimeout 区别

```text
queueTimeout：任务排队太久，开始执行前检查。
timeout：结果太久没返回，由 ResultPipeline 检查。
```

## 5. cancel(true) 能强制停止线程吗

不能。

Java 的 interrupt 是协作式机制。它只是给线程设置中断标记，或者打断可中断阻塞方法。

业务代码需要自己响应：

```java
if (Thread.currentThread().isInterrupted()) {
    return;
}
```

远程调用也要配置自己的超时。

## 6. fallback 在哪个线程执行

fallback 在独立的：

```text
ironConcurrencyFallbackExecutor
```

中执行。

这样可以避免 fallback 阻塞原始业务线程或 timeoutScheduler 线程。

## 7. 为什么不用 future.orTimeout

`CompletableFuture.orTimeout` 会修改原始 Future 自身。

组件需要非侵入式包装：

```text
原始 baseFuture 表示原始任务结果。
包装 finalFuture 表示 timeout/fallback 后最终结果。
```

否则 timeout 可能绕过 `TaskCommand.completeTimeout`，导致事件、指标、错误分类丢失。

## 8. 为什么需要 TaskDefinition

`AsyncTask` 是业务创建的可变对象。

提交后如果业务继续修改 timeout、fallback、operation，可能影响正在执行的任务。

所以提交时生成不可变快照：

```java
TaskDefinition.from(task)
```

后续主链路只读取 `TaskDefinition`。

## 9. Listener 抛异常会不会导致任务失败

不会。

监听器是旁路能力，不能影响主任务结果。

但组件应记录内部诊断日志或指标，避免监听器失效后完全静默。

## 10. shutdownNow 为什么要处理返回值

`shutdownNow()` 会返回队列中尚未开始执行的任务。

如果忽略这些任务：

```text
它们不会 run
不会 reject
不会 cancel
Future 可能永远 pending
```

所以必须遍历返回值并通知任务收口。

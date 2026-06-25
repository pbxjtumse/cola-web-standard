# DESIGN：总体设计思想

## 本文适合谁看

适合想理解“为什么并行组件要这样设计”的人。

## 读完你会知道什么

- 为什么不能简单封装一个线程池。
- 为什么需要任务模型、状态机、错误模型和生命周期事件。
- 为什么要把使用接口和内部接口分层。
- 当前设计的边界在哪里。

## 目录

- [1. 设计目标](#1-设计目标)
- [2. 非目标](#2-非目标)
- [3. 核心原则](#3-核心原则)
- [4. 业务接口与内部接口分离](#4-业务接口与内部接口分离)
- [5. 任务必须可观测](#5-任务必须可观测)
- [6. 拒绝必须可感知](#6-拒绝必须可感知)
- [7. 超时和 fallback 必须统一](#7-超时和-fallback-必须统一)
- [8. 状态机必须封闭](#8-状态机必须封闭)
- [9. 设计取舍](#9-设计取舍)

## 1. 设计目标

```text
让业务开发少写线程池细节。
让异步任务的状态、失败、超时、拒绝、取消都能被看见。
让组件在高并发和异常场景下不会让 Future 永远 pending。
让后续线程池治理、重试补偿、分布式任务控制有基础。
```

## 2. 非目标

一期不解决：

```text
跨 JVM 分布式调度
分布式锁
复杂补偿状态机
全局任务持久化
可视化控制台
```

## 3. 核心原则

| 原则 | 说明 |
|---|---|
| 简单使用 | 业务只通过 AsyncExecutor 提交任务 |
| 任务可追踪 | 每个任务有 taskId、状态、耗时、错误 |
| 拒绝可感知 | 被拒绝的任务也必须完成 Future |
| 状态不回退 | 终态不能被后续事件覆盖 |
| 旁路不影响主链路 | Listener、Metrics 异常不能导致任务失败 |
| 内部职责清晰 | Runtime 管状态，Command 管执行，Pipeline 管结果 |

## 4. 业务接口与内部接口分离

业务接口：

```text
AsyncExecutor
AsyncTask
TaskHandle
AsyncTemplate
TaskExecutionListener
AsyncErrorClassificationRule
```

内部接口：

```text
TaskDefinition
TaskExecutionContext
TaskExecutionRuntime
TaskCommand
TaskResultPipeline
RejectedTaskSupport
```

分离的好处：

```text
业务使用简单。
内部可以演进。
避免业务误操作状态机。
```

## 5. 任务必须可观测

一个异步任务如果失败，至少要能回答：

```text
哪个任务失败？
在哪个线程池？
什么时候提交？
排队多久？
运行多久？
失败阶段是什么？
是否触发 fallback？
最终状态是什么？
```

所以需要：

```text
TaskExecutionEvent
TaskExecutionSnapshot
TaskExecutionRegistry
Micrometer Metrics
AsyncError
```

## 6. 拒绝必须可感知

如果线程池拒绝任务，不能只抛异常或静默丢弃。

必须统一收口：

```text
状态 REJECTED
Future 异常完成
Registry 更新
Listener 通知
Metrics 记录
```

## 7. 超时和 fallback 必须统一

如果每个业务自己写：

```java
future.orTimeout(...).exceptionally(...)
```

会导致：

```text
状态不统一
错误分类不统一
fallback 线程不统一
指标缺失
排障困难
```

所以由 `TaskResultPipeline` 统一处理。

## 8. 状态机必须封闭

任务状态不能随便 `status.set(...)`。

必须通过：

```text
tryMarkSubmitted
tryMarkRunning
tryResolveBaseOutcome
tryMarkFallback
tryFinalize
tryCancel
```

保证非法状态转换失败。

## 9. 设计取舍

### 9.1 为什么 fallback 用独立线程池

避免 fallback 阻塞业务线程或 timeoutScheduler。

### 9.2 为什么 Registry 用内存

一期目标是本地 JVM 可观测。分布式持久化放到后续。

### 9.3 为什么错误分类传 TaskMetadata

因为 `AsyncTask` 可变，提交后应使用稳定快照。

### 9.4 为什么取消是协作式

Java 无法安全强杀线程，只能 interrupt。

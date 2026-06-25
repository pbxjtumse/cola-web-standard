# concurrency-component

> XJTU Iron 并行组件：面向 Java / Spring Boot 业务系统的统一异步执行、线程池治理、超时控制、fallback、取消、拒绝感知、状态追踪与可观测组件。

## 本文适合谁看

- 第一次接入并行组件的业务开发。
- 想快速知道组件能做什么、怎么配置、怎么调用的人。
- 不想先阅读内部实现，只想先把组件跑起来的人。

## 读完你会知道什么

- 并行组件解决什么问题。
- 项目有哪些模块。
- 最小配置怎么写。
- 第一个异步任务怎么提交。
- 更详细的设计、状态机、拒绝策略、测试文档应该去哪里看。

## 目录

- [1. 一句话理解](#1-一句话理解)
- [2. 组件解决什么问题](#2-组件解决什么问题)
- [3. 适用场景](#3-适用场景)
- [4. 模块结构](#4-模块结构)
- [5. 快速开始](#5-快速开始)
- [6. 最小配置](#6-最小配置)
- [7. 最小代码示例](#7-最小代码示例)
- [8. 当前能力清单](#8-当前能力清单)
- [9. 文档导航](#9-文档导航)
- [10. 推荐阅读顺序](#10-推荐阅读顺序)

## 1. 一句话理解

并行组件把业务里常见的异步执行能力统一封装起来：

```text
业务代码只关心：我要执行什么任务、用哪个线程池、超时多久、失败后怎么降级。
组件统一负责：线程池、拒绝、超时、fallback、取消、状态、指标、监听器和错误分类。
```

最小使用方式如下：

```java
CompletableFuture<UserDTO> future = asyncExecutor.submit(
        AsyncTask.of(
                "biz-query-pool",
                "queryUser",
                () -> userService.query(userId)
        )
        .timeout(Duration.ofSeconds(2))
        .fallback(error -> UserDTO.empty(userId))
);
```

## 2. 组件解决什么问题

在业务系统中直接使用 `CompletableFuture`、`ThreadPoolExecutor` 容易出现这些问题：

| 常见问题 | 直接写线程池的风险 | 组件提供的能力 |
|---|---|---|
| 线程池分散 | 每个业务自己 new，难治理 | 统一配置、注册、查询、调整 |
| 拒绝不可见 | 任务被拒绝后 Future 可能挂住 | 拒绝感知，状态变为 REJECTED |
| 超时混乱 | `orTimeout` 可能修改原始 Future | 结果层超时统一进入 TIMEOUT |
| 降级不统一 | 每个业务自己 catch | fallback 统一管道 |
| 任务不可查 | 只知道 Future，不知道 taskId | TaskExecutionRegistry 查询快照 |
| 错误不可分类 | 只看到 Throwable | AsyncError 结构化错误模型 |
| 监听分散 | 任务完成后各写各的 | TaskExecutionListener 生命周期事件 |
| 指标缺失 | 线程池和任务状态不可观测 | Micrometer 指标 |
| 取消不完整 | Future.cancel 不一定同步状态 | TaskHandle / cancel(taskId) |

## 3. 适用场景

适合：

- 多个远程接口并行查询。
- 批处理任务中对每个分片并行处理。
- 营销、清结算、账务、风控等系统中的异步解耦。
- 希望统一治理线程池、拒绝策略、超时、fallback 的后端系统。
- 需要通过 taskId 查询任务状态、失败原因和耗时的场景。

不适合：

- 极短、极简单、完全同步的本地方法调用。
- 需要跨 JVM 分布式调度的任务，当前一期主要是本地 JVM 并行组件。
- 需要强一致分布式锁的场景，分布式锁应作为独立组件设计。

## 4. 模块结构

```text
concurrency-component/
├── concurrency-api
│   └── 对业务暴露的 API、事件、状态、错误模型、监听器接口
├── concurrency-core
│   └── 核心执行引擎、TaskCommand、Pipeline、线程池注册表、拒绝策略
├── concurrency-config
│   └── 配置模型、ThreadPoolSpec、属性绑定对象
├── concurrency-spring-boot-starter
│   └── Spring Boot 自动装配、指标、管理接口
├── concurrency-integrations
│   └── 上下文传播、治理、可观测扩展
└── concurrency-demo
    └── Demo Controller 和验证接口
```

## 5. 快速开始

### 5.1 引入依赖

业务系统通常只需要引入 starter：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>concurrency-spring-boot-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

如果只是写不依赖 Spring 的单元测试，可以引入：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>concurrency-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

## 6. 最小配置

```yaml
xjtu:
  iron:
    concurrency:
      enabled: true
      thread-pools:
        default:
          core-pool-size: 4
          max-pool-size: 8
          queue-capacity: 200
          keep-alive-time: 60s
          thread-name-prefix: default-async-
          queue-type: BOUNDED_ARRAY_BLOCKING_QUEUE
          rejection-policy: ABORT
          wait-for-tasks-to-complete-on-shutdown: true
          await-termination: 10s

      pipeline:
        timeout-scheduler-size: 1
        timeout-thread-name-prefix: iron-concurrency-timeout-
        timeout-daemon: true

        fallback-core-pool-size: 2
        fallback-max-pool-size: 8
        fallback-queue-capacity: 1024
        fallback-keep-alive-time: 60s
        fallback-await-termination: 5s
        fallback-thread-name-prefix: iron-concurrency-fallback-
        fallback-daemon: true
        fallback-rejection-policy: ABORT
```

## 7. 最小代码示例

### 7.1 无返回值异步任务

```java
asyncExecutor.execute(
        "default",
        "sendAuditLog",
        () -> auditService.send(log)
);
```

### 7.2 有返回值异步任务

```java
CompletableFuture<UserDTO> future = asyncExecutor.supply(
        "default",
        "queryUser",
        () -> userService.query(userId)
);

UserDTO user = future.join();
```

### 7.3 完整任务模型

```java
CompletableFuture<UserDTO> future = asyncExecutor.submit(
        AsyncTask.of(
                "default",
                "queryUserProfile",
                () -> userService.queryProfile(userId)
        )
        .taskId("query-user-" + userId)
        .bizKey("userId=" + userId)
        .description("查询用户画像")
        .tag("scene", "user-profile")
        .timeout(Duration.ofSeconds(2))
        .queueTimeout(Duration.ofMillis(500))
        .cancelOnTimeout(true)
        .interruptOnTimeout(true)
        .fallback(error -> UserDTO.empty(userId))
);
```

### 7.4 可取消任务

```java
TaskHandle<String> handle = asyncExecutor.submitHandle(
        AsyncTask.of(
                "default",
                "longTask",
                () -> longTaskService.execute()
        )
);

TaskCancelResult cancelResult = handle.cancel(true);
```

## 8. 当前能力清单

| 能力 | 当前状态 | 说明 |
|---|---|---|
| 统一异步提交 | 已支持 | run、supply、submit、execute、tryExecute |
| 线程池配置 | 已支持 | 多线程池、队列类型、拒绝策略 |
| 拒绝感知 | 已支持 | Future 不会因为拒绝而永久 pending |
| 结果超时 | 已支持 | timeout 后进入 TIMEOUT，并可触发 fallback |
| 排队超时 | 已支持 | queueTimeout 防止任务长期排队 |
| fallback | 已支持 | 原任务失败、超时、拒绝后可恢复 |
| 主动取消 | 已支持 | TaskHandle.cancel / asyncExecutor.cancel(taskId) |
| 执行模式 | 已支持 | THREAD_POOL / CALLER_THREAD |
| 生命周期监听 | 已支持 | submitted、running、success、failed 等 |
| 错误分类 | 已支持 | AsyncErrorClassifier + 规则链 |
| 任务快照 | 已支持 | TaskExecutionRegistry 查询 |
| Micrometer 指标 | 已支持 | 任务数、耗时、状态、线程池指标 |
| Spring Boot 自动装配 | 已支持 | starter 默认装配 |

## 9. 文档导航

| 文档 | 适合谁 | 主要内容 |
|---|---|---|
| [docs/USER_GUIDE.md](docs/USER_GUIDE.md) | 业务开发 | 怎么用组件，不讲内部细节 |
| [docs/QUICK_START.md](docs/QUICK_START.md) | 第一次接入者 | 从依赖、配置到第一个任务 |
| [docs/API_GUIDE.md](docs/API_GUIDE.md) | 业务开发、维护者 | Public API、Extension API、Internal SPI 分层说明 |
| [docs/INTERNAL_DESIGN.md](docs/INTERNAL_DESIGN.md) | 组件维护者 | 内部类如何协作，为什么这么设计 |
| [docs/FLOW_AND_STATE.md](docs/FLOW_AND_STATE.md) | 所有人 | 状态机、时序图、成功失败超时拒绝取消流转 |
| [docs/THREAD_POOL_AND_REJECTION.md](docs/THREAD_POOL_AND_REJECTION.md) | 业务开发、维护者 | 线程池配置、五种拒绝策略、shutdown 语义 |
| [docs/TIMEOUT_FALLBACK_CANCEL.md](docs/TIMEOUT_FALLBACK_CANCEL.md) | 业务开发、维护者 | 超时、fallback、取消、fallbackExecutor 生命周期 |
| [docs/OBSERVABILITY_AND_ERROR.md](docs/OBSERVABILITY_AND_ERROR.md) | 排障人员、维护者 | 错误分类、监听器、指标、注册表 |
| [docs/TESTING.md](docs/TESTING.md) | 维护者 | 单元测试、并发测试、集成测试清单 |
| [docs/FAQ.md](docs/FAQ.md) | 所有人 | 常见问题解释 |
| [docs/ROADMAP.md](docs/ROADMAP.md) | 组件负责人 | 后续演进规划 |

## 10. 推荐阅读顺序

### 业务开发

```text
README.md
→ docs/USER_GUIDE.md
→ docs/QUICK_START.md
→ docs/API_GUIDE.md 的 Public API 部分
→ docs/FAQ.md
```

### 组件维护者

```text
README.md
→ docs/FLOW_AND_STATE.md
→ docs/INTERNAL_DESIGN.md
→ docs/THREAD_POOL_AND_REJECTION.md
→ docs/TIMEOUT_FALLBACK_CANCEL.md
→ docs/TESTING.md
```

### 排障人员

```text
README.md
→ docs/FLOW_AND_STATE.md
→ docs/OBSERVABILITY_AND_ERROR.md
→ docs/FAQ.md
```

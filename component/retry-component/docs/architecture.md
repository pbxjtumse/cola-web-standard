# 架构设计

## 一、模块依赖

```text
retry-api
    ↑
retry-core
    ↑
retry-config
    ↑
retry-demo
```

- `retry-api`：只定义稳定的领域协议和不可变模型。
- `retry-core`：实现同步有限重试，不依赖 Spring、Micrometer、消息客户端或数据库。
- `retry-config`：完成 Spring Boot 配置、事件和指标适配。
- `retry-demo`：只做用法演示，不承载核心能力。

## 二、核心对象关系

```text
RetryExecutor
    └── execute(RetryExecution<T>)
            ├── RetryOperation<T>
            ├── RetryPolicy
            ├── attributes
            ├── RetryCancellationToken
            └── optional retryId

DefaultRetryExecutor
    ├── RetryPolicyRegistry
    ├── RetryListener[*]
    ├── RetrySleeper
    ├── RetryClock
    └── RetryIdGenerator

before physical call
    -> RetryContext

physical call completed
    -> RetryAttempt<T>
    -> RetryClassifier
    -> RetryDecision
    -> delay override or BackoffStrategy
    -> RetryDelay
    -> next attempt or terminal RetryResult<T>
```

## 三、逻辑执行与物理尝试

一次逻辑执行只有一个 `retryId`：

```text
RetryExecution retryId=R-001
    ├── RetryAttempt #1
    ├── RetryAttempt #2
    └── RetryAttempt #3
```

这一区分为后续消息重试、任务持久化和可观测性提供稳定语义。

## 四、RetryContext 与 RetryAttempt

### RetryContext

表示“即将进入业务操作”的状态：

- 当前逻辑执行标识；
- 操作名与策略名；
- 当前尝试序号；
- 逻辑开始时间；
- 已消耗和剩余总时长；
- 上一次完成的尝试；
- 不可变上下文属性；
- 协作式取消令牌。

### RetryAttempt

表示“一次业务操作已经完成”的事实快照：

- 尝试开始和结束时间；
- 单次耗时和总耗时；
- 剩余总时长；
- 返回值或异常；
- 操作、策略和重试标识；
- 不可变属性。

二者不能合并。一个是调用前输入，一个是调用后事实。

## 五、决策模型

`RetryDecision` 包含：

```text
type
reason
failureCode
failureCategory
delayOverride
delaySource
```

决策类型：

- `SUCCESS`：当前尝试得到最终成功结果。
- `RETRY`：允许发起下一次物理尝试。
- `STOP`：失败或结果不满足成功条件，但明确不再重试。
- `ABORT`：中断、安全拒绝或非法状态要求立即终止。

只有 `RETRY` 可以携带延迟覆盖。

## 六、异常规则优先级

全局动作优先级固定为：

```text
InterruptedException
    > ABORT
    > STOP
    > RETRY
    > default STOP
```

同一动作内使用最具体异常类型：

```text
IOException 优先于 Exception
SocketTimeoutException 优先于 IOException
```

结果 Predicate 无法计算类型特异性，所以按声明顺序执行。

## 七、不可变与线程安全

- `RetryPolicy` 构建后不可变，可在线程间复用。
- `RetryExecution` 是单次逻辑调用的不可变输入。
- `RetryContext`、`RetryAttempt`、`RetryDecision`、`RetryDelay`、`RetryResult` 和 `RetryEvent` 都是不可变快照。
- `DefaultRetryPolicyRegistry` 使用并发映射表。
- `DefaultRetryExecutor` 自身不保存单次执行的可变状态，可以复用。
- 业务 `RetryOperation` 是否线程安全由调用方负责。

## 八、可替换基础设施

- `RetryClock`：提供墙上时间和单调时间，支持确定性测试。
- `RetrySleeper`：负责同步等待，生产默认使用 `Thread.sleep(Duration)`。
- `RetryIdGenerator`：生成逻辑执行 ID。
- `RetryListener`：接收生命周期事件。

核心不直接创建线程池，不执行异步调度。

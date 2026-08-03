# 包结构设计

## 一、分包原则

本项目不按“接口、实现类、枚举、模型”进行纯技术分包，而按职责域分包。

不采用：

```text
api
├── interfaces
├── models
├── enums
└── utils
```

原因是一个重试决策会被拆到多个互不相关的目录，阅读者需要跨包拼接完整语义。

采用：

```text
api
├── execution
├── policy
├── backoff
├── event
└── exception
```

同一职责域内可以同时存在接口、不可变模型和枚举。例如 `RetryClassifier`、`RetryDecision`、`RetryDecisionType` 和 `RetryFailureCategory` 都属于策略决策语义，应放在 `policy` 中。

## 二、retry-api

### execution

负责一次逻辑重试如何被提交、执行和观察：

- `RetryExecutor`：统一执行入口；
- `RetryExecution`：完整执行请求；
- `RetryOperation`：业务操作；
- `RetryContext`：尝试开始前上下文；
- `RetryAttempt`：物理尝试完成快照；
- `RetryResult`：逻辑执行最终结果；
- `RetryStatus`：最终状态；
- `RetryCancellationToken`：协作式取消；

### policy

负责是否重试以及策略边界：

- `RetryPolicy`；
- `RetryPolicyRegistry`；
- `RetryClassifier`；
- `RetryDecision`；
- `RetryDecisionType`；
- `RetryFailureCategory`；
- `OperationSafety`；
- `RetrySafetyMode`。

包内实现 `RuleBasedRetryClassifier`、`RetryExceptionRule` 和 `RetryResultRule` 不对调用方公开。

### backoff

负责计算下一次等待多久：

- `BackoffStrategy`；
- `BackoffStrategies`；
- `RetryDelay`；
- `RetryDelaySource`。

退避策略只计算时间，不执行 `Thread.sleep`。

### event

负责生命周期观测：

- `RetryEvent`；
- `RetryEventType`；
- `RetryListener`。

### exception

保存调用方选择“失败即抛异常”风格时使用的统一异常。

## 三、retry-core

### executor

`DefaultRetryExecutor` 只负责同步重试状态机。

`RetryEventDispatcher` 负责：

- 构造公共事件字段；
- 按顺序通知监听器；
- 隔离监听器异常。

### policy

`DefaultRetryPolicyRegistry` 是命名策略注册表默认实现。

### id

`retry-core` 不再维护 ID 实现；默认逻辑执行标识由 `foundation-id` 的 UUID v7 生成器提供。

### time

时钟与等待器保持可替换，便于单元测试不真实等待。

## 四、retry-config

### autoconfigure

只负责 Bean 装配和条件判断。

### properties

只负责外部配置绑定、继承解析和核心策略创建。

### observation

只负责 Spring 事件与 Micrometer 适配。

## 五、依赖方向

```text
retry-api
   ↑
retry-core
   ↑
retry-config
   ↑
retry-demo
```

禁止：

- `retry-api` 依赖 Spring；
- `retry-api` 依赖 `retry-core`；
- `retry-core` 依赖 `retry-config`；
- 消息组件直接依赖 `DefaultRetryExecutor`。

消息组件应依赖 `RetryExecutor`、`RetryPolicy` 等公共协议，由应用运行时通过自动配置提供实现。

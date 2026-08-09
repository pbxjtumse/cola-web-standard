# 类职责索引

## retry-api / execution

| 类型 | 职责 | 关键边界 |
|---|---|---|
| `RetryExecutor` | 对外同步执行入口 | 不定义线程池和持久化调度 |
| `RetryExecution<T>` | 一次逻辑执行的不可变请求 | 只保存输入，不保存运行时变化 |
| `RetryOperation<T>` | 调用方业务操作函数 | 可以抛出 `Exception`，执行器不捕获 `Error` |
| `RetryContext` | 每次操作开始前的上下文 | 表示调用前状态 |
| `RetryAttempt<T>` | 已完成物理尝试的事实快照 | 返回值与异常互斥 |
| `RetryResult<T>` | 一次逻辑执行的最终结果 | 状态、值、异常和尝试次数必须一致 |
| `RetryStatus` | 逻辑执行最终状态 | 与 `RetryDecisionType` 分离 |
| `RetryCancellationToken` | 协作式取消信号 | 不强制终止正在运行的同步业务 |

## retry-api / policy

| 类型 | 职责 | 关键边界 |
|---|---|---|
| `RetryPolicy` | 不可变重试规则快照 | 不保存 attempt 等运行状态 |
| `RetryPolicy.Builder` | 构建与校验策略 | 禁止冲突规则和混合分类器来源 |
| `RetryPolicyRegistry` | 命名策略注册协议 | 只定义查找和快照语义 |
| `RetryClassifier` | 将完成的尝试转为决策 | 不能把带异常尝试判定为成功 |
| `RetryDecision` | 成功、重试、停止或中止决定 | 只有 RETRY 可携带延迟覆盖 |
| `RetryDecisionType` | 决策动作枚举 | 不等于最终执行状态 |
| `RetryFailureCategory` | 跨协议统一失败分类 | 协议异常识别应在集成模块完成 |
| `OperationSafety` | 操作副作用安全声明 | 不等于业务幂等实现 |
| `RetrySafetyMode` | ALLOW/WARN/REJECT 风险处理 | REJECT 在策略构建阶段失败 |
| `RuleBasedRetryClassifier` | 声明式规则的包内实现 | 不作为公共 API 暴露 |

## retry-api / backoff

| 类型 | 职责 | 关键边界 |
|---|---|---|
| `BackoffStrategy` | 计算下一次等待 | 只计算，不实际阻塞 |
| `BackoffStrategies` | 常用退避策略工厂 | 不包含线程调度 |
| `RetryDelay` | 等待时长、来源和原因 | 允许显式零等待 |
| `RetryDelaySource` | 固定、指数、抖动、服务端等来源 | 用于事件和指标 |

## retry-api / event 与 exception

| 类型 | 职责 | 关键边界 |
|---|---|---|
| `RetryEvent` | 生命周期事件快照 | 不暴露完整业务载荷 |
| `RetryEventType` | 生命周期节点枚举 | 监听器异常不改变执行结果 |
| `RetryListener` | 事件观察扩展点 | 必须快速执行，不能长时间阻塞 |
| `RetryExecutionException` | `executeAndGet` 的统一失败异常 | 保留完整 `RetryResult` |

## retry-core

| 类型 | 职责 | 关键边界 |
|---|---|---|
| `DefaultRetryExecutor` | 完成同步有限重试状态机 | 不强杀线程、不持久化任务 |
| `RetryEventDispatcher` | 构造事件并隔离监听器异常 | 包内实现，不对业务暴露 |
| `DefaultRetryPolicyRegistry` | 管理命名策略 | `register` 拒绝覆盖，`replace` 显式覆盖 |
| `RetryClock` | 墙上时间和单调时间抽象 | 单调时间用于耗时计算 |
| `SystemRetryClock` | 系统时间实现 | 生产默认实现 |
| `RetrySleeper` | 同步等待抽象 | 测试可记录等待而不真实 sleep |
| `ThreadSleepRetrySleeper` | Java 17 同步休眠实现 | 阻塞调用线程，只适合短重试 |

## retry-config

| 类型 | 职责 | 关键边界 |
|---|---|---|
| `RetryProperties` | 绑定 `xjtu.iron.retry` 配置 | 使用包装类型表达继承三态 |
| `RetryPolicyConfigurationLoader` | 外部配置转核心策略的公开门面 | 隐藏解析器和工厂细节 |
| `ResolvedRetryPolicyProperties` | 完成继承后的配置快照 | 只在 properties 包内部使用 |
| `RetryPolicyPropertiesResolver` | 解析父子策略和循环依赖 | 显式空列表清空父规则 |
| `RetryPolicyFactory` | 将解析配置转为 `RetryPolicy` | 加载异常类型并创建退避策略 |
| `RetryAutoConfiguration` | 装配 Registry、Executor 和基础设施 | 所有关键 Bean 允许用户覆盖 |
| `RetryMetricsAutoConfiguration` | 条件装配 Micrometer 监听器 | 可通过 `metrics-enabled` 关闭 |
| `MicrometerRetryListener` | 将核心事件映射为指标 | 避免高基数标签 |
| `SpringApplicationRetryListener` | 将核心事件桥接到 Spring | 核心模块不依赖 Spring |

## retry-demo

| 类型 | 职责 |
|---|---|
| `RetryDemoApplication` | Spring Boot 演示入口 |
| `RetryDemoController` | 展示异常、结果、服务端等待、取消和不可重试场景 |

## 基础组件依赖

逻辑执行 ID 由 `foundation-id` 的 `StringIdGenerator` 生成，默认算法为 UUID v7。

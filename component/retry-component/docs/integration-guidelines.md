# 与其他基础组件的集成原则

## 一、与消息组件

### 发送链路

```text
MessageTemplate
    └── RetryExecutor
            └── MessageProvider
                    └── Kafka/RocketMQ/Pulsar Client
```

必须同时审查：

- Provider 客户端内部重试次数；
- 外层 `RetryPolicy.maxAttempts`；
- 网络库隐式重试；
- 整体最大持续时间；
- 是否存在未知发送结果；
- 消息 ID 是否可以作为 `retryId` 或幂等键。

不能简单叠加：

```text
客户端 5 次 × 外层 5 次 × 业务 3 次 = 75 次物理请求
```

未知发送结果不能因为抛出超时就直接重发，必须结合消息 ID、Broker 语义或 Outbox 状态判断。

### 消费链路

重试组件可以输出：

- 当前第几次尝试；
- 是否应该继续；
- 失败分类；
- 下一次建议时间；
- 是否已经耗尽。

消息组件负责映射成：

- 当前线程内短重试；
- NACK/reconsume；
- 重试 Topic；
- 延迟消息；
- DLQ；
- 人工处理。

长延迟不能通过消费者线程 `sleep` 实现。

## 二、与幂等组件

推荐重试在外层，幂等在每次真实执行内层：

```java
RetryExecution<Result> execution = RetryExecution.builder(
                "create-order",
                context -> idempotencyExecutor.execute(
                        idempotencyKey,
                        businessOperation
                ),
                retryPolicy
        )
        .retryId(idempotencyKey)
        .build();
```

`IDEMPOTENCY_PROTECTED` 只表示调用方声称已有保护。重试组件不会验证该保护真的存在。

## 三、与事务组件

适合重试：

- 数据库死锁；
- 锁等待超时；
- 可重新读取并计算的乐观锁冲突。

不应自动重试：

- SQL 语法错误；
- 约束定义错误；
- 业务校验失败；
- 提交结果不确定且没有业务唯一键。

事务模板必须位于每次 `RetryOperation` 内部，使失败后完整回滚，再重新开启新事务。

## 四、与并行组件

一期：

- 重试使用调用线程；
- `RetrySleeper` 阻塞调用线程；
- 只适合毫秒到短秒级等待。

二期：

- 使用并行组件提供的受管 Scheduler；
- 传播 Trace、MDC 和业务上下文；
- 调度拒绝要映射为稳定状态；
- 取消要同时撤销未执行调度任务；
- 防止前一次超时任务和下一次尝试并发运行。

## 五、与治理组件

每次物理尝试都应重新经过治理链路。熔断器打开时通常应 STOP，而不是不断重试熔断拒绝异常。

`THROTTLING` 分类可以用于：

- 选择更长退避；
- 消费服务端 Retry-After；
- 通知治理组件降低发送速度。

自适应限流仍属于治理组件，而不是重试核心。

## 六、与可观测组件

建议关联：

```text
traceId
retryId
operationName
policyName
attemptNumber
failureCode
failureCategory
finalStatus
```

禁止默认记录：

- 消息正文；
- 请求完整参数；
- 密码与 Token；
- 用户敏感信息。

`operationName`、`policyName` 和指标标签必须是有限集合，不得使用订单号、消息 ID 等高基数值。

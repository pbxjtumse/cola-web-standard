# 组件边界

## 一、retry-component 负责

- 定义一次逻辑重试执行和多次物理尝试的统一模型。
- 根据异常或返回结果生成 `RetryDecision`。
- 控制 `maxAttempts` 和 `maxDuration`。
- 计算固定、指数、全抖动或动态覆盖等待时间。
- 支持短时间同步等待。
- 在尝试之间响应协作式取消。
- 生成或接收逻辑执行 `retryId`。
- 返回统一的 `RetryResult<T>`。
- 发布同步生命周期事件。
- 管理命名策略不可变快照。
- 提供 Spring Boot 配置、Spring 事件桥接和 Micrometer 指标。
- 对明显非幂等的多次尝试提供 ALLOW/WARN/REJECT 风险检查。

## 二、retry-component 不负责

### 1. 不负责业务幂等

组件不知道订单、扣款、发券或消息消费是否已经完成。`OperationSafety` 只是声明，不会创建幂等表，也不会判断业务状态。

### 2. 不负责事务原子性

组件可以重新调用完整事务模板，但不创建、提交或回滚数据库事务。

### 3. 不负责消息协议动作

ACK、NACK、消费位点、重试主题、延迟消息和死信由消息组件和具体 Provider 负责。

### 4. 不负责治理

限流、熔断、并发隔离和自适应降速属于治理组件。重试组件只可以消费治理层返回的失败分类。

### 5. 不负责线程池

一期同步实现使用调用线程。二期调度必须依赖并行组件提供的受管 Scheduler。

### 6. 不负责业务补偿

退款、冲正、库存回补、Saga 和人工修复不是“再次执行原操作”，不能塞入重试核心。

### 7. 不负责长时间持久化重试

分钟、小时、跨重启和跨节点的任务需要持久化、租约、抢占、心跳和幂等，应与任务组件共同实现。

### 8. 不承诺强制取消同步业务

`RetryCancellationToken` 只能阻止尚未开始的尝试。业务代码已经运行后，组件不会使用不安全的线程强杀。

## 三、推荐组合

### 幂等

```java
retryExecutor.execute(
        "create-order",
        context -> idempotencyExecutor.execute(idempotencyKey, operation),
        retryPolicy
);
```

每一次尝试都重新进入幂等判断。

### 事务

```java
retryExecutor.execute(
        "settlement-transaction",
        context -> transactionTemplate.execute(status -> completeTransactionUnit()),
        retryPolicy
);
```

重试包裹完整事务单元，不在持锁事务内部进行长等待。

### 治理

```text
RetryExecutor
    └── 每一次物理尝试
            └── RateLimiter
                    └── CircuitBreaker
                            └── Bulkhead
                                    └── Remote Call
```

治理拒绝是否可重试必须由明确分类规则决定。

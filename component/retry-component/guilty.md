# 一、先给结论：重试组件到底是什么

我建议把重试组件定位为：

> **统一描述、判断、调度和观测“某个操作是否需要再次执行”的可靠性执行组件。**

它负责的是“再执行一次”的通用机制，但不负责保证业务最终一定成功。

重试组件不是简单的：

```java
for (int i = 0; i < 3; i++) {
    try {
        return operation.call();
    } catch (Exception ignored) {
    }
}
```

一个生产级重试组件至少要回答这些问题：

1. 什么错误可以重试？
2. 什么错误绝对不能重试？
3. 最多执行几次？
4. 每次间隔多久？
5. 总共允许耗时多久？
6. 单次执行是否有超时？
7. 重试会不会造成重复写入？
8. 多层重试会不会把请求放大几十倍？
9. 重试过程中线程是否被阻塞？
10. 服务重启后，未完成重试是否需要恢复？
11. 重试耗尽以后交给谁处理？
12. 如何记录每次失败、延迟、最终结果？
13. 如何防止下游故障时出现重试风暴？
14. 消息重试、任务重试、HTTP 重试和数据库重试有什么区别？

所以，重试组件本质上属于：

> **可靠性执行控制组件，而不是业务补偿组件。**

---

# 二、重试场景必须先分类

不能把所有重试都设计成一种模式。不同场景的失败语义、时间跨度和恢复方式完全不同。

## 2.1 按重试发生的位置分类

| 类型      | 示例                         | 特点                | 推荐处理方式                    |
| ------- | -------------------------- | ----------------- | ------------------------- |
| 进程内同步重试 | HTTP 调用失败、Redis 短暂超时       | 生命周期短，可以立即重试      | `RetryExecutor.execute()` |
| 进程内异步重试 | CompletableFuture、异步任务     | 不能阻塞工作线程          | 异步调度器执行                   |
| 消息生产重试  | Kafka、RocketMQ、Pulsar 发送失败 | Provider 自身通常已有重试 | Provider 重试与组件重试分层        |
| 消息消费重试  | 消费业务异常                     | 涉及 ACK、重新投递、死信    | 由消息组件控制，使用重试策略            |
| 数据库事务重试 | 死锁、锁等待超时、乐观锁冲突             | 必须重试完整事务单元        | 事务模板集成重试策略                |
| 定时任务重试  | 批任务、异步任务执行失败               | 可能需要延迟分钟或小时       | 任务组件负责调度                  |
| 持久化重试   | 服务重启后仍要继续                  | 必须保存状态            | 可选 Durable Retry 扩展       |
| 人工重放    | 死信任务、长期失败记录                | 不属于自动重试           | 运维管理能力                    |
| 启动恢复重试  | 初始化配置、连接外部系统               | 需考虑应用启动生命周期       | 启动阶段专用策略                  |
| 业务补偿执行  | 退款、冲正、撤销                   | 不是原操作重试           | 补偿组件或业务流程负责               |

这里最重要的区别是：

* **短时间、进程内重试**
* **长时间、可恢复重试**
* **业务补偿**

这三类不能混在一个核心执行器里。

---

## 2.2 按失败性质分类

### 1. 瞬时故障

通常可以重试：

* 网络抖动
* 连接暂时中断
* 请求超时
* 数据库死锁
* Redis 短暂不可用
* Broker 临时不可用
* DNS 短暂失败
* 服务返回 502、503、504

这类故障的特点是：

> 不修改请求参数，再执行一次可能成功。

---

### 2. 限流或过载

例如：

* HTTP 429
* 下游线程池满
* Broker 限流
* 数据库连接池暂时耗尽
* 服务返回“稍后重试”

可以重试，但必须尊重：

* `Retry-After`
* 服务端建议等待时间
* 本地重试预算
* 总超时时间

这类不能立即无脑重试，否则会加剧下游过载。

---

### 3. 并发冲突

例如：

* 乐观锁更新失败
* 数据库死锁
* 版本号冲突
* 条件更新未命中
* 分布式锁未获取

这类不能简单归为统一的“异常重试”。

例如分布式锁没有获取：

* 某些场景应该等待后重试；
* 某些场景应该立即返回；
* 某些场景应该转异步任务；
* 某些场景表示重复请求，根本不应该重试。

所以最终必须由业务或调用方配置判断规则。

---

### 4. 永久性错误

默认不允许重试：

* 参数校验失败
* 认证失败
* 权限不足
* 数据格式错误
* 资源不存在
* SQL 语法错误
* 业务规则拒绝
* 余额不足
* 库存不足
* 消息反序列化失败

这类错误重复执行不会发生本质变化。

---

### 5. 结果不满足预期

不一定抛异常，但仍然需要重试。

例如：

```java
PaymentStatus status = queryPaymentStatus();

if (status == PROCESSING) {
    // 需要稍后再次查询
}
```

或者：

```java
HttpResponse response = callRemote();

if (response.getStatusCode() == 202) {
    // 处理中，稍后继续查询
}
```

所以重试组件不能只支持异常判断，还必须支持：

* 异常分类
* 返回结果分类

---

### 6. 结果不确定

这是最危险的类型。

例如：

1. 客户端向支付系统提交扣款；
2. 支付系统已经成功扣款；
3. 返回响应时网络断开；
4. 客户端收到超时；
5. 客户端不知道到底成功还是失败。

这时不能直接再次执行扣款。

必须先依赖：

* 幂等号；
* 业务状态查询；
* 请求结果查询；
* 去重机制。

因此：

> **结果不确定并不意味着可以重试，而是意味着必须先确认操作状态。**

---

# 三、按操作副作用分类

重试是否安全，首先取决于操作本身。

| 操作类型     | 示例                  | 是否可以直接重试       |
| -------- | ------------------- | -------------- |
| 纯读取      | 查询订单、查询配置           | 通常可以           |
| 天然幂等写入   | `PUT /resource/123` | 一般可以           |
| 条件幂等写入   | 根据业务唯一号创建订单         | 有幂等保护后可以       |
| 非幂等写入    | 扣款、发券、发送短信          | 不能直接重试         |
| 可查询状态的操作 | 支付、物流下单             | 应先查询后决定        |
| 有补偿能力的操作 | 冻结后可解冻              | 重试与补偿分开处理      |
| 外部不可控操作  | 调用第三方银行接口           | 必须严格依赖幂等号和状态查询 |

所以重试组件需要明确：

> 它只负责执行重试，不自动保证操作是幂等的。

组件最多可以提供安全检查扩展，但不能替代幂等组件。

---

# 四、重试组件的职责边界

## 4.1 重试组件负责什么

重试组件负责：

1. 定义重试策略；
2. 判断异常是否可重试；
3. 判断返回结果是否需要重试；
4. 控制最大执行次数；
5. 控制最大累计耗时；
6. 计算下一次重试间隔；
7. 支持退避和随机抖动；
8. 保存本次执行上下文；
9. 返回统一执行结果；
10. 发布重试事件；
11. 输出指标和日志；
12. 处理线程中断、取消和超时；
13. 防止无界重试；
14. 支持命名策略和统一配置；
15. 提供同步和异步执行能力；
16. 为持久化重试预留扩展接口。

---

## 4.2 重试组件不负责什么

### 1. 不负责业务幂等

重试组件不能知道：

* 订单是否已创建；
* 优惠券是否已发放；
* 资金是否已扣除；
* 消息是否已经消费成功。

这些由幂等组件和业务系统负责。

---

### 2. 不负责业务补偿

例如：

* 扣款成功但订单失败，需要退款；
* 库存扣减成功但支付失败，需要回补；
* A 服务成功、B 服务失败，需要撤销 A。

这些属于：

* 事务补偿；
* Saga；
* 业务流程；
* 对账修复。

不能叫“重试”。

---

### 3. 不负责事务原子性

重试组件可以重新执行事务，但不能保证事务本身的原子性。

事务模板负责：

* 开启事务；
* 提交；
* 回滚；
* 事务传播。

重试组件负责：

* 事务失败后，是否重新执行完整事务单元。

---

### 4. 不负责消息 ACK 和死信

消息组件负责：

* ACK；
* NACK；
* Reconsume；
* 延迟消息；
* 死信队列；
* 消费位点。

重试组件只向消息组件提供：

* 重试策略；
* 异常分类；
* 退避计算；
* 最大次数；
* 重试结果。

---

### 5. 不负责线程池治理

线程池的创建、隔离、拒绝、上下文传播属于并行组件。

重试异步执行时，应使用并行组件提供的：

* `ScheduledExecutorService`
* `ThreadPoolManager`
* 上下文传播能力

重试组件不能私自创建大量不可管理的线程池。

---

### 6. 不负责熔断和限流

重试、限流、熔断是三个不同概念：

* 重试：失败以后再次执行；
* 限流：限制请求进入速度；
* 熔断：下游持续失败时暂时停止调用。

重试组件可以与治理组件组合，但不能自己变成治理组件。

---

# 五、重试组件的核心设计模型

我建议第一版不要制造几十个小类。核心模型控制在十个左右。

## 5.1 `RetryExecutor`

这是用户真正调用的核心入口。

概念上提供：

```java
public interface RetryExecutor {

    <T> RetryResult<T> execute(
            RetryOperation<T> operation,
            RetryPolicy policy
    );
}
```

它负责执行整个重试循环。

不要一开始就设计：

* `RetryService`
* `RetryManager`
* `RetryClient`
* `RetryTemplate`
* `RetryOperations`
* `RetryHandler`

五六个同义门面。

第一版保留一个主要入口即可。

我目前更推荐名称：

```java
RetryTemplate
```

原因是它和未来的：

* `TransactionTemplate`
* `AsyncTemplate`
* `MessageTemplate`

风格一致。

但如果强调执行模型，也可以使用 `RetryExecutor`。

最终建议：

* API 接口：`RetryExecutor`
* 默认实现：`DefaultRetryExecutor`
* Spring 使用门面：直接注入 `RetryExecutor`

不再额外增加 `RetryTemplate`。

---

## 5.2 `RetryOperation<T>`

表示本次需要执行的操作。

```java
@FunctionalInterface
public interface RetryOperation<T> {

    T execute(RetryContext context) throws Exception;
}
```

为什么需要传入 `RetryContext`？

因为业务有时需要知道：

* 当前第几次尝试；
* 是否是第一次；
* 上一次失败原因；
* 已经耗时多久；
* 剩余时间；
* 当前重试标识。

不过还需要提供更简单的重载，让用户可以直接传：

```java
Supplier<T>
Callable<T>
Runnable
```

不能强迫普通用户每次都处理 `RetryContext`。

---

## 5.3 `RetryPolicy`

这是重试配置的不可变快照。

核心字段建议包括：

```java
public final class RetryPolicy {

    private final String policyName;

    private final int maxAttempts;

    private final Duration maxDuration;

    private final Duration attemptTimeout;

    private final BackoffStrategy backoffStrategy;

    private final RetryClassifier retryClassifier;

    private final RetryListener retryListener;
}
```

其中：

### `maxAttempts`

必须明确：

> `maxAttempts` 包含第一次执行。

例如：

```java
maxAttempts = 3
```

表示：

* 第一次正常执行；
* 最多再重试两次；
* 总共执行三次。

不要再同时出现：

* `retryTimes`
* `maxRetries`
* `attempts`

否则一定会出现语义混乱。

---

### `maxDuration`

控制整个逻辑调用的最长耗时，包括：

* 每次执行耗时；
* 重试等待时间；
* 调度延迟。

即使次数没有耗尽，只要总时间耗尽，也必须停止。

---

### `attemptTimeout`

控制单次尝试最大耗时。

例如：

* 整个调用最多 10 秒；
* 每次尝试最多 2 秒；
* 最多尝试 3 次。

这两个超时不能混为一谈。

---

## 5.4 `RetryContext`

表示一次逻辑重试过程的运行时上下文。

建议包含：

```java
public final class RetryContext {

    private final String retryId;

    private final String policyName;

    private final String operationName;

    private final int attempt;

    private final Instant startTime;

    private final Duration elapsedTime;

    private final Throwable lastFailure;

    private final Object lastResult;

    private final Map<String, Object> attributes;
}
```

需要明确：

* `attempt` 从 1 开始；
* 第一次执行也是 attempt 1；
* `RetryContext` 是单次逻辑调用独立的；
* 不能在多个请求之间共享可变上下文。

---

## 5.5 `RetryClassifier`

这是决定“是否继续重试”的核心。

它不能只判断异常类型，建议同时支持：

```java
public interface RetryClassifier {

    RetryDecision classify(
            RetryContext context,
            Object result,
            Throwable failure
    );
}
```

返回：

```java
public enum RetryDecision {

    SUCCESS,

    RETRY,

    STOP,

    ABORT
}
```

语义分别为：

* `SUCCESS`：操作成功，直接返回；
* `RETRY`：本次失败，可以再次执行；
* `STOP`：不再重试，按失败结果返回；
* `ABORT`：出现取消、中断、非法状态等，需要立即终止。

不过为了减少使用难度，第一版应提供组合式构造能力：

```java
RetryPolicy.builder()
        .retryOn(IOException.class)
        .retryOn(TimeoutException.class)
        .abortOn(IllegalArgumentException.class)
        .retryIfResult(result -> result == PROCESSING)
        .build();
```

用户不需要每次手写完整分类器。

---

## 5.6 `BackoffStrategy`

负责计算下一次等待时间：

```java
public interface BackoffStrategy {

    Duration nextDelay(RetryContext context);
}
```

第一期至少支持：

### 固定间隔

```text
1s、1s、1s、1s
```

适合简单本地操作。

### 指数退避

```text
100ms、200ms、400ms、800ms
```

适合远程服务调用。

### 指数退避加随机抖动

例如理论延迟 800ms，实际随机选择：

```text
0ms ~ 800ms
```

这是远程调用最推荐的默认策略。

因为没有抖动时，成千上万个请求会同时失败、同时等待、同时再次冲击下游。

### 服务端指定延迟

例如 HTTP 返回：

```http
Retry-After: 30
```

此时应允许分类器或结果解析器覆盖默认延迟。

---

## 5.7 `RetryResult<T>`

不能只返回业务值，或者重试耗尽后直接抛出最后一个异常。

组件应提供统一结果：

```java
public final class RetryResult<T> {

    private final RetryStatus status;

    private final T value;

    private final Throwable failure;

    private final int attempts;

    private final Duration elapsedTime;

    private final boolean exhausted;
}
```

状态建议包括：

```java
public enum RetryStatus {

    SUCCESS,

    EXHAUSTED,

    NOT_RETRYABLE,

    TIMED_OUT,

    CANCELLED,

    INTERRUPTED,

    BUDGET_REJECTED,

    EXECUTION_FAILED
}
```

需要同时提供两种调用风格：

### 结果风格

```java
RetryResult<T> result = retryExecutor.execute(...);
```

适合基础组件内部使用，信息完整。

### 抛异常风格

```java
T value = retryExecutor.executeAndGet(...);
```

适合普通业务代码。

但内部统一执行逻辑只能有一套。

---

# 六、重试执行状态机

建议明确状态流转：

```text
CREATED
   ↓
RUNNING
   ↓
SUCCEEDED
```

失败时：

```text
RUNNING
   ↓
CLASSIFYING
   ├── 不可重试 → NOT_RETRYABLE
   ├── 已耗尽   → EXHAUSTED
   ├── 已超时   → TIMED_OUT
   ├── 已取消   → CANCELLED
   ├── 被中断   → INTERRUPTED
   └── 可重试   → WAITING
                      ↓
                   RUNNING
```

判断顺序建议固定为：

1. 检查线程是否中断或任务是否取消；
2. 检查单次结果；
3. 对异常或结果进行分类；
4. 检查最大次数；
5. 检查总时间；
6. 计算下一次延迟；
7. 检查等待后是否会超过总时间；
8. 检查重试预算；
9. 等待或提交异步调度；
10. 进入下一次尝试。

顺序不能随意。

例如已经超过总时间，就不应该再等待下一次退避时间。

---

# 七、默认异常处理规则

重试组件必须保持保守，不能默认“所有异常都重试”。

## 默认不重试

* `IllegalArgumentException`
* 参数校验异常
* 业务异常
* 权限异常
* 认证异常
* `CancellationException`
* `InterruptedException`
* `Error`

其中 `InterruptedException` 必须：

1. 立即停止；
2. 恢复线程中断标记；
3. 返回 `INTERRUPTED` 或向上抛出。

不能捕获后继续重试。

```java
Thread.currentThread().interrupt();
```

---

## 默认可重试

我不建议核心组件直接默认大量异常可重试。

更安全的默认行为是：

> 没有明确配置为可重试，就不重试。

不同集成模块可以提供预设策略，例如：

* HTTP 瞬时错误策略；
* JDBC 死锁策略；
* Redis 网络异常策略；
* Kafka 发送瞬时错误策略。

这样核心组件不会错误理解业务异常。

---

# 八、同步重试和异步重试

## 8.1 同步重试

最简单的实现会等待：

```java
sleep(delay);
```

但它会占用当前线程。

适合：

* 延迟很短；
* 重试次数少；
* 并发量不高；
* 普通同步调用。

例如：

```text
100ms、200ms、400ms
```

可以接受。

不适合：

```text
30 秒后重试、5 分钟后重试
```

---

## 8.2 异步重试

异步重试必须使用调度器，而不是：

```java
CompletableFuture.runAsync(() -> Thread.sleep(...))
```

建议提供：

```java
CompletionStage<RetryResult<T>> executeAsync(...)
```

延迟调度依赖：

```java
ScheduledExecutorService
```

但这个调度线程池不应该由重试组件私自创建。

推荐关系：

```text
retry-component
        ↓ 使用
concurrency-component 的受管调度线程池
```

重试组件只定义：

```java
RetryScheduler
```

并行组件提供默认实现。

---

# 九、立即重试、延迟重试和持久化重试

这是架构中最容易混乱的地方。

## 9.1 立即重试

时间跨度一般在毫秒到几秒：

```text
100ms → 300ms → 800ms
```

由核心重试组件完成。

---

## 9.2 延迟异步重试

时间跨度一般为几秒到几分钟。

可以由：

* 异步调度器；
* 消息延迟队列；
* 任务调度组件

完成。

核心重试组件只计算：

* 是否重试；
* 下一次执行时间；
* 当前尝试次数。

真正的延迟执行由外部载体完成。

---

## 9.3 持久化重试

服务重启以后仍然继续，必须保存：

```text
retryId
operationType
businessKey
attempt
maxAttempts
nextExecuteTime
status
lastErrorCode
lastErrorMessage
version
leaseOwner
leaseExpireTime
createdTime
updatedTime
traceContext
payloadReference
```

但我不建议第一期就在重试组件内部实现完整数据库调度中心。

因为这会迅速演变成：

* 任务调度组件；
* 分布式执行组件；
* 工作流组件。

我的建议是：

> 重试核心组件定义持久化重试协议；真正的调度、抢占、恢复由任务组件实现。

例如未来关系：

```text
retry-api
   ├── RetryPolicy
   ├── RetryContext
   ├── RetryDecision
   └── DurableRetryCommand

task-component
   ├── 保存 DurableRetryCommand
   ├── 到期扫描
   ├── 分布式抢占
   └── 调用 RetryExecutor 继续执行
```

这样边界更加清晰。

---

# 十、重试和消息组件的关系

消息场景至少有三层重试。

## 10.1 客户端内部重试

Kafka、RocketMQ、Pulsar 客户端本身通常有：

* 连接重试；
* 元数据刷新；
* 发送重试；
* Broker 切换。

这是 Provider 层的传输重试。

---

## 10.2 消息组件外层发送重试

例如：

```text
MessageTemplate
    ↓
RetryExecutor
    ↓
KafkaProvider
```

它解决的是一次逻辑消息发送失败后的再次执行。

但必须避免：

```text
外层重试 5 次
× Kafka 内部重试 5 次
× 网络库重试 3 次
= 最多 75 次实际请求
```

因此消息组件中必须记录和约束“重试层级”。

推荐原则：

1. Provider 内部只处理底层传输瞬时故障；
2. 消息组件处理逻辑发送重试；
3. 长时间重试进入 Outbox 或持久化任务；
4. 不允许多个层级都进行大次数、长退避重试。

---

## 10.3 消息消费重试

消费失败以后，消息组件负责决定：

* 当前线程立即重试；
* NACK 后重新投递；
* 延迟队列；
* 重试主题；
* 死信队列；
* 人工处理。

重试组件负责提供：

```text
第几次尝试
是否还可以重试
下一次等待多久
该异常是否可重试
是否已经耗尽
```

消息组件负责将这个决策映射为：

```text
ACK / NACK / RECONSUME / RETRY_TOPIC / DLQ
```

所以更合理的依赖方向是：

```text
message-component
        ↓
retry-api
```

消息组件不能直接依赖重试组件的数据库实现或调度实现。

---

# 十一、重试和幂等组件的关系

两者不能合并。

## 重试解决

> 失败以后是否再次执行。

## 幂等解决

> 再次执行时，如何确保不会产生重复结果。

典型执行流程：

```text
RetryExecutor
    ↓
开始第 N 次尝试
    ↓
IdempotencyExecutor
    ↓
检查业务唯一键
    ↓
执行真实业务
    ↓
记录最终结果
```

对于有副作用的操作，推荐顺序是：

```text
重试控制在外层
幂等控制在每次真实业务执行的内层
```

即：

```java
retryExecutor.execute(() ->
        idempotencyExecutor.execute(idempotencyKey, operation)
);
```

原因是每一次重试都必须重新进入幂等判断。

但重试组件本身不应该直接强依赖：

```java
MessageIdempotencyExecutor
```

或者：

```java
IdempotencyExecutor
```

否则纯查询操作也会被迫引入幂等组件。

可以在 `RetryPolicy` 或执行元数据中声明：

```text
operationSafety = READ_ONLY
operationSafety = IDEMPOTENT
operationSafety = IDEMPOTENCY_PROTECTED
operationSafety = NON_IDEMPOTENT
```

当配置为 `NON_IDEMPOTENT` 且尝试次数大于 1 时：

* 可以输出警告；
* 严格模式下拒绝执行。

但最终安全性仍由调用方负责。

---

# 十二、重试和事务模板的关系

对于数据库事务，必须重试整个事务边界。

正确方式：

```java
retryExecutor.execute(() ->
        transactionTemplate.execute(() -> {
            updateOrder();
            insertRecord();
            updateAccount();
        })
);
```

而不是：

```java
transactionTemplate.execute(() -> {
    updateOrder();

    retryExecutor.execute(() -> insertRecord());

    updateAccount();
});
```

后一种方式可能造成：

* 事务持锁期间等待重试；
* 事务持续时间过长；
* 局部语句重试破坏完整业务语义；
* 数据库死锁风险进一步增加。

因此建议关系：

```text
RetryExecutor
      ↓
TransactionTemplate
      ↓
完整事务单元
```

特殊情况除外，例如乐观锁单语句更新，但仍然应该由业务明确配置。

还要特别注意“不确定提交”：

1. 数据库已经提交；
2. 客户端未收到成功响应；
3. 连接中断；
4. 调用方看到异常。

这时不能简单重新执行事务，必须通过业务唯一键确认提交结果。

---

# 十三、重试和熔断、限流的组合

典型远程调用链可以设计为：

```text
总超时控制
   ↓
Retry
   ↓
RateLimiter
   ↓
CircuitBreaker
   ↓
Bulkhead
   ↓
单次远程调用
```

每一次物理重试都需要重新经过：

* 限流；
* 熔断判断；
* 资源隔离。

例如熔断器已经打开，返回的 `CallNotPermittedException` 应默认不可重试。

否则会出现：

```text
熔断器拒绝
→ 重试
→ 再次被拒绝
→ 重试
→ 再次被拒绝
```

毫无意义。

不过这些组合顺序最终应由治理组件的执行管道负责，而不是重试组件写死。

---

# 十四、必须设计重试预算

只设置单次请求最大次数还不够。

假设：

* 每秒 10 万请求；
* 每个请求最多重试 3 次；
* 下游完全故障。

理论上瞬间可能变成：

```text
10 万原始请求
+ 20 万重试请求
= 30 万请求
```

下游故障越严重，收到的流量反而越大。

因此二期应加入 `RetryBudget`。

例如：

```text
每 100 个正常请求最多允许产生 10 个重试请求
```

或者：

```text
每个服务每秒最多 1000 次重试
```

预算不足时返回：

```java
RetryStatus.BUDGET_REJECTED
```

重试预算可以按以下维度隔离：

* 服务名；
* 操作名；
* Provider；
* 下游系统；
* 租户。

但指标标签不能直接使用高基数业务 ID。

---

# 十五、重试事件和可观测性

重试组件至少应发布以下事件：

```text
RetryStartedEvent
RetryAttemptStartedEvent
RetryAttemptFailedEvent
RetryScheduledEvent
RetrySucceededEvent
RetryExhaustedEvent
RetryAbortedEvent
RetryBudgetRejectedEvent
```

不过这些可以统一为：

```java
RetryEvent
```

配合：

```java
RetryEventType
```

避免每个事件都创建一个类。

这点和之前分布式锁组件的事件模型保持一致。

核心指标建议：

| 指标                            | 含义         |
| ----------------------------- | ---------- |
| `retry.execution.total`       | 逻辑重试任务数量   |
| `retry.attempt.total`         | 实际执行次数     |
| `retry.success.total`         | 最终成功数量     |
| `retry.exhausted.total`       | 重试耗尽数量     |
| `retry.not_retryable.total`   | 不可重试失败数量   |
| `retry.budget.rejected.total` | 预算拒绝数量     |
| `retry.attempt.duration`      | 单次执行耗时     |
| `retry.total.duration`        | 整个逻辑调用耗时   |
| `retry.backoff.duration`      | 重试等待时间     |
| `retry.active`                | 当前进行中的重试数量 |

日志中建议输出：

```text
retryId
operationName
policyName
attempt
maxAttempts
failureType
failureCode
nextDelay
elapsedTime
finalStatus
traceId
```

不要把完整请求体、密码、Token 或消息体默认打印出来。

---

# 十六、配置模型

建议支持两种策略来源。

## 16.1 代码内构建

```java
RetryPolicy policy = RetryPolicy.builder()
        .name("payment-query")
        .maxAttempts(4)
        .maxDuration(Duration.ofSeconds(10))
        .retryOn(TimeoutException.class)
        .backoff(BackoffStrategies.exponentialWithJitter(
                Duration.ofMillis(100),
                Duration.ofSeconds(2)
        ))
        .build();
```

适合局部特殊场景。

---

## 16.2 命名策略

配置文件：

```yaml
iron:
  retry:
    policies:
      message-send:
        max-attempts: 3
        max-duration: 5s
        backoff:
          type: exponential-jitter
          initial-delay: 100ms
          max-delay: 2s

      payment-query:
        max-attempts: 5
        max-duration: 30s
        backoff:
          type: fixed
          delay: 2s
```

业务代码只引用：

```java
retryExecutor.execute("message-send", operation);
```

这里需要：

```java
RetryPolicyRegistry
```

负责管理命名策略。

动态配置可以后续接配置中心，但第一期不要直接绑定 Nacos。

---

# 十七、项目模块建议

结合我们现有工程结构，我建议：

```text
retry-component
├── retry-api
├── retry-core
├── retry-config
├── retry-integrations
│   ├── retry-integration-spring
│   ├── retry-integration-concurrency
│   ├── retry-integration-observability
│   ├── retry-integration-message
│   └── retry-integration-transaction
└── retry-demo
```

## `retry-api`

存放稳定公开模型：

```text
RetryExecutor
RetryOperation
RetryPolicy
RetryContext
RetryResult
RetryStatus
RetryDecision
RetryClassifier
BackoffStrategy
RetryListener
```

---

## `retry-core`

存放执行实现：

```text
DefaultRetryExecutor
RetryDecisionEngine
RetryDelayController
RetryPolicyValidator
DefaultRetryPolicyRegistry
RetryEventPublisher
```

这里不要过度拆分。

例如下面这些暂时没必要独立成类：

```text
AttemptNumberCalculator
RetryDurationChecker
RetryThrowableResolver
RetryResultResolver
```

可以先作为 `DefaultRetryExecutor` 或 `RetryDecisionEngine` 的私有方法。

---

## `retry-config`

负责：

* Spring Boot 属性绑定；
* 默认 Bean；
* 命名策略装配；
* 配置校验；
* Starter 自动配置。

---

## `retry-integrations`

只放不同组件之间的适配，不放核心语义。

例如：

### `retry-integration-concurrency`

提供：

* 异步重试调度器；
* 受管线程池接入；
* 上下文传播。

### `retry-integration-message`

提供：

* 消息发送重试策略适配；
* 消费异常到 `RetryDecision` 的映射；
* 重试次数和消息投递次数转换。

### `retry-integration-transaction`

提供：

* 数据库死锁异常分类；
* 事务模板组合辅助类。

### `retry-integration-observability`

提供：

* Micrometer 指标；
* Trace；
* MDC 上下文。

---

# 十八、建议分期

## 一期：进程内核心重试闭环

必须完成：

1. 同步重试；
2. 最大尝试次数；
3. 最大总耗时；
4. 异常分类；
5. 返回结果分类；
6. 固定退避；
7. 指数退避；
8. 随机抖动；
9. 线程中断处理；
10. 统一 `RetryResult`；
11. 事件发布；
12. 基础指标；
13. 命名策略；
14. Spring Boot 自动配置。

一期不做：

* 注解重试；
* 数据库存储；
* 分布式调度；
* 管理后台；
* 人工重放。

---

## 二期：组件集成和异步能力

完成：

1. `CompletionStage` 异步重试；
2. 并行组件调度器接入；
3. 消息组件接入；
4. 事务模板接入；
5. HTTP、JDBC 等预设分类器；
6. 重试预算；
7. 动态配置刷新；
8. 上下文传播；
9. 更完善的指标和事件。

---

## 三期：持久化和分布式重试

根据实际需求再做：

1. 持久化重试协议；
2. 重试任务仓储；
3. 到期任务调度；
4. 分布式抢占；
5. Lease 和续租；
6. 服务重启恢复；
7. 重试死信；
8. 人工重放；
9. 管理查询；
10. 大规模任务分片。

但这一期很可能需要和任务组件共同实现，而不是重试组件独立完成。

---

# 十九、几个必须提前确定的关键原则

## 原则一：默认不重试，显式声明可重试

比“默认所有异常都重试”安全得多。

## 原则二：最大次数包含第一次执行

统一语义，避免 `retryTimes` 和 `attempts` 混乱。

## 原则三：线程中断和取消永远优先

不能吞掉中断后继续执行。

## 原则四：有副作用的操作必须有幂等保障

重试组件不能替调用方承担重复执行风险。

## 原则五：短重试留在进程内，长重试交给外部载体

毫秒和秒级重试可以本地执行；分钟、小时级重试交给消息或任务组件。

## 原则六：禁止多层重试无约束叠加

必须明确 Provider、消息组件、业务层谁负责哪一层。

## 原则七：重试耗尽不等于业务结束

耗尽后可能进入：

* 死信；
* 补偿；
* 人工处理；
* 告警；
* 对账。

## 原则八：不要一开始通过注解隐藏执行语义

例如：

```java
@Retryable
public void pay() {
}
```

看起来方便，但很容易隐藏：

* 事务边界；
* 自调用失效；
* 幂等风险；
* 实际执行次数；
* 异步线程；
* 超时范围。

第一期优先显式 API。注解只能作为后续语法糖。

---

# 二十、我对我们这个项目的最终建议

消息组件一期结束后开始重试组件，这个顺序是合理的。

但是实施顺序应该是：

```text
第一步：完成 retry-api 的稳定语义
第二步：完成进程内 RetryExecutor
第三步：让消息组件只依赖 retry-api
第四步：补齐异步调度和并行组件集成
第五步：再接事务模板和幂等组件
第六步：最后评估是否真的需要持久化重试中心
```

当前最不应该做的是一上来实现：

* 重试数据库表；
* 分布式扫描；
* 注解体系；
* 管理后台；
* 大量 Provider；
* 通用工作流补偿。

否则“重试组件”会迅速膨胀成任务调度、事务补偿、消息重放和工作流的混合组件。

第一版的核心目标应该非常明确：

> **让任何一个进程内操作，都可以通过统一、安全、可观测、可配置的方式完成有限重试；同时为消息、事务、并行、幂等组件提供稳定的重试策略模型。**

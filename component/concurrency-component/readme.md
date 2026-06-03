并发执行
    我怎么更方便、更安全地并行执行多个任务？

    线程池封装
    CompletableFuture 封装
    异步任务编排
    超时控制
    fallback
    异常聚合
    上下文传播

并发协调
    解决的是 多个线程 / 多个实例同时想做同一件事，谁能做？谁等待？谁跳过？
典型的能力 

    本地锁
    分布式锁
    SingleFlight
    任务去重
    幂等互斥 

并发容量控制
    不能无限并发，否则线程池、数据库、Redis、下游接口会被打爆。

    信号量
    并发窗口
    Bulkhead 隔离
    队列限制
    拒绝策略

P1 必须吸收的

| JDK 能力                     | 作用         | 在 concurrency-component 中怎么用                   |
| -------------------------- | ---------- | ---------------------------------------------- |
| `ThreadPoolExecutor`       | 线程池核心实现    | 封装成 `ThreadPoolFactory` / `ThreadPoolRegistry` |
| `ExecutorService`          | 任务提交入口     | 底层执行器，不直接让业务散用                                 |
| `ScheduledExecutorService` | 定时、超时、延迟任务 | 用于 timeout、延迟执行、后续 watchdog                    |
| `CompletableFuture`        | 异步编排核心     | 作为 API 返回值，同时提供模板封装                            |
| `ThreadFactory`            | 线程命名       | 做统一线程名前缀、异常处理                                  |
| `RejectedExecutionHandler` | 拒绝策略       | 封装为组件自己的 `RejectionPolicy`                     |
| `ThreadLocal` / MDC        | 线程上下文      | 做上下文传播，不让异步任务丢 traceId                         |

P2 / P3 再吸收的

| JDK 能力                          | 作用       | 放在哪个域                           |
| ------------------------------- | -------- | ------------------------------- |
| `ReentrantLock`                 | 本地互斥锁    | P2 并发协调                         |
| `Condition`                     | 本地等待唤醒   | P2，可选                           |
| `Semaphore`                     | 并发容量控制   | P3 容量控制                         |
| `CountDownLatch`                | 等待多个任务完成 | P1 可用 CompletableFuture 替代，暂不暴露 |
| `CyclicBarrier`                 | 多线程阶段同步  | 后面作为高级协调工具                      |
| `Phaser`                        | 动态阶段同步   | 后面做批处理并发模板时再考虑                  |
| `ReadWriteLock` / `StampedLock` | 读写锁      | 暂不做，容易过度设计                      |
| `AtomicInteger` / `LongAdder`   | 计数器      | 内部指标用，不暴露业务 API                 |


| 能力                          | 原因                            |
| --------------------------- | ----------------------------- |
| `ForkJoinPool.commonPool()` | 不建议作为默认池，容易和业务/框架共用导致不可控      |
| `parallelStream()`          | 不建议纳入组件，线程池不可控                |
| Virtual Thread              | 你现在 Java 17，先不做；Java 21 后可以扩展 |
| Structured Concurrency      | Java 17 不适合直接依赖               |
| `Exchanger`                 | 场景太窄                          |
| `SynchronousQueue`          | 可以作为队列类型，但不作为默认               |

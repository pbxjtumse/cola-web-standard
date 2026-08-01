# 开源设计吸收说明

本项目吸收设计思想，不把任何第三方重试库类型暴露为公共 API。

## Failsafe

吸收：

- 不可变策略；
- 执行上下文和物理尝试快照；
- 异常与结果统一分类；
- 动态等待覆盖；
- 监听器异常隔离；
- 可替换时间和调度思想；
- 执行请求对象减少重载扩张。

不吸收：

- 把 Timeout、CircuitBreaker、Bulkhead、RateLimiter 和 Fallback 全部并入 `retry-core`。

## Resilience4j

吸收：

- 命名策略 Registry；
- 配置继承；
- 生命周期事件；
- Micrometer 适配；
- 结果和异常驱动的间隔选择。

不照搬：

- 以多层函数 Decorator 作为唯一入口；
- 把治理组件对象混入重试公共模型。

## AWS SDK

吸收：

- `RetryFailureCategory`；
- `TRANSIENT` 与 `THROTTLING` 分开表达；
- 服务端建议等待；
- Full Jitter；
- 二期 Retry Budget 方向。

暂不实现：

- adaptive 模式请求发送令牌桶，因为它会影响第一次请求，属于治理组件。

## Spring Framework / Spring Boot

吸收：

- 外部化配置；
- 自动配置可覆盖；
- ApplicationEvent 桥接；
- Micrometer/Actuator 集成方式。

暂不实现：

- 注解与 AOP 重试。显式 API 仍用于暴露真实事务、幂等和尝试边界。

## Reactor

吸收：

- `RetrySignal` 对统一 `RetryAttempt` 的启发；
- 取消是执行模型的一等能力；
- 非阻塞等待应由 Scheduler 完成。

暂不实现：

- Mono/Flux 重试；
- 在同步核心中调用 `block()`。

## Temporal

吸收为三期原则：

- 逻辑执行与物理尝试分离；
- 持久化下一次执行时间；
- Worker 崩溃后可能重复执行；
- At-least-once 下业务必须幂等；
- 长操作需要心跳和租约。

不吸收：

- 在 `retry-component` 内部实现工作流引擎。

## 最终组合

```text
Failsafe       -> 核心执行模型
Resilience4j   -> Registry、配置和观测
AWS SDK        -> 错误分类、Full Jitter、预算方向
Spring         -> 自动配置和框架适配
Reactor        -> 取消与异步信号模型
Temporal       -> 三期持久化语义
```

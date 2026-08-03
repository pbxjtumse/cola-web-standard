# retry-component 对 Foundation 能力的复用审查

## 一、结论

本版本只复用已经冻结、且语义与重试组件完全一致的 Foundation 能力。

```text
retry-api
    保持纯重试公共契约，不依赖 Foundation

retry-core
    -> foundation-id
    -> foundation-time
    -> foundation-core

retry-core tests
    -> foundation-test-support

retry-config
    -> foundation-id
    -> retry-core
```

复用不是目标本身。只有当基础能力的语义、生命周期和异常边界与重试场景一致时，才移除本地实现。

## 二、本次直接复用的能力

### 1. foundation-id

重试组件不再实现 UUID 生成算法。

```java
import com.xjtu.iron.foundation.id.api.StringIdGenerator;
import com.xjtu.iron.foundation.id.factory.IdGenerators;
```

普通 Java 默认执行器使用 UUID v7。Spring 环境仍然暴露固定名称 Bean：

```text
retryIdGenerator
```

如果应用存在 `StringIdGeneratorRegistry`，自动配置会优先查找用途名称：

```text
retry
```

Registry 存在但缺少该名称时立即失败；没有 Registry 时回退到 Foundation UUID v7。调用方显式设置的 `RetryExecution.retryId` 始终具有最高优先级。

### 2. foundation-time

`RetryClock` 现在扩展 Foundation `ClockProvider`：

```java
public interface RetryClock extends ClockProvider {
    long nanoTime();
}
```

Foundation 墙上时钟用于：

- 逻辑执行开始时间；
- 尝试开始和完成时间；
- 生命周期事件时间戳。

重试组件仍然保留 `nanoTime()`，用于：

- 单次尝试耗时；
- 总执行耗时；
- `maxDuration` 预算判断。

这是必要的重试语义，不是重复实现。墙上时间可能因为 NTP、人工校时或系统时钟回拨而变化，不能用于可靠的耗时预算。

### 3. foundation-core

`retry-core` 复用：

```text
Arguments
ExceptionSupport
```

用于统一：

- 构造器参数校验；
- 策略名称校验；
- 生成 ID 校验；
- Duration 非负校验；
- InterruptedException 中断标记恢复。

`retry-api` 没有因此依赖 `foundation-core`。公共契约模块继续保持最小依赖，避免所有只引用 RetryPolicy 的组件被迫传递基础实现依赖。

### 4. foundation-test-support

核心和自动配置测试复用：

```text
FixedStringIdGenerator
```

这样测试不再自己实现固定 ID Lambda 或临时测试生成器。

## 三、明确不复用的能力

### 1. Deadline 暂不用于 maxDuration

Foundation `Deadline` 使用绝对墙上时间。重试 `maxDuration` 必须依据单调时间计算，否则系统时钟回拨可能延长重试，系统时钟前跳可能提前终止重试。

因此：

```text
事件时间戳 -> ClockProvider
耗时预算   -> RetryClock.nanoTime
```

两者不能混为一个时间语义。

### 2. foundation-context 暂不替换 attributes

一期是同步、单线程、进程内重试。当前 `RetryExecution.attributes` 是一次调用的只读扩展属性，不承担跨线程传播。

二期引入异步重试、线程池调度和 Trace/MDC 传播时，再评估接入：

```text
ExecutionContext
ContextSnapshot
ContextCarrier
```

现在强行替换会扩大公共 API，并提前绑定尚未验证的异步上下文语义。

### 3. foundation-serialization 暂不接入

一期没有持久化重试任务，也不保存操作参数。三期如果实现 Durable Retry，再使用 Foundation Serializer 保存：

- 任务载荷；
- 失败快照；
- Trace 上下文；
- 下一次执行信息。

### 4. foundation-reflection 暂不接入

一期坚持显式 API，不实现 `@Retryable`。未来 Spring 注解适配层需要读取方法、注解和泛型时，才使用 Foundation Reflection。

### 5. CheckedSupplier 不替换 RetryOperation

`RetryOperation<T>` 不只是一个可抛异常的 Supplier，它还接收 `RetryContext`：

```java
T execute(RetryContext context) throws Exception;
```

其中包含尝试次数、剩余预算、上一次尝试和取消令牌。因此它是重试领域契约，不能被通用 `CheckedSupplier` 替代。

### 6. RetrySleeper 保留

Foundation 当前没有同步等待抽象。`RetrySleeper` 还承担：

- 隔离 `Thread.sleep`；
- 测试中记录退避而不真实等待；
- 二期切换调度器时提供清晰迁移边界。

所以它不是重复基础能力。

## 四、后续阶段复用路线

```text
消息组件一期验证
    -> foundation-id 生成 messageId
    -> foundation-serialization 统一消息载荷
    -> foundation-context 传递技术上下文

重试组件二期
    -> foundation-context 异步上下文快照
    -> concurrency-component 受管调度器

持久化重试三期
    -> foundation-serialization
    -> foundation-id
    -> foundation-time
    -> task-component / transaction-component
```

## 五、边界原则

1. Foundation 提供无业务语义的通用能力。
2. Retry 保留重试状态机、决策、退避和取消语义。
3. 不因为工具方法相似就让 `retry-api` 依赖所有 Foundation 模块。
4. 不使用墙上时钟替代单调耗时。
5. 不在一期提前引入异步上下文和持久化模型。

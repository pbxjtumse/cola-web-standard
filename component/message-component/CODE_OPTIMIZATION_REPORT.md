# message-component 代码优化说明

## 本次基于实际代码确认的结论

1. `MessageTemplate` 暂时不建议拆成 `MessageProducerTemplate` 和 `MessageConsumerTemplate` 两个外部入口。当前代码已经通过 `MessagePublisher` 和 `MessageConsumerRegistrar` 两个 API 接口分离了发送与消费契约，`MessageTemplate` 只是统一门面。这个方向可以保留。
2. `SendStatus.CONFIRMED` 在当前代码中已经定义为 Broker 或 Provider 明确确认接收成功，不代表业务消费成功。当前阶段不需要增加 `ACCEPTED`，因为 `DirectMessageSender` 和可靠发送实现都会等待 Provider 结果并转换为最终 `SendResult`。
3. 消费核心不应该引入 Spring。当前工程整体方向是正确的：`message-core` 保持纯 Java，`message-starter` 负责 Spring Boot 自动装配。但原代码存在消费执行模板在 `MessageTemplate` 内部直接 `new` 的问题，导致 Spring 无法注入真实幂等、事务和异常分类能力。

## 本次已完成的代码修改

### 1. MessageTemplate 支持注入 ConsumeExecutionTemplate

原问题：

```java
this.consumeExecutionTemplate = new ConsumeExecutionTemplate();
```

这会导致：

- `message-starter` 中即使后续装配了真实幂等执行器，也无法进入消费链路；
- 事务执行器无法被替换；
- 消费异常分类器无法被业务覆盖。

修改后：

- 保留原构造器和原 `create(...)` 方法，兼容一期代码；
- 新增完整构造器，允许传入 `ConsumeExecutionTemplate`；
- 新增 `create(..., MessageSendExecutor, ConsumeExecutionTemplate)` 工厂方法；
- Spring Boot Starter 创建 `MessageTemplate` 时注入消费执行模板。

### 2. MessageProperties 增加 consume 根配置

原问题：工程中存在：

- `MessageConsumeProperties`
- `MessageConsumeIdempotencyProperties`
- `MessageConsumeTransactionProperties`

但 `MessageProperties` 没有挂载 `consume` 字段，导致：

```yaml
xjtu:
  iron:
    message:
      consume:
        idempotency:
          enabled: true
```

这类配置无法真正绑定到根配置对象。

修改后：

```java
private MessageConsumeProperties consume = new MessageConsumeProperties();
```

并增加 `getConsume()` / `setConsume(...)`。

### 3. MessageAutoConfiguration 增加消费侧 Bean 装配

新增：

- `ConsumeExceptionClassifier`
- `MessageConsumeTransactionExecutor`
- `MessageIdempotencyExecutor`
- `ConsumeExecutionTemplate`

原则：

- core 不依赖 Spring；
- starter 负责装配；
- 没有真实幂等存储适配时默认 Noop；
- 如果全局显式开启消费幂等但没有 `MessageIdempotentOperations`，启动失败；
- 如果配置要求事务必须存在但没有真实事务执行器，启动失败。

### 4. ProviderConsumeResult 保留消费失败类型

原问题：`MessageTemplate.subscribe(...)` 中只返回：

```java
ProviderConsumeResult.of(handleInbound(...))
```

这样 decode 失败等错误会丢失 `ConsumeFailureType`。

修改后：

- `handleInbound(...)` 直接返回 `ProviderConsumeResult`；
- decode 阶段异常返回 `ProviderConsumeResult.retry(ConsumeFailureType.DECODE_ERROR, ...)`；
- Provider 后续可以保留更明确的失败诊断信息。

## 没有修改但建议后续处理的点

1. `MessageTemplate` 长期可以继续作为统一门面，但内部建议进一步拆出 `MessageProducerFacade` 和 `MessageConsumerFacade`，避免类继续变大。
2. `ConsumeExecutionTemplate` 后续应返回更完整的 `ConsumeExecutionResult`，同时携带 `ConsumeDecision`、`ConsumeFailureType`、description、retryAfter，而不是只返回 `ConsumeDecision`。
3. `message-integrations` 现在同时包含 Provider 实现和 Spring AutoConfiguration。长期建议拆成：
   - provider-kafka-core
   - provider-kafka-spring-boot-starter
   但当前阶段可以不拆，避免过早扩大模块数量。
4. 压缩包中包含大量 macOS AppleDouble 文件、`.DS_Store`、`target`、`.iml`。本次输出包已经清理这些文件，只保留源码、文档和配置。

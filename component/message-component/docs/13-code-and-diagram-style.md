# 13. 代码注释、换行与图示方法级约定

本文档记录 message-component 当前阶段的代码阅读风格，主要服务于后续继续理解二期可靠发送、Provider 映射和消费可靠性设计。

## 1. Java 代码换行原则

当前 Java 代码建议以 **120 字符左右** 作为默认换行参考，而不是过早在 70～80 字符处拆行。

推荐保留单行的场景：

```java
SendOptions actualOptions = sendOptions == null ? SendOptions.defaults() : sendOptions;
CompletionStage<ProviderSendResult> providerStage = prepared.provider().send(prepared.request());
```

建议继续多行展开的场景：

```java
return new SendResult(
        prepared.message().messageId(),
        prepared.destination(),
        prepared.providerDestination().providerName(),
        prepared.providerDestination().physicalName(),
        providerResult.status(),
        stage,
        providerResult.failureType(),
        providerResult.providerMessageId(),
        providerResult.description(),
        prepared.startedAt(),
        clock.instant(),
        providerResult.metadata(),
        reliabilityInfo);
```

原因是对象组装、构造器参数、builder 参数过多时，逐行展开反而更便于对照字段含义，也方便后续增加参数、调整顺序和排查返回结果。

## 2. 注释原则

本组件注释分三层：

1. **类注释**：说明这个类在组件中的位置、职责边界、为什么需要它。
2. **关键方法注释**：说明方法参与哪条主链路，以及输入输出语义。
3. **关键分支行内注释**：只解释容易误解的设计点，例如 UNKNOWN 默认不重试、confirmTimeout 不是 retry maxDuration。

不建议每一行都写注释。注释重点放在“设计含义”和“容易误解的边界语义”，不要重复描述 Java 语法本身。

## 3. 可靠发送特别注意点

1. `MessageTemplate` 负责准备发送，不负责 retry 细节。
2. `DefaultReliableMessageSender` 负责把 Provider 发送包装成 retry execution。
3. `MessageSendRetryClassifier` 负责把消息发送语义转换为 retry decision。
4. `UNKNOWN` 表示 Broker 是否收到不可确认，默认不重试。
5. `RETRY_EXHAUSTED` 是失败原因，不是 `SendStatus`。
6. 成功场景下 `lastFailureCode` 和 `lastFailureCategory` 保持空字符串，避免把 retry-component 内部的 `UNKNOWN` 成功分类误解为最后失败原因。

## 4. PlantUML 方法级约定

message-component 的时序图沿用 distributed-lock-component 的风格：

1. 参与者保持到类级别，不把一个方法画成一个参与者。
2. 箭头保留关键真实方法名，例如 `send(...)`、`prepare(...)`、`enrich(...)`、`resolve(...)`、`encode(...)`、`execute(...)`、`classify(...)`。
3. L1 展示主链路的关键方法，不展开所有异常。
4. L2 展示内部协作方法，例如 `DefaultReliableMessageSender.send(...)` 到 `RetryExecutor.execute(...)`。
5. L3 展示异常和边界方法，例如 `classifyProviderResult(...)`、`retryExhaustedResult(...)`。
6. L4 展示 Provider 原生结果映射方法，例如 `classifySendFailure(...)`、`classifySendStatus(...)`。

这样既能看到真实代码路径，又不会把图拆成过细的调用栈。

# 图示方法级约定

message-component 的时序图沿用 distributed-lock-component 的风格：

1. 参与者保持到类级别，不把一个方法画成一个参与者。
2. 箭头保留关键真实方法名，例如 `send(...)`、`prepare(...)`、`enrich(...)`、`resolve(...)`、`encode(...)`、`execute(...)`、`classify(...)`。
3. L1 展示主链路的关键方法，不展开所有异常。
4. L2 展示内部协作方法，例如 `DefaultReliableMessageSender.send(...)` 到 `RetryExecutor.execute(...)`。
5. L3 展示异常和边界方法，例如 `classifyProviderResult(...)`、`retryExhaustedResult(...)`。
6. L4 展示 Provider 原生结果映射方法，例如 `classifySendFailure(...)`、`classifySendStatus(...)`。

这样既能看到真实代码路径，又不会把图拆成过细的调用栈。

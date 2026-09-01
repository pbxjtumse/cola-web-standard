# message-component final optimization summary

## Final design decisions

### MessageTemplate

保留统一门面，不拆外部 API。

当前模型：

```
MessageTemplate
        |
        +-- MessagePublisher
        |
        +-- MessageConsumerRegistrar
```

原因：业务使用简单，发送和消费契约已经通过 API 接口隔离。

内部通过：

- MessageSendExecutor
- ConsumeExecutionTemplate

分别承载发送和消费执行链。

---

### SendStatus

保留：

- CONFIRMED
- FAILED
- REJECTED
- UNKNOWN

CONFIRMED 表示 Provider/Broker 明确确认。

UNKNOWN 表示客户端无法确认 Broker 是否收到，避免无脑重发。

当前阶段不新增 ACCEPTED。

---

### Consumer execution

优化前：

```
MessageTemplate
    new ConsumeExecutionTemplate()
```

导致 Spring Starter 无法注入真实：

- 幂等执行器
- 事务执行器
- 异常分类器

优化后：

```
message-core
    ConsumeExecutionTemplate

message-starter
    Bean装配
```

保持 core 纯 Java。

---

## Final code changes

1. MessageTemplate 支持注入 ConsumeExecutionTemplate。
2. MessageProperties 增加 consume 配置入口。
3. MessageAutoConfiguration 增加消费可靠性 Bean 装配。
4. 消费入站异常分类优化：
   - decode failure -> DECODE_ERROR
   - handler/execution failure -> HANDLER_ERROR
5. 清理 macOS 元数据、target、IDE 文件。

---

## Remaining future phase

后续二期再做：

- ConsumeExecutionResult（同时携带 decision/failure/retryAfter）
- ConsumerPipeline interceptor chain
- retry-component 深度接入消费重试
- Outbox
- DLQ 管理

当前版本适合作为消息组件可靠消费一期基础版本。


## Final correction

本版本进一步删除 MessageTemplate 内部和兼容路径中的 ConsumeExecutionTemplate 默认创建。

最终约束：

```text
message-core
    不创建 ConsumeExecutionTemplate

message-starter
    唯一负责装配 ConsumeExecutionTemplate
```

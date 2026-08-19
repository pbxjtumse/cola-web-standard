# 15. message-api 分包收口

> 文档定位：记录 message-component 二期可靠发送后的 API 分包收口。

## 1. 本次调整目标

原先 `message-api` 下大部分类都直接放在：

```text
com.xjtu.iron.message.api
```

随着发送模型、消费模型、可靠发送模型、序列化模型逐渐增加，根包已经开始拥挤。

本次只做一件事：

```text
按照 API 职责拆分包结构，保持现有功能语义不变。
```

## 2. 新包结构

```text
com.xjtu.iron.message.api
├── model
│   ├── MessageEnvelope
│   ├── MessageMetadata
│   ├── MessageHeaders
│   ├── MessageHeaderNames
│   ├── MessageContext
│   └── MessageDestination
│
├── publish
│   ├── MessagePublisher
│   ├── SendOptions
│   ├── SendResult
│   ├── SendStatus
│   ├── SendStage
│   ├── SendFailureType
│   └── SendReliabilityInfo
│
├── consume
│   ├── ConsumerDefinition
│   ├── MessageConsumerRegistrar
│   ├── MessageHandler
│   ├── MessageSubscription
│   ├── ConsumeContext
│   └── ConsumeDecision
│
├── codec
│   └── MessageSerializer
│
├── exception
│   └── MessageException
│
└── annotation
    └── MessageListener
```

## 3. 分包原则

| 包 | 职责 |
|---|---|
| `api.model` | 消息领域模型，只表达消息是什么。 |
| `api.publish` | 消息发送入口、发送选项、发送结果、发送状态。 |
| `api.consume` | 消费者定义、消费处理器、消费决策和订阅句柄。 |
| `api.codec` | 业务 payload 序列化契约。 |
| `api.exception` | 对外异常模型。 |
| `api.annotation` | 注解能力。 |

## 4. 为什么这样拆

`message-api` 是业务侧最常接触的模块，后续还会继续增长。如果所有类都堆在根包下，会出现几个问题：

1. 发送、消费、模型、序列化、异常概念混在一起；
2. IDE 自动补全时难以判断类的职责；
3. 后续新增可靠消费、批量发送、事务消息时根包会继续膨胀；
4. API 文档不容易按领域组织。

拆分以后，业务使用时更清晰，例如：

```java
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.api.model.MessageDestination;
import com.xjtu.iron.message.api.publish.SendResult;
```

消费侧则是：

```java
import com.xjtu.iron.message.api.consume.ConsumerDefinition;
import com.xjtu.iron.message.api.consume.MessageHandler;
import com.xjtu.iron.message.api.consume.ConsumeDecision;
```

## 5. 本次没有改什么

本次只调整包结构和 import，不改变二期可靠发送核心逻辑：

```text
DefaultReliableMessageSender
MessageSendRetryClassifier
MessageSendExecutor
DirectMessageSender
SendStatus / SendFailureType / UNKNOWN 策略
```

这些语义保持不变。

## 6. 后续注意

由于项目当前还没有正式上线，本次没有保留旧包兼容类，也没有制造 deprecated 过渡层。

如果后续要发布给外部业务系统使用，再考虑是否需要 facade 或迁移指南。

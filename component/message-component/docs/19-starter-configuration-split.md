# 19 Starter 配置收口说明

## 1. 本次修改目标

本次修改解决三个问题：

1. `MessageTemplateAutoConfiguration` 过大，发送、消费、Provider、基础 Bean 全部堆在一个类里；
2. `properties` 包所有配置类平铺，不能从包结构看出配置层级；
3. Demo 配置不应该放在 starter 中，starter 只提供通用组件配置。

## 2. AutoConfiguration 拆分

拆分后：

```text
MessageCoreAutoConfiguration
MessageProviderAutoConfiguration
MessageSendAutoConfiguration
MessageConsumeAutoConfiguration
MessageTemplateAutoConfiguration
```

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 已更新为上述五个类。

## 3. Properties 绑定原则

只保留一个根配置：

```java
@ConfigurationProperties(prefix = "xjtu.iron.message")
public final class MessageProperties
```

其它配置类不加 `@ConfigurationProperties`，只作为嵌套对象绑定。

## 4. Demo 配置原则

`MessageDemoProperties` 不属于 starter。Demo 是业务应用示例，配置放在 demo 模块的 `application.yml` 或 demo 自己的配置类中。

## 5. 最终装配链

```text
MessageCoreAutoConfiguration
    ├── Serializer
    ├── MessageWireCodec
    ├── MessageContextAccessor
    └── MessageEnvelopeEnricher

MessageProviderAutoConfiguration
    ├── DestinationRouteRegistry
    ├── DestinationResolver
    └── MessageProviderRegistry

MessageSendAutoConfiguration
    ├── MessageSendReliabilityOptions
    └── MessageSendExecutor

MessageConsumeAutoConfiguration
    ├── ConsumeExceptionClassifier
    ├── MessageIdempotencyExecutor
    ├── MessageConsumeExecutor
    └── MessageConsumerAdapter

MessageTemplateAutoConfiguration
    └── MessageTemplate
```

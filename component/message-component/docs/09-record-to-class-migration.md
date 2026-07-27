# 09 V4 record 转普通类迁移说明

## 1. 迁移原则

本次只调整 Java 表达形式，不重新设计模块和生命周期：

```text
record
  ↓
final class + private final 字段 + 显式构造器 + 显式访问器
```

保留内容：

- 原 package、类名和泛型参数
- 原字段顺序和字段语义
- 原构造校验、默认值和标准化逻辑
- 原静态工厂、Builder、业务方法和内部辅助方法
- 原类、属性、方法和内部中文注释
- 原 record 风格访问器名称，降低 API 破坏
- 原模块、Provider、Testkit、Demo、文档和时序图

## 2. 已转换类型

### message-api

- `ConsumeContext`
- `ConsumerDefinition`
- `MessageContext`
- `MessageDestination`
- `MessageMetadata`
- `SendOptions`
- `SendResult`

### message-spi

- `ProviderDestination`
- `ProviderInboundMessage`
- `ProviderSendRequest`
- `ProviderSendResult`
- `ProviderSubscriptionRequest`

### message-core

- `CurrentMessage`
- `DestinationRoute`
- `DestinationRouteRegistry.RouteKey`
- `MessageComponentOptions`
- `MessageTemplate.PreparedSend`
- `MessageWireCodec.DecodedInbound`
- `MessageWireMapper.DecodedInbound`

### integrations

- `KafkaMessageProviderConfig`
- `RocketMqMessageProviderConfig`
- `PulsarMessageProviderConfig`

### testkit

- `InMemoryMessageRecord`
- `InMemoryMessageProvider.SubscriptionState`

## 3. MessageWireMapper 修复

旧 `MessageWireMapper` 仍引用已经删除的字段和方法，例如：

```text
MessageHeaders.MESSAGE_ID
MessageDestination.category()
ProviderInboundMessage.deliveryAttempt()
ProviderInboundMessage.metadata()
```

同时项目中已经存在新的 `MessageWireCodec`，继续维护两套实现会导致线级协议分叉。

V4 的处理是：

```text
MessageWireMapper
    ↓ 兼容委托
MessageWireCodec
```

`MessageWireCodec` 成为唯一真实实现，`MessageWireMapper` 只保留旧 API 入口并标记 `@Deprecated`。

## 4. 验证规则

- Java 源码中不存在 record 类型声明。
- 公共模块使用 Java 17、`-Xlint:all -Werror` 编译。
- InMemory 发送、消费和上下文传播验证通过。
- 三个 Provider 配置类完成独立语法编译检查。
- 真实 Provider 仍需使用 Maven 和真实 Broker 验证。

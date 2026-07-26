# 03. 逻辑目的地与路由

## 1. MessageDestination

```java
public record MessageDestination(
        String name,
        String namespace,
        MessageCategory category,
        String providerHint)
```

### name

业务逻辑消息名称：

```text
order-paid
payment-requested
points-granted
```

### namespace

业务域或上下文边界：

```text
trade
payment
member
settlement
```

不建议放：

```text
dev
sit
uat
prod
```

环境应该由部署配置映射到物理资源，否则同一业务代码会因为环境变化而改变逻辑契约。

### category

- `EVENT`：已经发生的事实，例如 `OrderPaid`；
- `COMMAND`：要求某个明确接收方执行动作，例如 `CloseOrder`；
- `NOTIFICATION`：通知类语义，不强调领域事实或命令约束。

类别不是装饰字段。它可以在后续治理中影响：

- 命名规则；
- 权限；
- 消费者数量约束；
- 重试策略；
- 审计策略。

### providerHint

可选覆盖提示。使用场景：

- 灰度迁移；
- 测试时强制某个 Provider；
- 同一逻辑消息明确需要专用通道。

它不是物理 Topic，也不应该成为业务默认配置。

## 2. DestinationRoute

路由将逻辑目的地映射为：

```text
providerName
physicalName
attributes
```

例如：

```java
DestinationRoute.of(
        MessageDestination.event("trade", "order-paid"),
        "kafka",
        "bank-prod-trade-order-paid-v1");
```

Provider attributes 留给路由层的少量物理扩展，但一期不把它扩张成无约束万能 Map 的业务入口。

## 3. 解析优先级

V2 的解析顺序：

1. 有 `providerHint`：查找该 Provider 的精确路由；
2. 无 hint：查找默认 Provider 的精确路由；
3. 默认 Provider 没有路由，但只有一条 Provider 路由：使用唯一路由；
4. 有多条路由且默认 Provider 不匹配：判定歧义；
5. 完全无路由：根据 `DestinationRoutingMode` 处理。

## 4. STRICT

默认模式：

```text
缺少精确路由
    ↓
REJECTED
SendStage.RESOLVE
SendFailureType.ROUTING_ERROR
```

原因：

- Topic 名拼错不能默默发送；
- 多环境物理名称通常不应由代码猜测；
- RocketMQ Topic 可能需要预声明；
- Pulsar 完整 Topic 可能包含 tenant/namespace；
- Kafka 自动创建 Topic 在生产中也可能带来错误分区和副本配置。

## 5. IMPLICIT_DEFAULT

仅适合本地开发和快速演示。默认物理名：

```text
namespace-category-name
```

例如：

```text
trade-event-order-paid
```

它不会作为生产默认值。

## 6. 为什么物理字段不进入 MessageDestination

如果公共 API 直接包含：

```text
topic
tag
partition
subscription
tenant
pulsarNamespace
```

则业务模型会变成三家 SDK 字段并集，任何业务消息都要理解全部中间件概念。

V2 将它们放在：

- 路由配置；
- Provider 配置；
- 三期 Provider 专属能力。

## 7. 消费侧目的地校验

入站消息的系统头包含原始逻辑目的地。消费时必须与 `ConsumerDefinition.destination` 一致。

这防止：

- 多个逻辑消息误用同一物理 Topic；
- 路由配置错误；
- 消费者使用错误 payload 类型反序列化；
- 迁移期间跨逻辑目的地串线。

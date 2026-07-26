# 02. 消息模型与上下文

## 1. MessageEnvelope 字段分类

| 字段 | 是否稳定字段 | 发送前是否可空 | core 行为 |
|---|---:|---:|---|
| messageId | 是 | 是 | 缺失时生成；显式值保持不变 |
| messageType | 是 | 否 | 构造时校验 |
| schemaVersion | 是 | 是 | 使用默认版本补齐 |
| payload | 是 | 否 | 交给 Serializer |
| key | 是 | 是 | 映射为 Provider key |
| context | 是 | 否 | 空时使用 `MessageContext.empty()` |
| headers | 扩展 | 是 | 校验保留前缀后传播 |
| occurredAt | 是 | 是 | 默认等于 createdAt |
| createdAt | 是 | 是 | 使用统一 Clock 生成 |

## 2. 为什么上下文不是一个 Map

`source`、`correlationId`、`causationId` 和 `tenantId` 具有稳定语义，并参与 core 的自动传播规则。

若全部放入 `headers`：

- 字段名容易拼错；
- 业务可以覆盖系统值；
- 缺少统一补齐规则；
- 审计、监控和幂等集成需要重复解析；
- 后续字段演进无法通过类型系统发现。

因此使用显式不可变 `MessageContext`。

## 3. 为什么 Trace 和 MDC 仍在 headers

Trace/MDC 属于技术上下文，具体实现可能是：

- OpenTelemetry；
- Brave；
- SkyWalking；
- 自研 Trace；
- SLF4J MDC 白名单。

如果把 `traceId`、`spanId`、`baggage` 固化为 API 字段，公共 API 会绑定某种追踪模型。

V2 只保留开放载体：

```text
traceparent
tracestate
baggage
x-request-id
mdc-user-id
```

二期由 `MessageInterceptor` 或专门的 context propagation integration 负责采集和恢复。

## 4. source

`source` 是当前消息生产应用，而不是整个业务流程起点。

规则：

```text
explicitContext.source
        ↓ 若为空
MessageComponentOptions.applicationName
        ↓ 若仍为空
null
```

不会继承父消息 source。

## 5. correlationId

用于关联一个完整业务过程、Saga、订单流程或批次流程。

优先级：

```text
当前待发送消息显式 correlationId
        ↓
当前入站父消息 correlationId
        ↓
当前待发送消息 messageId
```

业务知道稳定流程 ID 时，建议显式设置，例如：

```text
order-10001
settlement-batch-202607260001
refund-process-90001
```

业务没有稳定流程 ID 时，根消息使用自身 messageId 也能建立关联链。

## 6. causationId

只表示直接父消息，不表示根消息。

优先级：

```text
当前待发送消息显式 causationId
        ↓
当前 Handler 正在处理的入站 messageId
        ↓
null
```

这允许通过消息集合重建因果树，而不是只有一条扁平关联链。

## 7. tenantId

规则：

```text
当前待发送消息显式 tenantId
        ↓
父消息 tenantId
        ↓
null
```

租户上下文允许继承，因为同一业务链路通常不能无意跨租户。二期还应增加租户覆盖安全策略，例如禁止 Handler 随意改变租户。

## 8. 三个 ID 的完整例子

```text
HTTP 请求创建订单
  ↓
M1 OrderCreated
messageId=M1
correlationId=order-10001
causationId=null
  ↓
M2 InventoryReserved
messageId=M2
correlationId=order-10001
causationId=M1
  ↓
M3 PaymentRequested
messageId=M3
correlationId=order-10001
causationId=M2
```

- 查询订单全部消息：按 `correlationId=order-10001`；
- 查询是谁直接触发 M3：看 `causationId=M2`；
- 幂等识别 M3 本身：使用 `messageId=M3` 加 consumerGroup。

## 9. 系统头线级映射

结构化字段在 Java API 中不是 Map，但跨 Broker 发送时仍需编码成线级属性：

```text
x-iron-message-id
x-iron-message-type
x-iron-message-schema-version
x-iron-message-source
x-iron-message-correlation-id
x-iron-message-causation-id
x-iron-message-tenant-id
x-iron-message-occurred-at
x-iron-message-created-at
x-iron-message-content-type
x-iron-message-destination-namespace
x-iron-message-destination-name
x-iron-message-destination-category
```

消费时 `MessageWireMapper`：

1. 读取系统头；
2. 校验必填字段；
3. 校验逻辑目的地；
4. 重建 `MessageEnvelope`；
5. 从业务可见 headers 中移除保留系统头；
6. 保留 trace 等非系统技术头。

这解决了“API 结构化”和“Broker 只能传属性”之间的矛盾。

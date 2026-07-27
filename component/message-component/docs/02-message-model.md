# 02 消息模型最终决策

## 1. 四层结构

```text
MessageEnvelope<T>
├── MessageMetadata
├── MessageContext
├── MessageHeaders
└── payload
```

`MessageEnvelope` 是聚合根，不是和另外三个对象并列的第四种元数据。

## 2. MessageMetadata

| 字段 | 语义 |
|---|---|
| `messageId` | 当前消息实例的唯一标识 |
| `messageType` | 稳定业务契约名称 |
| `schemaVersion` | payload 结构版本 |
| `messageKey` | 当前消息主要关联的业务实体键 |
| `occurredAt` | 业务事实实际发生时间 |
| `createdAt` | 消息信封创建时间 |

### messageId 不能替代 messageKey

同一订单可以产生：

```text
M1 OrderCreated  messageKey=order-10001
M2 OrderPaid     messageKey=order-10001
M3 OrderShipped  messageKey=order-10001
```

三条消息的 `messageId` 必须不同；`messageKey` 可以相同。如果直接把 `messageId` 当 Kafka Key，每条消息通常会被哈希到不同分区，无法建立同一业务实体的分区亲和。

`messageKey` 允许为空。它只提供业务实体亲和与原生 Key 映射，不构成跨 Provider 顺序保证。

## 3. MessageContext

| 字段 | 语义 |
|---|---|
| `source` | 当前消息的生产应用 |
| `correlationId` | 整个业务过程的关联标识 |
| `causationId` | 直接触发当前消息的父消息 ID |
| `tenantId` | 可选租户标识 |

消费 M1 时发送 M2：

```text
M2.correlationId = M1.correlationId
M2.causationId   = M1.messageId
M2.source        = 当前应用名
```

`messageKey` 不自动继承，因为子消息可能围绕另一个业务实体。

## 4. MessageHeaders

`MessageHeaders` 是不可变值对象，保存业务扩展和技术传播头，例如：

```text
traceparent
tracestate
baggage
biz-scene
```

组件保留头由 `MessageHeaderNames` 定义，只在 `MessageWireCodec` 中生成。业务不能写入 `x-iron-message-*`。

## 5. 删除 MessageCategory

一期公共 API 不再强制 `EVENT / COMMAND / NOTIFICATION`。

- Event 与 Command 的区别仍然有效，但属于契约语义和命名规范。
- `NOTIFICATION` 往往仍可归为事实 Event 或动作 Command，不适合作为同级稳定类别。
- Kafka、RocketMQ、Pulsar、路由和 ACK 流程都不依赖该字段。

推荐命名：

```text
事实：order-paid、points-granted
要求：create-payment、grant-points、close-order
```

未来只有当权限、单责任方、命令结果和审计确实需要时，才加入可选 `MessageIntent`。

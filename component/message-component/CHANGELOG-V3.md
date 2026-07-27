# V3 变更摘要

## 公共模型

- 新增 `MessageMetadata`。
- `MessageHeaders` 从系统头工具类改为真正不可变值对象。
- 新增 `MessageHeaderNames` 保存系统头常量。
- `MessageEnvelope` 改为 `metadata + context + headers + payload`。
- `key` 改名 `messageKey`，并删除“设置后保证顺序”的错误暗示。
- 删除 `MessageCategory`。
- `MessageDestination` 改为 `namespace + name + providerHint`。
- `ConsumeContext` 和 `ProviderInboundMessage` 删除统一 `deliveryAttempt`。

## core

- `MessageWireMapper` 改名 `MessageWireCodec`。
- 更新线级系统头和逻辑目的地校验。
- 子消息继续自动传播 correlationId、causationId 和 tenantId。
- `ThreadLocalMessageContextAccessor` 使用明确 Scope 内部类替代 AtomicBoolean Lambda。

## Provider

- `ProviderSendRequest.key` 改为 `messageKey`。
- Kafka 配置改名为 `clientIdPrefix`、`consumerRetryBackoff`。
- Kafka 原生属性禁止覆盖组件保护项。
- Kafka 显式设置 `acks=all` 和 `enable.idempotence=true`。
- 新增四组 Provider 元数据键常量。
- Kafka Worker 文档明确：单订阅单 Topic，多分区按分区处理，多订阅可并发多 Topic。

## 文档与图

- 固定三期路线。
- 新增 L0-L3 共 12 张业务时序图和 1 个公共样式文件。
- 新增 Java record 与 Kafka Worker 专项说明。

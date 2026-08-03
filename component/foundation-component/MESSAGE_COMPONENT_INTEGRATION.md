# 消息组件接入示例

## 一、依赖方向

```text
message-api  -> foundation-core + foundation-context + foundation-serialization-api
message-core -> message-api + foundation-id + foundation-codec
message-config -> foundation-serialization-jackson
```

消息 API 不应暴露 `ObjectMapper` 或 `JacksonJsonSerializer`。

## 二、MessageWireMapper 示例

```java
public final class MessageWireMapper {

    private final Serializer serializer;

    public MessageWireMapper(Serializer serializer) {
        this.serializer = Objects.requireNonNull(serializer);
    }

    public MessageWirePayload toWirePayload(MessageEnvelope<?> envelope) {
        SerializationContext context = new SerializationContext(
                "message-send",
                envelope.getSchemaVersion(),
                Map.of("messageType", envelope.getMessageType())
        );
        SerializationOptions options = SerializationOptions.builder()
                .maxBytes(4 * 1024 * 1024)
                .build();
        byte[] body = serializer.serialize(envelope.getPayload(), context, options);
        return new MessageWirePayload(
                body,
                serializer.format().getContentType(),
                envelope.getMessageType(),
                envelope.getSchemaVersion()
        );
    }
}
```

## 三、装配 Jackson

```java
ObjectMapper mapper = JacksonObjectMapperFactory.createDefault();
Serializer serializer = JacksonSerializerFactory.create(mapper);
MessageWireMapper wireMapper = new MessageWireMapper(serializer);
```

如果 Spring MVC 的 ObjectMapper 注册了面向前端的特殊时间格式，不建议直接共享实例。Foundation 序列化器会复制传入 Mapper，避免后续配置修改污染消息协议。

## 四、消息 ID 生成建议

消息组件可以直接依赖 `foundation-id`，但应在消息装配层使用专用 Bean 名称，例如 `messageIdGenerator`，不要注入一个全局无名称的 `StringIdGenerator`。

```java
StringIdGenerator messageIdGenerator = IdGenerators.uuidV7();
```

需要强调：

- `messageId` 是消息技术标识；
- 业务幂等键仍由业务定义；
- 发送结果未知时，不能因为重新生成了新 messageId 就认为重复发送风险消失；
- Outbox 记录 ID、消息 ID 和业务幂等键可以是三个不同概念。

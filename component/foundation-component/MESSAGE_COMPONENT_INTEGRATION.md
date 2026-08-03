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

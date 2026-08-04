# foundation-component V3 conflict-safe thin facade

这是 Foundation Component 的第三版重构版本。

本版按照以下原则调整：

1. 通用工具能力优先复用 JDK 17、Apache Commons、Jackson 等成熟开源能力；
2. Foundation 只提供一层很薄的统一门面，不再大量自研细碎工具类；
3. 工具门面不再命名为 `StringUtils`、`CollectionUtils`、`ListUtils` 这类容易与开源库重名的类；
4. Foundation 工具门面统一采用 `IronXxx` 命名，例如 `IronStrings`、`IronCollections`、`IronLists`；
5. 模型、值对象和能力接口不加 `Iron` 前缀，也不加 `Utils`，例如 `Deadline`、`ExecutionContext`、`IdGenerator`、`Serializer`；
6. 序列化能力合并为一个 `foundation-serialization` Jar，内部仍保留 `serialization` 协议包和 `serialization.jackson` 实现包。

## 模块

```text
foundation-component
├── foundation-core
├── foundation-time
├── foundation-id
├── foundation-codec
├── foundation-context
├── foundation-reflection
├── foundation-resource
├── foundation-serialization
├── foundation-test-support
└── foundation-architecture-tests
```

## 命名示例

```java
IronStrings.trimToNull(name);
IronCollections.groupBy(messages, Message::getTopic);
IronLists.partition(items, 100);
IronMaps.getString(headers, "traceId");
IronDigests.sha256Hex(payload);
```

这种命名避免了下面这种混乱：

```java
org.apache.commons.collections4.ListUtils.partition(...);
org.springframework.util.CollectionUtils.isEmpty(...);
com.xjtu.iron.foundation.core.collection.ListUtils.partition(...);
```

## 序列化使用

```java
Serializer serializer = new JacksonJsonSerializer(
        JacksonObjectMapperFactory.createDefault()
);

SerializedPayload payload = serializer.serialize(event, SerializationOptions.defaults());
OrderEvent event = serializer.deserialize(payload, OrderEvent.class);
```

## 构建

放入现有 `component` 工程后执行：

```bash
mvn -U -pl component/foundation-component -am clean verify
```

当前生成环境没有 Maven，因此无法在沙箱内执行完整 `mvn clean verify`。

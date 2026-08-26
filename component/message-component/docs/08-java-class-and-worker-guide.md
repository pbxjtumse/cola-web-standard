# 08 普通不可变类与 Kafka Worker 说明

## 1. 为什么当前版本不使用 record

当前版本将消息组件中的值对象统一写成普通 `final` 不可变类，主要为了让字段、构造过程、校验、防御性复制和访问器都在源码中显式可见。

以 `ProviderSendRequest` 为例：

```java
public final class ProviderSendRequest {

    private final ProviderDestination destination;
    private final String messageId;
    private final String messageKey;
    private final Map<String, String> headers;
    private final byte[] body;

    public ProviderSendRequest(
            ProviderDestination destination,
            String messageId,
            String messageKey,
            Map<String, String> headers,
            byte[] body) {
        this.destination = Objects.requireNonNull(destination);
        this.messageId = messageId.trim();
        this.messageKey = normalize(messageKey);
        this.headers = immutableHeaders(headers);
        this.body = Objects.requireNonNull(body).clone();
    }

    public String messageId() {
        return messageId;
    }
}
```

普通类仍然坚持：

- 类使用 `final`，避免通过继承破坏值对象约束。
- 字段使用 `private final`。
- 不提供 setter。
- 构造时完成校验和标准化。
- Map、Set、List 和数组执行防御性复制。
- 原有访问器名称继续保留，避免本次语法重构扩大成业务 API 迁移。
- 显式实现 `equals`、`hashCode` 和 `toString`。

## 2. 为什么没有强制把所有访问器改成 getXxx

当前目标是保持 API 稳定和源码可读，而不是同时进行全工程 API 改名。为了降低修改风险，普通类继续显式提供：

```java
messageId()
destination()
status()
```

这些方法已经不再由编译器生成，而是在源码中完整声明。后续是否统一为 `getMessageId()` 应作为独立 API 兼容性变更处理。

## 3. Kafka 专用 poll 线程

`KafkaConsumer` 不作为线程安全共享对象使用。Worker 让同一线程拥有：

```text
subscribe
poll
seek
commit
close
```

外部关闭线程只执行：

```text
running.compareAndSet(true, false)
consumer.wakeup()
```

`AtomicBoolean` 同时保证状态跨线程可见和 close 只生效一次。

当前一个 Worker 只订阅一个物理 Topic。多 Topic 通过多次 `subscribe` 创建多个 Worker 并行运行，而不是一个线程先拉完 Topic A 再拉 Topic B。

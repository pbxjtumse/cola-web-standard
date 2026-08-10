# message-component 二期工程收口：分包、ID 与 Codec

## 1. 分包收口

本轮把 `message-core` 根包中的杂散类按职责移动到以下包：

| 包 | 职责 |
|---|---|
| `com.xjtu.iron.message.core` | 核心门面和组件运行参数 |
| `com.xjtu.iron.message.core.context` | 当前消息上下文、ThreadLocal 上下文作用域 |
| `com.xjtu.iron.message.core.routing` | 逻辑目的地到 Provider 物理目的地路由 |
| `com.xjtu.iron.message.core.codec` | 线级协议编解码和默认 JSON payload 序列化 |
| `com.xjtu.iron.message.core.id` | 消息 ID 生成策略与 foundation ID 适配 |
| `com.xjtu.iron.message.core.provider` | Provider 注册表 |
| `com.xjtu.iron.message.core.enrich` | 发送前补齐 messageId、时间、上下文 |
| `com.xjtu.iron.message.core.send` | 发送执行抽象、直发执行器、发送快照 |
| `com.xjtu.iron.message.core.send.reliability` | 可靠发送、retry 接入、发送重试分类 |

## 2. messageId 生成接入 foundation

`message-core` 继续保留消息领域接口：

```java
public interface MessageIdGenerator {
    String nextId();
}
```

默认 demo 使用 `UuidMessageIdGenerator` 作为 fallback。生产工程应提供自己的 `MessageIdGenerator` Bean，例如：

```java
@Bean
MessageIdGenerator messageIdGenerator(FoundationIdGenerator foundationIdGenerator) {
    return FoundationMessageIdGenerator.from(foundationIdGenerator::nextId);
}
```

这样 message 组件不直接散用 foundation 的 ID API，而是通过消息领域适配器复用统一 ID 能力。

## 3. message-codec-jackson 合并进 core

本轮删除独立 `message-codec-jackson` 模块，把 `JacksonMessageSerializer` 移入：

```text
com.xjtu.iron.message.core.codec.JacksonMessageSerializer
```

注意：`MessageWireCodec` 仍然保留。

| 类 | 职责 |
|---|---|
| `JacksonMessageSerializer` | payload 对象和 JSON byte[] 的转换 |
| `MessageWireCodec` | envelope、headers、context、messageId、destination 等线级协议编码 |

二者不是一个层次，不能互相替代。

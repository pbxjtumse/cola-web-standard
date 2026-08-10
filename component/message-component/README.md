# message-component-final

这是面向一期 Pulsar 验收的最终版项目骨架，核心调整：

1. Demo 不再使用 `main` 方法手动 new Provider，而是作为真实 Spring Boot 应用启动。
2. 发送通过 Controller 触发，消费通过 Spring Bean + `@MessageListener` 触发。
3. Pulsar 连接、Topic、订阅等参数全部放在 `application.yml`。
4. `JacksonMessageSerializer` 不再维护 ObjectMapper，而是依赖 foundation 的 `JsonCodec`。
5. `message-testkit` 不再作为 Demo 入口；本项目直接用 `message-demo-springboot` 验收。

## 模块说明

```text
message-component
├─ foundation-serialization
├─ foundation-serialization-jackson
├─ message-api
├─ message-spi
├─ message-core
├─ message-spring-boot-starter
├─ message-integrations/message-integration-pulsar
└─ message-demo-springboot
```

如果你现有工程里已经有 `foundation-serialization` 与 `foundation-serialization-jackson`，可以保留你现有 foundation，只把 `message-core` 中的 `JacksonMessageSerializer` 改成依赖你已有的 `JsonCodec` 或统一 `ObjectMapper`。

## 启动

```bash
mvn -pl message-demo-springboot -am spring-boot:run
```

## 发送消息

```bash
curl -X POST http://localhost:18081/demo/messages/send \
  -H "Content-Type: application/json" \
  -d '{
    "businessKey": "order-1001",
    "eventType": "PAY_SUCCESS",
    "payload": {
      "orderNo": "order-1001",
      "amount": 100.20,
      "event": "PAY_SUCCESS"
    },
    "headers": {
      "source": "message-demo"
    }
  }'
```

## 查看消费结果

```bash
curl http://localhost:18081/demo/messages/received
```

## 清空消费结果

```bash
curl -X DELETE http://localhost:18081/demo/messages/received
```

## 验收点

- Spring Boot 能正常启动。
- `MessageTemplate` 是 Spring Bean。
- `@MessageListener` 能自动注册 Pulsar Consumer。
- Controller 能发送消息。
- Listener 能收到同一个 Topic 中的消息。
- `/demo/messages/received` 能看到消费结果。
- 业务代码不直接依赖 PulsarClient。
- Pulsar 参数完全从 YAML 读取。
- Jackson 配置由 foundation 管理，消息组件不再重复 new ObjectMapper。


## phase2-send-reliability-v2-clean 结构收口

本版本在二期可靠发送第一版基础上做工程收口：

1. `message-core` 按 context、routing、codec、id、provider、enrich、send、send.reliability 分包。
2. `MessageTemplate` 支持注入 `MessageIdGenerator`，生产工程可用 `FoundationMessageIdGenerator` 适配 foundation-component 统一 ID。
3. 删除独立 `message-codec-jackson` 模块，将默认 `JacksonMessageSerializer` 合并到 `message-core.codec`。
4. `MessageWireCodec` 继续保留，负责消息线级协议，不与 payload JSON 序列化混淆。

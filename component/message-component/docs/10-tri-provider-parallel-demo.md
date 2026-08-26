# 三 Provider 并行收发验证设计

本文件记录 message-demo-springboot 中用于一期收口的三 Provider 并行验证能力。

## 目标

验证同一套 message-api / message-core / message-spi 抽象是否可以同时驱动：

- Kafka Provider
- Pulsar Provider
- RocketMQ 4.x Remoting Provider

验证范围只包含普通消息：send / subscribe / ACK 或位点提交。重试、幂等、Outbox、事务消息不属于本阶段。

## 路由模型

业务代码只面对一个逻辑目的地：

```text
demo/message
```

配置中为同一个逻辑目的地配置三条 Provider 路由：

```yaml
xjtu:
  iron:
    message:
      routes:
        - namespace: demo
          name: message
          provider: kafka
          physical-name: message-demo-topic
        - namespace: demo
          name: message
          provider: pulsar
          physical-name: persistent://public/default/message-demo-topic
        - namespace: demo
          name: message
          provider: rocketmq
          physical-name: message-demo-topic
```

并行发送和并行订阅都通过 `MessageDestination.withProviderHint(provider)` 精确选择 Provider。

## Demo 接口

### 默认 Provider 发送

```http
POST /demo/messages/send
```

使用 `xjtu.iron.message.provider` 指定的默认 Provider。

### 指定 Provider 发送

```http
POST /demo/messages/send/kafka
POST /demo/messages/send/pulsar
POST /demo/messages/send/rocketmq
```

### 三 Provider 并行发送

```http
POST /demo/messages/send/all
```

该接口会同时向 `xjtu.iron.message.demo.providers` 中配置的 Provider 发送消息。
每个 Provider 都会生成独立的组件 messageId，但会共享一个 `demo-broadcast-batch-id` header，便于排查。

### 查看接收结果

```http
GET /demo/messages/received
GET /demo/messages/received/kafka
GET /demo/messages/received/pulsar
GET /demo/messages/received/rocketmq
GET /demo/messages/received-summary
```

## 验收标准

一次 `POST /demo/messages/send/all` 后：

1. response.total = 3；
2. response.confirmed = 3；
3. `GET /demo/messages/received-summary` 中 kafka、pulsar、rocketmq 都至少增加 1；
4. 三条接收记录都带有相同的 `demo-broadcast-batch-id`；
5. 三条记录的 providerName 分别为 kafka、pulsar、rocketmq；
6. providerMessageId、physicalDestination 能区分不同 Provider 原生结果。

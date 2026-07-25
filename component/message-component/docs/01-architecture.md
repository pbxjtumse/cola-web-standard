# 第一版架构说明

## 一、核心结论

一期同时实现 Kafka、RocketMQ、Pulsar 是可行的，而且有一个明显收益：可以尽早验证公共抽象是否被某一家中间件绑架。

但是一期只能实现“普通消息基础闭环”，不能把三种中间件的高级能力一起塞进统一接口。否则公共 API 会出现以下问题：

- RocketMQ 的 FIFO、延时、事务消息被错误映射为 Kafka 或 Pulsar 的同名能力。
- Kafka 的 offset、partition、seek、read-process-write 事务被隐藏后失去价值。
- Pulsar 的 Shared、Failover、Key_Shared、Reader 和分层存储等能力无法准确表达。

因此第一版采用两层边界：

```text
公共可移植层
  - 普通发送
  - 异步确认
  - 普通订阅
  - 成功确认
  - 失败重投
  - 消息键
  - 消息头

Provider 原生层
  - 负责把公共语义翻译为原生客户端操作
  - 暂时不向业务暴露高级能力
```

## 二、为什么第一版只有两个核心类

`message-core` 只有：

- `MessageTemplate`
- `MessageProviderRegistry`

`MessageTemplate` 内部通过私有方法完成：

- 校验。
- 默认值处理。
- 消息增强。
- 序列化。
- Provider 选择。
- 能力校验。
- 发送结果转换。
- 入站反序列化。
- Handler 调用。
- 消费决策转换。

没有提前拆出：

- `MessageValidator`
- `MessageEnricher`
- `SendExecutor`
- `SendResultMapper`
- `InboundDispatcher`
- `ConsumeDecisionResolver`

原因不是这些职责不存在，而是目前每种职责只有一个稳定实现，也没有独立替换需求。现在拆类只会增加跳转和理解成本。后续出现以下情况再提取：

1. 存在第二种真实实现。
2. 需要作为公共扩展点开放。
3. 当前私有逻辑明显膨胀且可以独立测试。
4. 提取后能够减少而不是增加认知负担。

## 三、公共 API 与 SPI 为什么放在同一模块

第一版 `message-api` 同时包含：

```text
com.xjtu.iron.message.api
com.xjtu.iron.message.api.spi
```

这是有意的。

当前 SPI 只有：

- `MessageProvider`
- `ProviderSendRequest`
- `ProviderSendResult`
- `ProviderInboundMessage`
- `ProviderSubscription`
- `ProviderMessageListener`
- `MessageCapability`

单独创建一个 Maven 模块不会带来真正的依赖隔离，只会增加父 POM、版本和导航成本。后续需要发布第三方 Provider SDK 或 SPI 独立兼容策略时，再拆成 `message-provider-spi`。

## 四、发送状态为什么必须有 UNKNOWN

网络超时或回调链路中断时，客户端可能无法判断 Broker 是否已经接收消息。

因此：

```text
CONFIRMED  已明确成功
FAILED     已明确失败
REJECTED   Broker 或权限明确拒绝
UNKNOWN    无法判断最终结果
```

`UNKNOWN` 不能自动等同于 `FAILED`。业务看到 `UNKNOWN` 后立即无条件重发，可能造成重复消息。二期应结合：

- 消息幂等。
- Producer 重试分类。
- Outbox。
- 发送审计。
- Broker 查询或业务对账。

## 五、消费决策为什么一期只有 SUCCESS 和 RETRY

第一版只需要验证三种 Provider 都能形成最小闭环：

```text
SUCCESS -> ACK 或推进消费位置
RETRY   -> 不推进位置或请求重新投递
```

`REJECT`、`DEAD_LETTER`、`SKIP` 等状态需要先明确：

- 谁创建死信目的地。
- 原消息与死信消息的头如何保留。
- 死信转发失败怎么办。
- 最大重试次数由 Broker、Provider 还是 core 决定。
- 不同异常分类如何影响决策。

这些属于二期可靠性设计，第一期提前加枚举只会产生没有完整行为的空概念。

## 六、依赖方向

```text
message-api
    ↑
message-core
    ↑
业务应用

message-api
    ↑
message-integration-kafka
message-integration-rocketmq
message-integration-pulsar

message-api
    ↑
message-testkit
```

三个 Provider 不依赖 `message-core`，只实现稳定 SPI。这样 Provider 不会反向耦合生命周期编排实现。

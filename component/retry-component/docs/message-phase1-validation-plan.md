# 消息组件一期验证计划

## 一、目标

消息组件一期先验证统一抽象和三种中间件的基本连通，不提前承诺可靠发送。

当前 K8s 已安装消息中间件但尚未接通，因此建议顺序为：

```text
Broker 可用性
→ 原生客户端连通
→ 消息组件 Provider 连通
→ 统一发送/消费模型
→ 异常分类
→ 一期验收
→ 二期可靠发送设计
```

## 二、第一阶段：基础设施确认

分别确认 Kafka、RocketMQ、Pulsar：

- Service、端口和 DNS；
- 集群内访问地址；
- 集群外 NodePort 或 Ingress 地址；
- 认证方式；
- TLS；
- Topic 是否存在或是否允许自动创建；
- 客户端版本与服务端版本兼容性；
- Broker、BookKeeper、NameServer 等核心 Pod 健康状态。

不要同时排查三种中间件。建议先选择当前状态最稳定的一种完成纵向闭环，再复制到其他 Provider。

## 三、第二阶段：原生客户端最小验证

每一种中间件先脱离自研消息组件验证：

1. 发送一条文本消息；
2. 消费并打印消息 ID；
3. 确认 ACK；
4. 重启消费者后继续消费；
5. 故意使用错误 Topic 或端口，记录原生异常；
6. 暂时关闭 Broker，记录超时和重连行为。

该阶段的目的不是写 Demo，而是拿到真实异常类型和客户端默认重试行为。

## 四、第三阶段：消息组件一期验证

验证统一接口：

- `MessageEnvelope` 到 Provider 原生消息的映射；
- destination/topic/tag/key；
- headers；
- 序列化和反序列化；
- 发送结果中的 messageId、partition、offset 等可选信息；
- 同步和异步 API 的语义是否一致；
- Provider 异常是否转换为稳定的消息异常。

一期建议只定义：

```text
SEND_SUCCEEDED
SEND_FAILED
SEND_OUTCOME_UNKNOWN
```

其中 `SEND_OUTCOME_UNKNOWN` 很重要：客户端超时不等于 Broker 一定没有收到消息。

## 五、重试组件在消息一期中的使用方式

消息一期不建议立即把所有发送操作套上外层重试。

先记录：

- Kafka Producer 自身 retries；
- RocketMQ sendMessage 重试次数；
- Pulsar Producer sendTimeout 和重发行为；
- 网络客户端自身重连；
- 业务层是否已经重试。

否则可能形成：

```text
业务层 3 次
× retry-component 3 次
× Provider 客户端 3 次
= 最多 27 次物理发送
```

一期只需要完成异常映射建议：

| 消息发送结果 | RetryFailureCategory | 一期处理 |
|---|---|---|
| 明确连接失败且 Broker 未接收 | `TRANSIENT` 或 `DEPENDENCY_UNAVAILABLE` | 可作为后续短重试候选 |
| Broker 明确限流 | `THROTTLING` | 尊重服务端退避 |
| 参数、序列化、Topic 非法 | `NON_RETRYABLE` | 不重试 |
| 超时但无法确认 Broker 是否收到 | 不应简单归类为 `TRANSIENT` | 标记 `SEND_OUTCOME_UNKNOWN` |

## 六、一期消息组件验收清单

每个 Provider 至少完成：

1. 一次正常发送；
2. 一次正常消费；
3. headers 和 key 保真；
4. 序列化失败明确返回不可重试异常；
5. Broker 不可用时返回稳定异常；
6. 超时场景能区分明确失败和结果未知；
7. 客户端默认重试参数有文档；
8. 指标记录发送数量、失败数量和耗时；
9. Trace/MDC 不丢失；
10. 不把消息正文默认写入日志。

## 七、进入二期可靠发送前必须回答

- 发送成功的业务定义是什么：客户端接受、Broker ACK，还是落盘？
- 结果未知时如何查询或去重？
- 是否使用 Outbox？
- 消息唯一键由谁生成？
- Producer 幂等能力是否打开？
- 消费端如何幂等？
- 本地短重试和 Broker 重试如何分层？
- 最终失败进入哪里：重试主题、死信、数据库任务还是人工处理？
- 服务重启后未完成发送如何恢复？

这些问题属于消息可靠发送二期，不应塞进当前重试组件一期。

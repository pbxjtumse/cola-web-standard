# Pulsar 真实集群调试指南

## 1. 本轮目标

本轮只验证一期普通消息闭环：

```text
MessageEnvelope
    ↓
MessageTemplate
    ↓
PulsarMessageProvider
    ↓
Pulsar Proxy / Broker
    ↓
Shared Subscription Consumer
    ↓
SUCCESS ACK 或 RETRY Negative ACK
```

暂时不验证：

- Retry Letter Topic；
- Dead Letter Policy；
- Key_Shared；
- 延时消息；
- Pulsar 事务；
- Spring Boot 自动装配。

## 2. 默认调试配置

`PulsarMessageDemo` 已写入当前 K8s 对外地址：

```text
serviceUrl  = pulsar://pulsar.xjtu-iron.online:6650
topic       = persistent://public/default/iron-message-component-debug
subscription= iron-message-component-debug-subscription
auth        = disabled
```

这里必须使用 Pulsar 二进制协议地址，不是 HTTP 8080，也不是 WebSocket 端口。

## 3. 服务端先确认 Topic

在 K8s Master 上执行：

```bash
kubectl exec \
  -n kubeblocks-pulsar \
  pulsar-cluster-broker-0 \
  -c broker \
  -- /pulsar/bin/pulsar-admin topics list public/default
```

没有调试 Topic 时创建：

```bash
kubectl exec \
  -n kubeblocks-pulsar \
  pulsar-cluster-broker-0 \
  -c broker \
  -- /pulsar/bin/pulsar-admin topics create \
  persistent://public/default/iron-message-component-debug
```

若集群允许自动创建 Topic，可以不手动创建；为了排除自动创建策略问题，首次调试建议显式创建。

## 4. IDEA 运行方式

运行类：

```text
com.xjtu.iron.message.demo.PulsarMessageDemo
```

默认不需要添加环境变量。

需要覆盖配置时，在 IDEA Run Configuration 中增加：

```text
IRON_PULSAR_SERVICE_URL=pulsar://pulsar.xjtu-iron.online:6650
IRON_PULSAR_TOPIC=persistent://public/default/iron-message-component-debug
IRON_PULSAR_SUBSCRIPTION=iron-message-component-debug-subscription
IRON_PULSAR_WAIT_SECONDS=30
IRON_PULSAR_RETRY_ONCE=false
```

集群开启 Token 认证后再增加：

```text
IRON_PULSAR_TOKEN=<token>
```

## 5. 第一次只验证成功 ACK

保持：

```text
IRON_PULSAR_RETRY_ONCE=false
```

期望输出包含：

```text
status=CONFIRMED
consumeDecision=SUCCESS
PULSAR MESSAGE COMPONENT DEBUG SUCCESS
```

消费端 `providerMetadata` 应至少包含：

```text
pulsar.topic
pulsar.message-id
pulsar.publish-time
pulsar.redelivery-count=0
```

## 6. 第二次验证 Negative ACK 重新投递

设置：

```text
IRON_PULSAR_RETRY_ONCE=true
```

第一次消费返回：

```text
consumeDecision=RETRY
```

两秒后应再次收到同一条消息，第二次返回：

```text
consumeDecision=SUCCESS
```

第二次 `providerMetadata` 中的：

```text
pulsar.redelivery-count
```

应大于第一次。

## 7. 服务端查看 Topic Stats

```bash
kubectl exec \
  -n kubeblocks-pulsar \
  pulsar-cluster-broker-0 \
  -c broker \
  -- /pulsar/bin/pulsar-admin topics stats \
  persistent://public/default/iron-message-component-debug
```

重点检查：

- `subscriptions` 中是否出现调试订阅；
- `msgBacklog` 是否归零；
- Consumer 是否在线；
- `unackedMessages` 是否归零。

## 8. 常见故障定位

### TCP 成功但 Java 客户端连接失败

`nc -vz` 只能证明 6650 端口可以建立 TCP 连接，不能证明 Pulsar Topic Lookup 返回的地址能被外部客户端访问。

如果日志中出现内部地址，例如：

```text
*.svc.cluster.local
10.x.x.x
broker Pod hostname
```

说明对外服务不是可用的 Proxy，或者 Broker 的 advertised listener 仍返回集群内地址。应优先检查 Pulsar Proxy 的二进制端口映射；若直接访问 Broker，则需要正确配置 external advertised listener。

### `TopicNotFoundException`

显式创建调试 Topic，或者检查 Broker 是否禁止自动创建 Topic。

### `AuthenticationException` / `AuthorizationException`

确认集群是否启用认证与授权；启用后设置 `IRON_PULSAR_TOKEN`，并给对应角色授予 `public/default` 的 produce/consume 权限。

### 发送 `CONFIRMED` 但消费超时

依次检查：

1. Topic 是否完全一致；
2. Subscription 是否创建成功；
3. Consumer 是否在线；
4. Topic Stats 中是否存在 backlog；
5. Proxy/Broker 广播地址是否外网可达；
6. Consumer 回调是否抛出异常并持续 Negative ACK。

### `UNKNOWN + TIMEOUT`

不能直接当作确定未发送。先通过 Topic Stats、Subscription backlog 或消息 ID 对照确认 Broker 是否收到，再决定是否重新发送。

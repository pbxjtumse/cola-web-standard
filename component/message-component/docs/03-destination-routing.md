# 03 逻辑目的地与路由

## 1. 逻辑目的地

```java
MessageDestination.of("trade", "order-paid")
```

字段：

```text
namespace = trade
name = order-paid
providerHint = null
qualifiedName = trade:order-paid
```

环境名不进入逻辑目的地。`dev/sit/prod` 应由部署配置决定。

## 2. 精确路由

```java
DestinationRoute.of(
        MessageDestination.of("trade", "order-paid"),
        "kafka",
        "bank-prod-trade-order-paid-v1");
```

同一逻辑目的地可以配置多个 Provider 路由。`providerHint` 只是一次调用或定义上的显式选择，不属于逻辑身份。

## 3. 严格模式

生产默认使用 `STRICT`：找不到路由就返回 `REJECTED + ROUTING_ERROR`，不猜测或创建 Topic。

本地验证可显式使用 `IMPLICIT_DEFAULT`，默认物理名为：

```text
namespace-name
```

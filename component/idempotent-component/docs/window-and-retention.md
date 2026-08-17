# WINDOWED：幂等窗口、执行租约与物理 Retention

## 1. 三个时间不能混

WINDOWED 模式至少有三个不同时间概念：

```text
processingTimeout
idempotencyWindow
recordRetentionTtl
```

它们职责不同。

---

## 2. processingTimeout：执行权租约

例如：

```text
processingTimeout = 30s
```

A 在 T0 抢占：

```text
PROCESSING
owner=A
version=1
processingExpireAt=T0+30s
```

30 秒后只说明：

> A 这一代 execution generation 已经超过执行权租约，可以被 RecoveryPolicy 评估是否允许接管。

它不等于幂等窗口结束。

---

## 3. idempotencyWindow：重复请求语义窗口

例如：

```text
idempotencyWindow = 5m
```

在窗口内：

```text
same namespace + same key
```

继续被视为同一个逻辑幂等请求。

即使第一次早已 SUCCESS，重复请求仍然：

```text
REPLAYED
```

窗口结束以后，可以开启新的 generation。

因此 V1.3 校验：

```text
idempotencyWindow > processingTimeout
```

避免执行权还可能有效，整个幂等语义却先结束。

---

## 4. recordRetentionTtl：窗口结束后的额外物理保留

例如：

```text
window = 5m
retention = 1h
```

含义：

```text
0 ~ 5m:
旧记录具有幂等阻断语义

5m ~ 65m:
旧记录可以物理存在用于审计/诊断
但已经不能阻止新的 generation
```

JDBC 中通过：

```text
window_expire_at
retention_expire_at
```

分离。

Redis 中 Key 的物理 TTL 可以到：

```text
windowExpireAt + retention
```

但 `tryAcquire.lua` 会先看 `window_expire_at`：

```text
语义窗口已结束
→ 在同一个 Hash 上重置并开启 version+1 generation
```

不是必须等 Redis Key 真正删除。

---

## 5. FIXED_FROM_FIRST_ACQUIRE

```text
T0 first acquire
windowExpireAt = T0 + 5m
```

后续：

```text
SUCCESS
FAILED
duplicate access
```

都不推进 windowExpireAt。

适合：

> 从第一次提交开始的固定防重复窗口。

---

## 6. SLIDING_ON_ACCESS

在窗口仍有效时，每次有效访问会推进：

```text
windowExpireAt = now + window
```

适合：

> 最近 N 分钟内持续有相同请求，就一直视为重复。

要谨慎使用，因为热点重复请求可能让窗口长期不结束。

---

## 7. WINDOWED 也可以使用 JDBC

V1.3 的语义不再写死：

```text
WINDOWED == Redis
```

JDBC Repository 也声明：

```text
windowedSupported=true
```

只是 Starter 默认：

```text
WINDOWED -> redis
```

因为 Redis 对高频短窗口去重通常更合适。

---

## 8. SHORT_TERM

旧枚举：

```java
IdempotencyMode.SHORT_TERM
```

只作为 V1.2 兼容别名保留。

Core/Provider 内部都会：

```text
canonical -> WINDOWED
```

新代码和新配置应统一写：

```text
WINDOWED
```

# 配置

```yaml
xjtu:
  iron:
    idempotent:
      enabled: true
      default-mode: DURABLE
      default-short-term-repository: redis
      default-durable-repository: jdbc
      processing-timeout: 30s
      short-term-record-ttl: 10m
      retry-on-processing-timeout: true
      retry-failed: true
      store-result: false
      lock:
        enabled: true
        provider-name: redisson
        wait-time: 0s
        lease-time: 5s
        fallback-to-state-on-failure: true
      redis:
        enabled: true
        key-prefix: iron:idempotency
      jdbc:
        enabled: true
        table-name: iron_idempotency_record
```

连接配置继续复用：

```yaml
spring:
  data:
    redis: ...
  datasource: ...
```

不要把连接信息复制到 `xjtu.iron.idempotent.*`。

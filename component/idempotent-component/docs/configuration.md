# Configuration - V1.1

```yaml
xjtu:
  iron:
    idempotent:
      enabled: true
      default-mode: DURABLE
      default-short-term-repository: redis
      default-durable-repository: jdbc
      processing-timeout: 30s
      store-result: false

      short-term:
        idempotency-window: 10m
        window-policy: FIXED_FROM_FIRST_ACQUIRE
        record-retention-ttl: 0s
        recovery-mode: NONE

      durable:
        recovery-mode: EXTERNAL_TASK
        recover-failed: true

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

基础设施连接继续使用：

```yaml
spring:
  data:
    redis: ...
  datasource: ...
```

## 短期窗口策略

固定窗口：

```yaml
window-policy: FIXED_FROM_FIRST_ACQUIRE
```

滑动窗口：

```yaml
window-policy: SLIDING_ON_ACCESS
```

滑动窗口才表达“每次有效访问后，再往后顺延 N 分钟”。

`record-retention-ttl` 只控制语义窗口结束后的物理记录额外保留时间，不应该替代 idempotencyWindow。

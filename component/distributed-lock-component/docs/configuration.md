# Distributed Lock 配置说明

分布式锁组件把配置分成两类：

1. `spring.data.redis.*`：Redis 连接配置，由 Spring Boot Redis AutoConfiguration 负责。
2. `iron.distributed-lock.*`：分布式锁语义配置，由本组件 starter 负责。

starter 模块不应该携带 `application.yml`。starter 只提供自动装配类和 `@ConfigurationProperties`，真正的连接地址应由业务应用或 demo 提供。

## Redis 连接配置

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      database: 0
      password:
      timeout: 3s
```

所有使用默认 Redis 的组件，例如缓存、幂等、分布式锁，原则上都复用业务应用中的同一个 `RedisConnectionFactory` / `StringRedisTemplate`。
不要让每个 starter 都单独维护一份 host、port、password。

## 分布式锁语义配置

```yaml
iron:
  distributed-lock:
    enabled: true
    default-provider: redis
    namespace: demo
    lease-time: 30s
    wait-time: 0s
    auto-renew: false
    # 不配置时由 LockOptions 按 lease-time / 3 自动推导。
    # renew-interval: 10s
    max-renew-time: 10m
    fencing-required: false
    # fencing-token-provider-name: redis
    fail-on-lock-lost: true
    redis:
      enabled: true
      key-prefix: iron:lock
      release-channel-prefix: iron:lock:release
      fencing-key-suffix: fence
```

`tryLock(lockName)` 和 `execute(lockName, callback)` 会使用上述默认 `LockOptions`。
如果业务代码显式传入 `LockOptions`，则以代码传入为准。

## 多组件共用 Redis 的建议

推荐：应用层统一配置 Redis 连接，各组件只配置自己的 key 前缀和业务语义。

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      database: 0

iron:
  cache:
    redis:
      key-prefix: iron:cache
  idempotent:
    redis:
      key-prefix: iron:idempotent
  distributed-lock:
    redis:
      key-prefix: iron:lock
```

只有出现“不同组件必须使用不同 Redis 集群”的情况，才建议额外暴露命名 Redis 连接，例如 `lockRedisTemplate`、`cacheRedisTemplate`。这应该作为高级扩展，而不是默认设计。

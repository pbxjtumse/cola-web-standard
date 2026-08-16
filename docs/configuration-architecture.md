# 配置架构说明

## 1. 最终应用与技术组件的职责

- `start`：最终可运行应用，负责提供 MySQL、Redis、MQ 等基础设施连接配置，并显式引入需要启用的技术组件 Starter。
- `*-starter`：定义 `@ConfigurationProperties`、自动装配和条件装配，不携带业务 `application.yml`。
- `*-demo`：独立演示应用，可以拥有自己的 `application.yml`，但密码必须通过环境变量或 Secret 注入。
- `api/core/provider`：不携带应用连接配置。

## 2. Redis 配置

统一连接由最终应用提供：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      database: ${REDIS_DATABASE:0}
      password: ${REDIS_PASSWORD:}
```

缓存和分布式锁默认复用同一 `RedisConnectionFactory`，通过 key 前缀隔离：

```yaml
xjtu:
  iron:
    cache:
      redis:
        key-prefix: "iron:cache:"
    distributed-lock:
      redis:
        key-prefix: iron:lock
```

当前版本只支持默认/主 Redis 连接。多 Redis 集群选择能力应统一设计后再加入，配置中不要提前放置尚未实现的 `template-bean-name`。

## 3. 配置属性归属

| 前缀 | 绑定类/来源 |
|---|---|
| `spring.data.redis.*` | Spring Boot Redis 自动配置 |
| `spring.datasource.*` | Spring Boot DataSource 自动配置 |
| `xjtu.iron.cache.*` | `XjtuIronCacheProperties` |
| `xjtu.iron.distributed-lock.*` | `DistributedLockProperties` |
| `xjtu.iron.distributed-lock.redis.*` | `RedisDistributedLockProperties` |
| `xjtu.iron.concurrency.*` | `XjtuIronConcurrencyProperties` |
| `xjtu.iron.governance.*` | `GovernanceProperties` |
| `xjtu.iron.observability.*` | `ObservabilityProperties` |

`start` 不重复定义这些 Properties；它只提供配置值并引入对应 Starter。

## 4. Kubernetes 建议

- ConfigMap：Redis/MySQL 地址、端口、database、非敏感组件配置。
- Secret：Redis/MySQL/Nacos 密码。
- Nacos：可安全动态调整的缓存 TTL、锁租期、线程池、治理阈值等语义配置。
- Redis 连接地址变化通过滚动重启生效，不建议默认热刷新连接工厂。

# Distributed Lock 配置说明

## 1. 配置分层

| 配置前缀 | 归属 | 说明 |
|---|---|---|
| `spring.data.redis.*` | 最终应用 | Redis 地址、端口、密码、database、连接池 |
| `spring.datasource.*` | 最终应用 | JDBC fencing 与业务数据库连接 |
| `xjtu.iron.distributed-lock.*` | 分布式锁 Starter | 默认锁选项和 Provider 选择 |
| `xjtu.iron.distributed-lock.redis.*` | Redis Provider | key 前缀与 Redis 锁语义 |
| `xjtu.iron.distributed-lock.fencing.jdbc.*` | JDBC fencing Provider | token 表、重试、建表策略 |

Starter、Core、Provider 不携带 `application.yml`；独立 Demo 和最终 `start` 应用负责提供配置值。

## 2. 完整示例

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      database: ${REDIS_DATABASE:0}
      username: ${REDIS_USERNAME:}
      password: ${REDIS_PASSWORD:}
      connect-timeout: ${REDIS_CONNECT_TIMEOUT:3s}
      timeout: ${REDIS_TIMEOUT:3s}

  datasource:
    url: ${MYSQL_URL:jdbc:mysql://127.0.0.1:3306/test}
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver

xjtu:
  iron:
    distributed-lock:
      enabled: true
      default-provider: redis
      namespace: default
      lease-time: 30s
      wait-time: 0s
      auto-renew: false
      # 为空时按 lease-time / 3 推导
      # renew-interval: 10s
      max-renew-time: 10m
      fencing-required: false
      # redis：强制使用 Redis 原生 INCR
      # jdbc-sequence：强制使用独立 JDBC Provider
      fencing-token-provider-name:
      fail-on-lock-lost: true

      redis:
        enabled: true
        key-prefix: iron:lock
        release-channel-prefix: iron:lock:release
        fencing-key-suffix: fence

      fencing:
        jdbc:
          enabled: false
          table-name: iron_lock_fencing_token
          max-retries: 5
          initialize-schema: false
          schema-platform: mysql
```

## 3. fencing 选择规则

当 `fencing-required=false` 时，不生成 token。

当 `fencing-required=true` 时：

1. `LockOptions.fencingTokenProviderName` 有值：
   - 等于当前锁 Provider 名称（如 `redis`）：使用原生 fencing；
   - 其他名称（如 `jdbc-sequence`）：从独立 Provider 注册表选择。
2. 未显式指定且锁 Provider 支持原生 fencing：使用原生 fencing。
3. 锁 Provider 不支持原生 fencing，且只有一个独立 Provider：自动使用该 Provider。
4. 存在多个独立 Provider 且未指定：拒绝猜测，返回参数错误。

代码显式传入的 `LockOptions` 不会与全局默认值逐字段合并；传入后以该对象为准。

## 4. Redis 与 JDBC 的选择

| 维度 | Redis INCR | JDBC sequence |
|---|---|---|
| 获取路径 | acquire Lua 内原子发号 | 加锁成功后独立数据库事务发号 |
| 延迟 | 低 | 较高 |
| 故障域 | 与 Redis 锁相同 | 可与业务数据库一致 |
| 数据回退风险 | Redis 丢数据可能破坏单调性 | 取决于数据库持久性 |
| 推荐场景 | 定时任务、批处理、可幂等普通业务 | 库存、资金状态、关键订单状态等强防旧写场景 |

## 5. JDBC token 表

```sql
CREATE TABLE IF NOT EXISTS iron_lock_fencing_token (
    namespace VARCHAR(128) NOT NULL,
    lock_name VARCHAR(256) NOT NULL,
    current_token BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (namespace, lock_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

生产环境建议由 Flyway/Liquibase 管理，配置：

```yaml
xjtu:
  iron:
    distributed-lock:
      fencing:
        jdbc:
          initialize-schema: false
```

Token 行不得删除或重置。清理 token 表会使后续 token 重新从 1 开始，旧 owner 可能重新获得更大的相对优先级，破坏 fencing 安全性。

## 6. Kubernetes 建议

- ConfigMap：Redis/MySQL 地址、端口、database、组件非敏感参数。
- Secret：Redis/MySQL 用户名和密码。
- 组件语义可由配置中心集中管理，但 Redis/DataSource 连接变化建议滚动重启，不依赖运行时热切换。

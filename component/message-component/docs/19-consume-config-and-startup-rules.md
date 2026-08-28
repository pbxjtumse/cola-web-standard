# 19. 消费配置与启动校验

推荐默认值：consume.reliability-mode=AT_LEAST_ONCE，idempotency.enabled=false，transaction.enabled=false。

fail-fast 规则：EFFECTIVELY_ONCE 必须启用 idempotency；idempotency.enabled=true 但没有 IdempotentExecutor 或等价适配器时启动失败；transaction.enabled=true 且 required=true 但没有事务执行器时启动失败；Kafka AT_LEAST_ONCE / EFFECTIVELY_ONCE 必须关闭 auto commit。

idempotency.store-name 是存储路由提示，不是表名。

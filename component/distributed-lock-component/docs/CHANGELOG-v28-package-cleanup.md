# V28 Package Cleanup

- 保持 `distributed-lock-api / core / provider / starter / demo` Maven module 结构不变。
- `acquire.outcome` 合并到 `acquire`；`LockHandleFactory` 移到 `client`。
- `result` 合并到 `execute`；`runtime` 合并到 `client`。
- `event + metrics` 合并为 `observability`；`name + token` 合并为 `support`。
- `spi.request / response / status / model` 合并为 `spi.protocol`，`DefaultLockProviderRegistry` 归入 `spi`。
- Starter 的 `configuration` 改为 `autoconfigure`，event/metrics/health 收敛为 `observability`。
- 删除当前工程没有任何引用的 `LockEventListener`、`LockWatchdogRegistry`。
- 恢复组件根 POM 对 `component-bom` 的 import。
- 不修改锁语义、Provider 行为、fencing、watchdog、Redisson/JDBC fencing 逻辑。
- 源码排版采用偏宽行风格：能在约 150 字符内清楚表达的声明/调用尽量不机械断行。

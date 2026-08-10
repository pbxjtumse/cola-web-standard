# v26 Redisson Provider + POM Governance Cleanup

本版以用户重新上传的 `distributed-lock-component(9)` 为真实基线，并重新合入 Redisson 能力；不是继续在上一份未覆盖到实际工程的压缩包上叠加。

## P0：POM 关系修正

1. 恢复 `distributed-lock-component/pom.xml` 对 `component-bom` 的 import。
2. 根组件只聚合 `distributed-lock-provider`，不再越级聚合 `distributed-lock-provider-redisson`。
3. `distributed-lock-provider/pom.xml` 统一聚合 redis / redisson / jdbc-fencing。
4. `distributed-lock-starter` 显式依赖 `distributed-lock-provider-redisson`，不写内部版本。
5. Redisson Provider 自身不写 `org.redisson:redisson` 版本；版本由完整工程根 dependencyManagement 管理。
6. 保留现有 parent `relativePath`、测试直接依赖等当前工程约定，不用生成版本覆盖掉已有 POM 治理。

## P0：Redisson 正式合入当前代码基线

- `RedissonLockProvider`
- `RedissonOwnershipRegistry`
- `RedissonLockKeyBuilder`
- `PROVIDER_NATIVE` waiting
- `PROVIDER_MANAGED` watchdog
- `RFencedLock` native fencing
- Redisson Starter 自动配置 / Client selector / Spring Redis connection factory
- Demo / Health / Contract tests / UML / docs

## P0：autoRenew 公共语义修复

上一版存在一个跨 API / Provider 不一致：watchdog 只由 `execute()` 启动，因此手工 `tryLock(autoRenew=true)` 下自研 Redis 不续期，而 Redisson 自己却会续期。

v26 改成在成功 acquire 创建 `LockHandle` 时统一启动 watchdog，释放 Handle 时立即停止。因此 `tryLock` 与 `execute` 的 autoRenew 语义一致。

## P1：Health 装配检查增强

`redisson.enabled=true` 后，如果 RedissonClient 未满足创建条件或 Provider 没有真正注册，Health 直接 DOWN，并输出：

- `redissonEnabled`
- `redissonProviderRegistered`
- `redissonConfigurationReady`

避免“配置写了 enabled=true，但实际上组件没有 Redisson Provider”仍显示健康。

## P1：时间语义优化

`ScheduledLockWatchdog` 改为使用可注入 `Clock`，不再在 tick 中直接 `Instant.now()`，与 acquisition/fencing/execute 其它 Core 流程保持一致，也便于确定性测试。

## P0：Provider 迁移安全文档

新增 `provider-migration-safety.md`：redis 与 redisson 使用不同物理协调对象，不能把 Provider 切换当普通灰度配置。旧 Pod(redis) 与新 Pod(redisson) 对同一逻辑 lockName 可能同时获取成功。切换必须排空/蓝绿/维护窗口，重要业务继续依赖 fencing 做资源端最后保护。

## 完整工程仍需同步的两处文件

本压缩包只包含 distributed-lock-component，因此以下内容以 `docs/integration-patches/` 精确补丁形式提供：

1. `cola-web-standard/pom.xml`：`redisson.version` + `org.redisson:redisson` dependencyManagement。
2. `component/component-bom/pom.xml`：新增 `distributed-lock-provider-redisson` 自研 artifact 版本管理。

## P1：移除 external fencing 的隐式默认 Provider

上一版 `FencingTokenCoordinator` 已经要求 external fencing 必须显式指定，但 `FencingTokenProviderRegistry` 仍残留 `defaultProvider()` 与“只有一个 Provider 时自动推导”的旧接口，形成两套相互矛盾的语义。

v26 彻底删除该默认推导能力：Registry 只负责 `providerName -> FencingTokenProvider` 精确查找；当当前 LockProvider 不支持 native fencing 时，必须显式配置 `fencingTokenProviderName`。同时更新 Starter 装配、测试和 `configuration.md`，保证代码、Health、配置文档使用同一套规则。

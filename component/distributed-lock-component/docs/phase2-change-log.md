# 二期实现变更清单

## API

- `LockHandle.fencingTokenProviderName()`：暴露 token 来源。
- `LockResult.fencingTokenProviderName`：结果中保留 token 来源。
- `FencingTokenGuard`：统一执行业务条件写并将拒绝映射为 `FENCING_REJECTED`。
- `LockStatusStageRules`：允许 `PROVIDER_ERROR + FENCING + acquired=true`。

## Core

- 新增 `FencingTokenMode`、`FencingTokenPlan`、`FencingTokenCoordinator`。
- 新增 `FencingTokenProviderRegistry` 与默认实现。
- `LockAcquireRequest` 新增 `nativeFencingRequired`，避免外部 JDBC 发号时 Redis 重复 INCR。
- `DefaultDistributedLockClient` 接入原生/外部 fencing 计划。
- 外部发号失败后尽最大努力释放锁，并保留 suppressed release error。
- 外部发号成功后重新 check ownerToken，防止慢发号跨过租约仍进入 callback。
- 事件增加 `FENCING_TOKEN_ISSUED`、`FENCING_TOKEN_FAILED`。
- 指标增加 `recordFencing`。

## Redis Provider

- `acquire.lua` 在成功获取锁时可选执行 `INCR`。
- Redis Cluster 下 lock key 与 fencing key 使用相同 hash tag。
- 外部 fencing 模式通过 `nativeFencingRequired=false` 禁止 Redis INCR。

## JDBC Provider

- 新增 `distributed-lock-fencing-provider-jdbc` 子模块。
- 新增 `JdbcSequenceFencingTokenProvider`，按 `namespace + lock_name` 维护单调 token。
- 新增 MySQL/H2 DDL 与快速 schema initializer。
- 首次并发 INSERT 冲突自动回滚重试。

## Starter / Actuator

- 新增 `JdbcFencingTokenProperties`。
- 新增 `JdbcFencingTokenAutoConfiguration`。
- 核心自动配置注册 Fencing Provider Registry 与 Coordinator。
- HealthIndicator 展示原生/外部 fencing 能力，并检查配置是否可用。
- Micrometer 增加 `iron.lock.fencing`。

## Demo

- Redis 原生 fencing endpoint。
- JDBC sequence fencing endpoint。
- 旧 token 覆盖拒绝 endpoint。
- 业务 Repository 使用 `last_fencing_token < incoming_token` 条件更新。

## 测试

- Coordinator 计划选择测试。
- Registry 默认与重复名称测试。
- Core 外部发号成功、失败和发号期间失锁测试。
- Redis native token 单调递增集成测试。
- JDBC token 单调、隔离和并发唯一性测试。
- 业务资源旧 token 拒绝测试。

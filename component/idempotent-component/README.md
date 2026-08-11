# 分布式幂等组件 v1

> 定位：统一描述、抢占、恢复和观测“同一个逻辑请求是否允许再次执行”的基础组件。

V1 的核心不是“加锁防重复”，而是一个**可恢复的幂等状态机**。

## 1. V1 核心能力

- 持久状态：`PROCESSING / SUCCESS / FAILED`
- `PROCESSING` 带 `ownerToken + version + processingExpireAt`
- PROCESSING 超时后语义上先进入 `FAILED(PROCESSING_TIMEOUT)`，再按策略决定是否重新抢占
- 每次重新抢占 `version + 1`
- `markSuccess / markFailed` 必须校验 `ownerToken + version`
- `requestHash` 防止同一个幂等 Key 被不同请求参数错误复用
- `SHORT_TERM`：Redis Hash + Lua + TTL，适合有限窗口重复抑制
- `DURABLE`：JDBC + Unique Key + Row Lock + 条件更新，适合长期业务幂等
- 可选接入 `DistributedLockClient`，只优化状态抢占竞争，不成为正确性根基
- 支持结果快照 replay（默认关闭）
- 支持 FailureClassifier、事件与 Micrometer 指标扩展点

## 2. 两类幂等模式

| 模式 | 默认 Repository | 生命周期 | 典型场景 |
|---|---|---|---|
| `SHORT_TERM` | Redis | `recordTtl` 到期后允许同 key 再执行 | 按钮连点、客户端 retry、5 分钟 requestId 去重 |
| `DURABLE` | JDBC | 不依赖 TTL 自动删除 | 订单、支付、退款、结算、重要消息消费 |

`SHORT_TERM` 是有限窗口 duplicate suppression，**不是限流器**。攻击者不断换 key 时它不会限制 QPS；真正接口防刷仍由 RateLimiter、验证码、风控、网关治理承担。

## 3. 分布式锁的边界

正确调用关系：

```text
DistributedLockClient (optional)
        ↓
Repository.tryAcquire   ← 锁只包住这一小段
        ↓
立即释放锁
        ↓
Business callback       ← 不持有分布式锁
        ↓
markSuccess / markFailed(ownerToken + version)
```

即使 `DistributedLockClient` 完全不可用，`IdempotencyRepository` 自己也必须通过 Unique/Lua/CAS 保证状态机正确。

## 4. 一个必须明确的 V1 边界

V1 已经保证：**旧 owner 不能覆盖新的幂等状态记录。**

但如果 callback 对业务数据库已经产生副作用，随后 `markSuccess` 失败，仍然存在“业务事实与幂等事实双写”的一致性窗口。因此高风险 DURABLE 场景应至少使用：

- 业务唯一键 / CAS；或
- `IdempotencyContext.fencingVersion()` 做业务条件写；
- 后续 V1.1 再增加事务模板集成，把业务事务与幂等最终状态更紧密地组合。

本版没有假装用 Redis 锁解决这个问题。

## 5. 项目结构

```text
idempotent-component
├── idempotent-api
├── idempotent-core
├── idempotent-provider
│   ├── idempotent-provider-redis
│   └── idempotent-provider-jdbc
├── idempotent-starter
├── idempotent-demo
└── docs
    ├── design.md
    ├── configuration.md
    └── diagrams
        ├── component
        ├── state
        └── sequence
```

详细设计见 `docs/design.md`，PlantUML 状态图/时序图见 `docs/diagrams/`。

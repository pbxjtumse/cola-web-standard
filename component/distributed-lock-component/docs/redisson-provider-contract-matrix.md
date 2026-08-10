# Redisson Provider Contract Matrix

> 目标：不是验证 Redisson 自己“会不会加锁”，而是验证 `RedissonLockProvider` 是否严格服从 iron-lock 的统一 SPI 语义。

## 1. 为什么需要 Contract Test

`LockProvider` 是后续 Redis Lua、Redisson、ZooKeeper、Etcd 的共同边界。一个 Provider 只有通过同一套行为契约，才能被 `LockProviderRegistry` 真正替换，而不是只做到“接口能编译”。

因此测试关注的是：

- Provider 返回的底层事实是否准确；
- owner 安全语义是否一致；
- wait/lease/watchdog 是否映射到统一 `LockOptions`；
- fencing token 是否属于本次成功 acquire；
- Provider 特有能力是否通过 `LockProviderCapabilities` 明确暴露，而不是偷偷改变公共 API。

## 2. 第一版契约矩阵

| 契约 | Redis Lua | Redisson | 当前 Redisson 测试 |
|---|---:|---:|---|
| acquire 成功返回 `ACQUIRED + LockLease` | ✅ | ✅ | `shouldMutuallyExclude...` |
| 同一 key 互斥 | ✅ | ✅ | `shouldMutuallyExclude...` |
| 竞争失败返回 `NOT_ACQUIRED` | ✅ | ✅ | `shouldMutuallyExclude...` |
| 错误 owner 不能释放当前锁 | ✅ | ✅ | `wrongOwnerMustNotReleaseCurrentLock` |
| 跨 Java 线程释放同一个 iron ownerToken | ✅ | ✅（ownership adapter） | `shouldMutuallyExcludeAndReleaseByOwnerTokenAcrossJavaThreads` |
| 旧 lease 过期后不能释放新 owner | ✅ | ✅ | `expiredOldOwnerMustNotReleaseNewOwner` |
| `check` 区分 HELD / NOT_OWNER / NOT_FOUND | ✅ | ✅ | 集成测试 + Provider 单测继续补齐 |
| fixed lease 自动到期 | ✅ | ✅ | `expiredOldOwner...` |
| Core managed watchdog | ✅ | N/A | Redis Provider 测试 |
| Provider managed watchdog | N/A | ✅ | `providerManagedWatchdogShouldKeepLockAlivePastInitialTimeout` |
| manual renew | ✅ | ❌（显式 capability=false） | Provider 返回 unsupported |
| Core BACKOFF waiting | ✅ | ✅ | Core waiter 契约 |
| Provider native waiting | ❌ | ✅ Pub/Sub | `shouldUseProviderNativePubSubWaiting` |
| native fencing token | ✅ Redis INCR | ✅ RFencedLock | `shouldReturnIncreasingNativeFencingTokens` |
| token 单调递增 | ✅ | ✅ | `shouldReturnIncreasingNativeFencingTokens` |
| 不泄漏底层 reentrant 语义 | N/A | ✅ 阻止 | `sameJavaThreadMustNotLeakRedissonReentrantSemantics` |
| Provider error 映射 | ✅ | ✅ | 需要在故障注入测试继续增强 |

## 3. 以后 ZK / Etcd 也复用这张契约

未来新增 Provider 时，测试结构应逐步收敛为：

```text
LockProviderContract
├── RedisLockProviderContractTest
├── RedissonLockProviderContractTest
├── ZookeeperLockProviderContractTest
└── EtcdLockProviderContractTest
```

具体产品可以有额外测试，但不能跳过统一契约。

## 4. 第一版 Redisson 明确不承诺的能力

虽然 Redisson 底层还支持 FairLock、ReadWriteLock、MultiLock、SpinLock、可重入锁等能力，但 v26 不把这些直接映射为 iron-lock 公共能力。

原因是 `ProviderCapabilities` 描述的是“iron-lock 已经稳定暴露并经过统一契约验证的能力”，而不是底层依赖库的产品功能列表。

第一版先完成：

1. 互斥；
2. owner-safe release/check；
3. fixed lease；
4. provider-managed watchdog；
5. provider-native wait；
6. native fencing；
7. 与 JDBC external fencing 组合。

这些稳定后，再单独设计 Fair/Reentrant 等公共语义。

# V28 分包收口版

本版基于 2026-08-22 当前分布式锁代码整理，保持 Maven module 结构 `distributed-lock-api / distributed-lock-core / distributed-lock-provider / distributed-lock-starter / distributed-lock-demo` 不变，只优化各 module 内部 package。没有新增 `distributed-lock-spi` Maven module，也没有修改 Redis Lua、Redisson、JDBC fencing、watchdog、fencing flow 等锁语义。

核心分包见 `docs/package-design-v28.md`，改动清单见 `docs/CHANGELOG-v28-package-cleanup.md`。代码排版采用偏宽行风格：能清晰放在约 150 字符内的声明和调用尽量保持同一行，避免一两个参数就机械换行。

---


当前组件在原有 Redis Lua Provider + JDBC fencing 基础上正式纳入 `redisson` LockProvider，并修正上一版 POM 聚合/BOM 关系。业务仍然只依赖 `DistributedLockClient`，不会直接依赖 `RedissonClient/RLock/RFencedLock`。


本版额外收口：

- `distributed-lock-component` 恢复 import `component-bom`；
- Redisson Provider 只由 `distributed-lock-provider` 二级聚合器聚合，不再根 POM 越级挂载；
- 子模块内部依赖全部无版本，由 `component-bom` 管自研版本、最外层根 POM 管 Redisson 第三方版本；
- Health 增加 Redisson “已启用但 Provider 未成功注册”的装配失败检测；
- watchdog 使用可注入 `Clock`，提升时间语义与测试一致性；
- 新增 `docs/provider-migration-safety.md`，明确 redis 与 redisson 是两个不同协调域，禁止无保护滚动混跑。

新增能力：

- `providerName=redisson`；
- `PROVIDER_NATIVE`：Provider 原生等待，Redisson 映射为 Pub/Sub waiting；
- `LockAutoRenewMode.PROVIDER_MANAGED`：Redisson 内部 watchdog 负责 TTL 续期；
- `RFencedLock.tryLockAndGetToken(...)` 映射为既有 `FencingTokenMode.NATIVE`；
- `RedissonOwnershipRegistry` 适配 iron ownerToken 与 Redisson threadId owner 语义；
- Redisson / Redis Lua 双 Provider 能力矩阵与 Contract Test；
- Spring Boot Starter 可复用 `spring.data.redis.*` 自动创建 RedissonClient，也支持选择业务已有 RedissonClient Bean。

详细设计见：

- `docs/redisson-integration-design.md`
- `docs/redisson-provider-contract-matrix.md`
- `docs/redisson-global-pom-changes.md`

---

一期：Redis Provider 做好，支持 token、Lua 解锁、Lua 续期、watchdog、事件、指标、执行模板。
二期：增加 fencing token 能力，先用 Redis INCR 或 DB sequence 实现。
三期：增加 ZK/Etcd Provider，服务强协调和公平锁。

分布式锁组件提供的是租约模型，而不是线程互斥模型。

每次加锁成功都会生成 ownerToken，用于证明当前 LockHandle 对锁的所有权。

ownerToken 用于安全解锁和安全续期，但不能单独保护业务资源。

对于核心状态写入，必须结合 fencing token、DB 条件更新、业务状态机和幂等机制。

Redis Provider 适合高性能、低成本、允许业务幂等兜底的场景。

ZK / Etcd Provider 适合强协调、Leader 选举、公平锁等场景。

组件默认不鼓励无限等待，推荐显式 waitTime 和 leaseTime。

业务优先使用 executeWithLock 模板，减少忘记释放锁、错误处理不统一等问题。

| 场景               | status           | acquired |
| ---------------- | ---------------- | -------- |
| tryLock 成功       | ACQUIRED         | true     |
| tryLock 没抢到      | NOT_ACQUIRED     | false    |
| execute 成功       | SUCCESS          | true     |
| execute 业务异常     | EXECUTION_FAILED | true     |
| execute 失锁       | LOCK_LOST        | true     |
| provider 异常，没拿到锁 | PROVIDER_ERROR   | false    |
| fencing 被拒绝      | FENCING_REJECTED | true     |


| 场景               | status                          | stage    | acquired |
| ---------------- | ------------------------------- | -------- | -------- |
| tryLock 成功       | ACQUIRED                        | ACQUIRE  | true     |
| waitTime 内没拿到锁   | NOT_ACQUIRED                    | WAIT     | false    |
| 参数非法             | INVALID_OPTIONS                 | VALIDATE | false    |
| Redis acquire 异常 | PROVIDER_ERROR                  | ACQUIRE  | false    |
| 业务回调异常           | EXECUTION_FAILED                | EXECUTE  | true     |
| 续期发现 key 不存在     | LOCK_LOST                       | RENEW    | true     |
| 续期 Redis 异常      | PROVIDER_ERROR                  | RENEW    | true     |
| DB fencing 拒绝    | FENCING_REJECTED                | FENCING  | true     |
| release Redis 异常 | RELEASE_FAILED 或 PROVIDER_ERROR | RELEASE  | true     |







如果业务已经成功，unlock 发现 key 不存在或 not owner，要不要把最终 LockResult 改成 RELEASE_FAILED？
NOT_FOUND / NOT_OWNER：记录 LOCK_LOST 事件，但业务结果可以仍然 SUCCESS。
PROVIDER_ERROR：可以返回 RELEASE_FAILED，或者 SUCCESS + releaseError，取决于你是否要强感知。


execute 业务成功，unlock 异常：
status = RELEASE_FAILED
acquired = true
error = release error

execute 业务成功，unlock 返回 NOT_FOUND / NOT_OWNER：
status = SUCCESS
acquired = true
记录 LOCK_LOST / RELEASE_FAILED 事件
因为 NOT_FOUND / NOT_OWNER 很可能只是锁已过期，不一定代表业务失败。真正业务正确性靠 fencing token。


Client 负责拿锁和模板执行；Handle 负责本次租约生命周期；Provider 负责底层原子操作；Waiter 负责等待；Watchdog 负责续期；FencingTokenProvider 负责生成业务写入版本号


Acquire 阶段 发生的事情

| Provider 结果        | API status     | stage   | acquired |
| ------------------ | -------------- | ------- | -------- |
| ACQUIRED           | ACQUIRED       | ACQUIRE | true     |
| NOT_ACQUIRED，且不再等待 | NOT_ACQUIRED   | WAIT    | false    |
| PROVIDER_ERROR     | PROVIDER_ERROR | ACQUIRE | false    |

Renew 阶段 

| Renew 结果       | API status     | stage | runtime                  |
| -------------- | -------------- | ----- | ------------------------ |
| RENEWED        | 不改变当前主流程状态     | RENEW | HELD                     |
| NOT_FOUND      | LOCK_LOST      | RENEW | LOST                     |
| NOT_OWNER      | LOCK_LOST      | RENEW | LOST                     |
| PROVIDER_ERROR | PROVIDER_ERROR | RENEW | UNKNOWN / HELD_UNCERTAIN |
这里要注意： NOT_FOUND / NOT_OWNER 是确定性失锁。 PROVIDER_ERROR 是不确定性异常。
Redis 超时不一定代表锁没了，只代表这次续期操作失败了。所以不能直接等价成 LOCK_LOST。更严谨是 续期返回 NOT_FOUND / NOT_OWNER -> LOCK_LOST
续期发生 Redis 异常 -> PROVIDER_ERROR，stage=RENEW

Release 阶段

| Release 结果     | API status               | stage   | runtime         |
| -------------- | ------------------------ | ------- | --------------- |
| RELEASED       | SUCCESS 或 ACQUIRED 后释放成功 | RELEASE | RELEASED        |
| NOT_FOUND      | LOCK_LOST 或 SUCCESS+事件   | RELEASE | LOST            |
| NOT_OWNER      | LOCK_LOST 或 SUCCESS+事件   | RELEASE | LOST            |
| PROVIDER_ERROR | RELEASE_FAILED           | RELEASE | RELEASE_UNKNOWN |




| 图中节点               | 类型                                    | 是否进入代码 | 说明                       |
| ------------------ | ------------------------------------- | -----: | ------------------------ |
| `INIT`             | 文档流程节点                                |      否 | 表示流程开始                   |
| `VALIDATING`       | 文档流程节点                                |      否 | 校验 lockName/options      |
| `INVALID_OPTIONS`  | `LockStatus`                          |      是 | 参数非法最终结果                 |
| `PREPARE_REQUEST`  | 文档流程节点                                |      否 | 生成 ownerToken、组装 request |
| `ACQUIRING`        | 文档流程节点                                |      否 | 正在尝试加锁                   |
| `ACQUIRED`         | `LockStatus` / `LockEventType`        |      是 | tryLock 成功               |
| `WAITING`          | 文档流程节点                                |      否 | 正在退避等待                   |
| `NOT_ACQUIRED`     | `LockStatus`                          |      是 | 正常竞争失败                   |
| `EXECUTING`        | 文档流程节点                                |      否 | 正在执行业务 callback          |
| `SUCCESS`          | `LockStatus`                          |      是 | execute 成功               |
| `EXECUTION_FAILED` | `LockStatus` / `LockEventType`        |      是 | 业务执行失败                   |
| `FENCING_REJECTED` | `LockStatus` / `LockEventType`        |      是 | fencing 被业务拒绝            |
| `RENEWED`          | `LockRenewStatus`                     |      是 | Provider 续期成功            |
| `NOT_FOUND`        | ProviderStatus                        |      是 | key 不存在                  |
| `NOT_OWNER`        | ProviderStatus                        |      是 | ownerToken 不匹配           |
| `LOCK_LOST`        | `LockStatus` / `LockEventType`        |      是 | 锁已丢失                     |
| `RELEASING`        | 文档流程节点                                |      否 | 正在释放锁                    |
| `RELEASED`         | `LockReleaseStatus` / `LockEventType` |      是 | Provider 释放成功            |
| `RELEASE_FAILED`   | `LockStatus` / `LockEventType`        |      是 | 释放失败                     |
| `STOP_WATCHDOG`    | 文档流程节点                                |      否 | 停止续期任务                   |



watchdog 不会无限续期。

watchdog 会在以下情况停止：
1. callback 执行完成；
2. handle.unlock() 成功或已调用；
3. renew 返回 NOT_FOUND / NOT_OWNER，说明锁已丢失；
4. renew 出现不可恢复 Provider 异常；
5. 达到 maxRenewTime；
6. 组件关闭。



README
架构图
核心时序图
状态图
Quick Start
Demo
单元测试
并发测试
故障测试
Benchmark
Metrics截图
生产边界说明
已知限制
版本发布记录



合法的组合 


| LockStatus         | 合法 LockStage | acquired | 说明                                                          |
| ------------------ | ------------ | -------: | ----------------------------------------------------------- |
| `INVALID_OPTIONS`  | `VALIDATE`   |    false | 参数非法                                                        |
| `ACQUIRED`         | `ACQUIRE`    |     true | `tryLock` 成功返回 `LockHandle`                                 |
| `NOT_ACQUIRED`     | `ACQUIRE`    |    false | `NO_WAIT` 下首次抢锁失败                                           |
| `NOT_ACQUIRED`     | `WAIT`       |    false | 等待重试后超时                                                     |
| `SUCCESS`          | `EXECUTE`    |     true | `execute` 业务正常完成，释放成功或释放失锁被降级为事件                            |
| `EXECUTION_FAILED` | `EXECUTE`    |     true | callback 普通异常                                               |
| `LOCK_LOST`        | `RENEW`      |     true | watchdog 续期发现 `NOT_FOUND / NOT_OWNER`                       |
| `LOCK_LOST`        | `CHECK`      |     true | `checkHeld/assertHeld` 发现失锁                                 |
| `LOCK_LOST`        | `RELEASE`    |     true | release 时发现 `NOT_FOUND / NOT_OWNER`，且 `failOnLockLost=true` |
| `FENCING_REJECTED` | `FENCING`    |     true | 二期 fencing token 被业务资源拒绝                                    |
| `RELEASE_FAILED`   | `RELEASE`    |     true | release 阶段 Provider 异常                                      |
| `PROVIDER_ERROR`   | `ACQUIRE`    |    false | acquire 阶段 Provider 异常                                      |
| `PROVIDER_ERROR`   | `RENEW`      |     true | renew 阶段 Provider 异常，是否失锁不确定                                |
| `PROVIDER_ERROR`   | `CHECK`      |     true | check 阶段 Provider 异常                                        |


最直观的对比

| 对象                 | 什么时候存在                | 是否变化 | 给谁用              | 表达什么                         |
| ------------------ | --------------------- | ---: | ---------------- | ---------------------------- |
| `LockRuntimeState` | callback 执行期间         |  会变化 | 组件内部             | 管“执行过程中这个 handle 现在怎么样”；|
| `LockResult`       | tryLock / execute 返回时 | 不应变化 | 业务方              | LockResult 管“这次 API 调用最后结果是什么”                 |
| `LockLease`        | 加锁成功时创建               | 不应变化 | 组件内部 / handle 使用 | 本次租约的身份信息                    |
| `LockHandle`       | 加锁成功后                 | 行为对象 | 业务方 + 组件内部       | 操作这次租约                       |



| lost  | releaseAttempted | 含义                      |
| ----- | ---------------- | ----------------------- |
| false | false            | 当前 handle 认为自己仍在持锁，还没释放 |
| true  | false            | 已经发现失锁，但还没进入本地释放流程      |
| false | true             | 正常进入过释放流程，且没有发现失锁       |
| true  | true             | 已经发现失锁，并且释放流程也执行过       |

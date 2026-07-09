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
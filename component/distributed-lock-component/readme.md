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
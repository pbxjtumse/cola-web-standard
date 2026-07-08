一期：Redis Provider 做好，支持 token、Lua 解锁、Lua 续期、watchdog、事件、指标、执行模板。
二期：增加 fencing token 能力，先用 Redis INCR 或 DB sequence 实现。
三期：增加 ZK/Etcd Provider，服务强协调和公平锁。
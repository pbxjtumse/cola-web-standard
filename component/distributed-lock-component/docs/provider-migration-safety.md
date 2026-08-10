# Provider 切换与滚动发布安全规则

## 结论

`providerName` 不是普通实现开关，而是协调域的一部分。

当前：

```text
redis Provider     -> 自研 Lua 锁对象
redisson Provider  -> Redisson RLock / RFencedLock 对象
```

两者故意使用不同物理 key/object 结构，避免底层数据格式互相破坏；代价是：**相同逻辑 lockName 在两个 Provider 之间不会天然互斥。**

## 禁止的滚动切换

不要这样直接把：

```yaml
default-provider: redis
```

滚动改成：

```yaml
default-provider: redisson
```

如果发布期间旧 Pod 与新 Pod 同时处理同一个业务资源，可能出现：

```text
Pod-A(redis)     获得 redis 域的 order:1001
Pod-B(redisson)  同时获得 redisson 域的 order:1001
```

从各自 Provider 看都“加锁成功”，但全局互斥已经被打破。

## 推荐迁移方式

优先级从高到低：

1. 停止/排空相关业务流量，确认旧 Provider 锁全部释放/过期，再统一切换；
2. 使用明确的维护窗口或蓝绿切换，保证同一业务资源不会同时由两个 Provider 处理；
3. 对重要业务同时使用 fencing token + 资源端条件写，作为旧执行覆盖新执行的最后防线；
4. 如果未来必须支持真正在线无损迁移，应单独设计 Dual-Read/Single-Write 或迁移协调协议，不能靠配置开关解决。

## `LockOptions.providerName` 的使用边界

允许按场景显式选择 Provider，但同一个业务资源族必须有稳定 Provider 约定。不要让不同调用方对同一 lockName 随意选择 redis/redisson。

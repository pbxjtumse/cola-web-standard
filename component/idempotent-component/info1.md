我建议提前把这个原则定死：

> **未来幂等组件不要直接依赖“自研 Redis 锁”或“Redisson 锁”，而是只依赖 `DistributedLockClient`。至于底层到底是 `redis` 还是 `redisson`，由最终应用配置决定。**

但如果你问我**生产环境默认推荐哪一个 Provider**，我会倾向：

> **幂等组件默认使用 Redisson Provider；自研 Redis Lua Provider 保留为轻量实现、基准实现和可替换实现。**

不过还有一个更重要的前提：**幂等组件的正确性不能建立在分布式锁之上。**

---

# 一、先把未来依赖关系定下来

我不建议未来写成：

```text
idempotent-component
        ↓
distributed-lock-provider-redisson
```

更不能：

```java
private RedissonClient redissonClient;
```

也不建议：

```java
private RedisLockProvider redisLockProvider;
```

应该是：

```text
idempotent-component
        ↓
distributed-lock-api
        ↓
DistributedLockClient
```

运行时：

```text
                    DistributedLockClient
                             │
                             ▼
                     LockProviderRegistry
                             │
              ┌──────────────┴──────────────┐
              ▼                             ▼
      RedisLockProvider             RedissonLockProvider
```

这样幂等组件根本不需要知道：

```text
RLock
RFencedLock
Lua
StringRedisTemplate
RedissonClient
```

这才符合我们一直建设技术组件的目标。

---

# 二、为什么我生产环境更倾向 Redisson？

不是因为自研 Redis Provider 做得不好。

而是两个 Provider 定位不同。

| 能力          |    自研 Redis Lua |            Redisson |
| ----------- | --------------: | ------------------: |
| 基础互斥        |               ✅ |                   ✅ |
| owner 安全释放  |               ✅ |                   ✅ |
| 自动续期        | ✅ Core watchdog | ✅ Redisson watchdog |
| 原生等待        |         BACKOFF |     Provider native |
| Pub/Sub 唤醒  |            ❌ 暂无 |                   ✅ |
| fencing     |      Redis INCR |         RFencedLock |
| 成熟度         |          我们自己维护 |                   高 |
| 代码依赖        |               轻 |                  较重 |
| 复杂 Redis 拓扑 |        需要自己持续维护 |         Redisson 负责 |
| 故障边界维护成本    |            我们负责 |     很多由 Redisson 负责 |

对于幂等这种将来会被很多业务调用的基础组件，我更愿意让：

```text
互斥执行能力
```

落在成熟 Provider 上。

所以未来生产默认可以是：

```yaml
xjtu:
  iron:
    distributed-lock:
      default-provider: redisson
```

幂等组件不用做任何改变。

---

# 三、但这里有一个更关键的问题

## 幂等 ≠ 分布式锁

千万不要未来设计成：

```text
请求进来
  ↓
获取分布式锁
  ↓
查有没有执行过
  ↓
没有
  ↓
执行
  ↓
成功
  ↓
释放锁
```

然后认为：

> 有 Redisson 以后幂等就解决了。

不够。

我们前面讨论 fencing 时，其实已经证明了：

```text
A 获取锁
A 卡顿
锁过期

B 获取锁
B 执行成功

A 恢复
A 继续执行
```

无论底层是：

```text
自研 Redis
Redisson
ZK
Etcd
```

**只靠租约锁都不能天然保证旧执行者不会继续执行业务。**

因此幂等组件的核心应该是：

```text
Idempotency State Machine
```

而不是 Lock。

---

# 四、我建议未来幂等组件的真正核心

例如：

```text
NOT_EXIST
    ↓
PROCESSING
    ↓
SUCCESS

PROCESSING
    ↓
FAILED / EXPIRED
```

核心数据：

```text
idempotencyKey
status
requestHash
result
owner
version
startedAt
expireAt
updatedAt
```

请求进来以后真正关键的动作应该是：

```text
1. 根据 idempotencyKey 查询状态

2. 原子抢占：
   NOT_EXIST / FAILED / EXPIRED
              ↓ CAS
          PROCESSING

3. 只有抢占成功者执行业务

4. 执行成功：
   PROCESSING → SUCCESS

5. 重复请求：
   SUCCESS
      → 直接返回历史结果

   PROCESSING
      → 等待 / 返回处理中 / 快速失败

   FAILED
      → 根据策略决定是否允许重新执行
```

其中最重要的是：

```text
原子状态转换
```

而不是分布式锁。

---

# 五、那么分布式锁在幂等组件里到底干什么？

我会把它定位成：

> **并发优化器 + 局部串行化手段，而不是最终正确性保证。**

例如：

```text
请求 A
请求 B
请求 C
同时 idempotencyKey = order:1001

          ↓

DistributedLockClient
把瞬时并发尽量串开

          ↓

IdempotencyRepository
仍然执行 CAS 状态判断

          ↓

只有真正抢占 PROCESSING 成功的人执行业务
```

也就是说：

```text
分布式锁
    ↓
减少数据库 CAS 冲突

状态机 / 条件更新
    ↓
保证最终正确性
```

这是我推荐的关系。

---

# 六、未来代码最好长这样

幂等 Core：

```java
public final class DefaultIdempotencyExecutor {

    private final IdempotencyRepository repository;

    private final DistributedLockClient distributedLockClient;

    public <T> IdempotencyResult<T> execute(
            String idempotencyKey,
            IdempotencyOptions options,
            IdempotencyCallback<T> callback) {

        // ...
    }
}
```

注意：

```java
DistributedLockClient
```

来自 `distributed-lock-api`。

而不是：

```java
RedissonLockProvider
```

---

# 七、甚至我建议 Lock 在幂等里是可选的

未来 `IdempotencyOptions` 可以有：

```java
private boolean lockEnabled;

private String lockProviderName;
```

或者组件级配置：

```yaml
xjtu:
  iron:
    idempotent:
      lock:
        enabled: true

        # 为空时继承 distributed-lock.default-provider
        provider-name:
```

默认：

```yaml
xjtu:
  iron:
    distributed-lock:
      default-provider: redisson

    idempotent:
      lock:
        enabled: true
```

那么幂等组件实际就是：

```text
Idempotent
    ↓ DistributedLockClient

providerName 没指定
    ↓

Distributed Lock 默认 Provider
    ↓

Redisson
```

如果某业务希望自研 Redis：

```yaml
xjtu:
  iron:
    idempotent:
      lock:
        provider-name: redis
```

最终构造：

```java
LockOptions.builder()
        .providerName(properties.getLock().getProviderName())
        ...
        .build();
```

就可以了。

---

# 八、甚至有些幂等模式不应该获取锁

例如数据库本身有：

```sql
UNIQUE KEY (idempotency_key)
```

第一次请求：

```sql
INSERT INTO idempotency_record(
    idempotency_key,
    status
)
VALUES (
    'order:1001',
    'PROCESSING'
);
```

A 插入成功：

```text
A 获得执行权
```

B 同时 INSERT：

```text
DuplicateKey
```

然后 B 查询：

```text
PROCESSING
```

这本身就是非常强的：

```text
原子抢占
```

这种模式理论上根本不一定需要：

```text
Redis Lock
```

所以我以后不会把：

```text
distributed-lock
```

设计成 `idempotent-core` 的强制基础依赖。

更理想的是：

```text
IdempotencyRepository
        ↓
真正状态正确性

Optional Lock Coordinator
        ↓
优化高并发竞争
```

---

# 九、我们未来甚至可以支持三种模式

我建议幂等一期设计时直接把这个概念考虑进去。

### 模式 A：STATE_ONLY

```text
不使用分布式锁

请求
 ↓
DB / Redis 原子状态抢占
 ↓
执行
```

适合：

```text
数据库唯一键
CAS
Redis Lua 状态机
```

---

### 模式 B：LOCK_AND_STATE

我最推荐的通用模式：

```text
Distributed Lock
       ↓
Idempotency State
       ↓
Business
```

这里锁：

```text
减少并发
```

State：

```text
保证正确性
```

生产默认可以选择：

```text
Redisson + DB/Redis idempotency state
```

---

### 模式 C：STATE + FENCING

高风险业务：

```text
Idempotency State
        +
Fencing Token
        +
Business conditional update
```

例如：

```sql
UPDATE payment_task
SET ...
WHERE id = ?
  AND fencing_token < ?
```

适合对：

```text
旧执行者恢复
```

特别敏感的场景。

---

# 十、如果幂等状态也存 Redis 呢？

这里又有一个很重要的区别。

假设幂等组件第一期有：

```text
RedisIdempotencyRepository
```

我反而会优先让它直接使用：

```text
Redis Lua
```

实现：

```text
查询 + 状态判断 + PROCESSING 抢占
```

而不是：

```text
先用 Redisson 加锁
再 GET
再 SET
```

比如可以一个 Lua 完成：

```text
key 不存在
    → SET PROCESSING
    → 返回 ACQUIRED

SUCCESS
    → 返回 SUCCESS + cached result

PROCESSING
    → 返回 PROCESSING

FAILED
    → 根据 retry policy 决定是否重新抢占
```

这比：

```text
RLock
GET
SET
unlock
```

更适合幂等状态本身。

所以要区分：

```text
Redisson
    是 LockProvider 的生产优选。

Redis Lua
    仍然可能是 RedisIdempotencyRepository
    最合适的状态原子操作实现。
```

这两个并不矛盾。

---

# 十一、所以未来可能出现这种组合

这其实是我最喜欢的一种：

```text
             IdempotencyExecutor
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
DistributedLockClient   IdempotencyRepository
          │                   │
          ▼                   ▼
RedissonLockProvider    Redis Lua Repository
```

也就是：

```text
锁：
Redisson

幂等状态机：
我们自己的 Redis Lua
```

或者：

```text
锁：
Redisson

幂等状态：
MySQL
```

这完全合理。

不要认为：

> 用了 Redisson 做锁以后，所有 Redis 操作都必须 Redisson 化。

不是这样。

---

# 十二、对于你现在这个组件体系，我的默认建议

我会提前把未来生产默认组合设计成：

| 场景      | Lock          | Idempotency State | 建议    |
| ------- | ------------- | ----------------- | ----- |
| 普通接口防重复 | Redisson      | Redis Lua         | ⭐⭐⭐⭐⭐ |
| 订单创建    | Redisson      | MySQL Unique/CAS  | ⭐⭐⭐⭐⭐ |
| 消息消费    | 可选 Redisson   | DB/Redis 状态记录     | ⭐⭐⭐⭐  |
| 定时任务防重复 | Redisson      | 状态表               | ⭐⭐⭐⭐⭐ |
| 高风险状态更新 | Redisson      | DB CAS + fencing  | ⭐⭐⭐⭐⭐ |
| 极轻量场景   | 自研 Redis Lock | Redis Lua         | ⭐⭐⭐⭐  |

所以总体不是：

```text
幂等用 Redis
or
幂等用 Redisson
```

而是：

```text
幂等组件：
    不绑定具体 Lock Provider。

生产默认 Lock：
    Redisson。

状态正确性：
    Redis Lua / DB CAS / Unique Key。

自研 Redis Lock：
    保留为可配置 Provider。
```

---

# 十三、这也决定了我们幂等组件的依赖方向

我建议以后：

```text
idempotent-component
├── idempotent-api
├── idempotent-core
├── idempotent-provider
│   ├── idempotent-provider-redis
│   └── idempotent-provider-jdbc
├── idempotent-starter
└── idempotent-demo
```

其中：

```text
idempotent-core
    ↓
distributed-lock-api
```

而不是：

```text
idempotent-core
    ↓
distributed-lock-provider-redisson   ❌
```

Starter 最终把：

```text
DistributedLockClient
IdempotencyRepository
IdempotencyExecutor
```

组装起来。

---

# 十四、我们甚至应该提前避免循环依赖

这一点很关键。

以后不要出现：

```text
distributed-lock-component
    ↓
idempotent-component

idempotent-component
    ↓
distributed-lock-component
```

正确依赖应该始终是：

```text
foundation
     ↑
distributed-lock-api
     ↑
idempotent-core
```

更准确地说：

```text
idempotent-core
    → distributed-lock-api

distributed-lock-core
    永远不知道 idempotent 存在
```

这样依赖方向单向。

---

# 十五、最后给你一个明确选择

如果明天我们开始写幂等一期，我会这样定：

```text
幂等组件的 Lock 抽象：
    DistributedLockClient

默认生产 Lock Provider：
    redisson

可替换 Provider：
    redis

幂等正确性核心：
    IdempotencyRepository + 状态机 + CAS

Redis 幂等存储：
    优先 Lua 原子状态转换

JDBC 幂等存储：
    Unique Key + 条件更新 / version

高一致场景：
    再叠加 fencing
```

因此，**不是让幂等组件“选择 Redisson 而抛弃自研 Redis”**。

更准确的架构是：

```text
                          Idempotency
                              │
              ┌───────────────┴───────────────┐
              │                               │
     并发协调 / 性能优化                   正确性核心
              │                               │
   DistributedLockClient              IdempotencyRepository
              │                               │
       ┌──────┴──────┐               ┌────────┴────────┐
       ▼             ▼               ▼                 ▼
    Redisson      Redis Lua       Redis Lua          JDBC
   默认生产        可替换          状态机            CAS/Unique
```

**我会推荐 Redisson 作为幂等组件默认使用的 Lock Provider，但绝不会让 Redisson 成为幂等组件的正确性根基。**

这个边界提前确定下来，对我们下一步设计幂等组件非常重要。
